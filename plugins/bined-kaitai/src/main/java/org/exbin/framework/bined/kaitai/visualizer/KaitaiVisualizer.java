package org.exbin.framework.bined.kaitai.visualizer;

import io.kaitai.struct.CompileLog;
import io.kaitai.struct.JavaRuntimeConfig;
import io.kaitai.struct.KaitaiStream;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.Main;
import io.kaitai.struct.RuntimeConfig;
import io.kaitai.struct.Version;
import io.kaitai.struct.format.ClassSpec;
import io.kaitai.struct.format.KSVersion;
import io.kaitai.struct.formats.JavaClassSpecs;
import io.kaitai.struct.formats.JavaKSYParser;
import io.kaitai.struct.languages.JavaCompiler$;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.swing.tree.DefaultTreeModel;
import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.framework.bined.kaitai.BinedKaitaiModule;
import org.exbin.framework.bined.kaitai.DefinitionRecord;
import org.exbin.framework.bined.kaitai.KaitaiSideManager;
import org.mdkt.compiler.InMemoryJavaCompiler;
import scala.Some;

@ParametersAreNonnullByDefault
public class KaitaiVisualizer {

    public static final String DEST_PACKAGE = "io.kaitai.struct.visualized";
    /**
     * Regexp with 2 groups: class name and type parameters. Type parameters
     * must be parsed with {@link #PARAMETER_NAME}.
     */
    private static final Pattern TOP_CLASS_NAME_AND_PARAMETERS = Pattern.compile(
            "public class (.+?) extends KaitaiStruct.*"
            + "public \\1\\(KaitaiStream _io, KaitaiStruct.* _parent, \\1 _root(.*?)\\)",
            Pattern.DOTALL
    );
    /**
     * Regexp, used to get parameter names from the generated source.
     */
    private static final Pattern PARAMETER_NAME = Pattern.compile(", \\S+ ([^,\\s]+)");

    protected Object struct;
    protected final DefaultTreeModel model = new DefaultTreeModel(null);

    public KaitaiVisualizer() {
        KSVersion.current_$eq(Version.version());
    }

    @Nonnull
    public DefaultTreeModel getModel() {
        return model;
    }

    @Nonnull
    public CompileResult compileDefinition(DefinitionRecord definitionRecord) {
        InputStream input = null;
        Reader inputReader = null;
        try {
            URL fileSource = definitionRecord.getUri().toURL();
            input = fileSource.openStream();
            inputReader = new InputStreamReader(input);
            ClassSpec classSpec = ClassSpec.fromYaml(JavaKSYParser.readerToYaml(inputReader), new Some<>("TEST"));
            final JavaClassSpecs specs = new JavaClassSpecs(null, null, classSpec);

            final RuntimeConfig config = new RuntimeConfig(
                    false,// autoRead - do not call `_read` automatically in constructor
                    true, // readStoresPos - enable generation of a position info which is accessed in DebugAids later
                    true, // opaqueTypes
                    false, // zeroCopySubstream
                    false, // readWrite
                    null, // cppConfig
                    null, // goPackage
                    new JavaRuntimeConfig(
                            DEST_PACKAGE,
                            // Class to be invoked in `fromFile` helper methods
                            "io.kaitai.struct.ByteBufferKaitaiStream",
                            // Exception class expected to be thrown on end-of-stream errors
                            "java.nio.BufferUnderflowException"
                    ),
                    null, // dotNetNamespace
                    null, // phpNamespace
                    null, // pythonPackage
                    null, // nimModule
                    null // nimOpaque
            );

            Main.importAndPrecompile(specs, config).value();
            final CompileLog.SpecSuccess result = Main.compile(specs, classSpec, JavaCompiler$.MODULE$, config);
            String javaSrc = result.files().apply(0).contents();
            final Matcher m = TOP_CLASS_NAME_AND_PARAMETERS.matcher(javaSrc);
            if (!m.find()) {
                throw new RuntimeException("Unable to find top-level class in generated .java");
            }
            // Parse parameter names
            final ArrayList<String> paramNames = new ArrayList<>();
            final Matcher p = PARAMETER_NAME.matcher(m.group(2));
            while (p.find()) {
                paramNames.add(p.group(1));
            }

            InMemoryJavaCompiler compiler = createInMemoryCompiler();
            final Class<?> ksyClass = compiler.compile(DEST_PACKAGE + "." + m.group(1), javaSrc);
            final Class<?> streamClass = compiler.getClassloader().loadClass("io.kaitai.struct.BinaryDataKaitaiStream");
            return new CompileResult(ksyClass, streamClass, paramNames);
        } catch (Exception ex) {
            return new CompileResult(ex.getMessage());
        } finally {
            if (inputReader != null) {
                try {
                    inputReader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    @Nonnull
    public ParsingResult parseData(Class<?> ksyClass, Class<?> streamClass, List<String> paramNames, EditableBinaryData sourceData) {
        try {
            struct = construct(ksyClass, streamClass, paramNames, sourceData);

            // Find and run "_read" that does actual parsing
            // TODO: wrap this in try-catch block
            Method readMethod = ksyClass.getMethod("_read");
            readMethod.invoke(struct);

            loadStruct();
            return new ParsingResult(null);
        } catch (Exception ex) {
            return new ParsingResult(ex.getMessage());
        }
    }

    private void loadStruct() throws IOException {
        final DataNode root = new DataNode(0, struct, "[root]");
        model.setRoot(root);
        root.explore(model, null);
    }

    @Nonnull
    private static Object construct(Class<?> ksyClass, Class<?> streamClass, List<String> paramNames, EditableBinaryData sourceData) throws Exception {
        final Constructor<?> streamConstructor = streamClass.getDeclaredConstructors()[0];
        Object sourceStream = streamConstructor.newInstance(sourceData);
        final Constructor<?> ksyConstructor = findKsyConstructor(ksyClass);
        final Class<?>[] types = ksyConstructor.getParameterTypes();
        final Object[] args = new Object[types.length];
        args[0] = sourceStream;
        for (int i = 3; i < args.length; ++i) {
            args[i] = getDefaultValue(types[i]);
        }
        // TODO: get parameters from user
        return ksyConstructor.newInstance(args);
    }

    @Nonnull
    private static <T> Constructor<T> findKsyConstructor(Class<T> ksyClass) {
        for (final Constructor c : ksyClass.getDeclaredConstructors()) {
            final Class<?>[] types = c.getParameterTypes();
            if (types.length >= 3
                    && KaitaiStream.class.getCanonicalName().equals(types[0].getCanonicalName())
                    && KaitaiStruct.ReadOnly.class.getCanonicalName().equals(types[1].getCanonicalName())
                    && ksyClass.getCanonicalName().equals(types[2].getCanonicalName())) {
                return c;
            }
        }
        throw new IllegalArgumentException(ksyClass + " has no KaitaiStruct-generated constructor");
    }

    @Nullable
    private static Object getDefaultValue(Class<?> clazz) {
        if (clazz == boolean.class) {
            return false;
        }
        if (clazz == char.class) {
            return (char) 0;
        }
        if (clazz == byte.class) {
            return (byte) 0;
        }
        if (clazz == short.class) {
            return (short) 0;
        }
        if (clazz == int.class) {
            return 0;
        }
        if (clazz == long.class) {
            return 0L;
        }
        if (clazz == float.class) {
            return 0.0f;
        }
        if (clazz == double.class) {
            return 0.0;
        }
        return null;
    }

    @Nonnull
    private InMemoryJavaCompiler createInMemoryCompiler() {
        InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();
        String runtimePath = "/runtime/";
        FileSystem fileSystem = null;
        try {
            Path rootPath = null;
            try {
                URI runtimeFiles = getClass().getResource(runtimePath).toURI();
                fileSystem = FileSystems.newFileSystem(runtimeFiles, Collections.<String, Object>emptyMap());
                rootPath = fileSystem.getPath(runtimePath);
            } catch (URISyntaxException | IOException ex) {
                Logger.getLogger(BinedKaitaiModule.class.getName()).log(Level.SEVERE, null, ex);
            }

            List<Path> paths = new ArrayList<>();
            paths.add(rootPath);
            while (!paths.isEmpty()) {
                Path path = paths.remove(paths.size() - 1);
                try {
                    Stream<Path> walk = Files.walk(path, 1);
                    for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
                        Path childPath = it.next();
                        if (childPath == path) {
                            continue;
                        }

                        String fileName = "";
                        if (childPath.getNameCount() > 0) {
                            fileName = childPath.getName(childPath.getNameCount() - 1).toString();
                        }
                        if (fileName.endsWith(File.separator)) {
                            paths.add(childPath);
                        } else if (fileName.endsWith(".java")) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(childPath)));
                            String line;
                            StringBuilder stringBuilder = new StringBuilder();
                            String ls = System.getProperty("line.separator");
                            try {
                                while ((line = reader.readLine()) != null) {
                                    stringBuilder.append(line);
                                    stringBuilder.append(ls);
                                }

                                String className = path.toString().substring(9).replace("/", ".");
                                className += fileName.substring(0, fileName.length() - 5);
                                compiler.addSource(className, stringBuilder.toString());
                            } catch (Exception ex) {
                                Logger.getLogger(KaitaiSideManager.class.getName()).log(Level.SEVERE, null, ex);
                            } finally {
                                reader.close();
                            }
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(BinedKaitaiModule.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } finally {
            try {
                if (fileSystem != null) {
                    fileSystem.close();
                }
            } catch (Throwable tw) {
                // ignore
            }
        }

        return compiler;
    }

    @Immutable
    @ParametersAreNonnullByDefault
    public static class CompileResult {

        private final Class<?> ksyClass;
        private final Class<?> streamClass;
        private final List<String> paramNames;
        private final String errorMessage;

        public CompileResult(Class<?> ksyClass, Class<?> streamClass, List<String> paramNames) {
            this.ksyClass = ksyClass;
            this.streamClass = streamClass;
            this.paramNames = paramNames;
            this.errorMessage = null;
        }

        public CompileResult(String errorMessage) {
            this.ksyClass = null;
            this.streamClass = null;
            this.paramNames = null;
            this.errorMessage = errorMessage;
        }

        @Nullable
        public Class<?> getKsyClass() {
            return ksyClass;
        }

        @Nullable
        public Class<?> getStreamClass() {
            return streamClass;
        }

        @Nullable
        public List<String> getParamNames() {
            return paramNames;
        }

        @Nullable
        public String getErrorMessage() {
            return errorMessage;
        }
    }

    @Immutable
    public static class ParsingResult {

        private final String errorMessage;

        public ParsingResult(@Nullable String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Nullable
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
