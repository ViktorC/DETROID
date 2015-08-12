package engine.board;


/**An enum type for the 8 files/columns of a chess board. Each constant has a field that contains a long with only the bits falling on the
 * file set.
 * 
 * @author Viktor
 *
 */
public enum File {
	
	A (0b0000000100000001000000010000000100000001000000010000000100000001L),
	B (0b0000001000000010000000100000001000000010000000100000001000000010L),
	C (0b0000010000000100000001000000010000000100000001000000010000000100L),
	D (0b0000100000001000000010000000100000001000000010000000100000001000L),
	E (0b0001000000010000000100000001000000010000000100000001000000010000L),
	F (0b0010000000100000001000000010000000100000001000000010000000100000L),
	G (0b0100000001000000010000000100000001000000010000000100000001000000L),
	H (0b1000000010000000100000001000000010000000100000001000000010000000L);
	
	public final long bitmap;
	
	private File(long bitmap) {
		this.bitmap = bitmap;
	}
	/**Returns a the numeric representation of a file of the chess board with only the bits falling on the specified file set.
	 * 
	 * @param fileInd the index of the file*/
	public static long getByIndex(int fileInd) {
		switch(fileInd) {
			case 0:  return 0b0000000100000001000000010000000100000001000000010000000100000001L;
			case 1:  return 0b0000001000000010000000100000001000000010000000100000001000000010L;
			case 2:  return 0b0000010000000100000001000000010000000100000001000000010000000100L;
			case 3:  return 0b0000100000001000000010000000100000001000000010000000100000001000L;
			case 4:  return 0b0001000000010000000100000001000000010000000100000001000000010000L;
			case 5:  return 0b0010000000100000001000000010000000100000001000000010000000100000L;
			case 6:  return 0b0100000001000000010000000100000001000000010000000100000001000000L;
			case 7:  return 0b1000000010000000100000001000000010000000100000001000000010000000L;
			default: throw new IllegalArgumentException("Invalid file index.");
		}
	}
	/**Returns a the numeric representation of the file of the chess board on which the input parameter square lies with only
	 * the relevant bits set.
	 * 
	 * @param sqr a Square enum*/
	public static long getBySquare(Square sqr) {
		return getByIndex(sqr.ordinal() & 7);
	}
	/**Returns a the numeric representation of the file of the chess board on which the input parameter square lies with only
	 * the relevant bits set.
	 * 
	 * @param sqrInd the index of the square*/
	public static long getBySquareIndex(int sqrInd) {
		return getByIndex(sqrInd & 7);
	}
}