package de.gsi.math.samples;

import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.math.DataSetMath;
import de.gsi.math.filter.iir.Bessel;
import de.gsi.math.filter.iir.Butterworth;
import de.gsi.math.filter.iir.ChebyshevI;
import de.gsi.math.filter.iir.ChebyshevII;
import de.gsi.math.samples.utils.AbstractDemoApplication;
import de.gsi.math.samples.utils.DemoChart;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;

/**
 * Sample to illustrate IIR-based Butterworth and Chebychev filters
 * 
 * @author rstein
 */
public class IIRFrequencyFilterSample extends AbstractDemoApplication {
    private static final String LOW_PASS = "Low-Pass";
    private static final String BAND_STOP = "Band-Stop";
    private static final String BAND_PASS = "Band-Pass";
    private static final String HIGH_PASS = "High-Pass";
    private static final int N_SAMPLES = 32768;
    private static final int N_SAMPLE_RATE = 1000;
    private static final int ORDER = 6;
    private static final double F_CUT_LOW = 0.2;
    private static final double F_CUT_HIGH = 0.3;
    private static final double ALLOWED_IN_BAND_RIPPLE_DB = 3;
    private static final double ALLOWED_OUT_OF_BAND_RIPPLE_DB = 20;
    private final DataSet2D demoDataSet = generateDemoDataSet();

    public IIRFrequencyFilterSample() {
        super(2 * DEFAULT_SCENE_WIDTH, DEFAULT_SCENE_HEIGTH);
    }

    @Override
    public Node getContent() {
        FlowPane flowPane = new FlowPane();

        flowPane.getChildren().add(getDemoChartButterworth());

        flowPane.getChildren().add(getDemoChartBessel());

        flowPane.getChildren().add(getDemoChartChebychevI());

        flowPane.getChildren().add(getDemoChartChebychevII());

        return flowPane;
    }

    private static DataSet2D generateDemoDataSet() {
        // generate some random samples
        final double[] xValues = new double[N_SAMPLES];
        final double[] yValues = new double[N_SAMPLES];
        double fs = N_SAMPLE_RATE;
        for (int i = 0; i < N_SAMPLES; i++) {
            xValues[i] = i / fs;
            // yValues[i] = i < N_SAMPLES / 2 ? 0.0 : 1.0; // step
        }
        yValues[N_SAMPLES / 2] = 0.5 * N_SAMPLES; // dirac delta
        return new DefaultDataSet("dirac", xValues, yValues, xValues.length, true);
    }

    private DemoChart getDemoChart(final String title) {
        final DemoChart defaultChart = new DemoChart();
        defaultChart.setTitle(title);
        defaultChart.getXAxis().setName("frequency");
        defaultChart.getYAxis().setName("magnitude");
        defaultChart.getYAxis().setUnit("dB");
        defaultChart.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(demoDataSet));
        defaultChart.getYAxis().setAutoRanging(false);
        defaultChart.getYAxis().setMin(-35.0);
        defaultChart.getYAxis().setMax(+5.0);
        defaultChart.setPrefSize(DEFAULT_SCENE_WIDTH, 400);
        return defaultChart;
    }

    private DemoChart getDemoChartButterworth() {
        final String filterType = "Butterworth - " + "(" + ORDER + "th-order)";
        DefaultDataSet lowPass = new DefaultDataSet(LOW_PASS);
        DefaultDataSet highPass = new DefaultDataSet(HIGH_PASS);
        DefaultDataSet bandPass = new DefaultDataSet(BAND_PASS);
        DefaultDataSet bandStop = new DefaultDataSet(BAND_STOP);

        final Butterworth iirLowPass = new Butterworth();
        iirLowPass.lowPass(ORDER, 1.0, F_CUT_HIGH);

        final Butterworth iirHighPass = new Butterworth();
        iirHighPass.highPass(ORDER, 1.0, F_CUT_LOW);

        final Butterworth iirBandPass = new Butterworth();
        iirBandPass.bandPass(ORDER, 1.0, 0.5 * (F_CUT_LOW + F_CUT_HIGH), (F_CUT_HIGH - F_CUT_LOW));

        final Butterworth iirBandStop = new Butterworth();
        iirBandStop.bandStop(ORDER, 1.0, 0.5 * (F_CUT_LOW + F_CUT_HIGH), (F_CUT_HIGH - F_CUT_LOW));

        for (int i = 0; i < demoDataSet.getDataCount(); i++) {
            final double x = demoDataSet.getX(i);
            final double y = demoDataSet.getY(i);

            lowPass.add(x, iirLowPass.filter(y));
            highPass.add(x, iirHighPass.filter(y));
            bandPass.add(x, iirBandPass.filter(y));
            bandStop.add(x, iirBandStop.filter(y));
        }

        DemoChart chart = getDemoChart(filterType);
        chart.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(lowPass),
                DataSetMath.normalisedMagnitudeSpectrumDecibel(highPass),
                DataSetMath.normalisedMagnitudeSpectrumDecibel(bandPass),
                DataSetMath.normalisedMagnitudeSpectrumDecibel(bandStop));
        return chart;
    }

    private DemoChart getDemoChartBessel() {
        final String filterType = "Bessel - " + "(" + ORDER + "th-order)";
        DefaultDataSet lowPass = new DefaultDataSet(LOW_PASS);
        DefaultDataSet highPass = new DefaultDataSet(HIGH_PASS);
        DefaultDataSet bandPass = new DefaultDataSet(BAND_PASS);
        DefaultDataSet bandStop = new DefaultDataSet(BAND_STOP);

        final Bessel iirLowPass = new Bessel();
        iirLowPass.lowPass(ORDER, 1.0, F_CUT_HIGH);

        final Bessel iirHighPass = new Bessel();
        iirHighPass.highPass(ORDER, 1.0, F_CUT_LOW);

        final Bessel iirBandPass = new Bessel();
        iirBandPass.bandPass(ORDER, 1.0, 0.5 * (F_CUT_LOW + F_CUT_HIGH), (F_CUT_HIGH - F_CUT_LOW));

        final Bessel iirBandStop = new Bessel();
        iirBandStop.bandStop(ORDER, 1.0, 0.5 * (F_CUT_LOW + F_CUT_HIGH), (F_CUT_HIGH - F_CUT_LOW));

        for (int i = 0; i < demoDataSet.getDataCount(); i++) {
            final double x = demoDataSet.getX(i);
            final double y = demoDataSet.getY(i);

            lowPass.add(x, iirLowPass.filter(y));
            highPass.add(x, iirHighPass.filter(y));
            bandPass.add(x, iirBandPass.filter(y));
            bandStop.add(x, iirBandStop.filter(y));
        }

        DemoChart chart = getDemoChart(filterType);
        chart.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(lowPass),
                DataSetMath.normalisedMagnitudeSpectrumDecibel(highPass),
                DataSetMath.normalisedMagnitudeSpectrumDecibel(bandPass),
                DataSetMath.normalisedMagnitudeSpectrumDecibel(bandStop));
        return chart;
    }

    private DemoChart getDemoChartChebychevI() {
        final String filterType = "ChebyshevI - " + "(" + ORDER + "th-order, " + ALLOWED_IN_BAND_RIPPLE_DB
                + " dB ripple)";
        DefaultDataSet lowPass = new DefaultDataSet(LOW_PASS);
        DefaultDataSet highPass = new DefaultDataSet(HIGH_PASS);
        DefaultDataSet bandPass = new DefaultDataSet(BAND_PASS);
        DefaultDataSet bandStop = new DefaultDataSet(BAND_STOP);

        final ChebyshevI iirLowPass = new ChebyshevI();
        iirLowPass.lowPass(ORDER, 1.0, F_CUT_HIGH, ALLOWED_IN_BAND_RIPPLE_DB);

        final ChebyshevI iirHighPass = new ChebyshevI();
        iirHighPass.highPass(ORDER, 1.0, F_CUT_LOW, ALLOWED_IN_BAND_RIPPLE_DB);

        final ChebyshevI iirBandPass = new ChebyshevI();
        iirBandPass.bandPass(ORDER, 1.0, 0.5 * (F_CUT_LOW + F_CUT_HIGH), (F_CUT_HIGH - F_CUT_LOW),
                ALLOWED_IN_BAND_RIPPLE_DB);

        final ChebyshevI iirBandStop = new ChebyshevI();
        iirBandStop.bandStop(ORDER, 1.0, 0.5 * (F_CUT_LOW + F_CUT_HIGH), (F_CUT_HIGH - F_CUT_LOW),
                ALLOWED_IN_BAND_RIPPLE_DB);

        for (int i = 0; i < demoDataSet.getDataCount(); i++) {
            final double x = demoDataSet.getX(i);
            final double y = demoDataSet.getY(i);

            lowPass.add(x, iirLowPass.filter(y));
            highPass.add(x, iirHighPass.filter(y));
            bandPass.add(x, iirBandPass.filter(y));
            bandStop.add(x, iirBandStop.filter(y));
        }

        DemoChart chart = getDemoChart(filterType);
        chart.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(lowPass),
                DataSetMath.normalisedMagnitudeSpectrumDecibel(highPass),
                DataSetMath.normalisedMagnitudeSpectrumDecibel(bandPass),
                DataSetMath.normalisedMagnitudeSpectrumDecibel(bandStop));
        return chart;
    }

    private DemoChart getDemoChartChebychevII() {
        final String filterType = "ChebyshevII - " + "(" + ORDER + "th-order, " + ALLOWED_OUT_OF_BAND_RIPPLE_DB
                + " dB ripple)";
        DefaultDataSet lowPass = new DefaultDataSet(LOW_PASS);
        DefaultDataSet highPass = new DefaultDataSet(HIGH_PASS);
        DefaultDataSet bandPass = new DefaultDataSet(BAND_PASS);
        DefaultDataSet bandStop = new DefaultDataSet(BAND_STOP);

        final ChebyshevII iirLowPass = new ChebyshevII();
        iirLowPass.lowPass(ORDER, 1.0, F_CUT_HIGH, ALLOWED_OUT_OF_BAND_RIPPLE_DB);

        final ChebyshevII iirHighPass = new ChebyshevII();
        iirHighPass.highPass(ORDER, 1.0, F_CUT_LOW, ALLOWED_OUT_OF_BAND_RIPPLE_DB);

        final ChebyshevII iirBandPass = new ChebyshevII();
        iirBandPass.bandPass(ORDER, 1.0, 0.5 * (F_CUT_LOW + F_CUT_HIGH), (F_CUT_HIGH - F_CUT_LOW),
                ALLOWED_OUT_OF_BAND_RIPPLE_DB);

        final ChebyshevII iirBandStop = new ChebyshevII();
        iirBandStop.bandStop(ORDER, 1.0, 0.5 * (F_CUT_LOW + F_CUT_HIGH), (F_CUT_HIGH - F_CUT_LOW),
                ALLOWED_OUT_OF_BAND_RIPPLE_DB);

        for (int i = 0; i < demoDataSet.getDataCount(); i++) {
            final double x = demoDataSet.getX(i);
            final double y = demoDataSet.getY(i);

            lowPass.add(x, iirLowPass.filter(y));
            highPass.add(x, iirHighPass.filter(y));
            bandPass.add(x, iirBandPass.filter(y));
            bandStop.add(x, iirBandStop.filter(y));
        }

        DemoChart chart = getDemoChart(filterType);
        chart.getRenderer(0).getDatasets().addAll(DataSetMath.normalisedMagnitudeSpectrumDecibel(lowPass),
                DataSetMath.normalisedMagnitudeSpectrumDecibel(highPass),
                DataSetMath.normalisedMagnitudeSpectrumDecibel(bandPass),
                DataSetMath.normalisedMagnitudeSpectrumDecibel(bandStop));
        return chart;
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
