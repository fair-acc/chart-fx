package de.gsi.dataset.spi.financial.api.ohlcv;

/**
 * Provides access to addons and extensions for OHLCV based datasets.
 */
public interface IOhlcvItemAware {
    /**
     * Provides OHLCV domain object with extensions
     * @param index to the dataset/domain structure
     * @return the filled ohlcv item
     */
    IOhlcvItem getItem(int index);

    /**
     * Provides the last available item in the OHLC/V structure
     * @return the last filled ohlcv item
     */
    IOhlcvItem getLastItem();
}
