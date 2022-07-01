/*****************************************************************************
 * *
 * Common Chart - data set for labeled markers *
 * *
 * modified: 2019-05-07 Harald Braeuning *
 * *
 ****************************************************************************/

package io.fair_acc.dataset.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.event.AddedDataEvent;
import io.fair_acc.dataset.event.RemovedDataEvent;
import io.fair_acc.dataset.event.UpdatedDataEvent;
import io.fair_acc.dataset.spi.utils.DoublePoint;
import io.fair_acc.dataset.utils.AssertUtils;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSet2D;

/**
 * Minor extension to <code>DefaultDataSet</code> to easier handle labeled markers.
 * 
 * @author braeun
 */
public class LabelledMarkerDataSet extends AbstractDataSet<LabelledMarkerDataSet> implements DataSet2D {
    private static final long serialVersionUID = -3267447868117053651L;
    protected final ArrayList<String> dataLabels = new ArrayList<>();
    protected final ArrayList<String> dataStyles = new ArrayList<>();
    protected final transient ArrayList<DoublePoint> data = new ArrayList<>();

    /**
     * @param name data set name
     */
    public LabelledMarkerDataSet(String name) {
        super(name, 2);
    }

    /**
     * @param marker new marker
     * @return itself (fluent design)
     */
    public LabelledMarkerDataSet add(LabelledMarker marker) {
        AssertUtils.notNull("marker", marker);
        lock().writeLockGuard(() -> {
            data.add(new DoublePoint(marker.getX(), marker.getY()));
            getAxisDescription(DIM_X).add(marker.getX());
            dataLabels.add(marker.getLabel());
            dataStyles.add(marker.getStyle());
        });
        fireInvalidated(new AddedDataEvent(this));
        return this;
    }

    /**
     * @return remove all data points
     */
    public LabelledMarkerDataSet clearData() {
        lock().writeLockGuard(() -> {
            data.clear();
            dataLabels.clear();
            dataStyles.clear();

            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        return fireInvalidated(new RemovedDataEvent(this, "clear"));
    }

    @Override
    public double get(final int dimIndex, final int index) {
        return dimIndex == DIM_X ? data.get(index).getX() : data.get(index).getY();
    }

    /**
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
     * Returns label of a data point specified by the index. The label can be used as a category name if
     * CategoryStepsDefinition is used or for annotations displayed for data points.
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
     * @return data point labels
     */
    public List<String> getDataLabels() {
        return dataLabels;
    }

    /**
     * @return data point styles
     */
    public List<String> getDataStyles() {
        return dataStyles;
    }

    /**
     * A string representation of the CSS style associated with this specific {@code DataSet} data point. @see
     * #getStyle()
     *
     * @param index the index of the specific data point
     * @return user-specific data set style description (ie. may be set by user)
     */
    @Override
    public String getStyle(final int index) {
        return dataStyles.get(index);
    }

    /**
     * remove sub-range of data (marker) points
     * 
     * @param fromIndex start index
     * @param toIndex stop index
     * @return itself (fluent design)
     */
    public LabelledMarkerDataSet remove(final int fromIndex, final int toIndex) {
        lock().writeLockGuard(() -> {
            AssertUtils.indexInBounds(fromIndex, getDataCount(), "fromIndex");
            AssertUtils.indexInBounds(toIndex, getDataCount(), "toIndex");
            AssertUtils.indexOrder(fromIndex, "fromIndex", toIndex, "toIndex");

            data.subList(fromIndex, toIndex).clear();
            dataLabels.subList(fromIndex, toIndex).clear();
            dataStyles.subList(fromIndex, toIndex).clear();

            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        return fireInvalidated(new RemovedDataEvent(this));
    }

    /**
     * replaces existing data marker with new marker value
     * 
     * @param index index of existing point
     * @param marker new marker value
     * @return itself (fluent design)
     */
    public LabelledMarkerDataSet set(int index, LabelledMarker marker) {
        AssertUtils.indexInBounds(index, getDataCount());
        lock().writeLockGuard(() -> {
            data.get(index).set(marker.getX(), marker.getY());
            getAxisDescription(DIM_X).add(marker.getX());
            dataLabels.set(index, marker.getLabel());
            dataStyles.set(index, marker.getStyle());
        });
        fireInvalidated(new UpdatedDataEvent(this));
        return this;
    }

    /**
     * replaces existing with new marker values
     * 
     * @param markers new marker values
     * @return itself (fluent design)
     */
    public LabelledMarkerDataSet set(LabelledMarker[] markers) {
        AssertUtils.notNull("markers", markers);
        return set(Arrays.asList(markers));
    }

    /**
     * replaces existing with new marker values
     * 
     * @param markers new marker values
     * @return itself (fluent design)
     */
    public LabelledMarkerDataSet set(List<LabelledMarker> markers) {
        AssertUtils.notNull("markers", markers);
        lock().writeLockGuard(() -> {
            data.clear();
            dataLabels.clear();
            dataStyles.clear();
            getAxisDescriptions().forEach(AxisDescription::clear);
            for (LabelledMarker marker : markers) {
                final double x = marker.getX();
                final double y = marker.getY();
                data.add(new DoublePoint(x, y));
                getAxisDescription(DIM_X).add(x);
                dataLabels.add(marker.getLabel());
                dataStyles.add(marker.getStyle());
            }
        });
        fireInvalidated(new UpdatedDataEvent(this, "fill"));
        return this;
    }

    @Override
    public DataSet set(final DataSet other, final boolean copy) {
        throw new UnsupportedOperationException("copy setting transposed data set is not implemented");
    }
}
