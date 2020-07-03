/*****************************************************************************
 * * Chart Common - dataset consisting of other dataset fragments * * modified: 2019-01-23 Harald Braeuning * *
 ****************************************************************************/

package de.gsi.dataset.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;

/**
 * @author braeun
 */
public class FragmentedDataSet extends AbstractDataSet<FragmentedDataSet> implements DataSet2D {
    private static final long serialVersionUID = 2540953806461866839L;
    protected int dataCount;
    // protected double xmin;
    // protected double xmax;
    // protected double ymin;
    // protected double ymax;
    protected final ArrayList<DataSet> list = new ArrayList<>();

    /**
     * @param name data set name
     */
    public FragmentedDataSet(final String name) {
        super(name, 2);
    }

    /**
     * @param set new data set to be added to list
     */
    public void add(final DataSet set) {
        lock().writeLockGuard(() -> {
            list.add(set);
            /* Trace data is expected to be sorted in ascending order */
            Collections.sort(list,
                    (o1, o2) -> Double.compare(o1.getAxisDescription(DIM_X).getMin(), o2.getAxisDescription(DIM_X).getMin()));
            dataCount += set.getDataCount();
            getAxisDescription(DIM_X).add(set.getAxisDescription(DIM_X).getMax());
            getAxisDescription(DIM_X).add(set.getAxisDescription(DIM_X).getMin());
            getAxisDescription(DIM_Y).add(set.getAxisDescription(DIM_Y).getMax());
            getAxisDescription(DIM_Y).add(set.getAxisDescription(DIM_Y).getMin());
        });
        fireInvalidated(new AddedDataEvent(this, "added data set"));
    }

    /**
     * adds new custom x and y array values (internally generates a new DataSet)
     * 
     * @param xValues new X coordinates
     * @param yValues new Y coordinates
     */
    public void add(final double[] xValues, final double[] yValues) {
        // TODO: harald -> please check whether this is supposed to be deep copy
        // (true) or by reference (false) hand over (assumption here is 'by reference/pointer' -> remove this comment
        // once checked
        final DoubleDataSet set = new DoubleDataSet(String.format("Fragement #%d", list.size() + 1), xValues, yValues,
                xValues.length, false);
        add(set);
    }

    /**
     * clears all sub-dataset references
     */
    public void clear() {
        lock().writeLockGuard(() -> {
            dataCount = 0;
            list.clear();
            fireInvalidated(new UpdatedDataEvent(this, "clear()"));
        });
    }

    @Override
    public int getDataCount() {
        return dataCount;
    }

    /**
     * @return number of sub-datasets
     */
    public int getDatasetCount() {
        return list.size();
    }

    /**
     * @return sub-datasets
     */
    public Collection<DataSet> getDatasets() {
        return list;
    }

    @Override
    public String getStyle(int i) {
        for (final DataSet dataset : list) {
            if (i < dataset.getDataCount()) {
                return dataset.getStyle(i);
            }
            i -= dataset.getDataCount();
        }
        return "";
    }

    @Override
    public double get(final int dimIndex, final int index) {
        int i = index;
        for (final DataSet dataset : list) {
            if (i < dataset.getDataCount()) {
                return dataset.get(dimIndex, i);
            }
            i -= dataset.getDataCount();
        }
        return Double.NaN;
    }

    @Override
    public double[] getValues(final int dimIndex) {
        return lock().readLockGuard(() -> {
            final double[] tmp = new double[dataCount];
            int index = 0;
            for (final DataSet dataset : list) {
                for (int i = 0; i < dataset.getDataCount(); i++) {
                    tmp[index++] = dataset.get(dimIndex, i);
                }
            }
            return tmp;
        });
    }
}
