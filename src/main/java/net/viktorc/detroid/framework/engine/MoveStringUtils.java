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
          type = (byte) MoveType.PROMOTION_TO_QUEEN.ordinal();
          break;
        case 'r':
          type = (byte) MoveType.PROMOTION_TO_ROOK.ordinal();
          break;
        case 'b':
          type = (byte) MoveType.PROMOTION_TO_BISHOP.ordinal();
          break;
        case 'n':
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
        movablePieces |= BitOperations.toBit(m.getFrom());
      }
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
        to = (byte) ((chars[0] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[1])) - 1));
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
        to = (byte) ((chars[0] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[1])) - 1));
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
        to = (byte) ((chars[1] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[2])) - 1));
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
        to = (byte) ((chars[1] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[2])) - 1));
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
        to = (byte) ((chars[1] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[2])) - 1));
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
        to = (byte) ((chars[2] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[3])) - 1));
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
        to = (byte) ((chars[2] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[3])) - 1));
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
        to = (byte) ((chars[2] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[3])) - 1));
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
        to = (byte) ((chars[1] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[2])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
        capturedPiece = (byte) Piece.NULL.ordinal();
        type = (byte) MoveType.NORMAL.ordinal();
        from = -1;
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^[KQRBN][a-h][a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[2] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[3])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
        capturedPiece = (byte) Piece.NULL.ordinal();
        type = (byte) MoveType.NORMAL.ordinal();
        from = -1;
        restriction = Bitboard.File.values()[chars[1] - 'a'].getBitboard();
      } else if (san.matches("^[KQRBN][1-8][a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[2] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[3])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
        capturedPiece = (byte) Piece.NULL.ordinal();
        type = (byte) MoveType.NORMAL.ordinal();
        from = -1;
        restriction = Bitboard.Rank.values()[chars[1] - '1'].getBitboard();
      } else if (san.matches("^[KQRBN][a-h][1-8][a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[3] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[4])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
        capturedPiece = (byte) Piece.NULL.ordinal();
        type = (byte) MoveType.NORMAL.ordinal();
        from = -1;
        restriction = Bitboard.File.values()[chars[1] - 'a'].getBitboard() &
            Bitboard.Rank.values()[chars[2] - '1'].getBitboard();
      } else if (san.matches("^[KQRBN]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((int) (chars[2] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[3])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
        capturedPiece = pos.getPiece(to);
        type = (byte) MoveType.NORMAL.ordinal();
        from = -1;
        restriction = Bitboard.FULL_BOARD;
      } else if (san.matches("^[KQRBN][a-h]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[3] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[4])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
        capturedPiece = pos.getPiece(to);
        type = (byte) MoveType.NORMAL.ordinal();
        from = -1;
        restriction = Bitboard.File.values()[chars[1] - 'a'].getBitboard();
      } else if (san.matches("^[KQRBN][1-8]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[3] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[4])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
        capturedPiece = pos.getPiece(to);
        type = (byte) MoveType.NORMAL.ordinal();
        from = -1;
        restriction = Bitboard.Rank.values()[chars[1] - '1'].getBitboard();
      } else if (san.matches("^[KQRBN][a-h][1-8]x[a-h][1-8][+#]?[/?!]{0,2}$")) {
        to = (byte) ((chars[4] - 'a') + 8 * (Integer.parseInt(Character.toString(chars[5])) - 1));
        movedPiece = (byte) (Arrays.stream(Piece.values())
            .filter(p -> p.getLetter() == chars[0]).findAny().get().ordinal() +
            (pos.isWhitesTurn() ? 0 : Piece.W_PAWN.ordinal()));
        capturedPiece = pos.getPiece(to);
        type = (byte) MoveType.NORMAL.ordinal();
        from = -1;
        restriction = Bitboard.File.values()[chars[1] - 'a'].getBitboard() & Bitboard.Rank.values()[chars[2] - '1'].getBitboard();
      } else {
        throw new ChessParseException("The move String violates the SAN standard.");
      }
      if (from == -1) {
        dB = MoveSetBase.values()[to];
        if (movedPiece == Piece.W_KING.ordinal()) {
          from = BitOperations.indexOfBit(dB.getKingMoveSet(pos.getWhiteKing()));
        } else if (movedPiece == Piece.W_QUEEN.ordinal()) {
          from = BitOperations.indexOfBit(dB.getQueenMoveSet(pos.getWhiteQueens() & movablePieces & restriction,
              pos.getAllOccupied()));
        } else if (movedPiece == Piece.W_ROOK.ordinal()) {
          from = BitOperations.indexOfBit(dB.getRookMoveSet(pos.getWhiteRooks() & movablePieces & restriction,
              pos.getAllOccupied()));
        } else if (movedPiece == Piece.W_BISHOP.ordinal()) {
          from = BitOperations.indexOfBit(dB.getBishopMoveSet(pos.getWhiteBishops() & movablePieces & restriction,
              pos.getAllOccupied()));
        } else if (movedPiece == Piece.W_KNIGHT.ordinal()) {
          from = BitOperations.indexOfBit(dB.getKnightMoveSet(pos.getWhiteKnights() & movablePieces & restriction));
        } else if (movedPiece == Piece.B_KING.ordinal()) {
          from = BitOperations.indexOfBit(dB.getKingMoveSet(pos.getBlackKing()));
        } else if (movedPiece == Piece.B_QUEEN.ordinal()) {
          from = BitOperations.indexOfBit(dB.getQueenMoveSet(pos.getBlackQueens() & movablePieces & restriction,
              pos.getAllOccupied()));
        } else if (movedPiece == Piece.B_ROOK.ordinal()) {
          from = BitOperations.indexOfBit(dB.getRookMoveSet(pos.getBlackRooks() & movablePieces & restriction,
              pos.getAllOccupied()));
        } else if (movedPiece == Piece.B_BISHOP.ordinal()) {
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
    if (move.getType() == MoveType.SHORT_CASTLING.ordinal()) {
      return "O-O";
    } else if (move.getType() == MoveType.LONG_CASTLING.ordinal()) {
      return "O-O-O";
    }
    String destRank = Integer.toString(move.getTo() / 8 + 1);
    String destFile = Character.toString((char) (move.getTo() % 8 + 'a'));
    Piece piece = Piece.values()[move.getMovedPiece()];
    String movedPiece;
    if (piece == Piece.W_PAWN || piece == Piece.B_PAWN) {
      movedPiece = "";
    } else {
      movedPiece = Character.toString(piece.getLetter()).toUpperCase();
    }
    String capture = move.getCapturedPiece() == Piece.NULL.ordinal() ? "" : "x";
    MoveSetBase dB = MoveSetBase.values()[move.getTo()];
    long movablePieces = 0;
    for (Move m : pos.getMoves()) {
      movablePieces |= BitOperations.toBit(m.getFrom());
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
        if (move.getCapturedPiece() != Piece.NULL.ordinal()) {
          possOriginSqrs = dB.getBlackPawnCaptureSet(pos.getWhitePawns() & movablePieces);
          if (BitOperations.hammingWeight(possOriginSqrs) == 1) {
            origin = "";
          } else {
            origin = Character.toString((char) (move.getFrom() % 8 + 'a'));
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
        if (move.getCapturedPiece() != Piece.NULL.ordinal()) {
          possOriginSqrs = dB.getWhitePawnCaptureSet(pos.getBlackPawns() & movablePieces);
          if (BitOperations.hammingWeight(possOriginSqrs) == 1) {
            origin = "";
          } else {
            origin = Character.toString((char) (move.getFrom() % 8 + 'a'));
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
      } else if (BitOperations.hammingWeight(Bitboard.File.getBySquareIndex(move.getFrom()).getBitboard() &
          possOriginSqrs) == 1) {
        origin = Character.toString((char) (move.getFrom() % 8 + 'a'));
      } else if (BitOperations.hammingWeight(Bitboard.Rank.getBySquareIndex(move.getFrom()).getBitboard() &
          possOriginSqrs) == 1) {
        origin = Integer.toString(move.getFrom() / 8 + 1);
      } else {
        origin = Character.toString((char) (move.getFrom() % 8 + 'a')) + Integer.toString(move.getFrom() / 8 + 1);
      }
    }
    String spMoveSuffix;
    if (move.getType() == MoveType.EN_PASSANT.ordinal()) {
      spMoveSuffix = "e.p.";
    } else if (move.getType() == MoveType.PROMOTION_TO_QUEEN.ordinal()) {
      spMoveSuffix = "=Q";
    } else if (move.getType() == MoveType.PROMOTION_TO_ROOK.ordinal()) {
      spMoveSuffix = "=R";
    } else if (move.getType() == MoveType.PROMOTION_TO_BISHOP.ordinal()) {
      spMoveSuffix = "=B";
    } else if (move.getType() == MoveType.PROMOTION_TO_KNIGHT.ordinal()) {
      spMoveSuffix = "=N";
    } else {
      spMoveSuffix = "";
    }
    String check = pos.givesCheck(move) ? "+" : "";
    return movedPiece + origin + capture + destFile + destRank + spMoveSuffix + check;
  }

}
