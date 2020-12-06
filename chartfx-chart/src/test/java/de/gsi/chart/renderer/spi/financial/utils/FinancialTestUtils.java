package de.gsi.chart.renderer.spi.financial.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;

import de.gsi.chart.Chart;
import de.gsi.chart.axes.Axis;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcv;
import de.gsi.dataset.utils.ProcessingProfiler;

public class FinancialTestUtils {
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

    public static void generateCosData(final DefaultErrorDataSet dataSet) {
        final long startTime = ProcessingProfiler.getTimeStamp();

        dataSet.autoNotification().set(false);
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
        dataSet.autoNotification().set(true);

        Platform.runLater(() -> dataSet.fireInvalidated(null));
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
}
