package org.referencia.model;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

/**
 * Modelo de arbol para ficheros y archivos comprimidos
 * TODO: Para mas tipos comprimidos, usar Apache commons compress http://apache.rediris.es//commons/compress/binaries/commons-compress-1.2-bin.tar.gz
 * User: alberto
 * Date: 24/09/11
 * Time: 1:42
 */

/**
 * A tree model to display files and jar/zip contents.
 *
 * @author Doug Brown
 * @version 1.0
 */
public class CompresedFileTreeModel implements TreeModel {

    // instance fields
    protected File root;                                                                            // root must be a directory
    protected Map<File, CompressedNode[]> jarReaded = new HashMap<File, CompressedNode[]>();             // maps jar to CompressedNode[]
    protected Map<File, Map<String, CompressedNode>> pathInsideJar = new HashMap<File, Map<String, CompressedNode>>(); // maps jar to Map of relative path to CompressedNode

    /**
     * Constructor.
     *
     * @param root a directory file
     */
    public CompresedFileTreeModel(File root) {
        this.root = root;
    }

    /**
     * Gets the root of this tree model.
     *
     * @return the root file
     */
    public Object getRoot() {
        return root;
    }

    /**
     * Returns true if the specified node is a leaf.
     *
     * @param node the tree node
     * @return true if node is a leaf
     */
    public boolean isLeaf(Object node) {
        if (node instanceof File) {
            File file = (File) node;
            if (checkCompressExtension(file)) { //$NON-NLS-1$
                return false;
            }
            return ((File) node).isFile();
        } else if (node instanceof CompressedNode) {
            CompressedNode treeNode = (CompressedNode) node;
            return treeNode.isLeaf();
        }
        return true;
    }

    private boolean checkCompressExtension(File file) {
        return file.getName().endsWith(".jar") || file.getName().endsWith(".zip");
    }

    /**
     * Determines the number of child nodes for the specified node.
     *
     * @param parent the parent node
     * @return the number of child nodes
     */
    public int getChildCount(Object parent) {
        if (parent instanceof File) {
            File parentFile = (File) parent;
            if (checkCompressExtension(parentFile)) { //$NON-NLS-1$
                CompressedNode[] nodes = getCompressedNodes(parentFile);
                return (nodes == null) ? 0 : nodes.length;
            }
            String[] children = ((File) parent).list();
            return (children == null) ? 0 : children.length;
        } else if (parent instanceof CompressedNode) {
            CompressedNode treeNode = (CompressedNode) parent;
            return treeNode.getChildCount();
        }
        return 0;
    }

    /**
     * Gets the child node at a specified index. Parent and child may be a
     * File or CompressedNode.
     *
     * @param parent the parent node
     * @param index  the index
     * @return the child node
     */
    public Object getChild(Object parent, int index) {
        if (parent instanceof File) {
            File parentFile = (File) parent;
            // if parent is the launch jar, return a CompressedNode
            if (checkCompressExtension(parentFile)) { //$NON-NLS-1$
                CompressedNode[] nodes = getCompressedNodes(parentFile);
                if ((nodes != null) && (nodes.length > index)) {
                    return nodes[index];
                }
                return "no child found";                  //$NON-NLS-1$
            }
            String[] children = parentFile.list();
            if ((children == null) || (index >= children.length)) {
                return null;
            }
            return new File(parentFile, children[index]) {
                public String toString() {
                    return getName();
                }

            };
        } else if (parent instanceof CompressedNode) {
            CompressedNode treeNode = (CompressedNode) parent;
            return treeNode.getChildAt(index);
        }
        return null;
    }

    /**
     * Gets the index of the specified child node.
     *
     * @param parent the parent node
     * @param child  the child node
     * @return the index of the child
     */
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof File) {
            File parentFile = (File) parent;
            if (checkCompressExtension(parentFile)) { //$NON-NLS-1$
                CompressedNode[] nodes = getCompressedNodes(parentFile);
                if (nodes == null) {
                    return -1;
                }
                for (int i = 0; i < nodes.length; i++) {
                    if (nodes[i].equals(child)) {
                        return i;
                    }
                }
            }
            String[] children = ((File) parent).list();
            if (children == null) {
                return -1;
            }
            String childname = ((File) child).getName();
            for (int i = 0; i < children.length; i++) {
                if (childname.equals(children[i])) {
                    return i;
                }
            }
        } else if (parent instanceof CompressedNode) {
            CompressedNode treeNode = (CompressedNode) parent;
            return treeNode.getIndex((CompressedNode) child);
        }
        return -1;
    }

    // methods required by TreeModel
    // these methods are empty since this is not an editable model
    public void valueForPathChanged(TreePath path, Object newvalue) {
        /** empty method */
    }

    public void addTreeModelListener(TreeModelListener l) {
        /** empty method */
    }

    public void removeTreeModelListener(TreeModelListener l) {
        /** empty method */
    }

    /**
     * Gets a child node with a given name. Parent and child may be a
     * File or CompressedNode.
     *
     * @param parent the parent node
     * @param name   the name
     * @return the child node
     */
    public Object getChild(Object parent, String name) {
        if (parent instanceof File) {
            File parentFile = (File) parent;
            // if parent is the launch jar, return a CompressedNode
            if (checkCompressExtension(parentFile)) { //$NON-NLS-1$
                CompressedNode[] nodes = getCompressedNodes(parentFile);
                if (nodes != null) {
                    for (int i = 0; i < nodes.length; i++) {
                        if (nodes[i].toString().equals(name)) {
                            return nodes[i];
                        }
                    }
                }
            }
            String[] children = parentFile.list();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    if (children[i].toString().equals(name)) {
                        return new File(parentFile, children[i]) {
                            public String toString() {
                                return getName();
                            }

                        };
                    }
                }
            }
        } else if (parent instanceof CompressedNode) {
            CompressedNode treeNode = (CompressedNode) parent;
            Enumeration<?> e = treeNode.children();
            while (e.hasMoreElements()) {
                CompressedNode next = (CompressedNode) e.nextElement();
                if (next.toString().equals(name)) {
                    return next;
                }
            }
        }
        return null;
    }

    /**
     * Returns all descendant paths for a parent path.
     * Descendants include the parent path itself.
     *
     * @param parentPath the parent Object[] path
     * @return a collection of descendant Object[] paths
     */
    protected Collection<Object[]> getDescendantPaths(Object[] parentPath) {
        Collection<Object[]> c = new ArrayList<Object[]>();
        c.add(parentPath);
        Object parent = parentPath[parentPath.length - 1];
        int n = getChildCount(parent);
        for (int i = 0; i < n; i++) {
            Object child = getChild(parent, i);
            // construct new path by adding child
            Object[] childPath = new Object[parentPath.length + 1];
            System.arraycopy(parentPath, 0, childPath, 0, parentPath.length);
            childPath[parentPath.length] = child;
            Collection<Object[]> childPaths = getDescendantPaths(childPath);
            c.addAll(childPaths);
        }
        return c;
    }

    /**
     * Gets the name from the specified path.
     *
     * @param path the full path
     * @return the name alone
     */
    public static String getName(String path) {
        if (path == null) {
            return ""; //$NON-NLS-1$
        }
        // remove path
        int i = path.lastIndexOf("/"); //$NON-NLS-1$
        if (i == -1) {
            i = path.lastIndexOf("\\"); //$NON-NLS-1$
        }
        if (i != -1) {
            return path.substring(i + 1);
        }
        return path;
    }

    /**
     * returns the CompressedNode associated with a path in a given jar file
     * @param jarFile
     * @param path
     * @return
     */
    public CompressedNode getCompressedNode(File jarFile, String path) {
        Map<String, CompressedNode> pathMap = pathInsideJar.get(jarFile);
        if (pathMap == null) {
            readJar(jarFile);
            pathMap = pathInsideJar.get(jarFile);
        }
        return pathMap.get(path);
    }

    /**
     * returns the top-level CompressedNodes in the specified jar
     * @param jarFile
     * @return
     */
    public CompressedNode[] getCompressedNodes(File jarFile) {
        //TODO: Igual esto tiene sentido con un sha1 por si cambia de contenido
        CompressedNode[] array = jarReaded.get(jarFile);
        if (array == null) {
            readJar(jarFile);
            array = jarReaded.get(jarFile);
        }
        return array;
    }

    /**
     * Replaces backslashes with slashes.
     *
     * @param path the path
     * @return the path with forward slashes
     */
    public static String forwardSlash(String path) {
        if (path == null) {
            return ""; //$NON-NLS-1$
        }
        int i = path.indexOf("\\"); //$NON-NLS-1$
        while (i != -1) {
            path = path.substring(0, i) + "/" + path.substring(i + 1); //$NON-NLS-1$
            i = path.indexOf("\\");                              //$NON-NLS-1$
        }
        return path;
    }

    /**
     * reads the specified jar
     */
    private void readJar(File jarFile) {
        // get all jar entries
        Collection<String> entries = getJarEntries(jarFile);
        ArrayList<CompressedNode> topLevelNodes = new ArrayList<CompressedNode>();
        Map<String, CompressedNode> nodes = new HashMap<String, CompressedNode>(); // maps String to CompressedNode
        for (Iterator<String> it = entries.iterator(); it.hasNext(); ) {
            String path = forwardSlash(it.next());
            // don't include meta-inf files
            if (path.startsWith("META-INF")) { //$NON-NLS-1$
                continue;
            }
            // for each path, make and connect CompressedNodes in the path
            CompressedNode parent = null;
            String parentPath = "";           //$NON-NLS-1$
            while (path != null) {
                int n = path.indexOf("/");      //$NON-NLS-1$
                if (n > -1) {                      // found directory
                    parentPath += path.substring(0, n + 1);
                    CompressedNode node = nodes.get(parentPath);
                    if (node == null) {
                        node = new CompressedNode(parentPath);
                        nodes.put(parentPath, node);
                        if (parent != null) {
                            parent.add(node);
                        } else {                    // this is a top level node
                            topLevelNodes.add(node);
                        }
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
                        if (parent != null) {
                            parent.add(node);
                        } else {                    // this is a top level node
                            topLevelNodes.add(node);
                        }
                    }
                    path = null;
                }
            }
        }
        CompressedNode[] array = topLevelNodes.toArray(new CompressedNode[0]);
        jarReaded.put(jarFile, array);
        pathInsideJar.put(jarFile, nodes);
    }

    /**
     * returns all JarEntries in the specified jar
     */
    private Collection<String> getJarEntries(File jarFile) {
        // create a JarFile
        JarFile jar = null;
        try {
            jar = new JarFile(jarFile);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (jar != null) {
            TreeSet<String> entries = new TreeSet<String>();
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                JarEntry entry = e.nextElement();
                entries.add(entry.getName());
            }
            return entries;
        }
        return null;
    }
}
