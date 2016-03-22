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
	 * @param bitmap
	 * @return
	 */
	public final static byte indexOfBit(long bitmap) {
		return DE_BRUIJN_TABLE[(int)((bitmap*DE_BRUIJN_CONST) >>> 58)];
	}
	/**
	 * Returns the most significant (leftmost) bit in a long.
	 * 
	 * @param bitmap
	 * @return
	 */
	public final static long getMSBit(long bitmap) {
		bitmap |= (bitmap >> 1);
		bitmap |= (bitmap >> 2);
		bitmap |= (bitmap >> 4);
		bitmap |= (bitmap >> 8);
		bitmap |= (bitmap >> 16);
		bitmap |= (bitmap >> 32);
		return bitmap - (bitmap >>> 1);
	}
	/**
	 * Returns a long with the most significant (leftmost) bit in the input parameter reset.
	 * 
	 * @param bitmap
	 * @return
	 */
	public final static long resetMSBit(long bitmap) {
		return bitmap^getMSBit(bitmap);
	}
	/**
	 * Returns the index of the most significant (leftmost) bit in a long.
	 * 
	 * @param bitmap
	 * @return
	 */
	public final static byte indexOfMSBit(long bitmap) {
		return DE_BRUIJN_TABLE[(int)((getMSBit(bitmap)*DE_BRUIJN_CONST) >>> 58)];
	}
	/**
	 * Returns the least significant (rightmost) bit in a long.
	 * 
	 * @param bitmap
	 * @return
	 */
	public final static long getLSBit(long bitmap) {
		return bitmap & -bitmap;
	}
	/**
	 * Returns a long with the least significant (rightmost) bit in the input parameter reset.
	 * 
	 * @param bitmap
	 * @return
	 */
	public final static long resetLSBit(long bitmap) {
		return bitmap & (bitmap - 1);
	}
	/**
	 * Returns the index of the least significant (rightmost) bit in a long.
	 * 
	 * @param bitmap
	 * @return
	 */
	public final static byte indexOfLSBit(long bitmap) {
		return DE_BRUIJN_TABLE[(int)((getLSBit(bitmap)*DE_BRUIJN_CONST) >>> 58)];
	}
	/**
	 * Returns the number of set bits in a long.
	 * 
	 * @param bitmap
	 * @return
	 */
	public final static byte getCardinality(long bitmap) {
		bitmap -= ((bitmap >>> 1) & SWAR_POPCOUNT_CONST1);
		bitmap  = (bitmap & SWAR_POPCOUNT_CONST2) + ((bitmap >>> 2) & SWAR_POPCOUNT_CONST2);
		bitmap  = (bitmap + (bitmap >>> 4)) & SWAR_POPCOUNT_CONST3;
	    return (byte)((bitmap*SWAR_POPCOUNT_CONSTF) >>> 56);
	}
	/**
	 * Returns a long with the bits of the input parameter reversed.
	 * 
	 * @param bitmap
	 * @return
	 */
	public final static long reverse(long bitmap) {
		bitmap = (((bitmap & BIT_REVERSAL_1_CONST1)  >>> 1)  | ((bitmap & BIT_REVERSAL_1_CONST2)  << 1));
		bitmap = (((bitmap & BIT_REVERSAL_2_CONST1)  >>> 2)  | ((bitmap & BIT_REVERSAL_2_CONST2)  << 2));
		bitmap = (((bitmap & BIT_REVERSAL_4_CONST1)  >>> 4)  | ((bitmap & BIT_REVERSAL_4_CONST2)  << 4));
		bitmap = (((bitmap & BIT_REVERSAL_8_CONST1)  >>> 8)  | ((bitmap & BIT_REVERSAL_8_CONST2)  << 8));
		bitmap = (((bitmap & BIT_REVERSAL_16_CONST1) >>> 16) | ((bitmap & BIT_REVERSAL_16_CONST2) << 16));
		return   (( bitmap >>> 32) 							 | ( bitmap << 32));
	}
	/**
	 * Returns a long with the bytes of the input parameter reversed/flipped.
	 * 
	 * @param bitmap
	 * @return
	 */
	public final static long reverseBytes(long bitmap) {
		bitmap = (bitmap & BIT_REVERSAL_32_CONST1) >>> 32 | (bitmap & BIT_REVERSAL_32_CONST2) << 32;
		bitmap = (bitmap & BIT_REVERSAL_16_CONST1) >>> 16 | (bitmap & BIT_REVERSAL_16_CONST2) << 16;
		return	 (bitmap & BIT_REVERSAL_8_CONST1)  >>> 8  | (bitmap & BIT_REVERSAL_8_CONST2)  << 8;
	}
	/**
	 * Returns a queue of the indexes of all set bits in the input parameter.
	 * 
	 * @param bitmap
	 * @return
	 */
	public final static ByteStack serialize(long bitmap) {
		ByteStack out = new ByteStack();
		while (bitmap != 0) {
			out.add(indexOfLSBit(bitmap));
			bitmap = resetLSBit(bitmap);
		}
		return out;
	}
	/**
	 * Returns an array of the indexes of all set bits in the input parameter.
	 * 
	 * @param bitmap
	 * @param numberOfSetBits is not checked
	 * @return
	 */
	public final static byte[] serialize(long bitmap, byte numberOfSetBits) {
		byte[] series = new byte[numberOfSetBits];
		int ind = 0;
		while (bitmap != 0) {
			series[ind] = indexOfLSBit(bitmap);
			bitmap = resetLSBit(bitmap);
			ind++;
		}
		return series;
	}
	/**
	 * Returns a String representation of a long in binary form with all the 64 bits displayed whether set or not.
	 * 
	 * @param bitmap
	 * @return
	 */
	public final static String toBinaryString(long bitmap) {
		String binString = Long.toBinaryString(bitmap);
		return ("0000000000000000000000000000000000000000000000000000000000000000" + binString).substring(binString.length());
	}
	/**
	 * Returns the binary literal of the input long as a String.
	 * 
	 * @param bitmap
	 * @return
	 */
	public final static String toBinaryLiteral(long bitmap) {
		return "0b"+ toBinaryString(bitmap) + "L";
	}
	/**
	 * Returns the hexadecimal literal of the input long as a String.
	 * 
	 * @param bitmap
	 * @return
	 */
	public final static String toHexLiteral(long bitmap) {
		String hexString = Long.toHexString(bitmap);
		return "0x" + ("0000000000000000" + hexString).substring(hexString.length()).toUpperCase() + "L";
	}
}
