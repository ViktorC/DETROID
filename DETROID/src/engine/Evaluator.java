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
	public static enum Material {
		
		KING	(400, 0),
		QUEEN	(900, 4),
		ROOK	(500, 2),
		BISHOP	(300, 1),
		KNIGHT	(300, 1),
		PAWN	(100, 0);
		
		public final int score;			// The standard worth of the piece type.
		public final int phaseValue;	// A measure of the impact a certain material type has on the phase evaluation.
		
		private Material(int score, int phaseValue) {
			this.score = score;
			this.phaseValue = phaseValue;
		}
		/**Returns the enum for a piece type defined by a piece index according to {@link #engine.Piece Piece}.
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
			else return null;
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
	
	public static enum GamePhase {
		
		OPENING 	(0, 22),
		MIDDLE_GAME (23, 192),
		ENDING		(193, 256);
		
		public final short lowerBound;
		public final short upperBound;
		
		private GamePhase(int lowerBound, int upperBound) {
			this.lowerBound = (short)lowerBound;
			this.upperBound = (short)upperBound;
		}
	}
	
	private static int TOTAL_PHASE_VALUE; {
		TOTAL_PHASE_VALUE = 4*(Material.KNIGHT.phaseValue + Material.BISHOP.phaseValue + Material.ROOK.phaseValue) + 2*Material.QUEEN.phaseValue;
	}
	
	/**Returns a score that can be used to estimate the phase in which the current game is based on the given position.
	 * 
	 * @param pos
	 * @return
	 */
	public static int evaluateGamePhase(Position pos) {
		int score = 0;
		score += BitOperations.getCardinality(pos.whiteQueens)*Material.QUEEN.phaseValue;
		score += BitOperations.getCardinality(pos.whiteRooks)*Material.ROOK.phaseValue;
		score += BitOperations.getCardinality(pos.whiteBishops)*Material.BISHOP.phaseValue;
		score += BitOperations.getCardinality(pos.whiteKnights)*Material.KNIGHT.phaseValue;
		score += BitOperations.getCardinality(pos.blackQueens)*Material.QUEEN.phaseValue;
		score += BitOperations.getCardinality(pos.blackRooks)*Material.ROOK.phaseValue;
		score += BitOperations.getCardinality(pos.blackBishops)*Material.BISHOP.phaseValue;
		score += BitOperations.getCardinality(pos.blackKnights)*Material.KNIGHT.phaseValue;
		return score;
	}
	/**A static exchange evaluation algorithm for determining a close approximation of a capture's value. It is mainly used for move ordering
	 * in the quiescence search.
	 * 
	 * @param pos
	 * @param move
	 * @return
	 */
	public static int SEE(Position pos, Move move) {
		int score = 0, victimVal, firstVictimVal, attackerVal, kingVictVal = 0;
		long attackers, bpAttack, rkAttack, occupied = pos.allOccupied;
		boolean whitesTurn;
		MoveTable dB;
		victimVal = Material.getByPieceInd(move.capturedPiece).score;
		// If the capturer was a king, return the captured piece's value as capturing the king would be illegal.
		if (move.movedPiece == Piece.W_KING.ind || move.movedPiece == Piece.B_KING.ind)
			return victimVal;
		firstVictimVal = victimVal;
		whitesTurn = pos.whitesTurn;
		occupied &= ~(1L << move.from);
		victimVal = Material.getByPieceInd(move.movedPiece).score;
		dB = MoveTable.getByIndex(move.to);
		while (true) {
			whitesTurn = !whitesTurn;
			attackerVal = 0;
			if (whitesTurn) {
				if ((attackers = dB.getBlackPawnCaptures(pos.whitePawns) & occupied) != 0)
					attackerVal = Material.PAWN.score;
				// Re-check could be omitted as a knight can not block any other piece's attack, but the savings would be minimal.
				else if ((attackers = dB.getKnightMoves(pos.whiteKnights) & occupied) != 0)
					attackerVal = Material.KNIGHT.score;
				else if ((attackers = (bpAttack = dB.getBishopMoves(occupied, occupied)) & pos.whiteBishops) != 0)
					attackerVal = Material.BISHOP.score;
				else if ((attackers = (rkAttack = dB.getRookMoves(occupied, occupied)) & pos.whiteRooks) != 0)
					attackerVal = Material.ROOK.score;
				else if ((attackers = (bpAttack | rkAttack) & pos.whiteQueens) != 0)
					attackerVal = Material.QUEEN.score;
				else if ((attackers = dB.getKingMoves(pos.whiteKing)) != 0) {
					attackerVal = Material.KING.score;
					kingVictVal = victimVal;
				}
				else
					break;
				// If the king has attackers, the exchange is over and the king's victim's value is disregarded
				if (victimVal == Material.KING.score) {
					score -= kingVictVal;
					break;
				}
				score += victimVal;
			}
			else {
				if ((attackers = dB.getWhitePawnCaptures(pos.blackPawns) & occupied) != 0)
					attackerVal = Material.PAWN.score;
				else if ((attackers = dB.getKnightMoves(pos.blackKnights) & occupied) != 0)
					attackerVal = Material.KNIGHT.score;
				else if ((attackers = (bpAttack = dB.getBishopMoves(occupied, occupied)) & pos.blackBishops) != 0)
					attackerVal = Material.BISHOP.score;
				else if ((attackers = (rkAttack = dB.getRookMoves(occupied, occupied)) & pos.blackRooks) != 0)
					attackerVal = Material.ROOK.score;
				else if ((attackers = (bpAttack | rkAttack) & pos.blackQueens) != 0)
					attackerVal = Material.QUEEN.score;
				else if ((attackers = dB.getKingMoves(pos.blackKing)) != 0) {
					attackerVal = Material.KING.score;
					kingVictVal = victimVal;
				}
				else
					break;
				if (victimVal == Material.KING.score) {
					score += kingVictVal;
					break;
				}
				score -= victimVal;
			}
			victimVal = attackerVal;
			bpAttack = 0;
			rkAttack = 0;
			// Simulate move.
			occupied &= ~BitOperations.getLSBit(attackers);
		}
		if (pos.whitesTurn)
			score += firstVictimVal;
		else {
			score -= firstVictimVal;
			score *= -1;
		}
		return score;
	}
	/**Returns the right score for when there are no more legal move in a position.
	 * 
	 * @param sideToMoveInCheck
	 * @param ply
	 * @return
	 */
	public static int mateScore(boolean sideToMoveInCheck, int ply) {
		if (sideToMoveInCheck)
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
		score += BitOperations.getCardinality(pos.whiteQueens)*Material.QUEEN.score;
		score += BitOperations.getCardinality(pos.whiteRooks)*Material.ROOK.score;
		score += BitOperations.getCardinality(pos.whiteBishops)*Material.BISHOP.score;
		score += BitOperations.getCardinality(pos.whiteKnights)*Material.KNIGHT.score;
		score += BitOperations.getCardinality(pos.whitePawns)*Material.PAWN.score;
		score -= BitOperations.getCardinality(pos.blackQueens)*Material.QUEEN.score;
		score -= BitOperations.getCardinality(pos.blackRooks)*Material.ROOK.score;
		score -= BitOperations.getCardinality(pos.blackBishops)*Material.BISHOP.score;
		score -= BitOperations.getCardinality(pos.blackKnights)*Material.KNIGHT.score;
		score -= BitOperations.getCardinality(pos.blackPawns)*Material.PAWN.score;
		if (!pos.whitesTurn)
			score *= -1;
		return score;
	}
}
