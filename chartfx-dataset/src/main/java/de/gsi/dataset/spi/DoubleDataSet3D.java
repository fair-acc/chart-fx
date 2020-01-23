package de.gsi.dataset.spi;

import java.util.Arrays;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.utils.AssertUtils;

/**
 * Implementation of a AbstractDataSet3D backed by arrays. The z-values are stored in a 2-dim array d[row][column] or
 * d[y][x].
 *
 * @author braeun
 */
public class DoubleDataSet3D extends AbstractDataSet3D<DoubleDataSet3D> {
    private static final long serialVersionUID = 1289344630607938420L;
    private double[] xValues;
    private double[] yValues;
    private double[][] zValues;

    /**
     * @param name of data set
     */
    public DoubleDataSet3D(final String name) {
        super(name);
        xValues = new double[0];
        yValues = new double[0];
        zValues = new double[0][0];
    }

    /**
     * @param name of data set
     * @param xValues array containing new X coordinates
     * @param yValues array containing new X coordinates
     * @param zValues array containing new X coordinates
     */
    public DoubleDataSet3D(final String name, final double[] xValues, final double[] yValues,
            final double[][] zValues) {
        super(name);
        checkDimensionConsistency(xValues, yValues, zValues);
        this.xValues = xValues;
        this.yValues = yValues;
        this.zValues = zValues;
    }

    /**
     * @param name of data set
     * @param zValues array containing new X coordinates
     */
    public DoubleDataSet3D(final String name, final double[][] zValues) {
        super(name);
        yValues = new double[zValues.length];
        for (int y = 0; y < yValues.length; y++) {
            yValues[y] = y;
        }
        if (yValues.length > 0) {
            xValues = new double[zValues[0].length];
            for (int x = 0; x < xValues.length; x++) {
                xValues[x] = x;
            }
        } else {
            xValues = new double[0];
        }
        this.zValues = zValues;
    }

    /**
     * @param name of data set
     * @param dimX horizontal binning dimension (equidistant model)
     * @param dimY vertical binning dimension (equidistant model)
     */
    public DoubleDataSet3D(final String name, final int dimX, final int dimY) {
        super(name);
        zValues = new double[dimY][dimX];
        yValues = new double[zValues.length];
        for (int y = 0; y < yValues.length; y++) {
            yValues[y] = y;
        }
        if (yValues.length > 0) {
            xValues = new double[zValues[0].length];
            for (int x = 0; x < xValues.length; x++) {
                xValues[x] = x;
            }
        } else {
            xValues = new double[0];
        }
    }

    /**
     * clears all data points
     * 
     * @return itself (fluent design)
     */
    public DoubleDataSet3D clearData() {
        lock().writeLockGuard(() -> {
            for (int i = 0; i < zValues.length; i++) {
                fillArray(zValues[i], 0, zValues[i].length, 0.0);
            }
        });
        fireInvalidated(new RemovedDataEvent(this, "clearData()"));
        return this;
    }

    @Override
    public int getDataCount(final int dimIndex) {
        if (dimIndex == DataSet.DIM_X) {
            return xValues.length;
        } else if (dimIndex == DataSet.DIM_Y) {
            return yValues.length;
        }
        return xValues.length * yValues.length;
    }

    @Override
    public String getStyle(final int index) {
        return null;
    }

    @Override
    public final double get(final int dimIndex, final int index) {
        switch (dimIndex) {
        case DIM_X:
            return xValues[index];
        case DIM_Y:
            return yValues[index];
        case DIM_Z:
            return zValues[index / xValues.length][index % xValues.length];
        default:
            throw new IndexOutOfBoundsException("dimIndex cannot be < 2");
        }
    }

    @Override
    public double getX(final int i) {
        return xValues[i];
    }

    @Override
    public double[] getValues(final int dimIndex) {
        switch (dimIndex) {
        case DIM_X:
            return Arrays.copyOf(xValues, xValues.length);
        case DIM_Y:
            return Arrays.copyOf(yValues, yValues.length);
        case DIM_Z:
            return super.getValues(dimIndex);
        default:
            throw new IndexOutOfBoundsException("dimIndex cannot be < 2");
        }
    }

    @Override
    public double getZ(final int xIndex, final int yIndex) {
        return zValues[yIndex][xIndex];
    }

    public double[][] getZValues() {
        return zValues;
    }

    /**
     * overwrites/replaces data points with new coordinates
     * 
     * @param xValues array containing new X coordinates
     * @param yValues array containing new X coordinates
     * @param zValues array containing new X coordinates
     */
    public void set(final double[] xValues, final double[] yValues, final double[][] zValues) {
        checkDimensionConsistency(xValues, yValues, zValues);
        this.xValues = xValues;
        this.yValues = yValues;
        this.zValues = zValues;
    }

    /**
     * @param xIndex index of the to be modified point
     * @param yIndex index of the to be modified point
     * @param z new Z coordinate
     */
    public void set(final int xIndex, final int yIndex, final double z) {
        zValues[yIndex][xIndex] = z;
    }

    public void set(final int xIndex, final int yIndex, final double x, final double y, final double z) {
        xValues[xIndex] = x;
        yValues[yIndex] = y;
        zValues[yIndex][xIndex] = z;
    }

    /**
     * @param xIndex index of the to be modified point
     * @param x new X coordinate
     */
    public void setX(final int xIndex, final double x) {
        xValues[xIndex] = x;
    }

    /**
     * @param yIndex index of the to be modified point
     * @param y new Y coordinate
     */
    public void setY(final int yIndex, final double y) {
        yValues[yIndex] = y;
    }

    private static void checkDimensionConsistency(final double[] xValues, final double[] yValues,
            final double[][] zValues) {
        if (xValues == null) {
            throw new IllegalArgumentException("xValues array is null");
        }
        if (yValues == null) {
            throw new IllegalArgumentException("yValues array is null");
        }
        if (zValues == null) {
            throw new IllegalArgumentException("zValues array is null");
        }
        if (zValues.length == 0) {
            if (0 != xValues.length || 0 != yValues.length) {
                final String msg = String.format(
                        "array zValues is empty but: xValues.length = %d and yValues.length = %d", xValues.length, yValues.length);
                throw new IllegalArgumentException(msg);
            }
        } else {
            if (zValues.length != yValues.length) {
                final String msg = String.format("array dimension mismatch: zValues.length = %d != yValues.length = %d",
                        zValues.length, yValues.length);
                throw new IllegalArgumentException(msg);
            }
            if (zValues[0].length != xValues.length) {
                final String msg = String.format(
                        "array dimension mismatch: zValues[0].length = %d != xValues.length = %d", zValues[0].length,
                        xValues.length);
                throw new IllegalArgumentException(msg);
            }
        }
    }

    /**
     * fast filling of an array with a default value <br>
     * initialize a smaller piece of the array and use the System.arraycopy call to fill in the rest of the array in an
     * expanding binary fashion
     *
     * @param array to be initialized
     * @param indexStart the first index to be set (inclusive)
     * @param indexStop the last index to be set (exclusive)
     * @param value the value for each to be set element
     */
    protected static void fillArray(final double[] array, final int indexStart, final int indexStop,
            final double value) {
        AssertUtils.notNull("array", array);
        final int len = indexStop - indexStart;

        if (len > 0) {
            array[indexStart] = value;
        }

        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, indexStart, array, indexStart + i, (len - i) < i ? len - i : i);
        }
    }
}
