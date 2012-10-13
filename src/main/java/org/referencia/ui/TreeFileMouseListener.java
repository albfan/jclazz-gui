package org.referencia.ui;

import org.referencia.model.CompressedNode;
import org.referencia.model.FileNode;
import org.referencia.ui.actions.IsolateAction;
import org.referencia.ui.actions.ReloadAction;
import org.referencia.ui.actions.SaveJarDecompiledAction;
import org.referencia.ui.actions.UpAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
* Created with IntelliJ IDEA.
* User: alberto
* Date: 4/08/12
* Time: 13:53
* To change this template use File | Settings | File Templates.
*/
class TreeFileMouseListener extends MouseAdapter {

    private FileVisor fileVisor;

    public TreeFileMouseListener(FileVisor fileVisor) {
        this.fileVisor = fileVisor;
    }
    @Override
    public void mousePressed(MouseEvent e) {
        final Object selectedNode = fileVisor.getFileTree().getLastSelectedPathComponent();
        if (selectedNode == null) {
            return;
        }
        if (e.isPopupTrigger()) {
            boolean showPopUp = false;
            JPopupMenu popup = new JPopupMenu("Acciones");
            if (selectedNode == fileVisor.getFileTree().getModel().getRoot()) {
                if (selectedNode instanceof FileNode) {
                    File file = ((FileNode)selectedNode).getFile();
                    if (file.isDirectory()) {
                        showPopUp = true;
                        popup.add(new UpAction(fileVisor, file));
                        popup.add(new ReloadAction(fileVisor, selectedNode));
                    }
                } else if (selectedNode instanceof CompressedNode) {

                }
            } else if (selectedNode instanceof FileNode) {
                File file = ((FileNode)selectedNode).getFile();
                if (file.isDirectory()) {
                    popup.add(new IsolateAction(fileVisor, file));
                } else if (file.getName().endsWith(".jar") || file.getName().endsWith(".apk")) {
                    popup.add(new SaveJarDecompiledAction(fileVisor, selectedNode));
                }
                showPopUp = true;
                //TODO: Acciones para ficheros normales, comparar etc...
            } else {
                CompressedNode compNode = (CompressedNode) selectedNode;
                final String filePath = compNode.getFilePath();
                if (filePath.endsWith(".jar")) {
                    popup.add(new AbstractAction("Mensaje") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            JOptionPane.showMessageDialog(fileVisor, filePath);
                        }
                    });
                }
            }
            if (showPopUp) {
                popup.show(fileVisor.getFileTree(), e.getX(), e.getY());
            }
        } else if (e.getClickCount() == 2) {
            if (selectedNode == fileVisor.getFileTree().getModel().getRoot()) {
                if (selectedNode instanceof FileNode) {
                    File file = ((FileNode)selectedNode).getFile();
                    if (file.isDirectory()) {
                        new UpAction(fileVisor, file).actionPerformed(null);
                    }
                }
            } else {
                if (selectedNode instanceof FileNode) {
                    File file = ((FileNode)selectedNode).getFile();
                    if (file.isDirectory()) {
                        new IsolateAction(fileVisor, file).actionPerformed(null);
                    }
                }
            }
        }
    }
}
