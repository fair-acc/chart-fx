package de.gsi.chart.renderer.spi.utils;

import java.util.WeakHashMap;

import de.gsi.dataset.utils.AssertUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;

public final class FillPatternStyleHelper {
    private static final int HATCH_WINDOW_SIZE = 8;
    private static int hatchSpacing = 10;
    private static int hatchAngle = 45;

    protected static WeakHashMap<Color, ImagePattern> defaultHatchCache = new WeakHashMap<>();

    protected static WeakHashMap<Paint, WeakHashMap<Double, Image>> defaultHatchCacheWithStrokeWidth = new WeakHashMap<>();

    private static Image createDefaultHatch(final Paint color, final double strokeWidth) {
        WeakHashMap<Double, Image> checkCache = FillPatternStyleHelper.defaultHatchCacheWithStrokeWidth.get(color);
        if (checkCache != null) {
            final Image val = checkCache.get(Double.valueOf(strokeWidth));
            if (val != null) {
                // found existing Image with given parameter
                return val;
            }
        }
        // need to recompute hatch pattern image

        final Pane pane = new Pane();
        pane.setPrefSize(10, 10);
        final Line fw = new Line(-5, -5, 25, 25);
        final Line bw = new Line(-5, 25, 25, -5);
        fw.setSmooth(false);
        bw.setSmooth(false);
        fw.setStroke(color);
        bw.setStroke(color);
        fw.setStrokeWidth(strokeWidth);
        bw.setStrokeWidth(strokeWidth);
        pane.getChildren().addAll(fw, bw);

        pane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0)");
        final Scene scene = new Scene(pane);
        scene.setFill(Color.TRANSPARENT);
        final Image retVal = pane.snapshot(null, null);
        // add retVal to cache
        if (checkCache == null) {
            final WeakHashMap<Double, Image> temp = new WeakHashMap<>();
            temp.put(Double.valueOf(strokeWidth), retVal);
            FillPatternStyleHelper.defaultHatchCacheWithStrokeWidth.put(color, temp);
            // checkCache = new WeakHashMap<>();
        } else {
            checkCache.put(Double.valueOf(strokeWidth), retVal);
        }

        return retVal;
    }

    private static Image createHatch(final FillPattern fillPattern, final Paint color, final double strokeWidth) {
        final Pane pane = new Pane();
        pane.setPrefSize(FillPatternStyleHelper.HATCH_WINDOW_SIZE, FillPatternStyleHelper.HATCH_WINDOW_SIZE);
        pane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0)");

        final int hatchAngle = 45;
        final int windowSize = 5 * FillPatternStyleHelper.HATCH_WINDOW_SIZE;
        FillPatternStyleHelper.drawHatching(pane, fillPattern, -windowSize, -windowSize, windowSize, windowSize, color,
                strokeWidth, hatchAngle, FillPatternStyleHelper.hatchSpacing);

        final Scene scene = new Scene(pane);
        scene.setFill(Color.TRANSPARENT);
        return pane.snapshot(null, null);
    }

    /**
     * Draws hatching within the (minx,miny) to (maxx,maxy) specified box. This call should be preceded by an
     * appropriate clipping range to limit the hatching to a given surface area
     *
     * @param pane the pane the prototype hatch should be drawn in
     * @param fillPattern requested hatching
     * @param minx the minimum X coordinate
     * @param miny the maximum X coordinate
     * @param maxx the minimum Y coordinate
     * @param maxy the maximum Y coordinate
     * @param color hatching color
     * @param strokeWidth hatching stroke width
     * @param setHatchAngle hatching angle
     * @param hatchSpacing hatch spacing
     */
    private static void drawHatching(final Pane pane, final FillPattern fillPattern, final int minx, final int miny,
            final int maxx, final int maxy, final Paint color, final double strokeWidth, final int setHatchAngle,
            final int hatchSpacing) {
        // set default hatch stroke attributes //
        int hatchAngle = setHatchAngle;
        switch (fillPattern) {
        case HATCH0:
            hatchAngle = 0;
            break;
        case HATCH30:
            hatchAngle = 30;
            break;
        case HATCH45:
        case HATCHCROSS1:
            hatchAngle = 45;
            break;
        case HATCH60:
            hatchAngle = 60;
            break;
        case HATCH90:
        case HATCHCROSS2:
            hatchAngle = 90;
            break;
        default:
        }

        if (fillPattern != FillPattern.HATCHCROSS1 && fillPattern != FillPattern.HATCHCROSS2) {
            int step = hatchSpacing;
            final int diff = (maxy - miny) / 2;
            final int center = miny + diff;
            if (hatchAngle % 180 != 0 && (hatchAngle + 90) % 180 != 0) {
                final double sin = Math.sin(Math.PI * hatchAngle / 180.0);
                final double cos = Math.cos(Math.PI * hatchAngle / 180.0);
                step = (int) (hatchSpacing / cos);
                final int dx = (int) (diff / cos);
                final int dy = (int) (diff / sin);
                for (int i = minx - dx; i < maxx + dx; i += step) {
                    final int x1 = i - dx;
                    final int x2 = i + dx;
                    final int y1 = center + dy;
                    final int y2 = center - dy;
                    final Line line = new Line(x1, y1, x2, y2);
                    line.setStroke(color);
                    line.setStrokeWidth(strokeWidth);
                    pane.getChildren().add(line);
                }
            } else if (hatchAngle % 180 != 0) {
                final int y1 = center + diff;
                final int y2 = center - diff;
                for (int i = minx; i < maxx; i += step) {
                    final Line line = new Line(i, y1, i, y2);
                    line.setStroke(color);
                    line.setStrokeWidth(strokeWidth);
                    pane.getChildren().add(line);
                }
            } else {
                final int x1 = minx;
                final int x2 = maxx;
                for (int i = miny; i < maxy; i += step) {
                    final Line line = new Line(x1, i, x2, i);
                    line.setStroke(color);
                    line.setStrokeWidth(strokeWidth);
                    pane.getChildren().add(line);
                }
            }
        } else if (fillPattern == FillPattern.HATCHCROSS1) {
            hatchAngle = 45;
            final int diff = (maxy - miny) / 2;
            final int center = miny + diff;
            final double sin = Math.sin(Math.PI * hatchAngle / 180.0);
            final double cos = Math.cos(Math.PI * hatchAngle / 180.0);
            final int step = (int) (hatchSpacing / cos);
            final int dx = (int) (diff / cos);
            final int dy = (int) (diff / sin);
            for (int i = minx - dx; i < maxx + dx; i += step) {
                Line line = new Line(i - dx, center + dy, i + dx, center - dy);
                line.setStroke(color);
                line.setStrokeWidth(strokeWidth);
                pane.getChildren().add(line);
                line = new Line(i + dx, center + dy, i - dx, center - dy);
                line.setStroke(color);
                line.setStrokeWidth(strokeWidth);
                pane.getChildren().add(line);
            }
        } else {
            hatchAngle = 0;
            final int step = hatchSpacing;
            final int diff = (maxy - miny) / 2;
            final int center = miny + diff;

            final int y1 = center + diff;
            final int y2 = center - diff;
            for (int i = minx; i < maxx; i += step) {
                final Line line = new Line(i, y1, i, y2);
                line.setStroke(color);
                line.setStrokeWidth(strokeWidth);
            }

            final int x1 = minx;
            final int x2 = maxx;
            for (int i = miny; i < maxy; i += step) {
                final Line line = new Line(x1, i, x2, i);
                line.setStroke(color);
                line.setStrokeWidth(strokeWidth);
            }
        }
    }

    public static ImagePattern getDefaultHatch(final Color color) {
        ImagePattern retVal = FillPatternStyleHelper.defaultHatchCache.get(color);
        if (retVal == null) {
            retVal = FillPatternStyleHelper.getDefaultHatch(color, 0.0);
            FillPatternStyleHelper.defaultHatchCache.put(color, retVal);
        }
        return retVal;
    }

    public static ImagePattern getDefaultHatch(final Paint color, final double xOffset) {
        return new ImagePattern(FillPatternStyleHelper.createDefaultHatch(color, 1.0), xOffset, xOffset,
                FillPatternStyleHelper.HATCH_WINDOW_SIZE, FillPatternStyleHelper.HATCH_WINDOW_SIZE, false);
    }

    public static int getDefaultHatchAngle() {
        return FillPatternStyleHelper.hatchAngle;
    }

    public static int getDefaultHatchSpacing() {
        return FillPatternStyleHelper.hatchSpacing;
    }

    public static ImagePattern getHatch(final FillPattern fillPattern, final Paint color) {
        return FillPatternStyleHelper.getHatch(fillPattern, color, 1.0);
    }

    public static ImagePattern getHatch(final FillPattern fillPattern, final Paint color, final double width) {
        final Image hatch = FillPatternStyleHelper.createHatch(fillPattern, color, width);
        return new ImagePattern(hatch, 0, 0, FillPatternStyleHelper.HATCH_WINDOW_SIZE, FillPatternStyleHelper.HATCH_WINDOW_SIZE,
                false);
    }

    public static void setDefaultHatchAngle(final int angle) {
        AssertUtils.gtEqThanZero("setDefaultHatchSpacing", angle);
        FillPatternStyleHelper.hatchAngle = angle;
    }

    public static void setDefaultHatchSpacing(final int spacing) {
        AssertUtils.gtEqThanZero("setDefaultHatchSpacing", spacing);
        FillPatternStyleHelper.hatchSpacing = spacing;
    }

    public enum FillPattern {
        SOLID, HATCH, HATCH0, HATCH30, HATCH45, HATCH60, HATCH90, HATCHCROSS1, HATCHCROSS2,
    }

}
