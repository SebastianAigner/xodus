package jetbrains.exodus.tree.ibtree;

import jetbrains.exodus.ByteBufferComparator;
import jetbrains.exodus.log.Log;
import jetbrains.exodus.tree.ExpiredLoggableCollection;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

/**
 * Representation of common layout of all pages both leaf and internal.
 * Layout composed as following:
 * <ol>
 *     <li>Key prefix size. Size of the common prefix which was truncated for all keys in tree.
 *     Currently not used but added to implement key compression without breaking of binary compatibility.</li>
 *     <li>Count of the entries contained inside of the given page.</li>
 *     <li>Array each entry of which contains either address of the loggable which contains key of the entry or pair
 *     (key position, key size) where 'key position' is position of the key stored inside of this page,
 *     key size is accordingly size of this key. To distinguish between those two meanings of the same value,
 *     key addresses always negative and their sign should be changed to load key.</li>
 *     <li> Array each entry of which contains either address of value of the entry if that is
 *     {@link  ImmutableLeafPage} or address of the mutableChild page if that is
 *     {@link ImmutableInternalPage}</li>
 *     <li>Array of keys embedded into this page.</li>
 * </ol>
 * <p>
 * All values are kept outside of the BTree page but keys are embedded if they are small enough.
 * Internal pages also keep size of the whole (sub)tree at the header of the page.
 * For all pages small is space kept to keep structure id and loggable type that why page offset is passed.
 * Page offset should be quantised by {@link Long#BYTES} bytes.
 */
abstract class ImmutableBasePage {
    static final int KEY_PREFIX_LEN_OFFSET = 0;

    //we could use short here but in such case we risk to get unaligned memory access for subsequent reads
    //so we use int
    static final int KEY_PREFIX_LEN_SIZE = Integer.BYTES;

    static final int ENTRIES_COUNT_OFFSET = KEY_PREFIX_LEN_OFFSET + KEY_PREFIX_LEN_SIZE;
    static final int ENTRIES_COUNT_SIZE = Integer.BYTES;

    static final int KEYS_OFFSET = ENTRIES_COUNT_OFFSET + ENTRIES_COUNT_SIZE;

    @NotNull
    final Log log;
    final long address;

    @NotNull
    final ByteBuffer page;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @NotNull
    final List<ByteBuffer> keyView;

    protected ImmutableBasePage(@NotNull final Log log, @NotNull final ByteBuffer page, long address) {
        this.log = log;
        this.address = address;
        this.page = page;

        //ensure that allocated page aligned to ensure fastest memory access and stable offsets of the data
        assert page.alignmentOffset(0, Long.BYTES) == 0;
        assert page.order() == ByteOrder.nativeOrder();

        keyView = new KeyView();
    }

    abstract long getTreeSize();

    final int find(ByteBuffer key) {
        return Collections.binarySearch(keyView, key, ByteBufferComparator.INSTANCE);
    }

    private int getKeyAddressPosition(final int index) {
        return KEYS_OFFSET + index * Long.BYTES;
    }

    private ByteBuffer getKey(int index) {
        final long keyAddress = getKeyAddress(index);

        if (keyAddress < 0) {
            var keyLoggable = log.read(-keyAddress);
            assert keyLoggable.getType() == ImmutableBTree.KEY_NODE;

            var data = keyLoggable.getData();
            return data.getByteBuffer();
        }

        return getEmbeddedKey(keyAddress);
    }

    final ByteBuffer getEmbeddedKey(final long keyAddress) {
        final int keyPosition = (int) keyAddress;
        final int keySize = (int) (keyAddress >> Integer.SIZE);

        return page.slice(keyPosition, keySize);
    }

    final long getKeyAddress(int index) {
        final int keyAddressPosition = getKeyAddressPosition(index);
        assert page.alignmentOffset(keyAddressPosition, Long.BYTES) == 0;

        return page.getLong(keyAddressPosition);
    }

    final int getEntriesCount() {
        assert page.alignmentOffset(ENTRIES_COUNT_OFFSET, Integer.BYTES) == 0;

        return page.getInt(ENTRIES_COUNT_OFFSET);
    }

    final ByteBuffer key(int index) {
        return keyView.get(index);
    }

    abstract MutablePage toMutable(ExpiredLoggableCollection expiredLoggables, MutableInternalPage parent);

    private int getChildAddressPosition(int index) {
        return KEYS_OFFSET + getEntriesCount() * Long.BYTES + index * Long.BYTES;
    }

    final long getChildAddress(int index) {
        final int childAddressPosition = getChildAddressPosition(index);

        assert page.alignmentOffset(childAddressPosition, Long.BYTES) == 0;

        return page.getLong(childAddressPosition);
    }

    final class KeyView extends AbstractList<ByteBuffer> implements RandomAccess {
        @Override
        public ByteBuffer get(int i) {
            return getKey(i);
        }

        @Override
        public int size() {
            return getEntriesCount();
        }
    }
}