package util;

/**
 * An interface that defines a method that returns the estimated size of the object it is called on in the JVM's heap in bytes.
 * 
 * @author Viktor
 *
 */
public interface Estimable {

	/**
	 * Returns the memory overhead in bytes.
	 * 
	 * @return The estimated memory the object consumes in the heap.
	 */
	long size();
}
