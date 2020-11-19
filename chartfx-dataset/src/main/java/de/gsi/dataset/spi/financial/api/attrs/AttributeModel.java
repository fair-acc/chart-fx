package de.gsi.dataset.spi.financial.api.attrs;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AttributeModel implements Cloneable {

    private Map<AttributeKey<?>, Object> attributes;

    public AttributeModel() {
        attributes = new LinkedHashMap<>();
    }

    public AttributeModel(Map<AttributeKey<?>, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * @return factory method for builder template
     */
    public static AttributeModel configure() {
        return new AttributeModel();
    }

    /**
     * @param template configure model by given filled model
     * @return factory method for builder template
     */
    public static AttributeModel configure(AttributeModel template) {
        return template.deepCopyAttributes();
    }

    /**
     * Returns an attribute of the plugin model
     *
     * @param key Key which identifies the attribute
     * @param <T> Type of the value
     * @return Attribute value or <code>null</code> if the attribute is not set
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(AttributeKey<T> key) {
        return (T) attributes.get(key);
    }

    /**
     * Returns an attribute value as required
     *
     * @param key Key which identifies the attribute
     * @param <T> Type of the value
     * @return Attribute value, <code>null</code> - throws IllegalArgumentException - required attributes
     */
    public <T> T getRequiredAttribute(AttributeKey<T> key) {
        T value = getAttribute(key);
        if (value == null) {
            throw new IllegalArgumentException("The attribute " + key + " is required!");
        }
        return value;
    }

    /**
     * Returns an attribute of the model
     *
     * @param key          Key which identifies the attribute
     * @param <T>          Type of the value
     * @param defaultValue value is used, if the value doesn't exist
     * @return Attribute value or <code>default value</code> if the attribute is not set
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(AttributeKey<T> key, T defaultValue) {
        T value = (T) attributes.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Returns an attribute of the model, if it is not available - the default
     * value is taken and automatically is set to the model!
     *
     * @param key          Key which identifies the attribute
     * @param <T>          Type of the value
     * @param defaultValue value is used, if the value doesn't exist, and set to the instance model
     * @return Attribute value or <code>default value</code> if the attribute is not set
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttributeAndSet(AttributeKey<T> key, T defaultValue) {
        T value = (T) attributes.get(key);
        if (value == null) {
            setAttribute(key, defaultValue);
            return defaultValue;
        }
        return value;
    }

    /**
     * Returns an attribute of the plugin model
     *
     * @param key   Key which identifies the attribute
     * @param <T>   Type of the value
     * @param <E>   retype to specific inherited class
     * @param clazz specific class which will be used for casting
     * @return Attribute value or <code>default value</code> if the attribute is not set
     */
    @SuppressWarnings("unchecked")
    public <T, E extends T> E getAttribute(AttributeKey<T> key, Class<E> clazz) {
        T value = (T) attributes.get(key);
        return (E) value;
    }

    /**
     * Returns an attribute of the plugin model
     *
     * @param key          Key which identifies the attribute
     * @param <T>          Type of the value
     * @param <E>          retype to specific inherited class
     * @param clazz        specific class which will be used for casting
     * @param defaultValue default value is used, if the value doesn't exist
     * @return Attribute value or <code>default value</code> if the attribute is not set
     */
    @SuppressWarnings("unchecked")
    public <T, E extends T> E getAttribute(AttributeKey<T> key, Class<E> clazz, E defaultValue) {
        T value = (T) attributes.get(key);
        if (value == null) {
            return defaultValue;
        }
        return (E) value;
    }

    /**
     * Sets an attribute on the plugin model. The key must not be null. If a value is null,
     * the attribute is removed, so containsAttribute method will return false
     * after this operation.
     *
     * @param key   Key which identifies the attribute
     * @param <T>   Type of the value
     * @param value Attribute value
     * @return this instance, can be used for builder syntax
     */
    public <T> AttributeModel setAttribute(AttributeKey<T> key, T value) {
        if (key == null) {
            throw new IllegalArgumentException("The attribute key hasn't be null");
        }
        if (value == null) {
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
        return this;
    }

    /**
     * Returns true if the plugin model contains an attribute specified by given key
     *
     * @param key Key which identifies the attribute
     */
    public boolean containsAttribute(AttributeKey<?> key) {
        return attributes.containsKey(key);
    }

    /**
     * @return provides all attribute keys in the model
     */
    public Set<AttributeKey<?>> getAttributes() {
        return attributes.keySet();
    }

    /**
     * Merge model to this actual model
     *
     * @param model AttributeModel
     */
    public synchronized void merge(AttributeModel model) {
        AttributeModel copiedModel = (AttributeModel) model.clone();
        // clone the included attribute models
        for (AttributeKey attributeKey : copiedModel.getAttributes()) {
            if (AttributeModel.class.isAssignableFrom(attributeKey.getType())) {
                AttributeModel attributeModel = (AttributeModel) copiedModel.getAttribute(attributeKey);
                attributeModel = attributeModel.deepCopyAttributes();
                setAttribute(attributeKey, attributeModel);
            } else {
                setAttribute(attributeKey, copiedModel.getAttribute(attributeKey));
            }
        }
    }

    /**
     * @return copy attributes model - deep copy of objects
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized AttributeModel deepCopyAttributes() {
        //Cloner cloner = new Cloner();
        //HashMap<AttributeKey<?>, Object> _attributes = (HashMap)cloner.deepClone(attributes);
        AttributeModel copiedModel = (AttributeModel) clone();
        // clone the included attribute models
        for (AttributeKey attributeKey : copiedModel.getAttributes()) {
            if (AttributeModel.class.isAssignableFrom(attributeKey.getType())) {
                AttributeModel attributeModel = (AttributeModel) copiedModel.getAttribute(attributeKey);
                attributeModel = attributeModel.deepCopyAttributes();
                copiedModel.setAttribute(attributeKey, attributeModel);
            }
        }
        return copiedModel;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Object clone() {
        try {
            AttributeModel model = (AttributeModel) super.clone();
            model.attributes = (HashMap) ((HashMap) attributes).clone();
            return model;

        } catch (CloneNotSupportedException ignored) {
        }
        return null;
    }

    @Override
    public String toString() {
        return "AttributeModel [attributes=" + attributes + "]";
    }

}
