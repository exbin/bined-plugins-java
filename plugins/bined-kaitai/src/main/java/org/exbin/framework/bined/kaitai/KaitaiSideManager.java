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

import io.kaitai.struct.JavaMain;
import io.kaitai.struct.languages.JavaCompiler;
import io.kaitai.struct.languages.components.LanguageCompiler;
import java.net.URI;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.framework.bined.kaitai.gui.KaitaiSidePanel;

/**
 * Kaitai side manager.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class KaitaiSideManager {

    protected KaitaiSidePanel sidePanel = new KaitaiSidePanel();

    @Nonnull
    public KaitaiSidePanel getSidePanel() {
        return sidePanel;
    }

    public void loadFrom(DefinitionRecord definitionRecord) {
        URI fileSource = definitionRecord.getUri();
        sidePanel.setCurrentDef(definitionRecord.getFileName());
//        io.kaitai.struct.JavaMain main = new JavaMain(config);
//        LanguageCompiler compiler = JavaCompiler.getCompiler(tp, config);
    }
}
