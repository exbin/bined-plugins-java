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
package org.exbin.bined.jaguif.kaitai.inspector;

import java.util.ResourceBundle;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.bined.jaguif.inspector.BinEdInspector;
import org.exbin.bined.jaguif.inspector.BinEdInspectorProvider;

/**
 * Kaitai content inspector provider.
 */
@ParametersAreNonnullByDefault
public class KaitaiInspectorProvider implements BinEdInspectorProvider {

    public static final String INSPECTOR_ID = "kaitai";

    protected KaitaiInspector inspector;
    protected final ResourceBundle resourceBundle;

    public KaitaiInspectorProvider(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    @Nonnull
    @Override
    public String getId() {
        return INSPECTOR_ID;
    }

    @Nonnull
    @Override
    public String getName() {
        return resourceBundle.getString("kaitaiInspector.name");
    }

    @Nonnull
    @Override
    public BinEdInspector createInspector() {
        if (inspector == null) {
            inspector = new KaitaiInspector();
        }
        return inspector;
    }
}
