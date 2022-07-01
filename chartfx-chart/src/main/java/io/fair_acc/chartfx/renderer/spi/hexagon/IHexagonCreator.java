package io.fair_acc.chartfx.renderer.spi.hexagon;

import javafx.scene.paint.Color;

public interface IHexagonCreator {
    void createHexagon(int q, int r, Color imagePixelColor, HexagonMap map);
}
