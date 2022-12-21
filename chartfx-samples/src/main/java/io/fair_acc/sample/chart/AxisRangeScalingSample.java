package io.fair_acc.sample.chart;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.axes.spi.MetricPrefix;

/**
 * Small example of how to use the dynamic/static axis scaling functionality N.B. applies only for axes derived from
 * Axis/AbstractNumericAxis
 *
 * @author rstein
 */
public class AxisRangeScalingSample extends ChartSample {
    private static final String CHART_CSS = Chart.class.getResource("chart.css").toExternalForm();
    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 800;

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final VBox root = new VBox();
        root.setAlignment(Pos.CENTER);

        final DefaultNumericAxis xAxis1 = new DefaultNumericAxis("standard axis label w/o unit", 0, 100, 1);
        VBox.setMargin(xAxis1, new Insets(20, 10, 20, 10));
        root.getChildren().add(xAxis1);

        final DefaultNumericAxis xAxis2 = new DefaultNumericAxis("axis label", 0, 100, 1);
        VBox.setMargin(xAxis2, new Insets(20, 10, 20, 10));
        xAxis2.setUnit("m");
        root.getChildren().add(xAxis2);

        final DefaultNumericAxis xAxis3 = new DefaultNumericAxis("current", 0, 100, 1);
        VBox.setMargin(xAxis3, new Insets(20, 10, 20, 10));
        xAxis3.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        xAxis3.setUnit("A");
        xAxis3.getAxisLabel().setFill(Color.RED.darker());
        root.getChildren().add(xAxis3);

        // to force unit scaling to '1000' 'k' suffix
        // N.B. tick unit is being overwritten by scaling
        final DefaultNumericAxis xAxis4 = new DefaultNumericAxis("very large current", 1e3, 100e3, 1e2);
        VBox.setMargin(xAxis4, new Insets(20, 10, 20, 10));
        xAxis4.setUnitScaling(1000);
        // alt:
        xAxis4.setUnitScaling(MetricPrefix.KILO);
        xAxis4.setUnit("A");
        xAxis4.getAxisLabel().setFont(Font.font("Times", 25));
        xAxis4.getAxisLabel().setFill(Color.RED.darker());
        root.getChildren().add(xAxis4);

        // to force unit scaling to '1e-3' '\mu' suffix
        final DefaultNumericAxis xAxis5 = new DefaultNumericAxis("small voltage", 0, 10e-6, 1e-6);
        VBox.setMargin(xAxis5, new Insets(20, 10, 20, 10));
        xAxis5.setUnitScaling(MetricPrefix.MICRO);
        xAxis5.setUnit("V");
        root.getChildren().add(xAxis5);

        // to force unit scaling to '1e-9' 'n' suffix
        final DefaultNumericAxis xAxis6 = new DefaultNumericAxis("tiny voltage", 0, 11e-9, 1e-9);
        VBox.setMargin(xAxis6, new Insets(20, 10, 20, 10));
        xAxis6.setUnitScaling(MetricPrefix.NANO);
        xAxis6.setUnit("V");
        root.getChildren().add(xAxis6);

        // example for scaling with non metric prefix
        final DefaultNumericAxis xAxis7 = new DefaultNumericAxis("non-metric scaling voltage variable", 0, 25e-6, 1e-6);
        VBox.setMargin(xAxis7, new Insets(20, 10, 20, 10));
        xAxis7.setUnitScaling(2.5e-6);
        xAxis7.setUnit("V");
        root.getChildren().add(xAxis7);

        // example for scaling with non metric prefix and w/o unit
        final DefaultNumericAxis xAxis8 = new DefaultNumericAxis("non-metric scaling voltage variable w/o unit", 0,
                25e-6, 1e-6);
        VBox.setMargin(xAxis8, new Insets(20, 10, 20, 10));
        xAxis8.setUnitScaling(2.5e-6);
        // or alternatively:
        // xAxis7.setUnit(null);
        root.getChildren().add(xAxis8);

        // example for dynamic scaling with metric prefix and unit
        final DefaultNumericAxis xAxis9 = new DefaultNumericAxis("dynamic Axis", -1e-6 * 0, 0.001, 1);
        VBox.setMargin(xAxis9, new Insets(20, 10, 20, 10));
        xAxis9.setUnit("V");
        xAxis9.setAutoUnitScaling(true);
        xAxis9.setMinorTickCount(10);
        xAxis9.setAutoRangeRounding(true);
        root.getChildren().add(xAxis9);
        final Label xAxis9Text = new Label();
        root.getChildren().add(xAxis9Text);

        final Timer timer = new Timer("sample-update-timer", true);
        final TimerTask task = new TimerTask() {
            private int counter = -9;
            private boolean directionUpwards = true;

            @Override
            public void run() {
                if (directionUpwards) {
                    counter++;
                } else {
                    counter--;
                }
                Platform.runLater(() -> {
                    final double power = Math.pow(10, counter);
                    xAxis9.maxProperty().set(power);
                    final String text = "actual SI range for dynamic axis: [" + xAxis9.getMin() + " V, "
                                        + xAxis9.getMax() + " V]";
                    xAxis9Text.setText(text);
                });
                if ((counter >= 9) || (counter <= -9)) {
                    directionUpwards = !directionUpwards;
                }
            }
        };
        timer.scheduleAtFixedRate(task, 0, TimeUnit.SECONDS.toMillis(2));

        return root;
    }

    @Override
    public String getControlStylesheetURL() {
        return CHART_CSS;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
