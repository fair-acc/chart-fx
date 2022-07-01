package io.fair_acc.chartfx.renderer.spi.financial.service;

/**
 * Extension point service for Renderers
 * Placement: Paint After bar painting
 */
@FunctionalInterface
public interface RendererPaintAfterEP {
    void paintAfter(OhlcvRendererEpData data);
}
