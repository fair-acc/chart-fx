package de.gsi.chart.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.dataset.utils.ArrayCache;

import ar.com.hjg.pngj.FilterType;

/**
 * Tests for {@link de.gsi.chart.utils.WriteFxImage}.
 *
 * @author Alexander Krimm
 */

@ExtendWith(ApplicationExtension.class)
public class WriteFxImageTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteFxImageBenchmark.class);
    private static final int DEFAULT_PALETTE_COLOR_COUNT = 256;
    private static final String INTERNAL_ARRAY_CACHE_NAME = "WriteFxImage-internalArray";
    private Image imageOvals;
    private Image imageRandom;
    private Image image1x1;

    @Test
    public void assertExceptions() {
        final ByteBuffer pngOutput = ByteBuffer.allocate(100);

        assertThrows(IllegalArgumentException.class, () -> WriteFxImage.encode(null, pngOutput, true, Deflater.BEST_SPEED, null));
    }

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
    public void testHelperFunctions() {
        assertImageEqual(image1x1, WriteFxImage.clone(image1x1));
        assertImageEqual(imageOvals, WriteFxImage.clone(imageOvals));
        assertImageEqual(imageRandom, WriteFxImage.clone(imageRandom));
    }

    @Test
    public void testPaletteGeneratorGetterSetter() {
        PaletteQuantizerNeuQuant test = new PaletteQuantizerNeuQuant(10, 10, (x, y) -> 0);

        assertDoesNotThrow(() -> test.setParAlphabiasshift(10));
        assertDoesNotThrow(() -> test.setParBeta(0.1));
        assertDoesNotThrow(() -> test.setParGamma(0.2));
        assertDoesNotThrow(() -> test.setParMaxPixelsToSample(2));
        assertDoesNotThrow(() -> test.setParNcolors(3));
        assertEquals(3, test.getColorCount());
        assertDoesNotThrow(() -> test.setParNcycles(4));
        assertDoesNotThrow(() -> test.setParRadiusbiasshift(5));
        assertDoesNotThrow(() -> test.setParRadiusdec(6));
        assertDoesNotThrow(() -> test.setParReserveAlphaColor(true));
        assertTrue(test.isParReserveAlphaColor());
        assertDoesNotThrow(() -> test.setParReserveAlphaColor(false));
        assertFalse(test.isParReserveAlphaColor());
        assertDoesNotThrow(() -> test.setParTransparencyThreshold(10));

        assertNotNull(test.convert(0, 0, 0));
        assertNotNull(test.convert(0, 0, 0, 0));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 })
    public void testPaletteHelper(final int testImageID) {
        final boolean alpha = true;
        final Image testImage = getTestImage(testImageID);
        int w = (int) testImage.getWidth();
        int h = (int) testImage.getHeight();

        final int requiredSize = w * h;
        final int[] uncompressedImageData = new int[requiredSize];
        WriteFxImage.copyImageDataToPixelBuffer(testImage, uncompressedImageData);

        PaletteQuantizer palette1 = WriteFxImage.estimatePalette(testImage, alpha, DEFAULT_PALETTE_COLOR_COUNT);
        PaletteQuantizer palette2 = WriteFxImage.estimatePalette(uncompressedImageData, w, h, alpha, DEFAULT_PALETTE_COLOR_COUNT);

        assertEquals(palette1.getColorCount(), palette2.getColorCount());
        for (int i = 0; i < palette1.getColorCount(); i++) {
            assertArrayEquals(palette1.getColor(i), palette2.getColor(i), "colour index " + i);
        }

        // test exceptions
        assertThrows(IllegalArgumentException.class, () -> WriteFxImage.copyImageDataToPixelBuffer(null, uncompressedImageData));
        assertThrows(IllegalArgumentException.class, () -> WriteFxImage.copyImageDataToPixelBuffer(testImage, null));
        assertThrows(IllegalArgumentException.class, () -> WriteFxImage.copyImageDataToPixelBuffer(testImage, new int[0]));

        assertThrows(IllegalArgumentException.class, () -> WriteFxImage.estimatePalette(null, true, DEFAULT_PALETTE_COLOR_COUNT));
        assertThrows(IllegalArgumentException.class, () -> WriteFxImage.estimatePalette(null, w, h, true, DEFAULT_PALETTE_COLOR_COUNT));
        assertThrows(IllegalArgumentException.class, () -> WriteFxImage.estimatePalette(uncompressedImageData, w + 10, h, true, DEFAULT_PALETTE_COLOR_COUNT));
        assertThrows(IllegalArgumentException.class, () -> WriteFxImage.estimatePalette(uncompressedImageData, w + 10, h + 10, true, DEFAULT_PALETTE_COLOR_COUNT));
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
        final ByteBuffer pngOutReal = WriteFxImage.encodeAlt(imageOvals, pngOutput, true, Deflater.BEST_SPEED, metaInfo);
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

        // Check that the array we pre-initialized was actually used
        assertNotEquals(fillByte, byteArray[15]);

        // load from png
        try (final InputStream is = new ByteArrayInputStream(pngOutput.array(), pngOutput.position(), pngOutput.limit())) {
            final Image recovered = new Image(is);
            // compare against original
            assertImageEqual(imageOvals, recovered);
        }
    }

    @ParameterizedTest
    @MethodSource("testWritingImageByteBufferProvider")
    public void testWritingImageByteBuffer(final int testImageID, final boolean allocateNewBuffer, final boolean encodeRGBA, final int compressionLevel, final FilterType filterType) throws IOException {
        // convert to png
        final Image testImage = getTestImage(testImageID);

        int w = (int) testImage.getWidth();
        int h = (int) testImage.getHeight();
        final int requiredSize = WriteFxImage.getCompressedSizeBound(w, h, encodeRGBA);
        final ByteBuffer pngOutput = allocateNewBuffer ? ByteBuffer.allocate(requiredSize) : null;
        final ByteBuffer pngOutReal = WriteFxImage.encode(testImage, pngOutput, encodeRGBA, compressionLevel, filterType);

        if (allocateNewBuffer) {
            // assert that the provided buffer was used
            assertSame(pngOutput, pngOutReal);
        } else {
            // user supplied output must be null
            assertNull(pngOutput);
        }

        // load from png
        try (final InputStream is = new ByteArrayInputStream(pngOutReal.array(), pngOutReal.position(), pngOutReal.limit())) {
            final Image recovered = new Image(is);
            // compare against original
            //assertImageEqual(testImage, recovered, encodeRGBA);
            assertImageSimilar(testImage, recovered, /* threshold */ 0.0, encodeRGBA);
        }
    }

    @Test
    public void testWritingImageByteBuffer1x1() throws IOException {
        // convert to png
        final ByteBuffer pngOutput = ByteBuffer.allocate(100);
        final Map<String, Object> metaInfo = new HashMap<>();
        final ByteBuffer pngOutReal = WriteFxImage.encodeAlt(image1x1, pngOutput, true, Deflater.BEST_SPEED, metaInfo);
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

    @ParameterizedTest
    @MethodSource("testWritingImageByteBufferProvider")
    public void testWritingImageByteBufferPalette(final int testImageID, final boolean allocateNewBuffer, final boolean encodeRGBA, final int compressionLevel, final FilterType filterType) throws IOException {
        final Image testImage = getTestImage(testImageID);

        int w = (int) testImage.getWidth();
        int h = (int) testImage.getHeight();
        // add in addition ~ 256*(3..4) bytes to store palette info
        final int requiredSize = WriteFxImage.getCompressedSizeBound(w, h, encodeRGBA) + 256 * 4;
        final ByteBuffer pngOutput = allocateNewBuffer ? ByteBuffer.allocate(requiredSize) : null;
        final ByteBuffer pngOutReal = WriteFxImage.encodePalette(testImage, pngOutput, encodeRGBA, compressionLevel, filterType);

        if (allocateNewBuffer) {
            // assert that the provided buffer was used
            assertSame(pngOutput, pngOutReal);
        } else {
            // user supplied output must be null
            assertNull(pngOutput);
        }

        // load from png
        try (final InputStream is = new ByteArrayInputStream(pngOutReal.array(), pngOutReal.position(), pngOutReal.limit())) {
            final Image recovered = new Image(is);
            // compare against original with color threshold
            assertImageSimilar(testImage, recovered, /* threshold */ 0.2, encodeRGBA);
        }
    }

    @Test
    public void testWritingImageByteBufferRandom() throws IOException {
        // convert to png
        int w = (int) imageRandom.getWidth();
        int h = (int) imageRandom.getHeight();
        final Map<String, Object> metaInfo = new HashMap<>();
        final ByteBuffer pngOutput = ByteBuffer.allocate(w * h * 4 + 100);
        final ByteBuffer pngOutReal = WriteFxImage.encodeAlt(imageRandom, pngOutput, true, Deflater.BEST_COMPRESSION,
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
    public void testWritingImageByteBufferRandomRbgNoCompression() throws IOException {
        // convert to png
        int w = (int) imageOvals.getWidth();
        int h = (int) imageOvals.getHeight();
        final ByteBuffer pngOutput = ByteBuffer.allocate(w * h * 3 + 1000);
        final Map<String, Object> metaInfo = new HashMap<>();
        final ByteBuffer pngOutReal = WriteFxImage.encodeAlt(imageOvals, pngOutput, false, Deflater.NO_COMPRESSION,
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

    private Image getTestImage(final int testImageID) {
        final Image testImage;
        switch (testImageID) {
        case 0:
            testImage = imageOvals;
            break;
        case 1:
            testImage = imageRandom;
            break;
        case 2:
        default:
            testImage = image1x1;
            break;
        }
        return testImage;
    }

    public static double colorDistance(final int colorARGB1, final int colorARGB2, final boolean checkAlpha) {
        final int deltaA = ((colorARGB1 & 0xff000000) >>> 24) - ((colorARGB2 & 0xff000000) >>> 24);
        final int deltaR = ((colorARGB1 & 0x00ff0000) >>> 16) - ((colorARGB2 & 0x00ff0000) >>> 16);
        final int deltaG = ((colorARGB1 & 0x0000ff00) >>> 8) - ((colorARGB2 & 0x0000ff00) >>> 8);
        final int deltaB = ((colorARGB1 & 0xff)) - ((colorARGB2 & 0xff));

        return (checkAlpha ? Math.sqrt((deltaA * deltaA) + (deltaR * deltaR) + (deltaG * deltaG) + (deltaB * deltaB)) / (4.0 * 255.0) //
                           : Math.sqrt((deltaR * deltaR) + (deltaG * deltaG) + (deltaB * deltaB)))
      / (3.0 * 255.0);
    }

    private static void assertImageEqual(final Image original, final Image recovered, final boolean... assertRGBA) {
        final int w = (int) original.getWidth();
        final int h = (int) original.getHeight();
        assertEquals(w, recovered.getWidth());
        assertEquals(h, recovered.getHeight());
        if (assertRGBA.length == 0 || assertRGBA[0]) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    assertEquals(original.getPixelReader().getArgb(x, y), recovered.getPixelReader().getArgb(x, y));
                }
            }
        } else {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    assertEquals((original.getPixelReader().getArgb(x, y) << 8) >> 8, (recovered.getPixelReader().getArgb(x, y) << 8) >> 8);
                }
            }
        }
    }

    private static void assertImageSimilar(final Image original, final Image recovered, final double threshold, final boolean checkAlpha) {
        final int w = (int) original.getWidth();
        final int h = (int) original.getHeight();
        assertEquals(w, recovered.getWidth());
        assertEquals(h, recovered.getHeight());
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                final double colorDistance = colorDistance(original.getPixelReader().getArgb(x, y), recovered.getPixelReader().getArgb(x, y), checkAlpha);
                assertTrue(colorDistance <= threshold, "colour distance mismatch for pixel(" + x + ", " + y + ") = " + colorDistance);
            }
        }
    }

    private static Stream<Arguments> testWritingImageByteBufferProvider() { // NOPMD -- is used in annotation /not detected by PMD
        List<Arguments> argumentList = new ArrayList<>();
        // N.B. limit to some selected sub-cases and not test all (full parameter space takes too much time for regular CI/CD checks
        for (int testImageID : new int[] { 0, 1, 2 }) {
            for (boolean encodeRGBA : new boolean[] { true, false }) {
                argumentList.add(Arguments.arguments(testImageID, true, encodeRGBA, Deflater.NO_COMPRESSION, FilterType.FILTER_NONE));
            }
            for (boolean allocateNewBuffer : new boolean[] { true, false }) {
                argumentList.add(Arguments.arguments(testImageID, allocateNewBuffer, true, Deflater.NO_COMPRESSION, FilterType.FILTER_NONE));
            }

            // loop through compression levels
            for (int compressionLevel : new int[] { Deflater.NO_COMPRESSION, Deflater.BEST_SPEED, Deflater.BEST_COMPRESSION }) {
                argumentList.add(Arguments.arguments(testImageID, true, true, compressionLevel, FilterType.FILTER_NONE));
            }

            // loop through PNG line filter settings
            for (FilterType filterType : new FilterType[] { FilterType.FILTER_NONE, FilterType.FILTER_PAETH }) {
                if (!FilterType.isValidStandard(filterType)) {
                    continue;
                }
                argumentList.add(Arguments.arguments(testImageID, true, true, Deflater.NO_COMPRESSION, filterType));
            }
        }

        //return Stream.of("apple", "banana");
        return Stream.of(argumentList.toArray(new Arguments[0]));
    }
}
