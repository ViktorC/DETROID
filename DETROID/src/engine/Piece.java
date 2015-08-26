package engine;

/**An enum type for the different chess pieces. Each piece has an initial position bitmap for the bitboards, an id number by which it is represented in the
 * array of the auxiliary offset board, a FEN notation character, and a standard value.
 * 
 * @author Viktor
 *
 */
public enum Piece {
	
	NULL			(0b0000000000000000111111111111111111111111111111110000000000000000L, 0,  '\u0000', 0),
	
	WHITE_KING		(0b0000000000000000000000000000000000000000000000000000000000010000L, 1,  'K', 0),
	WHITE_QUEEN		(0b0000000000000000000000000000000000000000000000000000000000001000L, 2,  'Q', 900),
	WHITE_ROOK		(0b0000000000000000000000000000000000000000000000000000000010000001L, 3,  'R', 500),
	WHITE_BISHOP	(0b0000000000000000000000000000000000000000000000000000000000100100L, 4,  'B', 300),
	WHITE_KNIGHT 	(0b0000000000000000000000000000000000000000000000000000000001000010L, 5,  'N', 300),
	WHITE_PAWN		(0b0000000000000000000000000000000000000000000000001111111100000000L, 6,  'P', 100),
	
	BLACK_KING		(0b0001000000000000000000000000000000000000000000000000000000000000L, 7,  'k', 0),
	BLACK_QUEEN		(0b0000100000000000000000000000000000000000000000000000000000000000L, 8,  'q', 900),
	BLACK_ROOK		(0b1000000100000000000000000000000000000000000000000000000000000000L, 9,  'r', 500),
	BLACK_BISHOP	(0b0010010000000000000000000000000000000000000000000000000000000000L, 10, 'b', 300),
	BLACK_KNIGHT	(0b0100001000000000000000000000000000000000000000000000000000000000L, 11, 'n', 300),
	BLACK_PAWN		(0b0000000011111111000000000000000000000000000000000000000000000000L, 12, 'p', 100);
	
	public final long initPosBitmap;		//a bitmap representing the initial position of the respective pieces at the start of the game
	public final int  numericNotation;		//a number between 1 and 12 that generally represents the respective piece, among others, for example on the offset board
	public final char fenNotation;			//a character denoting the piece-type in FEN notation
	public final int  standardValue;		//an integer representing the piece's standard evaluation score in centipawns
	
	private Piece(long initPosBitmap, int numericNotation, char fenNotation, int standardValue) {
		this.initPosBitmap = initPosBitmap;
		this.numericNotation = numericNotation;
		this.fenNotation = fenNotation;
		this.standardValue = standardValue;
	}
	/**Returns the piece defined by the input parameter according to {@link #engine.Piece Piece}'s character equivalent in FEN notation.
	 * 
	 * @param piece
	 * @return
	 */
	public static char fenNotation(int piece) {
		return getByNumericNotation(piece).fenNotation;
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
	/**Return an enum instance of the piece denoted by the input parameter numericNotation.
	 * 
	 * @param numericNotation
	 * @return
	 */
	public static Piece getByNumericNotation(int numericNotation) {
		switch (numericNotation) {
			case 0:
				return NULL;
			case 1:
				return WHITE_KING;
			case 2:
				return WHITE_QUEEN;
			case 3:
				return WHITE_ROOK;
			case 4:
				return WHITE_BISHOP;
			case 5:
				return WHITE_KNIGHT;
			case 6:
				return WHITE_PAWN;
			case 7:
				return BLACK_KING;
			case 8:
				return BLACK_QUEEN;
			case 9:
				return BLACK_ROOK;
			case 10:
				return BLACK_BISHOP;
			case 11:
				return BLACK_KNIGHT;
			case 12:
				return BLACK_PAWN;
			default:
				throw new IllegalArgumentException();
		}
	}
}
