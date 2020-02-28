package de.gsi.dataset.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.EditConstraints;
import de.gsi.dataset.EditableDataSet;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.event.UpdatedMetaDataEvent;
import de.gsi.dataset.locks.DataSetLock;
import de.gsi.dataset.locks.DefaultDataSetLock;
import de.gsi.dataset.spi.utils.MathUtils;
import de.gsi.dataset.spi.utils.StringHashMapList;
import de.gsi.dataset.utils.AssertUtils;

/**
 * <p>
 * The abstract implementation of DataSet interface that provides implementation of some methods.
 * </p>
 * <ul>
 * <li>It maintains the name of the DataSet
 * <li>It maintains a list of DataSetListener objects and provides methods that can be used to dispatch DataSetEvent
 * events.
 * <li>It maintains ranges of X and Y values.
 * <li>It gives a possibility to specify an undefined value.
 * <li>It maintains the names and units for the axes
 * </ul>
 * 
 * @param <D> java generics handling of DataSet for derived classes (needed for fluent design)
 */
public abstract class AbstractDataSet<D extends AbstractStylable<D>> extends AbstractStylable<D>
        implements DataSet, DataSetMetaData {
    private static final long serialVersionUID = -7612136495756923417L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDataSet.class);

    private static final String[] DEFAULT_AXES_NAME = { "x-Axis", "y-Axis", "z-Axis" };
    private final transient AtomicBoolean autoNotification = new AtomicBoolean(true);
    private String name;
    private int dimension;
    private final List<AxisDescription> axesDescriptions = new ArrayList<>();
    private final transient List<EventListener> updateListeners = Collections.synchronizedList(new LinkedList<>());
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
        AssertUtils.gtThanZero("dimension", dimension);
        this.name = name;
        this.dimension = dimension;
        for (int i = 0; i < this.dimension; i++) {
            final String axisName = i < DEFAULT_AXES_NAME.length ? DEFAULT_AXES_NAME[i] : "dim" + (i + 1) + "-Axis";
            axesDescriptions.add(new DefaultAxisDescription(this, axisName, "a.u."));
        }
    }

    /**
     * adds a custom new data label for a point The label can be used as a category name if CategoryStepsDefinition is
     * used or for annotations displayed for data points.
     *
     * @param index of the data point
     * @param label for the data point specified by the index
     * @return the previously set label or <code>null</code> if no label has been specified
     */
    public String addDataLabel(final int index, final String label) {
        final String retVal = lock().writeLockGuard(() -> dataLabels.put(index, label));
        fireInvalidated(new UpdatedMetaDataEvent(this, "added label"));
        return retVal;
    }

    /**
     * A string representation of the CSS style associated with this specific {@code DataSet} data point. @see
     * #getStyle()
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

    protected int binarySearch(final int dimIndex, final double search, final int indexMin, final int indexMax) {
        if (indexMin == indexMax) {
            return indexMin;
        }
        if (indexMax - indexMin == 1) {
            if (Math.abs(get(dimIndex, indexMin) - search) < Math.abs(get(dimIndex, indexMax) - search)) {
                return indexMin;
            }
            return indexMax;
        }
        final int middle = (indexMax + indexMin) / 2;
        final double valMiddle = get(dimIndex, middle);
        if (valMiddle == search) {
            return middle;
        }
        if (search < valMiddle) {
            return binarySearch(dimIndex, search, indexMin, middle);
        }
        return binarySearch(dimIndex, search, middle, indexMax);
    }

    public D clearMetaInfo() {
        infoList.clear();
        warningList.clear();
        errorList.clear();
        return fireInvalidated(new UpdatedMetaDataEvent(this, "cleared meta data"));
    }

    /**
     * checks for equal data labels, may be overwritten by derived classes
     * 
     * @param other class
     * @return {@code true} if equal
     */
    protected boolean equalDataLabels(final DataSet other) {
        if (other instanceof AbstractDataSet) {
            AbstractDataSet<?> otherAbsDs = (AbstractDataSet<?>) other;
            if (!getDataLabelMap().equals(otherAbsDs.getDataLabelMap())) {
                return false;
            }
        } else {
            for (int index = 0; index < getDataCount(); index++) {
                final String label1 = this.getDataLabel(index);
                final String label2 = other.getDataLabel(index);
                if (label1 == label2) {
                    continue;
                }
                if (label1 == null && label2 != null) {
                    return false;
                }
                if (label1 == null || !label1.equals(label2)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * checks for equal EditConstraints, may be overwritten by derived classes
     * 
     * @param other class
     * @return {@code true} if equal
     */
    protected boolean equalEditConstraints(final DataSet other) {
        // check for error constraints
        if (other instanceof EditableDataSet) {
            EditableDataSet otherEditDs = (EditableDataSet) other;
            if (editConstraints != null && otherEditDs.getEditConstraints() == null) {
                return false;
            }

            if (editConstraints != null && !editConstraints.equals(otherEditDs.getEditConstraints())) {
                return false;
            }
        }
        return true;
    }

    /**
     * checks for equal 'get' error values, may be overwritten by derived classes
     * 
     * @param other class
     * @param epsilon tolerance threshold
     * @return {@code true} if equal
     */
    protected boolean equalErrorValues(final DataSet other, final double epsilon) {
        // check for error data values
        if (!(this instanceof DataSetError) || !(other instanceof DataSetError)) {
            return true;
        }
        DataSetError thisErrorDs = (DataSetError) this;
        DataSetError otherErrorDs = (DataSetError) other;
        if (!thisErrorDs.getErrorType(DIM_X).equals(otherErrorDs.getErrorType(DIM_X))) {
            return false;
        }
        if (!thisErrorDs.getErrorType(DIM_Y).equals(otherErrorDs.getErrorType(DIM_Y))) {
            return false;
        }

        if (epsilon <= 0.0) {
            for (int dimIndex = 0; dimIndex < this.getDimension(); dimIndex++) {
                for (int index = 0; index < this.getDataCount(dimIndex); index++) {
                    if (thisErrorDs.getErrorNegative(dimIndex, index) != otherErrorDs.getErrorNegative(dimIndex, index)) {
                        return false;
                    }
                    if (thisErrorDs.getErrorPositive(dimIndex, index) != otherErrorDs.getErrorPositive(dimIndex, index)) {
                        return false;
                    }
                }
            }
        } else {
            for (int dimIndex = 0; dimIndex < this.getDimension(); dimIndex++) {
                for (int index = 0; index < this.getDataCount(dimIndex); index++) {
                    if (!MathUtils.nearlyEqual(thisErrorDs.getErrorNegative(dimIndex, index),
                                otherErrorDs.getErrorNegative(dimIndex, index), epsilon)) {
                        return false;
                    }
                    if (!MathUtils.nearlyEqual(thisErrorDs.getErrorPositive(dimIndex, index),
                                otherErrorDs.getErrorPositive(dimIndex, index), epsilon)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * checks for equal meta data, may be overwritten by derived classes
     * 
     * @param other class
     * @return {@code true} if equal
     */
    protected boolean equalMetaData(final DataSet other) {
        if (other instanceof DataSetMetaData) {
            DataSetMetaData otherMetaDs = (DataSetMetaData) other;
            if (!getErrorList().equals(otherMetaDs.getErrorList())) {
                return false;
            }
            if (!getWarningList().equals(otherMetaDs.getWarningList())) {
                return false;
            }
            if (!getInfoList().equals(otherMetaDs.getInfoList())) {
                return false;
            }
            if (!getMetaInfo().equals(otherMetaDs.getMetaInfo())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(final Object obj) {
        return equals(obj, -1);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * 
     * @param obj the reference object with which to compare.
     * @param epsilon tolerance parameter ({@code epsilon<=0} corresponds to numerically identical)
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise.
     */
    public boolean equals(final Object obj, final double epsilon) { // NOPMD - by design
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DataSet)) {
            return false;
        }
        final DataSet other = (DataSet) obj;
        // TODO: check whether to add a thread-safety guard
        // N.B. some complication equals can be invoked from both reader as well as
        // writer threads

        // check dimension and data counts
        if (this.getDimension() != other.getDimension()) {
            return false;
        }
        if (this.getDataCount() != other.getDataCount()) {
            return false;
        }
        for (int dimIndex = 0; dimIndex < this.getDimension(); dimIndex++) {
            if (this.getDataCount(dimIndex) != other.getDataCount(dimIndex)) {
                return false;
            }
        }

        // check names
        final String name1 = this.getName();
        final String name2 = other.getName();

        if (!(name1 == null ? name2 == null : name1.equals(name2))) {
            return false;
        }

        // check axis description
        if (getAxisDescriptions().isEmpty() && !other.getAxisDescriptions().isEmpty()) {
            return false;
        }
        if (!getAxisDescriptions().equals(other.getAxisDescriptions())) {
            return false;
        }

        // check axis data labels
        if (!equalDataLabels(other)) {
            return false;
        }

        // check for error constraints
        if (!equalEditConstraints(other)) {
            return false;
        }

        // check meta data
        if (!equalMetaData(other)) {
            return false;
        }

        // check normal data values
        if (!equalValues(other, epsilon)) {
            return false;
        }

        return equalErrorValues(other, epsilon);
    }

    /**
     * checks for equal 'get' values with tolerance band, may be overwritten by derived classes
     * 
     * @param other class
     * @param epsilon tolerance threshold
     * @return {@code true} if equal
     */
    protected boolean equalValues(final DataSet other, final double epsilon) {
        if (epsilon <= 0.0) {
            for (int dimIndex = 0; dimIndex < this.getDimension(); dimIndex++) {
                for (int index = 0; index < this.getDataCount(dimIndex); index++) {
                    if (get(dimIndex, index) != other.get(dimIndex, index)) {
                        return false;
                    }
                }
            }
        } else {
            for (int dimIndex = 0; dimIndex < this.getDimension(); dimIndex++) {
                for (int index = 0; index < this.getDataCount(dimIndex); index++) {
                    if (!MathUtils.nearlyEqual(get(dimIndex, index), other.get(dimIndex, index), epsilon)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Notifies listeners that the data has been invalidated. If the data is added to the chart, it triggers repaint.
     * 
     * @param event the change event
     * @return itself (fluent design)
     */
    public D fireInvalidated(final UpdateEvent event) {
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
     * Returns label of a data point specified by the index. The label can be used as a category name if
     * CategoryStepsDefinition is used or for annotations displayed for data points.
     *
     * @param index of the data label
     * @return data point label specified by the index or <code>null</code> if no label has been specified
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

    @Override
    protected D getThis() {
        return (D) this;
    }

    @Override
    public List<String> getWarningList() {
        return warningList;
    }

    /**
     * Gets the index of the data point closest to the given x coordinate.
     * If the x coordinate lies outside the range of the data set, the index of the first/last point is returned.
     *
     * @param dimIndex the dimension index
     * @param x the x position of the data point
     * @return the index of the data point
     */
    @Override
    public int getIndex(final int dimIndex, final double x) {
        if (this.getDataCount() == 0) {
            return 0;
        }

        if (!Double.isFinite(x)) {
            return 0;
        }

        if (x < this.getAxisDescription(dimIndex).getMin()) {
            return 0;
        }

        final int lastIndex = getDataCount(dimIndex) - 1;
        if (x > this.getAxisDescription(dimIndex).getMax()) {
            return lastIndex;
        }

        // binary closest search -- assumes sorted data set
        return binarySearch(dimIndex, x, 0, lastIndex);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + axesDescriptions.hashCode();
        result = prime * result + ((dataLabels == null) ? 0 : dataLabels.hashCode());
        result = prime * result + dimension;
        result = prime * result + ((editConstraints == null) ? 0 : editConstraints.hashCode());
        result = prime * result + ((errorList == null) ? 0 : errorList.hashCode());
        result = prime * result + ((infoList == null) ? 0 : infoList.hashCode());
        result = prime * result + metaInfoMap.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((warningList == null) ? 0 : warningList.hashCode());
        return result;
    }

    @Override
    public DataSetLock<? extends DataSet> lock() {
        return lock;
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

    /**
     * Computes limits (ranges) of this DataSet.
     * 
     * @param dimIndex the chosen dimension
     * @return itself (fluent design)
     */
    @Override
    public D recomputeLimits(final int dimIndex) {
        lock().writeLockGuard(() -> {
            // Clear previous ranges
            getAxisDescription(dimIndex).clear();
            final int dataCount = getDataCount(dimIndex);
            for (int i = 0; i < dataCount; i++) {
                getAxisDescription(dimIndex).add(get(dimIndex, i));
            }
        });
        return getThis();
    }

    /**
     * remove a custom data label for a point The label can be used as a category name if CategoryStepsDefinition is
     * used or for annotations displayed for data points.
     *
     * @param index of the data point
     * @return the previously set label or <code>null</code> if no label has been specified
     */
    public String removeDataLabel(final int index) {
        final String retVal = lock().writeLockGuard(() -> dataLabels.remove(index));
        fireInvalidated(new UpdatedMetaDataEvent(this, "removed label"));
        return retVal;
    }

    /**
     * A string representation of the CSS style associated with this specific {@code DataSet} data point. @see
     * #getStyle()
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
            builder.append(" dataCount(").append(i).append(")=").append(this.getDataCount(i)).append(',') //
                    .append(" axisName ='")
                    .append(desc.getName())
                    .append("',") //
                    .append(" axisUnit = '")
                    .append(desc.getUnit())
                    .append("',") //
                    .append(" axisRange = ") //
                    .append(" [min=")
                    .append(isDefined ? desc.getMin() : "NotDefined") //
                    .append(", max=")
                    .append(isDefined ? desc.getMax() : "NotDefined") //
                    .append("],");
        }
        builder.append(']');
        return builder.toString();
    }

    @Override
    public synchronized List<EventListener> updateEventListener() {
        return updateListeners;
    }
}
