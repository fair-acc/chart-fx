package io.fair_acc.chartfx.axes.spi;

import io.fair_acc.chartfx.ui.css.TextStyle;
import io.fair_acc.chartfx.ui.css.TextStyle.TextBounds;
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
    protected final TextBounds bounds = new TextBounds();

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
            bounds.set(-1, -1);
        }
        this.tickValue = tickValue;
        this.text = tickMarkLabel;
    }

    protected void updateBounds() {
        if (usedStyle != style.getChangeCounter() || bounds.getHeight() < 0) {
            style.computeTextBounds(text, bounds);
            usedStyle = style.getChangeCounter();
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
        updateBounds();
        return bounds.getHeight();
    }

    /**
     * @return the width of the tick mark including rotation etc.
     */
    public double getWidth() {
        updateBounds();
        return bounds.getWidth();
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
