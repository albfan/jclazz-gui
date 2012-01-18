package org.referencia.model;

import javax.swing.tree.DefaultMutableTreeNode;
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
}
