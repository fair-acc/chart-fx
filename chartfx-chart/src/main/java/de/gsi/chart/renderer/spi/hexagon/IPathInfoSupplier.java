package de.gsi.chart.renderer.spi.hexagon;

public interface IPathInfoSupplier {
    boolean isBlockingPath(Hexagon hexagon);

    int getMovementCost(Hexagon from, Hexagon to);
}
