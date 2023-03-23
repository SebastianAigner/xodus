package jetbrains.exodus.entitystore;

import jetbrains.exodus.newLogConcept.GarbageCollector.MVCCGarbageCollector;
import jetbrains.exodus.newLogConcept.GarbageCollector.TransactionGCEntry;
import jetbrains.exodus.newLogConcept.MVCC.MVCCRecord;
import jetbrains.exodus.newLogConcept.Transaction.TransactionState;
import org.jctools.maps.NonBlockingHashMapLong;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;


public class MVCCGarbageCollectorTest {


    @Test
    public void testFindMaxMinIdWithAllCommittedOneThread() {
        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
        NonBlockingHashMapLong<MVCCRecord> hashMap = new NonBlockingHashMapLong<>(); // primitive long keys

        for (long i = 1; i < 7; i++){
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
        }

        var collector = new MVCCGarbageCollector();
        collector.clean(7L, hashMap, transactionsGCMap);

        Assert.assertTrue(hashMap.containsKey(6L));

        for (long i = 1; i < 6; i++){
            Assert.assertFalse(hashMap.containsKey(i));
        }
    }

    @Test
    public void testFindMaxMinIdWithMissingIdsOneThread() {
        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
        NonBlockingHashMapLong<MVCCRecord> hashMap = new NonBlockingHashMapLong<>(); // primitive long keys

        for (long i = 1; i < 4; i++){
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
        }
        for (long i = 6; i < 7; i++){
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
        }

        var collector = new MVCCGarbageCollector();
        collector.clean(10L, hashMap, transactionsGCMap);

        Assert.assertTrue(hashMap.containsKey(3L));
        Assert.assertTrue(hashMap.containsKey(6L));

        for (long i = 1; i < 3; i++){
            Assert.assertFalse(hashMap.containsKey(i));
        }
        for (long i = 4; i < 6; i++){
            Assert.assertFalse(hashMap.containsKey(i));
        }

    }

    @Test
    public void testFindMaxMinIdWithMergedIdsOneThread() {
        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
        NonBlockingHashMapLong<MVCCRecord> hashMap = new NonBlockingHashMapLong<>(); // primitive long keys

        for (long i = 1; i < 4; i++){
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
        }
        for (long i = 4; i < 7; i++){
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }
        TransactionGCEntry entry = new TransactionGCEntry(TransactionState.COMMITTED.get(), 6);
        transactionsGCMap.put(4L, entry);

        for (long i = 7; i < 8; i++){
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }

        var collector = new MVCCGarbageCollector();
        collector.clean(10L, hashMap, transactionsGCMap);
        for (long i = 1; i < 7; i++){
            Assert.assertFalse(hashMap.containsKey(i));
        }
        Assert.assertTrue(hashMap.containsKey(7L));
    }

    @Test
    public void testFindMaxMinIdWithMergedIdsAndMissedValueOneThread() {
        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
        NonBlockingHashMapLong<MVCCRecord> hashMap = new NonBlockingHashMapLong<>(); // primitive long keys

        for (long i = 1; i < 4; i++){
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
        }

        for (long i = 5; i < 6; i++){
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }

        TransactionGCEntry entry = new TransactionGCEntry(TransactionState.COMMITTED.get(), 6);
        transactionsGCMap.put(5L, entry);
        for (long i = 7; i < 8; i++){
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }

        var collector = new MVCCGarbageCollector();
        collector.clean(10L, hashMap, transactionsGCMap);

        Assert.assertFalse(hashMap.containsKey(1L));
        Assert.assertFalse(hashMap.containsKey(2L));
        Assert.assertTrue(hashMap.containsKey(3L));
        Assert.assertFalse(hashMap.containsKey(4L));
        Assert.assertTrue(hashMap.containsKey(5L));
        Assert.assertFalse(hashMap.containsKey(6L));
        Assert.assertFalse(hashMap.containsKey(7L));

        Assert.assertEquals(collector.findMaxMinId(transactionsGCMap, 10L).longValue(), 3L);
    }


    @Test
    public void testFindMaxMinIdWithNoMaxMinId() {
        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
        NonBlockingHashMapLong<MVCCRecord> hashMap = new NonBlockingHashMapLong<>(); // primitive long keys

        transactionsGCMap.put(5L, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
        transactionsGCMap.put(6L, new TransactionGCEntry(TransactionState.COMMITTED.get(), 7L));
        hashMap.put(5L, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        hashMap.put(6L, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));

        for (long i = 8; i < 9; i++){
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }

        var collector = new MVCCGarbageCollector();
        collector.clean(10L, hashMap, transactionsGCMap);

        Assert.assertTrue(hashMap.containsKey(5L));
        Assert.assertTrue(hashMap.containsKey(6L));
        Assert.assertTrue(hashMap.containsKey(8L));

    }

    @Test
    public void testFindMaxMinIdWithoutMissingIdsOneThread() {
        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
        NonBlockingHashMapLong<MVCCRecord> hashMap = new NonBlockingHashMapLong<>(); // primitive long keys

        for (long i = 1; i < 4; i++){
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }
        for (long i = 4; i < 6; i++){
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }
        for (long i = 6; i < 7; i++){
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }
        var collector = new MVCCGarbageCollector();
        collector.clean(10L, hashMap, transactionsGCMap);

        Assert.assertFalse(hashMap.containsKey(1L));
        Assert.assertFalse(hashMap.containsKey(2L));
        for (long i = 3; i < 7; i++){
            Assert.assertTrue(hashMap.containsKey(i));
        }
    }

    @Test
    public void testFindMaxMinIdWithRevertedIdsOneThread() {
        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
        NonBlockingHashMapLong<MVCCRecord> hashMap = new NonBlockingHashMapLong<>(); // primitive long keys

        for (long i = 1; i < 4; i++){
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }
        for (long i = 4; i < 6; i++){
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.REVERTED.get()));
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }
        for (long i = 6; i < 7; i++){
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }
        var collector = new MVCCGarbageCollector();
        collector.clean(10L, hashMap, transactionsGCMap);

        for (long i = 1; i < 6; i++){
            Assert.assertFalse(hashMap.containsKey(i));
        }
        Assert.assertTrue(hashMap.containsKey(6L));
    }


    @Test
    public void getActiveTransactionsIdsOneThread() {
        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
        NonBlockingHashMapLong<MVCCRecord> hashMap = new NonBlockingHashMapLong<>(); // primitive long keys

        for (long i = 1; i < 4; i++){
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }
        for (long i = 4; i < 7; i++){
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }

        for (long i = 7; i < 10; i++){
            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
            hashMap.put(i, new MVCCRecord(new AtomicLong(0), new ConcurrentLinkedQueue<>()));
        }

        var collector = new MVCCGarbageCollector();
        collector.clean(15L, hashMap, transactionsGCMap);

        for (long i = 1; i < 3; i++){
            Assert.assertFalse(hashMap.containsKey(i));
        }
        for (long i = 4; i < 7; i++){
            Assert.assertTrue(hashMap.containsKey(i));
        }
        for (long i = 8; i < 10; i++){
            Assert.assertFalse(hashMap.containsKey(i));
        }
        Assert.assertTrue(hashMap.containsKey(7L));
    }

//    @Test
//    public void getMissingIdsOneThread() {
//        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
//
//        for (long i = 1; i < 4; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
//        }
//        for (long i = 5; i < 6; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.REVERTED.get()));
//        }
//
//        for (long i = 10; i < 11; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
//        }
//
//        var collector = new MVCCGarbageCollector();
//        Long maxMinId = collector.findMaxMinId(transactionsGCMap, 11L);
//        ConcurrentSkipListSet<Long> activeOrEmptyTransactionsIds =
//                collector.findMissingOrActiveTransactionsIds(maxMinId, 11L, transactionsGCMap);
//
//        ConcurrentSkipListSet<Long> correctActiveOrMissingTransactionsIds =
//                new ConcurrentSkipListSet<>(Arrays.asList(4L, 6L, 7L, 8L, 9L));
//
//        Assert.assertEquals(correctActiveOrMissingTransactionsIds, activeOrEmptyTransactionsIds);
//    }
//
//    @Test
//    public void getActiveOMissingTransactionsIdsOneThread() {
//        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
//
//        for (long i = 1; i < 4; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
//        }
//        for (long i = 5; i < 6; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.REVERTED.get()));
//        }
//        for (long i = 6; i < 8; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
//        }
//        for (long i = 9; i < 11; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
//        }
//
//        for (long i = 13; i < 20; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
//        }
//
//        var collector = new MVCCGarbageCollector();
//        Long maxMinId = collector.findMaxMinId(transactionsGCMap, 15L);
//        ConcurrentSkipListSet<Long> activeOrEmptyTransactionsIds =
//                collector.findMissingOrActiveTransactionsIds(maxMinId, 15L, transactionsGCMap);
//
//        ConcurrentSkipListSet<Long> correctActiveOrEmptyTransactionsIds =
//                new ConcurrentSkipListSet<>(Arrays.asList(4L, 6L, 7L, 8L, 11L, 12L, 13L, 14L));
//
//        Assert.assertEquals(correctActiveOrEmptyTransactionsIds, activeOrEmptyTransactionsIds);
//    }
//
//    @Test
//    public void getActiveAndMissingTransactionsIdsWithUpToOneThread() {
//        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
//
//        for (long i = 1; i < 3; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
//        }
//
//        transactionsGCMap.put(3L, new TransactionGCEntry(TransactionState.COMMITTED.get(), 6L));
//
//        for (long i = 7; i < 8; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
//        }
//        for (long i = 9; i < 11; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
//        }
//
//        transactionsGCMap.put(12L, new TransactionGCEntry(TransactionState.COMMITTED.get(), 14L));
//
//        for (long i = 16; i < 20; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
//        }
//
//        var collector = new MVCCGarbageCollector();
//        Long maxMinId = collector.findMaxMinId(transactionsGCMap, 18L);
//        ConcurrentSkipListSet<Long> activeOrEmptyTransactionsIds =
//                collector.findMissingOrActiveTransactionsIds(maxMinId, 18L, transactionsGCMap);
//
//        ConcurrentSkipListSet<Long> correctActiveOrEmptyTransactionsIds =
//                new ConcurrentSkipListSet<>(Arrays.asList(7L, 8L, 11L, 15L, 16L, 17L));
//
//        Assert.assertEquals(correctActiveOrEmptyTransactionsIds, activeOrEmptyTransactionsIds);
//    }
//
//
//    @Test
//    public void getActiveOMissingTransactionsIdsWithNoMaxMinIdOneThread() {
//        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
//
//        transactionsGCMap.put(1L, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
//        transactionsGCMap.put(2L, new TransactionGCEntry(TransactionState.REVERTED.get()));
//        transactionsGCMap.put(3L, new TransactionGCEntry(TransactionState.COMMITTED.get()));
//        transactionsGCMap.put(4L, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
//        transactionsGCMap.put(5L, new TransactionGCEntry(TransactionState.REVERTED.get()));
//        transactionsGCMap.put(6L, new TransactionGCEntry(TransactionState.COMMITTED.get(), 8L));
//        transactionsGCMap.put(9L, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
//
//        var collector = new MVCCGarbageCollector();
//        Long maxMinId = collector.findMaxMinId(transactionsGCMap, 12L);
//        Assert.assertNull(maxMinId);
//
//        ConcurrentSkipListSet<Long> activeOrEmptyTransactionsIds =
//                collector.findMissingOrActiveTransactionsIds(maxMinId, 12L, transactionsGCMap);
//        ConcurrentSkipListSet<Long> correctActiveOrEmptyTransactionsIds =
//                new ConcurrentSkipListSet<>(Arrays.asList(1L, 4L, 9L, 10L, 11L));
//
//        Assert.assertEquals(correctActiveOrEmptyTransactionsIds, activeOrEmptyTransactionsIds);
//    }
//
//    @Test
//    public void getActiveOMissingTransactionsIdsWithNoMaxMinIdOneThread2() {
//        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
//
//        transactionsGCMap.put(1L, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
//        transactionsGCMap.put(2L, new TransactionGCEntry(TransactionState.REVERTED.get()));
//        transactionsGCMap.put(3L, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
//        transactionsGCMap.put(4L, new TransactionGCEntry(TransactionState.REVERTED.get()));
//        transactionsGCMap.put(5L, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
//
//        var collector = new MVCCGarbageCollector();
//        Long maxMinId = collector.findMaxMinId(transactionsGCMap, 6L);
//        Assert.assertNull(maxMinId);
//
//        ConcurrentSkipListSet<Long> activeOrEmptyTransactionsIds =
//                collector.findMissingOrActiveTransactionsIds(maxMinId, 6L, transactionsGCMap);
//        ConcurrentSkipListSet<Long> correctActiveOrEmptyTransactionsIds =
//                new ConcurrentSkipListSet<>(Arrays.asList(1L, 3L, 5L));
//
//        Assert.assertEquals(correctActiveOrEmptyTransactionsIds, activeOrEmptyTransactionsIds);
//    }
//
//    @Test
//    public void getActiveOMissingTransactionsWithAllCommittedOrRevertedOneThread2() {
//        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
//
//        for (long i = 1; i < 4; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.REVERTED.get()));
//        }
//        for (long i = 4; i < 7; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
//        }
//
//
//        var collector = new MVCCGarbageCollector();
//        Long maxMinId = collector.findMaxMinId(transactionsGCMap, 7L);
//        Assert.assertEquals(6L, maxMinId.longValue());
//
//        ConcurrentSkipListSet<Long> activeOrEmptyTransactionsIds =
//                collector.findMissingOrActiveTransactionsIds(maxMinId, 7L, transactionsGCMap);
//        ConcurrentSkipListSet<Long> correctActiveOrEmptyTransactionsIds =
//                new ConcurrentSkipListSet<>(List.of());
//
//        Assert.assertEquals(correctActiveOrEmptyTransactionsIds, activeOrEmptyTransactionsIds);
//    }
//
//    @Test
//    public void getActiveOMissingTransactionsWithAllCommittedOneThread() {
//        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
//
//        for (long i = 1; i < 7; i++){
//            transactionsGCMap.put(i, new TransactionGCEntry(TransactionState.COMMITTED.get()));
//        }
//
//        var collector = new MVCCGarbageCollector();
//        Long maxMinId = collector.findMaxMinId(transactionsGCMap, 7L);
//        Assert.assertEquals(6L, maxMinId.longValue());
//
//        ConcurrentSkipListSet<Long> activeOrEmptyTransactionsIds =
//                collector.findMissingOrActiveTransactionsIds(maxMinId, 7L, transactionsGCMap);
//        ConcurrentSkipListSet<Long> correctActiveOrEmptyTransactionsIds =
//                new ConcurrentSkipListSet<>(List.of());
//
//        Assert.assertEquals(correctActiveOrEmptyTransactionsIds, activeOrEmptyTransactionsIds);
//    }
//
//    @Test
//    public void getActiveOMissingTransactionsWithAllCommittedAndMergedOneThread() {
//        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
//
//        transactionsGCMap.put(2L, new TransactionGCEntry(TransactionState.COMMITTED.get(), 4L));
//        transactionsGCMap.put(5L, new TransactionGCEntry(TransactionState.COMMITTED.get(), 6L));
//
//        var collector = new MVCCGarbageCollector();
//        Long maxMinId = collector.findMaxMinId(transactionsGCMap, 7L);
//        Assert.assertEquals(6L, maxMinId.longValue());
//
//        ConcurrentSkipListSet<Long> activeOrEmptyTransactionsIds =
//                collector.findMissingOrActiveTransactionsIds(maxMinId, 7L, transactionsGCMap);
//        ConcurrentSkipListSet<Long> correctActiveOrEmptyTransactionsIds =
//                new ConcurrentSkipListSet<>(List.of());
//
//        Assert.assertEquals(correctActiveOrEmptyTransactionsIds, activeOrEmptyTransactionsIds);
//    }
//    @Test
//    public void getActiveOMissingTransactionsWithAllMergedOneThread() {
//        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
//
//        transactionsGCMap.put(2L, new TransactionGCEntry(TransactionState.COMMITTED.get(), 7L));
//
//        var collector = new MVCCGarbageCollector();
//        Long maxMinId = collector.findMaxMinId(transactionsGCMap, 6L);
//        Assert.assertEquals(5L, maxMinId.longValue());
//
//        ConcurrentSkipListSet<Long> activeOrEmptyTransactionsIds =
//                collector.findMissingOrActiveTransactionsIds(maxMinId, 6L, transactionsGCMap);
//        ConcurrentSkipListSet<Long> correctActiveOrEmptyTransactionsIds =
//                new ConcurrentSkipListSet<>(List.of());
//
//        Assert.assertEquals(correctActiveOrEmptyTransactionsIds, activeOrEmptyTransactionsIds);
//    }
//
//    @Test
//    public void getActiveOMissingTransactionsWithAlmostAllMergedOneThread() {
//        ConcurrentSkipListMap<Long, TransactionGCEntry> transactionsGCMap = new ConcurrentSkipListMap<>();
//
//        transactionsGCMap.put(2L, new TransactionGCEntry(TransactionState.COMMITTED.get(), 7L));
//        transactionsGCMap.put(8L, new TransactionGCEntry(TransactionState.IN_PROGRESS.get()));
//
//        var collector = new MVCCGarbageCollector();
//        Long maxMinId = collector.findMaxMinId(transactionsGCMap, 9L);
//        Assert.assertEquals(7L, maxMinId.longValue());
//
//        ConcurrentSkipListSet<Long> activeOrEmptyTransactionsIds =
//                collector.findMissingOrActiveTransactionsIds(maxMinId, 9L, transactionsGCMap);
//        ConcurrentSkipListSet<Long> correctActiveOrEmptyTransactionsIds =
//                new ConcurrentSkipListSet<>(List.of(8L));
//
//        Assert.assertEquals(correctActiveOrEmptyTransactionsIds, activeOrEmptyTransactionsIds);
//    }
}