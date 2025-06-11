package gurt.commands;

import gurt.helperFunctions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Log 
{
    public void log(Path projRootDir)
    {
        try
        {
            Path dotGurtPath = projRootDir.resolve(".gurt");
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

            Path intermediateDir = objectsPath.resolve(firstTwo);
            Path commitFile = intermediateDir.resolve(rest);

            //read in commit object and parse
            byte[] commitContent = Files.readAllBytes(commitFile);
            byte[] withoutHeader = ObjectParser.removeHeader(commitContent);
            String commitText = new String(withoutHeader, StandardCharsets.UTF_8);

            //try to get parent commit hash
            String[] fields = commitText.split("\n");
            int curLine = 0;
            String parentHash = null;
            boolean hasParent = false;

            for (; curLine < fields.length; curLine++)
            {
                if (fields[curLine].equals(""))
                {
                    break;
                }

                if (fields[curLine].startsWith("parent "))
                {
                    hasParent = true;
                    parentHash = fields[curLine].substring(7);
                }
            }

            //get current commit message
            //skip blank line
            curLine++;
            StringBuilder message = new StringBuilder();
            for (; curLine < fields.length; curLine++)
            {
                message.append(fields[curLine]);
                message.append("\n");
            }

            //print out current commit data:
            System.out.println("commit: " + latestCommitHash);
            System.out.println(message.toString());
            System.out.println();

            while (hasParent)
            {
                latestCommitHash = parentHash;
                
                firstTwo = parentHash.substring(0,2);
                rest = parentHash.substring(2);

                intermediateDir = objectsPath.resolve(firstTwo);
                commitFile = intermediateDir.resolve(rest);

                commitContent = Files.readAllBytes(commitFile);
                withoutHeader = ObjectParser.removeHeader(commitContent);
                commitText = new String(withoutHeader, StandardCharsets.UTF_8);

                fields = commitText.split("\n");
                curLine = 0;
                hasParent = false;

                for (; curLine < fields.length; curLine++)
                {
                    if (fields[curLine].equals(""))
                    {
                        break;
                    }

                    if (fields[curLine].startsWith("parent "))
                    {
                        hasParent = true;
                        parentHash = fields[curLine].substring(7);
                    }
                }

                curLine++;
                message = new StringBuilder();

                for (; curLine < fields.length; curLine++)
                {
                    message.append(fields[curLine]);
                    message.append("\n");
                }

                System.out.println("commit: " + latestCommitHash);
                System.out.println(message.toString());
                System.out.println();
            }
        }
        catch (IOException e)
        {
            System.out.println(e);
        }

    }    
}
