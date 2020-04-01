package de.gsi.chart.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes a JavaFx Image into a ByteBuffer or file
 * TODO: possible improvements
 * - use multiple IDAT chunks to use fixed buffer size and limit memory footprint
 * - implement filtering for smaller file sizes
 * - Optionally add tEXT chunks for metadata (EXIF)
 *
 * @author Alexander Krimm
 */
public class WriteFxImage {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteFxImage.class);
    private static final int HEADER_SIZE = 8 + (12 + 13) + 12 + 12; // size of all the headers and other Metadata
    private static final int BUFFER_MARGIN = 200; // how many additional bytes to allocate for the buffers

    /**
     * private constructor for static utility class
     */
    private WriteFxImage() {
    }

    /**
     * Encodes a JavaFx image as an RGB png image
     *
     * @param image an javafx.scene.image.Image
     * @return a byte buffer with the encoded image
     * @see "https://tools.ietf.org/html/rfc2083"
     */
    public static ByteBuffer encode(final Image image) {
        return encode(image, null);
    }

    /**
     * Encodes a JavaFx image as an RGB png image.
     * If you pass in a ByteBuffer to use, please make sure that it has enough capacity to fit the encoded image or
     * handle errors (IndexOutOfBoundsException) accordingly.
     * 
     * @param image an javafx.scene.image.Image
     * @param byteBuffer optional byte buffer to store the output in, pass null to return a new one.
     * @return a byte buffer with the encoded image
     * @see "https://tools.ietf.org/html/rfc2083"
     */
    public static ByteBuffer encode(final Image image, final ByteBuffer byteBuffer) {
        if (byteBuffer != null) {
            byteBuffer.clear();
        }
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        // conservative allocation, assume zero compression
        final ByteBuffer outputByteBuffer = byteBuffer == null ? ByteBuffer.allocate((w + 1) * h * 3 + HEADER_SIZE + BUFFER_MARGIN) : byteBuffer;
        final PixelReader pr = image.getPixelReader();
        CRC32 crc = new CRC32();

        // File Signature - "\211PNG\r\n\032\n" - 8950 4e47 0d0a 1a0a
        outputByteBuffer.put(new byte[] { 0x89 - 0xff - 1, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a });

        // IHDR
        write(13, outputByteBuffer); // length of IHDR chunk
        crc.reset(); // checksum starts after length field
        write("IHDR".getBytes(), outputByteBuffer, crc);
        write(w, outputByteBuffer, crc); // with 4 bytes
        write(h, outputByteBuffer, crc); // height 4 bytes
        // Bit depth:          1 byte  1*,2*,4*,8,16° (*:indexed only, °: not indexed)
        // Color type:         1 byte  0 (grayscale), 2 (RGB), 3 (Indexed), 4 (grayscale+alpha), and 6 (RGBA).
        // Compression method: 1 byte  0 (deflate/inflate compression with a 32K sliding window)
        // Filter method:      1 byte  0 (adaptive filtering with five basic filter types)
        // Interlace method:   1 byte  0 (no interlace) or 1 (Adam7 interlace)
        write(new byte[] { 8, 2, 0, 0, 0 }, outputByteBuffer, crc); // RGB Mode
        write((int) crc.getValue(), outputByteBuffer);

        int sizePos = outputByteBuffer.position();
        write(0, outputByteBuffer); // zero size, will later be overwritten when we know the size
        crc.reset();
        write("IDAT".getBytes(), outputByteBuffer, crc);
        // compress image data
        final Deflater compressor = new Deflater(Deflater.BEST_COMPRESSION);
        final ByteBuffer uncompressedImageData = ByteBuffer.allocate((w + 1) * h * 3);
        compressor.setInput(uncompressedImageData);
        for (int y = 0; y < h; y++) {
            uncompressedImageData.put((byte) 0); // line filtering: 0: None 1: Sub 2: Up 3: Average 4: Paeth
            for (int x = 0; x < w; x++) {
                int pixel = pr.getArgb(x, y);
                uncompressedImageData.put((byte) ((pixel >> 16) & 0xff));
                uncompressedImageData.put((byte) ((pixel >> 8) & 0xff));
                uncompressedImageData.put((byte) (pixel & 0xff));
            }
        }
        // Does not work because rgb pixel format is not writable even if isWritable says so -.-
        // final WritablePixelFormat<ByteBuffer> pixelformat = (WritablePixelFormat<ByteBuffer>) PixelFormat
        //         .getByteRgbInstance();
        // uncompressedImageData.put((byte) 0); // line filtering: 0: None 1: Sub 2: Up 3: Average 4: Paeth
        // pr.getPixels(0, 0, w, h, pixelformat, uncompressedImageData, w + 1); // stride of w+1 puts line filtering to 0(None)
        uncompressedImageData.flip();
        compressor.finish();
        compressor.deflate(outputByteBuffer, Deflater.FULL_FLUSH);
        // update crc
        crc.update(outputByteBuffer.array(), sizePos + 8, compressor.getTotalOut());
        outputByteBuffer.putInt(sizePos, compressor.getTotalOut());
        write((int) crc.getValue(), outputByteBuffer);

        // IEND chunk
        write(0, outputByteBuffer);
        crc.reset();
        write("IEND".getBytes(), outputByteBuffer, crc);
        write((int) crc.getValue(), outputByteBuffer);

        outputByteBuffer.flip();

        // DEGBUG output
        if (LOGGER.isDebugEnabled()) {
            final float compression = (100.0f * compressor.getBytesWritten()) / compressor.getBytesRead();
            LOGGER.atDebug() //
                    .addArgument(w)
                    .addArgument(h) //
                    .addArgument(compressor.getBytesRead()) //
                    .addArgument(compressor.getBytesWritten()) //
                    .addArgument(compression) //
                    .addArgument(outputByteBuffer.limit() - outputByteBuffer.position()) //
                    .log("Convertet Image({}x{}) to png: IN: {} bytes, OUT: {} bytes, compression: {}%, total file size: {} bytes");
        }

        return outputByteBuffer;
    }

    /**
     * Saves the given Image as a png file.
     *
     * @param image The image to save
     * @param file The filename to save the image to.
     * @throws IOException if the file cannot be written
     */
    public static void savePng(Image image, File file) throws IOException {
        try (FileOutputStream os = new FileOutputStream(file); FileChannel wChannel = os.getChannel()) {
            ByteBuffer buffer = WriteFxImage.encode(image);
            wChannel.write(buffer);
        }
    }

    /**
     * Writes a byte array to the buffer and updates the checksum.
     *
     * @param b
     * @param buffer
     * @param crc
     */
    private static void write(byte b[], ByteBuffer buffer, CRC32 crc) {
        buffer.put(b);
        crc.update(b);
    }

    /**
     * Writes an integer value to the buffer.
     *
     * @param i integer value to write
     * @param buffer the buffer to write to
     */
    private static void write(int i, ByteBuffer buffer) {
        byte b[] = { (byte) ((i >> 24) & 0xff), (byte) ((i >> 16) & 0xff), (byte) ((i >> 8) & 0xff),
            (byte) (i & 0xff) };
        buffer.put(b);
    }

    /**
     * Writes an integer value to the buffer and updates the checksum.
     *
     * @param i integer value to write
     * @param buffer the buffer to write to
     * @param crc the checksum to be updated
     */
    private static void write(int i, ByteBuffer buffer, CRC32 crc) {
        byte b[] = { (byte) ((i >> 24) & 0xff), (byte) ((i >> 16) & 0xff), (byte) ((i >> 8) & 0xff),
            (byte) (i & 0xff) };
        write(b, buffer, crc);
    }
}
