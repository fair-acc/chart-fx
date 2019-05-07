package de.gsi.chart.renderer.spi.hexagon;

/**
 * This exception is thrown when the pathfinding algorithm cannot find any path to the goal
 */
public class NoPathFoundException extends Exception {

    private static final long serialVersionUID = 1362775696243612783L;

    public NoPathFoundException(String message) {
        super(message);
    }
}
