package io.fair_acc.chartfx.profiler;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
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
public class LiveChartProfiler extends XYChart {

    public static Profiler showInNewStage(String title) {
        return createChart(title, chart -> {
            var stage = new Stage();
            stage.setScene(new Scene(chart));
            stage.show();
        });
    }

    private static Profiler createChart(String title, Consumer<XYChart> onChart) {
        var profiler = new ProfileRenderer();
        var chart = new XYChart();
        chart.setTitle("Profiler: " + title);
        chart.getXAxis().setTimeAxis(false);
        chart.getXAxis().setUnit("s");
        chart.getYAxis().setUnit("s");
        chart.getYAxis().setAutoUnitScaling(true);
        chart.getRenderers().setAll(profiler);
        var zoomer = new Zoomer();
        zoomer.setAutoZoomEnabled(true);
        chart.getPlugins().add(zoomer);
        onChart.accept(chart);
//        chart.setProfiler(Profiler.DEBUG_PRINTER);
        return profiler;
    }

    private static class ProfileRenderer extends ErrorDataSetRenderer implements Profiler {

        public ProfileRenderer() {
            setMarker(DefaultMarker.RECTANGLE);
            setDrawMarker(true);
            setDrawBars(false);
            setDrawBubbles(false);
            setPolyLineStyle(LineStyle.NONE);
            setPointReduction(false);
        }

        @Override
        public DurationMeasurement newDuration(String tag) {
            var measurement = new DataSetMeasurement(tag, this, nanoStartOffset);
            getDatasets().add(measurement.dataSet);
            return measurement;
        }

        private final long nanoStartOffset = System.nanoTime();

    }

    private static class DataSetMeasurement extends DurationMeasurement.AbstractDurationMeasurement {

        final BitState localState = BitState.initClean(this).addChangeListener((src, bits) -> {
            this.updateDataSet();
        });

        protected DataSetMeasurement(String tag, ProfileRenderer renderer, long nanoStartOffset) {
            super(System::nanoTime);
            this.nanoStartOffset = nanoStartOffset;
            this.dataSet = new CircularDoubleDataSet2D(tag, defaultCapacity);
            Runnable updateDataSet = () -> {
                updateDataSet();
                // TODO: figure out why empty datasets fail to get started
                if (renderer.getChart() != null) {
                    renderer.getChart().invalidate();
                }
            };
            this.dataSet.getBitState().addChangeListener(ChartBits.DataSetDataAdded, (src, bits) -> {
                Platform.runLater(updateDataSet);
            });
        }

        @Override
        protected void recordDuration(long duration) {
            // the data gets generated during the draw phase,
            // so we can't modify the dataset, but we also can't
            // block the FX thread. This triggers an event that
            // will add all available data during the next pass.
            localState.setDirty(ChartBits.DataSetDataAdded);
            timeSec.add((System.nanoTime() - nanoStartOffset) * 1E-9);
            durationSec.add(duration * 1E-9);
        }

        public void updateDataSet() {
            final int n = timeSec.size();
            for (int i = 0; i < n; i++) {
                dataSet.add(timeSec.getDouble(i), durationSec.getDouble(i));
            }
            timeSec.clear();
            durationSec.clear();
            localState.clear();
        }

        private final DoubleArrayList timeSec = new DoubleArrayList(10);
        private final DoubleArrayList durationSec = new DoubleArrayList(10);
        private final CircularDoubleDataSet2D dataSet;
        private final long nanoStartOffset;

    }

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
