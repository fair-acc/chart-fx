package io.fair_acc.chartfx.plugins;

import javafx.beans.property.DoubleProperty;

import io.fair_acc.chartfx.axes.Axis;

public interface ValueIndicator {
    /**
     * returns the Axis that this indicator is associated with.
     *
     * @return associated Axis
     */
    Axis getAxis();

    /**
     * @return text displayed within or next to the indicator
     */
    String getText();

    /**
     * Returns the indicated value.
     *
     * @return indicated value
     */
    double getValue();

    /**
     *
     * @param text the new text. If {@code null}, the label will be hidden.
     */
    void setText(final String text);

    /**
     * Sets the value that should be indicated.
     *
     * @param newValue value to be indicated
     */
    void setValue(double newValue);

    /**
     * Value indicated by this plugin.
     *
     * @return value property
     */
    DoubleProperty valueProperty();
}