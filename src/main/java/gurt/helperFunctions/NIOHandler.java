package gurt.helperFunctions;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class NIOHandler 
{
    public static Path findDotGurt()
    {
        Path curDir = Paths.get(".").toAbsolutePath();
        while (curDir!=null)
        {
            Path gurt = curDir.resolve(".gurt");
            if (Files.exists(gurt) && Files.isDirectory(gurt))
            {
                return gurt;
            }
            curDir = curDir.getParent();
        }

        return null;
    }

    public static Path findProjectRoot()
    {
        Path curDir = Paths.get(".").toAbsolutePath().normalize();
        while (curDir!=null)
        {
            Path gurt = curDir.resolve(".gurt");
            if (Files.exists(gurt) && Files.isDirectory(gurt))
            {
                return curDir;
            }
            curDir = curDir.getParent();
        }

        return null;
    }

    //helper method used for add(), adds all seen files and directories to lists for tracking
    //checks to see whether directory already has been added to prevent unnecessary recursive calls
    public static void directoryRecurse(Path curDir, List<Path> directories, List<Path> files, Set<Path> seenDirs, Path gurtDir)
    {
        Path normalizedDir = curDir.toAbsolutePath().normalize();

        if (normalizedDir.startsWith(gurtDir))
        {
            return;
        }

        if (seenDirs.contains(normalizedDir))
        {
            return;
        }

        seenDirs.add(normalizedDir);
        
        directories.add(normalizedDir);
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(normalizedDir)) 
        {
            for (Path p : stream)
            {
                if (Files.isDirectory(p))
                {
                    directoryRecurse(p, directories, files, seenDirs, gurtDir);
                }
                else if (Files.isRegularFile(p))
                {
                    files.add(p.toAbsolutePath().normalize());
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("directoryRecurse error: " + e.getMessage());
        }
    }

    public static void writeObject(String hash, byte[] object, Path objectsPath)
    {
        try
        {
            String intermediateDirStr = hash.substring(0,2);
            String objFileNameStr = hash.substring(2);
            
            Path intermediateDir = objectsPath.resolve(intermediateDirStr);
            Path objFileName = intermediateDir.resolve(objFileNameStr);

            Files.createDirectories(intermediateDir);
            if (!Files.exists(objFileName))
            {
                Files.write(objFileName, object);
            }
        }
        catch(IOException e)
        {
            System.out.println(e);
        }

    }

    //recursive
    //takes in absolute path for refs/heads as well as curDir (absolute path) and adds relative path strings (to refs/heads) to branchList
    public static void branchesList(Path branchesPath, Path curDir, ArrayList<String> branchList)
    {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(curDir)) 
        {
            for (Path p : stream)
            {
                if (Files.isDirectory(p))
                {
                    branchesList(branchesPath, p, branchList);
                }
                else if (Files.isRegularFile(p))
                {
                    Path relPath = branchesPath.relativize(p);
                    
                    branchList.add(relPath.toString());
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("branch listing error: " + e.getMessage());
        }
    }


}
