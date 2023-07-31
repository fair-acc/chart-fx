package io.fair_acc.sample.math;

import io.fair_acc.sample.chart.ChartSample;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import io.fair_acc.dataset.spi.DefaultDataSet;
import io.fair_acc.math.DataSetMath;
import io.fair_acc.math.filter.FilterType;
import io.fair_acc.math.filter.fir.FirFilter;
import io.fair_acc.sample.math.utils.DemoChart;
import javafx.stage.Stage;

/**
 * Sample to illustrate array-based Butterworth and Chebychev filters
 *
 * @author rstein
 */
public class FrequencyFilterSample extends ChartSample {
    private static final int N_SAMPLES = 8192;
    private static final int N_SAMPLE_RATE = 1000;

    @Override
    public Node getChartPanel(Stage stage) {
        // generate some random samples
        final double[] xValues = new double[N_SAMPLES];
        final double[] yValues = new double[N_SAMPLES];
        double fs = N_SAMPLE_RATE;
        double fcut = 0.1;
        for (int i = 0; i < N_SAMPLES; i++) {
            xValues[i] = i / fs;
            // yValues[i] = i < N_SAMPLES / 2 ? 0.0 : 1.0; // step
        }
        yValues[N_SAMPLES / 2] = N_SAMPLES; // dirac delta
        DefaultDataSet dataSet = new DefaultDataSet("dirac", xValues, yValues, xValues.length, true);

        final DemoChart chartLP = new DemoChart();
        chartLP.getXAxis().setName("frequency");
        chartLP.getYAxis().setName("magnitude");
        chartLP.getYAxis().setUnit("dB");
        chartLP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(dataSet));

        DefaultDataSet butterWorthA = new DefaultDataSet("Butterworth(4th)", xValues,
                FirFilter.filterSignal(yValues, null, fcut, 4, FilterType.LOW_PASS, 0), xValues.length, true);
        chartLP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(butterWorthA));

        DefaultDataSet butterWorthB = new DefaultDataSet("Butterworth(6th)", xValues,
                FirFilter.filterSignal(yValues, null, fcut, 6, FilterType.LOW_PASS, 0), xValues.length, true);
        chartLP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(butterWorthB));

        DefaultDataSet butterWorthC = new DefaultDataSet("Butterworth(8th)", xValues,
                FirFilter.filterSignal(yValues, null, fcut, 8, FilterType.LOW_PASS, 0), xValues.length, true);
        chartLP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(butterWorthC));

        final DemoChart chartHP = new DemoChart();
        chartHP.getXAxis().setName("frequency");
        chartHP.getYAxis().setName("magnitude");
        chartHP.getYAxis().setUnit("dB");
        chartHP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(dataSet));

        DefaultDataSet butterWorthA2 = new DefaultDataSet("Butterworth(4th)", xValues,
                FirFilter.filterSignal(yValues, null, fcut, 4, FilterType.HIGH_PASS, 0.0), xValues.length, true);
        chartHP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(butterWorthA2));

        DefaultDataSet butterWorthB2 = new DefaultDataSet("Butterworth(6th)", xValues,
                FirFilter.filterSignal(yValues, null, fcut, 6, FilterType.HIGH_PASS, 0.0), xValues.length, true);
        chartHP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(butterWorthB2));

        DefaultDataSet butterWorthC2 = new DefaultDataSet("Butterworth(8th)", xValues,
                FirFilter.filterSignal(yValues, null, fcut, 8, FilterType.HIGH_PASS, 0.0), xValues.length, true);
        chartHP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(butterWorthC2));

        return new VBox(chartLP, chartHP);
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}