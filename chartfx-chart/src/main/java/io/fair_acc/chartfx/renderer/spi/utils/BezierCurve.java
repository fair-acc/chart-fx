package io.fair_acc.chartfx.renderer.spi.utils;

/**
 * small tool class to calculate Bezier Curve control points see: https://en.wikipedia.org/wiki/B%C3%A9zier_curve
 *
 * @author rstein
 */
public final class BezierCurve {

    private BezierCurve() {
        // private math class
    }

    public static void calcCurveControlPoints(final double[] xData, final double[] yData, double[] xControlPoint1,
            double[] yControlPoint1, double[] xControlPoint2, double[] yControlPoint2, int length) {
        final int n = length - 1;
        if (n == 1) { // Special case: Bezier curve should be a straight line.
            // 3P1 = 2P0 + P3
            xControlPoint1[0] = (2 * xData[0] + xData[1]) / 3;
            yControlPoint1[0] = (2 * yData[0] + yData[1]) / 3;

            // P2 = 2P1 – P0
            xControlPoint2[0] = 2 * xControlPoint1[0] - xData[0];
            yControlPoint2[0] = 2 * yControlPoint1[0] - yData[0];
            return;
        }

        // Calculate first Bezier control points
        // Right hand side vector
        final double[] rhs = new double[n];

        // Set right hand side X values
        for (int i = 1; i < n - 1; ++i) {
            rhs[i] = 4 * xData[i] + 2 * xData[i + 1];
        }
        rhs[0] = xData[0] + 2 * xData[1];
        rhs[n - 1] = (8 * xData[n - 1] + xData[n]) / 2.0;
        final double[] x = BezierCurve.getFirstControlPoints(rhs);

        // Set right hand side Y values
        for (int i = 1; i < n - 1; ++i) {
            rhs[i] = 4 * yData[i] + 2 * yData[i + 1];
        }
        rhs[0] = yData[0] + 2 * yData[1];
        rhs[n - 1] = (8 * yData[n - 1] + yData[n]) / 2.0;
        // Get first control points Y-values
        final double[] y = BezierCurve.getFirstControlPoints(rhs);

        // Fill output arrays.

        // First control points
        System.arraycopy(x, 0, xControlPoint1, 0, n);
        System.arraycopy(y, 0, yControlPoint1, 0, n);

        for (int i = 0; i < n; ++i) {
            // Second control point
            if (i < n - 1) {
                xControlPoint2[i] = 2 * xData[i + 1] - x[i + 1];
                yControlPoint2[i] = 2 * yData[i + 1] - y[i + 1];
            } else {
                xControlPoint2[i] = (xData[n] + x[n - 1]) / 2;
                yControlPoint2[i] = (yData[n] + y[n - 1]) / 2;
            }
        }
    }

    private static double[] getFirstControlPoints(double[] rhs) {
        final int n = rhs.length;
        final double[] x = new double[n]; // Solution vector.
        final double[] tmp = new double[n]; // Temp workspace.
        double b = 2.0;

        x[0] = rhs[0] / b;

        for (int i = 1; i < n; i++) {// Decomposition and forward substitution.
            tmp[i] = 1 / b;
            b = (i < n - 1 ? 4.0 : 3.5) - tmp[i];
            x[i] = (rhs[i] - x[i - 1]) / b;
        }
        for (int i = 1; i < n; i++) {
            x[n - i - 1] -= tmp[n - i] * x[n - i]; // Backsubstitution.
        }
        return x;
    }

}
