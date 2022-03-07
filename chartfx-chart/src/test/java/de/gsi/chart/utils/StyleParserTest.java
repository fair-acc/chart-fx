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
        final String testStyle = " color1 = blue; stroke= 0; bool1=true; color2 = rgb(255,0,0); unclean=\"a'; index1=2;index2=0xFE; "
                               + "float1=10e7; float2=10.333; malformedInt= 0.aG; emptyProperty=;invalidColor=darthRed#22;";

        assertEquals(true, StyleParser.getBooleanPropertyValue("booleanProperty=true", "booleanProperty"));
        assertEquals(false, StyleParser.getBooleanPropertyValue("booleanProperty=false", "booleanProperty"));
        assertNull(StyleParser.getBooleanPropertyValue("booleanProperty=false", null));
        assertNull(StyleParser.getBooleanPropertyValue(null, "booleanProperty"));
        assertNull(StyleParser.getBooleanPropertyValue("booleanProperty2=true", "booleanProperty"));
        assertEquals(false, StyleParser.getBooleanPropertyValue("booleanProperty=0", "booleanProperty"));
        assertEquals(false, StyleParser.getBooleanPropertyValue("booleanProperty=1", "booleanProperty"));

        assertNull(StyleParser.getPropertyValue(testStyle, null));
        assertNull(StyleParser.getPropertyValue(null, "color1"));
        assertEquals("defaultColor1", StyleParser.getPropertyValue(null, "color1", "defaultColor1"));
        assertEquals("blue", StyleParser.getPropertyValue(testStyle, "color1", "defaultColor1"));
        assertEquals("blue", StyleParser.getPropertyValue(testStyle, "color1", null));
        assertEquals("defaultColor1", StyleParser.getPropertyValue(testStyle, "colorDef", "defaultColor1"));
        assertNull(StyleParser.getPropertyValue(null, "color1", null));
        assertNull(StyleParser.getPropertyValue(testStyle, "booleanProperty", null));
        assertEquals(0, StyleParser.getIntegerPropertyValue(testStyle, "stroke"));
        assertNull(StyleParser.getIntegerPropertyValue(testStyle, null));
        assertNull(StyleParser.getIntegerPropertyValue(null, "stroke"));
        assertNull(StyleParser.getIntegerPropertyValue(testStyle, "malformedInt"));
        assertEquals(2, StyleParser.getIntegerPropertyValue("intStyle=2", "intStyle"));
        assertNull(StyleParser.getIntegerPropertyValue("intStyle=2", "intStyle2"));

        assertEquals(0, StyleParser.getFloatingDecimalPropertyValue(testStyle, "stroke"));
        assertNull(StyleParser.getFloatingDecimalPropertyValue(testStyle, null));
        assertNull(StyleParser.getFloatingDecimalPropertyValue(null, "stroke"));
        assertNull(StyleParser.getFloatingDecimalPropertyValue(testStyle, "malformedInt"));

        assertNull(StyleParser.getFloatingDecimalPropertyValue(testStyle, "emptyProperty"));

        assertArrayEquals(new double[] { 0 }, StyleParser.getFloatingDecimalArrayPropertyValue(testStyle, "stroke"));
        assertNull(StyleParser.getFloatingDecimalArrayPropertyValue(testStyle, null));
        assertArrayEquals(new double[] { 0.1 }, StyleParser.getFloatingDecimalArrayPropertyValue("floatingPointArray=0.1", "floatingPointArray"));
        assertNull(StyleParser.getFloatingDecimalArrayPropertyValue("floatingPointArray=0.1", "floatingPointArray2"));
        assertNull(StyleParser.getFloatingDecimalArrayPropertyValue("floatingPointArray=", "floatingPointArray"));
        assertArrayEquals(new double[] { 0.1, 0.2 }, StyleParser.getFloatingDecimalArrayPropertyValue("floatingPointArray=0.1,0.2", "floatingPointArray"));
        assertNull(StyleParser.getFloatingDecimalArrayPropertyValue(null, "stroke"));
        assertNull(StyleParser.getFloatingDecimalArrayPropertyValue(testStyle, "malformedInt"));

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
        assertNull(StyleParser.getColorPropertyValue("color=red", "color2"));
        assertNull(StyleParser.getColorPropertyValue("color=reddish", "color"));
        assertNull(StyleParser.getColorPropertyValue(testStyle, null));
        assertNull(StyleParser.getColorPropertyValue(null, "color2"));
        assertNull(StyleParser.getColorPropertyValue(null, "invalidColor"));

        assertEquals(2, StyleParser.getIntegerPropertyValue(testStyle, "index1"));
        assertEquals(0xFE, StyleParser.getIntegerPropertyValue(testStyle, "index2"));
        assertEquals(10e7, StyleParser.getFloatingDecimalPropertyValue(testStyle, "float1"), 1e-5);
        assertEquals(10.333, StyleParser.getFloatingDecimalPropertyValue(testStyle, "float2"), 1e-5);
        assertEquals(0.1, StyleParser.getFloatingDecimalPropertyValue("float1=0.1", "float1"), 1e-5);
        assertNull(StyleParser.getFloatingDecimalPropertyValue("float1=0.1", "float2"));
        assertEquals(11.0, StyleParser.getFloatingDecimalPropertyValue("float1=0.1", "float2", 11.0), 1e-5);
        assertEquals(0.1, StyleParser.getFloatingDecimalPropertyValue("float1=0.1", "float1", 11.0), 1e-5);

        assertTrue(StyleParser.getBooleanPropertyValue(testStyle, "bool1"));
        assertNull(StyleParser.getBooleanPropertyValue(testStyle, null));
        assertNull(StyleParser.getBooleanPropertyValue(null, "bool1"));
        assertFalse(StyleParser.getBooleanPropertyValue(testStyle, "malformedInt"));

        assertArrayEquals(new double[] { 0 }, StyleParser.getStrokeDashPropertyValue(testStyle, "stroke"));
        assertNull(StyleParser.getStrokeDashPropertyValue(testStyle, null));
        assertNull(StyleParser.getStrokeDashPropertyValue(null, "stroke"));
        assertNull(StyleParser.getStrokeDashPropertyValue(testStyle, "malformedInt"));
        assertNull(StyleParser.getStrokeDashPropertyValue("stroke=", "stroke2"));

        assertEquals(Font.font("Helvetica", 18.0), StyleParser.getFontPropertyValue(null));
        assertEquals(Font.font("Helvetica", 18.0), StyleParser.getFontPropertyValue(""));
        assertEquals(
            Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 20),
            StyleParser.getFontPropertyValue("font=Arial; fontWeight=bold; fontSize=20; fontPosture = italic;"));
        assertNotNull(StyleParser.getFontPropertyValue("font="));
        assertNotNull(StyleParser.getFontPropertyValue("font=Helvetica"));
        assertNotNull(StyleParser.getFontPropertyValue("font2=Helvetica"));
    }
}
