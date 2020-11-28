package de.gsi.chart.samples.financial.service.consolidate;

import de.gsi.chart.samples.financial.dos.Interval;
import de.gsi.chart.samples.financial.dos.OHLCVItem;
import de.gsi.chart.samples.financial.service.consolidate.OhlcvTimeframeConsolidation.OhlcvConsolidationComputation;
import de.gsi.chart.samples.financial.service.period.IntradayPeriod;

import java.util.Calendar;

public class VolumeIncrementalOhlcvConsolidation extends AbstractIncrementalOhlcvConsolidation {
    private double volumeDiff;
    private final double volumePeriod;

    public VolumeIncrementalOhlcvConsolidation(OhlcvConsolidationComputation consolidationComputation,
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
