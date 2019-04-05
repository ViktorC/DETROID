package net.viktorc.detroid.framework.engine;

import java.util.Arrays;
import net.viktorc.detroid.framework.util.BitOperations;

/**
 * A utility class for parsing move strings and converting move objects into move strings.
 *
 * @author Viktor
 */
public class MoveStringUtils {

  private MoveStringUtils() {
  }

  /**
   * Parses a move string in PACN format.
   *
   * @param pos A position in which the move defined by the move string is legal.
   * @param pacn The move string to parse.
   * @return The move object.
   * @throws ChessParseException If the move string is in an illegal format.
   */
  public static Move parsePACN(Position pos, String pacn) throws ChessParseException {
    String input = pacn.trim().toLowerCase();
    if (input.length() != 4 && input.length() != 5) {
      throw new ChessParseException("The input does not pass the formal requirements of a PACN String. Its " +
          "length is neither 4 nor 5");
    }
    byte from = (byte) ((input.charAt(0) - 'a') + 8 * (Integer.parseInt(Character.toString(input.charAt(1))) - 1));
    byte to = (byte) ((input.charAt(2) - 'a') + 8 * (Integer.parseInt(Character.toString(input.charAt(3))) - 1));
    byte movedPiece = pos.getPiece(from);
    byte capturedPiece;
    byte type;
    if (input.length() == 5) {
      switch (input.charAt(4)) {
        case 'q':
          type = MoveType.PROMOTION_TO_QUEEN.ind;
          break;
        case 'r':
          type = MoveType.PROMOTION_TO_ROOK.ind;
          break;
        case 'b':
          type = MoveType.PROMOTION_TO_BISHOP.ind;
          break;
        case 'n':
          type = MoveType.PROMOTION_TO_KNIGHT.ind;
          break;
        default:
          throw new ChessParseException("The input does not pass the formal requirements of a PACN String. " +
              "Wrong promotion notation");
      }
      capturedPiece = pos.getPiece(to);
    } else {
      if (movedPiece == Piece.W_PAWN.ind) {
        if (pos.getEnPassantRights() != EnPassantRights.NONE.ind &&
            to == pos.getEnPassantRights() + EnPassantRights.TO_W_DEST_SQR_IND) {
          type = MoveType.EN_PASSANT.ind;
          capturedPiece = Piece.B_PAWN.ind;
        } else {
          type = MoveType.NORMAL.ind;
          capturedPiece = pos.getPiece(to);
        }
      } else if (movedPiece == Piece.B_PAWN.ind) {
        if (pos.getEnPassantRights() != EnPassantRights.NONE.ind &&
            to == pos.getEnPassantRights() + EnPassantRights.TO_B_DEST_SQR_IND) {
          type = MoveType.EN_PASSANT.ind;
          capturedPiece = Piece.W_PAWN.ind;
        } else {
          type = MoveType.NORMAL.ind;
          capturedPiece = pos.getPiece(to);
        }
      } else {
        if ((movedPiece == Piece.W_KING.ind && from == Bitboard.Square.E1.ind &&
            to == Bitboard.Square.G1.ind) || (movedPiece == Piece.B_KING.ind &&
            from == Bitboard.Square.E8.ind && to == Bitboard.Square.G8.ind)) {
          type = MoveType.SHORT_CASTLING.ind;
          capturedPiece = Piece.NULL.ind;
        } else if ((movedPiece == Piece.W_KING.ind && from == Bitboard.Square.E1.ind &&
            to == Bitboard.Square.C1.ind) || (movedPiece == Piece.B_KING.ind &&
            from == Bitboard.Square.E8.ind && to == Bitboard.Square.C8.ind)) {
          type = MoveType.LONG_CASTLING.ind;
          capturedPiece = Piece.NULL.ind;
        } else {
          type = MoveType.NORMAL.ind;
          capturedPiece = pos.getPiece(to);
        }
      }
    }
    return new Move(from, to, movedPiece, capturedPiece, type);
  }

  /**
   * Parses a move string in SAN format.
   *
   * @param pos A position in which the move defined by the move string is legal.
   * @param san The move string to parse.
   * @return The move object.
   * @throws ChessParseException If the move string is in an illegal format.
   */
  public static Move parseSAN(Position pos, String san) throws ChessParseException, NullPointerException {
    byte from, to, movedPiece, capturedPiece, type;
    long movablePieces, restriction, pawnAdvancer;
    char[] chars;
    MoveSetBase dB;
    if (san == null) {
      return null;
    }
    try {
      movablePieces = 0;
      for (Move m : pos.getMoves()) {
        movablePieces |= BitOperations.toBit(m.from);
      }
      chars = san.toCharArray();
      if (san.matches("^O-O[+#]?[/?!]{0,2}$")) {
        if (pos.isWhitesTurn()) {
          to = Bitboard.Square.G1.ind;
          from = Bitboard.Square.E1.ind;
          movedPiece = Piece.W_KING.ind;
        } else {
          to = Bitboard.Square.G8.ind;
          from = Bitboard.Square.E8.ind;
          movedPiece = Piece.B_KING.ind;
        }
        capturedPiece = Piece.NULL.ind;
        type = MoveType.SHORT_CASTLING.ind;
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^O-O-O[+#]?[/?!]{0,2}$")) {
        if (pos.isWhitesTurn()) {
          to = Bitboard.Square.C1.ind;
          from = Bitboard.Square.E1.ind;
          movedPiece = Piece.W_KING.ind;
        } else {
          to = Bitboard.Square.C8.ind;
          from = Bitboard.Square.E8.ind;
          movedPiece = Piece.B_KING.ind;
        }
        capturedPiece = Piece.NULL.ind;
        type = MoveType.LONG_CASTLING.ind;
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^[a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[0] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[1])) - 1));
        if (pos.isWhitesTurn()) {
          movedPiece = Piece.W_PAWN.ind;
          pawnAdvancer = (1L << (to - 8)) & pos.getWhitePawns() & movablePieces;
          from = (byte) ((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to - 16);
        } else {
          movedPiece = Piece.B_PAWN.ind;
          pawnAdvancer = (1L << (to + 8)) & pos.getBlackPawns() & movablePieces;
          from = (byte) ((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to + 16);
        }
        capturedPiece = Piece.NULL.ind;
        type = MoveType.NORMAL.ind;
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^[a-h][1-8]=[QRBN][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[0] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[1])) - 1));
        if (pos.isWhitesTurn()) {
          movedPiece = Piece.W_PAWN.ind;
          pawnAdvancer = (1L << (to - 8)) & pos.getWhitePawns() & movablePieces;
          from = (byte) ((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to - 16);
        } else {
          movedPiece = Piece.B_PAWN.ind;
          pawnAdvancer = (1L << (to + 8)) & pos.getBlackPawns() & movablePieces;
          from = (byte) ((pawnAdvancer != 0) ? BitOperations.indexOfBit(pawnAdvancer) : to + 16);
        }
        capturedPiece = Piece.NULL.ind;
        type = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.letter == chars[3]).findAny().get().ind + 2);
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^x[a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[1] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[2])) - 1));
        dB = MoveSetBase.getByIndex(to);
        if (pos.isWhitesTurn()) {
          movedPiece = Piece.W_PAWN.ind;
          from = BitOperations.indexOfBit(dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces));
          if (pos.getEnPassantRights() != EnPassantRights.NONE.ind &&
              to == pos.getEnPassantRights() + EnPassantRights.TO_W_DEST_SQR_IND) {
            capturedPiece = Piece.B_PAWN.ind;
            type = MoveType.EN_PASSANT.ind;
          } else {
            capturedPiece = pos.getPiece(to);
            type = MoveType.NORMAL.ind;
          }
        } else {
          movedPiece = Piece.B_PAWN.ind;
          from = BitOperations.indexOfBit(dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces));
          if (pos.getEnPassantRights() != EnPassantRights.NONE.ind &&
              to == pos.getEnPassantRights() + EnPassantRights.TO_B_DEST_SQR_IND) {
            capturedPiece = Piece.W_PAWN.ind;
            type = MoveType.EN_PASSANT.ind;
          } else {
            capturedPiece = pos.getPiece(to);
            type = MoveType.NORMAL.ind;
          }
        }
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^x[a-h][1-8]=[QRBN][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[1] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[2])) - 1));
        dB = MoveSetBase.getByIndex(to);
        if (pos.isWhitesTurn()) {
          movedPiece = Piece.W_PAWN.ind;
          from = BitOperations.indexOfBit(dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces));
        } else {
          movedPiece = Piece.B_PAWN.ind;
          from = BitOperations.indexOfBit(dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces));
        }
        capturedPiece = pos.getPiece(to);
        type = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.letter == chars[4]).findAny().get().ind + 2);
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^x[a-h][1-8]e.p.[+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[1] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[2])) - 1));
        dB = MoveSetBase.getByIndex(to);
        if (pos.isWhitesTurn()) {
          movedPiece = Piece.W_PAWN.ind;
          from = BitOperations.indexOfBit(dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces));
          capturedPiece = Piece.B_PAWN.ind;
        } else {
          movedPiece = Piece.B_PAWN.ind;
          from = BitOperations.indexOfBit(dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces));
          capturedPiece = Piece.W_PAWN.ind;
        }
        type = MoveType.EN_PASSANT.ind;
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^[a-h]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[2] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[3])) - 1));
        dB = MoveSetBase.getByIndex(to);
        if (pos.isWhitesTurn()) {
          movedPiece = Piece.W_PAWN.ind;
          from = BitOperations.indexOfBit(dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces)
              & Bitboard.File.values()[chars[0] - 'a'].bitboard);
          if (pos.getEnPassantRights() != EnPassantRights.NONE.ind &&
              to == pos.getEnPassantRights() + EnPassantRights.TO_W_DEST_SQR_IND) {
            capturedPiece = Piece.B_PAWN.ind;
            type = MoveType.EN_PASSANT.ind;
          } else {
            capturedPiece = pos.getPiece(to);
            type = MoveType.NORMAL.ind;
          }
        } else {
          movedPiece = Piece.B_PAWN.ind;
          from = BitOperations.indexOfBit(dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces)
              & Bitboard.File.values()[chars[0] - 'a'].bitboard);
          if (pos.getEnPassantRights() != EnPassantRights.NONE.ind &&
              to == pos.getEnPassantRights() + EnPassantRights.TO_B_DEST_SQR_IND) {
            capturedPiece = Piece.W_PAWN.ind;
            type = MoveType.EN_PASSANT.ind;
          } else {
            capturedPiece = pos.getPiece(to);
            type = MoveType.NORMAL.ind;
          }
        }
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^[a-h]x[a-h][1-8]=[QRBN][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[2] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[3])) - 1));
        dB = MoveSetBase.getByIndex(to);
        if (pos.isWhitesTurn()) {
          movedPiece = Piece.W_PAWN.ind;
          from = BitOperations.indexOfBit(dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces)
              & Bitboard.File.values()[chars[0] - 'a'].bitboard);
        } else {
          movedPiece = Piece.B_PAWN.ind;
          from = BitOperations.indexOfBit(dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces)
              & Bitboard.File.values()[chars[0] - 'a'].bitboard);
        }
        capturedPiece = pos.getPiece(to);
        type = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.letter == chars[5]).findAny().get().ind + 2);
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^[a-h]x[a-h][1-8]e.p.[+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[2] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[3])) - 1));
        dB = MoveSetBase.getByIndex(to);
        if (pos.isWhitesTurn()) {
          movedPiece = Piece.W_PAWN.ind;
          from = BitOperations.indexOfBit(dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces)
              & Bitboard.File.values()[chars[0] - 'a'].bitboard);
          capturedPiece = Piece.B_PAWN.ind;
        } else {
          movedPiece = Piece.B_PAWN.ind;
          from = BitOperations.indexOfBit(dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces)
              & Bitboard.File.values()[chars[0] - 'a'].bitboard);
          capturedPiece = Piece.W_PAWN.ind;
        }
        type = MoveType.EN_PASSANT.ind;
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^[KQRBN][a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[1] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[2])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.letter == chars[0]).findAny().get().ind +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ind));
        capturedPiece = Piece.NULL.ind;
        type = MoveType.NORMAL.ind;
        from = -1;
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^[KQRBN][a-h][a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[2] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[3])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.letter == chars[0]).findAny().get().ind +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ind));
        capturedPiece = Piece.NULL.ind;
        type = MoveType.NORMAL.ind;
        from = -1;
        restriction = Bitboard.File.values()[chars[1] - 'a'].bitboard;
      } else if (san.matches("^[KQRBN][1-8][a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[2] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[3])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.letter == chars[0]).findAny().get().ind +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ind));
        capturedPiece = Piece.NULL.ind;
        type = MoveType.NORMAL.ind;
        from = -1;
        restriction = Bitboard.Rank.values()[chars[1] - '1'].bitboard;
      } else if (san.matches("^[KQRBN][a-h][1-8][a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[3] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[4])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.letter == chars[0]).findAny().get().ind +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ind));
        capturedPiece = Piece.NULL.ind;
        type = MoveType.NORMAL.ind;
        from = -1;
        restriction = Bitboard.File.values()[chars[1] - 'a'].bitboard &
            Bitboard.Rank.values()[chars[2] - '1'].bitboard;
      } else if (san.matches("^[KQRBN]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((int) (chars[2] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[3])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.letter == chars[0]).findAny().get().ind +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ind));
        capturedPiece = pos.getPiece(to);
        type = MoveType.NORMAL.ind;
        from = -1;
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^[KQRBN][a-h]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[3] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[4])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.letter == chars[0]).findAny().get().ind +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ind));
        capturedPiece = pos.getPiece(to);
        type = MoveType.NORMAL.ind;
        from = -1;
        restriction = Bitboard.File.values()[chars[1] - 'a'].bitboard;
      } else if (san.matches("^[KQRBN][1-8]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[3] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[4])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.letter == chars[0]).findAny().get().ind +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ind));
        capturedPiece = pos.getPiece(to);
        type = MoveType.NORMAL.ind;
        from = -1;
        restriction = Bitboard.Rank.values()[chars[1] - '1'].bitboard;
      } else if (san.matches("^[KQRBN][a-h][1-8]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[4] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[5])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.letter == chars[0]).findAny().get().ind +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ind));
        capturedPiece = pos.getPiece(to);
        type = MoveType.NORMAL.ind;
        from = -1;
        restriction = Bitboard.File.values()[chars[1] - 'a'].bitboard & Bitboard.Rank.values()[chars[2] - '1'].bitboard;
      } else {
        throw new ChessParseException("The move String violates the SAN standard.");
      }
      if (from == -1) {
        dB = MoveSetBase.getByIndex(to);
        if (movedPiece == Piece.W_KING.ind) {
          from = BitOperations.indexOfBit(dB.getKingMoveSet(pos.getWhiteKing()));
        } else if (movedPiece == Piece.W_QUEEN.ind) {
          from = BitOperations.indexOfBit(dB.getQueenMoveSet(pos.getWhiteQueens() & movablePieces & restriction,
              pos.getAllOccupied()));
        } else if (movedPiece == Piece.W_ROOK.ind) {
          from = BitOperations.indexOfBit(dB.getRookMoveSet(pos.getWhiteRooks() & movablePieces & restriction,
              pos.getAllOccupied()));
        } else if (movedPiece == Piece.W_BISHOP.ind) {
          from = BitOperations.indexOfBit(dB.getBishopMoveSet(pos.getWhiteBishops() & movablePieces & restriction,
              pos.getAllOccupied()));
        } else if (movedPiece == Piece.W_KNIGHT.ind) {
          from = BitOperations.indexOfBit(dB.getKnightMoveSet(pos.getWhiteKnights() & movablePieces & restriction));
        } else if (movedPiece == Piece.B_KING.ind) {
          from = BitOperations.indexOfBit(dB.getKingMoveSet(pos.getBlackKing()));
        } else if (movedPiece == Piece.B_QUEEN.ind) {
          from = BitOperations.indexOfBit(dB.getQueenMoveSet(pos.getBlackQueens() & movablePieces & restriction,
              pos.getAllOccupied()));
        } else if (movedPiece == Piece.B_ROOK.ind) {
          from = BitOperations.indexOfBit(dB.getRookMoveSet(pos.getBlackRooks() & movablePieces & restriction,
              pos.getAllOccupied()));
        } else if (movedPiece == Piece.B_BISHOP.ind) {
          from = BitOperations.indexOfBit(dB.getBishopMoveSet(pos.getBlackBishops() & movablePieces & restriction,
              pos.getAllOccupied()));
        } else {
          from = BitOperations.indexOfBit(dB.getKnightMoveSet(pos.getBlackKnights() & movablePieces & restriction));
        }
      }
    } catch (Exception e) {
      throw new ChessParseException(e);
    }
    return new Move(from, to, movedPiece, capturedPiece, type);
  }

  /**
   * It creates a move string in SAN format.
   *
   * @param pos A position in which the move defined by the move string is legal.
   * @param move The move to convert to a move string.
   * @return The move string.
   */
  public static String toSAN(Position pos, Move move) {
    if (move == null) {
      return null;
    }
    if (move.type == MoveType.SHORT_CASTLING.ind) {
      return "O-O";
    } else if (move.type == MoveType.LONG_CASTLING.ind) {
      return "O-O-O";
    }
    String destRank = Integer.toString(move.to / 8 + 1);
    String destFile = Character.toString((char) (move.to % 8 + 'a'));
    Piece piece = Piece.values()[move.movedPiece];
    String movedPiece;
    if (piece == Piece.W_PAWN || piece == Piece.B_PAWN) {
      movedPiece = "";
    } else {
      movedPiece = Character.toString(piece.letter).toUpperCase();
    }
    String capture = move.capturedPiece == Piece.NULL.ind ? "" : "x";
    MoveSetBase dB = MoveSetBase.getByIndex(move.to);
    long movablePieces = 0;
    for (Move m : pos.getMoves()) {
      movablePieces |= BitOperations.toBit(m.from);
    }
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
        if (move.capturedPiece != Piece.NULL.ind) {
          possOriginSqrs = dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces);
          if (BitOperations.hammingWeight(possOriginSqrs) == 1) {
            origin = "";
          } else {
            origin = Character.toString((char) (move.from % 8 + 'a'));
          }
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
        if (move.capturedPiece != Piece.NULL.ind) {
          possOriginSqrs = dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces);
          if (BitOperations.hammingWeight(possOriginSqrs) == 1) {
            origin = "";
          } else {
            origin = Character.toString((char) (move.from % 8 + 'a'));
          }
        } else {
          possOriginSqrs = 0;
          origin = "";
        }
        break;
      default:
        return null;
    }
    if (origin == null) {
      if (BitOperations.hammingWeight(possOriginSqrs) == 1) {
        origin = "";
      } else if (BitOperations.hammingWeight(Bitboard.File.getBySquareIndex(move.from).bitboard &
          possOriginSqrs) == 1) {
        origin = Character.toString((char) (move.from % 8 + 'a'));
      } else if (BitOperations.hammingWeight(Bitboard.Rank.getBySquareIndex(move.from).bitboard &
          possOriginSqrs) == 1) {
        origin = Integer.toString(move.from / 8 + 1);
      } else {
        origin = Character.toString((char) (move.from % 8 + 'a')) + Integer.toString(move.from / 8 + 1);
      }
    }
    String spMoveSuffix;
    if (move.type == MoveType.EN_PASSANT.ind) {
      spMoveSuffix = "e.p.";
    } else if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
      spMoveSuffix = "=Q";
    } else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
      spMoveSuffix = "=R";
    } else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
      spMoveSuffix = "=B";
    } else if (move.type == MoveType.PROMOTION_TO_KNIGHT.ind) {
      spMoveSuffix = "=N";
    } else {
      spMoveSuffix = "";
    }
    String check = pos.givesCheck(move) ? "+" : "";
    return movedPiece + origin + capture + destFile + destRank + spMoveSuffix + check;
  }

}
