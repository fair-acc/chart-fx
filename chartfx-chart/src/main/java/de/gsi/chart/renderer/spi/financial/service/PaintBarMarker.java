package de.gsi.chart.renderer.spi.financial.service;

import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import javafx.scene.paint.Paint;

/**
 * Specific painting/coloring of OHLCV/Candle bars.
 */
public interface PaintBarMarker {

    /**
     * Provides paint by selected OHLCV item domain object.
     * @param ohlcvItem selected OHLCV item (possibility to use addon, or timestamp).
     * @return paint bar
     */
    Paint getPaintBy(IOhlcvItem ohlcvItem);
}
