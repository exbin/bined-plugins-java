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
package org.exbin.framework.bined.kaitai.service;

import io.kaitai.struct.CompileLog;
import io.kaitai.struct.JavaRuntimeConfig;
import io.kaitai.struct.Main;
import io.kaitai.struct.RuntimeConfig;
import io.kaitai.struct.format.ClassSpec;
import io.kaitai.struct.formats.JavaClassSpecs;
import io.kaitai.struct.formats.JavaKSYParser;
import io.kaitai.struct.languages.JavaCompiler$;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.dvare.dynamic.compiler.DynamicCompiler;
import org.exbin.framework.bined.kaitai.DefinitionRecord;
import scala.Some;

/**
 * Kaitai compiler.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class KaitaiCompiler {

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

    @Nonnull
    public CompileResult compileDefinition(DefinitionRecord definitionRecord) {
        InputStream input = null;
        Reader inputReader = null;
        try {
            URL fileSource = definitionRecord.getUri().toURL();
            input = fileSource.openStream();
            inputReader = new InputStreamReader(input, "UTF-8");
            Object yamlSpec = JavaKSYParser.readerToYaml(inputReader);
            ClassSpec classSpec = ClassSpec.fromYaml(yamlSpec, new Some<>(definitionRecord.getFileName()));
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
            // TODO: There is some king of racing condition in the current implementation of the KaiTai compiler, wait a bit
            Thread.sleep(200);
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

            String wrapperClassSrc = "package " + DEST_PACKAGE + ";\n"
                    + "public class DataWrapper {\n"
                    + "public static Class getKsyClass() { return " + m.group(1) + ".class; }\n"
                    + "public static Class getStreamClass() { return io.kaitai.struct.BinaryDataKaitaiStream.class; }\n"
                    + "}\n";

            ClassLoader classLoader = getClass().getClassLoader();
            JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
            if (javaCompiler == null) {
                Class compilerClass = classLoader.loadClass("com.sun.tools.javac.api.JavacTool");
                javaCompiler = (JavaCompiler) compilerClass.getConstructor().newInstance();
            }
            DynamicCompiler compiler = new DynamicCompiler(classLoader, javaCompiler);
            compiler.addSource(DEST_PACKAGE + "." + m.group(1), javaSrc);
            compiler.addSource(DEST_PACKAGE + "." + "DataWrapper", wrapperClassSrc);
            Map<String, Class<?>> compiledClasses = compiler.build();
            final Class<?> wrapperClass = compiledClasses.get(DEST_PACKAGE + "." + "DataWrapper");
            final Class<?> ksyClass = (Class<?>) wrapperClass.getMethod("getKsyClass").invoke(null);
            final Class<?> streamClass = (Class<?>) wrapperClass.getMethod("getStreamClass").invoke(null);
            KaitaiParser parser = new KaitaiParser(definitionRecord, ksyClass, streamClass, paramNames);
            return new CompileResult(parser);
        } catch (Throwable ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String message = sw.toString();
            return new CompileResult(message == null ? "" : message);
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

    @Immutable
    @ParametersAreNonnullByDefault
    public static class CompileResult {

        private final KaitaiParser parser;
        private final String errorMessage;

        public CompileResult(KaitaiParser parser) {
            this.parser = parser;
            this.errorMessage = null;
        }

        public CompileResult(String errorMessage) {
            this.parser = null;
            this.errorMessage = errorMessage;
        }

        @Nullable
        public KaitaiParser getParser() {
            return parser;
        }

        @Nullable
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
