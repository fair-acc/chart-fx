package de.gsi.dataset.samples;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.DataSetError.ErrorType;
import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.testdata.spi.RandomDataGenerator;
import de.gsi.dataset.utils.DataSetUtils;
import de.gsi.dataset.utils.ProcessingProfiler;
import de.gsi.serializer.IoClassSerialiser;
import de.gsi.serializer.IoSerialiser;
import de.gsi.serializer.spi.BinarySerialiser;
import de.gsi.serializer.spi.FastByteBuffer;
import de.gsi.serializer.spi.iobuffer.DataSetSerialiser;
import de.gsi.serializer.utils.ClassUtils;

public class DataSetToByteArraySample {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetToByteArraySample.class);
    private static final int N_SAMPLES = 10_000; // default: 10_000
    private final DoubleErrorDataSet original = new DoubleErrorDataSet("init", N_SAMPLES);
    private final ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    private final FastByteBuffer byteBuffer = new FastByteBuffer();
    private final IoSerialiser binarySerialiser = new BinarySerialiser(byteBuffer);
    private final DataSetSerialiser dataSetSerialiser = DataSetSerialiser.withIoSerialiser(binarySerialiser);

    public DataSetToByteArraySample() {
        LOGGER.atInfo().log(DataSetToByteArraySample.class.getSimpleName() + " - init");
        generateData(original);
        LOGGER.atInfo().log(DataSetToByteArraySample.class.getSimpleName() + " - generated data");

        DataSetUtils.writeDataSetToByteArray(original, byteOutput, false, false);
        LOGGER.atInfo().addArgument(encodingBits(false)).addArgument(encodingBinary(false)).addArgument(humanReadableByteCount(byteOutput.size(), true)).log("byte buffer array length with {} {} encoding = {}");

        DataSetUtils.writeDataSetToByteArray(original, byteOutput, true, true);
        LOGGER.atInfo().addArgument(encodingBits(true)).addArgument(encodingBinary(true)).addArgument(humanReadableByteCount(byteOutput.size(), true)).log("byte buffer array length with {} {} encoding =  = {}");

        DataSetUtils.writeDataSetToByteArray(original, byteOutput, true, false);
        LOGGER.atInfo().addArgument(encodingBits(false)).addArgument(encodingBinary(true)).addArgument(humanReadableByteCount(byteOutput.size(), true)).log("byte buffer array length with {} {} encoding =  = {}");

        byteBuffer.ensureCapacity(byteOutput.size() + 1000);
    }

    public void clearGarbage() {
        // N.B. call gc to allow for light-weight performance comparisons... real benchmarking should be done with e.g. JMH
        System.gc(); //NOSONAR //NOPMD
        System.gc(); //NOSONAR //NOPMD
        LOGGER.atInfo().log("");
    }

    public void testDataSetSerialiserIdentity(final boolean withMetaData, final boolean asFloat32) {
        dataSetSerialiser.setMetaDataSerialised(true);
        dataSetSerialiser.setDataLablesSerialised(true);

        byteBuffer.reset(); // '0' writing at start of buffer
        dataSetSerialiser.write(original, asFloat32);
        byteBuffer.reset(); // reset to read position (==0)
        final DoubleErrorDataSet dataSet = (DoubleErrorDataSet) dataSetSerialiser.read();

        testIdentityCore(true, asFloat32, original, dataSet);
        testIdentityLabelsAndStyles(true, original, dataSet);
        testIdentityMetaData(true, original, dataSet);

        LOGGER.atInfo().addArgument(encodingBits(asFloat32)).addArgument(encodingBinary(true)).addArgument(withMetaData ? "with" : "w/o").log("testDataSetSerialiserIdentity passed for {} {} encoding {} meta-data");
    }

    public void testDataSetUtilsIdentity(final boolean binary, final boolean asFloat32) {
        DataSetUtils.writeDataSetToByteArray(original, byteOutput, binary, asFloat32);
        final DataSet dataSet = DataSetUtils.readDataSetFromByteArray(byteOutput.toByteArray());

        if (!(dataSet instanceof DoubleErrorDataSet)) {
            throw new IllegalStateException(
                    "DataSet is not not instanceof DoubleErrorDataSet, might be DataSet3D or DoubleDataSet");
        }

        testIdentityCore(binary, asFloat32, original, (DoubleErrorDataSet) dataSet);
        // following is not (yet) implemented in DataSetUtils
        // testIdentityLabelsAndStyles(true, asFloat32, original, dataSet);
        testIdentityMetaData(binary, original, (DataSetMetaData) dataSet);

        LOGGER.atInfo().addArgument(encodingBits(asFloat32)).addArgument(encodingBinary(binary)).log("testDataSetUtilsIdentity passed for {} {} encoding with partial meta-data");
    }

    public void testGenericSerialiserIdentity(final boolean asFloat32) {
        IoClassSerialiser serialiser = new IoClassSerialiser(byteBuffer, BinarySerialiser.class);
        DataSetWrapper dsOrig = new DataSetWrapper();
        dsOrig.source = original;
        DataSetWrapper cpOrig = new DataSetWrapper();

        byteBuffer.reset(); // '0' writing at start of buffer
        serialiser.serialiseObject(dsOrig);
        byteBuffer.reset(); // reset to read position (==0)
        final Object retOrig = serialiser.deserialiseObject(cpOrig);

        if (cpOrig != retOrig) {
            throw new IllegalStateException("Deserialisation should be in-place");
        }

        if (!(cpOrig.source instanceof DoubleErrorDataSet)) {
            throw new IllegalStateException(
                    "DataSet is not not instanceof DoubleErrorDataSet, might be DataSet3D or DoubleDataSet");
        }

        testIdentityCore(true, asFloat32, original, (DoubleErrorDataSet) cpOrig.source);
        testIdentityLabelsAndStyles(true, original, cpOrig.source);

        if (cpOrig.source instanceof DataSetMetaData) {
            testIdentityMetaData(true, original, (DataSetMetaData) cpOrig.source);
        }

        LOGGER.atInfo().addArgument(encodingBits(asFloat32)).addArgument(encodingBinary(true)).log("testGenericSerialiserIdentity passed for {} {} encoding with partial meta-data");
    }

    public void testGenericSerializerPerformance(final int iterations, final boolean asFloat32) {
        IoClassSerialiser serialiser = new IoClassSerialiser(byteBuffer, BinarySerialiser.class);
        byteBuffer.reset(); // reset to read position (==0)
        final DoubleErrorDataSet copy = new DoubleErrorDataSet("init", N_SAMPLES);
        copy.setErrorType(DIM_X, ErrorType.NO_ERROR);
        copy.setErrorType(DIM_Y, ErrorType.NO_ERROR);

        DataSetWrapper dsOrig = new DataSetWrapper();
        dsOrig.source = original;
        DataSetWrapper cpOrig = new DataSetWrapper();
        cpOrig.source = new DoubleErrorDataSet("test", 10);

        binarySerialiser.setPutFieldMetaData(true);
        final long startTime = ProcessingProfiler.getTimeStamp();
        for (int i = 0; i < iterations; i++) {
            if (i == 1) {
                binarySerialiser.setPutFieldMetaData(false);
            }
            original.setName("data set name " + System.currentTimeMillis());
            byteBuffer.reset(); // '0' writing at start of buffer
            serialiser.serialiseObject(dsOrig);

            byteBuffer.reset(); // reset to read position (==0)
            final Object retOrig = serialiser.deserialiseObject(cpOrig);

            if (retOrig != cpOrig) {
                throw new IllegalStateException("Deserialisation should be in-place");
            }

            if (!dsOrig.source.getName().equals(cpOrig.source.getName())) {
                LOGGER.atError().addArgument(dsOrig.source.getName()).addArgument(cpOrig.source.getName()).log("ERROR DataSet '{}' does not match '{}' -> potential streaming error at index = " + i);
                if (!dsOrig.source.equals(cpOrig.source)) {
                    throw new IllegalStateException("DataSet mismatch");
                }
                break;
            }
        }

        final long stopTime = ProcessingProfiler.getTimeDiff(startTime, "testGenericSerializerPerformance");

        binarySerialiser.setPutFieldMetaData(true);
        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteBuffer.position() / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(encodingBits(asFloat32)) //
                .addArgument(encodingBinary(true))
                .addArgument("with")
                .addArgument(humanReadableByteCount((long) byteCount, true))
                .addArgument(diffMillis / (double) iterations)
                .log("average {} {} IoClassSerialiser throughput {} meta infos = {}/s - time: {} ms per DataSet round-trip");
    }

    public void testIdentityCore(final boolean binary, final boolean asFloat32, final DataSetError originalDS,
            final DataSetError testDS) {
        // some checks
        if (originalDS.getDataCount() != testDS.getDataCount()) {
            throw new IllegalStateException("data set counts do not match (" + encodingBinary(binary) + "): original = "
                                            + originalDS.getDataCount() + " vs. copy = " + testDS.getDataCount());
        }

        if (!originalDS.getName().equals(testDS.getName())) {
            throw new IllegalStateException("data set name do not match (" + encodingBinary(binary) + "): original = "
                                            + originalDS.getName() + " vs. copy = " + testDS.getName());
        }

        // check for numeric value
        for (int i = 0; i < originalDS.getDataCount(); i++) {
            final double x0 = originalDS.get(DIM_X, i);
            final double y0 = originalDS.get(DIM_Y, i);
            final double exn0 = originalDS.getErrorNegative(DIM_X, i);
            final double exp0 = originalDS.getErrorPositive(DIM_X, i);
            final double eyn0 = originalDS.getErrorNegative(DIM_Y, i);
            final double eyp0 = originalDS.getErrorPositive(DIM_Y, i);

            final double x1 = testDS.get(DIM_X, i);
            final double y1 = testDS.get(DIM_Y, i);
            final double exn1 = testDS.getErrorNegative(DIM_X, i);
            final double exp1 = testDS.getErrorPositive(DIM_X, i);
            final double eyn1 = testDS.getErrorNegative(DIM_Y, i);
            final double eyp1 = testDS.getErrorPositive(DIM_Y, i);

            if (asFloat32) {
                if (!floatIdentity(x0, x1) || !floatIdentity(y0, y1) || !floatIdentity(exn0, exn1)
                        || !floatIdentity(exp0, exp1) || !floatIdentity(eyn0, eyn1) || (eyp0 != eyp1)) {
                    String diff = String.format(
                            "(x=%e - %e, y=%e - %e, exn=%e - %e, exp=%e - %e, eyn=%e - %e, eyp=%e - %e)", x0, x1, y0,
                            y1, exn0, exn1, exp0, exp1, eyn0, eyn1, eyp0, eyp1);
                    String delta = String.format("(dx=%e, dy=%e, dexn=%e, dexp=%e, deyn=%e, deyp=%e)", x0 - x1, y0 - y1,
                            exn0 - exn1, exp0 - exp1, eyn0 - eyn1, eyp0 - eyp1);
                    String msg = String.format(
                            "data set values do not match (%s): original-copy = at index %d%n%s%n%s%n",
                            encodingBinary(binary), i, diff, delta);
                    throw new IllegalStateException(msg);
                }
            } else {
                if ((x0 != x1) || (y0 != y1) || (exn0 != exn1) || (exp0 != exp1) || (eyn0 != eyn1) || (eyp0 != eyp1)) {
                    String diff = String.format(
                            "(x=%e - %e, y=%e - %e, exn=%e - %e, exp=%e - %e, eyn=%e - %e, eyp=%e - %e)", x0, x1, y0,
                            y1, exn0, exn1, exp0, exp1, eyn0, eyn1, eyp0, eyp1);
                    String delta = String.format("(dx=%e, dy=%e, dexn=%e, dexp=%e, deyn=%e, deyp=%e)", x0 - x1, y0 - y1,
                            exn0 - exn1, exp0 - exp1, eyn0 - eyn1, eyp0 - eyp1);
                    String msg = String.format(
                            "data set values do not match (%s): original-copy = at index %d%n%s%n%s%n",
                            encodingBinary(binary), i, diff, delta);
                    throw new IllegalStateException(msg);
                }
            }
        }
    }

    public void testIdentityLabelsAndStyles(final boolean binary, final DataSet2D originalDS,
            final DataSet testDS) {
        // check for labels & styles
        for (int i = 0; i < originalDS.getDataCount(); i++) {
            if (originalDS.getDataLabel(i) == null && testDS.getDataLabel(i) == null) {
                // cannot compare null vs null
                continue;
            }
            if (!originalDS.getDataLabel(i).equals(testDS.getDataLabel(i))) {
                String msg = String.format("data set label do not match (%s): original(%d) ='%s' vs. copy(%d) ='%s' %n",
                        encodingBinary(binary), i, originalDS.getDataLabel(i), i, testDS.getDataLabel(i));
                throw new IllegalStateException(msg);
            }
        }
        for (int i = 0; i < originalDS.getDataCount(); i++) {
            if (originalDS.getStyle(i) == null && testDS.getStyle(i) == null) {
                // cannot compare null vs null
                continue;
            }
            if (!originalDS.getStyle(i).equals(testDS.getStyle(i))) {
                String msg = String.format("data set style do not match (%s): original(%d) ='%s' vs. copy(%d) ='%s' %n",
                        encodingBinary(binary), i, originalDS.getStyle(i), i, testDS.getStyle(i));
                throw new IllegalStateException(msg);
            }
        }
    }

    public void testIdentityMetaData(final boolean binary, final DataSetMetaData originalDS,
            final DataSetMetaData testDS) {
        // check for meta data and meta messages
        if (!originalDS.getInfoList().equals(testDS.getInfoList())) {
            String msg = String.format("data set info lists do not match (%s): original ='%s' vs. copy ='%s' %n",
                    encodingBinary(binary), originalDS.getInfoList(), testDS.getInfoList());
            throw new IllegalStateException(msg);
        }
    }

    public void testPerformance(final int iterations, final boolean withMetaInfos, final boolean binary,
            final boolean asFloat32) {
        final long startTime = ProcessingProfiler.getTimeStamp();
        DataSetUtils.setExportMetaDataByDefault(withMetaInfos);
        for (int i = 0; i < iterations; i++) {
            DataSetUtils.writeDataSetToByteArray(original, byteOutput, binary, asFloat32);
            final DataSet dataSet = DataSetUtils.readDataSetFromByteArray(byteOutput.toByteArray());
            if (dataSet == null || !original.getName().equals(dataSet.getName())) {
                LOGGER.atError().log("ERROR data set does not match -> potential streaming error at index = " + i);
                break;
            }
        }

        final long stopTime = ProcessingProfiler.getTimeDiff(startTime, "testPerformance(int, boolean, boolean)");
        DataSetUtils.setExportMetaDataByDefault(true);

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteOutput.size() / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(encodingBits(asFloat32)) //
                .addArgument(encodingBinary(binary))
                .addArgument(withMetaInfos ? "with" : "w/o")
                .addArgument(humanReadableByteCount((long) byteCount, true))
                .addArgument(diffMillis / (double) iterations)
                .log("average {} {} DataSetUtils throughput {} meta infos = {}/s - time: {} ms per DataSet round-trip");
    }

    public void testSerializerPerformance(final int iterations, final boolean withMetaInfos, final boolean asFloat32) {
        dataSetSerialiser.setMetaDataSerialised(withMetaInfos);
        dataSetSerialiser.setDataLablesSerialised(withMetaInfos);

        final long startTime = ProcessingProfiler.getTimeStamp();

        byteBuffer.reset(); // reset to read position (==0)
        for (int i = 0; i < iterations; i++) {
            byteBuffer.reset(); // '0' writing at start of buffer
            dataSetSerialiser.write(original, asFloat32);
            byteBuffer.reset(); // reset to read position (==0)
            final DataSet dataSet = dataSetSerialiser.read();

            if (!original.getName().equals(dataSet.getName())) {
                LOGGER.atError().log("ERROR data set does not match -> potential streaming error at index = " + i);
                break;
            }
        }

        final long stopTime = ProcessingProfiler.getTimeDiff(startTime, "testSerializerPerformance");

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteBuffer.position() / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(encodingBits(asFloat32)) //
                .addArgument(encodingBinary(true))
                .addArgument(withMetaInfos ? "with" : "w/o")
                .addArgument(humanReadableByteCount((long) byteCount, true))
                .addArgument(diffMillis / (double) iterations)
                .log("average {} {} DataSetSerialiser throughput {} meta infos = {}/s - time: {} ms per DataSet round-trip");
    }

    protected boolean floatIdentity(double a, double b) {
        // 32-bit float uses 23-bit for the mantissa
        return Math.abs((float) a - (float) b) <= 2 / Math.pow(2, 23);
    }

    private void performIdentityChecks() {
        // some identity checks
        for (boolean bit32 : new Boolean[] { true, false }) {
            for (boolean binary : new Boolean[] { false, true }) {
                testDataSetUtilsIdentity(binary, bit32);
            }
            testGenericSerialiserIdentity(bit32);
        }
        clearGarbage();
        for (boolean bit32 : new Boolean[] { true, false }) {
            for (boolean withMetaData : new Boolean[] { false, true }) {
                testDataSetSerialiserIdentity(withMetaData, bit32);
            }
        }
        clearGarbage();
    }

    public static String humanReadableByteCount(final long bytes, final boolean si) {
        final int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }

        final int exp = (int) (Math.log(bytes) / Math.log(unit));
        final String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static void main(final String[] args) {
        // for extra timing diagnostics
        ProcessingProfiler.setVerboseOutputState(false);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(true);

        final DataSetToByteArraySample sample = new DataSetToByteArraySample();

        sample.performIdentityChecks();

        // parse and print class structure
        ClassUtils.getFieldDescription(DoubleErrorDataSet.class).printFieldStructure();

        // string based performance
        // N.B. loops only once, since this serialiser is relatively slow (~30 MB/s)
        sample.testPerformance(100, true, false, true);
        sample.testPerformance(100, true, false, false);
        sample.clearGarbage();

        final int iterations = 4;
        final Consumer<BiConsumer<Boolean, Boolean>> loopFunction = func -> {
            for (boolean bit32 : new Boolean[] { true, false }) {
                for (boolean header : new Boolean[] { true, false }) {
                    for (int i = 0; i < iterations; i++) {
                        func.accept(bit32, header);
                    }
                    sample.clearGarbage();
                }
            }
        };

        final int nLoops = 2000;
        loopFunction.accept((bit32, header) -> sample.testPerformance(nLoops, header, true, bit32));
        loopFunction.accept((bit32, header) -> sample.testSerializerPerformance(nLoops, header, bit32));
        loopFunction.accept((bit32, header) -> sample.testGenericSerializerPerformance(nLoops, false));

        // infinite loop for performance tracking
        // enable if necessary sample.testSerializerPerformance(Integer.MAX_VALUE, true, false)
        // enable if necessary sample.testGenericSerializerPerformance(Integer.MAX_VALUE, false)
    }

    private static String encodingBinary(final boolean isBinaryEncoding) {
        return isBinaryEncoding ? "binary-based" : "string-based";
    }

    private static String encodingBits(final boolean is32BitEncoding) {
        return is32BitEncoding ? "32-bit" : "64-bit";
    }

    private static void generateData(final DoubleErrorDataSet dataSet) {
        final long startTime = ProcessingProfiler.getTimeStamp();

        dataSet.autoNotification().set(false);
        dataSet.clearData();
        dataSet.setName("data set name " + System.currentTimeMillis());
        double oldY = 0;
        for (int n = 0; n < N_SAMPLES; n++) {
            oldY += RandomDataGenerator.random() - 0.5;
            final double y = oldY;
            final double eyNeg = 0.1;
            final double eyPos = 10;
            dataSet.set(n, n, y, eyNeg, eyPos);

            if (n == 5000) {
                dataSet.getDataLabelMap().put(n, "special outlier");
                dataSet.getDataStyleMap().put(n, "-stroke-color=red");
            }
        }
        dataSet.getInfoList().add("standard info1");
        dataSet.getInfoList().add("standard info2");
        dataSet.getWarningList().add("standard warning");
        dataSet.getErrorList().add("standard error");
        dataSet.getMetaInfo().put("metaKey#1", "metaValue#1");
        dataSet.getMetaInfo().put("metaKey#2", "metaValue#2");
        dataSet.getMetaInfo().put("metaKey#3", "metaValue#3");

        dataSet.autoNotification().set(true);
        ProcessingProfiler.getTimeDiff(startTime, "generating data DataSet");
    }

    private class DataSetWrapper {
        private DataSet source;
    }
}
