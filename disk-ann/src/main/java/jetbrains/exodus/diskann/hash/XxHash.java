package jetbrains.exodus.diskann.hash;

public class XxHash {
    private static final long P1 = -7046029288634856825L;
    private static final long P2 = -4417276706812531889L;
    private static final long P3 = 1609587929392839161L;
    private static final long P4 = -8796714831421723037L;
    private static final long P5 = 2870177450012600261L;

    public static long hash(long value) {
        final long seed = 9223372036854775783L;
        @SuppressWarnings("NumericOverflow")
        long hash = seed + P5 + 8;
        value *= P2;
        value = Long.rotateLeft(value, 31);
        value *= P1;
        hash ^= value;
        hash = Long.rotateLeft(hash, 27) * P1 + P4;
        return XxHash.finalize(hash);
    }

    private static long finalize(long hash) {
        hash ^= hash >>> 33;
        hash *= P2;
        hash ^= hash >>> 29;
        hash *= P3;
        hash ^= hash >>> 32;
        return hash;
    }
}