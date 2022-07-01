package io.fair_acc.chartfx.axes;

import java.util.List;

import io.fair_acc.chartfx.axes.spi.format.DefaultTickUnitSupplier;
import javafx.beans.property.ObjectProperty;

/**
 * @author rstein
 */
public interface AxisLabelFormatter {
    // N.B. needs to be an abstract class since there is no interface definition
    // for StringConverter

    /**
     * Converts the string provided into an object defined by the specific converter. Format of the string and type of
     * the resulting object is defined by the specific converter.
     *
     * @param string the string to be converted back into a number
     * @return an object representation of the string passed in.
     */
    Number fromString(String string);

    /**
     * Returns the value of the {@link #tickUnitSupplierProperty()}.
     *
     * @return the TickUnitSupplier
     */
    TickUnitSupplier getTickUnitSupplier();

    /**
     * Sets the value of the {@link #tickUnitSupplierProperty()}.
     *
     * @param supplier the tick unit supplier. If {@code null}, the default one will be used
     */
    void setTickUnitSupplier(final TickUnitSupplier supplier);

    /**
     * Strategy to compute major tick unit when auto-range is on or when axis bounds change. By default initialised to
     * {@link DefaultTickUnitSupplier}.
     * <p>
     * See {@link TickUnitSupplier} for more information about the expected behaviour of the strategy.
     * </p>
     *
     * @return tickUnitSupplier property
     */
    ObjectProperty<TickUnitSupplier> tickUnitSupplierProperty();

    /**
     * Converts the object provided into its string form. Format of the returned string is defined by the specific
     * converter.
     *
     * @param val the number to be converted
     * @return a string representation of the object passed in.
     */
    String toString(Number val);

    /**
     * Called just before new TickMarks are computed
     * 
     * @param newMajorTickMarks for which the labels should be computed
     * @param unitScaling scaling applied to the raw data set units
     */
    void updateFormatter(List<Double> newMajorTickMarks, double unitScaling);

}
