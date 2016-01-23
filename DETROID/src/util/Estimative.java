package util;

/**An interface that defines a method that returns the estimated size of the object it is called on in the JVM's heap in bytes.
 * 
 * @author Viktor
 *
 */
public interface Estimative {

	/**Returns the base size (the sum of the size of the object's members plus the pointer) in bytes.
	 * 
	 * @return The sum of the object's member fields' and the pointer's size.
	 */
	int baseSize();
	/**Returns the memory overhead in bytes.
	 * 
	 * @return The estimated memory the object consumes in the heap.
	 */
	default int allocatedMemory() {
		int size = baseSize();
		// The JVM rounds the allocated memory up to the closest multiple of 8.
		return size%8 == 0 ? size : size + 8 - size%8;
	}
	
}
