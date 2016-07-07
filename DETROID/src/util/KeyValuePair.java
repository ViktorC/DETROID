package util;

/**
 * A simple data structure for storing a value associated to a key.
 * 
 * @author Viktor
 *
 * @param <K> The type of the key.
 * @param <V> The type of the value.
 */
public class KeyValuePair<K, V> {

	final K key;
	final V value;
	
	public KeyValuePair(K key, V value) {
		this.key = key;
		this.value = value;
	}
	public K getKey() {
		return key;
	}
	public V getValue() {
		return value;
	}
}