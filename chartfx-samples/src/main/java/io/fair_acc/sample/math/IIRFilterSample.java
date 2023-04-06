package io.fair_acc.sample.math;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipInputStream;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.math.DataSetMath;
import io.fair_acc.math.filter.iir.Butterworth;
import io.fair_acc.sample.chart.ChartSample;
import io.fair_acc.sample.math.utils.DemoChart;

/**
 * Reads schottky measurement data and downmixes it with the following algorithm: * apply band-pass arround the relevant
 * band * multiply with 28 MHz sine signal * low-pass filter * downsampling Then applies different IIR Filters to the
 * signal
 *
 * @author rstein
 */
public class IIRFilterSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(IIRFilterSample.class);
    private static final int ORDER = 32; // Order of filters

    private final double sampling = 100e6; // Sampling Rate: 100 MS/s
    private final double center = 28e6; // Center Frequency: 28 MHz
    private final double width = 0.5e6; // Signal Bandwidth: 0.5 MHz
    private final int decimationFactor = 100;

    private DataSet fraw;
    private DataSet fraw1;
    private DataSet fraw2;
    private DataSet fspectra;
    private DataSet fspectra1;
    private DataSet fspectra2;

    @Override
    public Node getChartPanel(Stage stage) {
        initData();
        final DemoChart chart1 = new DemoChart();
        chart1.getXAxis().setName("time");
        chart1.getXAxis().setUnit("s");
        chart1.getYAxis().setName("magnitude");
        chart1.getYAxis().setUnit("a.u.");
        chart1.getDatasets().addAll(fraw1, fraw2);

        final DemoChart chart2 = new DemoChart();
        chart2.getXAxis().setName("frequency [fs]");
        chart2.getXAxis().setUnit("fs");
        // chart2.getXAxis().setAutoRanging(false);
        // chart2.getXAxis().setUpperBound(28e6 + 1e6);
        // chart2.getXAxis().setLowerBound(28e6 - 1e6);
        chart2.getYAxis().setName("magnitude");
        chart2.getYAxis().setUnit("a.u.");
        chart2.getDatasets().addAll(fspectra, fspectra1, fspectra2);

        return new VBox(chart1, chart2);
    }

    private void initData() {
        final double fs = 100e6;
        final int nBins = 4 * 8192;
        fraw = readDemoData(27500, nBins);
        if (fraw == null) {
            throw new IllegalStateException("fraw is null");
        }
        fraw1 = readDemoData(27500 + (int) (0.5e-3 * fs), nBins);
        if (fraw1 == null) {
            throw new IllegalStateException("fraw1 is null");
        }
        fraw2 = readDemoData(27500 + (int) (1.5e-3 * fs), nBins);
        if (fraw2 == null) {
            throw new IllegalStateException("fraw2 is null");
        }
        LOGGER.atInfo().log("length 0 = " + fraw.getDataCount());
        LOGGER.atInfo().log("length 1 = " + fraw1.getDataCount());
        LOGGER.atInfo().log("length 2 = " + fraw2.getDataCount());

        fspectra = DataSetMath.magnitudeSpectrumDecibel(fraw);
        fspectra1 = DataSetMath.magnitudeSpectrumDecibel(fraw1);
        fspectra2 = DataSetMath.magnitudeSpectrumDecibel(fraw2);
    }

    private DataSet readDemoData(final int offset, final int nSamples) {
        // setup Bandpass to capture Signal
        final Butterworth bandPass = new Butterworth();
        bandPass.bandPass(ORDER, sampling, center, width);
        // setup lowPass
        final Butterworth lowPass = new Butterworth();
        lowPass.lowPass(ORDER, sampling, width);
        // initalise return data set
        final DoubleDataSet ret = new DoubleDataSet("raw data@" + offset);
        // read measurement data: 400 000 samples, 1MS/s
        InputStream inputStream = IIRFilterSample.class.getResourceAsStream("20190319_Schottky_SumX.csv.zip");
        try (ZipInputStream zipStream = new ZipInputStream(inputStream)) {
            while (zipStream.getNextEntry() != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipStream))) {
                    for (int i = 0; i < 2; i++) {
                        // skip header;
                        reader.readLine();
                    }

                    int count = 0;
                    int n = 0;
                    LOGGER.atInfo().log("start reading from " + offset);
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        final String[] str = line.split(",");
                        final double s = Math.sin(2 * Math.PI * (sampling - width) * count);
                        final double y = lowPass.filter(s * bandPass.filter(Double.parseDouble(str[1])));
                        if (count >= offset && n < nSamples && count % decimationFactor == 0) {
                            // actual downsampling
                            ret.add(n / sampling, y);
                            n++;
                        }

                        count++;
                    }
                    LOGGER.atInfo().log("finished reading nSamples(total) = " + count);
                    return ret;

                } catch (final Exception e) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.atError().setCause(e).log("read error");
                    }
                }

                zipStream.closeEntry();
            }
        } catch (final Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("read error");
            }
        }
        return null;
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
