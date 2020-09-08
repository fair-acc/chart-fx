package de.gsi.chart.plugins.measurements.utils;

import static de.gsi.chart.axes.AxisMode.X;
import static de.gsi.chart.axes.AxisMode.Y;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.GridPane;

import de.gsi.chart.Chart;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.plugins.AbstractSingleValueIndicator;
import de.gsi.chart.plugins.ChartPlugin;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.XValueIndicator;
import de.gsi.chart.plugins.YValueIndicator;
import de.gsi.dataset.utils.NoDuplicatesList;

public class ValueIndicatorSelector extends GridPane {
    private static final int DEFAULT_SELECTOR_HEIGHT = 100;
    private static final int ROW_HEIGHT = 24;
    private final ObservableList<AbstractSingleValueIndicator> valueIndicators = FXCollections.observableArrayList(new NoDuplicatesList<>());
    private final ObservableList<AbstractSingleValueIndicator> valueIndicatorsUser = FXCollections.observableArrayList(new NoDuplicatesList<>());
    protected final ListView<AbstractSingleValueIndicator> indicatorListView = new ListView<>(valueIndicators);
    private final AxisMode axisMode;
    protected final CheckBox reuseIndicators = new CheckBox();
    protected ListChangeListener<? super ChartPlugin> pluginsChanged = change -> {
        while (change.next()) {
            change.getRemoved().forEach(this::removeIndicators);
            change.getAddedSubList().forEach(this::addNewIndicators);
        }
    };
    protected ChangeListener<? super Chart> chartChangeListener = (ch, o, n) -> {
        if (o != null) {
            o.getPlugins().removeListener(pluginsChanged);
        }

        if (n != null) {
            n.getPlugins().addListener(pluginsChanged);
        }
    };
    protected ListChangeListener<? super Integer> selectionChangeListener = change -> {
        while (change.next()) {
            List<AbstractSingleValueIndicator> list = new ArrayList<>(2); // NOPMD
            for (int index : indicatorListView.getSelectionModel().getSelectedIndices()) {
                list.add(valueIndicators.get(index));
            }
            valueIndicatorsUser.setAll(list);
        }
    };

    public ValueIndicatorSelector(final ParameterMeasurements plugin, final AxisMode axisMode, final int requiredNumberOfIndicators) {
        super();
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }
        this.axisMode = axisMode;
        reuseIndicators.setSelected(true);

        plugin.chartProperty().addListener(chartChangeListener);
        if (plugin.getChart() != null) {
            plugin.getChart().getPlugins().addListener(pluginsChanged);
            plugin.getChart().getPlugins().forEach(this::addNewIndicators);
        }

        final Label label = new Label("re-use inidcators: ");
        GridPane.setConstraints(label, 0, 0);
        GridPane.setConstraints(reuseIndicators, 1, 0);
        indicatorListView.disableProperty().bind(reuseIndicators.selectedProperty().not());
        indicatorListView.setOrientation(Orientation.VERTICAL);
        indicatorListView.setPrefSize(-1, DEFAULT_SELECTOR_HEIGHT);
        indicatorListView.setCellFactory(list -> new DataSelectorLabel());
        indicatorListView.setPrefHeight(Math.max(2, valueIndicators.size()) * ROW_HEIGHT + 2.0);
        GridPane.setConstraints(indicatorListView, 1, 1);
        final MultipleSelectionModel<AbstractSingleValueIndicator> selModel = indicatorListView.getSelectionModel();
        selModel.getSelectedIndices().addListener(selectionChangeListener);
        if (requiredNumberOfIndicators == 0) {
            this.setVisible(false);
        } else if (requiredNumberOfIndicators == 1) {
            selModel.setSelectionMode(SelectionMode.SINGLE);
        } else if (requiredNumberOfIndicators >= 2) {
            selModel.setSelectionMode(SelectionMode.MULTIPLE);
        }
        if (selModel.getSelectedIndices().isEmpty()) {
            for (int i = 0; i < Math.min(2, valueIndicators.size()); i++) {
                selModel.select(valueIndicators.get(i));
            }
        }

        if (requiredNumberOfIndicators > 0) {
            getChildren().addAll(label, reuseIndicators, indicatorListView);
        }
    }

    public CheckBox getReuseIndicators() {
        return reuseIndicators;
    }

    public ObservableList<AbstractSingleValueIndicator> getValueIndicators() {
        return valueIndicators;
    }

    public ObservableList<AbstractSingleValueIndicator> getValueIndicatorsUser() {
        return valueIndicatorsUser;
    }

    public boolean isReuseIndicators() {
        return reuseIndicators.isSelected();
    }

    protected void addNewIndicators(ChartPlugin newPlugin) {
        if ((axisMode == X && newPlugin instanceof XValueIndicator) || (axisMode == Y && newPlugin instanceof YValueIndicator)) {
            if (!valueIndicators.contains(newPlugin)) {
                valueIndicators.add((AbstractSingleValueIndicator) newPlugin);
            }

            if (reuseIndicators.isSelected() && valueIndicatorsUser.size() < 2 && !valueIndicatorsUser.contains(newPlugin)) {
                valueIndicatorsUser.add((AbstractSingleValueIndicator) newPlugin);
                indicatorListView.getSelectionModel().select((AbstractSingleValueIndicator) newPlugin);
            }
        }
    }

    protected void removeIndicators(ChartPlugin oldPlugin) {
        valueIndicators.remove(oldPlugin);
        valueIndicatorsUser.remove(oldPlugin);
    }

    protected static class DataSelectorLabel extends ListCell<AbstractSingleValueIndicator> {
        @Override
        public void updateItem(final AbstractSingleValueIndicator item, final boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {
                setText(item.getText());
            }
        }
    }
}
