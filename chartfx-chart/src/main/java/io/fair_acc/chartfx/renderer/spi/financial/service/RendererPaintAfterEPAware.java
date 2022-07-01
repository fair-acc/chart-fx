package io.fair_acc.chartfx.renderer.spi.financial.service;

import java.util.List;

/**
 * Provides DataSet Aware
 */
public interface RendererPaintAfterEPAware {
    /**
     * Inject extension point for Paint after bar.
     *
     * @param paintAfterEP service
     */
    void addPaintAfterEp(RendererPaintAfterEP paintAfterEP);

    /**
     * Get Paint after extension points
     *
     * @return list of the Paint after extension points.
     */
    List<RendererPaintAfterEP> getPaintAfterEps();
}
