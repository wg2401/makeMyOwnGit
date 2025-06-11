package gurt.helperFunctions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

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
            
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }
}
