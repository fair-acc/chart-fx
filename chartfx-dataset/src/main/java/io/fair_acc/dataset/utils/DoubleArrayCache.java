package io.fair_acc.dataset.utils;

import java.lang.ref.Reference;

/**
 * Implements double-array (double[]) cache collection to minimise memory re-allocation.
 *
 * <p>
 * N.B. This is useful to re-use short-lived data storage container to minimise the amount of garbage to be collected. This is used in situation replacing e.g.
 * <pre>
 *  {@code
 *      public returnValue frequentlyExecutedFunction(...) {
 *          final double[] storage = new byte[10000]; // allocate new memory block (costly)
 *          // [...] do short-lived computation on storage
 *          // storage is implicitly finalised by garbage collector (costly)
 *      }
 *  }
 * </pre>
 * with
 * <pre>
 *  {@code
 *      // ...
 *      private final ByteArrayCache cache = new ByteArrayCache();
 *      // ...
 *
 *      public returnValue frequentlyExecutedFunction(...) {
 *          final double[] storage = cache.getArray(10000); // return previously allocated array (cheap) or allocated new if necessary
 *          // [...] do short-lived computation on storage
 *          cache.add(storage); // return object to cache
 *      }
 *  }
 * </pre>
 *
 * @author rstein
 *
 */
public class DoubleArrayCache extends CacheCollection<double[]> {
    private static final DoubleArrayCache SELF = new DoubleArrayCache();

    public double[] getArray(final int requiredSize) {
        return getArray(requiredSize, false);
    }

    public double[] getArrayExact(final int requiredSize) {
        return getArray(requiredSize, true);
    }

    private double[] getArray(final int requiredSize, final boolean exact) {
        synchronized (contents) {
            double[] bestFit = null;
            int bestFitSize = Integer.MAX_VALUE;

            for (final Reference<double[]> candidate : contents) {
                final double[] localRef = candidate.get();
                if (localRef == null) {
                    continue;
                }

                final int sizeDiff = localRef.length - requiredSize;
                if (sizeDiff == 0) {
                    bestFit = localRef;
                    break;
                }

                if (sizeDiff > 0 && sizeDiff < bestFitSize && !exact) {
                    bestFitSize = sizeDiff;
                    bestFit = localRef;
                }
            }

            if (bestFit == null) {
                // could not find any cached, return new double[]
                bestFit = new double[requiredSize];
                return bestFit;
            }

            remove(bestFit);
            return bestFit;
        }
    }

    public static DoubleArrayCache getInstance() {
        return SELF;
    }
}
