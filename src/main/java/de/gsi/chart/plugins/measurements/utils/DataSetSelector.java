package de.gsi.chart.plugins.measurements.utils;

import de.gsi.chart.XYChart;
import de.gsi.chart.data.DataSet;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

/**
 * @author rstein
 */
public class DataSetSelector extends HBox {
    private static final int DEFAULT_SELECTOR_HEIGHT = 50;
    protected final ListView<DataSet> dataSets;
    protected final ObservableList<DataSet> allDataSets;

    public DataSetSelector(final XYChart chart) {
        super();
        final Label label = new Label("Selected Dataset:");

        allDataSets = chart.getAllDatasets();
        dataSets = new ListView<>(allDataSets);
        dataSets.setOrientation(Orientation.VERTICAL);
        dataSets.setPrefSize(-1, DataSetSelector.DEFAULT_SELECTOR_HEIGHT);
        if (!allDataSets.isEmpty()) {
            dataSets.getSelectionModel().select(0);
        }

        dataSets.setCellFactory(list -> new DataSetLabel());

        getChildren().addAll(label, dataSets);
    }

    static protected class DataSetLabel extends ListCell<DataSet> {
        @Override
        public void updateItem(final DataSet item, final boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {
                setText(item.getName());
            }
        }
    }

    public int getNumberDataSets() {
        return allDataSets.size();
    }

    public DataSet getSelectedDataSet() {
        return dataSets.getSelectionModel().getSelectedItem();
    }

    // protected final ObservableList<DataSet> getAllDataSets(final XYChartPane chartPane) {
    // final ObservableList<DataSet> allDataSets = FXCollections.observableArrayList();
    // allDataSets.addAll(chartPane.getChart().getAllDatasets());
    // final ObservableList<XYChart> a = chartPane.getOverlayCharts();
    // for (final XYChart chart : a) {
    // allDataSets.addAll(chart.getAllDatasets());
    // }
    //
    // return allDataSets;
    // }
}
