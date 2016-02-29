package engine;

import util.BitOperations;
import util.ByteList;
import engine.Board.Square;
import engine.Board.*;

/**
 * A class for generating occupancy variations and the attack set(s) for a rook or a bishop on the specified square for the given relevant
 * occupancy or array of occupancy variations. It can also generate the attack sets for all possible occupancy variations on all squares either
 * for the rook or for the bishop. It uses ray-wise parallel-prefix algorithms to determine the attack sets.
 * 
 * @author Viktor
 *
 */
public final class SliderAttack {
	
	private SliderAttack() {
		
	}
	/**
	 * Returns the rank-wise attack set for the relevant occupancy from the defined square.
	 * 
	 * @param sqr - the square on which the slider is
	 * @param relevantOccupancy - the relevant occupancy of the rank on which the square is (all occupied AND rank [minus the perimeter squares,
	 * as they make no difference])
	 * @return
	 */
	public static long rankAttackSet(Square sqr, long relevantOccupancy) {
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
	 * @param sqr - the square on which the slider is
	 * @param relevantOccupancy - the relevant occupancy of the file on which the square is (all occupied AND file [minus the perimeter squares,
	 * as they make no difference])
	 * @return
	 */
	public static long fileAttackSet(Square sqr, long relevantOccupancy) {
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
	 * @param sqr - the square on which the slider is
	 * @param relevantOccupancy - the relevant occupancy of the diagonal on which the square is (all occupied AND diagonal [minus the perimeter
	 * squares, as they make no difference])
	 * @return
	 */
	public static long diagonalAttackSet(Square sqr, long relevantOccupancy) {
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
	 * @param sqr - the square on which the slider is
	 * @param relevantOccupancy - the relevant occupancy of the anti-diagonal on which the square is (all occupied AND anti-diagonal
	 * [minus the perimeter squares, as they make no difference])
	 * @return
	 */
	public static long antiDiagonalAttackSet(Square sqr, long relevantOccupancy) {
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
	 * @param sqr - the square on which the rook is
	 * @param relevantOccupancy - the relevant occupancy of the rank and file that cross each other on the specified square
	 * (all occupied AND (rank OR file) [minus the perimeter squares, as they make no difference])
	 * @return
	 */
	public static long rookAttackSet(Square sqr, long occupancy) {
		return rankAttackSet(sqr, occupancy) | fileAttackSet(sqr, occupancy);
	}
	/**
	 * Returns a bishop's attack set from the defined square with the given occupancy.
	 * 
	 * @param sqr - the square on which the bishop is
	 * @param relevantOccupancy - the relevant occupancy of the diagonal and anti-diagonal that cross each other on the specified square
	 * (all occupied AND (diagonal OR anti-diagonal) [minus the perimeter squares, as they make no difference])
	 * @return
	 */
	public static long bishopAttackSet(Square sqr, long occupancy) {
		return diagonalAttackSet(sqr, occupancy) | antiDiagonalAttackSet(sqr, occupancy);
	}
	/**
	 * Generates all the variations for the occupancy mask fed to it.
	 * 
	 * @param sqrInd
	 * @return
	 */
	public static long[] occupancyVariations(long occupancyMask) {
		byte numOfBitsInMask = BitOperations.getCardinality(occupancyMask);
		byte[] occMaskBitIndArr = BitOperations.serialize(occupancyMask, numOfBitsInMask);
		ByteList occVarBitIndList;
		int numOfVar = 1 << numOfBitsInMask;
		long[] occVarArr = new long[numOfVar];
		long occVar;
		for (int i = 0; i < numOfVar; i++) {
			occVarBitIndList = BitOperations.serialize(i);
			occVar = 0L;
			while (occVarBitIndList.hasNext())
				occVar |= (1L << occMaskBitIndArr[occVarBitIndList.next()]);
			occVarArr[i] = occVar;
		}
		return occVarArr;
	}
	/**
	 * For each square it generates all the relevant occupancy variations for a rook.
	 * 
	 * @return
	 */
	public static long[][] rookOccupancyVariations() {
		long[][] rookOccVar = new long[64][];
		for (Square sqr : Square.values())
			rookOccVar[sqr.ordinal()] = occupancyVariations(MoveMask.rookOccupancyMask(sqr));
		return rookOccVar;
	}
	/**
	 * For each square it generates all the relevant occupancy variations for a bishop.
	 * 
	 * @return
	 */
	public static long[][] bishopOccupancyVariations() {
		long[][] bishopOccVar = new long[64][];
		for (Square sqr : Square.values())
			bishopOccVar[sqr.ordinal()] = SliderAttack.occupancyVariations(MoveMask.bishopOccupancyMask(sqr));
		return bishopOccVar;
	}
	public static long[] rookAttackSetVariations(Square sqr, long[] rookOccupancyVariations) {
		long[] rookAttVar = new long[rookOccupancyVariations.length];
		for (int i = 0; i < rookAttVar.length; i++)
			rookAttVar[i] = rookAttackSet(sqr, rookOccupancyVariations[i]);
		return rookAttVar;
	}
	/**
	 * Returns a bishop's attack set variations from the given square for all occupancy variations specified.
	 * 
	 * @param sqr - the square on which the bishop is
	 * @param bishopOccupancyVariations - the occupancy variations within the bishop's occupancy mask
	 * @return
	 */
	public static long[] bishopAttackSetVariations(Square sqr, long[] bishopOccupancyVariations) {
		long[] bishopAttVar = new long[bishopOccupancyVariations.length];
		for (int i = 0; i < bishopAttVar.length; i++)
			bishopAttVar[i] = bishopAttackSet(sqr, bishopOccupancyVariations[i]);
		return bishopAttVar;
	}
	/**
	 * Returns a rook's attack set variations from each square for all occupancy variations specified.
	 * 
	 * @param rookOccupancyVariations - the occupancy variations within the rook's occupancy mask for each square
	 * @return
	 */
	public static long[][] rookAttackSetVariations(long[][] rookOccupancyVariations) {
		long[][] rookAttVar = new long[64][];
		for (Square sqr : Square.values())
			rookAttVar[sqr.ind] = rookAttackSetVariations(sqr, rookOccupancyVariations[sqr.ind]);
		return rookAttVar;
	}
	/**
	 * Returns a bishop's attack set variations from each square for all occupancy variations specified.
	 * 
	 * @param bishopAttackSetVariations - the occupancy variations within the bishop's occupancy mask for each square
	 * @return
	 */
	public static long[][] bishopAttackSetVariations(long[][] bishopOccupancyVariations) {
		long[][] bishopAttVar = new long[64][];
		for (Square sqr : Square.values())
			bishopAttVar[sqr.ind] = bishopAttackSetVariations(sqr, bishopOccupancyVariations[sqr.ind]);
		return bishopAttVar;
	}
}
