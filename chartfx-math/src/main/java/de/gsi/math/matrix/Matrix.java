package de.gsi.math.matrix;

public interface Matrix extends Cloneable, java.io.Serializable {
    void checkMatrixDimensions(Matrix B);

    /**
     * @return copy of matrix
     */
    Matrix copy();

    /**
     * @param i row index
     * @param j column index
     * @return value of matrix element
     */
    double get(int i, int j);

    /**
     * Get column dimension.
     * 
     * @return n, the number of columns.
     */
    int getColumnDimension();

    /**
     * Get row dimension
     * 
     * @return m, the number of rows.
     */
    int getRowDimension();

    /**
     * @param i row index
     * @param j column index
     * @param val new value
     */
    void set(int i, int j, double val);
}
