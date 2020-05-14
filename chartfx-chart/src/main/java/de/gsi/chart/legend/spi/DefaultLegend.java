package de.gsi.chart.legend.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import de.gsi.chart.XYChartCss;
import de.gsi.chart.legend.Legend;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.utils.StyleParser;
import de.gsi.dataset.DataSet;

/**
 * A chart legend that displays a list of items with symbols in a box
 *
 * @author rstein
 */
public class DefaultLegend extends FlowPane implements Legend {
    // TODO: transform static integers to styleable property fields
    private static final int GAP = 5;
    private static final int SYMBOL_WIDTH = 20;
    private static final int SYMBOL_HEIGHT = 20;

    // -------------- PRIVATE FIELDS ------------------------------------------

    private final ListChangeListener<LegendItem> itemsListener = c -> {
        getChildren().setAll(getItems());
        if (isVisible()) {
            requestLayout();
        }
    };

    // -------------- PUBLIC PROPERTIES ----------------------------------------
    /**
     * The legend items should be laid out vertically in columns rather than horizontally in rows
     */
    private final BooleanProperty vertical = new SimpleBooleanProperty(this, "vertical", false) {
        @Override
        protected void invalidated() {
            setOrientation(get() ? Orientation.VERTICAL : Orientation.HORIZONTAL);
        }
    };

    /** The legend items to display in this legend */
    private final ObjectProperty<ObservableList<LegendItem>> items = new SimpleObjectProperty<ObservableList<LegendItem>>(
            this, "items") {
        private ObservableList<LegendItem> oldItems = null;

        @Override
        protected void invalidated() {
            if (oldItems != null) {
                oldItems.removeListener(itemsListener);
            }

            final ObservableList<LegendItem> newItems = get();
            if (newItems == null) {
                getChildren().clear();
            } else {
                newItems.addListener(itemsListener);
                getChildren().setAll(newItems);
            }
            oldItems = get();
            if (isVisible()) {
                requestLayout();
            }
        }
    };

    public DefaultLegend() {
        super(GAP, GAP);
        setItems(FXCollections.<LegendItem>observableArrayList());
        getStyleClass().setAll("chart-legend");
        setAlignment(Pos.CENTER);
    }

    @Override
    protected double computePrefHeight(final double forWidth) {
        // Legend prefHeight is zero if there are no legend items
        return getItems().isEmpty() ? 0 : super.computePrefHeight(forWidth);
    }

    @Override
    protected double computePrefWidth(final double forHeight) {
        // Legend prefWidth is zero if there are no legend items
        return getItems().isEmpty() ? 0 : super.computePrefWidth(forHeight);
    }

    public final ObservableList<LegendItem> getItems() {
        return items.get();
    }

    public LegendItem getNewLegendItem(final Renderer renderer, final DataSet series, final int seriesIndex) {
        final Canvas symbol = renderer.drawLegendSymbol(series, seriesIndex, SYMBOL_WIDTH, SYMBOL_HEIGHT);
        return new LegendItem(series.getName(), symbol);
    }

    @Override
    public Node getNode() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.gsi.chart.legend.Legend#isVertical()
     */
    @Override
    public final boolean isVertical() {
        return verticalProperty().get();
    }

    public final ObjectProperty<ObservableList<LegendItem>> itemsProperty() {
        return items;
    }

    public final void setItems(final ObservableList<LegendItem> value) {
        itemsProperty().set(value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.gsi.chart.legend.Legend#setVertical(boolean)
     */
    @Override
    public final void setVertical(final boolean value) {
        verticalProperty().set(value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.gsi.chart.legend.Legend#updateLegend(java.util.List, java.util.List)
     */
    @Override
    public void updateLegend(final List<DataSet> dataSets, final List<Renderer> renderers, final boolean forceUpdate) {
        // list of already drawn data sets in the legend
        final List<DataSet> alreadyDrawnDataSets = new ArrayList<>();
        final List<LegendItem> legendItems = new ArrayList<>();

        if (forceUpdate) {
            this.getItems().clear();
        }

        // process legend items common to all renderer
        int legendItemCount = 0;
        for (int seriesIndex = 0; seriesIndex < dataSets.size(); seriesIndex++) {
            final DataSet series = dataSets.get(seriesIndex);
            final String style = series.getStyle();
            final Boolean show = StyleParser.getBooleanPropertyValue(style, XYChartCss.DATASET_SHOW_IN_LEGEND);
            if (show != null && !show.booleanValue()) {
                continue;
            }

            if (!alreadyDrawnDataSets.contains(series) && !renderers.isEmpty()) {
                if (renderers.get(0).showInLegend()) {
                    legendItems.add(getNewLegendItem(renderers.get(0), series, seriesIndex));
                    alreadyDrawnDataSets.add(series);
                }
                legendItemCount++;
            }
        }

        // process data sets within the given renderer
        for (final Renderer renderer : renderers) {
            if (!renderer.showInLegend()) {
                continue;
            }
            for (final DataSet series : renderer.getDatasets()) {
                final String style = series.getStyle();
                final Boolean show = StyleParser.getBooleanPropertyValue(style, XYChartCss.DATASET_SHOW_IN_LEGEND);
                if (show != null && !show.booleanValue()) {
                    continue;
                }

                if (!alreadyDrawnDataSets.contains(series)) {
                    legendItems.add(getNewLegendItem(renderer, series, legendItemCount));
                    alreadyDrawnDataSets.add(series);
                    legendItemCount++;
                }
            }
        }

        boolean diffLegend = false;
        if (getItems().size() != legendItems.size()) {
            diffLegend = true;
        } else {
            final List<String> newItems = legendItems.stream().map(LegendItem::getText).collect(Collectors.toList());
            final List<String> oldItems = getItems().stream().map(LegendItem::getText).collect(Collectors.toList());

            for (final String item : newItems) {
                if (!oldItems.contains(item)) {
                    diffLegend = true;
                }
            }
        }

        if (diffLegend) {
            getItems().setAll(legendItems);
        }
    }

    public final BooleanProperty verticalProperty() {
        return vertical;
    }

    /** A item to be displayed on a Legend */
    public static class LegendItem extends Label {
        public LegendItem(final String text, final Node symbol) {
            setText(text);
            getStyleClass().add("chart-legend-item");
            setAlignment(Pos.CENTER_LEFT);
            setContentDisplay(ContentDisplay.LEFT);
            setSymbol(symbol);
        }

        public final Node getSymbol() {
            return getGraphic();
        }

        public final void setSymbol(final Node value) {
            this.setGraphic(value);
        }
    }
}
