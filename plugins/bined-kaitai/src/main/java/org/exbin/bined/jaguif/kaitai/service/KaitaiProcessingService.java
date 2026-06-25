package org.exbin.bined.jaguif.kaitai.service;

import io.kaitai.struct.Version;
import io.kaitai.struct.format.KSVersion;
import org.jspecify.annotations.NullMarked;
import javax.swing.tree.DefaultTreeModel;
import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.bined.jaguif.kaitai.DefinitionRecord;

/**
 * Kaitai processing service.
 */
@NullMarked
public class KaitaiProcessingService {

    protected Object struct;
    protected final DefaultTreeModel model = new DefaultTreeModel(null);

    public KaitaiProcessingService() {
        KSVersion.current_$eq(Version.version());
    }

    public DefaultTreeModel getModel() {
        return model;
    }

    public KaitaiCompiler.CompileResult compileDefinition(KaitaiCompiler compiler, DefinitionRecord definitionRecord) {
        return compiler.compileDefinition(definitionRecord);
    }
    
    public KaitaiParser.ParsingResult parseData(KaitaiParser parser, EditableBinaryData sourceData) {
        KaitaiParser.ParsingResult result = parser.parse(sourceData);
        if (result.getErrorMessage() == null) {
            this.struct = result.getStruct();
            loadStruct();
        }

        return result;
    }

    private void loadStruct() {
        final DataNode root = new DataNode(0, struct, "[root]");
        model.setRoot(root);
        root.explore(() -> {
            model.nodeStructureChanged(root);
        }, null);
    }
}
