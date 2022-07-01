package io.fair_acc.dataset.spi.financial.api.attrs;

/**
 * Provides access to Domain Objects Extensions
 * Common dynamic extension of API to transmit additional (addon) data.
 */
public interface AttributeModelAware {
    /**
     * @return OHLCV addon. If not available, null is return.
     */
    AttributeModel getAddon();

    /**
     * @return OHLCV Item addon. If not available, create it.
     */
    AttributeModel getAddonOrCreate();
}
