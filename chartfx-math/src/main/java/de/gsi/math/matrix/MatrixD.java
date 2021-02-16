package de.gsi.math.matrix;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import de.gsi.math.MathBase;
import de.gsi.math.functions.Function1D;

/**
 * Jama = Java Matrix class.
 * <P>
 * The Java Matrix Class provides the fundamental operations of numerical linear algebra. Various constructors create
 * Matrices from two dimensional arrays of double precision floating point numbers. Various "gets" and "sets" provide
 * access to sub-matrices and matrix elements. Several methods implement basic matrix arithmetic, including matrix
 * addition and multiplication, matrix norms, and element-by-element array operations. Methods for reading and printing
 * matrices are also included. All the operations in this version of the Matrix Class involve real matrices. Complex
 * matrices may be handled in a future version.
 * <P>
 * Five fundamental matrix decompositions, which consist of pairs or triples of matrices, permutation vectors, and the
 * like, produce results in five decomposition classes. These decompositions are accessed by the Matrix class to compute
 * solutions of simultaneous linear equations, determinants, inverses and other matrix functions. The five
 * decompositions are:
 * 
 * <UL>
 * <LI>Cholesky Decomposition of symmetric, positive definite matrices.
 * <LI>LU Decomposition of rectangular matrices.
 * <LI>QR Decomposition of rectangular matrices.
 * <LI>Singular Value Decomposition of rectangular matrices.
 * <LI>Eigenvalue Decomposition of both symmetric and nonsymmetric square matrices.
 * </UL>
 * <DL>
 * <DT><B>Example of use:</B></DT>
 * 
 * <DD>Solve a linear system A x = b and compute the residual norm, ||b - A x||.
 * 
 *
 * <PRE>
 * 
 * double[][] vals = { { 1., 2., 3 }, { 4., 5., 6. }, { 7., 8., 10. } };
 * Matrix A = new Matrix(vals);
 * Matrix b = Matrix.random(3, 1);
 * Matrix x = A.solve(b);
 * Matrix r = A.times(x).minus(b);
 * double rnorm = r.normInf();
 * </PRE>
 *
 * </DD>
 * </DL>
 *
 * @author The MathWorks, Inc. and the National Institute of Standards and Technology.
 * @version 5 August 1998
 */

public class MatrixD extends AbstractMatrix {
    private static final long serialVersionUID = 491870425070247870L;
    private final double[][] element; // internal array storage

    /*
     * ------------------------ Constructors ------------------------
     */

    /**
     * Construct a matrix from a one-dimensional packed array
     *
     * @param vals One-dimensional array of doubles
     * @param m Number of rows.
     * @exception IllegalArgumentException Array length must be a multiple of m.
     */
    public MatrixD(final double[] vals, final int m) {
        this(vals, m, true);
    }

    /**
     * Construct a matrix from a one-dimensional packed array
     *
     * @param vals One-dimensional array of doubles
     * @param m Number of rows.
     * @param rowMajor true: data is stored row-wise (C/C++), false: data is stored column-wise (Fortran)
     * @exception IllegalArgumentException Array length must be a multiple of m.
     */
    public MatrixD(final double[] vals, final int m, final boolean rowMajor) {
        super.m = m;
        super.n = m != 0 ? vals.length / m : 0;
        if (m * n != vals.length) {
            throw new IllegalArgumentException("Array length must be a multiple of m.");
        }
        element = new double[m][n];
        if (rowMajor) {
            for (int i = 0; i < m; i++) {
                System.arraycopy(vals, i * n, element[i], 0, n);
            }
        } else {
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    element[i][j] = vals[i + j * m];
                }
            }
        }
    }

    /**
     * Construct a matrix from a 2-D array.
     *
     * @param A Two-dimensional array of doubles.
     * @exception IllegalArgumentException All rows must have the same length
     */

    public MatrixD(final double[][] A) {
        super.m = A.length;
        super.n = A[0].length;
        for (int i = 0; i < m; i++) {
            if (A[i].length != n) {
                throw new IllegalArgumentException("All rows must have the same length.");
            }
        }
        element = A;
    }

    /**
     * Construct a matrix quickly without checking arguments.
     *
     * @param A Two-dimensional array of doubles.
     * @param m Number of rows.
     * @param n Number of columns.
     */
    public MatrixD(final double[][] A, final int m, final int n) {
        element = A;
        this.m = m;
        this.n = n;
    }

    /**
     * Construct an m-by-n matrix of zeros.
     *
     * @param m Number of rows.
     * @param n Number of columns.
     */
    public MatrixD(final int m, final int n) {
        super.m = m;
        super.n = n;
        element = new double[m][n];
    }

    /**
     * Construct an m-by-n constant matrix.
     *
     * @param m Number of rows.
     * @param n Number of columns.
     * @param s Fill the matrix with this scalar value.
     */
    public MatrixD(final int m, final int n, final double s) {
        super.m = m;
        super.n = n;
        element = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                element[i][j] = s;
            }
        }
    }

    /*
     * ------------------------ Public Methods ------------------------
     */

    /**
     * apply user specified function to each matrix element
     * 
     * @param func user-supplied function
     */
    public void apply1DFunction(final Function1D func) {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                element[i][j] = func.getValue(element[i][j]);
            }
        }
    }

    /**
     * Element-by-element left division, C = A.\B
     *
     * @param B another matrix
     * @return A.\B
     */
    public MatrixD arrayLeftDivide(final MatrixD B) {
        checkMatrixDimensions(B);
        final MatrixD X = new MatrixD(m, n);
        final double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = B.element[i][j] / element[i][j];
            }
        }
        return X;
    }

    /**
     * Element-by-element left division in place, A = A.\B
     *
     * @param B another matrix
     * @return A.\B
     */

    public MatrixD arrayLeftDivideEquals(final MatrixD B) {
        checkMatrixDimensions(B);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                element[i][j] = B.element[i][j] / element[i][j];
            }
        }
        return this;
    }

    /**
     * Element-by-element right division, C = A./B
     *
     * @param B another matrix
     * @return A./B
     */
    public MatrixD arrayRightDivide(final MatrixD B) {
        checkMatrixDimensions(B);
        final MatrixD X = new MatrixD(m, n);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                X.set(i, j, get(i, j) / B.get(i, j));
            }
        }
        return X;
    }

    /**
     * Element-by-element right division in place, A = A./B
     *
     * @param B another matrix
     * @return A./B
     */
    public MatrixD arrayRightDivideEquals(final MatrixD B) {
        checkMatrixDimensions(B);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                element[i][j] = element[i][j] / B.element[i][j];
            }
        }
        return this;
    }

    /**
     * Element-by-element multiplication, C = A.*B
     *
     * @param B another matrix
     * @return A.*B
     */

    public MatrixD arrayTimes(final MatrixD B) {
        checkMatrixDimensions(B);
        final MatrixD X = new MatrixD(m, n);
        final double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = element[i][j] * B.element[i][j];
            }
        }
        return X;
    }

    /**
     * Element-by-element multiplication in place, A = A.*B
     *
     * @param B another matrix
     * @return A.*B
     */

    public MatrixD arrayTimesEquals(final MatrixD B) {
        checkMatrixDimensions(B);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                set(i, j, get(i, j) * B.get(i, j));
            }
        }
        return this;
    }

    /**
     * Cholesky Decomposition
     *
     * @return CholeskyDecomposition
     * @see CholeskyDecomposition
     */

    public CholeskyDecomposition chol() {
        return new CholeskyDecomposition(this);
    }

    /**
     * Clone the Matrix object.
     */

    @Override
    public Object clone() {
        return copy();
    }

    /**
     * Matrix condition (2 norm)
     *
     * @return ratio of largest to smallest singular value.
     */

    public double cond() {
        return new SingularValueDecomposition(this).cond();
    }

    /**
     * Make a deep copy of a matrix
     */

    @Override
    public MatrixD copy() {
        final MatrixD X = new MatrixD(m, n);
        final double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            if (n >= 0)
                System.arraycopy(element[i], 0, C[i], 0, n);
        }
        return X;
    }

    /**
     * Matrix determinant
     *
     * @return determinant
     */

    public double det() {
        return new LUDecomposition(this).det();
    }

    /**
     * Eigenvalue Decomposition
     *
     * @return EigenvalueDecomposition
     * @see EigenvalueDecomposition
     */

    public EigenvalueDecomposition eig() {
        return new EigenvalueDecomposition(this);
    }

    /**
     * Get a single element.
     *
     * @param i Row index.
     * @param j Column index
     * @return A(i,j)
     */
    @Override
    public double get(final int i, final int j) {
        return element[i][j];
    }

    /**
     * Access the internal two-dimensional array.
     *
     * @return Pointer to the two-dimensional array of matrix elements.
     */

    public double[][] getArray() {
        return element;
    }

    /**
     * Copy the internal two-dimensional array.
     *
     * @return Two-dimensional array copy of matrix elements.
     */
    public double[][] getArrayCopy() {
        final double[][] C = new double[m][n];
        for (int i = 0; i < m; i++) {
            System.arraycopy(element[i], 0, C[i], 0, n);
        }
        return C;
    }

    /**
     * Make a one-dimensional column packed copy of the internal array.
     *
     * @return Matrix elements packed in a one-dimensional array by columns.
     */

    public double[] getColumnPackedCopy() {
        final double[] vals = new double[m * n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                vals[i + j * m] = element[i][j];
            }
        }
        return vals;
    }

    /**
     * Get a sub-matrix.
     *
     * @param i0 Initial row index
     * @param i1 Final row index
     * @param j0 Initial column index
     * @param j1 Final column index
     * @return A(i0:i1,j0:j1)
     * @exception ArrayIndexOutOfBoundsException sub-matrix indices
     */
    public MatrixD getMatrix(final int i0, final int i1, final int j0, final int j1) {
        final MatrixD X = new MatrixD(i1 - i0 + 1, j1 - j0 + 1);
        final double[][] B = X.getArray();
        try {
            for (int i = i0; i <= i1; i++) {
                System.arraycopy(element[i], j0, B[i - i0], j0, j1 - j0);
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return X;
    }

    /**
     * Get a sub-matrix.
     *
     * @param i0 Initial row index
     * @param i1 Final row index
     * @param c Array of column indices.
     * @return A(i0:i1,c(:))
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    public MatrixD getMatrix(final int i0, final int i1, final int[] c) {
        final MatrixD X = new MatrixD(i1 - i0 + 1, c.length);
        final double[][] B = X.getArray();
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = 0; j < c.length; j++) {
                    B[i - i0][j] = element[i][c[j]];
                }
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return X;
    }

    /**
     * Get a sub-matrix.
     *
     * @param r Array of row indices.
     * @param j0 Initial column index
     * @param j1 Final column index
     * @return A(r(:),j0:j1)
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    public MatrixD getMatrix(final int[] r, final int j0, final int j1) {
        final MatrixD X = new MatrixD(r.length, j1 - j0 + 1);
        final double[][] B = X.getArray();
        try {
            for (int i = 0; i < r.length; i++) {
                if (j1 + 1 - j0 >= 0)
                    System.arraycopy(element[r[i]], j0, B[i], j0 - j0, j1 + 1 - j0);
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return X;
    }

    /**
     * Get a sub-matrix.
     *
     * @param r Array of row indices.
     * @param c Array of column indices.
     * @return A(r(:),c(:))
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    public MatrixD getMatrix(final int[] r, final int[] c) {
        final MatrixD X = new MatrixD(r.length, c.length);
        final double[][] B = X.getArray();
        try {
            for (int i = 0; i < r.length; i++) {
                for (int j = 0; j < c.length; j++) {
                    B[i][j] = element[r[i]][c[j]];
                }
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return X;
    }

    /**
     * Make a one-dimensional row Major copy of the internal array.
     *
     * @return Matrix elements packed in a one-dimensional array by rows.
     */

    public double[] getRowPackedCopy() {
        final double[] vals = new double[m * n];
        for (int i = 0; i < m; i++) {
            System.arraycopy(element[i], 0, vals, i * m, n);
        }
        return vals;
    }

    /**
     * Matrix inverse or pseudo-inverse
     *
     * @return inverse(A) if A is square, pseudo-inverse otherwise.
     */
    public MatrixD inverse() {
        return solve(MatrixFactory.identity(m, m));
    }

    /**
     * LU Decomposition
     *
     * @return LUDecomposition
     * @see LUDecomposition
     */
    public LUDecomposition lu() {
        return new LUDecomposition(this);
    }

    /**
     * C = A - B
     *
     * @param B another matrix
     * @return A - B
     */

    public MatrixD minus(final MatrixD B) {
        checkMatrixDimensions(B);
        final MatrixD X = new MatrixD(m, n);
        final double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = element[i][j] - B.element[i][j];
            }
        }
        return X;
    }

    /**
     * A = A - B
     *
     * @param B another matrix
     * @return A - B
     */

    public MatrixD minusEquals(final MatrixD B) {
        checkMatrixDimensions(B);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                element[i][j] = element[i][j] - B.element[i][j];
            }
        }
        return this;
    }

    /**
     * C = A + B
     *
     * @param B another matrix
     * @return A + B
     */
    public MatrixD plus(final MatrixD B) {
        checkMatrixDimensions(B);
        final MatrixD X = new MatrixD(m, n);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                X.set(i, j, get(i, j) + B.get(i, j));
            }
        }
        return X;
    }

    /**
     * A = A + B
     *
     * @param B another matrix
     * @return A + B
     */
    public MatrixD plusEquals(final MatrixD B) {
        checkMatrixDimensions(B);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                set(i, j, get(i, j) + B.get(i, j));
            }
        }
        return this;
    }

    /**
     * Print the matrix to stdout. Line the elements up in columns with a Fortran-like 'Fw.d' style format.
     *
     * @param w Column width.
     * @param d Number of digits after the decimal.
     */

    public void print(final int w, final int d) {
        print(new PrintWriter(System.out, true), w, d);
    }

    /**
     * Print the matrix to stdout. Line the elements up in columns. Use the format object, and right justify within
     * columns of width characters. Note that is the matrix is to be read back in, you probably will want to use a
     * NumberFormat that is set to US Locale.
     *
     * @param format A Formatting object for individual elements.
     * @param width Field width for each column.
     * @see java.text.DecimalFormat#setDecimalFormatSymbols
     */

    public void print(final NumberFormat format, final int width) {
        print(new PrintWriter(System.out, true), format, width);
    }

    /**
     * Print the matrix to the output stream. Line the elements up in columns with a Fortran-like 'Fw.d' style format.
     *
     * @param output Output stream.
     * @param w Column width.
     * @param d Number of digits after the decimal.
     */

    public void print(final PrintWriter output, final int w, final int d) {
        final DecimalFormat format = new DecimalFormat();
        format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        format.setMinimumIntegerDigits(1);
        format.setMaximumFractionDigits(d);
        format.setMinimumFractionDigits(d);
        format.setGroupingUsed(false);
        print(output, format, w + 2);
    }

    /**
     * Print the matrix to the output stream. Line the elements up in columns. Use the format object, and right justify
     * within columns of width characters. Note that is the matrix is to be read back in, you probably will want to use
     * a NumberFormat that is set to US Locale.
     *
     * @param output the output stream.
     * @param format A formatting object to format the matrix elements
     * @param width Column width.
     * @see java.text.DecimalFormat#setDecimalFormatSymbols
     */

    public void print(final PrintWriter output, final NumberFormat format, final int width) {
        output.println(); // start on new line.
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                final String s = format.format(element[i][j]); // format the number
                final int padding = Math.max(1, width - s.length()); // At _least_ 1 space
                for (int k = 0; k < padding; k++) {
                    output.print(' ');
                }
                output.print(s);
            }
            output.println();
        }
        output.println(); // end with blank line.
    }

    /**
     * Matrix inversion using the SVD pseudo inverse
     *
     * @param condition condition number
     * @return inverse matrix
     */
    public MatrixD pseudoInverse(final double condition) {
        final SingularValueDecomposition decomp = svd();
        final double invCondition = condition > 0 ? 1.0 / condition : 1e19;
        final double[] sig = decomp.getSingularValues();
        final MatrixD newS = new MatrixD(sig.length, sig.length);
        final double first = sig[0];
        for (int i = 0; i < sig.length; i++) {
            if (sig[i] / first < invCondition || MathBase.abs(sig[i]) < 2 * Double.MIN_VALUE) {
                // discard eigenvalue
                if (true) {
                    System.out.println("TMatrixD::drop singluar eigenvalue " + i);
                }
                newS.set(i, i, 0.0);
            } else {
                newS.set(i, i, 1.0 / sig[i]);
            }
        }
        decomp.rank();

        return decomp.getV().times(newS).times(decomp.getU().transpose());
    }

    /**
     * QR Decomposition
     *
     * @return QRDecomposition
     * @see QRDecomposition
     */

    public QRDecomposition qr() {
        return new QRDecomposition(this);
    }

    /**
     * Matrix rank
     *
     * @return effective numerical rank, obtained from SVD.
     */

    public int rank() {
        return new SingularValueDecomposition(this).rank();
    }

    /**
     * Set a single element.
     *
     * @param i Row index.
     * @param j Column index.
     * @param s A(i,j).
     */
    @Override
    public void set(final int i, final int j, final double s) {
        element[i][j] = s;
    }

    /**
     * Set a submatrix.
     *
     * @param i0 Initial row index
     * @param i1 Final row index
     * @param j0 Initial column index
     * @param j1 Final column index
     * @param X A(i0:i1,j0:j1)
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */

    public void setMatrix(final int i0, final int i1, final int j0, final int j1, final MatrixD X) {
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = j0; j <= j1; j++) {
                    element[i][j] = X.get(i - i0, j - j0);
                }
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
    }

    /**
     * Set a submatrix.
     *
     * @param i0 Initial row index
     * @param i1 Final row index
     * @param c Array of column indices.
     * @param X A(i0:i1,c(:))
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */

    public void setMatrix(final int i0, final int i1, final int[] c, final MatrixD X) {
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = 0; j < c.length; j++) {
                    element[i][c[j]] = X.get(i - i0, j);
                }
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
    }

    /**
     * Set a submatrix.
     *
     * @param r Array of row indices.
     * @param j0 Initial column index
     * @param j1 Final column index
     * @param X A(r(:),j0:j1)
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */

    public void setMatrix(final int[] r, final int j0, final int j1, final MatrixD X) {
        try {
            for (int i = 0; i < r.length; i++) {
                for (int j = j0; j <= j1; j++) {
                    element[r[i]][j] = X.get(i, j - j0);
                }
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
    }

    /**
     * Set a submatrix.
     *
     * @param r Array of row indices.
     * @param c Array of column indices.
     * @param X A(r(:),c(:))
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */

    public void setMatrix(final int[] r, final int[] c, final MatrixD X) {
        try {
            for (int i = 0; i < r.length; i++) {
                for (int j = 0; j < c.length; j++) {
                    element[r[i]][c[j]] = X.get(i, j);
                }
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
    }

    /**
     * Solve A*X = B
     *
     * @param B right hand side
     * @return solution if A is square, least squares solution otherwise
     */

    public MatrixD solve(final MatrixD B) {
        return m == n ? new LUDecomposition(this).solve(B) : new QRDecomposition(this).solve(B);
    }

    /**
     * Solve X*A = B, which is also A'*X' = B'
     *
     * @param B right hand side
     * @return solution if A is square, least squares solution otherwise.
     */

    public MatrixD solveTranspose(final MatrixD B) {
        return transpose().solve(B.transpose());
    }

    /**
     * Square individual matrix elements
     */
    public void squareElements() {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                element[i][j] = MathBase.sqr(element[i][j]);
            }
        }
    }

    /**
     * Singular Value Decomposition
     *
     * @return SingularValueDecomposition
     * @see SingularValueDecomposition
     */

    public SingularValueDecomposition svd() {
        return new SingularValueDecomposition(this);
    }

    /**
     * Multiply a matrix by a scalar, C = s*A
     *
     * @param s scalar
     * @return s*A
     */

    public MatrixD times(final double s) {
        final MatrixD X = new MatrixD(m, n);
        final double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = s * element[i][j];
            }
        }
        return X;
    }

    /**
     * Linear algebraic matrix multiplication, A * B
     *
     * @param B another matrix
     * @return Matrix product, A * B
     * @exception IllegalArgumentException Matrix inner dimensions must agree.
     */
    public MatrixD times(final MatrixD B) {
        if (B.m != n) {
            throw new IllegalArgumentException("Matrix inner dimensions must agree.");
        }
        final MatrixD X = new MatrixD(m, B.n);
        final double[][] C = X.getArray();
        final double[] vector = new double[n];

        if (B.n != 1) {
            // general matrix-matrix multiplication
            for (int j = 0; j < B.n; j++) {
                for (int k = 0; k < n; k++) {
                    vector[k] = B.element[k][j];
                }
                for (int i = 0; i < m; i++) {
                    final double[] Arowi = element[i];
                    double s = 0;
                    for (int k = 0; k < n; k++) {
                        s += Arowi[k] * vector[k];
                    }
                    C[i][j] = s;
                }
            }
        } else {
            // special case of a matrix-vector multiplication
            // that allows some speed optimisation

            // copy first index of each row into vector
            for (int i = 0; i < n; i++) {
                vector[i] = B.element[i][0];
            }

            for (int i = 0; i < m; i++) {
                double val = 0;
                for (int j = 0; j < n; j++) {
                    val += element[i][j] * vector[j];
                }
                C[i][0] = val;
            }
        }
        return X;
    }

    /**
     * Multiply a matrix by a scalar in place, A = s*A
     *
     * @param s scalar
     * @return replace A by s*A
     */
    public MatrixD timesEquals(final double s) {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                element[i][j] = s * element[i][j];
            }
        }
        return this;
    }

    /**
     * Matrix trace.
     *
     * @return sum of the diagonal elements.
     */

    public double trace() {
        double t = 0;
        for (int i = 0; i < Math.min(m, n); i++) {
            t += element[i][i];
        }
        return t;
    }

    /**
     * Matrix transpose.
     *
     * @return A^{T}
     */
    public MatrixD transpose() {
        final MatrixD X = new MatrixD(n, m);
        final double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[j][i] = element[i][j];
            }
        }
        return X;
    }

    // DecimalFormat is a little disappointing coming from Fortran or C's printf.
    // Since it doesn't pad on the left, the elements will come out different
    // widths. Consequently, we'll pass the desired column width in as an
    // argument and do the extra padding ourselves.

    /**
     * Unary minus
     *
     * @return -A
     */
    public MatrixD uminus() {
        final MatrixD X = new MatrixD(m, n);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                X.set(i, j, -element[i][j]);
            }
        }
        return X;
    }

    /**
     * Read a matrix from a stream. The format is the same the print method, so printed matrices can be read back in
     * (provided they were printed using US Locale). Elements are separated by whitespace, all the elements for each row
     * appear on a single line, the last row is followed by a blank line.
     *
     * @param input the input stream.
     * @return matrix object that has been recovered from file
     * @throws java.io.IOException in case of troubles ;-)
     */

    public static MatrixD read(final BufferedReader input) throws java.io.IOException {
        final StreamTokenizer tokenizer = new StreamTokenizer(input);

        // Although StreamTokenizer will parse numbers, it doesn't recognize
        // scientific notation (E or D); however, Double.valueOf does.
        // The strategy here is to disable StreamTokenizer's number parsing.
        // We'll only get whitespace delimited words, EOL's and EOF's.
        // These words should all be numbers, for Double.valueOf to parse.

        tokenizer.resetSyntax();
        tokenizer.wordChars(0, 255);
        tokenizer.whitespaceChars(0, ' ');
        tokenizer.eolIsSignificant(true);
        final java.util.Vector<Double> v = new java.util.Vector<>();

        // Ignore initial empty lines
        while (tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
        }
        if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
            throw new java.io.IOException("Unexpected EOF on matrix read.");
        }
        do {
            v.addElement(Double.valueOf(tokenizer.sval)); // Read & store 1st row.
        } while (tokenizer.nextToken() == StreamTokenizer.TT_WORD);

        final int n = v.size(); // Now we've got the number of columns!
        Double[] row = new Double[n];
        v.removeAllElements();
        for (int j = 0; j < n; j++) {
            // extract the elements of the 1st row.
            v.addElement(v.elementAt(j));
        }
        // Start storing rows instead of columns.
        while (tokenizer.nextToken() == StreamTokenizer.TT_WORD) {
            // While non-empty lines
            int j = 0;
            do {
                if (j >= n) {
                    throw new java.io.IOException("Row " + v.size() + " is too long.");
                }
                v.addElement(Double.valueOf(tokenizer.sval));
            } while (tokenizer.nextToken() == StreamTokenizer.TT_WORD);
            if (j < n) {
                throw new java.io.IOException("Row " + v.size() + " is too short.");
            }
        }
        final int m = v.size(); // Now we've got the number of rows.
        final double[][] A = new double[m][];
        v.copyInto(A); // copy the rows out of the vector
        return new MatrixD(A);
    }
}
