package de.gsi.dataset.samples;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.testdata.spi.RandomDataGenerator;
import de.gsi.dataset.utils.DataSetUtils;
import de.gsi.dataset.utils.ProcessingProfiler;

public class DataSetToByteArraySample {
    private static final int N_SAMPLES = 100000; // default: 100000
    private final DoubleErrorDataSet original = new DoubleErrorDataSet("init", N_SAMPLES);
    private final ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

    public DataSetToByteArraySample() {
        System.err.println(DataSetToByteArraySample.class.getSimpleName() + " - init");
        generateData(original);
        System.err.println(DataSetToByteArraySample.class.getSimpleName() + " - generated data");

        DataSetUtils.writeDataSetToByteArray(original, byteOutput, false, false);
        System.err.println(
                "byte buffer array length with string encoding = " + humanReadableByteCount(byteOutput.size(), true));

        DataSetUtils.writeDataSetToByteArray(original, byteOutput, true, true);
        System.err.println("byte buffer array length with 32-bit binary encoding = "
                + humanReadableByteCount(byteOutput.size(), true));

        DataSetUtils.writeDataSetToByteArray(original, byteOutput, true, false);
        System.err.println("byte buffer array length with 64-bit binary encoding = "
                + humanReadableByteCount(byteOutput.size(), true));
    }

    protected boolean floatIdentity(double a, double b) {
        if (Math.abs((float)a - (float)b) > Float.MIN_VALUE) {
            return false;
        }
        return true;
    }

    public void testIdentity(final boolean binary, final boolean asFloat32) {
        DataSetUtils.writeDataSetToByteArray(original, byteOutput, binary, asFloat32);
        final String encoding = binary ? "binary-encoding" : "string-encoding";

        final DoubleErrorDataSet dataSet = DataSetUtils.readDataSetFromByteArray(byteOutput.toByteArray());

        // some checks
        final boolean test = true;
        if (original.getDataCount() != dataSet.getDataCount()) {
            throw new IllegalStateException("data set counts do not match (" + binary + "): original = "
                    + original.getDataCount() + " vs. copy = " + dataSet.getDataCount());
        }

        if (!original.getName().equals(dataSet.getName())) {
            throw new IllegalStateException("data set name do not match (" + binary + "): original = "
                    + original.getName() + " vs. copy = " + dataSet.getName());
        }

        // check for numeric value
        for (int i = 0; i < original.getDataCount(); i++) {
            final double x0 = original.getX(i);
            final double y0 = original.getY(i);
            final double exn0 = original.getXErrorNegative(i);
            final double exp0 = original.getXErrorPositive(i);
            final double eyn0 = original.getYErrorNegative(i);
            final double eyp0 = original.getYErrorPositive(i);

            final double x1 = dataSet.getX(i);
            final double y1 = dataSet.getY(i);
            final double exn1 = dataSet.getXErrorNegative(i);
            final double exp1 = dataSet.getXErrorPositive(i);
            final double eyn1 = dataSet.getYErrorNegative(i);
            final double eyp1 = dataSet.getYErrorPositive(i);

            // TODO: check whether to add precision (due to double/float
            // comparison)
            if (asFloat32) {
                if (!floatIdentity(x0, x1) || !floatIdentity(y0, y1) || !floatIdentity(exn0, exn1)
                        || !floatIdentity(exp0, exp1) || !floatIdentity(eyn0, eyn1) || (eyp0 != eyp1)) {
                    throw new IllegalStateException("data set name do not match (" + binary + "): original-copy = \n"
                            + String.format(
                                    "at index=%d:\n(x=%e - %e, y=%e - %e, exn=%e - %e, exp=%e - %e, eyn=%e - %e, eyp=%e - %e)\n",
                                    i, x0, x1, y0, y1, exn0, exn1, exp0, exp1, eyn0, eyn1, eyp0, eyp1)
                            + String.format("diffs (dx=%e, dy=%e, dexn=%e, dexp=%e, deyn=%e, deyp=%e)", x0 - x1,
                                    y0 - y1, exn0 - exn1, exp0 - exp1, eyn0 - eyn1, eyp0 - eyp1));
                }
            } else {
                if ((x0 != x1) || (y0 != y1) || (exn0 != exn1) || (exp0 != exp1) || (eyn0 != eyn1) || (eyp0 != eyp1)) {
                    throw new IllegalStateException("data set name do not match (" + binary + "): original-copy = \n"
                            + String.format(
                                    "at index=%d:\n(x=%e - %e, y=%e - %e, exn=%e - %e, exp=%e - %e, eyn=%e - %e, eyp=%e - %e)\n",
                                    i, x0, x1, y0, y1, exn0, exn1, exp0, exp1, eyn0, eyn1, eyp0, eyp1)
                            + String.format("diffs (dx=%e, dy=%e, dexn=%e, dexp=%e, deyn=%e, deyp=%e)", x0 - x1,
                                    y0 - y1, exn0 - exn1, exp0 - exp1, eyn0 - eyn1, eyp0 - eyp1));
                }
            }
        }

        // TODO: check for labels, styles, maps -&gt; should be JUnit test

        System.err.println("all identity tests passed for " + (asFloat32 ? "32-bit " : "64-bit ")
                + (binary ? "binary" : "string") + "-based encoding");
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
            final DoubleErrorDataSet dataSet = DataSetUtils.readDataSetFromByteArray(byteOutput.toByteArray());
            if (!original.getName().equals(dataSet.getName())) {
                System.err.println("ERROR data set does not match -> potential streaming error at index = " + i);
                break;
            }
        }

        final long stopTime = ProcessingProfiler.getTimeDiff(startTime, "generating data DataSet");

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteOutput.size() / diffMillis) * 1e3);
        System.err.println("average " + (binary ? ((asFloat32 ? "32-bit " : "64-bit ") + "binary") : "string")
                + "-encoded throughput = " + humanReadableByteCount((long) byteCount, true) + "/s");
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
            final double ey_neg = 0.1;
            final double ey_pos = 10;
            dataSet.set(n, x, y, ey_neg, ey_pos);

            if (n == 500000) {
                dataSet.getDataLabelMap().put(n, "special outlier");
            }
        }

        dataSet.setAutoNotifaction(true);
        ProcessingProfiler.getTimeDiff(startTime, "generating data DataSet");
    }

    public static void main(final String[] args) {

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // for extra timing diagnostics
        ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(true);

        final DataSetToByteArraySample sample = new DataSetToByteArraySample();

        sample.testPerformance(100, false, false);
        sample.testPerformance(100, true, false);
        sample.testPerformance(100, true, true);

        sample.testIdentity(true, false);
        sample.testIdentity(true, true);
        sample.testIdentity(false, false);

    }

}
