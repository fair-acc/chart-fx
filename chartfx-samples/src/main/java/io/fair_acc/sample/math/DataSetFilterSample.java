package io.fair_acc.sample.math;

import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.math.DataSetMath;
import io.fair_acc.math.functions.GaussianFunction;
import io.fair_acc.sample.chart.ChartSample;
import io.fair_acc.sample.math.utils.DemoChart;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.util.Random;

/**
 * Sample to illustrate low-pass and median filtering of a data set
 * 
 * @author rstein
 *
 */
public class DataSetFilterSample extends ChartSample {
    protected static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final int N_SAMPLES = 1000;
    private static final double FILTER_BW = 0.2;

    @Override
    public Node getChartPanel(Stage stage) {

        final DemoChart chart = new DemoChart();
        chart.getRenderer(0).setDrawMarker(false);
        chart.getRenderer(0).setErrorStyle(ErrorStyle.ERRORSURFACE);

        GaussianFunction gaussFunction = new GaussianFunction("gauss") {
            @Override
            public double getValue(final double x) {
                final double spike = RANDOM.nextDouble() > 0.98 ? 1.0 : 0.0;
                return spike == 0 ? super.getValue(x) + 0.01 * RANDOM.nextGaussian() : spike;
            }
        };
        gaussFunction.setParameterValue(0, 0.0); // mean
        gaussFunction.setParameterValue(1, 0.8); // sigma
        gaussFunction.setParameterValue(2, 1.0); // scale

        DataSet dataSet = gaussFunction.getDataSetEstimate(-10.0, +10.0, N_SAMPLES);
        dataSet.setStyle("strokeColor=darkblue;fillColor=darkblue;strokeWidth=1.0");

        chart.getRenderer(0).getDatasets().add(dataSet);

        DataSet lowPassFiltered = DataSetMath.lowPassFilterFunction(dataSet, FILTER_BW);
        lowPassFiltered.setStyle("strokeColor=red;fillColor=red;strokeWidth=2");
        chart.getRenderer(0).getDatasets().addAll(lowPassFiltered);

        DataSet medianFiltered = DataSetMath.medianFilteredFunction(dataSet, FILTER_BW);
        medianFiltered.setStyle("strokeColor=green;fillColor=green;strokeWidth=2");
        chart.getRenderer(0).getDatasets().addAll(medianFiltered);

        DataSet control = DataSetMath.lowPassFilterFunction(medianFiltered, FILTER_BW);
        control.setStyle("strokeColor=cyan;fillColor=cyan;strokeWidth=8");
        chart.getRenderer(0).getDatasets().addAll(control);

        DataSet lowPassFiltered2 = DataSetMath.iirLowPassFilterFunction(medianFiltered, FILTER_BW);
        lowPassFiltered2.setStyle("strokeColor=orange;fillColor=red;strokeWidth=2");
        chart.getRenderer(0).getDatasets().addAll(lowPassFiltered2);

        return chart;
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}