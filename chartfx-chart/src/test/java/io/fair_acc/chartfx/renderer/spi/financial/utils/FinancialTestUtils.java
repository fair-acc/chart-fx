package io.fair_acc.chartfx.renderer.spi.financial.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.dataset.spi.DefaultErrorDataSet;
import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcv;
import io.fair_acc.dataset.utils.ProcessingProfiler;

public class FinancialTestUtils {
    private static final String TS_TIMESTAMP_FORMAT = "MM/dd/yyyy HH:mm";
    private static final int N_SAMPLES = 10_000; // default: 10000

    public static IOhlcv createTestOhlcv() {
        LocalDate date = LocalDate.parse("2020-11-19");
        return new Ohlcv()
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(1)), 3001.0, 3005.0, 3000.10, 3002.84, 15001.0, 18007.0))
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(2)), 3002.0, 3007.0, 3001.35, 3005.64, 16005.0, 19002.0))
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(3)), 3003.0, 3009.15, 3002.50, 3008.75, 14004.0, 20005.0))
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(4)), 2999.0, 3000.75, 2997.15, 2998.10, 100085.0, 35001.0))
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(5)), 2996.0, 2998.0, 2990.10, 2992.50, 135001.0, 64010.0));
    }

    public static IOhlcv createTestOhlcv2() {
        try {
            return new Ohlcv()
                    .addOhlcvItem(parseOhlcvItem("09/01/2020,16:00,3483.00000,3519.75000,3474.00000,3516.75000,1283622,2696614"))
                    .addOhlcvItem(parseOhlcvItem("09/02/2020,16:00,3518.75000,3576.75000,3516.00000,3569.00000,1853078,2680985"))
                    .addOhlcvItem(parseOhlcvItem("09/03/2020,16:00,3568.50000,3576.25000,3414.25000,3451.25000,2872337,2665365"))
                    .addOhlcvItem(parseOhlcvItem("09/04/2020,16:00,3444.50000,3474.00000,3337.50000,3407.25000,2994363,2621756"))
                    .addOhlcvItem(parseOhlcvItem("09/08/2020,16:00,3410.25000,3436.75000,3317.25000,3325.25000,2732946,2580018"))
                    .addOhlcvItem(parseOhlcvItem("09/09/2020,16:00,3305.00000,3413.75000,3285.25000,3390.00000,85922,155841"))
                    .addOhlcvItem(parseOhlcvItem("09/10/2020,16:00,3386.50000,3414.00000,3316.75000,3330.00000,544080,379776"))
                    .addOhlcvItem(parseOhlcvItem("09/11/2020,16:00,3334.75000,3364.75000,3298.25000,3323.25000,2034367,961009"))
                    .addOhlcvItem(parseOhlcvItem("09/14/2020,16:00,3340.00000,3392.50000,3336.25000,3372.25000,2219334,1697593"))
                    .addOhlcvItem(parseOhlcvItem("09/15/2020,16:00,3369.75000,3409.00000,3366.25000,3395.00000,1903406,2164654"))
                    .addOhlcvItem(parseOhlcvItem("09/16/2020,16:00,3395.75000,3419.50000,3373.75000,3379.50000,1787505,2384985"))
                    .addOhlcvItem(parseOhlcvItem("09/17/2020,16:00,3381.50000,3386.50000,3310.25000,3351.00000,2676297,2470907"))
                    .addOhlcvItem(parseOhlcvItem("09/18/2020,16:00,3345.00000,3363.00000,3280.75000,3316.25000,2365554,2491348"))
                    .addOhlcvItem(parseOhlcvItem("09/21/2020,16:00,3314.50000,3326.25000,3217.75000,3275.00000,2507980,2486960"))
                    .addOhlcvItem(parseOhlcvItem("09/22/2020,16:00,3271.50000,3309.50000,3256.50000,3299.25000,1576319,2467130"))
                    .addOhlcvItem(parseOhlcvItem("09/23/2020,16:00,3303.75000,3319.75000,3221.00000,3231.25000,1979661,2461928"))
                    .addOhlcvItem(parseOhlcvItem("09/24/2020,16:00,3228.00000,3268.25000,3198.00000,3238.00000,1697090,2472127"))
                    .addOhlcvItem(parseOhlcvItem("09/25/2020,16:00,3243.50000,3296.25000,3206.50000,3287.25000,1697090,2471785"))
                    .addOhlcvItem(parseOhlcvItem("09/28/2020,16:00,3291.00000,3351.25000,3287.50000,3346.00000,1495804,2462903"))
                    .addOhlcvItem(parseOhlcvItem("09/29/2020,16:00,3348.75000,3363.00000,3316.50000,3333.75000,1326597,2461168"))
                    .addOhlcvItem(parseOhlcvItem("09/30/2020,16:00,3330.00000,3384.00000,3291.25000,3352.00000,2170601,2469121"))
                    .addOhlcvItem(parseOhlcvItem("10/01/2020,16:00,3344.75000,3388.00000,3343.25000,3367.75000,1517912,2472707"))
                    .addOhlcvItem(parseOhlcvItem("10/02/2020,16:00,3367.00000,3375.50000,3300.25000,3339.25000,2028921,2460288"))
                    .addOhlcvItem(parseOhlcvItem("10/05/2020,16:00,3360.00000,3400.00000,3346.50000,3393.00000,1077474,2446841"))
                    .addOhlcvItem(parseOhlcvItem("10/06/2020,16:00,3392.00000,3421.75000,3330.50000,3353.25000,1701062,2460750"))
                    .addOhlcvItem(parseOhlcvItem("10/07/2020,16:00,3338.00000,3416.75000,3332.25000,3406.75000,1400616,2460030"))
                    .addOhlcvItem(parseOhlcvItem("10/08/2020,16:00,3406.25000,3447.25000,3405.50000,3437.50000,1104986,2456991"))
                    .addOhlcvItem(parseOhlcvItem("10/09/2020,16:00,3447.25000,3480.00000,3445.25000,3473.25000,1185272,2462189"))
                    .addOhlcvItem(parseOhlcvItem("10/12/2020,16:00,3467.50000,3541.00000,3464.25000,3532.75000,1401063,2445766"))
                    .addOhlcvItem(parseOhlcvItem("10/13/2020,16:00,3534.00000,3535.75000,3491.50000,3504.75000,1617226,2450104"))
                    .addOhlcvItem(parseOhlcvItem("10/14/2020,16:00,3502.00000,3524.00000,3472.00000,3481.00000,1539718,2426698"))
                    .addOhlcvItem(parseOhlcvItem("10/15/2020,16:00,3479.25000,3486.50000,3431.50000,3475.50000,1650150,2435548"))
                    .addOhlcvItem(parseOhlcvItem("10/16/2020,16:00,3478.50000,3508.50000,3461.25000,3462.25000,1398947,2403234"))
                    .addOhlcvItem(parseOhlcvItem("10/19/2020,16:00,3471.00000,3496.50000,3410.75000,3422.75000,1645853,2406069"))
                    .addOhlcvItem(parseOhlcvItem("10/20/2020,16:00,3436.00000,3469.75000,3418.75000,3432.25000,1702183,2411689"))
                    .addOhlcvItem(parseOhlcvItem("10/21/2020,16:00,3437.00000,3458.00000,3420.50000,3432.50000,1681275,2414019"))
                    .addOhlcvItem(parseOhlcvItem("10/22/2020,16:00,3430.25000,3453.00000,3402.50000,3449.25000,1501259,2420827"))
                    .addOhlcvItem(parseOhlcvItem("10/23/2020,16:00,3455.75000,3462.50000,3431.50000,3451.75000,1216313,2394156"))
                    .addOhlcvItem(parseOhlcvItem("10/26/2020,16:00,3445.75000,3446.25000,3356.00000,3393.50000,1957780,2389403"))
                    .addOhlcvItem(parseOhlcvItem("10/27/2020,16:00,3394.25000,3410.00000,3368.25000,3383.00000,1566246,2410879"))
                    .addOhlcvItem(parseOhlcvItem("10/28/2020,16:00,3369.00000,3370.25000,3260.75000,3263.50000,2627180,2437565"))
                    .addOhlcvItem(parseOhlcvItem("10/29/2020,16:00,3279.00000,3333.75000,3250.25000,3302.25000,2494925,2404197"))
                    .addOhlcvItem(parseOhlcvItem("10/30/2020,16:00,3268.00000,3296.25000,3225.00000,3264.75000,2494925,2401075"));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Not correctly configured! e=" + e.getMessage(), e);
        }
    }

    // Syntax "09/01/2020,16:00,3483.00000,3519.75000,3474.00000,3516.75000,1283622,2696614"
    public static OhlcvItem parseOhlcvItem(String ohlcvItemString) throws ParseException {
        String[] parts = ohlcvItemString.split(",");
        return new OhlcvItem(createByTradeStationDateTime(parts[0] + " " + parts[1]).getTime(),
                Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4]),
                Double.parseDouble(parts[5]), Double.parseDouble(parts[6]), Double.parseDouble(parts[7]));
    }

    public static void generateCosData(final DefaultErrorDataSet dataSet) {
        final long startTime = ProcessingProfiler.getTimeStamp();

        dataSet.clearData();
        final double now = System.currentTimeMillis() / 1000.0 + 1; // N.B. '+1'
        for (int n = 0; n < N_SAMPLES; n++) {
            double t = now + n * 10;
            t *= +1;
            final double y = 100 * Math.cos(Math.PI * t * 0.0005) + 0 * 0.001 * (t - now) + 0 * 1e4;
            final double ex = 0.1;
            final double ey = 10;
            dataSet.add(t, y, ex, ey);
        }
        ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    public static Date toDate(LocalDate ldate) {
        return Date.from(ldate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @SuppressWarnings({ "PMD.UncommentedEmptyMethodBody" })
    public static class TestChart extends Chart {
        @Override
        public void updateAxisRange() {
            // use for test only
        }

        @Override
        protected void axesChanged(ListChangeListener.Change<? extends Axis> change) {
            // use for test only
        }

        @Override
        protected void redrawCanvas() {
            // use for test only
        }

        public Label getTitlePaint() {
            return titleLabel;
        }
    }

    //-------------- helpers ----------------

    private static Calendar createByTradeStationDateTime(String datetimePattern) throws ParseException {
        if (datetimePattern == null) {
            throw new ParseException("The resource datetime pattern is null", -1);
        }
        SimpleDateFormat sdf = new SimpleDateFormat(TS_TIMESTAMP_FORMAT);
        Date fromTotime = sdf.parse(datetimePattern);
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromTotime);
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0);

        return cal;
    }
}
