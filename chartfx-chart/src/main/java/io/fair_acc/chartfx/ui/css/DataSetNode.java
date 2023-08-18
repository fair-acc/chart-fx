package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.renderer.spi.AbstractRenderer;
import io.fair_acc.chartfx.renderer.spi.utils.FillPatternStyleHelper;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.event.EventSource;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.events.StateListener;
import io.fair_acc.dataset.utils.AssertUtils;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * A dataset wrapper that lives in the SceneGraph for CSS styling
 *
 * @author ennerf
 */
public class DataSetNode extends DataSetNodeParameter implements EventSource {

    public Paint getLineColor() {
        if (lineColor == null) {
            lineColor = getIntensifiedColor(getStroke());
        }
        return lineColor;
    }
    private Paint lineColor = null;

    public Paint getFillColor() {
        if(fillColor == null) {
            fillColor = getIntensifiedColor(getFill());
        }
        return fillColor;
    }
    private Paint fillColor = null;

    /**
     * @return a fill pattern of crossed lines using the lineFill color
     */
    public Paint getLineFillPattern() {
        if (lineFillPattern == null) {
            var color = getLineColor();
            color = color instanceof Color ? ((Color) color).brighter() : color;
            var hatchShift = getHatchShiftByIndex() * (getGlobalIndex() + 1); // start at 1 to look better
            lineFillPattern = FillPatternStyleHelper.getDefaultHatch(color, hatchShift);
        }
        return lineFillPattern;
    }

    private Paint lineFillPattern = null;

    {
        // Reset cached colors
        PropUtil.runOnChange(() -> lineColor = null, intensityProperty(), strokeProperty());
        PropUtil.runOnChange(() -> fillColor = null, intensityProperty(), fillProperty());
        PropUtil.runOnChange(() -> lineFillPattern = null, intensityProperty(), strokeProperty(),
                hatchShiftByIndexProperty(), globalIndexProperty());
    }

    public DataSetNode(AbstractRenderer<?> renderer,  DataSet dataSet) {
        this.renderer = AssertUtils.notNull("renderer", renderer);
        this.dataSet = AssertUtils.notNull("dataSet", dataSet);

        // Forward changes from dataset to the node
        final StateListener updateText = (src, bits) -> setText(dataSet.getName());
        final StateListener updateVisibility = (src, bits) -> setVisible(dataSet.isVisible());
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null) {
                dataSet.getBitState().removeInvalidateListener(updateText);
                dataSet.getBitState().removeInvalidateListener(updateVisibility);
            }
            setText(dataSet.getName());
            setVisible(dataSet.isVisible());
            if (newScene != null) {
                dataSet.getBitState().addInvalidateListener(ChartBits.DataSetName, updateText);
                dataSet.getBitState().addInvalidateListener(ChartBits.DataSetVisibility, updateVisibility);
            }
        });

        // Initialize with the dataset style TODO: integrate with deprecated style data
        if (!PropUtil.isNullOrEmpty(dataSet.getStyle())) {
            setStyle(dataSet.getStyle());
        }

        // Forward changes from the node to the dataset TODO: remove visibility from dataset
        PropUtil.runOnChange(() -> dataSet.setVisible(isVisible()), visibleProperty());

        // Notify style updates via the dataset state. Note that the node could
        // have a dedicated state, but that only provides benefits when one
        // dataset is in multiple charts where one draw could be skipped.
        // TODO: maybe notify the chart directly that the Canvas needs to be redrawn?
        changeCounterProperty().addListener(getBitState().onPropChange(ChartBits.DataSetMetaData)::set);

        // Integrate with the JavaFX default CSS color selectors
        StyleUtil.styleNode(this, "dataset", DefaultColorClass.getForIndex(getColorIndex()));
        colorIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                getStyleClass().removeAll(DefaultColorClass.getForIndex(oldValue.intValue()));
            }
            if (newValue != null) {
                getStyleClass().add(1, DefaultColorClass.getForIndex(newValue.intValue()));
            }
            // TODO: reapply CSS? usually set before CSS, but could potentially be modified after CSS too. might be expensive
        });
    }

    @Override
    public BitState getBitState() {
        return dataSet.getBitState();
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public AbstractRenderer<?> getRenderer() {
        return renderer;
    }

    private final DataSet dataSet;
    private final AbstractRenderer<?> renderer;

    static class DefaultColorClass {

        public static String getForIndex(int index) {
            if (index >= 0 && index < precomputed.length) {
                return precomputed[index];
            }
            return createDefaultClass(index);
        }

        private static String createDefaultClass(int colorIx) {
            return "default-color" + colorIx;
        }

        private static final String[] precomputed = new String[20];

        static {
            for (int i = 0; i < precomputed.length; i++) {
                precomputed[i] = createDefaultClass(i);
            }
        }
    }

}
