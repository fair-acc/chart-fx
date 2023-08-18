package io.fair_acc.sample.chart;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.ui.css.ColorPalette;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example illustrating the various colour scheme options
 *
 * @author rstein
 */
public class CustomColourSchemeSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomColourSchemeSample.class);
    private static final int N_SAMPLES = 2000; // default number of data points
    private static final int N_DATA_SETS_MAX = 10;

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final XYChart chart = new XYChart(new DefaultNumericAxis("x-Axis"), new DefaultNumericAxis("y-Axis"));
        chart.getPlugins().add(new Zoomer()); // standard plugins, useful for most cases
        chart.getPlugins().add(new EditAxis()); // for editing axes
        VBox.setVgrow(chart, Priority.ALWAYS);
        ErrorDataSetRenderer renderer = (ErrorDataSetRenderer) chart.getRenderers().get(0);

        for (int i = 0; i < N_DATA_SETS_MAX; i++) {
            DoubleErrorDataSet dataSet = new DoubleErrorDataSet("Set#" + i); // NOPMD
            for (int n = 0; n < N_SAMPLES; n++) {
                dataSet.add(n, 0.5 * i + Math.cos(Math.toRadians(1.0 * n)), 0.15, 0.15);
            }
            chart.getDatasets().add(dataSet);
        }

        ComboBox<ColorPalette> palettePseudoClassCB = new ComboBox<>();
        palettePseudoClassCB.setItems(FXCollections.observableArrayList(ColorPalette.values()));
        palettePseudoClassCB.getSelectionModel().select(chart.getColorPalette());
        chart.colorPaletteProperty().bind(palettePseudoClassCB.getSelectionModel().selectedItemProperty());
        palettePseudoClassCB.getSelectionModel().selectedItemProperty().addListener((ch, o, n) -> {
            LOGGER.atInfo().log("updated color palette style to " + n.name());
        });

        ComboBox<ErrorStyle> errorStyleCB = new ComboBox<>();
        errorStyleCB.getItems().setAll(ErrorStyle.values());
        errorStyleCB.getSelectionModel().select(renderer.getErrorType());
        renderer.errorStyleProperty().bind(errorStyleCB.getSelectionModel().selectedItemProperty());
        errorStyleCB.getSelectionModel().selectedItemProperty().addListener((ch, o, n) -> {
            LOGGER.atInfo().log("updated error style to " + n.name());
        });

        ToolBar toolBar = new ToolBar(
                new Label("CSS Palette: "), palettePseudoClassCB,
                new Label("error style: "), errorStyleCB);
        return new VBox(toolBar, chart);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
