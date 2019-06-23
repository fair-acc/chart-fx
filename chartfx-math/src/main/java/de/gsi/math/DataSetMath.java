package de.gsi.math;

import static de.gsi.math.DataSetMath.ErrType.EXP;
import static de.gsi.math.DataSetMath.ErrType.EYN;
import static de.gsi.math.DataSetMath.ErrType.EYP;

import java.util.Arrays;
import java.util.List;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.EditableDataSet;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.spi.utils.DoublePointError;
import de.gsi.math.spectra.Apodization;
import de.gsi.math.spectra.SpectrumTools;
import de.gsi.math.spectra.fft.DoubleFFT_1D;

/**
 * Some math operation on DataSet and DataSetError
 *
 * @author rstein
 */
public final class DataSetMath {

    private static final char INFINITY_SYMBOL = 0x221E;
    private static final char INTEGRAL_SYMBOL = 0x222B;
    private static final char DIFFERENTIAL_SYMBOL = 0x2202;
    private static final char MULTIPLICATION_SYMBOL = 0x00B7;
    private static final String DIFFERENTIAL = DIFFERENTIAL_SYMBOL + "/" + DIFFERENTIAL_SYMBOL + "x";
    private static final TRandom random = new TRandom(System.currentTimeMillis());

    /**
     *
     */
    private DataSetMath() {
        // private function, never called
    }

    public enum ErrType {
        EXN,
        EXP,
        EYN,
        EYP;
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
     * @param x the data set x-value for which the error should be interpolated
     * @return the given interpolated error
     */
    public static double error(final DataSet dataSet, final ErrType eType, final double x) {
        return error(dataSet, eType, -1, x, true);
    }

    // convenience short-hand notation for getting error variables (if defined for dataset)
    protected static double error(final DataSet dataSet, final ErrType eType, final int index, final double x,
            final boolean interpolate) {
        if (!(dataSet instanceof DataSetError)) {
            // data set does not have any error definition
            return 0.0;
        }
        final DataSetError ds = (DataSetError) dataSet;
        if (interpolate) {
            switch (eType) {
            case EXN:
                return ds.getXErrorNegative(x);
            case EXP:
                return ds.getXErrorPositive(x);
            case EYN:
                return ds.getYErrorNegative(x);
            case EYP:
                return ds.getYErrorPositive(x);
            }
        } else {
            switch (eType) {
            case EXN:
                return ds.getXErrorNegative(index);
            case EXP:
                return ds.getXErrorPositive(index);
            case EYN:
                return ds.getYErrorNegative(index);
            case EYP:
                return ds.getYErrorPositive(index);
            }
        }

        return 0;
    }

    protected static double[] cropToLength(final double[] in, final int length) {
        // small helper routine to crop data array in case it's to long
        if (in.length == length) {
            return in;
        }
        return Arrays.copyOf(in, length);
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
            return cropToLength(ds.getXErrorsNegative(), nDim);
        case EXP:
            return cropToLength(ds.getXErrorsPositive(), nDim);
        case EYN:
            return cropToLength(ds.getYErrorsNegative(), nDim);
        case EYP:
        default:
            return cropToLength(ds.getYErrorsPositive(), nDim);
        }
    }

    public static DataSetError averageDataSetsIIR(final DataSet prevAverage, final DataSet prevAverage2,
            final DataSet newDataSet, final int nUpdates) {
        final String functionName = "LP(" + newDataSet.getName() + ", IIR)";
        if (prevAverage == null || prevAverage2 == null || prevAverage.getDataCount() == 0
                || prevAverage2.getDataCount() == 0) {

            final double[] yValues = newDataSet.getYValues();
            final double[] eyn = errors(newDataSet, EYN);
            final double[] eyp = errors(newDataSet, EYP);
            if (prevAverage2 instanceof DoubleErrorDataSet) {
                ((DoubleErrorDataSet) prevAverage2).set(newDataSet.getXValues(), ArrayMath.sqr(yValues),
                        ArrayMath.sqr(eyn), ArrayMath.sqr(eyp));
            } else if (prevAverage2 instanceof DoubleDataSet) {
                ((DoubleDataSet) prevAverage2).set(newDataSet.getXValues(), ArrayMath.sqr(yValues));
            }

            return new DoubleErrorDataSet(functionName, newDataSet.getXValues(), yValues, eyn, eyp,
                    newDataSet.getDataCount(), true);
        }

        final DoubleErrorDataSet retFunction = prevAverage.getDataCount() == 0
                ? new DoubleErrorDataSet(functionName, newDataSet.getXValues(), newDataSet.getYValues(),
                        errors(newDataSet, EYN), errors(newDataSet, EYP), newDataSet.getDataCount(), true)
                : new DoubleErrorDataSet(prevAverage.getName(), prevAverage.getXValues(), prevAverage.getYValues(),
                        errors(prevAverage, EYN), errors(prevAverage, EYP), newDataSet.getDataCount(), true);

        final double alpha = 1.0 / (1.0 + nUpdates);
        final boolean avg2Empty = prevAverage2.getDataCount() == 0;

        for (int i = 0; i < prevAverage.getDataCount(); i++) {
            final double oldX = prevAverage.getX(i);
            final double oldY = prevAverage.getY(i);
            final double oldY2 = avg2Empty ? oldY * oldY : prevAverage2.getY(i);
            final double newX = newDataSet.getX(i);

            // whether we need to interpolate
            final boolean inter = oldX != newX;

            final double y = inter ? newDataSet.getValue(oldX) : newDataSet.getY(i);
            final double newY = (1 - alpha) * oldY + alpha * y;
            final double newY2 = (1 - alpha) * oldY2 + alpha * (y * y);

            final double eyn = error(newDataSet, EYN, i, newX, inter);
            final double eyp = error(newDataSet, EYP, i, newX, inter);

            if (prevAverage2 instanceof DoubleErrorDataSet) {
                if (avg2Empty) {
                    ((DoubleErrorDataSet) prevAverage2).add(newX, newY2, eyn, eyp);
                } else {
                    ((DoubleErrorDataSet) prevAverage2).set(i, newX, newY2, eyn, eyp);
                }
            }
            final double newEYN = Math.sqrt(Math.abs(newY2 - Math.pow(newY, 2)) + eyn * eyn);
            final double newEYP = Math.sqrt(Math.abs(newY2 - Math.pow(newY, 2)) + eyp * eyp);
            retFunction.set(i, oldX, newY, newEYN, newEYP);
        }

        return retFunction;
    }

    public static DataSetError averageDataSetsFIR(final List<DataSet> dataSets, final int nUpdates) {
        if (dataSets == null || dataSets.isEmpty()) {
            return null;
        }
        final String functionName = "LP(" + dataSets.get(0).getName() + ", FIR)";
        if (dataSets.size() <= 1) {
            if (dataSets.get(0) instanceof DataSetError) {
                final DataSetError newFunction = (DataSetError) dataSets.get(0);
                return new DoubleErrorDataSet(functionName, newFunction.getXValues(), newFunction.getYValues(),
                        newFunction.getYErrorsNegative(), newFunction.getYErrorsPositive(), newFunction.getDataCount(), true);
            }

            final DataSet newFunction = dataSets.get(0);
            return new DoubleErrorDataSet(functionName, newFunction.getXValues(), newFunction.getYValues(), new double[newFunction.getDataCount()], new double[newFunction.getDataCount()],
                    newFunction.getDataCount(), true);
        }

        final int nAvg = Math.min(nUpdates, dataSets.size());
        final DataSet newFunction = dataSets.get(dataSets.size() - 1);
        final DoubleErrorDataSet retFunction = new DoubleErrorDataSet(functionName, newFunction.getDataCount() + 2);

        for (int i = 0; i < newFunction.getDataCount(); i++) {
            final double newX = newFunction.getX(i);
            double mean = 0.0;
            double var = 0.0;
            double eyn = 0.0;
            double eyp = 0.0;

            int count = 0;
            for (int j = Math.max(0, dataSets.size() - nAvg); j < dataSets.size(); j++) {
                final DataSet oldFunction = dataSets.get(j);
                final double oldX = oldFunction.getX(i);
                final double oldY = oldX == newX ? oldFunction.getY(i) : oldFunction.getValue(newX);
                mean += oldY;
                var += oldY * oldY;
                // whether we need to interpolate
                final boolean inter = oldX != newX;
                eyn += error(oldFunction, EYN, i, newX, inter);
                eyp += error(oldFunction, EYP, i, newX, inter);
                count++;
            }

            mean /= count;
            eyn /= count;
            eyp /= count;
            var /= count;
            final double mean2 = mean * mean;
            final double diff = Math.abs(var - mean2);
            eyn = Math.sqrt(eyn * eyn + diff);
            eyp = Math.sqrt(eyp * eyp + diff);
            retFunction.add(newX, mean, eyn, eyp);
        }

        return retFunction;
    }

    public static DataSetError integrateFunction(final DataSet function) {
        return integrateFunction(function, Double.NaN, Double.NaN);
    }

    public static DataSetError integrateFunction(final DataSet function, final double xMin, final double xMax) {
        final int nLength = function.getDataCount();
        final String functionName = function.getName();
        String newName = INTEGRAL_SYMBOL + "(" + functionName + ")dyn";
        if (nLength <= 0) {
            return new DoubleErrorDataSet(function.getName(), function.getXValues(),
                    new double[function.getDataCount()],new double[function.getDataCount()],new double[function.getDataCount()],function.getDataCount(), true);
        }

        double xMinLocal = function.getXMin();
        double xMaxLocal = function.getXMax();
        double sign = 1.0;

        if (Double.isFinite(xMin) && Double.isFinite(xMax)) {
            xMinLocal = Math.min(xMin, xMax);
            xMaxLocal = Math.max(xMin, xMax);
            if (xMin > xMax) {
                sign = -1;
            }
            newName = INTEGRAL_SYMBOL + "(" + functionName + ")dyn|" + "_{" + xMinLocal + "}^{" + xMaxLocal + "}";
        } else if (Double.isFinite(xMin)) {
            xMinLocal = xMin;
            newName = INTEGRAL_SYMBOL + "(" + functionName + ")dyn|" + "_{" + xMinLocal + "}^{+" + INFINITY_SYMBOL
                    + "}";
        } else if (Double.isFinite(xMax)) {
            xMaxLocal = xMax;
            newName = INTEGRAL_SYMBOL + "(" + functionName + ")dyn|" + "_{-" + INFINITY_SYMBOL + "}^{" + xMaxLocal
                    + "}";
        }

        final DoubleErrorDataSet retFunction = new DoubleErrorDataSet(newName, nLength);
        if (nLength <= 1) {
            retFunction.add(function.getX(0), 0, 0, 0);
            return retFunction;
        }

        double integral = 0;
        double integralEN = 0.0;
        double integralEP = 0.0;

        if (Double.isFinite(xMin) && xMin <= function.getX(0)) {
            // interpolate before range where discrete function is defined
            final double x0 = xMin;
            final double val1 = function.getValue(xMin);
            final double x1 = function.getX(0);
            final double val2 = function.getY(0);
            final double step = x1 - x0;
            integral += sign * 0.5 * step * (val1 + val2);
            final double en1 = error(function, EYN, 0);
            final double ep1 = error(function, EYP, 0);

            // assuming uncorrelated errors between bins
            integralEN = Math.hypot(integralEN, step * en1);
            integralEP = Math.hypot(integralEP, step * ep1);

            retFunction.add(x0, integral, 0, 0);
        }

        retFunction.add(function.getX(0), integral, integralEN, integralEP);
        for (int i = 1; i < nLength; i++) {
            final double x0 = function.getX(i - 1);
            final double x1 = function.getX(i);
            double step = x1 - x0;
            final double y0 = function.getY(i - 1);
            final double en1 = error(function, EYN, i - 1);
            final double ep1 = error(function, EYP, i - 1);
            final double y1 = function.getY(i);
            final double en2 = error(function, EYN, i);
            final double ep2 = error(function, EYP, i);

            // simple triangulation integration
            if (x0 >= xMinLocal && x1 <= xMaxLocal) {
                integral += sign * 0.5 * step * (y0 + y1);

                // assuming uncorrelated errors between bins
                integralEN = Math.hypot(integralEN, 0.5 * step * (en1 + en2));
                integralEP = Math.hypot(integralEP, 0.5 * step * (ep1 + ep2));

            } else if (x1 < xMinLocal && x0 < xMinLocal) {
                // see below
            } else if (x0 < xMinLocal && x1 > xMinLocal) {
                retFunction.add(xMin, integral, integralEN, integralEP);
                step = x1 - xMinLocal;
                integral += sign * 0.5 * step * (function.getValue(xMinLocal) + y1);

                // assuming uncorrelated errors between bins
                integralEN = Math.hypot(integralEN, 0.5 * step * (en1 + en2));
                integralEP = Math.hypot(integralEP, 0.5 * step * (ep1 + ep2));
            } else if (x0 < xMaxLocal && x1 > xMaxLocal) {
                step = xMaxLocal - x0;
                final double yAtMax = function.getValue(xMaxLocal);
                integral += sign * 0.5 * step * (y0 + yAtMax);

                // assuming uncorrelated errors between bins
                integralEN = Math.hypot(integralEN, 0.5 * step * (en1 + en2));
                integralEP = Math.hypot(integralEP, 0.5 * step * (ep1 + ep2));

                retFunction.add(xMaxLocal, integral, integralEN, integralEP);
            }

            retFunction.add(x1, integral, integralEN, integralEP);
        }

        if (Double.isFinite(xMax) && xMax > function.getX(nLength - 1)) {
            // interpolate after range where discrete function is defined
            final double x0 = function.getX(nLength - 1);
            final double val1 = function.getY(0);
            final double x1 = xMax;
            final double val2 = function.getValue(xMax);
            final double step = x1 - x0;
            final double en1 = error(function, EYN, nLength - 1);
            final double ep1 = error(function, EYP, nLength - 1);
            // assuming uncorrelated errors between bins
            integralEN = Math.hypot(integralEN, step * en1);
            integralEP = Math.hypot(integralEP, step * ep1);

            integral += 0.5 * step * (val1 + val2);
            retFunction.add(xMax, integral, integralEN, integralEP);
        }

        return retFunction;
    }

    public static DoublePointError integral(final DataSet function) {
        final DataSetError integratedFunction = integrateFunction(function);
        final int lastPoint = integratedFunction.getDataCount() - 1;
        if (lastPoint <= 0) {
            return new DoublePointError(0.0, 0.0, 0.0, 0.0);
        }
        final double x = integratedFunction.getX(lastPoint);
        final double y = integratedFunction.getY(lastPoint);
        final double yen = integratedFunction.getYErrorNegative(lastPoint);
        final double yep = integratedFunction.getYErrorPositive(lastPoint);
        final double ye = 0.5 * (yen + yep);

        return new DoublePointError(x, y, 0.0, ye);
    }

    public static DoublePointError integral(final DataSet function, final double xMin, final double xMax) {
        final DataSetError integratedFunction = integrateFunction(function, xMin, xMax);
        final int lastPoint = integratedFunction.getDataCount() - 1;
        final double yen = integratedFunction.getYErrorNegative(lastPoint);
        final double yep = integratedFunction.getYErrorPositive(lastPoint);
        final double ye = 0.5 * (yen + yep);

        return new DoublePointError(integratedFunction.getX(lastPoint), integratedFunction.getY(lastPoint), 0.0, ye);
    }

    public static double integralSimple(final DataSet function) {
        double integral1 = 0.0;
        double integral2 = 0.0;

        if (function.getDataCount() <= 1) {
            return 0.0;
        }
        for (int i = 1; i < function.getDataCount(); i++) {
            final double step = function.getX(i) - function.getX(i - 1);
            final double val1 = function.getY(i - 1);
            final double val2 = function.getY(i);

            integral1 += step * val1;
            integral2 += step * val2;
        }
        return 0.5 * (integral1 + integral2);
    }

    public static DataSetError derivativeFunction(final DataSet function) {
        return derivativeFunction(function, +1.0);
    }

    public static DataSetError derivativeFunction(final DataSet function, final double sign) {
        final String signAdd = sign == 1.0 ? "" : Double.toString(sign) + MULTIPLICATION_SYMBOL;
        final DoubleErrorDataSet retFunction = new DoubleErrorDataSet(
                signAdd + DIFFERENTIAL + "(" + function.getName() + ")", function.getDataCount());

        if (function.getDataCount() <= 3) {
            return retFunction;
        }
        // TODO: check error estimate for derivative ...

        // // derivative for first point
        // final double y0 = function.getY(0);
        // final double y1 = function.getY(0);
        // final double yen0 = error(function, EYN, 0);
        // final double yep0 = error(function, EYP, 0);
        // final double yen1 = error(function, EYN, 1);
        // final double yep1 = error(function, EYP, 1);
        // final double deltaY0 = y1 - y0;
        // final double deltaYEN = Math.sqrt(Math.pow(yen0, 2) + Math.pow(yen1,
        // 2));
        // final double deltaYEP = Math.sqrt(Math.pow(yep0, 2) + Math.pow(yep1,
        // 2));
        // final double deltaX0 = function.getX(1) - function.getX(0);
        // retFunction.add(function.getX(0), sign * (deltaY0 / deltaX0),
        // deltaYEN, deltaYEP);
        for (int i = 0; i < 2; i++) {
            final double x0 = function.getX(i);
            retFunction.add(x0, 0, 0, 0);
        }
        for (int i = 2; i < function.getDataCount() - 2; i++) {
            final double x0 = function.getX(i);
            final double stepL = x0 - function.getX(i - 1);
            final double stepR = function.getX(i + 1) - x0;
            final double valL = function.getY(i - 1);
            final double valC = function.getY(i);
            final double valR = function.getY(i + 1);

            final double yenL = error(function, EYN, i - 1);
            final double yenC = error(function, EYN, i);
            final double yenR = error(function, EYN, i + 1);
            final double yen = Math.sqrt(TMathConstants.Sqr(yenL) + TMathConstants.Sqr(yenC) + TMathConstants.Sqr(yenR))
                    / 4;

            final double yepL = error(function, EYP, i - 1);
            final double yepC = error(function, EYP, i);
            final double yepR = error(function, EYP, i + 1);
            final double yep = Math.sqrt(TMathConstants.Sqr(yepL) + TMathConstants.Sqr(yepC) + TMathConstants.Sqr(yepR))
                    / 4;

            // simple derivative computation
            final double derivative = 0.5 * ((valC - valL) / stepL + (valR - valC) / stepR);

            retFunction.add(x0, sign * derivative, yen, yep);
        }
        for (int i = function.getDataCount() - 2; i < function.getDataCount(); i++) {
            final double x0 = function.getX(i);
            retFunction.add(x0, 0, 0, 0);
        }
        // // derivative for last point
        // final int last = function.getDataCount() - 1;
        // final double deltaYN = function.getY(last) - function.getY(last - 1);
        // final double deltaXN = function.getX(last) - function.getX(last - 1);
        // retFunction.add(function.getX(last), sign * (deltaYN / deltaXN), 0,
        // 0);

        return retFunction;
    }

    public static DataSetError normalisedFunction(final DataSet function) {
        return normalisedFunction(function, 1.0);
    }

    public static DataSetError normalisedFunction(final DataSet function, final double requiredIntegral) {
        final DoublePointError complexInt = integral(function);
        final double integral = complexInt.getY() / requiredIntegral;
        //final double integralErr = complexInt.getErrorY() / requiredIntegral;
        // TODO: add error propagation to normalised function error estimate
        if (integral == 0) {
            return new DoubleErrorDataSet(function.getName(), function.getXValues(),
                    new double[function.getDataCount()],new double[function.getDataCount()],new double[function.getDataCount()],function.getDataCount(), true);
        }
        final double[] xValues = function.getXValues();
        final double[] yValues = ArrayMath.divide(function.getYValues(), integral);
        final double[] eyp = ArrayMath.divide(errors(function, EYN), integral);
        final double[] eyn = ArrayMath.divide(errors(function, EYP), integral);

        return new DoubleErrorDataSet(function.getName(), xValues, yValues, eyp, eyn, function.getDataCount(), true);
    }

    public enum Filter {
        MEAN("LowPass"),
        MEDIAN("Median"),
        MIN("Min"),
        MAX("Max"),
        P2P("PeakToPeak"),
        RMS("RMS"),
        GEOMMEAN("GeometricMean");

        private String tag;

        Filter(final String tag) {
            this.tag = tag;
        }

        String getTag() {
            return tag;
        }
    }

    public static DataSetError lowPassFilterFunction(final DataSet function, final double width) {
        return filterFunction(function, width, Filter.MEAN);
    }

    public static DataSetError medianFilteredFunction(final DataSet function, final double width) {
        return filterFunction(function, width, Filter.MEDIAN);
    }

    public static DataSetError minFilteredFunction(final DataSet function, final double width) {
        return filterFunction(function, width, Filter.MIN);
    }

    public static DataSetError maxFilteredFunction(final DataSet function, final double width) {
        return filterFunction(function, width, Filter.MAX);
    }

    public static DataSetError peakToPeakFilteredFunction(final DataSet function, final double width) {
        return filterFunction(function, width, Filter.P2P);
    }

    public static DataSetError rmsFilteredFunction(final DataSet function, final double width) {
        return filterFunction(function, width, Filter.RMS);
    }

    public static DataSetError geometricMeanFilteredFunction(final DataSet function, final double width) {
        return filterFunction(function, width, Filter.GEOMMEAN);
    }

    public static DataSetError filterFunction(final DataSet function, final double width, final Filter filterType) {
        final int n = function.getDataCount();
        final DoubleErrorDataSet filteredFunction = new DoubleErrorDataSet(
                filterType.getTag() + "(" + function.getName() + "," + Double.toString(width) + ")", n);
        final double[] subArrayY = new double[n];
        final double[] subArrayYn = new double[n];
        final double[] subArrayYp = new double[n];

        final double[] xValues = function.getXValues();
        final double[] yValues = function.getYValues();
        final double[] yen = errors(function, EYN);
        final double[] yep = errors(function, EYN);

        for (int i = 0; i < n; i++) {
            final double time0 = xValues[i];

            int count = 0;
            for (int j = 0; j < n; j++) {
                final double time = xValues[j];
                if (Math.abs(time0 - time) <= width) {
                    subArrayY[count] = yValues[j];
                    subArrayYn[count] = yen[j];
                    subArrayYp[count] = yep[j];
                    count++;
                }
            }

            final double norm = count > 0 ? 1.0 / Math.sqrt(count) : 0.0;

            switch (filterType) {
            case MEDIAN:
                filteredFunction.add(time0, TMath.Median(subArrayY, count), TMath.Median(subArrayYn, count),
                        TMath.Median(subArrayYp, count));
                break;
            case MIN:
                filteredFunction.add(time0, TMath.Minimum(subArrayY, count), TMath.Minimum(subArrayYn, count),
                        TMath.Minimum(subArrayYp, count));
                break;
            case MAX:
                filteredFunction.add(time0, TMath.Maximum(subArrayY, count), TMath.Maximum(subArrayYn, count),
                        TMath.Maximum(subArrayYp, count));
                break;
            case P2P:
                filteredFunction.add(time0, TMath.PeakToPeak(subArrayY, count), TMath.PeakToPeak(subArrayYn, count),
                        TMath.PeakToPeak(subArrayYp, count));
                break;
            case RMS:
                filteredFunction.add(time0, TMath.RMS(subArrayY, count), TMath.RMS(subArrayYn, count),
                        TMath.RMS(subArrayYp, count));
                break;
            case GEOMMEAN:
                filteredFunction.add(time0, TMath.GeometricMean(subArrayY, count),
                        TMath.GeometricMean(subArrayYn, count), TMath.GeometricMean(subArrayYp, count));
                break;
            case MEAN:
            default:
                filteredFunction.add(time0, TMath.Mean(subArrayY, count), TMath.Mean(subArrayYn, count) * norm,
                        TMath.Mean(subArrayYp, count) * norm);
                break;
            }

        }

        return filteredFunction;
    }

    public static DataSetError iirLowPassFilterFunction(final DataSet function, final double width) {
        final int n = function.getDataCount();
        final DoubleErrorDataSet filteredFunction = new DoubleErrorDataSet(
                "iir" + Filter.MEAN.getTag() + "(" + function.getName() + "," + Double.toString(width) + ")", n);
        if (n <= 1) {
            filteredFunction.set(function);
            return filteredFunction;
        }
        final double[] xValues = function.getXValues();
        final double[] yValues = function.getYValues();
        final double[] yen = errors(function, EYN);
        final double[] yep = errors(function, EYN);

        final double[] yUp = new double[n];
        final double[] yDown = new double[n];
        final double[] ye1 = new double[n];
        final double[] ye2 = new double[n];

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
        // final double newEYN = Math.sqrt(Math.abs(smoothed2 - smoothed *
        // smoothed) + yen[i] * yen[i]);
        // final double newEYP = Math.sqrt(Math.abs(smoothed2 - smoothed *
        // smoothed) + yep[i] * yep[i]);
        //
        // filteredFunction.add(x1 - smoothing, smoothed, newEYN, newEYP);
        // }

        // calculate forward/backward to compensate for the IIR group-delay
        for (int i = 1; i < n; i++) {
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
        for (int i = n - 2; i >= 0; i--) {
            final double x0 = xValues[i];
            final double x1 = xValues[i + 1];
            final double y = yValues[i];
            smoothed += (x1 - x0) * (y - smoothed) / smoothing;
            smoothed2 += (x1 - x0) * (y * y - smoothed2) / smoothing;
            yDown[i] = smoothed;
            ye2[i] = smoothed2;
        }

        filteredFunction.add(xValues[0], yValues[0], yen[0], yep[0]);
        for (int i = 1; i < n; i++) {
            final double x1 = xValues[i];
            final double y = 0.5 * (yUp[i] + yDown[i]);
            final double mean2 = y * y;
            final double y2 = 0.5 * Math.pow(ye1[i] + ye2[i], 1);
            final double avgError2 = Math.abs(y2 - mean2);
            final double newEYN = Math.sqrt(avgError2 + yen[i] * yen[i]);
            final double newEYP = Math.sqrt(avgError2 + yep[i] * yep[i]);

            filteredFunction.add(x1, y, newEYN, newEYP);
        }

        return filteredFunction;
    }

    public enum MathOp {
        ADD("+"),
        SUBTRACT("-"),
        MULTIPLY("*"),
        DIVIDE("*"),
        SQR("SQR"),
        SQRT("SQRT"),
        LOG10("Log10"),
        DB("dB");

        private String tag;

        MathOp(final String tag) {
            this.tag = tag;
        }

        String getTag() {
            return tag;
        }
    }

    public static DataSetError addFunction(final DataSet function, final double value) {
        return mathFunction(function, value, MathOp.ADD);
    }

    public static DataSetError addFunction(final DataSet function1, final DataSet function2) {
        return mathFunction(function1, function2, MathOp.ADD);
    }

    public static DataSetError subtractFunction(final DataSet function, final double value) {
        return mathFunction(function, value, MathOp.SUBTRACT);
    }

    public static DataSetError subtractFunction(final DataSet function1, final DataSet function2) {
        return mathFunction(function1, function2, MathOp.SUBTRACT);
    }

    public static DataSetError multiplyFunction(final DataSet function, final double value) {
        return mathFunction(function, value, MathOp.MULTIPLY);
    }

    public static DataSetError multiplyFunction(final DataSet function1, final DataSet function2) {
        return mathFunction(function1, function2, MathOp.MULTIPLY);
    }

    public static DataSetError divideFunction(final DataSet function, final double value) {
        return mathFunction(function, value, MathOp.DIVIDE);
    }

    public static DataSetError divideFunction(final DataSet function1, final DataSet function2) {
        return mathFunction(function1, function2, MathOp.DIVIDE);
    }

    public static DataSetError sqrFunction(final DataSet function) {
        return mathFunction(function, 0.0, MathOp.SQR);
    }

    public static DataSetError sqrFunction(final DataSet function1, final DataSet function2) {
        return mathFunction(function1, function2, MathOp.SQR);
    }

    public static DataSetError sqrtFunction(final DataSet function) {
        return mathFunction(function, 0.0, MathOp.SQRT);
    }

    public static DataSetError sqrtFunction(final DataSet function1, final DataSet function2) {
        return mathFunction(function1, function2, MathOp.SQRT);
    }

    public static DataSetError log10Function(final DataSet function) {
        return mathFunction(function, 0.0, MathOp.LOG10);
    }

    public static DataSetError log10Function(final DataSet function1, final DataSet function2) {
        return mathFunction(function1, function2, MathOp.LOG10);
    }

    public static DataSetError dbFunction(final DataSet function) {
        return mathFunction(function, 0.0, MathOp.DB);
    }

    public static DataSetError dbFunction(final DataSet function1, final DataSet function2) {
        return mathFunction(function1, function2, MathOp.DB);
    }

    public static DataSetError mathFunction(final DataSet function, final double value, final MathOp op) {
        final String functionName = op.getTag() + "(" + function.getName() + ")";
        final double[] y = function.getYValues();
        final double[] eyn = errors(function, EYN);
        final double[] eyp = errors(function, EYP);
        double norm;
        switch (op) {
        case ADD:
            return new DoubleErrorDataSet(functionName, function.getXValues(), ArrayMath.add(y, value), eyn, eyp,
                    function.getDataCount(), true);
        case SUBTRACT:
            return new DoubleErrorDataSet(functionName, function.getXValues(), ArrayMath.subtract(y, value), eyn, eyp,
                    function.getDataCount(), true);
        case MULTIPLY:
            return new DoubleErrorDataSet(functionName, function.getXValues(), ArrayMath.multiply(y, value),
                    ArrayMath.multiply(eyn, value), ArrayMath.multiply(eyp, value), function.getDataCount(), true);
        case DIVIDE:
            return new DoubleErrorDataSet(functionName, function.getXValues(), ArrayMath.divide(y, value),
                    ArrayMath.divide(eyn, value), ArrayMath.divide(eyp, value), function.getDataCount(), true);
        case SQR:
            for (int i = 0; i < eyn.length; i++) {
                eyn[i] = 2 * Math.abs(y[i]) * eyn[i];
                eyp[i] = 2 * Math.abs(y[i]) * eyp[i];
            }
            return new DoubleErrorDataSet(functionName, function.getXValues(), ArrayMath.sqr(y), eyn, eyp,
                    function.getDataCount(), true);
        case SQRT:
            for (int i = 0; i < eyn.length; i++) {
                eyn[i] = Math.sqrt(Math.abs(y[i])) * eyn[i];
                eyp[i] = Math.sqrt(Math.abs(y[i])) * eyp[i];
            }
            return new DoubleErrorDataSet(functionName, function.getXValues(), ArrayMath.sqrt(y), eyn, eyp,
                    function.getDataCount(), true);
        case LOG10:
            norm = 1.0 / Math.log(10);
            for (int i = 0; i < eyn.length; i++) {
                eyn[i] = y[i] > 0 ? norm / Math.abs(y[i]) * eyn[i] : Double.NaN;
                eyp[i] = y[i] > 0 ? norm / Math.abs(y[i]) * eyp[i] : Double.NaN;
            }
            return new DoubleErrorDataSet(functionName, function.getXValues(), ArrayMath.tenLog10(y), eyn, eyp,
                    function.getDataCount(), true);
        case DB:
            norm = 20.0 / Math.log(10);
            for (int i = 0; i < eyn.length; i++) {
                eyn[i] = y[i] > 0 ? norm / Math.abs(y[i]) * eyn[i] : Double.NaN;
                eyp[i] = y[i] > 0 ? norm / Math.abs(y[i]) * eyp[i] : Double.NaN;
            }
            return new DoubleErrorDataSet(functionName, function.getXValues(), ArrayMath.decibel(y), eyn, eyp,
                    function.getDataCount(), true);
        default:
            // return copy if nothing else matches
            return new DoubleErrorDataSet(functionName, function.getXValues(), function.getYValues(),
                    errors(function, EYN), errors(function, EYP), function.getDataCount(), true);
        }
    }

    public static DataSetError mathFunction(final DataSet function1, final DataSet function2, final MathOp op) {
        final DoubleErrorDataSet ret = new DoubleErrorDataSet(function1.getName() + op.getTag() + function2.getName(),
                function1.getDataCount());

        for (int i = 0; i < function1.getDataCount(); i++) {
            final double X1 = function1.getX(i);
            final double X2 = function1.getX(i);
            final boolean inter = X1 != X2;
            final double Y1 = function1.getY(i);
            final double Y2 = inter ? function2.getY(i) : function2.getValue(X1);
            final double eyn1 = error(function1, EYN, i);
            final double eyp1 = error(function1, EYP, i);
            final double eyn2 = error(function2, EYN, i, X1, inter);
            final double eyp2 = error(function2, EYP, i, X1, inter);
            double newY;
            double nEYN;
            double nEYP;
            double norm;

            // switch through math operations
            switch (op) {
            case ADD:
                newY = Y1 + Y2;
                nEYN = Math.hypot(eyn1, eyn2);
                nEYP = Math.hypot(eyp1, eyp2);
                break;
            case SUBTRACT:
                newY = Y1 - Y2;
                nEYN = Math.hypot(eyn1, eyn2);
                nEYP = Math.hypot(eyp1, eyp2);
                break;
            case MULTIPLY:
                newY = Y1 * Y2;
                nEYN = Math.hypot(Y2 * eyn1, Y1 * eyn2);
                nEYP = Math.hypot(Y2 * eyp1, Y1 * eyp2);
                break;
            case DIVIDE:
                newY = Y1 / Y2;
                nEYN = Math.hypot(eyn1 / Y2, newY * eyn2 / Y2);
                nEYP = Math.hypot(eyp1 / Y2, newY * eyp2 / Y2);
                break;
            case SQR:
                newY = TMathConstants.Sqr(Y1 + Y2);
                nEYN = 2 * Math.abs(Y1 + Y2) * Math.hypot(eyn1, eyn2);
                nEYP = 2 * Math.abs(Y1 + Y2) * Math.hypot(eyp1, eyp2);
                break;
            case SQRT:
                newY = TMathConstants.Sqrt(Y1 + Y2);
                nEYN = Math.sqrt(Math.abs(Y1 + Y2)) * Math.hypot(eyn1, eyn2);
                nEYP = Math.sqrt(Math.abs(Y1 + Y2)) * Math.hypot(eyp1, eyp2);
                break;
            case LOG10:
                norm = 1.0 / Math.log(10);
                newY = TMathConstants.Log10(Y1 + Y2);
                nEYN = Y1 + Y2 > 0 ? norm / Math.abs(Y1 + Y2) * Math.hypot(eyn1, eyn2) : Double.NaN;
                nEYP = Y1 + Y2 > 0 ? norm / Math.abs(Y1 + Y2) * Math.hypot(eyp1, eyp2) : Double.NaN;
                break;
            case DB:
                norm = 20.0 / Math.log(10);
                newY = 20 * TMathConstants.Log10(Y1 + Y2);
                nEYN = Y1 + Y2 > 0 ? norm / Math.abs(Y1 + Y2) * Math.hypot(eyn1, eyn2) : Double.NaN;
                nEYP = Y1 + Y2 > 0 ? norm / Math.abs(Y1 + Y2) * Math.hypot(eyp1, eyp2) : Double.NaN;
                break;
            default:
                newY = Y1;
                nEYN = eyn1;
                nEYP = eyp1;
                break;
            }

            ret.add(X1, newY, nEYN, nEYP);
        }

        return ret;
    }

    public static DataSetError magnitudeSpectrum(final DataSet function) {
        return magnitudeSpectrum(function, Apodization.Hann, false, false);
    }

    public static DataSetError normalisedMagnitudeSpectrumDecibel(final DataSet function) {
        return magnitudeSpectrum(function, Apodization.Hann, true, true);
    }

    public static DataSetError magnitudeSpectrumDecibel(final DataSet function) {
        return magnitudeSpectrum(function, Apodization.Hann, true, false);
    }

    public static DataSetError magnitudeSpectrum(final DataSet function, final Apodization apodization,
            final boolean dbScale, final boolean normalisedFrequency) {
        final int n = function.getDataCount();

        final DoubleFFT_1D fastFourierTrafo = new DoubleFFT_1D(n);

        // N.B. since realForward computes the FFT in-place -> generate a copy
        final double[] fftSpectra = new double[n];
        for (int i = 0; i < n; i++) {
            final double window = apodization.getIndex(i, n);
            fftSpectra[i] = function.getY(i) * window;
        }

        fastFourierTrafo.realForward(fftSpectra);
        final double[] mag = dbScale ? SpectrumTools.computeMagnitudeSpectrum_dB(fftSpectra, true)
                : SpectrumTools.computeMagnitudeSpectrum(fftSpectra, true);
        final double dt = function.getX(function.getDataCount() - 1) - function.getX(0);
        final double fsampling = normalisedFrequency || dt <= 0 ? 0.5 / mag.length : 1.0 / dt;

        final String functionName = "Mag" + (dbScale ? "[dB]" : "") + "(" + function.getName() + ")";

        final DoubleErrorDataSet ret = new DoubleErrorDataSet(functionName, mag.length);
        for (int i = 0; i < mag.length; i++) {
            // TODO: consider magnitude error estimate
            ret.add(i * fsampling, mag[i], 0, 0);
        }

        return ret;
    }

    public static EditableDataSet setFunction(final EditableDataSet function, final double value, final double xMin,
            final double xMax) {
        final int nLength = function.getDataCount();
        double xMinLocal = function.getX(0);
        double xMaxLocal = function.getX(nLength - 1);

        if (Double.isFinite(xMin) && Double.isFinite(xMax)) {
            xMinLocal = Math.min(xMin, xMax);
            xMaxLocal = Math.max(xMin, xMax);
        } else if (Double.isFinite(xMin)) {
            xMinLocal = xMin;
        } else if (Double.isFinite(xMax)) {
            xMaxLocal = xMax;
        }

        final boolean oldFlag = function.isAutoNotification();
        function.setAutoNotifaction(false);
        for (int i = 0; i < nLength; i++) {
            final double x = function.getX(i);
            if (x >= xMinLocal && x <= xMaxLocal) {
                function.set(i, x, value);
            }
        }
        function.setAutoNotifaction(oldFlag);

        return function;
    }

    public static DataSet addGaussianNoise(final DataSet function, final double sigma) {
        final int nLength = function.getDataCount();
        final DoubleErrorDataSet ret = new DoubleErrorDataSet(function.getName() + "noise(" + sigma + ")", nLength);

        for (int i = 0; i < nLength; i++) {
            final double x = function.getX(i);
            final double y = function.getY(i) + random.Gaus(0, sigma);
            ret.add(x, y, sigma, sigma);
        }

        return ret;
    }

    public static DataSet getSubRange(final DataSet function, final double xMin, final double xMax) {
        final int nLength = function.getDataCount();
        final DoubleErrorDataSet ret = new DoubleErrorDataSet(
                function.getName() + "subRange(" + xMin + ", " + xMax + ")", nLength);

        for (int i = 0; i < nLength; i++) {
            final double x = function.getX(i);
            final double y = function.getY(i);
            final double ex = error(function, EXP, i);
            final double ey = error(function, EYP, i);
            if (x >= xMin && x <= xMax) {
                ret.add(x, y, ex, ey);
            }
        }

        return ret;
    }
}
