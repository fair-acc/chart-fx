package io.fair_acc.chartfx.ui.css;

import io.fair_acc.dataset.utils.DataSetStyleBuilder;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ennerf
 */
class DataSetStyleParserTest {

    final DataSetStyleBuilder builder = DataSetStyleBuilder.newInstance();
    final DataSetStyleParser parser = DataSetStyleParser.newInstance();

    @Test
    void testStyleParserAuto() {
        String style = builder.reset().build();
        assertEquals("", style);
        assertFalse(parser.tryParse(style));
        assertFalse(parser.getStrokeColor().isPresent());


        style = builder.reset().setStroke("red").build();
        assertEquals("-fx-stroke: red;", style);
        assertTrue(parser.tryParse(style));
        assertEquals(Color.RED, parser.getStrokeColor().orElseThrow());

        style = builder.reset().setFill("blue").build();
        assertEquals("-fx-fill: blue;", style);
        assertTrue(parser.tryParse(style));
        assertEquals(Color.BLUE, parser.getFillColor().orElseThrow());

        style = builder.reset().setFill(255,255,0, 1).build();
        assertEquals("-fx-fill: rgba(255,255,0,1.0);", style);
        assertTrue(parser.tryParse(style));
        assertEquals(Color.YELLOW, parser.getFillColor().orElseThrow());

        style = builder.reset().setStrokeWidth(1.3).build();
        assertEquals("-fx-stroke-width: 1.3;", style);
        assertTrue(parser.tryParse(style));
        assertEquals(1.3, parser.getLineWidth().orElseThrow());

        style = builder.reset().setIntensity(2.3).build();
        assertEquals("-fx-intensity: 2.3;", style);
        assertTrue(parser.tryParse(style));
        assertEquals(2.3, parser.getIntensity().orElseThrow());

        style = builder.reset().setFontWeight("bold").setFont("System").setVisible(true).build();
        assertEquals("visibility: visible;\n" +
                "-fx-font: System;\n" +
                "-fx-font-weight: bold;", style);
        assertTrue(parser.tryParse(style));
        assertEquals(true, parser.getVisible().orElseThrow());
        assertEquals("System", parser.getFont().orElseThrow().getFamily());
        assertEquals(FontWeight.BOLD, parser.getFontWeight().orElseThrow());

        style = builder.reset()
                .setStrokeWidth(3)
                .setFont("Serif")
                .setFontSize(20)
                .setFontItalic(true)
                .setFontWeight("bold")
                .setStrokeWidth(3)
                .setFont("monospace")
                .setFontItalic(true)
                .setStroke("0xEE00EE")
                .setFill("0xEE00EE")
                .setStrokeDashPattern(5, 8, 5, 16)
                .setFill("blue")
                .setStrokeDashPattern(3, 5, 8, 5)
                .build();
        assertTrue(parser.tryParse(style));
        assertArrayEquals(new double[]{3, 5, 8, 5}, parser.getLineDashPattern().orElseThrow());
    }

}