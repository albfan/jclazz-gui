package org.referencia.ui;

import org.referencia.config.Config;
import org.referencia.model.CompresedFileTreeModel;
import org.referencia.model.CompressedNode;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.io.*;
import java.nio.charset.Charset;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Visor de archivos normales y comprimidos
 * User: alberto
 * Date: 24/09/11
 * Time: 1:27
 */
public class FileVisor extends JFrame {
    private JTree fileTree;

    private JTextArea fileDetailsTextArea = new JTextArea();

    public FileVisor(String directory) {
        super(Config.getInstance().getMessage("fileVisor.title"));
        buildGUI(directory);
    }

    private void buildGUI(String directory) {
        fileDetailsTextArea.setEditable(false);
        fileTree = new JTree(buildTreeModel(directory));
        fileTree.setEditable(true);
        fileTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent event) {
                Object selectedNode = fileTree.getLastSelectedPathComponent();
                if (selectedNode instanceof File) {
                    File file = (File) selectedNode;
                    fileDetailsTextArea.setText(getFileDetails(file));
                } else {
                    CompressedNode compNode = (CompressedNode) selectedNode;
                    fileDetailsTextArea.setText(getCompressedDetails(compNode, event));
                }
            }
        });
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(
                fileTree), new JScrollPane(fileDetailsTextArea));
        getContentPane().add(splitPane);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(640, 480);
        setVisible(true);
    }

    private String getCompressedDetails(CompressedNode compNode, TreeSelectionEvent event) {
        String compressedFile = compNode.toString();
//        new JarInputStream()
        CompressedNode node = compNode;
        int i = 1;
        String fileComp = "";
        TreePath path = event.getPath();
        do{
            fileComp = (path.getPathComponent(path.getPathCount() - (i++))).toString() + (fileComp.length() > 0 ? File.separator+ fileComp: "");
        } while ((node = (CompressedNode) node.getParent()) != null);
        File file = (File) path.getPathComponent(path.getPathCount() - i);
        StringBuffer sb = new StringBuffer();
        sb.append("File: " + compressedFile);
        //TODO: Añadir propiedades
        sb.append("Prop: " + Config.getInstance().getMessage("fileVisor.compressedFile")+"\n");
        sb.append("Contents:\n");
        try {
            JarInputStream jarInput = new JarInputStream(new FileInputStream(file));
            JarEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = jarInput.getNextJarEntry()) != null) {
                if (entry.getName().equals(fileComp)) {
                    //TODO: Si el archivo leido es un jar tambien se podría leer para extraer en contenido, etc...
                    while (jarInput.read(buffer) != -1) {
                        sb.append(new String(buffer, Charset.defaultCharset()));
                    }
                }
            }
//            readContents(sb, jarInput);
        } catch (Exception e) {
        }
        return sb.toString();
    }

    private TreeModel buildTreeModel(String directory) {
        return new CompresedFileTreeModel(new File(directory));
//        return new FileSystemModel(new File(directory));
    }

    private String getFileDetails(File file) {
        if (file == null)
            return "";
        StringBuffer buffer = new StringBuffer();
        buffer.append("Name: " + file.getName() + "\n");
        buffer.append("Path: " + file.getPath() + "\n");
        buffer.append("Size: " + file.length() + "\n");
        try {
            if (file.getName().endsWith("jar") || file.getName().endsWith("zip") ) {
                buffer.append("Contents: Archivo comprimido");
            } else {
                readContents(buffer, new FileInputStream(file));
            }
        } catch (FileNotFoundException e) {
        }
        return buffer.toString();
    }

    private void readContents(StringBuffer buffer, InputStream fileInputStream) {
        buffer.append("Contents:\n");
        try {
            InputStream input = fileInputStream;

            byte[] fileData = new byte[input.available()];

            input.read(fileData);
            input.close();
            buffer.append(new String(fileData, Charset.defaultCharset()));
        } catch (IOException e) {
            buffer.append("error reading: " + e.getMessage());
        }
    }
}

