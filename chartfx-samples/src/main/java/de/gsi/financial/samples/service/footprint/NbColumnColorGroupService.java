package de.gsi.financial.samples.service.footprint;

import de.gsi.chart.renderer.spi.financial.service.footprint.NbColumnColorGroup;
import de.gsi.financial.samples.dos.PriceVolumeContainer;

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
