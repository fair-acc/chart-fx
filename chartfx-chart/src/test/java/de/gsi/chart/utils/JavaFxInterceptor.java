package de.gsi.chart.utils;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Simple JUnit 5 extension to ensure that {@code @Test} statements are executed in the JavaFX UI thread. 
 * This is (strictly) necessary when testing setter and/or getter methods of JavaFX classes (ie. Node derived, properties etc).
 * <p>
 * Example usage:
 * <pre>{@code
 * @ExtendWith(ApplicationExtension.class)
 * @ExtendWith(JavaFxInterceptor.class)
 * public class SquareButtonTest {
 *     @Start
 *     public void start(Stage stage) {
 *         // usual FX initialisation
 *         // ...
 *     }
 *
 *    @Test
 *    public void testSetterGetter() throws InterruptedException, ExecutionException {
 *        FXUtils.assertJavaFxThread(); // verifies that this test is indeed executed in the JavaFX thread
 *        // perform the regular JavaFX thread safe assertion tests 
 *        // ...
 *    }
 * }
 *
 * }</pre>
 * 
 * @author rstein
 */
public class JavaFxInterceptor implements InvocationInterceptor {
    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        AtomicReference<Throwable> throwable = new AtomicReference<>();

        // N.B. explicit run and wait since the test should only continue
        // if the previous JavaFX access as been finished.
        FXUtils.runAndWait(() -> {
            try {
                // executes function after @Test
                invocation.proceed();
            } catch (Throwable t) {
                throwable.set(t);
            }
        });
        Throwable t = throwable.get();
        if (t != null) {
            throw t;
        }
    }
}