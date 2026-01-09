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

import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.tree.DefaultMutableTreeNode;
import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.auxiliary.binary_data.array.ByteArrayEditableData;
import org.exbin.framework.App;
import org.exbin.framework.action.api.ContextComponent;
import org.exbin.framework.bined.BinaryDataComponent;
import org.exbin.framework.bined.kaitai.gui.KaitaiBuildInPanel;
import org.exbin.framework.bined.kaitai.gui.KaitaiDefinitionPreviewPanel;
import org.exbin.framework.bined.kaitai.gui.KaitaiDefinitionsPanel;
import org.exbin.framework.bined.kaitai.gui.KaitaiSidePanel;
import org.exbin.framework.bined.kaitai.gui.KaitaiProcessingMessagePanel;
import org.exbin.framework.file.api.DefaultFileTypes;
import org.exbin.framework.file.api.FileDialogsProvider;
import org.exbin.framework.file.api.FileModuleApi;
import org.exbin.framework.file.api.OpenFileResult;
import org.exbin.framework.sidebar.api.AbstractSideBarComponent;
import org.exbin.framework.window.api.WindowHandler;
import org.exbin.framework.window.api.WindowModuleApi;
import org.exbin.framework.window.api.controller.CloseControlController;
import org.exbin.framework.window.api.controller.DefaultControlController;
import org.exbin.framework.window.api.gui.CloseControlPanel;
import org.exbin.framework.window.api.gui.DefaultControlPanel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * Kaitai sidebar component.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class KaitaiSideBarComponent extends AbstractSideBarComponent {

    private static final String RESOURCE_FORMATS_PATH = "/org/exbin/framework/bined/kaitai/resources/formats/";
    protected KaitaiSideManager sideManager = new KaitaiSideManager();
    protected DefinitionRecord definitionRecord = null;
    protected BinaryDataComponent binaryDataComponent = null;

    @Override
    public JComponent createComponent() {
        KaitaiSidePanel sidePanel = sideManager.getSidePanel();
        sidePanel.setController(new KaitaiSidePanel.Controller() {
            @Override
            public void selectedDefinitionChanged() {
                DefinitionRecord definitionRecord = sidePanel.getSelectedDefinition();
                EditableBinaryData sourceData = binaryDataComponent != null ? (EditableBinaryData) binaryDataComponent.getCodeArea().getContentData() : new ByteArrayEditableData();
                sideManager.processDefinition(definitionRecord, sourceData);
            }

            @Override
            public void manageDefinitions() {
                WindowModuleApi windowModule = App.getModule(WindowModuleApi.class);
                final KaitaiDefinitionsPanel definitionsPanel = new KaitaiDefinitionsPanel();
                definitionsPanel.setDropTarget(new DropTarget() {
                    @Override
                    public synchronized void drop(DropTargetDropEvent event) {
                        try {
                            event.acceptDrop(DnDConstants.ACTION_COPY);
                            Object transferData = event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                            List<?> droppedFiles = (List) transferData;
                            for (Object droppedFile : droppedFiles) {
                                File file = (File) droppedFile;
                                DefinitionRecord record = new DefinitionRecord(file.getName(), file.getName(), file.toURI());
                                definitionsPanel.addDefinition(record);
                            }
                        } catch (UnsupportedFlavorException | IOException ex) {
                            Logger.getLogger(KaitaiSideBarComponent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });

                definitionsPanel.setController(new KaitaiDefinitionsPanel.Controller() {
                    @Override
                    public void addDefinition() {
                        FileModuleApi fileModule = App.getModule(FileModuleApi.class);
                        FileDialogsProvider dialogsProvider = fileModule.getFileDialogsProvider();
                        OpenFileResult openFileResult = dialogsProvider.showOpenFileDialog(new DefaultFileTypes(new KsyFileType()), null, null, null);
                        if (openFileResult.getDialogResult() == JFileChooser.APPROVE_OPTION) {
                            File file = openFileResult.getSelectedFile().get();
                            DefinitionRecord record = new DefinitionRecord(file.getName(), file.getName(), file.toURI());
                            definitionsPanel.addDefinition(record);
                        }
                    }

                    @Override
                    public void addBuildIn() {
                        KaitaiBuildInPanel buildInPanel = new KaitaiBuildInPanel();
                        KaitaiDefinitionPreviewPanel previewPanel = new KaitaiDefinitionPreviewPanel();
                        JSplitPane splitPane = new JSplitPane();
                        splitPane.setLeftComponent(buildInPanel);
                        splitPane.setRightComponent(previewPanel);
                        DefaultMutableTreeNode formatsRootNode = new DefaultMutableTreeNode("Definitions");
                        readAvailableFormats(formatsRootNode);
                        buildInPanel.setFormats(formatsRootNode);
                        DefaultControlPanel controlPanel = new DefaultControlPanel(buildInPanel.getResourceBundle());
//                        HelpModuleApi helpModule = App.getModule(HelpModuleApi.class);
//                        helpModule.addLinkToControlPanel(controlPanel, new HelpLink(HELP_ID));
                        splitPane.setSize(700, 500);
                        final WindowHandler dialog = windowModule.createDialog(splitPane, controlPanel);
                        windowModule.setWindowTitle(dialog, buildInPanel.getResourceBundle());
                        windowModule.addHeaderPanel(dialog.getWindow(), buildInPanel.getClass(), buildInPanel.getResourceBundle());
                        controlPanel.setController((DefaultControlController.ControlActionType actionType) -> {
                            if (actionType == DefaultControlController.ControlActionType.OK) {
                                Optional<DefaultMutableTreeNode> optSelectedNode = buildInPanel.getSelectedNode();
                                if (optSelectedNode.isPresent()) {
                                    Object userObject = optSelectedNode.get().getUserObject();
                                    if (userObject instanceof DefinitionRecord) {
                                        definitionsPanel.addDefinition((DefinitionRecord) userObject);
                                    }
                                }
                            }
                            dialog.close();
                            dialog.dispose();
                        });
                        dialog.showCentered(sidePanel);
                    }

                    @Override
                    public void editDefinition() {
                        RSyntaxTextArea textArea = new RSyntaxTextArea();
                        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
                        textArea.setEditable(false);
                        textArea.setSyntaxEditingStyle(RSyntaxTextArea.SYNTAX_STYLE_YAML);
                        DefinitionRecord definitionRecord = definitionsPanel.getSelectedDefinition().get();
                        InputStream input = null;
                        BufferedReader reader = null;
                        try {
                            URL fileSource = definitionRecord.getUri().toURL();
                            input = fileSource.openStream();
                            reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
                            StringBuilder builder = new StringBuilder();
                            String read;
                            try {
                                while ((read = reader.readLine()) != null) {
                                    builder.append(read);
                                    builder.append("\n");
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(KaitaiSideBarComponent.class.getName()).log(Level.SEVERE, null, ex);
                            }                            
                            textArea.setText(builder.toString());
                        } catch (IOException ex) {
                            Logger.getLogger(KaitaiSideBarComponent.class.getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            if (reader != null) {
                                try {
                                    reader.close();
                                } catch (IOException e) {
                                    // ignore
                                }
                            }
                            if (input != null) {
                                try {
                                    input.close();
                                } catch (IOException ex) {
                                    // ignore
                                }
                            }
                        }

                        scrollPane.setViewportView(textArea);
                        scrollPane.setSize(new Dimension(700, 500));
                        CloseControlPanel controlPanel = new CloseControlPanel();
                        final WindowHandler dialog = windowModule.createDialog(scrollPane, controlPanel);
                        controlPanel.setController(new CloseControlController() {
                            @Override
                            public void controlActionPerformed() {
                                dialog.close();
                            }
                        });
                        dialog.showCentered(sidePanel);
                    }

                    @Override
                    public void updatePreview(JPanel previewPanel, DefinitionRecord definition) {
                        // TODO
                    }
                });
                definitionsPanel.setDefinitions(sidePanel.getDefinitions());
                DefaultControlPanel controlPanel = new DefaultControlPanel(definitionsPanel.getResourceBundle());
//                        HelpModuleApi helpModule = App.getModule(HelpModuleApi.class);
//                        helpModule.addLinkToControlPanel(controlPanel, new HelpLink(HELP_ID));
                final WindowHandler dialog = windowModule.createDialog(definitionsPanel, controlPanel);
                windowModule.setWindowTitle(dialog, definitionsPanel.getResourceBundle());
                windowModule.addHeaderPanel(dialog.getWindow(), definitionsPanel.getClass(), definitionsPanel.getResourceBundle());
                controlPanel.setController((DefaultControlController.ControlActionType actionType) -> {
                    if (actionType == DefaultControlController.ControlActionType.OK) {
                        sidePanel.setDefinitions(definitionsPanel.getDefinitions());
                    }
                    dialog.close();
                    dialog.dispose();
                });
                dialog.showCentered(sidePanel);
            }

            @Override
            public void showStatusDetail() {
                WindowModuleApi windowModule = App.getModule(WindowModuleApi.class);
                final KaitaiProcessingMessagePanel statusPanel = new KaitaiProcessingMessagePanel();
                statusPanel.setProcessingMessage(sideManager.getProcessingMessage());
                CloseControlPanel controlPanel = new CloseControlPanel(statusPanel.getResourceBundle());
                final WindowHandler dialog = windowModule.createDialog(statusPanel, controlPanel);
                windowModule.setWindowTitle(dialog, statusPanel.getResourceBundle());
                controlPanel.setController(() -> {
                    dialog.close();
                    dialog.dispose();
                });
                dialog.getWindow().setSize(new Dimension(400, 300));
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

    public void setActiveComponent(@Nullable ContextComponent contextComponent) {
        binaryDataComponent = contextComponent instanceof BinaryDataComponent ? (BinaryDataComponent) contextComponent : null;
        update();
    }

    public void setDefinitionRecord(DefinitionRecord definitionRecord) {
        this.definitionRecord = definitionRecord;
        update();
    }

    public void setBuildInDefinition(String ksyFilePath) {
        FileSystem fileSystem = null;
        try {
            URI fileUri = getClass().getResource(RESOURCE_FORMATS_PATH + ksyFilePath).toURI();
            fileSystem = FileSystems.newFileSystem(fileUri, Collections.<String, Object>emptyMap());
            Path filePath = fileSystem.getPath(RESOURCE_FORMATS_PATH + ksyFilePath);
            String fileName = "";
            if (filePath.getNameCount() > 0) {
                fileName = filePath.getName(filePath.getNameCount() - 1).toString();
            }
            if (fileName.endsWith("/")) {
                fileName = fileName.substring(0, fileName.length() - 1);
            }
            setDefinitionRecord(new DefinitionRecord(fileName, fileName, filePath.toUri()));
        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(KaitaiSideBarComponent.class.getName()).log(Level.SEVERE, null, ex);
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

    private void readAvailableFormats(DefaultMutableTreeNode formatsRootNode) {
        Map<String, DefaultMutableTreeNode> nodes = new HashMap<>();

        String listLine;
        try (BufferedReader listReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(RESOURCE_FORMATS_PATH + "formats.txt")))) {
            do {
                listLine = listReader.readLine();
                if (listLine != null) {
                    if (listLine.endsWith("/")) {
                        DefaultMutableTreeNode parentNode;
                        String fileName;
                        int nameStart = listLine.lastIndexOf("/", listLine.length() - 2);
                        if (nameStart >= 0) {
                            parentNode = nodes.get(listLine.substring(0, nameStart + 1));
                            fileName = listLine.substring(nameStart + 1, listLine.length() - 2);
                        } else {
                            parentNode = formatsRootNode;
                            fileName = listLine.substring(0, listLine.length() - 2);
                        }
                        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(fileName);
                        parentNode.add(childNode);
                        nodes.put(listLine, childNode);

                        continue;
                    }

                    DefaultMutableTreeNode parentNode;
                    String fileName;
                    int nameStart = listLine.lastIndexOf("/");
                    if (nameStart >= 0) {
                        parentNode = nodes.get(listLine.substring(0, nameStart + 1));
                        fileName = listLine.substring(nameStart + 1);
                    } else {
                        parentNode = formatsRootNode;
                        fileName = listLine;
                    }
                    URI definitionUri = getClass().getResource(RESOURCE_FORMATS_PATH + listLine).toURI();
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new DefinitionRecord(fileName, fileName, definitionUri));
                    parentNode.add(childNode);
                }
            } while (listLine != null);
        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(KaitaiSideBarComponent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
