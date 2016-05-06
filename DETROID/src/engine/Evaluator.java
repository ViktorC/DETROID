package engine;

import engine.Board.Diagonal;
import engine.Board.File;
import engine.Board.Rank;
import engine.Board.Square;
import util.*;

/**
 * A class for evaluating chess positions.
 * 
 * @author Viktor
 *
 */
public final class Evaluator {
	
	// The sum of the respective weights of pieces for assessing the game phase.
	private final static int TOTAL_PHASE_WEIGHTS = 4*(Material.KNIGHT.phaseWeight + Material.BISHOP.phaseWeight +
			Material.ROOK.phaseWeight) + 2*Material.QUEEN.phaseWeight;
	
	// The margin for lazy evaluation. The extended score should never differ by more than this value form the core score.
	private final static int LAZY_EVAL_MAR = 151;
	
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
	
	// Piece-square tables based on and extending Tomasz Michniewski's "Unified Evaluation" tables.
	private final static byte[] PST_W_PAWN_OPENING =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		  50, 50, 50, 50, 50, 50, 50, 50,
		  10, 10, 30, 40, 40, 30, 10, 10,
		   5, 15, 25, 30, 30, 25, 15,  5,
		   0, 15, 20, 25, 25, 20, 15,  0,
		   0,  8, 15, 20, 20, 15,  8,  0,
		   5,  5,  5,-10,-10,  5,  5,  5,
		   0,  0,  0,  0,  0,  0,  0,  0};
	private final static byte[] PST_W_PAWN_ENDGAME =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		  70, 75, 80, 80, 80, 80, 75, 70,
		  20, 30, 40, 45, 45, 40, 30, 20,
		  10, 20, 25, 30, 30, 25, 20, 10,
		   5, 10, 10, 15, 15, 10, 10,  5,
		   0,  0, -5, -5, -5, -5,  0,  0,
		  -5,-10,-10,-20,-20,-10,-10, -5,
		   0,  0,  0,  0,  0,  0,  0,  0};
	private final static byte[] PST_W_KNIGHT_OPENING =
		{-15,-10, -5, -5, -5, -5,-10,-15,
		 -10,-10,  0,  0,  0,  0,-10,-10,
		  -5,  0,  5, 10, 10,  5,  0, -5,
		  -5,  0, 10, 15, 15, 10,  0, -5,
		  -5,  0, 10, 15, 15, 10,  0, -5,
		  -5,  0,  5, 10, 10,  5,  0, -5,
		 -10, -5,  0,  0,  0,  0, -5,-10,
		 -15,-10, -5, -5, -5, -5,-10,-15};
	private final static byte[] PST_W_KNIGHT_ENDGAME =
		{-50,-40,-30,-30,-30,-30,-40,-50,
		 -40,-20, -5,  0,  0, -5,-20,-40,
		 -30,  0,  0,  0,  0,  0, -5,-30,
		 -30,  0,  5,  5,  5,  5,  0,-30,
		 -30, -5,  5,  5,  5,  5, -5,-30,
		 -30,  0,  0,  0,  0,  0,  0,-30,
		 -40,-20, -5,  0,  0, -5,-20,-40,
		 -50,-40,-30,-30,-30,-30,-40,-50};
	private final static byte[] PST_W_BISHOP =
		{-20,-10,-10,-10,-10,-10,-10,-20,
		 -10,  0,  0,  0,  0,  0,  0,-10,
		 -10,  0,  5, 10, 10,  5,  0,-10,
		 -10,  5,  5, 15, 15,  5,  5,-10,
		 -10,  0, 10, 15, 15, 10,  0,-10,
		 -10, 10, 10, 10, 10, 10, 10,-10,
		 -10,  5,  0,  0,  0,  0,  5,-10,
		 -20,-10,-10,-10,-10,-10,-10,-20};
	private final static byte[] PST_W_ROOK_OPENING =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		   5, 10, 10, 10, 10, 10, 10,  5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  -5, -5, -5, -5, -5, -5, -5, -5,
		  20, -5,  0, 40,  5, 40, -5, 20};
	private final static byte[] PST_W_ROOK_ENDGAME =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		   5, 10, 10, 10, 10, 10, 10,  5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		   0,  0,  5,  5,  5,  5,  0,  0};
	private final static byte[] PST_W_QUEEN =
		{-20,-10,-10, -5, -5,-10,-10,-20,
		 -10,  0,  0,  0,  0,  0,  0,-10,
		 -10,  0,  5,  5,  5,  5,  0,-10,
		  -5,  0,  5,  5,  5,  5,  0, -5,
		   0,  0,  5,  5,  5,  5,  0, -5,
		 -10,  5,  5,  5,  5,  5,  0,-10,
		 -10,  0,  5,  0,  0,  0,  0,-10,
		 -20,-10,-10, -5, -5,-10,-10,-20};
	private final static byte[] PST_W_KING_OPENING =
		{-30,-40,-40,-50,-50,-40,-40,-30,
		 -30,-40,-40,-50,-50,-40,-40,-30,
		 -30,-40,-40,-50,-50,-40,-40,-30,
		 -30,-40,-40,-50,-50,-40,-40,-30,
		 -20,-30,-30,-40,-40,-30,-30,-20,
		 -10,-20,-20,-20,-20,-20,-20,-10,
		  20, 20,  0,  0,  0,  0, 20, 20,
		  20, 20, 20,-10,  0,-10, 20, 20};
	private final static byte[] PST_W_KING_ENDGAME =
		{-50,-40,-30,-20,-20,-30,-40,-50,
		 -30,-20,-10,  0,  0,-10,-20,-30,
		 -30,-10, 20, 30, 30, 20,-10,-30,
		 -30,-10, 30, 40, 40, 30,-10,-30,
		 -30,-10, 30, 40, 40, 30,-10,-30,
		 -30,-10, 20, 30, 30, 20,-10,-30,
		 -30,-30,  0,  0,  0,  0,-30,-30,
		 -50,-30,-30,-30,-30,-30,-30,-50};
	
	private final static byte[] PST_B_PAWN_OPENING = new byte[64];
	private final static byte[] PST_B_PAWN_ENDGAME = new byte[64];
	private final static byte[] PST_B_KNIGHT_OPENING = new byte[64];
	private final static byte[] PST_B_KNIGHT_ENDGAME = new byte[64];
	private final static byte[] PST_B_BISHOP = new byte[64];
	private final static byte[] PST_B_ROOK_OPENING = new byte[64];
	private final static byte[] PST_B_ROOK_ENDGAME = new byte[64];
	private final static byte[] PST_B_QUEEN = new byte[64];
	private final static byte[] PST_B_KING_OPENING = new byte[64];
	private final static byte[] PST_B_KING_ENDGAME = new byte[64];
	
	static {
		int c1, c2;
		// Due to the reversed order of the rows in the definition of the white piece-square tables,
		// they are just right for black with negated values.
		for (int i = 0; i < 64; i++) {
			PST_B_PAWN_OPENING[i] = (byte)-PST_W_PAWN_OPENING[i];
			PST_B_PAWN_ENDGAME[i] = (byte)-PST_W_PAWN_ENDGAME[i];
			PST_B_KNIGHT_OPENING[i] = (byte)-PST_W_KNIGHT_OPENING[i];
			PST_B_KNIGHT_ENDGAME[i] = (byte)-PST_W_KNIGHT_ENDGAME[i];
			PST_B_BISHOP[i] = (byte)-PST_W_BISHOP[i];
			PST_B_ROOK_OPENING[i] = (byte)-PST_W_ROOK_OPENING[i];
			PST_B_ROOK_ENDGAME[i] = (byte)-PST_W_ROOK_ENDGAME[i];
			PST_B_QUEEN[i] = (byte)-PST_W_QUEEN[i];
			PST_B_KING_OPENING[i] = (byte)-PST_W_KING_OPENING[i];
			PST_B_KING_ENDGAME[i] = (byte)-PST_W_KING_ENDGAME[i];
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
	
	private final static byte[][] PST_OPENING = {PST_W_KING_OPENING, PST_W_QUEEN, PST_W_ROOK_OPENING, PST_W_BISHOP, PST_W_KNIGHT_OPENING,
		PST_W_PAWN_OPENING, PST_B_KING_OPENING, PST_B_QUEEN, PST_B_ROOK_OPENING, PST_B_BISHOP, PST_B_KNIGHT_OPENING, PST_B_PAWN_OPENING};
	private final static byte[][] PST_ENDGAME = {PST_W_KING_ENDGAME, PST_W_QUEEN, PST_W_ROOK_ENDGAME, PST_W_BISHOP, PST_W_KNIGHT_ENDGAME,
		PST_W_PAWN_ENDGAME, PST_B_KING_ENDGAME, PST_B_QUEEN, PST_B_ROOK_ENDGAME, PST_B_BISHOP, PST_B_KNIGHT_ENDGAME, PST_B_PAWN_ENDGAME};
	
	private HashTable<ETEntry> eT;	// Evaluation score hash table.
	private HashTable<PTEntry> pT;	// Pawn hash table.
	
	private byte hashGen;	// Entry generation.
	
	public Evaluator(HashTable<ETEntry> evalTable, HashTable<PTEntry> pawnTable, byte hashEntryGeneration) {
		eT = evalTable;
		pT = pawnTable;
		hashGen = hashEntryGeneration;
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
	private static int phaseScore(int numOfQueens, int numOfRooks, int numOfBishops, int numOfKnights) {
		int phase = TOTAL_PHASE_WEIGHTS - (numOfQueens*Material.QUEEN.phaseWeight + numOfRooks*Material.ROOK.phaseWeight
					+ numOfBishops*Material.BISHOP.phaseWeight + numOfKnights*Material.KNIGHT.phaseWeight);
		return (phase*GamePhase.END_GAME.upperBound + TOTAL_PHASE_WEIGHTS/2)/TOTAL_PHASE_WEIGHTS;
	}
	/**
	 * Returns an estimation of the phase in which the current game is based on the given position.
	 * 
	 * @param pos
	 * @return
	 */
	public static int phaseScore(Position pos) {
		int numOfQueens, numOfRooks, numOfBishops, numOfKnights;
		numOfQueens = BitOperations.getHammingWeight(pos.whiteQueens | pos.blackQueens)*Material.QUEEN.phaseWeight;
		numOfRooks = BitOperations.getHammingWeight(pos.whiteRooks | pos.blackRooks)*Material.ROOK.phaseWeight;
		numOfBishops = BitOperations.getHammingWeight(pos.whiteBishops | pos.blackBishops)*Material.BISHOP.phaseWeight;
		numOfKnights = BitOperations.getHammingWeight(pos.whiteKnights | pos.blackKnights)*Material.KNIGHT.phaseWeight;
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
	private static int taperedEvalScore(int openingEval, int endGameEval, int phaseScore) {
		return (openingEval*(GamePhase.END_GAME.upperBound - phaseScore) + endGameEval*phaseScore)/GamePhase.END_GAME.upperBound;
	}
	/**
	 * A simple evaluation of the pawn structure.
	 * 
	 * @param pos
	 * @return
	 */
	private static short pawnKingStructureScore(Position pos, int whiteKingInd, int blackKingInd, int phaseScore) {
		int score, materialScore;
		byte numOfWhitePawns, numOfBlackPawns;
		long whitePawnAttacks, blackPawnAttacks;
		long whiteFrontSpans, blackFrontSpans;
		long whitePawnNeighbours, blackPawnNeighbours;
		long whitePassers, blackPassers;
		long whiteKingZone, blackKingZone;
		byte[] whitePawns, blackPawns;
		score = 0;
		// Base pawn material score.
		numOfWhitePawns = BitOperations.getHammingWeight(pos.whitePawns);
		numOfBlackPawns = BitOperations.getHammingWeight(pos.blackPawns);
		materialScore = (numOfWhitePawns - numOfBlackPawns)*Material.PAWN.score;
		// Pawns are worth more towards the end of the game.
		score += taperedEvalScore(materialScore, (int)(materialScore*1.5), phaseScore);
		// Pawn chain.
		whitePawnAttacks = BitParallelMoveSet.getWhitePawnCaptureSet(pos.whitePawns, -1);
		blackPawnAttacks = BitParallelMoveSet.getBlackPawnCaptureSet(pos.blackPawns, -1);
		score += 15*BitOperations.getHammingWeight(whitePawnAttacks & pos.whitePawns);
		score -= 15*BitOperations.getHammingWeight(blackPawnAttacks & pos.blackPawns);
		// Blocked pawns.
		score -= 25*BitOperations.getHammingWeight((pos.whitePawns << 8) & pos.whitePawns);
		score -= 10*BitOperations.getHammingWeight((pos.whitePawns << 16) & pos.whitePawns);
		score -= 5*BitOperations.getHammingWeight((pos.whitePawns << 24) & pos.whitePawns);
		score += 25*BitOperations.getHammingWeight((pos.blackPawns >>> 8) & pos.blackPawns);
		score += 10*BitOperations.getHammingWeight((pos.blackPawns >>> 16) & pos.blackPawns);
		score += 5*BitOperations.getHammingWeight((pos.blackPawns >>> 24) & pos.blackPawns);
		// Isolated and passed pawns.
		whiteFrontSpans = pos.whitePawns;
		whiteFrontSpans |= whiteFrontSpans << 8;
		whiteFrontSpans |= whiteFrontSpans << 16;
		whiteFrontSpans |= whiteFrontSpans << 32;
		whitePawnNeighbours = whiteFrontSpans;
		whiteFrontSpans = whiteFrontSpans | BitParallelMoveSet.getWhitePawnCaptureSet(whiteFrontSpans, -1);
		whitePawnNeighbours |= whitePawnNeighbours >>> 8;
		whitePawnNeighbours |= whitePawnNeighbours >>> 16;
		whitePawnNeighbours |= whitePawnNeighbours >>> 32;
		whitePawnNeighbours = whitePawnNeighbours | ((whitePawnNeighbours >>> 1) & ~File.H.bits) | ((whitePawnNeighbours << 1) & ~File.A.bits);
		blackFrontSpans = pos.blackPawns;
		blackFrontSpans |= blackFrontSpans >>> 8;
		blackFrontSpans |= blackFrontSpans >>> 16;
		blackFrontSpans |= blackFrontSpans >>> 32;
		blackPawnNeighbours = blackFrontSpans;
		blackFrontSpans = blackFrontSpans | BitParallelMoveSet.getBlackPawnCaptureSet(blackFrontSpans, -1);
		blackPawnNeighbours |= blackPawnNeighbours << 8;
		blackPawnNeighbours |= blackPawnNeighbours << 16;
		blackPawnNeighbours |= blackPawnNeighbours << 32;
		blackPawnNeighbours = blackPawnNeighbours | ((blackPawnNeighbours >>> 1) & ~File.H.bits) | ((blackPawnNeighbours << 1) & ~File.A.bits);
		score -= 5*BitOperations.getHammingWeight(~whitePawnNeighbours & pos.whitePawns); // Isolated pawns.
		score += 5*BitOperations.getHammingWeight(~blackPawnNeighbours & pos.blackPawns);
		whitePassers = BitOperations.getHammingWeight(pos.whitePawns & ~((whiteFrontSpans & ~pos.whitePawns) | blackFrontSpans)); // Passed pawns.
		blackPassers = BitOperations.getHammingWeight(pos.blackPawns & ~((blackFrontSpans & ~pos.blackPawns) | whiteFrontSpans));
		score += 35*whitePassers;
		score -= 35*blackPassers;
		// King safety.
		// Pawn shield and pawn storm.
		if (pos.whiteKing == Square.G1.bit || pos.whiteKing == Square.H1.bit) {
			if ((pos.whitePawns & Square.G2.bit) != 0)
				score += 15;
			else if ((pos.whitePawns & Square.G3.bit) != 0)
				score += 5;
			if ((pos.whitePawns & Square.H2.bit) != 0)
				score += 15;
			else if ((pos.whitePawns & Square.H3.bit) != 0)
				score += 5;
			if ((pos.whitePawns & Square.F2.bit) != 0)
				score += 10;
			score -= 15*BitOperations.getHammingWeight((Square.G3.bit | Square.G4.bit | Square.H3.bit | Square.H4.bit) & pos.blackPawns);
			score -= 10*BitOperations.getHammingWeight((Square.F3.bit | Square.F4.bit) & pos.blackPawns);
		}
		else if (pos.whiteKing == Square.A1.bit || pos.whiteKing == Square.B1.bit || pos.whiteKing == Square.C1.bit) {
			if ((pos.whitePawns & Square.A2.bit) != 0)
				score += 15;
			else if ((pos.whitePawns & Square.A3.bit) != 0)
				score += 5;
			if ((pos.whitePawns & Square.B2.bit) != 0)
				score += 15;
			else if ((pos.whitePawns & Square.B3.bit) != 0)
				score += 5;
			if ((pos.whitePawns & Square.C2.bit) != 0)
				score += 10;
			score -= 15*BitOperations.getHammingWeight((Square.A3.bit | Square.A4.bit | Square.B3.bit | Square.B4.bit |
					Square.C3.bit | Square.C4.bit) & pos.blackPawns);
			score -= 5*BitOperations.getHammingWeight((Square.D3.bit | Square.D4.bit) & pos.blackPawns);
		}
		if (pos.blackKing == Square.G8.bit || pos.blackKing == Square.H8.bit) {
			if ((pos.blackPawns & Square.G7.bit) != 0)
				score -= 15;
			else if ((pos.whitePawns & Square.G6.bit) != 0)
				score -= 5;
			if ((pos.blackPawns & Square.H7.bit) != 0)
				score -= 15;
			else if ((pos.whitePawns & Square.H6.bit) != 0)
				score -= 5;
			if ((pos.blackPawns & Square.F7.bit) != 0)
				score -= 10;
			score += 15*BitOperations.getHammingWeight((Square.G5.bit | Square.G6.bit | Square.H5.bit | Square.H6.bit) & pos.whitePawns);
			score += 10*BitOperations.getHammingWeight((Square.F5.bit | Square.F6.bit) & pos.whitePawns);
		}
		else if (pos.blackKing == Square.A8.bit || pos.blackKing == Square.B8.bit || pos.blackKing == Square.C8.bit) {
			if ((pos.blackPawns & Square.A7.bit) != 0)
				score -= 15;
			else if ((pos.whitePawns & Square.A6.bit) != 0)
				score -= 5;
			if ((pos.blackPawns & Square.B7.bit) != 0)
				score -= 15;
			else if ((pos.whitePawns & Square.B6.bit) != 0)
				score -= 5;
			if ((pos.blackPawns & Square.C7.bit) != 0)
				score -= 10;
			score += 15*BitOperations.getHammingWeight((Square.A5.bit | Square.A6.bit | Square.B5.bit | Square.B6.bit |
					Square.C5.bit | Square.C6.bit) & pos.whitePawns);
			score += 5*BitOperations.getHammingWeight((Square.D5.bit | Square.D6.bit) & pos.whitePawns);
		}
		// King-pawn defense and attack.
		whiteKingZone = MoveSetDatabase.getByIndex(whiteKingInd).kingMoveMask;
		score -= 15*BitOperations.getHammingWeight(blackPawnAttacks & whiteKingZone);
		score += 10*BitOperations.getHammingWeight(whitePawnAttacks & whiteKingZone);
		score += 5*BitOperations.getHammingWeight(pos.whitePawns & whiteKingZone);
		blackKingZone = MoveSetDatabase.getByIndex(blackKingInd).kingMoveMask;
		score += 15*BitOperations.getHammingWeight(whitePawnAttacks & blackKingZone);
		score -= 10*BitOperations.getHammingWeight(blackPawnAttacks & blackKingZone);
		score -= 5*BitOperations.getHammingWeight(pos.blackPawns & blackKingZone);
		// King-pawn tropism.
		whitePawns = BitOperations.serialize(pos.whitePawns, numOfWhitePawns);
		for (int i = 0; i < numOfWhitePawns; i++)
			score -= MANHATTAN_DISTANCE[whiteKingInd][whitePawns[i]];
		blackPawns = BitOperations.serialize(pos.blackPawns, numOfBlackPawns);
		for (int i = 0; i < numOfBlackPawns; i++)
			score += MANHATTAN_DISTANCE[blackKingInd][blackPawns[i]];
		return (short)score;
	}
	/**
	 * Rates the chess position from the color to move's point of view. It considers material imbalance, mobility, and king safety.
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
		ByteList whitePieces, blackPieces;
		short pawnScore, baseScore, openingScore, endgameScore, score, extendedScore;
		byte[] bishopSqrArr, offsetBoard;;
		boolean isWhitesTurn;
		ETEntry eE;
		PTEntry pE;
		eE = eT.lookUp(pos.key);
		if (eE != null) {
			eE.generation = hashGen;
			return eE.score;
		}
		score = 0;
		isWhitesTurn = pos.isWhitesTurn;
		whiteKingInd = blackKingInd = -1;
		if (eE == null) {
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
			if (phase >= 234 && numOfWhiteQueens == 0 && numOfBlackQueens == 0 &&
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
			baseScore += (numOfWhiteQueens - numOfBlackQueens)*Material.QUEEN.score;
			baseScore += (numOfWhiteRooks - numOfBlackRooks)*Material.ROOK.score;
			baseScore += (numOfWhiteBishops - numOfBlackBishops)*Material.BISHOP.score;
			baseScore += (numOfWhiteKnights - numOfBlackKnights)*Material.KNIGHT.score;
			whiteKingInd = blackKingInd = -1;
			// Try for hashed pawn score.
			pE = pT.lookUp(pos.pawnKey);
			if (pE != null) {
				pE.generation = hashGen;
				pawnScore = pE.score;
			}
			// Evaluate pawn structure.
			else {
				whiteKingInd = BitOperations.indexOfBit(pos.whiteKing);
				blackKingInd = BitOperations.indexOfBit(pos.blackKing);
				pawnScore = pawnKingStructureScore(pos, whiteKingInd, blackKingInd, phase);
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
			if (score < alpha - LAZY_EVAL_MAR || score > beta + LAZY_EVAL_MAR) {
				return score;
			}
		}
		extendedScore = 0;
		if (whiteKingInd == -1) {
			whiteKingInd = BitOperations.indexOfBit(pos.whiteKing);
			blackKingInd = BitOperations.indexOfBit(pos.blackKing);
		}
		// Coverage
		
		// Piece-king attack and defense
		
		// Piece-king tropism
		// The maximum theoretical tropism value is 111.
		whiteDistToBlackKing = 0;
		whitePieces = BitOperations.serialize(pos.allWhiteOccupied & ~pos.whitePawns);
		while (whitePieces.hasNext())
			whiteDistToBlackKing += CHEBYSHEV_DISTANCE[whitePieces.next()][blackKingInd];
		blackDistToWhiteKing = 0;
		blackPieces = BitOperations.serialize(pos.allBlackOccupied & ~pos.blackPawns);
		while (blackPieces.hasNext())
			blackDistToWhiteKing += CHEBYSHEV_DISTANCE[blackPieces.next()][whiteKingInd];
		extendedScore -= whiteDistToBlackKing;
		extendedScore += blackDistToWhiteKing;
		// Trapped pieces
		// Maximum theoretical value is 40.
		extendedScore -= 5*BitOperations.getHammingWeight((pos.whitePawns << 8) & (pos.allBlackOccupied^pos.blackPawns)); // Stopped pawns.
		extendedScore += 5*BitOperations.getHammingWeight((pos.blackPawns >>> 8) & (pos.allWhiteOccupied^pos.whitePawns));
		if (!isWhitesTurn)
			extendedScore *= -1;
		extendedScore += score;
		eT.insert(new ETEntry(pos.key, extendedScore, NodeType.EXACT.ind, hashGen));
		return extendedScore;
	}
}
