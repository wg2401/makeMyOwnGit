package gurt.commands;

import gurt.helperFunctions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

public class WriteTree 
{
    public static String writeTree(Path indexPath)
    {
        try
        {  
            //get root dir of project
            Path projRootPath = NIOHandler.findProjectRoot();
            
            //maps each directory to a list of filenames (strings) under that directory (for recursive subtree construction)
            HashMap<Path, ArrayList<String>> filesInDirectories = new HashMap<>();

            //maps each file to it's byte[] hash
            HashMap<String, byte[]> fileNameToHash = new HashMap<>();

            //read files from index and add them to maps
            List<String> indexEntries = Files.readAllLines(indexPath);

            for (String entry : indexEntries)
            {
                String[] entryParts = entry.split(" ", 2);
                if (entryParts.length != 2)
                {
                    continue;
                }
                String hash = entryParts[0];
                String filename = entryParts[1];

                //link files paths to different hashes, and files to their directories
                byte[] blobHashedBytes = ByteHandler.hexStringToBytes(hash);
                Path filePath = projRootPath.resolve(filename).normalize();;
                Path fileHomeDir = filePath.getParent();
                if (fileHomeDir == null) 
                {
                    fileHomeDir = projRootPath;
                }

                if (filesInDirectories.containsKey(fileHomeDir))
                {
                    filesInDirectories.get(fileHomeDir).add(filename);
                    fileNameToHash.put(filename, blobHashedBytes);
                }
                
                else
                {
                    ArrayList<String> fileList = new ArrayList<>();
                    fileList.add(filename);
                    filesInDirectories.put(fileHomeDir, fileList);
                    
                    fileNameToHash.put(filename, blobHashedBytes);
                }
            }

            //append files in root directory to tree stream
            ArrayList<String> curFiles = filesInDirectories.get(projRootPath);

            //guard against empty directory edge case:
            if (curFiles == null)
            {
                curFiles = new ArrayList<>();
            }

            ByteArrayOutputStream treeContentStream = new ByteArrayOutputStream();
            for (String file : curFiles)
            {
                treeContentStream.write("100644 ".getBytes(StandardCharsets.UTF_8));
                treeContentStream.write(file.getBytes(StandardCharsets.UTF_8));
                treeContentStream.write(0);   

                byte[] fileHash = fileNameToHash.get(file);
                treeContentStream.write(fileHash);
            }

            //recurse to write subtrees
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(projRootPath))
            {
                for (Path path : stream)
                {
                    if (Files.isDirectory(path) && filesInDirectories.containsKey(path))
                    {
                        byte[] subtreeHash = writeSubTrees(path, filesInDirectories, fileNameToHash, projRootPath);

                        treeContentStream.write("40000 ".getBytes(StandardCharsets.UTF_8));
                        Path relPath = projRootPath.relativize(path);
                        treeContentStream.write(relPath.toString().getBytes(StandardCharsets.UTF_8));
                        treeContentStream.write(0);
                        treeContentStream.write(subtreeHash);
                    }
                }
            }

            //getting hash of tree root:
            byte[] treeRootContent = treeContentStream.toByteArray();

            ByteArrayOutputStream treeHeaderStream = new ByteArrayOutputStream();
            String header = "tree " + treeRootContent.length;            
            treeHeaderStream.write(header.getBytes(StandardCharsets.UTF_8));
            treeHeaderStream.write(0);
            byte[] treeRootHeader = treeHeaderStream.toByteArray();

            byte[] treeRootObj = ByteHandler.combineTwoByteArrays(treeRootHeader,treeRootContent);
            String treeRootHash = ByteHandler.bytesToHashedSB(treeRootObj).toString();

            NIOHandler.writeObject(treeRootHash, treeRootObj, projRootPath.resolve(".gurt").resolve("objects"));
            
            
            return treeRootHash;
        }
        catch (IOException e)
        {
            System.out.println("write tree err: " + e);
            return null;
        }        
    }

    public static byte[] writeSubTrees(Path curDir, HashMap<Path, ArrayList<String>> filesInDirectories, 
    HashMap<String, byte[]> fileNameToHash, Path projRootPath)
    {
        try
        {
            ArrayList<String> curFiles = filesInDirectories.get(curDir);

            //guard against empty directory edge case:
            if (curFiles == null)
            {
                curFiles = new ArrayList<>();
            }

            //write files into tree obj
            ByteArrayOutputStream treeContentStream = new ByteArrayOutputStream();
            for (String file : curFiles)
            {
                treeContentStream.write("100644 ".getBytes(StandardCharsets.UTF_8));

                //convert file Path to relativized with curDir
                Path filePath = projRootPath.resolve(file).normalize();
                Path relFilePath = curDir.relativize(filePath);
                String relFileString = relFilePath.toString();

                treeContentStream.write(relFileString.getBytes(StandardCharsets.UTF_8));
                
                
                treeContentStream.write(0);   

                byte[] fileHash = fileNameToHash.get(file);
                treeContentStream.write(fileHash);
            }

            //recurse for subtrees, remember to relativize using curDir
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(curDir))
            {
                for (Path path : stream)
                {
                    if (Files.isDirectory(path))
                    {
                        byte[] subtreeHash = writeSubTrees(path, filesInDirectories, fileNameToHash, projRootPath);

                        treeContentStream.write("40000 ".getBytes(StandardCharsets.UTF_8));
                        Path relPath = curDir.relativize(path);
                        treeContentStream.write(relPath.toString().getBytes(StandardCharsets.UTF_8));
                        treeContentStream.write(0);
                        treeContentStream.write(subtreeHash);
                    }
                }
            }

            //getting hash of tree root:
            byte[] treeRootContent = treeContentStream.toByteArray();

            ByteArrayOutputStream treeHeaderStream = new ByteArrayOutputStream();
            String header = "tree " + treeRootContent.length;            
            treeHeaderStream.write(header.getBytes(StandardCharsets.UTF_8));
            treeHeaderStream.write(0);
            byte[] treeRootHeader = treeHeaderStream.toByteArray();

            byte[] treeRootObj = ByteHandler.combineTwoByteArrays(treeRootHeader,treeRootContent);
            String treeRootHash = ByteHandler.bytesToHashedSB(treeRootObj).toString();

            NIOHandler.writeObject(treeRootHash, treeRootObj, projRootPath.resolve(".gurt").resolve("objects"));
            
            
            return ByteHandler.hexStringToBytes(treeRootHash);

        }
        catch (IOException e)
        {
            System.out.println("writeSubTrees err: " + e);
            return null;
        }
    }
    
}
