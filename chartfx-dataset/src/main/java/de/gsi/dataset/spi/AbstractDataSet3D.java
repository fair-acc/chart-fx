package de.gsi.dataset.spi;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet3D;

/**
 * An abstract implementation of <code>DataSet3D</code> interface.
 *
 * @author rstein
 * @param <D> java generics handling of DataSet for derived classes (needed for fluent design)
 */
public abstract class AbstractDataSet3D<D extends AbstractDataSet3D<D>> extends AbstractDataSet<D>
        implements DataSet3D {
    private static final long serialVersionUID = 2766945109681463872L;

    /**
     * Creates a new <code>AbstractDataSet3D</code>.
     *
     * @param name
     *            name of this data set.
     */
    public AbstractDataSet3D(final String name) {
        super(name);
        getAxisDescriptions().add(new DefaultAxisDescription(this, "z-Axis", "a.u."));
    }

    /**
     * Returns total number of data points in this data set:<br>
     * <code>xDataCount * yDataCount</code>.
     */
    @Override
    public int getDataCount() {
        return getXDataCount() * getYDataCount();
    }

    //    @Override
    //    public DataRange getZRange() {
    //        if (!zRange.isDefined()) {
    //            computeLimits();
    //        }
    //        return zRange;
    //    }

    /**
     * recompute data set limits
     */
    @Override
    public D recomputeLimits(final int dimension) {
        lock().writeLockGuard(() -> {
            // Clear previous ranges
            getAxisDescription(dimension).empty();

            if (dimension == 0) {
                // x-range
                final int dataCount = getXDataCount();
                AxisDescription axisRange = getAxisDescription(dimension);
                for (int i = 0; i < dataCount; i++) {
                    axisRange.add(getX(i));
                }
            } else if (dimension == 1) {
                // x-range
                final int dataCount = getYDataCount();
                AxisDescription axisRange = getAxisDescription(dimension);
                for (int i = 0; i < dataCount; i++) {
                    axisRange.add(getY(i));
                }
            } else {
                // z-range
                final int xDataCount = getXDataCount();
                final int yDataCount = getYDataCount();
                AxisDescription axisRange = getAxisDescription(dimension);
                for (int i = 0; i < xDataCount; i++) {
                    for (int j = 0; j < yDataCount; j++) {
                        axisRange.add(getZ(i, j));
                    }
                }
            }
        });
        return getThis();
    }

    /**
     * Gets the index of the data point closest to the given x coordinate. The
     * index returned may be less then zero or larger the the number of data
     * points in the data set, if the x coordinate lies outside the range of the
     * data set.
     *
     * @param x
     *            the x position of the data point
     * @return the index of the data point
     */
    @Override
    public int getXIndex(final double x) {
        if (getXDataCount() == 0) {
            return 0;
        }
        if (x < getX(0)) {
            return 0;
        }
        final int lastIndex = getXDataCount() - 1;
        if (x > getX(lastIndex)) {
            return lastIndex;
        }
        // binary closest search
        return binarySearchX(x, 0, lastIndex);
    }

    /**
     * Gets the first index of the data point closest to the given y coordinate.
     *
     * @param y
     *            the y position of the data point
     * @return the index of the data point
     */
    @Override
    public int getYIndex(final double y) {
        if (getYDataCount() == 0) {
            return 0;
        }
        if (y < getY(0)) {
            return 0;
        }
        final int lastIndex = getYDataCount() - 1;
        if (y > getY(lastIndex)) {
            return lastIndex;
        }
        // binary closest search
        return binarySearchY(y, 0, lastIndex);
    }

}
