package de.gsi.chart.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test StyleParser
 * 
 * @author Alexander Krimm
 * @author rstein
 */
class StyleParserTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(StyleParserTest.class);

    @Test
    @DisplayName("Test parsing styles")
    public void testStyleParser() {
        final String testStyle = " color1 = blue; stroke= 0; bool1=true; color2 = rgb(255,0,0); unclean=\"a\'; index1=2;index2=0xFE; "
                                 + "float1=10e7; float2=10.333; malformedInt= 0.aG; emptyProperty=;invalidColor=darthRed#22;";

        assertEquals(null, StyleParser.getPropertyValue(testStyle, null));
        assertEquals(null, StyleParser.getPropertyValue(null, "color1"));
        assertEquals(0, StyleParser.getIntegerPropertyValue(testStyle, "stroke"));
        assertEquals(null, StyleParser.getIntegerPropertyValue(testStyle, null));
        assertEquals(null, StyleParser.getIntegerPropertyValue(null, "stroke"));
        assertEquals(null, StyleParser.getIntegerPropertyValue(testStyle, "malformedInt"));

        assertEquals(0, StyleParser.getFloatingDecimalPropertyValue(testStyle, "stroke"));
        assertEquals(null, StyleParser.getFloatingDecimalPropertyValue(testStyle, null));
        assertEquals(null, StyleParser.getFloatingDecimalPropertyValue(null, "stroke"));
        assertEquals(null, StyleParser.getFloatingDecimalPropertyValue(testStyle, "malformedInt"));

        assertEquals(null, StyleParser.getFloatingDecimalPropertyValue(testStyle, "emptyProperty"));

        assertArrayEquals(new double[] { 0 }, StyleParser.getFloatingDecimalArrayPropertyValue(testStyle, "stroke"));
        assertEquals(null, StyleParser.getFloatingDecimalArrayPropertyValue(testStyle, null));
        assertEquals(null, StyleParser.getFloatingDecimalArrayPropertyValue(null, "stroke"));
        assertEquals(null, StyleParser.getFloatingDecimalArrayPropertyValue(testStyle, "malformedInt"));

        Map<String, String> emptyMap = StyleParser.splitIntoMap(null);
        assertTrue(emptyMap.isEmpty());
        StyleParser.splitIntoMap("=2");
        emptyMap.put("property1", "value");
        assertEquals("property1=value;", StyleParser.mapToString(emptyMap));

        assertEquals("blue", StyleParser.getPropertyValue(testStyle, "color1"));

        assertEquals(Color.web("red"), StyleParser.getColorPropertyValue(testStyle, "color2"));
        assertEquals(null, StyleParser.getColorPropertyValue(testStyle, null));
        assertEquals(null, StyleParser.getColorPropertyValue(null, "color2"));
        assertEquals(null, StyleParser.getColorPropertyValue(null, "invalidColor"));

        assertEquals(2, StyleParser.getIntegerPropertyValue(testStyle, "index1"));
        assertEquals(0xFE, StyleParser.getIntegerPropertyValue(testStyle, "index2"));
        assertEquals(10e7, StyleParser.getFloatingDecimalPropertyValue(testStyle, "float1"));
        assertEquals(10.333, StyleParser.getFloatingDecimalPropertyValue(testStyle, "float2"));

        assertEquals(true, StyleParser.getBooleanPropertyValue(testStyle, "bool1"));
        assertEquals(null, StyleParser.getBooleanPropertyValue(testStyle, null));
        assertEquals(null, StyleParser.getBooleanPropertyValue(null, "bool1"));
        assertEquals(false, StyleParser.getBooleanPropertyValue(testStyle, "malformedInt"));

        assertArrayEquals(new double[] { 0 }, StyleParser.getStrokeDashPropertyValue(testStyle, "stroke"));
        assertArrayEquals(null, StyleParser.getStrokeDashPropertyValue(testStyle, null));
        assertArrayEquals(null, StyleParser.getStrokeDashPropertyValue(null, "stroke"));
        assertArrayEquals(null, StyleParser.getStrokeDashPropertyValue(testStyle, "malformedInt"));

        final String fontTestStyle1 = "font=Helvetica; fontWeight=bold; fontSize=18; fontPosture = italic;";
        final String fontTestStyle2 = "font=; fontWeight=bold; fontSize=18; fontPosture = italic;";
        assertEquals(Font.font("Helvetia", 18.0), StyleParser.getFontPropertyValue(null));
        assertEquals(Font.font("system", FontWeight.BOLD, FontPosture.ITALIC, 18),
                StyleParser.getFontPropertyValue(fontTestStyle1));
        assertEquals(Font.font("system", FontWeight.BOLD, FontPosture.ITALIC, 18),
                StyleParser.getFontPropertyValue(fontTestStyle2));
    }
}
