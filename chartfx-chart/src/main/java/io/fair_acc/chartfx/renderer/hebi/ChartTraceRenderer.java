package io.fair_acc.chartfx.renderer.hebi;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.marker.Marker;
import io.fair_acc.chartfx.renderer.spi.AbstractRendererXY;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.math.ArrayUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

        final double xAxisWidth = xAxis.getWidth();
        final double yAxisHeight = yAxis.getHeight();

        double unitsToRad = 1;
        final double polarRadius, xCenter, yCenter;
        final boolean isPolar = getChart().isPolarPlot();
        if (isPolar) {
            polarRadius = 0.5 * Math.max(Math.min(xAxisWidth, yAxisHeight), 20) * 0.9;
            xCenter = 0.5 * xAxisWidth;
            yCenter = 0.5 * yAxisHeight;
            if ("deg".equalsIgnoreCase(xAxis.getUnit())) {
                unitsToRad = 180 / Math.PI;
            }
        } else {
            polarRadius = 0;
            xCenter = 0;
            yCenter = 0;
        }


        // check for potentially reduced data range we are supposed to plot
        int min = Math.max(0, dataSet.getIndex(DIM_X, xMin) - 1);
        int max = Math.min(dataSet.getIndex(DIM_X, xMax) + 2, dataSet.getDataCount()); /* excluded in the drawing */

        // zero length/range data set -> nothing to be drawn
        if (max - min <= 0) {
            return;
        }

        // make sure temp array is large enough
        int newLength = max - min;
        this.style = style;
        xCoords = xCoordsShared = ArrayUtils.resizeMin(xCoordsShared, newLength);
        yCoords = yCoordsShared = ArrayUtils.resizeMin(yCoordsShared, newLength);

        // compute local screen coordinates
        double xi = dataSet.get(DIM_X, min);
        double yi = dataSet.get(DIM_Y, min);
        double x, y;
        if (!isPolar) {
            x = xAxis.getDisplayPosition(xi);
            y = yAxis.getDisplayPosition(yi);
        } else {
            double phi = xi * unitsToRad;
            double r = polarRadius * Math.abs(1 - (yAxis.getDisplayPosition(yi) / yAxisHeight));
            x = xCenter + (r * Math.cos(phi));
            y = yCenter + (r * Math.sin(phi));
        }
        double prevX = xCoords[0] = x;
        double prevY = yCoords[0] = y;
        coordLength = 1;
        for (int i = min + 1; i < max; i++) {
            xi = dataSet.get(DIM_X, i);
            yi = dataSet.get(DataSet.DIM_Y, i);
            if (!isPolar) {
                x = xAxis.getDisplayPosition(xi);
                y = yAxis.getDisplayPosition(yi);
            } else {
                double phi = xi * unitsToRad;
                double r = polarRadius * Math.abs(1 - (yAxis.getDisplayPosition(yi) / yAxisHeight));
                x = xCenter + (r * Math.cos(phi));
                y = yCenter + (r * Math.sin(phi));
            }

            // Reduce points if they don't provide any benefits
            if (isRedundantPoint(prevX, prevY, x, y)) {
                continue;
            }

            // Add point
            xCoords[coordLength] = prevX = x;
            yCoords[coordLength] = prevY = y;
            coordLength++;
        }

        // draw individual plot components
        if (drawMarker.get()) {
            drawMarker(gc); // individual points
        } else {
            drawPolyLine(gc); // solid and dashed line
        }
    }

    private static boolean isRedundantPoint(double prevX, double prevY, double x, double y) {
        // Ignore sequences of NaN
        if (Double.isNaN(y)) {
            return Double.isNaN(prevY);
        }

        // Keep points within a certain pixel distance
        double dx = Math.abs(x - prevX);
        double dy = Math.abs(y - prevY);
        return dx < minPointPixelDistance && dy < minPointPixelDistance;
    }

    // 1 Pixel distance already filters out quite a bit. Anything higher looks jumpy.
    private static final double minPointPixelDistance = 1;

    protected void drawMarker(final GraphicsContext gc) {
        gc.save();
        gc.setLineWidth(style.getLineWidth());
        gc.setStroke(style.getLineColor());
        gc.setFill(style.getLineColor());

        final var marker = style.getMarkerType();
        for (int i = 0; i < coordLength; i++) {
            marker.draw(gc, xCoords[i], yCoords[i], markerSize);
        }

        gc.restore();
    }

    protected void drawPolyLine(final GraphicsContext gc) {
        gc.save();
        gc.setLineWidth(style.getLineWidth());
        gc.setStroke(style.getLineColor());
        gc.setLineDashes(style.getLineDashes());

        gc.beginPath();
        gc.moveTo(xCoords[0], yCoords[0]);
        boolean lastIsFinite = true;
        double xLastValid = 0.0;
        double yLastValid = 0.0;
        for (int i = 0; i < coordLength; i++) {
            final double x0 = xCoords[i];
            final double y0 = yCoords[i];
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

        gc.restore();
    }

    /**
     * Draws lines between points as individual line primitives rather than as a
     * contiguous path. Preliminary results look like this is a bit faster, but
     * it needs more testing. Note: probably does not work with dashes.
     */
    protected void drawPolyLineIndividual(final GraphicsContext gc) {
        if (xCoords.length == 0) return;
        gc.save();
        gc.setLineWidth(style.getLineWidth());
        gc.setStroke(style.getLineColor());
        gc.setLineDashes(style.getLineDashes());

        double x0 = xCoords[0];
        double y0 = yCoords[0];
        for (int i = 1; i < coordLength; i++) {
            double x1 = xCoords[i];
            double y1 = yCoords[i];
            if (Double.isFinite(x0 + x1 + y0 + y1)) {
                gc.strokeLine(x0, y0, x1, y1);
            }
            x0 = x1;
            y0 = y1;
        }
        gc.restore();
    }

    @Override
    public boolean drawLegendSymbol(final DataSetNode style, final Canvas canvas) {
        final int width = (int) canvas.getWidth();
        final int height = (int) canvas.getHeight();
        final GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.save();
        gc.setLineWidth(style.getLineWidth());
        gc.setLineDashes(style.getLineDashes());
        gc.setStroke(style.getLineColor());

        final double y = height / 2.0;
        gc.strokeLine(1, y, width - 2.0, y);

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

    // TODO: move to style
    private BooleanProperty drawMarker = new SimpleBooleanProperty(true);

    private int markerSize = 1;

    private DataSetNode style;
    private double[] xCoords = xCoordsShared;
    private double[] yCoords = yCoordsShared;
    private int coordLength = 0;

    // Since rendering is always on the JavaFX thread, we can share a single instance for
    // all live charts. Renderers can create a larger local array for supporting large datasets.
    private static double[] xCoordsShared = new double[5000];
    private static double[] yCoordsShared = new double[xCoordsShared.length];

}
