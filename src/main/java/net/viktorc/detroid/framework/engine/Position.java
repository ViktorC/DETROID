package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.util.BitOperations;

import java.util.ArrayDeque;

class Position {

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
	static Position parse(String fen) throws ChessParseException {
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
			pos.squares[i] = Piece.NULL.ind;
		for (int i = 7; i >= 0; i--) {
			String rank = ranks[i];
			int index = 0;
			for (int j = 0; j < rank.length(); j++) {
				char piece = rank.charAt(j);
				int pieceNum = piece - '0';
				if (pieceNum >= 0 && pieceNum <= 8)
					index += pieceNum;
				else {
					pos.squares[index] = Piece.parse(piece).ind;
					switch (piece) {
						case 'K':
							pos.whiteKing = Bitboard.Square.getByIndex(index).bitboard;
							break;
						case 'Q':
							pos.whiteQueens	|= Bitboard.Square.getByIndex(index).bitboard;
							break;
						case 'R':
							pos.whiteRooks |= Bitboard.Square.getByIndex(index).bitboard;
							break;
						case 'B':
							pos.whiteBishops |= Bitboard.Square.getByIndex(index).bitboard;
							break;
						case 'N':
							pos.whiteKnights |= Bitboard.Square.getByIndex(index).bitboard;
							break;
						case 'P':
							pos.whitePawns |= Bitboard.Square.getByIndex(index).bitboard;
							break;
						case 'k':
							pos.blackKing = Bitboard.Square.getByIndex(index).bitboard;
							break;
						case 'q':
							pos.blackQueens |= Bitboard.Square.getByIndex(index).bitboard;
							break;
						case 'r':
							pos.blackRooks |= Bitboard.Square.getByIndex(index).bitboard;
							break;
						case 'b':
							pos.blackBishops |= Bitboard.Square.getByIndex(index).bitboard;
							break;
						case 'n':
							pos.blackKnights |= Bitboard.Square.getByIndex(index).bitboard;
							break;
						case 'p':
							pos.blackPawns |= Bitboard.Square.getByIndex(index).bitboard;
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
			pos.whiteCastlingRights = CastlingRights.NONE.ind;
			pos.blackCastlingRights = CastlingRights.NONE.ind;
		}
		if (castling.length() < 1 || castling.length() > 4)
			throw new ChessParseException("Invalid length");
		if (castling.contains("K"))
			pos.whiteCastlingRights = castling.contains("Q") ? CastlingRights.ALL.ind : CastlingRights.SHORT.ind;
		else if (castling.contains("Q"))
			pos.whiteCastlingRights = CastlingRights.LONG.ind;
		else
			pos.whiteCastlingRights = CastlingRights.NONE.ind;
		if (castling.contains("k"))
			pos.blackCastlingRights = castling.contains("q") ? CastlingRights.ALL.ind : CastlingRights.SHORT.ind;
		else if (castling.contains("q"))
			pos.blackCastlingRights = CastlingRights.LONG.ind;
		else
			pos.blackCastlingRights = CastlingRights.NONE.ind;
		if (enPassant.length() > 2)
			throw new ChessParseException();
		if (enPassant.equals("-")) {
			pos.enPassantRights = enPassant.equals("-") ? EnPassantRights.NONE.ind :
					EnPassantRights.values()[enPassant.toLowerCase().charAt(0) - 'a'].ind;
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
	Position(Position pos) {
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
	long getWhiteKing() {
		return whiteKing;
	}
	/**
	 * @return A bitboard for the white queens.
	 */
	long getWhiteQueens() {
		return whiteQueens;
	}
	/**
	 * @return A bitboard for the white rooks.
	 */
	long getWhiteRooks() {
		return whiteRooks;
	}
	/**
	 * @return A bitboard for the white bishops.
	 */
	long getWhiteBishops() {
		return whiteBishops;
	}
	/**
	 * @return A bitboard for the white knights.
	 */
	long getWhiteKnights() {
		return whiteKnights;
	}
	/**
	 * @return A bitboard for the white pawns.
	 */
	long getWhitePawns() {
		return whitePawns;
	}
	/**
	 * @return A bitboard for the black king.
	 */
	long getBlackKing() {
		return blackKing;
	}
	/**
	 * @return A bitboard for the black queens.
	 */
	long getBlackQueens() {
		return blackQueens;
	}
	/**
	 * @return A bitboard for the black rooks.
	 */
	long getBlackRooks() {
		return blackRooks;
	}
	/**
	 * @return A bitboard for the black bishops.
	 */
	long getBlackBishops() {
		return blackBishops;
	}
	/**
	 * @return A bitboard for the black knights.
	 */
	long getBlackKnights() {
		return blackKnights;
	}
	/**
	 * @return A bitboard for the black pawns.
	 */
	long getBlackPawns() {
		return blackPawns;
	}
	/**
	 * @return A bitboard for all squares occupied by white pieces.
	 */
	long getAllWhiteOccupied() {
		return allWhiteOccupied;
	}
	/**
	 * @return A bitboard for all squares occupied by black pieces.
	 */
	long getAllBlackOccupied() {
		return allBlackOccupied;
	}
	/**
	 * @return A bitboard for all empty squares.
	 */
	long getAllEmpty() {
		return allEmpty;
	}
	/**
	 * @return A bitboard for all pieces of the color to move checking the opponent's king.
	 */
	long getCheckers() {
		return checkers;
	}
	/**
	 * @return Whether the side to move is in check.
	 */
	boolean isInCheck() {
		return inCheck;
	}
	/**
	 * @return Whether it is white's turn to make a move.
	 */
	boolean isWhitesTurn() {
		return whitesTurn;
	}
	/**
	 * @param sqrInd The index of the square.
	 * @return The piece occupying the square denoted by the specified index.
	 */
	byte getPiece(int sqrInd) {
		return squares[sqrInd];
	}
	/**
	 * @return The number of half moves made.
	 */
	int getHalfMoveIndex() {
		return halfMoveIndex;
	}
	/**
	 * @return The number of half moves made since the last pawn move or capture.
	 */
	byte getFiftyMoveRuleClock() {
		return fiftyMoveRuleClock;
	}
	/**
	 * @return The index denoting the file on which en passant is possible.
	 */
	byte getEnPassantRights() {
		return enPassantRights;
	}
	/**
	 * @return The castling type that denotes to what extent it would still be possible to castle for white regardless
	 * of whether it is actually legally executable in the current position.
	 */
	byte getWhiteCastlingRights() {
		return whiteCastlingRights;
	}
	/**
	 * @return The castling type that denotes to what extent it would still be possible to castle for black regardless
	 * of whether it is actually legally executable in the current position.
	 */
	byte getBlackCastlingRights() {
		return blackCastlingRights;
	}
	/**
	 * @return A Zobrist key that is fairly close to a unique representation of the state of the instance in one 64
	 * bit number.
	 */
	long getKey() {
		return key;
	}
	/**
	 * @return A queue of all the moves made so far.
	 */
	ArrayDeque<Move> getMoveHistory() {
		return new ArrayDeque<>(moveHistory);
	}
	/**
	 * @param numberOfTimes The hypothetical number of times the position has occurred before. E.g. for a three-fold
	 * repetition check, it would be 2.
	 * @return Whether the current position has already occurred before at least the specified number of times.
	 */
	boolean hasRepeated(int numberOfTimes) {
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
	long getAllNonWhiteOccupied() {
		return ~allWhiteOccupied;
	}
	/**
	 * @return A bitboard for all squares not occupied by black pieces.
	 */
	long getAllNonBlackOccupied() {
		return ~allBlackOccupied;
	}
	/**
	 * @return A bitboard for all occupied squares.
	 */
	long getAllOccupied() {
		return ~allEmpty;
	}
	/**
	 * @return The number of pieces on the board
	 */
	int getNumberOfPieces() {
		return BitOperations.hammingWeight(~allEmpty);
	}
	/**
	 * @return The last move made.
	 */
	Move getLastMove() {
		return moveHistory.peekFirst();
	}
	/**
	 * @return The last position state.
	 */
	PositionStateRecord getUnmakeRegister() {
		return stateHistory.peekFirst();
	}
	void makeMove(Move move) {

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
					fen += Piece.getByNumericNotation(piece).letter;
				}
			}
			if (emptyCount != 0)
				fen += emptyCount;
			if (i != 0)
				fen += "/";
		}
		fen += " " + (whitesTurn ? "w" : "b") + " ";
		fen += CastlingRights.toFEN(CastlingRights.getByIndex(whiteCastlingRights),
				CastlingRights.getByIndex(blackCastlingRights)) + " ";
		fen += EnPassantRights.getByIndex(enPassantRights).toString();
		if (enPassantRights != EnPassantRights.NONE.ind)
			fen += (whitesTurn ? 6 : 3) + " ";
		fen += fiftyMoveRuleClock + " " + (1 + halfMoveIndex/2);
		return fen;
	}

}
