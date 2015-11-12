package engine;

import util.*;

/**A class for evaluating chess positions. It is constructed feeding it a {@link #engine.Position Position} object reference which then can be scored as it is
 * kept incrementally updated after moves made using {@link #score score}.
 * 
 * @author Viktor
 *
 */
public class Evaluator {
	
	/**An enum type defining the standard values of different piece types.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum MaterialScore {
		
		KING	(400),
		QUEEN	(900),
		ROOK	(500),
		BISHOP	(300),
		KNIGHT	(300),
		PAWN	(100);
		
		public final int value;	// The standard worth of the piece type.
		
		private MaterialScore(int value) {
			this.value = value;
		}
		/**Returns the value score of a piece type defined by a piece index according to {@link #engine.Piece Piece}.
		 * 
		 * @param pieceInd A piece index according to {@link #engine.Piece Piece}.
		 * @return The score of the piece type.
		 */
		public static int getValueByPieceInd(int pieceInd) {
			if (pieceInd == Piece.W_KING.ind) return KING.value;
			else if (pieceInd == Piece.W_QUEEN.ind) return QUEEN.value;
			else if (pieceInd == Piece.W_ROOK.ind) return ROOK.value;
			else if (pieceInd == Piece.W_BISHOP.ind) return BISHOP.value;
			else if (pieceInd == Piece.W_KNIGHT.ind) return KNIGHT.value;
			else if (pieceInd == Piece.W_PAWN.ind) return PAWN.value;
			else if (pieceInd == Piece.B_KING.ind) return KING.value;
			else if (pieceInd == Piece.B_QUEEN.ind) return QUEEN.value;
			else if (pieceInd == Piece.B_ROOK.ind) return ROOK.value;
			else if (pieceInd == Piece.B_BISHOP.ind) return BISHOP.value;
			else if (pieceInd == Piece.B_KNIGHT.ind) return KNIGHT.value;
			else if (pieceInd == Piece.B_PAWN.ind) return PAWN.value;
			else return 0;
		}
	}
	
	/**An enumeration type for different game state scores such as check mate, stale mate, and draw due to different reasons.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum StateScore {
		
		CHECK_MATE				(Short.MIN_VALUE + 1),
		STALE_MATE				(0),
		INSUFFICIENT_MATERIAL	(0),
		DRAW_CLAIMED			(1);
		
		public final short score;
		
		private StateScore(int score) {
			this.score = (short)score;
		}
	}
	
	public static int mateScore(boolean isInCheck, int ply) {
		if (isInCheck)
		// The longer the line of play is to a check mate, the better for the side getting mated.
			return StateScore.CHECK_MATE.score + ply;
		else
			return StateScore.STALE_MATE.score;
	}
	/**Rates the chess position from the color to move's point of view. It considers material imbalance, mobility, and king safety.
	 * 
	 * @return
	 */
	public static int score(Position pos, int ply) {
		int score = 0;
		List<Move> oppMoves, moves = pos.generateAllMoves();
		if (moves.length() == 0)
			return mateScore(pos.getCheck(), ply);
		pos.makeNullMove();
		oppMoves = pos.generateAllMoves();
		pos.unmakeMove();
		score += moves.length()*10;
		score -= oppMoves.length()*10;
		score += BitOperations.getCardinality(pos.whiteQueens)*MaterialScore.QUEEN.value;
		score += BitOperations.getCardinality(pos.whiteRooks)*MaterialScore.ROOK.value;
		score += BitOperations.getCardinality(pos.whiteBishops)*MaterialScore.BISHOP.value;
		score += BitOperations.getCardinality(pos.whiteKnights)*MaterialScore.KNIGHT.value;
		score += BitOperations.getCardinality(pos.whitePawns)*MaterialScore.PAWN.value;
		score -= BitOperations.getCardinality(pos.blackQueens)*MaterialScore.QUEEN.value;
		score -= BitOperations.getCardinality(pos.blackRooks)*MaterialScore.ROOK.value;
		score -= BitOperations.getCardinality(pos.blackBishops)*MaterialScore.BISHOP.value;
		score -= BitOperations.getCardinality(pos.blackKnights)*MaterialScore.KNIGHT.value;
		score -= BitOperations.getCardinality(pos.blackPawns)*MaterialScore.PAWN.value;
		if (!pos.whitesTurn)
			score *= -1;
		return score;
	}
}
