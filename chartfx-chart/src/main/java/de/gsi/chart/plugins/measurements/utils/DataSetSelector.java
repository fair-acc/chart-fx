package de.gsi.chart.plugins.measurements.utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.layout.HBox;

import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.dataset.DataSet;

/**
 * @author rstein
 */
public class DataSetSelector extends HBox {
    private static final int DEFAULT_SELECTOR_HEIGHT = 50;
    protected final ListView<DataSet> dataSetListView;
    protected final ObservableList<DataSet> allDataSets;

    public DataSetSelector(final ParameterMeasurements plugin) {
        super();
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }

        // wrap observable Array List, to prevent resetting the selection model whenever getAllDatasets() is called
        // somewhere in the code.
        allDataSets = plugin.getChart() != null ? FXCollections.observableArrayList(plugin.getChart().getAllDatasets()) : FXCollections.emptyObservableList();
        dataSetListView = new ListView<>(allDataSets);
        dataSetListView.setOrientation(Orientation.VERTICAL);
        dataSetListView.setPrefSize(-1, DEFAULT_SELECTOR_HEIGHT);
        MultipleSelectionModel<DataSet> selModel = dataSetListView.getSelectionModel();
        if (!allDataSets.isEmpty()) {
            selModel.select(0);
        }

        dataSetListView.setCellFactory(list -> new DataSetLabel());
        final Label label = new Label("Selected Dataset:");
        getChildren().addAll(label, dataSetListView);
    }

    public int getNumberDataSets() {
        return allDataSets.size();
    }

    public DataSet getSelectedDataSet() {
        MultipleSelectionModel<DataSet> selModel = dataSetListView.getSelectionModel();
        return selModel.getSelectedItem();
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
