package io.fair_acc.chartfx.utils;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;

import java.util.Objects;

/**
 * Utility class for working with JavaFX properties
 *
 * @author ennerf
 */
public class PropUtil {

    public static boolean set(DoubleProperty prop, double value) {
        if (isEqual(prop.get(), value)) {
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

    public static BooleanProperty createBooleanProperty(Object bean, String name, boolean initial, Runnable... onChange) {
        return new SimpleBooleanProperty(bean, name, initial) {
            @Override
            public void set(final boolean newValue) {
                final boolean oldValue = get();
                if (oldValue != newValue) {
                    super.set(newValue);
                    for (Runnable action : onChange) {
                        action.run();
                    }
                }
            }
        };
    }

    public static DoubleProperty createDoubleProperty(Object bean, String name, double initial, Runnable... onChange) {
        return new SimpleDoubleProperty(bean, name, initial) {
            @Override
            public void set(final double newValue) {
                if (!isEqual(get(), newValue)) {
                    super.set(newValue);
                    for (Runnable action : onChange) {
                        action.run();
                    }
                }
            }
        };
    }

    public static ReadOnlyDoubleWrapper createReadOnlyDoubleWrapper(Object bean, String name, double initial, Runnable onChange) {
        return new ReadOnlyDoubleWrapper(bean, name, initial) {
            @Override
            public void set(final double newValue) {
                if (!isEqual(get(), newValue)) {
                    super.set(newValue);
                    onChange.run();
                }
            }
        };
    }

    public static ReadOnlyDoubleWrapper createReadOnlyDoubleWrapper(Object bean, String name, double initial) {
        return new ReadOnlyDoubleWrapper(bean, name, initial);
    }

    public static <T> ObjectProperty<T> createObjectProperty(Object bean, String name, T initial) {
        return new SimpleObjectProperty<T>(bean, name, initial) {
            @Override
            public void set(final T newValue) {
                final T oldValue = getValue();
                if (!Objects.equals(oldValue, newValue)) {
                    super.set(newValue);
                }
            }
        };
    }

    /**
     * subscribes to property changes without requiring value boxing
     */
    public static void runOnChange(Runnable action, ObservableValue<?>... conditions){
        for (var condition : conditions) {
            if (condition instanceof ObservableDoubleValue) {
                var obs = (ObservableDoubleValue) condition;
                condition.addListener(new InvalidationListener() {
                    double prev = obs.get();

                    @Override
                    public void invalidated(Observable observable) {
                        if (!isEqual(prev, obs.get())) {
                            prev = obs.get();
                            action.run();
                        }
                    }
                });
            } else if (condition instanceof ObservableBooleanValue) {
                var obs = (ObservableBooleanValue) condition;
                condition.addListener(new InvalidationListener() {
                    boolean prev = obs.get();

                    @Override
                    public void invalidated(Observable observable) {
                        if (prev == obs.get()) {
                            prev = obs.get();
                            action.run();
                        }
                    }
                });
            } else if (condition instanceof ObservableIntegerValue) {
                var obs = (ObservableIntegerValue) condition;
                condition.addListener(new InvalidationListener() {
                    int prev = obs.get();

                    @Override
                    public void invalidated(Observable observable) {
                        if (prev == obs.get()) {
                            prev = obs.get();
                            action.run();
                        }
                    }
                });
            } else {
                condition.addListener((observable, oldValue, newValue) -> action.run());
            }
        }
    }

    private static boolean isEqual(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b); // supports NaN
    }

}
