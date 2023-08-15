package io.fair_acc.chartfx.legend.spi;

import java.util.ArrayList;
import java.util.List;

import io.fair_acc.chartfx.ui.css.CssPropertyFactory;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.chartfx.ui.css.StyleUtil;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.utils.PropUtil;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.*;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import io.fair_acc.chartfx.legend.Legend;
import io.fair_acc.chartfx.renderer.Renderer;

/**
 * A chart legend that displays a list of items with symbols in a box
 *
 * @author rstein
 */
public class DefaultLegend extends FlowPane implements Legend {
    private static final PseudoClass disabledClass = PseudoClass.getPseudoClass("disabled");

    // -------------- PUBLIC PROPERTIES ----------------------------------------

    StyleableObjectProperty<Side> side = CSS.createSideProperty(this, Side.BOTTOM);

    /**
     * The legend items to display in this legend
     */
    private final ObservableList<LegendItem> items = FXCollections.observableArrayList();
    private final ArrayList<LegendItem> tmpItems = new ArrayList<>();

    public DefaultLegend() {
        StyleUtil.addStyles(this, "chart-legend");
        items.addListener((ListChangeListener<LegendItem>) c -> getChildren().setAll(items));
        PropUtil.runOnChange(this::applyCss, sideProperty());
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

    public LegendItem getNewLegendItem(final DataSetNode series) {
        var item = new LegendItem(series);
        item.setOnMouseClicked(event -> series.setVisible(!series.isVisible()));
        PropUtil.initAndRunOnChange(
                () -> item.pseudoClassStateChanged(disabledClass, !series.isVisible()),
                series.visibleProperty());
        item.visibleProperty().bind(series.getRenderer().showInLegendProperty().and(series.showInLegendProperty()));
        item.managedProperty().bind(item.visibleProperty());
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
    public void updateLegend(final List<Renderer> renderers, final boolean forceUpdate) {
        if (forceUpdate) {
            getItems().clear();
        }

        tmpItems.clear();
        for (Renderer renderer : renderers) {
            for (DataSetNode series : renderer.getDatasetNodes()) {
                // Prefer existing nodes
                LegendItem item = null;
                for (LegendItem existing : getItems()) {
                    if (existing.getSeries() == series) {
                        item = existing;
                        break;
                    }
                }

                // New instance
                if(item == null) {
                    item = getNewLegendItem(series);
                }
                tmpItems.add(item);
            }
        }

        // Update all at once
        getItems().setAll(tmpItems);

    }

    @Override
    public void drawLegend() {
        for (LegendItem item : items) {
            if (item.isVisible()) {
                item.drawLegendSymbol();
            }
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

        public LegendItem(DataSetNode series) {
            StyleUtil.addStyles(this, "chart-legend-item");
            textProperty().bind(series.textProperty());
            setGraphic(symbol);
            symbol.widthProperty().bind(symbolWidth);
            symbol.heightProperty().bind(symbolHeight);
            this.series = series;
        }

        final Canvas symbol = new Canvas();
        final StyleableDoubleProperty symbolWidth = CSS.createDoubleProperty(this, "symbolWidth", 20);
        final StyleableDoubleProperty symbolHeight = CSS.createDoubleProperty(this, "symbolHeight", 20);
        final DataSetNode series;

        public DataSetNode getSeries() {
            return series;
        }

        public void drawLegendSymbol() {
            symbol.setVisible(series.getRenderer().drawLegendSymbol(series, symbol));
        }

        @Override
        public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
            return CSS.getCssMetaData();
        }
        private static final CssPropertyFactory<LegendItem> CSS = new CssPropertyFactory<>(Label.getClassCssMetaData());


    }
}
