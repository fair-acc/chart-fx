package de.gsi.dataset.spi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.EditConstraints;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.event.UpdatedMetaDataEvent;
import de.gsi.dataset.locks.DataSetLock;
import de.gsi.dataset.locks.DefaultDataSetLock;
import de.gsi.dataset.spi.utils.StringHashMapList;

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
 * <li>It maintains the names and units for the axes
 * </ul>
 * 
 * @param <D> java generics handling of DataSet for derived classes (needed for
 *            fluent design)
 */
public abstract class AbstractDataSet<D extends AbstractStylable<D>> extends AbstractStylable<D>
        implements DataSet, DataSetMetaData {
    private static final long serialVersionUID = -7612136495756923417L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDataSet.class);
    private transient AtomicBoolean autoNotification = new AtomicBoolean(true);
    private static final String[] DEFAULT_AXES_NAME = { "x-Axis", "y-Axis", "z-Axis" };
    private String name;
    private final int dimension;
    private final List<AxisDescription> axesDescriptions = new ArrayList<>();
    private final List<EventListener> updateListeners = new LinkedList<>();
    private final transient DataSetLock<? extends DataSet> lock = new DefaultDataSetLock<>(this);
    private StringHashMapList dataLabels = new StringHashMapList();
    private StringHashMapList dataStyles = new StringHashMapList();
    private List<String> infoList = new ArrayList<>();
    private List<String> warningList = new ArrayList<>();
    private List<String> errorList = new ArrayList<>();
    private EditConstraints editConstraints;
    private final Map<String, String> metaInfoMap = new ConcurrentHashMap<>();

    /**
     * default constructor
     * 
     * @param name the default name of the data set (meta data)
     * @param dimension dimension of this data set
     */
    public AbstractDataSet(final String name, final int dimension) {
        super();
        this.name = name;
        this.dimension = dimension;
        for (int i = 0; i < this.dimension; i++) {
            final String axisName = i < DEFAULT_AXES_NAME.length ? DEFAULT_AXES_NAME[i] : "dim" + (i + 1) + "-Axis";
            axesDescriptions.add(new DefaultAxisDescription(this, axisName, "a.u."));
        }
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
        final String retVal = lock().writeLockGuard(() -> dataLabels.put(index, label));
        fireInvalidated(new UpdatedMetaDataEvent(this, "added label"));
        return retVal;
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
        final String retVal = lock().writeLockGuard(() -> dataStyles.put(index, style));
        fireInvalidated(new UpdatedMetaDataEvent(this, "added style"));
        return retVal;
    }

    @Override
    public AtomicBoolean autoNotification() {
        return autoNotification;
    }

    public D clearMetaInfo() {
        infoList.clear();
        warningList.clear();
        errorList.clear();
        return fireInvalidated(new UpdatedMetaDataEvent(this, "cleared meta data"));
    }

    /**
     * Notifies listeners that the data has been invalidated. If the data is
     * added to the chart, it triggers repaint.
     * 
     * @param event the change event
     * @return itself (fluent design)
     */
    public D fireInvalidated(final UpdateEvent event) {
        if (!isAutoNotification() || updateEventListener().isEmpty()) {
            return getThis();
        }

        invokeListener(event);

        return getThis();
    }

    /**
     * @return axis descriptions of the primary and secondary axes
     */
    @Override
    public List<AxisDescription> getAxisDescriptions() {
        return axesDescriptions;
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
        // old implementation: caused issue/unnecessary copying of data
        // moved to DataPointToolTip (better place)
        // final String dataLabel = dataLabels.get(index);
        // if (dataLabel != null) {
        // return dataLabel;
        // }
        //
        // return getDefaultDataLabel(index);
        return dataLabels.get(index);
    }

    /**
     * @return data label map for given data point
     */
    public StringHashMapList getDataLabelMap() {
        return dataLabels;
    }

    /**
     * @return data style map (CSS-styling)
     */
    public StringHashMapList getDataStyleMap() {
        return dataStyles;
    }

    @Override
    public final int getDimension() {
        return dimension;
    }

    public EditConstraints getEditConstraints() {
        return editConstraints;
    }

    @Override
    public List<String> getErrorList() {
        return errorList;
    }

    @Override
    public List<String> getInfoList() {
        return infoList;
    }

    @Override
    public Map<String, String> getMetaInfo() {
        return metaInfoMap;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param index the index of the specific data point
     * @return user-specific data set style description (ie. may be set by user)
     */
    @Override
    public String getStyle(final int index) {
        return dataStyles.get(index);
    }

    @Override
    public List<String> getWarningList() {
        return warningList;
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
    public int getXIndex(final double x) {
        if (this.getDataCount() == 0) {
            return 0;
        }

        if (!Double.isFinite(x)) {
            return 0;
        }

        if (x < this.getAxisDescription(0).getMin()) {
            return 0;
        }

        final int lastIndex = getDataCount() - 1;
        if (x > this.getAxisDescription(0).getMax()) {
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
    public int getYIndex(final double y) {
        if (this.getDataCount(DIM_Y) == 0) {
            return 0;
        }
        final boolean startedAbove = y < get(DIM_Y, 0);
        for (int i = 0; i < getDataCount(DIM_Y); i++) {
            final double val = get(DIM_Y, i);
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
        return getDataCount(DIM_Y) - 1;
    }

    @Override
    public DataSetLock<? extends DataSet> lock() {
        return lock;
    }

    /**
     * Computes limits (ranges) of this DataSet.
     * 
     * @param dimension the chosen dimension
     * @return itself (fluent design)
     */
    @Override
    public D recomputeLimits(final int dimension) {
        lock().writeLockGuard(() -> {
            // Clear previous ranges
            getAxisDescription(dimension).clear();
            final int dataCount = getDataCount(dimension);
            if (dimension == 0) {
                for (int i = 0; i < dataCount; i++) {
                    getAxisDescription(dimension).add(get(DIM_X, i));

                }
            } else {
                for (int i = 0; i < dataCount; i++) {
                    getAxisDescription(dimension).add(get(dimension, i));
                }
            }
        });
        return getThis();
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
        final String retVal = lock().writeLockGuard(() -> dataLabels.remove(index));
        fireInvalidated(new UpdatedMetaDataEvent(this, "removed label"));
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
        final String retVal = lock().writeLockGuard(() -> dataStyles.remove(index));
        fireInvalidated(new UpdatedMetaDataEvent(this, "removed style"));
        return retVal;
    }

    public D setEditConstraints(final EditConstraints constraints) {
        lock().writeLockGuard(() -> editConstraints = constraints);
        return fireInvalidated(new UpdatedMetaDataEvent(this, "new edit constraints"));
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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName()).append(" [dim=").append(getDimension()).append(',');
        for (int i = 0; i < this.getDimension(); i++) {
            final AxisDescription desc = getAxisDescription(i);
            final boolean isDefined = desc.isDefined();
            builder.append(" dataCount(").append(i).append(")=").append(this.getDataCount(i)).append(',')//
                    .append(" axisName ='").append(desc.getName()).append("',")//
                    .append(" axisUnit = '").append(desc.getUnit()).append("',") //
                    .append(" axisRange = ")//
                    .append(" [min=").append(isDefined ? desc.getMin() : "NotDefined") //
                    .append(", max=").append(isDefined ? desc.getMax() : "NotDefined")//
                    .append("],");
        }
        builder.append(']');
        return builder.toString();
    }

    @Override
    public List<EventListener> updateEventListener() {
        return updateListeners;
    }

    protected int binarySearchX(final double search, final int indexMin, final int indexMax) {
        if (indexMin == indexMax) {
            return indexMin;
        }
        if (indexMax - indexMin == 1) {
            if (Math.abs(get(DIM_X, indexMin) - search) < Math.abs(get(DIM_X, indexMax) - search)) {
                return indexMin;
            }
            return indexMax;
        }
        final int middle = (indexMax + indexMin) / 2;
        final double valMiddle = get(DIM_X, middle);
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
            if (Math.abs(get(DIM_Y, indexMin) - search) < Math.abs(get(DIM_Y, indexMax) - search)) {
                return indexMin;
            }
            return indexMax;
        }
        final int middle = (indexMax + indexMin) / 2;
        final double valMiddle = get(DIM_Y, middle);
        if (valMiddle == search) {
            return middle;
        }
        if (search < valMiddle) {
            return binarySearchY(search, indexMin, middle);
        }
        return binarySearchY(search, middle, indexMax);
    }

    @Override
    protected D getThis() {
        return (D) this;
    }

    protected int minNeigbourSearchX(final double search, final int indexMin, final int indexMax) {
        double minAbsDiff = Double.MAX_VALUE;
        int searchIndex = indexMin;

        final double a = get(DIM_X, indexMin);
        final double b = get(DIM_X, indexMax);
        final String eq = a < b ? " < " : " > ";
        LOGGER.error("- new searchIndex  getX(indexMin)= " + a + eq + " getX(indexMax)= " + b);

        for (int i = indexMin; i <= indexMax; i++) {
            final double valX = get(DIM_X, i);
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
}
