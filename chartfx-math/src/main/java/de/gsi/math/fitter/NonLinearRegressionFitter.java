package de.gsi.math.fitter;

import java.util.ArrayList;

import de.gsi.math.ArrayConversion;
import de.gsi.math.TMath;
import de.gsi.math.TMathConstants;
import de.gsi.math.functions.Function;
import de.gsi.math.functions.Function1D;
import de.gsi.math.functions.FunctionND;
import de.gsi.math.matrix.MatrixD;

/**
 * Non-linear regression class Nelder &amp; Mead simplex algorithm being the primary back-bone of this implementation
 * initial implementation based on the package provided by: Michael Thomas Flanagan at www.ee.ucl.ac.uk/~mflanaga The
 * code has been cleaned up and adapted to further support multi-dimensional fits
 *
 * @author rstein
 */
public class NonLinearRegressionFitter {

    // HISTOGRAM CONSTRUCTION
    // Tolerance used in including an upper point in last histogram bin when it is outside due to riunding erors
    protected static double histTol = 1.0001D;
    protected int nData0 = 0; // number of y data points inputed (in a single array if multiple y arrays)
    protected int nData = 0; // number of y data points (nData0 times the number of y arrays)
    protected int nXarrays = 1; // number of x arrays
    protected int nYarrays = 1; // number of y arrays
    protected int nTerms = 0; // number of unknown parameters to be estimated
    // multiple linear (a + b.x1 +c.x2 + . . ., = nXarrays + 1
    // polynomial fitting; = polynomial degree + 1
    // generalised linear; = nXarrays
    // simplex = no. of parameters to be estimated
    protected int degreesOfFreedom = 0; // degrees of freedom = nData - nTerms
    protected double[][] xData = null; // x data values
    protected double[] yData = null; // y data values
    protected double[] yCalc = null; // calculated y values using the regression coefficients
    protected double[] weight = null; // weighting factors
    protected double[] residual = null; // residuals
    protected double[] residualW = null; // weighted residuals
    protected boolean weightOpt = false; // weighting factor option

    // = true; weights supplied
    // = false; weights set to unity in regression
    // average error used in statistical methods
    // if any weight[i] = zero,
    // weighOpt is set to false and
    // all weights set to unity
    protected int weightFlag = 0; // weighting flag - weightOpt = false, weightFlag = 0; weightOpt = true, weightFlag =
                                  // 1
    protected double[] best = null; // best estimates vector of the unknown parameters
    protected double[] bestSd = null; // standard deviation estimates of the best estimates of the unknown parameters
    protected double[] pseudoSd = null; // Pseudo-nonlinear sd
    protected double[] tValues = null; // t-values of the best estimates

    protected double[] pValues = null; // p-values of the best estimates
    protected double chiSquare = Double.NaN; // chi square (observed-calculated)^2/variance
    protected double reducedChiSquare = Double.NaN; // reduced chi square
    protected double sumOfSquares = Double.NaN; // Sum of the squares of the residuals
    protected double lastSSnoConstraint = 0.0D; // Last sum of the squares of the residuals with no constraint penalty
    protected double[][] covar = null; // Covariance matrix
    protected double[][] corrCoeff = null; // Correlation coefficient matrix
    protected double sampleR = Double.NaN; // coefficient of determination
    protected double sampleR2 = Double.NaN; // sampleR2 = sampleR*sampleR
    protected double adjustedR = Double.NaN; // adjusted coefficient of determination
    protected double adjustedR2 = Double.NaN; // adjustedR2 = adjustedR*adjustedR
    protected double multipleF = Double.NaN; // Multiple correlation coefficient F ratio

    protected double adjustedF = Double.NaN; // Adjusted Multiple correlation coefficient F ratio
    protected boolean linNonLin = true; // if true linear method, if false non-linear method

    protected boolean trueFreq = false; // true if xData values are true frequencies, e.g. in a fit to Gaussian
    // false if not
    // if true chiSquarePoisson (see above) is also calculated
    // Non-linear members
    protected boolean nlrStatus = true; // Status of non-linear regression on exiting regression method
    // = true - convergence criterion was met
    // = false - convergence criterion not met - current estimates returned
    protected int scaleOpt = 0; // if = 0; no scaling of initial estimates
    // if = 1; initial simplex estimates scaled to unity
    // if = 2; initial estimates scaled by user provided values in scale[]
    // (default = 0)
    protected double[] fscale = null; // values to scale initial estimate (see scaleOpt above)
    protected boolean zeroCheck = false; // true if any best estimate value is zero
    // if true the scale factor replaces the best estimate in numerical differentiation
    protected boolean penalty = false; // true if single parameter penalty function is included
    protected boolean sumPenalty = false; // true if multiple parameter penalty function is included
    protected int nConstraints = 0; // number of single parameter constraints
    protected int nSumConstraints = 0; // number of multiple parameter constraints
    protected int maxConstraintIndex = -1; // maximum index of constrained parameter/s
    protected double constraintTolerance = 1e-4; // tolerance in constraining parameter/s to a fixed value
    protected ArrayList<Object> penalties = new ArrayList<>(); // 3 method index,
    // number of single parameter constraints,
    // then repeated for each constraint:
    // penalty parameter index,
    // below or above constraint flag,
    // constraint boundary value
    protected ArrayList<Object> sumPenalties = new ArrayList<>(); // constraint method index,
    // number of multiple parameter constraints,
    // then repeated for each constraint:
    // number of parameters in summation
    // penalty parameter indices,
    // summation signs
    // below or above constraint flag,
    // constraint boundary value
    protected int[] penaltyCheck = null; // = -1 values below the single constraint boundary not allowed
    // = +1 values above the single constraint boundary not allowed
    protected int[] sumPenaltyCheck = null; // = -1 values below the multiple constraint boundary not allowed
    // = +1 values above the multiple constraint boundary not allowed
    protected double penaltyWeight = 1.0e30; // weight for the penalty functions
    protected int[] penaltyParam = null; // indices of paramaters subject to single parameter constraint
    protected int[][] sumPenaltyParam = null; // indices of paramaters subject to multiple parameter constraint
    protected double[][] sumPlusOrMinus = null; // valueall before each parameter in multiple parameter summation

    protected int[] sumPenaltyNumber = null; // number of paramaters in each multiple parameter constraint
    protected double[] constraints = null; // single parameter constraint values
    protected double[] sumConstraints = null; // multiple parameter constraint values

    protected int constraintMethod = 0; // constraint method number
    // =0: cliff to the power two (only method at present)
    protected int nMax = 3000; // Nelder and Mead simplex maximum number of iterations
    protected int nIter = 0; // Nelder and Mead simplex number of iterations performed
    protected int konvge = 3; // Nelder and Mead simplex number of restarts allowed
    protected int kRestart = 0; // Nelder and Mead simplex number of restarts taken
    protected double fMin = -1.0D; // Nelder and Mead simplex minimum value
    protected double fTol = 1e-9; // Nelder and Mead simplex convergence tolerance
    protected double rCoeff = 1.0D; // Nelder and Mead simplex reflection coefficient
    protected double eCoeff = 2.0D; // Nelder and Mead simplex extension coefficient
    protected double cCoeff = 0.5D; // Nelder and Mead simplex contraction coefficient
    protected double[] startH = null; // Nelder and Mead simplex initial estimates
    protected double[] step = null; // Nelder and Mead simplex step values
    protected double dStep = 0.5D; // Nelder and Mead simplex default step value
    protected double[][] grad = null; // Non-linear regression gradients
    protected double delta = 1e-4; // Fractional step in numerical differentiation
    protected boolean invertFlag = true; // Hessian Matrix ('linear' non-linear statistics) check
    // true matrix successfully inverted, false inversion failed
    protected boolean posVarFlag = true; // Hessian Matrix ('linear' non-linear statistics) check
    // true - all variances are positive; false - at least one is negative
    protected int minTest = 0; // Nelder and Mead minimum test
    // = 0; tests simplex sd < fTol
    // = 1; tests reduced chi suare or sum of squares < mean of abs(y values)*fTol
    protected double simplexSd = 0.0D; // simplex standard deviation
    protected boolean statFlag = true; // if true - statistical method called

    // if false - no statistical analysis //
    protected boolean multipleY = false;
    // = true if y variable consists of more than set of data each needing a different calculation in RegressionFunction
    // when set to true - the index of the y value is passed to the function in Regression function
    protected double[] values = null; // values entered into gaussianFixed

    protected boolean[] fixed = null; // true if above values[i] is fixed, false if it is not

    protected boolean ignoreDofFcheck = false; // when set to true, the check on whether degrees of freedom are greater
                                               // than zero is ignored

    protected boolean nFactorOption = false; // = true varaiance, covariance and standard deviation denominator = n
    // = false varaiance, covariance and standard deviation denominator = n-1

    /**
     * Constructor with data with x as 1D array and no weights provided
     *
     * @param xxData ???
     * @param yData ???
     */
    public NonLinearRegressionFitter(final double[] xxData, final double[] yData) {
        nData0 = yData.length;
        final int n = xxData.length;
        final double[][] xData = new double[1][n];
        final double[] weight = new double[n];

        for (int i = 0; i < n; i++) {
            xData[0][i] = xxData[i];
        }

        weightOpt = false;
        weightFlag = 0;
        for (int i = 0; i < n; i++) {
            weight[i] = 1.0D;
        }

        setDefaultValues(xData, yData, weight);
    }

    /**
     * Constructor with data with x as 1D array and weights provided
     *
     * @param xxData ???
     * @param yData ???
     * @param weight ???
     */
    public NonLinearRegressionFitter(final double[] xxData, final double[] yData, double[] weight) {
        nData0 = yData.length;
        final int n = xxData.length;
        final double[][] xData = new double[1][n];
        for (int i = 0; i < n; i++) {
            xData[0][i] = xxData[i];
        }

        weight = checkForZeroWeights(weight);
        if (weightOpt) {
            weightFlag = 1;
        }
        setDefaultValues(xData, yData, weight);
    }

    /**
     * Constructor with data with x as 1D array and y as a 2D array and no weights provided
     *
     * @param xxData ???
     * @param yyData ???
     */
    public NonLinearRegressionFitter(final double[] xxData, final double[][] yyData) {
        multipleY = true;
        final int nY1 = yyData.length;
        nYarrays = nY1;
        final int nY2 = yyData[0].length;
        nData0 = nY2;
        final double[] yData = new double[nY1 * nY2];
        int ii = 0;
        for (int i = 0; i < nY1; i++) {
            final int nY = yyData[i].length;
            if (nY != nY2) {
                throw new IllegalArgumentException("multiple y arrays must be of the same length");
            }
            for (int j = 0; j < nY2; j++) {
                yData[ii] = yyData[i][j];
                ii++;
            }
        }

        final double[][] xData = new double[1][nY1 * nY2];
        final double[] weight = new double[nY1 * nY2];

        ii = 0;
        final int n = xxData.length;
        for (int j = 0; j < nY1; j++) {
            for (int i = 0; i < n; i++) {
                xData[0][ii] = xxData[i];
                weight[ii] = 1.0D;
                ii++;
            }
        }
        weightOpt = false;
        weightFlag = 0;

        setDefaultValues(xData, yData, weight);
    }

    /**
     * Constructor with data with x as 1D array and y as 2D array and weights provided
     *
     * @param xxData ???
     * @param yyData ???
     * @param wWeight ???
     */
    public NonLinearRegressionFitter(final double[] xxData, final double[][] yyData, final double[][] wWeight) {

        multipleY = true;
        final int nY1 = yyData.length;
        nYarrays = nY1;
        final int nY2 = yyData[0].length;
        nData0 = nY2;
        final double[] yData = new double[nY1 * nY2];
        double[] weight = new double[nY1 * nY2];
        int ii = 0;
        for (int i = 0; i < nY1; i++) {
            final int nY = yyData[i].length;
            if (nY != nY2) {
                throw new IllegalArgumentException("multiple y arrays must be of the same length");
            }
            for (int j = 0; j < nY2; j++) {
                yData[ii] = yyData[i][j];
                weight[ii] = wWeight[i][j];
                ii++;
            }
        }
        final int n = xxData.length;
        if (n != nY2) {
            throw new IllegalArgumentException("x and y data lengths must be the same");
        }
        final double[][] xData = new double[1][nY1 * n];
        ii = 0;
        for (int j = 0; j < nY1; j++) {
            for (int i = 0; i < n; i++) {
                xData[0][ii] = xxData[i];
                ii++;
            }
        }

        weight = checkForZeroWeights(weight);
        if (weightOpt) {
            weightFlag = 1;
        }
        setDefaultValues(xData, yData, weight);
    }

    /**
     * Constructor with data with x as 2D array and no weights provided
     *
     * @param xData ???
     * @param yData ???
     */
    public NonLinearRegressionFitter(final double[][] xData, final double[] yData) {
        nData0 = yData.length;
        final int n = yData.length;
        final double[] weight = new double[n];

        weightOpt = false;
        weightFlag = 0;
        for (int i = 0; i < n; i++) {
            weight[i] = 1.0D;
        }

        setDefaultValues(xData, yData, weight);
    }

    /**
     * Constructor with data with x as 2D array and weights provided
     *
     * @param xData ???
     * @param yData ???
     * @param weight ???
     */
    public NonLinearRegressionFitter(final double[][] xData, final double[] yData, double[] weight) {
        nData0 = yData.length;
        weight = checkForZeroWeights(weight);
        if (weightOpt) {
            weightFlag = 1;
        }
        setDefaultValues(xData, yData, weight);
    }

    /**
     * Constructor with data with x and y as 2D arrays and no weights provided
     *
     * @param xxData ???
     * @param yyData ???
     */
    public NonLinearRegressionFitter(final double[][] xxData, final double[][] yyData) {
        multipleY = true;
        final int nY1 = yyData.length;
        nYarrays = nY1;
        final int nY2 = yyData[0].length;
        nData0 = nY2;
        final int nX1 = xxData.length;
        final double[] yData = new double[nY1 * nY2];
        if (nY1 != nX1) {
            throw new IllegalArgumentException(
                    "Multiple xData and yData arrays of different overall dimensions not supported");
        }
        final double[][] xData = new double[1][nY1 * nY2];
        int ii = 0;
        for (int i = 0; i < nY1; i++) {
            final int nY = yyData[i].length;
            if (nY != nY2) {
                throw new IllegalArgumentException("multiple y arrays must be of the same length");
            }
            final int nX = xxData[i].length;
            if (nY != nX) {
                throw new IllegalArgumentException(
                        "multiple y arrays must be of the same length as the x array length");
            }
            for (int j = 0; j < nY2; j++) {
                yData[ii] = yyData[i][j];
                xData[0][ii] = xxData[i][j];
                ii++;
            }
        }

        final int n = yData.length;
        final double[] weight = new double[n];

        weightOpt = false;
        for (int i = 0; i < n; i++) {
            weight[i] = 1.0D;
        }
        weightFlag = 0;

        setDefaultValues(xData, yData, weight);
    }

    /**
     * Constructor with data with x and y as 2D arrays and weights provided
     *
     * @param xxData ???
     * @param yyData ???
     * @param wWeight ???
     */
    public NonLinearRegressionFitter(final double[][] xxData, final double[][] yyData, final double[][] wWeight) {
        multipleY = true;
        final int nY1 = yyData.length;
        final int nY2 = yyData[0].length;
        nYarrays = nY1;
        nData0 = nY2;
        final int nX1 = xxData.length;
        final double[] yData = new double[nY1 * nY2];
        double[] weight = new double[nY1 * nY2];
        final double[][] xData = new double[nY1 * nY2][nX1];
        int ii = 0;
        for (int i = 0; i < nY1; i++) {

            final int nY = yyData[i].length;
            if (nY != nY2) {
                throw new IllegalArgumentException("multiple y arrays must be of the same length");
            }

            final int nX = xxData[i].length;
            if (nY != nX) {
                throw new IllegalArgumentException(
                        "multiple y arrays must be of the same length as the x array length");
            }

            for (int j = 0; j < nY2; j++) {
                yData[ii] = yyData[i][j];
                xData[ii][i] = xxData[i][j];
                weight[ii] = wWeight[i][j];
                ii++;
            }
        }
        weight = checkForZeroWeights(weight);
        if (weightOpt) {
            weightFlag = 1;
        }
        setDefaultValues(xData, yData, weight);
    }

    /**
     * add a single parameter constraint boundary for the non-linear regression
     *
     * @param paramIndex parameter index
     * @param conDir ???
     * @param constraint ???
     */
    public void addConstraint(final int paramIndex, final int conDir, final double constraint) {
        penalty = true;

        // First element reserved for method number if other methods than 'cliff' are added later
        if (penalties.isEmpty()) {
            penalties.add(Integer.valueOf(constraintMethod));
        }

        // add constraint
        if (penalties.size() == 1) {
            penalties.add(Integer.valueOf(1));
        } else {
            int nPC = ((Integer) penalties.get(1)).intValue();
            nPC++;
            penalties.set(1, Integer.valueOf(nPC));
        }
        penalties.add(Integer.valueOf(paramIndex));
        penalties.add(Integer.valueOf(conDir));
        penalties.add(Double.valueOf(constraint));
        if (paramIndex > maxConstraintIndex) {
            maxConstraintIndex = paramIndex;
        }
    }

    /**
     * add a multiple parameter constraint boundary for the non-linear regression
     *
     * @param paramIndices parameter indices
     * @param plusOrMinus positive/negative range constraint
     * @param conDir direction of fit constraint
     * @param constraint ???
     */
    public void addConstraint(final int[] paramIndices, final double[] plusOrMinus, final int conDir,
            final double constraint) {
        final int nCon = paramIndices.length;
        final int nPorM = plusOrMinus.length;
        if (nCon != nPorM) {
            throw new IllegalArgumentException(
                    "num of parameters, " + nCon + ", does not equal number of parameter signs, " + nPorM);
        }
        sumPenalty = true;

        // First element reserved for method number if other methods than 'cliff' are added later
        if (sumPenalties.isEmpty()) {
            sumPenalties.add(Integer.valueOf(constraintMethod));
        }

        // add constraint
        if (sumPenalties.size() == 1) {
            sumPenalties.add(Integer.valueOf(1));
        } else {
            int nPC = ((Integer) sumPenalties.get(1)).intValue();
            nPC++;
            sumPenalties.set(1, Integer.valueOf(nPC));
        }
        sumPenalties.add(Integer.valueOf(nCon));
        sumPenalties.add(paramIndices);
        sumPenalties.add(plusOrMinus);
        sumPenalties.add(Integer.valueOf(conDir));
        sumPenalties.add(Double.valueOf(constraint));
        final int maxI = TMath.Maximum(paramIndices);
        if (maxI > maxConstraintIndex) {
            maxConstraintIndex = maxI;
        }
    }

    /**
     * add a multiple parameter constraint boundary for the non-linear regression
     *
     * @param paramIndices parameter indices
     * @param plusOrMinus positive/negative range constraint
     * @param conDir direction of fit constraint
     * @param constraint ???
     */
    public void addConstraint(final int[] paramIndices, final int[] plusOrMinus, final int conDir,
            final double constraint) {
        final double[] dpom = ArrayConversion.getDoubleArray(plusOrMinus);
        addConstraint(paramIndices, dpom, conDir, constraint);
    }

    /**
     * Check entered weights for zeros. If more than 40% are zero or less than zero, all weights replaced by unity If
     * less than 40% are zero or less than zero, the zero or negative weights are replaced by the average of their
     * nearest neighbours
     *
     * @param weight weight vector
     * @return conditioned vector
     */
    protected double[] checkForZeroWeights(final double[] weight) {
        weightOpt = true;
        int nZeros = 0;
        final int n = weight.length;

        for (int i = 0; i < n; i++) {
            if (weight[i] <= 0.0) {
                nZeros++;
            }
        }
        final double perCentZeros = 100.0 * nZeros / n;
        if (perCentZeros > 40.0) {
            System.out.println(perCentZeros + "% of the weights are zero or less; all weights set to 1.0");
            for (int i = 0; i < n; i++) {
                weight[i] = 1.0D;
            }
            weightOpt = false;
        } else {
            if (perCentZeros > 0.0D) {
                for (int i = 0; i < n; i++) {
                    if (weight[i] <= 0.0) {
                        if (i == 0) {
                            int ii = 1;
                            boolean test = true;
                            while (test) {
                                if (weight[ii] > 0.0D) {
                                    final double ww = weight[0];
                                    weight[0] = weight[ii];
                                    System.out
                                            .println("weight at point " + i + ", " + ww + ", replaced by " + weight[i]);
                                    test = false;
                                } else {
                                    ii++;
                                }
                            }
                        }
                        if (i == n - 1) {
                            int ii = n - 2;
                            boolean test = true;
                            while (test) {
                                if (weight[ii] > 0.0D) {
                                    final double ww = weight[i];
                                    weight[i] = weight[ii];
                                    System.out
                                            .println("weight at point " + i + ", " + ww + ", replaced by " + weight[i]);
                                    test = false;
                                } else {
                                    ii--;
                                }
                            }
                        }
                        if (i > 0 && i < n - 2) {
                            double lower = 0.0;
                            double upper = 0.0;
                            int ii = i - 1;
                            boolean test = true;
                            while (test) {
                                if (weight[ii] > 0.0D) {
                                    lower = weight[ii];
                                    test = false;
                                } else {
                                    ii--;
                                    if (ii == 0) {
                                        test = false;
                                    }
                                }
                            }
                            ii = i + 1;
                            test = true;
                            while (test) {
                                if (weight[ii] > 0.0D) {
                                    upper = weight[ii];
                                    test = false;
                                } else {
                                    ii++;
                                    if (ii == n - 1) {
                                        test = false;
                                    }
                                }
                            }
                            final double ww = weight[i];
                            if (lower == 0.0) {
                                weight[i] = upper;
                            } else {
                                if (upper == 0.0) {
                                    weight[i] = lower;
                                } else {
                                    weight[i] = (lower + upper) / 2.0;
                                }
                            }
                            System.out.println("weight at point " + i + ", " + ww + ", replaced by " + weight[i]);
                        }
                    }
                }
            }
        }
        return weight;
    }

    /**
     * check y values for all y are very small value
     *
     * @param yPeak ???
     * @param ss ???
     * @return true: if a very small value has been removed
     */
    public double checkYallSmall(final double yPeak, final String ss) {
        double magScale = 1.0D;
        // truncate by four digits
        final double recipYpeak = (int) (1.0 / yPeak * 1e4) * 1e-4;
        if (yPeak < 1e-4) {
            System.out.println(ss + " fitting: The ordinate axis (y axis) has been rescaled by " + recipYpeak
                    + " to reduce rounding errors");
            for (int i = 0; i < nData; i++) {
                yData[i] *= recipYpeak;
                if (weightOpt) {
                    weight[i] *= recipYpeak;
                }
            }
            magScale = recipYpeak;
        }
        return magScale;
    }

    /**
     * check for zero and negative values
     *
     * @param xx ???
     * @param yy ???
     * @param ww ???
     */
    public void checkZeroNeg(final double[] xx, final double[] yy, final double[] ww) {
        int jj = 0;
        boolean test = true;
        for (int i = 0; i < nData; i++) {
            if (yy[i] <= 0.0D) {
                if (i <= jj) {
                    test = true;
                    jj = i;
                    while (test) {
                        jj++;
                        if (jj >= nData) {
                            throw new ArithmeticException("all zero cumulative data!!");
                        }
                        if (yy[jj] > 0.0D) {
                            yy[i] = yy[jj];
                            xx[i] = xx[jj];
                            ww[i] = ww[jj];
                            test = false;
                        }
                    }
                } else {
                    if (i == nData - 1) {
                        yy[i] = yy[i - 1];
                        xx[i] = xx[i - 1];
                        ww[i] = ww[i - 1];
                    } else {
                        yy[i] = (yy[i - 1] + yy[i + 1]) / 2.0D;
                        xx[i] = (xx[i - 1] + xx[i + 1]) / 2.0D;
                        ww[i] = (ww[i - 1] + ww[i + 1]) / 2.0D;
                    }
                }
            }
        }
    }

    /**
     * Enter data with x as 1D array and no weights provided
     *
     * @param xxData ???
     * @param yData ???
     */
    public void enterData(final double[] xxData, final double[] yData) {
        nData0 = yData.length;
        final int n = xxData.length;
        final double[][] xData = new double[1][n];
        final double[] weight = new double[n];

        for (int i = 0; i < n; i++) {
            xData[0][i] = xxData[i];
        }

        weightOpt = false;
        for (int i = 0; i < n; i++) {
            weight[i] = 1.0D;
        }
        weightFlag = 0;

        setDefaultValues(xData, yData, weight);
    }

    /**
     * Enter data as 1D arrays
     *
     * @param xxData ???
     * @param yData ???
     * @param weight ???
     */
    public void enterData(final double[] xxData, final double[] yData, double[] weight) {
        nData0 = yData.length;
        final int n = xxData.length;
        final double[][] xData = new double[1][n];
        for (int i = 0; i < n; i++) {
            xData[0][i] = xxData[i];
        }

        weight = checkForZeroWeights(weight);
        if (weightOpt) {
            weightFlag = 1;
        }
        setDefaultValues(xData, yData, weight);
    }

    /**
     * Enter data with x as 1D array and y as a 2D array and no weights provided
     *
     * @param xxData ???
     * @param yyData ???
     */
    public void enterData(final double[] xxData, final double[][] yyData) {
        multipleY = true;

        final int nY1 = yyData.length;
        final int nY2 = yyData[0].length;
        nYarrays = nY1;
        nData0 = nY2;
        final double[] yData = new double[nY1 * nY2];
        int ii = 0;
        for (int i = 0; i < nY1; i++) {
            final int nY = yyData[i].length;
            if (nY != nY2) {
                throw new IllegalArgumentException("multiple y arrays must be of the same length");
            }
            for (int j = 0; j < nY2; j++) {
                yData[ii] = yyData[i][j];
                ii++;
            }
        }

        final double[][] xData = new double[1][nY1 * nY2];
        final double[] weight = new double[nY1 * nY2];

        ii = 0;
        final int n = xxData.length;
        for (int j = 0; j < nY1; j++) {
            for (int i = 0; i < n; i++) {
                xData[0][ii] = xxData[i];
                weight[ii] = 1.0D;
                ii++;
            }
        }
        weightOpt = false;
        weightFlag = 0;

        setDefaultValues(xData, yData, weight);
    }

    /**
     * Enter data
     *
     * @param xxData 1D array
     * @param yyData 2D array
     * @param wWeight ???
     */
    public void enterData(final double[] xxData, final double[][] yyData, final double[][] wWeight) {

        multipleY = true;
        final int nY1 = yyData.length;
        nYarrays = nY1;
        final int nY2 = yyData[0].length;
        nData0 = nY2;
        final double[] yData = new double[nY1 * nY2];
        double[] weight = new double[nY1 * nY2];
        int ii = 0;
        for (int i = 0; i < nY1; i++) {
            final int nY = yyData[i].length;
            if (nY != nY2) {
                throw new IllegalArgumentException("multiple y arrays must be of the same length");
            }
            for (int j = 0; j < nY2; j++) {
                yData[ii] = yyData[i][j];
                weight[ii] = wWeight[i][j];
                ii++;
            }
        }
        final int n = xxData.length;
        if (n != nY2) {
            throw new IllegalArgumentException("x and y data lengths must be the same");
        }
        final double[][] xData = new double[1][nY1 * n];
        ii = 0;
        for (int j = 0; j < nY1; j++) {
            for (int i = 0; i < n; i++) {
                xData[0][ii] = xxData[i];
                ii++;
            }
        }

        weight = checkForZeroWeights(weight);
        if (weightOpt) {
            weightFlag = 1;
        }
        setDefaultValues(xData, yData, weight);
    }

    /**
     * Enter data with x as 2D array and no weights provided
     *
     * @param xData ???
     * @param yData ???
     */
    public void enterData(final double[][] xData, final double[] yData) {
        nData0 = yData.length;
        final int n = yData.length;
        final double[] weight = new double[n];

        weightOpt = false;
        for (int i = 0; i < n; i++) {
            weight[i] = 1.0D;
        }
        weightFlag = 0;
        setDefaultValues(xData, yData, weight);
    }

    /**
     * Enter data methods
     *
     * @param xData 2D array
     * @param yData 1D array
     * @param weight 1D array
     */
    public void enterData(final double[][] xData, final double[] yData, double[] weight) {
        nData0 = yData.length;
        weightOpt = true;
        weight = checkForZeroWeights(weight);
        if (weightOpt) {
            weightFlag = 1;
        }
        setDefaultValues(xData, yData, weight);
    }

    /**
     * Enter data with x and y as 2D arrays and no weights provided
     *
     * @param xxData ???
     * @param yyData ???
     */
    public void enterData(final double[][] xxData, final double[][] yyData) {
        multipleY = true;
        final int nY1 = yyData.length;
        nYarrays = nY1;
        final int nY2 = yyData[0].length;
        nData0 = nY2;
        final int nX1 = xxData.length;
        final double[] yData = new double[nY1 * nY2];
        final double[][] xData = new double[nY1 * nY2][nX1];
        int ii = 0;
        for (int i = 0; i < nY1; i++) {
            final int nY = yyData[i].length;
            if (nY != nY2) {
                throw new IllegalArgumentException("multiple y arrays must be of the same length");
            }
            final int nX = xxData[i].length;
            if (nY != nX) {
                throw new IllegalArgumentException(
                        "multiple y arrays must be of the same length as the x array length");
            }
            for (int j = 0; j < nY2; j++) {
                yData[ii] = yyData[i][j];
                xData[ii][i] = xxData[i][j];
                ii++;
            }
        }

        final int n = yData.length;
        final double[] weight = new double[n];

        weightOpt = false;
        for (int i = 0; i < n; i++) {
            weight[i] = 1.0D;
        }
        weightFlag = 0;

        setDefaultValues(xData, yData, weight);
    }

    /**
     * data entry, all 2D arrays
     *
     * @param xxData ???
     * @param yyData ???
     * @param wWeight ???
     */
    public void enterData(final double[][] xxData, final double[][] yyData, final double[][] wWeight) {
        multipleY = true;
        final int nY1 = yyData.length;
        nYarrays = nY1;
        final int nY2 = yyData[0].length;
        nData0 = nY2;
        final int nX1 = xxData.length;
        final double[] yData = new double[nY1 * nY2];
        double[] weight = new double[nY1 * nY2];
        final double[][] xData = new double[nY1 * nY2][nX1];
        int ii = 0;
        for (int i = 0; i < nY1; i++) {
            final int nY = yyData[i].length;
            if (nY != nY2) {
                throw new IllegalArgumentException("multiple y arrays must be of the same length");
            }
            final int nX = xxData[i].length;
            if (nY != nX) {
                throw new IllegalArgumentException(
                        "multiple y arrays must be of the same length as the x array length");
            }
            for (int j = 0; j < nY2; j++) {
                yData[ii] = yyData[i][j];
                xData[ii][i] = xxData[i][j];
                weight[ii] = wWeight[i][j];
                ii++;
            }
        }

        weight = checkForZeroWeights(weight);
        if (weightOpt) {
            weightFlag = 1;
        }
        setDefaultValues(xData, yData, weight);
    }

    /**
     * Generalised linear regression statistics (protected method called by linear(), linearGeneral() and polynomial())
     *
     * @param xd ???
     */
    protected void generalLinearStats(final double[][] xd) {
        double sde = 0.0D, sum = 0.0D, yCalctemp = 0.0D;
        final double[][] h = new double[nTerms][nTerms];
        double[][] stat = new double[nTerms][nTerms];
        covar = new double[nTerms][nTerms];
        corrCoeff = new double[nTerms][nTerms];
        final double[] coeffSd = new double[nTerms];
        final double[] coeff = new double[nTerms];

        for (int i = 0; i < nTerms; i++) {
            coeff[i] = best[i];
        }

        if (weightOpt) {
            chiSquare = 0.0D;
        }
        sumOfSquares = 0.0D;
        for (int i = 0; i < nData; ++i) {
            yCalctemp = 0.0;
            for (int j = 0; j < nTerms; ++j) {
                yCalctemp += coeff[j] * xd[j][i];
            }
            yCalc[i] = yCalctemp;
            yCalctemp -= yData[i];
            residual[i] = yCalctemp;
            residualW[i] = yCalctemp / weight[i];
            if (weightOpt) {
                chiSquare += TMathConstants.Sqr(yCalctemp / weight[i]);
            }
            sumOfSquares += yCalctemp * yCalctemp;
        }
        if (weightOpt || trueFreq) {
            reducedChiSquare = chiSquare / degreesOfFreedom;
        }
        final double varY = sumOfSquares / degreesOfFreedom;
        final double sdY = Math.sqrt(varY);

        if (sumOfSquares == 0.0D) {
            for (int i = 0; i < nTerms; i++) {
                coeffSd[i] = 0.0D;
                for (int j = 0; j < nTerms; j++) {
                    covar[i][j] = 0.0D;
                    if (i == j) {
                        corrCoeff[i][j] = 1.0D;
                    } else {
                        corrCoeff[i][j] = 0.0D;
                    }
                }
            }
        } else {
            for (int i = 0; i < nTerms; ++i) {
                for (int j = 0; j < nTerms; ++j) {
                    sum = 0.0;
                    for (int k = 0; k < nData; ++k) {
                        if (weightOpt) {
                            sde = weight[k];
                        } else {
                            sde = sdY;
                        }
                        sum += xd[i][k] * xd[j][k] / (sde * sde);
                    }
                    h[j][i] = sum;
                }
            }
            MatrixD hh = new MatrixD(h);

            hh = hh.inverse();
            stat = hh.getArrayCopy();
            for (int j = 0; j < nTerms; ++j) {
                coeffSd[j] = Math.sqrt(stat[j][j]);
            }

            for (int i = 0; i < nTerms; i++) {
                for (int j = 0; j < nTerms; j++) {
                    covar[i][j] = stat[i][j];
                }
            }

            for (int i = 0; i < nTerms; i++) {
                for (int j = 0; j < nTerms; j++) {
                    if (i == j) {
                        corrCoeff[i][j] = 1.0D;
                    } else {
                        corrCoeff[i][j] = covar[i][j] / (coeffSd[i] * coeffSd[j]);
                    }
                }
            }
        }

        for (int i = 0; i < nTerms; i++) {
            bestSd[i] = coeffSd[i];
            tValues[i] = best[i] / bestSd[i];
            final double atv = Math.abs(tValues[i]);
            pValues[i] = 1.0 - TMath.Student(atv, degreesOfFreedom);
        }

        if (nXarrays == 1 && nYarrays == 1) {
            sampleR = TMath.CorrelationCoefficient(xData[0], yData, weight);
            sampleR2 = sampleR * sampleR;
            adjustedR = sampleR;
            adjustedR2 = sampleR2;
        } else {
            multCorrelCoeff(yData, yCalc, weight);
        }
    }

    /**
     * Get the Adjusted Sample Correlation Coefficient
     *
     * @return Adjusted Sample Correlation Coefficient
     */
    public double getAdjustedR() {
        return adjustedR;
    }

    /**
     * Get the Adjusted Sample Correlation Coefficient Squared
     *
     * @return Adjusted Sample Correlation Coefficient Squared
     */
    public double getAdjustedR2() {
        return adjustedR2;
    }

    /**
     * Get the best estimates of the unknown parameters
     *
     * @return best estimates of the unknown parameters
     */
    public double[] getBestEstimates() {
        return best.clone();
    }

    /**
     * Get the estimates of the errors of the best estimates of the unknown parameters
     *
     * @return estimates of the errors of the best estimates
     */
    public double[] getBestEstimatesErrors() {
        return bestSd.clone();
    }

    /**
     * Get the estimates of the standard deviations of the best estimates of the unknown parameters
     *
     * @return estimates of the standard deviations
     */
    public double[] getBestEstimatesStandardDeviations() {
        return bestSd.clone();
    }

    /**
     * Get the chi square estimate
     *
     * @return chi square estimate
     */
    public double getChiSquare() {
        double ret = 0.0D;
        if (weightOpt) {
            ret = chiSquare;
        } else {
            System.out.println("Chi Square cannot be calculated as data are neither true frequencies nor weighted");
            System.out.println("A value of -1 is returned as Chi Square");
            ret = -1.0D;
        }
        return ret;
    }

    /**
     * Get the chi square probablity
     *
     * @return chi square probablity
     */
    public double getchiSquareProb() {
        double ret = 0.0D;
        if (weightOpt) {
            ret = 1.0D - TMath.ChisquareQuantile(chiSquare, nData - nXarrays);
        } else {
            System.out.println(
                    "A Chi Square probablity cannot be calculated as data are neither true frequencies nor weighted");
            System.out.println("A value of -1 is returned as Reduced Chi Square");
            ret = -1.0D;
        }
        return ret;
    }

    /**
     * Get the best estimates of the unknown parameters
     *
     * @return estimates of the unknown parameters
     */
    public double[] getCoeff() {
        return best.clone();
    }

    /**
     * Get the estimates of the errors of the best estimates of the unknown parameters
     *
     * @return estimates of the errors of the best estimates
     */
    public double[] getCoeffSd() {
        return bestSd.clone();
    }

    /**
     * Get the cofficients of variations of the best estimates of the unknown parameters
     *
     * @return cofficients of variations of the best estimates
     */
    public double[] getCoeffVar() {
        final double[] coeffVar = new double[nTerms];

        for (int i = 0; i < nTerms; i++) {
            coeffVar[i] = bestSd[i] * 100.0D / best[i];
        }
        return coeffVar;
    }

    /**
     * Get the correlation coefficient matrix
     *
     * @return correlation coefficient matrix
     */
    public double[][] getCorrCoeffMatrix() {
        return corrCoeff;
    }

    /**
     * Get the covariance matrix
     *
     * @return covariance matrix
     */
    public double[][] getCovMatrix() {
        return covar;
    }

    /**
     * Get the degrees of freedom
     *
     * @return degrees of freedom
     */
    public double getDegFree() {
        return degreesOfFreedom;
    }

    /**
     * Get the non-linear regression fractional step size used in numerical differencing
     *
     * @return non-linear regression fractional step size used in numerical differencing
     */
    public double getDelta() {
        return delta;
    }

    /**
     * Get the non-linear regression pre and post minimum gradients
     *
     * @return non-linear regression pre and post minimum gradients
     */
    public double[][] getGrad() {
        return grad;
    }

    /**
     * Get the non-linear regression statistics Hessian matrix inversion status flag
     *
     * @return non-linear regression statistics Hessian matrix inversion status flag
     */
    public boolean getInversionCheck() {
        return invertFlag;
    }

    /**
     * Get the non-linear regression convergence test option
     *
     * @return non-linear regression convergence test option
     */
    public int getMinTest() {
        return minTest;
    }

    /**
     * Get the Multiple Correlation Coefficient F ratio
     *
     * @return Multiple Correlation Coefficient F ratio
     */
    public double getMultipleF() {
        if (nXarrays == 1) {
            System.out.println(
                    "NonLinearRegression.getMultipleF - The regression is not a multple regession: NaN returned");
        }
        return multipleF;
    }

    /**
     * Get the number of iterations in nonlinear regression
     *
     * @return number of iterations in nonlinear regression
     */
    public int getNiter() {
        return nIter;
    }

    /**
     * Get the non-linear regression status true if convergence was achieved false if convergence not achieved before
     * maximum number of iterations current values then returned
     *
     * @return the non-linear regression status
     */
    public boolean getNlrStatus() {
        return nlrStatus;
    }

    /**
     * Get the maximum number of iterations allowed in nonlinear regression
     *
     * @return maximum number of iterations allowed in nonlinear regression
     */
    public int getNmax() {
        return nMax;
    }

    /**
     * Get the Nelder and Mead contraction coefficient [gamma]
     *
     * @return Nelder and Mead contraction coefficient [gamma]
     */
    public double getNMcontract() {
        return cCoeff;
    }

    /**
     * Get the Nelder and Mead extension coefficient [beta]
     *
     * @return Nelder and Mead extension coefficient [beta]
     */
    public double getNMextend() {
        return eCoeff;
    }

    /**
     * Get the Nelder and Mead reflection coefficient [alpha]
     *
     * @return Nelder and Mead reflection coefficient [alpha]
     */
    public double getNMreflect() {
        return rCoeff;
    }

    /**
     * Get the number of restarts in nonlinear regression
     *
     * @return number of restarts in nonlinear regression
     */
    public int getNrestarts() {
        return kRestart;
    }

    /**
     * Get the maximum number of restarts allowed in nonlinear regression
     *
     * @return maximum number of restarts allowed in nonlinear regression
     */
    public int getNrestartsMax() {
        return konvge;
    }

    /**
     * Get the non-linear regression statistics Hessian matrix inverse diagonal status flag
     *
     * @return non-linear regression statistics Hessian matrix inverse diagonal status flag
     */
    public boolean getPosVarCheck() {
        return posVarFlag;
    }

    /**
     * Get the pseudo-estimates of the errors of the best estimates of the unknown parameters
     *
     * @return pseudo-estimates of the errors of the best estimates
     */
    public double[] getPseudoErrors() {
        return pseudoSd.clone();
    }

    /**
     * Get the pseudo-estimates of the errors of the best estimates of the unknown parameters
     *
     * @return pseudo-estimates of the errors of the best estimates
     */
    public double[] getPseudoSd() {
        return pseudoSd.clone();
    }

    /**
     * Get the p-values of the best estimates
     *
     * @return p-values of the best estimates
     */
    public double[] getPvalues() {
        return pValues.clone();
    }

    /**
     * Get the reduced chi square estimate
     *
     * @return reduced chi square estimate
     */
    public double getReducedChiSquare() {
        double ret = 0.0D;
        if (weightOpt) {
            ret = reducedChiSquare;
        } else {
            System.out.println(
                    "A Reduced Chi Square cannot be calculated as data are neither true frequencies nor weighted");
            System.out.println("A value of -1 is returned as Reduced Chi Square");
            ret = -1.0D;
        }
        return ret;
    }

    /**
     * Get the unweighed residuals, y(experimental) - y(calculated)
     *
     * @return unweighed residuals, y(experimental) - y(calculated)
     */
    public double[] getResiduals() {
        final double[] temp = new double[nData];
        for (int i = 0; i < nData; i++) {
            temp[i] = yData[i] - yCalc[i];
        }
        return temp;
    }

    /**
     * Get the Sample Correlation Coefficient
     *
     * @return Sample Correlation Coefficient
     */
    public double getSampleR() {
        return sampleR;
    }

    /**
     * Get the Sample Correlation Coefficient Squared
     *
     * @return Sample Correlation Coefficient Squared
     */
    public double getSampleR2() {
        return sampleR2;
    }

    /**
     * @return scaling factors
     */
    public double[] getScale() {
        return fscale;
    }

    /**
     * Get the simplex sd at the minimum
     *
     * @return simplex sd at the minimum
     */
    public double getSimplexSd() {
        return simplexSd;
    }

    /**
     * Get the unweighed sum of squares of the residuals
     *
     * @return unweighed sum of squares of the residuals
     */
    public double getSumOfSquares() {
        return sumOfSquares;
    }

    /**
     * Get the non-linear regression tolerance
     *
     * @return non-linear regression tolerance
     */
    public double getTolerance() {
        return fTol;
    }

    /**
     * Get the true frequency test, trueFreq
     *
     * @return trueFreq
     */
    public boolean getTrueFreq() {
        return trueFreq;
    }

    /**
     * Get the t-values of the best estimates
     *
     * @return t-values of the best estimates
     */
    public double[] getTvalues() {
        return tValues.clone();
    }

    /**
     * Get the weighted residuals, (y(experimental) - y(calculated))/weight
     *
     * @return weighted residuals
     */
    public double[] getWeightedResiduals() {

        final double[] temp = new double[nData];
        for (int i = 0; i < nData; i++) {
            temp[i] = (yData[i] - yCalc[i]) / weight[i];
        }
        return temp;
    }

    /**
     * Get the input x values
     *
     * @return input x values
     */
    public double[][] getXdata() {
        return xData.clone();
    }

    /**
     * Get the calculated y values
     *
     * @return calculated y values
     */
    public double[] getYcalc() {
        final double[] temp = new double[nData];
        for (int i = 0; i < nData; i++) {
            temp[i] = yCalc[i];
        }
        return temp;
    }

    /**
     * Get the input y values
     *
     * @return input y values
     */
    public double[] getYdata() {
        return yData.clone();
    }

    /**
     * Ignore check on whether degrees of freedom are greater than zero
     */
    public void ignoreDofFcheck() {
        ignoreDofFcheck = true;
    }

    /**
     * Check for y value = infinity
     *
     * @param yPeak ???
     * @param peaki ???
     * @return true if an infinity value has been removed
     */
    public boolean infinityCheck(final double yPeak, final int peaki) {
        boolean flag = false;
        if (yPeak == 1.0D / 0.0D || yPeak == -1.0D / 0.0D) {
            int ii = peaki + 1;
            if (peaki == nData - 1) {
                ii = peaki - 1;
            }
            xData[0][peaki] = xData[0][ii];
            yData[peaki] = yData[ii];
            weight[peaki] = weight[ii];
            System.out.println("An infinty has been removed at point " + peaki);
            flag = true;
        }
        return flag;
    }

    /**
     * Calculate the multiple correlation coefficient
     *
     * @param yy ???
     * @param yyCalc ???
     * @param ww ???
     */
    protected void multCorrelCoeff(final double[] yy, final double[] yyCalc, final double[] ww) {

        // sum of reciprocal weights squared
        double sumRecipW = 0.0D;
        for (int i = 0; i < nData; i++) {
            sumRecipW += 1.0D / TMathConstants.Sqr(ww[i]);
        }

        // weighted mean of yy
        double my = 0.0D;
        for (int j = 0; j < nData; j++) {
            my += yy[j] / TMathConstants.Sqr(ww[j]);
        }
        my /= sumRecipW;

        // weighted mean of residuals
        double mr = 0.0D;
        final double[] residuals = new double[nData];
        for (int j = 0; j < nData; j++) {
            residuals[j] = yy[j] - yyCalc[j];
            mr += residuals[j] / TMathConstants.Sqr(ww[j]);
        }
        mr /= sumRecipW;

        // calculate yy weighted sum of squares
        double s2yy = 0.0D;
        for (int k = 0; k < nData; k++) {
            s2yy += TMathConstants.Sqr((yy[k] - my) / ww[k]);
        }

        // calculate residual weighted sum of squares
        double s2r = 0.0D;
        for (int k = 0; k < nData; k++) {
            s2r += TMathConstants.Sqr((residuals[k] - mr) / ww[k]);
        }

        // calculate multiple coefficient of determination
        sampleR2 = 1.0D - s2r / s2yy;
        sampleR = Math.sqrt(sampleR2);

        // Calculate adjusted multiple coefficient of determination
        adjustedR2 = ((nData - 1) * sampleR2 - nXarrays) / (nData - nXarrays - 1);
        adjustedR = Math.sqrt(adjustedR2);

        // F-ratio
        if (nXarrays > 1) {
            multipleF = sampleR2 * (nData - nXarrays) / ((1.0D - sampleR2) * (nXarrays - 1));
            adjustedF = adjustedR2 * (nData - nXarrays) / ((1.0D - adjustedR2) * (nXarrays - 1));
        }
    }

    /**
     * Nelder and Mead Simplex Simplex Non-linear Regression
     *
     * @param regFun function to be fitted to
     * @param start initial parameter values
     * @param step initial parameter step values
     * @param fTol tolerance
     * @param nMax maximum number of iterations
     */
    protected void nelderMead(final Object regFun, final double[] start, final double[] step, final double fTol,
            final int nMax) {
        final int np = start.length; // number of unknown parameters;
        if (maxConstraintIndex >= np) {
            throw new IllegalArgumentException("You have entered more constrained parameters (" + maxConstraintIndex
                    + ") than minimisation parameters (" + np + ")");
        }
        nlrStatus = true; // -> false if convergence criterion not met
        nTerms = np; // number of parameters whose best estimates are to be determined
        final int nnp = np + 1; // number of simplex apices
        lastSSnoConstraint = 0.0D; // last sum of squares without a penalty constraint being applied

        if (scaleOpt < 2) {
            fscale = new double[np]; // scaling factors
        }
        if (scaleOpt == 2 && fscale.length != start.length) {
            throw new IllegalArgumentException("scale array and initial estimate array are of different lengths");
        }
        if (step.length != start.length) {
            throw new IllegalArgumentException("step array length " + step.length
                    + " and initial estimate array length " + start.length + " are of different");
        }

        // check for zero step sizes
        for (int i = 0; i < np; i++) {
            if (step[i] == 0.0D) {
                throw new IllegalArgumentException("step " + i + " size is zero");
            }
        }

        // set statistic arrays to NaN if df check ignored
        if (ignoreDofFcheck) {
            bestSd = new double[nTerms];
            pseudoSd = new double[nTerms];
            tValues = new double[nTerms];
            pValues = new double[nTerms];

            covar = new double[nTerms][nTerms];
            corrCoeff = new double[nTerms][nTerms];
            ;
            for (int i = 0; i < nTerms; i++) {
                bestSd[i] = Double.NaN;
                pseudoSd[i] = Double.NaN;
                for (int j = 0; j < nTerms; j++) {
                    covar[i][j] = Double.NaN;
                    corrCoeff[i][j] = Double.NaN;
                }
            }
        }

        // set up arrays
        startH = new double[np]; // holding array of star values
        this.step = new double[np]; // Nelder and Mead step
        final double[] pmin = new double[np]; // Nelder and Mead Pmin
        best = new double[np]; // best estimates array
        bestSd = new double[np]; // sd of best estimates array
        tValues = new double[np]; // t-value of best estimates array
        pValues = new double[np]; // p-value of best estimates array

        final double[][] pp = new double[nnp][nnp]; // Nelder and Mead P
        final double[] yy = new double[nnp]; // Nelder and Mead y
        final double[] pbar = new double[nnp]; // Nelder and Mead P with bar superscript
        final double[] pstar = new double[nnp]; // Nelder and Mead P*
        final double[] p2star = new double[nnp]; // Nelder and Mead P**

        // mean of abs values of yData (for testing for minimum)
        double yabsmean = 0.0D;
        for (int i = 0; i < nData; i++) {
            yabsmean += Math.abs(yData[i]);
        }
        yabsmean /= nData;

        // Set any single parameter constraint parameters
        if (penalty) {
            Integer itemp = (Integer) penalties.get(1);
            nConstraints = itemp.intValue();
            penaltyParam = new int[nConstraints];
            penaltyCheck = new int[nConstraints];
            constraints = new double[nConstraints];
            Double dtemp = null;
            int j = 2;
            for (int i = 0; i < nConstraints; i++) {
                itemp = (Integer) penalties.get(j);
                penaltyParam[i] = itemp.intValue();
                j++;
                itemp = (Integer) penalties.get(j);
                penaltyCheck[i] = itemp.intValue();
                j++;
                dtemp = (Double) penalties.get(j);
                constraints[i] = dtemp.doubleValue();
                j++;
            }
        }

        // Set any multiple parameter constraint parameters
        if (sumPenalty) {
            Integer itemp = (Integer) sumPenalties.get(1);
            nSumConstraints = itemp.intValue();
            sumPenaltyParam = new int[nSumConstraints][];
            sumPlusOrMinus = new double[nSumConstraints][];
            sumPenaltyCheck = new int[nSumConstraints];
            sumPenaltyNumber = new int[nSumConstraints];
            sumConstraints = new double[nSumConstraints];
            int[] itempArray = null;
            double[] dtempArray = null;
            Double dtemp = null;
            int j = 2;
            for (int i = 0; i < nSumConstraints; i++) {
                itemp = (Integer) sumPenalties.get(j);
                sumPenaltyNumber[i] = itemp.intValue();
                j++;
                itempArray = (int[]) sumPenalties.get(j);
                sumPenaltyParam[i] = itempArray;
                j++;
                dtempArray = (double[]) sumPenalties.get(j);
                sumPlusOrMinus[i] = dtempArray;
                j++;
                itemp = (Integer) sumPenalties.get(j);
                sumPenaltyCheck[i] = itemp.intValue();
                j++;
                dtemp = (Double) sumPenalties.get(j);
                sumConstraints[i] = dtemp.doubleValue();
                j++;
            }
        }

        // Store unscaled start values
        for (int i = 0; i < np; i++) {
            startH[i] = start[i];
        }

        // scale initial estimates and step sizes
        if (scaleOpt > 0) {
            boolean testzero = false;
            for (int i = 0; i < np; i++) {
                if (start[i] == 0.0D) {
                    testzero = true;
                }
            }
            if (testzero) {
                System.out.println("Neler and Mead Simplex: a start value of zero precludes scaling");
                System.out.println("Regression performed without scaling");
                scaleOpt = 0;
            }
        }
        switch (scaleOpt) {
        case 0:
            for (int i = 0; i < np; i++) {
                fscale[i] = 1.0D;
            }
            break;
        case 1:
            for (int i = 0; i < np; i++) {
                fscale[i] = 1.0 / start[i];
                step[i] = step[i] / start[i];
                start[i] = 1.0D;
            }
            break;
        case 2:
            for (int i = 0; i < np; i++) {
                step[i] *= fscale[i];
                start[i] *= fscale[i];
            }
            break;
        }

        // set class member values
        this.fTol = fTol;
        this.nMax = nMax;
        nIter = 0;
        for (int i = 0; i < np; i++) {
            this.step[i] = step[i];
            fscale[i] = fscale[i];
        }

        // initial simplex
        double sho = 0.0D;
        for (int i = 0; i < np; ++i) {
            sho = start[i];
            pstar[i] = sho;
            p2star[i] = sho;
            pmin[i] = sho;
        }

        int jcount = konvge; // count of number of restarts still available

        for (int i = 0; i < np; ++i) {
            pp[i][nnp - 1] = start[i];
        }
        yy[nnp - 1] = sumSquares(regFun, start);
        for (int j = 0; j < np; ++j) {
            start[j] = start[j] + step[j];

            for (int i = 0; i < np; ++i) {
                pp[i][j] = start[i];
            }
            yy[j] = sumSquares(regFun, start);
            start[j] = start[j] - step[j];
        }

        // loop over allowed iterations
        double ynewlo = 0.0D; // current value lowest y
        double ystar = 0.0D; // Nelder and Mead y*
        double y2star = 0.0D; // Nelder and Mead y**
        double ylo = 0.0D; // Nelder and Mead y(low)
        // variables used in calculating the variance of the simplex at a putative minimum
        double curMin = 00D, sumnm = 0.0D, summnm = 0.0D, zn = 0.0D;
        int ilo = 0; // index of low apex
        int ihi = 0; // index of high apex
        int ln = 0; // counter for a check on low and high apices
        boolean test = true; // test becomes false on reaching minimum

        while (test) {
            // Determine h
            ylo = yy[0];
            ynewlo = ylo;
            ilo = 0;
            ihi = 0;
            for (int i = 1; i < nnp; ++i) {
                if (yy[i] < ylo) {
                    ylo = yy[i];
                    ilo = i;
                }
                if (yy[i] > ynewlo) {
                    ynewlo = yy[i];
                    ihi = i;
                }
            }
            // Calculate pbar
            for (int i = 0; i < np; ++i) {
                zn = 0.0D;
                for (int j = 0; j < nnp; ++j) {
                    zn += pp[i][j];
                }
                zn -= pp[i][ihi];
                pbar[i] = zn / np;
            }

            // Calculate p=(1+alpha).pbar-alpha.ph {Reflection}
            for (int i = 0; i < np; ++i) {
                pstar[i] = (1.0 + rCoeff) * pbar[i] - rCoeff * pp[i][ihi];
            }

            // Calculate y*
            ystar = sumSquares(regFun, pstar);

            ++nIter;

            // check for y*<yi
            if (ystar < ylo) {
                // Form p**=(1+gamma).p*-gamma.pbar {Extension}
                for (int i = 0; i < np; ++i) {
                    p2star[i] = pstar[i] * (1.0D + eCoeff) - eCoeff * pbar[i];
                }
                // Calculate y**
                y2star = sumSquares(regFun, p2star);
                ++nIter;
                if (y2star < ylo) {
                    // Replace ph by p**
                    for (int i = 0; i < np; ++i) {
                        pp[i][ihi] = p2star[i];
                    }
                    yy[ihi] = y2star;
                } else {
                    // Replace ph by p*
                    for (int i = 0; i < np; ++i) {
                        pp[i][ihi] = pstar[i];
                    }
                    yy[ihi] = ystar;
                }
            } else {
                // Check y*>yi, i!=h
                ln = 0;
                for (int i = 0; i < nnp; ++i) {
                    if (i != ihi && ystar > yy[i]) {
                        ++ln;
                    }
                }
                if (ln == np) {
                    // y*>= all yi; Check if y*>yh
                    if (ystar <= yy[ihi]) {
                        // Replace ph by p*
                        for (int i = 0; i < np; ++i) {
                            pp[i][ihi] = pstar[i];
                        }
                        yy[ihi] = ystar;
                    }
                    // Calculate p** =beta.ph+(1-beta)pbar {Contraction}
                    for (int i = 0; i < np; ++i) {
                        p2star[i] = cCoeff * pp[i][ihi] + (1.0 - cCoeff) * pbar[i];
                    }
                    // Calculate y**
                    y2star = sumSquares(regFun, p2star);
                    ++nIter;
                    // Check if y**>yh
                    if (y2star > yy[ihi]) {
                        // Replace all pi by (pi+pl)/2

                        for (int j = 0; j < nnp; ++j) {
                            for (int i = 0; i < np; ++i) {
                                pp[i][j] = 0.5 * (pp[i][j] + pp[i][ilo]);
                                pmin[i] = pp[i][j];
                            }
                            yy[j] = sumSquares(regFun, pmin);
                        }
                        nIter += nnp;
                    } else {
                        // Replace ph by p**
                        for (int i = 0; i < np; ++i) {
                            pp[i][ihi] = p2star[i];
                        }
                        yy[ihi] = y2star;
                    }
                } else {
                    // replace ph by p*
                    for (int i = 0; i < np; ++i) {
                        pp[i][ihi] = pstar[i];
                    }
                    yy[ihi] = ystar;
                }
            }

            // test for convergence
            // calculte sd of simplex and minimum point
            sumnm = 0.0;
            ynewlo = yy[0];
            ilo = 0;
            for (int i = 0; i < nnp; ++i) {
                sumnm += yy[i];
                if (ynewlo > yy[i]) {
                    ynewlo = yy[i];
                    ilo = i;
                }
            }
            sumnm /= nnp;
            summnm = 0.0;
            for (int i = 0; i < nnp; ++i) {
                zn = yy[i] - sumnm;
                summnm += zn * zn;
            }
            curMin = Math.sqrt(summnm / np);

            // test simplex sd
            switch (minTest) {
            case 0:
                if (curMin < fTol) {
                    test = false;
                }
                break;
            case 1:
                if (Math.sqrt(ynewlo / degreesOfFreedom) < yabsmean * fTol) {
                    test = false;
                }
                break;
            }
            sumOfSquares = ynewlo;
            if (!test) {
                // store best estimates
                for (int i = 0; i < np; ++i) {
                    pmin[i] = pp[i][ilo];
                }
                yy[nnp - 1] = ynewlo;
                // store simplex sd
                simplexSd = curMin;
                // test for restart
                --jcount;
                if (jcount > 0) {
                    test = true;
                    for (int j = 0; j < np; ++j) {
                        pmin[j] = pmin[j] + step[j];
                        for (int i = 0; i < np; ++i) {
                            pp[i][j] = pmin[i];
                        }
                        yy[j] = sumSquares(regFun, pmin);
                        pmin[j] = pmin[j] - step[j];
                    }
                }
            }

            if (test && nIter > this.nMax) {
                // TODO check wether we need this switch
                if (true) {
                    System.out.println("Maximum iteration number reached, in Regression.simplex(...)");
                    System.out.println("without the convergence criterion being satisfied");
                    System.out.println("Current parameter estimates and sum of squares values returned");
                }
                nlrStatus = false;
                // store current estimates
                for (int i = 0; i < np; ++i) {
                    pmin[i] = pp[i][ilo];
                }
                yy[nnp - 1] = ynewlo;
                test = false;
            }

        }

        for (int i = 0; i < np; ++i) {
            pmin[i] = pp[i][ilo];
            best[i] = pmin[i] / fscale[i];
            fscale[i] = 1.0D; // unscale for statistical methods
        }
        fMin = ynewlo;
        kRestart = konvge - jcount;

        if (statFlag) {
            if (!ignoreDofFcheck) {
                pseudoLinearStats(regFun);
            }
        } else {
            for (int i = 0; i < np; ++i) {
                bestSd[i] = Double.NaN;
            }
        }
    }

    /**
     * apply linear statistics to a non-linear regression
     *
     * @param regFun test function
     * @return pseudo linear statistics
     */
    protected int pseudoLinearStats(final Object regFun) {
        double f1 = 0.0D, f2 = 0.0D, f3 = 0.0D, f4 = 0.0D; // intermdiate values in numerical differentiation
        int flag = 0; // returned as 0 if method fully successful;
        // negative if partially successful or unsuccessful: check posVarFlag and invertFlag
        // -1 posVarFlag or invertFlag is false;
        // -2 posVarFlag and invertFlag are false
        final int np = nTerms;

        final double[] f = new double[np];
        double[] pmin = new double[np];
        final double[] coeffSd = new double[np];
        final double[] xd = new double[nXarrays];
        double[][] stat = new double[np][np];
        pseudoSd = new double[np];

        grad = new double[np][2];
        covar = new double[np][np];
        corrCoeff = new double[np][np];

        // get best estimates
        pmin = best.clone();

        // gradient both sides of the minimum
        double hold0 = 1.0D;
        double hold1 = 1.0D;
        for (int i = 0; i < np; ++i) {
            for (int k = 0; k < np; ++k) {
                f[k] = pmin[k];
            }
            hold0 = pmin[i];
            if (hold0 == 0.0D) {
                hold0 = step[i];
                zeroCheck = true;
            }
            f[i] = hold0 * (1.0D - delta);
            lastSSnoConstraint = sumOfSquares;
            f1 = sumSquares(regFun, f);
            f[i] = hold0 * (1.0 + delta);
            lastSSnoConstraint = sumOfSquares;
            f2 = sumSquares(regFun, f);
            grad[i][0] = (fMin - f1) / Math.abs(delta * hold0);
            grad[i][1] = (f2 - fMin) / Math.abs(delta * hold0);
        }

        // second patial derivatives at the minimum
        lastSSnoConstraint = sumOfSquares;
        for (int i = 0; i < np; ++i) {
            for (int j = 0; j < np; ++j) {
                for (int k = 0; k < np; ++k) {
                    f[k] = pmin[k];
                }
                hold0 = f[i];
                if (hold0 == 0.0D) {
                    hold0 = step[i];
                    zeroCheck = true;
                }
                f[i] = hold0 * (1.0 + delta / 2.0D);
                hold0 = f[j];
                if (hold0 == 0.0D) {
                    hold0 = step[j];
                    zeroCheck = true;
                }
                f[j] = hold0 * (1.0 + delta / 2.0D);
                lastSSnoConstraint = sumOfSquares;
                f1 = sumSquares(regFun, f);
                f[i] = pmin[i];
                f[j] = pmin[j];
                hold0 = f[i];
                if (hold0 == 0.0D) {
                    hold0 = step[i];
                    zeroCheck = true;
                }
                f[i] = hold0 * (1.0 - delta / 2.0D);
                hold0 = f[j];
                if (hold0 == 0.0D) {
                    hold0 = step[j];
                    zeroCheck = true;
                }
                f[j] = hold0 * (1.0 + delta / 2.0D);
                lastSSnoConstraint = sumOfSquares;
                f2 = sumSquares(regFun, f);
                f[i] = pmin[i];
                f[j] = pmin[j];
                hold0 = f[i];
                if (hold0 == 0.0D) {
                    hold0 = step[i];
                    zeroCheck = true;
                }
                f[i] = hold0 * (1.0 + delta / 2.0D);
                hold0 = f[j];
                if (hold0 == 0.0D) {
                    hold0 = step[j];
                    zeroCheck = true;
                }
                f[j] = hold0 * (1.0 - delta / 2.0D);
                lastSSnoConstraint = sumOfSquares;
                f3 = sumSquares(regFun, f);
                f[i] = pmin[i];
                f[j] = pmin[j];
                hold0 = f[i];
                if (hold0 == 0.0D) {
                    hold0 = step[i];
                    zeroCheck = true;
                }
                f[i] = hold0 * (1.0 - delta / 2.0D);
                hold0 = f[j];
                if (hold0 == 0.0D) {
                    hold0 = step[j];
                    zeroCheck = true;
                }
                f[j] = hold0 * (1.0 - delta / 2.0D);
                lastSSnoConstraint = sumOfSquares;
                f4 = sumSquares(regFun, f);
                stat[i][j] = (f1 - f2 - f3 + f4) / (delta * delta);
            }
        }

        // TODO: check fitter on/off implementation
        if (regFun instanceof Function) {
            ((Function) regFun).setFitterMode(true);
            final Function func = (Function) regFun;
            for (int i = 0; i < func.getParameterCount(); i++) {
                if (!func.isParameterFixed(i)) {
                    func.setParameterValue(i, pmin[i]);
                }
            }
        }

        double ss = 0.0D;
        double sc = 0.0D;
        for (int i = 0; i < nData; i++) {
            for (int j = 0; j < nXarrays; j++) {
                xd[j] = xData[j][i];
            }
            if (multipleY) {
                yCalc[i] = ((FunctionND) regFun).getValue(xd, i);
            } else {
                // TODO: check twice
                yCalc[i] = ((Function1D) regFun).getValue(xd[0]);
                // this.yCalc[i] = ((Function1D) regFun).function(pmin, xd);
            }
            residual[i] = yCalc[i] - yData[i];
            ss += TMathConstants.Sqr(residual[i]);
            residualW[i] = residual[i] / weight[i];
            sc += TMathConstants.Sqr(residualW[i]);
        }

        if (regFun instanceof Function) {
            ((Function) regFun).setFitterMode(false);
        }

        sumOfSquares = ss;

        if (weightOpt || trueFreq) {
            chiSquare = sc;
            reducedChiSquare = sc / (nData - np);
        }

        // calculate reduced sum of squares
        double red = 1.0D;
        if (!weightOpt && !trueFreq) {
            red = sumOfSquares / (nData - np);
        }

        // calculate pseudo errors - reduced sum of squares over second partial derivative
        for (int i = 0; i < np; i++) {
            pseudoSd[i] = 2.0D * delta * red * Math.abs(pmin[i]) / (grad[i][1] - grad[i][0]);
            if (pseudoSd[i] >= 0.0D) {
                pseudoSd[i] = Math.sqrt(pseudoSd[i]);
            } else {
                pseudoSd[i] = Double.NaN;
            }
        }

        // calculate covariance matrix
        if (np == 1) {
            hold0 = pmin[0];
            if (hold0 == 0.0D) {
                hold0 = step[0];
            }
            stat[0][0] = 1.0D / stat[0][0];
            covar[0][0] = stat[0][0] * red * hold0 * hold0;
            if (covar[0][0] >= 0.0D) {
                coeffSd[0] = Math.sqrt(covar[0][0]);
                corrCoeff[0][0] = 1.0D;
            } else {
                coeffSd[0] = Double.NaN;
                corrCoeff[0][0] = Double.NaN;
                posVarFlag = false;
            }
        } else {
            MatrixD cov = new MatrixD(stat);

            invertFlag = true;

            // cov = cov.inverse();
            cov = cov.pseudoInverse(1e12);

            // TODO: check purpose of matrixCheck
            // this.invertFlag = cov.getMatrixCheck();

            if (invertFlag == false) {
                flag--;
            }
            stat = cov.getArrayCopy();

            posVarFlag = true;
            if (invertFlag) {
                for (int i = 0; i < np; ++i) {
                    hold0 = pmin[i];
                    if (hold0 == 0.0D) {
                        hold0 = step[i];
                    }
                    for (int j = i; j < np; ++j) {
                        hold1 = pmin[j];
                        if (hold1 == 0.0D) {
                            hold1 = step[j];
                        }
                        covar[i][j] = 2.0D * stat[i][j] * red * hold0 * hold1;
                        covar[j][i] = covar[i][j];
                    }
                    if (covar[i][i] >= 0.0D) {
                        coeffSd[i] = Math.sqrt(covar[i][i]);
                    } else {
                        coeffSd[i] = Double.NaN;
                        posVarFlag = false;
                    }
                }

                for (int i = 0; i < np; ++i) {
                    for (int j = 0; j < np; ++j) {
                        if (Double.isFinite(coeffSd[i]) && Double.isFinite(coeffSd[j])) {
                            corrCoeff[i][j] = covar[i][j] / (coeffSd[i] * coeffSd[j]);
                        } else {
                            corrCoeff[i][j] = Double.NaN;
                        }
                    }
                }
            } else {
                for (int i = 0; i < np; ++i) {
                    for (int j = 0; j < np; ++j) {
                        covar[i][j] = Double.NaN;
                        corrCoeff[i][j] = Double.NaN;
                    }
                    coeffSd[i] = Double.NaN;
                    posVarFlag = false;
                }
            }
        }
        if (posVarFlag == false) {
            flag--;
        }

        for (int i = 0; i < nTerms; i++) {
            bestSd[i] = coeffSd[i];
            tValues[i] = best[i] / bestSd[i];
            final double atv = Math.abs(tValues[i]);
            pValues[i] = 1.0 - TMath.Student(atv, degreesOfFreedom);

        }

        multCorrelCoeff(yData, yCalc, weight);

        return flag;

    }

    /**
     * remove all constraint boundaries for the non-linear regression
     */
    public void removeConstraints() {

        // check if single parameter constraints already set
        if (!penalties.isEmpty()) {
            final int m = penalties.size();

            // remove single parameter constraints
            for (int i = m - 1; i >= 0; i--) {
                penalties.remove(i);
            }
        }
        penalty = false;
        nConstraints = 0;

        // check if mutiple parameter constraints already set
        if (!sumPenalties.isEmpty()) {
            final int m = sumPenalties.size();

            // remove multiple parameter constraints
            for (int i = m - 1; i >= 0; i--) {
                sumPenalties.remove(i);
            }
        }
        sumPenalty = false;
        nSumConstraints = 0;
        maxConstraintIndex = -1;
    }

    /**
     * Reset the tolerance used in a fixed value constraint
     *
     * @param tolerance fitting tolerance threshold
     */
    public void setConstraintTolerance(final double tolerance) {
        constraintTolerance = tolerance;
    }

    /**
     * Set data and default values
     *
     * @param xData ???
     * @param yData ???
     * @param weight ???
     */
    protected void setDefaultValues(final double[][] xData, final double[] yData, final double[] weight) {
        nData = yData.length;
        nXarrays = xData.length;
        nTerms = nXarrays;
        this.yData = new double[nData];
        yCalc = new double[nData];
        this.weight = new double[nData];
        residual = new double[nData];
        residualW = new double[nData];
        this.xData = new double[nXarrays][nData];
        int n = weight.length;
        if (n != nData) {
            throw new IllegalArgumentException("The weight and the y data lengths do not agree");
        }
        for (int i = 0; i < nData; i++) {
            this.yData[i] = yData[i];
            this.weight[i] = weight[i];
        }
        for (int j = 0; j < nXarrays; j++) {
            n = xData[j].length;
            if (n != nData) {
                throw new IllegalArgumentException(
                        "An x [" + j + "] length " + n + " and the y data length, " + nData + ", do not agree");
            }
            for (int i = 0; i < nData; i++) {
                this.xData[j][i] = xData[j][i];
            }
        }
    }

    /**
     * Set the non-linear regression fractional step size used in numerical differencing
     *
     * @param delta non-linear regression fractional step size used in numerical differencing
     */
    public void setDelta(final double delta) {
        this.delta = delta;
    }

    /**
     * Reset the non-linear regression convergence test option
     *
     * @param n 0 or 1
     */
    public void setMinTest(final int n) {
        if (n < 0 || n > 1) {
            throw new IllegalArgumentException("minTest must be 0 or 1");
        }
        minTest = n;
    }

    /**
     * Set the maximum number of iterations allowed in nonlinear regression
     *
     * @param nmax maximum number of iterations
     */
    public void setNmax(final int nmax) {
        nMax = nmax;
    }

    /**
     * Reset the Nelder and Mead contraction coefficient [gamma]
     *
     * @param con Nelder and Mead contraction coefficient [gamma]
     */
    public void setNMcontract(final double con) {
        cCoeff = con;
    }

    /**
     * Reset the Nelder and Mead extension coefficient [beta]
     *
     * @param ext extension coefficient beta
     */
    public void setNMextend(final double ext) {
        eCoeff = ext;
    }

    /**
     * Reset the Nelder and Mead reflection coefficient [alpha]
     *
     * @param refl alpha parameter
     */
    public void setNMreflect(final double refl) {
        rCoeff = refl;
    }

    /**
     *
     * @param nrs maximum number of restarts allowed in nonlinear regression
     */
    public void setNrestartsMax(final int nrs) {
        konvge = nrs;
    }

    /**
     * Reset scaling factors (scaleOpt 2, see above for scaleOpt 0 and 1)
     *
     * @param sc scaling factor
     */
    public void setScale(final double[] sc) {
        fscale = sc;
        scaleOpt = 2;
    }

    /**
     * Reset scaling factors (scaleOpt 0 and 1, see below for scaleOpt 2)
     *
     * @param n 0 or 1
     */
    public void setScale(final int n) {
        if (n < 0 || n > 1) {
            throw new IllegalArgumentException(
                    "The argument must be 0 (no scaling) 1(initial estimates all scaled to unity) or the array of scaling factors");
        }
        scaleOpt = n;
    }

    /***
     * Set the non-linear regression tolerance
     *
     * @param tol non-linear regression tolerance
     */
    public void setTolerance(final double tol) {
        fTol = tol;
    }

    /**
     * Reset the true frequency test, trueFreq true if yData values are true frequencies, e.g. in a fit to Gaussian;
     * false if not if true chiSquarePoisson (see above) is also calculated
     *
     * @param trFr ???
     */
    public void setTrueFreq(final boolean trFr) {
        final boolean trFrOld = trueFreq;
        trueFreq = trFr;
        if (trFr) {
            final boolean flag = setTrueFreqWeights(yData, weight);
            if (flag) {
                trueFreq = true;
                weightOpt = true;
            } else {
                trueFreq = false;
                weightOpt = false;
            }
        } else {
            if (trFrOld) {
                for (int i = 0; i < weight.length; i++) {
                    weight[i] = 1.0D;
                }
                weightOpt = false;
            }
        }
    }

    /**
     * Nelder and Mead simplex Default tolerance Default maximum iterations Default step option - all step[i] = dStep
     *
     * @param g function
     * @param start initial parameter values
     */
    public void simplex(final Function1D g, final double[] start) {
        if (multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        }
        final Object regFun = g;
        final int n = start.length;
        final int nMaxx = nMax;
        final double fToll = fTol;
        final double[] stepp = new double[n];
        for (int i = 0; i < n; i++) {
            stepp[i] = dStep * start[i];
        }
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, stepp, fToll, nMaxx);
    }

    /**
     * Nelder and Mead simplex Default maximum iterations Default step option - all step[i] = dStep
     *
     * @param g function
     * @param start initial parameter values
     * @param fTol fitting tolerance
     */
    public void simplex(final Function1D g, final double[] start, final double fTol) {
        if (multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        }
        final Object regFun = g;
        final int n = start.length;
        final int nMaxx = nMax;
        final double[] stepp = new double[n];
        for (int i = 0; i < n; i++) {
            stepp[i] = dStep * start[i];
        }
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, stepp, fTol, nMaxx);
    }

    /**
     * Nelder and Mead simplex Default step option - all step[i] = dStep
     *
     * @param g function
     * @param start initial parameter values
     * @param fTol fitting tolerance
     * @param nMax maximum number of iterations
     */
    public void simplex(final Function1D g, final double[] start, final double fTol, final int nMax) {
        if (multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        }
        final Object regFun = g;
        final int n = start.length;
        final double[] stepp = new double[n];
        for (int i = 0; i < n; i++) {
            stepp[i] = dStep * start[i];
        }
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, stepp, fTol, nMax);
    }

    /**
     * Nelder and Mead simplex
     *
     * @param g function
     * @param start initial parameter values
     * @param step initial parameter step values
     */
    public void simplex(final Function1D g, final double[] start, final double[] step) {
        if (multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        }
        final Object regFun = g;
        final double fToll = fTol;
        final int nMaxx = nMax;
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, step, fToll, nMaxx);
    }

    /**
     * Nelder and Mead simplex Default maximum iterations
     *
     * @param g function
     * @param start initial parameter values
     * @param step initial parameter step values
     * @param fTol fitting tolerance
     */
    public void simplex(final Function1D g, final double[] start, final double[] step, final double fTol) {
        if (multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        }
        final Object regFun = g;
        final int nMaxx = nMax;
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, step, fTol, nMaxx);
    }

    /**
     * Nelder and Mead Simplex Simplex Non-linear Regression
     *
     * @param g function
     * @param start initial parameter values
     * @param step initial parameter step values
     * @param fTol fitting tolerance
     * @param nMax maximum number of iterations
     */
    public void simplex(final Function1D g, final double[] start, final double[] step, final double fTol,
            final int nMax) {
        if (multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        }
        final Object regFun = g;
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, step, fTol, nMax);
    }

    /**
     * Nelder and Mead simplex Default tolerance
     *
     * @param g function
     * @param start initial parameter values
     * @param step initial parameter step values
     * @param nMax maximum number of iterations
     */
    public void simplex(final Function1D g, final double[] start, final double[] step, final int nMax) {
        if (multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        }
        final Object regFun = g;
        final double fToll = fTol;
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, step, fToll, nMax);
    }

    /**
     * Nelder and Mead simplex Default tolerance Default step option - all step[i] = dStep
     *
     * @param g function
     * @param start initial parameter values
     * @param nMax maximum number of iterations
     */
    public void simplex(final Function1D g, final double[] start, final int nMax) {
        if (multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        }
        final Object regFun = g;
        final int n = start.length;
        final double fToll = fTol;
        final double[] stepp = new double[n];
        for (int i = 0; i < n; i++) {
            stepp[i] = dStep * start[i];
        }
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, stepp, fToll, nMax);
    }

    /**
     * Nelder and Mead simplex Default tolerance Default maximum iterations Default step option - all step[i] = dStep
     *
     * @param g function to fit
     * @param start initial parameter values
     * @deprecated do not use yet in a production environment, needs some clean-up
     */
    @Deprecated
    public void simplex2(final FunctionND g, final double[] start) {
        if (!multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle singly dimensioned y array\nsimplex should have been called");
        }
        final Object regFun = g;
        final int n = start.length;
        final int nMaxx = nMax;
        final double fToll = fTol;
        final double[] stepp = new double[n];
        for (int i = 0; i < n; i++) {
            stepp[i] = dStep * start[i];
        }
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, stepp, fToll, nMaxx);
    }

    // FIT TO SPECIAL FUNCTIONS

    /**
     * Nelder and Mead simplex Default maximum iterations Default step option - all step[i] = dStep
     *
     * @param g function to fit
     * @param start initial parameter values
     * @param fTol fitting tolerance
     * @deprecated do not use yet in a production environment, needs some clean-up
     */
    @Deprecated
    public void simplex2(final FunctionND g, final double[] start, final double fTol) {
        if (!multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle singly dimensioned y array\nsimplex should have been called");
        }
        final Object regFun = g;
        final int n = start.length;
        final int nMaxx = nMax;
        final double[] stepp = new double[n];
        for (int i = 0; i < n; i++) {
            stepp[i] = dStep * start[i];
        }
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, stepp, fTol, nMaxx);
    }

    /**
     * Nelder and Mead simplex Default step option - all step[i] = dStep
     *
     * @param g function to fit
     * @param start initial parameter values
     * @param fTol fitting tolerance
     * @param nMax maximum number of iterations
     * @deprecated do not use yet in a production environment, needs some clean-up
     */
    @Deprecated
    public void simplex2(final FunctionND g, final double[] start, final double fTol, final int nMax) {
        if (!multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle singly dimensioned y array\nsimplex should have been called");
        }
        final Object regFun = g;
        final int n = start.length;
        final double[] stepp = new double[n];
        for (int i = 0; i < n; i++) {
            stepp[i] = dStep * start[i];
        }
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, stepp, fTol, nMax);
    }

    /**
     * Nelder and Mead simplex Default tolerance Default maximum iterations
     *
     * @param g function
     * @param start initial parameter values
     * @param step initial parameter step values
     * @deprecated do not use yet in a production environment, needs some clean-up
     */
    @Deprecated
    public void simplex2(final FunctionND g, final double[] start, final double[] step) {
        if (!multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle singly dimensioned y array\nsimplex should have been called");
        }
        final Object regFun = g;
        final double fToll = fTol;
        final int nMaxx = nMax;
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, step, fToll, nMaxx);
    }

    /**
     * Nelder and Mead simplex Default maximum iterations
     *
     * @param g function
     * @param start initial parameter values
     * @param step initial parameter step values
     * @param fTol fitting tolerance
     * @deprecated do not use yet in a production environment, needs some clean-up
     */
    @Deprecated
    public void simplex2(final FunctionND g, final double[] start, final double[] step, final double fTol) {
        if (!multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle singly dimensioned y array\nsimplex should have been called");
        }
        final Object regFun = g;
        final int nMaxx = nMax;
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, step, fTol, nMaxx);
    }

    /**
     * Nelder and Mead Simplex Simplex2 Non-linear Regression
     *
     * @param g function
     * @param start initial parameter values
     * @param step initial parameter step values
     * @param fTol fitting tolerance
     * @param nMax maximum number of iterations
     * @deprecated do not use yet in a production environment, needs some clean-up
     */
    @Deprecated
    public void simplex2(final FunctionND g, final double[] start, final double[] step, final double fTol,
            final int nMax) {
        if (!multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle singly dimensioned y array\nsimplex should have been called");
        }
        final Object regFun = g;
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, step, fTol, nMax);
    }

    /**
     * Nelder and Mead simplex Default tolerance
     *
     * @param g function
     * @param start initial parameter values
     * @param step initial parameter step values
     * @param nMax maximum number of iterations
     * @deprecated do not use yet in a production environment, needs some clean-up
     */
    @Deprecated
    public void simplex2(final FunctionND g, final double[] start, final double[] step, final int nMax) {
        if (!multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle singly dimensioned y array\nsimplex should have been called");
        }
        final Object regFun = g;
        final double fToll = fTol;
        linNonLin = false;
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, step, fToll, nMax);
    }

    /**
     * Nelder and Mead simplex Default tolerance Default step option - all step[i] = dStep
     *
     * @param g function to fit
     * @param start initial parameter values
     * @param nMax maximum number of iterations
     * @deprecated do not use yet in a production environment, needs some clean-up
     */
    @Deprecated
    public void simplex2(final FunctionND g, final double[] start, final int nMax) {
        if (!multipleY) {
            throw new IllegalArgumentException(
                    "This method cannot handle singly dimensioned y array\nsimplex should have been called");
        }

        final Object regFun = g;
        final int n = start.length;
        final double fToll = fTol;
        final double[] stepp = new double[n];
        for (int i = 0; i < n; i++) {
            stepp[i] = dStep * start[i];
        }
        zeroCheck = false;
        degreesOfFreedom = nData - start.length;
        nelderMead(regFun, start, stepp, fToll, nMax);
    }

    /**
     * Calculate the sum of squares of the residuals for non-linear regression
     *
     * @param regFun test function
     * @param testParameter test parameter
     * @return sum of squares
     */
    protected double sumSquares(final Object regFun, final double[] testParameter) {
        double ss = -3.0D;
        final double[] param = new double[nTerms];
        final double[] xd = new double[nXarrays];
        // rescale
        for (int i = 0; i < nTerms; i++) {
            param[i] = testParameter[i] / fscale[i];
        }

        // single parameter penalty functions
        final double tempFunctVal = lastSSnoConstraint;
        boolean test = true;
        if (penalty) {
            int k = 0;
            for (int i = 0; i < nConstraints; i++) {
                k = penaltyParam[i];
                switch (penaltyCheck[i]) {
                case -1:
                    if (param[k] < constraints[i]) {
                        ss = tempFunctVal + penaltyWeight * TMathConstants.Sqr(constraints[i] - param[k]);
                        test = false;
                    }
                    break;
                case 0:
                    if (param[k] < constraints[i] * (1.0 - constraintTolerance)) {
                        ss = tempFunctVal + penaltyWeight
                                * TMathConstants.Sqr(constraints[i] * (1.0 - constraintTolerance) - param[k]);
                        test = false;
                    }
                    if (param[k] > constraints[i] * (1.0 + constraintTolerance)) {
                        ss = tempFunctVal + penaltyWeight
                                * TMathConstants.Sqr(param[k] - constraints[i] * (1.0 + constraintTolerance));
                        test = false;
                    }
                    break;
                case 1:
                    if (param[k] > constraints[i]) {
                        ss = tempFunctVal + penaltyWeight * TMathConstants.Sqr(param[k] - constraints[i]);
                        test = false;
                    }
                    break;
                }
            }
        }

        // multiple parameter penalty functions
        if (sumPenalty) {
            int kk = 0;
            double pSign = 0;
            double sumPenaltySum = 0.0D;
            for (int i = 0; i < nSumConstraints; i++) {
                for (int j = 0; j < sumPenaltyNumber[i]; j++) {
                    kk = sumPenaltyParam[i][j];
                    pSign = sumPlusOrMinus[i][j];
                    sumPenaltySum += param[kk] * pSign;
                }
                switch (sumPenaltyCheck[i]) {
                case -1:
                    if (sumPenaltySum < sumConstraints[i]) {
                        ss = tempFunctVal + penaltyWeight * TMathConstants.Sqr(sumConstraints[i] - sumPenaltySum);
                        test = false;
                    }
                    break;
                case 0:
                    if (sumPenaltySum < sumConstraints[i] * (1.0 - constraintTolerance)) {
                        ss = tempFunctVal + penaltyWeight
                                * TMathConstants.Sqr(sumConstraints[i] * (1.0 - constraintTolerance) - sumPenaltySum);
                        test = false;
                    }
                    if (sumPenaltySum > sumConstraints[i] * (1.0 + constraintTolerance)) {
                        ss = tempFunctVal + penaltyWeight
                                * TMathConstants.Sqr(sumPenaltySum - sumConstraints[i] * (1.0 + constraintTolerance));
                        test = false;
                    }
                    break;
                case 1:
                    if (sumPenaltySum > sumConstraints[i]) {
                        ss = tempFunctVal + penaltyWeight * TMathConstants.Sqr(sumPenaltySum - sumConstraints[i]);
                        test = false;
                    }
                    break;
                }
            }
        }

        // compute \chi^2 estimate
        if (test) {
            if (regFun instanceof Function) {
                ((Function) regFun).setFitterMode(true);
                final Function func = (Function) regFun;
                for (int i = 0; i < func.getParameterCount(); i++) {
                    if (!func.isParameterFixed(i)) {
                        func.setParameterValue(i, param[i]);
                    }
                }
            }

            /*
             * ss = 0.0; for (int i = 0; i < this.nData; i++) { for (int j = 0; j < nXarrays; j++) { xd[j] =
             * this.xData[j][i]; } if (!this.multipleY) { ss += TMath.Sqr((this.yData[i] - g1.getValue(xd[0])) /
             * this.weight[i]); } else { ss += TMath.Sqr((this.yData[i] - g2.getValue(xd, i)) /this.weight[i]); } }
             */

            ss = 0.0;
            if (!multipleY) {
                final Function1D g1 = (Function1D) regFun;
                for (int i = 0; i < nData; i++) {

                    for (int j = 0; j < nXarrays; j++) {
                        xd[j] = xData[j][i];
                    }

                    ss += TMathConstants.Sqr((yData[i] - g1.getValue(xd[0])) / weight[i]);
                }
            } else {
                final FunctionND g2 = (FunctionND) regFun;

                /*
                 * for (int i = 0; i < this.nData; i++) { for (int j = 0; j < nXarrays; j++) { xd[j] = this.xData[j][i];
                 * }
                 *
                 * ss += TMath.Sqr((this.yData[i] - g2.getValue(xd, i)) / this.weight[i]); }
                 */
                // final int dimOut = g2.getOutputDimension();
                final int dimIn = g2.getInputDimension();
                final int length = nData / dimIn;
                System.err.println("length = " + nData + " dim = " + dimIn);

                for (int i = 0; i < length; i++) {
                    for (int j = 0; j < nXarrays; j++) {
                        xd[j] = xData[j][i];
                    }

                    for (int dim = 0; dim < dimIn; dim++) {
                        int index = dim * length + i;
                        index = i;
                        ss += TMathConstants.Sqr((yData[index] - g2.getValue(xd, index)) / weight[index]);
                    }

                }
            }

            lastSSnoConstraint = ss;
            if (regFun instanceof Function) {
                ((Function) regFun).setFitterMode(false);
            }

        }

        return ss;

    }

    /**
     * check data arrays for sign, max, min and peak
     *
     * @param data input data
     * @return data arrays for sign, max, min and peak
     * @deprecated replace with optimised routines
     */
    @Deprecated
    protected static ArrayList<Object> dataSign(final double[] data) {

        final ArrayList<Object> ret = new ArrayList<>();
        final int n = data.length;

        //
        double peak = 0.0D; // peak: larger of maximum and any abs(negative minimum)
        int peaki = -1; // index of above
        double shift = 0.0D; // shift to make all positive if a mixture of positive and negative
        double max = data[0]; // maximum
        int maxi = 0; // index of above
        double min = data[0]; // minimum
        int mini = 0; // index of above
        int signCheckPos = 0; // number of negative values
        int signCheckNeg = 0; // number of positive values
        int signCheckZero = 0; // number of zero values
        int signFlag = -1; // 0 all positive; 1 all negative; 2 positive and negative
        double mean = 0.0D; // mean value

        for (int i = 0; i < n; i++) {
            mean = +data[i];
            if (data[i] > max) {
                max = data[i];
                maxi = i;
            }
            if (data[i] < min) {
                min = data[i];
                mini = i;
            }
            if (data[i] == 0.0D) {
                signCheckZero++;
            }
            if (data[i] > 0.0D) {
                signCheckPos++;
            }
            if (data[i] < 0.0D) {
                signCheckNeg++;
            }
        }
        mean /= n;

        if (signCheckZero + signCheckPos == n) {
            peak = max;
            peaki = maxi;
            signFlag = 0;
        } else {
            if (signCheckZero + signCheckNeg == n) {
                peak = min;
                peaki = mini;
                signFlag = 1;
            } else {
                peak = max;
                peaki = maxi;
                if (-min > max) {
                    peak = min;
                    peak = mini;
                }
                signFlag = 2;
                shift = -min;
            }
        }

        // transfer results to the ArrayList
        ret.add(Double.valueOf(min));
        ret.add(Integer.valueOf(mini));
        ret.add(Double.valueOf(max));
        ret.add(Integer.valueOf(maxi));
        ret.add(Double.valueOf(peak));
        ret.add(Integer.valueOf(peaki));
        ret.add(Integer.valueOf(signFlag));
        ret.add(Double.valueOf(shift));
        ret.add(Double.valueOf(mean));

        return ret;
    }

    /**
     * returns estimate of half-height width
     *
     * @param xData x coordinate of input data
     * @param yData y coordinate of input data
     * @return estimate of half-height width
     */
    protected static double halfWidth(final double[] xData, final double[] yData) {
        double ymax = yData[0];
        int imax = 0;
        final int n = xData.length;

        for (int i = 1; i < n; i++) {
            if (yData[i] > ymax) {
                ymax = yData[i];
                imax = i;
            }
        }
        ymax /= 2.0D;

        double halflow = -1.0D;
        double temp = -1.0D;
        int ihl = -1;
        if (imax > 0) {
            ihl = imax - 1;
            halflow = Math.abs(ymax - yData[ihl]);
            for (int i = imax - 2; i >= 0; i--) {
                temp = Math.abs(ymax - yData[i]);
                if (temp < halflow) {
                    halflow = temp;
                    ihl = i;
                }
            }
            halflow = Math.abs(xData[ihl] - xData[imax]);
        }

        double halfhigh = -1.0D;
        temp = -1.0D;
        int ihh = -1;
        if (imax < n - 1) {
            ihh = imax + 1;
            halfhigh = Math.abs(ymax - yData[ihh]);
            for (int i = imax + 2; i < n; i++) {
                temp = Math.abs(ymax - yData[i]);
                if (temp < halfhigh) {
                    halfhigh = temp;
                    ihh = i;
                }
            }
            halfhigh = Math.abs(xData[ihh] - xData[imax]);
        }

        double halfw = 0.0D;
        int nd = 0;
        if (ihl != -1) {
            halfw += halflow;
            nd++;
        }
        if (ihh != -1) {
            halfw += halfhigh;
            nd++;
        }
        halfw /= nd;

        return halfw;
    }

    protected static boolean setTrueFreqWeights(final double[] yData, final double[] weight) {
        final int nData = yData.length;
        final boolean flag = true;

        // Set all weights to square root of frequency of occurence
        for (int ii = 0; ii < nData; ii++) {
            weight[ii] = Math.sqrt(Math.abs(yData[ii]));
        }

        // Check for zero weights and take average of neighbours as weight if it is zero
        for (int ii = 0; ii < nData; ii++) {
            double last = 0.0D;
            double next = 0.0D;
            if (weight[ii] == 0) {
                // find previous non-zero value
                boolean testLast = true;
                int iLast = ii - 1;
                while (testLast) {
                    if (iLast < 0) {
                        testLast = false;
                    } else {
                        if (weight[iLast] == 0.0D) {
                            iLast--;
                        } else {
                            last = weight[iLast];
                            testLast = false;
                        }
                    }
                }

                // find next non-zero value
                boolean testNext = true;
                int iNext = ii + 1;
                while (testNext) {
                    if (iNext >= nData) {
                        testNext = false;
                    } else {
                        if (weight[iNext] == 0.0D) {
                            iNext++;
                        } else {
                            next = weight[iNext];
                            testNext = false;
                        }
                    }
                }

                // Take average
                weight[ii] = (last + next) / 2.0D;
            }
        }
        return flag;
    }

    /**
     * sort elements x, y and w arrays of doubles into ascending order of the x array using selection sort method
     *
     * @param x ???
     * @param y ???
     * @param w ???
     */
    protected static void sort(final double[] x, final double[] y, final double[] w) {
        int index = 0;
        int lastIndex = -1;
        final int n = x.length;
        double holdx = 0.0D;
        double holdy = 0.0D;
        double holdw = 0.0D;

        while (lastIndex < n - 1) {
            index = lastIndex + 1;
            for (int i = lastIndex + 2; i < n; i++) {
                if (x[i] < x[index]) {
                    index = i;
                }
            }
            lastIndex++;
            holdx = x[index];
            x[index] = x[lastIndex];
            x[lastIndex] = holdx;
            holdy = y[index];
            y[index] = y[lastIndex];
            y[lastIndex] = holdy;
            holdw = w[index];
            w[index] = w[lastIndex];
            w[lastIndex] = holdw;
        }
    }

}
