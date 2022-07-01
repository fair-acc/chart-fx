package io.fair_acc.chartfx.axes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author rstein
 */
public class AxisModeTests {
    @Test
    public void axisModeTests() {
        assertTrue(AxisMode.X.allowsX());
        assertFalse(AxisMode.X.allowsY());

        assertFalse(AxisMode.Y.allowsX());
        assertTrue(AxisMode.Y.allowsY());

        assertTrue(AxisMode.XY.allowsX());
        assertTrue(AxisMode.XY.allowsY());
    }
}
