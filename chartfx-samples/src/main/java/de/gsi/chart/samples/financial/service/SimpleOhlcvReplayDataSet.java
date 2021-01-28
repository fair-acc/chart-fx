/**
 * LGPL-3.0, 2020/21, GSI-CS-CO/Chart-fx, BTA HF OpenSource Java-FX Branch, Financial Charts
 */
package de.gsi.chart.samples.financial.service;

import static de.gsi.chart.samples.financial.service.SimpleOhlcvReplayDataSet.DataInput.OHLC_TICK;

import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.samples.financial.dos.DefaultOHLCV;
import de.gsi.chart.samples.financial.dos.Interval;
import de.gsi.chart.samples.financial.dos.OHLCVItem;
import de.gsi.chart.samples.financial.service.consolidate.IncrementalOhlcvConsolidation;
import de.gsi.chart.samples.financial.service.consolidate.OhlcvTimeframeConsolidation;
import de.gsi.chart.samples.financial.service.period.IntradayPeriod;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.spi.financial.OhlcvDataSet;
import de.gsi.dataset.spi.financial.api.attrs.AttributeModelAware;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItemAware;

/**
 * Very simple financial OHLC replay data set.
 * The service is used just for simple testing of OHLC chart changes and performance.
 *
 * @author afischer
 */
public class SimpleOhlcvReplayDataSet extends OhlcvDataSet implements Iterable<IOhlcvItem>, IOhlcvItemAware, AttributeModelAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleOhlcvReplayDataSet.class);

    private static final String DATA_SOURCE_OHLC_TICK = "NQ-201609-GLOBEX";

    private static final String DATA_SOURCE_PATH = "chartfx-samples/target/classes/de/gsi/chart/samples/financial/%s.scid";

    private final DoubleProperty replayMultiply = new SimpleDoubleProperty(this, "replayMultiply", 1.0);

    private DataInput inputSource = OHLC_TICK;
    private String resource;
    protected DefaultOHLCV ohlcv;

    protected volatile boolean running = false;
    protected volatile boolean paused = false;
    protected transient final Object pause = new Object();

    protected transient SCIDByNio scid;
    protected transient TickOhlcvDataProvider tickOhlcvDataProvider;
    protected transient IncrementalOhlcvConsolidation consolidation;

    protected Set<OhlcvChangeListener> ohlcvChangeListeners = new LinkedHashSet<>();

    protected int maxXIndex = 0;

    public enum DataInput {
        OHLC_TICK
    }

    public SimpleOhlcvReplayDataSet(DataInput dataInput, IntradayPeriod period, Interval<Calendar> timeRange, Interval<Calendar> tt, Calendar replayFrom) {
        super(dataInput.name());
        setInputSource(dataInput);
        fillTestData(period, timeRange, tt, replayFrom); // NOPMD
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(SimpleOhlcvReplayDataSet.class.getSimpleName()).log("started '{}'");
        }
    }

    public void addOhlcvChangeListener(OhlcvChangeListener ohlcvChangeListener) {
        ohlcvChangeListeners.add(ohlcvChangeListener);
    }

    public void fillTestData(IntradayPeriod period, Interval<Calendar> timeRange, Interval<Calendar> tt, Calendar replayFrom) {
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

                        consolidation = OhlcvTimeframeConsolidation.createConsolidation(period, tt, null);

                        autoNotification().set(false);
                        setData(ohlcv);
                        // try first tick in the fill part
                        tick();
                        autoNotification().set(true);

                    } catch (TickDataFinishedException e) {
                        LOGGER.info(e.getMessage());

                    } catch (Exception e) {
                        throw new IllegalArgumentException(e.getMessage(), e);
                    }
                });
    }

    protected void tick() throws Exception {
        OHLCVItem increment = tickOhlcvDataProvider.get();
        //lock().writeLockGuard(() -> { // not write lock blinking
        consolidation.consolidate(ohlcv, increment);
        // recalculate limits
        if (maxXIndex < ohlcv.size()) {
            maxXIndex = ohlcv.size();
            // best performance solution
            getAxisDescription(DIM_X).set(get(DIM_X, 0), get(DIM_X, maxXIndex - 1));
        }
        // notify last tick listeners
        fireOhlcvTickEvent(increment);
        //});
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
        if (paused) {
            paused = false;
            synchronized (pause) {
                pause.notify();
            }
        } else {
            paused = true;
        }
    }

    /**
     * Update replay interval
     * @param updatePeriod replay multiple 1.0-N
     */
    public void setUpdatePeriod(final double updatePeriod) {
        replayMultiply.set(updatePeriod);
        if (!running) {
            start();
        }
    }

    /**
     * starts play back of the data source via the sound card
     */
    public void start() {
        paused = false;
        running = true;
        new Thread(getDataUpdateTask()).start();
    }

    public void step() {
        getDataUpdateTask().run();
    }

    /**
     * stops and resets play back of the data source via the sound card
     */
    public void stop() {
        if (running) {
            running = false;
            if (paused) {
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
            while (running) {
                try {
                    tick();
                    fireInvalidated(new AddedDataEvent(SimpleOhlcvReplayDataSet.this, "tick"));
                    // pause simple support
                    if (paused) {
                        try {
                            synchronized (pause) {
                                pause.wait();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (TickDataFinishedException e) {
                    stop();
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };
    }
}
