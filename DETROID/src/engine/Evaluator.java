package engine;

import engine.Bitboard.Diagonal;
import engine.Bitboard.File;
import engine.Bitboard.Rank;
import engine.Bitboard.Square;
import util.*;

/**
 * A class for evaluating chess positions. It uses an evaluation hash table and a pawn hash table to improve performance. It also offers a static
 * exchange evaluation function.
 * 
 * @author Viktor
 *
 */
public final class Evaluator {
	
	// Distance tables for tropism.
	private final static byte[][] MANHATTAN_DISTANCE = new byte[64][64];
	private final static byte[][] CHEBYSHEV_DISTANCE = new byte[64][64];
	
	static {
		int r1, r2;
		int f1, f2;
		int rankDist, fileDist;
		for (int i = 0; i < 64; i++) {
			r1 = Rank.getBySquareIndex(i).ordinal();
			f1 = File.getBySquareIndex(i).ordinal();
			for (int j = 0; j < 64; j++) {
				r2 = Rank.getBySquareIndex(j).ordinal();
				f2 = File.getBySquareIndex(j).ordinal();
				rankDist = Math.abs(r2 - r1);
				fileDist = Math.abs(f2 - f1);
				MANHATTAN_DISTANCE[i][j] = (byte)(rankDist + fileDist);
				CHEBYSHEV_DISTANCE[i][j] = (byte)Math.max(rankDist, fileDist);
			}
		}
	}
	
	private Parameters params;
	
	// The sum of the respective weights of pieces for assessing the game phase.
	private final int TOTAL_PHASE_WEIGHTS;
	private final int PHASE_SCORE_LIMIT_FOR_INSUFFICIENT_MAT;
	
	private byte[][] PST_OPENING;
	private byte[][] PST_ENDGAME;
	
	private HashTable<ETEntry> eT;	// Evaluation score hash table.
	private HashTable<PTEntry> pT;	// Pawn hash table.
	
	private byte hashGen;	// Entry generation.
	
	public Evaluator(Parameters params, HashTable<ETEntry> evalTable, HashTable<PTEntry> pawnTable, byte hashEntryGeneration) {
		this.params = params;
		TOTAL_PHASE_WEIGHTS = 4*(params.KNIGHT_PHASE_WEIGHT + params.BISHOP_PHASE_WEIGHT + params.ROOK_PHASE_WEIGHT) + 2*params.QUEEN_PHASE_WEIGHT;
		PHASE_SCORE_LIMIT_FOR_INSUFFICIENT_MAT = Math.min(phaseScore(0, 0, 2, 0), phaseScore(0, 0, 0, 2));
		PST_OPENING = new byte[][]{params.PST_W_KING_OPENING, params.PST_W_QUEEN, params.PST_W_ROOK_OPENING, params.PST_W_BISHOP,
			params.PST_W_KNIGHT_OPENING, params.PST_W_PAWN_OPENING, params.PST_B_KING_OPENING, params.PST_B_QUEEN, params.PST_B_ROOK_OPENING,
			params.PST_B_BISHOP, params.PST_B_KNIGHT_OPENING, params.PST_B_PAWN_OPENING};
		PST_ENDGAME = new byte[][]{params.PST_W_KING_ENDGAME, params.PST_W_QUEEN, params.PST_W_ROOK_ENDGAME, params.PST_W_BISHOP,
			params.PST_W_KNIGHT_ENDGAME, params.PST_W_PAWN_ENDGAME, params.PST_B_KING_ENDGAME, params.PST_B_QUEEN, params.PST_B_ROOK_ENDGAME,
			params.PST_B_BISHOP, params.PST_B_KNIGHT_ENDGAME, params.PST_B_PAWN_ENDGAME};
		eT = evalTable;
		pT = pawnTable;
		hashGen = hashEntryGeneration;
	}
	/**
	 * Returns the value of a piece type defined by a piece index according to {@link #engine.Piece Piece}.
	 * 
	 * @param pieceInd A piece index according to {@link #engine.Piece Piece}.
	 * @return The value of the piece type.
	 */
	public short materialValueByPieceInd(int pieceInd) {
		if (pieceInd == Piece.W_KING.ind) return params.KING_PHASE_WEIGHT;
		else if (pieceInd == Piece.W_QUEEN.ind) return params.QUEEN_VALUE;
		else if (pieceInd == Piece.W_ROOK.ind) return params.ROOK_VALUE;
		else if (pieceInd == Piece.W_BISHOP.ind) return params.BISHOP_VALUE;
		else if (pieceInd == Piece.W_KNIGHT.ind) return params.KNIGHT_VALUE;
		else if (pieceInd == Piece.W_PAWN.ind) return params.PAWN_VALUE;
		else if (pieceInd == Piece.B_KING.ind) return params.KING_VALUE;
		else if (pieceInd == Piece.B_QUEEN.ind) return params.QUEEN_VALUE;
		else if (pieceInd == Piece.B_ROOK.ind) return params.ROOK_VALUE;
		else if (pieceInd == Piece.B_BISHOP.ind) return params.BISHOP_VALUE;
		else if (pieceInd == Piece.B_KNIGHT.ind) return params.KNIGHT_VALUE;
		else if (pieceInd == Piece.B_PAWN.ind) return params.PAWN_VALUE;
		else return 0;
	}
	/**
	 * A static exchange evaluation algorithm for determining a close approximation of a capture's value. It is mainly used for move ordering
	 * in the quiescence search.
	 * 
	 * @param pos
	 * @param move
	 * @return
	 */
	public short SEE(Position pos, Move move) {
		short score = 0, victimVal, firstVictimVal, attackerVal, kingVictVal = 0;
		long attackers, bpAttack, rkAttack, occupied = pos.allOccupied;
		boolean whitesTurn, noRetaliation = true;
		MoveSetDatabase dB;
		victimVal = materialValueByPieceInd(move.capturedPiece);
		// If the capturer was a king, return the captured piece's value as capturing the king would be illegal.
		if (move.movedPiece == Piece.W_KING.ind || move.movedPiece == Piece.B_KING.ind)
			return victimVal;
		firstVictimVal = victimVal;
		whitesTurn = pos.isWhitesTurn;
		occupied &= ~(1L << move.from);
		victimVal = materialValueByPieceInd(move.movedPiece);
		dB = MoveSetDatabase.getByIndex(move.to);
		while (true) {
			whitesTurn = !whitesTurn;
			attackerVal = 0;
			if (whitesTurn) {
				if ((attackers = dB.getBlackPawnCaptureSet(pos.whitePawns) & occupied) != 0)
					attackerVal = params.PAWN_VALUE;
				// Re-check could be omitted as a knight can not block any other piece's attack, but the savings would be minimal.
				else if ((attackers = dB.getKnightMoveSet(pos.whiteKnights) & occupied) != 0)
					attackerVal = params.KNIGHT_VALUE;
				else if ((attackers = (bpAttack = dB.getBishopMoveSet(occupied, occupied)) & pos.whiteBishops) != 0)
					attackerVal = params.BISHOP_VALUE;
				else if ((attackers = (rkAttack = dB.getRookMoveSet(occupied, occupied)) & pos.whiteRooks) != 0)
					attackerVal = params.ROOK_VALUE;
				else if ((attackers = (bpAttack | rkAttack) & pos.whiteQueens) != 0)
					attackerVal = params.QUEEN_VALUE;
				else if ((attackers = dB.getKingMoveSet(pos.whiteKing)) != 0) {
					attackerVal = params.KING_VALUE;
					kingVictVal = victimVal;
				}
				else
					break;
				// If the king has attackers, the exchange is over and the king's victim's value is disregarded
				if (victimVal == params.KING_VALUE) {
					score -= kingVictVal;
					break;
				}
				score += victimVal;
			}
			else {
				if ((attackers = dB.getWhitePawnCaptureSet(pos.blackPawns) & occupied) != 0)
					attackerVal = params.PAWN_VALUE;
				else if ((attackers = dB.getKnightMoveSet(pos.blackKnights) & occupied) != 0)
					attackerVal = params.KNIGHT_VALUE;
				else if ((attackers = (bpAttack = dB.getBishopMoveSet(occupied, occupied)) & pos.blackBishops) != 0)
					attackerVal = params.BISHOP_VALUE;
				else if ((attackers = (rkAttack = dB.getRookMoveSet(occupied, occupied)) & pos.blackRooks) != 0)
					attackerVal = params.ROOK_VALUE;
				else if ((attackers = (bpAttack | rkAttack) & pos.blackQueens) != 0)
					attackerVal = params.QUEEN_VALUE;
				else if ((attackers = dB.getKingMoveSet(pos.blackKing)) != 0) {
					attackerVal = params.KING_VALUE;
					kingVictVal = victimVal;
				}
				else
					break;
				if (victimVal == params.KING_VALUE) {
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
				score += params.QUEEN_VALUE - params.PAWN_VALUE;
			else if (move.type == MoveType.PROMOTION_TO_ROOK.ind)
				score += params.ROOK_VALUE - params.PAWN_VALUE;
			else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind)
				score += params.BISHOP_VALUE - params.PAWN_VALUE;
			else
				score += params.KNIGHT_VALUE - params.PAWN_VALUE;
		}
		return score;
	}
	/**
	 * Returns whether the position is a case of insufficient material.
	 * 
	 * @param pos
	 * @return
	 */
	public static boolean isMaterialInsufficient(Position pos) {
		int numOfWhiteBishops, numOfWhiteKnights, numOfBlackBishops, numOfBlackKnights, numOfAllPieces;
		int bishopField, bishopColor, newBishopColor;
		byte[] bishopSqrArr;
		if (pos.whitePawns != 0 && pos.blackPawns != 0)
			return false;
		numOfWhiteBishops = BitOperations.getHammingWeight(pos.whiteBishops);
		numOfWhiteKnights = BitOperations.getHammingWeight(pos.whiteKnights);
		numOfBlackBishops = BitOperations.getHammingWeight(pos.blackBishops);
		numOfBlackKnights = BitOperations.getHammingWeight(pos.blackKnights);
		numOfAllPieces = BitOperations.getHammingWeight(pos.allOccupied);
		if (numOfAllPieces == 2 ||
		(numOfAllPieces == 3 && (numOfWhiteBishops == 1 || numOfBlackBishops == 1 ||
								numOfWhiteKnights == 1 || numOfBlackKnights == 1)) ||
		(numOfAllPieces == 4 && numOfWhiteBishops == 1 && numOfBlackBishops == 1 &&
		Diagonal.getBySquareIndex(BitOperations.indexOfBit(pos.whiteBishops)).ordinal()%2 ==
		Diagonal.getBySquareIndex(BitOperations.indexOfBit(pos.blackBishops)).ordinal()%2))
			return true;
		if (numOfWhiteKnights == 0 && numOfBlackKnights == 0) {
			bishopSqrArr = BitOperations.serialize(pos.whiteBishops | pos.blackBishops, (byte)(numOfWhiteBishops + numOfBlackBishops));
			bishopColor = Diagonal.getBySquareIndex(bishopSqrArr[0]).ordinal()%2;
			for (int i = 1; i < bishopSqrArr.length; i++) {
				bishopField = bishopSqrArr[i];
				if ((newBishopColor = Diagonal.getBySquareIndex(bishopField).ordinal()%2) != bishopColor)
					return true;
				bishopColor = newBishopColor;
			}
		}
		return false;
	}
	/**
	 * Returns a phaseScore between 0 and 256 � la Fruit.
	 * 
	 * @param numOfQueens
	 * @param numOfRooks
	 * @param numOfBishops
	 * @param numOfKnights
	 * @return
	 */
	private int phaseScore(int numOfQueens, int numOfRooks, int numOfBishops, int numOfKnights) {
		int phase = TOTAL_PHASE_WEIGHTS - (numOfQueens*params.QUEEN_PHASE_WEIGHT + numOfRooks*params.ROOK_PHASE_WEIGHT
					+ numOfBishops*params.BISHOP_PHASE_WEIGHT + numOfKnights*params.KNIGHT_PHASE_WEIGHT);
		return (phase*params.GAME_PHASE_END_GAME_UPPER + TOTAL_PHASE_WEIGHTS/2)/TOTAL_PHASE_WEIGHTS;
	}
	/**
	 * Returns an estimation of the phase in which the current game is based on the given position.
	 * 
	 * @param pos
	 * @return
	 */
	public int phaseScore(Position pos) {
		int numOfQueens, numOfRooks, numOfBishops, numOfKnights;
		numOfQueens = BitOperations.getHammingWeight(pos.whiteQueens | pos.blackQueens)*params.QUEEN_PHASE_WEIGHT;
		numOfRooks = BitOperations.getHammingWeight(pos.whiteRooks | pos.blackRooks)*params.ROOK_PHASE_WEIGHT;
		numOfBishops = BitOperations.getHammingWeight(pos.whiteBishops | pos.blackBishops)*params.BISHOP_PHASE_WEIGHT;
		numOfKnights = BitOperations.getHammingWeight(pos.whiteKnights | pos.blackKnights)*params.KNIGHT_PHASE_WEIGHT;
		return phaseScore(numOfQueens, numOfRooks, numOfBishops, numOfKnights);
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
	private int taperedEvalScore(int openingEval, int endGameEval, int phaseScore) {
		return (openingEval*(params.GAME_PHASE_END_GAME_UPPER - phaseScore) + endGameEval*phaseScore)/params.GAME_PHASE_END_GAME_UPPER;
	}
	/**
	 * A simple evaluation of the pawn structure.
	 * 
	 * @param pos
	 * @param whiteKingInd
	 * @param blackKingInd
	 * @param phaseScore
	 * @return
	 */
	private short pawnKingStructureScore(long whiteKing, long blackKing, long whitePawns, long blackPawns, int phaseScore) {
		int score;
		byte numOfWhitePawns, numOfBlackPawns;
		long whitePawnAttacks, blackPawnAttacks;
		long whiteAdvanceSpans, blackAdvanceSpans;
		long whiteAttackSpans, blackAttackSpans;
		long whiteFrontSpans, blackFrontSpans;
		long whitePawnNeighbours, blackPawnNeighbours;
		long whitePassers, blackPassers;
		long whiteBackwardPawns, blackBackwardPawns;
		int whiteKingInd, blackKingInd;
		long whiteKingZone, blackKingZone;
		long whiteTropism, blackTropism;
		byte[] whitePawnInds, blackPawnInds;
		score = 0;
		// Base pawn material score.
		numOfWhitePawns = BitOperations.getHammingWeight(whitePawns);
		numOfBlackPawns = BitOperations.getHammingWeight(blackPawns);
		score += (numOfWhitePawns - numOfBlackPawns)*params.PAWN_VALUE;
		// Pawn defense.
		whitePawnAttacks = MultiMoveSets.whitePawnCaptureSets(whitePawns, -1);
		blackPawnAttacks = MultiMoveSets.blackPawnCaptureSets(blackPawns, -1);
		score += params.DEFENDED_PAWN_WEIGHT*BitOperations.getHammingWeight(whitePawnAttacks & whitePawns);
		score -= params.DEFENDED_PAWN_WEIGHT*BitOperations.getHammingWeight(blackPawnAttacks & blackPawns);
		// Blocked pawns.
		score -= params.BLOCKED_PAWN_WEIGHT1*BitOperations.getHammingWeight((whitePawns << 8) & whitePawns);
		score -= params.BLOCKED_PAWN_WEIGHT2*BitOperations.getHammingWeight((whitePawns << 16) & whitePawns);
		score -= params.BLOCKED_PAWN_WEIGHT3*BitOperations.getHammingWeight((whitePawns << 24) & whitePawns);
		score += params.BLOCKED_PAWN_WEIGHT1*BitOperations.getHammingWeight((blackPawns >>> 8) & blackPawns);
		score += params.BLOCKED_PAWN_WEIGHT2*BitOperations.getHammingWeight((blackPawns >>> 16) & blackPawns);
		score += params.BLOCKED_PAWN_WEIGHT3*BitOperations.getHammingWeight((blackPawns >>> 24) & blackPawns);
		// Passed pawns.
		whiteAdvanceSpans = whitePawns << 8;
		whiteAdvanceSpans |= whiteAdvanceSpans << 16;
		whiteAdvanceSpans |= whiteAdvanceSpans << 32;
		whiteAttackSpans = ((whiteAdvanceSpans >>> 1) & ~File.H.bits) | ((whiteAdvanceSpans << 1) & ~File.A.bits);
		whiteFrontSpans = whiteAdvanceSpans | whiteAttackSpans;
		blackAdvanceSpans = blackPawns >>> 8;
		blackAdvanceSpans |= blackAdvanceSpans >>> 16;
		blackAdvanceSpans |= blackAdvanceSpans >>> 32;
		blackAttackSpans = ((blackAdvanceSpans >>> 1) & ~File.H.bits) | ((blackAdvanceSpans << 1) & ~File.A.bits);
		blackFrontSpans = blackAdvanceSpans | blackAttackSpans;
		// Only count one even if there are multiple pawns on the passer's file.
		whitePassers = BitOperations.getHammingWeight(whitePawns & ~blackFrontSpans & ~whiteAdvanceSpans);
		blackPassers = BitOperations.getHammingWeight(blackPawns & ~whiteFrontSpans & ~blackAdvanceSpans);
		score += params.PASSED_PAWN_WEIGHT*whitePassers;
		score -= params.PASSED_PAWN_WEIGHT*blackPassers;
		// Isolated pawns.
		whitePawnNeighbours = whiteAdvanceSpans;
		whitePawnNeighbours |= whitePawnNeighbours >>> 8;
		whitePawnNeighbours |= whitePawnNeighbours >>> 16;
		whitePawnNeighbours |= whitePawnNeighbours >>> 32;
		blackPawnNeighbours = blackAdvanceSpans;
		blackPawnNeighbours |= blackPawnNeighbours << 8;
		blackPawnNeighbours |= blackPawnNeighbours << 16;
		blackPawnNeighbours |= blackPawnNeighbours << 32;
		score -= params.ISOLATED_PAWN_WEIGHT*BitOperations.getHammingWeight(~whitePawnNeighbours & whitePawns);
		score += params.ISOLATED_PAWN_WEIGHT*BitOperations.getHammingWeight(~blackPawnNeighbours & blackPawns);
		// Backward pawns.
		whiteBackwardPawns = whitePawns & blackAttackSpans & ~whiteFrontSpans;
		blackBackwardPawns = blackPawns & whiteAttackSpans & ~blackFrontSpans;
		score -= params.BACKWARD_PAWN_WEIGHT1*BitOperations.getHammingWeight(whiteBackwardPawns);
		score += params.BACKWARD_PAWN_WEIGHT1*BitOperations.getHammingWeight(blackBackwardPawns);
		// Extra penalty if it is an open pawn.
		score -= params.BACKWARD_PAWN_WEIGHT2*BitOperations.getHammingWeight(whiteBackwardPawns & ~blackAdvanceSpans);
		score += params.BACKWARD_PAWN_WEIGHT2*BitOperations.getHammingWeight(blackBackwardPawns & ~whiteAdvanceSpans);
		// King safety.
		// Pawn shield and pawn storm.
		if (whiteKing == Square.G1.bit || whiteKing == Square.H1.bit) {
			if ((whitePawns & Square.G2.bit) != 0)
				score += params.SHIELDING_PAWN_WEIGHT1;
			else if ((whitePawns & Square.G3.bit) != 0)
				score += params.SHIELDING_PAWN_WEIGHT2;
			if ((whitePawns & Square.H2.bit) != 0)
				score += params.SHIELDING_PAWN_WEIGHT1;
			else if ((whitePawns & Square.H3.bit) != 0)
				score += params.SHIELDING_PAWN_WEIGHT2;
			if ((whitePawns & Square.F2.bit) != 0)
				score += params.SHIELDING_PAWN_WEIGHT2;
			score -= params.SHIELD_THREATENING_PAWN_WEIGHT1*BitOperations.getHammingWeight((Square.G3.bit | Square.G4.bit | Square.H3.bit | Square.H4.bit)
					& blackPawns);
			score -= params.SHIELD_THREATENING_PAWN_WEIGHT2*BitOperations.getHammingWeight((Square.F3.bit | Square.F4.bit) & blackPawns);
		}
		else if (whiteKing == Square.A1.bit || whiteKing == Square.B1.bit || whiteKing == Square.C1.bit) {
			if ((whitePawns & Square.A2.bit) != 0)
				score += params.SHIELDING_PAWN_WEIGHT1;
			else if ((whitePawns & Square.A3.bit) != 0)
				score += params.SHIELDING_PAWN_WEIGHT2;
			if ((whitePawns & Square.B2.bit) != 0)
				score += params.SHIELDING_PAWN_WEIGHT1;
			else if ((whitePawns & Square.B3.bit) != 0)
				score += params.SHIELDING_PAWN_WEIGHT2;
			if ((whitePawns & Square.C2.bit) != 0)
				score += params.SHIELDING_PAWN_WEIGHT3;
			score -= params.SHIELD_THREATENING_PAWN_WEIGHT1*BitOperations.getHammingWeight((Square.A3.bit | Square.A4.bit | Square.B3.bit | Square.B4.bit |
					Square.C3.bit | Square.C4.bit) & blackPawns);
		}
		if (blackKing == Square.G8.bit || blackKing == Square.H8.bit) {
			if ((blackPawns & Square.G7.bit) != 0)
				score -= params.SHIELDING_PAWN_WEIGHT1;
			else if ((whitePawns & Square.G6.bit) != 0)
				score -= params.SHIELDING_PAWN_WEIGHT2;
			if ((blackPawns & Square.H7.bit) != 0)
				score -= params.SHIELDING_PAWN_WEIGHT1;
			else if ((whitePawns & Square.H6.bit) != 0)
				score -= params.SHIELDING_PAWN_WEIGHT2;
			if ((blackPawns & Square.F7.bit) != 0)
				score -= params.SHIELDING_PAWN_WEIGHT2;
			score += params.SHIELD_THREATENING_PAWN_WEIGHT1*BitOperations.getHammingWeight((Square.G5.bit | Square.G6.bit | Square.H5.bit | Square.H6.bit)
					& whitePawns);
			score += params.SHIELD_THREATENING_PAWN_WEIGHT2*BitOperations.getHammingWeight((Square.F5.bit | Square.F6.bit) & whitePawns);
		}
		else if (blackKing == Square.A8.bit || blackKing == Square.B8.bit || blackKing == Square.C8.bit) {
			if ((blackPawns & Square.A7.bit) != 0)
				score -= params.SHIELDING_PAWN_WEIGHT1;
			else if ((whitePawns & Square.A6.bit) != 0)
				score -= params.SHIELDING_PAWN_WEIGHT2;
			if ((blackPawns & Square.B7.bit) != 0)
				score -= params.SHIELDING_PAWN_WEIGHT1;
			else if ((whitePawns & Square.B6.bit) != 0)
				score -= params.SHIELDING_PAWN_WEIGHT2;
			if ((blackPawns & Square.C7.bit) != 0)
				score -= params.SHIELDING_PAWN_WEIGHT3;
			score += params.SHIELD_THREATENING_PAWN_WEIGHT1*BitOperations.getHammingWeight((Square.A5.bit | Square.A6.bit | Square.B5.bit | Square.B6.bit |
					Square.C5.bit | Square.C6.bit) & whitePawns);
		}
		// King-pawn defense and attack.
		whiteKingInd = BitOperations.indexOfBit(whiteKing);
		whiteKingZone = MoveSetDatabase.getByIndex(whiteKingInd).kingMoveMask;
		score -= params.DEFENDED_KING_AREA_SQUARE_WEIGHT1*BitOperations.getHammingWeight(blackPawnAttacks & whiteKingZone);
		score += params.DEFENDED_KING_AREA_SQUARE_WEIGHT2*BitOperations.getHammingWeight(whitePawnAttacks & whiteKingZone);
		score += params.DEFENDED_KING_AREA_SQUARE_WEIGHT3*BitOperations.getHammingWeight(whitePawns & whiteKingZone);
		blackKingInd = BitOperations.indexOfBit(blackKing);
		blackKingZone = MoveSetDatabase.getByIndex(blackKingInd).kingMoveMask;
		score += params.DEFENDED_KING_AREA_SQUARE_WEIGHT1*BitOperations.getHammingWeight(whitePawnAttacks & blackKingZone);
		score -= params.DEFENDED_KING_AREA_SQUARE_WEIGHT2*BitOperations.getHammingWeight(blackPawnAttacks & blackKingZone);
		score -= params.DEFENDED_KING_AREA_SQUARE_WEIGHT3*BitOperations.getHammingWeight(blackPawns & blackKingZone);
		// King-pawn tropism.
		whitePawnInds = BitOperations.serialize(whitePawns, numOfWhitePawns);
		whiteTropism = 0;
		for (int i = 0; i < numOfWhitePawns; i++)
			whiteTropism += MANHATTAN_DISTANCE[whiteKingInd][whitePawnInds[i]];
		blackPawnInds = BitOperations.serialize(blackPawns, numOfBlackPawns);
		blackTropism = 0;
		for (int i = 0; i < numOfBlackPawns; i++)
			blackTropism += MANHATTAN_DISTANCE[blackKingInd][blackPawnInds[i]];
		score -= (params.KING_PAWN_TROPISM_WEIGHT*whiteTropism)/numOfWhitePawns;
		score += (params.KING_PAWN_TROPISM_WEIGHT*blackTropism)/numOfBlackPawns;
		return (short)score;
	}
	/**
	 * Rates the chess position from the color to move's point of view. It considers material imbalance, coverage, pawn structure, king safety, king
	 * tropism, and trapped pieces.
	 * 
	 * @param pos The position to evaluate.
	 * @param allMoves A list of all the legal moves for the side to move.
	 * @param ply The current level of distance from the root in the search.
	 * @return
	 */
	public int score(Position pos, int alpha, int beta) {
		byte numOfWhiteQueens, numOfBlackQueens, numOfWhiteRooks, numOfBlackRooks, numOfWhiteBishops, numOfBlackBishops, numOfWhiteKnights,
			numOfBlackKnights, numOfAllPieces;
		int bishopField, bishopColor, newBishopColor, phase, piece;
		int whiteDistToBlackKing, blackDistToWhiteKing, whiteKingInd, blackKingInd;
		long whitePinnedPieces, blackPinnedPieces;
		long whiteMovablePieces, blackMovablePieces;
		long whiteCoverage, blackCoverage;
		long whitePawnAttacks, blackPawnAttacks;
		ByteList whitePieces, blackPieces;
		short pawnScore, baseScore, openingScore, endgameScore, score, extendedScore;
		byte[] bishopSqrArr, offsetBoard;
		boolean isWhitesTurn;
		ETEntry eE;
		PTEntry pE;
		score = 0;
		// Probe evaluation hash table.
		eE = eT.lookUp(pos.key);
		if (eE != null) {
			eE.generation = hashGen;
			score = eE.score;
			// If the entry is exact or would also trigger lazy eval within the current alpha-beta context, return the score.
			if (eE.isExact || score >= beta + params.LAZY_EVAL_MAR || score <= alpha - params.LAZY_EVAL_MAR)
				return eE.score;
		}
		isWhitesTurn = pos.isWhitesTurn;
		// In case of no hash hit, calculate the base score from scratch.
		if (eE == null) {
			score = 0;
			numOfWhiteQueens = BitOperations.getHammingWeight(pos.whiteQueens);
			numOfWhiteRooks = BitOperations.getHammingWeight(pos.whiteRooks);
			numOfWhiteBishops = BitOperations.getHammingWeight(pos.whiteBishops);
			numOfWhiteKnights = BitOperations.getHammingWeight(pos.whiteKnights);
			numOfBlackQueens = BitOperations.getHammingWeight(pos.blackQueens);
			numOfBlackRooks = BitOperations.getHammingWeight(pos.blackRooks);
			numOfBlackBishops = BitOperations.getHammingWeight(pos.blackBishops);
			numOfBlackKnights = BitOperations.getHammingWeight(pos.blackKnights);
			phase = phaseScore(numOfWhiteQueens + numOfBlackQueens, numOfWhiteRooks + numOfBlackRooks, numOfWhiteBishops + numOfBlackBishops,
					numOfWhiteKnights + numOfBlackKnights);
			// Check for insufficient material. Only consider the widely acknowledged scenarios without blocked position testing.
			if (phase >= PHASE_SCORE_LIMIT_FOR_INSUFFICIENT_MAT && numOfWhiteQueens == 0 && numOfBlackQueens == 0 &&
			numOfWhiteRooks == 0 && numOfBlackRooks == 0 && pos.whitePawns == 0 && pos.blackPawns == 0) {
				numOfAllPieces = BitOperations.getHammingWeight(pos.allOccupied);
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
			// Basic material score.
			baseScore = 0;
			baseScore += (numOfWhiteQueens - numOfBlackQueens)*params.QUEEN_VALUE;
			baseScore += (numOfWhiteRooks - numOfBlackRooks)*params.ROOK_VALUE;
			baseScore += (numOfWhiteBishops - numOfBlackBishops)*params.BISHOP_VALUE;
			baseScore += (numOfWhiteKnights - numOfBlackKnights)*params.KNIGHT_VALUE;
			// Try for hashed pawn score.
			pE = pT.lookUp(pos.pawnKey);
			if (pE != null) {
				pE.generation = hashGen;
				pawnScore = pE.score;
			}
			// Evaluate pawn structure.
			else {
				pawnScore = pawnKingStructureScore(pos.whiteKing, pos.blackKing, pos.whitePawns, pos.blackPawns, phase);
				pT.insert(new PTEntry(pos.pawnKey, pawnScore, hashGen));
			}
			baseScore += pawnScore;
			// Piece-square scores.
			openingScore = endgameScore = 0;
			offsetBoard = pos.offsetBoard;
			for (int i = 0; i < offsetBoard.length; i++) {
				piece = offsetBoard[i] - 1;
				if (piece < Piece.NULL.ind)
					continue;
				openingScore += PST_OPENING[piece][i];
				endgameScore += PST_ENDGAME[piece][i];
			}
			score = (short)(baseScore + taperedEvalScore(openingScore, endgameScore, phase));
			if (!isWhitesTurn)
				score *= -1;
			if (score <= alpha - params.LAZY_EVAL_MAR || score >= beta + params.LAZY_EVAL_MAR) {
				eT.insert(new ETEntry(pos.key, score, false, hashGen));
				return score;
			}
		}
		extendedScore = 0;
		// Pinned pieces.
		whitePinnedPieces = pos.getPinnedPieces(true);
		whiteMovablePieces = ~whitePinnedPieces;
		extendedScore -= params.PINNED_QUEEN_WEIGHT*BitOperations.getHammingWeight(pos.whiteQueens & whitePinnedPieces);
		extendedScore -= params.PINNED_ROOK_WEIGHT*BitOperations.getHammingWeight(pos.whiteRooks & whitePinnedPieces);
		extendedScore -= params.PINNED_BISHOP_WEIGHT*BitOperations.getHammingWeight((pos.whiteBishops) & whitePinnedPieces);
		extendedScore -= params.PINNED_KNIGHT_WEIGHT*BitOperations.getHammingWeight((pos.whiteKnights) & whitePinnedPieces);
		blackPinnedPieces = pos.getPinnedPieces(false);
		blackMovablePieces = ~blackPinnedPieces;
		extendedScore += params.PINNED_QUEEN_WEIGHT*BitOperations.getHammingWeight(pos.blackQueens & blackPinnedPieces);
		extendedScore += params.PINNED_ROOK_WEIGHT*BitOperations.getHammingWeight(pos.blackRooks & blackPinnedPieces);
		extendedScore += params.PINNED_BISHOP_WEIGHT*BitOperations.getHammingWeight((pos.blackBishops) & blackPinnedPieces);
		extendedScore += params.PINNED_KNIGHT_WEIGHT*BitOperations.getHammingWeight((pos.blackKnights) & blackPinnedPieces);
		// Piece mobility and coverage.
		whitePawnAttacks = MultiMoveSets.whitePawnCaptureSets(pos.whitePawns, -1);
		blackPawnAttacks = MultiMoveSets.blackPawnCaptureSets(pos.blackPawns, -1);
		whiteCoverage = MultiMoveSets.rookMoveSets((pos.whiteRooks | pos.whiteQueens) & whiteMovablePieces,
				pos.allOccupied, pos.allEmpty);
		whiteCoverage |= MultiMoveSets.bishopMoveSets((pos.whiteBishops | pos.whiteQueens) & whiteMovablePieces,
				pos.allOccupied, pos.allEmpty);
		whiteCoverage |= MultiMoveSets.knightMoveSets(pos.whiteKnights & whiteMovablePieces, -1);
		whiteCoverage |= MultiMoveSets.kingMoveSets(pos.whiteKing, -1);
		blackCoverage = MultiMoveSets.rookMoveSets((pos.blackRooks | pos.blackQueens) & blackMovablePieces,
				pos.allOccupied, pos.allEmpty);
		blackCoverage |= MultiMoveSets.bishopMoveSets((pos.blackBishops | pos.blackQueens) & blackMovablePieces,
				pos.allOccupied, pos.allEmpty);
		blackCoverage |= MultiMoveSets.knightMoveSets(pos.blackKnights & blackMovablePieces, -1);
		blackCoverage |= MultiMoveSets.kingMoveSets(pos.blackKing, -1);
		extendedScore += params.COVERED_SQUARE_WEIGHT*BitOperations.getHammingWeight(whiteCoverage);
		extendedScore += params.COVERED_FRIENDLY_OCCUPIED_SQUARE_WEIGHT*BitOperations.getHammingWeight(whiteCoverage & pos.allWhiteOccupied);
		extendedScore -= params.COVERED_SQUARE_WEIGHT*BitOperations.getHammingWeight(blackCoverage);
		extendedScore -= params.COVERED_FRIENDLY_OCCUPIED_SQUARE_WEIGHT*BitOperations.getHammingWeight(blackCoverage & pos.allBlackOccupied);
		// Pawn-piece defense.
		whitePawnAttacks = MultiMoveSets.whitePawnCaptureSets(pos.whitePawns, -1);
		blackPawnAttacks = MultiMoveSets.blackPawnCaptureSets(pos.blackPawns, -1);
		score += params.PAWN_DEFENDED_PIECE_WEIGHT*BitOperations.getHammingWeight(whitePawnAttacks & ((pos.allWhiteOccupied^pos.whiteKing)^pos.whitePawns));
		score -= params.PAWN_DEFENDED_PIECE_WEIGHT*BitOperations.getHammingWeight(blackPawnAttacks & ((pos.allBlackOccupied^pos.blackKing)^pos.blackPawns));
		// Piece-king tropism.
		whiteKingInd = BitOperations.indexOfBit(pos.whiteKing);
		blackKingInd = BitOperations.indexOfBit(pos.blackKing);
		whiteDistToBlackKing = 0;
		whitePieces = BitOperations.serialize(pos.allWhiteOccupied & ~pos.whitePawns);
		while (whitePieces.hasNext())
			whiteDistToBlackKing += CHEBYSHEV_DISTANCE[whitePieces.next()][blackKingInd];
		blackDistToWhiteKing = 0;
		blackPieces = BitOperations.serialize(pos.allBlackOccupied & ~pos.blackPawns);
		while (blackPieces.hasNext())
			blackDistToWhiteKing += CHEBYSHEV_DISTANCE[blackPieces.next()][whiteKingInd];
		extendedScore -= (params.PIECE_KING_TROPISM_WEIGHT*whiteDistToBlackKing)/BitOperations.getHammingWeight(pos.allWhiteOccupied^pos.whitePawns);
		extendedScore += (params.PIECE_KING_TROPISM_WEIGHT*blackDistToWhiteKing)/BitOperations.getHammingWeight(pos.allBlackOccupied^pos.blackPawns);
		// Stopped pawns.
		extendedScore -= params.STOPPED_PAWN_WEIGHT*BitOperations.getHammingWeight((pos.whitePawns << 8) & (pos.allBlackOccupied^pos.blackPawns));
		extendedScore += params.STOPPED_PAWN_WEIGHT*BitOperations.getHammingWeight((pos.blackPawns >>> 8) & (pos.allWhiteOccupied^pos.whitePawns));
		if (!isWhitesTurn)
			extendedScore *= -1;
		extendedScore += score;
		eT.insert(new ETEntry(pos.key, extendedScore, true, hashGen));
		return extendedScore;
	}
}
