package de.gsi.dataset.serializer;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * Interface definition in line with the jdk Buffer abstract class.
 * This definition is needed to allow to redirect and allow for different buffer implementations.
 * <p>
 * A buffer is a linear, finite sequence of elements of a specific
 * primitive type. Aside from its content, the essential properties of a
 * buffer are its capacity, limit, and position:
 * </p>
 * <blockquote>
 * <p>
 * A buffer's <i>capacity</i> is the number of elements it contains. The
 * capacity of a buffer is never negative and never changes.
 * </p>
 * <p>
 * A buffer's <i>limit</i> is the index of the first element that should
 * not be read or written. A buffer's limit is never negative and is never
 * greater than its capacity.
 * </p>
 * <p>
 * A buffer's <i>position</i> is the index of the next element to be
 * read or written. A buffer's position is never negative and is never
 * greater than its limit.
 * </p>
 * </blockquote>
 * <p>
 * The following invariant holds for the mark, position, limit, and
 * capacity values:
 * <blockquote>
 * {@code 0} {@code <=}
 * <i>position</i> {@code <=}
 * <i>limit</i> {@code <=}
 * <i>capacity</i>
 * </blockquote>
 *
 * @author rstein
 * @param <C> generic type for inheriting self (fluent-design)
 */
@SuppressWarnings("PMD.TooManyMethods") // NOPMD - these are short-hand convenience methods
public interface IoBufferHeader<C extends IoBufferHeader<C>> {

    /**
     * @return the capacity of this buffer
     */
    int capacity();

    /**
     * Clears this buffer. The position is set to zero amd the limit is set to
     * the capacity.
     * <p>
     * Invoke this method before using a sequence of channel-read or
     * <i>put</i> operations to fill this buffer. For example:
     * <blockquote>
     *
     * <pre>
     * buf.clear(); // Prepare buffer for reading
     * in.read(buf); // Read data
     * </pre>
     *
     * </blockquote>
     * <p>
     * This method does not actually erase the data in the buffer, but it
     * is named as if it did because it will most often be used in situations
     * in which that might as well be the case.
     * </p>
     *
     * @return itself (fluent design)
     */
    C clear();

    C ensureAdditionalCapacity(final long capacity);

    C ensureCapacity(final long capacity);

    /**
     * Forces buffer to contain the given number of entries, preserving
     * just a part of the array.
     *
     * @param length the new minimum length for this array.
     * @param preserve the number of elements of the old buffer that shall be
     *            preserved in case a new allocation is necessary.
     * @return itself (fluent design)
     */
    C forceCapacity(final long length, final long preserve);

    /**
     * @return {@code true} if, and only if, there is at least one element remaining in this buffer
     */
    boolean hasRemaining();

    /**
     * @return {@code true} if, and only if, this buffer is read-only
     */
    boolean isReadOnly();

    /**
     * @return the limit of this buffer
     */
    long limit();

    /**
     * Sets this buffer's limit. If the position is larger than the new limit
     * then it is set to the new limit. If the mark is defined and larger than
     * the new limit then it is discarded.
     *
     * @param newLimit the new limit value; must be non-negative and no larger than this buffer's capacity
     * @return itself (fluent design)
     */
    C limit(final int newLimit);

    /**
     * For efficiency/performance reasons the buffer implementation is not required to safe-guard each put/get method
     * independently.
     * Thus the user-code should acquire the given lock around a set of put/get appropriately.
     *
     * @return the read-write lock
     */
    ReadWriteLock lock();

    /**
     * @return the position of this buffer
     */
    long position();

    /**
     * Sets this buffer's position. If the mark is defined and larger than the
     * new position then it is discarded.
     *
     * @param newPosition the new position value; must be non-negative and no larger than the current limit
     * @return itself (fluent design)
     */
    C position(final long newPosition);

    /**
     * @return the number of elements remaining in this buffer
     */
    long remaining();

    /**
     * resets the buffer read/write position to zero
     *
     * @return itself (fluent design)
     */
    C reset();

    /**
     * Trims the internal buffer array so that the capacity is equal to the
     * size.
     *
     * @see java.util.ArrayList#trimToSize()
     * @return itself (fluent design)
     */
    C trim();

    /**
     * Trims the internal buffer array if it is too large.
     * If the current array length is smaller than or equal to {@code n}, this
     * method does nothing. Otherwise, it trims the array length to the maximum
     * between {@code requestedCapacity} and {@link #capacity()}.
     * <p>
     * This method is useful when reusing FastBuffers. {@linkplain #reset()
     * Clearing a list} leaves the array length untouched. If you are reusing a
     * list many times, you can call this method with a typical size to avoid
     * keeping around a very large array just because of a few large transient
     * lists.
     *
     * @param requestedCapacity the threshold for the trimming.
     * @return itself (fluent design)
     */
    C trim(final int requestedCapacity);

}
