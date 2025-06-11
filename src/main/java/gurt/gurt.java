package gurt;

import java.util.*;

import gurt.commands.*;
import gurt.helperFunctions.*;

import java.nio.file.*;



public class Gurt
{
    public static void main(String[] args)
    {
        Path projRootDir = NIOHandler.findProjectRoot();
        boolean inGurtRepo;
        if (projRootDir == null)
        {
            inGurtRepo = false;
        }
        else
        {
            inGurtRepo = true;
        }

        if (args[0].equals("init"))
        {
            if (inGurtRepo)
            {
                System.out.println("error: this is already a gurt repo");
            }
            
            else
            {
                Init.init();
            }
        }

        else if (args[0].equals("add"))
        {
            if (!inGurtRepo)
            {
                System.out.println("fatal: not a gurt repository (or any of the parent directories): .gurt");
                return;
            }
            
            Path dotGurt = projRootDir.resolve(".gurt");

            Path objectsPath = dotGurt.resolve("objects");
            Path indexPath = dotGurt.resolve("index");
            if (!Files.exists(objectsPath) || !Files.exists(indexPath))
            {
                System.out.println("fatal: not a gurt repository (or any of the parent directories): .gurt");
                return;
            }
            ArrayList<String> toAdd = new ArrayList<>();
            int counter = 0;
            for (String a: args)
            {
                if (counter==0)
                {
                    counter++;
                }
                else
                {
                    toAdd.add(a);
                }
            }

            Add.add(toAdd, projRootDir);
        }

        else if (args[0].equals("yo"))
        {
            yo();
        }

        else if (args[0].equals("commit"))
        {
            if (!inGurtRepo)
            {
                System.out.println("fatal: not a gurt repository (or any of the parent directories): .gurt");
                return;
            }
            Path dotGurtPath = projRootDir.resolve(".gurt");
            if (args.length < 2) 
            {
                System.out.println("invalid args, usage: commit \"<message>\"");
                return;
            }
            
            //parse commit message
            StringBuilder commitMess = new StringBuilder();
            
            String firstWord = args[1];
            String lastWord = args[args.length - 1];

            if (firstWord.charAt(0) != '\"' || lastWord.charAt(lastWord.length() - 1) != '\"')
            {
                System.out.println("invalid args, usage: commit \"<message>\"");
                return;
            }

            for (int i = 1; i < args.length; i++)
            {
                commitMess.append(args[i] + " ");
            }

            String message = commitMess.toString().substring(0, commitMess.length() - 1);
            Commit.commit(dotGurtPath,message);
        }

        else if (args[0].equals("write-tree"))
        {
            if (!inGurtRepo)
            {
                System.out.println("fatal: not a gurt repository (or any of the parent directories): .gurt");
                return;
            }
            System.out.println(WriteTree.writeTree(projRootDir.resolve("index")));
        }

        else if (args[0].equals("log"))
        {
            if (!inGurtRepo)
            {
                System.out.println("fatal: not a gurt repository (or any of the parent directories): .gurt");
                return;
            }
            Log.log(projRootDir);
        }

        else
        {
            System.out.println("command not found");
        }
    }

    private static void yo()
    {
        System.out.println("gurt: yo");
    }



}