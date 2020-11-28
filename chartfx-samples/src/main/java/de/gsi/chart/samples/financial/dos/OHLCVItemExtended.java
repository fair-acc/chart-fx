package de.gsi.chart.samples.financial.dos;

import java.util.Date;

/**
 * Extended data for Volume/Market Profiles, Numbers Bars etc.
 */
public class OHLCVItemExtended {
    public final Object lock = new Object();

    private Date timestamp; // unique identifier of the item addon
    private PriceVolumeContainer priceVolumeMap = new PriceVolumeContainer();
    private OHLCVItem pullbackOhlcvItem;
    private OHLCVItem lastIncrementItem;

    private AbsorptionClusterDO absorptionClusterDO = null; // absorption of volume levels for fims

    public PriceVolumeContainer getPriceVolumeContainer() {
        return priceVolumeMap;
    }

    public void setPriceVolumeMap(PriceVolumeContainer priceVolumeMap) {
        this.priceVolumeMap = priceVolumeMap;
    }

    public void setAbsorptionClusterDO(AbsorptionClusterDO absorptionClusterDO) {
        this.absorptionClusterDO = absorptionClusterDO;
    }

    public AbsorptionClusterDO getAbsorptionClusterDO() {
        return absorptionClusterDO;
    }

    public OHLCVItem getPullbackOhlcvItem() {
        return pullbackOhlcvItem;
    }

    public void setPullbackOhlcvItem(OHLCVItem pullbackOhlcvItem) {
        this.pullbackOhlcvItem = pullbackOhlcvItem;
    }

    public Date getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setLastIncrementItem(OHLCVItem lastIncrementItem) {
        this.lastIncrementItem = lastIncrementItem;
    }

    public OHLCVItem getLastIncrementItem() {
        return lastIncrementItem;
    }
}
