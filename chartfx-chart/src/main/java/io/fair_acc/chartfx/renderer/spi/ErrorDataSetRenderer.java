package io.fair_acc.chartfx.renderer.spi;

import java.security.InvalidParameterException;
import java.util.*;

import io.fair_acc.chartfx.ui.css.DataSetNode;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.XYChartCss;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.marker.Marker;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.utils.BezierCurve;
import io.fair_acc.chartfx.renderer.spi.utils.DefaultRenderColorScheme;
import io.fair_acc.chartfx.utils.StyleParser;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError.ErrorType;
import io.fair_acc.dataset.spi.utils.Triple;
import io.fair_acc.dataset.utils.DoubleArrayCache;
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
    private Marker marker = DefaultMarker.DEFAULT;

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
     * @param dataSet the data set for which the representative icon should be generated
     * @param canvas the canvas in which the representative icon should be drawn
     * @return true if the renderer generates symbols that should be displayed
     */
    @Override
    public boolean drawLegendSymbol(final DataSetNode dataSet, final Canvas canvas) {
        final int width = (int) canvas.getWidth();
        final int height = (int) canvas.getHeight();
        final GraphicsContext gc = canvas.getGraphicsContext2D();

        final String style = dataSet.getStyle();
        final Integer layoutOffset = StyleParser.getIntegerPropertyValue(style, XYChartCss.DATASET_LAYOUT_OFFSET);
        final Integer dsIndexLocal = StyleParser.getIntegerPropertyValue(style, XYChartCss.DATASET_INDEX);

        final int dsLayoutIndexOffset = layoutOffset == null ? 0 : layoutOffset; // TODO: rationalise

        final int plottingIndex = dsLayoutIndexOffset + (dsIndexLocal == null ? dataSet.getColorIndex() : dsIndexLocal);

        gc.save();

        DefaultRenderColorScheme.setLineScheme(gc, dataSet.getStyle(), plottingIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, dataSet.getStyle());
        DefaultRenderColorScheme.setFillScheme(gc, dataSet.getStyle(), plottingIndex);
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
    public List<DataSet> render(final GraphicsContext gc, final Chart chart, final int unusedOffset,
            final ObservableList<DataSet> unusedDataSets) {
        if (!(chart instanceof XYChart)) {
            throw new InvalidParameterException("must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
        }

        // If there are no data sets
        if (getDatasets().isEmpty()) {
            return Collections.emptyList();
        }

        final Axis xAxis = getFirstHorizontalAxis();
        final Axis yAxis = getFirstVerticalAxis();

        final long start = ProcessingProfiler.getTimeStamp();
        final double xAxisWidth = xAxis.getWidth();
        final boolean xAxisInverted = xAxis.isInvertedAxis();
        final double xMin = xAxis.getValueForDisplay(xAxisInverted ? xAxisWidth : 0.0);
        final double xMax = xAxis.getValueForDisplay(xAxisInverted ? 0.0 : xAxisWidth);

        if (ProcessingProfiler.getDebugState()) {
            ProcessingProfiler.getTimeDiff(start, "init");
        }

        for (int dataSetIndex = getDatasetNodes().size() - 1; dataSetIndex >= 0; dataSetIndex--) {
            DataSetNode dataSet = getDatasetNodes().get(dataSetIndex);
            if (!dataSet.isVisible()) {
                continue;
            }

            // N.B. print out for debugging purposes, please keep (used for
            // detecting redundant or too frequent render updates)
            // System.err.println(String.format("render for range [%f,%f] and dataset = '%s'", xMin, xMax, dataSet.getName()));

            var timestamp = ProcessingProfiler.getTimeStamp();
            final var data = dataSet.getDataSet();
            int indexMin;
            int indexMax; /* indexMax is excluded in the drawing */
            if (isAssumeSortedData()) {
                indexMin = Math.max(0, data.getIndex(DataSet.DIM_X, xMin) - 1);
                indexMax = Math.min(data.getIndex(DataSet.DIM_X, xMax) + 2, data.getDataCount());
            } else {
                indexMin = 0;
                indexMax = data.getDataCount();
            }

            // zero length/range data set -> nothing to be drawn
            if (indexMax - indexMin <= 0) {
                continue;
            }

            if (ProcessingProfiler.getDebugState()) {
                timestamp = ProcessingProfiler.getTimeDiff(timestamp,
                        "get min/max" + String.format(" from:%d to:%d", indexMin, indexMax));
            }

            final CachedDataPoints points = STATIC_POINTS_CACHE.resizeMin(indexMin, indexMax, data.getDataCount(), true);
            if (ProcessingProfiler.getDebugState()) {
                timestamp = ProcessingProfiler.getTimeDiff(timestamp, "get CachedPoints");
            }

            // compute local screen coordinates
            final boolean isPolarPlot = ((XYChart) chart).isPolarPlot();
            if (isParallelImplementation()) {
                points.computeScreenCoordinatesInParallel(xAxis, yAxis, data,
                        dataSet.getColorIndex(), indexMin, indexMax, getErrorType(), isPolarPlot,
                        isallowNaNs());
            } else {
                points.computeScreenCoordinates(xAxis, yAxis, data, dataSet.getColorIndex(),
                        indexMin, indexMax, getErrorType(), isPolarPlot, isallowNaNs());
            }
            if (ProcessingProfiler.getDebugState()) {
                timestamp = ProcessingProfiler.getTimeDiff(timestamp, "computeScreenCoordinates()");
            }

            // invoke data reduction algorithm
            points.reduce(rendererDataReducerProperty().get(), isReducePoints(),
                    getMinRequiredReductionSize());

            // draw individual plot components
            drawChartCompontents(gc, points);
            if (ProcessingProfiler.getDebugState()) {
                timestamp = ProcessingProfiler.getTimeDiff(timestamp, "drawChartComponents()");
            }

        } // end of 'dataSetIndex' loop
        ProcessingProfiler.getTimeDiff(start);

        return getDatasets();
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
     * @param localCachedPoints reference to local cached data point object
     */
    protected void drawBars(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        if (!isDrawBars()) {
            return;
        }

        final int xOffset = Math.max(localCachedPoints.dataSetIndex, 0);
        final int minRequiredWidth = Math.max(getDashSize(), localCachedPoints.minDistanceX);

        final double barWPercentage = getBarWidthPercentage();
        final double dynBarWidth = minRequiredWidth * barWPercentage / 100;
        final double constBarWidth = getBarWidth();
        final double localBarWidth = isDynamicBarWidth() ? dynBarWidth : constBarWidth;
        final double barWidthHalf = localBarWidth / 2 - (isShiftBar() ? xOffset * getShiftBarOffset() : 0);

        gc.save();
        DefaultRenderColorScheme.setMarkerScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);

        if (localCachedPoints.polarPlot) {
            for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
                if (localCachedPoints.styles[i] == null) {
                    gc.strokeLine(localCachedPoints.xZero, localCachedPoints.yZero, localCachedPoints.xValues[i],
                            localCachedPoints.yValues[i]);
                } else {
                    // work-around: bar colour controlled by the marker color
                    gc.save();
                    gc.setFill(StyleParser.getColorPropertyValue(localCachedPoints.styles[i], XYChartCss.FILL_COLOR));
                    gc.setLineWidth(barWidthHalf);
                    gc.strokeLine(localCachedPoints.xZero, localCachedPoints.yZero, localCachedPoints.xValues[i],
                            localCachedPoints.yValues[i]);
                    gc.restore();
                }
            }
        } else {
            for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
                double yDiff = localCachedPoints.yValues[i] - localCachedPoints.yZero;
                final double yMin;
                if (yDiff > 0) {
                    yMin = localCachedPoints.yZero;
                } else {
                    yMin = localCachedPoints.yValues[i];
                    yDiff = Math.abs(yDiff);
                }

                if (localCachedPoints.styles[i] == null) {
                    gc.fillRect(localCachedPoints.xValues[i] - barWidthHalf, yMin, localBarWidth, yDiff);

                } else {
                    gc.save();
                    gc.setFill(StyleParser.getColorPropertyValue(localCachedPoints.styles[i], XYChartCss.FILL_COLOR));
                    gc.fillRect(localCachedPoints.xValues[i] - barWidthHalf, yMin, localBarWidth, yDiff);
                    gc.restore();
                }
            }
        }

        gc.restore();
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param localCachedPoints reference to local cached data point object
     */
    protected void drawBubbles(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        if (!isDrawBubbles()) {
            return;
        }
        gc.save();
        DefaultRenderColorScheme.setMarkerScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);

        // N.B. bubbles are drawn with the same colour as polyline (ie. not the fillColor)
        final Color fillColor = StyleParser.getColorPropertyValue(localCachedPoints.defaultStyle,
                XYChartCss.STROKE_COLOR);
        if (fillColor != null) {
            gc.setFill(fillColor);
        }

        final double minSize = getMarkerSize();
        if (localCachedPoints.errorType[DataSet.DIM_X] != ErrorType.NO_ERROR && localCachedPoints.errorType[DataSet.DIM_Y] == ErrorType.NO_ERROR) {
            // X, X_ASYMMETRIC
            for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
                final double radius = Math.max(minSize, localCachedPoints.errorXPos[i] - localCachedPoints.errorXNeg[i]);
                final double x = localCachedPoints.xValues[i] - radius;
                final double y = localCachedPoints.yValues[i] - radius;

                gc.fillOval(x, y, 2 * radius, 2 * radius);
            }
        } else if (localCachedPoints.errorType[DataSet.DIM_X] == ErrorType.NO_ERROR && localCachedPoints.errorType[DataSet.DIM_Y] != ErrorType.NO_ERROR) {
            // Y, Y_ASYMMETRIC
            for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
                final double radius = Math.max(minSize, localCachedPoints.errorYNeg[i] - localCachedPoints.errorYPos[i]);
                final double x = localCachedPoints.xValues[i] - radius;
                final double y = localCachedPoints.yValues[i] - radius;

                gc.fillOval(x, y, 2 * radius, 2 * radius);
            }
        } else if (localCachedPoints.errorType[DataSet.DIM_X] != ErrorType.NO_ERROR && localCachedPoints.errorType[DataSet.DIM_Y] != ErrorType.NO_ERROR) {
            // XY, XY_ASYMMETRIC
            for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
                final double width = Math.max(minSize, localCachedPoints.errorXPos[i] - localCachedPoints.errorXNeg[i]);
                final double height = Math.max(minSize, localCachedPoints.errorYNeg[i] - localCachedPoints.errorYPos[i]);
                final double x = localCachedPoints.xValues[i] - width;
                final double y = localCachedPoints.yValues[i] - height;

                gc.fillOval(x, y, 2 * width, 2 * height);
            }
        } else { // NO ERROR
            for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
                final double x = localCachedPoints.xValues[i] - minSize;
                final double y = localCachedPoints.yValues[i] - minSize;

                gc.fillOval(x, y, 2 * minSize, 2 * minSize);
            }
        }

        gc.restore();
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param localCachedPoints reference to local cached data point object
     */
    protected void drawDefaultNoErrors(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        drawBars(gc, localCachedPoints);
        drawPolyLine(gc, localCachedPoints);
        drawMarker(gc, localCachedPoints);
        drawBubbles(gc, localCachedPoints);
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param lCacheP reference to local cached data point object
     */
    protected void drawErrorBars(final GraphicsContext gc, final CachedDataPoints lCacheP) {
        final long start = ProcessingProfiler.getTimeStamp();

        drawBars(gc, lCacheP);

        final int dashHalf = getDashSize() / 2;
        gc.save();
        DefaultRenderColorScheme.setFillScheme(gc, lCacheP.defaultStyle, lCacheP.dataSetIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, lCacheP.defaultStyle);

        for (int i = 0; i < lCacheP.actualDataCount; i++) {
            if (lCacheP.errorType[DataSet.DIM_X] != ErrorType.NO_ERROR
                    && lCacheP.errorType[DataSet.DIM_Y] != ErrorType.NO_ERROR) {
                // draw error bars
                gc.strokeLine(lCacheP.xValues[i], lCacheP.errorYNeg[i], lCacheP.xValues[i], lCacheP.errorYPos[i]);
                gc.strokeLine(lCacheP.errorXNeg[i], lCacheP.yValues[i], lCacheP.errorXPos[i], lCacheP.yValues[i]);

                // draw horizontal dashes
                gc.strokeLine(lCacheP.xValues[i] - dashHalf, lCacheP.errorYNeg[i], lCacheP.xValues[i] + dashHalf,
                        lCacheP.errorYNeg[i]);
                gc.strokeLine(lCacheP.xValues[i] - dashHalf, lCacheP.errorYPos[i], lCacheP.xValues[i] + dashHalf,
                        lCacheP.errorYPos[i]);

                // draw vertical dashes
                gc.strokeLine(lCacheP.errorXNeg[i], lCacheP.yValues[i] - dashHalf, lCacheP.errorXNeg[i],
                        lCacheP.yValues[i] + dashHalf);
                gc.strokeLine(lCacheP.errorXPos[i], lCacheP.yValues[i] - dashHalf, lCacheP.errorXPos[i],
                        lCacheP.yValues[i] + dashHalf);
            } else if (lCacheP.errorType[DataSet.DIM_X] == ErrorType.NO_ERROR
                       && lCacheP.errorType[DataSet.DIM_Y] != ErrorType.NO_ERROR) {
                // draw error bars
                gc.strokeLine(lCacheP.xValues[i], lCacheP.errorYNeg[i], lCacheP.xValues[i], lCacheP.errorYPos[i]);

                // draw horizontal dashes
                gc.strokeLine(lCacheP.xValues[i] - dashHalf, lCacheP.errorYNeg[i], lCacheP.xValues[i] + dashHalf,
                        lCacheP.errorYNeg[i]);
                gc.strokeLine(lCacheP.xValues[i] - dashHalf, lCacheP.errorYPos[i], lCacheP.xValues[i] + dashHalf,
                        lCacheP.errorYPos[i]);
            } else if (lCacheP.errorType[DataSet.DIM_X] != ErrorType.NO_ERROR
                       && lCacheP.errorType[DataSet.DIM_Y] == ErrorType.NO_ERROR) {
                // draw error bars
                gc.strokeLine(lCacheP.errorXNeg[i], lCacheP.yValues[i], lCacheP.errorXPos[i], lCacheP.yValues[i]);

                // draw horizontal dashes
                gc.strokeLine(lCacheP.xValues[i] - dashHalf, lCacheP.errorYNeg[i], lCacheP.xValues[i] + dashHalf,
                        lCacheP.errorYNeg[i]);
                gc.strokeLine(lCacheP.xValues[i] - dashHalf, lCacheP.errorYPos[i], lCacheP.xValues[i] + dashHalf,
                        lCacheP.errorYPos[i]);
            }
        }
        gc.restore();

        drawPolyLine(gc, lCacheP);
        drawMarker(gc, lCacheP);
        drawBubbles(gc, lCacheP);

        ProcessingProfiler.getTimeDiff(start);
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param localCachedPoints reference to local cached data point object
     */
    protected void drawErrorSurface(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final long start = ProcessingProfiler.getTimeStamp();

        DefaultRenderColorScheme.setFillScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);

        final int nDataCount = localCachedPoints.actualDataCount;
        final int nPolygoneEdges = 2 * nDataCount;
        final double[] xValuesSurface = DoubleArrayCache.getInstance().getArray(nPolygoneEdges);
        final double[] yValuesSurface = DoubleArrayCache.getInstance().getArray(nPolygoneEdges);

        final int xend = nPolygoneEdges - 1;
        for (int i = 0; i < nDataCount; i++) {
            xValuesSurface[i] = localCachedPoints.xValues[i];
            yValuesSurface[i] = localCachedPoints.errorYNeg[i];
            xValuesSurface[xend - i] = localCachedPoints.xValues[i];
            yValuesSurface[xend - i] = localCachedPoints.errorYPos[i];
        }

        gc.setFillRule(FillRule.EVEN_ODD);
        gc.fillPolygon(xValuesSurface, yValuesSurface, nPolygoneEdges);

        drawPolyLine(gc, localCachedPoints);
        drawBars(gc, localCachedPoints);
        drawMarker(gc, localCachedPoints);
        drawBubbles(gc, localCachedPoints);

        DoubleArrayCache.getInstance().add(xValuesSurface);
        DoubleArrayCache.getInstance().add(yValuesSurface);

        ProcessingProfiler.getTimeDiff(start);
    }

    /**
     * NaN compatible algorithm
     *
     * @param gc the graphics context from the Canvas parent
     * @param localCachedPoints reference to local cached data point object
     */
    protected void drawErrorSurfaceNaNCompatible(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final long start = ProcessingProfiler.getTimeStamp();

        DefaultRenderColorScheme.setFillScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);

        gc.setFillRule(FillRule.EVEN_ODD);

        final int nDataCount = localCachedPoints.actualDataCount;
        final int nPolygoneEdges = 2 * nDataCount;
        final double[] xValuesSurface = DoubleArrayCache.getInstance().getArray(nPolygoneEdges);
        final double[] yValuesSurface = DoubleArrayCache.getInstance().getArray(nPolygoneEdges);

        final int xend = nPolygoneEdges - 1;
        int count = 0;
        for (int i = 0; i < nDataCount; i++) {
            final double x = localCachedPoints.xValues[i];
            final double yen = localCachedPoints.errorYNeg[i];
            final double yep = localCachedPoints.errorYPos[i];

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

        drawPolyLine(gc, localCachedPoints);
        drawBars(gc, localCachedPoints);
        drawMarker(gc, localCachedPoints);
        drawBubbles(gc, localCachedPoints);

        DoubleArrayCache.getInstance().add(xValuesSurface);
        DoubleArrayCache.getInstance().add(yValuesSurface);

        ProcessingProfiler.getTimeDiff(start);
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param localCachedPoints reference to local cached data point object
     */
    protected void drawMarker(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        if (!isDrawMarker()) {
            return;
        }
        gc.save();
        DefaultRenderColorScheme.setMarkerScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);

        final Triple<Marker, Color, Double> markerTypeColorAndSize = getDefaultMarker(localCachedPoints.defaultStyle);
        final Marker defaultMarker = markerTypeColorAndSize.getFirst();
        final Color defaultMarkerColor = markerTypeColorAndSize.getSecond();
        final double defaultMarkerSize = markerTypeColorAndSize.getThird();
        if (defaultMarkerColor != null) {
            gc.setFill(defaultMarkerColor);
            gc.setStroke(defaultMarkerColor);
        }
        for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
            final double x = localCachedPoints.xValues[i];
            final double y = localCachedPoints.yValues[i];
            if (localCachedPoints.styles[i] == null) {
                defaultMarker.draw(gc, x, y, defaultMarkerSize);
            } else {
                final Triple<Marker, Color, Double> markerForPoint = getDefaultMarker(
                        localCachedPoints.defaultStyle + localCachedPoints.styles[i]);
                gc.save();
                if (markerForPoint.getSecond() != null) {
                    gc.setFill(markerForPoint.getSecond());
                }
                final Marker pointMarker = markerForPoint.getFirst() == null ? defaultMarker
                                                                             : markerForPoint.getFirst();
                pointMarker.draw(gc, x, y, markerForPoint.getThird());
                gc.restore();
            }
        }

        gc.restore();
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param localCachedPoints reference to local cached data point object
     */
    protected void drawPolyLine(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        switch (getPolyLineStyle()) {
        case NONE:
            return;
        case AREA:
            drawPolyLineArea(gc, localCachedPoints);
            break;
        case ZERO_ORDER_HOLDER:
        case STAIR_CASE:
            drawPolyLineStairCase(gc, localCachedPoints);
            break;
        case HISTOGRAM:
            drawPolyLineHistogram(gc, localCachedPoints);
            break;
        case HISTOGRAM_FILLED:
            drawPolyLineHistogramFilled(gc, localCachedPoints);
            break;
        case BEZIER_CURVE:
            drawPolyLineHistogramBezier(gc, localCachedPoints);
            break;
        case NORMAL:
        default:
            drawPolyLineLine(gc, localCachedPoints);
            break;
        }
    }

    protected Triple<Marker, Color, Double> getDefaultMarker(final String dataSetStyle) {
        Marker defaultMarker = getMarker();
        // N.B. the markers are drawn in the same colour
        // as the polyline (ie. stroke color)
        Color defaultMarkerColor = StyleParser.getColorPropertyValue(dataSetStyle, XYChartCss.STROKE_COLOR);
        double defaultMarkerSize = getMarkerSize();

        if (dataSetStyle == null) {
            return new Triple<>(defaultMarker, defaultMarkerColor, defaultMarkerSize);
        }

        // parse style:
        final Map<String, String> map = StyleParser.splitIntoMap(dataSetStyle);

        final String markerType = map.get(XYChartCss.MARKER_TYPE.toLowerCase(Locale.UK));
        if (markerType != null) {
            try {
                defaultMarker = DefaultMarker.get(markerType);
            } catch (final IllegalArgumentException ex) {
                LOGGER.error("could not parse marker type description for '" + XYChartCss.MARKER_TYPE + "'='" + markerType + "'", ex);
            }
        }
        final String markerSize = map.get(XYChartCss.MARKER_SIZE.toLowerCase(Locale.UK));
        if (markerSize != null) {
            try {
                defaultMarkerSize = Double.parseDouble(markerSize);
            } catch (final NumberFormatException ex) {
                LOGGER.error("could not parse marker size description for '" + XYChartCss.MARKER_SIZE + "'='" + markerSize + "'", ex);
            }
        }

        final String markerColor = map.get(XYChartCss.MARKER_COLOR.toLowerCase(Locale.UK));
        if (markerColor != null) {
            try {
                defaultMarkerColor = Color.web(markerColor);
            } catch (final IllegalArgumentException ex) {
                LOGGER.error("could not parse marker color description for '" + XYChartCss.MARKER_COLOR + "'='" + markerColor + "'", ex);
            }
        }

        return new Triple<>(defaultMarker, defaultMarkerColor, defaultMarkerSize);
    }

    /**
     * @return the instance of this ErrorDataSetRenderer.
     */
    @Override
    protected ErrorDataSetRenderer getThis() {
        return this;
    }

    private void drawChartCompontents(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final long start = ProcessingProfiler.getTimeStamp();
        switch (getErrorType()) {
        case ERRORBARS:
            drawErrorBars(gc, localCachedPoints);
            break;
        case ERRORSURFACE:
            if (isallowNaNs()) {
                drawErrorSurfaceNaNCompatible(gc, localCachedPoints);
            } else {
                drawErrorSurface(gc, localCachedPoints);
            }
            break;
        case ERRORCOMBO:
            if (localCachedPoints.getMinXDistance() >= getDashSize() * 2) {
                drawErrorBars(gc, localCachedPoints);
            } else {
                if (isallowNaNs()) {
                    drawErrorSurfaceNaNCompatible(gc, localCachedPoints);
                } else {
                    drawErrorSurface(gc, localCachedPoints);
                }
            }
            break;
        case NONE:
        default:
            drawDefaultNoErrors(gc, localCachedPoints);
            break;
        }
        ProcessingProfiler.getTimeDiff(start);
    }

    protected static void drawPolyLineArea(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final int n = localCachedPoints.actualDataCount;
        if (n == 0) {
            return;
        }

        // need to allocate new array :-(
        final int length = n + 2;
        final double[] newX = DoubleArrayCache.getInstance().getArray(length);
        final double[] newY = DoubleArrayCache.getInstance().getArray(length);

        final double zero = localCachedPoints.yZero;
        System.arraycopy(localCachedPoints.xValues, 0, newX, 0, n);
        System.arraycopy(localCachedPoints.yValues, 0, newY, 0, n);
        newX[n] = localCachedPoints.xValues[n - 1];
        newY[n] = zero;
        newX[n + 1] = localCachedPoints.xValues[0];
        newY[n + 1] = zero;

        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);
        // use stroke as fill colour
        gc.setFill(gc.getStroke());
        gc.fillPolygon(newX, newY, length);
        gc.restore();

        // release arrays to cache
        DoubleArrayCache.getInstance().add(newX);
        DoubleArrayCache.getInstance().add(newY);
    }

    protected static void drawPolyLineHistogram(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final int n = localCachedPoints.actualDataCount;
        if (n == 0) {
            return;
        }

        // need to allocate new array :-(
        final int length = 2 * (n + 1);
        final double[] newX = DoubleArrayCache.getInstance().getArray(length);
        final double[] newY = DoubleArrayCache.getInstance().getArray(length);

        final double xRange = localCachedPoints.xMax - localCachedPoints.xMin;
        double diffLeft;
        double diffRight = n > 0 ? 0.5 * (localCachedPoints.xValues[1] - localCachedPoints.xValues[0]) : 0.5 * xRange;
        newX[0] = localCachedPoints.xValues[0] - diffRight;
        newY[0] = localCachedPoints.yZero;
        for (int i = 0; i < n; i++) {
            diffLeft = localCachedPoints.xValues[i] - newX[2 * i];
            diffRight = i + 1 < n ? 0.5 * (localCachedPoints.xValues[i + 1] - localCachedPoints.xValues[i]) : diffLeft;
            if (i == 0) {
                diffLeft = diffRight;
            }

            newX[2 * i + 1] = localCachedPoints.xValues[i] - diffLeft;
            newY[2 * i + 1] = localCachedPoints.yValues[i];
            newX[2 * i + 2] = localCachedPoints.xValues[i] + diffRight;
            newY[2 * i + 2] = localCachedPoints.yValues[i];
        }
        // last point
        newX[length - 1] = localCachedPoints.xValues[n - 1] + diffRight;
        newY[length - 1] = localCachedPoints.yZero;

        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);

        for (int i = 0; i < length - 1; i++) {
            final double x1 = newX[i];
            final double x2 = newX[i + 1];
            final double y1 = newY[i];
            final double y2 = newY[i + 1];
            gc.strokeLine(x1, y1, x2, y2);
        }

        gc.restore();

        // release arrays to cache
        DoubleArrayCache.getInstance().add(newX);
        DoubleArrayCache.getInstance().add(newY);
    }

    protected static void drawPolyLineHistogramBezier(final GraphicsContext gc,
            final CachedDataPoints localCachedPoints) {
        final int n = localCachedPoints.actualDataCount;
        if (n < 2) {
            drawPolyLineLine(gc, localCachedPoints);
            return;
        }

        // need to allocate new array :-(
        final double[] xCp1 = DoubleArrayCache.getInstance().getArray(n);
        final double[] yCp1 = DoubleArrayCache.getInstance().getArray(n);
        final double[] xCp2 = DoubleArrayCache.getInstance().getArray(n);
        final double[] yCp2 = DoubleArrayCache.getInstance().getArray(n);

        BezierCurve.calcCurveControlPoints(localCachedPoints.xValues, localCachedPoints.yValues, xCp1, yCp1, xCp2, yCp2,
                localCachedPoints.actualDataCount);

        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);
        // use stroke as fill colour
        gc.setFill(gc.getStroke());
        gc.beginPath();
        for (int i = 0; i < n - 1; i++) {
            final double x0 = localCachedPoints.xValues[i];
            final double x1 = localCachedPoints.xValues[i + 1];
            final double y0 = localCachedPoints.yValues[i];
            final double y1 = localCachedPoints.yValues[i + 1];

            // coordinates of first Bezier control point.
            final double xc0 = xCp1[i];
            final double yc0 = yCp1[i];
            // coordinates of the second Bezier control point.
            final double xc1 = xCp2[i];
            final double yc1 = yCp2[i];

            gc.moveTo(x0, y0);
            gc.bezierCurveTo(xc0, yc0, xc1, yc1, x1, y1);
        }
        gc.moveTo(localCachedPoints.xValues[n - 1], localCachedPoints.yValues[n - 1]);
        gc.closePath();
        gc.stroke();
        gc.restore();

        // release arrays to Cache
        DoubleArrayCache.getInstance().add(xCp1);
        DoubleArrayCache.getInstance().add(yCp1);
        DoubleArrayCache.getInstance().add(xCp2);
        DoubleArrayCache.getInstance().add(yCp2);
    }

    protected static void drawPolyLineHistogramFilled(final GraphicsContext gc,
            final CachedDataPoints localCachedPoints) {
        final int n = localCachedPoints.actualDataCount;
        if (n == 0) {
            return;
        }

        // need to allocate new array :-(
        final int length = 2 * (n + 1);
        final double[] newX = DoubleArrayCache.getInstance().getArray(length);
        final double[] newY = DoubleArrayCache.getInstance().getArray(length);

        final double xRange = localCachedPoints.xMax - localCachedPoints.xMin;
        double diffLeft;
        double diffRight = n > 0 ? 0.5 * (localCachedPoints.xValues[1] - localCachedPoints.xValues[0]) : 0.5 * xRange;
        newX[0] = localCachedPoints.xValues[0] - diffRight;
        newY[0] = localCachedPoints.yZero;
        for (int i = 0; i < n; i++) {
            diffLeft = localCachedPoints.xValues[i] - newX[2 * i];
            diffRight = i + 1 < n ? 0.5 * (localCachedPoints.xValues[i + 1] - localCachedPoints.xValues[i]) : diffLeft;
            if (i == 0) {
                diffLeft = diffRight;
            }

            newX[2 * i + 1] = localCachedPoints.xValues[i] - diffLeft;
            newY[2 * i + 1] = localCachedPoints.yValues[i];
            newX[2 * i + 2] = localCachedPoints.xValues[i] + diffRight;
            newY[2 * i + 2] = localCachedPoints.yValues[i];
        }
        // last point
        newX[length - 1] = localCachedPoints.xValues[n - 1] + diffRight;
        newY[length - 1] = localCachedPoints.yZero;

        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);
        // use stroke as fill colour
        gc.setFill(gc.getStroke());
        gc.fillPolygon(newX, newY, length);
        gc.restore();

        // release arrays to cache
        DoubleArrayCache.getInstance().add(newX);
        DoubleArrayCache.getInstance().add(newY);
    }

    protected static void drawPolyLineLine(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle, localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);

        if (localCachedPoints.allowForNaNs) {
            gc.beginPath();
            gc.moveTo(localCachedPoints.xValues[0], localCachedPoints.yValues[0]);
            boolean lastIsFinite = true;
            double xLastValid = 0.0;
            double yLastValid = 0.0;
            for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
                final double x0 = localCachedPoints.xValues[i];
                final double y0 = localCachedPoints.yValues[i];
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
                gc.strokePolyline(localCachedPoints.xValues, localCachedPoints.yValues, localCachedPoints.actualDataCount);
            } else {
                for (int i = 0; i < localCachedPoints.actualDataCount - 1; i++) {
                    final double x1 = localCachedPoints.xValues[i];
                    final double x2 = localCachedPoints.xValues[i + 1];
                    final double y1 = localCachedPoints.yValues[i];
                    final double y2 = localCachedPoints.yValues[i + 1];

                    gc.strokeLine(x1, y1, x2, y2);
                }
            }
        }

        gc.restore();
    }

    protected static void drawPolyLineStairCase(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final int n = localCachedPoints.actualDataCount;
        if (n == 0) {
            return;
        }

        // need to allocate new array :-(
        final int length = 2 * n;
        final double[] newX = DoubleArrayCache.getInstance().getArray(length);
        final double[] newY = DoubleArrayCache.getInstance().getArray(length);

        for (int i = 0; i < n - 1; i++) {
            newX[2 * i] = localCachedPoints.xValues[i];
            newY[2 * i] = localCachedPoints.yValues[i];
            newX[2 * i + 1] = localCachedPoints.xValues[i + 1];
            newY[2 * i + 1] = localCachedPoints.yValues[i];
        }
        // last point
        newX[length - 2] = localCachedPoints.xValues[n - 1];
        newY[length - 2] = localCachedPoints.yValues[n - 1];
        newX[length - 1] = localCachedPoints.xMax;
        newY[length - 1] = localCachedPoints.yValues[n - 1];

        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle, localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);
        // gc.strokePolyline(newX, newY, 2*n);

        for (int i = 0; i < length - 1; i++) {
            final double x1 = newX[i];
            final double x2 = newX[i + 1];
            final double y1 = newY[i];
            final double y2 = newY[i + 1];
            gc.strokeLine(x1, y1, x2, y2);
        }

        gc.restore();

        // release arrays to cache
        DoubleArrayCache.getInstance().add(newX);
        DoubleArrayCache.getInstance().add(newY);
    }

    private static void compactVector(final double[] input, final int stopIndex) {
        if (stopIndex >= 0) {
            System.arraycopy(input, input.length - stopIndex, input, stopIndex, stopIndex);
        }
    }

    // The points cache is thread-safe from the JavaFX thread and can be shared across all instances
    private static final CachedDataPoints STATIC_POINTS_CACHE = new CachedDataPoints();

    /**
     * Deletes all arrays that are larger than necessary for the last drawn dataset
     */
    public static void trimPointsCache() {
        STATIC_POINTS_CACHE.trim();
    }

}
