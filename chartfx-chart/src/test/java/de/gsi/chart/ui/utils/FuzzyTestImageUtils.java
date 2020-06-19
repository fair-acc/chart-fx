package de.gsi.chart.ui.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.utils.WriteFxImage;

/**
 * Small helper routines to generate and fuzzy-compare test images
 * 
 * @author rstein
 *
 */
public class FuzzyTestImageUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FuzzyTestImageUtils.class);
    private static final String srcResourceFolder = Paths.get("src", "test", "resources").toString();
    private static final String targetResourceFolder = Paths.get("target", "test-classes").toString();

    public static File getReferenceFile(Class<?> rootClass, final String referenceImageLocation) {
        final String resourceBase = rootClass.getResource("").getPath().replace(targetResourceFolder, srcResourceFolder);
        return new File(resourceBase + referenceImageLocation);
    }

    public static File getReferencePath(Class<?> rootClass) {
        final String resourceBase = rootClass.getResource("").getPath().replace(targetResourceFolder, srcResourceFolder);
        return new File(resourceBase);
    }

    public static Image getTestImage(Class<?> rootClass, final String referenceImageLocation) {
        try (InputStream is = new FileInputStream(getReferenceFile(rootClass, referenceImageLocation))) {
            return new Image(is);
        } catch (IOException e) {
            LOGGER.atWarn().addArgument(referenceImageLocation).addArgument(rootClass.getCanonicalName()).addArgument(e.getLocalizedMessage()) //
                    .log("could not load reference image '{}' for class '{}' - root cause: {}");
            return null;
        }
    }

    public static void writeTestImage(Class<?> rootClass, final String referenceImageLocation, final Image testImage) {
        final File file = getReferenceFile(rootClass, referenceImageLocation);
        final File fileParent = getReferencePath(rootClass);
        if (fileParent.mkdirs()) {
            LOGGER.atInfo().addArgument(fileParent.getPath()).addArgument(referenceImageLocation).log("needed to create directory '{}' for file {}");
        }
        try (OutputStream os = Files.newOutputStream(file.toPath())) {
            final ByteBuffer buffer = WriteFxImage.encode(testImage);
            os.write(buffer.array(), 0, buffer.limit());
        } catch (IOException e) {
            LOGGER.atWarn().addArgument(referenceImageLocation).addArgument(rootClass.getCanonicalName()).addArgument(e.getLocalizedMessage()) //
                    .log("could not write reference image '{}' for class '{}' - cause: {}");
            e.printStackTrace();
        }
    }

    public static double compareAndWriteReference(Class<?> rootClass, final String referenceImageLocation, final Image testImage) {
        final Image refImage = getTestImage(rootClass, referenceImageLocation);
        if (refImage == null) {
            writeTestImage(rootClass, referenceImageLocation, testImage);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.atInfo().addArgument(referenceImageLocation).addArgument(rootClass.getCanonicalName()).log("needed to write new reference image '{}' for class '{}'");
            }
            return 1.0;
        }

        // TODO: replace with a perceptual image hash function, e.g. something derived from https://www.phash.org/

        return comparePoorMansImageComparison(refImage, testImage);
    }

    private static double comparePoorMansImageComparison(final Image img1, final Image img2) {
        final int width = (int) img1.getWidth();
        final int height = (int) img1.getHeight();
        if (width != (int) img2.getWidth() || height != (int) img2.getHeight()) {
            return 0.0;
        }
        final double nPixels = width * height;
        double diff = 1.0;
        final PixelReader pixelReaderRef = img1.getPixelReader();
        final PixelReader pixelReaderTest = img2.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                diff *= 1.0 - (1.0 - pixelDiff(pixelReaderRef.getColor(x, y), pixelReaderTest.getColor(x, y))) / nPixels;
            }
        }
        return diff;
    }

    private static double pixelDiff(final Color ref, final Color test) {
        return (1.0 - Math.abs(ref.getHue() - test.getHue()) / 360.0); // *(1.0 - Math.abs(ref.getBrightness() - test.getBrightness()))*(1.0 - Math.abs(ref.getSaturation() - test.getSaturation()));
    }
}
