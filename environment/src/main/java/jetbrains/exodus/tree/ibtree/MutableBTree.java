/*
 * *
 *  * Copyright 2010 - 2022 JetBrains s.r.o.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package jetbrains.exodus.tree.ibtree;

import jetbrains.exodus.ByteBufferComparator;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.log.*;
import jetbrains.exodus.tree.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;

public final class MutableBTree implements IBTreeMutable {
    final ExpiredLoggableCollection expiredLoggables = new ExpiredLoggableCollection();
    @Nullable
    HashSet<ITreeCursorMutable> cursors;

    final ImmutableBTree immutableTree;
    final Log log;

    @NotNull
    MutablePage root;

    long size;

    public MutableBTree(ImmutableBTree immutableTree) {
        this.immutableTree = immutableTree;
        this.log = immutableTree.log;

        var immutableRoot = immutableTree.root;
        if (immutableRoot == null) {
            this.root = new MutableLeafPage(this, null, log, log.getCachePageSize(),
                    expiredLoggables, null);
        } else {
            this.root = immutableRoot.toMutable(this, expiredLoggables, null);
        }

        size = root.treeSize();
    }

    @Override
    public @NotNull Log getLog() {
        return log;
    }

    @Override
    public @NotNull DataIterator getDataIterator(long address) {
        return immutableTree.getDataIterator(address);
    }

    @Override
    public long getRootAddress() {
        return root.address();
    }

    @Override
    public int getStructureId() {
        return immutableTree.structureId;
    }

    @Override
    public @NotNull ITreeMutable getMutableCopy() {
        return this;
    }

    @Override
    public boolean isEmpty() {
        return root.treeSize() == 0;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public ITreeCursor openCursor() {
        var cursor = new TreeMutableCursor(this, this.root);

        if (cursors == null) {
            cursors = new HashSet<>();
        }

        cursors.add(cursor);

        return cursor;
    }

    @Override
    public LongIterator addressIterator() {
        return immutableTree.addressIterator();
    }

    @Override
    public boolean isAllowingDuplicates() {
        return false;
    }

    @Override
    public @Nullable Iterable<ITreeCursorMutable> getOpenCursors() {
        return cursors;
    }

    @Override
    public void cursorClosed(@NotNull ITreeCursorMutable cursor) {
        assert cursors != null;

        cursors.remove(cursor);
    }

    @Override
    public boolean put(@NotNull ByteBuffer key, @NotNull ByteBuffer value) {
        var page = root;

        while (true) {
            var index = page.find(key);
            if (page instanceof MutableLeafPage mutablePage) {
                if (index < 0) {
                    mutablePage.insert(-index - 1, key, value);

                    TreeMutableCursor.notifyCursors(this);
                    size++;
                    return true;
                }

                mutablePage.set(index, key, value);

                TreeMutableCursor.notifyCursors(this);
                return true;
            } else {
                if (index < 0) {
                    index = -index - 1;

                    if (index > 0) {
                        index--;
                    }
                }

                var internalPage = (MutableInternalPage) page;
                page = internalPage.mutableChild(index);
            }
        }
    }

    @Override
    public boolean put(@NotNull ByteIterable key, @NotNull ByteIterable value) {
        return put(key.getByteBuffer(), value.getByteBuffer());
    }

    @Override
    public void putRight(@NotNull ByteBuffer key, @NotNull ByteBuffer value) {
        var page = root;

        while (true) {
            if (page instanceof MutableLeafPage mutableLeafPage) {
                mutableLeafPage.append(key, value);

                TreeMutableCursor.notifyCursors(this);
                size++;
                return;
            } else {
                var mutableInternalPage = (MutableInternalPage) page;
                var numChildren = mutableInternalPage.getEntriesCount();

                page = mutableInternalPage.mutableChild(numChildren - 1);
            }
        }
    }

    @Override
    public void putRight(@NotNull ByteIterable key, @NotNull ByteIterable value) {
        putRight(key.getByteBuffer(), value.getByteBuffer());
    }

    @Override
    public boolean add(@NotNull ByteBuffer key, @NotNull ByteBuffer value) {
        final var stack = new ArrayList<ElemRef>();
        TraversablePage page = root;

        while (true) {
            var index = page.find(key);
            if (!page.isInternalPage()) {
                if (index < 0) {
                    makeAllStackPagesMutable(stack);

                    //reload page to insure that it is mutable page
                    page = stack.get(stack.size() - 1).page;
                    var mutablePage = (MutableLeafPage) page;

                    mutablePage.insert(index, key, value);
                    TreeMutableCursor.notifyCursors(this);

                    size++;
                    return true;
                }

                return false;
            } else {
                if (index < 0) {
                    index = -index - 1;

                    if (index > 0) {
                        index--;
                    }
                }

                page = page.child(index);
            }
        }
    }

    @Override
    public boolean add(@NotNull ByteIterable key, @NotNull ByteIterable value) {
        return add(key.getByteBuffer(), value.getByteBuffer());
    }

    @Override
    public void put(@NotNull INode ln) {
        var value = ln.getValue();
        var key = ln.getKey();

        Objects.requireNonNull(value);

        put(key, value);
    }

    @Override
    public void putRight(@NotNull INode ln) {
        var value = ln.getValue();
        var key = ln.getKey();

        Objects.requireNonNull(value);

        putRight(key, value);
    }

    @Override
    public boolean add(@NotNull INode ln) {
        var value = ln.getValue();
        var key = ln.getKey();

        Objects.requireNonNull(value);
        return add(key, value);
    }

    @Override
    public boolean delete(@NotNull ByteBuffer key) {
        var result = doDelete(key, null);

        if (result) {
            size--;
            TreeMutableCursor.notifyCursors(this);
        }

        return result;
    }

    @Override
    public boolean delete(@NotNull ByteIterable key) {
        return delete(key.getByteBuffer());
    }

    @Override
    public boolean delete(@NotNull ByteBuffer key, @Nullable ByteBuffer value, @Nullable ITreeCursorMutable cursorToSkip) {
        if (doDelete(key, value)) {
            TreeMutableCursor.notifyCursors(this, cursorToSkip);
            size--;
            return true;
        }

        return false;
    }

    @Override
    public boolean delete(@NotNull ByteIterable key, @Nullable ByteIterable value,
                          @Nullable ITreeCursorMutable cursorToSkip) {
        if (value == null) {
            return delete(key.getByteBuffer(), null, cursorToSkip);
        } else {
            return delete(key.getByteBuffer(), value.getByteBuffer(), cursorToSkip);
        }
    }

    private boolean doDelete(final ByteBuffer key, final ByteBuffer value) {
        var page = root;
        while (true) {
            var index = page.find(key);
            if (page instanceof MutableLeafPage mutablePage) {
                if (index >= 0) {
                    if (value == null) {
                        mutablePage.delete(index);
                        return true;
                    }

                    var val = page.value(index);
                    if (ByteBufferComparator.INSTANCE.compare(val, value) == 0) {
                        mutablePage.delete(index);
                        return true;
                    }

                    return false;
                }

                return false;
            } else {
                if (index < 0) {
                    index = -index - 1;

                    if (index > 0) {
                        index--;
                    } else {
                        return false;
                    }
                }

                var internalPage = (MutableInternalPage) page;
                page = internalPage.mutableChild(index);
            }
        }
    }

    @Override
    public long save() {
        var newRoot = root.rebalance();
        if (newRoot != null) {
            root = newRoot;
        }

        var prevRoot = root;
        root.spill();

        //re-spill the root if it was changed
        while (root != prevRoot) {
            prevRoot = root;
            root.spill();
        }

        var address = root.save(immutableTree.getStructureId());
        TreeMutableCursor.notifyCursors(this);
        return address;
    }

    @Override
    public @NotNull ExpiredLoggableCollection getExpiredLoggables() {
        return expiredLoggables;
    }

    @Override
    public boolean reclaim(@NotNull RandomAccessLoggable loggable, @NotNull Iterator<RandomAccessLoggable> loggables,
                           long segmentSize) {
        System.out.println("Reclaim !");
        final long fileAddress = loggable.getAddress() / segmentSize;
        final boolean isEmpty = isEmpty();

        boolean reclaimed = false;
        var address = loggable.getAddress();
        ArrayList<ElemRef> stack;

        if (!isEmpty) {
            stack = new ArrayList<>(8);
            stack.add(new ElemRef(root, 0));
        } else {
            stack = null;
        }

        loggableLoop:
        while (true) {
            var type = loggable.getType();
            switch (type) {
                case NullLoggable.TYPE:
                    continue;
                case ImmutableBTree.INTERNAL_PAGE:
                case ImmutableBTree.LEAF_PAGE:
                    if (isEmpty) {
                        continue;
                    }
                    reclaimed = reclaimed | doReclaimPage(loggable, stack);
                    break;
                case ImmutableBTree.INTERNAL_ROOT_PAGE:
                case ImmutableBTree.LEAF_ROOT_PAGE:
                    reclaimed = reclaimed | doReclaimPage(loggable, stack);
                    break loggableLoop;
            }

            if (loggables.hasNext()) {
                loggable = loggables.next();
            } else {
                break;
            }

            address = loggable.getAddress();
            if (address / segmentSize != fileAddress) {
                break;
            }
        }

        return reclaimed;
    }

    private boolean doReclaimPage(final Loggable pageLoggable, final ArrayList<ElemRef> stack) {
        var immutablePage = immutableTree.loadPage(pageLoggable.getAddress());
        var firstKey = immutablePage.key(0);

        moveToTheTopTillKeyInSearchRange(stack, firstKey);

        var pageAddress = pageLoggable.getAddress();
        var elemRef = stack.get(stack.size() - 1);

        var page = elemRef.page;
        if (page.address() == pageAddress) {
            //convert all stack of pages to mutable pages
            makeAllStackPagesMutable(stack);

            return true;
        }

        //restart search from the last page
        var index = page.find(firstKey);
        if (index < 0) {
            index = -index - 1;

            //no pages containing key stored inside of leaf page
            //because all pages contain key bigger than we are looking for
            if (index == 0) {
                return false;
            }
        }
        elemRef.index = index;

        while (true) {
            page = page.child(index);

            if (!page.isInternalPage()) {
                if (page.address() != pageAddress) {
                    return false;
                }

                //convert all stack of pages to mutable pages
                makeAllStackPagesMutable(stack);

                //make leaf page to be dirty so it will be saved again
                var lastPage = stack.get(stack.size() - 1).page;
                var childPage = lastPage.child(index);
                ((MutablePage) childPage).fetch();
            }

            index = page.find(firstKey);

            if (index < 0) {
                index = -index - 1;

                //no pages containing key stored inside of leaf page
                //because all pages contain key bigger than we are looking for
                if (index == 0) {
                    return false;
                }
            }

            stack.add(new ElemRef(page, index));
        }
    }

    private void makeAllStackPagesMutable(final ArrayList<ElemRef> stack) {
        var fetched = false;

        for (int i = 0; i < stack.size(); i++) {
            var elemRef = stack.get(i);

            var mutablePage = (MutableInternalPage) elemRef.page;

            if (!fetched) {
                fetched = mutablePage.fetch();

                if (fetched && i < stack.size() - 1) {
                    //because top pages were converted into mutable page we need to replace all pages bellow
                    stack.get(i + 1).page = mutablePage.mutableChild(elemRef.index);
                }
            } else if (i < stack.size() - 1) {
                //because top pages were converted into mutable page we need to replace all pages bellow
                stack.get(i + 1).page = mutablePage.mutableChild(elemRef.index);
            }
        }

    }

    private void moveToTheTopTillKeyInSearchRange(final ArrayList<ElemRef> stack, final ByteBuffer key) {
        while (stack.size() > 1) {
            var last = stack.get(stack.size() - 1);
            var page = last.page;

            if (!insideSearchRange(key, page)) {
                stack.remove(stack.size() - 1);
            } else {
                break;
            }
        }
    }

    private boolean insideSearchRange(final ByteBuffer key, final TraversablePage page) {
        assert page.isInternalPage();

        var firstKey = page.key(0);

        if (ByteBufferComparator.INSTANCE.compare(firstKey, key) < 0) {
            return false;
        }

        var lastKey = page.key(page.getEntriesCount() - 1);
        return ByteBufferComparator.INSTANCE.compare(key, lastKey) <= 0;
    }


    @Override
    public @NotNull TraversablePage getRoot() {
        return root;
    }
}
