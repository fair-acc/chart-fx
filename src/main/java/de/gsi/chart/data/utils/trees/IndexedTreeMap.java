package de.gsi.chart.data.utils.trees;

/**
 * User: Vitaly Sazanovich Date: 07/02/13 Time: 19:16 Email: Vitaly.Sazanovich@gmail.com
 */
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * A Red-Black tree based {@link java.util.NavigableMap} implementation. The map is sorted according to the
 * {@linkplain Comparable natural ordering} of its keys, or by a {@link java.util.Comparator} provided at map creation
 * time, depending on which constructor is used.
 * <p/>
 * <p>
 * This implementation provides guaranteed log(n) time cost for the <tt>containsKey</tt>, <tt>get</tt>, <tt>put</tt> and
 * <tt>remove</tt> operations. Algorithms are adaptations of those in Cormen, Leiserson, and Rivest's <I>Introduction to
 * Algorithms</I>.
 * <p/>
 * <p>
 * Note that the ordering maintained by a sorted map (whether or not an explicit comparator is provided) must be
 * <i>consistent with equals</i> if this sorted map is to correctly implement the <tt>Map</tt> interface. (See
 * <tt>Comparable</tt> or <tt>Comparator</tt> for a precise definition of <i>consistent with equals</i>.) This is so
 * because the <tt>Map</tt> interface is defined in terms of the equals operation, but a map performs all key
 * comparisons using its <tt>compareTo</tt> (or <tt>compare</tt>) method, so two keys that are deemed equal by this
 * method are, from the standpoint of the sorted map, equal. The behavior of a sorted map <i>is</i> well-defined even if
 * its ordering is inconsistent with equals; it just fails to obey the general contract of the <tt>Map</tt> interface.
 * <p/>
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If multiple threads access a map concurrently,
 * and at least one of the threads modifies the map structurally, it <i>must</i> be synchronized externally. (A
 * structural modification is any operation that adds or deletes one or more mappings; merely changing the value
 * associated with an existing key is not a structural modification.) This is typically accomplished by synchronizing on
 * some object that naturally encapsulates the map. If no such object exists, the map should be "wrapped" using the
 * {@link java.util.Collections#synchronizedSortedMap Collections.synchronizedSortedMap} method. This is best done at
 * creation time, to prevent accidental unsynchronized access to the map:
 * 
 * <pre>
 *   SortedMap m = Collections.synchronizedSortedMap(new IndexedTreeMap(...));
 * </pre>
 * <p/>
 * <p>
 * The iterators returned by the <tt>iterator</tt> method of the collections returned by all of this class's "collection
 * view methods" are <i>fail-fast</i>: if the map is structurally modified at any time after the iterator is created, in
 * any way except through the iterator's own <tt>remove</tt> method, the iterator will throw a
 * {@link java.util.ConcurrentModificationException}. Thus, in the face of concurrent modification, the iterator fails
 * quickly and cleanly, rather than risking arbitrary, non-deterministic behavior at an undetermined time in the future.
 * <p/>
 * <p>
 * Note that the fail-fast behavior of an iterator cannot be guaranteed as it is, generally speaking, impossible to make
 * any hard guarantees in the presence of unsynchronized concurrent modification. Fail-fast iterators throw
 * <tt>ConcurrentModificationException</tt> on a best-effort basis. Therefore, it would be wrong to write a program that
 * depended on this exception for its correctness: <i>the fail-fast behavior of iterators should be used only to detect
 * bugs.</i>
 * <p/>
 * <p>
 * All <tt>Map.Entry</tt> pairs returned by methods in this class and its views represent snapshots of mappings at the
 * time they were produced. They do <em>not</em> support the <tt>Entry.setValue</tt> method. (Note however that it is
 * possible to change mappings in the associated map using <tt>put</tt>.)
 * <p/>
 * <p>
 * This class is a member of the <a href="{@docRoot}/../technotes/guides/collections/index.html"> Java Collections
 * Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @author Josh Bloch, Doug Lea, Vitaly Sazanovich
 * @version 1.73, 05/10/06
 * @see java.util.Map
 * @see java.util.HashMap
 * @see java.util.Hashtable
 * @see Comparable
 * @see java.util.Comparator
 * @see java.util.Collection
 * @since 1.2
 */

@SuppressWarnings("unchecked")
public class IndexedTreeMap<K, V> extends AbstractMap<K, V>
        implements IndexedNavigableMap<K, V>, Cloneable, java.io.Serializable {

    /**
     * The comparator used to maintain order in this tree map, or null if it uses the natural ordering of its keys.
     *
     * @serial
     */
    private final Comparator<? super K> comparator;

    private transient Entry<K, V> root = null;

    /**
     * The number of entries in the tree
     */
    private transient int size = 0;

    /**
     * The number of structural modifications to the tree.
     */
    private transient int modCount = 0;

    /**
     * Constructs a new, empty tree map, using the natural ordering of its keys. All keys inserted into the map must
     * implement the {@link Comparable} interface. Furthermore, all such keys must be <i>mutually comparable</i>:
     * <tt>k1.compareTo(k2)</tt> must not throw a <tt>ClassCastException</tt> for any keys <tt>k1</tt> and <tt>k2</tt>
     * in the map. If the user attempts to put a key into the map that violates this constraint (for example, the user
     * attempts to put a string key into a map whose keys are integers), the <tt>put(Object key, Object value)</tt> call
     * will throw a <tt>ClassCastException</tt>.
     */
    public IndexedTreeMap() {
        comparator = null;
    }

    /**
     * Constructs a new, empty tree map, ordered according to the given comparator. All keys inserted into the map must
     * be <i>mutually comparable</i> by the given comparator: <tt>comparator.compare(k1,
     * k2)</tt> must not throw a <tt>ClassCastException</tt> for any keys <tt>k1</tt> and <tt>k2</tt> in the map. If the
     * user attempts to put a key into the map that violates this constraint, the <tt>put(Object
     * key, Object value)</tt> call will throw a <tt>ClassCastException</tt>.
     *
     * @param comparator the comparator that will be used to order this map. If <tt>null</tt>, the
     *            {@linkplain Comparable natural ordering} of the keys will be used.
     */
    public IndexedTreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    /**
     * Constructs a new tree map containing the same mappings as the given map, ordered according to the <i>natural
     * ordering</i> of its keys. All keys inserted into the new map must implement the {@link Comparable} interface.
     * Furthermore, all such keys must be <i>mutually comparable</i>: <tt>k1.compareTo(k2)</tt> must not throw a
     * <tt>ClassCastException</tt> for any keys <tt>k1</tt> and <tt>k2</tt> in the map. This method runs in n*log(n)
     * time.
     *
     * @param m the map whose mappings are to be placed in this map
     * @throws ClassCastException if the keys in m are not {@link Comparable}, or are not mutually comparable
     * @throws NullPointerException if the specified map is null
     */
    public IndexedTreeMap(Map<? extends K, ? extends V> m) {
        comparator = null;
        putAll(m);
    }

    /**
     * Constructs a new tree map containing the same mappings and using the same ordering as the specified sorted map.
     * This method runs in linear time.
     *
     * @param m the sorted map whose mappings are to be placed in this map, and whose comparator is to be used to sort
     *            this map
     * @throws NullPointerException if the specified map is null
     */
    public IndexedTreeMap(SortedMap<K, ? extends V> m) {
        comparator = m.comparator();
        try {
            buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
        } catch (java.io.IOException | ClassNotFoundException cannotHappen) {
            // do nothing
        }
    }

    // Query Operations

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified key.
     *
     * @param key key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified key
     * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
     * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     */
    @Override
    public boolean containsKey(Object key) {
        return getEntry((K) key) != null;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the specified value. More formally, returns
     * <tt>true</tt> if and only if this map contains at least one mapping to a value <tt>v</tt> such that
     * <tt>(value==null ? v==null : value.equals(v))</tt>. This operation will probably require time linear in the map
     * size for most implementations.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if a mapping to <tt>value</tt> exists; <tt>false</tt> otherwise
     * @since 1.2
     */
    @Override
    public boolean containsValue(Object value) {
        for (Entry<K, V> e = getFirstEntry(); e != null; e = IndexedTreeMap.successor(e)) {
            if (IndexedTreeMap.valEquals(value, e.value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the
     * key.
     * <p/>
     * <p>
     * More formally, if this map contains a mapping from a key {@code k} to a value {@code v} such that {@code key}
     * compares equal to {@code k} according to the map's ordering, then this method returns {@code v}; otherwise it
     * returns {@code null}. (There can be at most one such mapping.)
     * <p/>
     * <p>
     * A return value of {@code null} does not <i>necessarily</i> indicate that the map contains no mapping for the key;
     * it's also possible that the map explicitly maps the key to {@code null}. The {@link #containsKey containsKey}
     * operation may be used to distinguish these two cases.
     *
     * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
     * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     */
    @Override
    public V get(Object key) {
        final Entry<K, V> p = getEntry((K) key);
        return p == null ? null : p.value;
    }

    @Override
    public Comparator<? super K> comparator() {
        return comparator;
    }

    /**
     * @throws java.util.NoSuchElementException {@inheritDoc}
     */
    @Override
    public K firstKey() {
        return IndexedTreeMap.key(getFirstEntry());
    }

    /**
     * @throws java.util.NoSuchElementException {@inheritDoc}
     */
    @Override
    public K lastKey() {
        return IndexedTreeMap.key(getLastEntry());
    }

    /**
     * Copies all of the mappings from the specified map to this map. These mappings replace any mappings that this map
     * had for any of the keys currently in the specified map.
     *
     * @param map mappings to be stored in this map
     * @throws ClassCastException if the class of a key or value in the specified map prevents it from being stored in
     *             this map
     * @throws NullPointerException if the specified map is null or the specified map contains a null key and this map
     *             does not permit null keys
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        final int mapSize = map.size();
        if (size == 0 && mapSize != 0 && map instanceof SortedMap) {
            final Comparator<?> c = ((SortedMap<? extends K, ? extends V>) map).comparator();
            if (c == comparator || c != null && c.equals(comparator)) {
                ++modCount;
                try {
                    buildFromSorted(mapSize, map.entrySet().iterator(), null, null);
                } catch (java.io.IOException | ClassNotFoundException cannotHappen) {
                    // cannot happen
                }
                return;
            }
        }
        super.putAll(map);
    }

    /**
     * Returns this map's entry for the given key, or <tt>null</tt> if the map does not contain an entry for the key.
     *
     * @return this map's entry for the given key, or <tt>null</tt> if the map does not contain an entry for the key
     * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
     * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     */
    final Entry<K, V> getEntry(K key) {
        // Offload comparator-based version for sake of performance
        if (comparator != null) {
            return getEntryUsingComparator(key);
        }
        if (key == null) {
            throw new NullPointerException();
        }
        final Comparable<? super K> k = (Comparable<? super K>) key;
        Entry<K, V> p = root;
        while (p != null) {
            final int cmp = k.compareTo(p.key);
            if (cmp < 0) {
                p = p.left;
            } else if (cmp > 0) {
                p = p.right;
            } else {
                return p;
            }
        }
        return null;
    }

    /**
     * Version of getEntry using comparator. Split off from getEntry for performance. (This is not worth doing for most
     * methods, that are less dependent on comparator performance, but is worthwhile here.)
     */
    final Entry<K, V> getEntryUsingComparator(K key) {
        final K k = key;
        final Comparator<? super K> cpr = comparator;
        if (cpr != null) {
            Entry<K, V> p = root;
            while (p != null) {
                final int cmp = cpr.compare(k, p.key);
                if (cmp < 0) {
                    p = p.left;
                } else if (cmp > 0) {
                    p = p.right;
                } else {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Gets the entry corresponding to the specified key; if no such entry exists, returns the entry for the least key
     * greater than the specified key; if no such entry exists (i.e., the greatest key in the Tree is less than the
     * specified key), returns <tt>null</tt>.
     */
    final Entry<K, V> getCeilingEntry(K key) {
        Entry<K, V> p = root;
        while (p != null) {
            final int cmp = compare(key, p.key);
            if (cmp < 0) {
                if (p.left != null) {
                    p = p.left;
                } else {
                    return p;
                }
            } else if (cmp > 0) {
                if (p.right != null) {
                    p = p.right;
                } else {
                    Entry<K, V> parent = p.parent;
                    Entry<K, V> ch = p;
                    while (parent != null && ch == parent.right) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            } else {
                return p;
            }
        }
        return null;
    }

    /**
     * Gets the entry corresponding to the specified key; if no such entry exists, returns the entry for the greatest
     * key less than the specified key; if no such entry exists, returns <tt>null</tt>.
     */
    final Entry<K, V> getFloorEntry(K key) {
        Entry<K, V> p = root;
        while (p != null) {
            final int cmp = compare(key, p.key);
            if (cmp > 0) {
                if (p.right != null) {
                    p = p.right;
                } else {
                    return p;
                }
            } else if (cmp < 0) {
                if (p.left != null) {
                    p = p.left;
                } else {
                    Entry<K, V> parent = p.parent;
                    Entry<K, V> ch = p;
                    while (parent != null && ch == parent.left) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            } else {
                return p;
            }

        }
        return null;
    }

    public void debug() throws Exception {
        Entry<K, V> e = getFirstEntry();
        while (e != null) {
            //            String l = e.left == null ? "null" : "   " + e.left.key.toString();
            //            String r = e.right == null ? "null" : "   " + e.right.key.toString();
            //            System.out.println(e.key + ":" + l + ":" + r + ":" + e.weight);
            if (e.weight != e.sumup()) {
                throw new Exception("Weight is incorrect:" + e.weight + "!=" + e.sumup() + " for " + e.key);
            }
            e = IndexedTreeMap.successor(e);
        }
    }

    public void dbg() {
        Entry<K, V> e = getFirstEntry();
        while (e != null) {
            final String l = e.left == null ? "null" : "   " + e.left.key;
            final String r = e.right == null ? "null" : "   " + e.right.key;
            System.out.println(e.key + ":" + l + ":" + r + ":" + e.weight);
            e = IndexedTreeMap.successor(e);
        }
    }

    /**
     * Gets the entry for the least key greater than the specified key; if no such entry exists, returns the entry for
     * the least key greater than the specified key; if no such entry exists returns <tt>null</tt>.
     */
    final Entry<K, V> getHigherEntry(K key) {
        Entry<K, V> p = root;
        while (p != null) {
            final int cmp = compare(key, p.key);
            if (cmp < 0) {
                if (p.left != null) {
                    p = p.left;
                } else {
                    return p;
                }
            } else if (p.right != null) {
                p = p.right;
            } else {
                Entry<K, V> parent = p.parent;
                Entry<K, V> ch = p;
                while (parent != null && ch == parent.right) {
                    ch = parent;
                    parent = parent.parent;
                }
                return parent;
            }
        }
        return null;
    }

    /**
     * Returns the entry for the greatest key less than the specified key; if no such entry exists (i.e., the least key
     * in the Tree is greater than the specified key), returns <tt>null</tt>.
     */
    final Entry<K, V> getLowerEntry(K key) {
        Entry<K, V> p = root;
        while (p != null) {
            final int cmp = compare(key, p.key);
            if (cmp > 0) {
                if (p.right != null) {
                    p = p.right;
                } else {
                    return p;
                }
            } else if (p.left != null) {
                p = p.left;
            } else {
                Entry<K, V> parent = p.parent;
                Entry<K, V> ch = p;
                while (parent != null && ch == parent.left) {
                    ch = parent;
                    parent = parent.parent;
                }
                return parent;
            }
        }
        return null;
    }

    /**
     * Associates the specified value with the specified key in this map. If the map previously contained a mapping for
     * the key, the old value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no mapping for
     *         <tt>key</tt>. (A <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt>
     *         with <tt>key</tt>.)
     * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
     * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     */
    @Override
    public V put(K key, V value) {
        Entry<K, V> t = root;
        if (t == null) {
            // TBD:
            // 5045147: (coll) Adding null to an empty IndexedTreeSet should
            // throw NullPointerException
            //
            // compare(key, key); // type check
            root = new Entry<>(key, value, null);
            root.weight = 1;
            size = 1;
            modCount++;
            return null;
        }
        int cmp;
        Entry<K, V> parent;
        // split comparator and comparable paths
        final Comparator<? super K> cpr = comparator;
        if (cpr != null) {
            do {
                parent = t;
                cmp = cpr.compare(key, t.key);
                if (cmp < 0) {
                    t = t.left;
                } else if (cmp > 0) {
                    t = t.right;
                } else {
                    return t.setValue(value);
                }
            } while (t != null);
        } else {
            if (key == null) {
                throw new NullPointerException();
            }
            final Comparable<? super K> k = (Comparable<? super K>) key;
            do {
                parent = t;
                cmp = k.compareTo(t.key);
                if (cmp < 0) {
                    t = t.left;
                } else if (cmp > 0) {
                    t = t.right;
                } else {
                    return t.setValue(value);
                }
            } while (t != null);
        }
        final Entry<K, V> e = new Entry<>(key, value, parent);
        if (cmp < 0) {
            parent.left = e;
        } else {
            parent.right = e;
        }
        e.updateWeight(1);

        fixAfterInsertion(e);
        size++;
        modCount++;
        return null;
    }

    /**
     * Removes the mapping for this key from this IndexedTreeMap if present.
     *
     * @param key key for which mapping should be removed
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no mapping for
     *         <tt>key</tt>. (A <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt>
     *         with <tt>key</tt>.)
     * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
     * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     */
    @Override
    public V remove(Object key) {
        final Entry<K, V> p = getEntry((K) key);
        if (p == null) {
            return null;
        }

        final V oldValue = p.value;
        deleteEntry(p);
        return oldValue;
    }

    /**
     * Removes all of the mappings from this map. The map will be empty after this call returns.
     */
    @Override
    public void clear() {
        modCount++;
        size = 0;
        root = null;
    }

    /**
     * Returns a shallow copy of this <tt>IndexedTreeMap</tt> instance. (The keys and values themselves are not cloned.)
     *
     * @return a shallow copy of this map
     */
    @Override
    public Object clone() {
        IndexedTreeMap<K, V> clone = null;
        try {
            clone = (IndexedTreeMap<K, V>) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new InternalError();
        }

        // Put clone into "virgin" state (except for comparator)
        clone.root = null;
        clone.size = 0;
        clone.modCount = 0;
        clone.entrySet = null;
        clone.navigableKeySet = null;
        clone.descendingMap = null;

        // Initialize clone with our mappings
        try {
            clone.buildFromSorted(size, entrySet().iterator(), null, null);
        } catch (java.io.IOException | ClassNotFoundException cannotHappen) {
            // cannot happen
        }

        return clone;
    }

    // NavigableMap API methods

    /**
     * @since 1.6
     */
    @Override
    public Map.Entry<K, V> firstEntry() {
        return IndexedTreeMap.exportEntry(getFirstEntry());
    }

    /**
     * @since 1.6
     */
    @Override
    public Map.Entry<K, V> lastEntry() {
        return IndexedTreeMap.exportEntry(getLastEntry());
    }

    /**
     * @since 1.6
     */
    @Override
    public Map.Entry<K, V> pollFirstEntry() {
        final Entry<K, V> p = getFirstEntry();
        final Map.Entry<K, V> result = IndexedTreeMap.exportEntry(p);
        if (p != null) {
            deleteEntry(p);
        }
        return result;
    }

    /**
     * @since 1.6
     */
    @Override
    public Map.Entry<K, V> pollLastEntry() {
        final Entry<K, V> p = getLastEntry();
        final Map.Entry<K, V> result = IndexedTreeMap.exportEntry(p);
        if (p != null) {
            deleteEntry(p);
        }
        return result;
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     * @since 1.6
     */
    @Override
    public Map.Entry<K, V> lowerEntry(K key) {
        return IndexedTreeMap.exportEntry(getLowerEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     * @since 1.6
     */
    @Override
    public K lowerKey(K key) {
        return IndexedTreeMap.keyOrNull(getLowerEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     * @since 1.6
     */
    @Override
    public Map.Entry<K, V> floorEntry(K key) {
        return IndexedTreeMap.exportEntry(getFloorEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     * @since 1.6
     */
    @Override
    public K floorKey(K key) {
        return IndexedTreeMap.keyOrNull(getFloorEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     * @since 1.6
     */
    @Override
    public Map.Entry<K, V> ceilingEntry(K key) {
        return IndexedTreeMap.exportEntry(getCeilingEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     * @since 1.6
     */
    @Override
    public K ceilingKey(K key) {
        return IndexedTreeMap.keyOrNull(getCeilingEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     * @since 1.6
     */
    @Override
    public Map.Entry<K, V> higherEntry(K key) {
        return IndexedTreeMap.exportEntry(getHigherEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     * @since 1.6
     */
    @Override
    public K higherKey(K key) {
        return IndexedTreeMap.keyOrNull(getHigherEntry(key));
    }

    @Override
    public K exactKey(int index) {
        if (index < 0 || index > size() - 1) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return getExactKey(root, index);
    }

    private K getExactKey(Entry<K, V> e, int index) {
        if (e.left == null && index == 0) {
            return e.key;
        }
        if (e.left == null && e.right == null) {
            return e.key;
        }
        if (e.left != null && e.left.weight > index) {
            return getExactKey(e.left, index);
        }
        if (e.left != null && e.left.weight == index) {
            return e.key;
        }
        return getExactKey(e.right, index - (e.left == null ? 0 : e.left.weight) - 1);
    }

    @Override
    public int keyIndex(K key) {
        if (key == null) {
            throw new NullPointerException();
        }
        final Entry<K, V> e = getEntry(key);
        if (e == null) {
            throw new NullPointerException();
        }
        if (e == root) {
            return IndexedTreeMap.getWeight(e) - IndexedTreeMap.getWeight(e.right) - 1;//index to return
        }
        int index = 0;
        int cmp;
        index += IndexedTreeMap.getWeight(e.left);
        Entry<K, V> p = e.parent;
        // split comparator and comparable paths
        final Comparator<? super K> cpr = comparator;
        if (cpr != null) {
            while (p != null) {
                cmp = cpr.compare(key, p.key);
                if (cmp > 0) {
                    index += IndexedTreeMap.getWeight(p.left) + 1;
                }
                p = p.parent;
            }
        } else {
            final Comparable<? super K> k = (Comparable<? super K>) key;
            while (p != null) {
                if (k.compareTo(p.key) > 0) {
                    index += IndexedTreeMap.getWeight(p.left) + 1;
                }
                p = p.parent;
            }
        }
        return index;
    }

    @Override
    public Entry<K, V> exactEntry(int index) {
        return getExactEntry(root, index);
    }

    private Entry<K, V> getExactEntry(Entry<K, V> e, int index) {
        if (e.left == null && index == 0) {
            return e;
        }
        if (e.left == null && e.right == null) {
            return e;
        }
        if (e.left != null && e.left.weight > index) {
            return getExactEntry(e.left, index);
        }
        if (e.left != null && e.left.weight == index) {
            return e;
        }
        return getExactEntry(e.right, index - (e.left == null ? 0 : e.left.weight) - 1);
    }

    // Views

    /**
     * Fields initialized to contain an instance of the entry set view the first time this view is requested. Views are
     * stateless, so there's no reason to create more than one.
     */
    private transient EntrySet entrySet = null;
    private transient KeySet<K> navigableKeySet = null;
    private transient NavigableMap<K, V> descendingMap = null;

    /**
     * Returns a {@link java.util.Set} view of the keys contained in this map. The set's iterator returns the keys in
     * ascending order. The set is backed by the map, so changes to the map are reflected in the set, and vice-versa. If
     * the map is modified while an iteration over the set is in progress (except through the iterator's own
     * <tt>remove</tt> operation), the results of the iteration are undefined. The set supports element removal, which
     * removes the corresponding mapping from the map, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations. It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     */
    @Override
    public Set<K> keySet() {
        return navigableKeySet();
    }

    /**
     * @since 1.6
     */
    @SuppressWarnings("rawtypes")
    @Override
    public NavigableSet<K> navigableKeySet() {
        final KeySet<K> nks = navigableKeySet;
        return nks != null ? nks : (navigableKeySet = new KeySet(this));
    }

    /**
     * @since 1.6
     */
    @Override
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map. The collection's iterator returns the
     * values in ascending order of the corresponding keys. The collection is backed by the map, so changes to the map
     * are reflected in the collection, and vice-versa. If the map is modified while an iteration over the collection is
     * in progress (except through the iterator's own <tt>remove</tt> operation), the results of the iteration are
     * undefined. The collection supports element removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt>
     * operations. It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     */
    @Override
    public Collection<V> values() {
        final Collection<V> vs = values;
        return vs != null ? vs : (values = new Values());
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map. The set's iterator returns the entries in
     * ascending key order. The set is backed by the map, so changes to the map are reflected in the set, and
     * vice-versa. If the map is modified while an iteration over the set is in progress (except through the iterator's
     * own <tt>remove</tt> operation, or through the <tt>setValue</tt> operation on a map entry returned by the
     * iterator) the results of the iteration are undefined. The set supports element removal, which removes the
     * corresponding mapping from the map, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations. It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        final EntrySet es = entrySet;
        return es != null ? es : (entrySet = new EntrySet());
    }

    /**
     * @since 1.6
     */
    @Override
    public NavigableMap<K, V> descendingMap() {
        final NavigableMap<K, V> km = descendingMap;
        return km != null ? km : (descendingMap = new DescendingSubMap<>(this, true, null, true, true, null, true));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if <tt>fromKey</tt> or <tt>toKey</tt> is null and this map uses natural ordering, or
     *             its comparator does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return new AscendingSubMap<>(this, false, fromKey, fromInclusive, false, toKey, toInclusive);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if <tt>toKey</tt> is null and this map uses natural ordering, or its comparator does
     *             not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return new AscendingSubMap<>(this, true, null, true, false, toKey, inclusive);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if <tt>fromKey</tt> is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return new AscendingSubMap<>(this, false, fromKey, inclusive, true, null, true);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if <tt>fromKey</tt> or <tt>toKey</tt> is null and this map uses natural ordering, or
     *             its comparator does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if <tt>toKey</tt> is null and this map uses natural ordering, or its comparator does
     *             not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public SortedMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if <tt>fromKey</tt> is null and this map uses natural ordering, or its comparator
     *             does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    // View class support

    class Values extends AbstractCollection<V> {

        @Override
        public Iterator<V> iterator() {
            return new ValueIterator(getFirstEntry());
        }

        @Override
        public int size() {
            return IndexedTreeMap.this.size();
        }

        @Override
        public boolean contains(Object o) {
            return IndexedTreeMap.this.containsValue(o);
        }

        @Override
        public boolean remove(Object o) {
            for (Entry<K, V> e = getFirstEntry(); e != null; e = IndexedTreeMap.successor(e)) {
                if (IndexedTreeMap.valEquals(e.getValue(), o)) {
                    deleteEntry(e);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            IndexedTreeMap.this.clear();
        }
    }

    class EntrySet extends AbstractSet<Map.Entry<K, V>> {

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator(getFirstEntry());
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
            final V value = entry.getValue();
            final Entry<K, V> p = getEntry(entry.getKey());
            return p != null && IndexedTreeMap.valEquals(p.getValue(), value);
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
            final V value = entry.getValue();
            final Entry<K, V> p = getEntry(entry.getKey());
            if (p != null && IndexedTreeMap.valEquals(p.getValue(), value)) {
                deleteEntry(p);
                return true;
            }
            return false;
        }

        @Override
        public int size() {
            return IndexedTreeMap.this.size();
        }

        @Override
        public void clear() {
            IndexedTreeMap.this.clear();
        }
    }

    /*
     * Unlike Values and EntrySet, the KeySet class is static,
     * delegating to a NavigableMap to allow use by SubMaps, which
     * outweighs the ugliness of needing type-tests for the following
     * Iterator methods that are defined appropriately in main versus
     * submap classes.
     */

    Iterator<K> keyIterator() {
        return new KeyIterator(getFirstEntry());
    }

    Iterator<K> descendingKeyIterator() {
        return new DescendingKeyIterator(getLastEntry());
    }

    static final class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {

        private final NavigableMap<E, Object> m;

        KeySet(NavigableMap<E, Object> map) {
            m = map;
        }

        @Override
        public Iterator<E> iterator() {
            if (m instanceof IndexedTreeMap) {
                return ((IndexedTreeMap<E, Object>) m).keyIterator();
            } else {
                return ((IndexedTreeMap.NavigableSubMap<E, Object>) m).keyIterator();
            }
        }

        @Override
        public Iterator<E> descendingIterator() {
            if (m instanceof IndexedTreeMap) {
                return ((IndexedTreeMap<E, Object>) m).descendingKeyIterator();
            } else {
                return ((IndexedTreeMap.NavigableSubMap<E, Object>) m).descendingKeyIterator();
            }
        }

        @Override
        public int size() {
            return m.size();
        }

        @Override
        public boolean isEmpty() {
            return m.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return m.containsKey(o);
        }

        @Override
        public void clear() {
            m.clear();
        }

        @Override
        public E lower(E e) {
            return m.lowerKey(e);
        }

        @Override
        public E floor(E e) {
            return m.floorKey(e);
        }

        @Override
        public E ceiling(E e) {
            return m.ceilingKey(e);
        }

        @Override
        public E higher(E e) {
            return m.higherKey(e);
        }

        @Override
        public E first() {
            return m.firstKey();
        }

        @Override
        public E last() {
            return m.lastKey();
        }

        @Override
        public Comparator<? super E> comparator() {
            return m.comparator();
        }

        @Override
        public E pollFirst() {
            final Map.Entry<E, Object> e = m.pollFirstEntry();
            return e == null ? null : e.getKey();
        }

        @Override
        public E pollLast() {
            final Map.Entry<E, Object> e = m.pollLastEntry();
            return e == null ? null : e.getKey();
        }

        @Override
        public boolean remove(Object o) {
            final int oldSize = size();
            m.remove(o);
            return size() != oldSize;
        }

        @Override
        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return new IndexedTreeSet<>(m.subMap(fromElement, fromInclusive, toElement, toInclusive));
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return new IndexedTreeSet<>(m.headMap(toElement, inclusive));
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return new IndexedTreeSet<>(m.tailMap(fromElement, inclusive));
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @Override
        public SortedSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }

        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return new IndexedTreeSet<>(m.descendingMap());
        }
    }

    /**
     * Base class for IndexedTreeMap Iterators
     */
    abstract class PrivateEntryIterator<T> implements Iterator<T> {

        Entry<K, V> next;
        Entry<K, V> lastReturned;
        int expectedModCount;

        PrivateEntryIterator(Entry<K, V> first) {
            expectedModCount = modCount;
            lastReturned = null;
            next = first;
        }

        @Override
        public final boolean hasNext() {
            return next != null;
        }

        final Entry<K, V> nextEntry() {
            final Entry<K, V> e = next;
            if (e == null) {
                throw new NoSuchElementException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            next = IndexedTreeMap.successor(e);
            lastReturned = e;
            return e;
        }

        final Entry<K, V> prevEntry() {
            final Entry<K, V> e = next;
            if (e == null) {
                throw new NoSuchElementException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            next = IndexedTreeMap.predecessor(e);
            lastReturned = e;
            return e;
        }

        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            // deleted entries are replaced by their successors
            if (lastReturned.left != null && lastReturned.right != null) {
                next = lastReturned;
            }
            deleteEntry(lastReturned);
            expectedModCount = modCount;
            lastReturned = null;
        }
    }

    final class EntryIterator extends PrivateEntryIterator<Map.Entry<K, V>> {

        EntryIterator(Entry<K, V> first) {
            super(first);
        }

        @Override
        public Map.Entry<K, V> next() {
            return nextEntry();
        }
    }

    final class ValueIterator extends PrivateEntryIterator<V> {

        ValueIterator(Entry<K, V> first) {
            super(first);
        }

        @Override
        public V next() {
            return nextEntry().value;
        }
    }

    final class KeyIterator extends PrivateEntryIterator<K> {

        KeyIterator(Entry<K, V> first) {
            super(first);
        }

        @Override
        public K next() {
            return nextEntry().key;
        }
    }

    final class DescendingKeyIterator extends PrivateEntryIterator<K> {

        DescendingKeyIterator(Entry<K, V> first) {
            super(first);
        }

        @Override
        public K next() {
            return prevEntry().key;
        }
    }

    // Little utilities

    /**
     * Compares two keys using the correct comparison method for this IndexedTreeMap.
     */
    final int compare(Object k1, Object k2) {
        return comparator == null ? ((Comparable<? super K>) k1).compareTo((K) k2) : comparator.compare((K) k1, (K) k2);
    }

    /**
     * Test two values for equality. Differs from o1.equals(o2) only in that it copes with <tt>null</tt> o1 properly.
     */
    static boolean valEquals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    /**
     * Return SimpleImmutableEntry for entry, or null if null
     */
    static <K, V> Map.Entry<K, V> exportEntry(IndexedTreeMap.Entry<K, V> e) {
        return e == null ? null : new java.util.AbstractMap.SimpleImmutableEntry<>(e);
    }

    /**
     * Return key for entry, or null if null
     */
    static <K, V> K keyOrNull(IndexedTreeMap.Entry<K, V> e) {
        return e == null ? null : e.key;
    }

    /**
     * Returns the key corresponding to the specified Entry.
     *
     * @throws NoSuchElementException if the Entry is null
     */
    static <K> K key(Entry<K, ?> e) {
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e.key;
    }

    // SubMaps

    /**
     * @serial include
     */
    static abstract class NavigableSubMap<K, V> extends java.util.AbstractMap<K, V>
            implements NavigableMap<K, V>, java.io.Serializable {

        private static final long serialVersionUID = 2043167872700380033L;

        /**
         * The backing map.
         */
        final IndexedTreeMap<K, V> m;

        /**
         * Endpoints are represented as triples (fromStart, lo, loInclusive) and (toEnd, hi, hiInclusive). If fromStart
         * is true, then the low (absolute) bound is the start of the backing map, and the other values are ignored.
         * Otherwise, if loInclusive is true, lo is the inclusive bound, else lo is the exclusive bound. Similarly for
         * the upper bound.
         */
        final K lo;

        /**
         * Endpoints are represented as triples (fromStart, lo, loInclusive) and (toEnd, hi, hiInclusive). If fromStart
         * is true, then the low (absolute) bound is the start of the backing map, and the other values are ignored.
         * Otherwise, if loInclusive is true, lo is the inclusive bound, else lo is the exclusive bound. Similarly for
         * the upper bound.
         */
        final K hi;
        final boolean fromStart;

        final boolean toEnd;
        final boolean loInclusive;

        final boolean hiInclusive;

        NavigableSubMap(IndexedTreeMap<K, V> m, boolean fromStart, K lo, boolean loInclusive, boolean toEnd, K hi,
                boolean hiInclusive) {
            if (!fromStart && !toEnd) {
                if (m.compare(lo, hi) > 0) {
                    throw new IllegalArgumentException("fromKey > toKey");
                }
            } else {
                if (!fromStart) {
                    m.compare(lo, lo);
                }
                if (!toEnd) {
                    m.compare(hi, hi);
                }
            }

            this.m = m;
            this.fromStart = fromStart;
            this.lo = lo;
            this.loInclusive = loInclusive;
            this.toEnd = toEnd;
            this.hi = hi;
            this.hiInclusive = hiInclusive;
        }

        // internal utilities

        final boolean tooLow(Object key) {
            if (!fromStart) {
                final int c = m.compare(key, lo);
                if (c < 0 || c == 0 && !loInclusive) {
                    return true;
                }
            }
            return false;
        }

        final boolean tooHigh(Object key) {
            if (!toEnd) {
                final int c = m.compare(key, hi);
                if (c > 0 || c == 0 && !hiInclusive) {
                    return true;
                }
            }
            return false;
        }

        final boolean inRange(Object key) {
            return !tooLow(key) && !tooHigh(key);
        }

        final boolean inClosedRange(Object key) {
            return (fromStart || m.compare(key, lo) >= 0) && (toEnd || m.compare(hi, key) >= 0);
        }

        final boolean inRange(Object key, boolean inclusive) {
            return inclusive ? inRange(key) : inClosedRange(key);
        }

        /*
         * Absolute versions of relation operations.
         * Subclasses map to these using like-named "sub"
         * versions that invert senses for descending maps
         */

        final IndexedTreeMap.Entry<K, V> absLowest() {
            final IndexedTreeMap.Entry<K, V> e = fromStart ? m.getFirstEntry()
                    : loInclusive ? m.getCeilingEntry(lo) : m.getHigherEntry(lo);
            return e == null || tooHigh(e.key) ? null : e;
        }

        final IndexedTreeMap.Entry<K, V> absHighest() {
            final IndexedTreeMap.Entry<K, V> e = toEnd ? m.getLastEntry()
                    : hiInclusive ? m.getFloorEntry(hi) : m.getLowerEntry(hi);
            return e == null || tooLow(e.key) ? null : e;
        }

        final IndexedTreeMap.Entry<K, V> absCeiling(K key) {
            if (tooLow(key)) {
                return absLowest();
            }
            final IndexedTreeMap.Entry<K, V> e = m.getCeilingEntry(key);
            return e == null || tooHigh(e.key) ? null : e;
        }

        final IndexedTreeMap.Entry<K, V> absHigher(K key) {
            if (tooLow(key)) {
                return absLowest();
            }
            final IndexedTreeMap.Entry<K, V> e = m.getHigherEntry(key);
            return e == null || tooHigh(e.key) ? null : e;
        }

        final IndexedTreeMap.Entry<K, V> absFloor(K key) {
            if (tooHigh(key)) {
                return absHighest();
            }
            final IndexedTreeMap.Entry<K, V> e = m.getFloorEntry(key);
            return e == null || tooLow(e.key) ? null : e;
        }

        final IndexedTreeMap.Entry<K, V> absLower(K key) {
            if (tooHigh(key)) {
                return absHighest();
            }
            final IndexedTreeMap.Entry<K, V> e = m.getLowerEntry(key);
            return e == null || tooLow(e.key) ? null : e;
        }

        /**
         * Returns the absolute high fence for ascending traversal
         */
        final IndexedTreeMap.Entry<K, V> absHighFence() {
            return toEnd ? null : hiInclusive ? m.getHigherEntry(hi) : m.getCeilingEntry(hi);
        }

        /**
         * Return the absolute low fence for descending traversal
         */
        final IndexedTreeMap.Entry<K, V> absLowFence() {
            return fromStart ? null : loInclusive ? m.getLowerEntry(lo) : m.getFloorEntry(lo);
        }

        // Abstract methods defined in ascending vs descending classes
        // These relay to the appropriate absolute versions

        abstract IndexedTreeMap.Entry<K, V> subLowest();

        abstract IndexedTreeMap.Entry<K, V> subHighest();

        abstract IndexedTreeMap.Entry<K, V> subCeiling(K key);

        abstract IndexedTreeMap.Entry<K, V> subHigher(K key);

        abstract IndexedTreeMap.Entry<K, V> subFloor(K key);

        abstract IndexedTreeMap.Entry<K, V> subLower(K key);

        /**
         * Returns ascending iterator from the perspective of this submap
         */
        abstract Iterator<K> keyIterator();

        /**
         * Returns descending iterator from the perspective of this submap
         */
        abstract Iterator<K> descendingKeyIterator();

        // public methods

        @Override
        public boolean isEmpty() {
            return fromStart && toEnd ? m.isEmpty() : entrySet().isEmpty();
        }

        @Override
        public int size() {
            return fromStart && toEnd ? m.size() : entrySet().size();
        }

        @Override
        public final boolean containsKey(Object key) {
            return inRange(key) && m.containsKey(key);
        }

        @Override
        public final V put(K key, V value) {
            if (!inRange(key)) {
                throw new IllegalArgumentException("key out of range");
            }
            return m.put(key, value);
        }

        @Override
        public final V get(Object key) {
            return !inRange(key) ? null : m.get(key);
        }

        @Override
        public final V remove(Object key) {
            return !inRange(key) ? null : m.remove(key);
        }

        @Override
        public final Map.Entry<K, V> ceilingEntry(K key) {
            return IndexedTreeMap.exportEntry(subCeiling(key));
        }

        @Override
        public final K ceilingKey(K key) {
            return IndexedTreeMap.keyOrNull(subCeiling(key));
        }

        @Override
        public final Map.Entry<K, V> higherEntry(K key) {
            return IndexedTreeMap.exportEntry(subHigher(key));
        }

        @Override
        public final K higherKey(K key) {
            return IndexedTreeMap.keyOrNull(subHigher(key));
        }

        @Override
        public final Map.Entry<K, V> floorEntry(K key) {
            return IndexedTreeMap.exportEntry(subFloor(key));
        }

        @Override
        public final K floorKey(K key) {
            return IndexedTreeMap.keyOrNull(subFloor(key));
        }

        @Override
        public final Map.Entry<K, V> lowerEntry(K key) {
            return IndexedTreeMap.exportEntry(subLower(key));
        }

        @Override
        public final K lowerKey(K key) {
            return IndexedTreeMap.keyOrNull(subLower(key));
        }

        @Override
        public final K firstKey() {
            return IndexedTreeMap.key(subLowest());
        }

        @Override
        public final K lastKey() {
            return IndexedTreeMap.key(subHighest());
        }

        @Override
        public final Map.Entry<K, V> firstEntry() {
            return IndexedTreeMap.exportEntry(subLowest());
        }

        @Override
        public final Map.Entry<K, V> lastEntry() {
            return IndexedTreeMap.exportEntry(subHighest());
        }

        @Override
        public final Map.Entry<K, V> pollFirstEntry() {
            final IndexedTreeMap.Entry<K, V> e = subLowest();
            final Map.Entry<K, V> result = IndexedTreeMap.exportEntry(e);
            if (e != null) {
                m.deleteEntry(e);
            }
            return result;
        }

        @Override
        public final Map.Entry<K, V> pollLastEntry() {
            final IndexedTreeMap.Entry<K, V> e = subHighest();
            final Map.Entry<K, V> result = IndexedTreeMap.exportEntry(e);
            if (e != null) {
                m.deleteEntry(e);
            }
            return result;
        }

        // Views
        transient NavigableMap<K, V> descendingMapView = null;
        transient EntrySetView entrySetView = null;
        transient KeySet<K> navigableKeySetView = null;

        @SuppressWarnings("rawtypes")
        @Override
        public final NavigableSet<K> navigableKeySet() {
            final KeySet<K> nksv = navigableKeySetView;
            return nksv != null ? nksv : (navigableKeySetView = new IndexedTreeMap.KeySet(this));
        }

        @Override
        public final Set<K> keySet() {
            return navigableKeySet();
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        @Override
        public final SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @Override
        public final SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public final SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        // View classes

        abstract class EntrySetView extends AbstractSet<Map.Entry<K, V>> {

            private transient int size = -1;
            private transient int sizeModCount;

            @Override
            public int size() {
                if (fromStart && toEnd) {
                    return m.size();
                }
                if (size == -1 || sizeModCount != m.modCount) {
                    sizeModCount = m.modCount;
                    size = 0;
                    final Iterator<?> i = iterator();
                    while (i.hasNext()) {
                        size++;
                        i.next();
                    }
                }
                return size;
            }

            @Override
            public boolean isEmpty() {
                final IndexedTreeMap.Entry<K, V> n = absLowest();
                return n == null || tooHigh(n.key);
            }

            @Override
            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry)) {
                    return false;
                }
                final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
                final K key = entry.getKey();
                if (!inRange(key)) {
                    return false;
                }
                final IndexedTreeMap.Entry<K, V> node = m.getEntry(key);
                return node != null && IndexedTreeMap.valEquals(node.getValue(), entry.getValue());
            }

            @Override
            public boolean remove(Object o) {
                if (!(o instanceof Map.Entry)) {
                    return false;
                }
                final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
                final K key = entry.getKey();
                if (!inRange(key)) {
                    return false;
                }
                final IndexedTreeMap.Entry<K, V> node = m.getEntry(key);
                if (node != null && IndexedTreeMap.valEquals(node.getValue(), entry.getValue())) {
                    m.deleteEntry(node);
                    return true;
                }
                return false;
            }
        }

        /**
         * Iterators for SubMaps
         */
        abstract class SubMapIterator<T> implements Iterator<T> {

            IndexedTreeMap.Entry<K, V> lastReturned;
            IndexedTreeMap.Entry<K, V> next;
            final K fenceKey;
            int expectedModCount;

            SubMapIterator(IndexedTreeMap.Entry<K, V> first, IndexedTreeMap.Entry<K, V> fence) {
                expectedModCount = m.modCount;
                lastReturned = null;
                next = first;
                fenceKey = fence == null ? null : fence.key;
            }

            @Override
            public final boolean hasNext() {
                return next != null && next.key != fenceKey;
            }

            final IndexedTreeMap.Entry<K, V> nextEntry() {
                final IndexedTreeMap.Entry<K, V> e = next;
                if (e == null || e.key == fenceKey) {
                    throw new NoSuchElementException();
                }
                if (m.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                next = IndexedTreeMap.successor(e);
                lastReturned = e;
                return e;
            }

            final IndexedTreeMap.Entry<K, V> prevEntry() {
                final IndexedTreeMap.Entry<K, V> e = next;
                if (e == null || e.key == fenceKey) {
                    throw new NoSuchElementException();
                }
                if (m.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                next = IndexedTreeMap.predecessor(e);
                lastReturned = e;
                return e;
            }

            final void removeAscending() {
                if (lastReturned == null) {
                    throw new IllegalStateException();
                }
                if (m.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                // deleted entries are replaced by their successors
                if (lastReturned.left != null && lastReturned.right != null) {
                    next = lastReturned;
                }
                m.deleteEntry(lastReturned);
                lastReturned = null;
                expectedModCount = m.modCount;
            }

            final void removeDescending() {
                if (lastReturned == null) {
                    throw new IllegalStateException();
                }
                if (m.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                m.deleteEntry(lastReturned);
                lastReturned = null;
                expectedModCount = m.modCount;
            }

        }

        final class SubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {

            SubMapEntryIterator(IndexedTreeMap.Entry<K, V> first, IndexedTreeMap.Entry<K, V> fence) {
                super(first, fence);
            }

            @Override
            public Map.Entry<K, V> next() {
                return nextEntry();
            }

            @Override
            public void remove() {
                removeAscending();
            }
        }

        final class SubMapKeyIterator extends SubMapIterator<K> {

            SubMapKeyIterator(IndexedTreeMap.Entry<K, V> first, IndexedTreeMap.Entry<K, V> fence) {
                super(first, fence);
            }

            @Override
            public K next() {
                return nextEntry().key;
            }

            @Override
            public void remove() {
                removeAscending();
            }
        }

        final class DescendingSubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {

            DescendingSubMapEntryIterator(IndexedTreeMap.Entry<K, V> last, IndexedTreeMap.Entry<K, V> fence) {
                super(last, fence);
            }

            @Override
            public Map.Entry<K, V> next() {
                return prevEntry();
            }

            @Override
            public void remove() {
                removeDescending();
            }
        }

        final class DescendingSubMapKeyIterator extends SubMapIterator<K> {

            DescendingSubMapKeyIterator(IndexedTreeMap.Entry<K, V> last, IndexedTreeMap.Entry<K, V> fence) {
                super(last, fence);
            }

            @Override
            public K next() {
                return prevEntry().key;
            }

            @Override
            public void remove() {
                removeDescending();
            }
        }
    }

    /**
     * @serial include
     */
    static final class AscendingSubMap<K, V> extends NavigableSubMap<K, V> {

        private static final long serialVersionUID = 912986545866124060L;

        AscendingSubMap(IndexedTreeMap<K, V> m, boolean fromStart, K lo, boolean loInclusive, boolean toEnd, K hi,
                boolean hiInclusive) {
            super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }

        @Override
        public Comparator<? super K> comparator() {
            return m.comparator();
        }

        @Override
        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            if (!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new AscendingSubMap<>(m, false, fromKey, fromInclusive, false, toKey, toInclusive);
        }

        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            if (!inRange(toKey, inclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new AscendingSubMap<>(m, fromStart, lo, loInclusive, false, toKey, inclusive);
        }

        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            if (!inRange(fromKey, inclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            return new AscendingSubMap<>(m, false, fromKey, inclusive, toEnd, hi, hiInclusive);
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            final NavigableMap<K, V> mv = descendingMapView;
            return mv != null ? mv
                    : (descendingMapView = new DescendingSubMap<>(m, fromStart, lo, loInclusive, toEnd, hi,
                            hiInclusive));
        }

        @Override
        Iterator<K> keyIterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }

        @Override
        Iterator<K> descendingKeyIterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        final class AscendingEntrySetView extends EntrySetView {

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new SubMapEntryIterator(absLowest(), absHighFence());
            }
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            final EntrySetView es = entrySetView;
            return es != null ? es : new AscendingEntrySetView();
        }

        @Override
        IndexedTreeMap.Entry<K, V> subLowest() {
            return absLowest();
        }

        @Override
        IndexedTreeMap.Entry<K, V> subHighest() {
            return absHighest();
        }

        @Override
        IndexedTreeMap.Entry<K, V> subCeiling(K key) {
            return absCeiling(key);
        }

        @Override
        IndexedTreeMap.Entry<K, V> subHigher(K key) {
            return absHigher(key);
        }

        @Override
        IndexedTreeMap.Entry<K, V> subFloor(K key) {
            return absFloor(key);
        }

        @Override
        IndexedTreeMap.Entry<K, V> subLower(K key) {
            return absLower(key);
        }
    }

    /**
     * @serial include
     */
    static final class DescendingSubMap<K, V> extends NavigableSubMap<K, V> {

        private static final long serialVersionUID = 912986545866120460L;

        DescendingSubMap(IndexedTreeMap<K, V> m, boolean fromStart, K lo, boolean loInclusive, boolean toEnd, K hi,
                boolean hiInclusive) {
            super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }

        private final Comparator<? super K> reverseComparator = Collections.reverseOrder(m.comparator);

        @Override
        public Comparator<? super K> comparator() {
            return reverseComparator;
        }

        @Override
        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            if (!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new DescendingSubMap<>(m, false, toKey, toInclusive, false, fromKey, fromInclusive);
        }

        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            if (!inRange(toKey, inclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new DescendingSubMap<>(m, false, toKey, inclusive, toEnd, hi, hiInclusive);
        }

        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            if (!inRange(fromKey, inclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            return new DescendingSubMap<>(m, fromStart, lo, loInclusive, false, fromKey, inclusive);
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            final NavigableMap<K, V> mv = descendingMapView;
            return mv != null ? mv
                    : (descendingMapView = new AscendingSubMap<>(m, fromStart, lo, loInclusive, toEnd, hi,
                            hiInclusive));
        }

        @Override
        Iterator<K> keyIterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        @Override
        Iterator<K> descendingKeyIterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }

        final class DescendingEntrySetView extends EntrySetView {

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new DescendingSubMapEntryIterator(absHighest(), absLowFence());
            }
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            final EntrySetView es = entrySetView;
            return es != null ? es : new DescendingEntrySetView();
        }

        @Override
        IndexedTreeMap.Entry<K, V> subLowest() {
            return absHighest();
        }

        @Override
        IndexedTreeMap.Entry<K, V> subHighest() {
            return absLowest();
        }

        @Override
        IndexedTreeMap.Entry<K, V> subCeiling(K key) {
            return absFloor(key);
        }

        @Override
        IndexedTreeMap.Entry<K, V> subHigher(K key) {
            return absLower(key);
        }

        @Override
        IndexedTreeMap.Entry<K, V> subFloor(K key) {
            return absCeiling(key);
        }

        @Override
        IndexedTreeMap.Entry<K, V> subLower(K key) {
            return absHigher(key);
        }
    }

    // Red-black mechanics

    private static final boolean RED = false;
    private static final boolean BLACK = true;

    /**
     * Node in the Tree. Doubles as a means to pass key-value pairs back to user (see Map.Entry).
     */
    static final class Entry<K, V> implements Map.Entry<K, V> {

        K key;
        V value;
        Entry<K, V> left = null;
        Entry<K, V> right = null;
        Entry<K, V> parent;
        boolean color = IndexedTreeMap.BLACK;
        int weight = 0;

        int sumup() {
            final int left = this.left == null ? 0 : this.left.sumup();
            final int right = this.right == null ? 0 : this.right.sumup();
            return left + right + 1;
        }

        void updateWeight(int delta) {
            weight += delta;
            Entry<K, V> p = parent;
            while (p != null) {
                p.weight += delta;
                p = p.parent;
            }
        }

        /**
         * Make a new cell with given key, value, and parent, and with <tt>null</tt> child links, and BLACK color.
         */
        Entry(K key, V value, Entry<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }

        /**
         * Returns the key.
         *
         * @return the key
         */
        @Override
        public K getKey() {
            return key;
        }

        /**
         * Returns the value associated with the key.
         *
         * @return the value associated with the key
         */
        @Override
        public V getValue() {
            return value;
        }

        /**
         * Replaces the value currently associated with the key with the given value.
         *
         * @return the value associated with the key before this method was called
         */
        @Override
        public V setValue(V value) {
            final V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

            return IndexedTreeMap.valEquals(key, e.getKey()) && IndexedTreeMap.valEquals(value, e.getValue());
        }

        @Override
        public int hashCode() {
            final int keyHash = key == null ? 0 : key.hashCode();
            final int valueHash = value == null ? 0 : value.hashCode();
            return keyHash ^ valueHash;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    /**
     * Returns the first Entry in the IndexedTreeMap (according to the IndexedTreeMap's key-sort function). Returns null
     * if the IndexedTreeMap is empty.
     */
    final Entry<K, V> getFirstEntry() {
        Entry<K, V> p = root;
        if (p != null) {
            while (p.left != null) {
                p = p.left;
            }
        }
        return p;
    }

    /**
     * Returns the last Entry in the IndexedTreeMap (according to the IndexedTreeMap's key-sort function). Returns null
     * if the IndexedTreeMap is empty.
     */
    final Entry<K, V> getLastEntry() {
        Entry<K, V> p = root;
        if (p != null) {
            while (p.right != null) {
                p = p.right;
            }
        }
        return p;
    }

    /**
     * Returns the successor of the specified Entry, or null if no such.
     */
    static <K, V> IndexedTreeMap.Entry<K, V> successor(Entry<K, V> t) {
        if (t == null) {
            return null;
        } else if (t.right != null) {
            Entry<K, V> p = t.right;
            while (p.left != null) {
                p = p.left;
            }
            return p;
        } else {
            Entry<K, V> p = t.parent;
            Entry<K, V> ch = t;
            while (p != null && ch == p.right) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * Returns the predecessor of the specified Entry, or null if no such.
     */
    static <K, V> Entry<K, V> predecessor(Entry<K, V> t) {
        if (t == null) {
            return null;
        } else if (t.left != null) {
            Entry<K, V> p = t.left;
            while (p.right != null) {
                p = p.right;
            }
            return p;
        } else {
            Entry<K, V> p = t.parent;
            Entry<K, V> ch = t;
            while (p != null && ch == p.left) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * Balancing operations.
     * <p/>
     * Implementations of rebalancings during insertion and deletion are slightly different than the CLR version. Rather
     * than using dummy nilnodes, we use a set of accessors that deal properly with null. They are used to avoid
     * messiness surrounding nullness checks in the main algorithms.
     */

    private static <K, V> boolean colorOf(Entry<K, V> p) {
        return p == null ? IndexedTreeMap.BLACK : p.color;
    }

    private static <K, V> Entry<K, V> parentOf(Entry<K, V> p) {
        return p == null ? null : p.parent;
    }

    private static <K, V> void setColor(Entry<K, V> p, boolean c) {
        if (p != null) {
            p.color = c;
        }
    }

    private static <K, V> Entry<K, V> leftOf(Entry<K, V> p) {
        return p == null ? null : p.left;
    }

    private static <K, V> Entry<K, V> rightOf(Entry<K, V> p) {
        return p == null ? null : p.right;
    }

    private static <K, V> int getWeight(Entry<K, V> p) {
        return p == null ? 0 : p.weight;
    }

    /**
     * From CLR
     */
    private void rotateLeft(Entry<K, V> p) {
        if (p != null) {
            final Entry<K, V> r = p.right;

            int delta = IndexedTreeMap.getWeight(r.left) - IndexedTreeMap.getWeight(p.right);
            p.right = r.left;
            p.updateWeight(delta);

            if (r.left != null) {
                r.left.parent = p;
            }

            r.parent = p.parent;

            if (p.parent == null) {
                root = r;
            } else if (p.parent.left == p) {
                delta = IndexedTreeMap.getWeight(r) - IndexedTreeMap.getWeight(p.parent.left);
                p.parent.left = r;
                p.parent.updateWeight(delta);
            } else {
                delta = IndexedTreeMap.getWeight(r) - IndexedTreeMap.getWeight(p.parent.right);
                p.parent.right = r;
                p.parent.updateWeight(delta);
            }

            delta = IndexedTreeMap.getWeight(p) - IndexedTreeMap.getWeight(r.left);
            r.left = p;
            r.updateWeight(delta);

            p.parent = r;
        }
    }

    /**
     * From CLR
     */
    private void rotateRight(Entry<K, V> p) {
        if (p != null) {
            final Entry<K, V> l = p.left;

            int delta = IndexedTreeMap.getWeight(l.right) - IndexedTreeMap.getWeight(p.left);
            p.left = l.right;
            p.updateWeight(delta);

            if (l.right != null) {
                l.right.parent = p;
            }

            l.parent = p.parent;

            if (p.parent == null) {
                root = l;
            } else if (p.parent.right == p) {
                delta = IndexedTreeMap.getWeight(l) - IndexedTreeMap.getWeight(p.parent.right);
                p.parent.right = l;
                p.parent.updateWeight(delta);
            } else {
                delta = IndexedTreeMap.getWeight(l) - IndexedTreeMap.getWeight(p.parent.left);
                p.parent.left = l;
                p.parent.updateWeight(delta);
            }

            delta = IndexedTreeMap.getWeight(p) - IndexedTreeMap.getWeight(l.right);
            l.right = p;
            l.updateWeight(delta);

            p.parent = l;
        }
    }

    /**
     * From CLR
     */
    private void fixAfterInsertion(Entry<K, V> x) {
        x.color = IndexedTreeMap.RED;

        while (x != null && x != root && x.parent.color == IndexedTreeMap.RED) {
            if (IndexedTreeMap.parentOf(x) == IndexedTreeMap
                    .leftOf(IndexedTreeMap.parentOf(IndexedTreeMap.parentOf(x)))) {
                final Entry<K, V> y = IndexedTreeMap.rightOf(IndexedTreeMap.parentOf(IndexedTreeMap.parentOf(x)));
                if (IndexedTreeMap.colorOf(y) == IndexedTreeMap.RED) {
                    IndexedTreeMap.setColor(IndexedTreeMap.parentOf(x), IndexedTreeMap.BLACK);
                    IndexedTreeMap.setColor(y, IndexedTreeMap.BLACK);
                    IndexedTreeMap.setColor(IndexedTreeMap.parentOf(IndexedTreeMap.parentOf(x)), IndexedTreeMap.RED);
                    x = IndexedTreeMap.parentOf(IndexedTreeMap.parentOf(x));
                } else {
                    if (x == IndexedTreeMap.rightOf(IndexedTreeMap.parentOf(x))) {
                        x = IndexedTreeMap.parentOf(x);
                        rotateLeft(x);
                    }
                    IndexedTreeMap.setColor(IndexedTreeMap.parentOf(x), IndexedTreeMap.BLACK);
                    IndexedTreeMap.setColor(IndexedTreeMap.parentOf(IndexedTreeMap.parentOf(x)), IndexedTreeMap.RED);
                    rotateRight(IndexedTreeMap.parentOf(IndexedTreeMap.parentOf(x)));
                }
            } else {
                final Entry<K, V> y = IndexedTreeMap.leftOf(IndexedTreeMap.parentOf(IndexedTreeMap.parentOf(x)));
                if (IndexedTreeMap.colorOf(y) == IndexedTreeMap.RED) {
                    IndexedTreeMap.setColor(IndexedTreeMap.parentOf(x), IndexedTreeMap.BLACK);
                    IndexedTreeMap.setColor(y, IndexedTreeMap.BLACK);
                    IndexedTreeMap.setColor(IndexedTreeMap.parentOf(IndexedTreeMap.parentOf(x)), IndexedTreeMap.RED);
                    x = IndexedTreeMap.parentOf(IndexedTreeMap.parentOf(x));
                } else {
                    if (x == IndexedTreeMap.leftOf(IndexedTreeMap.parentOf(x))) {
                        x = IndexedTreeMap.parentOf(x);
                        rotateRight(x);
                    }
                    IndexedTreeMap.setColor(IndexedTreeMap.parentOf(x), IndexedTreeMap.BLACK);
                    IndexedTreeMap.setColor(IndexedTreeMap.parentOf(IndexedTreeMap.parentOf(x)), IndexedTreeMap.RED);
                    rotateLeft(IndexedTreeMap.parentOf(IndexedTreeMap.parentOf(x)));
                }
            }
        }
        root.color = IndexedTreeMap.BLACK;
    }

    /**
     * Delete node p, and then rebalance the tree.
     */
    private void deleteEntry(Entry<K, V> p) {
        modCount++;
        size--;

        // If strictly internal, copy successor's element to p and then make p
        // point to successor.
        if (p.left != null && p.right != null) {
            final Entry<K, V> s = IndexedTreeMap.successor(p);
            p.key = s.key;
            p.value = s.value;
            p = s;
        } // p has 2 children

        // Start fixup at replacement node, if it exists.
        final Entry<K, V> replacement = p.left != null ? p.left : p.right;

        if (replacement != null) {
            // Link replacement to parent
            replacement.parent = p.parent;
            if (p.parent == null) {
                root = replacement;
            } else if (p == p.parent.left) {
                final int delta = IndexedTreeMap.getWeight(replacement) - IndexedTreeMap.getWeight(p.parent.left);
                p.parent.left = replacement;
                p.parent.updateWeight(delta);
            } else {
                final int delta = IndexedTreeMap.getWeight(replacement) - IndexedTreeMap.getWeight(p.parent.right);
                p.parent.right = replacement;
                p.parent.updateWeight(delta);
            }

            // Null out links so they are OK to use by fixAfterDeletion.
            p.left = p.right = p.parent = null;

            // Fix replacement
            if (p.color == IndexedTreeMap.BLACK) {
                fixAfterDeletion(replacement);
            }
        } else if (p.parent == null) { // return if we are the only node.
            root = null;
        } else { //  No children. Use self as phantom replacement and unlink.
            if (p.color == IndexedTreeMap.BLACK) {
                fixAfterDeletion(p);
            }

            if (p.parent != null) {
                if (p == p.parent.left) {
                    //                    int delta = -getWeight(p.parent.left);
                    p.parent.left = null;
                    //                    p.parent.updateWeight(delta);
                } else if (p == p.parent.right) {
                    //                    int delta = -getWeight(p.parent.right);
                    p.parent.right = null;
                    //                    p.parent.updateWeight(delta);
                }

                p.parent.updateWeight(-1);

                p.parent = null;
            }
        }
    }

    /**
     * From CLR
     */
    private void fixAfterDeletion(Entry<K, V> x) {
        while (x != root && IndexedTreeMap.colorOf(x) == IndexedTreeMap.BLACK) {
            if (x == IndexedTreeMap.leftOf(IndexedTreeMap.parentOf(x))) {
                Entry<K, V> sib = IndexedTreeMap.rightOf(IndexedTreeMap.parentOf(x));

                if (IndexedTreeMap.colorOf(sib) == IndexedTreeMap.RED) {
                    IndexedTreeMap.setColor(sib, IndexedTreeMap.BLACK);
                    IndexedTreeMap.setColor(IndexedTreeMap.parentOf(x), IndexedTreeMap.RED);
                    rotateLeft(IndexedTreeMap.parentOf(x));
                    sib = IndexedTreeMap.rightOf(IndexedTreeMap.parentOf(x));
                }

                if (IndexedTreeMap.colorOf(IndexedTreeMap.leftOf(sib)) == IndexedTreeMap.BLACK
                        && IndexedTreeMap.colorOf(IndexedTreeMap.rightOf(sib)) == IndexedTreeMap.BLACK) {
                    IndexedTreeMap.setColor(sib, IndexedTreeMap.RED);
                    x = IndexedTreeMap.parentOf(x);
                } else {
                    if (IndexedTreeMap.colorOf(IndexedTreeMap.rightOf(sib)) == IndexedTreeMap.BLACK) {
                        IndexedTreeMap.setColor(IndexedTreeMap.leftOf(sib), IndexedTreeMap.BLACK);
                        IndexedTreeMap.setColor(sib, IndexedTreeMap.RED);
                        rotateRight(sib);
                        sib = IndexedTreeMap.rightOf(IndexedTreeMap.parentOf(x));
                    }
                    IndexedTreeMap.setColor(sib, IndexedTreeMap.colorOf(IndexedTreeMap.parentOf(x)));
                    IndexedTreeMap.setColor(IndexedTreeMap.parentOf(x), IndexedTreeMap.BLACK);
                    IndexedTreeMap.setColor(IndexedTreeMap.rightOf(sib), IndexedTreeMap.BLACK);
                    rotateLeft(IndexedTreeMap.parentOf(x));
                    x = root;
                }
            } else { // symmetric
                Entry<K, V> sib = IndexedTreeMap.leftOf(IndexedTreeMap.parentOf(x));

                if (IndexedTreeMap.colorOf(sib) == IndexedTreeMap.RED) {
                    IndexedTreeMap.setColor(sib, IndexedTreeMap.BLACK);
                    IndexedTreeMap.setColor(IndexedTreeMap.parentOf(x), IndexedTreeMap.RED);
                    rotateRight(IndexedTreeMap.parentOf(x));
                    sib = IndexedTreeMap.leftOf(IndexedTreeMap.parentOf(x));
                }

                if (IndexedTreeMap.colorOf(IndexedTreeMap.rightOf(sib)) == IndexedTreeMap.BLACK
                        && IndexedTreeMap.colorOf(IndexedTreeMap.leftOf(sib)) == IndexedTreeMap.BLACK) {
                    IndexedTreeMap.setColor(sib, IndexedTreeMap.RED);
                    x = IndexedTreeMap.parentOf(x);
                } else {
                    if (IndexedTreeMap.colorOf(IndexedTreeMap.leftOf(sib)) == IndexedTreeMap.BLACK) {
                        IndexedTreeMap.setColor(IndexedTreeMap.rightOf(sib), IndexedTreeMap.BLACK);
                        IndexedTreeMap.setColor(sib, IndexedTreeMap.RED);
                        rotateLeft(sib);
                        sib = IndexedTreeMap.leftOf(IndexedTreeMap.parentOf(x));
                    }
                    IndexedTreeMap.setColor(sib, IndexedTreeMap.colorOf(IndexedTreeMap.parentOf(x)));
                    IndexedTreeMap.setColor(IndexedTreeMap.parentOf(x), IndexedTreeMap.BLACK);
                    IndexedTreeMap.setColor(IndexedTreeMap.leftOf(sib), IndexedTreeMap.BLACK);
                    rotateRight(IndexedTreeMap.parentOf(x));
                    x = root;
                }
            }
        }

        IndexedTreeMap.setColor(x, IndexedTreeMap.BLACK);
    }

    private static final long serialVersionUID = 919286545866124006L;

    /**
     * Save the state of the <tt>IndexedTreeMap</tt> instance to a stream (i.e., serialize it).
     *
     * @serialData The <i>size</i> of the IndexedTreeMap (the number of key-value mappings) is emitted (int), followed
     *             by the key (Object) and value (Object) for each key-value mapping represented by the IndexedTreeMap.
     *             The key-value mappings are emitted in key-order (as determined by the IndexedTreeMap's Comparator, or
     *             by the keys' natural ordering if the IndexedTreeMap has no Comparator).
     */
    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
        // Write out the Comparator and any hidden stuff
        s.defaultWriteObject();

        // Write out size (number of Mappings)
        s.writeInt(size);

        // Write out keys and values (alternating)
        for (final java.util.Map.Entry<K, V> e : entrySet()) {
            s.writeObject(e.getKey());
            s.writeObject(e.getValue());
        }
    }

    /**
     * Reconstitute the <tt>IndexedTreeMap</tt> instance from a stream (i.e., deserialize it).
     */
    private void readObject(final java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        // Read in the Comparator and any hidden stuff
        s.defaultReadObject();

        // Read in size
        final int size = s.readInt();

        buildFromSorted(size, null, s, null);
    }

    /**
     * Intended to be called only from IndexedTreeSet.readObject
     */
    void readTreeSet(int size, java.io.ObjectInputStream s, V defaultVal)
            throws java.io.IOException, ClassNotFoundException {
        buildFromSorted(size, null, s, defaultVal);
        if (root != null) {
            updateWeight(root);
        }
    }

    private int updateWeight(Entry<K, V> e) {
        int weight = 1;
        if (e.left != null) {
            weight += updateWeight(e.left);
        }
        if (e.right != null) {
            weight += updateWeight(e.right);
        }
        e.weight = weight;
        return weight;
    }

    /**
     * Intended to be called only from IndexedTreeSet.addAll
     */
    void addAllForTreeSet(SortedSet<? extends K> set, V defaultVal) {
        try {
            buildFromSorted(set.size(), set.iterator(), null, defaultVal);
        } catch (final java.io.IOException | ClassNotFoundException cannotHappen) {}
    }

    /**
     * Linear time tree building algorithm from sorted data. Can accept keys and/or values from iterator or stream. This
     * leads to too many parameters, but seems better than alternatives. The four formats that this method accepts are:
     * <p/>
     * 1) An iterator of Map.Entries. (it != null, defaultVal == null). 2) An iterator of keys. (it != null, defaultVal
     * != null). 3) A stream of alternating serialized keys and values. (it == null, defaultVal == null). 4) A stream of
     * serialized keys. (it == null, defaultVal != null).
     * <p/>
     * It is assumed that the comparator of the IndexedTreeMap is already set prior to calling this method.
     *
     * @param size the number of keys (or key-value pairs) to be read from the iterator or stream
     * @param it If non-null, new entries are created from entries or keys read from this iterator.
     * @param str If non-null, new entries are created from keys and possibly values read from this stream in serialized
     *            form. Exactly one of it and str should be non-null.
     * @param defaultVal if non-null, this default value is used for each value in the map. If null, each value is read
     *            from iterator or stream, as described above.
     * @throws java.io.IOException propagated from stream reads. This cannot occur if str is null.
     * @throws ClassNotFoundException propagated from readObject. This cannot occur if str is null.
     */
    private void buildFromSorted(int size, Iterator<?> it, java.io.ObjectInputStream str, V defaultVal)
            throws java.io.IOException, ClassNotFoundException {
        this.size = size;
        root = buildFromSorted(0, 0, size - 1, IndexedTreeMap.computeRedLevel(size), it, str, defaultVal);
    }

    /**
     * Recursive "helper method" that does the real work of the previous method. Identically named parameters have
     * identical definitions. Additional parameters are documented below. It is assumed that the comparator and size
     * fields of the IndexedTreeMap are already set prior to calling this method. (It ignores both fields.)
     *
     * @param level the current level of tree. Initial call should be 0.
     * @param lo the first element index of this subtree. Initial should be 0.
     * @param hi the last element index of this subtree. Initial should be size-1.
     * @param redLevel the level at which nodes should be red. Must be equal to computeRedLevel for tree of this size.
     */
    private final Entry<K, V> buildFromSorted(int level, int lo, int hi, int redLevel, Iterator<?> it,
            java.io.ObjectInputStream str, V defaultVal) throws java.io.IOException, ClassNotFoundException {
        /*
         * Strategy: The root is the middlemost element. To get to it, we
         * have to first recursively construct the entire left subtree,
         * so as to grab all of its elements. We can then proceed with right
         * subtree.
         *
         * The lo and hi arguments are the minimum and maximum
         * indices to pull out of the iterator or stream for current subtree.
         * They are not actually indexed, we just proceed sequentially,
         * ensuring that items are extracted in corresponding order.
         */

        if (hi < lo) {
            return null;
        }

        final int mid = (lo + hi) / 2;

        Entry<K, V> left = null;
        if (lo < mid) {
            left = buildFromSorted(level + 1, lo, mid - 1, redLevel, it, str, defaultVal);
        }

        // extract key and/or value from iterator or stream
        K key;
        V value;
        if (it != null) {
            if (defaultVal == null) {
                final Map.Entry<K, V> entry = (Map.Entry<K, V>) it.next();
                key = entry.getKey();
                value = entry.getValue();
            } else {
                key = (K) it.next();
                value = defaultVal;
            }
        } else { // use stream
            key = (K) str.readObject();
            value = defaultVal != null ? defaultVal : (V) str.readObject();
        }

        final Entry<K, V> middle = new Entry<>(key, value, null);

        // color nodes in non-full bottommost level red
        if (level == redLevel) {
            middle.color = IndexedTreeMap.RED;
        }

        if (left != null) {
            middle.left = left;
            left.parent = middle;
        }

        if (mid < hi) {
            final Entry<K, V> right = buildFromSorted(level + 1, mid + 1, hi, redLevel, it, str, defaultVal);
            middle.right = right;
            right.parent = middle;
        }

        return middle;
    }

    /**
     * Find the level down to which to assign all nodes BLACK. This is the last `full' level of the complete binary tree
     * produced by buildTree. The remaining nodes are colored RED. (This makes a `nice' set of color assignments wrt
     * future insertions.) This level number is computed by finding the number of splits needed to reach the zeroeth
     * node. (The answer is ~lg(N), but in any case must be computed by same quick O(lg(N)) loop.)
     */
    private static int computeRedLevel(int sz) {
        int level = 0;
        for (int m = sz - 1; m >= 0; m = m / 2 - 1) {
            level++;
        }
        return level;
    }
}
