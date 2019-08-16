package de.gsi.dataset.utils.trees;

/**
 * User: Vitaly Sazanovich Date: 07/02/13 Time: 19:23 Email: Vitaly.Sazanovich@gmail.com
 */
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * This class provides a skeletal implementation of the <code>Map</code> interface, to minimize the effort required to
 * implement this interface.
 * </p>
 * <p>
 * To implement an unmodifiable map, the programmer needs only to extend this class and provide an implementation for
 * the <code>entrySet</code> method, which returns a set-view of the map's mappings. Typically, the returned set will,
 * in turn, be implemented atop <code>AbstractSet</code>. This set should not support the <code>add</code> or
 * <code>remove</code> methods, and its iterator should not support the <code>remove</code> method.
 * </p>
 * <p>
 * To implement a modifiable map, the programmer must additionally override this class's <code>put</code> method (which
 * otherwise throws an <code>UnsupportedOperationException</code>), and the iterator returned by
 * <code>entrySet().iterator()</code> must additionally implement its <code>remove</code> method.
 * </p>
 * <p>
 * The programmer should generally provide a void (no argument) and map constructor, as per the recommendation in the
 * <code>Map</code> interface specification.
 * </p>
 * <p>
 * The documentation for each non-abstract method in this class describes its implementation in detail. Each of these
 * methods may be overridden if the map being implemented admits a more efficient implementation.
 * </p>
 * <p>
 * This class is a member of the <a href="{@docRoot}/../technotes/guides/collections/index.html"> Java Collections
 * Framework</a>.
 * </p>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @author Josh Bloch
 * @author Neal Gafter
 * @version %I%, %G%
 * @see java.util.Map
 * @see java.util.Collection
 * @since 1.2
 */

@SuppressWarnings("unchecked")
public abstract class AbstractMap<K, V> implements Map<K, V> {

    /**
     * Each of these fields are initialized to contain an instance of the appropriate view the first time this view is
     * requested. The views are stateless, so there's no reason to create more than one of each.
     */
    transient volatile Set<K> keySet = null;

    // Query Operations

    transient volatile Collection<V> values = null;

    /**
     * Sole constructor. (For invocation by subclass constructors, typically implicit.)
     */
    protected AbstractMap() {
    }

    /**
     * <p>
     * This implementation calls <code>entrySet().clear()</code>.
     * </p>
     * <p>
     * Note that this implementation throws an <code>UnsupportedOperationException</code> if the <code>entrySet</code>
     * does not support the <code>clear</code> operation.
     * </p>
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     */
    @Override
    public void clear() {
        entrySet().clear();
    }

    /**
     * Returns a shallow copy of this <code>AbstractMap</code> instance: the keys and values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        final AbstractMap<K, V> result = (AbstractMap<K, V>) super.clone();
        result.keySet = null;
        result.values = null;
        return result;
    }

    /**
     * This implementation iterates over <code>entrySet()</code> searching for an entry with the specified key. If such
     * an entry is found, <code>true</code> is returned. If the iteration terminates without finding such an entry,
     * <code>false</code> is returned. Note that this implementation requires linear time in the size of the map; many
     * implementations will override this method.
     *
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public boolean containsKey(final Object key) {
        final Iterator<Map.Entry<K, V>> i = entrySet().iterator();
        if (key == null) {
            while (i.hasNext()) {
                final Entry<K, V> e = i.next();
                if (e.getKey() == null) {
                    return true;
                }
            }
        } else {
            while (i.hasNext()) {
                final Entry<K, V> e = i.next();
                if (key.equals(e.getKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Modification Operations

    /**
     * This implementation iterates over <code>entrySet()</code> searching for an entry with the specified value. If
     * such an entry is found, <code>true</code> is returned. If the iteration terminates without finding such an entry,
     * <code>false</code> is returned. Note that this implementation requires linear time in the size of the map.
     *
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public boolean containsValue(final Object value) {
        final Iterator<Entry<K, V>> i = entrySet().iterator();
        if (value == null) {
            while (i.hasNext()) {
                final Entry<K, V> e = i.next();
                if (e.getValue() == null) {
                    return true;
                }
            }
        } else {
            while (i.hasNext()) {
                final Entry<K, V> e = i.next();
                if (value.equals(e.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public abstract Set<Entry<K, V>> entrySet();

    // Bulk Operations

    /**
     * <p>
     * Compares the specified object with this map for equality. Returns <code>true</code> if the given object is also a
     * map and the two maps represent the same mappings. More formally, two maps <code>m1</code> and <code>m2</code>
     * represent the same mappings if <code>m1.entrySet().equals(m2.entrySet())</code>. This ensures that the
     * <code>equals</code> method works properly across different implementations of the <code>Map</code> interface.
     * </p>
     * <p>
     * This implementation first checks if the specified object is this map; if so it returns <code>true</code>. Then,
     * it checks if the specified object is a map whose size is identical to the size of this map; if not, it returns
     * <code>false</code>. If so, it iterates over this map's <code>entrySet</code> collection, and checks that the
     * specified map contains each mapping that this map contains. If the specified map fails to contain such a mapping,
     * <code>false</code> is returned. If the iteration completes, <code>true</code> is returned.
     * </p>
     *
     * @param o object to be compared for equality with this map
     * @return <code>true</code> if the specified object is equal to this map
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Map)) {
            return false;
        }
        final Map<K, V> m = (Map<K, V>) o;
        if (m.size() != size()) {
            return false;
        }

        try {
            final Iterator<Entry<K, V>> i = entrySet().iterator();
            while (i.hasNext()) {
                final Entry<K, V> e = i.next();
                final K key = e.getKey();
                final V value = e.getValue();
                if (value == null) {
                    if (!(m.get(key) == null && m.containsKey(key))) {
                        return false;
                    }
                } else if (!value.equals(m.get(key))) {
                    return false;
                }
            }
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }

        return true;
    }

    /**
     * This implementation iterates over <code>entrySet()</code> searching for an entry with the specified key. If such
     * an entry is found, the entry's value is returned. If the iteration terminates without finding such an entry,
     * <code>null</code> is returned. Note that this implementation requires linear time in the size of the map; many
     * implementations will override this method.
     *
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public V get(final Object key) {
        final Iterator<Entry<K, V>> i = entrySet().iterator();
        if (key == null) {
            while (i.hasNext()) {
                final Entry<K, V> e = i.next();
                if (e.getKey() == null) {
                    return e.getValue();
                }
            }
        } else {
            while (i.hasNext()) {
                final Entry<K, V> e = i.next();
                if (key.equals(e.getKey())) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    // Views

    /**
     * <p>
     * Returns the hash code value for this map. The hash code of a map is defined to be the sum of the hash codes of
     * each entry in the map's <code>entrySet()</code> view. This ensures that <code>m1.equals(m2)</code> implies that
     * <code>m1.hashCode()==m2.hashCode()</code> for any two maps <code>m1</code> and <code>m2</code>, as required by
     * the general contract of {@link Object#hashCode}.
     * </p>
     * <p>
     * This implementation iterates over <code>entrySet()</code>, calling {@link Map.Entry#hashCode hashCode()} on each
     * element (entry) in the set, and adding up the results.
     * </p>
     *
     * @return the hash code value for this map
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     */
    @Override
    public int hashCode() {
        int h = 0;
        final Iterator<Entry<K, V>> i = entrySet().iterator();
        while (i.hasNext()) {
            h += i.next().hashCode();
        }
        return h;
    }

    /**
     * This implementation returns <code>size() == 0</code>.
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * <p>
     * This implementation returns a set that subclasses {@link java.util.AbstractSet}. The subclass's iterator method
     * returns a "wrapper object" over this map's <code>entrySet()</code> iterator. The <code>size</code> method
     * delegates to this map's <code>size</code> method and the <code>contains</code> method delegates to this map's
     * <code>containsKey</code> method.
     * </p>
     * <p>
     * The set is created the first time this method is called, and returned in response to all subsequent calls. No
     * synchronization is performed, so there is a slight chance that multiple calls to this method will not all return
     * the same set.
     * </p>
     */
    @Override
    public Set<K> keySet() {
        if (keySet == null) {
            keySet = new AbstractSet<K>() {

                @Override
                public boolean contains(final Object k) {
                    return AbstractMap.this.containsKey(k);
                }

                @Override
                public Iterator<K> iterator() {
                    return new Iterator<K>() {

                        private final Iterator<Entry<K, V>> i = entrySet().iterator();

                        @Override
                        public boolean hasNext() {
                            return i.hasNext();
                        }

                        @Override
                        public K next() {
                            return i.next().getKey();
                        }

                        @Override
                        public void remove() {
                            i.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return AbstractMap.this.size();
                }
            };
        }
        return keySet;
    }

    /**
     * This implementation always throws an <code>UnsupportedOperationException</code>.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public V put(final K key, final V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation iterates over the specified map's <code>entrySet()</code> collection, and calls this map's
     * <code>put</code> operation once for each entry returned by the iteration.
     * </p>
     * <p>
     * Note that this implementation throws an <code>UnsupportedOperationException</code> if this map does not support
     * the <code>put</code> operation and the specified map is nonempty.
     * </p>
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        for (final Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    // Comparison and hashing

    /**
     * <p>
     * This implementation iterates over <code>entrySet()</code> searching for an entry with the specified key. If such
     * an entry is found, its value is obtained with its <code>getValue</code> operation, the entry is removed from the
     * collection (and the backing map) with the iterator's <code>remove</code> operation, and the saved value is
     * returned. If the iteration terminates without finding such an entry, <code>null</code> is returned. Note that
     * this implementation requires linear time in the size of the map; many implementations will override this method.
     * </p>
     * <p>
     * Note that this implementation throws an <code>UnsupportedOperationException</code> if the <code>entrySet</code>
     * iterator does not support the <code>remove</code> method and this map contains a mapping for the specified key.
     * </p>
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public V remove(final Object key) {
        final Iterator<Entry<K, V>> i = entrySet().iterator();
        Entry<K, V> correctEntry = null;
        if (key == null) {
            while (correctEntry == null && i.hasNext()) {
                final Entry<K, V> e = i.next();
                if (e.getKey() == null) {
                    correctEntry = e;
                }
            }
        } else {
            while (correctEntry == null && i.hasNext()) {
                final Entry<K, V> e = i.next();
                if (key.equals(e.getKey())) {
                    correctEntry = e;
                }
            }
        }

        V oldValue = null;
        if (correctEntry != null) {
            oldValue = correctEntry.getValue();
            i.remove();
        }
        return oldValue;
    }

    /**
     * This implementation returns <code>entrySet().size()</code>.
     */
    @Override
    public int size() {
        return entrySet().size();
    }

    /**
     * <p>
     * Returns a string representation of this map. The string representation consists of a list of key-value mappings
     * in the order returned by the map's <code>entrySet</code> view's iterator, enclosed in braces (<code>"{}"</code>).
     * Adjacent mappings are separated by the characters <code>", "</code> (comma and space). Each key-value mapping is
     * rendered as the key followed by an equals sign (<code>"="</code>) followed by the associated value. Keys and
     * values are converted to strings as by {@link String#valueOf(Object)}.
     * </p>
     *
     * @return a string representation of this map
     */
    @Override
    public String toString() {
        final Iterator<Entry<K, V>> i = entrySet().iterator();
        if (!i.hasNext()) {
            return "{}";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (;;) {
            final Entry<K, V> e = i.next();
            final K key = e.getKey();
            final V value = e.getValue();
            sb.append(key == this ? "(this Map)" : key);
            sb.append('=');
            sb.append(value == this ? "(this Map)" : value);
            if (!i.hasNext()) {
                return sb.append('}').toString();
            }
            sb.append(", ");
        }
    }

    /**
     * <p>
     * This implementation returns a collection that subclasses {@link AbstractCollection}. The subclass's iterator
     * method returns a "wrapper object" over this map's <code>entrySet()</code> iterator. The <code>size</code> method
     * delegates to this map's <code>size</code> method and the <code>contains</code> method delegates to this map's
     * <code>containsValue</code> method.
     * </p>
     * <p>
     * The collection is created the first time this method is called, and returned in response to all subsequent calls.
     * No synchronization is performed, so there is a slight chance that multiple calls to this method will not all
     * return the same collection.
     * </p>
     */
    @Override
    public Collection<V> values() {
        if (values == null) {
            values = new AbstractCollection<V>() {

                @Override
                public boolean contains(final Object v) {
                    return AbstractMap.this.containsValue(v);
                }

                @Override
                public Iterator<V> iterator() {
                    return new Iterator<V>() {

                        private final Iterator<Entry<K, V>> i = entrySet().iterator();

                        @Override
                        public boolean hasNext() {
                            return i.hasNext();
                        }

                        @Override
                        public V next() {
                            return i.next().getValue();
                        }

                        @Override
                        public void remove() {
                            i.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return AbstractMap.this.size();
                }
            };
        }
        return values;
    }

    /**
     * Utility method for SimpleEntry and SimpleImmutableEntry. Test for equality, checking for nulls.
     * 
     * @param o1 object1 to be checked
     * @param o2 object2 to be checked
     * @return true if equal
     */
    private static boolean eq(final Object o1, final Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    // Implementation Note: SimpleEntry and SimpleImmutableEntry
    // are distinct unrelated classes, even though they share
    // some code. Since you can't add or subtract final-ness
    // of a field in a subclass, they can't share representations,
    // and the amount of duplicated code is too small to warrant
    // exposing a common abstract class.

    /**
     * An Entry maintaining a key and a value. The value may be changed using the <code>setValue</code> method. This
     * class facilitates the process of building custom map implementations. For example, it may be convenient to return
     * arrays of <code>SimpleEntry</code> instances in method <code>Map.entrySet().toArray</code>.
     * 
     * @param <K> generic key
     * @param <V> generic value
     *
     * @since 1.6
     */
    public static class SimpleEntry<K, V> implements Entry<K, V>, java.io.Serializable {

        private static final long serialVersionUID = -8499721149061103585L;

        private final K key;
        private V value;

        /**
         * Creates an entry representing the same mapping as the specified entry.
         *
         * @param entry the entry to copy
         */
        public SimpleEntry(final Entry<? extends K, ? extends V> entry) {
            this.key = entry.getKey();
            this.value = entry.getValue();
        }

        /**
         * Creates an entry representing a mapping from the specified key to the specified value.
         *
         * @param key the key represented by this entry
         * @param value the value represented by this entry
         */
        public SimpleEntry(final K key, final V value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Compares the specified object with this entry for equality. Returns {@code true} if the given object is also
         * a map entry and the two entries represent the same mapping. More formally, two entries {@code e1} and
         * {@code e2} represent the same mapping if
         *
         * <pre>
         * (e1.getKey() == null ? e2.getKey() == null : e1.getKey().equals(e2.getKey()))
         *         &amp;&amp; (e1.getValue() == null ? e2.getValue() == null : e1.getValue().equals(e2.getValue()))
         * </pre>
         *
         * This ensures that the {@code equals} method works properly across different implementations of the
         * {@code Map.Entry} interface.
         *
         * @param o object to be compared for equality with this map entry
         * @return {@code true} if the specified object is equal to this map entry
         * @see #hashCode
         */
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<K, V> e = (Map.Entry<K, V>) o;
            return AbstractMap.eq(key, e.getKey()) && AbstractMap.eq(value, e.getValue());
        }

        /**
         * Returns the key corresponding to this entry.
         *
         * @return the key corresponding to this entry
         */
        @Override
        public K getKey() {
            return key;
        }

        /**
         * Returns the value corresponding to this entry.
         *
         * @return the value corresponding to this entry
         */
        @Override
        public V getValue() {
            return value;
        }

        /**
         * Returns the hash code value for this map entry. The hash code of a map entry {@code e} is defined to be:
         *
         * <pre>
         * (e.getKey() == null ? 0 : e.getKey().hashCode()) ^ (e.getValue() == null ? 0 : e.getValue().hashCode())
         * </pre>
         *
         * This ensures that {@code e1.equals(e2)} implies that {@code e1.hashCode()==e2.hashCode()} for any two Entries
         * {@code e1} and {@code e2}, as required by the general contract of {@link Object#hashCode}.
         *
         * @return the hash code value for this map entry
         * @see #equals
         */
        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        /**
         * Replaces the value corresponding to this entry with the specified value.
         *
         * @param value new value to be stored in this entry
         * @return the old value corresponding to the entry
         */
        @Override
        public V setValue(final V value) {
            final V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        /**
         * Returns a String representation of this map entry. This implementation returns the string representation of
         * this entry's key followed by the equals character ("<code>=</code>") followed by the string representation of
         * this entry's value.
         *
         * @return a String representation of this map entry
         */
        @Override
        public String toString() {
            return key + "=" + value;
        }

    }

    /**
     * An Entry maintaining an immutable key and value. This class does not support method <code>setValue</code>. This
     * class may be convenient in methods that return thread-safe snapshots of key-value mappings.
     * 
     * @param <K> generic key
     * @param <V> generic value
     *
     * @since 1.6
     */
    public static class SimpleImmutableEntry<K, V> implements Entry<K, V>, java.io.Serializable {

        private static final long serialVersionUID = 7138329143949025153L;

        private final K key;
        private final V value;

        /**
         * Creates an entry representing the same mapping as the specified entry.
         *
         * @param entry the entry to copy
         */
        public SimpleImmutableEntry(final Entry<? extends K, ? extends V> entry) {
            this.key = entry.getKey();
            this.value = entry.getValue();
        }

        /**
         * Creates an entry representing a mapping from the specified key to the specified value.
         *
         * @param key the key represented by this entry
         * @param value the value represented by this entry
         */
        public SimpleImmutableEntry(final K key, final V value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Compares the specified object with this entry for equality. Returns {@code true} if the given object is also
         * a map entry and the two entries represent the same mapping. More formally, two entries {@code e1} and
         * {@code e2} represent the same mapping if
         *
         * <pre>
         * (e1.getKey() == null ? e2.getKey() == null : e1.getKey().equals(e2.getKey()))
         *         &amp;&amp; (e1.getValue() == null ? e2.getValue() == null : e1.getValue().equals(e2.getValue()))
         * </pre>
         *
         * This ensures that the {@code equals} method works properly across different implementations of the
         * {@code Map.Entry} interface.
         *
         * @param o object to be compared for equality with this map entry
         * @return {@code true} if the specified object is equal to this map entry
         * @see #hashCode
         */
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<K, V> e = (Map.Entry<K, V>) o;
            return AbstractMap.eq(key, e.getKey()) && AbstractMap.eq(value, e.getValue());
        }

        /**
         * Returns the key corresponding to this entry.
         *
         * @return the key corresponding to this entry
         */
        @Override
        public K getKey() {
            return key;
        }

        /**
         * Returns the value corresponding to this entry.
         *
         * @return the value corresponding to this entry
         */
        @Override
        public V getValue() {
            return value;
        }

        /**
         * Returns the hash code value for this map entry. The hash code of a map entry {@code e} is defined to be:
         *
         * <pre>
         * (e.getKey() == null ? 0 : e.getKey().hashCode()) ^ (e.getValue() == null ? 0 : e.getValue().hashCode())
         * </pre>
         *
         * This ensures that {@code e1.equals(e2)} implies that {@code e1.hashCode()==e2.hashCode()} for any two Entries
         * {@code e1} and {@code e2}, as required by the general contract of {@link Object#hashCode}.
         *
         * @return the hash code value for this map entry
         * @see #equals
         */
        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        /**
         * Replaces the value corresponding to this entry with the specified value (optional operation). This
         * implementation simply throws <code>UnsupportedOperationException</code>, as this class implements an
         * <i>immutable</i> map entry.
         *
         * @param value new value to be stored in this entry
         * @return (Does not return)
         * @throws UnsupportedOperationException always
         */
        @Override
        public V setValue(final V value) {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns a String representation of this map entry. This implementation returns the string representation of
         * this entry's key followed by the equals character ("<code>=</code>") followed by the string representation of
         * this entry's value.
         *
         * @return a String representation of this map entry
         */
        @Override
        public String toString() {
            return key + "=" + value;
        }

    }

}
