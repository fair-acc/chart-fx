package de.gsi.math.samples;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.math.TMath;
import de.gsi.math.TMathConstants;
import de.gsi.math.functions.RandomWalkFunction;
import de.gsi.math.samples.utils.AbstractDemoApplication;
import de.gsi.math.samples.utils.DemoChart;
import de.gsi.math.spectra.wavelet.CDFWavelet;
import de.gsi.math.spectra.wavelet.FastWaveletTransform;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * example illustrating a wavelet denoising algorithm
 * 
 * @author rstein
 */
public class WaveletDenoising extends AbstractDemoApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaveletDenoising.class);
    private static final int MAX_POINTS = 512;
    private static final boolean LOAD_EXAMPLE_DATA = false;
    private DataSet fraw;
    private DataSet fdata;
    private DataSet freconstructed;
    private DataSet fspectraModel;
    private DataSet fspectra;
    private DataSet fspectraFit;

    @Override
    public Node getContent() {
        initData();

        final DemoChart chart1 = new DemoChart();
        chart1.getXAxis().setName("time");
        chart1.getDatasets().addAll(fdata, fraw, freconstructed);

        final DemoChart chart2 = new DemoChart();
        chart2.getXAxis().setName("frequency");
        chart2.getDatasets().addAll(fspectraModel, fspectra, fspectraFit);

        return new VBox(chart1, chart2);
    }

    private void initData() {
        // third order polynomial function
        final RandomWalkFunction func = new RandomWalkFunction("rand1", 0.1);

        double[] xValues;
        double[] yValues;
        double[] yModel;

        if (LOAD_EXAMPLE_DATA) {
            final double[][] data = readDemoData();
            xValues = data[0];
            yValues = data[1];
            yModel = Arrays.copyOf(yValues, yValues.length);
        } else {
            xValues = new double[MAX_POINTS];
            yValues = new double[MAX_POINTS];
            yModel = new double[MAX_POINTS];

            final Random rnd = new Random();
            for (int i = 0; i < xValues.length; i++) {
                final double x = i;
                double offset = 0;
                final double error = 0.2 * rnd.nextGaussian();
                if (i > xValues.length / 2) {
                    offset = -0.05;
                }
                xValues[i] = x;
                yValues[i] = func.getValue(0);
                yModel[i] = Math.sin(TMathConstants.TwoPi() * 3e-4 * x * (x + offset));
                yModel[i] = Math.sin(TMathConstants.TwoPi() * 0.05 * x);

                if (i < 100 || i > 400) {
                    yModel[i] = 0;
                }

                yValues[i] = yModel[i] + 0 * error;
            }
        }

        final CDFWavelet wvTrafo1 = new CDFWavelet();
        final FastWaveletTransform wvTrafo3 = new FastWaveletTransform();
        final boolean trafo1 = false;

        final double[] ySmooth = Arrays.copyOf(yValues, yValues.length);
        final double[] ySModel = Arrays.copyOf(yModel, yModel.length);

        if (trafo1) {
            wvTrafo1.fwt97(ySmooth, ySmooth.length);
            wvTrafo1.fwt97(ySModel, ySModel.length);
        } else {
            // wvTrafo1.fwt53(ySmooth, ySmooth.length);
            // wvTrafo1.fwt53(ySModel, ySModel.length);
            wvTrafo3.transform(ySmooth);
            // wvTrafo3.daubTrans(ySModel);
            wvTrafo3.transform(ySModel);
        }

        final double[] recon = Arrays.copyOf(ySmooth, yValues.length);
        final double[] reconAbs = Arrays.copyOf(ySmooth, yValues.length);
        for (int i = 0; i < reconAbs.length; i++) {
            reconAbs[i] = Math.abs(recon[i]);
        }

        // double max = TMath.Maximum(reconAbs);
        // double min = TMath.Minimum(reconAbs);
        // double median = TMath.Median(reconAbs);

        // Percival and Walden, "Wavelet Methods for Time Series Analysis",
        // Cambridge University Press, 2006, Section 10.5
        // David L. Donoho and Iain M. Johnstone, "Minimax Estimation via
        // Wavelet Shrinkage", The Annals of Statistics, 1998, Vol. 26, No. 3,
        // 879 921

        // classic implementation
        // double lambda = median / 0.6745 * Math.sqrt(Math.log(recon.length));
        // double norm = 1.0 / (1.0 - lambda);
        // for (int i = 0; i < xValues.length; i++) {
        // double val = recon[i];
        //
        // // hard threshold
        // if (true)
        // if (Math.abs(val) < lambda) {
        // recon[i] = 0.0;
        // }
        //
        // // soft threshold
        // if (false)
        // if (Math.abs(val) < lambda) {
        // recon[i] = 0.0;
        // } else {
        // recon[i] -= Math.signum(val) * lambda;
        // recon[i] *= norm;
        // }
        // }

        // test
        // for (int i = 0; i < xValues.length / 2; i++) {
        // final double val = Math.abs(recon[i]);
        // final double err = Math.abs(recon[xValues.length / 2 + i]);
        //
        // if (val < lambda) {
        // recon[i] = 0.0;
        // recon[xValues.length / 2 + i] = 0.0;
        // } else if (val != 0 && err < val && (err / val < lambda)) {
        // recon[xValues.length / 2 + i] = 0.0;
        // } else if (val != 0 && err > val && (val / err < lambda)) {
        // recon[i] = 0.0;
        // }
        //
        // }

        for (int i = 0; i < 0; i++) {
            recon[i] = 0.0;
        }

        fspectraModel = new DefaultDataSet("model", xValues, ySModel, xValues.length, true);
        fspectra = new DefaultDataSet("raw data", xValues, ySmooth, xValues.length, true);
        fspectraFit = new DefaultDataSet("reconstructed", xValues, Arrays.copyOf(recon, recon.length), xValues.length,
                true);

        if (trafo1) {
            wvTrafo1.iwt97(recon, recon.length);
        } else {
            // wvTrafo1.iwt53(recon, recon.length);
            wvTrafo3.invTransform(recon);
        }

        final double error1 = TMath.RMS(TMath.Difference(yValues, yModel));
        final double error2 = TMath.RMS(TMath.Difference(recon, yModel));
        if (error1 > error2) {
            LOGGER.atInfo().log("improved noise floor from %f \t-> %f \t(%f %%)\n", error1, error2,
                    (error1 - error2) / error1 * 100);
        } else {
            LOGGER.atInfo().log("deteriorated noise floor from %f \t-> %f \t(%f %%)\n", error1, error2,
                    (error1 - error2) / error1 * 100);
        }

        fdata = new DefaultDataSet("model ", xValues, yModel, xValues.length, true);
        fraw = new DefaultDataSet("raw data", xValues, yValues, xValues.length, true);
        freconstructed = new DefaultDataSet("reconstructed", xValues, recon, xValues.length, true);
    }

    private double[][] readDemoData() {
        final String fileName = "./BBQSpectra.dat";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(WaveletScalogram.class.getResourceAsStream(fileName)))) {

            String line = reader.readLine();
            final int nDim = line == null ? 0 : Integer.parseInt(line);
            double[][] ret = new double[2][nDim];
            for (int i = 0; i < nDim; i++) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                final String[] x = line.split("\t");
                ret[0][i] = Double.parseDouble(x[0]);
                ret[1][i] = Double.parseDouble(x[1]);
            }

            return ret;

        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("read error");
            }
        }
        return new double[10][10];
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
