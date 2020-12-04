package de.gsi.dataset.spi.financial;

import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;

/**
 * OHLCV Listener about structure changes.
 */
public interface OhlcvChangeListener {
    /**
     * Notification event about new ohlcv item changed
     * @param ohlcvItem new or changed ohlcv item
     * @exception Exception if the processing failed
     */
    void tickEvent(IOhlcvItem ohlcvItem) throws Exception;
}
