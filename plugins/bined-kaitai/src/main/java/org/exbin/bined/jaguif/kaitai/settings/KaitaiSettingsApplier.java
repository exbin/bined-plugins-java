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
package org.exbin.bined.jaguif.kaitai.settings;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.jaguif.App;
import org.exbin.jaguif.action.api.ContextComponent;
import org.exbin.bined.jaguif.component.BinaryDataComponent;
import org.exbin.bined.jaguif.kaitai.BinedKaitaiModule;
import org.exbin.bined.jaguif.kaitai.DefinitionRecord;
import org.exbin.bined.jaguif.kaitai.KaitaiSideManager;
import org.exbin.bined.jaguif.kaitai.gui.KaitaiSidePanel;
import org.exbin.jaguif.context.api.ActiveContextProvider;
import org.exbin.jaguif.options.settings.api.SettingsApplier;
import org.exbin.jaguif.options.settings.api.SettingsOptionsProvider;

/**
 * Kaitai settings applier.
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

        // Demo
        // definitionRecords.add(kaitaiModule.getBuildInDefinition("image/png.ksy"));

        sidePanel.setDefinitions(definitionRecords);
        sidePanel.setSelectedDefinition(options.getDefinition());
    }
}
