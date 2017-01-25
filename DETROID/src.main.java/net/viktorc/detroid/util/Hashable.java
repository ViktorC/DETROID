package net.viktorc.detroid.util;

/**
 * A functional interface that defines a function for providing a 64 bit hash key that identifies the object and allows it to be hashed onto an index.
 * 
 * @author Viktor
 *
 */
public interface Hashable {

	/**
	 * Returns a long integer hash code.
	 */
	long hashKey();
	
}
