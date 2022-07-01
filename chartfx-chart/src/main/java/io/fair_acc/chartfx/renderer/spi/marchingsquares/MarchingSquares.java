package io.fair_acc.chartfx.renderer.spi.marchingsquares;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.fair_acc.dataset.utils.CachedDaemonThreadFactory;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * <p>
 * Implementation of the Marching Squares algorithm described in: {@code https://en.wikipedia.org/wiki/Marching_squares}
 * </p>
 */
public class MarchingSquares {
    private static final ExecutorService ES = Executors.newCachedThreadPool(CachedDaemonThreadFactory.getInstance());
    private double[] isovalues;

    public GeneralPath[] buildContours(final double[][] data, final double[] levels)
            throws InterruptedException, ExecutionException {
        final long start = ProcessingProfiler.getTimeStamp();
        // find min, max, and guard
        double min = +Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        final int rowCount = data.length;
        final int colCount = data[0].length;
        double here;
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                here = data[i][j];
                min = Math.min(min, here);
                max = Math.max(max, here);
            }
        }

        isovalues = new double[levels.length];
        System.arraycopy(levels, 0, isovalues, 0, levels.length);

        GeneralPath[] result = null;
        if (min == max) {
            final String m = "All values are equal. Cannot build contours for a constant field";
            throw new IllegalArgumentException(m);
        }

        // IMPORTANT: pad data to ensure resulting linear strings are closed
        final double guard = min - 1;
        final double[][] padded = MarchingSquares.pad(data, guard);

        result = doConcurrent(padded);

        ProcessingProfiler.getTimeDiff(start, "built " + levels.length + " contours");
        return result;
    }

    private GeneralPath[] doConcurrent(final double[][] data) throws InterruptedException, ExecutionException {
        final Collection<Callable<Result>> workers = new ArrayList<>();
        for (int i = 0; i < isovalues.length; i++) {
            workers.add(new Task(i, data, isovalues[i]));
        }

        final List<Future<Result>> jobs = MarchingSquares.ES.invokeAll(workers);
        final GeneralPath[] result = new GeneralPath[isovalues.length];
        for (final Future<Result> future : jobs) {
            final Result r = future.get();
            result[r.ndx] = r.path;
        }
        return result;
    }

    private static Grid contour(final double[][] data, final double isovalue) {
        final int rowCount = data.length;
        final int colCount = data[0].length;

        // Every 2x2 block of pixels in the binary image forms a contouring cell,
        // so the whole image is represented by a grid of such cells. Note that
        // this contouring grid is one cell smaller in each direction than the
        // original 2D field.
        final Cell[][] cells = new Cell[rowCount - 1][colCount - 1];
        for (int r = 0; r < rowCount - 1; r++) {
            for (int c = 0; c < colCount - 1; c++) {
                // Compose the 4 bits at the corners of the cell to build a binary
                // index: walk around the cell in a clockwise direction appending
                // the bit to the index, using bitwise OR and left-shift, from most
                // significant bit at the top left, to least significant bit at the
                // bottom left. The resulting 4-bit index can have 16 possible
                // values in the range 0-15.
                int ndx = 0;
                final double tl = data[r + 1][c];
                final double tr = data[r + 1][c + 1];
                final double br = data[r][c + 1];
                final double bl = data[r][c];
                ndx |= tl > isovalue ? 0 : 8;
                ndx |= tr > isovalue ? 0 : 4;
                ndx |= br > isovalue ? 0 : 2;
                ndx |= bl > isovalue ? 0 : 1;
                boolean flipped = false;
                if (ndx == 5 || ndx == 10) {
                    // resolve the ambiguity by using the average data value for the
                    // center of the cell to choose between different connections of
                    // the interpolated points.
                    final double center = (tl + tr + br + bl) / 4;
                    if (ndx == 5 && center < isovalue) {
                        flipped = true;
                    } else if (ndx == 10 && center < isovalue) {
                        flipped = true;
                    }
                }
                // to node: 'rsn' - we only populate the grid without non-trivial cells;
                // i.e. those w/ an index different than 0 and 15.
                if (ndx != 0 && ndx != 15) {
                    // Apply linear interpolation between the original field data
                    // values to find the exact position of the contour line along
                    // the edges of the cell.
                    float left = 0.5F;
                    float top = 0.5F;
                    float right = 0.5F;
                    float bottom = 0.5F;
                    switch (ndx) {
                    case 1:
                    case 14:
                        left = (float) ((isovalue - bl) / (tl - bl));
                        bottom = (float) ((isovalue - bl) / (br - bl));
                        break;
                    case 2:
                    case 13:
                        bottom = (float) ((isovalue - bl) / (br - bl));
                        right = (float) ((isovalue - br) / (tr - br));
                        break;
                    case 3:
                    case 12:
                        left = (float) ((isovalue - bl) / (tl - bl));
                        right = (float) ((isovalue - br) / (tr - br));
                        break;
                    case 4:
                    case 11:
                        top = (float) ((isovalue - tl) / (tr - tl));
                        right = (float) ((isovalue - br) / (tr - br));
                        break;
                    case 5:
                    case 10:
                        left = (float) ((isovalue - bl) / (tl - bl));
                        bottom = (float) ((isovalue - bl) / (br - bl));
                        top = (float) ((isovalue - tl) / (tr - tl));
                        right = (float) ((isovalue - br) / (tr - br));
                        break;
                    case 6:
                    case 9:
                        bottom = (float) ((isovalue - bl) / (br - bl));
                        top = (float) ((isovalue - tl) / (tr - tl));
                        break;
                    case 7:
                    case 8:
                        left = (float) ((isovalue - bl) / (tl - bl));
                        top = (float) ((isovalue - tl) / (tr - tl));
                        break;
                    default: // shouldn't happen
                        final String m = "Unexpected cell index " + ndx;
                        throw new IllegalStateException(m);
                    }

                    cells[r][c] = new Cell(ndx, flipped, left, top, right, bottom);
                }
            }
        }
        final Grid result = new Grid(cells, isovalue);
        return result;
    }

    /**
     * <p>
     * Pad data with a given 'guard' value.
     * </p>
     *
     * @param data matrix to pad.
     * @param guard the value to use for padding. It's expected to be less than the minimum of all data cell values.
     * @return the resulting padded matrix which will be larger by 2 in both directions.
     */
    private static double[][] pad(final double[][] data, final double guard) {
        final int rowCount = data.length;
        final int colCount = data[0].length;
        final double[][] result = new double[rowCount + 2][colCount + 2];

        // top and bottom rows
        for (int j = 0; j < colCount + 2; j++) {
            result[0][j] = guard;
            result[rowCount + 1][j] = guard;
        }

        // left- and right-most columns excl. top and bottom rows
        for (int i = 1; i < rowCount + 1; i++) {
            result[i][0] = guard;
            result[i][colCount + 1] = guard;
        }

        // the middle
        for (int i = 0; i < rowCount; i++) {
            System.arraycopy(data[i], 0, result[i + 1], 1, colCount);
        }

        return result;
    }

    private static final class Result {
        private final int ndx;
        private final GeneralPath path;
        private String str;

        Result(final int ndx, final GeneralPath path) {
            super();
            this.ndx = ndx;
            this.path = path;
        }

        @Override
        public String toString() {
            if (str == null) {
                str = "Result{ndx=" + ndx + ", bbox=" + '}';
            }
            return str;
        }
    }

    private static final class Task implements Callable<Result> {
        private final int ndx;
        private final double[][] data;
        private final double level;

        Task(final int ndx, final double[][] data, final double level) {
            super();
            this.ndx = ndx;
            this.data = data;
            this.level = level;
        }

        @Override
        public Result call() throws Exception {
            GeneralPath path = null;
            try {
                path = new PathGenerator().generalPath(MarchingSquares.contour(data, level));
            } catch (final Exception x) {
                final String m = "Failed making contour at index #" + ndx + " for level " + level + ": "
                                 + x.getLocalizedMessage();
                throw new IllegalArgumentException(m, x);
            }
            return new Result(ndx, path);
        }
    }
}
