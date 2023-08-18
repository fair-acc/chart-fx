package io.fair_acc.sample.chart.legacy;

import java.util.Timer;

import io.fair_acc.sample.chart.RollingBufferSample;
import javafx.application.Application;

import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.datareduction.DefaultDataReducer;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;

/**
 * derived class to benchmark performance of new chart library against JavaFX Chart version
 * 
 * @author rstein
 *
 */
public class RollingBufferNewRefSample extends RollingBufferSample {
    public RollingBufferNewRefSample() {
        super();

        if (timer == null) {
            timer = new Timer[2];
            timer[0] = new Timer("sample-update-timer", true);
            timer[1] = new Timer("sample-update-timer", true);
            rollingBufferBeamIntensity.reset();
            rollingBufferDipoleCurrent.reset();
            timer[0].scheduleAtFixedRate(getTask(0), 0, UPDATE_PERIOD);
            timer[1].scheduleAtFixedRate(getTask(1), 0, UPDATE_PERIOD);
        }
    }

    @Override
    protected void initErrorDataSetRenderer(final ErrorDataSetRenderer eRenderer) {
        // for higher performance w/o error bars, enable this for comparing with
        // the standard JavaFX charting library (which does not support error
        // handling, etc.)
        eRenderer.setErrorStyle(ErrorStyle.NONE);
        eRenderer.setDashSize(0);
        eRenderer.setDrawMarker(false);
        final DefaultDataReducer reductionAlgorithm = (DefaultDataReducer) eRenderer.getRendererDataReducer();
        reductionAlgorithm.setMinPointPixelDistance(RollingBufferSample.MIN_PIXEL_DISTANCE);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
