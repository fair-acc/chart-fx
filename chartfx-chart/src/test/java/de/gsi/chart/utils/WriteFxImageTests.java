package de.gsi.chart.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.Deflater;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.dataset.utils.ArrayCache;

/**
 * Tests for {@link de.gsi.chart.utils.WriteFxImage}.
 *
 * @author Alexander Krimm
 */

@ExtendWith(ApplicationExtension.class)
public class WriteFxImageTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteFxImageBenchmark.class);
    private static final String INTERNAL_ARRAY_CACHE_NAME = "WriteFxImage-internalArray";
    private Image imageOvals;
    private Image imageRandom;
    private Image image1x1;

    @Start
    public void setUp(@SuppressWarnings("unused") final Stage stage) {
        int w = 200;
        int h = 300;
        // generate test image with simple shapes
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
        imageOvals = originalCanvas.snapshot(null, null);

        //generate test image with noise data (not very compressible)
        final Canvas noiseCanvas = new Canvas(600, 333);
        final GraphicsContext noiseContext = noiseCanvas.getGraphicsContext2D();
        byte[] randomArray = new byte[600 * 333 * 4];
        new Random().nextBytes(randomArray);
        noiseContext.getPixelWriter().setPixels(0, 0, 600, 333, PixelFormat.getByteBgraInstance(), randomArray, 0, 600);
        imageRandom = noiseCanvas.snapshot(null, null);

        // generate test image with minimal dimensions
        final Canvas onexoneCanvas = new Canvas(1, 1);
        final GraphicsContext onexoneContext = onexoneCanvas.getGraphicsContext2D();
        onexoneContext.getPixelWriter().setArgb(0, 0, 0xdeadbeef);
        image1x1 = onexoneCanvas.snapshot(null, null);
    }

    @Test
    public void testWritingImageToFile(@TempDir File tempDir) throws IOException {
        File tmpfile = new File(tempDir.getPath() + "test.png");
        // convert to png
        WriteFxImage.savePng(imageOvals, tmpfile);
        // load from png
        try (InputStream is = new FileInputStream(tmpfile.getPath())) {
            Image recovered = new Image(is);
            // compare against original
            assertImageEqual(imageOvals, recovered);
        }
    }

    @Test
    public void testWritingImageByteBuffer() throws IOException {
        // image metadata
        int w = (int) imageOvals.getWidth();
        int h = (int) imageOvals.getHeight();
        // Initialize cached array so implementation cannot rely on zero initialization
        final int rawDataSize = w * h * 4 + h; // image dimensions times bytesPerPixel + line filtering flag
        final byte[] byteArray = ArrayCache.getCachedByteArray(INTERNAL_ARRAY_CACHE_NAME, rawDataSize);
        final byte fillByte = (byte) 0x03; // important to use a valid line filtering value (1-4) here, other values will be ignored
        Arrays.fill(byteArray, fillByte);
        ArrayCache.release(INTERNAL_ARRAY_CACHE_NAME, byteArray); // allow WriteFxImage to use the buffer
        // convert to png
        final ByteBuffer pngOutput = ByteBuffer.allocate(w * h * 4 + 100);
        final Map<String, Object> metaInfo = new HashMap<>();
        final ByteBuffer pngOutReal = WriteFxImage.encode(imageOvals, pngOutput, true, Deflater.BEST_SPEED, metaInfo);
        LOGGER.atDebug() //
                .addArgument(metaInfo.get("width")) //
                .addArgument(metaInfo.get("height")) //
                .addArgument(metaInfo.get("colorMode")) //
                .addArgument(metaInfo.get("compressionLevel")) //
                .addArgument(metaInfo.get("bufferSize")) //
                .addArgument(metaInfo.get("outputSize")) //
                .addArgument(metaInfo.get("compression")) //
                .addArgument(metaInfo.get("outputSizeBound")) //
                .log("Compressed Image ({}x{} {}) using compression level {} to image buffer (cap: {}, size: {}). compression rate: {}, outputSizeUpperBound: {}");
        // assert that the provided buffer was used
        assertSame(pngOutput, pngOutReal);
        assertEquals(w, metaInfo.get("width"));
        assertEquals(h, metaInfo.get("height"));
        assertEquals("rgba", metaInfo.get("colorMode"));
        assertEquals(w * h * 4 + 100, (Integer) metaInfo.get("bufferSize"));
        assertEquals(274220, metaInfo.get("outputSizeBound"));
        assertEquals(1, metaInfo.get("compressionLevel"));
        assertTrue(w * h * 4 > (int) metaInfo.get("outputSize"));
        assertEquals(0.02, (double) metaInfo.get("compression"), 0.02);

        // Check that the array we preinitialized was actually used
        assertNotEquals(fillByte, byteArray[15]);

        // load from png
        try (final InputStream is = new ByteArrayInputStream(pngOutput.array(), pngOutput.position(), pngOutput.limit())) {
            final Image recovered = new Image(is);
            // compare against original
            assertImageEqual(imageOvals, recovered);
        }
    }

    @Test
    public void testWritingImageByteBufferRandomRbgNoCompression() throws IOException {
        // convert to png
        int w = (int) imageOvals.getWidth();
        int h = (int) imageOvals.getHeight();
        final ByteBuffer pngOutput = ByteBuffer.allocate(w * h * 3 + 1000);
        final Map<String, Object> metaInfo = new HashMap<>();
        final ByteBuffer pngOutReal = WriteFxImage.encode(imageOvals, pngOutput, false, Deflater.NO_COMPRESSION,
                metaInfo);
        LOGGER.atDebug() //
                .addArgument(metaInfo.get("width")) //
                .addArgument(metaInfo.get("height")) //
                .addArgument(metaInfo.get("colorMode")) //
                .addArgument(metaInfo.get("compressionLevel")) //
                .addArgument(metaInfo.get("bufferSize")) //
                .addArgument(metaInfo.get("outputSize")) //
                .addArgument(metaInfo.get("compression")) //
                .addArgument(metaInfo.get("outputSizeBound")) //
                .log("Compressed Image ({}x{} {}) using compression level {} to image buffer (cap: {}, size: {}). compression rate: {}, outputSizeUpperBound: {}");
        // assert that the provided buffer was used
        assertSame(pngOutput, pngOutReal);
        assertEquals(w, metaInfo.get("width"));
        assertEquals(h, metaInfo.get("height"));
        assertEquals("rgb", metaInfo.get("colorMode"));
        assertEquals(w * h * 3 + 1000, (Integer) metaInfo.get("bufferSize"));
        assertEquals(205783, metaInfo.get("outputSizeBound"));
        assertEquals(0, metaInfo.get("compressionLevel"));
        assertTrue(w * h * 3 < (int) metaInfo.get("outputSize"));
        assertEquals(1, (double) metaInfo.get("compression"), 0.02);

        // load from png
        try (final InputStream is = new ByteArrayInputStream(pngOutput.array(), pngOutput.position(), pngOutput.limit())) {
            final Image recovered = new Image(is);
            // compare against original
            assertImageEqual(imageOvals, recovered);
        }
    }

    @Test
    public void testWritingImageByteBufferRandom() throws IOException {
        // convert to png
        int w = (int) imageRandom.getWidth();
        int h = (int) imageRandom.getHeight();
        final Map<String, Object> metaInfo = new HashMap<>();
        final ByteBuffer pngOutput = ByteBuffer.allocate(w * h * 4 + 100);
        final ByteBuffer pngOutReal = WriteFxImage.encode(imageRandom, pngOutput, true, Deflater.BEST_COMPRESSION,
                metaInfo);
        LOGGER.atDebug() //
                .addArgument(metaInfo.get("width")) //
                .addArgument(metaInfo.get("height")) //
                .addArgument(metaInfo.get("colorMode")) //
                .addArgument(metaInfo.get("compressionLevel")) //
                .addArgument(metaInfo.get("bufferSize")) //
                .addArgument(metaInfo.get("outputSize")) //
                .addArgument(metaInfo.get("compression")) //
                .addArgument(metaInfo.get("outputSizeBound")) //
                .log("Compressed Image ({}x{} {}) using compression level {} to image buffer (cap: {}, size: {}). compression rate: {}, outputSizeUpperBound: {}");
        // assert that the provided buffer was used
        assertSame(pngOutput, pngOutReal);
        assertEquals(w, metaInfo.get("width"));
        assertEquals(h, metaInfo.get("height"));
        assertEquals("rgba", metaInfo.get("colorMode"));
        assertEquals(w * h * 4 + 100, (Integer) metaInfo.get("bufferSize"));
        assertEquals(912095, metaInfo.get("outputSizeBound"));
        assertEquals(9, metaInfo.get("compressionLevel"));
        assertTrue(w * h * 4 > (int) metaInfo.get("outputSize"));
        assertEquals(0.21, (double) metaInfo.get("compression"), 0.02);
        // load from png
        try (final InputStream is = new ByteArrayInputStream(pngOutput.array(), pngOutput.position(), pngOutput.limit())) {
            final Image recovered = new Image(is);
            // compare against original
            assertImageEqual(imageRandom, recovered);
        }
    }

    @Test
    public void testWritingImageByteBuffer1x1() throws IOException {
        // convert to png
        final ByteBuffer pngOutput = ByteBuffer.allocate(100);
        final Map<String, Object> metaInfo = new HashMap<>();
        final ByteBuffer pngOutReal = WriteFxImage.encode(image1x1, pngOutput, true, Deflater.BEST_SPEED, metaInfo);
        LOGGER.atDebug() //
                .addArgument(metaInfo.get("width")) //
                .addArgument(metaInfo.get("height")) //
                .addArgument(metaInfo.get("colorMode")) //
                .addArgument(metaInfo.get("compressionLevel")) //
                .addArgument(metaInfo.get("bufferSize")) //
                .addArgument(metaInfo.get("outputSize")) //
                .addArgument(metaInfo.get("compression")) //
                .addArgument(metaInfo.get("outputSizeBound")) //
                .log("Compressed Image ({}x{} {}) using compression level {} to image buffer (cap: {}, size: {}). compression rate: {}, outputSizeUpperBound: {}");
        // assert that the provided buffer was used
        assertSame(pngOutput, pngOutReal);
        assertEquals(1, metaInfo.get("width"));
        assertEquals(1, metaInfo.get("height"));
        assertEquals("rgba", metaInfo.get("colorMode"));
        assertEquals(100, (Integer) metaInfo.get("bufferSize"));
        assertEquals(133, metaInfo.get("outputSizeBound"));
        assertEquals(1, metaInfo.get("compressionLevel"));
        assertEquals(3, (double) metaInfo.get("compression"), 1.0);
        // load from png
        try (final InputStream is = new ByteArrayInputStream(pngOutput.array(), pngOutput.position(), pngOutput.limit())) {
            final Image recovered = new Image(is);
            // compare against original
            assertImageEqual(image1x1, recovered);
        }
    }

    private static void assertImageEqual(Image original, Image recovered) {
        final int w = (int) original.getWidth();
        final int h = (int) original.getHeight();
        assertEquals(w, recovered.getWidth());
        assertEquals(h, recovered.getHeight());
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                assertEquals(original.getPixelReader().getArgb(x, y), recovered.getPixelReader().getArgb(x, y));
            }
        }
    }
}
