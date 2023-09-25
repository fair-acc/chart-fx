package io.fair_acc.sample.misc;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.ui.ProfilerInfoBox;
import io.fair_acc.chartfx.ui.ProfilerInfoBox.DebugLevel;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DataSetBuilder;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.spi.TransposedDataSet;
import io.fair_acc.dataset.utils.DataSetStyleBuilder;
import io.fair_acc.sample.chart.ChartSample;
import io.fair_acc.sample.misc.plugins.Snow;

/**
 * Happy Christmas and a Happy Coding 2020
 *
 * @author rstein
 */
public class SnowFlakeSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowFlakeSample.class);
    protected static final String FONT_AWESOME = "FontAwesome";
    protected static final int FONT_SIZE = 20;
    protected static final String FONT_SYMBOL_TRANSPOSE = "fas-sync:" + FONT_SIZE; // sync symbol
    protected static final String FONT_SYMBOL_SNOW = "fa-snowflake-o:" + FONT_SIZE; // snow symbol
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 600;

    private final List<DataSet> tree = christmasTree();
    private final DataSet ornaments = treeOrnaments(tree.get(1), 0.5);

    private final BooleanProperty flipProperty = new SimpleBooleanProperty(this, "flip", false);
    private final BooleanProperty snowProperty = new SimpleBooleanProperty(this, "snowProperty", true);
    private final IntegerProperty numberOfFlakesProperty = new SimpleIntegerProperty(this, "numberOfFlakes", 100);
    private final DoubleProperty velocityProperty = new SimpleDoubleProperty(this, "velocity", 0.1);
    private final DoubleProperty meanSizeProperty = new SimpleDoubleProperty(this, "meanSizeProperty", 10.0);
    private final DoubleProperty rmsSizeProperty = new SimpleDoubleProperty(this, "rmsSizeProperty", 5.0);

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final XYChart chart1 = getChristmasChart(false);
        final XYChart chart2 = getChristmasChart(true);

        final CheckBox cbTransposed = new CheckBox("flip tree");
        cbTransposed.setTooltip(new Tooltip("press to flip tree"));
        cbTransposed.setGraphic(new FontIcon(FONT_SYMBOL_TRANSPOSE));
        cbTransposed.selectedProperty().bindBidirectional(flipProperty);

        final CheckBox cbSnow = new CheckBox("snow");
        cbSnow.setTooltip(new Tooltip("press to switch on/off snow"));
        cbSnow.setGraphic(new FontIcon(FONT_SYMBOL_SNOW));
        cbSnow.selectedProperty().bindBidirectional(snowProperty);

        Slider nFlakeSpeed = new Slider(0.0, 2, velocityProperty.get());
        nFlakeSpeed.setMaxWidth(100);
        nFlakeSpeed.setBlockIncrement(0.1);
        nFlakeSpeed.setMajorTickUnit(0.1);
        velocityProperty.bind(nFlakeSpeed.valueProperty());

        Spinner<Integer> nSnowFlakes = new Spinner<>(10, 1000, 200, numberOfFlakesProperty.get());
        nSnowFlakes.setMaxWidth(75);
        numberOfFlakesProperty.bind(nSnowFlakes.valueProperty());

        Spinner<Double> meanFlakeSize = new Spinner<>(0.1, 20.0, meanSizeProperty.get(), 0.1);
        meanFlakeSize.setMaxWidth(75);
        meanSizeProperty.bind(meanFlakeSize.valueProperty());

        Spinner<Double> rmsFlakeSize = new Spinner<>(0.1, 20.0, rmsSizeProperty.get(), 0.1);
        rmsFlakeSize.setMaxWidth(75);
        rmsSizeProperty.bind(rmsFlakeSize.valueProperty());

        final Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final ProfilerInfoBox profilerInfoBox = new ProfilerInfoBox();
        profilerInfoBox.setDebugLevel(DebugLevel.NONE);

        final ToolBar toolBar = new ToolBar(cbTransposed, cbSnow, new Label("speed:"), nFlakeSpeed, //
                new Label("n-flakes:"), nSnowFlakes, new Label("mean-size:"), meanFlakeSize, //
                new Label("rms-size:"), rmsFlakeSize, spacer, profilerInfoBox);
        final HBox hBox = new HBox(chart1, chart2);

        VBox.setVgrow(hBox, Priority.ALWAYS);
        return new VBox(toolBar, hBox);
    }

    private XYChart getChristmasChart(final boolean inverted) {
        DefaultNumericAxis xAxis = new DefaultNumericAxis("X", "mas");
        xAxis.setAutoRanging(false);
        xAxis.set(-11.0, +11.0);
        xAxis.setSide(Side.CENTER_HOR);
        xAxis.getAxisLabel().setTextAlignment(TextAlignment.RIGHT);
        DefaultNumericAxis yAxis = new DefaultNumericAxis("Y", "mas");
        yAxis.setAutoRanging(false);
        yAxis.set(-11.0, +11.0);
        yAxis.getAxisLabel().setTextAlignment(TextAlignment.RIGHT);
        yAxis.setSide(Side.CENTER_VER);

        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new EditAxis());
        Snow snowPlugin = new Snow();
        snowPlugin.snowProperty().bind(snowProperty);
        snowPlugin.velocityProperty().bind(velocityProperty);
        snowPlugin.numberOfFlakesProperty().bind(numberOfFlakesProperty);
        snowPlugin.meanSizeProperty().bind(meanSizeProperty);
        snowPlugin.rmsSizeProperty().bind(rmsSizeProperty);

        chart.getPlugins().add(snowPlugin);
        VBox.setVgrow(chart, Priority.ALWAYS);
        HBox.setHgrow(chart, Priority.ALWAYS);
        chart.getGridRenderer().setDrawOnTop(false);
        chart.setLegendVisible(false);

        chart.setTitle((inverted ? "Y" : "X") + "-mas tree");
        flipProperty.addListener((ch, o, n) -> chart.setTitle((inverted ^ n ? "Y" : "X") + "-mas tree"));

        final ErrorDataSetRenderer renderer1 = (ErrorDataSetRenderer) chart.getRenderers().get(0);
        renderer1.setAssumeSortedData(false);
        renderer1.setPolyLineStyle(LineStyle.AREA);
        renderer1.setPointReduction(false);

        ErrorDataSetRenderer renderer2 = new ErrorDataSetRenderer();
        renderer2.setPolyLineStyle(LineStyle.NONE);
        renderer2.setDrawMarker(true);
        chart.getRenderers().add(renderer2);

        final TransposedDataSet transposeStump = TransposedDataSet.transpose(tree.get(0), inverted);
        final TransposedDataSet transposeTree = TransposedDataSet.transpose(tree.get(1), inverted);
        final TransposedDataSet transposeOrnaments = TransposedDataSet.transpose(ornaments, inverted);
        renderer1.getDatasets().addAll(transposeStump, transposeTree);
        renderer2.getDatasets().add(transposeOrnaments);

        flipProperty.addListener((ch, o, n) -> {
            transposeStump.setTransposed(inverted ^ n);
            transposeTree.setTransposed(inverted ^ n);
            transposeOrnaments.setTransposed(inverted ^ n);
        });

        return chart;
    }

    public static List<DataSet> christmasTree() {
        List<DataSet> list = new ArrayList<>();
        final double scale = 25.0 / 10.0;

        double[] xStomp = { 0.0, 2.0, 2.0, 0.0 };
        double[] yStomp = { 2.0, 2.0, -2.0, -2.0 };
        for (int i = 0; i < xStomp.length; i++) {
            xStomp[i] /= scale;
            yStomp[i] /= scale;
        }
        final DataSet stomp = new DataSetBuilder().setName("tree").setValues(DIM_X, xStomp).setValues(DIM_Y, yStomp).build();
        stomp.getAxisDescription(DIM_X).set("X", "Mas");
        stomp.getAxisDescription(DIM_Y).set("Y", "Mas");
        stomp.setStyle(DataSetStyleBuilder.instance().setStroke("#B5651D").setFill("#B5651D").build());

        final double[] xTree = { 2.0, 2.0, 8.0, 8.0, 12.0, 12.0, 16.0, 16.0, 21.0, /* the tip */
            16.0, 16.0, 12.0, 12.0, 8.0, 8.0, 2.0, 2.0 };
        final double[] yTree = { 0.0, 9.0, 4.0, 7.0, 3.0, 5.0, 2.0, 3.0, 0.0, /* the tip */
            -3.0, -2.0, -5.0, -3.0, -7.0, -4.0, -9.0, 0.0 };
        for (int i = 0; i < xTree.length; i++) {
            xTree[i] /= scale;
            yTree[i] /= scale;
        }
        final DataSet tree = new DataSetBuilder().setName("tree").setValues(DIM_X, xTree).setValues(DIM_Y, yTree).build();
        tree.getAxisDescription(DIM_X).set("X", "Mas");
        tree.getAxisDescription(DIM_Y).set("Y", "Max");
        tree.setStyle(DataSetStyleBuilder.instance().setStroke("darkGreen").setFill("green").build());

        list.add(stomp);
        list.add(tree);

        return list;
    }

    public static void main(final String[] args) {
        launch(args);
    }

    public static DataSet treeOrnaments(final DataSet tree, final double spacing) {
        DoubleDataSet dataSet = new DoubleDataSet("ornaments");
        dataSet.setStyle("markerType=circle;");
        tree.recomputeLimits(DIM_X);
        tree.recomputeLimits(DIM_Y);
        final double xMin = tree.getAxisDescriptions().get(DataSet.DIM_X).getMin();
        final double xMax = tree.getAxisDescriptions().get(DataSet.DIM_X).getMax();
        final double yMin = tree.getAxisDescriptions().get(DataSet.DIM_Y).getMin();
        final double yMax = tree.getAxisDescriptions().get(DataSet.DIM_Y).getMax();

        int count = 0;
        int count2 = 0;
        for (double x = xMin; x <= xMax; x += spacing) {
            for (double y = yMin; y <= yMax; y += spacing) {
                final double yTree = Math.abs(tree.getValue(DataSet.DIM_Y, x));
                String markerSize = "markerSize=" + 3 + "; index=" + count + ";";
                if ((Math.abs(y) <= (yTree - 0.5 * spacing)) && Math.random() < 0.7) {
                    final String ballStyle = ((count2) % 2 == 0) ? "markerColor=#FF0000;" : "markerColor=orange;";
                    dataSet.add(x, y);
                    dataSet.addDataStyle(count, markerSize + ballStyle);
                    count++;
                    count2++;
                }
            }
            count2++;
        }

        return dataSet;
    }
}
