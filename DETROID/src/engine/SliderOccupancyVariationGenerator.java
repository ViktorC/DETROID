package engine;

/**A class that generates possible occupancy variations for rooks and bishops. It either generates a one-dimensional array
 * of variations for either of the two pieces for the specified square index or a two-dimensional array for all squares.
 * 
 * @author Viktor
 *
 */
public class SliderOccupancyVariationGenerator {

	public static long[] generateRookOccupancyVariations(int sqrInd) {
		SliderOccupancyMask occupancyMask = SliderOccupancyMask.getByIndex(sqrInd);
		long mask = occupancyMask.rookOccupancyMask;
		byte numOfSetBitsInMask = occupancyMask.rookOccupancyMaskBitCount;
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
	public static long[] generateBishopOccupancyVariations(int sqrInd) {
		SliderOccupancyMask occupancyMask = SliderOccupancyMask.getByIndex(sqrInd);
		long mask = occupancyMask.bishopOccupancyMask;
		byte numOfSetBitsInMask = occupancyMask.bishopOccupancyMaskBitCount;
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
	public static long[][] generateRookOccupancyVariations() {
		long[][] rookOccVar = new long[64][];
		for (int i = 0; i < 64; i++)
			rookOccVar[i] = generateRookOccupancyVariations(i);
		return rookOccVar;
	}
	public static long[][] generateBishopOccupancyVariations() {
		long[][] bishopOccVar = new long[64][];
		for (int i = 0; i < 64; i++)
			bishopOccVar[i] = generateBishopOccupancyVariations(i);
		return bishopOccVar;
	}
}