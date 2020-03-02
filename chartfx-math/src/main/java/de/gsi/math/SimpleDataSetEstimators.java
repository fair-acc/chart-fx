package de.gsi.math;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.utils.AssertUtils;

/**
 * computation of statistical estimates
 *
 * @author rstein
 */
public final class SimpleDataSetEstimators { // NOPMD name is as is (ie. no Helper/Utils ending

    private SimpleDataSetEstimators() {
        // this is a static class
    }

    /**
     * Computes the centre of mass in a given index range.
     * 
     * @param dataSet
     * @param minIndex
     * @param maxIndex
     * @return centre of mass
     */
    public static double computeCentreOfMass(DataSet dataSet, int minIndex, int maxIndex) {
        AssertUtils.gtEqThanZero("minIndex", minIndex);
        AssertUtils.gtOrEqual("maxIndex must be smaller than dataCount()", maxIndex, dataSet.getDataCount());
        double com = 0;
        double mass = 0;
        for (int i = minIndex; i < maxIndex; i++) {
            double freq = dataSet.get(DIM_X, i);
            double val = dataSet.get(DIM_Y, i);
            if (Double.isFinite(freq) && Double.isFinite(val)) {
                com += freq * val;
                mass += val;
            }
        }
        return com / mass;
    }

    /**
     * Computes the centre of mass in a given x range.
     * 
     * @param dataSet
     * @param min
     * @param max
     * @return centre of mass
     */
    public static double computeCentreOfMass(DataSet dataSet, double min, double max) {
        AssertUtils.gtOrEqual("max must be greater than min", min, max);
        return computeCentreOfMass(dataSet, dataSet.getIndex(DIM_X, min), dataSet.getIndex(DIM_X, max));
    }

    /**
     * Compute centre of mass over full DataSet
     * 
     * @param dataSet
     * @return centre of mass
     */
    public static double computeCentreOfMass(DataSet dataSet) {
        return computeCentreOfMass(dataSet, 0, dataSet.getDataCount(DIM_X));
    }

    /**
     * compute simple Full-Width-Half-Maximum (no inter-bin interpolation)
     *
     * @param data data array
     * @param length of data array
     * @param index 0&lt; index &lt; data.length
     * @return FWHM estimate [bins]
     */
    public static double computeFWHM(final double[] data, final int length, final int index) {
        if (!(index > 0 && index < length - 1)) {
            return Double.NaN;
        }
        final double maxHalf = 0.5 * data[index];
        int lowerLimit;
        int upperLimit;
        for (upperLimit = index; upperLimit < length && data[upperLimit] > maxHalf; upperLimit++) {
            // computation done in the abort condition
        }
        for (lowerLimit = index; lowerLimit > 0 && data[lowerLimit] > maxHalf; lowerLimit--) {
            // computation done in the abort condition
        }
        if (upperLimit >= length || lowerLimit < 0) {
            return Double.NaN;
        }
        return upperLimit - lowerLimit;
    }

    /**
     * compute interpolated Full-Width-Half-Maximum
     *
     * @param data data array
     * @param length of data array
     * @param index 0&lt; index &lt; data.length
     * @return FWHM estimate [bins]
     */
    public static double computeInterpolatedFWHM(final double[] data, final int length, final int index) {
        if (!(index > 0 && index < length - 1)) {
            return Double.NaN;
        }
        final double maxHalf = 0.5 * data[index];
        int lowerLimit;
        int upperLimit;
        for (upperLimit = index; upperLimit < length && data[upperLimit] > maxHalf; upperLimit++) {
            // computation done in the abort condition
        }
        for (lowerLimit = index; lowerLimit > 0 && data[lowerLimit] > maxHalf; lowerLimit--) {
            // computation done in the abort condition
        }
        if (upperLimit >= length || lowerLimit < 0) {
            return Double.NaN;
        }
        final double lowerLimitRefined = SimpleDataSetEstimators.linearInterpolate(lowerLimit, lowerLimit + 1.0,
                data[lowerLimit], data[lowerLimit + 1], maxHalf);
        final double upperLimitRefined = SimpleDataSetEstimators.linearInterpolate(upperLimit - 1.0, upperLimit,
                data[upperLimit - 1], data[upperLimit], maxHalf);
        return upperLimitRefined - lowerLimitRefined;
    }

    public static double getDistance(final DataSet dataSet, final int indexMin, final int indexMax,
            final boolean isHorizontal) {
        return isHorizontal ? dataSet.get(DIM_X, indexMax) - dataSet.get(DIM_X, indexMin)
                : dataSet.get(DIM_Y, indexMax) - dataSet.get(DIM_Y, indexMin);
    }

    public static double[] getDoubleArray(final DataSet dataSet, final int indexMin, final int indexMax) {
        if (indexMax - indexMin <= 0) {
            return new double[0];
        }
        final double[] ret = new double[indexMax - indexMin];

        int count = 0;
        for (int index = indexMin; index < indexMax; index++) {
            final double actual = dataSet.get(DIM_Y, index);
            ret[count] = actual;
            count++;
        }
        return ret;
    }

    public static double getDutyCycle(final DataSet dataSet, final int indexMin, final int indexMax) {
        final double minVal = SimpleDataSetEstimators.getMinimum(dataSet, indexMin, indexMax);
        final double maxVal = SimpleDataSetEstimators.getMaximum(dataSet, indexMin, indexMax);
        final double range = Math.abs(maxVal - minVal);

        int countLow = 0;
        int countHigh = 0;
        final double thresholdMin = minVal + 0.45 * range; // includes 10% hysteresis
        final double thresholdMax = minVal + 0.55 * range; // includes 10% hysteresis
        for (int index = indexMin; index < indexMax; index++) {
            final double actual = dataSet.get(DIM_Y, index);
            if (Double.isFinite(actual)) {
                if (actual < thresholdMin) {
                    countLow++;
                }

                if (actual > thresholdMax) {
                    countHigh++;
                }
            }
        }
        if ((countLow + countHigh) == 0) {
            return Double.NaN;
        }
        return (double) countHigh / (double) (countLow + countHigh);
    }

    public static double getEdgeDetect(final DataSet dataSet, final int indexMin, final int indexMax) {
        final double minVal = SimpleDataSetEstimators.getMinimum(dataSet, indexMin, indexMax);
        final double maxVal = SimpleDataSetEstimators.getMaximum(dataSet, indexMin, indexMax);
        final double range = SimpleDataSetEstimators.getRange(dataSet, indexMin, indexMax);

        final boolean inverted = dataSet.get(DIM_Y, indexMin) > dataSet.get(DIM_Y, indexMax);
        // detect 20% and 80% change
        final double startTime = dataSet.get(DIM_X, indexMin);
        double stopTime = dataSet.get(DIM_X, indexMax);
        if (inverted) {
            // detect falling edge
            for (int index = indexMin; index < indexMax; index++) {
                final double actual = dataSet.get(DIM_Y, index);
                if (Double.isFinite(actual) && actual < maxVal - 0.5 * range) {
                    stopTime = dataSet.get(DIM_X, index);
                    break;
                }
            }
        } else {
            // detect rising edge
            for (int index = indexMin; index < indexMax; index++) {
                final double actual = dataSet.get(DIM_Y, index);
                if (Double.isFinite(actual) && actual > minVal + 0.5 * range) {
                    stopTime = dataSet.get(DIM_X, index);
                    break;
                }
            }
        }
        return stopTime - startTime;
    }

    public static double getFrequencyEstimate(final DataSet dataSet, final int indexMin, final int indexMax) {
        final double minVal = SimpleDataSetEstimators.getMinimum(dataSet, indexMin, indexMax);
        final double maxVal = SimpleDataSetEstimators.getMaximum(dataSet, indexMin, indexMax);
        final double range = Math.abs(maxVal - minVal);

        final double thresholdMin = minVal + 0.45 * range; // includes 10% hysteresis
        final double thresholdMax = minVal + 0.55 * range; // includes 10% hysteresis

        double startRisingEdge = Double.NaN;
        double startFallingEdge = Double.NaN;
        double avgPeriod = 0.0;
        int avgPeriodCount = 0;
        double actualState = 0.0; // low assumes am below zero line
        for (int index = indexMin; index < indexMax; index++) {
            final double actual = dataSet.get(DIM_Y, index);
            if (!Double.isFinite(actual)) {
                continue;
            }

            if (actualState < 0.5) {
                // last sample was above zero line

                if (actual > thresholdMax) {
                    // detected rising edge
                    actualState = 1.0;
                    final double time = dataSet.get(DIM_X, index);

                    if (Double.isFinite(startRisingEdge)) {
                        final double period = time - startRisingEdge;
                        startRisingEdge = time;
                        avgPeriod += period;
                        avgPeriodCount++;
                    } else {
                        startRisingEdge = time;
                    }
                }
            } else // last sample was below zero line
            if (actual < thresholdMin) {
                // detected falling edge
                actualState = 0.0;
                final double time = dataSet.get(DIM_X, index);

                if (Double.isFinite(startFallingEdge)) {
                    final double period = time - startFallingEdge;
                    startFallingEdge = time;
                    avgPeriod += period;
                    avgPeriodCount++;
                } else {
                    startFallingEdge = time;
                }
            }
        }
        if (avgPeriodCount == 0) {
            return Double.NaN;
        }

        return avgPeriodCount / avgPeriod;
    }

    public static double getFullWidthHalfMaximum(final DataSet dataSet, final int indexMin, final int indexMax,
            final boolean interpolate) {
        final int locationMaximum = SimpleDataSetEstimators.getLocationMaximum(dataSet, indexMin, indexMax);
        if (locationMaximum <= indexMin + 1 || locationMaximum >= indexMax - 1) {
            return Double.NaN;
        }
        final double[] data = SimpleDataSetEstimators.getDoubleArray(dataSet, indexMin, indexMax);
        AssertUtils.gtThanZero("data.length", data.length);

        if (interpolate) {
            return SimpleDataSetEstimators.computeInterpolatedFWHM(data, data.length, locationMaximum - indexMin);
        }
        return SimpleDataSetEstimators.computeFWHM(data, data.length, locationMaximum - indexMin);
    }

    /**
     * @param dataSet
     * @param indexMin the starting index
     * @param indexMax the end index (switching indices reverses sign of result)
     * @return the Integral of the DataSet according to the trapezoidal rule
     */
    public static double getIntegral(final DataSet dataSet, final int indexMin, final int indexMax) {
        final double sign = TMathConstants.Sign(1, indexMax - indexMin);

        double integral = 0;
        for (int index = Math.min(indexMin, indexMax); index < Math.max(indexMin, indexMax) - 1; index++) {
            final double x0 = dataSet.get(DIM_X, index);
            final double x1 = dataSet.get(DIM_X, index + 1);
            final double y0 = dataSet.get(DIM_Y, index);
            final double y1 = dataSet.get(DIM_Y, index + 1);

            // algorithm here applies trapezoidal rule
            final double localIntegral = (x1 - x0) * 0.5 * (y0 + y1);
            if (Double.isFinite(localIntegral)) {
                integral += localIntegral;
            }
        }

        return sign * integral;
    }

    public static int getLocationMaximum(final DataSet dataSet, final int indexMin, final int indexMax) {
        int locMax = -1;
        double maxVal = -Double.MAX_VALUE;
        for (int index = indexMin; index < indexMax; index++) {
            final double actual = dataSet.get(DIM_Y, index);
            if (Double.isFinite(actual) && actual > maxVal) {
                maxVal = actual;
                locMax = index;
            }
        }
        return locMax;
    }

    public static double getLocationMaximumGaussInterpolated(final DataSet dataSet, final int indexMin,
            final int indexMax) {
        final int locationMaximum = SimpleDataSetEstimators.getLocationMaximum(dataSet, indexMin, indexMax);
        if (locationMaximum <= indexMin + 1 || locationMaximum >= indexMax - 1) {
            return Double.NaN;
        }
        final double[] data = SimpleDataSetEstimators.getDoubleArray(dataSet, indexMin, indexMax);
        if (data.length == 0) {
            return Double.NaN;
        }

        final double refinedValue = indexMin
                + SimpleDataSetEstimators.interpolateGaussian(data, data.length, locationMaximum - indexMin)
                - locationMaximum;
        final double valX0 = dataSet.get(DIM_X, locationMaximum);
        final double valX1 = dataSet.get(DIM_X, locationMaximum + 1);
        final double diff = valX1 - valX0;

        return valX0 + refinedValue * diff;
    }

    public static double getMaximum(final DataSet dataSet, final int indexMin, final int indexMax) {
        double val = -1.0 * Double.MAX_VALUE;
        for (int index = indexMin; index < indexMax; index++) {
            final double actual = dataSet.get(DIM_Y, index);
            if (Double.isFinite(actual)) {
                val = Math.max(val, actual);
            }
        }
        return val;
    }

    public static double getMean(final DataSet dataSet, final int indexMin, final int indexMax) {
        double val = 0.0;
        int count = 0;
        for (int index = indexMin; index < indexMax; index++) {
            final double actual = dataSet.get(DIM_Y, index);
            if (Double.isFinite(actual)) {
                val += actual;
                count++;
            }
        }
        if (count > 0) {
            return val / count;
        }
        return Double.NaN;
    }

    public static double getMedian(final DataSet dataSet, final int indexMin, final int indexMax) {
        final double[] data = SimpleDataSetEstimators.getDoubleArray(dataSet, indexMin, indexMax);
        if (data.length == 0) {
            return Double.NaN;
        }
        return SimpleDataSetEstimators.median(data, data.length);
    }

    public static double getMinimum(final DataSet dataSet, final int indexMin, final int indexMax) {
        double val = Double.MAX_VALUE;
        for (int index = indexMin; index < indexMax; index++) {
            final double actual = dataSet.get(DIM_Y, index);
            if (Double.isFinite(actual)) {
                val = Math.min(val, actual);
            }
        }
        return val;
    }

    /**
     * Returns the range of the y Data of the dataSet between the given indices.
     * This equals the maximum value in the range minus the minimum value.
     * 
     * @param dataSet
     * @param indexMin
     * @param indexMax
     * @return the range of yData between the given indices
     */
    public static double getRange(final DataSet dataSet, final int indexMin, final int indexMax) {
        if (dataSet.getDataCount() == 0) {
            return Double.NaN;
        }
        double valMin = Double.NaN;
        double valMax = -1.0 * Double.NaN;
        for (int index = indexMin; index < indexMax; index++) {
            final double actual = dataSet.get(DIM_Y, index);
            if (!Double.isNaN(actual)) {
                valMax = Double.isNaN(valMax) ? actual : Math.max(valMax, actual);
                valMin = Double.isNaN(valMin) ? actual : Math.min(valMin, actual);
            }
        }
        return Math.abs(valMax - valMin);
    }

    public static double getRms(final DataSet dataSet, final int indexMin, final int indexMax) {
        final double[] data = SimpleDataSetEstimators.getDoubleArray(dataSet, indexMin, indexMax);
        if (data.length == 0) {
            return Double.NaN;
        }
        return SimpleDataSetEstimators.rootMeanSquare(data, data.length);
    }

    /**
     * @param dataSet
     * @param indexMin
     * @param indexMax
     * @return the 20% to 80% rise time of the signal
     */
    public static double getSimpleRiseTime(final DataSet dataSet, final int indexMin, final int indexMax) {
        return getSimpleRiseTime2080(dataSet, indexMin, indexMax);
    }

    public static double getSimpleRiseTime(final DataSet dataSet, final int indexMin, final int indexMax,
            final double min, final double max) {
        if (!Double.isFinite(min) || min < 0.0 || min > 1.0 || !Double.isFinite(max) || max < 0.0 || max > 1.0
                || max <= min) {
            throw new IllegalArgumentException(new StringBuilder().append("[min=").append(min).append(",max=")
                    .append(max).append("] must be within [0.0, 1.0]").toString());
        }
        final double minVal = SimpleDataSetEstimators.getMinimum(dataSet, indexMin, indexMax);
        final double maxVal = SimpleDataSetEstimators.getMaximum(dataSet, indexMin, indexMax);
        final double range = Math.abs(maxVal - minVal);

        final boolean inverted = dataSet.get(DIM_Y, indexMin) > dataSet.get(DIM_Y, indexMax);
        // detect 'min' and 'max' level change
        double startTime = dataSet.get(DIM_X, indexMin);
        double stopTime = dataSet.get(DIM_X, indexMax);
        boolean foundStartRising = false;
        if (inverted) {
            // detect falling edge
            for (int index = indexMin; index < indexMax; index++) {
                final double actual = dataSet.get(DIM_Y, index);
                if (Double.isFinite(actual)) {
                    if (!foundStartRising && actual < maxVal - min * range) {
                        startTime = dataSet.get(DIM_X, index);
                        foundStartRising = true;
                        continue;
                    }

                    if (foundStartRising && actual < maxVal - max * range) {
                        stopTime = dataSet.get(DIM_X, index);
                        break;
                    }
                }
            }
        } else {
            // detect rising edge
            for (int index = indexMin; index < indexMax; index++) {
                final double actual = dataSet.get(DIM_Y, index);
                if (Double.isFinite(actual)) {
                    if (!foundStartRising && actual > minVal + min * range) {
                        startTime = dataSet.get(DIM_X, index);
                        foundStartRising = true;
                        continue;
                    }

                    if (foundStartRising && actual > minVal + max * range) {
                        stopTime = dataSet.get(DIM_X, index);
                        break;
                    }
                }
            }
        }
        return stopTime - startTime;
    }

    public static double getSimpleRiseTime1090(final DataSet dataSet, final int indexMin, final int indexMax) {
        return getSimpleRiseTime(dataSet, indexMin, indexMax, 0.1, 0.9);
    }

    public static double getSimpleRiseTime2080(final DataSet dataSet, final int indexMin, final int indexMax) {
        return getSimpleRiseTime(dataSet, indexMin, indexMax, 0.2, 0.8);
    }

    /**
     * Returns transmission as the absolute or relative ratio between the signal at the
     * indexMin'th sample and the indexMax'th sample.
     * The result is returned in percent.
     * 
     * @param dataSet A dataSet
     * @param indexMin The index to look at for the initial quantitiy
     * @param indexMax The index to look at for the final quantitiy
     * @param isAbsoluteTransmission true for absolute transmission, false for relative transmission
     * @return The transmission in percent
     */
    public static double getTransmission(final DataSet dataSet, final int indexMin, final int indexMax,
            final boolean isAbsoluteTransmission) {
        final double valRef = dataSet.get(DIM_Y, indexMin);
        final double val = dataSet.get(DIM_Y, indexMax);

        return (isAbsoluteTransmission ? val : val - valRef) / valRef * 100.0; // in [%]
    }

    /**
     * interpolation using a Gaussian interpolation
     *
     * @param data data array
     * @param length length of data arrays
     * @param index 0&lt; index &lt; data.length
     * @return location of the to be interpolated peak [bins]
     */
    public static double interpolateGaussian(final double[] data, final int length, final int index) {
        if (!(index > 0 && index < length - 1)) {
            return index;
        }
        final double left = Math.pow(data[index - 1], 1);
        final double center = Math.pow(data[index - 0], 1);
        final double right = Math.pow(data[index + 1], 1);
        double val = index;
        val += 0.5 * Math.log(right / left) / Math.log(Math.pow(center, 2) / (left * right));
        return val;
    }

    public static double linearInterpolate(final double x0, final double x1, final double y0, final double y1,
            final double y) {
        return x0 + (y - y0) * (x1 - x0) / (y1 - y0);
    }

    /**
     * @param data the input vector
     * @param length number of elements (less than data.length) to be used
     * @return median value of vector element
     */
    private static synchronized double median(final double[] data, final int length) {
        final double[] temp = SimpleDataSetEstimators.sort(data, length, false);

        if (length % 2 == 0) {
            return 0.5 * (temp[length / 2] + temp[length / 2 + 1]);
        }
        return temp[length / 2];
    }

    /**
     * @param data the input vector
     * @param length number of elements (less than data.length) to be used
     * @return un-biased r.m.s. of vector elements
     */
    protected static double rootMeanSquare(final double[] data, final int length) {
        AssertUtils.notNull("data", data);
        AssertUtils.indexInBounds(length, data.length + 1, "length must be inside bounds of data");
        if (length == 0) {
            return Double.NaN;
        }

        final double norm = 1.0 / length;
        double val1 = 0.0;
        double val2 = 0.0;
        for (int i = 0; i < length; i++) {
            val1 += data[i];
            val2 += data[i] * data[i];
        }

        val1 *= norm;
        val2 *= norm;
        // un-biased rms!
        return Math.sqrt(Math.abs(val2 - val1 * val1));
    }

    /**
     * Sorts the input a array
     *
     * @param a the input array
     * @param length <= data.length elements to be used
     * @param down true: ascending , false: descending order
     * @return the sorted array
     */
    protected static synchronized double[] sort(final double[] a, final int length, final boolean down) {
        if (a == null || a.length <= 0) {
            return new double[0];
        }
        if (length > a.length) {
            throw new IllegalArgumentException("length must be smaller or equal to the size of the input array");
        }
        final double[] index = java.util.Arrays.copyOf(a, length);
        java.util.Arrays.sort(index);

        if (down) {
            double temp;
            final int nlast = length - 1;
            for (int i = 0; i < length / 2; i++) {
                // swap values
                temp = index[i];
                index[i] = index[nlast - i];
                index[nlast - i] = temp;
            }
        }
        return index;
    }
}
