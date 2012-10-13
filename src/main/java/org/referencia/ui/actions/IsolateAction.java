package org.referencia.ui.actions;

import org.referencia.ui.FileVisor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
* Created with IntelliJ IDEA.
* User: alberto
* Date: 4/08/12
* Time: 14:02
* To change this template use File | Settings | File Templates.
*/
public class IsolateAction extends AbstractAction {
    private final File file;
    private FileVisor fileVisor;

    public IsolateAction(FileVisor fileVisor, File file) {
        super("Aislar");
        this.fileVisor = fileVisor;
        this.file = file;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        fileVisor.getFileTree().setModel(fileVisor.buildTreeModel(file.getAbsolutePath()));
    }
}
