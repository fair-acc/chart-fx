package io.fair_acc.sample.chart;

import static io.fair_acc.dataset.spi.AbstractHistogram.HistogramOuterBounds.BINS_CENTERED_ON_BOUNDARY;

import java.util.ArrayDeque;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.ParameterMeasurements;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.spi.HistogramRenderer;
import io.fair_acc.chartfx.renderer.spi.MetaDataRenderer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.Histogram;
import io.fair_acc.dataset.testdata.spi.RandomDataGenerator;
import io.fair_acc.math.DataSetMath;
import io.fair_acc.math.MathDataSet;

public class HistogramRendererSample extends ChartSample {
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 40; // [ms]
    private static final int N_BINS = 20;
    private final double[] xBins = { 0.0, 0.1, 0.2, 0.3, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0, 15.0, 16.0, 17.0, 18.0, 19.0, 19.7, 19.8, 19.9, 20.0 };
    private final Histogram dataSet1 = new Histogram("Histo1", N_BINS + 1, 0.0, N_BINS, BINS_CENTERED_ON_BOUNDARY);
    private final Histogram dataSet2 = new Histogram("Histo2", N_BINS + 1, 0.0, N_BINS, BINS_CENTERED_ON_BOUNDARY);
    private final Histogram dataSet3 = new Histogram("Histo3", xBins); // custom, non-equidistant histogram

    private int counter;

    private void fillData() {
        counter++;
        dataSet1.fill(RandomDataGenerator.nextGaussian() * 3 + 8.0);
        dataSet2.fill(RandomDataGenerator.nextGaussian() * 2 + 14.0);

        if (counter % 10 == 0) {
            dataSet3.fill(RandomDataGenerator.nextGaussian() * 3 + 10.0);
        }

        if (counter % 2000 == 0) {
            // reset distribution every now and then
            counter = 0;
            dataSet1.reset();
            dataSet2.reset();
            dataSet3.reset();
        }
    }

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final BorderPane root = new BorderPane();

        // first chart
        final XYChart chart1 = new XYChart();
        chart1.setTitle("Basic Histograms");
        final Zoomer zoomer = new Zoomer();
        zoomer.setAutoZoomEnabled(true);
        zoomer.setSliderVisible(false);
        chart1.getPlugins().addAll(new ParameterMeasurements(), new EditAxis(), zoomer);

        final HistogramRenderer renderer1 = new HistogramRenderer();
        renderer1.getDatasets().addAll(dataSet2);
        renderer1.setPolyLineStyle(LineStyle.HISTOGRAM_FILLED);
        chart1.getRenderers().set(0, renderer1);
        final HistogramRenderer renderer2 = new HistogramRenderer();
        renderer2.getDatasets().addAll(dataSet1, dataSet3);
        dataSet1.setStyle("strokeColor:red; strokeWidth:3");
        dataSet3.setStyle("strokeColor:green; strokeWidth:3");
        renderer2.setPolyLineStyle(LineStyle.HISTOGRAM);
        chart1.getRenderers().add(renderer2);

        final MetaDataRenderer metaRenderer = new MetaDataRenderer(chart1);
        metaRenderer.getDatasets().addAll(dataSet2, dataSet1);
        chart1.getRenderers().add(metaRenderer);

        // second chart
        final XYChart chart2 = new XYChart();
        chart2.setTitle("Stacked Histograms");
        final Zoomer zoomer2 = new Zoomer();
        zoomer2.setAutoZoomEnabled(true);
        zoomer2.setSliderVisible(false);
        chart2.getPlugins().addAll(new ParameterMeasurements(), new EditAxis(), zoomer2);
        chart2.getRenderers().setAll(new HistogramRenderer());
        final SummingDataSet dataSetSum31 = new SummingDataSet("Sum", dataSet1, dataSet3);
        final SummingDataSet dataSetSum312 = new SummingDataSet("Sum", dataSet1, dataSet2, dataSet3);
        chart2.getDatasets().addAll(dataSetSum312, dataSetSum31, dataSet3);

        HBox.setHgrow(chart1, Priority.ALWAYS);
        HBox.setHgrow(chart2, Priority.ALWAYS);
        root.setCenter(new HBox(chart1, chart2));

        final Timer timer = new Timer("sample-update-timer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                fillData();
            }
        }, UPDATE_DELAY, UPDATE_PERIOD);

        primaryStage.setOnCloseRequest(evt -> {
            timer.cancel();
            Platform.exit();
        });
        return root;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }

    public static class SummingDataSet extends MathDataSet { // NOSONAR NOPMD -- too many parents is out of our control (Java intrinsic)
        public SummingDataSet(final String name, final DataSet... functions) {
            super(name, (dataSets, returnFunction) -> {
                if (dataSets.isEmpty()) {
                    return;
                }
                final ArrayDeque<DataSet> lockQueue = new ArrayDeque<>(dataSets.size());
                try {
                    dataSets.forEach(ds -> {
                        lockQueue.push(ds);
                        ds.lock().readLock();
                    });
                    returnFunction.clearData();
                    final DataSet firstDataSet = dataSets.get(0);
                    returnFunction.add(firstDataSet.get(DIM_X, 0), 0);
                    returnFunction.add(firstDataSet.get(DIM_X, firstDataSet.getDataCount() - 1), 0);
                    dataSets.forEach(ds -> returnFunction.set(DataSetMath.addFunction(returnFunction, ds), false));
                } finally {
                    // unlock in reverse order
                    while (!lockQueue.isEmpty()) {
                        lockQueue.pop().lock().readUnLock();
                    }
                }
            }, functions);
        }
    }
}
