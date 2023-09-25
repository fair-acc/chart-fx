package io.fair_acc.chartfx.renderer.spi.marchingsquares;

import static javafx.geometry.Side.BOTTOM;
import static javafx.geometry.Side.LEFT;
import static javafx.geometry.Side.RIGHT;
import static javafx.geometry.Side.TOP;

import javafx.geometry.Side;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An object that knows how to translate a Grid of Marching Squares Contour Cells into a Java AWT General Path.
 * </p>
 */
public class PathGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PathGenerator.class);
    private static final double EPSILON = 1E-7;

    PathGenerator() {
        super();
    }

    /**
     * <p>
     * Construct a GeneralPath representing the isoline, itself represented by a given Grid.
     * </p>
     * <p>
     * <b>IMPLEMENTATION NOTE:</b> This method is destructive. It alters the Grid instance as it generates the resulting
     * path. If the 'original' Grid instance is needed after invoking this method then it's the responsibility of the
     * caller to deep clone it before passing it here.
     * </p>
     *
     * @param grid the matrix of contour cells w/ side crossing coordinates already interpolated and normalized; i.e. in
     *        the range 0.0..1.0.
     * @return the geometries of a contour, including sub-path(s) for disjoint area final holes.
     */
    public GeneralPath generalPath(final Grid grid) {
        final GeneralPath result = new GeneralPath();
        for (int r = 0; r < grid.rowCount; r++) {
            for (int c = 0; c < grid.colCount; c++) {
                // find a start node...
                final Cell cell = grid.getCellAt(r, c);
                if (cell != null && !cell.isTrivial() && !cell.isSaddle()) {
                    // complete the [sub-]path and close it
                    update(grid, r, c, result);
                }
            }
        }
        return result;
    }

    /**
     * <p>
     * Find the side on which lies the next cell to use in a CCW traversal.
     * </p>
     *
     * @param cell the Cell to process.
     * @param prev previous side, only used for saddle cells.
     * @return side where the next cell is to be picked.
     */
    private Side nextSide(final Cell cell, final Side prev) {
        return secondSide(cell, prev);
    }

    /**
     * <p>
     * Return the second side that should be used in a CCW traversal.
     * </p>
     *
     * @param cell the Cell to process.
     * @param prev previous side, only used for saddle cells.
     * @return the 2nd side of the line segment of the designated cell.
     */
    private Side secondSide(final Cell cell, final Side prev) {
        switch (cell.getCellNdx()) {
        case 8:
        case 12:
        case 14:
            return LEFT;
        case 1:
        case 9:
        case 13:
            return BOTTOM;
        case 2:
        case 3:
        case 11:
            return RIGHT;
        case 4:
        case 6:
        case 7:
            return TOP;
        case 5:
            if (prev == null) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.atError().addArgument(cell).log("cell '{}' switch case 5, prev is null");
                }
                throw new IllegalStateException("cell " + cell + " prev is null");
            }
            switch (prev) {
            case LEFT:
                return cell.isFlipped() ? BOTTOM : TOP;
            case RIGHT:
                return cell.isFlipped() ? TOP : BOTTOM;
            default:
                final String m = "Saddle w/ no connected neighbour; Cell = " + cell + ", previous side = " + prev;
                throw new IllegalStateException(m);
            }
        case 10:
            if (prev == null) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.atError().addArgument(cell).log("cell '{}' switch case 5, prev is null");
                }
                throw new IllegalStateException("cell " + cell + " prev is null");
            }
            switch (prev) {
            case BOTTOM:
                return cell.isFlipped() ? RIGHT : LEFT;
            case TOP:
                return cell.isFlipped() ? LEFT : RIGHT;
            default:
                final String m = "Saddle w/ no connected neighbour; Cell = " + cell + ", previous side = " + prev;
                throw new IllegalStateException(m);
            }
        default:
            final String m = "Attempt to use a trivial Cell as a node: " + cell;
            throw new IllegalStateException(m);
        }
    }

    /**
     * <p>
     * A given contour can be made up of multiple disconnected regions, each potentially having multiple holes. Both
     * regions and holes are captured as individual sub-paths.
     * </p>
     * <p>
     * The process is iterative. It starts w/ an empty GeneralPath instance and continues until all Cells are processed.
     * With every invocation the GeneralPath object is updated to reflect the new sub-path(s).
     * </p>
     * <p>
     * Once a non-saddle cell is used it is cleared so as to ensure it will not be re-used when finding sub-paths w/in
     * the ofinal l path.
     * </p>
     * final * @param grid on input the matrix of cells representinfinal ven contour. Note that the process will alter
     * the Cells, so on output the original Grid instance _will_ be modified. In other words this method is NOT
     * idempotent when using the same object references and values.
     *
     * @param grid the grid
     * @param rowIndex row index of the start Cell.
     * @param clumnIndex column index of the start Cell.
     * @param path a non-null GeneralPath instance to update.
     */
    private void update(final Grid grid, final int rowIndex, final int clumnIndex, final GeneralPath path) {
        Side prevSide = null; // was: NONE
        int r = rowIndex;
        int c = clumnIndex;
        final Cell start = grid.getCellAt(r, c);
        float[] pt = start.getXY(PathGenerator.firstSide(start, prevSide));
        float x = c + pt[0]; // may throw NPE
        float y = r + pt[1]; // likewise
        path.moveTo(x, y); // prepare for a new sub-path

        pt = start.getXY(secondSide(start, prevSide));
        float xPrev = c + pt[0];
        float yPrev = r + pt[1];

        prevSide = nextSide(start, prevSide);
        switch (prevSide) {
        case BOTTOM:
            r--;
            break;
        case LEFT:
            c--;
            break;
        case RIGHT:
            c++;
            break;
        case TOP:
            r++; // fall through
            break;
        default:
            break;
        }
        start.clear();

        Cell currentCell = grid.getCellAt(r, c);
        while (!start.equals(currentCell)) { // we want object reference equality
            pt = currentCell.getXY(secondSide(currentCell, prevSide));
            x = c + pt[0];
            y = r + pt[1];
            if (Math.abs(x - xPrev) > PathGenerator.EPSILON && Math.abs(y - yPrev) > PathGenerator.EPSILON) {
                path.lineTo(x, y);
            }
            xPrev = x;
            yPrev = y;
            prevSide = nextSide(currentCell, prevSide);
            switch (prevSide) {
            case BOTTOM:
                r--;
                break;
            case LEFT:
                c--;
                break;
            case RIGHT:
                c++;
                break;
            case TOP:
                r++;
                break;
            default:
                // System.out.println(
                // "update: Potential loop! Current cell = " + currentCell + ", previous side = " + prevSide);
                break;
            }
            currentCell.clear();
            currentCell = grid.getCellAt(r, c);
        }

        path.closePath();
    }

    /**
     * <p>
     * Return the first side that should be used in a CCW traversal.
     * </p>
     *
     * @param cell the Cell to process.
     * @param prev previous side, only used for saddle cells.
     * @return the 1st side of the line segment of the designated cell.
     */
    private static Side firstSide(final Cell cell, final Side prev) {
        switch (cell.getCellNdx()) {
        case 1:
        case 3:
        case 7:
            return LEFT;
        case 2:
        case 6:
        case 14:
            return BOTTOM;
        case 4:
        case 11:
        case 12:
        case 13:
            return RIGHT;
        case 8:
        case 9:
            return TOP;
        case 5:
            if (prev == null) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.atError().addArgument(cell).log("cell '{}' switch case 5, prev is null");
                }
                throw new IllegalStateException("cell " + cell + " prev is null");
            }
            switch (prev) {
            case LEFT:
                return RIGHT;
            case RIGHT:
                return LEFT;
            default:
                throw new NoSaddlePointException(cell, prev);
            }
        case 10:
            if (prev == null) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.atError().addArgument(cell).log("cell '{}' switch case 5, prev is null");
                }
                throw new IllegalStateException("cell " + cell + " prev is null");
            }
            switch (prev) {
            case BOTTOM:
                return TOP;
            case TOP:
                return BOTTOM;
            default:
                throw new NoSaddlePointException(cell, prev);
            }
        default:
            final String m = "Attempt to use a trivial cell as a start node: " + cell;
            throw new IllegalStateException(m);
        }
    }

    protected static class NoSaddlePointException extends IllegalStateException {
        private static final long serialVersionUID = -5628254997299110176L;

        NoSaddlePointException(final Cell cell, final Side prev) {
            super("Saddle w/ no connected neighbour; Cell = " + cell + ", previous side = " + prev);
        }
    }
}
