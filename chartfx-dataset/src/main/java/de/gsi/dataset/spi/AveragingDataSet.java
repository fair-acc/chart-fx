package de.gsi.dataset.spi;

import java.util.ArrayDeque;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.AddedDataEvent;

/**
 * TODO: Change to ErrorDataSet and calculate standard deviation.
 * 
 * @author braeun
 */
public class AveragingDataSet extends AbstractDataSet<AveragingDataSet> implements DataSet {
    private static final long serialVersionUID = 1L;
    private int averageSize = 1;
    private int fuzzyCount = 0;
    private InternalDataSet dataset;
    private final ArrayDeque<DataSet> deque = new ArrayDeque<>();

    /**
     * @param name data set name
     * @param nDims number of dimensions
     */
    public AveragingDataSet(final String name, final int nDims) {
        this(name, nDims, 0);
    }

    /**
     * @param name data set name
     * @param nDims number of dimensions
     * @param fuzzyCount binning accuracy @see #setFuzzyCount
     */
    public AveragingDataSet(String name, final int nDims, int fuzzyCount) {
        super(name, nDims);
        this.fuzzyCount = fuzzyCount;
    }

    /**
     * @param ds new DataSet to be added to average
     */
    public void add(DataSet ds) {
        if (averageSize == 1) {
            dataset = new InternalDataSet(ds);
        } else if (dataset == null || deque.isEmpty()) {
            dataset = new InternalDataSet(ds);
            deque.clear();
            deque.add(new InternalDataSet(ds));
        } else if (deque.size() < averageSize) {
            dataset.opScale(deque.size());
            dataset.opAdd(ds);
            deque.add(new InternalDataSet(ds));
            dataset.opScale(1.0 / deque.size());
        } else {
            dataset.opScale(deque.size());
            dataset.opSub(deque.pop());
            dataset.opAdd(ds);
            deque.add(new InternalDataSet(ds));
            dataset.opScale(1.0 / deque.size());
        }
        dataset.recomputeLimits(DIM_X);
        dataset.recomputeLimits(DIM_Y);
        fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * clear all data
     */
    public void clear() {
        deque.clear();
        dataset = null;
    }

    /**
     * @return number of data sets that have been averaged
     */
    public int getAverageCount() {
        if (averageSize == 1) {
            return dataset == null ? 0 : 1;
        }
        return deque.size();
    }

    /**
     * @return number of data sets that are supposed to be averaged
     */
    public int getAverageSize() {
        return averageSize;
    }

    @Override
    public int getDataCount() {
        return dataset == null ? 0 : dataset.getDataCount();
    }

    /**
     * Gets the fuzzy count.
     * 
     * @return fuzzy count
     */
    public int getFuzzyCount() {
        return fuzzyCount;
    }

    @Override
    public String getStyle(int index) {
        if (dataset == null) {
            return "";
        }
        return dataset.getStyle(index);
    }

    @Override
    public final double get(final int dimIndex, final int index) {
        if (dataset == null) {
            return Double.NaN;
        }
        return dataset.get(dimIndex, index);
    }

    /**
     * @param avgCount number of data sets that are supposed to be averaged
     */
    public void setAverageSize(int avgCount) {
        if (avgCount < 1) {
            return;
        }
        if (averageSize != avgCount) {
            deque.clear();
            dataset = null;
        }
        averageSize = avgCount;
    }

    /**
     * Sets the fuzzy count. The fuzzy count allows for a mismatch in the data count of the datasets added to the
     * average. Datasets will be added if the size difference is less or equal to the fuzzy count. In case of a mismatch
     * in size, the longer dataset will be truncated to the shorter one.
     * 
     * @param fuzzyCount the fuzzy count
     */
    public void setFuzzyCount(int fuzzyCount) {
        this.fuzzyCount = fuzzyCount;
    }

    private class InternalDataSet extends DoubleErrorDataSet {
        private static final long serialVersionUID = 1L;

        public InternalDataSet(DataSet ds) {
            super(ds.getName(), ds.getValues(DataSet.DIM_X), ds.getValues(DataSet.DIM_Y), new double[ds.getDataCount()], new double[ds.getDataCount()],
                    ds.getDataCount(), true);
        }

        public boolean isCompatible(DataSet d) {
            return Math.abs(super.getDataCount() - d.getDataCount()) <= fuzzyCount;
        }

        public void opAdd(DataSet d) {
            if (!isCompatible(d)) {
                throw new IllegalArgumentException("Datasets do not match");
            }

            yValues.size(d.getDataCount());
            for (int i = 0; i < yValues.size(); i++) {
                yValues.elements()[i] += d.get(DataSet.DIM_Y, i);
            }
        }

        public void opScale(double f) {
            for (int i = 0; i < yValues.size(); i++) {
                yValues.elements()[i] *= f;
            }
        }

        public void opSub(DataSet d) {
            if (!isCompatible(d)) {
                throw new IllegalArgumentException("Datasets do not match");
            }
            yValues.size(d.getDataCount());
            for (int i = 0; i < yValues.size(); i++) {
                yValues.elements()[i] -= d.get(DataSet.DIM_Y, i);
            }
        }
    }

    @Override
    public double getValue(int dimIndex, double... x) {
        return get(dimIndex, getIndex(DIM_X, x[0]));
    }

    @Override
    public DataSet set(final DataSet other, final boolean copy) {
        if (other instanceof AveragingDataSet) {
            this.fuzzyCount = ((AveragingDataSet) other).getFuzzyCount();
            this.averageSize = ((AveragingDataSet) other).getAverageSize();
            if (copy) {
                this.clear();
                ((AveragingDataSet) other).deque.forEach(ds -> this.add(new DefaultDataSet(ds)));
            } else {
                this.dataset = ((AveragingDataSet) other).dataset;
                this.deque.clear();
                this.deque.addAll(((AveragingDataSet) other).deque);
            }
            return this;
        }
        // non AveragingDataSet: add the other data set as a single data set
        this.clear();
        if (copy) {
            this.add(new DefaultDataSet(other));
        } else {
            this.add(other);
        }
        return this;
    }
}
