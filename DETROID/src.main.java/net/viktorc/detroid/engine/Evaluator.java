package net.viktorc.detroid.engine;

import net.viktorc.detroid.engine.Bitboard.Diagonal;
import net.viktorc.detroid.engine.Bitboard.File;
import net.viktorc.detroid.engine.Bitboard.Rank;
import net.viktorc.detroid.util.BitOperations;
import net.viktorc.detroid.util.LossyHashTable;

/**
 * A class for evaluating chess positions. It uses an evaluation hash table and a pawn hash table to improve performance. It also offers a static
 * exchange evaluation function.
 * 
 * @author Viktor
 *
 */
final class Evaluator {
	
	// Distance tables for tropism.
	private static final byte[][] MANHATTAN_DISTANCE = new byte[64][64];
	private static final byte[][] CHEBYSHEV_DISTANCE = new byte[64][64];
	
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
				MANHATTAN_DISTANCE[i][j] = (byte) (rankDist + fileDist);
				CHEBYSHEV_DISTANCE[i][j] = (byte) Math.max(rankDist, fileDist);
			}
		}
	}
	
	private final Params params;
	// Evaluation score hash table.
	private final LossyHashTable<ETEntry> eT;
	private final boolean useEt;
	// The sum of the respective weights of pieces for assessing the game phase.
	private final int totalPhaseWeights;
	
	private byte[] pstWpawnOpening;
	private byte[] pstWpawnEndgame;
	private byte[] pstWknightOpening;
	private byte[] pstWknightEndgame;
	private byte[] pstWbishop;
	private byte[] pstWrookOpening;
	private byte[] pstWrookEndgame;
	private byte[] pstWqueen;
	private byte[] pstWkingOpening;
	private byte[] pstWkingEndgame;
	
	private byte[] pstBpawnOpening;
	private byte[] pstBpawnEndgame;
	private byte[] pstBknightOpening;
	private byte[] pstBknightEndgame;
	private byte[] pstBbishop;
	private byte[] pstBrookOpening;
	private byte[] pstBrookEndgame;
	private byte[] pstBqueen;
	private byte[] pstBkingOpening;
	private byte[] pstBkingEndgame;
	
	private byte[][] pstOpening;
	private byte[][] pstEndgame;
	
	/**
	 * Initializes a chess position evaluator.
	 * 
	 * @param params
	 * @param evalTable
	 * @param pawnTable
	 */
	Evaluator(Params params, LossyHashTable<ETEntry> evalTable) {
		this.params = params;
		totalPhaseWeights = 4*(params.knightPhaseWeight + params.bishopPhaseWeight + params.rookPhaseWeight) + 2*params.queenPhaseWeight;
		initPieceSquareArrays();
		eT = evalTable;
		useEt = eT != null;
	}
	/**
	 * Initializes the piece square arrays with the correct order of values.
	 */
	private void initPieceSquareArrays() {
		int c1, c2;
		byte[] pstPawnOpening = params.getPstPawnOpening();
		byte[] pstPawnEndgame = params.getPstPawnEndgame();
		byte[] pstKnightOpening = params.getPstKnightOpening();
		byte[] pstKnightEndgame = params.getPstKnightEndgame();
		byte[] pstBishop = params.getPstBishop();
		byte[] pstRookOpening = params.getPstRookOpening();
		byte[] pstRookEndgame = params.getPstRookEndgame();
		byte[] pstQueen = params.getPstQueen();
		byte[] pstKingOpening = params.getPstKingOpening();
		byte[] pstKingEndgame = params.getPstKingEndgame();
		pstWpawnOpening = new byte[64];
		pstWpawnEndgame = new byte[64];
		pstWknightOpening = new byte[64];
		pstWknightEndgame = new byte[64];
		pstWbishop = new byte[64];
		pstWrookOpening = new byte[64];
		pstWrookEndgame = new byte[64];
		pstWqueen = new byte[64];
		pstWkingOpening = new byte[64];
		pstWkingEndgame = new byte[64];
		pstBpawnOpening = new byte[64];
		pstBpawnEndgame = new byte[64];
		pstBknightOpening = new byte[64];
		pstBknightEndgame = new byte[64];
		pstBbishop = new byte[64];
		pstBrookOpening = new byte[64];
		pstBrookEndgame = new byte[64];
		pstBqueen = new byte[64];
		pstBkingOpening = new byte[64];
		pstBkingEndgame = new byte[64];
		// Due to the reversed order of the rows in the definition of the white piece-square tables,
		// they are just right for black with negated values.
		for (int i = 0; i < 64; i++) {
			pstBpawnOpening[i] = (byte) -pstPawnOpening[i];
			pstBpawnEndgame[i] = (byte) -pstPawnEndgame[i];
			pstBknightOpening[i] = (byte) -pstKnightOpening[i];
			pstBknightEndgame[i] = (byte) -pstKnightEndgame[i];
			pstBbishop[i] = (byte) -pstBishop[i];
			pstBrookOpening[i] = (byte) -pstRookOpening[i];
			pstBrookEndgame[i] = (byte) -pstRookEndgame[i];
			pstBqueen[i] = (byte) -pstQueen[i];
			pstBkingOpening[i] = (byte) -pstKingOpening[i];
			pstBkingEndgame[i] = (byte) -pstKingEndgame[i];
		}
		// To get the right values for the white piece-square tables, we vertically mirror and negate the ones for black
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				c1 = i*8 + j;
				c2 = ((7 - i)*8) + j;
				pstWpawnOpening[c1] = (byte) -pstBpawnOpening[c2];
				pstWpawnEndgame[c1] = (byte) -pstBpawnEndgame[c2];
				pstWknightOpening[c1] = (byte) -pstBknightOpening[c2];
				pstWknightEndgame[c1] = (byte) -pstBknightEndgame[c2];
				pstWbishop[c1] = (byte) -pstBbishop[c2];
				pstWrookOpening[c1] = (byte) -pstBrookOpening[c2];
				pstWrookEndgame[c1] = (byte) -pstBrookEndgame[c2];
				pstWqueen[c1] = (byte) -pstBqueen[c2];
				pstWkingOpening[c1] = (byte) -pstBkingOpening[c2];
				pstWkingEndgame[c1] = (byte) -pstBkingEndgame[c2];
			}
		}
		// Set the opening and endgame arrays of piece square tables.
		pstOpening = new byte[][] { pstWkingOpening, pstWqueen, pstWrookOpening, pstWbishop, pstWknightOpening, pstWpawnOpening,
			pstBkingOpening, pstBqueen, pstBrookOpening, pstBbishop, pstBknightOpening, pstBpawnOpening };
		pstEndgame = new byte[][] { pstWkingEndgame, pstWqueen, pstWrookEndgame, pstWbishop, pstWknightEndgame, pstWpawnEndgame,
			pstBkingEndgame, pstBqueen, pstBrookEndgame, pstBbishop, pstBknightEndgame, pstBpawnEndgame };
	}
	/**
	 * Returns the value of a piece type defined by a piece index according to {@link #engine.Piece Piece}.
	 * 
	 * @param pieceInd A piece index according to {@link #engine.Piece Piece}.
	 * @return The value of the piece type.
	 */
	short materialValueByPieceInd(int pieceInd) {
		if (pieceInd == Piece.W_KING.ind) return params.kingPhaseWeight;
		else if (pieceInd == Piece.W_QUEEN.ind) return params.queenValue;
		else if (pieceInd == Piece.W_ROOK.ind) return params.rookValue;
		else if (pieceInd == Piece.W_BISHOP.ind) return params.bishopValue;
		else if (pieceInd == Piece.W_KNIGHT.ind) return params.knightValue;
		else if (pieceInd == Piece.W_PAWN.ind) return params.pawnValue;
		else if (pieceInd == Piece.B_KING.ind) return params.kingValue;
		else if (pieceInd == Piece.B_QUEEN.ind) return params.queenValue;
		else if (pieceInd == Piece.B_ROOK.ind) return params.rookValue;
		else if (pieceInd == Piece.B_BISHOP.ind) return params.bishopValue;
		else if (pieceInd == Piece.B_KNIGHT.ind) return params.knightValue;
		else if (pieceInd == Piece.B_PAWN.ind) return params.pawnValue;
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
	short SEE(Position pos, Move move) {
		short victimVal, attackerVal, prevAttackerVal;
		long attackers, bpAttack, rkAttack, occupied;
		boolean whitesTurn;
		short[] gains;
		int i;
		MoveSetDatabase dB;
		victimVal = materialValueByPieceInd(move.capturedPiece);
		// If the captor was a king, return the captured piece's value as capturing the king would be illegal.
		if (move.movedPiece == Piece.W_KING.ind || move.movedPiece == Piece.B_KING.ind)
			return victimVal;
		i = 0;
		gains = new short[32];
		gains[i] = victimVal;
		// In case the move is a promotion.
		if (move.type >= MoveType.PROMOTION_TO_QUEEN.ind) {
			if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
				gains[i] += params.queenValue - params.pawnValue;
				attackerVal = params.queenValue;
			} else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
				gains[i] += params.rookValue - params.pawnValue;
				attackerVal = params.rookValue;
			} else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
				gains[i] += params.bishopValue - params.pawnValue;
				attackerVal = params.bishopValue;
			} else { // PROMOTION_TO_KNIGHT
				gains[i] += params.knightValue - params.pawnValue;
				attackerVal = params.knightValue;
			}
		} else
			attackerVal = materialValueByPieceInd(move.movedPiece);
		occupied = (pos.allOccupied^(1L << move.from));
		whitesTurn = pos.isWhitesTurn;
		dB = MoveSetDatabase.getByIndex(move.to);
		do {
			i++;
			gains[i] = (short) (attackerVal - gains[i - 1]);
			prevAttackerVal = attackerVal;
			attackerVal = 0;
			whitesTurn = !whitesTurn;
			if (whitesTurn) {
				if ((attackers = dB.getBlackPawnCaptureSet(pos.whitePawns) & occupied) != 0)
					attackerVal = params.pawnValue;
				else if ((attackers = dB.getKnightMoveSet(pos.whiteKnights) & occupied) != 0)
					attackerVal = params.knightValue;
				else if ((attackers = (bpAttack = dB.getBishopMoveSet(occupied, occupied)) & pos.whiteBishops) != 0)
					attackerVal = params.bishopValue;
				else if ((attackers = (rkAttack = dB.getRookMoveSet(occupied, occupied)) & pos.whiteRooks) != 0)
					attackerVal = params.rookValue;
				else if ((attackers = (bpAttack | rkAttack) & pos.whiteQueens) != 0)
					attackerVal = params.queenValue;
				else if ((attackers = dB.getKingMoveSet(pos.whiteKing)) != 0)
					attackerVal = params.kingValue;
				else
					break;
			} else {
				if ((attackers = dB.getWhitePawnCaptureSet(pos.blackPawns) & occupied) != 0) {
					attackerVal = params.pawnValue;
				} else if ((attackers = dB.getKnightMoveSet(pos.blackKnights) & occupied) != 0)
					attackerVal = params.knightValue;
				else if ((attackers = (bpAttack = dB.getBishopMoveSet(occupied, occupied)) & pos.blackBishops) != 0)
					attackerVal = params.bishopValue;
				else if ((attackers = (rkAttack = dB.getRookMoveSet(occupied, occupied)) & pos.blackRooks) != 0)
					attackerVal = params.rookValue;
				else if ((attackers = (bpAttack | rkAttack) & pos.blackQueens) != 0)
					attackerVal = params.queenValue;
				else if ((attackers = dB.getKingMoveSet(pos.blackKing)) != 0)
					attackerVal = params.kingValue;
				else
					break;
			}
			// If the previous attacker was a king and the side to move can attack it, the exchange is over.
			if (prevAttackerVal == params.kingValue)
				break;
			// Prune if engaging in further captures would result in material loss.
			if (Math.max(gains[i], -gains[i - 1]) < 0)
				break;
			bpAttack = rkAttack = 0;
			// Simulate move.
			occupied ^= BitOperations.getLSBit(attackers);
		} while (true);
		while (--i > 0)
			gains[i - 1] = (short) Math.min(-gains[i], gains[i - 1]);
		return gains[0];
	}
	/**
	 * Returns whether the position is a case of insufficient material.
	 * 
	 * @param pos
	 * @return
	 */
	static boolean isMaterialInsufficient(Position pos) {
		int numOfWhiteKnights, numOfBlackKnights, numOfAllPieces;
		int bishopColor;
		byte[] bishopSqrArr;
		if (pos.whitePawns != 0 || pos.blackPawns != 0 || pos.whiteRooks != 0 || pos.blackRooks != 0 ||
				pos.whiteQueens != 0 || pos.blackQueens != 0)
			return false;
		numOfWhiteKnights = BitOperations.hammingWeight(pos.whiteKnights);
		numOfBlackKnights = BitOperations.hammingWeight(pos.blackKnights);
		numOfAllPieces = BitOperations.hammingWeight(pos.allOccupied);
		if (numOfAllPieces == 2 || numOfAllPieces == 3)
			return true;
		if (numOfAllPieces >= 4 && numOfWhiteKnights == 0 && numOfBlackKnights == 0) {
			bishopSqrArr = BitOperations.serialize(pos.whiteBishops | pos.blackBishops);
			bishopColor = Diagonal.getBySquareIndex(bishopSqrArr[0]).ordinal()%2;
			for (int i = 1; i < bishopSqrArr.length; i++) {
				if (Diagonal.getBySquareIndex(bishopSqrArr[i]).ordinal()%2 != bishopColor)
					return false;
			}
			return true;
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
		int phase = totalPhaseWeights - (numOfQueens*params.queenPhaseWeight + numOfRooks*params.rookPhaseWeight
					+ numOfBishops*params.bishopPhaseWeight + numOfKnights*params.knightPhaseWeight);
		return (phase*params.gamePhaseEndgameUpper + totalPhaseWeights/2)/totalPhaseWeights;
	}
	/**
	 * Returns an estimation of the phase in which the current game is based on the given position.
	 * 
	 * @param pos
	 * @return
	 */
	int phaseScore(Position pos) {
		int numOfQueens, numOfRooks, numOfBishops, numOfKnights;
		numOfQueens = BitOperations.hammingWeight(pos.whiteQueens | pos.blackQueens);
		numOfRooks = BitOperations.hammingWeight(pos.whiteRooks | pos.blackRooks);
		numOfBishops = BitOperations.hammingWeight(pos.whiteBishops | pos.blackBishops);
		numOfKnights = BitOperations.hammingWeight(pos.whiteKnights | pos.blackKnights);
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
		return (openingEval*(params.gamePhaseEndgameUpper - phaseScore) + endGameEval*phaseScore)/params.gamePhaseEndgameUpper;
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
		int whiteKingInd, blackKingInd;
		int pawnInd;
		long whitePawnAttacks, blackPawnAttacks;
		long whiteAdvanceSpans, blackAdvanceSpans;
		long whiteAttackSpans, blackAttackSpans;
		long whiteFrontSpans, blackFrontSpans;
		long whitePassedPawns, blackPassedPawns;
		long whiteOpenBackwardPawns, blackOpenBackwardPawns;
		long whiteKingWhitePawnTropism, whiteKingBlackPawnTropism;
		long blackKingWhitePawnTropism, blackKingBlackPawnTropism;
		long whitePawnSet, blackPawnSet;
		long pawn;
		score = 0;
		// Base pawn material score.
		numOfWhitePawns = BitOperations.hammingWeight(whitePawns);
		numOfBlackPawns = BitOperations.hammingWeight(blackPawns);
		score += (numOfWhitePawns - numOfBlackPawns)*params.pawnValue;
		// Pawn attacks.
		whitePawnAttacks = MultiMoveSets.whitePawnCaptureSets(whitePawns, Bitboard.FULL_BOARD);
		blackPawnAttacks = MultiMoveSets.blackPawnCaptureSets(blackPawns, Bitboard.FULL_BOARD);
		// Blocked pawns.
		score -= params.blockedPawnWeight1*BitOperations.hammingWeight((whitePawns << 8) & whitePawns);
		score -= params.blockedPawnWeight2*BitOperations.hammingWeight((whitePawns << 16) & whitePawns);
		score -= params.blockedPawnWeight3*BitOperations.hammingWeight((whitePawns << 24) & whitePawns);
		score += params.blockedPawnWeight1*BitOperations.hammingWeight((blackPawns >>> 8) & blackPawns);
		score += params.blockedPawnWeight2*BitOperations.hammingWeight((blackPawns >>> 16) & blackPawns);
		score += params.blockedPawnWeight3*BitOperations.hammingWeight((blackPawns >>> 24) & blackPawns);
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
		whitePassedPawns = whitePawns & ~blackFrontSpans;
		blackPassedPawns = blackPawns & ~whiteFrontSpans;
		score += params.passedPawnWeight*BitOperations.hammingWeight(whitePassedPawns);
		score -= params.passedPawnWeight*BitOperations.hammingWeight(blackPassedPawns);
		// Backward pawns.
		whiteOpenBackwardPawns = (whitePawns & (blackPawnAttacks >>> 8) & ~(whiteAttackSpans | (whiteAttackSpans >>> 8))) & ~blackAdvanceSpans;
		blackOpenBackwardPawns = (blackPawns & (whitePawnAttacks << 8) & ~(blackAttackSpans | (blackAttackSpans << 8))) & ~whiteAdvanceSpans;
		score -= params.openBackwardPawnWeight*BitOperations.hammingWeight(whiteOpenBackwardPawns);
		score += params.openBackwardPawnWeight*BitOperations.hammingWeight(blackOpenBackwardPawns);
		// King-pawn tropism.
		whiteKingInd = BitOperations.indexOfBit(whiteKing);
		blackKingInd = BitOperations.indexOfBit(blackKing);
		whitePawnSet = whitePawns;
		whiteKingWhitePawnTropism = blackKingWhitePawnTropism = 0;
		while (whitePawnSet != 0) {
			pawn = BitOperations.getLSBit(whitePawnSet);
			pawnInd = BitOperations.indexOfBit(pawn);
			if ((whitePassedPawns & pawn) != 0)
				blackKingWhitePawnTropism += params.kingOpponentPassedPawnTropismWeight*MANHATTAN_DISTANCE[blackKingInd][pawnInd];
			else if ((whiteOpenBackwardPawns & pawn) != 0) {
				whiteKingWhitePawnTropism += params.kingFriendlyOpenBackwardPawnTropismWeight*MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
				blackKingWhitePawnTropism += params.kingOpponentOpenBackwardPawnTropismWeight*MANHATTAN_DISTANCE[blackKingInd][pawnInd];
			} else {
				whiteKingWhitePawnTropism += params.kingFriendlyNormalPawnTropismWeight*MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
				blackKingWhitePawnTropism += params.kingOpponentNormalPawnTropismWeight*MANHATTAN_DISTANCE[blackKingInd][pawnInd];
			}
			whitePawnSet = BitOperations.resetLSBit(whitePawnSet);
		}
		blackPawnSet = blackPawns;
		whiteKingBlackPawnTropism = blackKingBlackPawnTropism = 0;
		while (blackPawnSet != 0) {
			pawn = BitOperations.getLSBit(blackPawnSet);
			pawnInd = BitOperations.indexOfBit(pawn);
			if ((blackPassedPawns & pawn) != 0)
				whiteKingBlackPawnTropism += params.kingOpponentPassedPawnTropismWeight*MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
			else if ((blackOpenBackwardPawns & pawn) != 0) {
				whiteKingBlackPawnTropism += params.kingOpponentOpenBackwardPawnTropismWeight*MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
				blackKingBlackPawnTropism += params.kingFriendlyOpenBackwardPawnTropismWeight*MANHATTAN_DISTANCE[blackKingInd][pawnInd];
			} else {
				whiteKingBlackPawnTropism += params.kingOpponentNormalPawnTropismWeight*MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
				blackKingBlackPawnTropism += params.kingFriendlyNormalPawnTropismWeight*MANHATTAN_DISTANCE[blackKingInd][pawnInd];
			}
			blackPawnSet = BitOperations.resetLSBit(blackPawnSet);
		}
		score -= numOfWhitePawns != 0 ? (whiteKingWhitePawnTropism - blackKingWhitePawnTropism)/numOfWhitePawns : 0;
		score += numOfBlackPawns != 0 ? (blackKingBlackPawnTropism - whiteKingBlackPawnTropism)/numOfBlackPawns : 0;
		return (short) score;
	}
	/**
	 * A static evaluation of the chess position from the color to move's point of view. It considers material imbalance, coverage, pawn structure,
	 * king safety, king tropism, king mobility, trapped pieces, etc.
	 * 
	 * @param pos
	 * @param hashGen
	 * @param alpha
	 * @param beta
	 * @return
	 */
	int score(Position pos, byte hashGen, int alpha, int beta) {
		final boolean isWhitesTurn = pos.isWhitesTurn;
		boolean allSameColor;
		byte numOfWhiteQueens, numOfWhiteRooks, numOfWhiteBishops, numOfWhiteKnights;
		byte numOfBlackQueens, numOfBlackRooks, numOfBlackBishops, numOfBlackKnights;
		byte numOfAllPieces;
		byte pieceInd;
		byte pinner;
		byte checker;
		short openingScore, endgameScore, score, tempScore;
		int bishopSqrColor;
		int square;
		int phase;
		int whiteKingInd, blackKingInd;
		long piece;
		long temp;
		long pinLine;
		long bishopSet;
		long whitePieceSet, blackPieceSet;
		long pinnedPieceMoveSetRestriction, checkMoveSetRestriction;
		long whitePinnedPieces, blackPinnedPieces;
		long whitePinningPieces, blackPinningPieces;
		long whitePawnAttacks, blackPawnAttacks;
		long whiteQueenKingTropism, blackQueenKingTropism;
		byte[] offsetBoard;
		long[] whitePinLines, blackPinLines;
		ETEntry eE;
		score = 0;
		// Probe evaluation hash table.
		if (useEt && (eE = eT.get(pos.key)) != null) {
			eE.generation = hashGen;
			score = eE.score;
			// If the entry is exact or would also trigger lazy eval within the current alpha-beta context, return the score.
			if (eE.isExact || score >= beta + params.lazyEvalMar || score <= alpha - params.lazyEvalMar)
				return eE.score;
		}
		// In case of no hash hit, calculate the base score from scratch.
		else {
			numOfWhiteQueens = BitOperations.hammingWeight(pos.whiteQueens);
			numOfWhiteRooks = BitOperations.hammingWeight(pos.whiteRooks);
			numOfWhiteBishops = BitOperations.hammingWeight(pos.whiteBishops);
			numOfWhiteKnights = BitOperations.hammingWeight(pos.whiteKnights);
			numOfBlackQueens = BitOperations.hammingWeight(pos.blackQueens);
			numOfBlackRooks = BitOperations.hammingWeight(pos.blackRooks);
			numOfBlackBishops = BitOperations.hammingWeight(pos.blackBishops);
			numOfBlackKnights = BitOperations.hammingWeight(pos.blackKnights);
			// Check for insufficient material. Only consider the widely acknowledged scenarios without blocked position testing.
			if (pos.whitePawns == 0 && pos.blackPawns == 0 && numOfWhiteRooks == 0 && numOfBlackRooks == 0 &&
					numOfWhiteQueens == 0 && numOfBlackQueens == 0) {
				numOfAllPieces = BitOperations.hammingWeight(pos.allOccupied);
				if (numOfAllPieces == 2 || numOfAllPieces == 3)
					return Termination.INSUFFICIENT_MATERIAL.score;
				if (numOfAllPieces >= 4 && numOfWhiteKnights == 0 && numOfBlackKnights == 0) {
					allSameColor = true;
					bishopSet = pos.whiteBishops | pos.blackBishops;
					bishopSqrColor = Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(bishopSet)).ordinal()%2;
					bishopSet = BitOperations.resetLSBit(bishopSet);
					while (bishopSet != 0) {
						if (Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(bishopSet)).ordinal()%2 != bishopSqrColor) {
							allSameColor = false;
							break;
						}
						bishopSet = BitOperations.resetLSBit(bishopSet);
					}
					if (allSameColor)
						return Termination.INSUFFICIENT_MATERIAL.score;
				}
			}
			// Phase score for tapered evaluation.
			phase = phaseScore(numOfWhiteQueens + numOfBlackQueens, numOfWhiteRooks + numOfBlackRooks, numOfWhiteBishops + numOfBlackBishops,
					numOfWhiteKnights + numOfBlackKnights);
			// Basic material score.
			score += (numOfWhiteQueens - numOfBlackQueens)*params.queenValue;
			score += (numOfWhiteRooks - numOfBlackRooks)*params.rookValue;
			score += (numOfWhiteBishops - numOfBlackBishops)*params.bishopValue;
			score += (numOfWhiteKnights - numOfBlackKnights)*params.knightValue;
			// Pawn structure.
			score += pawnKingStructureScore(pos.whiteKing, pos.blackKing, pos.whitePawns, pos.blackPawns);
			// Piece-square scores.
			openingScore = endgameScore = 0;
			offsetBoard = pos.offsetBoard;
			for (int i = 0; i < offsetBoard.length; i++) {
				square = offsetBoard[i] - 1;
				if (square < Piece.NULL.ind)
					continue;
				openingScore += pstOpening[square][i];
				endgameScore += pstEndgame[square][i];
			}
			score += (short) taperedEvalScore(openingScore, endgameScore, phase);
			// Bishop pair advantage.
			if (numOfWhiteBishops >= 2 && (BitOperations.indexOfLSBit(pos.whiteBishops) -
					BitOperations.indexOfLSBit(BitOperations.resetLSBit(pos.whiteBishops))%2 != 0))
				score += params.bishopPairAdvantage;
			if (numOfBlackBishops >= 2 && (BitOperations.indexOfLSBit(pos.blackBishops) -
					BitOperations.indexOfLSBit(BitOperations.resetLSBit(pos.blackBishops))%2 != 0))
				score -= params.bishopPairAdvantage;
			// Tempo advantage.
			score += (isWhitesTurn ? params.tempoAdvantage : -params.tempoAdvantage);
			// Non-exact score hashing.
			tempScore = (short) (isWhitesTurn ? score : -score);
			if (useEt && (tempScore <= alpha - params.lazyEvalMar || tempScore >= beta + params.lazyEvalMar)) {
				eT.put(new ETEntry(pos.key, tempScore, false, hashGen));
				return tempScore;
			}
		}
		// Pinned pieces.
		whiteKingInd = BitOperations.indexOfBit(pos.whiteKing);
		blackKingInd = BitOperations.indexOfBit(pos.blackKing);
		whitePinningPieces = pos.getPinningPieces(true);
		blackPinningPieces = pos.getPinningPieces(false);
		whitePinLines = new long[BitOperations.hammingWeight(whitePinningPieces)];
		temp = whitePinningPieces;
		blackPinnedPieces = 0;
		for (int i = 0; i < whitePinLines.length; i++) {
			pinner = BitOperations.indexOfLSBit(temp);
			pinLine = Bitboard.LINE_SEGMENTS[blackKingInd][pinner] | (1L << pinner);
			whitePinLines[i] = pinLine;
			blackPinnedPieces |= (pinLine & pos.allBlackOccupied);
			temp = BitOperations.resetLSBit(temp);
		}
		blackPinLines = new long[BitOperations.hammingWeight(blackPinningPieces)];
		temp = blackPinningPieces;
		whitePinnedPieces = 0;
		for (int i = 0; i < blackPinLines.length; i++) {
			pinner = BitOperations.indexOfLSBit(temp);
			pinLine = Bitboard.LINE_SEGMENTS[whiteKingInd][pinner] | (1L << pinner);
			blackPinLines[i] = pinLine;
			whitePinnedPieces |= (pinLine & pos.allWhiteOccupied);
			temp = BitOperations.resetLSBit(temp);
		}
		score -= params.pinnedQueenWeight*BitOperations.hammingWeight(pos.whiteQueens & whitePinnedPieces);
		score -= params.pinnedRookWeight*BitOperations.hammingWeight(pos.whiteRooks & whitePinnedPieces);
		score -= params.pinnedBishopWeight*BitOperations.hammingWeight(pos.whiteBishops & whitePinnedPieces);
		score -= params.pinnedKnightWeight*BitOperations.hammingWeight(pos.whiteKnights & whitePinnedPieces);
		score += params.pinnedQueenWeight*BitOperations.hammingWeight(pos.blackQueens & blackPinnedPieces);
		score -= params.pinnedRookWeight*BitOperations.hammingWeight(pos.blackRooks & blackPinnedPieces);
		score += params.pinnedBishopWeight*BitOperations.hammingWeight(pos.blackBishops & blackPinnedPieces);
		score += params.pinnedKnightWeight*BitOperations.hammingWeight(pos.blackKnights & blackPinnedPieces);
		// Stopped pawns.
		score -= params.stoppedPawnWeight*BitOperations.hammingWeight((pos.whitePawns << 8) & (pos.allBlackOccupied^pos.blackPawns));
		score += params.stoppedPawnWeight*BitOperations.hammingWeight((pos.blackPawns >>> 8) & (pos.allWhiteOccupied^pos.whitePawns));
		// Pawn defense.
		whitePawnAttacks = MultiMoveSets.whitePawnCaptureSets(pos.whitePawns & ~whitePinnedPieces, Bitboard.FULL_BOARD);
		blackPawnAttacks = MultiMoveSets.blackPawnCaptureSets(pos.blackPawns & ~blackPinnedPieces, Bitboard.FULL_BOARD);
		score += params.pawnDefendedRookWeight*BitOperations.hammingWeight(whitePawnAttacks & pos.whiteRooks);
		score += params.pawnDefendedBishopWeight*BitOperations.hammingWeight(whitePawnAttacks & pos.whiteBishops);
		score += params.pawnDefendedKnightWeight*BitOperations.hammingWeight(whitePawnAttacks & pos.whiteKnights);
		score += params.pawnDefendedPawnWeight*BitOperations.hammingWeight(whitePawnAttacks & pos.whitePawns);
		score -= params.pawnDefendedRookWeight*BitOperations.hammingWeight(blackPawnAttacks & pos.blackRooks);
		score -= params.pawnDefendedBishopWeight*BitOperations.hammingWeight(blackPawnAttacks & pos.blackBishops);
		score -= params.pawnDefendedKnightWeight*BitOperations.hammingWeight(blackPawnAttacks & pos.blackKnights);
		score -= params.pawnDefendedPawnWeight*BitOperations.hammingWeight(blackPawnAttacks & pos.blackPawns);
		// Pawn attack.
		score += params.pawnAttackedQueenWeight*BitOperations.hammingWeight(whitePawnAttacks & pos.blackQueens);
		score += params.pawnAttackedRookWeight*BitOperations.hammingWeight(whitePawnAttacks & pos.blackRooks);
		score += params.pawnAttackedBishopWeight*BitOperations.hammingWeight(whitePawnAttacks & pos.blackBishops);
		score += params.pawnAttackedKnightWeight*BitOperations.hammingWeight(whitePawnAttacks & pos.blackKnights);
		score += params.pawnAttackedPawnWeight*BitOperations.hammingWeight(whitePawnAttacks & pos.blackPawns);
		score -= params.pawnAttackedQueenWeight*BitOperations.hammingWeight(blackPawnAttacks & pos.whiteQueens);
		score -= params.pawnAttackedRookWeight*BitOperations.hammingWeight(blackPawnAttacks & pos.whiteRooks);
		score -= params.pawnAttackedBishopWeight*BitOperations.hammingWeight(blackPawnAttacks & pos.whiteBishops);
		score -= params.pawnAttackedKnightWeight*BitOperations.hammingWeight(blackPawnAttacks & pos.whiteKnights);
		score -= params.pawnAttackedPawnWeight*BitOperations.hammingWeight(blackPawnAttacks & pos.whitePawns);
		// En passant advantage.
		if (pos.enPassantRights != EnPassantRights.NONE.ind) {
			if (isWhitesTurn) {
				if ((whitePawnAttacks & (1L << (pos.enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND))) != 0)
					score += params.liveEpAdvantage;
			} else {
				if ((blackPawnAttacks & (1L << (pos.enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND))) != 0)
					score += params.liveEpAdvantage;
			}
		}
		// Iterate over pieces to assess their mobility and distance from the opponent's king.
		whitePieceSet = pos.allWhiteOccupied^pos.whiteKing^pos.whitePawns;
		blackPieceSet = pos.allBlackOccupied^pos.blackKing^pos.blackPawns;
		numOfWhiteQueens = numOfBlackQueens = 0;
		whiteQueenKingTropism = blackQueenKingTropism = 0;
		checkMoveSetRestriction = Bitboard.FULL_BOARD;
		if (pos.isInCheck) {
			if (BitOperations.resetLSBit(pos.checkers) != 0)
				checkMoveSetRestriction = 0;
			else {
				checker = BitOperations.indexOfLSBit(pos.checkers);
				checkMoveSetRestriction = Bitboard.LINE_SEGMENTS[(isWhitesTurn ? whiteKingInd : blackKingInd)][checker] | (1L << checker);
			}
		}
		while (whitePieceSet != 0) {
			piece = BitOperations.getLSBit(whitePieceSet);
			pinnedPieceMoveSetRestriction = Bitboard.FULL_BOARD;
			if ((piece & whitePinnedPieces) != 0) {
				for (long n : blackPinLines) {
					if ((piece & n) != 0) {
						pinnedPieceMoveSetRestriction = n;
						break;
					}
				}
			}
			pieceInd = BitOperations.indexOfBit(piece);
			if (pos.offsetBoard[pieceInd] == Piece.W_QUEEN.ind) {
				numOfWhiteQueens++;
				whiteQueenKingTropism += CHEBYSHEV_DISTANCE[pieceInd][blackKingInd];
			} else if (pos.offsetBoard[pieceInd] == Piece.W_ROOK.ind)
				score += BitOperations.hammingWeight((MoveSetDatabase.getByIndex(pieceInd).getRookMoveSet(pos.allNonWhiteOccupied, pos.allOccupied) &
						pinnedPieceMoveSetRestriction) & checkMoveSetRestriction)*params.rookMobilityWeight;
			else if (pos.offsetBoard[pieceInd] == Piece.W_BISHOP.ind)
				score += BitOperations.hammingWeight((MoveSetDatabase.getByIndex(pieceInd).getBishopMoveSet(pos.allNonWhiteOccupied, pos.allOccupied) &
						pinnedPieceMoveSetRestriction) & checkMoveSetRestriction)*params.bishopMobilityWeight;
			else // W_KNIGHT
				score += BitOperations.hammingWeight((MoveSetDatabase.getByIndex(pieceInd).getKnightMoveSet(pos.allNonWhiteOccupied) &
						pinnedPieceMoveSetRestriction) & checkMoveSetRestriction)*params.knightMobilityWeight;
			whitePieceSet = BitOperations.resetLSBit(whitePieceSet);
		}
		while (blackPieceSet != 0) {
			piece = BitOperations.getLSBit(blackPieceSet);
			pinnedPieceMoveSetRestriction = Bitboard.FULL_BOARD;
			if ((piece & blackPinnedPieces) != 0) {
				for (long n : whitePinLines) {
					if ((piece & n) != 0) {
						pinnedPieceMoveSetRestriction = n;
						break;
					}
				}
			}
			pieceInd = BitOperations.indexOfBit(piece);
			if (pos.offsetBoard[pieceInd] == Piece.B_QUEEN.ind) {
				numOfBlackQueens++;
				blackQueenKingTropism += CHEBYSHEV_DISTANCE[pieceInd][whiteKingInd];
			} else if (pos.offsetBoard[pieceInd] == Piece.B_ROOK.ind)
				score -= BitOperations.hammingWeight((MoveSetDatabase.getByIndex(pieceInd).getRookMoveSet(pos.allNonBlackOccupied, pos.allOccupied) &
						pinnedPieceMoveSetRestriction) & checkMoveSetRestriction)*params.rookMobilityWeight;
			else if (pos.offsetBoard[pieceInd] == Piece.B_BISHOP.ind)
				score -= BitOperations.hammingWeight((MoveSetDatabase.getByIndex(pieceInd).getBishopMoveSet(pos.allNonBlackOccupied, pos.allOccupied) &
						pinnedPieceMoveSetRestriction) & checkMoveSetRestriction)*params.bishopMobilityWeight;
			else // B_KNIGHT
				score -= BitOperations.hammingWeight((MoveSetDatabase.getByIndex(pieceInd).getKnightMoveSet(pos.allNonBlackOccupied) &
						pinnedPieceMoveSetRestriction) & checkMoveSetRestriction)*params.knightMobilityWeight;
			blackPieceSet = BitOperations.resetLSBit(blackPieceSet);
		}
		// Piece-king tropism.
		score -= numOfWhiteQueens != 0 ? params.queenKingTropismWeight*whiteQueenKingTropism/numOfWhiteQueens : 0;
		score += numOfBlackQueens != 0 ? params.queenKingTropismWeight*blackQueenKingTropism/numOfBlackQueens : 0;
		score *= (isWhitesTurn ? 1 : -1);
		if (useEt)
			eT.put(new ETEntry(pos.key, score, true, hashGen));
		return score;
	}
	
}
