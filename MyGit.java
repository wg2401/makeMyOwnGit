import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class MyGit
{
    public static void main(String[] args)
    {
        if (args[0].equals("init"))
        {
            init();
        }

        else if (args[0].equals("add"))
        {
            Path dotGirt = Paths.get(".girt");
            Path objects = Paths.get(".girt/objects");
            if (!Files.exists(dotGirt) || !Files.exists(objects))
            {
                System.out.println("fatal: not a girt repository (or any of the parent directories): .girt");
            }
            ArrayList<String> toAdd = new ArrayList<>();
            int counter = 0;
            for (String a: args)
            {
                if (counter==0)
                {
                    counter++;
                }
                else
                {
                    toAdd.add(a);
                }
            }

            add(toAdd);
        }
    }

    private static void init()
    {
        try
        {
            Files.createDirectories(Paths.get(".girt"));
        }
        catch(IOException e)
        {
            System.out.println(e.getMessage());
        }
        
        Path objects = Paths.get(".girt/objects");
        try
        {
            Files.createDirectories(objects);
        }
        catch (IOException e) 
        {
            System.out.println(".girt failed: " + e.getMessage());
        }

        Path branches = Paths.get(".girt/refs/heads");
        try
        {
            Files.createDirectories(branches);
        }
        catch (IOException e)
        {
            System.out.println("branches failure: " + e.getMessage());
        }

        Path HEAD = Paths.get(".girt/HEAD");
        try
        {
            Files.writeString(HEAD, "ref: refs/heads/main");
        }
        catch (IOException e)
        {
            System.out.println("HEAD failure:" + e.getMessage());
        }

        Path INDEX = Paths.get(".girt/index");
        try
        {
            Files.createFile(INDEX);
        }
        catch (IOException e)
        {
            System.out.println("INDEX failure:" + e.getMessage());
        }

    }

    private static void add(ArrayList<String> toAdd)
    {
        for (String file : toAdd)
        {
            try
            {
                byte[] hashed = null;
                byte[] content = null;
                byte[] contentBlob = null;

                //get content bytes
                
                content = Files.readAllBytes(Paths.get(file));
                

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String blobHeader = "blob " + content.length + "\0";

                //append blob header + content bytes to byte stream. Enforce UTF_8 encoding

                out.write(blobHeader.getBytes(StandardCharsets.UTF_8));
                out.write(content);

                
                //transfer byte stream to contentBlob array
                contentBlob = out.toByteArray();

                //convert contentBlob bytes to SHA-1

                MessageDigest md = MessageDigest.getInstance("SHA-1");
                hashed = md.digest(contentBlob);


                //get hashed bytes into hex format, and append to string
                StringBuilder hashedStringBuilder = new StringBuilder();
                for (byte b: hashed)
                {
                    hashedStringBuilder.append(String.format("%02x", b));
                }

                String hashString = hashedStringBuilder.toString();


                //get first 2 chars for directory prefix, and rest of string as blob content
                String directoryName = hashString.substring(0,2);
                String obj = hashString.substring(2);

                //create intermediate directory
                Path intermediateDir = Paths.get(".girt/objects/" + directoryName);
                Files.createDirectories(intermediateDir);
                
                Path fileBlobPath = Paths.get(".girt/objects/" + directoryName + "/" + obj);

                if (!Files.exists(fileBlobPath))
                {
                    Files.write(fileBlobPath, contentBlob);
                }


                
            }
            catch(IOException | NoSuchAlgorithmException e)
            {
                System.out.println("add error: " + e.getMessage());
            }
        }
    }
}