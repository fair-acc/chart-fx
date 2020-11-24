package de.gsi.chart.samples.financial.dos;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.gsi.dataset.spi.financial.api.attrs.AttributeModel;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;

/**
 * Common wrapper of OHLC/V-OI object
 */
public class OHLCVItem implements IOhlcvItem {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    private final Date timeStamp;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double volume;
    private double volumeUp; // ask volume
    private double volumeDown; // bid volume
    private final double openInterest;
    private boolean gap;
    private AttributeModel addon;

    public OHLCVItem(Date timeStamp, double open, double high, double low, double close, double volume, double openInterest) {
        this.timeStamp = timeStamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.openInterest = openInterest;
    }

    public OHLCVItem(Date timeStamp, double open, double high, double low, double close, double volume, double openInterest, double volumeUp, double volumeDown) {
        this.timeStamp = timeStamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.openInterest = openInterest;
        this.volumeUp = volumeUp;
        this.volumeDown = volumeDown;
    }

    public void setAddon(AttributeModel addon) {
        this.addon = addon;
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

    public Date getTimeStamp() {
        return timeStamp;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public double getVolume() {
        return volume;
    }

    /**
     * @return volume up/ask volume
     */
    public double getVolumeUpAsk() {
        return volumeUp;
    }

    /**
     * @return volume down/bid volume
     */
    public double getVolumeDownBid() {
        return volumeDown;
    }

    public double getOpenInterest() {
        return openInterest;
    }

    public boolean isGap() {
        return gap;
    }

    public void setGap(boolean gap) {
        this.gap = gap;
    }

    @Override
    public String toString() {
        return "OHLCV [timeStamp=" + dateFormat.format(timeStamp) + ", open=" + open + ", high=" + high + ", low=" + low + ", close=" + close + ", volume=" + volume + ", bidVol=" + volumeDown + ", askVol=" + volumeUp + ", openInterest="
                + openInterest + "]";
    }

    public String toStringShort() {
        return dateFormat.format(timeStamp) + ", " + open + ", " + high + ", " + low + ", " + close + ", V=" + volume + ", OI=" + openInterest + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(close);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(high);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(low);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(open);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(openInterest);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((timeStamp == null) ? 0 : timeStamp.hashCode());
        temp = Double.doubleToLongBits(volume);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OHLCVItem other = (OHLCVItem) obj;
        if (Double.doubleToLongBits(close) != Double.doubleToLongBits(other.close))
            return false;
        if (Double.doubleToLongBits(high) != Double.doubleToLongBits(other.high))
            return false;
        if (Double.doubleToLongBits(low) != Double.doubleToLongBits(other.low))
            return false;
        if (Double.doubleToLongBits(open) != Double.doubleToLongBits(other.open))
            return false;
        if (Double.doubleToLongBits(openInterest) != Double.doubleToLongBits(other.openInterest))
            return false;
        if (timeStamp == null) {
            if (other.timeStamp != null)
                return false;
        } else if (!timeStamp.equals(other.timeStamp))
            return false;
        if (Double.doubleToLongBits(volume) != Double.doubleToLongBits(other.volume))
            return false;
        return true;
    }
}
