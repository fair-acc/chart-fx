/*****************************************************************************
 *                                                                           *
 * Common Chart - data set for labeled markers                               *
 *                                                                           *
 * modified: 2019-05-07 Harald Braeuning                                     *
 *                                                                           *
 ****************************************************************************/

package de.gsi.dataset.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.spi.utils.DoublePoint;
import de.gsi.dataset.utils.AssertUtils;

/**
 * Minor extension to <code>DefaultDataSet</code> to easier handle labelled
 * markers.
 * 
 * @author braeun
 */
public class LabelledMarkerDataSet extends AbstractDataSet<LabelledMarkerDataSet> implements DataSet {

    protected ArrayList<String> dataLabels = new ArrayList<>();
    protected ArrayList<String> dataStyles = new ArrayList<>();
    protected ArrayList<DoublePoint> data = new ArrayList<>();

    /**
     * @param name
     *            data set name
     */
    public LabelledMarkerDataSet(String name) {
        super(name);
    }

    /**
     * 
     * @return data point labels
     */
    public List<String> getDataLabels() {
        return dataLabels;
    }

    /**
     * 
     * @return data point styles
     */
    public List<String> getDataStyles() {
        return dataStyles;
    }

    /**
     * 
     * @return list containing data point values
     */
    public List<DoublePoint> getData() {
        return data;
    }

    @Override
    public int getDataCount() {
        return data.size();
    }

    /**
     * 
     * @return remove all data points
     */
    public LabelledMarkerDataSet clearData() {
        lock().setAutoNotifaction(false);

        data.clear();
        dataLabels.clear();
        dataStyles.clear();

        xRange.empty();
        yRange.empty();

        return setAutoNotifaction(true).unlock().fireInvalidated(new RemovedDataEvent(this, "clear"));
    }

    @Override
    public double getX(final int index) {
        return data.get(index).getX();
    }

    @Override
    public double getY(final int index) {
        return data.get(index).getY();
    }

    /**
     * 
     * @param marker
     *            new marker
     * @return itself (fluent design)
     */
    public LabelledMarkerDataSet add(LabelledMarker marker) {
        AssertUtils.notNull("marker", marker);
        lock();
        try {
            data.add(new DoublePoint(marker.getX(), marker.getY()));
            xRange.add(marker.getX());
            // yRange.add(marker.getY());
            dataLabels.add(marker.getLabel());
            dataStyles.add(marker.getStyle());
        } finally {
            unlock();
        }
        fireInvalidated(new AddedDataEvent(this));
        return this;
    }

    /**
     * replaces existing with new marker values
     * @param markers new marker values
     * @return itself (fluent design)
     */
    public LabelledMarkerDataSet set(List<LabelledMarker> markers) {
        AssertUtils.notNull("markers", markers);
        lock();
        try {
            setAutoNotifaction(false);
            data.clear();
            dataLabels.clear();
            dataStyles.clear();
            xRange.empty();
            yRange.empty();
            for (LabelledMarker marker : markers) {
                final double x = marker.getX();
                final double y = marker.getY();
                data.add(new DoublePoint(x, y));
                xRange.add(x);
                // yRange.add(y);
                dataLabels.add(marker.getLabel());
                dataStyles.add(marker.getStyle());
            }
            setAutoNotifaction(true);
        } finally {
            unlock();
        }
        fireInvalidated(new UpdatedDataEvent(this, "fill"));
        return this;
    }

    /**
     * replaces existing with new marker values
     * @param markers new marker values
     * @return itself (fluent design)
     */
    public LabelledMarkerDataSet set(LabelledMarker[] markers) {
        AssertUtils.notNull("markers", markers);
        return set(Arrays.asList(markers));
    }

    /**
     * replaces existing data marker with new marker value
     * @param index index of existing point
     * @param marker new marker value
     * @return itself (fluent design)
     */
    public LabelledMarkerDataSet set(int index, LabelledMarker marker) {
        AssertUtils.indexInBounds(index, getDataCount());
        lock();
        try {
            data.get(index).set(marker.getX(), marker.getY());
            xRange.add(marker.getX());
            // yRange.add(marker.getY());
            dataLabels.set(index, marker.getLabel());
            dataStyles.set(index, marker.getStyle());
        } finally {
            unlock();
        }
        fireInvalidated(new UpdatedDataEvent(this));
        return this;
    }

    /**
     * remove sub-range of data (marker) points
     * @param fromIndex start index
     * @param toIndex stop index
     * @return itself (fluent design)
     */
    public LabelledMarkerDataSet remove(final int fromIndex, final int toIndex) {
        lock().setAutoNotifaction(false);
        AssertUtils.indexInBounds(fromIndex, getDataCount(), "fromIndex");
        AssertUtils.indexInBounds(toIndex, getDataCount(), "toIndex");
        AssertUtils.indexOrder(fromIndex, "fromIndex", toIndex, "toIndex");

        data.subList(fromIndex, toIndex).clear();
        dataLabels.subList(fromIndex, toIndex).clear();
        dataStyles.subList(fromIndex, toIndex).clear();

        xRange.empty();
        yRange.empty();

        return setAutoNotifaction(true).unlock().fireInvalidated(new RemovedDataEvent(this));
    }

    /**
     * Returns label of a data point specified by the index. The label can be
     * used as a category name if CategoryStepsDefinition is used or for
     * annotations displayed for data points.
     *
     * @param index
     *            of the data label
     * @return data point label specified by the index or <code>null</code> if
     *         no label has been specified
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
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param index
     *            the index of the specific data point
     * @return user-specific data set style description (ie. may be set by user)
     */
    @Override
    public String getStyle(final int index) {
        return dataStyles.get(index);
    }

    /**
     * Computes limits (ranges) of this DataSet.
     * 
     * @return itself (fluent design)
     */
    @Override
    protected LabelledMarkerDataSet computeLimits() {
        lock();
        // Clear previous ranges
        xRange.empty();
        yRange.empty();

        final int dataCount = getDataCount();

        for (int i = 0; i < dataCount; i++) {
            xRange.add(getX(i));
            // yRange.add(getY(i));
        }

        return unlock();
    }

}
