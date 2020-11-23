package de.gsi.chart.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test StyleParser
 * 
 * @author Alexander Krimm
 * @author rstein
 */
class StyleParserTest {
    @Test
    @DisplayName("Test parsing styles")
    public void testStyleParser() {
        final String testStyle = " color1 = blue; stroke= 0; bool1=true; color2 = rgb(255,0,0); unclean=\"a\'; index1=2;index2=0xFE; "
                                 + "float1=10e7; float2=10.333; malformedInt= 0.aG; emptyProperty=;invalidColor=darthRed#22;";

        assertEquals(true, StyleParser.getBooleanPropertyValue("booleanProperty=true", "booleanProperty"));
        assertEquals(false, StyleParser.getBooleanPropertyValue("booleanProperty=false", "booleanProperty"));
        assertEquals(null, StyleParser.getBooleanPropertyValue("booleanProperty=false", null));
        assertEquals(null, StyleParser.getBooleanPropertyValue(null, "booleanProperty"));
        assertEquals(null, StyleParser.getBooleanPropertyValue("booleanProperty2=true", "booleanProperty"));
        assertEquals(false, StyleParser.getBooleanPropertyValue("booleanProperty=0", "booleanProperty"));
        assertEquals(false, StyleParser.getBooleanPropertyValue("booleanProperty=1", "booleanProperty"));

        assertEquals(null, StyleParser.getPropertyValue(testStyle, null));
        assertEquals(null, StyleParser.getPropertyValue(null, "color1"));
        assertEquals(0, StyleParser.getIntegerPropertyValue(testStyle, "stroke"));
        assertEquals(null, StyleParser.getIntegerPropertyValue(testStyle, null));
        assertEquals(null, StyleParser.getIntegerPropertyValue(null, "stroke"));
        assertEquals(null, StyleParser.getIntegerPropertyValue(testStyle, "malformedInt"));
        assertEquals(2, StyleParser.getIntegerPropertyValue("intStyle=2", "intStyle"));
        assertEquals(null, StyleParser.getIntegerPropertyValue("intStyle=2", "intStyle2"));

        assertEquals(0, StyleParser.getFloatingDecimalPropertyValue(testStyle, "stroke"));
        assertEquals(null, StyleParser.getFloatingDecimalPropertyValue(testStyle, null));
        assertEquals(null, StyleParser.getFloatingDecimalPropertyValue(null, "stroke"));
        assertEquals(null, StyleParser.getFloatingDecimalPropertyValue(testStyle, "malformedInt"));

        assertEquals(null, StyleParser.getFloatingDecimalPropertyValue(testStyle, "emptyProperty"));

        assertArrayEquals(new double[] { 0 }, StyleParser.getFloatingDecimalArrayPropertyValue(testStyle, "stroke"));
        assertEquals(null, StyleParser.getFloatingDecimalArrayPropertyValue(testStyle, null));
        assertArrayEquals(new double[] { 0.1 }, StyleParser.getFloatingDecimalArrayPropertyValue("floatingPointArray=0.1", "floatingPointArray"));
        assertEquals(null, StyleParser.getFloatingDecimalArrayPropertyValue("floatingPointArray=0.1", "floatingPointArray2"));
        assertEquals(null, StyleParser.getFloatingDecimalArrayPropertyValue("floatingPointArray=", "floatingPointArray"));
        assertArrayEquals(new double[] { 0.1, 0.2 }, StyleParser.getFloatingDecimalArrayPropertyValue("floatingPointArray=0.1,0.2", "floatingPointArray"));
        assertEquals(null, StyleParser.getFloatingDecimalArrayPropertyValue(null, "stroke"));
        assertEquals(null, StyleParser.getFloatingDecimalArrayPropertyValue(testStyle, "malformedInt"));

        final Map<String, String> emptyMap = StyleParser.splitIntoMap(null);
        assertTrue(emptyMap.isEmpty());
        StyleParser.splitIntoMap("=2");
        emptyMap.put("property1", "value");
        assertEquals("property1=value;", StyleParser.mapToString(emptyMap));
        assertNotNull(StyleParser.splitIntoMap(""));

        assertEquals("blue", StyleParser.getPropertyValue(testStyle, "color1"));

        assertEquals(Color.web("red"), StyleParser.getColorPropertyValue(testStyle, "color2"));
        assertEquals(Color.web("red"), StyleParser.getColorPropertyValue("color=red", "color"));
        assertEquals(Color.web("black"), StyleParser.getColorPropertyValue("color=black", "borderColor", Color.web("black")));
        assertEquals(Color.web("black"), StyleParser.getColorPropertyValue("color=black", "color", Color.web("white")));
        assertEquals(null, StyleParser.getColorPropertyValue("color=red", "color2"));
        assertEquals(null, StyleParser.getColorPropertyValue("color=reddish", "color"));
        assertEquals(null, StyleParser.getColorPropertyValue(testStyle, null));
        assertEquals(null, StyleParser.getColorPropertyValue(null, "color2"));
        assertEquals(null, StyleParser.getColorPropertyValue(null, "invalidColor"));

        assertEquals(2, StyleParser.getIntegerPropertyValue(testStyle, "index1"));
        assertEquals(0xFE, StyleParser.getIntegerPropertyValue(testStyle, "index2"));
        assertEquals(10e7, StyleParser.getFloatingDecimalPropertyValue(testStyle, "float1"), 1e-5);
        assertEquals(10.333, StyleParser.getFloatingDecimalPropertyValue(testStyle, "float2"), 1e-5);
        assertEquals(0.1, StyleParser.getFloatingDecimalPropertyValue("float1=0.1", "float1"), 1e-5);
        assertEquals(null, StyleParser.getFloatingDecimalPropertyValue("float1=0.1", "float2"));
        assertEquals(11.0, StyleParser.getFloatingDecimalPropertyValue("float1=0.1", "float2", 11.0), 1e-5);
        assertEquals(0.1, StyleParser.getFloatingDecimalPropertyValue("float1=0.1", "float1", 11.0), 1e-5);

        assertEquals(true, StyleParser.getBooleanPropertyValue(testStyle, "bool1"));
        assertEquals(null, StyleParser.getBooleanPropertyValue(testStyle, null));
        assertEquals(null, StyleParser.getBooleanPropertyValue(null, "bool1"));
        assertEquals(false, StyleParser.getBooleanPropertyValue(testStyle, "malformedInt"));

        assertArrayEquals(new double[] { 0 }, StyleParser.getStrokeDashPropertyValue(testStyle, "stroke"));
        assertArrayEquals(null, StyleParser.getStrokeDashPropertyValue(testStyle, null));
        assertArrayEquals(null, StyleParser.getStrokeDashPropertyValue(null, "stroke"));
        assertArrayEquals(null, StyleParser.getStrokeDashPropertyValue(testStyle, "malformedInt"));
        assertArrayEquals(null, StyleParser.getStrokeDashPropertyValue("stroke=", "stroke2"));

        final String fontTestStyle1 = "font=Helvetica; fontWeight=bold; fontSize=18; fontPosture = italic;";
        final String fontTestStyle2 = "font=; fontWeight=bold; fontSize=18; fontPosture = italic;";
        assertEquals(Font.font("Helvetia", 18.0), StyleParser.getFontPropertyValue(null));
        assertEquals(Font.font("system", FontWeight.BOLD, FontPosture.ITALIC, 18), StyleParser.getFontPropertyValue(fontTestStyle1));
        assertEquals(Font.font("system", FontWeight.BOLD, FontPosture.ITALIC, 18), StyleParser.getFontPropertyValue(fontTestStyle2));
        assertNotNull(StyleParser.getFontPropertyValue("font=Helvetica"));
        assertNotNull(StyleParser.getFontPropertyValue("font2=Helvetica"));
    }
}
