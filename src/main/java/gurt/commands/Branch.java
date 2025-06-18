package gurt.commands;

import gurt.helperFunctions.*;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;


public class Branch 
{
    public void branchNew(Path projRootDir, String branchName)
    {
        try
        {
            Path dotGurtPath = projRootDir.resolve(".gurt");
            Path headPath = dotGurtPath.resolve("HEAD");
            Path branchesPath = dotGurtPath.resolve("refs").resolve("heads");

            //get current commit:
            String headText = Files.readString(headPath);
            String refsString = new String();
            
            if (headText.startsWith("ref:")) 
            {
                refsString = headText.substring("ref:".length()).trim();
            }
            
            Path refsPath = dotGurtPath.resolve(refsString);
            String curCom = "";
            
            if (Files.exists(refsPath))
            {
                curCom = Files.readString(refsPath).trim();

            }

            Path newBranchPath = branchesPath.resolve(branchName);

            //error checking:
            //branch already exists
            if (Files.exists(newBranchPath)) 
            {
                System.out.println("fatal: branch '" + branchName + "' already exists");
                return;
            }   

            //branch name is used for an intermediate directory
            if (Files.exists(newBranchPath) && Files.isDirectory(newBranchPath)) 
            {
                System.out.println("fatal: a directory named '" + branchName + "' already exists");
                return;
            }

            //branch path has a file that blocks its intermediate directory
            Path validDirCheck = newBranchPath.getParent();
            while (!validDirCheck.equals(branchesPath))
            {
                if (Files.isRegularFile(validDirCheck))
                {
                    System.out.println("fatal: " + validDirCheck + "' is not a directory");
                    return;
                }
            }

            Files.createDirectories(newBranchPath.getParent());
            Files.writeString(newBranchPath, curCom);

        }
        catch(IOException e)
        {

        }
    }

    public static void branchList(Path projRootDir)
    {
        try
        {   
            Path dotGurtPath = projRootDir.resolve(".gurt");
            Path headPath = dotGurtPath.resolve("HEAD");
            Path branchesPath = dotGurtPath.resolve("refs").resolve("heads");

            //get current branch:
            String headText = Files.readString(headPath);
            String[] headParts = headText.split(" ");
            String branchPathString = headParts[1];
            Path branchAbsolutePath = dotGurtPath.resolve(branchPathString);
            String curBranchName = (branchesPath.relativize(branchAbsolutePath)).toString();

            ArrayList<String> branchNames = new ArrayList<>();

            //recurse
            NIOHandler.branchesList(branchesPath, branchesPath, branchNames);

            Collections.sort(branchNames);

            System.out.println();
            for (String branch : branchNames)
            {
                if (curBranchName.equals(branch))
                {
                    System.out.print("*");
                }
                System.out.println(branch);
            }
            System.out.println();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }
}
