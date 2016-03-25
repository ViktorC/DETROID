package util;

/**
 * A class that provides an implementation for storing non-negative integers in reflected binary Gray code. It supports operations such as
 * incrementation and decrementation.
 * 
 * @author Viktor
 *
 */
public class GrayCode {

	private final static long MAX_GRAY_VALUE = 1L << 62;
	
	private long grayValue;
	private boolean isHammingWeightOdd;
	
	/**
	 * Instantiates a Gray code number with a value of 0. It is the same as {@link #GrayCode(long) GrayCode(0)}.
	 */
	public GrayCode() {
		grayValue = 0;
		isHammingWeightOdd = false;
	}
	/**
	 * Instantiates a Gray code number for the specified integer.
	 * 
	 * @param n
	 * @throws IllegalArgumentException If n < 0.
	 */
	public GrayCode(long n) throws IllegalArgumentException {
		if (n < 0)
			throw new IllegalArgumentException("n has to be unsigned (positive).");
		grayValue = n^(n >>> 1);
		isHammingWeightOdd = BitOperations.getHammingWeight(n)%2 == 1;
	}
	/**
	 * Returns the reflected binary Gray code number.
	 * 
	 * @return
	 */
	public long get() {
		return grayValue;
	}
	/**
	 * Returns the natural decimal value of the gray code number.
	 * 
	 * @return
	 */
	public long getNaturalValue() {
		long n;
		n = grayValue^(grayValue >> 32);
		n = n^(n >> 16);
		n = n^(n >> 8);
		n = n^(n >> 4);
	    n = n^(n >> 2);
	    n = n^(n >> 1);
	    return n;
	}
	/**
	 * Increments the natural value of the gray code number by one and returns the gray code. If the gray code number's natural value is already
	 * the maximum long value, it does not get incremented.
	 * 
	 * @return
	 */
	public long incrementAndGet() {
		if (grayValue == MAX_GRAY_VALUE)
			return grayValue;
		grayValue = isHammingWeightOdd ? grayValue^((grayValue & -grayValue) << 1) : grayValue^1L;
		isHammingWeightOdd = !isHammingWeightOdd;
		return grayValue;
	}
	/**
	 * Decrements the natural value of the gray code number by one and returns the gray code. If the gray code number's natural value is already
	 * 0, it does not get incremented.
	 * 
	 * @return
	 */
	public long decrementAndGet() {
		if (grayValue == 0)
			return grayValue;
		grayValue = !isHammingWeightOdd ? grayValue^((grayValue & -grayValue) << 1) : grayValue^1L;
		isHammingWeightOdd = !isHammingWeightOdd;
		return grayValue;
	}
}
