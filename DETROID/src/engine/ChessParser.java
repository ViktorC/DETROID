package engine;

import java.text.SimpleDateFormat;

import util.BitOperations;
import util.List;
import util.Queue;
import engine.Board.File;
import engine.Board.Rank;
import engine.Board.Square;
import engine.Game.State;

/**
 * A static class for parsing chess Strings of different notation systems such as FEN, PGN, SAN, PACN, etc.
 * 
 * @author Viktor
 *
 */
public class ChessParser {
	
	/**
	 * A FEN string for the starting chess position.
	 */
	public final static String INITIAL_POSITION_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	
	private ChessParser() {
		
	}
	/**
	 * It parses a FEN-String and sets the instance fields accordingly.
	 * Beside standard six-field FEN-Strings, it also accepts four-field Strings without the fifty-move rule clock and the move index.
	 * 
	 * @param fen
	 * @return
	 * @throws ChessParseException 
	 */
	public static Position parseFEN(String fen) throws ChessParseException {
		Position pos = new Position();
		String[] fenFields = fen.split(" "), ranks;
		String board, turn, castling, enPassant, rank;
		char piece;
		int pieceNum, index = 0, fiftyMoveRuleClock, moveIndex;
		CastlingRights[] castlingRights;
		EnPassantRights enPassantRights;
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
				if (fiftyMoveRuleClock >= 0)
					pos.fiftyMoveRuleClock = (short)fiftyMoveRuleClock;
				else
					pos.fiftyMoveRuleClock = 0;
			}
			catch (NumberFormatException e) {
				throw new ChessParseException("The fifty-move rule clock field of the FEN-string does not conform to the standards. " +
						"Parsing not possible.");
			}
			try {
				moveIndex = (Integer.parseInt(fenFields[5]) - 1)*2;
				if (!pos.isWhitesTurn)
					moveIndex++;
				if (moveIndex >= 0)
					pos.halfMoveIndex = moveIndex;
				else
					pos.halfMoveIndex = 0;
			}
			catch (NumberFormatException e) {
				throw new ChessParseException("The move index field does not conform to the standards. Parsing not possible.");
			}
		}
		else if (fenFields.length != 4)
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
					pos.offsetBoard[index] = parsePieceLN(piece).ind;
					switch (piece) {
						case 'K': {
							pos.whiteKing = Square.getByIndex(index).bitmap;
						}
						break;
						case 'Q': {
							pos.whiteQueens	|= Square.getByIndex(index).bitmap;
						}
						break;
						case 'R': {
							pos.whiteRooks |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'B': {
							pos.whiteBishops |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'N': {
							pos.whiteKnights |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'P': {
							pos.whitePawns |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'k': {
							pos.blackKing = Square.getByIndex(index).bitmap;
						}
						break;
						case 'q': {
							pos.blackQueens |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'r': {
							pos.blackRooks |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'b': {
							pos.blackBishops |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'n': {
							pos.blackKnights |= Square.getByIndex(index).bitmap;
						}
						break;
						case 'p': {
							pos.blackPawns |= Square.getByIndex(index).bitmap;
						}
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
		castlingRights = parseFENCastlingRights(castling);
		pos.whiteCastlingRights = castlingRights[0] != null ? castlingRights[0].ind : pos.whiteCastlingRights;
		pos.blackCastlingRights = castlingRights[1] != null ? castlingRights[1].ind : pos.blackCastlingRights;
		enPassantRights = parseFENEnPassantRights(enPassant);
		pos.enPassantRights = enPassantRights != null ? enPassantRights.ind : pos.enPassantRights;
		pos.checkers = pos.getCheckers();
		pos.isInCheck = pos.checkers != 0;
		pos.gen.setHashKeys(pos);
		pos.keyHistory[0] = pos.key;
		pos.pawnKeyHistory[0] = pos.pawnKey;
		pos.repetitions = 0;
		return pos;
	}
	/**
	 * Parses a game in PGN notation and returns a Game instance.
	 * 
	 * @param pgn
	 * @return
	 * @throws ChessParseException
	 */
	public static Game parsePGN(String pgn) throws ChessParseException {
		char tagChar;
		String tagContent, tagType, tagValue,
			event = null, site = null, date = null, round = null,
			whiteName = null, blackName = null, result = null, fen = null;
		int moveDescStartInd = 0;
		Game out = new Game();
		SimpleDateFormat dF;
		String[] moveDescParts;
		List<String> sanStrings = new Queue<>();
		Move move;
		if (pgn == null)
			return null;
		try {
			for (int i = 0; i < pgn.length(); i++) {
				if (pgn.charAt(i) == '[') {
					tagContent = "";
					while (++i < pgn.length() && (tagChar = pgn.charAt(i)) != ']')
						tagContent += tagChar;
					moveDescStartInd = i + 1;
					tagType = tagContent.substring(0, tagContent.indexOf(' '));
					tagValue = tagContent.substring(tagContent.indexOf('"') + 1, tagContent.lastIndexOf('"'));
					switch (tagType.toUpperCase()) {
						case "EVENT":
							event = tagValue;
						break;
						case "SITE":
							site = tagValue;
						break;
						case "DATE":
							date = tagValue;
						break;
						case "ROUND":
							round = tagValue;
						break;
						case "WHITE":
							whiteName = tagValue;
						break;
						case "BLACK":
							blackName = tagValue;
						break;
						case "RESULT":
							result = tagValue;
						break;
						case "FEN":
							fen = tagValue;
					}
				}
			}
			if (event == null || site == null || date == null || round == null ||
				whiteName == null || blackName == null || result == null)
				throw new ChessParseException("Missing tag(s).");
			out.event = event;
			out.site = site;
			dF = new SimpleDateFormat("yyyy.MM.dd");
			dF.setLenient(false);
			out.date = dF.parse(date);
			out.round = Short.parseShort(round);
			out.whitePlayerName = whiteName;
			out.blackPlayerName = blackName;
			out.state = parsePGNResultTag(result);
			out.position = fen == null ? new Position() : ChessParser.parseFEN(fen);
			if (moveDescStartInd < pgn.length())
				pgn = pgn.substring(moveDescStartInd);
			pgn = pgn.trim();
			pgn = pgn.replaceAll(";[.]*\\n", "");
			pgn = pgn.replaceAll("\\([^)]*\\)", "");
			pgn = pgn.replaceAll("\\{[^\\}]*\\}", "");
			if (out.state != State.IN_PROGRESS) {
				pgn = pgn.replaceAll("(1/2-1/2)|(1-0)|(0-1)", "");
			}
			moveDescParts = pgn.split("[\\s]+");
			for (String s : moveDescParts) {
				if (!s.matches("^[0-9]+.$") && !s.matches("^\\$[0-9]+$"))
					sanStrings.add(s);
			}
			while (sanStrings.hasNext()) {
				move = parseSAN(out.position, sanStrings.next());
				out.position.makeMove(move);
			}
		}
		catch (Exception e) {
			throw new ChessParseException(e);
		}
		return out;
	}
	/**
	 * Parses a String describing a move in Standard Algebraic Notation and returns a Move object created based on it. The move described by
	 * the SAN string is assumed to be legal in the position it is parsed for.
	 * 
	 * @param pos
	 * @param san
	 * @return
	 * @throws ChessParseException
	 * @throws NullPointerException
	 */
	public static Move parseSAN(Position pos, String san) throws ChessParseException, NullPointerException {
		byte from, to, movedPiece, capturedPiece, type;
		long movablePieces, restriction, pawnAdvancer;
		char[] chars;
		MoveSetDatabase mT;
		if (san == null)
			return null;
		try {
			chars = san.toCharArray();
			movablePieces = ~pos.getPinnedPieces(pos.isWhitesTurn);
			if (san.matches("^O-O[+#]?[//?!]{0,2}$")) {
				if (pos.isWhitesTurn) {
					to = Square.G1.ind;
					from = Square.E1.ind;
					movedPiece = Piece.W_KING.ind;
				}
				else {
					to = Square.G8.ind;
					from = Square.E8.ind;
					movedPiece = Piece.B_KING.ind;
				}
				capturedPiece = Piece.NULL.ind;
				type = MoveType.SHORT_CASTLING.ind;
				restriction = -1L;
			}
			else if (san.matches("^O-O-O[+#]?[//?!]{0,2}$")) {
				if (pos.isWhitesTurn) {
					to = Square.C1.ind;
					from = Square.E1.ind;
					movedPiece = Piece.W_KING.ind;
				}
				else {
					to = Square.C8.ind;
					from = Square.E8.ind;
					movedPiece = Piece.B_KING.ind;
				}
				capturedPiece = Piece.NULL.ind;
				type = MoveType.LONG_CASTLING.ind;
				restriction = -1L;
			}
			else if (san.matches("^[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[0] - 'a') + 8*(Integer.parseInt(Character.toString(chars[1])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (pos.isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					pawnAdvancer = (1L << (to - 8)) & pos.whitePawns & movablePieces;
					from = (byte)((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to - 16);
				}
				else {
					movedPiece = Piece.B_PAWN.ind;
					pawnAdvancer = (1L << (to + 8)) & pos.blackPawns & movablePieces;
					from = (byte)((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to + 16);
				}
				capturedPiece = Piece.NULL.ind;
				type = MoveType.NORMAL.ind;
				restriction = -1L;
			}
			else if (san.matches("^[a-h][1-8]=[QRBN][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[0] - 'a') + 8*(Integer.parseInt(Character.toString(chars[1])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (pos.isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					pawnAdvancer = (1L << (to - 8)) & pos.whitePawns & movablePieces;
					from = (byte)((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to - 16);
				}
				else {
					movedPiece = Piece.B_PAWN.ind;
					pawnAdvancer = (1L << (to + 8)) & pos.blackPawns & movablePieces;
					from = (byte)((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to + 16);
				}
				capturedPiece = Piece.NULL.ind;
				type = (byte)(parsePieceLN(chars[3]).ind + 2);
				restriction = -1L;
			}
			else if (san.matches("^x[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[1] - 'a') + 8*(Integer.parseInt(Character.toString(chars[2])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (pos.isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getBlackPawnCaptureSet(pos.whitePawns & movablePieces));
					if (pos.enPassantRights != EnPassantRights.NONE.ind && to == pos.enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND) {
						capturedPiece = Piece.B_PAWN.ind;
						type = MoveType.EN_PASSANT.ind;
					}
					else {
						capturedPiece = pos.offsetBoard[to];
						type = MoveType.NORMAL.ind;
					}
				}
				else {
					movedPiece = Piece.B_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getWhitePawnCaptureSet(pos.blackPawns & movablePieces));
					if (pos.enPassantRights != EnPassantRights.NONE.ind && to == pos.enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND) {
						capturedPiece = Piece.W_PAWN.ind;
						type = MoveType.EN_PASSANT.ind;
					}
					else {
						capturedPiece = pos.offsetBoard[to];
						type = MoveType.NORMAL.ind;
					}
				}
				restriction = -1L;
			}
			else if (san.matches("^x[a-h][1-8]=[QRBN][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[1] - 'a') + 8*(Integer.parseInt(Character.toString(chars[2])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (pos.isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getBlackPawnCaptureSet(pos.whitePawns & movablePieces));
				}
				else {
					movedPiece = Piece.B_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getWhitePawnCaptureSet(pos.blackPawns & movablePieces));
				}
				capturedPiece = pos.offsetBoard[to];
				type = (byte)(parsePieceLN(chars[4]).ind + 2);
				restriction = -1L;
			}
			else if (san.matches("^x[a-h][1-8]e.p.[+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[1] - 'a') + 8*(Integer.parseInt(Character.toString(chars[2])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (pos.isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getBlackPawnCaptureSet(pos.whitePawns & movablePieces));
					capturedPiece = Piece.B_PAWN.ind;
				}
				else {
					movedPiece = Piece.B_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getWhitePawnCaptureSet(pos.blackPawns & movablePieces));
					capturedPiece = Piece.W_PAWN.ind;
				}
				type = MoveType.EN_PASSANT.ind;
				restriction = -1L;
			}
			else if (san.matches("^[a-h]x[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (pos.isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getBlackPawnCaptureSet(pos.whitePawns & movablePieces)
							& File.getByIndex((int)(chars[0] - 'a')).bitmap);
					if (pos.enPassantRights != EnPassantRights.NONE.ind && to == pos.enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND) {
						capturedPiece = Piece.B_PAWN.ind;
						type = MoveType.EN_PASSANT.ind;
					}
					else {
						capturedPiece = pos.offsetBoard[to];
						type = MoveType.NORMAL.ind;
					}
				}
				else {
					movedPiece = Piece.B_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getWhitePawnCaptureSet(pos.blackPawns & movablePieces)
							& File.getByIndex((int)(chars[0] - 'a')).bitmap);
					if (pos.enPassantRights != EnPassantRights.NONE.ind && to == pos.enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND) {
						capturedPiece = Piece.W_PAWN.ind;
						type = MoveType.EN_PASSANT.ind;
					}
					else {
						capturedPiece = pos.offsetBoard[to];
						type = MoveType.NORMAL.ind;
					}
				}
				restriction = -1L;
			}
			else if (san.matches("^[a-h]x[a-h][1-8]=[QRBN][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (pos.isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getBlackPawnCaptureSet(pos.whitePawns & movablePieces)
							& File.getByIndex((int)(chars[0] - 'a')).bitmap);
				}
				else {
					movedPiece = Piece.B_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getWhitePawnCaptureSet(pos.blackPawns & movablePieces)
							& File.getByIndex((int)(chars[0] - 'a')).bitmap);
				}
				capturedPiece = pos.offsetBoard[to];
				type = (byte)(parsePieceLN(chars[5]).ind + 2);
				restriction = -1L;
			}
			else if (san.matches("^[a-h]x[a-h][1-8]e.p.[+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				mT = MoveSetDatabase.getByIndex(to);
				if (pos.isWhitesTurn) {
					movedPiece = Piece.W_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getBlackPawnCaptureSet(pos.whitePawns & movablePieces)
							& File.getByIndex((int)(chars[0] - 'a')).bitmap);
					capturedPiece = Piece.B_PAWN.ind;
				}
				else {
					movedPiece = Piece.B_PAWN.ind;
					from = BitOperations.indexOfBit(mT.getWhitePawnCaptureSet(pos.blackPawns & movablePieces)
							& File.getByIndex((int)(chars[0] - 'a')).bitmap);
					capturedPiece = Piece.W_PAWN.ind;
				}
				type = MoveType.EN_PASSANT.ind;
				restriction = -1L;
			}
			else if (san.matches("^[KQRBN][a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[1] - 'a') + 8*(Integer.parseInt(Character.toString(chars[2])) - 1));
				movedPiece = (byte)(parsePieceLN(chars[0]).ind + (pos.isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = Piece.NULL.ind;
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = -1L;
			}
			else if (san.matches("^[KQRBN][a-h][a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				movedPiece = (byte)(parsePieceLN(chars[0]).ind + (pos.isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = Piece.NULL.ind;
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = File.getByIndex((int)(chars[1] - 'a')).bitmap;
			}
			else if (san.matches("^[KQRBN][1-8][a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				movedPiece = (byte)(parsePieceLN(chars[0]).ind + (pos.isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = Piece.NULL.ind;
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = Rank.getByIndex((int)(chars[1] - '1')).bitmap;
			}
			else if (san.matches("^[KQRBN][a-h][1-8][a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[3] - 'a') + 8*(Integer.parseInt(Character.toString(chars[4])) - 1));
				movedPiece = (byte)(parsePieceLN(chars[0]).ind + (pos.isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = Piece.NULL.ind;
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = File.getByIndex((int)(chars[1] - 'a')).bitmap & Rank.getByIndex((int)(chars[2] - '1')).bitmap;
			}
			else if (san.matches("^[KQRBN]x[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				movedPiece = (byte)(parsePieceLN(chars[0]).ind + (pos.isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = pos.offsetBoard[to];
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = -1L;
			}
			else if (san.matches("^[KQRBN][a-h]x[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[3] - 'a') + 8*(Integer.parseInt(Character.toString(chars[4])) - 1));
				movedPiece = (byte)(parsePieceLN(chars[0]).ind + (pos.isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = pos.offsetBoard[to];
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = File.getByIndex((int)(chars[1] - 'a')).bitmap;
			}
			else if (san.matches("^[KQRBN][1-8]x[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[3] - 'a') + 8*(Integer.parseInt(Character.toString(chars[4])) - 1));
				movedPiece = (byte)(parsePieceLN(chars[0]).ind + (pos.isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = pos.offsetBoard[to];
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = Rank.getByIndex((int)(chars[1] - '1')).bitmap;
			}
			else if (san.matches("^[KQRBN][a-h][1-8]x[a-h][1-8][+#]?[//?!]{0,2}$")) {
				to = (byte)((int)(chars[4] - 'a') + 8*(Integer.parseInt(Character.toString(chars[5])) - 1));
				movedPiece = (byte)(parsePieceLN(chars[0]).ind + (pos.isWhitesTurn ? 0 : Piece.W_PAWN.ind));
				capturedPiece = pos.offsetBoard[to];
				type = MoveType.NORMAL.ind;
				from = -1;
				restriction = File.getByIndex((int)(chars[1] - 'a')).bitmap & Rank.getByIndex((int)(chars[2] - '1')).bitmap;
			}
			else
				throw new ChessParseException("The move String violates the SAN standard.");
			if (from == -1) {
				mT = MoveSetDatabase.getByIndex(to);
				if (movedPiece == Piece.W_KING.ind)
					from = BitOperations.indexOfBit(mT.getKingMoveSet(pos.whiteKing));
				else if (movedPiece == Piece.W_QUEEN.ind)
					from = BitOperations.indexOfBit(mT.getQueenMoveSet(pos.whiteQueens & movablePieces & restriction, pos.allOccupied));
				else if (movedPiece == Piece.W_ROOK.ind)
					from = BitOperations.indexOfBit(mT.getRookMoveSet(pos.whiteRooks & movablePieces & restriction, pos.allOccupied));
				else if (movedPiece == Piece.W_BISHOP.ind)
					from = BitOperations.indexOfBit(mT.getBishopMoveSet(pos.whiteBishops & movablePieces & restriction, pos.allOccupied));
				else if (movedPiece == Piece.W_KNIGHT.ind)
					from = BitOperations.indexOfBit(mT.getKnightMoveSet(pos.whiteKnights & movablePieces & restriction));
				else if (movedPiece == Piece.B_KING.ind)
					from = BitOperations.indexOfBit(mT.getKingMoveSet(pos.blackKing));
				else if (movedPiece == Piece.B_QUEEN.ind)
					from = BitOperations.indexOfBit(mT.getQueenMoveSet(pos.blackQueens & movablePieces & restriction, pos.allOccupied));
				else if (movedPiece == Piece.B_ROOK.ind)
					from = BitOperations.indexOfBit(mT.getRookMoveSet(pos.blackRooks & movablePieces & restriction, pos.allOccupied));
				else if (movedPiece == Piece.B_BISHOP.ind)
					from = BitOperations.indexOfBit(mT.getBishopMoveSet(pos.blackBishops & movablePieces & restriction, pos.allOccupied));
				else
					from = BitOperations.indexOfBit(mT.getKnightMoveSet(pos.blackKnights & movablePieces & restriction));
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
	 * @param pos
	 * @param pacn
	 * @return
	 * @throws ChessParseException
	 * @throws NullPointerException
	 */
	public static Move parsePACN(Position pos, String pacn) throws ChessParseException, NullPointerException {
		byte from, to, movedPiece, capturedPiece, type;
		String input = pacn.trim().toLowerCase();
		if (input.length() != 4 && input.length() != 6)
			throw new ChessParseException("The input does not pass the formal requirements of a PACN String. Its length is neither 4 nor 6");
		from = (byte)((int)(input.charAt(0) - 'a') + 8*(Integer.parseInt(Character.toString(input.charAt(1))) - 1));
		to = (byte)((int)(input.charAt(2) - 'a') + 8*(Integer.parseInt(Character.toString(input.charAt(3))) - 1));
		movedPiece = pos.offsetBoard[from];
		if (input.length() == 6) {
			switch (input.charAt(5)) {
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
			capturedPiece = pos.offsetBoard[to];
		}
		else {
			if (movedPiece == Piece.W_PAWN.ind) {
				if (pos.enPassantRights != EnPassantRights.NONE.ind && to == pos.enPassantRights + EnPassantRights.TO_W_DEST_SQR_IND) {
					type = MoveType.EN_PASSANT.ind;
					capturedPiece = Piece.B_PAWN.ind;
				}
				else {
					type = MoveType.NORMAL.ind;
					capturedPiece = pos.offsetBoard[to];
				}
			}
			else if (movedPiece == Piece.B_PAWN.ind) {
				if (pos.enPassantRights != EnPassantRights.NONE.ind && to == pos.enPassantRights + EnPassantRights.TO_B_DEST_SQR_IND) {
					type = MoveType.EN_PASSANT.ind;
					capturedPiece = Piece.W_PAWN.ind;
				}
				else {
					type = MoveType.NORMAL.ind;
					capturedPiece = pos.offsetBoard[to];
				}
			}
			else {
				if ((from == Square.E1.ind && to == Square.G1.ind) || (from == Square.E8.ind && to == Square.G8.ind)) {
					type = MoveType.SHORT_CASTLING.ind;
					capturedPiece = Piece.NULL.ind;
				}
				else if ((from == Square.E1.ind && to == Square.C1.ind) || (from == Square.E8.ind && to == Square.C8.ind)) {
					type = MoveType.LONG_CASTLING.ind;
					capturedPiece = Piece.NULL.ind;
				}
				else {
					type = MoveType.NORMAL.ind;
					capturedPiece = pos.offsetBoard[to];
				}
			}
		}
		return new Move(from, to, movedPiece, capturedPiece, type);
	}
	/**
	 * Returns the enumeration of the square specified by a String in algebraic notation consisting of its file and rank.
	 * 
	 * @param square
	 * @return
	 * @throws ChessParseException 
	 */
	public static Square parseSquareAN(String square) throws ChessParseException {
		if (square.length() == 2) {
			square = square.toLowerCase();
			int out = 8*(square.charAt(0) - 'a') + (square.charAt(1) - '1');
			if (out >= 0 && out < 64)
				return Square.getByIndex(out);
		}
		throw new ChessParseException();
	}
	/**
	 * Returns the piece defined by the FEN piece letter notation input parameter's numeric equivalent according to {@link #engine.Piece Piece}.
	 * 
	 * @param piece
	 * @return
	 * @throws ChessParseException 
	 */
	private static Piece parsePieceLN(char piece) throws ChessParseException {
		switch (piece) {
			case '\u0000':
				return Piece.NULL;
			case 'K':
				return Piece.W_KING;
			case 'Q':
				return Piece.W_QUEEN;
			case 'R':
				return Piece.W_ROOK;
			case 'B':
				return Piece.W_BISHOP;
			case 'N':
				return Piece.W_KNIGHT;
			case 'P':
				return Piece.W_PAWN;
			case 'k':
				return Piece.B_KING;
			case 'q':
				return Piece.B_QUEEN;
			case 'r':
				return Piece.B_ROOK;
			case 'b':
				return Piece.B_BISHOP;
			case 'n':
				return Piece.B_KNIGHT;
			case 'p':
				return Piece.B_PAWN;
			default:
				throw new ChessParseException();
		}
	}
	/**
	 * Parses the result tag field's value of a game in PGN and returns the equivalent state.
	 * 
	 * @param tagValue
	 * @return
	 * @throws ChessParseException 
	 */
	private static State parsePGNResultTag(String tagValue) throws ChessParseException {
		switch (tagValue) {
			case "1-0":
				return State.WHITE_WIN;
			case "0-1":
				return State.BLACK_WIN;
			case "1/2-1/2":
				return State.DRAW;
			case "*":
				return State.IN_PROGRESS;
			default:
				throw new ChessParseException();
		}
	}
	/**
	 * Parses a String of castling rights in FEN notation and returns an array of two containing white's and black's castling rights respectively.
	 * 
	 * @param fen
	 * @return
	 * @throws NullPointerException
	 * @throws ChessParseException 
	 */
	private static CastlingRights[] parseFENCastlingRights(String fen) throws ChessParseException, NullPointerException {
		if (fen.equals("-"))
			return new CastlingRights[] { CastlingRights.NONE, CastlingRights.NONE };
		if (fen.length() < 1 || fen.length() > 4)
			throw new ChessParseException("Invalid length");
		CastlingRights whiteCastlingRights, blackCastlingRights;
		if (fen.contains("K")) {
			if (fen.contains("Q"))
				whiteCastlingRights = CastlingRights.ALL;
			else
				whiteCastlingRights = CastlingRights.SHORT;
		}
		else if (fen.contains("Q"))
			whiteCastlingRights = CastlingRights.LONG;
		else
			whiteCastlingRights = CastlingRights.NONE;
		if (fen.contains("k")) {
			if (fen.contains("q"))
				blackCastlingRights = CastlingRights.ALL;
			else
				blackCastlingRights = CastlingRights.SHORT;
		}
		else if (fen.contains("q"))
			blackCastlingRights = CastlingRights.LONG;
		else
			blackCastlingRights = CastlingRights.NONE;
		return new CastlingRights[] { whiteCastlingRights, blackCastlingRights };
	}
	/**
	 * Parses a string in FEN notation and returns an EnPassantRights type.
	 * 
	 * @param fen
	 * @return
	 * @throws ChessParseException
	 * @throws NullPointerException
	 */
	private static EnPassantRights parseFENEnPassantRights(String fen) throws ChessParseException, NullPointerException {
		if (fen.length() > 2)
			throw new ChessParseException();
		if (fen.equals("-"))
			return EnPassantRights.NONE;
		return EnPassantRights.values()[fen.toLowerCase().charAt(0) - 'a'];
	}
}
