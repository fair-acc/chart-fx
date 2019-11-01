package de.gsi.dataset.serializer.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rstein
 */
public final class ClassDescriptions { // NOPMD - nomen est omen
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassDescriptions.class);
    public static int indentationNumerOfSpace = 4;
    private static final Map<Integer, ClassFieldDescription> CLASS_FIELD_DESCRIPTION_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> CLASS_STRING_MAP = new ConcurrentHashMap<>();

    private ClassDescriptions() {
        // empty constructor
    }

    @SafeVarargs
    private static int computeHashCode(final Class<?> classPrototype, final Class<? extends Object>... classArguments) {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((classPrototype == null) ? 0 : classPrototype.getName().hashCode());
        if ((classArguments == null) || (classArguments.length <= 0)) {
            return result;
        }

        for (final Class<? extends Object> clazz : Arrays.asList(classArguments)) {
            result = (prime * result) + clazz.hashCode();
        }

        return result;
    }

    @SafeVarargs
    public static ClassFieldDescription get(final Class<? extends Object> clazz,
            final Class<? extends Object>... classArguments) {
        if (clazz == null) {
            throw new IllegalArgumentException("object must not be null");
        }
        return CLASS_FIELD_DESCRIPTION_MAP.computeIfAbsent(computeHashCode(clazz, classArguments),
                key -> new ClassFieldDescription(clazz, false));
    }

    public static Class<?> getClassByName(final String name) {
        return CLASS_STRING_MAP.computeIfAbsent(name, key -> {
            try {
                return Class.forName(key);
            } catch (ClassNotFoundException | SecurityException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.atError().setCause(e).addArgument(name).log("exception while getting class {}");
                }
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

    public static Collection<ClassFieldDescription> getKnownClasses() {
        return CLASS_FIELD_DESCRIPTION_MAP.values();
    }

    public static void initNullMemberFields(final Object rootObject) {
        initNullMemberFields(rootObject, ClassDescriptions.get(rootObject.getClass()));
    }

    public static void initNullMemberFields(final Object rootObject, final ClassFieldDescription fieldRoot) {
        if (rootObject == null) {
            throw new IllegalArgumentException("rootObject must not be null");
        }
        Object localRoot = rootObject;
        for (final ClassFieldDescription field : fieldRoot.getChildren()) {
            if (!field.getType().isMemberClass()) {
                continue;
            }

            try {
                localRoot = field.getField().get(rootObject);
                if (localRoot == null) {
                    // need to allocate new object for field

                    final Constructor<?> constr = field.getType().getDeclaredConstructor(rootObject.getClass());
                    constr.setAccessible(true);

                    localRoot = constr.newInstance(rootObject);
                    field.getField().set(rootObject, localRoot);
                }
            } catch (IllegalArgumentException | IllegalAccessException | InstantiationException
                    | InvocationTargetException | SecurityException | NoSuchMethodException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.atError().setCause(e).log("could not initialise inner class:" + field.toString());
                }
            }

            if (localRoot == null) {
                throw new IllegalArgumentException("could not allocate localRoot object");
            }

            initNullMemberFields(localRoot, field);

        }
    }

    public static void printClassStructure(final ClassFieldDescription field) {
        printClassStructure(field, false, 0);
    }

    public static void printClassStructure(final ClassFieldDescription field, final boolean fullView,
            final int recursionLevel) {
        final String typeCategorgy = (field.isInterface() ? "interface " : (field.isPrimitive() ? "" : "class "));
        final String typeName = field.getTypeName() + field.getGenericFieldTypeString();
        final String mspace = spaces((recursionLevel + 1) * indentationNumerOfSpace);
        final boolean isSerialisable = field.isSerializable();

        if (isSerialisable || fullView) {
            LOGGER.atInfo().addArgument(mspace).addArgument(isSerialisable ? "  " : "//")
                    .addArgument(field.getModifierString()).addArgument(typeCategorgy).addArgument(typeName)
                    .addArgument(field.getFieldNameRelative()).log("{} {} {} {}{} {}");

            field.getChildren().stream().forEach(f -> printClassStructure(f, fullView, recursionLevel + 1));
        }
    }

    public static void printFullClassStructure(final ClassFieldDescription field) {
        printClassStructure(field, true, 0);
    }

    private static String spaces(final int spaces) {
        return CharBuffer.allocate(spaces).toString().replace('\0', ' ');
    }

    public static String translateClassName(final String name) {
        final String retName = name;
        if (retName.startsWith("[Z")) {
            return boolean[].class.getName();
        } else if (retName.startsWith("[B")) {
            return byte[].class.getName();
        } else if (retName.startsWith("[S")) {
            return short[].class.getName();
        } else if (retName.startsWith("[I")) {
            return int[].class.getName();
        } else if (retName.startsWith("[J")) {
            return long[].class.getName();
        } else if (retName.startsWith("[F")) {
            return float[].class.getName();
        } else if (retName.startsWith("[D")) {
            return double[].class.getName();
        } else if (retName.startsWith("[L")) {
            return retName.substring(2, retName.length() - 1) + "[]";
        }
//		else if (retName.startsWith("\\? extends ")) {
//			return retName.substring(WILDCARD_EXTENDS_LENGTH, retName.length() - 1);
//		}

        return retName;
    }
}
