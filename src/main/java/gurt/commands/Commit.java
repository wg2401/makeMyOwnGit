package gurt.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Commit 
{
    public static void commit(Path dotGurtPath)
    {
        try
        {
            Path objectsPath = dotGurtPath.resolve("objects");
            Path indexPath = dotGurtPath.resolve("index");

            List<String> indexEntries = Files.readAllLines(indexPath);

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            for (String entry : indexEntries)
            {
                String[] entryParts = entry.split(" ", 2);
                if (entryParts.length != 2)
                {
                    continue;
                }

                String hash = entryParts[0];
                String fileName = entryParts[1];
                out.write("100644 ".getBytes(StandardCharsets.UTF_8));
                out.write(fileName.getBytes(StandardCharsets.UTF_8));
                out.write(0);
                
                //convert this to raw bytes
                out.write(hash.getBytes(StandardCharsets.UTF_8));
            }
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
    }
}
