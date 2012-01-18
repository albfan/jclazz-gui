package org.referencia.model;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;

/**
* Nodo que representa un archivo comprimido
* User: alberto
* Date: 28/11/11
* Time: 3:49
*/
public class CompressedNode extends DefaultMutableTreeNode {
    String name;

    /**
     * Constructor CompressedNode
     *
     * @param path
     */
    public CompressedNode(String path) {
        name = getName(path);
        if (name.equals("")) {
            name = getName(path.substring(0, path.length() - 1));
        }
    }

    public String toString() {
        return name;
    }

    /**
     * Gets the name from the specified path.
     *
     * @param path the full path
     * @return the name alone
     */
    public String getName(String path) {
        if (path == null) {
            return "";
        }
        // remove path
        int i = path.lastIndexOf("/");
        if (i == -1) {
            i = path.lastIndexOf("\\");
        }
        if (i != -1) {
            return path.substring(i + 1);
        }
        return path;
    }

    public String getFilePath() {
        DefaultMutableTreeNode node = this;
        while ((node = (DefaultMutableTreeNode) node.getParent()) instanceof CompressedNode);
        return ((FileNode)node).getFile().getAbsolutePath();
    }

    public String getCompressPath() {
        DefaultMutableTreeNode node = this;
        String fileComp = "";
        do {
            fileComp = node.toString() + (fileComp.length() > 0 ? File.separator + fileComp : "");
        } while ((node = (DefaultMutableTreeNode) node.getParent()) instanceof CompressedNode);
        return fileComp;
    }
}
