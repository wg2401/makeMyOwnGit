package gurt.commands;

import gurt.helperFunctions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Commit 
{
    public static void commit(Path dotGurtPath, String message)
    {
        try
        {
            //getting dir paths
            Path objectsPath = dotGurtPath.resolve("objects");
            Path indexPath = dotGurtPath.resolve("index");
            Path headPath = dotGurtPath.resolve("HEAD");
            
            String treeHashString = WriteTree.writeTree(indexPath);

            //create and write commit object
            StringBuilder commitContent = new StringBuilder();
            commitContent.append("tree ");
            commitContent.append(treeHashString.toString());            
            commitContent.append("\n");

            //get previous commit hash (if exists)
            String headText = Files.readString(headPath);
            String refsString = new String();
            if (headText.startsWith("ref:")) 
            {
                refsString = headText.substring("ref:".length()).trim();
            }

            Path refsPath = dotGurtPath.resolve(refsString);
            if (Files.exists(refsPath))
            {
                commitContent.append("parent ");
                
                String prevCom = Files.readString(refsPath).trim();
                commitContent.append(prevCom);
                commitContent.append("\n");
            }

            commitContent.append("\n");
            commitContent.append(message);

            byte[] commitObjContent = commitContent.toString().getBytes(StandardCharsets.UTF_8);
            String commitObjHeader = "commit " + commitObjContent.length + "\0";
            ByteArrayOutputStream commitByteOutput = new ByteArrayOutputStream();
            commitByteOutput.write(commitObjHeader.getBytes(StandardCharsets.UTF_8));
            commitByteOutput.write(commitObjContent);

            byte[] fullCommitArray = commitByteOutput.toByteArray();

            //convert commit bytes into hash
            String commitHashString = ByteHandler.bytesToHashedSB(fullCommitArray).toString();
            String commitSubDirName = commitHashString.substring(0,2);
            String commitFileName = commitHashString.substring(2);

            Path commitSubDirPath = objectsPath.resolve(commitSubDirName);
            Path commitFileNamePath = commitSubDirPath.resolve(commitFileName);

            Files.createDirectories(commitSubDirPath);

            if (!Files.exists(commitFileNamePath))
            {
                Files.write(commitFileNamePath, fullCommitArray);
            }

            
            //update refs path
            Files.writeString(refsPath, commitHashString, StandardCharsets.UTF_8, 
            StandardOpenOption.CREATE,  StandardOpenOption.TRUNCATE_EXISTING);

        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
    }
}
