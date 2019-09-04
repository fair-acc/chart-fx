package de.gsi.chart.viewer;

import de.gsi.chart.XYChart;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Holds all charts/tables to be displayed
 *
 * @author Grzegorz Kruk (original idea)
 * @author rstein (adapted to JDVE&lt;-&gt;JavaFX bridge
 */
public class DataView {
    private final StringProperty name = new SimpleStringProperty(this, "name");
    private final ObjectProperty<Layout> layout = new SimpleObjectProperty<>(this, "layout", Layout.GRID);
    private final ObservableList<DataViewPane> children = FXCollections.observableArrayList();
    private final ObjectProperty<DataViewPane> maximizedView = new SimpleObjectProperty<>(this, "maximizedView");
    private final ObservableList<DataViewPane> minimizedView = FXCollections.observableArrayList();
    private final ObservableList<DataViewPane> visibleView = FXCollections.observableArrayList();

    public DataView() {
        this(null);
    }

    public DataView(final String name) {
        setName(name);

        children.addListener((ListChangeListener<DataViewPane>) change -> {
            while (change.next()) {
                change.getRemoved().forEach(v -> v.setDataView(null));
                change.getAddedSubList().forEach(v -> v.setDataView(this));
                change.getAddedSubList().forEach(v -> getVisibleChildren().add(v));

                if (change.getRemoved().contains(getMaximizedView())) {
                    setMaximizedView(null);
                }

                // request layout because of added/removed DataViews

            }
        });
    }

    /**
     * Sugar method to add DataViewPane with chart.
     * @param chartName name of chart
     * @param chart reference
     */
    public void add(final String chartName, final XYChart chart) {
        getChildren().add(new DataViewPane(chartName, chart));
    }

    public final ObservableList<DataViewPane> getChildren() {
        return children;
    }

    public final Layout getLayout() {
        return layoutProperty().get();
    }

    public final DataViewPane getMaximizedView() {
        return maximizedViewProperty().get();
    }

    public final ObservableList<DataViewPane> getMinimizedChildren() {
        return minimizedView;
    }

    public final String getName() {
        return nameProperty().get();
    }

    public final ObservableList<DataViewPane> getVisibleChildren() {
        return visibleView;
    }

    public final ObjectProperty<Layout> layoutProperty() {
        return layout;
    }

    public final ObjectProperty<DataViewPane> maximizedViewProperty() {
        return maximizedView;
    }

    public final StringProperty nameProperty() {
        return name;
    }

    public final void setLayout(final Layout layout) {
        layoutProperty().set(layout);
    }

    public final void setMaximizedView(final DataViewPane view) {
        maximizedViewProperty().set(view);
    }

    public final void setName(final String name) {
        nameProperty().set(name);
    }

    public enum Layout {
        HBOX,
        VBOX,
        GRID
    }
}
