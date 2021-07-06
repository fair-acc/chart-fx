package de.gsi.dataset;

import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;

import org.jetbrains.annotations.NotNull;

/**
 * Formatter defining the number (can be specialised to Double, Integer etc.) and String.
 *
 * @param <T> specialised converter type for which the default format behaviour is replaced in the formatter
 */
public interface Formatter<T> {
    /**
     *
     * @return class instance of type T for which the default format behaviour is replaced in the formatter
     */
    Class<T> getClassInstance();

    /**
     * Converts the provided value into its string form.
     *
     * @param value the number to convert
     * @return a string representation of the Number passed in.
     */
    @NotNull
    String toString(@NotNull final T value);

    /**
     * Converts the string provided into a value defined by the specific converter.
     *
     * @param string the {@code String} to convert
     * @param pos a ParsePosition object with index and error index information
     * @return a number representation of the string passed in.
     * @throws NumberFormatException  in case of parsing errors
     */
    default T fromString(@NotNull final String string, @NotNull final ParsePosition pos) {
        final int end = string.indexOf(' ', pos.getIndex());
        if (end == -1) {
            return fromString(string.substring(pos.getIndex()));
        }
        return fromString(string.substring(pos.getIndex(), end));
    }

    /**
     * Converts the string provided into an number defined by the specific converter.
     *
     * @param string the {@code String} to convert
     * @return a number representation of the string passed in.
     * @throws NumberFormatException in case of parsing errors
     */
    @NotNull
    T fromString(@NotNull final String string);

    /**
     * @param pattern the pattern for this message format
     * @param arguments an array of objects to be formatted and substituted - Numbers are formatted by @see #toString
     * @return formatted string
     */
    @NotNull
    default String format(@NotNull final String pattern, @NotNull final Object... arguments) {
        final Class<T> test = getClassInstance();

        final Format numberFormat = new Format() {
            @Override
            @SuppressWarnings("unchecked")
            public StringBuffer format(@NotNull final Object obj, @NotNull final StringBuffer toAppendTo, @NotNull final FieldPosition pos) {
                assert test.isAssignableFrom(obj.getClass())
                    : (" object is a " + obj.getClass().getName() + " and not a " + test.getName());
                return toAppendTo.append(Formatter.this.toString((T) obj)); // NOSONAR NOPMD - cannot check this due to type erasure
            }

            @Override
            public Object parseObject(final String source, @NotNull final ParsePosition pos) {
                return Formatter.this.fromString(source, pos); // not used
            }
        };

        final var formatter = new MessageFormat(pattern);
        for (var i = 0; i < arguments.length; i++) {
            if (test.isAssignableFrom(arguments[i].getClass())) {
                formatter.setFormatByArgumentIndex(i, numberFormat);
            }
        }
        return formatter.format(arguments);
    }
}
