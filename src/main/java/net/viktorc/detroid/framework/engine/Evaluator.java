package net.viktorc.detroid.framework.engine;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import net.viktorc.detroid.framework.engine.Bitboard.Diagonal;
import net.viktorc.detroid.framework.engine.Bitboard.File;
import net.viktorc.detroid.framework.engine.Bitboard.Rank;
import net.viktorc.detroid.framework.engine.Bitboard.Square;
import net.viktorc.detroid.framework.util.BitOperations;
import net.viktorc.detroid.framework.util.Cache;

/**
 * A class for evaluating chess positions. It uses an evaluation hash table to improve performance. It also offers a static exchange
 * evaluation function.
 *
 * @author Viktor
 */
public class Evaluator {

  // Game phase parameters.
  static final int MAX_PHASE_SCORE = 256;
  private static final int QUEEN_PHASE_WEIGHT = 4;
  private static final int ROOK_PHASE_WEIGHT = 2;
  private static final int BISHOP_PHASE_WEIGHT = 1;
  private static final int KNIGHT_PHASE_WEIGHT = 1;
  private static final int TOTAL_OPENING_PHASE_WEIGHT = 24;

  /**
   * A symbolic, unattainable value.
   */
  private static final int KING_VALUE = 20000;

  // MVV/LVA piece values.
  private static final byte[] MVV_LVA_PIECE_VALUES = new byte[]{0, 6, 5, 4, 3, 2, 1, 6, 5, 4, 3, 2, 1};
  // The values of different captor-victim combinations for MVV/LVA move assessment.
  private static final byte[][] MVV_LVA;

  static {
    short comb;
    byte assignedVal;
    int attackerVal, victimVal, lastVal;
    int attacker, victim;
    byte kingValue = MVV_LVA_PIECE_VALUES[Piece.W_KING.ind];
    MVV_LVA = new byte[13][13];
    List<Entry<Short, Integer>> combVals = new ArrayList<>();
    // Include illogical combinations without discrimination for the sake of better performance.
    for (Piece a : Piece.values()) {
      if (a != Piece.NULL) {
        for (Piece v : Piece.values()) {
          if (v != Piece.NULL) {
            comb = (short) (((short) v.ind) | (((short) a.ind) << 7));
            attackerVal = MVV_LVA_PIECE_VALUES[a.ind];
            victimVal = MVV_LVA_PIECE_VALUES[v.ind];
            victimVal *= kingValue;
            combVals.add(new SimpleEntry<>(comb, victimVal - attackerVal));
          }
        }
      }
    }
    combVals.sort(Comparator.comparingInt(Entry<Short, Integer>::getValue));
    lastVal = assignedVal = 0;
    for (Entry<Short, Integer> entry : combVals) {
      if (entry.getValue() > lastVal) {
        assignedVal++;
      }
      lastVal = entry.getValue();
      victim = entry.getKey() & 127;
      attacker = entry.getKey() >>> 7;
      MVV_LVA[attacker][victim] = assignedVal;
    }
  }

  // Distance tables for tropism.
  private static final byte[][] MANHATTAN_DISTANCE = new byte[64][64];
  private static final byte[][] CHEBYSHEV_DISTANCE = new byte[64][64];

  static {
    int r1, r2;
    int f1, f2;
    int rankDist, fileDist;
    for (int i = 0; i < 64; i++) {
      r1 = Rank.getBySquareIndex(i).ind;
      f1 = File.getBySquareIndex(i).ind;
      for (int j = 0; j < 64; j++) {
        r2 = Rank.getBySquareIndex(j).ind;
        f2 = File.getBySquareIndex(j).ind;
        rankDist = Math.abs(r2 - r1);
        fileDist = Math.abs(f2 - f1);
        MANHATTAN_DISTANCE[i][j] = (byte) (rankDist + fileDist);
        CHEBYSHEV_DISTANCE[i][j] = (byte) Math.max(rankDist, fileDist);
      }
    }
  }

  private static final String[][] PST_MG_PARAM_NAMES;
  private static final String[][] PST_EG_PARAM_NAMES;

  static {
    PST_MG_PARAM_NAMES = new String[12][64];
    PST_EG_PARAM_NAMES = new String[12][64];
    for (int i = 0; i < 64; i++) {
      PST_MG_PARAM_NAMES[6][i] = DetroidParameters.PST_KING_MG_PARAM_NAMES[i];
      PST_MG_PARAM_NAMES[7][i] = DetroidParameters.PST_QUEEN_MG_PARAM_NAMES[i];
      PST_MG_PARAM_NAMES[8][i] = DetroidParameters.PST_ROOK_MG_PARAM_NAMES[i];
      PST_MG_PARAM_NAMES[9][i] = DetroidParameters.PST_BISHOP_MG_PARAM_NAMES[i];
      PST_MG_PARAM_NAMES[10][i] = DetroidParameters.PST_KNIGHT_MG_PARAM_NAMES[i];
      PST_MG_PARAM_NAMES[11][i] = DetroidParameters.PST_PAWN_MG_PARAM_NAMES[i];
      PST_EG_PARAM_NAMES[6][i] = DetroidParameters.PST_KING_EG_PARAM_NAMES[i];
      PST_EG_PARAM_NAMES[7][i] = DetroidParameters.PST_QUEEN_EG_PARAM_NAMES[i];
      PST_EG_PARAM_NAMES[8][i] = DetroidParameters.PST_ROOK_EG_PARAM_NAMES[i];
      PST_EG_PARAM_NAMES[9][i] = DetroidParameters.PST_BISHOP_EG_PARAM_NAMES[i];
      PST_EG_PARAM_NAMES[10][i] = DetroidParameters.PST_KNIGHT_EG_PARAM_NAMES[i];
      PST_EG_PARAM_NAMES[11][i] = DetroidParameters.PST_PAWN_EG_PARAM_NAMES[i];
    }
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        int c1 = i * 8 + j;
        int c2 = ((7 - i) * 8) + j;
        PST_MG_PARAM_NAMES[0][c1] = PST_MG_PARAM_NAMES[6][c2];
        PST_MG_PARAM_NAMES[1][c1] = PST_MG_PARAM_NAMES[7][c2];
        PST_MG_PARAM_NAMES[2][c1] = PST_MG_PARAM_NAMES[8][c2];
        PST_MG_PARAM_NAMES[3][c1] = PST_MG_PARAM_NAMES[9][c2];
        PST_MG_PARAM_NAMES[4][c1] = PST_MG_PARAM_NAMES[10][c2];
        PST_MG_PARAM_NAMES[5][c1] = PST_MG_PARAM_NAMES[11][c2];
        PST_EG_PARAM_NAMES[0][c1] = PST_EG_PARAM_NAMES[6][c2];
        PST_EG_PARAM_NAMES[1][c1] = PST_EG_PARAM_NAMES[7][c2];
        PST_EG_PARAM_NAMES[2][c1] = PST_EG_PARAM_NAMES[8][c2];
        PST_EG_PARAM_NAMES[3][c1] = PST_EG_PARAM_NAMES[9][c2];
        PST_EG_PARAM_NAMES[4][c1] = PST_EG_PARAM_NAMES[10][c2];
        PST_EG_PARAM_NAMES[5][c1] = PST_EG_PARAM_NAMES[11][c2];
      }
    }
  }

  private static long SHORT_CASTLED_W_KING_PAWN_SHIELD1 = Square.F2.bitboard | Square.G2.bitboard | Square.H2.bitboard;
  private static long SHORT_CASTLED_W_KING_PAWN_SHIELD2 = SHORT_CASTLED_W_KING_PAWN_SHIELD1 << 8;
  private static long SHORT_CASTLED_W_KING_PAWN_STORM1 = SHORT_CASTLED_W_KING_PAWN_SHIELD2;
  private static long SHORT_CASTLED_W_KING_PAWN_STORM2 = SHORT_CASTLED_W_KING_PAWN_STORM1 << 8;

  private static long SHORT_CASTLED_B_KING_PAWN_SHIELD1 = Square.F7.bitboard | Square.G7.bitboard | Square.H7.bitboard;
  private static long SHORT_CASTLED_B_KING_PAWN_SHIELD2 = SHORT_CASTLED_B_KING_PAWN_SHIELD1 >>> 8;
  private static long SHORT_CASTLED_B_KING_PAWN_STORM1 = SHORT_CASTLED_B_KING_PAWN_SHIELD2;
  private static long SHORT_CASTLED_B_KING_PAWN_STORM2 = SHORT_CASTLED_B_KING_PAWN_STORM1 >>> 8;

  private static long LONG_CASTLED_W_KING_PAWN_SHIELD1 = Square.A2.bitboard | Square.B2.bitboard | Square.C2.bitboard;
  private static long LONG_CASTLED_W_KING_PAWN_SHIELD2 = LONG_CASTLED_W_KING_PAWN_SHIELD1 << 8;
  private static long LONG_CASTLED_W_KING_PAWN_STORM1 = LONG_CASTLED_W_KING_PAWN_SHIELD2;
  private static long LONG_CASTLED_W_KING_PAWN_STORM2 = LONG_CASTLED_W_KING_PAWN_STORM1 << 8;

  private static long LONG_CASTLED_B_KING_PAWN_SHIELD1 = Square.A7.bitboard | Square.B7.bitboard | Square.C7.bitboard;
  private static long LONG_CASTLED_B_KING_PAWN_SHIELD2 = LONG_CASTLED_B_KING_PAWN_SHIELD1 >>> 8;
  private static long LONG_CASTLED_B_KING_PAWN_STORM1 = LONG_CASTLED_B_KING_PAWN_SHIELD2;
  private static long LONG_CASTLED_B_KING_PAWN_STORM2 = LONG_CASTLED_B_KING_PAWN_STORM1 >>> 8;

  private final DetroidParameters params;
  // Evaluation score hash table.
  private final Cache<ETEntry> evalTable;

  private short[][] pstMg;
  private short[][] pstEg;

  /**
   * Initializes a chess position evaluator.
   *
   * @param params A reference to the engine parameters.
   * @param evalTable A reference to the evaluation hash table to use.
   */
  public Evaluator(DetroidParameters params, Cache<ETEntry> evalTable) {
    this.params = params;
    this.evalTable = evalTable;
    initPieceSquareArrays();
  }

  /**
   * Returns whether the position is a case of insufficient material.
   *
   * @param pos The position to test.
   * @return Whether there is sufficient material on board to mate.
   */
  public static boolean isMaterialInsufficient(Position pos) {
    if (pos.getWhitePawns() != Bitboard.EMPTY_BOARD || pos.getBlackPawns() != Bitboard.EMPTY_BOARD ||
        pos.getWhiteRooks() != Bitboard.EMPTY_BOARD || pos.getBlackRooks() != Bitboard.EMPTY_BOARD ||
        pos.getWhiteQueens() != Bitboard.EMPTY_BOARD || pos.getBlackQueens() != Bitboard.EMPTY_BOARD) {
      return false;
    }
    int numOfWhiteKnights = BitOperations.hammingWeight(pos.getWhiteKnights());
    int numOfBlackKnights = BitOperations.hammingWeight(pos.getBlackKnights());
    int numOfAllPieces = BitOperations.hammingWeight(pos.getAllOccupied());
    if (numOfAllPieces == 2 || numOfAllPieces == 3) {
      return true;
    }
    if (numOfAllPieces >= 4 && numOfWhiteKnights == 0 && numOfBlackKnights == 0) {
      byte[] bishopSqrArr = BitOperations.serialize(pos.getWhiteBishops() | pos.getBlackBishops());
      int bishopColor = Diagonal.getBySquareIndex(bishopSqrArr[0]).ind % 2;
      for (int i = 1; i < bishopSqrArr.length; i++) {
        if (Diagonal.getBySquareIndex(bishopSqrArr[i]).ind % 2 != bishopColor) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private void initPieceSquareArrays() {
    short[] pstPawnMg = params.getPstPawnMg();
    short[] pstPawnEg = params.getPstPawnEg();
    short[] pstKnightMg = params.getPstKnightMg();
    short[] pstKnightEg = params.getPstKnightEg();
    short[] pstBishopMg = params.getPstBishopMg();
    short[] pstBishopEg = params.getPstBishopEg();
    short[] pstRookMg = params.getPstRookMg();
    short[] pstRookEg = params.getPstRookEg();
    short[] pstQueenMg = params.getPstQueenMg();
    short[] pstQueenEg = params.getPstQueenEg();
    short[] pstKingMg = params.getPstKingMg();
    short[] pstKingEg = params.getPstKingEg();
    short[] pstWPawnMg = new short[64];
    short[] pstWPawnEg = new short[64];
    short[] pstWKnightMg = new short[64];
    short[] pstWKnightEg = new short[64];
    short[] pstWBishopMg = new short[64];
    short[] pstWBishopEg = new short[64];
    short[] pstWRookMg = new short[64];
    short[] pstWRookEg = new short[64];
    short[] pstWQueenMg = new short[64];
    short[] pstWQueenEg = new short[64];
    short[] pstWKingMg = new short[64];
    short[] pstWKingEg = new short[64];
    short[] pstBPawnMg = new short[64];
    short[] pstBPawnEg = new short[64];
    short[] pstBKnightMg = new short[64];
    short[] pstBKnightEg = new short[64];
    short[] pstBBishopMg = new short[64];
    short[] pstBBishopEg = new short[64];
    short[] pstBRookMg = new short[64];
    short[] pstBRookEg = new short[64];
    short[] pstBQueenMg = new short[64];
    short[] pstBQueenEg = new short[64];
    short[] pstBKingMg = new short[64];
    short[] pstBKingEg = new short[64];
    /* Due to the reversed order of the rows in the definition of the white piece-square tables, they are just
     * right for black with negated values. */
    for (int i = 0; i < 64; i++) {
      pstBPawnMg[i] = (short) -pstPawnMg[i];
      pstBPawnEg[i] = (short) -pstPawnEg[i];
      pstBKnightMg[i] = (short) -pstKnightMg[i];
      pstBKnightEg[i] = (short) -pstKnightEg[i];
      pstBBishopMg[i] = (short) -pstBishopMg[i];
      pstBBishopEg[i] = (short) -pstBishopEg[i];
      pstBRookMg[i] = (short) -pstRookMg[i];
      pstBRookEg[i] = (short) -pstRookEg[i];
      pstBQueenMg[i] = (short) -pstQueenMg[i];
      pstBQueenEg[i] = (short) -pstQueenEg[i];
      pstBKingMg[i] = (short) -pstKingMg[i];
      pstBKingEg[i] = (short) -pstKingEg[i];
    }
    // To get the right values for the white piece-square tables, vertically mirror and negate the ones for black.
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        int c1 = i * 8 + j;
        int c2 = ((7 - i) * 8) + j;
        pstWPawnMg[c1] = (short) -pstBPawnMg[c2];
        pstWPawnEg[c1] = (short) -pstBPawnEg[c2];
        pstWKnightMg[c1] = (short) -pstBKnightMg[c2];
        pstWKnightEg[c1] = (short) -pstBKnightEg[c2];
        pstWBishopMg[c1] = (short) -pstBBishopMg[c2];
        pstWBishopEg[c1] = (short) -pstBBishopEg[c2];
        pstWRookMg[c1] = (short) -pstBRookMg[c2];
        pstWRookEg[c1] = (short) -pstBRookEg[c2];
        pstWQueenMg[c1] = (short) -pstBQueenMg[c2];
        pstWQueenEg[c1] = (short) -pstBQueenEg[c2];
        pstWKingMg[c1] = (short) -pstBKingMg[c2];
        pstWKingEg[c1] = (short) -pstBKingEg[c2];
      }
    }
    // Set the opening and endgame arrays of piece square tables.
    pstMg = new short[][]{pstWKingMg, pstWQueenMg, pstWRookMg, pstWBishopMg, pstWKnightMg, pstWPawnMg,
        pstBKingMg, pstBQueenMg, pstBRookMg, pstBBishopMg, pstBKnightMg, pstBPawnMg};
    pstEg = new short[][]{pstWKingEg, pstWQueenEg, pstWRookEg, pstWBishopEg, pstWKnightEg, pstWPawnEg,
        pstBKingEg, pstBQueenEg, pstBRookEg, pstBBishopEg, pstBKnightEg, pstBPawnEg};
  }

  /**
   * Returns the value of a piece type based on the engine parameters.
   *
   * @param pieceInd The index of the piece type.
   * @return The value of the piece type.
   */
  public short materialValueByPieceInd(int pieceInd) {
    if (pieceInd == Piece.W_KING.ind) {
      return KING_VALUE;
    } else if (pieceInd == Piece.W_QUEEN.ind) {
      return params.queenValue;
    } else if (pieceInd == Piece.W_ROOK.ind) {
      return params.rookValue;
    } else if (pieceInd == Piece.W_BISHOP.ind) {
      return params.bishopValue;
    } else if (pieceInd == Piece.W_KNIGHT.ind) {
      return params.knightValue;
    } else if (pieceInd == Piece.W_PAWN.ind) {
      return params.pawnValue;
    } else if (pieceInd == Piece.B_KING.ind) {
      return KING_VALUE;
    } else if (pieceInd == Piece.B_QUEEN.ind) {
      return params.queenValue;
    } else if (pieceInd == Piece.B_ROOK.ind) {
      return params.rookValue;
    } else if (pieceInd == Piece.B_BISHOP.ind) {
      return params.bishopValue;
    } else if (pieceInd == Piece.B_KNIGHT.ind) {
      return params.knightValue;
    } else if (pieceInd == Piece.B_PAWN.ind) {
      return params.pawnValue;
    } else if (pieceInd == Piece.NULL.ind) {
      return 0;
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Returns the MVV/LVA score of the specified move.
   *
   * @param move The move to score.
   * @return The MVV/LVA score of the move.
   */
  public static short MVVLVA(Move move) {
    short score = 0;
    if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
      byte queenValue = MVV_LVA_PIECE_VALUES[Piece.W_QUEEN.ind];
      score += queenValue * queenValue;
    }
    score += MVV_LVA[move.movedPiece][move.capturedPiece];
    return score;
  }

  /**
   * A static exchange evaluation algorithm for determining a close approximation of a capture's value.
   *
   * @param pos The position in which the move is to be evaluated.
   * @param move The move to score.
   * @return The SEE score of the move.
   */
  public short SEE(Position pos, Move move) {
    short victimVal = materialValueByPieceInd(move.capturedPiece);
    // If the captor was a king, return the captured piece's value as capturing the king would be illegal.
    if (move.movedPiece == Piece.W_KING.ind || move.movedPiece == Piece.B_KING.ind) {
      return victimVal;
    }
    int i = 0;
    short[] gains = new short[32];
    gains[i] = victimVal;
    short attackerVal;
    // In case the move is a promotion.
    if (move.type >= MoveType.PROMOTION_TO_QUEEN.ind) {
      if (move.type == MoveType.PROMOTION_TO_QUEEN.ind) {
        gains[i] += params.queenValue - params.pawnValue;
        attackerVal = params.queenValue;
      } else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
        gains[i] += params.rookValue - params.pawnValue;
        attackerVal = params.rookValue;
      } else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
        gains[i] += params.bishopValue - params.pawnValue;
        attackerVal = params.bishopValue;
      } else { // PROMOTION_TO_KNIGHT
        gains[i] += params.knightValue - params.pawnValue;
        attackerVal = params.knightValue;
      }
    } else {
      attackerVal = materialValueByPieceInd(move.movedPiece);
    }
    long occupied = pos.getAllOccupied() ^ BitOperations.toBit(move.from);
    boolean whitesTurn = pos.isWhitesTurn();
    MoveSetBase dB = MoveSetBase.getByIndex(move.to);
    // Assume the following order of value: 1. queen, 2. rook, 3. bishop, 4. knight.
    do {
      i++;
      gains[i] = (short) (attackerVal - gains[i - 1]);
      short prevAttackerVal = attackerVal;
      whitesTurn = !whitesTurn;
      long attackers, bpAttack, rkAttack;
      if (whitesTurn) {
        if ((attackers = dB.getBlackPawnCaptureSet(pos.getWhitePawns()) & occupied) != Bitboard.EMPTY_BOARD) {
          attackerVal = params.pawnValue;
        } else if ((attackers = dB.getKnightMoveSet(pos.getWhiteKnights()) & occupied) != Bitboard.EMPTY_BOARD) {
          attackerVal = params.knightValue;
        } else if ((attackers = (bpAttack = dB.getBishopMoveSet(occupied, occupied)) & pos.getWhiteBishops()) !=
            Bitboard.EMPTY_BOARD) {
          attackerVal = params.bishopValue;
        } else if ((attackers = (rkAttack = dB.getRookMoveSet(occupied, occupied)) & pos.getWhiteRooks()) !=
            Bitboard.EMPTY_BOARD) {
          attackerVal = params.rookValue;
        } else if ((attackers = (bpAttack | rkAttack) & pos.getWhiteQueens()) != Bitboard.EMPTY_BOARD) {
          attackerVal = params.queenValue;
        } else if ((attackers = dB.getKingMoveSet(pos.getWhiteKing())) != Bitboard.EMPTY_BOARD) {
          attackerVal = KING_VALUE;
        } else {
          break;
        }
      } else {
        if ((attackers = dB.getWhitePawnCaptureSet(pos.getBlackPawns()) & occupied) != Bitboard.EMPTY_BOARD) {
          attackerVal = params.pawnValue;
        } else if ((attackers = dB.getKnightMoveSet(pos.getBlackKnights()) & occupied) != Bitboard.EMPTY_BOARD) {
          attackerVal = params.knightValue;
        } else if ((attackers = (bpAttack = dB.getBishopMoveSet(occupied, occupied)) & pos.getBlackBishops()) !=
            Bitboard.EMPTY_BOARD) {
          attackerVal = params.bishopValue;
        } else if ((attackers = (rkAttack = dB.getRookMoveSet(occupied, occupied)) & pos.getBlackRooks()) !=
            Bitboard.EMPTY_BOARD) {
          attackerVal = params.rookValue;
        } else if ((attackers = (bpAttack | rkAttack) & pos.getBlackQueens()) != Bitboard.EMPTY_BOARD) {
          attackerVal = params.queenValue;
        } else if ((attackers = dB.getKingMoveSet(pos.getBlackKing())) != Bitboard.EMPTY_BOARD) {
          attackerVal = KING_VALUE;
        } else {
          break;
        }
      }
      // If the previous attacker was a king and the side to move can attack it, the exchange is over.
      if (prevAttackerVal == KING_VALUE) {
        break;
      }
      // Prune if engaging in further captures would result in material loss.
      if (Math.max(gains[i], -gains[i - 1]) < 0) {
        break;
      }
      // Simulate move.
      occupied ^= BitOperations.getLSBit(attackers);
    } while (true);
    while (--i > 0) {
      gains[i - 1] = (short) Math.min(-gains[i], gains[i - 1]);
    }
    return gains[0];
  }

  private static int phaseScore(int numOfQueens, int numOfRooks, int numOfBishops, int numOfKnights) {
    int phase = TOTAL_OPENING_PHASE_WEIGHT - (numOfQueens * QUEEN_PHASE_WEIGHT + numOfRooks * ROOK_PHASE_WEIGHT
        + numOfBishops * BISHOP_PHASE_WEIGHT + numOfKnights * KNIGHT_PHASE_WEIGHT);
    return (phase * MAX_PHASE_SCORE + TOTAL_OPENING_PHASE_WEIGHT / 2) / TOTAL_OPENING_PHASE_WEIGHT;
  }

  /**
   * Returns an estimation of the phase in which the current game is based on the given position.
   *
   * @param pos The position whose phase is to be gauged.
   * @return The phase estimate.
   */
  public static int phaseScore(Position pos) {
    int numOfQueens = BitOperations.hammingWeight(pos.getWhiteQueens() | pos.getBlackQueens());
    int numOfRooks = BitOperations.hammingWeight(pos.getWhiteRooks() | pos.getBlackRooks());
    int numOfBishops = BitOperations.hammingWeight(pos.getWhiteBishops() | pos.getBlackBishops());
    int numOfKnights = BitOperations.hammingWeight(pos.getWhiteKnights() | pos.getBlackKnights());
    return phaseScore(numOfQueens, numOfRooks, numOfBishops, numOfKnights);
  }

  private static int taperedEvalScore(int mgEval, int egEval, int phaseScore) {
    return (mgEval * (MAX_PHASE_SCORE - phaseScore) + egEval * phaseScore) / MAX_PHASE_SCORE;
  }

  private int getValueOfMostValuableRookCapture(long candidates, long queens, AtomicReference<String> victimParamName) {
    // If the attacker is a rook and it isn't attacking a queen, the exchange cannot be profitable.
    if ((queens & candidates) != Bitboard.EMPTY_BOARD) {
      if (victimParamName != null) {
        victimParamName.set("queenValue");
      }
      return params.queenValue;
    }
    return 0;
  }

  private int getValueOfMostValuableMinorPieceOrPawnCapture(long candidates, long queens, long rooks, long bishops, long knights,
      AtomicReference<String> victimParamName) {
    // The queen is clearly the most valuable piece.
    if ((queens & candidates) != Bitboard.EMPTY_BOARD) {
      if (victimParamName != null) {
        victimParamName.set("queenValue");
      }
      return params.queenValue;
    }
    // And the rook is clearly the second most valuable.
    if ((rooks & candidates) != Bitboard.EMPTY_BOARD) {
      if (victimParamName != null) {
        victimParamName.set("rookValue");
      }
      return params.rookValue;
    }
    // Beyond that, it gets contentious.
    int highestValue = 0;
    if ((bishops & candidates) != Bitboard.EMPTY_BOARD) {
      if (victimParamName != null) {
        victimParamName.set("bishopValue");
      }
      highestValue = params.bishopValue;
    }
    if (params.knightValue > params.bishopValue && ((knights & candidates) != Bitboard.EMPTY_BOARD)) {
      if (victimParamName != null) {
        victimParamName.set("knightValue");
      }
      highestValue = params.knightValue;
    }
    return highestValue;
  }

  /**
   * A static evaluation of the chess position from the color to move's point of view. It considers material imbalance, coverage, pawn
   * structure, queen-king tropism, mobility, immediate captures, etc. It assumes that the position is not a check.
   *
   * @param pos The position to score.
   * @param hashGen The hash generation.
   * @param entry The pre-constructed evaluation table entry.
   * @param gradientCache An optional map for storing the gradient of the evaluation function w.r.t. the parameters used.
   * @return The score of the position.
   */
  public short score(Position pos, byte hashGen, ETEntry entry, Map<String, Double> gradientCache) {
    // Probe evaluation hash table.
    if (evalTable != null) {
      ETEntry eE = evalTable.get(pos.getKey());
      if (eE != null && eE.hashKey() == pos.getKey()) {
        eE.setGeneration(hashGen);
        return eE.getScore();
      }
    }
    short score = 0;
    int mostValuableExchange = 0;
    // In case of no hash hit, calculate the base score from scratch.
    byte numOfWhiteQueens = BitOperations.hammingWeight(pos.getWhiteQueens());
    byte numOfWhiteRooks = BitOperations.hammingWeight(pos.getWhiteRooks());
    byte numOfWhiteBishops = BitOperations.hammingWeight(pos.getWhiteBishops());
    byte numOfWhiteKnights = BitOperations.hammingWeight(pos.getWhiteKnights());
    byte numOfBlackQueens = BitOperations.hammingWeight(pos.getBlackQueens());
    byte numOfBlackRooks = BitOperations.hammingWeight(pos.getBlackRooks());
    byte numOfBlackBishops = BitOperations.hammingWeight(pos.getBlackBishops());
    byte numOfBlackKnights = BitOperations.hammingWeight(pos.getBlackKnights());
    // Check for insufficient material. Only consider the widely acknowledged scenarios without blocked position testing.
    if (pos.getWhitePawns() == 0 && pos.getBlackPawns() == 0 && numOfWhiteRooks == 0 && numOfBlackRooks == 0 &&
        numOfWhiteQueens == 0 && numOfBlackQueens == 0) {
      byte numOfAllPieces = BitOperations.hammingWeight(pos.getAllOccupied());
      if (numOfAllPieces == 2 || numOfAllPieces == 3) {
        return Score.INSUFFICIENT_MATERIAL.value;
      }
      if (numOfAllPieces >= 4 && numOfWhiteKnights == 0 && numOfBlackKnights == 0) {
        boolean allSameColor = true;
        long bishopSet = pos.getWhiteBishops() | pos.getBlackBishops();
        int bishopSqrColor = Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(bishopSet)).ind % 2;
        bishopSet = BitOperations.resetLSBit(bishopSet);
        while (bishopSet != 0) {
          if (Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(bishopSet)).ind % 2 != bishopSqrColor) {
            allSameColor = false;
            break;
          }
          bishopSet = BitOperations.resetLSBit(bishopSet);
        }
        if (allSameColor) {
          return Score.INSUFFICIENT_MATERIAL.value;
        }
      }
    }
    byte numOfWhitePawns = BitOperations.hammingWeight(pos.getWhitePawns());
    byte numOfBlackPawns = BitOperations.hammingWeight(pos.getBlackPawns());
    // Base material score.
    int numOfQueensDiff = numOfWhiteQueens - numOfBlackQueens;
    int numOfRooksDiff = numOfWhiteRooks - numOfBlackRooks;
    int numOfBishopsDiff = numOfWhiteBishops - numOfBlackBishops;
    int numOfKnightsDiff = numOfWhiteKnights - numOfBlackKnights;
    int numOfPawnsDiff = numOfWhitePawns - numOfBlackPawns;
    score += params.queenValue * numOfQueensDiff;
    score += params.rookValue * numOfRooksDiff;
    score += params.bishopValue * numOfBishopsDiff;
    score += params.knightValue * numOfKnightsDiff;
    score += params.pawnValue * numOfPawnsDiff;
    // Bishop pair advantage.
    int bishopPairAdvantageDiff = 0;
    if (numOfWhiteBishops >= 2 && Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(
        pos.getWhiteBishops())).ind % 2 != Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(
        BitOperations.resetLSBit(pos.getWhiteBishops()))).ind % 2) {
      bishopPairAdvantageDiff += 1;
    }
    if (numOfBlackBishops >= 2 && Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(
        pos.getBlackBishops())).ind % 2 != Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(
        BitOperations.resetLSBit(pos.getBlackBishops()))).ind % 2) {
      bishopPairAdvantageDiff -= 1;
    }
    score += params.bishopPairAdvantage * bishopPairAdvantageDiff;
    // Stopped pawns.
    int numOfStoppedPawnsDiff = BitOperations.hammingWeight(Bitboard.computeBlackPawnAdvanceSets(pos.getBlackPawns(),
        Bitboard.FULL_BOARD) & (pos.getAllWhiteOccupied() ^ pos.getWhitePawns())) -
        BitOperations.hammingWeight(Bitboard.computeWhitePawnAdvanceSets(pos.getWhitePawns(),
            Bitboard.FULL_BOARD) & (pos.getAllBlackOccupied() ^ pos.getBlackPawns()));
    score += params.stoppedPawnWeight * numOfStoppedPawnsDiff;
    // Pinned pieces.
    long whiteKing = pos.getWhiteKing();
    long blackKing = pos.getBlackKing();
    byte whiteKingInd = BitOperations.indexOfBit(whiteKing);
    byte blackKingInd = BitOperations.indexOfBit(blackKing);
    long whitePinningPieces = Bitboard.getPinningPieces(blackKingInd, pos.getWhiteQueens() | pos.getWhiteRooks(),
        pos.getWhiteQueens() | pos.getWhiteBishops(), pos.getAllOccupied(), pos.getAllBlackOccupied());
    long blackPinningPieces = Bitboard.getPinningPieces(whiteKingInd, pos.getBlackQueens() | pos.getBlackRooks(),
        pos.getBlackQueens() | pos.getBlackBishops(), pos.getAllOccupied(), pos.getAllWhiteOccupied());
    long[] whitePinLines = new long[BitOperations.hammingWeight(whitePinningPieces)];
    long temp = whitePinningPieces;
    long blackPinnedPieces = Bitboard.EMPTY_BOARD;
    for (int i = 0; i < whitePinLines.length; i++) {
      byte pinnerInd = BitOperations.indexOfLSBit(temp);
      long pinLine = Bitboard.getLineSegment(blackKingInd, pinnerInd) | (1L << pinnerInd);
      whitePinLines[i] = pinLine;
      blackPinnedPieces |= (pinLine & pos.getAllBlackOccupied());
      temp = BitOperations.resetLSBit(temp);
    }
    long[] blackPinLines = new long[BitOperations.hammingWeight(blackPinningPieces)];
    temp = blackPinningPieces;
    long whitePinnedPieces = Bitboard.EMPTY_BOARD;
    for (int i = 0; i < blackPinLines.length; i++) {
      byte pinnerInd = BitOperations.indexOfLSBit(temp);
      long pinLine = Bitboard.getLineSegment(blackKingInd, pinnerInd) | (1L << pinnerInd);
      blackPinLines[i] = pinLine;
      whitePinnedPieces |= (pinLine & pos.getAllWhiteOccupied());
      temp = BitOperations.resetLSBit(temp);
    }
    int numOfPinnedQueensDiff = BitOperations.hammingWeight(pos.getBlackQueens() & blackPinnedPieces) -
        BitOperations.hammingWeight(pos.getWhiteQueens() & whitePinnedPieces);
    int numOfPinnedRooksDiff = BitOperations.hammingWeight(pos.getBlackRooks() & blackPinnedPieces) -
        BitOperations.hammingWeight(pos.getWhiteRooks() & whitePinnedPieces);
    int numOfPinnedBishopsDiff = BitOperations.hammingWeight(pos.getBlackBishops() & blackPinnedPieces) -
        BitOperations.hammingWeight(pos.getWhiteBishops() & whitePinnedPieces);
    int numOfPinnedKnightsDiff = BitOperations.hammingWeight(pos.getBlackKnights() & blackPinnedPieces) -
        BitOperations.hammingWeight(pos.getWhiteKnights() & whitePinnedPieces);
    int numOfPinnedPawnsDiff = BitOperations.hammingWeight(pos.getBlackPawns() & blackPinnedPieces) -
        BitOperations.hammingWeight(pos.getWhitePawns() & whitePinnedPieces);
    score += params.pinnedQueenWeight * numOfPinnedQueensDiff;
    score += params.pinnedRookWeight * numOfPinnedRooksDiff;
    score += params.pinnedBishopWeight * numOfPinnedBishopsDiff;
    score += params.pinnedKnightWeight * numOfPinnedKnightsDiff;
    score += params.pinnedPawnWeight * numOfPinnedPawnsDiff;
    // Pawn shield and pawn storm.
    long whitePawns = pos.getWhitePawns();
    long blackPawns = pos.getBlackPawns();
    int pawnShield1Diff = 0;
    int pawnShield2Diff = 0;
    int pawnStorm1Diff = 0;
    int pawnStorm2Diff = 0;
    if (whiteKing == Square.G1.bitboard) {
      pawnShield1Diff += BitOperations.hammingWeight(whitePawns & SHORT_CASTLED_W_KING_PAWN_SHIELD1);
      pawnShield2Diff += BitOperations.hammingWeight(whitePawns & SHORT_CASTLED_W_KING_PAWN_SHIELD2);
      pawnStorm1Diff -= BitOperations.hammingWeight(blackPawns & SHORT_CASTLED_W_KING_PAWN_STORM1);
      pawnStorm2Diff -= BitOperations.hammingWeight(blackPawns & SHORT_CASTLED_W_KING_PAWN_STORM2);
    } else if (whiteKing == Square.C1.bitboard) {
      pawnShield1Diff += BitOperations.hammingWeight(whitePawns & LONG_CASTLED_W_KING_PAWN_SHIELD1);
      pawnShield2Diff += BitOperations.hammingWeight(whitePawns & LONG_CASTLED_W_KING_PAWN_SHIELD2);
      pawnStorm1Diff -= BitOperations.hammingWeight(blackPawns & LONG_CASTLED_W_KING_PAWN_STORM1);
      pawnStorm2Diff -= BitOperations.hammingWeight(blackPawns & LONG_CASTLED_W_KING_PAWN_STORM2);
    }
    if (blackKing == Square.G8.bitboard) {
      pawnShield1Diff -= BitOperations.hammingWeight(blackPawns & SHORT_CASTLED_B_KING_PAWN_SHIELD1);
      pawnShield2Diff -= BitOperations.hammingWeight(blackPawns & SHORT_CASTLED_B_KING_PAWN_SHIELD2);
      pawnStorm1Diff += BitOperations.hammingWeight(whitePawns & SHORT_CASTLED_B_KING_PAWN_STORM1);
      pawnStorm2Diff += BitOperations.hammingWeight(whitePawns & SHORT_CASTLED_B_KING_PAWN_STORM2);
    } else if (blackKing == Square.C8.bitboard) {
      pawnShield1Diff -= BitOperations.hammingWeight(blackPawns & LONG_CASTLED_B_KING_PAWN_SHIELD1);
      pawnShield2Diff -= BitOperations.hammingWeight(blackPawns & LONG_CASTLED_B_KING_PAWN_SHIELD2);
      pawnStorm1Diff += BitOperations.hammingWeight(whitePawns & LONG_CASTLED_B_KING_PAWN_STORM1);
      pawnStorm2Diff += BitOperations.hammingWeight(whitePawns & LONG_CASTLED_B_KING_PAWN_STORM2);
    }
    score += params.pawnShieldWeight1 * pawnShield1Diff;
    score += params.pawnShieldWeight2 * pawnShield2Diff;
    score += params.pawnStormWeight1 * pawnStorm1Diff;
    score += params.pawnStormWeight2 * pawnStorm2Diff;
    // Blocked pawns.
    int numOfBlockedPawnsDiff1 = (BitOperations.hammingWeight((blackPawns >>> 8) & blackPawns) -
        BitOperations.hammingWeight((whitePawns << 8) & whitePawns));
    int numOfBlockedPawnsDiff2 = (BitOperations.hammingWeight((blackPawns >>> 16) & blackPawns) -
        BitOperations.hammingWeight((whitePawns << 16) & whitePawns));
    int numOfBlockedPawnsDiff3 = (BitOperations.hammingWeight((blackPawns >>> 24) & blackPawns) -
        BitOperations.hammingWeight((whitePawns << 24) & whitePawns));
    score += params.blockedPawnWeight1 * numOfBlockedPawnsDiff1;
    score += params.blockedPawnWeight2 * numOfBlockedPawnsDiff2;
    score += params.blockedPawnWeight3 * numOfBlockedPawnsDiff3;
    // Passed pawns.
    long whiteAdvanceSpans = Bitboard.fillNorth(whitePawns) << 8;
    long whiteAttackSpans = ((whiteAdvanceSpans >>> 1) & ~File.H.bitboard) | ((whiteAdvanceSpans << 1) & ~File.A.bitboard);
    long whiteFrontSpans = whiteAdvanceSpans | whiteAttackSpans;
    long blackAdvanceSpans = Bitboard.fillSouth(blackPawns) >>> 8;
    long blackAttackSpans = ((blackAdvanceSpans >>> 1) & ~File.H.bitboard) | ((blackAdvanceSpans << 1) & ~File.A.bitboard);
    long blackFrontSpans = blackAdvanceSpans | blackAttackSpans;
    long whitePassedPawns = whitePawns & ~blackFrontSpans & ~whiteAdvanceSpans;
    long blackPassedPawns = blackPawns & ~whiteFrontSpans & ~blackAdvanceSpans;
    int numOfPassedPawnsDiff = (BitOperations.hammingWeight(whitePassedPawns) - BitOperations.hammingWeight(blackPassedPawns));
    score += params.passedPawnWeight * numOfPassedPawnsDiff;
    // Isolated pawns.
    long whiteSideSpans = Bitboard.fillSouth(whiteAttackSpans);
    long blackSideSpans = Bitboard.fillNorth(blackAttackSpans);
    long whiteIsolatedPawns = whitePawns & ~whiteSideSpans;
    long blackIsolatedPawns = blackPawns & ~blackSideSpans;
    int numOfIsolatedPawnsDiff = (BitOperations.hammingWeight(blackIsolatedPawns) - BitOperations.hammingWeight(whiteIsolatedPawns));
    score += params.isolatedPawnWeight * numOfIsolatedPawnsDiff;
    // Backward pawns.
    long whitePawnCaptures = Bitboard.computeWhitePawnCaptureSets(pos.getWhitePawns() & ~whitePinnedPieces, Bitboard.FULL_BOARD);
    long blackPawnCaptures = Bitboard.computeBlackPawnCaptureSets(pos.getBlackPawns() & ~blackPinnedPieces, Bitboard.FULL_BOARD);
    long whiteBackwardPawns = whitePawns & ((blackPawnCaptures & ~whiteAttackSpans) >>> 8);
    long blackBackwardPawns = blackPawns & ((whitePawnCaptures & ~blackAttackSpans) << 8);
    int numOfBackwardPawnsDiff = (BitOperations.hammingWeight(blackBackwardPawns) - BitOperations.hammingWeight(whiteBackwardPawns));
    score += params.backwardPawnWeight * numOfBackwardPawnsDiff;
    long whiteWeakPawns = whiteIsolatedPawns | whiteBackwardPawns;
    long blackWeakPawns = blackIsolatedPawns | blackBackwardPawns;
    // Piece defense.
    int numOfPawnDefendedQueensDiff = BitOperations.hammingWeight(pos.getWhiteQueens() & whitePawnCaptures) -
        BitOperations.hammingWeight(pos.getBlackQueens() & blackPawnCaptures);
    int numOfPawnDefendedRooksDiff = BitOperations.hammingWeight(pos.getWhiteRooks() & whitePawnCaptures) -
        BitOperations.hammingWeight(pos.getBlackRooks() & blackPawnCaptures);
    int numOfPawnDefendedBishopsDiff = BitOperations.hammingWeight(pos.getWhiteBishops() & whitePawnCaptures) -
        BitOperations.hammingWeight(pos.getBlackBishops() & blackPawnCaptures);
    int numOfPawnDefendedKnightsDiff = BitOperations.hammingWeight(pos.getWhiteKnights() & whitePawnCaptures) -
        BitOperations.hammingWeight(pos.getBlackKnights() & blackPawnCaptures);
    int numOfPawnDefendedPawnsDiff = BitOperations.hammingWeight(pos.getWhitePawns() & whitePawnCaptures) -
        BitOperations.hammingWeight(pos.getBlackPawns() & blackPawnCaptures);
    score += params.queenDefenseWeight * numOfPawnDefendedQueensDiff;
    score += params.rookDefenseWeight * numOfPawnDefendedRooksDiff;
    score += params.bishopDefenseWeight * numOfPawnDefendedBishopsDiff;
    score += params.knightDefenseWeight * numOfPawnDefendedKnightsDiff;
    score += params.pawnDefenseWeight * numOfPawnDefendedPawnsDiff;
    // Iterate over pieces to assess their mobility and distance from the opponent's king.
    long whiteKingZone = pos.getWhiteKing() | Bitboard.computeKingMoveSets(whiteKing, Bitboard.FULL_BOARD);
    whiteKingZone |= whiteKingZone << 8;
    long blackKingZone = pos.getBlackKing() | Bitboard.computeKingMoveSets(blackKing, Bitboard.FULL_BOARD);
    whiteKingZone |= whiteKingZone >>> 8;
    long attackedWhiteKingZoneSquares = 0;
    long attackedBlackKingZoneSquares = 0;
    int numKingZoneAttackersDiff = 0;
    long whitePieceSet = pos.getAllWhiteOccupied() ^ whiteKing;
    long blackPieceSet = pos.getAllBlackOccupied() ^ blackKing;
    int whiteQueenWhiteKingTropism = 0;
    int whiteRookWhiteKingTropism = 0;
    int whiteBishopWhiteKingTropism = 0;
    int whiteKnightWhiteKingTropism = 0;
    int whiteQueenBlackKingTropism = 0;
    int whiteRookBlackKingTropism = 0;
    int whiteBishopBlackKingTropism = 0;
    int whiteKnightBlackKingTropism = 0;
    int whitePassedPawnWhiteKingTropism = 0;
    int whiteWeakPawnWhiteKingTropism = 0;
    int whiteNormalPawnWhiteKingTropism = 0;
    int whitePassedPawnBlackKingTropism = 0;
    int whiteWeakPawnBlackKingTropism = 0;
    int whiteNormalPawnBlackKingTropism = 0;
    long whitePawnAttacks = Bitboard.EMPTY_BOARD;
    long whiteKnightAttacks = Bitboard.EMPTY_BOARD;
    long whiteBishopAttacks = Bitboard.EMPTY_BOARD;
    long whiteRookAttacks = Bitboard.EMPTY_BOARD;
    int whiteQueenMobility = 0;
    int whiteRookMobility = 0;
    int whiteBishopMobility = 0;
    int whiteKnightMobility = 0;
    int whitePawnMobility = 0;
    while (whitePieceSet != Bitboard.EMPTY_BOARD) {
      long piece = BitOperations.getLSBit(whitePieceSet);
      long pinnedPieceMoveSetRestriction = Bitboard.FULL_BOARD;
      if ((piece & whitePinnedPieces) != Bitboard.EMPTY_BOARD) {
        for (long n : blackPinLines) {
          if ((piece & n) != Bitboard.EMPTY_BOARD) {
            pinnedPieceMoveSetRestriction = n;
            break;
          }
        }
      }
      byte pieceInd = BitOperations.indexOfBit(piece);
      byte pieceType = pos.getPiece(pieceInd);
      MoveSetBase moveSetDb = MoveSetBase.getByIndex(pieceInd);
      long unrestrictedMoveSet;
      if (pieceType == Piece.W_QUEEN.ind) {
        unrestrictedMoveSet = moveSetDb.getQueenMoveSet(pos.getAllNonWhiteOccupied(), pos.getAllOccupied());
        long moveSet = unrestrictedMoveSet & pinnedPieceMoveSetRestriction;
        whiteQueenMobility += BitOperations.hammingWeight(moveSet);
        whiteQueenWhiteKingTropism += CHEBYSHEV_DISTANCE[pieceInd][whiteKingInd];
        whiteQueenBlackKingTropism += CHEBYSHEV_DISTANCE[pieceInd][blackKingInd];
      } else if (pieceType == Piece.W_ROOK.ind) {
        unrestrictedMoveSet = moveSetDb.getRookMoveSet(pos.getAllNonWhiteOccupied(), pos.getAllOccupied());
        long moveSet = unrestrictedMoveSet & pinnedPieceMoveSetRestriction;
        whiteRookAttacks |= moveSet;
        whiteRookMobility += BitOperations.hammingWeight(moveSet);
        whiteRookWhiteKingTropism += CHEBYSHEV_DISTANCE[pieceInd][whiteKingInd];
        whiteRookBlackKingTropism += CHEBYSHEV_DISTANCE[pieceInd][blackKingInd];
      } else if (pieceType == Piece.W_BISHOP.ind) {
        unrestrictedMoveSet = moveSetDb.getBishopMoveSet(pos.getAllNonWhiteOccupied(), pos.getAllOccupied());
        long moveSet = unrestrictedMoveSet & pinnedPieceMoveSetRestriction;
        whiteBishopAttacks |= moveSet;
        whiteBishopMobility += BitOperations.hammingWeight(moveSet);
        whiteBishopWhiteKingTropism += CHEBYSHEV_DISTANCE[pieceInd][whiteKingInd];
        whiteBishopBlackKingTropism += CHEBYSHEV_DISTANCE[pieceInd][blackKingInd];
      } else if (pieceType == Piece.W_KNIGHT.ind) {
        unrestrictedMoveSet = moveSetDb.getKnightMoveSet(pos.getAllNonWhiteOccupied());
        long moveSet = unrestrictedMoveSet & pinnedPieceMoveSetRestriction;
        whiteKnightAttacks |= moveSet;
        whiteKnightMobility += BitOperations.hammingWeight(moveSet);
        whiteKnightWhiteKingTropism += CHEBYSHEV_DISTANCE[pieceInd][whiteKingInd];
        whiteKnightBlackKingTropism += CHEBYSHEV_DISTANCE[pieceInd][blackKingInd];
      } else { // W_PAWN
        unrestrictedMoveSet = moveSetDb.getWhitePawnMoveSet(pos.getAllBlackOccupied(), pos.getAllEmpty());
        long moveSet = unrestrictedMoveSet & pinnedPieceMoveSetRestriction;
        whitePawnAttacks |= moveSet;
        whitePawnMobility += BitOperations.hammingWeight(moveSet);
        if ((whitePassedPawns & piece) != Bitboard.EMPTY_BOARD) {
          whitePassedPawnWhiteKingTropism += MANHATTAN_DISTANCE[pieceInd][whiteKingInd];
          whitePassedPawnBlackKingTropism += MANHATTAN_DISTANCE[pieceInd][blackKingInd];
        } else if ((whiteWeakPawns & piece) != Bitboard.EMPTY_BOARD) {
          whiteWeakPawnWhiteKingTropism += MANHATTAN_DISTANCE[pieceInd][whiteKingInd];
          whiteWeakPawnBlackKingTropism += MANHATTAN_DISTANCE[pieceInd][blackKingInd];
        } else {
          whiteNormalPawnWhiteKingTropism += MANHATTAN_DISTANCE[pieceInd][whiteKingInd];
          whiteNormalPawnBlackKingTropism += MANHATTAN_DISTANCE[pieceInd][blackKingInd];
        }
      }
      long attackedKingZoneSquares = unrestrictedMoveSet & blackKingZone;
      if (attackedKingZoneSquares != Bitboard.EMPTY_BOARD) {
        numKingZoneAttackersDiff++;
        attackedWhiteKingZoneSquares |= attackedKingZoneSquares;
      }
      whitePieceSet = BitOperations.resetLSBit(whitePieceSet);
    }
    int blackQueenBlackKingTropism = 0;
    int blackRookBlackKingTropism = 0;
    int blackBishopBlackKingTropism = 0;
    int blackKnightBlackKingTropism = 0;
    int blackQueenWhiteKingTropism = 0;
    int blackRookWhiteKingTropism = 0;
    int blackBishopWhiteKingTropism = 0;
    int blackKnightWhiteKingTropism = 0;
    int blackPassedPawnWhiteKingTropism = 0;
    int blackWeakPawnWhiteKingTropism = 0;
    int blackNormalPawnWhiteKingTropism = 0;
    int blackPassedPawnBlackKingTropism = 0;
    int blackWeakPawnBlackKingTropism = 0;
    int blackNormalPawnBlackKingTropism = 0;
    long blackPawnAttacks = Bitboard.EMPTY_BOARD;
    long blackKnightAttacks = Bitboard.EMPTY_BOARD;
    long blackBishopAttacks = Bitboard.EMPTY_BOARD;
    long blackRookAttacks = Bitboard.EMPTY_BOARD;
    int blackQueenMobility = 0;
    int blackRookMobility = 0;
    int blackBishopMobility = 0;
    int blackKnightMobility = 0;
    int blackPawnMobility = 0;
    while (blackPieceSet != Bitboard.EMPTY_BOARD) {
      long piece = BitOperations.getLSBit(blackPieceSet);
      long pinnedPieceMoveSetRestriction = Bitboard.FULL_BOARD;
      if ((piece & blackPinnedPieces) != Bitboard.EMPTY_BOARD) {
        for (long n : whitePinLines) {
          if ((piece & n) != Bitboard.EMPTY_BOARD) {
            pinnedPieceMoveSetRestriction = n;
            break;
          }
        }
      }
      byte pieceInd = BitOperations.indexOfBit(piece);
      byte pieceType = pos.getPiece(pieceInd);
      MoveSetBase moveSetDb = MoveSetBase.getByIndex(pieceInd);
      long unrestrictedMoveSet;
      if (pieceType == Piece.B_QUEEN.ind) {
        unrestrictedMoveSet = moveSetDb.getQueenMoveSet(pos.getAllNonBlackOccupied(), pos.getAllOccupied());
        long moveSet = unrestrictedMoveSet & pinnedPieceMoveSetRestriction;
        blackQueenMobility += BitOperations.hammingWeight(moveSet);
        blackQueenWhiteKingTropism += CHEBYSHEV_DISTANCE[pieceInd][whiteKingInd];
        blackQueenBlackKingTropism += CHEBYSHEV_DISTANCE[pieceInd][blackKingInd];
      } else if (pieceType == Piece.B_ROOK.ind) {
        unrestrictedMoveSet = moveSetDb.getRookMoveSet(pos.getAllNonBlackOccupied(), pos.getAllOccupied());
        long moveSet = unrestrictedMoveSet & pinnedPieceMoveSetRestriction;
        blackRookAttacks |= moveSet;
        blackRookMobility += BitOperations.hammingWeight(moveSet);
        blackRookWhiteKingTropism += CHEBYSHEV_DISTANCE[pieceInd][whiteKingInd];
        blackRookBlackKingTropism += CHEBYSHEV_DISTANCE[pieceInd][blackKingInd];
      } else if (pieceType == Piece.B_BISHOP.ind) {
        unrestrictedMoveSet = moveSetDb.getBishopMoveSet(pos.getAllNonBlackOccupied(), pos.getAllOccupied());
        long moveSet = unrestrictedMoveSet & pinnedPieceMoveSetRestriction;
        blackBishopAttacks |= moveSet;
        blackBishopMobility += BitOperations.hammingWeight(moveSet);
        blackBishopWhiteKingTropism += CHEBYSHEV_DISTANCE[pieceInd][whiteKingInd];
        blackBishopBlackKingTropism += CHEBYSHEV_DISTANCE[pieceInd][blackKingInd];
      } else if (pieceType == Piece.B_KNIGHT.ind) {
        unrestrictedMoveSet = moveSetDb.getKnightMoveSet(pos.getAllNonBlackOccupied());
        long moveSet = unrestrictedMoveSet & pinnedPieceMoveSetRestriction;
        blackKnightAttacks |= moveSet;
        blackKnightMobility += BitOperations.hammingWeight(moveSet);
        blackKnightWhiteKingTropism += CHEBYSHEV_DISTANCE[pieceInd][whiteKingInd];
        blackKnightBlackKingTropism += CHEBYSHEV_DISTANCE[pieceInd][blackKingInd];
      } else { // B_PAWN
        unrestrictedMoveSet = moveSetDb.getBlackPawnMoveSet(pos.getAllWhiteOccupied(), pos.getAllEmpty());
        long moveSet = unrestrictedMoveSet & pinnedPieceMoveSetRestriction;
        blackPawnAttacks |= moveSet;
        blackPawnMobility += BitOperations.hammingWeight(moveSet);
        if ((blackPassedPawns & piece) != Bitboard.EMPTY_BOARD) {
          blackPassedPawnBlackKingTropism += MANHATTAN_DISTANCE[pieceInd][blackKingInd];
          blackPassedPawnWhiteKingTropism += MANHATTAN_DISTANCE[pieceInd][whiteKingInd];
        } else if ((blackWeakPawns & piece) != Bitboard.EMPTY_BOARD) {
          blackWeakPawnBlackKingTropism += MANHATTAN_DISTANCE[pieceInd][blackKingInd];
          blackWeakPawnWhiteKingTropism += MANHATTAN_DISTANCE[pieceInd][whiteKingInd];
        } else {
          blackNormalPawnBlackKingTropism += MANHATTAN_DISTANCE[pieceInd][blackKingInd];
          blackNormalPawnWhiteKingTropism += MANHATTAN_DISTANCE[pieceInd][whiteKingInd];
        }
      }
      long attackedKingZoneSquares = unrestrictedMoveSet & whiteKingZone;
      if (attackedKingZoneSquares != Bitboard.EMPTY_BOARD) {
        numKingZoneAttackersDiff--;
        attackedBlackKingZoneSquares |= attackedKingZoneSquares;
      }
      blackPieceSet = BitOperations.resetLSBit(blackPieceSet);
    }
    // Mobility scores.
    int queenMobilityDiff = whiteQueenMobility - blackQueenMobility;
    int rookMobilityDiff = whiteRookMobility - blackRookMobility;
    int bishopMobilityDiff = whiteBishopMobility - blackBishopMobility;
    int knightMobilityDiff = whiteKnightMobility - blackKnightMobility;
    int pawnMobilityDiff = whitePawnMobility - blackPawnMobility;
    score += params.queenMobilityWeight * queenMobilityDiff;
    score += params.rookMobilityWeight * rookMobilityDiff;
    score += params.bishopMobilityWeight * bishopMobilityDiff;
    score += params.knightMobilityWeight * knightMobilityDiff;
    score += params.pawnMobilityWeight * pawnMobilityDiff;
    // Piece-king tropism.
    int friendlyQueenKingTropismDiff = blackQueenBlackKingTropism - whiteQueenWhiteKingTropism;
    int friendlyRookKingTropismDiff = blackRookBlackKingTropism - whiteRookWhiteKingTropism;
    int friendlyBishopKingTropismDiff = blackBishopBlackKingTropism - whiteBishopWhiteKingTropism;
    int friendlyKnightKingTropismDiff = blackKnightBlackKingTropism - whiteKnightWhiteKingTropism;
    int friendlyPassedPawnKingTropismDiff = blackPassedPawnBlackKingTropism - whitePassedPawnWhiteKingTropism;
    int friendlyWeakPawnKingTropismDiff = blackWeakPawnBlackKingTropism - whiteWeakPawnWhiteKingTropism;
    int friendlyNormalPawnKingTropismDiff = blackNormalPawnBlackKingTropism - whiteNormalPawnWhiteKingTropism;
    int opponentQueenKingTropismDiff = blackQueenWhiteKingTropism - whiteQueenBlackKingTropism;
    int opponentRookKingTropismDiff = blackRookWhiteKingTropism - whiteRookBlackKingTropism;
    int opponentBishopKingTropismDiff = blackBishopWhiteKingTropism - whiteBishopBlackKingTropism;
    int opponentKnightKingTropismDiff = blackKnightWhiteKingTropism - whiteKnightBlackKingTropism;
    int opponentPassedPawnKingTropismDiff = whitePassedPawnBlackKingTropism - blackPassedPawnWhiteKingTropism;
    int opponentWeakPawnKingTropismDiff = whiteWeakPawnBlackKingTropism - blackWeakPawnWhiteKingTropism;
    int opponentNormalPawnKingTropismDiff = whiteNormalPawnBlackKingTropism - blackNormalPawnWhiteKingTropism;
    score += params.friendlyQueenTropismWeight * friendlyQueenKingTropismDiff;
    score += params.friendlyRookTropismWeight * friendlyRookKingTropismDiff;
    score += params.friendlyBishopTropismWeight * friendlyBishopKingTropismDiff;
    score += params.friendlyKnightTropismWeight * friendlyKnightKingTropismDiff;
    score += params.friendlyPassedPawnTropismWeight * friendlyPassedPawnKingTropismDiff;
    score += params.friendlyWeakPawnTropismWeight * friendlyWeakPawnKingTropismDiff;
    score += params.friendlyNormalPawnTropismWeight * friendlyNormalPawnKingTropismDiff;
    score += params.opponentQueenTropismWeight * opponentQueenKingTropismDiff;
    score += params.opponentRookTropismWeight * opponentRookKingTropismDiff;
    score += params.opponentBishopTropismWeight * opponentBishopKingTropismDiff;
    score += params.opponentKnightTropismWeight * opponentKnightKingTropismDiff;
    score += params.opponentPassedPawnTropismWeight * opponentPassedPawnKingTropismDiff;
    score += params.opponentWeakPawnTropismWeight * opponentWeakPawnKingTropismDiff;
    score += params.opponentNormalPawnTropismWeight * opponentNormalPawnKingTropismDiff;
    // King zone attacks.
    int uniqueAttackedKingZoneSquaresDiff = BitOperations.hammingWeight(attackedBlackKingZoneSquares) -
        BitOperations.hammingWeight(attackedWhiteKingZoneSquares);
    score += params.attackedKingZoneSquareWeight * uniqueAttackedKingZoneSquaresDiff;
    score += params.kingZoneAttackerWeight * numKingZoneAttackersDiff;
    // Phase score for tapered evaluation.
    int phaseScore = phaseScore(numOfWhiteQueens + numOfBlackQueens, numOfWhiteRooks + numOfBlackRooks,
        numOfWhiteBishops + numOfBlackBishops, numOfWhiteKnights + numOfBlackKnights);
    short mgScore = 0;
    short egScore = 0;
    if (gradientCache == null) {
      // Piece-square scores.
      for (int i = 0; i < 64; i++) {
        byte piece = (byte) (pos.getPiece(i) - 1);
        if (piece < Piece.NULL.ind) {
          continue;
        }
        mgScore += pstMg[piece][i];
        egScore += pstEg[piece][i];
      }
      // Asymmetric evaluation terms for possible captures and promotions for unquiet positions.
      // Find the most valuable immediate capture.
      if (pos.isWhitesTurn()) {
        mostValuableExchange = Math.max(mostValuableExchange, getValueOfMostValuableRookCapture(whiteRookAttacks, pos.getBlackQueens(),
            null) - params.rookValue);
        mostValuableExchange = Math.max(mostValuableExchange, getValueOfMostValuableMinorPieceOrPawnCapture(whiteBishopAttacks,
            pos.getBlackQueens(), pos.getBlackRooks(), pos.getBlackBishops(), pos.getBlackKnights(), null) - params.bishopValue);
        mostValuableExchange = Math.max(mostValuableExchange, getValueOfMostValuableMinorPieceOrPawnCapture(whiteKnightAttacks,
            pos.getBlackQueens(), pos.getBlackRooks(), pos.getBlackBishops(), pos.getBlackKnights(), null) - params.knightValue);
        mostValuableExchange = Math.max(mostValuableExchange, getValueOfMostValuableMinorPieceOrPawnCapture(whitePawnAttacks,
            pos.getBlackQueens(), pos.getBlackRooks(), pos.getBlackBishops(), pos.getBlackKnights(), null) - params.pawnValue);
      } else {
        mostValuableExchange = Math.max(0, getValueOfMostValuableRookCapture(blackRookAttacks, pos.getWhiteQueens(),
            null) - params.rookValue);
        mostValuableExchange = Math.max(mostValuableExchange, getValueOfMostValuableMinorPieceOrPawnCapture(blackBishopAttacks,
            pos.getWhiteQueens(), pos.getWhiteRooks(), pos.getWhiteBishops(), pos.getWhiteKnights(), null) - params.bishopValue);
        mostValuableExchange = Math.max(mostValuableExchange, getValueOfMostValuableMinorPieceOrPawnCapture(blackKnightAttacks,
            pos.getWhiteQueens(), pos.getWhiteRooks(), pos.getWhiteBishops(), pos.getWhiteKnights(), null) - params.knightValue);
        mostValuableExchange = Math.max(mostValuableExchange, getValueOfMostValuableMinorPieceOrPawnCapture(blackPawnAttacks,
            pos.getWhiteQueens(), pos.getWhiteRooks(), pos.getWhiteBishops(), pos.getWhiteKnights(), null) - params.pawnValue);
      }
    } else {
      gradientCache.put("queenValue", (double) numOfQueensDiff);
      gradientCache.put("rookValue", (double) numOfRooksDiff);
      gradientCache.put("bishopValue", (double) numOfBishopsDiff);
      gradientCache.put("knightValue", (double) numOfKnightsDiff);
      gradientCache.put("pawnValue", (double) numOfPawnsDiff);
      gradientCache.put("bishopPairAdvantage", (double) bishopPairAdvantageDiff);
      gradientCache.put("stoppedPawnWeight", (double) numOfStoppedPawnsDiff);
      gradientCache.put("pinnedQueenWeight", (double) numOfPinnedQueensDiff);
      gradientCache.put("pinnedRookWeight", (double) numOfPinnedRooksDiff);
      gradientCache.put("pinnedBishopWeight", (double) numOfPinnedBishopsDiff);
      gradientCache.put("pinnedKnightWeight", (double) numOfPinnedKnightsDiff);
      gradientCache.put("pinnedPawnWeight", (double) numOfPinnedPawnsDiff);
      gradientCache.put("pawnShieldWeight1", (double) pawnShield1Diff);
      gradientCache.put("pawnShieldWeight2", (double) pawnShield2Diff);
      gradientCache.put("pawnStormWeight1", (double) pawnStorm1Diff);
      gradientCache.put("pawnStormWeight2", (double) pawnStorm2Diff);
      gradientCache.put("blockedPawnWeight1", (double) numOfBlockedPawnsDiff1);
      gradientCache.put("blockedPawnWeight2", (double) numOfBlockedPawnsDiff2);
      gradientCache.put("blockedPawnWeight3", (double) numOfBlockedPawnsDiff3);
      gradientCache.put("passedPawnWeight", (double) numOfPassedPawnsDiff);
      gradientCache.put("isolatedPawnWeight", (double) numOfIsolatedPawnsDiff);
      gradientCache.put("backwardPawnWeight", (double) numOfBackwardPawnsDiff);
      gradientCache.put("queenDefenseWeight", (double) numOfPawnDefendedQueensDiff);
      gradientCache.put("rookDefenseWeight", (double) numOfPawnDefendedRooksDiff);
      gradientCache.put("bishopDefenseWeight", (double) numOfPawnDefendedBishopsDiff);
      gradientCache.put("knightDefenseWeight", (double) numOfPawnDefendedKnightsDiff);
      gradientCache.put("pawnDefenseWeight", (double) numOfPawnDefendedPawnsDiff);
      gradientCache.put("queenMobilityWeight", (double) queenMobilityDiff);
      gradientCache.put("rookMobilityWeight", (double) rookMobilityDiff);
      gradientCache.put("bishopMobilityWeight", (double) bishopMobilityDiff);
      gradientCache.put("knightMobilityWeight", (double) knightMobilityDiff);
      gradientCache.put("pawnMobilityWeight", (double) pawnMobilityDiff);
      gradientCache.put("friendlyQueenTropismWeight", (double) friendlyQueenKingTropismDiff);
      gradientCache.put("friendlyRookTropismWeight", (double) friendlyRookKingTropismDiff);
      gradientCache.put("friendlyBishopTropismWeight", (double) friendlyBishopKingTropismDiff);
      gradientCache.put("friendlyKnightTropismWeight", (double) friendlyKnightKingTropismDiff);
      gradientCache.put("friendlyPassedPawnTropismWeight", (double) friendlyPassedPawnKingTropismDiff);
      gradientCache.put("friendlyWeakPawnTropismWeight", (double) friendlyWeakPawnKingTropismDiff);
      gradientCache.put("friendlyNormalPawnTropismWeight", (double) friendlyNormalPawnKingTropismDiff);
      gradientCache.put("opponentQueenTropismWeight", (double) opponentQueenKingTropismDiff);
      gradientCache.put("opponentRookTropismWeight", (double) opponentRookKingTropismDiff);
      gradientCache.put("opponentBishopTropismWeight", (double) opponentBishopKingTropismDiff);
      gradientCache.put("opponentKnightTropismWeight", (double) opponentKnightKingTropismDiff);
      gradientCache.put("opponentPassedPawnTropismWeight", (double) opponentPassedPawnKingTropismDiff);
      gradientCache.put("opponentWeakPawnTropismWeight", (double) opponentWeakPawnKingTropismDiff);
      gradientCache.put("opponentNormalPawnTropismWeight", (double) opponentNormalPawnKingTropismDiff);
      gradientCache.put("attackedKingZoneSquareWeight", (double) uniqueAttackedKingZoneSquaresDiff);
      gradientCache.put("kingZoneAttackerWeight", (double) numKingZoneAttackersDiff);
      // Calculate the derivative of the tapered evaluation w.r.t. the mid-game and end-game piece square table parameters.
      double dPstEgParam = (double) phaseScore / MAX_PHASE_SCORE;
      double dPstMgParam = 1d - dPstEgParam;
      for (int i = 0; i < 64; i++) {
        byte piece = (byte) (pos.getPiece(i) - 1);
        if (piece < Piece.NULL.ind) {
          continue;
        }
        String pstMgParamName = PST_MG_PARAM_NAMES[piece][i];
        String pstEgParamName = PST_EG_PARAM_NAMES[piece][i];
        Double cachedPstMgParamGrad = gradientCache.getOrDefault(pstMgParamName, 0d);
        Double cachedPstEgParamGrad = gradientCache.getOrDefault(pstEgParamName, 0d);
        if (piece < Piece.B_KING.ind) {
          gradientCache.put(pstMgParamName, cachedPstMgParamGrad + dPstMgParam);
          gradientCache.put(pstEgParamName, cachedPstEgParamGrad + dPstEgParam);
        } else {
          gradientCache.put(pstMgParamName, cachedPstMgParamGrad - dPstMgParam);
          gradientCache.put(pstEgParamName, cachedPstEgParamGrad - dPstEgParam);
        }
        mgScore += pstMg[piece][i];
        egScore += pstEg[piece][i];
      }
      // Calculate the immediate capture terms' effect on the gradient in case the function is called on a non-quiet position.
      double colorFactor;
      AtomicReference<String> rookVictimParamName = new AtomicReference<>();
      AtomicReference<String> bishopVictimParamName = new AtomicReference<>();
      AtomicReference<String> knightVictimParamName = new AtomicReference<>();
      AtomicReference<String> pawnVictimParamName = new AtomicReference<>();
      int mostValuableRookExchange, mostValuableKnightExchange, mostValuableBishopExchange, mostValuablePawnExchange;
      if (pos.isWhitesTurn()) {
        colorFactor = 1d;
        mostValuableRookExchange = getValueOfMostValuableRookCapture(whiteRookAttacks, pos.getBlackQueens(), rookVictimParamName) -
            params.rookValue;
        mostValuableBishopExchange = getValueOfMostValuableMinorPieceOrPawnCapture(whiteBishopAttacks, pos.getBlackQueens(),
            pos.getBlackRooks(), pos.getBlackBishops(), pos.getBlackKnights(), bishopVictimParamName) - params.bishopValue;
        mostValuableKnightExchange = getValueOfMostValuableMinorPieceOrPawnCapture(whiteKnightAttacks, pos.getBlackQueens(),
            pos.getBlackRooks(), pos.getBlackBishops(), pos.getBlackKnights(), knightVictimParamName) - params.knightValue;
        mostValuablePawnExchange = getValueOfMostValuableMinorPieceOrPawnCapture(whitePawnAttacks, pos.getBlackQueens(),
            pos.getBlackRooks(), pos.getBlackBishops(), pos.getBlackKnights(), pawnVictimParamName) - params.pawnValue;
      } else {
        colorFactor = -1d;
        mostValuableRookExchange = getValueOfMostValuableRookCapture(blackRookAttacks, pos.getWhiteQueens(), rookVictimParamName) -
            params.rookValue;
        mostValuableBishopExchange = getValueOfMostValuableMinorPieceOrPawnCapture(blackBishopAttacks, pos.getWhiteQueens(),
            pos.getWhiteRooks(), pos.getWhiteBishops(), pos.getWhiteKnights(), bishopVictimParamName) - params.bishopValue;
        mostValuableKnightExchange = getValueOfMostValuableMinorPieceOrPawnCapture(blackKnightAttacks, pos.getWhiteQueens(),
            pos.getWhiteRooks(), pos.getWhiteBishops(), pos.getWhiteKnights(), knightVictimParamName) - params.knightValue;
        mostValuablePawnExchange = getValueOfMostValuableMinorPieceOrPawnCapture(blackPawnAttacks, pos.getWhiteQueens(),
            pos.getWhiteRooks(), pos.getWhiteBishops(), pos.getWhiteKnights(), pawnVictimParamName) - params.pawnValue;
      }
      String captorParamName = null;
      String victimParamName = null;
      if (mostValuablePawnExchange > mostValuableExchange) {
        mostValuableExchange = mostValuablePawnExchange;
        captorParamName = "pawnValue";
        victimParamName = pawnVictimParamName.get();
      }
      if (mostValuableKnightExchange > mostValuableExchange) {
        mostValuableExchange = mostValuableKnightExchange;
        captorParamName = "knightValue";
        victimParamName = knightVictimParamName.get();
      }
      if (mostValuableBishopExchange > mostValuableExchange) {
        mostValuableExchange = mostValuableBishopExchange;
        captorParamName = "bishopValue";
        victimParamName = bishopVictimParamName.get();
      }
      if (mostValuableRookExchange > mostValuableExchange) {
        mostValuableExchange = mostValuableRookExchange;
        captorParamName = "rookValue";
        victimParamName = rookVictimParamName.get();
      }
      if (captorParamName != null && victimParamName != null) {
        Double captorGrad = gradientCache.getOrDefault(captorParamName, 0d);
        Double victimGrad = gradientCache.getOrDefault(victimParamName, 0d);
        gradientCache.put(captorParamName, captorGrad - colorFactor);
        gradientCache.put(victimParamName, victimGrad + colorFactor);
      }
      gradientCache.put("tempoAdvantage", colorFactor);
    }
    score += (short) taperedEvalScore(mgScore, egScore, phaseScore);
    // Adjust score to side to move.
    if (!pos.isWhitesTurn()) {
      score *= -1;
    }
    score += mostValuableExchange;
    // Tempo advantage.
    score += params.tempoAdvantage;
    if (evalTable != null) {
      entry.set(pos.getKey(), score, hashGen);
      entry.setupKey();
      evalTable.put(entry);
    }
    return score;
  }

  /**
   * A static evaluation of the chess position from the color to move's point of view. It considers material imbalance, coverage, pawn
   * structure, queen-king tropism, mobility, immediate captures, etc. It assumes that the position is not a check.
   *
   * @param pos The position to score.
   * @param hashGen The hash generation.
   * @param entry The pre-constructed evaluation table entry.
   * @return The score of the position.
   */
  public short score(Position pos, byte hashGen, ETEntry entry) {
    return score(pos, hashGen, entry, null);
  }

}
