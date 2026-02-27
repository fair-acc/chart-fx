package io.fair_acc.chartfx.axes.spi;

import io.fair_acc.chartfx.ui.css.StyleUtil;
import io.fair_acc.chartfx.ui.css.TextStyle;
import io.fair_acc.chartfx.ui.css.TextStyle.TextBounds;
import io.fair_acc.chartfx.utils.PropUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.css.StyleOrigin;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Transform;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Pre-determined sizes and images for rendering text
 *
 * @author ennerf
 */
public class GlyphAtlas {

    public GlyphAtlas(TextStyle style) {
        this.style = style;
        PropUtil.initAndRunOnChange(() -> needsScaling.set(style.getRotate() % 90 != 0), style.rotateProperty());
        var listener = StyleUtil.incrementOnChange(invalidCounter);
        listener.accept(needsScaling);
        listener.accept(style.fontProperty());
        listener.accept(style.opacityProperty());
        listener.accept(style.fillProperty());
        listener.accept(style.strokeProperty());
        listener.accept(style.strokeWidthProperty());
        listener.accept(style.fontSmoothingTypeProperty());
        PropUtil.initAndRunOnChange(() -> valid = false, invalidCounter);
    }

    protected static class GlyphRegion {
        double atlasX, atlasY, atlasW, atlasH;
        double visualWidth, visualHeight;
    }

    /**
     * @return false if the input can't be mapped
     */
    public boolean computeLayoutBounds(CharSequence chars, TextBounds result) {
        // Check for various features that are currently not supported
        ensureValid();
        double height = 0;
        double width = 0;
        final int n = chars.length();
        for (int i = 0; i < n; i++) {
            final char c = chars.charAt(i);
            if (c < MIN_CHAR || c > MAX_CHAR) {
                return false;
            }
            final var glyph = glyphMap[c];
            height = Math.max(height, glyph.visualHeight);
            width += glyph.visualWidth;
        }
        result.set(width, height);
        return true;
    }

    public boolean tryFillText(GraphicsContext gc, CharSequence text, double x, double y) {
        if (!computeLayoutBounds(text, bounds)) {
            return false;
        }

        final boolean smoothing = gc.isImageSmoothing();
        gc.setImageSmoothing(style.getRotate() % 90 != 0);

        // coordinates are at the center, but we write from the left
        double curX = x - bounds.getWidth() / 2;
        double curY = y - bounds.getHeight() / 2;
        final int n = text.length();
        for (int i = 0; i < n; i++) {
            final char c = text.charAt(i);
            final var glyph = glyphMap[c];

            // Note: the starting pixels need to be at a pixel boundary,
            // and it is important that source and destination have the
            // same exact size. Not applying rounding to the width/height
            // should work for both scaled and unscaled cases as the ratio
            // remains the same.
            gc.drawImage(atlas,
                    glyph.atlasX, glyph.atlasY,
                    glyph.atlasW, glyph.atlasH,
                    Math.ceil(curX), Math.round(curY),
                    glyph.visualWidth, glyph.visualHeight
            );
            curX += glyph.visualWidth;
        }

        gc.setImageSmoothing(smoothing);
        return true;
    }

    protected void ensureValid() {
        if (valid) return;
        String prevText = style.getText();
        try {
            bake();
        } finally {
            style.setText(prevText);
        }
        valid = true;
    }

    protected void bake() {
        double currentX = minPadding;
        double maxHeight = 0;

        // A 1 to 1 pixel mapping is best, but we need more
        // resolution for subpixel operations
        final double scale = (screenScale == 1 && style.getRotate() == 0) ? 1 : 2 * screenScale;

        // Determine char sizes
        for (char c = MIN_CHAR; c <= MAX_CHAR; c++) {
            currentX = padToAlignment(currentX);

            style.setText(String.valueOf(c));
            var bounds = style.getLayoutBounds();
            var pxWidth = Math.ceil(bounds.getWidth());
            var pxHeight = Math.ceil(bounds.getHeight());
            maxHeight = Math.max(maxHeight, pxHeight);

            // Preliminary coordinates
            var coords = new GlyphRegion();
            coords.atlasX = currentX;
            coords.atlasY = minPadding;
            coords.atlasW = bounds.getWidth();
            coords.atlasH = bounds.getHeight();
            coords.visualWidth = bounds.getWidth();
            coords.visualHeight = bounds.getHeight();
            glyphMap[c] = coords;

            // leave one letter distance
            currentX += Math.max(minPadding, pxWidth);
            currentX = padToAlignment(currentX);
        }
        double height = padToAlignment(2 * minPadding + maxHeight);

        // Create atlas
        var canvas = new Canvas(scale * currentX, scale * height);
        var gc = canvas.getGraphicsContext2D();
        style.copyStyleTo(gc);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.scale(scale, scale); // better quality than scaling the snapshot
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);

        for (char c = MIN_CHAR; c <= MAX_CHAR; c++) {
            var coords = glyphMap[c];
            gc.fillText(String.valueOf(c), coords.atlasX, coords.atlasY);
            if (!Objects.equals(style.getStroke(), Color.TRANSPARENT)) {
                // Note: might look odd, but enable at will
                gc.strokeText(String.valueOf(c), coords.atlasX, coords.atlasY);
            }
            coords.atlasX = Math.floor(coords.atlasX * scale);
            coords.atlasY = Math.floor(coords.atlasY * scale);
            coords.atlasW = coords.atlasW * scale;
            coords.atlasH = coords.atlasH * scale;
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        atlas = canvas.snapshot(params, null);

        if (showDebug) {

            var showCanvas = new Stage();
            showCanvas.setScene(new Scene(new VBox(canvas)));
            showCanvas.setTitle(canvas.getWidth() + "x" + canvas.getHeight());
            showCanvas.show();

            var showImage = new Stage();
            showImage.setScene(new Scene(new VBox(new ImageView(atlas))));
            showImage.setTitle(atlas.getWidth() + "x" + atlas.getHeight());
            showImage.show();

            showDebug = false;
        }

    }

    private static boolean showDebug = false;

    protected double padToAlignment(double currentX) {
        return Math.ceil(currentX / alignment) * alignment;
    }

    protected final TextStyle style;
    protected boolean valid = false;

    private Image atlas;
    private final GlyphRegion[] glyphMap = new GlyphRegion[128];
    private final TextBounds bounds = new TextBounds();
    private static final int MIN_CHAR = 32;
    private static final int MAX_CHAR = 126;
    private final double screenScale = Screen.getPrimary().getOutputScaleX();
    private final double minPadding = 8;
    private static final double alignment = 8;
    private final BooleanProperty needsScaling = new SimpleBooleanProperty(this, "needsScaling", false);
    private final LongProperty invalidCounter = new SimpleLongProperty(this, "invalidCounter", 0);

}
