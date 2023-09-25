package io.fair_acc.sample.financial.service.consolidate;

import java.util.Calendar;

import io.fair_acc.sample.financial.dos.Interval;
import io.fair_acc.sample.financial.dos.OHLCVItem;
import io.fair_acc.sample.financial.service.period.IntradayPeriod;

/**
 * Volume based financial charts
 *
 * @author afischer
 */
public class VolumeIncrementalOhlcvConsolidation extends AbstractIncrementalOhlcvConsolidation {
    private double volumeDiff;
    private final double volumePeriod;

    public VolumeIncrementalOhlcvConsolidation(OhlcvTimeframeConsolidation.OhlcvConsolidationComputation consolidationComputation,
            IntradayPeriod period, Interval<Calendar> tt,
            OhlcvConsolidationAddon[] _ohlcvConsolidationAddons) {
        super(consolidationComputation, period, tt, _ohlcvConsolidationAddons);
        this.volumePeriod = period.getPeriodValue();
    }

    @Override
    protected void defineConsolidationConditionAfterAddition(OHLCVItem finalItem) {
        defineConsolidationCondition(finalItem);
    }

    @Override
    protected void defineConsolidationConditionAfterUpdate(OHLCVItem finalItem) {
        defineConsolidationCondition(finalItem);
    }

    @Override
    protected boolean checkConsolidationCondition(OHLCVItem lastItem, OHLCVItem incrementItem) {
        return incrementItem.getVolume() <= volumeDiff;
    }

    private void defineConsolidationCondition(OHLCVItem finalItem) {
        volumeDiff = volumePeriod - finalItem.getVolume();
    }
}
