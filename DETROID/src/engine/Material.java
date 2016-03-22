package engine;

/**
 * An enum type defining the standard values of different piece types.
 * 
 * @author Viktor
 *
 */
public enum Material {
	
	KING	(20000, 0),
	QUEEN	(900, 4),
	ROOK	(500, 2),
	BISHOP	(330, 1),
	KNIGHT	(320, 1),
	PAWN	(100, 0),
	NULL	(0, 0);
	
	public final short score;		// The standard worth of the piece type.
	public final short phaseWeight;	// A measure of the impact a certain material type has on the phase evaluation.
	
	private Material(int score, int phaseWeight) {
		this.score = (short)score;
		this.phaseWeight = (byte)phaseWeight;
	}
	/**
	 * Returns the enum for a piece type defined by a piece index according to {@link #engine.Piece Piece}.
	 * 
	 * @param pieceInd A piece index according to {@link #engine.Piece Piece}.
	 * @return The enumeration of the piece type.
	 */
	public static Material getByPieceInd(int pieceInd) {
		if (pieceInd == Piece.W_KING.ind) return KING;
		else if (pieceInd == Piece.W_QUEEN.ind) return QUEEN;
		else if (pieceInd == Piece.W_ROOK.ind) return ROOK;
		else if (pieceInd == Piece.W_BISHOP.ind) return BISHOP;
		else if (pieceInd == Piece.W_KNIGHT.ind) return KNIGHT;
		else if (pieceInd == Piece.W_PAWN.ind) return PAWN;
		else if (pieceInd == Piece.B_KING.ind) return KING;
		else if (pieceInd == Piece.B_QUEEN.ind) return QUEEN;
		else if (pieceInd == Piece.B_ROOK.ind) return ROOK;
		else if (pieceInd == Piece.B_BISHOP.ind) return BISHOP;
		else if (pieceInd == Piece.B_KNIGHT.ind) return KNIGHT;
		else if (pieceInd == Piece.B_PAWN.ind) return PAWN;
		else return NULL;
	}
}