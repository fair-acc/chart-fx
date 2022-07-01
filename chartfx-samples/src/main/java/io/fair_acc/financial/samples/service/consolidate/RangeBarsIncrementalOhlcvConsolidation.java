package io.fair_acc.financial.samples.service.consolidate;

import java.util.Calendar;

import io.fair_acc.financial.samples.dos.Interval;
import io.fair_acc.financial.samples.dos.OHLCVItem;
import io.fair_acc.financial.samples.service.period.IntradayPeriod;

/**
 * Range-Bars based financial charts
 *
 * @author afischer
 */

public class RangeBarsIncrementalOhlcvConsolidation extends AbstractIncrementalOhlcvConsolidation {
    private final double rangeBarsLength;
    private boolean hasLength = true; // declare first bar
    private double low;
    private double high;

    public RangeBarsIncrementalOhlcvConsolidation(OhlcvTimeframeConsolidation.OhlcvConsolidationComputation consolidationComputation,
                                                  IntradayPeriod period, double minMoveTick, Interval<Calendar> tt,
                                                  OhlcvConsolidationAddon[] ohlcvConsolidationAddons) {
        super(consolidationComputation, period, tt, ohlcvConsolidationAddons);
        this.rangeBarsLength = period.getPeriodValue() * minMoveTick;
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
        return !hasLength || incrementItem.getLow() >= low && incrementItem.getHigh() <= high;
    }

    private void defineConsolidationCondition(OHLCVItem finalItem) {
        low = finalItem.getLow();
        high = finalItem.getHigh();
        hasLength = rangeBarsLength <= (high - low);
    }
}
