package de.gsi.chart.renderer.datareduction;

import de.gsi.chart.renderer.RendererDataReducer;
import de.gsi.chart.utils.AssertUtils;
import de.gsi.chart.utils.ProcessingProfiler;
import de.gsi.math.ArrayUtils;

/**
 * Filters data using Ramer-Douglas-Peucker algorithm with specified tolerance
 * N.B. numberical complexity: average O(n log (n)) -> worst-case O(n^2)
 *
 * @author Rzeźnik
 * @see <a href=
 *      "http://en.wikipedia.org/wiki/Ramer-Douglas-Peucker_algorithm">Ramer-Douglas-Peucker
 *      algorithm</a>
 */
public class RamanDouglasPeukerDataReducer implements RendererDataReducer {
    private double epsilon = 0.1;

    /**
     * @param epsilon
     *            maximum distance of a point in data between original curve and
     *            simplified curve
     */
    public void setEpsilon(final double epsilon) {
        AssertUtils.gtEqThanZero("epsilon", epsilon);
        this.epsilon = epsilon;
    }

    /**
     * @return {@code epsilon}
     */
    public double getEpsilon() {
        return epsilon;
    }

    public double[][] filter(final double[][] data) {
        return ramerDouglasPeuckerFunction(data, 0, data.length - 1);
    }

    protected double[][] ramerDouglasPeuckerFunction(final double[][] points, final int startIndex,
            final int endIndex) {
        double dmax = 0;
        int idx = 0;
        final double dx = points[endIndex][0] - points[startIndex][0];
        final double dy = points[endIndex][1] - points[startIndex][1];
        final double c = -(dy * points[startIndex][0] - dx * points[startIndex][1]);
        final double norm = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        for (int i = startIndex + 1; i < endIndex; i++) {
            final double distance = Math.abs(dy * points[i][0] - dx * points[i][1] + c) / norm;
            if (distance > dmax) {
                idx = i;
                dmax = distance;
            }
        }
        if (dmax >= epsilon) {
            final double[][] recursiveResult1 = ramerDouglasPeuckerFunction(points, startIndex, idx);
            final double[][] recursiveResult2 = ramerDouglasPeuckerFunction(points, idx, endIndex);
            final double[][] result = new double[recursiveResult1.length - 1 + recursiveResult2.length][2];
            System.arraycopy(recursiveResult1, 0, result, 0, recursiveResult1.length - 1);
            System.arraycopy(recursiveResult2, 0, result, recursiveResult1.length - 1, recursiveResult2.length);
            return result;
        } else {
            return new double[][] { points[startIndex], points[endIndex] };
        }
    }

    @Override
    public int reducePoints(final double[] xValues, final double[] yValues, final double[] xPointErrorsPos,
            final double[] xPointErrorsNeg, final double[] yPointErrorsPos, final double[] yPointErrorsNeg,
            final String[] styles, final boolean[] pointSelected, final int indexMin, final int indexMax) {
        final long startTimeStamp = ProcessingProfiler.getTimeStamp();
        final double[][] data = new double[indexMax - indexMin][2];
        int count = 0;
        for (int i = indexMin; i < indexMax; i++) {
            data[count][0] = xValues[i];
            data[count][1] = yValues[i];
            count++;
        }

        final double[][] ret = ramerDouglasPeuckerFunction(data, 0, data.length - 1);
        double minY = +Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        final double[] xValuesNew = new double[ret.length];
        final double[] yValuesNew = new double[ret.length];
        for (int i = 0; i < ret.length; i++) {
            xValuesNew[i] = ret[i][0];
            yValuesNew[i] = ret[i][1];
            minY = Math.min(minY, yValuesNew[i]);
            maxY = Math.max(maxY, yValuesNew[i]);
        }
        double range = maxY - minY;
        if (range <= 0) {
            range = 1.0;
        }
        epsilon = 100 / range;

        System.arraycopy(xValuesNew, 0, xValues, 0, xValuesNew.length);
        System.arraycopy(yValuesNew, 0, yValues, 0, yValuesNew.length);
        // ArrayUtils.fillArray(xPointErrorsPos, 0);
        // ArrayUtils.fillArray(xPointErrorsNeg, 0);
        ArrayUtils.fillArray(yPointErrorsPos, epsilon);
        ArrayUtils.fillArray(yPointErrorsNeg, epsilon);

        ProcessingProfiler.getTimeDiff(startTimeStamp,
                String.format("data reduction (from %d to %d)", indexMax - indexMin, xValuesNew.length));
        return xValuesNew.length;
    }

}