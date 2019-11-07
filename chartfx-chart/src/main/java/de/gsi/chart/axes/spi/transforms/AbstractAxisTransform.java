package de.gsi.chart.axes.spi.transforms;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisTransform;

/**
 * @author rstein
 */
public abstract class AbstractAxisTransform implements AxisTransform {

    protected Axis axis;
    protected double rangeMin = -Double.MAX_VALUE;
    protected double rangeMax = +Double.MAX_VALUE;

    protected AbstractAxisTransform(final Axis axis) {
        this.axis = axis;
        axis.minProperty().addListener((ch, o, n) -> rangeMin = n.doubleValue());
        axis.maxProperty().addListener((ch, o, n) -> rangeMax = n.doubleValue());

    }

    @Override
    public double getMaximumRange() {
        return rangeMax;
    }

    @Override
    public double getMinimumRange() {
        return rangeMin;
    }

    @Override
    public void setMaximumRange(final double val) {
        axis.maxProperty().set(val);
    }

    @Override
    public void setMinimumRange(final double val) {
        axis.minProperty().set(val);
    }
}
