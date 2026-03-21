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
package org.exbin.framework.bined.kaitai.inspector.value;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.framework.bined.kaitai.inspector.api.ValueRowItem;
import org.exbin.framework.bined.kaitai.inspector.api.ValueRowType;
import org.exbin.framework.bined.objectdata.source.CharBufferPageProvider;

/**
 * Byte value type.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class StringValueRowType implements ValueRowType {

    protected String propertyName;
    protected long position;
    protected int length;

    public StringValueRowType(String propertyName, long position, int length) {
        this.propertyName = propertyName;
        this.position = position;
        this.length = length;
    }

    @Nonnull
    @Override
    public String getId() {
        return "string";
    }

    @Nonnull
    @Override
    public String getName() {
        return "String";
    }

    @Nonnull
    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Nonnull
    @Override
    public ValueRowItem createRowItem() {
        return new ValueRowItem(getId(), propertyName, String.class.getTypeName(), position, null) {
            @Override
            public void updateRow(byte[] values, int available) {
                if (available < 1) {
                    setValue(null);
                    return;
                }

                String value;
                try {
                    value = new String(values, 0, length, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    value = "";
                }
                setValue(String.valueOf(value));
            }
        };
    }
}
