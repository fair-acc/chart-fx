package de.gsi.chart.renderer.spi.utils;

import java.util.Arrays;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;

/**
 * Workaround for dashed line JavaFX performance bottleneck for strictly horizontal and vertical lines as used in the
 * GridRenderer
 * 
 * see e.g. option 3 in:
 * https://stackoverflow.com/questions/47102734/performances-issue-when-drawing-dashed-line-in-java/47166129#47166129
 *
 * @author rstein
 *
 */
public final class DashPatternStyle { // NOPMD nomen est omen

    private static final WeakHashMap<Integer, ImagePattern> dashHashMap = new WeakHashMap<>();

    private DashPatternStyle() {
        // empty definition for utility class
    }

    private static Integer computeHash(final Paint color, final double strokeWidth, final boolean isHorizontal,
            final double[] pattern) {
        int hash = 7;
        hash = 31 * hash + color.hashCode();
        hash = 31 * hash + Double.hashCode(strokeWidth);
        hash = 31 * hash + Boolean.hashCode(isHorizontal);

        if (pattern == null) {
            hash = 31 * hash + 3141;
        } else {
            hash = 31 * hash + Arrays.hashCode(pattern);
        }

        return hash;
    }

    private static ImagePattern createDefaultHatch(final Paint color, final double strokeWidth,
            final boolean isHorizontal, final double[] pattern) {
        final Integer hash = computeHash(color, strokeWidth, isHorizontal, pattern);

        return DashPatternStyle.dashHashMap.computeIfAbsent(hash, t -> {
            // need to recompute hatch pattern image
            final double dashPatternLength = getPatternLength(pattern);
            final double width = isHorizontal ? dashPatternLength : strokeWidth;
            final double height = isHorizontal ? strokeWidth : dashPatternLength;
            final double middle = (int) (strokeWidth / 2.0);

            final Pane pane = new Pane();
            pane.setPrefSize(width, height);
            final Line fw = isHorizontal ? new Line(0, middle, dashPatternLength, middle)
                                         : new Line(middle, 0, middle, dashPatternLength);

            fw.setSmooth(false);
            fw.setStroke(color);
            if (pattern == null) {
                fw.getStrokeDashArray().setAll(dashPatternLength);
            } else {
                fw.getStrokeDashArray().setAll(DoubleStream.of(pattern).boxed().collect(Collectors.toList()));
            }
            fw.setStrokeWidth(strokeWidth);

            pane.getChildren().addAll(fw);
            pane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0)");
            final Scene scene = new Scene(pane);
            scene.setFill(Color.TRANSPARENT);

            final Image hatch = pane.snapshot(null, null);

            return new ImagePattern(hatch, width, 0, width, height, false);
        });
    }

    private static double getPatternLength(final double[] pattern) {
        if (pattern == null || pattern.length <= 1) {
            return 1.0;
        }
        double ret = 0;
        for (int i = 0; i < pattern.length; i++) { // NOPMD for loop is faster than forEach
            ret += pattern[i];
        }
        return ret;
    }

    public static void strokeDashedLine(final GraphicsContext gc, final double x0, final double y0, final double x1,
            final double y1) {
        final Paint color = gc.getStroke();
        final double strokeWidth = Math.max(gc.getLineWidth(), 1);
        final double strokeWidthHalf = strokeWidth / 2.0;
        final double[] pattern = gc.getLineDashes();
        final double width = Math.abs(x1 - x0);
        final double height = Math.abs(y1 - y0);
        final boolean isHorizontal = width > height;

        gc.setFill(DashPatternStyle.createDefaultHatch(color, strokeWidth, isHorizontal, pattern));
        if (isHorizontal) {
            gc.fillRect(x0, y0 - strokeWidthHalf, width, strokeWidth);
        } else {
            gc.fillRect(x0 - strokeWidthHalf, y0, strokeWidth, height);
        }
    }
}
