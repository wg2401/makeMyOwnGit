package gurt.helperFunctions;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
        Path curDir = Paths.get(".").toAbsolutePath();
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

    public static void directoryRecurse(Path curDir, List<Path> directories, List<Path> files)
    {
        directories.add(curDir);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(curDir)) 
        {
            for (Path p : stream)
            {
                if (Files.isDirectory(p))
                {
                    directoryRecurse(p, directories, files);
                }
                else if (Files.isRegularFile(p))
                {
                    files.add(p);
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
}
