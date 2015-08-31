package util;

/**A generic interface for comparing objects.
 * 
 * @author Viktor
 *
 * @param <T>
 */
public interface Comparable<T> {

	/**Returns whether the object that invoked the method is 'greater' than the parameter object.
	 * 
	 * @param t
	 * @return
	 */
	boolean greaterThan(T t);
	/**Returns whether the object that invoked the method is 'smaller' than the parameter object.
	 * 
	 * @param t
	 * @return
	 */
	boolean smallerThan(T t);
	
}
