/**
 * LGPL-3.0, 2020/21, GSI-CS-CO/Chart-fx, BTA HF OpenSource Java-FX Branch, Financial Charts
 */
package de.gsi.chart.samples.financial.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import de.gsi.chart.samples.financial.dos.DefaultOHLCV;
import de.gsi.chart.samples.financial.dos.OHLCVItem;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcv;
import de.gsi.dataset.utils.StreamUtils;

/**
 * Simple Tradestation OHLC data parser.
 *
 * @author afischer
 */
public class SimpleOhlcvDailyParser {
    private static final String CHART_SAMPLE_PATH = StreamUtils.CLASSPATH_PREFIX + "de/gsi/chart/samples/financial/%s.csv";

    private static final ConcurrentDateFormatAccess dateFormatParsing = new ConcurrentDateFormatAccess("MM/dd/yyyy HH:mm");
    private static final ConcurrentDateFormatAccess olderDateFormatParsing = new ConcurrentDateFormatAccess("MM/dd/yyyy HHmm");

    public IOhlcv getContinuousOHLCV(String future) throws IOException, NumberFormatException {
        String resource = String.format(CHART_SAMPLE_PATH, future);

        try (InputStreamReader is = new InputStreamReader(StreamUtils.getInputStream(resource));
                BufferedReader br = new BufferedReader(is)) {
            return convertTsRowStream(future, br.lines(), new DailyOhlcvItemRowParser());
        }
    }

    /**
     * Convert OHLCV string row stream. Each line is specific OHLCV structure from Tradestation String stream.
     * Correct parse of each line has to be used and inserted to this method.
     */
    private static IOhlcv convertTsRowStream(String title,
            Stream<String> rowStream,
            TradeStationRowParser parser) throws NumberFormatException {
        var ref = new TSConvertSettings(true, true);
        String symbol = new File(title).getName().replaceFirst("[.][^.]+$", "");
        Calendar cal = Calendar.getInstance();
        List<OHLCVItem> items = new ArrayList<>();

        DefaultOHLCV ohlcvOutput = new DefaultOHLCV();
        ohlcvOutput.setTitle(title);
        ohlcvOutput.setName(title);
        ohlcvOutput.setAssetName(title);
        ohlcvOutput.setSymbol(symbol);

        rowStream.forEach(r -> {
            OHLCVItem ohlcvItem = parser.parse(ref, cal, r);
            if (ohlcvItem == null)
                return;
            items.add(ohlcvItem);
        });
        ohlcvOutput.addOhlcvItems(items);

        return ohlcvOutput;
    }

    private static class TSConvertSettings {
        boolean header;
        boolean useNewStyle;

        public TSConvertSettings(boolean header, boolean useNewStyle) {
            this.header = header;
            this.useNewStyle = useNewStyle;
        }
    }

    @FunctionalInterface
    private interface TradeStationRowParser {
        OHLCVItem parse(TSConvertSettings ref, Calendar cal, String r);
    }

    /**
     * Read OHLCV structure from Tradestation export - table structure
     * "Date","Time","Open","High","Low","Close","Vol","OI"
     * 01/03/2007,09:00,778.30,780.10,775.40,775.70,3300,3224
     * 01/03/2007,09:30,775.50,781.40,775.20,780.10,2128,1711
     */
    private static class DailyOhlcvItemRowParser implements TradeStationRowParser {
        @Override
        public OHLCVItem parse(TSConvertSettings ref, Calendar cal, String r) {
            if (ref.header) {
                ref.header = false;
                return null;
            }
            String[] row = r.split(",");

            Date timestamp;
            try {
                if (ref.useNewStyle) {
                    timestamp = dateFormatParsing.parse(row[0] + " " + row[1]);
                } else {
                    timestamp = olderDateFormatParsing.parse(row[0] + " " + row[1]);
                }
            } catch (ParseException e) {
                try {
                    timestamp = olderDateFormatParsing.parse(row[0] + " " + row[1]);
                } catch (ParseException parseException) {
                    throw new IllegalArgumentException("Wrong format of daily data row=" + row[0] + " " + row[1]);
                }
                ref.useNewStyle = false;
            }
            return new OHLCVItem(
                    timestamp,
                    Double.parseDouble(row[2]),
                    Double.parseDouble(row[3]),
                    Double.parseDouble(row[4]),
                    Double.parseDouble(row[5]),
                    Double.parseDouble(row[6]),
                    Double.parseDouble(row[7]));
        }
    }
}
