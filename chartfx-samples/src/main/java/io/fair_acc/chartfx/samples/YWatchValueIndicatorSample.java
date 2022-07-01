package io.fair_acc.chartfx.samples;

import java.util.Objects;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.YWatchValueIndicator;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.testdata.spi.CosineFunction;
import io.fair_acc.dataset.testdata.spi.SineFunction;

/**
 * @author akrimm
 */
public class YWatchValueIndicatorSample extends Application {
    private static final int N_SAMPLES = 1000;

    @Override
    public void start(final Stage primaryStage) {
        final StackPane root = new StackPane();

        DefaultNumericAxis xAxis = new DefaultNumericAxis();
        DefaultNumericAxis yAxis = new DefaultNumericAxis();
        DefaultNumericAxis yAxis2 = new DefaultNumericAxis();
        yAxis2.setSide(Side.RIGHT);
        final XYChart chart = new XYChart(xAxis, yAxis);
        root.getChildren().add(chart);
        chart.getAxes().add(yAxis2);

        chart.getDatasets().addAll(new SineFunction("sine", N_SAMPLES), new CosineFunction("cosine", N_SAMPLES));

        final YWatchValueIndicator indicator1 = new YWatchValueIndicator(yAxis, 0.7);
        indicator1.setId("valA");
        final YWatchValueIndicator indicator2 = new YWatchValueIndicator(yAxis, 0.63);
        indicator2.setId("valB");
        final YWatchValueIndicator indicator3 = new YWatchValueIndicator(yAxis2, 0.18);
        indicator3.setId("valA");
        indicator3.setPreventOcclusion(true);
        final YWatchValueIndicator indicator4 = new YWatchValueIndicator(yAxis2, 0.2);
        indicator4.setId("valB");
        final YWatchValueIndicator indicator5 = new YWatchValueIndicator(yAxis2, 0.21);
        chart.getPlugins().addAll(indicator1, indicator2, indicator3, indicator4, indicator5);

        // animate indicators
        final Timeline timeline = new Timeline(new KeyFrame(Duration.millis(20), new EventHandler<ActionEvent>() {
            double time = 0;
            @Override
            public void handle(ActionEvent t) {
                time += 0.03;
                indicator2.setValue(0.65 + 0.14 * Math.cos(time));
                indicator4.setValue(0.21 + 0.18 * Math.cos(0.8 * time));
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);

        final Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(Objects.requireNonNull(YWatchValueIndicatorSample.class.getResource("YWatchValueIndicatorSample.css"), "stylesheet not found").toExternalForm());
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
        primaryStage.show();
        timeline.play();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
