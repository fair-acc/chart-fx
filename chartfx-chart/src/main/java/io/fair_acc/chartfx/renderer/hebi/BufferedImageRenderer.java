package io.fair_acc.chartfx.renderer.hebi;

import io.fair_acc.chartfx.renderer.spi.AbstractRendererXY;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.dataset.DataSet;
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
        var x = toCoord(xAxis.getDisplayPosition(dataSet.get(DIM_X, min)));
        var y = toCoord(yAxis.getDisplayPosition(dataSet.get(DIM_Y, min)));

        var prevX = xCoords[0] = x;
        var prevY = yCoords[0] = y;
        coordLength = 1;
        for (int i = min + 1; i < max; i++) {
            x = toCoord(xAxis.getDisplayPosition(dataSet.get(DIM_X, i)));
            y = toCoord(yAxis.getDisplayPosition(dataSet.get(DataSet.DIM_Y, i)));

            // Reduce points if they don't provide any benefits
            if (x == prevX && y == prevY) {
                continue;
            }

            // Add point
            xCoords[coordLength] = prevX = x;
            yCoords[coordLength] = prevY = y;
            coordLength++;
        }


        // draw polyline
        var gc = (Graphics2D) img.getGraphics();
        if (color == null) {
            var fxCol = (javafx.scene.paint.Color) style.getLineColor();
            color = new Color(
                    (float) fxCol.getBlue(),
                    (float) fxCol.getRed(),
                    (float) fxCol.getGreen());
        }
        gc.setColor(color);
        gc.drawPolyline(xCoords, yCoords, coordLength);

       /* double x0 = xCoords[0];
        double y0 = yCoords[0];
        for (int i = 1; i < coordLength; i++) {
            double x1 = xCoords[i];
            double y1 = yCoords[i];
            if (Double.isFinite(x0 + x1 + y0 + y1)) {
                gc.strokeLine(x0, y0, x1, y1);
            }
            x0 = x1;
            y0 = y1;
        }*/

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
