package gurt.helperFunctions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

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
    //also tracks directories that are ancestors of tracked files for this commit
    public static void loadCommit(HashMap<Path, String> fileToHash, String hash, Path projRootDir, HashSet<Path> dirs)
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

            subTreeRecurse(projRootDir, treeRootHash, projRootDir, fileToHash, dirs);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }


    //recursively add files to map from subtree objects
    //takes in projRootDir (absolute path) and maps file abs path -> hash
    public static void subTreeRecurse(Path curDir, String treeHash, Path projRootDir, HashMap<Path, String> fileToHash, HashSet<Path> dirs)
    {
        try
        {
            Path dotGurt = projRootDir.resolve(".gurt");
            Path objectPath = dotGurt.resolve("objects");

            dirs.add(curDir);

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
                Path absPath = curDir.resolve(path).toAbsolutePath().normalize();

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
                    subTreeRecurse(absPath, hashString, projRootDir, fileToHash, dirs);
                }
            }
            
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
    }

    //helper for checkout; takes in projRootDir (absolute path) and a string for the commitHash
    //also reconstructs index
    public static void rebuildRepo(Path projRootDir, String commitHash)
    {
        try
        {
            Path dotGurtPath = projRootDir.resolve(".gurt");
            Path objectsPath = dotGurtPath.resolve("objects");
            
            HashMap<Path, String> fileToHash = new HashMap<>();
            HashSet<Path> dirs = new HashSet<>();
            loadCommit(fileToHash, commitHash, projRootDir, dirs);
            rebuildRepoHelper(projRootDir, projRootDir, fileToHash);

            //now build missing files, iterate thru hashmap
            Iterator<Path> it = fileToHash.keySet().iterator();
            while (it.hasNext())
            {
                Path curPath = it.next();
                if (Files.isDirectory(curPath))
                {
                    NIOHandler.deleteDirectoryRecursive(curPath, dotGurtPath);
                }
                if (!Files.exists(curPath))
                {
                    Files.createDirectories(curPath.getParent());
                    String blobHash = fileToHash.get(curPath);
                    String blobDirName = blobHash.substring(0,2);
                    String blobFileName = blobHash.substring(2);

                    Path blobDir = objectsPath.resolve(blobDirName);
                    Path blobFile = blobDir.resolve(blobFileName);

                    byte[] blob = Files.readAllBytes(blobFile);
                    byte[] blobContent = removeHeader(blob);

                    Files.write(curPath, blobContent);

                }
            }

            //rebuilding index:
            Path indexPath = dotGurtPath.resolve("index");
            HashMap<String, String> relPathToHash = new HashMap<>();
            ArrayList<String> relPathList = new ArrayList<>();

            Iterator<Path> it2 = fileToHash.keySet().iterator();
            while (it2.hasNext())
            {
                Path curPath = it2.next();
                Path relPath = projRootDir.relativize(curPath);
                relPathList.add(relPath.toString());

                String hashString = fileToHash.get(curPath);
                relPathToHash.put(relPath.toString(), hashString);
            }

            Collections.sort(relPathList);

            StringBuilder toWrite = new StringBuilder();
            for (String file : relPathList)
            {
                String hash = relPathToHash.get(file);
                toWrite.append(hash + " " + file + '\n');
            }

            Files.writeString(indexPath, toWrite.toString(), StandardCharsets.UTF_8);

            //delete nontracked directories
            deleteUntrackedDirs(projRootDir, dirs, projRootDir);

        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }

    //takes in fileToHash (file abs path -> blob hash string) and curDir (absolute path)
    //recurses thru repo and replaces files with associated blobs
    public static void rebuildRepoHelper(Path curDir, Path projRootDir, HashMap<Path, String> fileToHash)
    {
        Path dotGurtPath = projRootDir.resolve(".gurt");
        Path objectsPath = dotGurtPath.resolve("objects");
        if (curDir.startsWith(dotGurtPath))
        {
            return; 
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(curDir)) 
        {
            for (Path p : stream)
            {
                p = p.toAbsolutePath().normalize();
                if (Files.isDirectory(p))
                {
                    rebuildRepoHelper(p, projRootDir, fileToHash);
                }
                
                if (Files.isRegularFile(p))
                {
                    if (fileToHash.containsKey(p))
                    {
                        Files.createDirectories(p.getParent());
                        String blobHash = fileToHash.get(p);
                        String blobDirName = blobHash.substring(0,2);
                        String blobFileName = blobHash.substring(2);
                        
                        Path blobDir = objectsPath.resolve(blobDirName);
                        Path blobFile = blobDir.resolve(blobFileName);

                        byte[] blob = Files.readAllBytes(blobFile);
                        byte[] blobContent = removeHeader(blob);

                        Files.write(p, blobContent);
                    }

                    else
                    {
                        Files.delete(p);
                    }
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("repo rebuild error: " + e.getMessage());
        }
    }

    //helper for rebuild, takes in projRootDir(absolute path)
    //dirs consists of absolute normalized paths
    public static void deleteUntrackedDirs(Path projRootDir, HashSet<Path> dirs, Path curDir)
    {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(curDir)) 
        {
            for (Path p : stream)
            {
                if (p.startsWith(projRootDir.resolve(".gurt")))
                {
                    continue;
                }
                
                if (!p.startsWith(projRootDir)) 
                {
                    continue;             
                }

                p = p.toAbsolutePath().normalize();

                if (Files.isDirectory(p))
                {

                    if (dirs.contains(p))
                    {
                        deleteUntrackedDirs(projRootDir, dirs, p);
                    }
                    else
                    {
                        NIOHandler.deleteDirectoryRecursive(p, projRootDir.resolve(".gurt"));
                    }
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("delete untracked dirs error: " + e);
        }
    }
}
