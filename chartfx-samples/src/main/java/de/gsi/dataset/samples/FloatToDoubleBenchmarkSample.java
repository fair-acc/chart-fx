package de.gsi.dataset.samples;

import java.util.ArrayList;

import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.spi.FloatDataSet;
import de.gsi.dataset.utils.ProcessingProfiler;
import de.gsi.math.TRandom;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

@SuppressWarnings("PMD") // this class tests possible performance bottle-necks
                         // not code style/readability
public class FloatToDoubleBenchmarkSample {
    private static final int N_DIM = 2000;
    protected double[][] matrixD;
    protected float[][] matrixF;
    protected double[] vectorInD;
    protected float[] vectorInF;
    protected double[] vectorOutD;
    protected float[] vectorOutF;
    protected ArrayList<Double> doubleList;
    protected DoubleArrayList doubleArrayList;
    protected double[] doubleArray;
    protected DoubleDataSet dataSet1;
    protected de.gsi.dataset.samples.legacy.DoubleDataSet dataSet2; // NOPMD
    protected FloatDataSet dataSet3;

    public FloatToDoubleBenchmarkSample() {
        matrixD = new double[N_DIM][N_DIM];
        matrixF = new float[N_DIM][N_DIM];
        vectorInD = new double[N_DIM];
        vectorInF = new float[N_DIM];
        vectorOutD = new double[N_DIM];
        vectorOutF = new float[N_DIM];
        doubleList = new ArrayList<>(N_DIM);
        doubleArrayList = new DoubleArrayList(N_DIM);
        doubleArray = new double[N_DIM];
        dataSet1 = new DoubleDataSet("test", N_DIM);
        dataSet2 = new de.gsi.dataset.samples.legacy.DoubleDataSet("test", N_DIM); // NOPMD
        dataSet3 = new FloatDataSet("test", N_DIM);

        TRandom rnd = new TRandom(0);
        for (int i = 0; i < N_DIM; i++) {
            for (int j = 0; j < N_DIM; j++) {
                double val = rnd.Rndm() - 0.5;
                matrixD[i][j] = val;
                matrixF[i][j] = (float) val;
            }
            double val = rnd.Rndm() - 0.5;
            vectorInD[i] = val;
            vectorInF[i] = (float) val;
            doubleList.add(i, val);
            doubleArrayList.add(i, val);
            doubleArray[i] = val;
            dataSet1.add(i, val);
            dataSet2.add(i, val); // NOPMD
            dataSet3.add(i, (float) val);
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
        double rowSum = 0;
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

        double rowSum = 0;
        for (int i = 0; i < N_DIM; i++) {
            rowSum += vectorOutD[i];
        }
        // printout sum to avoid JIT optimisation
        ProcessingProfiler.getTimeDiff(start, "testMatrixVectorMultiplicationDouble() result = " + rowSum);
    }

    public void testDoubleListNative(final int nIterations) {
        long start = ProcessingProfiler.getTimeStamp();

        for (int iter = 0; iter < nIterations; iter++) {
            // poor-man's rotate data through list
            final double temp = doubleList.get(0);
            for (int i = 1; i < N_DIM; i++) {
                doubleList.set(i - 1, doubleList.get(i));
            }
            doubleList.set(doubleList.size() - 1, temp);
        }

        // printout first element to avoid JIT optimisation
        ProcessingProfiler.getTimeDiff(start, "testDoubleList() result = " + doubleList.get(0));
    }

    public void testDoubleArrayList1(final int nIterations) {
        long start = ProcessingProfiler.getTimeStamp();

        for (int iter = 0; iter < nIterations; iter++) {
            // poor-man's rotate data through list
            final double temp = doubleArrayList.getDouble(0);
            for (int i = 1; i < N_DIM; i++) {
                doubleArrayList.set(i - 1, doubleArrayList.getDouble(i));
            }
            doubleArrayList.set(doubleArrayList.size() - 1, temp);
        }

        // printout first element to avoid JIT optimisation
        ProcessingProfiler.getTimeDiff(start, "testDoubleArrayList() result = " + doubleArrayList.getDouble(0));
    }

    public void testDoubleArrayList2(final int nIterations) {
        long start = ProcessingProfiler.getTimeStamp();

        for (int iter = 0; iter < nIterations; iter++) {
            // poor-man's rotate data through list
            final double temp = doubleArrayList.elements()[0];
            for (int i = 1; i < N_DIM; i++) {
                doubleArrayList.elements()[i - 1] = doubleArrayList.elements()[i];
            }
            doubleArrayList.elements()[N_DIM - 1] = temp;
        }

        // printout first element to avoid JIT optimisation
        ProcessingProfiler.getTimeDiff(start, "testDoubleArrayList() result = " + doubleArrayList.getDouble(0));
    }

    public void testDoubleArrayPlain(final int nIterations) {
        long start = ProcessingProfiler.getTimeStamp();

        for (int iter = 0; iter < nIterations; iter++) {
            // poor-man's rotate data through list
            final double temp = doubleArray[0];
            for (int i = 1; i < N_DIM; i++) {
                doubleArray[i - 1] = doubleArray[i];
            }
            doubleArray[N_DIM - 1] = temp;
        }

        // printout first element to avoid JIT optimisation
        ProcessingProfiler.getTimeDiff(start, "testDoubleArray() result = " + doubleArray[0]);
    }

    public void testDoubleDataSetOld1(final int nIterations, final boolean lock) {
        long start = ProcessingProfiler.getTimeStamp();

        if (lock) {
            // global lock, 'set' is already locked, so this should do nothing
            // in terms of thread-safety but is put in to assess the penalty of
            // locking
            dataSet2.lock().writeLock();
        }

        // reduce by x 2 since both X & Y are technically set
        // the other examples write/read only one value
        for (int iter = 0; iter < nIterations / 2; iter++) {
            // poor-man's rotate data through list
            final double tempX = dataSet2.getX(0);
            final double tempY = dataSet2.getY(0);
            for (int i = 1; i < N_DIM; i++) {
                dataSet2.set(i - 1, dataSet2.getX(i), dataSet2.getY(i));
            }
            dataSet2.set(dataSet1.getDataCount() - 1, tempX, tempY);
        }
        if (lock) {
            dataSet2.lock().writeUnLock();
        }

        // printout first element to avoid JIT optimisation
        ProcessingProfiler.getTimeDiff(start, "testDoubleDataSetOld1() result = " + dataSet2.getY(0));
    }

    public void testDoubleDataSetOld2(final int nIterations, final boolean lock) {
        long start = ProcessingProfiler.getTimeStamp();

        if (lock) {
            // global lock, 'set' is already locked, so this should do nothing
            // in terms of thread-safety but is put in to assess the penalty of
            // locking
            dataSet2.lock().writeLock();
        }

        // reduce by x 2 since both X & Y are technically set
        // the other examples write/read only one value
        for (int iter = 0; iter < nIterations / 2; iter++) {
            // poor-man's rotate data through list
            final double tempX = dataSet2.getXValues()[0];
            final double tempY = dataSet2.getYValues()[0];
            for (int i = 1; i < N_DIM; i++) {
                dataSet2.getXValues()[i - 1] = dataSet2.getXValues()[i];
                dataSet2.getYValues()[i - 1] = dataSet2.getYValues()[i];
            }
            dataSet2.getXValues()[N_DIM - 1] = tempX;
            dataSet2.getYValues()[N_DIM - 1] = tempY;
        }
 
        if (lock) {
            dataSet2.lock().writeUnLock();
        }

        // printout first element to avoid JIT optimisation
        ProcessingProfiler.getTimeDiff(start, "testDoubleDataSetOld2() result = " + dataSet2.getY(0));
    }

    public void testDoubleDataSetNew1(final int nIterations, final boolean lock) {
        long start = ProcessingProfiler.getTimeStamp();

        if (lock) {
            // global lock, 'set' is already locked, so this should do nothing
            // in terms of thread-safety but is put in to assess the penalty of
            // locking
            dataSet1.lock().writeLock();
        }

        // reduce by x 2 since both X & Y are technically set
        // the other examples write/read only one value
        for (int iter = 0; iter < nIterations / 2; iter++) {
            // poor-man's rotate data through list
            final double tempX = dataSet1.getX(0);
            final double tempY = dataSet1.getY(0);
            for (int i = 1; i < N_DIM; i++) {
                dataSet1.set(i - 1, dataSet1.getX(i), dataSet1.getY(i));
            }
            dataSet1.set(dataSet1.getDataCount() - 1, tempX, tempY);
        }

        if (lock) {
            dataSet1.lock().writeUnLock();
        }

        // printout first element to avoid JIT optimisation
        ProcessingProfiler.getTimeDiff(start, "testDoubleDataSetNew1() result = " + dataSet1.getY(0));
    }

    public void testDoubleDataSetNew2(final int nIterations, final boolean lock) {
        long start = ProcessingProfiler.getTimeStamp();

        if (lock) {
            // global lock, 'set' is already locked, so this should do nothing
            // in terms of thread-safety but is put in to assess the penalty of
            // locking
            dataSet1.lock().writeLock();
        }

        // reduce by x 2 since both X & Y are technically set
        // the other examples write/read only one value
        for (int iter = 0; iter < nIterations / 2; iter++) {
            // poor-man's rotate data through list
            final double tempX = dataSet1.getXValues()[0];
            final double tempY = dataSet1.getYValues()[0];
            for (int i = 1; i < N_DIM; i++) {
                dataSet1.getXValues()[i - 1] = dataSet1.getXValues()[i];
                dataSet1.getYValues()[i - 1] = dataSet1.getYValues()[i];
            }
            dataSet1.getXValues()[N_DIM - 1] = tempX;
            dataSet1.getYValues()[N_DIM - 1] = tempY;
        }

        if (lock) {
            dataSet1.lock().writeUnLock();
        }

        // printout first element to avoid JIT optimisation
        ProcessingProfiler.getTimeDiff(start, "testDoubleDataSetNew2() result = " + dataSet1.getY(0));
    }

    //

    public void testFloatDataSetNew1(final int nIterations, final boolean lock) {
        long start = ProcessingProfiler.getTimeStamp();

        if (lock) {
            // global lock, 'set' is already locked, so this should do nothing
            // in terms of thread-safety but is put in to assess the penalty of
            // locking
            dataSet3.lock().writeLock();
        }

        // reduce by x 2 since both X & Y are technically set
        // the other examples write/read only one value
        for (int iter = 0; iter < nIterations / 2; iter++) {
            // poor-man's rotate data through list
            final double tempX = dataSet3.getX(0);
            final double tempY = dataSet3.getY(0);
            for (int i = 1; i < N_DIM; i++) {
                dataSet3.set(i - 1, dataSet3.getX(i), dataSet3.getY(i));
            }
            dataSet3.set(dataSet3.getDataCount() - 1, tempX, tempY);
        }

        if (lock) {
            dataSet3.lock().writeUnLock();
        }

        // printout first element to avoid JIT optimisation
        ProcessingProfiler.getTimeDiff(start, "testFloatDataSetNew1() result = " + dataSet3.getY(0));
    }

    public void testFloatDataSetNew2(final int nIterations, final boolean lock) {
        long start = ProcessingProfiler.getTimeStamp();

        if (lock) {
            // global lock, 'set' is already locked, so this should do nothing
            // in terms of thread-safety but is put in to assess the penalty of
            // locking
            dataSet3.lock().writeLock();
        }

        // reduce by x 2 since both X & Y are technically set
        // the other examples write/read only one value
        for (int iter = 0; iter < nIterations / 2; iter++) {
            // poor-man's rotate data through list
            final double tempX = dataSet3.getXFloatValues()[0];
            final double tempY = dataSet3.getYFloatValues()[0];
            for (int i = 1; i < N_DIM; i++) {
                dataSet3.getXFloatValues()[i - 1] = dataSet3.getXFloatValues()[i];
                dataSet3.getYFloatValues()[i - 1] = dataSet3.getYFloatValues()[i];
            }
            dataSet3.getXFloatValues()[N_DIM - 1] = (float) tempX;
            dataSet3.getYFloatValues()[N_DIM - 1] = (float) tempY;
        }

        if (lock) {
            dataSet3.lock().writeUnLock();
        }

        // printout first element to avoid JIT optimisation
        ProcessingProfiler.getTimeDiff(start, "testFloatDataSetNew2() result = " + dataSet3.getY(0));
    }

    public static void main(String[] args) {
        ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setDebugState(true);

        final int nIterations = 1000;
        FloatToDoubleBenchmarkSample benchmark = new FloatToDoubleBenchmarkSample();

        // benchmark.testMatrixVectorMultiplicationDouble(nIterations);
        // benchmark.testMatrixVectorMultiplicationFloat(nIterations);
        // benchmark.testMatrixVectorMultiplicationDouble(nIterations); //
        // repeat
        // benchmark.testMatrixVectorMultiplicationFloat(nIterations);

        System.err.println("\n\n\ndouble array access performance test:");
        final int nMul = 100;
        benchmark.testDoubleListNative(nMul * nIterations);
        benchmark.testDoubleArrayList1(nMul * nIterations);
        benchmark.testDoubleArrayList2(nMul * nIterations);
        benchmark.testDoubleArrayPlain(nMul * nIterations);
        benchmark.testDoubleDataSetOld1(nMul * nIterations,  false);
        benchmark.testDoubleDataSetOld2(nMul * nIterations,  false);
        benchmark.testDoubleDataSetNew1(nMul * nIterations,  false);
        benchmark.testDoubleDataSetNew2(nMul * nIterations,  false);
        benchmark.testFloatDataSetNew1(nMul * nIterations,  false);
        benchmark.testFloatDataSetNew2(nMul * nIterations,  false);
        System.out.println("");

        benchmark.testDoubleListNative(nMul * nIterations); // repeat #1
        benchmark.testDoubleArrayList1(nMul * nIterations);
        benchmark.testDoubleArrayList2(nMul * nIterations);
        benchmark.testDoubleArrayPlain(nMul * nIterations);
        benchmark.testDoubleDataSetOld1(nMul * nIterations,  false);
        benchmark.testDoubleDataSetOld2(nMul * nIterations,  false);
        benchmark.testDoubleDataSetNew1(nMul * nIterations,  false);
        benchmark.testDoubleDataSetNew2(nMul * nIterations,  false);
        benchmark.testFloatDataSetNew1(nMul * nIterations,  false);
        benchmark.testFloatDataSetNew2(nMul * nIterations,  false);

        System.out.println("");
        benchmark.testDoubleListNative(nMul * nIterations); // repeat #2
        benchmark.testDoubleArrayList1(nMul * nIterations);
        benchmark.testDoubleArrayList2(nMul * nIterations);
        benchmark.testDoubleArrayPlain(nMul * nIterations);
        benchmark.testDoubleDataSetOld1(nMul * nIterations,  false);
        benchmark.testDoubleDataSetOld2(nMul * nIterations,  false);
        benchmark.testDoubleDataSetNew1(nMul * nIterations,  false);
        benchmark.testDoubleDataSetNew2(nMul * nIterations,  false);
        benchmark.testFloatDataSetNew1(nMul * nIterations,  false);
        benchmark.testFloatDataSetNew2(nMul * nIterations,  false);

    }

}
