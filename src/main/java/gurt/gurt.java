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
                System.out.println("invalid args, usage: commit <message>");
                return;
            }

            // Join all args after "commit" into a message
            StringBuilder commitMess = new StringBuilder();
            for (int i = 1; i < args.length; i++)
            {
                commitMess.append(args[i]).append(" ");
            }

            String message = commitMess.toString().trim();
            Commit.commit(dotGurtPath, message);
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

        else if (args[0].equals("status"))
        {
            if (!inGurtRepo)
            {
                System.out.println("fatal: not a gurt repository (or any of the parent directories): .gurt");
                return;
            }

            Status.status(projRootDir);
        }

        else if (args[0].equals("branch"))
        {
            if (!inGurtRepo)
            {
                System.out.println("fatal: not a gurt repository (or any of the parent directories): .gurt");
                return;
            }

            if (args.length == 1)
            {
                Branch.branchList(projRootDir);
            }
            
            else if (args.length == 2)
            {
                Branch.branchNew(projRootDir, args[1]);
            }
        }

        else if (args[0].equals("checkout"))
        {
            if (!inGurtRepo)
            {
                System.out.println("fatal: not a gurt repository (or any of the parent directories): .gurt");
                return;
            }

            if (args.length!=3)
            {
                System.out.println("invalid args, usage: checkout hash/branch <input>");
                return;
            }

            if (args[1].equals("hash"))
            {
                Checkout.checkoutHash(projRootDir, args[2]);
            }

            else if (args[1].equals("branch"))
            {
                Checkout.checkoutBranch(projRootDir, args[2]);
            }

            else
            {
                System.out.println("invalid args, usage: checkout hash/branch <input>");
                return; 
            }
        }

        else if (args[0].equals("merge"))
        {
            if (!inGurtRepo)
            {
                System.out.println("fatal: not a gurt repository (or any of the parent directories): .gurt");
                return;
            }

            if (args.length!=2)
            {
                System.out.println("invalid args, usage: merge <branch>");
            }

            else
            {
                Merge.merge(projRootDir, args[1]);
            }
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