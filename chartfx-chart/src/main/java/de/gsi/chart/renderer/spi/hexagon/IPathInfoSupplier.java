package de.gsi.chart.renderer.spi.hexagon;

public interface IPathInfoSupplier {
    int getMovementCost(Hexagon from, Hexagon to);

    boolean isBlockingPath(Hexagon hexagon);
}
