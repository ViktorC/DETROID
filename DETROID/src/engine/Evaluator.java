package engine;

import util.*;

public class Evaluator {
	
	public int score(Position pos) {
		int score = 0;
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
		return score; 
	}
}
