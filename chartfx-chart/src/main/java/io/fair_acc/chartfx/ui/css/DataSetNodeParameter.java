package io.fair_acc.chartfx.ui.css;

import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.css.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.marker.Marker;
import io.fair_acc.chartfx.renderer.spi.utils.FillPatternStyleHelper;
import io.fair_acc.chartfx.utils.PropUtil;

/**
 * Holds the styleable parameters of the DataSetNode
 *
 * @author ennerf
 */
public abstract class DataSetNodeParameter extends Parent implements StyleUtil.StyleNode {
    // ======================== State properties ========================
    private final LongProperty changeCounter = new SimpleLongProperty(0);
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty localIndex = new SimpleIntegerProperty();
    private final IntegerProperty globalIndex = new SimpleIntegerProperty();
    private final IntegerProperty colorIndex = addOnChange(new SimpleIntegerProperty());
    private final DoubleProperty intensity = addOnChange(css().createDoubleProperty(this, "intensity", 100));
    private final BooleanProperty showInLegend = addOnChange(css().createBooleanProperty(this, "showInLegend", true));
    private final DoubleProperty hatchShiftByIndex = addOnChange(css().createDoubleProperty(this, "hatchShiftByIndex", 1.5));

    {
        addOnChange(visibleProperty());
    }

    // ======================== Marker properties (ignored if markerSize is zero) ========================

    // The CSS enum property can't be set to the base interface, so we provide a user binding that overrides the CSS
    private final ObjectProperty<DefaultMarker> markerType = css().createEnumProperty(this, "markerType", DefaultMarker.DEFAULT, true, DefaultMarker.class);
    private final ObjectProperty<Marker> userMarkerType = new SimpleObjectProperty<>(null);
    private final ObjectBinding<Marker> actualMarkerType = addOnChange(Bindings.createObjectBinding(() -> {
        return userMarkerType.get() != null ? userMarkerType.get() : markerType.get();
    }, userMarkerType, markerType));

    private final DoubleProperty markerLineWidth = addOnChange(css().createDoubleProperty(this, "markerLineWidth", 0.5));
    private final DoubleProperty markerSize = addOnChange(css().createDoubleProperty(this, "markerSize", 1.5));
    private final ObjectProperty<Paint> markerColor = addOnChange(css().createPaintProperty(this, "markerColor", Color.BLACK));
    protected final ObjectBinding<Paint> intensifiedMarkerColor = intensifiedColor(markerColor);
    private final ObjectProperty<Number[]> markerLineDashArray = addOnChange(css().createNumberArrayProperty(this, "markerLineDashArray", null));
    private final ObjectBinding<double[]> markerLineDashes = StyleUtil.toUnboxedDoubleArray(markerLineDashArray);

    // ======================== Line properties (ignored if lineWidth is zero) ========================

    private final DoubleProperty lineWidth = addOnChange(css().createDoubleProperty(this, "lineWidth", 1.0));
    private final ObjectProperty<Paint> lineColor = addOnChange(css().createPaintProperty(this, "lineColor", Color.BLACK));
    protected final ObjectBinding<Paint> intensifiedLineColor = intensifiedColor(lineColor);
    private final ObjectBinding<Paint> lineFillPattern = hatchFillPattern(intensifiedLineColor);
    private final ObjectProperty<Number[]> lineDashArray = addOnChange(css().createNumberArrayProperty(this, "lineDashArray", null));
    private final ObjectBinding<double[]> lineDashes = StyleUtil.toUnboxedDoubleArray(lineDashArray);

    // ======================== Overriden accessors ========================

    public Paint getMarkerColor() {
        return intensifiedMarkerColor.get();
    }

    public Paint getLineColor() {
        return intensifiedLineColor.get();
    }

    public Marker getMarkerType() {
        return markerTypeProperty().get();
    }

    public ObjectBinding<Marker> markerTypeProperty() {
        return actualMarkerType;
    }

    public void setMarkerType(Marker marker) {
        this.userMarkerType.set(marker);
    }

    // ======================== Generated accessors ========================

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        // TODO: the name may revert to the dataSet name. Should this set the underlying dataSet name? may be shared?
        this.name.set(name);
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

    public int getColorIndex() {
        return colorIndex.get();
    }

    public IntegerProperty colorIndexProperty() {
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

    public double getHatchShiftByIndex() {
        return hatchShiftByIndex.get();
    }

    public DoubleProperty hatchShiftByIndexProperty() {
        return hatchShiftByIndex;
    }

    public void setHatchShiftByIndex(double hatchShiftByIndex) {
        this.hatchShiftByIndex.set(hatchShiftByIndex);
    }

    @Override
    public long getChangeCounter() {
        return changeCounter.get();
    }

    public void setChangeCounter(long changeCounter) {
        this.changeCounter.set(changeCounter);
    }

    public void setMarkerType(DefaultMarker markerType) {
        this.markerType.set(markerType);
    }

    public Marker getUserMarkerType() {
        return userMarkerType.get();
    }

    public ObjectProperty<Marker> userMarkerTypeProperty() {
        return userMarkerType;
    }

    public void setUserMarkerType(Marker userMarkerType) {
        this.userMarkerType.set(userMarkerType);
    }

    public Marker getActualMarkerType() {
        return actualMarkerType.get();
    }

    public ObjectBinding<Marker> actualMarkerTypeProperty() {
        return actualMarkerType;
    }

    public double getMarkerLineWidth() {
        return markerLineWidth.get();
    }

    public DoubleProperty markerLineWidthProperty() {
        return markerLineWidth;
    }

    public void setMarkerLineWidth(double markerLineWidth) {
        this.markerLineWidth.set(markerLineWidth);
    }

    public double getMarkerSize() {
        return markerSize.get();
    }

    public DoubleProperty markerSizeProperty() {
        return markerSize;
    }

    public void setMarkerSize(double markerSize) {
        this.markerSize.set(markerSize);
    }

    public ObjectProperty<Paint> markerColorProperty() {
        return markerColor;
    }

    public void setMarkerColor(Paint markerColor) {
        this.markerColor.set(markerColor);
    }

    public Paint getIntensifiedMarkerColor() {
        return intensifiedMarkerColor.get();
    }

    public ObjectBinding<Paint> intensifiedMarkerColorProperty() {
        return intensifiedMarkerColor;
    }

    public Number[] getMarkerLineDashArray() {
        return markerLineDashArray.get();
    }

    public ObjectProperty<Number[]> markerLineDashArrayProperty() {
        return markerLineDashArray;
    }

    public void setMarkerLineDashArray(Number[] markerLineDashArray) {
        this.markerLineDashArray.set(markerLineDashArray);
    }

    public double[] getMarkerLineDashes() {
        return markerLineDashes.get();
    }

    public ObjectBinding<double[]> markerLineDashesProperty() {
        return markerLineDashes;
    }

    public double getLineWidth() {
        return lineWidth.get();
    }

    public DoubleProperty lineWidthProperty() {
        return lineWidth;
    }

    public void setLineWidth(double lineWidth) {
        this.lineWidth.set(lineWidth);
    }

    public ObjectProperty<Paint> lineColorProperty() {
        return lineColor;
    }

    public void setLineColor(Paint lineColor) {
        this.lineColor.set(lineColor);
    }

    public Paint getIntensifiedLineColor() {
        return intensifiedLineColor.get();
    }

    public ObjectBinding<Paint> intensifiedLineColorProperty() {
        return intensifiedLineColor;
    }

    public Paint getLineFillPattern() {
        return lineFillPattern.get();
    }

    public ObjectBinding<Paint> lineFillPatternProperty() {
        return lineFillPattern;
    }

    public Number[] getLineDashArray() {
        return lineDashArray.get();
    }

    public ObjectProperty<Number[]> lineDashArrayProperty() {
        return lineDashArray;
    }

    public void setLineDashArray(Number[] lineDashArray) {
        this.lineDashArray.set(lineDashArray);
    }

    public double[] getLineDashes() {
        return lineDashes.get();
    }

    public ObjectBinding<double[]> lineDashesProperty() {
        return lineDashes;
    }

    // ======================== Utility methods ========================

    protected ObjectBinding<Paint> intensifiedColor(ObservableValue<Paint> base) {
        return Bindings.createObjectBinding(() -> getIntensifiedColor(base.getValue()), base, intensity);
    }

    protected ObjectBinding<Paint> hatchFillPattern(ObservableValue<Paint> base) {
        return Bindings.createObjectBinding(() -> {
            var color = base.getValue();
            if (color instanceof Color) {
                color = ((Color) color).brighter();
            }
            // start at 1 to look better
            var hatchShift = getHatchShiftByIndex() * (getGlobalIndex() + 1);
            return FillPatternStyleHelper.getDefaultHatch(color, hatchShift);
        }, base, globalIndex, hatchShiftByIndex);
    }

    protected Paint getIntensifiedColor(Paint color) {
        if (getIntensity() >= 100 || !(color instanceof Color)) {
            return color;
        }
        if (getIntensity() <= 0) {
            return Color.TRANSPARENT;
        }
        var scale = getIntensity() / 100d;
        return ((Color) color).deriveColor(0, scale, 1.0, scale);
    }

    @Override
    public ReadOnlyLongProperty changeCounterProperty() {
        return changeCounter;
    }

    protected void incrementChangeCounter() {
        changeCounter.set(changeCounter.get() + 1);
    }

    protected <T extends ObservableValue<?>> T addOnChange(T observable) {
        PropUtil.runOnChange(this::incrementChangeCounter, observable);
        return observable;
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

    private static final CssPropertyFactory<DataSetNodeParameter> CSS = new CssPropertyFactory<>(Parent.getClassCssMetaData());
}
