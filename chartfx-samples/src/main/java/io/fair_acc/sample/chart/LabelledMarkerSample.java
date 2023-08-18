package io.fair_acc.sample.chart;

import io.fair_acc.dataset.utils.DataSetStyleBuilder;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.stage.Stage;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.renderer.spi.LabelledMarkerRenderer;
import io.fair_acc.dataset.spi.DoubleDataSet;

/**
 * Example to illustrate the use and customisation of the LabelledMarkerRenderer
 *
 * @author rstein
 */
public class LabelledMarkerSample extends ChartSample {
    private static final int N_SAMPLES = 10;

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final XYChart chart = new XYChart(new DefaultNumericAxis(), new DefaultNumericAxis());
        chart.getRenderers().set(0, new LabelledMarkerRenderer());
        chart.legendVisibleProperty().set(true);

        final DoubleDataSet dataSet = new DoubleDataSet("myData");
        final var style = DataSetStyleBuilder.newInstance();

        for (int n = 0; n < LabelledMarkerSample.N_SAMPLES; n++) {
            if (n != 4) {
                dataSet.add(n, n, "DataLabel#" + n);
            } else {
                // index '4' has no label and is not drawn
                dataSet.add(n, n);
            }
            // for DataSets where the add(..) does not allow for a label
            // dataSet.add(n, n);
            // dataSet.addDataLabel(n, "DataLabel#" + n);

            // n=0..2 -> default style

            if (n == 3) {
                dataSet.addDataStyle(n, style.reset().setStroke("red").build());
            }

            // n == 4 has no label

            if (n == 5) {
                dataSet.addDataStyle(n, style.reset()
                        .setStroke("blue")
                        .setFill("blue")
                        .setStrokeDashPattern(3, 5, 8, 5)
                        .build());
            }

            if (n == 6) {
                dataSet.addDataStyle(n, style.reset()
                        .setStroke("0xEE00EE")
                        .setFill("0xEE00EE")
                        .setStrokeDashPattern(5, 8, 5, 16)
                        .build());
            }

            if (n == 7) {
                dataSet.addDataStyle(n, style.reset()
                        .setStrokeWidth(3)
                        .setFont("Serif")
                        .setFontSize(20)
                        .setFontItalic(true)
                        .setFontWeight("bold")
                        .build());
            }

            if (n == 8) {
                dataSet.addDataStyle(n, style.reset()
                        .setStrokeWidth(3)
                        .setFont("monospace")
                        .setFontItalic(true)
                        .build());
            }
        }
        chart.getDatasets().add(dataSet);

        return chart;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}