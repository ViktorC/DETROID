package engine.board;


/**An enum type for the 8 ranks/rows of a chess board. Each constant has a field that contains a long with only the byte on the rank's
 * index set.
 * 
 * @author Viktor
 *
 */
public enum Rank {
	
	R1 (0b0000000000000000000000000000000000000000000000000000000011111111L),
	R2 (0b0000000000000000000000000000000000000000000000001111111100000000L),
	R3 (0b0000000000000000000000000000000000000000111111110000000000000000L),
	R4 (0b0000000000000000000000000000000011111111000000000000000000000000L),
	R5 (0b0000000000000000000000001111111100000000000000000000000000000000L),
	R6 (0b0000000000000000111111110000000000000000000000000000000000000000L),
	R7 (0b0000000011111111000000000000000000000000000000000000000000000000L),
	R8 (0b1111111100000000000000000000000000000000000000000000000000000000L);
	
	public final long bitmap;
	
	private Rank(long bitmap) {
		this.bitmap = bitmap;
	}
	/**Returns a the numeric representation of a rank of the chess board with only the byte on the rank's index set.
	 * 
	 * @param rnkInd the index of the rank*/
	public static long getByIndex(int rnkInd) {
		switch(rnkInd) {
			case 0:  return 0b0000000000000000000000000000000000000000000000000000000011111111L;
			case 1:  return 0b0000000000000000000000000000000000000000000000001111111100000000L;
			case 2:  return 0b0000000000000000000000000000000000000000111111110000000000000000L;
			case 3:  return 0b0000000000000000000000000000000011111111000000000000000000000000L;
			case 4:  return 0b0000000000000000000000001111111100000000000000000000000000000000L;
			case 5:  return 0b0000000000000000111111110000000000000000000000000000000000000000L;
			case 6:  return 0b0000000011111111000000000000000000000000000000000000000000000000L;
			case 7:  return 0b1111111100000000000000000000000000000000000000000000000000000000L;
			default: throw new IllegalArgumentException("Invalid rank index.");
		}
	}
	/**Returns a the numeric representation of the rank of the chess board on which the input parameter square lies with only
	 * the byte on the rank's index set.
	 * 
	 * @param sqr a Square enum*/
	public static long getBySquare(Square sqr) {
		return getByIndex(sqr.ordinal() >>> 3);
	}
	/**Returns a the numeric representation of the rank of the chess board on which the input parameter square lies with only
	 * the byte on the rank's index set.
	 * 
	 * @param sqrInd the index of the square*/
	public static long getBySquareIndex(int sqrInd) {
		return getByIndex(sqrInd >>> 3);
	}
}