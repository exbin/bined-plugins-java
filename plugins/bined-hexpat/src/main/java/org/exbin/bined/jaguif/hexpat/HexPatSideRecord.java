/*
 * Copyright (C) ExBin Project, https://exbin.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exbin.bined.jaguif.hexpat;

import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.jaguif.App;
import org.exbin.bined.jaguif.hexpat.gui.HexPatSidePanel;
import org.exbin.bined.jaguif.hexpat.gui.ParserTreeComponent;
import org.exbin.bined.jaguif.hexpat.service.HexPatTreeListener;
import org.exbin.bined.jaguif.hexpat.service.HexPatProcessingService;

/**
 * Kaitai side record.
 */
@NullMarked
public class HexPatSideRecord {

    protected ParserTreeComponent parserComponent = new ParserTreeComponent();
    protected HexPatProcessingService visualizer = new HexPatProcessingService();
    protected HexPatTreeListener treeListener;

    protected DefinitionRecord definitionRecord;
    protected HexPatStatusType status = HexPatStatusType.NO_FILE;
    protected String processingMessage = "";

    public HexPatSideRecord() {
        JTree parserTree = parserComponent.getParserTree();
        DefaultTreeModel model = visualizer.getModel();
        parserTree.setModel(model);
        BinedHexPatModule hexPatModule = App.getModule(BinedHexPatModule.class);
        treeListener = new HexPatTreeListener(parserTree, hexPatModule.getHexPatColorModifier());
        parserTree.addTreeWillExpandListener(treeListener);
        parserTree.addTreeSelectionListener(treeListener);
    }

    public void processDefinition(EditableBinaryData sourceData, HexPatSidePanel sidePanel) {
        clearParseTree();
        // TODO
        updateStatus(sidePanel, HexPatStatusType.OK);
    }

    public void addNodeSelectionListener(HexPatTreeListener.SelectionListener listener) {
        treeListener.addNodeSelectionListener(listener);
    }

    public void removeNodeSelectionListener(HexPatTreeListener.SelectionListener listener) {
        treeListener.removeNodeSelectionListener(listener);
    }

    @Nullable
    public Object getSelectedNode() {
        TreePath treePath = parserComponent.getParserTree().getSelectionPath();
        return treePath == null ? null : treePath.getLastPathComponent();
    }

    private void clearParseTree() {
        DefaultTreeModel model = visualizer.getModel();
        model.setRoot(null);
    }

    @Nullable
    public DefinitionRecord getDefinitionRecord() {
        return definitionRecord;
    }

    public void setDefinitionRecord(DefinitionRecord definitionRecord) {
        this.definitionRecord = definitionRecord;
    }

    public ParserTreeComponent getParserComponent() {
        return parserComponent;
    }
    
    public void updateStatus(HexPatSidePanel sidePanel, HexPatStatusType status) {
        this.status = status;
        sidePanel.setStatus(status);
    }

    public HexPatStatusType getStatus() {
        return status;
    }

    public String getProcessingMessage() {
        return processingMessage;
    }
}
