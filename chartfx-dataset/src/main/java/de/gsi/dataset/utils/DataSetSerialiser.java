package de.gsi.dataset.utils;

import java.util.InputMismatchException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.spi.DataSetBuilder;
import de.gsi.dataset.utils.serializer.BinarySerialiser;
import de.gsi.dataset.utils.serializer.BinarySerialiser.FieldHeader;
import de.gsi.dataset.utils.serializer.BinarySerialiser.HeaderInfo;
import de.gsi.dataset.utils.serializer.DataType;
import de.gsi.dataset.utils.serializer.FastByteBuffer;

/**
 * Class to efficiently serialise and de-serialise DataSet objects into binary
 * byte arrays.
 * 
 * The performance can be tuned through:
 * <ul>
 * <li>using floats (ie. memory-IO vs network-IO bound serialisation), or</li>
 * <li>via {@link #setDataLablesSerialised(boolean)} (default: false) to control
 * whether data labels and styles shall be processed</li>
 * <li>via {@link #setMetaDataSerialised(boolean)} (default: true) to control
 * whether meta data shall be processed</li>
 * </ul>
 * 
 * @author rstein
 */
public class DataSetSerialiser extends DataSetUtilsHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetSerialiser.class);
    private static final String DATA_SET_NAME = "dataSetName";
    private static final String Y_ARRAY_NAME = "y";
    private static final String X_ARRAY_NAME = "x";
    private static final String XEN = "xen";
    private static final String XEP = "xep";
    private static final String YEP = "yep";
    private static final String YEN = "yen";
    private static final String Y_MAX = "yMax";
    private static final String Y_MIN = "yMin";
    private static final String X_MAX = "xMax";
    private static final String X_MIN = "xMin";
    private static final String META_INFO = "metaInfo";
    private static final String ERROR_LIST = "errorList";
    private static final String WARNING_LIST = "warningList";
    private static final String INFO_LIST = "infoList";
    private static final String DATA_STYLES = "dataStyles";
    private static final String DATA_LABELS = "dataLabels";
    private static final String VAL_RMS = "rms";
    private static final String VAL_MEAN = "mean";
    private static final String VAL_INTEGRAL = "integral";
    private static boolean transmitDataLabels = true;
    private static boolean transmitMetaData = true;

    protected DataSetSerialiser() {
        super();
        // utility class
    }

    public static boolean isMetaDataSerialised() {
        return transmitMetaData;
    }

    public static void setMetaDataSerialised(final boolean state) {
        transmitMetaData = state;
    }

    public static boolean isDataLablesSerialised() {
        return transmitDataLabels;
    }

    public static void setDataLablesSerialised(final boolean state) {
        transmitDataLabels = state;
    }

    /**
     * Write data set into byte buffer.
     *
     * @param dataSet The DataSet to export
     * @param buffer byte output buffer (N.B. keep caching this object)
     * @param asFloat {@code true}: encode data as binary floats (smaller size,
     *            performance), or {@code false} as double (better precision)
     */
    public static void writeDataSetToByteArray(final DataSet dataSet, final FastByteBuffer buffer,
            final boolean asFloat) {
        AssertUtils.notNull("dataSet", dataSet);
        AssertUtils.notNull("buffer", buffer);

        dataSet.lock();

        BinarySerialiser.putHeaderInfo(buffer);

        writeHeaderDataToStream(buffer, dataSet);

        if (isMetaDataSerialised()) {
            writeMetaDataToStream(buffer, dataSet);
        }

        if (isDataLablesSerialised()) {
            writeDataLabelsToStream(buffer, dataSet);
        }

        writeNumericBinaryDataToBuffer(buffer, dataSet, asFloat);

        BinarySerialiser.putEndMarker(buffer);
    }

    protected static void writeHeaderDataToStream(final FastByteBuffer buffer, final DataSet dataSet) {
        // common header data
        BinarySerialiser.put(buffer, DATA_SET_NAME, dataSet.getName());
        BinarySerialiser.put(buffer, X_MIN, dataSet.getXMin());
        BinarySerialiser.put(buffer, X_MAX, dataSet.getXMax());
        BinarySerialiser.put(buffer, Y_MIN, dataSet.getYMin());
        BinarySerialiser.put(buffer, Y_MAX, dataSet.getYMax());

        // write some statistics for the human readable benefit when
        // opening the
        // file with standard text-based viewers
        BinarySerialiser.put(buffer, VAL_INTEGRAL, integralSimple(dataSet));
        BinarySerialiser.put(buffer, VAL_MEAN, mean(dataSet.getYValues()));
        BinarySerialiser.put(buffer, VAL_RMS, rootMeanSquare(dataSet.getYValues()));
    }

    protected static void writeMetaDataToStream(final FastByteBuffer buffer, final DataSet dataSet) {
        if (!(dataSet instanceof DataSetMetaData)) {
            return;
        }
        final DataSetMetaData metaDataSet = (DataSetMetaData) dataSet;

        BinarySerialiser.put(buffer, INFO_LIST, metaDataSet.getInfoList().toArray(new String[0]));
        BinarySerialiser.put(buffer, WARNING_LIST, metaDataSet.getWarningList().toArray(new String[0]));
        BinarySerialiser.put(buffer, ERROR_LIST, metaDataSet.getErrorList().toArray(new String[0]));
        BinarySerialiser.put(buffer, META_INFO, metaDataSet.getMetaInfo());
    }

    protected static void writeDataLabelsToStream(final FastByteBuffer buffer, final DataSet dataSet) {
        final int dataCount = dataSet.getDataCount();
        Map<Integer, String> labelMap = new ConcurrentHashMap<>();
        for (int index = 0; index < dataCount; index++) {
            final String label = dataSet.getDataLabel(index);
            if (label != null && !label.isEmpty()) {
                labelMap.put(index, label);
            }
        }
        if (!labelMap.isEmpty()) {
            BinarySerialiser.put(buffer, DATA_LABELS, labelMap);
        }

        Map<Integer, String> styleMap = new ConcurrentHashMap<>();
        for (int index = 0; index < dataCount; index++) {
            final String style = dataSet.getStyle(index);
            if (style != null && !style.isEmpty()) {
                styleMap.put(index, style);
            }
        }
        if (!styleMap.isEmpty()) {
            BinarySerialiser.put(buffer, DATA_STYLES, styleMap);
        }
    }

    /**
     * @param buffer FastByteBuffer to write binary data into
     * @param dataSet to be exported
     * @param asFloat {@code true} use 32-bit floats (less memory, faster
     *            transfer) instead of 64-bit doubles (DataSet default, higher
     *            precision)
     */
    protected static void writeNumericBinaryDataToBuffer(final FastByteBuffer buffer, final DataSet dataSet,
            final boolean asFloat) {
        final int nsamples = dataSet.getDataCount();

        if (asFloat) {
            BinarySerialiser.put(buffer, X_ARRAY_NAME, toFloats(dataSet.getXValues()), new int[] { nsamples });
            BinarySerialiser.put(buffer, Y_ARRAY_NAME, toFloats(dataSet.getYValues()), new int[] { nsamples });
            if (!(dataSet instanceof DataSetError)) {
                // data set does not have any error definition
                return;
            }
            final DataSetError ds = (DataSetError) dataSet;
            switch (ds.getErrorType()) {
            case NO_ERROR:
                break;
            case X:
                BinarySerialiser.put(buffer, XEP, toFloats(ds.getXErrorsPositive()), new int[] { nsamples });
                break;
            case X_ASYMMETRIC:
                BinarySerialiser.put(buffer, XEN, toFloats(ds.getXErrorsNegative()), new int[] { nsamples });
                BinarySerialiser.put(buffer, XEP, toFloats(ds.getXErrorsPositive()), new int[] { nsamples });
                break;
            case Y:
                BinarySerialiser.put(buffer, YEP, toFloats(ds.getYErrorsPositive()), new int[] { nsamples });
                break;
            case XY:
                BinarySerialiser.put(buffer, XEP, toFloats(ds.getXErrorsPositive()), new int[] { nsamples });
                BinarySerialiser.put(buffer, YEP, toFloats(ds.getYErrorsPositive()), new int[] { nsamples });
                break;
            case Y_ASYMMETRIC:
                BinarySerialiser.put(buffer, YEN, toFloats(ds.getYErrorsNegative()), new int[] { nsamples });
                BinarySerialiser.put(buffer, YEP, toFloats(ds.getYErrorsPositive()), new int[] { nsamples });
                break;
            case XY_ASYMMETRIC:
            default:
                BinarySerialiser.put(buffer, XEN, toFloats(ds.getXErrorsNegative()), new int[] { nsamples });
                BinarySerialiser.put(buffer, XEP, toFloats(ds.getXErrorsPositive()), new int[] { nsamples });
                BinarySerialiser.put(buffer, YEN, toFloats(ds.getYErrorsNegative()), new int[] { nsamples });
                BinarySerialiser.put(buffer, YEP, toFloats(ds.getYErrorsPositive()), new int[] { nsamples });
                break;
            }
        } else {
            BinarySerialiser.put(buffer, X_ARRAY_NAME, dataSet.getXValues(), new int[] { nsamples });
            BinarySerialiser.put(buffer, Y_ARRAY_NAME, dataSet.getYValues(), new int[] { nsamples });
            if (!(dataSet instanceof DataSetError)) {
                // data set does not have any error definition
                return;
            }
            final DataSetError ds = (DataSetError) dataSet;
            switch (ds.getErrorType()) {
            case NO_ERROR:
                break;
            case X:
                BinarySerialiser.put(buffer, XEP, ds.getXErrorsPositive(), new int[] { nsamples });
                break;
            case X_ASYMMETRIC:
                BinarySerialiser.put(buffer, XEN, ds.getXErrorsNegative(), new int[] { nsamples });
                BinarySerialiser.put(buffer, XEP, ds.getXErrorsPositive(), new int[] { nsamples });
                break;
            case Y:
                BinarySerialiser.put(buffer, YEP, ds.getYErrorsPositive(), new int[] { nsamples });
                break;
            case XY:
                BinarySerialiser.put(buffer, XEP, ds.getXErrorsPositive(), new int[] { nsamples });
                BinarySerialiser.put(buffer, YEP, ds.getYErrorsPositive(), new int[] { nsamples });
                break;
            case Y_ASYMMETRIC:
                BinarySerialiser.put(buffer, YEN, ds.getYErrorsNegative(), new int[] { nsamples });
                BinarySerialiser.put(buffer, YEP, ds.getYErrorsPositive(), new int[] { nsamples });
                break;
            case XY_ASYMMETRIC:
            default:
                BinarySerialiser.put(buffer, XEN, ds.getXErrorsNegative(), new int[] { nsamples });
                BinarySerialiser.put(buffer, XEP, ds.getXErrorsPositive(), new int[] { nsamples });
                BinarySerialiser.put(buffer, YEN, ds.getYErrorsNegative(), new int[] { nsamples });
                BinarySerialiser.put(buffer, YEP, ds.getYErrorsPositive(), new int[] { nsamples });
                break;
            }
        }
    }

    protected static boolean checkFieldCompatibility(final FieldHeader fieldHeader, final String fieldName,
            final DataType requireDataType) {
        if (fieldHeader.getFieldName().equals(fieldName)) {
            if (!fieldHeader.getDataType().equals(requireDataType)) {
                throw new InputMismatchException(
                        fieldName + " is type " + fieldHeader.getDataType() + " vs. required type " + requireDataType);
            }
            return true;
        }
        return false;
    }

    /**
     * Read a Dataset from a byte array containing comma separated values.<br>
     * The data format is a custom extension of csv with an additional
     * #-commented Metadata Header and a $-commented column header. Expects the
     * following columns in this order to be present: index, x, y, eyn, eyp.
     *
     * @param readBuffer FastByteBuffer (encapsulates byte array).
     * @return DataSet with the data and metadata read from the file
     */
    public static DataSet readDataSetFromByteArray(final FastByteBuffer readBuffer) {
        DataSetBuilder builder = new DataSetBuilder();

        HeaderInfo bufferHeader = BinarySerialiser.checkHeaderInfo(readBuffer);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("read header = %s{}", bufferHeader);
        }

        FieldHeader fieldHeader;
        for (; (fieldHeader = BinarySerialiser.getFieldHeader(readBuffer)) != null;) {
            // read standard values
            // read strings
            if (checkFieldCompatibility(fieldHeader, DATA_SET_NAME, DataType.STRING)) {
                builder.setName(BinarySerialiser.getString(readBuffer));
                continue;
            }

            // read doubles
            if (checkFieldCompatibility(fieldHeader, X_MIN, DataType.DOUBLE)) {
                builder.setXMin(BinarySerialiser.getDouble(readBuffer));
                continue;
            }

            if (checkFieldCompatibility(fieldHeader, X_MAX, DataType.DOUBLE)) {
                builder.setXMax(BinarySerialiser.getDouble(readBuffer));
                continue;
            }

            if (checkFieldCompatibility(fieldHeader, Y_MIN, DataType.DOUBLE)) {
                builder.setYMin(BinarySerialiser.getDouble(readBuffer));
                continue;
            }

            if (checkFieldCompatibility(fieldHeader, Y_MAX, DataType.DOUBLE)) {
                builder.setYMax(BinarySerialiser.getDouble(readBuffer));
                continue;
            }

            if (checkFieldCompatibility(fieldHeader, VAL_INTEGRAL, DataType.DOUBLE)) {
                BinarySerialiser.getDouble(readBuffer); // not used for the
                                                        // moment
                continue;
            }

            if (checkFieldCompatibility(fieldHeader, VAL_MEAN, DataType.DOUBLE)) {
                BinarySerialiser.getDouble(readBuffer); // not used for the
                                                        // moment
                continue;
            }

            if (checkFieldCompatibility(fieldHeader, VAL_RMS, DataType.DOUBLE)) {
                BinarySerialiser.getDouble(readBuffer); // not used for the
                                                        // moment
                continue;
            }

            if (checkFieldCompatibility(fieldHeader, INFO_LIST, DataType.STRING_ARRAY)) {
                builder.setMetaInfoList(BinarySerialiser.getStringArray(readBuffer));
                continue;
            }

            if (checkFieldCompatibility(fieldHeader, WARNING_LIST, DataType.STRING_ARRAY)) {
                builder.setMetaWarningList(BinarySerialiser.getStringArray(readBuffer));
                continue;
            }

            if (checkFieldCompatibility(fieldHeader, ERROR_LIST, DataType.STRING_ARRAY)) {
                builder.setMetaErrorList(BinarySerialiser.getStringArray(readBuffer));
                continue;
            }

            if (checkFieldCompatibility(fieldHeader, META_INFO, DataType.MAP)) {
                Map<String, String> map = new ConcurrentHashMap<>();
                map = BinarySerialiser.getMap(readBuffer, map);
                builder.setMetaInfoMap(map);
                continue;
            }

            if (checkFieldCompatibility(fieldHeader, DATA_LABELS, DataType.MAP)) {
                Map<Integer, String> map = new ConcurrentHashMap<>();
                map = BinarySerialiser.getMap(readBuffer, map);
                builder.setDataLabelMap(map);
                continue;
            }

            if (checkFieldCompatibility(fieldHeader, DATA_STYLES, DataType.MAP)) {
                Map<Integer, String> map = new ConcurrentHashMap<>();
                map = BinarySerialiser.getMap(readBuffer, map);
                builder.setDataStyleMap(map);
                continue;
            }

            // read numeric data
            if (fieldHeader.getFieldName().equals(X_ARRAY_NAME)) {
                if (fieldHeader.getDataType().equals(DataType.DOUBLE_ARRAY)) {
                    builder.setXValues(BinarySerialiser.getDoubleArray(readBuffer));
                } else if (fieldHeader.getDataType().equals(DataType.FLOAT_ARRAY)) {
                    builder.setXValues(toDoubles(BinarySerialiser.getFloatArray(readBuffer)));
                } else {
                    throw new InputMismatchException(
                            "x is not a [double, float]_array but " + fieldHeader.getDataType());
                }
                continue;
            }
            if (fieldHeader.getFieldName().equals(Y_ARRAY_NAME)) {
                if (fieldHeader.getDataType().equals(DataType.DOUBLE_ARRAY)) {
                    builder.setYValues(BinarySerialiser.getDoubleArray(readBuffer));
                } else if (fieldHeader.getDataType().equals(DataType.FLOAT_ARRAY)) {
                    builder.setYValues(toDoubles(BinarySerialiser.getFloatArray(readBuffer)));
                } else {
                    throw new InputMismatchException(
                            "y is not a [double, float]_array but " + fieldHeader.getDataType());
                }
                continue;
            }
            if (fieldHeader.getFieldName().equals(XEN)) {
                if (fieldHeader.getDataType().equals(DataType.DOUBLE_ARRAY)) {
                    builder.setXNegErrorNoCopy(BinarySerialiser.getDoubleArray(readBuffer));
                } else if (fieldHeader.getDataType().equals(DataType.FLOAT_ARRAY)) {
                    builder.setXNegErrorNoCopy(toDoubles(BinarySerialiser.getFloatArray(readBuffer)));
                } else {
                    throw new InputMismatchException(
                            "xen is not a [double, float]_array but " + fieldHeader.getDataType());
                }
                continue;
            }
            if (fieldHeader.getFieldName().equals(XEP)) {
                if (fieldHeader.getDataType().equals(DataType.DOUBLE_ARRAY)) {
                    builder.setXPosErrorNoCopy(BinarySerialiser.getDoubleArray(readBuffer));
                } else if (fieldHeader.getDataType().equals(DataType.FLOAT_ARRAY)) {
                    builder.setXPosErrorNoCopy(toDoubles(BinarySerialiser.getFloatArray(readBuffer)));
                } else {
                    throw new InputMismatchException(
                            "xep is not a [double, float]_array but " + fieldHeader.getDataType());
                }
                continue;
            }
            if (fieldHeader.getFieldName().equals(YEN)) {
                if (fieldHeader.getDataType().equals(DataType.DOUBLE_ARRAY)) {
                    builder.setYNegErrorNoCopy(BinarySerialiser.getDoubleArray(readBuffer));
                } else if (fieldHeader.getDataType().equals(DataType.FLOAT_ARRAY)) {
                    builder.setYNegErrorNoCopy(toDoubles(BinarySerialiser.getFloatArray(readBuffer)));
                } else {
                    throw new InputMismatchException(
                            "yen is not a [double, float]_array but " + fieldHeader.getDataType());
                }
                continue;
            }
            if (fieldHeader.getFieldName().equals(YEP)) {
                if (fieldHeader.getDataType().equals(DataType.DOUBLE_ARRAY)) {
                    builder.setYPosErrorNoCopy(BinarySerialiser.getDoubleArray(readBuffer));
                } else if (fieldHeader.getDataType().equals(DataType.FLOAT_ARRAY)) {
                    builder.setYPosErrorNoCopy(toDoubles(BinarySerialiser.getFloatArray(readBuffer)));
                } else {
                    throw new InputMismatchException(
                            "yep is not a [double, float]_array but " + fieldHeader.getDataType());
                }
                continue;
            }

            // swallow rest
            swallowRest(readBuffer, fieldHeader);
        }
        // parsed until end of buffer

        return builder.build();
    }

    protected static void swallowRest(final FastByteBuffer readBuffer, final FieldHeader fieldHeader) {
        // parse whatever is left
        // N.B. this is/should be the only place where 'Object' is used
        // since the JVM will perform boxing of primitive types
        // automatically. Boxing and later un-boxing is a significant
        // high-performance bottleneck for any serialiser
        Object leftOver = null;
        switch (fieldHeader.getDataType()) {
        case BOOL:
            leftOver = BinarySerialiser.getBoolean(readBuffer);
            break;
        case BYTE:
            leftOver = BinarySerialiser.getByte(readBuffer);
            break;
        case SHORT:
            leftOver = BinarySerialiser.getShort(readBuffer);
            break;
        case INT:
            leftOver = BinarySerialiser.getInteger(readBuffer);
            break;
        case LONG:
            leftOver = BinarySerialiser.getLong(readBuffer);
            break;
        case FLOAT:
            leftOver = BinarySerialiser.getFloat(readBuffer);
            break;
        case DOUBLE:
            leftOver = BinarySerialiser.getDouble(readBuffer);
            break;
        case STRING:
            leftOver = BinarySerialiser.getString(readBuffer);
            break;
        case BOOL_ARRAY:
            leftOver = BinarySerialiser.getBooleanArray(readBuffer);
            break;
        case BYTE_ARRAY:
            leftOver = BinarySerialiser.getByteArray(readBuffer);
            break;
        case SHORT_ARRAY:
            leftOver = BinarySerialiser.getShortArray(readBuffer);
            break;
        case INT_ARRAY:
            leftOver = BinarySerialiser.getIntArray(readBuffer);
            break;
        case LONG_ARRAY:
            leftOver = BinarySerialiser.getLongArray(readBuffer);
            break;
        case FLOAT_ARRAY:
            leftOver = BinarySerialiser.getFloatArray(readBuffer);
            break;
        case DOUBLE_ARRAY:
            leftOver = BinarySerialiser.getDoubleArray(readBuffer);
            break;
        case STRING_ARRAY:
            leftOver = BinarySerialiser.getStringArray(readBuffer);
            break;
        case MAP:
            leftOver = BinarySerialiser.getMap(readBuffer, new ConcurrentHashMap<Object, Object>());
            break;
        case END_MARKER:
            byte endMarker = BinarySerialiser.getByte(readBuffer);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("swallowed '{}'='{}'", fieldHeader, Byte.toString(endMarker));
            }
            break;
        default:
            throw new IllegalArgumentException("encountered unknown format for " + fieldHeader.toString());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("swallowed unused element '%s{}'='%s{}'\n", fieldHeader, leftOver);
        }
    }

    protected static float[] toFloats(final double[] input) {
        float[] floatArray = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            floatArray[i] = (float) input[i];
        }
        return floatArray;
    }

    protected static double[] toDoubles(final float[] input) {
        double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            doubleArray[i] = (double) input[i];
        }
        return doubleArray;
    }
}
