package io.fair_acc.sample.math;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import io.fair_acc.dataset.events.ChartBits;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.TableViewer;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSet2D;
import io.fair_acc.dataset.event.UpdatedDataEvent;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.spi.utils.DoublePoint;
import io.fair_acc.math.ArrayMath;
import io.fair_acc.math.DataSetMath;
import io.fair_acc.math.MathDataSet;
import io.fair_acc.math.spectra.TSpectrum;
import io.fair_acc.math.spectra.TSpectrum.Direction;
import io.fair_acc.math.spectra.TSpectrum.FilterOrder;
import io.fair_acc.math.spectra.TSpectrum.SmoothWindow;

/**
 * @author rstein
 */
public class TSpectrumSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(TSpectrumSample.class);
    private static final String SOURCE1 = "./BBQSpectra.dat";
    private static final String SOURCE2 = "./rawDataCPS2.dat";
    private static final String SOURCE3 = "./rawDataLHCInj.dat";
    private final DoubleDataSet demoDataSet = readDemoData(SOURCE1);
    private ErrorDataSetRenderer backgroundRenderer;
    private ErrorDataSetRenderer peakRenderer;

    private final Spinner<Integer> nIterations = new Spinner<>(1, 100, 10);
    private final ComboBox<Direction> cbxDirection = new ComboBox<>(FXCollections.observableArrayList(Direction.values()));
    private final ComboBox<FilterOrder> cbxFilterOrder = new ComboBox<>(FXCollections.observableArrayList(FilterOrder.values()));
    private final ComboBox<SmoothWindow> cbxSmoothWindow = new ComboBox<>(FXCollections.observableArrayList(SmoothWindow.values()));

    private final CheckBox cbCompton = new CheckBox();
    private final CheckBox cbMarkov = new CheckBox();
    private final CheckBox cbBackground = new CheckBox();

    private final Spinner<Integer> spAverageMarkov = new Spinner<>(1, 100, 7);
    private final Spinner<Integer> spAverageSearch = new Spinner<>(1, 100, 7);

    private final Spinner<Double> spSigma = new Spinner<>(0.1, 100.0, 3, 0.1);
    private final Spinner<Double> spThreshold = new Spinner<>(0.001, 99.0, 10.0, 0.1);

    private VBox getBottomControls() {
        VBox root = new VBox();

        ToolBar toolBarBackground = new ToolBar();
        nIterations.valueProperty().addListener((ch, o, n) -> triggerDataSetUpdate());
        nIterations.setPrefWidth(70);
        cbxDirection.getSelectionModel().select(Direction.DECREASING);
        cbxDirection.setOnAction(evt -> triggerDataSetUpdate());
        cbxFilterOrder.getSelectionModel().select(FilterOrder.ORDER_6);
        cbxFilterOrder.setOnAction(evt -> triggerDataSetUpdate());
        cbxSmoothWindow.getSelectionModel().select(SmoothWindow.SMOOTHING_WIDTH15);
        cbxSmoothWindow.setOnAction(evt -> triggerDataSetUpdate());
        cbCompton.setOnAction(evt -> triggerDataSetUpdate());
        toolBarBackground.getItems().addAll(new Label("background:"), new Label("nIterations: "), nIterations, cbxDirection, cbxFilterOrder, cbxSmoothWindow, new Label("Compton:"),
                cbCompton);

        ToolBar toolBarMarkov = new ToolBar();
        spAverageMarkov.valueProperty().addListener((ch, o, n) -> triggerDataSetUpdate());
        spAverageMarkov.setPrefWidth(70);
        toolBarMarkov.getItems().addAll(new Label("Markov background:"), new Label("avg-width [bins: "), spAverageMarkov);

        ToolBar toolBarSearch = new ToolBar();
        spSigma.valueProperty().addListener((ch, o, n) -> triggerDataSetUpdate());
        spSigma.setPrefWidth(70);
        spSigma.setEditable(true);
        spThreshold.valueProperty().addListener((ch, o, n) -> triggerDataSetUpdate());
        spThreshold.setPrefWidth(100);
        spThreshold.setEditable(true);
        cbMarkov.setOnAction(evt -> triggerDataSetUpdate());
        cbBackground.setOnAction(evt -> triggerDataSetUpdate());
        spAverageSearch.valueProperty().addListener((ch, o, n) -> triggerDataSetUpdate());
        spAverageSearch.setPrefWidth(70);
        toolBarSearch.getItems().addAll(new Label("peak search: "), new Label("sigma [bins]: "), spSigma, new Label("threshold [%]: "), spThreshold, new Label("Markov?:"),
                cbMarkov, new Label("subtract bg: "), cbBackground, new Label("avg [bins]:"), spAverageSearch);

        root.getChildren().addAll(toolBarBackground, toolBarMarkov, toolBarSearch);
        return root;
    }

    private Chart getChart() {
        final DefaultNumericAxis xAxis = new DefaultNumericAxis("frequency", "frev");
        final DefaultNumericAxis yAxis = new DefaultNumericAxis("magnitude", "dB");
        yAxis.setForceZeroInRange(true);
        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new TableViewer());
        chart.getRenderers().get(0).getDatasets().add(demoDataSet);
        backgroundRenderer = new ErrorDataSetRenderer();
        peakRenderer = new ErrorDataSetRenderer();
        peakRenderer.setPolyLineStyle(LineStyle.NONE);
        peakRenderer.setMarkerSize(5);
        peakRenderer.setAssumeSortedData(false);
        chart.getRenderers().addAll(backgroundRenderer, peakRenderer);

        return chart;
    }

    private ToolBar getTopToolBar() {
        ToolBar toolBar = new ToolBar();
        ToggleGroup radioGroup = new ToggleGroup();

        RadioButton bbqButton1 = new RadioButton("LHC BBQ spectrum");
        bbqButton1.setSelected(true);
        bbqButton1.setToggleGroup(radioGroup);
        toolBar.getItems().add(bbqButton1);
        bbqButton1.selectedProperty().addListener((ch, o, n) -> {
            if (Boolean.FALSE.equals(n)) {
                return;
            }
            demoDataSet.set(readDemoData(SOURCE1));
        });

        RadioButton bbqButton2 = new RadioButton("CPS BBQ spectrum");
        bbqButton2.setToggleGroup(radioGroup);
        toolBar.getItems().add(bbqButton2);
        bbqButton2.selectedProperty().addListener((ch, o, n) -> {
            if (Boolean.FALSE.equals(n)) {
                return;
            }

            demoDataSet.set(new MathDataSet(null, DataSetMath::magnitudeSpectrumDecibel, readDemoData(SOURCE2)));
        });

        RadioButton bbqButton3 = new RadioButton("LHC injection BBQ spectrum");
        bbqButton3.setToggleGroup(radioGroup);
        toolBar.getItems().add(bbqButton3);
        bbqButton3.selectedProperty().addListener((ch, o, n) -> {
            if (Boolean.FALSE.equals(n)) {
                return;
            }
            demoDataSet.set(new MathDataSet(null, DataSetMath::magnitudeSpectrumDecibel, readDemoData(SOURCE3)));
        });

        RadioButton synthButton = new RadioButton("synthetic spectrum");
        Slider slider = new Slider(10, 8192, 512);
        slider.setBlockIncrement(10);
        slider.valueProperty().addListener((ch, o, n) -> {
            if (synthButton.isSelected()) {
                demoDataSet.set(generateDemoSineWaveData(n.intValue()));
            }
        });

        synthButton.setToggleGroup(radioGroup);
        toolBar.getItems().add(synthButton);
        synthButton.selectedProperty().addListener((ch, o, n) -> {
            if (Boolean.FALSE.equals(n)) {
                return;
            }
            demoDataSet.set(generateDemoSineWaveData((int) slider.getValue()));
        });
        toolBar.getItems().add(slider);

        return toolBar;
    }

    @Override
    public void start(final Stage primaryStage) {
        Chart chart = getChart();
        final BorderPane root = new BorderPane(chart);
        root.setTop(getTopToolBar());
        root.setBottom(getBottomControls());

        MathDataSet dsBackground = new MathDataSet("background", (final double[] input, final double[] output, final int length) -> {
            LOGGER.atInfo().log("trigger background update");
            final int nIter = nIterations.getValue();
            final Direction direction = cbxDirection.getSelectionModel().getSelectedItem();
            final FilterOrder filterOrder = cbxFilterOrder.getSelectionModel().getSelectedItem();
            final SmoothWindow smoothing = cbxSmoothWindow.getSelectionModel().getSelectedItem();
            boolean compton = cbCompton.isSelected();
            TSpectrum.background(input, output, length, nIter, direction, filterOrder, smoothing, compton);
        }, demoDataSet);
        backgroundRenderer.getDatasets().addAll(dsBackground);

        MathDataSet dsMarkov = new MathDataSet("bgMarkov", (final double[] input, final double[] output, final int length) -> {
            final int nAverage = spAverageMarkov.getValue();
            ArrayMath.decibelInPlace(TSpectrum.smoothMarkov(ArrayMath.inverseDecibel(input), output, length, nAverage));
        }, demoDataSet);
        backgroundRenderer.getDatasets().addAll(dsMarkov);

        DoubleDataSet dsBgSearch = new DoubleDataSet("peak search background");
        MathDataSet foundPeaks = new MathDataSet("peak", (DataSet dataSet) -> {
            if (!(dataSet instanceof DataSet2D)) {
                return new DoubleDataSet("no peaks(processing error)");
            }
            final double[] freq = ((DataSet2D) dataSet).getXValues();
            final double[] rawData = ((DataSet2D) dataSet).getYValues();
            final double[] destVector = new double[dataSet.getDataCount()];

            final double sigma = spSigma.getValue();
            final double threshold = spThreshold.getValue();
            final int nIter = nIterations.getValue();
            final int nAverage = spAverageSearch.getValue();
            final boolean markov = cbMarkov.isSelected();
            final boolean backgroundRemove = cbBackground.isSelected();

            final List<DoublePoint> peaks = TSpectrum.search(freq, ArrayMath.inverseDecibel(rawData), destVector, dataSet.getDataCount(), 100, sigma, threshold, //
                    backgroundRemove, nIter, markov, nAverage);

            dsBgSearch.set(freq, ArrayMath.decibel(destVector), dataSet.getDataCount(), true);

            DoubleDataSet retVal = new DoubleDataSet("peaks", 10);
            LOGGER.atInfo().addArgument(peaks.size()).addArgument(dataSet.getDataCount()).log("found {} peaks in spectrum of length {}");
            for (DoublePoint point : peaks) {
                retVal.add(point.getX(), 20 * Math.log10(point.getY()));
                LOGGER.atInfo().addArgument(point.getX()).addArgument(point.getY()).log("found peak at ({},{})");
            }

            return retVal;
        }, demoDataSet);
        peakRenderer.getDatasets().addAll(foundPeaks);
        backgroundRenderer.getDatasets().addAll(dsBgSearch);

        // MathDataSet normalisedBg = new MathDataSet("norm. Bg.", dataSets -> {
        //   final DataSet dsLin1 = DataSetMath.inversedbFunction(dataSets.get(0));
        //   final DataSet dsLin2 = DataSetMath.inversedbFunction(dataSets.get(1));
        //   return DataSetMath.dbFunction(DataSetMath.subtractFunction(dsLin1, dsLin2));
        // }, demoDataSet, dsBackground);
        // normalisedBg.setStyle("strokeColor=#CECECE");
        // backgroundRenderer.getDatasets().addAll(normalisedBg);

        final Scene scene = new Scene(root, 1600, 600);
        primaryStage.setTitle(getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
    }

    private void triggerDataSetUpdate() {
        demoDataSet.fireInvalidated(ChartBits.DataSetData);
    }

    protected static DoubleDataSet generateDemoSineWaveData(final int nData) {
        DoubleDataSet function = new DoubleDataSet("composite sine", nData);
        for (int i = 0; i < nData; i++) {
            final double t = i;
            double y = 0;
            final double centreFrequency = 0.25;
            final double diffFrequency = 0.05;
            for (int j = 0; j < 8; j++) {
                final double a = 0.1 * Math.pow(10, -j);
                final double diff = j == 0 ? 0 : (j % 2 - 0.5) * j * diffFrequency;
                y += a * Math.sin(2.0 * Math.PI * (centreFrequency + diff) * t);
            }

            function.add(t, y);
        }

        return new DoubleDataSet(DataSetMath.magnitudeSpectrumDecibel(function));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }

    protected static DoubleDataSet readDemoData(final String fileName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(TSpectrumSample.class.getResourceAsStream(fileName))))) {
            String line = reader.readLine();
            final int nDim = line == null ? 0 : Integer.parseInt(line);

            DoubleDataSet spectrum = new DoubleDataSet("BBQ spectrum", nDim);
            for (int i = 0; i < nDim; i++) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                final String[] x = line.split("\t");

                spectrum.add(Double.parseDouble(x[0]), Double.parseDouble(x[1]));
            }

            return spectrum;
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).addArgument(fileName).log("read error for file '{}'");
            }
        }
        return new DoubleDataSet("empty dataset <ERROR>");
    }
}
