package de.gsi.dataset.spi.financial.api.ohlcv;

import java.util.Date;

import de.gsi.dataset.spi.financial.api.attrs.AttributeModel;

/**
 * OHLCV Item data. Basic API Domain object for standard visualization processing.
 */
public interface IOhlcvItem {
    /**
     * @return get timestamp of this ohlcv item
     */
    Date getTimeStamp();

    /**
     * @return get instrument open value
     */
    double getOpen();

    /**
     * @return get instrument high value
     */
    double getHigh();

    /**
     * @return get instrument low value
     */
    double getLow();

    /**
     * @return get instrument close value
     */
    double getClose();

    /**
     * @return get instrument volume information (Up/Down Tick together)
     */
    double getVolume();

    /**
     * @return get instrument OI (Open interest)
     */
    double getOpenInterest();

    /**
     * @return OHLCV Item addon. If not available, null is return.
     */
    AttributeModel getAddon();

    /**
     * @return OHLCV Item addon. If not available, create it.
     */
    AttributeModel getAddonOrCreate();
}
