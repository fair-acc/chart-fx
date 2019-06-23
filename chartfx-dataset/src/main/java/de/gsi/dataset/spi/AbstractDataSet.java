package de.gsi.dataset.spi;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.EditConstraints;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.event.UpdatedMetaDataEvent;

/**
 * <p>
 * The abstract implementation of DataSet interface that provides implementation
 * of some methods.
 * </p>
 * <ul>
 * <li>It maintains the name of the DataSet
 * <li>It maintains a list of DataSetListener objects and provides methods that
 * can be used to dispatch DataSetEvent events.
 * <li>It maintains ranges of X and Y values.
 * <li>It gives a possibility to specify an undefined value.
 * </ul>
 * 
 * @param <D> java generics handling of DataSet for derived classes (needed for
 *            fluent design)
 */
public abstract class AbstractDataSet<D extends AbstractStylable<D>> extends AbstractStylable<D>
        implements DataSet, DataSetMetaData {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDataSet.class);
    protected String name;
    protected final List<EventListener> updateListeners = new LinkedList<>();
    protected final ReentrantLock lock = new ReentrantLock();
    boolean autoNotification = true;
    protected DataRange xRange = new DataRange();
    protected DataRange yRange = new DataRange();
    protected Map<Integer, String> dataLabels = new ConcurrentHashMap<>();
    protected Map<Integer, String> dataStyles = new ConcurrentHashMap<>();
    protected EditConstraints editConstraints;
    protected final Map<String, String> metaInfoMap = new ConcurrentHashMap<>();

    /**
     * default constructor
     * 
     * @param name the default name of the data set (meta data)
     */
    public AbstractDataSet(final String name) {
        super();
        this.name = name;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected D getThis() {
        return (D) this;
    }

    /**
     * Sets the name of data set (meta data)
     * 
     * @param name the new name
     * @return itself (fluent design)
     */
    public D setName(final String name) {
        this.name = name;
        return getThis();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public D lock() {
        lock.lock();
        return getThis();
    }

    @Override
    public D unlock() {
        lock.unlock();
        return getThis();
    }

    public List<EventListener> updateEventListener() {
        return updateListeners;
    }

    @Override
    public D setAutoNotifaction(final boolean flag) {
        autoNotification = flag;
        return getThis();
    }

    @Override
    public boolean isAutoNotification() {
        return autoNotification;
    }

    @Override
    public Double getUndefValue() {
        return Double.NaN;
    }

    /**
     * Gets the minimum x value of the data set.
     *
     * @return minimum x value
     */
    @Override
    public double getXMin() {
        if (!getXRange().isDefined()) {
            computeLimits();
        }
        return getXRange().getMin();
    }

    /**
     * Gets the maximum x value of the data set.
     *
     * @return maximum x value
     */
    @Override
    public double getXMax() {
        if (!getXRange().isDefined()) {
            computeLimits();
        }
        return getXRange().getMax();
    }

    /**
     * Gets the minimum y value of the data set.
     *
     * @return minimum y value
     */
    @Override
    public double getYMin() {
        if (!getYRange().isDefined()) {
            computeLimits();
        }
        return getYRange().getMin();
    }

    /**
     * Gets the maximum y value of the data set.
     *
     * @return maximum y value
     */
    @Override
    public double getYMax() {
        if (!getYRange().isDefined()) {
            computeLimits();
        }
        return getYRange().getMax();
    }

    /**
     * 
     * @return horizontal range of data set
     */
    public DataRange getXRange() {
        if (!xRange.isDefined()) {
            computeLimits();
        }
        return xRange;
    }

    /**
     * 
     * @return vertical range of data set
     */
    public DataRange getYRange() {
        if (!yRange.isDefined()) {
            computeLimits();
        }
        return yRange;
    }

    /**
     * Computes limits (ranges) of this DataSet.
     * 
     * @return itself (fluent design)
     */
    protected D computeLimits() {
        lock();
        // Clear previous ranges
        xRange.empty();
        yRange.empty();

        final int dataCount = getDataCount();

        for (int i = 0; i < dataCount; i++) {
            xRange.add(getX(i));
            yRange.add(getY(i));
        }

        return unlock();
    }

    /**
     * Gets the number of data points in the range xmin to xmax.
     *
     * @param xmin the lower end of the range
     * @param xmax the upper end of the range
     * @return the number of data points
     */
    @Override
    public int getDataCount(final double xmin, final double xmax) {
        lock();
        int count = 0;
        for (int i = 0; i < getDataCount(); i++) {
            final double x = getX(i);
            if (x >= xmin && x <= xmax) {
                count++;
            }
        }
        unlock();
        return count;
    }

    /**
     * Gets the index of the data point closest to the given x coordinate. The
     * index returned may be less then zero or larger the the number of data
     * points in the data set, if the x coordinate lies outside the range of the
     * data set.
     *
     * @param x the x position of the data point
     * @return the index of the data point
     */
    @Override
    public int getXIndex(final double x) {
        if (this.getDataCount() == 0) {
            return 0;
        }

        if (!Double.isFinite(x)) {
            return 0;
        }

        if (x < this.getXMin()) {
            return 0;
        }

        final int lastIndex = getDataCount() - 1;
        if (x > this.getXMax()) {
            return lastIndex;
        }

        // binary closest search -- assumes sorted data set
        // TODO: should implement either sorting and/or flag that data set is
        // sorted
        return binarySearchX(x, 0, lastIndex);
        // alt implementation (less performant than binary search)
        // System.err.println("AAA - range check: x = " + x + " vs. diff to min
        // = " + Math.abs(x - getXMin()) + " max =
        // "
        // + Math.abs(x - getXMax()));
        // System.err.println("AAA - " + getName() + " - search A = " +
        // minNeigbourSearchX(x, 0, lastIndex)
        // + " vs. search B = " + binarySearchX(x, 0, lastIndex) + " max= " +
        // this.getDataCount());
        // return minNeigbourSearchX(x, 0, lastIndex);
    }

    /**
     * Gets the first index of the data point closest to the given y coordinate.
     *
     * @param y the y position of the data point
     * @return the index of the data point
     */
    @Override
    public int getYIndex(final double y) {
        if (this.getDataCount() == 0) {
            return 0;
        }
        final boolean startedAbove = y < getY(0);
        for (int i = 0; i < getDataCount(); i++) {
            final double val = getY(i);
            if (Double.isFinite(val)) {
                if (startedAbove) {
                    if (val <= y) {
                        return i;
                    }
                } else // started below
                if (val >= y) {
                    return i;
                }
            }
        }
        return getDataCount() - 1;
    }

    protected int binarySearchX(final double search, final int indexMin, final int indexMax) {
        if (indexMin == indexMax) {
            return indexMin;
        }
        if (indexMax - indexMin == 1) {
            if (Math.abs(getX(indexMin) - search) < Math.abs(getX(indexMax) - search)) {
                return indexMin;
            }
            return indexMax;
        }
        final int middle = (indexMax + indexMin) / 2;
        final double valMiddle = getX(middle);
        if (valMiddle == search) {
            return middle;
        }
        if (search < valMiddle) {
            return binarySearchX(search, indexMin, middle);
        }
        return binarySearchX(search, middle, indexMax);
    }

    protected int binarySearchY(final double search, final int indexMin, final int indexMax) {
        if (indexMin == indexMax) {
            return indexMin;
        }
        if (indexMax - indexMin == 1) {
            if (Math.abs(getY(indexMin) - search) < Math.abs(getY(indexMax) - search)) {
                return indexMin;
            }
            return indexMax;
        }
        final int middle = (indexMax + indexMin) / 2;
        final double valMiddle = getY(middle);
        if (valMiddle == search) {
            return middle;
        }
        if (search < valMiddle) {
            return binarySearchY(search, indexMin, middle);
        }
        return binarySearchY(search, middle, indexMax);
    }

    protected int minNeigbourSearchX(final double search, final int indexMin, final int indexMax) {
        double minAbsDiff = Double.MAX_VALUE;
        int searchIndex = indexMin;

        final double a = getX(indexMin);
        final double b = getX(indexMax);
        final String eq = a < b ? " < " : " > ";
        LOGGER.error("- new searchIndex  getX(indexMin)= " + a + eq + " getX(indexMax)= " + b);

        for (int i = indexMin; i <= indexMax; i++) {
            final double valX = getX(i);
            if (!Double.isFinite(valX)) {
                LOGGER.error("non-finite value - autsch = " + valX + " index = " + i);
                throw new IllegalStateException("check");
                // continue;
            }

            final double absDiff = Math.abs(search - valX);

            if (Double.isFinite(absDiff) && absDiff < minAbsDiff) {
                searchIndex = i;
                minAbsDiff = absDiff;
            }
        }
        LOGGER.error("- new searchIndex Range = " + indexMin + " for " + indexMax);
        LOGGER.error("- new searchIndex = " + searchIndex + " for " + minAbsDiff);

        return searchIndex;
    }

    /**
     * Notifies listeners that the data has been invalidated. If the data is
     * added to the chart, it triggers repaint.
     * 
     * @param event the change event
     *
     * @return itself (fluent design)
     */
    public D fireInvalidated(final UpdateEvent event) {
        if (!autoNotification || updateEventListener().isEmpty()) {
            return getThis();
        }

        if (!xRange.isDefined() || !yRange.isDefined()) {
            computeLimits();
        }

        invokeListener(event);

        return getThis();
    }

    @Override
    public String toString() {
        return getClass().getName() + " [dataCnt=" + getDataCount() + ", xRange=" + getXRange() + ", yRange="
                + getYRange() + "]";
    }

    /**
     * adds a custom new data label for a point The label can be used as a
     * category name if CategoryStepsDefinition is used or for annotations
     * displayed for data points.
     *
     * @param index of the data point
     * @param label for the data point specified by the index
     * @return the previously set label or <code>null</code> if no label has
     *         been specified
     */
    public String addDataLabel(final int index, final String label) {
        lock();
        final String retVal = dataLabels.put(index, label);
        unlock();
        fireInvalidated(new UpdatedMetaDataEvent(this, "added label"));
        return retVal;
    }

    /**
     * remove a custom data label for a point The label can be used as a
     * category name if CategoryStepsDefinition is used or for annotations
     * displayed for data points.
     *
     * @param index of the data point
     * @return the previously set label or <code>null</code> if no label has
     *         been specified
     */
    public String removeDataLabel(final int index) {
        lock();
        final String retVal = dataLabels.remove(index);
        unlock();
        fireInvalidated(new UpdatedMetaDataEvent(this, "removed label"));
        return retVal;
    }

    /**
     * Returns label of a data point specified by the index. The label can be
     * used as a category name if CategoryStepsDefinition is used or for
     * annotations displayed for data points.
     *
     * @param index data point index
     * @return label of a data point specified by the index or <code>null</code>
     *         if none label has been specified for this data point.
     */
    protected String getDefaultDataLabel(final int index) {
        return getName() + "(" + index + "," + getX(index) + "," + getY(index) + ")";
    }

    /**
     * Returns label of a data point specified by the index. The label can be
     * used as a category name if CategoryStepsDefinition is used or for
     * annotations displayed for data points.
     *
     * @param index of the data label
     * @return data point label specified by the index or <code>null</code> if
     *         no label has been specified
     */
    @Override
    public String getDataLabel(final int index) {
        final String dataLabel = dataLabels.get(index);
        if (dataLabel != null) {
            return dataLabel;
        }

        return getDefaultDataLabel(index);
    }

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param index the index of the specific data point
     * @param style string for the data point specific CSS-styling
     * @return itself (fluent interface)
     */
    public String addDataStyle(final int index, final String style) {
        lock();
        final String retVal = dataStyles.put(index, style);
        unlock();
        fireInvalidated(new UpdatedMetaDataEvent(this, "added style"));
        return retVal;
    }

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param index the index of the specific data point
     * @return itself (fluent interface)
     */
    public String removeStyle(final int index) {
        lock();
        final String retVal = dataStyles.remove(index);
        lock();
        fireInvalidated(new UpdatedMetaDataEvent(this, "removed style"));
        return retVal;
    }

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param index the index of the specific data point
     * @return user-specific data set style description (ie. may be set by user)
     */
    public String getStyle(final int index) {
        return dataStyles.get(index);
    }

    public EditConstraints getEditConstraints() {
        return editConstraints;
    }

    public D setEditConstraints(final EditConstraints constraints) {
        lock();
        editConstraints = constraints;
        unlock();
        return fireInvalidated(new UpdatedMetaDataEvent(this, "new edit constraints"));
    }

    @Override
    public Map<String, String> getMetaInfo() {
        return metaInfoMap;
    }

    @Override
    public List<String> getInfoList() {
        return Collections.<String>emptyList();
    }

    @Override
    public List<String> getWarningList() {
        return Collections.<String>emptyList();
    }

    @Override
    public List<String> getErrorList() {
        return Collections.<String>emptyList();
    }
}
