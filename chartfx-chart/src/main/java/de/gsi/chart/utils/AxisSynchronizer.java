/*****************************************************************************
 *                                                                           *
 * Common Chart - axis synchronization                                       *
 *                                                                           *
 * modified: 2019-01-23 Harald Braeuning                                     *
 * modified: 2019-07-19 rstein                                               *
 *                                                                           *
 ****************************************************************************/

package de.gsi.chart.utils;

import java.util.ArrayList;

import de.gsi.chart.axes.Axis;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Synchronizes the axes of different charts. All charts are equal.
 *
 * @author braeun
 */
public class AxisSynchronizer {

    private boolean updating;
    private final ArrayList<Axis> axes = new ArrayList<>();
    private final ChangeListener<Number> upperBoundChangeListener = this::upperBoundChanged;
    private final ChangeListener<Number> lowerBoundChangeListener = this::lowerBoundChanged;

    public AxisSynchronizer() {
        super();
    }

    public void add(Axis axis) {
        axes.add(axis);
        axis.maxProperty().addListener(upperBoundChangeListener);
        axis.minProperty().addListener(lowerBoundChangeListener);
    }

    private Axis findAxis(ObservableValue<? extends Number> property) {
        for (final Axis chart : axes) {
            if (property == chart.maxProperty()) {
                return chart;
            }
            if (property == chart.minProperty()) {
                return chart;
            }
        }
        return null;
    }

    private void lowerBoundChanged(ObservableValue<? extends Number> property, Number oldValue, Number newValue) {
        if (!updating) {
            final double value = newValue.doubleValue();
            if (Double.isNaN(value)) {
                return;
            }
            if (value == oldValue.doubleValue())
                return;
            updating = true;
            final Axis sender = findAxis(property);
            if (sender == null) {
                updating = false;
                return;
            }
            final double tickUnit = sender.getTickUnit();
            for (final Axis axis : axes) {
                if (axis != sender) {
                    axis.setMin(value);
                    axis.setAutoRanging(false);
                }
                axis.setTickUnit(tickUnit);
            }
            updating = false;
        }
    }

    public void remove(Axis axis) {
        axes.remove(axis);
        axis.maxProperty().removeListener(upperBoundChangeListener);
        axis.minProperty().removeListener(lowerBoundChangeListener);
        axis.setAutoRanging(true);
    }

    private void upperBoundChanged(ObservableValue<? extends Number> property, Number oldValue, Number newValue) {
        if (!updating) {
            final double value = newValue.doubleValue();
            if (Double.isNaN(value)) {
                return;
            }
            if (value == oldValue.doubleValue())
                return;
            updating = true;
            final Axis sender = findAxis(property);
            if (sender == null) {
                updating = false;
                return;
            }
            final double tickUnit = sender.getTickUnit();
            for (final Axis axis : axes) {
                if (axis != sender) {
                    axis.setAutoRanging(false);
                    axis.setMax(value);
                }
                axis.setTickUnit(tickUnit);
            }
            updating = false;
        }
    }

}
