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

//todo: relativize paths, handle file deletion/nonexistent files, and handle directory inputs

public class Add 
{
    public static void add(ArrayList<String> toAdd)
    {
        Path gurtDir = NIOHandler.findDotGurt();
        
        //tracking files to handle duplicate index entries
        HashMap<String,String> filesTrack = new HashMap<>();
        ArrayList<String> uniqueFiles = new ArrayList<>();

        //writing blobs to objects
        for (String file : toAdd)
        {
            try
            {

                //user added same file more than once in same add call; skip
                if (filesTrack.containsKey(file))
                {
                    continue;
                }
                
                byte[] hashed = null;
                byte[] content = null;
                byte[] contentBlob = null;

                //get content bytes
                
                content = Files.readAllBytes(Paths.get(file));
                

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


                //get first 2 chars for directory prefix, and rest of string as blob content
                String directoryName = hashString.substring(0,2);
                String obj = hashString.substring(2);

                //create intermediate directory
                Path intermediateDir = gurtDir.resolve("objects/" + directoryName);
                Files.createDirectories(intermediateDir);
                
                Path fileBlobPath = gurtDir.resolve("objects/" + directoryName + "/" + obj);

                if (!Files.exists(fileBlobPath))
                {
                    Files.write(fileBlobPath, contentBlob);
                }

                //add file data to trackers
                uniqueFiles.add(file);
                filesTrack.put(file, hashString);
                
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
                if (!filesTrack.containsKey(fileName))
                {
                    filesTrack.put(fileName, hash);
                    uniqueFiles.add(fileName);
                }
            }

            Collections.sort(uniqueFiles);

            //construct new String to write a new index file
            String newLine = System.lineSeparator();
            StringBuilder toWrite = new StringBuilder();

            for (String file : uniqueFiles)
            {
                toWrite.append(filesTrack.get(file) + " " + file + newLine);
            }

            Files.writeString(indexPath, toWrite.toString());
            
        }
        catch(IOException e)
        {
            System.out.println("index write error: " + e.getMessage());
        }
    }    
}
