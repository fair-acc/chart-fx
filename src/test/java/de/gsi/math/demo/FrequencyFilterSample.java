package de.gsi.math.demo;

import de.gsi.chart.data.spi.DoubleDataSet;
import de.gsi.math.ArrayMath;
import de.gsi.math.ArrayMath.FilterType;
import de.gsi.math.DataSetMath;
import de.gsi.math.demo.utils.AbstractDemoApplication;
import de.gsi.math.demo.utils.DemoChart;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * Sample to illustrate array-based Butterworth and Chebychev filters
 * 
 * @author rstein
 */
public class FrequencyFilterSample extends AbstractDemoApplication {
    private static final int N_SAMPLES = 8192;
    private static final int N_SAMPLE_RATE = 1000;

    @Override
    public Node getContent() {
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
        DoubleDataSet dataSet = new DoubleDataSet("dirac", xValues, yValues);

        final DemoChart chartLP = new DemoChart();
        chartLP.getXAxis().setLabel("frequency");
        chartLP.getYAxis().setLabel("magnitude");
        chartLP.getYAxis().setUnit("dB");
        chartLP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(dataSet));

        DoubleDataSet butterWorthA = new DoubleDataSet("Butterworth(4th)", xValues,
                ArrayMath.filterSignal(yValues, fcut, 4, FilterType.LOW_PASS, 0));
        chartLP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(butterWorthA));

        DoubleDataSet butterWorthB = new DoubleDataSet("Butterworth(6th)", xValues,
                ArrayMath.filterSignal(yValues, fcut, 6, FilterType.LOW_PASS, 0));
        chartLP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(butterWorthB));

        DoubleDataSet butterWorthC = new DoubleDataSet("Butterworth(8th)", xValues,
                ArrayMath.filterSignal(yValues, fcut, 8, FilterType.LOW_PASS, 0));
        chartLP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(butterWorthC));

        final DemoChart chartHP = new DemoChart();
        chartHP.getXAxis().setLabel("frequency");
        chartHP.getYAxis().setLabel("magnitude");
        chartHP.getYAxis().setUnit("dB");
        chartHP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(dataSet));

        DoubleDataSet butterWorthA2 = new DoubleDataSet("Butterworth(4th)", xValues,
                ArrayMath.filterSignal(yValues, fcut, 4, FilterType.HIGH_PASS, 0.0));
        chartHP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(butterWorthA2));

        DoubleDataSet butterWorthB2 = new DoubleDataSet("Butterworth(6th)", xValues,
                ArrayMath.filterSignal(yValues, fcut, 6, FilterType.HIGH_PASS, 0.0));
        chartHP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(butterWorthB2));

        DoubleDataSet butterWorthC2 = new DoubleDataSet("Butterworth(8th)", xValues,
                ArrayMath.filterSignal(yValues, fcut, 8, FilterType.HIGH_PASS, 0.0));
        chartHP.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(butterWorthC2));

        return new VBox(chartLP, chartHP);
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}