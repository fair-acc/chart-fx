package de.gsi.chart.samples.financial.dos;

import java.util.Collection;
import java.util.TreeMap;

public class PriceVolumeContainer {
    private final TreeMap<Double, PriceVolume> priceVolumeMap = new TreeMap<>();
    private double pocPrice;
    private double pocVolume = -Double.MAX_VALUE;

    /**
     * Add volume up and down to specific price
     * @param price value
     * @param volumeDown tick down volume
     * @param volumeUp tick up volume
     */
    public void addPriceVolume(double price, double volumeDown, double volumeUp) {
        PriceVolume priceVolume = priceVolumeMap.get(price);
        if (priceVolume == null) {
            priceVolume = new PriceVolume(price, volumeDown, volumeUp);
            priceVolumeMap.put(price, priceVolume);

        } else {
            priceVolume.volumeUp += volumeUp;
            priceVolume.volumeDown += volumeDown;
        }
        double totalVolume = priceVolume.volumeUp + priceVolume.volumeDown;
        if (totalVolume > pocVolume) {
            pocVolume = totalVolume;
            pocPrice = price;
        }
    }

    /**
   * @param price return DO price volume by required price level
   * @return provides volume information for specific price
   */
    public PriceVolume getPriceVolumeBy(double price) {
        return priceVolumeMap.get(price);
    }

    /**
   * @return provides price volume tree map
   */
    public TreeMap<Double, PriceVolume> getCompletedPriceVolumeTreeMap() {
        return priceVolumeMap;
    }

    /**
   * @return provides price volume collection for actual bar
   */
    public Collection<PriceVolume> getCompletedPriceVolume() {
        return priceVolumeMap.values();
    }

    /**
   * Reset PriceVolume instance
   */
    public void clear() {
        priceVolumeMap.clear();
        pocVolume = -Double.MAX_VALUE;
        pocPrice = 0.0d;
    }

    /**
   * @return provides calculated POC price
   */
    public double getPocPrice() {
        return pocPrice;
    }

    /**
   * @return provides total volume of POS price
   */
    public double getPocVolume() {
        return pocVolume;
    }
}
