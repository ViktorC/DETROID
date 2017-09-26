package net.viktorc.detroid.framework.util;


/**
 * A static class exclusively for bitwise operations. Some of them are implemented in the Java API, but to have a clear idea about
 * the costs and to understand the logic, they are re-implemented and provided here.
 * 
 * @author Viktor
 *
 */
public final class BitOperations {
	
	private static final byte[] DE_BRUIJN_TABLE = {
			 0,  1, 48,  2, 57, 49, 28,  3,
			61, 58, 50, 42, 38, 29, 17,  4,
			62, 55, 59, 36, 53, 51, 43, 22,
			45, 39, 33, 30, 24, 18, 12,  5,
			63, 47, 56, 27, 60, 41, 37, 16,
			54, 35, 52, 21, 44, 32, 23, 11,
			46, 26, 40, 15, 34, 20, 31, 10,
			25, 14, 19,  9, 13,  8,  7,  6 };
	private static final long DE_BRUIJN_CONST = 0x03F79D71B4CB0A89L;
	
	private BitOperations() { }
	/**
	 * Returns the index of the single bit set in the input variable. It is assumed that the input parameter has only one set bit and it is not
	 * checked!
	 * 
	 * @param n The number with a single set bit.
	 * @return The index of that bit.
	 */
	public static byte indexOfBit(long n) {
		return DE_BRUIJN_TABLE[(int) ((n*DE_BRUIJN_CONST) >>> 58)];
	}
	/**
	 * Returns the most significant (leftmost) bit in a long.
	 * 
	 * @param n The number.
	 * @return The value of the leftmost set bit.
	 */
	public static long getMSBit(long n) {
		n |= (n >> 1);
		n |= (n >> 2);
		n |= (n >> 4);
		n |= (n >> 8);
		n |= (n >> 16);
		n |= (n >> 32);
		return n - (n >>> 1);
	}
	/**
	 * Returns a long with the most significant (leftmost) bit in the input parameter reset.
	 * 
	 * @param n The number.
	 * @return The number with the leftmost set bit reset.
	 */
	public static long resetMSBit(long n) {
		return n^getMSBit(n);
	}
	/**
	 * Returns the index of the most significant (leftmost) bit in a long.
	 * 
	 * @param n The number.
	 * @return The index of the leftmost set bit.
	 */
	public static byte indexOfMSBit(long n) {
		return DE_BRUIJN_TABLE[(int) ((getMSBit(n)*DE_BRUIJN_CONST) >>> 58)];
	}
	/**
	 * Returns the least significant (rightmost) bit in a long.
	 * 
	 * @param n The number.
	 * @return The value of the rightmost set bit.
	 */
	public static long getLSBit(long n) {
		return n & -n;
	}
	/**
	 * Returns a long with the least significant (rightmost) bit in the input parameter reset.
	 * 
	 * @param n The number.
	 * @return The number with the rightmost set bit reset.
	 */
	public static long resetLSBit(long n) {
		return n & (n - 1);
	}
	/**
	 * Returns the index of the least significant (rightmost) bit in a long.
	 * 
	 * @param n The number.
	 * @return The index of the rightmost set bit.
	 */
	public static byte indexOfLSBit(long n) {
		return DE_BRUIJN_TABLE[(int) (((n & -n)*DE_BRUIJN_CONST) >>> 58)];
	}
	/**
	 * Returns the number of set bits in a long.
	 * 
	 * @param n The number.
	 * @return The Hamming-weight of the number.
	 */
	public static byte hammingWeight(long n) {
		n -= ((n >>> 1) & 0x5555555555555555L);
		n = (n & 0x3333333333333333L) + ((n >>> 2) & 0x3333333333333333L);
		n = (n + (n >>> 4)) & 0x0F0F0F0F0F0F0F0FL;
	    return (byte) ((n*0x0101010101010101L) >>> 56);
	}
	/**
	 * Returns a long with the bits of the input parameter reversed.
	 * 
	 * @param n The number.
	 * @return The number with the bits reversed.
	 */
	public static long reverse(long n) {
		n = (((n & 0xAAAAAAAAAAAAAAAAL) >>> 1) | ((n & 0x5555555555555555L) << 1));
		n = (((n & 0xCCCCCCCCCCCCCCCCL) >>> 2) | ((n & 0x3333333333333333L) << 2));
		n = (((n & 0xF0F0F0F0F0F0F0F0L) >>> 4) | ((n & 0x0F0F0F0F0F0F0F0FL) << 4));
		n = (((n & 0xFF00FF00FF00FF00L) >>> 8) | ((n & 0x00FF00FF00FF00FFL) << 8));
		n = (((n & 0xFFFF0000FFFF0000L) >>> 16) | ((n & 0x0000FFFF0000FFFFL) << 16));
		return ((n >>> 32) | (n << 32));
	}
	/**
	 * Returns a long with the bytes of the input parameter reversed/flipped.
	 * 
	 * @param n The number.
	 * @return The number with the bytes reversed.
	 */
	public static long reverseBytes(long n) {
		n = (n & 0xFFFFFFFF00000000L) >>> 32 | (n & 0x00000000FFFFFFFFL) << 32;
		n = (n & 0xFFFF0000FFFF0000L) >>> 16 | (n & 0x0000FFFF0000FFFFL) << 16;
		return (n & 0xFF00FF00FF00FF00L)  >>> 8 | (n & 0x00FF00FF00FF00FFL) << 8;
	}
	/**
	 * Returns an array of the indexes of all set bits in the input parameter.
	 * 
	 * @param n The number.
	 * @return An array of bytes representing the indexes of all the set bits in the number.
	 */
	public static byte[] serialize(long n) {
		byte[] series = new byte[hammingWeight(n)];
		int ind = 0;
		while (n != 0) {
			series[ind] = DE_BRUIJN_TABLE[(int) (((n & -n)*DE_BRUIJN_CONST) >>> 58)];
			n = n & (n - 1);
			ind++;
		}
		return series;
	}
	/**
	 * Returns an array of all the bitwise subsets of the parameter number.
	 * 
	 * @param n The number.
	 * @return An array of the numbers made up by all the bitwise subsets of the number.
	 */
	public static long[] getAllSubsets(long n) {
		byte[] bitIndArray = BitOperations.serialize(n);
		byte[] subsetBitIndArr;
		int numOfSubsets = 1 << bitIndArray.length;
		long[] combArray = new long[numOfSubsets];
		long combination;
		for (int i = 0; i < numOfSubsets; i++) {
			subsetBitIndArr = BitOperations.serialize(i);
			combination = 0L;
			for (byte b : subsetBitIndArr)
				combination |= (1L << bitIndArray[b]);
			combArray[i] = combination;
		}
		return combArray;
	}
	/**
	 * Returns a string representation of a long in binary form with all the 64 bits displayed whether set or not.
	 * 
	 * @param n The number.
	 * @return A string representation with all leading zeros included.
	 */
	public static String toBinaryString(long n) {
		String binString = Long.toBinaryString(n);
		return ("0000000000000000000000000000000000000000000000000000000000000000" + binString).substring(binString.length());
	}
	/**
	 * Returns the binary literal of the input long as a string.
	 * 
	 * @param n The number.
	 * @return A Java binary literal with all leading zeros included.
	 */
	public static String toBinaryLiteral(long n) {
		return "0b"+ toBinaryString(n) + "L";
	}
	/**
	 * Returns the hexadecimal literal of the input long as a string.
	 * 
	 * @param n The number.
	 * @return A Java hexadecimal literal with all leading zeros included.
	 */
	public static String toHexLiteral(long n) {
		String hexString = Long.toHexString(n);
		return "0x" + ("0000000000000000" + hexString).substring(hexString.length()).toUpperCase() + "L";
	}
	
}
