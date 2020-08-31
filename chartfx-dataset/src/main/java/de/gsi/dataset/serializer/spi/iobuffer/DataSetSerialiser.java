package de.gsi.dataset.serializer.spi.iobuffer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.*;
import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.FieldDescription;
import de.gsi.dataset.serializer.IoSerialiser;
import de.gsi.dataset.serializer.spi.WireDataFieldDescription;
import de.gsi.dataset.spi.AbstractDataSet;
import de.gsi.dataset.spi.DataSetBuilder;
import de.gsi.dataset.spi.utils.StringHashMapList;
import de.gsi.dataset.utils.AssertUtils;
import de.gsi.dataset.utils.GenericsHelper;

/**
 * Class to efficiently serialise and de-serialise DataSet objects into binary byte arrays. The performance can be tuned
 * through:
 * <ul>
 * <li>using floats (ie. memory-IO vs network-IO bound serialisation), or</li>
 * <li>via {@link #setDataLablesSerialised(boolean)} (default: true) to control whether data labels and styles shall be processed</li>
 * <li>via {@link #setMetaDataSerialised(boolean)} (default: true) to control whether meta data shall be processed</li>
 * </ul>
 *
 * @author rstein
 */
public class DataSetSerialiser { // NOPMD
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetSerialiser.class);
    private static final String DATA_SET_NAME = "dataSetName";
    private static final String DIMENSIONS = "nDims";
    private static final String ARRAY_PREFIX = "array";
    private static final String EN_PREFIX = "en";
    private static final String EP_PREFIX = "ep";
    private static final String AXIS = "axis";
    private static final String NAME = "name";
    private static final String UNIT = "unit";
    private static final String MIN = "Min";
    private static final String MAX = "Max";
    private static final String META_INFO = "metaInfo";
    private static final String ERROR_LIST = "errorList";
    private static final String WARNING_LIST = "warningList";
    private static final String INFO_LIST = "infoList";
    private static final String DATA_STYLES = "dataStyles";
    private static final String DATA_LABELS = "dataLabels";
    private boolean transmitDataLabels = true;
    private boolean transmitMetaData = true;
    private final IoSerialiser ioSerialiser;

    public DataSetSerialiser(final IoSerialiser ioSerialiser) {
        this.ioSerialiser = ioSerialiser;
    }

    protected FieldDescription checkFieldCompatibility(final FieldDescription rootField, final int fieldNameHashCode, final String fieldName, final DataType... requireDataTypes) {
        FieldDescription fieldHeader = rootField.findChildField(fieldNameHashCode, fieldName);
        if (fieldHeader == null) {
            return null;
        }

        boolean foundMatchingDataType = false;
        for (DataType dataType : requireDataTypes) {
            if (fieldHeader.getDataType().equals(dataType)) {
                foundMatchingDataType = true;
                break;
            }
        }
        if (!foundMatchingDataType) {
            throw new InputMismatchException(fieldName + " is type " + fieldHeader.getDataType() + " vs. required type " + Arrays.asList(requireDataTypes).toString());
        }

        ioSerialiser.getBuffer().position(fieldHeader.getDataStartPosition());
        return fieldHeader;
    }

    public boolean isDataLablesSerialised() {
        return transmitDataLabels;
    }

    public boolean isMetaDataSerialised() {
        return transmitMetaData;
    }

    protected void parseDataLabels(final DataSetBuilder builder, final FieldDescription fieldRoot) {
        if (checkFieldCompatibility(fieldRoot, DATA_LABELS.hashCode(), DATA_LABELS, DataType.MAP) != null) {
            Map<Integer, String> map = new ConcurrentHashMap<>();
            map = ioSerialiser.getMap(map, null);
            builder.setDataLabelMap(map);
        }

        if (checkFieldCompatibility(fieldRoot, DATA_STYLES.hashCode(), DATA_STYLES, DataType.MAP) != null) {
            Map<Integer, String> map = new ConcurrentHashMap<>();
            map = ioSerialiser.getMap(map, null);
            builder.setDataStyleMap(map);
        }
    }

    protected void parseHeaders(final IoSerialiser ioSerialiser, final DataSetBuilder builder, final FieldDescription fieldRoot) {
        // read strings
        if (checkFieldCompatibility(fieldRoot, DATA_SET_NAME.hashCode(), DATA_SET_NAME, DataType.STRING) != null) {
            builder.setName(ioSerialiser.getBuffer().getString());
        }

        if (checkFieldCompatibility(fieldRoot, DIMENSIONS.hashCode(), DIMENSIONS, DataType.INT) != null) {
            builder.setDimension(ioSerialiser.getBuffer().getInt());
        }

        // check for axis descriptions (all fields starting with AXIS)
        for (FieldDescription fieldDescription : fieldRoot.getChildren()) {
            parseHeader(ioSerialiser, builder, fieldDescription);
        }
    }

    private void parseHeader(final IoSerialiser ioSerialiser, final DataSetBuilder builder, FieldDescription fieldDescription) {
        final String fieldName = fieldDescription.getFieldName();
        if (fieldName == null || !fieldName.startsWith(AXIS)) {
            return; // not axis related field
        }
        final String[] parsed = fieldName.split("\\.");
        if (parsed.length <= 1) {
            return; // couldn't parse axis field
        }
        final int dimension = getDimIndex(parsed[0], AXIS);
        if (dimension < 0) {
            return; // couldn't parse dimIndex
        }
        ioSerialiser.getBuffer().position(fieldDescription.getDataStartPosition());
        switch (parsed[1]) {
        case MIN:
            builder.setAxisMin(dimension, ioSerialiser.getBuffer().getDouble());
            break;
        case MAX:
            builder.setAxisMax(dimension, ioSerialiser.getBuffer().getDouble());
            break;
        case NAME:
            builder.setAxisName(dimension, ioSerialiser.getBuffer().getString());
            break;
        case UNIT:
            builder.setAxisUnit(dimension, ioSerialiser.getBuffer().getString());
            break;
        default:
            LOGGER.atWarn().addArgument(parsed[1]).log("parseHeader(): encountered unknown tag {} - ignore");
            break;
        }
    }

    protected void parseMetaData(final IoSerialiser ioSerialiser, final DataSetBuilder builder, final FieldDescription rootField) {
        if (checkFieldCompatibility(rootField, INFO_LIST.hashCode(), INFO_LIST, DataType.STRING_ARRAY) != null) {
            builder.setMetaInfoList(ioSerialiser.getStringArray());
        }

        if (checkFieldCompatibility(rootField, WARNING_LIST.hashCode(), WARNING_LIST, DataType.STRING_ARRAY) != null) {
            builder.setMetaWarningList(ioSerialiser.getStringArray());
        }

        if (checkFieldCompatibility(rootField, ERROR_LIST.hashCode(), ERROR_LIST, DataType.STRING_ARRAY) != null) {
            builder.setMetaErrorList(ioSerialiser.getStringArray());
        }

        if (checkFieldCompatibility(rootField, META_INFO.hashCode(), META_INFO, DataType.MAP) != null) {
            Map<String, String> map = new ConcurrentHashMap<>();
            map = ioSerialiser.getMap(map, null);
            builder.setMetaInfoMap(map);
        }
    }

    protected void parseNumericData(final IoSerialiser ioSerialiser, final DataSetBuilder builder, final FieldDescription rootField) {
        // check for numeric data
        for (FieldDescription fieldDescription : rootField.getChildren()) {
            final String fieldName = fieldDescription.getFieldName();
            if (fieldName == null || (fieldDescription.getDataType() != DataType.DOUBLE_ARRAY && fieldDescription.getDataType() != DataType.FLOAT_ARRAY)) {
                continue;
            }
            if (fieldName.startsWith(ARRAY_PREFIX)) {
                readValues(ioSerialiser, builder, fieldDescription, fieldName);
            } else if (fieldName.startsWith(EP_PREFIX)) {
                readPosError(ioSerialiser, builder, fieldDescription, fieldName);
            } else if (fieldName.startsWith(EN_PREFIX)) {
                readNegError(ioSerialiser, builder, fieldDescription, fieldName);
            }
        }
    }

    private void readValues(final IoSerialiser ioSerialiser, final DataSetBuilder builder, FieldDescription fieldDescription,
            final String fieldName) {
        int dimIndex = getDimIndex(fieldName, ARRAY_PREFIX);
        if (dimIndex >= 0) {
            ioSerialiser.getBuffer().position(fieldDescription.getDataStartPosition());
            builder.setValuesNoCopy(dimIndex, getDoubleArray(ioSerialiser, fieldDescription.getDataType()));
        }
    }

    private void readNegError(final IoSerialiser ioSerialiser, final DataSetBuilder builder, FieldDescription fieldDescription, final String fieldName) {
        int dimIndex = getDimIndex(fieldName, EN_PREFIX);
        if (dimIndex >= 0) {
            ioSerialiser.getBuffer().position(fieldDescription.getDataStartPosition());
            builder.setNegErrorNoCopy(dimIndex, getDoubleArray(ioSerialiser, fieldDescription.getDataType()));
        }
    }

    private void readPosError(final IoSerialiser ioSerialiser, final DataSetBuilder builder, FieldDescription fieldDescription,
            final String fieldName) {
        int dimIndex = getDimIndex(fieldName, EP_PREFIX);
        if (dimIndex >= 0) {
            ioSerialiser.getBuffer().position(fieldDescription.getDataStartPosition());
            builder.setPosErrorNoCopy(dimIndex, getDoubleArray(ioSerialiser, fieldDescription.getDataType()));
        }
    }

    protected static int getDimIndex(String fieldName, String prefix) {
        try {
            return Integer.parseInt(fieldName.substring(prefix.length()));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            LOGGER.atWarn().addArgument(fieldName).log("Invalid field name: {}");
            return -1;
        }
    }

    /**
     * Read a Dataset from a byte array containing comma separated values.<br>
     * The data format is a custom extension of csv with an additional #-commented Metadata Header and a $-commented
     * column header. Expects the following columns in this order to be present: index, x, y, eyn, eyp.
     *
     * @return DataSet with the data and metadata read from the file
     */
    public DataSet readDataSetFromByteArray() { // NOPMD
        final DataSetBuilder builder = new DataSetBuilder();
        FieldDescription root = ioSerialiser.parseIoStream(false);
        final FieldDescription fieldRoot = root.getChildren().get(0);
        // parsed until end of buffer

        parseHeaders(ioSerialiser, builder, fieldRoot);

        if (isMetaDataSerialised()) {
            parseMetaData(ioSerialiser, builder, fieldRoot);
        }

        if (isDataLablesSerialised()) {
            parseDataLabels(builder, fieldRoot);
        }

        parseNumericData(ioSerialiser, builder, fieldRoot);

        if (root.getChildren().size() != 2) {
            throw new IllegalArgumentException("fieldRoot children-count != 2: " + fieldRoot.getChildren().size());
        }
        final FieldDescription endMarker = root.getChildren().get(1);
        if (endMarker.getDataType() != DataType.END_MARKER) {
            throw new IllegalArgumentException("fieldRoot END_MARKER expected but found: " + endMarker);
        }
        // move read position to after end marker
        ioSerialiser.getBuffer().position(endMarker.getDataStartPosition());
        ioSerialiser.updateDataEndMarker((WireDataFieldDescription) endMarker);
        return builder.build();
    }

    public void setDataLablesSerialised(final boolean state) {
        transmitDataLabels = state;
    }

    public void setMetaDataSerialised(final boolean state) {
        transmitMetaData = state;
    }

    private float[] toFloats(final double[] input) {
        final float[] floatArray = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            floatArray[i] = (float) input[i];
        }
        return floatArray;
    }

    protected void writeDataLabelsToStream(final DataSet dataSet) {
        if (dataSet instanceof AbstractDataSet) {
            final StringHashMapList labelMap = ((AbstractDataSet<?>) dataSet).getDataLabelMap();
            if (!labelMap.isEmpty()) {
                ioSerialiser.put(DATA_LABELS, labelMap, Integer.class, String.class, null);
            }
            final StringHashMapList styleMap = ((AbstractDataSet<?>) dataSet).getDataStyleMap();
            if (!styleMap.isEmpty()) {
                ioSerialiser.put(DATA_STYLES, styleMap, Integer.class, String.class, null);
            }
            return;
        }

        final int dataCount = dataSet.getDataCount();
        final Map<Integer, String> labelMap = new ConcurrentHashMap<>();
        for (int index = 0; index < dataCount; index++) {
            final String label = dataSet.getDataLabel(index);
            if ((label != null) && !label.isEmpty()) {
                labelMap.put(index, label);
            }
        }
        if (!labelMap.isEmpty()) {
            ioSerialiser.put(DATA_LABELS, labelMap, Integer.class, String.class, null);
        }

        final Map<Integer, String> styleMap = new HashMap<>();
        for (int index = 0; index < dataCount; index++) {
            final String style = dataSet.getStyle(index);
            if ((style != null) && !style.isEmpty()) {
                styleMap.put(index, style);
            }
        }
        if (!styleMap.isEmpty()) {
            ioSerialiser.put(DATA_STYLES, styleMap, Integer.class, String.class, null);
        }
    }

    /**
     * Write data set into byte buffer.
     *
     * @param dataSet The DataSet to export
     * @param asFloat {@code true}: encode data as binary floats (smaller size, performance), or {@code false} as double
     *            (better precision)
     */
    public void writeDataSetToByteArray(final DataSet dataSet, final boolean asFloat) {
        AssertUtils.notNull("dataSet", dataSet);
        AssertUtils.notNull("ioSerialiser", ioSerialiser);
        final String dataStartMarkerName = "START_MARKER_DATASET:" + dataSet.getName();
        final WireDataFieldDescription dataStartMarker = new WireDataFieldDescription(ioSerialiser, null, dataStartMarkerName.hashCode(), dataStartMarkerName, DataType.OTHER, -1, -1, -1);
        ioSerialiser.putStartMarker(dataStartMarker);

        writeHeaderDataToStream(dataSet);

        if (isMetaDataSerialised()) {
            writeMetaDataToStream(dataSet);
        }

        if (isDataLablesSerialised()) {
            writeDataLabelsToStream(dataSet);
        }

        if (asFloat) {
            writeNumericBinaryDataToBufferFloat(dataSet);

        } else {
            writeNumericBinaryDataToBufferDouble(dataSet);
        }

        final String dataEndMarkerName = "END_MARKER_DATASET:" + dataSet.getName();
        final WireDataFieldDescription dataEndMarker = new WireDataFieldDescription(ioSerialiser, null, dataEndMarkerName.hashCode(), dataEndMarkerName, DataType.START_MARKER, -1, -1, -1);
        ioSerialiser.putEndMarker(dataEndMarker);
    }

    protected void writeHeaderDataToStream(final DataSet dataSet) {
        // common header data
        ioSerialiser.put(DATA_SET_NAME, dataSet.getName());
        ioSerialiser.put(DIMENSIONS, dataSet.getDimension());
        final List<AxisDescription> axisDescriptions = dataSet.getAxisDescriptions();
        StringBuilder builder = new StringBuilder(60);
        for (int i = 0; i < axisDescriptions.size(); i++) {
            builder.setLength(0);
            final String prefix = builder.append(AXIS).append(i).append('.').toString();
            builder.setLength(0);
            final String name = builder.append(prefix).append(NAME).toString();
            builder.setLength(0);
            final String unit = builder.append(prefix).append(UNIT).toString();
            builder.setLength(0);
            final String minName = builder.append(prefix).append(MIN).toString();
            builder.setLength(0);
            final String maxName = builder.append(prefix).append(MAX).toString();

            ioSerialiser.put(name, dataSet.getAxisDescription(i).getName());
            ioSerialiser.put(unit, dataSet.getAxisDescription(i).getUnit());
            ioSerialiser.put(minName, dataSet.getAxisDescription(i).getMin());
            ioSerialiser.put(maxName, dataSet.getAxisDescription(i).getMax());
        }
    }

    protected void writeMetaDataToStream(final DataSet dataSet) {
        if (!(dataSet instanceof DataSetMetaData)) {
            return;
        }
        final DataSetMetaData metaDataSet = (DataSetMetaData) dataSet;

        ioSerialiser.put(INFO_LIST, metaDataSet.getInfoList().toArray(new String[0]));
        ioSerialiser.put(WARNING_LIST, metaDataSet.getWarningList().toArray(new String[0]));
        ioSerialiser.put(ERROR_LIST, metaDataSet.getErrorList().toArray(new String[0]));
        ioSerialiser.put(META_INFO, metaDataSet.getMetaInfo(), String.class, String.class, null);
    }

    /**
     * @param dataSet to be exported
     */
    protected void writeNumericBinaryDataToBufferFloat(final DataSet dataSet) {
        final int nDim = dataSet.getDimension();
        if (dataSet instanceof GridDataSet) {
            GridDataSet gridDataSet = (GridDataSet) dataSet;
            for (int dimIndex = 0; dimIndex < nDim; dimIndex++) {
                final boolean gridDimension = dimIndex < gridDataSet.getNGrid();
                final int nsamples = gridDimension ? gridDataSet.getShape(dimIndex) : dataSet.getDataCount();
                final float[] values = toFloats(gridDimension ? gridDataSet.getGridValues(dimIndex) : dataSet.getValues(dimIndex));
                ioSerialiser.put(ARRAY_PREFIX + dimIndex, values, nsamples);
            }
            return; // GridDataSet does not provide errors
        }
        for (int dimIndex = 0; dimIndex < nDim; dimIndex++) {
            final int nsamples = dataSet.getDataCount();
            ioSerialiser.put(ARRAY_PREFIX + dimIndex, toFloats(dataSet.getValues(dimIndex)), nsamples);
        }

        if (!(dataSet instanceof DataSetError)) {
            return; // data set does not have any error definition
        }

        final DataSetError ds = (DataSetError) dataSet;
        for (int dimIndex = 0; dimIndex < nDim; dimIndex++) {
            final int nsamples = dataSet.getDataCount();
            switch (ds.getErrorType(dimIndex)) {
            default:
            case NO_ERROR:
                break;
            case SYMMETRIC:
                ioSerialiser.put(EP_PREFIX + dimIndex, toFloats(ds.getErrorsPositive(dimIndex)), nsamples);
                break;
            case ASYMMETRIC:
                ioSerialiser.put(EN_PREFIX + dimIndex, toFloats(ds.getErrorsNegative(dimIndex)), nsamples);
                ioSerialiser.put(EP_PREFIX + dimIndex, toFloats(ds.getErrorsPositive(dimIndex)), nsamples);
                break;
            }
        }
    }

    /**
     * @param dataSet to be exported
     */
    protected void writeNumericBinaryDataToBufferDouble(final DataSet dataSet) {
        final int nDim = dataSet.getDimension();
        if (dataSet instanceof GridDataSet) {
            GridDataSet gridDataSet = (GridDataSet) dataSet;
            for (int dimIndex = 0; dimIndex < nDim; dimIndex++) {
                final boolean gridDimension = dimIndex < gridDataSet.getNGrid();
                final int nsamples = gridDimension ? gridDataSet.getShape(dimIndex) : dataSet.getDataCount();
                final double[] values = gridDimension ? gridDataSet.getGridValues(dimIndex) : dataSet.getValues(dimIndex);
                ioSerialiser.put(ARRAY_PREFIX + dimIndex, values, nsamples);
            }
            return; // GridDataSet does not provide errors
        }
        for (int dimIndex = 0; dimIndex < nDim; dimIndex++) {
            final int nsamples = dataSet.getDataCount();
            ioSerialiser.put(ARRAY_PREFIX + dimIndex, dataSet.getValues(dimIndex), nsamples);
        }
        if (!(dataSet instanceof DataSetError)) {
            return; // data set does not have any error definition
        }
        final DataSetError ds = (DataSetError) dataSet;
        for (int dimIndex = 0; dimIndex < nDim; dimIndex++) {
            final int nsamples = dataSet.getDataCount();
            switch (ds.getErrorType(dimIndex)) {
            case SYMMETRIC:
                ioSerialiser.put(EP_PREFIX + dimIndex, ds.getErrorsPositive(dimIndex), nsamples);
                break;
            case ASYMMETRIC:
                ioSerialiser.put(EN_PREFIX + dimIndex, ds.getErrorsNegative(dimIndex), nsamples);
                ioSerialiser.put(EP_PREFIX + dimIndex, ds.getErrorsPositive(dimIndex), nsamples);
                break;
            case NO_ERROR:
            default:
                break;
            }
        }
    }

    protected static double[] getDoubleArray(IoSerialiser ioSerialiser, final DataType dataType) {
        switch (dataType) {
        case BOOL_ARRAY:
            return GenericsHelper.toDoublePrimitive(ioSerialiser.getBooleanArray());
        case BYTE_ARRAY:
            return GenericsHelper.toDoublePrimitive(ioSerialiser.getByteArray());
        case SHORT_ARRAY:
            return GenericsHelper.toDoublePrimitive(ioSerialiser.getShortArray());
        case INT_ARRAY:
            return GenericsHelper.toDoublePrimitive(ioSerialiser.getIntArray());
        case LONG_ARRAY:
            return GenericsHelper.toDoublePrimitive(ioSerialiser.getLongArray());
        case FLOAT_ARRAY:
            return GenericsHelper.toDoublePrimitive(ioSerialiser.getFloatArray());
        case DOUBLE_ARRAY:
            return ioSerialiser.getDoubleArray();
        case CHAR_ARRAY:
            return GenericsHelper.toDoublePrimitive(ioSerialiser.getCharArray());
        case STRING_ARRAY:
            return GenericsHelper.toDoublePrimitive(ioSerialiser.getStringArray());
        default:
            throw new IllegalArgumentException("dataType '" + dataType + "' is not an array");
        }
    }
}
