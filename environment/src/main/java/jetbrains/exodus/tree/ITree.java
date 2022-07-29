/**
 * Copyright 2010 - 2022 JetBrains s.r.o.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.exodus.tree;

import jetbrains.exodus.ByteBufferByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.log.DataIterator;
import jetbrains.exodus.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.nio.ByteBuffer;

/**
 * Interface for immutable tree implementations
 */
public interface ITree {

    @NotNull
    Log getLog();

    @NotNull
    DataIterator getDataIterator(long address);

    long getRootAddress();

    int getStructureId();

    @Nullable
    ByteIterable get(@NotNull final ByteIterable key);

    @Nullable
    default ByteBuffer get(@NotNull final ByteBuffer key) {
        var result = get(new ByteBufferByteIterable(key));

        if (result == null) {
            return null;
        }

        return result.getByteBuffer();
    }

    boolean hasPair(@NotNull final ByteIterable key, @NotNull final ByteIterable value);

    default boolean hasPair(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        return hasPair(new ByteBufferByteIterable(key), new ByteBufferByteIterable(value));
    }

    boolean hasKey(@NotNull final ByteIterable key);

    default boolean hasKey(@NotNull final ByteBuffer key) {
        return hasKey(new ByteBufferByteIterable(key));
    }

    @NotNull
    ITreeMutable getMutableCopy();

    boolean isEmpty();

    long getSize();

    ITreeCursor openCursor();

    LongIterator addressIterator();
}
