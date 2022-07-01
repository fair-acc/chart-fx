package io.fair_acc.financial.samples.service.addon;

import java.util.NavigableMap;

import io.fair_acc.financial.samples.dos.*;
import io.fair_acc.financial.samples.dos.*;
import io.fair_acc.financial.samples.service.consolidate.OhlcvConsolidationAddon;

public class AbsorptionConsolidationAddon implements OhlcvConsolidationAddon {
    private final boolean searchDynamic;
    private final int absorptionLevel;
    private final int absorptionCluster;
    private final double searchBarPercent;
    private final double extremeAbsorptionLevelParam;

    /**
	 * @param searchDynamic
	 *            - dynamic processing when the bar is painting
	 * @param absorptionLevel
	 *            - defines value for volume in the price level which detects
	 *            price for absorption cluster
	 * @param absorptionCluster
	 *            - minimal length of cluster for detection
	 * @param searchBarPercent
	 *            - where is accepted the cluster in whole bar in percent (e.g.
	 *            33% = 1/3 bar from low for bid, and high for ask)
	 * @param extremeAbsorptionLevelParam
	 *            - logic for searching extreme bid/ask volumes single clusters
	 */
    public AbsorptionConsolidationAddon(boolean searchDynamic, int absorptionLevel, int absorptionCluster,
            double searchBarPercent, double extremeAbsorptionLevelParam) {
        this.searchDynamic = searchDynamic;
        this.absorptionLevel = absorptionLevel;
        this.absorptionCluster = absorptionCluster;
        this.searchBarPercent = searchBarPercent;
        this.extremeAbsorptionLevelParam = extremeAbsorptionLevelParam;
    }

    @Override
    public DefaultOHLCV consolidationUpdateAddon(DefaultOHLCV ohlcv, OHLCVItem incrementItem) {
        return ohlcv;
    }

    @Override
    public DefaultOHLCV consolidationAdditionAddon(DefaultOHLCV ohlcv, OHLCVItem incrementItem) {
        if (ohlcv.size() < 2) {
            return ohlcv;
        }
        OHLCVItem lastBarItem = ohlcv.getBackOhlcvItem(1);
        findClusters(lastBarItem);

        return ohlcv;
    }

    @Override
    public boolean isDynamic() {
        return searchDynamic;
    }

    protected void findClusters(OHLCVItem barItem) {
        if (barItem.getExtended() == null) {
            return;
        }
        AbsorptionClusterDO absorptionClusterDO = new AbsorptionClusterDO();
        findClustersOfSide(barItem, true, absorptionClusterDO);
        findClustersOfSide(barItem, false, absorptionClusterDO);
        barItem.getExtended().setAbsorptionClusterDO(absorptionClusterDO); // replace previous
    }

    protected void findClustersOfSide(OHLCVItem barItem, boolean bidOrAsk, AbsorptionClusterDO absorptionClusterDO) {
        PriceVolumeContainer priceVolumeContainer = barItem.getExtended().getPriceVolumeContainer();
        NavigableMap<Double, Double[]> map = bidOrAsk ? priceVolumeContainer.getCompletedPriceVolumeTreeMap()
                                                      : priceVolumeContainer.getCompletedPriceVolumeTreeMap().descendingMap();

        double length = barItem.getHigh() - barItem.getLow();
        double maxPriceBid = length * searchBarPercent + barItem.getLow();
        double minPriceAsk = barItem.getHigh() - length * searchBarPercent;
        boolean clusterDetected = false;
        double val1 = -1;
        double val2 = -1;
        int clusterActiveLength = 0;
        boolean firstTime = false;

        //--------------------------------------------
        // BASIC CLUSTER DETECTION
        // basic detection of cluster defined by absorption level and absorption
        // cluster minimal length
        for (Double[] priceVolume : map.values()) {
            double volume = bidOrAsk ? priceVolume[1] : priceVolume[2];
            boolean inrange = firstTime
                           || (bidOrAsk ? priceVolume[0] <= maxPriceBid : priceVolume[0] >= minPriceAsk);
            if (volume >= absorptionLevel && inrange) {
                if (clusterActiveLength == 0) {
                    val1 = priceVolume[0];
                    firstTime = true;
                }
                clusterActiveLength++;
            } else {
                clusterActiveLength = 0;
                firstTime = false;
                if (clusterDetected) {
                    break;
                }
            }
            if (clusterActiveLength >= absorptionCluster) {
                clusterDetected = true;
                val2 = priceVolume[0];
            }
        }
        if (clusterDetected) {
            if (bidOrAsk) {
                absorptionClusterDO.addBidCluster(new Interval<>(val2, val1));
            } else {
                absorptionClusterDO.addAskCluster(new Interval<>(val1, val2));
            }
        } else {
            //-------------------------------------------------------------
            // EXTREME CLUSTER DETECTION
            // basic cluster doesn't exist
            // try to find extreme cluster accumulation bid/ask volume
            val1 = -1;
            val2 = -1;
            clusterActiveLength = 0;
            firstTime = false;

            double extremAbsorptionLevel = 0.0d;
            for (Double[] priceVolume : map.values()) {
                double volume = bidOrAsk ? priceVolume[1] : priceVolume[2];
                extremAbsorptionLevel += volume;
            }
            extremAbsorptionLevel = extremAbsorptionLevel / map.size();

            for (Double[] priceVolume : map.values()) {
                double volume = bidOrAsk ? priceVolume[1] : priceVolume[2];
                boolean inrange = firstTime
                               || (bidOrAsk ? priceVolume[0] <= maxPriceBid : priceVolume[0] >= minPriceAsk);
                if (volume - extremAbsorptionLevel >= extremeAbsorptionLevelParam && inrange) {
                    if (clusterActiveLength == 0) {
                        val1 = priceVolume[0];
                        firstTime = true;
                    }
                    clusterActiveLength++;
                } else {
                    clusterActiveLength = 0;
                    firstTime = false;
                    if (clusterDetected) {
                        break;
                    }
                }
                if (clusterActiveLength >= 1) {
                    clusterDetected = true;
                    val2 = priceVolume[0];
                }
            }
            if (clusterDetected) {
                if (bidOrAsk) {
                    absorptionClusterDO.addBidCluster(new Interval<>(val2, val1));
                } else {
                    absorptionClusterDO.addAskCluster(new Interval<>(val1, val2));
                }
            }
        }
    }
}
