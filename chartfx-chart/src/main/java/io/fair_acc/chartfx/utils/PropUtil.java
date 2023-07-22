package io.fair_acc.chartfx.utils;

import javafx.beans.property.*;

import java.util.Objects;

/**
 * Utility class for working with JavaFX properties
 *
 * @author ennerf
 */
public class PropUtil {

    public static boolean set(DoubleProperty prop, double value) {
        if (prop.get() == value) {
            return false;
        }
        prop.set(value);
        return true;
    }

    public static boolean set(StringProperty prop, String value){
        if (Objects.equals(prop.get(), value)) {
            return false;
        }
        prop.set(value);
        return true;
    }

    public static BooleanProperty createBooleanProperty(Object bean, String name, boolean initial, Runnable onChange) {
        return new SimpleBooleanProperty(bean, name, initial) {
            @Override
            public void set(final boolean newValue) {
                final boolean oldValue = get();
                if (oldValue != newValue) {
                    super.set(newValue);
                    onChange.run();
                }
            }
        };
    }

    public static DoubleProperty createDoubleProperty(Object bean, String name, double initial, Runnable onChange) {
        return new SimpleDoubleProperty(bean, name, initial) {
            @Override
            public void set(final double newValue) {
                final double oldValue = get();
                if (oldValue != newValue) {
                    super.set(newValue);
                    onChange.run();
                }
            }
        };
    }

    public static ReadOnlyDoubleWrapper createReadOnlyDoubleWrapper(Object bean, String name, double initial, Runnable onChange) {
        return new ReadOnlyDoubleWrapper(bean, name, initial) {
            @Override
            public void set(final double newValue) {
                final double oldValue = get();
                if (oldValue != newValue) {
                    super.set(newValue);
                    onChange.run();
                }
            }
        };
    }

    public static ReadOnlyDoubleWrapper createReadOnlyDoubleWrapper(Object bean, String name, double initial) {
        return new ReadOnlyDoubleWrapper(bean, name, initial);
    }

    public static <T> ObjectProperty<T> createObjectProperty(Object bean, String name, T initial, Runnable onChange) {
        return new SimpleObjectProperty<T>(bean, name, initial) {
            @Override
            public void set(final T newValue) {
                final T oldValue = getValue();
                if (oldValue != newValue) {
                    super.set(newValue);
                    onChange.run();
                }
            }
        };
    }
}
