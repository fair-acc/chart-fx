package io.fair_acc.chartfx.marker;

import io.fair_acc.chartfx.utils.PropUtil;
import javafx.beans.value.ObservableObjectValue;
import javafx.scene.SnapshotParameters;
import javafx.scene.SnapshotResult;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.util.Arrays;
import java.util.Objects;

/**
 * Experimental marker that caches the result of another marker
 * as an image and then draws the image. This provides significant
 * speedup for expensive markers, but can be slower than cheap ones.
 * <p>
 * Some tested examples
 * - diamond: ~100% faster
 * - plus: ~70% faster
 * - rectangle: ~5% slower
 * - empty circle: ~20% slower
 *
 * @author ennerf
 */
public class CachedMarker implements Marker {

    public CachedMarker(ObservableObjectValue<Marker> marker) {
        this.marker = marker;
        PropUtil.runOnChange(() -> image = null, marker);
    }

    @Override
    public void draw(GraphicsContext gc, double x, double y, double size) {
        if (useCachedImage(gc, size)) {
            gc.drawImage(image, x - size, y - size, image.getWidth(), image.getHeight());
            return;
        }
        updateCache(gc, size);
        marker.get().draw(gc, x, y, size);
    }

    private boolean useCachedImage(GraphicsContext target, double size) {
        var gc = canvas.getGraphicsContext2D();
        return image != null
                && canvas.getWidth() == size * 2
                && Objects.equals(gc.getFill(), target.getFill())
                && Objects.equals(gc.getStroke(), target.getStroke())
                && Objects.equals(gc.getLineWidth(), target.getLineWidth())
                && Arrays.equals(gc.getLineDashes(), target.getLineDashes());
    }

    private void updateCache(GraphicsContext target, double size) {
        if (isUpdating) {
            return;
        }
        isUpdating = true;
        final double pixels = Math.ceil(size*2);
        canvas.setWidth(pixels);
        canvas.setHeight(pixels);
        var gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(target.getFill());
        gc.setStroke(target.getStroke());
        gc.setLineWidth(target.getLineWidth());
        gc.setLineDashes(target.getLineDashes());
        marker.get().draw(gc, size, size, size);
        canvas.snapshot((Callback<SnapshotResult, Void>) param -> {
            image = param.getImage();
            isUpdating = false;
            return null;
        }, snapshotParams, image);
    }

    private WritableImage image = null;
    private final Canvas canvas = new Canvas();
    private static final SnapshotParameters snapshotParams = new SnapshotParameters();
    static {
        snapshotParams.setFill(Color.TRANSPARENT);
    }
    boolean isUpdating = false;
    private final ObservableObjectValue<Marker> marker;

}
