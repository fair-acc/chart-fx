package io.fair_acc.chartfx.renderer.spi.marchingsquares;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;

/**
 * @author rstein
 */
public class GeneralPath /* extends Path2D */ {
    private final List<Segment> drawList = new ArrayList<>();

    GeneralPath() {
        // super(Path2D.WIND_EVEN_ODD);
        beginPath();
    }

    public final void beginPath() {
        drawList.add(new Segment(PathType.GC_BEGINPATH, 0, 0));
    }

    public void closePath() {
        drawList.add(new Segment(PathType.GC_CLOSEPATH, 0, 0));
    }

    public int draw(final GraphicsContext gc) {
        int count = 0;
        for (final Segment segment : drawList) {
            switch (segment.type) {
            case GC_LINETO:
                gc.lineTo(segment.x, segment.y);
                count++;
                break;
            case GC_MOVETO:
                gc.moveTo(segment.x, segment.y);
                count++;
                break;
            case GC_BEGINPATH:
                gc.beginPath();
                break;
            case GC_CLOSEPATH:
                gc.closePath();
                gc.stroke();
                break;
            default:
                break;
            }
        }
        return count;
    }

    public void lineTo(final float x, final float y) {
        drawList.add(new Segment(PathType.GC_LINETO, x, y));
    }

    public void moveTo(final float x, final float y) {
        drawList.add(new Segment(PathType.GC_MOVETO, x, y));
    }

    public int size() {
        return drawList.size();
    }

    enum PathType {
        GC_LINETO,
        GC_MOVETO,
        GC_BEGINPATH,
        GC_CLOSEPATH
    }

    private static class Segment {
        public double x;
        public double y;
        public PathType type;

        Segment(final PathType type, final double x, final double y) {
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }
}
