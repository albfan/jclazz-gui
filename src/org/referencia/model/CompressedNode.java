package org.referencia.model;

import javax.swing.tree.DefaultMutableTreeNode;

/**
* Created by IntelliJ IDEA.
* User: alberto
* Date: 28/11/11
* Time: 3:49
*/ // CompressedNode class represents a compressed file in a jar
public class CompressedNode extends DefaultMutableTreeNode {
    String name;

    /**
     * Constructor CompressedNode
     *
     * @param path
     */
    public CompressedNode(String path) {
        name = CompresedFileTreeModel.getName(path);
        if (name.equals("")) { //$NON-NLS-1$
            name = CompresedFileTreeModel.getName(path.substring(0, path.length() - 1));
        }
    }

    public String toString() {
        return name;
    }

}
