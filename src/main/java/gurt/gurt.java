package gurt;

import java.util.*;

import gurt.commands.*;
import gurt.helperFunctions.*;

import java.nio.file.*;



public class Gurt
{
    public static void main(String[] args)
    {
        if (args[0].equals("init"))
        {
            Init.init();
        }

        else if (args[0].equals("add"))
        {
            Path dotGurt = directorySearching.findDotGurt();
            if (dotGurt == null)
            {
                System.out.println("fatal: not a gurt repository (or any of the parent directories): .gurt");
                return;
            }

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

            Add.add(toAdd);
        }

        else if (args[0].equals("yo"))
        {
            yo();
        }
    }

    private static void yo()
    {
        System.out.println("gurt: yo");
    }



}