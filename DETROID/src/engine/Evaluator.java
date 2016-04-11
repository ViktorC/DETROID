package engine;

import engine.Board.Diagonal;
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
	
	private final static int TOTAL_PHASE_WEIGHTS = 4*(Material.KNIGHT.phaseWeight + Material.BISHOP.phaseWeight +
			Material.ROOK.phaseWeight) + 2*Material.QUEEN.phaseWeight;
	
	private final static int LAZY_EVAL_MAR = 2*Material.PAWN.score;
	
	// Piece-square tables based on and extending Tomasz Michniewski's "Unified Evaluation".
	private final static byte[] PST_W_PAWN_OPENING =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		  50, 50, 50, 50, 50, 50, 50, 50,
		  10, 10, 20, 30, 30, 20, 10, 10,
		   5,  5, 10, 30, 30, 10,  5,  5,
		   0, 10,  5, 40, 50,  5, 10,  0,
		   0,  5,  5, 50, 40,  5,  5,  0,
		   5, 10, 10,-25,-25, 10, 10,  5,
		   0,  0,  0,  0,  0,  0,  0,  0};
	private final static byte[] PST_W_PAWN_ENDGAME =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		  50, 50, 50, 50, 50, 50, 50, 50,
		  10, 10, 20, 30, 30, 20, 10, 10,
		   5,  5, 10, 25, 25, 10,  5,  5,
		   0,  0,  0, 20, 30,  0,  0,  0,
		   5, -5,-10, 20, 10,-10, -5,  5,
		   5, 10, 10,-20,-20, 10, 10,  5,
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
		 -40,-20,  0,  0,  0,  0,-20,-40,
		 -30,  0, 10, 15, 15, 10,  0,-30,
		 -30,  5, 15, 20, 20, 15,  5,-30,
		 -30,  0, 15, 20, 20, 15,  0,-30,
		 -30,  5, 10, 15, 15, 10,  5,-30,
		 -40,-20,  0,  5,  5,  0,-20,-40,
		 -50,-40,-30,-30,-30,-30,-40,-50};
	private final static byte[] PST_W_BISHOP =
		{-20,-10,-10,-10,-10,-10,-10,-20,
		 -10,  0,  0,  0,  0,  0,  0,-10,
		 -10,  0,  5, 10, 10,  5,  0,-10,
		 -10,  5,  5, 10, 10,  5,  5,-10,
		 -10,  0, 10, 10, 10, 10,  0,-10,
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
		  20, -5,  0, 25, 25,  0, -5, 20};
	private final static byte[] PST_W_ROOK_ENDGAME =
		{  0,  0,  0,  0,  0,  0,  0,  0,
		   5, 10, 10, 10, 10, 10, 10,  5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		  -5,  0,  0,  0,  0,  0,  0, -5,
		   0,  0,  0,  5,  5,  0,  0,  0};
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
		  20, 30, 10,  0,  0, 10, 30, 20};
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
		// Vertically mirrored piece-square tables with negated values for black.
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				c1 = i*8 + j;
				c2 = 56 - i*8 + j;
				PST_B_PAWN_OPENING[c1] = (byte)-PST_W_PAWN_OPENING[c2];
				PST_B_PAWN_ENDGAME[c1] = (byte)-PST_W_PAWN_ENDGAME[c2];
				PST_B_KNIGHT_OPENING[c1] = (byte)-PST_W_KNIGHT_OPENING[c2];
				PST_B_KNIGHT_ENDGAME[c1] = (byte)-PST_W_KNIGHT_ENDGAME[c2];
				PST_B_BISHOP[c1] = (byte)-PST_W_BISHOP[c2];
				PST_B_ROOK_OPENING[c1] = (byte)-PST_W_ROOK_OPENING[c2];
				PST_B_ROOK_ENDGAME[c1] = (byte)-PST_W_ROOK_ENDGAME[c2];
				PST_B_QUEEN[c1] = (byte)-PST_W_QUEEN[c2];
				PST_B_KING_OPENING[c1] = (byte)-PST_W_KING_OPENING[c2];
				PST_B_KING_ENDGAME[c1] = (byte)-PST_W_KING_ENDGAME[c2];
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
	 * Returns a phaseScore between 0 and 256 á la Fruit.
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
	public static int phaseScore(Position pos) {
		int numOfQueens, numOfRooks, numOfBishops, numOfKnights;
		numOfQueens = BitOperations.getHammingWeight(pos.whiteQueens | pos.blackQueens)*Material.QUEEN.phaseWeight;
		numOfRooks = BitOperations.getHammingWeight(pos.whiteRooks | pos.blackRooks)*Material.ROOK.phaseWeight;
		numOfBishops = BitOperations.getHammingWeight(pos.whiteBishops | pos.blackBishops)*Material.BISHOP.phaseWeight;
		numOfKnights = BitOperations.getHammingWeight(pos.whiteKnights | pos.blackKnights)*Material.KNIGHT.phaseWeight;
		return phaseScore(numOfQueens, numOfRooks, numOfBishops, numOfKnights);
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
	public int score(Position pos, int alpha, int beta) {
		byte numOfWhiteQueens, numOfBlackQueens, numOfWhiteRooks, numOfBlackRooks, numOfWhiteBishops, numOfBlackBishops, numOfWhiteKnights,
			numOfBlackKnights, numOfAllPieces;
		int bishopField, bishopColor, newBishopColor, phase, piece;
		short pawnScore, baseScore, openingScore, endgameScore, score;
		byte[] bishopSqrArr, offsetBoard;;
		boolean isWhitesTurn;
		ETEntry eE;
		PTEntry pE;
		eE = eT.lookUp(pos.key);
		if (eE != null) {
			eE.generation = hashGen;
			return eE.score;
		}
		isWhitesTurn = pos.isWhitesTurn;
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
		// Try for hashed pawn score.
		pE = pT.lookUp(pos.pawnKey);
		if (pE != null) {
			pE.generation = hashGen;
			pawnScore = pE.score;
		}
		// Evaluate pawn structure.
		// Definitely needs to be thorough and sophisticated to make up for the costs of pawn hashing.
		else {
			pawnScore = (short)((BitOperations.getHammingWeight(pos.whitePawns) -
					BitOperations.getHammingWeight(pos.blackPawns))*Material.PAWN.score);
			pT.insert(new PTEntry(pos.pawnKey, pawnScore, hashGen));
		}
		baseScore = 0;
		baseScore += (numOfWhiteQueens - numOfBlackQueens)*Material.QUEEN.score;
		baseScore += (numOfWhiteRooks - numOfBlackRooks)*Material.ROOK.score;
		baseScore += (numOfWhiteBishops - numOfBlackBishops)*Material.BISHOP.score;
		baseScore += (numOfWhiteKnights - numOfBlackKnights)*Material.KNIGHT.score;
		baseScore += pawnScore;
		openingScore = endgameScore = 0;
		offsetBoard = pos.offsetBoard;
		for (int i = 0; i < offsetBoard.length; i++) {
			piece = offsetBoard[i];
			if (piece == Piece.NULL.ind)
				continue;
			openingScore += PST_OPENING[piece - 1][i];
			endgameScore += PST_ENDGAME[piece - 1][i];
		}
		openingScore += baseScore;
		endgameScore += baseScore;
		score = (short)taperedEvalScore(openingScore, endgameScore, phase);
		if (!isWhitesTurn)
			score *= -1;
		eT.insert(new ETEntry(pos.key, score, hashGen));
		return score;
	}
}
