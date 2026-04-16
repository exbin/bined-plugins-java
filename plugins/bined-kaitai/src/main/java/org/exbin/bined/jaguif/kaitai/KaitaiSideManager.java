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
package org.exbin.bined.jaguif.kaitai;

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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.auxiliary.binary_data.BinaryData;
import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.auxiliary.binary_data.array.ByteArrayEditableData;
import org.exbin.bined.swing.CodeAreaCore;
import org.exbin.jaguif.App;
import org.exbin.bined.jaguif.component.BinaryFileDocument;
import org.exbin.bined.jaguif.kaitai.gui.KaitaiSidePanel;
import org.exbin.bined.jaguif.kaitai.service.KaitaiTreeListener;

/**
 * Kaitai side manager.
 */
@ParametersAreNonnullByDefault
public class KaitaiSideManager {

    protected final Map<BinaryFileDocument, KaitaiSideRecord> records = new HashMap<>();
    protected KaitaiSidePanel sidePanel = new KaitaiSidePanel();

    public KaitaiSideManager() {
        sidePanel.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent event) {
                try {
                    BinedKaitaiModule kaitaiModule = App.getModule(BinedKaitaiModule.class);
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    Object transferData = event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    List<?> droppedFiles = (List) transferData;
                    for (Object droppedFile : droppedFiles) {
                        File file = (File) droppedFile;
                        DefinitionRecord record = kaitaiModule.getDefinitionByPath(file.toURI());
                        sidePanel.addDefinition(record);
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

    public void loadFrom(DefinitionRecord definitionRecord, BinaryData sourceData) {
        if (sourceData.isEmpty()) {
            return;
        }

        // TODO
//        if (parser == null || !definitionRecord.equals(parser.getDefinitionRecord())) {
//            sidePanel.addDefinition(definitionRecord);
//        }
    }

    public void addNodeSelectionListener(CodeAreaCore codeArea, KaitaiTreeListener.SelectionListener listener) {
        for (Map.Entry<BinaryFileDocument, KaitaiSideRecord> entry : records.entrySet()) {
            BinaryFileDocument key = entry.getKey();
            if (codeArea == key.getCodeArea()) {
                entry.getValue().addNodeSelectionListener(listener);
                break;
            }
        }
    }

    public void removeNodeSelectionListener(CodeAreaCore codeArea, KaitaiTreeListener.SelectionListener listener) {
        for (Map.Entry<BinaryFileDocument, KaitaiSideRecord> entry : records.entrySet()) {
            BinaryFileDocument key = entry.getKey();
            if (codeArea == key.getCodeArea()) {
                entry.getValue().removeNodeSelectionListener(listener);
                break;
            }
        }
    }
    
    @Nullable
    public KaitaiSideRecord findRecord(CodeAreaCore codeArea) {
        for (Map.Entry<BinaryFileDocument, KaitaiSideRecord> entry : records.entrySet()) {
            BinaryFileDocument key = entry.getKey();
            if (codeArea == key.getCodeArea()) {
                return entry.getValue();
            }
        }
        
        return null;
    }

    @Nonnull
    public KaitaiSideRecord getRecord(BinaryFileDocument document) {
        KaitaiSideRecord record = records.get(document);
        if (record == null) {
            record = new KaitaiSideRecord();
            records.put(document, record);
        }
        
        return record;
    }

    public void switchToDocument(@Nullable BinaryFileDocument document) {
        if (document != null) {
            KaitaiSideRecord record = records.get(document);
            if (record == null) {
                record = new KaitaiSideRecord();
                sidePanel.setParserComponent(record.getParserComponent());
                DefinitionRecord definition = sidePanel.getSelectedDefinition();
                record.setDefinitionRecord(definition);
                BinaryData sourceData = document.getCodeArea().getContentData();
                if (sourceData instanceof EditableBinaryData) {
                    record.processDefinition((EditableBinaryData) sourceData, sidePanel);
                }
                records.put(document, record);
                return;
            } else if (record.getStatus() == KaitaiStatusType.NO_FILE) {
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
            sidePanel.setStatus(KaitaiStatusType.NO_FILE);
        }
    }

    public void dropRecord(BinaryFileDocument document) {
        records.remove(document);
    }
}
