package de.gsi.chart.axes.spi;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.utils.FXUtils;

/**
 * Small test to demonstrate that the SoftHashMap TickMark cache sizes are in fact limited, memory-bound and do not leak
 * rather than their earlier WeakHashMap-based counterpart that had issues when the weak key was also part of the kept value.
 *
 * See following references for details:
 * https://ewirch.github.io/2013/12/weakhashmap-memory-leaks.html
 * https://franke.ms/memoryleak1.wiki
 * N.B. latter author filed this as a bug at http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7145759 which was apparently
 * dropped by Oracle devs as supposedly the intended behaviour for a WeakHashMap-based cache.
 *
 * effect is best seen with limiting the jvm's max memory: -Xmx20m
 *
 * @author rstein
 */
public class MemoryLeakTestDefaultAxis extends Application { // NOPMD -nomen est omen
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryLeakTestDefaultAxis.class);
    @Override
    public void start(final Stage primaryStage) {
        final TestAxis axis = new TestAxis();
        VBox.setMargin(axis, new Insets(5, 50, 5, 50));
        VBox.setVgrow(axis, Priority.ALWAYS);

        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            private int counter;
            @Override
            public void run() {
                FXUtils.runFX(() -> axis.set(now(), now() + 1));
                if (counter % 5000 == 0) {
                    LOGGER.atInfo().addArgument(axis.getTickMarkDoubleCache().size()).addArgument(axis.getTickMarkStringCache().size()).log("cache sizes - Map<Double,TickMark> = {} Map<String,TickMark> = {}");
                    System.gc(); // NOPMD NOSONAR - yes we need to eliminate the non-deterministic behaviour of the jvm's gc
                    System.gc(); // NOPMD NOSONAR - yes we need to eliminate the non-deterministic behaviour of the jvm's gc
                }
                counter++;
            }
        }, 0, 1);

        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(new Scene(new VBox(axis), 1800, 100));
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
        primaryStage.show();
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }

    private static double now() {
        return 0.001 * System.currentTimeMillis(); // [s]
    }

    private class TestAxis extends DefaultNumericAxis {
        public TestAxis() {
            super("test axis", now(), now() + 1, 0.05);
        }

        public Map<Double, TickMark> getTickMarkDoubleCache() {
            return tickMarkDoubleCache;
        }

        public Map<String, TickMark> getTickMarkStringCache() {
            return tickMarkStringCache;
        }
    }
}
