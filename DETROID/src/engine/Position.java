package engine;

import util.*;
import engine.Board.*;

/**
 * A bit board based class whose object holds information amongst others on the current board position, on all the previous moves and positions,
 * on castling and en passant rights, and on the player to move. It uses a pre-calculated 'magic' move database to avoid the cost of computing
 * the possible move sets of sliding pieces on the fly.
 * 
 * The main functions include:
 * {@link #generateAllMoves() generateAllMoves}
 * {@link #makeMove(Move) makeMove}
 * {@link #unmakeMove() unmakeMove}
 * {@link #perft(int) perft}
 * {@link #divide(int) divide}
 *  
 * @author Viktor
 * 
 */
public class Position implements Hashable, Copiable<Position> {
	
	// Bit boards for each piece type.
	long whiteKing;
	long whiteQueens;
	long whiteRooks;
	long whiteBishops;
	long whiteKnights;
	long whitePawns;
	
	long blackKing;
	long blackQueens;
	long blackRooks;
	long blackBishops;
	long blackKnights;
	long blackPawns;
	
	// Bit board unions maintained for faster processing of positions.
	long allWhiteOccupied;
	long allBlackOccupied;
	
	long allNonWhiteOccupied;
	long allNonBlackOccupied;
	
	long allOccupied;
	long allEmpty;
	
	/**A complimentary board data-structure to the bit boards to efficiently detect pieces on specific squares. */
	byte[] offsetBoard;
	
	/**Denotes whether it is white's turn to make a move, or not, i.e. it is black's. */
	boolean isWhitesTurn;
	
	/**A bitmap of all the pieces that attack the color to move's king. */
	long checkers;
	/**Denotes whether the color to move's king is in check or not. */
	boolean isInCheck;
	
	/**The count of the current ply/half-move. */
	int halfMoveIndex;
	/**The number of moves made since the last pawn move or capture; the choice of type fell on long due to data loss when smaller integer
	 * types are shifted beyond the 32nd bit in the move integer. */
	short fiftyMoveRuleClock;
	
	/**Denotes the file on which en passant is possible; 8 means no en passant rights. */
	byte enPassantRights;
	/**
	 * Denotes to what extent it would still be possible to castle for white regardless of whether it is actually legally executable in the
	 * current position. 0 - no castling rights, 1 - king-side castling only, 2 - queen-side castling only, 3 - all castling rights.
	 */
	byte whiteCastlingRights;
	/**
	 * Denotes to what extent it would still be possible to castle for black regardless of whether it is actually legally executable in the
	 * current position. 0 - no castling rights, 1 - king-side castling only, 2 - queen-side castling only, 3 - all castling rights.
	 */
	byte blackCastlingRights;
	
	/**A stack of all the moves made so far. */
	Stack<Move> moveList;
	/**A stack history of castling rights, en passant rights, fifty-move rule clock, repetitions, and check info. */
	Stack<UnmakeRegister> unmakeRegisterHistory;
	
	/**A Zobrist key generator instance. */
	ZobristKeyGenerator gen;
	/**The Zobrist key that is fairly close to a unique representation of the state of the Position instance in one number. */
	long key;
	/**A Zobrist key for the pawns' position only. */
	long pawnKey;
	/**All the positions that have occurred so far represented in Zobrist keys. */
	long[] keyHistory;
	/**All the pawn positions occurred. */
	long[] pawnKeyHistory;
	
	/**
	 * The number of times the current position has occurred before; the choice of type fell on long due to data loss when smaller integer
	 * types are shifted beyond the 32nd bit in the move integer.
	 */
	byte repetitions;
	
	/**Initializes a default, empty Position instance.*/
	Position() {
		moveList = new Stack<Move>();
		unmakeRegisterHistory = new Stack<UnmakeRegister>();
		gen = ZobristKeyGenerator.getInstance();
		/* "The longest decisive tournament game is Fressinet-Kosteniuk, Villandry 2007, which Kosteniuk won in 237 moves."
		 * - one third of that is used as the initial length of the history array. */
		keyHistory = new long[158];
		pawnKeyHistory = new long[158];
	}
	private Position(Position pos) {
		Stack<Move> reverseMoves;
		Stack<UnmakeRegister> reverseUnmake;
		whiteKing = pos.whiteKing;
		whiteQueens = pos.whiteQueens;
		whiteRooks = pos.whiteRooks;
		whiteBishops = pos.whiteBishops;
		whiteKnights = pos.whiteKnights;
		whitePawns = pos.whitePawns;
		blackKing = pos.blackKing;
		blackQueens = pos.blackQueens;
		blackRooks = pos.blackRooks;
		blackBishops = pos.blackBishops;
		blackKnights = pos.blackKnights;
		blackPawns = pos.blackPawns;
		allWhiteOccupied = pos.allWhiteOccupied;
		allBlackOccupied = pos.allBlackOccupied;
		allNonWhiteOccupied = pos.allNonWhiteOccupied;
		allNonBlackOccupied = pos.allNonBlackOccupied;
		allOccupied = pos.allOccupied;
		allEmpty = pos.allEmpty;
		enPassantRights = pos.enPassantRights;
		whiteCastlingRights = pos.whiteCastlingRights;
		blackCastlingRights = pos.blackCastlingRights;
		isWhitesTurn = pos.isWhitesTurn;
		isInCheck = pos.isInCheck;
		checkers = pos.checkers;
		halfMoveIndex = pos.halfMoveIndex;
		fiftyMoveRuleClock = pos.fiftyMoveRuleClock;
		key = pos.key;
		pawnKey = pos.pawnKey;
		repetitions = pos.repetitions;
		offsetBoard = new byte[pos.offsetBoard.length];
		for (int i = 0; i < pos.offsetBoard.length; i++)
			offsetBoard[i] = pos.offsetBoard[i];
		keyHistory = new long[pos.keyHistory.length];
		pawnKeyHistory = new long[pos.keyHistory.length];
		for (int i = 0; i < pos.keyHistory.length; i++) {
			keyHistory[i] = pos.keyHistory[i];
			pawnKeyHistory[i] = pos.pawnKeyHistory[i];
		}
		gen = pos.gen;
		reverseMoves = new Stack<>();
		moveList = new Stack<>();
		while (pos.moveList.hasNext())
			reverseMoves.add(pos.moveList.next());
		while (reverseMoves.hasNext())
			moveList.add(reverseMoves.next());
		reverseUnmake = new Stack<>();
		unmakeRegisterHistory = new Stack<>();
		while (pos.unmakeRegisterHistory.hasNext())
			reverseUnmake.add(pos.unmakeRegisterHistory.next());
		while (reverseUnmake.hasNext())
			unmakeRegisterHistory.add(reverseUnmake.next());
	}
	/**
	 * Returns a deep copy of the position.
	 * 
	 * @return
	 */
	@Override
	public Position deepCopy() {
		return new Position(this);
	}
	/**
	 * Returns the 64-bit Zobrist key of the current position. A Zobrist key is used to almost uniquely hash a chess position to an integer.
	 */
	@Override
	public long hashKey() {
		return key;
	}
	/**
	 * Returns the hash key of the position from before the last move was made.
	 * 
	 * @return
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public long getPrevHashKey() throws ArrayIndexOutOfBoundsException {
		return keyHistory[halfMoveIndex - 1];
	}
	/**
	 * Returns an object containing all relevant information about the last move made. If the move history list is empty, it returns null.
	 * 
	 * @return
	 */
	Move getLastMove() {
		return moveList.getHead();
	}
	/**
	 * Returns an object containing some information about the previous position.
	 * 
	 * @return
	 */
	UnmakeRegister getUnmakeRegister() {
		return unmakeRegisterHistory.getHead();
	}
	/**
	 * Returns whether there are any pieces of the color defined by byWhite that could be, in the current position, legally moved to the
	 * supposedly enemy occupied square specified by sqrInd.
	 * 
	 * @param sqrInd
	 * @param byWhite
	 * @return
	 */
	public boolean isAttacked(int sqrInd, boolean byWhite) {
		MoveSetDatabase dB = MoveSetDatabase.getByIndex(sqrInd);
		if (byWhite) {
			if ((whiteKing & dB.kingMoveMask) != 0) return true;
			if ((whiteKnights & dB.knightMoveMask) != 0) return true;
			if ((whitePawns & dB.pawnBlackCaptureMoveMask) != 0) return true;
			if (((whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied)) != 0) return true;
			if (((whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied)) != 0) return true;
			if (offsetBoard[sqrInd] == Piece.B_PAWN.ind && enPassantRights != EnPassantRights.NONE.ind &&
					sqrInd == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights) {
				if ((whitePawns & dB.kingMoveMask & Rank.R5.bitmap) != 0) return true;
			}
		}
		else {
			if ((blackKing & dB.kingMoveMask) != 0) return true;
			if ((blackKnights & dB.knightMoveMask) != 0) return true;
			if ((blackPawns & dB.pawnWhiteCaptureMoveMask) != 0) return true;
			if (((blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied)) != 0) return true;
			if (((blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied)) != 0) return true;
			if (offsetBoard[sqrInd] == Piece.W_PAWN.ind && enPassantRights != EnPassantRights.NONE.ind &&
					sqrInd == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights) {
				if ((blackPawns & dB.kingMoveMask & Rank.R4.bitmap) != 0) return true;
			}
		}
		return false;
	}
	/**
	 * Returns whether there are any sliding pieces of the color defined by byWhite that could be, in the current position, legally moved to the
	 * supposedly enemy occupied square specified by sqrInd.
	 * 
	 * @param sqrInd
	 * @param byWhite
	 * @return
	 */
	public boolean isAttackedBySliders(int sqrInd, boolean byWhite) {
		MoveSetDatabase dB = MoveSetDatabase.getByIndex(sqrInd);
		if (byWhite) {
			if (((whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied)) != 0) return true;
			if (((whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied)) != 0) return true;
		}
		else {
			if (((blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied)) != 0) return true;
			if (((blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied)) != 0) return true;
		}
		return false;
	}
	/**
	 * Returns a bitmap representing all the squares on which the pieces are of the color defined by byWhite and in the current position could
	 * legally be moved to the supposedly enemy occupied square specified by sqrInd.
	 * 
	 * @param sqrInd
	 * @param byWhite
	 * @return
	 */
	public long getAttackers(int sqrInd, boolean byWhite) {
		long attackers = 0;
		MoveSetDatabase dB = MoveSetDatabase.getByIndex(sqrInd);
		if (byWhite) {
			attackers = whiteKing & dB.kingMoveMask;
			attackers |= whiteKnights & dB.knightMoveMask;
			attackers |= whitePawns & dB.pawnBlackCaptureMoveMask;
			attackers |= (whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied);
			attackers |= (whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
			if (enPassantRights != EnPassantRights.NONE.ind && sqrInd == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights)
				attackers |=  whitePawns & dB.kingMoveMask & Rank.R5.bitmap;
		}
		else {
			attackers = blackKing & dB.kingMoveMask;
			attackers |= blackKnights & dB.knightMoveMask;
			attackers |= blackPawns & dB.pawnWhiteCaptureMoveMask;
			attackers |= (blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied);
			attackers |= (blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied);
			if (enPassantRights != EnPassantRights.NONE.ind && sqrInd == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights)
				attackers |=  blackPawns & dB.kingMoveMask & Rank.R4.bitmap;
		}
		return attackers;
	}
	/**
	 * Returns a long representing all the squares on which the pieces are of the color defined by byWhite and in the current position could
	 * legally be moved to the supposedly empty square specified by sqrInd.
	 * 
	 * @param sqrInd
	 * @param byWhite
	 * @return
	 */
	public long getBlockerCandidates(int sqrInd, boolean byWhite) {
		long blockerCandidates = 0;
		long sqrBit = 1L << sqrInd;
		long blackPawnAdvance = (sqrBit >>> 8), whitePawnAdvance = (sqrBit << 8);
		if (byWhite) {
			MoveSetDatabase dB = MoveSetDatabase.getByIndex(sqrInd);
			blockerCandidates |= whiteKnights & dB.knightMoveMask;
			blockerCandidates |= whitePawns & blackPawnAdvance;
			if ((sqrBit & Rank.R4.bitmap) != 0 && (allEmpty & blackPawnAdvance) != 0)
				blockerCandidates |= whitePawns & (blackPawnAdvance >>> 8);
			blockerCandidates |= (whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied);
			blockerCandidates |= (whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
			if (enPassantRights == sqrInd%8 && (sqrBit & Rank.R6.bitmap) != 0)
				blockerCandidates |=  whitePawns & dB.pawnBlackCaptureMoveMask;
		}
		else {
			MoveSetDatabase dB = MoveSetDatabase.getByIndex(sqrInd);
			blockerCandidates |= blackKnights & dB.knightMoveMask;
			blockerCandidates |= blackPawns & whitePawnAdvance;
			if ((sqrBit & Rank.R5.bitmap) != 0 && (allEmpty & whitePawnAdvance) != 0)
				blockerCandidates |= blackPawns & (whitePawnAdvance << 8);
			blockerCandidates |= (blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied);
			blockerCandidates |= (blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied);
			if (enPassantRights == sqrInd%8 && (sqrBit & Rank.R3.bitmap) != 0)
				blockerCandidates |=  blackPawns & dB.pawnWhiteCaptureMoveMask;
		}
		return blockerCandidates;
	}
	/**
	 * Returns a bitmap representing the attackers of the color to move's king.
	 * 
	 * @return
	 */
	public long getCheckers() {
		long attackers = 0;
		int sqrInd;
		MoveSetDatabase dB;
		if (this.isWhitesTurn) {
			sqrInd = BitOperations.indexOfBit(whiteKing);
			dB = MoveSetDatabase.getByIndex(sqrInd);
			attackers = blackKnights & dB.knightMoveMask;
			attackers |= blackPawns & dB.pawnWhiteCaptureMoveMask;
			attackers |= (blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied);
			attackers |= (blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied);
		}
		else {
			sqrInd = BitOperations.indexOfBit(blackKing);
			dB = MoveSetDatabase.getByIndex(sqrInd);
			attackers = whiteKnights & dB.knightMoveMask;
			attackers |= whitePawns & dB.pawnBlackCaptureMoveMask;
			attackers |= (whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied);
			attackers |= (whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
		}
		return attackers;
	}
	/**
	 * Returns a long representing all the squares on which there are pinned pieces of the color defined by forWhite in the current position.
	 * A pinned piece is one that when moved would expose its king to a check.
	 * 
	 * @param forWhite
	 * @return
	 */
	public long getPinnedPieces(boolean forWhite) {
		long rankPos, rankNeg, filePos, fileNeg, diagonalPos, diagonalNeg, antiDiagonalPos, antiDiagonalNeg;
		long straightSliders, diagonalSliders, pinnedPiece, pinnedPieces = 0;
		RayMask attRayMask;
		if (forWhite) {
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(this.whiteKing));
			rankPos = attRayMask.rankPos & allOccupied;
			rankNeg = attRayMask.rankNeg & allOccupied;
			filePos = attRayMask.filePos & allOccupied;
			fileNeg = attRayMask.fileNeg & allOccupied;
			diagonalPos = attRayMask.diagonalPos & allOccupied;
			diagonalNeg = attRayMask.diagonalNeg & allOccupied;
			antiDiagonalPos = attRayMask.antiDiagonalPos & allOccupied;
			antiDiagonalNeg = attRayMask.antiDiagonalNeg & allOccupied;
			straightSliders = blackQueens | blackRooks;
			diagonalSliders = blackQueens | blackBishops;
			if ((pinnedPiece = BitOperations.getLSBit(rankPos) & allWhiteOccupied) != 0) {
				if ((BitOperations.getLSBit(rankPos^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(filePos) & allWhiteOccupied) != 0) {
				if ((BitOperations.getLSBit(filePos^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos) & allWhiteOccupied) != 0) {
				if ((BitOperations.getLSBit(diagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) & allWhiteOccupied) != 0) {
				if ((BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg) & allWhiteOccupied) != 0) {
				if ((BitOperations.getMSBit(rankNeg^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg) & allWhiteOccupied) != 0) {
				if ((BitOperations.getMSBit(fileNeg^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg) & allWhiteOccupied) != 0) {
				if ((BitOperations.getMSBit(diagonalNeg^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(antiDiagonalNeg) & allWhiteOccupied) != 0) {
				if ((BitOperations.getMSBit(antiDiagonalNeg^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
		}
		else {
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(blackKing));
			rankPos = attRayMask.rankPos & allOccupied;
			rankNeg = attRayMask.rankNeg & allOccupied;
			filePos = attRayMask.filePos & allOccupied;
			fileNeg = attRayMask.fileNeg & allOccupied;
			diagonalPos = attRayMask.diagonalPos & allOccupied;
			diagonalNeg = attRayMask.diagonalNeg & allOccupied;
			antiDiagonalPos = attRayMask.antiDiagonalPos & allOccupied;
			antiDiagonalNeg = attRayMask.antiDiagonalNeg & allOccupied;
			straightSliders = whiteQueens | whiteRooks;
			diagonalSliders = whiteQueens | whiteBishops;
			if ((pinnedPiece = BitOperations.getLSBit(rankPos) & allBlackOccupied) != 0) {
				if ((BitOperations.getLSBit(rankPos^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(filePos) & allBlackOccupied) != 0) {
				if ((BitOperations.getLSBit(filePos^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos) & allBlackOccupied) != 0) {
				if ((BitOperations.getLSBit(diagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) & allBlackOccupied) != 0) {
				if ((BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg) & allBlackOccupied) != 0) {
				if ((BitOperations.getMSBit(rankNeg^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg) & allBlackOccupied) != 0) {
				if ((BitOperations.getMSBit(fileNeg^pinnedPiece) & straightSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg) & allBlackOccupied) != 0) {
				if ((BitOperations.getMSBit(diagonalNeg^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
			if ((pinnedPiece = BitOperations.getMSBit(antiDiagonalNeg) & allBlackOccupied) != 0) {
				if ((BitOperations.getMSBit(antiDiagonalNeg^pinnedPiece) & diagonalSliders) != 0)
					pinnedPieces |= pinnedPiece;
			}
		}
		return pinnedPieces;
	}
	/**
	 * Returns whether a move is legal or not in the current position. It expects the move to be possibly legal in some position at least, and
	 * it checks if it still is in this one. That means, it does not check for inconsistency, such as a castling move with a capture, or an en
	 * passant with a moved piece other than pawn, etc.
	 * 
	 * @param move
	 * @return
	 */
	public boolean isLegalSoft(Move move) {
		MoveSetDatabase dB;
		boolean checked;
		long moveSet = 0;
		long toBit = (1L << move.to);
		if (offsetBoard[move.from] == move.movedPiece) {
			dB = MoveSetDatabase.getByIndex(move.to);
			PseudoSwitch: {
				if (isWhitesTurn) {
					if (move.movedPiece == Piece.W_KING.ind) {
						if (move.type == MoveType.SHORT_CASTLING.ind) {
							if (!isInCheck && whiteCastlingRights == CastlingRights.SHORT.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
								if (((Square.F1.bitmap | Square.G1.bitmap) & allOccupied) == 0 &&
										offsetBoard[Square.H1.ind] == Piece.W_ROOK.ind) {
									if (!isAttacked(Square.F1.ind, false) && !isAttacked(Square.G1.ind, false))
										return true;
								}
							}
						}
						else if (move.type == MoveType.LONG_CASTLING.ind) {
							if (!isInCheck && whiteCastlingRights == CastlingRights.LONG.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
								if (((Square.B1.bitmap | Square.C1.bitmap | Square.D1.bitmap) & allOccupied) == 0 &&
										offsetBoard[Square.A1.ind] == Piece.W_ROOK.ind) {
									if (!isAttacked(Square.D1.ind, false) && !isAttacked(Square.C1.ind, false))
										return true;
								}
							}
						}
						else {
							moveSet = dB.getKingMoveSet(allNonWhiteOccupied);
							if ((moveSet & toBit) != 0 && !isAttacked(move.to, false))
								return true;
						}
						return false;
					}
					else if (move.movedPiece == Piece.W_QUEEN.ind)
						moveSet = dB.getQueenMoveSet(allNonWhiteOccupied, allOccupied);
					else if (move.movedPiece == Piece.W_ROOK.ind)
						moveSet = dB.getRookMoveSet(allNonWhiteOccupied, allOccupied);
					else if (move.movedPiece == Piece.W_BISHOP.ind)
						moveSet = dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied);
					else if (move.movedPiece == Piece.W_KNIGHT.ind)
						moveSet = dB.getKnightMoveSet(allNonWhiteOccupied);
					else if (move.movedPiece == Piece.W_PAWN.ind) {
						moveSet = dB.getWhitePawnMoveSet(allBlackOccupied, allEmpty);
						if (move.type == MoveType.EN_PASSANT.ind && enPassantRights != EnPassantRights.NONE.ind &&
								move.to == EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights) {
							moveSet |= toBit;
							break PseudoSwitch;
						}
					}
					else return false;
				}
				else {
					if (move.movedPiece == Piece.B_KING.ind) {
						if (move.type == MoveType.SHORT_CASTLING.ind) {
							if (!isInCheck && blackCastlingRights == CastlingRights.SHORT.ind || blackCastlingRights == CastlingRights.ALL.ind) {
								if (((Square.F8.bitmap | Square.G8.bitmap) & allOccupied) == 0 &&
										offsetBoard[Square.H8.ind] == Piece.B_ROOK.ind) {
									if (!isAttacked(Square.F8.ind, true) && !isAttacked(Square.G8.ind, true))
										return true;
								}
							}
						}
						else if (move.type == MoveType.LONG_CASTLING.ind) {
							if (!isInCheck && blackCastlingRights == CastlingRights.LONG.ind || blackCastlingRights == CastlingRights.ALL.ind) {
								if (((Square.B8.bitmap | Square.C8.bitmap | Square.D8.bitmap) & allOccupied) == 0 &&
										offsetBoard[Square.A8.ind] == Piece.B_ROOK.ind) {
									if (!isAttacked(Square.C8.ind, true) && !isAttacked(Square.D8.ind, true))
										return true;
								}
							}
						}
						else {
							moveSet = dB.getKingMoveSet(allNonBlackOccupied);
							if ((moveSet & toBit) != 0 && !isAttacked(move.to, false))
								return true;
						}
						return false;
					}
					else if (move.movedPiece == Piece.B_QUEEN.ind)
						moveSet = dB.getQueenMoveSet(allNonBlackOccupied, allOccupied);
					else if (move.movedPiece == Piece.B_ROOK.ind)
						moveSet = dB.getRookMoveSet(allNonBlackOccupied, allOccupied);
					else if (move.movedPiece == Piece.B_BISHOP.ind)
						moveSet = dB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
					else if (move.movedPiece == Piece.B_KNIGHT.ind)
						moveSet = dB.getKnightMoveSet(allNonBlackOccupied);
					else if (move.movedPiece == Piece.B_PAWN.ind) {
						moveSet = dB.getBlackPawnMoveSet(allWhiteOccupied, allEmpty);
						if (move.type == MoveType.EN_PASSANT.ind && enPassantRights != EnPassantRights.NONE.ind &&
								move.to == EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights) {
							moveSet |= toBit;
							break PseudoSwitch;
						}
					}
					else return false;
				}
				if (offsetBoard[move.to] != move.capturedPiece)
					return false;
			}
			if ((moveSet & toBit) != 0) {
				makeMoveOnBoard(move);
				checked = isAttacked(BitOperations.indexOfBit(isWhitesTurn ? whiteKing : blackKing), !isWhitesTurn);
				unmakeMoveOnBoard(move);
				if (!checked) return true;
			}
		}
		return false;
	}
	/**
	 * Returns whether a move is legal or not in the current position. It does not assume that the move argument is legal in any positions. It
	 * is more costly than {@link #isLegalSoft(Move) isLegalSoft} as it generates a list of all the legal moves in the given position and
	 * compares the input parameter to them.
	 * 
	 * @param move
	 * @return
	 */
	public boolean isLegalHard(Move move) {
		List<Move> moveList = generateAllMoves();
		while (moveList.hasNext()) {
			if (moveList.next().equals(move))
				return true;
		}
		return false;
	}
	/**
	 * Returns whether a move would give check or not in this position.
	 * 
	 * @param move
	 * @return Whether the move would give check or not in this position.
	 */
	public boolean givesCheck(Move move) {
		MoveSetDatabase db;
		long toBit = 1L << move.to;
		if (isWhitesTurn) {
			db = MoveSetDatabase.getByIndex(BitOperations.indexOfBit(blackKing));
			if (move.movedPiece == Piece.W_QUEEN.ind) {
				if ((db.getQueenMoveSet(allNonWhiteOccupied, allOccupied) & toBit) != 0) return true;
			}
			else if (move.movedPiece == Piece.W_ROOK.ind) {
				if ((db.getRookMoveSet(allNonWhiteOccupied, allOccupied) & toBit) != 0) return true;
			}
			else if (move.movedPiece == Piece.W_BISHOP.ind) {
				if ((db.getBishopMoveSet(allNonWhiteOccupied, allOccupied) & toBit) != 0) return true;
			}
			else if (move.movedPiece == Piece.W_KNIGHT.ind) {
				if ((db.getKnightMoveSet(allNonWhiteOccupied) & toBit) != 0) return true;
			}
			else {
				if ((db.pawnBlackCaptureMoveMask & toBit) != 0) return true;
			}
		}
		else {
			db = MoveSetDatabase.getByIndex(BitOperations.indexOfBit(whiteKing));
			if (move.movedPiece == Piece.B_QUEEN.ind) {
				if ((db.getQueenMoveSet(allNonBlackOccupied, allOccupied) & toBit) != 0) return true;
			}
			else if (move.movedPiece == Piece.B_ROOK.ind) {
				if ((db.getRookMoveSet(allNonBlackOccupied, allOccupied) & toBit) != 0) return true;
			}
			else if (move.movedPiece == Piece.B_BISHOP.ind) {
				if ((db.getBishopMoveSet(allNonBlackOccupied, allOccupied) & toBit) != 0) return true;
			}
			else if (move.movedPiece == Piece.B_KNIGHT.ind) {
				if ((db.getKnightMoveSet(allNonBlackOccupied) & toBit) != 0) return true;
			}
			else {
				if ((db.pawnWhiteCaptureMoveMask & toBit) != 0) return true;
			}
		}
		return false;
	}
	/**
	 * A method that returns an array of bitmaps of the squares from which the opponent's king can be checked for each piece type for
	 * the side to move (king obviously excluded).
	 * 
	 * @return An array of bitboards of squares from where the king can be checked  for each piece type for the side to move.
	 */
	public long[] squaresToCheckFrom() {
		MoveSetDatabase kingDB;
		long[] threatSquares = new long[5];
		if (isWhitesTurn) {
			kingDB = MoveSetDatabase.getByIndex(BitOperations.indexOfBit(blackKing));
			threatSquares[0] = kingDB.getQueenMoveSet(allNonWhiteOccupied, allOccupied);
			threatSquares[1] = kingDB.getRookMoveSet(allNonWhiteOccupied, allOccupied);
			threatSquares[2] = kingDB.getBishopMoveSet(allNonWhiteOccupied, allOccupied);
			threatSquares[3] = kingDB.getKnightMoveSet(allNonWhiteOccupied);
			threatSquares[4] = kingDB.pawnBlackCaptureMoveMask;
		}
		else {
			kingDB = MoveSetDatabase.getByIndex(BitOperations.indexOfBit(whiteKing));
			threatSquares[0] = kingDB.getQueenMoveSet(allNonBlackOccupied, allOccupied);
			threatSquares[1] = kingDB.getRookMoveSet(allNonBlackOccupied, allOccupied);
			threatSquares[2] = kingDB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
			threatSquares[3] = kingDB.getKnightMoveSet(allNonBlackOccupied);
			threatSquares[4] = kingDB.pawnWhiteCaptureMoveMask;
		}
		return threatSquares;
	}
	/**
	 * Generates and adds all pinned-piece-moves to the input parameter 'moves' and returns the set of pinned pieces as a bitboard.
	 * 
	 * @param moves The move list to which the pinned piece moves will be added.
	 * @return A bitboard representing the pinned pieces.
	 */
	private long addAllPinnedPieceMoves(List<Move> moves) {
		long straightSliders, diagonalSliders, pinnedPieceBit, pinnerBit, pinnedPieces = 0, enPassantDestination = 0;
		byte pinnedPieceInd, pinnedPiece, to;
		ByteStack pinnedPieceMoves;
		RayMask attRayMask;
		if (this.isWhitesTurn) {
			straightSliders = blackQueens | blackRooks;
			diagonalSliders = blackQueens | blackBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(whiteKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - whiteKing) << 1)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - whiteKing) << 1) & attRayMask.filePos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveSetDatabase.getByIndex(pinnedPieceInd).getWhitePawnAdvanceSet(allEmpty));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - whiteKing) << 1) & attRayMask.diagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = (byte)(pinnedPieceInd + 7))) &
									(pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = (byte)(pinnedPieceInd + 7)) == pinnerBit) {
								if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - whiteKing) << 1) & attRayMask.antiDiagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = (byte)(pinnedPieceInd + 9))) &
									(pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = (byte)(pinnedPieceInd + 9)) == pinnerBit) {
								if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((whiteKing - pinnerBit)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - pinnerBit) & attRayMask.fileNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveSetDatabase.getByIndex(pinnedPieceInd).getWhitePawnAdvanceSet(allEmpty));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - pinnerBit) & attRayMask.diagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - pinnerBit) & attRayMask.antiDiagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
		}
		else {
			straightSliders = whiteQueens | whiteRooks;
			diagonalSliders = whiteQueens | whiteBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(blackKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - blackKing) << 1)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - blackKing) << 1) & attRayMask.filePos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveSetDatabase.getByIndex(pinnedPieceInd).getBlackPawnAdvanceSet(allEmpty));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - blackKing) << 1) & attRayMask.diagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize((((pinnerBit - blackKing) << 1) & attRayMask.antiDiagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((blackKing - pinnerBit)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - pinnerBit) & attRayMask.fileNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveSetDatabase.getByIndex(pinnedPieceInd).getBlackPawnAdvanceSet(allEmpty));
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - pinnerBit) & attRayMask.diagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						if (this.enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = (byte)(pinnedPieceInd - 7))) &
									(pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = (byte)(pinnedPieceInd - 7)) == pinnerBit) {
								if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd  = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - pinnerBit) & attRayMask.antiDiagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext()) {
							to = pinnedPieceMoves.next();
							moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
						}
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = (byte)(pinnedPieceInd - 9))) &
									(pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = (byte)(pinnedPieceInd - 9)) == pinnerBit) {
								if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
		}
		return pinnedPieces;
	}
	/**
	 * Generates and adds only the pinned-piece-moves that change the material balance on the board (captures/promotions) to the input
	 * parameter 'moves' and returns the set of pinned pieces as a bitboard.
	 * 
	 * @param moves The move list to which the pinned piece moves will be added.
	 * @return A bitboard representing the pinned pieces.
	 */
	private long addMaterialPinnedPieceMoves(List<Move> moves) {
		long straightSliders, diagonalSliders, pinnedPieceBit, pinnerBit, pinnedPieces = 0, enPassantDestination = 0;
		byte pinnedPieceInd, pinnedPiece, to;
		RayMask attRayMask;
		if (this.isWhitesTurn) {
			straightSliders = blackQueens | blackRooks;
			diagonalSliders = blackQueens | blackBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(whiteKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd  = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = (byte)(pinnedPieceInd + 7))) &
									(pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = (byte)(pinnedPieceInd + 7)) == pinnerBit) {
								if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = (byte)(pinnedPieceInd + 9))) &
									(pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = (byte)(pinnedPieceInd + 9)) == pinnerBit) {
								if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
		}
		else {
			straightSliders = whiteQueens | whiteRooks;
			diagonalSliders = whiteQueens | whiteBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(blackKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						if (this.enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = (byte)(pinnedPieceInd - 7))) &
									(pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = (byte)(pinnedPieceInd - 7)) == pinnerBit) {
								if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd  = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						to = BitOperations.indexOfBit(pinnerBit);
						moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = (byte)(pinnedPieceInd - 9))) &
									(pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = (byte)(pinnedPieceInd - 9)) == pinnerBit) {
								if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
					}
				}
			}
		}
		return pinnedPieces;
	}
	/**
	 * Generates and adds only the pinned-piece-moves that do not change the material balance on the board to the input parameter 'moves'
	 * and returns the set of pinned pieces as a bitboard.
	 * 
	 * @param moves The move list to which the pinned piece moves will be added.
	 * @return A bitboard representing the pinned pieces.
	 */
	private long addNonMaterialPinnedPieceMoves(List<Move> moves) {
		long straightSliders, diagonalSliders, pinnedPieceBit, pinnerBit, pinnedPieces = 0;
		byte pinnedPieceInd, pinnedPiece;
		ByteStack pinnedPieceMoves;
		RayMask attRayMask;
		if (this.isWhitesTurn) {
			straightSliders = blackQueens | blackRooks;
			diagonalSliders = blackQueens | blackBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(whiteKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((pinnerBit - (whiteKing << 1))^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (whiteKing << 1)) & attRayMask.filePos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveSetDatabase.getByIndex(pinnedPieceInd).getWhitePawnAdvanceSet(allEmpty));
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (whiteKing << 1)) & attRayMask.diagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (whiteKing << 1)) &
								attRayMask.antiDiagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((whiteKing - (pinnerBit << 1))^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - (pinnerBit << 1)) & attRayMask.fileNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveSetDatabase.getByIndex(pinnedPieceInd).getWhitePawnAdvanceSet(allEmpty));
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - (pinnerBit << 1)) & attRayMask.diagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((whiteKing - (pinnerBit << 1)) &
								attRayMask.antiDiagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
		}
		else {
			straightSliders = whiteQueens | whiteRooks;
			diagonalSliders = whiteQueens | whiteBishops;
			attRayMask = RayMask.getByIndex(BitOperations.indexOfBit(blackKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((pinnerBit - (blackKing << 1))^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.filePos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.filePos & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (blackKing << 1)) & attRayMask.filePos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveSetDatabase.getByIndex(pinnedPieceInd).getBlackPawnAdvanceSet(allEmpty));
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.diagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.diagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (blackKing << 1)) & attRayMask.diagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.antiDiagonalPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.antiDiagonalPos & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((pinnerBit - (blackKing << 1)) &
								attRayMask.antiDiagonalPos)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.rankNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.rankNeg & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize((blackKing - (pinnerBit << 1))^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - (pinnerBit << 1)) & attRayMask.fileNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						pinnedPieceMoves = BitOperations.serialize(MoveSetDatabase.getByIndex(pinnedPieceInd).getBlackPawnAdvanceSet(allEmpty));
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.diagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.diagonalNeg & allOccupied)^pinnedPieceBit)  & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - (pinnerBit << 1)) & attRayMask.diagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.antiDiagonalNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.antiDiagonalNeg & allOccupied)^pinnedPieceBit) & diagonalSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd  = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_BISHOP.ind) {
						pinnedPieceMoves = BitOperations.serialize(((blackKing - (pinnerBit << 1)) &
								attRayMask.antiDiagonalNeg)^pinnedPieceBit);
						while (pinnedPieceMoves.hasNext())
							moves.add(new Move(pinnedPieceInd, pinnedPieceMoves.next(), pinnedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
					}
				}
			}
		}
		return pinnedPieces;
	}
	/**
	 * A method that returns a list (queue) of all the legal moves from a non-check position.
	 * 
	 * @return A queue of legal moves.
	 */
	private Queue<Move> generateAllNormalMoves() {
		long movablePieces, pieceSet, moveSet, enPassAttBits, enPassBits;
		byte king, piece, to, victim;
		ByteStack pieces, moveList;
		Move move;
		Queue<Move> moves = new Queue<Move>();
		if (isWhitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			moveSet  = MoveSetDatabase.getByIndex(king).getKingMoveSet(allNonWhiteOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
			if (whiteCastlingRights == CastlingRights.LONG.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.B1.bitmap | Square.C1.bitmap | Square.D1.bitmap) & allOccupied) == 0) {
					if ((move = moves.getTail()) != null && move.to == Square.D1.ind && !isAttacked(Square.C1.ind, false))
						moves.add(new Move(king, Square.C1.ind, Piece.W_KING.ind, Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
				}
			}
			if (whiteCastlingRights == CastlingRights.SHORT.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.F1.bitmap | Square.G1.bitmap) & allOccupied) == 0) {
					if (!isAttacked(Square.F1.ind, false) && !isAttacked(Square.G1.ind, false))
						moves.add(new Move(king, Square.G1.ind, Piece.W_KING.ind, Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
				}
			}
			movablePieces = ~addAllPinnedPieceMoves(moves);
			pieceSet = whiteQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveSetDatabase.getByIndex(piece).getQueenMoveSet(allNonWhiteOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_QUEEN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getRookMoveSet(allNonWhiteOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_ROOK.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getBishopMoveSet(allNonWhiteOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_BISHOP.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getKnightMoveSet(allNonWhiteOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_KNIGHT.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whitePawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveSetDatabase.getByIndex(piece).getWhitePawnMoveSet(allBlackOccupied, allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to >= Square.A8.ind) {
						victim = offsetBoard[to];
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_KNIGHT.ind));
					}
					else
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			if (enPassantRights != EnPassantRights.NONE.ind) {
				if ((pieceSet = MoveSetDatabase.getByIndex(to = (byte)(EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights))
						.getBlackPawnCaptureSet(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to - 8));
						allNonWhiteOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						if (!isAttackedBySliders(king, false))
							moves.add(new Move(piece, to, Piece.W_PAWN.ind, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
						allNonWhiteOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
					}
				}
			}
		}
		else {
			king  = BitOperations.indexOfBit(blackKing);
			moveSet	= MoveSetDatabase.getByIndex(king).getKingMoveSet(allNonBlackOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
			if (blackCastlingRights == CastlingRights.SHORT.ind || blackCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.F8.bitmap | Square.G8.bitmap) & allOccupied) == 0) {
					if ((move = moves.getHead()) != null && move.to == Square.F8.ind && !isAttacked(Square.G8.ind, true))
						moves.add(new Move(king, Square.G8.ind, Piece.B_KING.ind, Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
				}
			}
			if (blackCastlingRights == CastlingRights.LONG.ind || blackCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.B8.bitmap | Square.C8.bitmap | Square.D8.bitmap) & allOccupied) == 0) {
					if (!isAttacked(Square.C8.ind, true) && !isAttacked(Square.D8.ind, true))
						moves.add(new Move(king, Square.C8.ind, Piece.B_KING.ind, Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
				}
			}
			movablePieces = ~addAllPinnedPieceMoves(moves);
			pieceSet = blackQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getQueenMoveSet(allNonBlackOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_QUEEN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getRookMoveSet(allNonBlackOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_ROOK.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getBishopMoveSet(allNonBlackOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_BISHOP.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getKnightMoveSet(allNonBlackOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_KNIGHT.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackPawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveSetDatabase.getByIndex(piece).getBlackPawnMoveSet(allWhiteOccupied, allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to < Square.A2.ind) {
						victim = offsetBoard[to];
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_KNIGHT.ind));
					}
					else
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			if (enPassantRights != EnPassantRights.NONE.ind) {
				if ((pieceSet = MoveSetDatabase.getByIndex(to = (byte)(EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights))
						.getWhitePawnCaptureSet(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to + 8));
						allNonBlackOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						if (!isAttackedBySliders(king, true))
							moves.add(new Move(piece, to, Piece.B_PAWN.ind, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
						allNonBlackOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
					}
				}
			}
		}
		return moves;
	}
	/**
	 * A method that returns a list (queue) of the legal moves that change the material balance on the board (captures/promotions) from a
	 * non-check position.
	 * 
	 * @return A queue of material-tactical legal moves.
	 */
	private Queue<Move> generateMaterialNormalMoves() {
		long movablePieces, pieceSet, moveSet, enPassAttBits, enPassBits;
		byte king, piece, to, victim;
		ByteStack pieces, moveList;
		Queue<Move> moves = new Queue<Move>();
		if (isWhitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			moveSet  = MoveSetDatabase.getByIndex(king).getKingMoveSet(allBlackOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
			movablePieces = ~addMaterialPinnedPieceMoves(moves);
			pieceSet = whiteQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveSetDatabase.getByIndex(piece).getQueenMoveSet(allBlackOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_QUEEN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getRookMoveSet(allBlackOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_ROOK.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getBishopMoveSet(allBlackOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_BISHOP.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getKnightMoveSet(allBlackOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_KNIGHT.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = whitePawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveSetDatabase.getByIndex(piece).getWhitePawnMoveSet(allBlackOccupied, allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					victim = offsetBoard[to];
					if (to >= Square.A8.ind) {
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_KNIGHT.ind));
					}
					else if (victim != 0)
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.NORMAL.ind));
				}
			}
			if (enPassantRights != EnPassantRights.NONE.ind) {
				if ((pieceSet = MoveSetDatabase.getByIndex(to = (byte)(EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights))
						.getBlackPawnCaptureSet(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to - 8));
						allNonWhiteOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						if (!isAttackedBySliders(king, false))
							moves.add(new Move(piece, to, Piece.W_PAWN.ind, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
						allNonWhiteOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
					}
				}
			}
		}
		else {
			king  = BitOperations.indexOfBit(blackKing);
			moveSet	= MoveSetDatabase.getByIndex(king).getKingMoveSet(allWhiteOccupied);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
			movablePieces = ~addMaterialPinnedPieceMoves(moves);
			pieceSet = blackQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getQueenMoveSet(allWhiteOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_QUEEN.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getRookMoveSet(allWhiteOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_ROOK.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getBishopMoveSet(allWhiteOccupied, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_BISHOP.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getKnightMoveSet(allWhiteOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_KNIGHT.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackPawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveSetDatabase.getByIndex(piece).getBlackPawnMoveSet(allWhiteOccupied, allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					victim = offsetBoard[to];
					if (to < Square.A2.ind) {
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_KNIGHT.ind));
					}
					else if (victim != 0)
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.NORMAL.ind));
				}
			}
			if (enPassantRights != EnPassantRights.NONE.ind) {
				if ((pieceSet = MoveSetDatabase.getByIndex(to = (byte)(EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights))
						.getWhitePawnCaptureSet(pieceSet)) != 0) {
					pieces = BitOperations.serialize(pieceSet);
					while (pieces.hasNext()) {
						piece = pieces.next();
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to + 8));
						allNonBlackOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						if (!isAttackedBySliders(king, true))
							moves.add(new Move(piece, to, Piece.B_PAWN.ind, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
						allNonBlackOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
					}
				}
			}
		}
		return moves;
	}
	/**
	 * A method that returns a list (queue) of the legal moves that do not affect the material balance of the position (no captures or
	 * promotions) from a non-check position.
	 * 
	 * @return A queue of non-material legal moves.
	 */
	private Queue<Move> generateNonMaterialNormalMoves() {
		long movablePieces, pieceSet, moveSet;
		byte king, piece, to;
		ByteStack pieces, moveList;
		Move move;
		Queue<Move> moves = new Queue<Move>();
		if (isWhitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			moveSet  = MoveSetDatabase.getByIndex(king).getKingMoveSet(allEmpty);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
			}
			if (whiteCastlingRights == CastlingRights.LONG.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.B1.bitmap | Square.C1.bitmap | Square.D1.bitmap) & allOccupied) == 0) {
					if ((move = moves.getTail()) != null && move.to == Square.D1.ind && !isAttacked(Square.C1.ind, false))
						moves.add(new Move(king, Square.C1.ind, Piece.W_KING.ind, Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
				}
			}
			if (whiteCastlingRights == CastlingRights.SHORT.ind || whiteCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.F1.bitmap | Square.G1.bitmap) & allOccupied) == 0) {
					if (!isAttacked(Square.F1.ind, false) && !isAttacked(Square.G1.ind, false))
						moves.add(new Move(king, Square.G1.ind, Piece.W_KING.ind, Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
				}
			}
			movablePieces = ~addNonMaterialPinnedPieceMoves(moves);
			pieceSet = whiteQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveSetDatabase.getByIndex(piece).getQueenMoveSet(allEmpty, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_QUEEN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getRookMoveSet(allEmpty, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_ROOK.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getBishopMoveSet(allEmpty, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_BISHOP.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = whiteKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getKnightMoveSet(allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.W_KNIGHT.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = whitePawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveSetDatabase.getByIndex(piece).getWhitePawnAdvanceSet(allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to <= 55)
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
		}
		else {
			king  = BitOperations.indexOfBit(blackKing);
			moveSet	= MoveSetDatabase.getByIndex(king).getKingMoveSet(allEmpty);
			moveList = BitOperations.serialize(moveSet);
			while (moveList.hasNext()) {
				to = moveList.next();
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
			}
			if (blackCastlingRights == CastlingRights.SHORT.ind || blackCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.F8.bitmap | Square.G8.bitmap) & allOccupied) == 0) {
					if ((move = moves.getHead()) != null && move.to == Square.F8.ind && !isAttacked(Square.G8.ind, true))
						moves.add(new Move(king, Square.G8.ind, Piece.B_KING.ind, Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
				}
			}
			if (blackCastlingRights == CastlingRights.LONG.ind || blackCastlingRights == CastlingRights.ALL.ind) {
				if (((Square.B8.bitmap | Square.C8.bitmap | Square.D8.bitmap) & allOccupied) == 0) {
					if (!isAttacked(Square.C8.ind, true) && !isAttacked(Square.D8.ind, true))
						moves.add(new Move(king, Square.C8.ind, Piece.B_KING.ind, Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
				}
			}
			movablePieces = ~addNonMaterialPinnedPieceMoves(moves);
			pieceSet = blackQueens & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getQueenMoveSet(allEmpty, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_QUEEN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackRooks & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getRookMoveSet(allEmpty, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_ROOK.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackBishops & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getBishopMoveSet(allEmpty, allOccupied);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_BISHOP.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackKnights & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet	= MoveSetDatabase.getByIndex(piece).getKnightMoveSet(allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					moves.add(new Move(piece, to, Piece.B_KNIGHT.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			pieceSet = blackPawns & movablePieces;
			pieces = BitOperations.serialize(pieceSet);
			while (pieces.hasNext()) {
				piece = pieces.next();
				moveSet = MoveSetDatabase.getByIndex(piece).getBlackPawnAdvanceSet(allEmpty);
				moveList = BitOperations.serialize(moveSet);
				while (moveList.hasNext()) {
					to = moveList.next();
					if (to >= 8)
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
		}
		return moves;
	}
	/**
	 * This method returns a list (queue) of all the legal moves from a position in which the side to move is in check.
	 * 
	 * @return A queue of all legal moves from a check position.
	 */
	private Queue<Move> generateAllCheckEvasionMoves() {
		long kingMoveSet, movablePieces, squaresOfInterventionSet, checkerAttackerSet, checkerBlockerSet;
		byte checker1, checker2, checkerPiece1, checkerPiece2, squareOfIntervention, checkerAttackerSquare,
			checkerBlockerSquare, king, to, movedPiece;
		ByteStack kingMoves, squaresOfIntervention, checkerAttackers, checkerBlockers;
		Queue<Move> moves = new Queue<Move>();
		MoveSetDatabase dB, kingDb;
		boolean promotionOnAttackPossible = false, promotionOnBlockPossible = false;
		if (this.isWhitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			movablePieces = ~getPinnedPieces(true);
			kingDb = MoveSetDatabase.getByIndex(king);
			kingMoveSet = kingDb.getKingMoveSet(allNonWhiteOccupied);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = this.offsetBoard[checker1];
				dB = MoveSetDatabase.getByIndex(checker1);
				if ((checkers & Rank.R8.bitmap) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, true) & movablePieces & ~whiteKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					movedPiece = offsetBoard[checkerAttackerSquare];
					if (movedPiece == Piece.W_PAWN.ind) {
						if (promotionOnAttackPossible) {
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_QUEEN.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_ROOK.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_BISHOP.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_KNIGHT.ind));
						}
						else if (enPassantRights != EnPassantRights.NONE.ind &&
								checker1 == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights)
							moves.add(new Move(checkerAttackerSquare, (byte)(checker1 + 8), movedPiece,
									checkerPiece1, MoveType.EN_PASSANT.ind));
						else
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
					}
					else
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
				}
				switch (checkerPiece1) {
					case 8: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getRookMoveSet(allNonBlackOccupied, allOccupied) &
									kingDb.getRookMoveSet(allNonWhiteOccupied, allOccupied));
							if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getBishopMoveSet(allNonBlackOccupied, allOccupied) &
									kingDb.getBishopMoveSet(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.W_PAWN.ind) {
									if (promotionOnBlockPossible) {
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
									}
									else if (enPassantRights != EnPassantRights.NONE.ind &&
											squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
									else
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
					}
					break;
					case 9: {
						if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getRookMoveSet(allNonBlackOccupied, allOccupied) &
								kingDb.getRookMoveSet(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.W_PAWN.ind) {
									if (promotionOnBlockPossible) {
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
									}
									else if (enPassantRights != EnPassantRights.NONE.ind &&
											squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
									else
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getRookMoveMask();
					}
					break;
					case 10: {
						squaresOfInterventionSet = (dB.getBishopMoveSet(allNonBlackOccupied, allOccupied) &
								kingDb.getBishopMoveSet(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.W_PAWN.ind) {
									if (enPassantRights != EnPassantRights.NONE.ind &&
											squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
									else
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getBishopMoveMask();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = offsetBoard[checker2];
				dB = MoveSetDatabase.getByIndex(checker1);
				switch (checkerPiece1) {
					case 8:
						kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getRookMoveMask();
					break;
					case 10:
						kingMoveSet &= ~dB.getBishopMoveMask();
					break;
					case 11:
						kingMoveSet &= ~dB.knightMoveMask;
				}
				dB = MoveSetDatabase.getByIndex(checker2);
				switch (checkerPiece2) {
					case 8:
						kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getRookMoveMask();
					break;
					case 10:
						kingMoveSet &= ~dB.getBishopMoveMask();
					break;
					case 11:
						kingMoveSet &= ~dB.knightMoveMask;
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
		}
		else {
			king = BitOperations.indexOfBit(blackKing);
			movablePieces = ~getPinnedPieces(false);
			kingDb = MoveSetDatabase.getByIndex(king);
			kingMoveSet = kingDb.getKingMoveSet(allNonBlackOccupied);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				dB = MoveSetDatabase.getByIndex(checker1);
				if ((checkers & Rank.R1.bitmap) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, false) & movablePieces & ~blackKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					movedPiece = offsetBoard[checkerAttackerSquare];
					if (movedPiece == Piece.B_PAWN.ind) {
						if (promotionOnAttackPossible) {
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_QUEEN.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_ROOK.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_BISHOP.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_KNIGHT.ind));
						}
						else if (enPassantRights != EnPassantRights.NONE.ind &&
								checker1 == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights)
							moves.add(new Move(checkerAttackerSquare, (byte)(checker1 - 8), movedPiece,
									checkerPiece1, MoveType.EN_PASSANT.ind));
						else
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
					}
					else
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
				}
				switch (checkerPiece1) {
					case 2: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getRookMoveSet(allNonWhiteOccupied, allOccupied) &
									kingDb.getRookMoveSet(allNonBlackOccupied, allOccupied));
							if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied) &
									kingDb.getBishopMoveSet(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.B_PAWN.ind) {
									if (promotionOnBlockPossible) {
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
									}
									else if (enPassantRights != EnPassantRights.NONE.ind &&
											squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
									else
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
					}
					break;
					case 3: {
						if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getRookMoveSet(allNonWhiteOccupied, allOccupied) &
								kingDb.getRookMoveSet(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.B_PAWN.ind) {
									if (promotionOnBlockPossible) {
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
									}
									else if (enPassantRights != EnPassantRights.NONE.ind &&
											squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
									else
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getRookMoveMask();
					}
					break;
					case 4: {
						squaresOfInterventionSet = (dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied) &
								kingDb.getBishopMoveSet(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (movedPiece == Piece.B_PAWN.ind) {
									if (enPassantRights != EnPassantRights.NONE.ind &&
											squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
									else
										moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
												Piece.NULL.ind, MoveType.NORMAL.ind));
								}
								else
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getBishopMoveMask();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = this.offsetBoard[checker2];
				dB = MoveSetDatabase.getByIndex(checker1);
				switch (checkerPiece1) {
					case 2:
						kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getRookMoveMask();
					break;
					case 4:
						kingMoveSet &= ~dB.getBishopMoveMask();
					break;
					case 5:
						kingMoveSet &= ~dB.knightMoveMask;
				}
				dB = MoveSetDatabase.getByIndex(checker2);
				switch (checkerPiece2) {
					case 2:
						kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getRookMoveMask();
					break;
					case 4:
						kingMoveSet &= ~dB.getBishopMoveMask();
					break;
					case 5:
						kingMoveSet &= ~dB.knightMoveMask;
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				}
			}
		}
		return moves;
	}
	/**
	 * This method returns a list (queue) of the legal moves that change the material balance on the board (captures/promotions) from a
	 * position in which the side to move is in check.
	 * 
	 * @return A queue of legal material moves from a check position.
	 */
	private Queue<Move> generateMaterialCheckEvasionMoves() {
		long kingMoveSet, movablePieces, squaresOfInterventionSet, checkerAttackerSet, checkerBlockerSet;
		byte checker1, checker2, checkerPiece1, checkerPiece2, squareOfIntervention, checkerAttackerSquare,
			checkerBlockerSquare, king, to, movedPiece;
		ByteStack kingMoves, squaresOfIntervention, checkerAttackers, checkerBlockers;
		Queue<Move> moves = new Queue<Move>();
		MoveSetDatabase dB, kingDb;
		boolean promotionOnAttackPossible = false, promotionOnBlockPossible = false;
		if (this.isWhitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			movablePieces = ~getPinnedPieces(true);
			kingDb = MoveSetDatabase.getByIndex(king);
			kingMoveSet = kingDb.getKingMoveSet(allBlackOccupied);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = this.offsetBoard[checker1];
				dB = MoveSetDatabase.getByIndex(checker1);
				if ((checkers & Rank.R8.bitmap) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, true) & movablePieces & ~whiteKing;
				checkerAttackers = BitOperations.serialize(checkerAttackerSet);
				while (checkerAttackers.hasNext()) {
					checkerAttackerSquare = checkerAttackers.next();
					movedPiece = offsetBoard[checkerAttackerSquare];
					if (movedPiece == Piece.W_PAWN.ind) {
						if (promotionOnAttackPossible) {
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_QUEEN.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_ROOK.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_BISHOP.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_KNIGHT.ind));
						}
						else if (enPassantRights != EnPassantRights.NONE.ind &&
								checker1 == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights)
							moves.add(new Move(checkerAttackerSquare, (byte)(checker1 + 8), movedPiece,
									checkerPiece1, MoveType.EN_PASSANT.ind));
						else
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
					}
					else
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
				}
				switch (checkerPiece1) {
				case 8: {
					if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
						squaresOfInterventionSet = (dB.getRookMoveSet(allNonBlackOccupied, allOccupied) &
								kingDb.getRookMoveSet(allNonWhiteOccupied, allOccupied));
						if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
							promotionOnBlockPossible = true;
					}
					else
						squaresOfInterventionSet = (dB.getBishopMoveSet(allNonBlackOccupied, allOccupied) &
								kingDb.getBishopMoveSet(allNonWhiteOccupied, allOccupied));
					squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
					while (squaresOfIntervention.hasNext()) {
						squareOfIntervention = squaresOfIntervention.next();
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
						checkerBlockers = BitOperations.serialize(checkerBlockerSet);
						while (checkerBlockers.hasNext()) {
							checkerBlockerSquare = checkerBlockers.next();
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (movedPiece == Piece.W_PAWN.ind) {
								if (promotionOnBlockPossible) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else if (enPassantRights != EnPassantRights.NONE.ind &&
										squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
							}
						}
					}
					kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
				}
				break;
				case 9: {
					if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
						promotionOnBlockPossible = true;
					squaresOfInterventionSet = (dB.getRookMoveSet(allNonBlackOccupied, allOccupied) &
							kingDb.getRookMoveSet(allNonWhiteOccupied, allOccupied));
					squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
					while (squaresOfIntervention.hasNext()) {
						squareOfIntervention = squaresOfIntervention.next();
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
						checkerBlockers = BitOperations.serialize(checkerBlockerSet);
						while (checkerBlockers.hasNext()) {
							checkerBlockerSquare = checkerBlockers.next();
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (movedPiece == Piece.W_PAWN.ind) {
								if (promotionOnBlockPossible) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else if (enPassantRights != EnPassantRights.NONE.ind &&
										squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
							}
						}
					}
					kingMoveSet &= ~dB.getRookMoveMask();
				}
				break;
				case 10: {
					squaresOfInterventionSet = (dB.getBishopMoveSet(allNonBlackOccupied, allOccupied) &
							kingDb.getBishopMoveSet(allNonWhiteOccupied, allOccupied));
					squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
					while (squaresOfIntervention.hasNext()) {
						squareOfIntervention = squaresOfIntervention.next();
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
						checkerBlockers = BitOperations.serialize(checkerBlockerSet);
						while (checkerBlockers.hasNext()) {
							checkerBlockerSquare = checkerBlockers.next();
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (movedPiece == Piece.W_PAWN.ind) {
								if (enPassantRights != EnPassantRights.NONE.ind &&
										squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
							}
						}
					}
					kingMoveSet &= ~dB.getBishopMoveMask();
				}
			}
			kingMoves = BitOperations.serialize(kingMoveSet);
			while (kingMoves.hasNext()) {
				to = kingMoves.next();
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
		}
		else {
			checker1 = BitOperations.indexOfLSBit(checkers);
			checkerPiece1 = offsetBoard[checker1];
			checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
			checkerPiece2 = offsetBoard[checker2];
			dB = MoveSetDatabase.getByIndex(checker1);
			switch (checkerPiece1) {
				case 8:
					kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
				break;
				case 9:
					kingMoveSet &= ~dB.getRookMoveMask();
				break;
				case 10:
					kingMoveSet &= ~dB.getBishopMoveMask();
				break;
				case 11:
					kingMoveSet &= ~dB.knightMoveMask;
			}
			dB = MoveSetDatabase.getByIndex(checker2);
			switch (checkerPiece2) {
				case 8:
					kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
				break;
				case 9:
					kingMoveSet &= ~dB.getRookMoveMask();
				break;
				case 10:
					kingMoveSet &= ~dB.getBishopMoveMask();
				break;
				case 11:
					kingMoveSet &= ~dB.knightMoveMask;
			}
			kingMoves = BitOperations.serialize(kingMoveSet);
			while (kingMoves.hasNext()) {
				to = kingMoves.next();
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
		}
	}
	else {
		king = BitOperations.indexOfBit(blackKing);
		movablePieces = ~getPinnedPieces(false);
		kingDb = MoveSetDatabase.getByIndex(king);
		kingMoveSet = kingDb.getKingMoveSet(allWhiteOccupied);
		if (BitOperations.resetLSBit(checkers) == 0) {
			checker1 = BitOperations.indexOfBit(checkers);
			checkerPiece1 = offsetBoard[checker1];
			dB = MoveSetDatabase.getByIndex(checker1);
			if ((checkers & Rank.R1.bitmap) != 0)
				promotionOnAttackPossible = true;
			checkerAttackerSet = getAttackers(checker1, false) & movablePieces & ~blackKing;
			checkerAttackers = BitOperations.serialize(checkerAttackerSet);
			while (checkerAttackers.hasNext()) {
				checkerAttackerSquare = checkerAttackers.next();
				movedPiece = offsetBoard[checkerAttackerSquare];
				if (movedPiece == Piece.B_PAWN.ind) {
					if (promotionOnAttackPossible) {
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_KNIGHT.ind));
					}
					else if (enPassantRights != EnPassantRights.NONE.ind && checker1 == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights)
						moves.add(new Move(checkerAttackerSquare, (byte)(checker1 - 8), movedPiece,
								checkerPiece1, MoveType.EN_PASSANT.ind));
					else
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
				}
				else
					moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
			}
			switch (checkerPiece1) {
				case 2: {
					if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
						squaresOfInterventionSet = (dB.getRookMoveSet(allNonWhiteOccupied, allOccupied) &
								kingDb.getRookMoveSet(allNonBlackOccupied, allOccupied));
						if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
							promotionOnBlockPossible = true;
					}
					else
						squaresOfInterventionSet = (dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied) &
								kingDb.getBishopMoveSet(allNonBlackOccupied, allOccupied));
					squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
					while (squaresOfIntervention.hasNext()) {
						squareOfIntervention = squaresOfIntervention.next();
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
						checkerBlockers = BitOperations.serialize(checkerBlockerSet);
						while (checkerBlockers.hasNext()) {
							checkerBlockerSquare = checkerBlockers.next();
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (movedPiece == Piece.B_PAWN.ind) {
								if (promotionOnBlockPossible) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else if (enPassantRights != EnPassantRights.NONE.ind &&
										squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
							}
						}
					}
					kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
				}
				break;
				case 3: {
					if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
						promotionOnBlockPossible = true;
					squaresOfInterventionSet = (dB.getRookMoveSet(allNonWhiteOccupied, allOccupied) &
							kingDb.getRookMoveSet(allNonBlackOccupied, allOccupied));
					squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
					while (squaresOfIntervention.hasNext()) {
						squareOfIntervention = squaresOfIntervention.next();
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
						checkerBlockers = BitOperations.serialize(checkerBlockerSet);
						while (checkerBlockers.hasNext()) {
							checkerBlockerSquare = checkerBlockers.next();
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (movedPiece == Piece.B_PAWN.ind) {
								if (promotionOnBlockPossible) {
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
								}
								else if (enPassantRights != EnPassantRights.NONE.ind &&
										squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
							}
						}
					}
					kingMoveSet &= ~dB.getRookMoveMask();
				}
				break;
				case 4: {
					squaresOfInterventionSet = (dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied) &
							kingDb.getBishopMoveSet(allNonBlackOccupied, allOccupied));
					squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
					while (squaresOfIntervention.hasNext()) {
						squareOfIntervention = squaresOfIntervention.next();
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
						checkerBlockers = BitOperations.serialize(checkerBlockerSet);
						while (checkerBlockers.hasNext()) {
							checkerBlockerSquare = checkerBlockers.next();
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (movedPiece == Piece.B_PAWN.ind) {
								if (enPassantRights != EnPassantRights.NONE.ind &&
										squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
							}
						}
					}
					kingMoveSet &= ~dB.getBishopMoveMask();
				}
			}
			kingMoves = BitOperations.serialize(kingMoveSet);
			while (kingMoves.hasNext()) {
				to = kingMoves.next();
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
		}
		else {
			checker1 = BitOperations.indexOfLSBit(checkers);
			checkerPiece1 = offsetBoard[checker1];
			checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
			checkerPiece2 = this.offsetBoard[checker2];
			dB = MoveSetDatabase.getByIndex(checker1);
			switch (checkerPiece1) {
				case 2:
					kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
				break;
				case 3:
					kingMoveSet &= ~dB.getRookMoveMask();
				break;
				case 4:
					kingMoveSet &= ~dB.getBishopMoveMask();
				break;
				case 5:
					kingMoveSet &= ~dB.knightMoveMask;
			}
			dB = MoveSetDatabase.getByIndex(checker2);
			switch (checkerPiece2) {
				case 2:
					kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
				break;
				case 3:
					kingMoveSet &= ~dB.getRookMoveMask();
				break;
				case 4:
					kingMoveSet &= ~dB.getBishopMoveMask();
				break;
				case 5:
					kingMoveSet &= ~dB.knightMoveMask;
			}
			kingMoves = BitOperations.serialize(kingMoveSet);
			while (kingMoves.hasNext()) {
				to = kingMoves.next();
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
			}
		}
	}
	return moves;
}
	/**
	 * This method returns a list (queue) of the legal moves that do not affect the material balance (no captures or promotions) from a
	 * position in which the side to move is in check.
	 * 
	 * @return A queue of non-material legal moves from a check position.
	 */
	private Queue<Move> generateNonMaterialCheckEvasionMoves() {
		long kingMoveSet, movablePieces, squaresOfInterventionSet, checkerBlockerSet;
		byte checker1, checker2, checkerPiece1, checkerPiece2, squareOfIntervention,
			checkerBlockerSquare, king, to, movedPiece;
		ByteStack kingMoves, squaresOfIntervention, checkerBlockers;
		Queue<Move> moves = new Queue<Move>();
		MoveSetDatabase dB, kingDb;
		boolean promotionOnAttackPossible = false, promotionOnBlockPossible = false;
		if (this.isWhitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			movablePieces = ~getPinnedPieces(true);
			kingDb = MoveSetDatabase.getByIndex(king);
			kingMoveSet = kingDb.getKingMoveSet(allEmpty);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = this.offsetBoard[checker1];
				dB = MoveSetDatabase.getByIndex(checker1);
				if ((checkers & Rank.R8.bitmap) != 0)
					promotionOnAttackPossible = true;
				switch (checkerPiece1) {
					case 8: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getRookMoveSet(allNonBlackOccupied, allOccupied) &
									kingDb.getRookMoveSet(allNonWhiteOccupied, allOccupied));
							if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getBishopMoveSet(allNonBlackOccupied, allOccupied) &
									kingDb.getBishopMoveSet(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (!promotionOnBlockPossible || movedPiece != Piece.W_PAWN.ind)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
					}
					break;
					case 9: {
						if (promotionOnAttackPossible && (whiteKing & Rank.R8.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getRookMoveSet(allNonBlackOccupied, allOccupied) &
								kingDb.getRookMoveSet(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (!promotionOnBlockPossible || movedPiece != Piece.W_PAWN.ind)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getRookMoveMask();
					}
					break;
					case 10: {
						squaresOfInterventionSet = (dB.getBishopMoveSet(allNonBlackOccupied, allOccupied) &
								kingDb.getBishopMoveSet(allNonWhiteOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, offsetBoard[checkerBlockerSquare],
										Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getBishopMoveMask();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = offsetBoard[checker2];
				dB = MoveSetDatabase.getByIndex(checker1);
				switch (checkerPiece1) {
					case 8:
						kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getRookMoveMask();
					break;
					case 10:
						kingMoveSet &= ~dB.getBishopMoveMask();
					break;
					case 11:
						kingMoveSet &= ~dB.knightMoveMask;
				}
				dB = MoveSetDatabase.getByIndex(checker2);
				switch (checkerPiece2) {
					case 8:
						kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
					break;
					case 9:
						kingMoveSet &= ~dB.getRookMoveMask();
					break;
					case 10:
						kingMoveSet &= ~dB.getBishopMoveMask();
					break;
					case 11:
						kingMoveSet &= ~dB.knightMoveMask;
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
		}
		else {
			king = BitOperations.indexOfBit(blackKing);
			movablePieces = ~getPinnedPieces(false);
			kingDb = MoveSetDatabase.getByIndex(king);
			kingMoveSet = kingDb.getKingMoveSet(allEmpty);
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				dB = MoveSetDatabase.getByIndex(checker1);
				if ((checkers & Rank.R1.bitmap) != 0)
					promotionOnAttackPossible = true;
				switch (checkerPiece1) {
					case 2: {
						if ((File.getBySquareIndex(king).bitmap & checkers) != 0 || (Rank.getBySquareIndex(king).bitmap & checkers) != 0) {
							squaresOfInterventionSet = (dB.getRookMoveSet(allNonWhiteOccupied, allOccupied) &
									kingDb.getRookMoveSet(allNonBlackOccupied, allOccupied));
							if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
								promotionOnBlockPossible = true;
						}
						else
							squaresOfInterventionSet = (dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied) &
									kingDb.getBishopMoveSet(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (!promotionOnBlockPossible || movedPiece != Piece.B_PAWN.ind)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
					}
					break;
					case 3: {
						if (promotionOnAttackPossible && (blackKing & Rank.R1.bitmap) != 0)
							promotionOnBlockPossible = true;
						squaresOfInterventionSet = (dB.getRookMoveSet(allNonWhiteOccupied, allOccupied) &
								kingDb.getRookMoveSet(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								movedPiece = offsetBoard[checkerBlockerSquare];
								if (!promotionOnBlockPossible || movedPiece != Piece.B_PAWN.ind)
									moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
											Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getRookMoveMask();
					}
					break;
					case 4: {
						squaresOfInterventionSet = (dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied) &
								kingDb.getBishopMoveSet(allNonBlackOccupied, allOccupied));
						squaresOfIntervention = BitOperations.serialize(squaresOfInterventionSet);
						while (squaresOfIntervention.hasNext()) {
							squareOfIntervention = squaresOfIntervention.next();
							checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
							checkerBlockers = BitOperations.serialize(checkerBlockerSet);
							while (checkerBlockers.hasNext()) {
								checkerBlockerSquare = checkerBlockers.next();
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, offsetBoard[checkerBlockerSquare],
										Piece.NULL.ind, MoveType.NORMAL.ind));
							}
						}
						kingMoveSet &= ~dB.getBishopMoveMask();
					}
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = this.offsetBoard[checker2];
				dB = MoveSetDatabase.getByIndex(checker1);
				switch (checkerPiece1) {
					case 2:
						kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getRookMoveMask();
					break;
					case 4:
						kingMoveSet &= ~dB.getBishopMoveMask();
					break;
					case 5:
						kingMoveSet &= ~dB.knightMoveMask;
				}
				dB = MoveSetDatabase.getByIndex(checker2);
				switch (checkerPiece2) {
					case 2:
						kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
					break;
					case 3:
						kingMoveSet &= ~dB.getRookMoveMask();
					break;
					case 4:
						kingMoveSet &= ~dB.getBishopMoveMask();
					break;
					case 5:
						kingMoveSet &= ~dB.knightMoveMask;
				}
				kingMoves = BitOperations.serialize(kingMoveSet);
				while (kingMoves.hasNext()) {
					to = kingMoves.next();
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				}
			}
		}
		return moves;
	}
	/**
	 * Generates a queue of Move objects that represents all the legal moves from the current position.
	 * 
	 * @return A queue of all the legal moves from this position.
	 */
	public Queue<Move> generateAllMoves() {
		if (isInCheck)
			return generateAllCheckEvasionMoves();
		else
			return generateAllNormalMoves();
	}
	/**
	 * Generates a queue of Move objects that represents the material legal moves (i.e. the ones that change the material balance of the
	 * position such as captures and promotions) from the current position.
	 * 
	 * @return A queue of the material legal moves from this position.
	 */
	public Queue<Move> generateMaterialMoves() {
		if (isInCheck)
			return generateMaterialCheckEvasionMoves();
		else
			return generateMaterialNormalMoves();
	}
	/**
	 * Generates a queue of Move objects that represents the non-material legal moves (i.e. the ones that do not affect the material
	 * balance of the position such as non-promotion and non-capture moves) from the current position.
	 * 
	 * @return A queue of the non-material legal moves from this position.
	 */
	public Queue<Move> generateNonMaterialMoves() {
		if (isInCheck)
			return generateNonMaterialCheckEvasionMoves();
		else
			return generateNonMaterialNormalMoves();
	}
	/**
	 * Makes a move only on the chess board representations of this Position object.
	 * 
	 * @param move
	 */
	private void makeMoveOnBoard(Move move) {
		int enPassantVictimSquare;
		long fromBit = 1L << move.from;
		long toBit = 1L << move.to;
		long changedBits = (fromBit | toBit);
		long collChangedBits;
		if (isWhitesTurn) {
			if (move.type == MoveType.NORMAL.ind) {
				offsetBoard[move.from] = Piece.NULL.ind;
				offsetBoard[move.to] = move.movedPiece;
				if (move.movedPiece == Piece.W_KING.ind)
					whiteKing = toBit;
				else if (move.movedPiece == Piece.W_QUEEN.ind)
					whiteQueens ^= changedBits;
				else if (move.movedPiece == Piece.W_ROOK.ind)
					whiteRooks ^= changedBits;
				else if (move.movedPiece == Piece.W_BISHOP.ind)
					whiteBishops ^= changedBits;
				else if (move.movedPiece == Piece.W_KNIGHT.ind)
					whiteKnights ^= changedBits;
				else
					whitePawns ^= changedBits;
				allWhiteOccupied ^= changedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					allBlackOccupied ^= toBit;
					blackQueens &= allBlackOccupied;
					blackRooks &= allBlackOccupied;
					blackBishops &= allBlackOccupied;
					blackKnights &= allBlackOccupied;
					blackPawns &= allBlackOccupied;
					allNonBlackOccupied = ~allBlackOccupied;
				}
			}
			else if (move.type == MoveType.SHORT_CASTLING.ind) {
				offsetBoard[Square.H1.ind] = Piece.NULL.ind;
				offsetBoard[Square.F1.ind] = Piece.W_ROOK.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				offsetBoard[move.to] = move.movedPiece;
				whiteKing = toBit;
				collChangedBits = (1L << Square.H1.ind) | (1L << Square.F1.ind);
				whiteRooks ^= collChangedBits;
				allWhiteOccupied ^= (changedBits | collChangedBits);
				allNonWhiteOccupied = ~allWhiteOccupied;
			}
			else if (move.type == MoveType.LONG_CASTLING.ind) {
				offsetBoard[Square.A1.ind] = Piece.NULL.ind;
				offsetBoard[Square.D1.ind] = Piece.W_ROOK.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				offsetBoard[move.to] = move.movedPiece;
				whiteKing = toBit;
				collChangedBits = (1L << Square.A1.ind) | (1L << Square.D1.ind);
				whiteRooks ^= collChangedBits;
				allWhiteOccupied ^= (changedBits | collChangedBits);
				allNonWhiteOccupied = ~allWhiteOccupied;
			}
			else if (move.type == MoveType.EN_PASSANT.ind) {
				enPassantVictimSquare = move.to - 8;
				offsetBoard[move.from] = Piece.NULL.ind;
				offsetBoard[move.to] = move.movedPiece;
				offsetBoard[enPassantVictimSquare] = Piece.NULL.ind;
				whitePawns ^= changedBits;
				allWhiteOccupied ^= changedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
				collChangedBits = 1L << enPassantVictimSquare;
				blackPawns ^= collChangedBits;
				allBlackOccupied ^= collChangedBits;
				allNonBlackOccupied = ~allBlackOccupied;
			}
			else if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
				offsetBoard[move.to] = Piece.W_QUEEN.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				whitePawns ^= fromBit;
				whiteQueens ^= toBit;
				allWhiteOccupied ^= changedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					allBlackOccupied ^= toBit;
					blackQueens &= allBlackOccupied;
					blackRooks &= allBlackOccupied;
					blackBishops &= allBlackOccupied;
					blackKnights &= allBlackOccupied;
					// It is impossible for a pawn to reside on the first or last ranks.
					allNonBlackOccupied = ~allBlackOccupied;
				}
			}
			else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
				offsetBoard[move.to] = Piece.W_ROOK.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				whitePawns ^= fromBit;
				whiteRooks ^= toBit;
				allWhiteOccupied ^= changedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					allBlackOccupied ^= toBit;
					blackQueens &= allBlackOccupied;
					blackRooks &= allBlackOccupied;
					blackBishops &= allBlackOccupied;
					blackKnights &= allBlackOccupied;
					allNonBlackOccupied = ~allBlackOccupied;
				}
			}
			else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
				offsetBoard[move.to] = Piece.W_BISHOP.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				whitePawns ^= fromBit;
				whiteBishops ^= toBit;
				allWhiteOccupied ^= changedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					allBlackOccupied ^= toBit;
					blackQueens &= allBlackOccupied;
					blackRooks &= allBlackOccupied;
					blackBishops &= allBlackOccupied;
					blackKnights &= allBlackOccupied;
					allNonBlackOccupied = ~allBlackOccupied;
				}
			}
			else {
				offsetBoard[move.to] = Piece.W_KNIGHT.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				whitePawns ^= fromBit;
				whiteKnights ^= toBit;
				allWhiteOccupied ^= changedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					allBlackOccupied ^= toBit;
					blackQueens &= allBlackOccupied;
					blackRooks &= allBlackOccupied;
					blackBishops &= allBlackOccupied;
					blackKnights &= allBlackOccupied;
					allNonBlackOccupied = ~allBlackOccupied;
				}
			}
		}
		else {
			if (move.type == MoveType.NORMAL.ind) {
				offsetBoard[move.from] = Piece.NULL.ind;
				offsetBoard[move.to] = move.movedPiece;
				if (move.movedPiece == Piece.B_KING.ind)
					blackKing = toBit;
				else if (move.movedPiece == Piece.B_QUEEN.ind)
					blackQueens ^= changedBits;
				else if (move.movedPiece == Piece.B_ROOK.ind)
					blackRooks ^= changedBits;
				else if (move.movedPiece == Piece.B_BISHOP.ind)
					blackBishops ^= changedBits;
				else if (move.movedPiece == Piece.B_KNIGHT.ind)
					blackKnights ^= changedBits;
				else
					blackPawns ^= changedBits;
				allBlackOccupied ^= changedBits;
				allNonBlackOccupied = ~allBlackOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					allWhiteOccupied ^= toBit;
					whiteQueens &= allWhiteOccupied;
					whiteRooks &= allWhiteOccupied;
					whiteBishops &= allWhiteOccupied;
					whiteKnights &= allWhiteOccupied;
					whitePawns &= allWhiteOccupied;
					allNonWhiteOccupied = ~allWhiteOccupied;
				}
			}
			else if (move.type == MoveType.SHORT_CASTLING.ind) {
				offsetBoard[Square.H8.ind] = Piece.NULL.ind;
				offsetBoard[Square.F8.ind] = Piece.B_ROOK.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				offsetBoard[move.to] = move.movedPiece;
				blackKing = toBit;
				collChangedBits = (1L << Square.H8.ind) | (1L << Square.F8.ind);
				blackRooks ^= collChangedBits;
				allBlackOccupied ^= (changedBits | collChangedBits);
				allNonBlackOccupied = ~allBlackOccupied;
			}
			else if (move.type == MoveType.LONG_CASTLING.ind) {
				offsetBoard[Square.A8.ind] = Piece.NULL.ind;
				offsetBoard[Square.D8.ind] = Piece.B_ROOK.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				offsetBoard[move.to] = move.movedPiece;
				blackKing = toBit;
				collChangedBits = (1L << Square.A8.ind) | (1L << Square.D8.ind);
				blackRooks ^= collChangedBits;
				allBlackOccupied ^= (changedBits | collChangedBits);
				allNonBlackOccupied = ~allBlackOccupied;
			}
			else if (move.type == MoveType.EN_PASSANT.ind) {
				enPassantVictimSquare = move.to + 8;
				offsetBoard[move.from] = Piece.NULL.ind;
				offsetBoard[move.to] = move.movedPiece;
				offsetBoard[enPassantVictimSquare] = Piece.NULL.ind;
				blackPawns ^= changedBits;
				allBlackOccupied ^= changedBits;
				allNonBlackOccupied = ~allBlackOccupied;
				collChangedBits = 1L << enPassantVictimSquare;
				whitePawns ^= collChangedBits;
				allWhiteOccupied ^= collChangedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
			}
			else if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
				offsetBoard[move.to] = Piece.B_QUEEN.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				blackPawns ^= fromBit;
				blackQueens ^= toBit;
				allBlackOccupied ^= changedBits;
				allNonBlackOccupied = ~allBlackOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					allWhiteOccupied ^= toBit;
					whiteQueens &= allWhiteOccupied;
					whiteRooks &= allWhiteOccupied;
					whiteBishops &= allWhiteOccupied;
					whiteKnights &= allWhiteOccupied;
					allNonWhiteOccupied = ~allWhiteOccupied;
				}
			}
			else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
				offsetBoard[move.to] = Piece.B_ROOK.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				blackPawns ^= fromBit;
				blackRooks ^= toBit;
				allBlackOccupied ^= changedBits;
				allNonBlackOccupied = ~allBlackOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					allWhiteOccupied ^= toBit;
					whiteQueens &= allWhiteOccupied;
					whiteRooks &= allWhiteOccupied;
					whiteBishops &= allWhiteOccupied;
					whiteKnights &= allWhiteOccupied;
					allNonWhiteOccupied = ~allWhiteOccupied;
				}
			}
			else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
				offsetBoard[move.to] = Piece.B_BISHOP.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				blackPawns ^= fromBit;
				blackBishops ^= toBit;
				allBlackOccupied ^= changedBits;
				allNonBlackOccupied = ~allBlackOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					allWhiteOccupied ^= toBit;
					whiteQueens &= allWhiteOccupied;
					whiteRooks &= allWhiteOccupied;
					whiteBishops &= allWhiteOccupied;
					whiteKnights &= allWhiteOccupied;
					allNonWhiteOccupied = ~allWhiteOccupied;
				}
			}
			else {
				offsetBoard[move.to] = Piece.B_KNIGHT.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				blackPawns ^= fromBit;
				blackKnights ^= toBit;
				allBlackOccupied ^= changedBits;
				allNonBlackOccupied = ~allBlackOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					allWhiteOccupied ^= toBit;
					whiteQueens &= allWhiteOccupied;
					whiteRooks &= allWhiteOccupied;
					whiteBishops &= allWhiteOccupied;
					whiteKnights &= allWhiteOccupied;
					allNonWhiteOccupied = ~allWhiteOccupied;
				}
			}
		}
		allOccupied = (allWhiteOccupied | allBlackOccupied);
		allEmpty = ~allOccupied;
	}
	/**
	 * Unmakes a move only on the chess board representations of this Position object.
	 * 
	 * @param move
	 */
	private void unmakeMoveOnBoard(Move move) {
		int enPassantVictimSquare;
		long fromBit = Square.getByIndex(move.from).bitmap;
		long toBit = Square.getByIndex(move.to).bitmap;
		long changedBits = (fromBit | toBit);
		long collChangedBits;
		if (isWhitesTurn) {
			if (move.type == MoveType.NORMAL.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = move.capturedPiece;
				if (move.movedPiece == Piece.W_KING.ind)
					whiteKing = fromBit;
				else if (move.movedPiece == Piece.W_QUEEN.ind)
					whiteQueens ^= changedBits;
				else if (move.movedPiece == Piece.W_ROOK.ind)
					whiteRooks ^= changedBits;
				else if (move.movedPiece == Piece.W_BISHOP.ind)
					whiteBishops ^= changedBits;
				else if (move.movedPiece == Piece.W_KNIGHT.ind)
					whiteKnights ^= changedBits;
				else
					whitePawns ^= changedBits;
				allWhiteOccupied ^= changedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					if (move.capturedPiece == Piece.B_QUEEN.ind)
						blackQueens ^= toBit;
					else if (move.capturedPiece == Piece.B_ROOK.ind)
						blackRooks ^= toBit;
					else if (move.capturedPiece == Piece.B_BISHOP.ind)
						blackBishops ^= toBit;
					else if (move.capturedPiece == Piece.B_KNIGHT.ind)
						blackKnights ^= toBit;
					else
						blackPawns ^= toBit;
					allBlackOccupied ^= toBit;
					allNonBlackOccupied = ~allBlackOccupied;
				}
			}
			else if (move.type == MoveType.SHORT_CASTLING.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = Piece.NULL.ind;
				offsetBoard[Square.H1.ind] = Piece.W_ROOK.ind;
				offsetBoard[Square.F1.ind] = Piece.NULL.ind;
				whiteKing = fromBit;
				collChangedBits = (1L << Square.H1.ind) | (1L << Square.F1.ind);
				whiteRooks ^= collChangedBits;
				allWhiteOccupied ^= (changedBits | collChangedBits);
				allNonWhiteOccupied = ~allWhiteOccupied;
			}
			else if (move.type == MoveType.LONG_CASTLING.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = Piece.NULL.ind;
				offsetBoard[Square.A1.ind] = Piece.W_ROOK.ind;
				offsetBoard[Square.D1.ind] = Piece.NULL.ind;
				whiteKing = fromBit;
				collChangedBits = (1L << Square.A1.ind) | (1L << Square.D1.ind);
				whiteRooks ^= collChangedBits;
				allWhiteOccupied ^= (changedBits | collChangedBits);
				allNonWhiteOccupied = ~allWhiteOccupied;
			}
			else if (move.type == MoveType.EN_PASSANT.ind) {
				enPassantVictimSquare = move.to - 8;
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = Piece.NULL.ind;
				offsetBoard[enPassantVictimSquare] = move.capturedPiece;
				whitePawns ^= changedBits;
				allWhiteOccupied ^= changedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
				collChangedBits = 1L << enPassantVictimSquare;
				blackPawns ^= collChangedBits;
				allBlackOccupied ^= collChangedBits;
				allNonBlackOccupied = ~allBlackOccupied;
			}
			else if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = move.capturedPiece;
				whitePawns ^= fromBit;
				whiteQueens ^= toBit;
				allWhiteOccupied ^= changedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					if (move.capturedPiece == Piece.B_QUEEN.ind)
						blackQueens ^= toBit;
					else if (move.capturedPiece == Piece.B_ROOK.ind)
						blackRooks ^= toBit;
					else if (move.capturedPiece == Piece.B_BISHOP.ind)
						blackBishops ^= toBit;
					else if (move.capturedPiece == Piece.B_KNIGHT.ind)
						blackKnights ^= toBit;
					else
						blackPawns ^= toBit;
					allBlackOccupied ^= toBit;
					allNonBlackOccupied = ~allBlackOccupied;
				}
			}
			else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = move.capturedPiece;
				whitePawns ^= fromBit;
				whiteRooks ^= toBit;
				allWhiteOccupied ^= changedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					if (move.capturedPiece == Piece.B_QUEEN.ind)
						blackQueens ^= toBit;
					else if (move.capturedPiece == Piece.B_ROOK.ind)
						blackRooks ^= toBit;
					else if (move.capturedPiece == Piece.B_BISHOP.ind)
						blackBishops ^= toBit;
					else if (move.capturedPiece == Piece.B_KNIGHT.ind)
						blackKnights ^= toBit;
					else
						blackPawns ^= toBit;
					allBlackOccupied ^= toBit;
					allNonBlackOccupied = ~allBlackOccupied;
				}
			}
			else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = move.capturedPiece;
				whitePawns ^= fromBit;
				whiteBishops ^= toBit;
				allWhiteOccupied ^= changedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					if (move.capturedPiece == Piece.B_QUEEN.ind)
						blackQueens ^= toBit;
					else if (move.capturedPiece == Piece.B_ROOK.ind)
						blackRooks ^= toBit;
					else if (move.capturedPiece == Piece.B_BISHOP.ind)
						blackBishops ^= toBit;
					else if (move.capturedPiece == Piece.B_KNIGHT.ind)
						blackKnights ^= toBit;
					else
						blackPawns ^= toBit;
					allBlackOccupied ^= toBit;
					allNonBlackOccupied = ~allBlackOccupied;
				}
			}
			else {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = move.capturedPiece;
				whitePawns ^= fromBit;
				whiteKnights ^= toBit;
				allWhiteOccupied ^= changedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					if (move.capturedPiece == Piece.B_QUEEN.ind)
						blackQueens ^= toBit;
					else if (move.capturedPiece == Piece.B_ROOK.ind)
						blackRooks ^= toBit;
					else if (move.capturedPiece == Piece.B_BISHOP.ind)
						blackBishops ^= toBit;
					else if (move.capturedPiece == Piece.B_KNIGHT.ind)
						blackKnights ^= toBit;
					else
						blackPawns ^= toBit;
					allBlackOccupied ^= toBit;
					allNonBlackOccupied = ~allBlackOccupied;
				}
			}
		}
		else {
			if (move.type == MoveType.NORMAL.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = move.capturedPiece;
				if (move.movedPiece == Piece.B_KING.ind)
					blackKing = fromBit;
				else if (move.movedPiece == Piece.B_QUEEN.ind)
					blackQueens ^= changedBits;
				else if (move.movedPiece == Piece.B_ROOK.ind)
					blackRooks ^= changedBits;
				else if (move.movedPiece == Piece.B_BISHOP.ind)
					blackBishops ^= changedBits;
				else if (move.movedPiece == Piece.B_KNIGHT.ind)
					blackKnights ^= changedBits;
				else
					blackPawns ^= changedBits;
				allBlackOccupied ^= changedBits;
				allNonBlackOccupied = ~allBlackOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					if (move.capturedPiece == Piece.W_QUEEN.ind)
						whiteQueens ^= toBit;
					else if (move.capturedPiece == Piece.W_ROOK.ind)
						whiteRooks ^= toBit;
					else if (move.capturedPiece == Piece.W_BISHOP.ind)
						whiteBishops ^= toBit;
					else if (move.capturedPiece == Piece.W_KNIGHT.ind)
						whiteKnights ^= toBit;
					else
						whitePawns ^= toBit;
					allWhiteOccupied ^= toBit;
					allNonWhiteOccupied = ~allWhiteOccupied;
				}
			}
			else if (move.type == MoveType.SHORT_CASTLING.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = Piece.NULL.ind;
				offsetBoard[Square.H8.ind] = Piece.B_ROOK.ind;
				offsetBoard[Square.F8.ind] = Piece.NULL.ind;
				blackKing = fromBit;
				collChangedBits = (1L << Square.H8.ind) | (1L << Square.F8.ind);
				blackRooks ^= collChangedBits;
				allBlackOccupied ^= (changedBits | collChangedBits);
				allNonBlackOccupied = ~allBlackOccupied;
			}
			else if (move.type == MoveType.LONG_CASTLING.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = Piece.NULL.ind;
				offsetBoard[Square.A8.ind] = Piece.B_ROOK.ind;
				offsetBoard[Square.D8.ind] = Piece.NULL.ind;
				blackKing = fromBit;
				collChangedBits = (1L << Square.A8.ind) | (1L << Square.D8.ind);
				blackRooks ^= collChangedBits;
				allBlackOccupied ^= (changedBits | collChangedBits);
				allNonBlackOccupied = ~allBlackOccupied;
			}
			else if (move.type == MoveType.EN_PASSANT.ind) {
				enPassantVictimSquare = move.to + 8;
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = Piece.NULL.ind;
				offsetBoard[enPassantVictimSquare] = move.capturedPiece;
				blackPawns ^= changedBits;
				allBlackOccupied ^= changedBits;
				allNonBlackOccupied = ~allBlackOccupied;
				collChangedBits = 1L << enPassantVictimSquare;
				whitePawns ^= collChangedBits;
				allWhiteOccupied ^= collChangedBits;
				allNonWhiteOccupied = ~allWhiteOccupied;
			}
			else if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = move.capturedPiece;
				blackPawns ^= fromBit;
				blackQueens ^= toBit;
				allBlackOccupied ^= changedBits;
				allNonBlackOccupied = ~allBlackOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					if (move.capturedPiece == Piece.W_QUEEN.ind)
						whiteQueens ^= toBit;
					else if (move.capturedPiece == Piece.W_ROOK.ind)
						whiteRooks ^= toBit;
					else if (move.capturedPiece == Piece.W_BISHOP.ind)
						whiteBishops ^= toBit;
					else if (move.capturedPiece == Piece.W_KNIGHT.ind)
						whiteKnights ^= toBit;
					else
						whitePawns ^= toBit;
					allWhiteOccupied ^= toBit;
					allNonWhiteOccupied = ~allWhiteOccupied;
				}
			}
			else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = move.capturedPiece;
				blackPawns ^= fromBit;
				blackRooks ^= toBit;
				allBlackOccupied ^= changedBits;
				allNonBlackOccupied = ~allBlackOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					if (move.capturedPiece == Piece.W_QUEEN.ind)
						whiteQueens ^= toBit;
					else if (move.capturedPiece == Piece.W_ROOK.ind)
						whiteRooks ^= toBit;
					else if (move.capturedPiece == Piece.W_BISHOP.ind)
						whiteBishops ^= toBit;
					else if (move.capturedPiece == Piece.W_KNIGHT.ind)
						whiteKnights ^= toBit;
					else
						whitePawns ^= toBit;
					allWhiteOccupied ^= toBit;
					allNonWhiteOccupied = ~allWhiteOccupied;
				}
			}
			else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = move.capturedPiece;
				blackPawns ^= fromBit;
				blackBishops ^= toBit;
				allBlackOccupied ^= changedBits;
				allNonBlackOccupied = ~allBlackOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					if (move.capturedPiece == Piece.W_QUEEN.ind)
						whiteQueens ^= toBit;
					else if (move.capturedPiece == Piece.W_ROOK.ind)
						whiteRooks ^= toBit;
					else if (move.capturedPiece == Piece.W_BISHOP.ind)
						whiteBishops ^= toBit;
					else if (move.capturedPiece == Piece.W_KNIGHT.ind)
						whiteKnights ^= toBit;
					else
						whitePawns ^= toBit;
					allWhiteOccupied ^= toBit;
					allNonWhiteOccupied = ~allWhiteOccupied;
				}
			}
			else {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = move.capturedPiece;
				blackPawns ^= fromBit;
				blackKnights ^= toBit;
				allBlackOccupied ^= changedBits;
				allNonBlackOccupied = ~allBlackOccupied;
				if (move.capturedPiece != Piece.NULL.ind) {
					if (move.capturedPiece == Piece.W_QUEEN.ind)
						whiteQueens ^= toBit;
					else if (move.capturedPiece == Piece.W_ROOK.ind)
						whiteRooks ^= toBit;
					else if (move.capturedPiece == Piece.W_BISHOP.ind)
						whiteBishops ^= toBit;
					else if (move.capturedPiece == Piece.W_KNIGHT.ind)
						whiteKnights ^= toBit;
					else
						whitePawns ^= toBit;
					allWhiteOccupied ^= toBit;
					allNonWhiteOccupied = ~allWhiteOccupied;
				}
			}
		}
		allOccupied = (allWhiteOccupied | allBlackOccupied);
		allEmpty = ~allOccupied;
	}
	/**
	 * Makes a single move from a LongQueue generated by generateMoves.
	 * 
	 * @param move A Move object that is going to be made in the position.
	 */
	public void makeMove(Move move) {
		long[] temp;
		makeMoveOnBoard(move);
		moveList.add(move);
		unmakeRegisterHistory.add(new UnmakeRegister(whiteCastlingRights, blackCastlingRights, enPassantRights, fiftyMoveRuleClock,
				repetitions, checkers));
		isWhitesTurn = !isWhitesTurn;
		if (isWhitesTurn) {
			// Check castling rights.
			if (whiteCastlingRights == CastlingRights.NONE.ind);
			else if (whiteCastlingRights == CastlingRights.SHORT.ind) {
				if (offsetBoard[Square.E1.ind] != Piece.W_KING.ind || offsetBoard[Square.H1.ind] != Piece.W_ROOK.ind)
					whiteCastlingRights = CastlingRights.NONE.ind;
			}
			else if (whiteCastlingRights == CastlingRights.LONG.ind) {
				if (offsetBoard[Square.E1.ind] != Piece.W_KING.ind || offsetBoard[Square.A1.ind] != Piece.W_ROOK.ind)
					whiteCastlingRights = CastlingRights.NONE.ind;
			}
			else {
				if (offsetBoard[Square.E1.ind] == Piece.W_KING.ind) {
					if (offsetBoard[Square.H1.ind] != Piece.W_ROOK.ind) {
						if (offsetBoard[Square.A1.ind] != Piece.W_ROOK.ind)
							whiteCastlingRights = CastlingRights.NONE.ind;
						else
							whiteCastlingRights = CastlingRights.LONG.ind;
					}
					else if (offsetBoard[Square.A1.ind] != Piece.W_ROOK.ind)
						whiteCastlingRights = CastlingRights.SHORT.ind;
				}
				else
					whiteCastlingRights = CastlingRights.NONE.ind;
			}
			// Check en passant rights.
			enPassantRights = (move.movedPiece == Piece.B_PAWN.ind && move.from - move.to == 16) ? (byte)(move.to%8) : 8;
			// Fifty move rule
			fiftyMoveRuleClock = (short)((move.capturedPiece != Piece.NULL.ind || move.movedPiece == Piece.B_PAWN.ind) ?
					0 : fiftyMoveRuleClock + 1);
		}
		else {
			// Check castling rights.
			if (blackCastlingRights == CastlingRights.NONE.ind);
			else if (blackCastlingRights == CastlingRights.SHORT.ind) {
				if (offsetBoard[Square.E8.ind] != Piece.B_KING.ind || offsetBoard[Square.H8.ind] != Piece.B_ROOK.ind)
					blackCastlingRights = CastlingRights.NONE.ind;
			}
			else if (blackCastlingRights == CastlingRights.LONG.ind) {
				if (offsetBoard[Square.E8.ind] != Piece.B_KING.ind || offsetBoard[Square.A8.ind] != Piece.B_ROOK.ind)
					blackCastlingRights = CastlingRights.NONE.ind;
			}
			else {
				if (offsetBoard[Square.E8.ind] == Piece.B_KING.ind) {
					if (offsetBoard[Square.H8.ind] != Piece.B_ROOK.ind) {
						if (offsetBoard[Square.A8.ind] != Piece.B_ROOK.ind)
							blackCastlingRights = CastlingRights.NONE.ind;
						else
							blackCastlingRights = CastlingRights.LONG.ind;
					}
					else if (offsetBoard[Square.A8.ind] != Piece.B_ROOK.ind)
						blackCastlingRights = CastlingRights.SHORT.ind;
				}
				else
					blackCastlingRights = CastlingRights.NONE.ind;
			}
			// Check en passant rights.
			enPassantRights = (move.movedPiece == Piece.W_PAWN.ind && move.to - move.from == 16) ? (byte)(move.to%8) : 8;
			// Fifty move rule
			fiftyMoveRuleClock = (short)((move.capturedPiece != Piece.NULL.ind || move.movedPiece == Piece.W_PAWN.ind) ?
					0 : fiftyMoveRuleClock + 1);
				
		}
		checkers = getCheckers();
		isInCheck = checkers != 0;
		halfMoveIndex++;
		if (keyHistory.length - halfMoveIndex <= 2) {
			temp = keyHistory;
			keyHistory = new long[keyHistory.length + 50];
			for (int i = 0; i < temp.length; i++)
				keyHistory[i] = temp[i];
			temp = pawnKeyHistory;
			pawnKeyHistory = new long[pawnKeyHistory.length + 50];
			for (int i = 0; i < temp.length; i++)
				pawnKeyHistory[i] = temp[i];
		}
		gen.updateKeys(this);
		keyHistory[halfMoveIndex] = key;
		pawnKeyHistory[halfMoveIndex] = pawnKey;
		repetitions = 1;
		if (fiftyMoveRuleClock >= 4) {
			for (int i = halfMoveIndex - 2; i >= (halfMoveIndex - fiftyMoveRuleClock); i -= 2) {
				if (keyHistory[i] == key)
					repetitions++;
			}
		}
	}
	/**
	 * Makes a null move that can be taken back without breaking the game.
	 */
	public void makeNullMove() {
		long[] temp;
		unmakeRegisterHistory.add(new UnmakeRegister(whiteCastlingRights, blackCastlingRights, enPassantRights, fiftyMoveRuleClock,
				repetitions, checkers));
		isWhitesTurn = !isWhitesTurn;
		enPassantRights = EnPassantRights.NONE.ind;
		if (isWhitesTurn) {
			if (whiteCastlingRights == CastlingRights.NONE.ind);
			else if (whiteCastlingRights == CastlingRights.SHORT.ind) {
				if (offsetBoard[Square.E1.ind] != Piece.W_KING.ind || offsetBoard[Square.H1.ind] != Piece.W_ROOK.ind)
					whiteCastlingRights = CastlingRights.NONE.ind;
			}
			else if (whiteCastlingRights == CastlingRights.LONG.ind) {
				if (offsetBoard[Square.E1.ind] != Piece.W_KING.ind || offsetBoard[Square.A1.ind] != Piece.W_ROOK.ind)
					whiteCastlingRights = CastlingRights.NONE.ind;
			}
			else {
				if (offsetBoard[Square.E1.ind] == Piece.W_KING.ind) {
					if (offsetBoard[Square.H1.ind] != Piece.W_ROOK.ind) {
						if (offsetBoard[Square.A1.ind] != Piece.W_ROOK.ind)
							whiteCastlingRights = CastlingRights.NONE.ind;
						else
							whiteCastlingRights = CastlingRights.LONG.ind;
					}
					else if (offsetBoard[Square.A1.ind] != Piece.W_ROOK.ind)
						whiteCastlingRights = CastlingRights.SHORT.ind;
				}
				else
					whiteCastlingRights = CastlingRights.NONE.ind;
			}
		}
		else {
			if (blackCastlingRights == CastlingRights.NONE.ind);
			else if (blackCastlingRights == CastlingRights.SHORT.ind) {
				if (offsetBoard[Square.E8.ind] != Piece.B_KING.ind || offsetBoard[Square.H8.ind] != Piece.B_ROOK.ind)
					blackCastlingRights = CastlingRights.NONE.ind;
			}
			else if (blackCastlingRights == CastlingRights.LONG.ind) {
				if (offsetBoard[Square.E8.ind] != Piece.B_KING.ind || offsetBoard[Square.A8.ind] != Piece.B_ROOK.ind)
					blackCastlingRights = CastlingRights.NONE.ind;
			}
			else {
				if (offsetBoard[Square.E8.ind] == Piece.B_KING.ind) {
					if (offsetBoard[Square.H8.ind] != Piece.B_ROOK.ind) {
						if (offsetBoard[Square.A8.ind] != Piece.B_ROOK.ind)
							blackCastlingRights = CastlingRights.NONE.ind;
						else
							blackCastlingRights = CastlingRights.LONG.ind;
					}
					else if (offsetBoard[Square.A8.ind] != Piece.B_ROOK.ind)
						blackCastlingRights = CastlingRights.SHORT.ind;
				}
				else
					blackCastlingRights = CastlingRights.NONE.ind;
			}
		}
		moveList.add(null);
		halfMoveIndex++;
		if (keyHistory.length - halfMoveIndex <= 2) {
			temp = keyHistory;
			keyHistory = new long[keyHistory.length + 50];
			for (int i = 0; i < temp.length; i++)
				keyHistory[i] = temp[i];
			temp = pawnKeyHistory;
			pawnKeyHistory = new long[pawnKeyHistory.length + 50];
			for (int i = 0; i < temp.length; i++)
				pawnKeyHistory[i] = temp[i];
		}
		gen.updateKeys(this);
		keyHistory[halfMoveIndex] = key;
		pawnKeyHistory[halfMoveIndex] = pawnKey;
		repetitions = 1;
	}
	/**
	 * Reverts the state of the instance to that before the last move made in every aspect necessary for the traversal of the game tree.
	 * Used within the engine.
	 */
	public void unmakeMove() {
		Move move = moveList.pop();
		isWhitesTurn = !isWhitesTurn;
		if (move != null) unmakeMoveOnBoard(move);
		UnmakeRegister positionInfo = unmakeRegisterHistory.pop();
		whiteCastlingRights = positionInfo.whiteCastlingRights;
		blackCastlingRights = positionInfo.blackCastlingRights;
		enPassantRights = positionInfo.enPassantRights;
		fiftyMoveRuleClock = positionInfo.fiftyMoveRuleClock;
		repetitions = positionInfo.repetitions;
		checkers = positionInfo.checkers;
		isInCheck = checkers != 0;
		keyHistory[halfMoveIndex] = 0;
		pawnKeyHistory[halfMoveIndex] = 0;
		key = keyHistory[--halfMoveIndex];
		pawnKey = pawnKeyHistory[halfMoveIndex];
	}
	/**
	 * Creates and returns a string of the move in Standard Algebraic Notation. The move is assumed to be legal in the position on which
	 * the method is called.
	 * 
	 * @param move
	 * @return
	 */
	public String toSAN(Move move) {
		String san, movedPiece, capture, origin, destFile, destRank;
		Piece mPiece;
		MoveSetDatabase mT;
		long possOriginSqrs, movablePieces;
		if (move == null)
			return null;
		if (move.type == MoveType.SHORT_CASTLING.ind)
			return "O-O";
		else if (move.type == MoveType.LONG_CASTLING.ind)
			return "O-O-O";
		destRank = Integer.toString(move.to/8 + 1);
		destFile = Character.toString((char)(move.to%8 + 'a'));
		mPiece = Piece.getByNumericNotation(move.movedPiece);
		if (mPiece == Piece.W_PAWN || mPiece == Piece.B_PAWN)
			movedPiece  = "";
		else
			movedPiece  = Character.toString(mPiece.letter).toUpperCase();
		capture = move.capturedPiece == Piece.NULL.ind ? "" : "x";
		mT = MoveSetDatabase.getByIndex(move.to);
		movablePieces = ~getPinnedPieces(isWhitesTurn);
		switch (mPiece) {
			case W_KING: {
				possOriginSqrs = 0;
				origin = "";
			}
			break;
			case W_QUEEN: {
				possOriginSqrs = mT.getQueenMoveSet(whiteQueens & movablePieces, allOccupied);
				origin = null;
			}
			break;
			case W_ROOK: {
				possOriginSqrs = mT.getRookMoveSet(whiteRooks & movablePieces, allOccupied);
				origin = null;
			}
			break;
			case W_BISHOP: {
				possOriginSqrs = mT.getBishopMoveSet(whiteBishops & movablePieces, allOccupied);
				origin = null;
			}
			break;
			case W_KNIGHT: {
				possOriginSqrs = mT.getKnightMoveSet(whiteKnights & movablePieces);
				origin = null;
			}
			break;
			case W_PAWN: {
				if (move.capturedPiece != Piece.NULL.ind) {
					possOriginSqrs = mT.getBlackPawnCaptureSet(whitePawns & movablePieces);
					if (BitOperations.getHammingWeight(possOriginSqrs) == 1)
						origin = "";
					else
						origin = Character.toString((char)(move.from%8 + 'a'));
				}
				else {
					possOriginSqrs = 0;
					origin = "";
				}
			}
			break;
			case B_KING: {
				possOriginSqrs = 0;
				origin = "";
			}
			break;
			case B_QUEEN: {
				possOriginSqrs = mT.getQueenMoveSet(blackQueens & movablePieces, allOccupied);
				origin = null;
			}
			break;
			case B_ROOK: {
				possOriginSqrs = mT.getRookMoveSet(blackRooks & movablePieces, allOccupied);
				origin = null;
			}
			break;
			case B_BISHOP: {
				possOriginSqrs = mT.getBishopMoveSet(blackBishops & movablePieces, allOccupied);
				origin = null;
			}
			break;
			case B_KNIGHT: {
				possOriginSqrs = mT.getKnightMoveSet(blackKnights & movablePieces);
				origin = null;
			}
			break;
			case B_PAWN: {
				if (move.capturedPiece != Piece.NULL.ind) {
					possOriginSqrs = mT.getWhitePawnCaptureSet(blackPawns & movablePieces);
					if (BitOperations.getHammingWeight(possOriginSqrs) == 1)
						origin = "";
					else
						origin = Character.toString((char)(move.from%8 + 'a'));
				}
				else {
					possOriginSqrs = 0;
					origin = "";
				}
			}
			break;
			default:
				return null;
		}
		if (origin == null) {
			if (BitOperations.getHammingWeight(possOriginSqrs) == 1)
				origin = "";
			else if (BitOperations.getHammingWeight(File.getBySquareIndex(move.from).bitmap & possOriginSqrs) == 1)
				origin = Character.toString((char)(move.from%8 + 'a'));
			else if (BitOperations.getHammingWeight(Rank.getBySquareIndex(move.from).bitmap & possOriginSqrs) == 1)
				origin = Integer.toString(move.from/8 + 1);
			else
				origin = Character.toString((char)(move.from%8 + 'a')) + Integer.toString(move.from/8 + 1);
		}
		san = movedPiece + origin + capture + destFile + destRank;
		if (move.type == MoveType.EN_PASSANT.ind)
			return san + "e.p.";
		else if (move.type == MoveType.PROMOTION_TO_QUEEN.ind)
			return san + "=Q";
		else if (move.type == MoveType.PROMOTION_TO_ROOK.ind)
			return san + "=R";
		else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind)
			return san + "=B";
		else if (move.type == MoveType.PROMOTION_TO_KNIGHT.ind)
			return san + "=N";
		else
			return san;
	}
	/**
	 * Returns a string representation of the position's move list in SAN with six full moves per line.
	 * 
	 * @return
	 */
	public String moveListToSAN() {
		String moveListSAN = "";
		boolean printRound = true;
		int roundNum = 0;
		Move move;
		List<Move> moveStack = new Stack<>();
		while (moveList.hasNext()) {
			moveStack.add(moveList.next());
			unmakeMove();
		}
		while (moveStack.hasNext()) {
			if (printRound)
				moveListSAN += ++roundNum + ". ";
			move = moveStack.next();
			moveListSAN += toSAN(move) + " ";
			makeMove(move);
			printRound = !printRound;
			if (roundNum%6 == 0 && printRound)
				moveListSAN += "\n";
		}
		return moveListSAN;
	}
	/**
	 * Draws and returns a detailed String of the chess board based on the offset board. Pieces are represented according to the standard
	 * English piece-letter notation.
	 * 
	 * @param pos
	 * @return
	 */
	public String toFancyBoardString() {
		String out = "";
		for (int i = 16; i >= 0; i--) {
			if (i%2 == 0) {
				out += "  ";
				for (int j = 0; j < 17; j++) {
					if (j%2 == 0)
						out += "+";
					else
						out += "---";
				}
			}
			else {
				out += (i + 1)/2 + " ";
				for (int j = 0; j < 17; j++) {
					if (j%2 == 0)
						out += "|";
					else
						out += " " + Piece.getByNumericNotation(offsetBoard[(i - 1)*4 + j/2]).letter + " ";
				}
			}
			out += "\n";
		}
		out += "  ";
		for (int i = 0; i < 8; i++) {
			out += "  " + (char)('A' + i) + " ";
		}
		out += "\n";
		return out;
	}
	/**
	 * Returns a detailed String representation of a position objects state.
	 * 
	 * @param pos
	 * @return
	 */
	public String toDetailedString() {
		ByteStack checkers;
		String out = "";
		out += toFancyBoardString() + "\n";
		out += String.format("%-23s ", "To move: ");
		if (isWhitesTurn)
			out += "white\n";
		else
			out += "black\n";
		if (isInCheck) {
			out += String.format("%-23s ", "Checker(s): ");
			checkers = BitOperations.serialize(this.checkers);
			while (checkers.hasNext())
				out += Square.getByIndex(checkers.next()).toString() + " ";
			out += "\n";
		}
		out += String.format("%-23s ", "Castling rights: ");
		out += CastlingRights.toFEN(CastlingRights.getByIndex(whiteCastlingRights), CastlingRights.getByIndex(blackCastlingRights)) + "\n";
		out += String.format("%-23s ", "En passant rights: ");
		out += EnPassantRights.getByIndex(enPassantRights).toString() + "\n";
		out += String.format("%-23s " + halfMoveIndex + "\n", "Half-move index: ");
		out += String.format("%-23s " + fiftyMoveRuleClock + "\n", "Fifty-move rule clock: ");
		out += String.format("%-23s " + BitOperations.toHexLiteral(key) + "\n", "Hash key: ");
		out += String.format("%-23s ", "Move history: " + moveListToSAN() + "\n");
		return out;
	}
	/**
	 * Returns the current state of a Board object as a one-line String in FEN-notation. The FEN-notation consists of six fields separated
	 * by spaces. The six fields are as follows:
	 * 		1. board position
	 * 		2. color to move
	 * 		3. castling rights
	 * 		4. en passant rights
	 * 		5. fifty-move rule clock
	 * 		6. fullmove number
	 */
	@Override
	public String toString() {
		String fen = "";
		int piece, emptyCount;
		for (int i = 7; i >= 0; i--) {
			emptyCount = 0;
			for (int j = 0; j < 8; j++) {
				piece = offsetBoard[i*8 + j];
				if (piece == 0)
					emptyCount++;
				else {
					if (emptyCount != 0)
						fen += emptyCount;
					emptyCount = 0;
					fen += Piece.getByNumericNotation(piece).letter;
				}
			}
			if (emptyCount != 0)
				fen += emptyCount;
			if (i != 0)
				fen += '/';
		}
		fen += ' ';
		if (isWhitesTurn)
			fen += 'w';
		else
			fen += 'b';
		fen += ' ';
		fen += CastlingRights.toFEN(CastlingRights.getByIndex(whiteCastlingRights), CastlingRights.getByIndex(blackCastlingRights));
		fen += ' ';
		fen += EnPassantRights.getByIndex(enPassantRights).toString();
		if (enPassantRights != EnPassantRights.NONE.ind) {
			if (isWhitesTurn)
				fen += 6;
			else
				fen += 3;
		}
		fen += ' ';
		fen += fiftyMoveRuleClock;
		fen += ' ';
		fen += 1 + halfMoveIndex/2;
		return fen;
	}
	/**
	 * Runs a perft test to the given depth and returns the number of leaf nodes the traversed game tree had. It is used mainly for move
	 * generation and move making speed benchmarking; and bug detection by comparing the returned values to validated results.
	 * 
	 * @param depth
	 * @return
	 */
	public long perft(int depth) {
		Queue<Move> moves;
		Move move;
		long leafNodes = 0;
		if (depth == 0)
			return 1;
		moves = generateAllMoves();
		while (moves.hasNext()) {
			move = moves.next();
			makeMove(move);
			leafNodes += perft(depth - 1);
			unmakeMove();
		}
		return leafNodes;
	}
	/**
	 * Runs a perft test with staged move generation, first generating only the material moves such as captures and promotions and then
	 * proceeding to generate the non-material moves i.e. the rest.
	 * 
	 * @param depth
	 * @return
	 */
	public long materialStagedPerft(int depth) {
		Queue<Move> materialMoves, nonMaterialMoves;
		Move move;
		long leafNodes = 0;
		if (depth == 0)
			return 1;
		materialMoves = generateMaterialMoves();
		while (materialMoves.hasNext()) {
			move = materialMoves.next();
			makeMove(move);
			leafNodes += materialStagedPerft(depth - 1);
			unmakeMove();
		}
		nonMaterialMoves = generateNonMaterialMoves();
		while (nonMaterialMoves.hasNext()) {
			move = nonMaterialMoves.next();
			makeMove(move);
			leafNodes += materialStagedPerft(depth - 1);
			unmakeMove();
		}
		return leafNodes;
	}
	/**
	 * Runs a perft test faster than the standard method. Instead of making and unmaking the moves leading to the leafnodes, it simply
	 * returns the number of generated moves from the nodes at depth 1. More suitable for benchmarking move generation.
	 * 
	 * @param depth
	 * @return
	 */
	public long quickPerft(int depth) {
		Queue<Move> moves;
		Move move;
		long leafNodes = 0;
		moves = generateAllMoves();
		if (depth == 1)
			return moves.length();
		while (moves.hasNext()) {
			move = moves.next();
			makeMove(move);
			leafNodes += quickPerft(depth - 1);
			unmakeMove();
		}
		return leafNodes;
	}
	/**
	 * Runs a quick perft test (not making and unmaking leaf moves, just returning their count) with staged move generation, first
	 * generating only the material moves such as captures and promotions and then proceeding to generate the non-material moves i.e.
	 * the rest.
	 * 
	 * @param depth
	 * @return
	 */
	public long materialStagedQuickPerft(int depth) {
		Queue<Move> materialMoves, nonMaterialMoves;
		Move move;
		long leafNodes = 0;
		if (depth == 1)
			return generateMaterialMoves().length() + generateNonMaterialMoves().length();
		materialMoves = generateMaterialMoves();
		while (materialMoves.hasNext()) {
			move = materialMoves.next();
			makeMove(move);
			leafNodes += materialStagedQuickPerft(depth - 1);
			unmakeMove();
		}
		nonMaterialMoves = generateNonMaterialMoves();
		while (nonMaterialMoves.hasNext()) {
			move = nonMaterialMoves.next();
			makeMove(move);
			leafNodes += materialStagedQuickPerft(depth - 1);
			unmakeMove();
		}
		return leafNodes;
	}
}
