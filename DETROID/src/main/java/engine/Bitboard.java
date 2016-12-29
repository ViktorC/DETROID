package main.java.engine;

import main.java.util.BitOperations;

/*
 * A class to group together objects/enums exclusive to the chess board itself such as the squares, files, and ranks.
 * 
 * @author Viktor
 *
 */
final class Bitboard {
	
	/**
	 * A bitboard with all the 64 bits set.
	 */
	final static long FULL_BOARD = -1L;
	/**
	 * A straight line, if one exists, between an [origin square] and a [target square] for each two-square combination. If the two squares do not fall on the same
	 * rank, file, diagonal, or anti-diagonal the line is '0'.
	 */
	final static long[][] LINE_SEGMENTS;
	static {
		Rays originRays, targetRays;
		long line;
		LINE_SEGMENTS = new long[64][64];
		for (int origin = 0; origin < 64; origin++) {
			originRays = Rays.getByIndex(origin);
			for (int target = 0; target < 64; target++) {
				targetRays = Rays.getByIndex(target);
				line = 0;
				line |= (originRays.rankPos & targetRays.rankNeg);
				line |= (originRays.rankNeg & targetRays.rankPos);
				line |= (originRays.diagonalPos & targetRays.diagonalNeg);
				line |= (originRays.diagonalNeg & targetRays.diagonalPos);
				line |= (originRays.filePos & targetRays.fileNeg);
				line |= (originRays.fileNeg & targetRays.filePos);
				line |= (originRays.antiDiagonalPos & targetRays.antiDiagonalNeg);
				line |= (originRays.antiDiagonalNeg & targetRays.antiDiagonalPos);
				LINE_SEGMENTS[origin][target] = line;
			}
		}
	}
	
	/**
	 * An enum type for the 64 squares of the chess board. Each constant has a field that contains a long with only the bit on
	 * the respective square's index set.
	 * 
	 * @author Viktor
	 *
	 */
	static enum Square {

		A1, B1, C1, D1, E1, F1, G1, H1,
		A2, B2, C2, D2, E2, F2, G2, H2,
		A3, B3, C3, D3, E3, F3, G3, H3,
		A4, B4, C4, D4, E4, F4, G4, H4,
		A5, B5, C5, D5, E5, F5, G5, H5,
		A6, B6, C6, D6, E6, F6, G6, H6,
		A7, B7, C7, D7, E7, F7, G7, H7,
		A8, B8, C8, D8, E8, F8, G8, H8;

		final byte ind;
		final long bit;

		private Square() {
			ind = (byte)ordinal();
			bit = 1L << ind;
		}
		/**
		 * Returns a String representation of the square.
		 */
		@Override
		public String toString() {
			return ("" + (char)('a' + ind%8) + "" + (ind/8 + 1)).toUpperCase();
		}
		/**
		 * @return a Square enum.
		 */
		static Square getByIndex(int sqrInd) {
			switch(sqrInd) {
				case 0:  return A1; case 1:  return B1; case 2:  return C1; case 3:  return D1; case 4:  return E1; case 5:  return F1;
				case 6:  return G1; case 7:  return H1; case 8:  return A2; case 9:  return B2; case 10: return C2; case 11: return D2;
				case 12: return E2; case 13: return F2; case 14: return G2; case 15: return H2; case 16: return A3; case 17: return B3;
				case 18: return C3; case 19: return D3; case 20: return E3; case 21: return F3; case 22: return G3; case 23: return H3;
				case 24: return A4; case 25: return B4; case 26: return C4; case 27: return D4; case 28: return E4; case 29: return F4;
				case 30: return G4; case 31: return H4; case 32: return A5; case 33: return B5; case 34: return C5; case 35: return D5;
				case 36: return E5; case 37: return F5; case 38: return G5; case 39: return H5; case 40: return A6; case 41: return B6;
				case 42: return C6; case 43: return D6; case 44: return E6; case 45: return F6; case 46: return G6; case 47: return H6;
				case 48: return A7; case 49: return B7; case 50: return C7; case 51: return D7; case 52: return E7; case 53: return F7;
				case 54: return G7; case 55: return H7; case 56: return A8; case 57: return B8; case 58: return C8; case 59: return D8;
				case 60: return E8; case 61: return F8; case 62: return G8; case 63: return H8; default:
					throw new IllegalArgumentException("Invalid square index.");
			}
		}
	}

	/**
	 * An enum type for the 8 files/columns of a chess board. Each constant has a field that contains a long with only the bits falling on the
	 * file set.
	 * 
	 * @author Viktor
	 *
	 */
	static enum File {
		
		A, B, C, D, E, F, G, H;
		
		final long bits;
		
		private File() {
			bits = 0b0000000100000001000000010000000100000001000000010000000100000001L << ordinal();
		}
		/**
		 * Returns the file 'fileInd' to the right from the file A.
		 * 
		 * @param fileInd The index of the file.
		 */
		static File getByIndex(int fileInd) {
			switch(fileInd) {
				case 0:  return A;
				case 1:  return B;
				case 2:  return C;
				case 3:  return D;
				case 4:  return E;
				case 5:  return F;
				case 6:  return G;
				case 7:  return H;
				default: throw new IllegalArgumentException("Invalid file index.");
			}
		}
		/**
		 * Returns a the file of the chess board on which the input parameter square lies.
		 * 
		 * @param sqr A Square enum.
		 */
		static File getBySquare(Square sqr) {
			return getByIndex(sqr.ind & 7);
		}
		/**
		 * Returns the file of the chess board on which the input parameter square lies.
		 * 
		 * @param sqrInd The index of the square.
		 */
		static File getBySquareIndex(int sqrInd) {
			return getByIndex(sqrInd & 7);
		}
	}
	
	/**
	 * An enum type for the 8 ranks/rows of a chess board. Each constant has a field that contains a long with only the byte on the rank's
	 * index set.
	 * 
	 * @author Viktor
	 *
	 */
	static enum Rank {
		
		R1, R2, R3, R4, R5, R6, R7, R8;
		
		final long bits;
		
		private Rank() {
			bits = 0b0000000000000000000000000000000000000000000000000000000011111111L << (8*ordinal());
		}
		/**
		 * Returns the 'rnkInd'th rank.
		 * 
		 * @param rnkInd The index of the rank.
		 */
		static Rank getByIndex(int rnkInd) {
			switch(rnkInd) {
				case 0:  return R1;
				case 1:  return R2;
				case 2:  return R3;
				case 3:  return R4;
				case 4:  return R5;
				case 5:  return R6;
				case 6:  return R7;
				case 7:  return R8;
				default: throw new IllegalArgumentException("Invalid rank index.");
			}
		}
		/**
		 * Returns the rank of the chess board on which the input parameter square lies.
		 * 
		 * @param sqr A Square enum.
		 */
		static Rank getBySquare(Square sqr) {
			return getByIndex(sqr.ind >>> 3);
		}
		/**
		 * Returns the rank of the chess board on which the input parameter square lies.
		 * 
		 * @param sqrInd The index of the square.
		 */
		static Rank getBySquareIndex(int sqrInd) {
			return getByIndex(sqrInd >>> 3);
		}
	}
	
	/**
	 * An enum type for the 15 diagonals of a chess board. Each constant has a field that contains a long with only the bits on indexes
	 * of the squares falling on the diagonal set.
	 * 
	 * @author Viktor
	 *
	 */
	static enum Diagonal {
		
		DG1, DG2, DG3, DG4, DG5, DG6, DG7, DG8, DG9, DG10, DG11, DG12, DG13, DG14, DG15;
		
		final long bits;
		
		private Diagonal() {
			long base = 0b0000000100000010000001000000100000010000001000000100000010000000L;
			int shift = 7 - ordinal();
			bits = shift > 0 ? base >>> 8*shift : base << 8*-shift;
				
		}
		/**
		 * Returns the diagonal indexed by the input parameter.
		 * 
		 * @param dgnInd The index of the diagonal.
		 */
		static Diagonal getByIndex(int dgnInd) {
			switch(dgnInd) {
				case 0:  return DG1;
				case 1:  return DG2;
				case 2:  return DG3;
				case 3:  return DG4;
				case 4:  return DG5;
				case 5:  return DG6;
				case 6:  return DG7;
				case 7:  return DG8;
				case 8:  return DG9;
				case 9:  return DG10;
				case 10: return DG11;
				case 11: return DG12;
				case 12: return DG13;
				case 13: return DG14;
				case 14: return DG15;
				default: throw new IllegalArgumentException("Invalid diagonal index.");
			}
		}
		/**
		 * Returns the diagonal of the chess board on which the input parameter square lies.
		 * 
		 * @param sqr A Square enum.
		 */
		static Diagonal getBySquare(Square sqr) {
			return getByIndex((sqr.ind & 7) + (sqr.ind >>> 3));
		}
		/**
		 * Returns the diagonal of the chess board on which the input parameter square lies.
		 * 
		 * @param sqrInd The index of a square.
		 */
		static Diagonal getBySquareIndex(int sqrInd) {
			return getByIndex((sqrInd & 7) + (sqrInd >>> 3));
		}
	}
	
	/**
	 * An enum type for the 15 anti-diagonals of a chess board. Each constant has a field that contains a long with only the bits on indexes
	 * of the squares falling on the diagonal set.
	 * 
	 * @author Viktor
	 *
	 */
	static enum AntiDiagonal {
		
		ADG1, ADG2, ADG3, ADG4, ADG5, ADG6, ADG7, ADG8, ADG9, ADG10, ADG11, ADG12, ADG13, ADG14, ADG15;
		
		final long bits;
		
		private AntiDiagonal() {
			long base = 0b1000000001000000001000000001000000001000000001000000001000000001L;
			int shift = 7 - ordinal();
			bits = shift > 0 ? base << 8*shift : base >>> 8*-shift;
		}
		/**
		 * Returns the anti-diagonal indexed by the input parameter.
		 * 
		 * @param adgnInd The index of the anti-diagonal.
		 */
		static AntiDiagonal getByIndex(int adgnInd) {
			switch(adgnInd) {
				case 0:  return ADG1;
				case 1:  return ADG2;
				case 2:  return ADG3;
				case 3:  return ADG4;
				case 4:  return ADG5;
				case 5:  return ADG6;
				case 6:  return ADG7;
				case 7:  return ADG8;
				case 8:  return ADG9;
				case 9:  return ADG10;
				case 10: return ADG11;
				case 11: return ADG12;
				case 12: return ADG13;
				case 13: return ADG14;
				case 14: return ADG15;
				default: throw new IllegalArgumentException("Invalid anti-diagonal index.");
			}
		}
		/**
		 * Returns the anti-diagonal of the chess board on which the input parameter square lies.
		 * 
		 * @param sqr A Square enum.
		 */
		static AntiDiagonal getBySquare(Square sqr) {
			return getByIndex((sqr.ind & 7) + (7 - (sqr.ind >>> 3)));
		}
		/**
		 * Returns the anti-diagonal of the chess board on which the input parameter square lies.
		 * 
		 * @param sqrInd The index of a square.
		 */
		static AntiDiagonal getBySquareIndex(int sqrInd) {
			return getByIndex((sqrInd & 7) + (7 - (sqrInd >>> 3)));
		}
	}
	/**
	 * An enum type for all the eight different rays on the chess board for each square.
	 * 
	 * @author Viktor
	 *
	 */
	enum Rays {
		
		A1, B1, C1, D1, E1, F1, G1, H1,
		A2, B2, C2, D2, E2, F2, G2, H2,
		A3, B3, C3, D3, E3, F3, G3, H3,
		A4, B4, C4, D4, E4, F4, G4, H4,
		A5, B5, C5, D5, E5, F5, G5, H5,
		A6, B6, C6, D6, E6, F6, G6, H6,
		A7, B7, C7, D7, E7, F7, G7, H7,
		A8, B8, C8, D8, E8, F8, G8, H8;
		
		final long rankPos;
		final long rankNeg;
		final long filePos;
		final long fileNeg;
		final long diagonalPos;
		final long diagonalNeg;
		final long antiDiagonalPos;
		final long antiDiagonalNeg;
		
		private Rays( ) {
			int sqrInd = this.ordinal();
			long sqrBit = Square.getByIndex(sqrInd).bit;
			Rank rank = Rank.getBySquareIndex(sqrInd);
			File file = File.getBySquareIndex(sqrInd);
			Diagonal diagonal = Diagonal.getBySquareIndex(sqrInd);
			AntiDiagonal antiDiagonal = AntiDiagonal.getBySquareIndex(sqrInd);
			rankPos = rank.bits & ~((sqrBit << 1) - 1);
			rankNeg = rank.bits & (sqrBit - 1);
			filePos = file.bits & ~((sqrBit << 1) - 1);
			fileNeg = file.bits & (sqrBit - 1);
			diagonalPos = diagonal.bits & ~((sqrBit << 1) - 1);
			diagonalNeg = diagonal.bits & (sqrBit - 1);
			antiDiagonalPos = antiDiagonal.bits & ~((sqrBit << 1) - 1);
			antiDiagonalNeg = antiDiagonal.bits & (sqrBit - 1);
		}
		/**
		 * Returns a Ray enum instance that holds the ray masks for the square specified by the given square index, sqrInd.
		 * 
		 * @param sqrInd
		 * @return
		 */
		static Rays getByIndex(int sqrInd) {
			switch(sqrInd) {
				case 0:  return A1; case 1:  return B1; case 2:  return C1; case 3:  return D1; case 4:  return E1; case 5:  return F1;
				case 6:  return G1; case 7:  return H1; case 8:  return A2; case 9:  return B2; case 10: return C2; case 11: return D2;
				case 12: return E2; case 13: return F2; case 14: return G2; case 15: return H2; case 16: return A3; case 17: return B3;
				case 18: return C3; case 19: return D3; case 20: return E3; case 21: return F3; case 22: return G3; case 23: return H3;
				case 24: return A4; case 25: return B4; case 26: return C4; case 27: return D4; case 28: return E4; case 29: return F4;
				case 30: return G4; case 31: return H4; case 32: return A5; case 33: return B5; case 34: return C5; case 35: return D5;
				case 36: return E5; case 37: return F5; case 38: return G5; case 39: return H5; case 40: return A6; case 41: return B6;
				case 42: return C6; case 43: return D6; case 44: return E6; case 45: return F6; case 46: return G6; case 47: return H6;
				case 48: return A7; case 49: return B7; case 50: return C7; case 51: return D7; case 52: return E7; case 53: return F7;
				case 54: return G7; case 55: return H7; case 56: return A8; case 57: return B8; case 58: return C8; case 59: return D8;
				case 60: return E8; case 61: return F8; case 62: return G8; case 63: return H8;
				default: throw new IllegalArgumentException("Invalid square index.");
			}
		}
	}
	
	private Bitboard() {
		
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction north of multiple sliding pieces at the same
	 * time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	static long northFill(long generator, long propagator) {
		generator  |= (generator  << 8)  & propagator;
		propagator &= (propagator << 8);
		generator  |= (generator  << 16) & propagator;
		propagator &= (propagator << 16);
		generator  |= (generator  << 32) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction south of multiple sliding pieces at the same
	 * time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	static long southFill(long generator, long propagator) {
		generator  |= (generator  >>> 8)  & propagator;
		propagator &= (propagator >>> 8);
		generator  |= (generator  >>> 16) & propagator;
		propagator &= (propagator >>> 16);
		generator  |= (generator  >>> 32) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction west of multiple sliding pieces at the same
	 * time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect is
	 * handled by the method.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	static long westFill(long generator, long propagator) {
		propagator &= 0b0111111101111111011111110111111101111111011111110111111101111111L;
		generator  |= (generator  >>> 1) & propagator;
		propagator &= (propagator >>> 1);
		generator  |= (generator  >>> 2) & propagator;
		propagator &= (propagator >>> 2);
		generator  |= (generator  >>> 4) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction east of multiple sliding pieces at the same
	 * time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect is
	 * handled by the method.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	static long eastFill(long generator, long propagator) {
		propagator &= 0b1111111011111110111111101111111011111110111111101111111011111110L;
		generator  |= (generator  << 1) & propagator;
		propagator &= (propagator << 1);
		generator  |= (generator  << 2) & propagator;
		propagator &= (propagator << 2);
		generator  |= (generator  << 4) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction north-west of multiple sliding pieces at the
	 * same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect
	 * is handled by the method.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	static long northWestFill(long generator, long propagator) {
		propagator &= 0b0111111101111111011111110111111101111111011111110111111101111111L;
		generator  |= (generator  << 7)  & propagator;
		propagator &= (propagator << 7);
		generator  |= (generator  << 14) & propagator;
		propagator &= (propagator << 14);
		generator  |= (generator  << 28) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction north-east of multiple sliding pieces at the
	 * same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect
	 * is handled by the method.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	static long northEastFill(long generator, long propagator) {
		propagator &= 0b1111111011111110111111101111111011111110111111101111111011111110L;
		generator  |= (generator  << 9)  & propagator;
		propagator &= (propagator << 9);
		generator  |= (generator  << 18) & propagator;
		propagator &= (propagator << 18);
		generator  |= (generator  << 36) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction south-west of multiple sliding pieces at the
	 * same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect
	 * is handled by the method.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	static long southWestFill(long generator, long propagator) {
		propagator &= 0b0111111101111111011111110111111101111111011111110111111101111111L;
		generator  |= (generator  >>> 9)  & propagator;
		propagator &= (propagator >>> 9);
		generator  |= (generator  >>> 18) & propagator;
		propagator &= (propagator >>> 18);
		generator  |= (generator  >>> 36) & propagator;
		return generator;
	}
	/**
	 * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction south-east of multiple sliding pieces at the
	 * same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect
	 * is handled by the method.
	 * 
	 * @param generator Piece squares.
	 * @param propagator All empty squares.
	 * @return
	 */
	static long southEastFill(long generator, long propagator) {
		propagator &= 0b1111111011111110111111101111111011111110111111101111111011111110L;
		generator  |= (generator  >>> 7)  & propagator;
		propagator &= (propagator >>> 7);
		generator  |= (generator  >>> 14) & propagator;
		propagator &= (propagator >>> 14);
		generator  |= (generator  >>> 28) & propagator;
		return generator;
	}
	/**
	 * Returns a long in binary form aligned like a chess board with one byte per row, in a human-readable way.
	 * 
	 * @param bitboard
	 * @return
	 */
	static String bitboardToString(long bitboard) {
		String out = "";
		String board = BitOperations.toBinaryString(bitboard);
		for (int i = 0; i < 64; i += 8) {
			for (int j = i + 7; j >= i; j--)
				out += board.charAt(j);
			out += "\n";
		}
		out += "\n";
		return out;
	}
}
