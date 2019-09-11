package de.gsi.chart.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.paint.Color;

/**
 * Test StyleParser
 * 
 * @author Alexander Krimm
 */
class StyleParserTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(StyleParserTest.class);
    
    @Test
    @DisplayName("Test parsing styles")
    public void testStyleParser() {
        final String testStyle = " color1 = blue; stroke= 0; bool1=true; color2 = rgb(255,0,0); unclean=\"a\'; index1=2;index2=0xFE; float1=10e7; float2=10.333";
        
        assertEquals("blue", StyleParser.getPropertyValue(testStyle, "color1"));
        assertEquals(Color.web("red"), StyleParser.getColorPropertyValue(testStyle, "color2"));
        assertEquals(2, StyleParser.getIntegerPropertyValue(testStyle, "index1"));
        assertEquals(0xFE, StyleParser.getIntegerPropertyValue(testStyle, "index2"));
        assertEquals(10e7, StyleParser.getFloatingDecimalPropertyValue(testStyle, "float1"));
        assertEquals(10.333, StyleParser.getFloatingDecimalPropertyValue(testStyle, "float2"));
        assertEquals(true, StyleParser.getBooleanPropertyValue(testStyle, "bool1"));
    }

}
