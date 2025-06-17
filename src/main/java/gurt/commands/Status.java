package gurt.commands;

import gurt.helperFunctions.*;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;


public class Status
{
    public static void status(Path projRootDir)
    {
       try
       {
            Path dotGurtPath = projRootDir.resolve(".gurt");
            Path headPath = dotGurtPath.resolve("HEAD");

            if (!Files.exists(headPath)) 
            {
                System.out.println("Error: no HEAD found. Did you run `gurt init`?");
                return;
            }

            //get current commit and load it into map
            String headText = Files.readString(headPath);
            String refsString = new String();
            if (headText.startsWith("ref:")) 
            {
                refsString = headText.substring("ref:".length()).trim();
            }
            Path refsPath = dotGurtPath.resolve(refsString);

            HashMap<Path, String> fileToHash = new HashMap<>();

            //load latest commit blobs
            if (Files.exists(refsPath))
            {
                String prevCom = Files.readString(refsPath).trim();
                GurtFileHandler.loadCommit(fileToHash, prevCom, projRootDir);
            }

            //load files into lists
            ArrayList<String> toCommit = new ArrayList<>();
            ArrayList<String> unstaged = new ArrayList<>(); 
            ArrayList<String> untracked = new ArrayList<>();
            findHashes(projRootDir, toCommit, unstaged, untracked, projRootDir, fileToHash);

            //get cur branch name
            String[] headParts = headText.split(" ");
            String branchPathString = headParts[0];
            Path branchAbsolutePath = projRootDir.resolve(branchPathString);
            String branchName = branchAbsolutePath.getFileName().toString();

            System.out.println("On branch " + branchName + ":");
            System.out.println();

            System.out.println("Changes to be committed:");
            if (toCommit.isEmpty())
            {
                System.out.println("    (none)");
            }
            else
            {
                for (String f : toCommit)
                {
                    System.out.println("    " + f);
                }
            }
            System.out.println();

            System.out.println("Changes not staged for commit:");
            if (unstaged.isEmpty())
            {
                System.out.println("    (none)");
            }
            else
            {
                for (String f : unstaged)
                {
                    System.out.println("    " + f);
                }
            }
            System.out.println();
            
            System.out.println("Untracked files:");
            if (untracked.isEmpty())
            {
                System.out.println("    (none)");
            }
            else
            {
                for (String f : untracked)
                {
                    System.out.println("    " + f);
                }
            }
            System.out.println();
       }
       catch (IOException e)
       {
            System.out.println(e);
       }
    }

    //recurses thru all dirs and adds path names from index to respective lists (skipping .gurt)
    //pass in projRoot Dir (absolute path), so curDir will always be an absolute path
    //ArrayLists contain relativized paths
    private static void findHashes(Path curDir, ArrayList<String> toCommit, ArrayList<String> unstaged, ArrayList<String> untracked, Path projRootDir, HashMap<Path,String> fileToHash)
    {
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(curDir)) 
        {   
            if (curDir.equals(projRootDir.resolve(".gurt")))
            {
                return;
            }
            
            //recurse to add to lists
            for (Path p : stream)
            {
                if (Files.isDirectory(p))
                {
                    findHashes(p, toCommit, unstaged, untracked, projRootDir, fileToHash);
                }
                
                if (Files.isRegularFile(p))
                {
                    String hash = GurtFileHandler.indexHashSearch(projRootDir, p);
                    if (hash == null)
                    {
                        //path not found in index; untracked
                        untracked.add(projRootDir.relativize(p).toString());
                        continue;
                    }
                    else
                    {
                        //compute file's hash:
                        String fileHash = ByteHandler.computeBlobHash(p);

                        if (fileHash.equals(hash))
                        {
                            String fileBlobInCommit = fileToHash.get(p);
                            if (fileBlobInCommit == null || !fileHash.equals(fileBlobInCommit))
                            {
                                toCommit.add(projRootDir.relativize(p).toString());
                            }
                        }
                        else
                        {
                            unstaged.add(projRootDir.relativize(p).toString());
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("directoryRecurse error: " + e.getMessage());
        }
    }
}