package de.gsi.chart.data.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import de.gsi.chart.data.DataSet;
import de.gsi.chart.data.DataSetMetaData;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.scene.transform.Scale;

/**
 * <p>
 * The abstract implementation of DataSet interface that provides implementation of some methods.
 * </p>
 * <p>
 * <ul>
 * <li>It maintains the name of the DataSet
 * <li>It maintains a list of DataSetListener objects and provides methods that can be used to dispatch DataSetEvent
 * events.
 * <li>It maintains ranges of X and Y values.
 * <li>It gives a possibility to specify an undefined value.
 * </ul>
 * </p>
 */
public abstract class AbstractDataSet<D extends AbstractStylable<D>> extends AbstractStylable<D>
        implements DataSet, DataSetMetaData {

    protected String name;
    protected final List<InvalidationListener> listeners = new LinkedList<>();
    protected final ReentrantLock lock = new ReentrantLock();
    boolean autoNotification = true;
    protected DataRange xRange = new DataRange();
    protected DataRange yRange = new DataRange();
    private final Map<String, String> metaInfoMap = new ConcurrentHashMap<>();

    public AbstractDataSet(final String name) {
        super();
        this.name = name;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected D getThis() {
        return (D) this;
    }

    public void setName(final String name) {
        this.name = name;
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

    @Override
    public void addListener(final InvalidationListener listener) {
        Objects.requireNonNull(listener, "InvalidationListener must not be null");
        // N.B. suppress duplicates
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(final InvalidationListener listener) {
        listeners.remove(listener);
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

    public DataRange getXRange() {
        if (!xRange.isDefined()) {
            computeLimits();
        }
        return xRange;
    }

    public DataRange getYRange() {
        if (!xRange.isDefined()) {
            computeLimits();
        }
        return yRange;
    }

    /**
     * Computes limits (ranges) of this DataSet.
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
     * Gets the index of the data point closest to the given x coordinate. The index returned may be less then zero or
     * larger the the number of data points in the data set, if the x coordinate lies outside the range of the data set.
     *
     * @param x the x position of the data point
     * @return the index of the data point
     */
    @Override
    public int getXIndex(final double x) {
        if (this.getDataCount() == 0) {
            return 0;
        }
        final int lastIndex = getDataCount() - 1;

        if (!Double.isFinite(x)) {
            // System.err.println("value is not finite = " + x);
            // System.exit(0);
            return 0;
        }

        if (x < this.getXMin()) {
            return 0;
        }

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
        System.err.println("- new searchIndex  getX(indexMin)= " + a + eq + " getX(indexMax)= " + b);

        for (int i = indexMin; i <= indexMax; i++) {
            final double valX = getX(i);
            if (!Double.isFinite(valX)) {
                System.err.println("non-finite value - autsch = " + valX + " index = " + i);
                throw new RuntimeException("check");
                // continue;
            }

            final double absDiff = Math.abs(search - valX);

            if (Double.isFinite(absDiff) && absDiff < minAbsDiff) {
                searchIndex = i;
                minAbsDiff = absDiff;
            }
        }
        System.err.println("- new searchIndex Range = " + indexMin + " for " + indexMax);
        System.err.println("- new searchIndex = " + searchIndex + " for " + minAbsDiff);

        return searchIndex;
    }

    /**
     * Notifies listeners that the data has been invalidated. If the data is added to the chart, it triggers repaint.
     *
     * @return itself (fluent design)
     */
    public D fireInvalidated() {
        if (!autoNotification || listeners.isEmpty()) {
            return getThis();
        }

        if (!xRange.isDefined() || !yRange.isDefined()) {
            computeLimits();
        }

        if (Platform.isFxApplicationThread()) {
            executeFireInvalidated();
        } else {
            Platform.runLater(this::executeFireInvalidated);
        }
        return getThis();
    }

    protected D executeFireInvalidated() {
        for (final InvalidationListener listener : new ArrayList<>(listeners)) {
            listener.invalidated(this);
        }
        return getThis();
    }

    /**
     * Returns label of a data point specified by the index. The label can be used as a category name if
     * CategoryStepsDefinition is used or for annotations displayed for data points.
     *
     * @param index
     * @return label of a data point specified by the index or <code>null</code> if none label has been specified for
     *         this data point.
     * @see CategoryStepsDefinition
     * @see #setCategory(boolean)
     * @see Scale#setCategory(DataSet)
     */
    @Override
    public String getDataLabel(final int index) {
        return getName() + "(" + index + "," + getX(index) + "," + getY(index) + ")";
    }

    @Override
    public String toString() {
        return getClass().getName() + " [dataCnt=" + getDataCount() + ", xRange=" + getXRange() + ", yRange="
                + getYRange() + "]";
    }

    @Override
    public Map<String, String> getMetaInfo() {
        return metaInfoMap;
    }

    @Override
    public List<String> getInfoList() {
        return Collections.<String> emptyList();
    }

    @Override
    public List<String> getWarningList() {
        return Collections.<String> emptyList();
    }

    @Override
    public List<String> getErrorList() {
        return Collections.<String> emptyList();
    }
}
