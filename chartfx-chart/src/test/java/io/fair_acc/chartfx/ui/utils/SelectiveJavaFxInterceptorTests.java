package io.fair_acc.chartfx.ui.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;

/**
 * Tests for {@link JavaFXInterceptorUtils.SelectiveJavaFxInterceptor}.
 *
 * @author rstein
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class SelectiveJavaFxInterceptorTests {
    @Start
    public void start(final Stage stage) {
        // usual FX initialisation
        // ...
        assertTrue(Platform.isFxApplicationThread());
        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @TestFx // forces execution in JavaFX thread
    public void testJavaFxThreadSafety() {
        // verifies that this test is indeed executed in the JavaFX thread
        assertTrue(Platform.isFxApplicationThread());

        // perform the regular JavaFX thread safe assertion tests
        // ...
    }

    @Test // explicitly not executed in JavaFX thread; for different behaviour use: {@code @ExtendWith(JavaFxInterceptor.class)
    public void testNonJavaFx() {
        // verifies that this test is not executed within the JavaFX thread
        assertFalse(Platform.isFxApplicationThread());

        // perform the regular non-JavaFX thread-related assertion tests
        // ...
    }
}
