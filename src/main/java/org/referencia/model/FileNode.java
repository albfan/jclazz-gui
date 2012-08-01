package org.referencia.model;

import org.referencia.config.Config;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

/**
 * Nodo para ficheros normales
 * User: alberto
 * Date: 18/01/12
 * Time: 0:41
 */
public class FileNode extends DefaultMutableTreeNode {
    private boolean showFullPath;

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    private boolean loaded;

    public FileNode(File file) {
        this(file, false);
    }

    public FileNode(File file, boolean showFullPath) {
        super(file);
        this.showFullPath = showFullPath;
    }

    @Override
    public boolean isLeaf() {
        return super.isLeaf() && getFile().isFile() && !isCompressedFile();
    }

    public void buildChilds() {
        if(isCompressedFile()) {
            JarUtils.readJar(this);
        } else {
            removeAllChildren();
            File[] children = getFile().listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return Config.getInstance().isShowHiddenFiles() || !file.isHidden();
                }
            });
            if (children !=null) {
                Arrays.sort(children, Config.getInstance().getFileSorter());
                for (int i = 0; i < children.length; i++) {
                    File child = children[i];
                    add(new FileNode(child));
                }
            }
            setLoaded(true);
        }
    }

    private boolean isCompressedFile() {
        return JarUtils.isCompressFile(getFile().getName());
    }

    public File getFile() {
        return (File) getUserObject();
    }

    @Override
    public String toString() {
        return showFullPath ? getFile().getPath() : getFile().getName();
    }
}
