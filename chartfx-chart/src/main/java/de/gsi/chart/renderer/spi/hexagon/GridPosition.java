package de.gsi.chart.renderer.spi.hexagon;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores coordinates and has functions for grid calculations, e.g. getLine, ring and distance. These calculations do
 * not depend on how you have placed the Hexagons on the HexagonMap. The axial coordinate system is used.
 */
public class GridPosition implements Cloneable, Serializable {

    private static final long serialVersionUID = -6932865381701419097L;

    /**
     * The Axial Q coordinate
     */
    protected final int q;

    /**
     * The Axial R coordinate
     */
    protected final int r;

    /**
     * @param q the axial Q coordinate
     * @param r the axial R coordinate
     */
    public GridPosition(final int q, final int r) {
        this.q = q;
        this.r = r;
    }

    public int getQ() {
        return q;
    }

    public int getR() {
        return r;
    }

    /**
     * Finds the adjacent position in the specified direction from this position
     *
     * @param direction in which to search
     * @return the adjacent position
     */
    public GridPosition getNeighborPosition(final HexagonMap.Direction direction) {
        final int i = GridPosition.getNumberFromDirection(direction);
        final int[][] neighbors = new int[][] { { 0, -1 }, { +1, -1 }, { +1, 0 }, { 0, +1 }, { -1, +1 }, { -1, 0 } };
        final int[] d = neighbors[i];
        return new GridPosition(q + d[0], r + d[1]);
    }

    /**
     * Finds all positions that are on the edge of a circle in which this position is the centre. If radius is 0, an
     * array with only this GridPosition will be returned.
     *
     * @param radius circle radius
     * @return positions that are on the edge of a circle
     */
    ArrayList<GridPosition> getPositionsOnCircleEdge(final int radius) {
        final ArrayList<GridPosition> result = new ArrayList<>();
        if (radius == 0) {
            result.add(this);
            return result;
        } else {
            GridPosition h = this;
            for (int i = 0; i < radius; i++) {
                h = h.getNeighborPosition(HexagonMap.Direction.SOUTHWEST);
            }
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < radius; j++) {
                    result.add(h.clone());
                    h = h.getNeighborPosition(GridPosition.getDirectionFromNumber(i));
                }
            }
            return result;
        }
    }

    ArrayList<GridPosition> getPositionsInCircleArea(final int radius) {
        final ArrayList<GridPosition> result = new ArrayList<>();
        for (int i = 0; i <= radius; i++) {
            final ArrayList<GridPosition> positions = getPositionsOnCircleEdge(i);
            result.addAll(positions);
        }
        return result;
    }

    private static int getNumberFromDirection(final HexagonMap.Direction direction) {
        switch (direction) {
        case NORTHWEST:
            return 0;
        case NORTHEAST:
            return 1;
        case EAST:
            return 2;
        case SOUTHEAST:
            return 3;
        case SOUTHWEST:
            return 4;
        case WEST:
            return 5;
        }
        throw new RuntimeException();
    }

    static HexagonMap.Direction getDirectionFromNumber(final int i) {
        switch (i) {
        case 0:
            return HexagonMap.Direction.NORTHWEST;
        case 1:
            return HexagonMap.Direction.NORTHEAST;
        case 2:
            return HexagonMap.Direction.EAST;
        case 3:
            return HexagonMap.Direction.SOUTHEAST;
        case 4:
            return HexagonMap.Direction.SOUTHWEST;
        case 5:
            return HexagonMap.Direction.WEST;
        }
        throw new RuntimeException();
    }

    String getCoordinates() {
        final String s = q + ", " + r;
        return s;
    }

    /**
     * Finds the position that best matches given non-integer coordinates
     *
     * @param q coordinate
     * @param r coordinate
     * @return position that best matches given non-integer coordinates
     */
    public static GridPosition hexRound(final double q, final double r) {
        final double cubeX = q;
        final double cubeY = r;
        final double cubeZ = -cubeX - cubeY;

        long rx = Math.round(cubeX);
        long ry = Math.round(cubeY);
        long rz = Math.round(cubeZ);

        final double x_diff = Math.abs(rx - cubeX);
        final double y_diff = Math.abs(ry - cubeY);
        final double z_diff = Math.abs(rz - cubeZ);

        if (x_diff > y_diff && x_diff > z_diff) {
            rx = -ry - rz;
        } else if (y_diff > z_diff) {
            ry = -rx - rz;
        } else {
            rz = -rx - ry;
        }
        return new GridPosition((int) rx, (int) ry);
    }

    @Override
    protected GridPosition clone() {
        try {
            return (GridPosition) super.clone();
        } catch (final CloneNotSupportedException ex) {
            Logger.getLogger(GridPosition.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * Two positions are equal if they have the same q and r
     *
     * @param obj object to compare to
     * @return positions are equal if they have the same q and r
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass().equals(this.getClass())) {
            final GridPosition gridPositionObj = (GridPosition) obj;
            if (gridPositionObj.q == q && gridPositionObj.r == r) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + q;
        hash = 97 * hash + r;
        return hash;
    }

    /**
     * @param otherPosition other hex grid position
     * @return true if the positions are adjacent
     */
    public boolean isAdjacent(final GridPosition otherPosition) {
        GridPosition neighbor;
        for (int i = 0; i < 6; i++) {
            neighbor = getNeighborPosition(GridPosition.getDirectionFromNumber(i));
            if (otherPosition.equals(neighbor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param otherPosition reference grid position
     * @return the direction
     */
    public HexagonMap.Direction getDirectionTo(final GridPosition otherPosition) {
        if (equals(otherPosition)) {
            throw new IllegalArgumentException(
                    "Other position (" + otherPosition + ") cannot be same as this (" + this + ")");
        }
        final GridPosition firstStepInLine = line(otherPosition).get(1);

        for (int i = 0; i < 6; i++) {
            if (getNeighborPosition(GridPosition.getDirectionFromNumber(i)).equals(firstStepInLine)) {
                return GridPosition.getDirectionFromNumber(i);
            }
        }
        throw new RuntimeException();
    }

    /**
     * Finds all GridPositions that are on a getLine between this and the given position (the array includes this and
     * the destination positions)
     *
     * @param destination destination grid position
     * @return an array positions
     */
    public ArrayList<GridPosition> line(final GridPosition destination) {
        final ArrayList<GridPosition> result = new ArrayList<>();
        GridPosition p;
        double q_calculated;
        double r_calculated;
        final double n = GridPosition.getDistance(this, destination);
        for (int i = 0; i < n; i++) {
            final double j = i;
            q_calculated = q * (1.0 - j / n) + destination.q * j / n;
            r_calculated = r * (1.0 - j / n) + destination.r * j / n;
            p = GridPosition.hexRound(q_calculated, r_calculated);
            result.add(p);

        }
        result.add(destination);
        return result;
    }

    /**
     * Calculates the grid distance between two positions
     *
     * @param a the start position
     * @param b the destination position
     * @return the distance (number of hexagons)
     */
    public static int getDistance(final GridPosition a, final GridPosition b) {
        return (Math.abs(a.q - b.q) + Math.abs(a.r - b.r) + Math.abs(a.q + a.r - b.q - b.r)) / 2;
    }

    public int getDistance(final GridPosition target) {
        return GridPosition.getDistance(this, target);
    }

    @Override
    public String toString() {
        return "GridPosition q=" + q + ", r=" + r;
    }
}
