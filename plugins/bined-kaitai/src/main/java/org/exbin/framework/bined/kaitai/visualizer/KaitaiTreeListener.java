package org.exbin.framework.bined.kaitai.visualizer;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

@ParametersAreNonnullByDefault
public class KaitaiTreeListener implements TreeWillExpandListener, TreeSelectionListener {

    protected final DefaultTreeModel model;

    public KaitaiTreeListener(DefaultTreeModel model) {
        this.model = model;
    }

    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        TreePath path = event.getPath();
        if (path.getLastPathComponent() instanceof DataNode) {
            DataNode node = (DataNode) path.getLastPathComponent();
            node.explore(model, null);
        }
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
    }

    @Override
    public void valueChanged(TreeSelectionEvent event) {
        /* hexEditor.getSelectionModel().clearSelection();
            for (final TreePath path : tree.getSelectionPaths()) {
                final Object selected = path.getLastPathComponent();
                if (!(selected instanceof DataNode)) continue;

                final DataNode node = (DataNode)selected;
                final Integer start = node.posStart();
                final Integer end   = node.posEnd();
                if (start == null || end == null) continue;
                // Selection in nibbles, so multiply by 2
                hexEditor.getSelectionModel().addSelectionInterval(2*start, 2*end-1);
            } */
    }
}
