package engine;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import engine.Bitboard.*;
import util.BitOperations;

/**
 * A class whose instance generates 64 bit 'magic' numbers for hashing occupancy variations onto an index in a pre-calculated sliding piece move
 * database by multiplying the intersection of the occupancy bitmap and the occupancy mask by the magic number, then right shifting the product by
 * the magic shift value specific to the given square--calculated by extracting the number of bits in the relevant occupancy mask from 64.
 * 
 * With better magics, even smaller tablebases can be produced. Sometimes magic numbers can be found that when multiplied by the occupancy bitmap
 * can be shifted to the right by one more than the usual number of bits and will still hash on the right index because of the existence of
 * different occupancy variations that allow for the same attack sets. All one should have to do is try generating magics with the usual shift
 * value plus one for the squares for which one wishes to find better magics.
 * 
 * @author Viktor
 *
 */
final class MagicsGenerator {
	
	/**
	 * A simple class for returning both the magic number and the magic shift value.
	 * 
	 * @author Viktor
	 *
	 */
	static class Magics {
		
		final boolean rook;
		final byte sqrInd;
		final long magicNumber;
		final byte magicShift;
		
		Magics(boolean rook, byte sqrInd, long magicNumber, byte magicShift) {
			this.rook = rook;
			this.sqrInd = sqrInd;
			this.magicNumber = magicNumber;
			this.magicShift = magicShift;
		}
		/**
		 * Format:
		 * TYPE: SQUARE (MAGIC_NUMBER, MAGIC_SHIFT)
		 */
		@Override
		public String toString() {
			String type = rook ? "ROOK" : "BISHOP";
			return String.format("%-6s " + Square.getByIndex(sqrInd) + "  (" + BitOperations.toHexLiteral(magicNumber) +
					", %2d),", type + ":", magicShift);
		}
	}
	
	private static MagicsGenerator INSTANCE = new MagicsGenerator();
	
	private long[][] rookOccupancyVariations;
	private long[][] bishopOccupancyVariations;
	private long[][] rookAttackSetVariations;
	private long[][] bishopAttackSetVariations;
	
	private MagicsGenerator() {
		long bit;
		long[] bishopOccupancyMask, rookOccupancyMask;
		bishopOccupancyMask = new long[64];
		rookOccupancyMask = new long[64];
		bishopOccupancyVariations = new long[64][];
		rookOccupancyVariations = new long[64][];
		bishopAttackSetVariations = new long[64][];
		rookAttackSetVariations = new long[64][];
		for (int i = 0; i < 64; i++) {
			bit = 1L << i;
			bishopOccupancyMask[i] = MultiMoveSets.bishopMoveSets(bit, 0, -1) & ~(File.A.bits | File.H.bits | Rank.R1.bits | Rank.R8.bits);
			rookOccupancyMask[i] = (Bitboard.northFill(bit, ~Rank.R8.bits) | Bitboard.southFill(bit, ~Rank.R1.bits) |
					Bitboard.westFill(bit, ~File.A.bits) | Bitboard.eastFill(bit, ~File.H.bits))^bit;
			bishopOccupancyVariations[i] = BitOperations.getAllSubsets(bishopOccupancyMask[i]);
			rookOccupancyVariations[i] = BitOperations.getAllSubsets(rookOccupancyMask[i]);
			bishopAttackSetVariations[i] = new long[bishopOccupancyVariations[i].length];
			rookAttackSetVariations[i] = new long[rookOccupancyVariations[i].length];
			for (int j = 0; j < bishopOccupancyVariations[i].length; j++)
				bishopAttackSetVariations[i][j] = MultiMoveSets.bishopMoveSets(bit, -1, ~bishopOccupancyVariations[i][j]);
			for (int j = 0; j < rookOccupancyVariations[i].length; j++)
				rookAttackSetVariations[i][j] = MultiMoveSets.rookMoveSets(bit, -1, ~rookOccupancyVariations[i][j]);
		}
	}
	/**
	 * Returns a {@link #engine.MagicGenerator MagicGenerator} instance.
	 * 
	 * @return
	 */
	static MagicsGenerator getInstance() {
		return INSTANCE;
	}
	/**
	 * Generates a magic number for the square specified by 'sqrInd' either for a rook or for a bishop depending on 'rook' and returns it in a
	 * {@link #engine.MagicGenerator.Magics Magics} instance. If enhanced is set true, it will try to find a magic that can be right
	 * shifted by one more than the usual value resulting in denser tables.
	 * called on.
	 * 
	 * @param sqrInd
	 * @param rook
	 * @param enhanced
	 * @return
	 */
	synchronized Magics generateMagics(int sqrInd, boolean rook, boolean enhanced) {
		final Random random = new Random();
		int shift;
		long[] occVar, attVar;
		Function<Integer, Long> gen;
		CompletionService<Long> pool;
		ArrayList<Future<Long>> futures;
		int numOfProcessors;
		long num;
		if (rook) {
			occVar = rookOccupancyVariations[sqrInd];
			attVar = rookAttackSetVariations[sqrInd];
		}
		else {
			occVar = bishopOccupancyVariations[sqrInd];
			attVar = bishopAttackSetVariations[sqrInd];
		}
		int variations = occVar.length;
		gen = (s) -> {
			long[] magicDatabase;
			long magicNumber;
			boolean collision;
			int index;
			magicDatabase = new long[variations];
			collision = false;
			do {
				for (int i = 0; i < variations; i++)
					magicDatabase[i] = 0;
				collision = false;
				magicNumber = random.nextLong() & random.nextLong() & random.nextLong();
				for (int i = 0; i < variations; i++) {
					index = (int)((occVar[i]*magicNumber) >>> s);
					if (magicDatabase[index] == 0)
						magicDatabase[index] = attVar[i];
					else if (magicDatabase[index] != attVar[i]) {
						collision = true;
						break;
					}
				}
			} while (collision);
			return magicNumber;
		};
		shift = 64 - BitOperations.indexOfBit(variations);
		if (enhanced) {
			numOfProcessors = Runtime.getRuntime().availableProcessors();
			pool = new ExecutorCompletionService<Long>(Executors.newFixedThreadPool(numOfProcessors));
			futures = new ArrayList<>();
			for (int i = 0; i < numOfProcessors; i++)
				futures.add(pool.submit(() -> gen.apply(shift + 1)));
			try {
				num = pool.take().get();
			} catch (InterruptedException | ExecutionException e) { num = 0; e.printStackTrace(); }
			for (Future<Long> f : futures)
				f.cancel(true);
		}
		else
			num = gen.apply(shift);
		return new Magics(rook, (byte)sqrInd, num, (byte)shift);
	}
	/**
	 * Generates magic numbers for each square either for a rook or a bishop depending on 'rook' and returns them in a
	 * {@link #engine.MagicNumberGenerator.Magics Magics} array. For the squares whose indices are fed to this method, it will attempt to
	 * find enhanced magics. If print is set, it also prints all the objects in the array to the console.
	 * 
	 * @param rook
	 * @param print
	 * @param enhancedSquares
	 * @return
	 */
	synchronized Magics[] generateAllMagics(boolean rook, boolean print, int... enhancedSquares) {
		Magics[] allMagics = new Magics[64];
		Magics m;
		if (print)
			System.out.format("%-6s %-3s %-21s %s\n", "TYPE", "SQR", "MAGIC_NUMBER", "SHIFT");
		OuterLoop: for (int i = 0; i < 64; i++) {
			for (int sqr : enhancedSquares) {
				if (sqr == i) {
					m = generateMagics(i, rook, true);
					allMagics[i] = m;
					if (print)
						System.out.println(m);
					continue OuterLoop;
				}
			}
			m = generateMagics(i, rook, false);
			allMagics[i] = generateMagics(i, rook, false);
			if (print)
				System.out.println(m);
		}
		return allMagics;
	}
}
