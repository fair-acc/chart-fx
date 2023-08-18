package io.fair_acc.sample.math;

import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.math.DataSetMath;
import io.fair_acc.math.functions.TrigCosineFunction;
import io.fair_acc.math.functions.TrigSineFunction;
import io.fair_acc.sample.chart.ChartSample;
import io.fair_acc.sample.math.utils.DemoChart;
import io.fair_acc.math.spectra.Apodization;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Random;

/**
 * example illustrating the Fast-Fourier transform and math operations directly on DataSet
 * 
 * N.B. Zoom into the peaks to see the details
 * 
 * N.B. also works with unequal sampling and ranges of input data
 * 
 * @author rstein
 */
public class DataSetSpectrumSample extends ChartSample {
    protected static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final int N_SAMPLES = 200;
    private static boolean unequalSampling;

    @Override
    public Node getChartPanel(Stage stage) {

        final DemoChart chart = new DemoChart();
        chart.getRenderer(0).setDrawMarker(false);
        chart.getRenderer(0).setErrorStyle(ErrorStyle.ERRORSURFACE);

        TrigSineFunction sineFunction = new TrigSineFunction("sine") {
            @Override
            public double getValue(final double x) {
                return super.getValue(x) + 0.001 * RANDOM.nextGaussian();
            }
        };
        sineFunction.setParameterValue(1, 1); // frequency

        TrigCosineFunction cosineFunction = new TrigCosineFunction("cos");
        cosineFunction.setParameterValue(1, 3); // frequency

        DataSet dataSet1 = sineFunction.getDataSetEstimate(-10.0, +10.0, N_SAMPLES);
        DataSet dataSet2 = unequalSampling ? cosineFunction.getDataSetEstimate(-8.0, +8.0, N_SAMPLES)
                : cosineFunction.getDataSetEstimate(-10.0, +10.0, N_SAMPLES);

        // test adding of functions
        DataSet dataSet = DataSetMath.addFunction(dataSet1, dataSet2);

        chart.getRenderer(0).getDatasets().addAll(dataSet1, dataSet2, dataSet);

        final boolean normaliseFrequency = false;
        final boolean dbScale = false;
        final DemoChart chart2 = new DemoChart(2);
        chart2.getRenderer(0).setDrawMarker(true);
        chart2.getRenderer(0).setErrorStyle(ErrorStyle.ERRORSURFACE);
        chart2.getXAxis().setName("frequency");
        chart2.getXAxis().setUnit(normaliseFrequency ? "fs" : "Hz");
        chart2.getYAxis().setName("magnitude");
        chart2.getYAxis().setUnit("a.u.");
        chart2.getYAxis(1).setName("magnitude");
        chart2.getYAxis(1).setUnit("dB");
        chart2.getYAxis().setLogAxis(true);
        chart2.getYAxis().setLogarithmBase(10);

        // N.B. first calculate magnitude spectrum then convert to dB scale
        DataSet magnitudeSpectrum = DataSetMath.magnitudeSpectrum(dataSet, Apodization.Hann, dbScale,
                normaliseFrequency);
        magnitudeSpectrum.setStyle("strokeColor=red;fillColor=red;strokeWidth=1");
        chart2.getRenderer(0).getDatasets().addAll(magnitudeSpectrum);

        DataSet magnitudeSpectrumDecibel = DataSetMath.dbFunction(magnitudeSpectrum);
        magnitudeSpectrumDecibel.setStyle("strokeColor=darkgreen;fillColor=darkgreen;strokeWidth=1");
        chart2.getRenderer(1).getDatasets().addAll(magnitudeSpectrumDecibel);

        // N.B. the direct way to dB scaled magnitude spectrum
        DataSet control = DataSetMath.magnitudeSpectrumDecibel(dataSet);
        control.setStyle("strokeColor=cyan;fillColor=cyan;strokeWidth=1");
        chart2.getRenderer(1).getDatasets().addAll(control);

        return new VBox(chart, chart2);
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}