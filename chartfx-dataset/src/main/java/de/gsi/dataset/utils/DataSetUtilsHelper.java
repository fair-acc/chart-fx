package de.gsi.dataset.utils;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.security.InvalidParameterException;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;

/**
 * Small static helper routines to ease the reading of the DataSetUtils class
 * 
 * @author rstein
 */
public class DataSetUtilsHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetUtilsHelper.class);
    protected static final ReentrantLock BYTE_ARRAY_CACHE_LOCK = new ReentrantLock();
    protected static WeakHashMap<String, WeakHashMap<Integer, ByteBuffer>> byteArrayCache = new WeakHashMap<>();
    protected static final ReentrantLock STRING_BUFFER_CACHE_LOCK = new ReentrantLock();
    protected static WeakHashMap<String, WeakHashMap<Integer, StringBuilder>> stringBuilderCache = new WeakHashMap<>();

    protected static ByteBuffer getCachedDoubleArray(final String arrayName, final int size) {
        BYTE_ARRAY_CACHE_LOCK.lock();
        WeakHashMap<Integer, ByteBuffer> arrayMap = byteArrayCache.computeIfAbsent(arrayName,
                name -> new WeakHashMap<>());

        ByteBuffer cachedArray = arrayMap.get(size);
        if (cachedArray == null) {
            cachedArray = ByteBuffer.allocate(size);
            // cache missed
        } else {
            byteArrayCache.get(arrayName).remove(size);
        }
        BYTE_ARRAY_CACHE_LOCK.unlock();
        return cachedArray;
    }

    protected static void release(final String arrayName, final ByteBuffer cachedArray) {
        if (cachedArray == null) {
            return;
        }
        cachedArray.clear();
        BYTE_ARRAY_CACHE_LOCK.lock();
        byteArrayCache.get(arrayName).put(cachedArray.capacity(), cachedArray);
        BYTE_ARRAY_CACHE_LOCK.unlock();
    }

    protected static StringBuilder getCachedStringBuilder(final String arrayName, final int size) {
        STRING_BUFFER_CACHE_LOCK.lock();
        WeakHashMap<Integer, StringBuilder> arrayMap = stringBuilderCache.computeIfAbsent(arrayName,
                name -> new WeakHashMap<>());

        StringBuilder cachedArray = arrayMap.get(size);
        if (cachedArray == null) {
            cachedArray = new StringBuilder(size);
            // cache missed
        } else {
            stringBuilderCache.get(arrayName).remove(size);
        }
        cachedArray.delete(0, cachedArray.length());
        STRING_BUFFER_CACHE_LOCK.unlock();
        return cachedArray;
    }

    protected static void release(final String arrayName, final StringBuilder cachedArray) {
        if (cachedArray == null) {
            return;
        }
        STRING_BUFFER_CACHE_LOCK.lock();
        stringBuilderCache.get(arrayName).put(cachedArray.capacity(), cachedArray);
        STRING_BUFFER_CACHE_LOCK.unlock();
    }

    /**
     * @param input
     *            double array input
     * @return float array output
     */
    public static float[] toFloatArray(final double[] input) {
        if (input == null) {
            return null;
        }
        int n = input.length;
        float[] ret = new float[n];
        for (int i = 0; i < n; i++) {
            ret[i] = (float) input[i];
        }
        return ret;
    }

    /**
     * @param input
     *            float array input
     * @return double array output
     */
    public static double[] toDoubleArray(final float[] input) {
        if (input == null) {
            return null;
        }
        int n = input.length;
        double[] ret = new double[n];
        for (int i = 0; i < n; i++) {
            ret[i] = input[i];
        }
        return ret;
    }

    protected static void writeDoubleArrayToByteBuffer(final ByteBuffer byteBuffer, final double[] doubleBuffer,
            final int nSamples) {
        if (byteBuffer == null) {
            throw new InvalidParameterException("ByteBuffer is 'null'");
        }
        if (doubleBuffer == null) {
            throw new InvalidParameterException("doubleBuffer is 'null'");
        }
        if (byteBuffer.capacity() < nSamples * Double.BYTES) {
            throw new InvalidParameterException("byte buffer size (" + byteBuffer.capacity()
                    + ") is smaller than double buffer size (" + nSamples * Float.BYTES + ")");
        }
        if (doubleBuffer.length < nSamples) {
            throw new InvalidParameterException("double array contains less (" + doubleBuffer.length
            + ") than nsamples (" + nSamples + ") entries.");
        }
        byteBuffer.position(0);
        for (int i = 0; i < nSamples; i++) {
            byteBuffer.putDouble(doubleBuffer[i]);
        }
        // alt: (N.B. a bit more overhead/slower compared to the above code)
        // final DoubleBuffer xDouble = byteBuffer.asDoubleBuffer();
        // xDouble.put(doubleBuffer);
    }

    protected static void writeDoubleArrayAsFloatToByteBuffer(final ByteBuffer byteBuffer, final double[] doubleBuffer,
            final int nSamples) {
        if (byteBuffer == null) {
            throw new InvalidParameterException("ByteBuffer is 'null'");
        }
        if (doubleBuffer == null) {
            throw new InvalidParameterException("doubleBuffer is 'null'");
        }
        if (byteBuffer.capacity() < nSamples * Float.BYTES) {
            throw new InvalidParameterException("byte buffer size (" + byteBuffer.capacity()
                    + ") is smaller than double buffer size (" + nSamples * Float.BYTES + ")");
        }
        if (doubleBuffer.length < nSamples) {
            throw new InvalidParameterException("double array contains less (" + doubleBuffer.length
            + ") than nsamples (" + nSamples + ") entries.");
        }
        byteBuffer.position(0);
        for (int i = 0; i < nSamples; i++) {
            byteBuffer.putFloat((float) doubleBuffer[i]);
        }
        // alt: (N.B. a bit more overhead/slower compared to the above code)
        // final DoubleBuffer xDouble = byteBuffer.asDoubleBuffer();
        // xDouble.put(doubleBuffer);
    }

    protected static double[] readDoubleArrayFromBuffer(final FloatBuffer floatBuffer,
            final DoubleBuffer doubleBuffer) {
        double[] retArray;
        if (floatBuffer != null) {
            retArray = new double[floatBuffer.limit()];
            for (int i = 0; i < retArray.length; i++) {
                retArray[i] = floatBuffer.get(i);
            }
            return retArray;
        }
        if (doubleBuffer != null) {
            retArray = new double[doubleBuffer.limit()];
            for (int i = 0; i < retArray.length; i++) {
                retArray[i] = doubleBuffer.get(i);
            }
            // alt:
            // doubleBuffer.get(retArray);
            return retArray;
        }
        throw new InvalidParameterException("floatBuffer and doubleBuffer must not both be null");
    }

    protected static double integralSimple(final DataSet function) {
        double integral1 = 0.0;
        double integral2 = 0.0;

        if (function.getDataCount(DataSet.DIM_X) <= 1) {
            return 0.0;
        }
        if (function instanceof DataSet3D) {
            LOGGER.warn("integral not implemented for DataSet3D");
            return 0.0;
        }
        for (int i = 1; i < function.getDataCount(DataSet.DIM_X); i++) {
            final double step = function.get(DataSet.DIM_X, i) - function.get(DataSet.DIM_X, i - 1);
            final double val1 = function.get(DataSet.DIM_Y, i - 1);
            final double val2 = function.get(DataSet.DIM_Y, i);

            integral1 += step * val1;
            integral2 += step * val2;
        }
        return 0.5 * (integral1 + integral2);
    }

    /**
     * @param data
     *            the input vector
     * @return average of vector elements
     */
    protected static synchronized double mean(final double[] data) {
        if (data.length <= 0) {
            return Double.NaN;
        }
        final double norm = 1.0 / (data.length);
        double val = 0.0;
        for (int i = 0; i < data.length; i++) {
            val += norm * data[i];
        }
        return val;
    }

    /**
     * @param data
     *            the input vector
     * @return un-biased r.m.s. of vector elements
     */
    protected static synchronized double rootMeanSquare(final double[] data) {
        if (data.length <= 0) {
            return Double.NaN;
        }

        final double norm = 1.0 / (data.length);
        double val1 = 0.0;
        double val2 = 0.0;
        for (int i = 0; i < data.length; i++) {
            val1 += data[i];
            val2 += data[i] * data[i];
        }

        val1 *= norm;
        val2 *= norm;
        // un-biased rms!
        return Math.sqrt(Math.abs(val2 - (val1 * val1)));
    }
}
