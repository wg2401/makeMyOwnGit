package gurt.commands;

import gurt.helperFunctions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

//todo: relativize paths, handle file deletion/nonexistent files, and handle directory inputs

public class Add 
{
    public static void add(ArrayList<String> toAdd, Path projRootDir)
    {
        Path gurtDir = projRootDir.resolve(".gurt");
        
        //tracking files to handle duplicate index entries
        //absolute path string to hash strings
        HashMap<String,String> filesTrack = new HashMap<>();
        ArrayList<String> uniqueFiles = new ArrayList<>();

        //track duplicate directories to cut down runtime
        Set<Path> seenDirs = new HashSet<>();
        seenDirs.add(gurtDir.toAbsolutePath().normalize());

        //track nonexistent file paths for deletion or warning; stores absolute path strings
        HashSet<String> nonexistentFilesSet = new HashSet<>();
        ArrayList<String> nonexistentFilesList = new ArrayList<>();
        ArrayList<String> deletedFilesList = new ArrayList<>();

        //writing blobs to objects
        while (!toAdd.isEmpty())
        {
            try
            {   
                String file = toAdd.get(toAdd.size() - 1);
                toAdd.remove(toAdd.size() - 1);

                //get normalized absolute path for consistency
                Path fPath = Paths.get(file);
                Path absNormPath = fPath.toAbsolutePath().normalize();

                String absNormPathString = absNormPath.toString();

                if (!Files.exists(absNormPath)) 
                {
                    nonexistentFilesSet.add(absNormPathString);
                    nonexistentFilesList.add(absNormPathString);
                    continue;
                }

                //directory handling:
                if (Files.isDirectory(absNormPath))
                {
                    if (!seenDirs.contains(absNormPath))
                    {
                        List<Path> filesUnderDir = new ArrayList<>();
                        List<Path> directoriesUnderDir = new ArrayList<>();

                        NIOHandler.directoryRecurse(absNormPath, directoriesUnderDir, filesUnderDir, seenDirs, gurtDir);
                        for (Path f : filesUnderDir)
                        {   
                            toAdd.add(f.toString());
                        }
                    }
                    continue;
                }

                //user added same file more than once in same add call; skip
                if (filesTrack.containsKey(absNormPathString))
                {
                    continue;
                }
                
                byte[] content = null;
                byte[] contentBlob = null;

                //get content bytes
                
                content = Files.readAllBytes(Paths.get(absNormPathString));
                

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String blobHeader = "blob " + content.length + "\0";

                //append blob header + content bytes to byte stream. Enforce UTF_8 encoding

                out.write(blobHeader.getBytes(StandardCharsets.UTF_8));
                out.write(content);

                
                //transfer byte stream to contentBlob array
                contentBlob = out.toByteArray();

                //convert contentBlob bytes to hashed str
                StringBuilder hashedStringBuilder = ByteHandler.bytesToHashedSB(contentBlob);

                String hashString = hashedStringBuilder.toString();

                NIOHandler.writeObject(hashString, contentBlob, gurtDir.resolve("objects"));

                //add file data to trackers
                uniqueFiles.add(absNormPathString);
                filesTrack.put(absNormPathString, hashString);
                
            }
            catch(IOException e)
            {
                System.out.println("adding files error: " + e.getMessage());
            }
        }

        //writing new index file
        try
        {
            //getting entries from existing index file
            //if an entry is for a file that wasn't just added, add to files list and put hash in map
            Path indexPath = gurtDir.resolve("index");
            List<String> indexEntries = Files.readAllLines(indexPath);
            for (String entry : indexEntries)
            {
                String[] entryParts = entry.split(" ", 2);
                if (entryParts.length != 2)
                {
                    continue;
                }
                String hash = entryParts[0];
                String fileName = entryParts[1];

                String absNormPathString = projRootDir.resolve(fileName).toAbsolutePath().normalize().toString();

                Path oldPath = Paths.get(absNormPathString);
                if (!filesTrack.containsKey(absNormPathString) && Files.exists(oldPath))
                {
                    filesTrack.put(absNormPathString, hash);
                    uniqueFiles.add(absNormPathString);
                }
            }

            Collections.sort(uniqueFiles);

            //construct new String to write a new index file
            String newLine = System.lineSeparator();
            StringBuilder toWrite = new StringBuilder();

            for (String file : uniqueFiles)
            {
                if (nonexistentFilesSet.contains(file))
                {
                    nonexistentFilesSet.remove(file);
                    deletedFilesList.add(file);
                    continue;
                }

                Path fPath = Paths.get(file);
                Path relPath = projRootDir.relativize(fPath);

                toWrite.append(filesTrack.get(file) + " " + relPath.toString() + newLine);
                
            }

            System.out.println();

            if (!deletedFilesList.isEmpty())
            {
                System.out.println("Staging for deletion: ");
            }

            for (String file : deletedFilesList)
            {  
                Path fPath = Paths.get(file);
                String pathString = projRootDir.relativize(fPath).toString();
                System.out.println("    " + pathString);
            }

            if (nonexistentFilesList.size() - deletedFilesList.size() > 0)
            {
                System.out.println("Skipping following nonexistent files:");
            }

            for (String file : nonexistentFilesList)
            {
                if (nonexistentFilesSet.contains(file))
                {
                    Path fPath = Paths.get(file);
                    String pathString = projRootDir.relativize(fPath).toString();
                    System.out.println("    " + pathString);
                }
            }

            System.out.println();

            Files.writeString(indexPath, toWrite.toString());
            
        }
        catch(IOException e)
        {
            System.out.println("index write error: " + e.getMessage());
        }
    }    
}
