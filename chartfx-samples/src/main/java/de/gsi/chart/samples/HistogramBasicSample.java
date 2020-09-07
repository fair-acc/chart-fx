package de.gsi.chart.samples;

import static de.gsi.dataset.spi.AbstractHistogram.HistogramOuterBounds.BINS_ALIGNED_WITH_BOUNDARY;

import java.text.DateFormatSymbols;
import java.util.*;

import javax.tools.Tool;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.renderer.spi.MetaDataRenderer;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.AbstractHistogram;
import de.gsi.dataset.spi.Histogram;
import de.gsi.dataset.testdata.spi.RandomDataGenerator;
import de.gsi.math.Math;

public class HistogramBasicSample extends Application {
    private Histogram dataSet1 = new Histogram("myHistogram1", 4, 0.0, 4.0, BINS_ALIGNED_WITH_BOUNDARY);

    @Override
    public void start(final Stage primaryStage) {
        final XYChart chart = new XYChart();

        final ErrorDataSetRenderer renderer1 = new ErrorDataSetRenderer();
        renderer1.setPolyLineStyle(LineStyle.HISTOGRAM_FILLED);
        chart.getRenderers().add(renderer1);
        renderer1.getDatasets().addAll(dataSet1);

        chart.getPlugins().add(new EditAxis());
        final Zoomer zoomer = new Zoomer();
        zoomer.setSliderVisible(false);
        chart.getPlugins().add(zoomer);

        final VBox root = new VBox();
        final Scene scene = new Scene(root, 800, 600);
        // Controls to generate new equidistant dataset
        final ToolBar addDataSet = new ToolBar();
        final Slider nBinsSlider = new Slider(1, 25, 4);
        nBinsSlider.setMajorTickUnit(1);
        nBinsSlider.setSnapToTicks(true);
        nBinsSlider.setShowTickLabels(true);
        nBinsSlider.setShowTickMarks(true);
        final Slider minSlider = new Slider(-10, 10, 0);
        minSlider.setShowTickLabels(true);
        minSlider.setShowTickMarks(true);
        final Slider maxSlider = new Slider(-10, 10, 4);
        maxSlider.setShowTickLabels(true);
        maxSlider.setShowTickMarks(true);
        final ComboBox<AbstractHistogram.HistogramOuterBounds> histoBoundsCombo = new ComboBox<>(FXCollections.observableList(List.of(AbstractHistogram.HistogramOuterBounds.values())));
        histoBoundsCombo.setValue(BINS_ALIGNED_WITH_BOUNDARY);
        final Button newHistogramButton = new Button("New Histogram");
        newHistogramButton.setOnAction(e -> {
            dataSet1 = new Histogram("myHistogram1", (int) nBinsSlider.getValue(), minSlider.getValue(), maxSlider.getValue(), histoBoundsCombo.getValue());
            for (int i = 0; i < dataSet1.getDataCount() + 2; i++) {
                dataSet1.addBinContent(i, dataSet1.getDataCount() + 2.0 - Math.abs(i - 0.5 * (dataSet1.getDataCount() + 2)));
            }
            renderer1.getDatasets().setAll(dataSet1);
        });
        addDataSet.getItems().addAll( //
                new Label("nBins: "), nBinsSlider, //
                new Label("min: "), minSlider, //
                new Label("max: "), maxSlider, //
                histoBoundsCombo, //
                newHistogramButton //
        );
        // controls to generate new non-equidistant histogram
        final ToolBar addNeqDataSet = new ToolBar();
        final TextField points = new TextField("0.0, 1.0, 2.0, 3.0, 4.0");
        final Button newNeqHistogramButton = new Button("New non-equidistant Histogram");
        newNeqHistogramButton.setOnAction(e -> {
            dataSet1 = new Histogram("Non equidistant histogram", Arrays.stream(points.getText().split(",")).mapToDouble(v -> Double.parseDouble(v.trim())).toArray());
            for (int i = 0; i < dataSet1.getDataCount() + 2; i++) {
                dataSet1.addBinContent(i, dataSet1.getDataCount() + 2.0 - Math.abs(i - 0.5 * (dataSet1.getDataCount() + 2)));
            }
            renderer1.getDatasets().setAll(dataSet1);
        });
        addNeqDataSet.getItems().addAll( //
                new Label("Bin boundaries: "), points, //
                newNeqHistogramButton, //
                new Label("NOTE: because the ErrorDataSetRenderer cannot obtain the widths of the bins, bin boundaries will be off") //
        );

        // controls to add data points
        final ToolBar addDataBar = new ToolBar();
        final ToggleGroup addDataToggleGroup = new ToggleGroup();
        final RadioButton addDataValueRadio = new RadioButton("Add data to value: ");
        addDataValueRadio.setToggleGroup(addDataToggleGroup);
        addDataValueRadio.setSelected(true);
        final Spinner<Double> valueToAddTo = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0, 0.1));
        valueToAddTo.setEditable(true);
        valueToAddTo.disableProperty().bind(Bindings.not(addDataValueRadio.selectedProperty()));
        final RadioButton addDataBinRadio = new RadioButton("Add data to bin: ");
        addDataBinRadio.setToggleGroup(addDataToggleGroup);
        final Spinner<Integer> binToAddTo = new Spinner<>(0, Integer.MAX_VALUE, 2);
        binToAddTo.setEditable(true);
        binToAddTo.disableProperty().bind(Bindings.not(addDataBinRadio.selectedProperty()));
        final Spinner<Double> valueToAdd = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0, 0.1));
        valueToAdd.setEditable(true);
        final Button addDataBtn = new Button("add");
        addDataBtn.setOnAction(e -> {
            if (addDataValueRadio.isSelected()) {
                dataSet1.addBinContent(dataSet1.findBin(DataSet.DIM_X, valueToAddTo.getValue()), valueToAdd.getValue());
            } else {
                dataSet1.addBinContent(binToAddTo.getValue(), valueToAdd.getValue());
            }
        });
        addDataBar.getItems().addAll( //
                addDataValueRadio, valueToAddTo, //
                addDataBinRadio, binToAddTo, //
                new Label("Weight: "), valueToAdd, //
                addDataBtn //
        );
        // show all controls
        root.getChildren().addAll(addDataSet, addNeqDataSet, addDataBar, chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(evt -> {
            Platform.exit();
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
