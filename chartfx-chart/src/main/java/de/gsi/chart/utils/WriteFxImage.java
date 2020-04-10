package de.gsi.chart.utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import de.gsi.dataset.utils.ArrayCache;

/**
 * Writes a JavaFx Image into a ByteBuffer or file
 *
 * possible improvements:
 * - use multiple IDAT chunks to use fixed buffer size and limit memory footprint
 * - implement filtering of lines before compression for smaller file sizes
 * - Optionally add tEXT chunks for metadata (EXIF)
 *
 * @author Alexander Krimm
 */
public final class WriteFxImage {
    private static final int HEADER_SIZE = 8 + 12 + 13 + 12 + 12; // size of all the headers and other Metadata
    private static final String INTERNAL_ARRAY_CACHE_NAME = "WriteFxImage-internalArray";

    /**
     * private constructor for static utility class
     */
    private WriteFxImage() {
    }

    /**
     * copy the given Image to a WritableImage
     * 
     * @param image the input image
     * @return clone of image
     */
    public static WritableImage clone(Image image) {
        int height = (int) image.getHeight();
        int width = (int) image.getWidth();
        PixelReader pixelReader = image.getPixelReader();
        WritableImage writableImage = new WritableImage(width, height);
        PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pixelReader.getColor(x, y);
                pixelWriter.setColor(x, y, color);
            }
        }
        return writableImage;
    }

    /**
     * Encodes a JavaFx image as an RGB png image with fastest (lossless) compression
     *
     * @param image The input image to be encoded
     * @return a byte buffer with the encoded image
     * @see <a href="https://tools.ietf.org/html/rfc2083">rfc2083</a>
     */
    public static ByteBuffer encode(final Image image) {
        return encode(image, null, true, Deflater.BEST_SPEED, null);
    }

    /**
     * Encodes a JavaFx image as an RGB png image.
     * If you pass in a ByteBuffer to use, please make sure that it has enough capacity to fit the encoded image or
     * handle errors (IndexOutOfBoundsException) accordingly. If you want to be on the safe side, use
     * {@link #getCompressedSizeBound(int, int, boolean) getCompressedSizeBound(width, height, alpha)} 
     * to get an upper bound for the output size.
     *
     * @param image The input image to be encoded
     * @param byteBuffer optional byte buffer to store the output in, pass null to return a new one.
     * @param alpha whether to include alpha information in the image
     * @param compressionLevel {@link Deflater#BEST_COMPRESSION} (9) to {@link Deflater#BEST_SPEED} (0)
     * @param metaInfo an optional map which will be filled with debugging information like compression efficiency
     * @return a byte buffer with the encoded image
     * @see "https://tools.ietf.org/html/rfc2083"
     */
    public static ByteBuffer encode(final Image image, final ByteBuffer byteBuffer, final boolean alpha,
            final int compressionLevel, final Map<String, Object> metaInfo) {
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        // get meta info
        final int w = (int) image.getWidth();
        final int h = (int) image.getHeight();
        // allocate output buffer if necessary. conservative allocation, assume upper bound for deflate algorithm
        final ByteBuffer outputByteBuffer = byteBuffer == null ? ByteBuffer.allocate(getCompressedSizeBound(w, h, alpha)) : byteBuffer;
        // get necessary helper classes
        final PixelReader pr = image.getPixelReader();
        if (pr == null) {
            throw new IllegalStateException("image PixelReader not available");
        }
        final CRC32 crc = new CRC32();
        final Deflater compressor = new Deflater(compressionLevel);
        // actual PNG encoding
        writeImageHeader(w, h, alpha, outputByteBuffer, crc);
        writeImageData(pr, w, h, alpha, compressor, outputByteBuffer, crc);
        writeImageFooter(outputByteBuffer, crc);
        // prepare to return outputBuffer
        outputByteBuffer.flip();
        // DEGBUG output
        if (metaInfo != null) {
            final int bytesPerPixel = alpha ? 4 : 3;
            metaInfo.put("bufferSize", outputByteBuffer.capacity());
            metaInfo.put("outputSizeBound", getCompressedSizeBound(w, h, alpha));
            metaInfo.put("compression", (double) compressor.getBytesWritten() / (w * h * bytesPerPixel));
            metaInfo.put("width", w);
            metaInfo.put("height", h);
            metaInfo.put("colorMode", alpha ? "rgba" : "rgb");
            metaInfo.put("compressionLevel", compressionLevel);
            metaInfo.put("outputSize", outputByteBuffer.limit() - outputByteBuffer.position());
        }
        return outputByteBuffer;
    }

    /**
     * Returns the conservative upper bound for the compressed image size.
     *
     * @param width Image width
     * @param height Image height
     * @param alpha Alpha enabled
     * @return the upper bound for the size of the resulting png in bytes
     * @see <a href="https://github.com/madler/zlib/blob/master/deflate.c#L659-L661">zlib sourcefor deflate upper bound</a>
     */
    public static int getCompressedSizeBound(final int width, final int height, final boolean alpha) {
        final int bytesPerPixel = alpha ? 4 : 3;
        final int uncompressedSize = width * height * bytesPerPixel + height + HEADER_SIZE;
        final int compressedSize = uncompressedSize + (uncompressedSize + 7 >> 3) + (uncompressedSize + 63 >> 6) + 5;
        return compressedSize + HEADER_SIZE;
    }

    /**
     * Saves the given image as a png file.
     *
     * @param image The image to save
     * @param file The filename to save the image to.
     * @throws IOException if the file cannot be written
     */
    public static void savePng(final Image image, final File file) throws IOException {
        try (OutputStream os = Files.newOutputStream(file.toPath())) {
            final ByteBuffer buffer = WriteFxImage.encode(image);
            os.write(buffer.array(), 0, buffer.limit());
        }
    }

    /**
     * Writes a byte array to the buffer and updates the checksum.
     *
     * @param b byteArray to put into buffer
     * @param buffer output buffer
     * @param crc checksum calculator
     */
    private static void write(final byte[] b, final ByteBuffer buffer, final CRC32 crc) {
        buffer.put(b);
        crc.update(b);
    }

    /**
     * Writes an integer value to the buffer and updates the checksum.
     *
     * @param i integer value to write
     * @param buffer the buffer to write to
     * @param crc the checksum to be updated
     */
    private static void write(final int i, final ByteBuffer buffer, final CRC32 crc) {
        final byte[] b = { (byte) (i >> 24 & 0xff), (byte) (i >> 16 & 0xff), (byte) (i >> 8 & 0xff), (byte) (i & 0xff) };
        write(b, buffer, crc);
    }

    /**
     * Writes the IDAT chunk of a png file, which contains the compressed image data to the supplied buffer.
     *
     * @param pr PixelWriter
     * @param w with of the image
     * @param h height of the image
     * @param alpha whether to include alpha information
     * @param outputByteBuffer the byte buffer to write to
     * @param crc checksum calculator
     */
    private static void writeImageData(final PixelReader pr, final int w, final int h, final boolean alpha, final Deflater compressor,
            final ByteBuffer outputByteBuffer, final CRC32 crc) {
        // get raw image data
        final int bytesPerPixel = alpha ? 4 : 3;
        final int rawDataSize = w * h * bytesPerPixel + h; // image dimensions times bytesPerPixel + line filtering flag
        final byte[] uncompressedImageData = ArrayCache.getCachedByteArray(INTERNAL_ARRAY_CACHE_NAME, rawDataSize);
        int i = 0;
        if (alpha) {
            for (int y = 0; y < h; y++) {
                uncompressedImageData[i++] = 0; // LineFiltering: 0: None 1: Sub 2: Up 3: Average 4: Paeth
                for (int x = 0; x < w; x++) {
                    final int pixel = pr.getArgb(x, y);
                    uncompressedImageData[i++] = (byte) (pixel >> 16 & 0xff); //red
                    uncompressedImageData[i++] = (byte) (pixel >> 8 & 0xff); // green
                    uncompressedImageData[i++] = (byte) (pixel & 0xff); // blue
                    uncompressedImageData[i++] = (byte) (pixel >> 24 & 0xff); // alpha
                }
            }
        } else {
            for (int y = 0; y < h; y++) {
                uncompressedImageData[i++] = 0; // LineFiltering: 0: None 1: Sub 2: Up 3: Average 4: Paeth
                for (int x = 0; x < w; x++) {
                    final int pixel = pr.getArgb(x, y);
                    uncompressedImageData[i++] = (byte) (pixel >> 16 & 0xff); //red
                    uncompressedImageData[i++] = (byte) (pixel >> 8 & 0xff); // green
                    uncompressedImageData[i++] = (byte) (pixel & 0xff); // blue
                }
            }
        }
        // write compressed image data to IDAT block
        compressor.setInput(uncompressedImageData);
        outputByteBuffer.mark();
        outputByteBuffer.putInt(0); // zero size, will later be overwritten when we know the size
        outputByteBuffer.put("IDAT".getBytes());
        compressor.finish();
        compressor.deflate(outputByteBuffer, Deflater.FULL_FLUSH);
        outputByteBuffer.limit(outputByteBuffer.position());
        outputByteBuffer.reset();
        outputByteBuffer.putInt(compressor.getTotalOut());
        crc.reset();
        crc.update(outputByteBuffer);
        outputByteBuffer.limit(outputByteBuffer.capacity());
        outputByteBuffer.putInt((int) crc.getValue());
        ArrayCache.release(INTERNAL_ARRAY_CACHE_NAME, uncompressedImageData);
    }

    /**
     * Writes the end marker for the png file format
     *
     * @param outputByteBuffer the buffer to write into
     * @param crc The checksum calculator
     */
    private static void writeImageFooter(final ByteBuffer outputByteBuffer, final CRC32 crc) {
        outputByteBuffer.putInt(0);
        crc.reset();
        write("IEND".getBytes(), outputByteBuffer, crc);
        outputByteBuffer.putInt((int) crc.getValue());
    }

    /**
     * Writes the PNG file header and IHDR meta-information block
     *
     * @param width The with of the image
     * @param height The height of the image
     * @param alpha whether to support alpha
     * @param outputByteBuffer the buffer to write into
     * @param crc the checksum calculator
     */
    private static void writeImageHeader(final int width, final int height, final boolean alpha, final ByteBuffer outputByteBuffer, final CRC32 crc) {
        // File Signature - "\211PNG\r\n\032\n" - 8950 4e47 0d0a 1a0a
        outputByteBuffer.put(new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a });
        // IHDR
        outputByteBuffer.putInt(13); // length of IHDR chunk
        crc.reset(); // checksum starts after length field
        write("IHDR".getBytes(), outputByteBuffer, crc);
        write(width, outputByteBuffer, crc); // with 4 bytes
        write(height, outputByteBuffer, crc); // height 4 bytes
        // Bit depth:          1 byte  1*,2*,4*,8,16° (*:indexed only, °: not indexed)
        // Color type:         1 byte  0 (grayscale), 2 (RGB), 3 (Indexed), 4 (grayscale+alpha), and 6 (RGBA).
        // Compression method: 1 byte  0 (deflate/inflate compression with a 32K sliding window)
        // Filter method:      1 byte  0 (adaptive filtering with five basic filter types)
        // Interlace method:   1 byte  0 (no interlace) or 1 (Adam7 interlace)
        write(alpha ? new byte[] { 8, 6, 0, 0, 0 } : new byte[] { 8, 2, 0, 0, 0 }, outputByteBuffer, crc); // RGB(A) Mode
        outputByteBuffer.putInt((int) crc.getValue());
    }
}
