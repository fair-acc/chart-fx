package de.gsi.chart.plugins.measurements.utils;

import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.GridPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.measurements.AbstractChartMeasurement;
import de.gsi.chart.plugins.measurements.DataSetMeasurements;

/**
 * @author rstein
 */
public class ChartMeasurementSelector extends GridPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChartMeasurementSelector.class);
    private static final int DEFAULT_SELECTOR_HEIGHT = 50;
    private static final int ROW_HEIGHT = 24;
    protected final ListView<AbstractChartMeasurement> chartMeasurementListView;

    public ChartMeasurementSelector(final ParameterMeasurements plugin, final AbstractChartMeasurement dataSetMeasurement, final int requiredNumberOfChartMeasurements) {
        super();
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }

        final Label label = new Label("Selected Measurement: ");
        GridPane.setConstraints(label, 0, 0);
        chartMeasurementListView = new ListView<>(plugin.getChartMeasurements().filtered(t -> !t.equals(dataSetMeasurement)));
        GridPane.setConstraints(chartMeasurementListView, 1, 0);
        chartMeasurementListView.setOrientation(Orientation.VERTICAL);
        chartMeasurementListView.setPrefSize(-1, DEFAULT_SELECTOR_HEIGHT);
        chartMeasurementListView.setCellFactory(list -> new ChartMeasurementLabel());
        chartMeasurementListView.setPrefHeight(Math.max(2, plugin.getChartMeasurements().size()) * ROW_HEIGHT + 2.0);
        MultipleSelectionModel<AbstractChartMeasurement> selModel = chartMeasurementListView.getSelectionModel();
        if (requiredNumberOfChartMeasurements == 1) {
            selModel.setSelectionMode(SelectionMode.SINGLE);
        } else if (requiredNumberOfChartMeasurements >= 2) {
            selModel.setSelectionMode(SelectionMode.MULTIPLE);
        }

        // add default initially selected ChartMeasurements
        if (selModel.getSelectedIndices().isEmpty() && plugin.getChartMeasurements().size() >= requiredNumberOfChartMeasurements) {
            for (int i = 0; i < requiredNumberOfChartMeasurements; i++) {
                selModel.select(i);
            }
        }

        if (selModel.getSelectedIndices().size() < requiredNumberOfChartMeasurements && LOGGER.isWarnEnabled()) {
            LOGGER.atWarn().addArgument(plugin.getChartMeasurements().size()).addArgument(requiredNumberOfChartMeasurements).log("could not add default selection: required {} vs. selected {}");
        }

        if (requiredNumberOfChartMeasurements >= 1) {
            getChildren().addAll(label, chartMeasurementListView);
        }
    }

    public AbstractChartMeasurement getSelectedChartMeasurement() {
        MultipleSelectionModel<AbstractChartMeasurement> selModel = chartMeasurementListView.getSelectionModel();
        return selModel.getSelectedItem();
    }

    public ObservableList<AbstractChartMeasurement> getSelectedChartMeasurements() {
        MultipleSelectionModel<AbstractChartMeasurement> selModel = chartMeasurementListView.getSelectionModel();
        return selModel.getSelectedItems();
    }

    protected static class ChartMeasurementLabel extends ListCell<AbstractChartMeasurement> {
        @Override
        public void updateItem(final AbstractChartMeasurement item, final boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {
                final String dataSetName = item.getDataSet() == null ? "n/a" : item.getDataSet().getName();
                setText(item.getTitle() + "<" + dataSetName + ">");
            }
        }
    }
}
