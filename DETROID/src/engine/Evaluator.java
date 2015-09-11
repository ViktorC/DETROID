package engine;

import util.*;
import engine.Game.State;

/**A class for evaluating chess positions. It is constructed feeding it a {@link #engine.Position Position} object reference which then can be scored as it is
 * kept incrementally updated after moves made using {@link #score score}.
 * 
 * @author Viktor
 *
 */
public class Evaluator {
	
	/**Rates the chess position from the color to move's point of view. It considers material imbalance, mobility, and king safety.
	 * 
	 * @return
	 */
	public static int score(Position pos) {
		int score = 0;
		Move move;
		IntList gloriaSquares;
		List<Move> moves = pos.generateMoves();
		if (moves.length() == 0) {
			if (pos.getCheck())
				return State.LOSS.score;
			else
				return State.TIE.score;
		}
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
		gloriaSquares = BitOperations.serialize(MoveDatabase.getByIndex(BitOperations.indexOfBit(pos.whiteKing)).getCrudeKingMoves());
		while (gloriaSquares.hasNext())
			score -= BitOperations.getCardinality(pos.getAttackers(gloriaSquares.next(), false))*5;
		while (gloriaSquares.hasNext())
			score += BitOperations.getCardinality(pos.getAttackers(gloriaSquares.next(), true))*5;
		gloriaSquares = BitOperations.serialize(MoveDatabase.getByIndex(BitOperations.indexOfBit(pos.blackKing)).getCrudeKingMoves());
		while (gloriaSquares.hasNext())
			score += BitOperations.getCardinality(pos.getAttackers(gloriaSquares.next(), true))*5;
		while (gloriaSquares.hasNext())
			score -= BitOperations.getCardinality(pos.getAttackers(gloriaSquares.next(), false))*5;
		if (pos.whiteCastlingRights != 0)
			score += 25;
		if (pos.blackCastlingRights != 0)
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
				score += Piece.getByNumericNotation(move.movedPiece).standardValue/100;
			score += Piece.getByNumericNotation(move.capturedPiece).standardValue/20;
			if (move.type > 3)
				score += 40;
		}
		pos.whitesTurn = !pos.whitesTurn;
		moves = pos.generateMoves();
		while (moves.hasNext()) {
			move = moves.next();
			if (move.movedPiece == 1 || move.movedPiece == 7)
				score -= 10;
			else
				score -= Piece.getByNumericNotation(move.movedPiece).standardValue/100;
			score -= Piece.getByNumericNotation(move.capturedPiece).standardValue/20;
			if (move.type > 3)
				score -= 40;
		}
		pos.whitesTurn = !pos.whitesTurn;
		return score;
	}
}
