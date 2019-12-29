/*****************************************************************************
 * *
 * Chart Common - dataset wrapping another dataset *
 * *
 * modified: 2019-01-23 Harald Braeuning *
 * *
 ****************************************************************************/

package de.gsi.dataset.spi;

import java.util.ArrayList;
import java.util.List;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.UpdatedDataEvent;

/**
 * A data set implementation which wraps another data set.
 *
 * @author braeun
 */
public class WrappedDataSet extends AbstractDataSet<WrappedDataSet> implements DataSet2D {
    private static final long serialVersionUID = -2324840899629186284L;
    private DataSet2D dataset;
    private final EventListener listener = s -> datasetInvalidated();

    /**
     * @param name data set name
     */
    public WrappedDataSet(final String name) {
        super(name, 2);
    }

    private void datasetInvalidated() {
        recomputeLimits(0);
        recomputeLimits(1);
        fireInvalidated(new UpdatedDataEvent(this));
    }

    @Override
    public double get(int dimIndex, int index) {
        return dimIndex == DIM_X ? getX(index) : getY(index);
    }

    @Override
    public List<AxisDescription> getAxisDescriptions() {
        return dataset == null ? new ArrayList<>() : dataset.getAxisDescriptions();
    }

    @Override
    public int getDataCount(final int dimIndex) {
        return dataset == null ? 0 : dataset.getDataCount();
    }

    /**
     * @return wrapped internal data set
     */
    public DataSet2D getDataset() {
        return dataset;
    }

    /**
     * Returns the name of the dataset. This will return the name of the wrapped dataset. If no dataset is wrapped, the
     * name of this object is returned.
     * 
     * @return name of the dataset
     */
    @Override
    public String getName() {
        if (dataset != null)
            return dataset.getName();
        return super.getName();
    }

    @Override
    public String getStyle(final int index) {
        return dataset == null ? null : dataset.getStyle(index);
    }

    @Override
    public double getX(final int i) {
        return dataset == null ? 0 : dataset.getX(i);
    }

    @Override
    public int getXIndex(final double x) {
        return dataset == null ? 0 : dataset.getXIndex(x);
    }

    @Override
    public double getY(final int i) {
        return dataset == null ? 0 : dataset.getY(i);
    }

    /**
     * update/overwrite internal data set with content from other data set
     * 
     * @param dataset new data set
     */
    public void setDataset(final DataSet2D dataset) {
        if (this.dataset != null) {
            this.dataset.removeListener(listener);
        }
        this.dataset = dataset;
        if (this.dataset != null) {
            this.dataset.addListener(listener);
        }
        // xRange.setMax(Double.NaN);
        // yRange.setMax(Double.NaN);
        fireInvalidated(new UpdatedDataEvent(this));
    }

}
