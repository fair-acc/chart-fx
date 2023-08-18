package io.fair_acc.sample.math;

import static io.fair_acc.dataset.DataSet.DIM_Y;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import io.fair_acc.dataset.utils.DataSetStyleBuilder;
import io.fair_acc.sample.chart.ChartSample;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.renderer.spi.LabelledMarkerRenderer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.testdata.spi.GaussFunction;
import io.fair_acc.math.DataSetMath;
import io.fair_acc.sample.math.utils.DemoChart;

/**
 * Reads schottky measurement data and provides width estimates based on (a-) symmetric integration intervals
 *
 * @author rstein
 */
public class PeakWidthSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeakWidthSample.class);
    private static final String FILE_NAME = "./LongSchottkySIS18.dat";
    private static final String MEAS_STROKE_COLOUR = DataSetStyleBuilder.instance().setStroke("lightGray").build();
    private static final String FONT_SIZE = DataSetStyleBuilder.instance().setFontSize(20).build();
    private static final char SIGMA_CHAR = (char) 0x03C3;
    private static final int N_SAMPLES = 3000;
    private static final double A1 = 1.05;
    private static final double SIGMA1 = 0.65;
    private static final double A2 = 1.8;
    private static final double SIGMA2 = 0.18;
    private static final double MAX_INT_WIDTH = 3.0;

    private final DemoChart chart1 = new DemoChart();
    private final DemoChart chart2 = new DemoChart();

    @Override
    public Node getChartPanel(Stage stage) {
        final List<DataSet> rawData = readDemoData();
        final List<DataSet> linearData = rawData.stream().map(DataSetMath::inversedbFunction).collect(Collectors.toList());
        linearData.forEach(ds -> ds.setStyle(MEAS_STROKE_COLOUR));

        var average = new DoubleDataSet("average", N_SAMPLES);
        final var gauss1 = new DoubleDataSet("gauss1", N_SAMPLES);
        final var gauss2 = new DoubleDataSet("gauss2", N_SAMPLES);
        gauss1.setStyle("strokeColor=darkred");
        gauss2.setStyle("strokeColor=darkgreen");
        for (var i = 0; i < N_SAMPLES; i++) {
            var x = -3.0 + (double) i / N_SAMPLES * 6.0;
            var y = 0.0;
            for (DataSet ds : linearData) {
                y += ds.getValue(DIM_Y, x) / linearData.size();
            }
            average.add(x, y);
            gauss1.add(x, A1 * GaussFunction.gauss(x, 0, SIGMA1) * SIGMA1);
            gauss2.add(x, A2 * GaussFunction.gauss(x, 0, SIGMA2) * SIGMA2);
        }

        final var dataOffset = average.getAxisDescription(DIM_Y).getMin();
        final var avgOffsetCompensated = DataSetMath.subtractFunction(average, dataOffset);
        avgOffsetCompensated.setStyle("strokeColor=black");
        final List<DataSet> linDataOffsetCompensated = linearData.stream().map(ds -> DataSetMath.subtractFunction(ds, dataOffset).setStyle(MEAS_STROKE_COLOUR)).collect(Collectors.toList());

        chart1.getDatasets().addAll(avgOffsetCompensated, gauss1, gauss2);
        chart1.getDatasets().addAll(linDataOffsetCompensated);

        chart1.getXAxis().setName("dp/p");
        chart1.getXAxis().setUnit("1e-3");
        chart1.getYAxis().setName("amplitude");
        chart1.getYAxis().setUnit("a.u.");
        chart1.setLegendVisible(false);

        chart2.getXAxis().setName("dp/p [fs]");
        chart2.getXAxis().setUnit("1e-3");
        chart2.getYAxis().setName("norm. integral");
        chart2.getYAxis().setUnit(null);
        chart2.setLegendVisible(false);

        final BiFunction<DataSet, Double, DataSet> integralWidth = (ds, offset) -> DataSetMath.integrateFromCentre(DataSetMath.subtractFunction(ds, offset), Double.NaN, MAX_INT_WIDTH, true);

        final List<DataSet> intData = linearData.stream().map(ds -> integralWidth.apply(ds, dataOffset)).map(ds -> ds.setStyle(MEAS_STROKE_COLOUR)).collect(Collectors.toList());
        final var intAverage = DataSetMath.integrateFromCentre(avgOffsetCompensated, Double.NaN, MAX_INT_WIDTH, true);
        final var intGauss1 = DataSetMath.integrateFromCentre(gauss1, Double.NaN, MAX_INT_WIDTH, true);
        final var intGauss2 = DataSetMath.integrateFromCentre(gauss2, Double.NaN, MAX_INT_WIDTH, true);
        intGauss1.setStyle("strokeColor=darkred");
        intGauss2.setStyle("strokeColor=darkgreen");

        chart2.getDatasets().addAll(intAverage, intGauss1, intGauss2);
        chart2.getDatasets().addAll(intData);

        final var labelRenderer = new LabelledMarkerRenderer();
        final var marker1 = new DoubleDataSet("marker1");
        marker1.setStyle("strokeColor=darkRed; fillColor=darkRed;" + FONT_SIZE);
        for (var i = 1; i < 3; i++) {
            marker1.add(i * SIGMA1, 1.0, "" + i + SIGMA_CHAR);
        }

        final var marker1b = new DoubleDataSet("marker1b");
        marker1b.setStyle("strokeColor=red; fillColor=red;" + FONT_SIZE);
        final double sigma1 = DataSetMath.integralWidth(gauss1, Double.NaN, MAX_INT_WIDTH, 0.683);
        final double sigma2 = DataSetMath.integralWidth(gauss1, Double.NaN, MAX_INT_WIDTH, 0.955);
        marker1b.add(sigma1, 1.0, " ");
        marker1b.add(sigma2, 1.0, " ");
        LOGGER.atInfo().addArgument(1 * SIGMA1).addArgument(sigma1).log("sigma {} (def) vs {} (meas)");
        LOGGER.atInfo().addArgument(2 * SIGMA1).addArgument(sigma2).log("sigma {} (def) vs {} (meas)");

        final var marker2 = new DoubleDataSet("marker2");
        marker2.setStyle("strokeColor=darkBlue; fillColor=darkBlue;" + FONT_SIZE);
        marker2.add(DataSetMath.integralWidth(avgOffsetCompensated, Double.NaN, MAX_INT_WIDTH, 0.683), 1.0, "" + 1 + SIGMA_CHAR);
        marker2.add(DataSetMath.integralWidth(avgOffsetCompensated, Double.NaN, MAX_INT_WIDTH, 0.955), 1.0, "" + 2 + SIGMA_CHAR);

        labelRenderer.getDatasets().addAll(marker1, marker1b, marker2);
        chart2.getRenderers().add(labelRenderer);

        return new VBox(chart1, chart2);
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }

    private List<DataSet> readDemoData() {
        List<DataSet> dataSets = new ArrayList<>();
        try {
            try (var reader = new BufferedReader(new InputStreamReader(EMDSample.class.getResourceAsStream(FILE_NAME)))) {
                String line = reader.readLine(); // read first line
                assert line != null;
                while ((line = reader.readLine()) != null) {
                    // parse spectral header
                    final String header = StringUtils.split(line, " ")[1];
                    final var harmonic = Integer.parseInt(StringUtils.split(header, "#")[1]);
                    final String xValues = reader.readLine();
                    String[] x = StringUtils.split(xValues, ","); // parse x coordinates
                    final String yValues = reader.readLine();
                    String[] y = StringUtils.split(yValues, ","); // parse x coordinates
                    assert x.length == y.length;
                    final var dataSet = new DoubleDataSet("h=" + harmonic, x.length - 1);
                    for (var i = 1; i < x.length; i++) {
                        dataSet.add(Double.parseDouble(x[i]), Double.parseDouble(y[i]));
                    }

                    dataSet.recomputeLimits(DIM_Y);
                    if (harmonic > 100 && dataSet.getAxisDescription(DIM_Y).getMax() < 5 && harmonic != 159 && harmonic != 154) {
                        dataSet.setStyle(MEAS_STROKE_COLOUR);
                        dataSets.add(dataSet);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.atError().setCause(e).log("read data error");
        }
        return dataSets;
    }
}
