package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.util.BitOperations;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * A bitboard based chess position class that stores information about the side to move, en passant and castling
 * rights, the board position, all the moves made, and all the previous position states. It allows for the making and
 * taking back of chess moves.
 *
 * @author Viktor
 *
 */
public class Position {

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
	private long allEmpty;
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
		pos.allEmpty = ~(pos.allWhiteOccupied | pos.allBlackOccupied);
		pos.whitesTurn = turn.toLowerCase().compareTo("w") == 0;
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
		pos.inCheck = pos.checkers != 0;
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
	/**
	 * @return A bitboard for all squares not occupied by white pieces.
	 */
	public long getAllNonWhiteOccupied() {
		return ~allWhiteOccupied;
	}
	/**
	 * @return A bitboard for all squares not occupied by black pieces.
	 */
	public long getAllNonBlackOccupied() {
		return ~allBlackOccupied;
	}
	/**
	 * @return A bitboard for all occupied squares.
	 */
	public long getAllOccupied() {
		return ~allEmpty;
	}
	/**
	 * @return The number of pieces on the board
	 */
	public int getNumberOfPieces() {
		return BitOperations.hammingWeight(~allEmpty);
	}
	/**
	 * @return The last move made.
	 */
	public Move getLastMove() {
		return moveHistory.peekFirst();
	}
	/**
	 * @return The last position state.
	 */
	public PositionStateRecord getLastStateRecord() {
		return stateHistory.peekFirst();
	}
	private void updateAllEmptyBitboard() {
		allEmpty = ~(allWhiteOccupied | allBlackOccupied);
	}
	private void captureWhitePieceOnBitboard(byte capturedPiece, long toBit) {
		if (capturedPiece != Piece.NULL.ordinal()) {
			allWhiteOccupied ^= toBit;
			whiteQueens &= allWhiteOccupied;
			whiteRooks &= allWhiteOccupied;
			whiteBishops &= allWhiteOccupied;
			whiteKnights &= allWhiteOccupied;
			whitePawns &= allWhiteOccupied;
		}
	}
	private void captureBlackPieceOnBitboard(byte capturedPiece, long toBit) {
		if (capturedPiece != Piece.NULL.ordinal()) {
			allBlackOccupied ^= toBit;
			blackQueens &= allBlackOccupied;
			blackRooks &= allBlackOccupied;
			blackBishops &= allBlackOccupied;
			blackKnights &= allBlackOccupied;
			blackPawns &= allBlackOccupied;
		}
	}
	private void makeWhiteNormalMoveOnBoard(byte from, byte to, byte movedPiece, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = movedPiece;
		long toBit = 1L << to;
		long changedBits = (1L << from) | toBit;
		if (movedPiece == Piece.W_KING.ordinal())
			whiteKing = toBit;
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
		captureBlackPieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void makeBlackNormalMoveOnBoard(byte from, byte to, byte movedPiece, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = movedPiece;
		long toBit = 1L << to;
		long changedBits = (1L << from) | toBit;
		if (movedPiece == Piece.B_KING.ordinal())
			blackKing = toBit;
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
		captureWhitePieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void makeWhiteShortCastlingMoveOnBoard() {
		squares[Bitboard.Square.H1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.F1.ordinal()] = (byte) Piece.W_ROOK.ordinal();
		squares[Bitboard.Square.E1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.G1.ordinal()] = (byte) Piece.W_KING.ordinal();
		long toBit = Bitboard.Square.G1.getBitboard();
		whiteKing = toBit;
		long rookChangedBits = Bitboard.Square.H1.getBitboard() | Bitboard.Square.F1.getBitboard();
		whiteRooks ^= rookChangedBits;
		allWhiteOccupied ^= (Bitboard.Square.E1.getBitboard() | toBit | rookChangedBits);
		updateAllEmptyBitboard();
	}
	private void makeBlackShortCastlingMoveOnBoard() {
		squares[Bitboard.Square.H8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.F8.ordinal()] = (byte) Piece.B_ROOK.ordinal();
		squares[Bitboard.Square.E8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.G8.ordinal()] = (byte) Piece.B_KING.ordinal();
		long toBit = Bitboard.Square.G8.getBitboard();
		blackKing = toBit;
		long rookChangedBits = Bitboard.Square.H8.getBitboard() | Bitboard.Square.F8.getBitboard();
		blackRooks ^= rookChangedBits;
		allBlackOccupied ^= (Bitboard.Square.E8.getBitboard() | toBit | rookChangedBits);
		updateAllEmptyBitboard();
	}
	private void makeWhiteLongCastlingMoveOnBoard() {
		squares[Bitboard.Square.A1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.D1.ordinal()] = (byte) Piece.W_ROOK.ordinal();
		squares[Bitboard.Square.E1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.C1.ordinal()] = (byte) Piece.W_KING.ordinal();
		long toBit = Bitboard.Square.C1.getBitboard();
		whiteKing = toBit;
		long rookChangedBits = Bitboard.Square.A1.getBitboard() | Bitboard.Square.D1.getBitboard();
		whiteRooks ^= rookChangedBits;
		allWhiteOccupied ^= (Bitboard.Square.E1.getBitboard() | toBit | rookChangedBits);
		updateAllEmptyBitboard();
	}
	private void makeBlackLongCastlingMoveOnBoard() {
		squares[Bitboard.Square.A8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.D8.ordinal()] = (byte) Piece.B_ROOK.ordinal();
		squares[Bitboard.Square.E8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.C8.ordinal()] = (byte) Piece.B_KING.ordinal();
		long toBit = Bitboard.Square.C8.getBitboard();
		blackKing = toBit;
		long rookChangedBits = Bitboard.Square.A8.getBitboard() | Bitboard.Square.D8.getBitboard();
		blackRooks ^= rookChangedBits;
		allBlackOccupied ^= (Bitboard.Square.E8.getBitboard() | toBit | rookChangedBits);
		updateAllEmptyBitboard();
	}
	private void makeWhiteEnPassantMoveOnBoard(byte from, byte to) {
		int enPassantVictimSqrInd = to - 8;
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.W_PAWN.ordinal();
		squares[enPassantVictimSqrInd] = (byte) Piece.NULL.ordinal();
		long changedBits = (1L << from) | (1L << to);
		whitePawns ^= changedBits;
		allWhiteOccupied ^= changedBits;
		long victimBit = 1L << enPassantVictimSqrInd;
		blackPawns ^= victimBit;
		allBlackOccupied ^= victimBit;
		updateAllEmptyBitboard();
	}
	private void makeBlackEnPassantMoveOnBoard(byte from, byte to) {
		int enPassantVictimSqrInd = to + 8;
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.B_PAWN.ordinal();
		squares[enPassantVictimSqrInd] = (byte) Piece.NULL.ordinal();
		long changedBits = (1L << from) | (1L << to);
		blackPawns ^= changedBits;
		allBlackOccupied ^= changedBits;
		long victimBit = 1L << enPassantVictimSqrInd;
		whitePawns ^= victimBit;
		allWhiteOccupied ^= victimBit;
		updateAllEmptyBitboard();
	}
	private void makeWhiteQueenPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.W_QUEEN.ordinal();
		long fromBit = 1L << from;
		long toBit = 1L << to;
		whitePawns ^= fromBit;
		whiteQueens ^= toBit;
		allWhiteOccupied ^= (fromBit | toBit);
		captureBlackPieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void makeBlackQueenPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.B_QUEEN.ordinal();
		long fromBit = 1L << from;
		long toBit = 1L << to;
		blackPawns ^= fromBit;
		blackQueens ^= toBit;
		allBlackOccupied ^= (fromBit | toBit);
		captureWhitePieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void makeWhiteRookPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.W_ROOK.ordinal();
		long fromBit = 1L << from;
		long toBit = 1L << to;
		whitePawns ^= fromBit;
		whiteRooks ^= toBit;
		allWhiteOccupied ^= (fromBit | toBit);
		captureBlackPieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void makeBlackRookPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.B_ROOK.ordinal();
		long fromBit = 1L << from;
		long toBit = 1L << to;
		blackPawns ^= fromBit;
		blackRooks ^= toBit;
		allBlackOccupied ^= (fromBit | toBit);
		captureWhitePieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void makeWhiteBishopPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.W_BISHOP.ordinal();
		long fromBit = 1L << from;
		long toBit = 1L << to;
		whitePawns ^= fromBit;
		whiteBishops ^= toBit;
		allWhiteOccupied ^= (fromBit | toBit);
		captureBlackPieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void makeBlackBishopPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.B_BISHOP.ordinal();
		long fromBit = 1L << from;
		long toBit = 1L << to;
		blackPawns ^= fromBit;
		blackBishops ^= toBit;
		allBlackOccupied ^= (fromBit | toBit);
		captureWhitePieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void makeWhiteKnightPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.W_KNIGHT.ordinal();
		long fromBit = 1L << from;
		long toBit = 1L << to;
		whitePawns ^= fromBit;
		whiteKnights ^= toBit;
		allWhiteOccupied ^= (fromBit | toBit);
		captureBlackPieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void makeBlackKnightPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.NULL.ordinal();
		squares[to] = (byte) Piece.B_KNIGHT.ordinal();
		long fromBit = 1L << from;
		long toBit = 1L << to;
		blackPawns ^= fromBit;
		blackKnights ^= toBit;
		allBlackOccupied ^= (fromBit | toBit);
		captureWhitePieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void uncaptureWhitePieceOnBitboard(byte capturedPiece, long toBit) {
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
	private void uncaptureBlackPieceOnBitboard(byte capturedPiece, long toBit) {
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
	private void unmakeWhiteNormalMoveOnBoard(byte from, byte to, byte movedPiece, byte capturedPiece) {
		squares[from] = movedPiece;
		squares[to] = capturedPiece;
		long fromBit = 1L << from;
		long toBit = 1L << to;
		long changedBits = fromBit | toBit;
		if (movedPiece == Piece.W_KING.ordinal())
			whiteKing = fromBit;
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
		uncaptureBlackPieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void unmakeBlackNormalMoveOnBoard(byte from, byte to, byte movedPiece, byte capturedPiece) {
		squares[from] = movedPiece;
		squares[to] = capturedPiece;
		long fromBit = 1L << from;
		long toBit = 1L << to;
		long changedBits = fromBit | toBit;
		if (movedPiece == Piece.B_KING.ordinal())
			blackKing = fromBit;
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
		uncaptureWhitePieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void unmakeWhiteShortCastlingMoveOnBoard() {
		squares[Bitboard.Square.F1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.H1.ordinal()] = (byte) Piece.W_ROOK.ordinal();
		squares[Bitboard.Square.G1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.E1.ordinal()] = (byte) Piece.W_KING.ordinal();
		long fromBit = Bitboard.Square.E1.getBitboard();
		whiteKing = fromBit;
		long rookChangedBits = Bitboard.Square.H1.getBitboard() | Bitboard.Square.F1.getBitboard();
		whiteRooks ^= rookChangedBits;
		allWhiteOccupied ^= (Bitboard.Square.G1.getBitboard() | fromBit | rookChangedBits);
		updateAllEmptyBitboard();
	}
	private void unmakeBlackShortCastlingMoveOnBoard() {
		squares[Bitboard.Square.F8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.H8.ordinal()] = (byte) Piece.B_ROOK.ordinal();
		squares[Bitboard.Square.G8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.E8.ordinal()] = (byte) Piece.B_KING.ordinal();
		long fromBit = Bitboard.Square.E8.getBitboard();
		blackKing = fromBit;
		long rookChangedBits = Bitboard.Square.H8.getBitboard() | Bitboard.Square.F8.getBitboard();
		blackRooks ^= rookChangedBits;
		allBlackOccupied ^= (Bitboard.Square.G8.getBitboard() | fromBit | rookChangedBits);
		updateAllEmptyBitboard();
	}
	private void unmakeWhiteLongCastlingMoveOnBoard() {
		squares[Bitboard.Square.D1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.A1.ordinal()] = (byte) Piece.W_ROOK.ordinal();
		squares[Bitboard.Square.C1.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.E1.ordinal()] = (byte) Piece.W_KING.ordinal();
		long fromBit = Bitboard.Square.E1.getBitboard();
		whiteKing = fromBit;
		long rookChangedBits = Bitboard.Square.A1.getBitboard() | Bitboard.Square.D1.getBitboard();
		whiteRooks ^= rookChangedBits;
		allWhiteOccupied ^= (Bitboard.Square.C1.getBitboard() | fromBit | rookChangedBits);
		updateAllEmptyBitboard();
	}
	private void unmakeBlackLongCastlingMoveOnBoard() {
		squares[Bitboard.Square.D8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.A8.ordinal()] = (byte) Piece.B_ROOK.ordinal();
		squares[Bitboard.Square.C8.ordinal()] = (byte) Piece.NULL.ordinal();
		squares[Bitboard.Square.E8.ordinal()] = (byte) Piece.B_KING.ordinal();
		long fromBit = Bitboard.Square.E8.getBitboard();
		blackKing = fromBit;
		long rookChangedBits = Bitboard.Square.A8.getBitboard() | Bitboard.Square.D8.getBitboard();
		blackRooks ^= rookChangedBits;
		allBlackOccupied ^= (Bitboard.Square.C8.getBitboard() | fromBit | rookChangedBits);
		updateAllEmptyBitboard();
	}
	private void unmakeWhiteEnPassantMoveOnBoard(byte from, byte to) {
		int enPassantVictimSqrInd = to - 8;
		squares[from] = (byte) Piece.W_PAWN.ordinal();
		squares[to] = (byte) Piece.NULL.ordinal();
		squares[enPassantVictimSqrInd] = (byte) Piece.B_PAWN.ordinal();
		long changedBits = (1L << from) | (1L << to);
		whitePawns ^= changedBits;
		allWhiteOccupied ^= changedBits;
		long victimBit = 1L << enPassantVictimSqrInd;
		blackPawns ^= victimBit;
		allBlackOccupied ^= victimBit;
		updateAllEmptyBitboard();
	}
	private void unmakeBlackEnPassantMoveOnBoard(byte from, byte to) {
		int enPassantVictimSqrInd = to + 8;
		squares[from] = (byte) Piece.B_PAWN.ordinal();
		squares[to] = (byte) Piece.NULL.ordinal();
		squares[enPassantVictimSqrInd] = (byte) Piece.W_PAWN.ordinal();
		long changedBits = (1L << from) | (1L << to);
		blackPawns ^= changedBits;
		allBlackOccupied ^= changedBits;
		long victimBit = 1L << enPassantVictimSqrInd;
		whitePawns ^= victimBit;
		allWhiteOccupied ^= victimBit;
		updateAllEmptyBitboard();
	}
	private void unmakeWhiteQueenPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.W_PAWN.ordinal();
		squares[to] = capturedPiece;
		long fromBit = 1L << from;
		long toBit = 1L << to;
		whitePawns ^= fromBit;
		whiteQueens ^= toBit;
		allWhiteOccupied ^= (fromBit | toBit);
		uncaptureBlackPieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void unmakeBlackQueenPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.B_PAWN.ordinal();
		squares[to] = capturedPiece;
		long fromBit = 1L << from;
		long toBit = 1L << to;
		blackPawns ^= fromBit;
		blackQueens ^= toBit;
		allBlackOccupied ^= (fromBit | toBit);
		uncaptureWhitePieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void unmakeWhiteRookPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.W_PAWN.ordinal();
		squares[to] = capturedPiece;
		long fromBit = 1L << from;
		long toBit = 1L << to;
		whitePawns ^= fromBit;
		whiteRooks ^= toBit;
		allWhiteOccupied ^= (fromBit | toBit);
		uncaptureBlackPieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void unmakeBlackRookPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.B_PAWN.ordinal();
		squares[to] = capturedPiece;
		long fromBit = 1L << from;
		long toBit = 1L << to;
		blackPawns ^= fromBit;
		blackRooks ^= toBit;
		allBlackOccupied ^= (fromBit | toBit);
		uncaptureWhitePieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void unmakeWhiteBishopPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.W_PAWN.ordinal();
		squares[to] = capturedPiece;
		long fromBit = 1L << from;
		long toBit = 1L << to;
		whitePawns ^= fromBit;
		whiteBishops ^= toBit;
		allWhiteOccupied ^= (fromBit | toBit);
		uncaptureBlackPieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void unmakeBlackBishopPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.B_PAWN.ordinal();
		squares[to] = capturedPiece;
		long fromBit = 1L << from;
		long toBit = 1L << to;
		blackPawns ^= fromBit;
		blackBishops ^= toBit;
		allBlackOccupied ^= (fromBit | toBit);
		uncaptureWhitePieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void unmakeWhiteKnightPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.W_PAWN.ordinal();
		squares[to] = capturedPiece;
		long fromBit = 1L << from;
		long toBit = 1L << to;
		whitePawns ^= fromBit;
		whiteKnights ^= toBit;
		allWhiteOccupied ^= (fromBit | toBit);
		uncaptureBlackPieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void unmakeBlackKnightPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
		squares[from] = (byte) Piece.B_PAWN.ordinal();
		squares[to] = capturedPiece;
		long fromBit = 1L << from;
		long toBit = 1L << to;
		blackPawns ^= fromBit;
		blackKnights ^= toBit;
		allBlackOccupied ^= (fromBit | toBit);
		uncaptureWhitePieceOnBitboard(capturedPiece, toBit);
		updateAllEmptyBitboard();
	}
	private void makeWhiteMoveAndUpdateKeyOnBoard(Move move) {
		byte moveType = move.getType();
		ZobristKeyGenerator gen = ZobristKeyGenerator.getInstance();
		if (moveType == MoveType.NORMAL.ordinal()) {
			makeWhiteNormalMoveOnBoard(move.getFrom(), move.getTo(), move.getMovedPiece(), move.getCapturedPiece());
			key = gen.updateBoardHashKeyAfterNormalMove(key, move.getFrom(), move.getTo(), move.getMovedPiece(),
					move.getCapturedPiece());
		} else if (moveType == MoveType.SHORT_CASTLING.ordinal()) {
			makeWhiteShortCastlingMoveOnBoard();
			key = gen.updateBoardHashKeyAfterWhiteShortCastlinglMove(key);
		} else if (moveType == MoveType.LONG_CASTLING.ordinal()) {
			makeWhiteLongCastlingMoveOnBoard();
			key = gen.updateBoardHashKeyAfterWhiteLongCastlinglMove(key);
		} else if (moveType == MoveType.EN_PASSANT.ordinal()) {
			makeWhiteEnPassantMoveOnBoard(move.getFrom(), move.getTo());
			key = gen.updateBoardHashKeyAfterWhiteEnPassantMove(key, move.getFrom(), move.getTo());
		} else if (moveType == MoveType.PROMOTION_TO_QUEEN.ordinal()) {
			makeWhiteQueenPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.updateBoardHashKeyAfterWhiteQueenPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_ROOK.ordinal()) {
			makeWhiteRookPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.updateBoardHashKeyAfterWhiteRookPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_BISHOP.ordinal()) {
			makeWhiteBishopPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.updateBoardHashKeyAfterWhiteBishopPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		} else {
			makeWhiteKnightPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.updateBoardHashKeyAfterWhiteKnightPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		}
	}
	private void makeBlackMoveAndUpdateKeyOnBoard(Move move) {
		byte moveType = move.getType();
		ZobristKeyGenerator gen = ZobristKeyGenerator.getInstance();
		if (moveType == MoveType.NORMAL.ordinal()) {
			makeBlackNormalMoveOnBoard(move.getFrom(), move.getTo(), move.getMovedPiece(), move.getCapturedPiece());
			key = gen.updateBoardHashKeyAfterNormalMove(key, move.getFrom(), move.getTo(), move.getMovedPiece(),
					move.getCapturedPiece());
		} else if (moveType == MoveType.SHORT_CASTLING.ordinal()) {
			makeBlackShortCastlingMoveOnBoard();
			key = gen.updateBoardHashKeyAfterBlackShortCastlinglMove(key);
		} else if (moveType == MoveType.LONG_CASTLING.ordinal()) {
			makeBlackLongCastlingMoveOnBoard();
			key = gen.updateBoardHashKeyAfterBlackLongCastlinglMove(key);
		} else if (moveType == MoveType.EN_PASSANT.ordinal()) {
			makeBlackEnPassantMoveOnBoard(move.getFrom(), move.getTo());
			key = gen.updateBoardHashKeyAfterBlackEnPassantMove(key, move.getFrom(), move.getTo());
		} else if (moveType == MoveType.PROMOTION_TO_QUEEN.ordinal()) {
			makeBlackQueenPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.updateBoardHashKeyAfterBlackQueenPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_ROOK.ordinal()) {
			makeBlackRookPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.updateBoardHashKeyAfterBlackRookPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		} else if (moveType == MoveType.PROMOTION_TO_BISHOP.ordinal()) {
			makeBlackBishopPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.updateBoardHashKeyAfterBlackBishopPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		} else {
			makeBlackKnightPromotionMoveOnBoard(move.getFrom(), move.getTo(), move.getCapturedPiece());
			key = gen.updateBoardHashKeyAfterBlackKnightPromotionMove(key, move.getFrom(), move.getTo(),
					move.getCapturedPiece());
		}
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
	private long updateWhiteKingCheckers() {
		long allNonWhiteOccupied = ~allWhiteOccupied;
		long allOccupied = ~allEmpty;
		MoveSetBase dB = MoveSetBase.values()[BitOperations.indexOfBit(whiteKing)];
		long attackers = blackKnights & dB.getKnightMoveMask();
		attackers |= blackPawns & dB.getPawnWhiteCaptureMoveMask();
		attackers |= (blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied);
		attackers |= (blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied);
		return attackers;
	}
	private long updateBlackKingCheckers() {
		long allNonBlackOccupied = ~allBlackOccupied;
		long allOccupied = ~allEmpty;
		MoveSetBase dB = MoveSetBase.values()[BitOperations.indexOfBit(blackKing)];
		long attackers = whiteKnights & dB.getKnightMoveMask();
		attackers |= whitePawns & dB.getPawnBlackCaptureMoveMask();
		attackers |= (whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied);
		attackers |= (whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
		return attackers;
	}
	private void ensureKeyHistoryCapacity() {
		if (keyHistory.length - halfMoveIndex <= 3)
			keyHistory = Arrays.copyOf(keyHistory, keyHistory.length + (keyHistory.length >> 1));
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
			checkers = updateBlackKingCheckers();
			updateBlackCastlingRights();
			enPassantRights = (move.getMovedPiece() == Piece.W_PAWN.ordinal() &&
					move.getTo() - move.getFrom() == 16) ? (byte) (move.getTo()%8) : 8;
			fiftyMoveRuleClock = (move.getCapturedPiece() != Piece.NULL.ordinal() ||
					move.getMovedPiece() == Piece.W_PAWN.ordinal()) ? 0 : (byte) (fiftyMoveRuleClock + 1);
		} else {
			makeBlackMoveAndUpdateKeyOnBoard(move);
			checkers = updateWhiteKingCheckers();
			updateWhiteCastlingRights();
			enPassantRights = (move.getMovedPiece() == Piece.B_PAWN.ordinal() &&
					move.getFrom() - move.getTo() == 16) ? (byte) (move.getTo()%8) : 8;
			fiftyMoveRuleClock = (move.getCapturedPiece() != Piece.NULL.ordinal() ||
					move.getMovedPiece() == Piece.B_PAWN.ordinal()) ? 0 : (byte) (fiftyMoveRuleClock + 1);
		}
		whitesTurn = !whitesTurn;
		inCheck = checkers != 0;
		halfMoveIndex++;
		ensureKeyHistoryCapacity();
		key = ZobristKeyGenerator.getInstance().updateOffBoardHashKey(this);
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
		key = ZobristKeyGenerator.getInstance().updateOffBoardHashKey(this);
		keyHistory[halfMoveIndex] = key;
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
		inCheck = checkers != 0;
		keyHistory[halfMoveIndex] = 0;
		key = keyHistory[--halfMoveIndex];
		return move;
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
