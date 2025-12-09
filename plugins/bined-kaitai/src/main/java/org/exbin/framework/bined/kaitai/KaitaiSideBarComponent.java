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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.auxiliary.binary_data.array.ByteArrayEditableData;
import org.exbin.framework.App;
import org.exbin.framework.action.api.ContextComponent;
import org.exbin.framework.bined.BinaryDataComponent;
import org.exbin.framework.bined.kaitai.gui.KaitaiDefinitionsPanel;
import org.exbin.framework.bined.kaitai.gui.KaitaiSidePanel;
import org.exbin.framework.bined.kaitai.gui.KaitaiStatusPanel;
import org.exbin.framework.sidebar.api.AbstractSideBarComponent;
import org.exbin.framework.window.api.WindowHandler;
import org.exbin.framework.window.api.WindowModuleApi;
import org.exbin.framework.window.api.controller.DefaultControlController;
import org.exbin.framework.window.api.gui.CloseControlPanel;
import org.exbin.framework.window.api.gui.DefaultControlPanel;

/**
 * Kaitai sidebar component.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class KaitaiSideBarComponent extends AbstractSideBarComponent {

    protected KaitaiSideManager sideManager = new KaitaiSideManager();
    protected DefinitionRecord definitionRecord = null;
    protected BinaryDataComponent binaryDataComponent = null;

    @Override
    public JComponent createComponent() {
        KaitaiSidePanel sidePanel = sideManager.getSidePanel();
        sidePanel.setController(new KaitaiSidePanel.Controller() {
            @Override
            public void manageDefinitions() {
                WindowModuleApi windowModule = App.getModule(WindowModuleApi.class);
                final KaitaiDefinitionsPanel definitionsPanel = new KaitaiDefinitionsPanel();
                DefaultMutableTreeNode formatsRootNode = new DefaultMutableTreeNode("Definitions");
                readAvailableFormats(formatsRootNode);
                definitionsPanel.setFormats(formatsRootNode);
                DefaultControlPanel controlPanel = new DefaultControlPanel(definitionsPanel.getResourceBundle());
//                        HelpModuleApi helpModule = App.getModule(HelpModuleApi.class);
//                        helpModule.addLinkToControlPanel(controlPanel, new HelpLink(HELP_ID));
                final WindowHandler dialog = windowModule.createDialog(definitionsPanel, controlPanel);
                windowModule.setWindowTitle(dialog, definitionsPanel.getResourceBundle());
                windowModule.addHeaderPanel(dialog.getWindow(), definitionsPanel.getClass(), definitionsPanel.getResourceBundle());
                controlPanel.setController((DefaultControlController.ControlActionType actionType) -> {
                    if (actionType == DefaultControlController.ControlActionType.OK) {
                        Optional<DefaultMutableTreeNode> optSelectedNode = definitionsPanel.getSelectedNode();
                        if (optSelectedNode.isPresent()) {
                            Object userObject = optSelectedNode.get().getUserObject();
                            if (userObject instanceof DefinitionRecord) {
                                definitionRecord = (DefinitionRecord) userObject;
                                update();
                            }
                        }
                    }
                    dialog.close();
                    dialog.dispose();
                });
                dialog.showCentered(sidePanel);
            }

            @Override
            public void showStatusDetail() {
                WindowModuleApi windowModule = App.getModule(WindowModuleApi.class);
                final KaitaiStatusPanel statusPanel = new KaitaiStatusPanel();
                CloseControlPanel controlPanel = new CloseControlPanel(statusPanel.getResourceBundle());
                final WindowHandler dialog = windowModule.createDialog(statusPanel, controlPanel);
                windowModule.setWindowTitle(dialog, statusPanel.getResourceBundle());
                controlPanel.setController(() -> {
                    dialog.close();
                    dialog.dispose();
                });
                dialog.showCentered(sidePanel);
            }
        });
        return sidePanel;
    }
    
    public void update() {
        if (definitionRecord == null) {
            return;
        }

        EditableBinaryData sourceData = binaryDataComponent != null ? (EditableBinaryData) binaryDataComponent.getCodeArea().getContentData() : new ByteArrayEditableData();
        sideManager.loadFrom(definitionRecord, sourceData);
    }

    private void readAvailableFormats(DefaultMutableTreeNode formatsRootNode) {
        String formatsPath = "/org/exbin/framework/bined/kaitai/resources/formats/";
        FileSystem fileSystem = null;
        try {
            Path rootPath = null;
            try {
                URI formats = getClass().getResource(formatsPath).toURI();
                fileSystem = FileSystems.newFileSystem(formats, Collections.<String, Object>emptyMap());
                rootPath = fileSystem.getPath(formatsPath);
            } catch (URISyntaxException | IOException ex) {
                Logger.getLogger(BinedKaitaiModule.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (rootPath == null) {
                return;
            }

            List<PathRecord> records = new ArrayList<>();
            records.add(new PathRecord(rootPath, formatsRootNode));
            while (!records.isEmpty()) {
                PathRecord record = records.remove(records.size() - 1);
                Path path = record.path;
                try {
                    Stream<Path> walk = Files.walk(path, 1);
                    for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
                        Path childPath = it.next();
                        if (childPath == path) {
                            continue;
                        }

                        String fileName = "";
                        if (childPath.getNameCount() > 0) {
                            fileName = childPath.getName(childPath.getNameCount() - 1).toString();
                        }
                        if (fileName.endsWith(File.separator)) {
                            fileName = fileName.substring(0, fileName.length() - 1);
                            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(fileName);
                            record.node.add(childNode);
                            records.add(new PathRecord(childPath, childNode));
                        } else if (fileName.endsWith(".ksy")) {
                            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new DefinitionRecord(fileName, childPath.toUri()));
                            record.node.add(childNode);
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(BinedKaitaiModule.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } finally {
            try {
                if (fileSystem != null) {
                    fileSystem.close();
                }
            } catch (Throwable tw) {
                // ignore
            }
        }
    }

    public void setActiveComponent(@Nullable ContextComponent contextComponent) {
        binaryDataComponent = contextComponent instanceof BinaryDataComponent ? (BinaryDataComponent) contextComponent : null;
        update();
    }

    @ParametersAreNonnullByDefault
    private static class PathRecord {

        Path path;
        DefaultMutableTreeNode node;

        public PathRecord(Path path, DefaultMutableTreeNode node) {
            this.path = path;
            this.node = node;
        }
    }
}
