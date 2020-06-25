package de.gsi.chart.ui.css;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.*;

import javafx.beans.property.Property;
import javafx.css.*;
import javafx.scene.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of the StylablePropertyFactory. Adds types like [Stylish]DoubleProperties and provides callbacks for changes
 * and filters for updates (e.g. clamping the value of a property to a given range).
 * Also adds an enum property which sets the currently selected enum as a pseudo class.
 *
 * @author Alexander Krimm
 * @author rstein
 * @param <S> the type of Styleable
 */
public class CssPropertyFactory<S extends Styleable> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CssPropertyFactory.class);
    protected final List<CssMetaData<? extends Styleable, ?>> metaData;
    protected final List<CssMetaData<? extends Styleable, ?>> unmodifiableList;
    protected final Map<String, CssMetaData<S, ?>> metaDataSet = new HashMap<>();
    protected final Map<String, PseudoClass> pseudoClasses = new HashMap<>();
    protected final Map<Styleable, List<String>> propertyNames = new HashMap<>();

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

    public static List<Field> getAllFields(List<Field> fields, Class<?> clazz) {
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

        if (clazz.getSuperclass() != null) {
            getAllFields(fields, clazz.getSuperclass());
        }

        return fields;
    }

    public static Field getField(Class<?> clazz, final String fieldName) {
        final List<Field> fields = getAllFields(new LinkedList<>(), clazz);

        final Optional<Field> field = fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst();
        field.ifPresent(f -> f.setAccessible(true));

        return field.orElse(null);
    }

    /**
     * @return all styleable properties added via the factory and the ones for the parents.
     */
    public final List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return unmodifiableList;
    }

    /**
     * @return all styleable properties added via the factory and the ones for the parents (modifyable).
     */
    public final List<CssMetaData<? extends Styleable, ?>> getCssMetaDataModifyable() {
        return metaData;
    }

    /**
     * Create a StyleableProperty&lt;Double&gt; with initial value and inherit flag.
     *
     * @param styleableBean the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param inherits whether or not the CSS style can be inherited by child nodes
     * @param filter a filter to react on changes and limit them: {@code (oldVal, newVal) -&gt; filteredVal}
     * @param invalidateActions zero, one or two Runnables (vararg) first one will be executed before and second one after invalidation
     * @return a #StyleableDoubleProperty created with initial value and inherit flag
     */
    public final StyleableDoubleProperty createDoubleProperty(S styleableBean, String propertyName, String cssProperty, double initialValue,
            boolean inherits, DoubleBinaryOperator filter, Runnable... invalidateActions) {
        return new StylishDoubleProperty(styleableBean, propertyName, cssProperty, initialValue, inherits, filter, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Double&gt; with initial value and inherit flag.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param invalidateActions zero, one or two Runnables (vararg) first one will be executed before and second one after invalidation
     * @return a #StyleableDoubleProperty created with initial value and inherit flag
     */
    public final StyleableDoubleProperty createDoubleProperty(S styleable, String propertyName, String cssProperty, double initialValue, Runnable... invalidateActions) {
        return createDoubleProperty(styleable, propertyName, cssProperty, initialValue, true, null, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param inherits whether or not the CSS style can be inherited by child nodes
     * @param filter a filter to react on changes and limit them: {@code (oldVal, newVal) -&gt; filteredVal}
     * @param invalidateActions zero, one or two Runnables (vararg) first one will be executed before and second one after invalidation
     * @return a #StyleableIntegerProperty created with initial value and inherit flag
     */
    public final StyleableIntegerProperty createIntegerProperty(S styleable, String propertyName, String cssProperty, int initialValue, boolean inherits, IntBinaryOperator filter, Runnable... invalidateActions) {
        return new StylishIntegerProperty(styleable, propertyName, cssProperty, initialValue, inherits, filter, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param invalidateActions zero, one or two Runnables (vararg) first one will be executed before and second one after invalidation
     * @return a #StyleableIntegerProperty created with initial value and inherit flag
     */
    public final StyleableIntegerProperty createIntegerProperty(S styleable, String propertyName, String cssProperty, int initialValue, Runnable... invalidateActions) {
        return createIntegerProperty(styleable, propertyName, cssProperty, initialValue, true, null, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Long&gt; with initial value and inherit flag.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param inherits whether or not the CSS style can be inherited by child nodes
     * @param filter a filter to react on changes and limit them: {@code (oldVal, newVal) -&gt; filteredVal}
     * @param invalidateActions zero, one or two Runnables (vararg) first one will be executed before and second one after invalidation
     * @return a #StyleableLongProperty created with initial value and inherit flag
     */
    public final StyleableLongProperty createLongProperty(S styleable, String propertyName, String cssProperty, long initialValue, boolean inherits, LongBinaryOperator filter, Runnable... invalidateActions) {
        return new StylishLongProperty(styleable, propertyName, cssProperty, initialValue, inherits, filter, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Long&gt; with initial value and inherit flag.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param invalidateActions zero, one or two {@code Runnable}s (vararg) first one will be executed before and second one after invalidation
     * @return a #StyleableLongProperty created with initial value and inherit flag
     */
    public final StyleableLongProperty createLongProperty(S styleable, String propertyName, String cssProperty, long initialValue, Runnable... invalidateActions) {
        return createLongProperty(styleable, propertyName, cssProperty, initialValue, true, null, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Float&gt; with initial value and inherit flag.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param inherits whether or not the CSS style can be inherited by child nodes
     * @param filter a filter to react on changes and limit them: {@code (oldVal, newVal) -&gt; filteredVal}
     * @param invalidateActions zero, one or two {@code Runnable}s (vararg) first one will be executed before and second one after invalidation
     * @return a #StyleableFloatProperty created with initial value and inherit flag
     */
    public final StyleableFloatProperty createFloatProperty(S styleable, String propertyName, String cssProperty, float initialValue, boolean inherits, BinaryOperator<Float> filter, Runnable... invalidateActions) {
        return new StylishFloatProperty(styleable, propertyName, cssProperty, initialValue, inherits, filter, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Float&gt; with initial value and inherit flag.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param invalidateActions zero, one or two {@code Runnable}s (vararg) first one will be executed before and second one after invalidation
     * @return a #StyleableFloatProperty created with initial value and inherit flag
     */
    public final StyleableFloatProperty createFloatProperty(S styleable, String propertyName, String cssProperty, float initialValue, Runnable... invalidateActions) {
        return createFloatProperty(styleable, propertyName, cssProperty, initialValue, true, null, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param inherits whether or not the CSS style can be inherited by child nodes
     * @param filter a filter to react on changes and limit them: {@code (oldVal, newVal) -&gt; filteredVal}
     * @param invalidateActions zero, one or two {@code Runnable}s (vararg) first one will be executed before and second one after invalidation
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableBooleanProperty createBooleanProperty(S styleable, String propertyName, String cssProperty, boolean initialValue, boolean inherits, BinaryOperator<Boolean> filter, Runnable... invalidateActions) {
        return new StylishBooleanProperty(styleable, propertyName, cssProperty, initialValue, inherits, filter, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param invalidateActions zero, one or two {@code Runnable}s (vararg) first one will be executed before and second one after invalidation
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableBooleanProperty createBooleanProperty(S styleable, String propertyName, String cssProperty, boolean initialValue, Runnable... invalidateActions) {
        return createBooleanProperty(styleable, propertyName, cssProperty, initialValue, true, null, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param inherits whether or not the CSS style can be inherited by child nodes
     * @param filter a filter to react on changes and limit them: {@code (oldVal, newVal) -&gt; filteredVal}
     * @param invalidateActions zero, one or two {@code Runnable}s (vararg) first one will be executed before and second one after invalidation
     * @param converter the style converter to convert the style to the object
     * @return a StyleableProperty created with initial value and inherit flag
     * @param <T> Type of the Property
     */
    public final <T> StyleableObjectProperty<T> createObjectProperty(S styleable, String propertyName, String cssProperty, T initialValue,
            boolean inherits, StyleConverter<?, T> converter, BinaryOperator<T> filter, Runnable... invalidateActions) {
        return new StylishObjectProperty<>(styleable, propertyName, cssProperty, initialValue, inherits, converter, filter, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param invalidateActions zero, one or two {@code Runnable}s (vararg) first one will be executed before and second one after invalidation
     * @param converter the style converter to convert the style to the object
     * @return a StyleableProperty created with initial value and inherit flag
     * @param <T> Type of the Property
     */
    public final <T> StyleableObjectProperty<T> createObjectProperty(S styleable, String propertyName, String cssProperty, T initialValue, StyleConverter<?, T> converter, Runnable... invalidateActions) {
        return createObjectProperty(styleable, propertyName, cssProperty, initialValue, true, converter, null, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param inherits whether or not the CSS style can be inherited by child nodes
     * @param filter a filter to react on changes and limit them: {@code (oldVal, newVal) -&gt; filteredVal}
     * @param invalidateActions zero, one or two {@code Runnable}s (vararg) first one will be executed before and second one after invalidation
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableStringProperty createStringProperty(S styleable, String propertyName, String cssProperty, String initialValue, boolean inherits, BinaryOperator<String> filter, Runnable... invalidateActions) {
        return new StylishStringProperty(styleable, propertyName, cssProperty, initialValue, inherits, filter, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param invalidateActions zero, one or two {@code Runnable}s (vararg) first one will be executed before and second one after invalidation
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableStringProperty createStringProperty(S styleable, String propertyName, String cssProperty, String initialValue, Runnable... invalidateActions) {
        return createStringProperty(styleable, propertyName, cssProperty, initialValue, true, null, invalidateActions);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     * This also creates pseudoclasses for each enum value and keeps them up to date with the property.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param inherits whether or not the CSS style can be inherited by child nodes
     * @param filter a filter to react on changes and limit them: {@code (oldVal, newVal) -&gt; filteredVal}
     * @param invalidateActions zero, one or two {@code Runnable}s (vararg) first one will be executed before and second one after invalidation
     * @param enumClass the type of enum to read
     * @return a StyleableProperty created with initial value and inherit flag
     * @param <T> Type of the Property
     */
    public <T extends Enum<T>> StyleableObjectProperty<T> createEnumPropertyWithPseudoclasses(Styleable styleable, String propertyName, String cssProperty, T initialValue,
            boolean inherits, Class<T> enumClass, BinaryOperator<T> filter, Runnable... invalidateActions) {
        BinaryOperator<T> pseudoClassUpdatingFilter = (oldVal, newVal) -> {
            // update pseudo class
            if (oldVal != null) {
                ((Node) styleable).pseudoClassStateChanged(pseudoClasses.get(oldVal.toString().toLowerCase().replace('_', '-')), false);
            }
            if (newVal != null) {
                ((Node) styleable).pseudoClassStateChanged(pseudoClasses.get(newVal.toString().toLowerCase().replace('_', '-')), true);
            }
            // apply filter
            if (filter != null) {
                return filter.apply(oldVal, newVal);
            }
            return newVal;
        };
        final StylishEnumProperty<T> newProperty = new StylishEnumProperty<>(styleable, propertyName, cssProperty, initialValue, inherits, enumClass, pseudoClassUpdatingFilter, invalidateActions);
        // set initial pseudo class style
        ((Node) styleable).pseudoClassStateChanged(pseudoClasses.get(initialValue.toString().toLowerCase().replace('_', '-')), true);

        return newProperty;
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     * This also creates pseudoclasses for each enum value and keeps them up to date with the property.
     *
     * @param styleable the <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName the field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty the CSS property name
     * @param initialValue the initial value of the property. CSS may reset the property to this value.
     * @param invalidateActions zero, one or two {@code Runnable}s (vararg) first one will be executed before and second one after invalidation
     * @param enumClass the type of enum to read
     * @return a StyleableProperty created with initial value and inherit flag
     * @param <T> Type of the Property
     */
    public <T extends Enum<T>> StyleableObjectProperty<T> createEnumPropertyWithPseudoclasses(Styleable styleable, String propertyName, String cssProperty, T initialValue, Class<T> enumClass, Runnable... invalidateActions) {
        return createEnumPropertyWithPseudoclasses(styleable, propertyName, cssProperty, initialValue, true, enumClass, null, invalidateActions);
    }

    private <T> Function<S, StyleableProperty<T>> getStyleablePropertyFunction(Field field, String propertyName) {
        return s -> {
            try {
                return (StyleableProperty<T>) (field.get(s));
            } catch (IllegalAccessException e) {
                LOGGER.atError().setCause(e).addArgument(field).addArgument(propertyName).log("field {} propertyName {}");
            }
            return null;
        };
    }

    private void checkPropertyConsistency(final Styleable styleableBean, final String propertyName, final Runnable... invalidateActions) {
        if (styleableBean == null) {
            throw new IllegalArgumentException("styleableBean is null");
        }
        final Class<?> clazz = styleableBean.getClass();

        final List<String> propertyList = propertyNames.get(styleableBean);
        if (propertyList != null && propertyList.contains(propertyName)) {
            throw new IllegalArgumentException("class " + clazz.getName() + " contains duplicate CSS property '" + propertyName + "'");
        } else {
            propertyNames.computeIfAbsent(styleableBean, bean -> new ArrayList<String>()).add(propertyName);
        }

        final Field field = getField(clazz, propertyName);
        if (field == null) {
            throw new IllegalArgumentException("class " + clazz.getName() + " does not assign CSS property '" + propertyName + "' to class field with the same name");
        }

        if (invalidateActions.length > 2) {
            throw new IllegalArgumentException("config error in class " + clazz.getName() + " for CSS property '" + propertyName //
                                               + "' : only 2 (post- and pre invalidation) actions allowed, but " + invalidateActions.length + " actions are supplied");
        }
    }

    protected String getCssPropertyName(final String propertyName) {
        return "-fx-" + propertyName.replaceAll("([A-Z])", "-$1").toLowerCase();
    }

    public static class SimpleCssMetaData<S extends Styleable, T> extends CssMetaData<S, T> {
        private final Function<S, StyleableProperty<T>> function;

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

    public class StylishBooleanProperty extends SimpleStyleableBooleanProperty {
        protected final Runnable preInvalidateAction;
        protected final Runnable postInvalidateAction;
        protected final BinaryOperator<Boolean> filter;

        /**
         * The constructor of the {@code StylishBooleanProperty}.
         *
         * @param styleableBean the bean of this {@code BooleanProperty}
         * @param propertyName the name of this {@code BooleanProperty}
         * @param cssProperty  the CSS property name
         * @param initialValue the initial value of the wrapped {@code Object}
         * @param inherits whether or not the CSS style can be inherited by child nodes
         * @param filter possibility to modify the new value based on old and updated value
         * @param invalidateActions lambda expressions executed after and before invalidation
         */
        public StylishBooleanProperty(Styleable styleableBean, String propertyName, String cssProperty, boolean initialValue,
                boolean inherits, BinaryOperator<Boolean> filter, Runnable... invalidateActions) {
            super((CssMetaData<S, Boolean>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
                final Field field = getField(styleableBean.getClass(), propertyName);
                final SimpleCssMetaData<S, Boolean> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getBooleanConverter(), initialValue, inherits, null,
                        getStyleablePropertyFunction(field, propertyName));
                metaData.add(newData);
                return newData;
            }), styleableBean, propertyName, initialValue);

            checkPropertyConsistency(styleableBean, propertyName, invalidateActions);

            this.filter = filter;
            this.postInvalidateAction = invalidateActions.length > 0 ? invalidateActions[0] : null;
            this.preInvalidateAction = invalidateActions.length > 1 ? invalidateActions[1] : null;
        }

        @Override
        public void set(boolean v) {
            if (preInvalidateAction != null) {
                preInvalidateAction.run();
            }
            if (filter == null) {
                super.set(v);
            } else {
                super.set(filter.apply(get(), v));
            }
            if (postInvalidateAction != null) {
                postInvalidateAction.run();
            }
        }
    }

    public class StylishIntegerProperty extends SimpleStyleableIntegerProperty {
        protected final Runnable preInvalidateAction;
        protected final Runnable postInvalidateAction;
        protected final IntBinaryOperator filter;

        /**
         * The constructor of the {@code StylishIntegerProperty}.
         *
         * @param styleableBean the bean of this {@code IntegerProperty}
         * @param propertyName the name of this {@code IntegerProperty}
         * @param cssProperty  the CSS property
         * @param initialValue the initial value of the wrapped {@code Object}
         * @param inherits whether or not the CSS style can be inherited by child nodes
         * @param filter possibility to modify the new value based on old and updated value
         * @param invalidateActions lambda expressions executed after and before invalidation
         */
        public StylishIntegerProperty(Styleable styleableBean, String propertyName, String cssProperty, int initialValue,
                boolean inherits, IntBinaryOperator filter, Runnable... invalidateActions) {
            super((CssMetaData<S, Number>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
                final Field field = getField(styleableBean.getClass(), propertyName);
                final SimpleCssMetaData<S, Number> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getSizeConverter(), initialValue, inherits, null,
                        getStyleablePropertyFunction(field, propertyName));
                metaData.add(newData);
                return newData;
            }), styleableBean, propertyName, initialValue);

            checkPropertyConsistency(styleableBean, propertyName, invalidateActions);

            this.filter = filter;
            this.postInvalidateAction = invalidateActions.length > 0 ? invalidateActions[0] : null;
            this.preInvalidateAction = invalidateActions.length > 1 ? invalidateActions[1] : null;
        }

        @Override
        public void set(int v) {
            if (preInvalidateAction != null) {
                preInvalidateAction.run();
            }
            if (filter == null) {
                super.set(v);
            } else {
                super.set(filter.applyAsInt(get(), v));
            }
            if (postInvalidateAction != null) {
                postInvalidateAction.run();
            }
        }
    }

    public class StylishLongProperty extends SimpleStyleableLongProperty {
        protected final Runnable preInvalidateAction;
        protected final Runnable postInvalidateAction;
        protected final LongBinaryOperator filter;

        /**
         * The constructor of the {@code StylishIntegerProperty}.
         *
         * @param styleableBean the bean of this {@code IntegerProperty}
         * @param propertyName the name of this {@code IntegerProperty}
         * @param cssProperty  the CSS property name
         * @param initialValue the initial value of the wrapped {@code Object}
         * @param inherits whether or not the CSS style can be inherited by child nodes
         * @param filter possibility to modify the new value based on old and updated value
         * @param invalidateActions lambda expressions executed after and before invalidation
         * */
        public StylishLongProperty(Styleable styleableBean, String propertyName, String cssProperty, long initialValue, boolean inherits, LongBinaryOperator filter, Runnable... invalidateActions) {
            super((CssMetaData<S, Number>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
                final Field field = getField(styleableBean.getClass(), propertyName);
                final SimpleCssMetaData<S, Number> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getSizeConverter(), initialValue, inherits, null,
                        getStyleablePropertyFunction(field, propertyName));
                metaData.add(newData);
                return newData;
            }), styleableBean, propertyName, initialValue);

            checkPropertyConsistency(styleableBean, propertyName, invalidateActions);

            this.filter = filter;
            this.postInvalidateAction = invalidateActions.length > 0 ? invalidateActions[0] : null;
            this.preInvalidateAction = invalidateActions.length > 1 ? invalidateActions[1] : null;
        }

        @Override
        public void set(long v) {
            if (preInvalidateAction != null) {
                preInvalidateAction.run();
            }
            if (filter == null) {
                super.set(v);
            } else {
                super.set(filter.applyAsLong(get(), v));
            }
            if (postInvalidateAction != null) {
                postInvalidateAction.run();
            }
        }
    }

    public class StylishFloatProperty extends SimpleStyleableFloatProperty {
        protected final Runnable preInvalidateAction;
        protected final Runnable postInvalidateAction;
        protected final BinaryOperator<Float> filter;

        /**
         * The constructor of the {@code StylishFloatProperty}.
         *
         * @param styleableBean the styleableBean of this {@code FloatProperty}
         * @param propertyName the propertyName of this {@code FloatProperty}
         * @param cssProperty the CSS property name of this {@code FloatProperty}
         * @param initialValue the initial value of the wrapped {@code Object}
         * @param inherits whether or not the CSS style can be inherited by child nodes
         * @param filter possibility to modify the new value based on old and updated value
         * @param invalidateActions lambda expressions executed after and before invalidation
         */
        public StylishFloatProperty(Styleable styleableBean, String propertyName, String cssProperty, float initialValue, boolean inherits, BinaryOperator<Float> filter, Runnable... invalidateActions) {
            super((CssMetaData<S, Number>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
                final Field field = getField(styleableBean.getClass(), propertyName);
                final SimpleCssMetaData<S, Number> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getSizeConverter(), initialValue, inherits, null, getStyleablePropertyFunction(field, propertyName));
                metaData.add(newData);
                return newData;
            }), styleableBean, propertyName, initialValue);

            checkPropertyConsistency(styleableBean, propertyName, invalidateActions);

            this.filter = filter;
            this.postInvalidateAction = invalidateActions.length > 0 ? invalidateActions[0] : null;
            this.preInvalidateAction = invalidateActions.length > 1 ? invalidateActions[1] : null;
        }

        @Override
        public void set(float v) {
            if (preInvalidateAction != null) {
                preInvalidateAction.run();
            }
            if (filter == null) {
                super.set(v);
            } else {
                super.set(filter.apply(get(), v));
            }
            if (postInvalidateAction != null) {
                postInvalidateAction.run();
            }
        }
    }

    public class StylishDoubleProperty extends SimpleStyleableDoubleProperty {
        protected final Runnable preInvalidateAction;
        protected final Runnable postInvalidateAction;
        protected final DoubleBinaryOperator filter;

        /**
         * The constructor of the {@code StylishDoubleProperty}.
         *
         * @param styleableBean the styleableBean of this {@code DoubleProperty}
         * @param propertyName the propertyName of this {@code DoubleProperty}
         * @param cssProperty the CSS property name of this {@code DoubleProperty}
         * @param initialValue the initial value of the wrapped {@code Object}
         * @param inherits whether or not the CSS style can be inherited by child nodes
         * @param filter possibility to modify the new value based on old and updated value
         * @param invalidateActions lambda expressions executed after and before invalidation
         */
        public StylishDoubleProperty(Styleable styleableBean, String propertyName, String cssProperty, double initialValue, boolean inherits, DoubleBinaryOperator filter, Runnable... invalidateActions) {
            super((CssMetaData<S, Number>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
                final Field field = getField(styleableBean.getClass(), propertyName);
                final SimpleCssMetaData<S, Number> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getSizeConverter(), initialValue, inherits, null, getStyleablePropertyFunction(field, propertyName));
                metaData.add(newData);
                return newData;
            }), styleableBean, propertyName, initialValue);

            checkPropertyConsistency(styleableBean, propertyName, invalidateActions);

            this.filter = filter;
            this.postInvalidateAction = invalidateActions.length > 0 ? invalidateActions[0] : null;
            this.preInvalidateAction = invalidateActions.length > 1 ? invalidateActions[1] : null;
        }

        @Override
        public void set(double v) {
            if (preInvalidateAction != null) {
                preInvalidateAction.run();
            }
            if (filter == null) {
                super.set(v);
            } else {
                super.set(filter.applyAsDouble(get(), v));
            }
            if (postInvalidateAction != null) {
                postInvalidateAction.run();
            }
        }
    }

    public class StylishEnumProperty<T extends Enum<T>> extends SimpleStyleableObjectProperty<T> {
        protected final Runnable preInvalidateAction;
        protected final Runnable postInvalidateAction;
        protected final BinaryOperator<T> filter;

        /**
         * The constructor of the {@code StylishEnumProperty}.
         *
         * @param styleableBean the bean of this {@code ObjectProperty}
         * @param propertyName the name of this {@code ObjectProperty}
         * @param cssProperty the CSS property name of this {@code DoubleProperty}
         * @param initialValue the initial value of the wrapped {@code Enum}
         " @param inherits whether or not the CSS style can be inherited by child nodes
         * @param inherits whether or not the CSS style can be inherited by child nodes
         * @param enumClass the type of enum to read
         * @param filter possibility to modify the new value based on old and updated value
         * @param invalidateActions lambda expressions executed after and before invalidation
         */
        public StylishEnumProperty(Styleable styleableBean, String propertyName, String cssProperty, T initialValue, boolean inherits, Class<T> enumClass, BinaryOperator<T> filter, Runnable... invalidateActions) {
            super((CssMetaData<S, T>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
                final Field field = getField(styleableBean.getClass(), propertyName);
                final SimpleCssMetaData<S, T> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getEnumConverter(enumClass), initialValue, inherits, null, getStyleablePropertyFunction(field, propertyName));
                metaData.add(newData);
                // add all existing pseudo classes
                for (final Enum<T> e : enumClass.getEnumConstants()) {
                    final String name = e.toString().toLowerCase().replace('_', '-');
                    pseudoClasses.computeIfAbsent(name, PseudoClass::getPseudoClass);
                }
                return newData;
            }), styleableBean, propertyName, initialValue);

            checkPropertyConsistency(styleableBean, propertyName, invalidateActions);

            this.filter = filter;
            this.postInvalidateAction = invalidateActions.length > 0 ? invalidateActions[0] : null;
            this.preInvalidateAction = invalidateActions.length > 1 ? invalidateActions[1] : null;
        }

        @Override
        public void set(T v) {
            if (preInvalidateAction != null) {
                preInvalidateAction.run();
            }
            if (filter == null) {
                super.set(v);
            } else {
                super.set(filter.apply(get(), v));
            }
            if (postInvalidateAction != null) {
                postInvalidateAction.run();
            }
        }
    }

    public class StylishObjectProperty<T> extends SimpleStyleableObjectProperty<T> {
        protected final Runnable preInvalidateAction;
        protected final Runnable postInvalidateAction;
        protected final BinaryOperator<T> filter;

        /**
         * The constructor of the {@code StylishObjectProperty}.
         *
         * @param styleableBean the bean of this {@code ObjectProperty}
         * @param propertyName the name of this {@code ObjectProperty}
         * @param cssProperty the CSS property name of this {@code DoubleProperty}
         * @param initialValue the initial value of the wrapped {@code Object}
         * @param inherits whether or not the CSS style can be inherited by child nodes
         * @param converter the style converter to convert the style to the object
         * @param filter possibility to modify the new value based on old and updated value
         * @param invalidateActions lambda expressions executed after and before invalidation
         */
        public StylishObjectProperty(Styleable styleableBean, String propertyName, String cssProperty, T initialValue, boolean inherits, StyleConverter<?, T> converter, BinaryOperator<T> filter, Runnable... invalidateActions) {
            super((CssMetaData<S, T>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
                final Field field = getField(styleableBean.getClass(), propertyName);
                final SimpleCssMetaData<S, T> newData = new SimpleCssMetaData<>(cssProp, converter, initialValue, inherits, null, getStyleablePropertyFunction(field, propertyName));
                metaData.add(newData);
                return newData;
            }), styleableBean, propertyName, initialValue);

            checkPropertyConsistency(styleableBean, propertyName, invalidateActions);

            this.filter = filter;
            this.postInvalidateAction = invalidateActions.length > 0 ? invalidateActions[0] : null;
            this.preInvalidateAction = invalidateActions.length > 1 ? invalidateActions[1] : null;
        }

        @Override
        public void set(T v) {
            if (preInvalidateAction != null) {
                preInvalidateAction.run();
            }
            if (filter == null) {
                super.set(v);
            } else {
                super.set(filter.apply(get(), v));
            }
            if (postInvalidateAction != null) {
                postInvalidateAction.run();
            }
        }
    }

    public class StylishStringProperty extends SimpleStyleableStringProperty {
        protected final Runnable preInvalidateAction;
        protected final Runnable postInvalidateAction;
        protected final BinaryOperator<String> filter;

        /**
         * The constructor of the {@code StylishStringProperty}.
         *
         * @param styleableBean the bean of this {@code ObjectProperty}
         * @param propertyName the name of this {@code ObjectProperty}
         * @param cssProperty the CSS property name of this {@code DoubleProperty}
         * @param initialValue the initial value of the wrapped {@code Object}
         * @param inherits whether or not the CSS style can be inherited by child nodes
         * @param filter possibility to modify the new value based on old and updated value
         * @param invalidateActions lambda expressions executed after and before invalidation
         */
        public StylishStringProperty(Styleable styleableBean, String propertyName, String cssProperty, String initialValue, boolean inherits, BinaryOperator<String> filter, Runnable... invalidateActions) {
            super((CssMetaData<S, String>) metaDataSet.computeIfAbsent(cssProperty, cssProp -> {
                final Field field = getField(styleableBean.getClass(), propertyName);
                final SimpleCssMetaData<S, String> newData = new SimpleCssMetaData<>(cssProp, StyleConverter.getStringConverter(), initialValue, inherits, null, getStyleablePropertyFunction(field, propertyName));
                metaData.add(newData);
                return newData;
            }), styleableBean, propertyName, initialValue);

            checkPropertyConsistency(styleableBean, propertyName, invalidateActions);

            this.filter = filter;
            this.postInvalidateAction = invalidateActions.length > 0 ? invalidateActions[0] : null;
            this.preInvalidateAction = invalidateActions.length > 1 ? invalidateActions[1] : null;
        }

        @Override
        public void set(String v) {
            if (preInvalidateAction != null) {
                preInvalidateAction.run();
            }
            if (filter == null) {
                super.set(v);
            } else {
                super.set(filter.apply(get(), v));
            }
            if (postInvalidateAction != null) {
                postInvalidateAction.run();
            }
        }
    }
}
