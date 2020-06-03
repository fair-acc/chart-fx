package de.gsi.math.samples;

import javafx.application.Application;
import javafx.scene.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.math.TMath;
import de.gsi.math.TMathConstants;
import de.gsi.math.fitter.NonLinearRegressionFitter;
import de.gsi.math.functions.AbstractFunction1D;
import de.gsi.math.samples.utils.AbstractDemoApplication;
import de.gsi.math.samples.utils.DemoChart;

/**
 * example illustrating fitting of a Gaussian Distribution
 * 
 * @author rstein
 */
public class GaussianFitSample extends AbstractDemoApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(GaussianFitSample.class);
    private static final int MAX_POINTS = 101;
    private DataSet fmodel;
    private DataSet fdataOrig;
    private DataSet fdataFitted;

    @Override
    public Node getContent() {
        initData();

        final DemoChart chart = new DemoChart();
        chart.getRenderer(0).getDatasets().addAll(fmodel, fdataOrig, fdataFitted);

        return chart;
    }

    private void initData() {
        // user specific fitting function, here: normalised Gaussian Function
        // y := scale*1/sqrt(2*Pi*sigma^2)*exp(-0.5*(x-mu)^2/sigma^2)
        // ... MyGaussianFunction(name, double[]{mu, sigma, scale})
        final MyGaussianFunction func = new MyGaussianFunction("gauss1", new double[] { -3.0, 1.0, 10.0 });
        LOGGER.atInfo().log("before fit");
        func.printParameters();

        double[] xValues = new double[MAX_POINTS];
        double[] yValues = new double[MAX_POINTS];
        double[] yModel = new double[MAX_POINTS];
        double[] yErrors = new double[MAX_POINTS];

        for (int i = 0; i < xValues.length; i++) {
            final double error = 0.5 * RANDOM.nextGaussian();
            xValues[i] = (i - xValues.length / 2.0) * 30.0 / MAX_POINTS; // equidistant
                    // sampling

            final double value = func.getValue(xValues[i]);
            // add some slope and offset to make the fit a bit more tricky
            // remember: in this example, the slope is not part of the fitting
            // check whether fit converged via chi^2
            // value += xValues[i]*0.1+0.5;
            // may converge depending on parameter values
            // if you need to fit this -> add a slope, offset parameter to your
            // Gaussian function

            yModel[i] = value;
            yValues[i] = value + error;
            yErrors[i] = Math.abs(error);
        }

        final NonLinearRegressionFitter fitter = new NonLinearRegressionFitter(xValues, yValues, yErrors);

        // initial estimates
        double[] start = new double[3];
        start[0] = 0.0; // initial estimate of mu
        start[1] = 1.0; // initial estimate of sigma
        start[2] = 0.6; // initial estimate of the scale

        // initial step sizes
        double[] step = new double[3];
        step[0] = 0.6; // initial step size for mu
        step[1] = 0.05; // initial step size for sigma
        step[2] = 0.1; // initial step size for scale
        fitter.simplex(func, start, step);

        final double[] fittedParameter = fitter.getBestEstimates();
        final double[] fittedParameterError = fitter.getBestEstimatesErrors();

        func.setParameterValues(fittedParameter);
        for (int i = 0; i < func.getParameterCount(); i++) {
            final double value = fittedParameter[i];
            final double error = fittedParameterError[i];
            func.setParameterRange(i, value - error, value + error);
        }

        final double[] yPredicted = func.getValues(xValues);
        final double[] yPredictedError = new double[yPredicted.length];

        LOGGER.atInfo().log("after fit");
        func.printParameters();

        LOGGER.atInfo().log("fit results chi^2 =" + fitter.getChiSquare() + ":");
        for (int i = 0; i < 3; i++) {
            LOGGER.atInfo().addArgument(func.getParameterName(i)).addArgument(start[i]).addArgument(fittedParameter[i]).addArgument(fittedParameterError[i]).log("fitted-parameter  '{}' = {} -> {} +- {}");
        }

        fmodel = new DefaultDataSet("design model", xValues, yModel, xValues.length, true);
        fdataOrig = new DefaultErrorDataSet("data seed with errors", xValues, yValues, yErrors, yErrors, xValues.length,
                true);
        fdataFitted = new DefaultErrorDataSet("fitted model", xValues, yPredicted, yPredictedError, yPredictedError,
                xValues.length, true);

        // plot fitting results
        // for (int i = 0; i < func.getParameterCount(); i++) {
        //     LOGGER.atInfo().addArgument(func.getParameterName(i)) //
        //             .addArgument(func.getParameterValue(i)) //
        //             .addArgument(0.5 * (func.getParameterRangeMaximum(i) - func.getParameterRangeMinimum(i))) //
        //             .log("fitted parameter '{}': {} +- {}"); //
        // }
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }

    /**
     * example fitting function y = scale/(sqrt(2*pi*sigma)*exp(- 0.5*(x-mu)^2/sigma^2)
     */
    protected class MyGaussianFunction extends AbstractFunction1D {
        public MyGaussianFunction(final String name, final double[] parameter) {
            super(name, new double[3]);
            // declare parameter names
            this.setParameterName(0, "mu");
            this.setParameterName(1, "sigma");
            this.setParameterName(2, "scale");

            if (parameter == null) {
                // set some default values
                setParameterValue(0, 0.0); // mu
                setParameterValue(0, 1.0); // sigma
                setParameterValue(0, 1.0); // scale
                return;
            }

            // assign default values
            final int maxIndex = TMathConstants.Min(parameter.length, this.getParameterCount());
            for (int i = 0; i < maxIndex; i++) {
                setParameterValue(i, parameter[i]);
            }
        }

        @Override
        public double getValue(final double x) {
            final double mu = fparameter[0];
            final double sigma = fparameter[1];
            final double scale = fparameter[2];

            return scale * 1.0 / (Math.sqrt(TMathConstants.TwoPi()) * sigma) * Math.exp(-0.5 * Math.pow((x - mu) / sigma, 2));
        }
    }
}
