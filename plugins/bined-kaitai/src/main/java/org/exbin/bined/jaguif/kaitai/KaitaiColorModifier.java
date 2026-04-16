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

import java.awt.Color;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.bined.CodeAreaSection;
import org.exbin.bined.swing.CodeAreaColorAssessor;
import org.exbin.bined.swing.CodeAreaPaintState;
import org.exbin.jaguif.utils.UiUtils;

/**
 * Kaitai color modifier.
 */
@ParametersAreNonnullByDefault
public class KaitaiColorModifier implements CodeAreaColorAssessor {

    protected CodeAreaColorAssessor parentAssessor;
    protected long position = -1;
    protected long length;
    protected Color color;
    protected long outerPosition = -1;
    protected long outerLength;
    protected Color outerColor;

    public KaitaiColorModifier() {
        this(null);
    }

    public KaitaiColorModifier(@Nullable CodeAreaColorAssessor parentAssessor) {
        this.parentAssessor = parentAssessor;
    }

    @Nullable
    @Override
    public Color getPositionBackgroundColor(long rowDataPosition, int byteOnRow, int charOnRow, CodeAreaSection section, boolean inSelection) {
        if (position >= 0) {
            long dataPosition = rowDataPosition + byteOnRow;
            if (dataPosition >= position && dataPosition < position + length) {
                return color;
            }
        }

        if (outerPosition >= 0) {
            long dataPosition = rowDataPosition + byteOnRow;
            if (dataPosition >= outerPosition && dataPosition < outerPosition + outerLength) {
                return outerColor;
            }
        }

        return null;
    }

    @Nullable
    @Override
    public Color getPositionTextColor(long rowDataPosition, int byteOnRow, int charOnRow, CodeAreaSection section, boolean inSelection) {
        return null;
    }

    @Nonnull
    @Override
    public Optional<CodeAreaColorAssessor> getParentColorAssessor() {
        return Optional.ofNullable(parentAssessor);
    }

    @Override
    public void startPaint(CodeAreaPaintState codeAreaPaintState) {
        color = UiUtils.isDarkUI() ? Color.GREEN.darker().darker() : Color.GREEN;
        outerColor = UiUtils.isDarkUI()
                ? new Color(color.getRed() / 2, color.getGreen() / 2, color.getBlue() / 2)
                : new Color((511 + color.getRed()) / 3, (511 + color.getGreen()) / 3, (511 + color.getBlue()) / 3);
    }

    public void setRange(long position, long length) {
        this.position = position;
        this.length = length;
    }

    public void clearRange() {
        this.position = -1;
        this.length = 0;
    }

    public void setOuterRange(long position, long length) {
        this.outerPosition = position;
        this.outerLength = length;
    }

    public void clearOuterRange() {
        this.outerPosition = -1;
        this.outerLength = 0;
    }

    /* TODO
    public void setColor(Color color) {
        this.color = color;
    }
     */
}
