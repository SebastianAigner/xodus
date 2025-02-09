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
package jetbrains.exodus.io;

public abstract class AbstractDataWriter implements DataWriter {

    private boolean open;

    protected AbstractDataWriter() {
        open = false;
    }

    @Override
    public final boolean isOpen() {
        return open;
    }

    @Override
    public final void sync() {
        if (open) {
            syncImpl();
        }
    }

    @Override
    public void syncDirectory() {
    }

    @Override
    public final void close() {
        if (open) {
            closeImpl();
            open = false;
        }
    }

    @Override
    public void clear() {
        close();
        clearImpl();
    }

    @Override
    public final Block openOrCreateBlock(final long address, final long length) {
        if (open) {
            throw new IllegalStateException("Can't create blocks with open data writer");
        } else {
            final Block result = openOrCreateBlockImpl(address, length);
            open = true;
            return result;
        }
    }

    protected abstract void syncImpl();

    protected abstract void closeImpl();

    protected abstract void clearImpl();

    protected abstract Block openOrCreateBlockImpl(final long address, final long length);

}
