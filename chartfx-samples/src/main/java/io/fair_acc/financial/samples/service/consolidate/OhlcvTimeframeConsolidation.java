package io.fair_acc.financial.samples.service.consolidate;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.fair_acc.financial.samples.dos.Interval;
import io.fair_acc.financial.samples.dos.OHLCVItem;
import io.fair_acc.financial.samples.dos.OHLCVItemExtended;
import io.fair_acc.financial.samples.dos.PriceVolumeContainer;
import io.fair_acc.financial.samples.service.period.IntradayPeriod;

public class OhlcvTimeframeConsolidation {
    private OhlcvTimeframeConsolidation() {
    }

    /**
     * Create incremental consolidation (re-sampling) of OHLC/V.
     *
     * @param intradayPeriod required re-sampled period.
     * @param tt             time template range. Necessary for perfect consolidation times are calculated from beginning of the TT and ending of TT.
     *                       If missing (null), the "00:00" time is selected as beginning for consolidation algorithm.
     * @param addons         which can extend re-sampling process about additional services to add more specific behaviours.
     * @return prepared service for incremental consolidation.
     */
    public static IncrementalOhlcvConsolidation createConsolidation(IntradayPeriod intradayPeriod, Interval<Calendar> tt, Map<String, OhlcvConsolidationAddon[]> addons) {
        IncrementalOhlcvConsolidation consolidation = null;
        OhlcvConsolidationComputation ohlcvConsolidationComputation = intradayPeriod.isExtendedCalculation() ? new ExtendedOhlcvConsolidationComputation() : new StandardOhlcvConsolidationComputation();

        OhlcvConsolidationAddon[] _ohlcvConsolidationAddons = null;
        if (intradayPeriod.getCalculationAddonServicesType() != null && addons != null) {
            _ohlcvConsolidationAddons = addons.get(intradayPeriod.getCalculationAddonServicesType());
        }

        switch (intradayPeriod.getPeriod()) {
        case RB: // Example of range bars length 4 and one tick is 0.25 of point
            consolidation = new RangeBarsIncrementalOhlcvConsolidation(
                    ohlcvConsolidationComputation, intradayPeriod, intradayPeriod.getMinimalMoveSymbol(), tt, _ohlcvConsolidationAddons);
            break;
        case V:
            consolidation = new VolumeIncrementalOhlcvConsolidation(ohlcvConsolidationComputation, intradayPeriod, tt, _ohlcvConsolidationAddons);
            break;
        case T: // no consolidation
            break;
        case H:
            throw new IllegalArgumentException("HOUR timeframe is not allowed for tick data provider.");
        default: //M, S
            consolidation = new TimePeriodIncrementalOhlcvConsolidation(ohlcvConsolidationComputation, intradayPeriod, tt, _ohlcvConsolidationAddons);
            break;
        }
        return consolidation;
    }

    public interface OhlcvConsolidationComputation {
        OHLCVItem consolidate(List<OHLCVItem> ohlcvItems);

        OHLCVItem consolidate(Date timestamp, List<OHLCVItem> ohlcvItems);

        OHLCVItem consolidate(Date timeStamp, OHLCVItem lastConsolidatedItem, OHLCVItem incrementItem);
    }

    public static class StandardOhlcvConsolidationComputation implements OhlcvConsolidationComputation {
        @Override
        public OHLCVItem consolidate(Date timeStamp, OHLCVItem lastConsolidatedItem, OHLCVItem incrementItem) {
            double oi = 0.0d;
            double volume = lastConsolidatedItem.getVolume();
            double low = lastConsolidatedItem.getLow();
            double high = lastConsolidatedItem.getHigh();

            if (incrementItem.getLow() < low) {
                low = incrementItem.getLow();
            }
            if (incrementItem.getHigh() > high) {
                high = incrementItem.getHigh();
            }
            volume += incrementItem.getVolume();

            return new OHLCVItem(
                    timeStamp,
                    lastConsolidatedItem.getOpen(),
                    high,
                    low,
                    incrementItem.getClose(),
                    volume,
                    oi);
        }

        @Override
        public OHLCVItem consolidate(List<OHLCVItem> ohlcvItems) {
            return consolidate(null, ohlcvItems);
        }

        @Override
        public OHLCVItem consolidate(Date timestamp, List<OHLCVItem> ohlcvItems) {
            if (timestamp == null) {
                timestamp = ohlcvItems.get(0).getTimeStamp();
            }
            double volume = 0.0d;
            double oi = 0.0d;
            double open = ohlcvItems.get(0).getOpen();
            double close = ohlcvItems.get(ohlcvItems.size() - 1).getClose();
            double low = Double.MAX_VALUE;
            double high = -1 * Double.MAX_VALUE;

            for (OHLCVItem ohlcvItem : ohlcvItems) {
                if (ohlcvItem.getLow() < low) {
                    low = ohlcvItem.getLow();
                }
                if (ohlcvItem.getHigh() > high) {
                    high = ohlcvItem.getHigh();
                }
                volume += ohlcvItem.getVolume();
            }

            return new OHLCVItem(timestamp, open, high, low, close, volume, oi);
        }
    }

    public static class ExtendedOhlcvConsolidationComputation implements OhlcvConsolidationComputation {
        @Override
        public OHLCVItem consolidate(Date timeStamp, OHLCVItem lastConsolidatedItem, OHLCVItem incrementItem) {
            return consolidate(timeStamp, lastConsolidatedItem, incrementItem, true);
        }

        public OHLCVItem consolidate(Date timestamp, OHLCVItem lastConsolidatedItem, OHLCVItem incrementItem, boolean computePullbackColumn) {
            double oi = 0.0d;
            double volume = lastConsolidatedItem.getVolume();
            double volumeUp = lastConsolidatedItem.getVolumeUpAsk();
            double volumeDown = lastConsolidatedItem.getVolumeDownBid();
            double open = lastConsolidatedItem.getOpen();
            double close = incrementItem.getClose();
            double low = lastConsolidatedItem.getLow();
            double high = lastConsolidatedItem.getHigh();

            OHLCVItemExtended ohlcvItemExtended = lastConsolidatedItem.getExtended();
            if (ohlcvItemExtended == null) {
                ohlcvItemExtended = new OHLCVItemExtended();
            }

            synchronized (ohlcvItemExtended.lock) {
                ohlcvItemExtended.setLastIncrementItem(incrementItem);

                PriceVolumeContainer priceVolumeContainer = ohlcvItemExtended.getPriceVolumeContainer();
                OHLCVItem pullbackOhlcvItem = ohlcvItemExtended.getPullbackOhlcvItem();

                if (incrementItem.getLow() < low) {
                    low = incrementItem.getLow();
                    if (computePullbackColumn) {
                        // create low pullback column
                        pullbackOhlcvItem = new OHLCVItem(timestamp, low, low, low, low, 0, 0, 0.0d, 0.0d);
                        ohlcvItemExtended.setPullbackOhlcvItem(pullbackOhlcvItem);
                    }
                }
                if (incrementItem.getHigh() > high) {
                    high = incrementItem.getHigh();
                    if (computePullbackColumn) {
                        // create high pullback column
                        pullbackOhlcvItem = new OHLCVItem(timestamp, high, high, high, high, 0, 0, 0.0d, 0.0d);
                        ohlcvItemExtended.setPullbackOhlcvItem(pullbackOhlcvItem);
                    }
                }
                if (pullbackOhlcvItem != null) {
                    OHLCVItem pullbackOhlcvItemUpdated = consolidate(timestamp, pullbackOhlcvItem, incrementItem, false);
                    ohlcvItemExtended.setPullbackOhlcvItem(pullbackOhlcvItemUpdated);
                }

                volume += incrementItem.getVolume();
                volumeUp += incrementItem.getVolumeUpAsk();
                volumeDown += incrementItem.getVolumeDownBid();

                priceVolumeContainer.addPriceVolume(incrementItem.getClose(), incrementItem.getVolumeDownBid(), incrementItem.getVolumeUpAsk());
            }

            OHLCVItem ohlcvItem = new OHLCVItem(timestamp, open, high, low, close, volume, oi, volumeUp, volumeDown);
            ohlcvItemExtended.setTimestamp(timestamp); // unique identifier
            ohlcvItem.setExtended(ohlcvItemExtended);

            return ohlcvItem;
        }

        @Override
        public OHLCVItem consolidate(List<OHLCVItem> ohlcvItems) {
            return consolidate(null, ohlcvItems);
        }

        @Override
        public OHLCVItem consolidate(Date timestamp, List<OHLCVItem> ohlcvItems) {
            if (timestamp == null) {
                timestamp = ohlcvItems.get(0).getTimeStamp();
            }
            double volume = 0.0d;
            double volumeUp = 0.0d;
            double volumeDown = 0.0d;
            double oi = 0.0d;
            double open = ohlcvItems.get(0).getOpen();
            double close = ohlcvItems.get(ohlcvItems.size() - 1).getClose();
            double low = Double.MAX_VALUE;
            double high = -1 * Double.MAX_VALUE;

            OHLCVItemExtended ohlcvItemExtended = new OHLCVItemExtended();
            PriceVolumeContainer priceVolumeContainer = ohlcvItemExtended.getPriceVolumeContainer();

            for (OHLCVItem ohlcvItem : ohlcvItems) {
                if (ohlcvItem.getLow() < low) {
                    low = ohlcvItem.getLow();
                }
                if (ohlcvItem.getHigh() > high) {
                    high = ohlcvItem.getHigh();
                }
                volume += ohlcvItem.getVolume();
                volumeUp += ohlcvItem.getVolumeUpAsk();
                volumeDown += ohlcvItem.getVolumeDownBid();

                priceVolumeContainer.addPriceVolume(
                        ohlcvItem.getClose(), ohlcvItem.getVolumeDownBid(), ohlcvItem.getVolumeUpAsk());
            }

            OHLCVItem ohlcvItem = new OHLCVItem(timestamp, open, high, low, close, volume, oi, volumeUp, volumeDown);
            ohlcvItemExtended.setTimestamp(timestamp); // unique identifier
            ohlcvItem.setExtended(ohlcvItemExtended);

            return ohlcvItem;
        }
    }
}
