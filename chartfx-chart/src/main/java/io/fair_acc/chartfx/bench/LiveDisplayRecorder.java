package io.fair_acc.chartfx.bench;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.ChartPlugin;
import io.fair_acc.chartfx.plugins.CrosshairIndicator;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.AbstractRendererXY;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.bench.MeasurementRecorder;
import io.fair_acc.bench.TimeMeasure;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Recorder that shows measurements in a real-time chart. It currently
 * has no explicit support for measurements recorded on background threads.
 *
 * @author ennerf
 */
public class LiveDisplayRecorder implements MeasurementRecorder {

    public static LiveDisplayRecorder showInNewStage(String title) {
        return createChart(title, chart -> {
            var stage = new Stage();
            stage.setScene(new Scene(chart));
            stage.show();
        });
    }

    public static LiveDisplayRecorder createChart(String title, Consumer<Parent> onChart) {
        // Top chart w/ time series
        var timeChart = new XYChart(createTimeAxisX(), createValueAxisY());
        timeChart.setTitle("Benchmark: " + title);
        timeChart.getPlugins().addAll(createPlugins());
        timeChart.getLegend().getNode().setVisible(false); // TODO: somehow it still shows up without any datasets?
        timeChart.getLegend().getNode().setManaged(false);

        var timeRenderer = new ErrorDataSetRenderer();
        timeRenderer.setDrawMarker(true);
        timeRenderer.setPolyLineStyle(LineStyle.NONE);
        timeRenderer.setPointReduction(false);
        timeRenderer.setAllowNaNs(false);
        timeChart.getRenderers().setAll(timeRenderer);

        // Bottom chart w/ percentile plot
        var percentileChart = new XYChart(createPercentileAxisX(), createValueAxisY());
        percentileChart.getPlugins().addAll(createPlugins());

        var percentileRenderer = new ErrorDataSetRenderer();
        percentileRenderer.setDrawMarker(false);
        percentileRenderer.setPolyLineStyle(LineStyle.NORMAL);
        percentileRenderer.setPointReduction(false);
        percentileRenderer.setAllowNaNs(false);
        percentileChart.getRenderers().setAll(percentileRenderer);

        var recorder = new LiveDisplayRecorder(timeRenderer, percentileRenderer);
        var clearBtn = new Button("clear");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(a -> recorder.clear());

        final Button clearButton = new Button(null, new FontIcon("fa-trash:22"));
        clearButton.setPadding(new Insets(3, 3, 3, 3));
        clearButton.setTooltip(new Tooltip("clears existing data"));
        clearButton.setOnAction(e -> recorder.clear());
        percentileChart.getToolBar().getChildren().add(clearButton);

        var pane = new SplitPane(timeChart, percentileChart);
        pane.setOrientation(Orientation.VERTICAL);
        onChart.accept(pane);
        return recorder;
    }

    private static DefaultNumericAxis createTimeAxisX() {
        var axis = new DefaultNumericAxis();
        axis.setTimeAxis(true);
        axis.setName("time");
        axis.setTimeAxis(false);
        axis.setUnit("s");
        return axis;
    }

    private static DefaultNumericAxis createValueAxisY() {
        var axis = new DefaultNumericAxis();
        axis.setForceZeroInRange(true);
        axis.setName("value");
        axis.setUnit("s");
        axis.setAutoUnitScaling(true);
        return axis;
    }

    private static DefaultNumericAxis createPercentileAxisX() {
        var axis = new PercentileAxis();
        axis.setName("percentile");
        axis.setUnit("%");
        return axis;
    }

    private static ChartPlugin[] createPlugins() {
        var zoomer = new Zoomer();
        zoomer.setAddButtonsToToolBar(false);
        zoomer.setAnimated(false);
        zoomer.setSliderVisible(false);
        zoomer.setAutoZoomEnabled(true);
        var crosshair = new CrosshairIndicator();
        return new ChartPlugin[]{zoomer/*,crosshair*/};
    }

    public LiveDisplayRecorder(AbstractRendererXY<?> timeSeriesRenderer, AbstractRendererXY<?> percentileRenderer) {
        this.timeSeriesRenderer = timeSeriesRenderer;
        this.percentileRenderer = percentileRenderer;
        Runnable updateDataSets = () -> {
            for (Runnable updateAction : updateActions) {
                updateAction.run();
            }
            state.clear();

            // TODO: figure out why empty datasets fail to get started
            Optional.ofNullable(timeSeriesRenderer.getChart()).ifPresent(Chart::invalidate);
            Optional.ofNullable(percentileRenderer.getChart()).ifPresent(Chart::invalidate);
        };
        state.addChangeListener((src, bits) -> Platform.runLater(updateDataSets));
    }

    public void clear() {
        nanoStartOffset = System.nanoTime();
        clearActions.forEach(Runnable::run);
    }

    @Override
    public TimeMeasure newTime(String tag, IntSupplier level) {
        // The data gets generated during the draw phase, so the dataSet may
        // be locked and can't be modified. We solve this by storing the data
        // in intermediate arrays.
        final var x = new DoubleArrayList(10);
        final var y = new DoubleArrayList(10);
        final TimeMeasure trace = (unit, duration) -> {
            x.add((System.nanoTime() - nanoStartOffset) * 1E-9);
            y.add(unit.toNanos(duration) * 1E-9);
            state.setDirty(ChartBits.DataSetDataAdded);
        };

        // Do a batch update during the next pulse
        final var timeSeriesDs = new CircularDoubleDataSet2D(tag, defaultCapacity);
        final var percentileDs = new HdrHistogramDataSet(tag);
        updateActions.add(() -> {
            for (int i = 0; i < x.size(); i++) {
                timeSeriesDs.add(x.getDouble(i), y.getDouble(i));
                percentileDs.add(y.getDouble(i));
            }
            percentileDs.convertHistogramToXY();
            x.clear();
            y.clear();
        });
        clearActions.add(() -> {
            timeSeriesDs.clear();
            percentileDs.clear();
            percentileDs.convertHistogramToXY();
            x.clear();
            y.clear();
        });

        var timeNode = timeSeriesRenderer.addDataSet(timeSeriesDs);
        var percentileNode = percentileRenderer.addDataSet(percentileDs);
        percentileNode.visibleProperty().bindBidirectional(timeNode.visibleProperty());

        return trace;
    }

    final Renderer timeSeriesRenderer;
    final Renderer percentileRenderer;
    final List<Runnable> updateActions = new ArrayList<>();
    final List<Runnable> clearActions = new ArrayList<>();
    final BitState state = BitState.initClean(this);
    private long nanoStartOffset = System.nanoTime();

    private static final int defaultCapacity = 60 /* Hz */ * 60 /* s */;

}
