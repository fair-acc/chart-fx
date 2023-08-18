package io.fair_acc.dataset.spi;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;

import io.fair_acc.dataset.*;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.locks.DataSetLock;
import io.fair_acc.dataset.locks.DefaultDataSetLock;
import io.fair_acc.dataset.spi.utils.MathUtils;
import io.fair_acc.dataset.spi.utils.StringHashMapList;
import io.fair_acc.dataset.utils.AssertUtils;
import io.fair_acc.dataset.utils.IndexedStringConsumer;

/**
 * <p>
 * The abstract implementation of DataSet interface that provides implementation of some methods.
 * </p>
 * <ul>
 * <li>It maintains the name of the DataSet
 * <li>It maintains a list of DataSetListener objects and provides methods that can be used to dispatch DataSetEvent
 * events.
 * <li>It maintains ranges for all dimensions of the DataSet.
 * <li>It maintains the names and units for the axes
 * </ul>
 *
 * @param <D> java generics handling of DataSet for derived classes (needed for fluent design)
 */
public abstract class AbstractDataSet<D extends AbstractStylable<D>> extends AbstractStylable<D> implements DataSet, DataSetMetaData {
    private static final long serialVersionUID = -7612136495756923417L;

    private static final String[] DEFAULT_AXES_NAME = { "x-Axis", "y-Axis", "z-Axis" };
    private String name;
    protected final int dimension;
    private boolean isVisible = true;
    private final List<AxisDescription> axesDescriptions = new ArrayList<>();
    private final transient BitState state = BitState.initDirty(this);
    private final transient DataSetLock<? extends DataSet> lock = new DefaultDataSetLock<>(this);
    private final StringHashMapList dataLabels = new StringHashMapList();
    private final StringHashMapList dataStyles = new StringHashMapList();
    private final List<String> styleClasses = new ArrayList<>();
    private final List<String> infoList = new ArrayList<>();
    private final List<String> warningList = new ArrayList<>();
    private final List<String> errorList = new ArrayList<>();
    private transient EditConstraints editConstraints;
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
            final AxisDescription axisDescription = new DefaultAxisDescription(i, axisName, "a.u.");
            axisDescription.addListener(state);
            axesDescriptions.add(axisDescription);
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
        fireInvalidated(ChartBits.DataSetMetaData);
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
        fireInvalidated(ChartBits.DataSetMetaData);
        return retVal;
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
        fireInvalidated(ChartBits.DataSetMetaData);
        return getThis();
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
            return getDataLabelMap().equals(otherAbsDs.getDataLabelMap());
        }
        for (int index = 0; index < getDataCount(); index++) {
            final String label1 = this.getDataLabel(index);
            final String label2 = other.getDataLabel(index);
            if (!Objects.equals(label1, label2)) {
                return false;
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

            return editConstraints == null || editConstraints.equals(otherEditDs.getEditConstraints());
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
                for (int index = 0; index < this.getDataCount(); index++) {
                    if (thisErrorDs.getErrorNegative(dimIndex, index) != otherErrorDs.getErrorNegative(dimIndex, index)) {
                        return false;
                    }
                    if (thisErrorDs.getErrorPositive(dimIndex, index) != otherErrorDs.getErrorPositive(dimIndex, index)) {
                        return false;
                    }
                }
            }
            return true;
        }

        for (int dimIndex = 0; dimIndex < this.getDimension(); dimIndex++) {
            for (int index = 0; index < this.getDataCount(); index++) {
                if (!MathUtils.nearlyEqual(thisErrorDs.getErrorNegative(dimIndex, index), otherErrorDs.getErrorNegative(dimIndex, index), epsilon)) {
                    return false;
                }
                if (!MathUtils.nearlyEqual(thisErrorDs.getErrorPositive(dimIndex, index), otherErrorDs.getErrorPositive(dimIndex, index), epsilon)) {
                    return false;
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
            return getMetaInfo().equals(otherMetaDs.getMetaInfo());
        }
        return true;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DataSet)) {
            return false;
        }
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

        // check dimension and data counts
        if (this.getDimension() != other.getDimension()) {
            return false;
        }
        if (this.getDataCount() != other.getDataCount()) {
            return false;
        }

        // check names
        final String name1 = this.getName();
        final String name2 = other.getName();

        if (!(Objects.equals(name1, name2))) {
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
                for (int index = 0; index < this.getDataCount(); index++) {
                    if (get(dimIndex, index) != other.get(dimIndex, index)) {
                        return false;
                    }
                }
            }
        } else {
            for (int dimIndex = 0; dimIndex < this.getDimension(); dimIndex++) {
                for (int index = 0; index < this.getDataCount(); index++) {
                    if (!MathUtils.nearlyEqual(get(dimIndex, index), other.get(dimIndex, index), epsilon)) {
                        return false;
                    }
                }
            }
        }
        return true;
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
        return dataLabels.get(index);
    }

    @Override
    public boolean hasDataLabels() {
        return !dataLabels.isEmpty();
    }

    @Override
    public void forEachDataLabel(int minIx, int maxIx, IndexedStringConsumer consumer) {
        for (Map.Entry<Integer, String> entry : dataLabels.entrySet()) {
            int index = entry.getKey();
            if (index >= minIx && index < maxIx) {
                consumer.accept(index, entry.getValue());
            }
        }
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

    @Override
    public List<String> getStyleClasses() {
        return styleClasses;
    }

    public D addStyleClasses(String... cssClass) {
        boolean changed = false;
        for (String selector : cssClass) {
            if (!styleClasses.contains(selector)) {
                styleClasses.add(selector);
                changed = true;
            }
        }
        if (changed) {
            fireInvalidated(ChartBits.DataSetStyle);
        }
        return getThis();
    }

    public D removeStyleClasses(String... cssClass) {
        boolean changed = false;
        for (String selector : cssClass) {
            changed |= styleClasses.remove(selector);
        }
        if (changed) {
            fireInvalidated(ChartBits.DataSetStyle);
        }
        return getThis();
    }

    @Override
    public D setStyle(final String style) {
        if (!Objects.equals(getStyle(), style)) {
            super.setStyle(style);
            fireInvalidated(ChartBits.DataSetStyle);
        }
        return getThis();
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
    public boolean hasStyles() {
        return !dataStyles.isEmpty();
    }

    @Override
    public void forEachStyle(int minIx, int maxIx, IndexedStringConsumer consumer) {
        for (Map.Entry<Integer, String> entry : dataStyles.entrySet()) {
            int index = entry.getKey();
            if (index >= minIx && index < maxIx) {
                consumer.accept(index, entry.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected D getThis() {
        return (D) this;
    }

    @Override
    public List<String> getWarningList() {
        return warningList;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + axesDescriptions.hashCode();
        result = prime * result + dataLabels.hashCode();
        result = prime * result + dimension;
        result = prime * result + Objects.hashCode(editConstraints);
        result = prime * result + errorList.hashCode();
        result = prime * result + infoList.hashCode();
        result = prime * result + metaInfoMap.hashCode();
        result = prime * result + Objects.hashCode(name);
        result = prime * result + warningList.hashCode();
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public DataSetLock<? extends DataSet> lock() {
        return lock;
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
        fireInvalidated(ChartBits.DataSetMetaData);
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
        fireInvalidated(ChartBits.DataSetMetaData);
        return retVal;
    }

    public D setEditConstraints(final EditConstraints constraints) {
        lock().writeLockGuard(() -> editConstraints = constraints);
        fireInvalidated(ChartBits.DataSetMetaData);
        return getThis();
    }

    /**
     * Sets the name of data set (meta data)
     *
     * @param name the new name
     * @return itself (fluent design)
     */
    public D setName(final String name) {
        this.name = name;
        fireInvalidated(ChartBits.DataSetName);
        return getThis();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName()).append(" [dim=").append(getDimension()).append(',').append(" dataCount=").append(this.getDataCount()).append(',');
        for (int i = 0; i < this.getDimension(); i++) {
            final AxisDescription desc = getAxisDescription(i);
            final boolean isDefined = desc.isDefined();
            builder.append(" axisName ='")
                    .append(desc.getName())
                    .append("',") //
                    .append(" axisUnit = '")
                    .append(desc.getUnit())
                    .append("',") //
                    .append(" axisRange = ") //
                    .append(" [")
                    .append(isDefined ? desc.getMin() : "NotDefined") //
                    .append(", ")
                    .append(isDefined ? desc.getMax() : "NotDefined") //
                    .append("],");
            if (this instanceof DataSetError) {
                DataSetError.ErrorType error = ((DataSetError) this).getErrorType(i);
                builder.append(" errorType(").append(i).append(")=").append(error).append(',');
            }
        }
        builder.append(']');
        return builder.toString();
    }

    @Override
    public double[] getValues(final int dimIndex) {
        final int n = getDataCount();
        final double[] retValues = new double[n];
        for (int i = 0; i < n; i++) {
            retValues[i] = get(dimIndex, i);
        }
        return retValues;
    }

    @Override
    public double getValue(final int dimIndex, final double... x) {
        AssertUtils.checkArrayDimension("x", x, 1);
        final int index1 = getIndex(DIM_X, x);
        final double x1 = get(DIM_X, index1);
        final double y1 = get(dimIndex, index1);
        int index2 = x1 < x[0] ? index1 + 1 : index1 - 1;
        index2 = Math.max(0, Math.min(index2, this.getDataCount() - 1));
        final double y2 = get(dimIndex, index2);

        if (Double.isNaN(y1) || Double.isNaN(y2)) {
            // case where the function has a gap (y-coordinate equals to NaN
            return Double.NaN;
        }

        final double x2 = get(DIM_X, index2);
        if (x1 == x2) {
            return get(dimIndex, index1);
        }

        final double de1 = get(dimIndex, index1);
        return de1 + (get(dimIndex, index2) - de1) * (x[0] - x1) / (x2 - x1);
    }

    public static int binarySearch(final double search, final int indexMin, final int indexMax, IntToDoubleFunction getter) {
        if (indexMin == indexMax) {
            return indexMin;
        }
        if (indexMax - indexMin == 1) {
            if (Math.abs(getter.applyAsDouble(indexMin) - search) < Math.abs(getter.applyAsDouble(indexMax) - search)) {
                return indexMin;
            }
            return indexMax;
        }
        final int middle = (indexMax + indexMin) / 2;
        final double valMiddle = getter.applyAsDouble(middle);
        if (valMiddle == search) {
            return middle;
        }
        if (search < valMiddle) {
            return binarySearch(search, indexMin, middle, getter);
        }
        return binarySearch(search, middle, indexMax, getter);
    }

    @Override
    public int getIndex(final int dimIndex, final double... x) {
        AssertUtils.checkArrayDimension("x", x, 1);
        if (this.getDataCount() == 0) {
            return 0;
        }

        if (!Double.isFinite(x[0])) {
            return 0;
        }

        final double min = this.getAxisDescription(dimIndex).getMin();
        final double max = this.getAxisDescription(dimIndex).getMax();

        if ((Double.isFinite(min) && x[0] <= min) || x[0] <= get(dimIndex, 0)) {
            return 0;
        }

        final int lastIndex = getDataCount() - 1;
        if ((Double.isFinite(max) && x[0] >= max) || x[0] >= get(dimIndex, getDataCount() - 1)) {
            return lastIndex;
        }

        // binary closest search -- assumes sorted data set
        return binarySearch(x[0], 0, lastIndex, val -> get(dimIndex, val));
    }

    @Override
    public DataSet recomputeLimits(final int dimIndex) {
        // first compute range (does not trigger notify events)
        DataRange newRange = new DataRange();
        final int dataCount = getDataCount();
        for (int i = 0; i < dataCount; i++) {
            newRange.add(get(dimIndex, i));
        }
        // set to new computed one and trigger notify event if different to old limits
        getAxisDescription(dimIndex).set(newRange.getMin(), newRange.getMax());
        return this;
    }

    @Override
    public BitState getBitState() {
        return state;
    }

    protected boolean copyMetaData(final DataSet other) {
        this.setName(other.getName());
        if (!(other instanceof DataSetMetaData)) {
            return false;
        }
        DataSetMetaData otherMeta = (DataSetMetaData) other;
        infoList.clear();
        infoList.addAll(otherMeta.getInfoList());
        warningList.clear();
        warningList.addAll(otherMeta.getWarningList());
        errorList.clear();
        errorList.addAll(otherMeta.getErrorList());
        metaInfoMap.clear();
        metaInfoMap.putAll(otherMeta.getMetaInfo());

        return true;
    }

    protected void copyDataLabelsAndStyles(final DataSet other, final boolean copy) {
        this.setStyle(other.getStyle());

        if (copy || !(other instanceof AbstractDataSet)) {
            // deep copy data point labels and styles
            getDataLabelMap().clear();
            for (int index = 0; index < other.getDataCount(); index++) {
                final String label = other.getDataLabel(index);
                if (label != null && !label.isEmpty()) {
                    this.addDataLabel(index, label);
                }
            }
            getDataStyleMap().clear();
            for (int index = 0; index < other.getDataCount(); index++) {
                final String style = other.getStyle(index);
                if (style != null && !style.isEmpty()) {
                    this.addDataStyle(index, style);
                }
            }
            return;
        }

        var otherAbstract = (AbstractDataSet<?>) other;
        getDataLabelMap().clear();
        getDataLabelMap().putAll(otherAbstract.getDataLabelMap());
        getDataStyleMap().clear();
        getDataStyleMap().putAll(otherAbstract.getDataStyleMap());
    }

    protected void copyAxisDescription(final DataSet other) {
        // synchronise axis description
        for (int dimIndex = 0; dimIndex < getDimension(); dimIndex++) {
            this.getAxisDescription(dimIndex).set(other.getAxisDescription(dimIndex));
        }
    }
}
