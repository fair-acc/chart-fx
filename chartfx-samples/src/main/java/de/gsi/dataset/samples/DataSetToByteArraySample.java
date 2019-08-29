package de.gsi.dataset.samples;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.serializer.spi.FastByteBuffer;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.testdata.spi.RandomDataGenerator;
import de.gsi.dataset.utils.DataSetSerialiser;
import de.gsi.dataset.utils.DataSetUtils;
import de.gsi.dataset.utils.ProcessingProfiler;

public class DataSetToByteArraySample {
    private static final int N_SAMPLES = 100000; // default: 100000
    private final DoubleErrorDataSet original = new DoubleErrorDataSet("init", N_SAMPLES);
    private final ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    private final FastByteBuffer byteBuffer = new FastByteBuffer();

    public DataSetToByteArraySample() {
        System.err.println(DataSetToByteArraySample.class.getSimpleName() + " - init");
        generateData(original);
        System.err.println(DataSetToByteArraySample.class.getSimpleName() + " - generated data");

        DataSetUtils.writeDataSetToByteArray(original, byteOutput, false, false);
        System.err.printf("byte buffer array length with string encoding =%s%n",
                humanReadableByteCount(byteOutput.size(), true));

        DataSetUtils.writeDataSetToByteArray(original, byteOutput, true, true);
        System.err.printf("byte buffer array length with 32-bit binary encoding =  =%s%n",
                humanReadableByteCount(byteOutput.size(), true));

        DataSetUtils.writeDataSetToByteArray(original, byteOutput, true, false);
        System.err.printf("byte buffer array length with 64-bit binary encoding =  =%s%n",
                humanReadableByteCount(byteOutput.size(), true));

        byteBuffer.ensureCapacity(byteOutput.size() + 1000);
    }

    protected boolean floatIdentity(double a, double b) {
        // 32-bit float uses 23-bit for the mantissa 
        return Math.abs((float) a - (float) b) <= 2/Math.pow(2, 23);
    }

    public void testDataSetUtilsIdentity(final boolean binary, final boolean asFloat32) {
        DataSetUtils.writeDataSetToByteArray(original, byteOutput, binary, asFloat32);
        final DataSet dataSet = DataSetUtils.readDataSetFromByteArray(byteOutput.toByteArray());
        
        if (! (dataSet instanceof DoubleErrorDataSet)) {
            throw new IllegalStateException("DataSet is not not instanceof DoubleErrorDataSet, might be DataSet3D or DoubleDataSet");
        }

        testIdentityCore(binary, asFloat32, original, (DoubleErrorDataSet) dataSet);
        // N.B. following tests fail for DataSetUtils (missing functionality)
        // testIdentityLabelsAndStyles(binary, asFloat32, original, dataSet);
        // testIdentityMetaData(binary, asFloat32, original, dataSet);

        System.err.printf("testDataSetUtilsIdentity passed for %s-bit %s-based encoding with partial meta-data%n",
                asFloat32 ? "32" : "64", binary ? "binary" : "string");
    }

    public void testDataSetSerialiserIdentity(final boolean withMetaData, final boolean asFloat32) {
        DataSetSerialiser.setMetaDataSerialised(true);
        DataSetSerialiser.setDataLablesSerialised(false);

        byteBuffer.reset(); // '0' writing at start of buffer
        DataSetSerialiser.writeDataSetToByteArray(original, byteBuffer, asFloat32);
        byteBuffer.reset(); // reset to read position (==0)
        final DoubleErrorDataSet dataSet = (DoubleErrorDataSet) DataSetSerialiser.readDataSetFromByteArray(byteBuffer);

        testIdentityCore(true, asFloat32, original, dataSet);
        testIdentityLabelsAndStyles(true, asFloat32, original, dataSet);
        testIdentityMetaData(true, asFloat32, original, dataSet);

        System.err.printf("testDataSetSerialiserIdentity passed for %s-bit binary-based encoding %s meta-data%n",
                asFloat32 ? "32" : "64", withMetaData ? "with" : "w/o");
    }

    public void testIdentityCore(final boolean binary, final boolean asFloat32, final DoubleErrorDataSet originalDS,
            final DoubleErrorDataSet testDS) {
        final String binaryStr = binary ? "binary-based" : "string-based";
        // some checks
        if (originalDS.getDataCount() != testDS.getDataCount()) {
            throw new IllegalStateException("data set counts do not match (" + binaryStr + "): original = "
                    + originalDS.getDataCount() + " vs. copy = " + testDS.getDataCount());
        }

        if (!originalDS.getName().equals(testDS.getName())) {
            throw new IllegalStateException("data set name do not match (" + binaryStr + "): original = "
                    + originalDS.getName() + " vs. copy = " + testDS.getName());
        }

        // check for numeric value
        for (int i = 0; i < originalDS.getDataCount(); i++) {
            final double x0 = originalDS.getX(i);
            final double y0 = originalDS.getY(i);
            final double exn0 = originalDS.getXErrorNegative(i);
            final double exp0 = originalDS.getXErrorPositive(i);
            final double eyn0 = originalDS.getYErrorNegative(i);
            final double eyp0 = originalDS.getYErrorPositive(i);

            final double x1 = testDS.getX(i);
            final double y1 = testDS.getY(i);
            final double exn1 = testDS.getXErrorNegative(i);
            final double exp1 = testDS.getXErrorPositive(i);
            final double eyn1 = testDS.getYErrorNegative(i);
            final double eyp1 = testDS.getYErrorPositive(i);

            // TODO: check whether to add precision (due to double/float
            // comparison)
            if (asFloat32) {
                if (!floatIdentity(x0, x1) || !floatIdentity(y0, y1) || !floatIdentity(exn0, exn1)
                        || !floatIdentity(exp0, exp1) || !floatIdentity(eyn0, eyn1) || (eyp0 != eyp1)) {
                    String diff = String.format(
                            "(x=%e - %e, y=%e - %e, exn=%e - %e, exp=%e - %e, eyn=%e - %e, eyp=%e - %e)", x0, x1, y0,
                            y1, exn0, exn1, exp0, exp1, eyn0, eyn1, eyp0, eyp1);
                    String delta = String.format("(dx=%e, dy=%e, dexn=%e, dexp=%e, deyn=%e, deyp=%e)", x0 - x1,
                            y0 - y1, exn0 - exn1, exp0 - exp1, eyn0 - eyn1, eyp0 - eyp1);
                    String msg = String.format(
                            "data set values do not match (%s): original-copy = at index %d%n%s%n%s%n", binaryStr, i,
                            diff, delta);
                    throw new IllegalStateException(msg);
                }
            } else {
                if ((x0 != x1) || (y0 != y1) || (exn0 != exn1) || (exp0 != exp1) || (eyn0 != eyn1) || (eyp0 != eyp1)) {
                    String diff = String.format(
                            "(x=%e - %e, y=%e - %e, exn=%e - %e, exp=%e - %e, eyn=%e - %e, eyp=%e - %e)", x0, x1, y0,
                            y1, exn0, exn1, exp0, exp1, eyn0, eyn1, eyp0, eyp1);
                    String delta = String.format("(dx=%e, dy=%e, dexn=%e, dexp=%e, deyn=%e, deyp=%e)", x0 - x1,
                            y0 - y1, exn0 - exn1, exp0 - exp1, eyn0 - eyn1, eyp0 - eyp1);
                    String msg = String.format(
                            "data set values do not match (%s): original-copy = at index %d%n%s%n%s%n", binaryStr, i,
                            diff, delta);
                    throw new IllegalStateException(msg);
                }
            }
        }
    }

    public void testIdentityLabelsAndStyles(final boolean binary, final boolean asFloat32,
            final DoubleErrorDataSet originalDS, final DoubleErrorDataSet testDS) {
        final String binaryStr = binary ? "binary-based" : "string-based";
        // check for labels & styles
        for (int i = 0; i < originalDS.getDataCount(); i++) {
            if (originalDS.getDataLabel(i) == null && testDS.getDataLabel(i) == null) {
                // cannot compare null vs null
                continue;
            }
            if (originalDS.getDataLabel(i) == testDS.getDataLabel(i)) {
                String msg = String.format("data set label do not match (%s): original(%d) ='%s' vs. copy(%d) ='%s' %n",
                        binaryStr, i, originalDS.getDataLabel(i), i, testDS.getDataLabel(i));
                throw new IllegalStateException(msg);
            }
        }
        for (int i = 0; i < originalDS.getDataCount(); i++) {
            if (originalDS.getStyle(i) == null && testDS.getStyle(i) == null) {
                // cannot compare null vs null
                continue;
            }
            if (originalDS.getStyle(i) == testDS.getStyle(i)) {
                String msg = String.format("data set style do not match (%s): original(%d) ='%s' vs. copy(%d) ='%s' %n",
                        binaryStr, i, originalDS.getStyle(i), i, testDS.getStyle(i));
                throw new IllegalStateException(msg);
            }
        }
    }

    public void testIdentityMetaData(final boolean binary, final boolean asFloat32, final DoubleErrorDataSet originalDS,
            final DoubleErrorDataSet testDS) {
        final String binaryStr = binary ? "binary-based" : "string-based";
        // check for meta data and meta messages
        if (!originalDS.getInfoList().equals(testDS.getInfoList())) {
            String msg = String.format("data set info lists do not match (%s): original ='%s' vs. copy ='%s' %n",
                    binaryStr, originalDS.getInfoList(), testDS.getInfoList());
            throw new IllegalStateException(msg);
        }
        // }dataSet.getInfoList().add("standard
        // info1");dataSet.getInfoList().add("standard
        // info2");dataSet.getWarningList().add("standard
        // warning");dataSet.getErrorList().add("standard
        // error");dataSet.getMetaInfo().put("metaKey#1","metaValue#1");dataSet.getMetaInfo().put("metaKey#2","metaValue#2");dataSet.getMetaInfo().put("metaKey#3","metaValue#3");

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

    public void testPerformance(final int iterations, final boolean binary, final boolean asFloat32) {
        final long startTime = ProcessingProfiler.getTimeStamp();

        for (int i = 0; i < iterations; i++) {
            DataSetUtils.writeDataSetToByteArray(original, byteOutput, binary, asFloat32);
            final DataSet dataSet = DataSetUtils.readDataSetFromByteArray(byteOutput.toByteArray());
            if (!original.getName().equals(dataSet.getName())) {
                System.err.println("ERROR data set does not match -> potential streaming error at index = " + i);
                break;
            }
        }

        final long stopTime = ProcessingProfiler.getTimeDiff(startTime, "generating data DataSet");

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteOutput.size() / diffMillis) * 1e3);
        System.err.printf("average %s-bit binary-encoded DataSetUtils throughput with partial meta infos = %s/s%n",
                asFloat32 ? "32" : "64", humanReadableByteCount((long) byteCount, true));
    }

    public void testSerializerPerformance(final int iterations, final boolean withMetaInfos, final boolean asFloat32) {
        DataSetSerialiser.setMetaDataSerialised(withMetaInfos);
        DataSetSerialiser.setDataLablesSerialised(withMetaInfos);

        final long startTime = ProcessingProfiler.getTimeStamp();

        byteBuffer.reset(); // reset to read position (==0)
        for (int i = 0; i < iterations; i++) {
            byteBuffer.reset(); // '0' writing at start of buffer
            DataSetSerialiser.writeDataSetToByteArray(original, byteBuffer, asFloat32);
            byteBuffer.reset(); // reset to read position (==0)
            final DataSet dataSet = DataSetSerialiser.readDataSetFromByteArray(byteBuffer);

            if (!original.getName().equals(dataSet.getName())) {
                System.err.println("ERROR data set does not match -> potential streaming error at index = " + i);
                break;
            }
        }

        final long stopTime = ProcessingProfiler.getTimeDiff(startTime, "generating data DataSet");

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteBuffer.position() / diffMillis) * 1e3);
        System.err.printf("average %s-bit binary-encoded DataSetSerialiser throughput %s meta infos = %s/s%n",
                asFloat32 ? "32" : "64", withMetaInfos ? "with" : "w/o",
                humanReadableByteCount((long) byteCount, true));
    }

    private void generateData(final DoubleErrorDataSet dataSet) {
        final long startTime = ProcessingProfiler.getTimeStamp();

        dataSet.setAutoNotifaction(false);
        dataSet.clearData();
        dataSet.setName("data set name" + System.currentTimeMillis());
        double oldY = 0;
        for (int n = 0; n < N_SAMPLES; n++) {
            final double x = n;
            oldY += RandomDataGenerator.random() - 0.5;
            final double y = oldY + (n == 500000 ? 500.0 : 0);
            final double eyNeg = 0.1;
            final double eyPos = 10;
            dataSet.set(n, x, y, eyNeg, eyPos);

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

        dataSet.setAutoNotifaction(true);
        ProcessingProfiler.getTimeDiff(startTime, "generating data DataSet");
    }

    public void clearGarbage() {
        System.gc();
        System.gc();
        System.err.println("");
    }

    public static void main(final String[] args) {

        // for extra timing diagnostics
        ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(true);

        final DataSetToByteArraySample sample = new DataSetToByteArraySample();
        // some identity checks
        for (boolean bit32 : new Boolean[] { true, false }) {
            for (boolean binary : new Boolean[] { false, true }) {
                sample.testDataSetUtilsIdentity(binary, bit32);
            }
        }
        sample.clearGarbage();
        for (boolean bit32 : new Boolean[] { true, false }) {
            for (boolean withMetaData : new Boolean[] { false, true }) {
                sample.testDataSetSerialiserIdentity(withMetaData, bit32);
            }
        }
        sample.clearGarbage();

        // string based performance
        // N.B. loops only once, since this serialiser is relatively slow (~30
        // MB/s)
        sample.testPerformance(100, false, true);
        sample.testPerformance(100, false, false);
        sample.clearGarbage();

        final int iterations = 4;
        final int nLoops = 200;
        for (boolean bit32 : new Boolean[] { true, false }) {
            for (int i = 0; i < iterations; i++) {
                sample.testPerformance(nLoops, true, bit32);
            }
            sample.clearGarbage();
        }

        for (boolean bit32 : new Boolean[] { true, false }) {
            for (boolean header : new Boolean[] { true, false }) {
                for (int i = 0; i < iterations; i++) {
                    sample.testSerializerPerformance(nLoops, header, bit32);
                }
                sample.clearGarbage();
            }
        }
    }

}
