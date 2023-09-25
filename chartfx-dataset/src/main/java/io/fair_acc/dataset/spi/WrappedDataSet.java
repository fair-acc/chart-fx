package io.fair_acc.dataset.spi;

import java.util.ArrayList;
import java.util.List;

import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.events.ChartBits;

/**
 * A data set implementation which wraps another data set.
 *
 * @author braeun
 */
public class WrappedDataSet extends AbstractDataSet<WrappedDataSet> implements DataSet {
    private static final long serialVersionUID = -2324840899629186284L;
    private DataSet dataset;

    /**
     * @param name data set name
     */
    public WrappedDataSet(final String name) {
        super(name, 2);
    }

    private void datasetInvalidated() {
        // invalidate ranges
        getAxisDescriptions().forEach(AxisDescription::clear);
        fireInvalidated(ChartBits.DataSetData);
    }

    @Override
    public double get(int dimIndex, int index) {
        return dataset == null ? 0.0 : dataset.get(dimIndex, index);
    }

    @Override
    public List<AxisDescription> getAxisDescriptions() {
        return dataset == null ? new ArrayList<>() : dataset.getAxisDescriptions();
    }

    @Override
    public int getDataCount() {
        return dataset == null ? 0 : dataset.getDataCount();
    }

    /**
     * @return wrapped internal data set
     */
    public DataSet getDataset() {
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

    /**
     * update/overwrite internal data set with content from other data set
     *
     * @param dataset new data set
     */
    public void setDataset(final DataSet dataset) {
        if (this.dataset != null) {
            this.dataset.removeListener(this);
        }
        this.dataset = dataset;
        if (this.dataset != null) {
            this.dataset.addListener(this);
        }
        fireInvalidated(ChartBits.DataSetData);
    }

    @Override
    public DataSet set(final DataSet other, final boolean copy) {
        lock().writeLockGuard(() -> other.lock().writeLockGuard(() -> this.setDataset(other)));
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }
}
