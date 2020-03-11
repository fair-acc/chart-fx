package de.gsi.dataset.spi;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
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
     * @param name name of this data set.
     */
    public AbstractDataSet3D(final String name) {
        super(name, 3);
        getAxisDescriptions().add(new DefaultAxisDescription(this, "z-Axis", "a.u."));
    }

    /**
     * Returns total number of data points in this data set:<br>
     * <code>xDataCount * yDataCount</code>.
     */
    @Override
    public int getDataCount() {
        return getDataCount(DataSet.DIM_X) * getDataCount(DataSet.DIM_Y);
    }

    // @Override
    // public DataRange getZRange() {
    // if (!zRange.isDefined()) {
    // computeLimits();
    // }
    // return zRange;
    // }

    /**
     * recompute data set limits
     */
    @Override
    public D recomputeLimits(final int dimension) {
        // Clear previous ranges
        getAxisDescription(dimension).clear();

        if (dimension == 0) {
            // x-range
            final int dataCount = getDataCount(DataSet.DIM_X);
            AxisDescription axisRange = getAxisDescription(dimension);
            for (int i = 0; i < dataCount; i++) {
                axisRange.add(getX(i));
            }
        } else if (dimension == 1) {
            // x-range
            final int dataCount = getDataCount(DataSet.DIM_Y);
            AxisDescription axisRange = getAxisDescription(dimension);
            for (int i = 0; i < dataCount; i++) {
                axisRange.add(getY(i));
            }
        } else {
            // z-range
            final int xDataCount = getDataCount(DataSet.DIM_X);
            final int yDataCount = getDataCount(DataSet.DIM_Y);
            AxisDescription axisRange = getAxisDescription(dimension);
            for (int i = 0; i < xDataCount; i++) {
                for (int j = 0; j < yDataCount; j++) {
                    axisRange.add(getZ(i, j));
                }
            }
        }
        return getThis();
    }
}
