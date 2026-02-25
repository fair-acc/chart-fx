package io.fair_acc.chartfx.axes.spi;

import io.fair_acc.chartfx.fxinternals.FxFontMetrics;
import io.fair_acc.chartfx.ui.css.TextStyle;
import io.fair_acc.chartfx.ui.geometry.Side;

import java.util.Objects;

/**
 * TickMark represents the label text dimension, its associated tick mark value and position along the axis for each tick.
 * For performance reasons the tick mark gets rendered into the canvas rather than being a separate node.
 *
 * @author rstein
 * @author ennerf
 */
public class TickMark {
    protected double tickValue = Double.NaN; // tick mark in data units
    protected String text = ""; // the actual label text
    protected double height = Double.NaN; // the label height in display units
    protected double width = Double.NaN; // the label width in display units

    protected double tickPosition = Double.NaN; // tick position along axis in display units
    protected boolean visible = true; // whether the tick mark should be displayed
    protected final TextStyle style;
    private long usedStyle = -1;

    /**
     * @param style a styleable node that the text can be rendered into
     */
    public TickMark(TextStyle style) {
        this.style = style;
    }

    /**
     * Updates a tick mark
     *
     * @param tickValue     numeric value of tick
     * @param tickMarkLabel string label associated with tick
     */
    public void setValue(double tickValue, String tickMarkLabel) {
        // Get size on demand
        if (!Objects.equals(tickMarkLabel, text)) {
            this.height = -1;
            this.width = -1;
        }
        this.tickValue = tickValue;
        this.text = tickMarkLabel;
    }

    protected void updateTextSize() {
        if (usedStyle != style.getChangeCounter() || height < 0) {
            if (text == null || text.isEmpty()) {
                height = 0;
                width = 0;
            } else {
                updateBounds();
            }
            usedStyle = style.getChangeCounter();
        }
    }

    protected void updateBounds() {
        // N.B. important: usage of getBoundsInParent() which also takes into account text rotations.
        //
        // Checking text bounds via a node is incredibly wasteful, so we try to use an internal API
        // that is available with appropriate jvm flags. The two results can differ by tiny amounts
        // as the width may depend on the actual character sequence (e.g. 4 followed by 3), but in
        // initial tests the diff was usually below 1px. This should not matter in practice.
        if (FxFontMetrics.isAvailable()) {
            this.height = FxFontMetrics.getLineHeight(style.getFont());
            this.width = FxFontMetrics.getWidth(style.getFont(), text);
            if (getRotation() != 0) {
                var rad = Math.toRadians(getRotation());
                var sin = Math.sin(rad);
                var cos = Math.cos(rad);
                var w = width;
                var h = height;
                this.width = (w * cos) + (h * sin) + 0.5; // account for AA
                this.height = (w * sin) + (h * cos) + 0.5; // account for AA
            }
        } else {
            style.setText(text);
            var bounds = style.getBoundsInParent();
            height = bounds.getHeight();
            width = bounds.getWidth();
        }
    }

    /**
     * @return the style applied to this tickmark
     */
    public TextStyle getStyle() {
        return style;
    }

    /**
     * @return tick mark value in data units
     */
    public double getValue() {
        return tickValue;
    }

    /**
     * @return tick mark label text
     */
    public String getText() {
        return text;
    }

    /**
     * @return the height of the tick mark including rotation etc.
     */
    public double getHeight() {
        updateTextSize();
        return height;
    }

    /**
     * @return the width of the tick mark including rotation etc.
     */
    public double getWidth() {
        updateTextSize();
        return width;
    }

    /**
     * @return value tick position along the axis in display units
     */
    public double getPosition() {
        return tickPosition;
    }

    /**
     * @param value tick position along the axis in display units
     */
    public void setPosition(final double value) {
        this.tickPosition = value;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    // ------- deprecated methods for backwards compatibility with unit tests -------

    @Deprecated
        // TODO: update tests
    TickMark(final Side side, final double tickValue, final double tickPosition, final double tickRotation,
             final String tickMarkLabel) {
        this(new TextStyle());
        setValue(tickValue, tickMarkLabel);
        this.style.setRotate(tickRotation);
        this.tickPosition = tickPosition;
    }

    public double getRotation() {
        return style.getRotate();
    }

    @Deprecated
    void setValue(double v) {
        this.tickValue = v;
    }
}
