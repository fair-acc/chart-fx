package de.gsi.chart.ui.css;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javafx.beans.property.Property;
import javafx.css.CssMetaData;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleableStringProperty;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.SizeConverter;
import javafx.css.converter.StringConverter;

/**
 * Extension of the StylablePropertyFactory to allow more property types (e.g. double, object).
 *
 * @author Alexander Krimmd
 * @param <S> The type of Styleable
 */
public class CssPropertyFactory<S extends Styleable> {
    protected final List<CssMetaData<? extends Styleable, ?>> metaData;
    protected final List<CssMetaData<? extends Styleable, ?>> unmodifiableList;
    protected final Map<String, CssMetaData<S, ?>> metaDataSet = new HashMap<>();

    public CssPropertyFactory() {
        this(null);
    }

    public CssPropertyFactory(List<CssMetaData<? extends Styleable, ?>> parentCss) {
        if (parentCss != null) {
            metaData = new ArrayList<>(parentCss);
        } else {
            metaData = new ArrayList<>();
        }
        this.unmodifiableList = Collections.unmodifiableList(metaData);
    }

    public final List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return unmodifiableList;
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
     * @param filter A filter to apply to updated data
     * @param invalidateActions Runnables to be executed on invalidation of the property // TODO document vararg
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableDoubleProperty createDoubleProperty(S styleable, String propertyName,
            String cssProperty, Function<S, StyleableDoubleProperty> function, double initialValue, boolean inherits,
            BinaryOperator<Double> filter, Runnable... invalidateActions) {
        CssMetaData<S, Number> cssMetaData = (CssMetaData<S, Number>) metaDataSet.computeIfAbsent(cssProperty,
                cssProp -> new CssMetaData<>(cssProp, SizeConverter.getInstance(), initialValue, inherits, null) {
                    @Override
                    public boolean isSettable(S styleBean) {
                        StyleableProperty<Number> prop = getStyleableProperty(styleBean);
                        if (prop instanceof Property<?>) {
                            return !((Property<Number>) prop).isBound();
                        }
                        return prop != null;
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(S styleBean) {
                        return function.apply(styleBean);
                    }
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
    public final StyleableIntegerProperty createIntegerProperty(S styleable, String propertyName,
            String cssProperty, Function<S, StyleableIntegerProperty> function, int initialValue, boolean inherits,
            Runnable invalidateAction, UnaryOperator<Integer> filter) {
        CssMetaData<S, Number> cssMetaData = (CssMetaData<S, Number>) metaDataSet.computeIfAbsent(cssProperty,
                cssProp -> new CssMetaData<>(cssProp, SizeConverter.getInstance(), initialValue, inherits, null) {
                    @Override
                    public boolean isSettable(S styleBean) {
                        StyleableProperty<Number> prop = getStyleableProperty(styleBean);
                        if (prop instanceof Property<?>) {
                            return !((Property<Number>) prop).isBound();
                        }
                        return prop != null;
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(S styleBean) {
                        return function.apply(styleBean);
                    }
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
    public final StyleableBooleanProperty createBooleanProperty(S styleable, String propertyName,
            String cssProperty, Function<S, StyleableBooleanProperty> function, boolean initialValue, boolean inherits,
            Runnable invalidateAction, UnaryOperator<Boolean> filter) {
        CssMetaData<S, Boolean> cssMetaData = (CssMetaData<S, Boolean>) metaDataSet.computeIfAbsent(cssProperty,
                cssProp -> new CssMetaData<>(cssProp, BooleanConverter.getInstance(), initialValue, inherits, null) {
                    @Override
                    public boolean isSettable(S styleBean) {
                        StyleableProperty<Boolean> prop = getStyleableProperty(styleBean);
                        if (prop instanceof Property<?>) {
                            return !((Property<Boolean>) prop).isBound();
                        }
                        return prop != null;
                    }

                    @Override
                    public StyleableProperty<Boolean> getStyleableProperty(S styleBean) {
                        return function.apply(styleBean);
                    }
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
    public final <T> StyleableObjectProperty<T> createObjectProperty(S styleable, String propertyName,
            String cssProperty, Function<S, StyleableProperty<T>> function, T initialValue, boolean inherits,
            Runnable invalidateAction, UnaryOperator<T> filter, StyleConverter<?, T> converter) {
        CssMetaData<S, T> cssMetaData = (CssMetaData<S, T>) metaDataSet.computeIfAbsent(cssProperty,
                cssProp -> new CssMetaData<>(cssProp, converter, initialValue, inherits, null) {
                    @Override
                    public boolean isSettable(S styleBean) {
                        StyleableProperty<T> prop = getStyleableProperty(styleBean);
                        if (prop instanceof Property<?>) {
                            return !((Property<Boolean>) prop).isBound();
                        }
                        return prop != null;
                    }

                    @Override
                    public StyleableProperty<T> getStyleableProperty(S styleBean) {
                        return function.apply(styleBean);
                    }
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
    public final StyleableStringProperty createStringProperty(S styleable, String propertyName,
            String cssProperty, Function<S, StyleableStringProperty> function, String initialValue, boolean inherits,
            Runnable invalidateAction, UnaryOperator<String> filter) {
        CssMetaData<S, String> cssMetaData = (CssMetaData<S, String>) metaDataSet.computeIfAbsent(cssProperty,
                cssProp -> new CssMetaData<>(cssProp, StringConverter.getInstance(), initialValue, inherits, null) {
                    @Override
                    public boolean isSettable(S styleBean) {
                        StyleableProperty<String> prop = getStyleableProperty(styleBean);
                        if (prop instanceof Property<?>) {
                            return !((Property<String>) prop).isBound();
                        }
                        return prop != null;
                    }

                    @Override
                    public StyleableProperty<String> getStyleableProperty(S styleBean) {
                        return function.apply(styleBean);
                    }
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
}
