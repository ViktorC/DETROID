package engine;

import util.BitOperations;
import engine.Board.*;

/**
 * A class for generating occupancy variations and the attack set(s) for a rook or a bishop on the specified square for the given relevant
 * occupancy or array of occupancy variations. It can also generate the attack sets for all possible occupancy variations on all squares either
 * for the rook or for the bishop. It uses ray-wise parallel-prefix algorithms to determine the attack sets.
 * 
 * @author Viktor
 *
 */
public final class Sliders {
	
	private final static long ANTIFRAME_VERTICAL = ~(File.A.bitmap | File.H.bitmap);
	private final static long ANTIFRAME_HORIZONTAL = ~(Rank.R1.bitmap | Rank.R8.bitmap);
	private final static long ANTIFRAME = (ANTIFRAME_VERTICAL & ANTIFRAME_HORIZONTAL);
	
	private Sliders() {
		
	}
	/**
	 * Returns the rank-wise attack set for the relevant occupancy from the defined square.
	 * 
	 * @param sqr The square on which the slider is.
	 * @param relevantOccupancy The relevant occupancy of the rank on which the square is (all occupied AND rank [minus the perimeter squares,
	 * as they make no difference]).
	 * @return
	 */
	public static long getRankAttackSet(Square sqr, long relevantOccupancy) {
		Rank rank = Rank.getBySquareIndex(sqr.ind);
		long forward, reverse;
		forward  = rank.bitmap & relevantOccupancy;
		reverse  = BitOperations.reverse(relevantOccupancy);
		forward -= 2*sqr.bitmap;
		reverse -= 2*BitOperations.reverse(sqr.bitmap);
		forward ^= BitOperations.reverse(reverse);
		return forward & rank.bitmap;
	}
	/**
	 * Returns the file-wise attack set for the relevant occupancy from the defined square.
	 * 
	 * @param sqr The square on which the slider is.
	 * @param relevantOccupancy The relevant occupancy of the file on which the square is (all occupied AND file [minus the perimeter squares,
	 * as they make no difference]).
	 * @return
	 */
	public static long getFileAttackSet(Square sqr, long relevantOccupancy) {
		File file = File.getBySquareIndex(sqr.ind);
		long forward, reverse;
		forward  = file.bitmap & relevantOccupancy;
		reverse  = BitOperations.reverseBytes(forward);
		forward -= sqr.bitmap;
		reverse -= BitOperations.reverseBytes(sqr.bitmap);
		forward ^= BitOperations.reverseBytes(reverse);
		return forward & file.bitmap;
	}
	/**
	 * Returns the diagonal-wise attack set for the relevant occupancy from the defined square.
	 * 
	 * @param sqr The square on which the slider is.
	 * @param relevantOccupancy The relevant occupancy of the diagonal on which the square is (all occupied AND diagonal [minus the perimeter
	 * squares, as they make no difference]).
	 * @return
	 */
	public static long getDiagonalAttackSet(Square sqr, long relevantOccupancy) {
		Diagonal diagonal = Diagonal.getBySquareIndex(sqr.ind);
		long forward, reverse;
		forward  = diagonal.bitmap & relevantOccupancy;
		reverse  = BitOperations.reverseBytes(forward);
		forward -= sqr.bitmap;
		reverse -= BitOperations.reverseBytes(sqr.bitmap);
		forward ^= BitOperations.reverseBytes(reverse);
		return forward & diagonal.bitmap;
	}
	/**
	 * Returns the anti-diagonal-wise attack set for the relevant occupancy from the defined square.
	 * 
	 * @param sqr The square on which the slider is.
	 * @param relevantOccupancy The relevant occupancy of the anti-diagonal on which the square is (all occupied AND anti-diagonal
	 * [minus the perimeter squares, as they make no difference]).
	 * @return
	 */
	public static long getAntiDiagonalAttackSet(Square sqr, long relevantOccupancy) {
		AntiDiagonal antiDiagonal = AntiDiagonal.getBySquareIndex(sqr.ind);
		long forward, reverse;
		forward  = antiDiagonal.bitmap & relevantOccupancy;
		reverse  = BitOperations.reverseBytes(forward);
		forward -= sqr.bitmap;
		reverse -= BitOperations.reverseBytes(sqr.bitmap);
		forward ^= BitOperations.reverseBytes(reverse);
		return forward & antiDiagonal.bitmap;
	}
	/**
	 * Returns a rook's attack set from the defined square with the given occupancy.
	 * 
	 * @param sqr The square on which the rook is.
	 * @param relevantOccupancy The relevant occupancy of the rank and file that cross each other on the specified square
	 * (all occupied AND (rank OR file) [minus the perimeter squares, as they make no difference]).
	 * @return
	 */
	public static long getRookAttackSet(Square sqr, long occupancy) {
		return getRankAttackSet(sqr, occupancy) | getFileAttackSet(sqr, occupancy);
	}
	/**
	 * Returns a bishop's attack set from the defined square with the given occupancy.
	 * 
	 * @param sqr The square on which the bishop is.
	 * @param relevantOccupancy The relevant occupancy of the diagonal and anti-diagonal that cross each other on the specified square
	 * (all occupied AND (diagonal OR anti-diagonal) [minus the perimeter squares, as they make no difference]).
	 * @return
	 */
	public static long getBishopAttackSet(Square sqr, long occupancy) {
		return getDiagonalAttackSet(sqr, occupancy) | getAntiDiagonalAttackSet(sqr, occupancy);
	}
	/**
	 * Generates a bitmap of the relevant occupancy mask for a rook on the square specified by 'sqr'.
	 * 
	 * @param sqr
	 * @return
	 */
	public static long getRookOccupancyMask(Square sqr) {
		return ((Rank.getBySquare(sqr).bitmap^sqr.bitmap) & ANTIFRAME_VERTICAL) |
				((File.getBySquare(sqr).bitmap^sqr.bitmap) & ANTIFRAME_HORIZONTAL);
	}
	/**
	 * Generates a bitmap of the relevant occupancy mask for a bishop on the square specified by 'sqr'.
	 * 
	 * @param sqr
	 * @return
	 */
	public static long getBishopOccupancyMask(Square sqr) {
		return ((Diagonal.getBySquare(sqr).bitmap^sqr.bitmap) | (AntiDiagonal.getBySquare(sqr).bitmap^sqr.bitmap)) & ANTIFRAME;
	}
	/**
	 * Returns a rook's attack set variations from the given square for all occupancy variations specified.
	 * 
	 * @param sqr The square on which the rook is.
	 * @param bishopOccupancyVariations The occupancy variations within the rook's occupancy mask.
	 * @return
	 */
	public static long[] getRookAttackSetVariations(Square sqr, long[] rookOccupancyVariations) {
		long[] rookAttVar = new long[rookOccupancyVariations.length];
		for (int i = 0; i < rookAttVar.length; i++)
			rookAttVar[i] = getRookAttackSet(sqr, rookOccupancyVariations[i]);
		return rookAttVar;
	}
	/**
	 * Returns a bishop's attack set variations from the given square for all occupancy variations specified.
	 * 
	 * @param sqr - the square on which the bishop is
	 * @param bishopOccupancyVariations - the occupancy variations within the bishop's occupancy mask
	 * @return
	 */
	public static long[] getBishopAttackSetVariations(Square sqr, long[] bishopOccupancyVariations) {
		long[] bishopAttVar = new long[bishopOccupancyVariations.length];
		for (int i = 0; i < bishopAttVar.length; i++)
			bishopAttVar[i] = getBishopAttackSet(sqr, bishopOccupancyVariations[i]);
		return bishopAttVar;
	}
}
