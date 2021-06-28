package de.gsi.financial.samples.service.consolidate;

import de.gsi.financial.samples.dos.DefaultOHLCV;
import de.gsi.financial.samples.dos.OHLCVItem;

public interface OhlcvConsolidationAddon {
    /**
   * Base method for addon calculation process
   * @param ohlcv existed ohlcv structure
   * @param incrementItem incremental ohlc item
   * @return enhanced signal
   */
    DefaultOHLCV consolidationUpdateAddon(DefaultOHLCV ohlcv, OHLCVItem incrementItem);

    /**
   * Base method for addon calculation process
   * @param ohlcv existed ohlcv structure
   * @param incrementItem incremental ohlc item
   * @return enhanced signal
   */
    DefaultOHLCV consolidationAdditionAddon(DefaultOHLCV ohlcv, OHLCVItem incrementItem);

    /**
   * @return true = addon needs recalculation per tick in the consolidation process,
   * false = the computation is processing by new tick which create new bar. It means
   * in the end of previous closed bar - on close of bar.
   */
    boolean isDynamic();
}
