package de.gsi.chart.samples.financial.service.consolidate;

import de.gsi.chart.samples.financial.dos.DefaultOHLCV;
import de.gsi.chart.samples.financial.dos.OHLCVItem;
import de.gsi.chart.samples.financial.service.period.IntradayPeriod;

public interface IncrementalOhlcvConsolidation {
    /**
   * Base method for incremental consolidation process
   * @param ohlcv existed consolidated ohlcv structure
   * @param incrementItem tick actual ohlcv item
   * @return consolidated signal
   */
    DefaultOHLCV consolidate(DefaultOHLCV ohlcv, OHLCVItem incrementItem);

    /**
   * @return provides information about consolidation settings period
   */
    IntradayPeriod getPeriod();
}
