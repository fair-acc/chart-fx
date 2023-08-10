package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.XYChartCss;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.event.EventSource;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.utils.AssertUtils;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.css.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Shape;

import java.util.List;

/**
 * A dataset wrapper that lives in the SceneGraph for CSS styling
 *
 * @author ennerf
 */
public class DataSetNode extends TextStyle implements EventSource {

    public DataSetNode(DataSet dataSet) {
        StyleUtil.styleNode(this, "dataset");
        this.dataSet = AssertUtils.notNull("dataSet", dataSet);
        setVisible(dataSet.isVisible());
        if (!PropUtil.isNullOrEmpty(dataSet.getStyle())) {
            setStyle(dataSet.getStyle());
        }
    }

    // Index within the renderer set
    final IntegerProperty localIndex = new SimpleIntegerProperty();

    // Index within all chart sets
    final IntegerProperty globalIndex = new SimpleIntegerProperty();

    // Offset for the color indexing
    final StyleableIntegerProperty dsLayoutOffset = CSS.createIntegerProperty(this, XYChartCss.DATASET_LAYOUT_OFFSET, 0);

    // A stylable local index. TODO: should this really be settable?
    final StyleableIntegerProperty dsIndex = CSS.createIntegerProperty(this, XYChartCss.DATASET_INDEX, 0);

    final StyleableDoubleProperty intensity = CSS.createDoubleProperty(this, XYChartCss.DATASET_INTENSITY, 100);
    final StyleableBooleanProperty showInLegend = CSS.createBooleanProperty(this, XYChartCss.DATASET_SHOW_IN_LEGEND, true);

    @Override
    public Node getStyleableNode() {
        return this;
    }

    @Override
    public BitState getBitState() {
        return dataSet.getBitState();
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public int getLocalIndex() {
        return localIndex.get();
    }

    public IntegerProperty localIndexProperty() {
        return localIndex;
    }

    public void setLocalIndex(int localIndex) {
        this.localIndex.set(localIndex);
    }

    public int getGlobalIndex() {
        return globalIndex.get();
    }

    public IntegerProperty globalIndexProperty() {
        return globalIndex;
    }

    public void setGlobalIndex(int globalIndex) {
        this.globalIndex.set(globalIndex);
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    private final DataSet dataSet;

    private static final CssPropertyFactory<DataSetNode> CSS = new CssPropertyFactory<>(Shape.getClassCssMetaData());

}
