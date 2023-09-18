package io.fair_acc.chartfx.renderer.spi;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.AxisRange;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError;
import io.fair_acc.dataset.EditableDataSet;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;
import javafx.scene.canvas.GraphicsContext;

import java.util.LinkedList;
import java.util.List;

/**
 * Renders the data set with the pre-described
 *
 * @author R.J. Steinhagen
 */
public class HistoryDataSetRenderer extends ErrorDataSetRenderer implements Renderer {
    protected static final int DEFAULT_HISTORY_DEPTH = 3;

    public HistoryDataSetRenderer() {
        this(HistoryDataSetRenderer.DEFAULT_HISTORY_DEPTH);
    }

    public HistoryDataSetRenderer(final int historyDepth) {
        if (historyDepth < 0) {
            throw new IllegalArgumentException(
                    String.format("historyDepth=='%d' should be larger than '0'", historyDepth));
        }
        this.historyDepth = historyDepth;
    }

    @Override
    protected void render(final GraphicsContext gc, final DataSet dataSet, final DataSetNode style) {
        final double originalIntensity = style.getIntensity();
        try {
            // render historical data oldest first
            var history = ((HistoryDataSetNode) style).getHistory();
            int histIx = history.size() - 1;
            for (DataSet histDs : history) {
                final var faded = Math.pow(getIntensityFading(), histIx + 2.0) * originalIntensity;
                style.setIntensity((int) faded);
                histIx--;
                super.render(gc, histDs, style);
            }
        } finally {
            style.setIntensity(originalIntensity);
        }
        // render latest data
        super.render(gc, dataSet, style);
    }

    @Override
    protected void updateAxisRange(AxisRange range, int dim) {
        // Add the range of the historical data as well
        for (DataSetNode node : getDatasetNodes()) {
            if (node.isVisible()) {
                updateAxisRange(node.getDataSet(), range, dim);
                for (DataSet histDs : ((HistoryDataSetNode) node).getHistory()) {
                    updateAxisRange(histDs, range, dim);
                }
            }
        }
    }

    /**
     * clear renderer history
     */
    public void clearHistory() {
        for (DataSetNode ds : getDatasetNodes()) {
            ((HistoryDataSetNode) ds).clear();
        }
    }

    public void shiftHistory() {
        for (DataSetNode ds : getDatasetNodes()) {
            ((HistoryDataSetNode) ds).shift();
        }
    }

    @Override
    protected HistoryDataSetNode createNode(DataSet dataSet) {
        return new HistoryDataSetNode(this, dataSet, historyDepth);
    }

    static class HistoryDataSetNode extends DataSetNode {

        HistoryDataSetNode(AbstractRenderer<?> renderer, DataSet dataSet, int depth) {
            super(renderer, dataSet);
            this.depth = depth;
        }

        /**
         * @return history with the first element being the oldest
         */
        public List<DataSet> getHistory() {
            return history;
        }

        public void shift() {
            var src = getDataSet();
            if (history.size() < depth) {
                history.add(copy(src));
            } else {
                history.add(history.removeFirst().set(src));
            }

            // Set names (TODO: is this used anywhere?)
            var prefix = src.getName().split("_")[0];
            int index = history.size() - 1;
            for (DataSet histDs : history) {
                ((EditableDataSet) histDs).setName(prefix + "History_{-" + index-- + "}");
            }
        }

        public void clear() {
            history.clear();
        }

        static DataSet copy(DataSet ds) {
            return ds instanceof DataSetError ? new DoubleErrorDataSet(ds) : new DoubleDataSet(ds);
        }

        final int depth;
        final LinkedList<DataSet> history = new LinkedList<>();

    }

    final int historyDepth;

}
