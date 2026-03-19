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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.framework.bined.kaitai.inspector.api.ValueRowItem;
import org.exbin.framework.bined.kaitai.inspector.api.ValueRowType;

/**
 * Float value type.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class FloatValueRowType implements ValueRowType {

    protected String propertyName;
    protected long position;
    protected ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

    public FloatValueRowType(String propertyName, long position) {
        this.propertyName = propertyName;
        this.position = position;
    }

    @Nonnull
    @Override
    public String getId() {
        return "float";
    }

    @Nonnull
    @Override
    public String getName() {
        return "Float";
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
                if (available < 4) {
                    setValue(null);
                    return;
                }
                
                int length = Math.min(available, 4);

                ByteBuffer byteBuffer = ByteBuffer.allocate(4);
                byteBuffer.put(values, 0, length);
                byteBuffer.rewind();
                if (byteBuffer.order() != byteOrder) {
                    byteBuffer.order(byteOrder);
                }

                setValue(byteBuffer.getFloat());
            }
        };
    }
}
