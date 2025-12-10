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

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.framework.bined.kaitai.gui.KaitaiSidePanel;
import org.exbin.framework.bined.kaitai.visualizer.KaitaiTreeListener;
import org.exbin.framework.bined.kaitai.visualizer.KaitaiVisualizer;

/**
 * Kaitai side manager.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class KaitaiSideManager {

    protected KaitaiSidePanel sidePanel = new KaitaiSidePanel();
    protected KaitaiVisualizer visualizer = new KaitaiVisualizer();
    protected JTree parserTree;
    protected String processingMessage = "";

    public KaitaiSideManager() {
        parserTree = sidePanel.getParserTree();
        DefaultTreeModel model = visualizer.getModel();
        parserTree.setModel(model);
        KaitaiTreeListener treeListener = new KaitaiTreeListener(model);
        parserTree.addTreeWillExpandListener(treeListener);
        parserTree.addTreeSelectionListener(treeListener);
        sidePanel.setStatus(KaitaiStatusType.NO_DEFINITION);
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
        sidePanel.setCurrentDef(definitionRecord.getFileName());
        sidePanel.clearParseTree();
        sidePanel.setStatus(KaitaiStatusType.PROCESSING);
        KaitaiVisualizer.CompileResult compileResult = visualizer.compileDefinition(definitionRecord);
        if (compileResult.getErrorMessage() != null) {
            processingMessage = compileResult.getErrorMessage();
            sidePanel.setStatus(KaitaiStatusType.COMPILE_FAILED);
            return;
        }

        KaitaiVisualizer.ParsingResult parsingResult = visualizer.parseData(compileResult.getKsyClass(), compileResult.getStreamClass(), compileResult.getParamNames(), sourceData);
        if (parsingResult.getErrorMessage() != null) {
            processingMessage = parsingResult.getErrorMessage();
            sidePanel.setStatus(KaitaiStatusType.PARSING_FAILED);
            return;
        }

        sidePanel.setStatus(KaitaiStatusType.OK);
    }
}
