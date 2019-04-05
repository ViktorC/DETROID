package net.viktorc.detroid.framework.engine;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import net.viktorc.detroid.framework.util.BitOperations;

/**
 * A bitboard based chess position class that stores information about the side to move, en passant and castling rights, the board position,
 * all the moves made, and all the previous position states. It allows for the generation, making, and taking back of chess moves.
 *
 * @author Viktor
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
   * It parses a FEN string and initializes a position instance based on it. Beside standard six-field FEN-strings, it also accepts
   * four-field Strings without the fifty-move rule clock and the move index.
   *
   * @param fen The FEN string.
   * @return The position instance.
   * @throws ChessParseException If the string is invalid.
   */
  public static Position parse(String fen) throws ChessParseException {
    Position pos = new Position();
    String[] fenFields = fen.split(" ");
    if (fenFields.length != 4 && fenFields.length != 6) {
      throw new ChessParseException("The FEN-String has an unallowed number of fields.");
    }
    String board = fenFields[0];
    String turn = fenFields[1];
    String castling = fenFields[2];
    String enPassant = fenFields[3];
    String[] ranks = board.split("/");
    if (ranks.length != 8) {
      throw new ChessParseException("The board position representation does not have eight ranks.");
    }
    pos.squares = new byte[64];
    for (int i = 0; i < 64; i++) {
      pos.squares[i] = Piece.NULL.ind;
    }
    int index = 0;
    for (int i = 7; i >= 0; i--) {
      String rank = ranks[i];
      for (int j = 0; j < rank.length(); j++) {
        char piece = rank.charAt(j);
        int pieceNum = piece - '0';
        if (pieceNum >= 0 && pieceNum <= 8) {
          index += pieceNum;
        } else {
          long bitboard = Bitboard.Square.values()[index].bitboard;
          switch (piece) {
            case 'K':
              pos.whiteKing = bitboard;
              pos.squares[index] = Piece.W_KING.ind;
              break;
            case 'Q':
              pos.whiteQueens |= bitboard;
              pos.squares[index] = Piece.W_QUEEN.ind;
              break;
            case 'R':
              pos.whiteRooks |= bitboard;
              pos.squares[index] = Piece.W_ROOK.ind;
              break;
            case 'B':
              pos.whiteBishops |= bitboard;
              pos.squares[index] = Piece.W_BISHOP.ind;
              break;
            case 'N':
              pos.whiteKnights |= bitboard;
              pos.squares[index] = Piece.W_KNIGHT.ind;
              break;
            case 'P':
              pos.whitePawns |= bitboard;
              pos.squares[index] = Piece.W_PAWN.ind;
              break;
            case 'k':
              pos.blackKing = bitboard;
              pos.squares[index] = Piece.B_KING.ind;
              break;
            case 'q':
              pos.blackQueens |= bitboard;
              pos.squares[index] = Piece.B_QUEEN.ind;
              break;
            case 'r':
              pos.blackRooks |= bitboard;
              pos.squares[index] = Piece.B_ROOK.ind;
              break;
            case 'b':
              pos.blackBishops |= bitboard;
              pos.squares[index] = Piece.B_BISHOP.ind;
              break;
            case 'n':
              pos.blackKnights |= bitboard;
              pos.squares[index] = Piece.B_KNIGHT.ind;
              break;
            case 'p':
              pos.blackPawns |= bitboard;
              pos.squares[index] = Piece.B_PAWN.ind;
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
      pos.whiteCastlingRights = CastlingRights.NONE.ind;
      pos.blackCastlingRights = CastlingRights.NONE.ind;
    }
    if (castling.length() < 1 || castling.length() > 4) {
      throw new ChessParseException("Invalid length");
    }
    if (castling.contains("K")) {
      pos.whiteCastlingRights = (castling.contains("Q") ? CastlingRights.ALL.ind :
          CastlingRights.SHORT.ind);
    } else if (castling.contains("Q")) {
      pos.whiteCastlingRights = CastlingRights.LONG.ind;
    } else {
      pos.whiteCastlingRights = CastlingRights.NONE.ind;
    }
    if (castling.contains("k")) {
      pos.blackCastlingRights = (castling.contains("q") ? CastlingRights.ALL.ind :
          CastlingRights.SHORT.ind);
    } else if (castling.contains("q")) {
      pos.blackCastlingRights = CastlingRights.LONG.ind;
    } else {
      pos.blackCastlingRights = CastlingRights.NONE.ind;
    }
    if (enPassant.length() > 2) {
      throw new ChessParseException("Illegal en passant field.");
    }
    pos.enPassantRights = (byte) (enPassant.equals("-") ? EnPassantRights.NONE.ind :
        enPassant.toLowerCase().charAt(0) - 'a');
    if (fenFields.length == 6) {
      try {
        pos.fiftyMoveRuleClock = (byte) Math.max(0, Integer.parseInt(fenFields[4]));
      } catch (NumberFormatException e) {
        throw new ChessParseException("The fifty-move rule clock field of the " +
            "FEN-string does not conform to the standards. Parsing not possible.");
      }
      try {
        int moveIndex = (Integer.parseInt(fenFields[5]) - 1) * 2;
        if (!pos.whitesTurn) {
          moveIndex++;
        }
        pos.halfMoveIndex = Math.max(0, moveIndex);
      } catch (NumberFormatException e) {
        throw new ChessParseException("The move index field does not conform to the standards. " +
            "Parsing not possible.");
      }
    }
    pos.checkers = pos.isWhitesTurn() ? pos.getBlackCheckers(BitOperations.indexOfBit(pos.whiteKing)) :
        pos.getWhiteCheckers(BitOperations.indexOfBit(pos.blackKing));
    pos.inCheck = pos.checkers != Bitboard.EMPTY_BOARD;
    pos.key = ZobristKeyGenerator.getInstance().generateHashKey(pos);
    if (pos.halfMoveIndex >= pos.keyHistory.length) {
      int keyHistLength = pos.keyHistory.length;
      while (keyHistLength <= pos.halfMoveIndex) {
        keyHistLength += (keyHistLength >> 1);
      }
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
    allNonWhiteOccupied = pos.allNonWhiteOccupied;
    allNonBlackOccupied = pos.allNonBlackOccupied;
    allOccupied = pos.allOccupied;
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
   * @return The castling type that denotes to what extent it would still be possible to castle for white regardless of whether it is
   * actually legally executable in the current position.
   */
  public byte getWhiteCastlingRights() {
    return whiteCastlingRights;
  }

  /**
   * @return The castling type that denotes to what extent it would still be possible to castle for black regardless of whether it is
   * actually legally executable in the current position.
   */
  public byte getBlackCastlingRights() {
    return blackCastlingRights;
  }

  /**
   * @return A Zobrist key that is fairly close to a unique representation of the state of the instance in one 64 bit number.
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
   * @param numberOfTimes The hypothetical number of times the position has occurred before. E.g. for a three-fold repetition check, it
   * would be 2.
   * @return Whether the current position has already occurred before at least the specified number of times.
   */
  public boolean hasRepeated(int numberOfTimes) {
    int repetitions = 0;
    if (fiftyMoveRuleClock >= 4) {
      for (int i = halfMoveIndex - 4; i >= halfMoveIndex - fiftyMoveRuleClock; i -= 2) {
        if (keyHistory[i] == key) {
          repetitions++;
          if (repetitions >= numberOfTimes) {
            return true;
          }
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
    if (capturedPiece != Piece.NULL.ind) {
      if (capturedPiece == Piece.W_QUEEN.ind) {
        whiteQueens ^= toBit;
      } else if (capturedPiece == Piece.W_ROOK.ind) {
        whiteRooks ^= toBit;
      } else if (capturedPiece == Piece.W_BISHOP.ind) {
        whiteBishops ^= toBit;
      } else if (capturedPiece == Piece.W_KNIGHT.ind) {
        whiteKnights ^= toBit;
      } else {
        whitePawns ^= toBit;
      }
      allWhiteOccupied ^= toBit;
    }
  }

  private void captureBlackPieceOnBitboards(byte capturedPiece, long toBit) {
    if (capturedPiece != Piece.NULL.ind) {
      if (capturedPiece == Piece.B_QUEEN.ind) {
        blackQueens ^= toBit;
      } else if (capturedPiece == Piece.B_ROOK.ind) {
        blackRooks ^= toBit;
      } else if (capturedPiece == Piece.B_BISHOP.ind) {
        blackBishops ^= toBit;
      } else if (capturedPiece == Piece.B_KNIGHT.ind) {
        blackKnights ^= toBit;
      } else {
        blackPawns ^= toBit;
      }
      allBlackOccupied ^= toBit;
    }
  }

  private void makeWhiteNormalMoveOnBitboards(long fromBit, long toBit, byte movedPiece, byte capturedPiece) {
    long changedBits = fromBit | toBit;
    if (movedPiece == Piece.W_KING.ind) {
      whiteKing ^= changedBits;
    } else if (movedPiece == Piece.W_QUEEN.ind) {
      whiteQueens ^= changedBits;
    } else if (movedPiece == Piece.W_ROOK.ind) {
      whiteRooks ^= changedBits;
    } else if (movedPiece == Piece.W_BISHOP.ind) {
      whiteBishops ^= changedBits;
    } else if (movedPiece == Piece.W_KNIGHT.ind) {
      whiteKnights ^= changedBits;
    } else {
      whitePawns ^= changedBits;
    }
    allWhiteOccupied ^= changedBits;
    captureBlackPieceOnBitboards(capturedPiece, toBit);
    updateAggregateBitboards();
  }

  private void makeBlackNormalMoveOnBitboards(long fromBit, long toBit, byte movedPiece, byte capturedPiece) {
    long changedBits = fromBit | toBit;
    if (movedPiece == Piece.B_KING.ind) {
      blackKing ^= changedBits;
    } else if (movedPiece == Piece.B_QUEEN.ind) {
      blackQueens ^= changedBits;
    } else if (movedPiece == Piece.B_ROOK.ind) {
      blackRooks ^= changedBits;
    } else if (movedPiece == Piece.B_BISHOP.ind) {
      blackBishops ^= changedBits;
    } else if (movedPiece == Piece.B_KNIGHT.ind) {
      blackKnights ^= changedBits;
    } else {
      blackPawns ^= changedBits;
    }
    allBlackOccupied ^= changedBits;
    captureWhitePieceOnBitboards(capturedPiece, toBit);
    updateAggregateBitboards();
  }

  private void makeWhiteShortCastlingMoveOnBitboards() {
    long changedBits = Bitboard.Square.E1.bitboard | Bitboard.Square.G1.bitboard;
    whiteKing ^= changedBits;
    long rookChangedBits = Bitboard.Square.H1.bitboard | Bitboard.Square.F1.bitboard;
    whiteRooks ^= rookChangedBits;
    allWhiteOccupied ^= (changedBits | rookChangedBits);
    updateAggregateBitboards();
  }

  private void makeBlackShortCastlingMoveOnBitboards() {
    long changedBits = Bitboard.Square.E8.bitboard | Bitboard.Square.G8.bitboard;
    blackKing ^= changedBits;
    long rookChangedBits = Bitboard.Square.H8.bitboard | Bitboard.Square.F8.bitboard;
    blackRooks ^= rookChangedBits;
    allBlackOccupied ^= (changedBits | rookChangedBits);
    updateAggregateBitboards();
  }

  private void makeWhiteLongCastlingMoveOnBitboards() {
    long changedBits = Bitboard.Square.E1.bitboard | Bitboard.Square.C1.bitboard;
    whiteKing ^= changedBits;
    long rookChangedBits = Bitboard.Square.A1.bitboard | Bitboard.Square.D1.bitboard;
    whiteRooks ^= rookChangedBits;
    allWhiteOccupied ^= (changedBits | rookChangedBits);
    updateAggregateBitboards();
  }

  private void makeBlackLongCastlingMoveOnBitboards() {
    long changedBits = Bitboard.Square.E8.bitboard | Bitboard.Square.C8.bitboard;
    blackKing ^= changedBits;
    long rookChangedBits = Bitboard.Square.A8.bitboard | Bitboard.Square.D8.bitboard;
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
    squares[from] = Piece.NULL.ind;
    squares[to] = movedPiece;
    makeWhiteNormalMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), movedPiece, capturedPiece);
  }

  private void makeBlackNormalMoveOnBoard(byte from, byte to, byte movedPiece, byte capturedPiece) {
    squares[from] = Piece.NULL.ind;
    squares[to] = movedPiece;
    makeBlackNormalMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), movedPiece, capturedPiece);
  }

  private void makeWhiteShortCastlingMoveOnBoard() {
    squares[Bitboard.Square.H1.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.F1.ind] = Piece.W_ROOK.ind;
    squares[Bitboard.Square.E1.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.G1.ind] = Piece.W_KING.ind;
    makeWhiteShortCastlingMoveOnBitboards();
  }

  private void makeBlackShortCastlingMoveOnBoard() {
    squares[Bitboard.Square.H8.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.F8.ind] = Piece.B_ROOK.ind;
    squares[Bitboard.Square.E8.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.G8.ind] = Piece.B_KING.ind;
    makeBlackShortCastlingMoveOnBitboards();
  }

  private void makeWhiteLongCastlingMoveOnBoard() {
    squares[Bitboard.Square.A1.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.D1.ind] = Piece.W_ROOK.ind;
    squares[Bitboard.Square.E1.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.C1.ind] = Piece.W_KING.ind;
    makeWhiteLongCastlingMoveOnBitboards();
  }

  private void makeBlackLongCastlingMoveOnBoard() {
    squares[Bitboard.Square.A8.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.D8.ind] = Piece.B_ROOK.ind;
    squares[Bitboard.Square.E8.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.C8.ind] = Piece.B_KING.ind;
    makeBlackLongCastlingMoveOnBitboards();
  }

  private void makeWhiteEnPassantMoveOnBoard(byte from, byte to) {
    squares[from] = Piece.NULL.ind;
    squares[to] = Piece.W_PAWN.ind;
    squares[to - 8] = Piece.NULL.ind;
    makeWhiteEnPassantMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to));
  }

  private void makeBlackEnPassantMoveOnBoard(byte from, byte to) {
    squares[from] = Piece.NULL.ind;
    squares[to] = Piece.B_PAWN.ind;
    squares[to + 8] = Piece.NULL.ind;
    makeBlackEnPassantMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to));
  }

  private void makeWhiteQueenPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.NULL.ind;
    squares[to] = Piece.W_QUEEN.ind;
    makeWhiteQueenPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void makeBlackQueenPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.NULL.ind;
    squares[to] = Piece.B_QUEEN.ind;
    makeBlackQueenPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void makeWhiteRookPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.NULL.ind;
    squares[to] = Piece.W_ROOK.ind;
    makeWhiteRookPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void makeBlackRookPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.NULL.ind;
    squares[to] = Piece.B_ROOK.ind;
    makeBlackRookPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void makeWhiteBishopPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.NULL.ind;
    squares[to] = Piece.W_BISHOP.ind;
    makeWhiteBishopPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void makeBlackBishopPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.NULL.ind;
    squares[to] = Piece.B_BISHOP.ind;
    makeBlackBishopPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void makeWhiteKnightPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.NULL.ind;
    squares[to] = Piece.W_KNIGHT.ind;
    makeWhiteKnightPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void makeBlackKnightPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.NULL.ind;
    squares[to] = Piece.B_KNIGHT.ind;
    makeBlackKnightPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to),
        capturedPiece);
  }

  private void makeWhiteMoveAndUpdateKeyOnBoard(Move move) {
    byte moveType = move.type;
    ZobristKeyGenerator gen = ZobristKeyGenerator.getInstance();
    if (moveType == MoveType.NORMAL.ind) {
      makeWhiteNormalMoveOnBoard(move.from, move.to, move.movedPiece, move.capturedPiece);
      key = gen.getUpdatedBoardHashKeyAfterNormalMove(key, move.from, move.to, move.movedPiece,
          move.capturedPiece);
    } else if (moveType == MoveType.SHORT_CASTLING.ind) {
      makeWhiteShortCastlingMoveOnBoard();
      key = gen.getUpdatedBoardHashKeyAfterWhiteShortCastlinglMove(key);
    } else if (moveType == MoveType.LONG_CASTLING.ind) {
      makeWhiteLongCastlingMoveOnBoard();
      key = gen.getUpdatedBoardHashKeyAfterWhiteLongCastlinglMove(key);
    } else if (moveType == MoveType.EN_PASSANT.ind) {
      makeWhiteEnPassantMoveOnBoard(move.from, move.to);
      key = gen.getUpdatedBoardHashKeyAfterWhiteEnPassantMove(key, move.from, move.to);
    } else if (moveType == MoveType.PROMOTION_TO_QUEEN.ind) {
      makeWhiteQueenPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
      key = gen.getUpdatedBoardHashKeyAfterWhiteQueenPromotionMove(key, move.from, move.to,
          move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_ROOK.ind) {
      makeWhiteRookPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
      key = gen.getUpdatedBoardHashKeyAfterWhiteRookPromotionMove(key, move.from, move.to,
          move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_BISHOP.ind) {
      makeWhiteBishopPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
      key = gen.getUpdatedBoardHashKeyAfterWhiteBishopPromotionMove(key, move.from, move.to,
          move.capturedPiece);
    } else {
      makeWhiteKnightPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
      key = gen.getUpdatedBoardHashKeyAfterWhiteKnightPromotionMove(key, move.from, move.to,
          move.capturedPiece);
    }
  }

  private void makeBlackMoveAndUpdateKeyOnBoard(Move move) {
    byte moveType = move.type;
    ZobristKeyGenerator gen = ZobristKeyGenerator.getInstance();
    if (moveType == MoveType.NORMAL.ind) {
      makeBlackNormalMoveOnBoard(move.from, move.to, move.movedPiece, move.capturedPiece);
      key = gen.getUpdatedBoardHashKeyAfterNormalMove(key, move.from, move.to, move.movedPiece,
          move.capturedPiece);
    } else if (moveType == MoveType.SHORT_CASTLING.ind) {
      makeBlackShortCastlingMoveOnBoard();
      key = gen.getUpdatedBoardHashKeyAfterBlackShortCastlinglMove(key);
    } else if (moveType == MoveType.LONG_CASTLING.ind) {
      makeBlackLongCastlingMoveOnBoard();
      key = gen.getUpdatedBoardHashKeyAfterBlackLongCastlinglMove(key);
    } else if (moveType == MoveType.EN_PASSANT.ind) {
      makeBlackEnPassantMoveOnBoard(move.from, move.to);
      key = gen.getUpdatedBoardHashKeyAfterBlackEnPassantMove(key, move.from, move.to);
    } else if (moveType == MoveType.PROMOTION_TO_QUEEN.ind) {
      makeBlackQueenPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
      key = gen.getUpdatedBoardHashKeyAfterBlackQueenPromotionMove(key, move.from, move.to,
          move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_ROOK.ind) {
      makeBlackRookPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
      key = gen.getUpdatedBoardHashKeyAfterBlackRookPromotionMove(key, move.from, move.to,
          move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_BISHOP.ind) {
      makeBlackBishopPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
      key = gen.getUpdatedBoardHashKeyAfterBlackBishopPromotionMove(key, move.from, move.to,
          move.capturedPiece);
    } else {
      makeBlackKnightPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
      key = gen.getUpdatedBoardHashKeyAfterBlackKnightPromotionMove(key, move.from, move.to,
          move.capturedPiece);
    }
  }

  private void updateWhiteCastlingRights() {
    if (whiteCastlingRights == CastlingRights.NONE.ind) {
      return;
    }
    if (whiteCastlingRights == CastlingRights.SHORT.ind) {
      if (squares[Bitboard.Square.E1.ind] != Piece.W_KING.ind ||
          squares[Bitboard.Square.H1.ind] != Piece.W_ROOK.ind) {
        whiteCastlingRights = CastlingRights.NONE.ind;
      }
    } else if (whiteCastlingRights == CastlingRights.LONG.ind) {
      if (squares[Bitboard.Square.E1.ind] != Piece.W_KING.ind ||
          squares[Bitboard.Square.A1.ind] != Piece.W_ROOK.ind) {
        whiteCastlingRights = CastlingRights.NONE.ind;
      }
    } else {
      if (squares[Bitboard.Square.E1.ind] == Piece.W_KING.ind) {
        if (squares[Bitboard.Square.H1.ind] != Piece.W_ROOK.ind) {
          if (squares[Bitboard.Square.A1.ind] != Piece.W_ROOK.ind) {
            whiteCastlingRights = CastlingRights.NONE.ind;
          } else {
            whiteCastlingRights = CastlingRights.LONG.ind;
          }
        } else if (squares[Bitboard.Square.A1.ind] != Piece.W_ROOK.ind) {
          whiteCastlingRights = CastlingRights.SHORT.ind;
        }
      } else {
        whiteCastlingRights = CastlingRights.NONE.ind;
      }
    }
  }

  private void updateBlackCastlingRights() {
    if (blackCastlingRights == CastlingRights.NONE.ind) {
      return;
    }
    if (blackCastlingRights == CastlingRights.SHORT.ind) {
      if (squares[Bitboard.Square.E8.ind] != Piece.B_KING.ind ||
          squares[Bitboard.Square.H8.ind] != Piece.B_ROOK.ind) {
        blackCastlingRights = CastlingRights.NONE.ind;
      }
    } else if (blackCastlingRights == CastlingRights.LONG.ind) {
      if (squares[Bitboard.Square.E8.ind] != Piece.B_KING.ind ||
          squares[Bitboard.Square.A8.ind] != Piece.B_ROOK.ind) {
        blackCastlingRights = CastlingRights.NONE.ind;
      }
    } else {
      if (squares[Bitboard.Square.E8.ind] == Piece.B_KING.ind) {
        if (squares[Bitboard.Square.H8.ind] != Piece.B_ROOK.ind) {
          if (squares[Bitboard.Square.A8.ind] != Piece.B_ROOK.ind) {
            blackCastlingRights = CastlingRights.NONE.ind;
          } else {
            blackCastlingRights = CastlingRights.LONG.ind;
          }
        } else if (squares[Bitboard.Square.A8.ind] != Piece.B_ROOK.ind) {
          blackCastlingRights = CastlingRights.SHORT.ind;
        }
      } else {
        blackCastlingRights = CastlingRights.NONE.ind;
      }
    }
  }

  private void ensureKeyHistoryCapacity() {
    if (keyHistory.length - halfMoveIndex <= 3) {
      keyHistory = Arrays.copyOf(keyHistory, keyHistory.length + (keyHistory.length >> 1));
    }
  }

  private long getWhiteCheckers(int sqrInd) {
    MoveSetBase dB = MoveSetBase.getByIndex(sqrInd);
    long attackers = whiteKnights & dB.knightMoveMask;
    attackers |= whitePawns & dB.blackPawnCaptureMoveMask;
    attackers |= (whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied);
    attackers |= (whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
    return attackers;
  }

  private long getBlackCheckers(int sqrInd) {
    MoveSetBase dB = MoveSetBase.getByIndex(sqrInd);
    long attackers = blackKnights & dB.knightMoveMask;
    attackers |= blackPawns & dB.whitePawnCaptureMoveMask;
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
      enPassantRights = (move.movedPiece == Piece.W_PAWN.ind &&
          move.to - move.from == 16) ? (byte) (move.to % 8) : 8;
      fiftyMoveRuleClock = (move.capturedPiece != Piece.NULL.ind ||
          move.movedPiece == Piece.W_PAWN.ind) ? 0 : (byte) (fiftyMoveRuleClock + 1);
    } else {
      makeBlackMoveAndUpdateKeyOnBoard(move);
      checkers = getBlackCheckers(BitOperations.indexOfBit(whiteKing));
      updateWhiteCastlingRights();
      enPassantRights = (move.movedPiece == Piece.B_PAWN.ind &&
          move.from - move.to == 16) ? (byte) (move.to % 8) : 8;
      fiftyMoveRuleClock = (move.capturedPiece != Piece.NULL.ind ||
          move.movedPiece == Piece.B_PAWN.ind) ? 0 : (byte) (fiftyMoveRuleClock + 1);
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
    enPassantRights = EnPassantRights.NONE.ind;
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
    squares[Bitboard.Square.F1.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.H1.ind] = Piece.W_ROOK.ind;
    squares[Bitboard.Square.G1.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.E1.ind] = Piece.W_KING.ind;
    makeWhiteShortCastlingMoveOnBitboards();
  }

  private void unmakeBlackShortCastlingMoveOnBoard() {
    squares[Bitboard.Square.F8.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.H8.ind] = Piece.B_ROOK.ind;
    squares[Bitboard.Square.G8.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.E8.ind] = Piece.B_KING.ind;
    makeBlackShortCastlingMoveOnBitboards();
  }

  private void unmakeWhiteLongCastlingMoveOnBoard() {
    squares[Bitboard.Square.D1.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.A1.ind] = Piece.W_ROOK.ind;
    squares[Bitboard.Square.C1.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.E1.ind] = Piece.W_KING.ind;
    makeWhiteLongCastlingMoveOnBitboards();
  }

  private void unmakeBlackLongCastlingMoveOnBoard() {
    squares[Bitboard.Square.D8.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.A8.ind] = Piece.B_ROOK.ind;
    squares[Bitboard.Square.C8.ind] = Piece.NULL.ind;
    squares[Bitboard.Square.E8.ind] = Piece.B_KING.ind;
    makeBlackLongCastlingMoveOnBitboards();
  }

  private void unmakeWhiteEnPassantMoveOnBoard(byte from, byte to) {
    squares[from] = Piece.W_PAWN.ind;
    squares[to] = Piece.NULL.ind;
    squares[to - 8] = Piece.B_PAWN.ind;
    makeWhiteEnPassantMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to));
  }

  private void unmakeBlackEnPassantMoveOnBoard(byte from, byte to) {
    squares[from] = Piece.B_PAWN.ind;
    squares[to] = Piece.NULL.ind;
    squares[to + 8] = Piece.W_PAWN.ind;
    makeBlackEnPassantMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to));
  }

  private void unmakeWhiteQueenPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.W_PAWN.ind;
    squares[to] = capturedPiece;
    makeWhiteQueenPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void unmakeBlackQueenPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.B_PAWN.ind;
    squares[to] = capturedPiece;
    makeBlackQueenPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void unmakeWhiteRookPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.W_PAWN.ind;
    squares[to] = capturedPiece;
    makeWhiteRookPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void unmakeBlackRookPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.B_PAWN.ind;
    squares[to] = capturedPiece;
    makeBlackRookPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void unmakeWhiteBishopPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.W_PAWN.ind;
    squares[to] = capturedPiece;
    makeWhiteBishopPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void unmakeBlackBishopPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.B_PAWN.ind;
    squares[to] = capturedPiece;
    makeBlackBishopPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void unmakeWhiteKnightPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.W_PAWN.ind;
    squares[to] = capturedPiece;
    makeWhiteKnightPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void unmakeBlackKnightPromotionMoveOnBoard(byte from, byte to, byte capturedPiece) {
    squares[from] = Piece.B_PAWN.ind;
    squares[to] = capturedPiece;
    makeBlackKnightPromotionMoveOnBitboards(BitOperations.toBit(from), BitOperations.toBit(to), capturedPiece);
  }

  private void unmakeWhiteMoveOnBoard(Move move) {
    byte moveType = move.type;
    if (moveType == MoveType.NORMAL.ind) {
      unmakeWhiteNormalMoveOnBoard(move.from, move.to, move.movedPiece, move.capturedPiece);
    } else if (moveType == MoveType.SHORT_CASTLING.ind) {
      unmakeWhiteShortCastlingMoveOnBoard();
    } else if (moveType == MoveType.LONG_CASTLING.ind) {
      unmakeWhiteLongCastlingMoveOnBoard();
    } else if (moveType == MoveType.EN_PASSANT.ind) {
      unmakeWhiteEnPassantMoveOnBoard(move.from, move.to);
    } else if (moveType == MoveType.PROMOTION_TO_QUEEN.ind) {
      unmakeWhiteQueenPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_ROOK.ind) {
      unmakeWhiteRookPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_BISHOP.ind) {
      unmakeWhiteBishopPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
    } else {
      unmakeWhiteKnightPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
    }
  }

  private void unmakeBlackMoveOnBoard(Move move) {
    byte moveType = move.type;
    if (moveType == MoveType.NORMAL.ind) {
      unmakeBlackNormalMoveOnBoard(move.from, move.to, move.movedPiece, move.capturedPiece);
    } else if (moveType == MoveType.SHORT_CASTLING.ind) {
      unmakeBlackShortCastlingMoveOnBoard();
    } else if (moveType == MoveType.LONG_CASTLING.ind) {
      unmakeBlackLongCastlingMoveOnBoard();
    } else if (moveType == MoveType.EN_PASSANT.ind) {
      unmakeBlackEnPassantMoveOnBoard(move.from, move.to);
    } else if (moveType == MoveType.PROMOTION_TO_QUEEN.ind) {
      unmakeBlackQueenPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_ROOK.ind) {
      unmakeBlackRookPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_BISHOP.ind) {
      unmakeBlackBishopPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
    } else {
      unmakeBlackKnightPromotionMoveOnBoard(move.from, move.to, move.capturedPiece);
    }
  }

  /**
   * Takes back the last move made and returns it. If no move has been made yet, it returns null.
   *
   * @return The move taken back or null if no move has been made yet.
   */
  public Move unmakeMove() {
    PositionStateRecord prevState = stateHistory.poll();
    if (prevState == null) {
      return null;
    }
    Move move = moveHistory.pop();
    whitesTurn = !whitesTurn;
    if (!move.equals(Move.NULL_MOVE)) {
      if (whitesTurn) {
        unmakeWhiteMoveOnBoard(move);
      } else {
        unmakeBlackMoveOnBoard(move);
      }
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
    MoveSetBase dB = MoveSetBase.getByIndex(sqrInd);
    return ((whiteKnights & dB.knightMoveMask) != Bitboard.EMPTY_BOARD ||
        (whitePawns & dB.blackPawnCaptureMoveMask) != Bitboard.EMPTY_BOARD ||
        ((whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied)) !=
            Bitboard.EMPTY_BOARD ||
        ((whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied)) !=
            Bitboard.EMPTY_BOARD);
  }

  private boolean isCheckedByBlack(int sqrInd) {
    MoveSetBase dB = MoveSetBase.getByIndex(sqrInd);
    return ((whiteKnights & dB.knightMoveMask) != Bitboard.EMPTY_BOARD ||
        (whitePawns & dB.blackPawnCaptureMoveMask) != Bitboard.EMPTY_BOARD ||
        ((whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied)) !=
            Bitboard.EMPTY_BOARD ||
        ((whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied)) !=
            Bitboard.EMPTY_BOARD);
  }

  private boolean givesWhiteCheck(Move move) {
    byte moveType = move.type;
    boolean givesCheck;
    if (moveType == MoveType.NORMAL.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeBlackNormalMoveOnBitboards(fromBit, toBit, move.movedPiece, move.capturedPiece);
      givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
      makeBlackNormalMoveOnBitboards(fromBit, toBit, move.movedPiece, move.capturedPiece);
    } else if (moveType == MoveType.SHORT_CASTLING.ind) {
      makeBlackShortCastlingMoveOnBitboards();
      givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
      makeBlackShortCastlingMoveOnBitboards();
    } else if (moveType == MoveType.LONG_CASTLING.ind) {
      makeBlackLongCastlingMoveOnBitboards();
      givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
      makeBlackLongCastlingMoveOnBitboards();
    } else if (moveType == MoveType.EN_PASSANT.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeBlackEnPassantMoveOnBitboards(fromBit, toBit);
      givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
      makeBlackEnPassantMoveOnBitboards(fromBit, toBit);
    } else if (moveType == MoveType.PROMOTION_TO_QUEEN.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeBlackQueenPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
      makeBlackQueenPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_ROOK.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeBlackRookPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
      makeBlackRookPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_BISHOP.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeBlackBishopPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
      makeBlackBishopPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    } else {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeBlackKnightPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      givesCheck = isCheckedByBlack(BitOperations.indexOfBit(whiteKing));
      makeBlackKnightPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    }
    return givesCheck;
  }

  private boolean givesBlackCheck(Move move) {
    byte moveType = move.type;
    boolean givesCheck;
    if (moveType == MoveType.NORMAL.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeWhiteNormalMoveOnBitboards(fromBit, toBit, move.movedPiece, move.capturedPiece);
      givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
      makeWhiteNormalMoveOnBitboards(fromBit, toBit, move.movedPiece, move.capturedPiece);
    } else if (moveType == MoveType.SHORT_CASTLING.ind) {
      makeWhiteShortCastlingMoveOnBitboards();
      givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
      makeWhiteShortCastlingMoveOnBitboards();
    } else if (moveType == MoveType.LONG_CASTLING.ind) {
      makeWhiteLongCastlingMoveOnBitboards();
      givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
      makeWhiteLongCastlingMoveOnBitboards();
    } else if (moveType == MoveType.EN_PASSANT.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeWhiteEnPassantMoveOnBitboards(fromBit, toBit);
      givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
      makeWhiteEnPassantMoveOnBitboards(fromBit, toBit);
    } else if (moveType == MoveType.PROMOTION_TO_QUEEN.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeWhiteQueenPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
      makeWhiteQueenPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_ROOK.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeWhiteRookPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
      makeWhiteRookPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_BISHOP.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeWhiteBishopPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
      makeWhiteBishopPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    } else {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeWhiteKnightPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      givesCheck = isCheckedByWhite(BitOperations.indexOfBit(blackKing));
      makeWhiteKnightPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
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
    MoveSetBase dB = MoveSetBase.getByIndex(sqrInd);
    return ((whiteKnights & dB.knightMoveMask) != Bitboard.EMPTY_BOARD ||
        (whitePawns & dB.blackPawnCaptureMoveMask) != Bitboard.EMPTY_BOARD ||
        ((whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied)) !=
            Bitboard.EMPTY_BOARD ||
        ((whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied)) !=
            Bitboard.EMPTY_BOARD ||
        (whiteKing & dB.kingMoveMask) != Bitboard.EMPTY_BOARD ||
        (squares[sqrInd] == Piece.B_PAWN.ind && enPassantRights != EnPassantRights.NONE.ind &&
            sqrInd == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights &&
            (whitePawns & dB.kingMoveMask & Bitboard.Rank.R5.bitboard) != Bitboard.EMPTY_BOARD));
  }

  private boolean isAttackedByBlack(int sqrInd) {
    MoveSetBase dB = MoveSetBase.getByIndex(sqrInd);
    return ((blackKnights & dB.knightMoveMask) != Bitboard.EMPTY_BOARD ||
        (blackPawns & dB.whitePawnCaptureMoveMask) != Bitboard.EMPTY_BOARD ||
        ((blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied)) !=
            Bitboard.EMPTY_BOARD ||
        ((blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied)) !=
            Bitboard.EMPTY_BOARD ||
        (blackKing & dB.kingMoveMask) != Bitboard.EMPTY_BOARD ||
        (squares[sqrInd] == Piece.W_PAWN.ind && enPassantRights != EnPassantRights.NONE.ind &&
            sqrInd == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights &&
            (blackPawns & dB.kingMoveMask & Bitboard.Rank.R4.bitboard) != Bitboard.EMPTY_BOARD));
  }

  private boolean leavesWhiteChecked(Move move) {
    byte moveType = move.type;
    boolean leavesChecked;
    if (moveType == MoveType.NORMAL.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeWhiteNormalMoveOnBitboards(fromBit, toBit, move.movedPiece, move.capturedPiece);
      leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
      makeWhiteNormalMoveOnBitboards(fromBit, toBit, move.movedPiece, move.capturedPiece);
    } else if (moveType == MoveType.SHORT_CASTLING.ind) {
      makeWhiteShortCastlingMoveOnBitboards();
      leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
      makeWhiteShortCastlingMoveOnBitboards();
    } else if (moveType == MoveType.LONG_CASTLING.ind) {
      makeWhiteLongCastlingMoveOnBitboards();
      leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
      makeWhiteLongCastlingMoveOnBitboards();
    } else if (moveType == MoveType.EN_PASSANT.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeWhiteEnPassantMoveOnBitboards(fromBit, toBit);
      leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
      makeWhiteEnPassantMoveOnBitboards(fromBit, toBit);
    } else if (moveType == MoveType.PROMOTION_TO_QUEEN.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeWhiteQueenPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
      makeWhiteQueenPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_ROOK.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeWhiteRookPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
      makeWhiteRookPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_BISHOP.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeWhiteBishopPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
      makeWhiteBishopPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    } else {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeWhiteKnightPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      leavesChecked = isAttackedByBlack(BitOperations.indexOfBit(whiteKing));
      makeWhiteKnightPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    }
    return leavesChecked;
  }

  private boolean leavesBlackChecked(Move move) {
    byte moveType = move.type;
    boolean leavesChecked;
    if (moveType == MoveType.NORMAL.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeBlackNormalMoveOnBitboards(fromBit, toBit, move.movedPiece, move.capturedPiece);
      leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
      makeBlackNormalMoveOnBitboards(fromBit, toBit, move.movedPiece, move.capturedPiece);
    } else if (moveType == MoveType.SHORT_CASTLING.ind) {
      makeBlackShortCastlingMoveOnBitboards();
      leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
      makeBlackShortCastlingMoveOnBitboards();
    } else if (moveType == MoveType.LONG_CASTLING.ind) {
      makeBlackLongCastlingMoveOnBitboards();
      leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
      makeBlackLongCastlingMoveOnBitboards();
    } else if (moveType == MoveType.EN_PASSANT.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeBlackEnPassantMoveOnBitboards(fromBit, toBit);
      leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
      makeBlackEnPassantMoveOnBitboards(fromBit, toBit);
    } else if (moveType == MoveType.PROMOTION_TO_QUEEN.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeBlackQueenPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
      makeBlackQueenPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_ROOK.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeBlackRookPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
      makeBlackRookPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    } else if (moveType == MoveType.PROMOTION_TO_BISHOP.ind) {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeBlackBishopPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
      makeBlackBishopPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    } else {
      long fromBit = BitOperations.toBit(move.from);
      long toBit = BitOperations.toBit(move.to);
      makeBlackKnightPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
      leavesChecked = isAttackedByWhite(BitOperations.indexOfBit(blackKing));
      makeBlackKnightPromotionMoveOnBitboards(fromBit, toBit, move.capturedPiece);
    }
    return leavesChecked;
  }

  private boolean isLegalForWhite(Move move) {
    long moveSet;
    byte movedPiece = move.movedPiece;
    if (squares[move.from] != movedPiece) {
      return false;
    }
    long toBit = BitOperations.toBit(move.to);
    if (movedPiece == Piece.W_PAWN.ind) {
      moveSet = Bitboard.EMPTY_BOARD;
      if (move.type == MoveType.EN_PASSANT.ind && enPassantRights != EnPassantRights.NONE.ind &&
          move.to == EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights) {
        moveSet |= toBit;
      } else if (squares[move.to] != move.capturedPiece) {
        return false;
      }
      moveSet |= MoveSetBase.getByIndex(move.from).getWhitePawnMoveSet(allBlackOccupied, allEmpty);
    } else {
      if (squares[move.to] != move.capturedPiece) {
        return false;
      }
      MoveSetBase dB = MoveSetBase.getByIndex(move.from);
      if (movedPiece == Piece.W_KING.ind) {
        if (move.type == MoveType.SHORT_CASTLING.ind) {
          return !inCheck && (whiteCastlingRights == CastlingRights.SHORT.ind ||
              whiteCastlingRights == CastlingRights.ALL.ind) &&
              ((Bitboard.Square.F1.bitboard | Bitboard.Square.G1.bitboard) &
                  allOccupied) == Bitboard.EMPTY_BOARD &&
              squares[Bitboard.Square.H1.ind] == Piece.W_ROOK.ind &&
              !isAttackedByBlack(Bitboard.Square.F1.ind) &&
              !isAttackedByBlack(Bitboard.Square.G1.ind);
        } else if (move.type == MoveType.LONG_CASTLING.ind) {
          return !inCheck && (whiteCastlingRights == CastlingRights.LONG.ind ||
              whiteCastlingRights == CastlingRights.ALL.ind) &&
              ((Bitboard.Square.B1.bitboard | Bitboard.Square.C1.bitboard |
                  Bitboard.Square.D1.bitboard) & allOccupied) == Bitboard.EMPTY_BOARD &&
              squares[Bitboard.Square.A1.ind] == Piece.W_ROOK.ind &&
              !isAttackedByBlack(Bitboard.Square.C1.ind) &&
              !isAttackedByBlack(Bitboard.Square.D1.ind);
        } else {
          moveSet = dB.getKingMoveSet(allNonWhiteOccupied);
        }
      } else if (movedPiece == Piece.W_QUEEN.ind) {
        moveSet = dB.getQueenMoveSet(allNonWhiteOccupied, allOccupied);
      } else if (movedPiece == Piece.W_ROOK.ind) {
        moveSet = dB.getRookMoveSet(allNonWhiteOccupied, allOccupied);
      } else if (movedPiece == Piece.W_BISHOP.ind) {
        moveSet = dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied);
      } else if (movedPiece == Piece.W_KNIGHT.ind) {
        moveSet = dB.getKnightMoveSet(allNonWhiteOccupied);
      } else {
        return false;
      }
    }
    return (moveSet & toBit) != Bitboard.EMPTY_BOARD && !leavesWhiteChecked(move);
  }

  private boolean isLegalForBlack(Move move) {
    long moveSet;
    byte movedPiece = move.movedPiece;
    if (squares[move.from] != movedPiece) {
      return false;
    }
    long toBit = BitOperations.toBit(move.to);
    if (movedPiece == Piece.B_PAWN.ind) {
      moveSet = Bitboard.EMPTY_BOARD;
      if (move.type == MoveType.EN_PASSANT.ind && enPassantRights != EnPassantRights.NONE.ind &&
          move.to == EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights) {
        moveSet |= toBit;
      } else if (squares[move.to] != move.capturedPiece) {
        return false;
      }
      moveSet |= MoveSetBase.getByIndex(move.from).getBlackPawnMoveSet(allWhiteOccupied, allEmpty);
    } else {
      if (squares[move.to] != move.capturedPiece) {
        return false;
      }
      MoveSetBase dB = MoveSetBase.getByIndex(move.from);
      if (movedPiece == Piece.B_KING.ind) {
        if (move.type == MoveType.SHORT_CASTLING.ind) {
          return !inCheck && (blackCastlingRights == CastlingRights.SHORT.ind ||
              blackCastlingRights == CastlingRights.ALL.ind) &&
              ((Bitboard.Square.F8.bitboard | Bitboard.Square.G8.bitboard) &
                  allOccupied) == Bitboard.EMPTY_BOARD &&
              squares[Bitboard.Square.H8.ind] == Piece.B_ROOK.ind &&
              !isAttackedByWhite(Bitboard.Square.F8.ind) &&
              !isAttackedByWhite(Bitboard.Square.G8.ind);
        } else if (move.type == MoveType.LONG_CASTLING.ind) {
          return !inCheck && (blackCastlingRights == CastlingRights.LONG.ind ||
              blackCastlingRights == CastlingRights.ALL.ind) &&
              ((Bitboard.Square.B8.bitboard | Bitboard.Square.C8.bitboard |
                  Bitboard.Square.D8.bitboard) & allOccupied) == Bitboard.EMPTY_BOARD &&
              squares[Bitboard.Square.A8.ind] == Piece.B_ROOK.ind &&
              !isAttackedByWhite(Bitboard.Square.C8.ind) &&
              !isAttackedByWhite(Bitboard.Square.D8.ind);
        } else {
          moveSet = dB.getKingMoveSet(allNonBlackOccupied);
        }
      } else if (movedPiece == Piece.B_QUEEN.ind) {
        moveSet = dB.getQueenMoveSet(allNonBlackOccupied, allOccupied);
      } else if (movedPiece == Piece.B_ROOK.ind) {
        moveSet = dB.getRookMoveSet(allNonBlackOccupied, allOccupied);
      } else if (movedPiece == Piece.B_BISHOP.ind) {
        moveSet = dB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
      } else if (movedPiece == Piece.B_KNIGHT.ind) {
        moveSet = dB.getKnightMoveSet(allNonBlackOccupied);
      } else {
        return false;
      }
    }
    return (moveSet & toBit) != Bitboard.EMPTY_BOARD && !leavesBlackChecked(move);
  }

  /**
   * Determines whether the move is legal in the current position. It assumes that there exists a position in which the move is legal and
   * checks if this one is one of them. This method may fail to detect inconsistencies in the move attributes. A fail safe way of legality
   * checking is generating all the moves for the current position and see if the move is among them.
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
      byte from = BitOperations.indexOfBit(pinnedPiece);
      byte pinnedPieceType = squares[from];
      if (pinnedPieceType == queenType || pinnedPieceType == rookType) {
        byte to = BitOperations.indexOfBit(pinningPiece);
        moves.add(new Move(from, to, pinnedPieceType, squares[to], MoveType.NORMAL.ind));
      }
      return pinnedPiece;
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addWhiteTacticalDiagonalPinnedPieceMoveAndGetPinnedPiece(long pinnedPiece, long pinningPiece,
      long ray, boolean positive, List<Move> moves) {
    if (pinningPiece != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfBit(pinnedPiece);
      byte pinnedPieceType = squares[from];
      if (pinnedPieceType == Piece.W_QUEEN.ind || pinnedPieceType == Piece.W_BISHOP.ind) {
        byte to = BitOperations.indexOfBit(pinningPiece);
        moves.add(new Move(from, to, pinnedPieceType, squares[to], MoveType.NORMAL.ind));
      } else if (pinnedPieceType == Piece.W_PAWN.ind) {
        if (positive) {
          byte to = BitOperations.indexOfBit(pinningPiece);
          if ((pinnedPiece & Bitboard.Rank.R7.bitboard) != Bitboard.EMPTY_BOARD) {
            addPromotionMoves(from, to, pinnedPieceType, squares[to], moves);
            return pinnedPiece;
          } else if (Bitboard.computeWhitePawnCaptureSets(pinnedPiece, pinningPiece) != Bitboard.EMPTY_BOARD) {
            moves.add(new Move(from, to, pinnedPieceType, squares[to], MoveType.NORMAL.ind));
            return pinnedPiece;
          }
        }
        if (enPassantRights != EnPassantRights.NONE.ind &&
            Bitboard.computeWhitePawnCaptureSets(pinnedPiece, BitOperations.toBit(
                EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights) & ray) != Bitboard.EMPTY_BOARD) {
          moves.add(new Move(from, BitOperations.indexOfBit(pinningPiece), pinnedPieceType,
              Piece.B_PAWN.ind, MoveType.EN_PASSANT.ind));
        }
      }
      return pinnedPiece;
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addBlackTacticalDiagonalPinnedPieceMoveAndGetPinnedPiece(long pinnedPiece, long pinningPiece,
      long ray, boolean positive, List<Move> moves) {
    if (pinningPiece != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfBit(pinnedPiece);
      byte pinnedPieceType = squares[from];
      if (pinnedPieceType == Piece.B_QUEEN.ind || pinnedPieceType == Piece.B_BISHOP.ind) {
        byte to = BitOperations.indexOfBit(pinningPiece);
        moves.add(new Move(from, to, pinnedPieceType, squares[to], MoveType.NORMAL.ind));
      } else if (pinnedPieceType == Piece.B_PAWN.ind) {
        if (!positive) {
          byte to = BitOperations.indexOfBit(pinningPiece);
          if ((pinnedPiece & Bitboard.Rank.R2.bitboard) != Bitboard.EMPTY_BOARD) {
            addPromotionMoves(from, to, pinnedPieceType, squares[to], moves);
            return pinnedPiece;
          } else if (Bitboard.computeBlackPawnCaptureSets(pinnedPiece, pinningPiece) != Bitboard.EMPTY_BOARD) {
            moves.add(new Move(from, to, pinnedPieceType, squares[to], MoveType.NORMAL.ind));
            return pinnedPiece;
          }
        }
        if (enPassantRights != EnPassantRights.NONE.ind &&
            Bitboard.computeBlackPawnCaptureSets(pinnedPiece, BitOperations.toBit(
                EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights) & ray) != Bitboard.EMPTY_BOARD) {
          moves.add(new Move(from, BitOperations.indexOfBit(pinningPiece), pinnedPieceType,
              Piece.W_PAWN.ind, MoveType.EN_PASSANT.ind));
        }
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
          BitOperations.getLSBit(rayOccupancy ^ pinnedPiece) & sliders, queenType, rookType, moves);
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addTacticalNegativeStraightPinnedPieceMoveAndGetPinnedPiece(long rayOccupancy, long allSameColorOccupied,
      long sliders, byte queenType, byte rookType, List<Move> moves) {
    long pinnedPiece = BitOperations.getMSBit(rayOccupancy) & allSameColorOccupied;
    if (pinnedPiece != Bitboard.EMPTY_BOARD) {
      return addTacticalStraightPinnedPieceMoveAndGetPinnedPiece(pinnedPiece,
          BitOperations.getMSBit(rayOccupancy ^ pinnedPiece) & sliders, queenType, rookType, moves);
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addWhiteTacticalPositiveDiagonalPinnedPieceMoveAndGetPinnedPiece(long ray, long sliders,
      List<Move> moves) {
    long rayOccupancy = ray & allOccupied;
    long pinnedPiece = BitOperations.getLSBit(rayOccupancy) & allWhiteOccupied;
    if (pinnedPiece != Bitboard.EMPTY_BOARD) {
      return addWhiteTacticalDiagonalPinnedPieceMoveAndGetPinnedPiece(pinnedPiece,
          BitOperations.getLSBit(rayOccupancy ^ pinnedPiece) & sliders, ray, true, moves);
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addBlackTacticalPositiveDiagonalPinnedPieceMoveAndGetPinnedPiece(long ray, long sliders,
      List<Move> moves) {
    long rayOccupancy = ray & allOccupied;
    long pinnedPiece = BitOperations.getLSBit(rayOccupancy) & allBlackOccupied;
    if (pinnedPiece != Bitboard.EMPTY_BOARD) {
      return addBlackTacticalDiagonalPinnedPieceMoveAndGetPinnedPiece(pinnedPiece,
          BitOperations.getLSBit(rayOccupancy ^ pinnedPiece) & sliders, ray, true, moves);
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addWhiteTacticalNegativeDiagonalPinnedPieceMoveAndGetPinnedPiece(long ray, long sliders,
      List<Move> moves) {
    long rayOccupancy = ray & allOccupied;
    long pinnedPiece = BitOperations.getMSBit(rayOccupancy) & allWhiteOccupied;
    if (pinnedPiece != Bitboard.EMPTY_BOARD) {
      return addWhiteTacticalDiagonalPinnedPieceMoveAndGetPinnedPiece(pinnedPiece,
          BitOperations.getMSBit(rayOccupancy ^ pinnedPiece) & sliders, ray, false, moves);
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addBlackTacticalNegativeDiagonalPinnedPieceMoveAndGetPinnedPiece(long ray, long sliders,
      List<Move> moves) {
    long rayOccupancy = ray & allOccupied;
    long pinnedPiece = BitOperations.getMSBit(rayOccupancy) & allBlackOccupied;
    if (pinnedPiece != Bitboard.EMPTY_BOARD) {
      return addBlackTacticalDiagonalPinnedPieceMoveAndGetPinnedPiece(pinnedPiece,
          BitOperations.getMSBit(rayOccupancy ^ pinnedPiece) & sliders, ray, false, moves);
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addWhiteTacticalPinnedPieceMovesAndGetPinnedPieces(byte kingInd, List<Move> moves) {
    Bitboard.Rays rays = Bitboard.Rays.values()[kingInd];
    long straightSliders = blackQueens | blackRooks;
    long diagonalSliders = blackQueens | blackBishops;
    long pinnedPieces = addTacticalPositiveStraightPinnedPieceMoveAndGetPinnedPiece(rays.rankPos & allOccupied,
        allWhiteOccupied, straightSliders, Piece.W_QUEEN.ind, Piece.W_ROOK.ind, moves);
    pinnedPieces |= addTacticalPositiveStraightPinnedPieceMoveAndGetPinnedPiece(rays.filePos & allOccupied,
        allWhiteOccupied, straightSliders, Piece.W_QUEEN.ind, Piece.W_ROOK.ind, moves);
    pinnedPieces |= addWhiteTacticalPositiveDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.diagonalPos,
        diagonalSliders, moves);
    pinnedPieces |= addWhiteTacticalPositiveDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.antiDiagonalPos,
        diagonalSliders, moves);
    pinnedPieces |= addTacticalNegativeStraightPinnedPieceMoveAndGetPinnedPiece(rays.rankNeg & allOccupied,
        allWhiteOccupied, straightSliders, Piece.W_QUEEN.ind, Piece.W_ROOK.ind, moves);
    pinnedPieces |= addTacticalNegativeStraightPinnedPieceMoveAndGetPinnedPiece(rays.fileNeg & allOccupied,
        allWhiteOccupied, straightSliders, Piece.W_QUEEN.ind, Piece.W_ROOK.ind, moves);
    pinnedPieces |= addWhiteTacticalNegativeDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.diagonalNeg,
        diagonalSliders, moves);
    pinnedPieces |= addWhiteTacticalNegativeDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.antiDiagonalNeg,
        diagonalSliders, moves);
    return pinnedPieces;
  }

  private long addBlackTacticalPinnedPieceMovesAndGetPinnedPieces(byte kingInd, List<Move> moves) {
    Bitboard.Rays rays = Bitboard.Rays.values()[kingInd];
    long straightSliders = whiteQueens | whiteRooks;
    long diagonalSliders = whiteQueens | whiteBishops;
    long pinnedPieces = addTacticalPositiveStraightPinnedPieceMoveAndGetPinnedPiece(rays.rankPos & allOccupied,
        allBlackOccupied, straightSliders, Piece.B_QUEEN.ind, Piece.B_ROOK.ind, moves);
    pinnedPieces |= addTacticalPositiveStraightPinnedPieceMoveAndGetPinnedPiece(rays.filePos & allOccupied,
        allBlackOccupied, straightSliders, Piece.B_QUEEN.ind, Piece.B_ROOK.ind, moves);
    pinnedPieces |= addBlackTacticalPositiveDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.diagonalPos,
        diagonalSliders, moves);
    pinnedPieces |= addBlackTacticalPositiveDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.antiDiagonalPos,
        diagonalSliders, moves);
    pinnedPieces |= addTacticalNegativeStraightPinnedPieceMoveAndGetPinnedPiece(rays.rankNeg & allOccupied,
        allBlackOccupied, straightSliders, Piece.B_QUEEN.ind, Piece.B_ROOK.ind, moves);
    pinnedPieces |= addTacticalNegativeStraightPinnedPieceMoveAndGetPinnedPiece(rays.fileNeg & allOccupied,
        allBlackOccupied, straightSliders, Piece.B_QUEEN.ind, Piece.B_ROOK.ind, moves);
    pinnedPieces |= addBlackTacticalNegativeDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.diagonalNeg,
        diagonalSliders, moves);
    pinnedPieces |= addBlackTacticalNegativeDiagonalPinnedPieceMoveAndGetPinnedPiece(rays.antiDiagonalNeg,
        diagonalSliders, moves);
    return pinnedPieces;
  }

  private void addWhiteKingNormalMoves(byte from, long targets, List<Move> moves) {
    long moveSet = MoveSetBase.getByIndex(from).getKingMoveSet(targets);
    while (moveSet != Bitboard.EMPTY_BOARD) {
      byte to = BitOperations.indexOfLSBit(moveSet);
      if (!isAttackedByBlack(to)) {
        moves.add(new Move(from, to, Piece.W_KING.ind, squares[to],
            MoveType.NORMAL.ind));
      }
      moveSet = BitOperations.resetLSBit(moveSet);
    }
  }

  private void addBlackKingNormalMoves(byte from, long targets, List<Move> moves) {
    long moveSet = MoveSetBase.getByIndex(from).getKingMoveSet(targets);
    while (moveSet != Bitboard.EMPTY_BOARD) {
      byte to = BitOperations.indexOfLSBit(moveSet);
      if (!isAttackedByWhite(to)) {
        moves.add(new Move(from, to, Piece.B_KING.ind, squares[to],
            MoveType.NORMAL.ind));
      }
      moveSet = BitOperations.resetLSBit(moveSet);
    }
  }

  private void addWhiteKingCastlingMoves(byte from, List<Move> moves) {
    if ((whiteCastlingRights == CastlingRights.LONG.ind ||
        whiteCastlingRights == CastlingRights.ALL.ind) &&
        ((Bitboard.Square.B1.bitboard | Bitboard.Square.C1.bitboard |
            Bitboard.Square.D1.bitboard) & allOccupied) == Bitboard.EMPTY_BOARD &&
        !isAttackedByBlack(Bitboard.Square.D1.ind) &&
        !isAttackedByBlack(Bitboard.Square.C1.ind)) {
      moves.add(new Move(from, Bitboard.Square.C1.ind, Piece.W_KING.ind,
          Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
    }
    if ((whiteCastlingRights == CastlingRights.SHORT.ind ||
        whiteCastlingRights == CastlingRights.ALL.ind) &&
        ((Bitboard.Square.F1.bitboard | Bitboard.Square.G1.bitboard) &
            allOccupied) == Bitboard.EMPTY_BOARD &&
        !isAttackedByBlack(Bitboard.Square.F1.ind) &&
        !isAttackedByBlack(Bitboard.Square.G1.ind)) {
      moves.add(new Move(from, Bitboard.Square.G1.ind, Piece.W_KING.ind,
          Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
    }
  }

  private void addBlackKingCastlingMoves(byte from, List<Move> moves) {
    if ((blackCastlingRights == CastlingRights.LONG.ind ||
        blackCastlingRights == CastlingRights.ALL.ind) &&
        ((Bitboard.Square.B8.bitboard | Bitboard.Square.C8.bitboard |
            Bitboard.Square.D8.bitboard) & allOccupied) == Bitboard.EMPTY_BOARD &&
        !isAttackedByWhite(Bitboard.Square.D8.ind) &&
        !isAttackedByWhite(Bitboard.Square.C8.ind)) {
      moves.add(new Move(from, Bitboard.Square.C8.ind, Piece.B_KING.ind,
          Piece.NULL.ind, MoveType.LONG_CASTLING.ind));
    }
    if ((blackCastlingRights == CastlingRights.SHORT.ind ||
        blackCastlingRights == CastlingRights.ALL.ind) &&
        ((Bitboard.Square.F8.bitboard | Bitboard.Square.G8.bitboard) &
            allOccupied) == Bitboard.EMPTY_BOARD &&
        !isAttackedByWhite(Bitboard.Square.F8.ind) &&
        !isAttackedByWhite(Bitboard.Square.G8.ind)) {
      moves.add(new Move(from, Bitboard.Square.G8.ind, Piece.B_KING.ind,
          Piece.NULL.ind, MoveType.SHORT_CASTLING.ind));
    }
  }

  private void addNormalMovesFromOrigin(byte from, byte movedPiece, long moveSet, List<Move> moves) {
    while (moveSet != Bitboard.EMPTY_BOARD) {
      byte to = BitOperations.indexOfLSBit(moveSet);
      moves.add(new Move(from, to, movedPiece, squares[to], MoveType.NORMAL.ind));
      moveSet = BitOperations.resetLSBit(moveSet);
    }
  }

  private void addNormalNonCaptureMovesFromOrigin(byte from, byte movedPiece, long moveSet, List<Move> moves) {
    while (moveSet != Bitboard.EMPTY_BOARD) {
      moves.add(new Move(from, BitOperations.indexOfLSBit(moveSet), movedPiece, Piece.NULL.ind, MoveType.NORMAL.ind));
      moveSet = BitOperations.resetLSBit(moveSet);
    }
  }

  private void addQueenMoves(byte pieceType, long pieces, long targets, List<Move> moves) {
    while (pieces != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfLSBit(pieces);
      addNormalMovesFromOrigin(from, pieceType, MoveSetBase.getByIndex(from).getQueenMoveSet(targets, allOccupied),
          moves);
      pieces = BitOperations.resetLSBit(pieces);
    }
  }

  private void addRookMoves(byte pieceType, long pieces, long targets, List<Move> moves) {
    while (pieces != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfLSBit(pieces);
      addNormalMovesFromOrigin(from, pieceType, MoveSetBase.getByIndex(from).getRookMoveSet(targets, allOccupied),
          moves);
      pieces = BitOperations.resetLSBit(pieces);
    }
  }

  private void addBishopMoves(byte pieceType, long pieces, long targets, List<Move> moves) {
    while (pieces != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfLSBit(pieces);
      addNormalMovesFromOrigin(from, pieceType, MoveSetBase.getByIndex(from).getBishopMoveSet(targets, allOccupied),
          moves);
      pieces = BitOperations.resetLSBit(pieces);
    }
  }

  private void addKnightMoves(byte pieceType, long pieces, long targets, List<Move> moves) {
    while (pieces != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfLSBit(pieces);
      addNormalMovesFromOrigin(from, pieceType, MoveSetBase.getByIndex(from).getKnightMoveSet(targets), moves);
      pieces = BitOperations.resetLSBit(pieces);
    }
  }

  private void addWhitePawnNormalMoves(long movablePieces, long oppTargets, long emptyTargets, List<Move> moves) {
    long pieces = whitePawns & movablePieces & ~Bitboard.Rank.R7.bitboard;
    while (pieces != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfLSBit(pieces);
      MoveSetBase moveSetBase = MoveSetBase.getByIndex(from);
      addNormalMovesFromOrigin(from, Piece.W_PAWN.ind, moveSetBase.getWhitePawnCaptureSet(oppTargets), moves);
      addNormalNonCaptureMovesFromOrigin(from, Piece.W_PAWN.ind, moveSetBase.getWhitePawnAdvanceSet(emptyTargets), moves);
      pieces = BitOperations.resetLSBit(pieces);
    }
  }

  private void addBlackPawnNormalMoves(long movablePieces, long oppTargets, long emptyTargets, List<Move> moves) {
    long pieces = blackPawns & movablePieces & ~Bitboard.Rank.R2.bitboard;
    while (pieces != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfLSBit(pieces);
      MoveSetBase moveSetBase = MoveSetBase.getByIndex(from);
      addNormalMovesFromOrigin(from, Piece.B_PAWN.ind, moveSetBase.getBlackPawnCaptureSet(oppTargets), moves);
      addNormalNonCaptureMovesFromOrigin(from, Piece.B_PAWN.ind, moveSetBase.getBlackPawnAdvanceSet(emptyTargets), moves);
      pieces = BitOperations.resetLSBit(pieces);
    }
  }

  private void addWhitePawnEnPassantMoves(long movablePieces, byte whiteKingInd, List<Move> moves) {
    if (enPassantRights != EnPassantRights.NONE.ind && movablePieces != Bitboard.EMPTY_BOARD) {
      byte to = (byte) (EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights);
      long pieces = MoveSetBase.getByIndex(to).getBlackPawnCaptureSet(whitePawns) & movablePieces;
      if (pieces == Bitboard.EMPTY_BOARD) {
        return;
      }
      MoveSetBase kingDb = MoveSetBase.getByIndex(whiteKingInd);
      do {
        byte from = BitOperations.indexOfLSBit(pieces);
        // Make sure that the en passant does not leave the king exposed to check.
        long changedWhiteBits = ((BitOperations.toBit(from)) | (BitOperations.toBit(to)));
        long allNonWhiteOccupiedTemp = allNonWhiteOccupied ^ changedWhiteBits;
        long allOccupiedTemp = allOccupied ^ (changedWhiteBits | BitOperations.toBit(to - 8));
        if (((blackQueens | blackRooks) & kingDb.getRookMoveSet(allNonWhiteOccupiedTemp,
            allOccupiedTemp)) == Bitboard.EMPTY_BOARD && ((blackQueens | blackBishops) &
            kingDb.getBishopMoveSet(allNonWhiteOccupiedTemp, allOccupiedTemp)) == Bitboard.EMPTY_BOARD) {
          moves.add(new Move(from, to, Piece.W_PAWN.ind, Piece.B_PAWN.ind,
              MoveType.EN_PASSANT.ind));
        }
        pieces = BitOperations.resetLSBit(pieces);
      } while (pieces != Bitboard.EMPTY_BOARD);
    }
  }

  private void addBlackPawnEnPassantMoves(long movablePieces, byte blackKingInd, List<Move> moves) {
    if (enPassantRights != EnPassantRights.NONE.ind && movablePieces != Bitboard.EMPTY_BOARD) {
      byte to = (byte) (EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights);
      long pieces = MoveSetBase.getByIndex(to).getWhitePawnCaptureSet(blackPawns) & movablePieces;
      if (pieces == Bitboard.EMPTY_BOARD) {
        return;
      }
      MoveSetBase kingDb = MoveSetBase.getByIndex(blackKingInd);
      do {
        byte from = BitOperations.indexOfLSBit(pieces);
        long changedBlackBits = BitOperations.toBit(from) | BitOperations.toBit(to);
        long allNonBlackOccupiedTemp = allNonBlackOccupied ^ changedBlackBits;
        long allOccupiedTemp = allOccupied ^ (changedBlackBits | BitOperations.toBit(to + 8));
        if (((whiteQueens | whiteRooks) & kingDb.getRookMoveSet(allNonBlackOccupiedTemp,
            allOccupiedTemp)) == Bitboard.EMPTY_BOARD && ((whiteQueens | whiteBishops) &
            kingDb.getBishopMoveSet(allNonBlackOccupiedTemp, allOccupiedTemp)) == Bitboard.EMPTY_BOARD) {
          moves.add(new Move(from, to, Piece.B_PAWN.ind, Piece.W_PAWN.ind,
              MoveType.EN_PASSANT.ind));
        }
        pieces = BitOperations.resetLSBit(pieces);
      } while (pieces != Bitboard.EMPTY_BOARD);
    }
  }

  private static void addPromotionMoves(byte from, byte to, byte movedPiece, byte capturedPiece, List<Move> moves) {
    moves.add(new Move(from, to, movedPiece, capturedPiece, MoveType.PROMOTION_TO_QUEEN.ind));
    moves.add(new Move(from, to, movedPiece, capturedPiece, MoveType.PROMOTION_TO_ROOK.ind));
    moves.add(new Move(from, to, movedPiece, capturedPiece, MoveType.PROMOTION_TO_BISHOP.ind));
    moves.add(new Move(from, to, movedPiece, capturedPiece, MoveType.PROMOTION_TO_KNIGHT.ind));
  }

  private void addWhitePawnPromotionMoves(long movablePieces, long targets, List<Move> moves) {
    long pieces = whitePawns & movablePieces & Bitboard.Rank.R7.bitboard;
    while (pieces != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfLSBit(pieces);
      long moveSet = MoveSetBase.getByIndex(from).getWhitePawnMoveSet(allBlackOccupied, allEmpty) & targets;
      while (moveSet != Bitboard.EMPTY_BOARD) {
        byte to = BitOperations.indexOfLSBit(moveSet);
        addPromotionMoves(from, to, Piece.W_PAWN.ind, squares[to], moves);
        moveSet = BitOperations.resetLSBit(moveSet);
      }
      pieces = BitOperations.resetLSBit(pieces);
    }
  }

  private void addBlackPawnPromotionMoves(long movablePieces, long targets, List<Move> moves) {
    long pieces = blackPawns & movablePieces & Bitboard.Rank.R2.bitboard;
    while (pieces != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfLSBit(pieces);
      long moveSet = MoveSetBase.getByIndex(from).getBlackPawnMoveSet(allWhiteOccupied, allEmpty) & targets;
      while (moveSet != Bitboard.EMPTY_BOARD) {
        byte to = BitOperations.indexOfLSBit(moveSet);
        addPromotionMoves(from, to, Piece.B_PAWN.ind, squares[to], moves);
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
    addKnightMoves(Piece.W_KNIGHT.ind, whiteKnights & movablePieces, allBlackOccupied, moves);
    addBishopMoves(Piece.W_BISHOP.ind, whiteBishops & movablePieces, allBlackOccupied, moves);
    addRookMoves(Piece.W_ROOK.ind, whiteRooks & movablePieces, allBlackOccupied, moves);
    addQueenMoves(Piece.W_QUEEN.ind, whiteQueens & movablePieces, allBlackOccupied, moves);
    addWhiteKingNormalMoves(kingInd, allBlackOccupied, moves);
  }

  private void addBlackTacticalMoves(List<Move> moves) {
    byte kingInd = BitOperations.indexOfBit(blackKing);
    long movablePieces = ~addBlackTacticalPinnedPieceMovesAndGetPinnedPieces(kingInd, moves);
    addBlackPawnPromotionMoves(movablePieces, Bitboard.FULL_BOARD, moves);
    addBlackPawnEnPassantMoves(movablePieces, kingInd, moves);
    addBlackPawnNormalMoves(movablePieces, allWhiteOccupied, Bitboard.EMPTY_BOARD, moves);
    addKnightMoves(Piece.B_KNIGHT.ind, blackKnights & movablePieces, allWhiteOccupied, moves);
    addBishopMoves(Piece.B_BISHOP.ind, blackBishops & movablePieces, allWhiteOccupied, moves);
    addRookMoves(Piece.B_ROOK.ind, blackRooks & movablePieces, allWhiteOccupied, moves);
    addQueenMoves(Piece.B_QUEEN.ind, blackQueens & movablePieces, allWhiteOccupied, moves);
    addBlackKingNormalMoves(kingInd, allWhiteOccupied, moves);
  }

  private void addNormalMovesToDestination(byte to, byte capturedPiece, long pieces, List<Move> moves) {
    while (pieces != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfLSBit(pieces);
      moves.add(new Move(from, to, squares[from], capturedPiece, MoveType.NORMAL.ind));
      pieces = BitOperations.resetLSBit(pieces);
    }
  }

  private long getKingAllowedTargets(byte checkerInd, byte queenType, byte rookType, byte bishopType, byte knightType,
      long king, long allNonSameColorOccupied) {
    long kingAllowedTargets;
    byte checkerType = squares[checkerInd];
    if (checkerType == queenType) {
      kingAllowedTargets = ~MoveSetBase.getByIndex(checkerInd).getQueenMoveSet(allNonSameColorOccupied,
          allOccupied ^ king);
    } else if (checkerType == rookType) {
      kingAllowedTargets = ~MoveSetBase.getByIndex(checkerInd).getRookMoveMask();
    } else if (checkerType == bishopType) {
      kingAllowedTargets = ~MoveSetBase.getByIndex(checkerInd).getBishopMoveMask();
    } else if (checkerType == knightType) {
      kingAllowedTargets = ~MoveSetBase.getByIndex(checkerInd).knightMoveMask;
    } else {
      kingAllowedTargets = Bitboard.FULL_BOARD;
    }
    return kingAllowedTargets;
  }

  private void addWhiteTacticalCheckEvasionMoves(List<Move> moves) {
    byte kingInd = BitOperations.indexOfBit(whiteKing);
    long checker1 = BitOperations.getLSBit(checkers);
    byte checker1Ind = BitOperations.indexOfBit(checker1);
    long checkersTemp = BitOperations.resetLSBit(checkers);
    long kingAllowedTargets = getKingAllowedTargets(checker1Ind, Piece.B_QUEEN.ind,
        Piece.B_ROOK.ind, Piece.B_BISHOP.ind, Piece.B_KNIGHT.ind,
        whiteKing, allNonWhiteOccupied);
    if (checkersTemp == Bitboard.EMPTY_BOARD) {
      long checkLine = Bitboard.getLineSegment(checker1Ind, kingInd);
      long movablePieces = ~Bitboard.getPinnedPieces(kingInd, blackQueens | blackRooks, blackQueens | blackBishops,
          allOccupied, allWhiteOccupied);
      long attackers = getWhiteCheckers(checker1Ind);
      /* The intersection of the last rank and the check line can only be non-empty if the checker truly is a
       * sliding piece (rook or queen) and thus there really is a check line. */
      long lastRankCheckLine = (Bitboard.Rank.R8.bitboard & checkLine);
      addWhitePawnPromotionMoves(Bitboard.computeBlackPawnAdvanceSets(lastRankCheckLine, movablePieces) |
          (attackers & movablePieces), lastRankCheckLine | checker1, moves);
      // Just assume en passant is legal in the position as the method anyway 'double' checks it.
      if (checker1Ind == EnPassantRights.TO_W_VICT_SQR_IND + enPassantRights ||
          ((checker1 & (blackQueens | blackRooks | blackBishops)) != Bitboard.EMPTY_BOARD &&
              (checkLine & BitOperations.toBit(EnPassantRights.TO_W_DEST_SQR_IND + enPassantRights)) !=
                  Bitboard.EMPTY_BOARD)) {
        addWhitePawnEnPassantMoves(movablePieces, kingInd, moves);
      }
      addNormalMovesToDestination(checker1Ind, squares[checker1Ind], attackers &
          ~(whitePawns & Bitboard.Rank.R7.bitboard) & movablePieces, moves);
    } else {
      kingAllowedTargets &= getKingAllowedTargets(BitOperations.indexOfBit(checkersTemp),
          Piece.B_QUEEN.ind, Piece.B_ROOK.ind, Piece.B_BISHOP.ind,
          Piece.B_KNIGHT.ind, whiteKing, allNonWhiteOccupied);
    }
    addWhiteKingNormalMoves(kingInd, allBlackOccupied & kingAllowedTargets, moves);
  }

  private void addBlackTacticalCheckEvasionMoves(List<Move> moves) {
    byte kingInd = BitOperations.indexOfBit(blackKing);
    long checker1 = BitOperations.getLSBit(checkers);
    byte checker1Ind = BitOperations.indexOfBit(checker1);
    long checkersTemp = BitOperations.resetLSBit(checkers);
    long kingAllowedTargets = getKingAllowedTargets(checker1Ind, Piece.W_QUEEN.ind,
        Piece.W_ROOK.ind, Piece.W_BISHOP.ind, Piece.W_KNIGHT.ind,
        blackKing, allNonBlackOccupied);
    if (checkersTemp == Bitboard.EMPTY_BOARD) {
      long checkLine = Bitboard.getLineSegment(checker1Ind, kingInd);
      long movablePieces = ~Bitboard.getPinnedPieces(kingInd, whiteQueens | whiteRooks, whiteQueens | whiteBishops,
          allOccupied, allBlackOccupied);
      long attackers = getBlackCheckers(checker1Ind);
      long lastRankCheckLine = (Bitboard.Rank.R1.bitboard & checkLine);
      addBlackPawnPromotionMoves(Bitboard.computeWhitePawnAdvanceSets(lastRankCheckLine, movablePieces) |
          (attackers & movablePieces), lastRankCheckLine | checker1, moves);
      if (checker1Ind == EnPassantRights.TO_B_VICT_SQR_IND + enPassantRights ||
          ((checker1 & (whiteQueens | whiteRooks | whiteBishops)) != Bitboard.EMPTY_BOARD &&
              (checkLine & BitOperations.toBit(EnPassantRights.TO_B_DEST_SQR_IND + enPassantRights)) !=
                  Bitboard.EMPTY_BOARD)) {
        addBlackPawnEnPassantMoves(movablePieces, kingInd, moves);
      }
      addNormalMovesToDestination(checker1Ind, squares[checker1Ind], attackers &
          ~(blackPawns & Bitboard.Rank.R2.bitboard) & movablePieces, moves);
    } else {
      kingAllowedTargets &= getKingAllowedTargets(BitOperations.indexOfBit(checkersTemp),
          Piece.W_QUEEN.ind, Piece.W_ROOK.ind, Piece.W_BISHOP.ind,
          Piece.W_KNIGHT.ind, blackKing, allNonBlackOccupied);
    }
    addBlackKingNormalMoves(kingInd, allWhiteOccupied & kingAllowedTargets, moves);
  }

  /**
   * Returns a list of all the legal tactical moves in the current position. Tactical moves are ordinary captures, promotions, and en
   * passant captures.
   *
   * @return A list of all the legal tactical moves.
   */
  public List<Move> getTacticalMoves() {
    List<Move> moves = new LinkedList<>();
    if (whitesTurn) {
      if (inCheck) {
        addWhiteTacticalCheckEvasionMoves(moves);
      } else {
        addWhiteTacticalMoves(moves);
      }
    } else {
      if (inCheck) {
        addBlackTacticalCheckEvasionMoves(moves);
      } else {
        addBlackTacticalMoves(moves);
      }
    }
    return moves;
  }

  private long addQuietPinnedPieceMoveAndGetPinnedPiece(byte kingInd, long pinnedPiece, long pinningPiece,
      byte queenType, byte secondarySliderType, List<Move> moves) {
    if (pinningPiece != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfBit(pinnedPiece);
      byte pinnedPieceType = squares[from];
      if (pinnedPieceType == queenType || pinnedPieceType == secondarySliderType) {
        addNormalMovesFromOrigin(from, pinnedPieceType,
            Bitboard.getLineSegment(BitOperations.indexOfBit(pinningPiece), kingInd) ^ pinnedPiece, moves);
      }
      return pinnedPiece;
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addQuietPositivePinnedPieceMoveAndGetPinnedPiece(byte kingInd, long rayOccupancy,
      long allSameColorOccupied, long sliders, byte queenType, byte secondarySliderType, List<Move> moves) {
    long pinnedPiece = BitOperations.getLSBit(rayOccupancy) & allSameColorOccupied;
    if (pinnedPiece != Bitboard.EMPTY_BOARD) {
      return addQuietPinnedPieceMoveAndGetPinnedPiece(kingInd, pinnedPiece,
          BitOperations.getLSBit(rayOccupancy ^ pinnedPiece) & sliders, queenType,
          secondarySliderType, moves);
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addQuietNegativePinnedPieceMoveAndGetPinnedPiece(byte kingInd, long rayOccupancy,
      long allSameColorOccupied, long sliders, byte queenType, byte secondarySliderType, List<Move> moves) {
    long pinnedPiece = BitOperations.getMSBit(rayOccupancy) & allSameColorOccupied;
    if (pinnedPiece != Bitboard.EMPTY_BOARD) {
      return addQuietPinnedPieceMoveAndGetPinnedPiece(kingInd, pinnedPiece,
          BitOperations.getMSBit(rayOccupancy ^ pinnedPiece) & sliders, queenType,
          secondarySliderType, moves);
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addWhiteQuietFilePinnedPieceMoveAndGetPinnedPiece(byte kingInd, long pinnedPiece, long pinningPiece,
      List<Move> moves) {
    if (pinningPiece != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfBit(pinnedPiece);
      byte pinnedPieceType = squares[from];
      if (pinnedPieceType == Piece.W_QUEEN.ind || pinnedPieceType == Piece.W_ROOK.ind) {
        addNormalMovesFromOrigin(from, pinnedPieceType,
            Bitboard.getLineSegment(BitOperations.indexOfBit(pinningPiece), kingInd) ^ pinnedPiece, moves);
      } else if (pinnedPieceType == Piece.W_PAWN.ind) {
        addNormalMovesFromOrigin(from, pinnedPieceType,
            MoveSetBase.getByIndex(from).getWhitePawnAdvanceSet(allEmpty), moves);
      }
      return pinnedPiece;
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addBlackQuietFilePinnedPieceMoveAndGetPinnedPiece(byte kingInd, long pinnedPiece, long pinningPiece,
      List<Move> moves) {
    if (pinningPiece != Bitboard.EMPTY_BOARD) {
      byte from = BitOperations.indexOfBit(pinnedPiece);
      byte pinnedPieceType = squares[from];
      if (pinnedPieceType == Piece.B_QUEEN.ind || pinnedPieceType == Piece.B_ROOK.ind) {
        addNormalMovesFromOrigin(from, pinnedPieceType,
            Bitboard.getLineSegment(BitOperations.indexOfBit(pinningPiece), kingInd) ^ pinnedPiece, moves);
      } else if (pinnedPieceType == Piece.B_PAWN.ind) {
        addNormalMovesFromOrigin(from, pinnedPieceType,
            MoveSetBase.getByIndex(from).getBlackPawnAdvanceSet(allEmpty), moves);
      }
      return pinnedPiece;
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addWhiteQuietFilePositivePinnedPieceMoveAndGetPinnedPiece(byte kingInd, long rayOccupancy,
      long allSameColorOccupied, long sliders, List<Move> moves) {
    long pinnedPiece = BitOperations.getLSBit(rayOccupancy) & allSameColorOccupied;
    if (pinnedPiece != Bitboard.EMPTY_BOARD) {
      return addWhiteQuietFilePinnedPieceMoveAndGetPinnedPiece(kingInd, pinnedPiece,
          BitOperations.getLSBit(rayOccupancy ^ pinnedPiece) & sliders, moves);
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addBlackQuietFilePositivePinnedPieceMoveAndGetPinnedPiece(byte kingInd, long rayOccupancy,
      long allSameColorOccupied, long sliders, List<Move> moves) {
    long pinnedPiece = BitOperations.getLSBit(rayOccupancy) & allSameColorOccupied;
    if (pinnedPiece != Bitboard.EMPTY_BOARD) {
      return addBlackQuietFilePinnedPieceMoveAndGetPinnedPiece(kingInd, pinnedPiece,
          BitOperations.getLSBit(rayOccupancy ^ pinnedPiece) & sliders, moves);
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addWhiteQuietFileNegativePinnedPieceMoveAndGetPinnedPiece(byte kingInd, long rayOccupancy,
      long allSameColorOccupied, long sliders, List<Move> moves) {
    long pinnedPiece = BitOperations.getMSBit(rayOccupancy) & allSameColorOccupied;
    if (pinnedPiece != Bitboard.EMPTY_BOARD) {
      return addWhiteQuietFilePinnedPieceMoveAndGetPinnedPiece(kingInd, pinnedPiece,
          BitOperations.getMSBit(rayOccupancy ^ pinnedPiece) & sliders, moves);
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addBlackQuietFileNegativePinnedPieceMoveAndGetPinnedPiece(byte kingInd, long rayOccupancy,
      long allSameColorOccupied, long sliders, List<Move> moves) {
    long pinnedPiece = BitOperations.getMSBit(rayOccupancy) & allSameColorOccupied;
    if (pinnedPiece != Bitboard.EMPTY_BOARD) {
      return addBlackQuietFilePinnedPieceMoveAndGetPinnedPiece(kingInd, pinnedPiece,
          BitOperations.getMSBit(rayOccupancy ^ pinnedPiece) & sliders, moves);
    }
    return Bitboard.EMPTY_BOARD;
  }

  private long addWhiteQuietPinnedPieceMovesAndGetPinnedPieces(byte kingInd, List<Move> moves) {
    Bitboard.Rays rays = Bitboard.Rays.values()[kingInd];
    long straightSliders = blackQueens | blackRooks;
    long diagonalSliders = blackQueens | blackBishops;
    long pinnedPieces = addQuietPositivePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.rankPos & allOccupied,
        allWhiteOccupied, straightSliders, Piece.W_QUEEN.ind, Piece.W_ROOK.ind, moves);
    pinnedPieces |= addWhiteQuietFilePositivePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.filePos & allOccupied,
        allWhiteOccupied, straightSliders, moves);
    pinnedPieces |= addQuietPositivePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.diagonalPos & allOccupied,
        allWhiteOccupied, diagonalSliders, Piece.W_QUEEN.ind, Piece.W_BISHOP.ind, moves);
    pinnedPieces |= addQuietPositivePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.antiDiagonalPos & allOccupied,
        allWhiteOccupied, diagonalSliders, Piece.W_QUEEN.ind, Piece.W_BISHOP.ind, moves);
    pinnedPieces |= addQuietNegativePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.rankNeg & allOccupied,
        allWhiteOccupied, straightSliders, Piece.W_QUEEN.ind, Piece.W_ROOK.ind, moves);
    pinnedPieces |= addWhiteQuietFileNegativePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.fileNeg & allOccupied,
        allWhiteOccupied, straightSliders, moves);
    pinnedPieces |= addQuietNegativePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.diagonalNeg & allOccupied,
        allWhiteOccupied, diagonalSliders, Piece.W_QUEEN.ind, Piece.W_BISHOP.ind, moves);
    pinnedPieces |= addQuietNegativePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.antiDiagonalNeg & allOccupied,
        allWhiteOccupied, diagonalSliders, Piece.W_QUEEN.ind, Piece.W_BISHOP.ind, moves);
    return pinnedPieces;
  }

  private long addBlackQuietPinnedPieceMovesAndGetPinnedPieces(byte kingInd, List<Move> moves) {
    Bitboard.Rays rays = Bitboard.Rays.values()[kingInd];
    long straightSliders = whiteQueens | whiteRooks;
    long diagonalSliders = whiteQueens | whiteBishops;
    long pinnedPieces = addQuietPositivePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.rankPos & allOccupied,
        allBlackOccupied, straightSliders, Piece.B_QUEEN.ind, Piece.B_ROOK.ind, moves);
    pinnedPieces |= addBlackQuietFilePositivePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.filePos & allOccupied,
        allBlackOccupied, straightSliders, moves);
    pinnedPieces |= addQuietPositivePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.diagonalPos & allOccupied,
        allBlackOccupied, diagonalSliders, Piece.B_QUEEN.ind, Piece.B_BISHOP.ind, moves);
    pinnedPieces |= addQuietPositivePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.antiDiagonalPos & allOccupied,
        allBlackOccupied, diagonalSliders, Piece.B_QUEEN.ind, Piece.B_BISHOP.ind, moves);
    pinnedPieces |= addQuietNegativePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.rankNeg & allOccupied,
        allBlackOccupied, straightSliders, Piece.B_QUEEN.ind, Piece.B_ROOK.ind, moves);
    pinnedPieces |= addBlackQuietFileNegativePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.fileNeg & allOccupied,
        allBlackOccupied, straightSliders, moves);
    pinnedPieces |= addQuietNegativePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.diagonalNeg & allOccupied,
        allBlackOccupied, diagonalSliders, Piece.B_QUEEN.ind, Piece.B_BISHOP.ind, moves);
    pinnedPieces |= addQuietNegativePinnedPieceMoveAndGetPinnedPiece(kingInd, rays.antiDiagonalNeg & allOccupied,
        allBlackOccupied, diagonalSliders, Piece.B_QUEEN.ind, Piece.B_BISHOP.ind, moves);
    return pinnedPieces;
  }

  private void addWhiteQuietMoves(List<Move> moves) {
    byte kingInd = BitOperations.indexOfBit(whiteKing);
    long movablePieces = ~addWhiteQuietPinnedPieceMovesAndGetPinnedPieces(kingInd, moves);
    addWhitePawnNormalMoves(movablePieces, Bitboard.EMPTY_BOARD, allEmpty, moves);
    addKnightMoves(Piece.W_KNIGHT.ind, whiteKnights & movablePieces, allEmpty, moves);
    addBishopMoves(Piece.W_BISHOP.ind, whiteBishops & movablePieces, allEmpty, moves);
    addRookMoves(Piece.W_ROOK.ind, whiteRooks & movablePieces, allEmpty, moves);
    addQueenMoves(Piece.W_QUEEN.ind, whiteQueens & movablePieces, allEmpty, moves);
    addWhiteKingCastlingMoves(kingInd, moves);
    addWhiteKingNormalMoves(kingInd, allEmpty, moves);
  }

  private void addBlackQuietMoves(List<Move> moves) {
    byte kingInd = BitOperations.indexOfBit(blackKing);
    long movablePieces = ~addBlackQuietPinnedPieceMovesAndGetPinnedPieces(kingInd, moves);
    addBlackPawnNormalMoves(movablePieces, Bitboard.EMPTY_BOARD, allEmpty, moves);
    addKnightMoves(Piece.B_KNIGHT.ind, blackKnights & movablePieces, allEmpty, moves);
    addBishopMoves(Piece.B_BISHOP.ind, blackBishops & movablePieces, allEmpty, moves);
    addRookMoves(Piece.B_ROOK.ind, blackRooks & movablePieces, allEmpty, moves);
    addQueenMoves(Piece.B_QUEEN.ind, blackQueens & movablePieces, allEmpty, moves);
    addBlackKingCastlingMoves(kingInd, moves);
    addBlackKingNormalMoves(kingInd, allEmpty, moves);
  }

  private long getWhitePushers(int sqrInd) {
    MoveSetBase dB = MoveSetBase.getByIndex(sqrInd);
    long blackPawnAdvance = dB.blackPawnAdvanceMoveMask;
    long pushers = whiteKnights & dB.knightMoveMask;
    pushers |= whitePawns & blackPawnAdvance;
    if (Bitboard.Rank.getBySquareIndex(sqrInd) == Bitboard.Rank.R4 &&
        (allEmpty & blackPawnAdvance) != Bitboard.EMPTY_BOARD) {
      pushers |= Bitboard.computeBlackPawnAdvanceSets(blackPawnAdvance, whitePawns);
    }
    pushers |= (whiteQueens | whiteRooks) & dB.getRookMoveSet(allNonBlackOccupied, allOccupied);
    pushers |= (whiteQueens | whiteBishops) & dB.getBishopMoveSet(allNonBlackOccupied, allOccupied);
    return pushers;
  }

  private long getBlackPushers(int sqrInd) {
    MoveSetBase dB = MoveSetBase.getByIndex(sqrInd);
    long whitePawnAdvance = dB.whitePawnAdvanceMoveMask;
    long pushers = blackKnights & dB.knightMoveMask;
    pushers |= blackPawns & whitePawnAdvance;
    if (Bitboard.Rank.getBySquareIndex(sqrInd) == Bitboard.Rank.R5 &&
        (allEmpty & whitePawnAdvance) != Bitboard.EMPTY_BOARD) {
      pushers |= Bitboard.computeWhitePawnAdvanceSets(whitePawnAdvance, blackPawns);
    }
    pushers |= (blackQueens | blackRooks) & dB.getRookMoveSet(allNonWhiteOccupied, allOccupied);
    pushers |= (blackQueens | blackBishops) & dB.getBishopMoveSet(allNonWhiteOccupied, allOccupied);
    return pushers;
  }

  private void addWhiteQuietCheckEvasionMoves(List<Move> moves) {
    byte kingInd = BitOperations.indexOfBit(whiteKing);
    long checker1 = BitOperations.getLSBit(checkers);
    byte checker1Ind = BitOperations.indexOfBit(checker1);
    long kingAllowedTargets = getKingAllowedTargets(checker1Ind, Piece.B_QUEEN.ind,
        Piece.B_ROOK.ind, Piece.B_BISHOP.ind, Piece.B_KNIGHT.ind,
        whiteKing, allNonWhiteOccupied);
    long straightSliders = blackQueens | blackRooks;
    long diagonalSliders = blackQueens | blackBishops;
    long checkersTemp = BitOperations.resetLSBit(checkers);
    if (checkersTemp == Bitboard.EMPTY_BOARD) {
      if (((straightSliders | diagonalSliders) & checker1) != Bitboard.EMPTY_BOARD) {
        long checkLine1 = Bitboard.getLineSegment(checker1Ind, kingInd);
        if (checkLine1 != Bitboard.EMPTY_BOARD) {
          long movablePieces = ~Bitboard.getPinnedPieces(kingInd, straightSliders, diagonalSliders, allOccupied,
              allWhiteOccupied) & ~(whitePawns & Bitboard.Rank.R7.bitboard);
          long checkLinesTemp = checkLine1;
          while (checkLinesTemp != Bitboard.EMPTY_BOARD) {
            byte to = BitOperations.indexOfLSBit(checkLinesTemp);
            addNormalMovesToDestination(to, Piece.NULL.ind, getWhitePushers(to) & movablePieces,
                moves);
            checkLinesTemp = BitOperations.resetLSBit(checkLinesTemp);
          }
        }
      }
    } else {
      kingAllowedTargets &= getKingAllowedTargets(BitOperations.indexOfBit(checkersTemp),
          Piece.B_QUEEN.ind, Piece.B_ROOK.ind, Piece.B_BISHOP.ind,
          Piece.B_KNIGHT.ind, whiteKing, allNonWhiteOccupied);
    }
    addWhiteKingNormalMoves(kingInd, allEmpty & kingAllowedTargets, moves);
  }

  private void addBlackQuietCheckEvasionMoves(List<Move> moves) {
    byte kingInd = BitOperations.indexOfBit(blackKing);
    long checker1 = BitOperations.getLSBit(checkers);
    byte checker1Ind = BitOperations.indexOfBit(checker1);
    long kingAllowedTargets = getKingAllowedTargets(checker1Ind, Piece.W_QUEEN.ind,
        Piece.W_ROOK.ind, Piece.W_BISHOP.ind, Piece.W_KNIGHT.ind,
        blackKing, allNonBlackOccupied);
    long straightSliders = whiteQueens | whiteRooks;
    long diagonalSliders = whiteQueens | whiteBishops;
    long checkersTemp = BitOperations.resetLSBit(checkers);
    if (checkersTemp == Bitboard.EMPTY_BOARD) {
      if (((straightSliders | diagonalSliders) & checker1) != Bitboard.EMPTY_BOARD) {
        long checkLine1 = Bitboard.getLineSegment(checker1Ind, kingInd);
        if (checkLine1 != Bitboard.EMPTY_BOARD) {
          long movablePieces = ~Bitboard.getPinnedPieces(kingInd, straightSliders, diagonalSliders, allOccupied,
              allBlackOccupied) & ~(blackPawns & Bitboard.Rank.R2.bitboard);
          long checkLinesTemp = checkLine1;
          while (checkLinesTemp != Bitboard.EMPTY_BOARD) {
            byte to = BitOperations.indexOfLSBit(checkLinesTemp);
            addNormalMovesToDestination(to, Piece.NULL.ind, getBlackPushers(to) & movablePieces,
                moves);
            checkLinesTemp = BitOperations.resetLSBit(checkLinesTemp);
          }
        }
      }
    } else {
      kingAllowedTargets &= getKingAllowedTargets(BitOperations.indexOfBit(checkersTemp),
          Piece.W_QUEEN.ind, Piece.W_ROOK.ind, Piece.W_BISHOP.ind,
          Piece.W_KNIGHT.ind, blackKing, allNonBlackOccupied);
    }
    addBlackKingNormalMoves(kingInd, allEmpty & kingAllowedTargets, moves);
  }

  /**
   * Returns a list of all the legal quiet moves in the current position. Quiet moves are ordinary non-capture moves and castling moves.
   *
   * @return A list of all the legal quiet moves.
   */
  public List<Move> getQuietMoves() {
    List<Move> moves = new LinkedList<>();
    if (whitesTurn) {
      if (inCheck) {
        addWhiteQuietCheckEvasionMoves(moves);
      } else {
        addWhiteQuietMoves(moves);
      }
    } else {
      if (inCheck) {
        addBlackQuietCheckEvasionMoves(moves);
      } else {
        addBlackQuietMoves(moves);
      }
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
    StringBuilder fenBuffer = new StringBuilder();
    for (int i = 7; i >= 0; i--) {
      int emptyCount = 0;
      for (int j = 0; j < 8; j++) {
        int piece = squares[i * 8 + j];
        if (piece == 0) {
          emptyCount++;
        } else {
          if (emptyCount != 0) {
            fenBuffer.append(Integer.toString(emptyCount));
          }
          emptyCount = 0;
          fenBuffer.append(Piece.values()[piece].letter);
        }
      }
      if (emptyCount != 0) {
        fenBuffer.append(Integer.toString(emptyCount));
      }
      if (i != 0) {
        fenBuffer.append("/");
      }
    }
    fenBuffer.append(" ").append(whitesTurn ? "w" : "b").append(" ");
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
    fenBuffer.append(castlingRights.isEmpty() ? "-" : castlingRights).append(" ");
    fenBuffer.append(enPassantRights == EnPassantRights.NONE.ind ? "-" :
        (EnPassantRights.values()[enPassantRights].toString().toLowerCase() + (whitesTurn ? 6 : 3))).append(" ");
    fenBuffer.append(Byte.toString(fiftyMoveRuleClock)).append(" ").append(Integer.toString(1 + halfMoveIndex / 2));
    return fenBuffer.toString();
  }

}
