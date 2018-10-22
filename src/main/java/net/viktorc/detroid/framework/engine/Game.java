package net.viktorc.detroid.framework.engine;

import java.util.*;

import net.viktorc.detroid.framework.util.BitOperations;
import net.viktorc.detroid.framework.validation.GameState;

/**
 * A class representing a chess game. It can parse PGN strings and moves in both FEN and SAN format. It also allows
 * for playing and unplaying moves while keeping track of the state of the game.
 * 
 * @author Viktor
 *
 */
public class Game {
	
	private Position startPosition;
	private Position position;
	private String event;
	private String site;
	private String date;
	private int round;
	private String whitePlayerName;
	private String blackPlayerName;
	private GameState state;

	private static Move parsePACN(Position pos, String pacn) throws ChessParseException {
		String input = pacn.trim().toLowerCase();
		if (input.length() != 4 && input.length() != 5) {
			throw new ChessParseException("The input does not pass the formal requirements of a PACN String. Its " +
					"length is neither 4 nor 5");
		}
		byte from = (byte) ((input.charAt(0) - 'a') + 8*(Integer.parseInt(Character.toString(input.charAt(1))) - 1));
		byte to = (byte) ((input.charAt(2) - 'a') + 8*(Integer.parseInt(Character.toString(input.charAt(3))) - 1));
		byte movedPiece = pos.getPiece(from);
		byte capturedPiece;
		byte type;
		if (input.length() == 5) {
			switch (input.charAt(4)) {
				case 'q' :
					type = (byte) MoveType.PROMOTION_TO_QUEEN.ordinal();
					break;
				case 'r' :
					type = (byte) MoveType.PROMOTION_TO_ROOK.ordinal();
					break;
				case 'b' :
					type = (byte) MoveType.PROMOTION_TO_BISHOP.ordinal();
					break;
				case 'n' :
					type = (byte) MoveType.PROMOTION_TO_KNIGHT.ordinal();
					break;
				default:
					throw new ChessParseException("The input does not pass the formal requirements of a PACN String. " +
							"Wrong promotion notation");
			}
			capturedPiece = pos.getPiece(to);
		} else {
			if (movedPiece == Piece.W_PAWN.ordinal()) {
				if (pos.getEnPassantRights() != EnPassantRights.NONE.ordinal() &&
						to == pos.getEnPassantRights() + EnPassantRights.TO_W_DEST_SQR_IND) {
					type = (byte) MoveType.EN_PASSANT.ordinal();
					capturedPiece = (byte) Piece.B_PAWN.ordinal();
				} else {
					type = (byte) MoveType.NORMAL.ordinal();
					capturedPiece = pos.getPiece(to);
				}
			} else if (movedPiece == Piece.B_PAWN.ordinal()) {
				if (pos.getEnPassantRights() != EnPassantRights.NONE.ordinal() &&
						to == pos.getEnPassantRights() + EnPassantRights.TO_B_DEST_SQR_IND) {
					type = (byte) MoveType.EN_PASSANT.ordinal();
					capturedPiece = (byte) Piece.W_PAWN.ordinal();
				} else {
					type = (byte) MoveType.NORMAL.ordinal();
					capturedPiece = pos.getPiece(to);
				}
			} else {
				if ((movedPiece == Piece.W_KING.ordinal() && from == Bitboard.Square.E1.ordinal() &&
						to == Bitboard.Square.G1.ordinal()) || (movedPiece == Piece.B_KING.ordinal() &&
						from == Bitboard.Square.E8.ordinal() && to == Bitboard.Square.G8.ordinal())) {
					type = (byte) MoveType.SHORT_CASTLING.ordinal();
					capturedPiece = (byte) Piece.NULL.ordinal();
				} else if ((movedPiece == Piece.W_KING.ordinal() && from == Bitboard.Square.E1.ordinal() &&
						to == Bitboard.Square.C1.ordinal()) || (movedPiece == Piece.B_KING.ordinal() &&
						from == Bitboard.Square.E8.ordinal() && to == Bitboard.Square.C8.ordinal())) {
					type = (byte) MoveType.LONG_CASTLING.ordinal();
					capturedPiece = (byte) Piece.NULL.ordinal();
				} else {
					type = (byte) MoveType.NORMAL.ordinal();
					capturedPiece = pos.getPiece(to);
				}
			}
		}
		return new Move(from, to, movedPiece, capturedPiece, type);
	}
	private static Move parseSAN(Position pos, String san) throws ChessParseException, NullPointerException {
		byte from, to, movedPiece, capturedPiece, type;
		long movablePieces, restriction, pawnAdvancer;
		char[] chars;
		MoveSetBase dB;
		if (san == null)
			return null;
		try {
			movablePieces = 0;
			for (Move m : pos.getMoves())
				movablePieces |= BitOperations.toBit(m.getFrom());
			chars = san.toCharArray();
			if (san.matches("^O-O[+#]?[/?!]{0,2}$")) {
				if (pos.isWhitesTurn()) {
					to = (byte) Bitboard.Square.G1.ordinal();
					from = (byte) Bitboard.Square.E1.ordinal();
					movedPiece = (byte) Piece.W_KING.ordinal();
				} else {
					to = (byte) Bitboard.Square.G8.ordinal();
					from = (byte) Bitboard.Square.E8.ordinal();
					movedPiece = (byte) Piece.B_KING.ordinal();
				}
				capturedPiece = (byte) Piece.NULL.ordinal();
				type = (byte) MoveType.SHORT_CASTLING.ordinal();
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^O-O-O[+#]?[/?!]{0,2}$")) {
				if (pos.isWhitesTurn()) {
					to = (byte) Bitboard.Square.C1.ordinal();
					from = (byte) Bitboard.Square.E1.ordinal();
					movedPiece = (byte) Piece.W_KING.ordinal();
				} else {
					to = (byte) Bitboard.Square.C8.ordinal();
					from = (byte) Bitboard.Square.E8.ordinal();
					movedPiece = (byte) Piece.B_KING.ordinal();
				}
				capturedPiece = (byte) Piece.NULL.ordinal();
				type = (byte) MoveType.LONG_CASTLING.ordinal();
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[a-h][1-8][+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[0] - 'a') + 8*(Integer.parseInt(Character.toString(chars[1])) - 1));
				if (pos.isWhitesTurn()) {
					movedPiece = (byte) Piece.W_PAWN.ordinal();
					pawnAdvancer = (1L << (to - 8)) & pos.getWhitePawns() & movablePieces;
					from = (byte) ((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to - 16);
				} else {
					movedPiece = (byte) Piece.B_PAWN.ordinal();
					pawnAdvancer = (1L << (to + 8)) & pos.getBlackPawns() & movablePieces;
					from = (byte) ((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to + 16);
				}
				capturedPiece = (byte) Piece.NULL.ordinal();
				type = (byte) MoveType.NORMAL.ordinal();
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[a-h][1-8]=[QRBN][+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[0] - 'a') + 8*(Integer.parseInt(Character.toString(chars[1])) - 1));
				if (pos.isWhitesTurn()) {
					movedPiece = (byte) Piece.W_PAWN.ordinal();
					pawnAdvancer = (1L << (to - 8)) & pos.getWhitePawns() & movablePieces;
					from = (byte) ((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to - 16);
				} else {
					movedPiece = (byte) Piece.B_PAWN.ordinal();
					pawnAdvancer = (1L << (to + 8)) & pos.getBlackPawns() & movablePieces;
					from = (byte) ((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to + 16);
				}
				capturedPiece = (byte) Piece.NULL.ordinal();
				type = (byte) (Arrays.stream(Piece.values())
						.filter(p -> p.getLetter() == chars[3]).findAny().get().ordinal() + 2);
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^x[a-h][1-8][+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[1] - 'a') + 8*(Integer.parseInt(Character.toString(chars[2])) - 1));
				dB = MoveSetBase.values()[to];
				if (pos.isWhitesTurn()) {
					movedPiece = (byte) Piece.W_PAWN.ordinal();
					from = BitOperations.indexOfBit(dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces));
					if (pos.getEnPassantRights() != EnPassantRights.NONE.ordinal() &&
							to == pos.getEnPassantRights() + EnPassantRights.TO_W_DEST_SQR_IND) {
						capturedPiece = (byte) Piece.B_PAWN.ordinal();
						type = (byte) MoveType.EN_PASSANT.ordinal();
					} else {
						capturedPiece = pos.getPiece(to);
						type = (byte) MoveType.NORMAL.ordinal();
					}
				} else {
					movedPiece = (byte) Piece.B_PAWN.ordinal();
					from = BitOperations.indexOfBit(dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces));
					if (pos.getEnPassantRights() != EnPassantRights.NONE.ordinal() &&
							to == pos.getEnPassantRights() + EnPassantRights.TO_B_DEST_SQR_IND) {
						capturedPiece = (byte) Piece.W_PAWN.ordinal();
						type = (byte) MoveType.EN_PASSANT.ordinal();
					} else {
						capturedPiece = pos.getPiece(to);
						type = (byte) MoveType.NORMAL.ordinal();
					}
				}
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^x[a-h][1-8]=[QRBN][+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[1] - 'a') + 8*(Integer.parseInt(Character.toString(chars[2])) - 1));
				dB = MoveSetBase.values()[to];
				if (pos.isWhitesTurn()) {
					movedPiece = (byte) Piece.W_PAWN.ordinal();
					from = BitOperations.indexOfBit(dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces));
				} else {
					movedPiece = (byte) Piece.B_PAWN.ordinal();
					from = BitOperations.indexOfBit(dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces));
				}
				capturedPiece = pos.getPiece(to);
				type = (byte) (Arrays.stream(Piece.values())
						.filter(p -> p.getLetter() == chars[4]).findAny().get().ordinal() + 2);
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^x[a-h][1-8]e.p.[+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[1] - 'a') + 8*(Integer.parseInt(Character.toString(chars[2])) - 1));
				dB = MoveSetBase.values()[to];
				if (pos.isWhitesTurn()) {
					movedPiece = (byte) Piece.W_PAWN.ordinal();
					from = BitOperations.indexOfBit(dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces));
					capturedPiece = (byte) Piece.B_PAWN.ordinal();
				} else {
					movedPiece = (byte) Piece.B_PAWN.ordinal();
					from = BitOperations.indexOfBit(dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces));
					capturedPiece = (byte) Piece.W_PAWN.ordinal();
				}
				type = (byte) MoveType.EN_PASSANT.ordinal();
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[a-h]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				dB = MoveSetBase.values()[to];
				if (pos.isWhitesTurn()) {
					movedPiece = (byte) Piece.W_PAWN.ordinal();
					from = BitOperations.indexOfBit(dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces)
							& Bitboard.File.values()[chars[0] - 'a'].getBitboard());
					if (pos.getEnPassantRights() != EnPassantRights.NONE.ordinal() &&
							to == pos.getEnPassantRights() + EnPassantRights.TO_W_DEST_SQR_IND) {
						capturedPiece = (byte) Piece.B_PAWN.ordinal();
						type = (byte) MoveType.EN_PASSANT.ordinal();
					} else {
						capturedPiece = pos.getPiece(to);
						type = (byte) MoveType.NORMAL.ordinal();
					}
				} else {
					movedPiece = (byte) Piece.B_PAWN.ordinal();
					from = BitOperations.indexOfBit(dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces)
							& Bitboard.File.values()[chars[0] - 'a'].getBitboard());
					if (pos.getEnPassantRights() != EnPassantRights.NONE.ordinal() &&
							to == pos.getEnPassantRights() + EnPassantRights.TO_B_DEST_SQR_IND) {
						capturedPiece = (byte) Piece.W_PAWN.ordinal();
						type = (byte) MoveType.EN_PASSANT.ordinal();
					} else {
						capturedPiece = pos.getPiece(to);
						type = (byte) MoveType.NORMAL.ordinal();
					}
				}
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[a-h]x[a-h][1-8]=[QRBN][+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				dB = MoveSetBase.values()[to];
				if (pos.isWhitesTurn()) {
					movedPiece = (byte) Piece.W_PAWN.ordinal();
					from = BitOperations.indexOfBit(dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces)
							& Bitboard.File.values()[chars[0] - 'a'].getBitboard());
				} else {
					movedPiece = (byte) Piece.B_PAWN.ordinal();
					from = BitOperations.indexOfBit(dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces)
							& Bitboard.File.values()[chars[0] - 'a'].getBitboard());
				}
				capturedPiece = pos.getPiece(to);
				type = (byte) (Arrays.stream(Piece.values())
						.filter(p -> p.getLetter() == chars[5]).findAny().get().ordinal() + 2);
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[a-h]x[a-h][1-8]e.p.[+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				dB = MoveSetBase.values()[to];
				if (pos.isWhitesTurn()) {
					movedPiece = (byte) Piece.W_PAWN.ordinal();
					from = BitOperations.indexOfBit(dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces)
							& Bitboard.File.values()[chars[0] - 'a'].getBitboard());
					capturedPiece = (byte) Piece.B_PAWN.ordinal();
				} else {
					movedPiece = (byte) Piece.B_PAWN.ordinal();
					from = BitOperations.indexOfBit(dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces)
							& Bitboard.File.values()[chars[0] - 'a'].getBitboard());
					capturedPiece = (byte) Piece.W_PAWN.ordinal();
				}
				type = (byte) MoveType.EN_PASSANT.ordinal();
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[KQRBN][a-h][1-8][+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[1] - 'a') + 8*(Integer.parseInt(Character.toString(chars[2])) - 1));
				movedPiece = (byte) (Arrays.stream(Piece.values())
						.filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
						(pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
				capturedPiece = (byte) Piece.NULL.ordinal();
				type = (byte) MoveType.NORMAL.ordinal();
				from = -1;
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[KQRBN][a-h][a-h][1-8][+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				movedPiece = (byte) (Arrays.stream(Piece.values())
						.filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
						(pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
				capturedPiece = (byte) Piece.NULL.ordinal();
				type = (byte) MoveType.NORMAL.ordinal();
				from = -1;
				restriction = Bitboard.File.values()[chars[0] - 'a'].getBitboard();
			} else if (san.matches("^[KQRBN][1-8][a-h][1-8][+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				movedPiece = (byte) (Arrays.stream(Piece.values())
						.filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
						(pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
				capturedPiece = (byte) Piece.NULL.ordinal();
				type = (byte) MoveType.NORMAL.ordinal();
				from = -1;
				restriction = Bitboard.Rank.values()[chars[1] - '1'].getBitboard();
			} else if (san.matches("^[KQRBN][a-h][1-8][a-h][1-8][+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[3] - 'a') + 8*(Integer.parseInt(Character.toString(chars[4])) - 1));
				movedPiece = (byte) (Arrays.stream(Piece.values())
						.filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
						(pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
				capturedPiece = (byte) Piece.NULL.ordinal();
				type = (byte) MoveType.NORMAL.ordinal();
				from = -1;
				restriction = Bitboard.File.values()[chars[0] - 'a'].getBitboard() &
						Bitboard.Rank.values()[chars[2] - '1'].getBitboard();
			} else if (san.matches("^[KQRBN]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
				to = (byte) ((int) (chars[2] - 'a') + 8*(Integer.parseInt(Character.toString(chars[3])) - 1));
				movedPiece = (byte) (Arrays.stream(Piece.values())
						.filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
						(pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
				capturedPiece = pos.getPiece(to);
				type = (byte) MoveType.NORMAL.ordinal();
				from = -1;
				restriction = Bitboard.FULL_BOARD;
			} else if (san.matches("^[KQRBN][a-h]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[3] - 'a') + 8*(Integer.parseInt(Character.toString(chars[4])) - 1));
				movedPiece = (byte) (Arrays.stream(Piece.values())
						.filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
						(pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
				capturedPiece = pos.getPiece(to);
				type = (byte) MoveType.NORMAL.ordinal();
				from = -1;
				restriction = Bitboard.File.values()[chars[1] - 'a'].getBitboard();
			} else if (san.matches("^[KQRBN][1-8]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[3] - 'a') + 8*(Integer.parseInt(Character.toString(chars[4])) - 1));
				movedPiece = (byte) (Arrays.stream(Piece.values())
						.filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
						(pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
				capturedPiece = pos.getPiece(to);
				type = (byte) MoveType.NORMAL.ordinal();
				from = -1;
				restriction = Bitboard.Rank.values()[chars[1] - '1'].getBitboard();
			} else if (san.matches("^[KQRBN][a-h][1-8]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
				to = (byte) ((chars[4] - 'a') + 8*(Integer.parseInt(Character.toString(chars[5])) - 1));
				movedPiece = (byte) (Arrays.stream(Piece.values())
						.filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
						(pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
				capturedPiece = pos.getPiece(to);
				type = (byte) MoveType.NORMAL.ordinal();
				from = -1;
				restriction = Bitboard.File.values()[chars[1] - 'a'].getBitboard() & Bitboard.Rank.values()[chars[2] - '1'].getBitboard();
			} else
				throw new ChessParseException("The move String violates the SAN standard.");
			if (from == -1) {
				dB = MoveSetBase.values()[to];
				if (movedPiece == Piece.W_KING.ordinal())
					from = BitOperations.indexOfBit(dB.getKingMoveSet(pos.getWhiteKing()));
				else if (movedPiece == Piece.W_QUEEN.ordinal())
					from = BitOperations.indexOfBit(dB.getQueenMoveSet(pos.getWhiteQueens() & movablePieces & restriction,
							pos.getAllOccupied()));
				else if (movedPiece == Piece.W_ROOK.ordinal())
					from = BitOperations.indexOfBit(dB.getRookMoveSet(pos.getWhiteRooks() & movablePieces & restriction,
							pos.getAllOccupied()));
				else if (movedPiece == Piece.W_BISHOP.ordinal())
					from = BitOperations.indexOfBit(dB.getBishopMoveSet(pos.getWhiteBishops() & movablePieces & restriction,
							pos.getAllOccupied()));
				else if (movedPiece == Piece.W_KNIGHT.ordinal())
					from = BitOperations.indexOfBit(dB.getKnightMoveSet(pos.getWhiteKnights() & movablePieces & restriction));
				else if (movedPiece == Piece.B_KING.ordinal())
					from = BitOperations.indexOfBit(dB.getKingMoveSet(pos.getBlackKing()));
				else if (movedPiece == Piece.B_QUEEN.ordinal())
					from = BitOperations.indexOfBit(dB.getQueenMoveSet(pos.getBlackQueens() & movablePieces & restriction,
							pos.getAllOccupied()));
				else if (movedPiece == Piece.B_ROOK.ordinal())
					from = BitOperations.indexOfBit(dB.getRookMoveSet(pos.getBlackRooks() & movablePieces & restriction,
							pos.getAllOccupied()));
				else if (movedPiece == Piece.B_BISHOP.ordinal())
					from = BitOperations.indexOfBit(dB.getBishopMoveSet(pos.getBlackBishops() & movablePieces & restriction,
							pos.getAllOccupied()));
				else
					from = BitOperations.indexOfBit(dB.getKnightMoveSet(pos.getBlackKnights() & movablePieces & restriction));
			}
		}
		catch (Exception e) {
			throw new ChessParseException(e);
		}
		return new Move(from, to, movedPiece, capturedPiece, type);
	}
	private static String toSAN(Position pos, Move move) {
		if (move == null)
			return null;
		if (move.getType() == MoveType.SHORT_CASTLING.ordinal())
			return "O-O";
		else if (move.getType() == MoveType.LONG_CASTLING.ordinal())
			return "O-O-O";
		String destRank = Integer.toString(move.getTo()/8 + 1);
		String destFile = Character.toString((char) (move.getTo()%8 + 'a'));
		Piece piece = Piece.values()[move.getMovedPiece()];
		String movedPiece;
		if (piece == Piece.W_PAWN || piece == Piece.B_PAWN)
			movedPiece  = "";
		else
			movedPiece  = Character.toString(piece.getLetter()).toUpperCase();
		String capture = move.getCapturedPiece() == Piece.NULL.ordinal() ? "" : "x";
		MoveSetBase dB = MoveSetBase.values()[move.getTo()];
		long movablePieces = 0;
		for (Move m : pos.getMoves())
			movablePieces |= BitOperations.toBit(m.getFrom());
		long possOriginSqrs;
		String origin;
		switch (piece) {
			case W_KING:
				possOriginSqrs = 0;
				origin = "";
				break;
			case W_QUEEN:
				possOriginSqrs = dB.getQueenMoveSet(pos.getWhiteQueens() & movablePieces, pos.getAllOccupied());
				origin = null;
				break;
			case W_ROOK:
				possOriginSqrs = dB.getRookMoveSet(pos.getWhiteRooks() & movablePieces, pos.getAllOccupied());
				origin = null;
				break;
			case W_BISHOP:
				possOriginSqrs = dB.getBishopMoveSet(pos.getWhiteBishops() & movablePieces, pos.getAllOccupied());
				origin = null;
				break;
			case W_KNIGHT:
				possOriginSqrs = dB.getKnightMoveSet(pos.getWhiteKnights() & movablePieces);
				origin = null;
				break;
			case W_PAWN:
				if (move.getCapturedPiece() != Piece.NULL.ordinal()) {
					possOriginSqrs = dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces);
					if (BitOperations.hammingWeight(possOriginSqrs) == 1)
						origin = "";
					else
						origin = Character.toString((char) (move.getFrom()%8 + 'a'));
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
				possOriginSqrs = dB.getQueenMoveSet(pos.getBlackQueens() & movablePieces, pos.getAllOccupied());
				origin = null;
				break;
			case B_ROOK:
				possOriginSqrs = dB.getRookMoveSet(pos.getBlackRooks() & movablePieces, pos.getAllOccupied());
				origin = null;
				break;
			case B_BISHOP:
				possOriginSqrs = dB.getBishopMoveSet(pos.getBlackBishops() & movablePieces, pos.getAllOccupied());
				origin = null;
				break;
			case B_KNIGHT:
				possOriginSqrs = dB.getKnightMoveSet(pos.getBlackKnights() & movablePieces);
				origin = null;
				break;
			case B_PAWN:
				if (move.getCapturedPiece() != Piece.NULL.ordinal()) {
					possOriginSqrs = dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces);
					if (BitOperations.hammingWeight(possOriginSqrs) == 1)
						origin = "";
					else
						origin = Character.toString((char) (move.getFrom()%8 + 'a'));
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
			else if (BitOperations.hammingWeight(Bitboard.File.getBySquareIndex(move.getFrom()).getBitboard() &
					possOriginSqrs) == 1)
				origin = Character.toString((char) (move.getFrom()%8 + 'a'));
			else if (BitOperations.hammingWeight(Bitboard.Rank.getBySquareIndex(move.getFrom()).getBitboard() &
					possOriginSqrs) == 1)
				origin = Integer.toString(move.getFrom()/8 + 1);
			else
				origin = Character.toString((char) (move.getFrom()%8 + 'a')) + Integer.toString(move.getFrom()/8 + 1);
		}
		String spMoveSuffix;
		if (move.getType() == MoveType.EN_PASSANT.ordinal())
			spMoveSuffix = "e.p.";
		else if (move.getType() == MoveType.PROMOTION_TO_QUEEN.ordinal())
			spMoveSuffix = "=Q";
		else if (move.getType() == MoveType.PROMOTION_TO_ROOK.ordinal())
			spMoveSuffix = "=R";
		else if (move.getType() == MoveType.PROMOTION_TO_BISHOP.ordinal())
			spMoveSuffix = "=B";
		else if (move.getType() == MoveType.PROMOTION_TO_KNIGHT.ordinal())
			spMoveSuffix = "=N";
		else
			spMoveSuffix = "";
		String check = pos.givesCheck(move) ? "+" : "";
		return movedPiece + origin + capture + destFile + destRank + spMoveSuffix + check;
	}
	/**
	 * Parses a game in PGN notation and returns a game instance.
	 * 
	 * @param pgn The PGN string.
	 * @return The game instance.
	 * @throws ChessParseException If the PGN string cannot be parsed.
	 */
	public static Game parse(String pgn) throws ChessParseException {
		char tagChar;
		String tagContent, tagType, tagValue,
			event = null, site = null, date = null, round = null,
			whiteName = null, blackName = null, result = null, fen = null;
		Game out = new Game();
		String[] moveDescParts;
		ArrayList<String> sanStrings = new ArrayList<>();
		Move move;
		if (pgn == null)
			return null;
		try {
			for (int i = 0; i < pgn.length(); i++) {
				if (pgn.charAt(i) == '[') {
					tagContent = "";
					while (++i < pgn.length() && (tagChar = pgn.charAt(i)) != ']')
						tagContent += tagChar;
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
			out.date = date;
			out.round = "?".equals(round) ? -1 : Short.parseShort(round);
			out.whitePlayerName = whiteName;
			out.blackPlayerName = blackName;
			out.position = fen == null ? Position.parse(Position.START_POSITION_FEN) : Position.parse(fen);
			out.startPosition = new Position(out.position);
			pgn = pgn.substring(pgn.lastIndexOf(']') + 1);
			pgn = pgn.trim();
			pgn = pgn.replaceAll(";[.]*\\n", "");
			pgn = pgn.replaceAll("\\([^)]*\\)", "");
			pgn = pgn.replaceAll("\\{[^\\}]*\\}", "");
			pgn = pgn.replaceAll("[0-9]+\\.", "");
			switch (result) {
			case "1-0":
				pgn = pgn.replaceAll("1-0", "");
				break;
			case "0-1":
				pgn = pgn.replaceAll("0-1", "");
				break;
			case "1/2-1/2":
				pgn = pgn.replaceAll("1/2-1/2", "");
				break;
			case "*":
				pgn = pgn.replaceAll("\\*", "");
				break;
			default:
				throw new ChessParseException("Invalid result.");
			}
			moveDescParts = pgn.split("[\\s]+");
			for (String s : moveDescParts) {
				s = s.trim();
				if (!s.isEmpty() && !s.matches("^\\$[0-9]+$"))
					sanStrings.add(s);
			}
			for (String sanString : sanStrings) {
				move = parseSAN(out.position, sanString);
				out.position.makeMove(move);
			}
			out.updateState();
			if (out.state == GameState.IN_PROGRESS && !"*".equals(result)) {
				if ("1-0".equals(result))
					out.state = GameState.UNSPECIFIED_WHITE_WIN;
				else if ("0-1".equals(result))
					out.state = GameState.UNSPECIFIED_BLACK_WIN;
				else
					out.state = GameState.DRAW_BY_AGREEMENT;
			}
		}
		catch (Exception e) {
			throw new ChessParseException(e);
		}
		return out;
	}
	/**
	 * @param position The start position of the game.
	 * @param event The name of the event.
	 * @param site The site of the event.
	 * @param whitePlayerName The white player's name.
	 * @param blackPlayerName The black player's name.
	 * @param round The round number in case the game is one of a series of games.
	 */
	public Game(Position position, String event, String site, String whitePlayerName, String blackPlayerName,
				int round) {
		try {
			this.position = Position.parse(position.toString());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		startPosition = new Position(this.position);
		this.event = event;
		this.site = site;
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		date = cal.get(Calendar.YEAR) + "." + cal.get(Calendar.MONTH) + "." + cal.get(Calendar.DAY_OF_MONTH);
		this.round = round;
		this.whitePlayerName = whitePlayerName;
		this.blackPlayerName = blackPlayerName;
		updateState();
	}
	/**
	 * @param position The start position of the game.
	 * @param event The name of the event.
	 * @param site The site of the event.
	 * @param whitePlayerName The white player's name.
	 * @param blackPlayerName The black player's name.
	 */
	Game(Position position, String event, String site, String whitePlayerName, String blackPlayerName) {
		this(position, event, site, whitePlayerName, blackPlayerName, -1);
	}
	/**
	 * @param position The start position of the game.
	 */
	Game(Position position) {
		this(position, null, null, null, null, -1);
	}
	/**
	 * Default constructor.
	 */
	Game() {
		try {
			position = Position.parse(Position.START_POSITION_FEN);
		} catch (ChessParseException e) { }
		startPosition = new Position(position);
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		date = cal.get(Calendar.YEAR) + "." + cal.get(Calendar.MONTH) + "." + cal.get(Calendar.DAY_OF_MONTH);
		round = 1;
		state = GameState.IN_PROGRESS;
	}
	/**
	 * @return A deep copy of the starting position of the game.
	 */
	public Position getStartPos() {
		return new Position(startPosition);
	}
	/**
	 * @return A deep copy of the current position
	 */
	public Position getPosition() {
		return new Position(position);
	}
	/**
	 * @return The name of the event the game took place at.
	 */
	public String getEvent() {
		return event;
	}
	/**
	 * @return The site of the event where the game took place.
	 */
	public String getSite() {
		return site;
	}
	/**
	 * @return The date the game took place.
	 */
	public String getDate() {
		return date;
	}
	/**
	 * @return The round of the game.
	 */
	public int getRound() {
		return round;
	}
	/**
	 * @return The white player's name.
	 */
	public String getWhitePlayerName() {
		return whitePlayerName;
	}
	/**
	 * @return The black player's name.
	 */
	public String getBlackPlayerName() {
		return blackPlayerName;
	}
	/**
	 * @return The state of the game.
	 */
	public GameState getState() {
		return state;
	}
	/**
	 * @param event The name of the event at which the game took place.
	 */
	public void setEvent(String event) {
		this.event = event;
	}
	/**
	 * @param site The site of the event where the game took place.
	 */
	public void setSite(String site) {
		this.site = site;
	}
	/**
	 * @param whitePlayerName The name of the white player.
	 */
	public void setWhitePlayerName(String whitePlayerName) {
		this.whitePlayerName = whitePlayerName;
	}
	/**
	 * @param blackPlayerName The name of the black player.
	 */
	public void setBlackPlayerName(String blackPlayerName) {
		this.blackPlayerName = blackPlayerName;
	}
	/**
	 * Sets the state of the game in case of draw by agreement, resignation, or time out. Otherwise it is a no-op.
	 * 
	 * @param state The new state of the game.
	 */
	public void setState(GameState state) {
		if (state != null && (state == GameState.DRAW_BY_AGREEMENT || state == GameState.UNSPECIFIED_WHITE_WIN ||
				state == GameState.UNSPECIFIED_BLACK_WIN))
			this.state = state;
	}
	private void updateState() {
		if (position.getMoves().size() == 0) {
			state = position.isInCheck() ?
					(position.isWhitesTurn() ? GameState.BLACK_MATES : GameState.WHITE_MATES) :
					GameState.STALE_MATE;
		} else {
			if (Evaluator.isMaterialInsufficient(position))
				state = GameState.DRAW_BY_INSUFFICIENT_MATERIAL;
			else if (position.hasRepeated(2))
				state = GameState.DRAW_BY_3_FOLD_REPETITION;
			else if (position.getFiftyMoveRuleClock() >= 100)
				state = GameState.DRAW_BY_50_MOVE_RULE;
			else
				state = GameState.IN_PROGRESS;
		}
	}
	/**
	 * Plays a move defined either in PACN or SAN format if legal.
	 * 
	 * @param move The move to make defined either in Pure Algebraic Coordinate Notation or Standard Algebraic Notation.
	 * @return Whether the move was legal and of valid format.
	 */
	public boolean play(String move) {
		Move m;
		try {
			m = parsePACN(position, move);
		} catch (ChessParseException | NullPointerException e) {
			try {
				m = parseSAN(position, move);
			} catch (ChessParseException | NullPointerException e1) { return false; }
		}
		if (position.getMoves().contains(m)) {
			position.makeMove(m);
			updateState();
			return true;
		}
		return false;
	}
	/**
	 * Unmakes the last move and returns it in Pure Algebraic Coordinate Notation. It returns null if no moves have
	 * been made yet.
	 * 
	 * @return The last move PACN format.
	 */
	public String unplay() {
		Move m = position.unmakeMove();
		updateState();
		return m == null ? null : m.toString();
	}
	private String moveListToSAN() {
		String moveListSAN = "";
		boolean printRound = true;
		int roundNum = 0;
		Position posCopy = new Position(position);
		ArrayDeque<Move> moves = new ArrayDeque<>();
		for (Move move : posCopy.getMoveHistory()) {
			moves.addFirst(move);
			posCopy.unmakeMove();
		}
		for (Move move : moves) {
			if (roundNum%6 == 0 && printRound)
				moveListSAN += "\n";
			if (printRound)
				moveListSAN += ++roundNum + ". ";
			printRound = !printRound;
			moveListSAN += toSAN(posCopy, move) + " ";
			posCopy.makeMove(move);
		}
		return moveListSAN;
	}
	@Override
	public String toString() {
		String pgn = "", result;
		pgn += "[Event \"" + (event == null ? "N/A" : event) + "\"]\n";
		pgn += "[Site \"" + (site == null ? "N/A" : site) + "\"]\n";
		pgn += "[Date \"" + date + "\"]\n";
		pgn += "[Round \"" + (round == -1 ? "?" : round) + "\"]\n";
		pgn += "[White \"" + (whitePlayerName == null ? "N/A" : whitePlayerName) + "\"]\n";
		pgn += "[Black \"" + (blackPlayerName == null ? "N/A" : blackPlayerName) + "\"]\n";
		if (state == GameState.IN_PROGRESS)
			result = "*";
		else if (state == GameState.WHITE_MATES || state == GameState.UNSPECIFIED_WHITE_WIN)
			result = "1-0";
		else if (state == GameState.BLACK_MATES || state == GameState.UNSPECIFIED_BLACK_WIN)
			result = "0-1";
		else
			result = "1/2-1/2";
		pgn += "[Result \"" + result + "\"]\n";
		pgn += "[FEN \"" + startPosition.toString() + "\"]\n";
		pgn += moveListToSAN();
		return pgn;
	}
	
}
