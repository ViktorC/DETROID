package main.java.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import main.java.engine.Bitboard.*;
import main.java.util.*;

/**
 * A bit board based class whose object holds information amongst others on the current board position, on all the previous moves and positions,
 * on castling and en passant rights, and on the player to move. It uses a pre-calculated 'magic' move database to avoid the cost of computing
 * the possible move sets of sliding pieces on the fly.
 * 
 * The main functions include:
 * {@link #getMoves() getMoves}
 * {@link #makeMove(Move) makeMove}
 * {@link #unmakeMove() unmakeMove}
 * {@link #perft(int) perft}
 *  
 * @author Viktor
 * 
 */
class Position implements Copiable<Position>, Hashable {
	
	/**
	 * A FEN string for the starting chess position.
	 */
	public final static String START_POSITION_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	
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
	
	/**The number of half moves made. */
	int halfMoveIndex;
	/**The number of half moves made since the last pawn move or capture. */
	byte fiftyMoveRuleClock;
	
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
	ArrayDeque<Move> moveList;
	/**A stack history of castling rights, en passant rights, fifty-move rule clock, repetitions, and check info. */
	ArrayDeque<UnmakeMoveRecord> unmakeRegisterHistory;
	
	/**A Zobrist key generator instance. */
	ZobristKeyGenerator gen;
	/**A Zobrist key that is fairly close to a unique representation of the state of the Position instance in one 64 bit number. */
	long key;
	/**All the positions that have occurred so far represented in Zobrist keys. */
	long[] keyHistory;
	
	/**
	 * It parses a FEN-String and initializes a Position instance based on it.
	 * Beside standard six-field FEN-Strings, it also accepts four-field Strings without the fifty-move rule clock and the move index.
	 * 
	 * @param fen
	 * @return
	 * @throws ChessParseException 
	 */
	static Position parse(String fen) throws ChessParseException {
		Position pos = new Position();
		String[] fenFields = fen.split(" "), ranks;
		String board, turn, castling, enPassant, rank;
		char piece;
		int pieceNum, index = 0, fiftyMoveRuleClock, moveIndex;
		pos.isWhitesTurn = true;
		pos.checkers = 0;
		pos.isInCheck = false;
		pos.halfMoveIndex = 0;
		pos.fiftyMoveRuleClock = 0;
		pos.enPassantRights = EnPassantRights.NONE.ind;
		pos.whiteCastlingRights = CastlingRights.ALL.ind;
		pos.blackCastlingRights = CastlingRights.ALL.ind;
		if (fenFields.length == 6) {
			try {
				fiftyMoveRuleClock = Integer.parseInt(fenFields[4]);
				pos.fiftyMoveRuleClock = (byte) Math.max(0, fiftyMoveRuleClock);
			} catch (NumberFormatException e) { throw new ChessParseException("The fifty-move rule clock field of the FEN-string does not conform to the" +
						"standards. Parsing not possible."); }
			try {
				moveIndex = (Integer.parseInt(fenFields[5]) - 1)*2;
				if (!pos.isWhitesTurn)
					moveIndex++;
				if (moveIndex >= 0)
					pos.halfMoveIndex = moveIndex;
				else
					pos.halfMoveIndex = 0;
			} catch (NumberFormatException e) {
				throw new ChessParseException("The move index field does not conform to the standards. Parsing not possible.");
			}
		} else if (fenFields.length != 4)
			throw new ChessParseException("The FEN-String has an unallowed number of fields.");
		board = fenFields[0];
		turn = fenFields[1];
		castling = fenFields[2];
		enPassant = fenFields[3];
		ranks = board.split("/");
		if (ranks.length != 8)
			throw new ChessParseException("The board position representation does not have eight ranks.");
		pos.offsetBoard = new byte[64];
		for (int i = 0; i < 64; i++)
			pos.offsetBoard[i] = Piece.NULL.ind;
		for (int i = 7; i >= 0; i--) {
			rank = ranks[i];
			for (int j = 0; j < rank.length(); j++) {
				piece = rank.charAt(j);
				pieceNum = piece - '0';
				if (pieceNum >= 0 && pieceNum <= 8)
					index += pieceNum;
				else {
					pos.offsetBoard[index] = Piece.parse(piece).ind;
					switch (piece) {
						case 'K':
							pos.whiteKing = Square.getByIndex(index).bit;
						break;
						case 'Q':
							pos.whiteQueens	|= Square.getByIndex(index).bit;
						break;
						case 'R':
							pos.whiteRooks |= Square.getByIndex(index).bit;
						break;
						case 'B':
							pos.whiteBishops |= Square.getByIndex(index).bit;
						break;
						case 'N':
							pos.whiteKnights |= Square.getByIndex(index).bit;
						break;
						case 'P':
							pos.whitePawns |= Square.getByIndex(index).bit;
						break;
						case 'k':
							pos.blackKing = Square.getByIndex(index).bit;
						break;
						case 'q':
							pos.blackQueens |= Square.getByIndex(index).bit;
						break;
						case 'r':
							pos.blackRooks |= Square.getByIndex(index).bit;
						break;
						case 'b':
							pos.blackBishops |= Square.getByIndex(index).bit;
						break;
						case 'n':
							pos.blackKnights |= Square.getByIndex(index).bit;
						break;
						case 'p':
							pos.blackPawns |= Square.getByIndex(index).bit;
					}
					index++;
				}
			}
		}
		pos.allWhiteOccupied = pos.whiteKing | pos.whiteQueens | pos.whiteRooks | pos.whiteBishops | pos.whiteKnights | pos.whitePawns;
		pos.allBlackOccupied = pos.blackKing | pos.blackQueens | pos.blackRooks | pos.blackBishops | pos.blackKnights | pos.blackPawns;
		pos.allNonWhiteOccupied = ~pos.allWhiteOccupied;
		pos.allNonBlackOccupied = ~pos.allBlackOccupied;
		pos.allOccupied	= pos.allWhiteOccupied | pos.allBlackOccupied;
		pos.allEmpty = ~pos.allOccupied;
		if (turn.toLowerCase().compareTo("w") == 0)
			pos.isWhitesTurn = true;
		else
			pos.isWhitesTurn = false;
		if (castling.equals("-")) {
			pos.whiteCastlingRights = CastlingRights.NONE.ind;
			pos.blackCastlingRights = CastlingRights.NONE.ind;
		}
		if (castling.length() < 1 || castling.length() > 4)
			throw new ChessParseException("Invalid length");
		if (castling.contains("K")) {
			if (castling.contains("Q"))
				pos.whiteCastlingRights = CastlingRights.ALL.ind;
			else
				pos.whiteCastlingRights = CastlingRights.SHORT.ind;
		} else if (castling.contains("Q"))
			pos.whiteCastlingRights = CastlingRights.LONG.ind;
		else
			pos.whiteCastlingRights = CastlingRights.NONE.ind;
		if (castling.contains("k")) {
			if (castling.contains("q"))
				pos.blackCastlingRights = CastlingRights.ALL.ind;
			else
				pos.blackCastlingRights = CastlingRights.SHORT.ind;
		} else if (castling.contains("q"))
			pos.blackCastlingRights = CastlingRights.LONG.ind;
		else
			pos.blackCastlingRights = CastlingRights.NONE.ind;
		if (enPassant.length() > 2)
			throw new ChessParseException();
		if (enPassant.equals("-"))
			pos.enPassantRights = EnPassantRights.NONE.ind;
		else
			pos.enPassantRights = EnPassantRights.values()[enPassant.toLowerCase().charAt(0) - 'a'].ind;
		pos.checkers = pos.getCheckers();
		pos.isInCheck = pos.checkers != 0;
		pos.key = pos.gen.generateHashKey(pos);
		if (pos.halfMoveIndex >= pos.keyHistory.length) {
			int keyHistLength = pos.keyHistory.length;
			while (keyHistLength <= pos.halfMoveIndex)
				keyHistLength += (keyHistLength >> 1);
			pos.keyHistory = new long[keyHistLength];
		}
		pos.keyHistory[pos.halfMoveIndex] = pos.key;
		return pos;
	}
	/**Initializes a default, empty Position instance.*/
	private Position() {
		moveList = new ArrayDeque<Move>();
		unmakeRegisterHistory = new ArrayDeque<UnmakeMoveRecord>();
		gen = ZobristKeyGenerator.getInstance();
		keyHistory = new long[32]; // Factor of two.
	}
	private Position(Position pos) {
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
		offsetBoard = new byte[pos.offsetBoard.length];
		for (int i = 0; i < pos.offsetBoard.length; i++)
			offsetBoard[i] = pos.offsetBoard[i];
		keyHistory = new long[pos.keyHistory.length];
		for (int i = 0; i < pos.keyHistory.length; i++)
			keyHistory[i] = pos.keyHistory[i];
		gen = pos.gen;
		moveList = new ArrayDeque<>();
		for (Move m : pos.moveList)
			moveList.add(m);
		unmakeRegisterHistory = new ArrayDeque<>();
		for (UnmakeMoveRecord u : pos.unmakeRegisterHistory)
			unmakeRegisterHistory.add(u);
	}
	/**
	 * Returns the number of times the current position has already occurred before. If the position has already occurred before within the last
	 * x number of moves specified by sensitiveHalfMoveWindow, the maximum integer value is returned.
	 * 
	 * @param sensitiveHalfMoveWindow
	 * @return
	 */
	int getNumberOfRepetitions(int sensitiveHalfMoveWindow) {
		int repetitions = 0;
		if (fiftyMoveRuleClock >= 4) {
			for (int i = halfMoveIndex - 4; i >= halfMoveIndex - fiftyMoveRuleClock; i -= 2) {
				if (keyHistory[i] == key) {
					repetitions++;
					if (i > halfMoveIndex - sensitiveHalfMoveWindow) {
						repetitions = Integer.MAX_VALUE;
						break;
					}
				}
			}
		}
		return repetitions;
	}
	/**
	 * Returns the number of pieces on the board.
	 * 
	 * @return
	 */
	int getNumberOfPieces() {
		return BitOperations.hammingWeight(allOccupied);
	}
	/**
	 * Returns an object containing all relevant information about the last move made. If the move history list is empty, it returns null.
	 * 
	 * @return
	 */
	Move getLastMove() {
		return moveList.peekFirst();
	}
	/**
	 * Returns an object containing some information about the previous position.
	 * 
	 * @return
	 */
	UnmakeMoveRecord getUnmakeRegister() {
		return unmakeRegisterHistory.peekFirst();
	}
	/**
	 * Returns a 64 bit Zobrist hash key for the pawn-king structure on the board.
	 * 
	 * @return
	 */
	long getPawnKingHashKey() {
		return gen.generatePawnKingHashKey(this);
	}
	/**
	 * Returns whether there are any pieces on the board other than pawns and kings.
	 * 
	 * @return
	 */
	boolean areTherePiecesOtherThanKingsAndPawns() {
		return allOccupied != (whitePawns | blackPawns | whiteKing | blackKing);
	}
	/**
	 * Returns a bitmap representing all the squares on which the pieces are of the colour defined by byWhite and in the current position could
	 * legally be moved to the supposedly enemy occupied square specified by sqrInd.
	 * 
	 * @param sqrInd
	 * @param byWhite
	 * @return
	 */
	long getAttackers(int sqrInd, boolean byWhite) {
		long attackers = 0;
		MoveSetDatabase dB = MoveSetDatabase.getByIndex(sqrInd);
		if (byWhite) {
			attackers = whiteKing & dB.kingMoveMask;
			attackers |= whiteKnights & dB.knightMoveMask;
			attackers |= whitePawns & dB.pawnBlackCaptureMoveMask;
			attackers |= (whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied);
			attackers |= (whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
			if (enPassantRights != EnPassantRights.NONE.ind && sqrInd == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights)
				attackers |= whitePawns & dB.kingMoveMask & Rank.R5.bits;
		} else {
			attackers = blackKing & dB.kingMoveMask;
			attackers |= blackKnights & dB.knightMoveMask;
			attackers |= blackPawns & dB.pawnWhiteCaptureMoveMask;
			attackers |= (blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied);
			attackers |= (blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied);
			if (enPassantRights != EnPassantRights.NONE.ind && sqrInd == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights)
				attackers |= blackPawns & dB.kingMoveMask & Rank.R4.bits;
		}
		return attackers;
	}
	/**
	 * Returns whether there are any pieces of the color defined by byWhite that could be, in the current position, legally moved to the
	 * supposedly enemy occupied square specified by sqrInd.
	 * 
	 * @param sqrInd
	 * @param byWhite
	 * @return
	 */
	boolean isAttacked(int sqrInd, boolean byWhite) {
		MoveSetDatabase dB = MoveSetDatabase.getByIndex(sqrInd);
		if (byWhite) {
			return ((whiteKing & dB.kingMoveMask) != 0 ||
					(whiteKnights & dB.knightMoveMask) != 0 ||
					(whitePawns & dB.pawnBlackCaptureMoveMask) != 0 ||
					((whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied)) != 0 ||
					((whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied)) != 0 || 
					(offsetBoard[sqrInd] == Piece.B_PAWN.ind && enPassantRights != EnPassantRights.NONE.ind &&
					sqrInd == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights && (whitePawns & dB.kingMoveMask & Rank.R5.bits) != 0));
		} else {
			return ((blackKing & dB.kingMoveMask) != 0 ||
					(blackKnights & dB.knightMoveMask) != 0 ||
					(blackPawns & dB.pawnWhiteCaptureMoveMask) != 0 ||
					((blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied)) != 0 ||
					((blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied)) != 0 ||
					(offsetBoard[sqrInd] == Piece.W_PAWN.ind && enPassantRights != EnPassantRights.NONE.ind &&
					sqrInd == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights && (blackPawns & dB.kingMoveMask & Rank.R4.bits) != 0));
		}
	}
	/**
	 * Returns a long representing all the squares on which the pieces are of the colour defined by byWhite and in the current position could
	 * legally be moved to the supposedly empty square specified by sqrInd.
	 * 
	 * @param sqrInd
	 * @param byWhite
	 * @return
	 */
	long getBlockerCandidates(int sqrInd, boolean byWhite) {
		long blockerCandidates = 0;
		long sqrBit = 1L << sqrInd;
		long blackPawnAdvance = (sqrBit >>> 8), whitePawnAdvance = (sqrBit << 8);
		if (byWhite) {
			MoveSetDatabase dB = MoveSetDatabase.getByIndex(sqrInd);
			blockerCandidates |= whiteKnights & dB.knightMoveMask;
			blockerCandidates |= whitePawns & blackPawnAdvance;
			if ((sqrBit & Rank.R4.bits) != 0 && (allEmpty & blackPawnAdvance) != 0)
				blockerCandidates |= whitePawns & (blackPawnAdvance >>> 8);
			blockerCandidates |= (whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied);
			blockerCandidates |= (whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
			if (enPassantRights == sqrInd%8 && (sqrBit & Rank.R6.bits) != 0)
				blockerCandidates |=  whitePawns & dB.pawnBlackCaptureMoveMask;
		} else {
			MoveSetDatabase dB = MoveSetDatabase.getByIndex(sqrInd);
			blockerCandidates |= blackKnights & dB.knightMoveMask;
			blockerCandidates |= blackPawns & whitePawnAdvance;
			if ((sqrBit & Rank.R5.bits) != 0 && (allEmpty & whitePawnAdvance) != 0)
				blockerCandidates |= blackPawns & (whitePawnAdvance << 8);
			blockerCandidates |= (blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied);
			blockerCandidates |= (blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied);
			if (enPassantRights == sqrInd%8 && (sqrBit & Rank.R3.bits) != 0)
				blockerCandidates |=  blackPawns & dB.pawnWhiteCaptureMoveMask;
		}
		return blockerCandidates;
	}
	/**
	 * Returns a bitmap representing the attackers of the color to move's king.
	 * 
	 * @return
	 */
	long getCheckers() {
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
		} else {
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
	 * Returns a long representing all the squares on which there are pinned pieces in the current position for the specified side. A pinned piece is one that when
	 * moved would expose its king to a check.
	 * 
	 * @param white
	 * @return
	 */
	long getPinnedPieces(boolean white) {
		long rankPos, rankNeg, filePos, fileNeg, diagonalPos, diagonalNeg, antiDiagonalPos, antiDiagonalNeg;
		long straightSliders, diagonalSliders, pinnedPiece, pinnedPieces = 0;
		Rays attRayMask;
		if (white) {
			attRayMask = Rays.getByIndex(BitOperations.indexOfBit(whiteKing));
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
			if ((pinnedPiece = BitOperations.getLSBit(rankPos) & allWhiteOccupied) != 0 &&
					(BitOperations.getLSBit(rankPos^pinnedPiece) & straightSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getLSBit(filePos) & allWhiteOccupied) != 0 &&
					(BitOperations.getLSBit(filePos^pinnedPiece) & straightSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos) & allWhiteOccupied) != 0 &&
					(BitOperations.getLSBit(diagonalPos^pinnedPiece) & diagonalSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) & allWhiteOccupied) != 0 &&
					(BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg) & allWhiteOccupied) != 0 &&
					(BitOperations.getMSBit(rankNeg^pinnedPiece) & straightSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg) & allWhiteOccupied) != 0 &&
					(BitOperations.getMSBit(fileNeg^pinnedPiece) & straightSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg) & allWhiteOccupied) != 0 &&
					(BitOperations.getMSBit(diagonalNeg^pinnedPiece) & diagonalSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getMSBit(antiDiagonalNeg) & allWhiteOccupied) != 0 &&
					(BitOperations.getMSBit(antiDiagonalNeg^pinnedPiece) & diagonalSliders) != 0)
				pinnedPieces |= pinnedPiece;
		} else {
			attRayMask = Rays.getByIndex(BitOperations.indexOfBit(blackKing));
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
			if ((pinnedPiece = BitOperations.getLSBit(rankPos) & allBlackOccupied) != 0 &&
					(BitOperations.getLSBit(rankPos^pinnedPiece) & straightSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getLSBit(filePos) & allBlackOccupied) != 0 &&
					(BitOperations.getLSBit(filePos^pinnedPiece) & straightSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos) & allBlackOccupied) != 0 &&
					(BitOperations.getLSBit(diagonalPos^pinnedPiece) & diagonalSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) & allBlackOccupied) != 0 &&
					(BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg) & allBlackOccupied) != 0 &&
					(BitOperations.getMSBit(rankNeg^pinnedPiece) & straightSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg) & allBlackOccupied) != 0 &&
					(BitOperations.getMSBit(fileNeg^pinnedPiece) & straightSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg) & allBlackOccupied) != 0 &&
					(BitOperations.getMSBit(diagonalNeg^pinnedPiece) & diagonalSliders) != 0)
				pinnedPieces |= pinnedPiece;
			if ((pinnedPiece = BitOperations.getMSBit(antiDiagonalNeg) & allBlackOccupied) != 0 &&
					(BitOperations.getMSBit(antiDiagonalNeg^pinnedPiece) & diagonalSliders) != 0)
				pinnedPieces |= pinnedPiece;
		}
		return pinnedPieces;
	}
	/**
	 * Returns a long representing all the squares on which there are pieces pinning opponent pieces to their king.
	 * 
	 * @param white Whether the colour of the pinning pieces should be white.
	 * @return
	 */
	long getPinningPieces(boolean white) {
		long rankPos, rankNeg, filePos, fileNeg, diagonalPos, diagonalNeg, antiDiagonalPos, antiDiagonalNeg;
		long straightSliders, diagonalSliders, pinnedPiece, pinningPieces = 0;
		Rays attRayMask;
		pinningPieces = 0;
		if (white) {
			attRayMask = Rays.getByIndex(BitOperations.indexOfBit(blackKing));
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
			if ((pinnedPiece = BitOperations.getLSBit(rankPos) & allBlackOccupied) != 0)
				pinningPieces |= (BitOperations.getLSBit(rankPos^pinnedPiece) & straightSliders);
			if ((pinnedPiece = BitOperations.getLSBit(filePos) & allBlackOccupied) != 0)
				pinningPieces |= (BitOperations.getLSBit(filePos^pinnedPiece) & straightSliders);
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos) & allBlackOccupied) != 0)
				pinningPieces |= (BitOperations.getLSBit(diagonalPos^pinnedPiece) & diagonalSliders);
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) & allBlackOccupied) != 0)
				pinningPieces |= (BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders);
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg) & allBlackOccupied) != 0)
				pinningPieces |= (BitOperations.getMSBit(rankNeg^pinnedPiece) & straightSliders);
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg) & allBlackOccupied) != 0)
				pinningPieces |= (BitOperations.getMSBit(fileNeg^pinnedPiece) & straightSliders);
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg) & allBlackOccupied) != 0)
				pinningPieces |= (BitOperations.getMSBit(diagonalNeg^pinnedPiece) & diagonalSliders);
			if ((pinnedPiece = BitOperations.getMSBit(antiDiagonalNeg) & allBlackOccupied) != 0)
				pinningPieces |= (BitOperations.getMSBit(antiDiagonalNeg^pinnedPiece) & diagonalSliders);
		} else {
			attRayMask = Rays.getByIndex(BitOperations.indexOfBit(whiteKing));
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
			if ((pinnedPiece = BitOperations.getLSBit(rankPos) & allWhiteOccupied) != 0)
				pinningPieces |= (BitOperations.getLSBit(rankPos^pinnedPiece) & straightSliders);
			if ((pinnedPiece = BitOperations.getLSBit(filePos) & allWhiteOccupied) != 0)
				pinningPieces |= (BitOperations.getLSBit(filePos^pinnedPiece) & straightSliders);
			if ((pinnedPiece = BitOperations.getLSBit(diagonalPos) & allWhiteOccupied) != 0)
				pinningPieces |= (BitOperations.getLSBit(diagonalPos^pinnedPiece) & diagonalSliders);
			if ((pinnedPiece = BitOperations.getLSBit(antiDiagonalPos) & allWhiteOccupied) != 0)
				pinningPieces |= (BitOperations.getLSBit(antiDiagonalPos^pinnedPiece) & diagonalSliders);
			if ((pinnedPiece = BitOperations.getMSBit(rankNeg) & allWhiteOccupied) != 0)
				pinningPieces |= (BitOperations.getMSBit(rankNeg^pinnedPiece) & straightSliders);
			if ((pinnedPiece = BitOperations.getMSBit(fileNeg) & allWhiteOccupied) != 0)
				pinningPieces |= (BitOperations.getMSBit(fileNeg^pinnedPiece) & straightSliders);
			if ((pinnedPiece = BitOperations.getMSBit(diagonalNeg) & allWhiteOccupied) != 0)
				pinningPieces |= (BitOperations.getMSBit(diagonalNeg^pinnedPiece) & diagonalSliders);
			if ((pinnedPiece = BitOperations.getMSBit(antiDiagonalNeg) & allWhiteOccupied) != 0)
				pinningPieces |= (BitOperations.getMSBit(antiDiagonalNeg^pinnedPiece) & diagonalSliders);
		}
		return pinningPieces;
	}
	/**
	 * Returns whether a move is legal or not in the current position. It's only guaranteed to be correct if a position exists in which the move is legal.
	 * 
	 * @param move
	 * @return
	 */
	boolean isLegalSoft(Move move) {
		MoveSetDatabase dB;
		boolean checked;
		long moveSet = 0;
		long toBit = (1L << move.to);
		if (offsetBoard[move.from] == move.movedPiece) {
			dB = MoveSetDatabase.getByIndex(move.from);
			PseudoSwitch: {
				if (isWhitesTurn) {
					if (move.movedPiece == Piece.W_KING.ind) {
						if (move.type == MoveType.SHORT_CASTLING.ind)
							return !isInCheck && (whiteCastlingRights == CastlingRights.SHORT.ind || whiteCastlingRights == CastlingRights.ALL.ind) &&
									((Square.F1.bit | Square.G1.bit) & allOccupied) == 0 && offsetBoard[Square.H1.ind] == Piece.W_ROOK.ind &&
									!isAttacked(Square.F1.ind, false) && !isAttacked(Square.G1.ind, false);
						else if (move.type == MoveType.LONG_CASTLING.ind)
							return !isInCheck && (whiteCastlingRights == CastlingRights.LONG.ind || whiteCastlingRights == CastlingRights.ALL.ind) &&
									((Square.B1.bit | Square.C1.bit | Square.D1.bit) & allOccupied) == 0 && offsetBoard[Square.A1.ind] == Piece.W_ROOK.ind &&
									!isAttacked(Square.C1.ind, false) && !isAttacked(Square.D1.ind, false);
						else moveSet = dB.getKingMoveSet(allNonWhiteOccupied);
					} else if (move.movedPiece == Piece.W_QUEEN.ind)
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
					} else return false;
				}
				else {
					if (move.movedPiece == Piece.B_KING.ind) {
						if (move.type == MoveType.SHORT_CASTLING.ind)
							return !isInCheck && (blackCastlingRights == CastlingRights.SHORT.ind || blackCastlingRights == CastlingRights.ALL.ind) &&
									((Square.F8.bit | Square.G8.bit) & allOccupied) == 0 && offsetBoard[Square.H8.ind] == Piece.B_ROOK.ind &&
									!isAttacked(Square.F8.ind, true) && !isAttacked(Square.G8.ind, true);
						else if (move.type == MoveType.LONG_CASTLING.ind)
							return !isInCheck && (blackCastlingRights == CastlingRights.LONG.ind || blackCastlingRights == CastlingRights.ALL.ind) &&
									((Square.B8.bit | Square.C8.bit | Square.D8.bit) & allOccupied) == 0 && offsetBoard[Square.A8.ind] == Piece.B_ROOK.ind &&
									!isAttacked(Square.C8.ind, true) && !isAttacked(Square.D8.ind, true);
						else moveSet = dB.getKingMoveSet(allNonBlackOccupied);
					} else if (move.movedPiece == Piece.B_QUEEN.ind)
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
					} else return false;
				}
				if (offsetBoard[move.to] != move.capturedPiece) return false;
			}
			if ((moveSet & toBit) != 0) {
				makeMoveOnBoard(move);
				checked = isAttacked(BitOperations.indexOfBit(isWhitesTurn ? whiteKing : blackKing), !isWhitesTurn);
				unmakeMoveOnBoard(move);
				return !checked;
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
	boolean isLegal(Move move) {
		for (Move m : getMoves()) {
			if (m.equals(move))
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
	boolean givesCheck(Move move) {
		MoveSetDatabase db;
		long toBit = 1L << move.to;
		if (isWhitesTurn) {
			db = MoveSetDatabase.getByIndex(BitOperations.indexOfBit(blackKing));
			if (move.movedPiece == Piece.W_QUEEN.ind)
				return (db.getQueenMoveSet(allNonWhiteOccupied, allOccupied) & toBit) != 0;
			else if (move.movedPiece == Piece.W_ROOK.ind)
				return (db.getRookMoveSet(allNonWhiteOccupied, allOccupied) & toBit) != 0;
			else if (move.movedPiece == Piece.W_BISHOP.ind)
				return (db.getBishopMoveSet(allNonWhiteOccupied, allOccupied) & toBit) != 0;
			else if (move.movedPiece == Piece.W_KNIGHT.ind)
				return (db.getKnightMoveSet(allNonWhiteOccupied) & toBit) != 0;
			else
				return (db.pawnBlackCaptureMoveMask & toBit) != 0;
		} else {
			db = MoveSetDatabase.getByIndex(BitOperations.indexOfBit(whiteKing));
			if (move.movedPiece == Piece.B_QUEEN.ind)
				return (db.getQueenMoveSet(allNonBlackOccupied, allOccupied) & toBit) != 0;
			else if (move.movedPiece == Piece.B_ROOK.ind)
				return (db.getRookMoveSet(allNonBlackOccupied, allOccupied) & toBit) != 0;
			else if (move.movedPiece == Piece.B_BISHOP.ind)
				return (db.getBishopMoveSet(allNonBlackOccupied, allOccupied) & toBit) != 0;
			else if (move.movedPiece == Piece.B_KNIGHT.ind)
				return (db.getKnightMoveSet(allNonBlackOccupied) & toBit) != 0;
			else
				return (db.pawnWhiteCaptureMoveMask & toBit) != 0;
		}
	}
	/**
	 * Generates and adds only the pinned-piece-moves that change the material balance on the board (captures/promotions) to the input
	 * parameter 'moves' and returns the set of pinned pieces as a bitboard.
	 * 
	 * @param moves The move list to which the pinned piece moves will be added.
	 * @return A bitboard representing the pinned pieces.
	 */
	private long addTacticalPinnedPieceMoves(List<Move> moves) {
		long straightSliders, diagonalSliders, pinnedPieceBit, pinnerBit, pinnedPieces = 0, enPassantDestination = 0;
		byte pinnedPieceInd, pinnedPiece, to;
		Rays attRayMask;
		if (isWhitesTurn) {
			straightSliders = blackQueens | blackRooks;
			diagonalSliders = blackQueens | blackBishops;
			attRayMask = Rays.getByIndex(BitOperations.indexOfBit(whiteKing));
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
					// Check also for possible pinned pawn en passant and promotion.
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = (byte) (pinnedPieceInd + 7))) &
									(pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								} else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						} else {
							if (1L << (to = (byte) (pinnedPieceInd + 7)) == pinnerBit) {
								if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								} else
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
					// Check also for possible pinned pawn en passant and promotion.
					else if (pinnedPiece == Piece.W_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = (byte) (pinnedPieceInd + 9))) &
									(pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								} else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						}
						else {
							if (1L << (to = (byte) (pinnedPieceInd + 9)) == pinnerBit) {
								if (to >= Square.A8.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								} else
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
			attRayMask = Rays.getByIndex(BitOperations.indexOfBit(blackKing));
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
					// Check also for possible pinned pawn en passant and promotion.
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						if (this.enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = (byte) (pinnedPieceInd - 7))) &
									(pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								} else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						} else {
							if (1L << (to = (byte)(pinnedPieceInd - 7)) == pinnerBit) {
								if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								} else
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
					// Check also for possible pinned pawn en passant and promotion.
					else if (pinnedPiece == Piece.B_PAWN.ind) {
						if (enPassantRights != EnPassantRights.NONE.ind) {
							if (((1L << (to = (byte) (pinnedPieceInd - 9))) &
									(pinnerBit | (1L << (enPassantDestination = EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)))) != 0) {
								if (to == enPassantDestination)
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
								else if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								} else
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.NORMAL.ind));
							}
						} else {
							if (1L << (to = (byte) (pinnedPieceInd - 9)) == pinnerBit) {
								if (to < Square.A2.ind) {
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_QUEEN.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_ROOK.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_BISHOP.ind));
									moves.add(new Move(pinnedPieceInd, to, pinnedPiece, offsetBoard[to], MoveType.PROMOTION_TO_KNIGHT.ind));
								} else
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
	private long addQuietPinnedPieceMoves(List<Move> moves) {
		long straightSliders, diagonalSliders, pinnedPieceBit, pinnerBit, pinnedPieces = 0;
		byte pinnedPieceInd, pinnedPiece;
		long pinnedPieceMoveSet;
		Rays attRayMask;
		if (this.isWhitesTurn) {
			straightSliders = blackQueens | blackRooks;
			diagonalSliders = blackQueens | blackBishops;
			attRayMask = Rays.getByIndex(BitOperations.indexOfBit(whiteKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allWhiteOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.W_QUEEN.ind || pinnedPiece == Piece.W_ROOK.ind) {
						pinnedPieceMoveSet = (pinnerBit - (whiteKing << 1))^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
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
						pinnedPieceMoveSet = ((pinnerBit - (whiteKing << 1)) & attRayMask.filePos)^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
						}
					} else if (pinnedPiece == Piece.W_PAWN.ind) {
						pinnedPieceMoveSet = MoveSetDatabase.getByIndex(pinnedPieceInd).getWhitePawnAdvanceSet(allEmpty);
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
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
						pinnedPieceMoveSet = ((pinnerBit - (whiteKing << 1)) & attRayMask.diagonalPos)^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
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
						pinnedPieceMoveSet = ((pinnerBit - (whiteKing << 1)) & attRayMask.antiDiagonalPos)^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
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
						pinnedPieceMoveSet = (whiteKing - (pinnerBit << 1))^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
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
						pinnedPieceMoveSet = ((whiteKing - (pinnerBit << 1)) & attRayMask.fileNeg)^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
						}
					} else if (pinnedPiece == Piece.W_PAWN.ind) {
						pinnedPieceMoveSet = MoveSetDatabase.getByIndex(pinnedPieceInd).getWhitePawnAdvanceSet(allEmpty);
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
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
						pinnedPieceMoveSet = ((whiteKing - (pinnerBit << 1)) & attRayMask.diagonalNeg)^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
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
						pinnedPieceMoveSet = ((whiteKing - (pinnerBit << 1)) & attRayMask.antiDiagonalNeg)^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
						}
					}
				}
			}
		}
		else {
			straightSliders = whiteQueens | whiteRooks;
			diagonalSliders = whiteQueens | whiteBishops;
			attRayMask = Rays.getByIndex(BitOperations.indexOfBit(blackKing));
			if ((pinnedPieceBit = BitOperations.getLSBit(attRayMask.rankPos & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getLSBit((attRayMask.rankPos & allOccupied)^pinnedPieceBit) & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoveSet = (pinnerBit - (blackKing << 1))^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
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
						pinnedPieceMoveSet = ((pinnerBit - (blackKing << 1)) & attRayMask.filePos)^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
						}
					} else if (pinnedPiece == Piece.B_PAWN.ind) {
						pinnedPieceMoveSet = MoveSetDatabase.getByIndex(pinnedPieceInd).getBlackPawnAdvanceSet(allEmpty);
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
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
						pinnedPieceMoveSet = ((pinnerBit - (blackKing << 1)) & attRayMask.diagonalPos)^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
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
						pinnedPieceMoveSet = ((pinnerBit - (blackKing << 1)) & attRayMask.antiDiagonalPos)^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
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
						pinnedPieceMoveSet = (blackKing - (pinnerBit << 1))^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
						}
					}
				}
			}
			if ((pinnedPieceBit = BitOperations.getMSBit(attRayMask.fileNeg & allOccupied) & allBlackOccupied) != 0) {
				if ((pinnerBit = BitOperations.getMSBit((attRayMask.fileNeg & allOccupied)^pinnedPieceBit)  & straightSliders) != 0) {
					pinnedPieces |= pinnedPieceBit;
					pinnedPieceInd = BitOperations.indexOfBit(pinnedPieceBit);
					pinnedPiece = offsetBoard[pinnedPieceInd];
					if (pinnedPiece == Piece.B_QUEEN.ind || pinnedPiece == Piece.B_ROOK.ind) {
						pinnedPieceMoveSet = ((blackKing - (pinnerBit << 1)) & attRayMask.fileNeg)^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
						}
					} else if (pinnedPiece == Piece.B_PAWN.ind) {
						pinnedPieceMoveSet = MoveSetDatabase.getByIndex(pinnedPieceInd).getBlackPawnAdvanceSet(allEmpty);
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
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
						pinnedPieceMoveSet = ((blackKing - (pinnerBit << 1)) & attRayMask.diagonalNeg)^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
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
						pinnedPieceMoveSet = ((blackKing - (pinnerBit << 1)) & attRayMask.antiDiagonalNeg)^pinnedPieceBit;
						while (pinnedPieceMoveSet != 0) {
							moves.add(new Move(pinnedPieceInd, BitOperations.indexOfLSBit(pinnedPieceMoveSet), pinnedPiece, Piece.NULL.ind,
									MoveType.NORMAL.ind));
							pinnedPieceMoveSet = BitOperations.resetLSBit(pinnedPieceMoveSet);
						}
					}
				}
			}
		}
		return pinnedPieces;
	}
	/**
	 * A method that returns a list of the legal moves that change the material balance on the board (captures/promotions) from a
	 * non-check position.
	 * 
	 * @return A list of material-tactical legal moves.
	 */
	private List<Move> generateTacticalMoves() {
		long movablePieces, pieceSet, moveSet, enPassAttBits, enPassBits, pawnPieceSet;
		byte king, piece, to, victim;
		MoveSetDatabase kingDb;
		ArrayList<Move> moves = new ArrayList<Move>();
		if (isWhitesTurn) {
			movablePieces = ~addTacticalPinnedPieceMoves(moves);
			king = BitOperations.indexOfBit(whiteKing);
			moveSet = MoveSetDatabase.getByIndex(king).getKingMoveSet(allBlackOccupied);
			while (moveSet != 0) {
				to = BitOperations.indexOfLSBit(moveSet);
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				moveSet = BitOperations.resetLSBit(moveSet);
			}
			pieceSet = whiteQueens & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet = MoveSetDatabase.getByIndex(piece).getQueenMoveSet(allBlackOccupied, allOccupied);
				while (moveSet != 0) {
					to = BitOperations.indexOfLSBit(moveSet);
					moves.add(new Move(piece, to, Piece.W_QUEEN.ind, offsetBoard[to], MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = whiteRooks & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getRookMoveSet(allBlackOccupied, allOccupied);
				while (moveSet != 0) {
					to = BitOperations.indexOfLSBit(moveSet);
					moves.add(new Move(piece, to, Piece.W_ROOK.ind, offsetBoard[to], MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = whiteBishops & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getBishopMoveSet(allBlackOccupied, allOccupied);
				while (moveSet != 0) {
					to = BitOperations.indexOfLSBit(moveSet);
					moves.add(new Move(piece, to, Piece.W_BISHOP.ind, offsetBoard[to], MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = whiteKnights & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getKnightMoveSet(allBlackOccupied);
				while (moveSet != 0) {
					to = BitOperations.indexOfLSBit(moveSet);
					moves.add(new Move(piece, to, Piece.W_KNIGHT.ind, offsetBoard[to], MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = pawnPieceSet = whitePawns & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet = MoveSetDatabase.getByIndex(piece).getWhitePawnMoveSet(allBlackOccupied, allEmpty);
				while (moveSet != 0) {
					to = BitOperations.indexOfLSBit(moveSet);
					victim = offsetBoard[to];
					// Check for promotion.
					if (to >= Square.A8.ind) {
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.PROMOTION_TO_KNIGHT.ind));
					} else if (victim != 0)
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, victim, MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			// Check for en passant moves.
			if (enPassantRights != EnPassantRights.NONE.ind) {
				if ((pieceSet = MoveSetDatabase.getByIndex(to = (byte) (EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights))
						.getBlackPawnCaptureSet(pawnPieceSet)) != 0) {
					kingDb = MoveSetDatabase.getByIndex(king);
					while (pieceSet != 0) {
						piece = BitOperations.indexOfLSBit(pieceSet);
						// Make sure that the en passant does not leave the king exposed to check.
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to - 8));
						allNonWhiteOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						if (((blackQueens | blackRooks) & kingDb.getRookMoveSet(allNonWhiteOccupied, allOccupied)) == 0 &&
								((blackQueens | blackBishops) & kingDb.getBishopMoveSet(allNonWhiteOccupied, allOccupied)) == 0)
							moves.add(new Move(piece, to, Piece.W_PAWN.ind, Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
						allNonWhiteOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						pieceSet = BitOperations.resetLSBit(pieceSet);
					}
				}
			}
		}
		else {
			movablePieces = ~addTacticalPinnedPieceMoves(moves);
			king = BitOperations.indexOfBit(blackKing);
			moveSet	= MoveSetDatabase.getByIndex(king).getKingMoveSet(allWhiteOccupied);
			while (moveSet != 0) {
				to = BitOperations.indexOfLSBit(moveSet);
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
				moveSet = BitOperations.resetLSBit(moveSet);
			}
			pieceSet = blackQueens & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getQueenMoveSet(allWhiteOccupied, allOccupied);
				while (moveSet != 0) {
					to = BitOperations.indexOfLSBit(moveSet);
					moves.add(new Move(piece, to, Piece.B_QUEEN.ind, offsetBoard[to], MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = blackRooks & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getRookMoveSet(allWhiteOccupied, allOccupied);
				while (moveSet != 0) {
					to = BitOperations.indexOfLSBit(moveSet);
					moves.add(new Move(piece, to, Piece.B_ROOK.ind, offsetBoard[to], MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = blackBishops & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getBishopMoveSet(allWhiteOccupied, allOccupied);
				while (moveSet != 0) {
					to = BitOperations.indexOfLSBit(moveSet);
					moves.add(new Move(piece, to, Piece.B_BISHOP.ind, offsetBoard[to], MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = blackKnights & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getKnightMoveSet(allWhiteOccupied);
				while (moveSet != 0) {
					to = BitOperations.indexOfLSBit(moveSet);
					moves.add(new Move(piece, to, Piece.B_KNIGHT.ind, offsetBoard[to], MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = pawnPieceSet = blackPawns & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet = MoveSetDatabase.getByIndex(piece).getBlackPawnMoveSet(allWhiteOccupied, allEmpty);
				while (moveSet != 0) {
					to = BitOperations.indexOfLSBit(moveSet);;
					victim = offsetBoard[to];
					// Check for promotion.
					if (to < Square.A2.ind) {
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_QUEEN.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_ROOK.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_BISHOP.ind));
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.PROMOTION_TO_KNIGHT.ind));
					} else if (victim != 0)
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, victim, MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			// Check for en passant moves.
			if (enPassantRights != EnPassantRights.NONE.ind) {
				if ((pieceSet = MoveSetDatabase.getByIndex(to = (byte) (EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights))
						.getWhitePawnCaptureSet(pawnPieceSet)) != 0) {
					kingDb = MoveSetDatabase.getByIndex(king);
					while (pieceSet != 0) {
						piece = BitOperations.indexOfLSBit(pieceSet);
						// Make sure that the en passant does not leave the king exposed to check.
						enPassAttBits = ((1L << piece) | (1L << to));
						enPassBits = enPassAttBits | (1L << (to + 8));
						allNonBlackOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						if (((whiteQueens | whiteRooks) & kingDb.getRookMoveSet(allNonBlackOccupied, allOccupied)) == 0 &&
								((whiteQueens | whiteBishops) & kingDb.getBishopMoveSet(allNonBlackOccupied, allOccupied)) == 0)
							moves.add(new Move(piece, to, Piece.B_PAWN.ind, Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
						allNonBlackOccupied ^= enPassAttBits;
						allOccupied ^= enPassBits;
						pieceSet = BitOperations.resetLSBit(pieceSet);
					}
				}
			}
		}
		return moves;
	}
	/**
	 * A method that returns a list of the legal moves that do not affect the material balance of the position (no captures or
	 * promotions) from a non-check position.
	 * 
	 * @return A list of non-material legal moves.
	 */
	private List<Move> generateQuietMoves() {
		long movablePieces, pieceSet, moveSet;
		byte king, piece, to;
		ArrayList<Move> moves = new ArrayList<Move>();
		if (isWhitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			moveSet = MoveSetDatabase.getByIndex(king).getKingMoveSet(allEmpty);
			while (moveSet != 0) {
				to = BitOperations.indexOfLSBit(moveSet);
				if (!isAttacked(to, false))
					moves.add(new Move(king, to, Piece.W_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				moveSet = BitOperations.resetLSBit(moveSet);
			}
			// Check for the legality of castling.
			if ((whiteCastlingRights == CastlingRights.LONG.ind || whiteCastlingRights == CastlingRights.ALL.ind) &&
					((Square.B1.bit | Square.C1.bit | Square.D1.bit) & allOccupied) == 0 && !isAttacked(Square.D1.ind, false) &&
					!isAttacked(Square.C1.ind, false))
				moves.add(new Move(king, Square.C1.ind, Piece.W_KING.ind, Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
			if ((whiteCastlingRights == CastlingRights.SHORT.ind || whiteCastlingRights == CastlingRights.ALL.ind) &&
					((Square.F1.bit | Square.G1.bit) & allOccupied) == 0 && !isAttacked(Square.F1.ind, false) && !isAttacked(Square.G1.ind, false))
				moves.add(new Move(king, Square.G1.ind, Piece.W_KING.ind, Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
			movablePieces = ~addQuietPinnedPieceMoves(moves);
			pieceSet = whiteQueens & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet = MoveSetDatabase.getByIndex(piece).getQueenMoveSet(allEmpty, allOccupied);
				while (moveSet != 0) {
					moves.add(new Move(piece, BitOperations.indexOfLSBit(moveSet), Piece.W_QUEEN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = whiteRooks & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getRookMoveSet(allEmpty, allOccupied);
				while (moveSet != 0) {
					moves.add(new Move(piece, BitOperations.indexOfLSBit(moveSet), Piece.W_ROOK.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = whiteBishops & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getBishopMoveSet(allEmpty, allOccupied);
				while (moveSet != 0) {
					moves.add(new Move(piece, BitOperations.indexOfLSBit(moveSet), Piece.W_BISHOP.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = whiteKnights & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getKnightMoveSet(allEmpty);
				while (moveSet != 0) {
					moves.add(new Move(piece, BitOperations.indexOfLSBit(moveSet), Piece.W_KNIGHT.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = whitePawns & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet = MoveSetDatabase.getByIndex(piece).getWhitePawnAdvanceSet(allEmpty);
				while (moveSet != 0) {
					to = BitOperations.indexOfLSBit(moveSet);
					if (to <= 55)
						moves.add(new Move(piece, to, Piece.W_PAWN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
		}
		else {
			king  = BitOperations.indexOfBit(blackKing);
			moveSet	= MoveSetDatabase.getByIndex(king).getKingMoveSet(allEmpty);
			while (moveSet != 0) {
				to = BitOperations.indexOfLSBit(moveSet);
				if (!isAttacked(to, true))
					moves.add(new Move(king, to, Piece.B_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
				moveSet = BitOperations.resetLSBit(moveSet);
			}
			// Check for the legality of castling.
			if ((blackCastlingRights == CastlingRights.SHORT.ind || blackCastlingRights == CastlingRights.ALL.ind) &&
					((Square.F8.bit | Square.G8.bit) & allOccupied) == 0 && !isAttacked(Square.F8.ind, true) && !isAttacked(Square.G8.ind, true))
				moves.add(new Move(king, Square.G8.ind, Piece.B_KING.ind, Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
			if ((blackCastlingRights == CastlingRights.LONG.ind || blackCastlingRights == CastlingRights.ALL.ind) &&
					((Square.B8.bit | Square.C8.bit | Square.D8.bit) & allOccupied) == 0 && !isAttacked(Square.C8.ind, true) &&
					!isAttacked(Square.D8.ind, true))
				moves.add(new Move(king, Square.C8.ind, Piece.B_KING.ind, Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
			movablePieces = ~addQuietPinnedPieceMoves(moves);
			pieceSet = blackQueens & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getQueenMoveSet(allEmpty, allOccupied);
				while (moveSet != 0) {
					moves.add(new Move(piece, BitOperations.indexOfLSBit(moveSet), Piece.B_QUEEN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = blackRooks & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getRookMoveSet(allEmpty, allOccupied);
				while (moveSet != 0) {
					moves.add(new Move(piece, BitOperations.indexOfLSBit(moveSet), Piece.B_ROOK.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = blackBishops & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getBishopMoveSet(allEmpty, allOccupied);
				while (moveSet != 0) {
					moves.add(new Move(piece, BitOperations.indexOfLSBit(moveSet), Piece.B_BISHOP.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = blackKnights & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet	= MoveSetDatabase.getByIndex(piece).getKnightMoveSet(allEmpty);
				while (moveSet != 0) {
					moves.add(new Move(piece, BitOperations.indexOfLSBit(moveSet), Piece.B_KNIGHT.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
			pieceSet = blackPawns & movablePieces;
			while (pieceSet != 0) {
				piece = BitOperations.indexOfLSBit(pieceSet);
				moveSet = MoveSetDatabase.getByIndex(piece).getBlackPawnAdvanceSet(allEmpty);
				while (moveSet != 0) {
					to = BitOperations.indexOfLSBit(moveSet);
					if (to >= 8)
						moves.add(new Move(piece, to, Piece.B_PAWN.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					moveSet = BitOperations.resetLSBit(moveSet);
				}
				pieceSet = BitOperations.resetLSBit(pieceSet);
			}
		}
		return moves;
	}
	/**
	 * This method returns a list of the legal moves that change the material balance on the board (captures/promotions) from a
	 * position in which the side to move is in check.
	 * 
	 * @return A list of legal material moves from a check position.
	 */
	private List<Move> generateTacticalCheckEvasionMoves() {
		long kingMoveSet, movablePieces, squaresOfInterventionSet, checkerAttackerSet, checkerBlockerSet;
		byte checker1, checker2, checkerPiece1, checkerPiece2, squareOfIntervention, checkerAttackerSquare,
			checkerBlockerSquare, king, to, movedPiece;
		ArrayList<Move> moves = new ArrayList<Move>();
		MoveSetDatabase dB, kingDb;
		boolean promotionOnAttackPossible = false, promotionOnBlockPossible = false;
		if (isWhitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			movablePieces = ~getPinnedPieces(isWhitesTurn);
			kingDb = MoveSetDatabase.getByIndex(king);
			kingMoveSet = kingDb.getKingMoveSet(allBlackOccupied);
			// Single check.
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = this.offsetBoard[checker1];
				dB = MoveSetDatabase.getByIndex(checker1);
				if ((checkers & Rank.R8.bits) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, true) & movablePieces & ~whiteKing;
				while (checkerAttackerSet != 0) {
					checkerAttackerSquare = BitOperations.indexOfLSBit(checkerAttackerSet);
					movedPiece = offsetBoard[checkerAttackerSquare];
					if (movedPiece == Piece.W_PAWN.ind) {
						if (promotionOnAttackPossible) {
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_QUEEN.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_ROOK.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_BISHOP.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_KNIGHT.ind));
						} else if (enPassantRights != EnPassantRights.NONE.ind && checker1 == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights &&
								((1L << checkerAttackerSquare) & ((checkers << 1) | (checkers >>> 1)) & Rank.R5.bits) != 0)
							moves.add(new Move(checkerAttackerSquare, (byte) (checker1 + 8), movedPiece,
									checkerPiece1, MoveType.EN_PASSANT.ind));
						else
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
					}
					else
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
					checkerAttackerSet = BitOperations.resetLSBit(checkerAttackerSet);
				}
				// Check for possible promotion on blocking.
				if (checkerPiece1 == Piece.B_QUEEN.ind) {
					if ((File.getBySquareIndex(king).bits & checkers) != 0 || (Rank.getBySquareIndex(king).bits & checkers) != 0) {
						squaresOfInterventionSet = (dB.getRookMoveSet(allNonBlackOccupied, allOccupied) &
								kingDb.getRookMoveSet(allNonWhiteOccupied, allOccupied));
						if (promotionOnAttackPossible && (whiteKing & Rank.R8.bits) != 0)
							promotionOnBlockPossible = true;
					} else
						squaresOfInterventionSet = (dB.getBishopMoveSet(allNonBlackOccupied, allOccupied) &
								kingDb.getBishopMoveSet(allNonWhiteOccupied, allOccupied));
					while (squaresOfInterventionSet != 0) {
						squareOfIntervention = BitOperations.indexOfLSBit(squaresOfInterventionSet);
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & whitePawns & movablePieces;
						while (checkerBlockerSet != 0) {
							checkerBlockerSquare = BitOperations.indexOfLSBit(checkerBlockerSet);
							movedPiece = Piece.W_PAWN.ind;
							if (promotionOnBlockPossible) {
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
							} else if (enPassantRights != EnPassantRights.NONE.ind &&
									squareOfIntervention ==enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
							checkerBlockerSet = BitOperations.resetLSBit(checkerBlockerSet);
						}
						squaresOfInterventionSet = BitOperations.resetLSBit(squaresOfInterventionSet);
					}
					kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
				}
				else if (checkerPiece1 == Piece.B_ROOK.ind) {
					if (promotionOnAttackPossible && (whiteKing & Rank.R8.bits) != 0)
						promotionOnBlockPossible = true;
					squaresOfInterventionSet = (dB.getRookMoveSet(allNonBlackOccupied, allOccupied) &
							kingDb.getRookMoveSet(allNonWhiteOccupied, allOccupied));
					while (squaresOfInterventionSet != 0) {
						squareOfIntervention = BitOperations.indexOfLSBit(squaresOfInterventionSet);
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & whitePawns & movablePieces;
						while (checkerBlockerSet != 0) {
							checkerBlockerSquare = BitOperations.indexOfLSBit(checkerBlockerSet);
							movedPiece = Piece.W_PAWN.ind;
							if (promotionOnBlockPossible) {
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
							} else if (enPassantRights != EnPassantRights.NONE.ind &&
									squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
							checkerBlockerSet = BitOperations.resetLSBit(checkerBlockerSet);
						}
						squaresOfInterventionSet = BitOperations.resetLSBit(squaresOfInterventionSet);
					}
					kingMoveSet &= ~dB.getRookMoveMask();
				}
				else if (checkerPiece1 == Piece.B_BISHOP.ind) {
					squaresOfInterventionSet = (dB.getBishopMoveSet(allNonBlackOccupied, allOccupied) &
							kingDb.getBishopMoveSet(allNonWhiteOccupied, allOccupied));
					while (squaresOfInterventionSet != 0) {
						squareOfIntervention = BitOperations.indexOfLSBit(squaresOfInterventionSet);
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & whitePawns & movablePieces;
						while (checkerBlockerSet != 0) {
							checkerBlockerSquare = BitOperations.indexOfLSBit(checkerBlockerSet);
							movedPiece = Piece.W_PAWN.ind;
							if (enPassantRights != EnPassantRights.NONE.ind &&
									squareOfIntervention == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND)
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
							checkerBlockerSet = BitOperations.resetLSBit(checkerBlockerSet);
						}
						squaresOfInterventionSet = BitOperations.resetLSBit(squaresOfInterventionSet);
					}
					kingMoveSet &= ~dB.getBishopMoveMask();
				}
				while (kingMoveSet != 0) {
					to = BitOperations.indexOfLSBit(kingMoveSet);
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
					kingMoveSet = BitOperations.resetLSBit(kingMoveSet);
				}
			}
			// Double check, only the king can move.
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = offsetBoard[checker2];
				dB = MoveSetDatabase.getByIndex(checker1);
				if (checkerPiece1 == Piece.B_QUEEN.ind) kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
				else if (checkerPiece1 == Piece.B_ROOK.ind) kingMoveSet &= ~dB.getRookMoveMask();
				else if (checkerPiece1 == Piece.B_BISHOP.ind) kingMoveSet &= ~dB.getBishopMoveMask();
				else if (checkerPiece1 == Piece.B_KNIGHT.ind) kingMoveSet &= ~dB.knightMoveMask;
				dB = MoveSetDatabase.getByIndex(checker2);
				if (checkerPiece2 == Piece.B_QUEEN.ind) kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
				else if (checkerPiece2 == Piece.B_ROOK.ind) kingMoveSet &= ~dB.getRookMoveMask();
				else if (checkerPiece2 == Piece.B_BISHOP.ind) kingMoveSet &= ~dB.getBishopMoveMask();
				else if (checkerPiece2 == Piece.B_KNIGHT.ind) kingMoveSet &= ~dB.knightMoveMask;
				while (kingMoveSet != 0) {
					to = BitOperations.indexOfLSBit(kingMoveSet);
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
					kingMoveSet = BitOperations.resetLSBit(kingMoveSet);
				}
			}
		}
		else {
			king = BitOperations.indexOfBit(blackKing);
			movablePieces = ~getPinnedPieces(isWhitesTurn);
			kingDb = MoveSetDatabase.getByIndex(king);
			kingMoveSet = kingDb.getKingMoveSet(allWhiteOccupied);
			// Single check.
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				dB = MoveSetDatabase.getByIndex(checker1);
				if ((checkers & Rank.R1.bits) != 0)
					promotionOnAttackPossible = true;
				checkerAttackerSet = getAttackers(checker1, false) & movablePieces & ~blackKing;
				while (checkerAttackerSet != 0) {
					checkerAttackerSquare = BitOperations.indexOfLSBit(checkerAttackerSet);
					movedPiece = offsetBoard[checkerAttackerSquare];
					if (movedPiece == Piece.B_PAWN.ind) {
						if (promotionOnAttackPossible) {
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_QUEEN.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_ROOK.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_BISHOP.ind));
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.PROMOTION_TO_KNIGHT.ind));
						} else if (enPassantRights != EnPassantRights.NONE.ind && checker1 == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights &&
								((1L << checkerAttackerSquare) & ((checkers << 1) | (checkers >>> 1)) & Rank.R4.bits) != 0)
							moves.add(new Move(checkerAttackerSquare, (byte) (checker1 - 8), movedPiece,
									checkerPiece1, MoveType.EN_PASSANT.ind));
						else
							moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
					} else
						moves.add(new Move(checkerAttackerSquare, checker1, movedPiece, checkerPiece1, MoveType.NORMAL.ind));
					checkerAttackerSet = BitOperations.resetLSBit(checkerAttackerSet);
				}
				// Check for possible promotion on blocking.
				if (checkerPiece1 == Piece.W_QUEEN.ind) {
					if ((File.getBySquareIndex(king).bits & checkers) != 0 || (Rank.getBySquareIndex(king).bits & checkers) != 0) {
						squaresOfInterventionSet = (dB.getRookMoveSet(allNonWhiteOccupied, allOccupied) &
								kingDb.getRookMoveSet(allNonBlackOccupied, allOccupied));
						if (promotionOnAttackPossible && (blackKing & Rank.R1.bits) != 0)
							promotionOnBlockPossible = true;
					} else
						squaresOfInterventionSet = (dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied) &
								kingDb.getBishopMoveSet(allNonBlackOccupied, allOccupied));
					while (squaresOfInterventionSet != 0) {
						squareOfIntervention = BitOperations.indexOfLSBit(squaresOfInterventionSet);
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & blackPawns & movablePieces;
						while (checkerBlockerSet != 0) {
							checkerBlockerSquare = BitOperations.indexOfLSBit(checkerBlockerSet);
							movedPiece = Piece.B_PAWN.ind;
							if (promotionOnBlockPossible) {
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
							} else if (enPassantRights != EnPassantRights.NONE.ind &&
									squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
							checkerBlockerSet = BitOperations.resetLSBit(checkerBlockerSet);
						}
						squaresOfInterventionSet = BitOperations.resetLSBit(squaresOfInterventionSet);
					}
					kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
				} else if (checkerPiece1 == Piece.W_ROOK.ind) {
					if (promotionOnAttackPossible && (blackKing & Rank.R1.bits) != 0)
						promotionOnBlockPossible = true;
					squaresOfInterventionSet = (dB.getRookMoveSet(allNonWhiteOccupied, allOccupied) &
							kingDb.getRookMoveSet(allNonBlackOccupied, allOccupied));
					while (squaresOfInterventionSet != 0) {
						squareOfIntervention = BitOperations.indexOfLSBit(squaresOfInterventionSet);
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & blackPawns & movablePieces;
						while (checkerBlockerSet != 0) {
							checkerBlockerSquare = BitOperations.indexOfLSBit(checkerBlockerSet);
							movedPiece = Piece.B_PAWN.ind;
							if (promotionOnBlockPossible) {
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_QUEEN.ind));
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_ROOK.ind));
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_BISHOP.ind));
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.PROMOTION_TO_KNIGHT.ind));
							} else if (enPassantRights != EnPassantRights.NONE.ind &&
									squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
							checkerBlockerSet = BitOperations.resetLSBit(checkerBlockerSet);
						}
						squaresOfInterventionSet = BitOperations.resetLSBit(squaresOfInterventionSet);
					}
					kingMoveSet &= ~dB.getRookMoveMask();
				} else if (checkerPiece1 == Piece.W_BISHOP.ind) {
					squaresOfInterventionSet = (dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied) &
							kingDb.getBishopMoveSet(allNonBlackOccupied, allOccupied));
					while (squaresOfInterventionSet != 0) {
						squareOfIntervention = BitOperations.indexOfLSBit(squaresOfInterventionSet);
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & blackPawns & movablePieces;
						while (checkerBlockerSet != 0) {
							checkerBlockerSquare = BitOperations.indexOfLSBit(checkerBlockerSet);
							movedPiece = Piece.B_PAWN.ind;
							if (enPassantRights != EnPassantRights.NONE.ind &&
									squareOfIntervention == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND)
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
							checkerBlockerSet = BitOperations.resetLSBit(checkerBlockerSet);
						}
						squaresOfInterventionSet = BitOperations.resetLSBit(squaresOfInterventionSet);
					}
					kingMoveSet &= ~dB.getBishopMoveMask();
				}
				while (kingMoveSet != 0) {
					to = BitOperations.indexOfLSBit(kingMoveSet);
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
					kingMoveSet = BitOperations.resetLSBit(kingMoveSet);
				}
			}
			// Double check, only the king can move.
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = this.offsetBoard[checker2];
				dB = MoveSetDatabase.getByIndex(checker1);
				if (checkerPiece1 == Piece.W_QUEEN.ind) kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
				else if (checkerPiece1 == Piece.W_ROOK.ind) kingMoveSet &= ~dB.getRookMoveMask();
				else if (checkerPiece1 == Piece.W_BISHOP.ind) kingMoveSet &= ~dB.getBishopMoveMask();
				else if (checkerPiece1 == Piece.W_KNIGHT.ind) kingMoveSet &= ~dB.knightMoveMask;
				dB = MoveSetDatabase.getByIndex(checker2);
				if (checkerPiece2 == Piece.W_QUEEN.ind) kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
				else if (checkerPiece2 == Piece.W_ROOK.ind) kingMoveSet &= ~dB.getRookMoveMask();
				else if (checkerPiece2 == Piece.W_BISHOP.ind) kingMoveSet &= ~dB.getBishopMoveMask();
				else if (checkerPiece2 == Piece.W_KNIGHT.ind) kingMoveSet &= ~dB.knightMoveMask;
				while (kingMoveSet != 0) {
					to = BitOperations.indexOfLSBit(kingMoveSet);
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, offsetBoard[to], MoveType.NORMAL.ind));
					kingMoveSet = BitOperations.resetLSBit(kingMoveSet);
				}
			}
		}
		return moves;
	}
	/**
	 * This method returns a list of the legal moves that do not affect the material balance (no captures or promotions) from a
	 * position in which the side to move is in check.
	 * 
	 * @return A list of non-material legal moves from a check position.
	 */
	private List<Move> generateQuietCheckEvasionMoves() {
		long kingMoveSet, movablePieces, squaresOfInterventionSet, checkerBlockerSet;
		byte checker1, checker2, checkerPiece1, checkerPiece2, squareOfIntervention,
			checkerBlockerSquare, king, to, movedPiece;
		ArrayList<Move> moves = new ArrayList<Move>();
		MoveSetDatabase dB, kingDb;
		boolean promotionOnAttackPossible = false, promotionOnBlockPossible = false;
		if (this.isWhitesTurn) {
			king = BitOperations.indexOfBit(whiteKing);
			movablePieces = ~getPinnedPieces(isWhitesTurn);
			kingDb = MoveSetDatabase.getByIndex(king);
			kingMoveSet = kingDb.getKingMoveSet(allEmpty);
			// Single check.
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = this.offsetBoard[checker1];
				dB = MoveSetDatabase.getByIndex(checker1);
				if ((checkers & Rank.R8.bits) != 0)
					promotionOnAttackPossible = true;
				if (checkerPiece1 == Piece.B_QUEEN.ind) {
					if ((File.getBySquareIndex(king).bits & checkers) != 0 || (Rank.getBySquareIndex(king).bits & checkers) != 0) {
						squaresOfInterventionSet = (dB.getRookMoveSet(allNonBlackOccupied, allOccupied) &
								kingDb.getRookMoveSet(allNonWhiteOccupied, allOccupied));
						if (promotionOnAttackPossible && (whiteKing & Rank.R8.bits) != 0)
							promotionOnBlockPossible = true;
					} else
						squaresOfInterventionSet = (dB.getBishopMoveSet(allNonBlackOccupied, allOccupied) &
								kingDb.getBishopMoveSet(allNonWhiteOccupied, allOccupied));
					while (squaresOfInterventionSet != 0) {
						squareOfIntervention = BitOperations.indexOfLSBit(squaresOfInterventionSet);
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
						while (checkerBlockerSet != 0) {
							checkerBlockerSquare = BitOperations.indexOfLSBit(checkerBlockerSet);
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (!promotionOnBlockPossible || movedPiece != Piece.W_PAWN.ind)
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.NORMAL.ind));
							checkerBlockerSet = BitOperations.resetLSBit(checkerBlockerSet);
						}
						squaresOfInterventionSet = BitOperations.resetLSBit(squaresOfInterventionSet);
					}
					kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
				} else if (checkerPiece1 == Piece.B_ROOK.ind) {
					if (promotionOnAttackPossible && (whiteKing & Rank.R8.bits) != 0)
						promotionOnBlockPossible = true;
					squaresOfInterventionSet = (dB.getRookMoveSet(allNonBlackOccupied, allOccupied) &
							kingDb.getRookMoveSet(allNonWhiteOccupied, allOccupied));
					while (squaresOfInterventionSet != 0) {
						squareOfIntervention = BitOperations.indexOfLSBit(squaresOfInterventionSet);
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
						while (checkerBlockerSet != 0) {
							checkerBlockerSquare = BitOperations.indexOfLSBit(checkerBlockerSet);
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (!promotionOnBlockPossible || movedPiece != Piece.W_PAWN.ind)
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.NORMAL.ind));
							checkerBlockerSet = BitOperations.resetLSBit(checkerBlockerSet);
						}
						squaresOfInterventionSet = BitOperations.resetLSBit(squaresOfInterventionSet);
					}
					kingMoveSet &= ~dB.getRookMoveMask();
				} else if (checkerPiece1 == Piece.B_BISHOP.ind) {
					squaresOfInterventionSet = (dB.getBishopMoveSet(allNonBlackOccupied, allOccupied) &
							kingDb.getBishopMoveSet(allNonWhiteOccupied, allOccupied));
					while (squaresOfInterventionSet != 0) {
						squareOfIntervention = BitOperations.indexOfLSBit(squaresOfInterventionSet);
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, true) & movablePieces;
						while (checkerBlockerSet != 0) {
							checkerBlockerSquare = BitOperations.indexOfLSBit(checkerBlockerSet);
							moves.add(new Move(checkerBlockerSquare, squareOfIntervention, offsetBoard[checkerBlockerSquare],
									Piece.NULL.ind, MoveType.NORMAL.ind));
							checkerBlockerSet = BitOperations.resetLSBit(checkerBlockerSet);
						}
						squaresOfInterventionSet = BitOperations.resetLSBit(squaresOfInterventionSet);
					}
					kingMoveSet &= ~dB.getBishopMoveMask();
				}
				while (kingMoveSet != 0) {
					to = BitOperations.indexOfLSBit(kingMoveSet);
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					kingMoveSet = BitOperations.resetLSBit(kingMoveSet);
				}
			}
			// Double check, only the king can move.
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = offsetBoard[checker2];
				dB = MoveSetDatabase.getByIndex(checker1);
				if (checkerPiece1 == Piece.B_QUEEN.ind) kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
				else if (checkerPiece1 == Piece.B_ROOK.ind) kingMoveSet &= ~dB.getRookMoveMask();
				else if (checkerPiece1 == Piece.B_BISHOP.ind) kingMoveSet &= ~dB.getBishopMoveMask();
				else if (checkerPiece1 == Piece.B_KNIGHT.ind) kingMoveSet &= ~dB.knightMoveMask;
				dB = MoveSetDatabase.getByIndex(checker2);
				if (checkerPiece2 == Piece.B_QUEEN.ind) kingMoveSet &= ~dB.getQueenMoveSet(allNonWhiteOccupied, (allOccupied^whiteKing));
				else if (checkerPiece2 == Piece.B_ROOK.ind) kingMoveSet &= ~dB.getRookMoveMask();
				else if (checkerPiece2 == Piece.B_BISHOP.ind) kingMoveSet &= ~dB.getBishopMoveMask();
				else if (checkerPiece2 == Piece.B_KNIGHT.ind) kingMoveSet &= ~dB.knightMoveMask;
				while (kingMoveSet != 0) {
					to = BitOperations.indexOfLSBit(kingMoveSet);
					if (!isAttacked(to, false))
						moves.add(new Move(king, to, Piece.W_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					kingMoveSet = BitOperations.resetLSBit(kingMoveSet);
				}
			}
		}
		else {
			king = BitOperations.indexOfBit(blackKing);
			movablePieces = ~getPinnedPieces(isWhitesTurn);
			kingDb = MoveSetDatabase.getByIndex(king);
			kingMoveSet = kingDb.getKingMoveSet(allEmpty);
			// Single check.
			if (BitOperations.resetLSBit(checkers) == 0) {
				checker1 = BitOperations.indexOfBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				dB = MoveSetDatabase.getByIndex(checker1);
				if ((checkers & Rank.R1.bits) != 0)
					promotionOnAttackPossible = true;
				if (checkerPiece1 == Piece.W_QUEEN.ind) {
					if ((File.getBySquareIndex(king).bits & checkers) != 0 || (Rank.getBySquareIndex(king).bits & checkers) != 0) {
						squaresOfInterventionSet = (dB.getRookMoveSet(allNonWhiteOccupied, allOccupied) &
								kingDb.getRookMoveSet(allNonBlackOccupied, allOccupied));
						if (promotionOnAttackPossible && (blackKing & Rank.R1.bits) != 0)
							promotionOnBlockPossible = true;
					} else
						squaresOfInterventionSet = (dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied) &
								kingDb.getBishopMoveSet(allNonBlackOccupied, allOccupied));
					while (squaresOfInterventionSet != 0) {
						squareOfIntervention = BitOperations.indexOfLSBit(squaresOfInterventionSet);
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
						while (checkerBlockerSet != 0) {
							checkerBlockerSquare = BitOperations.indexOfLSBit(checkerBlockerSet);
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (!promotionOnBlockPossible || movedPiece != Piece.B_PAWN.ind)
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.NORMAL.ind));
							checkerBlockerSet = BitOperations.resetLSBit(checkerBlockerSet);
						}
						squaresOfInterventionSet = BitOperations.resetLSBit(squaresOfInterventionSet);
					}
					kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
				} else if (checkerPiece1 == Piece.W_ROOK.ind) {
					if (promotionOnAttackPossible && (blackKing & Rank.R1.bits) != 0)
						promotionOnBlockPossible = true;
					squaresOfInterventionSet = (dB.getRookMoveSet(allNonWhiteOccupied, allOccupied) &
							kingDb.getRookMoveSet(allNonBlackOccupied, allOccupied));
					while (squaresOfInterventionSet != 0) {
						squareOfIntervention = BitOperations.indexOfLSBit(squaresOfInterventionSet);
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
						while (checkerBlockerSet != 0) {
							checkerBlockerSquare = BitOperations.indexOfLSBit(checkerBlockerSet);
							movedPiece = offsetBoard[checkerBlockerSquare];
							if (!promotionOnBlockPossible || movedPiece != Piece.B_PAWN.ind)
								moves.add(new Move(checkerBlockerSquare, squareOfIntervention, movedPiece,
										Piece.NULL.ind, MoveType.NORMAL.ind));
							checkerBlockerSet = BitOperations.resetLSBit(checkerBlockerSet);
						}
						squaresOfInterventionSet = BitOperations.resetLSBit(squaresOfInterventionSet);
					}
					kingMoveSet &= ~dB.getRookMoveMask();
				} else if (checkerPiece1 == Piece.W_BISHOP.ind) {
					squaresOfInterventionSet = (dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied) &
							kingDb.getBishopMoveSet(allNonBlackOccupied, allOccupied));
					while (squaresOfInterventionSet != 0) {
						squareOfIntervention = BitOperations.indexOfLSBit(squaresOfInterventionSet);
						checkerBlockerSet = getBlockerCandidates(squareOfIntervention, false) & movablePieces;
						while (checkerBlockerSet != 0) {
							checkerBlockerSquare = BitOperations.indexOfLSBit(checkerBlockerSet);
							moves.add(new Move(checkerBlockerSquare, squareOfIntervention, offsetBoard[checkerBlockerSquare],
									Piece.NULL.ind, MoveType.NORMAL.ind));
							checkerBlockerSet = BitOperations.resetLSBit(checkerBlockerSet);
						}
						squaresOfInterventionSet = BitOperations.resetLSBit(squaresOfInterventionSet);
					}
					kingMoveSet &= ~dB.getBishopMoveMask();
				}
				while (kingMoveSet != 0) {
					to = BitOperations.indexOfLSBit(kingMoveSet);
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					kingMoveSet = BitOperations.resetLSBit(kingMoveSet);
				}
			}
			// Double check, only the king can move.
			else {
				checker1 = BitOperations.indexOfLSBit(checkers);
				checkerPiece1 = offsetBoard[checker1];
				checker2 = BitOperations.indexOfBit(BitOperations.resetLSBit(checkers));
				checkerPiece2 = this.offsetBoard[checker2];
				dB = MoveSetDatabase.getByIndex(checker1);
				if (checkerPiece1 == Piece.W_QUEEN.ind) kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
				else if (checkerPiece1 == Piece.W_ROOK.ind) kingMoveSet &= ~dB.getRookMoveMask();
				else if (checkerPiece1 == Piece.W_BISHOP.ind) kingMoveSet &= ~dB.getBishopMoveMask();
				else if (checkerPiece1 == Piece.W_KNIGHT.ind) kingMoveSet &= ~dB.knightMoveMask;
				dB = MoveSetDatabase.getByIndex(checker2);
				if (checkerPiece2 == Piece.W_QUEEN.ind) kingMoveSet &= ~dB.getQueenMoveSet(allNonBlackOccupied, (allOccupied^blackKing));
				else if (checkerPiece2 == Piece.W_ROOK.ind) kingMoveSet &= ~dB.getRookMoveMask();
				else if (checkerPiece2 == Piece.W_BISHOP.ind) kingMoveSet &= ~dB.getBishopMoveMask();
				else if (checkerPiece2 == Piece.W_KNIGHT.ind) kingMoveSet &= ~dB.knightMoveMask;
				while (kingMoveSet != 0) {
					to = BitOperations.indexOfLSBit(kingMoveSet);
					if (!isAttacked(to, true))
						moves.add(new Move(king, to, Piece.B_KING.ind, Piece.NULL.ind, MoveType.NORMAL.ind));
					kingMoveSet = BitOperations.resetLSBit(kingMoveSet);
				}
			}
		}
		return moves;
	}
	/**
	 * Generates a list of Move objects that represents all the legal moves from the current position.
	 * 
	 * @return A list of all the legal moves from this position.
	 */
	public List<Move> getMoves() {
		List<Move> moves;
		if (isInCheck) {
			moves = generateTacticalCheckEvasionMoves();
			moves.addAll(generateQuietCheckEvasionMoves());
		} else {
			moves = generateTacticalMoves();
			moves.addAll(generateQuietMoves());
		}
		return moves;
	}
	/**
	 * Generates a list of Move objects that represents the material legal moves (i.e. the ones that change the material balance of the
	 * position such as captures and promotions) from the current position.
	 * 
	 * @return A list of the material legal moves from this position.
	 */
	List<Move> getTacticalMoves() {
		return isInCheck? generateTacticalCheckEvasionMoves() : generateTacticalMoves();
	}
	/**
	 * Generates a list of Move objects that represents the non-material legal moves (i.e. the ones that do not affect the material
	 * balance of the position such as non-promotion and non-capture moves) from the current position.
	 * 
	 * @return A list of the non-material legal moves from this position.
	 */
	List<Move> getQuietMoves() {
		return isInCheck? generateQuietCheckEvasionMoves() : generateQuietMoves();
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
			} else if (move.type == MoveType.SHORT_CASTLING.ind) {
				offsetBoard[Square.H1.ind] = Piece.NULL.ind;
				offsetBoard[Square.F1.ind] = Piece.W_ROOK.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				offsetBoard[move.to] = move.movedPiece;
				whiteKing = toBit;
				collChangedBits = (1L << Square.H1.ind) | (1L << Square.F1.ind);
				whiteRooks ^= collChangedBits;
				allWhiteOccupied ^= (changedBits | collChangedBits);
				allNonWhiteOccupied = ~allWhiteOccupied;
			} else if (move.type == MoveType.LONG_CASTLING.ind) {
				offsetBoard[Square.A1.ind] = Piece.NULL.ind;
				offsetBoard[Square.D1.ind] = Piece.W_ROOK.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				offsetBoard[move.to] = move.movedPiece;
				whiteKing = toBit;
				collChangedBits = (1L << Square.A1.ind) | (1L << Square.D1.ind);
				whiteRooks ^= collChangedBits;
				allWhiteOccupied ^= (changedBits | collChangedBits);
				allNonWhiteOccupied = ~allWhiteOccupied;
			} else if (move.type == MoveType.EN_PASSANT.ind) {
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
			} else if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
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
			} else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
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
			} else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
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
			} else {
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
		} else {
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
			} else if (move.type == MoveType.SHORT_CASTLING.ind) {
				offsetBoard[Square.H8.ind] = Piece.NULL.ind;
				offsetBoard[Square.F8.ind] = Piece.B_ROOK.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				offsetBoard[move.to] = move.movedPiece;
				blackKing = toBit;
				collChangedBits = (1L << Square.H8.ind) | (1L << Square.F8.ind);
				blackRooks ^= collChangedBits;
				allBlackOccupied ^= (changedBits | collChangedBits);
				allNonBlackOccupied = ~allBlackOccupied;
			} else if (move.type == MoveType.LONG_CASTLING.ind) {
				offsetBoard[Square.A8.ind] = Piece.NULL.ind;
				offsetBoard[Square.D8.ind] = Piece.B_ROOK.ind;
				offsetBoard[move.from] = Piece.NULL.ind;
				offsetBoard[move.to] = move.movedPiece;
				blackKing = toBit;
				collChangedBits = (1L << Square.A8.ind) | (1L << Square.D8.ind);
				blackRooks ^= collChangedBits;
				allBlackOccupied ^= (changedBits | collChangedBits);
				allNonBlackOccupied = ~allBlackOccupied;
			} else if (move.type == MoveType.EN_PASSANT.ind) {
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
			} else if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
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
			} else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
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
			} else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
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
			} else {
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
		long fromBit = Square.getByIndex(move.from).bit;
		long toBit = Square.getByIndex(move.to).bit;
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
			} else if (move.type == MoveType.SHORT_CASTLING.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = Piece.NULL.ind;
				offsetBoard[Square.H1.ind] = Piece.W_ROOK.ind;
				offsetBoard[Square.F1.ind] = Piece.NULL.ind;
				whiteKing = fromBit;
				collChangedBits = (1L << Square.H1.ind) | (1L << Square.F1.ind);
				whiteRooks ^= collChangedBits;
				allWhiteOccupied ^= (changedBits | collChangedBits);
				allNonWhiteOccupied = ~allWhiteOccupied;
			} else if (move.type == MoveType.LONG_CASTLING.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = Piece.NULL.ind;
				offsetBoard[Square.A1.ind] = Piece.W_ROOK.ind;
				offsetBoard[Square.D1.ind] = Piece.NULL.ind;
				whiteKing = fromBit;
				collChangedBits = (1L << Square.A1.ind) | (1L << Square.D1.ind);
				whiteRooks ^= collChangedBits;
				allWhiteOccupied ^= (changedBits | collChangedBits);
				allNonWhiteOccupied = ~allWhiteOccupied;
			} else if (move.type == MoveType.EN_PASSANT.ind) {
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
			} else if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
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
			} else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
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
			} else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
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
			} else {
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
		} else {
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
			} else if (move.type == MoveType.SHORT_CASTLING.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = Piece.NULL.ind;
				offsetBoard[Square.H8.ind] = Piece.B_ROOK.ind;
				offsetBoard[Square.F8.ind] = Piece.NULL.ind;
				blackKing = fromBit;
				collChangedBits = (1L << Square.H8.ind) | (1L << Square.F8.ind);
				blackRooks ^= collChangedBits;
				allBlackOccupied ^= (changedBits | collChangedBits);
				allNonBlackOccupied = ~allBlackOccupied;
			} else if (move.type == MoveType.LONG_CASTLING.ind) {
				offsetBoard[move.from] = move.movedPiece;
				offsetBoard[move.to] = Piece.NULL.ind;
				offsetBoard[Square.A8.ind] = Piece.B_ROOK.ind;
				offsetBoard[Square.D8.ind] = Piece.NULL.ind;
				blackKing = fromBit;
				collChangedBits = (1L << Square.A8.ind) | (1L << Square.D8.ind);
				blackRooks ^= collChangedBits;
				allBlackOccupied ^= (changedBits | collChangedBits);
				allNonBlackOccupied = ~allBlackOccupied;
			} else if (move.type == MoveType.EN_PASSANT.ind) {
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
			} else if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
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
			} else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
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
			} else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
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
			} else {
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
	 * Sets the castling rights for the side to move.
	 */
	private void setCastlingRights() {
		if (isWhitesTurn) {
			if (whiteCastlingRights == CastlingRights.NONE.ind);
			else if (whiteCastlingRights == CastlingRights.SHORT.ind) {
				if (offsetBoard[Square.E1.ind] != Piece.W_KING.ind || offsetBoard[Square.H1.ind] != Piece.W_ROOK.ind)
					whiteCastlingRights = CastlingRights.NONE.ind;
			} else if (whiteCastlingRights == CastlingRights.LONG.ind) {
				if (offsetBoard[Square.E1.ind] != Piece.W_KING.ind || offsetBoard[Square.A1.ind] != Piece.W_ROOK.ind)
					whiteCastlingRights = CastlingRights.NONE.ind;
			} else {
				if (offsetBoard[Square.E1.ind] == Piece.W_KING.ind) {
					if (offsetBoard[Square.H1.ind] != Piece.W_ROOK.ind) {
						if (offsetBoard[Square.A1.ind] != Piece.W_ROOK.ind)
							whiteCastlingRights = CastlingRights.NONE.ind;
						else
							whiteCastlingRights = CastlingRights.LONG.ind;
					} else if (offsetBoard[Square.A1.ind] != Piece.W_ROOK.ind)
						whiteCastlingRights = CastlingRights.SHORT.ind;
				} else
					whiteCastlingRights = CastlingRights.NONE.ind;
			}
		} else {
			if (blackCastlingRights == CastlingRights.NONE.ind);
			else if (blackCastlingRights == CastlingRights.SHORT.ind) {
				if (offsetBoard[Square.E8.ind] != Piece.B_KING.ind || offsetBoard[Square.H8.ind] != Piece.B_ROOK.ind)
					blackCastlingRights = CastlingRights.NONE.ind;
			} else if (blackCastlingRights == CastlingRights.LONG.ind) {
				if (offsetBoard[Square.E8.ind] != Piece.B_KING.ind || offsetBoard[Square.A8.ind] != Piece.B_ROOK.ind)
					blackCastlingRights = CastlingRights.NONE.ind;
			} else {
				if (offsetBoard[Square.E8.ind] == Piece.B_KING.ind) {
					if (offsetBoard[Square.H8.ind] != Piece.B_ROOK.ind) {
						if (offsetBoard[Square.A8.ind] != Piece.B_ROOK.ind)
							blackCastlingRights = CastlingRights.NONE.ind;
						else
							blackCastlingRights = CastlingRights.LONG.ind;
					} else if (offsetBoard[Square.A8.ind] != Piece.B_ROOK.ind)
						blackCastlingRights = CastlingRights.SHORT.ind;
				} else
					blackCastlingRights = CastlingRights.NONE.ind;
			}
		}
	}
	/**
	 * Makes a single move from a LongQueue generated by generateMoves.
	 * 
	 * @param move A Move object that is going to be made in the position.
	 */
	void makeMove(Move move) {
		makeMoveOnBoard(move);
		moveList.addFirst(move);
		unmakeRegisterHistory.addFirst(new UnmakeMoveRecord(whiteCastlingRights, blackCastlingRights, enPassantRights, 
				fiftyMoveRuleClock, checkers));
		isWhitesTurn = !isWhitesTurn;
		setCastlingRights();
		// Check en passant rights and the fifty move rule.
		if (isWhitesTurn) {
			enPassantRights = (move.movedPiece == Piece.B_PAWN.ind && move.from - move.to == 16) ? (byte) (move.to%8) : 8;
			fiftyMoveRuleClock = (byte) ((move.capturedPiece != Piece.NULL.ind || move.movedPiece == Piece.B_PAWN.ind) ?
					0 : fiftyMoveRuleClock + 1);
		} else {
			enPassantRights = (move.movedPiece == Piece.W_PAWN.ind && move.to - move.from == 16) ? (byte) (move.to%8) : 8;
			fiftyMoveRuleClock = (byte) ((move.capturedPiece != Piece.NULL.ind || move.movedPiece == Piece.W_PAWN.ind) ?
					0 : fiftyMoveRuleClock + 1);
		}
		checkers = getCheckers();
		isInCheck = checkers != 0;
		halfMoveIndex++;
		// Ensure the key history is big enough to hold the entries.
		if (keyHistory.length - halfMoveIndex <= 3)
			keyHistory = Arrays.copyOf(keyHistory, keyHistory.length + (keyHistory.length >> 1));
		key = gen.getUpdatedHashKey(this);
		keyHistory[halfMoveIndex] = key;
	}
	/**
	 * Makes a null move that can be taken back with {@link #unmakeMove() unmakeMove}. Consecutive null moves are not supported.
	 */
	void makeNullMove() {
		unmakeRegisterHistory.addFirst(new UnmakeMoveRecord(whiteCastlingRights, blackCastlingRights, enPassantRights,
				fiftyMoveRuleClock, checkers));
		isWhitesTurn = !isWhitesTurn;
		setCastlingRights();
		enPassantRights = EnPassantRights.NONE.ind;
		moveList.addFirst(Move.NULL_MOVE);
		halfMoveIndex++;
		key = gen.getUpdatedHashKey(this);
		keyHistory[halfMoveIndex] = key;
	}
	/**
	 * Reverts the changes made to the state of the Position instance by the last move made. It returns a reference to the move
	 * unmade.
	 *
	 * @return
	 */
	Move unmakeMove() {
		UnmakeMoveRecord positionInfo = unmakeRegisterHistory.poll();
		if (positionInfo == null) return null;
		Move move = moveList.pop();
		isWhitesTurn = !isWhitesTurn;
		if (!move.equals(Move.NULL_MOVE)) unmakeMoveOnBoard(move);
		whiteCastlingRights = positionInfo.whiteCastlingRights;
		blackCastlingRights = positionInfo.blackCastlingRights;
		enPassantRights = positionInfo.enPassantRights;
		fiftyMoveRuleClock = positionInfo.fiftyMoveRuleClock;
		checkers = positionInfo.checkers;
		isInCheck = checkers != 0;
		keyHistory[halfMoveIndex] = 0;
		key = keyHistory[--halfMoveIndex];
		return move;
	}
	/**
	 * Runs a staged perft test to the given depth and returns the number of leaf nodes the traversed game tree had. It is used mainly for move
	 * generation and move making speed benchmarking; and bug detection by comparing the returned values to validated results.
	 * 
	 * @param depth
	 * @return
	 */
	long perft(int depth) {
		List<Move> tacticalMoves, quietMoves;
		long leafNodes;
//		if (depth == 0)
//			return 1;
		leafNodes = 0;
		tacticalMoves = getTacticalMoves();
		quietMoves = getQuietMoves();
		if (depth == 1)
			return tacticalMoves.size() + quietMoves.size();
		for (Move m : tacticalMoves) {
			makeMove(m);
			leafNodes += perft(depth - 1);
			unmakeMove();
		}
		for (Move m : quietMoves) {
			makeMove(m);
			leafNodes += perft(depth - 1);
			unmakeMove();
		}
		return leafNodes;
	}
	/**
	 * Parses a String describing a move in Standard Algebraic Notation and returns a Move object created based on it. The move described by
	 * the SAN string is assumed to be legal in the position it is parsed for.
	 * 
	 * @param san
	 * @return
	 * @throws ChessParseException
	 * @throws NullPointerException
	 */
	Move parseSAN(String san) throws ChessParseException, NullPointerException {
		byte from, to, movedPiece, capturedPiece, type;
		long movablePieces, restriction, pawnAdvancer;
		char[] chars;
		MoveSetDatabase mT;
		if (san == null)
			return null;
		try {
			chars = san.toCharArray();
			movablePieces = ~getPinnedPieces(isWhitesTurn);
			if (san.matches("^O-O[+#]?[//?!]{0,2}$")) {
				if (isWhitesTurn) {
					to = Square.G1.ind;
					from = Square.E1.ind;
					movedPiece = Piece.W_KING.ind;
				} else {
					to = Square.G8.ind;
					from = Square.E8.ind;
					movedPiece = Piece.B_KING.ind;
				}
				capturedPiece = Piece.NULL.ind;
				type = MoveType.SHORT_CASTLING.ind;
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^O-O-O[+#]?[//?!]{0,2}$")) {
				if (isWhitesTurn) {
					to = Square.C1.ind;
					from = Square.E1.ind;
					movedPiece = Piece.W_KING.ind;
				} else {
					to = Square.C8.ind;
					from = Square.E8.ind;
					movedPiece = Piece.B_KING.ind;
				}
				capturedPiece = Piece.NULL.ind;
				type = MoveType.LONG_CASTLING.ind;
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[0] - 'a') + 8*(Integer.parseInt(Character.toString(chars[1])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					pawnAdvancer = (1L << (to - 8)) & whitePawns & movablePieces;
					from = (byte) ((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to - 16);
				} else {
					movedPiece = Piece.B_PAWN.ind;
					pawnAdvancer = (1L << (to + 8)) & blackPawns & movablePieces;
					from = (byte) ((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to + 16);
				}
				capturedPiece = Piece.NULL.ind;
				type = MoveType.NORMAL.ind;
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[a-h][1-8]=[QRBN][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[0] - 'a') + 8*(Integer.parseInt(Character.toString(chars[1])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					pawnAdvancer = (1L << (to - 8)) & whitePawns & movablePieces;
					from = (byte) ((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to - 16);
				} else {
					movedPiece = Piece.B_PAWN.ind;
					pawnAdvancer = (1L << (to + 8)) & blackPawns & movablePieces;
					from = (byte) ((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to + 16);
				}
				capturedPiece = Piece.NULL.ind;
				type = (byte) (Piece.parse(chars[3]).ind + 2);
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^x[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[1] - 'a') + 8*(Integer.parseInt(Character.toString(chars[2])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getBlackPawnCaptureSet(whitePawns & movablePieces));
					if (enPassantRights != EnPassantRights.NONE.ind && to == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND) {
						capturedPiece = Piece.B_PAWN.ind;
						type = MoveType.EN_PASSANT.ind;
					} else {
						capturedPiece = offsetBoard[to];
						type = MoveType.NORMAL.ind;
					}
				} else {
					movedPiece = Piece.B_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getWhitePawnCaptureSet(blackPawns & movablePieces));
					if (enPassantRights != EnPassantRights.NONE.ind && to == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND) {
						capturedPiece = Piece.W_PAWN.ind;
						type = MoveType.EN_PASSANT.ind;
					} else {
						capturedPiece = offsetBoard[to];
						type = MoveType.NORMAL.ind;
					}
				}
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^x[a-h][1-8]=[QRBN][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[1] - 'a') + 8*(Integer.parseInt(Character.toString(chars[2])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getBlackPawnCaptureSet(whitePawns & movablePieces));
				} else {
					movedPiece = Piece.B_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getWhitePawnCaptureSet(blackPawns & movablePieces));
				}
				capturedPiece = offsetBoard[to];
				type = (byte) (Piece.parse(chars[4]).ind + 2);
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^x[a-h][1-8]e.p.[+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[1] - 'a') + 8*(Integer.parseInt(Character.toString(chars[2])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getBlackPawnCaptureSet(whitePawns & movablePieces));
					capturedPiece = Piece.B_PAWN.ind;
				} else {
					movedPiece = Piece.B_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getWhitePawnCaptureSet(blackPawns & movablePieces));
					capturedPiece = Piece.W_PAWN.ind;
				}
				type = MoveType.EN_PASSANT.ind;
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[a-h]x[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getBlackPawnCaptureSet(whitePawns & movablePieces)
							& File.getByIndex((int) (chars[0] - 'a')).bits);
					if (enPassantRights != EnPassantRights.NONE.ind && to == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND) {
						capturedPiece = Piece.B_PAWN.ind;
						type = MoveType.EN_PASSANT.ind;
					} else {
						capturedPiece = offsetBoard[to];
						type = MoveType.NORMAL.ind;
					}
				} else {
					movedPiece = Piece.B_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getWhitePawnCaptureSet(blackPawns & movablePieces)
							& File.getByIndex((int) (chars[0] - 'a')).bits);
					if (enPassantRights != EnPassantRights.NONE.ind && to == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND) {
						capturedPiece = Piece.W_PAWN.ind;
						type = MoveType.EN_PASSANT.ind;
					} else {
						capturedPiece = offsetBoard[to];
						type = MoveType.NORMAL.ind;
					}
				}
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[a-h]x[a-h][1-8]=[QRBN][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getBlackPawnCaptureSet(whitePawns & movablePieces)
							& File.getByIndex((int) (chars[0] - 'a')).bits);
				} else {
					movedPiece = Piece.B_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getWhitePawnCaptureSet(blackPawns & movablePieces)
							& File.getByIndex((int) (chars[0] - 'a')).bits);
				}
				capturedPiece = offsetBoard[to];
				type = (byte) (Piece.parse(chars[5]).ind + 2);
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[a-h]x[a-h][1-8]e.p.[+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getBlackPawnCaptureSet(whitePawns & movablePieces)
							& File.getByIndex((int) (chars[0] - 'a')).bits);
					capturedPiece = Piece.B_PAWN.ind;
				} else {
					movedPiece = Piece.B_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getWhitePawnCaptureSet(blackPawns & movablePieces)
							& File.getByIndex((int) (chars[0] - 'a')).bits);
					capturedPiece = Piece.W_PAWN.ind;
				}
				type = MoveType.EN_PASSANT.ind;
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[KQRBN][a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[1] - 'a') + 8*(Integer.parseInt(Character.toString(chars[2])) - 1));
				movedPiece = (byte) (Piece.parse(chars[0]).ind + (isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = Piece.NULL.ind;
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[KQRBN][a-h][a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				movedPiece = (byte) (Piece.parse(chars[0]).ind + (isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = Piece.NULL.ind;
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = File.getByIndex((int) (chars[1] - 'a')).bits;
			} else if (san.matches("^[KQRBN][1-8][a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				movedPiece = (byte) (Piece.parse(chars[0]).ind + (isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = Piece.NULL.ind;
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = Rank.getByIndex((int) (chars[1] - '1')).bits;
			} else if (san.matches("^[KQRBN][a-h][1-8][a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[3] - 'a') + 8*(Integer.parseInt(Character.toString(chars[4])) - 1));
				movedPiece = (byte) (Piece.parse(chars[0]).ind + (isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = Piece.NULL.ind;
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = File.getByIndex((int) (chars[1] - 'a')).bits & Rank.getByIndex((int) (chars[2] - '1')).bits;
			} else if (san.matches("^[KQRBN]x[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				movedPiece = (byte) (Piece.parse(chars[0]).ind + (isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = offsetBoard[to];
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[KQRBN][a-h]x[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[3] - 'a') + 8*(Integer.parseInt(Character.toString(chars[4])) - 1));
				movedPiece = (byte) (Piece.parse(chars[0]).ind + (isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = offsetBoard[to];
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = File.getByIndex((int) (chars[1] - 'a')).bits;
			} else if (san.matches("^[KQRBN][1-8]x[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[3] - 'a') + 8*(Integer.parseInt(Character.toString(chars[4])) - 1));
				movedPiece = (byte) (Piece.parse(chars[0]).ind + (isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = offsetBoard[to];
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = Rank.getByIndex((int) (chars[1] - '1')).bits;
			} else if (san.matches("^[KQRBN][a-h][1-8]x[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte) ((int) (chars[4] - 'a') + 8*(Integer.parseInt(Character.toString(chars[5])) - 1));
				movedPiece = (byte) (Piece.parse(chars[0]).ind + (isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = offsetBoard[to];
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = File.getByIndex((int) (chars[1] - 'a')).bits & Rank.getByIndex((int) (chars[2] - '1')).bits;
			} else
				throw new ChessParseException("The move String violates the SAN standard.");
			if (from == -1) {
				mT = MoveSetDatabase.getByIndex(to);
				if (movedPiece == Piece.W_KING.ind)
					from = BitOperations.indexOfBit(mT.getKingMoveSet(whiteKing));
				else if (movedPiece == Piece.W_QUEEN.ind)
					from = BitOperations.indexOfBit(mT.getQueenMoveSet(whiteQueens & movablePieces & restriction, allOccupied));
				else if (movedPiece == Piece.W_ROOK.ind)
					from = BitOperations.indexOfBit(mT.getRookMoveSet(whiteRooks & movablePieces & restriction, allOccupied));
				else if (movedPiece == Piece.W_BISHOP.ind)
					from = BitOperations.indexOfBit(mT.getBishopMoveSet(whiteBishops & movablePieces & restriction, allOccupied));
				else if (movedPiece == Piece.W_KNIGHT.ind)
					from = BitOperations.indexOfBit(mT.getKnightMoveSet(whiteKnights & movablePieces & restriction));
				else if (movedPiece == Piece.B_KING.ind)
					from = BitOperations.indexOfBit(mT.getKingMoveSet(blackKing));
				else if (movedPiece == Piece.B_QUEEN.ind)
					from = BitOperations.indexOfBit(mT.getQueenMoveSet(blackQueens & movablePieces & restriction, allOccupied));
				else if (movedPiece == Piece.B_ROOK.ind)
					from = BitOperations.indexOfBit(mT.getRookMoveSet(blackRooks & movablePieces & restriction, allOccupied));
				else if (movedPiece == Piece.B_BISHOP.ind)
					from = BitOperations.indexOfBit(mT.getBishopMoveSet(blackBishops & movablePieces & restriction, allOccupied));
				else
					from = BitOperations.indexOfBit(mT.getKnightMoveSet(blackKnights & movablePieces & restriction));
			}
		}
		catch (Exception e) {
			throw new ChessParseException(e);
		}
		return new Move(from, to, movedPiece, capturedPiece, type);
	}
	/**
	 * Parses a Pure Algebraic Coordinate Notation move string into a {@link #engine.Move Move} object. If the input string does not pass the formal
	 * requirements of a PACN string, the method throws an {@link #engine.ChessTextParseException ChessTextParseException}. No legality checks are
	 * performed on the move.
	 * 
	 * @param pacn
	 * @return
	 * @throws ChessParseException
	 * @throws NullPointerException
	 */
	Move parsePACN(String pacn) throws ChessParseException, NullPointerException {
		byte from, to, movedPiece, capturedPiece, type;
		String input = pacn.trim().toLowerCase();
		if (input.length() != 4 && input.length() != 5)
			throw new ChessParseException("The input does not pass the formal requirements of a PACN String. Its length is neither 4 nor 5");
		from = (byte) ((int) (input.charAt(0) - 'a') + 8*(Integer.parseInt(Character.toString(input.charAt(1))) - 1));
		to = (byte) ((int) (input.charAt(2) - 'a') + 8*(Integer.parseInt(Character.toString(input.charAt(3))) - 1));
		movedPiece = offsetBoard[from];
		if (input.length() == 5) {
			switch (input.charAt(4)) {
				case 'q' : type = MoveType.PROMOTION_TO_QUEEN.ind;
				break;
				case 'r' : type = MoveType.PROMOTION_TO_ROOK.ind;
				break;
				case 'b' : type = MoveType.PROMOTION_TO_BISHOP.ind;
				break;
				case 'n' : type = MoveType.PROMOTION_TO_KNIGHT.ind;
				break;
				default:
					throw new ChessParseException("The input does not pass the formal requirements of a PACN String. Wrong promotion notation");
			}
			capturedPiece = offsetBoard[to];
		} else {
			if (movedPiece == Piece.W_PAWN.ind) {
				if (enPassantRights != EnPassantRights.NONE.ind && to == enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND) {
					type = MoveType.EN_PASSANT.ind;
					capturedPiece = Piece.B_PAWN.ind;
				} else {
					type = MoveType.NORMAL.ind;
					capturedPiece = offsetBoard[to];
				}
			} else if (movedPiece == Piece.B_PAWN.ind) {
				if (enPassantRights != EnPassantRights.NONE.ind && to == enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND) {
					type = MoveType.EN_PASSANT.ind;
					capturedPiece = Piece.W_PAWN.ind;
				} else {
					type = MoveType.NORMAL.ind;
					capturedPiece = offsetBoard[to];
				}
			} else {
				if ((movedPiece == Piece.W_KING.ind && from == Square.E1.ind && to == Square.G1.ind) ||
						(movedPiece == Piece.B_KING.ind && from == Square.E8.ind && to == Square.G8.ind)) {
					type = MoveType.SHORT_CASTLING.ind;
					capturedPiece = Piece.NULL.ind;
				} else if ((movedPiece == Piece.W_KING.ind && from == Square.E1.ind && to == Square.C1.ind) ||
						(movedPiece == Piece.B_KING.ind && from == Square.E8.ind && to == Square.C8.ind)) {
					type = MoveType.LONG_CASTLING.ind;
					capturedPiece = Piece.NULL.ind;
				} else {
					type = MoveType.NORMAL.ind;
					capturedPiece = offsetBoard[to];
				}
			}
		}
		return new Move(from, to, movedPiece, capturedPiece, type);
	}
	/**
	 * Creates and returns a string of the move in Standard Algebraic Notation. The move is assumed to be legal in the position on which
	 * the method is called.
	 * 
	 * @param move
	 * @return
	 */
	String toSAN(Move move) {
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
		destFile = Character.toString((char) (move.to%8 + 'a'));
		mPiece = Piece.getByNumericNotation(move.movedPiece);
		if (mPiece == Piece.W_PAWN || mPiece == Piece.B_PAWN)
			movedPiece  = "";
		else
			movedPiece  = Character.toString(mPiece.letter).toUpperCase();
		capture = move.capturedPiece == Piece.NULL.ind ? "" : "x";
		mT = MoveSetDatabase.getByIndex(move.to);
		movablePieces = ~getPinnedPieces(isWhitesTurn);
		switch (mPiece) {
			case W_KING:
				possOriginSqrs = 0;
				origin = "";
				break;
			case W_QUEEN:
				possOriginSqrs = mT.getQueenMoveSet(whiteQueens & movablePieces, allOccupied);
				origin = null;
				break;
			case W_ROOK:
				possOriginSqrs = mT.getRookMoveSet(whiteRooks & movablePieces, allOccupied);
				origin = null;
				break;
			case W_BISHOP:
				possOriginSqrs = mT.getBishopMoveSet(whiteBishops & movablePieces, allOccupied);
				origin = null;
				break;
			case W_KNIGHT:
				possOriginSqrs = mT.getKnightMoveSet(whiteKnights & movablePieces);
				origin = null;
				break;
			case W_PAWN:
				if (move.capturedPiece != Piece.NULL.ind) {
					possOriginSqrs = mT.getBlackPawnCaptureSet(whitePawns & movablePieces);
					if (BitOperations.hammingWeight(possOriginSqrs) == 1)
						origin = "";
					else
						origin = Character.toString((char) (move.from%8 + 'a'));
				} else {
					possOriginSqrs = 0;
					origin = "";
				}
				break;
			case B_KING:
				possOriginSqrs = 0;
				origin = "";
				break;
			case B_QUEEN:
				possOriginSqrs = mT.getQueenMoveSet(blackQueens & movablePieces, allOccupied);
				origin = null;
				break;
			case B_ROOK:
				possOriginSqrs = mT.getRookMoveSet(blackRooks & movablePieces, allOccupied);
				origin = null;
				break;
			case B_BISHOP:
				possOriginSqrs = mT.getBishopMoveSet(blackBishops & movablePieces, allOccupied);
				origin = null;
				break;
			case B_KNIGHT:
				possOriginSqrs = mT.getKnightMoveSet(blackKnights & movablePieces);
				origin = null;
				break;
			case B_PAWN:
				if (move.capturedPiece != Piece.NULL.ind) {
					possOriginSqrs = mT.getWhitePawnCaptureSet(blackPawns & movablePieces);
					if (BitOperations.hammingWeight(possOriginSqrs) == 1)
						origin = "";
					else
						origin = Character.toString((char) (move.from%8 + 'a'));
				} else {
					possOriginSqrs = 0;
					origin = "";
				}
				break;
			default:
				return null;
		}
		if (origin == null) {
			if (BitOperations.hammingWeight(possOriginSqrs) == 1)
				origin = "";
			else if (BitOperations.hammingWeight(File.getBySquareIndex(move.from).bits & possOriginSqrs) == 1)
				origin = Character.toString((char) (move.from%8 + 'a'));
			else if (BitOperations.hammingWeight(Rank.getBySquareIndex(move.from).bits & possOriginSqrs) == 1)
				origin = Integer.toString(move.from/8 + 1);
			else
				origin = Character.toString((char) (move.from%8 + 'a')) + Integer.toString(move.from/8 + 1);
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
	@Override
	public long hashKey() {
		return key;
	}
	@Override
	public Position deepCopy() {
		return new Position(this);
	}
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
}
