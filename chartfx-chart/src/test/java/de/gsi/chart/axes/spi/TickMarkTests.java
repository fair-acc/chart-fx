package de.gsi.chart.axes.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import org.junit.jupiter.api.Test;

import de.gsi.chart.ui.geometry.Side;

/**
 * @author rstein
 */
public class TickMarkTests {
    @Test
    public void basicTickMarkTests() {
        assertDoesNotThrow(() -> new TickMark(Side.TOP, 0.0, 0.0, 0.0, "label"));
        assertDoesNotThrow(() -> new TickMark(Side.BOTTOM, 0.0, 0.0, 0.0, "label"));
        assertDoesNotThrow(() -> new TickMark(Side.LEFT, 0.0, 0.0, 0.0, "label"));
        assertDoesNotThrow(() -> new TickMark(Side.RIGHT, 0.0, 0.0, 0.0, "label"));
        assertDoesNotThrow(() -> new TickMark(Side.CENTER_HOR, 0.0, 0.0, 0.0, "label"));
        assertDoesNotThrow(() -> new TickMark(Side.CENTER_VER, 0.0, 0.0, 0.0, "label"));

        {
            TickMark tickMark = new TickMark(Side.TOP, 0.0, 0.0, 0.0, "label");
            assertTrue(tickMark.equals(tickMark));
            assertFalse(tickMark.equals(new Object()));
            assertEquals(tickMark.hashCode(), new TickMark(Side.TOP, 0.0, 0.0, 0.0, "label").hashCode());
            assertEquals(tickMark.hashCode(), new TickMark(Side.TOP, 0.0, 0.0, 0.0, null).hashCode());

            assertTrue(tickMark.equals(new TickMark(Side.TOP, 0.0, 0.0, 0.0, "label")));
            assertTrue(tickMark.equals(new TickMark(Side.TOP, 0.0, 0.0, 0.0, "label"))); // side & label are irrelevant here
            assertTrue(tickMark.equals(new TickMark(Side.BOTTOM, 0.0, 0.0, 0.0, "label_irrelevant"))); // side & label are irrelevant here
            assertFalse(tickMark.equals(new TickMark(Side.TOP, 1.0, 0.0, 0.0, "label")));
            assertFalse(tickMark.equals(new TickMark(Side.TOP, 0.0, 1.0, 0.0, "label")));
            assertFalse(tickMark.equals(new TickMark(Side.TOP, 0.0, 0.0, 90.0, "label")));

            assertEquals(0.0, tickMark.getPosition());
            tickMark.setPosition(20.0);
            assertEquals(20.0, tickMark.getPosition());

            assertEquals(0.0, tickMark.getRotation());
            tickMark.setRotation(90);
            assertEquals(90.0, tickMark.getRotation());

            assertEquals(0.0, tickMark.getValue());
            tickMark.setValue(50.0);
            assertEquals(50.0, tickMark.getValue());

            assertDoesNotThrow(() -> tickMark.getHeight());
            assertDoesNotThrow(() -> tickMark.getWidth());
        }
    }

    @Test
    public void tickMarkPositionTests() {
        {
            TickMark markAxisTop = new TickMark(Side.TOP, 0.0, 0.0, 0.0, "label");
            TickMark markAxisBottom = new TickMark(Side.BOTTOM, 0.0, 0.0, 0.0, "label");
            TickMark markAxisLeft = new TickMark(Side.LEFT, 0.0, 0.0, 0.0, "label");
            TickMark markAxisRight = new TickMark(Side.RIGHT, 0.0, 0.0, 0.0, "label");

            assertEquals(Side.TOP, markAxisTop.getSide());
            assertEquals(Side.BOTTOM, markAxisBottom.getSide());
            assertEquals(Side.LEFT, markAxisLeft.getSide());
            assertEquals(Side.RIGHT, markAxisRight.getSide());

            assertEquals(TextAlignment.CENTER, markAxisTop.getTextAlignment());
            assertEquals(VPos.BOTTOM, markAxisTop.getTextOrigin());

            assertEquals(TextAlignment.CENTER, markAxisBottom.getTextAlignment());
            assertEquals(VPos.TOP, markAxisBottom.getTextOrigin());

            assertEquals(TextAlignment.RIGHT, markAxisLeft.getTextAlignment());
            assertEquals(VPos.CENTER, markAxisLeft.getTextOrigin());

            assertEquals(TextAlignment.LEFT, markAxisRight.getTextAlignment());
            assertEquals(VPos.CENTER, markAxisRight.getTextOrigin());
        }

        {
            // labels rotated by 90 degrees
            TickMark markAxisTopRotated = new TickMark(Side.TOP, 0.0, 0.0, 90.0, "label");
            TickMark markAxisBottomRotated = new TickMark(Side.BOTTOM, 0.0, 0.0, 90.0, "label");
            TickMark markAxisLeftRotated = new TickMark(Side.LEFT, 0.0, 0.0, 90.0, "label");
            TickMark markAxisRightRotated = new TickMark(Side.RIGHT, 0.0, 0.0, 90.0, "label");

            assertEquals(TextAlignment.LEFT, markAxisTopRotated.getTextAlignment());
            assertEquals(VPos.CENTER, markAxisTopRotated.getTextOrigin());

            assertEquals(TextAlignment.LEFT, markAxisBottomRotated.getTextAlignment());
            assertEquals(VPos.CENTER, markAxisBottomRotated.getTextOrigin());

            assertEquals(TextAlignment.CENTER, markAxisLeftRotated.getTextAlignment());
            assertEquals(VPos.BOTTOM, markAxisLeftRotated.getTextOrigin());

            assertEquals(TextAlignment.CENTER, markAxisRightRotated.getTextAlignment());
            assertEquals(VPos.TOP, markAxisRightRotated.getTextOrigin());
        }

        {
            // special non 'n x 90 degree' rotation cases for top/bottom
            TickMark markAxisTopRotated = new TickMark(Side.TOP, 0.0, 0.0, 45.0, "label");
            TickMark markAxisBottomRotated = new TickMark(Side.BOTTOM, 0.0, 0.0, 45.0, "label");
            TickMark markAxisLeftRotated = new TickMark(Side.LEFT, 0.0, 0.0, 45.0, "label");
            TickMark markAxisRightRotated = new TickMark(Side.RIGHT, 0.0, 0.0, 45.0, "label");

            assertEquals(TextAlignment.LEFT, markAxisTopRotated.getTextAlignment());
            assertEquals(VPos.BOTTOM, markAxisTopRotated.getTextOrigin());

            assertEquals(TextAlignment.LEFT, markAxisBottomRotated.getTextAlignment());
            assertEquals(VPos.TOP, markAxisBottomRotated.getTextOrigin());

            // should be equal to 90 degree case
            assertEquals(TextAlignment.RIGHT, markAxisLeftRotated.getTextAlignment());
            assertEquals(VPos.CENTER, markAxisLeftRotated.getTextOrigin());

            assertEquals(TextAlignment.LEFT, markAxisRightRotated.getTextAlignment());
            assertEquals(VPos.CENTER, markAxisRightRotated.getTextOrigin());
        }
    }

    @Test
    public void tickMarkPropertyTests() {
        TickMark tickMark = new TickMark(Side.TOP, 0.0, 0.0, 0.0, "label");

        assertDoesNotThrow(() -> tickMark.fontProperty().set(Font.font(20)));
        assertDoesNotThrow(() -> tickMark.fillProperty().set(Color.BLUE));
        assertDoesNotThrow(() -> tickMark.visibleProperty().set(false));
    }
}
