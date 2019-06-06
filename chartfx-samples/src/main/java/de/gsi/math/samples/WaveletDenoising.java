package de.gsi.math.samples;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.math.TMath;
import de.gsi.math.TMathConstants;
import de.gsi.math.samples.utils.AbstractDemoApplication;
import de.gsi.math.samples.utils.DemoChart;
import de.gsi.math.functions.RandomWalkFunction;
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

    private final int MAX_POINTS = 512;
    public static boolean LOAD_EXAMPLE_DATA = false;
    private DataSet fraw;
    private DataSet fdata;
    private DataSet freconstructed;
    private DataSet fspectraModel, fspectra, fspectraFit;

    private void initData() {
        // third order polynomial function
        RandomWalkFunction func = new RandomWalkFunction("rand1", 0.1);

        double[] xValues, yValues, yModel;

        if (!LOAD_EXAMPLE_DATA) {
            xValues = new double[MAX_POINTS];
            yValues = new double[MAX_POINTS];
            yModel = new double[MAX_POINTS];

            Random rnd = new Random();
            for (int i = 0; i < xValues.length; i++) {
                double x = i;
                double offset = 0;
                double error = 0.2 * rnd.nextGaussian();
                if (i > xValues.length / 2) {
                    offset = -0.05;
                }
                xValues[i] = x;
                yValues[i] = func.getValue(0);
                yModel[i] = Math.sin(TMathConstants.TwoPi() * 3e-4 * x * (x + offset));
                yModel[i] = Math.sin(TMathConstants.TwoPi() * 0.05 * x);

                if (true)
                    if (i < 100 || i > 400) {
                        yModel[i] = 0;
                    }

                yValues[i] = yModel[i] + 0 * error;
            }

        } else {
            double[][] data = readDemoData();
            xValues = data[0];
            yValues = data[1];
            yModel = Arrays.copyOf(yValues, yValues.length);
        }

        CDFWavelet wvTrafo1 = new CDFWavelet();
        FastWaveletTransform wvTrafo3 = new FastWaveletTransform();
        boolean trafo1 = false;

        double[] ySmooth = Arrays.copyOf(yValues, yValues.length);
        double[] ySModel = Arrays.copyOf(yModel, yModel.length);

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

        double[] recon = Arrays.copyOf(ySmooth, yValues.length);
        double[] reconAbs = Arrays.copyOf(ySmooth, yValues.length);
        for (int i = 0; i < reconAbs.length; i++) {
            reconAbs[i] = Math.abs(recon[i]);
        }

        //        double max = TMath.Maximum(reconAbs);
        //        double min = TMath.Minimum(reconAbs);
        //        double median = TMath.Median(reconAbs);

        // Percival and Walden, "Wavelet Methods for Time Series Analysis",
        // Cambridge University Press, 2006, Section 10.5
        // David L. Donoho and Iain M. Johnstone, "Minimax Estimation via
        // Wavelet Shrinkage", The Annals of Statistics, 1998, Vol. 26, No. 3,
        // 879 921

        // classic implementation
        // double lambda = median / 0.6745 * Math.sqrt(Math.log(recon.length));
        // double norm = 1.0 / (1.0 - lambda);
        //            for (int i = 0; i < xValues.length; i++) {
        //                double val = recon[i];
        //
        //                // hard threshold
        //                if (true)
        //                    if (Math.abs(val) < lambda) {
        //                        recon[i] = 0.0;
        //                    }
        //
        //                // soft threshold
        //                if (false)
        //                    if (Math.abs(val) < lambda) {
        //                        recon[i] = 0.0;
        //                    } else {
        //                        recon[i] -= Math.signum(val) * lambda;
        //                        recon[i] *= norm;
        //                    }
        //            }

        // test
        //            for (int i = 0; i < xValues.length / 2; i++) {
        //                final double val = Math.abs(recon[i]);
        //                final double err = Math.abs(recon[xValues.length / 2 + i]);
        //
        //                if (val < lambda) {
        //                    recon[i] = 0.0;
        //                    recon[xValues.length / 2 + i] = 0.0;
        //                } else if (val != 0 && err < val && (err / val < lambda)) {
        //                    recon[xValues.length / 2 + i] = 0.0;
        //                } else if (val != 0 && err > val && (val / err < lambda)) {
        //                    recon[i] = 0.0;
        //                }
        //
        //            }

        for (int i = 0; i < 0; i++) {
            recon[i] = 0.0;
        }

        fspectraModel = new DefaultErrorDataSet("model", xValues, ySModel);
        fspectra = new DefaultErrorDataSet("raw data", xValues, ySmooth);
        fspectraFit = new DefaultErrorDataSet("reconstructed", xValues, Arrays.copyOf(recon, recon.length));

        if (trafo1) {
            wvTrafo1.iwt97(recon, recon.length);
        } else {
            // wvTrafo1.iwt53(recon, recon.length);
            wvTrafo3.invTransform(recon);
        }

        double error1 = TMath.RMS(TMath.Difference(yValues, yModel));
        double error2 = TMath.RMS(TMath.Difference(recon, yModel));
        if (error1 > error2) {
            System.out.printf("improved noise floor from %f \t-> %f \t(%f %%)\n", error1, error2,
                    (error1 - error2) / error1 * 100);
        } else {
            System.err.printf("deteriorated noise floor from %f \t-> %f \t(%f %%)\n", error1, error2,
                    (error1 - error2) / error1 * 100);
        }

        fdata = new DefaultErrorDataSet("model ", xValues, yModel);
        fraw = new DefaultErrorDataSet("raw data", xValues, yValues);
        freconstructed = new DefaultErrorDataSet("reconstructed", xValues, recon);
    }

    @Override
    public Node getContent() {
        initData();

        DemoChart chart1 = new DemoChart();
        chart1.getXAxis().setLabel("time");
        chart1.getDatasets().addAll(fdata, fraw, freconstructed);

        DemoChart chart2 = new DemoChart();
        chart2.getXAxis().setLabel("frequency");
        chart2.getDatasets().addAll(fspectraModel, fspectra, fspectraFit);

        return new VBox(chart1, chart2);
    }

    private double[][] readDemoData() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(WaveletScalogram.class.getResourceAsStream("./BBQSpectra.dat")));

            int nDim = Integer.parseInt(reader.readLine());
            double[][] ret = new double[2][nDim];
            for (int i = 0; i < nDim; i++) {
                String[] x = reader.readLine().split("\t");
                ret[0][i] = Double.parseDouble(x[0]);
                ret[1][i] = Double.parseDouble(x[1]);
            }

            return ret;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
