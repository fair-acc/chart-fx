package de.gsi.math.storage;

public interface VoxelArrayND {

    public int getLocalIndex(int[] index);

    public int[] getInverseLocalIndex(int index);

    public double[] get(int[] index);

    public void set(int[] index, double val[]);

    public int getLocalStorageDim();

    public double[] getLocal(int localIndex);

    public void setLocal(int localIndex, double[] val);

    public void initialiseWithValue(double val);

    public int getValueDimension();

    public VoxelArrayND copy();
}
