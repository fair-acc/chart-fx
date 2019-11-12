/**
 * ***************************************************************************
 * *
 * BI Common - Rolling data set *
 * *
 * modified: 2018-08-24 Harald Braeuning *
 * *
 */
package de.gsi.dataset.spi;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;

/**
 * @author braeun
 */
public class RollingDataSet extends FragmentedDataSet {
    private static final long serialVersionUID = -3110510420345581034L;
    private double lastLength;
    private int depth = 0;

    /**
     * @param name data set name
     */
    public RollingDataSet(final String name) {
        super(name);
    }

    // @Override
    // public void add(final DataSet set)
    // {
    // ArrayList<DataSet> tmp = new ArrayList<>(list);
    // while (tmp.size() >= depth.get())
    // {
    // tmp.remove(0);
    // }
    // list.clear();
    // dataCount = 0;
    // /*
    // * Because AbstractDataSet does not allow editing of its data, we have to
    // * copy the dataset to shift the y values
    // */
    // for (final DataSet ds : tmp)
    // {
    // double[] x = new double[ds.getDataCount()];
    // double[] y = new double[ds.getDataCount()];
    // for (int i = 0; i < ds.getDataCount(); i++)
    // {
    // x[i] = ds.getX(i) - lastLength;
    // y[i] = ds.getY(i);
    // }
    // list.add(new DoubleDataSet(ds.getName(), x, y, false));
    // dataCount += ds.getDataCount();
    // }
    // list.add(set);
    // dataCount += set.getDataCount();
    // lastLength = set.getXMax();
    // computeLimits();
    // fireInvalidated();
    // }

    @Override
    public void add(final DataSet set) {
        while (list.size() >= depth) {
            final DataSet ds = list.remove(0);
            dataCount -= ds.getDataCount();
        }
        for (final DataSet ds : list) {
            ((InternalDataSet) ds).shift(-lastLength);
        }
        list.add(new InternalDataSet(set));
        dataCount += set.getDataCount();
        lastLength = set.getAxisDescription(0).getMax();
        recomputeLimits(0);
        recomputeLimits(1);
        fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * @return maximum depth before points are being dropped
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @return distance to last data point
     */
    public double getLastLength() {
        return lastLength;
    }

    /**
     * @param d maximum depth before points are being dropped
     */
    public void setDepth(final int d) {
        depth = d;
    }

    private class InternalDataSet extends DoubleDataSet {

        private static final long serialVersionUID = 1L;

        public InternalDataSet(DataSet ds) {
            super(ds);
            recomputeLimits(0);
            recomputeLimits(1);
        }

        public void shift(double value) {
            lock().writeLockGuard(() -> {
                for (int i = 0; i < xValues.size(); i++) {
                    this.getXValues()[i] += value;
                }
            });
            fireInvalidated(new UpdatedDataEvent(this));
        }

    }

}
