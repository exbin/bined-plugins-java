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

import io.kaitai.struct.KaitaiStream;
import io.kaitai.struct.KaitaiStruct;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import org.exbin.auxiliary.binary_data.EditableBinaryData;

/**
 * Kaitai parser.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class KaitaiParser {

    protected final DefinitionRecord definitionRecord;
    protected final Class<?> ksyClass;
    protected final Class<?> streamClass;
    protected final List<String> paramNames;

    public KaitaiParser(DefinitionRecord definitionRecord, Class<?> ksyClass, Class<?> streamClass, List<String> paramNames) {
        this.definitionRecord = definitionRecord;
        this.ksyClass = ksyClass;
        this.streamClass = streamClass;
        this.paramNames = paramNames;
    }

    @Nonnull
    public DefinitionRecord getDefinitionRecord() {
        return definitionRecord;
    }

    @Nonnull
    public Class<?> getKsyClass() {
        return ksyClass;
    }

    @Nonnull
    public Class<?> getStreamClass() {
        return streamClass;
    }

    @Nonnull
    public List<String> getParamNames() {
        return paramNames;
    }
    
    @Nonnull
    public ParsingResult parse(EditableBinaryData sourceData) {
        try {
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
            Object struct = ksyConstructor.newInstance(args);

            // Find and run "_read" that does actual parsing
            // TODO: wrap this in try-catch block
            Method readMethod = ksyClass.getMethod("_read");
            readMethod.invoke(struct);

            return new ParsingResult(struct);
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String message = sw.toString();
            return new ParsingResult(message == null ? "" : message);
        }
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

    @Immutable
    @ParametersAreNonnullByDefault
    public static class ParsingResult {
        
        private final Object struct;
        private final String errorMessage;

        public ParsingResult(Object struct) {
            this.struct = struct;
            this.errorMessage = null;
        }

        public ParsingResult(@Nullable String errorMessage) {
            this.struct = null;
            this.errorMessage = errorMessage;
        }

        @Nullable
        public Object getStruct() {
            return struct;
        }

        @Nullable
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
