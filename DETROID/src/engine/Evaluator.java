package engine;

import engine.Board.Diagonal;
import engine.Move.MoveType;
import util.*;

/**
 * A class for evaluating chess positions. It is constructed feeding it a {@link #engine.Position Position} object reference which then can be scored as it is
 * kept incrementally updated after moves made using {@link #score score}.
 * 
 * @author Viktor
 *
 */
public final class Evaluator {
	
	/**
	 * An enumeration type for different game state scores such as check mate, stale mate, and draw due to different reasons.
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
	
	private HashTable<ETEntry> eT;	// Evaluation score hash table.
	private HashTable<PTEntry> pT;	// Pawn hash table.
	
	private byte eGen;	// Entry generation.
	
	private static int TOTAL_PHASE_WEIGHTS = 4*(Material.KNIGHT.phaseWeight + Material.BISHOP.phaseWeight +
												Material.ROOK.phaseWeight) + 2*Material.QUEEN.phaseWeight;
	
	public Evaluator(HashTable<ETEntry> evalTable, HashTable<PTEntry> pawnTable, byte entryGeneration) {
		eT = evalTable;
		pT = pawnTable;
		eGen = entryGeneration;
	}
	/**
	 * Returns a phaseScore between 0 and 256.
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
	/**
	 * Returns an estimation of the phase in which the current game is based on the given position.
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
	/**
	 * A static exchange evaluation algorithm for determining a close approximation of a capture's value. It is mainly used for move ordering
	 * in the quiescence search.
	 * 
	 * @param pos
	 * @param move
	 * @return
	 */
	public static short SEE(Position pos, Move move) {
		short score = 0, victimVal, firstVictimVal, attackerVal, kingVictVal = 0;
		long attackers, bpAttack, rkAttack, occupied = pos.allOccupied;
		boolean whitesTurn, noRetaliation = true;
		MoveSetDatabase dB;
		victimVal = Material.getByPieceInd(move.capturedPiece).score;
		// If the capturer was a king, return the captured piece's value as capturing the king would be illegal.
		if (move.movedPiece == Piece.W_KING.ind || move.movedPiece == Piece.B_KING.ind)
			return victimVal;
		firstVictimVal = victimVal;
		whitesTurn = pos.isWhitesTurn;
		occupied &= ~(1L << move.from);
		victimVal = Material.getByPieceInd(move.movedPiece).score;
		dB = MoveSetDatabase.getByIndex(move.to);
		while (true) {
			whitesTurn = !whitesTurn;
			attackerVal = 0;
			if (whitesTurn) {
				if ((attackers = dB.getBlackPawnCaptureSet(pos.whitePawns) & occupied) != 0)
					attackerVal = Material.PAWN.score;
				// Re-check could be omitted as a knight can not block any other piece's attack, but the savings would be minimal.
				else if ((attackers = dB.getKnightMoveSet(pos.whiteKnights) & occupied) != 0)
					attackerVal = Material.KNIGHT.score;
				else if ((attackers = (bpAttack = dB.getBishopMoveSet(occupied, occupied)) & pos.whiteBishops) != 0)
					attackerVal = Material.BISHOP.score;
				else if ((attackers = (rkAttack = dB.getRookMoveSet(occupied, occupied)) & pos.whiteRooks) != 0)
					attackerVal = Material.ROOK.score;
				else if ((attackers = (bpAttack | rkAttack) & pos.whiteQueens) != 0)
					attackerVal = Material.QUEEN.score;
				else if ((attackers = dB.getKingMoveSet(pos.whiteKing)) != 0) {
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
				if ((attackers = dB.getWhitePawnCaptureSet(pos.blackPawns) & occupied) != 0)
					attackerVal = Material.PAWN.score;
				else if ((attackers = dB.getKnightMoveSet(pos.blackKnights) & occupied) != 0)
					attackerVal = Material.KNIGHT.score;
				else if ((attackers = (bpAttack = dB.getBishopMoveSet(occupied, occupied)) & pos.blackBishops) != 0)
					attackerVal = Material.BISHOP.score;
				else if ((attackers = (rkAttack = dB.getRookMoveSet(occupied, occupied)) & pos.blackRooks) != 0)
					attackerVal = Material.ROOK.score;
				else if ((attackers = (bpAttack | rkAttack) & pos.blackQueens) != 0)
					attackerVal = Material.QUEEN.score;
				else if ((attackers = dB.getKingMoveSet(pos.blackKing)) != 0) {
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
		if (pos.isWhitesTurn) {
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
	/**
	 * Returns an evaluation score according to the current phase and the evaluation scores of the same position in the context of an opening and
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
	/**
	 * Rates the chess position from the color to move's point of view. It considers material imbalance, mobility, and king safety.
	 * 
	 * @param pos The position to evaluate.
	 * @param allMoves A list of all the legal moves for the side to move.
	 * @param ply The current level of distance from the root in the search.
	 * @return
	 */
	public int score(Position pos) {
		byte numOfWhiteQueens, numOfBlackQueens, numOfWhiteRooks, numOfBlackRooks, numOfWhiteBishops, numOfBlackBishops, numOfWhiteKnights,
			numOfBlackKnights, numOfAllPieces;
		int bishopField, bishopColor, newBishopColor, phase;
		short pawnScore, baseScore, openingScore, endGameScore, score;
		byte[] bishopSqrArr;
		ETEntry eE;
		PTEntry pE;
		eE = eT.lookUp(pos.key);
		if (eE != null) {
			eE.generation = eGen;
			return eE.score;
		}
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
				bishopSqrArr = BitOperations.serialize(pos.whiteBishops | pos.blackBishops, (byte)(numOfWhiteBishops + numOfBlackBishops));
				bishopColor = Diagonal.getBySquareIndex(bishopSqrArr[0]).ordinal()%2;
				for (int i = 1; i < bishopSqrArr.length; i++) {
					bishopField = bishopSqrArr[i];
					if ((newBishopColor = Diagonal.getBySquareIndex(bishopField).ordinal()%2) != bishopColor)
						return Termination.INSUFFICIENT_MATERIAL.score;
					bishopColor = newBishopColor;
				}
			}
		}
		// Try for hashed pawn score.
		pE = pT.lookUp(pos.pawnKey);
		if (pE != null) {
			pE.generation = eGen;
			pawnScore = pE.score;
		}
		// Evaluate pawn structure.
		// Definitely needs to be thorough and sophisticated to make up for the costs of pawn hashing.
		else {
			pawnScore = (short)((BitOperations.getCardinality(pos.whitePawns) -
					BitOperations.getCardinality(pos.blackPawns))*Material.PAWN.score);
			pT.insert(new PTEntry(pos.pawnKey, pawnScore, eGen));
		}
		baseScore = 0;
		baseScore += (numOfWhiteQueens - numOfBlackQueens)*Material.QUEEN.score;
		baseScore += (numOfWhiteRooks - numOfBlackRooks)*Material.ROOK.score;
		baseScore += (numOfWhiteBishops - numOfBlackBishops)*Material.BISHOP.score;
		baseScore += (numOfWhiteKnights - numOfBlackKnights)*Material.KNIGHT.score;
		baseScore += pawnScore;
		// Need to implement separate evaluation features for openings and end games.
		// Pairs of piece square tables are imperative.
		openingScore = endGameScore = 0;
		openingScore += baseScore;
		endGameScore += baseScore;
		score = (short)taperedEvalScore(openingScore, endGameScore, phase);
		eT.insert(new ETEntry(pos.key, score, eGen));
		if (!pos.isWhitesTurn)
			score *= -1;
		return score;
	}
}
