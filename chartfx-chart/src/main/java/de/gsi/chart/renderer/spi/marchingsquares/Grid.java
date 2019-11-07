package de.gsi.chart.renderer.spi.marchingsquares;

/**
 * <p>
 * Given a two-dimensional scalar field (rectangular array of individual numerical values) the Marching Squares
 * algorithm applies a <em>threshold</em> (a.k.a contour level or isovalue) to make a binary image containing:
 * </p>
 * <ul>
 * <li>1 where the data value is above the isovalue,</li>
 * <li>0 where the data value is below the isovalue.</li>
 * </ul>
 * <p>
 * Every 2x2 block of pixels in the binary image forms a contouring cell, so the whole image is represented by a grid of
 * such cells (shown in green in the picture below). Note that this contouring grid is one cell smaller in each
 * direction than the original 2D data field.
 * </p>
 */
class Grid {
    private final Cell[][] cells;
    public final int rowCount;
    public final int colCount;
    private final double threshold;
    private String str;

    Grid(final Cell[][] cells, final double threshold) {
        super();
        this.cells = cells;
        rowCount = cells.length;
        colCount = cells[0].length;
        this.threshold = threshold;
    }

    public Cell getCellAt(final int r, final int c) {
        return cells[r][c];
    }

    public int getCellNdxAt(final int r, final int c) {
        final Cell cell = cells[r][c];
        if (cell == null) {
            return 0;
        }
        return cells[r][c].getCellNdx();
    }

    @Override
    public String toString() {
        if (str == null) {
            str = "Grid{rowCount=" + rowCount + ", colCount=" + colCount + ", threshold=" + threshold + '}';
        }
        return str;
    }

}
