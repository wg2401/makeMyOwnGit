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
    public void branchNew()
    {

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
