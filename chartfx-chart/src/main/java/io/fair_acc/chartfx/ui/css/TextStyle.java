package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.axes.spi.GlyphAtlas;
import io.fair_acc.chartfx.fxinternals.FxFontMetrics;
import io.fair_acc.chartfx.utils.PropUtil;
import javafx.beans.property.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.Objects;

/**
 * @author ennerf
 */
public class TextStyle extends Text implements StyleUtil.StyleNode {

    public TextStyle(String... styles) {
        StyleUtil.styleNode(this, styles);
        var incrementCounter = StyleUtil.incrementOnChange(changeCounter);
        StyleUtil.forEachStyleProp(this, incrementCounter);
        incrementCounter.accept(glyphAtlasEnabled);
        changeCounter.addListener(observable -> boundsValid = false);
        textProperty().addListener(observable -> boundsValid = false);
    }

    public static class TextBounds {

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }

        public void set(double width, double height) {
            this.width = width;
            this.height = height;
        }

        protected double width = Double.NaN;
        protected double height = Double.NaN;

    }

    private final TextBounds bounds = new TextBounds();
    private boolean boundsValid = false;

    public TextBounds getCachedBoundsInParent() {
        if (!boundsValid) {
            computeTextBounds(getText(), bounds);
            boundsValid = true;
        }
        return bounds;
    }

    /**
     * @param chars content
     * @param result bounds of the text including rotation
     */
    public TextBounds computeTextBounds(CharSequence chars, TextBounds result) {
        if (chars == null || chars.isEmpty()) {
            result.width = 0;
            result.height = 0;
            return result;
        }

        // Checking text bounds via a node is incredibly wasteful, so we try to use an internal API
        // that is available with appropriate jvm flags. The two results can differ by tiny amounts
        // as the width may depend on the actual character sequence (e.g. 4 followed by 3). In our
        // tests the diff was generally within 1px, so this should not matter in practice.
        var text = chars.toString();
        final double w, h;
        if (isGlyphAtlasEnabled() && getGlyphAtlas().computeLayoutBounds(chars, result)) {
            w = result.getWidth();
            h = result.getHeight();
        } else if (FxFontMetrics.isAvailable()) {
            h = FxFontMetrics.getLineHeight(getFont()) * countLines(chars);
            w = FxFontMetrics.getWidth(getFont(), chars);
        } else {
            setTextIfChanged(text);
            var bounds = getLayoutBounds();
            h = bounds.getHeight();
            w = bounds.getWidth();
        }

        // Account for rotation
        if (getRotate() == 0) {
            result.width = w;
            result.height = h;
        } else {
            var rad = Math.toRadians(getRotate());
            var sin = Math.abs(Math.sin(rad));
            var cos = Math.abs(Math.cos(rad));
            result.height = (w * sin) + (h * cos);
            result.width = (w * cos) + (h * sin);
        }

        if (DEBUG) {
            // Note: getBoundsInParent() takes into account the rotation,
            // but it's a more expensive call.
            setTextIfChanged(text);
            var parent = getBoundsInParent();
            var hp = parent.getHeight();
            var wp = parent.getWidth();

            System.out.printf("[Label dimensions] Text: '%s' | Rotation: %.1f°%n" +
                              "  Manual:  W: %8.4f | H: %8.4f%n" +
                              "  Layout:  W: %8.4f | H: %8.4f%n" +
                              "  Delta:   W: %8.4f | H: %8.4f%n",
                    text, getRotate(),
                    result.width, result.height,
                    wp, hp,
                    Math.abs(result.width - wp), Math.abs(result.height - hp));
        }

        return result;
    }

    /**
     * Renders text at the current context location. Rotations are handled
     * internally.
     *
     * @param gc
     * @param text
     */
    public void renderText(GraphicsContext gc, CharSequence text) {
        if (getRotate() != 0) {
            gc.rotate(getRotate());
        }

        renderTextRotated(gc, text);

        if (getRotate() != 0) {
            gc.rotate(-getRotate());
        }
    }

    private void renderTextRotated(GraphicsContext gc, CharSequence text) {
        // Optional fast path using copied images
        if (isGlyphAtlasEnabled() && getGlyphAtlas().tryFillText(gc, text, 0, 0)) {
            return;
        }

        // Fallback using standard text rendering
        String string = text.toString();
        gc.fillText(string, 0, 0);
        if (!Objects.equals(gc.getStroke(), Color.TRANSPARENT)) {
            gc.strokeText(string, 0, 0);
        }
    }

    /**
     * same as setText, but avoids invalidating caches if the strings are the same
     */
    public void setTextIfChanged(String text) {
        if (!PropUtil.isEqual(text, getText())) {
            super.setText(text);
        }
    }

    private int countLines(CharSequence chars) {
        int lines = 1;
        final int n = chars.length();
        for (int i = 0; i < n; i++) {
            if (chars.charAt(i) == '\n') lines++;
        }
        return lines;
    }

    private static final boolean DEBUG = false;

    protected void incrementChangeCounter() {
        changeCounter.set(changeCounter.get() + 1);
    }

    @Override
    public String toString() {
        return StyleUtil.toStyleString(this);
    }

    public ReadOnlyLongProperty changeCounterProperty() {
        return changeCounter;
    }

    protected GlyphAtlas getGlyphAtlas() {
        if(glyphAtlas == null) {
            glyphAtlas = new GlyphAtlas(this);
        }
        return glyphAtlas;
    }

    public boolean isGlyphAtlasEnabled() {
        return glyphAtlasEnabled.get();
    }

    public void setGlyphAtlasEnabled(boolean glyphAtlasEnabled) {
        this.glyphAtlasEnabled.set(glyphAtlasEnabled);
    }

    public BooleanProperty glyphAtlasEnabledProperty() {
        return glyphAtlasEnabled;
    }

    private final LongProperty changeCounter = new SimpleLongProperty(0);
    protected final BooleanProperty glyphAtlasEnabled = new SimpleBooleanProperty(false);
    protected GlyphAtlas glyphAtlas;
}
