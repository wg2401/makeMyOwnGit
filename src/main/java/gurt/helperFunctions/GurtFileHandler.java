package gurt.helperFunctions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

public class GurtFileHandler 
{
    //remove header from object bytes
    public static byte[] removeHeader(byte[] obj)
    {
        int nullTermInd = 0;

        while (obj[nullTermInd] != 0)
        {
            nullTermInd++;
        }

        byte[] withoutHeader = new byte[obj.length - 1 - nullTermInd];
        
        for (int i = nullTermInd + 1; i < obj.length; i++)
        {
            withoutHeader[i - 1 - nullTermInd] = obj[i];
        }
        
        return withoutHeader;
    }    

    //pass in relative or absolute path to conduct binary search in index
    //returns the chosen path's hash, or -1 if the path doesn't exist in index
    public static String indexHashSearch(Path projRootDir, Path lookFor)
    {
        try
        {
            //useful paths
            Path dotGurtPath = projRootDir.resolve(".gurt");
            Path indexPath = dotGurtPath.resolve("index");
            
            //sanitize path
            Path normalizedRelPath = projRootDir.relativize(lookFor.toAbsolutePath().normalize());
            String relPathStr = normalizedRelPath.toString();

            //binary search
            List<String> indexEntries = Files.readAllLines(indexPath);
            int lp = 0;
            int rp = indexEntries.size() - 1;
            int middle;

            while (lp <= rp) 
            {
                middle = (lp + rp)/2;
                String[] fields = indexEntries.get(middle).split(" ", 2);
                
                //guard against bad index entries
                if (fields.length != 2)
                {
                    break;
                }

                String midPath = fields[1];
                
                if (midPath.compareToIgnoreCase(relPathStr) == 0)
                {
                    return fields[0];
                }

                else if (midPath.compareToIgnoreCase(relPathStr) < 0)
                {
                    lp = middle + 1;
                }

                else
                {
                    rp = middle - 1;
                }

            }

            return null;
        }
        catch(IOException e)
        {
            throw new RuntimeException("Failed to read index during binary search", e);
        }
    }

    //fills hashmap with absolute path -> hash pairings for the given commit hash
    public static void loadCommit(HashMap<Path, String> fileToHash, String hash, Path projRootDir)
    {
        try
        {
            Path dotGurt = projRootDir.resolve(".gurt");
            Path objectPath = dotGurt.resolve("objects");

            //get and trim commit object
            String firstTwo = hash.substring(0,2);
            String rest = hash.substring(2);

            Path intermediate = objectPath.resolve(firstTwo);
            Path commitObj = intermediate.resolve(rest);

            byte[] commitContent = Files.readAllBytes(commitObj);
            commitContent = removeHeader(commitContent);

            //now parse
            String commitText = new String(commitContent, StandardCharsets.UTF_8);
            String[] fields = commitText.split("\n");
            String treeRootHash = fields[0];
            treeRootHash = treeRootHash.substring(5);

            subTreeRecurse(projRootDir, treeRootHash, projRootDir, fileToHash);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }

    //recursively add files to map from subtree objects
    //takes in projRootDir (absolute path) and maps file abs path -> hash
    public static void subTreeRecurse(Path curDir, String treeHash, Path projRootDir, HashMap<Path, String> fileToHash)
    {
        try
        {
            Path dotGurt = projRootDir.resolve(".gurt");
            Path objectPath = dotGurt.resolve("objects");

            //read in and clean tree object:
            String firstTwo = treeHash.substring(0,2);
            String rest = treeHash.substring(2);
            Path intermediate = objectPath.resolve(firstTwo);
            Path treeObjPath = intermediate.resolve(rest);

            byte[] treeObj = Files.readAllBytes(treeObjPath);
            byte[] treeContent = removeHeader(treeObj);

            //parse tree object and add to map/recurse
            int pointer = 0;
            while (pointer < treeContent.length)
            {
                int start = pointer;
                
                //find null pointer
                while (treeContent[pointer] != 0)
                {
                    pointer++;
                }

                //get entry mode and pathname
                String entryFields = new String(Arrays.copyOfRange(treeContent, start, pointer));

                //parse entryfields
                String[] parts = entryFields.split(" ", 2);
                String mode = parts[0];
                String path = parts[1];
                Path absPath = curDir.resolve(path);

                //skip null pter
                pointer++;

                //get SHA 256 hash (32 bytes)
                byte[] hashBytes = Arrays.copyOfRange(treeContent, pointer, pointer + 32);
                String hashString = ByteHandler.hashToString(hashBytes).toString();

                pointer += 32;

                if (mode.equals("100644"))
                {
                    fileToHash.put(absPath, hashString);
                }
                
                else if (mode.equals("040000"))
                {
                    subTreeRecurse(absPath, hashString, projRootDir, fileToHash);
                }
            }
            
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
    }
}
