/** ***************************************************************************
 *                                                                            *
 * BI Common - Rolling data set                                               *
 *                                                                            *
 * modified: 2018-08-24 Harald Braeuning                                      *
 *                                                                            *
 **************************************************************************** */
package de.gsi.chart.data.spi;

import de.gsi.chart.data.DataSet;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * @author braeun
 */
public class RollingDataSet extends FragmentedDataSet {

  private double lastLength;
  private final SimpleIntegerProperty depth = new SimpleIntegerProperty(0);

  public RollingDataSet(final String name)
  {
    super(name);
  }

//  @Override
//  public void add(final DataSet set)
//  {
//    ArrayList<DataSet> tmp = new ArrayList<>(list);
//    while (tmp.size() >= depth.get())
//    {
//      tmp.remove(0);
//    }
//    list.clear();
//    dataCount = 0;
//    /*
//     * Because AbstractDataSet does not allow editing of its data, we have to
//     * copy the dataset to shift the y values
//     */
//    for (final DataSet ds : tmp)
//    {
//      double[] x = new double[ds.getDataCount()];
//      double[] y = new double[ds.getDataCount()];
//      for (int i = 0; i < ds.getDataCount(); i++)
//      {
//        x[i] = ds.getX(i) - lastLength;
//        y[i] = ds.getY(i);
//      }
//      list.add(new DoubleDataSet(ds.getName(), x, y, false));
//      dataCount += ds.getDataCount();
//    }
//    list.add(set);
//    dataCount += set.getDataCount();
//    lastLength = set.getXMax();
//    computeLimits();
//    fireInvalidated();
//  }

  @Override
  public void add(final DataSet set)
  {
    while (list.size() >= depth.get())
    {
      final DataSet ds = list.remove(0);
      dataCount -= ds.getDataCount();
    }
    for (final DataSet ds : list)
    {
      ((InternalDataSet)ds).shift(-lastLength);
    }
    list.add(new InternalDataSet(set));
    dataCount += set.getDataCount();
    lastLength = set.getXMax();
    computeLimits();
    fireInvalidated();
  }

  public double getLastLength()
  {
    return lastLength;
  }

  public void setDepth(final int d)
  {
    depth.set(d);
  }

  public int getDepth()
  {
    return depth.get();
  }

  public SimpleIntegerProperty depthProperty()
  {
    return depth;
  }



  private class InternalDataSet extends DoubleDataSet {

    public InternalDataSet(DataSet ds)
    {
      super(ds.getName());
      xValues = new double[ds.getDataCount()];
      yValues = new double[ds.getDataCount()];
      for (int i=0;i<yValues.length;i++)
      {
        xValues[i] = ds.getX(i);
        yValues[i] = ds.getY(i);
      }
      computeLimits();
    }

    public void shift(double value)
    {
      for (int i=0;i<xValues.length;i++) {
		xValues[i] += value;
	}
      computeLimits();
    }

  }



}
