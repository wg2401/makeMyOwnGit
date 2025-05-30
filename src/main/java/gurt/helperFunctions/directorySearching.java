package gurt.helperFunctions;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class directorySearching 
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
}
