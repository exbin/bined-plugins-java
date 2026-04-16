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

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JComponent;
import org.exbin.bined.CodeAreaCaretListener;
import org.exbin.bined.DataChangedListener;
import org.exbin.bined.operation.command.BinaryDataUndoRedo;
import org.exbin.bined.swing.CodeAreaCore;
import org.exbin.jaguif.App;
import org.exbin.bined.jaguif.inspector.BinEdInspector;
import org.exbin.bined.jaguif.kaitai.BinedKaitaiModule;
import org.exbin.bined.jaguif.kaitai.KaitaiSideManager;
import org.exbin.bined.jaguif.kaitai.inspector.gui.KaitaiInspectorPanel;
import org.exbin.bined.jaguif.kaitai.service.KaitaiTreeListener;

/**
 * Kaitai content inspector.
 */
@ParametersAreNonnullByDefault
public class KaitaiInspector implements BinEdInspector {

    protected KaitaiInspectorPanel component;
    protected CodeAreaCore codeArea;

    protected DataChangedListener dataChangedListener;
    protected CodeAreaCaretListener caretMovedListener;
    protected KaitaiTreeListener.SelectionListener nodeSelectionListener;

    @Nonnull
    @Override
    public JComponent getComponent() {
        if (component == null) {
            component = new KaitaiInspectorPanel();
            dataChangedListener = component::requestUpdate;
            caretMovedListener = (caretPosition) -> {
                component.requestUpdate();
            };
            nodeSelectionListener = new KaitaiTreeListener.SelectionListener() {
                @Override
                public void selectionChanged() {
                    component.requestUpdate();
                }
            };
        }
        return component;
    }

    @Override
    public void setCodeArea(CodeAreaCore codeArea, BinaryDataUndoRedo undoRedo) {
        this.codeArea = codeArea;
        component.setCodeArea(codeArea);
    }

    @Override
    public void activateSync() {
        BinedKaitaiModule kaitaiModule = App.getModule(BinedKaitaiModule.class);
        KaitaiSideManager kaitaiSideManager = kaitaiModule.getKaitaiSideManager();
        kaitaiSideManager.addNodeSelectionListener(codeArea, nodeSelectionListener);
//        codeArea.addDataChangedListener(dataChangedListener);
//        ((CaretCapable) codeArea).addCaretMovedListener(caretMovedListener);
    }

    @Override
    public void deactivateSync() {
        BinedKaitaiModule kaitaiModule = App.getModule(BinedKaitaiModule.class);
        KaitaiSideManager kaitaiSideManager = kaitaiModule.getKaitaiSideManager();
        kaitaiSideManager.removeNodeSelectionListener(codeArea, nodeSelectionListener);
//        codeArea.removeDataChangedListener(dataChangedListener);
//        ((CaretCapable) codeArea).removeCaretMovedListener(caretMovedListener);
    }
}
