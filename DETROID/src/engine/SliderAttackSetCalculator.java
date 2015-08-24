package engine;

import util.BitOperations;
import engine.Bitboard.*;

/**A class that generates the attack set(s) for a rook or a bishop on the specified square for the given relevant occupancy or array of occupancy variations,
 * or for multiple rooks or bishops based on the occupancy bitmap. It can also generate the attack sets for all possible occupancy variations on all squares
 * either for the rook or for the bishop. It uses ray-wise parallel-prefix algorithms to determine the attack sets.
 * 
 * @author Viktor
 *
 */
public class SliderAttackSetCalculator {
	
	/**A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction north of multiple sliding pieces at the same time. The generator
	 * is usually the set of pieces to be shifted, and the propagator is the set of empty squares.
	 * 
	 * @param generator - piece squares
	 * @param propagator - all empty squares
	 * @return
	 */
	public static long northFill(long generator, long propagator) {
		generator  |= (generator  << 8)  & propagator;
		propagator &= (propagator << 8);
		generator  |= (generator  << 16) & propagator;
		propagator &= (propagator << 16);
		generator  |= (generator  << 32) & propagator;
		return generator;
	}
	/**A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction south of multiple sliding pieces at the same time. The generator
	 * is usually the set of pieces to be shifted, and the propagator is the set of empty squares.
	 * 
	 * @param generator - piece squares
	 * @param propagator - all empty squares
	 * @return
	 */
	public static long southFill(long generator, long propagator) {
		generator  |= (generator  >>> 8)  & propagator;
		propagator &= (propagator >>> 8);
		generator  |= (generator  >>> 16) & propagator;
		propagator &= (propagator >>> 16);
		generator  |= (generator  >>> 32) & propagator;
		return generator;
	}
	/**A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction west of multiple sliding pieces at the same time. The generator
	 * is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect is handled by the method.
	 * 
	 * @param generator - piece squares
	 * @param propagator - all empty squares
	 * @return
	 */
	public static long westFill(long generator, long propagator) {
		propagator &= 0b0111111101111111011111110111111101111111011111110111111101111111L;
		generator  |= (generator  >>> 1) & propagator;
		propagator &= (propagator >>> 1);
		generator  |= (generator  >>> 2) & propagator;
		propagator &= (propagator >>> 2);
		generator  |= (generator  >>> 4) & propagator;
		return generator;
	}
	/**A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction east of multiple sliding pieces at the same time. The generator
	 * is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect is handled by the method.
	 * 
	 * @param generator - piece squares
	 * @param propagator - all empty squares
	 * @return
	 */
	public static long eastFill(long generator, long propagator) {
		propagator &= 0b1111111011111110111111101111111011111110111111101111111011111110L;
		generator  |= (generator  << 1) & propagator;
		propagator &= (propagator << 1);
		generator  |= (generator  << 2) & propagator;
		propagator &= (propagator << 2);
		generator  |= (generator  << 4) & propagator;
		return generator;
	}
	/**A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction north-west of multiple sliding pieces at the same time. The
	 * generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect is handled by the method.
	 * 
	 * @param generator - piece squares
	 * @param propagator - all empty squares
	 * @return
	 */
	public static long northWestFill(long generator, long propagator) {
		propagator &= 0b0111111101111111011111110111111101111111011111110111111101111111L;
		generator  |= (generator  << 7)  & propagator;
		propagator &= (propagator << 7);
		generator  |= (generator  << 14) & propagator;
		propagator &= (propagator << 14);
		generator  |= (generator  << 28) & propagator;
		return generator;
	}
	/**A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction north-east of multiple sliding pieces at the same time. The
	 * generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect is handled by the method.
	 * 
	 * @param generator - piece squares
	 * @param propagator - all empty squares
	 * @return
	 */
	public static long northEastFill(long generator, long propagator) {
		propagator &= 0b1111111011111110111111101111111011111110111111101111111011111110L;
		generator  |= (generator  << 9)  & propagator;
		propagator &= (propagator << 9);
		generator  |= (generator  << 18) & propagator;
		propagator &= (propagator << 18);
		generator  |= (generator  << 36) & propagator;
		return generator;
	}
	/**A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction south-west of multiple sliding pieces at the same time. The
	 * generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect is handled by the method.
	 * 
	 * @param generator - piece squares
	 * @param propagator - all empty squares
	 * @return
	 */
	public static long southWestFill(long generator, long propagator) {
		propagator &= 0b0111111101111111011111110111111101111111011111110111111101111111L;
		generator  |= (generator  >>> 9)  & propagator;
		propagator &= (propagator >>> 9);
		generator  |= (generator  >>> 18) & propagator;
		propagator &= (propagator >>> 18);
		generator  |= (generator  >>> 36) & propagator;
		return generator;
	}
	/**A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction south-east of multiple sliding pieces at the same time. The
	 * generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around effect is handled by the method.
	 * 
	 * @param generator - piece squares
	 * @param propagator - all empty squares
	 * @return
	 */
	public static long southEastFill(long generator, long propagator) {
		propagator &= 0b1111111011111110111111101111111011111110111111101111111011111110L;
		generator  |= (generator  >>> 7)  & propagator;
		propagator &= (propagator >>> 7);
		generator  |= (generator  >>> 14) & propagator;
		propagator &= (propagator >>> 14);
		generator  |= (generator  >>> 28) & propagator;
		return generator;
	}
	/**A bit parallel method that returns the combined move (non-attack) sets of multiple rooks simultaneously.
	 * 
	 * @param rooks
	 * @param empty
	 * @return
	 */
	public static long multiRookMoveSet(long rooks, long empty) {
		return northFill(rooks, empty) | southFill(rooks, empty) | westFill(rooks, empty) | eastFill(rooks, empty);
	}
	/**A bit parallel method that returns the combined move (non-attack) sets of multiple rooks simultaneously.
	 * 
	 * @param bishops
	 * @param empty
	 * @return
	 */
	public static long multiBishopMoveSet(long bishops, long empty) {
		return northWestFill(bishops, empty) | southWestFill(bishops, empty) | northEastFill(bishops, empty) | southEastFill(bishops, empty);
	}
	/**A bit parallel method that returns the combined attack sets of multiple rooks simultaneously.
	 * 
	 * @param rooks
	 * @param empty
	 * @param enemyOccupied
	 * @return
	 */
	public static long multiRookAttackSet(long rooks, long empty, long enemyOccupied) {
		long gen, attackSet = 0;
		gen = northFill(rooks, empty);
		attackSet |= gen | ((gen << 8) & enemyOccupied);
		gen = southFill(rooks, empty);
		attackSet |= gen | ((gen >>> 8) & enemyOccupied);
		gen = westFill(rooks, empty);
		attackSet |= gen | ((gen >>> 1) & enemyOccupied);
		gen = eastFill(rooks, empty);
		attackSet |= gen | ((gen << 1) & enemyOccupied);
		return attackSet;
	}
	/**A bit parallel method that returns the combined attack sets of multiple bishops simultaneously.
	 * 
	 * @param bishops
	 * @param empty
	 * @param enemyOccupied
	 * @return
	 */
	public static long multiBishopAttackSet(long bishops, long empty, long enemyOccupied) {
		long gen, attackSet = 0;
		gen = northWestFill(bishops, empty);
		attackSet |= gen | ((gen << 7) & enemyOccupied);
		gen = southWestFill(bishops, empty);
		attackSet |= gen | ((gen >>> 9) & enemyOccupied);
		gen = northEastFill(bishops, empty);
		attackSet |= gen | ((gen << 9) & enemyOccupied);
		gen = southEastFill(bishops, empty);
		attackSet |= gen | ((gen >>> 7) & enemyOccupied);
		return attackSet;
	}
	/**Returns the rank-wise attack set for the relevant occupancy from the defined square.
	 * 
	 * @param sqr - the square on which the slider is
	 * @param relevantOccupancy - the relevant occupancy of the rank on which the square is (all occupied AND rank [minus the perimeter squares, as they make no difference])
	 * @return
	 */
	public static long rankAttackSet(Square sqr, long relevantOccupancy) {
		int sqrInd = sqr.ordinal();
		Rank rank = Rank.getBySquareIndex(sqrInd);
		long forward, reverse;
		forward  = rank.bitmap & relevantOccupancy;
		reverse  = BitOperations.reverse(relevantOccupancy);
		forward -= 2*sqr.bitmap;
		reverse -= 2*BitOperations.reverse(sqr.bitmap);
		forward ^= BitOperations.reverse(reverse);
		return forward & rank.bitmap;
	}
	/**Returns the file-wise attack set for the relevant occupancy from the defined square.
	 * 
	 * @param sqr - the square on which the slider is
	 * @param relevantOccupancy - the relevant occupancy of the file on which the square is (all occupied AND file [minus the perimeter squares, as they make no difference])
	 * @return
	 */
	public static long fileAttackSet(Square sqr, long relevantOccupancy) {
		int sqrInd = sqr.ordinal();
		File file = File.getBySquareIndex(sqrInd);
		long forward, reverse;
		forward  = file.bitmap & relevantOccupancy;
		reverse  = BitOperations.reverseBytes(forward);
		forward -= sqr.bitmap;
		reverse -= BitOperations.reverseBytes(sqr.bitmap);
		forward ^= BitOperations.reverseBytes(reverse);
		return forward & file.bitmap;
	}
	/**Returns the diagonal-wise attack set for the relevant occupancy from the defined square.
	 * 
	 * @param sqr - the square on which the slider is
	 * @param relevantOccupancy - the relevant occupancy of the diagonal on which the square is (all occupied AND diagonal [minus the perimeter squares, as they make no difference])
	 * @return
	 */
	public static long diagonalAttackSet(Square sqr, long relevantOccupancy) {
		int sqrInd = sqr.ordinal();
		Diagonal diagonal = Diagonal.getBySquareIndex(sqrInd);
		long forward, reverse;
		forward  = diagonal.bitmap & relevantOccupancy;
		reverse  = BitOperations.reverseBytes(forward);
		forward -= sqr.bitmap;
		reverse -= BitOperations.reverseBytes(sqr.bitmap);
		forward ^= BitOperations.reverseBytes(reverse);
		return forward & diagonal.bitmap;
	}
	/**Returns the anti-diagonal-wise attack set for the relevant occupancy from the defined square.
	 * 
	 * @param sqr - the square on which the slider is
	 * @param relevantOccupancy - the relevant occupancy of the anti-diagonal on which the square is (all occupied AND anti-diagonal [minus the perimeter squares, as they make no difference])
	 * @return
	 */
	public static long antiDiagonalAttackSet(Square sqr, long relevantOccupancy) {
		int sqrInd = sqr.ordinal();
		AntiDiagonal antiDiagonal = AntiDiagonal.getBySquareIndex(sqrInd);
		long forward, reverse;
		forward  = antiDiagonal.bitmap & relevantOccupancy;
		reverse  = BitOperations.reverseBytes(forward);
		forward -= sqr.bitmap;
		reverse -= BitOperations.reverseBytes(sqr.bitmap);
		forward ^= BitOperations.reverseBytes(reverse);
		return forward & antiDiagonal.bitmap;
	}
	/**Returns a rook's attack set from the defined square with the given occupancy.
	 * 
	 * @param sqr - the square on which the rook is
	 * @param relevantOccupancy - the relevant occupancy of the rank and file that cross each other on the specified square
	 * (all occupied AND (rank OR file) [minus the perimeter squares, as they make no difference])
	 * @return
	 */
	public static long rookAttackSet(Square sqr, long occupancy) {
		return rankAttackSet(sqr, occupancy) | fileAttackSet(sqr, occupancy);
	}
	/**Returns a bishop's attack set from the defined square with the given occupancy.
	 * 
	 * @param sqr - the square on which the bishop is
	 * @param relevantOccupancy - the relevant occupancy of the diagonal and anti-diagonal that cross each other on the specified square
	 * (all occupied AND (diagonal OR anti-diagonal) [minus the perimeter squares, as they make no difference])
	 * @return
	 */
	public static long bishopAttackSet(Square sqr, long occupancy) {
		return diagonalAttackSet(sqr, occupancy) | antiDiagonalAttackSet(sqr, occupancy);
	}
	/**Returns a rook's attack set variations from the given square for all occupancy variations specified.
	 * 
	 * @param sqr - the square on which the bishop is
	 * @param rookOccupancyVariations - the occupancy variations within the rook's occupancy mask
	 * @return
	 */
	public static long[] rookAttackSetVariations(Square sqr, long[] rookOccupancyVariations) {
		long[] rookAttVar = new long[rookOccupancyVariations.length];
		for (int i = 0; i < rookAttVar.length; i++)
			rookAttVar[i] = rookAttackSet(sqr, rookOccupancyVariations[i]);
		return rookAttVar;
	}
	/**Returns a bishop's attack set variations from the given square for all occupancy variations specified.
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
	/**Returns a rook's attack set variations from each square for all occupancy variations specified.
	 * 
	 * @param rookOccupancyVariations - the occupancy variations within the rook's occupancy mask for each square
	 * @return
	 */
	public static long[][] rookAttackSetVariations(long[][] rookOccupancyVariations) {
		int sqrInd;
		long[][] rookAttVar = new long[64][];
		for (Square sqr : Square.values()) {
			sqrInd = sqr.ordinal();
			rookAttVar[sqrInd] = rookAttackSetVariations(sqr, rookOccupancyVariations[sqrInd]);
		}
		return rookAttVar;
	}
	/**Returns a bishop's attack set variations from each square for all occupancy variations specified.
	 * 
	 * @param bishopAttackSetVariations - the occupancy variations within the bishop's occupancy mask for each square
	 * @return
	 */
	public static long[][] bishopAttackSetVariations(long[][] bishopOccupancyVariations) {
		int sqrInd;
		long[][] bishopAttVar = new long[64][];
		for (Square sqr : Square.values()) {
			sqrInd = sqr.ordinal();
			bishopAttVar[sqrInd] = bishopAttackSetVariations(sqr, bishopOccupancyVariations[sqrInd]);
		}
		return bishopAttVar;
	}
}
