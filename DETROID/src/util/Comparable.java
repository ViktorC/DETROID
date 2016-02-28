package util;

/**
 * A generic interface for comparing objects.
 * 
 * @author Viktor
 *
 * @param <T>
 */
public interface Comparable<T> {

	/**
	 * Returns whether the object that invoked the method is more valuable than the parameter object.
	 * 
	 * @param t
	 * @return
	 */
	boolean betterThan(T t);
	/**
	 * Returns whether the object that invoked the method is less valuable than the parameter object.
	 * 
	 * @param t
	 * @return
	 */
	default boolean worseThan(T t) {
		throw new UnsupportedOperationException("Not implemented.");
	}
	
}
