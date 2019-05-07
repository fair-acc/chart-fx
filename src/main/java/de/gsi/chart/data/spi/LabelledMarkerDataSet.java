/*****************************************************************************
 *                                                                           *
 * Common Chart - data set for labeled markers                               *
 *                                                                           *
 * modified: 2018-11-13 Harald Braeuning                                     *
 *                                                                           *
 ****************************************************************************/

package de.gsi.chart.data.spi;

import java.util.Arrays;
import java.util.List;

import de.gsi.chart.data.DataSet;
import de.gsi.chart.utils.AssertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Minor extension to <code>DefaultDataSet</code> to easier handle labeled markers.
 * @author braeun
 */
public class LabelledMarkerDataSet extends AbstractDataSet<DefaultDataSet> implements DataSet {

  protected ObservableList<String> dataLabels = FXCollections.observableArrayList();
  protected ObservableList<String> dataStyles = FXCollections.observableArrayList();
  protected ObservableList<DoublePoint> data = FXCollections.observableArrayList();

  public LabelledMarkerDataSet(String name)
  {
    super(name);
  }

    public ObservableList< String> getDataLabelProperty() {
        return dataLabels;
    }

    public ObservableList<String> getDataStyleProperty() {
        return dataStyles;
    }

    public ObservableList<DoublePoint> getDataProperty() {
        return data;
    }

    @Override
    public int getDataCount() {
        return data.size();
    }

    public DefaultDataSet clearData() {
        lock().setAutoNotifaction(false);

        data.clear();
        dataLabels.clear();
        dataStyles.clear();

        xRange.empty();
        yRange.empty();

        return setAutoNotifaction(true).unlock().fireInvalidated();
    }

    @Override
    public double getX(final int index) {
        return data.get(index).getX();
    }

    @Override
    public double getY(final int index) {
        return data.get(index).getY();
    }

  public LabelledMarkerDataSet add(LabelledMarker marker)
  {
    AssertUtils.notNull("marker",marker);
    lock();
    try
    {
      data.add(new DoublePoint(marker.getX(),marker.getY()));
      xRange.add(marker.getX());
      yRange.add(marker.getY());
      dataLabels.add(marker.getLabel());
      dataStyles.add(marker.getStyle());
    }
    finally
    {
      unlock();
    }
    fireInvalidated();
    return this;
  }

  public LabelledMarkerDataSet set(List<LabelledMarker> markers)
  {
    AssertUtils.notNull("markers",markers);
    lock();
    try
    {
      setAutoNotifaction(false);
      data.clear();
      dataLabels.clear();
      dataStyles.clear();
      xRange.empty();
      yRange.empty();
      for (LabelledMarker marker : markers) {
        final double x = marker.getX();
        final double y = marker.getY();
        data.add(new DoublePoint(x,y));
        xRange.add(x);
        yRange.add(y);
        dataLabels.add(marker.getLabel());
        dataStyles.add(marker.getStyle());
      }
      setAutoNotifaction(true);
    }
    finally
    {
      unlock();
    }
    fireInvalidated();
    return this;
  }

  public LabelledMarkerDataSet set(LabelledMarker[] markers)
  {
    AssertUtils.notNull("markers",markers);
    return set(Arrays.asList(markers));
  }

  public LabelledMarkerDataSet set(int index, LabelledMarker marker)
  {
    AssertUtils.indexInBounds(index, getDataCount());
    lock();
    try
    {
      data.get(index).set(marker.getX(),marker.getY());
      xRange.add(marker.getX());
      yRange.add(marker.getY());
      dataLabels.set(index,marker.getLabel());
      dataStyles.set(index,marker.getStyle());
    }
    finally
    {
      unlock();
    }
    fireInvalidated();
    return this;
  }

    public DefaultDataSet remove(final int fromIndex, final int toIndex) {
        lock().setAutoNotifaction(false);
        AssertUtils.indexInBounds(fromIndex, getDataCount(), "fromIndex");
        AssertUtils.indexInBounds(toIndex, getDataCount(), "toIndex");
        AssertUtils.indexOrder(fromIndex, "fromIndex", toIndex, "toIndex");

        data.remove(fromIndex, toIndex);
        dataLabels.remove(fromIndex, toIndex);
        dataStyles.remove(fromIndex, toIndex);

        xRange.empty();
        yRange.empty();

        return setAutoNotifaction(true).unlock().fireInvalidated();
    }


    /**
     * Returns label of a data point specified by the index.
     * The label can be used as a category name if CategoryStepsDefinition is used or for annotations displayed for data
     * points.
     *
     * @param index of the data label
     * @return data point label specified by the index or <code>null</code> if no label has been specified
     */
    @Override
    public String getDataLabel(final int index) {
        final String dataLabel = dataLabels.get(index);
        if (dataLabel != null) {
            return dataLabel;
        }

        return super.getDataLabel(index);
    }

    /**
     * A string representation of the CSS style associated with this
     * specific {@code DataSet} data point. @see #getStyle()
     *
     * @param index the index of the specific data point
     * @return user-specific data set style description (ie. may be set by user)
     */
    @Override
    public String getStyle(final int index) {
        return dataStyles.get(index);
    }


}
