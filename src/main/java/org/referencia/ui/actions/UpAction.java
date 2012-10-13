package org.referencia.ui.actions;

import org.referencia.ui.FileVisor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
* Created with IntelliJ IDEA.
* User: alberto
* Date: 4/08/12
* Time: 14:00
* To change this template use File | Settings | File Templates.
*/
public class UpAction extends AbstractAction {
    private final File file;
    private FileVisor fileVisor;

    public UpAction(FileVisor fileVisor, File file) {
        super("Subir");
        this.fileVisor = fileVisor;
        this.file = file;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String absolutePath = file.getAbsolutePath();
        int endIndex = absolutePath.lastIndexOf(System.getProperty("file.separator"));
        String upperDir = absolutePath.substring(0
                , absolutePath.lastIndexOf(System.getProperty("file.separator"), endIndex));
        fileVisor.getFileTree().setModel(fileVisor.buildTreeModel(upperDir));
    }
}
