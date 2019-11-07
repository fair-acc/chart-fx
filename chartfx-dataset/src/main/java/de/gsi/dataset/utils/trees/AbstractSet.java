package de.gsi.dataset.utils.trees;

/**
 * User: Vitaly Sazanovich Date: 07/02/13 Time: 19:25 Email: Vitaly.Sazanovich@gmail.com
 */
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * <p>
 * This class provides a skeletal implementation of the <code>Set</code> interface to minimize the effort required to
 * implement this interface.
 * </p>
 * <p>
 * The process of implementing a set by extending this class is identical to that of implementing a Collection by
 * extending AbstractCollection, except that all of the methods and constructors in subclasses of this class must obey
 * the additional constraints imposed by the <code>Set</code> interface (for instance, the add method must not permit
 * addition of multiple instances of an object to a set).
 * </p>
 * <p>
 * Note that this class does not override any of the implementations from the <code>AbstractCollection</code> class. It
 * merely adds implementations for <code>equals</code> and <code>hashCode</code>.
 * </p>
 * <p>
 * This class is a member of the <a href="{@docRoot}/../technotes/guides/collections/index.html"> Java Collections
 * Framework</a>.
 * </p>
 *
 * @param <E> the type of elements maintained by this set
 * @author Josh Bloch
 * @author Neal Gafter
 * @version %I%, %G%
 * @see java.util.Collection
 * @see java.util.AbstractCollection
 * @see java.util.Set
 * @since 1.2
 */
@SuppressWarnings("unchecked")
public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E> {

    /**
     * Sole constructor. (For invocation by subclass constructors, typically implicit.)
     */
    protected AbstractSet() {
    }

    // Comparison and hashing

    /**
     * Compares the specified object with this set for equality. Returns <code>true</code> if the given object is also a
     * set, the two sets have the same size, and every member of the given set is contained in this set. This ensures
     * that the <code>equals</code> method works properly across different implementations of the <code>Set</code>
     * interface.
     * 
     * This implementation first checks if the specified object is this set; if so it returns <code>true</code>. Then,
     * it checks if the specified object is a set whose size is identical to the size of this set; if not, it returns
     * false. If so, it returns <code>containsAll((Collection) o)</code>.
     *
     * @param o object to be compared for equality with this set
     * @return <code>true</code> if the specified object is equal to this set
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Set)) {
            return false;
        }
        final Collection<E> c = (Collection<E>) o;
        if (c.size() != size()) {
            return false;
        }
        try {
            return containsAll(c);
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }
    }

    /**
     * Returns the hash code value for this set. The hash code of a set is defined to be the sum of the hash codes of
     * the elements in the set, where the hash code of a <code>null</code> element is defined to be zero. This ensures
     * that <code>s1.equals(s2)</code> implies that <code>s1.hashCode()==s2.hashCode()</code> for any two sets
     * <code>s1</code> and <code>s2</code>, as required by the general contract of {@link Object#hashCode}.
     * 
     * This implementation iterates over the set, calling the <code>hashCode</code> method on each element in the set,
     * and adding up the results.
     *
     * @return the hash code value for this set
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     */
    @Override
    public int hashCode() {
        int h = 0;
        final Iterator<E> i = iterator();
        while (i.hasNext()) {
            final E obj = i.next();
            if (obj != null) {
                h += obj.hashCode();
            }
        }
        return h;
    }

    /**
     * Removes from this set all of its elements that are contained in the specified collection (optional operation). If
     * the specified collection is also a set, this operation effectively modifies this set so that its value is the
     * <i>asymmetric set difference</i> of the two sets.
     * 
     * This implementation determines which is the smaller of this set and the specified collection, by invoking the
     * <code>size</code> method on each. If this set has fewer elements, then the implementation iterates over this set,
     * checking each element returned by the iterator in turn to see if it is contained in the specified collection. If
     * it is so contained, it is removed from this set with the iterator's <code>remove</code> method. If the specified
     * collection has fewer elements, then the implementation iterates over the specified collection, removing from this
     * set each element returned by the iterator, using this set's <code>remove</code> method.
     * 
     * Note that this implementation will throw an <code>UnsupportedOperationException</code> if the iterator returned
     * by the <code>iterator</code> method does not implement the <code>remove</code> method.
     *
     * @param c collection containing elements to be removed from this set
     * @return <code>true</code> if this set changed as a result of the call
     * @throws UnsupportedOperationException if the <code>removeAll</code> operation is not supported by this set
     * @throws ClassCastException if the class of an element of this set is incompatible with the specified collection
     *         (optional)
     * @throws NullPointerException if this set contains a null element and the specified collection does not permit
     *         null elements (optional), or if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;

        if (size() > c.size()) {
            for (final Object name : c) {
                modified |= remove(name);
            }
        } else {
            for (final Iterator<?> i = iterator(); i.hasNext();) {
                if (c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
        }
        return modified;
    }

}
