package gurt.commands;

import gurt.helperFunctions.*;

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
            //getting dir paths
            Path objectsPath = dotGurtPath.resolve("objects");
            Path indexPath = dotGurtPath.resolve("index");
            Path headPath = dotGurtPath.resolve("HEAD");

            //reading in index entries into byte buffer
            List<String> indexEntries = Files.readAllLines(indexPath);

            ByteArrayOutputStream contentStream = new ByteArrayOutputStream();

            for (String entry : indexEntries)
            {
                String[] entryParts = entry.split(" ", 2);
                if (entryParts.length != 2)
                {
                    continue;
                }

                String hash = entryParts[0];
                String fileName = entryParts[1];
                contentStream.write("100644 ".getBytes(StandardCharsets.UTF_8));
                contentStream.write(fileName.getBytes(StandardCharsets.UTF_8));
                contentStream.write(0);                
                contentStream.write(ByteHandler.hexStringToBytes(hash));                                
            }

            //construct header
            int treeContentLength = contentStream.toByteArray().length;
            String header = "tree " + treeContentLength;
            ByteArrayOutputStream headerByteArrayOutputStream = new ByteArrayOutputStream();
            headerByteArrayOutputStream.write(header.getBytes(StandardCharsets.UTF_8));
            headerByteArrayOutputStream.write(0);
            byte[] headerBytes = headerByteArrayOutputStream.toByteArray();

            //construct tree obj byte (to be written)
            byte[] treeBytes = ByteHandler.combineTwoByteArrays(headerBytes, contentStream.toByteArray());

            //get hash
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] treeHashBytes = md.digest(treeBytes);
            StringBuilder treeHashString = new StringBuilder();
            for (byte b : treeHashBytes)
            {
                treeHashString.append(String.format("%02x", b));
            }

            //write tree to obj directory
            String treeIntermediateName = treeHashString.toString().substring(0,2);
            String treeDirectoryName = treeHashString.toString().substring(2);

            Path interTreeDir = objectsPath.resolve(treeIntermediateName);
            Path treePath = interTreeDir.resolve(treeDirectoryName);
            Files.createDirectories(interTreeDir);

            if (!Files.exists(treePath))
            {
                Files.write(treePath, treeBytes);
            }

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
            if (Files.exists(refsPath));
            {
                String prevCom = Files.readString(refsPath);
                commitContent.append(prevCom);
                commitContent.append("\n");
            }

            commitContent.append("\n");
            commitContent.append("placeholder msg for now");

            byte[] commitObjContent = commitContent.toString().getBytes(StandardCharsets.UTF_8);
            String commitObjHeader = "commit " + commitObjContent + "\0";
            ByteArrayOutputStream commitByteOutput = new ByteArrayOutputStream();
            commitByteOutput.write(commitObjHeader.getBytes(StandardCharsets.UTF_8));
            commitByteOutput.write(commitObjContent);

            byte[] fullCommitArray = commitByteOutput.toByteArray();

            md = MessageDigest.getInstance("SHA-1");
            byte[] hashedCommit = md.digest(fullCommitArray);

        }
        catch (IOException | NoSuchAlgorithmException e)
        {
            System.out.println(e.getMessage());
        }
    }
}
