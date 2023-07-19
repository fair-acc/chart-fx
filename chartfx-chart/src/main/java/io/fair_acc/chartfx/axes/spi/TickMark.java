package io.fair_acc.chartfx.axes.spi;

import io.fair_acc.chartfx.ui.css.TextStyle;
import javafx.geometry.VPos;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

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

    private double tickValue = Double.NaN; // tick mark in data units
    private String text = ""; // the actual label text
    private double height = Double.NaN; // the label height in display units
    private double width = Double.NaN; // the label width in display units

    private double tickPosition = Double.NaN; // tick position along axis in display units
    private boolean visible = false; // whether the tick mark should be displayed
    private final TextStyle style;
    private long usedStyle = -1;

    /**
     * @param style a styleable node that the text can be rendered into
     */
    public TickMark(TextStyle style) {
        this.style = style;
    }

    /**
     * Updates a tick mark
     * @param tickValue numeric value of tick
     * @param tickMarkLabel string label associated with tick
     */
    public void setValue(double tickValue, String tickMarkLabel) {
        // Get size on demand
        if(!Objects.equals(tickMarkLabel, text)) {
            this.height = -1;
            this.width = -1;
        }
        this.tickValue = tickValue;
        this.text = tickMarkLabel;
    }

    private void updateTextSize() {
        if(usedStyle != style.getChangeCounter() || height < 0) {
            if (text == null || text.isEmpty()) {
                height = 0;
                width = 0;
            } else {
                // N.B. important: usage of getBoundsInParent() which also takes into
                // account text rotations
                style.setText(text);
                var bounds = style.getBoundsInParent();
                height = bounds.getHeight();
                width = bounds.getWidth();
            }
            usedStyle = style.getChangeCounter();
        }
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

}
