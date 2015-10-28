package engine;

import util.*;
import engine.Game.State;
import engine.Position.CastlingRights;

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
		
		public final int value;	//the standard worth of the piece type
		
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
	/**Rates the chess position from the color to move's point of view. It considers material imbalance, mobility, and king safety.
	 * 
	 * @return
	 */
	public static int score(Position pos) {
		int score = 0;
		Move move;
		IntList gloriaSquares;
		List<Move> moves = pos.generateAllMoves();
		if (moves.length() == 0) {
			if (pos.getCheck())
				return State.LOSS.score;
			else
				return State.TIE.score;
		}
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
		gloriaSquares = BitOperations.serialize(MoveTable.getByIndex(BitOperations.indexOfBit(pos.whiteKing)).getCrudeKingMoves());
		while (gloriaSquares.hasNext())
			score -= BitOperations.getCardinality(pos.getAttackers(gloriaSquares.next(), false))*5;
		while (gloriaSquares.hasNext())
			score += BitOperations.getCardinality(pos.getAttackers(gloriaSquares.next(), true))*5;
		gloriaSquares = BitOperations.serialize(MoveTable.getByIndex(BitOperations.indexOfBit(pos.blackKing)).getCrudeKingMoves());
		while (gloriaSquares.hasNext())
			score += BitOperations.getCardinality(pos.getAttackers(gloriaSquares.next(), true))*5;
		while (gloriaSquares.hasNext())
			score -= BitOperations.getCardinality(pos.getAttackers(gloriaSquares.next(), false))*5;
		if (pos.whiteCastlingRights != CastlingRights.NONE.ind)
			score += 25;
		if (pos.blackCastlingRights != CastlingRights.NONE.ind)
			score -= 25;
		if (!pos.whitesTurn)
			score *= -1;
		if (pos.getLastMove().type == 1 || pos.getLastMove().type == 2)
			score += 50;
		while (moves.hasNext()) {
			move = moves.next();
			if (move.movedPiece == 1 || move.movedPiece == 7)
				score += 10;
			else
				score += MaterialScore.getValueByPieceInd(move.movedPiece)/100;
			score += MaterialScore.getValueByPieceInd(move.capturedPiece)/20;
			if (move.type > 3)
				score += 40;
		}
		pos.whitesTurn = !pos.whitesTurn;
		moves = pos.generateAllMoves();
		while (moves.hasNext()) {
			move = moves.next();
			if (move.movedPiece == 1 || move.movedPiece == 7)
				score -= 10;
			else
				score += MaterialScore.getValueByPieceInd(move.movedPiece)/100;
			score += MaterialScore.getValueByPieceInd(move.capturedPiece)/20;
			if (move.type > 3)
				score -= 40;
		}
		pos.whitesTurn = !pos.whitesTurn;
		return score;
	}
}
