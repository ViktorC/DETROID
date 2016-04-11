package engine;

/**
 * An enum type for the different chess pieces. Each piece has an id number by which it is represented in the array of the auxiliary offset board, and
 * a FEN notation character.
 * 
 * @author Viktor
 *
 */
public enum Piece {
	
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
	
	public final byte  ind;		// A number that generally represents the respective piece, among others, for example on the offset board.
	public final char letter;	// A character denoting the piece-type in chess notation.
	
	private Piece(char fen) {
		this.ind = (byte)ordinal();
		this.letter = fen;
	}
	/**
	 * Returns an enum instance of the piece denoted by the input parameter numericNotation.
	 * 
	 * @param numericNotation
	 * @return
	 */
	public static Piece getByNumericNotation(int numericNotation) {
		switch (numericNotation) {
			case 0: return NULL;
			case 1: return W_KING; case 2: return W_QUEEN; case 3: return W_ROOK; case 4: return W_BISHOP; case 5: return W_KNIGHT;
			case 6: return W_PAWN; case 7: return B_KING; case 8: return B_QUEEN; case 9: return B_ROOK; case 10: return B_BISHOP;
			case 11: return B_KNIGHT; case 12: return B_PAWN; default: throw new IllegalArgumentException();
		}
	}
}
