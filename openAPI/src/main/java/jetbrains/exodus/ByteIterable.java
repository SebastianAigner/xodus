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
 * If working with {@link jetbrains.exodus.env.Environment}, any key and value should be a ByteIterable.
 * ByteIterable is a mix of iterable and array. It allows to lazily enumerate bytes without boxing.
 * On the other hand, you can get its length using method getLength(). Generally, iterating over bytes
 * of ByteIterable is performed by means of getting {@link ByteIterator}.
 *
 * @see jetbrains.exodus.ArrayByteIterable
 * @see jetbrains.exodus.ByteBufferByteIterable
 * @see jetbrains.exodus.FileByteIterable
 * @see jetbrains.exodus.FixedLengthByteIterable
 * @see jetbrains.exodus.CompoundByteIterable
 */
public interface ByteIterable extends Comparable<ByteIterable> {

    byte[] EMPTY_BYTES = {};

    ByteIterator iterator();

    /**
     * @return raw content of the {@code ByteIterable}. May return array with length greater than {@link #getLength()}.
     */
    byte[] getBytesUnsafe();

    int baseOffset();

    byte[] getBaseBytes();

    /**
     * @param offset Offset of the byte to return.
     * @return byte at the given location of {@code ByteIterable} or
     * {@linkplain IndexOutOfBoundsException} if offset exceeds {@linkplain #getLength()} of the iterable.
     */
    byte byteAt(int offset);

    /**
     * @return length of the {@code ByteIterable}.
     */
    int getLength();

    int compareTo(int length, ByteIterable right, int rightLength);

    int compareTo(int from, int length, ByteIterable right, int rightFrom, int rightLength);

    /**
     * @param offset start offset, inclusive
     * @param length length of the sub-iterable
     * @return a fixed-length sub-iterable of the {@code ByteIterable} starting from {@code offset}.
     */
    @NotNull
    ByteIterable subIterable(final int offset, final int length);

    ByteIterator EMPTY_ITERATOR = new ByteIterator() {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public byte next() {
            return (byte) 0;
        }
        @Override
        public long skip(long bytes) {
            return 0;
        }
    };

    ByteIterable EMPTY = new ByteIterable() {

        @Override
        public ByteIterator iterator() {
            return EMPTY_ITERATOR;
        }

        @Override
        public int compareTo(@NotNull ByteIterable right) {
            return right.iterator().hasNext() ? -1 : 0;
        }

        @Override
        public int compareTo(int length, ByteIterable right, int rightLength) {
            if (length > 0) {
                throw new IllegalArgumentException();
            }

            if (rightLength == 0) {
                return 0;
            }

            return right.iterator().hasNext() ? -1 : 0;
        }

        @Override
        public int compareTo(int from, int length, ByteIterable right, int rightFrom, int rightLength) {
            if (from > 0 || length > 0) {
                throw new IllegalArgumentException();
            }

            if (rightLength == 0) {
                return 0;
            }

            return -1;
        }

        @Override
        public byte[] getBytesUnsafe() {
            return EMPTY_BYTES;
        }

        @Override
        public int baseOffset() {
            return 0;
        }

        @Override
        public byte[] getBaseBytes() {
            return EMPTY_BYTES;
        }

        @Override
        public byte byteAt(int offset) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public int getLength() {
            return 0;
        }

        @NotNull
        @Override
        public ByteIterable subIterable(int offset, int length) {
            return this;
        }

        @Override
        public String toString() {
            return "[ByteIterable.EMPTY]";
        }
    };
}
