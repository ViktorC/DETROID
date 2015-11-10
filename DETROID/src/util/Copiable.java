package util;

/**A functional interface declaring a method for deep-copying objects of the implementing class.
 * 
 * @author Viktor
 *
 * @param <T> The type of class that implements the interface.
 */
public interface Copiable<T> {
	
	/**Returns a deep copy of the object. */
	T deepCopy();
}
