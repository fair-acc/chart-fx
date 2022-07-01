package io.fair_acc.chartfx.renderer.spi.hexagon;

public interface IPathInfoSupplier {
    int getMovementCost(Hexagon from, Hexagon to);

    boolean isBlockingPath(Hexagon hexagon);
}
