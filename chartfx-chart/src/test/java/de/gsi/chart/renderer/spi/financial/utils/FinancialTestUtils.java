package de.gsi.chart.renderer.spi.financial.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcv;

public class FinancialTestUtils {
    public static IOhlcv createTestOhlcv() {
        LocalDate date = LocalDate.parse("2020-11-19");
        return new Ohlcv()
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(1)), 3001.0, 3005.0, 3000.10, 3002.84, 15001.0, 18007.0))
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(1)), 3002.0, 3007.0, 3001.35, 3005.64, 16005.0, 19002.0))
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(1)), 3003.0, 3009.15, 3002.50, 3008.75, 14004.0, 20005.0))
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(1)), 2999.0, 3000.75, 2997.15, 2998.10, 100085.0, 35001.0))
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(1)), 2996.0, 2998.0, 2994.10, 2993.50, 135001.0, 64010.0));
    }

    public static Date toDate(LocalDate ldate) {
        return Date.from(ldate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
