package engine;

import engine.Board.*;

/**A static class that generates the basic move masks for each piece type. It does not include special moves  occupancy considerations.
 * 
 * @author Viktor
 *
 */
public class MoveMask {
	
	private static long ANTIFRAME_VERTICAL;
	private static long ANTIFRAME_HORIZONTAL;
	private static long ANTIFRAME;
	
	static {
		ANTIFRAME_VERTICAL 		= ~(File.A.bitmap 	| File.H.bitmap);
		ANTIFRAME_HORIZONTAL	= ~(Rank.R1.bitmap  | Rank.R8.bitmap);
		ANTIFRAME				=  (ANTIFRAME_VERTICAL	& ANTIFRAME_HORIZONTAL);
	}
	/**Generates a bitmap of the basic king's move mask. Does not include target squares of castling; handles the wrap-around effect.
	 * 
	 * @param sqr
	 * @return
	 */
	public final static long kingMoveMask(Square sqr) {
		long mask;
		mask =	(sqr.bitmap << 7)  | (sqr.bitmap << 8)  | (sqr.bitmap << 9)  |
				(sqr.bitmap << 1)    			    	| (sqr.bitmap >>> 1) |
				(sqr.bitmap >>> 9) | (sqr.bitmap >>> 8) | (sqr.bitmap >>> 7) ;
		if (sqr.ind%8 == 0)
			mask &= ~File.H.bitmap;
		else if ((sqr.ind + 1)%8 == 0)
			mask &= ~File.A.bitmap;
		return mask;
	}
	/**Generates a bitmap of the basic knight's move mask. Occupancies are disregarded. It handles the wrap-around effect.
	 * 
	 * @param sqr
	 * @return
	 */
	public final static long knightMoveMask(Square sqr) {
		long mask;
		mask =		 	(sqr.bitmap << 15)	| (sqr.bitmap << 17) |
				(sqr.bitmap << 6)			| 		  (sqr.bitmap << 10)   |
				(sqr.bitmap >>> 10)			|		  (sqr.bitmap >>> 6)   |
						(sqr.bitmap >>> 17)	| (sqr.bitmap >>> 15);
		if (sqr.ind%8 == 0)
			mask &= ~(File.H.bitmap | File.G.bitmap);
		else if ((sqr.ind - 1)%8 == 0)
			mask &= ~File.H.bitmap;
		else if ((sqr.ind + 1)%8 == 0)
			mask &= ~(File.A.bitmap | File.B.bitmap);
		else if ((sqr.ind + 2)%8 == 0)
			mask &= ~File.A.bitmap;
		return mask;
	}
	/**Generates a bitmap of the basic white pawn's capture mask. Occupancies are disregarded. It handles the wrap-around effect.
	 * 
	 * @param sqr
	 * @return
	 */
	public final static long whitePawnCaptureMask(Square sqr) {
		long mask;
		if (sqr.ind > 55)
				return 0;
		mask = (sqr.bitmap << 7) | (sqr.bitmap << 9);
		if (sqr.ind%8 == 0)
			mask &= ~File.H.bitmap;
		else if ((sqr.ind + 1)%8 == 0)
			mask &= ~File.A.bitmap;
		return mask;
	}
	/**Generates a bitmap of the basic black pawn's capture mask. Occupancies are disregarded. It handles the wrap-around effect.
	 * 
	 * @param sqr
	 * @return
	 */
	public final static long blackPawnCaptureMask(Square sqr) {
		long mask;
		if (sqr.ind < 8)
				return 0;
		mask = (sqr.bitmap >>> 9) | (sqr.bitmap >>> 7);
		if (sqr.ind%8 == 0)
			mask &= ~File.H.bitmap;
		else if ((sqr.ind + 1)%8 == 0)
			mask &= ~File.A.bitmap;
		return mask;
	}
	/**Generates a bitmap of the basic white pawn's advance mask. Double advance from initial square is included. Occupancies are disregarded. It handles the wrap-around effect.
	 * 
	 * @param sqr
	 * @return
	 */
	public final static long whitePawnAdvanceMask(Square sqr) {
		if (sqr.ind < 8 || sqr.ind > 55)
			return 0;
		return sqr.bitmap << 8;
	}
	/**Generates a bitmap of the basic black pawn's advance mask. Double advance from initial square is included. Occupancies are disregarded. It handles the wrap-around effect.
	 * 
	 * @param sqr
	 * @return
	 */
	public final static long blackPawnAdvanceMask(Square sqr) {
		if (sqr.ind < 8 || sqr.ind > 55)
			return 0;
		return sqr.bitmap >>> 8;
	}
	/**Generates a bitmap of the basic rook's rank-wise/horizontal move mask. Occupancies are disregarded. Perimeter squares are included.
	 * 
	 * @param sqr
	 * @return
	 */
	public final static long rookRankMoveMask(Square sqr) {
		return Rank.getBySquare(sqr).bitmap^sqr.bitmap;
	}
	/**Generates a bitmap of the basic rook's file-wise/vertical move mask. Occupancies are disregarded. Perimeter squares are included.
	 * 
	 * @param sqr
	 * @return
	 */
	public final static long rookFileMoveMask(Square sqr) {
		return File.getBySquare(sqr).bitmap^sqr.bitmap;
	}
	/**Generates a bitmap of the basic bishop's diagonal move mask. Occupancies are disregarded. Perimeter squares are included.
	 * 
	 * @param sqr
	 * @return
	 */
	public final static long bishopDiagonalMoveMask(Square sqr) {
		return Diagonal.getBySquare(sqr).bitmap^sqr.bitmap;
	}
	/**Generates a bitmap of the basic bishop's anti-diagonal move mask. Occupancies are disregarded. Perimeter squares are included.
	 * 
	 * @param sqr
	 * @return
	 */
	public final static long bishopAntiDiagonalMoveMask(Square sqr) {
		return AntiDiagonal.getBySquare(sqr).bitmap^sqr.bitmap;
	}
	/**Generates a bitmap of the relevant occupancy mask for a rook on the square specified by 'sqr'.
	 * 
	 * @param sqr
	 * @return
	 */
	public static long rookOccupancyMask(Square sqr) {
		return (rookRankMoveMask(sqr) & ANTIFRAME_VERTICAL) | (rookFileMoveMask(sqr) & ANTIFRAME_HORIZONTAL);
	}
	/**Generates a bitmap of the relevant occupancy mask for a bishop on the square specified by 'sqr'.
	 * 
	 * @param sqr
	 * @return
	 */
	public static long bishopOccupancyMask(Square sqr) {
		return (bishopDiagonalMoveMask(sqr) | bishopAntiDiagonalMoveMask(sqr)) & ANTIFRAME;
	}
}