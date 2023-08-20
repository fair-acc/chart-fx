package io.fair_acc.chartfx.profiler;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.dataset.profiler.DurationMeasure;
import io.fair_acc.dataset.profiler.SimpleDurationMeasure;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.AbstractRendererXY;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.profiler.Profiler;
import io.fair_acc.dataset.spi.AbstractDataSet;
import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;
import io.fair_acc.dataset.utils.DoubleCircularBuffer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Experimental profiler that shows profile information in a chart
 *
 * @author ennerf
 */
public class ChartProfiler implements Profiler {

    public static ChartProfiler showInNewStage(String title) {
        return createChart(title, chart -> {
            var stage = new Stage();
            stage.setScene(new Scene(chart));
            stage.show();
        });
    }

    private static ChartProfiler createChart(String title, Consumer<XYChart> onChart) {
        var chart = new XYChart();
        chart.setTitle("Profiler: " + title);
        chart.getXAxis().setTimeAxis(false);
        chart.getXAxis().setUnit("s");
        chart.getYAxis().setUnit("s");
        chart.getYAxis().setAutoUnitScaling(true);

        var renderer = new ErrorDataSetRenderer();
        renderer.setMarker(DefaultMarker.RECTANGLE);
        renderer.setDrawMarker(true);
        renderer.setDrawBars(false);
        renderer.setDrawBubbles(false);
        renderer.setPolyLineStyle(LineStyle.NONE);
        renderer.setPointReduction(false);
        chart.getRenderers().setAll(renderer);

        var zoomer = new Zoomer();
        zoomer.setAutoZoomEnabled(true);
        chart.getPlugins().add(zoomer);
        onChart.accept(chart);

        return new ChartProfiler(renderer);
    }

    public ChartProfiler(AbstractRendererXY<?> renderer) {
        this.renderer = renderer;
        Runnable updateDataSets = () -> {
            for (Runnable updateAction : updateActions) {
                updateAction.run();
            }
            state.clear();
            // TODO: figure out why empty datasets fail to get started
            if (renderer.getChart() != null) {
                renderer.getChart().invalidate();
            }
        };
        state.addChangeListener((src, bits) -> Platform.runLater(updateDataSets));
    }

    @Override
    public DurationMeasure newDuration(String tag) {
        // The data gets generated during the draw phase, so the dataSet may
        // be locked and can't be modified. We solve this by storing the data
        // in intermediate arrays.
        final var x = new DoubleArrayList(10);
        final var y = new DoubleArrayList(10);
        final var measure = SimpleDurationMeasure.usingNanoTime(duration -> {
            x.add((System.nanoTime() - nanoStartOffset) * 1E-9);
            y.add(duration * 1E-9);
            state.setDirty(ChartBits.DataSetDataAdded);
        });

        // Do a batch update during the next pulse
        final var dataSet = new CircularDoubleDataSet2D(tag, defaultCapacity);
        updateActions.add(() -> {
            for (int i = 0; i < x.size(); i++) {
                dataSet.add(x.getDouble(i), y.getDouble(i));
            }
            x.clear();
            y.clear();
        });

        renderer.getDatasets().add(dataSet);
        return measure;
    }

    final Renderer renderer;
    final List<Runnable> updateActions = new ArrayList<>();
    final BitState state = BitState.initClean(this);
    private final long nanoStartOffset = System.nanoTime();

    private static class CircularDoubleDataSet2D extends AbstractDataSet<CircularDoubleDataSet2D> {

        public CircularDoubleDataSet2D(String name, int capacity) {
            super(name, 2);
            x = new DoubleCircularBuffer(capacity);
            y = new DoubleCircularBuffer(capacity);
        }

        @Override
        public double get(int dimIndex, int index) {
            switch (dimIndex) {
                case DIM_X:
                    return x.get(index);
                case DIM_Y:
                    return y.get(index);
                default:
                    return Double.NaN;
            }
        }

        public void add(double x, double y) {
            this.x.put(x);
            this.y.put(y);
            getAxisDescription(DIM_X).add(x);
            getAxisDescription(DIM_Y).add(y);
            fireInvalidated(ChartBits.DataSetData);
        }

        @Override
        public int getDataCount() {
            return x.available();
        }

        @Override
        public DataSet set(DataSet other, boolean copy) {
            throw new UnsupportedOperationException();
        }

        protected final DoubleCircularBuffer x;
        protected final DoubleCircularBuffer y;

    }

    private static final int defaultCapacity = 60 /* Hz */ * 60 /* s */;

}
