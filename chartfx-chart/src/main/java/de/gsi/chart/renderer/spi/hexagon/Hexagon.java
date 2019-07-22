package de.gsi.chart.renderer.spi.hexagon;

import java.util.ArrayList;
import java.util.List;

import de.gsi.chart.renderer.spi.hexagon.HexagonMap.Direction;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;

/**
 * A Hexagon is the building block of the grid.
 */
public class Hexagon extends Polygon {

    public final GridPosition position;
    private HexagonMap map;
    private boolean isVisualObstacle;
    private boolean isBlockingPath;
    int aStarGscore; // Variables for the A* pathfinding algorithm.
    int aStarFscore;
    Hexagon aStarCameFrom;
    private int graphicsXoffset;
    private int graphicsYoffset;

    /**
     * The position of the Hexagon is specified with axial coordinates
     *
     * @param q the Q coordinate
     * @param r the R coordinate
     */
    public Hexagon(final int q, final int r) {
        super();
        position = new GridPosition(q, r);
        setStroke(Color.DARKGRAY);
        setFill(Color.GREY);
    }

    public void init() {
        for (final double p : calculatePolygonPoints()) {
            getPoints().add(p);
        }
    }

    /**
     * @return axial Q-value
     */
    public int getQ() {
        return position.q;
    }

    /**
     * @return axial R-value
     */
    public int getR() {
        return position.r;
    }

    /**
     * This affects the field of view calculations. If true, the hexagons behind this hexagon cannot be seen (but this
     * hexagon can still be seen).
     * 
     * @param b true: opaque hexagon
     */
    public void setIsVisualObstacle(final boolean b) {
        isVisualObstacle = b;
    }

    /**
     * This affects the field of view calculations.
     *
     * @return If true, the hexagons behind this hexagon cannot be seen (but this hexagon can still be seen).
     */
    public boolean isVisualObstacle() {
        return isVisualObstacle;
    }

    /**
     * This affects the pathfinding calculations. If true, the algorithm will try to find a path around this Hexagon. If
     * you want to have more control over this, you can supply your own class implementing IPathInfoSupplier to the
     * pathfinding method.
     * 
     * @param b true: blocking hexagon
     */
    public void setIsBlockingPath(final boolean b) {
        isBlockingPath = b;
    }

    /**
     * This affects the pathfinding calculations
     *
     * @return true if this is an obstacle that blocks the path
     */
    public boolean isBlockingPath() {
        return isBlockingPath;
    }

    private static double[] sinAngle = { 0.5 * Math.sqrt(3), 0.5 * Math.sqrt(3), 0, -0.5 * Math.sqrt(3),
            -0.5 * Math.sqrt(3), 0, 0.5 * Math.sqrt(3) };
    private static double[] cosAngle = { 0.5, -0.5, -1, -0.5, 0.5, 1, 0.5 };

    // --------------------- Graphics
    // --------------------------------------------
    private double[] calculatePolygonPoints() {
        checkMap();
        final int graphicsHeight = map.hexagonSize * 2;
        final double graphicsWidth = Math.sqrt(3) / 2 * graphicsHeight;
        graphicsXoffset = (int) (graphicsWidth * position.q + 0.5 * graphicsWidth * position.r);
        graphicsYoffset = (int) (3.0 / 4.0 * graphicsHeight * position.r);
        graphicsXoffset = graphicsXoffset + map.graphicsXpadding;
        graphicsYoffset = graphicsYoffset + map.graphicsYpadding;

        final double[] polyPoints = new double[12];
        for (int i = 0; i < 6; i++) {
            polyPoints[i * 2] = graphicsXoffset + map.hexagonSize * Hexagon.sinAngle[i];
            polyPoints[i * 2 + 1] = graphicsYoffset + map.hexagonSize * Hexagon.cosAngle[i];
        }
        return polyPoints;
    }

    /**
     * @return where this Hexagon is when rendererd into a JavaFX Group
     */
    public int getGraphicsXoffset() {
        if (graphicsXoffset == 0) {
            calculatePolygonPoints();
        }
        return graphicsXoffset;
    }

    /**
     * @return where this Hexagon is when rendererd into a JavaFX Group
     */
    public int getGraphicsYoffset() {
        if (graphicsYoffset == 0) {
            calculatePolygonPoints();
        }
        return graphicsYoffset;
    }

    /**
     * This method is the safe way to change the background color since it makes sure that the change is made on the
     * JavaFX Application thread.
     *
     * @param c the color
     */
    public void setBackgroundColor(final Color c) {
        Platform.runLater(new UIupdater(this, c));
    }

    @Override
    public String toString() {
        return "Hexagon q:" + position.q + " r:" + position.r;
    }

    /**
     * Finds the direction (NORTHWEST, NORTHEAST, EAST, SOUTHEAST, SOUTHWEST or WEST) If target is a neighbour, then it
     * is quite simple. If target is not a neighbour, this returns the direction to the first step on a line to the
     * target.
     * 
     * @param target target hexagon
     * @return direction towards target
     */
    public HexagonMap.Direction getDirectionTo(final Hexagon target) {
        return position.getDirectionTo(target.position);
    }

    /**
     * Returns all Hexagons that are located a certain distance from here
     * 
     * @param radius in hex grid coordinates
     * @return list of all hexagon within the circle
     */
    public List<Hexagon> getHexagonsOnRingEdge(final int radius) {
        checkMap();
        return GridCalculationsHelper.getHexagonsOnRingEdge(this, radius, map);
    }

    /**
     * Returns all Hexagons that are located within a certain distance from here
     * 
     * @param radius in hex grid coordinates
     * @return list of all hexagon on the circle radius
     */
    public List<Hexagon> getHexagonsInRingArea(final int radius) {
        checkMap();
        return GridCalculationsHelper.getHexagonsInRingArea(this, radius, map);
    }

    private void checkMap() {
        if (map == null) {
            throw new IllegalStateException(
                    "Hexagon must be added to a HexagonMap before this operation. See addHexagon()");
        }
    }

    /**
     * Finds the neighbour of this Hexagon
     *
     * @param direction direction from source
     * @return neighbour
     */
    public Hexagon getNeighbour(final HexagonMap.Direction direction) {
        checkMap();
        final GridPosition neighborPosition = position.getNeighborPosition(direction);
        return map.getHexagon(neighborPosition);
    }

    /**
     * Finds all neighbours of this Hexagon
     * 
     * @return list of all direct neighbours
     */
    public List<Hexagon> getNeighbours() {
        final ArrayList<Hexagon> result = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            final Hexagon neighbour = getNeighbour(GridPosition.getDirectionFromNumber(i));
            if (neighbour == null) {
                result.add(neighbour);
            }
        }
        return result;
    }

    /**
     * Finds the cheapest path from start to the goal. The A* algorithm is used.
     *
     * @param destination the target Hexagon
     * @param pathInfoSupplier a class implementing the IPathInfoSupplier interface. This can be used to add inpassable
     *            hexagons and customize the movement costs.
     * @return an array of Hexagons, sorted so that the first step comes first.
     * @throws NoPathFoundException if there exists no path between start and the goal
     */
    public List<Hexagon> getPathTo(final Hexagon destination, final IPathInfoSupplier pathInfoSupplier)
            throws NoPathFoundException {
        checkMap();
        return GridCalculationsHelper.getPathBetween(this, destination, pathInfoSupplier);
    }

    /**
     * Finds the cheapest path from here to the destination. The A* algorithm is used. This method uses the method
     * isBlockingPath() in Hexagon and the movement cost between neighboring hexagons is always 1.
     *
     * @param destination the target Hexagon
     * @return an array of Hexagons, sorted so that the first step comes first.
     * @throws NoPathFoundException if there exists no path between start and the goal
     */
    public List<Hexagon> getPathTo(final Hexagon destination) throws NoPathFoundException {
        checkMap();
        return GridCalculationsHelper.getPathBetween(this, destination, new HexagonMap.DefaultPathInfoSupplier());
    }

    /**
     * Finds all Hexagons that are on a line between this and destination
     * 
     * @param origin source hex tile
     * @param destination target hex tile
     * @return list of hexagon on the path
     */
    public List<Hexagon> getLine(final Hexagon origin, final Hexagon destination) {
        checkMap();
        return GridCalculationsHelper.getLine(origin.position, destination.position, map);
    }

    /**
     * Calculates all Hexagons that are visible from this Hexagon. The line of sight can be blocked by Hexagons that has
     * isVisualObstacle == true. NOTE: Accuracy is not guaranteed!
     *
     * @param visibleRange a limit of how long distance can be seen assuming there are no obstacles
     * @return an array of Hexagons that are visible
     */
    public List<Hexagon> getVisibleHexes(final int visibleRange) {
        checkMap();
        return GridCalculationsHelper.getVisibleHexes(this, visibleRange, map);
    }

    /**
     * Calculates the distance (number of hexagons) to the target hexagon
     * 
     * @param target destination hex tile
     * @return distance in hex grid coordinates
     */
    public int getDistance(final Hexagon target) {
        return position.getDistance(target.position);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + getQ();
        hash = 31 * hash + getR();
        return hash;
    }

    /**
     * Two Hexagons are equal if they have the same q and r
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (!obj.getClass().equals(this.getClass())) {
            return false;
        }
        final Hexagon hexagonObj = (Hexagon) obj;
        return hexagonObj.getQ() == getQ() && hexagonObj.getR() == getR();
    }

    /**
     * This gives the Hexagon access a HexagonMap without actually adding it to the HexagonMap. It can be useful e.g. if
     * you want to make some calculations before creating another Hexagon.
     * 
     * @param map global map reference
     */
    public void setMap(final HexagonMap map) {
        this.map = map;
        if (map != null) {
            init();
        }
    }

    public void drawHexagon(final GraphicsContext gc) {
        final ObservableList<Double> points = getPoints();
        final int nPoints = points.size() / 2;
        final double[] xPoints = new double[nPoints];
        final double[] yPoints = new double[nPoints];
        for (int i = 0; i < nPoints; i++) {
            xPoints[i] = 0.5 + Math.round(points.get(2 * i));
            yPoints[i] = 0.5 + Math.round(points.get(2 * i + 1));
        }
        gc.fillPolygon(xPoints, yPoints, nPoints);
        gc.strokePolygon(xPoints, yPoints, nPoints);
    }

    public void drawHexagon(final GraphicsContext gc, Direction... directions) {
        final ObservableList<Double> points = getPoints();
        final int nPoints = points.size() / 2;
        final double[] xPoints = new double[nPoints];
        final double[] yPoints = new double[nPoints];
        for (int i = 0; i < nPoints; i++) {
            xPoints[i] = 0.5 + Math.round(points.get(2 * i));
            yPoints[i] = 0.5 + Math.round(points.get(2 * i + 1));
        }
        gc.fillPolygon(xPoints, yPoints, nPoints);

        for (final Direction side : directions) {
            switch (side) {
            case EAST:
                gc.strokeLine(xPoints[0], yPoints[0], xPoints[1], yPoints[1]);
                break;
            case NORTHEAST:
                gc.strokeLine(xPoints[1], yPoints[1], xPoints[2], yPoints[2]);
                break;
            case NORTHWEST:
                gc.strokeLine(xPoints[2], yPoints[2], xPoints[3], yPoints[3]);
                break;
            case WEST:
                gc.strokeLine(xPoints[3], yPoints[3], xPoints[4], yPoints[4]);
                break;
            case SOUTHWEST:
                gc.strokeLine(xPoints[4], yPoints[4], xPoints[5], yPoints[5]);
                break;
            case SOUTHEAST:
                gc.strokeLine(xPoints[5], yPoints[5], xPoints[0], yPoints[0]);
                break;
            default:
                break;
            }
        }
        // gc.strokePolygon(xPoints, yPoints, nPoints);
    }

    public void draw(GraphicsContext gc) {
        gc.save();
        gc.setStroke(getStroke());
        gc.setLineWidth(getStrokeWidth());
        gc.setFill(getFill());
        drawHexagon(gc);
        gc.restore();
    }

    public void drawContour(GraphicsContext gc) {
        gc.save();
        gc.setStroke(getStroke());
        gc.setLineWidth(getStrokeWidth());
        gc.setFill(getFill());
        if (map == null) {
            drawHexagon(gc, Direction.values());
            gc.restore();
        }

        final List<Direction> list = new ArrayList<>();
        for (final Direction direction : Direction.values()) {
            final Hexagon neighbour = getNeighbour(direction);
            if (neighbour == null) {
                continue;
            }
            final Paint stroke = neighbour.getStroke();
            // if (stroke != null && !stroke.equals(this.getStroke())) {
            // list.add(direction);
            // }

            //TODO: find work-around for colour smoothing operation on image scaling
            if (stroke != null && !myColourCompare(stroke, getStroke(), 0.2)) {
                list.add(direction);
            }
        }

        drawHexagon(gc, list.toArray(new Direction[list.size()]));

        gc.restore();
    }

    private boolean myColourCompare(Paint a, Paint b, double threshold) {
        if (!(a instanceof Color) || !(b instanceof Color)) {
            return false;
        }
        final Color ca = (Color) a;
        final Color cb = (Color) b;
        return !(Math.abs(ca.getRed() - cb.getRed()) > threshold)
                && (!(Math.abs(ca.getGreen() - cb.getGreen()) > threshold)
                        && (!(Math.abs(ca.getBlue() - cb.getBlue()) > threshold)
                                && !(Math.abs(ca.getOpacity() - cb.getOpacity()) > threshold)));
    }

    public void renderCoordinates(GraphicsContext gc) {
        final Text text = new Text(position.getCoordinates());
        if (map != null) {
            // TODO re-enable font
            // text.setFont(map.getFont());
        }
        final double textWidth = text.getBoundsInLocal().getWidth();
        final double textHeight = text.getBoundsInLocal().getHeight();
        final double x = getGraphicsXoffset() - textWidth / 2;
        final double y = getGraphicsYoffset() + textHeight / 4;
        text.setX(x);
        text.setY(y);
        // Not sure why, but 4 seems like a good value
        gc.strokeText(position.getCoordinates(), x, y);
        // root.getChildren().add(text);
    }

    class UIupdater implements Runnable {

        private final Hexagon h;
        private final Color c;

        UIupdater(final Hexagon h, final Color c) {
            this.h = h;
            this.c = c;
        }

        @Override
        public void run() {
            h.setFill(c);
        }
    }

}
