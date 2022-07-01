package io.fair_acc.chartfx.utils;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.dataset.utils.ArrayCache;
import io.fair_acc.dataset.utils.ByteBufferOutputStream;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

/**
 * Writes a JavaFx Image into a ByteBuffer or file
 *
 * possible improvements: - use multiple IDAT chunks to use fixed buffer size
 * and limit memory footprint - implement filtering of lines before compression
 * for smaller file sizes - Optionally add tEXT chunks for metadata (EXIF)
 *
 * @author Alexander Krimm
 */
@SuppressWarnings("PMD.GodClass")
public final class WriteFxImage {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteFxImage.class);
    private static final int DEFAULT_PALETTE_COLOR_COUNT = 256;
    private static final String IMAGE_PIXEL_READER_NOT_AVAILABLE = "image PixelReader not available";
    private static final String IMAGE_MUST_NOT_BE_NULL = "image must not be null";
    private static final int HEADER_SIZE = 8 + 12 + 13 + 12 + 12; // size of all the headers and other Metadata
    private static final String INTERNAL_ARRAY_CACHE_NAME = "WriteFxImage-internalArray";
    private static final String INTERNAL_LINE_ARRAY_CACHE_NAME = "WriteFxImage-internalLineArray";

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
        WritableImage writableImage = WritableImageCache.getInstance().getImage(width, height);
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        if (pixelWriter == null) {
            throw new IllegalStateException(IMAGE_PIXEL_READER_NOT_AVAILABLE);
        }

        final PixelReader pixelReader = image.getPixelReader();
        if (pixelReader == null) {
            throw new IllegalStateException(IMAGE_PIXEL_READER_NOT_AVAILABLE);
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pixelReader.getColor(x, y);
                pixelWriter.setColor(x, y, color);
            }
        }
        return writableImage;
    }

    public static void copyImageDataToPixelBuffer(final Image image, final int[] uncompressedImageData) {
        if (image == null) {
            throw new IllegalArgumentException("image is null");
        }
        final PixelReader pr = image.getPixelReader();
        if (pr == null) {
            throw new IllegalStateException(IMAGE_PIXEL_READER_NOT_AVAILABLE);
        }
        if (uncompressedImageData == null) {
            throw new IllegalArgumentException("uncompressedImageData is null");
        }
        final int w = (int) image.getWidth();
        final int h = (int) image.getHeight();
        final int requiredSize = w * h;
        if (uncompressedImageData.length < requiredSize) {
            throw new IllegalArgumentException("uncompressedImageData.length = " //
                                               + uncompressedImageData.length + " too small, should be at least" + requiredSize);
        }
        int i = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                uncompressedImageData[i++] = pr.getArgb(x, y);
            }
        }
    }

    /**
     * Encodes a JavaFx image as an RGB png image with fastest (lossless)
     * compression
     *
     * @param image The input image to be encoded
     * @return a byte buffer with the encoded image
     * @see <a href="https://tools.ietf.org/html/rfc2083">rfc2083</a>
     */
    public static ByteBuffer encode(final Image image) {
        return encode(image, null, true, Deflater.BEST_SPEED, null);
    }

    /**
     * Encodes a JavaFx image as an RGB png image. If you pass in a ByteBuffer to
     * use, please make sure that it has enough capacity to fit the encoded image or
     * handle errors (IndexOutOfBoundsException) accordingly. If you want to be on
     * the safe side, use {@link #getCompressedSizeBound(int, int, boolean)
     * getCompressedSizeBound(width, height, alpha)} to get an upper bound for the
     * output size.
     *
     * @param image            The input image to be encoded
     * @param byteBuffer       optional byte buffer to store the output in, pass
     *                         null to return a new one.
     * @param alpha            whether to include alpha information in the image
     * @param compressionLevel {@link Deflater#BEST_COMPRESSION} (9) to
     *                         {@link Deflater#BEST_SPEED} (0)
     * @param filterType       filter as outlines in https://tools.ietf.org/html/rfc2083#section-6
     *                         plus some custom additional options implemented in the undelying
     *                         PNG encode implementation
     * @return a byte buffer with the encoded image
     * @see "https://tools.ietf.org/html/rfc2083"
     */
    public static ByteBuffer encode(final Image image, final ByteBuffer byteBuffer, final boolean alpha, final int compressionLevel, final FilterType filterType) {
        if (image == null) {
            throw new IllegalArgumentException(IMAGE_MUST_NOT_BE_NULL);
        }
        // get meta info
        final PixelReader pr = image.getPixelReader();
        if (pr == null) {
            throw new IllegalStateException(IMAGE_PIXEL_READER_NOT_AVAILABLE);
        }
        final int w = (int) image.getWidth();
        final int h = (int) image.getHeight();
        final ByteBuffer outputByteBuffer = byteBuffer == null ? ByteBuffer.allocate(getCompressedSizeBound(w, h, alpha)) : byteBuffer;
        try (ByteBufferOutputStream os = new ByteBufferOutputStream(outputByteBuffer, false)) {
            PngWriter png = new PngWriter(os, new ImageInfo(w, h, 8, alpha, false, false));
            png.getPixelsWriter().setFilterType(filterType == null ? FilterType.FILTER_NONE : filterType);
            png.setIdatMaxSize(0x10000);
            png.setCompLevel(compressionLevel);

            ImageLineInt line = new ImageLineInt(png.imgInfo);
            if (alpha) {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        final int pixel = pr.getArgb(x, y);
                        ImageLineHelper.setPixelRGBA8(line, x, pixel);
                    }
                    png.writeRow(line, y);
                }
            } else {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        final int pixel = pr.getArgb(x, y);
                        ImageLineHelper.setPixelRGB8(line, x, pixel);
                    }
                    png.writeRow(line, y);
                }
            }
            png.end();
            return os.buffer().flip();
        } catch (IOException e) {
            LOGGER.atError().setCause(e).log("buffer couldn't be closes");
        }
        return null;
    }

    /**
     * Encodes a JavaFx image as an RGB png image. If you pass in a ByteBuffer to
     * use, please make sure that it has enough capacity to fit the encoded image or
     * handle errors (IndexOutOfBoundsException) accordingly. If you want to be on
     * the safe side, use {@link #getCompressedSizeBound(int, int, boolean)
     * getCompressedSizeBound(width, height, alpha)} to get an upper bound for the
     * output size.
     *
     * @param image            The input image to be encoded
     * @param byteBuffer       optional byte buffer to store the output in, pass
     *                         null to return a new one.
     * @param alpha            whether to include alpha information in the image
     * @param compressionLevel {@link Deflater#BEST_COMPRESSION} (9) to
     *                         {@link Deflater#BEST_SPEED} (0)
     * @param metaInfo         an optional map which will be filled with debugging
     *                         information like compression efficiency
     * @return a byte buffer with the encoded image
     * @see "https://tools.ietf.org/html/rfc2083"
     */
    public static ByteBuffer encodeAlt(final Image image, final ByteBuffer byteBuffer, final boolean alpha, final int compressionLevel, final Map<String, Object> metaInfo) {
        if (image == null) {
            throw new IllegalArgumentException(IMAGE_MUST_NOT_BE_NULL);
        }
        // get meta info
        // get necessary helper classes
        final PixelReader pr = image.getPixelReader();
        if (pr == null) {
            throw new IllegalStateException(IMAGE_PIXEL_READER_NOT_AVAILABLE);
        }
        final int w = (int) image.getWidth();
        final int h = (int) image.getHeight();

        // allocate output buffer if necessary. conservative allocation, assume upper
        // bound for deflate algorithm
        final ByteBuffer outputByteBuffer = byteBuffer == null ? ByteBuffer.allocate(getCompressedSizeBound(w, h, alpha)) : byteBuffer;
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

    public static ByteBuffer encodePalette(final Image image, final ByteBuffer byteBuffer, final boolean alpha, final int compressionLevel, final FilterType filterType, final PaletteQuantizer... userPalette) { // NOPMD w.r.t path complexity
        if (image == null) {
            throw new IllegalArgumentException(IMAGE_MUST_NOT_BE_NULL);
        }
        // get meta info
        final int w = (int) image.getWidth();
        final int h = (int) image.getHeight();
        final int nPixel = w * h;
        final int[] uncompressedImageData = ArrayCache.getCachedIntArray(INTERNAL_ARRAY_CACHE_NAME, nPixel);
        copyImageDataToPixelBuffer(image, uncompressedImageData);

        PaletteQuantizer palette = userPalette.length > 0 && userPalette[0] != null ? userPalette[0] : estimatePalette(uncompressedImageData, w, h, alpha, DEFAULT_PALETTE_COLOR_COUNT);
        // add in addition ~ 256*(3..4) bytes to store palette info
        // N.B. alpha with palette has a bug, thus forcing it here
        final int bytesPerPixel = 3; // (alpha ? 4 : 3);
        final int bytesPerPalette = bytesPerPixel * palette.getColorCount();
        final int requiredSize = getCompressedSizeBound(w, h, alpha) + bytesPerPalette;
        final ByteBuffer outputByteBuffer = byteBuffer == null ? ByteBuffer.allocate(requiredSize) : byteBuffer;
        try (ByteBufferOutputStream os = new ByteBufferOutputStream(outputByteBuffer, false)) {
            // N.B. alpha with palette has a bug, thus forcing it here
            ImageInfo imageInfo = new ImageInfo(w, h, 8, false /*alpha*/, false, true);
            PngWriter pngWriter = new PngWriter(os, imageInfo);
            pngWriter.getPixelsWriter().setFilterType(filterType);
            pngWriter.setIdatMaxSize(requiredSize > 2 * 0x10000 ? 0x10000 : 32_000);
            pngWriter.setCompLevel(compressionLevel);

            preparePaletteHeader(pngWriter, palette);

            final int[] lineArray = ArrayCache.getCachedIntArray(INTERNAL_LINE_ARRAY_CACHE_NAME, w);
            final ImageLineInt line = new ImageLineInt(pngWriter.imgInfo, lineArray);
            if (alpha) {
                int row = 0;
                for (int i = 0; i < nPixel; i++) {
                    final int lineIndex = i % w;
                    final int pixel = uncompressedImageData[i];
                    lineArray[lineIndex] = palette.lookup(pixel >> 16 & 0xFF, pixel >> 8 & 0xFF, pixel & 0xFF, pixel >> 24 & 0xFF);
                    if (lineIndex == w - 1) {
                        pngWriter.writeRow(line, row++);
                    }
                }
            } else {
                for (int i = 0; i < nPixel; i++) {
                    final int lineIndex = i % w;
                    final int pixel = uncompressedImageData[i];
                    lineArray[lineIndex] = palette.lookup(pixel >> 16 & 0xFF, pixel >> 8 & 0xFF, pixel & 0xFF);
                    if (lineIndex == w - 1) {
                        pngWriter.writeRow(line);
                    }
                }
            }
            pngWriter.end();
            ArrayCache.release(INTERNAL_ARRAY_CACHE_NAME, uncompressedImageData);
            ArrayCache.release(INTERNAL_LINE_ARRAY_CACHE_NAME, lineArray);
            return os.buffer().flip();
        } catch (IOException e) {
            LOGGER.atError().setCause(e).log("buffer couldn't be closed");
        }
        return null;
    }

    public static PaletteQuantizer estimatePalette(final Image image, final boolean alpha, final int nColors) {
        if (image == null) {
            throw new IllegalArgumentException(IMAGE_MUST_NOT_BE_NULL);
        }
        // get meta info
        final PixelReader pr = image.getPixelReader();
        if (pr == null) {
            throw new IllegalStateException(IMAGE_PIXEL_READER_NOT_AVAILABLE);
        }
        final int w = (int) image.getWidth();
        final int h = (int) image.getHeight();

        PaletteQuantizerNeuQuant cuant = new PaletteQuantizerNeuQuant(w, h, (x, y) -> pr.getArgb(y, x));
        cuant.setParReserveAlphaColor(alpha);
        cuant.setParNcolors(nColors);
        cuant.run();

        return cuant;
    }

    public static PaletteQuantizer estimatePalette(final int[] pixelArray, final int width, final int heigth, final boolean alpha, final int nColors) {
        if (pixelArray == null) {
            throw new IllegalArgumentException("pixelArray must not be null");
        }

        if (pixelArray.length < width * heigth) {
            throw new IllegalArgumentException("pixelArray.length(" + pixelArray.length + " must be >= " + (width * heigth) + " = " + width + " (width) x" + heigth + " (height)");
        }

        PaletteQuantizerNeuQuant cuant = new PaletteQuantizerNeuQuant(width, heigth, (x, y) -> pixelArray[x * width + y]);
        cuant.setParReserveAlphaColor(alpha);
        cuant.setParNcolors(nColors);
        cuant.run();

        return cuant;
    }

    /**
     * Returns the conservative upper bound for the compressed image size.
     *
     * @param width  Image width
     * @param height Image height
     * @param alpha  Alpha enabled
     * @return the upper bound for the size of the resulting png in bytes
     * @see <a href=
     *      "https://github.com/madler/zlib/blob/master/deflate.c#L659-L661">zlib
     *      sourcefor deflate upper bound</a>
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
     * @param file  The filename to save the image to.
     * @throws IOException if the file cannot be written
     */
    public static void savePng(final Image image, final File file) throws IOException {
        try (OutputStream os = Files.newOutputStream(file.toPath())) {
            final ByteBuffer buffer = WriteFxImage.encode(image);
            os.write(buffer.array(), 0, buffer.limit());
        }
    }

    private static void preparePaletteHeader(PngWriter pngWriter, PaletteQuantizer cuant) {
        // create palette
        PngChunkPLTE palette = pngWriter.getMetadata().createPLTEChunk();
        final int ncolors = cuant.getColorCount();
        palette.setNentries(ncolors);
        for (int i = 0; i < ncolors; i++) {
            int[] col = cuant.getColor(i);
            palette.setEntry(i, col[0], col[1], col[2]);
        }

        int transparentIndex = cuant.getTransparentIndex();
        if (transparentIndex >= 0) {
            PngChunkTRNS transparent = new PngChunkTRNS(pngWriter.imgInfo);
            transparent.setIndexEntryAsTransparent(transparentIndex);
            pngWriter.getChunksList().queue(transparent);
        }
    }

    /**
     * Writes a byte array to the buffer and updates the checksum.
     *
     * @param b      byteArray to put into buffer
     * @param buffer output buffer
     * @param crc    checksum calculator
     */
    private static void write(final byte[] b, final ByteBuffer buffer, final CRC32 crc) {
        buffer.put(b);
        crc.update(b);
    }

    /**
     * Writes an integer value to the buffer and updates the checksum.
     *
     * @param i      integer value to write
     * @param buffer the buffer to write to
     * @param crc    the checksum to be updated
     */
    private static void write(final int i, final ByteBuffer buffer, final CRC32 crc) {
        final byte[] b = { (byte) (i >> 24 & 0xff), (byte) (i >> 16 & 0xff), (byte) (i >> 8 & 0xff), (byte) (i & 0xff) };
        write(b, buffer, crc);
    }

    /**
     * Writes the IDAT chunk of a png file, which contains the compressed image data
     * to the supplied buffer.
     *
     * @param pr               PixelWriter
     * @param w                with of the image
     * @param h                height of the image
     * @param alpha            whether to include alpha information
     * @param outputByteBuffer the byte buffer to write to
     * @param crc              checksum calculator
     */
    private static void writeImageData(final PixelReader pr, final int w, final int h, final boolean alpha, final Deflater compressor, final ByteBuffer outputByteBuffer, final CRC32 crc) {
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
                    uncompressedImageData[i++] = (byte) (pixel >> 16 & 0xff); // red
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
                    uncompressedImageData[i++] = (byte) (pixel >> 16 & 0xff); // red
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
     * @param crc              The checksum calculator
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
     * @param width            The with of the image
     * @param height           The height of the image
     * @param alpha            whether to support alpha
     * @param outputByteBuffer the buffer to write into
     * @param crc              the checksum calculator
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
        // Bit depth: 1 byte 1*,2*,4*,8,16° (*:indexed only, °: not indexed)
        // Color type: 1 byte 0 (grayscale), 2 (RGB), 3 (Indexed), 4 (grayscale+alpha),
        // and 6 (RGBA).
        // Compression method: 1 byte 0 (deflate/inflate compression with a 32K sliding
        // window)
        // Filter method: 1 byte 0 (adaptive filtering with five basic filter types)
        // Interlace method: 1 byte 0 (no interlace) or 1 (Adam7 interlace)
        write(alpha ? new byte[] { 8, 6, 0, 0, 0 } : new byte[] { 8, 2, 0, 0, 0 }, outputByteBuffer, crc); // RGB(A) Mode
        outputByteBuffer.putInt((int) crc.getValue());
    }
}
