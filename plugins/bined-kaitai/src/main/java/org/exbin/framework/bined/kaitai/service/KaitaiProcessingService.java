package org.exbin.framework.bined.kaitai.service;

import io.kaitai.struct.Version;
import io.kaitai.struct.format.KSVersion;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.tree.DefaultTreeModel;
import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.framework.bined.kaitai.DefinitionRecord;

/**
 * Kaitai processing service.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class KaitaiProcessingService {

    protected Object struct;
    protected final DefaultTreeModel model = new DefaultTreeModel(null);

    public KaitaiProcessingService() {
        KSVersion.current_$eq(Version.version());
    }

    @Nonnull
    public DefaultTreeModel getModel() {
        return model;
    }

    @Nonnull
    public KaitaiCompiler.CompileResult compileDefinition(KaitaiCompiler compiler, DefinitionRecord definitionRecord) {
        return compiler.compileDefinition(definitionRecord);
    }
    
    @Nonnull
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
        root.explore(model, null);
    }
}
