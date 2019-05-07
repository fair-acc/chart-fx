/*****************************************************************************
 * * Chart Common - dataset consisting of other dataset fragments * * modified: 2019-01-23 Harald Braeuning * *
 ****************************************************************************/

package de.gsi.chart.data.spi;

import java.util.ArrayList;
import java.util.Collections;

import de.gsi.chart.data.DataSet;

/**
 * @author braeun
 */
public class FragmentedDataSet extends AbstractDataSet<FragmentedDataSet> {

    protected int dataCount;
    //    protected double xmin;
    //    protected double xmax;
    //    protected double ymin;
    //    protected double ymax;
    protected final ArrayList<DataSet> list = new ArrayList<>();

    public FragmentedDataSet(final String name) {
        super(name);
    }

    /**
     * Returns a copy of the dataset. However, in contrast to a deep copy only the references to the contained dataset
     * are copied.
     * 
     * @return copy of the dataset
     */
    @Deprecated
    public AbstractDataSet<?> shallowCopy() {
        final FragmentedDataSet d = new FragmentedDataSet(getName());
        d.dataCount = dataCount;
        d.xRange = xRange;
        d.yRange = yRange;
        //    d.xmin = xmin;
        //    d.xmax = xmax;
        //    d.ymin = ymin;
        //    d.ymax = ymax;
        d.list.addAll(list);
        return d;
    }

    @Override
    public String toString() {
        return "FragmentedDataSet " + getXRange() + " " + getYRange() + " [" + dataCount + "]";
    }

    public void clear() {
        lock();
        try {
            dataCount = 0;
            list.clear();
            fireInvalidated();
        } finally {
            unlock();
        }
    }

    public void add(final double[] xValues, final double[] yValues) {
        final DoubleDataSet set = new DoubleDataSet(String.format("Fragement #%d", list.size() + 1), xValues, yValues);
        add(set);
    }

    public void add(final DataSet set) {
        lock();
        try {
            list.add(set);
            /* Trace data is expected to be sorted in ascending order */
            Collections.sort(list, (o1, o2) -> Double.compare(o1.getXMin(), o2.getXMin()));
            dataCount += set.getDataCount();
        } finally {
            unlock();
        }
        computeLimits();
        fireInvalidated();
    }

    public int getDatasetCount() {
        return list.size();
    }

    @Override
    public double[] getYValues() {
        lock();
        try {
            final double[] tmp = new double[dataCount];
            int index = 0;
            for (final DataSet dataset : list) {
                for (int i = 0; i < dataset.getDataCount(); i++) {
                    tmp[index++] = dataset.getY(i);
                }
            }
            return tmp;
        } finally {
            unlock();
        }
    }

    @Override
    public double[] getXValues() {
        lock();
        try {
            final double[] tmp = new double[dataCount];
            int index = 0;
            for (final DataSet dataset : list) {
                for (int i = 0; i < dataset.getDataCount(); i++) {
                    tmp[index++] = dataset.getX(i);
                }
            }
            return tmp;
        } finally {
            unlock();
        }
    }

    @Override
    public int getDataCount() {
        return dataCount;
    }

    @Override
    public int getDataCount(final double xmin, final double xmax) {
        int from = getXIndex(xmin);
        if (from < 0) {
            from = 0;
        }
        int to = getXIndex(xmax) + 1;
        if (to > getDataCount()) {
            to = getDataCount();
        }
        return to - from;
    }

    @Override
    public double getX(int i) {
        for (final DataSet dataset : list) {
            if (i < dataset.getDataCount()) {
                return dataset.getX(i);
            }
            i -= dataset.getDataCount();
        }
        return 0;
    }

    @Override
    public double getY(int i) {
        for (final DataSet dataset : list) {
            if (i < dataset.getDataCount()) {
                return dataset.getY(i);
            }
            i -= dataset.getDataCount();
        }
        return 0;
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
    public int getXIndex(double x) {
        lock();
        try {
            if (x < getXMin()) {
                return 0;
            }
            int index = 0;
            for (final DataSet dataset : list) {
                if (x >= dataset.getXMin() && x <= dataset.getXMax()) {
                    return index + dataset.getXIndex(x);
                }
                index += dataset.getDataCount();
            }
            return getDataCount();
        } finally {
            unlock();
        }
    }

    //  @Override
    //  public void opScale(double f)
    //  {
    //    lock();
    //    try
    //    {
    //      for (AbstractTraceDataSet ds : list) ds.opScale(f);
    //      updateLimits();
    //      fireInvalidated();
    //    }
    //    finally
    //    {
    //      unlock();
    //    }
    //  }
    //
    //  @Override
    //  public void opAdd(AbstractTraceDataSet ds)
    //  {
    //    if (!isCompatible(ds)) throw new IllegalArgumentException("Datasets do not match");
    //    lock();
    //    try
    //    {
    //      FragmentedDataSet fds = (FragmentedDataSet)ds;
    //      for (int i=0;i<list.size();i++) list.get(i).opAdd(fds.list.get(i));
    //      updateLimits();
    //      fireInvalidated();
    //    }
    //    finally
    //    {
    //      unlock();
    //    }
    //  }
    //
    //  @Override
    //  public void opSub(AbstractTraceDataSet ds)
    //  {
    //    if (!isCompatible(ds)) throw new IllegalArgumentException("Datasets do not match");
    //    lock();
    //    try
    //    {
    //      FragmentedDataSet fds = (FragmentedDataSet)ds;
    //      for (int i=0;i<list.size();i++) list.get(i).opSub(fds.list.get(i));
    //      updateLimits();
    //      fireInvalidated();
    //    }
    //    finally
    //    {
    //      unlock();
    //    }
    //  }

    //  public AbstractDataSet subset(int from)
    //  {
    //    FragmentedDataSet d = new FragmentedDataSet(getName());
    //    d.dataCount = 0;
    ////    d.setXOffset(getXOffset()+from*getXScale());
    ////    d.setXScale(getXScale());
    //    d.xmin = xmin;
    //    d.xmax = xmax;
    //    d.ymin = ymin;
    //    d.ymax = ymax;
    //    for (AbstractDataSet set : list)
    //    {
    //      if (set.getDataCount() < from)
    //      {
    //        from -= set.getDataCount();
    //      }
    //      else
    //      {
    //        AbstractTraceDataSet ds = set.deepCopy();
    //        if (from > 0)
    //        {
    //          ds = (AbstractTraceDataSet)ds.subset(from);
    //          from = 0;
    //        }
    //        d.list.add(ds);
    //        d.dataCount += set.getDataCount();
    //      }
    //    }
    //    d.computeLimits();
    //    return d;
    //  }

    //  public boolean isCompatible(AbstractDataSet ds)
    //  {
    //    if (!(ds instanceof FragmentedDataSet)) return false;
    //    lock();
    //    try
    //    {
    //      FragmentedDataSet fds = (FragmentedDataSet)ds;
    //      if (list.size() != fds.list.size()) return false;
    //      return true;
    //    }
    //    finally
    //    {
    //      unlock();
    //    }
    //  }

}
