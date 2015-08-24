package engine;

import engine.Bitboard.Square;
import util.BitOperations;

/**A class that generates occupancy variations for rooks and bishops. It either generates a one-dimensional array
 * of variations for either of the two pieces for the specified square index or a two-dimensional array for all squares.
 * 
 * @author Viktor
 *
 */
public class SliderOccupancyVariationGenerator {

	/**Generates all the variations for the occupancy mask fed to it.
	 * 
	 * @param sqrInd
	 * @return
	 */
	public static long[] occupancyVariations(long occupancyMask) {
		int numOfSetBitsInMask = BitOperations.getCardinality(occupancyMask);
		int[] setBitsInMask = BitOperations.serialize(occupancyMask, numOfSetBitsInMask);
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
	/**For each square it generates all the relevant occupancy variations for a rook.
	 * 
	 * @return
	 */
	public static long[][] rookOccupancyVariations() {
		long[][] rookOccVar = new long[64][];
		for (Square sqr : Square.values())
			rookOccVar[sqr.ordinal()] = occupancyVariations(MaskGenerator.rookOccupancyMask(sqr));
		return rookOccVar;
	}
	/**For each square it generates all the relevant occupancy variations for a bishop.
	 * 
	 * @return
	 */
	public static long[][] bishopOccupancyVariations() {
		long[][] bishopOccVar = new long[64][];
		for (Square sqr : Square.values())
			bishopOccVar[sqr.ordinal()] = occupancyVariations(MaskGenerator.bishopOccupancyMask(sqr));
		return bishopOccVar;
	}
}