package engine;

import util.*;

import java.util.Random;

/**A bit board based class whose object holds information amongst others on the current board position, on all the previous moves and positions,
 * on castling and en passant rights, and on the player to move. It uses its own precalculated 'magic' move database to avoid the cost of
 * computing the possible move sets of sliding pieces on the fly. The functions include:
 * {@link #generateMoves() generateMoves}
 * {@link #makeMove(long) makeMove}
 * {@link #unmakeMove() unmakeMove}
 * {@link #perft(int) perft}
 * {@link #perftWithConsoleOutput(int, long, long, boolean) perftWithConsoleOutput}
 * 
 * It relies heavily on values hard-coded or computed on compile. These values are always different for each square on the board, thus most of them 
 * are stored in 64-fold enums with switch statements providing fast access (about one to five percent faster than array access) based on the index 
 * of the square.
 *  
 * @author Viktor
 *
 */
public class Board {
	
	/**A static class exclusively for bitwise operations. Some of them are implemented in the Java API, but to have a clear idea about
	 * the costs and to understand the logic, they are re-implemented and provided here.
	 * 
	 * @author Viktor
	 *
	 */
	public final static class BitOperations {
		
		final static int[] 		DE_BRUIJN_TABLE 	  	  = { 0,  1, 48,  2, 57, 49, 28,  3,
															 61, 58, 50, 42, 38, 29, 17,  4,
															 62, 55, 59, 36, 53, 51, 43, 22,
															 45, 39, 33, 30, 24, 18, 12,  5,
															 63, 47, 56, 27, 60, 41, 37, 16,
															 54, 35, 52, 21, 44, 32, 23, 11,
															 46, 26, 40, 15, 34, 20, 31, 10,
															 25, 14, 19,  9, 13,  8,  7,  6};
		
		final static long		DE_BRUIJN_CONST 		  =  0b0000001111110111100111010111000110110100110010110000101010001001L;
		
		final static long 		SWAR_POPCOUNT_CONST1  	  =  0b0101010101010101010101010101010101010101010101010101010101010101L;
		final static long 		SWAR_POPCOUNT_CONST2	  =  0b0011001100110011001100110011001100110011001100110011001100110011L;
		final static long 		SWAR_POPCOUNT_CONST3	  =  0b0000111100001111000011110000111100001111000011110000111100001111L;
		final static long 		SWAR_POPCOUNT_CONSTF	  =  0b0000000100000001000000010000000100000001000000010000000100000001L;
			
		final static long		BIT_REVERSAL_1_CONST1	  =  0b1010101010101010101010101010101010101010101010101010101010101010L;
		final static long		BIT_REVERSAL_1_CONST2	  =  0b0101010101010101010101010101010101010101010101010101010101010101L;
		final static long		BIT_REVERSAL_2_CONST1	  =  0b1100110011001100110011001100110011001100110011001100110011001100L;
		final static long		BIT_REVERSAL_2_CONST2	  =  0b0011001100110011001100110011001100110011001100110011001100110011L;
		final static long		BIT_REVERSAL_4_CONST1	  =  0b1111000011110000111100001111000011110000111100001111000011110000L;
		final static long		BIT_REVERSAL_4_CONST2	  =  0b0000111100001111000011110000111100001111000011110000111100001111L;
		final static long		BIT_REVERSAL_8_CONST1	  =  0b1111111100000000111111110000000011111111000000001111111100000000L;
		final static long		BIT_REVERSAL_8_CONST2	  =  0b0000000011111111000000001111111100000000111111110000000011111111L;
		final static long		BIT_REVERSAL_16_CONST1	  =  0b1111111111111111000000000000000011111111111111110000000000000000L;
		final static long		BIT_REVERSAL_16_CONST2	  =  0b0000000000000000111111111111111100000000000000001111111111111111L;
		final static long		BIT_REVERSAL_32_CONST1	  =  0b1111111111111111111111111111111100000000000000000000000000000000L;
		final static long		BIT_REVERSAL_32_CONST2	  =  0b0000000000000000000000000000000011111111111111111111111111111111L;
		
		/**Returns the most significant (leftmost) bit in a long.*/
		public final static long getMSBit(long bitmap) {
			bitmap |= (bitmap >> 1);
			bitmap |= (bitmap >> 2);
			bitmap |= (bitmap >> 4);
			bitmap |= (bitmap >> 8);
			bitmap |= (bitmap >> 16);
			bitmap |= (bitmap >> 32);
			return bitmap - (bitmap >>> 1);
		}
		/**Returns a long with the most significant (leftmost) bit in the input parameter reset.*/
		public final static long resetMSBit(long bitmap) {
			return bitmap^getMSBit(bitmap);
		}
		/**Returns the index of the most significant (leftmost) bit in a long.*/
		public final static int indexOfMSBit(long bitmap) {
			return DE_BRUIJN_TABLE[(int)((getMSBit(bitmap)*DE_BRUIJN_CONST) >>> 58)];
		}
		/**Returns the least significant (rightmost) bit in a long.*/
		public final static long getLSBit(long bitmap) {
			return bitmap & -bitmap;
		}
		/**Returns a long with the least significant (rightmost) bit in the input parameter reset.*/
		public final static long resetLSBit(long bitmap) {
			return bitmap & (bitmap - 1);
		}
		/**Returns the index of the least significant (rightmost) bit in a long.*/
		public final static int indexOfLSBit(long bitmap) {
			return DE_BRUIJN_TABLE[(int)((getLSBit(bitmap)*DE_BRUIJN_CONST) >>> 58)];
		}
		/**Returns a queue of the indexes of all set bits in the input parameter.*/
		public final static IntStack serialize(long bitmap) {
			IntStack out = new IntStack();
			while (bitmap != 0) {
				out.add(indexOfLSBit(bitmap));
				bitmap = resetLSBit(bitmap);
			}
			return out;
		}
		/**Returns an array of the indexes of all set bits in the input parameter.
		 * 
		 * @param numberOfSetBits is not checked*/
		public final static int[] serialize(long bitmap, int numberOfSetBits) {
			int[] series = new int[numberOfSetBits];
			int ind = 0;
			while (bitmap != 0) {
				series[ind] = indexOfLSBit(bitmap);
				bitmap = resetLSBit(bitmap);
				ind++;
			}
			return series;
		}
		/**Returns the number of set bits in a long.*/
		public final static int getCardinality(long bitmap) {
			bitmap -= ((bitmap >>> 1) & SWAR_POPCOUNT_CONST1);
			bitmap  = (bitmap & SWAR_POPCOUNT_CONST2) + ((bitmap >>> 2) & SWAR_POPCOUNT_CONST2);
			bitmap  = (bitmap + (bitmap >>> 4)) & SWAR_POPCOUNT_CONST3;
		    return (int)((bitmap*SWAR_POPCOUNT_CONSTF) >>> 56);
		}
		/**Returns a long with the bits of the input parameter reversed/flipped.
		 * 
		 * It is equal to a 180� rotation of the board.*/
		public final static long reverse(long bitmap) {
			bitmap = (((bitmap & BIT_REVERSAL_1_CONST1)  >>> 1)  | ((bitmap & BIT_REVERSAL_1_CONST2)  << 1));
			bitmap = (((bitmap & BIT_REVERSAL_2_CONST1)  >>> 2)  | ((bitmap & BIT_REVERSAL_2_CONST2)  << 2));
			bitmap = (((bitmap & BIT_REVERSAL_4_CONST1)  >>> 4)  | ((bitmap & BIT_REVERSAL_4_CONST2)  << 4));
			bitmap = (((bitmap & BIT_REVERSAL_8_CONST1)  >>> 8)  | ((bitmap & BIT_REVERSAL_8_CONST2)  << 8));
			bitmap = (((bitmap & BIT_REVERSAL_16_CONST1) >>> 16) | ((bitmap & BIT_REVERSAL_16_CONST2) << 16));
			return   (( bitmap >>> 32) 							 | ( bitmap << 32));
		}
		/**Returns a long with the bytes of the input parameter reversed/flipped.
		 * 
		 * It is equal to the 'horizontal' mirroring of the board.*/
		public final static long reverseBytes(long bitmap) {
			bitmap = (bitmap & BIT_REVERSAL_32_CONST1) >>> 32 | (bitmap & BIT_REVERSAL_32_CONST2) << 32;
			bitmap = (bitmap & BIT_REVERSAL_16_CONST1) >>> 16 | (bitmap & BIT_REVERSAL_16_CONST2) << 16;
			return	 (bitmap & BIT_REVERSAL_8_CONST1)  >>> 8  | (bitmap & BIT_REVERSAL_8_CONST2)  << 8;
		}
		/**One square westward shift.*/
		protected final static long vShiftRight(long bitmap) {
			return bitmap << 1;
		}
		/**One square eastward shift.*/
		protected final static long vShiftLeft(long bitmap) {
			return bitmap >>> 1;
		}
		/**One square northward shift.*/
		protected final static long vShiftUp(long bitmap) {
			return bitmap << 8;
		}
		/**One square southward shift.*/
		protected final static long vShiftDown(long bitmap) {
			return bitmap >>> 8;
		}
		/**One square north-westward shift.*/
		protected final static long vShiftUpRight(long bitmap) {
			return bitmap << 9;
		}
		/**One square south-westward shift.*/
		protected final static long vShiftDownRight(long bitmap) {
			return bitmap >>> 7;
		}
		/**One square north-eastward shift.*/
		protected final static long vShiftUpLeft(long bitmap) {
			return bitmap << 7;
		}
		/**One square south-eastward shift.*/
		protected final static long vShiftDownLeft(long bitmap) {
			return bitmap >>> 9;
		}
		
	}

	/**An enum type for the 64 squares of the chess board. Each constant has a field that contains a long with only the bit on
	 * the respective square's index set.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum Square {
		
		A1 (0b0000000000000000000000000000000000000000000000000000000000000001L),
		B1 (0b0000000000000000000000000000000000000000000000000000000000000010L),
		C1 (0b0000000000000000000000000000000000000000000000000000000000000100L),
		D1 (0b0000000000000000000000000000000000000000000000000000000000001000L),
		E1 (0b0000000000000000000000000000000000000000000000000000000000010000L),
		F1 (0b0000000000000000000000000000000000000000000000000000000000100000L),
		G1 (0b0000000000000000000000000000000000000000000000000000000001000000L),
		H1 (0b0000000000000000000000000000000000000000000000000000000010000000L),
		A2 (0b0000000000000000000000000000000000000000000000000000000100000000L),
		B2 (0b0000000000000000000000000000000000000000000000000000001000000000L),
		C2 (0b0000000000000000000000000000000000000000000000000000010000000000L),
		D2 (0b0000000000000000000000000000000000000000000000000000100000000000L),
		E2 (0b0000000000000000000000000000000000000000000000000001000000000000L),
		F2 (0b0000000000000000000000000000000000000000000000000010000000000000L),
		G2 (0b0000000000000000000000000000000000000000000000000100000000000000L),
		H2 (0b0000000000000000000000000000000000000000000000001000000000000000L),
		A3 (0b0000000000000000000000000000000000000000000000010000000000000000L),
		B3 (0b0000000000000000000000000000000000000000000000100000000000000000L),
		C3 (0b0000000000000000000000000000000000000000000001000000000000000000L),
		D3 (0b0000000000000000000000000000000000000000000010000000000000000000L),
		E3 (0b0000000000000000000000000000000000000000000100000000000000000000L),
		F3 (0b0000000000000000000000000000000000000000001000000000000000000000L),
		G3 (0b0000000000000000000000000000000000000000010000000000000000000000L),
		H3 (0b0000000000000000000000000000000000000000100000000000000000000000L),
		A4 (0b0000000000000000000000000000000000000001000000000000000000000000L),
		B4 (0b0000000000000000000000000000000000000010000000000000000000000000L),
		C4 (0b0000000000000000000000000000000000000100000000000000000000000000L),
		D4 (0b0000000000000000000000000000000000001000000000000000000000000000L),
		E4 (0b0000000000000000000000000000000000010000000000000000000000000000L),
		F4 (0b0000000000000000000000000000000000100000000000000000000000000000L),
		G4 (0b0000000000000000000000000000000001000000000000000000000000000000L),
		H4 (0b0000000000000000000000000000000010000000000000000000000000000000L),
		A5 (0b0000000000000000000000000000000100000000000000000000000000000000L),
		B5 (0b0000000000000000000000000000001000000000000000000000000000000000L),
		C5 (0b0000000000000000000000000000010000000000000000000000000000000000L),
		D5 (0b0000000000000000000000000000100000000000000000000000000000000000L),
		E5 (0b0000000000000000000000000001000000000000000000000000000000000000L),
		F5 (0b0000000000000000000000000010000000000000000000000000000000000000L),
		G5 (0b0000000000000000000000000100000000000000000000000000000000000000L),
		H5 (0b0000000000000000000000001000000000000000000000000000000000000000L),
		A6 (0b0000000000000000000000010000000000000000000000000000000000000000L),
		B6 (0b0000000000000000000000100000000000000000000000000000000000000000L),
		C6 (0b0000000000000000000001000000000000000000000000000000000000000000L),
		D6 (0b0000000000000000000010000000000000000000000000000000000000000000L),
		E6 (0b0000000000000000000100000000000000000000000000000000000000000000L),
		F6 (0b0000000000000000001000000000000000000000000000000000000000000000L),
		G6 (0b0000000000000000010000000000000000000000000000000000000000000000L),
		H6 (0b0000000000000000100000000000000000000000000000000000000000000000L),
		A7 (0b0000000000000001000000000000000000000000000000000000000000000000L),
		B7 (0b0000000000000010000000000000000000000000000000000000000000000000L),
		C7 (0b0000000000000100000000000000000000000000000000000000000000000000L),
		D7 (0b0000000000001000000000000000000000000000000000000000000000000000L),
		E7 (0b0000000000010000000000000000000000000000000000000000000000000000L),
		F7 (0b0000000000100000000000000000000000000000000000000000000000000000L),
		G7 (0b0000000001000000000000000000000000000000000000000000000000000000L),
		H7 (0b0000000010000000000000000000000000000000000000000000000000000000L),
		A8 (0b0000000100000000000000000000000000000000000000000000000000000000L),
		B8 (0b0000001000000000000000000000000000000000000000000000000000000000L),
		C8 (0b0000010000000000000000000000000000000000000000000000000000000000L),
		D8 (0b0000100000000000000000000000000000000000000000000000000000000000L),
		E8 (0b0001000000000000000000000000000000000000000000000000000000000000L),
		F8 (0b0010000000000000000000000000000000000000000000000000000000000000L),
		G8 (0b0100000000000000000000000000000000000000000000000000000000000000L),
		H8 (0b1000000000000000000000000000000000000000000000000000000000000000L);
		
		final long bitmap;
		
		private Square(long bitmap) {
			this.bitmap = bitmap;
		}
		public long getBitmap() {
			return this.bitmap;
		}
		/**Prints the code for the switch statement based {@link #getByIndex(int) getByIndex} method.*/
		public final static void printCodeGetByIndex() {
			int ind = 0;
			System.out.println("public static Square getByIndex(int sqrInd) {\n" +
									"\tswitch(sqrInd) {");
			for (Square sqr : Square.values()) {
				System.out.println("\t\tcase " + ind + ":\n" +
										"\t\t\treturn " + sqr + ";");
				ind++;
			}
			System.out.println("\t\tdefault:\n" +
									"\t\t\tthrow new IllegalArgumentException(\"Invalid square index.\");\n" +
							"\t}\n" +
						"}");
		}
		/**@return a Square enum.*/
		public static Square getByIndex(int sqrInd) {
			switch(sqrInd) {
				case 0:
					return A1;
				case 1:
					return B1;
				case 2:
					return C1;
				case 3:
					return D1;
				case 4:
					return E1;
				case 5:
					return F1;
				case 6:
					return G1;
				case 7:
					return H1;
				case 8:
					return A2;
				case 9:
					return B2;
				case 10:
					return C2;
				case 11:
					return D2;
				case 12:
					return E2;
				case 13:
					return F2;
				case 14:
					return G2;
				case 15:
					return H2;
				case 16:
					return A3;
				case 17:
					return B3;
				case 18:
					return C3;
				case 19:
					return D3;
				case 20:
					return E3;
				case 21:
					return F3;
				case 22:
					return G3;
				case 23:
					return H3;
				case 24:
					return A4;
				case 25:
					return B4;
				case 26:
					return C4;
				case 27:
					return D4;
				case 28:
					return E4;
				case 29:
					return F4;
				case 30:
					return G4;
				case 31:
					return H4;
				case 32:
					return A5;
				case 33:
					return B5;
				case 34:
					return C5;
				case 35:
					return D5;
				case 36:
					return E5;
				case 37:
					return F5;
				case 38:
					return G5;
				case 39:
					return H5;
				case 40:
					return A6;
				case 41:
					return B6;
				case 42:
					return C6;
				case 43:
					return D6;
				case 44:
					return E6;
				case 45:
					return F6;
				case 46:
					return G6;
				case 47:
					return H6;
				case 48:
					return A7;
				case 49:
					return B7;
				case 50:
					return C7;
				case 51:
					return D7;
				case 52:
					return E7;
				case 53:
					return F7;
				case 54:
					return G7;
				case 55:
					return H7;
				case 56:
					return A8;
				case 57:
					return B8;
				case 58:
					return C8;
				case 59:
					return D8;
				case 60:
					return E8;
				case 61:
					return F8;
				case 62:
					return G8;
				case 63:
					return H8;
				default:
					throw new IllegalArgumentException("Invalid square index.");
			}
		}
		/**Prints the code for the switch statement based {@link #getBitmapByIndex(int) getBitmapByIndex} method.*/
		public final static void printCodeGetBitmapByIndex() {
			int ind = 0;
			System.out.println("public static long getByIndex(int sqrInd) {\n" +
									"\tswitch(sqrInd) {");
			for (Square sqr : Square.values()) {
				System.out.println("\t\tcase " + ind + ":\n" +
										"\t\t\treturn " + Board.toBinaryLiteral(sqr.getBitmap()) + ";");
				ind++;
			}
			System.out.println("\t\tdefault:\n" +
									"\t\t\tthrow new IllegalArgumentException(\"Invalid square index.\");\n" +
							"\t}\n" +
						"}");
		}
		/**@return a long with only the selected square set.*/
		public static long getBitmapByIndex(int sqrInd) {
			switch(sqrInd) {
				case 0:
					return 0b0000000000000000000000000000000000000000000000000000000000000001L;
				case 1:
					return 0b0000000000000000000000000000000000000000000000000000000000000010L;
				case 2:
					return 0b0000000000000000000000000000000000000000000000000000000000000100L;
				case 3:
					return 0b0000000000000000000000000000000000000000000000000000000000001000L;
				case 4:
					return 0b0000000000000000000000000000000000000000000000000000000000010000L;
				case 5:
					return 0b0000000000000000000000000000000000000000000000000000000000100000L;
				case 6:
					return 0b0000000000000000000000000000000000000000000000000000000001000000L;
				case 7:
					return 0b0000000000000000000000000000000000000000000000000000000010000000L;
				case 8:
					return 0b0000000000000000000000000000000000000000000000000000000100000000L;
				case 9:
					return 0b0000000000000000000000000000000000000000000000000000001000000000L;
				case 10:
					return 0b0000000000000000000000000000000000000000000000000000010000000000L;
				case 11:
					return 0b0000000000000000000000000000000000000000000000000000100000000000L;
				case 12:
					return 0b0000000000000000000000000000000000000000000000000001000000000000L;
				case 13:
					return 0b0000000000000000000000000000000000000000000000000010000000000000L;
				case 14:
					return 0b0000000000000000000000000000000000000000000000000100000000000000L;
				case 15:
					return 0b0000000000000000000000000000000000000000000000001000000000000000L;
				case 16:
					return 0b0000000000000000000000000000000000000000000000010000000000000000L;
				case 17:
					return 0b0000000000000000000000000000000000000000000000100000000000000000L;
				case 18:
					return 0b0000000000000000000000000000000000000000000001000000000000000000L;
				case 19:
					return 0b0000000000000000000000000000000000000000000010000000000000000000L;
				case 20:
					return 0b0000000000000000000000000000000000000000000100000000000000000000L;
				case 21:
					return 0b0000000000000000000000000000000000000000001000000000000000000000L;
				case 22:
					return 0b0000000000000000000000000000000000000000010000000000000000000000L;
				case 23:
					return 0b0000000000000000000000000000000000000000100000000000000000000000L;
				case 24:
					return 0b0000000000000000000000000000000000000001000000000000000000000000L;
				case 25:
					return 0b0000000000000000000000000000000000000010000000000000000000000000L;
				case 26:
					return 0b0000000000000000000000000000000000000100000000000000000000000000L;
				case 27:
					return 0b0000000000000000000000000000000000001000000000000000000000000000L;
				case 28:
					return 0b0000000000000000000000000000000000010000000000000000000000000000L;
				case 29:
					return 0b0000000000000000000000000000000000100000000000000000000000000000L;
				case 30:
					return 0b0000000000000000000000000000000001000000000000000000000000000000L;
				case 31:
					return 0b0000000000000000000000000000000010000000000000000000000000000000L;
				case 32:
					return 0b0000000000000000000000000000000100000000000000000000000000000000L;
				case 33:
					return 0b0000000000000000000000000000001000000000000000000000000000000000L;
				case 34:
					return 0b0000000000000000000000000000010000000000000000000000000000000000L;
				case 35:
					return 0b0000000000000000000000000000100000000000000000000000000000000000L;
				case 36:
					return 0b0000000000000000000000000001000000000000000000000000000000000000L;
				case 37:
					return 0b0000000000000000000000000010000000000000000000000000000000000000L;
				case 38:
					return 0b0000000000000000000000000100000000000000000000000000000000000000L;
				case 39:
					return 0b0000000000000000000000001000000000000000000000000000000000000000L;
				case 40:
					return 0b0000000000000000000000010000000000000000000000000000000000000000L;
				case 41:
					return 0b0000000000000000000000100000000000000000000000000000000000000000L;
				case 42:
					return 0b0000000000000000000001000000000000000000000000000000000000000000L;
				case 43:
					return 0b0000000000000000000010000000000000000000000000000000000000000000L;
				case 44:
					return 0b0000000000000000000100000000000000000000000000000000000000000000L;
				case 45:
					return 0b0000000000000000001000000000000000000000000000000000000000000000L;
				case 46:
					return 0b0000000000000000010000000000000000000000000000000000000000000000L;
				case 47:
					return 0b0000000000000000100000000000000000000000000000000000000000000000L;
				case 48:
					return 0b0000000000000001000000000000000000000000000000000000000000000000L;
				case 49:
					return 0b0000000000000010000000000000000000000000000000000000000000000000L;
				case 50:
					return 0b0000000000000100000000000000000000000000000000000000000000000000L;
				case 51:
					return 0b0000000000001000000000000000000000000000000000000000000000000000L;
				case 52:
					return 0b0000000000010000000000000000000000000000000000000000000000000000L;
				case 53:
					return 0b0000000000100000000000000000000000000000000000000000000000000000L;
				case 54:
					return 0b0000000001000000000000000000000000000000000000000000000000000000L;
				case 55:
					return 0b0000000010000000000000000000000000000000000000000000000000000000L;
				case 56:
					return 0b0000000100000000000000000000000000000000000000000000000000000000L;
				case 57:
					return 0b0000001000000000000000000000000000000000000000000000000000000000L;
				case 58:
					return 0b0000010000000000000000000000000000000000000000000000000000000000L;
				case 59:
					return 0b0000100000000000000000000000000000000000000000000000000000000000L;
				case 60:
					return 0b0001000000000000000000000000000000000000000000000000000000000000L;
				case 61:
					return 0b0010000000000000000000000000000000000000000000000000000000000000L;
				case 62:
					return 0b0100000000000000000000000000000000000000000000000000000000000000L;
				case 63:
					return 0b1000000000000000000000000000000000000000000000000000000000000000L;
				default:
					throw new IllegalArgumentException("Invalid square index.");
			}
		}
	}
	
	/**An enum type for the 8 ranks/rows of a chess board. Each constant has a field that contains a long with only the byte on the rank's
	 * index set.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum Rank {
		
		R1 (0b0000000000000000000000000000000000000000000000000000000011111111L),
		R2 (0b0000000000000000000000000000000000000000000000001111111100000000L),
		R3 (0b0000000000000000000000000000000000000000111111110000000000000000L),
		R4 (0b0000000000000000000000000000000011111111000000000000000000000000L),
		R5 (0b0000000000000000000000001111111100000000000000000000000000000000L),
		R6 (0b0000000000000000111111110000000000000000000000000000000000000000L),
		R7 (0b0000000011111111000000000000000000000000000000000000000000000000L),
		R8 (0b1111111100000000000000000000000000000000000000000000000000000000L);
		
		final long bitmap;
		
		private Rank(long bitmap) {
			this.bitmap = bitmap;
		}
		public long getBitmap() {
			return this.bitmap;
		}
		/**Prints the code for the switch statement based {@link #getByIndex(int) getByIndex} method.*/
		public final static void printCodeGetByIndex() {
			int ind = 0;
			System.out.println("public static long getByIndex(int rnkInd) {\n" +
									"\tswitch(rnkInd) {");
			for (Rank rnk : Rank.values()) {
				System.out.println("\t\tcase " + ind + ":\n" +
										"\t\t\treturn " + Board.toBinaryLiteral(rnk.getBitmap()) + ";");
				ind++;
			}
			System.out.println("\t\tdefault:\n" +
									"\t\t\tthrow new IllegalArgumentException(\"Invalid rank index.\");\n" +
							"\t}\n" +
						"}");
		}
		/**Returns a the numeric representation of a rank of the chess board with only the byte on the rank's index set.
		 * 
		 * @param rnkInd the index of the rank*/
		public static long getByIndex(int rnkInd) {
			switch(rnkInd) {
				case 0:
					return 0b0000000000000000000000000000000000000000000000000000000011111111L;
				case 1:
					return 0b0000000000000000000000000000000000000000000000001111111100000000L;
				case 2:
					return 0b0000000000000000000000000000000000000000111111110000000000000000L;
				case 3:
					return 0b0000000000000000000000000000000011111111000000000000000000000000L;
				case 4:
					return 0b0000000000000000000000001111111100000000000000000000000000000000L;
				case 5:
					return 0b0000000000000000111111110000000000000000000000000000000000000000L;
				case 6:
					return 0b0000000011111111000000000000000000000000000000000000000000000000L;
				case 7:
					return 0b1111111100000000000000000000000000000000000000000000000000000000L;
				default:
					throw new IllegalArgumentException("Invalid rank index.");
			}
		}
		/**Returns a the numeric representation of the rank of the chess board on which the input parameter square lies with only
		 * the byte on the rank's index set.
		 * 
		 * @param sqr a Square enum*/
		public static long getBySquare(Square sqr) {
			return getByIndex(sqr.ordinal() >>> 3);
		}
		/**Returns a the numeric representation of the rank of the chess board on which the input parameter square lies with only
		 * the byte on the rank's index set.
		 * 
		 * @param sqrInd the index of the square*/
		public static long getBySquareIndex(int sqrInd) {
			return getByIndex(sqrInd >>> 3);
		}
	}

	/**An enum type for the 8 files/columns of a chess board. Each constant has a field that contains a long with only the bits falling on the
	 * file set.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum File {
		
		A (0b0000000100000001000000010000000100000001000000010000000100000001L),
		B (0b0000001000000010000000100000001000000010000000100000001000000010L),
		C (0b0000010000000100000001000000010000000100000001000000010000000100L),
		D (0b0000100000001000000010000000100000001000000010000000100000001000L),
		E (0b0001000000010000000100000001000000010000000100000001000000010000L),
		F (0b0010000000100000001000000010000000100000001000000010000000100000L),
		G (0b0100000001000000010000000100000001000000010000000100000001000000L),
		H (0b1000000010000000100000001000000010000000100000001000000010000000L);
		
		final long bitmap;
		
		private File(long bitmap) {
			this.bitmap = bitmap;
		}
		public long getBitmap() {
			return this.bitmap;
		}
		/**Prints the code for the switch statement based {@link #getByIndex(int) getByIndex} method.*/
		public final static void printCodeGetByIndex() {
			int ind = 0;
			System.out.println("public static long getByIndex(int fileInd) {\n" +
									"\tswitch(fileInd) {");
			for (File file : File.values()) {
				System.out.println("\t\tcase " + ind + ":\n" +
										"\t\t\treturn " + Board.toBinaryLiteral(file.getBitmap()) + ";");
				ind++;
			}
			System.out.println("\t\tdefault:\n" +
									"\t\t\tthrow new IllegalArgumentException(\"Invalid file index.\");\n" +
							"\t}\n" +
						"}");
		}
		/**Returns a the numeric representation of a file of the chess board with only the bits falling on the specified file set.
		 * 
		 * @param fileInd the index of the file*/
		public static long getByIndex(int fileInd) {
			switch(fileInd) {
				case 0:
					return 0b0000000100000001000000010000000100000001000000010000000100000001L;
				case 1:
					return 0b0000001000000010000000100000001000000010000000100000001000000010L;
				case 2:
					return 0b0000010000000100000001000000010000000100000001000000010000000100L;
				case 3:
					return 0b0000100000001000000010000000100000001000000010000000100000001000L;
				case 4:
					return 0b0001000000010000000100000001000000010000000100000001000000010000L;
				case 5:
					return 0b0010000000100000001000000010000000100000001000000010000000100000L;
				case 6:
					return 0b0100000001000000010000000100000001000000010000000100000001000000L;
				case 7:
					return 0b1000000010000000100000001000000010000000100000001000000010000000L;
				default:
					throw new IllegalArgumentException("Invalid file index.");
			}
		}
		/**Returns a the numeric representation of the file of the chess board on which the input parameter square lies with only
		 * the relevant bits set.
		 * 
		 * @param sqr a Square enum*/
		public static long getBySquare(Square sqr) {
			return getByIndex(sqr.ordinal() & 7);
		}
		/**Returns a the numeric representation of the file of the chess board on which the input parameter square lies with only
		 * the relevant bits set.
		 * 
		 * @param sqrInd the index of the square*/
		public static long getBySquareIndex(int sqrInd) {
			return getByIndex(sqrInd & 7);
		}
	}
	
	/**An enum type for the 15 diagonals of a chess board. Each constant has a field that contains a long with only the bits on indexes
	 * of the squares falling on the diagonal set.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum Diagonal {
		
		DG1  (0b0000000000000000000000000000000000000000000000000000000000000001L),
		DG2  (0b0000000000000000000000000000000000000000000000000000000100000010L),
		DG3  (0b0000000000000000000000000000000000000000000000010000001000000100L),
		DG4  (0b0000000000000000000000000000000000000001000000100000010000001000L),
		DG5  (0b0000000000000000000000000000000100000010000001000000100000010000L),
		DG6  (0b0000000000000000000000010000001000000100000010000001000000100000L),
		DG7  (0b0000000000000001000000100000010000001000000100000010000001000000L),
		DG8  (0b0000000100000010000001000000100000010000001000000100000010000000L),
		DG9  (0b0000001000000100000010000001000000100000010000001000000000000000L),
		DG10 (0b0000010000001000000100000010000001000000100000000000000000000000L),
		DG11 (0b0000100000010000001000000100000010000000000000000000000000000000L),
		DG12 (0b0001000000100000010000001000000000000000000000000000000000000000L),
		DG13 (0b0010000001000000100000000000000000000000000000000000000000000000L),
		DG14 (0b0100000010000000000000000000000000000000000000000000000000000000L),
		DG15 (0b1000000000000000000000000000000000000000000000000000000000000000L);
		
		final long bitmap;
		
		private Diagonal(long bitmap) {
			this.bitmap = bitmap;
		}
		public long getBitmap() {
			return this.bitmap;
		}
		/**Prints the binary literals for the enum constants so they can be hard-coded.*/
		public void printBitmapLiterals() {
			long[] aDiag = new long[15];
			aDiag[0]	= Square.A1.getBitmap();
			aDiag[1]	= Square.A2.getBitmap() | Square.B1.getBitmap();
			aDiag[2]	= Square.A3.getBitmap() | Square.B2.getBitmap() | Square.C1.getBitmap();
			aDiag[3]	= Square.A4.getBitmap() | Square.B3.getBitmap() | Square.C2.getBitmap() | Square.D1.getBitmap();
			aDiag[4]	= Square.A5.getBitmap() | Square.B4.getBitmap() | Square.C3.getBitmap() | Square.D2.getBitmap() | Square.E1.getBitmap();
			aDiag[5]	= Square.A6.getBitmap() | Square.B5.getBitmap() | Square.C4.getBitmap() | Square.D3.getBitmap() | Square.E2.getBitmap() | Square.F1.getBitmap();
			aDiag[6]	= Square.A7.getBitmap() | Square.B6.getBitmap() | Square.C5.getBitmap() | Square.D4.getBitmap() | Square.E3.getBitmap() | Square.F2.getBitmap() | Square.G1.getBitmap();
			aDiag[7] 	= Square.A8.getBitmap() | Square.B7.getBitmap() | Square.C6.getBitmap() | Square.D5.getBitmap() | Square.E4.getBitmap() | Square.F3.getBitmap() | Square.G2.getBitmap() | Square.H1.getBitmap();
			aDiag[8] 	= Square.B8.getBitmap() | Square.C7.getBitmap() | Square.D6.getBitmap() | Square.E5.getBitmap() | Square.F4.getBitmap() | Square.G3.getBitmap() | Square.H2.getBitmap();
			aDiag[9]	= Square.C8.getBitmap() | Square.D7.getBitmap() | Square.E6.getBitmap() | Square.F5.getBitmap() | Square.G4.getBitmap() | Square.H3.getBitmap();
			aDiag[10] 	= Square.D8.getBitmap() | Square.E7.getBitmap() | Square.F6.getBitmap() | Square.G5.getBitmap() | Square.H4.getBitmap();
			aDiag[11] 	= Square.E8.getBitmap() | Square.F7.getBitmap() | Square.G6.getBitmap() | Square.H5.getBitmap();
			aDiag[12] 	= Square.F8.getBitmap() | Square.G7.getBitmap() | Square.H6.getBitmap();
			aDiag[13] 	= Square.G8.getBitmap() | Square.H7.getBitmap();
			aDiag[14]	= Square.H8.getBitmap();
			for (int i = 0; i < aDiag.length; i++) {
				System.out.println("Diagonal " + String.format("%2d", i+1) + ": " + Board.toBinaryLiteral(aDiag[i]));
			}
		}
		/**Prints the code for the switch statement based {@link #getByIndex(int) getByIndex} method.*/
		public final static void printCodeGetByIndex() {
			int ind = 0;
			System.out.println("public static long getByIndex(int dgnInd) {\n" +
									"\tswitch(dgnInd) {");
			for (Diagonal dgn : Diagonal.values()) {
				System.out.println("\t\tcase " + ind + ":\n" +
										"\t\t\treturn " + Board.toBinaryLiteral(dgn.getBitmap()) + ";");
				ind++;
			}
			System.out.println("\t\tdefault:\n" +
									"\t\t\tthrow new IllegalArgumentException(\"Invalid diagonal index.\");\n" +
							"\t}\n" +
						"}");
		}
		/**Returns a the numeric representation of a diagonal of the chess board with only the bits falling on the specified diagonal set.
		 * 
		 * @param dgnInd the index of the diagonal*/
		public static long getByIndex(int dgnInd) {
			switch(dgnInd) {
				case 0:
					return 0b0000000000000000000000000000000000000000000000000000000000000001L;
				case 1:
					return 0b0000000000000000000000000000000000000000000000000000000100000010L;
				case 2:
					return 0b0000000000000000000000000000000000000000000000010000001000000100L;
				case 3:
					return 0b0000000000000000000000000000000000000001000000100000010000001000L;
				case 4:
					return 0b0000000000000000000000000000000100000010000001000000100000010000L;
				case 5:
					return 0b0000000000000000000000010000001000000100000010000001000000100000L;
				case 6:
					return 0b0000000000000001000000100000010000001000000100000010000001000000L;
				case 7:
					return 0b0000000100000010000001000000100000010000001000000100000010000000L;
				case 8:
					return 0b0000001000000100000010000001000000100000010000001000000000000000L;
				case 9:
					return 0b0000010000001000000100000010000001000000100000000000000000000000L;
				case 10:
					return 0b0000100000010000001000000100000010000000000000000000000000000000L;
				case 11:
					return 0b0001000000100000010000001000000000000000000000000000000000000000L;
				case 12:
					return 0b0010000001000000100000000000000000000000000000000000000000000000L;
				case 13:
					return 0b0100000010000000000000000000000000000000000000000000000000000000L;
				case 14:
					return 0b1000000000000000000000000000000000000000000000000000000000000000L;
				default:
					throw new IllegalArgumentException("Invalid diagonal index.");
			}
		}
		/**Returns a the numeric representation of a diagonal of the chess board on which the input parameter square lies with only
		 * the relevant bits set.
		 * 
		 * @param sqr a Square enum*/
		public static long getBySquare(Square sqr) {
			int sqrInd = sqr.ordinal();
			int fileInd = sqrInd & 7;
			return getByIndex(((sqrInd - fileInd) >>> 3) + fileInd);
		}
		/**Returns a the numeric representation of a diagonal of the chess board on which the input parameter square lies with only
		 * the relevant bits set.
		 * 
		 * @param sqrInd the index of a square*/
		public static long getBySquareIndex(int sqrInd) {
			int fileInd = sqrInd & 7;
			return getByIndex(((sqrInd - fileInd) >>> 3) + fileInd);
		}
	}
	
	/**An enum type for the 15 anti-diagonals of a chess board. Each constant has a field that contains a long with only the bits on indexes
	 * of the squares falling on the diagonal set.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum AntiDiagonal {
		
		ADG1  (0b0000000100000000000000000000000000000000000000000000000000000000L),
		ADG2  (0b0000001000000001000000000000000000000000000000000000000000000000L),
		ADG3  (0b0000010000000010000000010000000000000000000000000000000000000000L),
		ADG4  (0b0000100000000100000000100000000100000000000000000000000000000000L),
		ADG5  (0b0001000000001000000001000000001000000001000000000000000000000000L),
		ADG6  (0b0010000000010000000010000000010000000010000000010000000000000000L),
		ADG7  (0b0100000000100000000100000000100000000100000000100000000100000000L),
		ADG8  (0b1000000001000000001000000001000000001000000001000000001000000001L),
		ADG9  (0b0000000010000000010000000010000000010000000010000000010000000010L),
		ADG10 (0b0000000000000000100000000100000000100000000100000000100000000100L),
		ADG11 (0b0000000000000000000000001000000001000000001000000001000000001000L),
		ADG12 (0b0000000000000000000000000000000010000000010000000010000000010000L),
		ADG13 (0b0000000000000000000000000000000000000000100000000100000000100000L),
		ADG14 (0b0000000000000000000000000000000000000000000000001000000001000000L),
		ADG15 (0b0000000000000000000000000000000000000000000000000000000010000000L);
		
		final long bitmap;
		
		private AntiDiagonal(long bitmap) {
			this.bitmap = bitmap;
		}
		public long getBitmap() {
			return this.bitmap;
		}
		/**Prints the binary literals for the enum constants so they can be hard-coded.*/
		public void printBitmapLiterals() {
			long[] diag = new long[15];
			diag[0]		= Square.A8.getBitmap();
			diag[1]		= Square.A7.getBitmap() | Square.B8.getBitmap();
			diag[2]		= Square.A6.getBitmap() | Square.B7.getBitmap() | Square.C8.getBitmap();
			diag[3]		= Square.A5.getBitmap() | Square.B6.getBitmap() | Square.C7.getBitmap() | Square.D8.getBitmap();
			diag[4]		= Square.A4.getBitmap() | Square.B5.getBitmap() | Square.C6.getBitmap() | Square.D7.getBitmap() | Square.E8.getBitmap();
			diag[5]		= Square.A3.getBitmap() | Square.B4.getBitmap() | Square.C5.getBitmap() | Square.D6.getBitmap() | Square.E7.getBitmap() | Square.F8.getBitmap();
			diag[6]		= Square.A2.getBitmap() | Square.B3.getBitmap() | Square.C4.getBitmap() | Square.D5.getBitmap() | Square.E6.getBitmap() | Square.F7.getBitmap() | Square.G8.getBitmap();
			diag[7] 	= Square.A1.getBitmap() | Square.B2.getBitmap() | Square.C3.getBitmap() | Square.D4.getBitmap() | Square.E5.getBitmap() | Square.F6.getBitmap() | Square.G7.getBitmap() | Square.H8.getBitmap();
			diag[8] 	= Square.B1.getBitmap() | Square.C2.getBitmap() | Square.D3.getBitmap() | Square.E4.getBitmap() | Square.F5.getBitmap() | Square.G6.getBitmap() | Square.H7.getBitmap();
			diag[9]		= Square.C1.getBitmap() | Square.D2.getBitmap() | Square.E3.getBitmap() | Square.F4.getBitmap() | Square.G5.getBitmap() | Square.H6.getBitmap();
			diag[10] 	= Square.D1.getBitmap() | Square.E2.getBitmap() | Square.F3.getBitmap() | Square.G4.getBitmap() | Square.H5.getBitmap();
			diag[11] 	= Square.E1.getBitmap() | Square.F2.getBitmap() | Square.G3.getBitmap() | Square.H4.getBitmap();
			diag[12] 	= Square.F1.getBitmap() | Square.G2.getBitmap() | Square.H3.getBitmap();
			diag[13] 	= Square.G1.getBitmap() | Square.H2.getBitmap();
			diag[14]	= Square.H1.getBitmap();
			for (int i = 0; i < diag.length; i++) {
				System.out.println("Diagonal " + String.format("%2d", i+1) + ": " + Board.toBinaryLiteral(diag[i]));
			}
		}
		/**Prints the code for the switch statement based {@link #getByIndex(int) getByIndex} method.*/
		public final static void printCodeGetByIndex() {
			int ind = 0;
			System.out.println("public static long getByIndex(int adgnInd) {\n" +
									"\tswitch(adgnInd) {");
			for (AntiDiagonal adgn : AntiDiagonal.values()) {
				System.out.println("\t\tcase " + ind + ":\n" +
										"\t\t\treturn " + Board.toBinaryLiteral(adgn.getBitmap()) + ";");
				ind++;
			}
			System.out.println("\t\tdefault:\n" +
									"\t\t\tthrow new IllegalArgumentException(\"Invalid anti-diagonal index.\");\n" +
							"\t}\n" +
						"}");
		}
		/**Returns a the numeric representation of an anti-diagonal of the chess board with only the bits falling on the specified diagonal set.
		 * 
		 * @param adgnInd the index of the anti-diagonal*/
		public static long getByIndex(int adgnInd) {
			switch(adgnInd) {
				case 0:
					return 0b0000000100000000000000000000000000000000000000000000000000000000L;
				case 1:
					return 0b0000001000000001000000000000000000000000000000000000000000000000L;
				case 2:
					return 0b0000010000000010000000010000000000000000000000000000000000000000L;
				case 3:
					return 0b0000100000000100000000100000000100000000000000000000000000000000L;
				case 4:
					return 0b0001000000001000000001000000001000000001000000000000000000000000L;
				case 5:
					return 0b0010000000010000000010000000010000000010000000010000000000000000L;
				case 6:
					return 0b0100000000100000000100000000100000000100000000100000000100000000L;
				case 7:
					return 0b1000000001000000001000000001000000001000000001000000001000000001L;
				case 8:
					return 0b0000000010000000010000000010000000010000000010000000010000000010L;
				case 9:
					return 0b0000000000000000100000000100000000100000000100000000100000000100L;
				case 10:
					return 0b0000000000000000000000001000000001000000001000000001000000001000L;
				case 11:
					return 0b0000000000000000000000000000000010000000010000000010000000010000L;
				case 12:
					return 0b0000000000000000000000000000000000000000100000000100000000100000L;
				case 13:
					return 0b0000000000000000000000000000000000000000000000001000000001000000L;
				case 14:
					return 0b0000000000000000000000000000000000000000000000000000000010000000L;
				default:
					throw new IllegalArgumentException("Invalid anti-diagonal index.");
			}
		}
		/**Returns a the numeric representation of a diagonal of the chess board on which the input parameter square lies with only
		 * the relevant bits set.
		 * 
		 * @param sqr a Square enum*/
		public static long getBySquare(Square sqr) {
			int sqrInd = sqr.ordinal();
			int rightMostSquare = sqrInd | 7;
			return getByIndex(7 - (rightMostSquare >>> 3) + (sqrInd & 7));
		}
		/**Returns a the numeric representation of a diagonal of the chess board on which the input parameter square lies with only
		 * the relevant bits set.
		 * 
		 * @param sqrInd the index of a square*/
		public static long getBySquareIndex(int sqrInd) {
			int rightMostSquare = sqrInd^7;
			return getByIndex(7 - (rightMostSquare >>> 3) + (sqrInd & 7));
		}
	}
	
	/**An enum type for the different chess pieces. Each piece has an initial position and and id number by which it is represented on the
	 * array of the auxiliary offset board.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum Piece {
		
		WHITE_KING		(0b0000000000000000000000000000000000000000000000000000000000010000L, 1),
		WHITE_QUEEN		(0b0000000000000000000000000000000000000000000000000000000000001000L, 2),
		WHITE_ROOK		(0b0000000000000000000000000000000000000000000000000000000010000001L, 3),
		WHITE_BISHOP	(0b0000000000000000000000000000000000000000000000000000000000100100L, 4),
		WHITE_KNIGHT 	(0b0000000000000000000000000000000000000000000000000000000001000010L, 5),
		WHITE_PAWN		(0b0000000000000000000000000000000000000000000000001111111100000000L, 6),
		
		BLACK_KING		(0b0001000000000000000000000000000000000000000000000000000000000000L, 7),
		BLACK_QUEEN		(0b0000100000000000000000000000000000000000000000000000000000000000L, 8),
		BLACK_ROOK		(0b1000000100000000000000000000000000000000000000000000000000000000L, 9),
		BLACK_BISHOP	(0b0010010000000000000000000000000000000000000000000000000000000000L, 10),
		BLACK_KNIGHT	(0b0100001000000000000000000000000000000000000000000000000000000000L, 11),
		BLACK_PAWN		(0b0000000011111111000000000000000000000000000000000000000000000000L, 12);
		
		final long initPosBitmap;
		final int  offsetBoardRep;
		
		private Piece(long initPosBitmap, int offsetBoardRep) {
			this.initPosBitmap = initPosBitmap;
			this.offsetBoardRep = offsetBoardRep;
		}
		public long getInitPosBitmap() {
			return this.initPosBitmap;
		}
		public int getOffsetBoardRep() {
			return this.offsetBoardRep;
		}
	}
	
	public final static class MoveMask {
		
		public final static long generateKingsMoveMask(Square sqr) {
			long mask;
			int sqrInd = sqr.ordinal();
			long sqrBit = sqr.getBitmap();
			mask =	BitOperations.vShiftUpLeft(sqrBit)   | BitOperations.vShiftUp(sqrBit)  	| BitOperations.vShiftUpRight(sqrBit)   |
					BitOperations.vShiftLeft(sqrBit)    									| BitOperations.vShiftRight(sqrBit)     |
					BitOperations.vShiftDownLeft(sqrBit) | BitOperations.vShiftDown(sqrBit) | BitOperations.vShiftDownRight(sqrBit) ;
			if (sqrInd%8 == 0)
				mask &= ~File.H.getBitmap();
			else if ((sqrInd + 1)%8 == 0)
				mask &= ~File.A.getBitmap();
			return mask;
		}
		public final static long generateKnightMasks(Square sqr) {
			long mask;
			int sqrInd = sqr.ordinal();
			long sqrBit = sqr.getBitmap();
			mask =		 	BitOperations.vShiftUpLeft(BitOperations.vShiftUp(sqrBit))	   | BitOperations.vShiftUpRight(BitOperations.vShiftUp(sqrBit)) 	|
					BitOperations.vShiftUpLeft(BitOperations.vShiftLeft(sqrBit))		   | 		  BitOperations.vShiftUpRight(BitOperations.vShiftRight(sqrBit))   |
					BitOperations.vShiftDownLeft(BitOperations.vShiftLeft(sqrBit))	 	   |		  BitOperations.vShiftDownRight(BitOperations.vShiftRight(sqrBit)) |
							BitOperations.vShiftDownLeft(BitOperations.vShiftDown(sqrBit)) | BitOperations.vShiftDownRight(BitOperations.vShiftDown(sqrBit));
			if (sqrInd%8 == 0)
				mask &= ~(File.H.getBitmap() | File.G.getBitmap());
			else if ((sqrInd - 1)%8 == 0)
				mask &= ~File.H.getBitmap();
			else if ((sqrInd + 1)%8 == 0)
				mask &= ~(File.A.getBitmap() | File.B.getBitmap());
			else if ((sqrInd + 2)%8 == 0)
				mask &= ~File.A.getBitmap();
			return mask;
		}
		public final static long generateWhitePawnsCaptureMasks(Square sqr) {
			long mask;
			int sqrInd = sqr.ordinal();
			if (sqrInd < 8 || sqrInd > 55)
					return 0;
			long sqrBit = sqr.getBitmap();
			mask =		BitOperations.vShiftUpLeft(sqrBit)	 | BitOperations.vShiftUpRight(sqrBit);
			if (sqrInd%8 == 0)
				mask &= ~File.H.getBitmap();
			else if ((sqrInd + 1)%8 == 0)
				mask &= ~File.A.getBitmap();
			return mask;
		}
		public final static long generateBlackPawnsCaptureMasks(Square sqr) {
			long mask;
			int sqrInd = sqr.ordinal();
			if (sqrInd < 8 || sqrInd > 55)
					return 0;
			long sqrBit = sqr.getBitmap();
			mask =		BitOperations.vShiftDownLeft(sqrBit) | BitOperations.vShiftDownRight(sqrBit);
			if (sqrInd%8 == 0)
				mask &= ~File.H.getBitmap();
			else if ((sqrInd + 1)%8 == 0)
				mask &= ~File.A.getBitmap();
			return mask;
		}
		public final static long generateWhitePawnsAdvanceMasks(Square sqr) {
			long mask;
			int sqrInd = sqr.ordinal();
			if (sqrInd < 8 || sqrInd > 55)
				return 0;
			if (sqrInd < 16) {
				long sqrBit = sqr.getBitmap();
				mask =  BitOperations.vShiftUp(BitOperations.vShiftUp(sqrBit))	|
						BitOperations.vShiftUp(sqrBit);
				return mask;
			}
			long sqrBit = sqr.getBitmap();
			mask = 		BitOperations.vShiftUp(sqrBit);
			return mask;
		}
		public final static long generateBlackPawnsAdvanceMasks(Square sqr) {
			long mask;
			int sqrInd = sqr.ordinal();
			if (sqrInd < 8 || sqrInd > 55)
				return 0;
			if (sqrInd >= 48) {
				long sqrBit = sqr.getBitmap();
				mask =	BitOperations.vShiftDown(sqrBit) |
						BitOperations.vShiftDown(BitOperations.vShiftDown(sqrBit));
				return mask;
			}
			long sqrBit = sqr.getBitmap();
			mask =		BitOperations.vShiftDown(sqrBit);
			return mask;
		}
		public final static long generateRooksRankMoveMask(Square sqr) {
			long mask;
			mask =	(Rank.getBySquare(sqr)^sqr.getBitmap());
			return mask;
		}
		public final static long generateRooksFileMoveMask(Square sqr) {
			long mask;
			mask =	(File.getBySquare(sqr)^sqr.getBitmap());
			return mask;
		}
		public final static long generateRooksCompleteMoveMask(Square sqr) {
			long mask;
			mask = generateRooksRankMoveMask(sqr) | generateRooksFileMoveMask(sqr);
			return mask;
		}
		public final static long generateBishopsDiagonalMoveMask(Square sqr) {
			long mask;
			mask = (Diagonal.getBySquare(sqr)^sqr.getBitmap());
			return mask;
		}
		public final static long generateBishopsAntiDiagonalMoveMask(Square sqr) {
			long mask;
			mask = (AntiDiagonal.getBySquare(sqr)^sqr.getBitmap());
			return mask;
		}
		public final static long generateBishopsCompleteMoveMask(Square sqr) {
			long mask;
			mask = generateBishopsDiagonalMoveMask(sqr) | generateBishopsAntiDiagonalMoveMask(sqr);
			return mask;
		}
		public final static long generateQueensRankMoveMask(Square sqr) {
			return generateRooksRankMoveMask(sqr);
		}
		public final static long generateQueensFileMoveMask(Square sqr) {
			return generateRooksFileMoveMask(sqr);
		}
		public final static long generateQueensDiagonalMoveMask(Square sqr) {
			return generateBishopsDiagonalMoveMask(sqr);
		}
		public final static long generateQueensAntiDiagonalMoveMask(Square sqr) {
			return generateBishopsAntiDiagonalMoveMask(sqr);
		}
		public final static long generateQueensCompleteMoveMask(Square sqr) {
			long mask;
			mask = generateRooksCompleteMoveMask(sqr) | generateBishopsCompleteMoveMask(sqr);
			return mask;
		}
	}
	
	public static enum SliderAttackRayMask {
		
		A1 (Square.A1),
		B1 (Square.B1),
		C1 (Square.C1),
		D1 (Square.D1),
		E1 (Square.E1),
		F1 (Square.F1),
		G1 (Square.G1),
		H1 (Square.H1),
		A2 (Square.A2),
		B2 (Square.B2),
		C2 (Square.C2),
		D2 (Square.D2),
		E2 (Square.E2),
		F2 (Square.F2),
		G2 (Square.G2),
		H2 (Square.H2),
		A3 (Square.A3),
		B3 (Square.B3),
		C3 (Square.C3),
		D3 (Square.D3),
		E3 (Square.E3),
		F3 (Square.F3),
		G3 (Square.G3),
		H3 (Square.H3),
		A4 (Square.A4),
		B4 (Square.B4),
		C4 (Square.C4),
		D4 (Square.D4),
		E4 (Square.E4),
		F4 (Square.F4),
		G4 (Square.G4),
		H4 (Square.H4),
		A5 (Square.A5),
		B5 (Square.B5),
		C5 (Square.C5),
		D5 (Square.D5),
		E5 (Square.E5),
		F5 (Square.F5),
		G5 (Square.G5),
		H5 (Square.H5),
		A6 (Square.A6),
		B6 (Square.B6),
		C6 (Square.C6),
		D6 (Square.D6),
		E6 (Square.E6),
		F6 (Square.F6),
		G6 (Square.G6),
		H6 (Square.H6),
		A7 (Square.A7),
		B7 (Square.B7),
		C7 (Square.C7),
		D7 (Square.D7),
		E7 (Square.E7),
		F7 (Square.F7),
		G7 (Square.G7),
		H7 (Square.H7),
		A8 (Square.A8),
		B8 (Square.B8),
		C8 (Square.C8),
		D8 (Square.D8),
		E8 (Square.E8),
		F8 (Square.F8),
		G8 (Square.G8),
		H8 (Square.H8);
		
		final long rankPos;
		final long rankNeg;
		final long filePos;
		final long fileNeg;
		final long diagonalPos;
		final long diagonalNeg;
		final long antiDiagonalPos;
		final long antiDiagonalNeg;
		
		private SliderAttackRayMask(Square sqr) {
			int sqrInd = sqr.ordinal();
			long sqrBit = sqr.getBitmap();
			long rank = Rank.getBySquareIndex(sqrInd);
			long file = File.getBySquareIndex(sqrInd);
			long diagonal = Diagonal.getBySquareIndex(sqrInd);
			long antiDiagonal = AntiDiagonal.getBySquareIndex(sqrInd);
			this.rankPos = rank & ~((sqrBit << 1) - 1);
			this.rankNeg = rank & (sqrBit - 1);
			this.filePos = file & ~((sqrBit << 1) - 1);
			this.fileNeg = file & (sqrBit - 1);
			this.diagonalPos = diagonal & ~((sqrBit << 1) - 1);
			this.diagonalNeg = diagonal & (sqrBit - 1);
			this.antiDiagonalPos = antiDiagonal & ~((sqrBit << 1) - 1);
			this.antiDiagonalNeg = antiDiagonal & (sqrBit - 1);
		}
		public long getRankPos() {
			return this.rankPos;
		}
		public long getRankNeg() {
			return this.rankNeg;
		}
		public long getFilePos() {
			return this.filePos;
		}
		public long getFileNeg() {
			return this.fileNeg;
		}
		public long getDiagonalPos() {
			return this.diagonalPos;
		}
		public long getDiagonalNeg() {
			return this.diagonalNeg;
		}
		public long getAntiDiagonalPos() {
			return this.antiDiagonalPos;
		}
		public long getAntiDiagonalNeg() {
			return this.antiDiagonalNeg;
		}
		public static SliderAttackRayMask getByIndex(int sqrInd) {
			switch(sqrInd) {
				case 0:
					return A1;
				case 1:
					return B1;
				case 2:
					return C1;
				case 3:
					return D1;
				case 4:
					return E1;
				case 5:
					return F1;
				case 6:
					return G1;
				case 7:
					return H1;
				case 8:
					return A2;
				case 9:
					return B2;
				case 10:
					return C2;
				case 11:
					return D2;
				case 12:
					return E2;
				case 13:
					return F2;
				case 14:
					return G2;
				case 15:
					return H2;
				case 16:
					return A3;
				case 17:
					return B3;
				case 18:
					return C3;
				case 19:
					return D3;
				case 20:
					return E3;
				case 21:
					return F3;
				case 22:
					return G3;
				case 23:
					return H3;
				case 24:
					return A4;
				case 25:
					return B4;
				case 26:
					return C4;
				case 27:
					return D4;
				case 28:
					return E4;
				case 29:
					return F4;
				case 30:
					return G4;
				case 31:
					return H4;
				case 32:
					return A5;
				case 33:
					return B5;
				case 34:
					return C5;
				case 35:
					return D5;
				case 36:
					return E5;
				case 37:
					return F5;
				case 38:
					return G5;
				case 39:
					return H5;
				case 40:
					return A6;
				case 41:
					return B6;
				case 42:
					return C6;
				case 43:
					return D6;
				case 44:
					return E6;
				case 45:
					return F6;
				case 46:
					return G6;
				case 47:
					return H6;
				case 48:
					return A7;
				case 49:
					return B7;
				case 50:
					return C7;
				case 51:
					return D7;
				case 52:
					return E7;
				case 53:
					return F7;
				case 54:
					return G7;
				case 55:
					return H7;
				case 56:
					return A8;
				case 57:
					return B8;
				case 58:
					return C8;
				case 59:
					return D8;
				case 60:
					return E8;
				case 61:
					return F8;
				case 62:
					return G8;
				case 63:
					return H8;
				default:
					throw new IllegalArgumentException("Invalid square index.");
			}
		}
	}
	
	public static enum SliderOccupancyMask {
		
		A1 (Square.A1),
		B1 (Square.B1),
		C1 (Square.C1),
		D1 (Square.D1),
		E1 (Square.E1),
		F1 (Square.F1),
		G1 (Square.G1),
		H1 (Square.H1),
		A2 (Square.A2),
		B2 (Square.B2),
		C2 (Square.C2),
		D2 (Square.D2),
		E2 (Square.E2),
		F2 (Square.F2),
		G2 (Square.G2),
		H2 (Square.H2),
		A3 (Square.A3),
		B3 (Square.B3),
		C3 (Square.C3),
		D3 (Square.D3),
		E3 (Square.E3),
		F3 (Square.F3),
		G3 (Square.G3),
		H3 (Square.H3),
		A4 (Square.A4),
		B4 (Square.B4),
		C4 (Square.C4),
		D4 (Square.D4),
		E4 (Square.E4),
		F4 (Square.F4),
		G4 (Square.G4),
		H4 (Square.H4),
		A5 (Square.A5),
		B5 (Square.B5),
		C5 (Square.C5),
		D5 (Square.D5),
		E5 (Square.E5),
		F5 (Square.F5),
		G5 (Square.G5),
		H5 (Square.H5),
		A6 (Square.A6),
		B6 (Square.B6),
		C6 (Square.C6),
		D6 (Square.D6),
		E6 (Square.E6),
		F6 (Square.F6),
		G6 (Square.G6),
		H6 (Square.H6),
		A7 (Square.A7),
		B7 (Square.B7),
		C7 (Square.C7),
		D7 (Square.D7),
		E7 (Square.E7),
		F7 (Square.F7),
		G7 (Square.G7),
		H7 (Square.H7),
		A8 (Square.A8),
		B8 (Square.B8),
		C8 (Square.C8),
		D8 (Square.D8),
		E8 (Square.E8),
		F8 (Square.F8),
		G8 (Square.G8),
		H8 (Square.H8);
		
		final long rookOccupancyMask;
		final long bishopOccupancyMask;
		
		final byte rookOccupancyMaskBitCount;
		final byte bishopOccupancyMaskBitCount;
		
		private static long ANTIFRAME_VERTICAL;
		private static long ANTIFRAME_HORIZONTAL;
		private static long ANTIFRAME;
		
		private SliderOccupancyMask(Square sqr) {
			this.initializeAntiFrames();
			this.rookOccupancyMask = generateRooksCompleteOccupancyMask(sqr);
			this.bishopOccupancyMask = generateBishopsCompleteOccupancyMask(sqr);
			this.rookOccupancyMaskBitCount = (byte)BitOperations.getCardinality(this.rookOccupancyMask);
			this.bishopOccupancyMaskBitCount = (byte)BitOperations.getCardinality(this.bishopOccupancyMask);
		}
		public long getRookOccupancyMask() {
			return this.rookOccupancyMask;
		}
		public long getBishopOccupancyMask() {
			return this.bishopOccupancyMask;
		}
		public byte getRookOccupancyMaskBitCount() {
			return this.rookOccupancyMaskBitCount;
		}
		public byte getBishopOccupancyMaskBitCount() {
			return this.bishopOccupancyMaskBitCount;
		}
		private void initializeAntiFrames() {
			if (ANTIFRAME == 0) {
				ANTIFRAME_VERTICAL 		= ~(File.A.getBitmap() 	| File.H.getBitmap());
				ANTIFRAME_HORIZONTAL	= ~(Rank.R1.getBitmap() | Rank.R8.getBitmap());
				ANTIFRAME				=  (ANTIFRAME_VERTICAL	& ANTIFRAME_HORIZONTAL);
			}
		}
		private static long generateRooksRankOccupancyMask(Square sqr) {
			return (MoveMask.generateRooksRankMoveMask(sqr) & ANTIFRAME_VERTICAL);
		}
		private static long generateRooksFileOccupancyMask(Square sqr) {
			return (MoveMask.generateRooksFileMoveMask(sqr) & ANTIFRAME_HORIZONTAL);
		}
		private static long generateRooksCompleteOccupancyMask(Square sqr) {
			return (generateRooksRankOccupancyMask(sqr) | generateRooksFileOccupancyMask(sqr));
		}
		private static long generateBishopsDiagonalOccupancyMask(Square sqr) {
			return (MoveMask.generateBishopsDiagonalMoveMask(sqr) & ANTIFRAME);
		}
		private static long generateBishopsAntiDiagonalOccupancyMask(Square sqr) {
			return (MoveMask.generateBishopsAntiDiagonalMoveMask(sqr) & ANTIFRAME);
		}
		private static long generateBishopsCompleteOccupancyMask(Square sqr) {
			return (generateBishopsDiagonalOccupancyMask(sqr) | generateBishopsAntiDiagonalOccupancyMask(sqr));
		}
		public static void printCodeVariables() {
			for (int i = 0; i < 63; i++)
				System.out.println(Square.getByIndex(i) + " (Square." + Square.getByIndex(i) + "),");
			System.out.println(Square.getByIndex(63) + " (Square." + Square.getByIndex(63) + ");");
		}
		public static SliderOccupancyMask getByIndex(int sqrInd) {
			switch(sqrInd) {
				case 0:
					return A1;
				case 1:
					return B1;
				case 2:
					return C1;
				case 3:
					return D1;
				case 4:
					return E1;
				case 5:
					return F1;
				case 6:
					return G1;
				case 7:
					return H1;
				case 8:
					return A2;
				case 9:
					return B2;
				case 10:
					return C2;
				case 11:
					return D2;
				case 12:
					return E2;
				case 13:
					return F2;
				case 14:
					return G2;
				case 15:
					return H2;
				case 16:
					return A3;
				case 17:
					return B3;
				case 18:
					return C3;
				case 19:
					return D3;
				case 20:
					return E3;
				case 21:
					return F3;
				case 22:
					return G3;
				case 23:
					return H3;
				case 24:
					return A4;
				case 25:
					return B4;
				case 26:
					return C4;
				case 27:
					return D4;
				case 28:
					return E4;
				case 29:
					return F4;
				case 30:
					return G4;
				case 31:
					return H4;
				case 32:
					return A5;
				case 33:
					return B5;
				case 34:
					return C5;
				case 35:
					return D5;
				case 36:
					return E5;
				case 37:
					return F5;
				case 38:
					return G5;
				case 39:
					return H5;
				case 40:
					return A6;
				case 41:
					return B6;
				case 42:
					return C6;
				case 43:
					return D6;
				case 44:
					return E6;
				case 45:
					return F6;
				case 46:
					return G6;
				case 47:
					return H6;
				case 48:
					return A7;
				case 49:
					return B7;
				case 50:
					return C7;
				case 51:
					return D7;
				case 52:
					return E7;
				case 53:
					return F7;
				case 54:
					return G7;
				case 55:
					return H7;
				case 56:
					return A8;
				case 57:
					return B8;
				case 58:
					return C8;
				case 59:
					return D8;
				case 60:
					return E8;
				case 61:
					return F8;
				case 62:
					return G8;
				case 63:
					return H8;
				default:
					throw new IllegalArgumentException("Invalid square index.");
			}
		}
	}
	
	public static class SliderOccupancyVariations {
		
		private final static long[][] rookOccupancyVariations	= initializeRookOccupancyVariations();
		private final static long[][] bishopOccupancyVariations = initializeBishopOccupancyVariations();
		
		public SliderOccupancyVariations() {
			
		}
		public long[][] getRookOccupancyVariations() {
			return rookOccupancyVariations;
		}
		public long[][] getBishopOccupancyVariations() {
			return bishopOccupancyVariations;
		}
		public static long[] generateRookOccupancyVariations(Square sqr) {
			int sqrInd = sqr.ordinal();
			long mask = SliderOccupancyMask.getByIndex(sqrInd).getRookOccupancyMask();
			byte numOfSetBitsInMask = SliderOccupancyMask.getByIndex(sqrInd).getRookOccupancyMaskBitCount();
			int[] setBitsInMask = BitOperations.serialize(mask, numOfSetBitsInMask);
			int totalNumOfVariations = (1 << numOfSetBitsInMask);
			long[] occVar = new long[totalNumOfVariations];
			for (int i = 0; i < totalNumOfVariations; i++) {
				int[] setBitsInVarInd = BitOperations.serialize(i, BitOperations.getCardinality(i));
				for (int j = 0; j < setBitsInVarInd.length; j++) {
					occVar[i] |= (1L << (setBitsInMask[setBitsInVarInd[j]]));
				}
			}
			return occVar;
		}
		public static long[] generateBishopOccupancyVariations(Square sqr) {
			int sqrInd = sqr.ordinal();
			long mask = SliderOccupancyMask.getByIndex(sqrInd).getBishopOccupancyMask();
			byte numOfSetBitsInMask = SliderOccupancyMask.getByIndex(sqrInd).getBishopOccupancyMaskBitCount();
			int[] setBitsInMask = BitOperations.serialize(mask, numOfSetBitsInMask);
			int totalNumOfVariations = (1 << numOfSetBitsInMask);
			long[] occVar = new long[totalNumOfVariations];
			for (int i = 0; i < totalNumOfVariations; i++) {
				int[] setBitsInVarInd = BitOperations.serialize(i, BitOperations.getCardinality(i));
				for (int j = 0; j < setBitsInVarInd.length; j++) {
					occVar[i] |= (1L << (setBitsInMask[setBitsInVarInd[j]]));
				}
			}
			return occVar;
		}
		private static long[][] initializeRookOccupancyVariations() {
			int sqrInd;
			long[][] rookOccVar = new long[64][];
			for (Square sqr : Square.values()) {
				sqrInd = sqr.ordinal();
				rookOccVar[sqrInd] = generateRookOccupancyVariations(sqr);
			}
			return rookOccVar;
		}
		private static long[][] initializeBishopOccupancyVariations() {
			int sqrInd;
			long[][] bishopOccVar = new long[64][];
			for (Square sqr : Square.values()) {
				sqrInd = sqr.ordinal();
				bishopOccVar[sqrInd] = generateBishopOccupancyVariations(sqr);
			}
			return bishopOccVar;
		}
	}
	
	public static class SliderAttackSetVariations {
		
		private final static long[][] rookAttackSetVariations	= initializeRookAttackSetVariations();
		private final static long[][] bishopAttackSetVariations = initializeBishopAttackSetVariations();
		
		public SliderAttackSetVariations() {
			
		}
		public long[][] getRookAttackSetVariations() {
			return rookAttackSetVariations;
		}
		public long[][] getBishopAttackSetVariations() {
			return bishopAttackSetVariations;
		}
		private static long rankAttacks(Square sqr, long occupancy) {
			long sqrBit = sqr.getBitmap();
			int sqrInd = sqr.ordinal();
			long rank = Rank.getBySquareIndex(sqrInd);
			long forward, reverse;
			forward  = rank & occupancy;
			reverse  = BitOperations.reverse(occupancy);
			forward -= 2*sqrBit;
			reverse -= 2*BitOperations.reverse(sqrBit);
			forward ^= BitOperations.reverse(reverse);
			return forward & rank;
		}
		private static long fileAttacks(Square sqr, long occupancy) {
			long sqrBit = sqr.getBitmap();
			int sqrInd = sqr.ordinal();
			long file = File.getBySquareIndex(sqrInd);
			long forward, reverse;
			forward  = file & occupancy;
			reverse  = BitOperations.reverseBytes(forward);
			forward -= sqrBit;
			reverse -= BitOperations.reverseBytes(sqrBit);
			forward ^= BitOperations.reverseBytes(reverse);
			return forward & file;
		}
		private static long diagonalAttacks(Square sqr, long occupancy) {
			long sqrBit = sqr.getBitmap();
			int sqrInd = sqr.ordinal();
			long diagonal = Diagonal.getBySquareIndex(sqrInd);
			long forward, reverse;
			forward  = diagonal & occupancy;
			reverse  = BitOperations.reverseBytes(forward);
			forward -= sqrBit;
			reverse -= BitOperations.reverseBytes(sqrBit);
			forward ^= BitOperations.reverseBytes(reverse);
			return forward & diagonal;
		}
		private static long antiDiagonalAttacks(Square sqr, long occupancy) {
			long sqrBit = sqr.getBitmap();
			int sqrInd = sqr.ordinal();
			long antiDiagonal = AntiDiagonal.getBySquareIndex(sqrInd);
			long forward, reverse;
			forward  = antiDiagonal & occupancy;
			reverse  = BitOperations.reverseBytes(forward);
			forward -= sqrBit;
			reverse -= BitOperations.reverseBytes(sqrBit);
			forward ^= BitOperations.reverseBytes(reverse);
			return forward & antiDiagonal;
		}
		public static long computeRookAttackSet(Square sqr, long occupancy) {
			return rankAttacks(sqr, occupancy) | fileAttacks(sqr, occupancy);
		}
		public static long computeBishopAttackSet(Square sqr, long occupancy) {
			return diagonalAttacks(sqr, occupancy) | antiDiagonalAttacks(sqr, occupancy);
		}
		private static long[][] initializeRookAttackSetVariations() {
			SliderOccupancyVariations occVar = new SliderOccupancyVariations();
			long[][] rookOccupancyVariations = occVar.getRookOccupancyVariations();
			long[][] rookAttVar = new long[64][];
			int variations;
			int sqrInd;
			for (Square sqr : Square.values()) {
				sqrInd = sqr.ordinal();
				variations = rookOccupancyVariations[sqrInd].length;
				rookAttVar[sqrInd] = new long[variations];
				for (int i = 0; i < variations; i++) {
					rookAttVar[sqrInd][i] = computeRookAttackSet(sqr, rookOccupancyVariations[sqrInd][i]);
				}
			}
			return rookAttVar;
		}
		private static long[][] initializeBishopAttackSetVariations() {
			SliderOccupancyVariations occVar = new SliderOccupancyVariations();
			long[][] bishopOccupancyVariations = occVar.getBishopOccupancyVariations();
			long[][] bishopAttVar = new long[64][];
			int variations;
			int sqrInd;
			for (Square sqr : Square.values()) {
				sqrInd = sqr.ordinal();
				variations = bishopOccupancyVariations[sqrInd].length;
				bishopAttVar[sqrInd] = new long[variations];
				for (int i = 0; i < variations; i++) {
					bishopAttVar[sqrInd][i] = computeBishopAttackSet(sqr, bishopOccupancyVariations[sqrInd][i]);
				}
			}
			return bishopAttVar;
		}
	}
	
	public static enum MagicShift {
		
		A1	(computeRookShifts(0),  computeBishopShifts(0)),
		B1	(computeRookShifts(1),  computeBishopShifts(1)),
		C1	(computeRookShifts(2),  computeBishopShifts(2)),
		D1	(computeRookShifts(3),  computeBishopShifts(3)),
		E1	(computeRookShifts(4),  computeBishopShifts(4)),
		F1	(computeRookShifts(5),  computeBishopShifts(5)),
		G1	(computeRookShifts(6),  computeBishopShifts(6)),
		H1	(computeRookShifts(7),  computeBishopShifts(7)),
		A2	(computeRookShifts(8),  computeBishopShifts(8)),
		B2	(computeRookShifts(9),  computeBishopShifts(9)),
		C2	(computeRookShifts(10), computeBishopShifts(10)),
		D2	(computeRookShifts(11), computeBishopShifts(11)),
		E2	(computeRookShifts(12), computeBishopShifts(12)),
		F2	(computeRookShifts(13), computeBishopShifts(13)),
		G2	(computeRookShifts(14), computeBishopShifts(14)),
		H2	(computeRookShifts(15), computeBishopShifts(15)),
		A3	(computeRookShifts(16), computeBishopShifts(16)),
		B3	(computeRookShifts(17), computeBishopShifts(17)),
		C3	(computeRookShifts(18), computeBishopShifts(18)),
		D3	(computeRookShifts(19), computeBishopShifts(19)),
		E3	(computeRookShifts(20), computeBishopShifts(20)),
		F3	(computeRookShifts(21), computeBishopShifts(21)),
		G3	(computeRookShifts(22), computeBishopShifts(22)),
		H3	(computeRookShifts(23), computeBishopShifts(23)),
		A4	(computeRookShifts(24), computeBishopShifts(24)),
		B4	(computeRookShifts(25), computeBishopShifts(25)),
		C4	(computeRookShifts(26), computeBishopShifts(26)),
		D4	(computeRookShifts(27), computeBishopShifts(27)),
		E4	(computeRookShifts(28), computeBishopShifts(28)),
		F4	(computeRookShifts(29), computeBishopShifts(29)),
		G4	(computeRookShifts(30), computeBishopShifts(30)),
		H4	(computeRookShifts(31), computeBishopShifts(31)),
		A5	(computeRookShifts(32), computeBishopShifts(32)),
		B5	(computeRookShifts(33), computeBishopShifts(33)),
		C5	(computeRookShifts(34), computeBishopShifts(34)),
		D5	(computeRookShifts(35), computeBishopShifts(35)),
		E5	(computeRookShifts(36), computeBishopShifts(36)),
		F5	(computeRookShifts(37), computeBishopShifts(37)),
		G5	(computeRookShifts(38), computeBishopShifts(38)),
		H5	(computeRookShifts(39), computeBishopShifts(39)),
		A6	(computeRookShifts(40), computeBishopShifts(40)),
		B6	(computeRookShifts(41), computeBishopShifts(41)),
		C6	(computeRookShifts(42), computeBishopShifts(42)),
		D6	(computeRookShifts(43), computeBishopShifts(43)),
		E6	(computeRookShifts(44), computeBishopShifts(44)),
		F6	(computeRookShifts(45), computeBishopShifts(45)),
		G6	(computeRookShifts(46), computeBishopShifts(46)),
		H6	(computeRookShifts(47), computeBishopShifts(47)),
		A7	(computeRookShifts(48), computeBishopShifts(48)),
		B7	(computeRookShifts(49), computeBishopShifts(49)),
		C7	(computeRookShifts(50), computeBishopShifts(50)),
		D7	(computeRookShifts(51), computeBishopShifts(51)),
		E7	(computeRookShifts(52), computeBishopShifts(52)),
		F7	(computeRookShifts(53), computeBishopShifts(53)),
		G7	(computeRookShifts(54), computeBishopShifts(54)),
		H7	(computeRookShifts(55), computeBishopShifts(55)),
		A8	(computeRookShifts(56), computeBishopShifts(56)),
		B8	(computeRookShifts(57), computeBishopShifts(57)),
		C8	(computeRookShifts(58), computeBishopShifts(58)),
		D8	(computeRookShifts(59), computeBishopShifts(59)),
		E8	(computeRookShifts(60), computeBishopShifts(60)),
		F8	(computeRookShifts(61), computeBishopShifts(61)),
		G8	(computeRookShifts(62), computeBishopShifts(62)),
		H8	(computeRookShifts(63), computeBishopShifts(63));

		
		final byte rook;
		final byte bishop;
		
		private MagicShift(byte rook, byte bishop) {
			this.rook = rook;
			this.bishop = bishop;
		}
		public byte getRook() {
			return this.rook;
		}
		public byte getBishop() {
			return this.bishop;
		}
		private static byte computeRookShifts(int sqrInd) {
			return (byte)(64 - SliderOccupancyMask.getByIndex(sqrInd).getRookOccupancyMaskBitCount());
		}
		private static byte computeBishopShifts(int sqrInd) {
			return (byte)(64 - SliderOccupancyMask.getByIndex(sqrInd).getBishopOccupancyMaskBitCount());
		}
		public static void printCodeVariables() {
			for (int i = 0; i < 10; i++)
				System.out.println(Square.getByIndex(i) + "\t(computeRookMagicShift(" + i + "),  computeBishopMagicShift(" + i + ")),");
			for (int i = 10; i < 63; i++) 
				System.out.println(Square.getByIndex(i) + "\t(computeRookMagicShift(" + i + "), computeBishopMagicShift(" + i + ")),");
			System.out.println(Square.getByIndex(63) + "\t(computeRookMagicShift(" + 63 + "), computeBishopMagicShift(" + 63 + "));");
		}
		public static MagicShift getByIndex(int sqrInd) {
			switch(sqrInd) {
				case 0:
					return A1;
				case 1:
					return B1;
				case 2:
					return C1;
				case 3:
					return D1;
				case 4:
					return E1;
				case 5:
					return F1;
				case 6:
					return G1;
				case 7:
					return H1;
				case 8:
					return A2;
				case 9:
					return B2;
				case 10:
					return C2;
				case 11:
					return D2;
				case 12:
					return E2;
				case 13:
					return F2;
				case 14:
					return G2;
				case 15:
					return H2;
				case 16:
					return A3;
				case 17:
					return B3;
				case 18:
					return C3;
				case 19:
					return D3;
				case 20:
					return E3;
				case 21:
					return F3;
				case 22:
					return G3;
				case 23:
					return H3;
				case 24:
					return A4;
				case 25:
					return B4;
				case 26:
					return C4;
				case 27:
					return D4;
				case 28:
					return E4;
				case 29:
					return F4;
				case 30:
					return G4;
				case 31:
					return H4;
				case 32:
					return A5;
				case 33:
					return B5;
				case 34:
					return C5;
				case 35:
					return D5;
				case 36:
					return E5;
				case 37:
					return F5;
				case 38:
					return G5;
				case 39:
					return H5;
				case 40:
					return A6;
				case 41:
					return B6;
				case 42:
					return C6;
				case 43:
					return D6;
				case 44:
					return E6;
				case 45:
					return F6;
				case 46:
					return G6;
				case 47:
					return H6;
				case 48:
					return A7;
				case 49:
					return B7;
				case 50:
					return C7;
				case 51:
					return D7;
				case 52:
					return E7;
				case 53:
					return F7;
				case 54:
					return G7;
				case 55:
					return H7;
				case 56:
					return A8;
				case 57:
					return B8;
				case 58:
					return C8;
				case 59:
					return D8;
				case 60:
					return E8;
				case 61:
					return F8;
				case 62:
					return G8;
				case 63:
					return H8;
				default:
					throw new IllegalArgumentException("Invalid square index.");
			}
		}
	}
	
	public static class MagicNumberGenerator {
		
		private long[][] rookOccupancyVariations;
		private long[][] bishopOccupancyVariations;
		private long[][] rookAttackSetVariations;
		private long[][] bishopAttackSetVariations;
		
		private long[] rookMagicNumbers;
		private long[] bishopMagicNumbers;
		
		public MagicNumberGenerator() {
			SliderOccupancyVariations occVar = new SliderOccupancyVariations();
			rookOccupancyVariations = occVar.getRookOccupancyVariations();
			bishopOccupancyVariations = occVar.getBishopOccupancyVariations();
			SliderAttackSetVariations attSetVar = new SliderAttackSetVariations();
			rookAttackSetVariations = attSetVar.getRookAttackSetVariations();
			bishopAttackSetVariations = attSetVar.getBishopAttackSetVariations();
		}
		public long[] getRookMagicNumbers() {
			return this.rookMagicNumbers;
		}
		public long[] getBishopMagicNumbers() {
			return this.bishopMagicNumbers;
		}
		public void generateRookMagicNumbers() {
			rookMagicNumbers = new long[64];
			long[][] magicRookDatabase = new long[64][];
			Random random = new Random();
			long magicNumber;
			int index;
			boolean collision = false;
			for (int i = 0; i < 64; i++) {
				long[] occVar = this.rookOccupancyVariations[i];
				long[] attVar = this.rookAttackSetVariations[i];
				int variations = occVar.length;
				magicRookDatabase[i] = new long[variations];
				do {
					for (int j = 0; j < variations; j++)
						magicRookDatabase[i][j] = 0;
					collision = false;
					magicNumber = random.nextLong() & random.nextLong() & random.nextLong();
					rookMagicNumbers[i] = magicNumber;
					for (int j = 0; j < variations; j++) {
						index = (int)((occVar[j]*magicNumber) >>> MagicShift.getByIndex(i).getRook());
						if (magicRookDatabase[i][index] == 0)
							magicRookDatabase[i][index] = attVar[j];
						else if (magicRookDatabase[i][index] != attVar[j]) {
							collision = true;
							break;
						}
					}
				}
				while (collision);
			}
		}
		public void generateBishopMagicNumbers() {
			bishopMagicNumbers = new long[64];
			long[][] magicBishopDatabase = new long[64][];
			Random random = new Random();
			long magicNumber;
			int index;
			boolean collision = false;
			for (int i = 0; i < 64; i++) {
				long[] occVar = this.bishopOccupancyVariations[i];
				long[] attVar = this.bishopAttackSetVariations[i];
				int variations = occVar.length;
				magicBishopDatabase[i] = new long[variations];
				do {
					for (int j = 0; j < variations; j++)
						magicBishopDatabase[i][j] = 0;
					collision = false;
					magicNumber = random.nextLong() & random.nextLong() & random.nextLong();
					bishopMagicNumbers[i] = magicNumber;
					for (int j = 0; j < variations; j++) {
						index = (int)((occVar[j]*magicNumber) >>> MagicShift.getByIndex(i).getBishop());
						if (magicBishopDatabase[i][index] == 0)
							magicBishopDatabase[i][index] = attVar[j];
						else if (magicBishopDatabase[i][index] != attVar[j]) {
							collision = true;
							break;
						}
					}
				}
				while (collision);
			}
		}
		public void generateMagicNumbers() {
			this.generateRookMagicNumbers();
			this.generateBishopMagicNumbers();
		}
		public void printMagicNumbers() {
			if (this.rookMagicNumbers != null || this.bishopMagicNumbers != null) {
				if (this.rookMagicNumbers == null)
					this.rookMagicNumbers = new long[64];
				if (this.bishopMagicNumbers == null)
					this.bishopMagicNumbers = new long[64];
				System.out.format("%s %5s %70s\n\n", "SQ", "ROOK", "BISHOP");
				for (Square sqr : Square.values()) {
					int sqrInd = sqr.ordinal();
					System.out.println(sqr + " (" + toBinaryLiteral(this.rookMagicNumbers[sqrInd]) + ", " + toBinaryLiteral(this.bishopMagicNumbers[sqrInd]) + "),");
				}
			}
			else
				System.out.println("No magic numbers have been generated so far.");
		}
	}
	
	public static enum MagicNumber {
		
		A1 (0b1001000010000000000000000001001000100000010000000000000010000001L, 0b0000000000000010000100000000000100001000000000001000000010000000L),
		B1 (0b0000000011000000000100000000001001100000000000000100000000000000L, 0b1010000000010001000100000000000110000010100100001000000001000100L),
		C1 (0b0000000010000000000010010010000000000000100000000101000000000100L, 0b0100000000000100000010000000000010000001000000010000000000010000L),
		D1 (0b0000000010000000000100000000000001000100100000000000100000000000L, 0b0000100000100100010000010000000000100000000100110000000001000011L),
		E1 (0b0000000010000000000000100000010000000000000010000000000010000000L, 0b0000000000000100000001010010000000000001001001001000000010000000L),
		F1 (0b0010000010000000000001000000000000100001000001100000000010000000L, 0b0000000000000010000000101000001000100000001000000100000000000000L),
		G1 (0b0110010000000000000011000000000010000001000000100011000000001000L, 0b1001000000000100000000010000010000000110101000000010000000000000L),
		H1 (0b0000001010000000000000100100010000100101000000000000000010000000L, 0b0000100000000001000001001000001000000000101000000000010001000000L),
		A2 (0b0000000001010000100000000000000010000000001000000100000000010100L, 0b1000011010000000000010000101100100001000000010000000000011001000L),
		B2 (0b1000000000010000010000000000000000100000000100000000000001000100L, 0b0000000000000010001000000000000100000010001000001000001100000000L),
		C2 (0b0000000000000110000000000100000001010110000000001000000001100000L, 0b0000000010000001100111100010001000000100000010010000000110000000L),
		D2 (0b0001100000000001000000000011000000000000001000110000001000001000L, 0b0110100000001101100001000100000001000000100010000000000001000000L),
		E2 (0b0000000000000000100000000000100000000011100000000000010000000000L, 0b0001001000100001000001000000010000100000110000100000000000000000L),
		F2 (0b0011000000000010000000000000100000000100000100000000101000000000L, 0b0000000000001010000000010011000000001000001000000000000000001000L),
		G2 (0b0001000100100001000000000000000100000000000001100000000010000100L, 0b0000010100100000001000100000100100000001001000000001001000100000L),
		H2 (0b0001000101000010000000000000010010000000010011000000001100010010L, 0b0010000000010000100000100000000100100000100010000000010000000000L),
		A3 (0b0101000100000011000000010000000000100010100000000000000001000000L, 0b0000000010000100000100000000100000010000000001000000100000000000L),
		B3 (0b0000000100110000000001000100000000000000010010001010000000000000L, 0b0100000010100110000010000010000001010100000000110000001000000000L),
		C3 (0b0000000001001010100000001000000001010000000000000010000000000001L, 0b0010000001001000000110100000010000001000000000001010000010011000L),
		D3 (0b0100000100000011100000011000000000001000000000000011000000000100L, 0b1000000000010100000000100000100000000100100100011001000000000000L),
		E3 (0b0000000100001110000000100000000000001000101000000001000000000100L, 0b0000001000001001000000010100100000100000000010000000010101000100L),
		F3 (0b0010010000100001000000010000000000000100000000001000100001000010L, 0b1011000000000010000000000000110000100000100101000010000000000000L),
		G3 (0b0000000010000000000001000000000000011000000100000000000110001010L, 0b0000000000000001010000000000001100011000000000100001000000000000L),
		H3 (0b0000100100000101000000100000000000010001000000001000000001010100L, 0b0001100001000001010000000100000100000001010010000000010000000000L),
		A4 (0b0000000001000000100000000010000010000000000000000100000000000000L, 0b1000010000010000010000000100000001000100000001000000010000000101L),
		B4 (0b1000000001000010100000010000000100000000001000000100000000001001L, 0b0011100000000100000011000001001000100000101010000000010010000000L),
		C4 (0b0000010000000100010000000011000100000000001000000000000100000001L, 0b0000000000010000111010000000100000100100000100000100010000000000L),
		D4 (0b0000000000000100011000010000000100000000000100000000000000001000L, 0b0100100000001100000000010000000010000000001000000000100010000000L),
		E4 (0b0001000001000000000110000000000010000000000001000000000010000000L, 0b0000001000001101000000000100000000101100000001000100000000000000L),
		F4 (0b1001000000001010000000000000101000000000010010001001000000000100L, 0b0100011010001000000010100000010000000000110000000100001000000010L),
		G4 (0b0000000000010000000000110000010000000000010100100100100000010000L, 0b0100000000001000000000100000000000000100100100100000000100000000L),
		H4 (0b1100010110000001100000000000110010000000000010000100100100000000L, 0b0001000000000000110000010000000000101000100001000001000100010100L),
		A5 (0b0000010000000000100000000100000000001100100000000000100000100000L, 0b0100000000010001001000000010000000100000000110000000100001001000L),
		B5 (0b0010000000010000000000000010000000010100110000000000000001000110L, 0b0000000000000000100100000001000000000000000101000000010000110000L),
		C5 (0b0000000000001100100000100001000000000000100000000010000000000000L, 0b0100000000000010000000100000101000000001000000010000100000000000L),
		D5 (0b0000000000001010000000000100000000010010000000000010000000101000L, 0b0000000100000001001000000010000000100000000010000000000010000000L),
		E5 (0b0000010000001000000000011000000000001000100000000000010000000000L, 0b0100000000010000000000100000000010000000000000000101000000000101L),
		F5 (0b1000000000001000000010100000000010000000100000000000010000000000L, 0b0000000000100010000000001000001000000100000000010000000001000000L),
		G5 (0b0000010100100001110000010000100000110100000000000001000000100010L, 0b0000000000001000000000001010010000001110010100010000000100000000L),
		H5 (0b0000000001001010011000000100010000001010000000000000001110100001L, 0b0000000000000100000000001000000000111000000001100000000100000000L),
		A6 (0b1000001010000000000000000100000100100000000001000100000000000011L, 0b0000010000000100000001000000010001000000000000000000010100000000L),
		B6 (0b0000000000110000000000000100110110100000000000010100000000000000L, 0b0000000000000010000000001001000000000100000001000000100010000000L),
		C6 (0b0000000000010000000000000010000000000001100000001000000000011000L, 0b0000001000000110000000001000001000100000100000000000010000001100L),
		D6 (0b0100010000010101000000001000101000110000000000010000000000100000L, 0b0000000010010000010000000100001000000000100000101000100000000000L),
		E6 (0b0000010000001000000011000000000000001000000000001000000010000000L, 0b0000000000000000010010000000000100000100000000010001000001000010L),
		F6 (0b0000000000000010000000010000010000001000001000100000000000110000L, 0b0000100000000100000100000000000010100010000000000001000100001000L),
		G6 (0b1010000100000100000100100010000100001000000001000000000000010000L, 0b1000000000100100010010000001111010000101000010000001010000000000L),
		H6 (0b0000010000000001000000000011000001000010100010010000000000000010L, 0b0000001000001001000010000000001000010000010101000010001010000000L),
		A7 (0b1000000000000000100010100000000100000100001000000100001000000000L, 0b0000000000010010000000010000000110001000010000000000100000001000L),
		B7 (0b0100000000000010010000000000000010000000001000000000010010000000L, 0b0100000001000010000000110000001100001001001100000000000010000000L),
		C7 (0b0100001000000001010000000011001000000101101000001000001000000000L, 0b0100000001000000001001100000000100010100000001100000100000100000L),
		D7 (0b1000000000010000000000010000000000010000011001001010100100000000L, 0b0000000000000000100000000001101001000110000010000000000001000000L),
		E7 (0b0000000000010100000010000000000010000000100001000000000110000000L, 0b0001011001000000011000000000110000000101000001000000110000100100L),
		F7 (0b1000000001101000000000100000000010000000100001000000000010000000L, 0b0000010000000000011000000000110000010000100000001000001000000000L),
		G7 (0b0000000100000001000000000100001000000000010001000000000100000000L, 0b0010000001100010101001000001000000000110100000010000000010010000L),
		H7 (0b0000010000010011000000000000011000000000010100001000000100000000L, 0b0001010001100000010010000000001000000000110000000100100000001000L),
		A8 (0b0100000000001110000000000010000010000000010000010101010100000010L, 0b0000000000000000010000100000000001000100001000000010010000000000L),
		B8 (0b0010000100010000100001000100000100000010000000000010010000010010L, 0b1000000000001001000000001100000001000110000100000001000000000010L),
		C8 (0b0000001010000010100000000100000100001010010100100000000000100010L, 0b0000100000000000000000001001101000000000100001000000010000000000L),
		D8 (0b0000010000000000000010000101000000000001000000000010000000110101L, 0b0000000100000000000000000000010001000000010000100000001000000100L),
		E8 (0b1000000000000010000000000010010100001000101000000001000000010010L, 0b0010000000000000000000001000000000010000000000100000001000000000L),
		F8 (0b0000000000010001000000000000001010000100000000000000100000000001L, 0b0000000000000000000000010010010010011000100100000000000100000010L),
		G8 (0b1000010000000000001000001000000100010000000010000000001000000100L, 0b0010001000000100000010010010000001011000001000001000100100000000L),
		H8 (0b1000001000000000000000010000010000001110001000001000010101000010L, 0b0100000000000100110010000000000010001000000100100000000001000100L);
		
		final long rookMagicNumber;
		final long bishopMagicNumber;
		
		private MagicNumber(long rookMagicNumber, long bishopMagicNumber) {
			this.rookMagicNumber = rookMagicNumber;
			this.bishopMagicNumber = bishopMagicNumber;
		}
		public long getRookMagicNumber() {
			return this.rookMagicNumber;
		}
		public long getBishopMagicNumber() {
			return this.bishopMagicNumber;
		}
		public static MagicNumber getByIndex(int sqrInd) {
			switch(sqrInd) {
				case 0:
					return A1;
				case 1:
					return B1;
				case 2:
					return C1;
				case 3:
					return D1;
				case 4:
					return E1;
				case 5:
					return F1;
				case 6:
					return G1;
				case 7:
					return H1;
				case 8:
					return A2;
				case 9:
					return B2;
				case 10:
					return C2;
				case 11:
					return D2;
				case 12:
					return E2;
				case 13:
					return F2;
				case 14:
					return G2;
				case 15:
					return H2;
				case 16:
					return A3;
				case 17:
					return B3;
				case 18:
					return C3;
				case 19:
					return D3;
				case 20:
					return E3;
				case 21:
					return F3;
				case 22:
					return G3;
				case 23:
					return H3;
				case 24:
					return A4;
				case 25:
					return B4;
				case 26:
					return C4;
				case 27:
					return D4;
				case 28:
					return E4;
				case 29:
					return F4;
				case 30:
					return G4;
				case 31:
					return H4;
				case 32:
					return A5;
				case 33:
					return B5;
				case 34:
					return C5;
				case 35:
					return D5;
				case 36:
					return E5;
				case 37:
					return F5;
				case 38:
					return G5;
				case 39:
					return H5;
				case 40:
					return A6;
				case 41:
					return B6;
				case 42:
					return C6;
				case 43:
					return D6;
				case 44:
					return E6;
				case 45:
					return F6;
				case 46:
					return G6;
				case 47:
					return H6;
				case 48:
					return A7;
				case 49:
					return B7;
				case 50:
					return C7;
				case 51:
					return D7;
				case 52:
					return E7;
				case 53:
					return F7;
				case 54:
					return G7;
				case 55:
					return H7;
				case 56:
					return A8;
				case 57:
					return B8;
				case 58:
					return C8;
				case 59:
					return D8;
				case 60:
					return E8;
				case 61:
					return F8;
				case 62:
					return G8;
				case 63:
					return H8;
				default:
					throw new IllegalArgumentException("Invalid square index.");
			}
		}
	}
	
	public static enum MoveDatabase {
		
		A1 (Square.A1),
		B1 (Square.B1),
		C1 (Square.C1),
		D1 (Square.D1),
		E1 (Square.E1),
		F1 (Square.F1),
		G1 (Square.G1),
		H1 (Square.H1),
		A2 (Square.A2),
		B2 (Square.B2),
		C2 (Square.C2),
		D2 (Square.D2),
		E2 (Square.E2),
		F2 (Square.F2),
		G2 (Square.G2),
		H2 (Square.H2),
		A3 (Square.A3),
		B3 (Square.B3),
		C3 (Square.C3),
		D3 (Square.D3),
		E3 (Square.E3),
		F3 (Square.F3),
		G3 (Square.G3),
		H3 (Square.H3),
		A4 (Square.A4),
		B4 (Square.B4),
		C4 (Square.C4),
		D4 (Square.D4),
		E4 (Square.E4),
		F4 (Square.F4),
		G4 (Square.G4),
		H4 (Square.H4),
		A5 (Square.A5),
		B5 (Square.B5),
		C5 (Square.C5),
		D5 (Square.D5),
		E5 (Square.E5),
		F5 (Square.F5),
		G5 (Square.G5),
		H5 (Square.H5),
		A6 (Square.A6),
		B6 (Square.B6),
		C6 (Square.C6),
		D6 (Square.D6),
		E6 (Square.E6),
		F6 (Square.F6),
		G6 (Square.G6),
		H6 (Square.H6),
		A7 (Square.A7),
		B7 (Square.B7),
		C7 (Square.C7),
		D7 (Square.D7),
		E7 (Square.E7),
		F7 (Square.F7),
		G7 (Square.G7),
		H7 (Square.H7),
		A8 (Square.A8),
		B8 (Square.B8),
		C8 (Square.C8),
		D8 (Square.D8),
		E8 (Square.E8),
		F8 (Square.F8),
		G8 (Square.G8),
		H8 (Square.H8);
		
		final byte sqrInd;
		
		final long king;
		private final long[] rook;
		private final long[] bishop;
		final long knight;
		final long pawnWhiteAdvance;
		final long pawnWhiteCapture;
		final long pawnBlackAdvance;
		final long pawnBlackCapture;
		
		private MoveDatabase(Square sqr) {
			this.sqrInd = (byte)sqr.ordinal();
			SliderOccupancyVariations occVar = new SliderOccupancyVariations();
			long[] rookOccVar 	= occVar.getRookOccupancyVariations()[this.sqrInd];
			long[] bishopOccVar = occVar.getBishopOccupancyVariations()[this.sqrInd];
			int rookNumOfVar 	= rookOccVar.length;
			int bishopNumOfVar 	= bishopOccVar.length;
			this.rook 	= new long[rookNumOfVar];
			this.bishop = new long[bishopNumOfVar];
			SliderAttackSetVariations attVar = new SliderAttackSetVariations();
			long[] rookAttVar 	= attVar.getRookAttackSetVariations()[this.sqrInd];
			long[] bishopAttVar = attVar.getBishopAttackSetVariations()[this.sqrInd];
			int index;
			for (int i = 0; i < rookNumOfVar; i++) {
				index = (int)((rookOccVar[i]*MagicNumber.getByIndex(this.sqrInd).getRookMagicNumber()) >>> MagicShift.getByIndex(this.sqrInd).getRook());
				this.rook[index] = rookAttVar[i];
			}
			for (int i = 0; i < bishopNumOfVar; i++) {
				index = (int)((bishopOccVar[i]*MagicNumber.getByIndex(this.sqrInd).getBishopMagicNumber()) >>> MagicShift.getByIndex(this.sqrInd).getBishop());
				this.bishop[index] = bishopAttVar[i];
			}
			this.king 				= MoveMask.generateKingsMoveMask(sqr);
			this.knight 			= MoveMask.generateKnightMasks(sqr);
			this.pawnWhiteAdvance 	= MoveMask.generateWhitePawnsAdvanceMasks(sqr);
			this.pawnWhiteCapture 	= MoveMask.generateWhitePawnsCaptureMasks(sqr);
			this.pawnBlackAdvance 	= MoveMask.generateBlackPawnsAdvanceMasks(sqr);
			this.pawnBlackCapture 	= MoveMask.generateBlackPawnsCaptureMasks(sqr);
		}
		public long getCrudeKingMoves() {
			return this.king;
		}
		public long getCrudeKnightMoves() {
			return this.knight;
		}
		public long getCrudeWhitePawnCaptures() {
			return this.pawnWhiteCapture;
		}
		public long getCrudeBlackPawnCaptures() {
			return this.pawnBlackCapture;
		}
		public long getCrudeRookMoves() {
			return this.rook[0];
		}
		public long getCrudeBishopMoves() {
			return this.bishop[0];
		}
		public long getCrudeQueenMoves() {
			return this.rook[0] | this.bishop[0];
		}
		public long getWhiteKingMoves(long allNonWhiteOccupied) {
			return this.king & allNonWhiteOccupied;
		}
		public long getBlackKingMoves(long allNonBlackOccupied) {
			return this.king & allNonBlackOccupied;
		}
		public long getWhiteKnightMoves(long allNonWhiteOccupied) {
			return this.knight & allNonWhiteOccupied;
		}
		public long getBlackKnightMoves(long allNonBlackOccupied) {
			return this.knight & allNonBlackOccupied;
		}
		public long getWhitePawnCaptures(long allBlackPieces) {
			return this.pawnWhiteCapture & allBlackPieces;
		}
		public long getBlackPawnCaptures(long allWhitePieces) {
			return this.pawnBlackCapture & allWhitePieces;
		}
		public long getWhitePawnAdvances(long allEmpty) {
			if (this.sqrInd > 15)
				return this.pawnWhiteAdvance & allEmpty;
			long firstBlocker = BitOperations.getLSBit(this.pawnWhiteAdvance & ~allEmpty);
			return this.pawnWhiteAdvance & (-1 + firstBlocker);
		}
		public long getBlackPawnAdvances(long allEmpty) {
			if (this.sqrInd < 48)
				return this.pawnBlackAdvance & allEmpty;
			long firstBlocker = BitOperations.getMSBit(this.pawnBlackAdvance & ~allEmpty);
			return this.pawnBlackAdvance^(this.pawnBlackAdvance & -(firstBlocker << 1));
		}
		public long getWhitePawnMoves(long allBlackPieces, long allEmpty) {
			return this.getWhitePawnAdvances(allEmpty) | this.getWhitePawnCaptures(allBlackPieces);
		}
		public long getBlackPawnMoves(long allWhitePieces, long allEmpty) {
			return this.getBlackPawnAdvances(allEmpty) | this.getBlackPawnCaptures(allWhitePieces);
		}
		public long getWhiteRookMoves(long allNonWhiteOccupied, long allOccupied) {
			long occupancy 	= SliderOccupancyMask.getByIndex(this.sqrInd).rookOccupancyMask & allOccupied;
			int index 		= (int)((occupancy*MagicNumber.getByIndex(this.sqrInd).rookMagicNumber) >>> MagicShift.getByIndex(this.sqrInd).rook);
			return this.rook[index] & allNonWhiteOccupied;
		}
		public long getBlackRookMoves(long allNonBlackOccupied, long allOccupied) {
			long occupancy 	= SliderOccupancyMask.getByIndex(this.sqrInd).rookOccupancyMask & allOccupied;
			int index 		= (int)((occupancy*MagicNumber.getByIndex(this.sqrInd).rookMagicNumber) >>> MagicShift.getByIndex(this.sqrInd).rook);
			return this.rook[index] & allNonBlackOccupied;
		}
		public long getWhiteBishopMoves(long allNonWhiteOccupied, long allOccupied) {
			long occupancy 	= SliderOccupancyMask.getByIndex(this.sqrInd).bishopOccupancyMask & allOccupied;
			int index 		= (int)((occupancy*MagicNumber.getByIndex(this.sqrInd).bishopMagicNumber) >>> MagicShift.getByIndex(this.sqrInd).bishop);
			return this.bishop[index] & allNonWhiteOccupied;
		}
		public long getBlackBishopMoves(long allNonBlackOccupied, long allOccupied) {
			long occupancy 	= SliderOccupancyMask.getByIndex(this.sqrInd).bishopOccupancyMask & allOccupied;
			int index 		= (int)((occupancy*MagicNumber.getByIndex(this.sqrInd).bishopMagicNumber) >>> MagicShift.getByIndex(this.sqrInd).bishop);
			return this.bishop[index] & allNonBlackOccupied;
		}
		public long getWhiteQueenMoves(long allNonWhiteOccupied, long allOccupied) {
			long rookOccupancy 		= SliderOccupancyMask.getByIndex(this.sqrInd).rookOccupancyMask & allOccupied;
			long bishopOccupancy 	= SliderOccupancyMask.getByIndex(this.sqrInd).bishopOccupancyMask & allOccupied;
			int rookIndex 			= (int)((rookOccupancy*MagicNumber.getByIndex(this.sqrInd).rookMagicNumber) >>> MagicShift.getByIndex(this.sqrInd).rook);
			int bishopIndex 		= (int)((bishopOccupancy*MagicNumber.getByIndex(this.sqrInd).bishopMagicNumber) >>> MagicShift.getByIndex(this.sqrInd).bishop);
			return (this.bishop[bishopIndex] | this.rook[rookIndex]) & allNonWhiteOccupied;
		}
		public long getBlackQueenMoves(long allNonBlackOccupied, long allOccupied) {
			long rookOccupancy 		= SliderOccupancyMask.getByIndex(this.sqrInd).rookOccupancyMask & allOccupied;
			long bishopOccupancy 	= SliderOccupancyMask.getByIndex(this.sqrInd).bishopOccupancyMask & allOccupied;
			int rookIndex 			= (int)((rookOccupancy*MagicNumber.getByIndex(this.sqrInd).rookMagicNumber) >>> MagicShift.getByIndex(this.sqrInd).rook);
			int bishopIndex 		= (int)((bishopOccupancy*MagicNumber.getByIndex(this.sqrInd).bishopMagicNumber) >>> MagicShift.getByIndex(this.sqrInd).bishop);
			return (this.bishop[bishopIndex] | this.rook[rookIndex]) & allNonBlackOccupied;
		}
		public static MoveDatabase getByIndex(int sqrInd) {
			switch(sqrInd) {
				case 0:
					return A1;
				case 1:
					return B1;
				case 2:
					return C1;
				case 3:
					return D1;
				case 4:
					return E1;
				case 5:
					return F1;
				case 6:
					return G1;
				case 7:
					return H1;
				case 8:
					return A2;
				case 9:
					return B2;
				case 10:
					return C2;
				case 11:
					return D2;
				case 12:
					return E2;
				case 13:
					return F2;
				case 14:
					return G2;
				case 15:
					return H2;
				case 16:
					return A3;
				case 17:
					return B3;
				case 18:
					return C3;
				case 19:
					return D3;
				case 20:
					return E3;
				case 21:
					return F3;
				case 22:
					return G3;
				case 23:
					return H3;
				case 24:
					return A4;
				case 25:
					return B4;
				case 26:
					return C4;
				case 27:
					return D4;
				case 28:
					return E4;
				case 29:
					return F4;
				case 30:
					return G4;
				case 31:
					return H4;
				case 32:
					return A5;
				case 33:
					return B5;
				case 34:
					return C5;
				case 35:
					return D5;
				case 36:
					return E5;
				case 37:
					return F5;
				case 38:
					return G5;
				case 39:
					return H5;
				case 40:
					return A6;
				case 41:
					return B6;
				case 42:
					return C6;
				case 43:
					return D6;
				case 44:
					return E6;
				case 45:
					return F6;
				case 46:
					return G6;
				case 47:
					return H6;
				case 48:
					return A7;
				case 49:
					return B7;
				case 50:
					return C7;
				case 51:
					return D7;
				case 52:
					return E7;
				case 53:
					return F7;
				case 54:
					return G7;
				case 55:
					return H7;
				case 56:
					return A8;
				case 57:
					return B8;
				case 58:
					return C8;
				case 59:
					return D8;
				case 60:
					return E8;
				case 61:
					return F8;
				case 62:
					return G8;
				case 63:
					return H8;
				default:
					throw new IllegalArgumentException("Invalid square index.");
			}
		}
	}
	
	//the bitboards for each piece
	private long whiteKing;
	private long whiteQueens;
	private long whiteRooks;
	private long whiteBishops;
	private long whiteKnights;
	private long whitePawns;
	
	private long blackKing;
	private long blackQueens;
	private long blackRooks;
	private long blackBishops;
	private long blackKnights;
	private long blackPawns;
	
	//bitboard collections maintained for faster processing of the position
	private long allWhitePieces;
	private long allBlackPieces;
	
	private long allNonWhiteOccupied;
	private long allNonBlackOccupied;
	
	private long allOccupied;
	private long allEmpty;
	
	private int[] offsetBoard;							//a complimentary board data-structure to the bitboards to efficiently detect pieces on specific squares
	
	private boolean whitesTurn = true;
	
	private long checkers = 0;							//a bitboard of all the pieces that attack the color to move's king
	private boolean check = false;
	
	private LongStack moveList = new LongStack();		//a stack of all the moves made so far
	
	private int moveIndex = 0;							//the count of the current move
	private int fiftyMoveRuleClock = 0;					//the number of moves made since the last pawn move or capture
	
	private int enPassantRights = 8;					//denotes the file on which en passant is possible; 8 means no en passant rights
	
	private int whiteCastlingRights = 3;				//denotes to what extent it would still be possible to castle regardless of whether it is actually legally executable in the current position
	private int blackCastlingRights = 3;				//0 - no castling rights, 1 - king-side castling only, 2 - queen-side castling only, 3 - all castling rights
	
	private Zobrist keyGen = new Zobrist();				//a Zobrist key generator for hashing the board
	
	private long zobristKey;							//the Zobrist key that is fairly close to a unique representation of the state of the Board instance in one number
	private long[] zobristKeyHistory = new long[2*237];	/*All the positions that have occured so far represented in Zobrist keys.
														 "The longest decisive tournament game is Fressinet-Kosteniuk, Villandry 2007, which Kosteniuk won in 237 moves."*/
	private int repetitions = 0;						//the number of times the current position has occured before
	
	public Board() {
		this.initializeBitBoards();
		this.initializeOffsetBoard();
		this.initializeZobristKeys();
	}
	/**It parses a FEN-String and sets the instance fields accordingly.
	 * Beside standard six-field FEN-Strings, it also accepts four-field Strings without the fifty-move rule clock and the move index.*/
	public Board(String fen) {
		String[] fenFields = fen.split(" "), ranks;
		String board, turn, castling, enPassant, fiftyMoveClock, moveCount, rank;
		char piece;
		int pieceNum, index = 0;
		if (fenFields.length == 6) {
			fiftyMoveClock 	= fenFields[4];
			moveCount 		= fenFields[5];
			try {
				this.fiftyMoveRuleClock = Integer.parseInt(fiftyMoveClock);
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException("The fifty-move rule clock field of the FEN-string does not conform to the standards. Parsing not possible.");
			}
			try {
				this.moveIndex = (Integer.parseInt(moveCount) - 1)*2;
				if (!this.whitesTurn)
					this.moveIndex++;
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException("The move index field does not conform to the standards. Parsing not possible.");
			}
		}
		else if (fenFields.length != 4)
			throw new IllegalArgumentException("The FEN-String has an unallowed number of fields.");
		board 			= fenFields[0];
		turn 			= fenFields[1];
		castling 		= fenFields[2];
		enPassant 		= fenFields[3];
		ranks = board.split("/");
		if (ranks.length != 8)
			throw new IllegalArgumentException("The board position representation does not have eight ranks.");
		this.offsetBoard = new int[64];
		for (int i = 0; i < 64; i++)
			this.offsetBoard[i] = 0;
		for (int i = 7; i >= 0; i--) {
			rank = ranks[i];
			for (int j = 0; j < rank.length(); j++) {
				piece = rank.charAt(j);
				pieceNum = piece - '0';
				if (pieceNum >= 0 && pieceNum <= 8)
					index += pieceNum;
				else {
					switch (piece) {
						case 'K': {
							this.offsetBoard[index] = 1;
							this.whiteKing		 = Square.getBitmapByIndex(index);
						}
						break;
						case 'Q': {
							this.offsetBoard[index] = 2;
							this.whiteQueens	|= Square.getBitmapByIndex(index);
						}
						break;
						case 'R': {
							this.offsetBoard[index] = 3;
							this.whiteRooks		|= Square.getBitmapByIndex(index);
						}
						break;
						case 'B': {
							this.offsetBoard[index] = 4;
							this.whiteBishops	|= Square.getBitmapByIndex(index);
						}
						break;
						case 'N': {
							this.offsetBoard[index] = 5;
							this.whiteKnights	|= Square.getBitmapByIndex(index);
						}
						break;
						case 'P': {
							this.offsetBoard[index] = 6;
							this.whitePawns		|= Square.getBitmapByIndex(index);
						}
						break;
						case 'k': {
							this.offsetBoard[index] = 7;
							this.blackKing		 = Square.getBitmapByIndex(index);
						}
						break;
						case 'q': {
							this.offsetBoard[index] = 8;
							this.blackQueens	|= Square.getBitmapByIndex(index);
						}
						break;
						case 'r': {
							this.offsetBoard[index] = 9;
							this.blackRooks		|= Square.getBitmapByIndex(index);
						}
						break;
						case 'b': {
							this.offsetBoard[index] = 10;
							this.blackBishops	|= Square.getBitmapByIndex(index);
						}
						break;
						case 'n': {
							this.offsetBoard[index] = 11;
							this.blackKnights	|= Square.getBitmapByIndex(index);
						}
						break;
						case 'p': {
							this.offsetBoard[index] = 12;
							this.blackPawns		|= Square.getBitmapByIndex(index);
						}
					}
					index++;
				}
			}
		}
		this.initializeCollections();
		if (turn.toLowerCase().compareTo("w") == 0)
			this.whitesTurn = true;
		else
			this.whitesTurn = false;
		this.whiteCastlingRights = 0;
		if (castling.contains("K"))
			this.whiteCastlingRights += 1;
		if (castling.contains("Q"))
			this.whiteCastlingRights += 2;
		this.blackCastlingRights = 0;
		if (castling.contains("k"))
			this.blackCastlingRights += 1;
		if (castling.contains("q"))
			this.blackCastlingRights += 2;
		if (enPassant.compareTo("-") == 0)
			this.enPassantRights = 8;
		else
			this.enPassantRights = enPassant.toLowerCase().charAt(0) - 'a';
		this.setCheck();
		this.initializeZobristKeys();
	}
	private void initializeCollections() {
		this.allWhitePieces		 =  this.whiteKing | this.whiteQueens | this.whiteRooks | this.whiteBishops | this.whiteKnights | this.whitePawns;
		this.allBlackPieces		 =  this.blackKing | this.blackQueens | this.blackRooks | this.blackBishops | this.blackKnights | this.blackPawns;
		this.allNonWhiteOccupied = ~this.allWhitePieces;
		this.allNonBlackOccupied = ~this.allBlackPieces;
		this.allOccupied		 =  this.allWhitePieces | this.allBlackPieces;
		this.allEmpty			 = ~this.allOccupied;
	}
	private void initializeBitBoards() {
		this.whiteKing		=  Piece.WHITE_KING.getInitPosBitmap();
		this.whiteQueens	=  Piece.WHITE_QUEEN.getInitPosBitmap();
		this.whiteRooks		=  Piece.WHITE_ROOK.getInitPosBitmap();
		this.whiteBishops	=  Piece.WHITE_BISHOP.getInitPosBitmap();
		this.whiteKnights	=  Piece.WHITE_KNIGHT.getInitPosBitmap();
		this.whitePawns		=  Piece.WHITE_PAWN.getInitPosBitmap();
		
		this.blackKing		=  Piece.BLACK_KING.getInitPosBitmap();
		this.blackQueens	=  Piece.BLACK_QUEEN.getInitPosBitmap();
		this.blackRooks		=  Piece.BLACK_ROOK.getInitPosBitmap();
		this.blackBishops	=  Piece.BLACK_BISHOP.getInitPosBitmap();
		this.blackKnights	=  Piece.BLACK_KNIGHT.getInitPosBitmap();
		this.blackPawns		=  Piece.BLACK_PAWN.getInitPosBitmap();
		this.initializeCollections();
	}
	private void initializeOffsetBoard() {
		this.offsetBoard = new int[64];
		this.offsetBoard[0] =  Piece.WHITE_ROOK.getOffsetBoardRep();
		this.offsetBoard[1] =  Piece.WHITE_KNIGHT.getOffsetBoardRep();
		this.offsetBoard[2] =  Piece.WHITE_BISHOP.getOffsetBoardRep();
		this.offsetBoard[3] =  Piece.WHITE_QUEEN.getOffsetBoardRep();
		this.offsetBoard[4] =  Piece.WHITE_KING.getOffsetBoardRep();
		this.offsetBoard[5] =  Piece.WHITE_BISHOP.getOffsetBoardRep();
		this.offsetBoard[6] =  Piece.WHITE_KNIGHT.getOffsetBoardRep();
		this.offsetBoard[7] =  Piece.WHITE_ROOK.getOffsetBoardRep();
		for (int i = 8; i < 16; i++)
			this.offsetBoard[i] = Piece.WHITE_PAWN.getOffsetBoardRep();
		
		for (int i = 48; i < 56; i++)
			this.offsetBoard[i] = Piece.BLACK_PAWN.getOffsetBoardRep();
		this.offsetBoard[56] = Piece.BLACK_ROOK.getOffsetBoardRep();
		this.offsetBoard[57] = Piece.BLACK_KNIGHT.getOffsetBoardRep();
		this.offsetBoard[58] = Piece.BLACK_BISHOP.getOffsetBoardRep();
		this.offsetBoard[59] = Piece.BLACK_QUEEN.getOffsetBoardRep();
		this.offsetBoard[60] = Piece.BLACK_KING.getOffsetBoardRep();
		this.offsetBoard[61] = Piece.BLACK_BISHOP.getOffsetBoardRep();
		this.offsetBoard[62] = Piece.BLACK_KNIGHT.getOffsetBoardRep();
		this.offsetBoard[63] = Piece.BLACK_ROOK.getOffsetBoardRep();
	}
	private void initializeZobristKeys() {
		this.zobristKey = keyGen.hash(this);
		this.zobristKeyHistory[0] = this.zobristKey;
	}
	public int[] getOffsetBoard() {
		return this.offsetBoard;
	}
	public boolean getTurn() {
		return this.whitesTurn;
	}
	public boolean getCheck() {
		return this.check;
	}
	public int getMoveIndex() {
		return this.moveIndex;
	}
	public int getFiftyMoveRuleClock() {
		return this.fiftyMoveRuleClock;
	}
	public int getWhiteCastlingRights() {
		return this.whiteCastlingRights;
	}
	public int getBlackCastlingRights() {
		return this.blackCastlingRights;
	}
	public int getEnPassantRights() {
		return this.enPassantRights;
	}
	public int getRepetitions() {
		return this.repetitions;
	}
	public long getZobristKey() {
		return this.zobristKey;
	}
	public long getLastMove() {
		return this.moveList.getHead();
	}
	private void setBitboards(int moved, int captured, long fromBit, long toBit) {
		if (this.whitesTurn) {
			switch (moved) {
				case 1:
					this.whiteKing = toBit;
				break;
				case 2: {
					this.whiteQueens ^= fromBit;
					this.whiteQueens |= toBit;
				}
				break;
				case 3: {
					this.whiteRooks ^= fromBit;
					this.whiteRooks |= toBit;
				}
				break;
				case 4: {
					this.whiteBishops ^= fromBit;
					this.whiteBishops |= toBit;
				}
				break;
				case 5: {
					this.whiteKnights ^= fromBit;
					this.whiteKnights |= toBit;
				}
				break;
				case 6: {
					this.whitePawns ^= fromBit;
					this.whitePawns |= toBit;
				}
				break;
			}
			switch (captured) {
				case 0:
				break;
				case 8:
					this.blackQueens ^= toBit;
				break;
				case 9:
					this.blackRooks ^= toBit;
				break;
				case 10:
					this.blackBishops ^= toBit;
				break;
				case 11:
					this.blackKnights ^= toBit;
				break;
				case 12:
					this.blackPawns ^= toBit;
				break;
			}
			this.allWhitePieces 	 ^=  fromBit;
			this.allWhitePieces 	 |=  toBit;
			this.allBlackPieces 	 &= ~toBit;
			this.allNonWhiteOccupied  = ~this.allWhitePieces;
			this.allNonBlackOccupied  = ~this.allBlackPieces;
			this.allOccupied 		  =  this.allWhitePieces | this.allBlackPieces;
			this.allEmpty			  = ~this.allOccupied;
		}
		else {
			switch (moved) {
				case 7:
					this.blackKing = toBit;
				break;
				case 8: {
					this.blackQueens ^= fromBit;
					this.blackQueens |= toBit;
				}
				break;
				case 9: {
					this.blackRooks ^= fromBit;
					this.blackRooks |= toBit;
				}
				break;
				case 10: {
					this.blackBishops ^= fromBit;
					this.blackBishops |= toBit;
				}
				break;
				case 11: {
					this.blackKnights ^= fromBit;
					this.blackKnights |= toBit;
				}
				break;
				case 12: {
					this.blackPawns ^= fromBit;
					this.blackPawns |= toBit;
				}
				break;
			}
			switch (captured) {
				case 0:
				break;
				case 2:
					this.whiteQueens ^= toBit;
				break;
				case 3:
					this.whiteRooks ^= toBit;
				break;
				case 4:
					this.whiteBishops ^= toBit;
				break;
				case 5:
					this.whiteKnights ^= toBit;
				break;
				case 6:
					this.whitePawns ^= toBit;
				break;
			}
			this.allBlackPieces 	 ^=  fromBit;
			this.allBlackPieces 	 |=  toBit;
			this.allWhitePieces 	 &= ~toBit;
			this.allNonWhiteOccupied  = ~this.allWhitePieces;
			this.allNonBlackOccupied  = ~this.allBlackPieces;
			this.allOccupied 		  =  this.allWhitePieces | this.allBlackPieces;
			this.allEmpty			  = ~this.allOccupied;
		}
	}
	private void setTurn() {
		this.whitesTurn = !this.whitesTurn;
	}
	private void setMoveIndices() {
		this.moveIndex++;
		long lastMove 	= this.moveList.getHead();
		long moved		= ((lastMove >>> Move.MOVED_PIECE.shift) 		& Move.MOVED_PIECE.mask);
		long captured 	= ((lastMove >>> Move.CAPTURED_PIECE.shift) 	& Move.CAPTURED_PIECE.mask);
		if (captured != 0 || moved == 6 || moved == 12)
			this.fiftyMoveRuleClock = 0;
		else
			this.fiftyMoveRuleClock++;
	}
	/**Should be used after resetKeys().*/
	private void resetMoveIndices() {
		this.moveIndex--;
		long lastMove 					= this.moveList.getHead();
		this.fiftyMoveRuleClock			= (int)((lastMove >>> Move.PREVIOUS_FIFTY_MOVE_RULE_INDEX.shift) & Move.PREVIOUS_FIFTY_MOVE_RULE_INDEX.mask);
	}
	private void setEnPassantRights() {
		long lastMove 	= this.moveList.getHead();
		long from		= ((lastMove >>> Move.FROM.shift) 			& Move.FROM.mask);
		long to	  		= ((lastMove >>> Move.TO.shift) 			& Move.TO.mask);
		long movedPiece = ((lastMove >>> Move.MOVED_PIECE.shift) 	& Move.MOVED_PIECE.mask);
		if (movedPiece == 6) {
			if (to - from == 16) {
				this.enPassantRights = (int)to%8;
				return;
			}
		}
		else if (movedPiece == 12) {
			if (from - to == 16) {
				this.enPassantRights = (int)to%8;
				return;
			}
		}
		this.enPassantRights = 8;
	}
	private void resetEnPassantRights() {
		this.enPassantRights = (int)(this.moveList.getHead() >>> Move.PREVIOUS_ENPASSANT_RIGHTS.shift & Move.PREVIOUS_ENPASSANT_RIGHTS.mask);
	}
	private void setCastlingRights() {
		if (this.whitesTurn) {
			if (this.whiteCastlingRights != 0) {
				if (this.whiteCastlingRights == 1) {
					if (this.offsetBoard[4] != 1 || this.offsetBoard[7] != 3)
						this.whiteCastlingRights = 0;
				}
				else if (this.whiteCastlingRights == 2) {
					if (this.offsetBoard[4] != 1 || this.offsetBoard[0] != 3)
						this.whiteCastlingRights = 0;
				}
				else {
					if (this.offsetBoard[4] != 1) {
						this.whiteCastlingRights = 0;
					}
					else if (this.offsetBoard[0] != 3) {
						if (this.offsetBoard[7] == 3)
							this.whiteCastlingRights = 2;
						else
							this.whiteCastlingRights = 0;
					}
					else if (this.offsetBoard[7] != 3) {
						this.whiteCastlingRights = 1;
					}
				}
			}
		}
		else {
			if (this.blackCastlingRights != 0) {
				if (this.blackCastlingRights == 1) {
					if (this.offsetBoard[60] != 1 || this.offsetBoard[63] != 3)
						this.blackCastlingRights = 0;
				}
				else if (this.blackCastlingRights == 2) {
					if (this.offsetBoard[60] != 1 || this.offsetBoard[56] != 3)
						this.blackCastlingRights = 0;
				}
				else {
					if (this.offsetBoard[60] != 1) {
						this.blackCastlingRights = 0;
					}
					else if (this.offsetBoard[56] != 3) {
						if (this.offsetBoard[63] == 3)
							this.blackCastlingRights = 2;
						else
							this.blackCastlingRights = 0;
					}
					else if (this.offsetBoard[63] != 3) {
						this.blackCastlingRights = 1;
					}
				}
			}
		}
	}
	private void resetCastlingRights() {
		long lastMove = this.moveList.getHead();
		this.whiteCastlingRights = (int)(lastMove >>> Move.PREVIOUS_WHITE_CASTLING_RIGHTS.shift & Move.PREVIOUS_WHITE_CASTLING_RIGHTS.mask);
		this.blackCastlingRights = (int)(lastMove >>> Move.PREVIOUS_BLACK_CASTLING_RIGHTS.shift & Move.PREVIOUS_BLACK_CASTLING_RIGHTS.mask);
	}
	private void setCheck() {
		if (this.whitesTurn) {
			this.checkers = getAttackers(BitOperations.indexOfLSBit(this.whiteKing), false);
			if (this.checkers == 0)
				this.check = false;
			else
				this.check = true;
		}
		else {
			this.checkers = getAttackers(BitOperations.indexOfLSBit(this.blackKing), true);
			if (this.checkers == 0)
				this.check = false;
			else
				this.check = true;
		}
	}
	private void resetCheck() {
		this.check = (this.moveList.getHead() >>> Move.PREVIOUS_CHECK.shift & Move.PREVIOUS_CHECK.mask) == 1 ? true : false;
	}
	private void setKeys() {
		this.zobristKey = keyGen.updateKey(this);
		this.zobristKeyHistory[this.moveIndex] = this.zobristKey;
	}
	/**Should be used before resetMoveIndices().*/
	private void resetKeys() {
		this.zobristKeyHistory[this.moveIndex] = 0;
		this.zobristKey = this.zobristKeyHistory[this.moveIndex - 1];
	}
	private void setRepetitions() {
		if ((this.fiftyMoveRuleClock) >= 4) {
			for (int i = this.moveIndex; i >= (this.moveIndex - this.fiftyMoveRuleClock); i -= 2) {
				if (this.zobristKeyHistory[i] == this.zobristKey)
					this.repetitions++;
			}
		}
		else
			this.repetitions = 0;
	}
	public boolean isAttacked(int sqrInd, boolean byWhite) {
		if (byWhite) {
			MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
			if ((this.whiteKing		& dB.getCrudeKingMoves()) != 0)
				return true;
			if ((this.whiteKnights 	& dB.getCrudeKnightMoves()) != 0)
				return true;
			if ((this.whitePawns 	& dB.getCrudeBlackPawnCaptures()) != 0)
				return true;
			if (((this.whiteQueens | this.whiteRooks) 	& dB.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied)) != 0)
				return true;
			if (((this.whiteQueens | this.whiteBishops) & dB.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied)) != 0)
				return true;
			if (this.offsetBoard[sqrInd] == 12 && this.enPassantRights == sqrInd%8) {
				if ((this.whitePawns & dB.getCrudeKingMoves() & Rank.getByIndex(4)) != 0)
					return true;
			}
		}
		else {
			MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
			if ((this.blackKing		& dB.getCrudeKingMoves()) != 0)
				return true;
			if ((this.blackKnights 	& dB.getCrudeKnightMoves()) != 0)
				return true;
			if ((this.blackPawns 	& dB.getCrudeWhitePawnCaptures()) != 0)
				return true;
			if (((this.blackQueens | this.blackRooks) 	& dB.getBlackRookMoves(this.allNonWhiteOccupied, this.allOccupied)) != 0)
				return true;
			if (((this.blackQueens | this.blackBishops) & dB.getBlackBishopMoves(this.allNonWhiteOccupied, this.allOccupied)) != 0)
				return true;
			if (this.offsetBoard[sqrInd] == 6 && this.enPassantRights == sqrInd%8) {
				if ((this.blackPawns & dB.getCrudeKingMoves() & Rank.getByIndex(3)) != 0)
					return true;
			}
		}
		return false;
	}
	public long getAttackers(int sqrInd, boolean byWhite) {
		long attackers = 0;
		if (byWhite) {
			MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
			attackers  =  this.whiteKing						& dB.getCrudeKingMoves();
			attackers |=  this.whiteKnights						& dB.getCrudeKnightMoves();
			attackers |=  this.whitePawns 						& dB.getCrudeBlackPawnCaptures();
			attackers |= (this.whiteQueens | this.whiteRooks)	& dB.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied);
			attackers |= (this.whiteQueens | this.whiteBishops) & dB.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied);
			if (this.offsetBoard[sqrInd] == 12 && this.enPassantRights == sqrInd%8)
				attackers |=  this.whitePawns & dB.getCrudeKingMoves() & Rank.getByIndex(4);
		}
		else {
			MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
			attackers  =  this.blackKing						& dB.getCrudeKingMoves();
			attackers |=  this.blackKnights						& dB.getCrudeKnightMoves();
			attackers |=  this.blackPawns 						& dB.getCrudeWhitePawnCaptures();
			attackers |= (this.blackQueens | this.blackRooks)	& dB.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied);
			attackers |= (this.blackQueens | this.blackBishops) & dB.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied);
			if (this.offsetBoard[sqrInd] == 6 && this.enPassantRights == sqrInd%8)
				attackers |=  this.blackPawns & dB.getCrudeKingMoves() & Rank.getByIndex(3);
		}
		return attackers;
	}
	public long getBlockerCandidates(int sqrInd, boolean byWhite) {
		long blockerCandidates = 0;
		long sqrBit = Square.getBitmapByIndex(sqrInd);
		long blackPawnAdvance = BitOperations.vShiftDown(sqrBit), whitePawnAdvance = BitOperations.vShiftUp(sqrBit);
		if (byWhite) {
			MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
			blockerCandidates |=  this.whiteKnights						& dB.getCrudeKnightMoves();
			blockerCandidates |=  this.whitePawns 						& blackPawnAdvance;
			if ((sqrBit & Rank.getByIndex(3)) != 0 && (this.allEmpty & blackPawnAdvance) != 0)
				blockerCandidates |=  this.whitePawns 					& BitOperations.vShiftDown(blackPawnAdvance);
			blockerCandidates |= (this.whiteQueens | this.whiteRooks)	& dB.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied);
			blockerCandidates |= (this.whiteQueens | this.whiteBishops) & dB.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied);
			if (this.enPassantRights == sqrInd%8 && (sqrBit & Rank.getByIndex(5)) != 0)
				blockerCandidates |=  this.whitePawns & dB.getCrudeBlackPawnCaptures();
		}
		else {
			MoveDatabase dB = MoveDatabase.getByIndex(sqrInd);
			blockerCandidates |=  this.blackKnights						& dB.getCrudeKnightMoves();
			blockerCandidates |=  this.blackPawns 						& whitePawnAdvance;
			if ((sqrBit & Rank.getByIndex(4)) != 0 && (this.allEmpty & whitePawnAdvance) != 0)
				blockerCandidates |=  this.blackPawns 					& BitOperations.vShiftUp(whitePawnAdvance);
			blockerCandidates |= (this.blackQueens | this.blackRooks)	& dB.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied);
			blockerCandidates |= (this.blackQueens | this.blackBishops) & dB.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied);
			if (this.enPassantRights == sqrInd%8 && (sqrBit & Rank.getByIndex(2)) != 0)
				blockerCandidates |=  this.blackPawns & dB.getCrudeWhitePawnCaptures();
		}
		return blockerCandidates;
	}
	public long getPinnedPieces(boolean forWhite) {
		long rankPos, rankNeg, filePos, fileNeg, diagonalPos, diagonalNeg, antiDiagonalPos, antiDiagonalNeg;
		long straightSliders, diagonalSliders;
		SliderAttackRayMask attRayMask;
		int sqrInd;
		long pinnedPiece;
		long pinnedPieces = 0;
		if (forWhite) {
			sqrInd = BitOperations.indexOfLSBit(this.whiteKing);
			straightSliders = this.blackQueens | this.blackRooks;
			diagonalSliders = this.blackQueens | this.blackBishops;
			attRayMask 		= SliderAttackRayMask.getByIndex(sqrInd);
			rankPos 		= attRayMask.getRankPos() 			& this.allOccupied;
			rankNeg 		= attRayMask.getRankNeg() 			& this.allOccupied;
			filePos 		= attRayMask.getFilePos() 			& this.allOccupied;
			fileNeg 		= attRayMask.getFileNeg() 			& this.allOccupied;
			diagonalPos 	= attRayMask.getDiagonalPos() 		& this.allOccupied;
			diagonalNeg 	= attRayMask.getDiagonalNeg() 		& this.allOccupied;
			antiDiagonalPos = attRayMask.getAntiDiagonalPos() 	& this.allOccupied;
			antiDiagonalNeg = attRayMask.getAntiDiagonalNeg() 	& this.allOccupied;
			if ((pinnedPiece = BitOperations.getLSBit(rankPos)			 & this.allWhitePieces) != 0) {
				if ((BitOperations.getLSBit(rankPos^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(filePos)			 & this.allWhitePieces) != 0) {
				if ((BitOperations.getLSBit(filePos^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos)	 	 & this.allWhitePieces) != 0) {
				if ((BitOperations.getLSBit(diagonalPos^pinnedPiece) 	 & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) 	 & this.allWhitePieces) != 0) {
				if ((BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg)		 	 & this.allWhitePieces) != 0) {
				if ((BitOperations.getMSBit(rankNeg^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg)			 & this.allWhitePieces) != 0) {
				if ((BitOperations.getMSBit(fileNeg^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg)	 	 & this.allWhitePieces) != 0) {
				if ((BitOperations.getMSBit(diagonalNeg^pinnedPiece) 	 & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalNeg) 	 & this.allWhitePieces) != 0) {
				if ((BitOperations.getMSBit(antiDiagonalNeg^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
		}
		else {
			sqrInd = BitOperations.indexOfLSBit(this.blackKing);
			straightSliders = this.whiteQueens | this.whiteRooks;
			diagonalSliders = this.whiteQueens | this.whiteBishops;
			attRayMask		= SliderAttackRayMask.getByIndex(sqrInd);
			rankPos 		= attRayMask.getRankPos() 			& this.allOccupied;
			rankNeg 		= attRayMask.getRankNeg() 			& this.allOccupied;
			filePos 		= attRayMask.getFilePos() 			& this.allOccupied;
			fileNeg 		= attRayMask.getFileNeg() 			& this.allOccupied;
			diagonalPos 	= attRayMask.getDiagonalPos() 		& this.allOccupied;
			diagonalNeg 	= attRayMask.getDiagonalNeg() 		& this.allOccupied;
			antiDiagonalPos = attRayMask.getAntiDiagonalPos() 	& this.allOccupied;
			antiDiagonalNeg = attRayMask.getAntiDiagonalNeg() 	& this.allOccupied;
			if ((pinnedPiece = BitOperations.getLSBit(rankPos)			 & this.allBlackPieces) != 0) {
				if ((BitOperations.getLSBit(rankPos^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(filePos)			 & this.allBlackPieces) != 0) {
				if ((BitOperations.getLSBit(filePos^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos)	 	 & this.allBlackPieces) != 0) {
				if ((BitOperations.getLSBit(diagonalPos^pinnedPiece) 	 & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) 	 & this.allBlackPieces) != 0) {
				if ((BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg)		 	 & this.allBlackPieces) != 0) {
				if ((BitOperations.getMSBit(rankNeg^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg)			 & this.allBlackPieces) != 0) {
				if ((BitOperations.getMSBit(fileNeg^pinnedPiece) 		 & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg)	 	 & this.allBlackPieces) != 0) {
				if ((BitOperations.getMSBit(diagonalNeg^pinnedPiece) 	 & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalNeg) 	 & this.allBlackPieces) != 0) {
				if ((BitOperations.getMSBit(antiDiagonalNeg^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
		}
		return pinnedPieces;
	}
	private LongQueue generateNormalMoves() {
		long pinnedPieces, movablePieces, pieceSet, moveSet;
		int king, queen, rook, bishop, knight, pawn;
		IntStack queens, rooks, bishops, knights, pawns;
		long kingMove = 0, queenMove = 0, rookMove = 0, bishopMove = 0, knightMove = 0, pawnMove = 0;
		IntStack kingMoves, queenMoves, rookMoves, bishopMoves, knightMoves, pawnMoves;
		LongQueue moves = new LongQueue();
		long move = 0;
		int to;
		move |= (this.whiteCastlingRights		<< Move.PREVIOUS_WHITE_CASTLING_RIGHTS.shift);
		move |= (this.blackCastlingRights		<< Move.PREVIOUS_BLACK_CASTLING_RIGHTS.shift);
		move |= (this.enPassantRights	 		<< Move.PREVIOUS_ENPASSANT_RIGHTS.shift);
		move |= (this.check	? 1 : 0				<< Move.PREVIOUS_CHECK.shift);
		move |= (this.fiftyMoveRuleClock		<< Move.PREVIOUS_FIFTY_MOVE_RULE_INDEX.shift);
		move |= (this.repetitions				<< Move.PREVIOUS_REPETITIONS.shift);
		if (this.whitesTurn) {
			pinnedPieces  =  getPinnedPieces(true);
			movablePieces = ~pinnedPieces;
			king = BitOperations.indexOfLSBit(this.whiteKing);
			kingMove  = move;
			kingMove |= king;
			kingMove |= (1 << Move.MOVED_PIECE.shift);
			moveSet	  = MoveDatabase.getByIndex(king).getWhiteKingMoves(this.allNonWhiteOccupied);
			if (moveSet != 0) {
				kingMoves = BitOperations.serialize(moveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false)) {
						moves.add(kingMove | (to << Move.TO.getShift()) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
					}
				}
				if ((this.whiteCastlingRights & 2) != 0) {
					if (((Square.getBitmapByIndex(1) | Square.getBitmapByIndex(2) | Square.getBitmapByIndex(3)) & this.allOccupied) == 0) {
						if (((moves.getTail() >>> Move.TO.shift) & Move.TO.mask) == 3 && !isAttacked(2, false))
							moves.add(kingMove | (1 << Move.TO.shift) | (2 << Move.TYPE.shift));
					}
				}
				if ((this.whiteCastlingRights & 1) != 0) {
					if (((Square.getBitmapByIndex(5) | Square.getBitmapByIndex(6)) & this.allOccupied) == 0) {
						if (!isAttacked(5, false) && !isAttacked(6, false))
							moves.add(kingMove | (6 << Move.TO.shift) | (1 << Move.TYPE.shift));
					}
				}
			}
			pieceSet = this.whiteQueens & movablePieces;
			if (pieceSet != 0) {
				queens = BitOperations.serialize(pieceSet);
				while (queens.hasNext()) {
					queen = queens.next();
					queenMove  = move;
					queenMove |= queen;
					queenMove |= (2 << Move.MOVED_PIECE.shift);
					moveSet	   = MoveDatabase.getByIndex(queen).getWhiteQueenMoves(this.allNonWhiteOccupied, this.allOccupied);
					if (moveSet != 0) {
						queenMoves = BitOperations.serialize(moveSet);
						while (queenMoves.hasNext()) {
							to = queenMoves.next();
							moves.add(queenMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			pieceSet = this.whiteRooks & movablePieces;
			if (pieceSet != 0) {
				rooks	  = BitOperations.serialize(pieceSet);
				while (rooks.hasNext()) {
					rook = rooks.next();
					rookMove  = move;
					rookMove |= rook;
					rookMove |= (3 << Move.MOVED_PIECE.shift);
					moveSet	  = MoveDatabase.getByIndex(rook).getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied);
					if (moveSet != 0) {
						rookMoves = BitOperations.serialize(moveSet);
						while (rookMoves.hasNext()) {
							to = rookMoves.next();
							moves.add(rookMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			pieceSet = this.whiteBishops & movablePieces;
			if (pieceSet != 0) {
				bishops = BitOperations.serialize(pieceSet);
				while (bishops.hasNext()) {
					bishop = bishops.next();
					bishopMove  = move;
					bishopMove |= bishop;
					bishopMove |= (4 << Move.MOVED_PIECE.shift);
					moveSet		= MoveDatabase.getByIndex(bishop).getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied);
					if (moveSet != 0) {
						bishopMoves = BitOperations.serialize(moveSet);
						while (bishopMoves.hasNext()) {
							to = bishopMoves.next();
							moves.add(bishopMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			pieceSet = this.whiteKnights & movablePieces;
			if (pieceSet != 0) {
				knights = BitOperations.serialize(pieceSet);
				while (knights.hasNext()) {
					knight = knights.next();
					knightMove  = move;
					knightMove |= knight;
					knightMove |= (5 << Move.MOVED_PIECE.shift);
					moveSet		= MoveDatabase.getByIndex(knight).getWhiteKnightMoves(this.allNonWhiteOccupied);
					if (moveSet != 0) {
						knightMoves = BitOperations.serialize(moveSet);
						while (knightMoves.hasNext()) {
							to = knightMoves.next();
							moves.add(knightMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			pieceSet = this.whitePawns & movablePieces;
			if (pieceSet != 0) {
				pawns = BitOperations.serialize(pieceSet);
				while (pawns.hasNext()) {
					pawn = pawns.next();
					pawnMove  = move;
					pawnMove |= pawn;
					pawnMove |= (6 << Move.MOVED_PIECE.shift);
					if (this.enPassantRights != 8) {
						to = 40 + this.enPassantRights;
						if ((MoveDatabase.getByIndex(to).getCrudeBlackPawnCaptures() & pawn) != 0)
							moves.add(pawnMove | (to << Move.TO.shift) | (12 << Move.CAPTURED_PIECE.shift) | (3 << Move.TYPE.shift));
					}
					moveSet = MoveDatabase.getByIndex(pawn).getWhitePawnMoves(this.allBlackPieces, this.allEmpty);
					if (moveSet != 0) {
						pawnMoves = BitOperations.serialize(moveSet);
						while (pawnMoves.hasNext()) {
							to = pawnMoves.next();
							if (to > 55) {
								moves.add(pawnMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift) | (4 << Move.TYPE.shift));
								moves.add(pawnMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift) | (5 << Move.TYPE.shift));
								moves.add(pawnMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift) | (6 << Move.TYPE.shift));
								moves.add(pawnMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift) | (7 << Move.TYPE.shift));
							}
							else
								moves.add(pawnMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
		}
		else {
			pinnedPieces  =  getPinnedPieces(false);
			movablePieces = ~pinnedPieces;
			king = BitOperations.indexOfLSBit(this.blackKing);
			kingMove  = move;
			kingMove |= king;
			kingMove |= (7 << Move.MOVED_PIECE.shift);
			moveSet	  = MoveDatabase.getByIndex(king).getBlackKingMoves(this.allNonBlackOccupied);
			if (moveSet != 0) {
				kingMoves = BitOperations.serialize(moveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true)) {
						moves.add(kingMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
					}
				}
				if ((this.whiteCastlingRights & 1) != 0) {
					if (((Square.getBitmapByIndex(61) | Square.getBitmapByIndex(62)) & this.allOccupied) == 0) {
						if (((moves.getHead() >>> Move.TO.shift) & Move.TO.mask) == 61 && !isAttacked(62, true))
							moves.add(kingMove | (62 << Move.TO.shift) | (1 << Move.TYPE.shift));
					}
				}
				if ((this.whiteCastlingRights & 2) != 0) {
					if (((Square.getBitmapByIndex(57) | Square.getBitmapByIndex(58) | Square.getBitmapByIndex(59)) & this.allOccupied) == 0) {
						if (!isAttacked(58, true) && !isAttacked(59, true))
							moves.add(kingMove | (58 << Move.TO.shift) | (2 << Move.TYPE.shift));
					}
				}
			}
			pieceSet = this.blackQueens & movablePieces;
			if (pieceSet != 0) {
				queens = BitOperations.serialize(pieceSet);
				while (queens.hasNext()) {
					queen = queens.next();
					queenMove  = move;
					queenMove |= queen;
					queenMove |= (8 << Move.MOVED_PIECE.shift);
					moveSet	   = MoveDatabase.getByIndex(queen).getBlackQueenMoves(this.allNonBlackOccupied, this.allOccupied);
					if (moveSet != 0) {
						queenMoves = BitOperations.serialize(moveSet);
						while (queenMoves.hasNext()) {
							to = queenMoves.next();
							moves.add(queenMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			pieceSet = this.blackRooks & movablePieces;
			if (pieceSet != 0) {
				rooks = BitOperations.serialize(pieceSet);
				while (rooks.hasNext()) {
					rook = rooks.next();
					rookMove  = move;
					rookMove |= rook;
					rookMove |= (9 << Move.MOVED_PIECE.shift);
					moveSet	  = MoveDatabase.getByIndex(rook).getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied);
					if (moveSet != 0) {
						rookMoves = BitOperations.serialize(moveSet);
						while (rookMoves.hasNext()) {
							to = rookMoves.next();
							moves.add(rookMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			pieceSet = this.blackBishops & movablePieces;
			if (pieceSet != 0) {
				bishops = BitOperations.serialize(pieceSet);
				while (bishops.hasNext()) {
					bishop = bishops.next();
					bishopMove  = move;
					bishopMove |= bishop;
					bishopMove |= (10 << Move.MOVED_PIECE.shift);
					moveSet		= MoveDatabase.getByIndex(bishop).getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied);
					if (moveSet != 0) {
						bishopMoves = BitOperations.serialize(moveSet);
						while (bishopMoves.hasNext()) {
							to = bishopMoves.next();
							moves.add(bishopMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			pieceSet = this.blackKnights & movablePieces;
			if (pieceSet != 0) {
				knights = BitOperations.serialize(pieceSet);
				while (knights.hasNext()) {
					knight = knights.next();
					knightMove  = move;
					knightMove |= knight;
					knightMove |= (11 << Move.MOVED_PIECE.shift);
					moveSet		= MoveDatabase.getByIndex(knight).getBlackKnightMoves(this.allNonBlackOccupied);
					if (moveSet != 0) {
						knightMoves = BitOperations.serialize(moveSet);
						while (knightMoves.hasNext()) {
							to = knightMoves.next();
							moves.add(knightMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
			pieceSet = this.blackPawns & movablePieces;
			if (pieceSet != 0) {
				pawns = BitOperations.serialize(pieceSet);
				while (pawns.hasNext()) {
					pawn = pawns.next();
					pawnMove  = move;
					pawnMove |= pawn;
					pawnMove |= (12 << Move.MOVED_PIECE.shift);
					if (this.enPassantRights != 8) {
						to = 24 + this.enPassantRights;
						if ((MoveDatabase.getByIndex(to).getCrudeWhitePawnCaptures() & pawn) != 0)
							moves.add(pawnMove | (to << Move.TO.shift) | (6 << Move.CAPTURED_PIECE.shift) | (3 << Move.TYPE.shift));
					}
					moveSet = MoveDatabase.getByIndex(pawn).getBlackPawnMoves(this.allWhitePieces, this.allEmpty);
					if (moveSet != 0) {
						pawnMoves = BitOperations.serialize(moveSet);
						while (pawnMoves.hasNext()) {
							to = pawnMoves.next();
							if (to < 8) {
								moves.add(pawnMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift) | (4 << Move.TYPE.shift));
								moves.add(pawnMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift) | (5 << Move.TYPE.shift));
								moves.add(pawnMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift) | (6 << Move.TYPE.shift));
								moves.add(pawnMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift) | (7 << Move.TYPE.shift));
							}
							else
								moves.add(pawnMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
						}
					}
				}
			}
		}
		return moves;
	}
	private LongQueue generateCheckEvasionMoves() {
		long move = 0, kingMove = 0, kingMoveSet, pinnedPieces, movablePieces, squaresOfInterventionSet, checkerAttackerSet, checkerBlockerSet, checkerAttackerMove, checkerBlockerMove;
		int checker1, checker2, checkerPiece1, checkerPiece2, squareOfIntervention, checkerAttackerSquare, checkerBlockerSquare;
		int king;
		IntStack kingMoves, squaresOfIntervention, checkerAttackers, checkerBlockers;
		LongQueue moves = new LongQueue();
		MoveDatabase dB, kingDb;
		boolean promotionOnAttackPossible = false, promotionOnBlockPossible = false;
		int to;
		move |= (this.whiteCastlingRights		<< Move.PREVIOUS_WHITE_CASTLING_RIGHTS.shift);
		move |= (this.blackCastlingRights		<< Move.PREVIOUS_BLACK_CASTLING_RIGHTS.shift);
		move |= (this.enPassantRights	 		<< Move.PREVIOUS_ENPASSANT_RIGHTS.shift);
		move |= (this.check	? 1 : 0				<< Move.PREVIOUS_CHECK.shift);
		move |= (this.fiftyMoveRuleClock		<< Move.PREVIOUS_FIFTY_MOVE_RULE_INDEX.shift);
		move |= (this.repetitions				<< Move.PREVIOUS_REPETITIONS.shift);
		if (this.whitesTurn) {
			if (BitOperations.resetLSBit(this.checkers) == 0) {
				pinnedPieces 	 =  getPinnedPieces(true);
				movablePieces	 = ~pinnedPieces;
				checker1  		 = BitOperations.indexOfLSBit(this.checkers);
				checkerPiece1 	 = this.offsetBoard[checker1];
				king 	  		 = BitOperations.indexOfLSBit(this.whiteKing);
				kingMove 		 = move;
				kingMove		|= king;
				kingMove		|= (1 << Move.MOVED_PIECE.shift);
				kingDb			 = MoveDatabase.getByIndex(king);
				kingMoveSet		 = kingDb.getWhiteKingMoves(this.allNonWhiteOccupied);
				dB = MoveDatabase.getByIndex(checker1);
				if ((this.checkers & Rank.getByIndex(7)) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, true) & movablePieces & ~this.whiteKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					checkerAttackerMove = move | checkerAttackerSquare | (checker1 << Move.TO.shift) | (this.offsetBoard[checkerAttackerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[checker1] << Move.CAPTURED_PIECE.shift);
					if ((checkerAttackerMove >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 6) {
						if (promotionOnAttackPossible) {
							moves.add(checkerAttackerMove | (4 << Move.TYPE.shift));
							moves.add(checkerAttackerMove | (5 << Move.TYPE.shift));
							moves.add(checkerAttackerMove | (6 << Move.TYPE.shift));
							moves.add(checkerAttackerMove | (7 << Move.TYPE.shift));
						}
						else if (checkerPiece1 == 12 && (Rank.getBySquareIndex(checker1) & Square.getBitmapByIndex(checkerAttackerSquare)) != 0)
							moves.add(((checkerAttackerMove | (Move.TO.mask << Move.TO.shift))^(Move.TO.mask << Move.TO.shift)) | ((checker1 + 8) << Move.TO.shift) | (3 << Move.TYPE.shift));
					}
					else
						moves.add(checkerAttackerMove);
				}
				switch (checkerPiece1) {
					case 8: {
						if ((File.getBySquareIndex(king) & this.checkers) != 0 || (Rank.getBySquareIndex(king) & this.checkers) != 0) {
							squaresOfInterventionSet = (dB.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied) & kingDb.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied));
							if (promotionOnAttackPossible && (this.whiteKing & Rank.getByIndex(7)) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied) & kingDb.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							if (checkerBlockerSet != 0) {
								checkerBlockers = BitOperations.serialize(checkerBlockerSet);
								while (checkerBlockers.hasNext()) {
									checkerBlockerSquare = checkerBlockers.next();
									checkerBlockerMove = move | checkerBlockerSquare | (squareOfIntervention << Move.TO.shift) | (this.offsetBoard[checkerBlockerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[squareOfIntervention] << Move.CAPTURED_PIECE.shift);
									if (promotionOnBlockPossible && (checkerBlockerMove >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 6) {
										moves.add(checkerBlockerMove | (4 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (5 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (6 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (7 << Move.TYPE.shift));
									}
									else
										moves.add(checkerBlockerMove);
								}
							}
						}
						kingMoveSet &= ~dB.getBlackQueenMoves(this.allNonBlackOccupied, (this.allOccupied^this.whiteKing));
					}
					break;
					case 9: {
						if (promotionOnAttackPossible && (this.whiteKing & Rank.getByIndex(7)) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied) & kingDb.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							if (checkerBlockerSet != 0) {
								checkerBlockers = BitOperations.serialize(checkerBlockerSet);
								while (checkerBlockers.hasNext()) {
									checkerBlockerSquare = checkerBlockers.next();
									checkerBlockerMove = move | checkerBlockerSquare | (squareOfIntervention << Move.TO.shift) | (this.offsetBoard[checkerBlockerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[squareOfIntervention] << Move.CAPTURED_PIECE.shift);
									if (promotionOnBlockPossible && (checkerBlockerMove >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 6) {
										moves.add(checkerBlockerMove | (4 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (5 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (6 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (7 << Move.TYPE.shift));
									}
									else
										moves.add(checkerBlockerMove);
								}
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 10: {
						squaresOfInterventionSet = (dB.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied) & kingDb.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							if (checkerBlockerSet != 0) {
								checkerBlockers = BitOperations.serialize(checkerBlockerSet);
								while (checkerBlockers.hasNext()) {
									checkerBlockerSquare = checkerBlockers.next();
									checkerBlockerMove = move | checkerBlockerSquare | (squareOfIntervention << Move.TO.shift) | (this.offsetBoard[checkerBlockerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[squareOfIntervention] << Move.CAPTURED_PIECE.shift);
									if (promotionOnBlockPossible && (checkerBlockerMove >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 6) {
										moves.add(checkerBlockerMove | (4 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (5 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (6 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (7 << Move.TYPE.shift));
									}
									else
										moves.add(checkerBlockerMove);
								}
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				if (kingMoveSet != 0) {
					kingMoves = BitOperations.serialize(kingMoveSet);
					while (kingMoves.hasNext()) {
						to = kingMoves.next();
						if (!isAttacked(to, false))
							moves.add(kingMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
					}
				}
			}
			else {
				king 	  		= BitOperations.indexOfLSBit(this.whiteKing);
				kingMove	   |= move;
				kingMove 	   |= king;
				kingMove 	   |= (1 << Move.MOVED_PIECE.shift);
				kingMoveSet		= MoveDatabase.getByIndex(king).getWhiteKingMoves(this.allNonWhiteOccupied);
				checker1 		= BitOperations.indexOfLSBit(this.checkers);
				checkerPiece1 	= this.offsetBoard[checker1];
				checker2		= BitOperations.indexOfLSBit(BitOperations.resetLSBit(this.checkers));
				checkerPiece2 	= this.offsetBoard[checker2];
				dB = MoveDatabase.getByIndex(checker1);
				switch (checkerPiece1) {
					case 8:
						kingMoveSet &= ~dB.getBlackQueenMoves(this.allNonBlackOccupied, (this.allOccupied^this.whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 10:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 11:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				dB = MoveDatabase.getByIndex(checker2);
				switch (checkerPiece2) {
					case 8:
						kingMoveSet &= ~dB.getBlackQueenMoves(this.allNonBlackOccupied, (this.allOccupied^this.whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 10:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 11:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				if (kingMoveSet != 0) {
					kingMoves = BitOperations.serialize(kingMoveSet);
					while (kingMoves.hasNext()) {
						to = kingMoves.next();
						if (!isAttacked(to, false))
							moves.add(kingMove | (to << Move.TO.getShift()) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
					}
				}
			}
		}
		else {
			if (BitOperations.resetLSBit(this.checkers) == 0) {
				pinnedPieces  	=  getPinnedPieces(false);
				movablePieces 	= ~pinnedPieces;
				checker1  		= BitOperations.indexOfLSBit(this.checkers);
				checkerPiece1	= this.offsetBoard[checker1];
				king 	  		= BitOperations.indexOfLSBit(this.blackKing);
				kingMove 	    = move;
				kingMove 	   |= king;
				kingMove 	   |= (7 << Move.MOVED_PIECE.shift);
				kingDb	 		= MoveDatabase.getByIndex(king);
				kingMoveSet		= kingDb.getBlackKingMoves(this.allNonBlackOccupied);
				dB				= MoveDatabase.getByIndex(checker1);
				if ((this.checkers & Rank.getByIndex(0)) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, false) & movablePieces & ~this.blackKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					checkerAttackerMove = move | checkerAttackerSquare | (checker1 << Move.TO.shift) | (this.offsetBoard[checkerAttackerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[checker1] << Move.CAPTURED_PIECE.shift);
					if ((checkerAttackerMove >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 12) {
						if (promotionOnAttackPossible) {
							moves.add(checkerAttackerMove | (4 << Move.TYPE.shift));
							moves.add(checkerAttackerMove | (5 << Move.TYPE.shift));
							moves.add(checkerAttackerMove | (6 << Move.TYPE.shift));
							moves.add(checkerAttackerMove | (7 << Move.TYPE.shift));
						}
						else if (checkerPiece1 == 6 && (Rank.getBySquareIndex(checker1) & Square.getBitmapByIndex(checkerAttackerSquare)) != 0)
							moves.add(((checkerAttackerMove | (Move.TO.mask << Move.TO.shift))^(Move.TO.mask << Move.TO.shift)) | ((checker1 - 8) << Move.TO.shift) | (3 << Move.TYPE.shift));
					}
					else
						moves.add(checkerAttackerMove);
				}
				switch (checkerPiece1) {
					case 2: {
						if ((File.getBySquareIndex(king) & this.checkers) != 0 || (Rank.getBySquareIndex(king) & this.checkers) != 0) {
							squaresOfInterventionSet = (dB.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied) & kingDb.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied));
							if (promotionOnAttackPossible && (this.blackKing & Rank.getByIndex(0)) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied) & kingDb.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							if (checkerBlockerSet != 0) {
								checkerBlockers = BitOperations.serialize(checkerBlockerSet);
								while (checkerBlockers.hasNext()) {
									checkerBlockerSquare = checkerBlockers.next();
									checkerBlockerMove = move | checkerBlockerSquare | (squareOfIntervention << Move.TO.shift) | (this.offsetBoard[checkerBlockerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[squareOfIntervention] << Move.CAPTURED_PIECE.shift);
									if (promotionOnBlockPossible && (checkerBlockerMove >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 12) {
										moves.add(checkerBlockerMove | (4 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (5 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (6 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (7 << Move.TYPE.shift));
									}
									else
										moves.add(checkerBlockerMove);
								}
							}
						}
						kingMoveSet &= ~dB.getWhiteQueenMoves(this.allNonWhiteOccupied, (this.allOccupied^this.blackKing));
					}
					break;
					case 3: {
						if (promotionOnAttackPossible && (this.blackKing & Rank.getByIndex(0)) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getWhiteRookMoves(this.allNonWhiteOccupied, this.allOccupied) & kingDb.getBlackRookMoves(this.allNonBlackOccupied, this.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							if (checkerBlockerSet != 0) {
								checkerBlockers = BitOperations.serialize(checkerBlockerSet);
								while (checkerBlockers.hasNext()) {
									checkerBlockerSquare = checkerBlockers.next();
									checkerBlockerMove = move | checkerBlockerSquare | (squareOfIntervention << Move.TO.shift) | (this.offsetBoard[checkerBlockerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[squareOfIntervention] << Move.CAPTURED_PIECE.shift);
									if (promotionOnBlockPossible && (checkerBlockerMove >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 12) {
										moves.add(checkerBlockerMove | (4 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (5 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (6 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (7 << Move.TYPE.shift));
									}
									else
										moves.add(checkerBlockerMove);
								}
							}
						}
						kingMoveSet &= ~dB.getCrudeRookMoves();
					}
					break;
					case 4: {
						squaresOfInterventionSet = (dB.getWhiteBishopMoves(this.allNonWhiteOccupied, this.allOccupied) & kingDb.getBlackBishopMoves(this.allNonBlackOccupied, this.allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							if (checkerBlockerSet != 0) {
								checkerBlockers = BitOperations.serialize(checkerBlockerSet);
								while (checkerBlockers.hasNext()) {
									checkerBlockerSquare = checkerBlockers.next();
									checkerBlockerMove = move | checkerBlockerSquare | (squareOfIntervention << Move.TO.shift) | (this.offsetBoard[checkerBlockerSquare] << Move.MOVED_PIECE.shift) | (this.offsetBoard[squareOfIntervention] << Move.CAPTURED_PIECE.shift);
									if (promotionOnBlockPossible && (checkerBlockerMove >>> Move.MOVED_PIECE.shift & Move.MOVED_PIECE.mask) == 12) {
										moves.add(checkerBlockerMove | (4 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (5 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (6 << Move.TYPE.shift));
										moves.add(checkerBlockerMove | (7 << Move.TYPE.shift));
									}
									else
										moves.add(checkerBlockerMove);
								}
							}
						}
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					}
				}
				if (kingMoveSet != 0) {
					kingMoves = BitOperations.serialize(kingMoveSet);
					while (kingMoves.hasNext()) {
						to = kingMoves.next();
						if (!isAttacked(to, true))
							moves.add(kingMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
					}
				}
			}
			else {
				king			= BitOperations.indexOfLSBit(this.blackKing);
				kingMove 	   |= move;
				kingMove 	   |= king;
				kingMove 	   |= (7 << Move.MOVED_PIECE.shift);
				kingMoveSet 	= MoveDatabase.getByIndex(king).getBlackKingMoves(this.allNonBlackOccupied);
				checker1 		= BitOperations.indexOfLSBit(this.checkers);
				checkerPiece1 	= this.offsetBoard[checker1];
				checker2		= BitOperations.indexOfLSBit(BitOperations.resetLSBit(this.checkers));
				checkerPiece2 	= this.offsetBoard[checker2];
				dB				= MoveDatabase.getByIndex(checker1);
				switch (checkerPiece1) {
					case 2:
						kingMoveSet &= ~dB.getWhiteQueenMoves(this.allNonWhiteOccupied, (this.allOccupied^this.blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 4:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 5:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				dB = MoveDatabase.getByIndex(checker2);
				switch (checkerPiece2) {
					case 2:
						kingMoveSet &= ~dB.getWhiteQueenMoves(this.allNonWhiteOccupied, (this.allOccupied^this.blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getCrudeRookMoves();
					break;
					case 4:
						kingMoveSet &= ~dB.getCrudeBishopMoves();
					break;
					case 5:
						kingMoveSet &= ~dB.getCrudeKnightMoves();
				}
				if (kingMoveSet != 0) {
					kingMoves = BitOperations.serialize(kingMoveSet);
					while (kingMoves.hasNext()) {
						to = kingMoves.next();
						if (!isAttacked(to, true))
							moves.add(kingMove | (to << Move.TO.shift) | (this.offsetBoard[to] << Move.CAPTURED_PIECE.shift));
					}
				}
			}
		}
		return moves;
	}
	public LongQueue generateMoves() {
		if (this.check)
			return this.generateCheckEvasionMoves();
		else
			return this.generateNormalMoves();
	}
	public void makeMove(long move) {
		int from 			= (int)((move >>> Move.FROM.shift)		 	  & Move.FROM.mask);
		int to	 			= (int)((move >>> Move.TO.shift) 			  & Move.TO.mask);
		int moved			= (int)((move >>> Move.MOVED_PIECE.shift) 	  & Move.MOVED_PIECE.mask);
		int captured	 	= (int)((move >>> Move.CAPTURED_PIECE.shift)  & Move.CAPTURED_PIECE.mask);
		int type			= (int)((move >>> Move.TYPE.shift)  		  & Move.TYPE.mask);
		int square;
		long fromBit = Square.getBitmapByIndex(from);
		long toBit	 = Square.getBitmapByIndex(to);
		switch (type) {
			case 0: {
				this.offsetBoard[from]  = 0;
				this.offsetBoard[to]	= moved;
				this.setBitboards(moved, captured, fromBit, toBit);
			}
			break;
			case 1: {
				this.offsetBoard[from]   = 0;
				this.offsetBoard[to]	 = moved;
				this.setBitboards(moved, captured, fromBit, toBit);
				if (this.whitesTurn) {
					this.offsetBoard[7]	 = 0;
					this.offsetBoard[5]	 = 3;
					this.setBitboards(3, 0, Square.getBitmapByIndex(7), Square.getBitmapByIndex(5));
				}
				else {
					this.offsetBoard[63] = 0;
					this.offsetBoard[61] = 9;
					this.setBitboards(9, 0, Square.getBitmapByIndex(63), Square.getBitmapByIndex(61));
				}
			}
			break;
			case 2: {
				this.offsetBoard[from]   = 0;
				this.offsetBoard[to]	 = moved;
				this.setBitboards(moved, captured, fromBit, toBit);
				if (this.whitesTurn) {
					this.offsetBoard[0]	 = 0;
					this.offsetBoard[3]	 = 3;
					this.setBitboards(3, 0, Square.getBitmapByIndex(0), Square.getBitmapByIndex(3));
				}
				else {
					this.offsetBoard[56] = 0;
					this.offsetBoard[59] = 9;
					this.setBitboards(9, 0, Square.getBitmapByIndex(56), Square.getBitmapByIndex(59));
				}
			}
			break;
			case 3: {
				this.offsetBoard[from]   = 0;
				this.offsetBoard[to]	 = moved;
				this.setBitboards(moved, 0, fromBit, toBit);
				if (this.whitesTurn) {
					square = to - 8;
					this.offsetBoard[square] = 0;
				}
				else {
					square = to + 8;
					this.offsetBoard[square] = 0;
				}
				this.setBitboards(captured, 0, Square.getBitmapByIndex(square), 0);
			}
			break;
			case 4: {
				this.offsetBoard[from]   = 0;
				this.setBitboards(moved, captured, fromBit, 0);
				if (this.whitesTurn) {
					this.offsetBoard[to] = 2;
					this.setBitboards(2, 0, 0, toBit);
				}
				else {
					this.offsetBoard[to] = 8;
					this.setBitboards(8, 0, 0, toBit);
				}
			}
			break;
			case 5: {
				this.offsetBoard[from]   = 0;
				this.setBitboards(moved, captured, fromBit, 0);
				if (this.whitesTurn) {
					this.offsetBoard[to] = 3;
					this.setBitboards(3, 0, 0, toBit);
				}
				else {
					this.offsetBoard[to] = 9;
					this.setBitboards(9, 0, 0, toBit);
				}
			}
			break;
			case 6: {
				this.offsetBoard[from]   = 0;
				this.setBitboards(moved, captured, fromBit, 0);
				if (this.whitesTurn) {
					this.offsetBoard[to] = 4;
					this.setBitboards(4, 0, 0, toBit);
				}
				else {
					this.offsetBoard[to] = 10;
					this.setBitboards(10, 0, 0, toBit);
				}
			}
			break;
			case 7: {
				this.offsetBoard[from]   = 0;
				this.setBitboards(moved, captured, fromBit, 0);
				if (this.whitesTurn) {
					this.offsetBoard[to] = 5;
					this.setBitboards(5, 0, 0, toBit);
				}
				else {
					this.offsetBoard[to] = 11;
					this.setBitboards(11, 0, 0, toBit);
				}
			}
		}
		this.setTurn();
		this.moveList.add(move);
		
	}
	public void unMakeMove() {
		long move = this.moveList.getHead();
		int from 			= (int)((move >>> Move.FROM.shift)		 	  & Move.FROM.mask);
		int to	 			= (int)((move >>> Move.TO.shift) 			  & Move.TO.mask);
		int moved			= (int)((move >>> Move.MOVED_PIECE.shift) 	  & Move.MOVED_PIECE.mask);
		int captured	 	= (int)((move >>> Move.CAPTURED_PIECE.shift)  & Move.CAPTURED_PIECE.mask);
		int type			= (int)((move >>> Move.TYPE.shift)  		  & Move.TYPE.mask);
		int square;
		long fromBit = Square.getBitmapByIndex(from);
		long toBit	 = Square.getBitmapByIndex(to);
		this.setTurn();
		switch (type) {
			case 0: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to]	= captured;
				this.setBitboards(moved, captured, toBit, fromBit);
			}
			break;
			case 1: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to]	= captured;
				this.setBitboards(moved, captured, toBit, fromBit);
				if (this.whitesTurn) {
					this.offsetBoard[7]	 = 3;
					this.offsetBoard[5]	 = 0;
					this.setBitboards(3, 0, Square.getBitmapByIndex(5), Square.getBitmapByIndex(7));
				}
				else {
					this.offsetBoard[63] = 9;
					this.offsetBoard[61] = 0;
					this.setBitboards(9, 0, Square.getBitmapByIndex(61), Square.getBitmapByIndex(63));
				}
			}
			break;
			case 2: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to]	= captured;
				this.setBitboards(moved, captured, toBit, fromBit);
				if (this.whitesTurn) {
					this.offsetBoard[0]	 = 3;
					this.offsetBoard[3]	 = 0;
					this.setBitboards(3, 0, Square.getBitmapByIndex(3), Square.getBitmapByIndex(0));
				}
				else {
					this.offsetBoard[56] = 9;
					this.offsetBoard[59] = 0;
					this.setBitboards(9, 0, Square.getBitmapByIndex(59), Square.getBitmapByIndex(56));
				}
			}
			break;
			case 3: {
				this.offsetBoard[from]   = moved;
				this.offsetBoard[to]	 = 0;
				this.setBitboards(moved, 0, toBit, fromBit);
				if (this.whitesTurn) {
					square = to - 8;
					this.offsetBoard[square] = 12;
				}
				else {
					square = to + 8;
					this.offsetBoard[square] = 6;
				}
				this.setBitboards(captured, 0, 0, Square.getBitmapByIndex(square));
			}
			break;
			case 4: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to] 	= captured;
				this.setBitboards(moved, 0, 0, fromBit);
				if (this.whitesTurn) {
					this.setBitboards(2, 0, toBit, 0);
					this.setBitboards(captured, 0, 0, toBit);
				}
				else {
					this.setBitboards(8, 0, toBit, 0);
					this.setBitboards(captured, 0, 0, toBit);
				}
			}
			break;
			case 5: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to] 	= captured;
				this.setBitboards(moved, 0, 0, fromBit);
				if (this.whitesTurn) {
					this.setBitboards(3, 0, toBit, 0);
					this.setBitboards(captured, 0, 0, toBit);
				}
				else {
					this.setBitboards(9, 0, toBit, 0);
					this.setBitboards(captured, 0, 0, toBit);
				}
			}
			break;
			case 6: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to] 	= captured;
				this.setBitboards(moved, 0, 0, fromBit);
				if (this.whitesTurn) {
					this.setBitboards(4, 0, toBit, 0);
					this.setBitboards(captured, 0, 0, toBit);
				}
				else {
					this.setBitboards(10, 0, toBit, 0);
					this.setBitboards(captured, 0, 0, toBit);
				}
			}
			break;
			case 7: {
				this.offsetBoard[from]  = moved;
				this.offsetBoard[to] 	= captured;
				this.setBitboards(moved, 0, 0, fromBit);
				if (this.whitesTurn) {
					this.setBitboards(5, 0, 0, Square.getBitmapByIndex(to));
				}
				else {
					this.setBitboards(11, 0, 0, Square.getBitmapByIndex(to));
				}
			}
		}
		
		this.moveList.pop();
	}
	/**Returns the current state of a Board object as a one-line String in FEN-notation. The FEN-notation consists of six fields separated by spaces.
	 * The six fields are as follows:
	 * 		1. board position
	 * 		2. color to move
	 * 		3. castling rights
	 * 		4. en passant rights
	 * 		5. fifty-move rule clock
	 * 		6. fullmove number
	 */
	public String toString() {
		String fen = "";
		int piece, emptyCount;
		for (int i = 7; i >= 0; i--) {
			emptyCount = 0;
			for (int j = 0; j < 8; j++) {
				piece = this.offsetBoard[i*8 + j];
				if (piece == 0)
					emptyCount++;
				else {
					if (emptyCount != 0)
						fen += emptyCount;
					emptyCount = 0;
					switch (piece) {
						case 1:
							fen += 'K';
						break;
						case 2:
							fen += 'Q';
						break;
						case 3:
							fen += 'R';
						break;
						case 4:
							fen += 'B';
						break;
						case 5:
							fen += 'N';
						break;
						case 6:
							fen += 'P';
						break;
						case 7:
							fen += 'k';
						break;
						case 8:
							fen += 'q';
						break;
						case 9:
							fen += 'r';
						break;
						case 10:
							fen += 'b';
						break;
						case 11:
							fen += 'n';
						break;
						case 12:
							fen += 'p';
					}
				}
			}
			if (emptyCount != 0)
				fen += emptyCount;
			if (i != 0)
				fen += '/';
		}
		fen += ' ';
		if (this.whitesTurn)
			fen += 'w';
		else
			fen += 'b';
		fen += ' ';
		if (this.whiteCastlingRights != 0) {
			if ((this.whiteCastlingRights & 1) != 0)
				fen += 'K';
			if ((this.whiteCastlingRights & 2) != 0)
				fen += 'Q';
		}
		if (this.blackCastlingRights != 0) {
			if ((this.blackCastlingRights & 1) != 0)
				fen += 'k';
			if ((this.blackCastlingRights & 2) != 0)
				fen += 'q';
		}
		if (this.whiteCastlingRights == 0 && this.blackCastlingRights == 0)
			fen += '-';
		fen += ' ';
		if (this.enPassantRights == 8)
			fen += '-';
		else {
			fen += (char)(this.enPassantRights + 'a');
			if (this.whitesTurn)
				fen += 6;
			else
				fen += 3;
		}
		fen += ' ';
		fen += this.fiftyMoveRuleClock;
		fen += ' ';
		fen += 1 + this.moveIndex/2;
		return fen;
	}
	/**Returns a String representation of a long in binary form with all the 64 bits displayed whether set or not.*/
	public static String toBinary(long bitmap) {
		return ("0000000000000000000000000000000000000000000000000000000000000000" + Long.toBinaryString(bitmap)).substring(Long.toBinaryString(bitmap).length());
	}
	/**Returns the binary literal of the input long as a String.*/
	public static String toBinaryLiteral(long bitmap) {
		return "0b"+ toBinary(bitmap) + "L";
	}
	/**Prints a long to the console in binary form, aligned much like a chess board with one byte per row, in a human-readable way.*/
	public static void printBitboardToConsole(long bitmap) {
		String board = toBinary(bitmap);
		for (int i = 0; i < 64; i += 8) {
			for (int j = i + 7; j >= i; j--)
				System.out.print(board.charAt(j));
			System.out.println();
		}
		System.out.println();
	}
	/**Prints a bitboard representing all the occupied squares of the object's board position to the console in a human-readable form,
	 * aligned like a chess board.*/
	public void printBitboardToConsole() {
		printBitboardToConsole(this.allOccupied);
	}
	/**Prints an array representing the object's board position to the console in a human-readable form, aligned like a chess board with 
	 * integers denoting the pieces. 0 means an empty square, 1 is the white king, 2 is the white queen, ..., 7 is the black king, etc.*/
	public void printOffsetBoardToConsole() {
		for (int i = 7; i >= 0; i--) {
			for (int j = 0; j < 8; j++)
				System.out.format("%3d", this.offsetBoard[i*8 + j]);
			System.out.println();
		}
		System.out.println();
	}
	/**Runs a perft test to the given depth and returns the number of leaf nodes the traversed game tree had. It is used mainly for bug detection
	 * by comparing the returned values to validated results.*/
	public long perft(int depth) {
		LongList moves;
		long move, leafNodes = 0;
		if (depth == 0)
			return 1;
		moves = this.generateMoves();
		while (moves.hasNext()) {
			move = moves.next();
			this.makeMove(move);
			leafNodes += this.perft(depth - 1);
			this.unMakeMove();
		}
		return leafNodes;
	}
	private long perftWithBitboardConsoleOutput(int depth, long lowerBound, long upperBound, long[] count) {
		LongList moves;
		long move, leafNodes = 0;
		if (depth == 0) {
			if (count[0] >= lowerBound && count[0] <= upperBound) {
				System.out.println(count[0]);
				this.printBitboardToConsole();
			}
			count[0]++;
			return 1;
		}
		moves = this.generateMoves();
		while (moves.hasNext()) {
			move = moves.next();
			this.makeMove(move);
			leafNodes += this.perftWithBitboardConsoleOutput(depth - 1, lowerBound, upperBound, count);
			this.unMakeMove();
		}
		return leafNodes;
	}
	private long perftWithOffsetBoardConsoleOutput(int depth, long lowerBound, long upperBound, long[] count) {
		LongList moves;
		long move, leafNodes = 0;
		if (depth == 0) {
			if (count[0] >= lowerBound && count[0] <= upperBound) {
				System.out.println(count[0]);
				this.printOffsetBoardToConsole();
			}
			count[0]++;
			return 1;
		}
		moves = this.generateMoves();
		while (moves.hasNext()) {
			move = moves.next();
			this.makeMove(move);
			leafNodes += this.perftWithOffsetBoardConsoleOutput(depth - 1, lowerBound, upperBound, count);
			this.unMakeMove();
		}
		return leafNodes;
	}
	/**Runs a perft test to the given depth and prints out the leaf nodes that fall within the specified range's board positions using
	 * {@link #printOffsetBoardToConsole() printOffsetBoardToConsole} if @param detailed is true or using {@link #printBitboardToConsole() printBitboardToConsole}
	 * if false. Useful for debugging purposes.*/
	public void perftWithConsoleOutput(int depth, long lowerBound, long upperBound, boolean detailed) {
		long[] count = {0};
		if (detailed)
			this.perftWithOffsetBoardConsoleOutput(depth, lowerBound, upperBound, count);
		else
			this.perftWithBitboardConsoleOutput(depth, lowerBound, upperBound, count);
	}
}
