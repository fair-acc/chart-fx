package de.gsi.math.matrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.math.TRandom;

/**
 * computes IN = U*S*V^T. With diag(S)={fEigenValues(0), fEigenValues(1),..,fEigenValues(n)} and V the fEigenVector
 * matrix of IN
 * <p>
 * algorithm according to Golub and Reinsch G. Golub and C. Reinsch, "Handbook for Automatic Computation II, Linear
 * Algebra". Springer, NY, 1971. numerically checked but... TODO: code clean up necessary... code as been translated
 * from FORTRAN -&gt; C -&gt; C++ -&gt; Java ;-)
 *
 * @author rstein
 */
@SuppressWarnings("PMD") // -- needs to be worked upon
public class SingularValueDecomposition {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingularValueDecomposition.class);
    private static final int MAX_SVD_CONVERSION_LIMIT = 30;
    private static final double TEST_SVD_THRESHOLD = 1e-10;
    private static final double TEST_INVERT_THRESHOLD = 1e-6;
    private boolean fInit;
    private boolean fInitSVD;
    private MatrixD finputMatrix; // the input matrix (m x n)
    private MatrixD feigenVectorsU; // the eigenvector matrix U (m x n)
    private MatrixD feigenVectorsV; // the eigenvector matrix V (n x n)
    private MatrixD fEigenValues; // the diagonal eigenvalue matrix of input
                                  // matrix (n x n)
    private MatrixD fInverseEigenValues; // pseuodo-inverse diagonal eigenvalue
                                         // matrix of input matrix (n x n)

    private double fCut;

    /**
     * default constructor.
     */
    public SingularValueDecomposition() {
        fInit = false;
        fInitSVD = false;
        fCut = 1e-20;
    }

    /**
     * default constructor.
     *
     * @param inputMatrix the preset input matrix to be decomposed
     */
    public SingularValueDecomposition(final MatrixD inputMatrix) {
        final int m = inputMatrix.getRowDimension();
        final int n = inputMatrix.getColumnDimension();
        fInitSVD = false;
        fCut = 1e-20;
        finputMatrix = new MatrixD(m, n);
        feigenVectorsU = new MatrixD(m, n);
        fEigenValues = new MatrixD(n, n);
        fInverseEigenValues = new MatrixD(n, n);
        feigenVectorsV = new MatrixD(n, n);
        finputMatrix = inputMatrix.copy();
        fInit = true;
        fInitSVD = false;
    }

    /**
     * Two norm condition number
     *
     * @return max(S)/min(S)
     */
    public double cond() {
        if (!fInitSVD) {
            decompose();
        }
        final double firstEigenValue = fEigenValues.get(0, 0);
        final int m = getEigenVectorMatrixU().getRowDimension();
        final int n = getEigenVectorMatrixU().getColumnDimension();
        final int index = Math.min(m, n) - 1;
        final double lastEigenValue = fEigenValues.get(index, index);
        return firstEigenValue / lastEigenValue;
    }

    /**
     * Perform the singular value decomposition. (does not use intermediate square matrices)
     *
     * @return true if operation was successful, false otherwise
     */
    public boolean decompose() {
        return decompose(false);
    }

    /**
     * Perform the singular value decomposition.
     *
     * @param useSquareMatrix whether to use intermediate step of transforming the input matrix to a square one.
     * @return true if operation was successful, false otherwise
     */
    public boolean decompose(final boolean useSquareMatrix) {
        if (!fInit) {
            LOGGER.error("no matrix specified");
            return false;
        }
        int m = finputMatrix.getRowDimension();
        int n = finputMatrix.getColumnDimension();
        if (m == 0 || n == 0) {
            LOGGER.error(String.format("null matrix specified (%d,%d)", m, n));
            return false;
        }
        double[] a;
        final double[] eigenValues = new double[n];
        final double[] eigenVectorMatrixV = new double[n * n];

        if (useSquareMatrix) {
            // final MatrixD square =
            // finputMatrix.times(finputMatrix.transpose());
            final MatrixD square = finputMatrix.transpose().times(finputMatrix);
            n = square.getRowDimension();
            m = n;

            LOGGER.debug(String.format("reduced to %dx%d matrix", n, n));
            a = new double[n * n];

            // A = Rt*R
            // MatrixD A(fIN, MatrixD::kTransposeMult, fIN);
            // ffIN=A.GetMatrixArray();
            // for (int i=0; i<n*n; i++) a[i]=ffIN[i];
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    a[i * n + j] = square.get(i, j);
                }
            }

        } else {
            a = new double[m * n];
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    a[i * n + j] = finputMatrix.get(i, j);
                }
            }
        }

        // for (int i=0; i<n; i++)
        // for (int j=0; j<n; j++)
        // gsl_matrix_set(A, i, j, a[i*n+j]);

        // gsl_linalg_SV_decomp (A,V,S,work);
        svdcmp(a, m, n, eigenValues, eigenVectorMatrixV);

        // sorting of Eigenvalues and Eigenvectors
        if (useSquareMatrix) {
            sortEigenValues(eigenValues, eigenVectorMatrixV);
        } else {
            sortEigenValues(a, eigenValues, eigenVectorMatrixV, m);
        }

        fInverseEigenValues.times(0.0);
        fEigenValues.times(0.0);
        if (useSquareMatrix) {
            for (int i = 0; i < n; i++) {
                final double eigenValue = Math.sqrt(eigenValues[i]);
                fEigenValues.set(i, i, eigenValue);
                if (eigenValue == 0) {
                    fInverseEigenValues.set(i, i, 0.0);
                } else {
                    fInverseEigenValues.set(i, i, 1 / eigenValue);
                }
            }
        } else {
            for (int i = 0; i < n; i++) {
                final double eigenValue = eigenValues[i];
                fEigenValues.set(i, i, eigenValue);
                if (eigenValue == 0) {
                    fInverseEigenValues.set(i, i, 0.0);
                } else {
                    fInverseEigenValues.set(i, i, 1 / eigenValue);
                }
            }
        }

        // standard equation O(n^4);
        // fU = fIN * fEigenVectors * Lambda_minus1;
        // Lambda_minus1(,) is diagonal, hence fEigenVectors*Lambda_minus1 can
        // be computed in O(n^2)

        if (useSquareMatrix) {
            final MatrixD EigenTimesLambda_minus1 = new MatrixD(n, n);
            for (int i = 0; i < n; i++) {
                final int row_index = i * n;
                for (int j = 0; j < n; j++) {
                    feigenVectorsV.set(i, j, eigenVectorMatrixV[row_index + j]);
                    final double eigenValue = fInverseEigenValues.get(j, j);
                    EigenTimesLambda_minus1.set(i, j, eigenValue * eigenVectorMatrixV[row_index + j]);
                }
            }

            feigenVectorsU = finputMatrix.times(EigenTimesLambda_minus1);
        } else {
            final MatrixD EigenTimesLambda_minus1 = new MatrixD(n, n);

            for (int i = 0; i < n; i++) {
                final int row_index = i * n;
                for (int j = 0; j < n; j++) {
                    feigenVectorsV.set(i, j, eigenVectorMatrixV[row_index + j]);
                    final double eigenValue = fInverseEigenValues.get(j, j);
                    EigenTimesLambda_minus1.set(i, j, eigenValue * eigenVectorMatrixV[row_index + j]);
                }
            }

            feigenVectorsU = finputMatrix.times(EigenTimesLambda_minus1);
        }

        fInitSVD = true;
        return true;
    }

    public MatrixD getEigenSolution(final int eigen) {
        if (!fInitSVD) {
            decompose();
        }
        final int n = feigenVectorsU.getRowDimension();
        final MatrixD ret = new MatrixD(n, 1);
        for (int i = 0; i < n; i++) {
            ret.set(i, 0, feigenVectorsU.get(i, eigen));
        }
        return ret;
    }

    public MatrixD getEigenValues() {
        if (!fInitSVD) {
            decompose();
        }
        return fEigenValues;
    }

    public MatrixD getEigenVector(final int eigen) {
        if (!fInitSVD) {
            decompose();
        }
        final int n = feigenVectorsV.getRowDimension();
        final MatrixD ret = new MatrixD(n, 1);
        for (int i = 0; i < n; i++) {
            ret.set(i, 0, feigenVectorsV.get(i, eigen));
        }

        return ret;
    }

    /**
     * @return the eigenvector matrix U (m x n)
     */
    public MatrixD getEigenVectorMatrixU() {
        if (!fInitSVD) {
            decompose();
        }
        return feigenVectorsU;
    }

    /**
     * @return the eigenvector matrix V (n x n)
     */
    public MatrixD getEigenVectorMatrixV() {
        if (!fInitSVD) {
            decompose();
        }
        return feigenVectorsV;
    }

    public MatrixD getInverse() {
        if (!fInitSVD) {
            decompose();
        }
        return getInverse(false, -1);
    }

    public MatrixD getInverse(final boolean timer, final int nEigenValues) {
        final int m = feigenVectorsU.getRowDimension();
        final int n = feigenVectorsU.getColumnDimension();
        int nEigen = nEigenValues;

        if (nEigen < 0) {
            nEigen = fEigenValues.getRowDimension() - 1;
        }

        if (nEigen < 0 || nEigen >= n) {
            LOGGER.warn(String.format("selected number of eigenvalues %d exceeds maximum %d . et to max", nEigen, n));
            nEigen = n;
        }

        if (!fInitSVD) {
            LOGGER.debug(String.format("forced decomposition of %dx%d matrix", m, n));
            decompose(false);
        }

        LOGGER.debug(String.format("cut below %e and max %d eigenvalues", fCut, nEigen));

        final MatrixD lambdaMinus1 = new MatrixD(n, n); // inverse eigenvector
                                                        // matrix w.r.t. R

        final double eigenValueMax = fEigenValues.get(0, 0);
        for (int i = 0; i < n; i++) {
            final double eigenValue = fEigenValues.get(i, i);
            if (eigenValue / eigenValueMax > fCut && i <= nEigen) {
                lambdaMinus1.set(i, i, fInverseEigenValues.get(i, i));
            } else {
                LOGGER.debug(String.format("discarding from eigenvalue %d", i + 1));
                break;
            }
        }

        // MatrixD UR= IN*EigenVectors*Lambda_minus1; // UR is the U Matrix w.r.
        // to R
        // MatrixD URt(MatrixD::kTransposed,UR);

        // old:
        // fU= fIN*fEigenVectors*Lambda_minus1;
        // feigenVectorsU =
        // finputMatrix.times(feigenVectorsV.times(lambdaMinus1));
        final MatrixD eigenVectorsUt = feigenVectorsU.transpose();
        final MatrixD iRESPONSE = feigenVectorsV.times(lambdaMinus1.times(eigenVectorsUt));

        return iRESPONSE;
    }

    public MatrixD getMatrix() {
        if (!fInitSVD) {
            decompose();
        }
        return feigenVectorsU.times(fEigenValues.times(feigenVectorsV.transpose()));
    }

    public MatrixD getPseudoInverseEigenvalues() {
        if (!fInitSVD) {
            decompose();
        }
        return fInverseEigenValues.copy();
    }

    /***************************************************************************************************/
    /******************
     * the holy cow: the svd algorithm
     ************************************************/
    /***************************************************************************************************/

    public double[] getSingularValues() {
        if (!fInitSVD) {
            decompose();
        }
        final int n = fEigenValues.getColumnDimension();
        final double[] retVal = new double[n];
        for (int i = 0; i < n; i++) {
            retVal[i] = fEigenValues.get(i, i);
        }
        return retVal;
    }

    public double getThreshold() {
        return fCut;
    }

    public double getTol() {
        return getThreshold();
    }

    /**
     * @return the eigenvector matrix U (n x n)
     */
    public MatrixD getU() {
        return getEigenVectorMatrixU();
    }

    /**
     * @return the eigenvector matrix V (n x n)
     */
    public MatrixD getV() {
        return getEigenVectorMatrixV();
    }

    private double mySIGN(final double a, final double b) {
        return b >= 0.0 ? Math.abs(a) : -Math.abs(a);
    }

    /***************************************************************************************************/
    /******************
     * the holy cow: the svd algorithm ***** end
     **************************************/
    /***************************************************************************************************/

    /**
     * Two norm
     *
     * @return max(S)
     * @deprecated used only in old implementation
     */
    @Deprecated
    public double norm2() {
        if (!fInitSVD) {
            decompose();
        }
        return fEigenValues.get(0, 0);
    }

    /**
     * @param a input a
     * @param b input b
     * @return numerically precise implementation of pythagoras (vs. 'sqrt(a*a + b*b)')
     */
    private double pythagoras(final double a, final double b) {
        final double absa = Math.abs(a);
        final double absb = Math.abs(b);

        if (absa > absb) {
            return absa * Math.sqrt(1.0 + square(absb / absa));
        } else {
            return absb == 0.0 ? 0.0 : absb * Math.sqrt(1.0 + square(absa / absb));
        }
    }

    /**
     * Effective numerical matrix rank
     *
     * @return Number of non-negligible singular values.
     */
    public int rank() {
        if (!fInitSVD) {
            decompose();
        }
        int rank = 0;
        final double eps = Math.pow(2.0, -52.0);
        final int m = getEigenVectorMatrixU().getRowDimension();
        final int n = getEigenVectorMatrixU().getColumnDimension();
        final double firstEigenValue = fEigenValues.get(0, 0);
        final double tol = Math.max(m, n) * firstEigenValue * eps;
        for (int i = 0; i < fEigenValues.getColumnDimension(); i++) {
            final double eigenValue = fEigenValues.get(i, i);
            if (eigenValue > tol) {
                rank++;
            }
        }

        return rank;
    }

    /**
     * Sets the input matrix to be decomposed.
     *
     * @param inputMatrix the input matrix
     */
    public void setMatrix(final MatrixD inputMatrix) {
        final int m = inputMatrix.getRowDimension();
        final int n = inputMatrix.getColumnDimension();
        fInitSVD = false;
        fCut = 1e-20;
        finputMatrix = new MatrixD(m, n);
        feigenVectorsU = new MatrixD(m, n);
        fEigenValues = new MatrixD(n, n);
        fInverseEigenValues = new MatrixD(n, n);
        feigenVectorsV = new MatrixD(n, n);
        finputMatrix = inputMatrix.copy();
        fInit = true;
        fInitSVD = false;
    }

    public void setThreshold(final double val) {
        fCut = val;
    }

    public void setTol(final double val) {
        setThreshold(val);
    }

    /**
     * 
     * @param eigenValues vector containing eigen values
     * @param eigenVectorMatrixV vector containing the diagonal matrix elements of eigen vector matrix
     */
    private void sortEigenValues(final double[] eigenValues, final double[] eigenVectorMatrixV) {

        // Given the eigenvalues d[1..n] and eigenvectors v[1..n][1..n]
        // this routine sorts (straight insertion) the eigenvalues into
        // descending order, and rearranges
        // the columns of v correspondingly.
        final int n = eigenValues.length;

        for (int i = 0; i < n - 1; i++) {
            int k = i;
            double p = eigenValues[k];
            for (int j = i + 1; j < n; j++) {
                if (eigenValues[j] >= p) {
                    k = j;
                    p = eigenValues[k];
                }
            }
            if (k != i) {
                eigenValues[k] = eigenValues[i];
                eigenValues[i] = p;
                for (int j = 0; j < n; j++) {
                    p = eigenVectorMatrixV[j * n + i];
                    eigenVectorMatrixV[j * n + i] = eigenVectorMatrixV[j * n + k];
                    eigenVectorMatrixV[j * n + k] = p;
                }
            }
        }
    }

    private void sortEigenValues(final double[] eigenVectorU, final double[] eigenValues,
            final double[] eigenVectorMatrixV, final int m) {
        // Given the eigenvalues d[1..n] and eigenvectors v[1..n][1..n]
        // this routine sorts (straight insertion) the eigenvalues into
        // descending order, and rearranges
        // the columns resp. rows of v and u correspondingly.
        final int n = eigenValues.length;

        for (int i = 0; i < n - 1; i++) {
            int k = i;
            double p = eigenValues[k];
            for (int j = i + 1; j < n; j++) {
                if (eigenValues[j] >= p) {
                    k = j;
                    p = eigenValues[k];
                }
            }
            if (k != i) {
                // switch indices i <. k
                eigenValues[k] = eigenValues[i];
                eigenValues[i] = p;
                for (int j = 0; j < n; j++) {
                    p = eigenVectorMatrixV[j * n + i];
                    eigenVectorMatrixV[j * n + i] = eigenVectorMatrixV[j * n + k];
                    eigenVectorMatrixV[j * n + k] = p;
                }
                for (int j = 0; j < m; j++) {
                    p = eigenVectorU[j * n + i];
                    eigenVectorU[j * n + i] = eigenVectorU[j * n + k];
                    eigenVectorU[j * n + k] = p;
                }
            }
        }
    }

    private double square(final double a) {
        return a * a;
    }

    private void svdcmp(final double[] inputMatrix, final int m, final int n, final double[] eigenValues,
            final double[] eigenVectorMatrixV) {
        // Given a matrix a[1..m][1..n], this routine computes its singular
        // value decomposition,
        // inputMatrix =U * S *Vt
        // The matrix U replaces inputMatrix on output.
        // The diagonal matrix of singular values S is out-put as a vector
        // w[1..n]
        // The matrix V (not the transpose Vt) is output as v[1..n][1..n]
        int l = 0; // semi-global variable
        int nm = 0; // semi-global variable
        double anorm = 0.0; // semi-global variable
        double g = 0.0; // semi-global variable
        double scale = 0.0; // semi-global variable
        final double[] rv = new double[n]; // semi-global variable

        // Householder reduction to bidiagonal form.
        for (int i = 0; i < n; i++) {
            l = i + 1;
            rv[i] = scale * g;
            g = 0.0;
            double s = 0.0;
            scale = 0.0;
            if (i < m) {
                for (int k = i; k < m; k++) {
                    scale += Math.abs(inputMatrix[k * n + i]);
                }
                if (scale != 0.0) {
                    for (int k = i; k < m; k++) {
                        inputMatrix[k * n + i] /= scale;
                        s += inputMatrix[k * n + i] * inputMatrix[k * n + i];
                    }

                    double f = inputMatrix[i * n + i];
                    g = -mySIGN(Math.sqrt(s), f);

                    final double h = f * g - s;
                    inputMatrix[i * n + i] = f - g;
                    for (int j = l; j < n; j++) {
                        s = 0.0;
                        for (int k = i; k < m; k++) {
                            s += inputMatrix[k * n + i] * inputMatrix[k * n + j];
                        }
                        f = s / h;
                        for (int k = i; k < m; k++) {
                            inputMatrix[k * n + j] += f * inputMatrix[k * n + i];
                        }
                    }
                    for (int k = i; k < m; k++) {
                        inputMatrix[k * n + i] *= scale;
                    }
                }
            } // end of: if (i < m)...

            eigenValues[i] = scale * g;
            g = 0.0;
            s = 0.0;
            scale = 0.0;
            if (i < m && i != n - 1) {
                for (int k = l; k < n; k++) {
                    scale += Math.abs(inputMatrix[i * n + k]);
                }
                if (scale != 0.0) {
                    for (int k = l; k < n; k++) {
                        inputMatrix[i * n + k] /= scale;
                        s += inputMatrix[i * n + k] * inputMatrix[i * n + k];
                    }
                    final double f = inputMatrix[i * n + l];
                    g = -mySIGN(Math.sqrt(s), f);
                    final double h = f * g - s;
                    inputMatrix[i * n + l] = f - g;
                    for (int k = l; k < n; k++) {
                        rv[k] = inputMatrix[i * n + k] / h;
                    }
                    for (int j = l; j < m; j++) {
                        s = 0.0;
                        for (int k = l; k < n; k++) {
                            s += inputMatrix[j * n + k] * inputMatrix[i * n + k];
                        }
                        for (int k = l; k < n; k++) {
                            inputMatrix[j * n + k] += s * rv[k];
                        }
                    }
                    for (int k = l; k < n; k++) {
                        inputMatrix[i * n + k] *= scale;
                    }
                }
            }
            anorm = Math.max(anorm, Math.abs(eigenValues[i]) + Math.abs(rv[i]));
        } // end of: for (i=0;i<n;i++).... Householdertransform.....

        // Accumulation of right-hand transformations.
        for (int i = n - 1; i >= 0; i--) {
            if (i < n - 1) {
                if (g != 0.0) {
                    // Double division to avoid possible underflow.
                    for (int j = l; j < n; j++) {
                        eigenVectorMatrixV[j * n + i] = inputMatrix[i * n + j] / inputMatrix[i * n + l] / g;
                    }
                    for (int j = l; j < n; j++) {
                        double s = 0.0;
                        for (int k = l; k < n; k++) {
                            s += inputMatrix[i * n + k] * eigenVectorMatrixV[k * n + j];
                        }
                        for (int k = l; k < n; k++) {
                            eigenVectorMatrixV[k * n + j] += s * eigenVectorMatrixV[k * n + i];
                        }
                    }
                }

                for (int j = l; j < n; j++) {
                    eigenVectorMatrixV[i * n + j] = 0.0;
                    eigenVectorMatrixV[j * n + i] = 0.0;
                }
            }
            eigenVectorMatrixV[i * n + i] = 1.0;
            g = rv[i];
            l = i;
        }

        for (int i = Math.min(m - 1, n - 1); i >= 0; i--) {
            // Accumulation of left-hand transformations.
            l = i + 1;
            g = eigenValues[i];
            for (int j = l; j < n; j++) {
                inputMatrix[i * n + j] = 0.0;
            }
            if (g == 0.0) {
                for (int j = i; j < m; j++) {
                    inputMatrix[j * n + i] = 0.0;
                }
            } else {
                g = 1.0 / g;
                for (int j = l; j < n; j++) {
                    double s = 0.0;
                    for (int k = l; k < m; k++) {
                        s += inputMatrix[k * n + i] * inputMatrix[k * n + j];
                    }
                    final double f = s / inputMatrix[i * n + i] * g;
                    for (int k = i; k < m; k++) {
                        inputMatrix[k * n + j] += f * inputMatrix[k * n + i];
                    }
                }

                for (int j = i; j < m; j++) {
                    inputMatrix[j * n + i] *= g;
                }
            }
            ++inputMatrix[i * n + i];
        }

        for (int k = n - 1; k >= 0; k--) {
            // Diagonalization of the bidiagonal form: Loop over singular
            // values, and over allowed iterations.
            for (int its = 1; its <= MAX_SVD_CONVERSION_LIMIT; its++) {
                boolean flag = true;
                for (l = k; l > 0; l--) {
                    // Test for splitting.
                    nm = l - 1;
                    // Note that rv[1] is always zero.
                    if (Math.abs(rv[l]) + anorm == anorm) {
                        flag = false;
                        break;
                    }

                    if (Math.abs(eigenValues[nm]) + anorm == anorm) {
                        break;
                    }
                }

                if (flag) {
                    double c = 0.0;
                    // Cancellation of rv[l], if l > 1.
                    double s = 1.0;
                    for (int i = l; i <= k; i++) {
                        final double f = s * rv[i];
                        rv[i] = c * rv[i];
                        if (Math.abs(f) + anorm == anorm) {
                            break;
                        }
                        g = eigenValues[i];
                        double h = pythagoras(f, g);

                        eigenValues[i] = h;
                        h = 1.0 / h;
                        c = g * h;
                        s = -f * h;
                        for (int j = 0; j < m; j++) {
                            final int row = j * n;
                            final double y = inputMatrix[row + nm];
                            final double z = inputMatrix[row + i];
                            inputMatrix[row + nm] = y * c + z * s;
                            inputMatrix[row + i] = z * c - y * s;
                        }
                    }
                }
                double z = eigenValues[k];

                if (l == k) {
                    // Convergence.
                    if (z < 0.0) {
                        // Singular value is made nonnegative.
                        eigenValues[k] = -z;
                        for (int j = 0; j < n; j++) {
                            eigenVectorMatrixV[j * n + k] = -eigenVectorMatrixV[j * n + k];
                        }
                    }
                    break;
                }
                if (its == MAX_SVD_CONVERSION_LIMIT) {
                    LOGGER.warn(String.format("no convergence in %d svdcmp iterations", MAX_SVD_CONVERSION_LIMIT));
                    break;
                }
                double x = eigenValues[l];
                // Shift from bottom 2-by-2 minor.
                nm = k - 1;
                double y = eigenValues[nm];
                g = rv[nm];
                double h = rv[k];
                double f = ((y - z) * (y + z) + (g - h) * (g + h)) / (2.0 * h * y);
                g = pythagoras(f, 1.0);

                f = ((x - z) * (x + z) + h * (y / (f + mySIGN(g, f)) - h)) / x;
                double c = 1.0;
                double s = 1.0;

                // Next QR transformation:
                for (int j = l; j <= nm; j++) {
                    final int i = j + 1;
                    g = rv[i];
                    y = eigenValues[i];
                    h = s * g;
                    g = c * g;
                    z = pythagoras(f, h);

                    rv[j] = z;
                    c = f / z;
                    s = h / z;
                    f = x * c + g * s;
                    g = g * c - x * s;
                    h = y * s;
                    y *= c;
                    for (int jj = 0; jj < n; jj++) {
                        final int row = jj * n;
                        x = eigenVectorMatrixV[row + j];
                        z = eigenVectorMatrixV[row + i];
                        eigenVectorMatrixV[row + j] = x * c + z * s;
                        eigenVectorMatrixV[row + i] = z * c - x * s;
                    }
                    z = pythagoras(f, h);

                    eigenValues[j] = z;
                    if (z != 0.0) {
                        z = 1.0 / z;
                        c = f * z;
                        s = h * z;
                    }
                    f = c * g + s * y;
                    x = c * y - s * g;
                    for (int jj = 0; jj < m; jj++) {
                        final int row = jj * n;
                        y = inputMatrix[row + j];
                        z = inputMatrix[row + i];
                        inputMatrix[row + j] = y * c + z * s;
                        inputMatrix[row + i] = z * c - y * s;
                    }
                }

                rv[l] = 0.0;
                rv[k] = f;
                eigenValues[k] = x;
            }
        }

    }

    /**
     * Test of 'inputMatrix'*'pseudo-inverse SVD matrix' == 1. only works for non-singular matrices
     *
     * @see #testInvert(double threshold) for more info
     * @return true if test successful, false otherwise
     */
    // @Test
    public boolean testInvert() {
        return testInvert(TEST_INVERT_THRESHOLD);
    }

    /**
     * Test of 'inputMatrix'*'pseudo-inverse SVD matrix' == '1' matrix. only works for non-singular matrices
     *
     * @param threshold the numerical threshold for the test
     * @return true if test successful, false otherwise
     */
    // @Test
    public boolean testInvert(final double threshold) {
        final double stol = getTol();
        setTol(0.0);
        final MatrixD pseudoInverse = this.getInverse();
        // final MatrixD inputSVD = this.getMatrix();
        final MatrixD inputRAW = finputMatrix;
        final MatrixD testMatrix = pseudoInverse.times(inputRAW);

        setTol(stol);

        LOGGER.debug(String.format("dimension of test matrix %dx%d", testMatrix.getRowDimension(),
                testMatrix.getColumnDimension()));
        final int nEigenMax = fEigenValues.getRowDimension() - 1;
        LOGGER.debug(String.format("max/min eigenvalue ratio: %+e",
                fEigenValues.get(0, 0) / fEigenValues.get(nEigenMax, nEigenMax)));

        for (int i = 0; i < testMatrix.getRowDimension(); i++) {
            for (int j = 0; j < testMatrix.getColumnDimension(); j++) {
                if (i == j) {
                    testMatrix.set(i, j, testMatrix.get(i, j) - 1);
                }
                if (Math.abs(testMatrix.get(i, j)) > threshold) {
                    LOGGER.warn("TestInvert() - found that element (%d,%d) differs %e from zero!", i, j,
                            testMatrix.get(i, j));
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Tests whether 'inputMatrix' == 'SVD decomposed matrix'. assumes default numerical precision threshold,
     *
     * @see #testSVD(double threshold) for more info
     * @return true if test successful, false otherwise
     */
    // @Test
    public boolean testSVD() {
        return testSVD(TEST_SVD_THRESHOLD);
    }

    /**
     * Tests whether 'inputMatrix' == 'SVD decomposed matrix'.
     *
     * @param threshold the numerical threshold for the test
     * @return true if test successful, false otherwise
     */
    // @Test
    public boolean testSVD(final double threshold) {
        final MatrixD testMatrix = finputMatrix.minus(getMatrix());

        for (int i = 0; i < testMatrix.getRowDimension(); i++) {
            for (int j = 0; j < testMatrix.getColumnDimension(); j++) {
                if (Math.abs(testMatrix.get(i, j)) > threshold) {
                    LOGGER.warn(String.format("found that element (%d,%d) differs %e from zero!", i, j,
                            testMatrix.get(i, j)));
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * some small test routines to check SVD matrix computation.
     *
     * @param argc the input parameter (unused)
     */
    public static void main(final String[] argc) {
        final TRandom rnd = new TRandom(1);
        final MatrixD input = new MatrixD(200, 100);
        for (int i = 0; i < input.getRowDimension(); i++) {
            for (int j = 0; j < input.getColumnDimension(); j++) {
                final double value = rnd.Integer(10);
                input.set(i, j, value);
            }
        }
        final SingularValueDecomposition svd = new SingularValueDecomposition(input);
        MatrixD eigenvalues1 = null;
        MatrixD eigenvalues2 = null;
        final boolean useIntermediateSquareMatrix = true;
        if (svd.decompose(useIntermediateSquareMatrix)) {
            LOGGER.info("decompose() - square intermediate matrix - successful");
            eigenvalues1 = svd.getEigenValues();
        } else {
            LOGGER.info("decompose() - failed");
        }

        if (svd.decompose(false)) {
            LOGGER.info("decompose() - successful");
            eigenvalues2 = svd.getEigenValues();
        } else {
            LOGGER.info("decompose() - failed");
        }

        if (eigenvalues1 != null && eigenvalues2 != null) {
            for (int i = 0; i < eigenvalues1.getRowDimension(); i++) {
                LOGGER.info("eigenvalue(%2d) = %+e vs %+e   diff = %+e", i, eigenvalues1.get(i, i),
                        eigenvalues2.get(i, i), eigenvalues1.get(i, i) - eigenvalues2.get(i, i));
            }
        } else if (eigenvalues1 != null) {
            for (int i = 0; i < eigenvalues1.getRowDimension(); i++) {
                LOGGER.info("eigenvalue(%2d) = %+e", i, eigenvalues1.get(i, i));
            }
        } else if (eigenvalues2 != null) {
            for (int i = 0; i < eigenvalues2.getRowDimension(); i++) {
                LOGGER.info("eigenvalue(%2d) = %+e", i, eigenvalues2.get(i, i));
            }
        }

        LOGGER.info("norm2 = %lf", svd.norm2());
        LOGGER.info("cond = %lf", svd.cond());
        LOGGER.info("rank = %lf", svd.rank());

        LOGGER.info("check - testSVD()");
        if (svd.testSVD()) {
            LOGGER.info("testSVD() - passed");

        } else {
            LOGGER.warn("testSVD() - failed");
        }

        LOGGER.info("check - testInvert()");
        if (svd.testInvert()) {
            LOGGER.info("testInvert() - passed");

        } else {
            LOGGER.warn("testInvert() - failed");

            if (input.getRowDimension() < 20 && input.getColumnDimension() < 20) {
                final MatrixD matS = svd.getEigenValues();
                final MatrixD matSm = svd.getPseudoInverseEigenvalues();
                final MatrixD matU = svd.getEigenVectorMatrixU();
                final MatrixD matV = svd.getEigenVectorMatrixV();

                LOGGER.info("eigenvalues x pseudo-inverse eigenvalue matrix =");
                matS.times(matSm).print(4, 3);

                LOGGER.info("V x Vt =");
                matV.times(matV.transpose()).print(4, 2);

                LOGGER.info("U x Ut =");
                matU.times(matU.transpose()).print(4, 2);

                LOGGER.info("Ut x U =");
                matU.transpose().times(matU).print(4, 2);
            }
        }
    }

}
