package de.gsi.chart.renderer.spi.financial.service;

import de.gsi.dataset.DataSet;

/**
 * Provides DataSet Aware
 */
public interface DataSetAware {
    DataSet getDataSet();
}
