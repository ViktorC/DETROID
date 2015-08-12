package engine;

import engine.board.File;
import engine.board.Rank;
import engine.board.Square;

/**An enum type that holds the rank, file, diagonal, and anti-diagonal that cross the square for each square as bitmaps. The square itself and the perimeter squares are excluded as their occupancies do not affect the attack set variations.
 * 
 * @author Viktor
 *
 */
public enum SliderOccupancyMask {
	
	A1(true),
		B1, C1, D1, E1, F1, G1, H1,
	A2, B2, C2, D2, E2, F2, G2, H2,
	A3, B3, C3, D3, E3, F3, G3, H3,
	A4, B4, C4, D4, E4, F4, G4, H4,
	A5, B5, C5, D5, E5, F5, G5, H5,
	A6, B6, C6, D6, E6, F6, G6, H6,
	A7, B7, C7, D7, E7, F7, G7, H7,
	A8, B8, C8, D8, E8, F8, G8, H8;
	
	public final long rookOccupancyMask;
	public final long bishopOccupancyMask;
	
	public final byte rookOccupancyMaskBitCount;
	public final byte bishopOccupancyMaskBitCount;
	
	private static long ANTIFRAME_VERTICAL;
	private static long ANTIFRAME_HORIZONTAL;
	private static long ANTIFRAME;
	
	private void initializeAntiFrames() {
		ANTIFRAME_VERTICAL 		= ~(File.A.bitmap 	| File.H.bitmap);
		ANTIFRAME_HORIZONTAL	= ~(Rank.R1.bitmap  | Rank.R8.bitmap);
		ANTIFRAME				=  (ANTIFRAME_VERTICAL	& ANTIFRAME_HORIZONTAL);
	}
	private SliderOccupancyMask(boolean flag) {
		this.initializeAntiFrames();
		Square sqr = Square.getByIndex(this.ordinal());
		this.rookOccupancyMask = generateRooksCompleteOccupancyMask(sqr);
		this.bishopOccupancyMask = generateBishopsCompleteOccupancyMask(sqr);
		this.rookOccupancyMaskBitCount = (byte)BitOperations.getCardinality(this.rookOccupancyMask);
		this.bishopOccupancyMaskBitCount = (byte)BitOperations.getCardinality(this.bishopOccupancyMask);
	}
	private SliderOccupancyMask() {
		Square sqr = Square.getByIndex(this.ordinal());
		this.rookOccupancyMask = generateRooksCompleteOccupancyMask(sqr);
		this.bishopOccupancyMask = generateBishopsCompleteOccupancyMask(sqr);
		this.rookOccupancyMaskBitCount = (byte)BitOperations.getCardinality(this.rookOccupancyMask);
		this.bishopOccupancyMaskBitCount = (byte)BitOperations.getCardinality(this.bishopOccupancyMask);
	}
	private static long generateRooksRankOccupancyMask(Square sqr) {
		return (MoveMaskGenerator.generateRooksRankMoveMask(sqr) & ANTIFRAME_VERTICAL);
	}
	private static long generateRooksFileOccupancyMask(Square sqr) {
		return (MoveMaskGenerator.generateRooksFileMoveMask(sqr) & ANTIFRAME_HORIZONTAL);
	}
	private static long generateRooksCompleteOccupancyMask(Square sqr) {
		return (generateRooksRankOccupancyMask(sqr) | generateRooksFileOccupancyMask(sqr));
	}
	private static long generateBishopsDiagonalOccupancyMask(Square sqr) {
		return (MoveMaskGenerator.generateBishopsDiagonalMoveMask(sqr) & ANTIFRAME);
	}
	private static long generateBishopsAntiDiagonalOccupancyMask(Square sqr) {
		return (MoveMaskGenerator.generateBishopsAntiDiagonalMoveMask(sqr) & ANTIFRAME);
	}
	private static long generateBishopsCompleteOccupancyMask(Square sqr) {
		return (generateBishopsDiagonalOccupancyMask(sqr) | generateBishopsAntiDiagonalOccupancyMask(sqr));
	}
	public static SliderOccupancyMask getByIndex(int sqrInd) {
		switch(sqrInd) {
			case 0:  return A1; case 1:  return B1; case 2:  return C1; case 3:  return D1; case 4:  return E1; case 5:  return F1; case 6:  return G1; case 7:  return H1;
			case 8:  return A2; case 9:  return B2; case 10: return C2; case 11: return D2; case 12: return E2; case 13: return F2; case 14: return G2; case 15: return H2;
			case 16: return A3; case 17: return B3; case 18: return C3; case 19: return D3; case 20: return E3; case 21: return F3; case 22: return G3; case 23: return H3;
			case 24: return A4; case 25: return B4; case 26: return C4; case 27: return D4; case 28: return E4; case 29: return F4; case 30: return G4; case 31: return H4;
			case 32: return A5; case 33: return B5; case 34: return C5; case 35: return D5; case 36: return E5; case 37: return F5; case 38: return G5; case 39: return H5;
			case 40: return A6; case 41: return B6; case 42: return C6; case 43: return D6; case 44: return E6; case 45: return F6; case 46: return G6; case 47: return H6;
			case 48: return A7; case 49: return B7; case 50: return C7; case 51: return D7; case 52: return E7; case 53: return F7; case 54: return G7; case 55: return H7;
			case 56: return A8; case 57: return B8; case 58: return C8; case 59: return D8; case 60: return E8; case 61: return F8; case 62: return G8; case 63: return H8;
			default: throw new IllegalArgumentException("Invalid square index.");
		}
	}
}