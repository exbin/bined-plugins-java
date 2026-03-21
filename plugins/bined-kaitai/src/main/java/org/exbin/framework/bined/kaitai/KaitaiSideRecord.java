/*
 * Copyright (C) ExBin Project
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
package org.exbin.framework.bined.kaitai;

import org.exbin.framework.bined.kaitai.service.KaitaiCompiler;
import org.exbin.framework.bined.kaitai.service.KaitaiParser;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.framework.App;
import org.exbin.framework.bined.kaitai.gui.KaitaiSidePanel;
import org.exbin.framework.bined.kaitai.gui.ParserTreeComponent;
import org.exbin.framework.bined.kaitai.service.KaitaiTreeListener;
import org.exbin.framework.bined.kaitai.service.KaitaiProcessingService;

/**
 * Kaitai side record.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class KaitaiSideRecord {

    protected ParserTreeComponent parserComponent = new ParserTreeComponent();
    protected KaitaiProcessingService visualizer = new KaitaiProcessingService();
    protected KaitaiCompiler compiler = new KaitaiCompiler();
    protected KaitaiParser parser = null;
    protected KaitaiTreeListener treeListener;

    protected DefinitionRecord definitionRecord;
    protected KaitaiStatusType status = KaitaiStatusType.NO_FILE;
    protected String processingMessage = "";

    public KaitaiSideRecord() {
        JTree parserTree = parserComponent.getParserTree();
        DefaultTreeModel model = visualizer.getModel();
        parserTree.setModel(model);
        BinedKaitaiModule kaitaiModule = App.getModule(BinedKaitaiModule.class);
        treeListener = new KaitaiTreeListener(parserTree, kaitaiModule.getKaitaiColorModifier());
        parserTree.addTreeWillExpandListener(treeListener);
        parserTree.addTreeSelectionListener(treeListener);
    }

    public void processDefinition(EditableBinaryData sourceData, KaitaiSidePanel sidePanel) {
        clearParseTree();
        updateStatus(sidePanel, KaitaiStatusType.COMPILING);
        KaitaiCompiler.CompileResult compileResult = visualizer.compileDefinition(compiler, definitionRecord);
        if (compileResult.getErrorMessage() != null) {
            processingMessage += compileResult.getErrorMessage();
            updateStatus(sidePanel, KaitaiStatusType.COMPILE_FAILED);
            this.parser = null;
            return;
        }
        this.parser = compileResult.getParser();

        updateStatus(sidePanel, KaitaiStatusType.PARSING);
        KaitaiParser.ParsingResult parsingResult = visualizer.parseData(parser, sourceData);
        if (parsingResult.getErrorMessage() != null) {
            clearParseTree();
            processingMessage += parsingResult.getErrorMessage();
            updateStatus(sidePanel, KaitaiStatusType.PARSE_FAILED);
            return;
        }

        updateStatus(sidePanel, KaitaiStatusType.OK);
    }

    public void addNodeSelectionListener(KaitaiTreeListener.SelectionListener listener) {
        treeListener.addNodeSelectionListener(listener);
    }

    public void removeNodeSelectionListener(KaitaiTreeListener.SelectionListener listener) {
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

    @Nonnull
    public ParserTreeComponent getParserComponent() {
        return parserComponent;
    }
    
    public void updateStatus(KaitaiSidePanel sidePanel, KaitaiStatusType status) {
        this.status = status;
        sidePanel.setStatus(status);
    }

    @Nonnull
    public KaitaiStatusType getStatus() {
        return status;
    }

    @Nonnull
    public String getProcessingMessage() {
        return processingMessage;
    }
}
