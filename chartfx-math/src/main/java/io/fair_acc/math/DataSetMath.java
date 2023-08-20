package io.fair_acc.math;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;
import static io.fair_acc.dataset.DataSet.DIM_Z;
import static io.fair_acc.dataset.Histogram.Boundary.LOWER;
import static io.fair_acc.dataset.Histogram.Boundary.UPPER;
import static io.fair_acc.math.DataSetMath.ErrType.EXP;
import static io.fair_acc.math.DataSetMath.ErrType.EYN;
import static io.fair_acc.math.DataSetMath.ErrType.EYP;
import static io.fair_acc.math.MathBase.max;
import static io.fair_acc.math.MathBase.min;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.fair_acc.dataset.*;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;
import io.fair_acc.dataset.spi.Histogram;
import io.fair_acc.dataset.spi.utils.DoublePointError;
import io.fair_acc.dataset.utils.NoDuplicatesList;
import io.fair_acc.math.spectra.Apodization;
import io.fair_acc.math.spectra.SpectrumTools;

import org.jetbrains.annotations.NotNull;
import org.jtransforms.fft.DoubleFFT_1D;

/**
 * Some math operation on DataSet, DataSetError and Histogram
 *
 * @author rstein
 */
public final class DataSetMath { // NOPMD - nomen est omen
    private static final char INTEGRAL_SYMBOL = 0x222B;
    private static final char DIFFERENTIAL_SYMBOL = 0x2202;
    private static final char MULTIPLICATION_SYMBOL = 0x00B7;
    private static final String DIFFERENTIAL = DIFFERENTIAL_SYMBOL + "/" + DIFFERENTIAL_SYMBOL + "x";
    public static Formatter<Number> DEFAULT_FORMATTER = new DefaultNumberFormatter(); // NOSONAR NOPMD -- explicitly not getter/setter

    /**
     *
     */
    private DataSetMath() {
        // private function, never called
    }

    public static DoubleErrorDataSet applyMathOperation(final DoubleErrorDataSet ret, final MathOp op, final double x1, final double y1, final double y2, final double eyn1, final double eyp1, final double eyn2, final double eyp2) { // NOPMD NOSONAR
        // switch through math operations
        switch (op) {
        case ADD:
            return ret.add(x1, y1 + y2, MathBase.hypot(eyn1, eyn2), MathBase.hypot(eyp1, eyp2));
        case SUBTRACT:
            return ret.add(x1, y1 - y2, MathBase.hypot(eyn1, eyn2), MathBase.hypot(eyp1, eyp2));
        case MULTIPLY:
            return ret.add(x1, y1 * y2, MathBase.hypot(y2 * eyn1, y1 * eyn2), MathBase.hypot(y2 * eyp1, y1 * eyp2));
        case DIVIDE:
            final double newY = y1 / y2;
            final double nEYN = MathBase.hypot(eyn1 / y2, newY * eyn2 / y2);
            final double nEYP = MathBase.hypot(eyp1 / y2, newY * eyp2 / y2);
            return ret.add(x1, newY, nEYN, nEYP);
        case SQR:
            return ret.add(x1, MathBase.sqr(y1 + y2), 2 * MathBase.abs(y1 + y2) * MathBase.hypot(eyn1, eyn2), 2 * MathBase.abs(y1 + y2) * MathBase.hypot(eyp1, eyp2));
        case SQRT:
            return ret.add(x1, MathBase.sqrt(y1 + y2), MathBase.sqrt(MathBase.abs(y1 + y2)) * MathBase.hypot(eyn1, eyn2), MathBase.sqrt(MathBase.abs(y1 + y2)) * MathBase.hypot(eyp1, eyp2));
        case LOG10:
            double norm = 1.0 / MathBase.log(10);
            final double nEYNLog = y1 + y2 > 0 ? norm / MathBase.abs(y1 + y2) * MathBase.hypot(eyn1, eyn2) : Double.NaN;
            final double nEYPLog = y1 + y2 > 0 ? norm / MathBase.abs(y1 + y2) * MathBase.hypot(eyp1, eyp2) : Double.NaN;
            return ret.add(x1, 10 * MathBase.log10(y1 + y2), nEYNLog, nEYPLog);
        case DB:
            final double normDb = 20.0 / MathBase.log(10);
            final double nEYNDb = y1 + y2 > 0 ? normDb / MathBase.abs(y1 + y2) * MathBase.hypot(eyn1, eyn2) : Double.NaN;
            final double nEYPDb = y1 + y2 > 0 ? normDb / MathBase.abs(y1 + y2) * MathBase.hypot(eyp1, eyp2) : Double.NaN;
            return ret.add(x1, 20 * MathBase.log10(y1 + y2), nEYNDb, nEYPDb);
        case IDENTITY:
        default:
            return ret.add(x1, y1 + y2, eyn1, eyp1);
        }
    }

    // convenience short-hand notation for getting error variables (if defined for dataset)
    private static double error(final DataSet dataSet, final ErrType eType, final int index, final double x,
                                final boolean interpolate) {
        if (!(dataSet instanceof DataSetError)) {
            // data set does not have any error definition
            return 0.0;
        }
        final DataSetError ds = (DataSetError) dataSet;
        if (interpolate) {
            switch (eType) {
            case EXN:
                return ds.getErrorNegative(DIM_X, x);
            case EXP:
                return ds.getErrorPositive(DIM_X, x);
            case EYN:
                return ds.getErrorNegative(DIM_Y, x);
            case EYP:
                return ds.getErrorPositive(DIM_Y, x);
            }
        } else {
            switch (eType) {
            case EXN:
                return ds.getErrorNegative(DIM_X, index);
            case EXP:
                return ds.getErrorPositive(DIM_X, index);
            case EYN:
                return ds.getErrorNegative(DIM_Y, index);
            case EYP:
                return ds.getErrorPositive(DIM_Y, index);
            }
        }

        return 0;
    }

    @SafeVarargs
    public static DataSet addFunction(final DataSet function1, final DataSet function2, @NotNull final Formatter<Number>... format) {
        return mathFunction(function1, function2, MathOp.ADD, format);
    }

    @SafeVarargs
    public static DataSet addFunction(final DataSet function, final double value, @NotNull final Formatter<Number>... format) {
        return mathFunction(function, value, MathOp.ADD, format);
    }

    @SafeVarargs
    public static DataSet addGaussianNoise(final DataSet function, final double sigma, @NotNull final Formatter<Number>... format) {
        final int nLength = function.getDataCount();
        final var dataSetName = getFormatter(format).format("{0}+noise({1})", function.getName(), sigma);
        final var ret = new DoubleErrorDataSet(dataSetName, nLength);

        for (var i = 0; i < nLength; i++) {
            final double x = function.get(DIM_X, i);
            final double y = function.get(DIM_Y, i) + TRandom.Gaus(0, sigma);
            ret.add(x, y, sigma, sigma);
        }

        return ret;
    }

    @SafeVarargs
    public static DataSet averageDataSetsFIR(@NotNull final List<DataSet> dataSets, final int nUpdates, @NotNull final Formatter<Number>... format) {
        if (dataSets.isEmpty()) {
            return new DoubleErrorDataSet(getFormatter(format).format("LP({0}, FIR)", "<empty>"));
        }
        final String functionName = getFormatter(format).format("LP({0}, FIR)", dataSets.get(0).getName());
        if (dataSets.size() <= 1) {
            final var newFunction = dataSets.get(0);
            if (newFunction instanceof DataSetError) {
                return new DoubleErrorDataSet(functionName, newFunction.getValues(DIM_X), newFunction.getValues(DIM_Y),
                        errors(newFunction, EYN), errors(newFunction, EYP), newFunction.getDataCount(), true);
            }

            final int ncount = newFunction.getDataCount();
            return new DoubleErrorDataSet(functionName, newFunction.getValues(DIM_X), newFunction.getValues(DIM_Y),
                    new double[ncount], new double[ncount], ncount, true);
        }

        final int nAvg = min(nUpdates, dataSets.size());
        final var newFunction = dataSets.get(dataSets.size() - 1);
        final var retFunction = new DoubleErrorDataSet(functionName, newFunction.getDataCount() + 2);

        for (var i = 0; i < newFunction.getDataCount(); i++) {
            final double newX = newFunction.get(DIM_X, i);
            var mean = 0.0;
            var variance = 0.0;
            var eyn = 0.0;
            var eyp = 0.0;

            var count = 0;
            for (int j = max(0, dataSets.size() - nAvg); j < dataSets.size(); j++) {
                final var oldFunction = dataSets.get(j);
                final double oldX = oldFunction.get(DIM_X, i);
                final double oldY = oldX == newX ? oldFunction.get(DIM_Y, i) : oldFunction.getValue(DIM_X, newX);
                mean += oldY;
                variance += oldY * oldY;
                // whether we need to interpolate
                final boolean inter = oldX != newX;
                eyn += error(oldFunction, EYN, i, newX, inter);
                eyp += error(oldFunction, EYP, i, newX, inter);
                count++;
            }

            if (count == 0) {
                // cannot compute average
                retFunction.add(newX, Double.NaN, Double.NaN, Double.NaN);
            } else {
                mean /= count;
                eyn /= count;
                eyp /= count;
                variance /= count;
                final double mean2 = mean * mean;
                final double diff = MathBase.abs(variance - mean2);
                eyn = MathBase.sqrt(eyn * eyn + diff);
                eyp = MathBase.sqrt(eyp * eyp + diff);
                retFunction.add(newX, mean, eyn, eyp);
            }
        }

        return retFunction;
    }

    @SafeVarargs
    public static DataSet averageDataSetsIIR(final DataSet prevAverage, final DataSet prevAverage2, final DataSet newDataSet, final int nUpdates, @NotNull final Formatter<Number>... format) {
        final String functionName = getFormatter(format).format("LP({0}, IIR)", newDataSet.getName());
        if (prevAverage == null || prevAverage2 == null || prevAverage.getDataCount() == 0 || prevAverage2.getDataCount() == 0) {
            final double[] yValues = newDataSet.getValues(DIM_Y);
            final double[] eyn = errors(newDataSet, EYN);
            final double[] eyp = errors(newDataSet, EYP);
            if (prevAverage2 instanceof DoubleErrorDataSet) {
                ((DoubleErrorDataSet) prevAverage2).set(newDataSet.getValues(DIM_X), ArrayMath.sqr(yValues), ArrayMath.sqr(eyn), ArrayMath.sqr(eyp));
            } else if (prevAverage2 instanceof DoubleDataSet) {
                ((DoubleDataSet) prevAverage2).set(newDataSet.getValues(DIM_X), ArrayMath.sqr(yValues));
            }

            return new DoubleErrorDataSet(functionName, newDataSet.getValues(DIM_X), yValues, eyn, eyp,
                    newDataSet.getDataCount(), true);
        }
        final int dataCount1 = prevAverage.getDataCount();
        final int dataCount2 = prevAverage2.getDataCount();

        final DoubleErrorDataSet retFunction = dataCount1 == 0
                                                     ? new DoubleErrorDataSet(functionName, newDataSet.getValues(DIM_X), newDataSet.getValues(DIM_Y),
                                                             errors(newDataSet, EYN), errors(newDataSet, EYP), newDataSet.getDataCount(), true)
                                                     : new DoubleErrorDataSet(prevAverage.getName(), prevAverage.getValues(DIM_X), prevAverage.getValues(DIM_Y),
                                                             errors(prevAverage, EYN), errors(prevAverage, EYP), newDataSet.getDataCount(), true);

        final double alpha = 1.0 / (1.0 + nUpdates);
        final boolean avg2Empty = dataCount2 == 0;

        for (var i = 0; i < dataCount1; i++) {
            final double oldX = prevAverage.get(DIM_X, i);
            final double oldY = prevAverage.get(DIM_Y, i);
            final double oldY2 = avg2Empty ? oldY * oldY : prevAverage2.get(DIM_Y, i);
            final double newX = newDataSet.get(DIM_X, i);

            // whether we need to interpolate
            final boolean inter = oldX != newX;

            final double y = inter ? newDataSet.getValue(DIM_Y, oldX) : newDataSet.get(DIM_Y, i);
            final double newVal = (1 - alpha) * oldY + alpha * y;
            final double newVal2 = (1 - alpha) * oldY2 + alpha * (y * y);

            final double eyn = error(newDataSet, EYN, i, newX, inter);
            final double eyp = error(newDataSet, EYP, i, newX, inter);

            if (prevAverage2 instanceof DoubleErrorDataSet) {
                if (avg2Empty) {
                    ((DoubleErrorDataSet) prevAverage2).add(newX, newVal2, eyn, eyp);
                } else {
                    ((DoubleErrorDataSet) prevAverage2).set(i, newX, newVal2, eyn, eyp);
                }
            }
            final double newEYN = MathBase.sqrt(MathBase.abs(newVal2 - MathBase.pow(newVal, 2)) + eyn * eyn);
            final double newEYP = MathBase.sqrt(MathBase.abs(newVal2 - MathBase.pow(newVal, 2)) + eyp * eyp);
            retFunction.set(i, oldX, newVal, newEYN, newEYP);
        }

        return retFunction;
    }

    @SafeVarargs
    public static DataSet dbFunction(final DataSet function, @NotNull final Formatter<Number>... format) {
        return mathFunction(function, 0.0, MathOp.DB, format);
    }

    @SafeVarargs
    public static DataSet dbFunction(final DataSet function1, final DataSet function2, @NotNull final Formatter<Number>... format) {
        return mathFunction(function1, function2, MathOp.DB, format);
    }

    @SafeVarargs
    public static DataSet derivativeFunction(final DataSet function, @NotNull final Formatter<Number>... format) {
        return derivativeFunction(function, +1.0, format);
    }

    @SafeVarargs
    public static DataSet derivativeFunction(final DataSet function, final double sign, @NotNull final Formatter<Number>... format) {
        final String signAdd = sign == 1.0 ? "" : Double.toString(sign) + MULTIPLICATION_SYMBOL;
        final int ncount = function.getDataCount();
        final String functionName = getFormatter(format).format("{0}{1}({2})", signAdd, DIFFERENTIAL, function.getName());
        final var retFunction = new DoubleErrorDataSet(functionName, ncount);

        if (ncount <= 3) {
            return retFunction;
        }
        // TODO: check error estimate for derivative ...

        // // derivative for first point
        // final double y0 = function.get(DIM_Y, 0);
        // final double y1 = function.get(DIM_Y, 0);
        // final double yen0 = error(function, EYN, 0);
        // final double yep0 = error(function, EYP, 0);
        // final double yen1 = error(function, EYN, 1);
        // final double yep1 = error(function, EYP, 1);
        // final double deltaY0 = y1 - y0;
        // final double deltaYEN = MathBase.sqrt(MathBase.pow(yen0, 2) + MathBase.pow(yen1,
        // 2));
        // final double deltaYEP = MathBase.sqrt(MathBase.pow(yep0, 2) + MathBase.pow(yep1,
        // 2));
        // final double deltaX0 = function.get(DIM_X, 1) - function.get(DIM_X, 0);
        // retFunction.add(function.get(DIM_X, 0), sign * (deltaY0 / deltaX0),
        // deltaYEN, deltaYEP);
        for (var i = 0; i < 2; i++) {
            final double x0 = function.get(DIM_X, i);
            retFunction.add(x0, 0, 0, 0);
        }
        for (var i = 2; i < ncount - 2; i++) {
            final double x0 = function.get(DIM_X, i);
            final double stepL = x0 - function.get(DIM_X, i - 1);
            final double stepR = function.get(DIM_X, i + 1) - x0;
            final double valL = function.get(DIM_Y, i - 1);
            final double valC = function.get(DIM_Y, i);
            final double valR = function.get(DIM_Y, i + 1);

            final double yenL = error(function, EYN, i - 1);
            final double yenC = error(function, EYN, i);
            final double yenR = error(function, EYN, i + 1);
            final double yen = MathBase.sqrt(MathBase.sqr(yenL) + MathBase.sqr(yenC) + MathBase.sqr(yenR))
                             / 4;

            final double yepL = error(function, EYP, i - 1);
            final double yepC = error(function, EYP, i);
            final double yepR = error(function, EYP, i + 1);
            final double yep = MathBase.sqrt(MathBase.sqr(yepL) + MathBase.sqr(yepC) + MathBase.sqr(yepR))
                             / 4;

            // simple derivative computation
            final double derivative = 0.5 * ((valC - valL) / stepL + (valR - valC) / stepR);

            retFunction.add(x0, sign * derivative, yen, yep);
        }
        for (int i = ncount - 2; i < ncount; i++) {
            final double x0 = function.get(DIM_X, i);
            retFunction.add(x0, 0, 0, 0);
        }
        // // derivative for last point
        // final int last = ncount - 1;
        // final double deltaYN = function.get(DIM_Y, last) - function.get(DIM_Y, last - 1);
        // final double deltaXN = function.get(DIM_X, last) - function.get(DIM_X, last - 1);
        // retFunction.add(function.get(DIM_X, last), sign * (deltaYN / deltaXN), 0,
        // 0);

        return retFunction;
    }

    @SafeVarargs
    public static DataSet divideFunction(final DataSet function1, final DataSet function2, @NotNull final Formatter<Number>... format) {
        return mathFunction(function1, function2, MathOp.DIVIDE, format);
    }

    @SafeVarargs
    public static DataSet divideFunction(final DataSet function, final double value, @NotNull final Formatter<Number>... format) {
        return mathFunction(function, value, MathOp.DIVIDE, format);
    }

    /**
     * convenience short-hand notation for getting error variables (if defined for dataset)
     *
     * @param dataSet the source data set
     * @param eType the error type
     * @param x the data set x-value for which the error should be interpolated
     * @return the given interpolated error
     */
    public static double error(final DataSet dataSet, final ErrType eType, final double x) {
        return error(dataSet, eType, -1, x, true);
    }

    /**
     * convenience short-hand notation for getting error variables (if defined for dataset)
     *
     * @param dataSet the source data set
     * @param eType the error type
     * @param index the data set index
     * @return the given error
     */
    public static double error(final DataSet dataSet, final ErrType eType, final int index) {
        return error(dataSet, eType, index, 0.0, false);
    }

    /**
     * convenience short-hand notation for getting error variables (if defined for dataset)
     *
     * @param dataSet the source data set
     * @param eType the error type
     * @return the given error array (cropped to data set length if necessary)
     */
    public static double[] errors(final DataSet dataSet, final ErrType eType) {
        final int nDim = dataSet.getDataCount();
        if (!(dataSet instanceof DataSetError)) {
            // data set does not have any error definition
            return new double[nDim];
        }
        final DataSetError ds = (DataSetError) dataSet;
        switch (eType) {
        case EXN:
            return cropToLength(ds.getErrorsNegative(DIM_X), nDim);
        case EXP:
            return cropToLength(ds.getErrorsPositive(DIM_X), nDim);
        case EYN:
            return cropToLength(ds.getErrorsNegative(DIM_Y), nDim);
        case EYP:
        default:
            return cropToLength(ds.getErrorsPositive(DIM_Y), nDim);
        }
    }

    @SafeVarargs
    public static DataSet filterFunction(final DataSet function, final double width, final Filter filterType, @NotNull final Formatter<Number>... format) {
        final int n = function.getDataCount();
        final String dataSetName = getFormatter(format).format("{0}({1},{2})", filterType.getTag(), function.getName(), width);
        final var filteredFunction = new DoubleErrorDataSet(dataSetName, n);
        for (var dim = 0; dim < filteredFunction.getDimension(); dim++) {
            final var refAxisDescription = function.getAxisDescription(dim);
            filteredFunction.getAxisDescription(dim).set(refAxisDescription.getName(), refAxisDescription.getUnit());
        }
        final var subArrayY = new double[n];
        final var subArrayYn = new double[n];
        final var subArrayYp = new double[n];

        final double[] xValues = function.getValues(DIM_X);
        final double[] yValues = function.getValues(DIM_Y);
        final double[] yen = errors(function, EYN);
        final double[] yep = errors(function, EYN);

        for (var i = 0; i < n; i++) {
            final double time0 = xValues[i];

            var count = 0;
            for (var j = 0; j < n; j++) {
                final double time = xValues[j];
                if (MathBase.abs(time0 - time) <= width) {
                    subArrayY[count] = yValues[j];
                    subArrayYn[count] = yen[j];
                    subArrayYp[count] = yep[j];
                    count++;
                }
            }

            final double norm = count > 0 ? 1.0 / MathBase.sqrt(count) : 0.0;

            switch (filterType) {
            case MEDIAN:
                filteredFunction.add(time0, Math.median(subArrayY, count), Math.median(subArrayYn, count),
                        Math.median(subArrayYp, count));
                break;
            case MIN:
                filteredFunction.add(time0, Math.minimum(subArrayY, count), Math.minimum(subArrayYn, count),
                        Math.minimum(subArrayYp, count));
                break;
            case MAX:
                filteredFunction.add(time0, Math.maximum(subArrayY, count), Math.maximum(subArrayYn, count),
                        Math.maximum(subArrayYp, count));
                break;
            case P2P:
                filteredFunction.add(time0, Math.peakToPeak(subArrayY, count), Math.peakToPeak(subArrayYn, count),
                        Math.peakToPeak(subArrayYp, count));
                break;
            case RMS:
                filteredFunction.add(time0, Math.rms(subArrayY, count), Math.rms(subArrayYn, count),
                        Math.rms(subArrayYp, count));
                break;
            case GEOMMEAN:
                filteredFunction.add(time0, Math.geometricMean(subArrayY, 0, count),
                        Math.geometricMean(subArrayYn, 0, count), Math.geometricMean(subArrayYp, 0, count));
                break;
            case MEAN:
            default:
                filteredFunction.add(time0, Math.mean(subArrayY, count), Math.mean(subArrayYn, count) * norm,
                        Math.mean(subArrayYp, count) * norm);
                break;
            }
        }

        return filteredFunction;
    }

    @SafeVarargs
    public static DataSet geometricMeanFilteredFunction(final DataSet function, final double width, @NotNull final Formatter<Number>... format) {
        return filterFunction(function, width, Filter.GEOMMEAN, format);
    }

    @SafeVarargs
    public static DataSet getSubRange(final DataSet function, final double xMin, final double xMax, @NotNull final Formatter<Number>... format) {
        final int nLength = function.getDataCount();
        final String dataSetName = getFormatter(format).format("subRange({0}, {1})", xMin, xMax);
        final var ret = new DoubleErrorDataSet(dataSetName, nLength);

        for (var i = 0; i < nLength; i++) {
            final double x = function.get(DIM_X, i);
            final double y = function.get(DIM_Y, i);
            final double ex = error(function, EXP, i);
            final double ey = error(function, EYP, i);
            if (x >= xMin && x <= xMax) {
                ret.add(x, y, ex, ey);
            }
        }

        return ret;
    }

    @SafeVarargs
    public static DataSet iirLowPassFilterFunction(final DataSet function, final double width, @NotNull final Formatter<Number>... format) {
        final int n = function.getDataCount();
        final String dataSetName = getFormatter(format).format("iir{0}({1},{2})", Filter.MEAN.getTag(), function.getName(), width);
        final var filteredFunction = new DoubleErrorDataSet(dataSetName, n);
        if (n <= 1) {
            if (!(function instanceof GridDataSet)) {
                filteredFunction.set(function);
                return filteredFunction;
            }
            filteredFunction.set(function.getValues(DIM_X), function.getValues(DIM_Y), errors(function, EYN),
                    errors(function, EYP));
            for (var index = 0; index < function.getDataCount(); index++) {
                final String label = function.getDataLabel(index);
                if (label != null && !label.isEmpty()) {
                    filteredFunction.addDataLabel(index, label);
                }
            }
            for (var index = 0; index < function.getDataCount(); index++) {
                final String style = function.getStyle(index);
                if (style != null && !style.isEmpty()) {
                    filteredFunction.addDataStyle(index, style);
                }
            }
            filteredFunction.setStyle(function.getStyle());
            return filteredFunction;
        }
        final double[] xValues = function.getValues(DIM_X);
        final double[] yValues = function.getValues(DIM_Y);
        final double[] yen = errors(function, EYN);
        final double[] yep = errors(function, EYN);

        final var yUp = new double[n];
        final var yDown = new double[n];
        final var ye1 = new double[n];
        final var ye2 = new double[n];

        final double smoothing = 0.5 * width;
        // IIR smoothing algorithm:
        // smoothed += elapsedTime * ( newValue - smoothed ) / smoothing
        double smoothed = yValues[0];
        double smoothed2 = smoothed * smoothed;
        // for (int i = 1; i < n; i++) {
        // final double x0 = xValues[i - 1];
        // final double x1 = xValues[i];
        // final double y = yValues[i];
        // smoothed += (x1 - x0) * (y - smoothed) / smoothing;
        // smoothed2 += (x1 - x0) * (y * y - smoothed2) / smoothing;
        // final double newEYN = MathBase.sqrt(MathBase.abs(smoothed2 - smoothed *
        // smoothed) + yen[i] * yen[i]);
        // final double newEYP = MathBase.sqrt(MathBase.abs(smoothed2 - smoothed *
        // smoothed) + yep[i] * yep[i]);
        //
        // filteredFunction.add(x1 - smoothing, smoothed, newEYN, newEYP);
        // }

        // calculate forward/backward to compensate for the IIR group-delay
        for (var i = 1; i < n; i++) {
            final double x0 = xValues[i - 1];
            final double x1 = xValues[i];
            final double y = yValues[i];
            smoothed += (x1 - x0) * (y - smoothed) / smoothing;
            smoothed2 += (x1 - x0) * (y * y - smoothed2) / smoothing;
            yUp[i] = smoothed;
            ye1[i] = smoothed2;
        }
        smoothed = yValues[n - 1];
        smoothed2 = smoothed * smoothed;
        for (var i = n - 2; i >= 0; i--) {
            final double x0 = xValues[i];
            final double x1 = xValues[i + 1];
            final double y = yValues[i];
            smoothed += (x1 - x0) * (y - smoothed) / smoothing;
            smoothed2 += (x1 - x0) * (y * y - smoothed2) / smoothing;
            yDown[i] = smoothed;
            ye2[i] = smoothed2;
        }

        filteredFunction.add(xValues[0], yValues[0], yen[0], yep[0]);
        for (var i = 1; i < n; i++) {
            final double x1 = xValues[i];
            final double y = 0.5 * (yUp[i] + yDown[i]);
            final double mean2 = y * y;
            final double y2 = 0.5 * MathBase.pow(ye1[i] + ye2[i], 1);
            final double avgError2 = MathBase.abs(y2 - mean2);
            final double newEYN = MathBase.sqrt(avgError2 + yen[i] * yen[i]);
            final double newEYP = MathBase.sqrt(avgError2 + yep[i] * yep[i]);

            filteredFunction.add(x1, y, newEYN, newEYP);
        }

        return filteredFunction;
    }

    public static DoublePointError integral(final DataSet function) {
        final DataSet integratedFunction = integrateFunction(function);
        final var lastPoint = integratedFunction.getDataCount() - 1;
        if (lastPoint <= 0) {
            return new DoublePointError(0.0, 0.0, 0.0, 0.0);
        }
        final double x = integratedFunction.get(DIM_X, lastPoint);
        final double y = integratedFunction.get(DIM_Y, lastPoint);
        final double yen = error(integratedFunction, EYN, lastPoint);
        final double yep = error(integratedFunction, EYP, lastPoint);
        final double ye = 0.5 * (yen + yep);

        return new DoublePointError(x, y, 0.0, ye);
    }

    public static DoublePointError integral(final DataSet function, final double xMin, final double xMax) {
        final DataSet integratedFunction = integrateFunction(function, xMin, xMax);
        final var lastPoint = integratedFunction.getDataCount() - 1;
        final double yen = error(integratedFunction, EYN, lastPoint);
        final double yep = error(integratedFunction, EYP, lastPoint);
        final double ye = 0.5 * (yen + yep);

        return new DoublePointError(integratedFunction.get(DIM_X, lastPoint), integratedFunction.get(DIM_Y, lastPoint), 0.0, ye);
    }

    public static double integralSimple(final DataSet function) {
        var integral1 = 0.0;
        var integral2 = 0.0;
        final int nCount = function.getDataCount();
        if (nCount <= 1) {
            return 0.0;
        }
        for (var i = 1; i < nCount; i++) {
            final double step = function.get(DIM_X, i) - function.get(DIM_X, i - 1);
            final double val1 = function.get(DIM_Y, i - 1);
            final double val2 = function.get(DIM_Y, i);

            integral1 += step * val1;
            integral2 += step * val2;
        }
        return 0.5 * (integral1 + integral2);
    }

    @SafeVarargs
    public static DataSet integrateFunction(final DataSet function, @NotNull final Formatter<Number>... format) {
        return integrateFunction(function, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, format);
    }

    @SafeVarargs
    public static DataSet integrateFunction(final DataSet function, final double xMin, final double xMax, @NotNull final Formatter<Number>... format) {
        final int nLength = function.getDataCount();
        final var pattern = "{0}({1})dyn|_'{'{2}'}'^'{'{3}'}'";
        final String dataSetName = getFormatter(format).format(pattern, INTEGRAL_SYMBOL, function.getName(), xMin, xMax);
        if (nLength <= 0) {
            if (!(function instanceof GridDataSet) || function.getDimension() > 2) {
                final int ncount = function.getDataCount();
                final var emptyVector = new double[ncount];
                return new DoubleErrorDataSet(dataSetName, function.getValues(DIM_X), emptyVector, emptyVector,
                        emptyVector, ncount, true);
            }
            throw new IllegalStateException("not yet implemented for non 2D dataSets");
        }

        if (!function.getAxisDescription(DIM_X).isDefined()) {
            function.recomputeLimits(DIM_X);
        }
        double xMinLocal = function.getAxisDescription(DIM_X).getMin();
        double xMaxLocal = function.getAxisDescription(DIM_X).getMax();
        var sign = 1.0;
        if (Double.isFinite(xMin) && Double.isFinite(xMax)) {
            xMinLocal = min(xMin, xMax);
            xMaxLocal = max(xMin, xMax);
            if (xMin > xMax) {
                sign = -1;
            }
        } else if (Double.isFinite(xMin)) {
            xMinLocal = xMin;
        } else if (Double.isFinite(xMax)) {
            xMaxLocal = xMax;
        }
        final var retFunction = new DoubleErrorDataSet(dataSetName, nLength);
        if (nLength <= 1) {
            retFunction.add(function.get(DIM_X, 0), 0, 0, 0);
            return retFunction;
        }

        var integral = 0.0;
        var integralEN = 0.0;
        var integralEP = 0.0;

        if (Double.isFinite(xMin) && xMin <= function.get(DIM_X, 0)) {
            // interpolate before range where discrete function is defined
            final double val1 = function.getValue(DIM_Y, xMin);
            final double x1 = function.get(DIM_X, 0);
            final double val2 = function.get(DIM_Y, 0);
            final double step = x1 - xMin;
            integral += sign * 0.5 * step * (val1 + val2);
            final double en1 = error(function, EYN, 0);
            final double ep1 = error(function, EYP, 0);

            // assuming uncorrelated errors between bins
            integralEN = MathBase.hypot(integralEN, step * en1);
            integralEP = MathBase.hypot(integralEP, step * ep1);

            retFunction.add(xMin, integral, 0, 0);
        }

        retFunction.add(function.get(DIM_X, 0), integral, integralEN, integralEP);
        for (var i = 1; i < nLength; i++) {
            final double x0 = function.get(DIM_X, i - 1);
            final double x1 = function.get(DIM_X, i);
            double step = x1 - x0;
            final double y0 = function.get(DIM_Y, i - 1);
            final double en1 = error(function, EYN, i - 1);
            final double ep1 = error(function, EYP, i - 1);
            final double y1 = function.get(DIM_Y, i);
            final double en2 = error(function, EYN, i);
            final double ep2 = error(function, EYP, i);

            // simple triangulation integration
            if (x0 >= xMinLocal && x1 <= xMaxLocal) {
                integral += sign * 0.5 * step * (y0 + y1);

                // assuming uncorrelated errors between bins
                integralEN = MathBase.hypot(integralEN, 0.5 * step * (en1 + en2));
                integralEP = MathBase.hypot(integralEP, 0.5 * step * (ep1 + ep2));

            } else if (x1 < xMinLocal && x0 < xMinLocal) { // NOSONAR NOPMD
                // see below
            } else if (x0 < xMinLocal && x1 > xMinLocal) {
                retFunction.add(xMin, integral, integralEN, integralEP);
                step = x1 - xMinLocal;
                integral += sign * 0.5 * step * (function.getValue(DIM_Y, xMinLocal) + y1);

                // assuming uncorrelated errors between bins
                integralEN = MathBase.hypot(integralEN, 0.5 * step * (en1 + en2));
                integralEP = MathBase.hypot(integralEP, 0.5 * step * (ep1 + ep2));
            } else if (x0 < xMaxLocal && x1 > xMaxLocal) {
                step = xMaxLocal - x0;
                final double yAtMax = function.getValue(DIM_Y, xMaxLocal);
                integral += sign * 0.5 * step * (y0 + yAtMax);

                // assuming uncorrelated errors between bins
                integralEN = MathBase.hypot(integralEN, 0.5 * step * (en1 + en2));
                integralEP = MathBase.hypot(integralEP, 0.5 * step * (ep1 + ep2));

                retFunction.add(xMaxLocal, integral, integralEN, integralEP);
            }

            retFunction.add(x1, integral, integralEN, integralEP);
        }

        if (Double.isFinite(xMax) && xMax > function.get(DIM_X, nLength - 1)) {
            // interpolate after range where discrete function is defined
            final double x0 = function.get(DIM_X, nLength - 1);
            final double val1 = function.get(DIM_Y, nLength - 1);
            final double val2 = function.getValue(DIM_Y, xMax);
            final double step = xMax - x0;
            final double en1 = error(function, EYN, nLength - 1);
            final double ep1 = error(function, EYP, nLength - 1);
            // assuming uncorrelated errors between bins
            integralEN = MathBase.hypot(integralEN, step * en1);
            integralEP = MathBase.hypot(integralEP, step * ep1);

            integral += 0.5 * step * (val1 + val2);
            retFunction.add(xMax, integral, integralEN, integralEP);
        }

        return retFunction;
    }

    @SafeVarargs
    public static DataSet integrateFromCentre(@NotNull DataSet function, final double centre, final double width, final boolean normalise, @NotNull final Formatter<Number>... format) {
        final double xMinLocal = function.getAxisDescription(DIM_X).getMin();
        final double xMaxLocal = function.getAxisDescription(DIM_X).getMax();
        final double centreLocal = Double.isFinite(centre) ? centre : SimpleDataSetEstimators.computeCentreOfMass(function, 0, function.getDataCount());

        final var pattern = "{0}({1},c={2})dyn|_'{'c-{3}'}'^'{'c+{3}'}'";
        final int nLength = function.getDataCount();
        final String dataSetName = getFormatter(format).format(pattern, INTEGRAL_SYMBOL, function.getName(), centreLocal, width);
        if (nLength < 2) { // need at least two
            if (!(function instanceof GridDataSet) || function.getDimension() > 2) {
                final int ncount = function.getDataCount();
                final var emptyVector = new double[ncount];
                return new DoubleErrorDataSet(dataSetName, function.getValues(DIM_X), emptyVector, emptyVector, emptyVector, ncount, true);
            }
            throw new IllegalStateException("not yet implemented for non 2D dataSets");
        }
        final var retFunction = new DoubleErrorDataSet(dataSetName, nLength / 2);
        if (width <= 0.0) {
            return retFunction;
        }
        if (centreLocal <= xMinLocal || centreLocal >= xMaxLocal) {
            throw new IllegalArgumentException(String.format("centre %f is outside DataSetRange [%f,%f]", centreLocal, xMinLocal, xMaxLocal));
        }

        final double scaleLocal = normalise ? 1.0 / integral(function, max(centreLocal - width, xMinLocal), min(centreLocal + width, xMaxLocal)).getY() : 1.0;
        final var centreLocalIndex = function.getIndex(DIM_X, centreLocal);
        final var maxIntIndex = min(centreLocalIndex, function.getDataCount() - 1 - centreLocalIndex);
        var integral = 0.0;
        for (var i = 0; i < maxIntIndex; i++) {
            // x-values
            final double xL0 = function.get(DIM_X, centreLocalIndex - i - 1);
            final double xL1 = function.get(DIM_X, centreLocalIndex - i);
            final double stepL = xL1 - xL0;

            final double xR0 = function.get(DIM_X, centreLocalIndex + i);
            final double xR1 = function.get(DIM_X, centreLocalIndex + i + 1);
            final double stepR = xR1 - xR0;

            // y-values
            final double yL0 = function.get(DIM_Y, centreLocalIndex - i - 1);
            final double yL1 = function.get(DIM_Y, centreLocalIndex - i);
            final double yR0 = function.get(DIM_Y, centreLocalIndex + i);
            final double yR1 = function.get(DIM_Y, centreLocalIndex + i + 1);

            // simple triangulation integration
            integral += scaleLocal * 0.5 * (stepL * (yL0 + yL1) + stepR * (yR0 + yR1));

            final double distanceFromCentre = 0.5 * (xR1 - xL0);
            retFunction.add(distanceFromCentre, integral, 0, 0);
        }

        return retFunction;
    }

    public static double integralWidth(@NotNull DataSet function, final double centre, final double maxWidth, final double threshold) {
        return SimpleDataSetEstimators.getZeroCrossing(integrateFromCentre(function, centre, maxWidth, true), threshold);
    }

    @SafeVarargs
    public static DataSet inversedbFunction(final DataSet function, @NotNull final Formatter<Number>... format) {
        return mathFunction(function, 1.0, MathOp.INV_DB, format);
    }

    @SafeVarargs
    public static DataSet log10Function(final DataSet function, @NotNull final Formatter<Number>... format) {
        return mathFunction(function, 0.0, MathOp.LOG10, format);
    }

    @SafeVarargs
    public static DataSet log10Function(final DataSet function1, final DataSet function2, @NotNull final Formatter<Number>... format) {
        return mathFunction(function1, function2, MathOp.LOG10, format);
    }

    @SafeVarargs
    public static DataSet lowPassFilterFunction(final DataSet function, final double width, @NotNull final Formatter<Number>... format) {
        return filterFunction(function, width, Filter.MEAN, format);
    }

    @SafeVarargs
    public static DataSet magnitudeSpectrum(final DataSet function, @NotNull final Formatter<Number>... format) {
        return magnitudeSpectrum(function, Apodization.Hann, false, false, format);
    }

    @SafeVarargs
    public static DataSet magnitudeSpectrum(final DataSet function, final Apodization apodization, final boolean dbScale, final boolean normalisedFrequency, @NotNull final Formatter<Number>... format) {
        final String functionName = getFormatter(format).format("Mag{0}({1})", dbScale ? "[dB]" : "", function.getName());
        final int n = function.getDataCount();

        if (n == 0) {
            return new DoubleErrorDataSet(functionName, 0);
        }

        final var fastFourierTrafo = new DoubleFFT_1D(n);

        // N.B. since realForward computes the FFT in-place -> generate a copy
        final var fftSpectra = new double[n];
        for (var i = 0; i < n; i++) {
            final double window = apodization.getIndex(i, n);
            fftSpectra[i] = function.get(DIM_Y, i) * window;
        }

        fastFourierTrafo.realForward(fftSpectra);
        final var mag = dbScale ? SpectrumTools.computeMagnitudeSpectrum_dB(fftSpectra, true) : SpectrumTools.computeMagnitudeSpectrum(fftSpectra, true);
        final var dt = function.get(DIM_X, function.getDataCount() - 1) - function.get(DIM_X, 0);
        final var fsampling = normalisedFrequency || dt <= 0 ? 0.5 / mag.length : 1.0 / dt;

        final var ret = new DoubleErrorDataSet(functionName, mag.length);
        for (var i = 0; i < mag.length; i++) {
            // TODO: consider magnitude error estimate
            ret.add(i * fsampling, mag[i], 0, 0);
        }

        return ret;
    }

    @SafeVarargs
    public static DataSet magnitudeSpectrumComplex(final DataSet function, @NotNull final Formatter<Number>... format) {
        return magnitudeSpectrumComplex(function, Apodization.Hann, false, false, format);
    }

    @SafeVarargs
    public static DataSet magnitudeSpectrumComplex(final DataSet function, final Apodization apodization,
            final boolean dbScale, final boolean normalisedFrequency, @NotNull final Formatter<Number>... format) {
        final int n = function.getDataCount();

        final var fastFourierTrafo = new DoubleFFT_1D(n);

        // N.B. since realForward computes the FFT in-place -> generate a copy
        final var fftSpectra = new double[2 * n];
        for (var i = 0; i < n; i++) {
            final double window = apodization.getIndex(i, n);
            fftSpectra[2 * i] = function.get(DIM_Y, i) * window;
            fftSpectra[2 * i + 1] = function.get(DIM_Z, i) * window;
        }

        fastFourierTrafo.complexForward(fftSpectra);
        final var mag = dbScale ? SpectrumTools.computeMagnitudeSpectrum_dB(fftSpectra, true)
                                : SpectrumTools.computeMagnitudeSpectrum(fftSpectra, true);
        final var dt = function.get(DIM_X, function.getDataCount() - 1) - function.get(DIM_X, 0);
        final var fsampling = normalisedFrequency || dt <= 0 ? 0.5 / mag.length : 1.0 / dt;

        final var functionName = getFormatter(format).format("Mag{0}({1})", dbScale ? "[dB]" : "", function.getName());
        final var ret = new DoubleErrorDataSet(functionName, mag.length);
        for (var i = 0; i < mag.length; i++) {
            // TODO: consider magnitude error estimate
            if (i < mag.length / 2) {
                ret.add((i - mag.length / 2.0) * fsampling, mag[i + mag.length / 2], 0, 0);
            } else {
                ret.add((i - mag.length / 2.0) * fsampling, mag[i - mag.length / 2], 0, 0);
            }
        }

        return ret;
    }

    public static DataSet magnitudeSpectrumDecibel(final DataSet function) {
        return magnitudeSpectrum(function, Apodization.Hann, true, false);
    }

    public static boolean sameHorizontalBase(final DataSet function1, final DataSet function2) {
        if (function1.getDataCount() != function2.getDataCount()) {
            return false;
        }
        for (var i = 0; i < function1.getDataCount(); i++) {
            final double X1 = function1.get(DIM_X, i);
            final double X2 = function2.get(DIM_X, i);
            if (X1 != X2) {
                return false;
            }
        }

        return true;
    }

    @SafeVarargs
    public static DataSet mathFunction(final DataSet function1, final DataSet function2, final MathOp op, @NotNull final Formatter<Number>... format) {
        final String newDataSetName = getFormatter(format).format("{0}{1}{2}", function1.getName(), op.getTag(), function2.getName());
        final var ret = new DoubleErrorDataSet(newDataSetName, function1.getDataCount());
        ret.getAxisDescription(DIM_X).set(function1.getAxisDescription(DIM_X));
        ret.getAxisDescription(DIM_Y).set(function1.getAxisDescription(DIM_Y).getName(), function1.getAxisDescription(DIM_Y).getUnit());

        final boolean needsInterpolation = !sameHorizontalBase(function1, function2);
        if (needsInterpolation) {
            final List<Double> xValues = getCommonBase(function1, function2);
            for (double x : xValues) {
                final double Y1 = function1.getValue(DIM_Y, x);
                final double Y2 = function2.getValue(DIM_Y, x);
                final double eyn1 = error(function1, EYN, 0, x, needsInterpolation);
                final double eyp1 = error(function1, EYP, 0, x, needsInterpolation);
                final double eyn2 = error(function2, EYN, 0, x, needsInterpolation);
                final double eyp2 = error(function2, EYP, 0, x, needsInterpolation);
                applyMathOperation(ret, op, x, Y1, Y2, eyn1, eyp1, eyn2, eyp2);
            }
            return ret;
        }

        for (var i = 0; i < function1.getDataCount(); i++) {
            final double X1 = function1.get(DIM_X, i);
            // not needed : final double X2 = function1.get(DIM_X, i) ...
            final double Y1 = function1.get(DIM_Y, i);
            final double Y2 = function2.get(DIM_Y, i);
            final double eyn1 = error(function1, EYN, i);
            final double eyp1 = error(function1, EYP, i);
            final double eyn2 = error(function2, EYN, i, X1, needsInterpolation);
            final double eyp2 = error(function2, EYP, i, X1, needsInterpolation);
            applyMathOperation(ret, op, X1, Y1, Y2, eyn1, eyp1, eyn2, eyp2);
        }

        return ret;
    }

    public static List<Double> getCommonBase(final DataSet... functions) {
        final List<Double> xValues = new NoDuplicatesList<>();
        for (DataSet function : functions) {
            for (var i = 0; i < function.getDataCount(); i++) {
                if (function instanceof Histogram) {
                    xValues.add(((Histogram) function).getBinLimits(DIM_X, LOWER, i));
                    xValues.add(((Histogram) function).getBinCenter(DIM_X, i));
                    xValues.add(((Histogram) function).getBinLimits(DIM_X, UPPER, i));
                } else {
                    xValues.add(function.get(DIM_X, i));
                }
            }
        }
        Collections.sort(xValues);
        return xValues;
    }

    @SafeVarargs
    public static DataSet mathFunction(final DataSet function, final double value, final MathOp op, @NotNull final Formatter<Number>... format) {
        final String functionName = getFormatter(format).format("{0}({1})", op.getTag(), function.getName());
        final double[] y = function.getValues(DIM_Y);
        final double[] eyn = Arrays.copyOf(errors(function, EYN), y.length);
        final double[] eyp = Arrays.copyOf(errors(function, EYP), y.length);

        final int ncount = function.getDataCount();
        switch (op) {
        case ADD:
            return new DoubleErrorDataSet(functionName, function.getValues(DIM_X), ArrayMath.add(y, value), eyn, eyp,
                    ncount, true);
        case SUBTRACT:
            return new DoubleErrorDataSet(functionName, function.getValues(DIM_X), ArrayMath.subtract(y, value), eyn, eyp,
                    ncount, true);
        case MULTIPLY:
            return new DoubleErrorDataSet(functionName, function.getValues(DIM_X), ArrayMath.multiply(y, value),
                    ArrayMath.multiply(eyn, value), ArrayMath.multiply(eyp, value), ncount, true);
        case DIVIDE:
            return new DoubleErrorDataSet(functionName, function.getValues(DIM_X), ArrayMath.divide(y, value),
                    ArrayMath.divide(eyn, value), ArrayMath.divide(eyp, value), ncount, true);
        case SQR:
            for (var i = 0; i < eyn.length; i++) {
                eyn[i] = 2 * MathBase.abs(y[i] + value) * eyn[i];
                eyp[i] = 2 * MathBase.abs(y[i] + value) * eyp[i];
            }
            return new DoubleErrorDataSet(functionName, function.getValues(DIM_X), value == 0.0 ? ArrayMath.sqr(y) : ArrayMath.sqr(ArrayMath.add(y, value)), eyn, eyp, ncount,
                    true);
        case SQRT:
            for (var i = 0; i < eyn.length; i++) {
                eyn[i] = MathBase.sqrt(MathBase.abs(y[i] + value)) * eyn[i];
                eyp[i] = MathBase.sqrt(MathBase.abs(y[i] + value)) * eyp[i];
            }
            return new DoubleErrorDataSet(functionName, function.getValues(DIM_X), value == 0.0 ? ArrayMath.sqrt(y) : ArrayMath.sqrt(ArrayMath.add(y, value)), eyn, eyp, ncount,
                    true);
        case LOG10:
            for (var i = 0; i < eyn.length; i++) {
                eyn[i] = 0.0; // 0.0 as a work-around
                eyp[i] = 0.0;
            }
            return new DoubleErrorDataSet(functionName, function.getValues(DIM_X), ArrayMath.tenLog10(y), eyn, eyp,
                    ncount, true);
        case DB:
            for (var i = 0; i < eyn.length; i++) {
                eyn[i] = 0.0; // 0.0 as a work-around
                eyp[i] = 0.0;
            }
            return new DoubleErrorDataSet(functionName, function.getValues(DIM_X), ArrayMath.decibel(y), eyn, eyp, ncount,
                    true);
        case INV_DB:
            for (var i = 0; i < eyn.length; i++) {
                eyn[i] = 0.0; // 0.0 as a work-around
                eyp[i] = 0.0;
            }
            return new DoubleErrorDataSet(functionName, function.getValues(DIM_X), ArrayMath.inverseDecibel(y), eyn, eyp,
                    ncount, true);
        case IDENTITY:
        default:
            // return copy if nothing else matches
            return new DoubleErrorDataSet(functionName, function.getValues(DIM_X), function.getValues(DIM_Y),
                    errors(function, EYN), errors(function, EYP), ncount, true);
        }
    }

    @SafeVarargs
    public static DataSet maxFilteredFunction(final DataSet function, final double width, @NotNull final Formatter<Number>... format) {
        return filterFunction(function, width, Filter.MAX, format);
    }

    @SafeVarargs
    public static DataSet medianFilteredFunction(final DataSet function, final double width, @NotNull final Formatter<Number>... format) {
        return filterFunction(function, width, Filter.MEDIAN, format);
    }

    @SafeVarargs
    public static DataSet minFilteredFunction(final DataSet function, final double width, @NotNull final Formatter<Number>... format) {
        return filterFunction(function, width, Filter.MIN, format);
    }

    @SafeVarargs
    public static DataSet multiplyFunction(final DataSet function1, final DataSet function2, @NotNull final Formatter<Number>... format) {
        return mathFunction(function1, function2, MathOp.MULTIPLY, format);
    }

    @SafeVarargs
    public static DataSet multiplyFunction(final DataSet function, final double value, @NotNull final Formatter<Number>... format) {
        return mathFunction(function, value, MathOp.MULTIPLY, format);
    }

    @SafeVarargs
    public static DataSet normalisedFunction(final DataSet function, @NotNull final Formatter<Number>... format) {
        return normalisedFunction(function, 1.0, format);
    }

    @SafeVarargs
    public static DataSet normalisedFunction(final DataSet function, final double requiredIntegral, @NotNull final Formatter<Number>... format) {
        final DoublePointError complexInt = integral(function);
        final double integral = complexInt.getY() / requiredIntegral;
        final int ncount = function.getDataCount();
        // final double integralErr = complexInt.getErrorY() / requiredIntegral;
        // TODO: add error propagation to normalised function error estimate
        final String functionName = getFormatter(format).format("{0}", function.getName());
        if (integral == 0) {
            return new DoubleErrorDataSet(functionName, function.getValues(DIM_X), new double[ncount],
                    new double[ncount], new double[ncount], ncount, true);
        }
        final var xValues = function.getValues(DIM_X);
        final var yValues = ArrayMath.divide(function.getValues(DIM_Y), integral);
        final var eyp = ArrayMath.divide(errors(function, EYN), integral);
        final var eyn = ArrayMath.divide(errors(function, EYP), integral);

        return new DoubleErrorDataSet(functionName, xValues, yValues, eyp, eyn, ncount, true);
    }

    @SafeVarargs
    public static DataSet normalisedMagnitudeSpectrumDecibel(final DataSet function, @NotNull final Formatter<Number>... format) {
        return magnitudeSpectrum(function, Apodization.Hann, true, true, format);
    }

    @SafeVarargs
    public static DataSet peakToPeakFilteredFunction(final DataSet function, final double width, @NotNull final Formatter<Number>... format) {
        return filterFunction(function, width, Filter.P2P, format);
    }

    @SafeVarargs
    public static DataSet rmsFilteredFunction(final DataSet function, final double width, @NotNull final Formatter<Number>... format) {
        return filterFunction(function, width, Filter.RMS, format);
    }

    public static EditableDataSet setFunction(final EditableDataSet function, final double value, final double xMin, final double xMax) {
        final int nLength = function.getDataCount();
        double xMinLocal = function.get(DIM_X, 0);
        double xMaxLocal = function.get(DIM_X, nLength - 1);

        if (Double.isFinite(xMin) && Double.isFinite(xMax)) {
            xMinLocal = min(xMin, xMax);
            xMaxLocal = max(xMin, xMax);
        } else if (Double.isFinite(xMin)) {
            xMinLocal = xMin;
        } else if (Double.isFinite(xMax)) {
            xMaxLocal = xMax;
        }

        for (var i = 0; i < nLength; i++) {
            final double x = function.get(DIM_X, i);
            if (x >= xMinLocal && x <= xMaxLocal) {
                function.set(i, x, value);
            }
        }

        return function;
    }

    @SafeVarargs
    public static DataSet sqrFunction(final DataSet function, final double value, @NotNull final Formatter<Number>... format) {
        return mathFunction(function, value, MathOp.SQR, format);
    }

    @SafeVarargs
    public static DataSet sqrFunction(final DataSet function1, final DataSet function2, @NotNull final Formatter<Number>... format) {
        return mathFunction(function1, function2, MathOp.SQR, format);
    }

    @SafeVarargs
    public static DataSet sqrtFunction(final DataSet function, final double value, @NotNull final Formatter<Number>... format) {
        return mathFunction(function, value, MathOp.SQRT, format);
    }

    @SafeVarargs
    public static DataSet sqrtFunction(final DataSet function1, final DataSet function2, @NotNull final Formatter<Number>... format) {
        return mathFunction(function1, function2, MathOp.SQRT, format);
    }

    @SafeVarargs
    public static DataSet subtractFunction(final DataSet function1, final DataSet function2, @NotNull final Formatter<Number>... format) {
        return mathFunction(function1, function2, MathOp.SUBTRACT, format);
    }

    @SafeVarargs
    public static DataSet subtractFunction(final DataSet function, final double value, @NotNull final Formatter<Number>... format) {
        return mathFunction(function, value, MathOp.SUBTRACT, format);
    }

    @SafeVarargs
    private static Formatter<Number> getFormatter(@NotNull final Formatter<Number>... format) {
        return Objects.requireNonNull(format, "user-supplied format").length > 0 ? format[0] : DEFAULT_FORMATTER;
    }

    private static double[] cropToLength(final double[] in, final int length) {
        // small helper routine to crop data array in case it's to long
        if (in.length == length) {
            return in;
        }
        return Arrays.copyOf(in, length);
    }

    public enum ErrType {
        EXN,
        EXP,
        EYN,
        EYP
    }

    public enum Filter {
        MEAN("LowPass"),
        MEDIAN("Median"),
        MIN("Min"),
        MAX("Max"),
        P2P("PeakToPeak"),
        RMS("RMS"),
        GEOMMEAN("GeometricMean");

        private final String tag;

        Filter(final String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }
    }

    public enum MathOp {
        ADD("+"),
        SUBTRACT("-"),
        MULTIPLY("*"),
        DIVIDE("*"),
        SQR("SQR"),
        SQRT("SQRT"),
        LOG10("Log10"),
        DB("dB"),
        INV_DB("dB^{-1}"),
        IDENTITY("Copy");

        private final String tag;

        MathOp(final String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }
    }
}
