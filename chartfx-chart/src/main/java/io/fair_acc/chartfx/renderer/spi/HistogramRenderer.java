package io.fair_acc.chartfx.renderer.spi;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.dataset.events.ChartBits;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChartCss;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.CategoryAxis;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.utils.BezierCurve;
import io.fair_acc.chartfx.renderer.spi.utils.DefaultRenderColorScheme;
import io.fair_acc.chartfx.utils.StyleParser;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.Histogram;
import io.fair_acc.dataset.spi.LimitedIndexedTreeDataSet;
import io.fair_acc.dataset.utils.DoubleArrayCache;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * Simple renderer specialised for 1D histograms.
 *
 * N.B. this is _not_ primarily optimised for speed, does not deploy caching, and is intended for DataSets
 * (and Histogram derivatives) with significantly less than 1k data points. Non-histogram DataSets are sorted by default
 * (can be overridden via #autoSortingProperty()).
 * Please have a look at the ErrorDataSetRenderer for larger DataSets,
 *
 * @author rstein
 */
public class HistogramRenderer extends AbstractErrorDataSetRendererParameter<HistogramRenderer> implements Renderer {
    private final BooleanProperty animate = new SimpleBooleanProperty(this, "animate", false);
    private final BooleanProperty autoSorting = new SimpleBooleanProperty(this, "autoSorting", true);
    private final BooleanProperty roundedCorner = new SimpleBooleanProperty(this, "roundedCorner", true);
    private final IntegerProperty roundedCornerRadius = new SimpleIntegerProperty(this, "roundedCornerRadius", 10);
    private final Map<String, Double> scaling = new ConcurrentHashMap<>();
    private final AnimationTimer timer = new MyTimer();

    public HistogramRenderer() {
        super();
        setPolyLineStyle(LineStyle.HISTOGRAM_FILLED);
        PropUtil.runOnChange(this::invalidateCanvas,
                animate,
                autoSorting,
                roundedCorner,
                roundedCornerRadius);
    }

    public BooleanProperty animateProperty() {
        return animate;
    }

    public BooleanProperty autoSortingProperty() {
        return autoSorting;
    }

    @Override
    public boolean drawLegendSymbol(final DataSetNode dataSet, final Canvas canvas) {
        final int width = (int) canvas.getWidth();
        final int height = (int) canvas.getHeight();
        final GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.save();
        DefaultRenderColorScheme.setLineScheme(gc, dataSet);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, dataSet.getStyle());
        DefaultRenderColorScheme.setFillScheme(gc, dataSet.getStyle(), dataSet.getColorIndex());

        final double y = height / 2.0;
        gc.fillRect(1, 1, width - 2.0, height - 2.0);
        gc.strokeLine(1, y, width - 2.0, y);
        gc.restore();
        return true;
    }

    public int getRoundedCornerRadius() {
        return roundedCornerRadiusProperty().get();
    }

    public boolean isAnimate() {
        return animateProperty().get();
    }

    public boolean isAutoSorting() {
        return autoSortingProperty().get();
    }

    public boolean isRoundedCorner() {
        return roundedCornerProperty().get();
    }

    @Override
    protected void render(final GraphicsContext gc, DataSet dataSet, final DataSetNode style) {
        // replace DataSet with sorted variety
        // do not need to do this for Histograms as they are always sorted by design
        if (!(dataSet instanceof Histogram) && isAutoSorting() && (!isDataSetSorted(dataSet, DIM_X) && !isDataSetSorted(dataSet, DIM_Y))) {
            LimitedIndexedTreeDataSet newDataSet = new LimitedIndexedTreeDataSet(dataSet.getName(), Integer.MAX_VALUE);
            dataSet = newDataSet.set(dataSet);
        }

        // TODO: replace styling with CSS node
        var localDataSetList = Collections.singletonList(dataSet);
        drawHistograms(gc, localDataSetList, xAxis, yAxis, style.getColorIndex());
        drawBars(gc, localDataSetList, xAxis, yAxis, style.getColorIndex(), true);

        if (isAnimate()) {
            timer.start();
        }

    }

    public BooleanProperty roundedCornerProperty() {
        return roundedCorner;
    }

    public IntegerProperty roundedCornerRadiusProperty() {
        return roundedCornerRadius;
    }

    public void setAnimate(final boolean animate) {
        this.animateProperty().set(animate);
    }

    public void setAutoSorting(final boolean autoSorting) {
        this.autoSortingProperty().set(autoSorting);
    }

    public void setRoundedCorner(final boolean roundedCorner) {
        this.roundedCornerProperty().set(roundedCorner);
    }

    public void setRoundedCornerRadius(final int roundedCornerRadius) {
        this.roundedCornerRadius.set(roundedCornerRadius);
    }

    protected void drawBars(final GraphicsContext gc, final List<DataSet> dataSets, final Axis xAxis, final Axis yAxis, final int dataSetOffset, final boolean filled) { // NOPMD NOSONAR - complexity nearly unavoidable
        if (!isDrawBars()) {
            return;
        }

        int lindex = dataSetOffset - 1;
        for (DataSet ds : dataSets) {
            lindex++;
            if (!ds.isVisible() || ds.getDataCount() == 0) {
                continue;
            }
            final double scaleValue = isAnimate() ? scaling.getOrDefault(ds.getName(), 1.0) : 1.0;
            final boolean isVerticalDataSet = isVerticalDataSet(ds);

            final double barWPercentage = getBarWidthPercentage();
            final double constBarWidth = getBarWidth();

            final int dimIndexAbscissa = isVerticalDataSet ? DIM_Y : DIM_X;
            final int dimIndexOrdinate = isVerticalDataSet ? DIM_X : DIM_Y;
            final Axis abscissa = isVerticalDataSet ? yAxis : xAxis;
            final Axis ordinate = isVerticalDataSet ? xAxis : yAxis;

            final int indexMin = Math.max(0, ds.getIndex(dimIndexAbscissa, Math.min(abscissa.getMin(), abscissa.getMax())));
            final int indexMax = Math.min(ds.getDataCount(), ds.getIndex(dimIndexAbscissa, Math.max(abscissa.getMin(), abscissa.getMax()) + 1.0));
            final int nRange = Math.abs(indexMax - indexMin);
            final double axisMin = getAxisMin(xAxis, yAxis, !isVerticalDataSet);
            final boolean isHistogram = ds instanceof Histogram;

            gc.save();
            DefaultRenderColorScheme.setMarkerScheme(gc, ds.getStyle(), lindex);
            DefaultRenderColorScheme.setLineScheme(gc, ds.getStyle(), lindex);
            DefaultRenderColorScheme.setGraphicsContextAttributes(gc, ds.getStyle());
            gc.setFill(gc.getStroke()); // global fill equals to stroke

            for (int i = 0; i < nRange; i++) {
                final int index = indexMin + i;

                final double scale = isAnimate() ? Math.max(0.0, Math.min(1.0, scaleValue - index)) : 1.0;
                final double binValue = ordinate.getDisplayPosition(scale * ds.get(dimIndexOrdinate, index));
                final double binCentre = abscissa.getDisplayPosition(ds.get(dimIndexAbscissa, index));
                final double binStart = abscissa.getDisplayPosition(getBinStart(ds, dimIndexAbscissa, index));
                final double binStop = abscissa.getDisplayPosition(getBinStop(ds, dimIndexAbscissa, index));
                final double minRequiredWidth = Math.max(getDashSize(), Math.abs(binStop - binStart) / (this.isShiftBar() ? dataSets.size() : 1.0));
                final double binWidth = minRequiredWidth * barWPercentage / 100.0;
                final double localBarWidth = isDynamicBarWidth() ? 0.5 * binWidth : constBarWidth;
                final double barOffset;
                if (dataSets.size() == 1) {
                    barOffset = 0.0;
                } else {
                    barOffset = (isDynamicBarWidth() ? minRequiredWidth : getShiftBarOffset()) * (lindex - 0.25 * dataSets.size());
                }

                final double offset = this.isShiftBar() ? barOffset : 0.0;
                final double x0 = isHistogram ? binStart : binCentre - localBarWidth - offset;
                final double x1 = isHistogram ? binStop : binCentre + localBarWidth - offset;
                final String dataPointStyle = ds.getStyle(index);
                if (dataPointStyle != null) {
                    gc.save();
                    DefaultRenderColorScheme.setMarkerScheme(gc, dataPointStyle, lindex);
                    DefaultRenderColorScheme.setLineScheme(gc, dataPointStyle, lindex);
                    DefaultRenderColorScheme.setFillScheme(gc, dataPointStyle, lindex);
                    DefaultRenderColorScheme.setGraphicsContextAttributes(gc, dataPointStyle);
                }
                double topRadius = isRoundedCorner() ? Math.max(0, Math.min(getRoundedCornerRadius(), 0.5 * binWidth)) : 0.0;

                drawBar(gc, x0, axisMin, x1, binValue, topRadius, isVerticalDataSet, filled);

                if (dataPointStyle != null) {
                    gc.restore();
                }
            }

            gc.restore();
        } /* end of DataSet list loop */

        gc.save();
    }

    protected void drawHistograms(final GraphicsContext gc, final List<DataSet> dataSets, final Axis xAxis, final Axis yAxis, final int dataSetOffset) {
        switch (getPolyLineStyle()) {
        case NONE:
            return;
        case AREA:
            drawPolyLineLine(gc, dataSets, xAxis, yAxis, dataSetOffset, true);
            break;
        case ZERO_ORDER_HOLDER:
        case STAIR_CASE:
            drawPolyLineStairCase(gc, dataSets, xAxis, yAxis, dataSetOffset, false);
            break;
        case HISTOGRAM:
            drawPolyLineHistogram(gc, dataSets, xAxis, yAxis, dataSetOffset, false);
            break;
        case HISTOGRAM_FILLED:
            drawPolyLineHistogram(gc, dataSets, xAxis, yAxis, dataSetOffset, true);
            break;
        case BEZIER_CURVE:
            drawPolyLineHistogramBezier(gc, dataSets, xAxis, yAxis, dataSetOffset, true);
            break;
        case NORMAL:
        default:
            drawPolyLineLine(gc, dataSets, xAxis, yAxis, dataSetOffset, false);
            break;
        }
    }

    protected static void drawPolyLineHistogram(final GraphicsContext gc, final List<DataSet> dataSets, final Axis xAxis, final Axis yAxis, final int dataSetOffset, boolean filled) {
        int lindex = dataSetOffset - 1;
        for (DataSet ds : dataSets) {
            lindex++;
            if (!ds.isVisible() || ds.getDataCount() == 0) {
                continue;
            }
            final boolean isVerticalDataSet = isVerticalDataSet(ds);

            final int dimIndexAbscissa = isVerticalDataSet ? DIM_Y : DIM_X;
            final int dimIndexOrdinate = isVerticalDataSet ? DIM_X : DIM_Y;
            final Axis abscissa = isVerticalDataSet ? yAxis : xAxis;
            final Axis ordinate = isVerticalDataSet ? xAxis : yAxis;

            final int indexMin = Math.max(0, ds.getIndex(dimIndexAbscissa, Math.min(abscissa.getMin(), abscissa.getMax())));
            final int indexMax = Math.min(ds.getDataCount(), ds.getIndex(dimIndexAbscissa, Math.max(abscissa.getMin(), abscissa.getMax()) + 1.0));

            // need to allocate new array :-(
            final int nRange = Math.abs(indexMax - indexMin);
            final double[] newX = DoubleArrayCache.getInstance().getArrayExact(2 * (nRange + 1));
            final double[] newY = DoubleArrayCache.getInstance().getArrayExact(2 * (nRange + 1));
            final double axisMin = getAxisMin(xAxis, yAxis, !isVerticalDataSet);

            for (int i = 0; i < nRange; i++) {
                final int index = indexMin + i;
                final double binValue = ordinate.getDisplayPosition(ds.get(dimIndexOrdinate, index));
                final double binStart = abscissa.getDisplayPosition(getBinStart(ds, dimIndexAbscissa, index));
                final double binStop = abscissa.getDisplayPosition(getBinStop(ds, dimIndexAbscissa, index));
                newX[2 * i + 1] = binStart;
                newY[2 * i + 1] = binValue;
                newX[2 * i + 2] = binStop;
                newY[2 * i + 2] = binValue;
            }
            // first point
            newX[0] = newX[1];
            newY[0] = axisMin;

            // last point
            newX[2 * (nRange + 1) - 1] = newX[2 * (nRange + 1) - 2];
            newY[2 * (nRange + 1) - 1] = axisMin;

            gc.save();
            DefaultRenderColorScheme.setLineScheme(gc, ds.getStyle(), lindex);
            DefaultRenderColorScheme.setGraphicsContextAttributes(gc, ds.getStyle());

            drawPolygon(gc, newX, newY, filled, isVerticalDataSet);
            gc.restore();

            // release arrays to cache
            DoubleArrayCache.getInstance().add(newX);
            DoubleArrayCache.getInstance().add(newY);
        }
    }

    protected static void drawPolyLineHistogramBezier(final GraphicsContext gc, final List<DataSet> dataSets, final Axis xAxis, final Axis yAxis, final int dataSetOffset, boolean filled) {
        int lindex = dataSetOffset - 1;
        for (DataSet ds : dataSets) {
            lindex++;
            if (!ds.isVisible()) {
                continue;
            }
            final boolean isVerticalDataSet = isVerticalDataSet(ds);

            final int dimIndexAbscissa = isVerticalDataSet ? DIM_Y : DIM_X;
            final Axis abscissa = isVerticalDataSet ? yAxis : xAxis;
            final int indexMin = Math.max(0, ds.getIndex(dimIndexAbscissa, Math.min(abscissa.getMin(), abscissa.getMax())));
            final int indexMax = Math.min(ds.getDataCount(), ds.getIndex(dimIndexAbscissa, Math.max(abscissa.getMin(), abscissa.getMax()) + 1.0));

            final int min = Math.min(indexMin, indexMax);
            final int nRange = Math.abs(indexMax - indexMin);

            if (nRange <= 2) {
                drawPolyLineLine(gc, List.of(ds), xAxis, yAxis, lindex, filled);
                continue;
            }

            // need to allocate new array :-(
            final double[] xCp1 = DoubleArrayCache.getInstance().getArrayExact(nRange);
            final double[] yCp1 = DoubleArrayCache.getInstance().getArrayExact(nRange);
            final double[] xCp2 = DoubleArrayCache.getInstance().getArrayExact(nRange);
            final double[] yCp2 = DoubleArrayCache.getInstance().getArrayExact(nRange);

            final double[] xValues = DoubleArrayCache.getInstance().getArrayExact(nRange);
            final double[] yValues = DoubleArrayCache.getInstance().getArrayExact(nRange);

            for (int i = 0; i < nRange; i++) {
                xValues[i] = xAxis.getDisplayPosition(ds.get(DIM_X, min + i));
                yValues[i] = yAxis.getDisplayPosition(ds.get(DIM_Y, min + i));
            }
            BezierCurve.calcCurveControlPoints(xValues, yValues, xCp1, yCp1, xCp2, yCp2, nRange);

            gc.save();
            DefaultRenderColorScheme.setLineScheme(gc, ds.getStyle(), lindex);
            DefaultRenderColorScheme.setGraphicsContextAttributes(gc, ds.getStyle());

            // use stroke as fill colour
            gc.setFill(gc.getStroke());
            gc.beginPath();
            for (int i = 0; i < nRange - 1; i++) {
                final double x0 = xValues[i];
                final double x1 = xValues[i + 1];
                final double y0 = yValues[i];
                final double y1 = yValues[i + 1];

                // coordinates of first Bezier control point.
                final double xc0 = xCp1[i];
                final double yc0 = yCp1[i];
                // coordinates of the second Bezier control point.
                final double xc1 = xCp2[i];
                final double yc1 = yCp2[i];

                gc.moveTo(x0, y0);
                gc.bezierCurveTo(xc0, yc0, xc1, yc1, x1, y1);
            }
            gc.moveTo(xValues[nRange - 1], yValues[nRange - 1]);
            gc.closePath();
            gc.stroke();
            gc.restore();

            // release arrays to Cache
            DoubleArrayCache.getInstance().add(xValues);
            DoubleArrayCache.getInstance().add(yValues);
            DoubleArrayCache.getInstance().add(xCp1);
            DoubleArrayCache.getInstance().add(yCp1);
            DoubleArrayCache.getInstance().add(xCp2);
            DoubleArrayCache.getInstance().add(yCp2);
        }
    }

    protected static void drawPolyLineLine(final GraphicsContext gc, final List<DataSet> dataSets, final Axis xAxis, final Axis yAxis, final int dataSetOffset, boolean filled) { // NOPMD NOSONAR - complexity nearly unavoidable
        int lindex = dataSetOffset - 1;
        for (DataSet ds : dataSets) {
            lindex++;
            if (!ds.isVisible()) {
                continue;
            }
            final boolean isVerticalDataSet = isVerticalDataSet(ds);

            final int dimIndexAbscissa = isVerticalDataSet ? DIM_Y : DIM_X;
            final Axis abscissa = isVerticalDataSet ? yAxis : xAxis;
            final int indexMin = Math.max(0, ds.getIndex(dimIndexAbscissa, Math.min(abscissa.getMin(), abscissa.getMax())));
            final int indexMax = Math.min(ds.getDataCount(), ds.getIndex(dimIndexAbscissa, Math.max(abscissa.getMin(), abscissa.getMax()) + 1.0));
            final int nRange = Math.abs(indexMax - indexMin);
            if (nRange == 0) {
                continue;
            }

            gc.save();
            DefaultRenderColorScheme.setLineScheme(gc, ds.getStyle(), lindex);
            DefaultRenderColorScheme.setGraphicsContextAttributes(gc, ds.getStyle());
            gc.beginPath();
            double a = xAxis.getDisplayPosition(ds.get(DIM_X, indexMin));
            double b = yAxis.getDisplayPosition(ds.get(DIM_Y, indexMin));
            gc.moveTo(a, b);
            boolean lastIsFinite = true;
            double xLastValid = 0.0;
            double yLastValid = 0.0;
            for (int i = indexMin + 1; i < indexMax; i++) {
                a = xAxis.getDisplayPosition(ds.get(DIM_X, i));
                b = yAxis.getDisplayPosition(ds.get(DIM_Y, i));

                if (Double.isFinite(a) && Double.isFinite(b)) {
                    if (!lastIsFinite) {
                        gc.moveTo(a, b);
                        lastIsFinite = true;
                        continue;
                    }
                    gc.lineTo(a, b);
                    xLastValid = a;
                    yLastValid = b;
                    lastIsFinite = true;
                } else {
                    lastIsFinite = false;
                }
            }
            gc.moveTo(xLastValid, yLastValid);
            gc.closePath();

            if (filled) {
                gc.fill();
            } else {
                gc.stroke();
            }

            gc.restore();
        }
    }

    protected static void drawPolyLineStairCase(final GraphicsContext gc, final List<DataSet> dataSets, final Axis xAxis, final Axis yAxis, final int dataSetOffset, boolean filled) {
        int lindex = dataSetOffset - 1;
        for (DataSet ds : dataSets) {
            lindex++;
            if (!ds.isVisible()) {
                continue;
            }
            final boolean isVerticalDataSet = isVerticalDataSet(ds);

            final int dimIndexAbscissa = isVerticalDataSet ? DIM_Y : DIM_X;
            final int dimIndexOrdinate = isVerticalDataSet ? DIM_X : DIM_Y;
            final Axis abscissa = isVerticalDataSet ? yAxis : xAxis;
            final Axis ordinate = isVerticalDataSet ? xAxis : yAxis;

            final int indexMin = Math.max(0, ds.getIndex(dimIndexAbscissa, Math.min(abscissa.getMin(), abscissa.getMax())));
            final int indexMax = Math.min(ds.getDataCount(), ds.getIndex(dimIndexAbscissa, Math.max(abscissa.getMin(), abscissa.getMax()) + 1.0));

            final int min = Math.min(indexMin, indexMax);
            final int nRange = Math.abs(indexMax - indexMin);
            final double axisMin = getAxisMin(xAxis, yAxis, !isVerticalDataSet);
            if (nRange <= 0) {
                drawPolyLineLine(gc, List.of(ds), xAxis, yAxis, lindex, filled);
                continue;
            }

            // need to allocate new array :-(
            final double[] newX = DoubleArrayCache.getInstance().getArrayExact(2 * nRange);
            final double[] newY = DoubleArrayCache.getInstance().getArrayExact(2 * nRange);

            for (int i = 0; i < nRange - 1; i++) {
                final int index = i + min;
                newX[2 * i] = abscissa.getDisplayPosition(ds.get(dimIndexAbscissa, index));
                newY[2 * i] = ordinate.getDisplayPosition(ds.get(dimIndexOrdinate, index));
                newX[2 * i + 1] = abscissa.getDisplayPosition(ds.get(dimIndexAbscissa, index + 1));
                newY[2 * i + 1] = newY[2 * i];
            }
            // last point
            newX[2 * (nRange - 1)] = abscissa.getDisplayPosition(ds.get(dimIndexAbscissa, min + nRange - 1));
            newY[2 * (nRange - 1)] = ordinate.getDisplayPosition(ds.get(dimIndexOrdinate, min + nRange - 1));
            newX[2 * nRange - 1] = abscissa.getDisplayPosition(axisMin);
            newY[2 * nRange - 1] = newY[2 * (nRange - 1)];

            gc.save();
            DefaultRenderColorScheme.setLineScheme(gc, ds.getStyle(), lindex);
            DefaultRenderColorScheme.setGraphicsContextAttributes(gc, ds.getStyle());

            drawPolygon(gc, newX, newY, filled, isVerticalDataSet);
            gc.restore();

            // release arrays to cache
            DoubleArrayCache.getInstance().add(newX);
            DoubleArrayCache.getInstance().add(newY);
        }
    }

    protected static void drawPolygon(final GraphicsContext gc, final double[] a, final double[] b, final boolean filled, final boolean isVerticalDataSet) {
        if (filled) {
            // use stroke as fill colour
            gc.setFill(gc.getStroke());
            if (isVerticalDataSet) {
                gc.fillPolygon(b, a, a.length); // NOPMD NOSONAR - flip on purpose
            } else {
                gc.fillPolygon(a, b, a.length);
            }
            return;
        }
        // stroke only
        if (isVerticalDataSet) {
            gc.strokePolyline(b, a, a.length); // NOPMD NOSONAR - flip on purpose
        } else {
            gc.strokePolyline(a, b, a.length);
        }
    }

    protected static double estimateHalfBinWidth(final DataSet ds, final int dimIndex, final int index) {
        final int nMax = ds.getDataCount();
        if (nMax == 0) {
            return 0.5;
        } else if (nMax == 1) {
            return 0.5 * Math.abs(ds.get(dimIndex, 1) - ds.get(dimIndex, 0));
        }
        final double binCentre = ds.get(dimIndex, index);
        final double diffLeft = index - 1 >= 0 ? Math.abs(binCentre - ds.get(dimIndex, index - 1)) : -1;
        final double diffRight = index + 1 < nMax ? Math.abs(ds.get(dimIndex, index + 1)) - binCentre : -1;
        final boolean isInValidLeft = diffLeft < 0;
        final boolean isInValidRight = diffRight < 0;
        if (isInValidLeft && isInValidRight) {
            return 0.5;
        } else if (isInValidLeft || isInValidRight) {
            return 0.5 * Math.max(diffLeft, diffRight);
        }
        return 0.5 * Math.min(diffLeft, diffRight);
    }

    protected static double getBinStart(final DataSet ds, final int dimIndex, final int index) {
        if (ds instanceof Histogram) {
            return ((Histogram) ds).getBinLimits(dimIndex, Histogram.Boundary.LOWER, index + 1); // '+1' because binIndex starts with '0' (under-flow bin)
        }
        return ds.get(dimIndex, index) - estimateHalfBinWidth(ds, dimIndex, index);
    }

    protected static double getBinStop(final DataSet ds, final int dimIndex, final int index) {
        if (ds instanceof Histogram) {
            return ((Histogram) ds).getBinLimits(dimIndex, Histogram.Boundary.UPPER, index + 1); // '+1' because binIndex starts with '0' (under-flow bin)
        }
        return ds.get(dimIndex, index) + estimateHalfBinWidth(ds, dimIndex, index);
    }

    @Override
    protected HistogramRenderer getThis() {
        return this;
    }

    protected static boolean isDataSetSorted(final DataSet dataSet, final int dimIndex) {
        if (dataSet.getDataCount() < 2) {
            return true;
        }
        double xLast = dataSet.get(dimIndex, 0);
        for (int i = 1; i < dataSet.getDataCount(); i++) {
            final double x = dataSet.get(dimIndex, i);
            if (x < xLast) {
                return false;
            }
            xLast = x;
        }

        return true;
    }

    protected static boolean isVerticalDataSet(final DataSet ds) {
        if (ds instanceof Histogram) {
            Histogram histogram = (Histogram) ds;
            return histogram.getBinCount(DIM_X) == 0 && histogram.getBinCount(DIM_Y) > 0;
        } else {
            boolean sortedInX = isDataSetSorted(ds, DIM_X);
            boolean sortedInY = isDataSetSorted(ds, DIM_Y);
            if (sortedInX && sortedInY) {
                // if sorted both in X and Y -> default to horizontal
                return false;
            }
            return sortedInY;
        }
    }

    private void drawBar(final GraphicsContext gc, final double x0, final double y0, final double x1, final double y1, final double radius, final boolean verticalDataSet, final boolean filled) { // NOPMD NOSONAR -- number of arguments
        final double a0 = verticalDataSet ? y0 : x0;
        final double a1 = verticalDataSet ? y1 : x1;
        final double b0 = verticalDataSet ? x0 : y0;
        final double b1 = verticalDataSet ? x1 : y1;

        if (filled) {
            gc.fillRoundRect(Math.min(a0, a1), Math.min(b0, b1), Math.abs(a1 - a0), Math.abs(b1 - b0), radius, radius);
        } else {
            gc.strokeRoundRect(Math.min(a0, a1), Math.min(b0, b1), Math.abs(a1 - a0), Math.abs(b1 - b0), radius, radius);
        }
    }

    private static double getAxisMin(final Axis xAxis, final Axis yAxis, final boolean isHorizontalDataSet) { // NOPMD NOSONAR -- unavoidable complexity
        final double xMin = Math.min(xAxis.getMin(), xAxis.getMax());
        final double yMin = Math.min(yAxis.getMin(), yAxis.getMax());

        if (isHorizontalDataSet) {
            // horizontal DataSet - draw bars/filling towards y-axis (N.B. most common case)
            if (yAxis.isLogAxis()) {
                // draws axis towards the bottom side or  -- if axis is inverted towards the top side
                return yAxis.isInvertedAxis() ? 0.0 : yAxis.getLength();
            } else {
                return yAxis.isInvertedAxis() ? Math.max(yAxis.getDisplayPosition(0), yAxis.getDisplayPosition(yMin)) : Math.min(yAxis.getDisplayPosition(0), yAxis.getDisplayPosition(yMin));
            }
        }

        // vertical DataSet - draw bars/filling towards y-axis
        if (xAxis.isLogAxis()) {
            // draws axis towards the left side or  -- if axis is inverted towards the right side
            return xAxis.isInvertedAxis() ? xAxis.getLength() : 0.0;
        } else {
            return yAxis.isInvertedAxis() ? Math.min(xAxis.getDisplayPosition(0), xAxis.getDisplayPosition(xMin)) : Math.max(xAxis.getDisplayPosition(0), xAxis.getDisplayPosition(xMin));
        }
    }

    private class MyTimer extends AnimationTimer {
        @Override
        public void handle(final long now) {
            if (!isAnimate()) {
                this.stop();
                return;
            }

            for (final var dataSet : getDatasets()) {
                // scheme 1
                // final Double val = scaling.put(dataSet.getName(), Math.min(scaling.computeIfAbsent(dataSet.getName(), ds -> 0.0) + 0.05, 1.0))
                // scheme 2
                final Double val = scaling.put(dataSet.getName(), Math.min(scaling.computeIfAbsent(dataSet.getName(), ds -> 0.0) + 0.05, dataSet.getDataCount() + 1.0));
                if (val != null && val < dataSet.getDataCount() + 1.0) {
                    invalidateCanvas();
                }
            }
        }
    }
}
