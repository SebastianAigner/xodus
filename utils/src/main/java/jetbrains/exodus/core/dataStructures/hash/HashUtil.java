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
package jetbrains.exodus.core.dataStructures.hash;

import java.util.Arrays;

public class HashUtil {

    public static final int MIN_CAPACITY = 4;
    public static final float DEFAULT_LOAD_FACTOR = 1;

    private static final float CAPACITY_MULTIPLE = 1.618033989f;

    private HashUtil() {
    }

    public static int indexFor(int hash, final int length, final int mask) {
        hash = (hash + (hash >>> 16)) & mask;
        /** The following statement is the same as
         *
         * if (hash >= length) {
         *     hash -= length;
         * }
         *
         * but without branching.
         */
        hash -= length & (((hash - length) ^ 0x80000000) >> 31);
        return hash;
    }

    public static int indexFor(final long hash, final int length, final int mask) {
        int result = (int) ((hash + (hash >>> 16)) & mask);
        /** The following statement is the same as
         *
         * if (result >= length) {
         *     result -= length;
         * }
         *
         * but without branching.
         */
        result -= length & (((result - length) ^ 0x80000000) >> 31);
        return result;
    }

    public static int nextCapacity(final int capacity) {
        return (int) ((capacity * CAPACITY_MULTIPLE)) + MIN_CAPACITY;
    }


    public static int nextPrimeCapacity(final int capacity) {
        int i = Arrays.binarySearch(tableSizes, capacity);
        if (i < 0) {
            i = ~i;
        }
        return tableSizes[i + 1];

    }

    public static int shift(int length) {
        int shift = 1;
        int shifted = 2;
        while (shifted <= length) {
            ++shift;
            shifted <<= 1;
        }
        return shift;
    }

    public static int getFirstPrime() {
        return tableSizes[0];
    }

    public static int getCeilingPrime(int size) {
        int i = Arrays.binarySearch(tableSizes, size);
        if (i < 0) {
            i = ~i;
        }
        return tableSizes[i];
    }

    public static int getFloorPrime(int size) {
        int result = getCeilingPrime(size);
        if (result > size) {
            result = getPreviousPrime(result);
        }
        return result;
    }

    public static int getPreviousPrime(int prime) {
        int i = Arrays.binarySearch(tableSizes, prime);
        if (i < 0) {
            throw new IllegalArgumentException("No such prime: " + prime);
        }
        return i == 0 ? tableSizes[i] : tableSizes[i - 1];
    }

    private static final int[] tableSizes = {

            // include all odd primes under 1000
            3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67,
            71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139,
            149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223,
            227, 229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293,
            307, 311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383,
            389, 397, 401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463,
            467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557, 563, 569,
            571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641, 643, 647,
            653, 659, 661, 673, 677, 683, 691, 701, 709, 719, 727, 733, 739, 743,
            751, 757, 761, 769, 773, 787, 797, 809, 811, 821, 823, 827, 829, 839,
            853, 857, 859, 863, 877, 881, 883, 887, 907, 911, 919, 929, 937, 941,
            947, 953, 967, 971, 977, 983, 991, 997,

            1031, 2063, 4127, 8263, 16529, 33071, 66161, 132329, 264659, 529327, 1058657, 2117317, 4234651, 8469319,
            1091, 2203, 4409, 8819, 17657, 35317, 70639, 141283, 282571, 565163, 1130351, 2260717, 4521509, 9043033,
            1153, 2309, 4621, 9257, 18517, 37039, 74093, 148193, 296437, 592877, 1185787, 2371609, 4743223, 9486469,
            1217, 2437, 4877, 9767, 19541, 39089, 78179, 156361, 312727, 625477, 1250969, 2501953, 5003909, 10007819,
            1283, 2579, 5167, 10337, 20681, 41381, 82763, 165527, 331063, 662141, 1324313, 2648629, 5297287, 10594583,
            1361, 2729, 5471, 10949, 21911, 43853, 87719, 175447, 350899, 701819, 1403641, 2807303, 5614657, 11229331,
            1409, 2819, 5639, 11279, 22567, 45137, 90281, 180563, 361159, 722321, 1444649, 2889317, 5778649, 11557303,
            1481, 2963, 5927, 11863, 23741, 47491, 94993, 189989, 379979, 759959, 1519939, 3039917, 6079861, 12159731,
            1543, 3089, 6197, 12401, 24809, 49627, 99257, 198529, 397063, 794137, 1588289, 3176597, 6353213, 12706451,
            1601, 3203, 6421, 12853, 25717, 51437, 102877, 205759, 411527, 823117, 1646237, 3292489, 6584983, 13169977,
            1667, 3343, 6689, 13381, 26777, 53569, 107171, 214351, 428731, 857471, 1714957, 3429961, 6859933, 13719869,
            1733, 3467, 6947, 13901, 27803, 55609, 111227, 222461, 444929, 889871, 1779761, 3559537, 7119103, 14238221,
            1801, 3607, 7219, 14447, 28901, 57803, 115613, 231241, 462491, 924997, 1850021, 3700043, 7400123, 14800271,
            1861, 3727, 7457, 14923, 29851, 59707, 119417, 238837, 477677, 955363, 1910729, 3821483, 7643017, 15286079,
            1931, 3863, 7727, 15461, 30931, 61871, 123757, 247519, 495041, 990137, 1980281, 3960581, 7921169,
            1993, 3989, 7993, 15991, 31991, 63997, 127997, 256019, 512047, 1024099, 2048203, 4096427, 8192867,

            26339969, 52679969, 105359939, 210719881, 421439783, 842879579, 1685759167,
            28977863, 57955739, 115911563, 231823147, 463646329, 927292699, 1854585413,
            31322867, 62645741, 125291483, 250582987, 501165979, 1002331963, 2004663929,
            17135863, 34271747, 68543509, 137087021, 274174111, 548348231, 1096696463,
            18366923, 36733847, 73467739, 146935499, 293871013, 587742049, 1175484103,
            19845871, 39691759, 79383533, 158767069, 317534141, 635068283, 1270136683,
            21006137, 42012281, 84024581, 168049163, 336098327, 672196673, 1344393353,
            22458671, 44917381, 89834777, 179669557, 359339171, 718678369, 1437356741,
            23723597, 47447201, 94894427, 189788857, 379577741, 759155483, 1518310967,
            25002389, 50004791, 100009607, 200019221, 400038451, 800076929, 1600153859,

            Integer.MAX_VALUE,
    };

    static {
        Arrays.sort(tableSizes);
    }
}
