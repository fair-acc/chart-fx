package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.renderer.spi.AbstractRenderer;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.event.EventSource;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.utils.AssertUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.css.*;
import javafx.scene.Node;
import javafx.scene.shape.Shape;

import java.util.List;

/**
 * A dataset wrapper that lives in the SceneGraph for CSS styling
 *
 * @author ennerf
 */
public class DataSetNode extends TextStyle implements EventSource {

    public DataSetNode(AbstractRenderer<?> renderer,  DataSet dataSet) {
        this.renderer = AssertUtils.notNull("renderer", renderer);
        this.dataSet = AssertUtils.notNull("dataSet", dataSet);
        setVisible(dataSet.isVisible());
        setText(dataSet.getName());
        PropUtil.runOnChange(() -> dataSet.setVisible(isVisible()), visibleProperty());

        if (!PropUtil.isNullOrEmpty(dataSet.getStyle())) {
            // TODO: integrate with deprecated style data
            setStyle(dataSet.getStyle());
        }
        var actualColorIndex = Bindings.createIntegerBinding(
                () -> colorIndex.get() % maxColors.get(),
                colorIndex, maxColors);
        actualColorIndex.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                getStyleClass().removeAll(DefaultColorClasses.getForIndex(oldValue.intValue()));
            }
            if (newValue != null) {
                getStyleClass().add(1, DefaultColorClasses.getForIndex(newValue.intValue()));
            }
            // TODO: reapply CSS? usually set before CSS, but could potentially be modified after CSS too. might be expensive
        });
        StyleUtil.styleNode(this, "dataset", DefaultColorClasses.getForIndex(actualColorIndex.intValue()));
    }

    // Index for automatic coloring
    final IntegerProperty colorIndex = new SimpleIntegerProperty();
    final StyleableIntegerProperty maxColors = css().createIntegerProperty(this, "maxColors", 8);
    final StyleableDoubleProperty intensity = css().createDoubleProperty(this, "intensity", 100);
    final StyleableBooleanProperty showInLegend = css().createBooleanProperty(this, "showInLegend", true);
    final StyleableObjectProperty<DefaultMarker> markerType = css().createEnumProperty(this, "markerType", DefaultMarker.DEFAULT, true, DefaultMarker.class);

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

    public int getColorIndex() {
        return colorIndex.get();
    }

    public IntegerProperty colorIndexProperty() {
        return colorIndex;
    }

    public void setColorIndex(int colorIndex) {
        this.colorIndex.set(colorIndex);
    }
    protected CssPropertyFactory<DataSetNode> css() {
        return CSS;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return css().getCssMetaData();
    }

    public boolean isShowInLegend() {
        return showInLegend.get();
    }

    public StyleableBooleanProperty showInLegendProperty() {
        return showInLegend;
    }

    public void setShowInLegend(boolean showInLegend) {
        this.showInLegend.set(showInLegend);
    }

    public AbstractRenderer<?> getRenderer() {
        return renderer;
    }

    private final DataSet dataSet;
    private final AbstractRenderer<?> renderer;

    private static final CssPropertyFactory<DataSetNode> CSS = new CssPropertyFactory<>(Shape.getClassCssMetaData());

    static class DefaultColorClasses {

        public static String getForIndex(int index) {
            if (index >= 0 && index < precomputed.length) {
                return precomputed[index];
            }
            return createDefaultClass(index);
        }

        private static String createDefaultClass(int colorIx) {
            return "default-color" + colorIx + ".chart-series-line";
        }

        private static final String[] precomputed = new String[20];

        static {
            for (int i = 0; i < precomputed.length; i++) {
                precomputed[i] = createDefaultClass(i);
            }
        }
    }

}
