package engine;

import util.*;
import engine.Board.*;

/**PRE-MATURE; FOR TESTING ONLY
 * 
 * @author Viktor
 *
 */
public class Evaluator {
	
	final static short WIN  = Short.MAX_VALUE;
	final static short TIE  = 0;
	final static short LOSS = Short.MIN_VALUE + 1;
	
	private Position pos;
	
	public Evaluator(Position pos) {
		this.pos = pos;
	}
	public int score() {
		int score = 0;
		Move move;
		List<Move> moves = pos.generateMoves();
		if (moves.length() == 0)
			return LOSS;
		score += BitOperations.getCardinality(pos.whiteQueens)*Piece.WHITE_QUEEN.standardValue;
		score += BitOperations.getCardinality(pos.whiteRooks)*Piece.WHITE_ROOK.standardValue;
		score += BitOperations.getCardinality(pos.whiteBishops)*Piece.WHITE_BISHOP.standardValue;
		score += BitOperations.getCardinality(pos.whiteKnights)*Piece.WHITE_KNIGHT.standardValue;
		score += BitOperations.getCardinality(pos.whitePawns)*Piece.WHITE_PAWN.standardValue;
		score -= BitOperations.getCardinality(pos.blackQueens)*Piece.BLACK_QUEEN.standardValue;
		score -= BitOperations.getCardinality(pos.blackRooks)*Piece.BLACK_ROOK.standardValue;
		score -= BitOperations.getCardinality(pos.blackBishops)*Piece.BLACK_BISHOP.standardValue;
		score -= BitOperations.getCardinality(pos.blackKnights)*Piece.BLACK_KNIGHT.standardValue;
		score -= BitOperations.getCardinality(pos.blackPawns)*Piece.BLACK_PAWN.standardValue;
		if (!pos.whitesTurn)
			score *= -1;
		while (moves.hasNext()) {
			move = moves.next();
			if (move.movedPiece == 1 || move.movedPiece == 7)
				score += 27;
			else
				score += Piece.getByNumericNotation(move.movedPiece).standardValue/100;
			score += Piece.getByNumericNotation(move.capturedPiece).standardValue/20;
		}
		pos.whitesTurn = !pos.whitesTurn;
		moves = pos.generateMoves();
		while (moves.hasNext()) {
			move = moves.next();
			if (move.movedPiece == 1 || move.movedPiece == 7)
				score -= 27;
			else
				score -= Piece.getByNumericNotation(move.movedPiece).standardValue/100;
			score -= Piece.getByNumericNotation(move.capturedPiece).standardValue/20;
		}
		pos.whitesTurn = !pos.whitesTurn;
		return score;
	}
}
