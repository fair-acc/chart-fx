package de.gsi.math.storage;

public interface VoxelArrayND {

    public VoxelArrayND copy();

    public double[] get(int[] index);

    public int[] getInverseLocalIndex(int index);

    public double[] getLocal(int localIndex);

    public int getLocalIndex(int[] index);

    public int getLocalStorageDim();

    public int getValueDimension();

    public void initialiseWithValue(double val);

    public void set(int[] index, double val[]);

    public void setLocal(int localIndex, double[] val);
}
