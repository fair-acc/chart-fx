package de.gsi.math.matrix;

public abstract class AbstractMatrix implements Matrix {

    private static final long serialVersionUID = 4161055769254544900L;
    protected int m; // row dimension
    protected int n; // column dimension

    /**
     * Get row dimension
     * 
     * @return m, the number of rows.
     */
    @Override
    public int getRowDimension() {
        return m;
    }

    /**
     * Get column dimension.
     * 
     * @return n, the number of columns.
     */
    @Override
    public int getColumnDimension() {
        return n;
    }

    /**
     * One norm
     * 
     * @return maximum column sum.
     */
    public double norm1() {
        double f = 0;
        for (int j = 0; j < n; j++) {
            double s = 0;
            for (int i = 0; i < m; i++) {
                s += Math.abs(get(i, j));
            }
            f = Math.max(f, s);
        }
        return f;
    }

    /**
     * Two norm
     * 
     * @return maximum singular value.
     */
    public double norm2() {
        if (this instanceof MatrixD) {
            return (new SingularValueDecomposition((MatrixD) this).norm2());
        } else {
            MatrixD temp = new MatrixD(getRowDimension(), getColumnDimension());
            for (int i = 0; i < getRowDimension(); i++) {
                for (int j = 0; j < getColumnDimension(); j++) {
                    temp.set(i, j, get(i, j));
                }
            }
            return (new SingularValueDecomposition(temp).norm2());
        }
    }

    /**
     * Infinity norm
     * 
     * @return maximum row sum.
     */
    public double normInf() {
        double f = 0;
        for (int i = 0; i < m; i++) {
            double s = 0;
            for (int j = 0; j < n; j++) {
                s += Math.abs(get(i, j));
            }
            f = Math.max(f, s);
        }
        return f;
    }

    /**
     * Frobenius norm
     * 
     * @return sqrt of sum of squares of all elements.
     */
    public double normF() {
        double f = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                f = Math.hypot(f, get(i, j));
            }
        }
        return f;
    }

    /** Check if size(A) == size(B) **/
    @Override
    public void checkMatrixDimensions(Matrix B) {
        if (B.getRowDimension() != m || B.getColumnDimension() != n) {
            throw new IllegalArgumentException("Matrix dimensions must agree.");
        }
    }

}
