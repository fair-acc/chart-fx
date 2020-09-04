package de.gsi.serializer.utils;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.serializer.spi.ClassFieldDescription;

public final class ClassUtils { //NOPMD nomen est omen
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassUtils.class);
    // some helper declarations
    private static final Map<Class<?>, Class<?>> primitiveWrapperMap = new HashMap<>();
    private static final Map<Class<?>, Class<?>> wrapperPrimitiveMap = new HashMap<>();
    private static final Map<Class<?>, Class<?>> primitiveArrayBoxedMap = new HashMap<>();
    private static final Map<Class<?>, Class<?>> boxedArrayPrimitiveMap = new HashMap<>();
    private static final Map<Integer, ClassFieldDescription> CLASS_FIELD_DESCRIPTION_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> CLASS_STRING_MAP = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Method>> CLASS_METHOD_MAP = new ConcurrentHashMap<>();
    private static int indentationNumberOfSpace = 4;
    private static int maxRecursionDepth = 10;

    static {
        // primitive types
        add(wrapperPrimitiveMap, primitiveWrapperMap, Boolean.class, boolean.class);
        add(wrapperPrimitiveMap, primitiveWrapperMap, Byte.class, byte.class);
        add(wrapperPrimitiveMap, primitiveWrapperMap, Character.class, char.class);
        add(wrapperPrimitiveMap, primitiveWrapperMap, Short.class, short.class);
        add(wrapperPrimitiveMap, primitiveWrapperMap, Integer.class, int.class);
        add(wrapperPrimitiveMap, primitiveWrapperMap, Long.class, long.class);
        add(wrapperPrimitiveMap, primitiveWrapperMap, Float.class, float.class);
        add(wrapperPrimitiveMap, primitiveWrapperMap, Double.class, double.class);
        add(wrapperPrimitiveMap, primitiveWrapperMap, Void.class, void.class);
        add(wrapperPrimitiveMap, primitiveWrapperMap, String.class, String.class);

        // primitive arrays
        add(primitiveArrayBoxedMap, boxedArrayPrimitiveMap, boolean[].class, Boolean[].class);
        add(primitiveArrayBoxedMap, boxedArrayPrimitiveMap, byte[].class, Byte[].class);
        add(primitiveArrayBoxedMap, boxedArrayPrimitiveMap, char[].class, Character[].class);
        add(primitiveArrayBoxedMap, boxedArrayPrimitiveMap, short[].class, Short[].class);
        add(primitiveArrayBoxedMap, boxedArrayPrimitiveMap, int[].class, Integer[].class);
        add(primitiveArrayBoxedMap, boxedArrayPrimitiveMap, long[].class, Long[].class);
        add(primitiveArrayBoxedMap, boxedArrayPrimitiveMap, float[].class, Float[].class);
        add(primitiveArrayBoxedMap, boxedArrayPrimitiveMap, double[].class, Double[].class);
        add(primitiveArrayBoxedMap, boxedArrayPrimitiveMap, String[].class, String[].class);

        // boxed arrays
    }
    private ClassUtils() {
        // utility class
    }

    public static void checkArgument(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
    }

    public static Class<?> getClassByName(final String name) {
        return CLASS_STRING_MAP.computeIfAbsent(name, key -> {
            try {
                return Class.forName(key);
            } catch (ClassNotFoundException | SecurityException e) {
                LOGGER.atError().setCause(e).addArgument(name).log("exception while getting class {}");
                return null;
            }
        });
    }

    public static Class<?> getClassByNameNonVerboseError(final String name) {
        return CLASS_STRING_MAP.computeIfAbsent(name, key -> {
            try {
                return Class.forName(key);
            } catch (ClassNotFoundException | SecurityException e) {
                return Object.class;
            }
        });
    }

    public static Map<Integer, ClassFieldDescription> getClassDescriptions() {
        return CLASS_FIELD_DESCRIPTION_MAP;
    }

    public static ClassFieldDescription getFieldDescription(final Class<?> clazz, final Class<?>... classArguments) {
        if (clazz == null) {
            throw new IllegalArgumentException("object must not be null");
        }
        return CLASS_FIELD_DESCRIPTION_MAP.computeIfAbsent(computeHashCode(clazz, classArguments),
                key -> new ClassFieldDescription(clazz, false));
    }

    public static int getIndentationNumberOfSpace() {
        return indentationNumberOfSpace;
    }

    public static Collection<ClassFieldDescription> getKnownClasses() {
        return CLASS_FIELD_DESCRIPTION_MAP.values();
    }

    public static Map<Class<?>, Map<String, Method>> getKnownMethods() {
        return CLASS_METHOD_MAP;
    }

    public static int getMaxRecursionDepth() {
        return maxRecursionDepth;
    }

    public static Method getMethod(final Class<?> clazz, final String methodName) {
        return CLASS_METHOD_MAP.computeIfAbsent(clazz, c -> new ConcurrentHashMap<>()).computeIfAbsent(methodName, name -> {
            try {
                return clazz.getMethod(methodName);
            } catch (NoSuchMethodException | SecurityException e) {
                return null;
            }
        });
    }

    public static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            // type is a normal class.
            return (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            // unsure why getRawType() returns Type instead of Class.
            // possibly related to pathological case involving nested classes....
            Type rawType = parameterizedType.getRawType();
            checkArgument(rawType instanceof Class);
            return (Class<?>) rawType;

        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();

        } else if (type instanceof TypeVariable) {
            // we could use the variable's bounds, but that won't work if there are multiple.
            // having a raw type that's more general than necessary is okay
            return Object.class;

        } else if (type instanceof WildcardType) {
            return getRawType(((WildcardType) type).getUpperBounds()[0]);

        } else {
            String className = type == null ? "null" : type.getClass().getName();
            throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
                                               + "GenericArrayType, but <" + type + "> is of type " + className);
        }
    }

    public static Type[] getSecondaryType(final Type type) {
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments();
        }
        return new Type[0];
    }

    public static boolean isBoxedArray(final Class<?> type) {
        return boxedArrayPrimitiveMap.containsKey(type);
    }

    public static boolean isPrimitiveArray(final Class<?> type) {
        return primitiveArrayBoxedMap.containsKey(type);
    }

    public static boolean isPrimitiveOrString(final Class<?> type) {
        if (type == null) {
            return false;
        }
        return type.isPrimitive() || String.class.isAssignableFrom(type);
    }

    public static boolean isPrimitiveOrWrapper(final Class<?> type) {
        if (type == null) {
            return false;
        }
        return type.isPrimitive() || isPrimitiveWrapper(type);
    }

    public static boolean isPrimitiveWrapper(final Class<?> type) {
        return wrapperPrimitiveMap.containsKey(type);
    }

    public static boolean isPrimitiveWrapperOrString(final Class<?> type) {
        if (type == null) {
            return false;
        }
        return wrapperPrimitiveMap.containsKey(type) || String.class.isAssignableFrom(type);
    }

    public static Class<?> primitiveToWrapper(final Class<?> cls) {
        Class<?> convertedClass = cls;
        if (cls != null && cls.isPrimitive()) {
            convertedClass = primitiveWrapperMap.get(cls);
        }
        return convertedClass;
    }

    public static void setIndentationNumberOfSpace(final int indentationNumberOfSpace) {
        ClassUtils.indentationNumberOfSpace = indentationNumberOfSpace;
    }

    public static void setMaxRecursionDepth(final int maxRecursionDepth) {
        ClassUtils.maxRecursionDepth = maxRecursionDepth;
    }

    public static String spaces(final int spaces) {
        return CharBuffer.allocate(spaces).toString().replace('\0', ' ');
    }

    public static String translateClassName(final String name) {
        if (name.startsWith("[Z")) {
            return boolean[].class.getName();
        } else if (name.startsWith("[B")) {
            return byte[].class.getName();
        } else if (name.startsWith("[S")) {
            return short[].class.getName();
        } else if (name.startsWith("[I")) {
            return int[].class.getName();
        } else if (name.startsWith("[J")) {
            return long[].class.getName();
        } else if (name.startsWith("[F")) {
            return float[].class.getName();
        } else if (name.startsWith("[D")) {
            return double[].class.getName();
        } else if (name.startsWith("[L")) {
            return name.substring(2, name.length() - 1) + "[]";
        }

        return name;
    }

    private static void add(Map<Class<?>, Class<?>> map1, Map<Class<?>, Class<?>> map2, Class<?> obj1, Class<?> obj2) {
        map1.put(obj1, obj2);
        map2.put(obj2, obj1);
    }

    private static int computeHashCode(final Class<?> classPrototype, final Class<?>... classArguments) {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((classPrototype == null) ? 0 : classPrototype.getName().hashCode());
        if ((classArguments == null) || (classArguments.length <= 0)) {
            return result;
        }

        for (final Class<?> clazz : classArguments) {
            result = (prime * result) + clazz.hashCode();
        }

        return result;
    }
}
