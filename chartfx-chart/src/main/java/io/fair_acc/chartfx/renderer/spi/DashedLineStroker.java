package io.fair_acc.chartfx.renderer.spi;

import javafx.scene.canvas.GraphicsContext;

/**
 * Strokes dashed polylines as individual lines to avoid rendering them via JavaFX's slow strokePolyLine.
 * The dash phase runs along the length of the line and restarts at the first point, like JavaFX's own dashes.
 *
 * TODO: this implementation was generated and needs more verification
 *
 * @author ennerf
 */
final class DashedLineStroker {

    /**
     * Strokes the polyline with the dashes and dash offset of the gc
     */
    static void strokeDashedLine(GraphicsContext gc, double width, double height, double[] x, double[] y, int length) {
        final double[] dashes = gc.getLineDashes();
        if (dashes == null) {
            for (int i = 1; i < length; i++) {
                gc.strokeLine(x[i - 1], y[i - 1], x[i], y[i]);
            }
            return;
        }
        gc.setLineDashes((double[]) null);
        try {
            strokeDashes(gc::strokeLine, dashes, gc.getLineDashOffset(), width, height, x, y, length);
        } finally {
            gc.setLineDashes(dashes);
        }
    }

    /**
     * Cuts the polyline into its on-pieces. Pieces outside the plot area plus one pattern period are not
     * stroked, but still advance the phase.
     */
    static void strokeDashes(LineStroker out, double[] dashes, double offset, double width, double height,
            double[] x, double[] y, int length) {
        final int numDashes = dashes.length % 2 == 0 ? dashes.length : 2 * dashes.length; // odd arrays repeat
        double period = 0;
        for (int k = 0; k < numDashes; k++) {
            period += dashes[k % dashes.length];
        }
        if (!(period > 0 && period < Double.POSITIVE_INFINITY) || !Double.isFinite(offset)) {
            return;
        }
        final double minX = -period, maxX = width + period;
        final double minY = -period, maxY = height + period;

        double phase = mod(offset, period); // position in the pattern at the start of the current segment
        for (int i = 1; i < length; i++) {
            final double x0 = x[i - 1], y0 = y[i - 1], x1 = x[i], y1 = y[i];
            final double dx = x1 - x0, dy = y1 - y0;
            final double segment = Math.hypot(dx, dy);
            if (!(segment < Double.POSITIVE_INFINITY)) {
                return; // non-finite points have no defined phase for the rest of the line
            }
            if (segment == 0) {
                if (length == 2 && isOn(dashes, numDashes, phase)) {
                    out.strokeLine(x0, y0, x1, y1);
                }
                continue;
            }

            // clip to the plot area (Liang-Barsky) to find the visible parameter range
            final double[] range = { 0, 1 };
            if (clip(-dx, x0 - minX, range) && clip(dx, maxX - x0, range)
                    && clip(-dy, y0 - minY, range) && clip(dy, maxY - y0, range)) {
                final double from = range[0] * segment, to = range[1] * segment;

                // find the dash that contains from
                double pos = mod(phase + from, period);
                int k = 0;
                while (k < numDashes - 1 && pos >= dashes[k % dashes.length]) {
                    pos -= dashes[k % dashes.length];
                    k++;
                }

                // stroke the on-pieces between from and to
                double start = from;
                double end = from + dashes[k % dashes.length] - pos;
                while (true) {
                    final double stop = Math.min(end, to);
                    if (k % 2 == 0 && stop > start) {
                        final double t0 = start / segment, t1 = stop / segment;
                        out.strokeLine(x0 + t0 * dx, y0 + t0 * dy, x0 + t1 * dx, y0 + t1 * dy);
                    }
                    if (!(end < to)) {
                        break;
                    }
                    start = end;
                    k = (k + 1) % numDashes;
                    end = start + dashes[k % dashes.length];
                }
            }
            phase = mod(phase + segment, period);
        }
    }

    private static boolean clip(double p, double q, double[] range) {
        if (p == 0) {
            return q >= 0;
        }
        final double t = q / p;
        if (p < 0) {
            if (t > range[1]) {
                return false;
            }
            range[0] = Math.max(range[0], t);
        } else {
            if (t < range[0]) {
                return false;
            }
            range[1] = Math.min(range[1], t);
        }
        return range[0] <= range[1];
    }

    private static boolean isOn(double[] dashes, int numDashes, double phase) {
        int k = 0;
        while (k < numDashes - 1 && phase >= dashes[k % dashes.length]) {
            phase -= dashes[k % dashes.length];
            k++;
        }
        return k % 2 == 0;
    }

    private static double mod(double value, double period) {
        final double result = value % period;
        return result < 0 ? result + period : result;
    }

    @FunctionalInterface
    interface LineStroker {
        void strokeLine(double x0, double y0, double x1, double y1);
    }

    private DashedLineStroker() {
        // utility class
    }

}
