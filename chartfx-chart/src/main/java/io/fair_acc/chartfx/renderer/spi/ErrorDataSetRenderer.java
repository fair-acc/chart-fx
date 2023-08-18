package io.fair_acc.chartfx.renderer.spi;

import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.chartfx.ui.css.ErrorStyleParser;
import io.fair_acc.chartfx.utils.FastDoubleArrayCache;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.FillRule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.marker.Marker;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.utils.BezierCurve;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError.ErrorType;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * Renders data points with error bars and/or error surfaces It can be used e.g. to render horizontal and/or vertical
 * errors additional functionality:
 * <ul>
 * <li>bar-type plot
 * <li>polar-axis plotting
 * <li>scatter and/or bubble-chart-type plots
 * </ul>
 *
 * @author R.J. Steinhagen
 */
@SuppressWarnings({ "PMD.LongVariable", "PMD.ShortVariable" }) // short variables like x, y are perfectly fine, as well
// as descriptive long ones
public class ErrorDataSetRenderer extends AbstractErrorDataSetRendererParameter<ErrorDataSetRenderer>
        implements Renderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorDataSetRenderer.class);

    @Deprecated // should go on styleable node
    private Marker marker = DefaultMarker.DEFAULT;

    private final ErrorStyleParser styleParser = new ErrorStyleParser();

    /**
     * Creates new <code>ErrorDataSetRenderer</code>.
     */
    public ErrorDataSetRenderer() {
        this(3);
    }

    /**
     * Creates new <code>ErrorDataSetRenderer</code>.
     *
     * @param dashSize initial size (top/bottom cap) on top of the error bars
     */
    public ErrorDataSetRenderer(final int dashSize) {
        setDashSize(dashSize);
    }

    /**
     * @param style the data set for which the representative icon should be generated
     * @param canvas the canvas in which the representative icon should be drawn
     * @return true if the renderer generates symbols that should be displayed
     */
    @Override
    public boolean drawLegendSymbol(final DataSetNode style, final Canvas canvas) {
        final int width = (int) canvas.getWidth();
        final int height = (int) canvas.getHeight();
        final GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.save();

        gc.setLineWidth(style.getLineWidth());
        gc.setLineDashes(style.getLineDashes());
        gc.setStroke(style.getLineColor());

        if (getErrorType() == ErrorStyle.ERRORBARS) {
            final double x = width / 2.0;
            final double y = height / 2.0;
            if (getDashSize() > 2) {
                gc.strokeLine(x - 1.0, 1, x + 1.0, 1.0);
                gc.strokeLine(x - 1.0, height - 2.0, x + 1.0, height - 2.0);
                gc.strokeLine(x, 1.0, x, height - 2.0);
            }
            gc.strokeLine(1, y, width, y);
        } else if (getErrorType() == ErrorStyle.ERRORSURFACE || getErrorType() == ErrorStyle.ERRORCOMBO) {
            final double y = height / 2.0;
            gc.setFill(style.getLineFillPattern());
            gc.fillRect(1, 1, width - 2.0, height - 2.0);
            gc.strokeLine(1, y, width - 2.0, y);
        } else {
            final double x = width / 2.0;
            final double y = height / 2.0;
            if (getDashSize() > 2) {
                gc.strokeLine(x - 1.0, 1.0, x + 1.0, 1.0);
                gc.strokeLine(x - 1.0, height - 2.0, x + 1, height - 2.0);
                gc.strokeLine(x, 1.0, x, height - 2.0);
            }
            gc.strokeLine(1, y, width - 2.0, y);
        }
        gc.restore();
        return true;
    }

    /**
     * Returns the marker used by this renderer.
     *
     * @return the marker to be drawn on the data points
     */
    public Marker getMarker() {
        return marker;
    }

    @Override
    protected void render(final GraphicsContext gc, final DataSet dataSet, final DataSetNode style) {
        // N.B. print out for debugging purposes, please keep (used for
        // detecting redundant or too frequent render updates)
        // System.err.println(String.format("render for range [%f,%f] and dataset = '%s'", xMin, xMax, dataSet.getName()));

        var timestamp = ProcessingProfiler.getTimeStamp();
        int indexMin;
        int indexMax; /* indexMax is excluded in the drawing */
        if (isAssumeSortedData()) {
            indexMin = Math.max(0, dataSet.getIndex(DataSet.DIM_X, xMin) - 1);
            indexMax = Math.min(dataSet.getIndex(DataSet.DIM_X, xMax) + 2, dataSet.getDataCount());
        } else {
            indexMin = 0;
            indexMax = dataSet.getDataCount();
        }

        // zero length/range data set -> nothing to be drawn
        if (indexMax - indexMin <= 0) {
            return;
        }

        if (ProcessingProfiler.getDebugState()) {
            timestamp = ProcessingProfiler.getTimeDiff(timestamp,
                    "get min/max" + String.format(" from:%d to:%d", indexMin, indexMax));
        }

        final CachedDataPoints points = SHARED_POINTS_CACHE.resizeMin(indexMin, indexMax, dataSet.getDataCount(), true);
        if (ProcessingProfiler.getDebugState()) {
            timestamp = ProcessingProfiler.getTimeDiff(timestamp, "get CachedPoints");
        }

        // compute local screen coordinates
        final boolean isPolarPlot = getChart().isPolarPlot();
        if (isParallelImplementation()) {
            points.computeScreenCoordinatesInParallel(xAxis, yAxis, dataSet, style,
                    indexMin, indexMax, getErrorType(), isPolarPlot,
                    isallowNaNs());
        } else {
            points.computeScreenCoordinates(xAxis, yAxis, dataSet, style,
                    indexMin, indexMax, getErrorType(), isPolarPlot, isallowNaNs());
        }
        if (ProcessingProfiler.getDebugState()) {
            timestamp = ProcessingProfiler.getTimeDiff(timestamp, "computeScreenCoordinates()");
        }

        // invoke data reduction algorithm
        points.reduce(rendererDataReducerProperty().get(), isReducePoints(),
                getMinRequiredReductionSize());

        // draw individual plot components
        drawChartComponents(gc, style, points);
        if (ProcessingProfiler.getDebugState()) {
            timestamp = ProcessingProfiler.getTimeDiff(timestamp, "drawChartComponents()");
        }

    }

    /**
     * Replaces marker used by this renderer.
     *
     * @param marker the marker to be drawn on the data points
     */
    public void setMarker(final Marker marker) {
        this.marker = marker;
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param points reference to local cached data point object
     */
    protected void drawBars(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
        if (!isDrawBars()) {
            return;
        }

        final int xOffset = Math.max(style.getGlobalIndex(), 0);
        final int minRequiredWidth = Math.max(getDashSize(), points.minDistanceX);

        final double barWPercentage = getBarWidthPercentage();
        final double dynBarWidth = minRequiredWidth * barWPercentage / 100;
        final double constBarWidth = getBarWidth();
        final double localBarWidth = isDynamicBarWidth() ? dynBarWidth : constBarWidth;
        final double barWidthHalf = localBarWidth / 2 - (isShiftBar() ? xOffset * getShiftBarOffset() : 0);

        gc.save();

        // Bars are drawn like markers
        gc.setLineWidth(style.getMarkerLineWidth());
        var markerColor = style.getMarkerColor();
        gc.setStroke(markerColor);
        gc.setFill(markerColor);

        if (points.polarPlot) {
            for (int i = 0; i < points.actualDataCount; i++) {
                if (points.styles[i] == null || !styleParser.tryParse(points.styles[i])) {
                    gc.strokeLine(points.xZero, points.yZero, points.xValues[i],
                            points.yValues[i]);
                } else {
                    // work-around: bar colour controlled by the marker color
                    gc.save();
                    styleParser.getFillColor().ifPresent(gc::setFill);
                    gc.setLineWidth(barWidthHalf);
                    gc.strokeLine(points.xZero, points.yZero, points.xValues[i],
                            points.yValues[i]);
                    gc.restore();
                }
            }
        } else {
            for (int i = 0; i < points.actualDataCount; i++) {
                double yDiff = points.yValues[i] - points.yZero;
                final double yMin;
                if (yDiff > 0) {
                    yMin = points.yZero;
                } else {
                    yMin = points.yValues[i];
                    yDiff = Math.abs(yDiff);
                }

                if (points.styles[i] == null || !styleParser.tryParse(points.styles[i])) {
                    gc.fillRect(points.xValues[i] - barWidthHalf, yMin, localBarWidth, yDiff);

                } else {
                    gc.save();
                    styleParser.getFillColor().ifPresent(gc::setFill);
                    gc.fillRect(points.xValues[i] - barWidthHalf, yMin, localBarWidth, yDiff);
                    gc.restore();
                }
            }
        }

        gc.restore();
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param points reference to local cached data point object
     */
    protected void drawBubbles(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
        if (!isDrawBubbles()) {
            return;
        }
        gc.save();

        // N.B. bubbles are drawn with the same colour as polyline
        gc.setFill(style.getLineColor());
        final double minSize = style.getMarkerSize();

        if (points.errorType[DataSet.DIM_X] != ErrorType.NO_ERROR && points.errorType[DataSet.DIM_Y] == ErrorType.NO_ERROR) {
            // X, X_ASYMMETRIC
            for (int i = 0; i < points.actualDataCount; i++) {
                final double radius = Math.max(minSize, points.errorXPos[i] - points.errorXNeg[i]);
                final double x = points.xValues[i] - radius;
                final double y = points.yValues[i] - radius;

                gc.fillOval(x, y, 2 * radius, 2 * radius);
            }
        } else if (points.errorType[DataSet.DIM_X] == ErrorType.NO_ERROR && points.errorType[DataSet.DIM_Y] != ErrorType.NO_ERROR) {
            // Y, Y_ASYMMETRIC
            for (int i = 0; i < points.actualDataCount; i++) {
                final double radius = Math.max(minSize, points.errorYNeg[i] - points.errorYPos[i]);
                final double x = points.xValues[i] - radius;
                final double y = points.yValues[i] - radius;

                gc.fillOval(x, y, 2 * radius, 2 * radius);
            }
        } else if (points.errorType[DataSet.DIM_X] != ErrorType.NO_ERROR && points.errorType[DataSet.DIM_Y] != ErrorType.NO_ERROR) {
            // XY, XY_ASYMMETRIC
            for (int i = 0; i < points.actualDataCount; i++) {
                final double width = Math.max(minSize, points.errorXPos[i] - points.errorXNeg[i]);
                final double height = Math.max(minSize, points.errorYNeg[i] - points.errorYPos[i]);
                final double x = points.xValues[i] - width;
                final double y = points.yValues[i] - height;

                gc.fillOval(x, y, 2 * width, 2 * height);
            }
        } else { // NO ERROR
            for (int i = 0; i < points.actualDataCount; i++) {
                final double x = points.xValues[i] - minSize;
                final double y = points.yValues[i] - minSize;

                gc.fillOval(x, y, 2 * minSize, 2 * minSize);
            }
        }

        gc.restore();
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param points reference to local cached data point object
     */
    protected void drawDefaultNoErrors(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
        drawBars(gc, style, points);
        drawPolyLine(gc, style, points);
        drawMarker(gc, style, points);
        drawBubbles(gc, style, points);
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param points reference to local cached data point object
     */
    protected void drawErrorBars(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
        final long start = ProcessingProfiler.getTimeStamp();

        drawBars(gc, style, points);

        final int dashHalf = getDashSize() / 2;
        gc.save();

        gc.setStroke(style.getLineColor());
        gc.setLineWidth(style.getLineWidth());

        for (int i = 0; i < points.actualDataCount; i++) {
            if (points.errorType[DataSet.DIM_X] != ErrorType.NO_ERROR
                    && points.errorType[DataSet.DIM_Y] != ErrorType.NO_ERROR) {
                // draw error bars
                gc.strokeLine(points.xValues[i], points.errorYNeg[i], points.xValues[i], points.errorYPos[i]);
                gc.strokeLine(points.errorXNeg[i], points.yValues[i], points.errorXPos[i], points.yValues[i]);

                // draw horizontal dashes
                gc.strokeLine(points.xValues[i] - dashHalf, points.errorYNeg[i], points.xValues[i] + dashHalf,
                        points.errorYNeg[i]);
                gc.strokeLine(points.xValues[i] - dashHalf, points.errorYPos[i], points.xValues[i] + dashHalf,
                        points.errorYPos[i]);

                // draw vertical dashes
                gc.strokeLine(points.errorXNeg[i], points.yValues[i] - dashHalf, points.errorXNeg[i],
                        points.yValues[i] + dashHalf);
                gc.strokeLine(points.errorXPos[i], points.yValues[i] - dashHalf, points.errorXPos[i],
                        points.yValues[i] + dashHalf);
            } else if (points.errorType[DataSet.DIM_X] == ErrorType.NO_ERROR
                       && points.errorType[DataSet.DIM_Y] != ErrorType.NO_ERROR) {
                // draw error bars
                gc.strokeLine(points.xValues[i], points.errorYNeg[i], points.xValues[i], points.errorYPos[i]);

                // draw horizontal dashes
                gc.strokeLine(points.xValues[i] - dashHalf, points.errorYNeg[i], points.xValues[i] + dashHalf,
                        points.errorYNeg[i]);
                gc.strokeLine(points.xValues[i] - dashHalf, points.errorYPos[i], points.xValues[i] + dashHalf,
                        points.errorYPos[i]);
            } else if (points.errorType[DataSet.DIM_X] != ErrorType.NO_ERROR
                       && points.errorType[DataSet.DIM_Y] == ErrorType.NO_ERROR) {
                // draw error bars
                gc.strokeLine(points.errorXNeg[i], points.yValues[i], points.errorXPos[i], points.yValues[i]);

                // draw horizontal dashes
                gc.strokeLine(points.xValues[i] - dashHalf, points.errorYNeg[i], points.xValues[i] + dashHalf,
                        points.errorYNeg[i]);
                gc.strokeLine(points.xValues[i] - dashHalf, points.errorYPos[i], points.xValues[i] + dashHalf,
                        points.errorYPos[i]);
            }
        }
        gc.restore();

        drawPolyLine(gc, style, points);
        drawMarker(gc, style, points);
        drawBubbles(gc, style, points);

        ProcessingProfiler.getTimeDiff(start);
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param points reference to local cached data point object
     */
    protected void drawErrorSurface(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
        final long start = ProcessingProfiler.getTimeStamp();

        gc.setFill(style.getLineFillPattern());

        final int nDataCount = points.actualDataCount;
        final int nPolygoneEdges = 2 * nDataCount;
        final double[] xValuesSurface = SHARED_ARRAYS.getArray(0, nPolygoneEdges);
        final double[] yValuesSurface = SHARED_ARRAYS.getArray(1, nPolygoneEdges);

        final int xend = nPolygoneEdges - 1;
        for (int i = 0; i < nDataCount; i++) {
            xValuesSurface[i] = points.xValues[i];
            yValuesSurface[i] = points.errorYNeg[i];
            xValuesSurface[xend - i] = points.xValues[i];
            yValuesSurface[xend - i] = points.errorYPos[i];
        }

        gc.setFillRule(FillRule.EVEN_ODD);
        gc.fillPolygon(xValuesSurface, yValuesSurface, nPolygoneEdges);

        drawPolyLine(gc, style, points);
        drawBars(gc, style, points);
        drawMarker(gc, style, points);
        drawBubbles(gc, style, points);

        ProcessingProfiler.getTimeDiff(start);
    }

    /**
     * NaN compatible algorithm
     *
     * @param gc the graphics context from the Canvas parent
     * @param points reference to local cached data point object
     */
    protected void drawErrorSurfaceNaNCompatible(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
        final long start = ProcessingProfiler.getTimeStamp();

        gc.setFill(style.getLineFillPattern());
        gc.setFillRule(FillRule.EVEN_ODD);

        final int nDataCount = points.actualDataCount;
        final int nPolygoneEdges = 2 * nDataCount;
        final double[] xValuesSurface = SHARED_ARRAYS.getArray(0, nPolygoneEdges);
        final double[] yValuesSurface = SHARED_ARRAYS.getArray(1, nPolygoneEdges);

        final int xend = nPolygoneEdges - 1;
        int count = 0;
        for (int i = 0; i < nDataCount; i++) {
            final double x = points.xValues[i];
            final double yen = points.errorYNeg[i];
            final double yep = points.errorYPos[i];

            if (Double.isFinite(yen) && Double.isFinite(yep)) {
                xValuesSurface[count] = x;
                yValuesSurface[count] = yep;
                xValuesSurface[xend - count] = x;
                yValuesSurface[xend - count] = yen;
                count++;
            } else if (count != 0) {
                // remove zeros and plot intermediate segment
                compactVector(xValuesSurface, count);
                compactVector(yValuesSurface, count);

                gc.fillPolygon(xValuesSurface, yValuesSurface, 2 * count);
                count = 0;
            }
        }
        if (count > 0) {
            // swap y coordinates at mid-point
            // remove zeros and plot intermediate segment
            compactVector(xValuesSurface, count);
            compactVector(yValuesSurface, count);
            if (count > 4) {
                final double yTmp = yValuesSurface[count - 1];
                yValuesSurface[count - 1] = yValuesSurface[count];
                yValuesSurface[count] = yTmp;
            }

            gc.fillPolygon(xValuesSurface, yValuesSurface, 2 * count);
        }

        drawPolyLine(gc, style, points);
        drawBars(gc, style, points);
        drawMarker(gc, style, points);
        drawBubbles(gc, style, points);

        ProcessingProfiler.getTimeDiff(start);
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param localCachedPoints reference to local cached data point object
     */
    protected void drawMarker(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints localCachedPoints) {
        if (!isDrawMarker()) {
            return;
        }
        gc.save();

        Marker marker = style.getMarkerType();
        var markerColor = style.getMarkerColor();
        double markerSize = style.getMarkerSize();

        gc.setLineWidth(style.getMarkerLineWidth());
        gc.setStroke(markerColor);
        gc.setFill(markerColor);

        for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
            final double x = localCachedPoints.xValues[i];
            final double y = localCachedPoints.yValues[i];
            if (localCachedPoints.styles[i] == null || !styleParser.tryParse(localCachedPoints.styles[i])) {
                marker.draw(gc, x, y, markerSize);
            } else {
                var customColor = styleParser.getMarkerColor().orElse(markerColor);
                Marker customMarker = styleParser.getMarker().orElse(marker);
                double customSize = styleParser.getMarkerSize().orElse(markerSize);
                gc.save();
                gc.setFill(customColor);
                gc.setStroke(customColor);
                customMarker.draw(gc, x, y, customSize);
                gc.restore();
            }
        }

        gc.restore();
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param points reference to local cached data point object
     */
    protected void drawPolyLine(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
        switch (getPolyLineStyle()) {
        case NONE:
            return;
        case AREA:
            drawPolyLineArea(gc, style, points);
            break;
        case ZERO_ORDER_HOLDER:
        case STAIR_CASE:
            drawPolyLineStairCase(gc, style, points);
            break;
        case HISTOGRAM:
            drawPolyLineHistogram(gc, style, points);
            break;
        case HISTOGRAM_FILLED:
            drawPolyLineHistogramFilled(gc, style, points);
            break;
        case BEZIER_CURVE:
            drawPolyLineHistogramBezier(gc, style, points);
            break;
        case NORMAL:
        default:
            drawPolyLineLine(gc, style, points);
            break;
        }
    }

    /**
     * @return the instance of this ErrorDataSetRenderer.
     */
    @Override
    protected ErrorDataSetRenderer getThis() {
        return this;
    }

    private void drawChartComponents(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
        final long start = ProcessingProfiler.getTimeStamp();
        switch (getErrorType()) {
        case ERRORBARS:
            drawErrorBars(gc, style, points);
            break;
        case ERRORSURFACE:
            if (isallowNaNs()) {
                drawErrorSurfaceNaNCompatible(gc, style, points);
            } else {
                drawErrorSurface(gc, style, points);
            }
            break;
        case ERRORCOMBO:
            if (points.getMinXDistance() >= getDashSize() * 2) {
                drawErrorBars(gc, style, points);
            } else {
                if (isallowNaNs()) {
                    drawErrorSurfaceNaNCompatible(gc, style, points);
                } else {
                    drawErrorSurface(gc, style, points);
                }
            }
            break;
        case NONE:
        default:
            drawDefaultNoErrors(gc, style, points);
            break;
        }
        ProcessingProfiler.getTimeDiff(start);
    }

    protected static void drawPolyLineArea(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
        final int n = points.actualDataCount;
        if (n == 0) {
            return;
        }

        final int length = n + 2;
        final double[] newX = SHARED_ARRAYS.getArray(0, length);
        final double[] newY = SHARED_ARRAYS.getArray(1, length);

        final double zero = points.yZero;
        System.arraycopy(points.xValues, 0, newX, 0, n);
        System.arraycopy(points.yValues, 0, newY, 0, n);
        newX[n] = points.xValues[n - 1];
        newY[n] = zero;
        newX[n + 1] = points.xValues[0];
        newY[n + 1] = zero;

        gc.save();
        gc.setFill(style.getLineColor());
        gc.fillPolygon(newX, newY, length);
        gc.restore();
    }

    protected static void drawPolyLineHistogram(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
        final int n = points.actualDataCount;
        if (n == 0) {
            return;
        }

        final int length = 2 * (n + 1);
        final double[] newX = SHARED_ARRAYS.getArray(0, length);
        final double[] newY = SHARED_ARRAYS.getArray(1, length);

        final double xRange = points.xMax - points.xMin;
        double diffLeft;
        double diffRight = n > 0 ? 0.5 * (points.xValues[1] - points.xValues[0]) : 0.5 * xRange;
        newX[0] = points.xValues[0] - diffRight;
        newY[0] = points.yZero;
        for (int i = 0; i < n; i++) {
            diffLeft = points.xValues[i] - newX[2 * i];
            diffRight = i + 1 < n ? 0.5 * (points.xValues[i + 1] - points.xValues[i]) : diffLeft;
            if (i == 0) {
                diffLeft = diffRight;
            }

            newX[2 * i + 1] = points.xValues[i] - diffLeft;
            newY[2 * i + 1] = points.yValues[i];
            newX[2 * i + 2] = points.xValues[i] + diffRight;
            newY[2 * i + 2] = points.yValues[i];
        }
        // last point
        newX[length - 1] = points.xValues[n - 1] + diffRight;
        newY[length - 1] = points.yZero;

        gc.save();
        gc.setStroke(style.getLineColor());
        gc.setLineWidth(style.getLineWidth());
        gc.setLineDashes(style.getLineDashes());

        for (int i = 0; i < length - 1; i++) {
            final double x1 = newX[i];
            final double x2 = newX[i + 1];
            final double y1 = newY[i];
            final double y2 = newY[i + 1];
            gc.strokeLine(x1, y1, x2, y2);
        }

        gc.restore();
    }

    protected static void drawPolyLineHistogramBezier(final GraphicsContext gc,
            final DataSetNode style,
            final CachedDataPoints points) {
        final int n = points.actualDataCount;
        if (n < 2) {
            drawPolyLineLine(gc, style, points);
            return;
        }

        final double[] xCp1 = SHARED_ARRAYS.getArray(0, n);
        final double[] yCp1 = SHARED_ARRAYS.getArray(1, n);
        final double[] xCp2 = SHARED_ARRAYS.getArray(2, n);
        final double[] yCp2 = SHARED_ARRAYS.getArray(3, n);

        BezierCurve.calcCurveControlPoints(points.xValues, points.yValues, xCp1, yCp1, xCp2, yCp2,
                points.actualDataCount);

        gc.save();

        gc.setLineWidth(style.getLineWidth());
        gc.setLineDashes(style.getLineDashes());
        gc.setStroke(style.getLineColor());
        gc.setFill(gc.getStroke());

        gc.beginPath();
        for (int i = 0; i < n - 1; i++) {
            final double x0 = points.xValues[i];
            final double x1 = points.xValues[i + 1];
            final double y0 = points.yValues[i];
            final double y1 = points.yValues[i + 1];

            // coordinates of first Bezier control point.
            final double xc0 = xCp1[i];
            final double yc0 = yCp1[i];
            // coordinates of the second Bezier control point.
            final double xc1 = xCp2[i];
            final double yc1 = yCp2[i];

            gc.moveTo(x0, y0);
            gc.bezierCurveTo(xc0, yc0, xc1, yc1, x1, y1);
        }
        gc.moveTo(points.xValues[n - 1], points.yValues[n - 1]);
        gc.closePath();
        gc.stroke();
        gc.restore();
    }

    protected static void drawPolyLineHistogramFilled(final GraphicsContext gc,
                                                      final DataSetNode style,
                                                      final CachedDataPoints points) {
        final int n = points.actualDataCount;
        if (n == 0) {
            return;
        }

        final int length = 2 * (n + 1);
        final double[] newX = SHARED_ARRAYS.getArray(0, length);
        final double[] newY = SHARED_ARRAYS.getArray(1, length);

        final double xRange = points.xMax - points.xMin;
        double diffLeft;
        double diffRight = n > 0 ? 0.5 * (points.xValues[1] - points.xValues[0]) : 0.5 * xRange;
        newX[0] = points.xValues[0] - diffRight;
        newY[0] = points.yZero;
        for (int i = 0; i < n; i++) {
            diffLeft = points.xValues[i] - newX[2 * i];
            diffRight = i + 1 < n ? 0.5 * (points.xValues[i + 1] - points.xValues[i]) : diffLeft;
            if (i == 0) {
                diffLeft = diffRight;
            }

            newX[2 * i + 1] = points.xValues[i] - diffLeft;
            newY[2 * i + 1] = points.yValues[i];
            newX[2 * i + 2] = points.xValues[i] + diffRight;
            newY[2 * i + 2] = points.yValues[i];
        }
        // last point
        newX[length - 1] = points.xValues[n - 1] + diffRight;
        newY[length - 1] = points.yZero;

        gc.save();
        gc.setFill(style.getLineColor());
        gc.fillPolygon(newX, newY, length);
        gc.restore();
    }

    protected static void drawPolyLineLine(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
        gc.save();

        gc.setLineWidth(style.getLineWidth());
        gc.setLineDashes(style.getLineDashes());
        gc.setStroke(style.getLineColor());
        gc.setFill(gc.getStroke());

        if (points.allowForNaNs) {
            gc.beginPath();
            gc.moveTo(points.xValues[0], points.yValues[0]);
            boolean lastIsFinite = true;
            double xLastValid = 0.0;
            double yLastValid = 0.0;
            for (int i = 0; i < points.actualDataCount; i++) {
                final double x0 = points.xValues[i];
                final double y0 = points.yValues[i];
                if (Double.isFinite(x0) && Double.isFinite(y0)) {
                    if (!lastIsFinite) {
                        gc.moveTo(x0, y0);
                        lastIsFinite = true;
                        continue;
                    }
                    gc.lineTo(x0, y0);
                    xLastValid = x0;
                    yLastValid = y0;
                    lastIsFinite = true;
                } else {
                    lastIsFinite = false;
                }
            }
            gc.moveTo(xLastValid, yLastValid);
            gc.closePath();
            gc.stroke();
        } else {
            if (gc.getLineDashes() != null) {
                gc.strokePolyline(points.xValues, points.yValues, points.actualDataCount);
            } else {
                for (int i = 0; i < points.actualDataCount - 1; i++) {
                    final double x1 = points.xValues[i];
                    final double x2 = points.xValues[i + 1];
                    final double y1 = points.yValues[i];
                    final double y2 = points.yValues[i + 1];

                    gc.strokeLine(x1, y1, x2, y2);
                }
            }
        }

        gc.restore();
    }

    protected static void drawPolyLineStairCase(final GraphicsContext gc, final DataSetNode style, final CachedDataPoints points) {
        final int n = points.actualDataCount;
        if (n == 0) {
            return;
        }

        // need to allocate new array :-(
        final int length = 2 * n;
        final double[] newX = SHARED_ARRAYS.getArray(0, length);
        final double[] newY = SHARED_ARRAYS.getArray(1, length);

        for (int i = 0; i < n - 1; i++) {
            newX[2 * i] = points.xValues[i];
            newY[2 * i] = points.yValues[i];
            newX[2 * i + 1] = points.xValues[i + 1];
            newY[2 * i + 1] = points.yValues[i];
        }
        // last point
        newX[length - 2] = points.xValues[n - 1];
        newY[length - 2] = points.yValues[n - 1];
        newX[length - 1] = points.xMax;
        newY[length - 1] = points.yValues[n - 1];

        gc.save();

        gc.setStroke(style.getLineColor());
        gc.setFill(gc.getStroke());
        gc.setLineWidth(style.getLineWidth());
        gc.setLineDashes(style.getLineDashes());

        // gc.strokePolyline(newX, newY, 2*n);

        for (int i = 0; i < length - 1; i++) {
            final double x1 = newX[i];
            final double x2 = newX[i + 1];
            final double y1 = newY[i];
            final double y2 = newY[i + 1];
            gc.strokeLine(x1, y1, x2, y2);
        }

        gc.restore();
    }

    private static void compactVector(final double[] input, final int stopIndex) {
        if (stopIndex >= 0) {
            System.arraycopy(input, input.length - stopIndex, input, stopIndex, stopIndex);
        }
    }

    // The cache can be shared because there can only ever be one renderer accessing it
    // Note: should not be exposed to child classes to guarantee that arrays aren't double used.
    private static final FastDoubleArrayCache SHARED_ARRAYS = new FastDoubleArrayCache(4);
    private static final CachedDataPoints SHARED_POINTS_CACHE = new CachedDataPoints();

    /**
     * Deletes all arrays that are larger than necessary for the last drawn dataset
     */
    public static void trimCache() {
        SHARED_ARRAYS.trim();
        SHARED_POINTS_CACHE.trim();
    }

}