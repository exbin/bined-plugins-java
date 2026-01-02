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

import java.util.ResourceBundle;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.framework.App;
import org.exbin.framework.ModuleUtils;
import org.exbin.framework.PluginModule;
import org.exbin.framework.action.api.ContextComponent;
import org.exbin.framework.bined.BinEdFileManager;
import org.exbin.framework.bined.BinedModule;
import org.exbin.framework.bined.inspector.BinEdInspector;
import org.exbin.framework.bined.inspector.BinEdInspectorManager;
import org.exbin.framework.bined.inspector.BinEdInspectorProvider;
import org.exbin.framework.bined.inspector.BinedInspectorModule;
import org.exbin.framework.bined.kaitai.inspector.KaitaiInspector;
import org.exbin.framework.context.api.ContextChange;
import org.exbin.framework.context.api.ContextChangeRegistration;
import org.exbin.framework.language.api.LanguageModuleApi;
import org.exbin.framework.sidebar.api.SideBarComponent;
import org.exbin.framework.sidebar.api.SideBarDefinitionManagement;
import org.exbin.framework.sidebar.api.SideBarModuleApi;
import org.exbin.framework.ui.api.UiModuleApi;

/**
 * Binary editor plugin for Kaitai analyzer support.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class BinedKaitaiModule implements PluginModule {

    public static final String MODULE_ID = ModuleUtils.getModuleIdByApi(BinedKaitaiModule.class);
    public static final String SIDEBAR_COMPONENT_ID = "kaitai";

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
            // registerMenuActions();
            // registerPopupMenuActions();

            BinedModule binedModule = App.getModule(BinedModule.class);
            BinEdFileManager fileManager = binedModule.getFileManager();
            fileManager.addPainterColorModifier(kaitaiColorModifier);

            SideBarModuleApi sideBarModule = App.getModule(SideBarModuleApi.class);
            sideBarModule.setAutoShow(true);
            sideBarComponent.setBuildInDefinition("image/png.ksy");
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
        sideBarComponent = new KaitaiSideBarComponent();
        sideBarComponent.putValue(SideBarComponent.KEY_ID, SIDEBAR_COMPONENT_ID);
        //sideBarComponent.putValue(SideBarComponent.KEY_NAME, "KT");
        sideBarComponent.putValue(SideBarComponent.KEY_ICON, new javax.swing.ImageIcon(getClass().getResource(resourceBundle.getString("kaitaiSideBarComponent.icon"))));
        sideBarComponent.putValue(SideBarComponent.KEY_TOOLTIP, resourceBundle.getString("kaitaiSideBarComponent.toolTip"));
        sideBarComponent.putValue(SideBarComponent.KEY_CONTEXT_CHANGE, new ContextChange() {
            @Override
            public void register(ContextChangeRegistration registrar) {
                registrar.registerListener(ContextComponent.class, (instance) -> {
                    sideBarComponent.setActiveComponent(instance);
                });
            }
        });
        sideBarManager.registerSideBarComponent(sideBarComponent);
    }

    public void registerInspector() {
        BinedInspectorModule binedInspectorModule = App.getModule(BinedInspectorModule.class);
        BinEdInspectorManager inspectorManager = binedInspectorModule.getBinEdInspectorManager();
        inspectorManager.addInspector(new BinEdInspectorProvider() {

            private KaitaiInspector inspector;

            @Nonnull
            @Override
            public String getName() {
                return "Kaitai";
            }

            @Nonnull
            @Override
            public BinEdInspector createInspector() {
                if (inspector == null) {
                    inspector = new KaitaiInspector();
                }
                return inspector;
            }
        });
    }

    public void setBuildInDefinition(String ksyFilePath) {
        sideBarComponent.setBuildInDefinition(ksyFilePath);
    }

    @Nullable
    public KaitaiColorModifier getKaitaiColorModifier() {
        return kaitaiColorModifier;
    }
}
