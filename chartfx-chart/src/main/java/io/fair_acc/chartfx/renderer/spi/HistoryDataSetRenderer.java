package io.fair_acc.chartfx.renderer.spi;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.fair_acc.chartfx.ui.css.DataSetNode;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.canvas.GraphicsContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.XYChartCss;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.chartfx.utils.StyleParser;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.EditableDataSet;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * Renders the data set with the pre-described
 *
 * @author R.J. Steinhagen
 */
public class HistoryDataSetRenderer extends ErrorDataSetRenderer implements Renderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryDataSetRenderer.class);
    protected static final int DEFAULT_HISTORY_DEPTH = 3;
    protected final ObservableList<DataSet> chartDataSetsCopy = FXCollections.observableArrayList();
    protected final ObservableList<ErrorDataSetRenderer> renderers = FXCollections.observableArrayList();
    protected boolean itself = false;

    public HistoryDataSetRenderer() {
        this(HistoryDataSetRenderer.DEFAULT_HISTORY_DEPTH);
    }

    public HistoryDataSetRenderer(final int historyDepth) {
        super();

        if (historyDepth < 0) {
            throw new IllegalArgumentException(
                    String.format("historyDepth=='%d' should be larger than '0'", historyDepth));
        }

        for (int i = 0; i < historyDepth; i++) {
            final ErrorDataSetRenderer newRenderer = new ErrorDataSetRenderer();
            newRenderer.bind(this);
            // do not show history sets in legend (single exception to binding)
            newRenderer.showInLegendProperty().unbind();
            newRenderer.setShowInLegend(false);
            renderers.add(newRenderer);
        }

        getAxes().addListener(HistoryDataSetRenderer.this::axisChanged);

        // special data set handling to re-add local datasets from dependent
        // renderers

        super.getDatasets().addListener((ListChangeListener<? super DataSet>) e -> {
            while (e.next()) {
                if (e.wasAdded()) {
                    final ObservableList<DataSet> localList = FXCollections.observableArrayList();
                    for (final Renderer r : renderers) {
                        for (final DataSet set : r.getDatasets()) {
                            // don't add duplicates
                            if (!getDatasets().contains(set)) {
                                localList.add(set);
                            }
                        }
                    }
                    // this funny looking expression avoids infinite loops
                    if (!localList.isEmpty() && !itself) {
                        itself = true;
                        super.getDatasets().addAll(localList);
                        itself = false;
                    }
                }
            }
        });
    }

    protected void axisChanged(final ListChangeListener.Change<? extends Axis> change) {
        while (change.next()) {
            if (change.wasRemoved()) {
                for (final ErrorDataSetRenderer renderer : renderers) {
                    renderer.getAxes().removeAll(change.getRemoved());
                }
            }

            if (change.wasAdded()) {
                for (final ErrorDataSetRenderer renderer : renderers) {
                    renderer.getAxes().addAll(change.getAddedSubList());
                }
            }
        }
    }

    /**
     * clear renderer history
     */
    public void clearHistory() {
        for (final Renderer renderer : renderers) {
            try {
                FXUtils.runAndWait(() -> {
                    super.getDatasets().removeAll(renderer.getDatasets());
                    renderer.getDatasets().clear();
                });
            } catch (final Exception e) {
                LOGGER.atError().setCause(e).log("clearHistory()");
            }
        }
    }

    /**
     * @return all DataSets that are either from the calling graph or this first specific renderer
     */
    private ObservableList<DataSet> getLocalDataSets() {
        final ObservableList<DataSet> retVal = FXCollections.observableArrayList();
        retVal.addAll(getDatasets());

        final List<DataSet> removeList = new ArrayList<>();
        for (final Renderer r : renderers) {
            removeList.addAll(r.getDatasets());
        }

        retVal.removeAll(removeList);
        return retVal;
    }

    protected void modifyStyle(final DataSet dataSet, final int dataSetIndex) {
        // modify style and add dsIndex if there is not strokeColor or dsIndex
        // Marker
        final String style = dataSet.getStyle();
        final Map<String, String> map = StyleParser.splitIntoMap(style);

        final String stroke = map.get(XYChartCss.DATASET_STROKE_COLOR.toLowerCase());
        final String fill = map.get(XYChartCss.DATASET_FILL_COLOR.toLowerCase());
        final String index = map.get(XYChartCss.DATASET_INDEX.toLowerCase());

        if (stroke == null && fill == null && index == null) {
            map.put(XYChartCss.DATASET_INDEX, Integer.toString(dataSetIndex));
            dataSet.setStyle(StyleParser.mapToString(map));
        }
    }

    @Override
    protected void render(final GraphicsContext gc, final DataSet dataSet, final DataSetNode style) {
        final double originalIntensity = style.getIntensity();
        try {

            // render history in reverse order
            final int nRenderer = renderers.size();
            for (int historyIx = nRenderer - 1; historyIx >= 0; historyIx--) {
                final var historical = renderers.get(historyIx);
                if (historical.getDatasets().size() > style.getLocalIndex()) {
                    // Change the intensity to make the history more faded
                    // Note: we are already in the drawing phase, so the changes won't cause a new pulse
                    // TODO: the fading does not seem to work properly yet. Check HistoryDataSetSample.
                    // TODO: maybe copy the style node so we don't accidentally prevent CSS updates?
                    final var faded = (int) Math.pow(getIntensityFading(), historyIx + 2.0) * originalIntensity;
                    style.setIntensity(faded);

                    // Draw the historical set
                    var histDs = historical.getDatasets().get(style.getLocalIndex());
                    super.render(gc, histDs, style);
                }
            }

        } finally {
            style.setIntensity(originalIntensity);
        }

        super.render(gc, dataSet, style);
    }

    public void shiftHistory() {
        final int nRenderer = renderers.size();
        if (nRenderer <= 0) {
            return;
        }

        final ObservableList<DataSet> oldDataSetsToRemove = renderers.get(nRenderer - 1).getDatasets();
        if (!oldDataSetsToRemove.isEmpty()) {
            try {
                FXUtils.runAndWait(() -> getDatasets().removeAll(oldDataSetsToRemove));
            } catch (final Exception e) {
                LOGGER.atError().setCause(e).log("oldDataSetsToRemove listener");
            }
        }

        // create local copy of to be shifted data set
        final ObservableList<DataSet> copyDataSet = getDatasetsCopy(getLocalDataSets());

        for (int index = nRenderer - 1; index >= 0; index--) {
            final ErrorDataSetRenderer renderer = renderers.get(index);
            final boolean isFirstRenderer = index == 0;
            final ErrorDataSetRenderer previousRenderer = isFirstRenderer ? this : renderers.get(index - 1);

            final ObservableList<DataSet> copyList = isFirstRenderer ? copyDataSet : previousRenderer.getDatasets();

            final int fading = (int) (Math.pow(getIntensityFading(), index + 2.0) * 100);
            for (final DataSet ds : copyList) {
                if (ds instanceof EditableDataSet) {
                    ((EditableDataSet) ds).setName(ds.getName().split("_")[0] + "History_{-" + index + "}");
                }

                // modify style TODO: get rid of old-style style settings
                final String style = ds.getStyle();
                final Map<String, String> map = StyleParser.splitIntoMap(style);
                map.put(XYChartCss.DATASET_INTENSITY.toLowerCase(), Double.toString(fading));
                map.put(XYChartCss.DATASET_SHOW_IN_LEGEND.toLowerCase(), Boolean.toString(false));
                ds.setStyle(StyleParser.mapToString(map));

                if (!getDatasets().contains(ds)) {
                    try {
                        FXUtils.runAndWait(() -> getDatasets().add(ds));
                    } catch (final Exception e) {
                        LOGGER.atError().setCause(e).log("add missing dataset");
                    }
                }
            }

            try {
                FXUtils.runAndWait(() -> renderer.getDatasets().setAll(copyList));
            } catch (final Exception e) {
                LOGGER.atError().setCause(e).log("add new copied dataset to getDatasets()");
            }
        }

        // N.B. added explicit garbage collection to reduce dynamic footprint
        // otherwise this would cause a big saw-tooth like memory footprint
        // which obfuscates debugging/memory-leak
        // checking -> tradeoff between: 'reduced memory footprint' vs.
        // 'significantly reduced CPU efficiency'
        // System.gc();
    }

    private static String setLegendCounter(final String oldStyle, final int count) {
        final Map<String, String> map = StyleParser.splitIntoMap(oldStyle);
        map.put(XYChartCss.DATASET_INDEX, Integer.toString(count));
        return StyleParser.mapToString(map);
    }
}
