package gurt.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Init 
{
    public static void init()
    {
        try
        {
            Files.createDirectories(Paths.get(".gurt"));
        }
        catch(IOException e)
        {
            System.out.println(e.getMessage());
        }
        
        Path objects = Paths.get(".gurt/objects");
        try
        {
            Files.createDirectories(objects);
        }
        catch (IOException e) 
        {
            System.out.println(".gurt failed: " + e.getMessage());
        }

        Path branches = Paths.get(".gurt/refs/heads");
        try
        {
            Files.createDirectories(branches);
        }
        catch (IOException e)
        {
            System.out.println("branches failure: " + e.getMessage());
        }

        Path HEAD = Paths.get(".gurt/HEAD");
        try
        {
            Files.writeString(HEAD, "ref: refs/heads/main");
        }
        catch (IOException e)
        {
            System.out.println("HEAD failure:" + e.getMessage());
        }

        Path INDEX = Paths.get(".gurt/index");
        try
        {
            Files.createFile(INDEX);
        }
        catch (IOException e)
        {
            System.out.println("INDEX failure:" + e.getMessage());
        }

    }
}
