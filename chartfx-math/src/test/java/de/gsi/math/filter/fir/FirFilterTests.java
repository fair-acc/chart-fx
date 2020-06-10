package de.gsi.math.filter.fir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.gsi.math.TRandom;
import de.gsi.math.filter.FilterType;

public class FirFilterTests {
    private static final TRandom rnd = new TRandom(0);

    @ParameterizedTest
    @ValueSource(ints = { 512, 513 })
    public void filterLowPassTests(int dim) {
        final double[] input = new double[dim];
        for (int i = 0; i < dim; i++) {
            input[i] = 10.0 + Math.sin(2.0 * Math.PI * 0.4 * i) + TRandom.Gaus(0.0, 0.1);
        }

        final double cutoffFraction = 0.25;
        final int filterOrder = 4;
        final double ripplePercent = 0.0; // butterworth

        assertDoesNotThrow(() -> FirFilter.filterSignal(input, null, cutoffFraction, filterOrder, FilterType.LOW_PASS, ripplePercent));
        assertDoesNotThrow(() -> FirFilter.filterSignal(input, null, cutoffFraction, filterOrder, FilterType.LOW_PASS, 1));
        assertDoesNotThrow(() -> FirFilter.filterSignal(input, null, cutoffFraction, filterOrder, FilterType.HIGH_PASS, ripplePercent));
        assertDoesNotThrow(() -> FirFilter.filterSignal(input, null, cutoffFraction, filterOrder, FilterType.HIGH_PASS, 1));

        assertDoesNotThrow(() -> FirFilter.filterSignal(input, new double[input.length], cutoffFraction, filterOrder, FilterType.LOW_PASS, ripplePercent));
    }
}
