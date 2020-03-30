package de.gsi.chart.ui.utils;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * extension of JUnits @Test annotation to indicate that a particular test should be explicitly executed within the JavaFX thread. 
 * See also {@link de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor }
 * @author rstein
 *
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = STABLE, since = "5.0")
@Test
@Tag("TestWithinJavaFX")
public @interface TestFx {
}
