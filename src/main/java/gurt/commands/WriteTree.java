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

                //
                byte[] blobHashedBytes = ByteHandler.hexStringToBytes(hash);
                Path filePath = Paths.get(filename);
                Path fileHomeDir = filePath.getParent();

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

            //append files in root directory
            ArrayList<String> curFiles = filesInDirectories.get(projRootPath);
            
            ByteArrayOutputStream treeContentStream = new ByteArrayOutputStream();
            for (String file : curFiles)
            {
                treeContentStream.write("100644 ".getBytes(StandardCharsets.UTF_8));
                treeContentStream.write(file.getBytes(StandardCharsets.UTF_8));
                treeContentStream.write(0);   

                byte[] fileHash = fileNameToHash.get(file);
                treeContentStream.write(fileHash);
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(projRootPath))
            {
                for (Path path : stream)
                {
                    if (Files.isDirectory(path))
                    {
                        byte[] subtreeHash = writeSubTrees(filesInDirectories, fileNameToHash);

                        treeContentStream.write("040000 ".getBytes(StandardCharsets.UTF_8));
                        treeContentStream.write(path.toString().getBytes(StandardCharsets.UTF_8));
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

            String intermediateDir = treeRootHash.substring(0,2);
            String objFileName = treeRootHash.substring(2);
            

            return null;
        }
        catch (IOException e)
        {


            return null;
        }        
    }

    public static byte[] writeSubTrees(HashMap<Path, ArrayList<String>> filesInDirectories, HashMap<String, byte[]> fileNameToHash)
    {





        return null;
    }
}
