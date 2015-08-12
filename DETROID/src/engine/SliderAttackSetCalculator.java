package engine;

import engine.board.AntiDiagonal;
import engine.board.Diagonal;
import engine.board.File;
import engine.board.Rank;
import engine.board.Square;

/**A class that generates the attack set(s) for a rook or a bishop on the specified square for the given occupancy or array of
 * occupancy variations. It can also generate the attack sets for all possible occupancy variations on all squares either for the rook
 * or for the bishop. It uses ray-wise parallel-prefix algorithms to determine the attack sets.
 * 
 * @author Viktor
 *
 */
public class SliderAttackSetCalculator {
	
	private static long rankAttacks(Square sqr, long occupancy) {
		int sqrInd = sqr.ordinal();
		long rank = Rank.getBySquareIndex(sqrInd);
		long forward, reverse;
		forward  = rank & occupancy;
		reverse  = BitOperations.reverse(occupancy);
		forward -= 2*sqr.bitmap;
		reverse -= 2*BitOperations.reverse(sqr.bitmap);
		forward ^= BitOperations.reverse(reverse);
		return forward & rank;
	}
	private static long fileAttacks(Square sqr, long occupancy) {
		int sqrInd = sqr.ordinal();
		long file = File.getBySquareIndex(sqrInd);
		long forward, reverse;
		forward  = file & occupancy;
		reverse  = BitOperations.reverseBytes(forward);
		forward -= sqr.bitmap;
		reverse -= BitOperations.reverseBytes(sqr.bitmap);
		forward ^= BitOperations.reverseBytes(reverse);
		return forward & file;
	}
	private static long diagonalAttacks(Square sqr, long occupancy) {
		int sqrInd = sqr.ordinal();
		long diagonal = Diagonal.getBySquareIndex(sqrInd);
		long forward, reverse;
		forward  = diagonal & occupancy;
		reverse  = BitOperations.reverseBytes(forward);
		forward -= sqr.bitmap;
		reverse -= BitOperations.reverseBytes(sqr.bitmap);
		forward ^= BitOperations.reverseBytes(reverse);
		return forward & diagonal;
	}
	private static long antiDiagonalAttacks(Square sqr, long occupancy) {
		int sqrInd = sqr.ordinal();
		long antiDiagonal = AntiDiagonal.getBySquareIndex(sqrInd);
		long forward, reverse;
		forward  = antiDiagonal & occupancy;
		reverse  = BitOperations.reverseBytes(forward);
		forward -= sqr.bitmap;
		reverse -= BitOperations.reverseBytes(sqr.bitmap);
		forward ^= BitOperations.reverseBytes(reverse);
		return forward & antiDiagonal;
	}
	public static long computeRookAttackSet(Square sqr, long occupancy) {
		return rankAttacks(sqr, occupancy) | fileAttacks(sqr, occupancy);
	}
	public static long computeBishopAttackSet(Square sqr, long occupancy) {
		return diagonalAttacks(sqr, occupancy) | antiDiagonalAttacks(sqr, occupancy);
	}
	public static long[] computeRookAttackSetVariations(Square sqr, long[] rookOccupancyVariations) {
		long[] rookAttVar = new long[rookOccupancyVariations.length];
		for (int i = 0; i < rookAttVar.length; i++)
			rookAttVar[i] = computeRookAttackSet(sqr, rookOccupancyVariations[i]);
		return rookAttVar;
	}
	public static long[] computeBishopAttackSetVariations(Square sqr, long[] bishopOccupancyVariations) {
		long[] bishopAttVar = new long[bishopOccupancyVariations.length];
		for (int i = 0; i < bishopAttVar.length; i++)
			bishopAttVar[i] = computeBishopAttackSet(sqr, bishopOccupancyVariations[i]);
		return bishopAttVar;
	}
	public static long[][] computeRookAttackSetVariations(long[][] rookOccupancyVariations) {
		int sqrInd;
		long[][] rookAttVar = new long[64][];
		for (Square sqr : Square.values()) {
			sqrInd = sqr.ordinal();
			rookAttVar[sqrInd] = computeRookAttackSetVariations(sqr, rookOccupancyVariations[sqrInd]);
		}
		return rookAttVar;
	}
	public static long[][] computeBishopAttackSetVariations(long[][] bishopOccupancyVariations) {
		int sqrInd;
		long[][] bishopAttVar = new long[64][];
		for (Square sqr : Square.values()) {
			sqrInd = sqr.ordinal();
			bishopAttVar[sqrInd] = computeBishopAttackSetVariations(sqr, bishopOccupancyVariations[sqrInd]);
		}
		return bishopAttVar;
	}
}