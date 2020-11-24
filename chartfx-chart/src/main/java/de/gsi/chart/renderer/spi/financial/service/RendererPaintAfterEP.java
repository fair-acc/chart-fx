package de.gsi.chart.renderer.spi.financial.service;

import de.gsi.chart.renderer.spi.financial.service.OhlcvRendererEpData;

/**
 * Extension point service for Renderers
 * Placement: Paint After bar painting
 */
@FunctionalInterface
public interface RendererPaintAfterEP {
    void paintAfter(OhlcvRendererEpData data);
}
