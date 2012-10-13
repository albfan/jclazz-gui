package org.referencia.ui.actions;

import org.referencia.model.FileNode;
import org.referencia.ui.FileVisor;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.event.ActionEvent;

/**
* Recarga de archivos
* User: alberto
* Date: 4/08/12
* Time: 14:02
*/
public class ReloadAction extends AbstractAction {
    private final Object selectedNode;
    private FileVisor fileVisor;

    public ReloadAction(FileVisor fileVisor, Object selectedNode) {
        super("Recargar");
        this.selectedNode = selectedNode;
        this.fileVisor = fileVisor;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //TODO: Se podría llamar también al suceder un cambio en el sistema (haría falta un filesystem)
        if ((TreeNode) selectedNode instanceof FileNode) {
            ((FileNode) (TreeNode) selectedNode).buildChilds();
        }
        ((DefaultTreeModel) fileVisor.getFileTree().getModel()).nodeStructureChanged((TreeNode) selectedNode);
    }

}
