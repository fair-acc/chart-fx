package io.fair_acc.chartfx.renderer.spi.hexagon;

/**
 * This exception is thrown when trying to retrieve a Hexagon from a position where ther is no Hexagon
 */
public class NoHexagonFoundException extends Exception {

    private static final long serialVersionUID = -3886480899940588279L;

    public NoHexagonFoundException(String message) {
        super(message);
    }
}
