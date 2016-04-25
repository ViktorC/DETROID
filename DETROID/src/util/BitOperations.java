package util;


/**
 * A static class exclusively for bitwise operations. Some of them are implemented in the Java API, but to have a clear idea about
 * the costs and to understand the logic, they are re-implemented and provided here.
 * 
 * @author Viktor
 *
 */
public final class BitOperations {
	
	private final static long DE_BRUIJN_CONST = 0b0000001111110111100111010111000110110100110010110000101010001001L;
	private final static byte[] DE_BRUIJN_TABLE =
		{ 0,  1, 48,  2, 57, 49, 28,  3,
		 61, 58, 50, 42, 38, 29, 17,  4,
		 62, 55, 59, 36, 53, 51, 43, 22,
		 45, 39, 33, 30, 24, 18, 12,  5,
		 63, 47, 56, 27, 60, 41, 37, 16,
		 54, 35, 52, 21, 44, 32, 23, 11,
		 46, 26, 40, 15, 34, 20, 31, 10,
		 25, 14, 19,  9, 13,  8,  7,  6};
	
	private final static long SWAR_POPCOUNT_CONST1 = 0b0101010101010101010101010101010101010101010101010101010101010101L;
	private final static long SWAR_POPCOUNT_CONST2 = 0b0011001100110011001100110011001100110011001100110011001100110011L;
	private final static long SWAR_POPCOUNT_CONST3 = 0b0000111100001111000011110000111100001111000011110000111100001111L;
	private final static long SWAR_POPCOUNT_CONSTF = 0b0000000100000001000000010000000100000001000000010000000100000001L;
		
	private final static long BIT_REVERSAL_1_CONST1 = 0b1010101010101010101010101010101010101010101010101010101010101010L;
	private final static long BIT_REVERSAL_1_CONST2 = 0b0101010101010101010101010101010101010101010101010101010101010101L;
	private final static long BIT_REVERSAL_2_CONST1 = 0b1100110011001100110011001100110011001100110011001100110011001100L;
	private final static long BIT_REVERSAL_2_CONST2 = 0b0011001100110011001100110011001100110011001100110011001100110011L;
	private final static long BIT_REVERSAL_4_CONST1 = 0b1111000011110000111100001111000011110000111100001111000011110000L;
	private final static long BIT_REVERSAL_4_CONST2 = 0b0000111100001111000011110000111100001111000011110000111100001111L;
	private final static long BIT_REVERSAL_8_CONST1 = 0b1111111100000000111111110000000011111111000000001111111100000000L;
	private final static long BIT_REVERSAL_8_CONST2 = 0b0000000011111111000000001111111100000000111111110000000011111111L;
	private final static long BIT_REVERSAL_16_CONST1 = 0b1111111111111111000000000000000011111111111111110000000000000000L;
	private final static long BIT_REVERSAL_16_CONST2 = 0b0000000000000000111111111111111100000000000000001111111111111111L;
	private final static long BIT_REVERSAL_32_CONST1 = 0b1111111111111111111111111111111100000000000000000000000000000000L;
	private final static long BIT_REVERSAL_32_CONST2 = 0b0000000000000000000000000000000011111111111111111111111111111111L;
	
	private BitOperations() {
		
	}
	/**
	 * Returns the index of the single bit set in the input variable. It is assumed that the input parameter has only one set bit and it is not
	 * checked!
	 * 
	 * @param n
	 * @return
	 */
	public final static byte indexOfBit(long n) {
		return DE_BRUIJN_TABLE[(int)((n*DE_BRUIJN_CONST) >>> 58)];
	}
	/**
	 * Returns the most significant (leftmost) bit in a long.
	 * 
	 * @param n
	 * @return
	 */
	public final static long getMSBit(long n) {
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
	 * @param n
	 * @return
	 */
	public final static long resetMSBit(long n) {
		return n^getMSBit(n);
	}
	/**
	 * Returns the index of the most significant (leftmost) bit in a long.
	 * 
	 * @param n
	 * @return
	 */
	public final static byte indexOfMSBit(long n) {
		return DE_BRUIJN_TABLE[(int)((getMSBit(n)*DE_BRUIJN_CONST) >>> 58)];
	}
	/**
	 * Returns the least significant (rightmost) bit in a long.
	 * 
	 * @param n
	 * @return
	 */
	public final static long getLSBit(long n) {
		return n & -n;
	}
	/**
	 * Returns a long with the least significant (rightmost) bit in the input parameter reset.
	 * 
	 * @param n
	 * @return
	 */
	public final static long resetLSBit(long n) {
		return n & (n - 1);
	}
	/**
	 * Returns the index of the least significant (rightmost) bit in a long.
	 * 
	 * @param n
	 * @return
	 */
	public final static byte indexOfLSBit(long n) {
		return DE_BRUIJN_TABLE[(int)(((n & -n)*DE_BRUIJN_CONST) >>> 58)];
	}
	/**
	 * Returns the number of set bits in a long.
	 * 
	 * @param n
	 * @return
	 */
	public final static byte getHammingWeight(long n) {
		n -= ((n >>> 1) & SWAR_POPCOUNT_CONST1);
		n  = (n & SWAR_POPCOUNT_CONST2) + ((n >>> 2) & SWAR_POPCOUNT_CONST2);
		n  = (n + (n >>> 4)) & SWAR_POPCOUNT_CONST3;
	    return (byte)((n*SWAR_POPCOUNT_CONSTF) >>> 56);
	}
	/**
	 * Returns a long with the bits of the input parameter reversed.
	 * 
	 * @param n
	 * @return
	 */
	public final static long reverse(long n) {
		n = (((n & BIT_REVERSAL_1_CONST1)  >>> 1)  | ((n & BIT_REVERSAL_1_CONST2)  << 1));
		n = (((n & BIT_REVERSAL_2_CONST1)  >>> 2)  | ((n & BIT_REVERSAL_2_CONST2)  << 2));
		n = (((n & BIT_REVERSAL_4_CONST1)  >>> 4)  | ((n & BIT_REVERSAL_4_CONST2)  << 4));
		n = (((n & BIT_REVERSAL_8_CONST1)  >>> 8)  | ((n & BIT_REVERSAL_8_CONST2)  << 8));
		n = (((n & BIT_REVERSAL_16_CONST1) >>> 16) | ((n & BIT_REVERSAL_16_CONST2) << 16));
		return   (( n >>> 32) 							 | ( n << 32));
	}
	/**
	 * Returns a long with the bytes of the input parameter reversed/flipped.
	 * 
	 * @param n
	 * @return
	 */
	public final static long reverseBytes(long n) {
		n = (n & BIT_REVERSAL_32_CONST1) >>> 32 | (n & BIT_REVERSAL_32_CONST2) << 32;
		n = (n & BIT_REVERSAL_16_CONST1) >>> 16 | (n & BIT_REVERSAL_16_CONST2) << 16;
		return	 (n & BIT_REVERSAL_8_CONST1)  >>> 8  | (n & BIT_REVERSAL_8_CONST2)  << 8;
	}
	/**
	 * Returns a queue of the indexes of all set bits in the input parameter.
	 * 
	 * @param n
	 * @return
	 */
	public final static ByteStack serialize(long n) {
		ByteStack out = new ByteStack();
		while (n != 0) {
			out.add(DE_BRUIJN_TABLE[(int)(((n & -n)*DE_BRUIJN_CONST) >>> 58)]);
			n = n & (n - 1);
		}
		return out;
	}
	/**
	 * Returns an array of the indexes of all set bits in the input parameter.
	 * 
	 * @param n
	 * @param numberOfSetBits The Hamming weight of the number. Assumed to be correct.
	 * @return
	 */
	public final static byte[] serialize(long n, byte numberOfSetBits) {
		byte[] series = new byte[numberOfSetBits];
		int ind = 0;
		while (n != 0) {
			series[ind] = DE_BRUIJN_TABLE[(int)(((n & -n)*DE_BRUIJN_CONST) >>> 58)];
			n = n & (n - 1);
			ind++;
		}
		return series;
	}
	/**
	 * Returns an array of all the bit-subsets of the parameter number.
	 * 
	 * @param n
	 * @return
	 */
	public final static long[] getAllSubsets(long n) {
		byte numOfSetBits = BitOperations.getHammingWeight(n);
		byte[] bitIndArray = BitOperations.serialize(n, numOfSetBits);
		ByteList subsetBitIndList;
		int numOfSubsets = 1 << numOfSetBits;
		long[] combArray = new long[numOfSubsets];
		long combination;
		for (int i = 0; i < numOfSubsets; i++) {
			subsetBitIndList = BitOperations.serialize(i);
			combination = 0L;
			while (subsetBitIndList.hasNext())
				combination |= (1L << bitIndArray[subsetBitIndList.next()]);
			combArray[i] = combination;
		}
		return combArray;
	}
	/**
	 * Returns a String representation of a long in binary form with all the 64 bits displayed whether set or not.
	 * 
	 * @param n
	 * @return
	 */
	public final static String toBinaryString(long n) {
		String binString = Long.toBinaryString(n);
		return ("0000000000000000000000000000000000000000000000000000000000000000" + binString).substring(binString.length());
	}
	/**
	 * Returns the binary literal of the input long as a String.
	 * 
	 * @param n
	 * @return
	 */
	public final static String toBinaryLiteral(long n) {
		return "0b"+ toBinaryString(n) + "L";
	}
	/**
	 * Returns the hexadecimal literal of the input long as a String.
	 * 
	 * @param n
	 * @return
	 */
	public final static String toHexLiteral(long n) {
		String hexString = Long.toHexString(n);
		return "0x" + ("0000000000000000" + hexString).substring(hexString.length()).toUpperCase() + "L";
	}
}
