/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.gsi.chart.data.spi;

import de.gsi.math.ArrayUtils;

/**
 * Implementation of a AbstractDataSet3D backed by arrays. The z-values are
 * stored in a 2-dim array d[row][column] or d[y][x].
 *
 * @author braeun
 */
public class DoubleDataSet3D extends AbstractDataSet3D<DoubleDataSet3D> {

    private double[] xValues;
    private double[] yValues;
    private double[][] zValues;

    public DoubleDataSet3D(final String name) {
        super(name);
        xValues = new double[0];
        yValues = new double[0];
        zValues = new double[0][0];
    }

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

    public DoubleDataSet3D(final String name, final double[] xValues, final double[] yValues,
            final double[][] zValues) {
        super(name);
        checkDimensionConsistency(xValues, yValues, zValues);
        this.xValues = xValues;
        this.yValues = yValues;
        this.zValues = zValues;
    }

    public void set(final double[] xValues, final double[] yValues, final double[][] zValues) {
        checkDimensionConsistency(xValues, yValues, zValues);
        this.xValues = xValues;
        this.yValues = yValues;
        this.zValues = zValues;
    }

    private static void checkDimensionConsistency(final double[] xValues, final double[] yValues,
            final double[][] zValues) {
        if (xValues == null) {
            throw new IllegalArgumentException("xValues array is null");
        }
        if (xValues.length == 0) {
            throw new IllegalArgumentException("xValues array length is '0'");
        }
        if (yValues == null) {
            throw new IllegalArgumentException("yValues array is null");
        }
        if (yValues.length == 0) {
            throw new IllegalArgumentException("yValues array length is '0'");
        }
        if (zValues == null) {
            throw new IllegalArgumentException("zValues array is null");
        }
        if (zValues.length == 0) {
            throw new IllegalArgumentException("zValues array length is '0'");
        }
        if (zValues.length != yValues.length) {
            final String msg = String.format("array dimension mismatch: zValues.length = %d != yValues.length = %d",
                    zValues.length, yValues.length);
            throw new IllegalArgumentException(msg);
        }
        if (zValues[0].length != xValues.length) {
            final String msg = String.format("array dimension mismatch: zValues[0].length = %d != xValues.length = %d",
                    zValues.length, yValues.length);
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public double getZ(final int xIndex, final int yIndex) {
        // TODO: eventually remove disabled code below (performance hot-spot)
        // if (xIndex < 0) {
        // return 0;
        // }
        // if (xIndex > xValues.length) {
        // return 0;
        // }
        // if (yIndex < 0) {
        // return 0;
        // }
        // if (yIndex > yValues.length) {
        // return 0;
        // }
        return zValues[yIndex][xIndex];
    }

    public DoubleDataSet3D clearData() {
        lock();
        for (int i = 0; i < zValues.length; i++) {
            ArrayUtils.fillArray(zValues[i], 0.0);
        }
        unlock();
        fireInvalidated();
        return this;
    }

    @Override
    public void set(final int xIndex, final int yIndex, final double x, final double y, final double z) {
        xValues[xIndex] = x;
        yValues[yIndex] = y;
        zValues[yIndex][xIndex] = z;

    }

    public void setX(final int xIndex, final double x) {
        xValues[xIndex] = x;
    }

    public void setY(final int yIndex, final double y) {
        yValues[yIndex] = y;
    }

    public void set(final int xIndex, final int yIndex, final double z) {
        zValues[yIndex][xIndex] = z;

    }

    @Override
    public int getXDataCount() {
        return xValues.length;
    }

    @Override
    public int getYDataCount() {
        return yValues.length;
    }

    @Override
    public double getX(final int i) {
        return xValues[i];
    }

    @Override
    public double getY(final int i) {
        return yValues[i];
    }

    @Override
    public String getStyle(final int index) {
        return null;
    }

}
