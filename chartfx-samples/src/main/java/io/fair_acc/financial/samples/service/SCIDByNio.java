package io.fair_acc.financial.samples.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import javafx.beans.property.DoubleProperty;

import org.jetbrains.annotations.NotNull;

import io.fair_acc.financial.samples.dos.Interval;
import io.fair_acc.financial.samples.dos.OHLCVItem;

/**
 * Create OHLCV from Sierra Chart SCID files (intraday tick format).
 *
 * @author afischer
 */
public class SCIDByNio implements AutoCloseable {
    private FileInputStream fileInputStream;
    private FileChannel fileChannel;
    private ByteBuffer bufferRecordDouble;
    private ByteBuffer bufferRecordFloat;
    private ByteBuffer bufferRecordULong;
    private final Calendar cal = Calendar.getInstance();
    private int timeZone;

    @SuppressWarnings({ "lgtm[java/output-resource-leak" })
    public void openNewChannel(String resource) throws IOException {
        timeZone = cal.get(Calendar.ZONE_OFFSET);

        fileInputStream = new FileInputStream(resource); // lgtm[java/output-resource-leak]
        fileChannel = fileInputStream.getChannel();

        bufferRecordDouble = ByteBuffer.allocate(8);
        bufferRecordDouble.order(ByteOrder.LITTLE_ENDIAN);

        bufferRecordFloat = ByteBuffer.allocate(4);
        bufferRecordFloat.order(ByteOrder.LITTLE_ENDIAN);

        bufferRecordULong = ByteBuffer.allocate(4);
        bufferRecordULong.order(ByteOrder.LITTLE_ENDIAN);

        fileChannel.position(56);
    }

    public void closeActualChannel() throws IOException {
        if (fileChannel.isOpen()) {
            fileChannel.close();
        }
        fileInputStream.close();
    }

    @Override
    public void close() throws Exception {
        closeActualChannel();
    }

    /**
     * Find position which if FIRST or equaled after you inserted timestamp.
     * Check if the position is negative. If the position is negative it is first position
     * after your required timestamp. Beware if the position is higher that maximal position.
     * Usage of Binary Search algorithm
     *
     * @param timestamp Date
     * @return file position of timestamp of record
     * @throws IOException if reading of file failed
     */
    public long findPositionByTimestamp(Date timestamp) throws IOException {
        // usage of binary search
        long lo = 56;
        long hi = fileChannel.size() - 40;
        long mid;
        while (lo <= hi) {
            // Key is in a[lo..hi] or not present.
            mid = lo + (hi - lo) / 2;
            mid = ((mid - 56) / 40) * 40 + 56; // recalculate for nearest timestamp
            Date midTimestamp = loadTimestamp(mid);
            if (timestamp.before(midTimestamp)) {
                hi = mid - 40;
            } else if (timestamp.after(midTimestamp)) {
                lo = mid + 40;
            } else
                return mid;
        }
        return -lo;
    }

    /**
     * Return first or equaled position for required position result
     *
     * @param timestamp Date
     * @return modified position long
     * @throws IOException if reading of file failed
     */
    public long ensureNearestTimestampPosition(Date timestamp) throws IOException {
        long position = findPositionByTimestamp(timestamp);

        if (position > 0) {
            return position;
        }
        position = Math.abs(position);

        long positionEnd = fileChannel.size() - 40;
        return Math.min(position, positionEnd);
    }

    /**
     * Create instance of tick ohlcv data provider for replay stream
     *
     * @param requiredTimestamps [from, to] interval
     * @param replayStarTime     Date - point of replay timing start
     * @param replaySpeed        multiply of replay simulation (with real timing!)
     * @return tick data provider
     * @throws IOException if reading of file failed
     */
    public TickOhlcvDataProvider createTickDataReplayStream(@NotNull final Interval<Calendar> requiredTimestamps,
            @NotNull final Date replayStarTime, DoubleProperty replaySpeed) throws IOException {
        // define boundaries of loaded data
        final long positionStart = ensureNearestTimestampPosition(requiredTimestamps.from.getTime());
        final long positionEnd = ensureNearestTimestampPosition(requiredTimestamps.to.getTime());
        final long ohlcvReplayStartIndex = ensureNearestTimestampPosition(replayStarTime);

        // initialization settings
        fileChannel.position(positionStart);

        return new TickOhlcvDataProvider() {
            private OHLCVItem prevItem = null;
            private OHLCVItem item = null;

            @Override
            public OHLCVItem get() throws TickDataFinishedException, IOException {
                long position = fileChannel.position();
                if (positionEnd != -1 && position >= positionEnd) {
                    throw new TickDataFinishedException("The replay finished.");
                }
                if (position >= ohlcvReplayStartIndex) {
                    long prevTime = prevItem != null ? prevItem.getTimeStamp().getTime() : 0L;
                    long time = item != null ? item.getTimeStamp().getTime() : 0L;
                    long waitingTime = Math.round((time - prevTime) / replaySpeed.get());
                    waitingTime = Math.max(1, waitingTime);
                    try {
                        // waiting to send next sample - simulation of replay processing
                        Thread.sleep(waitingTime);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
                prevItem = item;
                item = loadOhlcvItemRealtime();

                return item;
            }
        };
    }

    /**
     * Base method for reading of interval ohlcv item in the realtime mode
     *
     * @return domain object
     * @throws IOException if reading of file failed
     */
    private OHLCVItem loadOhlcvItemRealtime() throws IOException {
        double dt;
        float open;
        float high;
        float low;
        float close;

        long totalVolume;
        long bidVolume;
        long askVolume;

        int bytesRead;
        do {
            bytesRead = fileChannel.read(bufferRecordDouble);
            if (bytesRead == -1) {
                // wait for new realtime data
                synchronized (this) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(25)); // default is 25ms
                }
            }
        } while (bytesRead == -1);

        // timestamp
        bufferRecordDouble.flip();
        dt = bufferRecordDouble.getDouble();
        bufferRecordDouble.clear();

        // open
        // In Sierra Chart version 1150 and higher, in the case where the data
        // record holds 1 tick/trade of data, the Open will be equal to 0.
        if (fileChannel.read(bufferRecordFloat) < 0) {
            throw new IOException("could not read open values");
        }
        bufferRecordFloat.clear();

        // high
        if (fileChannel.read(bufferRecordFloat) < 0) {
            throw new IOException("could not read high values");
        }
        bufferRecordFloat.flip();
        high = bufferRecordFloat.getFloat();
        bufferRecordFloat.clear();

        // low
        if (fileChannel.read(bufferRecordFloat) < 0) {
            throw new IOException("could not read low values");
        }
        bufferRecordFloat.flip();
        low = bufferRecordFloat.getFloat();
        bufferRecordFloat.clear();

        // close
        if (fileChannel.read(bufferRecordFloat) < 0) {
            throw new IOException("could not read close values");
        }
        bufferRecordFloat.flip();
        close = bufferRecordFloat.getFloat();
        open = close; // tick data only!
        bufferRecordFloat.clear();

        // number of trades
        if (fileChannel.read(bufferRecordULong) < 0) {
            throw new IOException("could not read number of trade values");
        }
        bufferRecordULong.flip();
        bufferRecordULong.getInt();
        bufferRecordULong.clear();

        // total volume
        if (fileChannel.read(bufferRecordULong) < 0) {
            throw new IOException("could not read total volume values");
        }
        bufferRecordULong.flip();
        totalVolume = bufferRecordULong.getInt();
        bufferRecordULong.clear();

        // bid volume
        if (fileChannel.read(bufferRecordULong) < 0) {
            throw new IOException("could not read bid volume values");
        }
        bufferRecordULong.flip();
        bidVolume = bufferRecordULong.getInt();
        bufferRecordULong.clear();

        // ask volume
        if (fileChannel.read(bufferRecordULong) < 0) {
            throw new IOException("could not read ask volume values");
        }
        bufferRecordULong.flip();
        askVolume = bufferRecordULong.getInt();
        bufferRecordULong.clear();

        // timestamp conversion to date structure
        var timestamp = new Date(convertWindowsTimeToMilliseconds(dt));

        // assembly one ohlcv item domain object
        return new OHLCVItem(timestamp, open, high, low, close, totalVolume, 0, askVolume, bidVolume);
    }

    /**
     * Load timestamp of the required position
     *
     * @param position in file
     * @return timestamp
     * @throws IOException if reading of file failed
     */
    private Date loadTimestamp(long position) throws IOException {
        double dt;

        fileChannel.position(position);
        int bytesRead = fileChannel.read(bufferRecordDouble);
        if (bytesRead == -1) {
            throw new IOException("could not read time-stamp for position: " + position);
        }
        bufferRecordDouble.flip();
        dt = bufferRecordDouble.getDouble();
        bufferRecordDouble.clear();

        return new Date(convertWindowsTimeToMilliseconds(dt));
    }

    /**
     * Thanks to @see
     * http://svn.codehaus.org/groovy/modules/scriptom/branches/SCRIPTOM
     * -1.5.4-ANT/src/com/jacob/com/DateUtilities.java
     *
     * @param comTime time in windows time for convert to java format
     * @return java format of windows format with usage of specific timezone
     */
    public long convertWindowsTimeToMilliseconds(double comTime) {
        comTime = comTime - 25569D;
        long result = Math.round(86400000L * comTime) - timeZone;
        cal.setTime(new Date(result));
        result -= cal.get(Calendar.DST_OFFSET);

        return result;
    }
}
