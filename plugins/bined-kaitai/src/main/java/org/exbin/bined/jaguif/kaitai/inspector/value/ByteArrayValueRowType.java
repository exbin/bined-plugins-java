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
package org.exbin.bined.jaguif.kaitai.inspector.value;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.bined.jaguif.kaitai.inspector.api.ValueRowItem;
import org.exbin.bined.jaguif.kaitai.inspector.api.ValueRowType;

/**
 * Byte array value type.
 */
@ParametersAreNonnullByDefault
public class ByteArrayValueRowType implements ValueRowType {

    protected String propertyName;
    protected long position;
    protected int length;

    public ByteArrayValueRowType(String propertyName, long position, int length) {
        this.propertyName = propertyName;
        this.position = position;
        this.length = length;
    }

    @Nonnull
    @Override
    public String getId() {
        return "byteArray";
    }

    @Nonnull
    @Override
    public String getName() {
        return "byte[]";
    }

    @Nonnull
    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Nonnull
    @Override
    public ValueRowItem createRowItem() {
        return new ValueRowItem(getId(), propertyName, byte[].class.getTypeName(), position, null) {
            @Override
            public void updateRow(byte[] values, int available) {
                if (available < 1) {
                    setValue(null);
                    return;
                }

                int capLength = available > length ? available : length;
                if (values.length < capLength) {
                    capLength = values.length;
                }
                byte[] value = new byte[capLength];
                System.arraycopy(values, 0, value, 0, capLength);
                setValue(value);
            }
        };
    }
}
