package gurt.commands;

import gurt.helperFunctions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class Checkout
{
    public static void checkoutHash(Path projRootDir, String commitHash)
    {



        GurtFileHandler.rebuildRepo(projRootDir, commitHash);
        System.out.println("HEAD is now in a detached state");
    }

    public static void checkoutBranch(String branchName)
    {

    }


}