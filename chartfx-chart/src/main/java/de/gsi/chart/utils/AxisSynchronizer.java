/*****************************************************************************
 *                                                                           *
 * Common Chart - axis synchronization                                       *
 *                                                                           *
 * modified: 2019-01-23 Harald Braeuning                                     *
 *                                                                           *
 ****************************************************************************/

package de.gsi.chart.utils;

import java.util.ArrayList;

import de.gsi.chart.axes.spi.AbstractAxis;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Synchronizes the axes of different charts. All charts are equal.
 *
 * @author braeun
 */
public class AxisSynchronizer {

    private boolean updating;
    private final ArrayList<AbstractAxis> charts = new ArrayList<>();
    private final ChangeListener<Number> upperBoundChangeListener = this::upperBoundChanged;
    private final ChangeListener<Number> lowerBoundChangeListener = this::lowerBoundChanged;

    public AxisSynchronizer() {
    }

    public void add(AbstractAxis axis) {
        charts.add(axis);
        // if (charts.size() > 1) axis.setAutoRanging(false);
        axis.upperBoundProperty().addListener(upperBoundChangeListener);
        axis.lowerBoundProperty().addListener(lowerBoundChangeListener);
        // axis.tickUnitProperty().bind(master.tickUnitProperty());
    }

    public void remove(AbstractAxis axis) {
        charts.remove(axis);
        axis.upperBoundProperty().removeListener(upperBoundChangeListener);
        axis.lowerBoundProperty().removeListener(lowerBoundChangeListener);
        // axis.tickUnitProperty().unbind();
        axis.setAutoRanging(true);
    }

    private void upperBoundChanged(ObservableValue<? extends Number> property, Number oldValue, Number newValue) {
        if (!updating) {
            final double value = newValue.doubleValue();
            if (Double.isNaN(value)) {
                return;
            }
            if (value == oldValue.doubleValue()) return;
            updating = true;
            final AbstractAxis sender = findAxis(property);
            final double tickUnit = calculateTickUnit(sender);
            for (final AbstractAxis axis : charts) {
                if (axis != sender) {
                    axis.setAutoRanging(false);
                    axis.setUpperBound(value);
                }
                axis.setTickUnit(tickUnit);
            }
            updating = false;
        }
    }

    private void lowerBoundChanged(ObservableValue<? extends Number> property, Number oldValue, Number newValue) {
        if (!updating) {
            final double value = newValue.doubleValue();
            if (Double.isNaN(value)) {
                return;
            }
            if (value == oldValue.doubleValue()) return;
            updating = true;
            final AbstractAxis sender = findAxis(property);
            final double tickUnit = calculateTickUnit(sender);
            for (final AbstractAxis axis : charts) {
                if (axis != sender) {
                    axis.setLowerBound(value);
                    axis.setAutoRanging(false);
                }
                axis.setTickUnit(tickUnit);
            }
            updating = false;
        }
    }

    private AbstractAxis findAxis(ObservableValue<? extends Number> property) {
        for (final AbstractAxis chart : charts) {
            if (property == chart.upperBoundProperty()) {
                return chart;
            }
            if (property == chart.lowerBoundProperty()) {
                return chart;
            }
        }
        return null;
    }

    // private static final int MAX_TICK_COUNT = 20;

    private double calculateTickUnit(AbstractAxis axis) {
        return axis.computePreferredTickUnit(axis.getLength());
        // final double labelSize = axis.getTickLabelFont().getSize() * 2;
        // final double axisLength = Math.max(axis.getWidth(),axis.getHeight());
        // final int numOfFittingLabels = (int) Math.floor(axisLength /
        // labelSize);
        // final int numOfTickMarks = Math.max(Math.min(numOfFittingLabels,
        // MAX_TICK_COUNT), 2);
        // double rawTickUnit = (axis.getUpperBound() - axis.getLowerBound()) /
        // numOfTickMarks;
        // if (rawTickUnit == 0 || Double.isNaN(rawTickUnit))
        // {
        // rawTickUnit = 1e-3;// TODO: remove hac
        // }
        // final TickUnitSupplier unitSupplier =
        // axis.getDefaultAxisLabelFormatter().getTickUnitSupplier();
        // if (unitSupplier == null)
        // {
        // // unitSupplier = DEFAULT_TICK_UNIT_SUPPLIER;
        // throw new IllegalStateException("class defaults not properly
        // initialised");
        // }
        // final double majorUnit = unitSupplier.computeTickUnit(rawTickUnit);
        // if (majorUnit <= 0)
        // {
        // throw new IllegalArgumentException("The " +
        // unitSupplier.getClass().getName()
        // + " computed illegal unit value [" + majorUnit + "] for argument " +
        // rawTickUnit);
        // }
        // return majorUnit;
    }

}
