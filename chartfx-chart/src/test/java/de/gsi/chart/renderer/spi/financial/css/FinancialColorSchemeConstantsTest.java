package de.gsi.chart.renderer.spi.financial.css;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FinancialColorSchemeConstantsTest {

    @Test
    void getDefaultColorSchemes() {
        assertEquals(5, FinancialColorSchemeConstants.getDefaultColorSchemes().length);
    }
}