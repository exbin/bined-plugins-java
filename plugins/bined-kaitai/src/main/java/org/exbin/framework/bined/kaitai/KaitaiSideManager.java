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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.exbin.framework.bined.kaitai.service.KaitaiCompiler;
import org.exbin.framework.bined.kaitai.service.KaitaiParser;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.framework.App;
import org.exbin.framework.bined.kaitai.gui.KaitaiSidePanel;
import org.exbin.framework.bined.kaitai.service.KaitaiTreeListener;
import org.exbin.framework.bined.kaitai.service.KaitaiProcessingService;

/**
 * Kaitai side manager.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class KaitaiSideManager {

    protected KaitaiSidePanel sidePanel = new KaitaiSidePanel();
    protected KaitaiProcessingService visualizer = new KaitaiProcessingService();
    protected KaitaiCompiler compiler = new KaitaiCompiler();
    protected KaitaiParser parser = null;
    protected JTree parserTree;
    protected String processingMessage = "";

    public KaitaiSideManager() {
        parserTree = sidePanel.getParserTree();
        DefaultTreeModel model = visualizer.getModel();
        parserTree.setModel(model);
        BinedKaitaiModule kaitaiModule = App.getModule(BinedKaitaiModule.class);
        KaitaiTreeListener treeListener = new KaitaiTreeListener(parserTree, kaitaiModule.getKaitaiColorModifier());
        parserTree.addTreeWillExpandListener(treeListener);
        parserTree.addTreeSelectionListener(treeListener);
        sidePanel.setStatus(KaitaiStatusType.NO_DEFINITION);
        sidePanel.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent event) {
                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    Object transferData = event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    List<?> droppedFiles = (List) transferData;
                    for (Object droppedFile : droppedFiles) {
//                        sidePanel.addDefinition();
//                        openFile(((File) file).toURI(), null);
                    }
                } catch (UnsupportedFlavorException | IOException ex) {
                    Logger.getLogger(KaitaiSideManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    @Nonnull
    public KaitaiSidePanel getSidePanel() {
        return sidePanel;
    }

    @Nonnull
    public String getProcessingMessage() {
        return processingMessage;
    }

    public void loadFrom(DefinitionRecord definitionRecord, EditableBinaryData sourceData) {
        if (sourceData.isEmpty()) {
            return;
        }

        if (parser == null || !definitionRecord.equals(parser.getDefinitionRecord())) {
            sidePanel.addDefinition(definitionRecord);
        }
    }
    
    public void processDefinition(DefinitionRecord definitionRecord, EditableBinaryData sourceData) {
        clearParseTree();
        sidePanel.setStatus(KaitaiStatusType.COMPILING);
        KaitaiCompiler.CompileResult compileResult = visualizer.compileDefinition(compiler, definitionRecord);
        if (compileResult.getErrorMessage() != null) {
            processingMessage += compileResult.getErrorMessage();
            sidePanel.setStatus(KaitaiStatusType.COMPILE_FAILED);
            this.parser = null;
            return;
        }
        this.parser = compileResult.getParser();

        sidePanel.setStatus(KaitaiStatusType.PARSING);
        KaitaiParser.ParsingResult parsingResult = visualizer.parseData(parser, sourceData);
        if (parsingResult.getErrorMessage() != null) {
            clearParseTree();
            processingMessage += parsingResult.getErrorMessage();
            sidePanel.setStatus(KaitaiStatusType.PARSE_FAILED);
            return;
        }

        sidePanel.setStatus(KaitaiStatusType.OK);
    }

    private void clearParseTree() {
        DefaultTreeModel model = visualizer.getModel();
        model.setRoot(null);
    }
}
