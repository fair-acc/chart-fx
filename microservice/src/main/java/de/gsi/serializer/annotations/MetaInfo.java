package de.gsi.serializer.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.TYPE })
public @interface MetaInfo {
    String unit() default "";
    String description() default "";
    String direction() default "";
    String[] groups() default "";
}