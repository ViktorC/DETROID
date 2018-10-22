package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.util.BitOperations;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A bitboard based chess position class that stores information about the side to move, en passant and castling
 * rights, the board position, all the moves made, and all the previous position states. It allows for the generation,
 * making, and taking back of chess moves.
 *
 * @author Viktor
 *
 */
public class Position {

	/**
	 * A FEN string for the starting chess position.
	 */
	public final static String START_POSITION_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

	private long whiteKing;
	private long whiteQueens;
	private long whiteRooks;
	private long whiteBishops;
	private long whiteKnights;
	private long whitePawns;
	private long blackKing;
	private long blackQueens;
	private long blackRooks;
	private long blackBishops;
	private long blackKnights;
	private long blackPawns;
	private long allWhiteOccupied;
	private long allBlackOccupied;
	private long allNonWhiteOccupied;
	private long allNonBlackOccupied;
	private long allEmpty;
	private long allOccupied;
	private long checkers;
	private boolean inCheck;
	private boolean whitesTurn;
	private byte[] squares;
	private int halfMoveIndex;
	private byte fiftyMoveRuleClock;
	private byte enPassantRights;
	private byte whiteCastlingRights;
	private byte blackCastlingRights;
	private long key;
	private long[] keyHistory;
	private ArrayDeque<Move> moveHistory;
	private ArrayDeque<PositionStateRecord> stateHistory;

	/**
	 * It parses a FEN string and initializes a position instance based on it. Beside standard six-field FEN-strings,
	 * it also accepts four-field Strings without the fifty-move rule clock and the move index.
	 *
	 * @param fen The FEN string.
	 * @return The position instance.
	 * @throws ChessParseException If the string is invalid.
	 */
	public static Position parse(String fen) throws ChessParseException {
		Position pos = new Position();
		String[] fenFields = fen.split(" ");
		if (fenFields.length != 4 && fenFields.length != 6)
			throw new ChessParseException("The FEN-String has an unallowed number of fields.");
		String board = fenFields[0];
		String turn = fenFields[1];
		String castling = fenFields[2];
		String enPassant = fenFields[3];
		String[] ranks = board.split("/");
		if (ranks.length != 8)
			throw new ChessParseException("The board position representation does not have eight ranks.");
		pos.squares = new byte[64];
		for (int i = 0; i < 64; i++)
			pos.squares[i] = (byte) Piece.NULL.ordinal();
		for (int i = 7; i >= 0; i--) {
			String rank = ranks[i];
			int index = 0;
			for (int j = 0; j < rank.length(); j++) {
				char piece = rank.charAt(j);
				int pieceNum = piece - '0';
				if (pieceNum >= 0 && pieceNum <= 8)
					index += pieceNum;
				else {
					long bitboard = Bitboard.Square.values()[index].getBitboard();
					switch (piece) {
						case 'K':
							pos.whiteKing = bitboard;
							pos.squares[index] = (byte) Piece.W_KING.ordinal();
							break;
						case 'Q':
							pos.whiteQueens |= bitboard;
							pos.squares[index] = (byte) Piece.W_QUEEN.ordinal();
							break;
						case 'R':
							pos.whiteRooks |= bitboard;
							pos.squares[index] = (byte) Piece.W_ROOK.ordinal();
							break;
						case 'B':
							pos.whiteBishops |= bitboard;
							pos.squares[index] = (byte) Piece.W_BISHOP.ordinal();
							break;
						case 'N':
							pos.whiteKnights |= bitboard;
							pos.squares[index] = (byte) Piece.W_KNIGHT.ordinal();
							break;
						case 'P':
							pos.whitePawns |= bitboard;
							pos.squares[index] = (byte) Piece.W_PAWN.ordinal();
							break;
						case 'k':
							pos.blackKing = bitboard;
							pos.squares[index] = (byte) Piece.B_KING.ordinal();
							break;
						case 'q':
							pos.blackQueens |= bitboard;
							pos.squares[index] = (byte) Piece.B_QUEEN.ordinal();
							break;
						case 'r':
							pos.blackRooks |= bitboard;
							pos.squares[index] = (byte) Piece.B_ROOK.ordinal();
							break;
						case 'b':
							pos.blackBishops |= bitboard;
							pos.squares[index] = (byte) Piece.B_BISHOP.ordinal();
							break;
						case 'n':
							pos.blackKnights |= bitboard;
							pos.squares[index] = (byte) Piece.B_KNIGHT.ordinal();
							break;
						case 'p':
							pos.blackPawns |= bitboard;
							pos.squares[index] = (byte) Piece.B_PAWN.ordinal();
					}
					index++;
				}
			}
		}
		pos.allWhiteOccupied = pos.whiteKing | pos.whiteQueens | pos.whiteRooks | pos.whiteBishops |
				pos.whiteKnights | pos.whitePawns;
		pos.allBlackOccupied = pos.blackKing | pos.blackQueens | pos.blackRooks | pos.blackBishops |
				pos.blackKnights | pos.blackPawns;
		pos.updateAggregateBitboards();
		pos.whitesTurn = turn.toLowerCase().compareTo("w") == Bitboard.EMPTY_BOARD;
		if (castling.equals("-")) {
			pos.whiteCastlingRights = (byte) CastlingRights.NONE.ordinal();
			pos.blackCastlingRights = (byte) CastlingRights.NONE.ordinal();
		}
		if (castling.length() < 1 || castling.length() > 4)
			throw new ChessParseException("Invalid length");
		if (castling.contains("K")) {
			pos.whiteCastlingRights = (byte) (castling.contains("Q") ? CastlingRights.ALL.ordinal() :
					CastlingRights.SHORT.ordinal());
		} else if (castling.contains("Q")) {
			pos.whiteCastlingRights = (byte) CastlingRights.LONG.ordinal();
		} else {
			pos.whiteCastlingRights = (byte) CastlingRights.NONE.ordinal();
		}
		if (castling.contains("k")) {
			pos.blackCastlingRights = (byte) (castling.contains("q") ? CastlingRights.ALL.ordinal() :
					CastlingRights.SHORT.ordinal());
		} else if (castling.contains("q")) {
			pos.blackCastlingRights = (byte) CastlingRights.LONG.ordinal();
		} else {
			pos.blackCastlingRights = (byte) CastlingRights.NONE.ordinal();
		}
		if (enPassant.length() > 2)
			throw new ChessParseException("Illegal en passant field.");
		if (enPassant.equals("-")) {
			pos.enPassantRights = (byte) (enPassant.equals("-") ? EnPassantRights.NONE.ordinal() :
					EnPassantRights.values()[enPassant.toLowerCase().charAt(0) - 'a'].ordinal());
		}
		if (fenFields.length == 6) {
			try {
				pos.fiftyMoveRuleClock = (byte) Math.max(0, Integer.parseInt(fenFields[4]));
			} catch (NumberFormatException e) {
				throw new ChessParseException("The fifty-move rule clock field of the " +
						"FEN-string does not conform to the standards. Parsing not possible.");
			}
			try {
				int moveIndex = (Integer.parseInt(fenFields[5]) - 1)*2;
				if (!pos.whitesTurn)
					moveIndex++;
				pos.halfMoveIndex = Math.max(0, moveIndex);
			} catch (NumberFormatException e) {
				throw new ChessParseException("The move index field does not conform to the standards. " +
						"Parsing not possible.");
			}
		}
		pos.checkers = pos.getCheckers();
		pos.inCheck = pos.checkers != Bitboard.EMPTY_BOARD;
		pos.key = ZobristKeyGenerator.getInstance().generateHashKey(pos);
		if (pos.halfMoveIndex >= pos.keyHistory.length) {
			int keyHistLength = pos.keyHistory.length;
			while (keyHistLength <= pos.halfMoveIndex)
				keyHistLength += (keyHistLength >> 1);
			pos.keyHistory = new long[keyHistLength];
		}
		pos.keyHistory[pos.halfMoveIndex] = pos.key;
		return pos;
	}
	/**
	 * Initializes a default, empty position instance.
	 */
	private Position() {
		moveHistory = new ArrayDeque<>();
		stateHistory = new ArrayDeque<>();
		keyHistory = new long[32]; // Factor of two.
	}
	/**
	 * Clones the specified position instance.
	 *
	 * @param pos The position to clone.
	 */
	public Position(Position pos) {
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
		allEmpty = pos.allEmpty;
		enPassantRights = pos.enPassantRights;
		whiteCastlingRights = pos.whiteCastlingRights;
		blackCastlingRights = pos.blackCastlingRights;
		whitesTurn = pos.whitesTurn;
		inCheck = pos.inCheck;
		checkers = pos.checkers;
		halfMoveIndex = pos.halfMoveIndex;
		fiftyMoveRuleClock = pos.fiftyMoveRuleClock;
		key = pos.key;
		squares = Arrays.copyOf(pos.squares, pos.squares.length);
		keyHistory = Arrays.copyOf(pos.keyHistory, pos.keyHistory.length);
		moveHistory = new ArrayDeque<>(pos.moveHistory);
		stateHistory = new ArrayDeque<>(pos.stateHistory);
	}
	/**
	 * @return A bitboard for the white king.
	 */
	public long getWhiteKing() {
		return whiteKing;
	}
	/**
	 * @return A bitboard for the white queens.
	 */
	public long getWhiteQueens() {
		return whiteQueens;
	}
	/**
	 * @return A bitboard for the white rooks.
	 */
	public long getWhiteRooks() {
		return whiteRooks;
	}
	/**
	 * @return A bitboard for the white bishops.
	 */
	public long getWhiteBishops() {
		return whiteBishops;
	}
	/**
	 * @return A bitboard for the white knights.
	 */
	public long getWhiteKnights() {
		return whiteKnights;
	}
	/**
	 * @return A bitboard for the white pawns.
	 */
	public long getWhitePawns() {
		return whitePawns;
	}
	/**
	 * @return A bitboard for the black king.
	 */
	public long getBlackKing() {
		return blackKing;
	}
	/**
	 * @return A bitboard for the black queens.
	 */
	public long getBlackQueens() {
		return blackQueens;
	}
	/**
	 * @return A bitboard for the black rooks.
	 */
	public long getBlackRooks() {
		return blackRooks;
	}
	/**
	 * @return A bitboard for the black bishops.
	 */
	public long getBlackBishops() {
		return blackBishops;
	}
	/**
	 * @return A bitboard for the black knights.
	 */
	public long getBlackKnights() {
		return blackKnights;
	}
	/**
	 * @return A bitboard for the black pawns.
	 */
	public long getBlackPawns() {
		return blackPawns;
	}
	/**
	 * @return A bitboard for all squares occupied by white pieces.
	 */
	public long getAllWhiteOccupied() {
		return allWhiteOccupied;
	}
	/**
	 * @return A bitboard for all squares occupied by black pieces.
	 */
	public long getAllBlackOccupied() {
		return allBlackOccupied;
	}

	/**
	 * @return A bitboard for all squares not occupied by white pieces.
	 */
	public long getAllNonWhiteOccupied() {
		return allNonWhiteOccupied;
	}
	/**
	 * @return A bitboard for all squares not occupied by black pieces.
	 */
	public long getAllNonBlackOccupied() {
		return allNonBlackOccupied;
	}
	/**
	 * @return A bitboard for all occupied squares.
	 */
	public long getAllOccupied() {
		return allOccupied;
	}
	/**
	 * @return A bitboard for all empty squares.
	 */
	public long getAllEmpty() {
		return allEmpty;
	}
	/**
	 * @return A bitboard for all pieces of the color to move checking the opponent's king.
	 */
	public long getCheckers() {
		return checkers;
	}
	/**
	 * @return Whether the side to move is in check.
	 */
	public boolean isInCheck() {
		return inCheck;
	}
	/**
	 * @return Whether it is white's turn to make a move.
	 */
	public boolean isWhitesTurn() {
		return whitesTurn;
	}
	/**
	 * @param sqrInd The index of the square.
	 * @return The piece occupying the square denoted by the specified index.
	 */
	public byte getPiece(int sqrInd) {
		return squares[sqrInd];
	}
	/**
	 * @return The number of half moves made.
	 */
	public int getHalfMoveIndex() {
		return halfMoveIndex;
	}
	/**
	 * @return The number of half moves made since the last pawn move or capture.
	 */
	public byte getFiftyMoveRuleClock() {
		return fiftyMoveRuleClock;
	}
	/**
	 * @return The index denoting the file on which en passant is possible.
	 */
	public byte getEnPassantRights() {
		return enPassantRights;
	}
	/**
	 * @return The castling type that denotes to what extent it would still be possible to castle for white regardless
	 * of whether it is actually legally executable in the current position.
	 */
	public byte getWhiteCastlingRights() {
		return whiteCastlingRights;
	}
	/**
	 * @return The castling type that denotes to what extent it would still be possible to castle for black regardless
	 * of whether it is actually legally executable in the current position.
	 */
	public byte getBlackCastlingRights() {
		return blackCastlingRights;
	}
	/**
	 * @return A Zobrist key that is fairly close to a unique representation of the state of the instance in one 64
	 * bit number.
	 */
	public long getKey() {
		return key;
	}
	/**
	 * @return A queue of all the moves made so far.
	 */
	public ArrayDeque<Move> getMoveHistory() {
		return new ArrayDeque<>(moveHistory);
	}
	/**
	 * @return The last move made.
	 */
	public Move getLastMove() {
		return moveHistory.peekFirst();
	}
	/**
	 * @return A queue of all the state history records so far.
	 */
	public ArrayDeque<PositionStateRecord> getStateHistory() {
		return new ArrayDeque<>(stateHistory);
	}
	/**
	 * @return The last position state.
	 */
	public PositionStateRecord getLastState() {
		return stateHistory.peekFirst();
	}
	/**
	 * @param numberOfTimes The hypothetical number of times the position has occurred before. E.g. for a three-fold
	 * repetition check, it would be 2.
	 * @return Whether the current position has already occurred before at least the specified number of times.
	 */
	public boolean hasRepeated(int numberOfTimes) {
		int repetitions = 0;
		if (fiftyMoveRuleClock >= 4) {
			for (int i = halfMoveIndex - 4; i >= halfMoveIndex - fiftyMoveRuleClock; i -= 2) {
				if (keyHistory[i] == key) {
					repetitions++;
					if (repetitions >= numberOfTimes)
						return true;
				}
			}
		}
		return repetitions >= numberOfTimes;
	}
	private void updateAggregateBitboards() {
		allNonWhiteOccupied = ~allWhiteOccupied;
		allNonBlackOccupied = ~allBlackOccupied;
		allOccupied = allWhiteOccupied | allBlackOccupied;
		allEmpty = ~allOccupied;
	}
	private void captureWhitePieceOnBitboards(byte capturedPiece, long toBit) {
		if (capturedPiece != Piece.NULL.ordinal()) {
			if (capturedPiece == Piece.W_QUEEN.ordinal())
				whiteQueens ^= toBit;
			else if (capturedPiece == Piece.W_ROOK.ordinal())
				whiteRooks ^= toBit;
			else if (capturedPiece == Piece.W_BISHOP.ordinal())
				whiteBishops ^= toBit;
			else if (capturedPiece == Piece.W_KNIGHT.ordinal())
				whiteKnights ^= toBit;
			else
				whitePawns ^= toBit;
			allWhiteOccupied ^= toBit;
		}
	}
	private void captureBlackPieceOnBitboards(byte capturedPiece, long toBit) {
		if (capturedPiece != Piece.NULL.ordinal()) {
			if (capturedPiece == Piece.B_QUEEN.ordinal())
				blackQueens ^= toBit;
			else if (capturedPiece == Piece.B_ROOK.ordinal())
				blackRooks ^= toBit;
			else if (capturedPiece == Piece.B_BISHOP.ordinal())
				blackBishops ^= toBit;
			else if (capturedPiece == Piece.B_KNIGHT.ordinal())
				blackKnights ^= toBit;
			else
				blackPawns ^= toBit;
			allBlackOccupied ^= toBit;
		}
	}
	private void makeWhiteNormalMoveOnBitboards(long fromBit, long toBit, byte movedPiece, byte capturedPiece) {
		long changedBits = fromBit | toBit;
		if (movedPiece == Piece.W_KING.ordinal())
			whiteKing ^= changedBits;
		else if (movedPiece == Piece.W_QUEEN.ordinal())
			whiteQueens ^= changedBits;
		else if (movedPiece == Piece.W_ROOK.ordinal())
			whiteRooks ^= changedBits;
		else if (movedPiece == Piece.W_BISHOP.ordinal())
			whiteBishops ^= changedBits;
		else if (movedPiece == Piece.W_KNIGHT.ordinal())
			whiteKnights ^= changedBits;
		else
			whitePawns ^= changedBits;
		allWhiteOccupied ^= changedBits;
		captureBlackPieceOnBitboards(capturedPiece, toBit);
		updateAggregateBitboards();
	}
	private void makeBlackNormalMoveOnBitboards(long fromBit, long toBit, byte movedPiece, byte capturedPiece) {
		long changedBits = fromBit | toBit;
		if (movedPiece == Piece.B_KING.ordinal())
			blackKing ^= changedBits;
		else if (movedPiece == Piece.B_QUEEN.ordinal())
			blackQueens ^= changedBits;
		else if (movedPiece == Piece.B_ROOK.ordinal())
			blackRooks ^= changedBits;
		else if (movedPiece == Piece.B_BISHOP.ordinal())
			blackBishops ^= changedBits;
		else if (movedPiece == Piece.B_KNIGHT.ordinal())
			blackKnights ^= changedBits;
		else
			blackPawns ^= changedBits;
		allBlackOccupied ^= changedBits;
		captureWhitePieceOnBitboards(capturedPiece, toBit);
		updateAggregateBitboards();
	}
	private void makeWhiteShortCastlingMoveOnBitboards() {
		long changedBits = Bitboard.Square.E1.getBitboard() | Bitboard.Square.G1.getBitboard();
		whiteKing ^= changedBits;
		long rookChangedBits = Bitboard.Square.H1.getBitboard() | Bitboard.Square.F1.getBitboard();
		whiteRooks ^= rookChangedBits;
		allWhiteOccupied ^= (changedBits | rookChangedBits);
		updateAggregateBitboards();
	}
	private void makeBlackShortCastlingMoveOnBitboards() {
		long changedBits = Bitboard.Square.E8.getBitboard() | Bitboard.Square.G8.getBitboard();
		blackKing ^= changedBits;
		long rookChangedBits = Bitboard.Square.H8.getBitboard() | Bitboard.Square.F8.getBitboard();
		blackRooks ^= rookChangedBits;
		allBlackOccupied ^= (changedBits | rookChangedBits);
		updateAggregateBitboards();
	}
	private void makeWhiteLongCastlingMoveOnBitboards() {
		long changedBits = Bitboard.Square.E1.getBitboard() | Bitboard.Square.C1.getBitboard();
		whiteKing ^= changedBits;
		long rookChangedBits = Bitboard.Square.A1.getBitboard() | Bitboard.Square.D1.getBitboard();
		whiteRooks ^= rookChangedBits;
		allWhiteOccupied ^= (changedBits | rookChangedBits);
		updateAggregateBitboards();
	}
	private void makeBlackLongCastlingMoveOnBitboards() {
		long changedBits = Bitboard.Square.E8.getBitboard() | Bitboard.Square.C8.getBitboard();
		blackKing ^= changedBits;
		long rookChangedBits = Bitboard.Square.A8.getBitboard() | Bitboard.Square.D8.getBitboard();
		blackRooks ^= rookChangedBits;
		allBlackOccupied ^= (changedBits | rookChangedBits);
		updateAggregateBitboards();
	}
	private void makeWhiteEnPassantMoveOnBitboards(long fromBit, long toBit) {
		long changedBits = fromBit | toBit;
		whitePawns ^= changedBits;
		allWhiteOccupied ^= changedBits;
		long victimBit = Bitboard.computeBlackPawnAdvanceSets(toBit, Bitboard.FULL_BOARD);
		blackPawns ^= victimBit;
		allBlackOccupied ^= victimBit;
		updateAggregateBitboards();
	}
	private void makeBlackEnPassantMoveOnBitboards(long fromBit, long toBit) {
		long changedBits = fromBit | toBit;
		blackPawns ^= changedBits;
		allBlackOccupied ^= changedBits;
		long victimBit = Bitboard.computeWhitePawnAdvanceSets(toBit, Bitboard.FULL_BOARD);
		whitePawns ^= victimBit;
		allWhiteOccupied ^= victimBit;
		updateAggregateBitboards();
	}
	private void pushWhitePawnToPromotionOnBitboards(long fromBit, long toBit, byte capturedPiece) {
		whitePawns ^= fromBit;
		allWhiteOccupied ^= (fromBit | toBit);
		captureBlackPieceOnBitboards(capturedPiece, toBit);
		updateAggregateBitboards();
	}
	private void pushBlackPawnToPromotionOnBitboards(long fromBit, long toBit, byte capturedPiece) {
		blackPawns ^= fromBit;
		allBlackOccupied ^= (fromBit | toBit);
		captureWhitePieceOnBitboards(capturedPiece, toBit);
		updateAggregateBitboards();
	}
	private void makeWhiteQueenPromotionMoveOnBitboards(long fromBit, long toBit, byte capturedPiece) {
		whiteQueens ^= toBit;
		pushWhitePawnToPromotionOnBitboards(fromBit, toBit, capturedPiece);
	}
	private void makeBlackQueenPromotionMoveOnBitboards(long fromBit, long toBit, byte capturedPiece) {
		blackQueens ^= toBit;
		pushBlackPawnToPromotionOnBitboards(fromBit, toBit, capturedPiece);
	}
	private void makeWhiteRookPromotionMoveOnBitboards(long fromBit, long toBit, byte capturedPiece) {
		whiteRooks ^= toBit;
		pushWhitePawnToPromotionOnBitboards(fromBit, toBit, capturedPiece);
	}
	private void makeBlackRookPromotionMoveOnBitboards(long fromBit, long toBit, byte capturedPiece) {
		blackRooks ^= toBit;
		pushBlackPawnToPromotionOnBitboards(fromBit, toBit, capturedPiece);
	}
	private void makeWhiteBishopPromotionMoveOnBitboards(long fromBit, long toBit, byte capturedPiece) {
		whiteBishops ^= toBit;
		pushWhitePawnToPromotionOnBitboards(fromBit, toBit, capturedPiece);
	}
	private void makeBlackBishopPromotionMoveOnBitboards(long fromBit, long toBit, byte capturedPiece) {
		blackBishops ^= toBit;
		pushBlackPawnToPromotionOnBitboards(fromBit, toBit, capturedPiece);
	}
	private void makeWhiteKnightPromotionMoveOnBitboards(long fromBit, long toBit, byte capturedPiece) {
		whiteKnights ^= toBit;
		pushWhitePawnToPromotionOnBitboards(fromBit, toBit, capturedPiece);
	}
	private void makeBlackKnightPromotionMoveOnBitboards(long fromBit, long toBit, byte capturedPiece) {
		blackKnights ^= toBit;
		pushBlackPawnToPromotionOnBitboards(fromBit, toBit, capturedPiece);
	}
	private void makeWhiteNormalMoveOnBoard(byte from, byte to, byte movedPiece, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = movedPiece;
		makeWhiteNormalMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), movedPiece, capturedPiece);
	}
	private void makeBlackNormalMoveOnBoard(byte from, byte to, byte movedPiece, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = movedPiece;
		makeBlackNormalMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), movedPiece, capturedPiece);
	}
	private void makeWhiteShortCastlingMoveOnBoard() {
		squares[Bitboard.Square.H1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.F1.ordinal()] = (byte) Piece.W_ROOK.ordinal();
		squares[Bitboard.Square.E1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.G1.ordinal()] = (byte) Piece.W_KING.ordinal();
		makeWhiteShortCastlingMoveOnBitboards();
	}
	private void makeBlackShortCastlingMoveOnBoard() {
		squares[Bitboard.Square.H8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.F8.ordinal()] = (byte) Piece.B_ROOK.ordinal();
		squares[Bitboard.Square.E8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.G8.ordinal()] = (byte) Piece.B_KING.ordinal();
		makeBlackShortCastlingMoveOnBitboards();
	}
	private void makeWhiteLongCastlingMoveOnBoard() {
		squares[Bitboard.Square.A1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.D1.ordinal()] = (byte) Piece.W_ROOK.ordinal();
		squares[Bitboard.Square.E1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.C1.ordinal()] = (byte) Piece.W_KING.ordinal();
		makeWhiteLongCastlingMoveOnBitboards();
	}
	private void makeBlackLongCastlingMoveOnBoard() {
		squares[Bitboard.Square.A8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.D8.ordinal()] = (byte) Piece.B_ROOK.ordinal();
		squares[Bitboard.Square.E8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.C8.ordinal()] = (byte) Piece.B_KING.ordinal();
		makeBlackLongCastlingMoveOnBitboards();
	}
	private void makeWhiteEnPassantMoveOnBoard(byte from, byte to) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.W_PAWN.ordinal();
		squares[to - 8] = (byte) Piece.NULL.ordinal();
		makeWhiteEnPassantMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to));
	}
	private void makeBlackEnPassantMoveOnBoard(byte from, byte to) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.B_PAWN.ordinal();
		squares[to + 8] = (byte) Piece.NULL.ordinal();
		makeBlackEnPassantMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to));
	}
	private void makeWhiteQueenPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.W_QUEEN.ordinal();
		makeWhiteQueenPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void makeBlackQueenPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.B_QUEEN.ordinal();
		makeBlackQueenPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void makeWhiteRookPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.W_ROOK.ordinal();
		makeWhiteRookPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void makeBlackRookPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.B_ROOK.ordinal();
		makeBlackRookPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void makeWhiteBishopPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.W_BISHOP.ordinal();
		makeWhiteBishopPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void makeBlackBishopPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.B_BISHOP.ordinal();
		makeBlackBishopPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void makeWhiteKnightPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.W_KNIGHT.ordinal();
		makeWhiteKnightPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void makeBlackKnightPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.B_KNIGHT.ordinal();
		makeBlackKnightPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to),
				capturedPiece);
	}
	private void makeWhiteMoveAndUpdateKeyOnBoard(Move move) {
		byte moveType = move.getType();
		ZobristKeyGenerator gen = ZobristKeyGenerator.getInstance();
		if (moveType == MoveType.NORMAL.ordinal()) {
			makeWhiteNormalMoveOnBoard(move.getFrom(), move.getTo(), move.getMovedPiece(), move.getCapturedPiece());
			key = gen.getUpdatedBoardHashKeyAfterNormalMove(key, move.getFrom(), move.getTo(), move.getMovedPiece(),
					move.getCapturedPiece());
		} else if (moveType == MoveType.SHORT_CASTLING.ordinal()) {
			makeWhiteShortCastlingMoveOnBoard();
			key = gen.getUpdatedBoardHashKeyAfterWhiteShortCastlinglMove(key);
		} else if (moveType == MoveType.LONG_CASTLING.ordinal()) {
			makeWhiteLongCastlingMoveOnBoard();
			key = gen.getUpdatedBoardHashKeyAfterWhiteLongCastlinglMove(key);
		} else if (moveType == MoveType.EN_PASSANT.ordinal()) {
			makeWhiteEnPassantMoveOnBoard(move.getFrom(), move.getTo());
			key = gen.getUpdatedBoardHashKeyAfterWhiteEnPassantMove(key, move.getFrom(), move.getTo());
		} else if (moveType == MoveType.PROMOTION_TO_QUEEN.ordinal()) {
			makeWhiteQueenPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.getUpdatedBoardHashKeyAfterWhiteQueenPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_ROOK.ordinal()) {
			makeWhiteRookPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.getUpdatedBoardHashKeyAfterWhiteRookPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_BISHOP.ordinal()) {
			makeWhiteBishopPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.getUpdatedBoardHashKeyAfterWhiteBishopPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		} else {
			makeWhiteKnightPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.getUpdatedBoardHashKeyAfterWhiteKnightPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		}
	}
	private void makeBlackMoveAndUpdateKeyOnBoard(Move move) {
		byte moveType = move.getType();
		ZobristKeyGenerator gen = ZobristKeyGenerator.getInstance();
		if (moveType == MoveType.NORMAL.ordinal()) {
			makeBlackNormalMoveOnBoard(move.getFrom(), move.getTo(), move.getMovedPiece(), move.getCapturedPiece());
			key = gen.getUpdatedBoardHashKeyAfterNormalMove(key, move.getFrom(), move.getTo(), move.getMovedPiece(),
					move.getCapturedPiece());
		} else if (moveType == MoveType.SHORT_CASTLING.ordinal()) {
			makeBlackShortCastlingMoveOnBoard();
			key = gen.getUpdatedBoardHashKeyAfterBlackShortCastlinglMove(key);
		} else if (moveType == MoveType.LONG_CASTLING.ordinal()) {
			makeBlackLongCastlingMoveOnBoard();
			key = gen.getUpdatedBoardHashKeyAfterBlackLongCastlinglMove(key);
		} else if (moveType == MoveType.EN_PASSANT.ordinal()) {
			makeBlackEnPassantMoveOnBoard(move.getFrom(), move.getTo());
			key = gen.getUpdatedBoardHashKeyAfterBlackEnPassantMove(key, move.getFrom(), move.getTo());
		} else if (moveType == MoveType.PROMOTION_TO_QUEEN.ordinal()) {
			makeBlackQueenPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.getUpdatedBoardHashKeyAfterBlackQueenPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_ROOK.ordinal()) {
			makeBlackRookPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.getUpdatedBoardHashKeyAfterBlackRookPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_BISHOP.ordinal()) {
			makeBlackBishopPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.getUpdatedBoardHashKeyAfterBlackBishopPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		} else {
			makeBlackKnightPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.getUpdatedBoardHashKeyAfterBlackKnightPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		}
	}
	private void updateWhiteCastlingRights() {
		if (whiteCastlingRights == CastlingRights.NONE.ordinal())
			return;
		if (whiteCastlingRights == CastlingRights.SHORT.ordinal()) {
			if (squares[Bitboard.Square.E1.ordinal()] != Piece.W_KING.ordinal() ||
					squares[Bitboard.Square.H1.ordinal()] != Piece.W_ROOK.ordinal())
				whiteCastlingRights = (byte) CastlingRights.NONE.ordinal();
		} else if (whiteCastlingRights == CastlingRights.LONG.ordinal()) {
			if (squares[Bitboard.Square.E1.ordinal()] != Piece.W_KING.ordinal() ||
					squares[Bitboard.Square.A1.ordinal()] != Piece.W_ROOK.ordinal())
				whiteCastlingRights = (byte) CastlingRights.NONE.ordinal();
		} else {
			if (squares[Bitboard.Square.E1.ordinal()] == Piece.W_KING.ordinal()) {
				if (squares[Bitboard.Square.H1.ordinal()] != Piece.W_ROOK.ordinal()) {
					if (squares[Bitboard.Square.A1.ordinal()] != Piece.W_ROOK.ordinal())
						whiteCastlingRights = (byte) CastlingRights.NONE.ordinal();
					else
						whiteCastlingRights = (byte) CastlingRights.LONG.ordinal();
				} else if (squares[Bitboard.Square.A1.ordinal()] != Piece.W_ROOK.ordinal())
					whiteCastlingRights = (byte) CastlingRights.SHORT.ordinal();
			} else
				whiteCastlingRights = (byte) CastlingRights.NONE.ordinal();
		}
	}
	private void updateBlackCastlingRights() {
		if (blackCastlingRights == CastlingRights.NONE.ordinal())
			return;
		if (blackCastlingRights == CastlingRights.SHORT.ordinal()) {
			if (squares[Bitboard.Square.E8.ordinal()] != Piece.B_KING.ordinal() ||
					squares[Bitboard.Square.H8.ordinal()] != Piece.B_ROOK.ordinal())
				blackCastlingRights = (byte) CastlingRights.NONE.ordinal();
		} else if (blackCastlingRights == CastlingRights.LONG.ordinal()) {
			if (squares[Bitboard.Square.E8.ordinal()] != Piece.B_KING.ordinal() ||
					squares[Bitboard.Square.A8.ordinal()] != Piece.B_ROOK.ordinal())
				blackCastlingRights = (byte) CastlingRights.NONE.ordinal();
		} else {
			if (squares[Bitboard.Square.E8.ordinal()] == Piece.B_KING.ordinal()) {
				if (squares[Bitboard.Square.H8.ordinal()] != Piece.B_ROOK.ordinal()) {
					if (squares[Bitboard.Square.A8.ordinal()] != Piece.B_ROOK.ordinal())
						blackCastlingRights = (byte) CastlingRights.NONE.ordinal();
					else
						blackCastlingRights = (byte) CastlingRights.LONG.ordinal();
				} else if (squares[Bitboard.Square.A8.ordinal()] != Piece.B_ROOK.ordinal())
					blackCastlingRights = (byte) CastlingRights.SHORT.ordinal();
			} else
				blackCastlingRights = (byte) CastlingRights.NONE.ordinal();
		}
	}
	private void ensureKeyHistoryCapacity() {
		if (keyHistory.length - halfMoveIndex <= 3)
			keyHistory = Arrays.copyOf(keyHistory, keyHistory.length + (keyHistory.length >> 1));
	}
	private long getWhiteCheckers(int sqrInd) {
		MoveSetBase dB = MoveSetBase.values()[sqrInd];
		long attackers = whiteKnights & dB.getKnightMoveMask();
		attackers |= whitePawns & dB.getPawnBlackCaptureMoveMask();
		attackers |= (whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied);
		attackers |= (whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
		return attackers;
	}
	private long getBlackCheckers(int sqrInd) {
		MoveSetBase dB = MoveSetBase.values()[sqrInd];
		long attackers = blackKnights & dB.getKnightMoveMask();
		attackers |= blackPawns & dB.getPawnWhiteCaptureMoveMask();
		attackers |= (blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied);
		attackers |= (blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied);
		return attackers;
	}
	/**
	 * Makes the specified move. No legality checks are performed.
	 *
	 * @param move The move to make.
	 */
	public void makeMove(Move move) {
		moveHistory.addFirst(move);
		stateHistory.addFirst(new PositionStateRecord(whiteCastlingRights, blackCastlingRights, enPassantRights,
				fiftyMoveRuleClock, checkers));
		if (whitesTurn) {
			makeWhiteMoveAndUpdateKeyOnBoard(move);
			checkers = getWhiteCheckers(BitOperations.indexOfBit(blackKing));
			updateBlackCastlingRights();
			enPassantRights = (move.getMovedPiece() == Piece.W_PAWN.ordinal() &&
					move.getTo() - move.getFrom() == 16) ? (byte) (move.getTo()%8) : 8;
			fiftyMoveRuleClock = (move.getCapturedPiece() != Piece.NULL.ordinal() ||
					move.getMovedPiece() == Piece.W_PAWN.ordinal()) ? 0 : (byte) (fiftyMoveRuleClock + 1);
		} else {
			makeBlackMoveAndUpdateKeyOnBoard(move);
			checkers = getBlackCheckers(BitOperations.indexOfBit(whiteKing));
			updateWhiteCastlingRights();
			enPassantRights = (move.getMovedPiece() == Piece.B_PAWN.ordinal() &&
					move.getFrom() - move.getTo() == 16) ? (byte) (move.getTo()%8) : 8;
			fiftyMoveRuleClock = (move.getCapturedPiece() != Piece.NULL.ordinal() ||
					move.getMovedPiece() == Piece.B_PAWN.ordinal()) ? 0 : (byte) (fiftyMoveRuleClock + 1);
		}
		whitesTurn = !whitesTurn;
		inCheck = checkers != Bitboard.EMPTY_BOARD;
		halfMoveIndex++;
		ensureKeyHistoryCapacity();
		key = ZobristKeyGenerator.getInstance().getUpdatedOffBoardHashKey(key, getLastState(),
				whiteCastlingRights, blackCastlingRights, enPassantRights);
		keyHistory[halfMoveIndex] = key;
	}
	/**
	 * Makes a null move.
	 */
	public void makeNullMove() {
		moveHistory.addFirst(Move.NULL_MOVE);
		stateHistory.addFirst(new PositionStateRecord(whiteCastlingRights, blackCastlingRights, enPassantRights,
				fiftyMoveRuleClock, checkers));
		if (whitesTurn) {
			updateBlackCastlingRights();
			whitesTurn = false;
		} else {
			updateWhiteCastlingRights();
			whitesTurn = true;
		}
		enPassantRights = (byte) EnPassantRights.NONE.ordinal();
		halfMoveIndex++;
		ensureKeyHistoryCapacity();
        key = ZobristKeyGenerator.getInstance().getUpdatedOffBoardHashKey(key, getLastState(),
				whiteCastlingRights, blackCastlingRights, enPassantRights);
		keyHistory[halfMoveIndex] = key;
	}
	private void unmakeWhiteNormalMoveOnBoard(byte from, byte to, byte movedPiece, byte capturedPiece) {
		squares[from] = movedPiece;
		squares[to] = capturedPiece;
		makeWhiteNormalMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), movedPiece, capturedPiece);
	}
	private void unmakeBlackNormalMoveOnBoard(byte from, byte to, byte movedPiece, byte capturedPiece) {
		squares[from] = movedPiece;
		squares[to] = capturedPiece;
		makeBlackNormalMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), movedPiece, capturedPiece);
	}
	private void unmakeWhiteShortCastlingMoveOnBoard() {
		squares[Bitboard.Square.F1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.H1.ordinal()] = (byte) Piece.W_ROOK.ordinal();
		squares[Bitboard.Square.G1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.E1.ordinal()] = (byte) Piece.W_KING.ordinal();
		makeWhiteShortCastlingMoveOnBitboards();
	}
	private void unmakeBlackShortCastlingMoveOnBoard() {
		squares[Bitboard.Square.F8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.H8.ordinal()] = (byte) Piece.B_ROOK.ordinal();
		squares[Bitboard.Square.G8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.E8.ordinal()] = (byte) Piece.B_KING.ordinal();
		makeBlackShortCastlingMoveOnBitboards();
	}
	private void unmakeWhiteLongCastlingMoveOnBoard() {
		squares[Bitboard.Square.D1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.A1.ordinal()] = (byte) Piece.W_ROOK.ordinal();
		squares[Bitboard.Square.C1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.E1.ordinal()] = (byte) Piece.W_KING.ordinal();
		makeWhiteLongCastlingMoveOnBitboards();
	}
	private void unmakeBlackLongCastlingMoveOnBoard() {
		squares[Bitboard.Square.D8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.A8.ordinal()] = (byte) Piece.B_ROOK.ordinal();
		squares[Bitboard.Square.C8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.E8.ordinal()] = (byte) Piece.B_KING.ordinal();
		makeBlackLongCastlingMoveOnBitboards();
	}
	private void unmakeWhiteEnPassantMoveOnBoard(byte from, byte to) {
		squares[from] = (byte) Piece.W_PAWN.ordinal();
		squares[to] = (byte) Piece.NULL.ordinal();
		squares[to - 8] = (byte) Piece.B_PAWN.ordinal();
		makeWhiteEnPassantMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to));
	}
	private void unmakeBlackEnPassantMoveOnBoard(byte from, byte to) {
		squares[from] = (byte) Piece.B_PAWN.ordinal();
		squares[to] = (byte) Piece.NULL.ordinal();
		squares[to + 8] = (byte) Piece.W_PAWN.ordinal();
		makeBlackEnPassantMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to));
	}
	private void unmakeWhiteQueenPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.W_PAWN.ordinal();
		squares[to] = capturedPiece;
		makeWhiteQueenPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void unmakeBlackQueenPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.B_PAWN.ordinal();
		squares[to] = capturedPiece;
		makeBlackQueenPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void unmakeWhiteRookPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.W_PAWN.ordinal();
		squares[to] = capturedPiece;
		makeWhiteRookPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void unmakeBlackRookPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.B_PAWN.ordinal();
		squares[to] = capturedPiece;
		makeBlackRookPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void unmakeWhiteBishopPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.W_PAWN.ordinal();
		squares[to] = capturedPiece;
		makeWhiteBishopPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void unmakeBlackBishopPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.B_PAWN.ordinal();
		squares[to] = capturedPiece;
		makeBlackBishopPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void unmakeWhiteKnightPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.W_PAWN.ordinal();
		squares[to] = capturedPiece;
		makeWhiteKnightPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void unmakeBlackKnightPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.B_PAWN.ordinal();
		squares[to] = capturedPiece;
		makeBlackKnightPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
	}
	private void unmakeWhiteMoveOnBoard(Move move) {
		byte moveType = move.getType();
		if (moveType == MoveType.NORMAL.ordinal())
			unmakeWhiteNormalMoveOnBoard(move.getFrom(), move.getTo(), move.getMovedPiece(), move.getCapturedPiece());
		else if (moveType == MoveType.SHORT_CASTLING.ordinal())
			unmakeWhiteShortCastlingMoveOnBoard();
		else if (moveType == MoveType.LONG_CASTLING.ordinal())
			unmakeWhiteLongCastlingMoveOnBoard();
		else if (moveType == MoveType.EN_PASSANT.ordinal())
			unmakeWhiteEnPassantMoveOnBoard(move.getFrom(), move.getTo());
		else if (moveType == MoveType.PROMOTION_TO_QUEEN.ordinal())
			unmakeWhiteQueenPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
		else if (moveType == MoveType.PROMOTION_TO_ROOK.ordinal())
			unmakeWhiteRookPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
		else if (moveType == MoveType.PROMOTION_TO_BISHOP.ordinal())
			unmakeWhiteBishopPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
		else
			unmakeWhiteKnightPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
	}
	private void unmakeBlackMoveOnBoard(Move move) {
		byte moveType = move.getType();
		if (moveType == MoveType.NORMAL.ordinal())
			unmakeBlackNormalMoveOnBoard(move.getFrom(), move.getTo(), move.getMovedPiece(), move.getCapturedPiece());
		else if (moveType == MoveType.SHORT_CASTLING.ordinal())
			unmakeBlackShortCastlingMoveOnBoard();
		else if (moveType == MoveType.LONG_CASTLING.ordinal())
			unmakeBlackLongCastlingMoveOnBoard();
		else if (moveType == MoveType.EN_PASSANT.ordinal())
			unmakeBlackEnPassantMoveOnBoard(move.getFrom(), move.getTo());
		else if (moveType == MoveType.PROMOTION_TO_QUEEN.ordinal())
			unmakeBlackQueenPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
		else if (moveType == MoveType.PROMOTION_TO_ROOK.ordinal())
			unmakeBlackRookPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
		else if (moveType == MoveType.PROMOTION_TO_BISHOP.ordinal())
			unmakeBlackBishopPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
		else
			unmakeBlackKnightPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
	}
	/**
	 * Takes back the last move made and returns it. If no move has been made yet, it returns null.
	 *
	 * @return The move taken back or null if no move has been made yet.
	 */
	public Move unmakeMove() {
		PositionStateRecord prevState = stateHistory.poll();
		if (prevState == null) return null;
		Move move = moveHistory.pop();
		whitesTurn = !whitesTurn;
		if (!move.equals(Move.NULL_MOVE)) {
			if (whitesTurn)
				unmakeWhiteMoveOnBoard(move);
			else
				unmakeBlackMoveOnBoard(move);
		}
		whiteCastlingRights = prevState.getWhiteCastlingRights();
		blackCastlingRights = prevState.getBlackCastlingRights();
		enPassantRights = prevState.getEnPassantRights();
		fiftyMoveRuleClock = prevState.getFiftyMoveRuleClock();
		checkers = prevState.getCheckers();
		inCheck = checkers != Bitboard.EMPTY_BOARD;
		keyHistory[halfMoveIndex] = 0;
		key = keyHistory[--halfMoveIndex];
		return move;
	}
	private boolean isCheckedByWhite(int sqrInd) {
		MoveSetBase dB = MoveSetBase.values()[sqrInd];
		return ((whiteKnights & dB.getKnightMoveMask()) != Bitboard.EMPTY_BOARD ||
				(whitePawns & dB.getPawnBlackCaptureMoveMask()) != Bitboard.EMPTY_BOARD ||
				((whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied)) !=
						Bitboard.EMPTY_BOARD ||
				((whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied)) !=
						Bitboard.EMPTY_BOARD);
	}
	private boolean isCheckedByBlack(int sqrInd) {
		MoveSetBase dB = MoveSetBase.values()[sqrInd];
		return ((whiteKnights & dB.getKnightMoveMask()) != Bitboard.EMPTY_BOARD ||
				(whitePawns & dB.getPawnBlackCaptureMoveMask()) != Bitboard.EMPTY_BOARD ||
				((whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied)) !=
						Bitboard.EMPTY_BOARD ||
				((whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied)) !=
						Bitboard.EMPTY_BOARD);
	}
	private boolean givesWhiteCheck(Move move) {
		byte moveType = move.getType();
		boolean givesCheck;
		if (moveType == MoveType.NORMAL.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeBlackNormalMoveOnBitboards(fromBit, toBit, move.getMovedPiece(), move.getCapturedPiece());
			givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
			makeBlackNormalMoveOnBitboards(fromBit, toBit, move.getMovedPiece(), move.getCapturedPiece());
		} else if (moveType == MoveType.SHORT_CASTLING.ordinal()) {
			makeBlackShortCastlingMoveOnBitboards();
			givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
			makeBlackShortCastlingMoveOnBitboards();
		} else if (moveType == MoveType.LONG_CASTLING.ordinal()) {
			makeBlackLongCastlingMoveOnBitboards();
			givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
			makeBlackLongCastlingMoveOnBitboards();
		} else if (moveType == MoveType.EN_PASSANT.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeBlackEnPassantMoveOnBitboards(fromBit, toBit);
			givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
			makeBlackEnPassantMoveOnBitboards(fromBit, toBit);
		} else if (moveType == MoveType.PROMOTION_TO_QUEEN.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeBlackQueenPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
			makeBlackQueenPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_ROOK.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeBlackRookPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
			makeBlackRookPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_BISHOP.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeBlackBishopPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
			makeBlackBishopPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		} else {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeBlackKnightPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
			makeBlackKnightPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		}
		return givesCheck;
	}
	private boolean givesBlackCheck(Move move) {
		byte moveType = move.getType();
		boolean givesCheck;
		if (moveType == MoveType.NORMAL.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeWhiteNormalMoveOnBitboards(fromBit, toBit, move.getMovedPiece(), move.getCapturedPiece());
			givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
			makeWhiteNormalMoveOnBitboards(fromBit, toBit, move.getMovedPiece(), move.getCapturedPiece());
		} else if (moveType == MoveType.SHORT_CASTLING.ordinal()) {
			makeWhiteShortCastlingMoveOnBitboards();
			givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
			makeWhiteShortCastlingMoveOnBitboards();
		} else if (moveType == MoveType.LONG_CASTLING.ordinal()) {
			makeWhiteLongCastlingMoveOnBitboards();
			givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
			makeWhiteLongCastlingMoveOnBitboards();
		} else if (moveType == MoveType.EN_PASSANT.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeWhiteEnPassantMoveOnBitboards(fromBit, toBit);
			givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
			makeWhiteEnPassantMoveOnBitboards(fromBit, toBit);
		} else if (moveType == MoveType.PROMOTION_TO_QUEEN.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeWhiteQueenPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
			makeWhiteQueenPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_ROOK.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeWhiteRookPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
			makeWhiteRookPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_BISHOP.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeWhiteBishopPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
			makeWhiteBishopPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		} else {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeWhiteKnightPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
			makeWhiteKnightPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		}
		return givesCheck;
	}
	/**
	 * Checks whether the specified move puts the opponent in check. The move is assumed to be legal.
	 *
	 * @param move The move to check.
	 * @return Whether the move checks the opponent.
	 */
	public boolean givesCheck(Move move) {
		return whitesTurn ? givesBlackCheck(move) : givesWhiteCheck(move);
	}
	private boolean isAttackedByWhite(int sqrInd) {
		MoveSetBase dB = MoveSetBase.values()[sqrInd];
		return ((whiteKnights & dB.getKnightMoveMask()) != Bitboard.EMPTY_BOARD ||
				(whitePawns & dB.getPawnBlackCaptureMoveMask()) != Bitboard.EMPTY_BOARD ||
				((whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied)) !=
						Bitboard.EMPTY_BOARD ||
				((whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied)) !=
						Bitboard.EMPTY_BOARD ||
				(whiteKing & dB.getKingMoveMask()) != Bitboard.EMPTY_BOARD ||
				(squares[sqrInd] == Piece.B_PAWN.ordinal() && enPassantRights != EnPassantRights.NONE.ordinal() &&
						sqrInd == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights &&
						(whitePawns & dB.getKingMoveMask() & Bitboard.Rank.R5.getBitboard()) != Bitboard.EMPTY_BOARD));
	}
	private boolean isAttackedByBlack(int sqrInd) {
		MoveSetBase dB = MoveSetBase.values()[sqrInd];
		return ((blackKnights & dB.getKnightMoveMask()) != Bitboard.EMPTY_BOARD ||
				(blackPawns & dB.getPawnWhiteCaptureMoveMask()) != Bitboard.EMPTY_BOARD ||
				((blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied)) !=
						Bitboard.EMPTY_BOARD ||
				((blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied)) !=
						Bitboard.EMPTY_BOARD ||
				(blackKing & dB.getKingMoveMask()) != Bitboard.EMPTY_BOARD ||
				(squares[sqrInd] == Piece.W_PAWN.ordinal() && enPassantRights != EnPassantRights.NONE.ordinal() &&
						sqrInd == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights &&
						(blackPawns & dB.getKingMoveMask() & Bitboard.Rank.R4.getBitboard()) != Bitboard.EMPTY_BOARD));
	}
	private boolean leavesWhiteChecked(Move move) {
		byte moveType = move.getType();
		boolean leavesChecked;
		if (moveType == MoveType.NORMAL.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeWhiteNormalMoveOnBitboards(fromBit, toBit, move.getMovedPiece(), move.getCapturedPiece());
			leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
			makeWhiteNormalMoveOnBitboards(fromBit, toBit, move.getMovedPiece(), move.getCapturedPiece());
		} else if (moveType == MoveType.SHORT_CASTLING.ordinal()) {
			makeWhiteShortCastlingMoveOnBitboards();
			leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
			makeWhiteShortCastlingMoveOnBitboards();
		} else if (moveType == MoveType.LONG_CASTLING.ordinal()) {
			makeWhiteLongCastlingMoveOnBitboards();
			leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
			makeWhiteLongCastlingMoveOnBitboards();
		} else if (moveType == MoveType.EN_PASSANT.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeWhiteEnPassantMoveOnBitboards(fromBit, toBit);
			leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
			makeWhiteEnPassantMoveOnBitboards(fromBit, toBit);
		} else if (moveType == MoveType.PROMOTION_TO_QUEEN.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeWhiteQueenPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
			makeWhiteQueenPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_ROOK.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeWhiteRookPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
			makeWhiteRookPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_BISHOP.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeWhiteBishopPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
			makeWhiteBishopPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		} else {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeWhiteKnightPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
			makeWhiteKnightPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		}
		return leavesChecked;
	}
	private boolean leavesBlackChecked(Move move) {
		byte moveType = move.getType();
		boolean leavesChecked;
		if (moveType == MoveType.NORMAL.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeBlackNormalMoveOnBitboards(fromBit, toBit, move.getMovedPiece(), move.getCapturedPiece());
			leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
			makeBlackNormalMoveOnBitboards(fromBit, toBit, move.getMovedPiece(), move.getCapturedPiece());
		} else if (moveType == MoveType.SHORT_CASTLING.ordinal()) {
			makeBlackShortCastlingMoveOnBitboards();
			leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
			makeBlackShortCastlingMoveOnBitboards();
		} else if (moveType == MoveType.LONG_CASTLING.ordinal()) {
			makeBlackLongCastlingMoveOnBitboards();
			leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
			makeBlackLongCastlingMoveOnBitboards();
		} else if (moveType == MoveType.EN_PASSANT.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeBlackEnPassantMoveOnBitboards(fromBit, toBit);
			leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
			makeBlackEnPassantMoveOnBitboards(fromBit, toBit);
		} else if (moveType == MoveType.PROMOTION_TO_QUEEN.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeBlackQueenPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
			makeBlackQueenPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_ROOK.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeBlackRookPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
			makeBlackRookPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_BISHOP.ordinal()) {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeBlackBishopPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
			makeBlackBishopPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		} else {
			long fromBit = BitOperations.toBit(move.getFrom());
			long toBit = BitOperations.toBit(move.getTo());
			makeBlackKnightPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
			leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
			makeBlackKnightPromotionMoveOnBitboards(fromBit, toBit, move.getCapturedPiece());
		}
		return leavesChecked;
	}
	private boolean isLegalForWhite(Move move) {
		long moveSet;
		byte movedPiece = move.getMovedPiece();
		if (squares[move.getFrom()] != movedPiece)
			return false;
		long toBit = BitOperations.toBit(move.getTo());
		if (movedPiece == Piece.W_PAWN.ordinal()) {
			moveSet = Bitboard.EMPTY_BOARD;
			if (move.getType() == MoveType.EN_PASSANT.ordinal() && enPassantRights != EnPassantRights.NONE.ordinal() &&
					move.getTo() == EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)
				moveSet |= toBit;
			else if (squares[move.getTo()] != move.getCapturedPiece())
				return false;
			moveSet |= MoveSetBase.values()[move.getFrom()].getWhitePawnMoveSet(allBlackOccupied, allEmpty);
		} else {
			if (squares[move.getTo()] != move.getCapturedPiece())
				return false;
			MoveSetBase dB = MoveSetBase.values()[move.getFrom()];
			if (movedPiece == Piece.W_KING.ordinal()) {
				if (move.getType() == MoveType.SHORT_CASTLING.ordinal()) {
					return !inCheck && (whiteCastlingRights == CastlingRights.SHORT.ordinal() ||
							whiteCastlingRights == CastlingRights.ALL.ordinal()) &&
							((Bitboard.Square.F1.getBitboard() | Bitboard.Square.G1.getBitboard()) &
									allOccupied) == Bitboard.EMPTY_BOARD &&
							squares[Bitboard.Square.H1.ordinal()] == Piece.W_ROOK.ordinal() &&
							!isAttackedByBlack(Bitboard.Square.F1.ordinal()) &&
							!isAttackedByBlack(Bitboard.Square.G1.ordinal());
				} else if (move.getType() == MoveType.LONG_CASTLING.ordinal()) {
					return !inCheck && (whiteCastlingRights == CastlingRights.LONG.ordinal() ||
							whiteCastlingRights == CastlingRights.ALL.ordinal()) &&
							((Bitboard.Square.B1.getBitboard() | Bitboard.Square.C1.getBitboard() |
									Bitboard.Square.D1.getBitboard()) & allOccupied) == Bitboard.EMPTY_BOARD &&
							squares[Bitboard.Square.A1.ordinal()] == Piece.W_ROOK.ordinal() &&
							!isAttackedByBlack(Bitboard.Square.C1.ordinal()) &&
							!isAttackedByBlack(Bitboard.Square.D1.ordinal());
				} else
					moveSet = dB.getKingMoveSet(allNonWhiteOccupied);
			} else if (movedPiece == Piece.W_QUEEN.ordinal())
				moveSet = dB.getQueenMoveSet(allNonWhiteOccupied, allOccupied);
			else if (movedPiece == Piece.W_ROOK.ordinal())
				moveSet = dB.getRookMoveSet(allNonWhiteOccupied, allOccupied);
			else if (movedPiece == Piece.W_BISHOP.ordinal())
				moveSet = dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied);
			else if (movedPiece == Piece.W_KNIGHT.ordinal())
				moveSet = dB.getKnightMoveSet(allNonWhiteOccupied);
			else
				return false;
		}
		return (moveSet & toBit) != Bitboard.EMPTY_BOARD && !leavesWhiteChecked(move);
	}
	private boolean isLegalForBlack(Move move) {
		long moveSet;
		byte movedPiece = move.getMovedPiece();
		if (squares[move.getFrom()] != movedPiece)
			return false;
		long toBit = BitOperations.toBit(move.getTo());
		if (movedPiece == Piece.B_PAWN.ordinal()) {
			moveSet = Bitboard.EMPTY_BOARD;
			if (move.getType() == MoveType.EN_PASSANT.ordinal() && enPassantRights != EnPassantRights.NONE.ordinal() &&
					move.getTo() == EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)
				moveSet |= toBit;
			else if (squares[move.getTo()] != move.getCapturedPiece())
				return false;
			moveSet |= MoveSetBase.values()[move.getFrom()].getBlackPawnMoveSet(allWhiteOccupied, allEmpty);
		} else {
			if (squares[move.getTo()] != move.getCapturedPiece())
				return false;
			MoveSetBase dB = MoveSetBase.values()[move.getFrom()];
			if (movedPiece == Piece.B_KING.ordinal()) {
				if (move.getType() == MoveType.SHORT_CASTLING.ordinal()) {
					return !inCheck && (blackCastlingRights == CastlingRights.SHORT.ordinal() ||
							blackCastlingRights == CastlingRights.ALL.ordinal()) &&
							((Bitboard.Square.F8.getBitboard() | Bitboard.Square.G8.getBitboard()) &
									allOccupied) == Bitboard.EMPTY_BOARD &&
							squares[Bitboard.Square.H8.ordinal()] == Piece.B_ROOK.ordinal() &&
							!isAttackedByWhite(Bitboard.Square.F8.ordinal()) &&
							!isAttackedByWhite(Bitboard.Square.G8.ordinal());
				} else if (move.getType() == MoveType.LONG_CASTLING.ordinal()) {
					return !inCheck && (blackCastlingRights == CastlingRights.LONG.ordinal() ||
							blackCastlingRights == CastlingRights.ALL.ordinal()) &&
							((Bitboard.Square.B8.getBitboard() | Bitboard.Square.C8.getBitboard() |
									Bitboard.Square.D8.getBitboard()) & allOccupied) == Bitboard.EMPTY_BOARD &&
							squares[Bitboard.Square.A8.ordinal()] == Piece.B_ROOK.ordinal() &&
							!isAttackedByWhite(Bitboard.Square.C8.ordinal()) &&
							!isAttackedByWhite(Bitboard.Square.D8.ordinal());
				} else
					moveSet = dB.getKingMoveSet(allNonBlackOccupied);
			} else if (movedPiece == Piece.B_QUEEN.ordinal())
				moveSet = dB.getQueenMoveSet(allNonBlackOccupied, allOccupied);
			else if (movedPiece == Piece.B_ROOK.ordinal())
				moveSet = dB.getRookMoveSet(allNonBlackOccupied, allOccupied);
			else if (movedPiece == Piece.B_BISHOP.ordinal())
				moveSet = dB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
			else if (movedPiece == Piece.B_KNIGHT.ordinal())
				moveSet = dB.getKnightMoveSet(allNonBlackOccupied);
			else
				return false;
		}
		return (moveSet & toBit) != Bitboard.EMPTY_BOARD && !leavesBlackChecked(move);
	}
	/**
	 * Determines whether the move is legal in the current position. It assumes that there exists a position in which
	 * the move is legal and checks if this one is one of them. This method may fail to detect inconsistencies in the
	 * move attributes. A fail safe way of legality checking is generating all the moves for the current position
	 * and see if the move is among them.
	 *
	 * @param move The move to perform legality check on.
	 * @return Whether the move is legal or not.
	 */
	public boolean isLegal(Move move) {
		return whitesTurn ? isLegalForWhite(move) : isLegalForBlack(move);
	}
	private long addTacticalStraightPinnedPieceMoveAndGetPinnedPiece(long pinnedPiece, long pinningPiece,
			byte queenType, byte rookType, List<Move> moves) {
		if (pinningPiece != Bitboard.EMPTY_BOARD) {
			byte from  = BitOperations.indexOfBit(pinnedPiece);
			byte pinnedPieceType = squares[from];
			if (pinnedPieceType == queenType || pinnedPieceType == rookType) {
				byte to = BitOperations.indexOfBit(pinningPiece);
				moves.add(new Move(from, to, pinnedPieceType, squares[to], (byte) MoveType.NORMAL.ordinal()));
			}
			return pinnedPiece;
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addTacticalPositiveStraightPinnedPieceMoveAndGetPinnedPiece(long rayOccupancy, long allSameColorOccupied,
			long sliders, byte queenType, byte rookType, List<Move> moves) {
		long pinnedPiece = BitOperations.getLSBit(rayOccupancy) & allSameColorOccupied;
		if (pinnedPiece != Bitboard.EMPTY_BOARD) {
			return addTacticalStraightPinnedPieceMoveAndGetPinnedPiece(pinnedPiece,
					BitOperations.getLSBit(rayOccupancy^pinnedPiece) & sliders, queenType, rookType, moves);
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addTacticalNegativeStraightPinnedPieceMoveAndGetPinnedPiece(long rayOccupancy, long allSameColorOccupied,
			long sliders, byte queenType, byte rookType, List<Move> moves) {
		long pinnedPiece = BitOperations.getMSBit(rayOccupancy) & allSameColorOccupied;
		if (pinnedPiece != Bitboard.EMPTY_BOARD) {
			return addTacticalStraightPinnedPieceMoveAndGetPinnedPiece(pinnedPiece,
					BitOperations.getMSBit(rayOccupancy^pinnedPiece) & sliders, queenType, rookType, moves);
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addWhiteTacticalPositiveDiagonalPinnedPieceMoveAndGetPinnedPiece(long ray, long sliders,
			List<Move> moves) {
		long rayOccupancy = ray & allOccupied;
		long pinnedPiece = BitOperations.getLSBit(rayOccupancy) & allWhiteOccupied;
		if (pinnedPiece != Bitboard.EMPTY_BOARD) {
			long pinningPiece = BitOperations.getLSBit(rayOccupancy^pinnedPiece) & sliders;
			if (pinningPiece != Bitboard.EMPTY_BOARD) {
				byte from  = BitOperations.indexOfBit(pinnedPiece);
				byte pinnedPieceType = squares[from];
				if (pinnedPieceType == Piece.W_QUEEN.ordinal() || pinnedPieceType == Piece.W_BISHOP.ordinal()) {
					byte to = BitOperations.indexOfBit(pinningPiece);
					moves.add(new Move(from, to, pinnedPieceType, squares[to], (byte) MoveType.NORMAL.ordinal()));
				} else if (pinnedPieceType == Piece.W_PAWN.ordinal()) {
					byte to = BitOperations.indexOfBit(pinningPiece);
					if ((pinnedPiece & Bitboard.Rank.R7.getBitboard()) != Bitboard.EMPTY_BOARD)
						addPromotionMoves(from, to, pinnedPieceType, squares[to], moves);
					else if (Bitboard.computeWhitePawnCaptureSets(pinnedPiece, pinningPiece) != Bitboard.EMPTY_BOARD)
						moves.add(new Move(from, to, pinnedPieceType, squares[to], (byte) MoveType.NORMAL.ordinal()));
					else if (enPassantRights != EnPassantRights.NONE.ordinal() &&
							Bitboard.computeWhitePawnCaptureSets(pinnedPiece,
									BitOperations.toBit(EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights) & ray) !=
									Bitboard.EMPTY_BOARD) {
						moves.add(new Move(from, to, pinnedPieceType, (byte) Piece.B_PAWN.ordinal(),
								(byte) MoveType.EN_PASSANT.ordinal()));
					}
				}
				return pinnedPiece;
			}
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addBlackTacticalPositiveDiagonalPinnedPieceMoveAndGetPinnedPiece(long ray, long sliders,
			List<Move> moves) {
		long rayOccupancy = ray & allOccupied;
		long pinnedPiece = BitOperations.getLSBit(rayOccupancy) & allBlackOccupied;
		if (pinnedPiece != Bitboard.EMPTY_BOARD) {
			long pinningPiece = BitOperations.getLSBit(rayOccupancy^pinnedPiece) & sliders;
			if (pinningPiece != Bitboard.EMPTY_BOARD) {
				byte from  = BitOperations.indexOfBit(pinnedPiece);
				byte pinnedPieceType = squares[from];
				if (pinnedPieceType == Piece.B_QUEEN.ordinal() || pinnedPieceType == Piece.B_BISHOP.ordinal()) {
					byte to = BitOperations.indexOfBit(pinningPiece);
					moves.add(new Move(from, to, pinnedPieceType, squares[to], (byte) MoveType.NORMAL.ordinal()));
				} else if (pinnedPieceType == Piece.B_PAWN.ordinal() &&
						enPassantRights != EnPassantRights.NONE.ordinal() &&
						Bitboard.computeBlackPawnCaptureSets(pinnedPiece, BitOperations.toBit(
								EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights) & ray) != Bitboard.EMPTY_BOARD) {
					moves.add(new Move(from, BitOperations.indexOfBit(pinningPiece), pinnedPieceType,
							(byte) Piece.W_PAWN.ordinal(), (byte) MoveType.EN_PASSANT.ordinal()));
				}
				return pinnedPiece;
			}
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addWhiteTacticalNegativeDiagonalPinnedPieceMoveAndGetPinnedPiece(long ray, long sliders,
			 List<Move> moves) {
		long rayOccupancy = ray & allOccupied;
		long pinnedPiece = BitOperations.getMSBit(rayOccupancy) & allWhiteOccupied;
		if (pinnedPiece != Bitboard.EMPTY_BOARD) {
			long pinningPiece = BitOperations.getMSBit(rayOccupancy^pinnedPiece) & sliders;
			if (pinningPiece != Bitboard.EMPTY_BOARD) {
				byte from  = BitOperations.indexOfBit(pinnedPiece);
				byte pinnedPieceType = squares[from];
				if (pinnedPieceType == Piece.W_QUEEN.ordinal() || pinnedPieceType == Piece.W_BISHOP.ordinal()) {
					byte to = BitOperations.indexOfBit(pinningPiece);
					moves.add(new Move(from, to, pinnedPieceType, squares[to], (byte) MoveType.NORMAL.ordinal()));
				} else if (pinnedPieceType == Piece.W_PAWN.ordinal() &&
						enPassantRights != EnPassantRights.NONE.ordinal() &&
						Bitboard.computeWhitePawnCaptureSets(pinnedPiece, BitOperations.toBit(
								EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights) & ray) != Bitboard.EMPTY_BOARD) {
					moves.add(new Move(from, BitOperations.indexOfBit(pinningPiece), pinnedPieceType,
							(byte) Piece.B_PAWN.ordinal(), (byte) MoveType.EN_PASSANT.ordinal()));
				}
				return pinnedPiece;
			}
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addBlackTacticalNegativeDiagonalPinnedPieceMoveAndGetPinnedPiece(long ray, long sliders,
			List<Move> moves) {
		long rayOccupancy = ray & allOccupied;
		long pinnedPiece = BitOperations.getMSBit(rayOccupancy) & allBlackOccupied;
		if (pinnedPiece != Bitboard.EMPTY_BOARD) {
			long pinningPiece = BitOperations.getMSBit(rayOccupancy^pinnedPiece) & sliders;
			if (pinningPiece != Bitboard.EMPTY_BOARD) {
				byte from  = BitOperations.indexOfBit(pinnedPiece);
				byte pinnedPieceType = squares[from];
				if (pinnedPieceType == Piece.B_QUEEN.ordinal() || pinnedPieceType == Piece.B_BISHOP.ordinal()) {
					byte to = BitOperations.indexOfBit(pinningPiece);
					moves.add(new Move(from, to, pinnedPieceType, squares[to], (byte) MoveType.NORMAL.ordinal()));
				} else if (pinnedPieceType == Piece.B_PAWN.ordinal()) {
					byte to = BitOperations.indexOfBit(pinningPiece);
					if ((pinnedPiece & Bitboard.Rank.R2.getBitboard()) != Bitboard.EMPTY_BOARD)
						addPromotionMoves(from, to, pinnedPieceType, squares[to], moves);
					else if (Bitboard.computeBlackPawnCaptureSets(pinnedPiece, pinningPiece) != Bitboard.EMPTY_BOARD)
						moves.add(new Move(from, to, pinnedPieceType, squares[to], (byte) MoveType.NORMAL.ordinal()));
					else if (enPassantRights != EnPassantRights.NONE.ordinal() &&
							Bitboard.computeBlackPawnCaptureSets(pinnedPiece,
									BitOperations.toBit(EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights) & ray) !=
									Bitboard.EMPTY_BOARD) {
						moves.add(new Move(from, to, pinnedPieceType, (byte) Piece.W_PAWN.ordinal(),
								(byte) MoveType.EN_PASSANT.ordinal()));
					}
				}
				return pinnedPiece;
			}
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addWhiteTacticalPinnedPieceMovesAndGetPinnedPieces(byte kingInd, List<Move> moves) {
		Bitboard.Rays rays = Bitboard.Rays.values()[kingInd];
		long straightSliders = blackQueens | blackRooks;
		long diagonalSliders = blackQueens | blackBishops;
		long pinnedPieces = addTacticalPositiveStraightPinnedPieceMoveAndGetPinnedPiece(rays.getRankPos() & allOccupied,
				allWhiteOccupied, straightSliders, (byte) Piece.W_QUEEN.ordinal(), (byte) Piece.W_ROOK.ordinal(), moves);
		pinnedPieces |= addTacticalPositiveStraightPinnedPieceMoveAndGetPinnedPiece(rays.getFilePos() & allOccupied,
				allWhiteOccupied, straightSliders, (byte) Piece.W_QUEEN.ordinal(), (byte) Piece.W_ROOK.ordinal(), moves);
		pinnedPieces |= addWhiteTacticalPositiveDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.getDiagonalPos(),
				diagonalSliders, moves);
		pinnedPieces |= addWhiteTacticalPositiveDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.getAntiDiagonalPos(),
				diagonalSliders, moves);
		pinnedPieces |= addTacticalNegativeStraightPinnedPieceMoveAndGetPinnedPiece(rays.getRankNeg() & allOccupied,
				allWhiteOccupied, straightSliders, (byte) Piece.W_QUEEN.ordinal(), (byte) Piece.W_ROOK.ordinal(), moves);
		pinnedPieces |= addTacticalNegativeStraightPinnedPieceMoveAndGetPinnedPiece(rays.getFileNeg() & allOccupied,
				allWhiteOccupied, straightSliders, (byte) Piece.W_QUEEN.ordinal(), (byte) Piece.W_ROOK.ordinal(), moves);
		pinnedPieces |= addWhiteTacticalNegativeDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.getDiagonalNeg(),
				diagonalSliders, moves);
		pinnedPieces |= addWhiteTacticalNegativeDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.getAntiDiagonalNeg(),
				diagonalSliders, moves);
		return pinnedPieces;
	}
	private long addBlackTacticalPinnedPieceMovesAndGetPinnedPieces(byte kingInd, List<Move> moves) {
		Bitboard.Rays rays = Bitboard.Rays.values()[kingInd];
		long straightSliders = whiteQueens | whiteRooks;
		long diagonalSliders = whiteQueens | whiteBishops;
		long pinnedPieces = addTacticalPositiveStraightPinnedPieceMoveAndGetPinnedPiece(rays.getRankPos() & allOccupied,
				allBlackOccupied, straightSliders, (byte) Piece.B_QUEEN.ordinal(), (byte) Piece.B_ROOK.ordinal(), moves);
		pinnedPieces |= addTacticalPositiveStraightPinnedPieceMoveAndGetPinnedPiece(rays.getFilePos() & allOccupied,
				allBlackOccupied, straightSliders, (byte) Piece.B_QUEEN.ordinal(), (byte) Piece.B_ROOK.ordinal(), moves);
		pinnedPieces |= addBlackTacticalPositiveDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.getDiagonalPos(),
				diagonalSliders, moves);
		pinnedPieces |= addBlackTacticalPositiveDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.getAntiDiagonalPos(),
				diagonalSliders, moves);
		pinnedPieces |= addTacticalNegativeStraightPinnedPieceMoveAndGetPinnedPiece(rays.getRankNeg() & allOccupied,
				allBlackOccupied, straightSliders, (byte) Piece.B_QUEEN.ordinal(), (byte) Piece.B_ROOK.ordinal(), moves);
		pinnedPieces |= addTacticalNegativeStraightPinnedPieceMoveAndGetPinnedPiece(rays.getFileNeg() & allOccupied,
				allBlackOccupied, straightSliders, (byte) Piece.B_QUEEN.ordinal(), (byte) Piece.B_ROOK.ordinal(), moves);
		pinnedPieces |= addBlackTacticalNegativeDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.getDiagonalNeg(),
				diagonalSliders, moves);
		pinnedPieces |= addBlackTacticalNegativeDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.getAntiDiagonalNeg(),
				diagonalSliders, moves);
		return pinnedPieces;
	}
	private void addWhiteKingNormalMoves(byte from, long targets, List<Move> moves) {
		long moveSet = MoveSetBase.values()[from].getKingMoveSet(targets);
		while (moveSet != Bitboard.EMPTY_BOARD) {
			byte to = BitOperations.indexOfLSBit(moveSet);
			if (!isAttackedByBlack(to)) {
				moves.add(new Move(from, to, (byte) Piece.W_KING.ordinal(), squares[to],
						(byte) MoveType.NORMAL.ordinal()));
			}
			moveSet = BitOperations.resetLSBit(moveSet);
		}
	}
	private void addBlackKingNormalMoves(byte from, long targets, List<Move> moves) {
		long moveSet = MoveSetBase.values()[from].getKingMoveSet(targets);
		while (moveSet != Bitboard.EMPTY_BOARD) {
			byte to = BitOperations.indexOfLSBit(moveSet);
			if (!isAttackedByWhite(to)) {
				moves.add(new Move(from, to, (byte) Piece.B_KING.ordinal(), squares[to],
						(byte) MoveType.NORMAL.ordinal()));
			}
			moveSet = BitOperations.resetLSBit(moveSet);
		}
	}
	private void addWhiteKingCastlingMoves(byte from, List<Move> moves) {
		if ((whiteCastlingRights == CastlingRights.LONG.ordinal() ||
				whiteCastlingRights == CastlingRights.ALL.ordinal()) &&
				((Bitboard.Square.B1.getBitboard() | Bitboard.Square.C1.getBitboard() |
						Bitboard.Square.D1.getBitboard()) & allOccupied) == Bitboard.EMPTY_BOARD &&
				!isAttackedByBlack(Bitboard.Square.D1.ordinal()) &&
				!isAttackedByBlack(Bitboard.Square.C1.ordinal())) {
			moves.add(new Move(from, (byte) Bitboard.Square.C1.ordinal(), (byte) Piece.W_KING.ordinal(),
					(byte) Piece.NULL.ordinal(), (byte) MoveType.LONG_CASTLING.ordinal()));
		}
		if ((whiteCastlingRights == CastlingRights.SHORT.ordinal() ||
				whiteCastlingRights == CastlingRights.ALL.ordinal()) &&
				((Bitboard.Square.F1.getBitboard() | Bitboard.Square.G1.getBitboard()) &
						allOccupied) == Bitboard.EMPTY_BOARD &&
				!isAttackedByBlack(Bitboard.Square.F1.ordinal()) &&
				!isAttackedByBlack(Bitboard.Square.G1.ordinal())) {
			moves.add(new Move(from, (byte) Bitboard.Square.G1.ordinal(), (byte) Piece.W_KING.ordinal(),
					(byte) Piece.NULL.ordinal(), (byte) MoveType.SHORT_CASTLING.ordinal()));
		}
	}
	private void addBlackKingCastlingMoves(byte from, List<Move> moves) {
		if ((blackCastlingRights == CastlingRights.LONG.ordinal() ||
				blackCastlingRights == CastlingRights.ALL.ordinal()) &&
				((Bitboard.Square.B8.getBitboard() | Bitboard.Square.C8.getBitboard() |
						Bitboard.Square.D8.getBitboard()) & allOccupied) == Bitboard.EMPTY_BOARD &&
				!isAttackedByWhite(Bitboard.Square.D8.ordinal()) &&
				!isAttackedByWhite(Bitboard.Square.C8.ordinal())) {
			moves.add(new Move(from, (byte) Bitboard.Square.C8.ordinal(), (byte) Piece.B_KING.ordinal(),
					(byte) Piece.NULL.ordinal(), (byte) MoveType.LONG_CASTLING.ordinal()));
		}
		if ((blackCastlingRights == CastlingRights.SHORT.ordinal() ||
				blackCastlingRights == CastlingRights.ALL.ordinal()) &&
				((Bitboard.Square.F8.getBitboard() | Bitboard.Square.G8.getBitboard()) &
						allOccupied) == Bitboard.EMPTY_BOARD &&
				!isAttackedByWhite(Bitboard.Square.F8.ordinal()) &&
				!isAttackedByWhite(Bitboard.Square.G8.ordinal())) {
			moves.add(new Move(from, (byte) Bitboard.Square.G8.ordinal(), (byte) Piece.B_KING.ordinal(),
					(byte) Piece.NULL.ordinal(), (byte) MoveType.SHORT_CASTLING.ordinal()));
		}
	}
	private void addNormalMovesFromOrigin(byte from, byte movedPiece, long moveSet, List<Move> moves) {
		while (moveSet != Bitboard.EMPTY_BOARD) {
			byte to = BitOperations.indexOfLSBit(moveSet);
			moves.add(new Move(from, to, movedPiece, squares[to], (byte) MoveType.NORMAL.ordinal()));
			moveSet = BitOperations.resetLSBit(moveSet);
		}
	}
	private void addQueenMoves(byte pieceType, long pieces, long targets, List<Move> moves) {
		while (pieces != Bitboard.EMPTY_BOARD) {
			byte from = BitOperations.indexOfLSBit(pieces);
			addNormalMovesFromOrigin(from, pieceType, MoveSetBase.values()[from].getQueenMoveSet(targets, allOccupied),
					moves);
			pieces = BitOperations.resetLSBit(pieces);
		}
	}
	private void addRookMoves(byte pieceType, long pieces, long targets, List<Move> moves) {
		while (pieces != Bitboard.EMPTY_BOARD) {
			byte from = BitOperations.indexOfLSBit(pieces);
			addNormalMovesFromOrigin(from, pieceType, MoveSetBase.values()[from].getRookMoveSet(targets, allOccupied),
					moves);
			pieces = BitOperations.resetLSBit(pieces);
		}
	}
	private void addBishopMoves(byte pieceType, long pieces, long targets, List<Move> moves) {
		while (pieces != Bitboard.EMPTY_BOARD) {
			byte from = BitOperations.indexOfLSBit(pieces);
			addNormalMovesFromOrigin(from, pieceType, MoveSetBase.values()[from].getBishopMoveSet(targets, allOccupied),
					moves);
			pieces = BitOperations.resetLSBit(pieces);
		}
	}
	private void addKnightMoves(byte pieceType, long pieces, long targets, List<Move> moves) {
		while (pieces != Bitboard.EMPTY_BOARD) {
			byte from = BitOperations.indexOfLSBit(pieces);
			addNormalMovesFromOrigin(from, pieceType, MoveSetBase.values()[from].getKnightMoveSet(targets), moves);
			pieces = BitOperations.resetLSBit(pieces);
		}
	}
	private void addWhitePawnNormalMoves(long movablePieces, long oppTargets, long emptyTargets, List<Move> moves) {
		long pieces = whitePawns & movablePieces & ~Bitboard.Rank.R7.getBitboard();
		while (pieces != Bitboard.EMPTY_BOARD) {
			byte from = BitOperations.indexOfLSBit(pieces);
			addNormalMovesFromOrigin(from, (byte) Piece.W_PAWN.ordinal(),
					MoveSetBase.values()[from].getWhitePawnMoveSet(oppTargets, emptyTargets), moves);
			pieces = BitOperations.resetLSBit(pieces);
		}
	}
	private void addBlackPawnNormalMoves(long movablePieces, long oppTargets, long emptyTargets, List<Move> moves) {
		long pieces = blackPawns & movablePieces & ~Bitboard.Rank.R2.getBitboard();
		while (pieces != Bitboard.EMPTY_BOARD) {
			byte from = BitOperations.indexOfLSBit(pieces);
			addNormalMovesFromOrigin(from, (byte) Piece.B_PAWN.ordinal(),
					MoveSetBase.values()[from].getBlackPawnMoveSet(oppTargets, emptyTargets), moves);
			pieces = BitOperations.resetLSBit(pieces);
		}
	}
	private void addWhitePawnEnPassantMoves(long movablePieces, byte whiteKingInd, List<Move> moves) {
		if (enPassantRights != EnPassantRights.NONE.ordinal() && movablePieces != Bitboard.EMPTY_BOARD) {
			byte to = (byte) (EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights);
			long pieces = MoveSetBase.values()[to].getBlackPawnCaptureSet(whitePawns) & movablePieces;
			if (pieces == Bitboard.EMPTY_BOARD)
				return;
			MoveSetBase kingDb = MoveSetBase.values()[whiteKingInd];
			do {
				byte from = BitOperations.indexOfLSBit(pieces);
				// Make sure that the en passant does not leave the king exposed to check.
				long changedWhiteBits = ((BitOperations.toBit(from)) | (BitOperations.toBit(to)));
				long allNonWhiteOccupiedTemp = allNonWhiteOccupied^changedWhiteBits;
				long allOccupiedTemp = allOccupied^(changedWhiteBits | BitOperations.toBit(to - 8));
				if (((blackQueens | blackRooks) & kingDb.getRookMoveSet(allNonWhiteOccupiedTemp,
						allOccupiedTemp)) == Bitboard.EMPTY_BOARD && ((blackQueens | blackBishops) &
						kingDb.getBishopMoveSet(allNonWhiteOccupiedTemp, allOccupiedTemp)) == Bitboard.EMPTY_BOARD) {
					moves.add(new Move(from, to, (byte) Piece.W_PAWN.ordinal(), (byte) Piece.B_PAWN.ordinal(),
							(byte) MoveType.EN_PASSANT.ordinal()));
				}
				pieces = BitOperations.resetLSBit(pieces);
			} while (pieces != Bitboard.EMPTY_BOARD);
		}
	}
	private void addBlackPawnEnPassantMoves(long movablePieces, byte blackKingInd, List<Move> moves) {
		if (enPassantRights != EnPassantRights.NONE.ordinal() && movablePieces != Bitboard.EMPTY_BOARD) {
			byte to = (byte) (EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights);
			long pieces = MoveSetBase.values()[to].getWhitePawnCaptureSet(blackPawns) & movablePieces;
			if (pieces == Bitboard.EMPTY_BOARD)
				return;
			MoveSetBase kingDb = MoveSetBase.values()[blackKingInd];
			do {
				byte from = BitOperations.indexOfLSBit(pieces);
				long changedBlackBits = ((BitOperations.toBit(from)) | (BitOperations.toBit(to)));
				long allNonBlackOccupiedTemp = allNonWhiteOccupied^changedBlackBits;
				long allOccupiedTemp = allOccupied^(changedBlackBits | BitOperations.toBit(to + 8));
				if (((whiteQueens | whiteRooks) & kingDb.getRookMoveSet(allNonBlackOccupiedTemp,
						allOccupiedTemp)) == Bitboard.EMPTY_BOARD && ((whiteQueens | whiteBishops) &
						kingDb.getBishopMoveSet(allNonBlackOccupiedTemp, allOccupiedTemp)) == Bitboard.EMPTY_BOARD) {
					moves.add(new Move(from, to, (byte) Piece.B_PAWN.ordinal(), (byte) Piece.W_PAWN.ordinal(),
							(byte) MoveType.EN_PASSANT.ordinal()));
				}
				pieces = BitOperations.resetLSBit(pieces);
			} while (pieces != Bitboard.EMPTY_BOARD);
		}
	}
	private static void addPromotionMoves(byte from, byte to, byte movedPiece, byte capturedPiece, List<Move> moves) {
		moves.add(new Move(from, to, movedPiece, capturedPiece, (byte) MoveType.PROMOTION_TO_QUEEN.ordinal()));
		moves.add(new Move(from, to, movedPiece, capturedPiece, (byte) MoveType.PROMOTION_TO_ROOK.ordinal()));
		moves.add(new Move(from, to, movedPiece, capturedPiece, (byte) MoveType.PROMOTION_TO_BISHOP.ordinal()));
		moves.add(new Move(from, to, movedPiece, capturedPiece, (byte) MoveType.PROMOTION_TO_KNIGHT.ordinal()));
	}
	private void addWhitePawnPromotionMoves(long movablePieces, long targets, List<Move> moves) {
		long pieces = whitePawns & movablePieces & Bitboard.Rank.R7.getBitboard();
		while (pieces != Bitboard.EMPTY_BOARD) {
			byte from = BitOperations.indexOfLSBit(pieces);
			long moveSet = MoveSetBase.values()[from].getWhitePawnMoveSet(allBlackOccupied, allEmpty) & targets;
			while (moveSet != Bitboard.EMPTY_BOARD) {
				byte to = BitOperations.indexOfLSBit(moveSet);
				addPromotionMoves(from, to, (byte) Piece.W_PAWN.ordinal(), squares[to], moves);
				moveSet = BitOperations.resetLSBit(moveSet);
			}
			pieces = BitOperations.resetLSBit(pieces);
		}
	}
	private void addBlackPawnPromotionMoves(long movablePieces, long targets, List<Move> moves) {
		long pieces = blackPawns & movablePieces & Bitboard.Rank.R2.getBitboard();
		while (pieces != Bitboard.EMPTY_BOARD) {
			byte from = BitOperations.indexOfLSBit(pieces);
			long moveSet = MoveSetBase.values()[from].getBlackPawnMoveSet(allWhiteOccupied, allEmpty) & targets;
			while (moveSet != Bitboard.EMPTY_BOARD) {
				byte to = BitOperations.indexOfLSBit(moveSet);
				addPromotionMoves(from, to, (byte) Piece.B_PAWN.ordinal(), squares[to], moves);
				moveSet = BitOperations.resetLSBit(moveSet);
			}
			pieces = BitOperations.resetLSBit(pieces);
		}
	}
	private void addWhiteTacticalMoves(List<Move> moves) {
		byte kingInd = BitOperations.indexOfBit(whiteKing);
		long movablePieces = ~addWhiteTacticalPinnedPieceMovesAndGetPinnedPieces(kingInd, moves);
		addWhitePawnPromotionMoves(movablePieces, Bitboard.FULL_BOARD, moves);
		addWhitePawnEnPassantMoves(movablePieces, kingInd, moves);
		addWhitePawnNormalMoves(movablePieces, allBlackOccupied, Bitboard.EMPTY_BOARD, moves);
		addKnightMoves((byte) Piece.W_KNIGHT.ordinal(), whiteKnights & movablePieces, allBlackOccupied, moves);
		addBishopMoves((byte) Piece.W_BISHOP.ordinal(), whiteBishops & movablePieces, allBlackOccupied, moves);
		addRookMoves((byte) Piece.W_ROOK.ordinal(), whiteRooks & movablePieces, allBlackOccupied, moves);
		addQueenMoves((byte) Piece.W_QUEEN.ordinal(), whiteQueens & movablePieces, allBlackOccupied, moves);
		addWhiteKingNormalMoves(kingInd, allBlackOccupied, moves);
	}
	private void addBlackTacticalMoves(List<Move> moves) {
		byte kingInd = BitOperations.indexOfBit(blackKing);
		long movablePieces = ~addBlackTacticalPinnedPieceMovesAndGetPinnedPieces(kingInd, moves);
		addBlackPawnPromotionMoves(movablePieces, Bitboard.FULL_BOARD, moves);
		addBlackPawnEnPassantMoves(movablePieces, kingInd, moves);
		addBlackPawnNormalMoves(movablePieces, allWhiteOccupied, Bitboard.EMPTY_BOARD, moves);
		addKnightMoves((byte) Piece.B_KNIGHT.ordinal(), blackKnights & movablePieces, allWhiteOccupied, moves);
		addBishopMoves((byte) Piece.B_BISHOP.ordinal(), blackBishops & movablePieces, allWhiteOccupied, moves);
		addRookMoves((byte) Piece.B_ROOK.ordinal(), blackRooks & movablePieces, allWhiteOccupied, moves);
		addQueenMoves((byte) Piece.B_QUEEN.ordinal(), blackQueens & movablePieces, allWhiteOccupied, moves);
		addBlackKingNormalMoves(kingInd, allWhiteOccupied, moves);
	}
	private static long getPositivePinnedPiece(long ray, long sliders, long allSameColorOccupied) {
		long pinnedPiece = BitOperations.getLSBit(ray) & allSameColorOccupied;
		return (pinnedPiece != Bitboard.EMPTY_BOARD &&
				(BitOperations.getLSBit(ray^pinnedPiece) & sliders) != Bitboard.EMPTY_BOARD) ?
				pinnedPiece : Bitboard.EMPTY_BOARD;
	}
	private static long getNegativePinnedPiece(long ray, long sliders, long allSameColorOccupied) {
		long pinnedPiece = BitOperations.getMSBit(ray) & allSameColorOccupied;
		return (pinnedPiece != Bitboard.EMPTY_BOARD &&
				(BitOperations.getMSBit(ray^pinnedPiece) & sliders) != Bitboard.EMPTY_BOARD) ?
		pinnedPiece : Bitboard.EMPTY_BOARD;
	}
	private static long getPinnedPieces(byte kingInd, long straightSliders, long diagonalSliders, long allOccupied,
			long allSameColorOccupied) {
		Bitboard.Rays rays = Bitboard.Rays.values()[kingInd];
		long pinnedPieces = getPositivePinnedPiece(rays.getRankPos() & allOccupied, straightSliders, allSameColorOccupied);
		pinnedPieces |= getPositivePinnedPiece(rays.getFilePos() & allOccupied, straightSliders, allSameColorOccupied);
		pinnedPieces |= getPositivePinnedPiece(rays.getDiagonalPos() & allOccupied, diagonalSliders, allSameColorOccupied);
		pinnedPieces |= getPositivePinnedPiece(rays.getAntiDiagonalPos() & allOccupied, diagonalSliders, allSameColorOccupied);
		pinnedPieces |= getNegativePinnedPiece(rays.getRankNeg() & allOccupied, straightSliders, allSameColorOccupied);
		pinnedPieces |= getNegativePinnedPiece(rays.getFileNeg() & allOccupied, straightSliders, allSameColorOccupied);
		pinnedPieces |= getNegativePinnedPiece(rays.getDiagonalNeg() & allOccupied, diagonalSliders, allSameColorOccupied);
		pinnedPieces |= getNegativePinnedPiece(rays.getAntiDiagonalNeg() & allOccupied, diagonalSliders, allSameColorOccupied);
		return pinnedPieces;
	}
	private void addNormalMovesToDestination(byte to, byte capturedPiece, long pieces, List<Move> moves) {
		while (pieces != Bitboard.EMPTY_BOARD) {
			byte from = BitOperations.indexOfLSBit(pieces);
			moves.add(new Move(from, to, squares[from], capturedPiece, (byte) MoveType.NORMAL.ordinal()));
			pieces = BitOperations.resetLSBit(pieces);
		}
	}
	private void addWhiteTacticalCheckEvasionMoves(List<Move> moves) {
		byte kingInd = BitOperations.indexOfBit(whiteKing);
		if (BitOperations.resetLSBit(checkers) == Bitboard.EMPTY_BOARD) {
			long checker1 = BitOperations.getLSBit(checkers);
			byte checker1Ind = BitOperations.indexOfBit(checker1);
			long checkLine = Bitboard.getLineSegment(checker1Ind, kingInd);
			long movablePieces = ~getPinnedPieces(kingInd, blackQueens | blackRooks, blackQueens | blackBishops,
					allOccupied, allWhiteOccupied);
			long attackers = getWhiteCheckers(checker1Ind);
			/* The intersection of the last rank and the check line can only be non-empty if the checker truly is a
			 * sliding piece (rook or queen) and thus there really is a check line. */
			long lastRankCheckLine = (Bitboard.Rank.R8.getBitboard() & checkLine);
			addWhitePawnPromotionMoves(Bitboard.computeBlackPawnAdvanceSets(lastRankCheckLine, movablePieces) |
					(attackers & movablePieces), lastRankCheckLine | checker1, moves);
			// Just assume en passant is legal in the position as the method anyway 'double' checks it.
			if (checker1Ind == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights ||
					((checker1 & (blackQueens | blackRooks | blackBishops)) != Bitboard.EMPTY_BOARD &&
					(checkLine & BitOperations.toBit(EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)) !=
							Bitboard.EMPTY_BOARD))
				addWhitePawnEnPassantMoves(movablePieces, kingInd, moves);
			addNormalMovesToDestination(checker1Ind, squares[checker1Ind], attackers &
					~(whitePawns & Bitboard.Rank.R7.getBitboard()) & movablePieces, moves);
		}
		addWhiteKingNormalMoves(kingInd, allBlackOccupied, moves);
	}
	private void addBlackTacticalCheckEvasionMoves(List<Move> moves) {
		byte kingInd = BitOperations.indexOfBit(blackKing);
		if (BitOperations.resetLSBit(checkers) == Bitboard.EMPTY_BOARD) {
			long checker1 = BitOperations.getLSBit(checkers);
			byte checker1Ind = BitOperations.indexOfBit(checker1);
			long checkLine = Bitboard.getLineSegment(checker1Ind, kingInd);
			long movablePieces = ~getPinnedPieces(kingInd, whiteQueens | whiteRooks, whiteQueens | whiteBishops,
					allOccupied, allBlackOccupied);
			long attackers = getBlackCheckers(checker1Ind);
			long lastRankCheckLine = (Bitboard.Rank.R1.getBitboard() & checkLine);
			addBlackPawnPromotionMoves(Bitboard.computeWhitePawnAdvanceSets(lastRankCheckLine, movablePieces) |
							(attackers & movablePieces), lastRankCheckLine | checker1, moves);
			if (checker1Ind == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights ||
					((checker1 & (whiteQueens | whiteRooks | whiteBishops)) != Bitboard.EMPTY_BOARD &&
					(checkLine & BitOperations.toBit((EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights))) !=
							Bitboard.EMPTY_BOARD))
				addBlackPawnEnPassantMoves(movablePieces, kingInd, moves);
			addNormalMovesToDestination(checker1Ind, squares[checker1Ind], attackers &
					~(blackPawns & Bitboard.Rank.R2.getBitboard()) & movablePieces, moves);
		}
		addBlackKingNormalMoves(kingInd, allWhiteOccupied, moves);
	}
	/**
	 * Returns a list of all the legal tactical moves in the current position. Tactical moves are ordinary captures,
	 * promotions, and en passant captures.
	 *
	 * @return A list of all the legal tactical moves.
	 */
	public List<Move> getTacticalMoves() {
		List<Move> moves = new LinkedList<>();
		if (whitesTurn) {
			if (inCheck)
				addWhiteTacticalCheckEvasionMoves(moves);
			else
				addWhiteTacticalMoves(moves);
		} else {
			if (inCheck)
				addBlackTacticalCheckEvasionMoves(moves);
			else
				addBlackTacticalMoves(moves);
		}
		return moves;
	}
	private long addQuietPinnedPieceMoveAndGetPinnedPiece(long pinnedPiece, long pinningPiece, byte queenType,
			byte secondarySliderType, List<Move> moves) {
		if (pinningPiece != Bitboard.EMPTY_BOARD) {
			byte from  = BitOperations.indexOfBit(pinnedPiece);
			byte pinnedPieceType = squares[from];
			if (pinnedPieceType == queenType || pinnedPieceType == secondarySliderType) {
				addNormalMovesFromOrigin(from, pinnedPieceType, Bitboard.getLineSegment(from,
						BitOperations.indexOfBit(pinningPiece))^pinnedPiece, moves);
			}
			return pinnedPiece;
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addQuietPositivePinnedPieceMoveAndGetPinnedPiece(long rayOccupancy, long allSameColorOccupied,
			long sliders, byte queenType, byte secondarySliderType, List<Move> moves) {
		long pinnedPiece = BitOperations.getLSBit(rayOccupancy) & allSameColorOccupied;
		if (pinnedPiece != Bitboard.EMPTY_BOARD) {
			return addQuietPinnedPieceMoveAndGetPinnedPiece(pinnedPiece,
					BitOperations.getLSBit(rayOccupancy^pinnedPiece) & sliders, queenType,
					secondarySliderType, moves);
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addQuietNegativePinnedPieceMoveAndGetPinnedPiece(long rayOccupancy, long allSameColorOccupied,
			long sliders, byte queenType, byte secondarySliderType, List<Move> moves) {
		long pinnedPiece = BitOperations.getMSBit(rayOccupancy) & allSameColorOccupied;
		if (pinnedPiece != Bitboard.EMPTY_BOARD) {
			return addQuietPinnedPieceMoveAndGetPinnedPiece(pinnedPiece,
					BitOperations.getMSBit(rayOccupancy^pinnedPiece) & sliders, queenType,
					secondarySliderType, moves);
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addWhiteQuietFilePinnedPieceMoveAndGetPinnedPiece(long pinnedPiece, long pinningPiece,
			List<Move> moves) {
		if (pinningPiece != Bitboard.EMPTY_BOARD) {
			byte from  = BitOperations.indexOfBit(pinnedPiece);
			byte pinnedPieceType = squares[from];
			if (pinnedPieceType == Piece.W_QUEEN.ordinal() || pinnedPieceType == Piece.W_ROOK.ordinal()) {
				addNormalMovesFromOrigin(from, pinnedPieceType, Bitboard.getLineSegment(from,
						BitOperations.indexOfBit(pinningPiece))^pinnedPiece, moves);
			} else if (pinnedPieceType == Piece.W_PAWN.ordinal()) {
				addNormalMovesFromOrigin(from, pinnedPieceType,
						MoveSetBase.values()[from].getWhitePawnAdvanceSet(allEmpty), moves);
			}
			return pinnedPiece;
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addBlackQuietFilePinnedPieceMoveAndGetPinnedPiece(long pinnedPiece, long pinningPiece,
			List<Move> moves) {
		if (pinningPiece != Bitboard.EMPTY_BOARD) {
			byte from  = BitOperations.indexOfBit(pinnedPiece);
			byte pinnedPieceType = squares[from];
			if (pinnedPieceType == Piece.B_QUEEN.ordinal() || pinnedPieceType == Piece.B_ROOK.ordinal()) {
				addNormalMovesFromOrigin(from, pinnedPieceType, Bitboard.getLineSegment(from,
						BitOperations.indexOfBit(pinningPiece))^pinnedPiece, moves);
			} else if (pinnedPieceType == Piece.B_PAWN.ordinal()) {
				addNormalMovesFromOrigin(from, pinnedPieceType,
						MoveSetBase.values()[from].getBlackPawnAdvanceSet(allEmpty), moves);
			}
			return pinnedPiece;
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addWhiteQuietFilePositivePinnedPieceMoveAndGetPinnedPiece(long rayOccupancy, long allSameColorOccupied,
			long sliders, List<Move> moves) {
		long pinnedPiece = BitOperations.getLSBit(rayOccupancy) & allSameColorOccupied;
		if (pinnedPiece != Bitboard.EMPTY_BOARD) {
			return addWhiteQuietFilePinnedPieceMoveAndGetPinnedPiece(pinnedPiece,
					BitOperations.getLSBit(rayOccupancy^pinnedPiece) & sliders, moves);
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addBlackQuietFilePositivePinnedPieceMoveAndGetPinnedPiece(long rayOccupancy, long allSameColorOccupied,
			long sliders, List<Move> moves) {
		long pinnedPiece = BitOperations.getLSBit(rayOccupancy) & allSameColorOccupied;
		if (pinnedPiece != Bitboard.EMPTY_BOARD) {
			return addBlackQuietFilePinnedPieceMoveAndGetPinnedPiece(pinnedPiece,
					BitOperations.getLSBit(rayOccupancy^pinnedPiece) & sliders, moves);
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addWhiteQuietFileNegativePinnedPieceMoveAndGetPinnedPiece(long rayOccupancy, long allSameColorOccupied,
			long sliders, List<Move> moves) {
		long pinnedPiece = BitOperations.getMSBit(rayOccupancy) & allSameColorOccupied;
		if (pinnedPiece != Bitboard.EMPTY_BOARD) {
			return addWhiteQuietFilePinnedPieceMoveAndGetPinnedPiece(pinnedPiece,
					BitOperations.getMSBit(rayOccupancy^pinnedPiece) & sliders, moves);
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addBlackQuietFileNegativePinnedPieceMoveAndGetPinnedPiece(long rayOccupancy, long allSameColorOccupied,
			long sliders, List<Move> moves) {
		long pinnedPiece = BitOperations.getMSBit(rayOccupancy) & allSameColorOccupied;
		if (pinnedPiece != Bitboard.EMPTY_BOARD) {
			return addBlackQuietFilePinnedPieceMoveAndGetPinnedPiece(pinnedPiece,
					BitOperations.getMSBit(rayOccupancy^pinnedPiece) & sliders, moves);
		}
		return Bitboard.EMPTY_BOARD;
	}
	private long addWhiteQuietPinnedPieceMovesAndGetPinnedPieces(byte kingInd, List<Move> moves) {
		Bitboard.Rays rays = Bitboard.Rays.values()[kingInd];
		long straightSliders = blackQueens | blackRooks;
		long diagonalSliders = blackQueens | blackBishops;
		long pinnedPieces = addQuietPositivePinnedPieceMoveAndGetPinnedPiece(rays.getRankPos() & allOccupied,
				allWhiteOccupied, straightSliders, (byte) Piece.W_QUEEN.ordinal(), (byte) Piece.W_ROOK.ordinal(), moves);
		pinnedPieces |= addWhiteQuietFilePositivePinnedPieceMoveAndGetPinnedPiece(rays.getFilePos() & allOccupied,
				allWhiteOccupied, straightSliders, moves);
		pinnedPieces |= addQuietPositivePinnedPieceMoveAndGetPinnedPiece(rays.getDiagonalPos() & allOccupied,
				allWhiteOccupied, diagonalSliders, (byte) Piece.W_QUEEN.ordinal(), (byte) Piece.W_BISHOP.ordinal(), moves);
		pinnedPieces |= addQuietPositivePinnedPieceMoveAndGetPinnedPiece(rays.getAntiDiagonalPos() & allOccupied,
				allWhiteOccupied, diagonalSliders, (byte) Piece.W_QUEEN.ordinal(), (byte) Piece.W_BISHOP.ordinal(), moves);
		pinnedPieces |= addQuietNegativePinnedPieceMoveAndGetPinnedPiece(rays.getRankNeg() & allOccupied,
				allWhiteOccupied, straightSliders, (byte) Piece.W_QUEEN.ordinal(), (byte) Piece.W_ROOK.ordinal(), moves);
		pinnedPieces |= addWhiteQuietFileNegativePinnedPieceMoveAndGetPinnedPiece(rays.getFileNeg() & allOccupied,
				allWhiteOccupied, straightSliders, moves);
		pinnedPieces |= addQuietNegativePinnedPieceMoveAndGetPinnedPiece(rays.getDiagonalNeg() & allOccupied,
				allWhiteOccupied, diagonalSliders, (byte) Piece.W_QUEEN.ordinal(), (byte) Piece.W_BISHOP.ordinal(), moves);
		pinnedPieces |= addQuietNegativePinnedPieceMoveAndGetPinnedPiece(rays.getAntiDiagonalNeg() & allOccupied,
				allWhiteOccupied, diagonalSliders, (byte) Piece.W_QUEEN.ordinal(), (byte) Piece.W_BISHOP.ordinal(), moves);
		return pinnedPieces;
	}
	private long addBlackQuietPinnedPieceMovesAndGetPinnedPieces(byte kingInd, List<Move> moves) {
		Bitboard.Rays rays = Bitboard.Rays.values()[kingInd];
		long straightSliders = whiteQueens | whiteRooks;
		long diagonalSliders = whiteQueens | whiteBishops;
		long pinnedPieces = addQuietPositivePinnedPieceMoveAndGetPinnedPiece(rays.getRankPos() & allOccupied,
				allBlackOccupied, straightSliders, (byte) Piece.B_QUEEN.ordinal(), (byte) Piece.B_ROOK.ordinal(), moves);
		pinnedPieces |= addBlackQuietFilePositivePinnedPieceMoveAndGetPinnedPiece(rays.getFilePos() & allOccupied,
				allBlackOccupied, straightSliders, moves);
		pinnedPieces |= addQuietPositivePinnedPieceMoveAndGetPinnedPiece(rays.getDiagonalPos() & allOccupied,
				allBlackOccupied, diagonalSliders, (byte) Piece.B_QUEEN.ordinal(), (byte) Piece.B_BISHOP.ordinal(), moves);
		pinnedPieces |= addQuietPositivePinnedPieceMoveAndGetPinnedPiece(rays.getAntiDiagonalPos() & allOccupied,
				allBlackOccupied, diagonalSliders, (byte) Piece.B_QUEEN.ordinal(), (byte) Piece.B_BISHOP.ordinal(), moves);
		pinnedPieces |= addQuietNegativePinnedPieceMoveAndGetPinnedPiece(rays.getRankNeg() & allOccupied,
				allBlackOccupied, straightSliders, (byte) Piece.B_QUEEN.ordinal(), (byte) Piece.B_ROOK.ordinal(), moves);
		pinnedPieces |= addBlackQuietFileNegativePinnedPieceMoveAndGetPinnedPiece(rays.getFileNeg() & allOccupied,
				allBlackOccupied, straightSliders, moves);
		pinnedPieces |= addQuietNegativePinnedPieceMoveAndGetPinnedPiece(rays.getDiagonalNeg() & allOccupied,
				allBlackOccupied, diagonalSliders, (byte) Piece.B_QUEEN.ordinal(), (byte) Piece.B_BISHOP.ordinal(), moves);
		pinnedPieces |= addQuietNegativePinnedPieceMoveAndGetPinnedPiece(rays.getAntiDiagonalNeg() & allOccupied,
				allBlackOccupied, diagonalSliders, (byte) Piece.B_QUEEN.ordinal(), (byte) Piece.B_BISHOP.ordinal(), moves);
		return pinnedPieces;
	}
	private void addWhiteQuietMoves(List<Move> moves) {
		byte kingInd = BitOperations.indexOfBit(whiteKing);
		long movablePieces = ~addWhiteQuietPinnedPieceMovesAndGetPinnedPieces(kingInd, moves);
		addWhitePawnNormalMoves(movablePieces, Bitboard.EMPTY_BOARD, allEmpty, moves);
		addKnightMoves((byte) Piece.W_KNIGHT.ordinal(), whiteKnights & movablePieces, allEmpty, moves);
		addBishopMoves((byte) Piece.W_BISHOP.ordinal(), whiteBishops & movablePieces, allEmpty, moves);
		addRookMoves((byte) Piece.W_ROOK.ordinal(), whiteRooks & movablePieces, allEmpty, moves);
		addQueenMoves((byte) Piece.W_QUEEN.ordinal(), whiteQueens & movablePieces, allEmpty, moves);
		addWhiteKingCastlingMoves(kingInd, moves);
		addWhiteKingNormalMoves(kingInd, allEmpty, moves);
	}
	private void addBlackQuietMoves(List<Move> moves) {
		byte kingInd = BitOperations.indexOfBit(blackKing);
		long movablePieces = ~addBlackQuietPinnedPieceMovesAndGetPinnedPieces(kingInd, moves);
		addBlackPawnNormalMoves(movablePieces, Bitboard.EMPTY_BOARD, allEmpty, moves);
		addKnightMoves((byte) Piece.B_KNIGHT.ordinal(), blackKnights & movablePieces, allEmpty, moves);
		addBishopMoves((byte) Piece.B_BISHOP.ordinal(), blackBishops & movablePieces, allEmpty, moves);
		addRookMoves((byte) Piece.B_ROOK.ordinal(), blackRooks & movablePieces, allEmpty, moves);
		addQueenMoves((byte) Piece.B_QUEEN.ordinal(), blackQueens & movablePieces, allEmpty, moves);
		addBlackKingCastlingMoves(kingInd, moves);
		addBlackKingNormalMoves(kingInd, allEmpty, moves);
	}
	private long getWhitePushers(int sqrInd) {
		MoveSetBase dB = MoveSetBase.values()[sqrInd];
		long blackPawnAdvance = dB.getBlackPawnAdvanceSet(Bitboard.FULL_BOARD);
		long pushers = whiteKnights & dB.getKnightMoveMask();
		pushers |= whitePawns & blackPawnAdvance;
		if (Bitboard.Rank.getBySquareIndex(sqrInd) == Bitboard.Rank.R4 &&
				(allEmpty & blackPawnAdvance) != Bitboard.EMPTY_BOARD)
			pushers |= Bitboard.computeWhitePawnAdvanceSets(blackPawnAdvance, whitePawns);
		pushers |= (whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied);
		pushers |= (whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
		return pushers;
	}
	private long getBlackPushers(int sqrInd) {
		MoveSetBase dB = MoveSetBase.values()[sqrInd];
		long whitePawnAdvance = dB.getWhitePawnAdvanceSet(Bitboard.FULL_BOARD);
		long pushers = blackKnights & dB.getKnightMoveMask();
		pushers |= blackPawns & whitePawnAdvance;
		if (Bitboard.Rank.getBySquareIndex(sqrInd) == Bitboard.Rank.R5 &&
				(allEmpty & whitePawnAdvance) != Bitboard.EMPTY_BOARD)
			pushers |= Bitboard.computeWhitePawnAdvanceSets(whitePawnAdvance, blackPawns);
		pushers |= (blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied);
		pushers |= (blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied);
		return pushers;
	}
	private void addWhiteQuietCheckEvasionMoves(List<Move> moves) {
		byte kingInd = BitOperations.indexOfBit(whiteKing);
		long checker1 = BitOperations.getLSBit(checkers);
		long straightSliders = blackQueens | blackRooks;
		long diagonalSliders = blackQueens | blackBishops;
		long sliders = straightSliders | diagonalSliders;
		long checkLines = (checker1 & sliders) != Bitboard.EMPTY_BOARD ?
				Bitboard.getLineSegment(BitOperations.indexOfBit(checker1), kingInd) : Bitboard.EMPTY_BOARD;
		long checkersTemp = BitOperations.resetLSBit(checkers);
		if (checkersTemp != Bitboard.EMPTY_BOARD) {
			checkLines |= (checkersTemp & sliders) != Bitboard.EMPTY_BOARD ?
					Bitboard.getLineSegment(BitOperations.indexOfBit(checkersTemp), kingInd) : Bitboard.EMPTY_BOARD;
		} else if (checkLines != Bitboard.EMPTY_BOARD) {
			long movablePieces = ~getPinnedPieces(kingInd, straightSliders, diagonalSliders, allOccupied,
					allWhiteOccupied) & ~(whitePawns & Bitboard.Rank.R7.getBitboard());
			long checkLinesTemp = checkLines;
			while (checkLinesTemp != Bitboard.EMPTY_BOARD) {
				byte to = BitOperations.indexOfLSBit(checkLinesTemp);
				addNormalMovesToDestination(to, (byte) Piece.NULL.ordinal(), getWhitePushers(to) & movablePieces, moves);
				checkLinesTemp = BitOperations.resetLSBit(checkLinesTemp);
			}
		}
		addWhiteKingNormalMoves(kingInd, allEmpty & ~checkLines, moves);
	}
	private void addBlackQuietCheckEvasionMoves(List<Move> moves) {
		byte kingInd = BitOperations.indexOfBit(blackKing);
		long checker1 = BitOperations.getLSBit(checkers);
		long straightSliders = whiteQueens | whiteRooks;
		long diagonalSliders = whiteQueens | whiteBishops;
		long sliders = straightSliders | diagonalSliders;
		long checkLines = (checker1 & sliders) != Bitboard.EMPTY_BOARD ?
				Bitboard.getLineSegment(BitOperations.indexOfBit(checker1), kingInd) : Bitboard.EMPTY_BOARD;
		long checkersTemp = BitOperations.resetLSBit(checkers);
		if (checkersTemp != Bitboard.EMPTY_BOARD) {
			checkLines |= (checkersTemp & sliders) != Bitboard.EMPTY_BOARD ?
					Bitboard.getLineSegment(BitOperations.indexOfBit(checkersTemp), kingInd) : Bitboard.EMPTY_BOARD;
		} else if (checkLines != Bitboard.EMPTY_BOARD) {
			long movablePieces = ~getPinnedPieces(kingInd, straightSliders, diagonalSliders, allOccupied,
					allBlackOccupied) & ~(blackPawns & Bitboard.Rank.R2.getBitboard());
			long checkLinesTemp = checkLines;
			while (checkLinesTemp != Bitboard.EMPTY_BOARD) {
				byte to = BitOperations.indexOfLSBit(checkLinesTemp);
				addNormalMovesToDestination(to, (byte) Piece.NULL.ordinal(), getBlackPushers(to) & movablePieces, moves);
				checkLinesTemp = BitOperations.resetLSBit(checkLinesTemp);
			}
		}
		addBlackKingNormalMoves(kingInd, allEmpty & ~checkLines, moves);
	}
	/**
	 * Returns a list of all the legal quiet moves in the current position. Quiet moves are ordinary non-capture moves
	 * and castling moves.
	 *
	 * @return A list of all the legal quiet moves.
	 */
	public List<Move> getQuietMoves() {
		List<Move> moves = new LinkedList<>();
		if (whitesTurn) {
			if (inCheck)
				addWhiteQuietCheckEvasionMoves(moves);
			else
				addWhiteQuietMoves(moves);
		} else {
			if (inCheck)
				addBlackQuietCheckEvasionMoves(moves);
			else
				addBlackQuietMoves(moves);
		}
		return moves;
	}
	/**
	 * Returns a list of all the legal moves in the current position.
	 *
	 * @return A list of all the legal moves.
	 */
	public List<Move> getMoves() {
		List<Move> moves = new LinkedList<>();
		if (whitesTurn) {
			if (inCheck) {
				addWhiteTacticalCheckEvasionMoves(moves);
				addWhiteQuietCheckEvasionMoves(moves);
			} else {
				addWhiteTacticalMoves(moves);
				addWhiteQuietMoves(moves);
			}
		} else {
			if (inCheck) {
				addBlackTacticalCheckEvasionMoves(moves);
				addBlackQuietCheckEvasionMoves(moves);
			} else {
				addBlackTacticalMoves(moves);
				addBlackQuietMoves(moves);
			}
		}
		return moves;
	}
	@Override
	public String toString() {
		String fen = "";
		for (int i = 7; i >= 0; i--) {
			int emptyCount = 0;
			for (int j = 0; j < 8; j++) {
				int piece = squares[i*8 + j];
				if (piece == 0)
					emptyCount++;
				else {
					if (emptyCount != 0)
						fen += emptyCount;
					emptyCount = 0;
					fen += Piece.values()[piece].getLetter();
				}
			}
			if (emptyCount != 0)
				fen += emptyCount;
			if (i != 0)
				fen += "/";
		}
		fen += " " + (whitesTurn ? "w" : "b") + " ";
		String castlingRights = "";
		switch (CastlingRights.values()[whiteCastlingRights]) {
			case SHORT:
				castlingRights += "K";
				break;
			case LONG:
				castlingRights += "Q";
				break;
			case ALL:
				castlingRights += "KQ";
				break;
			case NONE:
				break;
		}
		switch (CastlingRights.values()[blackCastlingRights]) {
			case SHORT:
				castlingRights += "k";
				break;
			case LONG:
				castlingRights += "q";
				break;
			case ALL:
				castlingRights += "kq";
				break;
			case NONE:
				break;
		}
		fen += (castlingRights.isEmpty() ? "-" : castlingRights) + " ";
		fen += (enPassantRights == EnPassantRights.NONE.ordinal() ? "-" :
				(EnPassantRights.values()[enPassantRights].toString().toLowerCase() + (whitesTurn ? 6 : 3))) + " ";
		fen += fiftyMoveRuleClock + " " + (1 + halfMoveIndex/2);
		return fen;
	}

}
