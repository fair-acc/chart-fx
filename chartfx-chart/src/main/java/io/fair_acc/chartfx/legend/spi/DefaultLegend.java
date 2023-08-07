package io.fair_acc.chartfx.legend.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.fair_acc.chartfx.ui.css.CssPropertyFactory;
import io.fair_acc.chartfx.ui.css.StyleUtil;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.events.StateListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.*;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import io.fair_acc.chartfx.XYChartCss;
import io.fair_acc.chartfx.legend.Legend;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.utils.StyleParser;
import io.fair_acc.dataset.DataSet;

/**
 * A chart legend that displays a list of items with symbols in a box
 *
 * @author rstein
 */
public class DefaultLegend extends FlowPane implements Legend {
    private static final PseudoClass disabledClass = PseudoClass.getPseudoClass("disabled");

    // -------------- PUBLIC PROPERTIES ----------------------------------------

    StyleableObjectProperty<Side> side = CSS.createSideProperty(this, Side.BOTTOM);
    StyleableDoubleProperty symbolWidth = CSS.createDoubleProperty(this, "symbolWidth", 20);
    StyleableDoubleProperty symbolHeight = CSS.createDoubleProperty(this, "symbolHeight", 20);

    /**
     * The legend items to display in this legend
     */
    private final ObservableList<LegendItem> items = FXCollections.observableArrayList();

    public DefaultLegend() {
        getStyleClass().setAll("chart-legend");
        managedProperty().bind(visibleProperty().and(Bindings.size(items).isNotEqualTo(0)));
        items.addListener((ListChangeListener<LegendItem>) c -> getChildren().setAll(items));
        PropUtil.runOnChange(this::applyCss, sideProperty());

        // TODO:
        //  (1) The legend does not have a reference to the chart, so for now do a hack
        //      and try to get it out of the hierarchy.
        //  (2) The items are currently created with a fixed size before the styling phase,
        //      so live-updates w/ CSSFX dont work properly without re-instantiating the chart.
        PropUtil.runOnChange(() -> FXUtils.tryGetChartParent(this)
                        .ifPresent(chart -> chart.fireInvalidated(ChartBits.ChartLegend)),
                symbolHeight, symbolWidth);
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
        return items;
    }

    public final void setItems(List<LegendItem> items) {
        // TODO: remove after changing unit tests
        this.items.setAll(items);
    }

    public LegendItem getNewLegendItem(final Renderer renderer, final DataSet series, final int seriesIndex) {
        final Canvas symbol = renderer.drawLegendSymbol(series, seriesIndex,
                (int) Math.round(getSymbolWidth()),
                (int) Math.round(getSymbolHeight()));
        var item = new LegendItem(series.getName(), symbol);
        item.setOnMouseClicked(event -> series.setVisible(!series.isVisible()));
        Runnable updateCss = () -> item.pseudoClassStateChanged(disabledClass, !series.isVisible());
        StateListener listener = (obj, bits) -> updateCss.run();
        item.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene == null) {
                series.getBitState().removeInvalidateListener(listener);
            } else if (oldScene == null) {
                updateCss.run(); // changing pseudo class in CSS does not trigger another pulse
                series.getBitState().addInvalidateListener(ChartBits.DataSetVisibility, listener);
            }
        });
        return item;
    }

    @Override
    public Node getNode() {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see io.fair_acc.chartfx.legend.Legend#isVertical()
     */
    @Override
    public final boolean isVertical() {
        return getOrientation() == Orientation.VERTICAL;
    }

    /*
     * (non-Javadoc)
     *
     * @see io.fair_acc.chartfx.legend.Legend#setVertical(boolean)
     */
    @Override
    public final void setVertical(final boolean vertical) {
        setOrientation(vertical ? Orientation.VERTICAL : Orientation.HORIZONTAL);
    }

    /*
     * (non-Javadoc)
     *
     * @see io.fair_acc.chartfx.legend.Legend#updateLegend(java.util.List, java.util.List)
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
            if (show != null && !show) {
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
                legendItemCount += renderer.getDatasets().size();
                continue;
            }
            for (final DataSet series : renderer.getDatasets()) {
                final String style = series.getStyle();
                final Boolean show = StyleParser.getBooleanPropertyValue(style, XYChartCss.DATASET_SHOW_IN_LEGEND);
                if (show != null && !show) {
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
                    break;
                }
            }
        }

        if (diffLegend) {
            getItems().setAll(legendItems);
        }
    }

    public Side getSide() {
        return side.get();
    }

    public StyleableObjectProperty<Side> sideProperty() {
        return side;
    }

    public void setSide(Side side) {
        this.side.set(side);
    }

    public double getSymbolWidth() {
        return symbolWidth.get();
    }

    public StyleableDoubleProperty symbolWidthProperty() {
        return symbolWidth;
    }

    public void setSymbolWidth(double symbolWidth) {
        this.symbolWidth.set(symbolWidth);
    }

    public double getSymbolHeight() {
        return symbolHeight.get();
    }

    public StyleableDoubleProperty symbolHeightProperty() {
        return symbolHeight;
    }

    public void setSymbolHeight(double symbolHeight) {
        this.symbolHeight.set(symbolHeight);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS.getCssMetaData();
    }

    private static final CssPropertyFactory<DefaultLegend> CSS = new CssPropertyFactory<>(FlowPane.getClassCssMetaData());

    /**
     * A item to be displayed on a Legend
     */
    public static class LegendItem extends Label {
        public LegendItem(final String text, final Node symbol) {
            StyleUtil.addStyles(this, "chart-legend-item");
            setText(text);
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
