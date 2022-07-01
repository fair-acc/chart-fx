package io.fair_acc.chartfx.renderer.spi.financial.css;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FinancialColorSchemeConstantsTest {
    @Test
    void getDefaultColorSchemes() {
        assertEquals(5, FinancialColorSchemeConstants.getDefaultColorSchemes().length);
    }
}
