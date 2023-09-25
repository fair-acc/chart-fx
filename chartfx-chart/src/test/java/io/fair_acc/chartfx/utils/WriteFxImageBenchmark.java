package io.fair_acc.chartfx.utils;

import static io.fair_acc.chartfx.utils.WriteFxImageBenchmark.Implementation.NEWREF;
import static io.fair_acc.chartfx.utils.WriteFxImageBenchmark.Implementation.OLDREF;
import static io.fair_acc.chartfx.utils.WriteFxImageBenchmark.Implementation.PALETTE;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.Deflater;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.com.hjg.pngj.FilterType;

/**
 * Checking the performance of writing a PNG image from a JavaFx Image
 *
 * @author Alexander Krimm
 */
public class WriteFxImageBenchmark {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteFxImageBenchmark.class);
    private static final int DEFAULT_PALETTE_COLOR_COUNT = 8;
    private static final int N_ITER = 50;

    private static final int w = 333;
    private static final int h = 777;
    private static final ByteBuffer noisePixels = ByteBuffer.allocate(w * h * 4);
    private static PixelBuffer<ByteBuffer> noiseBuffer;
    private static WritableImage testimage; // test image with noise (N.B. hard to compress)
    private static final int w2 = 777;
    private static final int h2 = 333;
    private static Image testimage2; // test image with shapes (N.B. easy to compress
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static void initalizeImage() {
        noiseBuffer = new PixelBuffer<>(w, h, noisePixels, PixelFormat.getByteBgraPreInstance());
        testimage = new WritableImage(noiseBuffer);
        final Canvas noiseCanvas = new Canvas(w, h);
        final GraphicsContext noiseContext = noiseCanvas.getGraphicsContext2D();
        final byte[] randomArray = new byte[w * h * 4];
        new Random().nextBytes(randomArray);
        noiseContext.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getByteBgraInstance(), randomArray, 0, w);
        noiseCanvas.snapshot(null, testimage);

        final Canvas easyCanvas = new Canvas(w2, h2);
        final GraphicsContext easyContext = easyCanvas.getGraphicsContext2D();
        easyContext.setStroke(Color.BLUE);
        easyContext.strokeOval(20, 30, 40, 50);
        easyContext.setStroke(Color.RED);
        easyContext.strokeOval(30, 40, 50, 60);
        easyContext.setStroke(Color.GREEN);
        easyContext.strokeOval(40, 50, 60, 70);
        easyContext.setStroke(Color.ORANGE);
        easyContext.strokeRect(0, 0, w2, h2);
        testimage2 = easyCanvas.snapshot(null, null);

        initialized.set(true);
    }

    public static void main(String[] args) throws IOException {
        // get the image on the javafx application thread for snapshot to work
        Platform.startup(WriteFxImageBenchmark::initalizeImage);
        while (!initialized.get()) { // NOPMD busy loop
            //
        }
        Platform.exit();

        LOGGER.atInfo().log("Image with noise data (difficult to compress)");
        for (final boolean alpha : new boolean[] { false, true }) {
            for (final boolean keepBuffer : new boolean[] { false, true }) {
                writeFxImage(testimage, alpha, keepBuffer, Deflater.NO_COMPRESSION, NEWREF);
                writeFxImage(testimage, alpha, keepBuffer, Deflater.BEST_SPEED, NEWREF);
                writeFxImage(testimage, alpha, keepBuffer, Deflater.BEST_COMPRESSION, NEWREF);
            }
        }
        writeImageIoImage(testimage, false, false);
        writeImageIoImage(testimage, true, false);
        writeImageIoImage(testimage, false, true);
        writeImageIoImage(testimage, true, true);

        LOGGER.atInfo().log("Image with simple shapes (easy to compress)");
        for (final boolean alpha : new boolean[] { false, true }) {
            for (final boolean keepBuffer : new boolean[] { false, true }) {
                writeFxImage(testimage2, alpha, keepBuffer, Deflater.NO_COMPRESSION, NEWREF);
                writeFxImage(testimage2, alpha, keepBuffer, Deflater.BEST_SPEED, NEWREF);
                writeFxImage(testimage2, alpha, keepBuffer, Deflater.BEST_COMPRESSION, NEWREF);
            }
        }
        writeImageIoImage(testimage2, false, false);
        writeImageIoImage(testimage2, true, false);
        writeImageIoImage(testimage2, false, true);
        writeImageIoImage(testimage2, true, true);

        testCompressionPerformance(testimage, "noise data (difficult to compress)");
        testCompressionPerformance(testimage2, "simple shapes (easy to compress)");
    }

    public static void testCompressionPerformance(final Image image, final String description) {
        LOGGER.atInfo().addArgument(description).log("Test compression level performance with image with {}");
        // precompute typical palette
        final PaletteQuantizer userPaletteRGBA = WriteFxImage.estimatePalette(image, true, DEFAULT_PALETTE_COLOR_COUNT);
        final PaletteQuantizer userPaletteRGB = WriteFxImage.estimatePalette(image, false, DEFAULT_PALETTE_COLOR_COUNT);

        for (final boolean alpha : new boolean[] { true, false }) {
            LOGGER.atInfo().addArgument(alpha ? "with" : "without").log("{} alpha channel");
            for (int compressionLevel = Deflater.NO_COMPRESSION; compressionLevel <= Deflater.BEST_COMPRESSION; compressionLevel++) {
                writeFxImage(image, alpha, true, compressionLevel, OLDREF);
                writeFxImage(image, alpha, true, compressionLevel, NEWREF);
                // compute palette on-the-fly
                writeFxImage(image, alpha, true, compressionLevel, PALETTE);
                // use pre-computed palette
                writeFxImage(image, alpha, true, compressionLevel, PALETTE, alpha ? userPaletteRGBA : userPaletteRGB);
                LOGGER.atInfo().log(" "); // deliberatly empty line for better readability
            }
        }
    }

    private static void writeFxImage(Image image, boolean alpha, boolean keepBuffer, int compression, final Implementation implementation, PaletteQuantizer... quantizer) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(WriteFxImage.getCompressedSizeBound((int) image.getWidth(), (int) image.getHeight(), alpha));
        int size = 0;
        final long start = System.currentTimeMillis();
        switch (implementation) {
        case OLDREF:
            for (int i = 0; i < N_ITER; i++) {
                final ByteBuffer bb = WriteFxImage.encodeAlt(image, keepBuffer ? byteBuffer : null, alpha, compression, null);
                size += bb.limit();
            }
            break;
        case PALETTE:
            for (int i = 0; i < N_ITER; i++) {
                final ByteBuffer bb = WriteFxImage.encodePalette(image, keepBuffer ? byteBuffer : null, alpha, compression, FilterType.FILTER_NONE, quantizer.length == 0 ? null : quantizer[0]);
                size += bb.limit();
            }
            break;
        case NEWREF:
        default:
            for (int i = 0; i < N_ITER; i++) {
                final ByteBuffer bb = WriteFxImage.encode(image, keepBuffer ? byteBuffer : null, alpha, compression, FilterType.FILTER_NONE);
                size += bb.limit();
            }
            break;
        }
        final long stop = System.currentTimeMillis();
        final double avgSize = size / (double) N_ITER;
        final double actualCompression = 100.0 * avgSize / (double) WriteFxImage.encode(image, null, alpha, Deflater.NO_COMPRESSION, null).limit();
        LOGGER.atInfo() //
                .addArgument(implementation.getName())
                .addArgument((int) image.getWidth())
                .addArgument((int) image.getHeight()) //
                .addArgument(String.format("%5.1f", actualCompression)) //
                .addArgument(String.format("%5.2f", (stop - start) / (double) N_ITER)) //
                .addArgument(alpha ? "rgba" : "rgb ") //
                .addArgument(keepBuffer ? "keepBuffer" : "discardBuffer") //
                .addArgument(compression) //
                .log("FxImage-{}: size {}x{} \t compression: {}% \t {} ms/image    {} {} compressionLevel: {}");
    }

    private static void writeImageIoImage(Image image, boolean keepStream, boolean keepBImg) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(w * h * 4);
        BufferedImage bimg = null;
        int size = 0;
        final long start = System.currentTimeMillis();
        for (int i = 0; i < N_ITER; i++) {
            final BufferedImage newbimg = SwingFXUtils.fromFXImage(image, bimg);
            ImageIO.write(newbimg, "PNG", os);
            if (keepBImg) {
                bimg = newbimg;
            }
            size += os.size();
            if (keepStream) {
                os.reset();
            } else {
                os = new ByteArrayOutputStream(w * h * 4);
            }
        }
        final long stop = System.currentTimeMillis();
        final double avgSize = size / (double) N_ITER;
        final double actualCompression = 100.0 * avgSize / (double) WriteFxImage.encode(image, null, true, Deflater.NO_COMPRESSION, null).limit();
        LOGGER.atInfo() //
                .addArgument((int) image.getWidth())
                .addArgument((int) image.getHeight()) //
                .addArgument(String.format("%5.1f", actualCompression)) //
                .addArgument(String.format("%5.2f", (stop - start) / (double) N_ITER)) //
                .addArgument(keepStream ? "keepStream" : "reallocateStream") //
                .addArgument(keepBImg ? "keepBufferedImage" : "reallocateBufferedImage") //
                .log("ImageIO: size {}x{} \t compression: {}% \t {} ms/image    rgba {} {}");
    }

    public enum Implementation {
        OLDREF,
        NEWREF,
        PALETTE;

        public String getName() {
            switch (this) {
            case OLDREF:
                return "OldRef ";
            case NEWREF:
                return "NewRef ";
            case PALETTE:
                return "Palette";
            default:
                return "unknown";
            }
        }
    }
}
