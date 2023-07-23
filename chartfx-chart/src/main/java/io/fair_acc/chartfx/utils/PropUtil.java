package io.fair_acc.chartfx.utils;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.beans.value.*;

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
        var prop = new SimpleBooleanProperty(bean, name, initial);
        for (Runnable action : onChange) {
            runOnChange(action, prop);
        }
        return prop;
    }

    public static DoubleProperty createDoubleProperty(Object bean, String name, double initial, Runnable... onChange) {
        var prop = new SimpleDoubleProperty(bean, name, initial);
        for (Runnable action : onChange) {
            runOnChange(action, prop);
        }
        return prop;
    }

    public static ReadOnlyDoubleWrapper createReadOnlyDoubleWrapper(Object bean, String name, double initial, Runnable... onChange) {
        var prop = new ReadOnlyDoubleWrapper(bean, name, initial);
        for (Runnable action : onChange) {
            runOnChange(action, prop);
        }
        return prop;
    }

    public static <T> ObjectProperty<T> createObjectProperty(Object bean, String name, T initial, Runnable... onChange) {
        var prop = new SimpleObjectProperty<>(bean, name, initial);
        for (Runnable action : onChange) {
            runOnChange(action, prop);
        }
        return prop;
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
            } else if (condition instanceof ObservableLongValue) {
                var obs = (ObservableLongValue) condition;
                condition.addListener(new InvalidationListener() {
                    long prev = obs.get();

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

    public static boolean isEqual(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b); // supports NaN
    }

    public static boolean isNullOrEmpty(String string){
        return string == null || string.isBlank();
    }

    private PropUtil() {
    }

}
