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
            Path dotGurt = findDotGurt();
            if (dotGurt == null)
            {
                System.out.println("fatal: not a gurt repository (or any of the parent directories): .gurt");
                return;
            }

            Path objectsPath = dotGurt.resolve("objects");
            Path indexPath = dotGurt.resolve("index");
            if (!Files.exists(objectsPath) || !Files.exists(indexPath))
            {
                System.out.println("fatal: not a gurt repository (or any of the parent directories): .gurt");
                return;
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

        private static Path findDotGurt()
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

    private static void init()
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

    private static void add(ArrayList<String> toAdd)
    {
        Path gurtDir = findDotGurt();
        
        //tracking files to handle duplicate index entries
        HashMap<String,String> filesTrack = new HashMap<>();
        ArrayList<String> uniqueFiles = new ArrayList<>();

        //writing blobs to objects
        for (String file : toAdd)
        {
            try
            {

                //user added same file more than once in same add call; skip
                if (filesTrack.containsKey(file))
                {
                    continue;
                }
                
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
                Path intermediateDir = gurtDir.resolve("objects/" + directoryName);
                Files.createDirectories(intermediateDir);
                
                Path fileBlobPath = gurtDir.resolve("objects/" + directoryName + "/" + obj);

                if (!Files.exists(fileBlobPath))
                {
                    Files.write(fileBlobPath, contentBlob);
                }

                //add file data to trackers
                uniqueFiles.add(file);
                filesTrack.put(file, hashString);
                
            }
            catch(IOException | NoSuchAlgorithmException e)
            {
                System.out.println("adding files error: " + e.getMessage());
            }
        }

        //writing new index file
        try
        {
            //getting entries from existing index file
            //if an entry is for a file that wasn't just added, add to files list and put hash in map
            Path indexPath = gurtDir.resolve("index");
            List<String> indexEntries = Files.readAllLines(indexPath);
            for (String entry : indexEntries)
            {
                String[] entryParts = entry.split(" ", 2);
                if (entryParts.length != 2)
                {
                    continue;
                }
                String hash = entryParts[0];
                String fileName = entryParts[1];
                if (!filesTrack.containsKey(fileName))
                {
                    filesTrack.put(fileName, hash);
                    uniqueFiles.add(fileName);
                }
            }

            Collections.sort(uniqueFiles);

            //construct new String to write a new index file
            String newLine = System.lineSeparator();
            StringBuilder toWrite = new StringBuilder();

            for (String file : uniqueFiles)
            {
                toWrite.append(filesTrack.get(file) + " " + file + newLine);
            }

            Files.writeString(indexPath, toWrite.toString());
            
        }
        catch(IOException e)
        {
            System.out.println("index write error: " + e.getMessage());
        }
    }
}