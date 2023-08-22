package io.fair_acc.chartfx.renderer.hebi;

import io.fair_acc.chartfx.renderer.spi.AbstractRendererXY;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.profiler.AggregateDurationMeasure;
import io.fair_acc.dataset.profiler.DurationMeasure;
import io.fair_acc.dataset.profiler.Profiler;
import io.fair_acc.dataset.utils.StreamUtils;
import io.fair_acc.math.ArrayUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.fair_acc.chartfx.renderer.hebi.ChartTraceRenderer.*;
import static io.fair_acc.dataset.DataSet.*;

/**
 * Experimental renderer that uses a BufferedImage to render into a PixelBuffer
 * that gets displayed in an ImageView image.
 *
 * @author ennerf
 */
public class BufferedImageRenderer extends AbstractRendererXY<BufferedImageRenderer> {

    private void initImage(int width, int height) {
        if (img == null || img.getWidth() < width || img.getHeight() < height) {
            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            graphics = (Graphics2D) img.getGraphics();
            int[] pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
            pixelBuffer = new PixelBuffer<>(width, height, IntBuffer.wrap(pixels), PixelFormat.getIntArgbPreInstance());
            imageView.setImage(new WritableImage(pixelBuffer));
        }
        if (imageView.getViewport() == null
                || imageView.getViewport().getWidth() != width
                || imageView.getViewport().getHeight() != height) {
            imageView.setViewport(new Rectangle2D(0, 0, width, height));
        }
        graphics.setBackground(TRANSPARENT);
        graphics.clearRect(0, 0, width, height);
    }

    Color TRANSPARENT = new Color(0, true);

    @Override
    public void render() {
        int width = (int) getChart().getCanvas().getWidth();
        int height = (int) getChart().getCanvas().getHeight();
        if(width == 0 || height == 0) {
            return;
        }
        initImage(width, height);
        super.render();
        pixelBuffer.updateBuffer(buffer -> null);
    }

    int toCoord(double value) {
        return Double.isNaN(value) ? 0 : (int) value;
    }

    @Override
    protected void render(GraphicsContext unused, DataSet dataSet, DataSetNode style) {

        // check for potentially reduced data range we are supposed to plot
        final int min = Math.max(0, dataSet.getIndex(DIM_X, xMin) - 1);
        final int max = Math.min(dataSet.getIndex(DIM_X, xMax) + 2, dataSet.getDataCount()); /* excluded in the drawing */
        final int count = max - min;
        if (count <= 0) {
            return;
        }

        var gc = (Graphics2D) img.getGraphics();
        if (color == null) {
            var fxCol = (javafx.scene.paint.Color) style.getLineColor();
            color = new Color(
                    (float) fxCol.getBlue(),
                    (float) fxCol.getRed(),
                    (float) fxCol.getGreen(),
                    (float) fxCol.getOpacity());
        }
        graphics.setColor(color);

        // make sure temp array is large enough
        xCoords = xCoordsShared = ArrayUtils.resizeMin(xCoordsShared, count);
        yCoords = xCoordsShared = ArrayUtils.resizeMin(yCoordsShared, count);

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
            double prevX = x;
            double prevY = y;
            xCoords[0] = (int) x;
            yCoords[0] = (int) y;
            coordLength = 1;

            // Build contiguous non-nan segments so we can use strokePolyLine
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
                prevX = x;
                prevY = y;
                xCoords[coordLength] = (int) x;
                yCoords[coordLength] = (int) y;
                coordLength++;

            }
            benchComputeCoords.stop();

            // Draw coordinates
             if (coordLength == 1) {
                // corner case for a single point that would be skipped by strokePolyLine
                graphics.drawLine(xCoords[0], yCoords[0], xCoords[0], yCoords[0]);
            } else {
                // solid and dashed line
                benchPolyLine.start();
                graphics.drawPolyline(xCoords, yCoords, coordLength);
                benchPolyLine.stop();
            }

        }

        benchComputeCoords.reportSum();
        benchPolyLine.reportSum();

    }

    Color color;

    @Override
    public boolean drawLegendSymbol(DataSetNode style, Canvas canvas) {
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
    protected BufferedImageRenderer getThis() {
        return this;
    }

    public BufferedImageRenderer() {
        chartProperty().addListener((observable, oldChart, newChart) -> {
            if (oldChart != null) {
                oldChart.getPlotBackground().getChildren().remove(imageView);
            }
            if (newChart != null) {
                newChart.getPlotBackground().getChildren().add(imageView);
            }
        });
    }

    @Override
    public void setProfiler(Profiler profiler) {
        super.setProfiler(profiler);
        benchPolyLine = AggregateDurationMeasure.wrap(profiler.newDebugDuration("j2d-drawPolyLine"));
        benchComputeCoords = AggregateDurationMeasure.wrap(profiler.newDebugDuration("j2d-computeCoords"));
    }

    AggregateDurationMeasure benchComputeCoords = AggregateDurationMeasure.DISABLED;
    AggregateDurationMeasure benchPolyLine = AggregateDurationMeasure.DISABLED;

    final ImageView imageView = new ImageView();
    BufferedImage img;
    Graphics2D graphics;
    PixelBuffer<IntBuffer> pixelBuffer;

    private DataSetNode style;
    private int[] xCoords = xCoordsShared;
    private int[] yCoords = yCoordsShared;
    private int coordLength = 0;

    private static int[] xCoordsShared = new int[6000];
    private static int[] yCoordsShared = new int[6000];

}
