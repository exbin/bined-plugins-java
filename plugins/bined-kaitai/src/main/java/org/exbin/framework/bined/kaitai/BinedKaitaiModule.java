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
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import org.exbin.framework.App;
import org.exbin.framework.ModuleUtils;
import org.exbin.framework.PluginModule;
import org.exbin.framework.bined.kaitai.gui.KaitaiDefinitionsPanel;
import org.exbin.framework.bined.kaitai.gui.KaitaiSidePanel;
import org.exbin.framework.language.api.LanguageModuleApi;
import org.exbin.framework.sidebar.api.AbstractSideBarComponent;
import org.exbin.framework.sidebar.api.SideBarComponent;
import org.exbin.framework.sidebar.api.SideBarDefinitionManagement;
import org.exbin.framework.sidebar.api.SideBarModuleApi;
import org.exbin.framework.ui.api.UiModuleApi;
import org.exbin.framework.window.api.WindowHandler;
import org.exbin.framework.window.api.WindowModuleApi;
import org.exbin.framework.window.api.controller.DefaultControlController;
import org.exbin.framework.window.api.gui.DefaultControlPanel;

/**
 * Binary editor plugin supporting Kaitai decompilers.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class BinedKaitaiModule implements PluginModule {

    public static final String MODULE_ID = ModuleUtils.getModuleIdByApi(BinedKaitaiModule.class);

    private java.util.ResourceBundle resourceBundle = null;

    public BinedKaitaiModule() {
    }

    @Override
    public void register() {
        UiModuleApi uiModule = App.getModule(UiModuleApi.class);
        uiModule.addPostInitAction(() -> {
            registerSideBar();
            // registerMenuActions();
            // registerPopupMenuActions();
        });
    }

    @Nonnull
    public ResourceBundle getResourceBundle() {
        if (resourceBundle == null) {
            resourceBundle = App.getModule(LanguageModuleApi.class).getBundle(BinedKaitaiModule.class);
        }

        return resourceBundle;
    }

    public void registerSideBar() {
        SideBarModuleApi sideBarModule = App.getModule(SideBarModuleApi.class);
        SideBarDefinitionManagement sideBarManager = sideBarModule.getMainSideBarManager(SideBarModuleApi.MODULE_ID);
        AbstractSideBarComponent sideBarComponent = new AbstractSideBarComponent() {
            @Override
            public JComponent createComponent() {
                KaitaiSidePanel sidePanel = new KaitaiSidePanel();
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
                            }
                            dialog.close();
                            dialog.dispose();
                        });
                        dialog.showCentered(sidePanel);
                    }

                    @Override
                    public void statusDetail() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }
                });
                return sidePanel;
            }
        };
        sideBarComponent.putValue(SideBarComponent.KEY_ID, "test1");
        //sideBarComponent.putValue(SideBarComponent.KEY_NAME, "KT");
        sideBarComponent.putValue(SideBarComponent.KEY_ICON, new javax.swing.ImageIcon(getClass().getResource("/org/exbin/framework/bined/kaitai/resources/icons/kaitai_light_32.png")));
        sideBarComponent.putValue(SideBarComponent.KEY_TOOLTIP, "Kaitai");
        sideBarManager.registerSideBarComponent(sideBarComponent);
    }

    private void readAvailableFormats(DefaultMutableTreeNode formatsRootNode) {
        String formatsPath = "/org/exbin/framework/bined/kaitai/resources/formats/";
        Path rootPath = null;
        try {
            URI formats = getClass().getResource(formatsPath).toURI();
            FileSystem fileSystem = FileSystems.newFileSystem(formats, Collections.<String, Object>emptyMap());
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
                        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(fileName);
                        record.node.add(childNode);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(BinedKaitaiModule.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private static class PathRecord {
        Path path;
        DefaultMutableTreeNode node;

        public PathRecord(Path path, DefaultMutableTreeNode node) {
            this.path = path;
            this.node = node;
        }
    }
}
