package net.viktorc.detroid.framework.util;

/**
 * An interface that defines a function for providing a 64 bit hash key that identifies the object and allows it to be hashed 
 * onto an index.
 * 
 * @author Viktor
 *
 */
public interface Hashable {

	/**
	 * Returns a long integer hash code.
	 * 
	 * @return A 64 bit hash key.
	 */
	long hashKey();
	
}
