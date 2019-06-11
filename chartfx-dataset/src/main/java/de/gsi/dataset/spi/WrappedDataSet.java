/*****************************************************************************
 *                                                                           *
 * Chart Common - dataset wrapping another dataset                           *
 *                                                                           *
 * modified: 2019-01-23 Harald Braeuning                                     *
 *                                                                           *
 ****************************************************************************/

package de.gsi.dataset.spi;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.UpdatedDataEvent;

/**
 * A data set implementation which wraps another data set.
 *
 * @author braeun
 */
public class WrappedDataSet extends AbstractDataSet<WrappedDataSet> {

    private DataSet dataset;
    private final EventListener listener = s -> datasetInvalidated();

    /**
     * 
     * @param name data set name
     */
    public WrappedDataSet(final String name) {
        super(name);
    }

    /**
     * Returns the name of the dataset. This will return the name of the wrapped
     * dataset. If no dataset is wrapped, the name of this object is returned.
     * @return name of the dataset
     */
    @Override
    public String getName()
    {
      if (dataset != null) return dataset.getName();
      return super.getName();
    }
    
    /**
     * update/overwrite internal data set with content from other data set
     * @param dataset new data set
     */
    public void setDataset(final DataSet dataset) {
        if (this.dataset != null) {
            this.dataset.removeListener(listener);
        }
        this.dataset = dataset;
        if (this.dataset != null) {
            this.dataset.addListener(listener);
        }
//        xRange.setMax(Double.NaN);
//        yRange.setMax(Double.NaN);
        fireInvalidated(new UpdatedDataEvent(this));
    }

    /**
     * 
     * @return wrapped internal data set
     */
    public DataSet getDataset() {
        return dataset;
    }

    @Override
    public int getDataCount() {
        return dataset == null ? 0 : dataset.getDataCount();
    }

    @Override
    public int getDataCount(final double xmin, final double xmax) {
        return dataset == null ? 0 : dataset.getDataCount(xmin, xmax);
    }

    @Override
    public double getX(final int i) {
        return dataset == null ? 0 : dataset.getX(i);
    }

    @Override
    public double getY(final int i) {
        return dataset == null ? 0 : dataset.getY(i);
    }

    @Override
    public int getXIndex(final double x) {
        return dataset == null ? 0 : dataset.getXIndex(x);
    }

    @Override
    public double getXMin() {
        return dataset == null ? 0 : dataset.getXMin();
    }

    @Override
    public double getXMax() {
        return dataset == null ? 0 : dataset.getXMax();
    }

    @Override
    public double getYMin() {
        return dataset == null ? 0 : dataset.getYMin();
    }

    @Override
    public double getYMax() {
        return dataset == null ? 0 : dataset.getYMax();
    }

    private void datasetInvalidated() {
//        xRange.setMax(Double.NaN);
//        yRange.setMax(Double.NaN);
        computeLimits();
        fireInvalidated(new UpdatedDataEvent(this));
    }

    @Override
    public String getStyle(final int index) {
        return dataset == null ? null : dataset.getStyle(index);
    }

}
