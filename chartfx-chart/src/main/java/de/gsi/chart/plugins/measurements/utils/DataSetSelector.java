package de.gsi.chart.plugins.measurements.utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.GridPane;

import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.dataset.DataSet;

/**
 * @author rstein
 */
public class DataSetSelector extends GridPane {
    private static final int DEFAULT_SELECTOR_HEIGHT = 50;
    private static final int ROW_HEIGHT = 24;
    protected final ListView<DataSet> dataSetListView;
    protected final ObservableList<DataSet> allDataSets;

    public DataSetSelector(final ParameterMeasurements plugin, final int requiredNumberOfDataSets) {
        super();
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }

        // wrap observable Array List, to prevent resetting the selection model whenever getAllDatasets() is called
        // somewhere in the code.
        allDataSets = plugin.getChart() != null ? FXCollections.observableArrayList(plugin.getChart().getAllDatasets()) : FXCollections.emptyObservableList();

        final Label label = new Label("Selected Dataset: ");
        GridPane.setConstraints(label, 0, 0);
        dataSetListView = new ListView<>(allDataSets);
        GridPane.setConstraints(dataSetListView, 1, 0);
        dataSetListView.setOrientation(Orientation.VERTICAL);
        dataSetListView.setPrefSize(-1, DEFAULT_SELECTOR_HEIGHT);
        dataSetListView.setCellFactory(list -> new DataSetLabel());
        dataSetListView.setPrefHeight(Math.max(2, allDataSets.size()) * ROW_HEIGHT + 2.0);
        MultipleSelectionModel<DataSet> selModel = dataSetListView.getSelectionModel();
        if (requiredNumberOfDataSets == 1) {
            selModel.setSelectionMode(SelectionMode.SINGLE);
        } else if (requiredNumberOfDataSets >= 2) {
            selModel.setSelectionMode(SelectionMode.MULTIPLE);
        }

        // add default initially selected DataSets
        if (selModel.getSelectedIndices().isEmpty() && allDataSets.size() >= requiredNumberOfDataSets) {
            for (int i = 0; i < requiredNumberOfDataSets; i++) {
                selModel.select(i);
            }
        }

        if (requiredNumberOfDataSets >= 1) {
            getChildren().addAll(label, dataSetListView);
        }
    }

    public int getNumberDataSets() {
        return allDataSets.size();
    }

    public DataSet getSelectedDataSet() {
        MultipleSelectionModel<DataSet> selModel = dataSetListView.getSelectionModel();
        return selModel.getSelectedItem();
    }

    public ObservableList<DataSet> getSelectedDataSets() {
        MultipleSelectionModel<DataSet> selModel = dataSetListView.getSelectionModel();
        return selModel.getSelectedItems();
    }

    public ListView<DataSet> getDataSetListView() {
        return dataSetListView;
    }

    protected static class DataSetLabel extends ListCell<DataSet> {
        @Override
        public void updateItem(final DataSet item, final boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {
                setText(item.getName());
            }
        }
    }
}
