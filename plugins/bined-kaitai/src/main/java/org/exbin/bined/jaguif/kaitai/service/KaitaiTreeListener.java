package org.exbin.bined.jaguif.kaitai.service;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import org.exbin.bined.jaguif.kaitai.KaitaiColorModifier;

/**
 * Kaitai parse tree listener.
 */
@ParametersAreNonnullByDefault
public class KaitaiTreeListener implements TreeWillExpandListener, TreeSelectionListener {

    protected final JTree tree;
    protected KaitaiColorModifier colorModifier = null;
    protected List<SelectionListener> nodeSelectionListeners = new ArrayList<>();

    public KaitaiTreeListener(JTree tree, KaitaiColorModifier colorModifier) {
        this.tree = tree;
        this.colorModifier = colorModifier;
    }

    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        TreePath path = event.getPath();
        if (path.getLastPathComponent() instanceof DataNode) {
            DataNode node = (DataNode) path.getLastPathComponent();
            node.explore(() -> {
                ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(node);
            }, null);

            for (SelectionListener nodeSelectionListener : nodeSelectionListeners) {
                nodeSelectionListener.selectionChanged();
            }
        }
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
    }

    @Override
    public void valueChanged(TreeSelectionEvent event) {
        for (SelectionListener nodeSelectionListener : nodeSelectionListeners) {
            nodeSelectionListener.selectionChanged();
        }

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

                Integer outerStart = null;
                DataNode parentNode = (DataNode) node.getParent();
                outerStart = parentNode.posStart();
                Integer outerEnd = parentNode.posEnd();
                if (outerEnd == null) {
                    outerStart = null;
                }
                if (outerStart != null) {
                    colorModifier.setOuterRange(outerStart, outerEnd - outerStart);
                } else {
                    colorModifier.clearOuterRange();
                }

                return;
            }
        }

        colorModifier.clearRange();
        colorModifier.clearOuterRange();
    }

    public void addNodeSelectionListener(SelectionListener listener) {
        nodeSelectionListeners.add(listener);
    }

    public void removeNodeSelectionListener(SelectionListener listener) {
        nodeSelectionListeners.remove(listener);
    }

    public interface SelectionListener {

        void selectionChanged();
    }
}
