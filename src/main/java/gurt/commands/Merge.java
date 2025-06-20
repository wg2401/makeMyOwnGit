package gurt.commands;

import gurt.helperFunctions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;


//   TODO:
//     • Implement GurtFileHandler.writeIndexFromMap(...)
//     • Implement GurtFileHandler.rebuildRepoFromMap(...)

  
public class Merge {

    public static void merge(Path projRootDir, String branch) 
    {
        try 
        {
            Path gurtDir     = projRootDir.resolve(".gurt");
            Path headPath    = gurtDir.resolve("HEAD");
            Path objectsDir  = gurtDir.resolve("objects");
            Path refsHeads   = gurtDir.resolve("refs").resolve("heads");

            //detached head case
            String headTxt = Files.readString(headPath).trim();
            if (!headTxt.startsWith("ref:")) 
            {
                System.out.println("fatal: HEAD is detached; check out a branch first");
                return;
            }

            //resolve ours
            Path oursRefPath = gurtDir.resolve(headTxt.substring(5).trim());
            if (!Files.exists(oursRefPath)) 
            {
                System.out.println("fatal: current branch ref missing");
                return;
            }
            String oursHash = Files.readString(oursRefPath).trim();

            //resolve theirs
            Path theirsRefPath = refsHeads;
            for (String part : branch.split("/"))
                theirsRefPath = theirsRefPath.resolve(part);

            if (!Files.exists(theirsRefPath)) 
            {
                System.out.println("fatal: branch '" + branch + "' not found");
                return;
            }
            String theirsHash = Files.readString(theirsRefPath).trim();

            //fast forward for ancestor case
            if (isAncestor(oursHash, theirsHash, gurtDir)) 
            {
                Map<Path, String> theirsTree = loadTree(theirsHash, projRootDir);
                GurtFileHandler.rebuildRepoFromMap(projRootDir, theirsTree);
                GurtFileHandler.writeIndexFromMap(gurtDir.resolve("index"), theirsTree);
                Files.writeString(oursRefPath, theirsHash + System.lineSeparator());
                System.out.println("Fast-forward to " + theirsHash.substring(0, 7));
                return;
            }
            if (isAncestor(theirsHash, oursHash, gurtDir)) 
            {
                System.out.println("Already up-to-date.");
                return;
            }

            //find minimal common ancestor 
            String baseHash = findMergeBase(oursHash, theirsHash, gurtDir);
            if (baseHash == null) 
            {
                System.out.println("fatal: criss-cross merge bases not supported yet");
                return;
            }

            //load the three trees
            Map<Path,String> baseMap   = loadTree(baseHash,   projRootDir);
            Map<Path,String> oursMap   = loadTree(oursHash,   projRootDir);
            Map<Path,String> theirsMap = loadTree(theirsHash, projRootDir);

            //three-way merge 
            Map<Path,String> resultMap = new HashMap<>();
            List<Path> conflicts       = new ArrayList<>();

            Set<Path> all = new HashSet<>();
            all.addAll(baseMap.keySet());
            all.addAll(oursMap.keySet());
            all.addAll(theirsMap.keySet());

            for (Path p : all) 
            {
                String b = baseMap.get(p);
                String o = oursMap.get(p);
                String t = theirsMap.get(p);

                if (Objects.equals(o, t)) 
                { 
                    // identical in ours & theirs
                    if (o != null)
                    {
                        resultMap.put(p, o);
                    }
                } 
                else if (Objects.equals(b, o)) 
                {
                    // changed only in theirs
                    if (t != null)
                    {
                        resultMap.put(p, t);
                    }
                } 
                else if (Objects.equals(b, t)) 
                {
                    // changed only in ours
                    if (o != null)
                    { 
                        resultMap.put(p, o);
                    }
                }
                else 
                {
                    // divergent change
                    conflicts.add(projRootDir.relativize(p));
                }
            }

            if (!conflicts.isEmpty()) 
            {
                System.out.println("Merge conflict(s):");
                for (Path p : conflicts) System.out.println("    " + p);
                System.out.println("Aborting merge.");
                return;  // leave work-tree untouched
            }

            //write new index & work-tree
            Path indexPath = gurtDir.resolve("index");
            GurtFileHandler.writeIndexFromMap(indexPath, resultMap);       // TODO
            GurtFileHandler.rebuildRepoFromMap(projRootDir, resultMap);    // TODO

            // create merge commit object
            String treeHash = WriteTree.writeTree(indexPath);

            String payload =
                "tree "   + treeHash   + "\n" +
                "parent " + oursHash   + "\n" +
                "parent " + theirsHash + "\n\n" +
                "Merge branch '" + branch + "'\n";

            byte[] header    = ("commit " + payload.getBytes(StandardCharsets.UTF_8).length + '\0').getBytes(StandardCharsets.UTF_8);
            byte[] commitObj = ByteHandler.combineTwoByteArrays(header, payload.getBytes(StandardCharsets.UTF_8));
            String commitHash = ByteHandler.bytesToHashedSB(commitObj).toString();

            Path objPath = objectsDir.resolve(commitHash.substring(0,2)).resolve(commitHash.substring(2));
            Files.createDirectories(objPath.getParent());
            Files.write(objPath, commitObj);

            //move current branch pointer & HEAD
            Files.writeString(oursRefPath, commitHash + System.lineSeparator());
            Checkout.checkoutHash(projRootDir, commitHash);  // rebuilds dir / index

            System.out.println("Merge made commit " + commitHash.substring(0,7));
        } 
        catch (IOException e) 
        {
            System.out.println("Merge failed: " + e.getMessage());
        }
    }

    // true if a is an ancestor of b 
    private static boolean isAncestor(String a, String b, Path gurtDir) throws IOException 
    {
        Deque<String> q = new ArrayDeque<>();
        q.push(b);
        while (!q.isEmpty()) 
        {
            String cur = q.pop();
            if (cur.equals(a)) 
            {
                return true;
            }
            
            q.addAll(GurtFileHandler.getParents(cur, gurtDir));
        }
        return false;
    }

    // returns minimal merge base, or null if criss-cross 
    private static String findMergeBase(String ours, String theirs, Path gurtDir) throws IOException 
    {
        Set<String> anc = new HashSet<>();
        Deque<String> st = new ArrayDeque<>();
        st.push(ours);
        while (!st.isEmpty()) 
        {
            String c = st.pop();
            if (anc.add(c))
            {
                st.addAll(GurtFileHandler.getParents(c, gurtDir));
            }
        }

        Set<String> lcas   = new HashSet<>();
        Set<String> above  = new HashSet<>();
        Deque<String> s2   = new ArrayDeque<>();
        Deque<String> td   = new ArrayDeque<>();
        s2.push(theirs);

        while (!s2.isEmpty()) 
        {
            String c = s2.pop();
            if (anc.contains(c)) 
            {
                lcas.add(c);
                td.addAll(GurtFileHandler.getParents(c, gurtDir));
                continue;
            }
            s2.addAll(GurtFileHandler.getParents(c, gurtDir));
        }
        while (!td.isEmpty()) 
        {
            String d = td.pop();
            if (above.add(d))
            {
                td.addAll(GurtFileHandler.getParents(d, gurtDir));
            }
        }
        lcas.removeAll(above);
        return (lcas.size() == 1) ? lcas.iterator().next() : null;
    }

    // loads (path → blob-hash) map for a given commit
    private static Map<Path,String> loadTree(String commitHash, Path projRoot) throws IOException 
    {
        HashMap<Path,String> m = new HashMap<>();
        GurtFileHandler.loadCommit(m, commitHash, projRoot, new HashSet<>());
        return m;
    }
}
