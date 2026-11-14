package org.exbin.bined.jaguif.hexpat.service.hexpat;

import java.awt.Color;
import java.util.Optional;
import org.exbin.bined.CodeAreaSection;
import org.exbin.bined.swing.CodeAreaColorAssessor;
import org.exbin.bined.swing.CodeAreaCore;
import org.exbin.bined.swing.CodeAreaPaintState;
import org.jspecify.annotations.NullMarked;

/**
 * Code area painter which colours bytes covered by the evaluated .hexpat pattern.
 *
 * Selection and search-match highlighting keep priority over the pattern colours.
 */
@NullMarked
public class HexPatHighlightPainter implements CodeAreaColorAssessor {

    private final HexPatColorizer colorizer;

    public HexPatHighlightPainter(CodeAreaCore codeArea, HexPatColorizer colorizer) {
//        super(codeArea);
        this.colorizer = colorizer;
    }

    @Override
    public Color getPositionBackgroundColor(long rowDataPosition, int byteOnRow, int charOnRow,
            CodeAreaSection section, boolean inSelection) {
        return null;
        /* Color base = super.getPositionBackgroundColor(rowDataPosition, byteOnRow, charOnRow, section, inSelection);
        if (inSelection) {
            return base;
        }
        if (base != null && (base.equals(getFoundMatchesBackgroundColor()) || base.equals(getCurrentMatchBackgroundColor()))) {
            return base;
        }
        Color pattern = colorizer.backgroundAt(rowDataPosition + byteOnRow);
        return pattern != null ? pattern : base; */
    }

    @Override
    public Color getPositionTextColor(long rowDataPosition, int offsetDataPosition, int columnPosition, CodeAreaSection section, boolean inSelection) {
        return null;
    }

    @Override
    public Optional<CodeAreaColorAssessor> getParentColorAssessor() {
        return Optional.empty();
    }

    @Override
    public void startPaint(CodeAreaPaintState codeAreaPaintState) {
    }
}
