package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.utils.PropUtil;
import javafx.beans.property.*;
import javafx.css.*;
import javafx.scene.Node;

import java.util.List;

/**
 * Holds the styleable parameters of the DataSetNode
 *
 * @author ennerf
 */
public abstract class DataSetNodeParameter extends TextStyle {

    public DataSetNodeParameter() {
        PropUtil.runOnChange(super::incrementChangeCounter,
                colorIndex,
                intensity,
                showInLegend,
                markerType
        );
    }

    final IntegerProperty localIndex = new SimpleIntegerProperty();
    final IntegerProperty globalIndex = new SimpleIntegerProperty();
    final IntegerProperty colorIndex = new SimpleIntegerProperty();
    final DoubleProperty intensity = css().createDoubleProperty(this, "intensity", 100);
    final BooleanProperty showInLegend = css().createBooleanProperty(this, "showInLegend", true);
    final ObjectProperty<DefaultMarker> markerType = css().createEnumProperty(this, "markerType", DefaultMarker.DEFAULT, true, DefaultMarker.class);

    public int getLocalIndex() {
        return localIndex.get();
    }

    public ReadOnlyIntegerProperty localIndexProperty() {
        return localIndex;
    }

    public void setLocalIndex(int localIndex) {
        this.localIndex.set(localIndex);
    }

    public int getGlobalIndex() {
        return globalIndex.get();
    }

    public ReadOnlyIntegerProperty globalIndexProperty() {
        return globalIndex;
    }

    public void setGlobalIndex(int globalIndex) {
        this.globalIndex.set(globalIndex);
    }

    public int getColorIndex() {
        return colorIndex.get();
    }

    public ReadOnlyIntegerProperty colorIndexProperty() {
        return colorIndex;
    }

    public void setColorIndex(int colorIndex) {
        this.colorIndex.set(colorIndex);
    }

    public double getIntensity() {
        return intensity.get();
    }

    public DoubleProperty intensityProperty() {
        return intensity;
    }

    public void setIntensity(double intensity) {
        this.intensity.set(intensity);
    }

    public boolean isShowInLegend() {
        return showInLegend.get();
    }

    public BooleanProperty showInLegendProperty() {
        return showInLegend;
    }

    public void setShowInLegend(boolean showInLegend) {
        this.showInLegend.set(showInLegend);
    }

    public DefaultMarker getMarkerType() {
        return markerType.get();
    }

    public ObjectProperty<DefaultMarker> markerTypeProperty() {
        return markerType;
    }

    public void setMarkerType(DefaultMarker markerType) {
        this.markerType.set(markerType);
    }

    @Override
    public Node getStyleableNode() {
        return this;
    }

    protected CssPropertyFactory<DataSetNodeParameter> css() {
        return CSS;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return css().getCssMetaData();
    }

    private static final CssPropertyFactory<DataSetNodeParameter> CSS = new CssPropertyFactory<>(TextStyle.getClassCssMetaData());

}
