package de.gsi.chart.samples.legacy.utils;

import javafx.application.Application;
import javafx.scene.Node;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.datareduction.DefaultDataReducer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.renderer.spi.ReducingLineRenderer;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.testdata.spi.SineFunction;

public class TestChart extends AbstractTestApplication implements ChartTestCase {
    protected int nSamples = MAX_DATA_POINTS_100K;
    protected final XYChart chart;
    protected final DefaultNumericAxis xAxis = new DefaultNumericAxis();
    protected final DefaultNumericAxis yAxis = new DefaultNumericAxis("irrelevant y-axis test case 2", -1.1, +1.1, 0.2);
    protected SineFunction testFunction = new SineFunction("test", nSamples, true);
    protected final DoubleDataSet dataSet = new DoubleDataSet("test");

    public TestChart() {
        this(false);
    }

    public TestChart(final boolean altRenderer) {
        chart = new XYChart(xAxis, yAxis);
        chart.legendVisibleProperty().set(true);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        xAxis.setAutoRangeRounding(false);
        // chart.getDatasets().add(dataSet);
        chart.getDatasets().setAll(testFunction);
        if (altRenderer) {
            xAxis.setName("x-axis (new Chart - ReducingLinerenderer)");
            yAxis.setName("irrelevant y-axis - ReducingLinerenderer");
            ReducingLineRenderer renderer = new ReducingLineRenderer();
            renderer.setMaxPoints(750);
            chart.getRenderers().set(0, renderer);
            chart.getCanvas().widthProperty().addListener((ch, o, n) -> renderer.setMaxPoints(Math.max(750, (int) (n.doubleValue() / 5.0))));

            // xAxis.setLabel("x-axis (new Chart - ErrorDataSetRenderer -
            // parallel)");
            // yAxis.setLabel("irrelevant y-axis - ErrorDataSetRenderer");
            // ErrorDataSetRenderer renderer = (ErrorDataSetRenderer)
            // chart.getRenderers().get(0);
            // renderer.setDrawBars(false);
            // renderer.setDrawMarker(false);
            // renderer.setErrorType(ErrorStyle.NONE);
            // renderer.setParallelImplementation(true);
            // DefaultDataReducer reducer = (DefaultDataReducer)
            // renderer.getRendererDataReducer();
            // reducer.setMinPointPixelDistance(5);
        } else {
            xAxis.setName("x-axis (new Chart - ErrorDataSetRenderer)");
            yAxis.setName("irrelevant y-axis - ErrorDataSetRenderer");
            ErrorDataSetRenderer renderer = (ErrorDataSetRenderer) chart.getRenderers().get(0);
            renderer.setDrawBars(false);
            renderer.setDrawMarker(false);
            renderer.setErrorType(ErrorStyle.NONE);
            renderer.setParallelImplementation(false);
            DefaultDataReducer reducer = (DefaultDataReducer) renderer.getRendererDataReducer();
            reducer.setMinPointPixelDistance(5);
            renderer.setAllowNaNs(false);
        }

        setNumberOfSamples(MAX_DATA_POINTS_1K);
        updateDataSet(); // NOPMD
    }

    @Override
    public Node getChart(int nSamples) {
        return chart;
    }

    @Override
    public void initChart() {
        test = new TestChart(false);
    }

    @Override
    public void setNumberOfSamples(int nSamples) {
        this.nSamples = nSamples;
        testFunction = new SineFunction("test", nSamples, true);
        chart.getDatasets().setAll(testFunction);
        xAxis.setMax(nSamples - 1.0);
        xAxis.setMin(0);
        xAxis.setTickUnit(nSamples / 20.0);
        updateDataSet();
    }

    @Override
    public void updateDataSet() {
        // final double[] x = testFunction.generateX(nSamples);
        // final double[] y = testFunction.generateY(nSamples);
        //
        // dataSet.set(x, y);
        testFunction.update();
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
