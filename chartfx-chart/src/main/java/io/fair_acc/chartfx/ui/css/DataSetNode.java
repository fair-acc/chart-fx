package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.renderer.spi.AbstractRenderer;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.event.EventSource;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.utils.AssertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Objects;

/**
 * A dataset wrapper that lives in the SceneGraph for CSS styling
 *
 * @author ennerf
 */
public class DataSetNode extends DataSetNodeParameter implements EventSource {

    public DataSetNode(AbstractRenderer<?> renderer,  DataSet dataSet) {
        this.renderer = AssertUtils.notNull("renderer", renderer);
        this.dataSet = AssertUtils.notNull("dataSet", dataSet);

        // Generate a class for the default CSS color selector.
        // Note: we don't force applyCss() here because the index typically gets set before CSS,
        // and it could result in a good bit of overhead. Setting via CSS might trigger an extra pulse.
        StyleUtil.styleNode(this, getDefaultColorClass(), "dataset");
        PropUtil.runOnChange(() -> getStyleClass().set(0, getDefaultColorClass()), colorIndexProperty());

        // Add other styles

        // Initialize styles in case the dataset has clean bits
        setName(dataSet.getName());
        setStyle(dataSet.getStyle());
        currentUserStyles.setAll(dataSet.getStyleClasses());
        getStyleClass().addAll(currentUserStyles);

        // Notify style updates via the dataset state. Note that the node could
        // have a dedicated state, but that only provides benefits when one
        // dataset is in multiple charts where one draw could be skipped.
        // TODO: maybe notify the chart directly that the Canvas needs to be redrawn?
        changeCounterProperty().addListener(dataSet.getBitState().onPropChange(ChartBits.ChartCanvas)::set);
    }

    protected String getDefaultColorClass() {
        return getDefaultColorClass(getColorIndex());
    }

    public static String getDefaultColorClass(int index) {
        return DefaultColorClass.getForIndex(index);
    }

    /**
     * Updates any style or name changes on the source set. Needs to be called before CSS.
     */
    public void runPreLayout() {
        var state = dataSet.getBitState();
        if (state.isClean(ChartBits.DataSetName, ChartBits.DataSetStyle)) {
            return;
        }

        // Note: don't clear because the dataset might be in multiple nodes
        if (state.isDirty(ChartBits.DataSetName)) {
            if (!nameProperty().isBound() && !Objects.equals(getName(), dataSet.getName())) {
                setName(dataSet.getName());
            }
        }

        // Update style info
        if (state.isDirty(ChartBits.DataSetStyle)) {

            // Replace user styles with the new classes
            if (!currentUserStyles.equals(dataSet.getStyleClasses())) {
                getStyleClass().removeAll(currentUserStyles);
                currentUserStyles.setAll(dataSet.getStyleClasses());
                getStyleClass().addAll(currentUserStyles);
            }

            // Update the style
            if (!Objects.equals(getStyle(), dataSet.getStyle())) {
                setStyle(dataSet.getStyle());
            }

        }
    }

    private final ObservableList<String> currentUserStyles = FXCollections.observableArrayList();

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
