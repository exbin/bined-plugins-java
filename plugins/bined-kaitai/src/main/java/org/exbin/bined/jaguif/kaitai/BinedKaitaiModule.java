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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.jaguif.App;
import org.exbin.jaguif.ModuleUtils;
import org.exbin.jaguif.PluginModule;
import org.exbin.jaguif.action.api.ContextComponent;
import org.exbin.bined.jaguif.component.BinEdFileManager;
import org.exbin.bined.jaguif.component.BinaryFileDocument;
import org.exbin.bined.jaguif.component.BinedComponentModule;
import org.exbin.bined.jaguif.inspector.BinEdInspectorManager;
import org.exbin.bined.jaguif.inspector.BinedInspectorModule;
import org.exbin.bined.jaguif.kaitai.gui.KaitaiSidePanel;
import org.exbin.bined.jaguif.kaitai.inspector.KaitaiInspectorProvider;
import org.exbin.bined.jaguif.kaitai.settings.KaitaiOptions;
import org.exbin.bined.jaguif.kaitai.settings.KaitaiSettingsApplier;
import org.exbin.jaguif.context.api.ContextChange;
import org.exbin.jaguif.context.api.ContextChangeRegistration;
import org.exbin.jaguif.document.api.ContextDocument;
import org.exbin.jaguif.frame.api.FrameModuleApi;
import org.exbin.jaguif.language.api.LanguageModuleApi;
import org.exbin.jaguif.options.api.OptionsModuleApi;
import org.exbin.jaguif.options.api.OptionsStorage;
import org.exbin.jaguif.options.settings.api.ApplySettingsContribution;
import org.exbin.jaguif.options.settings.api.OptionsSettingsManagement;
import org.exbin.jaguif.options.settings.api.OptionsSettingsModuleApi;
import org.exbin.jaguif.sidebar.api.ComponentSideBarContribution;
import org.exbin.jaguif.sidebar.api.SideBarComponent;
import org.exbin.jaguif.sidebar.api.SideBarDefinitionManagement;
import org.exbin.jaguif.sidebar.api.SideBarModuleApi;
import org.exbin.jaguif.ui.api.UiModuleApi;

/**
 * Binary editor plugin for Kaitai analyzer support.
 */
@ParametersAreNonnullByDefault
public class BinedKaitaiModule implements PluginModule {

    public static final String MODULE_ID = ModuleUtils.getModuleIdByApi(BinedKaitaiModule.class);
    public static final String SIDEBAR_COMPONENT_ID = "kaitai";
    public static final String RESOURCE_FORMATS_PATH = "/org/exbin/framework/bined/kaitai/resources/formats/";

    private java.util.ResourceBundle resourceBundle = null;

    private KaitaiColorModifier kaitaiColorModifier;
    private KaitaiSideBarComponent sideBarComponent;

    public BinedKaitaiModule() {
    }

    @Override
    public void register() {
        UiModuleApi uiModule = App.getModule(UiModuleApi.class);
        uiModule.addPostInitAction(() -> {
            kaitaiColorModifier = new KaitaiColorModifier();

            registerSideBar();
            registerInspector();
            registerSettings();
            // registerMenuActions();
            // registerPopupMenuActions();

            BinedComponentModule binedComponentModule = App.getModule(BinedComponentModule.class);
            BinEdFileManager fileManager = binedComponentModule.getFileManager();
            fileManager.addPainterColorModifier(kaitaiColorModifier);

            SideBarModuleApi sideBarModule = App.getModule(SideBarModuleApi.class);
            sideBarModule.setAutoShow(true);
        });

        FrameModuleApi frameModule = App.getModule(FrameModuleApi.class);
        frameModule.addClosingListener(() -> {
            saveState();
            return true;
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
        getResourceBundle();
        SideBarModuleApi sideBarModule = App.getModule(SideBarModuleApi.class);
        SideBarDefinitionManagement sideBarManager = sideBarModule.getMainSideBarManager(SideBarModuleApi.MODULE_ID);
        sideBarManager.registerSideBarContribution(new ComponentSideBarContribution() {
            @Nonnull
            public String getContributionId() {
                return SIDEBAR_COMPONENT_ID;
            }
            
            @Nonnull
            public SideBarComponent createComponent() {
                sideBarComponent = new KaitaiSideBarComponent();
                sideBarComponent.putValue(SideBarComponent.KEY_ID, SIDEBAR_COMPONENT_ID);
                //sideBarComponent.putValue(SideBarComponent.KEY_NAME, "KT");
                sideBarComponent.putValue(SideBarComponent.KEY_ICON, new javax.swing.ImageIcon(getClass().getResource(resourceBundle.getString("kaitaiSideBarComponent.icon"))));
                sideBarComponent.putValue(SideBarComponent.KEY_TOOLTIP, resourceBundle.getString("kaitaiSideBarComponent.toolTip"));
                sideBarComponent.putValue(SideBarComponent.KEY_CONTEXT_CHANGE, new ContextChange() {
                    @Override
                    public void register(ContextChangeRegistration registrar) {
                        registrar.registerChangeListener(ContextDocument.class, (instance) -> {
                            if (instance instanceof BinaryFileDocument) {
                                sideBarComponent.setActiveDocument((BinaryFileDocument) instance);
                            }
                        });
                    }
                });
                return sideBarComponent;
            }
        });
    }

    public void registerInspector() {
        BinedInspectorModule binedInspectorModule = App.getModule(BinedInspectorModule.class);
        BinEdInspectorManager inspectorManager = binedInspectorModule.getBinEdInspectorManager();
        inspectorManager.addInspector(new KaitaiInspectorProvider(getResourceBundle()));
    }
    
    public void registerSettings() {
        getResourceBundle();
        OptionsSettingsModuleApi settingsModule = App.getModule(OptionsSettingsModuleApi.class);
        OptionsSettingsManagement settingsManagement = settingsModule.getMainSettingsManager();

        settingsManagement.registerSettingsOptions(KaitaiOptions.class, (optionsStorage) -> new KaitaiOptions(optionsStorage));
        settingsManagement.registerApplySetting(KaitaiOptions.class, new ApplySettingsContribution(KaitaiSettingsApplier.APPLIER_ID, new KaitaiSettingsApplier()));
        settingsManagement.registerApplyContextSetting(ContextComponent.class, new ApplySettingsContribution(KaitaiSettingsApplier.APPLIER_ID, new KaitaiSettingsApplier()));
    }
    
    public void saveState() {
        OptionsModuleApi optionsModule = App.getModule(OptionsModuleApi.class);
        OptionsStorage optionsStorage = optionsModule.getAppOptions();
        KaitaiOptions options = new KaitaiOptions(optionsStorage);
        KaitaiSidePanel sidePanel = getKaitaiSideManager().getSidePanel();
        List<DefinitionRecord> definitions = sidePanel.getDefinitions();
        int listSize = definitions.size();
        options.setListSize(listSize);
        for (int i = 0; i < listSize; i++) {
            DefinitionRecord record = definitions.get(i);
            URI uri = record.getUri();
            int pathLength = BinedKaitaiModule.RESOURCE_FORMATS_PATH.length();
            if ("jar".equals(uri.getScheme())) {
                String path = uri.toString();
                int lastPos = path.lastIndexOf(BinedKaitaiModule.RESOURCE_FORMATS_PATH);
                options.setListItemBuildIn(path.substring(lastPos + pathLength), i);
                options.setListItemPath("", i);
            } else {
                options.setListItemBuildIn("", i);
                options.setListItemPath(uri.toString(), i);
            }
        }
        options.setDefinition(sidePanel.getSelectedDefinitionIndex());
        optionsStorage.flush();
    }

    public void setBuildInDefinition(String ksyFilePath) {
        sideBarComponent.setBuildInDefinition(ksyFilePath);
    }

    @Nullable
    public KaitaiColorModifier getKaitaiColorModifier() {
        return kaitaiColorModifier;
    }

    @Nonnull
    public KaitaiSideManager getKaitaiSideManager() {
        return sideBarComponent.getSideManager();
    }

    @Nonnull
    public DefinitionRecord getDefinitionByPath(URI fileUri) {
        File file = new File(fileUri);
        return new DefinitionRecord(file.getName(), file.getName(), fileUri);
    }

    @Nonnull
    public DefinitionRecord getBuildInDefinition(String buildIn) {
        FileSystem fileSystem = null;
        try {
            URI fileUri = getClass().getResource(RESOURCE_FORMATS_PATH + buildIn).toURI();
            fileSystem = FileSystems.newFileSystem(fileUri, Collections.<String, Object>emptyMap());
            Path filePath = fileSystem.getPath(RESOURCE_FORMATS_PATH + buildIn);
            String fileName = "";
            if (filePath.getNameCount() > 0) {
                fileName = filePath.getName(filePath.getNameCount() - 1).toString();
            }
            if (fileName.endsWith("/")) {
                fileName = fileName.substring(0, fileName.length() - 1);
            }

            String title = null;
            InputStream stream = null;
            BufferedReader reader = null;
            try {
                stream = fileUri.toURL().openStream();
                reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                String read;
                while ((read = reader.readLine()) != null) {
                    int pos = read.indexOf("title:");
                    if (pos >= 0) {
                        title = read.substring(pos + 6).trim();
                        if ('\"' == title.charAt(0)) {
                            title = title.substring(1, title.length() - 1);
                        }
                        break;
                    }
                }
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }

            String defTitle = title.isEmpty() ? fileName.substring(0, fileName.length() - 4) : title;
            return new DefinitionRecord(defTitle, fileName, fileUri);
        } catch (URISyntaxException | IOException ex) {
            throw new RuntimeException("Failed to open build-in kaitai definition " + buildIn, ex);
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
}
