/*****************************************************************************
 *                                                                           *
 * Common Chart - axis synchronization                                       *
 *                                                                           *
 * modified: 2018-08-23 Harald Braeuning                                     *
 *                                                                           *
 ****************************************************************************/

package de.gsi.chart.utils;

import java.util.ArrayList;

import de.gsi.chart.axes.spi.AbstractAxis;

/**
 * Synchronizes the axes of different slave charts to the axis of a master
 * chart.
 *
 * @author braeun
 */
public class MasterSlaveAxisSynchronizer {

    private final AbstractAxis master;
    private final ArrayList<AbstractAxis> slaves = new ArrayList<>();

    public MasterSlaveAxisSynchronizer(AbstractAxis master) {
        this.master = master;
        master.maxProperty().addListener((p, o, n) -> upperBoundChanged(n.doubleValue()));
        master.minProperty().addListener((p, o, n) -> lowerBoundChanged(n.doubleValue()));
    }

    public void add(AbstractAxis axis) {
        slaves.add(axis);
        axis.setAutoRanging(false);
        axis.tickUnitProperty().bind(master.tickUnitProperty());
    }

    public void remove(AbstractAxis axis) {
        slaves.remove(axis);
        axis.tickUnitProperty().unbind();
        axis.setAutoRanging(true);
    }

    private void upperBoundChanged(double value) {
        if (Double.isNaN(value)) {
            return;
        }
        for (final AbstractAxis slave : slaves) {
            slave.setMax(value);
            // slave.setTickUnit(master.getTickUnit());
        }
    }

    private void lowerBoundChanged(double value) {
        if (Double.isNaN(value)) {
            return;
        }
        for (final AbstractAxis slave : slaves) {
            slave.setMin(value);
            // slave.setTickUnit(master.getTickUnit());
        }
    }

}
