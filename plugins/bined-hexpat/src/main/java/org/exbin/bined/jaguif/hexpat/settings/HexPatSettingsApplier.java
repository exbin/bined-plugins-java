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
package org.exbin.bined.jaguif.hexpat.settings;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.exbin.jaguif.App;
import org.exbin.jaguif.context.api.ContextComponent;
import org.exbin.jaguif.context.api.ContextStateProvider;
import org.exbin.bined.jaguif.component.BinaryDataComponent;
import org.exbin.bined.jaguif.hexpat.BinedHexPatModule;
import org.exbin.bined.jaguif.hexpat.DefinitionRecord;
import org.exbin.bined.jaguif.hexpat.HexPatSideManager;
import org.exbin.bined.jaguif.hexpat.gui.HexPatSidePanel;
import org.exbin.jaguif.options.settings.api.SettingsApplier;
import org.exbin.jaguif.options.settings.api.SettingsOptionsProvider;

/**
 * HexPat settings applier.
 */
@NullMarked
public class HexPatSettingsApplier implements SettingsApplier {

    public static final String APPLIER_ID = "hexpat";

    @Override
    public void applySettings(ContextStateProvider contextProvider, SettingsOptionsProvider settingsProvider) {
        ContextComponent instance = contextProvider.getActiveState(ContextComponent.class);
        if (!(instance instanceof BinaryDataComponent)) {
            return;
        }

        BinedHexPatModule hexPatModule = App.getModule(BinedHexPatModule.class);
        HexPatSideManager hexPatSideManager = hexPatModule.getHexPatSideManager();
        HexPatOptions options = settingsProvider.getSettingsOptions(HexPatOptions.class);
        HexPatSidePanel sidePanel = hexPatSideManager.getSidePanel();
        List<DefinitionRecord> definitionRecords = new ArrayList<>();
        int listSize = options.getListSize();
        for (int i = 0; i < listSize; i++) {
            String buildIn = options.getListItemBuildIn(i);
            DefinitionRecord record;
            if (buildIn.isEmpty()) {
                String path = options.getListItemPath(i);
                record = hexPatModule.getDefinitionByPath(URI.create(path));
            } else {
                record = hexPatModule.getBuildInDefinition(buildIn);
            }
            definitionRecords.add(record);
        }

        // Demo
        // definitionRecords.add(hexPatModule.getBuildInDefinition("image/png.ksy"));

        sidePanel.setDefinitions(definitionRecords);
        sidePanel.setSelectedDefinition(options.getDefinition());
    }
}
