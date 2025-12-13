package org.exbin.framework.bined.kaitai.visualizer;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import org.exbin.framework.bined.kaitai.KaitaiColorModifier;

/**
 * Kaitai parse tree listener
 */
@ParametersAreNonnullByDefault
public class KaitaiTreeListener implements TreeWillExpandListener, TreeSelectionListener {

    protected final JTree tree;
    protected KaitaiColorModifier colorModifier = null;

    public KaitaiTreeListener(JTree tree, KaitaiColorModifier colorModifier) {
        this.tree = tree;
        this.colorModifier = colorModifier;
    }

    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        TreePath path = event.getPath();
        if (path.getLastPathComponent() instanceof DataNode) {
            DataNode node = (DataNode) path.getLastPathComponent();
            node.explore((DefaultTreeModel) tree.getModel(), null);
        }
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
    }

    @Override
    public void valueChanged(TreeSelectionEvent event) {
        TreePath[] selectionPaths = tree.getSelectionPaths();
        if (selectionPaths != null) {
            for (final TreePath path : selectionPaths) {
                final Object selected = path.getLastPathComponent();
                if (!(selected instanceof DataNode)) {
                    continue;
                }

                final DataNode node = (DataNode) selected;
                final Integer start = node.posStart();
                final Integer end = node.posEnd();
                if (start == null || end == null) {
                    continue;
                }
                colorModifier.setRange(start, end - start);
                return;
            }
        }

        colorModifier.clearRange();
    }
}
