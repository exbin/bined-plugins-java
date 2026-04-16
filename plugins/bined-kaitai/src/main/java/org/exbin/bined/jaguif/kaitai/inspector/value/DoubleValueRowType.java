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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.bined.jaguif.kaitai.inspector.api.ValueRowItem;
import org.exbin.bined.jaguif.kaitai.inspector.api.ValueRowType;

/**
 * Double value type.
 */
@ParametersAreNonnullByDefault
public class DoubleValueRowType implements ValueRowType {

    protected String propertyName;
    protected long position;
    protected ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

    public DoubleValueRowType(String propertyName, long position) {
        this.propertyName = propertyName;
        this.position = position;
    }

    @Nonnull
    @Override
    public String getId() {
        return "double";
    }

    @Nonnull
    @Override
    public String getName() {
        return "Double";
    }

    @Nonnull
    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Nonnull
    @Override
    public ValueRowItem createRowItem() {
        return new ValueRowItem(getId(), propertyName, Short.class.getTypeName(), position, null) {
            @Override
            public void updateRow(byte[] values, int available) {
                if (available < 8) {
                    setValue(null);
                    return;
                }

                int length = Math.min(available, 8);

                ByteBuffer byteBuffer = ByteBuffer.allocate(8);
                byteBuffer.put(values, 0, length);
                byteBuffer.rewind();
                if (byteBuffer.order() != byteOrder) {
                    byteBuffer.order(byteOrder);
                }

                setValue(byteBuffer.getDouble());
            }
        };
    }
}
