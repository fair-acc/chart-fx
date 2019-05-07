/**
 * Copyright (c) 2016 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.axes.spi.format;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import de.gsi.chart.axes.TickUnitSupplier;

/**
 * TickUnitSupplier that computes tick units as multiplications of powers of 10.
 * <p>
 * The multipliers (factors) can be specified during the construction. They are expected to be numbers between 1
 * (inclusive) and 10 (exclusive). See {@link #computeTickUnit(double)} for more details.
 *
 * @author Grzegorz Kruk
 */
public final class DefaultTickUnitSupplier implements TickUnitSupplier {
    private static final int BASE = 10;
    private static final SortedSet<Number> DEFAULT_MULTIPLIERS = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(1d, 2.5, 5d)));
    private final double[] multipliers;

    /**
     * Creates a new instance of {@code DefaultTickUnitSupplier} initialized with multipliers: [1, 2.5, 5].
     */
    public DefaultTickUnitSupplier() {
        this(DefaultTickUnitSupplier.DEFAULT_MULTIPLIERS);
    }

    /**
     * Creates a new instance of {@code DefaultTickUnitSupplier} with specified multipliers.
     *
     * @param multipliers an array of ascending numbers, with at least one element, from 1 (inclusive) to 10
     *            (exclusive).
     */
    public DefaultTickUnitSupplier(final SortedSet<? extends Number> multipliers) {
        Objects.requireNonNull(multipliers, "The multipliers must not be null");
        if (multipliers.isEmpty()) {
            throw new IllegalArgumentException("The set of multipliers must not be empty");
        }
        DefaultTickUnitSupplier.checkRange(multipliers);

        this.multipliers = multipliers.stream().mapToDouble(Number::doubleValue).toArray();
    }

    private static void checkRange(final SortedSet<? extends Number> multipliers) {
        for (final Number mult : multipliers) {
            if (mult.doubleValue() < 1 || mult.doubleValue() >= DefaultTickUnitSupplier.BASE) {
                throw new IllegalArgumentException("The multiplier values must be in range [1, 10)");
            }
        }
    }

    /**
     * Computes tick unit using the following formula: tickUnit = M*10^E, where M is one of the multipliers specified in
     * the constructor and E is an exponent of 10. Both M and E are selected so that the calculated unit is the smallest
     * (closest to the zero) value that is grater than or equal to the reference tick unit.
     * <p>
     * For example with multipliers [1, 2, 5], the method will give the following results:
     *
     * <pre>
     * computeTickUnit(0.01) returns 0.01
     * computeTickUnit(0.42) returns 0.5
     * computeTickUnit(1.73) returns 2
     * computeTickUnit(5)    returns 5
     * computeTickUnit(27)   returns 50
     * </pre>
     *
     * @param referenceTickUnit the reference tick unit, must be a positive number
     */
    @Override
    public double computeTickUnit(final double referenceTickUnit) {
        // if (referenceTickUnit <= 0) {
        // throw new IllegalArgumentException(
        // "The referenceTickUnit (is: '" + referenceTickUnit + "') must be a positive number");
        // }
        final double localReferenceTickUnit = Math.abs(referenceTickUnit);
        int exp = (int) Math.floor(Math.log10(localReferenceTickUnit));
        final double factor = localReferenceTickUnit / Math.pow(DefaultTickUnitSupplier.BASE, exp);
        double multiplier = 0;

        final int lastIndex = multipliers.length - 1;
        if (factor > multipliers[lastIndex]) {
            exp++;
            multiplier = multipliers[0];
        } else {
            for (int i = lastIndex; i >= 0; i--) {
                if (factor <= multipliers[i]) {
                    multiplier = multipliers[i];
                } else {
                    break;
                }
            }
        }
        return multiplier * Math.pow(DefaultTickUnitSupplier.BASE, exp);
    }
}
