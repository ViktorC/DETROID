package engine;

import java.util.Random;

import util.BitOperations;
import engine.Bitboard.*;

/**A class that generates 64 bit 'magic' numbers for hashing occupancy variations onto an index in a pre-calculated sliding piece move database by multiplying the intersection
 * of the occupancy bitmap and the occupancy mask by the magic number, then right shifting the product by the magic shift value specific to the given square--calculated by extracting
 * the number of bits in the relevant occupancy mask from 64.
 * 
 * With better magics, even smaller tablebases can be produced. Sometimes magic numbers can be found that when multiplied by the occupancy bitmap can be shifted to the right by one more than
 * the usual number of bits and will still hash on the right index because of the existence of different occupancy variations that allow for the same attack sets. All one should have to do is
 * try generating magics with the usual shift value plus one for the squares for which one wishes to find better magics.
 * 
 * @author Viktor
 *
 */
public class Magics {
	
	/**A simple unencapsulated class for returning both the magic number and the magic shift value.
	 * 
	 * @author Viktor
	 *
	 */
	public static class MagicData {
		
		public boolean rook;
		public byte sqrInd;
		public long magicNumber;
		public byte magicShift;
		
		public MagicData(boolean rook, byte sqrInd, long magicNumber, byte magicShift) {
			this.rook = rook;
			this.sqrInd = sqrInd;
			this.magicNumber = magicNumber;
			this.magicShift = magicShift;
		}
		/**Format:
		 * TYPE: SQUARE (MAGIC_NUMBER, MAGIC_SHIFT)*/
		public String toString() {
			String type = rook ? "ROOK" : "BISHOP";
			return String.format("%-6s " + Square.getByIndex(sqrInd) + "  (" + BitOperations.toBinaryLiteral(magicNumber) + ", %2d),", type + ":", magicShift);
		}
	}
	
	private static long[][] rookOccupancyVariations;
	private static long[][] bishopOccupancyVariations;
	private static long[][] rookAttackSetVariations;
	private static long[][] bishopAttackSetVariations;
	
	static {
		rookOccupancyVariations = SliderAttack.rookOccupancyVariations();
		bishopOccupancyVariations = SliderAttack.bishopOccupancyVariations();
		rookAttackSetVariations = SliderAttack.rookAttackSetVariations(rookOccupancyVariations);
		bishopAttackSetVariations = SliderAttack.bishopAttackSetVariations(bishopOccupancyVariations);
	}
	/**Generates a magic number for the square specified by 'sqrInd' either for a rook or for a bishop depending on 'rook' and returns it in a
	 * {@link #engine.MagicNumberGenerator.Magics Magics} instance. If enhanced is set true, it will try to find a magic that can be right shifted by one more
	 * than the usual value resulting in denser tables.
	 * 
	 * @param sqrInd
	 * @param rook
	 * @param enhanced
	 * @return
	 */
	public static MagicData generateMagics(int sqrInd, boolean rook, boolean enhanced) {
		long[] magicDatabase;
		Random random = new Random();
		long magicNumber;
		int index, shift;
		boolean collision = false;
		long[] occVar, attVar;
		if (rook) {
			occVar = rookOccupancyVariations[sqrInd];
			attVar = rookAttackSetVariations[sqrInd];
		}
		else {
			occVar = bishopOccupancyVariations[sqrInd];
			attVar = bishopAttackSetVariations[sqrInd];
		}
		int variations = occVar.length;
		shift = 64 - BitOperations.indexOfBit(variations);
		if (enhanced)
			shift++; 
		magicDatabase = new long[variations];
		do {
			for (int i = 0; i < variations; i++)
				magicDatabase[i] = 0;
			collision = false;
			magicNumber = random.nextLong() & random.nextLong() & random.nextLong();
			for (int i = 0; i < variations; i++) {
				index = (int)((occVar[i]*magicNumber) >>> shift);
				if (magicDatabase[index] == 0)
					magicDatabase[index] = attVar[i];
				else if (magicDatabase[index] != attVar[i]) {
					collision = true;
					break;
				}
			}
		}
		while (collision);
		return new MagicData(rook, (byte)sqrInd, magicNumber, (byte)shift);
	}
	/**Generates magic numbers for each square either for a rook or a bishop depending on 'rook' and returns them in a {@link #engine.MagicNumberGenerator.Magics Magics} array.
	 * For the squares whose indices are fed to this method, it will attempt to find enhanced magics. If print is set, it also prints all the objects in the array to the console.
	 * @param rook
	 * @param print
	 * @param enhancedSquares
	 * @return
	 */
	public static MagicData[] generateMagics(boolean rook, boolean print, int... enhancedSquares) {
		MagicData[] allMagics = new MagicData[64];
		OuterLoop: for (int i = 0; i < 64; i++) {
			for (int sqr : enhancedSquares) {
				if (sqr == i) {
					allMagics[i] = generateMagics(i, rook, true);
					continue OuterLoop;
				}
			}
			allMagics[i] = generateMagics(i, rook, false);
		}
		if (print) {
			System.out.format("%-7s %-4s %-68s %s\n", "TYPE", "SQR", "MAGIC_NUMBER", "SHIFT");
			for (MagicData m : allMagics)
				System.out.println(m);
		}
		return allMagics;
	}
}
