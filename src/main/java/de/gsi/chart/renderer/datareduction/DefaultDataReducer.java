package de.gsi.chart.renderer.datareduction;

import java.security.InvalidParameterException;

import de.gsi.chart.renderer.RendererDataReducer;
import de.gsi.chart.utils.AssertUtils;
import de.gsi.chart.utils.ProcessingProfiler;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Default data reduction algorithm implementation for the ErrorDataSet Renderer
 * <br>
 * Simple algorithm that reduces the number of points if neighbouring x
 * coordinates are closer than the user-defined dash size. Points in between are
 * dropped and their errors propagated to the following drawn data point. N.B.
 * numerical complexity: average = worst-case = O(n)
 *
 * @author rstein
 */
public class DefaultDataReducer implements RendererDataReducer {

    protected IntegerProperty minPointPixelDistance = new SimpleIntegerProperty(this, "minPixelDistance", 6) {
        @Override
        public void set(final int value) {
            if (value < 0) {
                throw new InvalidParameterException("minPointPixelDistance " + value + " must be greater than zero");
            }
            super.set(value);
        }
    };

    /**
     * @return the <code>minPointPixelDistance</code>.
     */
    public final int getMinPointPixelDistance() {
        return minPointPixelDistanceProperty().get();
    }

    /**
     * Sets the <code>minPointPixelDistance</code> to the specified value.
     *
     * @param minPixelDistance
     *            the minimum distance between two adjacent points.
     */
    public final void setMinPointPixelDistance(final int minPixelDistance) {
        minPointPixelDistanceProperty().setValue(minPixelDistance);
    }

    public final IntegerProperty minPointPixelDistanceProperty() {
        return minPointPixelDistance;
    }

    /**
     * Internal function to the ErrorDataSetRenderer arrays are cached copies
     * and operations are assumed to be performed in-place (<-> for performance
     * reasons/minimisation of memory allocation)
     *
     * @param xValues
     *            array of x coordinates
     * @param yValues
     *            array of y coordinates
     * @param xPointErrorsPos
     *            array of coordinates containing x+exp
     * @param xPointErrorsNeg
     *            array of coordinates containing x-exn
     * @param yPointErrorsPos
     *            array of coordinates containing x+eyp
     * @param yPointErrorsNeg
     *            array of coordinates containing x+eyn
     * @param pointSelected
     *            array containing the points that have been specially selected
     *            by the user
     * @param indexMin
     *            minimum index of those array that shall be considered
     * @param indexMax
     *            maximum index of those array that shall be considered
     * @return effective number of points that remain after the reduction
     */
    @Override
    public int reducePoints(final double[] xValues, final double[] yValues, final double[] xPointErrorsPos,
            final double[] xPointErrorsNeg, final double[] yPointErrorsPos, final double[] yPointErrorsNeg,
            final String[] styles, final boolean[] pointSelected, final int indexMin, final int indexMax) {
        AssertUtils.nonEmptyArray("xValues", xValues);
        final int defaultDataLength = xValues.length;
        AssertUtils.checkArrayDimension("yValues", yValues, defaultDataLength);
        AssertUtils.checkArrayDimension("pointSelected", pointSelected, defaultDataLength);
        AssertUtils.gtEqThanZero("indexMax", indexMin);
        AssertUtils.gtThanZero("indexMax", indexMax);

        final boolean xErrorPos = xPointErrorsPos != null;
        final boolean xErrorNeg = xPointErrorsNeg != null;
        final boolean yErrorPos = yPointErrorsPos != null;
        final boolean yErrorNeg = yPointErrorsNeg != null;

        if (xErrorPos && xErrorNeg && yErrorPos && yErrorNeg) {
            AssertUtils.checkArrayDimension("xPointErrorsPos", xPointErrorsPos, defaultDataLength);
            AssertUtils.checkArrayDimension("xPointErrorsNeg", xPointErrorsNeg, defaultDataLength);
            AssertUtils.checkArrayDimension("yPointErrorsPos", yPointErrorsPos, defaultDataLength);
            AssertUtils.checkArrayDimension("yPointErrorsNeg", yPointErrorsNeg, defaultDataLength);
            return reducePointsInternal(xValues, yValues, xPointErrorsPos, xPointErrorsNeg, yPointErrorsPos,
                    yPointErrorsNeg, styles, pointSelected, indexMin, indexMax);
        } else if (yErrorPos && yErrorNeg) {
            AssertUtils.checkArrayDimension("yPointErrorsPos", yPointErrorsPos, defaultDataLength);
            AssertUtils.checkArrayDimension("yPointErrorsNeg", yPointErrorsNeg, defaultDataLength);
            return reducePointsInternal(xValues, yValues, yPointErrorsPos, yPointErrorsNeg, styles, pointSelected,
                    indexMin, indexMax);
        } else {
            return reducePointsInternal(xValues, yValues, styles, pointSelected, indexMin, indexMax);
        }
    }

    private int reducePointsInternal(final double[] xValues, final double[] yValues, final double[] xPointErrorsPos,
            final double[] xPointErrorsNeg, final double[] yPointErrorsPos, final double[] yPointErrorsNeg,
            final String[] styles, final boolean[] pointSelected, final int indexMin, final int indexMax) {
        final long start = ProcessingProfiler.getTimeStamp();
        int count = 0;
        int ncount = 0;
        double meanX = 0;
        double meanY = 0;
        int minY = +Integer.MAX_VALUE;
        int maxY = -Integer.MAX_VALUE;
        int minX = +Integer.MAX_VALUE;
        int maxX = -Integer.MAX_VALUE;
        String style = null;
        boolean sel = false;
        double xold = xValues[indexMin];
        double yold = yValues[indexMin];
        final int minPixelDistance = getMinPointPixelDistance();

        // add first point - by default (was earlier as a conditional statement
        // in for loop which reduced performance, ie. this code is a hot-spot)
        xValues[count] = xValues[indexMin];
        yValues[count] = yValues[indexMin];
        xPointErrorsNeg[count] = xPointErrorsNeg[indexMin];
        xPointErrorsPos[count] = xPointErrorsPos[indexMin];
        yPointErrorsNeg[count] = yPointErrorsNeg[indexMin];
        yPointErrorsPos[count] = yPointErrorsPos[indexMin];
        pointSelected[count] = pointSelected[indexMin];
        styles[count] = styles[indexMin];
        count++;

        // for loop for points between the first and last point (N.B. we
        // required via ErrorDataSet that indexMax-indexMin is always >3)
        for (int i = indexMin + 1; i < indexMax - 1; i++) {
            final double newXValue = xValues[i];
            final double newYValue = yValues[i];
            final int differenceInX = (int) Math.abs(xold - newXValue);
            final int differenceInY = (int) Math.abs(yold - newYValue);

            // check hor. and ver. pixel distance of new to last drawn point
            if (differenceInX > minPixelDistance || differenceInY > minPixelDistance) {
                if (ncount == 0) {
                    // nothing accumulated simply add point
                    xValues[count] = newXValue;
                    yValues[count] = newYValue;
                    xPointErrorsNeg[count] = xPointErrorsNeg[i];
                    xPointErrorsPos[count] = xPointErrorsPos[i];
                    yPointErrorsNeg[count] = yPointErrorsNeg[i];
                    yPointErrorsPos[count] = yPointErrorsPos[i];
                    pointSelected[count] = pointSelected[i];
                    styles[count] = styles[i];
                    xold = xValues[count];
                    yold = yValues[count];
                    meanX = 0;
                    meanY = 0;
                    minY = +Integer.MAX_VALUE;
                    maxY = -Integer.MAX_VALUE;
                    minX = +Integer.MAX_VALUE;
                    maxX = -Integer.MAX_VALUE;
                    sel = false;
                    ncount = 0;
                    count++;
                    continue;
                }

                // absorbed at least one point before
                // compute mean -- first part: accumulation, min, max
                meanX += newXValue;
                meanY += newYValue;
                minX = Math.min(minX, (int) xPointErrorsPos[i]);
                maxX = Math.max(maxX, (int) xPointErrorsNeg[i]);
                minY = Math.min(minY, (int) yPointErrorsPos[i]);
                maxY = Math.max(maxY, (int) yPointErrorsNeg[i]);
                sel |= pointSelected[i];
                style = styles[i];
                ncount++;

                // compute mean -- second part: (division by number of samples
                // that were absorbed)
                xValues[count] = (int) (meanX / ncount);
                yValues[count] = (int) (meanY / ncount);
                xPointErrorsNeg[count] = minX;
                xPointErrorsPos[count] = maxX;
                yPointErrorsNeg[count] = maxY;
                yPointErrorsPos[count] = minY;
                pointSelected[count] = sel;
                styles[count] = style;
                xold = xValues[count];
                yold = yValues[count];
                count++;

                // reset statistics variables
                meanX = 0;
                meanY = 0;
                minY = +Integer.MAX_VALUE;
                maxY = -Integer.MAX_VALUE;
                minX = +Integer.MAX_VALUE;
                maxX = -Integer.MAX_VALUE;
                sel = false;
                ncount = 0;
            } else {
                // points are closer than the dash size, drop new point
                // compute mean -- first part: accumulation, min, max
                meanX += newXValue;
                meanY += newYValue;
                minY = Math.min(minY, (int) yPointErrorsPos[i]);
                maxY = Math.max(maxY, (int) yPointErrorsNeg[i]);
                minX = Math.min(minX, (int) xPointErrorsPos[i]);
                maxX = Math.max(maxX, (int) xPointErrorsNeg[i]);
                sel |= pointSelected[i];
                ncount++;
            }
        }

        // add last point - by default (was earlier as a conditional statement
        // in for loop which reduced performance, ie. this code is a hot-spot)
        minX = Math.min(minX, (int) xPointErrorsPos[indexMax - 1]);
        maxX = Math.max(maxX, (int) xPointErrorsNeg[indexMax - 1]);
        minY = Math.min(minY, (int) yPointErrorsPos[indexMax - 1]);
        maxY = Math.max(maxY, (int) yPointErrorsNeg[indexMax - 1]);
        xValues[count] = xValues[indexMax - 1];
        yValues[count] = yValues[indexMax - 1];
        xPointErrorsNeg[count] = minX;
        xPointErrorsPos[count] = maxX;
        yPointErrorsNeg[count] = minY;
        yPointErrorsPos[count] = maxY;
        pointSelected[count] = pointSelected[indexMax - 1];
        styles[count] = styles[indexMax - 1];
        count++;

        ProcessingProfiler.getTimeDiff(start,
                String.format("data reduction (from %d to %d)", indexMax - indexMin, count));
        return count;
    }

    private int reducePointsInternal(final double[] xValues, final double[] yValues, final double[] yPointErrorsPos,
            final double[] yPointErrorsNeg, final String[] styles, final boolean[] pointSelected, final int indexMin,
            final int indexMax) {
        final long start = ProcessingProfiler.getTimeStamp();
        int count = 0;
        int ncount = 0;
        double meanX = 0;
        double meanY = 0;
        int minY = +Integer.MAX_VALUE;
        int maxY = -Integer.MAX_VALUE;
        String style = null;
        boolean sel = false;
        double xold = xValues[indexMin];
        double yold = yValues[indexMin];
        final int minPixelDistance = getMinPointPixelDistance();

        // add first point - by default (was earlier as a conditional statement
        // in for loop which reduced performance, ie. this code is a hot-spot)
        xValues[count] = xValues[indexMin];
        yValues[count] = yValues[indexMin];
        yPointErrorsNeg[count] = yPointErrorsNeg[indexMin];
        yPointErrorsPos[count] = yPointErrorsPos[indexMin];
        pointSelected[count] = pointSelected[indexMin];
        styles[count] = styles[indexMin];
        count++;

        // for loop for points between the first and last point (N.B. we
        // required via ErrorDataSet that indexMax-indexMin is always >3)
        for (int i = indexMin + 1; i < indexMax - 1; i++) {
            final double newXValue = xValues[i];
            final double newYValue = yValues[i];
            final int differenceInX = (int) Math.abs(xold - newXValue);
            final int differenceInY = (int) Math.abs(yold - newYValue);

            // check hor. and ver. pixel distance of new to last drawn point
            if (differenceInX > minPixelDistance || differenceInY > minPixelDistance) {
                if (ncount == 0) {
                    // nothing accumulated simply add point
                    xValues[count] = newXValue;
                    yValues[count] = newYValue;
                    yPointErrorsNeg[count] = yPointErrorsNeg[i];
                    yPointErrorsPos[count] = yPointErrorsPos[i];
                    pointSelected[count] = pointSelected[i];
                    styles[count] = styles[i];
                    xold = xValues[count];
                    yold = yValues[count];
                    meanX = 0;
                    meanY = 0;
                    minY = +Integer.MAX_VALUE;
                    maxY = -Integer.MAX_VALUE;
                    sel = false;
                    ncount = 0;
                    count++;
                    continue;
                }

                // absorbed at least one point before
                // compute mean -- first part: accumulation, min, max
                meanX += newXValue;
                meanY += newYValue;
                minY = Math.min(minY, (int) yPointErrorsPos[i]);
                maxY = Math.max(maxY, (int) yPointErrorsNeg[i]);
                sel |= pointSelected[i];
                style = styles[i];
                ncount++;

                // compute mean -- second part: (division by number of samples
                // that were absorbed)
                xValues[count] = (int) (meanX / ncount);
                yValues[count] = (int) (meanY / ncount);
                yPointErrorsNeg[count] = maxY;
                yPointErrorsPos[count] = minY;
                pointSelected[count] = sel;
                styles[count] = style;
                xold = xValues[count];
                yold = yValues[count];
                count++;

                // reset statistics variables
                meanX = 0;
                meanY = 0;
                minY = +Integer.MAX_VALUE;
                maxY = -Integer.MAX_VALUE;
                sel = false;
                ncount = 0;
            } else {
                // points are closer than the dash size, drop new point
                // compute mean -- first part: accumulation, min, max
                meanX += newXValue;
                meanY += newYValue;
                minY = Math.min(minY, (int) yPointErrorsNeg[i]);
                maxY = Math.max(maxY, (int) yPointErrorsPos[i]);
                sel |= pointSelected[i];
                style = styles[i];
                ncount++;
            }
        }

        // add last point - by default (was earlier as a conditional statement
        // in for loop which reduced performance, ie. this code is a hot-spot)
        minY = Math.min(minY, (int) yPointErrorsPos[indexMax - 1]);
        maxY = Math.max(maxY, (int) yPointErrorsNeg[indexMax - 1]);
        xValues[count] = xValues[indexMax - 1];
        yValues[count] = yValues[indexMax - 1];
        yPointErrorsNeg[count] = minY;
        yPointErrorsPos[count] = maxY;
        pointSelected[count] = pointSelected[indexMax - 1];
        styles[count] = styles[indexMax - 1];
        count++;

        ProcessingProfiler.getTimeDiff(start,
                String.format("data reduction (from %d to %d)", indexMax - indexMin, count));
        return count;
    }

    private int reducePointsInternal(final double[] xValues, final double[] yValues, final String[] styles,
            final boolean[] pointSelected, final int indexMin, final int indexMax) {
        final long start = ProcessingProfiler.getTimeStamp();
        int count = 0;
        int ncount = 0;
        double meanX = 0;
        double meanY = 0;
        String style = null;
        boolean sel = false;
        double xold = xValues[indexMin];
        double yold = yValues[indexMin];
        final int minPixelDistance = getMinPointPixelDistance();

        // add first point - by default (was earlier as a conditional statement
        // in for loop which reduced performance, ie. this code is a hot-spot)
        xValues[count] = xValues[indexMin];
        yValues[count] = yValues[indexMin];
        pointSelected[count] = pointSelected[indexMin];
        styles[count] = styles[indexMin];
        count++;

        // for loop for points between the first and last point (N.B. we
        // required via ErrorDataSet that indexMax-indexMin is always >3)
        for (int i = indexMin + 1; i < indexMax - 1; i++) {
            final double newXValue = xValues[i];
            final double newYValue = yValues[i];
            final int differenceInX = (int) Math.abs(xold - newXValue);
            final int differenceInY = (int) Math.abs(yold - newYValue);

            // check hor. and ver. pixel distance of new to last drawn point
            if (differenceInX > minPixelDistance || differenceInY > minPixelDistance) {
                if (ncount == 0) {
                    // nothing accumulated simply add point
                    xValues[count] = newXValue;
                    yValues[count] = newYValue;
                    pointSelected[count] = pointSelected[i];
                    styles[count] = styles[i];
                    xold = xValues[count];
                    yold = yValues[count];
                    meanX = 0;
                    meanY = 0;
                    sel = false;
                    ncount = 0;
                    count++;
                    continue;
                }

                // absorbed at least one point before
                // compute mean -- first part: accumulation, min, max
                meanX += newXValue;
                meanY += newYValue;
                sel |= pointSelected[i];
                style = styles[i];
                ncount++;

                // compute mean -- second part: (division by number of samples
                // that were absorbed)
                xValues[count] = (int) (meanX / ncount);
                yValues[count] = (int) (meanY / ncount);
                pointSelected[count] = sel;
                styles[count] = style;
                xold = xValues[count];
                yold = yValues[count];
                count++;

                // reset statistics variables
                meanX = 0;
                meanY = 0;
                sel = false;
                ncount = 0;
            } else {
                // points are closer than the dash size, drop new point
                // compute mean -- first part: accumulation, min, max
                meanX += newXValue;
                meanY += newYValue;
                sel |= pointSelected[i];
                ncount++;

            }
        }

        // add last point - by default (was earlier as a conditional statement
        // in for loop which reduced performance, ie. this code is a hot-spot)
        xValues[count] = xValues[indexMax - 1];
        yValues[count] = yValues[indexMax - 1];
        pointSelected[count] = pointSelected[indexMax - 1];
        styles[count] = styles[indexMax - 1];
        count++;

        ProcessingProfiler.getTimeDiff(start,
                String.format("data reduction (from %d to %d)", indexMax - indexMin, count));
        return count;
    }

}
