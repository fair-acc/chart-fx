package io.fair_acc.math.fitter;

import java.security.InvalidParameterException;
import java.util.Random;

import io.fair_acc.math.functions.Function;
import io.fair_acc.math.functions.Function1D;
import io.fair_acc.math.functions.PolynomialFunction;
import io.fair_acc.math.matrix.MatrixD;
import io.fair_acc.math.matrix.SingularValueDecomposition;

/**
 * Simple linear regression fitter The fit is based on a local gradient matrix that is computed using the supplied
 * function. This fit works fine for polynomial function or other functions where scale-type factors need to be fitted
 * For non-linear fits (e.g. Gaussian, etc. ), please use 'NonLinearRegressionFitter'
 *
 * @author rstein last modified: 2011-07-25 exported fitted parameter values, errors, added Tikhonov regularisation,
 *         expanded/generalised interface
 */
public class LinearRegressionFitter {
    private static final String NULL_FUNCTION_WARN = "function pointer is null/is not defined or fit hasn't been run yet";
    public static boolean USE_SVD = true;

    private MatrixD forwardMatrix;

    private MatrixD errorMatrix;
    private MatrixD inverseMatrix;
    private MatrixD errorPropagationMatrix;
    private Function1D function;
    private double[] xValuesRef;
    private double[] yValuesRef;
    private double fsvdCutOff = 1e-16;

    private double ftikhonov = 1.0;
    private REG_METHOD fregularisationMethod = REG_METHOD.STANDARD;
    private boolean useErrors;

    private boolean isConverged;
    private long lastPrepareDuration = -1;
    private long lastFitDuration = -1;
    private double chiSquared = -1;
    private boolean fisSilent = true;
    private MatrixD flastFitResult;

    private MatrixD flastFitError;

    public LinearRegressionFitter() {
        // utility class
        reinitialise();
    }

    /**
     * Local linear regression fitter using standard SVD-type or Tikhonov regularisation The linear gradient matrix is
     * calculated using the provided @param func function template and for the given @param xValues vector. (Near-)
     * Singular values are rejected using a standard SVD-type regularisation (default) of setting the inverse of near or
     * singular eigenvalues of the gradient matrix to zero or using a Tikhonov based approach (aka. Wiener filter) of
     * reducing the weight of near-singular values.
     *
     * @param func the prototype function to fit the data to
     * @param xValues horizontal data coordinates
     * @param yValues vertical data coordinates
     * @return matrix three component vector containing matrix[0]: a copy of xValues matrix[1]: a computed estimate of
     *         the fitted yValues matrix[2]: a computed estimate of the fitted yValues errors
     */
    public synchronized double[][] fit(final Function func, final double[] xValues, final double[] yValues) {
        // invoke general private fitter function
        fitLocal(func, xValues, yValues);

        // compute fit estimates and transfer them to local variables
        final MatrixD valEstimate = forwardMatrix.times(flastFitResult);
        chiSquared = 0;
        final double[] yValuesPred = new double[yValuesRef.length];
        final double[] yValuesPredError = new double[yValuesRef.length];
        for (int i = 0; i < yValuesPred.length; i++) {
            yValuesPred[i] = valEstimate.get(i, 0);
        }

        final MatrixD error2 = flastFitError.copy();
        final MatrixD errEstimate = errorPropagationMatrix.times(error2);
        for (int i = 0; i < yValuesPred.length; i++) {
            yValuesPredError[i] = Math.sqrt(errEstimate.get(i, 0));
        }

        return new double[][] { xValuesRef, yValuesPred, yValuesPredError };
    }

    /**
     * Local linear regression fitter using standard SVD-type or Tikhonov regularisation The linear gradient matrix is
     * calculated using the provided @param func function template and for the given @param xValues vector. (Near-)
     * Singular values are rejected using a standard SVD-type regularisation (default) of setting the inverse of near or
     * singular eigenvalues of the gradient matrix to zero or using a Tikhonov based approach (aka. Wiener filter) of
     * reducing the weight of near-singular values.
     *
     * @param func the prototype function to fit the data to
     * @param xValues horizontal data coordinates
     * @param yValues vertical data coordinates
     * @param returnFunction copy with the adjusted fitted parameter function
     */
    public synchronized void fit(final Function func, final Function returnFunction, final double[] xValues,
            final double[] yValues) {
        // invoke genereal private fitter function
        fitLocal(func, xValues, yValues);

        if (returnFunction == null) {
            throw new InvalidParameterException("LinearRegressionFitter::fit(Function, Function, double[], double[]) - "
                                                + "return function pointer is null");
        }

        final int p1 = func.getParameterCount();
        final int p2 = returnFunction.getParameterCount();
        if (p1 >= p2) {
            throw new InvalidParameterException("LinearRegressionFitter::fit(Function, Function, double[], double[]) - "
                                                + "return parameter function has insufficient parameter count (" + p2 + " vs. required " + p1);
        }

        for (int i = 0; i < func.getParameterCount(); i++) {
            final double value = getParameter(i);
            final double error = getParError(i);

            returnFunction.setParameterValue(i, value);
            returnFunction.setParameterRange(i, value - error, value + error);
        }
    }

    /**
     * Local linear regression fitter using standard SVD-type or Tikhonov regularisation The linear gradient matrix is
     * calculated using the provided @param func function template and for the given @param xValues vector. (Near-)
     * Singular values are rejected using a standard SVD-type regularisation (default) of setting the inverse of near or
     * singular eigenvalues of the gradient matrix to zero or using a Tikhonov based approach (aka. Wiener filter) of
     * reducing the weight of near-singular values.
     *
     * @param func function to be fitted
     * @param xValues horizontal coordinates
     * @param yValues vertical coordinates
     */
    private synchronized void fitLocal(final Function func, final double[] xValues, final double[] yValues) {
        final String funcName = "RegressionFitter::fit(Function,";
        final String funcNameEmpty = funcName + " double[], double[]) - ";
        if (func == null) {
            throw new InvalidParameterException(funcNameEmpty + "function pointer is null");
        }
        if (xValues == null || yValues == null) {
            throw new InvalidParameterException(funcName + (xValues != null ? "double[]" : "null") + ", "
                                                + (yValues != null ? "double[]" : "null") + ") - "
                                                + "array pointer are null");
        }
        if (xValues.length != yValues.length) {
            throw new InvalidParameterException(funcName + "double[" + xValues.length + "], double[" + yValues.length
                                                + "]) - "
                                                + "array pointer size mis-match");
        }
        if (func.getInputDimension() != 1) {
            throw new InvalidParameterException(
                    funcNameEmpty + " for the time being: only one dimensional fits implemented");
        }
        if (func.getFreeParameterCount() > yValues.length) {
            throw new InvalidParameterException(
                    funcNameEmpty + " cannot fit function with more free parameters than data points "
                    + "("
                    + func.getParameterCount() + " vs. " + yValues.length + ")");
        }

        final long start = System.currentTimeMillis();
        boolean recomputeMatrices = true;
        reinitialise();

        function = (Function1D) func;
        xValuesRef = java.util.Arrays.copyOf(xValues, xValues.length);
        yValuesRef = java.util.Arrays.copyOf(yValues, yValues.length);

        function.setFitterMode(true);

        if (recomputeMatrices) {
            final double[][] temp = new double[yValuesRef.length][function.getParameterCount()];
            for (int i = 0; i < xValuesRef.length; i++) {
                final double x = xValues[i];

                // drop zero or near-singular eigenvalues
                // TODO: add Tikonov regularisation
                for (int j = 0; j < function.getParameterCount(); j++) {
                    if (!function.isParameterFixed(j)) {
                        function.clearParameterValues();
                        function.setParameterValue(j, 1.0);
                        temp[i][j] = function.getValue(x);
                    } else {
                        temp[i][j] = 0.0;
                    }
                }
            }
            function.clearParameterValues();

            forwardMatrix = new MatrixD(temp);

            errorPropagationMatrix = (MatrixD) forwardMatrix.clone();
            errorPropagationMatrix.squareElements();

            if (USE_SVD) {
                // use SVD
                final SingularValueDecomposition decomp = forwardMatrix.svd();
                final double[] sig = decomp.getSingularValues();
                final MatrixD newS = new MatrixD(sig.length, sig.length);
                final double first = sig[0];

                switch (fregularisationMethod) {
                case TIKHONOV:
                    // Tikonhov regularisation
                    // lambda_i -> lambda_i/(lambda_i^2+mu^2)
                    for (int i = 0; i < sig.length; i++) {
                        if (sig[i] != 0) {
                            if (ftikhonov > 0) {
                                newS.set(i, i, sig[i] / (sig[i] * sig[i] + ftikhonov * ftikhonov));
                            } else if (Math.abs(sig[i]) > 1e-16) {
                                newS.set(i, i, 1.0 / sig[i]);
                            } else {
                                newS.set(i, i, 0.0);
                                if (!fisSilent) {
                                    System.out.println("drop singular eigenvalue " + i);
                                }
                            }
                        } else {
                            newS.set(i, i, 0.0);
                            if (!fisSilent) {
                                System.out.println("drop singular eigenvalue " + i);
                            }
                        }
                    }

                    break;
                case STANDARD:
                default:
                    // simple regularisation by dropping (near-) singular
                    // eigenvalues
                    for (int i = 0; i < sig.length; i++) {
                        if (sig[i] / first < fsvdCutOff || sig[i] < 1e-16) {
                            // discard eigenvalue
                            if (!fisSilent) {
                                System.out.println("drop singular eigenvalue " + i);
                            }
                            newS.set(i, i, 0.0);
                        } else {
                            newS.set(i, i, 1.0 / sig[i]);
                        }
                    }
                    decomp.rank();
                }

                inverseMatrix = decomp.getV().times(newS).times(decomp.getU().transpose());
            } else {
                // use QR factorisation
                // (may not converge in the presence of singular values)
                inverseMatrix = forwardMatrix.inverse();
            }

            // compute the error of the linear regression
            errorMatrix = (MatrixD) inverseMatrix.clone();
            errorMatrix.squareElements();
        }
        function.setFitterMode(false);

        // compute the linear regression
        flastFitResult = inverseMatrix.times(new MatrixD(yValues, yValues.length));
        if (flastFitResult == null) {
            throw new IllegalArgumentException("could not generate fit results: null-vector");
        }

        chiSquared = 0;
        final double[] yValuesPred = new double[yValuesRef.length];
        final MatrixD valEstimate = forwardMatrix.times(flastFitResult);
        for (int i = 0; i < yValuesRef.length; i++) {
            yValuesPred[i] = valEstimate.get(i, 0);
            chiSquared += Math.pow(yValuesPred[i], 2) / Math.abs(yValuesRef[i]);
        }
        final MatrixD errorVector = new MatrixD(yValuesPred, yValuesPred.length);
        flastFitError = errorMatrix.times(errorVector);

        isConverged = true;
        final long stop = System.currentTimeMillis();
        lastPrepareDuration = stop - start;
        lastFitDuration = stop - start;

        // some debug output
        if (!fisSilent) {
            for (int i = 0; i < function.getParameterCount(); i++) {
                System.out.printf("parameter %d (orig. vs. fit.): %+f vs. %+f ( %+f <-> %+f) \n", i,
                        function.getParameterValue(i), flastFitResult.get(i, 0),
                        Math.abs(function.getParameterValue(i) - flastFitResult.get(i, 0))
                                / function.getParameterValue(i),
                        flastFitError.get(i, 0) / function.getParameterValue(i));
            }
        }
    }

    /**
     * Get the best estimates of the unknown parameters.
     *
     * @return best estimate value
     */
    public synchronized double[] getBestEstimates() {
        if (flastFitResult == null) {
            throw new InvalidParameterException("LinearRegressionFitter::getBestEstimates() - " + NULL_FUNCTION_WARN);
        }
        final double[] ret = new double[flastFitResult.getRowDimension()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = flastFitResult.get(i, 0);
        }
        return ret;
    }

    /**
     * Get the estimates of the standard deviations of the best estimates of the unknown parameters.
     *
     * @return parameter best estimates error
     */
    public synchronized double[] getBestEstimatesErrors() {
        if (flastFitError == null) {
            throw new InvalidParameterException(
                    "LinearRegressionFitter::getBestEstimatesErrors() - " + NULL_FUNCTION_WARN);
        }
        final double[] ret = new double[flastFitError.getRowDimension()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = flastFitError.get(i, 0);
        }
        return ret;
    }

    /**
     * @return the \chi^2 value of the last fit
     */
    public synchronized double getChiSquared() {
        return chiSquared;
    }

    /**
     * @return last fitting execution time [ms]
     */
    public synchronized long getLastFitDuration() {
        return lastFitDuration;
    }

    /**
     * @return last fitting execution time including preparation [ms]
     */
    public synchronized long getLastPrepareDuration() {
        return lastPrepareDuration;
    }

    /**
     * Value of fitted parameter.
     *
     * @param index of parameter
     * @return parameter value
     */
    public synchronized double getParameter(final int index) {
        if (flastFitResult == null) {
            throw new InvalidParameterException("LinearRegressionFitter::getParameter(int) - " + NULL_FUNCTION_WARN);
        }
        if (index < 0 || index >= flastFitResult.getRowDimension()) {
            throw new InvalidParameterException("LinearRegressionFitter::getParameter(" + index + ") - "
                                                + "requested invalid parameter index [0," + flastFitResult.getRowDimension() + "]");
        }
        return flastFitResult.get(index, 0);
    }

    /**
     * Error estimate of fitted parameter N.B. estimate based on assumption that measurement errors are uncorrelated
     *
     * @param index of parameter
     * @return parameter error estimation value
     */
    public synchronized double getParError(final int index) {
        if (flastFitError == null) {
            throw new InvalidParameterException("LinearRegressionFitter::getParameter(int) - " + NULL_FUNCTION_WARN);
        }
        if (index < 0 || index >= flastFitError.getRowDimension()) {
            throw new InvalidParameterException("LinearRegressionFitter::getParameter(" + index + ") - "
                                                + "requested invalid parameter index [0," + flastFitError.getRowDimension() + "]");
        }
        return flastFitError.get(index, 0);
    }

    /**
     * @return kStandard: standard SVD-type cut<br>
     *
     *         1/lambda_i -&gt; 1/lambda_i<br>
     *         for (|lambda_i/lambda_0| &gt; getSVDCutOff &amp; |1/lambda_i| &gt; 1e-16 1/lambda_i -&gt; 0 <br>
     *         otherwise (numerical stability) Tikhonov type regularisation:<br>
     *         1/lambda_i -&gt; lambda_i/(lambda_i^2+mu^2) for |1/lambda_i| &gt; 1e-16 1/lambda_i<br>
     *
     *         the parameter mu can be read set via function getTikhonovRegularisationParameter, or function
     *         setTikhonovRegularisationParameter
     */
    public synchronized REG_METHOD getRegularisationMethod() {
        return fregularisationMethod;
    }

    /**
     * @return SVD cut off threshold for ill-conditioned systems, default: 1e-16
     */
    public synchronized double getSVDCutOff() {
        return fsvdCutOff;
    }

    /**
     * 1/lambda_i -&gt; lambda_i/(lambda_i^2+mu^2) for |1/lambda_i| &gt; 1e-16<br>
     * 1/lambda_i -&gt; 0 (for numerical stability)<br>
     *
     * @return Tikhonov regularisation parameter mu, default: 1.0
     */
    public synchronized double getTikhonovRegularisation() {
        return ftikhonov;
    }

    /**
     * @return last fit converged successfully
     */
    public synchronized boolean isConverged() {
        return isConverged;
    }

    /**
     * @return true: errors are used as fitting weights
     */
    public synchronized boolean isErrorWeighting() {
        return useErrors;
    }

    /**
     * Fitter verbosity (local print-out) is enabled... for debugging purposes
     *
     * @return whether fitter is silent (ie. not outputting debugging/info messages
     */
    public synchronized boolean isSilent() {
        return fisSilent;
    }

    private synchronized void reinitialise() {
        forwardMatrix = null;
        errorMatrix = null;
        inverseMatrix = null;
        errorPropagationMatrix = null;
        function = null;
        xValuesRef = null;
        yValuesRef = null;

        useErrors = false;
        isConverged = false;
        lastPrepareDuration = -1;
        lastFitDuration = -1;
        chiSquared = -1;
    }

    /**
     * @param state true: use errors as fitting weights
     */
    public synchronized void setErrorWeighting(final boolean state) {
        useErrors = state;
    }

    /**
     * KStandard: standard SVD-type cut<br>
     *
     * 1/lambda_i -&gt; 1/lambda_i<br>
     * for (|lambda_i/lambda_0| &gt; getSVDCutOff &amp; |1/lambda_i| &gt; 1e-16 1/lambda_i -&gt; 0 <br>
     * otherwise (numerical stability) Tikhonov type regularisation:<br>
     * 1/lambda_i -&gt; lambda_i/(lambda_i^2+mu^2) for |1/lambda_i| &gt; 1e-16 1/lambda_i<br>
     *
     * @param method regularisation method to be used
     *
     *        the parameter mu can be read set via function getTikhonovRegularisationParameter, or function
     *        setTikhonovRegularisationParameter
     */
    public synchronized void setRegularisationMethod(final REG_METHOD method) {
        fregularisationMethod = method;
    }

    /**
     * SVD cut off threshold for ill-conditioned systems, default: 1e-16
     *
     * @param cutOff minimum eigenvalue cut-off
     */
    public synchronized void setSVDCutOff(final double cutOff) {
        fsvdCutOff = cutOff;
    }

    /**
     * Tikhonov regularisation parameter mu, default: <br>
     *
     * 1.0 1/lambda_i -&gt; lambda_i/(lambda_i^2+mu^2) for |1/lambda_i| &gt; 1e-16 1/lambda_i -&gt; 0 (for numerical
     * stability)
     *
     * @param cutOff minimum eigenvalue cut-off
     */
    public synchronized void setTikhonovRegularisation(final double cutOff) {
        ftikhonov = cutOff;
    }

    /**
     * Fitter verbosity (local print-out) is enabled... for debugging purposes
     *
     * @param state true &lt;-&gt; 'on', false &lt;-&gt; 'off'
     */
    public synchronized void setVerbosity(final boolean state) {
        fisSilent = !state;
    }

    public static void main(final String[] args) {
        // define third order polynomial function
        // y = 0.5 + 1*x + 2*x^2 + 0.6*x^3
        final PolynomialFunction func = new PolynomialFunction("poly1", new double[] { 0.5, 1, 2, 0.6, 1e-4 });

        final double[] xValues = new double[20];
        final double[] yValues = new double[20];

        final Random rnd = new Random();
        for (int i = 0; i < xValues.length; i++) {
            final double error = rnd.nextGaussian();
            xValues[i] = i - xValues.length / 2.0;
            yValues[i] = func.getValue(xValues[i]) + 0.1 * error;
            // System.out.printf("%+2d: poly(%+4.2f) = %f\n", i, xValues[i],
        }

        func.fixParameter(0, true);

        final LinearRegressionFitter fitter = new LinearRegressionFitter();
        // final double[][] result = fitter.fit(func, xValues, yValues);
        fitter.fit(func, xValues, yValues);

        System.out.println("elapsed time " + fitter.getLastFitDuration() + " ms,  ch2 = " + fitter.getChiSquared());
    }

    public enum REG_METHOD {
        STANDARD,
        TIKHONOV
    }
}
