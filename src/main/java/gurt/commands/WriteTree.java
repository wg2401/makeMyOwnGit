package gurt.commands;

import gurt.helperFunctions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

public class WriteTree 
{
    public static String writeTree(Path indexPath)
    {
        try
        {  
            //get root dir of project
            Path projRootPath = NIOHandler.findProjectRoot();
            
            //maps each directory to a list of filenames (strings) under that directory (for recursive subtree construction)
            //maps absolute paths to relative paths
            HashMap<Path, ArrayList<String>> filesInDirectories = new HashMap<>();

            //maps each file path to it's byte[] hash
            //maps relative path (from proj root) to byte[]
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
                filesInDirectories.put(projRootPath,curFiles);
            }

            //add all files to a list to write to tree later
            ArrayList<Path> fileAbsPaths = new ArrayList<>();
            for (String file : curFiles)
            {
                Path toAdd = projRootPath.resolve(file).normalize();
                fileAbsPaths.add(toAdd);
            }
            //sort to keep keep lexi sort
            Collections.sort(fileAbsPaths);

            ArrayList<Path> dirs = new ArrayList<>();
            //add dirs to list to sort
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(projRootPath))
            {
                for (Path path : stream)
                {
                    if (Files.isDirectory(path) && filesInDirectories.containsKey(path))
                    {
                        dirs.add(path);
                    }
                }
            }

            Collections.sort(dirs);

            ByteArrayOutputStream treeContentStream = new ByteArrayOutputStream();

            //merge sort step to keep lexi sort order of tree
            int fileP = 0;
            int dirP = 0;
            while (fileP < fileAbsPaths.size() || dirP < dirs.size())
            {
                Path curFP = null;
                Path curDP = null;
                
                //absolute paths
                if (fileP < fileAbsPaths.size())
                {
                    curFP = fileAbsPaths.get(fileP);
                }

                if (dirP < dirs.size())
                {
                    curDP = dirs.get(dirP);
                }
                
                // if no more files then write directory entry 
                if (curFP == null) 
                {
                    byte[] subtreeHash = writeSubTrees(curDP, filesInDirectories, fileNameToHash, projRootPath);
                    //write the directory's flat file name
                    Path relPath = curDP.getFileName();

                    treeContentStream.write("040000 ".getBytes(StandardCharsets.UTF_8));
                    treeContentStream.write(relPath.toString().getBytes(StandardCharsets.UTF_8));
                    treeContentStream.write(0);
                    treeContentStream.write(subtreeHash);

                    dirP++;
                }

                //write file to tree
                else if (curDP == null || curFP.compareTo(curDP) < 0 || dirP >= dirs.size())
                {
                    //convert absolute path back to relative path
                    Path relF = projRootPath.relativize(curFP);
                    
                    treeContentStream.write("100644 ".getBytes(StandardCharsets.UTF_8));
                    treeContentStream.write(relF.toString().getBytes(StandardCharsets.UTF_8));
                    treeContentStream.write(0);

                    //use relative path string to get hash bytes
                    byte[] fileHash = fileNameToHash.get(relF.toString());
                    treeContentStream.write(fileHash);

                    fileP++;
                }
                //recursively write dir to tree
                else
                {
                    //get hash from recursive call
                    byte[] subtreeHash = writeSubTrees(curDP, filesInDirectories, fileNameToHash, projRootPath);

                    //write the directory's flat file name
                    Path relPath = curDP.getFileName();

                    treeContentStream.write("040000 ".getBytes(StandardCharsets.UTF_8));
                    treeContentStream.write(relPath.toString().getBytes(StandardCharsets.UTF_8));
                    treeContentStream.write(0);
                    treeContentStream.write(subtreeHash);

                    dirP++;
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

    private static byte[] writeSubTrees(Path curDir, HashMap<Path, ArrayList<String>> filesInDirectories, 
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

            //put file relative paths into list for sorting
            ArrayList<Path> fileRelPaths = new ArrayList<>();
            ByteArrayOutputStream treeContentStream = new ByteArrayOutputStream();
            for (String file : curFiles)
            {
                Path absFile = projRootPath.resolve(file).normalize();
                Path relPath = curDir.relativize(absFile);
                fileRelPaths.add(relPath);
            }

            Collections.sort(fileRelPaths);

            //put directory relative paths in for sorting
            ArrayList<Path> dirRelPaths = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(curDir))
            {
                for (Path path : stream)
                {
                    if (Files.isDirectory(path) && filesInDirectories.containsKey(path))
                    {
                        Path relPath = curDir.relativize(path);
                        dirRelPaths.add(relPath);
                    }
                }
            }

            Collections.sort(dirRelPaths);

            //merge sort
            int fileP = 0;
            int dirP = 0;

            while (fileP < fileRelPaths.size() || dirP < dirRelPaths.size())
            {
                Path curFP = null;
                Path curDP = null;
                
                //absolute paths
                if (fileP < fileRelPaths.size())
                {
                    curFP = fileRelPaths.get(fileP);
                }

                if (dirP < dirRelPaths.size())
                {
                    curDP = dirRelPaths.get(dirP);
                }

                //if no more files then write dir
                if (curFP == null) 
                {
                    byte[] subtreeHash = writeSubTrees(curDP, filesInDirectories, fileNameToHash, projRootPath);
                    
                    //write in flat file name
                    Path relPath = curDP;

                    treeContentStream.write("040000 ".getBytes(StandardCharsets.UTF_8));
                    treeContentStream.write(relPath.toString().getBytes(StandardCharsets.UTF_8));
                    treeContentStream.write(0);
                    treeContentStream.write(subtreeHash);

                    dirP++;
                }

                //write file to tree
                else if (curDP == null || curFP.compareTo(curDP) < 0 || dirP >= dirRelPaths.size())
                {                    
                    treeContentStream.write("100644 ".getBytes(StandardCharsets.UTF_8));
                    //write curFP (path relativized with curDir) directly
                    treeContentStream.write(curFP.toString().getBytes(StandardCharsets.UTF_8));
                    treeContentStream.write(0);

                    //get path relative to projRootDir to get filehash
                    Path absFile = curDir.resolve(curFP).normalize();
                    Path rootRelPath = projRootPath.relativize(absFile);
                    byte[] fileHash = fileNameToHash.get(rootRelPath.toString());
                    treeContentStream.write(fileHash);

                    fileP++;
                }
                //recursively write dir to tree
                else
                {
                    //get absolute path for recursive call:
                    Path absDir = curDir.resolve(curDP).normalize();

                    //get hash from recursive call
                    byte[] subtreeHash = writeSubTrees(absDir, filesInDirectories, fileNameToHash, projRootPath);

                    treeContentStream.write("040000 ".getBytes(StandardCharsets.UTF_8));
                    //directly write in curDP (relative path)
                    treeContentStream.write(curDP.toString().getBytes(StandardCharsets.UTF_8));
                    treeContentStream.write(0);
                    treeContentStream.write(subtreeHash);

                    dirP++;
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
