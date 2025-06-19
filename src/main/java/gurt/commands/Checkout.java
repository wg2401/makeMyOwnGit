package gurt.commands;

import gurt.helperFunctions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class Checkout
{
    public static void checkoutHash(Path projRootDir, String commitHash)
    {
        try
        {
            Path dotGurtPath = projRootDir.resolve(".gurt");
            Path headPath = dotGurtPath.resolve("HEAD");
            Path objectsPath = dotGurtPath.resolve("objects");

            //valid input checking:
            //verify that the input is valid hash format
            if (!commitHash.matches("[0-9a-fA-F]{64}"))
            {
                System.out.println("fatal: invalid commit hash"); 
                return; 
            }

            //verify object exists
            Path comPath = objectsPath.resolve(commitHash.substring(0,2));
            comPath = comPath.resolve(commitHash.substring(2));

            if (!Files.exists(comPath))
            {
                System.out.println("fatal: object not found");
                return;
            }

            byte[] comObject = Files.readAllBytes(comPath);
            String comString = new String(comObject, StandardCharsets.UTF_8);
            if (!comString.startsWith("commit"))
            {
                System.out.println("fatal: object is not a commit");
                return;
            }


            GurtFileHandler.rebuildRepo(projRootDir, commitHash);
            Files.writeString(headPath, commitHash + System.lineSeparator());
            System.out.println("HEAD is now in a detached state");

            
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }

    public static void checkoutBranch(Path projRootDir, String branchName)
    {
        try
        {
            Path dotGurtPath = projRootDir.resolve(".gurt");
            Path headPath = dotGurtPath.resolve("HEAD");
            Path branchesPath = dotGurtPath.resolve("refs").resolve("heads");

            //getting path for inputted branch
            String[] pathComponents = branchName.split("/");
            Path bPath = branchesPath;

            for (String component : pathComponents)
            {
                if (component.equals(""))
                {
                    continue;
                }
                bPath = bPath.resolve(component);
            }

            if (!Files.isRegularFile(bPath))
            {
                System.out.println("error: branch doesn't exist");
                return;
            }

            //get commit hash and use to rebuild repo
            String hashString = Files.readString(bPath).trim();
            GurtFileHandler.rebuildRepo(projRootDir, hashString);
            Files.writeString(headPath, "ref: " + "refs/heads/" + branchName + System.lineSeparator());

            System.out.println("Now working on " + branchName);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }


}