package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.marker.Marker;
import io.fair_acc.chartfx.renderer.spi.utils.FillPatternStyleHelper;
import io.fair_acc.chartfx.utils.PropUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.css.*;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.List;
import java.util.WeakHashMap;

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
                actualMarkerType,
                markerSize
        );
    }

    public Paint getMarkerColor() {
        return getModifiedColor(getStroke());
    }

    public double getMarkerLineWidth() {
        return getMarkerStrokeWidth();
    }

    public Paint getLineColor() {
        return getModifiedColor(getStroke());
    }

    public double getLineWidth() {
        return getStrokeWidth();
    }

    /**
     * @return a fill pattern of crossed lines using the lineFill color
     */
    public Paint getLineFillPattern() {
        return lineFillPattern.computeIfAbsent(getLineColor(), color -> {
            color = color instanceof Color ? ((Color) color).brighter() : color;
            var defaultHatchShift = 1.5;
            return FillPatternStyleHelper.getDefaultHatch(color, defaultHatchShift);
        });
    }

    private static WeakHashMap<Paint, Paint> lineFillPattern = new WeakHashMap<>(31);

    public double[] getLineDashes() {
        if (getStrokeDashArray().isEmpty()) {
            return null;
        }
        if (dashArray == null || dashArray.length != getStrokeDashArray().size()) {
            dashArray = new double[getStrokeDashArray().size()];
        }
        for (int i = 0; i < dashArray.length; i++) {
            dashArray[i] = getStrokeDashArray().get(i);
        }
        return dashArray;
    }

    private double[] dashArray = null;

    private Paint getModifiedColor(Paint color) {
        if (getIntensity() >= 100 || !(color instanceof Color)) {
            return color;
        }
        if (getIntensity() <= 0) {
            return Color.TRANSPARENT;
        }
        int scale = (int) (getIntensity() / 100);
        return ((Color) color).deriveColor(0, scale, 1.0, scale);
    }

    private final IntegerProperty localIndex = new SimpleIntegerProperty();
    private final IntegerProperty globalIndex = new SimpleIntegerProperty();
    private final IntegerProperty colorIndex = new SimpleIntegerProperty();
    private final DoubleProperty intensity = css().createDoubleProperty(this, "intensity", 100);
    private final BooleanProperty showInLegend = css().createBooleanProperty(this, "showInLegend", true);

    // The CSS enum property can't be set to the base interface, so we provide a user binding that overrides the CSS
    private final ObjectProperty<DefaultMarker> markerType = css().createEnumProperty(this, "markerType", DefaultMarker.DEFAULT, true, DefaultMarker.class);
    private final ObjectProperty<Marker> userMarkerType = new SimpleObjectProperty<>(null);
    private final ObjectBinding<Marker> actualMarkerType = Bindings.createObjectBinding(() -> {
        return userMarkerType.get() != null ? userMarkerType.get() : markerType.get();
    }, userMarkerType, markerType);

    // Marker specific properties
    private final DoubleProperty markerStrokeWidth = css().createDoubleProperty(this, "markerStrokeWidth", 0.5);
    private final DoubleProperty markerSize = css().createDoubleProperty(this, "markerSize", 1.5, true, (oldVal, newVal) -> {
        return newVal >= 0 ? newVal : oldVal;
    });

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

    public Marker getMarkerType() {
        return markerTypeProperty().get();
    }

    public ObjectBinding<Marker> markerTypeProperty() {
        return actualMarkerType;
    }

    public void setMarkerType(Marker marker) {
        this.userMarkerType.set(marker);
    }

    public double getMarkerStrokeWidth() {
        return markerStrokeWidth.get();
    }

    public DoubleProperty markerStrokeWidthProperty() {
        return markerStrokeWidth;
    }

    public void setMarkerStrokeWidth(double markerStrokeWidth) {
        this.markerStrokeWidth.set(markerStrokeWidth);
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
