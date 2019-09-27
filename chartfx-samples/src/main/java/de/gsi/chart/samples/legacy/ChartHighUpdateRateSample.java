package de.gsi.chart.samples.legacy;

import de.gsi.chart.samples.RollingBufferSample;
import javafx.application.Application;

/**
 * chart-fx stress test for updates at 100 Hz to 1 kHz
 * 
 * @author rstein
 * @Deprecated chart-fx not (yet) nominally designed to allow for > 100 Hz update rates
 */
@Deprecated
public class ChartHighUpdateRateSample extends RollingBufferSample {
    static {
        N_SAMPLES = 30000;
        UPDATE_PERIOD = 10;
        BUFFER_CAPACITY = 7500;
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
