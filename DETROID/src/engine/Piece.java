package engine;

/**
 * An enum type for the different chess pieces. Each piece has an id number by which it is represented in the array of the auxiliary offset board, and
 * a FEN notation character.
 * 
 * @author Viktor
 *
 */
enum Piece {
	
	NULL	(' '),
	
	W_KING	('K'),
	W_QUEEN	('Q'),
	W_ROOK	('R'),
	W_BISHOP('B'),
	W_KNIGHT('N'),
	W_PAWN	('P'),
	
	B_KING	('k'),
	B_QUEEN	('q'),
	B_ROOK	('r'),
	B_BISHOP('b'),
	B_KNIGHT('n'),
	B_PAWN	('p');
	
	final byte  ind;		// A number that generally represents the respective piece, among others, for example on the offset board.
	final char letter;	// A character denoting the piece-type in chess notation.
	
	private Piece(char fen) {
		this.ind = (byte) ordinal();
		this.letter = fen;
	}
	/**
	 * Returns the piece defined by the FEN piece letter notation input parameter.
	 * 
	 * @param piece
	 * @return
	 * @throws ChessParseException 
	 */
	static Piece parse(char piece) throws ChessParseException {
		switch (piece) {
			case '\u0000':
				return Piece.NULL;
			case 'K':
				return Piece.W_KING;
			case 'Q':
				return Piece.W_QUEEN;
			case 'R':
				return Piece.W_ROOK;
			case 'B':
				return Piece.W_BISHOP;
			case 'N':
				return Piece.W_KNIGHT;
			case 'P':
				return Piece.W_PAWN;
			case 'k':
				return Piece.B_KING;
			case 'q':
				return Piece.B_QUEEN;
			case 'r':
				return Piece.B_ROOK;
			case 'b':
				return Piece.B_BISHOP;
			case 'n':
				return Piece.B_KNIGHT;
			case 'p':
				return Piece.B_PAWN;
			default:
				throw new ChessParseException();
		}
	}
	/**
	 * Returns an enum instance of the piece denoted by the input parameter numericNotation.
	 * 
	 * @param numericNotation
	 * @return
	 */
	static Piece getByNumericNotation(int numericNotation) {
		switch (numericNotation) {
			case 0: return NULL;
			case 1: return W_KING; case 2: return W_QUEEN; case 3: return W_ROOK; case 4: return W_BISHOP; case 5: return W_KNIGHT;
			case 6: return W_PAWN; case 7: return B_KING; case 8: return B_QUEEN; case 9: return B_ROOK; case 10: return B_BISHOP;
			case 11: return B_KNIGHT; case 12: return B_PAWN; default: throw new IllegalArgumentException();
		}
	}
}
