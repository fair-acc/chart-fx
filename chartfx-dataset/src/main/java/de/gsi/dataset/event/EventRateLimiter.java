package de.gsi.dataset.event;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.gsi.dataset.utils.DoubleCircularBuffer;

/**
 * @author rstein
 */
public class EventRateLimiter implements EventListener {
    private static final int MAX_RATE_BUFFER = 20;
    private static final AtomicInteger serialCounter = new AtomicInteger(0);
    private final Timer timer = new Timer(EventRateLimiter.class.getSimpleName() + serialCounter.getAndIncrement(),
            true);
    private final AtomicBoolean rateLimitActive = new AtomicBoolean(false);
    private final Object lock = new Object();
    private final DoubleCircularBuffer rateEstimatorBuffer = new DoubleCircularBuffer(MAX_RATE_BUFFER);
    private final EventListener eventListener;
    private final long minUpdatePeriod;
    private final double maxUpdateRate;
    private final UpdateStrategy updateStrategy;
    private long lastUpdateMillis;
    private UpdateEvent lastUpdateEvent;

    public EventRateLimiter(final EventListener eventListener, final long minUpdatePeriod,
            final UpdateStrategy updateStrategy) {
        super();
        lastUpdateMillis = System.currentTimeMillis();
        this.eventListener = eventListener;
        this.minUpdatePeriod = minUpdatePeriod;
        maxUpdateRate = 1000.0 / minUpdatePeriod;
        this.updateStrategy = updateStrategy == null ? UpdateStrategy.INSTANTANEOUS_RATE : updateStrategy;
        rateEstimatorBuffer.put(lastUpdateMillis);
    }

    public double getRateEstimate() {
        synchronized (lock) {
            final long now = System.currentTimeMillis();
            if (rateEstimatorBuffer.available() <= 1) {
                final long diff = Math.abs(now - lastUpdateMillis);
                return diff >= 1 ? 1000.0 / diff : 1.0;
            }

            double lastUpate = now;
            final int nData = rateEstimatorBuffer.available();
            double diff = 0.0;
            for (int i = 0; i < nData; i++) {
                final double timeStamp = rateEstimatorBuffer.get(i);
                diff += Math.abs(timeStamp - lastUpate);
                lastUpate = timeStamp;
            }
            final double avgPeriod = diff / nData;
            return 2000.0 / avgPeriod;
        }
    }

    @Override
    public void handle(UpdateEvent event) {
        final long now = System.currentTimeMillis();
        synchronized (lock) {
            lastUpdateEvent = event;
            final long diff = now - lastUpdateMillis;
            boolean suppressUpdate = false;
            switch (updateStrategy) {
            case AVERAGE_RATE:
                suppressUpdate = getRateEstimate() > maxUpdateRate;
                break;
            case INSTANTANEOUS_RATE:
            default:
                suppressUpdate = diff < minUpdatePeriod;
                break;
            }

            if (suppressUpdate) {
                if (rateLimitActive.compareAndSet(false, true)) {
                    timer.schedule(new DelayedUpdateTask(), minUpdatePeriod);
                }
                return;
            }
            rateEstimatorBuffer.put(now);
            lastUpdateMillis = now;
        }

        eventListener.handle(event);
    }

    public enum UpdateStrategy {
        INSTANTANEOUS_RATE,
        AVERAGE_RATE;
    }

    protected class DelayedUpdateTask extends TimerTask {
        @Override
        public void run() {
            rateLimitActive.set(false);
            EventRateLimiter.this.handle(lastUpdateEvent);
        }
    }
}
