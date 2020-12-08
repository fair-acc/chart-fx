package de.gsi.chart.samples;

import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.samples.financial.FinancialAdvancedCandlestickSample;
import de.gsi.chart.samples.financial.FinancialCandlestickSample;
import de.gsi.chart.samples.financial.FinancialHiLowSample;
import de.gsi.chart.samples.financial.FinancialRealtimeCandlestickSample;
import de.gsi.chart.utils.PeriodicScreenCapture;

/**
 * @author rstein
 */
public class RunChartSamples extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunChartSamples.class);
    private static final int DEFAULT_DELAY = 2;
    private static final int DEFAULT_PERIOD = 5;
    private final String userHome = System.getProperty("user.home");
    private final Path path = Paths.get(userHome + "/ChartSamples");

    private final Stage stage = new Stage();
    private final CheckBox makeScreenShot = new CheckBox("make screenshot to home directory");

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
        buttons.getChildren().add(new MyButton("ChartPerformanceGraph", new ChartPerformanceGraph()));
        buttons.getChildren().add(new MyButton("ContourChartSample", new ContourChartSample()));
        buttons.getChildren().add(new MyButton("CustomColourSchemeSample", new CustomColourSchemeSample()));
        buttons.getChildren().add(new MyButton("CustomFragmentedRendererSample", new CustomFragmentedRendererSample()));
        buttons.getChildren().add(new MyButton("DataViewerSample", new DataViewerSample()));
        buttons.getChildren().add(new MyButton("DimReductionDataSetSample", new DimReductionDataSetSample()));
        buttons.getChildren().add(new MyButton("EditDataSetSample", new EditDataSetSample()));
        buttons.getChildren().add(new MyButton("ErrorDataSetRendererSample", new ErrorDataSetRendererSample()));
        buttons.getChildren()
                .add(new MyButton("ErrorDataSetRendererStylingSample", new ErrorDataSetRendererStylingSample()));
        buttons.getChildren().add(new MyButton("FinancialCandlestickSample", new FinancialCandlestickSample()));
        buttons.getChildren().add(new MyButton("FinancialHiLowSample", new FinancialHiLowSample()));
        buttons.getChildren().add(new MyButton("FinancialAdvancedCandlestickSample", new FinancialAdvancedCandlestickSample()));
        buttons.getChildren().add(new MyButton("FinancialRealtimeCandlestickSample", new FinancialRealtimeCandlestickSample()));
        buttons.getChildren().add(new MyButton("FxmlSample", new FxmlSample()));
        buttons.getChildren().add(new MyButton("GridRendererSample", new GridRendererSample()));
        buttons.getChildren().add(new MyButton("HexagonSamples", new HexagonSamples()));
        buttons.getChildren().add(new MyButton("Histogram2DimSample", new Histogram2DimSample()));
        buttons.getChildren().add(new MyButton("HistogramBasicSample", new HistogramBasicSample()));
        buttons.getChildren().add(new MyButton("HistogramSample", new HistogramSample()));
        buttons.getChildren().add(new MyButton("HistoryDataSetRendererSample", new HistoryDataSetRendererSample()));
        buttons.getChildren().add(new MyButton("LabelledMarkerSample", new LabelledMarkerSample()));
        buttons.getChildren().add(new MyButton("LogAxisSample", new LogAxisSample()));
        buttons.getChildren().add(new MyButton("MetaDataRendererSample", new MetaDataRendererSample()));
        buttons.getChildren().add(new MyButton("MountainRangeRendererSample", new MountainRangeRendererSample()));
        buttons.getChildren().add(new MyButton("MultipleAxesSample", new MultipleAxesSample()));
        buttons.getChildren().add(new MyButton("NotANumberSample", new NotANumberSample()));
        buttons.getChildren().add(new MyButton("OscilloscopeAxisSample", new OscilloscopeAxisSample()));
        buttons.getChildren().add(new MyButton("PolarPlotSample", new PolarPlotSample()));
        buttons.getChildren().add(new MyButton("RollingBufferSample", new RollingBufferSample()));
        buttons.getChildren().add(new MyButton("RollingBufferSortedTreeSample", new RollingBufferSortedTreeSample()));
        buttons.getChildren().add(new MyButton("ScatterAndBubbleRendererSample", new ScatterAndBubbleRendererSample()));
        buttons.getChildren().add(new MyButton("SimpleChartSample", new SimpleChartSample()));
        buttons.getChildren().add(new MyButton("TimeAxisRangeSample", new TimeAxisRangeSample()));
        buttons.getChildren().add(new MyButton("TimeAxisSample", new TimeAxisSample()));
        buttons.getChildren().add(new MyButton("TransposedDataSetSample", new TransposedDataSetSample()));
        buttons.getChildren().add(new MyButton("ValueIndicatorSample", new ValueIndicatorSample()));
        buttons.getChildren().add(new MyButton("WaterfallPerformanceSample", new WaterfallPerformanceSample()));
        buttons.getChildren().add(new MyButton("WriteDataSetToFileSample", new WriteDataSetToFileSample()));
        buttons.getChildren().add(new MyButton("ZoomerSample", new ZoomerSample()));

        final Scene scene = new Scene(root);

        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }

    protected class MyButton extends Button {
        public MyButton(final String buttonText, final Application run) {
            super(buttonText);
            setOnAction(e -> {
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
                                        LOGGER.atInfo()
                                                .log("make screen shot to file of " + run.getClass().getSimpleName());
                                        final PeriodicScreenCapture screenCapture = new PeriodicScreenCapture(path,
                                                run.getClass().getSimpleName(), stage.getScene(), DEFAULT_DELAY,
                                                DEFAULT_PERIOD, false);
                                        screenCapture.performScreenCapture();
                                    });
                                } catch (final InterruptedException e) {
                                    if (LOGGER.isErrorEnabled()) {
                                        LOGGER.atError().setCause(e).log("InterruptedException");
                                    }
                                }
                            }
                        }.start();
                    }
                } catch (final Exception e1) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.atError().setCause(e1).log("InterruptedException");
                    }
                }
            });
        }
    }
}
