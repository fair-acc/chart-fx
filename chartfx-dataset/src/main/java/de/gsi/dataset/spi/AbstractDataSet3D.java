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
    }

    /**
     * Returns total number of data points in this data set:<br>
     * <code>xDataCount * yDataCount</code>.
     */
    @Override
    public int getDataCount() {
        return getDataCount(DataSet.DIM_X) * getDataCount(DataSet.DIM_Y);
    }

}
