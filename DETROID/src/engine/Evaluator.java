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
		victimVal = MaterialScore.getValueByPieceInd(move.capturedPiece);
		// If the capturer was a king, return the captured piece's value as capturing the king would be illegal.
		if (move.movedPiece == Piece.W_KING.ind || move.movedPiece == Piece.B_KING.ind)
			return victimVal;
		firstVictimVal = victimVal;
		whitesTurn = pos.whitesTurn;
		occupied &= ~(1L << move.from);
		victimVal = MaterialScore.getValueByPieceInd(move.movedPiece);
		dB = MoveTable.getByIndex(move.to);
		while (true) {
			whitesTurn = !whitesTurn;
			attackerVal = 0;
			if (whitesTurn) {
				if ((attackers = dB.getBlackPawnCaptures(pos.whitePawns) & occupied) != 0)
					attackerVal = MaterialScore.PAWN.value;
				// Re-check could be omitted as a knight can not block any other piece's attack, but the savings would be minimal.
				else if ((attackers = dB.getKnightMoves(pos.whiteKnights) & occupied) != 0)
					attackerVal = MaterialScore.KNIGHT.value;
				else if ((attackers = (bpAttack = dB.getBishopMoves(occupied, occupied)) & pos.whiteBishops) != 0)
					attackerVal = MaterialScore.BISHOP.value;
				else if ((attackers = (rkAttack = dB.getRookMoves(occupied, occupied)) & pos.whiteRooks) != 0)
					attackerVal = MaterialScore.ROOK.value;
				else if ((attackers = (bpAttack | rkAttack) & pos.whiteQueens) != 0)
					attackerVal = MaterialScore.QUEEN.value;
				else if ((attackers = dB.getKingMoves(pos.whiteKing)) != 0) {
					attackerVal = MaterialScore.KING.value;
					kingVictVal = victimVal;
				}
				else
					break;
				// If the king has attackers, the exchange is over and the king's victim's value is disregarded
				if (victimVal == MaterialScore.KING.value) {
					score -= kingVictVal;
					break;
				}
				score += victimVal;
			}
			else {
				if ((attackers = dB.getWhitePawnCaptures(pos.blackPawns) & occupied) != 0)
					attackerVal = MaterialScore.PAWN.value;
				else if ((attackers = dB.getKnightMoves(pos.blackKnights) & occupied) != 0)
					attackerVal = MaterialScore.KNIGHT.value;
				else if ((attackers = (bpAttack = dB.getBishopMoves(occupied, occupied)) & pos.blackBishops) != 0)
					attackerVal = MaterialScore.BISHOP.value;
				else if ((attackers = (rkAttack = dB.getRookMoves(occupied, occupied)) & pos.blackRooks) != 0)
					attackerVal = MaterialScore.ROOK.value;
				else if ((attackers = (bpAttack | rkAttack) & pos.blackQueens) != 0)
					attackerVal = MaterialScore.QUEEN.value;
				else if ((attackers = dB.getKingMoves(pos.blackKing)) != 0) {
					attackerVal = MaterialScore.KING.value;
					kingVictVal = victimVal;
				}
				else
					break;
				if (victimVal == MaterialScore.KING.value) {
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
