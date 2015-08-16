package util;

/**A generic interface for comparing objects.
 * 
 * @author Viktor
 *
 * @param <T>
 */
public interface Comparable<T> {

	/**Returns a quantified, numeric difference between the two objects; if the returned value is positive, the owner of the instance method is greater
	 * than the parameter object, if the returned value is 0, they are equal in, and if it is negative, the parameter object is greater.
	 * 
	 * @param t
	 * @return
	 */
	public int compareTo(T t);
	/**Returns whether the object that invoked the method is 'greater' than the parameter object.
	 * 
	 * @param t
	 * @return
	 */
	public boolean greaterThan(T t);
	/**Returns whether the object that invoked the method is 'smaller' than the parameter object.
	 * 
	 * @param t
	 * @return
	 */
	public boolean smallerThan(T t);
	
}
