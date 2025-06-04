package gurt.commands;

import gurt.helperFunctions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class WriteTree 
{
    public String writeTree(Path indexPath)
    {
        try
        {  
            //Path projRootPath = 
            
            //read 
            List<String> indexEntries = Files.readAllLines(indexPath);


            return null;
        }
        catch (IOException e)
        {


            return null;
        }
        
    }
}
