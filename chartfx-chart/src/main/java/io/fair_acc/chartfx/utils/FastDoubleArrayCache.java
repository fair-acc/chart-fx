package io.fair_acc.chartfx.utils;

import io.fair_acc.math.ArrayUtils;

/**
 * A highly efficient cache for temporary arrays that
 * only gets accessed by a single thread.
 *
 * @author ennerf
 */
public class FastDoubleArrayCache {

    public FastDoubleArrayCache(int maxArrays) {
        cache = new double[maxArrays][];
    }

    public void trim() {
        for (int i = 0; i < cache.length; i++) {
            cache[i] = ArrayUtils.clearIfLarger(cache[i], lastRequestedSize);
        }
    }

    public double[] getArray(int index, int minSize) {
        return cache[index] = ArrayUtils.resizeMin(cache[index], lastRequestedSize = minSize);
    }

    int lastRequestedSize;
    private final double[][] cache;

}
