package io.fair_acc.chartfx.renderer.spi.financial.service;

import io.fair_acc.dataset.DataSet;

/**
 * Provides DataSet Aware
 */
public interface DataSetAware {
    DataSet getDataSet();
}
