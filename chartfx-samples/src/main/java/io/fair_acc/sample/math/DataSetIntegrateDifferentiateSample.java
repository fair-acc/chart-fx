package io.fair_acc.sample.math;

import io.fair_acc.sample.chart.ChartSample;
import javafx.application.Application;
import javafx.scene.Node;

import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DefaultErrorDataSet;
import io.fair_acc.math.DataSetMath;
import io.fair_acc.math.functions.GaussianFunction;
import io.fair_acc.sample.math.utils.DemoChart;
import javafx.stage.Stage;

import java.util.Random;

/**
 * Sample to illustrate integral and differentiation of data sets low-pass filter
 * 
 * @author rstein
 */
public class DataSetIntegrateDifferentiateSample extends ChartSample {
    protected static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final int N_SAMPLES = 100;

    @Override
    public Node getChartPanel(Stage stage) {
        final DemoChart chart = new DemoChart();
        chart.getRenderer(0).setDrawMarker(false);
        chart.getRenderer(0).setErrorStyle(ErrorStyle.ERRORSURFACE);

        GaussianFunction gaussFunction = new GaussianFunction("gauss") {
            @Override
            public double getValue(final double x) {
                return super.getValue(x) + 0 * 0.005 * RANDOM.nextGaussian();
            }
        };
        gaussFunction.setParameterValue(0, 0.0); // mean
        gaussFunction.setParameterValue(1, 0.8); // sigma
        gaussFunction.setParameterValue(2, 1.0); // scale

        DataSet dataSet = gaussFunction.getDataSetEstimate(-10.0, +10.0, N_SAMPLES);
        dataSet.setStyle("strokeColor=darkblue;fillColor=darkblue;strokeWidth=0.5");
        // add some errors
        if (dataSet instanceof DefaultErrorDataSet) {
            DefaultErrorDataSet ds = (DefaultErrorDataSet) dataSet;
            for (int i = 0; i < ds.getDataCount(); i++) {
                final double x = ds.getX(i);
                final double y = ds.getY(i);
                ds.set(i, x, y, 0.05, 0.05);
            }
        }

        chart.getRenderer(0).getDatasets().add(dataSet);

        DataSet integral = DataSetMath.integrateFunction(dataSet);
        integral.setStyle("strokeColor=red;fillColor=red;strokeWidth=1");
        chart.getRenderer(0).getDatasets().addAll(integral);

        DataSet derivative = DataSetMath.derivativeFunction(dataSet);
        derivative.setStyle("strokeColor=darkgreen;fillColor=darkgreen;strokeWidth=1");
        chart.getRenderer(0).getDatasets().addAll(derivative);

        DataSet control = DataSetMath.integrateFunction(derivative);
        control.setStyle("strokeColor=cyan;fillColor=cyan;strokeWidth=1");
        // renderer1.getDatasets().addAll(control);

        return chart;
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}