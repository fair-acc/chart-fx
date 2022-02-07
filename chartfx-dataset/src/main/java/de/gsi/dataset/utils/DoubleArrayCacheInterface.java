package de.gsi.dataset.utils;

/**
 * Interface to cache double-arrays (double[]) in order to minimise memory re-allocation.
 */
public interface DoubleArrayCacheInterface {

    double[] getArray(int requiredSize);

    double[] getArrayExact(int requiredSize);

    boolean add(double[] recoveredObject);
    
    boolean contains(Object object);

    boolean remove(Object o);

    int size();
}
