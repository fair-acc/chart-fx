package io.fair_acc.chartfx.renderer.spi.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;

import org.junit.jupiter.api.Test;

public class ColorGradientTests {
    @Test
    public void testColorGradientApi() {
        assertDoesNotThrow(() -> new ColorGradient(new ArrayList<>()));
        assertDoesNotThrow(() -> new ColorGradient((Stop) null, null));
        assertDoesNotThrow(() -> new ColorGradient("myGradient", new ArrayList<>()));
        assertDoesNotThrow(() -> new ColorGradient("myGradient", null, null));

        final List<ColorGradient> gradients = new ArrayList<>(ColorGradient.colorGradients());
        gradients.add(new ColorGradient("myGradient1", new ArrayList<>()));
        gradients.add(new ColorGradient("myGradient2", new ArrayList<>()));
        for (ColorGradient gradient : gradients) {
            assertNotNull(gradient, "gradient not null");

            final Color colorRef = gradient.getColor(0.5);
            assertEquals(colorRef, gradient.getColor(0.5), "Color caching");

            final int[] colorBytesRef = gradient.getColorBytes(0.5);
            assertArrayEquals(colorBytesRef, gradient.getColorBytes(0.5), "Color caching int[]");

            assertEquals(Color.TRANSPARENT, gradient.getColor(-0.1), " color below range ");
            assertEquals(Color.TRANSPARENT, gradient.getColor(+1.1), " color above range ");

            final int[] transparentColorBytes = new int[] { 0, 0, 0, 0 };
            assertArrayEquals(transparentColorBytes, gradient.getColorBytes(-0.1), " color bytes below range ");
            assertArrayEquals(transparentColorBytes, gradient.getColorBytes(+1.1), " color bytes above range ");

            assertNotNull(gradient.toString(), "gradient name");
        }
    }
}
