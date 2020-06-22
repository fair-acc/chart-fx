package de.gsi.chart.ui.css;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableFloatProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableLongProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleableStringProperty;
import javafx.scene.Node;

import de.gsi.chart.axes.spi.AbstractAxisParameter;
import de.gsi.chart.ui.geometry.Side;

/**
 * Extension of the StylablePropertyFactory. Adds types like DoubleProperties and provides callbacks for changes
 * and filters for updates (e.g. clamping the value of a property to a given range).
 * Also adds an enum property which sets the currently selected enum as a pseudo class.
 *
 * @author Alexander Krimm
 * @param <S> The type of Styleable
 */
public class CssPropertyFactory<S extends Styleable> {
    protected final List<CssMetaData<? extends Styleable, ?>> metaData;
    protected final List<CssMetaData<? extends Styleable, ?>> unmodifiableList;
    protected final Map<String, CssMetaData<S, ?>> metaDataSet = new HashMap<>();
    protected final Map<String, PseudoClass> pseudoClasses = new HashMap<>();

    /**
     * Create a property factory without any properties from the parent class.
     * Only use this, if the parent class does not provide any styleable properties.
     */
    public CssPropertyFactory() {
        this(null);
    }

    /**
     * Create a property factory which also provides the styleable properties of the parent class.
     * {@code private static final CssPropertyFactory<MyStyleable> CSS = new CssPropertyFactory<>(Parent.getClassCssMetaData()}
     * 
     * @param parentCss List containing all styleable properties of the parent class
     */
    public CssPropertyFactory(List<CssMetaData<? extends Styleable, ?>> parentCss) {
        if (parentCss != null) {
            metaData = new ArrayList<>(parentCss);
        } else {
            metaData = new ArrayList<>();
        }
        this.unmodifiableList = Collections.unmodifiableList(metaData);
    }

    /**
     * @return All styleable properties added via the factory and the ones for the parents.
     */
    public final List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return unmodifiableList;
    }

    /**
     * Create a StyleableProperty&lt;Double&gt; with initial value and inherit flag.
     * 
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that was created by this method
     *            call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @param filter A filter to apply to updated data (oldValue, newValue) -> filteredNewValue
     * @param invalidateActions Runnables to be executed on invalidation of the property // TODO document vararg
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableDoubleProperty createDoubleProperty(S styleable, String propertyName, String cssProperty,
            Function<S, StyleableProperty<Number>> function, double initialValue, boolean inherits, BinaryOperator<Double> filter,
            Runnable... invalidateActions) {
        final CssMetaData<S, Number> cssMetaData = (CssMetaData<S, Number>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
            final SimpleCssMetaData<S, Number> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getSizeConverter(), initialValue, inherits, null, function);
            metaData.add(newData);
            return newData;
        });
        return new StylishDoubleProperty(cssMetaData, styleable, propertyName, initialValue, filter, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     * 
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that was created by this method
     *            call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @param invalidateAction Runnable to be executed on invalidation of the property
     * @param filter A filter to apply to updated data
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableIntegerProperty createIntegerProperty(S styleable, String propertyName, String cssProperty,
            Function<S, StyleableProperty<Number>> function, int initialValue, boolean inherits, Runnable invalidateAction, UnaryOperator<Integer> filter) {
        final CssMetaData<S, Number> cssMetaData = (CssMetaData<S, Number>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
            final SimpleCssMetaData<S, Number> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getSizeConverter(), initialValue, inherits, null, function);
            metaData.add(newData);
            return newData;
        });
        if (filter != null) {
            return new StylishIntegerProperty(cssMetaData, styleable, propertyName, initialValue, invalidateAction) {
                @Override
                public void set(final int value) {
                    super.set(filter.apply(value));
                }
            };
        }
        return new StylishIntegerProperty(cssMetaData, styleable, propertyName, initialValue, invalidateAction);
    }

    /**
     * Create a StyleableProperty&lt;Long&gt; with initial value and inherit flag.
     * 
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that was created by this method
     *            call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @param invalidateAction Runnable to be executed on invalidation of the property
     * @param filter A filter to apply to updated data
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableLongProperty createLongProperty(S styleable, String propertyName, String cssProperty,
            Function<S, StyleableProperty<Number>> function, long initialValue, boolean inherits, Runnable invalidateAction, UnaryOperator<Long> filter) {
        final CssMetaData<S, Number> cssMetaData = (CssMetaData<S, Number>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
            final SimpleCssMetaData<S, Number> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getSizeConverter(), initialValue, inherits, null, function);
            metaData.add(newData);
            return newData;
        });
        if (filter != null) {
            return new StylishLongProperty(cssMetaData, styleable, propertyName, initialValue, invalidateAction) {
                @Override
                public void set(final long value) {
                    super.set(filter.apply(value));
                }
            };
        }
        return new StylishLongProperty(cssMetaData, styleable, propertyName, initialValue, invalidateAction);
    }

    /**
     * Create a StyleableProperty&lt;Float&gt; with initial value and inherit flag.
     * 
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that was created by this method
     *            call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @param invalidateAction Runnable to be executed on invalidation of the property
     * @param filter A filter to apply to updated data
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableFloatProperty createFloatProperty(S styleable, String propertyName, String cssProperty,
            Function<S, StyleableProperty<Number>> function, float initialValue, boolean inherits, Runnable invalidateAction, UnaryOperator<Float> filter) {
        final CssMetaData<S, Number> cssMetaData = (CssMetaData<S, Number>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
            final SimpleCssMetaData<S, Number> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getSizeConverter(), initialValue, inherits, null, function);
            metaData.add(newData);
            return newData;
        });
        if (filter != null) {
            return new StylishFloatProperty(cssMetaData, styleable, propertyName, initialValue, invalidateAction) {
                @Override
                public void set(final float value) {
                    super.set(filter.apply(value));
                }
            };
        }
        return new StylishFloatProperty(cssMetaData, styleable, propertyName, initialValue, invalidateAction);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     * 
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that was created by this method
     *            call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @param invalidateAction Runnable to be executed on invalidation of the property
     * @param filter A filter to apply to updated data
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableBooleanProperty createBooleanProperty(S styleable, String propertyName, String cssProperty,
            Function<S, StyleableProperty<Boolean>> function, boolean initialValue, boolean inherits, Runnable invalidateAction,
            UnaryOperator<Boolean> filter) {
        final CssMetaData<S, Boolean> cssMetaData = (CssMetaData<S, Boolean>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
            final SimpleCssMetaData<S, Boolean> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getBooleanConverter(), initialValue, inherits, null,
                    function);
            metaData.add(newData);
            return newData;
        });
        if (filter != null) {
            return new StylishBooleanProperty(cssMetaData, styleable, propertyName, initialValue, invalidateAction) {
                @Override
                public void set(final boolean value) {
                    super.set(filter.apply(value));
                }
            };
        }
        return new StylishBooleanProperty(cssMetaData, styleable, propertyName, initialValue, invalidateAction);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     * 
     * @param <T> Type of the Property
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @param invalidateAction Runnable to be executed on invalidation of the property
     * @param filter A filter to apply to updated data
     * @param converter The style converter to convert the style to the object
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final <T> StyleableObjectProperty<T> createObjectProperty(S styleable, String propertyName, String cssProperty,
            Function<S, StyleableProperty<T>> function, T initialValue, boolean inherits, Runnable invalidateAction, UnaryOperator<T> filter,
            StyleConverter<?, T> converter) {
        final CssMetaData<S, T> cssMetaData = (CssMetaData<S, T>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
            final SimpleCssMetaData<S, T> newData = new SimpleCssMetaData<>(cssProp, converter, initialValue, inherits, null, function);
            metaData.add(newData);
            return newData;
        });
        if (filter != null) {
            return new StylishObjectProperty<>(cssMetaData, styleable, propertyName, initialValue, invalidateAction) {
                @Override
                public void set(final T value) {
                    super.set(filter.apply(value));
                }
            };
        }
        return new StylishObjectProperty<>(cssMetaData, styleable, propertyName, initialValue, invalidateAction);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     * 
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that was created by this method
     *            call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @param invalidateAction Runnable to be executed on invalidation of the property
     * @param filter A filter to apply to updated data
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableStringProperty createStringProperty(S styleable, String propertyName, String cssProperty,
            Function<S, StyleableProperty<String>> function, String initialValue, boolean inherits, Runnable invalidateAction, UnaryOperator<String> filter) {
        final CssMetaData<S, String> cssMetaData = (CssMetaData<S, String>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
            final SimpleCssMetaData<S, String> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getStringConverter(), initialValue, inherits, null,
                    function);
            metaData.add(newData);
            return newData;
        });
        if (filter != null) {
            return new StylishStringProperty(cssMetaData, styleable, propertyName, initialValue, invalidateAction) {
                @Override
                public void set(final String value) {
                    super.set(filter.apply(value));
                }
            };
        }
        return new StylishStringProperty(cssMetaData, styleable, propertyName, initialValue, invalidateAction);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     * This also creates pseudoclasses for each enum value and keeps them up to date with the property.
     * 
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that was created by this method
     *            call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @param invalidateAction Runnable to be executed on invalidation of the property
     * @param filter A filter to apply to updated data
     * @param enumClass The type of enum to read
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public <T extends Enum<T>> ObjectProperty<T> createEnumPropertyWithPseudoclasses(Styleable styleable, String propertyName, String cssProperty,
            Function<S, StyleableProperty<T>> function, T initialValue, boolean inherits, Runnable invalidateAction, UnaryOperator<T> filter,
            Class<T> enumClass) {
        final CssMetaData<S, T> cssMetaData = (CssMetaData<S, T>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
            final SimpleCssMetaData<S, T> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getEnumConverter(enumClass), initialValue, inherits, null,
                    function);
            metaData.add(newData);
            // add all existing pseudo classes
            for (final Enum<?> e : enumClass.getEnumConstants()) {
                final String name = e.toString().toLowerCase().replace('_', '-');
                pseudoClasses.computeIfAbsent(name, PseudoClass::getPseudoClass);
            }
            // Add pseudo classes for boolean functions in Enum? e.g. isHorizontal() -> "horizontal" pseudo class?
            return newData;
        });
        ((Node) styleable).pseudoClassStateChanged(pseudoClasses.get(initialValue.toString().toLowerCase().replace('_', '-')), true);
        return new StylishObjectProperty<>(cssMetaData, styleable, propertyName, initialValue, invalidateAction) {
            @Override
            public void set(final T value) {
                // update pseudo class
                ((Node) styleable).pseudoClassStateChanged(pseudoClasses.get(get().toString().toLowerCase().replace('_', '-')), false);
                ((Node) styleable).pseudoClassStateChanged(pseudoClasses.get(value.toString().toLowerCase().replace('_', '-')), true);
                // apply filter
                if (filter != null) {
                    super.set(filter.apply(value));
                } else {
                    super.set(value);
                }
            }
        };
    }

    public static class SimpleCssMetaData<S extends Styleable, T> extends CssMetaData<S, T> {
        private Function<S, StyleableProperty<T>> function;

        /**
         * @param property css property name
         * @param converter style converter to interpret styles
         * @param initialValue the value assigned to the property at initialization
         * @param inherits whether the class inherits this style from its parents
         * @param subProperties list of sub properties
         * @param function Function which returns the property given a styleable
         */
        protected SimpleCssMetaData(String property, StyleConverter<?, T> converter, T initialValue, boolean inherits,
                List<CssMetaData<? extends Styleable, ?>> subProperties, Function<S, StyleableProperty<T>> function) {
            super(property, converter, initialValue, inherits, subProperties);
            this.function = function;
        }

        @Override
        public boolean isSettable(S styleBean) {
            StyleableProperty<T> prop = getStyleableProperty(styleBean);
            if (prop instanceof Property<?>) {
                return !((Property<T>) prop).isBound();
            }
            return prop != null;
        }

        @Override
        public StyleableProperty<T> getStyleableProperty(S styleBean) {
            return function.apply(styleBean);
        }
    }
}
