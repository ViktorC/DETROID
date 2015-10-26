package engine;

/**An enum type for the different chess pieces. Each piece has an id number by which it is represented in the array of the auxiliary offset board, a FEN notation
 * character, and a standard value.
 * 
 * @author Viktor
 *
 */
public enum Piece {
	
	NULL		('\u0000', 0),
	
	W_KING	('K', 0),
	W_QUEEN	('Q', 900),
	W_ROOK	('R', 500),
	W_BISHOP('B', 300),
	W_KNIGHT('N', 300),
	W_PAWN	('P', 100),
	
	B_KING	('k', 0),
	B_QUEEN	('q', 900),
	B_ROOK	('r', 500),
	B_BISHOP('b', 300),
	B_KNIGHT('n', 300),
	B_PAWN	('p', 100);
	
	public final int  ind;			//a number that generally represents the respective piece, among others, for example on the offset board
	public final char fen;				//a character denoting the piece-type in FEN notation
	public final int  standardValue;	//an integer representing the piece's standard evaluation score in centipawns
	
	private Piece(char fen, int standardValue) {
		this.ind = ordinal();
		this.fen = fen;
		this.standardValue = standardValue;
	}
	/**Returns the piece defined by the FEN piece notation input parameter's numeric equivalent according to {@link #engine.Piece Piece}.
	 * 
	 * @param piece
	 * @return
	 */
	public static Piece getByFenNotation(char piece) {
		switch (piece) {
			case '\u0000':
				return NULL;
			case 'K':
				return W_KING;
			case 'Q':
				return W_QUEEN;
			case 'R':
				return W_ROOK;
			case 'B':
				return W_BISHOP;
			case 'N':
				return W_KNIGHT;
			case 'P':
				return W_PAWN;
			case 'k':
				return B_KING;
			case 'q':
				return B_QUEEN;
			case 'r':
				return B_ROOK;
			case 'b':
				return B_BISHOP;
			case 'n':
				return B_KNIGHT;
			case 'p':
				return B_PAWN;
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
			case 0: return NULL;
			case 1: return W_KING; case 2: return W_QUEEN; case 3: return W_ROOK; case 4: return W_BISHOP; case 5: return W_KNIGHT; case 6: return W_PAWN;
			case 7: return B_KING; case 8: return B_QUEEN; case 9: return B_ROOK; case 10: return B_BISHOP; case 11: return B_KNIGHT; case 12: return B_PAWN;
			default: throw new IllegalArgumentException();
		}
	}
}
