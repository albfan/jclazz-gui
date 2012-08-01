package org.referencia.model;

import org.referencia.model.jar.JarEntryOutputStream;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by IntelliJ IDEA.
 * User: alberto
 * Date: 18/01/12
 * Time: 1:29
 * To change this template use File | Settings | File Templates.
 */
public class JarUtils {

    //TODO: Mejorar la lectura de jar y añadir los nodos de variables, y metodos (con el posicionamiento al seleccionar)

    /**
     * reads the specified jar
     */
    public static void readJar(FileNode fileNode) {
        // get all jar entries
        Collection<String> entries = getJarEntries(fileNode);
        Map<String, CompressedNode> nodes = new HashMap<String, CompressedNode>(); // maps String to CompressedNode
        for (Iterator<String> it = entries.iterator(); it.hasNext(); ) {
            String path = forwardSlash(it.next());
            // don't include meta-inf files
            if (path.startsWith("META-INF")) {
                continue;
            }
            // for each path, make and connect CompressedNodes in the path
            DefaultMutableTreeNode parent = fileNode;
            String parentPath = "";
            while (path != null) {
                int n = path.indexOf("/");
                if (n > -1) {                      // found directory
                    parentPath += path.substring(0, n + 1);
                    CompressedNode node = nodes.get(parentPath);
                    if (node == null) {
                        node = new CompressedNode(parentPath);
                        nodes.put(parentPath, node);
                        parent.add(node);
                    }
                    path = path.substring(n + 1);
                    parent = node;
                } else {                        // found file
                    //TODO: Si un zip tiene dentro un jar habría que poder verlo
                    path = parentPath + path;
                    CompressedNode node = nodes.get(path);
                    if (node == null) {
                        node = new CompressedNode(path);
                        nodes.put(path, node);
                        parent.add(node);
                    }
                    path = null;
                }
            }
        }
    }

    public static Collection<String> getJarEntries(FileNode fileNode) {
        try {
            JarFile jar = new JarFile(fileNode.getFile());
            TreeSet<String> entries = new TreeSet<String>();
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                JarEntry entry = e.nextElement();
                entries.add(entry.getName());
            }
            return entries;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Replaces backslashes with slashes.
     *
     * @param path the path
     * @return the path with forward slashes
     */
    public static String forwardSlash(String path) {
        if (path == null) {
            return "";
        }
        int i = path.indexOf("\\");
        while (i != -1) {
            path = path.substring(0, i) + "/" + path.substring(i + 1);
            i = path.indexOf("\\");
        }
        return path;
    }

    public static boolean isCompressFile(String fileName) {
        return fileName.endsWith(".jar") || fileName.endsWith(".zip") || fileName.endsWith(".apk");
    }

    /*

    URL url = new URL("jar:file:/c://my.jar!/");
    JarURLConnection conn = (JarURLConnection) url.openConnection();

    JarEntry jarEntry = conn.getJarEntry();

    */

    public static final String JAR_DELIMETER = "/";

    /**
     * Returns a list of entries that are
     * immediately below the entry named by entryName in the jar's directory
     * structure.
     *
     * @param entryName the name of the directory entry name
     * @return List a list of java.util.jar.JarEntry objects that are
     * immediately below the entry named by entryName in the jar's directory
     * structure.
     */
    public static List listSubEntries(JarFile jar, String entryName) {
        Enumeration entries = jar.entries();
        List subEntries = new ArrayList();

        while(entries.hasMoreElements()) {
            JarEntry nextEntry = (JarEntry) entries.nextElement();

            if (nextEntry.getName().startsWith(entryName)) {
                // the next entry name starts with the entryName so it
                // is a potential sub entry

                // tokenize the rest of the next entry name to see how
                // many tokens exist
                StringTokenizer tokenizer = new StringTokenizer(nextEntry.getName().substring(entryName.length()), JAR_DELIMETER);

                if (tokenizer.countTokens() == 1) {
                    // only 1 token exists, so it is a sub-entry
                    subEntries.add(nextEntry);
                }
            }
        }

        return subEntries;
    }

    /**
     * Removes the given entry from the jar.  If the entry does not exist,
     * the method returns without doing anything.
     *
     * @param entry entry to be removed
     * @throws java.io.IOException if there is a problem writing the changes
     * to the jar
     */
    public static void removeEntry(JarFile jar, JarEntry entry) throws IOException {
        // opens an output stream and closes it without writing anything to it
        if (entry != null && jar.getEntry(entry.getName()) != null) {
            JarEntryOutputStream outputStream = new JarEntryOutputStream(jar, entry.getName());

            outputStream.close();
        }
    }

    /**
     * Utility method used to swap the underlying jar file out for the new one.
     * This method closes the old jar file, deletes it, moves the new jar
     * file to the location where the old one used to be and opens it.
     *
     * This is used when modifying the jar (removal, addition, or changes
     * of entries)
     *
     * @param newJarFile the file object pointing to the new jar file
     */
    public static void swapJars(JarFile jar, File newJarFile) throws IOException {
        File oldJarFile = new File(jar.getName());
        jar.close();
        oldJarFile.delete();
        if (newJarFile.renameTo(oldJarFile)) {
            new JarFile(oldJarFile);      //TODO: No se para que hace esto
        } else {
            throw new IOException();
        }
    }
}
