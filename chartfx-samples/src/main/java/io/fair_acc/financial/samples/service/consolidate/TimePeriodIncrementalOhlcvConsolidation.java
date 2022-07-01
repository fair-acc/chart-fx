package io.fair_acc.financial.samples.service.consolidate;

import java.util.Calendar;
import java.util.Date;

import io.fair_acc.financial.samples.dos.Interval;
import io.fair_acc.financial.samples.dos.OHLCVItem;
import io.fair_acc.financial.samples.service.period.IntradayPeriod;

/**
 * Time based financial charts
 *
 * @author afischer
 */

public class TimePeriodIncrementalOhlcvConsolidation extends AbstractIncrementalOhlcvConsolidation {
    private final Calendar calFrom = Calendar.getInstance(); // FROM
    private final Calendar calTo = Calendar.getInstance(); // TO

    public TimePeriodIncrementalOhlcvConsolidation(OhlcvTimeframeConsolidation.OhlcvConsolidationComputation consolidationComputation,
                                                   IntradayPeriod period, Interval<Calendar> tt,
                                                   OhlcvConsolidationAddon[] _ohlcvConsolidationAddons) {
        super(consolidationComputation, period, tt, _ohlcvConsolidationAddons);
        calFrom.setFirstDayOfWeek(Calendar.SUNDAY); // US style
        calTo.setFirstDayOfWeek(Calendar.SUNDAY); // US style
    }

    @Override
    protected void defineConsolidationConditionAfterAddition(OHLCVItem finalItem) {
        defineTimeInterval(calFrom, calTo, finalItem.getTimeStamp(), period);
    }

    @Override
    protected void defineConsolidationConditionAfterUpdate(OHLCVItem finalItem) {
        // nothing to do
    }

    @Override
    protected boolean checkConsolidationCondition(OHLCVItem lastItem, OHLCVItem incrementItem) {
        // if the increment is inside in the interval - it has to be consolidated with lastItem
        Date incrementTimestamp = incrementItem.getTimeStamp();
        return incrementTimestamp.getTime() <= calTo.getTimeInMillis() && incrementTimestamp.getTime() > calFrom.getTimeInMillis();
    }

    @Override
    protected OHLCVItem processConsolidation(OHLCVItem lastItem, OHLCVItem incrementItem) {
        // for final item used the end timestamp of the re-sample interval (calTo timestamp)
        return consolidationComputation.consolidate(calTo.getTime(), lastItem, incrementItem);
    }

    /**
     * Defines consolidation condition interval
     */
    private void defineTimeInterval(Calendar calFrom, Calendar calTo, Date aDate, IntradayPeriod period) {
        double periodValue = period.getPeriodValue();
        IntradayPeriod.IntradayPeriodEnum intradayPeriodEnum = period.getPeriod();
        double toTime;
        switch (intradayPeriodEnum) {
        case S:
            toTime = 1000;
            break;
        case M:
            toTime = 60000;
            break;
        case H:
            toTime = 3600000;
            break;
        default:
            throw new IllegalArgumentException("This type of Intraday period is not supported!");
        }
        Calendar ttStart = Calendar.getInstance();
        //note: there is possibility to defined more complex time template which starts day before.
        //In this case, there is necessary to ttStart calculate by formula and decrease the day from aDate.
        ttStart.setTime(aDate);
        ttStart.set(Calendar.HOUR_OF_DAY, tt == null ? 0 : tt.from.get(Calendar.HOUR_OF_DAY));
        ttStart.set(Calendar.MINUTE, tt == null ? 0 : tt.from.get(Calendar.MINUTE));
        ttStart.set(Calendar.SECOND, 0);
        ttStart.set(Calendar.MILLISECOND, 0);

        double ttEndTime = -1.0;
        if (tt != null) {
            Calendar ttEnd = Calendar.getInstance();
            ttEnd.setTime(aDate);
            ttEnd.set(Calendar.HOUR_OF_DAY, tt.to.get(Calendar.HOUR_OF_DAY));
            ttEnd.set(Calendar.MINUTE, tt.to.get(Calendar.MINUTE));
            ttEnd.set(Calendar.SECOND, 59);
            ttEnd.set(Calendar.MILLISECOND, 999999999);

            ttEndTime = ttEnd.getTime().getTime() / toTime;
        }

        double aDateTime = aDate.getTime() / toTime;
        double ttStartTime = ttStart.getTime().getTime() / toTime;

        double diff = aDateTime - ttStartTime;
        double n = Math.floor(diff / periodValue);

        double resampleFrom;
        double resampleTo;
        resampleFrom = ttStartTime + periodValue * (diff % periodValue == 0 ? n - 1 : n);
        resampleTo = resampleFrom + periodValue;
        if (tt != null && resampleTo >= ttEndTime) {
            resampleTo = ttEndTime;
        }

        calFrom.setTime(new Date(Math.round(resampleFrom * toTime)));
        calTo.setTime(new Date(Math.round(resampleTo * toTime)));
    }
}
