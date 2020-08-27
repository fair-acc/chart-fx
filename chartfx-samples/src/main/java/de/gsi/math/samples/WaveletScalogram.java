package de.gsi.math.samples;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import org.jtransforms.fft.DoubleFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.renderer.spi.ContourDataSetRenderer;
import de.gsi.chart.renderer.spi.utils.ColorGradient;
import de.gsi.chart.utils.AxisSynchronizer;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.GridDataSet;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.math.Math;
import de.gsi.math.samples.utils.AbstractDemoApplication;
import de.gsi.math.samples.utils.DemoChart;
import de.gsi.math.spectra.SpectrumTools;
import de.gsi.math.spectra.wavelet.ContinuousWavelet;

/**
 * example illustrating wavelet-based scalograms
 *
 * @author rstein
 */
public class WaveletScalogram extends AbstractDemoApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaveletScalogram.class);
    private static final int MAX_POINTS = 1024;
    public static final boolean LOAD_EXAMPLE_DATA = true;
    private GridDataSet fdataset;
    private DefaultDataSet fwavelet;
    private DefaultDataSet ffourier;
    private double[] yValues;

    private DataSet createDataSet() {
        final double nu = 2 * 25;
        final int nQuantx = 512;
        final int nQuanty = 1024;
        final double fmin = 0.05;
        final double fmax = 0.50;

        if (LOAD_EXAMPLE_DATA) {
            // show-room data
            // case 1: chirped CPS tune acquisition, the horizontal, cross-term
            // tune,
            // and a reference tone above 0.45 are visible
            // case 2: LHC B2 horizontal injection oscillations,
            // recommendation to choose nu >= 25
            // -> injection synchrotron oscillations are visible
            yValues = readDemoData(1);
        } else {
            // synthetic data
            yValues = loadSyntheticData();
        }

        // the wavelet scalogram computation
        final ContinuousWavelet wtrafo = new ContinuousWavelet();

        new Thread() {
            @Override
            public void run() {
                fdataset = wtrafo.getScalogram(yValues, nQuantx, nQuanty, nu, fmin, fmax);
            }
        }.start();

        do {
            sleep(1000);
            final int status = wtrafo.getStatus();
            if (status > 10) {
                LOGGER.atInfo().log(status + " % of computation done");
            }
        } while (wtrafo.isBusy());

        sleep(1000);

        final DoubleFFT_1D fft = new DoubleFFT_1D(yValues.length);
        final double[] fftSpectra = Arrays.copyOf(yValues, yValues.length);
        fft.realForward(fftSpectra);
        final double[] frequency1 = wtrafo.getScalogramFrequencyAxis(nQuantx, nQuanty, nu, fmin, fmax);
        final double[] magWavelet = new double[frequency1.length];
        final int nboundary = fdataset.getShape(DataSet.DIM_X) / 20;

        for (int i = 0; i < fdataset.getShape(DataSet.DIM_Y); i++) {
            double val = 0.0;
            int count = 0;

            for (int j = nboundary; j < fdataset.getShape(DataSet.DIM_X) - nboundary; j++) {
                val += fdataset.get(DataSet.DIM_Z, i * fdataset.getShape(DataSet.DIM_X) + j);
                count++;
            }
            if (count > 0) {
                magWavelet[i] = val / count;
            }
        }

        final double[] magFourier = SpectrumTools.computeMagnitudeSpectrum_dB(fftSpectra, true);
        final double[] frequency2 = SpectrumTools.computeFrequencyScale(fftSpectra.length / 2);

        // normalise FFT and wavelet spectra for better comparison
        final double maxWavelet = Math.maximum(magWavelet);
        for (int i = 0; i < magWavelet.length; i++) {
            magWavelet[i] -= maxWavelet;
        }

        final double maxFourier = Math.maximum(magFourier);
        for (int i = 0; i < magFourier.length; i++) {
            magFourier[i] -= maxFourier;
        }

        fwavelet = new DefaultDataSet("Wavelet magnitude", frequency1, magWavelet, frequency1.length, true);
        ffourier = new DefaultDataSet("Fourier magnitude", frequency2, magFourier, frequency2.length, true);

        fdataset.recomputeLimits(DataSet.DIM_Y);
        return fdataset;
    }

    @Override
    public Node getContent() {
        final DemoChart chart1 = new DemoChart();
        chart1.getXAxis().setName("time");
        chart1.getXAxis().setUnit("turns");
        chart1.getYAxis().setAutoRangeRounding(false);
        chart1.getYAxis().setAutoRangePadding(0.0);
        chart1.getYAxis().setName("frequency");
        chart1.getYAxis().setUnit("fs");
        final ContourDataSetRenderer contourChartRenderer = new ContourDataSetRenderer();
        chart1.getRenderers().set(0, contourChartRenderer);
        contourChartRenderer.setColorGradient(ColorGradient.RAINBOW);
        // contourChartRenderer.setColorGradient(ColorGradient.JET);
        // contourChartRenderer.setColorGradient(ColorGradient.TOPO_EXT);
        contourChartRenderer.getDatasets().add(createDataSet());

        final DemoChart chart2 = new DemoChart();
        chart2.getXAxis().setName("frequency");
        chart2.getXAxis().setUnit("fs");
        chart2.getYAxis().setName("magnitude");
        chart1.getXAxis().setAutoRangeRounding(false);
        chart1.getXAxis().setAutoRangePadding(0.0);
        chart2.getDatasets().addAll(fwavelet, ffourier);

        final AxisSynchronizer sync = new AxisSynchronizer();
        sync.add(chart1.getYAxis());
        sync.add(chart2.getXAxis());

        return new VBox(chart1, chart2);
    }

    private double[] loadSyntheticData() {
        // synthetic data
        final double[] yModel = new double[MAX_POINTS];

        final Random rnd = new Random();
        for (int i = 0; i < yModel.length; i++) {
            final double x = i;
            double offset = 0;
            final double error = 0.1 * rnd.nextGaussian();

            // linear chirp with discontinuity
            offset = (i > 500) ? -20 : 0;
            yModel[i] = (i > 100 && i < 700) ? 0.7 * Math.sin(Math.TWO_PI * 2e-4 * x * (x + offset)) : 0;

            // single tone at 0.25
            yModel[i] += (i > 50 && i < 500) ? 1.0 * Math.sin(Math.TWO_PI * 0.25 * x) : 0;

            // modulation around 0.4
            final double mod = Math.cos(Math.TWO_PI * 0.01 * x);
            yModel[i] += (i > 300 && i < 900) ? 1.0 * Math.sin(Math.TWO_PI * (0.4 - 5e-4 * mod) * x) : 0;

            // quadratic chirp starting at 0.1
            yModel[i] += 0.5 * Math.sin(Math.TWO_PI * ((0.1 + 5e-8 * x * x) * x));

            yModel[i] = yModel[i] + error;
        }
        return yModel;
    }

    private double[] readDemoData(int index) {
        final String fileName = index <= 1 ? "./rawDataCPS2.dat" : "./rawDataLHCInj.dat";
        try {
            try (BufferedReader reader = new BufferedReader(
                         new InputStreamReader(EMDSample.class.getResourceAsStream(fileName)))) {
                String line = reader.readLine();
                final int nDim = line == null ? 0 : Integer.parseInt(line);
                double[] ret = new double[nDim];
                for (int i = 0; i < nDim; i++) {
                    line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    final String[] x = line.split("\t");
                    ret[i] = Double.parseDouble(x[1]);
                }

                return ret;
            }

        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("read error");
            }
        }

        return new double[1000];
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("InterruptedException");
            }
            Thread.currentThread().interrupt();
        }
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
