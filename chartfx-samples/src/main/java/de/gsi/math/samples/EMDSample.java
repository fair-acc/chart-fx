package de.gsi.math.samples;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.renderer.spi.ContourDataSetRenderer;
import de.gsi.chart.renderer.spi.utils.ColorGradient;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.math.TMath;
import de.gsi.math.matrix.MatrixD;
import de.gsi.math.samples.utils.AbstractDemoApplication;
import de.gsi.math.samples.utils.DemoChart;
import de.gsi.math.spectra.EEMD;

/**
 * example illustrating EMD-based spectrograms
 * 
 * @author rstein TODO: some fixes in EMD necessary
 */
public class EMDSample extends AbstractDemoApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(FourierSample.class);
    private static final int MAX_POINTS = 1024;
    private static final boolean LOAD_EXAMPLE_DATA = true;
    private DataSet dataset;
    private DataSet[] fmodeDataSets = new DataSet[10];
    private double[][] fmodeData = new double[10][];
    private double[] yValues;

    private DataSet createDataSet() {
        final int nQuantx = 1024;
        final int nQuanty = 1024;

        // the empirical-mode-decomposition (EEMD) computation
        final EEMD trafoHHT = new EEMD();

        new Thread() {
            @Override
            public void run() {
                dataset = trafoHHT.getScalogram(yValues, nQuantx, nQuanty);
            }
        }.start();

        try {
            do {
                sleep(100);
                int status = trafoHHT.getStatus();
                if (status > 10) {
                    LOGGER.atInfo().log(status + " % of computation done");
                }
            } while (trafoHHT.isBusy());
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("error during computation");
            }
        }

        return dataset;
    }

    private void createModeDataSet() {
        final EEMD trafoHHT = new EEMD();
        MatrixD emd = trafoHHT.eemd(yValues, 0, 1.0);

        double[] time = new double[yValues.length];
        for (int i = 0; i < time.length; i++) {
            time[i] = i;
        }

        for (int nmode = 0; nmode < (emd.getColumnDimension() - 1); nmode++) {
            String name = (nmode == 0) ? "raw" : ("mode" + nmode);
            if (nmode < fmodeDataSets.length) {
                fmodeData[nmode] = new double[time.length];
                for (int j = 0; j < fmodeData[nmode].length; j++) {
                    fmodeData[nmode][j] = emd.get(j, nmode) - 5 * nmode;
                }
                LOGGER.atInfo().log("%s mean = %f\n", name,
                        (TMath.Mean(fmodeData[nmode]) + 2 * nmode) / TMath.PeakToPeak(fmodeData[nmode]));
                fmodeDataSets[nmode] = new DefaultErrorDataSet(name, time, fmodeData[nmode], new double[time.length],
                        new double[time.length], time.length, true);
            }
        }
    }

    @Override
    public Node getContent() {
        initData();

        final DemoChart chart1 = new DemoChart();
        chart1.getXAxis().setName("time");
        chart1.getXAxis().setUnit("turns");
        chart1.getYAxis().setName("frequency");
        chart1.getYAxis().setUnit("fs");
        ContourDataSetRenderer contourChartRenderer = new ContourDataSetRenderer();
        chart1.getRenderers().set(0, contourChartRenderer);
        contourChartRenderer.setColorGradient(ColorGradient.RAINBOW);
        // contourChartRenderer.setColorGradient(ColorGradient.JET);
        // contourChartRenderer.setColorGradient(ColorGradient.TOPO_EXT);

        contourChartRenderer.getDatasets().add(createDataSet());

        final DemoChart chart2 = new DemoChart();
        chart2.getXAxis().setName("time");
        chart2.getXAxis().setUnit("turns");
        chart2.getYAxis().setName("amplitude");
        chart2.getYAxis().setUnit("[a.u.]");
        for (int i = 0; i < fmodeDataSets.length; i++) {
            if (fmodeDataSets[i] != null) {
                chart2.getDatasets().add(fmodeDataSets[i]);
            }
        }

        return new VBox(chart1, chart2);
    }

    public void initData() {
        if (LOAD_EXAMPLE_DATA) {
            // show-room data
            // case 1: chirped CPS tune acquisition, the horizontal, cross-term
            // tune,
            // and a reference tone above 0.45 are visible
            // case 2: LHC B2 horizontal injection oscillations,
            // recommendation to choose nu == 30
            // -> injection synchrotron oscillations are visible
            yValues = readDemoData(1);
        } else {
            yValues = loadSyntheticData();
        }
        createModeDataSet();
    }

    private double[] loadSyntheticData() {
        // synthetic data
        final double[] yModel = new double[MAX_POINTS];

        for (int i = 0; i < yValues.length; i++) {
            final double x = i;
            double offset = 0;
            double error = 0.1 * RANDOM.nextGaussian();

            // linear chirp with discontinuity
            offset = (i > 500) ? -20 : 0;
            yModel[i] = (i > 100 && i < 700) ? 0.7 * Math.sin(TMath.TwoPi() * 2e-4 * x * (x + offset)) : 0;

            // single tone at 0.25
            yModel[i] += (i > 50 && i < 500) ? 1.0 * Math.sin(TMath.TwoPi() * 0.25 * x) : 0;
            // yModel[i] = Math.sin(TMath.TwoPi() * 0.3* x);

            // modulation around 0.4
            double mod = Math.cos(TMath.TwoPi() * 0.01 * x);
            if (i < 470) {
                mod = 0.0;
            }
            yModel[i] += (i > 300 && i < 900) ? 1.0 * Math.sin(TMath.TwoPi() * (0.4 - 5e-4 * mod) * x) : 0;

            // quadratic chirp starting at 0.1
            yModel[i] += 0.5 * Math.sin(TMath.TwoPi() * ((0.1 + 5e-8 * x * x) * x));

            yModel[i] = yModel[i] + error;
        }
        return Arrays.copyOf(yModel, yModel.length);
    }

    private double[] readDemoData(int index) {
        String fileName = index <= 1 ? "./rawDataCPS2.dat" : "./rawDataLHCInj.dat";
        try {
            try (BufferedReader reader = new BufferedReader(
                         new InputStreamReader(EMDSample.class.getResourceAsStream(fileName)))) {
                String line = reader.readLine();
                int nDim = line == null ? 0 : Integer.parseInt(line);
                double[] ret = new double[nDim];
                for (int i = 0; i < nDim; i++) {
                    line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    String[] x = line.split("\t");
                    ret[i] = Double.parseDouble(x[1]);
                }

                return ret;
            }

        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("read data error");
            }
        }

        return new double[1000];
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("sleep error");
            }
            Thread.currentThread().interrupt();
        }
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
