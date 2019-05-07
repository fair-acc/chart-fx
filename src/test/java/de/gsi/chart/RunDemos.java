package de.gsi.chart;

import java.nio.file.Path;
import java.nio.file.Paths;

import de.gsi.chart.demo.AxisRangeScalingSample;
import de.gsi.chart.demo.CategoryAxisSample;
import de.gsi.chart.demo.ChartAnatomySample;
import de.gsi.chart.demo.ChartIndicatorSample;
import de.gsi.chart.demo.ContourChartSample;
import de.gsi.chart.demo.DataViewerSample;
import de.gsi.chart.demo.EditDataSetSample;
import de.gsi.chart.demo.ErrorDataSetRendererSample;
import de.gsi.chart.demo.ErrorDataSetRendererStylingSample;
import de.gsi.chart.demo.GridRendererSample;
import de.gsi.chart.demo.HexagonSamples;
import de.gsi.chart.demo.Histogram2DimSample;
import de.gsi.chart.demo.HistogramSample;
import de.gsi.chart.demo.HistoryDataSetRendererSample;
import de.gsi.chart.demo.LabelledMarkerSample;
import de.gsi.chart.demo.LogAxisSample;
import de.gsi.chart.demo.MetaDataRendererSample;
import de.gsi.chart.demo.MountainRangeRendererSample;
import de.gsi.chart.demo.MultipleAxesSample;
import de.gsi.chart.demo.PolarPlotSample;
import de.gsi.chart.demo.RollingBufferSample;
import de.gsi.chart.demo.RollingBufferSortedTreeSample;
import de.gsi.chart.demo.SimpleChartSample;
import de.gsi.chart.demo.TimeAxisRangeSample;
import de.gsi.chart.demo.TimeAxisSample;
import de.gsi.chart.demo.ValueIndicatorSample;
import de.gsi.chart.demo.WriteDataSetToFileSample;
import de.gsi.chart.utils.PeriodicScreenCapture;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

/**
 * @author rstein
 */
public class RunDemos extends Application {

    private static final int DEFAULT_DELAY = 2;
    private static final int DEFAULT_PERIOD = 5;
    final String userHome = System.getProperty("user.home");
    final Path path = Paths.get(userHome + "/ChartSamples");

    Stage stage = new Stage();
    CheckBox makeScreenShot = new CheckBox("make screenshot to home directory");

    @Override
    public void start(final Stage primaryStage) {
        final BorderPane root = new BorderPane();

        final FlowPane buttons = new FlowPane();
        buttons.setAlignment(Pos.CENTER_LEFT);
        root.setCenter(buttons);
        root.setBottom(makeScreenShot);

        buttons.getChildren().add(new MyButton("AxisRangeScalingSample", new AxisRangeScalingSample()));
        buttons.getChildren().add(new MyButton("CategoryAxisSample", new CategoryAxisSample()));
        buttons.getChildren().add(new MyButton("ChartAnatomySample", new ChartAnatomySample()));
        buttons.getChildren().add(new MyButton("ChartIndicatorSample", new ChartIndicatorSample()));
        buttons.getChildren().add(new MyButton("ContourChartSample", new ContourChartSample()));
        buttons.getChildren().add(new MyButton("DataViewerSample", new DataViewerSample()));
        buttons.getChildren().add(new MyButton("EditDataSetSample", new EditDataSetSample()));
        buttons.getChildren().add(new MyButton("ErrorDataSetRendererSample", new ErrorDataSetRendererSample()));
        buttons.getChildren()
                .add(new MyButton("ErrorDataSetRendererStylingSample", new ErrorDataSetRendererStylingSample()));
        buttons.getChildren().add(new MyButton("GridRendererSample", new GridRendererSample()));
        buttons.getChildren().add(new MyButton("HexagonSamples", new HexagonSamples()));
        buttons.getChildren().add(new MyButton("Histogram2DimSample", new Histogram2DimSample()));
        buttons.getChildren().add(new MyButton("HistogramSample", new HistogramSample()));
        buttons.getChildren().add(new MyButton("HistoryDataSetRendererSample", new HistoryDataSetRendererSample()));
        buttons.getChildren().add(new MyButton("LabelledMarkerSample", new LabelledMarkerSample()));
        buttons.getChildren().add(new MyButton("LogAxisSample", new LogAxisSample()));
        buttons.getChildren().add(new MyButton("MetaDataRendererSample", new MetaDataRendererSample()));
        buttons.getChildren().add(new MyButton("MountainRangeRendererSample", new MountainRangeRendererSample()));
        buttons.getChildren().add(new MyButton("MultipleAxesSample", new MultipleAxesSample()));
        buttons.getChildren().add(new MyButton("PolarPlotSample", new PolarPlotSample()));
        buttons.getChildren().add(new MyButton("RollingBufferSample", new RollingBufferSample()));
        buttons.getChildren().add(new MyButton("RollingBufferSortedTreeSample", new RollingBufferSortedTreeSample()));
        buttons.getChildren().add(new MyButton("SimpleChartSample", new SimpleChartSample()));
        buttons.getChildren().add(new MyButton("TimeAxisRangeSample", new TimeAxisRangeSample()));
        buttons.getChildren().add(new MyButton("TimeAxisSample", new TimeAxisSample()));
        buttons.getChildren().add(new MyButton("ValueIndicatorSample", new ValueIndicatorSample()));
        buttons.getChildren().add(new MyButton("WriteDataSetToFileSample", new WriteDataSetToFileSample()));

        final Scene scene = new Scene(root);

        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> System.exit(0));
        primaryStage.show();
    }

    protected class MyButton extends Button {

        public MyButton(final String buttonText, final Application run) {
            super(buttonText);
            this.setOnAction(e -> {
                try {
                    run.start(stage);
                    stage.getScene().getRoot().layout();
                    stage.show();

                    if (makeScreenShot.isSelected()) {
                        new Thread() {

                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(2000);
                                    Platform.runLater(() -> {
                                        System.err.println(
                                                "make screen shot to file of " + run.getClass().getSimpleName());
                                        final PeriodicScreenCapture screenCapture = new PeriodicScreenCapture(path,
                                                run.getClass().getSimpleName(), stage.getScene(), DEFAULT_DELAY,
                                                DEFAULT_PERIOD, false);
                                        screenCapture.performScreenCapture();
                                    });
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();

                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            });
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);

    }
}
