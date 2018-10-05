package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.util.BitOperations;

import java.util.ArrayDeque;

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
		moveHistory = new ArrayDeque<Move>();
		stateHistory = new ArrayDeque<PositionStateRecord>();
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
		squares = new byte[pos.squares.length];
		for (int i = 0; i < pos.squares.length; i++)
			squares[i] = pos.squares[i];
		keyHistory = new long[pos.keyHistory.length];
		for (int i = 0; i < pos.keyHistory.length; i++)
			keyHistory[i] = pos.keyHistory[i];
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
	public PositionStateRecord getUnmakeRegister() {
		return stateHistory.peekFirst();
	}
	public void makeMove(Move move) {

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
