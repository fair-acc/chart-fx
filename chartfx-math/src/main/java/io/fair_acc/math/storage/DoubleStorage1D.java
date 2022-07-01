package io.fair_acc.math.storage;

import java.util.Arrays;

public class DoubleStorage1D implements VoxelArrayND {
    protected double[] fdata;

    public DoubleStorage1D(double[] data) {
        setArray(data);
    }

    public DoubleStorage1D(DoubleStorage1D data) {
        setArray(data.fdata);
    }

    @Override
    public VoxelArrayND copy() {
        return new DoubleStorage1D(fdata);
    }

    public double get(int index) {
        return fdata[index];
    }

    @Override
    public double[] get(int[] index) {
        return new double[] { get(index[0]) };
    }

    public double[] getArray() {
        return fdata;
    }

    @Override
    public int[] getInverseLocalIndex(int index) {
        return new int[] { index };
    }

    @Override
    public double[] getLocal(int localIndex) {
        return new double[] { get(localIndex) };
    }

    @Override
    public int getLocalIndex(int[] index) {
        return index[0];
    }

    @Override
    public int getLocalStorageDim() {
        return fdata.length;
    }

    @Override
    public int getValueDimension() {
        return 1;
    }

    @Override
    public void initialiseWithValue(double val) {
        Arrays.fill(fdata, val);
    }

    public void set(int index, double val) {
        fdata[index] = val;
    }

    @Override
    public void set(int[] index, double[] val) {
        set(index[0], val[0]);
    }

    public void setArray(double[] data) {
        fdata = Arrays.copyOf(data, data.length);
    }

    @Override
    public void setLocal(int localIndex, double[] val) {
        fdata[localIndex] = val[0];
    }
}
