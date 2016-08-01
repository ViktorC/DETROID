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
final class Evaluator {
	
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
	
	byte[] PST_W_PAWN_OPENING;
	byte[] PST_W_PAWN_ENDGAME;
	byte[] PST_W_KNIGHT_OPENING;
	byte[] PST_W_KNIGHT_ENDGAME;
	byte[] PST_W_BISHOP;
	byte[] PST_W_ROOK_OPENING;
	byte[] PST_W_ROOK_ENDGAME;
	byte[] PST_W_QUEEN;
	byte[] PST_W_KING_OPENING;
	byte[] PST_W_KING_ENDGAME;
	
	byte[] PST_B_PAWN_OPENING;
	byte[] PST_B_PAWN_ENDGAME;
	byte[] PST_B_KNIGHT_OPENING;
	byte[] PST_B_KNIGHT_ENDGAME;
	byte[] PST_B_BISHOP;
	byte[] PST_B_ROOK_OPENING;
	byte[] PST_B_ROOK_ENDGAME;
	byte[] PST_B_QUEEN;
	byte[] PST_B_KING_OPENING;
	byte[] PST_B_KING_ENDGAME;
	
	private byte[][] PST_OPENING;
	private byte[][] PST_ENDGAME;
	
	private LossyHashTable<ETEntry> eT;	// Evaluation score hash table.
	private LossyHashTable<PTEntry> pT;	// Pawn hash table.
	
	private byte hashGen;	// Entry generation.
	
	/**
	 * Initializes a chess position evaluator.
	 * 
	 * @param params
	 * @param evalTable
	 * @param pawnTable
	 * @param hashEntryGeneration
	 */
	public Evaluator(Parameters params, LossyHashTable<ETEntry> evalTable, LossyHashTable<PTEntry> pawnTable, byte hashEntryGeneration) {
		this.params = params;
		TOTAL_PHASE_WEIGHTS = 4*(params.KNIGHT_PHASE_WEIGHT + params.BISHOP_PHASE_WEIGHT + params.ROOK_PHASE_WEIGHT) + 2*params.QUEEN_PHASE_WEIGHT;
		PHASE_SCORE_LIMIT_FOR_INSUFFICIENT_MAT = Math.min(phaseScore(0, 0, 2, 0), phaseScore(0, 0, 0, 2));
		initPieceSquareArrays();
		PST_OPENING = new byte[][]{PST_W_KING_OPENING, PST_W_QUEEN, PST_W_ROOK_OPENING, PST_W_BISHOP, PST_W_KNIGHT_OPENING, PST_W_PAWN_OPENING,
			PST_B_KING_OPENING, PST_B_QUEEN, PST_B_ROOK_OPENING, PST_B_BISHOP, PST_B_KNIGHT_OPENING, PST_B_PAWN_OPENING};
		PST_ENDGAME = new byte[][]{PST_W_KING_ENDGAME, PST_W_QUEEN, PST_W_ROOK_ENDGAME, PST_W_BISHOP, PST_W_KNIGHT_ENDGAME, PST_W_PAWN_ENDGAME,
			PST_B_KING_ENDGAME, PST_B_QUEEN, PST_B_ROOK_ENDGAME, PST_B_BISHOP, PST_B_KNIGHT_ENDGAME, PST_B_PAWN_ENDGAME};
		eT = evalTable;
		pT = pawnTable;
		hashGen = hashEntryGeneration;
	}
	/**
	 * Initializes the piece square arrays with the correct order of values.
	 */
	public void initPieceSquareArrays() {
		int c1, c2;
		PST_W_PAWN_OPENING = new byte[64];
		PST_W_PAWN_ENDGAME = new byte[64];
		PST_W_KNIGHT_OPENING = new byte[64];
		PST_W_KNIGHT_ENDGAME = new byte[64];
		PST_W_BISHOP = new byte[64];
		PST_W_ROOK_OPENING = new byte[64];
		PST_W_ROOK_ENDGAME = new byte[64];
		PST_W_QUEEN = new byte[64];
		PST_W_KING_OPENING = new byte[64];
		PST_W_KING_ENDGAME = new byte[64];
		PST_B_PAWN_OPENING = new byte[64];
		PST_B_PAWN_ENDGAME = new byte[64];
		PST_B_KNIGHT_OPENING = new byte[64];
		PST_B_KNIGHT_ENDGAME = new byte[64];
		PST_B_BISHOP = new byte[64];
		PST_B_ROOK_OPENING = new byte[64];
		PST_B_ROOK_ENDGAME = new byte[64];
		PST_B_QUEEN = new byte[64];
		PST_B_KING_OPENING = new byte[64];
		PST_B_KING_ENDGAME = new byte[64];
		// Due to the reversed order of the rows in the definition of the white piece-square tables,
		// they are just right for black with negated values.
		for (int i = 0; i < 64; i++) {
			PST_B_PAWN_OPENING[i] = (byte)-params.PST_PAWN_OPENING[i];
			PST_B_PAWN_ENDGAME[i] = (byte)-params.PST_PAWN_ENDGAME[i];
			PST_B_KNIGHT_OPENING[i] = (byte)-params.PST_KNIGHT_OPENING[i];
			PST_B_KNIGHT_ENDGAME[i] = (byte)-params.PST_KNIGHT_ENDGAME[i];
			PST_B_BISHOP[i] = (byte)-params.PST_BISHOP[i];
			PST_B_ROOK_OPENING[i] = (byte)-params.PST_ROOK_OPENING[i];
			PST_B_ROOK_ENDGAME[i] = (byte)-params.PST_ROOK_ENDGAME[i];
			PST_B_QUEEN[i] = (byte)-params.PST_QUEEN[i];
			PST_B_KING_OPENING[i] = (byte)-params.PST_KING_OPENING[i];
			PST_B_KING_ENDGAME[i] = (byte)-params.PST_KING_ENDGAME[i];
		}
		// To get the right values for the white piece-square tables, we vertically mirror and negate the ones for black
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				c1 = i*8 + j;
				c2 = ((7 - i)*8) + j;
				PST_W_PAWN_OPENING[c1] = (byte)-PST_B_PAWN_OPENING[c2];
				PST_W_PAWN_ENDGAME[c1] = (byte)-PST_B_PAWN_ENDGAME[c2];
				PST_W_KNIGHT_OPENING[c1] = (byte)-PST_B_KNIGHT_OPENING[c2];
				PST_W_KNIGHT_ENDGAME[c1] = (byte)-PST_B_KNIGHT_ENDGAME[c2];
				PST_W_BISHOP[c1] = (byte)-PST_B_BISHOP[c2];
				PST_W_ROOK_OPENING[c1] = (byte)-PST_B_ROOK_OPENING[c2];
				PST_W_ROOK_ENDGAME[c1] = (byte)-PST_B_ROOK_ENDGAME[c2];
				PST_W_QUEEN[c1] = (byte)-PST_B_QUEEN[c2];
				PST_W_KING_OPENING[c1] = (byte)-PST_B_KING_OPENING[c2];
				PST_W_KING_ENDGAME[c1] = (byte)-PST_B_KING_ENDGAME[c2];
			}
		}
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
	 * Returns a phaseScore between 0 and 256 a la Fruit.
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
		return (phase*params.GAME_PHASE_ENDGAME_UPPER + TOTAL_PHASE_WEIGHTS/2)/TOTAL_PHASE_WEIGHTS;
	}
	/**
	 * Returns an estimation of the phase in which the current game is based on the given position.
	 * 
	 * @param pos
	 * @return
	 */
	public int phaseScore(Position pos) {
		int numOfQueens, numOfRooks, numOfBishops, numOfKnights;
		numOfQueens = BitOperations.getHammingWeight(pos.whiteQueens | pos.blackQueens);
		numOfRooks = BitOperations.getHammingWeight(pos.whiteRooks | pos.blackRooks);
		numOfBishops = BitOperations.getHammingWeight(pos.whiteBishops | pos.blackBishops);
		numOfKnights = BitOperations.getHammingWeight(pos.whiteKnights | pos.blackKnights);
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
		return (openingEval*(params.GAME_PHASE_ENDGAME_UPPER - phaseScore) + endGameEval*phaseScore)/params.GAME_PHASE_ENDGAME_UPPER;
	}
	/**
	 * A simple static, context free evaluation of the pawn-king structure.
	 * 
	 * @param whiteKing
	 * @param blackKing
	 * @param whitePawns
	 * @param blackPawns
	 * @return
	 */
	private short pawnKingStructureScore(long whiteKing, long blackKing, long whitePawns, long blackPawns) {
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
			score -= params.SHIELD_THREATENING_PAWN_WEIGHT1*BitOperations.getHammingWeight((Square.G3.bit | Square.G4.bit | Square.H3.bit |
					Square.H4.bit) & blackPawns);
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
			score -= params.SHIELD_THREATENING_PAWN_WEIGHT1*BitOperations.getHammingWeight((Square.A3.bit | Square.A4.bit | Square.B3.bit |
					Square.B4.bit | Square.C3.bit | Square.C4.bit) & blackPawns);
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
			score += params.SHIELD_THREATENING_PAWN_WEIGHT1*BitOperations.getHammingWeight((Square.G5.bit | Square.G6.bit | Square.H5.bit |
					Square.H6.bit) & whitePawns);
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
			score += params.SHIELD_THREATENING_PAWN_WEIGHT1*BitOperations.getHammingWeight((Square.A5.bit | Square.A6.bit | Square.B5.bit |
					Square.B6.bit | Square.C5.bit | Square.C6.bit) & whitePawns);
		}
		// King-pawn defense and attack.
		whiteKingInd = BitOperations.indexOfBit(whiteKing);
		whiteKingZone = MoveSetDatabase.getByIndex(whiteKingInd).kingMoveMask;
		score -= params.ATTACKED_KING_AREA_SQUARE_WEIGHT*BitOperations.getHammingWeight(blackPawnAttacks & whiteKingZone);
		score += params.DEFENDED_KING_AREA_SQUARE_WEIGHT*BitOperations.getHammingWeight(whitePawnAttacks & whiteKingZone);
		blackKingInd = BitOperations.indexOfBit(blackKing);
		blackKingZone = MoveSetDatabase.getByIndex(blackKingInd).kingMoveMask;
		score += params.ATTACKED_KING_AREA_SQUARE_WEIGHT*BitOperations.getHammingWeight(whitePawnAttacks & blackKingZone);
		score -= params.DEFENDED_KING_AREA_SQUARE_WEIGHT*BitOperations.getHammingWeight(blackPawnAttacks & blackKingZone);
		// King-pawn tropism.
		whitePawnInds = BitOperations.serialize(whitePawns, numOfWhitePawns);
		whiteTropism = 0;
		for (int i = 0; i < numOfWhitePawns; i++)
			whiteTropism += MANHATTAN_DISTANCE[whiteKingInd][whitePawnInds[i]];
		blackPawnInds = BitOperations.serialize(blackPawns, numOfBlackPawns);
		blackTropism = 0;
		for (int i = 0; i < numOfBlackPawns; i++)
			blackTropism += MANHATTAN_DISTANCE[blackKingInd][blackPawnInds[i]];
		score -= (params.KING_PAWN_TROPISM_WEIGHT*whiteTropism)/Math.max(1, numOfWhitePawns);
		score += (params.KING_PAWN_TROPISM_WEIGHT*blackTropism)/Math.max(1, numOfBlackPawns);
		return (short)score;
	}
	/**
	 * A static evaluation of the chess position from the color to move's point of view. It considers material imbalance, coverage, pawn structure,
	 * king safety, king tropism, king mobility, trapped pieces, etc.
	 * 
	 * @param pos
	 * @param alpha
	 * @param beta
	 * @return
	 */
	public int score(Position pos, int alpha, int beta) {
		byte numOfWhiteQueens, numOfBlackQueens, numOfWhiteRooks, numOfBlackRooks, numOfWhiteBishops, numOfBlackBishops, numOfWhiteKnights,
			numOfBlackKnights, numOfAllPieces;
		int bishopField, bishopColor, newBishopColor, phase, piece;
		int whiteDistToBlackKing, blackDistToWhiteKing, whiteKingInd, blackKingInd;
		long whitePinnedPieces, blackPinnedPieces;
		long whiteMovablePieces, blackMovablePieces;
		long whiteKnightCoverage, whiteBishopCoverage, whiteRookCoverage, whiteQueenCoverage;
		long blackKnightCoverage, blackBishopCoverage, blackRookCoverage, blackQueenCoverage;
		long whitePawnAttacks, blackPawnAttacks;
		long whiteKingMobility, blackKingMobility;
		ByteList whitePieces, blackPieces;
		short pawnScore, baseScore, openingScore, endgameScore, score, extendedScore;
		byte[] bishopSqrArr, offsetBoard;
		boolean isWhitesTurn;
		ETEntry eE;
		PTEntry pE;
		isWhitesTurn = pos.isWhitesTurn;;
		score = 0;
		// Probe evaluation hash table.
		eE = eT.lookUp(pos.key);
		if (eE != null) {
			eE.generation = hashGen;
			score = eE.score;
			// If the entry is exact or would also trigger lazy eval within the current alpha-beta context, return the score.
			if (eE.isExact || score >= beta + params.LAZY_EVAL_MAR || score <= alpha - params.LAZY_EVAL_MAR)
				return score;
		}
		// In case of no hash hit, calculate the base score from scratch.
		/* @!TODO Try storing only exact scores or remove lazy eval alltogether and use tapered eval for certain extended eval terms such as
		 * king mobility and see how it affects performance
		 */
//		else {
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
//			pE = pT.lookUp(pos.pawnKey);
//			if (pE != null) {
//				pE.generation = hashGen;
//				pawnScore = pE.score;
//			}
//			// Evaluate pawn structure.
//			else {
//				pawnScore = pawnKingStructureScore(pos.whiteKing, pos.blackKing, pos.whitePawns, pos.blackPawns);
//				pT.insert(new PTEntry(pos.pawnKey, pawnScore, hashGen));
//			}
//			baseScore += pawnScore;
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
//			if (score <= alpha - params.LAZY_EVAL_MAR || score >= beta + params.LAZY_EVAL_MAR) {
//				eT.insert(new ETEntry(pos.key, score, false, hashGen));
//				return score;
//			}
//		}
//		extendedScore = 0;
//		// Pinned pieces.
//		whitePinnedPieces = pos.getPinnedPieces(true);
//		whiteMovablePieces = ~whitePinnedPieces;
//		extendedScore -= params.PINNED_QUEEN_WEIGHT*BitOperations.getHammingWeight(pos.whiteQueens & whitePinnedPieces);
//		extendedScore -= params.PINNED_ROOK_WEIGHT*BitOperations.getHammingWeight(pos.whiteRooks & whitePinnedPieces);
//		extendedScore -= params.PINNED_BISHOP_WEIGHT*BitOperations.getHammingWeight((pos.whiteBishops) & whitePinnedPieces);
//		extendedScore -= params.PINNED_KNIGHT_WEIGHT*BitOperations.getHammingWeight((pos.whiteKnights) & whitePinnedPieces);
//		blackPinnedPieces = pos.getPinnedPieces(false);
//		blackMovablePieces = ~blackPinnedPieces;
//		extendedScore += params.PINNED_QUEEN_WEIGHT*BitOperations.getHammingWeight(pos.blackQueens & blackPinnedPieces);
//		extendedScore += params.PINNED_ROOK_WEIGHT*BitOperations.getHammingWeight(pos.blackRooks & blackPinnedPieces);
//		extendedScore += params.PINNED_BISHOP_WEIGHT*BitOperations.getHammingWeight((pos.blackBishops) & blackPinnedPieces);
//		extendedScore += params.PINNED_KNIGHT_WEIGHT*BitOperations.getHammingWeight((pos.blackKnights) & blackPinnedPieces);
//		// Piece mobility and coverage.
//		whitePawnAttacks = MultiMoveSets.whitePawnCaptureSets(pos.whitePawns, -1);
//		blackPawnAttacks = MultiMoveSets.blackPawnCaptureSets(pos.blackPawns, -1);
//		whiteQueenCoverage = MultiMoveSets.queenMoveSets(pos.whiteQueens & whiteMovablePieces,
//				pos.allOccupied, pos.allEmpty);
//		whiteRookCoverage = MultiMoveSets.rookMoveSets(pos.whiteRooks & whiteMovablePieces,
//				pos.allOccupied, pos.allEmpty);
//		whiteBishopCoverage = MultiMoveSets.bishopMoveSets(pos.whiteBishops & whiteMovablePieces,
//				pos.allOccupied, pos.allEmpty);
//		whiteKnightCoverage = MultiMoveSets.knightMoveSets(pos.whiteKnights & whiteMovablePieces, -1);
//		blackQueenCoverage = MultiMoveSets.queenMoveSets(pos.blackQueens & blackMovablePieces,
//				pos.allOccupied, pos.allEmpty);
//		blackRookCoverage = MultiMoveSets.rookMoveSets(pos.blackRooks & blackMovablePieces,
//				pos.allOccupied, pos.allEmpty);
//		blackBishopCoverage = MultiMoveSets.bishopMoveSets(pos.blackBishops & blackMovablePieces,
//				pos.allOccupied, pos.allEmpty);
//		blackKnightCoverage = MultiMoveSets.knightMoveSets(pos.blackKnights & blackMovablePieces, -1);
//		extendedScore += params.QUEEN_COVERED_SQUARE_WEIGHT*BitOperations.getHammingWeight(whiteQueenCoverage);
//		extendedScore += params.ROOK_COVERED_SQUARE_WEIGHT*BitOperations.getHammingWeight(whiteRookCoverage);
//		extendedScore += params.BISHOP_COVERED_SQUARE_WEIGHT*BitOperations.getHammingWeight(whiteBishopCoverage);
//		extendedScore += params.KNIGHT_COVERED_SQUARE_WEIGHT*BitOperations.getHammingWeight(whiteKnightCoverage);
//		extendedScore -= params.QUEEN_COVERED_SQUARE_WEIGHT*BitOperations.getHammingWeight(blackQueenCoverage);
//		extendedScore -= params.ROOK_COVERED_SQUARE_WEIGHT*BitOperations.getHammingWeight(blackRookCoverage);
//		extendedScore -= params.BISHOP_COVERED_SQUARE_WEIGHT*BitOperations.getHammingWeight(blackBishopCoverage);
//		extendedScore -= params.KNIGHT_COVERED_SQUARE_WEIGHT*BitOperations.getHammingWeight(blackKnightCoverage);
//		// Pawn-piece defense.
//		whitePawnAttacks = MultiMoveSets.whitePawnCaptureSets(pos.whitePawns & whiteMovablePieces, -1);
//		blackPawnAttacks = MultiMoveSets.blackPawnCaptureSets(pos.blackPawns & blackMovablePieces, -1);
//		score += params.PAWN_DEFENDED_PIECE_WEIGHT*BitOperations.getHammingWeight(whitePawnAttacks &
//				((pos.allWhiteOccupied^pos.whiteKing)^pos.whitePawns));
//		score -= params.PAWN_DEFENDED_PIECE_WEIGHT*BitOperations.getHammingWeight(blackPawnAttacks &
//				((pos.allBlackOccupied^pos.blackKing)^pos.blackPawns));
//		// Pawn-piece attack.
//		score += params.PAWN_ATTACKED_PIECE_WEIGHT*BitOperations.getHammingWeight(whitePawnAttacks &
//				((pos.allBlackOccupied^pos.blackKing)^pos.blackPawns));
//		score -= params.PAWN_ATTACKED_PIECE_WEIGHT*BitOperations.getHammingWeight(blackPawnAttacks &
//				((pos.allWhiteOccupied^pos.whiteKing)^pos.whitePawns));
//		// Stopped pawns.
//		extendedScore -= params.STOPPED_PAWN_WEIGHT*BitOperations.getHammingWeight((pos.whitePawns << 8) & (pos.allBlackOccupied^pos.blackPawns));
//		extendedScore += params.STOPPED_PAWN_WEIGHT*BitOperations.getHammingWeight((pos.blackPawns >>> 8) & (pos.allWhiteOccupied^pos.whitePawns));
//		// Piece-king tropism.
//		whiteKingInd = BitOperations.indexOfBit(pos.whiteKing);
//		blackKingInd = BitOperations.indexOfBit(pos.blackKing);
//		whiteDistToBlackKing = 0;
//		whitePieces = BitOperations.serialize(pos.allWhiteOccupied & ~pos.whitePawns);
//		while (whitePieces.hasNext())
//			whiteDistToBlackKing += CHEBYSHEV_DISTANCE[whitePieces.next()][blackKingInd];
//		blackDistToWhiteKing = 0;
//		blackPieces = BitOperations.serialize(pos.allBlackOccupied & ~pos.blackPawns);
//		while (blackPieces.hasNext())
//			blackDistToWhiteKing += CHEBYSHEV_DISTANCE[blackPieces.next()][whiteKingInd];
//		extendedScore -= (params.PIECE_KING_TROPISM_WEIGHT*whiteDistToBlackKing)/
//				BitOperations.getHammingWeight(pos.allWhiteOccupied^pos.whitePawns);
//		extendedScore += (params.PIECE_KING_TROPISM_WEIGHT*blackDistToWhiteKing)/
//				BitOperations.getHammingWeight(pos.allBlackOccupied^pos.blackPawns);
//		// King mobility.
//		whiteKingMobility = MoveSetDatabase.getByIndex(whiteKingInd).getKingMoveSet(pos.allNonWhiteOccupied);
//		blackKingMobility = MoveSetDatabase.getByIndex(blackKingInd).getKingMoveSet(pos.allNonBlackOccupied);
//		extendedScore += params.KING_MOBILITY_WEIGHT*BitOperations.getHammingWeight(whiteKingMobility & ~(blackPawnAttacks |
//				blackKnightCoverage | blackBishopCoverage | blackRookCoverage | blackQueenCoverage | blackKingMobility));
//		extendedScore -= params.KING_MOBILITY_WEIGHT*BitOperations.getHammingWeight(blackKingMobility & ~(whitePawnAttacks |
//				whiteKnightCoverage | whiteBishopCoverage | whiteRookCoverage | whiteQueenCoverage | whiteKingMobility));
//		// Final extended score.
//		if (!isWhitesTurn)
//			extendedScore *= -1;
//		score += extendedScore;
		eT.insert(new ETEntry(pos.key, score, true, hashGen));
		return score;
	}
}
