package de.gsi.dataset.samples;

import de.gsi.dataset.utils.ProcessingProfiler;
import de.gsi.math.TRandom;

public class FloatToDoubleBenchmarkSample {
	private static final int N_DIM = 2000;
	protected double[][] matrixD = new double[N_DIM][N_DIM];
	protected float[][] matrixF;
	protected double[] vectorInD;
	protected float[] vectorInF;
	protected double[] vectorOutD;
	protected float[] vectorOutF;
	
	
	public FloatToDoubleBenchmarkSample() {
//		matrixD = new double[N_DIM][N_DIM];
		matrixF = new float[N_DIM][N_DIM];
		vectorInD = new double[N_DIM];
		vectorInF = new float[N_DIM];
		vectorOutD = new double[N_DIM];
		vectorOutF = new float[N_DIM];
		
		TRandom rnd = new TRandom(0);
		for (int i = 0; i < N_DIM; i++) {
			for (int j = 0; j < N_DIM; j++) {
				double val = rnd.Rndm() - 0.5;
				matrixD[i][j] = val;
				matrixF[i][j] = (float)val;
			}
			double val = rnd.Rndm() - 0.5;
			vectorInD[i] = val;
			vectorInF[i] = (float)val;
		}
		
	}
	
	public void testMatrixVectorMultiplicationDouble(final int nIterations) {
		long start = ProcessingProfiler.getTimeStamp();
		for (int iter = 0; iter < nIterations; iter++) {
			for (int i = 0; i < N_DIM; i++) {
				vectorOutD[i] = 0.0;
				for (int j = 0; j < N_DIM; j++) {
					vectorOutD[i] += matrixD[i][j] * vectorInD[j];
				}
			}
			// swap input with output to avoid JIT optimisation
			vectorInD = vectorOutD;			
		}
		int rowSum = 0;
		for (int i = 0; i < N_DIM; i++) {
			rowSum += vectorOutD[i];
		}
		// printout sum to avoid JIT optimisation
		ProcessingProfiler.getTimeDiff(start, "testMatrixVectorMultiplicationDouble() result = " + rowSum);
	}
	
	public void testMatrixVectorMultiplicationFloat(final int nIterations) {
		long start = ProcessingProfiler.getTimeStamp();
		for (int iter = 0; iter < nIterations; iter++) {
			for (int i = 0; i < N_DIM; i++) {
				vectorOutF[i] = 0.0f;
				for (int j = 0; j < N_DIM; j++) {
					vectorOutF[i] += matrixF[i][j] * vectorInF[j];
				}
			}
			// swap input with output to avoid JIT optimisation
			vectorInF = vectorOutF;			
		}
		int rowSum = 0;
		for (int i = 0; i < N_DIM; i++) {
			rowSum += vectorOutF[i];
		}
		// printout sum to avoid JIT optimisation
		ProcessingProfiler.getTimeDiff(start, "testMatrixVectorMultiplicationDouble() result = " + rowSum);
	}
	
	public static void main(String[] args) {
		ProcessingProfiler.setVerboseOutputState(true);
		ProcessingProfiler.setDebugState(true);
		
		final int nIterations = 1000;
		FloatToDoubleBenchmarkSample benchmark = new FloatToDoubleBenchmarkSample();
		
		benchmark.testMatrixVectorMultiplicationDouble(nIterations);
		
		benchmark.testMatrixVectorMultiplicationFloat(nIterations);

		benchmark.testMatrixVectorMultiplicationDouble(nIterations);
		
		benchmark.testMatrixVectorMultiplicationFloat(nIterations);
	}

}
