package io.fair_acc.sample.math;

import io.fair_acc.sample.chart.ChartSample;
import javafx.application.Application;
import javafx.scene.Node;

import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;
import io.fair_acc.dataset.utils.LimitedQueue;
import io.fair_acc.math.DataSetMath;
import io.fair_acc.math.functions.SigmoidFunction;
import io.fair_acc.sample.math.utils.DemoChart;

import java.util.Random;

/**
 * Sample to illustrate averaging over several data sets with an IIR and FIR low-pass filter
 * 
 * @author rstein
 */
public class DataSetAverageSample extends ChartSample {
    protected static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetAverageSample.class);
    private static final int N_GRAPHS = 20;
    private static final int N_SAMPLES = 100;
    private DataSet oldAverageDataSet;
    private final DataSet oldAverageDataSet2 = new DoubleErrorDataSet("var2");

    @Override
    public Node getChartPanel(Stage stage) {
        final DemoChart chart = new DemoChart(2);
        chart.getRenderer(0).setPolyLineStyle(LineStyle.NONE);
        chart.getRenderer(0).setErrorStyle(ErrorStyle.NONE);
        chart.getRenderer(1).setErrorStyle(ErrorStyle.ERRORSURFACE);
        chart.getYAxis(1).setAutoRanging(false);
        chart.getYAxis().maxProperty().bindBidirectional(chart.getYAxis(1).maxProperty());
        chart.getYAxis().minProperty().bindBidirectional(chart.getYAxis(1).minProperty());
        chart.getYAxis().tickUnitProperty().bindBidirectional(chart.getYAxis(1).tickUnitProperty());

        LimitedQueue<DataSet> lastDataSets = new LimitedQueue<>(N_GRAPHS);
        for (int i = 0; i < 20 * N_GRAPHS; i++) {
            SigmoidFunction sigmoidFunction = new SigmoidFunction("sigmoid") {
                @Override
                public double getValue(final double x) {
                    return 10.0 + super.getValue(x) + 0.05 * RANDOM.nextGaussian();
                }
            };

            DataSet dataSet = sigmoidFunction.getDataSetEstimate(-10.0, +10.0, N_SAMPLES);
            dataSet.setStyle("strokeColor=darkblue;fillColor=darkblue;strokeWidth=0.5");
            lastDataSets.add(dataSet);

            oldAverageDataSet = DataSetMath.averageDataSetsIIR(oldAverageDataSet, oldAverageDataSet2, dataSet,
                    N_GRAPHS);
        }

        chart.getRenderer(0).getDatasets().setAll(lastDataSets);
        DataSet filteredFIR = DataSetMath.averageDataSetsFIR(lastDataSets, N_GRAPHS);
        filteredFIR.setStyle("strokeColor=red;fillColor=red;strokeWidth=1");
        oldAverageDataSet.setStyle("strokeColor=darkOrange;fillColor=darkOrange;strokeWidth=2");

        chart.getRenderer(1).getDatasets().addAll(filteredFIR, oldAverageDataSet);

        LOGGER.atInfo().log("value at zero = " + filteredFIR.get(DataSet.DIM_Y, 0));

        return chart;
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
