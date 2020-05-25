package de.gsi.math.filter.iir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.math.SimpleDataSetEstimators.getMaximum;
import static de.gsi.math.SimpleDataSetEstimators.getRange;

import org.apache.commons.math3.complex.Complex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.math.DataSetMath;
import de.gsi.math.TMathConstants;

public class IirFilterTests {
    private static final double EPSILON_DB = 0.01;
    private static final int N_SAMPLES = 512;
    private static final int N_SAMPLES_FFT = N_SAMPLES / 2;
    private static final int N_SAMPLE_RATE = 1000;
    private static final double F_CUT_LOW = 0.1;
    private static final double F_CUT_HIGH = 0.3;
    private static final double F_BAND_CENTRE = 0.25;
    private static final double F_BAND_WIDTH = 0.15;
    private static final double F_BAND_START = F_BAND_CENTRE - F_BAND_WIDTH / 2;
    private static final double F_BAND_STOP = F_BAND_CENTRE + F_BAND_WIDTH / 2;
    private static final double ALLOWED_IN_BAND_RIPPLE_DB = 3;
    private static final double ALLOWED_OUT_OF_BAND_RIPPLE_DB = 20;
    private final DataSet demoDataSet = generateDemoDataSet();

    @DisplayName("Bessel - Band-Pass")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testBesselBandPass(final int filterOrder, final int algorithmVariant) {
        final Bessel iirBandPass = new Bessel();
        switch (algorithmVariant) {
        case 1:
            iirBandPass.bandPass(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirBandPass.bandPass(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirBandPass.bandPass(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH);
            break;
        }

        final DataSet magBandPass = filterAndGetMagnitudeSpectrum(iirBandPass, demoDataSet);
        assertThat("band-pass cut-off (low)", magBandPass.getValue(DIM_X, F_BAND_START), lessThan(EPSILON_DB));
        assertThat("band-pass cut-off (high)", magBandPass.getValue(DIM_X, F_BAND_STOP), lessThan(EPSILON_DB));
        assertThat("band-pass rejection (low)", magBandPass.getValue(DIM_X, 0.1 * F_BAND_START), lessThan(3.0 + EPSILON_DB - 10 * filterOrder));
        assertThat("band-pass rejection (high)", magBandPass.getValue(DIM_X, 10 * F_BAND_STOP), lessThan(3.0 + EPSILON_DB - 10 * filterOrder));
        final double rangePassBand = getRange(magBandPass,
                magBandPass.getIndex(DIM_X, (F_BAND_CENTRE - 0.1 * F_BAND_WIDTH)),
                magBandPass.getIndex(DIM_X, (F_BAND_CENTRE + 0.1 * F_BAND_WIDTH)));
        assertThat("band-pass pass-band ripple", rangePassBand, lessThan(10 * EPSILON_DB));
    }

    @DisplayName("Bessel - Band-Stop")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testBesselBandStop(final int filterOrder, final int algorithmVariant) {
        final Bessel iirBandStop = new Bessel();
        switch (algorithmVariant) {
        case 1:
            iirBandStop.bandStop(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirBandStop.bandStop(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirBandStop.bandStop(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH);
            break;
        }

        final DataSet magBandStop = filterAndGetMagnitudeSpectrum(iirBandStop, demoDataSet);
        assertThat("band-stop cut-off (low)", magBandStop.getValue(DIM_X, F_BAND_START), lessThan(EPSILON_DB));
        assertThat("band-stop cut-off (high)", magBandStop.getValue(DIM_X, F_BAND_STOP), lessThan(EPSILON_DB));
        assertThat("band-pass pass-band (low)", getRange(magBandStop, 0, magBandStop.getIndex(DIM_X, F_BAND_START * 0.1)), lessThan(EPSILON_DB));
        assertThat("band-pass pass-band (high)", getRange(magBandStop, (int) (N_SAMPLES_FFT * 0.95), N_SAMPLES_FFT), lessThan(EPSILON_DB));
        final double rangeStopBand = getMaximum(magBandStop,
                magBandStop.getIndex(DIM_X, (F_BAND_CENTRE - 0.03 * F_BAND_WIDTH)),
                magBandStop.getIndex(DIM_X, (F_BAND_CENTRE + 0.03 * F_BAND_WIDTH)));
        assertThat("band-pass stop-band ripple", rangeStopBand, lessThan(3.0 + EPSILON_DB - 10 * filterOrder));
    }

    @DisplayName("Bessel - High-Pass")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testBesselHighPass(final int filterOrder, final int algorithmVariant) {
        final Bessel iirHighPass = new Bessel();
        switch (algorithmVariant) {
        case 1:
            iirHighPass.highPass(filterOrder, 1.0, F_CUT_HIGH, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirHighPass.highPass(filterOrder, 1.0, F_CUT_HIGH, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirHighPass.highPass(filterOrder, 1.0, F_CUT_HIGH);
            break;
        }

        final DataSet magHighPass = filterAndGetMagnitudeSpectrum(iirHighPass, demoDataSet);
        assertThat("high-pass cut-off", magHighPass.getValue(DIM_X, F_CUT_HIGH), lessThan(EPSILON_DB));
        assertThat("high-pass rejection", magHighPass.getValue(DIM_X, 0.1 * F_CUT_HIGH), lessThan(3.0 + EPSILON_DB - 10 * filterOrder));
        assertThat("high-pass pass-band ripple", getRange(magHighPass, (int) (N_SAMPLES_FFT * 0.95), N_SAMPLES_FFT), lessThan(2 * EPSILON_DB));
    }
    @DisplayName("Bessel - Low-Pass")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testBesselLowPass(final int filterOrder, final int algorithmVariant) {
        final Bessel iirLowPass = new Bessel();
        switch (algorithmVariant) {
        case 1:
            iirLowPass.lowPass(filterOrder, 1.0, F_CUT_LOW, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirLowPass.lowPass(filterOrder, 1.0, F_CUT_LOW, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirLowPass.lowPass(filterOrder, 1.0, F_CUT_LOW);
            break;
        }
        final DataSet magLowPass = filterAndGetMagnitudeSpectrum(iirLowPass, demoDataSet);
        assertThat("low-pass cut-off", magLowPass.getValue(DIM_X, F_CUT_LOW), lessThan(EPSILON_DB));
        assertThat("low-pass rejection", magLowPass.getValue(DIM_X, 10 * F_CUT_LOW), lessThan(-3.0 + EPSILON_DB - 10 * filterOrder));
        assertThat("low-pass pass-band ripple", getRange(magLowPass, 0, magLowPass.getIndex(DIM_X, F_CUT_LOW * 0.1)), lessThan(EPSILON_DB));
    }

    @DisplayName("Butterworth - Band-Pass")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testButterworthBandPass(final int filterOrder, final int algorithmVariant) {
        final Butterworth iirBandPass = new Butterworth();
        switch (algorithmVariant) {
        case 1:
            iirBandPass.bandPass(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirBandPass.bandPass(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirBandPass.bandPass(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH);
            break;
        }

        final DataSet magBandPass = filterAndGetMagnitudeSpectrum(iirBandPass, demoDataSet);
        assertThat("band-pass cut-off (low)", magBandPass.getValue(DIM_X, F_BAND_START), lessThan(-3.0 + EPSILON_DB));
        assertThat("band-pass cut-off (high)", magBandPass.getValue(DIM_X, F_BAND_STOP), lessThan(-3.0 + EPSILON_DB));
        assertThat("band-pass rejection (low)", magBandPass.getValue(DIM_X, 0.1 * F_BAND_START), lessThan(-3.0 + EPSILON_DB - 20 * filterOrder));
        assertThat("band-pass rejection (high)", magBandPass.getValue(DIM_X, 10 * F_BAND_STOP), lessThan(-3.0 + EPSILON_DB - 20 * filterOrder));
        final double rangePassBand = getRange(magBandPass,
                magBandPass.getIndex(DIM_X, (F_BAND_CENTRE - 0.1 * F_BAND_WIDTH)),
                magBandPass.getIndex(DIM_X, (F_BAND_CENTRE + 0.1 * F_BAND_WIDTH)));
        assertThat("band-pass pass-band ripple", rangePassBand, lessThan(EPSILON_DB));
    }

    @DisplayName("Butterworth - Band-Stop")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testButterworthBandStop(final int filterOrder, final int algorithmVariant) {
        final Butterworth iirBandStop = new Butterworth();
        switch (algorithmVariant) {
        case 1:
            iirBandStop.bandStop(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirBandStop.bandStop(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirBandStop.bandStop(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH);
            break;
        }

        final DataSet magBandStop = filterAndGetMagnitudeSpectrum(iirBandStop, demoDataSet);
        assertThat("band-stop cut-off (low)", magBandStop.getValue(DIM_X, F_BAND_START), lessThan(-3.0 + EPSILON_DB));
        assertThat("band-stop cut-off (high)", magBandStop.getValue(DIM_X, F_BAND_STOP), lessThan(-3.0 + EPSILON_DB));
        assertThat("band-pass pass-band (low)", getRange(magBandStop, 0, magBandStop.getIndex(DIM_X, F_BAND_START * 0.1)), lessThan(EPSILON_DB));
        assertThat("band-pass pass-band (high)", getRange(magBandStop, (int) (N_SAMPLES_FFT * 0.95), N_SAMPLES_FFT), lessThan(EPSILON_DB));
        final double rangeStopBand = getMaximum(magBandStop,
                magBandStop.getIndex(DIM_X, (F_BAND_CENTRE - 0.03 * F_BAND_WIDTH)),
                magBandStop.getIndex(DIM_X, (F_BAND_CENTRE + 0.03 * F_BAND_WIDTH)));
        assertThat("band-pass stop-band ripple", rangeStopBand, lessThan(-3.0 + EPSILON_DB - 20 * filterOrder));
    }

    @DisplayName("Butterworth - High-Pass")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testButterworthHighPass(final int filterOrder, final int algorithmVariant) {
        final Butterworth iirHighPass = new Butterworth();
        switch (algorithmVariant) {
        case 1:
            iirHighPass.highPass(filterOrder, 1.0, F_CUT_HIGH, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirHighPass.highPass(filterOrder, 1.0, F_CUT_HIGH, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirHighPass.highPass(filterOrder, 1.0, F_CUT_HIGH);
            break;
        }

        final DataSet magHighPass = filterAndGetMagnitudeSpectrum(iirHighPass, demoDataSet);
        assertThat("high-pass cut-off", magHighPass.getValue(DIM_X, F_CUT_HIGH), lessThan(-3.0 + EPSILON_DB));
        assertThat("high-pass rejection", magHighPass.getValue(DIM_X, 0.1 * F_CUT_HIGH), lessThan(-3.0 + EPSILON_DB - 20 * filterOrder));
        assertThat("high-pass pass-band ripple", getRange(magHighPass, (int) (N_SAMPLES_FFT * 0.95), N_SAMPLES_FFT), lessThan(EPSILON_DB));
    }

    @DisplayName("Butterworth - Low-Pass")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testButterworthLowPass(final int filterOrder, final int algorithmVariant) {
        final Butterworth iirLowPass = new Butterworth();
        switch (algorithmVariant) {
        case 1:
            iirLowPass.lowPass(filterOrder, 1.0, F_CUT_LOW, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirLowPass.lowPass(filterOrder, 1.0, F_CUT_LOW, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirLowPass.lowPass(filterOrder, 1.0, F_CUT_LOW);
            break;
        }
        final DataSet magLowPass = filterAndGetMagnitudeSpectrum(iirLowPass, demoDataSet);
        assertThat("low-pass cut-off", magLowPass.getValue(DIM_X, F_CUT_LOW), lessThan(-3.0 + EPSILON_DB));
        assertThat("low-pass rejection", magLowPass.getValue(DIM_X, 10 * F_CUT_LOW), lessThan(-3.0 + EPSILON_DB - 20 * filterOrder));
        assertThat("low-pass pass-band ripple", getRange(magLowPass, 0, magLowPass.getIndex(DIM_X, F_CUT_LOW * 0.1)), lessThan(EPSILON_DB));
    }

    @Test
    public void testButterworthResponse() {
        final int filterOrder = 4;
        final Butterworth iirLowPass = new Butterworth();
        iirLowPass.lowPass(filterOrder, 1.0, F_CUT_LOW);
        final DataSet magLowPass = filterAndGetMagnitudeSpectrum(iirLowPass, demoDataSet);
        assertThat("low-pass cut-off", magLowPass.getValue(DIM_X, F_CUT_LOW), lessThan(-3.0 + EPSILON_DB));
        assertThat("low-pass rejection", magLowPass.getValue(DIM_X, 10 * F_CUT_LOW), lessThan(-3.0 + EPSILON_DB - 20 * filterOrder));
        assertThat("low-pass pass-band ripple", getRange(magLowPass, 0, magLowPass.getIndex(DIM_X, F_CUT_LOW * 0.1)), lessThan(EPSILON_DB));

        // response
        assertEquals(filterOrder / 2, iirLowPass.getNumBiquads());
        Complex cornerFrequency = new Complex(1, 0);
        for (int i = 0; i < iirLowPass.getNumBiquads(); i++) {
            cornerFrequency = cornerFrequency.multiply(iirLowPass.getBiquad(i).response(F_CUT_LOW));
        }
        final double valueAtCutOff = 20 * TMathConstants.Log10(cornerFrequency.abs());
        assertThat("low-pass cut-off (response calculation)", valueAtCutOff, lessThan(-3.0 + EPSILON_DB));
    }

    @DisplayName("ChebyshevI - Band-Pass")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testChebyshevIBandPass(final int filterOrder, final int algorithmVariant) {
        final ChebyshevI iirBandPass = new ChebyshevI();
        switch (algorithmVariant) {
        case 1:
            iirBandPass.bandPass(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, ALLOWED_IN_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirBandPass.bandPass(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, ALLOWED_IN_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirBandPass.bandPass(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, ALLOWED_IN_BAND_RIPPLE_DB);
            break;
        }

        final DataSet magBandPass = filterAndGetMagnitudeSpectrum(iirBandPass, demoDataSet);
        assertThat("band-pass cut-off (low)", magBandPass.getValue(DIM_X, F_BAND_START), lessThan(-3.0 + EPSILON_DB));
        assertThat("band-pass cut-off (high)", magBandPass.getValue(DIM_X, F_BAND_STOP), lessThan(-3.0 + EPSILON_DB));
        assertThat("band-pass rejection (low)", magBandPass.getValue(DIM_X, 0.1 * F_BAND_START), lessThan(-3.0 + EPSILON_DB - 20 * filterOrder));
        assertThat("band-pass rejection (high)", magBandPass.getValue(DIM_X, 10 * F_BAND_STOP), lessThan(-3.0 + EPSILON_DB - 20 * filterOrder));
        final double rangePassBand = getRange(magBandPass,
                magBandPass.getIndex(DIM_X, (F_BAND_CENTRE - 0.1 * F_BAND_WIDTH)),
                magBandPass.getIndex(DIM_X, (F_BAND_CENTRE + 0.1 * F_BAND_WIDTH)));
        assertThat("band-pass pass-band ripple", rangePassBand, lessThan(ALLOWED_IN_BAND_RIPPLE_DB + EPSILON_DB));
    }
    @DisplayName("ChebyshevI - Band-Stop")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testChebyshevIBandStop(final int filterOrder, final int algorithmVariant) {
        final ChebyshevI iirBandStop = new ChebyshevI();
        switch (algorithmVariant) {
        case 1:
            iirBandStop.bandStop(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, ALLOWED_IN_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirBandStop.bandStop(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, ALLOWED_IN_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirBandStop.bandStop(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, ALLOWED_IN_BAND_RIPPLE_DB);
            break;
        }

        final DataSet magBandStop = filterAndGetMagnitudeSpectrum(iirBandStop, demoDataSet);
        assertThat("band-stop cut-off (low)", magBandStop.getValue(DIM_X, F_BAND_START), lessThan(-3.0 + EPSILON_DB));
        assertThat("band-stop cut-off (high)", magBandStop.getValue(DIM_X, F_BAND_STOP), lessThan(-3.0 + EPSILON_DB));
        assertThat("band-pass pass-band (low)", getRange(magBandStop, 0, magBandStop.getIndex(DIM_X, F_BAND_START * 0.1)), lessThan(ALLOWED_IN_BAND_RIPPLE_DB + EPSILON_DB));
        assertThat("band-pass pass-band (high)", getRange(magBandStop, (int) (N_SAMPLES_FFT * 0.95), N_SAMPLES_FFT), lessThan(ALLOWED_IN_BAND_RIPPLE_DB + EPSILON_DB));
        final double rangeStopBand = getMaximum(magBandStop,
                magBandStop.getIndex(DIM_X, (F_BAND_CENTRE - 0.03 * F_BAND_WIDTH)),
                magBandStop.getIndex(DIM_X, (F_BAND_CENTRE + 0.03 * F_BAND_WIDTH)));
        assertThat("band-pass stop-band ripple", rangeStopBand, lessThan(-3.0 + EPSILON_DB - 20 * filterOrder));
    }
    @DisplayName("ChebyshevI - High-Pass")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testChebyshevIHighPass(final int filterOrder, final int algorithmVariant) {
        final ChebyshevI iirHighPass = new ChebyshevI();
        switch (algorithmVariant) {
        case 1:
            iirHighPass.highPass(filterOrder, 1.0, F_CUT_HIGH, ALLOWED_IN_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirHighPass.highPass(filterOrder, 1.0, F_CUT_HIGH, ALLOWED_IN_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirHighPass.highPass(filterOrder, 1.0, F_CUT_HIGH, ALLOWED_IN_BAND_RIPPLE_DB);
            break;
        }

        final DataSet magHighPass = filterAndGetMagnitudeSpectrum(iirHighPass, demoDataSet);
        assertThat("high-pass cut-off", magHighPass.getValue(DIM_X, F_CUT_HIGH), lessThan(-3.0 + EPSILON_DB));
        assertThat("high-pass rejection", magHighPass.getValue(DIM_X, 0.1 * F_CUT_HIGH), lessThan(-3.0 + EPSILON_DB - 20 * filterOrder));
        assertThat("high-pass pass-band ripple", getRange(magHighPass, (int) (N_SAMPLES_FFT * 0.95), N_SAMPLES_FFT), lessThan(ALLOWED_IN_BAND_RIPPLE_DB + EPSILON_DB));
    }

    @DisplayName("ChebyshevII - Band-Pass")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testChebyshevIILBandPass(final int filterOrder, final int algorithmVariant) {
        final ChebyshevII iirBandPass = new ChebyshevII();
        switch (algorithmVariant) {
        case 1:
            iirBandPass.bandPass(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, ALLOWED_OUT_OF_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirBandPass.bandPass(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, ALLOWED_OUT_OF_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirBandPass.bandPass(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, ALLOWED_OUT_OF_BAND_RIPPLE_DB);
            break;
        }

        final DataSet magBandPass = filterAndGetMagnitudeSpectrum(iirBandPass, demoDataSet);
        assertThat("band-pass cut-off (low)", magBandPass.getValue(DIM_X, F_BAND_START), lessThan(ALLOWED_OUT_OF_BAND_RIPPLE_DB + EPSILON_DB));
        assertThat("band-pass cut-off (high)", magBandPass.getValue(DIM_X, F_BAND_STOP), lessThan(ALLOWED_OUT_OF_BAND_RIPPLE_DB + EPSILON_DB));
        assertThat("band-pass rejection (low)", magBandPass.getValue(DIM_X, 0.1 * F_BAND_START), lessThan(ALLOWED_OUT_OF_BAND_RIPPLE_DB + EPSILON_DB));
        assertThat("band-pass rejection (high)", magBandPass.getValue(DIM_X, 10 * F_BAND_STOP), lessThan(ALLOWED_OUT_OF_BAND_RIPPLE_DB + EPSILON_DB));
        final double rangePassBand = getRange(magBandPass,
                magBandPass.getIndex(DIM_X, (F_BAND_CENTRE - 0.1 * F_BAND_WIDTH)),
                magBandPass.getIndex(DIM_X, (F_BAND_CENTRE + 0.1 * F_BAND_WIDTH)));
        assertThat("band-pass pass-band ripple", rangePassBand, lessThan(ALLOWED_IN_BAND_RIPPLE_DB + EPSILON_DB));
    }

    @DisplayName("ChebyshevII - BandStop")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testChebyshevIILBandStop(final int filterOrder, final int algorithmVariant) {
        final ChebyshevII iirBandStop = new ChebyshevII();
        switch (algorithmVariant) {
        case 1:
            iirBandStop.bandStop(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, ALLOWED_OUT_OF_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirBandStop.bandStop(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, ALLOWED_OUT_OF_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirBandStop.bandStop(filterOrder, 1.0, F_BAND_CENTRE, F_BAND_WIDTH, ALLOWED_OUT_OF_BAND_RIPPLE_DB);
            break;
        }

        final DataSet magBandStop = filterAndGetMagnitudeSpectrum(iirBandStop, demoDataSet);
        assertThat("band-stop cut-off (low)", magBandStop.getValue(DIM_X, F_BAND_START), lessThan(ALLOWED_OUT_OF_BAND_RIPPLE_DB + EPSILON_DB));
        assertThat("band-stop cut-off (high)", magBandStop.getValue(DIM_X, F_BAND_STOP), lessThan(ALLOWED_OUT_OF_BAND_RIPPLE_DB + EPSILON_DB));
        assertThat("band-pass pass-band (low)", getRange(magBandStop, 0, magBandStop.getIndex(DIM_X, F_BAND_START * 0.1)), lessThan(ALLOWED_IN_BAND_RIPPLE_DB + EPSILON_DB));
        assertThat("band-pass pass-band (high)", getRange(magBandStop, (int) (N_SAMPLES_FFT * 0.95), N_SAMPLES_FFT), lessThan(ALLOWED_IN_BAND_RIPPLE_DB + EPSILON_DB));
        final double rangeStopBand = getMaximum(magBandStop,
                magBandStop.getIndex(DIM_X, (F_BAND_CENTRE - 0.03 * F_BAND_WIDTH)),
                magBandStop.getIndex(DIM_X, (F_BAND_CENTRE + 0.03 * F_BAND_WIDTH)));
        assertThat("band-pass stop-band ripple", rangeStopBand, lessThan(ALLOWED_OUT_OF_BAND_RIPPLE_DB + EPSILON_DB));
    }

    @DisplayName("ChebyshevII - High-Pass")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testChebyshevIILHighPass(final int filterOrder, final int algorithmVariant) {
        final ChebyshevII iirHighPass = new ChebyshevII();
        switch (algorithmVariant) {
        case 1:
            iirHighPass.highPass(filterOrder, 1.0, F_CUT_HIGH, ALLOWED_OUT_OF_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirHighPass.highPass(filterOrder, 1.0, F_CUT_HIGH, ALLOWED_OUT_OF_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirHighPass.highPass(filterOrder, 1.0, F_CUT_HIGH, ALLOWED_OUT_OF_BAND_RIPPLE_DB);
            break;
        }

        final DataSet magHighPass = filterAndGetMagnitudeSpectrum(iirHighPass, demoDataSet);
        assertThat("high-pass cut-off", magHighPass.getValue(DIM_X, F_CUT_HIGH), lessThan(ALLOWED_OUT_OF_BAND_RIPPLE_DB + EPSILON_DB));
        assertThat("high-pass rejection", getMaximum(magHighPass, 0, magHighPass.getIndex(DIM_X, 0.8 * F_CUT_HIGH)), lessThan(ALLOWED_OUT_OF_BAND_RIPPLE_DB + EPSILON_DB));
        assertThat("high-pass pass-band ripple", getRange(magHighPass, (int) (N_SAMPLES_FFT * 0.95), N_SAMPLES_FFT), lessThan(ALLOWED_IN_BAND_RIPPLE_DB + EPSILON_DB));
    }

    @DisplayName("ChebyshevII - Low-pass")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testChebyshevIILowPass(final int filterOrder, final int algorithmVariant) {
        final ChebyshevII iirLowPass = new ChebyshevII();
        switch (algorithmVariant) {
        case 1:
            iirLowPass.lowPass(filterOrder, 1.0, F_CUT_LOW, ALLOWED_OUT_OF_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirLowPass.lowPass(filterOrder, 1.0, F_CUT_LOW, ALLOWED_OUT_OF_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirLowPass.lowPass(filterOrder, 1.0, F_CUT_LOW, ALLOWED_OUT_OF_BAND_RIPPLE_DB);
            break;
        }
        final DataSet magLowPass = filterAndGetMagnitudeSpectrum(iirLowPass, demoDataSet);
        assertThat("low-pass cut-off", magLowPass.getValue(DIM_X, F_CUT_LOW), lessThan(ALLOWED_OUT_OF_BAND_RIPPLE_DB + EPSILON_DB));
        assertThat("low-pass rejection", getMaximum(magLowPass, magLowPass.getIndex(DIM_X, 2 * F_CUT_LOW), N_SAMPLES_FFT), lessThan(ALLOWED_OUT_OF_BAND_RIPPLE_DB + EPSILON_DB));
        assertThat("low-pass pass-band ripple", getRange(magLowPass, 0, magLowPass.getIndex(DIM_X, F_CUT_LOW * 0.1)), lessThan(EPSILON_DB));
    }

    @DisplayName("ChebyshevI - Low-Pass")
    @ParameterizedTest(name = "{displayName}: filter-order: {0}, algorithm: {1}")
    @CsvSource({ "2, 0", "3, 0", "4, 0", "2, 1", "3, 1", "4, 1", "2, 2", "3, 2", "4, 2" })
    public void testChebyshevILowPass(final int filterOrder, final int algorithmVariant) {
        final ChebyshevI iirLowPass = new ChebyshevI();
        switch (algorithmVariant) {
        case 1:
            iirLowPass.lowPass(filterOrder, 1.0, F_CUT_LOW, ALLOWED_IN_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_I);
            break;
        case 2:
            iirLowPass.lowPass(filterOrder, 1.0, F_CUT_LOW, ALLOWED_IN_BAND_RIPPLE_DB, DirectFormAbstract.DIRECT_FORM_II);
            break;
        case 0:
        default:
            iirLowPass.lowPass(filterOrder, 1.0, F_CUT_LOW, ALLOWED_IN_BAND_RIPPLE_DB);
            break;
        }
        final DataSet magLowPass = filterAndGetMagnitudeSpectrum(iirLowPass, demoDataSet);
        assertThat("low-pass cut-off", magLowPass.getValue(DIM_X, F_CUT_LOW), lessThan(-3.0 + EPSILON_DB));
        assertThat("low-pass rejection", magLowPass.getValue(DIM_X, 10 * F_CUT_LOW), lessThan(-3.0 + EPSILON_DB - 20 * filterOrder));
        assertThat("low-pass pass-band ripple", getRange(magLowPass, 0, magLowPass.getIndex(DIM_X, F_CUT_LOW * 0.1)), lessThan(ALLOWED_IN_BAND_RIPPLE_DB + EPSILON_DB));
    }

    @Test
    public void testConstructors() {
        assertDoesNotThrow(() -> new Butterworth());
        assertDoesNotThrow(() -> new Bessel());
        assertDoesNotThrow(() -> new ChebyshevI());
        assertDoesNotThrow(() -> new ChebyshevII());
    }

    @Test
    public void testHelperMethodBiquad() {
        assertDoesNotThrow(() -> new Biquad());
        assertDoesNotThrow(() -> new Biquad().setIdentity());
    }

    @Test
    public void testHelperMethodComplexPair() {
        assertDoesNotThrow(() -> new Complex(1, 0));
        assertDoesNotThrow(() -> new ComplexPair(new Complex(1, 0), new Complex(2, 0)));

        assertFalse(new ComplexPair(new Complex(1, 0), new Complex(2, 0)).isNaN());
        assertTrue(new ComplexPair(new Complex(Double.NaN, 0), new Complex(2, 0)).isNaN());
        assertTrue(new ComplexPair(new Complex(Double.NaN, 0), new Complex(2, Double.NaN)).isNaN());
        assertTrue(new ComplexPair(new Complex(1, 0), new Complex(2, Double.NaN)).isNaN());

        assertFalse(new ComplexPair(new Complex(1, +1), new Complex(1, +1)).isConjugate());
        assertTrue(new ComplexPair(new Complex(1, +1), new Complex(1, -1)).isConjugate());

        assertFalse(new ComplexPair(new Complex(1, +1), new Complex(1, +1)).isMatchedPair());
        assertFalse(new ComplexPair(new Complex(1, 0), new Complex(1, +1)).isMatchedPair());
        assertFalse(new ComplexPair(new Complex(1, 1), new Complex(2, 0)).isMatchedPair());
        assertTrue(new ComplexPair(new Complex(1, +1), new Complex(1, -1)).isMatchedPair());
        assertTrue(new ComplexPair(new Complex(1, 0), new Complex(2, 0)).isMatchedPair());
        assertFalse(new ComplexPair(new Complex(0, 0), new Complex(2, 0)).isMatchedPair());
        assertFalse(new ComplexPair(new Complex(0, 0), new Complex(0, 0)).isMatchedPair());

        assertTrue(new ComplexPair(new Complex(0, 0), new Complex(2, 0)).isReal());
        assertFalse(new ComplexPair(new Complex(0, 0), new Complex(2, 1)).isReal());
        assertFalse(new ComplexPair(new Complex(0, 1), new Complex(2, 0)).isReal());
        assertFalse(new ComplexPair(new Complex(0, 1), new Complex(2, 1)).isReal());
    }

    @Test
    public void testHelperMethodLayoutBase() {
        assertDoesNotThrow(() -> new LayoutBase(new PoleZeroPair(new Complex(1, +1), new Complex(1, +1))));

        assertTrue(new PoleZeroPair(new Complex(Double.NaN, 0), new Complex(Double.NaN, 0)).isNaN());
        assertTrue(new PoleZeroPair(new Complex(Double.NaN, 0), new Complex(1, 0)).isNaN());
        assertTrue(new PoleZeroPair(new Complex(1, 0), new Complex(Double.NaN, 0)).isNaN());
        assertFalse(new PoleZeroPair(new Complex(0, 0), new Complex(2, 1)).isNaN());

        assertTrue(new PoleZeroPair(new Complex(0, 0), new Complex(0, 0)).isSinglePole());
        assertTrue(new PoleZeroPair(new Complex(0, 0), new Complex(0, 0)).isSinglePole());
        assertTrue(new PoleZeroPair(new Complex(1, 0), new Complex(Double.NaN, 0)).isSinglePole());

        final LayoutBase normalBase = new LayoutBase(4);
        assertEquals(0, normalBase.getNumPoles());
        assertDoesNotThrow(() -> normalBase.addPoleZeroConjugatePairs(new Complex(1, 0), new Complex(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> normalBase.addPoleZeroConjugatePairs(null, new Complex(1, 0)));
        assertThrows(IllegalArgumentException.class, () -> normalBase.addPoleZeroConjugatePairs(new Complex(1, 0), null));
    }

    private static DataSet filterAndGetMagnitudeSpectrum(final Cascade filter, final DataSet input) {
        DefaultDataSet filteredDataSet = new DefaultDataSet("filtered data");
        for (int i = 0; i < input.getDataCount(); i++) {
            filteredDataSet.add(input.get(DIM_X, i), filter.filter(input.get(DIM_Y, i)));
        }
        return DataSetMath.normalisedMagnitudeSpectrumDecibel(filteredDataSet);
    }

    private static DataSet generateDemoDataSet() {
        // generate some random samples
        final double[] xValues = new double[N_SAMPLES];
        final double[] yValues = new double[N_SAMPLES];
        double fs = N_SAMPLE_RATE;
        for (int i = 0; i < N_SAMPLES; i++) {
            xValues[i] = i / fs;
        }
        yValues[N_SAMPLES / 2] = 0.5 * N_SAMPLES; // dirac delta
        return new DefaultDataSet("dirac", xValues, yValues, xValues.length, true);
    }
}
