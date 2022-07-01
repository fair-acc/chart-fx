package io.fair_acc.financial.samples.service.consolidate;

import java.util.Calendar;

import io.fair_acc.financial.samples.dos.DefaultOHLCV;
import io.fair_acc.financial.samples.dos.Interval;
import io.fair_acc.financial.samples.dos.OHLCVItem;
import io.fair_acc.financial.samples.service.consolidate.OhlcvTimeframeConsolidation.OhlcvConsolidationComputation;
import io.fair_acc.financial.samples.service.consolidate.OhlcvTimeframeConsolidation.StandardOhlcvConsolidationComputation;
import io.fair_acc.financial.samples.service.period.IntradayPeriod;

/**
 * Incremental consolidation based class for OHLCV structures.
 * It can be used for time period, range bars and volume graphs.
 */
public abstract class AbstractIncrementalOhlcvConsolidation implements IncrementalOhlcvConsolidation {
    protected IntradayPeriod period;
    protected OhlcvConsolidationComputation consolidationComputation;
    protected OhlcvConsolidationAddon[] ohlcvConsolidationAddons;
    protected Interval<Calendar> tt;

    private OHLCVItem lastItem;

    public AbstractIncrementalOhlcvConsolidation(OhlcvConsolidationComputation consolidationComputation,
            IntradayPeriod period, Interval<Calendar> tt,
            OhlcvConsolidationAddon[] ohlcvConsolidationAddons) {
        this.period = period;
        this.consolidationComputation = consolidationComputation == null ? new StandardOhlcvConsolidationComputation() : consolidationComputation;
        this.ohlcvConsolidationAddons = ohlcvConsolidationAddons;
        this.tt = tt;
    }

    @Override
    public IntradayPeriod getPeriod() {
        return period;
    }

    @Override
    public DefaultOHLCV consolidate(DefaultOHLCV ohlcv, OHLCVItem incrementItem) {
        if (lastItem == null) {
            lastItem = incrementItem;
        }

        if (checkConsolidationCondition(lastItem, incrementItem)) {
            OHLCVItem finalItem = processConsolidation(lastItem, incrementItem);
            ohlcv.updateOhlcvItem(ohlcv.size() - 1, finalItem);
            lastItem = finalItem;
            defineConsolidationConditionAfterUpdate(lastItem);
            processConsolidationAddonsInUpdate(ohlcv, incrementItem);

        } else {
            ohlcv.addOhlcvItem(incrementItem);
            lastItem = incrementItem;
            defineConsolidationConditionAfterAddition(lastItem);
            processConsolidationAddonsInAddition(ohlcv, incrementItem);
        }

        return ohlcv;
    }

    protected void processConsolidationAddonsInUpdate(DefaultOHLCV ohlcv, OHLCVItem incrementItem) {
        if (ohlcvConsolidationAddons != null) {
            for (int i = 0; i < ohlcvConsolidationAddons.length; i++) {
                if (ohlcvConsolidationAddons[i].isDynamic()) {
                    ohlcvConsolidationAddons[i].consolidationUpdateAddon(ohlcv, incrementItem);
                }
            }
        }
    }

    protected void processConsolidationAddonsInAddition(DefaultOHLCV ohlcv, OHLCVItem incrementItem) {
        if (ohlcvConsolidationAddons != null) {
            for (int i = 0; i < ohlcvConsolidationAddons.length; i++) {
                ohlcvConsolidationAddons[i].consolidationAdditionAddon(ohlcv, incrementItem);
            }
        }
    }

    /**
     * Define consolidation condition after addition for next checking of performConsolidation method
     *
     * @param finalItem for definition consolidation condition
     */
    protected abstract void defineConsolidationConditionAfterAddition(OHLCVItem finalItem);

    /**
     * Define consolidation condition after update for next checking of performConsolidation method
     *
     * @param finalItem for definition consolidation condition
     */
    protected abstract void defineConsolidationConditionAfterUpdate(OHLCVItem finalItem);

    /**
     * Different test for consolidation defined by IntradayPeriod instance
     *
     * @param lastItem      of consolidated structure
     * @param incrementItem tick which will be increased to the consolidation structure
     * @return true = consolidation process has to be performed
     */
    protected abstract boolean checkConsolidationCondition(OHLCVItem lastItem, OHLCVItem incrementItem);

    /**
     * Process consolidation process with actual increment.
     * Standard or extended (footpring) consolidation processing of OHLCV structure
     *
     * @param lastItem      of consolidated structure
     * @param incrementItem tick which will be increased to the consolidation structure
     * @return consolidated ohlcv item
     */
    protected OHLCVItem processConsolidation(OHLCVItem lastItem, OHLCVItem incrementItem) {
        // use servant for this processing
        return consolidationComputation.consolidate(lastItem.getTimeStamp(), lastItem, incrementItem);
    }
}
