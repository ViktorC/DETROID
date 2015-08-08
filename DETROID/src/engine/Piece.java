package engine;

/**An enum type for the different chess pieces. Each piece has an initial position and and id number by which it is represented in the
 * array of the auxiliary offset board.
 * 
 * @author Viktor
 *
 */
public enum Piece {
	
	WHITE_KING		(0b0000000000000000000000000000000000000000000000000000000000010000L, 1,  'K'),
	WHITE_QUEEN		(0b0000000000000000000000000000000000000000000000000000000000001000L, 2,  'Q'),
	WHITE_ROOK		(0b0000000000000000000000000000000000000000000000000000000010000001L, 3,  'R'),
	WHITE_BISHOP	(0b0000000000000000000000000000000000000000000000000000000000100100L, 4,  'B'),
	WHITE_KNIGHT 	(0b0000000000000000000000000000000000000000000000000000000001000010L, 5,  'N'),
	WHITE_PAWN		(0b0000000000000000000000000000000000000000000000001111111100000000L, 6,  'P'),
	
	BLACK_KING		(0b0001000000000000000000000000000000000000000000000000000000000000L, 7,  'k'),
	BLACK_QUEEN		(0b0000100000000000000000000000000000000000000000000000000000000000L, 8,  'q'),
	BLACK_ROOK		(0b1000000100000000000000000000000000000000000000000000000000000000L, 9,  'r'),
	BLACK_BISHOP	(0b0010010000000000000000000000000000000000000000000000000000000000L, 10, 'b'),
	BLACK_KNIGHT	(0b0100001000000000000000000000000000000000000000000000000000000000L, 11, 'n'),
	BLACK_PAWN		(0b0000000011111111000000000000000000000000000000000000000000000000L, 12, 'p');
	
	public final long initPosBitmap;		//a bitmap representing the initial position of the respective pieces at the start of the game
	public final int  numericNotation;		//a number between 1 and 12 that generally represents the respective piece, among others, for example on the offset board
	public final char fenNotation;			//a character denoting the piece-type in FEN notation
	
	private Piece(long initPosBitmap, int numericNotation, char fenNotation) {
		this.initPosBitmap = initPosBitmap;
		this.numericNotation = numericNotation;
		this.fenNotation = fenNotation;
	}
	/**Returns the piece defined by the input parameter according to {@link #engine.Piece Piece}'s character equivalent in FEN notation.
	 * 
	 * @param piece
	 * @return
	 */
	public static char fenNotation(int piece) {
		if (piece == 0)
			return ' ';
		return Piece.values()[piece - 1].fenNotation;
	}
	/**Returns the piece defined by the FEN piece notation input parameter's numeric equivalent according to {@link #engine.Piece Piece}.
	 * 
	 * @param piece
	 * @return
	 */
	public static int numericNotation(char piece) {
		switch (piece) {
			case 'K':
				return 1;
			case 'Q':
				return 2;
			case 'R':
				return 3;
			case 'B':
				return 4;
			case 'N':
				return 5;
			case 'P':
				return 6;
			case 'k':
				return 7;
			case 'q':
				return 8;
			case 'r':
				return 9;
			case 'b':
				return 10;
			case 'n':
				return 11;
			case 'p':
				return 12;
			default:
				throw new IllegalArgumentException();
		}
	}
}
