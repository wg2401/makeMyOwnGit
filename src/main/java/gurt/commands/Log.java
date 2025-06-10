package gurt.commands;

import gurt.helperFunctions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class Log 
{
    public void log(Path projRootDir)
    {
        try
        {
            Path dotGurtPath = projRootDir.resolve(projRootDir);
            Path objectsPath = dotGurtPath.resolve("objects");
            Path headPath = dotGurtPath.resolve("HEAD");
            
            //get file pointer to refs from head:
            String headText = Files.readString(headPath);
            String refsString = new String();
            if (headText.startsWith("ref:")) 
            {
                refsString = headText.substring("ref:".length()).trim();
            }

            Path refsPath = dotGurtPath.resolve(refsString);

            if (!Files.exists(refsPath))
            {
                System.out.println("No commits yet");
                return;
            }
            
            String latestCommitHash = Files.readString(refsPath).trim();
            String firstTwo = latestCommitHash.substring(0,2);
            String rest = latestCommitHash.substring(2);

            Path intermediateDir = objectsPath.resolve("firstTwo");
            Path commitFile = intermediateDir.resolve(rest);

            byte[] commitContent = Files.readAllBytes(commitFile);
            
            byte[] withoutHeader = ObjectParser.removeHeader(commitContent);

        }
        catch (IOException e)
        {
            System.out.println(e);
        }

    }    
}
