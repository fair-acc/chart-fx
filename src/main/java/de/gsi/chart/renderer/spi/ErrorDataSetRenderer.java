package de.gsi.chart.renderer.spi;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.XYChartCss;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.chart.data.DataSet;
import de.gsi.chart.data.DataSet3D;
import de.gsi.chart.data.DataSetError;
import de.gsi.chart.data.DataSetError.ErrorType;
import de.gsi.chart.marker.Marker;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.RendererDataReducer;
import de.gsi.chart.renderer.spi.utils.BezierCurve;
import de.gsi.chart.renderer.spi.utils.Cache;
import de.gsi.chart.renderer.spi.utils.DefaultRenderColorScheme;
import de.gsi.chart.utils.ProcessingProfiler;
import de.gsi.chart.utils.StyleParser;
import de.gsi.math.ArrayUtils;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;

/**
 * Renders data points with error bars. It can be used e.g. to render horizontal and/or vertical errors
 *
 * @author R.J. Steinhagen
 */
public class ErrorDataSetRenderer extends AbstractErrorDataSetRendererParameter<ErrorDataSetRenderer>
        implements Renderer {

    private static final double DEG_TO_RAD = Math.PI / 180.0;
    private static final int MAX_THREADS = Math.max(4, Runtime.getRuntime().availableProcessors());
    // private static final ExecutorService executorService =
    // Executors.newCachedThreadPool();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(2 * MAX_THREADS);

    protected Cache cache = new Cache();
    private Marker marker; // TODO: generate marker
    private boolean isPolarPlot = false;

    /**
     * Creates new <code>ErrorDataSetRenderer</code>.
     */
    public ErrorDataSetRenderer() {
        this(3);
    }

    /**
     * Creates new <code>ErrorDataSetRenderer</code>.
     *
     * @param dashSize the initial size (top/bottom cap) of the dash on top of the error bars
     */
    public ErrorDataSetRenderer(final int dashSize) {
        super();
        setDashSize(dashSize);
    }

    @Override
    public void render(final GraphicsContext gc, final Chart chart, final int dataSetOffset,
            final ObservableList<DataSet> datasets) {
        final long start = ProcessingProfiler.getTimeStamp();
        if (!(chart instanceof XYChart)) {
            throw new InvalidParameterException(
                    "must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
        }
        final XYChart xyChart = (XYChart) chart;
        isPolarPlot = xyChart.isPolarPlot();

        // make local copy and add renderer specific data sets
        final List<DataSet> localDataSetList = new ArrayList<>(datasets);
        localDataSetList.addAll(super.getDatasets());

        // If there are no data sets
        if (localDataSetList.isEmpty()) {
            return;
        }

        if (getFirstAxis(Orientation.HORIZONTAL) == null) {
            throw new InvalidParameterException("x-Axis must not be null - axesList() = " + getAxes());
        }

        if (getFirstAxis(Orientation.VERTICAL) == null) {
            throw new InvalidParameterException("y-Axis must not be null - axesList() = " + getAxes());
        }

        if (!(getFirstAxis(Orientation.HORIZONTAL) instanceof Axis)) {
            throw new InvalidParameterException("x-Axis must be a derivative of Axis, axis is = "
                    + getFirstAxis(Orientation.HORIZONTAL).getClass().getSimpleName());
        }

        if (!(getFirstAxis(Orientation.VERTICAL) instanceof Axis)) {
            throw new InvalidParameterException("y-Axis must be a derivative of Axis, axis is = "
                    + getFirstAxis(Orientation.VERTICAL).getClass().getSimpleName());
        }

        final Axis xAxis = getFirstAxis(Orientation.HORIZONTAL);
        final double xAxisWidth = xAxis.getWidth();
        final double xMin = xAxis.getValueForDisplay(0);
        final double xMax = xAxis.getValueForDisplay(xAxisWidth);

        ProcessingProfiler.getTimeDiff(start, "init");

        for (int dataSetIndex = localDataSetList.size() - 1; dataSetIndex >= 0; dataSetIndex--) {
            long stop = ProcessingProfiler.getTimeStamp();
            final DataSet dataSet = localDataSetList.get(dataSetIndex);
            if (dataSet instanceof DataSet3D) {
                // this renderer cannot use 3D data sets directly, use
                // MountainRangeRenderer instead
                // continue;
            }

            // N.B. print out for debugging purposes, please keep (used for
            // detecting redundant or too frequent render updates)
            // System.err.println(
            // String.format("render for range [%f,%f] and dataset = '%s'",
            // xMin, xMax, dataSet.getName()));

            dataSet.lock();
            stop = ProcessingProfiler.getTimeDiff(stop, "dataSet.lock()");

            // update categories in case of category axes for the first (index
            // == '0') indexed data set
            if (dataSetIndex == 0) {
                if (getFirstAxis(Orientation.HORIZONTAL) instanceof CategoryAxis) {
                    final CategoryAxis axis = (CategoryAxis) getFirstAxis(Orientation.HORIZONTAL);
                    axis.updateCategories(dataSet);
                }

                if (getFirstAxis(Orientation.VERTICAL) instanceof CategoryAxis) {
                    final CategoryAxis axis = (CategoryAxis) getFirstAxis(Orientation.VERTICAL);
                    axis.updateCategories(dataSet);
                }
            }

            // check for potentially reduced data range we are supposed to plot

            int indexMin = Math.max(0, dataSet.getXIndex(xMin));
            /* indexMax is excluded in the drawing */
            int indexMax = Math.min(dataSet.getXIndex(xMax) + 1, dataSet.getDataCount());
            if (xAxis.isInvertedAxis()) {
                final int temp = indexMin;
                indexMin = indexMax - 1;
                indexMax = temp + 1;
            }

            if (indexMax - indexMin <= 0) {
                // zero length/range data set -> nothing to be drawn
                dataSet.unlock();
                continue;
            }

            stop = ProcessingProfiler.getTimeDiff(stop,
                    "get min/max" + String.format(" from:%d to:%d", indexMin, indexMax));

            final CachedDataPoints localCachedPoints = new CachedDataPoints(indexMin, indexMax, dataSet.getDataCount(),
                    true);
            stop = ProcessingProfiler.getTimeDiff(stop, "get CachedPoints");

            // compute local screen coordinates
            localCachedPoints.computeScreenCoordinates(chart, dataSet, dataSetOffset + dataSetIndex, indexMin,
                    indexMax);
            stop = ProcessingProfiler.getTimeDiff(stop, "computeScreenCoordinates()");
            dataSet.unlock();
            stop = ProcessingProfiler.getTimeDiff(stop, "dataSet.unlock()");

            // invoke data reduction algorithm
            localCachedPoints.reduce();

            synchronized (gc) {
                // draw individual plot components
                drawChartCompontents(gc, localCachedPoints);
            }
            stop = ProcessingProfiler.getTimeStamp();

            localCachedPoints.release();
            ProcessingProfiler.getTimeDiff(stop, "localCachedPoints.release()");
        } // end of 'dataSetIndex' loop
        ProcessingProfiler.getTimeDiff(start);
    }

    private void drawChartCompontents(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final long start = ProcessingProfiler.getTimeStamp();
        switch (getErrorType()) {
        case ERRORBARS:
            drawErrorBars(gc, localCachedPoints);
            break;
        case ERRORSURFACE:
            drawErrorSurface(gc, localCachedPoints);
            break;
        case ERRORCOMBO:
            if (localCachedPoints.getMinXDistance() >= getDashSize() * 2) {
                drawErrorBars(gc, localCachedPoints);
            } else {
                drawErrorSurface(gc, localCachedPoints);
            }
            break;
        case NONE:
        default:
            drawDefaultNoErrors(gc, localCachedPoints);
            break;
        }
        ProcessingProfiler.getTimeDiff(start);
    }

    /**
     * @param dataSet the data set for which the representative icon should be generated
     * @param dsIndex index within renderer set
     * @param width requested width of the returning Canvas
     * @param height requested height of the returning Canvas
     * @return a graphical icon representation of the given data sets
     */
    @Override
    public Canvas drawLegendSymbol(final DataSet dataSet, final int dsIndex, final int width, final int height) {
        final Canvas canvas = new Canvas(width, height);
        final GraphicsContext gc = canvas.getGraphicsContext2D();

        final String style = dataSet.getStyle();
        final Integer layoutOffset = StyleParser.getIntegerPropertyValue(style, XYChartCss.DATASET_LAYOUT_OFFSET);
        final Integer dsIndexLocal = StyleParser.getIntegerPropertyValue(style, XYChartCss.DATASET_INDEX);

        final int dsLayoutIndexOffset = layoutOffset == null ? 0 : layoutOffset.intValue(); // TODO:
                                                                                            // rationalise

        final int plotingIndex = dsLayoutIndexOffset + (dsIndexLocal == null ? dsIndex : dsIndexLocal.intValue());

        gc.save();

        DefaultRenderColorScheme.setLineScheme(gc, dataSet.getStyle(), plotingIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, dataSet.getStyle());
        DefaultRenderColorScheme.setFillScheme(gc, dataSet.getStyle(), plotingIndex);
        // DefaultRenderColorScheme.setLineScheme(gc, dataSet.getStyle(),
        // plotingIndex);
        // DefaultRenderColorScheme.setLineScheme(gc, dataSet.getStyle(),
        // plotingIndex);
        // DefaultRenderColorScheme.setLineScheme(gc, dataSet.getStyle(),
        // plotingIndex);
        if (getErrorType() == ErrorStyle.ERRORBARS) {
            final double x = width / 2.0;
            final double y = height / 2.0;
            if (getDashSize() > 2) {
                gc.strokeLine(x - 1.0, 1, x + 1.0, 1.0);
                gc.strokeLine(x - 1.0, height - 2.0, x + 1.0, height - 2.0);
                gc.strokeLine(x, 1.0, x, height - 2.0);
            }

            // DefaultRenderColorScheme.setLineScheme(gc, dataSet.getStyle(),
            // plotingIndex);
            gc.strokeLine(1, y, width, y);
        } else if (getErrorType() == ErrorStyle.ERRORSURFACE) {
            final double y = height / 2.0;
            gc.fillRect(1, 1, width - 2.0, height - 2.0);
            gc.strokeLine(1, y, width - 2.0, y);
        } else if (getErrorType() == ErrorStyle.ERRORCOMBO) {
            final double y = height / 2.0;
            gc.fillRect(1, 1, width - 2.0, height - 2.0);

            // DefaultRenderColorScheme.setLineScheme(gc, dataSet.getStyle(),
            // plotingIndex);
            gc.strokeLine(1, y, width - 2.0, y);
        } else {
            final double x = width / 2.0;
            final double y = height / 2.0;
            if (getDashSize() > 2) {
                gc.strokeLine(x - 1.0, 1.0, x + 1.0, 1.0);
                gc.strokeLine(x - 1.0, height - 2.0, x + 1, height - 2.0);
                gc.strokeLine(x, 1.0, x, height - 2.0);
            }

            // DefaultRenderColorScheme.setLineScheme(gc, dataSet.getStyle(),
            // plotingIndex);
            gc.strokeLine(1, y, width - 2.0, y);
        }
        gc.restore();
        return canvas;
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param localCachedPoints reference to local cached data point object
     */
    protected void drawDefaultNoErrors(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {

        drawBars(gc, localCachedPoints);
        drawPolyLine(gc, localCachedPoints);
        drawMarker(gc, localCachedPoints);

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

    protected void drawPolyLineLine(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);

        // gc.strokePolyline(localCachedPoints.xValues,
        // localCachedPoints.yValues, localCachedPoints.actualDataCount);

        for (int i = 0; i < localCachedPoints.actualDataCount - 1; i++) {
            final double x1 = localCachedPoints.xValues[i];
            final double x2 = localCachedPoints.xValues[i + 1];
            final double y1 = localCachedPoints.yValues[i];
            final double y2 = localCachedPoints.yValues[i + 1];
            gc.strokeLine(x1, y1, x2, y2);
        }

        // gc.beginPath();
        // for (int i=0; i< localCachedPoints.actualDataCount -1; i++) {
        // double x0 = localCachedPoints.xValues[i];
        // double x1 = localCachedPoints.xValues[i+1];
        // double y0 = localCachedPoints.yValues[i];
        // double y1 = localCachedPoints.yValues[i+1];
        // gc.moveTo(x0, y0);
        // gc.lineTo(x1, y1);
        // }
        // gc.closePath();
        // gc.stroke();

        gc.restore();
    }

    protected void drawPolyLineArea(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final double zero = localCachedPoints.yZero;
        final int n = localCachedPoints.actualDataCount;
        if (n == 0) {
            return;
        }

        // need to allocate new array :-(
        final double[] newX = cache.getCachedDoubleArray("xDrawPolyLineArea", n + 2);
        final double[] newY = cache.getCachedDoubleArray("yDrawPolyLineArea", n + 2);

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
        gc.fillPolygon(newX, newY, n + 2);
        gc.restore();

        // release arrays to cache
        cache.release("xDrawPolyLineArea", newX);
        cache.release("yDrawPolyLineArea", newY);
    }

    protected void drawPolyLineStairCase(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final int n = localCachedPoints.actualDataCount;
        if (n == 0) {
            return;
        }

        // need to allocate new array :-(
        final double[] newX = cache.getCachedDoubleArray("xDrawPolyLineStairCase", 2 * n);
        final double[] newY = cache.getCachedDoubleArray("yDrawPolyLineStairCase", 2 * n);

        for (int i = 0; i < n - 1; i++) {
            newX[2 * i] = localCachedPoints.xValues[i];
            newY[2 * i] = localCachedPoints.yValues[i];
            newX[2 * i + 1] = localCachedPoints.xValues[i + 1];
            newY[2 * i + 1] = localCachedPoints.yValues[i];
        }
        // last point
        newX[2 * (n - 1)] = localCachedPoints.xValues[n - 1];
        newY[2 * (n - 1)] = localCachedPoints.yValues[n - 1];
        newX[2 * n - 1] = localCachedPoints.xMax;
        newY[2 * n - 1] = localCachedPoints.yValues[n - 1];

        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);
        // gc.strokePolyline(newX, newY, 2*n);

        for (int i = 0; i < 2 * n - 1; i++) {
            final double x1 = newX[i];
            final double x2 = newX[i + 1];
            final double y1 = newY[i];
            final double y2 = newY[i + 1];
            gc.strokeLine(x1, y1, x2, y2);
        }

        gc.restore();

        // release arrays to cache
        cache.release("xDrawPolyLineStairCase", newX);
        cache.release("yDrawPolyLineStairCase", newY);
    }

    protected void drawPolyLineHistogram(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final int n = localCachedPoints.actualDataCount;
        if (n == 0) {
            return;
        }

        // need to allocate new array :-(
        final double[] newX = cache.getCachedDoubleArray("xDrawPolyLineHistogram", 2 * (n + 1));
        final double[] newY = cache.getCachedDoubleArray("yDrawPolyLineHistogram", 2 * (n + 1));

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
        newX[2 * (n + 1) - 1] = localCachedPoints.xValues[n - 1] + diffRight;
        newY[2 * (n + 1) - 1] = localCachedPoints.yZero;

        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);
        // gc.strokePolyline(newX, newY, 2*(n+1));

        for (int i = 0; i < 2 * (n + 1) - 1; i++) {
            final double x1 = newX[i];
            final double x2 = newX[i + 1];
            final double y1 = newY[i];
            final double y2 = newY[i + 1];
            gc.strokeLine(x1, y1, x2, y2);
        }

        gc.restore();

        // release arrays to cache
        cache.release("xDrawPolyLineHistogram", newX);
        cache.release("yDrawPolyLineHistogram", newY);
    }

    protected void drawPolyLineHistogramFilled(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final int n = localCachedPoints.actualDataCount;
        if (n == 0) {
            return;
        }

        // need to allocate new array :-(
        final double[] newX = cache.getCachedDoubleArray("xDrawPolyLineHistogram", 2 * (n + 1));
        final double[] newY = cache.getCachedDoubleArray("yDrawPolyLineHistogram", 2 * (n + 1));

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
        newX[2 * (n + 1) - 1] = localCachedPoints.xValues[n - 1] + diffRight;
        newY[2 * (n + 1) - 1] = localCachedPoints.yZero;

        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, localCachedPoints.defaultStyle,
                localCachedPoints.dataSetIndex + localCachedPoints.dataSetStyleIndex);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, localCachedPoints.defaultStyle);
        // use stroke as fill colour
        gc.setFill(gc.getStroke());
        gc.fillPolygon(newX, newY, 2 * (n + 1));
        gc.restore();

        // release arrays to cache
        cache.release("xDrawPolyLineHistogram", newX);
        cache.release("yDrawPolyLineHistogram", newY);
    }

    protected void drawPolyLineHistogramBezier(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        final int n = localCachedPoints.actualDataCount;
        if (n < 2) {
            drawPolyLineLine(gc, localCachedPoints);
            return;
        }

        // need to allocate new array :-(
        final double[] xCp1 = cache.getCachedDoubleArray("xBezierFirstControlPoint", n);
        final double[] yCp1 = cache.getCachedDoubleArray("yBezierFirstControlPoint", n);
        final double[] xCp2 = cache.getCachedDoubleArray("xBezierSecondControlPoint", n);
        final double[] yCp2 = cache.getCachedDoubleArray("yBezierSecondControlPoint", n);

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

        // release arrays to cache
        cache.release("xBezierFirstControlPoint", xCp1);
        cache.release("yBezierFirstControlPoint", yCp1);
        cache.release("xBezierSecondControlPoint", xCp2);
        cache.release("yBezierSecondControlPoint", yCp2);
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

        // N.B. the markers are drawn in the same colour as the polyline (ie.
        // not the fillColor)
        final Color fillColor = StyleParser.getColorPropertyValue(localCachedPoints.defaultStyle,
                XYChartCss.STROKE_COLOR);
        if (fillColor != null) {
            gc.setFill(fillColor);
        }

        // TODO check how-to draw marker
        // final Marker marker = getMarker();
        // if (marker == null) {
        // // marker = MarkerFactory.getMarker(Marker.CIRCLE);
        // }

        for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
            // marker.draw(gc, localCachedPoints.xValues[i],
            // localCachedPoints.yValues[i],
            // getMarkerSize(), style[i], localCachedPoints.selected[i]);
            final double mSize = getMarkerSize();
            gc.fillRect(localCachedPoints.xValues[i] - mSize, localCachedPoints.yValues[i] - mSize, 2 * mSize,
                    2 * mSize);
        }

        gc.restore();
    }

    /**
     * @param gc the graphics context from the Canvas parent
     * @param localCachedPoints reference to local cached data point object
     */
    protected void drawBars(final GraphicsContext gc, final CachedDataPoints localCachedPoints) {
        if (!isDrawBars()) {
            return;
        }

        final int xOffset = localCachedPoints.dataSetIndex >= 0 ? localCachedPoints.dataSetIndex : 0;
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
                if (localCachedPoints.selected[i]) {
                    gc.strokeLine(localCachedPoints.xZero, localCachedPoints.yZero, localCachedPoints.xValues[i],
                            localCachedPoints.yValues[i]);
                } else {
                    // work-around: bar colour controlled by the marker color
                    gc.save();
                    gc.setLineWidth(barWidthHalf);
                    gc.strokeLine(localCachedPoints.xZero, localCachedPoints.yZero, localCachedPoints.xValues[i],
                            localCachedPoints.yValues[i]);
                    gc.restore();
                }
            }
        } else {
            for (int i = 0; i < localCachedPoints.actualDataCount; i++) {
                double yDiff = localCachedPoints.yValues[i] - localCachedPoints.yZero;
                double yMin;
                if (yDiff > 0) {
                    yMin = localCachedPoints.yZero;
                } else {
                    yMin = localCachedPoints.yValues[i];
                    yDiff = Math.abs(yDiff);
                }

                if (localCachedPoints.selected[i]) {
                    gc.strokeRect(localCachedPoints.xValues[i] - barWidthHalf, yMin, localBarWidth, yDiff);
                } else {
                    // work-around: bar colour controlled by the marker color
                    gc.fillRect(localCachedPoints.xValues[i] - barWidthHalf, yMin, localBarWidth, yDiff);
                }
            }
        }

        gc.restore();
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

            if (lCacheP.errorType == ErrorType.XY || lCacheP.errorType == ErrorType.XY_ASYMMETRIC) {
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
            } else if (lCacheP.errorType == ErrorType.Y || lCacheP.errorType == ErrorType.Y_ASYMMETRIC) {
                // draw error bars
                gc.strokeLine(lCacheP.xValues[i], lCacheP.errorYNeg[i], lCacheP.xValues[i], lCacheP.errorYPos[i]);

                // draw horizontal dashes
                gc.strokeLine(lCacheP.xValues[i] - dashHalf, lCacheP.errorYNeg[i], lCacheP.xValues[i] + dashHalf,
                        lCacheP.errorYNeg[i]);
                gc.strokeLine(lCacheP.xValues[i] - dashHalf, lCacheP.errorYPos[i], lCacheP.xValues[i] + dashHalf,
                        lCacheP.errorYPos[i]);

            } else if (lCacheP.errorType == ErrorType.X || lCacheP.errorType == ErrorType.X_ASYMMETRIC) {
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
        final double[] xValuesSurface = cache.getCachedDoubleArray("xValuesSurface", nPolygoneEdges);
        final double[] yValuesSurface = cache.getCachedDoubleArray("yValuesSurface", nPolygoneEdges);

        final int xend = nPolygoneEdges - 1;
        for (int i = 0; i < nDataCount; i++) {
            xValuesSurface[i] = localCachedPoints.xValues[i];
            yValuesSurface[i] = localCachedPoints.errorYNeg[i];
            xValuesSurface[xend - i] = localCachedPoints.xValues[i];
            yValuesSurface[xend - i] = localCachedPoints.errorYPos[i];
        }
        // swap y coordinates at mid-point
        if (nDataCount > 4) {
            final double yTmp = yValuesSurface[nDataCount - 1];
            yValuesSurface[nDataCount - 1] = yValuesSurface[xend - nDataCount + 1];
            yValuesSurface[xend - nDataCount + 1] = yTmp;
        }

        gc.setFillRule(FillRule.EVEN_ODD);
        gc.fillPolygon(xValuesSurface, yValuesSurface, nPolygoneEdges);

        drawPolyLine(gc, localCachedPoints);
        drawBars(gc, localCachedPoints);
        drawMarker(gc, localCachedPoints);

        cache.release("xValuesSurface", xValuesSurface);
        cache.release("yValuesSurface", yValuesSurface);

        ProcessingProfiler.getTimeDiff(start);
    }

    /**
     * @return the instance of this ErrorDataSetRenderer.
     */
    @Override
    protected ErrorDataSetRenderer getThis() {
        return this;
    }

    /**
     * Returns the marker used by this renderer.
     *
     * @return the marker to be drawn on the data points
     */
    public Marker getMarker() {
        return marker;
    }

    /**
     * Replaces marker used by this renderer.
     *
     * @param marker the marker to be drawn on the data points
     */
    public void setMarker(final Marker marker) {
        this.marker = marker;
    }

    /********************************************************************
     ******* private class implementation (data point caching) **********
     *******************************************************************/

    /**
     * local screen data point cache (minimises re-allocation/garbage collection)
     */
    protected class CachedDataPoints {

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
        protected String defaultStyle;
        protected int dataSetIndex;
        protected int dataSetStyleIndex;
        protected ErrorType errorType;
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
        protected double xRange;
        protected double yRange;
        protected double maxRadius;
        protected int maxDataCount;
        protected int actualDataCount; // number of data points that remain
                                       // after data reduction

        public CachedDataPoints(final int indexMin, final int indexMax, final int dataLength, final boolean full) {
            maxDataCount = dataLength;
            xValues = cache.getCachedDoubleArray("xValues", maxDataCount);
            yValues = cache.getCachedDoubleArray("yValues", maxDataCount);
            styles = cache.getCachedStringArray("styles", dataLength);
            this.indexMin = indexMin;
            this.indexMax = indexMax;
            errorYNeg = cache.getCachedDoubleArray("errorYNeg", maxDataCount);
            errorYPos = cache.getCachedDoubleArray("errorYPos", maxDataCount);
            if (full) {
                errorXNeg = cache.getCachedDoubleArray("errorXNeg", maxDataCount);
                errorXPos = cache.getCachedDoubleArray("errorXPos", maxDataCount);
            }
            selected = cache.getCachedBooleanArray("selected", dataLength);
            // ArrayUtils.fillArray(selected, true);
            ArrayUtils.fillArray(styles, null);
        }

        /**
         * computes the minimum distance in between data points N.B. assumes sorted data set points
         *
         * @param coordinateSystem
         * @param dataSet reference to the specific data set to be drawn @see de.gsi.chart.data.DataSet
         * @param indexRange
         * @return
         */
        private int getMinXDistance() {
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

        private void computeScreenCoordinates(final Chart chart, final DataSet dataSet, final int dsIndex,
                final int min, final int max) {
            if (!(getFirstAxis(Orientation.HORIZONTAL) instanceof Axis)) {
                throw new InvalidParameterException(
                        "x Axis not a Axis derivative, xAxis = " + getFirstAxis(Orientation.HORIZONTAL));
            }
            if (!(getFirstAxis(Orientation.VERTICAL) instanceof Axis)) {
                throw new InvalidParameterException(
                        "y Axis not a Axis derivative, yAxis = " + getFirstAxis(Orientation.VERTICAL));
            }
            final Axis xAxis = getFirstAxis(Orientation.HORIZONTAL);
            final Axis yAxis = getFirstAxis(Orientation.VERTICAL);
            xAxisInverted = xAxis.isInvertedAxis();
            yAxisInverted = yAxis.isInvertedAxis();

            indexMin = min;
            indexMax = max;

            // compute cached axis variables ... about 50% faster than the
            // generic template based version from
            // ValueAxsis<Number>
            if (xAxis.isLogAxis()) {
                xZero = xAxis.getDisplayPosition(xAxis.getLowerBound());
            } else {
                xZero = xAxis.getDisplayPosition(0);
            }
            if (yAxis.isLogAxis()) {
                yZero = yAxis.getDisplayPosition(yAxis.getLowerBound());
            } else {
                yZero = yAxis.getDisplayPosition(0);
            }

            polarPlot = isPolarPlot;

            yMin = yAxis.getDisplayPosition(yAxis.getLowerBound());
            yMax = yAxis.getDisplayPosition(yAxis.getUpperBound());
            xMin = xAxis.getDisplayPosition(xAxis.getLowerBound());
            xMax = xAxis.getDisplayPosition(xAxis.getUpperBound());

            defaultStyle = dataSet.getStyle();

            xRange = Math.abs(xMax - xMin);
            yRange = Math.abs(yMax - yMin);
            maxRadius = 0.5 * Math.max(Math.min(xRange, yRange), 20) * 0.9;
            // TODO: parameterise '0.9' -> radius axis fills 90% of min canvas
            // axis
            if (polarPlot) {
                xZero = 0.5 * xRange;
                yZero = 0.5 * yRange;
                // System.err.println("maxRadius = " + maxRadius + " range: yMin
                // " + yMin + " yMax" + yMax);
            }

            final Integer layoutOffset = StyleParser.getIntegerPropertyValue(defaultStyle,
                    XYChartCss.DATASET_LAYOUT_OFFSET);
            final Integer dsIndexLocal = StyleParser.getIntegerPropertyValue(defaultStyle, XYChartCss.DATASET_INDEX);

            dataSetStyleIndex = layoutOffset == null ? 0 : layoutOffset.intValue(); // TODO:
                                                                                    // rationalise

            dataSetIndex = dsIndexLocal == null ? dsIndex : dsIndexLocal.intValue();

            // compute screen coordinates of other points
            if (dataSet instanceof DataSetError) {
                final DataSetError ds = (DataSetError) dataSet;
                errorType = ds.getErrorType();
            } else {
                // fall-back for standard DataSet

                // default: ErrorType=Y fall-back also for 'DataSet' without
                // errors
                // rationale: scientific honesty
                // if data points are being compressed, the error of compression
                // (e.g. due to local transients that are being suppressed) are
                // nevertheless being computed and shown even if individual data
                // points have no error
                errorType = ErrorType.Y;
            }

            // special case where users does not want error bars
            if (getErrorType() == ErrorStyle.NONE) {
                errorType = ErrorType.NO_ERROR;
            }

            // ErrorDataSetRenderer.this.setParallelImplementation(false);
            // compute data set to screen coordinates
            if (isParallelImplementation()) {
                final int minthreshold = 1000;
                // Math.min(length / minthreshold, MAX_THREADS);
                int divThread = (int) Math.ceil(Math.abs(max - min) / (double) MAX_THREADS);
                int stepSize = Math.max(divThread, minthreshold);
                final List<Callable<Boolean>> workers = new ArrayList<>();
                for (int i = min; i < max; i += stepSize) {
                    final int start = i;
                    workers.add(() -> {
                        computeScreenCoordinates(xAxis, yAxis, dataSet, start, Math.min(max, start + stepSize));
                        return Boolean.TRUE;
                    });
                }

                try {
                    final List<Future<Boolean>> jobs = ErrorDataSetRenderer.executorService.invokeAll(workers);
                    for (final Future<Boolean> future : jobs) {
                        final Boolean r = future.get();
                        if (!r) {
                            throw new IllegalStateException("one parallel worker thread finished execution with error");
                        }
                    }
                } catch (final InterruptedException | ExecutionException e) {
                    throw new IllegalStateException("one parallel worker thread finished execution with error", e);
                }
            } else {
                computeScreenCoordinates(xAxis, yAxis, dataSet, min, max);
            }

        }

        private void computeScreenCoordinates(final Axis xAxis, final Axis yAxis, final DataSet dataSet, final int min,
                final int max) {
            switch (errorType) {
            case NO_ERROR: // no error attached
                if (!polarPlot) {
                    for (int index = min; index < max; index++) {
                        final double x = dataSet.getX(index);
                        final double y = dataSet.getY(index);
                        // check if error should be surrounded by Math.abs(..)
                        // to ensure that they are always positive
                        xValues[index] = xAxis.getDisplayPosition(x);
                        yValues[index] = yAxis.getDisplayPosition(y);
                        if (!Double.isFinite(yValues[index])) {
                            yValues[index] = yMin;
                        }
                    }
                } else {
                    // experimental transform euclidean to polar coordinates

                    for (int index = min; index < max; index++) {
                        final double x = dataSet.getX(index);
                        final double y = dataSet.getY(index);
                        // check if error should be surrounded by Math.abs(..)
                        // to ensure that they are always positive
                        final double phi = x * ErrorDataSetRenderer.DEG_TO_RAD;
                        final double r = maxRadius * Math.abs(1 - yAxis.getDisplayPosition(y) / yRange);
                        xValues[index] = xZero + r * Math.cos(phi);
                        yValues[index] = yZero + r * Math.sin(phi);

                        if (!Double.isFinite(yValues[index])) {
                            yValues[index] = yZero;
                        }
                    }
                }

                return;
            case Y: // only symmetric errors around y
            case Y_ASYMMETRIC: // asymmetric errors around y
                if (!polarPlot) {
                    if (dataSet instanceof DataSetError) {
                        final DataSetError ds = (DataSetError) dataSet;

                        for (int index = min; index < max; index++) {
                            final double x = dataSet.getX(index);
                            final double y = dataSet.getY(index);
                            // check if error should be surrounded by
                            // Math.abs(..)
                            // to ensure that they are always positive
                            xValues[index] = xAxis.getDisplayPosition(x);
                            yValues[index] = yAxis.getDisplayPosition(y);
                            if (Double.isFinite(yValues[index])) {
                                errorYNeg[index] = yAxis.getDisplayPosition(y - ds.getYErrorNegative(index));
                                errorYPos[index] = yAxis.getDisplayPosition(y + ds.getYErrorPositive(index));
                            } else {
                                yValues[index] = yMin;
                                errorYNeg[index] = yMin;
                                errorYPos[index] = yMin;
                            }
                        }
                        return;
                    }

                    // default dataset
                    for (int index = min; index < max; index++) {
                        final double x = dataSet.getX(index);
                        final double y = dataSet.getY(index);
                        // check if error should be surrounded by Math.abs(..)
                        // to ensure that they are always positive
                        xValues[index] = xAxis.getDisplayPosition(x);
                        yValues[index] = yAxis.getDisplayPosition(y);

                        if (!Double.isFinite(xValues[index])) {
                            xValues[index] = xMin;
                        }
                        if (Double.isFinite(yValues[index])) {
                            errorYNeg[index] = yValues[index];
                            errorYPos[index] = yValues[index];
                        } else {
                            yValues[index] = yMin;
                            errorYNeg[index] = yMin;
                            errorYPos[index] = yMin;
                        }
                    }
                } else {
                    for (int index = min; index < max; index++) {
                        final double x = dataSet.getX(index);
                        final double y = dataSet.getY(index);
                        // check if error should be surrounded by Math.abs(..)
                        // to ensure that they are always positive
                        final double phi = x * ErrorDataSetRenderer.DEG_TO_RAD;
                        final double r = maxRadius * Math.abs(1 - yAxis.getDisplayPosition(y) / yRange);
                        xValues[index] = xZero + r * Math.cos(phi);
                        yValues[index] = yZero + r * Math.sin(phi);

                        // ignore errors (for now) -> TODO: add proper
                        // transformation
                        errorXNeg[index] = 0.0;
                        errorXPos[index] = 0.0;
                        errorYNeg[index] = 0.0;
                        errorYPos[index] = 0.0;

                        if (!Double.isFinite(yValues[index])) {
                            yValues[index] = yZero;
                        }
                    }
                }
                return;
            case X: // only symmetric errors around x
            case X_ASYMMETRIC: // asymmetric errors around x
            case XY: // symmetric errors around x and y
            case XY_ASYMMETRIC: // asymmetric errors around x and y
            default:
                if (!(dataSet instanceof DataSetError)) {
                    throw new IllegalStateException("dataSet may not be non-DataSetError at this stage, dataSet = "
                            + dataSet.getName() + " errorType = " + errorType);
                }
                final DataSetError ds = (DataSetError) dataSet;

                if (!polarPlot) {
                    for (int index = min; index < max; index++) {
                        final double x = dataSet.getX(index);
                        final double y = dataSet.getY(index);
                        // check if error should be surrounded by
                        // Math.abs(..) to ensure that they are always positive
                        xValues[index] = xAxis.getDisplayPosition(x);
                        yValues[index] = yAxis.getDisplayPosition(y);

                        if (Double.isFinite(xValues[index])) {
                            errorXNeg[index] = xAxis.getDisplayPosition(x - ds.getXErrorNegative(index));
                            errorXPos[index] = xAxis.getDisplayPosition(x + ds.getXErrorPositive(index));
                        } else {
                            xValues[index] = xMin;
                            errorXNeg[index] = xMin;
                            errorXPos[index] = xMin;
                        }

                        if (Double.isFinite(yValues[index])) {
                            errorYNeg[index] = yAxis.getDisplayPosition(y - ds.getYErrorNegative(index));
                            errorYPos[index] = yAxis.getDisplayPosition(y + ds.getYErrorPositive(index));
                        } else {
                            yValues[index] = yMin;
                            errorYNeg[index] = yMin;
                            errorYPos[index] = yMin;
                        }
                    }
                } else {
                    for (int index = min; index < max; index++) {
                        final double x = dataSet.getX(index);
                        final double y = dataSet.getY(index);
                        // check if error should be surrounded by Math.abs(..)
                        // to ensure that they are always positive
                        final double phi = x * ErrorDataSetRenderer.DEG_TO_RAD;
                        final double r = maxRadius * Math.abs(1 - yAxis.getDisplayPosition(y) / yRange);
                        // double r = maxRadius * Math.abs(-y /
                        // (dataSet.getYMax()-dataSet.getYMin()));
                        xValues[index] = xZero + r * Math.cos(phi);
                        yValues[index] = yZero + r * Math.sin(phi);

                        // ignore errors (for now) -> TODO: add proper
                        // transformation
                        errorXNeg[index] = 0.0;
                        errorXPos[index] = 0.0;
                        errorYNeg[index] = 0.0;
                        errorYPos[index] = 0.0;

                        if (!Double.isFinite(yValues[index])) {
                            yValues[index] = yZero;
                        }
                    }
                }
                return;
            }
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

        protected void reduce() {
            final long startTimeStamp = ProcessingProfiler.getTimeStamp();
            actualDataCount = 1;

            if (!isReducePoints() || Math.abs(indexMax - indexMin) < getMinRequiredReductionSize()) {
                actualDataCount = indexMax - indexMin;
                System.arraycopy(xValues, indexMin, xValues, 0, actualDataCount);
                System.arraycopy(yValues, indexMin, yValues, 0, actualDataCount);
                System.arraycopy(selected, indexMin, selected, 0, actualDataCount);
                switch (errorType) {
                case NO_ERROR: // no error attached
                    break;
                case Y: // only symmetric errors around y
                case Y_ASYMMETRIC: // asymmetric errors around y
                    System.arraycopy(errorYNeg, indexMin, errorYNeg, 0, actualDataCount);
                    System.arraycopy(errorYPos, indexMin, errorYPos, 0, actualDataCount);
                    break;
                case XY: // symmetric errors around x and y
                case X: // only symmetric errors around x
                case X_ASYMMETRIC: // asymmetric errors around x
                default:
                    System.arraycopy(errorXNeg, indexMin, errorXNeg, 0, actualDataCount);
                    System.arraycopy(errorXPos, indexMin, errorXPos, 0, actualDataCount);
                    System.arraycopy(errorYNeg, indexMin, errorYNeg, 0, actualDataCount);
                    System.arraycopy(errorYPos, indexMin, errorYPos, 0, actualDataCount);
                    break;
                }

                ProcessingProfiler.getTimeDiff(startTimeStamp,
                        String.format("no data reduction (%d)", actualDataCount));
                return;
            }

            final RendererDataReducer cruncher = rendererDataReducerProperty().get();

            if (!isReducePoints()) {

            }

            switch (errorType) {
            case NO_ERROR: // see comment above
            case Y:
                actualDataCount = cruncher.reducePoints(xValues, yValues, null, null, errorYPos, errorYNeg, styles,
                        selected, indexMin, indexMax);
                minDataPointDistanceX();
                break;
            case X:
            case XY:
            default:
                actualDataCount = cruncher.reducePoints(xValues, yValues, errorXPos, errorXNeg, errorYPos, errorYNeg,
                        styles, selected, indexMin, indexMax);

                minDataPointDistanceX();
                break;
            }
            // ProcessingProfiler.getTimeDiff(startTimeStamp,
        }

        @Override
        protected void finalize() throws Throwable {
            release();
            super.finalize();
        }

        public void release() {
            cache.release("xValues", xValues);
            cache.release("yValues", yValues);
            cache.release("errorYNeg", errorYNeg);
            cache.release("errorYPos", errorYPos);
            cache.release("errorXNeg", errorXNeg);
            cache.release("errorXPos", errorXPos);
            cache.release("selected", selected);
            cache.release("styles", styles);
        }
    }

}
