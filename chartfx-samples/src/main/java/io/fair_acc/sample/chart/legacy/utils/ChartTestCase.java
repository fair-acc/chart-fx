package io.fair_acc.sample.chart.legacy.utils;

import javafx.scene.Node;

public interface ChartTestCase {

    Node getChart(final int nSamples);

    void setNumberOfSamples(final int nSamples);

    void updateDataSet();
}
