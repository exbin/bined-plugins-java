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
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.auxiliary.binary_data.EditableBinaryData;

/**
 * Binary data kaitai stream.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class BinaryDataKaitaiStream extends KaitaiStream {

    protected final EditableBinaryData binaryData;
    protected long position;

    public BinaryDataKaitaiStream(EditableBinaryData binaryData) {
        this.binaryData = binaryData;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean isEof() {
        return position >= binaryData.getDataSize();
    }

    @Override
    public void seek(int position) {
        seek((long) position);
    }

    @Override
    public void seek(long position) {
        if (bitsWriteMode) {
            writeAlignToByte();
        } else {
            alignToByte();
        }

        this.position = position;
    }

    @Override
    public int pos() {
        return (int) position;
    }

    @Override
    public long size() {
        return binaryData.getDataSize();
    }

    @Override
    public byte readS1() {
        alignToByte();
        if (position + 1 >= size()) {
            throw new RuntimeException(new EOFException());
        }

        byte result = binaryData.getByte(position);
        position++;
        return result;
    }

    @Override
    public short readS2be() {
        alignToByte();
        if (position + 2 >= size()) {
            throw new RuntimeException(new EOFException());
        }

        byte b1 = binaryData.getByte(position);
        byte b2 = binaryData.getByte(position + 1);
        position += 2;
        return (short) ((b1 << 8) | (b2 & 0xff));
    }

    @Override
    public int readS4be() {
        alignToByte();
        if (position + 4 >= size()) {
            throw new RuntimeException(new EOFException());
        }

        byte b1 = binaryData.getByte(position);
        byte b2 = binaryData.getByte(position + 1);
        byte b3 = binaryData.getByte(position + 2);
        byte b4 = binaryData.getByte(position + 3);
        position += 4;
        return (b1 << 24) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 8) | (b4 & 0xff);
    }

    @Override
    public long readS8be() {
        alignToByte();
        long b1 = readU4be();
        long b2 = readU4be();
        return (b1 << 32) + b2;
    }

    @Override
    public short readS2le() {
        alignToByte();
        if (position + 2 >= size()) {
            throw new RuntimeException(new EOFException());
        }

        byte b1 = binaryData.getByte(position);
        byte b2 = binaryData.getByte(position + 1);
        position += 2;
        return (short) ((b2 << 8) + (b1 & 0xff));
    }

    @Override
    public int readS4le() {
        alignToByte();
        if (position + 4 >= size()) {
            throw new RuntimeException(new EOFException());
        }

        byte b1 = binaryData.getByte(position);
        byte b2 = binaryData.getByte(position + 1);
        byte b3 = binaryData.getByte(position + 2);
        byte b4 = binaryData.getByte(position + 3);
        position += 4;
        return (b4 << 24) | ((b3 & 0xff) << 16) | ((b2 & 0xff) << 8) | (b1 & 0xff);
    }

    @Override
    public long readS8le() {
        alignToByte();
        long b1 = readU4le();
        long b2 = readU4le();
        return (b2 << 32) + b1;
    }

    @Override
    public int readU1() {
        alignToByte();
        if (position + 1 >= size()) {
            throw new RuntimeException(new EOFException());
        }

        byte result = binaryData.getByte(position);
        position++;
        return result & 0xff;
    }

    @Override
    public int readU2be() {
        alignToByte();
        if (position + 2 >= size()) {
            throw new RuntimeException(new EOFException());
        }

        byte b1 = binaryData.getByte(position);
        byte b2 = binaryData.getByte(position + 1);
        position += 2;
        return (short) ((b1 << 8) | (b2 | 0xff));
    }

    @Override
    public long readU4be() {
        alignToByte();
        if (position + 4 >= size()) {
            throw new RuntimeException(new EOFException());
        }

        byte b1 = binaryData.getByte(position);
        byte b2 = binaryData.getByte(position + 1);
        byte b3 = binaryData.getByte(position + 2);
        byte b4 = binaryData.getByte(position + 3);
        position += 4;
        return ((b1 & 0xff) << 24) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 8) | (b4 & 0xff);
    }

    @Override
    public int readU2le() {
        alignToByte();
        if (position + 2 >= size()) {
            throw new RuntimeException(new EOFException());
        }

        byte b1 = binaryData.getByte(position);
        byte b2 = binaryData.getByte(position + 1);
        position += 2;
        return (short) (((b2 & 0xff) << 8) | (b1 & 0xff));
    }

    @Override
    public long readU4le() {
        alignToByte();
        if (position + 4 >= size()) {
            throw new RuntimeException(new EOFException());
        }

        byte b1 = binaryData.getByte(position);
        byte b2 = binaryData.getByte(position + 1);
        byte b3 = binaryData.getByte(position + 2);
        byte b4 = binaryData.getByte(position + 3);
        position += 4;
        return ((b4 & 0xff) << 24) | ((b3 & 0xff) << 16) | ((b2 & 0xff) << 8) | (b1 & 0xff);
    }

    @Override
    public float readF4be() {
        return Float.intBitsToFloat(readS4be());
    }

    @Override
    public double readF8be() {
        return Double.longBitsToDouble(readS8be());
    }

    @Override
    public float readF4le() {
        return Float.intBitsToFloat(readS4le());
    }

    @Override
    public double readF8le() {
        return Double.longBitsToDouble(readS8le());
    }

    @Override
    protected byte[] readBytesNotAligned(long length) {
        int arrayLength = toByteArrayLength(length);
        byte[] buf = new byte[arrayLength];
        binaryData.copyToArray(position, buf, 0, arrayLength);
        position += arrayLength;
        return buf;
    }

    @Override
    public byte[] readBytesFull() {
        alignToByte();
        return readBytesNotAligned(size() - position);
    }

    @Override
    public byte[] readBytesTerm(byte term, boolean includeTerm, boolean consumeTerm, boolean eosError) {
        alignToByte();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        while (true) {
            if (isEof()) {
                if (eosError) {
                    throw new RuntimeException("End of stream reached, but no terminator " + term + " found");
                }
                return buf.toByteArray();
            }
            byte c = binaryData.getByte(position);
            position++;
            if (c == term) {
                if (includeTerm) {
                    buf.write(c);
                }
                if (!consumeTerm) {
                    position--;
                }
                return buf.toByteArray();
            }
            buf.write(c);
        }
    }

    @Override
    public byte[] readBytesTermMulti(byte[] term, boolean includeTerm, boolean consumeTerm, boolean eosError) {
        alignToByte();
        int unitSize = term.length;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] c = new byte[unitSize];
        while (true) {
            int restSize = (int) (size() - position);
            if (restSize < unitSize) {
                if (eosError) {
                    throw new RuntimeException("End of stream reached, but no terminator " + byteArrayToHex(term) + " found");
                }
                binaryData.copyToArray(position, c, 0, restSize);
                position += restSize;
                buf.write(c, 0, restSize);
                return buf.toByteArray();
            }
            binaryData.copyToArray(0, c, 0, c.length);
            position += c.length;
            if (Arrays.equals(c, term)) {
                if (includeTerm)
                    buf.write(c, 0, c.length); // see the comment about `buf.write(c)` below
                if (!consumeTerm)
                    position -= c.length;
                return buf.toByteArray();
            }
            // we could also just use `buf.write(c)`, but then Java thinks that it could throw
            // `IOException` when it really can't (Java 11 adds `ByteArrayOutputStream.writeBytes`
            // for this reason, but we still want to support Java 7+)
            buf.write(c, 0, c.length);
        }
    }

    @Override
    public void writeS1(byte b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeS2be(short s) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeS4be(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeS8be(long l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeS2le(short s) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeS4le(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeS8le(long l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeF4be(float f) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeF8be(double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeF4le(float f) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeF8le(double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void writeBytesNotAligned(byte[] bytes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Nonnull
    @Override
    public KaitaiStream substream(long length) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    static String byteArrayToHex(byte[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0)
                sb.append(' ');
            sb.append(String.format("%02x", arr[i]));
        }
        sb.append(']');
        return sb.toString();
    }
}
