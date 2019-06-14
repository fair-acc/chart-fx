package de.gsi.math.samples;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.math.TMath;
import de.gsi.math.samples.utils.AbstractDemoApplication;
import de.gsi.math.samples.utils.DemoChart;
import de.gsi.math.fitter.NonLinearRegressionFitter;
import de.gsi.math.functions.AbstractFunction1D;
import javafx.application.Application;
import javafx.scene.Node;

/**
 * example illustrating fitting of a Gaussian Distribution
 * 
 * @author rstein
 */
public class GaussianFitSample extends AbstractDemoApplication {

    private static final int MAX_POINTS = 101;
    private DataSet fmodel;
    private DataSet fdataOrig;
    private DataSet fdataFitted;

    private void initData() {
        // user specific fitting function, here: normalised Gaussian Function
        // y := scale*1/sqrt(2*Pi*sigma^2)*exp(-0.5*(x-mu)^2/sigma^2)
        // ... MyGaussianFunction(name, double[]{mu, sigma, scale})
        MyGaussianFunction func = new MyGaussianFunction("gauss1", new double[] { -3.0, 1.0, 10.0 });
        System.err.println("before fit");
        func.printParameters();

        double[] xValues = new double[MAX_POINTS];
        double[] yValues = new double[MAX_POINTS];
        double[] yModel = new double[MAX_POINTS];
        double[] yErrors = new double[MAX_POINTS];

        for (int i = 0; i < xValues.length; i++) {
            double error = 0.5 * RANDOM.nextGaussian();
            xValues[i] = (i - xValues.length / 2.0) * 30.0 / MAX_POINTS; // equidistant
                                                                         // sampling

            double value = func.getValue(xValues[i]);
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

        NonLinearRegressionFitter fitter = new NonLinearRegressionFitter(xValues, yValues, yErrors);

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

        double[] fittedParameter = fitter.getBestEstimates();
        double[] fittedParameterError = fitter.getBestEstimatesErrors();

        func.setParameterValues(fittedParameter);
        for (int i = 0; i < func.getParameterCount(); i++) {
            double value = fittedParameter[i];
            double error = fittedParameterError[i];
            func.setParameterRange(i, value - error, value + error);
        }

        double[] yPredicted = func.getValues(xValues);
        double[] yPredictedError = new double[yPredicted.length];

        System.err.println("after fit");
        func.printParameters();

        System.out.println("fit results chi^2 =" + fitter.getChiSquare() + ":");
        for (int i = 0; i < 3; i++) {
            System.out.printf("fitted-parameter  '%s' = %f -> %f +- %f\n", func.getParameterName(i), start[i],
                    fittedParameter[i], fittedParameterError[i]);
        }

        fmodel = new DoubleErrorDataSet("design model", xValues, yModel);
        fdataOrig = new DefaultErrorDataSet("data seed with errors", xValues, yValues, yErrors);
        fdataFitted = new DefaultErrorDataSet("fitted model", xValues, yPredicted, yPredictedError);

        // plot fitting results
        /*
         * for (int i=0; i < func.getParameterCount(); i++) {
         * System.out.printf("fitted parameter '%s': %f +- %f\n",
         * func.getParameterName(i), func.getParameterValue(i),
         * 0.5*(func.getParameterRangeMaximum(i)-func.getParameterRangeMinimum(i
         * ))); }
         */
    }

    @Override
    public Node getContent() {
        initData();

        DemoChart chart = new DemoChart();
        chart.getRenderer(0).getDatasets().addAll(fmodel, fdataOrig, fdataFitted);

        return chart;
    }

    /**
     * example fitting function y = scale/(sqrt(2*pi*sigma)*exp(- 0.5*(x-mu)^2/sigma^2)
     */
    class MyGaussianFunction extends AbstractFunction1D {

        public MyGaussianFunction(String name, double[] parameter) {
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
            final int maxIndex = TMath.Min(parameter.length, this.getParameterCount());
            for (int i = 0; i < maxIndex; i++) {
                setParameterValue(i, parameter[i]);
            }
        }

        @Override
        public double getValue(double x) {
            double mu = fparameter[0];
            double sigma = fparameter[1];
            double scale = fparameter[2];
            double y = scale * 1.0 / (Math.sqrt(TMath.TwoPi()) * sigma)
                    * Math.exp(-0.5 * Math.pow((x - mu) / sigma, 2));
            return y;
        }
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }

}
