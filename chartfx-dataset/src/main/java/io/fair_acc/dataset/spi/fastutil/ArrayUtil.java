package io.fair_acc.dataset.spi.fastutil;

/**
 * @author Florian Enner
 * @since 26 Mär 2023
 */
class ArrayUtil {

    /** Ensures that a range given by its first (inclusive) and last (exclusive) elements fits an array of given length.
     *
     * <p>This method may be used whenever an array range check is needed.
     *
     * @param arrayLength an array length.
     * @param from a start index (inclusive).
     * @param to an end index (inclusive).
     * @throws IllegalArgumentException if {@code from} is greater than {@code to}.
     * @throws ArrayIndexOutOfBoundsException if {@code from} or {@code to} are greater than {@code arrayLength} or negative.
     */
    public static void ensureFromTo(final int arrayLength, final int from, final int to) {
        if (from < 0) throw new ArrayIndexOutOfBoundsException("Start index (" + from + ") is negative");
        if (from > to) throw new IllegalArgumentException("Start index (" + from + ") is greater than end index (" + to + ")");
        if (to > arrayLength) throw new ArrayIndexOutOfBoundsException("End index (" + to + ") is greater than array length (" + arrayLength + ")");
    }

    public static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

}
