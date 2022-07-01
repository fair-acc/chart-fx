package io.fair_acc.dataset.spi;

import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.event.AddedDataEvent;
import io.fair_acc.dataset.event.UpdatedDataEvent;
import io.fair_acc.dataset.DataSet;

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

    @Override
    public void add(final DataSet set) {
        while (!list.isEmpty() && list.size() >= depth) {
            final DataSet ds = list.remove(0);
            dataCount -= ds.getDataCount();
        }
        for (final DataSet ds : list) {
            ((InternalDataSet) ds).shift(-lastLength);
        }
        list.add(new InternalDataSet(set));
        dataCount += set.getDataCount();
        lastLength = set.getAxisDescription(DIM_X).getMax();
        // invalidate ranges
        getAxisDescriptions().forEach(AxisDescription::clear);
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

    private static class InternalDataSet extends DoubleDataSet {
        private static final long serialVersionUID = 1L;

        public InternalDataSet(DataSet ds) {
            super(ds);
            // invalidate ranges
            getAxisDescriptions().forEach(AxisDescription::clear);
        }

        public void shift(double value) {
            lock().writeLockGuard(() -> {
                for (int i = 0; i < xValues.size(); i++) {
                    this.getValues(DIM_X)[i] += value;
                }
            });
            fireInvalidated(new UpdatedDataEvent(this));
        }
    }
}
