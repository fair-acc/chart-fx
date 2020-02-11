package de.gsi.dataset.event;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import de.gsi.dataset.event.EventRateLimiter.UpdateStrategy;

/**
 * Tests the EventRateLimiter
 * @author rstein
 */
public class EventRateLimiterTests {
    private static final int MAX_UPDATE_PERIOD = 100;

    @Test
    public void rateLimiterTests() {
        Timer timer = new Timer();
        final TestEventSource evtSource = new TestEventSource();
        final AtomicInteger updateCount1 = new AtomicInteger();
        final AtomicInteger updateCount2 = new AtomicInteger();
        final AtomicInteger updateCount3 = new AtomicInteger();
        final AtomicInteger updateCount4 = new AtomicInteger();

        // create anonymous listener
        evtSource.addListener(evt -> updateCount1.incrementAndGet());
        evtSource.addListener(new EventRateLimiter(evt -> updateCount2.incrementAndGet(), MAX_UPDATE_PERIOD, null));
        evtSource.addListener(new EventRateLimiter(evt -> updateCount3.incrementAndGet(), MAX_UPDATE_PERIOD,
                UpdateStrategy.INSTANTANEOUS_RATE));

        final EventRateLimiter rateLimitAvg = new EventRateLimiter(evt -> updateCount4.incrementAndGet(),
                MAX_UPDATE_PERIOD, UpdateStrategy.AVERAGE_RATE);
        evtSource.addListener(rateLimitAvg);

        assert rateLimitAvg.getRateEstimate() > 0;

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                evtSource.invokeListener();
            }
        }, 10, MAX_UPDATE_PERIOD / 10);

        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> updateCount1.get() >= 100);
        timer.cancel();
        final double rateLimit = rateLimitAvg.getRateEstimate();
        assert updateCount1.get() >= 100;
        assert updateCount2.get() >= 5;
        assert updateCount2.get() <= 15;
        assert updateCount3.get() >= 5;
        assert updateCount3.get() <= 15;
        assert updateCount4.get() >= 5;
        assert updateCount4.get() <= 15;
        assertAll("rate within [5,15] Hz limits", () -> assertTrue(rateLimit >= 5.0, "min limit"),
                () -> assertTrue(rateLimit <= 15.0, "max limit"));

    }

}
