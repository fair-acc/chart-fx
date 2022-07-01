package io.fair_acc.chartfx.axes.spi.transforms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.axes.Axis;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * @author rstein
 */
public class LogarithmicAxisTransform extends AbstractAxisTransform {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogarithmicAxisTransform.class);
    public static final double DEFAULT_LOGARITHM_BASE = 10;
    public static final double DEFAULT_LOG_MIN_VALUE = 1e-6;
    public static final double DB20_LOGARITHM_BASE = Math.exp(Math.log10(10) / 20.0);
    public static final double DB10_LOGARITHM_BASE = Math.exp(Math.log10(10) / 10.0);

    // cached values -- for performance reasons
    private double logarithmBaseCache = LogarithmicAxisTransform.DEFAULT_LOGARITHM_BASE;
    private double logBaseCache = Math.log10(logarithmBaseCache);

    private final DoubleProperty logarithmBase = new SimpleDoubleProperty(this, "logarithmBase", logarithmBaseCache) {
        @Override
        protected void invalidated() {
            if (get() <= 1) {
                throw new IllegalArgumentException("logarithmBase must be grater than 1");
            }
            logarithmBaseCache = get();
            logBaseCache = Math.log10(logarithmBaseCache);
        }
    };

    public LogarithmicAxisTransform(final Axis axis) {
        super(axis);
        rangeMin = LogarithmicAxisTransform.DEFAULT_LOG_MIN_VALUE;
    }

    @Override
    public double backward(final double val) {
        return pow(val);
    }

    @Override
    public double forward(final double val) {
        return log(val);
    }

    /**
     * Returns the value of the {@link #logarithmBaseProperty()}.
     *
     * @return base of the logarithm
     */
    public double getLogarithmBase() {
        return logarithmBase.get();
    }

    @Override
    public double getRoundedMaximumRange(final double max) {
        return backward(Math.ceil(forward(max)));
    }

    @Override
    public double getRoundedMinimumRange(final double min) {
        return min <= 0 ? rangeMin : backward(Math.floor(forward(min)));
    }

    protected double log(final double value) {
        if (value <= 0) {
            return Double.NaN;
            // return rangeMin * 0.99;
        }
        return Math.log10(value) / logBaseCache;
    }

    /**
     * Base of the logarithm used by the axis, must be grater than 1.
     * <p>
     * <b>Default value: 10</b>
     * </p>
     *
     * @return base of the logarithm
     */
    public DoubleProperty logarithmBaseProperty() {
        return logarithmBase;
    }

    protected double pow(final double value) {
        return Math.pow(logarithmBaseCache, value);
    }

    /**
     * Sets value of the {@link #logarithmBaseProperty()}.
     *
     * @param value base of the logarithm, value &gt; 1
     */
    public void setLogarithmBase(final double value) {
        logarithmBaseProperty().set(value);
    }

    @Override
    public void setMaximumRange(final double val) {
        if (val <= 0) {
            // reject
            LogarithmicAxisTransform.LOGGER.warn(String.format("%s::setMaximumRange(%f) - rejected",
                    LogarithmicAxisTransform.class.getSimpleName(), val));
            return;
        }
        axis.maxProperty().set(val);
    }

    @Override
    public void setMinimumRange(final double val) {
        if (val <= 0) {
            // reject
            LogarithmicAxisTransform.LOGGER.warn(String.format("%s::setMinimumRange(%f) - rejected",
                    LogarithmicAxisTransform.class.getSimpleName(), val));
            return;
        }
        axis.minProperty().set(val);
    }

}
