package org.eclipse.emf.henshin.variability.matcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BiMap<K, V> {

	private final Map<K, V> keyToValue = new HashMap<>();
	private final Map<V, K> valueToKey = new HashMap<>();

	public V put(final K key, final V value) {
		this.valueToKey.put(value, key);
		return this.keyToValue.put(key, value);
	}

	public boolean containsKey(final K key) {
		return this.keyToValue.containsKey(key);
	}

	public V getValue(final K key) {
		return this.keyToValue.get(key);
	}

	public K getKey(final V value) {
		return this.valueToKey.get(value);
	}

	public Collection<V> values() {
		return this.valueToKey.keySet();
	}

	public Collection<K> keys() {
		return this.keyToValue.keySet();
	}
}

