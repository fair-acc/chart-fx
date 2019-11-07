package de.gsi.chart.samples.legacy;

import java.util.Timer;

import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.datareduction.DefaultDataReducer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.samples.RollingBufferSample;
import javafx.application.Application;

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
            timer[0] = new Timer();
            timer[1] = new Timer();
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
        eRenderer.setErrorType(ErrorStyle.NONE);
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
