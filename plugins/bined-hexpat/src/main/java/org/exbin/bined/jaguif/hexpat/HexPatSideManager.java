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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import org.exbin.auxiliary.binary_data.BinaryData;
import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.auxiliary.binary_data.array.ByteArrayEditableData;
import org.exbin.bined.swing.CodeAreaCore;
import org.exbin.jaguif.App;
import org.exbin.bined.jaguif.document.BinaryFileDocument;
import org.exbin.bined.jaguif.hexpat.gui.HexPatSidePanel;
import org.exbin.bined.jaguif.hexpat.service.HexPatTreeListener;

/**
 * HexPat side manager.
 */
@NullMarked
public class HexPatSideManager {

    protected final Map<BinaryFileDocument, HexPatSideRecord> records = new HashMap<>();
    protected HexPatSidePanel sidePanel = new HexPatSidePanel();

    public HexPatSideManager() {
        sidePanel.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent event) {
                try {
                    BinedHexPatModule hexPatModule = App.getModule(BinedHexPatModule.class);
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    Object transferData = event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    List<?> droppedFiles = (List) transferData;
                    for (Object droppedFile : droppedFiles) {
                        File file = (File) droppedFile;
                        DefinitionRecord record = hexPatModule.getDefinitionByPath(file.toURI());
                        sidePanel.addDefinition(record);
                    }
                } catch (UnsupportedFlavorException | IOException ex) {
                    Logger.getLogger(HexPatSideManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    public HexPatSidePanel getSidePanel() {
        return sidePanel;
    }

    public void loadFrom(DefinitionRecord definitionRecord, BinaryData sourceData) {
        if (sourceData.isEmpty()) {
            return;
        }

        // TODO
//        if (parser == null || !definitionRecord.equals(parser.getDefinitionRecord())) {
//            sidePanel.addDefinition(definitionRecord);
//        }
    }

    public void addNodeSelectionListener(CodeAreaCore codeArea, HexPatTreeListener.SelectionListener listener) {
        for (Map.Entry<BinaryFileDocument, HexPatSideRecord> entry : records.entrySet()) {
            BinaryFileDocument key = entry.getKey();
            if (codeArea == key.getCodeArea()) {
                entry.getValue().addNodeSelectionListener(listener);
                break;
            }
        }
    }

    public void removeNodeSelectionListener(CodeAreaCore codeArea, HexPatTreeListener.SelectionListener listener) {
        for (Map.Entry<BinaryFileDocument, HexPatSideRecord> entry : records.entrySet()) {
            BinaryFileDocument key = entry.getKey();
            if (codeArea == key.getCodeArea()) {
                entry.getValue().removeNodeSelectionListener(listener);
                break;
            }
        }
    }
    
    @Nullable
    public HexPatSideRecord findRecord(CodeAreaCore codeArea) {
        for (Map.Entry<BinaryFileDocument, HexPatSideRecord> entry : records.entrySet()) {
            BinaryFileDocument key = entry.getKey();
            if (codeArea == key.getCodeArea()) {
                return entry.getValue();
            }
        }
        
        return null;
    }

    public HexPatSideRecord getRecord(BinaryFileDocument document) {
        HexPatSideRecord record = records.get(document);
        if (record == null) {
            record = new HexPatSideRecord();
            records.put(document, record);
        }
        
        return record;
    }

    public void switchToDocument(@Nullable BinaryFileDocument document) {
        if (document != null) {
            HexPatSideRecord record = records.get(document);
            if (record == null) {
                record = new HexPatSideRecord();
                sidePanel.setParserComponent(record.getParserComponent());
                DefinitionRecord definition = sidePanel.getSelectedDefinition();
                record.setDefinitionRecord(definition);
                BinaryData sourceData = document.getCodeArea().getContentData();
                if (sourceData instanceof EditableBinaryData) {
                    record.processDefinition((EditableBinaryData) sourceData, sidePanel);
                }
                records.put(document, record);
                return;
            } else if (record.getStatus() == HexPatStatusType.NO_FILE) {
                BinaryData sourceData = document.getCodeArea().getContentData();
                if (sourceData instanceof EditableBinaryData) {
                    record.processDefinition((EditableBinaryData) sourceData, sidePanel);
                }
            }

            int definitionIndex = sidePanel.getDefinitions().indexOf(record.getDefinitionRecord());
            if (definitionIndex >= 0) {
                sidePanel.setSelectedDefinition(definitionIndex);
            }
            sidePanel.setParserComponent(record.getParserComponent());
            sidePanel.setStatus(record.getStatus());
        } else {
            sidePanel.setParserComponent(null);
            sidePanel.setStatus(HexPatStatusType.NO_FILE);
        }
    }

    public void dropRecord(BinaryFileDocument document) {
        records.remove(document);
    }
}
