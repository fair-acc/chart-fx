package io.fair_acc.chartfx.samples;

import java.util.Objects;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.CrosshairIndicator;
import io.fair_acc.chartfx.plugins.DataPointTooltip;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.dataset.testdata.spi.CosineFunction;
import io.fair_acc.dataset.testdata.spi.GaussFunction;
import io.fair_acc.dataset.testdata.spi.RandomWalkFunction;

/**
 * Simple example of how to use css to change the appearance of the chart.
 * 
 * @author akrimm
 */
public class CssStylingSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(CssStylingSample.class);
    private static final int N_SAMPLES = 100; // default number of data points
    public static final ObservableList<String> CSS_LIST = FXCollections.observableArrayList("none", "CustomCss1.css", "CustomCss2.css");

    @Override
    public void start(final Stage primaryStage) {
        final ComboBox<String> globalCssBox = new ComboBox<String>(CSS_LIST);
        globalCssBox.getSelectionModel().select(0);

        final DefaultNumericAxis yAxis = new DefaultNumericAxis();
        yAxis.setAutoRanging(true); // default: true
        yAxis.setAutoRangePadding(0.5); // here: 50% padding on top and bottom of axis

        final XYChart chart = new XYChart(new DefaultNumericAxis(), yAxis);
        chart.getPlugins().addAll(new Zoomer(), new CrosshairIndicator(), new EditAxis()); // standard plugin, useful for most cases

        chart.getDatasets().addAll(new GaussFunction("Gauss", N_SAMPLES), new CosineFunction("Cosine", N_SAMPLES)); // for two data sets
        final ComboBox<String> cssBox = new ComboBox<String>(CSS_LIST);
        cssBox.getSelectionModel().select(0);
        VBox.setVgrow(chart, Priority.ALWAYS);

        final DefaultNumericAxis yAxisRight = new DefaultNumericAxis();
        yAxisRight.setAutoRanging(true); // default: true
        yAxisRight.setAutoRangePadding(0.5); // here: 50% padding on top and bottom of axis

        final XYChart chartRight = new XYChart(new DefaultNumericAxis(), yAxisRight);
        chartRight.getPlugins().addAll(new Zoomer(), new DataPointTooltip(), new EditAxis()); // standard plugin, useful for most cases

        chartRight.getDatasets().addAll(new RandomWalkFunction("RandomWalk", N_SAMPLES));
        final ComboBox<String> cssBoxRight = new ComboBox<String>(CSS_LIST);
        cssBoxRight.getSelectionModel().select(0);
        VBox.setVgrow(chartRight, Priority.ALWAYS);

        final HBox hBox = new HBox( //
                new VBox(new HBox(new Label("Stylesheet for left HBox: "), cssBox), chart), //
                new VBox(new HBox(new Label("Stylesheet for right HBox: "), cssBoxRight), chartRight) //
        );
        VBox.setVgrow(hBox, Priority.ALWAYS);
        hBox.getChildren().forEach(child -> HBox.setHgrow(child, Priority.ALWAYS));
        final Scene scene = new Scene(new VBox(new HBox(new Label("Stylesheet for Scene: "), globalCssBox), hBox), 800, 600);
        primaryStage.setTitle(getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(evt -> Platform.exit());

        globalCssBox.valueProperty().addListener((prop, oldVal, newVal) -> {
            if ("none".equals(newVal)) {
                scene.getStylesheets().clear();
            } else {
                scene.getStylesheets().setAll(Objects.requireNonNull(CssStylingSample.class.getResource(newVal), "could not load css file: " + newVal).toExternalForm());
            }
        });
        cssBox.valueProperty().addListener((prop, oldVal, newVal) -> {
            if ("none".equals(newVal)) {
                chart.getStylesheets().clear();
            } else {
                chart.getStylesheets().setAll(Objects.requireNonNull(CssStylingSample.class.getResource(newVal), "could not load css file: " + newVal).toExternalForm());
            }
        });
        cssBoxRight.valueProperty().addListener((prop, oldVal, newVal) -> {
            if ("none".equals(newVal)) {
                chartRight.getStylesheets().clear();
            } else {
                chartRight.getStylesheets().setAll(Objects.requireNonNull(CssStylingSample.class.getResource(newVal), "could not load css file: " + newVal).toExternalForm());
            }
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
