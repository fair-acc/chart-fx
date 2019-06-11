package de.gsi.math.storage;

import java.util.Arrays;

public class DoubleStorage1D implements VoxelArrayND {
	protected double[]	fdata;
	
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
	
	@Override
	public int getValueDimension() {		
		return 1;
	}
	
	public double[] getArray() {
		return fdata;
	}
	
	public void setArray(double[] data) {
		fdata = Arrays.copyOf(data, data.length);
	}
	
	public double get(int index) {
		return fdata[index];
	}
	
	public void set(int index, double val) {
		fdata[index] = val;
	}
	
	@Override
	public double[] get(int[] index) {
		return new double[]{get(index[0])};
	}
	
	@Override
	public void set(int[] index, double[] val) {
		set(index[0], val[0]);		
	}

	@Override
	public double[] getLocal(int localIndex) {
		return new double[]{get(localIndex)};
	}
	
	@Override
	public void setLocal(int localIndex, double[] val) {		
		fdata[localIndex] = val[0];
	}	

	@Override
	public int getLocalIndex(int[] index) {
		return index[0];
	}
	
	@Override
	public int[] getInverseLocalIndex(int index) {
		return new int[]{index};
	}

	@Override
	public int getLocalStorageDim() {		
		return fdata.length;
	}

	@Override
	public void initialiseWithValue(double val) {
		for (int i=0; i < fdata.length; i++) {
			fdata[i] = val;
		}
	}	

}
