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
package org.exbin.framework.bined.kaitai.settings;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.framework.App;
import org.exbin.framework.action.api.ContextComponent;
import org.exbin.framework.bined.BinaryDataComponent;
import org.exbin.framework.bined.kaitai.BinedKaitaiModule;
import org.exbin.framework.bined.kaitai.DefinitionRecord;
import org.exbin.framework.bined.kaitai.KaitaiSideManager;
import org.exbin.framework.bined.kaitai.gui.KaitaiSidePanel;
import org.exbin.framework.context.api.ActiveContextProvider;
import org.exbin.framework.options.settings.api.SettingsApplier;
import org.exbin.framework.options.settings.api.SettingsOptionsProvider;

/**
 * Kaitai settings applier.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class KaitaiSettingsApplier implements SettingsApplier {

    public static final String APPLIER_ID = "kaitai";

    @Override
    public void applySettings(ActiveContextProvider contextProvider, SettingsOptionsProvider settingsProvider) {
        ContextComponent instance = contextProvider.getActiveState(ContextComponent.class);
        if (!(instance instanceof BinaryDataComponent)) {
            return;
        }

        BinedKaitaiModule kaitaiModule = App.getModule(BinedKaitaiModule.class);
        KaitaiSideManager kaitaiSideManager = kaitaiModule.getKaitaiSideManager();
        KaitaiOptions options = settingsProvider.getSettingsOptions(KaitaiOptions.class);
        KaitaiSidePanel sidePanel = kaitaiSideManager.getSidePanel();
        List<DefinitionRecord> definitionRecords = new ArrayList<>();
        int listSize = options.getListSize();
        for (int i = 0; i < listSize; i++) {
            String buildIn = options.getListItemBuildIn(i);
            DefinitionRecord record;
            if (buildIn.isEmpty()) {
                String path = options.getListItemPath(i);
                record = kaitaiModule.getDefinitionByPath(URI.create(path));
            } else {
                record = kaitaiModule.getBuildInDefinition(buildIn);
            }
            definitionRecords.add(record);
        }
        sidePanel.setDefinitions(definitionRecords);
        sidePanel.setSelectedDefinition(options.getDefinition());
    }
}
