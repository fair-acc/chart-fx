package de.gsi.dataset.utils;

/**
 * Proof of concept for a user definable array cache
 *
 * @author ennerf
 */
public interface ArrayPool<T> {

    public interface DoubleArrayPool extends ArrayPool<double[]> {
    }

    public T allocate(int requiredSize);

    public void release(T array);

}
