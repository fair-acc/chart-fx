package io.fair_acc.financial.samples.service.footprint;

import io.fair_acc.chartfx.renderer.spi.financial.service.footprint.NbColumnColorGroup;
import io.fair_acc.financial.samples.dos.PriceVolumeContainer;

/**
 * Calculate color group settings for each bid/ask volume in each level price
 */
public interface NbColumnColorGroupService {
    /**
     * Calculate color group settings for each bid/ask volume in each level price
     * @param priceVolumeContainer which has to be painted
     * @return the result with column color group data result
     */
    NbColumnColorGroup calculate(PriceVolumeContainer priceVolumeContainer);
}
