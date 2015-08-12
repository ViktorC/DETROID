package engine;

import java.util.Random;

import engine.Board.SliderAttackSetCalculator;
import engine.Board.SliderOccupancyMask;
import engine.Board.SliderOccupancyVariationGenerator;
import engine.Board.Square;

/**A class that generates 64 bit 'magic' numbers for hashing occupancy variations onto an index in a pre-calculated sliding piece move database by multiplying the intersection
 * of the occupancy bitmap and the occupancy mask, then right shifting the product by the magic shift value specific to the given square--calculated by extracting the number of
 * bits in the occupancy mask from 64.
 * 
 * @author Viktor
 *
 */
public class MagicNumberGenerator {
	
	private static long[][] rookOccupancyVariations;
	private static long[][] bishopOccupancyVariations;
	private static long[][] rookAttackSetVariations;
	private static long[][] bishopAttackSetVariations;
	
	private long[] rookMagicNumbers;
	private long[] bishopMagicNumbers;
	
	static {
		rookOccupancyVariations = SliderOccupancyVariationGenerator.generateRookOccupancyVariations();
		bishopOccupancyVariations = SliderOccupancyVariationGenerator.generateBishopOccupancyVariations();
		rookAttackSetVariations = SliderAttackSetCalculator.computeRookAttackSetVariations(rookOccupancyVariations);
		bishopAttackSetVariations = SliderAttackSetCalculator.computeBishopAttackSetVariations(bishopOccupancyVariations);
	}
	/**Returns an 64-element array of the rook magic numbers.
	 * 
	 * @return
	 */
	public long[] getRookMagicNumbers() {
		return this.rookMagicNumbers;
	}
	/**Returns an 64-element array of the bishop magic numbers.
	 * 
	 * @return
	 */
	public long[] getBishopMagicNumbers() {
		return this.bishopMagicNumbers;
	}
	/**Generates the rook magic numbers and stores them in the instance field accessible by {@link #getRookMagicNumbers getRookMagicNumbers}.*/
	public void generateRookMagicNumbers() {
		rookMagicNumbers = new long[64];
		long[][] magicRookDatabase = new long[64][];
		Random random = new Random();
		long magicNumber;
		int index;
		boolean collision = false;
		for (int i = 0; i < 64; i++) {
			long[] occVar = rookOccupancyVariations[i];
			long[] attVar = rookAttackSetVariations[i];
			int variations = occVar.length;
			magicRookDatabase[i] = new long[variations];
			do {
				for (int j = 0; j < variations; j++)
					magicRookDatabase[i][j] = 0;
				collision = false;
				magicNumber = random.nextLong() & random.nextLong() & random.nextLong();
				rookMagicNumbers[i] = magicNumber;
				for (int j = 0; j < variations; j++) {
					index = (int)((occVar[j]*magicNumber) >>> (64 - SliderOccupancyMask.getByIndex(i).rookOccupancyMaskBitCount));
					if (magicRookDatabase[i][index] == 0)
						magicRookDatabase[i][index] = attVar[j];
					else if (magicRookDatabase[i][index] != attVar[j]) {
						collision = true;
						break;
					}
				}
			}
			while (collision);
		}
	}
	/**Generates the bishop magic numbers and stores them in the instance field accessible by {@link #getBishopMagicNumbers getBishopMagicNumbers}.*/
	public void generateBishopMagicNumbers() {
		bishopMagicNumbers = new long[64];
		long[][] magicBishopDatabase = new long[64][];
		Random random = new Random();
		long magicNumber;
		int index;
		boolean collision = false;
		for (int i = 0; i < 64; i++) {
			long[] occVar = bishopOccupancyVariations[i];
			long[] attVar = bishopAttackSetVariations[i];
			int variations = occVar.length;
			magicBishopDatabase[i] = new long[variations];
			do {
				for (int j = 0; j < variations; j++)
					magicBishopDatabase[i][j] = 0;
				collision = false;
				magicNumber = random.nextLong() & random.nextLong() & random.nextLong();
				bishopMagicNumbers[i] = magicNumber;
				for (int j = 0; j < variations; j++) {
					index = (int)((occVar[j]*magicNumber) >>> (64 - SliderOccupancyMask.getByIndex(i).bishopOccupancyMaskBitCount));
					if (magicBishopDatabase[i][index] == 0)
						magicBishopDatabase[i][index] = attVar[j];
					else if (magicBishopDatabase[i][index] != attVar[j]) {
						collision = true;
						break;
					}
				}
			}
			while (collision);
		}
	}
	/**Generates both the rook and bishop magic numbers and stores them in the instance fields accessible by {@link #getRookMagicNumbers getRookMagicNumbers}
	 * and {@link #getBishopMagicNumbers getBishopMagicNumbers}.*/
	public void generateMagicNumbers() {
		this.generateRookMagicNumbers();
		this.generateBishopMagicNumbers();
	}
	/**Prints all the already generated magic numbers to the console.*/
	public void printMagicNumbers() {
		if (this.rookMagicNumbers != null || this.bishopMagicNumbers != null) {
			if (this.rookMagicNumbers == null)
				this.rookMagicNumbers = new long[64];
			if (this.bishopMagicNumbers == null)
				this.bishopMagicNumbers = new long[64];
			System.out.format("%s %5s %70s\n\n", "SQR", "ROOK", "BISHOP");
			for (Square sqr : Square.values()) {
				int sqrInd = sqr.ordinal();
				System.out.println(sqr + "  (" + BitOperations.toBinaryLiteral(this.rookMagicNumbers[sqrInd]) + ", " + BitOperations.toBinaryLiteral(this.bishopMagicNumbers[sqrInd]) + "),");
			}
		}
		else
			System.out.println("No magic numbers have been generated so far.");
	}
}