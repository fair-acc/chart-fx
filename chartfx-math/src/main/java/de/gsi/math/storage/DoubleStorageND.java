package de.gsi.math.storage;

import java.security.InvalidParameterException;
import java.util.Arrays;

public class DoubleStorageND implements VoxelArrayND {
	private int			fndimIn;
	private int			fdimOut;
	private int[]			flength;
	private double[][]	fdata;
	
	public DoubleStorageND(int[] dimIn, int outDim) {
		fndimIn   = dimIn.length;
		fdimOut  = outDim;
		flength = Arrays.copyOf(dimIn, dimIn.length);
		int rlength = 1;
		for (int i=0; i < fndimIn; i++) {
			rlength *= flength[i];
		}
		fdata = new double[rlength][fdimOut];
	}
	
	public DoubleStorageND(DoubleStorageND data) {
		fndimIn = data.fndimIn;
		fdimOut = data.fdimOut;
		flength = Arrays.copyOf(data.flength, data.flength.length);
		int rlength = 1;
		for (int i=0; i < fndimIn; i++) {
			rlength *= flength[i];
		}
		fdata = new double[rlength][fdimOut];
		for (int i=0; i < rlength; i++) {
			fdata[i] = Arrays.copyOf(data.fdata[i], data.fdata[i].length);
		}
	}
	
	@Override
	public VoxelArrayND copy() {
		return new DoubleStorageND(this);
	}
	
	@Override
	public int getValueDimension() {		
		return fdimOut;
	}
	
	public int getLocalIndex(int[] index) {
		int ret = 0;
		for (int i=0; i < fndimIn; i++) {
			int multiplicator = 1;
			for (int j=i; j < fndimIn-1; j++) {
				multiplicator *= flength[j+1];
			}
			ret += multiplicator*index[i];
		}		
		return ret;
	}

	@Override
	public int[] getInverseLocalIndex(int index) {
		// TODO find faster routine
		int lindex = index;
		int[] ret = new int[fndimIn];
		for (int i=0; i < fndimIn; i++) {
			int multiplicator = 1;
			for (int j=i; j < fndimIn-1; j++) {
				multiplicator *= flength[j+1];
			}	
			
			/*
			do {
				ret[i]++;
			} while (ret[i]*multiplicator<=lindex);
			ret[i]--;
			*/
			
			ret[i] = lindex/multiplicator;
			
			//System.out.printf("%d - %d\n", lindex, ret[i]*multiplicator);
			lindex -= ret[i]*multiplicator;
		}				
		return ret;
	}
	
	public double[] get(int[] index) {
		final int rindex = getLocalIndex(index);
		return fdata[rindex];
	}
	
	@Override
	public int getLocalStorageDim() {
		return fdata.length;
	}
	
	@Override
	public double[] getLocal(int localIndex) {
		return fdata[localIndex]; 
	}
	
	@Override
	public void setLocal(int localIndex, double[] val) {
		fdata[localIndex] = val;
	}	
	
	@Override
	public void initialiseWithValue(double val) {
		for (int i=0; i < fdata.length; i++) {
			for (int j=0; j < fdata[i].length; j++) {
				fdata[i][j] = val;			
			}
		}
	 
	}
	
	@Override
	public void set(int[] index, double val[]) {
		if (val.length != this.fdimOut) {
			throw new InvalidParameterException("invalid value dimension " + val.length);
		}
		final int rindex = getLocalIndex(index);
		fdata[rindex] = val;
	}
	
	public static void main(String argv[]) {
		DoubleStorageND test1 = new DoubleStorageND(new int[]{3,5}, 10);		
		for (int i=0; i < 3; i++) {
			for (int j=0; j < 5; j++) {
				System.out.printf("1:tupple index (%d,%d) mapped to %d\n", i, j, test1.getLocalIndex(new int[]{i,j}));
			}
		}
		
		DoubleStorageND test2 = new DoubleStorageND(new int[]{3,5,2}, 10);		
		for (int i=0; i < 3; i++) {
			for (int j=0; j < 5; j++) {
				for (int k=0; k < 2; k++) {
					System.out.printf("2:tupple index (%d,%d,%d) mapped to %d\n", 
							i, j, k, test2.getLocalIndex(new int[]{i,j,k}));
				}
			}
		}
		System.out.println();
		
		for (int i=0; i < test2.getLocalStorageDim(); i++) {
			int[] lindex = test2.getInverseLocalIndex(i);
			System.out.printf("2:inverse tupple index %3d mapped to (%d,%d,%d)\n", 
					i, lindex[0], lindex[1], lindex[2]);			
		}
	}	

}
