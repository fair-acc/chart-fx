package de.gsi.dataset.spi.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ObjectObjectMap<K, V> implements Map<K, V> {
	private static final Object FREE_KEY = new Object();
	private static final Object REMOVED_KEY = new Object();

	/** Keys and values */
	private Object[] m_data;

	/** Value for the null key (if inserted into a map) */
	private Object m_nullValue;
	private boolean m_hasNull;

	/** Fill factor, must be between (0 and 1) */
	private final float m_fillFactor;
	/** We will resize a map once it reaches this size */
	private int m_threshold;
	/** Current map size */
	private int m_size;
	/** Mask to calculate the original position */
	private int m_mask;
	/** Mask to wrap the actual array pointer */
	private int m_mask2;

	public ObjectObjectMap(final int size, final float fillFactor) {
		if (fillFactor <= 0 || fillFactor >= 1)
			throw new IllegalArgumentException("FillFactor must be in (0, 1)");
		if (size <= 0)
			throw new IllegalArgumentException("Size must be positive!");
		final int capacity = HashMapTools.arraySize(size, fillFactor);
		m_mask = capacity - 1;
		m_mask2 = capacity * 2 - 1;
		m_fillFactor = fillFactor;

		m_data = new Object[capacity * 2];
		Arrays.fill(m_data, FREE_KEY);

		m_threshold = (int) (capacity * fillFactor);
	}

	@Override
	public void clear() {
		Arrays.fill(m_data, FREE_KEY);
	}

	@Override
	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		throw new IllegalStateException("not implemented");
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		throw new IllegalStateException("not implemented");
		// return null;
	}

	@Override
	public V get(final Object key) {
		if (key == null)
			return (V) m_nullValue; // we null it on remove, so safe not to check a flag here

		int ptr = (key.hashCode() & m_mask) << 1;
		Object k = m_data[ptr];

		if (k == FREE_KEY)
			return null; // end of chain already
		if (k.equals(key)) // we check FREE and REMOVED prior to this call
			return (V) m_data[ptr + 1];
		while (true) {
			ptr = (ptr + 2) & m_mask2; // that's next index
			k = m_data[ptr];
			if (k == FREE_KEY)
				return null;
			if (k.equals(key))
				return (V) m_data[ptr + 1];
		}
	}

	public int getStartIndex(final Object key) {
		// key is not null here
		return key.hashCode() & m_mask;
	}

	private V insertNullKey(final V value) {
		if (m_hasNull) {
			final Object ret = m_nullValue;
			m_nullValue = value;
			return (V) ret;
		} else {
			m_nullValue = value;
			++m_size;
			return null;
		}
	}

	@Override
	public boolean isEmpty() {
		return m_size == 0;
	}

	@Override
	public Set<K> keySet() {
		// TODO Auto-generated method stub
		throw new IllegalStateException("not implemented");
//		Set<K> retVal = new HashSet<>();
//		for (int i=0; i < m_data.length; i++) {
//			if (m_data[i] != null) {
//				retVal.add(i);
//			}
//		}
//		return retVal;
	}

	@Override
	public V put(final K key, final V value) {
		if (key == null)
			return insertNullKey(value);

		int ptr = getStartIndex(key) << 1;
		Object k = m_data[ptr];

		if (k == FREE_KEY) // end of chain already
		{
			m_data[ptr] = key;
			m_data[ptr + 1] = value;
			if (m_size >= m_threshold)
				rehash(m_data.length * 2); // size is set inside
			else
				++m_size;
			return null;
		} else if (k.equals(key)) // we check FREE and REMOVED prior to this call
		{
			final Object ret = m_data[ptr + 1];
			m_data[ptr + 1] = value;
			return (V) ret;
		}

		int firstRemoved = -1;
		if (k == REMOVED_KEY)
			firstRemoved = ptr; // we may find a key later

		while (true) {
			ptr = (ptr + 2) & m_mask2; // that's next index calculation
			k = m_data[ptr];
			if (k == FREE_KEY) {
				if (firstRemoved != -1)
					ptr = firstRemoved;
				m_data[ptr] = key;
				m_data[ptr + 1] = value;
				if (m_size >= m_threshold)
					rehash(m_data.length * 2); // size is set inside
				else
					++m_size;
				return null;
			} else if (k.equals(key)) {
				final Object ret = m_data[ptr + 1];
				m_data[ptr + 1] = value;
				return (V) ret;
			} else if (k == REMOVED_KEY) {
				if (firstRemoved == -1)
					firstRemoved = ptr;
			}
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new IllegalStateException("not implemented");
	}

	private void rehash(final int newCapacity) {
		m_threshold = (int) (newCapacity / 2 * m_fillFactor);
		m_mask = newCapacity / 2 - 1;
		m_mask2 = newCapacity - 1;

		final int oldCapacity = m_data.length;
		final Object[] oldData = m_data;

		m_data = new Object[newCapacity];
		Arrays.fill(m_data, FREE_KEY);

		m_size = m_hasNull ? 1 : 0;

		for (int i = 0; i < oldCapacity; i += 2) {
			final Object oldKey = oldData[i];
			if (oldKey != FREE_KEY && oldKey != REMOVED_KEY)
				put((K) oldKey, (V) oldData[i + 1]);
		}
	}

	@Override
	public V remove(final Object key) {
		if (key == null)
			return removeNullKey();

		int ptr = getStartIndex(key) << 1;
		Object k = m_data[ptr];
		if (k == FREE_KEY)
			return null; // end of chain already
		else if (k.equals(key)) // we check FREE and REMOVED prior to this call
		{
			--m_size;
			if (m_data[(ptr + 2) & m_mask2] == FREE_KEY)
				m_data[ptr] = FREE_KEY;
			else
				m_data[ptr] = REMOVED_KEY;
			final V ret = (V) m_data[ptr + 1];
			m_data[ptr + 1] = null;
			return ret;
		}
		while (true) {
			ptr = (ptr + 2) & m_mask2; // that's next index calculation
			k = m_data[ptr];
			if (k == FREE_KEY)
				return null;
			else if (k.equals(key)) {
				--m_size;
				if (m_data[(ptr + 2) & m_mask2] == FREE_KEY)
					m_data[ptr] = FREE_KEY;
				else
					m_data[ptr] = REMOVED_KEY;
				final V ret = (V) m_data[ptr + 1];
				m_data[ptr + 1] = null;
				return ret;
			}
		}
	}

	private V removeNullKey() {
		if (m_hasNull) {
			final Object ret = m_nullValue;
			m_nullValue = null;
			m_hasNull = false;
			--m_size;
			return (V) ret;
		} else {
			return null;
		}
	}

	@Override
	public int size() {
		return m_size;
	}

	@Override
	public Collection<V> values() {
		// TODO Auto-generated method stub
		//return null;
		throw new IllegalStateException("not implemented");
	}
}