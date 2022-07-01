package io.fair_acc.chartfx.ui.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils.JavaFxInterceptor;

/**
 * Tests for {@link JavaFXInterceptorUtils.JavaFxInterceptor}.
 *
 * @author rstein
 *
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(JavaFxInterceptor.class)
public class JavaFxInterceptorTests {
    @Start
    public void start(final Stage stage) {
        // usual FX initialisation
        // ...
        assertTrue(Platform.isFxApplicationThread());
        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @TestFx // note: this is equivalent to {@code @Test} when using {@code @ExtendWith(JavaFxInterceptor.class)}
    public void testJavaFxThreadSafety() {
        // verifies that this test is indeed executed in the JavaFX thread
        assertTrue(Platform.isFxApplicationThread());

        // perform regular assertion tests within the JavaFX thread
        // ...
    }

    @Test // also executed in JavaFX thread, for different behaviour use: {@code @ExtendWith(SelectiveJavaFxInterceptor.class)
    public void testNonJavaFx() {
        // verifies that this test is indeed executed in the JavaFX thread
        assertTrue(Platform.isFxApplicationThread());

        // perform also the regular assertion tests within the JavaFX thread
        // ...
    }
}
