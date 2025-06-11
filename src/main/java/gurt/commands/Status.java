package gurt.commands;

import gurt.helperFunctions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;


public class Status
{
    public static void status(Path projRootDir)
    {
       try
       {
            Path dotGurtPath = projRootDir.resolve(".gurt");
            Path headPath = dotGurtPath.resolve("HEAD");

            //get cur branch name
            String headText = Files.readString(headPath);
            String[] headParts = headText.split(" ");
            String branchPathString = headParts[0];
            Path branchAbsolutePath = projRootDir.resolve(branchPathString);
            String branchName = branchAbsolutePath.getFileName().toString();

            System.out.println("On branch " + branchName + ":");
            System.out.println();

            
       }
       catch (IOException e)
       {
            System.out.println(e);
       }
    }

    //recurses thru all dirs and adds path names from index to respective lists
    private void findHashes(Path curDir, ArrayList<String> toCommit, ArrayList<String> modified, ArrayList<String> untracked, Path projRootDir)
    {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(curDir)) 
        {
            for (Path p : stream)
            {
                if (Files.isDirectory(p))
                {
                    findHashes(p, toCommit, modified, untracked, projRootDir);
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
                            toCommit.add(projRootDir.relativize(p).toString());
                        }
                        else
                        {
                            modified.add(projRootDir.relativize(p).toString());
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