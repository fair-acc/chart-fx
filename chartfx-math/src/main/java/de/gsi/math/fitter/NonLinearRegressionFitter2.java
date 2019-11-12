package de.gsi.math.fitter;

import java.util.ArrayList;

import de.gsi.math.ArrayConversion;
import de.gsi.math.TMath;
import de.gsi.math.TMathConstants;
import de.gsi.math.functions.Function;
import de.gsi.math.functions.Function1D;
import de.gsi.math.functions.FunctionND;
import de.gsi.math.matrix.MatrixD;
import de.gsi.math.storage.DoubleStorage1D;
import de.gsi.math.storage.VoxelArrayND;

/**
 * Non-linear regression class Nelder &amp; Mead simplex algorithm being the primary back-bone of this implementation
 * initial implementation based on the package provided by: Michael Thomas Flanagan at www.ee.ucl.ac.uk/~mflanaga,
 * appears in all copies The code has been cleaned up and adapted to support true MxN multi-dimensional fits
 *
 * @author rstein
 * @deprecated need to fix the n-dimensional handling - do not use (yet)
 */
@Deprecated
public class NonLinearRegressionFitter2 {

    // HISTOGRAM CONSTRUCTION
    // Tolerance used in including an upper point in last histogram bin when it is outside due to riunding erors
    protected static double histTol = 1.0001D;
    protected int nData = 0; // number of y data points
    protected int nYarrays = 1; // number of y arrays

    protected int nTerms = 0; // number of unknown parameters to be estimated
    // multiple linear (a + b.x1 +c.x2 + . . ., = nXarrays + 1
    // polynomial fitting; = polynomial degree + 1
    // generalised linear; = nXarrays
    // simplex = no. of parameters to be estimated
    protected int degreesOfFreedom = 0; // degrees of freedom = nData - nTerms
    protected VoxelArrayND xData = null; // x data values
    protected VoxelArrayND yData = null; // y data values
    protected VoxelArrayND yCalc = null; // calculated y values using the regression coefficients
    protected VoxelArrayND weight = null; // weighting factors
    protected VoxelArrayND residual = null; // residuals
    protected VoxelArrayND residualW = null; // weighted residuals
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
     * Constructor with data with x as 1D array and weights provided
     *
     * @param xData x coordinate of input data
     * @param yData y coordinate of input data
     * @param weight weighting vector
     */
    public NonLinearRegressionFitter2(final double[] xData, final double[] yData, final double[] weight) {
        setData(xData, yData, weight);
    }

    /**
     * Constructor with data with x as 1D array and weights provided
     *
     * @param xData x coordinate of input data
     * @param yData y coordinate of input data
     * @param weights weighting vector
     */
    public NonLinearRegressionFitter2(final VoxelArrayND xData, final VoxelArrayND yData, final VoxelArrayND weights) {
        this.setData(xData, yData, weights);
    }

    // add a single parameter constraint boundary for the non-linear regression
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

    // add a multiple parameter constraint boundary for the non-linear regression
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

    // add a multiple parameter constraint boundary for the non-linear regression
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
     * @param weights weighting vector
     * @return the regularised weight vector
     */
    protected VoxelArrayND checkForZeroWeights(final VoxelArrayND weights) {
        weightOpt = true;
        int nZeros = 0;

        if (weights == null) {
            throw new IllegalArgumentException("weights are null");
        }

        // count number of zeros
        for (int i = 0; i < weights.getLocalStorageDim(); i++) {
            final double[] val = weights.getLocal(i);
            for (int j = 0; j < val.length; j++) {
                if (val[j] <= 0.0) {
                    nZeros++;
                }
            }
        }
        final double perCentZeros = 100.0 * nZeros / weights.getLocalStorageDim();

        if (perCentZeros > 40.0) {
            System.out.println(perCentZeros + "% of the weights are zero or less; all weights set to 1.0");
            weight.initialiseWithValue(1.0);
            weightOpt = false;

        } else {
            if (perCentZeros > 0.0D) {
                // TODO: deal with negative and zero weight factors
                weight.initialiseWithValue(1.0);
                /*
                 * for (int i = 0; i < n; i++) {
                 *
                 * if (weight[i] <= 0.0) { // weight is negative or zero
                 *
                 *
                 * if (i == 0) { int ii = 1; for (boolean test = true; test; ) { if (weight[ii] > 0.0D) { double ww =
                 * weight[0]; weight[0] = weight[ii]; System.out.println("weight at point " + i + ", " + ww +
                 * ", replaced by " + weight[i]); test = false; } else { ii++; } } }
                 *
                 * if (i == (n - 1)) { int ii = n - 2; boolean test = true; while (test) { if (weight[ii] > 0.0D) {
                 * double ww = weight[i]; weight[i] = weight[ii]; System.out.println("weight at point " + i + ", " + ww
                 * + ", replaced by " + weight[i]); test = false; } else { ii--; } } }
                 *
                 * if (i > 0 && i < (n - 2)) { double lower = 0.0; double upper = 0.0; int ii = i - 1; boolean test =
                 * true; while (test) { if (weight[ii] > 0.0D) { lower = weight[ii]; test = false; } else { ii--; if (ii
                 * == 0) test = false; } } ii = i + 1; test = true; while (test) { if (weight[ii] > 0.0D) { upper =
                 * weight[ii]; test = false; } else { ii++; if (ii == (n - 1)) test = false; } } double ww = weight[i];
                 * if (lower == 0.0) { weight[i] = upper; } else { if (upper == 0.0) { weight[i] = lower; } else {
                 * weight[i] = (lower + upper) / 2.0; } } System.out.println("weight at point " + i + ", " + ww +
                 * ", replaced by " + weight[i]); } }
                 *
                 *
                 * }
                 */
            }

        }

        return weight;
    }

    // check for zero and negative values
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

    // Get the best estimates of the unknown parameters
    public double[] getBestEstimates() {
        return best.clone();
    }

    // Get the estimates of the errors of the best estimates of the unknown parameters
    public double[] getBestEstimatesErrors() {
        return bestSd.clone();
    }

    // Get the estimates of the standard deviations of the best estimates of the unknown parameters
    public double[] getbestestimatesStandardDeviations() {
        return bestSd.clone();
    }

    // Get the estimates of the errors of the best estimates of the unknown parameters
    public double[] getBestEstimatesStandardDeviations() {
        return bestSd.clone();
    }

    // Get the chi square estimate
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

    // Get the chi square probablity
    public double getchiSquareProb() {
        double ret = 0.0D;
        if (weightOpt) {
            ret = 1.0D - TMath.ChisquareQuantile(chiSquare, nData - 1);
        } else {
            System.out.println(
                    "A Chi Square probablity cannot be calculated as data are neither true frequencies nor weighted");
            System.out.println("A value of -1 is returned as Reduced Chi Square");
            ret = -1.0D;
        }
        return ret;
    }

    // Get the best estimates of the unknown parameters
    public double[] getCoeff() {
        return best.clone();
    }

    // Get the estimates of the errors of the best estimates of the unknown parameters
    public double[] getCoeffSd() {
        return bestSd.clone();
    }

    // Get the cofficients of variations of the best estimates of the unknown parameters
    public double[] getCoeffVar() {
        final double[] coeffVar = new double[nTerms];

        for (int i = 0; i < nTerms; i++) {
            coeffVar[i] = bestSd[i] * 100.0D / best[i];
        }
        return coeffVar;
    }

    // Get the correlation coefficient matrix
    public double[][] getCorrCoeffMatrix() {
        return corrCoeff;
    }

    // Get the covariance matrix
    public double[][] getCovMatrix() {
        return covar;
    }

    // Get the degrees of freedom
    public double getDegFree() {
        return degreesOfFreedom;
    }

    // Get the non-linear regression fractional step size used in numerical differencing
    public double getDelta() {
        return delta;
    }

    // Get the non-linear regression pre and post minimum gradients
    public double[][] getGrad() {
        return grad;
    }

    // Get the non-linear regression statistics Hessian matrix inversion status flag
    public boolean getInversionCheck() {
        return invertFlag;
    }

    // Get the non-linear regression convergence test option
    public int getMinTest() {
        return minTest;
    }

    // Get the number of iterations in nonlinear regression
    public int getNiter() {
        return nIter;
    }

    // Get the non-linear regression status
    // true if convergence was achieved
    // false if convergence not achieved before maximum number of iterations
    // current values then returned
    public boolean getNlrStatus() {
        return nlrStatus;
    }

    // Get the maximum number of iterations allowed in nonlinear regression
    public int getNmax() {
        return nMax;
    }

    // Get the Nelder and Mead contraction coefficient [gamma]
    public double getNMcontract() {
        return cCoeff;
    }

    // Get the Nelder and Mead extension coefficient [beta]
    public double getNMextend() {
        return eCoeff;
    }

    // Get the Nelder and Mead reflection coefficient [alpha]
    public double getNMreflect() {
        return rCoeff;
    }

    // Get the number of restarts in nonlinear regression
    public int getNrestarts() {
        return kRestart;
    }

    // Get the maximum number of restarts allowed in nonlinear regression
    public int getNrestartsMax() {
        return konvge;
    }

    // Get the non-linear regression statistics Hessian matrix inverse diagonal status flag
    public boolean getPosVarCheck() {
        return posVarFlag;
    }

    // Get the pseudo-estimates of the errors of the best estimates of the unknown parameters
    public double[] getPseudoErrors() {
        return pseudoSd.clone();
    }

    // Get the pseudo-estimates of the errors of the best estimates of the unknown parameters
    public double[] getPseudoSd() {
        return pseudoSd.clone();
    }

    // Get the p-values of the best estimates
    public double[] getPvalues() {
        return pValues.clone();
    }

    // Get the reduced chi square estimate
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

    // Get the unweighted residuals, y(experimental) - y(calculated)
    public double[] getResiduals() {
        final double[] temp = new double[nData];
        for (int i = 0; i < nData; i++) {
            // TODO: fix here
            // temp[i] = this.yData[i] - this.yCalc[i];
        }
        return temp;
    }

    // Get scaling factors
    public double[] getScale() {
        return fscale;
    }

    // Get the simplex sd at the minimum
    public double getSimplexSd() {
        return simplexSd;
    }

    // Get the unweighted sum of squares of the residuals
    public double getSumOfSquares() {
        return sumOfSquares;
    }

    // Get the non-linear regression tolerance
    public double getTolerance() {
        return fTol;
    }

    // Get the true frequency test, trueFreq
    public boolean getTrueFreq() {
        return trueFreq;
    }

    // Get the t-values of the best estimates
    public double[] getTvalues() {
        return tValues.clone();
    }

    // Get the weighted residuals, (y(experimental) - y(calculated))/weight
    public double[] getWeightedResiduals() {

        final double[] temp = new double[nData];
        for (int i = 0; i < nData; i++) {
            // TODO: fix here
            // temp[i] = (this.yData[i] - this.yCalc[i]) / weight[i];
        }
        return temp;
    }

    /**
     * Get the input x values
     *
     * @return array with x coordinates
     */
    public double[] getXdata() {
        if (xData instanceof DoubleStorage1D) {
            return ((DoubleStorage1D) xData).getArray().clone();
        }

        // TODO fix here
        return null;
    }

    // Get the calculated y values
    public double[] getYcalc() {
        if (yCalc instanceof DoubleStorage1D) {
            return ((DoubleStorage1D) yCalc).getArray().clone();
        }
        // TODO fix here
        return null;
    }

    /**
     * Get the inputed y values
     *
     * @return array with input y coordinates
     */
    public double[] getYdata() {
        if (yData instanceof DoubleStorage1D) {
            return ((DoubleStorage1D) yData).getArray().clone();
        }
        // TODO fix here
        return null;
    }

    // Ignore check on whether degrees of freedom are greater than zero
    public void ignoreDofFcheck() {
        ignoreDofFcheck = true;
    }

    /**
     * Nelder and Mead Simplex Simplex Non-linear Regression
     *
     * @param regFun test function
     * @param start initial parameter values
     * @param step initial parameter step size
     * @param fTol regression tolerance
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
        double yabsmean = 0.0;
        int yabscount = 0;
        for (int i = 0; i < yData.getLocalStorageDim(); i++) {
            final double[] val = yData.getLocal(i);
            for (int j = 0; j < val.length; j++) {
                yabsmean += Math.abs(val[j]);
                yabscount++;
            }
        }
        yabsmean /= yabscount;

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

        // Store un-scaled start values
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
            start[j] += step[j];

            for (int i = 0; i < np; ++i) {
                pp[i][j] = start[i];
            }
            yy[j] = sumSquares(regFun, start);

            start[j] -= step[j];
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
                // TODO check whether we need this switch
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
     * @return 0 if method fully successful; -1 posVarFlag or invertFlag is false; -2 posVarFlag and invertFlag are
     *         false
     */
    protected int pseudoLinearStats(final Object regFun) {
        double f1 = 0.0D, f2 = 0.0D, f3 = 0.0D, f4 = 0.0D; // intermediate values in numerical differentiation
        int flag = 0;
        // returned as 0 if method fully successful;
        // negative if partially successful or unsuccessful: check posVarFlag and invertFlag
        // -1 posVarFlag or invertFlag is false;
        // -2 posVarFlag and invertFlag are false
        final int np = nTerms;

        final double[] f = new double[np];
        double[] pmin = new double[np];
        final double[] coeffSd = new double[np];
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

            // computes f(x-epsilon)
            f[i] = hold0 * (1.0D - delta);
            lastSSnoConstraint = sumOfSquares;
            f1 = sumSquares(regFun, f);

            // computes f(x+epsilon)
            f[i] = hold0 * (1.0 + delta);
            lastSSnoConstraint = sumOfSquares;
            f2 = sumSquares(regFun, f);

            // computes gradient around minimum
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

        for (int i = 0; i < yCalc.getLocalStorageDim(); i++) {

            if (regFun instanceof FunctionND) {
                final double[] xd = xData.getLocal(i);
                final double[] val = ((FunctionND) regFun).getValue(xd);
                yCalc.setLocal(i, val);
            } else if (regFun instanceof Function1D) {
                final double[] xd = ((DoubleStorage1D) xData).getArray();
                // TODO: check twice
                final double[] yCalcLocal = ((DoubleStorage1D) yCalc).getArray();
                // final double[] val = ((Function1D) regFun).getValues(xd);
                // System.arraycopy(val, 0, yCalcLocal, 0, val.length);
                for (int j = 0; j < xd.length; j++) {
                    yCalcLocal[j] = ((Function1D) regFun).getValue(xd[j]);
                }

                // this.yCalc[i] = ((Function1D) regFun).function(pmin, xd);
            }
        }

        for (int i = 0; i < yCalc.getLocalStorageDim(); i++) {
            final double[] val = TMath.Difference(yCalc.getLocal(i), yData.getLocal(i));
            final double[] val2 = residualW.getLocal(i);
            final double[] weightLocal = weight.getLocal(i);
            residual.setLocal(i, val);

            for (int j = 0; j < val.length; j++) {
                ss += TMathConstants.Sqr(val[j]);
                val2[j] = val[j] / weightLocal[j];
                sc += TMathConstants.Sqr(val2[j]);
            }
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
            cov.print(5, 5);
            cov = cov.pseudoInverse(1e12);
            cov.print(5, 5);

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

        return flag;
    }

    // remove all constraint boundaries for the non-linear regression
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

    // Reset the tolerance used in a fixed value constraint
    public void setConstraintTolerance(final double tolerance) {
        constraintTolerance = tolerance;
    }

    public void setData(final double[] xData, final double[] yData, final double[] weights) {
        final DoubleStorage1D xDataLocal = new DoubleStorage1D(xData);
        final DoubleStorage1D yDataLocal = new DoubleStorage1D(xData);
        final DoubleStorage1D weightLocal = new DoubleStorage1D(weights);
        if (weights == null) {
            System.err.println("weights are null");
        }
        setData(xDataLocal, yDataLocal, weightLocal);
    }

    public void setData(final VoxelArrayND xData, final VoxelArrayND yData, final VoxelArrayND weights) {
        weight = checkForZeroWeights(weights);
        if (weightOpt) {
            weightFlag = 1;
        }
        this.xData = xData;
        this.yData = yData;
        weight = weights;

        yCalc = yData.copy();
        residual = yData.copy();
        residualW = yData.copy();

    }

    // Set the non-linear regression fractional step size used in numerical differencing
    public void setDelta(final double delta) {
        this.delta = delta;
    }

    // Reset the non-linear regression convergence test option
    public void setMinTest(final int n) {
        if (n < 0 || n > 1) {
            throw new IllegalArgumentException("minTest must be 0 or 1");
        }
        minTest = n;
    }

    // Set the maximum number of iterations allowed in nonlinear regression
    public void setNmax(final int nmax) {
        nMax = nmax;
    }

    // Reset the Nelder and Mead contraction coefficient [gamma]
    public void setNMcontract(final double con) {
        cCoeff = con;
    }

    // Reset the Nelder and Mead extension coefficient [beta]
    public void setNMextend(final double ext) {
        eCoeff = ext;
    }

    // Reset the Nelder and Mead reflection coefficient [alpha]
    public void setNMreflect(final double refl) {
        rCoeff = refl;
    }

    // Set the maximum number of restarts allowed in nonlinear regression
    public void setNrestartsMax(final int nrs) {
        konvge = nrs;
    }

    // Reset scaling factors (scaleOpt 2, see above for scaleOpt 0 and 1)
    public void setScale(final double[] sc) {
        fscale = sc;
        scaleOpt = 2;
    }

    // Reset scaling factors (scaleOpt 0 and 1, see below for scaleOpt 2)
    public void setScale(final int n) {
        if (n < 0 || n > 1) {
            throw new IllegalArgumentException(
                    "The argument must be 0 (no scaling) 1(initial estimates all scaled to unity) or the array of scaling factors");
        }
        scaleOpt = n;
    }

    // Set the non-linear regression tolerance
    public void setTolerance(final double tol) {
        fTol = tol;
    }

    // Reset the true frequency test, trueFreq
    // true if yData values are true frequencies, e.g. in a fit to Gaussian; false if not
    // if true chiSquarePoisson (see above) is also calculated
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
                weight.initialiseWithValue(1.0);
                weightOpt = false;
            }
        }
    }

    // Nelder and Mead simplex
    // Default tolerance
    // Default maximum iterations
    // Default step option - all step[i] = dStep
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

    // Nelder and Mead simplex
    // Default maximum iterations
    // Default step option - all step[i] = dStep
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

    // Nelder and Mead simplex
    // Default step option - all step[i] = dStep
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

    // Nelder and Mead simplex
    // Default tolerance
    // Default maximum iterations
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

    // Nelder and Mead simplex
    // Default maximum iterations
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

    // Nelder and Mead Simplex Simplex Non-linear Regression
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

    // Nelder and Mead simplex
    // Default tolerance
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

    // Nelder and Mead simplex
    // Default tolerance
    // Default step option - all step[i] = dStep
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

    // Nelder and Mead simplex
    // Default tolerance
    // Default maximum iterations
    // Default step option - all step[i] = dStep
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

    // Nelder and Mead simplex
    // Default maximum iterations
    // Default step option - all step[i] = dStep
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

    // Nelder and Mead simplex
    // Default step option - all step[i] = dStep
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

    // Nelder and Mead simplex
    // Default tolerance
    // Default maximum iterations
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

    // Nelder and Mead simplex
    // Default maximum iterations
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

    // Nelder and Mead Simplex Simplex2 Non-linear Regression
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

    // Nelder and Mead simplex
    // Default tolerance
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

    // Nelder and Mead simplex
    // Default tolerance
    // Default step option - all step[i] = dStep
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
     * @return the sum of all square values
     */
    protected double sumSquares(final Object regFun, final double[] testParameter) {
        double ss = -3.0D;
        final double[] param = new double[nTerms];

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

            if (regFun instanceof Function1D) {
                final Function1D g1 = (Function1D) regFun;
                double[] xd, yd, weightd = null;

                if (xData instanceof DoubleStorage1D) {
                    xd = ((DoubleStorage1D) xData).getArray();
                } else {
                    throw new RuntimeException("x-data storage is not a 1D array");
                }

                if (yData instanceof DoubleStorage1D) {
                    yd = ((DoubleStorage1D) yData).getArray();
                } else {
                    throw new RuntimeException("y-data storage is not a 1D array");
                }

                if (weight instanceof DoubleStorage1D) {
                    weightd = ((DoubleStorage1D) weight).getArray();
                } else {
                    throw new RuntimeException("weight-data storage is not a 1D array");
                }

                for (int i = 0; i < yd.length; i++) {
                    ss += TMathConstants.Sqr((yd[i] - g1.getValue(xd[i])) / weightd[i]);
                }

            } else {
                final FunctionND g2 = (FunctionND) regFun;
                double[] xd = new double[xData.getValueDimension()];
                double[] yd = new double[yData.getValueDimension()];
                double[] weightd = new double[yData.getValueDimension()];

                // loops over all dimensions
                for (int i = 0; i < xData.getLocalStorageDim(); i++) {
                    xd = xData.getLocal(i);
                    yd = yData.getLocal(i);
                    weightd = weight.getLocal(i);
                    final double[] ysim = g2.getValue(xd);

                    final int vlength = ysim.length;
                    for (int index = 0; index < vlength; index++) {
                        ss += TMathConstants.Sqr((yd[index] - ysim[index]) / weightd[index]);
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

    // returns estimate of half-height width
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

    // Distribute data into bins to obtain histogram
    // zero bin position provided
    public static double[][] histogramBins(final double[] data, final double binWidth, final double binZero) {
        final double dmax = TMath.Maximum(data);
        int nBins = (int) Math.ceil((dmax - binZero) / binWidth);
        if (binZero + nBins * binWidth > dmax) {
            nBins++;
        }
        final int nPoints = data.length;
        final int[] dataCheck = new int[nPoints];
        for (int i = 0; i < nPoints; i++) {
            dataCheck[i] = 0;
        }
        final double[] binWall = new double[nBins + 1];
        binWall[0] = binZero;
        for (int i = 1; i <= nBins; i++) {
            binWall[i] = binWall[i - 1] + binWidth;
        }
        final double[][] binFreq = new double[2][nBins];
        for (int i = 0; i < nBins; i++) {
            binFreq[0][i] = (binWall[i] + binWall[i + 1]) / 2.0D;
            binFreq[1][i] = 0.0D;
        }
        boolean test = true;

        for (int i = 0; i < nPoints; i++) {
            test = true;
            int j = 0;
            while (test) {
                if (j == nBins - 1) {
                    if (data[i] >= binWall[j]
                            && data[i] <= binWall[j + 1] * (1.0D + NonLinearRegressionFitter.histTol)) {
                        binFreq[1][j] += 1.0D;
                        dataCheck[i] = 1;
                        test = false;
                    }
                } else {
                    if (data[i] >= binWall[j] && data[i] < binWall[j + 1]) {
                        binFreq[1][j] += 1.0D;
                        dataCheck[i] = 1;
                        test = false;
                    }
                }
                if (test) {
                    if (j == nBins - 1) {
                        test = false;
                    } else {
                        j++;
                    }
                }
            }
        }
        int nMissed = 0;
        for (int i = 0; i < nPoints; i++) {
            if (dataCheck[i] == 0) {
                nMissed++;
                System.out.println("p " + i + " " + data[i] + " " + binWall[0] + " " + binWall[nBins]);
            }
        }
        if (nMissed > 0) {
            System.out.println(nMissed + " data points, outside histogram limits, excluded in histogramBins");
        }
        return binFreq;
    }

    // HISTOGRAM METHODS
    // Distribute data into bins to obtain histogram
    // zero bin position and upper limit provided
    public static double[][] histogramBins(final double[] data, final double binWidth, final double binZero,
            final double binUpper) {
        int n = 0; // new array length
        final int m = data.length; // old array length;
        for (int i = 0; i < m; i++) {
            if (data[i] <= binUpper) {
                n++;
            }
        }
        if (n != m) {
            final double[] newData = new double[n];
            int j = 0;
            for (int i = 0; i < m; i++) {
                if (data[i] <= binUpper) {
                    newData[j] = data[i];
                    j++;
                }
            }
            System.out.println(m - n + " data points, above histogram upper limit, excluded in histogramBins");
            return histogramBins(newData, binWidth, binZero);
        } else {
            return histogramBins(data, binWidth, binZero);

        }
    }

    protected static boolean setTrueFreqWeights(final VoxelArrayND yData, final VoxelArrayND weight) {
        final boolean flag = true;

        // Set all weights to square root of frequency of occurrence
        for (int ii = 0; ii < yData.getLocalStorageDim(); ii++) {
            // TODO: refactor here
            // weight[ii] = Math.sqrt(Math.abs(yData[ii]));
        }

        // Check for zero weights and take average of neighbours as weight if it is zero
        for (int ii = 0; ii < yData.getLocalStorageDim(); ii++) {
            final double last = 0.0D;
            final double next = 0.0D;

            // TODO: refactor here
            /*
             * if (weight[ii] == 0) { // find previous non-zero value boolean testLast = true; int iLast = ii - 1; while
             * (testLast) { if (iLast < 0) { testLast = false; } else { if (weight[iLast] == 0.0D) { iLast--; } else {
             * last = weight[iLast]; testLast = false; } } }
             *
             * // find next non-zero value boolean testNext = true; int iNext = ii + 1; while (testNext) { if (iNext >=
             * nData) { testNext = false; } else { if (weight[iNext] == 0.0D) { iNext++; } else { next = weight[iNext];
             * testNext = false; } } }
             *
             * // Take average weight[ii] = (last + next) / 2.0D; }
             */
        }
        return flag;
    }

    // sort elements x, y and w arrays of doubles into ascending order of the x array
    // using selection sort method
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
