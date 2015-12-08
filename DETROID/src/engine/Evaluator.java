package engine;

import engine.Move.MoveType;
import engine.Board.Diagonal;
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
		PAWN	(100, 0),
		NULL	(0, 0);
		
		public final short score;		// The standard worth of the piece type.
		public final short phaseWeight;	// A measure of the impact a certain material type has on the phase evaluation.
		
		private Material(int score, int phaseWeight) {
			this.score = (short)score;
			this.phaseWeight = (byte)phaseWeight;
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
			else return NULL;
		}
	}
	
	/**An enumeration type for different game state scores such as check mate, stale mate, and draw due to different reasons.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum Termination {
		
		CHECK_MATE				(Short.MIN_VALUE + 1),
		STALE_MATE				(0),
		INSUFFICIENT_MATERIAL	(0),
		DRAW_CLAIMED			(1);
		
		public final short score;
		
		private Termination(int score) {
			this.score = (short)score;
		}
	}
	
	/**An enumration type for game phases such as opening, middle game, and end game so searches can be conducted accordingly.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum GamePhase {
		
		OPENING 	(0, 22),
		MIDDLE_GAME (23, 170),
		END_GAME	(171, 256);	// Very early end game.
		
		public final short lowerBound;
		public final short upperBound;
		
		private GamePhase(int lowerBound, int upperBound) {
			this.lowerBound = (short)lowerBound;
			this.upperBound = (short)upperBound;
		}
		/**Returns the phase associated with the given phase score.
		 * 
		 * @param phaseScore
		 * @return
		 */
		public static GamePhase getByPhaseScore(int phaseScore) {
			if (phaseScore < MIDDLE_GAME.lowerBound)
				return OPENING;
			else if (phaseScore >= END_GAME.lowerBound)
				return END_GAME;
			else
				return MIDDLE_GAME;
		}
	}
	
	private static HashTable<PTEntry> pT = new HashTable<>();	// Pawn table.
	
	private static int TOTAL_PHASE_WEIGHTS = 4*(Material.KNIGHT.phaseWeight + Material.BISHOP.phaseWeight +
												Material.ROOK.phaseWeight) + 2*Material.QUEEN.phaseWeight;
	
	/**Returns a phaseScore between 0 and 256.
	 * 
	 * @param numOfQueens
	 * @param numOfRooks
	 * @param numOfBishops
	 * @param numOfKnights
	 * @return
	 */
	private static int phaseScore(int numOfQueens, int numOfRooks, int numOfBishops, int numOfKnights) {
		int phase = TOTAL_PHASE_WEIGHTS - (numOfQueens*Material.QUEEN.phaseWeight + numOfRooks*Material.ROOK.phaseWeight
					+ numOfBishops*Material.BISHOP.phaseWeight + numOfKnights*Material.KNIGHT.phaseWeight);
		return (phase*256 + TOTAL_PHASE_WEIGHTS/2)/TOTAL_PHASE_WEIGHTS;
	}
	/**Returns an estimation of the phase in which the current game is based on the given position.
	 * 
	 * @param pos
	 * @return
	 */
	public static GamePhase evaluateGamePhase(Position pos) {
		int numOfQueens, numOfRooks, numOfBishops, numOfKnights;
		numOfQueens = BitOperations.getCardinality(pos.whiteQueens | pos.blackQueens)*Material.QUEEN.phaseWeight;
		numOfRooks = BitOperations.getCardinality(pos.whiteRooks | pos.blackRooks)*Material.ROOK.phaseWeight;
		numOfBishops = BitOperations.getCardinality(pos.whiteBishops | pos.blackBishops)*Material.BISHOP.phaseWeight;
		numOfKnights = BitOperations.getCardinality(pos.whiteKnights | pos.blackKnights)*Material.KNIGHT.phaseWeight;
		return GamePhase.getByPhaseScore(phaseScore(numOfQueens, numOfRooks, numOfBishops, numOfKnights));
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
		boolean whitesTurn, noRetaliation = true;
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
			noRetaliation = false;
			victimVal = attackerVal;
			bpAttack = 0;
			rkAttack = 0;
			// Simulate move.
			occupied &= ~BitOperations.getLSBit(attackers);
		}
		if (pos.whitesTurn) {
			score += firstVictimVal;
		}
		else {
			score -= firstVictimVal;
			score *= -1;
		}
		if (noRetaliation && move.type >= MoveType.PROMOTION_TO_QUEEN.ind) {
			if (move.type == MoveType.PROMOTION_TO_QUEEN.ind)
				score += Material.QUEEN.score - Material.PAWN.score;
			else if (move.type == MoveType.PROMOTION_TO_ROOK.ind)
				score += Material.ROOK.score - Material.PAWN.score;
			else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind)
				score += Material.BISHOP.score - Material.PAWN.score;
			else
				score += Material.KNIGHT.score - Material.PAWN.score;
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
			return Termination.CHECK_MATE.score + ply;
		else
			return Termination.STALE_MATE.score;
	}
	/**Returns an evaluation score according to the current phase and the evaluation scores of the same position in the context of an opening and
	 * in the context of and end game to establish continuity.
	 * 
	 * @param openingEval
	 * @param endGameEval
	 * @param phaseScore
	 * @return
	 */
	private static int taperedEvalScore(int openingEval, int endGameEval, int phaseScore) {
		return (openingEval*(256 - phaseScore) + endGameEval*phaseScore)/256;
	}
	/**Rates the chess position from the color to move's point of view. It considers material imbalance, mobility, and king safety.
	 * 
	 * @param pos The position to evaluate.
	 * @param allMoves A list of all the legal moves for the side to move.
	 * @param ply The current level of distance from the root in the search.
	 * @return
	 */
	public static int score(Position pos, List<Move> allMoves, int ply) {
		int numOfWhiteQueens, numOfBlackQueens, numOfWhiteRooks, numOfBlackRooks, numOfWhiteBishops, numOfBlackBishops, numOfWhiteKnights,
			numOfBlackKnights, numOfAllPieces, phase, baseScore = 0, pawnScore = 0, openingScore = 0, endGameScore = 0,
			bishopColor, bishopField, newBishopColor;
		int[] bishopSqrArr;
		PTEntry e;
		if (allMoves.length() == 0)
			return mateScore(pos.getCheck(), ply);
		numOfWhiteQueens = BitOperations.getCardinality(pos.whiteQueens);
		numOfWhiteRooks = BitOperations.getCardinality(pos.whiteRooks);
		numOfWhiteBishops = BitOperations.getCardinality(pos.whiteBishops);
		numOfWhiteKnights = BitOperations.getCardinality(pos.whiteKnights);
		numOfBlackQueens = BitOperations.getCardinality(pos.blackQueens);
		numOfBlackRooks = BitOperations.getCardinality(pos.blackRooks);
		numOfBlackBishops = BitOperations.getCardinality(pos.blackBishops);
		numOfBlackKnights = BitOperations.getCardinality(pos.blackKnights);
		phase = phaseScore(numOfWhiteQueens + numOfBlackQueens, numOfWhiteRooks + numOfBlackRooks, numOfWhiteBishops + numOfBlackBishops,
				numOfWhiteKnights + numOfBlackKnights);
		// Check for insufficient material. Only consider the widely acknowledged scenarios without blocked position testing.
		if (phase >= 234 && numOfWhiteQueens == 0 && numOfBlackQueens == 0 &&
		numOfWhiteRooks == 0 && numOfBlackRooks == 0 && pos.whitePawns == 0 && pos.blackPawns == 0) {
			numOfAllPieces = BitOperations.getCardinality(pos.allOccupied);
			if (numOfAllPieces == 2 ||
			(numOfAllPieces == 3 && (numOfWhiteBishops == 1 || numOfBlackBishops == 1 ||
									numOfWhiteKnights == 1 || numOfBlackKnights == 1)) ||
			(numOfAllPieces == 4 && numOfWhiteBishops == 1 && numOfBlackBishops == 1 &&
			Diagonal.getBySquareIndex(BitOperations.indexOfBit(pos.whiteBishops)).ordinal()%2 ==
			Diagonal.getBySquareIndex(BitOperations.indexOfBit(pos.blackBishops)).ordinal()%2))
				return Termination.INSUFFICIENT_MATERIAL.score;
			if (numOfWhiteKnights == 0 && numOfBlackKnights == 0) {
				bishopSqrArr = BitOperations.serialize(pos.whiteBishops | pos.blackBishops, numOfWhiteBishops + numOfBlackBishops);
				bishopColor = Diagonal.getBySquareIndex(bishopSqrArr[0]).ordinal()%2;
				for (int i = 1; i < bishopSqrArr.length; i++) {
					bishopField = bishopSqrArr[i];
					if ((newBishopColor = Diagonal.getBySquareIndex(bishopField).ordinal()%2) != bishopColor)
						return Termination.INSUFFICIENT_MATERIAL.score;
					bishopColor = newBishopColor;
				}
			}
		}
		/* Try for hashed pawn score.
		e = pT.lookUp(pos.pawnKey);
		if (e != null) {
			pawnScore = e.score;
		}
		// Evaluate pawn structure.
		// Definitely needs to be thorough and sophisticated to make up for the costs of pawn hashing.
		else {
			pawnScore = (BitOperations.getCardinality(pos.whitePawns) - BitOperations.getCardinality(pos.blackPawns))*Material.PAWN.score;
			pT.insert(new PTEntry(pos.pawnKey, true, pawnScore));
		} */
		baseScore += (numOfWhiteQueens - numOfBlackQueens)*Material.QUEEN.score;
		baseScore += (numOfWhiteRooks - numOfBlackRooks)*Material.ROOK.score;
		baseScore += (numOfWhiteBishops - numOfBlackBishops)*Material.BISHOP.score;
		baseScore += (numOfWhiteKnights - numOfBlackKnights)*Material.KNIGHT.score;
		baseScore += pawnScore;
		if (!pos.whitesTurn)
			baseScore *= -1;
		// Need to implement separate evaluation features for openings and end games.
		// Pairs of piece square tables are imperative.
		openingScore += baseScore;
		endGameScore += baseScore;
		return taperedEvalScore(openingScore, endGameScore, phase);
	}
}
