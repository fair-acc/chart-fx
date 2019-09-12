package de.gsi.dataset.utils;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser.HeaderInfo;
import de.gsi.dataset.serializer.spi.FieldHeader;
import de.gsi.dataset.spi.DataSetBuilder;

/**
 * Class to efficiently serialise and de-serialise DataSet objects into binary
 * byte arrays. The performance can be tuned through:
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
	private static final String VAL_RMS = "rms";
	private static final String VAL_MEAN = "mean";
	private static final String VAL_INTEGRAL = "integral";
	private static boolean transmitDataLabels = true;
	private static boolean transmitMetaData = true;

	protected DataSetSerialiser() {
		super();
		// utility class
	}

	protected static Optional<FieldHeader> checkFieldCompatibility(final IoBuffer buffer,
			final List<FieldHeader> fieldHeaderList, final String fieldName, final DataType... requireDataTypes) {
		Optional<FieldHeader> fieldHeader = FieldHeader.findHeaderFor(fieldHeaderList, fieldName);
		if (!fieldHeader.isPresent()) {
			return Optional.empty();
		}

		if (fieldHeader.get().getFieldName().equals(fieldName)) {
			boolean foundMatchingDataType = false;
			for (DataType dataType : requireDataTypes) {
				if (fieldHeader.get().getDataType().equals(dataType)) {
					foundMatchingDataType = true;
					break;
				}
			}
			if (!foundMatchingDataType) {
				throw new InputMismatchException(fieldName + " is type " + fieldHeader.get().getDataType()
						+ " vs. required type " + requireDataTypes);
			}

			final long dataPosition = fieldHeader.get().getDataBufferPosition();
			buffer.position(dataPosition);
			return fieldHeader;
		}
		return Optional.empty();
	}

	public static boolean isDataLablesSerialised() {
		return transmitDataLabels;
	}

	public static boolean isMetaDataSerialised() {
		return transmitMetaData;
	}

	protected static void parseDataLabels(final IoBuffer readBuffer, final DataSetBuilder builder,
			final List<FieldHeader> fieldHeaderList) {
		if (checkFieldCompatibility(readBuffer, fieldHeaderList, DATA_LABELS, DataType.MAP).isPresent()) {
			Map<Integer, String> map = new ConcurrentHashMap<>();
			map = BinarySerialiser.getMap(readBuffer, map);
			builder.setDataLabelMap(map);
		}

		if (checkFieldCompatibility(readBuffer, fieldHeaderList, DATA_STYLES, DataType.MAP).isPresent()) {
			Map<Integer, String> map = new ConcurrentHashMap<>();
			map = BinarySerialiser.getMap(readBuffer, map);
			builder.setDataStyleMap(map);
		}
	}

	protected static void parseHeader(final IoBuffer readBuffer, final DataSetBuilder builder,
			final List<FieldHeader> fieldHeaderList) {
		// read strings
		if (checkFieldCompatibility(readBuffer, fieldHeaderList, DATA_SET_NAME, DataType.STRING).isPresent()) {
			builder.setName(BinarySerialiser.getString(readBuffer));
		}

		// check for axis descriptions
		// TODO: match any field variable starting with 'axis<int>.XXXX'
		for (FieldHeader fieldHeader : fieldHeaderList) {
			final String fieldName = fieldHeader.getFieldName();
			if (fieldName == null || !fieldName.startsWith(AXIS)) {
				continue;
			}
			final String[] parsed = fieldName.split("\\.");
			if (parsed.length <= 1) {
				// couldn't parse
				continue;
			}
			final int dimension;
			try {
				dimension = Integer.parseInt(parsed[0].substring(AXIS.length()));
			} catch (NumberFormatException e) {
				if (LOGGER.isErrorEnabled()) {
					LOGGER.atError().setCause(e).addArgument(parsed[0]).addArgument(fieldName)
							.log("could not parse sub-string {} of {} for integer");
				}
				continue;
			}
			readBuffer.position(fieldHeader.getDataBufferPosition());
			if (parsed[1].equals(MIN)) {
				builder.setAxisMin(dimension, BinarySerialiser.getDouble(readBuffer));
			} else if (parsed[1].equals(MAX)) {
				builder.setAxisMax(dimension, BinarySerialiser.getDouble(readBuffer));
			} else if (parsed[1].equals(NAME)) {
				builder.setAxisName(dimension, BinarySerialiser.getString(readBuffer));
			} else if (parsed[1].equals(UNIT)) {
				builder.setAxisUnit(dimension, BinarySerialiser.getString(readBuffer));
			}
		}

		// if (checkFieldCompatibility(readBuffer, fieldHeaderList, VAL_INTEGRAL,
		// DataType.DOUBLE).isPresent()) {
		// BinarySerialiser.getDouble(readBuffer); // not used for the moment
		// }
		//
		// if (checkFieldCompatibility(readBuffer, fieldHeaderList, VAL_MEAN,
		// DataType.DOUBLE).isPresent()) {
		// BinarySerialiser.getDouble(readBuffer); // not used for the moment
		// }
		//
		// if (checkFieldCompatibility(readBuffer, fieldHeaderList, VAL_RMS,
		// DataType.DOUBLE).isPresent()) {
		// BinarySerialiser.getDouble(readBuffer); // not used for the moment
		// }
	}

	protected static void parseMetaData(final IoBuffer readBuffer, final DataSetBuilder builder,
			final List<FieldHeader> fieldHeaderList) {
		if (checkFieldCompatibility(readBuffer, fieldHeaderList, INFO_LIST, DataType.STRING_ARRAY).isPresent()) {
			builder.setMetaInfoList(BinarySerialiser.getStringArray(readBuffer));
		}

		if (checkFieldCompatibility(readBuffer, fieldHeaderList, WARNING_LIST, DataType.STRING_ARRAY).isPresent()) {
			builder.setMetaWarningList(BinarySerialiser.getStringArray(readBuffer));
		}

		if (checkFieldCompatibility(readBuffer, fieldHeaderList, ERROR_LIST, DataType.STRING_ARRAY).isPresent()) {
			builder.setMetaErrorList(BinarySerialiser.getStringArray(readBuffer));
		}

		if (checkFieldCompatibility(readBuffer, fieldHeaderList, META_INFO, DataType.MAP).isPresent()) {
			Map<String, String> map = new ConcurrentHashMap<>();
			map = BinarySerialiser.getMap(readBuffer, map);
			builder.setMetaInfoMap(map);
		}
	}

	protected static void parseNumericData(final IoBuffer readBuffer, final DataSetBuilder builder,
			final List<FieldHeader> fieldHeaderList) {
		// read numeric data

		Optional<FieldHeader> header;

		header = checkFieldCompatibility(readBuffer, fieldHeaderList, X_ARRAY_NAME, DataType.DOUBLE_ARRAY,
				DataType.FLOAT_ARRAY);
		if (header.isPresent()) {
			builder.setXValues(BinarySerialiser.getDoubleArray(readBuffer, header.get().getDataType()));
		}

		header = checkFieldCompatibility(readBuffer, fieldHeaderList, Y_ARRAY_NAME, DataType.DOUBLE_ARRAY,
				DataType.FLOAT_ARRAY);
		if (header.isPresent()) {
			builder.setYValues(BinarySerialiser.getDoubleArray(readBuffer, header.get().getDataType()));
		}

		header = checkFieldCompatibility(readBuffer, fieldHeaderList, XEN, DataType.DOUBLE_ARRAY, DataType.FLOAT_ARRAY);
		if (header.isPresent()) {
			builder.setXNegErrorNoCopy(BinarySerialiser.getDoubleArray(readBuffer, header.get().getDataType()));
		}

		header = checkFieldCompatibility(readBuffer, fieldHeaderList, XEP, DataType.DOUBLE_ARRAY, DataType.FLOAT_ARRAY);
		if (header.isPresent()) {
			builder.setXPosErrorNoCopy(BinarySerialiser.getDoubleArray(readBuffer, header.get().getDataType()));
		}

		header = checkFieldCompatibility(readBuffer, fieldHeaderList, YEN, DataType.DOUBLE_ARRAY, DataType.FLOAT_ARRAY);
		if (header.isPresent()) {
			builder.setYNegErrorNoCopy(BinarySerialiser.getDoubleArray(readBuffer, header.get().getDataType()));
		}

		header = checkFieldCompatibility(readBuffer, fieldHeaderList, YEP, DataType.DOUBLE_ARRAY, DataType.FLOAT_ARRAY);
		if (header.isPresent()) {
			builder.setYPosErrorNoCopy(BinarySerialiser.getDoubleArray(readBuffer, header.get().getDataType()));
		}
	}

	/**
	 * Read a Dataset from a byte array containing comma separated values.<br>
	 * The data format is a custom extension of csv with an additional #-commented
	 * Metadata Header and a $-commented column header. Expects the following
	 * columns in this order to be present: index, x, y, eyn, eyp.
	 *
	 * @param readBuffer IoBuffer (encapsulates byte array).
	 * @return DataSet with the data and metadata read from the file
	 */
	public static DataSet readDataSetFromByteArray(final IoBuffer readBuffer) { // NOPMD
		final DataSetBuilder builder = new DataSetBuilder();

		final HeaderInfo bufferHeader = BinarySerialiser.checkHeaderInfo(readBuffer);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.atDebug().addArgument(bufferHeader).log("read header = {}");
		}
		final List<FieldHeader> fieldHeaderList = new ArrayList<>();

		FieldHeader fieldHeader;
		for (; (fieldHeader = BinarySerialiser.getFieldHeader(readBuffer)) != null;) {
			fieldHeaderList.add(fieldHeader);

			final long bytesToSkip = fieldHeader.getExpectedNumberOfDataBytes();
			final long skipPosition = readBuffer.position() + bytesToSkip;

			if (bytesToSkip < 0) {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.atWarn().addArgument(fieldHeader.getFieldName()).addArgument(fieldHeader.getDataType())
							.addArgument(bytesToSkip).log("FieldHeader for '{}' type '{}' has bytesToSkip '{} <= 0'");
				}

				// fall-back option in case of
				swallowRest(readBuffer, fieldHeader);
			} else {
				readBuffer.position(skipPosition);
			}
		}
		// parsed until end of buffer
		parseHeader(readBuffer, builder, fieldHeaderList);

		if (isMetaDataSerialised()) {
			parseMetaData(readBuffer, builder, fieldHeaderList);
		}

		if (isDataLablesSerialised()) {
			parseDataLabels(readBuffer, builder, fieldHeaderList);
		}

		parseNumericData(readBuffer, builder, fieldHeaderList);

		return builder.build();
	}

	public static void setDataLablesSerialised(final boolean state) {
		transmitDataLabels = state;
	}

	public static void setMetaDataSerialised(final boolean state) {
		transmitMetaData = state;
	}

	protected static void swallowRest(final IoBuffer readBuffer, final FieldHeader fieldHeader) {
		// parse whatever is left
		// N.B. this is/should be the only place where 'Object' is used
		// since the JVM will perform boxing of primitive types
		// automatically. Boxing and later un-boxing is a significant
		// high-performance bottleneck for any serialiser
		Object leftOver = null;
		int size = -1;
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
			size = readBuffer.getInt();
			leftOver = BinarySerialiser.getBooleanArray(readBuffer);
			break;
		case BYTE_ARRAY:
			size = readBuffer.getInt();
			leftOver = BinarySerialiser.getByteArray(readBuffer);
			break;
		case SHORT_ARRAY:
			size = readBuffer.getInt();
			leftOver = BinarySerialiser.getShortArray(readBuffer);
			break;
		case INT_ARRAY:
			size = readBuffer.getInt();
			leftOver = BinarySerialiser.getIntArray(readBuffer);
			break;
		case LONG_ARRAY:
			size = readBuffer.getInt();
			leftOver = BinarySerialiser.getLongArray(readBuffer);
			break;
		case FLOAT_ARRAY:
			size = readBuffer.getInt();
			leftOver = BinarySerialiser.getFloatArray(readBuffer);
			break;
		case DOUBLE_ARRAY:
			size = readBuffer.getInt();
			leftOver = BinarySerialiser.getDoubleArray(readBuffer);
			break;
		case STRING_ARRAY:
			size = readBuffer.getInt();
			leftOver = BinarySerialiser.getStringArray(readBuffer);
			break;
		case MAP:
			size = readBuffer.getInt();
			leftOver = BinarySerialiser.getMap(readBuffer, new ConcurrentHashMap<>());
			break;
		case END_MARKER:
			final byte endMarker = BinarySerialiser.getByte(readBuffer);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.atDebug().addArgument(fieldHeader).addArgument(Byte.toString(endMarker))
						.log("swallowed '{}'='{}'");
			}
			break;
		default:
			throw new IllegalArgumentException("encountered unknown format for " + fieldHeader.toString());
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.atDebug().addArgument(fieldHeader).addArgument(leftOver).addArgument(size)
					.log("swallowed unused element '%s{}'='%s{}' size = {}");
		}
	}

	private static float[] toFloats(final double[] input) {
		final float[] floatArray = new float[input.length];
		for (int i = 0; i < input.length; i++) {
			floatArray[i] = (float) input[i];
		}
		return floatArray;
	}

	protected static void writeDataLabelsToStream(final IoBuffer buffer, final DataSet dataSet) {
		final int dataCount = dataSet.getDataCount();
		final Map<Integer, String> labelMap = new ConcurrentHashMap<>();
		for (int index = 0; index < dataCount; index++) {
			final String label = dataSet.getDataLabel(index);
			if ((label != null) && !label.isEmpty()) {
				labelMap.put(index, label);
			}
		}
		if (!labelMap.isEmpty()) {
			BinarySerialiser.put(buffer, DATA_LABELS, labelMap);
		}

		final Map<Integer, String> styleMap = new ConcurrentHashMap<>();
		for (int index = 0; index < dataCount; index++) {
			final String style = dataSet.getStyle(index);
			if ((style != null) && !style.isEmpty()) {
				styleMap.put(index, style);
			}
		}
		if (!styleMap.isEmpty()) {
			BinarySerialiser.put(buffer, DATA_STYLES, styleMap);
		}
	}

	/**
	 * Write data set into byte buffer.
	 *
	 * @param dataSet The DataSet to export
	 * @param buffer  byte output buffer (N.B. keep caching this object)
	 * @param asFloat {@code true}: encode data as binary floats (smaller size,
	 *                performance), or {@code false} as double (better precision)
	 */
	public static void writeDataSetToByteArray(final DataSet dataSet, final IoBuffer buffer, final boolean asFloat) {
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

	protected static void writeHeaderDataToStream(final IoBuffer buffer, final DataSet dataSet) {
		// common header data
		BinarySerialiser.put(buffer, DATA_SET_NAME, dataSet.getName());
		final List<AxisDescription> axisDescriptions = dataSet.getAxisDescriptions();
		StringBuilder builder = new StringBuilder(60);
		for (int i = 0; i < axisDescriptions.size(); i++) {
			builder.setLength(0);
			final String prefix = builder.append(AXIS).append(Integer.toString(i)).append('.').toString();
			builder.setLength(0);
			final String name = builder.append(prefix).append(NAME).toString();
			builder.setLength(0);
			final String unit = builder.append(prefix).append(UNIT).toString();
			builder.setLength(0);
			final String minName = builder.append(prefix).append(MIN).toString();
			builder.setLength(0);
			final String maxName = builder.append(prefix).append(MAX).toString();

			BinarySerialiser.put(buffer, name, dataSet.getAxisDescription(i).getName());
			BinarySerialiser.put(buffer, unit, dataSet.getAxisDescription(i).getUnit());
			BinarySerialiser.put(buffer, minName, dataSet.getAxisDescription(i).getMin());
			BinarySerialiser.put(buffer, maxName, dataSet.getAxisDescription(i).getMax());
		}

		// write some statistics for the human readable benefit when
		// opening the
		// file with standard text-based viewers
		BinarySerialiser.put(buffer, VAL_INTEGRAL, integralSimple(dataSet));
		BinarySerialiser.put(buffer, VAL_MEAN, mean(dataSet.getYValues()));
		BinarySerialiser.put(buffer, VAL_RMS, rootMeanSquare(dataSet.getYValues()));
	}

	protected static void writeMetaDataToStream(final IoBuffer buffer, final DataSet dataSet) {
		if (!(dataSet instanceof DataSetMetaData)) {
			return;
		}
		final DataSetMetaData metaDataSet = (DataSetMetaData) dataSet;

		BinarySerialiser.put(buffer, INFO_LIST, metaDataSet.getInfoList().toArray(new String[0]));
		BinarySerialiser.put(buffer, WARNING_LIST, metaDataSet.getWarningList().toArray(new String[0]));
		BinarySerialiser.put(buffer, ERROR_LIST, metaDataSet.getErrorList().toArray(new String[0]));
		BinarySerialiser.put(buffer, META_INFO, metaDataSet.getMetaInfo());
	}

	/**
	 * @param buffer  IoBuffer to write binary data into
	 * @param dataSet to be exported
	 * @param asFloat {@code true} use 32-bit floats (less memory, faster transfer)
	 *                instead of 64-bit doubles (DataSet default, higher precision)
	 */
	protected static void writeNumericBinaryDataToBuffer(final IoBuffer buffer, final DataSet dataSet,
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
}
