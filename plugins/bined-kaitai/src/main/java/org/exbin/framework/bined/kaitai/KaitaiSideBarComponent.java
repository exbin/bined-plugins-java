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

import java.awt.BorderLayout;
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
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.tree.DefaultMutableTreeNode;
import org.exbin.auxiliary.binary_data.BinaryData;
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
import org.yaml.snakeyaml.Yaml;

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
            public void selectedDefinitionChanged() {
                DefinitionRecord definitionRecord = sidePanel.getSelectedDefinition();
                BinaryData sourceData = binaryDataComponent != null ? binaryDataComponent.getCodeArea().getContentData() : null;
                sideManager.processDefinition(definitionRecord, sourceData instanceof EditableBinaryData ? (EditableBinaryData) sourceData : new ByteArrayEditableData());
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
                            BinedKaitaiModule kaitaiModule = App.getModule(BinedKaitaiModule.class);
                            for (Object droppedFile : droppedFiles) {
                                DefinitionRecord record = kaitaiModule.getDefinitionByPath(((File) droppedFile).toURI());
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
                        OpenFileResult openFileResult = dialogsProvider.showOpenFileDialog(definitionsPanel, new DefaultFileTypes(new KsyFileType()), null, null, null);
                        if (openFileResult.getDialogResult() == JFileChooser.APPROVE_OPTION) {
                            BinedKaitaiModule kaitaiModule = App.getModule(BinedKaitaiModule.class);
                            File file = openFileResult.getSelectedFile().get();
                            DefinitionRecord record = kaitaiModule.getDefinitionByPath(file.toURI());
                            definitionsPanel.addDefinition(record);
                        }
                    }

                    @Override
                    public void addBuildIn() {
                        final KaitaiBuildInPanel buildInPanel = new KaitaiBuildInPanel();
                        final KaitaiDefinitionPreviewPanel previewPanel = new KaitaiDefinitionPreviewPanel();
                        DefaultMutableTreeNode formatsRootNode = new DefaultMutableTreeNode("Definitions");
                        buildInPanel.setController(new KaitaiBuildInPanel.Controller() {
                            @Override
                            public void selectedDefinitionChanged() {
                                Optional<DefaultMutableTreeNode> optSelectedNode = buildInPanel.getSelectedNode();
                                if (optSelectedNode.isPresent()) {
                                    DefaultMutableTreeNode treeNode = optSelectedNode.get();
                                    Object userObject = treeNode.getUserObject();
                                    if (userObject instanceof DefinitionRecord) {
                                        String title = "";
                                        String id = "";
                                        String extension = "";
                                        String mimeType = "";

                                        InputStream input = null;
                                        Reader inputReader = null;
                                        try {
                                            URL fileSource = ((DefinitionRecord) userObject).getUri().toURL();
                                            input = fileSource.openStream();
                                            inputReader = new InputStreamReader(input, "UTF-8");
                                            Yaml yaml = new Yaml();
                                            Object content = yaml.load(input);
                                            if (content instanceof Map) {
                                                Map map = (Map) content;
                                                Object metaNode = map.get("meta");
                                                if (metaNode instanceof Map) {
                                                    Map metaMap = (Map) metaNode;
                                                    Object value = metaMap.get("id");
                                                    if (value instanceof String) {
                                                        id = (String) value;
                                                    }
                                                    value = metaMap.get("title");
                                                    if (value instanceof String) {
                                                        title = (String) value;
                                                    }
                                                    value = metaMap.get("file-extension");
                                                    if (value instanceof String) {
                                                        extension = (String) value;
                                                    }
                                                    value = metaMap.get("xref");
                                                    if (value instanceof Map) {
                                                        Map xRefMap = (Map) value;
                                                        value = xRefMap.get("mime");
                                                        if (value instanceof String) {
                                                            mimeType = (String) value;
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (Throwable ex) {
                                            // ignore
                                        } finally {
                                            if (inputReader != null) {
                                                try {
                                                    inputReader.close();
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
                                        previewPanel.setData(title, id, extension, mimeType);
                                        return;
                                    }
                                }

                                previewPanel.clearData();
                            }

                            @Override
                            public void filter(String filter) {
                                if (filter.isEmpty()) {
                                    buildInPanel.setFormats(formatsRootNode);
                                    return;
                                }

                                DefaultMutableTreeNode filteredFormats = new DefaultMutableTreeNode("Definitions");
                                
                                LinkedList<NodeFilterRecord> nodesToProcess = new LinkedList<>();
                                LinkedList<DefaultMutableTreeNode> nodesToCheck = new LinkedList<>();
                                nodesToProcess.add(new NodeFilterRecord(formatsRootNode, filteredFormats));

                                while (!nodesToProcess.isEmpty()) {
                                    NodeFilterRecord filterRecord = nodesToProcess.remove();
                                    DefaultMutableTreeNode currentNode = filterRecord.node;
                                    DefaultMutableTreeNode filteredNode = filterRecord.filteredNode;
                                    
                                    for (int i = 0; i < currentNode.getChildCount(); i++) {
                                        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) currentNode.getChildAt(i);
                                        Object userObject = childNode.getUserObject();
                                        if (userObject instanceof DefinitionRecord) {
                                            DefinitionRecord definition = (DefinitionRecord) userObject;
                                            if (definition.fileName.contains(filter) || definition.title.contains(filter)) {
                                                DefaultMutableTreeNode childFilteredNode = (DefaultMutableTreeNode) childNode.clone();
                                                filteredNode.add(childFilteredNode);
                                            }
                                        } else {
                                            DefaultMutableTreeNode childFilteredNode = (DefaultMutableTreeNode) childNode.clone();
                                            filteredNode.add(childFilteredNode);
                                            nodesToProcess.add(new NodeFilterRecord(childNode, childFilteredNode));
                                            nodesToCheck.addFirst(childFilteredNode);
                                        }
                                    }
                                }
                                
                                while (!nodesToCheck.isEmpty()) {
                                    DefaultMutableTreeNode node = nodesToCheck.remove();
                                    if (node.isLeaf()) {
                                        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
                                        parentNode.remove(node);
                                    }
                                }
                                
                                buildInPanel.setFormats(filteredFormats);
                            }
                        });
                        JPanel wrapperPanel = new JPanel(new BorderLayout());
                        JSplitPane splitPane = new JSplitPane();
                        splitPane.setLeftComponent(buildInPanel);
                        splitPane.setRightComponent(previewPanel);
                        readAvailableFormats(formatsRootNode);
                        buildInPanel.setFormats(formatsRootNode);
                        DefaultControlPanel controlPanel = new DefaultControlPanel(buildInPanel.getResourceBundle());
//                        HelpModuleApi helpModule = App.getModule(HelpModuleApi.class);
//                        helpModule.addLinkToControlPanel(controlPanel, new HelpLink(HELP_ID));
                        wrapperPanel.add(splitPane, BorderLayout.CENTER);
                        splitPane.setDividerLocation(400);
                        wrapperPanel.setPreferredSize(new Dimension(800, 500));
                        wrapperPanel.setSize(800, 500);
                        final WindowHandler dialog = windowModule.createDialog(wrapperPanel, controlPanel);
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
                        JPanel definitionPanel = new JPanel(new BorderLayout());
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
                            textArea.setCaretPosition(0);
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
                        definitionPanel.add(scrollPane, BorderLayout.CENTER);
                        scrollPane.setPreferredSize(new Dimension(700, 500));
                        scrollPane.setSize(700, 500);
                        CloseControlPanel controlPanel = new CloseControlPanel();
                        final WindowHandler dialog = windowModule.createDialog(definitionPanel, controlPanel);
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

        BinaryData sourceData = binaryDataComponent != null ? (BinaryData) binaryDataComponent.getCodeArea().getContentData() : new ByteArrayEditableData();
        sideManager.loadFrom(definitionRecord, sourceData);
    }

    @Nonnull
    public KaitaiSideManager getSideManager() {
        return sideManager;
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
        BinedKaitaiModule kaitaiModule = App.getModule(BinedKaitaiModule.class);
        DefinitionRecord record = kaitaiModule.getBuildInDefinition(ksyFilePath);
        setDefinitionRecord(record);
    }

    private void readAvailableFormats(DefaultMutableTreeNode formatsRootNode) {
        Map<String, DefaultMutableTreeNode> nodes = new HashMap<>();

        String listLine;
        try (BufferedReader listReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(BinedKaitaiModule.RESOURCE_FORMATS_PATH + "formats.txt")))) {
            do {
                listLine = listReader.readLine();
                if (listLine != null) {
                    String title = listReader.readLine();
                    if (listLine.endsWith("/")) {
                        DefaultMutableTreeNode parentNode;
                        String fileName;
                        int nameStart = listLine.lastIndexOf("/", listLine.length() - 2);
                        if (nameStart >= 0) {
                            parentNode = nodes.get(listLine.substring(0, nameStart + 1));
                            fileName = listLine.substring(nameStart + 1, listLine.length() - 1);
                        } else {
                            parentNode = formatsRootNode;
                            fileName = listLine.substring(0, listLine.length() - 1);
                        }
                        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(title.isEmpty() ? fileName : title);
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
                    URI definitionUri = getClass().getResource(BinedKaitaiModule.RESOURCE_FORMATS_PATH + listLine).toURI();
                    String defTitle = title.isEmpty() ? fileName.substring(0, fileName.length() - 4) : title;
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new DefinitionRecord(defTitle, fileName, definitionUri));
                    parentNode.add(childNode);
                }
            } while (listLine != null);
        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(KaitaiSideBarComponent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static class NodeFilterRecord {
        DefaultMutableTreeNode node;
        DefaultMutableTreeNode filteredNode;

        public NodeFilterRecord(DefaultMutableTreeNode node, DefaultMutableTreeNode filteredNode) {
            this.node = node;
            this.filteredNode = filteredNode;
        }
    }
}
