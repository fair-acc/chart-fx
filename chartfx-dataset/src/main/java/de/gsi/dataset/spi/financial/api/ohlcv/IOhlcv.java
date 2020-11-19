package de.gsi.dataset.spi.financial.api.ohlcv;

import de.gsi.dataset.spi.financial.api.attrs.AttributeModel;

/**
 * Domain Object implements this API for providing of OHLCV information data.
 */
public interface IOhlcv extends Iterable<IOhlcvItem> {

    /**
     * @param index index of the ohlcv item
     * @return provides ohlcv item
     */
    IOhlcvItem getOhlcvItem(int index);

    /**
     * @return count of actual available items
     */
    int size();

    /**
     * @return OHLCV addon. If not available, null is return.
     */
    AttributeModel getAddon();

    /**
     * @return OHLCV Item addon. If not available, create it.
     */
    AttributeModel getAddonOrCreate();
}
