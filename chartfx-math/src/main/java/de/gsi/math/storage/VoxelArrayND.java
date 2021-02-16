package de.gsi.math.storage;

public interface VoxelArrayND {
    VoxelArrayND copy();

    double[] get(int[] index);

    int[] getInverseLocalIndex(int index);

    double[] getLocal(int localIndex);

    int getLocalIndex(int[] index);

    int getLocalStorageDim();

    int getValueDimension();

    void initialiseWithValue(double val);

    void set(int[] index, double[] val);

    void setLocal(int localIndex, double[] val);
}
