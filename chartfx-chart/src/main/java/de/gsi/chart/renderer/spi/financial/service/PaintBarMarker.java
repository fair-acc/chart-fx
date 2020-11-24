package de.gsi.chart.renderer.spi.financial.service;

import javafx.scene.paint.Paint;

/**
 * Specific painting/coloring of OHLCV/Candle bars.
 * Addon and extension possibilities.
 * The PaintBar is know techniques in the trading frameworks and platforms.
 */
public interface PaintBarMarker {
    /**
     * Provides paint by selected OHLCV item domain object.
     * @param data domain object for Renderer Extension Points
     * @return paint bar
     */
    Paint getPaintBy(OhlcvRendererEpData data);
}
