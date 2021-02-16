package de.gsi.chart.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;

/**
 * Tests {@link de.gsi.chart.renderer.spi.LabelledMarkerRenderer }
 *
 * @author rstein
 *
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class FXUtilsTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(FXUtilsTests.class);
    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;
    private Label testLabel;

    @Start
    public void start(@SuppressWarnings("unused") Stage stage) {
        // needed only to initialize FX UI Thread infrastructure
        testLabel = new Label("test label");
        stage.setScene(new Scene(testLabel, WIDTH, HEIGHT));
        stage.show();
    }

    @TestFx
    public void testWithinFxThread() throws Exception {
        FXUtils.assertJavaFxThread();
        FXUtils.keepJavaFxAlive();

        // execute Runnable in JavaFX thread
        FXUtils.runAndWait(FXUtils::assertJavaFxThread);

        assertTrue(FXUtils.runAndWait(() -> {
            // execute Supplier in JavaFX thread
            FXUtils.assertJavaFxThread();
            return true;
        }));

        assertEquals(42.0, FXUtils.runAndWait(42.0, a -> {
            // execute Function<R,T> in JavaFX thread
            FXUtils.assertJavaFxThread();
            return a;
        }));

        // execute Runnable in runLater in JavaFX thread
        FXUtils.runFX(FXUtils::assertJavaFxThread);

        Awaitility.setDefaultPollDelay(100, TimeUnit.MILLISECONDS);
        Awaitility.pollInSameThread();
        // FXUtils.waitForFxTicks(testLabel.getScene(), 3);
        Awaitility.waitAtMost(200, TimeUnit.MILLISECONDS).until(() -> FXUtils.waitForFxTicks(testLabel.getScene(), 3));

        Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(() -> FXUtils.waitForFxTicks(testLabel.getScene(), 3, 100));

        // test assertions
        assertThrows(IllegalStateException.class, () -> FXUtils.runFX(() -> {
            // execute Runnable with exception in runFX in JavaFX thread
            throw new IllegalStateException("should be caught and swallowed by unit-test");
        }));

        assertThrows(IllegalStateException.class, () -> FXUtils.runAndWait(() -> {
            // execute Runnable with exception in runAndWait in JavaFX thread
            throw new IllegalStateException("should be caught and swallowed by unit-test");
        }));
    }

    @Test
    public void testOutsideFxThread() throws Exception {
        assertFalse(Platform.isFxApplicationThread());

        assertThrows(IllegalStateException.class, FXUtils::assertJavaFxThread);

        // execute Runnable in JavaFX thread
        FXUtils.runAndWait(FXUtils::assertJavaFxThread);

        try {
            final Boolean result = FXUtils.runAndWait(() -> {
                // execute Supplier in JavaFX thread
                try {
                    FXUtils.assertJavaFxThread();
                } catch (Exception e) {
                    LOGGER.atError().setCause(e).log("error in FXUtils.assertJavaFxThread()");
                    return false;
                }
                return true;
            });
            assertNotNull(result, "return value of executed Supplier in JavaFX thread");
            assertTrue(result);
        } catch (Exception e) {
            LOGGER.atError().setCause(e).log("error in FXUtils.runAndWait(() -> { FXUtils.assertJavaFxThread(); ...}");
            throw e;
        }

        assertEquals(42.0, FXUtils.runAndWait(42.0, a -> {
            // execute Function<R,T> in JavaFX thread
            try {
                FXUtils.assertJavaFxThread();
            } catch (Exception e) {
                LOGGER.atError().setCause(e).log("error in FXUtils.assertJavaFxThread()");
                return -1.0;
            }
            return a;
        }));

        // execute Runnable in runLater in JavaFX thread
        FXUtils.runFX(FXUtils::assertJavaFxThread);

        Awaitility.setDefaultPollDelay(100, TimeUnit.MILLISECONDS);
        Awaitility.pollInSameThread();
        // FXUtils.waitForFxTicks(testLabel.getScene(), 3);
        Awaitility.waitAtMost(200, TimeUnit.MILLISECONDS).until(() -> FXUtils.waitForFxTicks(testLabel.getScene(), 3));

        LOGGER.atInfo().log("following [FXUtils-thread] 'FXUtils::waitForTicks(..) interrupted by timeout' warning is the normal library behaviour");
        Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(() -> FXUtils.waitForFxTicks(testLabel.getScene(), 3, 100));

        // check for own time-out
        assertFalse(FXUtils.waitForFxTicks(testLabel.getScene(), 1000, 20));

        // test assertions
        // N.B. the exception thrown in runLater cannot be forwarded to the calling thread (asynchronicity)
        // assertThrows(IllegalStateException.class, () -> FXUtils.runFX(() -> {
        //      LOGGER.atInfo().log("execute Runnable with exception in runFX in JavaFX thread");
        //      throw new IllegalStateException("should be caught and swallowed by unit-test");
        // }));

        assertThrows(IllegalStateException.class, () -> FXUtils.runAndWait(() -> {
            // execute Runnable with exception in runAndWait in JavaFX thread
            throw new IllegalStateException("should be caught and swallowed by unit-test");
        }));
    }
}
