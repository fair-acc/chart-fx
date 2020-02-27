package de.gsi.chart.axes.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Basic interface tests for MetricPrefix enum
 *
 * @author rstein
 */
public class MetricPrefixTests {
    @Test
    public void basicTests() {
        for (MetricPrefix prefix : MetricPrefix.values()) {
            assertNotNull(prefix.getLongPrefix());
            assertNotNull(prefix.getShortPrefix());

            // assert that power is unique
            final double power = prefix.getPower();
            for (MetricPrefix prefix2 : MetricPrefix.values()) {
                if (prefix.equals(prefix2)) {
                    // skip identity
                    continue;
                }
                final double power2 = prefix2.getPower();
                assertNotEquals(power, power2, "power non-equality not confirmed for " //
                                                       + prefix + "(" + power + ") vs. " //
                                                       + prefix2 + "(" + power2 + ")");
            }

            assertEquals(prefix.getLongPrefix(), MetricPrefix.getLongPrefix(power));
            assertEquals(prefix.getShortPrefix(), MetricPrefix.getShortPrefix(power));

            // nearest match
            assertEquals(prefix, MetricPrefix.getNearestMatch(1.01 * power));
        }

        // nearest match
        assertEquals(MetricPrefix.NONE, MetricPrefix.getNearestMatch(0));

        // prefix for non canonical powers
        assertEquals("*1.3", MetricPrefix.getLongPrefix(1.3));
        assertEquals("*1.5", MetricPrefix.getShortPrefix(1.5));
    }
}
