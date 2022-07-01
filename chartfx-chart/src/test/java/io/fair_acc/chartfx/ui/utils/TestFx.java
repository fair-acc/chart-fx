package io.fair_acc.chartfx.ui.utils;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * extension of JUnits @Test annotation to indicate that a particular test should be explicitly executed within the JavaFX thread. 
 * See also {@link JavaFXInterceptorUtils.SelectiveJavaFxInterceptor }
 * @author rstein
 *
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Test
@Tag("TestWithinJavaFX")
public @interface TestFx {
}
