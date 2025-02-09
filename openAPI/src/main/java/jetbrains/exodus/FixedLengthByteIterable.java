/*
 * Copyright 2010 - 2023 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.exodus;

import org.jetbrains.annotations.NotNull;

/**
 * Helps to create new {@code ByteIterable} as a part of another (source) {@code ByteIterable}.
 * Is used in basic implementation of {@link ByteIterable#subIterable(int, int)}.
 */
public class FixedLengthByteIterable extends ByteIterableBase {

    protected final ByteIterable source;
    protected final int offset;
    protected int baseOffset;

    protected FixedLengthByteIterable(@NotNull final ByteIterable source, final int offset, final int length) {
        assert source.getLength() >= offset + length;

        if (length < 0) {
            throw new ExodusException("ByteIterable length can't be less than zero");
        }
        this.source = source;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public byte[] getBytesUnsafe() {
        return doGetBytes();
    }

    @Override
    public byte[] getBaseBytes() {
        return doGetBytes();
    }

    private byte[] doGetBytes() {
        if (bytes == null) {
            final int length = this.length;
            if (source instanceof ByteIterableBase) {
                final ByteIterableBase s = (ByteIterableBase) source;
                final byte[] baseBytes = s.getBaseBytes();
                final int baseOffset = s.baseOffset();

                if (baseBytes != null) {
                    this.bytes = baseBytes;
                    this.baseOffset = offset + baseOffset;

                    return this.bytes;
                }
            }

            byte[] bytes = new byte[length];
            final ByteIterator it = source.iterator();
            it.skip(offset);

            for (int i = 0; it.hasNext() && i < length; ++i) {
                bytes[i] = it.next();
            }

            this.bytes = bytes;
        }
        return bytes;
    }

    @Override
    public int baseOffset() {
        return baseOffset;
    }

    @Override
    public int getLength() {
        return length;
    }

    @NotNull
    @Override
    public ByteIterable subIterable(final int offset, final int length) {
        final int safeLength = Math.min(length, this.length - offset);
        return safeLength == 0 ? EMPTY : new FixedLengthByteIterable(source, this.offset + offset, safeLength);
    }

    public ByteIterable getSource() {
        return source;
    }

    @Override
    protected ByteIterator getIterator() {
        if (length == 0) {
            return ByteIterable.EMPTY_ITERATOR;
        }

        if (bytes != null) {
            return new ArrayByteIterable.Iterator(bytes, baseOffset, length);
        }

        final ByteIterator bi = source.iterator();
        bi.skip(offset);
        return new ByteIterator() {
            private int i = length;

            @Override
            public boolean hasNext() {
                return i > 0 && bi.hasNext();
            }

            @Override
            public byte next() {
                i--;
                return bi.next();
            }

            @Override
            public long skip(long bytes) {
                long result = bi.skip(Math.min(bytes, i));
                i -= (int) result;
                return result;
            }
        };
    }
}
