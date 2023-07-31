package io.fair_acc.sample.financial.service;

import static io.fair_acc.sample.financial.service.SimpleOhlcvReplayDataSet.DataInput.OHLC_TICK;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.fair_acc.dataset.events.ChartBits;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.dataset.spi.financial.OhlcvDataSet;
import io.fair_acc.dataset.spi.financial.api.attrs.AttributeModelAware;
import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcvItemAware;
import io.fair_acc.sample.financial.dos.DefaultOHLCV;
import io.fair_acc.sample.financial.dos.Interval;
import io.fair_acc.sample.financial.dos.OHLCVItem;
import io.fair_acc.sample.financial.service.consolidate.IncrementalOhlcvConsolidation;
import io.fair_acc.sample.financial.service.consolidate.OhlcvConsolidationAddon;
import io.fair_acc.sample.financial.service.consolidate.OhlcvTimeframeConsolidation;
import io.fair_acc.sample.financial.service.period.IntradayPeriod;

/**
 * Very simple financial OHLC replay data set.
 * The service is used just for simple testing of OHLC chart changes and performance.
 *
 * @author afischer
 */
public class SimpleOhlcvReplayDataSet extends OhlcvDataSet implements Iterable<IOhlcvItem>, IOhlcvItemAware, AttributeModelAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleOhlcvReplayDataSet.class);

    private static final String DATA_SOURCE_OHLC_TICK = "NQ-201609-GLOBEX";

    private static final String DATA_SOURCE_PATH = "chartfx-samples/target/classes/io/fair_acc/sample/chart/financial/%s.scid";

    private final transient DoubleProperty replayMultiply = new SimpleDoubleProperty(this, "replayMultiply", 1.0);

    private DataInput inputSource = OHLC_TICK;
    private String resource;
    protected transient DefaultOHLCV ohlcv;

    protected AtomicBoolean running = new AtomicBoolean(false);
    protected AtomicBoolean paused = new AtomicBoolean(false);
    protected final transient Object pauseSemaphore = new Object();

    protected transient SCIDByNio scid;
    protected transient TickOhlcvDataProvider tickOhlcvDataProvider;
    protected transient IncrementalOhlcvConsolidation consolidation;

    protected transient Set<OhlcvChangeListener> ohlcvChangeListeners = new LinkedHashSet<>();

    protected int maxXIndex = 0;

    public enum DataInput {
        OHLC_TICK
    }

    public SimpleOhlcvReplayDataSet(DataInput dataInput, IntradayPeriod period, Interval<Calendar> timeRange,
            Interval<Calendar> tt, Calendar replayFrom, Map<String, OhlcvConsolidationAddon[]> addons) {
        super(dataInput.name());
        setInputSource(dataInput);
        fillTestData(period, timeRange, tt, replayFrom, addons); // NOPMD
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(SimpleOhlcvReplayDataSet.class.getSimpleName()).log("started '{}'");
        }
    }

    public void addOhlcvChangeListener(OhlcvChangeListener ohlcvChangeListener) {
        ohlcvChangeListeners.add(ohlcvChangeListener);
    }

    public void fillTestData(IntradayPeriod period, Interval<Calendar> timeRange, Interval<Calendar> tt, Calendar replayFrom, Map<String, OhlcvConsolidationAddon[]> addons) {
        lock().writeLockGuard(
                () -> {
                    try {
                        if (getInputSource() == OHLC_TICK) {
                            resource = DATA_SOURCE_OHLC_TICK;
                        }
                        // create services
                        scid = new SCIDByNio();
                        scid.openNewChannel(String.format(DATA_SOURCE_PATH, resource));
                        tickOhlcvDataProvider = scid.createTickDataReplayStream(timeRange, replayFrom.getTime(), replayMultiply);

                        ohlcv = new DefaultOHLCV();
                        ohlcv.setTitle(resource);

                        consolidation = OhlcvTimeframeConsolidation.createConsolidation(period, tt, addons);

                        setData(ohlcv);
                        // try first tick in the fill part
                        tick();

                    } catch (TickDataFinishedException e) {
                        LOGGER.info(e.getMessage());
                    } catch (ClosedChannelException e) {
                        LOGGER.info("The ticker resource was closed already.");
                    } catch (Exception e) {
                        throw new IllegalArgumentException(e.getMessage(), e);
                    }
                });
    }

    protected void tick() throws Exception {
        OHLCVItem increment = tickOhlcvDataProvider.get();
        consolidation.consolidate(ohlcv, increment);
        // recalculate limits
        if (maxXIndex < ohlcv.size()) {
            maxXIndex = ohlcv.size();
            // best performance solution
            getAxisDescription(DIM_X).set(get(DIM_X, 0), get(DIM_X, maxXIndex - 1));
        }
        // notify last tick listeners
        fireOhlcvTickEvent(increment);
    }

    protected void fireOhlcvTickEvent(IOhlcvItem ohlcvItem) throws Exception {
        for (OhlcvChangeListener listener : ohlcvChangeListeners) {
            listener.tickEvent(ohlcvItem);
        }
    }

    public String getResource() {
        return resource;
    }

    public DataInput getInputSource() {
        return inputSource;
    }

    public void setInputSource(DataInput inputSource) {
        this.inputSource = inputSource;
    }

    /**
     * pause/resume play back of the data source via the sound card
     */
    public void pauseResume() {
        if (paused.get()) {
            paused.set(false);
            synchronized (pauseSemaphore) {
                pauseSemaphore.notifyAll();
            }
        } else {
            paused.set(true);
        }
    }

    /**
     * Update replay interval
     * @param updatePeriod replay multiple 1.0-N
     */
    public void setUpdatePeriod(final double updatePeriod) {
        replayMultiply.set(updatePeriod);
        if (!running.get()) {
            start();
        }
    }

    /**
     * starts play back of the data source via the sound card
     */
    public void start() {
        paused.set(false);
        running.set(true);
        new Thread(getDataUpdateTask()).start();
    }

    public void step() {
        getDataUpdateTask().run();
    }

    /**
     * stops and resets play back of the data source via the sound card
     */
    public void stop() {
        if (running.get()) {
            running.set(false);
            if (paused.get()) {
                pauseResume();
            }
            try {
                if (scid != null) {
                    scid.closeActualChannel();
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    protected Runnable getDataUpdateTask() {
        return () -> {
            while (running.get()) {
                try {
                    tick();
                    fireInvalidated(ChartBits.DataSetData);
                    // pause simple support
                    while (paused.get()) {
                        synchronized (pauseSemaphore) {
                            pauseSemaphore.wait(TimeUnit.MILLISECONDS.toMillis(25));
                        }
                    }
                } catch (TickDataFinishedException e) {
                    stop();
                } catch (ClosedChannelException e) {
                    LOGGER.info("The OHLCV data channel is already closed.");
                } catch (Exception e) { // NOSONAR NOPMD
                    throw new IllegalArgumentException(e);
                }
            }
        };
    }
}
