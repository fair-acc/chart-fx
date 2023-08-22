package io.fair_acc.chartfx.renderer.hebi;

import io.fair_acc.chartfx.renderer.spi.AbstractRendererXY;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.chartfx.utils.FastDoubleArrayCache;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.profiler.AggregateDurationMeasure;
import io.fair_acc.dataset.profiler.DurationMeasure;
import io.fair_acc.dataset.profiler.Profiler;
import io.fair_acc.dataset.spi.CircularDoubleErrorDataSet;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import static io.fair_acc.dataset.DataSet.*;

/**
 * Special cased renderer that omits dynamic array allocations for live rendering. Optimizes
 * our particular use cases.
 *
 * @author Florian Enner
 * @since 25 Feb 2021
 */
public class ChartTraceRenderer extends AbstractRendererXY<ChartTraceRenderer> {

    public ChartTraceRenderer(DataSet... dataSets) {
        getDatasets().setAll(dataSets);
    }

    @Override
    protected void render(GraphicsContext gc, DataSet dataSet, DataSetNode style) {

        // check for potentially reduced data range we are supposed to plot
        final int min = Math.max(0, dataSet.getIndex(DIM_X, xMin) - 1);
        final int max = Math.min(dataSet.getIndex(DIM_X, xMax) + 2, dataSet.getDataCount()); /* excluded in the drawing */
        final int count = max - min;
        if (count <= 0) {
            return;
        }

        // make sure temp array is large enough
        this.style = style;
        xCoords = SHARED_ARRAYS.getArray(0, count);
        yCoords = SHARED_ARRAYS.getArray(1, count);

        gc.save();
        gc.setLineWidth(style.getLineWidth());
        gc.setStroke(style.getLineColor());
        gc.setFill(style.getLineColor());

        final var marker = style.getMarkerType();

        // compute local screen coordinates
        for (int i = min; i < max; ) {

            benchComputeCoords.start();

            // find the first valid point
            double xi = dataSet.get(DIM_X, i);
            double yi = dataSet.get(DIM_Y, i);
            i++;
            while (Double.isNaN(xi) || Double.isNaN(yi)) {
                i++;
                continue;
            }

            // start coord array
            double x = xAxis.getDisplayPosition(xi);
            double y = yAxis.getDisplayPosition(yi);
            double prevX = xCoords[0] = x;
            double prevY = yCoords[0] = y;
            coordLength = 1;

            // Build contiguous non-nan segments, so we can use the more efficient strokePolyLine
            while (i < max) {
                xi = dataSet.get(DIM_X, i);
                yi = dataSet.get(DIM_Y, i);
                i++;

                // Skip iteration and draw whatever we have for now
                if (Double.isNaN(xi) || Double.isNaN(yi)) {
                    break;
                }

                // Remove points that are unnecessary
                x = xAxis.getDisplayPosition(xi);
                y = yAxis.getDisplayPosition(yi);
                if (isSamePoint(prevX, prevY, x, y)) {
                    continue;
                }

                // Add point
                xCoords[coordLength] = prevX = x;
                yCoords[coordLength] = prevY = y;
                coordLength++;

            }
            benchComputeCoords.stop();

            // Draw coordinates
            if (drawMarker.get()) {
                // individual points
                for (int c = 0; c < coordLength; c++) {
                    marker.draw(gc, xCoords[c], yCoords[c], markerSize);
                }
            } else if (coordLength == 1) {
                // corner case for a single point that would be skipped by strokePolyLine
                gc.strokeLine(xCoords[0], yCoords[0], xCoords[0], yCoords[0]);
            } else {
                // solid and dashed line
                benchPolyLine.start();
                gc.strokePolyline(xCoords, yCoords, coordLength);
                benchPolyLine.stop();
            }

        }

        gc.restore();
        benchComputeCoords.reportSum();
        benchPolyLine.reportSum();

    }

    static boolean isSamePoint(double prevX, double prevY, double x, double y) {
        // Keep points within a certain pixel distance
        double dx = Math.abs(x - prevX);
        double dy = Math.abs(y - prevY);
        return dx < minPointPixelDistance && dy < minPointPixelDistance;
    }

    // 1 Pixel distance already filters out quite a bit. Anything higher looks jumpy.
    private static final double minPointPixelDistance = 1;

    @Override
    public boolean drawLegendSymbol(final DataSetNode style, final Canvas canvas) {
        var gc = canvas.getGraphicsContext2D();
        gc.save();
        gc.setLineWidth(style.getLineWidth());
        gc.setLineDashes(style.getLineDashes());
        gc.setStroke(style.getLineColor());
        gc.strokeLine(1, canvas.getHeight() / 2.0, canvas.getWidth() - 2.0, canvas.getHeight() / 2.0);
        gc.restore();
        return true;
    }

    @Override
    protected ChartTraceRenderer getThis() {
        return this;
    }

    public ChartTraceRenderer setMarkerSize(int markerSize) {
        this.markerSize = markerSize;
        return this;
    }

    public boolean isDrawMarker() {
        return drawMarker.get();
    }

    public BooleanProperty drawMarkerProperty() {
        return drawMarker;
    }

    public void setDrawMarker(boolean drawMarker) {
        this.drawMarker.set(drawMarker);
    }

    @Override
    public void setProfiler(Profiler profiler) {
        super.setProfiler(profiler);
        benchPolyLine = AggregateDurationMeasure.wrap(profiler.newDebugDuration("gc-drawPolyLine"));
        benchComputeCoords = AggregateDurationMeasure.wrap(profiler.newDebugDuration("gc-computeCoords"));
    }

    AggregateDurationMeasure benchComputeCoords = AggregateDurationMeasure.DISABLED;
    AggregateDurationMeasure benchPolyLine = AggregateDurationMeasure.DISABLED;

    // TODO: move to style
    private BooleanProperty drawMarker = new SimpleBooleanProperty(true);

    private int markerSize = 1;

    private DataSetNode style;
    private double[] xCoords;
    private double[] yCoords;
    private int coordLength = 0;

    private static final FastDoubleArrayCache SHARED_ARRAYS = new FastDoubleArrayCache(2);

    /**
     * Deletes all arrays that are larger than necessary for the last drawn dataset
     */
    public static void trimCache() {
        SHARED_ARRAYS.trim();
    }

}
