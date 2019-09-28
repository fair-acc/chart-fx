package de.gsi.chart.samples.legacy;

import com.sun.javafx.tk.Toolkit;

import de.gsi.chart.samples.RollingBufferSample;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;

/**
 * chart-fx stress test for updates at 100 Hz to 1 kHz
 * 
 * @author rstein
 */
public class ChartHighUpdateRateSample extends RollingBufferSample {
    static {
        N_SAMPLES = 30000;
        UPDATE_PERIOD = 1;
        BUFFER_CAPACITY = 7500;
    }
    private static int counter = 0;

    @Override
    public BorderPane initComponents(Scene scene) {
        BorderPane pane = super.initComponents(scene);
        rollingBufferDipoleCurrent.autoNotification().set(true);
        rollingBufferBeamIntensity.autoNotification().set(true);
        Toolkit.getToolkit().addSceneTkPulseListener(() -> {
            counter = (counter + 1) % 100;
            if (counter == 0) {
                System.err.println("pulse auto dipole = " + rollingBufferDipoleCurrent.autoNotification().get()
                + " auto beam " + rollingBufferBeamIntensity.autoNotification().get());
            }
        });

        Toolkit.getToolkit().checkFxUserThread();
        return pane;
    }

    public static void main(final String[] args) {
        Application.launch(args);

    }
}
