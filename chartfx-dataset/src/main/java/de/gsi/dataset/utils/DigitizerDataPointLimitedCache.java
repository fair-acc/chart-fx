package de.gsi.dataset.utils;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.gsi.dataset.utils.DoubleArrayCacheInterface;

public class DigitizerDataPointLimitedCache implements DoubleArrayCacheInterface {

    private static final List<Reference<double[]>> contents = Collections.synchronizedList(new LinkedList<>());

    private static final int capacity = 64;

    @Override
    public double[] getArray(final int requiredSize) {
        return getArray(requiredSize, false);
    }

    @Override
    public double[] getArrayExact(final int requiredSize) {
        return getArray(requiredSize, true);
    }

    private double[] getArray(final int requiredSize, final boolean exact) {
        synchronized (contents) {
            double[] bestFit = null;
            int bestFitSize = Integer.MAX_VALUE;

            for (Iterator<Reference<double[]>> iterator = contents.iterator(); iterator.hasNext();) {
                Reference<double[]> candidate = iterator.next();
                final double[] localRef = candidate.get();
                
                if (localRef == null) {
                    iterator.remove();
                    continue;
                }

                final int sizeDiff = localRef.length - requiredSize;
                // Case: exact match
                if (sizeDiff == 0) {
                    bestFit = localRef;
                    iterator.remove();
                    return localRef;
                }

                if (!exact && sizeDiff > 0 && sizeDiff < bestFitSize) {
                    bestFitSize = sizeDiff;
                    bestFit = localRef;
                }
            }

            // Case: empty / no match
            if (bestFit == null) {
                // could not find any cached, return new double[]
                bestFit = new double[requiredSize];
                return bestFit;
            }

            // Case: close / best match
            remove(bestFit);
            return bestFit;
        }
    }

    @Override
    public boolean add(double[] recoveredObject) {
        if (recoveredObject != null) {
            synchronized (contents) {
                if (contains(recoveredObject)) {
                    return false;
                }

                while (contents.size() >= capacity) {
                    contents.remove(0); // Remove the first / oldest entry
                }

                return contents.add(new SoftReference<>(recoveredObject));
            }
        }
        return false;
    }

    public boolean contains(Object object) {
        if (object != null) {
            synchronized (contents) {
                for (Reference<double[]> weakReference : contents) {
                    if (object.equals(weakReference.get())) return true;
                }
            }
        }
        return false;
    }

    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        synchronized (contents) {
            Iterator<Reference<double[]>> iter = contents.iterator();
            while (iter.hasNext()) {
                final Reference<double[]> candidate = iter.next();
                final double[] test = candidate.get();
                if (test == null) {
                    iter.remove();
                    continue;
                }
                if (o.equals(test)) {
                    iter.remove();
                    return true;
                }
            }
        }
        return false;
    }

    public int size() {
        synchronized (contents) {
            cleanup();
            return contents.size();
        }
    }

    protected void cleanup() {
        synchronized (contents) {
            Iterator<Reference<double[]>> iterator = contents.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().get() == null) {
                    iterator.remove();
                }
            }
        }
    }
}
