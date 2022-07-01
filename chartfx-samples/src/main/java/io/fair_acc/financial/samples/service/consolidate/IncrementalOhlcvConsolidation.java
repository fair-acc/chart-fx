package io.fair_acc.financial.samples.service.consolidate;

import io.fair_acc.financial.samples.dos.DefaultOHLCV;
import io.fair_acc.financial.samples.dos.OHLCVItem;
import io.fair_acc.financial.samples.service.period.IntradayPeriod;

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
