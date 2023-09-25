/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package io.fair_acc.dataset.utils;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Hash table based implementation of the {@code Map} interface, with
 * <em>weak (SoftReference) keys</em>.
 * An entry in a {@code SoftHashMap} will automatically be removed when
 * its key is no longer in ordinary use.  More precisely, the presence of a
 * mapping for a given key will not prevent the key from being discarded by the
 * garbage collector, that is, made finalizable, finalized, and then reclaimed.
 * When a key has been discarded its entry is effectively removed from the map,
 * so this class behaves somewhat differently from other {@code Map}
 * implementations.
 *
 * <p> Both null values and the null key are supported. This class has
 * performance characteristics similar to those of the {@code HashMap}
 * class, and has the same efficiency parameters of <em>initial capacity</em>
 * and <em>load factor</em>.
 *
 * <p> Like most collection classes, this class is not synchronized.
 * A synchronized {@code SoftHashMap} may be constructed using the
 * {@link java.util.Collections#synchronizedMap Collections.synchronizedMap}
 * method.
 *
 * <p> This class is intended primarily for use with key objects whose
 * {@code equals} methods test for object identity using the
 * {@code ==} operator.  Once such a key is discarded it can never be
 * recreated, so it is impossible to do a lookup of that key in a
 * {@code SoftHashMap} at some later time and be surprised that its entry
 * has been removed.  This class will work perfectly well with key objects
 * whose {@code equals} methods are not based upon object identity, such
 * as {@code String} instances.  With such recreatable key objects,
 * however, the automatic removal of {@code SoftHashMap} entries whose
 * keys have been discarded may prove to be confusing.
 *
 * <p> The behavior of the {@code SoftHashMap} class depends in part upon
 * the actions of the garbage collector, so several familiar (though not
 * required) {@code Map} invariants do not hold for this class.  Because
 * the garbage collector may discard keys at any time, a
 * {@code SoftHashMap} may behave as though an unknown thread is silently
 * removing entries.  In particular, even if you synchronize on a
 * {@code SoftHashMap} instance and invoke none of its mutator methods, it
 * is possible for the {@code size} method to return smaller values over
 * time, for the {@code isEmpty} method to return {@code false} and
 * then {@code true}, for the {@code containsKey} method to return
 * {@code true} and later {@code false} for a given key, for the
 * {@code get} method to return a value for a given key but later return
 * {@code null}, for the {@code put} method to return
 * {@code null} and the {@code remove} method to return
 * {@code false} for a key that previously appeared to be in the map, and
 * for successive examinations of the key set, the value collection, and
 * the entry set to yield successively smaller numbers of elements.
 *
 * <p> Each key object in a {@code SoftHashMap} is stored indirectly as
 * the referent of a soft reference.  Therefore a key will automatically be
 * removed only after the soft references to it, both inside and outside of the
 * map, have been cleared by the garbage collector.
 *
 * <p> <strong>Implementation note:</strong> The value objects in a
 * {@code SoftHashMap} are held by ordinary strong references.  Thus care
 * should be taken to ensure that value objects do not strongly refer to their
 * own keys, either directly or indirectly, since that will prevent the keys
 * from being discarded.  Note that a value object may refer indirectly to its
 * key via the {@code SoftHashMap} itself; that is, a value object may
 * strongly refer to some other key object whose associated value object, in
 * turn, strongly refers to the key of the first value object.  If the values
 * in the map do not rely on the map holding strong references to them, one way
 * to deal with this is to wrap values themselves within
 * {@code SoftReferences} before
 * inserting, as in: {@code m.put(key, new SoftReference(value))},
 * and then unwrapping upon each {@code get}.
 *
 * <p>The iterators returned by the {@code iterator} method of the collections
 * returned by all of this class's "collection view methods" are
 * <i>fail-fast</i>: if the map is structurally modified at any time after the
 * iterator is created, in any way except through the iterator's own
 * {@code remove} method, the iterator will throw a {@link
 * ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:  <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 * @author Oracle,
 * @author rstein, adapted to use within chart-fx
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author      Doug Lea
 * @author      Josh Bloch
 * @author      Mark Reinhold
 * @since       1.2
 * @see         java.util.HashMap
 * @see         java.lang.ref.SoftReference
 */
@SuppressWarnings("PMD")
public class SoftKeyHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
    /**
     * The default initial capacity -- MUST be a power of two.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     */
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * Value representing null keys inside tables.
     */
    private static final Object NULL_KEY = new Object();

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    Entry<K, V>[] table;

    /**
     * The number of key-value mappings contained in this soft hash map.
     */
    private int size;

    /**
     * The next size value at which to resize (capacity * load factor).
     */
    private int threshold;

    /**
     * The load factor for the hash table.
     */
    private final float loadFactor;

    /**
     * Reference queue for cleared WeakEntries
     */
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    /**
     * The number of times this SoftHashMap has been structurally modified.
     * Structural modifications are those that change the number of
     * mappings in the map or otherwise modify its internal structure
     * (e.g., rehash).  This field is used to make iterators on
     * Collection-views of the map fail-fast.
     *
     * @see ConcurrentModificationException
     */
    int modCount;

    private transient Set<Map.Entry<K, V>> entrySet;

    /**
     * Constructs a new, empty {@code SoftHashMap} with the default initial
     * capacity (16) and load factor (0.75).
     */
    public SoftKeyHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a new, empty {@code SoftHashMap} with the given initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity The initial capacity of the {@code SoftHashMap}
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public SoftKeyHashMap(final int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a new, empty {@code SoftHashMap} with the given initial
     * capacity and the given load factor.
     *
     * @param  initialCapacity The initial capacity of the {@code SoftHashMap}
     * @param  loadFactor      The load factor of the {@code SoftHashMap}
     * @throws IllegalArgumentException if the initial capacity is negative,
     *         or if the load factor is nonpositive.
     */
    public SoftKeyHashMap(int initialCapacity, final float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Initial Capacity: " + initialCapacity);
        }
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }

        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal Load factor: " + loadFactor);
        }
        int capacity = 1;
        while (capacity < initialCapacity) {
            capacity <<= 1;
        }
        table = newTable(capacity);
        this.loadFactor = loadFactor;
        threshold = (int) (capacity * loadFactor);
    }

    // internal utilities

    /**
     * Constructs a new {@code SoftHashMap} with the same mappings as the
     * specified map.  The {@code SoftHashMap} is created with the default
     * load factor (0.75) and an initial capacity sufficient to hold the
     * mappings in the specified map.
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     * @since   1.3
     */
    public SoftKeyHashMap(final Map<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                     DEFAULT_INITIAL_CAPACITY),
                DEFAULT_LOAD_FACTOR);
        putAll(m);
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    @Override
    public void clear() {
        // clear out ref queue. We don't need to expunge entries
        // since table is getting cleared.
        while (queue.poll() != null) {
            // nothing to do
        }

        modCount++;
        Arrays.fill(table, null);
        size = 0;

        // Allocation of array may have caused GC, which may have caused
        // additional entries to go stale.  Removing these entries from the
        // reference queue will make them eligible for reclamation.
        while (queue.poll() != null) {
            // nothing to do
        }
    }

    /**
     * Returns {@code true} if this map contains a mapping for the
     * specified key.
     *
     * @param  key   The key whose presence in this map is to be tested
     * @return {@code true} if there is a mapping for {@code key};
     *         {@code false} otherwise
     */
    @Override
    public boolean containsKey(final Object key) {
        return getEntry(key) != null;
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the
     *         specified value
     */
    @Override
    public boolean containsValue(final Object value) {
        if (value == null) {
            return containsNullValue();
        }

        final Entry<K, V>[] tab = getTable();
        for (int i = tab.length; i-- > 0;) {
            for (Entry<K, V> e = tab[i]; e != null; e = e.next) {
                if (value.equals(e.value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own {@code remove} operation, or through the
     * {@code setValue} operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the {@code Iterator.remove},
     * {@code Set.remove}, {@code removeAll}, {@code retainAll} and
     * {@code clear} operations.  It does not support the
     * {@code add} or {@code addAll} operations.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        final Set<Map.Entry<K, V>> es = entrySet;
        return es != null ? es : (entrySet = new EntrySet());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void forEach(final BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        final int expectedModCount = modCount;

        final Entry<K, V>[] tab = getTable();
        for (Entry<K, V> entry : tab) {
            while (entry != null) {
                final Object key = entry.get();
                if (key != null) {
                    action.accept((K) SoftKeyHashMap.unmaskNull(key), entry.value);
                }
                entry = entry.next;

                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that
     * {@code Objects.equals(key, k)},
     * then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    @Override
    public V get(final Object key) {
        final Object k = maskNull(key);
        final int h = hash(k);
        final Entry<K, V>[] tab = getTable();
        final int index = indexFor(h, tab.length);
        Entry<K, V> e = tab[index];
        while (e != null) {
            if (e.hash == h && eq(k, e.get())) {
                return e.value;
            }
            e = e.next;
        }
        return null;
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key}.)
     */
    @Override
    public V put(final K key, final V value) {
        if (key == null || value == null)
            throw new NullPointerException();
        final Object k = maskNull(key);
        final int h = hash(k);
        final Entry<K, V>[] tab = getTable();
        final int i = indexFor(h, tab.length);

        for (Entry<K, V> e = tab[i]; e != null; e = e.next) {
            if (h == e.hash && eq(k, e.get())) {
                final V oldValue = e.value;
                if (value != oldValue) {
                    e.value = value;
                }
                return oldValue;
            }
        }

        modCount++;
        final Entry<K, V> e = tab[i];
        tab[i] = new Entry<>(k, value, queue, h, e);
        if (++size >= threshold) {
            resize(tab.length * 2);
        }
        return null;
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for any
     * of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map.
     * @throws  NullPointerException if the specified map is null.
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        if (m == null) {
            return;
        }
        final int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0) {
            return;
        }

        /*
         * Expand the map if the map if the number of mappings to be added
         * is greater than or equal to threshold.  This is conservative; the
         * obvious condition is (m.size() + size) >= threshold, but this
         * condition could result in a map with twice the appropriate capacity,
         * if the keys to be added overlap with the keys already in this map.
         * By using the conservative calculation, we subject ourself
         * to at most one extra resize.
         */
        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int) (numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY) {
                targetCapacity = MAXIMUM_CAPACITY;
            }
            int newCapacity = table.length;
            while (newCapacity < targetCapacity) {
                newCapacity <<= 1;
            }
            if (newCapacity > table.length) {
                resize(newCapacity);
            }
        }

        for (final Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Removes the mapping for a key from this soft hash map if it is present.
     * More formally, if this map contains a mapping from key {@code k} to
     * value {@code v} such that <code>(key==null ?  k==null :
     * key.equals(k))</code>, that mapping is removed.  (The map can contain
     * at most one such mapping.)
     *
     * <p>Returns the value to which this map previously associated the key,
     * or {@code null} if the map contained no mapping for the key.  A
     * return value of {@code null} does not <i>necessarily</i> indicate
     * that the map contained no mapping for the key; it's also possible
     * that the map explicitly mapped the key to {@code null}.
     *
     * <p>The map will not contain a mapping for the specified key once the
     * call returns.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     */
    @Override
    public V remove(final Object key) {
        final Object k = maskNull(key);
        final int h = hash(k);
        final Entry<K, V>[] tab = getTable();
        final int i = indexFor(h, tab.length);
        Entry<K, V> prev = tab[i];
        Entry<K, V> e = prev;

        while (e != null) {
            final Entry<K, V> next = e.next;
            if (h == e.hash && eq(k, e.get())) {
                modCount++;
                size--;
                if (prev == e) {
                    tab[i] = next;
                } else {
                    prev.next = next;
                }
                return e.value;
            }
            prev = e;
            e = next;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        final int expectedModCount = modCount;

        final Entry<K, V>[] tab = getTable();

        for (Entry<K, V> entry : tab) {
            while (entry != null) {
                final Object key = entry.get();
                if (key != null) {
                    entry.value = function.apply((K) SoftKeyHashMap.unmaskNull(key), entry.value);
                }
                entry = entry.next;

                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    /**
     * Returns the number of key-value mappings in this map.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    @Override
    public int size() {
        if (size == 0) {
            return 0;
        }
        expungeStaleEntries();
        return size;
    }

    /**
     * @return true for containsValue with (to be evicted) null argument
     */
    private boolean containsNullValue() {
        final Entry<K, V>[] tab = getTable();
        for (int i = tab.length; i-- > 0;) {
            for (Entry<K, V> e = tab[i]; e != null; e = e.next) {
                if (e.value == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Expunges stale entries from the table.
     */
    private void expungeStaleEntries() {
        for (Object x; (x = queue.poll()) != null;) {
            synchronized (queue) {
                @SuppressWarnings("unchecked")
                final Entry<K, V> e = (Entry<K, V>) x;
                final int i = indexFor(e.hash, table.length);

                Entry<K, V> prev = table[i];
                Entry<K, V> p = prev;
                while (p != null) {
                    final Entry<K, V> next = p.next;
                    if (p == e) {
                        if (prev == e) {
                            table[i] = next;
                        } else {
                            prev.next = next;
                        }
                        // Must not null out e.next;
                        // stale entries may be in use by a HashIterator
                        e.value = null; // Help GC
                        size--;
                        break;
                    }
                    prev = p;
                    p = next;
                }
            }
        }
    }

    /**
     * Returns the table after first expunging stale entries.
     * @return internal reference table
     */
    private Entry<K, V>[] getTable() {
        expungeStaleEntries();
        return table;
    }

    @SuppressWarnings("unchecked")
    private Entry<K, V>[] newTable(final int n) {
        return (Entry<K, V>[]) new Entry<?, ?>[n];
    }

    /**
     * Transfers all entries from src to dest tables
     * @param src source entry
     * @param dest destination entry
     */
    private void transfer(final Entry<K, V>[] src, final Entry<K, V>[] dest) {
        for (int j = 0; j < src.length; ++j) {
            Entry<K, V> e = src[j];
            src[j] = null;
            while (e != null) {
                final Entry<K, V> next = e.next;
                final Object key = e.get();
                if (key == null) {
                    e.next = null; // Help GC
                    e.value = null; //  "   "
                    size--;
                } else {
                    final int i = indexFor(e.hash, dest.length);
                    e.next = dest[i];
                    dest[i] = e;
                }
                e = next;
            }
        }
    }

    /**
     * Returns the entry associated with the specified key in this map.
     * Returns null if the map contains no mapping for this key.
     * @param key the key
     * @return entry for the given key
     */
    Entry<K, V> getEntry(final Object key) {
        final Object k = maskNull(key);
        final int h = hash(k);
        final Entry<K, V>[] tab = getTable();
        final int index = indexFor(h, tab.length);
        Entry<K, V> e = tab[index];
        while (e != null && (e.hash != h || !eq(k, e.get()))) {
            e = e.next;
        }
        return e;
    }

    /**
     * Retrieve object hash code and applies a supplemental hash function to the
     * result hash, which defends against poor quality hash functions.  This is
     * critical because HashMap uses power-of-two length hash tables, that
     * otherwise encounter collisions for hashCodes that do not differ
     * in lower bits.
     * @param k key
     * @return hash code for given key
     */
    final int hash(final Object k) {
        int h = k.hashCode();

        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        h ^= h >>> 20 ^ h >>> 12;
        return h ^ h >>> 7 ^ h >>> 4;
    }

    /* Special version of remove needed by Entry set */
    boolean removeMapping(final Object o) {
        if (!(o instanceof Map.Entry)) {
            return false;
        }
        final Entry<K, V>[] tab = getTable();
        final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        final Object k = maskNull(entry.getKey());
        final int h = hash(k);
        final int i = indexFor(h, tab.length);
        Entry<K, V> prev = tab[i];
        Entry<K, V> e = prev;

        while (e != null) {
            final Entry<K, V> next = e.next;
            if (h == e.hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e) {
                    tab[i] = next;
                } else {
                    prev.next = next;
                }
                return true;
            }
            prev = e;
            e = next;
        }

        return false;
    }

    /**
     * Rehashes the contents of this map into a new array with a
     * larger capacity.  This method is called automatically when the
     * number of keys in this map reaches its threshold.
     *
     * If current capacity is MAXIMUM_CAPACITY, this method does not
     * resize the map, but sets threshold to Integer.MAX_VALUE.
     * This has the effect of preventing future calls.
     *
     * @param newCapacity the new capacity, MUST be a power of two;
     *        must be greater than current capacity unless current
     *        capacity is MAXIMUM_CAPACITY (in which case value
     *        is irrelevant).
     */
    void resize(final int newCapacity) {
        final Entry<K, V>[] oldTable = getTable();
        final int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        final Entry<K, V>[] newTable = newTable(newCapacity);
        transfer(oldTable, newTable);
        table = newTable;

        /*
         * If ignoring null elements and processing ref queue caused massive
         * shrinkage, then restore old table.  This should be rare, but avoids
         * unbounded expansion of garbage-filled tables.
         */
        if (size >= threshold / 2) {
            threshold = (int) (newCapacity * loadFactor);
        } else {
            expungeStaleEntries();
            transfer(newTable, oldTable);
            table = oldTable;
        }
    }

    /*
     * Checks for equality of non-null reference x and possibly-null y.  By
     * default uses Object.equals.
     */
    private static boolean eq(final Object x, final Object y) {
        return x == y || x.equals(y);
    }

    /*
     * Returns index for hash code h.
     */
    private static int indexFor(final int h, final int length) {
        return h & length - 1;
    }

    /*
     * Use NULL_KEY for key if it is null.
     */
    private static Object maskNull(final Object key) {
        return key == null ? NULL_KEY : key;
    }

    /*
     * Returns internal representation of null key back to caller as null.
     */
    static Object unmaskNull(final Object key) {
        return key == NULL_KEY ? null : key;
    }

    /*
     * The entries in this hash table extend SoftReference, using its main ref
     * field as the key.
     */
    private static class Entry<K, V> extends SoftReference<Object> implements Map.Entry<K, V> {
        V value;
        final int hash;
        Entry<K, V> next;

        /*
         * Creates new entry.
         */
        Entry(final Object key, final V value, final ReferenceQueue<Object> queue, final int hash, final Entry<K, V> next) {
            super(key, queue);
            this.value = value;
            this.hash = hash;
            this.next = next;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            final K k1 = getKey();
            final Object k2 = e.getKey();
            if (Objects.equals(k1, k2)) {
                final V v1 = getValue();
                final Object v2 = e.getValue();
                return Objects.equals(v1, v2);
            }
            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        public K getKey() {
            return (K) SoftKeyHashMap.unmaskNull(get());
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            final K k = getKey();
            final V v = getValue();
            return Objects.hashCode(k) ^ Objects.hashCode(v);
        }

        @Override
        public V setValue(final V newValue) {
            final V oldValue = value;
            value = newValue;
            return oldValue;
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    // Views

    private class EntryIterator extends HashIterator<Map.Entry<K, V>> {
        @Override
        public Map.Entry<K, V> next() {
            return nextEntry();
        }
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        @Override
        public void clear() {
            SoftKeyHashMap.this.clear();
        }

        @Override
        public boolean contains(final Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            final Entry<K, V> candidate = getEntry(e.getKey());
            return candidate != null && candidate.equals(e);
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean remove(final Object o) {
            return removeMapping(o);
        }

        @Override
        public int size() {
            return SoftKeyHashMap.this.size();
        }

        @Override
        public Spliterator<Map.Entry<K, V>> spliterator() {
            return new EntrySpliterator<>(SoftKeyHashMap.this, 0, -1, 0, 0);
        }

        @Override
        public Object[] toArray() {
            return deepCopy().toArray();
        }

        @Override
        public <T> T[] toArray(final T[] a) {
            return deepCopy().toArray(a);
        }

        private List<Map.Entry<K, V>> deepCopy() {
            final List<Map.Entry<K, V>> list = new ArrayList<>(size());
            this.forEach((final Map.Entry<K, V> e) -> list.add(new AbstractMap.SimpleEntry<>(e)));
            return list;
        }
    }

    private abstract class HashIterator<T> implements Iterator<T> {
        private int index;
        private Entry<K, V> entry;
        private Entry<K, V> lastReturned;
        private int expectedModCount = modCount;

        /**
         * Strong reference needed to avoid disappearance of key
         * between hasNext and next
         */
        private Object nextKey;

        /**
         * Strong reference needed to avoid disappearance of key
         * between nextEntry() and any use of the entry
         */
        private Object currentKey;

        HashIterator() {
            index = isEmpty() ? 0 : table.length;
        }

        @Override
        public boolean hasNext() {
            final Entry<K, V>[] t = table;

            while (nextKey == null) {
                Entry<K, V> e = entry;
                int i = index;
                while (e == null && i > 0) {
                    e = t[--i];
                }
                entry = e;
                index = i;
                if (e == null) {
                    currentKey = null;
                    return false;
                }
                nextKey = e.get(); // hold on to key in strong ref
                if (nextKey == null) {
                    entry = entry.next;
                }
            }
            return true;
        }

        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }

            SoftKeyHashMap.this.remove(currentKey);
            expectedModCount = modCount;
            lastReturned = null;
            currentKey = null;
        }

        /**
         * The common parts of next() across different types of iterators
         * @return next entry if available
         * @throws ConcurrentModificationException in case of concurrent modification
         * @throws NoSuchElementException if there is no next elements (N.B. should be called only after {@link #hasNext()})
         */
        protected Entry<K, V> nextEntry() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (nextKey == null && !hasNext()) {
                throw new NoSuchElementException();
            }

            lastReturned = entry;
            entry = entry.next;
            currentKey = nextKey;
            nextKey = null;
            return lastReturned;
        }
    }

    static final class EntrySpliterator<K, V>
            extends SoftHashMapSpliterator<K, V>
            implements Spliterator<Map.Entry<K, V>> {
        EntrySpliterator(final SoftKeyHashMap<K, V> m, final int origin, final int fence, final int est,
                final int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        @Override
        public int characteristics() {
            return Spliterator.DISTINCT;
        }

        @Override
        public void forEachRemaining(final Consumer<? super Map.Entry<K, V>> action) {
            int i;
            int hi;
            final int mc;
            if (action == null) {
                throw new NullPointerException();
            }
            final SoftKeyHashMap<K, V> m = map;
            final SoftKeyHashMap.Entry<K, V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = tab.length;
            } else {
                mc = expectedModCount;
            }
            if (tab.length >= hi && (i = index) >= 0 && (i < (index = hi) || current != null)) {
                SoftKeyHashMap.Entry<K, V> p = current;
                current = null; // exhaust
                do {
                    if (p == null) {
                        p = tab[i++];
                    } else {
                        final Object x = p.get();
                        final V v = p.value;
                        p = p.next;
                        if (x != null) {
                            @SuppressWarnings("unchecked")
                            final
                                    K k
                                    = (K) SoftKeyHashMap.unmaskNull(x);
                            action.accept(new AbstractMap.SimpleImmutableEntry<>(k, v));
                        }
                    }
                } while (p != null || i < hi);
            }
            if (m.modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public boolean tryAdvance(final Consumer<? super Map.Entry<K, V>> action) {
            final int hi;
            if (action == null) {
                throw new NullPointerException();
            }
            final SoftKeyHashMap.Entry<K, V>[] tab = map.table;
            if (tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null) {
                        current = tab[index++];
                    } else {
                        final Object x = current.get();
                        final V v = current.value;
                        current = current.next;
                        if (x != null) {
                            @SuppressWarnings("unchecked")
                            final
                                    K k
                                    = (K) SoftKeyHashMap.unmaskNull(x);
                            action.accept(new AbstractMap.SimpleImmutableEntry<>(k, v));
                            if (map.modCount != expectedModCount) {
                                throw new ConcurrentModificationException();
                            }
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public EntrySpliterator<K, V> trySplit() {
            final int hi = getFence();
            final int lo = index;
            final int mid = lo + hi >>> 1;
            return lo >= mid ? null : new EntrySpliterator<>(map, lo, index = mid, est >>>= 1, expectedModCount);
        }
    }

    static final class KeySpliterator<K, V>
            extends SoftHashMapSpliterator<K, V>
            implements Spliterator<K> {
        KeySpliterator(final SoftKeyHashMap<K, V> m, final int origin, final int fence, final int est,
                final int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        @Override
        public int characteristics() {
            return Spliterator.DISTINCT;
        }

        @Override
        public void forEachRemaining(final Consumer<? super K> action) {
            int i;
            int hi;
            final int mc;
            if (action == null) {
                throw new NullPointerException();
            }
            final SoftKeyHashMap<K, V> m = map;
            final SoftKeyHashMap.Entry<K, V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = tab.length;
            } else {
                mc = expectedModCount;
            }
            if (tab.length >= hi && (i = index) >= 0 && (i < (index = hi) || current != null)) {
                SoftKeyHashMap.Entry<K, V> p = current;
                current = null; // exhaust
                do {
                    if (p == null) {
                        p = tab[i++];
                    } else {
                        final Object x = p.get();
                        p = p.next;
                        if (x != null) {
                            @SuppressWarnings("unchecked")
                            final
                                    K k
                                    = (K) SoftKeyHashMap.unmaskNull(x);
                            action.accept(k);
                        }
                    }
                } while (p != null || i < hi);
            }
            if (m.modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public boolean tryAdvance(final Consumer<? super K> action) {
            final int hi;
            if (action == null) {
                throw new NullPointerException();
            }
            final SoftKeyHashMap.Entry<K, V>[] tab = map.table;
            if (tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null) {
                        current = tab[index++];
                    } else {
                        final Object x = current.get();
                        current = current.next;
                        if (x != null) {
                            @SuppressWarnings("unchecked")
                            final
                                    K k
                                    = (K) SoftKeyHashMap.unmaskNull(x);
                            action.accept(k);
                            if (map.modCount != expectedModCount) {
                                throw new ConcurrentModificationException();
                            }
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public KeySpliterator<K, V> trySplit() {
            final int hi = getFence();
            final int lo = index;
            final int mid = lo + hi >>> 1;
            return lo >= mid ? null : new KeySpliterator<>(map, lo, index = mid, est >>>= 1, expectedModCount);
        }
    }

    /**
     * Similar form as other hash Spliterators, but skips dead
     * elements.
     * @param <K> generics for key
     * @param <V> generics for value
     */
    static class SoftHashMapSpliterator<K, V> {
        final SoftKeyHashMap<K, V> map;
        SoftKeyHashMap.Entry<K, V> current; // current node
        int index; // current index, modified on advance/split
        int fence; // -1 until first use; then one past last index
        int est; // size estimate
        int expectedModCount; // for comodification checks

        SoftHashMapSpliterator(final SoftKeyHashMap<K, V> m, final int origin,
                final int fence, final int est,
                final int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        public final long estimateSize() {
            getFence(); // force init
            return est;
        }

        final int getFence() { // initialize fence and size on first use
            int hi;
            if ((hi = fence) < 0) {
                final SoftKeyHashMap<K, V> m = map;
                est = m.size();
                expectedModCount = m.modCount;
                hi = fence = m.table.length;
            }
            return hi;
        }
    }

    static final class ValueSpliterator<K, V>
            extends SoftHashMapSpliterator<K, V>
            implements Spliterator<V> {
        ValueSpliterator(final SoftKeyHashMap<K, V> m, final int origin, final int fence, final int est,
                final int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        @Override
        public int characteristics() {
            return 0;
        }

        @Override
        public void forEachRemaining(final Consumer<? super V> action) {
            int i;
            int hi;
            final int mc;
            if (action == null) {
                throw new NullPointerException();
            }
            final SoftKeyHashMap<K, V> m = map;
            final SoftKeyHashMap.Entry<K, V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = tab.length;
            } else {
                mc = expectedModCount;
            }
            if (tab.length >= hi && (i = index) >= 0 && (i < (index = hi) || current != null)) {
                SoftKeyHashMap.Entry<K, V> p = current;
                current = null; // exhaust
                do {
                    if (p == null) {
                        p = tab[i++];
                    } else {
                        final Object x = p.get();
                        final V v = p.value;
                        p = p.next;
                        if (x != null) {
                            action.accept(v);
                        }
                    }
                } while (p != null || i < hi);
            }
            if (m.modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public boolean tryAdvance(final Consumer<? super V> action) {
            final int hi;
            if (action == null) {
                throw new NullPointerException();
            }
            final SoftKeyHashMap.Entry<K, V>[] tab = map.table;
            if (tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null) {
                        current = tab[index++];
                    } else {
                        final Object x = current.get();
                        final V v = current.value;
                        current = current.next;
                        if (x != null) {
                            action.accept(v);
                            if (map.modCount != expectedModCount) {
                                throw new ConcurrentModificationException();
                            }
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public ValueSpliterator<K, V> trySplit() {
            final int hi = getFence();
            final int lo = index;
            final int mid = lo + hi >>> 1;
            return lo >= mid ? null : new ValueSpliterator<>(map, lo, index = mid, est >>>= 1, expectedModCount);
        }
    }
}
