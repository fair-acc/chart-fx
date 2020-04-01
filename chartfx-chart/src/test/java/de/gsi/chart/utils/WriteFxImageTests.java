package de.gsi.chart.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationExtension;

import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;
import de.gsi.chart.utils.WriteFxImage;

/**
 * Tests for {@link de.gsi.chart.ui.utils.WriteFxImage}.
 *
 * @author Alexander Krimm
 */

@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class WriteFxImageTests {
    @TestFx // needs to be on the application thread for the snapshot method
    public void testWritingImageToFile(@TempDir File tempDir) throws IOException {
        File tmpfile = new File(tempDir.getPath() + "test.png");
        int w = 200;
        int h = 300;
        // generate test image
        Canvas originalCanvas = new Canvas(w, h);
        GraphicsContext originalContext = originalCanvas.getGraphicsContext2D();
        originalContext.setStroke(Color.BLUE);
        originalContext.strokeOval(20, 30, 40, 50);
        originalContext.setStroke(Color.RED);
        originalContext.strokeOval(30, 40, 50, 60);
        originalContext.setStroke(Color.GREEN);
        originalContext.strokeOval(40, 50, 60, 70);
        originalContext.setStroke(Color.ORANGE);
        originalContext.strokeRect(0, 0, w, h);
        Image original = originalCanvas.snapshot(null, null);
        // convert to png
        WriteFxImage.savePng(original, tmpfile);
        // load from png
        try (InputStream is = new FileInputStream(tmpfile.getPath())) {
            Image recovered = new Image(is);
            // compare against original
            assertEquals(original.getWidth(), recovered.getWidth());
            assertEquals(original.getHeight(), recovered.getHeight());
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    assertEquals(original.getPixelReader().getArgb(x, y), recovered.getPixelReader().getArgb(x, y));
                }
            }
        }
    }

    @TestFx // needs to be on the application thread for the snapshot method
    public void testWritingImageByteBuffer() throws IOException {
        int w = 200;
        int h = 300;
        final ByteBuffer pngOutput = ByteBuffer.allocate(w * h * 3 + 100);
        // generate test image
        final Canvas originalCanvas = new Canvas(w, h);
        final GraphicsContext originalContext = originalCanvas.getGraphicsContext2D();
        originalContext.setStroke(Color.BLUE);
        originalContext.strokeOval(20, 30, 40, 50);
        originalContext.setStroke(Color.RED);
        originalContext.strokeOval(30, 40, 50, 60);
        originalContext.setStroke(Color.GREEN);
        originalContext.strokeOval(40, 50, 60, 70);
        originalContext.setStroke(Color.ORANGE);
        originalContext.strokeRect(0, 0, w, h);
        final Image original = originalCanvas.snapshot(null, null);
        // convert to png
        final ByteBuffer pngOutReal = WriteFxImage.encode(original, pngOutput);
        // assert that the provided buffer was used
        assertSame(pngOutput, pngOutReal);
        // load from png
        try (final InputStream is = new ByteArrayInputStream(pngOutput.array(), pngOutput.position(), pngOutput.limit())) {
            final Image recovered = new Image(is);
            // compare against original
            assertEquals(original.getWidth(), recovered.getWidth());
            assertEquals(original.getHeight(), recovered.getHeight());
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    assertEquals(original.getPixelReader().getArgb(x, y), recovered.getPixelReader().getArgb(x, y));
                }
            }
        }
    }
}
