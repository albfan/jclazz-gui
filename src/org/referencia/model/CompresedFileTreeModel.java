package org.referencia.model;

import javax.swing.tree.DefaultTreeModel;
import java.io.File;

/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 *
 * Modelo de arbol para ficheros y archivos comprimidos
 * TODO: Para mas tipos comprimidos, usar Apache commons compress http://apache.rediris.es//commons/compress/binaries/commons-compress-1.2-bin.tar.gz
 *
 * User: alberto
 * Date: 24/09/11
 * Time: 1:42
 *
 * @author Doug Brown
 * @version 1.0
 */
public class CompresedFileTreeModel extends DefaultTreeModel {

    /**
     * Constructor.
     *
     * @param root a directory file
     */
    public CompresedFileTreeModel(File root) {
        super(new FileNode(root, true)); //TODO: comprobar que root es un directorio
        ((FileNode)getRoot()).buildChilds();
    }
}
