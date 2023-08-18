package io.fair_acc.chartfx.renderer.spi;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;
import static io.fair_acc.math.ArrayUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.RendererDataReducer;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError;
import io.fair_acc.dataset.DataSetError.ErrorType;
import io.fair_acc.dataset.utils.CachedDaemonThreadFactory;
import io.fair_acc.dataset.utils.IndexedStringConsumer;
import io.fair_acc.dataset.utils.ProcessingProfiler;
import io.fair_acc.math.ArrayUtils;

/**
 * package private class implementation (data point caching) required by ErrorDataSetRenderer local screen data point
 * cache (minimises re-allocation/garbage collection)
 *
 * @author rstein
 */
@SuppressWarnings({ "PMD.TooManyMethods", "PMD.TooManyFields" }) // designated purpose of this class
class CachedDataPoints {
    private static final double DEG_TO_RAD = Math.PI / 180.0;

    protected double[] xValues;
    protected double[] yValues;
    protected double[] errorXNeg;
    protected double[] errorXPos;
    protected double[] errorYNeg;
    protected double[] errorYPos;
    protected boolean[] selected;
    protected String[] styles;
    protected boolean xAxisInverted;
    protected boolean yAxisInverted;
    protected boolean allowForNaNs;
    protected boolean hasStyles;
    protected ErrorType[] errorType;
    protected int indexMin;
    protected int indexMax;
    protected int minDistanceX = +Integer.MAX_VALUE;
    protected double xZero; // reference zero 'x' axis coordinate
    protected double yZero; // reference zero 'y' axis coordinate
    protected double yMin;
    protected double yMax;
    protected double xMin;
    protected double xMax;
    protected boolean polarPlot;
    protected ErrorStyle rendererErrorStyle;
    protected double xRange;
    protected double yRange;
    protected double maxRadius;
    protected int maxDataCount;
    protected int actualDataCount; // number of data points that remain after data reduction

    public void trim() {
        xValues = clearIfLarger(xValues, maxDataCount);
        yValues = clearIfLarger(yValues, maxDataCount);
        errorYNeg = clearIfLarger(errorYNeg, maxDataCount);
        errorYPos = clearIfLarger(errorYPos, maxDataCount);
        errorXNeg = clearIfLarger(errorXNeg, maxDataCount);
        errorXPos = clearIfLarger(errorXPos, maxDataCount);
        selected = clearIfLarger(selected, maxDataCount);
        styles = clearIfLarger(styles, maxDataCount);
        errorType = clearIfLarger(errorType, 10); // depends on ds dimensions
    }

    public CachedDataPoints resizeMin(final int indexMin, final int indexMax, final int dataLength, final boolean useErrorsX) {
        this.indexMin = indexMin;
        this.indexMax = indexMax;
        maxDataCount = dataLength;
        xValues = ArrayUtils.resizeMin(xValues, dataLength);
        yValues = ArrayUtils.resizeMin(yValues, dataLength);
        errorYNeg = ArrayUtils.resizeMin(errorYNeg, dataLength);
        errorYPos = ArrayUtils.resizeMin(errorYPos, dataLength);
        if (useErrorsX) {
            errorXNeg = ArrayUtils.resizeMin(errorXNeg, dataLength);
            errorXPos = ArrayUtils.resizeMin(errorXPos, dataLength);
        }
        selected = ArrayUtils.resizeMin(selected,  dataLength);
        hasStyles = false;  // Styles get updated in boundary condition.
        return this;
    }

    protected void computeBoundaryVariables(final Axis xAxis, final Axis yAxis) {
        xAxisInverted = xAxis.isInvertedAxis();
        yAxisInverted = yAxis.isInvertedAxis();

        // compute cached axis variables ... about 50% faster than the
        // generic template based version from the original ValueAxis<Number>
        if (xAxis.isLogAxis()) {
            xZero = xAxis.getDisplayPosition(xAxis.getMin());
        } else {
            xZero = xAxis.getDisplayPosition(0);
        }
        if (yAxis.isLogAxis()) {
            yZero = yAxis.getDisplayPosition(yAxis.getMin());
        } else {
            yZero = yAxis.getDisplayPosition(0);
        }

        yMin = yAxis.getDisplayPosition(yAxis.getMin());
        yMax = yAxis.getDisplayPosition(yAxis.getMax());
        xMin = xAxis.getDisplayPosition(xAxis.getMin());
        xMax = xAxis.getDisplayPosition(xAxis.getMax());

        xRange = Math.abs(xMax - xMin);
        yRange = Math.abs(yMax - yMin);
        maxRadius = 0.5 * Math.max(Math.min(xRange, yRange), 20) * 0.9;
        // TODO: parameterise '0.9' -> radius axis fills 90% of min canvas axis
        if (polarPlot) {
            xZero = 0.5 * xRange;
            yZero = 0.5 * yRange;
        }
    }

    private void computeFullPolar(final Axis yAxis, final DataSetError dataSet, final int min, final int max) {
        for (int index = min; index < max; index++) {
            final double x = dataSet.get(DIM_X, index);
            final double y = dataSet.get(DIM_Y, index);
            // check if error should be surrounded by Math.abs(..)
            // to ensure that they are always positive
            final double phi = x * DEG_TO_RAD;
            final double r = maxRadius * Math.abs(1 - (yAxis.getDisplayPosition(y) / yRange));
            xValues[index] = xZero + (r * Math.cos(phi));
            yValues[index] = yZero + (r * Math.sin(phi));

            // ignore errors (for now) -> TODO: add proper transformation
            errorXNeg[index] = 0.0;
            errorXPos[index] = 0.0;
            errorYNeg[index] = 0.0;
            errorYPos[index] = 0.0;

            if (!Double.isFinite(yValues[index])) {
                yValues[index] = yZero;
            }
        }
    }

    private void computeNoErrorPolar(final Axis yAxis, final DataSet dataSet, final int min, final int max) {
        // experimental transform euclidean to polar coordinates
        for (int index = min; index < max; index++) {
            final double x = dataSet.get(DIM_X, index);
            final double y = dataSet.get(DIM_Y, index);
            // check if error should be surrounded by Math.abs(..)
            // to ensure that they are always positive
            final double phi = x * DEG_TO_RAD;
            final double r = maxRadius * Math.abs(1 - (yAxis.getDisplayPosition(y) / yRange));
            xValues[index] = xZero + (r * Math.cos(phi));
            yValues[index] = yZero + (r * Math.sin(phi));

            if (!Double.isFinite(yValues[index])) {
                yValues[index] = yZero;
            }
        }
    }

    protected void computeScreenCoordinates(final Axis xAxis, final Axis yAxis, DataSet dataSet, final DataSetNode style,
                                            final int min, final int max, final ErrorStyle localRendErrorStyle,
            final boolean isPolarPlot, final boolean doAllowForNaNs) {
        setBoundaryConditions(xAxis, yAxis, dataSet, style, min, max, localRendErrorStyle, isPolarPlot,
                doAllowForNaNs);

        // compute data set to screen coordinates
        computeScreenCoordinatesNonThreaded(xAxis, yAxis, dataSet, min, max);
    }

    private void computeScreenCoordinatesEuclidean(final Axis xAxis, final Axis yAxis, final DataSet dataSet,
            final int min, final int max) {
        for (int dimIndex = 0; dimIndex < 2; dimIndex++) {
            switch (errorType[dimIndex]) {
            case NO_ERROR:
                if (allowForNaNs) {
                    computeWithNoErrorAllowingNaNs(dimIndex == DIM_X ? xAxis : yAxis, dataSet, dimIndex, min, max);
                } else {
                    computeWithNoError(dimIndex == DIM_X ? xAxis : yAxis, dataSet, dimIndex, min, max);
                }
                break;
            case SYMMETRIC:
            case ASYMMETRIC:
            default:
                if (allowForNaNs) {
                    computeWithErrorAllowingNaNs(dimIndex == DIM_X ? xAxis : yAxis, dataSet, dimIndex, min, max);
                } else {
                    computeWithError(dimIndex == DIM_X ? xAxis : yAxis, dataSet, dimIndex, min, max);
                }
                break;
            }
        }
    }

    protected void computeScreenCoordinatesInParallel(final Axis xAxis, final Axis yAxis, final DataSet dataSet, final DataSetNode style,
                                                      final int min, final int max, final ErrorStyle localRendErrorStyle,
            final boolean isPolarPlot, final boolean doAllowForNaNs) {
        setBoundaryConditions(xAxis, yAxis, dataSet, style, min, max, localRendErrorStyle, isPolarPlot,
                doAllowForNaNs);

        // compute data set to screen coordinates
        computeScreenCoordinatesParallel(xAxis, yAxis, dataSet, min, max);
    }

    protected void computeScreenCoordinatesNonThreaded(final Axis xAxis, final Axis yAxis, final DataSet dataSet,
            final int min, final int max) {
        if (polarPlot) {
            computeScreenCoordinatesPolar(yAxis, dataSet, min, max);
        } else {
            computeScreenCoordinatesEuclidean(xAxis, yAxis, dataSet, min, max);
        }
    }

    protected void computeScreenCoordinatesParallel(final Axis xAxis, final Axis yAxis, final DataSet dataSet,
            final int min, final int max) {
        final int minthreshold = 1000;
        final int divThread = (int) Math
                                      .ceil(Math.abs(max - min) / (double) CachedDaemonThreadFactory.getNumbersOfThreads());
        final int stepSize = Math.max(divThread, minthreshold);
        final List<Callable<Boolean>> workers = new ArrayList<>();
        for (int i = min; i < max; i += stepSize) {
            final int start = i;
            workers.add(() -> {
                if (polarPlot) {
                    computeScreenCoordinatesPolar(yAxis, dataSet, start, Math.min(max, start + stepSize));
                } else {
                    computeScreenCoordinatesEuclidean(xAxis, yAxis, dataSet, start, Math.min(max, start + stepSize));
                }
                return Boolean.TRUE;
            });
        }

        try {
            final List<Future<Boolean>> jobs = CachedDaemonThreadFactory.getCommonPool().invokeAll(workers);
            for (final Future<Boolean> future : jobs) {
                final Boolean r = future.get();
                if (Boolean.FALSE.equals(r)) {
                    throw new IllegalStateException("one parallel worker thread finished execution with error");
                }
            }
        } catch (final InterruptedException | ExecutionException e) {
            throw new IllegalStateException("one parallel worker thread finished execution with error", e);
        }
    }

    private void computeScreenCoordinatesPolar(final Axis yAxis, final DataSet dataSet, final int min, final int max) {
        if ((errorType[DIM_X] == ErrorType.NO_ERROR) && (errorType[DIM_Y] == ErrorType.NO_ERROR)) {
            computeNoErrorPolar(yAxis, dataSet, min, max);
        } else if (errorType[DIM_X] == ErrorType.NO_ERROR) {
            computeYonlyPolar(yAxis, dataSet, min, max);
        } else {
            // dataSet may not be non-DataSetError at this stage
            final DataSetError ds = (DataSetError) dataSet;
            computeFullPolar(yAxis, ds, min, max);
        }
    }

    private void computeWithError(final Axis yAxis, final DataSet dataSet, final int dimIndex, final int min,
            final int max) {
        if (dataSet instanceof DataSetError) {
            final double[] values = dimIndex == DIM_X ? xValues : yValues;
            final double[] valuesEN = dimIndex == DIM_X ? errorXNeg : errorYNeg;
            final double[] valuesEP = dimIndex == DIM_X ? errorXPos : errorYPos;
            final double minValue = dimIndex == DIM_X ? xMin : yMin;
            final DataSetError ds = (DataSetError) dataSet;
            for (int index = min; index < max; index++) {
                final double value = dataSet.get(dimIndex, index);

                values[index] = yAxis.getDisplayPosition(value);

                if (!Double.isNaN(values[index])) {
                    valuesEN[index] = yAxis.getDisplayPosition(value - ds.getErrorNegative(dimIndex, index));
                    valuesEP[index] = yAxis.getDisplayPosition(value + ds.getErrorPositive(dimIndex, index));
                    continue;
                }
                values[index] = minValue;
                valuesEN[index] = minValue;
                valuesEP[index] = minValue;
            }
            return;
        }

        // default dataset
        final double[] values = dimIndex == DIM_X ? xValues : yValues;
        final double[] valuesEN = dimIndex == DIM_X ? errorXNeg : errorYNeg;
        final double[] valuesEP = dimIndex == DIM_X ? errorXPos : errorYPos;
        final double minValue = dimIndex == DIM_X ? xMin : yMin;

        for (int index = min; index < max; index++) {
            values[index] = yAxis.getDisplayPosition(dataSet.get(dimIndex, index));
            if (Double.isFinite(values[index])) {
                valuesEN[index] = values[index];
                valuesEP[index] = values[index];
            } else {
                values[index] = minValue;
                valuesEN[index] = minValue;
                valuesEP[index] = minValue;
            }
        }
    }

    private void computeWithErrorAllowingNaNs(final Axis yAxis, final DataSet dataSet, final int dimIndex,
            final int min, final int max) {
        if (dataSet instanceof DataSetError) {
            final double[] values = dimIndex == DIM_X ? xValues : yValues;
            final double[] valuesEN = dimIndex == DIM_X ? errorXNeg : errorYNeg;
            final double[] valuesEP = dimIndex == DIM_X ? errorXPos : errorYPos;
            final DataSetError ds = (DataSetError) dataSet;
            for (int index = min; index < max; index++) {
                final double value = dataSet.get(dimIndex, index);

                if (!Double.isFinite(value)) {
                    values[index] = Double.NaN;
                    valuesEN[index] = Double.NaN;
                    valuesEP[index] = Double.NaN;
                    continue;
                }

                values[index] = yAxis.getDisplayPosition(value);
                valuesEN[index] = yAxis.getDisplayPosition(value - ds.getErrorNegative(dimIndex, index));
                valuesEP[index] = yAxis.getDisplayPosition(value + ds.getErrorPositive(dimIndex, index));
            }
            return;
        }

        // default dataset
        final double[] values = dimIndex == DIM_X ? xValues : yValues;
        final double[] valuesEN = dimIndex == DIM_X ? errorXNeg : errorYNeg;
        final double[] valuesEP = dimIndex == DIM_X ? errorXPos : errorYPos;

        for (int index = min; index < max; index++) {
            values[index] = yAxis.getDisplayPosition(dataSet.get(dimIndex, index));

            if (Double.isFinite(values[index])) {
                valuesEN[index] = values[index];
                valuesEP[index] = values[index];
            } else {
                values[index] = Double.NaN;
                valuesEN[index] = Double.NaN;
                valuesEP[index] = Double.NaN;
            }
        }
    }

    private void computeWithNoError(final Axis axis, final DataSet dataSet, final int dimIndex, final int min,
            final int max) {
        // no error attached
        final double[] values = dimIndex == DIM_X ? xValues : yValues;
        final double minValue = dimIndex == DIM_X ? xMin : yMin;
        for (int index = min; index < max; index++) {
            final double value = dataSet.get(dimIndex, index);

            values[index] = axis.getDisplayPosition(value);

            if (Double.isNaN(values[index])) {
                yValues[index] = minValue;
            }
        }

        if ((dimIndex == DIM_Y) && (rendererErrorStyle != ErrorStyle.NONE)) {
            System.arraycopy(values, min, errorYNeg, min, max - min);
            System.arraycopy(values, min, errorYPos, min, max - min);
        }
    }

    private void computeWithNoErrorAllowingNaNs(final Axis axis, final DataSet dataSet, final int dimIndex,
            final int min, final int max) {
        // no error attached
        final double[] values = dimIndex == DIM_X ? xValues : yValues;
        for (int index = min; index < max; index++) {
            final double value = dataSet.get(dimIndex, index);

            if (Double.isFinite(value)) {
                values[index] = axis.getDisplayPosition(value);
            } else {
                values[index] = Double.NaN;
            }
        }

        if ((dimIndex == DIM_Y) && (rendererErrorStyle != ErrorStyle.NONE)) {
            System.arraycopy(values, min, errorYNeg, min, max - min);
            System.arraycopy(values, min, errorYPos, min, max - min);
        }
    }

    private void computeYonlyPolar(final Axis yAxis, final DataSet dataSet, final int min, final int max) {
        for (int index = min; index < max; index++) {
            final double x = dataSet.get(DIM_X, index);
            final double y = dataSet.get(DIM_Y, index);
            // check if error should be surrounded by Math.abs(..)
            // to ensure that they are always positive
            final double phi = x * DEG_TO_RAD;
            final double r = maxRadius * Math.abs(1 - (yAxis.getDisplayPosition(y) / yRange));
            xValues[index] = xZero + (r * Math.cos(phi));
            yValues[index] = yZero + (r * Math.sin(phi));

            // ignore errors (for now) -> TODO: add proper transformation
            errorXNeg[index] = 0.0;
            errorXPos[index] = 0.0;
            errorYNeg[index] = 0.0;
            errorYPos[index] = 0.0;

            if (!Double.isFinite(yValues[index])) {
                yValues[index] = yZero;
            }
        }
    }

    /**
     * computes the minimum distance in between data points N.B. assumes sorted data set points
     *
     * @return min distance
     */
    protected int getMinXDistance() {
        if (minDistanceX < Integer.MAX_VALUE) {
            return minDistanceX;
        }

        if (indexMin >= indexMax) {
            minDistanceX = 1;
            return minDistanceX;
        }

        minDistanceX = Integer.MAX_VALUE;
        for (int i = 1; i < actualDataCount; i++) {
            final double x0 = xValues[i - 1];
            final double x1 = xValues[i];
            minDistanceX = Math.min(minDistanceX, (int) Math.abs(x1 - x0));
        }
        return minDistanceX;
    }

    private int minDataPointDistanceX() {
        if (actualDataCount <= 1) {
            minDistanceX = 1;
            return minDistanceX;
        }
        minDistanceX = Integer.MAX_VALUE;
        for (int i = 1; i < actualDataCount; i++) {
            final double x0 = xValues[i - 1];
            final double x1 = xValues[i];
            minDistanceX = Math.min(minDistanceX, (int) Math.abs(x1 - x0));
        }
        return minDistanceX;
    }

    protected void reduce(final RendererDataReducer cruncher, final boolean isReducePoints,
            final int minRequiredReductionSize) {
        final long startTimeStamp = ProcessingProfiler.getTimeStamp();
        actualDataCount = 1;

        if (!isReducePoints || (Math.abs(indexMax - indexMin) < minRequiredReductionSize)) {
            actualDataCount = indexMax - indexMin;
            System.arraycopy(xValues, indexMin, xValues, 0, actualDataCount);
            System.arraycopy(yValues, indexMin, yValues, 0, actualDataCount);
            System.arraycopy(selected, indexMin, selected, 0, actualDataCount);
            if (errorType[DIM_X] != ErrorType.NO_ERROR) {
                // XY: // symmetric errors around x and y
                // X: // only symmetric errors around x
                // X_ASYMMETRIC: // asymmetric errors around x
                System.arraycopy(errorXNeg, indexMin, errorXNeg, 0, actualDataCount);
                System.arraycopy(errorXPos, indexMin, errorXPos, 0, actualDataCount);
                System.arraycopy(errorYNeg, indexMin, errorYNeg, 0, actualDataCount);
                System.arraycopy(errorYPos, indexMin, errorYPos, 0, actualDataCount);
            } else if (errorType[DIM_Y] != ErrorType.NO_ERROR) {
                // Y: // only symmetric errors around y
                // Y_ASYMMETRIC: // asymmetric errors around y
                System.arraycopy(errorYNeg, indexMin, errorYNeg, 0, actualDataCount);
                System.arraycopy(errorYPos, indexMin, errorYPos, 0, actualDataCount);
            }

            ProcessingProfiler.getTimeDiff(startTimeStamp, String.format("no data reduction (%d)", actualDataCount));
            return;
        }
        if (errorType[DIM_X] == ErrorType.NO_ERROR) {
            actualDataCount = cruncher.reducePoints(xValues, yValues, null, null, errorYPos, errorYNeg, styles,
                    selected, indexMin, indexMax);
        } else {
            actualDataCount = cruncher.reducePoints(xValues, yValues, errorXPos, errorXNeg, errorYPos, errorYNeg,
                    styles, selected, indexMin, indexMax);
        }
        minDataPointDistanceX();
    }

    private void setBoundaryConditions(final Axis xAxis, final Axis yAxis, DataSet dataSet, final DataSetNode style,
                                       final int min, final int max, final ErrorStyle rendererErrorStyle, final boolean isPolarPlot,
                                       final boolean doAllowForNaNs) {
        indexMin = min;
        indexMax = max;
        polarPlot = isPolarPlot;
        this.allowForNaNs = doAllowForNaNs;
        this.rendererErrorStyle = rendererErrorStyle;

        // set optional styles
        hasStyles = dataSet.hasStyles();
        if (hasStyles) {
            styles = ArrayUtils.resizeMinNulled(styles, maxDataCount, String[]::new);
            dataSet.forEachStyle(min, max, styleSetter);
        } else {
            // For now we still need to allocate the array to not break other code
            // (e.g. reducer). TODO: remove unnecessary array
            styles = ArrayUtils.resizeMin(styles, maxDataCount, String[]::new, false);
        }

        computeBoundaryVariables(xAxis, yAxis);
        setErrorType(dataSet, rendererErrorStyle);
    }
    private final IndexedStringConsumer styleSetter = (i, style) -> styles[i] = style;

    protected void setErrorType(final DataSet dataSet, final ErrorStyle errorStyle) {
        errorType = ArrayUtils.resizeMinNulled(errorType, dataSet.getDimension(), ErrorType[]::new);
        if (dataSet instanceof DataSetError) {
            final DataSetError ds = (DataSetError) dataSet;
            for (int dimIndex = 0; dimIndex < ds.getDimension(); dimIndex++) {
                errorType[dimIndex] = ds.getErrorType(dimIndex);
            }
        } else if (errorStyle == ErrorStyle.NONE) {
            // special case where users does not want error bars
            for (int dimIndex = 0; dimIndex < dataSet.getDimension(); dimIndex++) {
                errorType[dimIndex] = ErrorType.NO_ERROR;
            }
        } else {
            // fall-back for standard DataSet

            // default: ErrorType=Y fall-back also for 'DataSet' without
            // errors
            // rationale: scientific honesty
            // if data points are being compressed, the error of compression
            // (e.g. due to local transients that are being suppressed) are
            // nevertheless being computed and shown even if individual data
            // points have no error
            for (int dimIndex = 0; dimIndex < dataSet.getDimension(); dimIndex++) {
                errorType[dimIndex] = dimIndex == DIM_Y ? ErrorType.ASYMMETRIC : ErrorType.NO_ERROR;
            }
        }
    }

}
