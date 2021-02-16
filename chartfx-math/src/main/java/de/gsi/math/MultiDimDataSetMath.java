package de.gsi.math;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;

import java.util.Arrays;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.GridDataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.utils.DoubleArrayCache;

/**
 * Some math operation on multi-dimensional DataSets (nDim larger than 2)
 *
 * @author rstein
 */
public final class MultiDimDataSetMath { // NOPMD -- nomen est omen

    private MultiDimDataSetMath() {
        // this is a utility class
    }

    public static void computeIntegral(final GridDataSet source, final DoubleErrorDataSet output, final int dimIndex, final double xMin, final double xMax) {
        computeMeanIntegral(source, output, dimIndex, xMin, xMax, false);
    }

    public static void computeMax(final GridDataSet source, final DoubleErrorDataSet output, final int dimIndex, final double xMin, final double xMax) {
        computeMinMax(source, output, dimIndex, xMin, xMax, false);
    }

    public static void computeMean(final GridDataSet source, final DoubleErrorDataSet output, final int dimIndex, final double xMin, final double xMax) {
        computeMeanIntegral(source, output, dimIndex, xMin, xMax, true);
    }

    public static void computeMin(final GridDataSet source, final DoubleErrorDataSet output, final int dimIndex, final double xMin, final double xMax) {
        computeMinMax(source, output, dimIndex, xMin, xMax, true);
    }

    public static void computeSlice(final GridDataSet source, final DoubleErrorDataSet output, final int dimIndex, final double xMin) {
        checkMultiDimDataSetCompatibility(source);
        checkOutputDataSetCompatibility(output);
        final double[] xValues = output.getValues(DIM_X);
        final double[] yValues = output.getValues(DIM_Y);
        final double[] yErrorNeg = output.getErrorsNegative(DIM_Y);
        final double[] yErrorPos = output.getErrorsPositive(DIM_Y);

        final int nsize = source.getShape(dimIndex);
        if (nsize == output.getDataCount()) {
            System.arraycopy(source.getGridValues(dimIndex), 0, xValues, 0, nsize);
            final double[] ret = MultiDimDataSetMath.getSliceArray(source, dimIndex, xMin, yValues);
            output.set(xValues, ret, yErrorNeg, yErrorPos, false);
        } else {
            final double[] ret = MultiDimDataSetMath.getSliceArray(source, dimIndex, xMin, yValues);
            output.set(Arrays.copyOf(source.getGridValues(dimIndex), nsize), ret, new double[nsize], new double[nsize], false);
        }

        output.setName("slice(" + source.getName() + ")@" + xMin + " " + source.getAxisDescription(dimIndex).getUnit());
        output.getAxisDescription(DIM_X).set(source.getAxisDescription(dimIndex).getName(), source.getAxisDescription(dimIndex).getUnit());
        output.getAxisDescription(DIM_Y).set(source.getAxisDescription(DIM_Z).getName(), source.getAxisDescription(DIM_Z).getUnit());
        output.getAxisDescriptions().forEach(AxisDescription::clear);
    }

    public static double[] getMeanIntegralArray(final GridDataSet source, final int dimIndex, final double xMin, final double xMax, final double[] buffer, final boolean isMean) {
        checkMultiDimDataSetCompatibility(source);
        final double[] ret = getSanitizedBuffer(source, dimIndex, buffer);

        final int minIndex = source.getGridIndex(dimIndex == DIM_X ? DIM_Y : DIM_X, xMin);
        final int maxIndex = source.getGridIndex(dimIndex == DIM_X ? DIM_Y : DIM_X, xMax);
        final int min = Math.min(minIndex, maxIndex);
        final int max = Math.max(Math.max(minIndex, maxIndex), min + 1);

        final int nDataCount = source.getShape(dimIndex);
        if (dimIndex == DIM_Y) {
            for (int index = 0; index < nDataCount; index++) {
                double integral = 0.0;
                int nSlices = 0;
                for (int i = min; i <= Math.min(max, nDataCount - 1); i++) {
                    integral += source.get(DIM_Z, i, index);
                    nSlices += 1;
                }
                ret[index] = isMean ? nSlices == 0 ? Double.NaN : integral / nSlices : integral;
            }
        } else {
            for (int index = 0; index < nDataCount; index++) {
                double integral = 0.0;
                int nSlices = 0;
                for (int i = min; i <= Math.min(max, nDataCount - 1); i++) {
                    integral += source.get(DIM_Z, index, i);
                    nSlices += 1;
                }
                ret[index] = isMean ? nSlices == 0 ? Double.NaN : integral / nSlices : integral;
            }
        }
        return ret;
    }

    public static double[] getMinMaxArray(final GridDataSet source, final int dimIndex, final double xMin, final double xMax, final double[] buffer, final boolean isMin) {
        checkMultiDimDataSetCompatibility(source);
        final double[] ret = getSanitizedBuffer(source, dimIndex, buffer);

        final int minIndex = source.getIndex(dimIndex == DIM_X ? DIM_Y : DIM_X, xMin);
        final int maxIndex = source.getIndex(dimIndex == DIM_X ? DIM_Y : DIM_X, xMax);

        final int min = Math.min(minIndex, maxIndex);
        final int max = Math.max(Math.max(minIndex, maxIndex), min + 1);

        final int nDataCount = source.getShape(dimIndex);
        if (dimIndex == DIM_Y) {
            for (int index = 0; index < nDataCount; index++) {
                double extreme = source.get(DIM_Z, min, index);
                for (int i = min + 1; i <= Math.min(max, nDataCount - 1); i++) {
                    final double val = source.get(DIM_Z, i, index);
                    extreme = isMin ? Math.min(val, extreme) : Math.max(val, extreme);
                }
                ret[index] = extreme;
            }
        } else {
            for (int index = 0; index < nDataCount; index++) {
                double extreme = source.get(DIM_Z, index, min);
                for (int i = min + 1; i <= Math.min(max, nDataCount - 1); i++) {
                    final double val = source.get(DIM_Z, index, i);
                    extreme = isMin ? Math.min(val, extreme) : Math.max(val, extreme);
                }
                ret[index] = extreme;
            }
        }

        return ret;
    }

    public static double[] getSliceArray(final GridDataSet source, final int dimIndex, final double xMin, final double[] buffer) {
        checkMultiDimDataSetCompatibility(source);
        final double[] ret = getSanitizedBuffer(source, dimIndex, buffer);

        final int minIndex = source.getGridIndex(dimIndex == DIM_X ? DIM_Y : DIM_X, xMin);

        final int nDataCount = source.getShape(dimIndex);
        if (dimIndex == DIM_Y) {
            for (int index = 0; index < nDataCount; index++) {
                final double y = source.get(DIM_Z, minIndex, index);
                ret[index] = y;
            }
        } else {
            for (int index = 0; index < nDataCount; index++) {
                final double y = source.get(DIM_Z, index, minIndex);
                ret[index] = y;
            }
        }

        return ret;
    }

    private static void checkMultiDimDataSetCompatibility(final DataSet source) {
        if (source == null || source.getDimension() <= 2) {
            throw new IllegalArgumentException("source is " + (source == null ? "null" : " has insufficient dimension = " + source.getDimension()));
        }
    }

    private static void checkOutputDataSetCompatibility(final DataSet ouput) {
        if (ouput == null || ouput.getDimension() != 2) {
            throw new IllegalArgumentException("output is " + (ouput == null ? "null" : " has insufficient dimension = " + ouput.getDimension()));
        }
    }

    private static void computeMeanIntegral(final GridDataSet source, final DoubleErrorDataSet output, final int dimIndex, final double xMin, final double xMax, final boolean isMean) {
        checkMultiDimDataSetCompatibility(source);
        checkOutputDataSetCompatibility(output);
        final double[] xValues = output.getValues(DIM_X);
        final double[] yValues = output.getValues(DIM_Y);
        final double[] yErrorNeg = output.getErrorsNegative(DIM_Y);
        final double[] yErrorPos = output.getErrorsPositive(DIM_Y);

        final int nsize = source.getShape(dimIndex);
        if (nsize == output.getDataCount()) {
            System.arraycopy(source.getValues(dimIndex), 0, xValues, 0, nsize);
            final double[] ret = MultiDimDataSetMath.getMeanIntegralArray(source, dimIndex, xMin, xMax, yValues, isMean);
            output.set(xValues, ret, yErrorNeg, yErrorPos, false);
        } else {
            final double[] ret = MultiDimDataSetMath.getMeanIntegralArray(source, dimIndex, xMin, xMax, yValues, isMean);
            output.set(Arrays.copyOf(source.getValues(dimIndex), nsize), ret, new double[nsize], new double[nsize], false);
        }

        output.setName((isMean ? "mean(" : "int(") + source.getName() + ")@" + xMin + " -> " + xMax + " " + source.getAxisDescription(dimIndex).getUnit());
        output.getAxisDescription(DIM_X).set(source.getAxisDescription(dimIndex).getName(), source.getAxisDescription(dimIndex).getUnit());
        output.getAxisDescription(DIM_Y).set(source.getAxisDescription(DIM_Z).getName(), source.getAxisDescription(DIM_Z).getUnit());
        output.getAxisDescriptions().forEach(AxisDescription::clear);
    }

    private static void computeMinMax(final GridDataSet source, final DoubleErrorDataSet output, final int dimIndex, final double xMin, final double xMax, final boolean isMin) {
        checkMultiDimDataSetCompatibility(source);
        checkOutputDataSetCompatibility(output);
        final double[] xValues = output.getValues(DIM_X);
        final double[] yValues = output.getValues(DIM_Y);
        final double[] yErrorNeg = output.getErrorsNegative(DIM_Y);
        final double[] yErrorPos = output.getErrorsPositive(DIM_Y);

        final int nsize = source.getShape(dimIndex);
        if (nsize == output.getDataCount()) {
            System.arraycopy(source.getValues(dimIndex), 0, xValues, 0, nsize);
            final double[] ret = MultiDimDataSetMath.getMinMaxArray(source, dimIndex, xMin, xMax, yValues, isMin);
            output.set(xValues, ret, yErrorNeg, yErrorPos, false);
        } else {
            final double[] ret = MultiDimDataSetMath.getMinMaxArray(source, dimIndex, xMin, xMax, yValues, isMin);
            output.set(Arrays.copyOf(source.getValues(dimIndex), nsize), ret, new double[nsize], new double[nsize], false);
        }

        output.setName((isMin ? "min(" : "max(") + source.getName() + ")@" + xMin + " -> " + xMax + " " + source.getAxisDescription(dimIndex).getUnit());
        output.getAxisDescription(DIM_X).set(source.getAxisDescription(dimIndex).getName(), source.getAxisDescription(dimIndex).getUnit());
        output.getAxisDescription(DIM_Y).set(source.getAxisDescription(DIM_Z).getName(), source.getAxisDescription(DIM_Z).getUnit());
        output.getAxisDescriptions().forEach(AxisDescription::clear);
    }

    private static double[] getSanitizedBuffer(final GridDataSet source, final int dimIndex, final double[] buffer) {
        final int size = source.getShape(dimIndex);
        final boolean invalidBuffer = buffer == null || buffer.length < size;
        return invalidBuffer ? DoubleArrayCache.getInstance().getArrayExact(size) : buffer;
    }
}
