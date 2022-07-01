package io.fair_acc.chartfx.axes.spi.format;

import java.util.List;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.AxisLabelFormatter;
import io.fair_acc.chartfx.axes.TickUnitSupplier;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.StringConverter;

/**
 * @author rstein
 */
public abstract class AbstractFormatter extends StringConverter<Number> implements AxisLabelFormatter {
    private static final TickUnitSupplier DEFAULT_TICK_UNIT_SUPPLIER = new DefaultTickUnitSupplier();
    private final ObjectProperty<TickUnitSupplier> tickUnitSupplier = new SimpleObjectProperty<>(this,
            "tickUnitSupplier", AbstractFormatter.DEFAULT_TICK_UNIT_SUPPLIER);
    protected FormatterLabelCache labelCache = new FormatterLabelCache();
    protected List<Double> majorTickMarksCopy;
    protected double unitScaling;
    protected double rangeMin;
    protected double rangeMax;

    protected double localSchmidtTriggerThreshold = 0.01;
    protected DoubleProperty schmittTriggerThreshold = new SimpleDoubleProperty(this, "schmittTriggerThreshold",
            localSchmidtTriggerThreshold) {
        @Override
        public void set(final double value) {
            super.set(Math.abs(value));
        }
    };

    public AbstractFormatter() {
        super();
        rangeMin = 0;
        rangeMax = 1;
    }

    /**
     * Construct a DefaultFormatter for the given NumberAxis
     *
     * @param axis The axis to format tick marks for
     */
    public AbstractFormatter(final Axis axis) {
        this();
        if (axis == null) {
            return;
        }
    }

    protected double getLogRange() {
        final double diff = getRange();

        return diff > 0 ? Math.log10(diff) : 1;
    }

    protected double getRange() {
        return Math.abs(rangeMax - rangeMin);
    }

    /**
     * Returns the value of the {@link #tickUnitSupplierProperty()}.
     *
     * @return the TickUnitSupplier
     */
    @Override
    public TickUnitSupplier getTickUnitSupplier() {
        return tickUnitSupplierProperty().get();
    }

    protected abstract void rangeUpdated();

    /**
     * sets the min/max threshold when to change from one formatter domain to the other
     *
     * @return the Schmitt trigger threshold property
     */
    public DoubleProperty schmittTriggerThresholdProperty() {
        return schmittTriggerThreshold;
    }

    /**
     * Sets the value of the {@link #tickUnitSupplierProperty()}.
     *
     * @param supplier the tick unit supplier. If {@code null}, the default one will be used
     */
    @Override
    public void setTickUnitSupplier(final TickUnitSupplier supplier) {
        tickUnitSupplierProperty().set(supplier);
    }

    /**
     * Strategy to compute major tick unit when auto-range is on or when axis bounds change. By default initialised to
     * {@link DefaultTickUnitSupplier}.
     * <p>
     * See {@link TickUnitSupplier} for more information about the expected behaviour of the strategy.
     * </p>
     *
     * @return tickUnitSupplier property
     */
    @Override
    public ObjectProperty<TickUnitSupplier> tickUnitSupplierProperty() {
        return tickUnitSupplier;
    }

    @Override
    public void updateFormatter(final List<Double> newMajorTickMarks, final double unitScaling) {
        majorTickMarksCopy = newMajorTickMarks;
        this.unitScaling = unitScaling;

        this.rangeMin = +Double.MAX_VALUE;
        this.rangeMax = -Double.MAX_VALUE;
        for (Number num : majorTickMarksCopy) {
            double val = num.doubleValue();
            if (Double.isFinite(val)) {
                this.rangeMin = Math.min(this.rangeMin, val);
                this.rangeMax = Math.max(this.rangeMax, val);
            }
        }
        this.rangeMin /= unitScaling;
        this.rangeMax /= unitScaling;

        rangeUpdated();
    }

}
