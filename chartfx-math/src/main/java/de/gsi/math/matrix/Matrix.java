package de.gsi.math.matrix;

public interface Matrix extends Cloneable, java.io.Serializable {

    public void checkMatrixDimensions(Matrix B);

    /**
     * @return copy of matrix
     */
    public Matrix copy();

    /**
     * @param i row index
     * @param j column index
     * @return value of matrix element
     */
    public double get(int i, int j);

    /**
     * Get column dimension.
     * 
     * @return n, the number of columns.
     */
    public int getColumnDimension();

    /**
     * Get row dimension
     * 
     * @return m, the number of rows.
     */
    public int getRowDimension();

    /**
     * @param i row index
     * @param j column index
     * @param val new value
     */
    public void set(int i, int j, double val);
}
