package de.gsi.math.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JOptionPane;

import de.gsi.chart.data.DataSet3D;
import de.gsi.chart.data.spi.DefaultDataSet;
import de.gsi.chart.renderer.spi.ContourDataSetRenderer;
import de.gsi.chart.renderer.spi.utils.ColorGradient;
import de.gsi.chart.utils.AxisSynchronizer;
import de.gsi.math.TMath;
import de.gsi.math.demo.utils.AbstractDemoApplication;
import de.gsi.math.demo.utils.DemoChart;
import de.gsi.math.spectra.SpectrumTools;
import de.gsi.math.spectra.fft.DoubleFFT_1D;
import de.gsi.math.spectra.wavelet.ContinuousWavelet;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * example illustrating wavelet-based scalograms
 * 
 * @author rstein
 */
public class WaveletScalogram extends AbstractDemoApplication {

    private final int MAX_POINTS = 1024;
    public static boolean LOAD_EXAMPLE_DATA = true;
    private DataSet3D fdataset;
    private DefaultDataSet fwavelet;
    private DefaultDataSet ffourier;
    private double[] yValues;
    private double[] yModel;

    private DataSet3D createDataSet() {
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
            yModel = readDemoData(1);
            yValues = Arrays.copyOf(yModel, yModel.length);
        } else {
            // synthetic data
            yValues = new double[MAX_POINTS];
            yModel = new double[MAX_POINTS];

            Random rnd = new Random();
            for (int i = 0; i < yValues.length; i++) {
                double x = i;
                double offset = 0;
                double error = 0.1 * rnd.nextGaussian();

                // linear chirp with discontinuity
                offset = (i > 500) ? -20 : 0;
                yModel[i] = (i > 100 && i < 700) ? 0.7 * Math.sin(TMath.TwoPi() * 2e-4 * x * (x + offset)) : 0;

                // single tone at 0.25
                yModel[i] += (i > 50 && i < 500) ? 1.0 * Math.sin(TMath.TwoPi() * 0.25 * x) : 0;

                // modulation around 0.4
                double mod = Math.cos(TMath.TwoPi() * 0.01 * x);
                yModel[i] += (i > 300 && i < 900) ? 1.0 * Math.sin(TMath.TwoPi() * (0.4 - 5e-4 * mod) * x) : 0;

                // quadratic chirp starting at 0.1
                yModel[i] += 0.5 * Math.sin(TMath.TwoPi() * ((0.1 + 5e-8 * x * x) * x));

                yValues[i] = yModel[i] + error;
            }

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
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int status = wtrafo.getStatus();
            if (status > 10) {
                System.out.println(status + " % of computation done");
            }
        } while (wtrafo.isBusy());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(yValues.length);
        double[] fftSpectra = Arrays.copyOf(yValues, yValues.length);
        fft.realForward(fftSpectra);
        double[] frequency1 = wtrafo.getScalogramFrequencyAxis(nQuantx, nQuanty, nu, fmin, fmax);
        double[] magWavelet = new double[frequency1.length];
        final int nboundary = fdataset.getXDataCount() / 20;

        for (int i = 0; i < fdataset.getYDataCount(); i++) {
            double val = 0.0;
            int count = 0;

            for (int j = nboundary; j < fdataset.getXDataCount() - nboundary; j++) {
                val += fdataset.getZ(j, i);
                count++;
            }
            magWavelet[i] = val / count;
        }

        double[] magFourier = SpectrumTools.computeMagnitudeSpectrum_dB(fftSpectra, true);
        double[] frequency2 = SpectrumTools.computeFrequencyScale(fftSpectra.length / 2);

        // normalise FFT and wavelet spectra for better comparison
        double maxWavelet = TMath.Maximum(magWavelet);
        for (int i = 0; i < magWavelet.length; i++) {
            magWavelet[i] -= maxWavelet;
        }

        double maxFourier = TMath.Maximum(magFourier);
        for (int i = 0; i < magFourier.length; i++) {
            magFourier[i] -= maxFourier;
        }

        fwavelet = new DefaultDataSet("Wavelet magnitude", frequency1, magWavelet);
        ffourier = new DefaultDataSet("Fourier magnitude", frequency2, magFourier);

        return fdataset;
    }

    @Override
    public Node getContent() {

        DemoChart chart1 = new DemoChart();
        chart1.getXAxis().setLabel("time");
        chart1.getXAxis().setUnit("turns");
        chart1.getYAxis().setAutoRangeRounding(false);
        chart1.getYAxis().setAutoRangePadding(0.0);
        chart1.getYAxis().setLabel("frequency");
        chart1.getYAxis().setUnit("fs");
        ContourDataSetRenderer contourChartRenderer = new ContourDataSetRenderer();
        chart1.getRenderers().set(0, contourChartRenderer);
        contourChartRenderer.setColorGradient(ColorGradient.RAINBOW);
        // contourChartRenderer.setColorGradient(ColorGradient.JET);
        // contourChartRenderer.setColorGradient(ColorGradient.TOPO_EXT);
        contourChartRenderer.getDatasets().add(createDataSet());

        DemoChart chart2 = new DemoChart();
        chart2.getXAxis().setLabel("frequency");
        chart2.getXAxis().setUnit("fs");
        chart2.getYAxis().setLabel("magnitude");
        chart1.getXAxis().setAutoRangeRounding(false);
        chart1.getXAxis().setAutoRangePadding(0.0);
        chart2.getDatasets().addAll(fwavelet, ffourier);

        AxisSynchronizer sync = new AxisSynchronizer();
        sync.add(chart2.getXAxis());
        sync.add(chart1.getYAxis());

        return new VBox(chart1, chart2);
    }

    private double[] readDemoData(int index) {
        try {
            BufferedReader reader;

            if (index <= 1) {
                reader = new BufferedReader(
                        new InputStreamReader(WaveletScalogram.class.getResourceAsStream("./rawDataCPS2.dat")));
            } else {
                reader = new BufferedReader(
                        new InputStreamReader(WaveletScalogram.class.getResourceAsStream("./rawDataLHCInj.dat")));
            }
            int nDim = Integer.parseInt(reader.readLine());
            double[] ret = new double[nDim];
            for (int i = 0; i < nDim; i++) {
                String[] x = reader.readLine().split("\t");
                ret[i] = Double.parseDouble(x[1]);
            }

            return ret;

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Could not data file:\n" + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }

}
