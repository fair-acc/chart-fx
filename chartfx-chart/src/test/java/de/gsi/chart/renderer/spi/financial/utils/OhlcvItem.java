package de.gsi.chart.renderer.spi.financial.utils;

import java.util.Date;
import java.util.Objects;

import de.gsi.dataset.spi.financial.api.attrs.AttributeModel;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;

public class OhlcvItem implements IOhlcvItem {
    private final Date timestamp;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double volume;
    private final double oi;

    private AttributeModel addon;

    public OhlcvItem(Date timestamp, double open, double high, double low, double close, double volume, double oi) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.oi = oi;
    }

    @Override
    public Date getTimeStamp() {
        return timestamp;
    }

    @Override
    public double getOpen() {
        return open;
    }

    @Override
    public double getHigh() {
        return high;
    }

    @Override
    public double getLow() {
        return low;
    }

    @Override
    public double getClose() {
        return close;
    }

    @Override
    public double getVolume() {
        return volume;
    }

    @Override
    public double getOpenInterest() {
        return oi;
    }

    @Override
    public AttributeModel getAddon() {
        return addon;
    }

    @Override
    public AttributeModel getAddonOrCreate() {
        if (addon == null) {
            addon = new AttributeModel();
        }
        return addon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OhlcvItem ohlcvItem = (OhlcvItem) o;
        return Double.compare(ohlcvItem.open, open) == 0 && Double.compare(ohlcvItem.high, high) == 0 && Double.compare(ohlcvItem.low, low) == 0 && Double.compare(ohlcvItem.close, close) == 0 && Double.compare(ohlcvItem.volume, volume) == 0 && Double.compare(ohlcvItem.oi, oi) == 0 && Objects.equals(timestamp, ohlcvItem.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, open, high, low, close, volume, oi);
    }
}
