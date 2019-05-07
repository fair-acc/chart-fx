package de.gsi.chart.axes.spi;

import javafx.scene.text.Text;

/**
 * TickMark represents the label text, its associated tick mark value and
 * position along the axis for each tick.
 *
 * @author rstein
 */
public class TickMark extends Text {
    private Double tickValue; // tick mark in data units
    private double tickPosition; // tick position along axis in display units

    /**
     * Creates and initialises an instance of TickMark.
     */
    public TickMark(final double tickValue, final double tickPosition, final String tickMarkLabel) {
        super();
        this.tickValue = tickValue;
        this.tickPosition = tickPosition;
        setText(tickMarkLabel);
    }

    /**
     * @param newValue
     *            tick mark value in data units
     */
    public void setValue(final Double newValue) {
        tickValue = newValue;
    }

    /**
     * @return tick mark value in data units
     */
    public Double getValue() {
        return tickValue;
    }

    /**
     * @param value
     *            tick position along the axis in display units
     */
    public void setPosition(final double value) {
        tickPosition = value;
    }

    /**
     * @return value tick position along the axis in display units
     */
    public Double getPosition() {
        return tickPosition;
    }

    /**
     * @return the width of the tick mark including rotation etc.
     */
    public double getWidth() {
        // N.B. important: usage of getBoundsInParent() which also takes into
        // account text rotations
        return getBoundsInParent().getWidth();
    }

    /**
     * @return the height of the tick mark including rotation etc.
     */
    public double getHeight() {
        // N.B. important: usage of getBoundsInParent() which also takes into
        // account text rotations
        return getBoundsInParent().getHeight();
    }
}
