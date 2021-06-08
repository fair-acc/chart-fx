package de.gsi.math;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.Histogram.Boundary.LOWER;
import static de.gsi.dataset.Histogram.Boundary.UPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.AbstractHistogram;
import de.gsi.dataset.spi.Histogram;
import de.gsi.dataset.testdata.spi.AbstractTestFunction;
import de.gsi.dataset.testdata.spi.GaussFunction;
import de.gsi.dataset.testdata.spi.TriangleFunction;

/**
 * Unit-Tests of #de.gsi.math.DataSetMath
 *
 * N.B. to be extended by kind volunteers
 *
 * @author rstein
 */
class DataSetMathTests {
    private static final int N_SAMPLES = 10;
    interface TestFunction {
        void apply(int index, double a, double b) throws Throwable;
    }

    private static class IdentityFunction extends AbstractTestFunction<IdentityFunction> {
        private final double value;
        private final double timeOffset;

        private IdentityFunction(final String name, final int count, final double value, final double timeOffset) {
            super(name, count);
            this.value = value;
            this.timeOffset = timeOffset;
            update();
        }

        @Override
        public double[] generateY(final int count) {
            final double[] retVal = new double[count];
            Arrays.fill(retVal, value);
            return retVal;
        }

        @Override
        public double get(final int dimIndex, final int index) {
            return dimIndex == DIM_X ? index + timeOffset : super.get(DIM_Y, index);
        }
    }

    @Test
    void testCommonBaseFunction() {
        final TriangleFunction refFunction1 = new TriangleFunction("triag", N_SAMPLES);
        List<Double> base = DataSetMath.getCommonBase(refFunction1);
        for (int i = 0; i < refFunction1.getDataCount(); i++) {
            final double x = refFunction1.get(DIM_X, i);
            assertTrue(base.contains(x), "x index not in list: " + x);
        }

        final TriangleFunction refFunction2 = new TriangleFunction("triag", 2 * N_SAMPLES);
        base = DataSetMath.getCommonBase(refFunction1, refFunction2);
        for (int i = 0; i < refFunction1.getDataCount(); i++) {
            final double x = refFunction1.get(DIM_X, i);
            assertTrue(base.contains(x), "x index not in list: " + x);
        }

        for (int i = 0; i < refFunction2.getDataCount(); i++) {
            final double x = refFunction2.get(DIM_X, i);
            assertTrue(base.contains(x), "x index not in list: " + x);
        }

        final Histogram refFunction3 = new Histogram("hist", N_SAMPLES, 0, N_SAMPLES, AbstractHistogram.HistogramOuterBounds.BINS_ALIGNED_WITH_BOUNDARY);
        base = DataSetMath.getCommonBase(refFunction1, refFunction3);
        for (int i = 0; i < refFunction3.getDataCount() - 1; i++) {
            final double x = refFunction3.get(DIM_X, i);
            final double xL = refFunction3.getBinLimits(DIM_X, LOWER, i + 1);
            final double xC = refFunction3.getBinCenter(DIM_X, i + 1);
            final double xR = refFunction3.getBinLimits(DIM_X, UPPER, i + 1);
            assertTrue(base.contains(x), "x index not in list: " + x);
            assertTrue(base.contains(xL), "xL index not in list: " + xL);
            assertTrue(base.contains(xC), "xC index not in list: " + xC);
            assertTrue(base.contains(xR), "xR index not in list: " + xR);
        }
    }

    @Test
    void basicMathOpsFunctionValueTests() { // NOSONAR NOPMD -- excessive assertions -- is a necessity of unit-tests ;-)
        final TriangleFunction refFunction = new TriangleFunction("triag", N_SAMPLES);
        DataSet returnFunction;

        returnFunction = DataSetMath.addFunction(refFunction, 1.0);
        testFunctionStrictBase("addFunction(DataSet, double)", refFunction, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 + 1, y2));

        returnFunction = DataSetMath.subtractFunction(refFunction, 1.0);
        testFunctionStrictBase("subtractFunction(DataSet, double)", refFunction, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 - 1, y2));

        returnFunction = DataSetMath.multiplyFunction(refFunction, 2.0);
        testFunctionStrictBase("multiplyFunction(DataSet, double)", refFunction, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 * 2, y2));

        returnFunction = DataSetMath.divideFunction(refFunction, 2.0);
        testFunctionStrictBase("divideFunction(DataSet, double)", refFunction, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 / 2, y2));

        returnFunction = DataSetMath.sqrFunction(refFunction, 1.0);
        testFunctionStrictBase("sqrFunction(DataSet, double)", refFunction, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(Math.sqr(y1 + 1.0), y2));

        returnFunction = DataSetMath.sqrtFunction(refFunction, 1.0);
        testFunctionStrictBase("sqrtFunction(DataSet, double)", refFunction, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(Math.sqrt(y1 + 1.0), y2));

        returnFunction = DataSetMath.log10Function(refFunction);
        testFunctionStrictBase("log10Function(DataSet, double)", refFunction, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(10 * MathBase.log10(y1 + 0.0), y2));

        returnFunction = DataSetMath.dbFunction(refFunction);
        testFunctionStrictBase("dbFunction(DataSet, double)", refFunction, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(20 * MathBase.log10(y1 + 0.0), y2));

        returnFunction = DataSetMath.inversedbFunction(refFunction);
        testFunctionStrictBase("inversedbFunction(DataSet, double)", refFunction, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(Math.pow(10, y1 / 20), y2));

        returnFunction = DataSetMath.mathFunction(refFunction, 1.0, DataSetMath.MathOp.IDENTITY);
        testFunctionStrictBase("mathFunction(DataSet, double, IDENTITY)", refFunction, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1, y2));
    }

    @Test
    void basicMathOpsFunctionFunctionTests() { // NOSONAR NOPMD -- excessive assertions -- is a necessity of unit-tests ;-)
        final TriangleFunction refFunction1 = new TriangleFunction("triag", 10);
        final IdentityFunction refFunction2 = new IdentityFunction("identity", 10, 2.0, 0.0);
        DataSet returnFunction;

        returnFunction = DataSetMath.addFunction(refFunction1, refFunction2);
        testFunctionStrictBase("addFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 + 2.0, y2));

        returnFunction = DataSetMath.subtractFunction(refFunction1, refFunction2);
        testFunctionStrictBase("subtractFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 - 2.0, y2));

        returnFunction = DataSetMath.multiplyFunction(refFunction1, refFunction2);
        testFunctionStrictBase("multiplyFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 * 2.0, y2));

        returnFunction = DataSetMath.divideFunction(refFunction1, refFunction2);
        testFunctionStrictBase("divideFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 / 2.0, y2));

        returnFunction = DataSetMath.sqrFunction(refFunction1, refFunction2);
        testFunctionStrictBase("sqrFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(Math.sqr(y1 + 2.0), y2));

        returnFunction = DataSetMath.sqrtFunction(refFunction1, refFunction2);
        testFunctionStrictBase("sqrtFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(Math.sqrt(y1 + 2.0), y2));

        returnFunction = DataSetMath.log10Function(refFunction1, refFunction2);
        testFunctionStrictBase("log10Function(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(10 * MathBase.log10(y1 + 2.0), y2));

        returnFunction = DataSetMath.dbFunction(refFunction1, refFunction2);
        testFunctionStrictBase("dbFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(20 * MathBase.log10(y1 + 2.0), y2));

        returnFunction = DataSetMath.mathFunction(refFunction1, refFunction2, DataSetMath.MathOp.IDENTITY);
        testFunctionStrictBase("mathFunction(DataSet, DataSet, IDENTITY)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 + 2.0, y2));
    }

    @Test
    void basicMathOpsFunctionTimeOffsetFunctionTests() { // NOSONAR NOPMD -- excessive assertions -- is a necessity of unit-tests ;-)
        final TriangleFunction refFunction1 = new TriangleFunction("triag", 10);
        final IdentityFunction refFunction2 = new IdentityFunction("identity", 10, 2.0, 0.5);
        DataSet returnFunction;

        returnFunction = DataSetMath.addFunction(refFunction1, refFunction2);
        testFunctionInterpolatedBase("addFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 + 2.0, y2));

        returnFunction = DataSetMath.subtractFunction(refFunction1, refFunction2);
        testFunctionInterpolatedBase("subtractFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 - 2.0, y2));

        returnFunction = DataSetMath.multiplyFunction(refFunction1, refFunction2);
        testFunctionInterpolatedBase("multiplyFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 * 2.0, y2));

        returnFunction = DataSetMath.divideFunction(refFunction1, refFunction2);
        testFunctionInterpolatedBase("divideFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 / 2.0, y2));

        returnFunction = DataSetMath.sqrFunction(refFunction1, refFunction2);
        testFunctionInterpolatedBase("sqrFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(Math.sqr(y1 + 2.0), y2));

        returnFunction = DataSetMath.sqrtFunction(refFunction1, refFunction2);
        testFunctionInterpolatedBase("sqrtFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(Math.sqrt(y1 + 2.0), y2));

        returnFunction = DataSetMath.log10Function(refFunction1, refFunction2);
        testFunctionInterpolatedBase("log10Function(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(10 * MathBase.log10(y1 + 2.0), y2));

        returnFunction = DataSetMath.dbFunction(refFunction1, refFunction2);
        testFunctionInterpolatedBase("dbFunction(DataSet, DataSet)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(20 * MathBase.log10(y1 + 2.0), y2));

        returnFunction = DataSetMath.mathFunction(refFunction1, refFunction2, DataSetMath.MathOp.IDENTITY);
        testFunctionInterpolatedBase("mathFunction(DataSet, DataSet, IDENTITY)", refFunction1, returnFunction, (i, x1, x2) -> assertEquals(x1, x2), (i, y1, y2) -> assertEquals(y1 + 2.0, y2));
    }

    void testFunctionStrictBase(final String testName, final DataSet refFunction, final DataSet testFunction, final TestFunction xValueCheck, final TestFunction yValueCheck) {
        assertEquals(refFunction.getDataCount(), testFunction.getDataCount());

        for (int i = 0; i < refFunction.getDataCount(); i++) {
            final double x1 = refFunction.get(DIM_X, i);
            final double x2 = testFunction.get(DIM_X, i);
            final double y1 = refFunction.get(DIM_Y, i);
            final double y2 = testFunction.get(DIM_Y, i);
            try {
                xValueCheck.apply(i, x1, x2);
            } catch (Throwable e) {
                throw new AssertionFailedError("Test: '" + testName + "' exception: " + e.getMessage() + " for x-test index: " + i, e);
            }
            try {
                yValueCheck.apply(i, y1, y2);
            } catch (Throwable e) {
                throw new AssertionFailedError("Test: '" + testName + "' exception: " + e.getMessage() + " for y-test index: " + i, e);
            }
        }
    }

    void testFunctionInterpolatedBase(final String testName, final DataSet refFunction, final DataSet testFunction, final TestFunction xValueCheck, final TestFunction yValueCheck) {
        List<Double> base = DataSetMath.getCommonBase(refFunction, testFunction);
        int count = 0;
        for (double x : base) {
            final double y1 = refFunction.getValue(DIM_Y, x);
            final double y2 = testFunction.getValue(DIM_Y, x);
            try {
                xValueCheck.apply(count, x, x);
            } catch (Throwable e) {
                throw new AssertionFailedError("Test: '" + testName + "' exception: " + e.getMessage() + " for x-test index: " + count + " x = " + x, e);
            }
            try {
                yValueCheck.apply(count, y1, y2);
            } catch (Throwable e) {
                throw new AssertionFailedError("Test: '" + testName + "' exception: " + e.getMessage() + " for y-test index: " + count + " x = " + x, e);
            }
            count++;
        }
    }

    @Test
    void testIntegralWidthEstimator() {
        final int count = 1000;
        final double maxHalfWidth = count / 2.0;
        GaussFunction gaussFunction = new GaussFunction("testGauss", count, 500, 100.0);
        assertEquals(500, SimpleDataSetEstimators.computeCentreOfMass(gaussFunction), 0.001, "centre-of-mass check");
        assertEquals(100.0, DataSetMath.integrateFromCentre(gaussFunction, Double.NaN, maxHalfWidth, false).getAxisDescription(DIM_Y).getMax(), 0.01, "integral max");
        assertEquals(1.0, DataSetMath.integrateFromCentre(gaussFunction, Double.NaN, maxHalfWidth, true).getAxisDescription(DIM_Y).getMax(), 0.01, "integral max");

        assertEquals(100, DataSetMath.integralWidth(gaussFunction, Double.NaN, maxHalfWidth, 0.6827), 1, "1 sigma equivalency");
        assertEquals(200, DataSetMath.integralWidth(gaussFunction, Double.NaN, maxHalfWidth, 0.9545), 1, "2 sigma equivalency");
        assertEquals(300, DataSetMath.integralWidth(gaussFunction, Double.NaN, maxHalfWidth, 0.9973), 1, "3 sigma equivalency");

        // test error cases
        assertEquals(0, DataSetMath.integrateFromCentre(new GaussFunction("zeroGauss", 0), Double.NaN, 100, true).getDataCount());
        assertEquals(0, DataSetMath.integrateFromCentre(new GaussFunction("zeroGauss", 2), Double.NaN, -1, true).getDataCount());
        assertThrows(IllegalArgumentException.class, () -> DataSetMath.integrateFromCentre(new GaussFunction("zeroGauss", 2), -1, 2, true));
        assertThrows(IllegalArgumentException.class, () -> DataSetMath.integrateFromCentre(new GaussFunction("zeroGauss", 2), 0, 2, true));
        assertThrows(IllegalArgumentException.class, () -> DataSetMath.integrateFromCentre(new GaussFunction("zeroGauss", 2), 2, 2, true));
        assertThrows(IllegalArgumentException.class, () -> DataSetMath.integrateFromCentre(new GaussFunction("zeroGauss", 2), 3, 2, true));
    }
}
