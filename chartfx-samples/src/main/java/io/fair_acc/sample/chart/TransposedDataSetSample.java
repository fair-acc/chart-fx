package io.fair_acc.sample.chart;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;
import static io.fair_acc.dataset.DataSet.DIM_Z;

import java.util.Arrays;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.UpdateAxisLabels;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.spi.ContourDataSetRenderer;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DataSetBuilder;
import io.fair_acc.dataset.spi.TransposedDataSet;

/**
 * Simple Example for the use of the TransposedDataSet Shows how 1D and 2D dataSets can be transposed and how to reduce
 * the dimensionality of data.
 *
 * @author akrimm
 */
public class TransposedDataSetSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransposedDataSetSample.class);
    protected static final int FONT_SIZE = 20;
    protected static final String FONT_SYMBOL_TRANSPOSE = "fas-sync:" + FONT_SIZE; // sync symbol
    protected static final String FONT_SYMBOL_CHECK = "fa-check-circle:" + FONT_SIZE; // check symbol
    private static final int N_SAMPLES = 1000; // default number of data points
    private static final int N_TURNS = 20; // default number of data points

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        // init default 2D Chart
        final XYChart chart1 = getDefaultChart();
        final ErrorDataSetRenderer renderer = (ErrorDataSetRenderer) chart1.getRenderers().get(0);
        renderer.setAssumeSortedData(false); // necessary to suppress sorted-DataSet-only optimisations

        // alternate DataSet:
        // final CosineFunction dataSet1 = new CosineFunction("test cosine", N_SAMPLES);
        final DataSet dataSet1 = test8Function(0, 4, -1, 3, (Math.PI / N_SAMPLES) * 2 * N_TURNS, 0.2, N_SAMPLES);
        final TransposedDataSet transposedDataSet1 = TransposedDataSet.transpose(dataSet1, false);
        renderer.getDatasets().add(transposedDataSet1);

        // init default 3D Chart with HeatMapRenderer
        final XYChart chart2 = getDefaultChart();
        final ContourDataSetRenderer contourRenderer = new ContourDataSetRenderer();
        chart2.getRenderers().setAll(contourRenderer);

        final DataSet dataSet2 = createTestData();
        dataSet2.getAxisDescription(DIM_X).set("time", "s");
        dataSet2.getAxisDescription(DIM_Y).set("position", "m");
        final TransposedDataSet transposedDataSet2 = TransposedDataSet.transpose(dataSet2, false);
        contourRenderer.getDatasets().add(transposedDataSet2);

        // init ToolBar items to illustrate the two different methods to flip the DataSet
        final CheckBox cbTransposed = new CheckBox("flip data set");
        final TextField textPermutation = new TextField("0,1,2");
        textPermutation.setPrefWidth(100);
        cbTransposed.setTooltip(new Tooltip("press to transpose DataSet"));
        cbTransposed.setGraphic(new FontIcon(FONT_SYMBOL_TRANSPOSE));
        final Button bApplyPermutation = new Button(null, new FontIcon(FONT_SYMBOL_CHECK));
        bApplyPermutation.setTooltip(new Tooltip("press to apply permutation"));

        // flipping method #1: via 'setTransposed(boolean)' - flips only first two axes
        cbTransposed.setOnAction(evt -> {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.atInfo().addArgument(cbTransposed.isSelected()).log("set transpose state to '{}'");
            }
            transposedDataSet1.setTransposed(cbTransposed.isSelected());
            transposedDataSet2.setTransposed(cbTransposed.isSelected());
            textPermutation.setText(Arrays.stream(transposedDataSet2.getPermutation()).boxed().map(String::valueOf).collect(Collectors.joining(",")));
        });

        // flipping method #2: via 'setPermutation(int[])' - flips arbitrary combination of axes
        final Runnable permutationAction = () -> {
            final int[] parsedInt1 = Arrays.asList(textPermutation.getText().split(","))
                                             .subList(0, transposedDataSet1.getDimension())
                                             .stream()
                                             .map(String::trim)
                                             .mapToInt(Integer::parseInt)
                                             .toArray();
            final int[] parsedInt2 = Arrays.asList(textPermutation.getText().split(","))
                                             .subList(0, transposedDataSet2.getDimension())
                                             .stream()
                                             .map(String::trim)
                                             .mapToInt(Integer::parseInt)
                                             .toArray();

            transposedDataSet1.setPermutation(parsedInt1);
            transposedDataSet2.setPermutation(parsedInt2);
        };

        textPermutation.setOnAction(evt -> permutationAction.run());
        bApplyPermutation.setOnAction(evt -> permutationAction.run());

        // the usual JavaFX Application boiler-plate code
        final ToolBar toolBar = new ToolBar(new Label("method #1 - transpose: "), cbTransposed, new Separator(),
                new Label("method #2 - permutation: "), textPermutation, bApplyPermutation);
        final HBox hBox = new HBox(chart1, chart2);

        VBox.setVgrow(hBox, Priority.ALWAYS);
        return new VBox(toolBar, hBox);
    }

    private static DataSet createTestData() {
        final int nPoints = 300;
        final double f = 0.1;
        final double[] x = new double[nPoints];
        final double[] y = new double[2 * nPoints];
        for (int i = 0; i < x.length; i++) {
            final double val = (i / (double) x.length + 0.5) * 10;
            x[i] = val;
        }
        for (int i = 0; i < y.length; i++) {
            final double val = (i / (double) y.length - 0.5) * 10;
            y[i] = val;
        }
        final double[][] z = new double[y.length][x.length];
        for (int yIndex = 0; yIndex < y.length; yIndex++) {
            for (int xIndex = 0; xIndex < x.length; xIndex++) {
                // if (x[xIndex]>=-3 && x[xIndex]<=-2 && y[yIndex]>=1 &&
                // y[yIndex]<=2) {
                // z[xIndex][yIndex] = 200;
                // } else {
                // z[xIndex][yIndex] = 1000.0;
                // }
                z[yIndex][xIndex] = (x[xIndex]) * (y[yIndex] - 3) * Math.sin(5.0 * Math.PI * f * x[xIndex]) * Math.cos(2.0 * Math.PI * f * y[yIndex]);
            }
        }

        return new DataSetBuilder("3D test data").setValues(DIM_X, x).setValues(DIM_Y, y).setValues(DIM_Z, z).build();
    }

    private static XYChart getDefaultChart() {
        final XYChart chart = new XYChart();
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new UpdateAxisLabels());
        chart.getXAxis().setAutoRanging(true);
        chart.getYAxis().setAutoRanging(true);
        VBox.setVgrow(chart, Priority.ALWAYS);
        HBox.setHgrow(chart, Priority.ALWAYS);

        return chart;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }

    public static DataSet test8Function(final double xMin, final double xMax, final double yMin, final double yMax,
            final double omega, final double pert, final int nSamples) {
        final double[] x = new double[nSamples];
        final double[] y = new double[nSamples];
        final double a = (xMax - xMin) / 2.0;
        final double b = (yMax - yMin) / 2.0;
        for (int i = 0; i < nSamples; i++) {
            x[i] = xMin + (a * (Math.sin(i * omega) + 1))
                 + (pert * Math.sin(i * omega * 0.95) * Math.cos(i * omega * 2));
            y[i] = yMin + (b * (Math.sin(i * omega * 2) + 1))
                 + (pert * Math.sin(i * omega * 0.777) * Math.cos(i * omega));
        }
        final DataSet dataSet = new DataSetBuilder().setName("non-sorted 2D DataSet").setValues(DIM_X, x).setValues(DIM_Y, y).build();
        dataSet.getAxisDescription(DIM_X).set("position", "m");
        dataSet.getAxisDescription(DIM_Y).set("velocity", "m/s");

        return dataSet;
    }
}
