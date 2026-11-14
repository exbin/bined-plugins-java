package org.exbin.bined.jaguif.hexpat.service;

import org.jspecify.annotations.NullMarked;
import javax.swing.tree.DefaultTreeModel;

/**
 * HexPat processing service.
 */
@NullMarked
public class HexPatProcessingService {

    protected Object struct;
    protected final DefaultTreeModel model = new DefaultTreeModel(null);

    public HexPatProcessingService() {
        
    }

    public DefaultTreeModel getModel() {
        return model;
    }
}
