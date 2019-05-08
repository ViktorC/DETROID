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

  /**
   * A symbolic, unattainable value.
   */
  private static final int KING_VALUE = 20000;

  private static final String MG_SUFFIX = "Mg";
  private static final String EG_SUFFIX = "Eg";

  // MVV/LVA piece values.
  private static final byte[] MVV_LVA_PIECE_VALUES = new byte[]{0, 6, 5, 4, 3, 2, 1, 6, 5, 4, 3, 2, 1};
  // The values of different captor-victim combinations for MVV/LVA move assessment.
  private static final byte[][] MVV_LVA;

  static {
    byte kingValue = MVV_LVA_PIECE_VALUES[Piece.W_KING.ind];
    MVV_LVA = new byte[13][13];
    List<Entry<Short, Integer>> combVals = new ArrayList<>();
    // Include illogical combinations without discrimination for the sake of better performance.
    for (Piece a : Piece.values()) {
      if (a != Piece.NULL) {
        for (Piece v : Piece.values()) {
          if (v != Piece.NULL) {
            short comb = (short) (((short) v.ind) | (((short) a.ind) << 7));
            int attackerVal = MVV_LVA_PIECE_VALUES[a.ind];
            int victimVal = MVV_LVA_PIECE_VALUES[v.ind];
            victimVal *= kingValue;
            combVals.add(new SimpleEntry<>(comb, victimVal - attackerVal));
          }
        }
      }
    }
    combVals.sort(Comparator.comparingInt(Entry<Short, Integer>::getValue));
    int lastVal = 0;
    byte assignedVal = 0;
    for (Entry<Short, Integer> entry : combVals) {
      if (entry.getValue() > lastVal) {
        assignedVal++;
      }
      lastVal = entry.getValue();
      int victim = entry.getKey() & 127;
      int attacker = entry.getKey() >>> 7;
      MVV_LVA[attacker][victim] = assignedVal;
    }
  }

  // Distance tables for tropism.
  private static final byte[][] MANHATTAN_DISTANCE = new byte[64][64];
  private static final byte[][] CHEBYSHEV_DISTANCE = new byte[64][64];

  static {
    for (int i = 0; i < 64; i++) {
      int r1 = Rank.getBySquareIndex(i).ind;
      int f1 = File.getBySquareIndex(i).ind;
      for (int j = 0; j < 64; j++) {
        int r2 = Rank.getBySquareIndex(j).ind;
        int f2 = File.getBySquareIndex(j).ind;
        int rankDist = Math.abs(r2 - r1);
        int fileDist = Math.abs(f2 - f1);
        MANHATTAN_DISTANCE[i][j] = (byte) (rankDist + fileDist);
        CHEBYSHEV_DISTANCE[i][j] = (byte) Math.max(rankDist, fileDist);
      }
    }
  }

  private static final byte[] MANHATTAN_DISTANCE_TO_CENTER = new byte[64];

  static {
    for (int i = 0; i < 64; i++) {
      byte distToCenterSquare1 = MANHATTAN_DISTANCE[i][Square.D4.ind];
      byte distToCenterSquare2 = MANHATTAN_DISTANCE[i][Square.D5.ind];
      byte distToCenterSquare3 = MANHATTAN_DISTANCE[i][Square.E4.ind];
      byte distToCenterSquare4 = MANHATTAN_DISTANCE[i][Square.E5.ind];
      int minDistance = Math.min(distToCenterSquare1, Math.min(distToCenterSquare2, Math.min(distToCenterSquare3, distToCenterSquare4)));
      MANHATTAN_DISTANCE_TO_CENTER[i] = (byte) minDistance;
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

  private static long SHORT_CASTLED_W_KING_LOC = Square.F1.bitboard | Square.G1.bitboard | Square.H1.bitboard;
  private static long SHORT_CASTLED_W_KING_PAWN_SHIELD1 = SHORT_CASTLED_W_KING_LOC << 8;
  private static long SHORT_CASTLED_W_KING_PAWN_SHIELD2 = SHORT_CASTLED_W_KING_PAWN_SHIELD1 << 8;

  private static long SHORT_CASTLED_B_KING_LOC = Square.F8.bitboard | Square.G8.bitboard | Square.H8.bitboard;
  private static long SHORT_CASTLED_B_KING_PAWN_SHIELD1 = SHORT_CASTLED_B_KING_LOC >>> 8;
  private static long SHORT_CASTLED_B_KING_PAWN_SHIELD2 = SHORT_CASTLED_B_KING_PAWN_SHIELD1 >>> 8;

  private static long LONG_CASTLED_W_KING_LOC = Square.A1.bitboard | Square.B1.bitboard | Square.C1.bitboard;
  private static long LONG_CASTLED_W_KING_PAWN_SHIELD1 = LONG_CASTLED_W_KING_LOC << 8;
  private static long LONG_CASTLED_W_KING_PAWN_SHIELD2 = LONG_CASTLED_W_KING_PAWN_SHIELD1 << 8;

  private static long LONG_CASTLED_B_KING_LOC = Square.A8.bitboard | Square.B8.bitboard | Square.C8.bitboard;
  private static long LONG_CASTLED_B_KING_PAWN_SHIELD1 = LONG_CASTLED_B_KING_LOC >>> 8;
  private static long LONG_CASTLED_B_KING_PAWN_SHIELD2 = LONG_CASTLED_B_KING_PAWN_SHIELD1 >>> 8;

  private final DetroidParameters params;
  // Evaluation score hash table.
  private final Cache<ETEntry> evalTable;

  private short[] pstWhiteKingMg;
  private short[] pstWhiteQueenMg;
  private short[] pstWhiteRookMg;
  private short[] pstWhiteBishopMg;
  private short[] pstWhiteKnightMg;
  private short[] pstWhitePawnMg;
  private short[] pstBlackKingMg;
  private short[] pstBlackQueenMg;
  private short[] pstBlackRookMg;
  private short[] pstBlackBishopMg;
  private short[] pstBlackKnightMg;
  private short[] pstBlackPawnMg;

  private short[] pstWhiteKingEg;
  private short[] pstWhiteQueenEg;
  private short[] pstWhiteRookEg;
  private short[] pstWhiteBishopEg;
  private short[] pstWhiteKnightEg;
  private short[] pstWhitePawnEg;
  private short[] pstBlackKingEg;
  private short[] pstBlackQueenEg;
  private short[] pstBlackRookEg;
  private short[] pstBlackBishopEg;
  private short[] pstBlackKnightEg;
  private short[] pstBlackPawnEg;

  // Tapered piece values for all possible game phases.
  private short[] queenValues;
  private short[] rookValues;
  private short[] bishopValues;
  private short[] knightValues;
  private short[] pawnValues;

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
    initPieceValues();
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
    pstWhitePawnMg = new short[64];
    pstWhitePawnEg = new short[64];
    pstWhiteKnightMg = new short[64];
    pstWhiteKnightEg = new short[64];
    pstWhiteBishopMg = new short[64];
    pstWhiteBishopEg = new short[64];
    pstWhiteRookMg = new short[64];
    pstWhiteRookEg = new short[64];
    pstWhiteQueenMg = new short[64];
    pstWhiteQueenEg = new short[64];
    pstWhiteKingMg = new short[64];
    pstWhiteKingEg = new short[64];
    pstBlackPawnMg = new short[64];
    pstBlackPawnEg = new short[64];
    pstBlackKnightMg = new short[64];
    pstBlackKnightEg = new short[64];
    pstBlackBishopMg = new short[64];
    pstBlackBishopEg = new short[64];
    pstBlackRookMg = new short[64];
    pstBlackRookEg = new short[64];
    pstBlackQueenMg = new short[64];
    pstBlackQueenEg = new short[64];
    pstBlackKingMg = new short[64];
    pstBlackKingEg = new short[64];
    /* Due to the reversed order of the rows in the definition of the white piece-square tables, they are just
     * right for black with negated values. */
    for (int i = 0; i < 64; i++) {
      pstBlackPawnMg[i] = pstPawnMg[i];
      pstBlackPawnEg[i] = pstPawnEg[i];
      pstBlackKnightMg[i] = pstKnightMg[i];
      pstBlackKnightEg[i] = pstKnightEg[i];
      pstBlackBishopMg[i] = pstBishopMg[i];
      pstBlackBishopEg[i] = pstBishopEg[i];
      pstBlackRookMg[i] = pstRookMg[i];
      pstBlackRookEg[i] = pstRookEg[i];
      pstBlackQueenMg[i] = pstQueenMg[i];
      pstBlackQueenEg[i] = pstQueenEg[i];
      pstBlackKingMg[i] = pstKingMg[i];
      pstBlackKingEg[i] = pstKingEg[i];
    }
    // To get the right values for the white piece-square tables, vertically mirror and negate the ones for black.
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        int c1 = i * 8 + j;
        int c2 = ((7 - i) * 8) + j;
        pstWhitePawnMg[c1] = pstBlackPawnMg[c2];
        pstWhitePawnEg[c1] = pstBlackPawnEg[c2];
        pstWhiteKnightMg[c1] = pstBlackKnightMg[c2];
        pstWhiteKnightEg[c1] = pstBlackKnightEg[c2];
        pstWhiteBishopMg[c1] = pstBlackBishopMg[c2];
        pstWhiteBishopEg[c1] = pstBlackBishopEg[c2];
        pstWhiteRookMg[c1] = pstBlackRookMg[c2];
        pstWhiteRookEg[c1] = pstBlackRookEg[c2];
        pstWhiteQueenMg[c1] = pstBlackQueenMg[c2];
        pstWhiteQueenEg[c1] = pstBlackQueenEg[c2];
        pstWhiteKingMg[c1] = pstBlackKingMg[c2];
        pstWhiteKingEg[c1] = pstBlackKingEg[c2];
      }
    }
  }

  private void initPieceValues() {
    int uniquePhaseScores = Position.MAX_PHASE_SCORE + 1;
    queenValues = new short[uniquePhaseScores];
    rookValues = new short[uniquePhaseScores];
    bishopValues = new short[uniquePhaseScores];
    knightValues = new short[uniquePhaseScores];
    pawnValues = new short[uniquePhaseScores];
    for (int phaseScore = 0; phaseScore < uniquePhaseScores; phaseScore++) {
      queenValues[phaseScore] = taperedEvalScore(params.queenValueMg, params.queenValueEg, phaseScore);
      rookValues[phaseScore] = taperedEvalScore(params.rookValueMg, params.rookValueEg, phaseScore);
      bishopValues[phaseScore] = taperedEvalScore(params.bishopValueMg, params.bishopValueEg, phaseScore);
      knightValues[phaseScore] = taperedEvalScore(params.knightValueMg, params.knightValueEg, phaseScore);
      pawnValues[phaseScore] = taperedEvalScore(params.pawnValueMg, params.pawnValueEg, phaseScore);
    }
  }

  private static short taperedEvalScore(int mgEval, int egEval, int phaseScore) {
    return (short) ((mgEval * (Position.MAX_PHASE_SCORE - phaseScore) + egEval * phaseScore) / Position.MAX_PHASE_SCORE);
  }

  private static short materialValueByPieceInd(short queenValue, short rookValue, short bishopValue, short knightValue, short pawnValue,
      int pieceInd) {
    if (pieceInd == Piece.W_KING.ind) {
      return KING_VALUE;
    } else if (pieceInd == Piece.W_QUEEN.ind) {
      return queenValue;
    } else if (pieceInd == Piece.W_ROOK.ind) {
      return rookValue;
    } else if (pieceInd == Piece.W_BISHOP.ind) {
      return bishopValue;
    } else if (pieceInd == Piece.W_KNIGHT.ind) {
      return knightValue;
    } else if (pieceInd == Piece.W_PAWN.ind) {
      return pawnValue;
    } else if (pieceInd == Piece.B_KING.ind) {
      return KING_VALUE;
    } else if (pieceInd == Piece.B_QUEEN.ind) {
      return queenValue;
    } else if (pieceInd == Piece.B_ROOK.ind) {
      return rookValue;
    } else if (pieceInd == Piece.B_BISHOP.ind) {
      return bishopValue;
    } else if (pieceInd == Piece.B_KNIGHT.ind) {
      return knightValue;
    } else if (pieceInd == Piece.B_PAWN.ind) {
      return pawnValue;
    } else if (pieceInd == Piece.NULL.ind) {
      return 0;
    } else {
      throw new IllegalArgumentException();
    }
  }

  private static int highestValueImmediateCapture(short queenValue, short rookValue, short bishopValue, short knightValue, short pawnValue,
      long pawnAttacks, long knightAttacks, long bishopAttacks, long rookAttacks, long opponentKnights, long opponentBishops,
      long opponentRooks, long opponentQueens, AtomicReference<String> victimParamNameRef, AtomicReference<String> captorParamNameRef) {
    String victimParamName = null;
    String captorParamName = null;
    int highestExchangeValue = 0;
    // Pawn attacked opponent pieces.
    if ((pawnAttacks & opponentQueens) != Bitboard.EMPTY_BOARD) {
      highestExchangeValue = queenValue - pawnValue;
      victimParamName = "queenValue";
      captorParamName = "pawnValue";
    } else if ((pawnAttacks & opponentRooks) != Bitboard.EMPTY_BOARD) {
      highestExchangeValue = rookValue - pawnValue;
      victimParamName = "rookValue";
      captorParamName = "pawnValue";
    } else if ((pawnAttacks & opponentBishops) != Bitboard.EMPTY_BOARD) {
      highestExchangeValue = bishopValue - pawnValue;
      victimParamName = "bishopValue";
      captorParamName = "pawnValue";
    } else if ((pawnAttacks & opponentKnights) != Bitboard.EMPTY_BOARD) {
      highestExchangeValue = knightValue - pawnValue;
      victimParamName = "knightValue";
      captorParamName = "pawnValue";
    }
    // Knight attacked opponent pieces.
    if ((knightAttacks & opponentQueens) != Bitboard.EMPTY_BOARD) {
      int exchangeValue = queenValue - knightValue;
      if (exchangeValue > highestExchangeValue) {
        highestExchangeValue = exchangeValue;
        victimParamName = "queenValue";
        captorParamName = "knightValue";
      }
    } else if ((knightAttacks & opponentRooks) != Bitboard.EMPTY_BOARD) {
      int exchangeValue = rookValue - knightValue;
      if (exchangeValue > highestExchangeValue) {
        highestExchangeValue = exchangeValue;
        victimParamName = "rookValue";
        captorParamName = "knightValue";
      }
    } else if ((knightAttacks & opponentBishops) != Bitboard.EMPTY_BOARD) {
      int exchangeValue = bishopValue - knightValue;
      if (exchangeValue > highestExchangeValue) {
        highestExchangeValue = exchangeValue;
        victimParamName = "bishopValue";
        captorParamName = "knightValue";
      }
    }
    // Bishop attacked opponent pieces.
    if ((bishopAttacks & opponentQueens) != Bitboard.EMPTY_BOARD) {
      int exchangeValue = queenValue - bishopValue;
      if (exchangeValue > highestExchangeValue) {
        highestExchangeValue = exchangeValue;
        victimParamName = "queenValue";
        captorParamName = "bishopValue";
      }
    } else if ((bishopAttacks & opponentRooks) != Bitboard.EMPTY_BOARD) {
      int exchangeValue = rookValue - bishopValue;
      if (exchangeValue > highestExchangeValue) {
        highestExchangeValue = exchangeValue;
        victimParamName = "rookValue";
        captorParamName = "bishopValue";
      }
    }
    // Rook attacked opponent pieces.
    if ((rookAttacks & opponentQueens) != Bitboard.EMPTY_BOARD) {
      int exchangeValue = queenValue - rookValue;
      if (exchangeValue > highestExchangeValue) {
        highestExchangeValue = exchangeValue;
        victimParamName = "queenValue";
        captorParamName = "rookValue";
      }
    }
    if (victimParamNameRef != null && captorParamNameRef != null) {
      victimParamNameRef.set(victimParamName);
      captorParamNameRef.set(captorParamName);
    }
    return highestExchangeValue;
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
    int numOfAllPieces = BitOperations.hammingWeight(pos.getAllOccupied());
    if (numOfAllPieces == 2 || numOfAllPieces == 3) {
      return true;
    }
    if (numOfAllPieces >= 4 && BitOperations.hammingWeight(pos.getWhiteKnights()) == 0 &&
        BitOperations.hammingWeight(pos.getBlackKnights()) == 0) {
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
      return allSameColor;
    }
    return false;
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
    final short phaseScore = pos.getPhaseScore();
    final short queenValue = queenValues[phaseScore];
    final short rookValue = rookValues[phaseScore];
    final short bishopValue = bishopValues[phaseScore];
    final short knightValue = knightValues[phaseScore];
    final short pawnValue = pawnValues[phaseScore];
    short victimVal = materialValueByPieceInd(queenValue, rookValue, bishopValue, knightValue, pawnValue, move.capturedPiece);
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
        gains[i] += queenValue - pawnValue;
        attackerVal = queenValue;
      } else if (move.type == MoveType.PROMOTION_TO_ROOK.ind) {
        gains[i] += rookValue - pawnValue;
        attackerVal = rookValue;
      } else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind) {
        gains[i] += bishopValue - pawnValue;
        attackerVal = bishopValue;
      } else { // Promotion to knight.
        gains[i] += knightValue - pawnValue;
        attackerVal = knightValue;
      }
    } else {
      attackerVal = materialValueByPieceInd(queenValue, rookValue, bishopValue, knightValue, pawnValue, move.movedPiece);
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
          attackerVal = pawnValue;
        } else if ((attackers = dB.getKnightMoveSet(pos.getWhiteKnights()) & occupied) != Bitboard.EMPTY_BOARD) {
          attackerVal = knightValue;
        } else if ((attackers = (bpAttack = dB.getBishopMoveSet(occupied, occupied)) & pos.getWhiteBishops()) !=
            Bitboard.EMPTY_BOARD) {
          attackerVal = bishopValue;
        } else if ((attackers = (rkAttack = dB.getRookMoveSet(occupied, occupied)) & pos.getWhiteRooks()) !=
            Bitboard.EMPTY_BOARD) {
          attackerVal = rookValue;
        } else if ((attackers = (bpAttack | rkAttack) & pos.getWhiteQueens()) != Bitboard.EMPTY_BOARD) {
          attackerVal = queenValue;
        } else if ((attackers = dB.getKingMoveSet(pos.getWhiteKing())) != Bitboard.EMPTY_BOARD) {
          attackerVal = KING_VALUE;
        } else {
          break;
        }
      } else {
        if ((attackers = dB.getWhitePawnCaptureSet(pos.getBlackPawns()) & occupied) != Bitboard.EMPTY_BOARD) {
          attackerVal = pawnValue;
        } else if ((attackers = dB.getKnightMoveSet(pos.getBlackKnights()) & occupied) != Bitboard.EMPTY_BOARD) {
          attackerVal = knightValue;
        } else if ((attackers = (bpAttack = dB.getBishopMoveSet(occupied, occupied)) & pos.getBlackBishops()) !=
            Bitboard.EMPTY_BOARD) {
          attackerVal = bishopValue;
        } else if ((attackers = (rkAttack = dB.getRookMoveSet(occupied, occupied)) & pos.getBlackRooks()) !=
            Bitboard.EMPTY_BOARD) {
          attackerVal = rookValue;
        } else if ((attackers = (bpAttack | rkAttack) & pos.getBlackQueens()) != Bitboard.EMPTY_BOARD) {
          attackerVal = queenValue;
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
    short mgScore = 0;
    short egScore = 0;
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
    if (isMaterialInsufficient(pos)) {
      return Score.INSUFFICIENT_MATERIAL.value;
    }
    // Phase score for tapered evaluation.
    final short phaseScore = pos.getPhaseScore();
    final short queenValue = queenValues[phaseScore];
    final short rookValue = rookValues[phaseScore];
    final short bishopValue = bishopValues[phaseScore];
    final short knightValue = knightValues[phaseScore];
    final short pawnValue = pawnValues[phaseScore];
    byte numOfWhitePawns = BitOperations.hammingWeight(pos.getWhitePawns());
    byte numOfBlackPawns = BitOperations.hammingWeight(pos.getBlackPawns());
    // Base material score.
    int numOfQueensDiff = numOfWhiteQueens - numOfBlackQueens;
    int numOfRooksDiff = numOfWhiteRooks - numOfBlackRooks;
    int numOfBishopsDiff = numOfWhiteBishops - numOfBlackBishops;
    int numOfKnightsDiff = numOfWhiteKnights - numOfBlackKnights;
    int numOfPawnsDiff = numOfWhitePawns - numOfBlackPawns;
    score += queenValue * numOfQueensDiff;
    score += rookValue * numOfRooksDiff;
    score += bishopValue * numOfBishopsDiff;
    score += knightValue * numOfKnightsDiff;
    score += pawnValue * numOfPawnsDiff;
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
    mgScore += params.bishopPairAdvantageMg * bishopPairAdvantageDiff;
    egScore += params.bishopPairAdvantageEg * bishopPairAdvantageDiff;
    // Stopped pawns.
    int numOfStoppedPawnsDiff = BitOperations.hammingWeight(Bitboard.computeBlackPawnAdvanceSets(pos.getBlackPawns(),
        Bitboard.FULL_BOARD) & (pos.getAllWhiteOccupied() ^ pos.getWhitePawns())) -
        BitOperations.hammingWeight(Bitboard.computeWhitePawnAdvanceSets(pos.getWhitePawns(),
            Bitboard.FULL_BOARD) & (pos.getAllBlackOccupied() ^ pos.getBlackPawns()));
    mgScore += params.stoppedPawnWeightMg * numOfStoppedPawnsDiff;
    egScore += params.stoppedPawnWeightEg * numOfStoppedPawnsDiff;
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
    // Pawn shield and pawn storm.
    long whitePawns = pos.getWhitePawns();
    long blackPawns = pos.getBlackPawns();
    int pawnShield1Diff = 0;
    int pawnShield2Diff = 0;
    if ((whiteKing & SHORT_CASTLED_W_KING_LOC) != Bitboard.EMPTY_BOARD) {
      pawnShield1Diff += BitOperations.hammingWeight(whitePawns & SHORT_CASTLED_W_KING_PAWN_SHIELD1);
      pawnShield2Diff += BitOperations.hammingWeight(whitePawns & SHORT_CASTLED_W_KING_PAWN_SHIELD2);
    } else if ((whiteKing & LONG_CASTLED_W_KING_LOC) != Bitboard.EMPTY_BOARD) {
      pawnShield1Diff += BitOperations.hammingWeight(whitePawns & LONG_CASTLED_W_KING_PAWN_SHIELD1);
      pawnShield2Diff += BitOperations.hammingWeight(whitePawns & LONG_CASTLED_W_KING_PAWN_SHIELD2);
    }
    if ((blackKing & SHORT_CASTLED_B_KING_LOC) != Bitboard.EMPTY_BOARD) {
      pawnShield1Diff -= BitOperations.hammingWeight(blackPawns & SHORT_CASTLED_B_KING_PAWN_SHIELD1);
      pawnShield2Diff -= BitOperations.hammingWeight(blackPawns & SHORT_CASTLED_B_KING_PAWN_SHIELD2);
    } else if ((blackKing & LONG_CASTLED_B_KING_LOC) != Bitboard.EMPTY_BOARD) {
      pawnShield1Diff -= BitOperations.hammingWeight(blackPawns & LONG_CASTLED_B_KING_PAWN_SHIELD1);
      pawnShield2Diff -= BitOperations.hammingWeight(blackPawns & LONG_CASTLED_B_KING_PAWN_SHIELD2);
    }
    mgScore += params.pawnShieldWeight1Mg * pawnShield1Diff;
    egScore += params.pawnShieldWeight1Eg * pawnShield1Diff;
    mgScore += params.pawnShieldWeight2Mg * pawnShield2Diff;
    egScore += params.pawnShieldWeight2Eg * pawnShield2Diff;
    // Blocked pawns.
    int numOfBlockedPawnsDiff = (BitOperations.hammingWeight((blackPawns >>> 8) & blackPawns) -
        BitOperations.hammingWeight((whitePawns << 8) & whitePawns));
    mgScore += params.blockedPawnWeightMg * numOfBlockedPawnsDiff;
    egScore += params.blockedPawnWeightEg * numOfBlockedPawnsDiff;
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
    mgScore += params.passedPawnWeightMg * numOfPassedPawnsDiff;
    egScore += params.passedPawnWeightEg * numOfPassedPawnsDiff;
    // Isolated pawns.
    long whiteSideSpans = Bitboard.fillSouth(whiteAttackSpans);
    long blackSideSpans = Bitboard.fillNorth(blackAttackSpans);
    long whiteIsolatedPawns = whitePawns & ~whiteSideSpans;
    long blackIsolatedPawns = blackPawns & ~blackSideSpans;
    int numOfIsolatedPawnsDiff = (BitOperations.hammingWeight(blackIsolatedPawns) - BitOperations.hammingWeight(whiteIsolatedPawns));
    mgScore += params.isolatedPawnWeightMg * numOfIsolatedPawnsDiff;
    egScore += params.isolatedPawnWeightEg * numOfIsolatedPawnsDiff;
    // Backward pawns.
    long whitePawnCaptures = Bitboard.computeWhitePawnCaptureSets(pos.getWhitePawns() & ~whitePinnedPieces, Bitboard.FULL_BOARD);
    long blackPawnCaptures = Bitboard.computeBlackPawnCaptureSets(pos.getBlackPawns() & ~blackPinnedPieces, Bitboard.FULL_BOARD);
    long whiteBackwardPawns = whitePawns & ((blackPawnCaptures & ~whiteAttackSpans) >>> 8);
    long blackBackwardPawns = blackPawns & ((whitePawnCaptures & ~blackAttackSpans) << 8);
    int numOfBackwardPawnsDiff = (BitOperations.hammingWeight(blackBackwardPawns) - BitOperations.hammingWeight(whiteBackwardPawns));
    mgScore += params.backwardPawnWeightMg * numOfBackwardPawnsDiff;
    egScore += params.backwardPawnWeightEg * numOfBackwardPawnsDiff;
    long whiteWeakPawns = whiteIsolatedPawns | whiteBackwardPawns;
    long blackWeakPawns = blackIsolatedPawns | blackBackwardPawns;
    // Iterate over pieces to assess their mobility and distance from the opponent's king.
    int numKingZoneAttackersDiff = 0;
    long whitePieceSet = pos.getAllWhiteOccupied() ^ whiteKing;
    long blackPieceSet = pos.getAllBlackOccupied() ^ blackKing;
    long baseWhiteKingZone = Bitboard.computeKingMoveSets(whiteKing, Bitboard.FULL_BOARD);
    long baseBlackKingZone = Bitboard.computeKingMoveSets(blackKing, Bitboard.FULL_BOARD);
    long whiteKingZone = baseWhiteKingZone | (baseWhiteKingZone << 8);
    long blackKingZone = baseBlackKingZone | (baseBlackKingZone >>> 8);
    long attackedWhiteKingZoneSquares = Bitboard.EMPTY_BOARD;
    long attackedBlackKingZoneSquares = Bitboard.EMPTY_BOARD;
    long whitePieceAttacksAndDefense = Bitboard.EMPTY_BOARD;
    long whitePawnAttacks = Bitboard.EMPTY_BOARD;
    long whiteKnightAttacks = Bitboard.EMPTY_BOARD;
    long whiteBishopAttacks = Bitboard.EMPTY_BOARD;
    long whiteRookAttacks = Bitboard.EMPTY_BOARD;
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
    int whiteQueenMobility = 0;
    int whiteRookMobility = 0;
    int whiteBishopMobility = 0;
    int whiteKnightMobility = 0;
    int whitePawnMobility = 0;
    byte[] whiteKingChebyshevDistances = CHEBYSHEV_DISTANCE[whiteKingInd];
    byte[] whiteKingManhattanDistances = MANHATTAN_DISTANCE[whiteKingInd];
    byte[] blackKingChebyshevDistances = CHEBYSHEV_DISTANCE[blackKingInd];
    byte[] blackKingManhattanDistances = MANHATTAN_DISTANCE[blackKingInd];
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
      long pseudoLegalMoveSet;
      if (pieceType == Piece.W_PAWN.ind) {
        pseudoLegalMoveSet = moveSetDb.getWhitePawnMoveSet(pos.getAllBlackOccupied(), pos.getAllEmpty());
        long moveSet = pseudoLegalMoveSet & pinnedPieceMoveSetRestriction;
        whitePawnAttacks |= moveSet;
        whitePawnMobility += BitOperations.hammingWeight(moveSet);
        if ((whitePassedPawns & piece) != Bitboard.EMPTY_BOARD) {
          whitePassedPawnWhiteKingTropism += whiteKingManhattanDistances[pieceInd];
          whitePassedPawnBlackKingTropism += blackKingManhattanDistances[pieceInd];
        } else if ((whiteWeakPawns & piece) != Bitboard.EMPTY_BOARD) {
          whiteWeakPawnWhiteKingTropism += whiteKingManhattanDistances[pieceInd];
          whiteWeakPawnBlackKingTropism += blackKingManhattanDistances[pieceInd];
        } else {
          whiteNormalPawnWhiteKingTropism += whiteKingManhattanDistances[pieceInd];
          whiteNormalPawnBlackKingTropism += blackKingManhattanDistances[pieceInd];
        }
        mgScore += pstWhitePawnMg[pieceInd];
        egScore += pstWhitePawnEg[pieceInd];
      } else {
        long unrestrictedMoveSet;
        if (pieceType == Piece.W_KNIGHT.ind) {
          unrestrictedMoveSet = moveSetDb.getKnightMoveSet(Bitboard.FULL_BOARD);
          pseudoLegalMoveSet = unrestrictedMoveSet & pos.getAllNonWhiteOccupied();
          long moveSet = pseudoLegalMoveSet & pinnedPieceMoveSetRestriction;
          whiteKnightAttacks |= moveSet;
          whiteKnightMobility += BitOperations.hammingWeight(moveSet);
          whiteKnightWhiteKingTropism += whiteKingChebyshevDistances[pieceInd];
          whiteKnightBlackKingTropism += blackKingChebyshevDistances[pieceInd];
          mgScore += pstWhiteKnightMg[pieceInd];
          egScore += pstWhiteKnightEg[pieceInd];
        } else if (pieceType == Piece.W_BISHOP.ind) {
          unrestrictedMoveSet = moveSetDb.getBishopMoveSet(Bitboard.FULL_BOARD, pos.getAllOccupied());
          pseudoLegalMoveSet = unrestrictedMoveSet & pos.getAllNonWhiteOccupied();
          long moveSet = pseudoLegalMoveSet & pinnedPieceMoveSetRestriction;
          whiteBishopAttacks |= moveSet;
          whiteBishopMobility += BitOperations.hammingWeight(moveSet);
          whiteBishopWhiteKingTropism += whiteKingChebyshevDistances[pieceInd];
          whiteBishopBlackKingTropism += blackKingChebyshevDistances[pieceInd];
          mgScore += pstWhiteBishopMg[pieceInd];
          egScore += pstWhiteBishopEg[pieceInd];
        } else if (pieceType == Piece.W_ROOK.ind) {
          unrestrictedMoveSet = moveSetDb.getRookMoveSet(Bitboard.FULL_BOARD, pos.getAllOccupied());
          pseudoLegalMoveSet = unrestrictedMoveSet & pos.getAllNonWhiteOccupied();
          long moveSet = pseudoLegalMoveSet & pinnedPieceMoveSetRestriction;
          whiteRookAttacks |= moveSet;
          whiteRookMobility += BitOperations.hammingWeight(moveSet);
          whiteRookWhiteKingTropism += whiteKingChebyshevDistances[pieceInd];
          whiteRookBlackKingTropism += blackKingChebyshevDistances[pieceInd];
          mgScore += pstWhiteRookMg[pieceInd];
          egScore += pstWhiteRookEg[pieceInd];
        } else { // White queen.
          unrestrictedMoveSet = moveSetDb.getQueenMoveSet(Bitboard.FULL_BOARD, pos.getAllOccupied());
          pseudoLegalMoveSet = unrestrictedMoveSet & pos.getAllNonWhiteOccupied();
          long moveSet = pseudoLegalMoveSet & pinnedPieceMoveSetRestriction;
          whiteQueenMobility += BitOperations.hammingWeight(moveSet);
          whiteQueenWhiteKingTropism += whiteKingChebyshevDistances[pieceInd];
          whiteQueenBlackKingTropism += blackKingChebyshevDistances[pieceInd];
          mgScore += pstWhiteQueenMg[pieceInd];
          egScore += pstWhiteQueenEg[pieceInd];
        }
        whitePieceAttacksAndDefense |= (unrestrictedMoveSet & pinnedPieceMoveSetRestriction);
      }
      long attackedKingZoneSquares = pseudoLegalMoveSet & blackKingZone;
      if (attackedKingZoneSquares != Bitboard.EMPTY_BOARD) {
        numKingZoneAttackersDiff++;
        attackedWhiteKingZoneSquares |= attackedKingZoneSquares;
      }
      whitePieceSet = BitOperations.resetLSBit(whitePieceSet);
    }
    long blackPieceAttacksAndDefense = Bitboard.EMPTY_BOARD;
    long blackPawnAttacks = Bitboard.EMPTY_BOARD;
    long blackKnightAttacks = Bitboard.EMPTY_BOARD;
    long blackBishopAttacks = Bitboard.EMPTY_BOARD;
    long blackRookAttacks = Bitboard.EMPTY_BOARD;
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
      long pseudoLegalMoveSet;
      if (pieceType == Piece.B_PAWN.ind) {
        pseudoLegalMoveSet = moveSetDb.getBlackPawnMoveSet(pos.getAllWhiteOccupied(), pos.getAllEmpty());
        long moveSet = pseudoLegalMoveSet & pinnedPieceMoveSetRestriction;
        blackPawnAttacks |= moveSet;
        blackPawnMobility += BitOperations.hammingWeight(moveSet);
        if ((blackPassedPawns & piece) != Bitboard.EMPTY_BOARD) {
          blackPassedPawnBlackKingTropism += blackKingManhattanDistances[pieceInd];
          blackPassedPawnWhiteKingTropism += whiteKingManhattanDistances[pieceInd];
        } else if ((blackWeakPawns & piece) != Bitboard.EMPTY_BOARD) {
          blackWeakPawnBlackKingTropism += blackKingManhattanDistances[pieceInd];
          blackWeakPawnWhiteKingTropism += whiteKingManhattanDistances[pieceInd];
        } else {
          blackNormalPawnBlackKingTropism += blackKingManhattanDistances[pieceInd];
          blackNormalPawnWhiteKingTropism += whiteKingManhattanDistances[pieceInd];
        }
        mgScore -= pstBlackPawnMg[pieceInd];
        egScore -= pstBlackPawnEg[pieceInd];
      } else {
        long unrestrictedMoveSet;
        if (pieceType == Piece.B_KNIGHT.ind) {
          unrestrictedMoveSet = moveSetDb.getKnightMoveSet(Bitboard.FULL_BOARD);
          pseudoLegalMoveSet = unrestrictedMoveSet & pos.getAllNonBlackOccupied();
          long moveSet = pseudoLegalMoveSet & pinnedPieceMoveSetRestriction;
          blackKnightAttacks |= moveSet;
          blackKnightMobility += BitOperations.hammingWeight(moveSet);
          blackKnightBlackKingTropism += blackKingChebyshevDistances[pieceInd];
          blackKnightWhiteKingTropism += whiteKingChebyshevDistances[pieceInd];
          mgScore -= pstBlackKnightMg[pieceInd];
          egScore -= pstBlackKnightEg[pieceInd];
        } else if (pieceType == Piece.B_BISHOP.ind) {
          unrestrictedMoveSet = moveSetDb.getBishopMoveSet(Bitboard.FULL_BOARD, pos.getAllOccupied());
          pseudoLegalMoveSet = unrestrictedMoveSet & pos.getAllNonBlackOccupied();
          long moveSet = pseudoLegalMoveSet & pinnedPieceMoveSetRestriction;
          blackBishopAttacks |= moveSet;
          blackBishopMobility += BitOperations.hammingWeight(moveSet);
          blackBishopBlackKingTropism += blackKingChebyshevDistances[pieceInd];
          blackBishopWhiteKingTropism += whiteKingChebyshevDistances[pieceInd];
          mgScore -= pstBlackBishopMg[pieceInd];
          egScore -= pstBlackBishopEg[pieceInd];
        } else if (pieceType == Piece.B_ROOK.ind) {
          unrestrictedMoveSet = moveSetDb.getRookMoveSet(Bitboard.FULL_BOARD, pos.getAllOccupied());
          pseudoLegalMoveSet = unrestrictedMoveSet & pos.getAllNonBlackOccupied();
          long moveSet = pseudoLegalMoveSet & pinnedPieceMoveSetRestriction;
          blackRookAttacks |= moveSet;
          blackRookMobility += BitOperations.hammingWeight(moveSet);
          blackRookBlackKingTropism += blackKingChebyshevDistances[pieceInd];
          blackRookWhiteKingTropism += whiteKingChebyshevDistances[pieceInd];
          mgScore -= pstBlackRookMg[pieceInd];
          egScore -= pstBlackRookEg[pieceInd];
        } else { // Black queen.
          unrestrictedMoveSet = moveSetDb.getQueenMoveSet(Bitboard.FULL_BOARD, pos.getAllOccupied());
          pseudoLegalMoveSet = unrestrictedMoveSet & pos.getAllNonBlackOccupied();
          long moveSet = pseudoLegalMoveSet & pinnedPieceMoveSetRestriction;
          blackQueenMobility += BitOperations.hammingWeight(moveSet);
          blackQueenBlackKingTropism += blackKingChebyshevDistances[pieceInd];
          blackQueenWhiteKingTropism += whiteKingChebyshevDistances[pieceInd];
          mgScore -= pstBlackQueenMg[pieceInd];
          egScore -= pstBlackQueenEg[pieceInd];
        }
        blackPieceAttacksAndDefense |= (unrestrictedMoveSet & pinnedPieceMoveSetRestriction);
      }
      long attackedKingZoneSquares = pseudoLegalMoveSet & whiteKingZone;
      if (attackedKingZoneSquares != Bitboard.EMPTY_BOARD) {
        numKingZoneAttackersDiff--;
        attackedBlackKingZoneSquares |= attackedKingZoneSquares;
      }
      blackPieceSet = BitOperations.resetLSBit(blackPieceSet);
    }
    // King piece-square scores.
    mgScore += pstWhiteKingMg[whiteKingInd];
    egScore += pstWhiteKingEg[whiteKingInd];
    mgScore -= pstBlackKingMg[blackKingInd];
    egScore -= pstBlackKingEg[blackKingInd];
    // Mobility scores.
    int queenMobilityDiff = whiteQueenMobility - blackQueenMobility;
    int rookMobilityDiff = whiteRookMobility - blackRookMobility;
    int bishopMobilityDiff = whiteBishopMobility - blackBishopMobility;
    int knightMobilityDiff = whiteKnightMobility - blackKnightMobility;
    int pawnMobilityDiff = whitePawnMobility - blackPawnMobility;
    mgScore += params.queenMobilityWeightMg * queenMobilityDiff;
    egScore += params.queenMobilityWeightEg * queenMobilityDiff;
    mgScore += params.rookMobilityWeightMg * rookMobilityDiff;
    egScore += params.rookMobilityWeightEg * rookMobilityDiff;
    mgScore += params.bishopMobilityWeightMg * bishopMobilityDiff;
    egScore += params.bishopMobilityWeightEg * bishopMobilityDiff;
    mgScore += params.knightMobilityWeightMg * knightMobilityDiff;
    egScore += params.knightMobilityWeightEg * knightMobilityDiff;
    mgScore += params.pawnMobilityWeightMg * pawnMobilityDiff;
    egScore += params.pawnMobilityWeightEg * pawnMobilityDiff;
    // Piece defense.
    int numOfPieceDefendedQueensDiff = BitOperations.hammingWeight(pos.getWhiteQueens() & whitePieceAttacksAndDefense) -
        BitOperations.hammingWeight(pos.getBlackQueens() & blackPieceAttacksAndDefense);
    int numOfPieceDefendedRooksDiff = BitOperations.hammingWeight(pos.getWhiteRooks() & whitePieceAttacksAndDefense) -
        BitOperations.hammingWeight(pos.getBlackRooks() & blackPieceAttacksAndDefense);
    int numOfPieceDefendedBishopsDiff = BitOperations.hammingWeight(pos.getWhiteBishops() & whitePieceAttacksAndDefense) -
        BitOperations.hammingWeight(pos.getBlackBishops() & blackPieceAttacksAndDefense);
    int numOfPieceDefendedKnightsDiff = BitOperations.hammingWeight(pos.getWhiteKnights() & whitePieceAttacksAndDefense) -
        BitOperations.hammingWeight(pos.getBlackKnights() & blackPieceAttacksAndDefense);
    int numOfPieceDefendedPawnsDiff = BitOperations.hammingWeight(pos.getWhitePawns() & whitePieceAttacksAndDefense) -
        BitOperations.hammingWeight(pos.getBlackPawns() & blackPieceAttacksAndDefense);
    mgScore += params.pieceDefendedQueenWeightMg * numOfPieceDefendedQueensDiff;
    egScore += params.pieceDefendedQueenWeightEg * numOfPieceDefendedQueensDiff;
    mgScore += params.pieceDefendedRookWeightMg * numOfPieceDefendedRooksDiff;
    egScore += params.pieceDefendedRookWeightEg * numOfPieceDefendedRooksDiff;
    mgScore += params.pieceDefendedBishopWeightMg * numOfPieceDefendedBishopsDiff;
    egScore += params.pieceDefendedBishopWeightEg * numOfPieceDefendedBishopsDiff;
    mgScore += params.pieceDefendedKnightWeightMg * numOfPieceDefendedKnightsDiff;
    egScore += params.pieceDefendedKnightWeightEg * numOfPieceDefendedKnightsDiff;
    mgScore += params.pieceDefendedPawnWeightMg * numOfPieceDefendedPawnsDiff;
    egScore += params.pieceDefendedPawnWeightEg * numOfPieceDefendedPawnsDiff;
    // Pawn defense.
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
    mgScore += params.pawnDefendedQueenWeightMg * numOfPawnDefendedQueensDiff;
    egScore += params.pawnDefendedQueenWeightEg * numOfPawnDefendedQueensDiff;
    mgScore += params.pawnDefendedRookWeightMg * numOfPawnDefendedRooksDiff;
    egScore += params.pawnDefendedRookWeightEg * numOfPawnDefendedRooksDiff;
    mgScore += params.pawnDefendedBishopWeightMg * numOfPawnDefendedBishopsDiff;
    egScore += params.pawnDefendedBishopWeightEg * numOfPawnDefendedBishopsDiff;
    mgScore += params.pawnDefendedKnightWeightMg * numOfPawnDefendedKnightsDiff;
    egScore += params.pawnDefendedKnightWeightEg * numOfPawnDefendedKnightsDiff;
    mgScore += params.pawnDefendedPawnWeightMg * numOfPawnDefendedPawnsDiff;
    egScore += params.pawnDefendedPawnWeightEg * numOfPawnDefendedPawnsDiff;
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
    mgScore += params.friendlyQueenTropismWeightMg * friendlyQueenKingTropismDiff;
    egScore += params.friendlyQueenTropismWeightEg * friendlyQueenKingTropismDiff;
    mgScore += params.friendlyRookTropismWeightMg * friendlyRookKingTropismDiff;
    egScore += params.friendlyRookTropismWeightEg * friendlyRookKingTropismDiff;
    mgScore += params.friendlyBishopTropismWeightMg * friendlyBishopKingTropismDiff;
    egScore += params.friendlyBishopTropismWeightEg * friendlyBishopKingTropismDiff;
    mgScore += params.friendlyKnightTropismWeightMg * friendlyKnightKingTropismDiff;
    egScore += params.friendlyKnightTropismWeightEg * friendlyKnightKingTropismDiff;
    mgScore += params.friendlyPassedPawnTropismWeightMg * friendlyPassedPawnKingTropismDiff;
    egScore += params.friendlyPassedPawnTropismWeightEg * friendlyPassedPawnKingTropismDiff;
    mgScore += params.friendlyWeakPawnTropismWeightMg * friendlyWeakPawnKingTropismDiff;
    egScore += params.friendlyWeakPawnTropismWeightEg * friendlyWeakPawnKingTropismDiff;
    mgScore += params.friendlyNormalPawnTropismWeightMg * friendlyNormalPawnKingTropismDiff;
    egScore += params.friendlyNormalPawnTropismWeightEg * friendlyNormalPawnKingTropismDiff;
    mgScore += params.opponentQueenTropismWeightMg * opponentQueenKingTropismDiff;
    egScore += params.opponentQueenTropismWeightEg * opponentQueenKingTropismDiff;
    mgScore += params.opponentRookTropismWeightMg * opponentRookKingTropismDiff;
    egScore += params.opponentRookTropismWeightEg * opponentRookKingTropismDiff;
    mgScore += params.opponentBishopTropismWeightMg * opponentBishopKingTropismDiff;
    egScore += params.opponentBishopTropismWeightEg * opponentBishopKingTropismDiff;
    mgScore += params.opponentKnightTropismWeightMg * opponentKnightKingTropismDiff;
    egScore += params.opponentKnightTropismWeightEg * opponentKnightKingTropismDiff;
    mgScore += params.opponentPassedPawnTropismWeightMg * opponentPassedPawnKingTropismDiff;
    egScore += params.opponentPassedPawnTropismWeightEg * opponentPassedPawnKingTropismDiff;
    mgScore += params.opponentWeakPawnTropismWeightMg * opponentWeakPawnKingTropismDiff;
    egScore += params.opponentWeakPawnTropismWeightEg * opponentWeakPawnKingTropismDiff;
    mgScore += params.opponentNormalPawnTropismWeightMg * opponentNormalPawnKingTropismDiff;
    egScore += params.opponentNormalPawnTropismWeightEg * opponentNormalPawnKingTropismDiff;
    // King zone attacks.
    int uniqueAttackedKingZoneSquaresDiff = BitOperations.hammingWeight(attackedBlackKingZoneSquares) -
        BitOperations.hammingWeight(attackedWhiteKingZoneSquares);
    mgScore += params.attackedKingZoneSquareWeightMg * uniqueAttackedKingZoneSquaresDiff;
    egScore += params.attackedKingZoneSquareWeightEg * uniqueAttackedKingZoneSquaresDiff;
    mgScore += params.kingZoneAttackerWeightMg * numKingZoneAttackersDiff;
    egScore += params.kingZoneAttackerWeightEg * numKingZoneAttackersDiff;
    // Asymmetric evaluation terms.
    int highestExchangeValue = 0;
    AtomicReference<String> victimParamNameRef;
    AtomicReference<String> captorParamNameRef;
    if (gradientCache != null) {
      victimParamNameRef = new AtomicReference<>();
      captorParamNameRef = new AtomicReference<>();
    } else {
      victimParamNameRef = null;
      captorParamNameRef = null;
    }
    // The most valuable immediate capture.
    double colorFactor;
    if (pos.isWhitesTurn()) {
      colorFactor = 1d;
      if (((whitePawnCaptures | whitePieceAttacksAndDefense) & pos.getAllBlackOccupied()) != Bitboard.EMPTY_BOARD) {
        highestExchangeValue = highestValueImmediateCapture(queenValue, rookValue, bishopValue, knightValue, pawnValue, whitePawnAttacks,
            whiteKnightAttacks, whiteBishopAttacks, whiteRookAttacks, pos.getBlackKnights(), pos.getBlackBishops(), pos.getBlackRooks(),
            pos.getBlackQueens(), victimParamNameRef, captorParamNameRef);
      }
    } else {
      colorFactor = -1d;
      if (((blackPawnCaptures | blackPieceAttacksAndDefense) & pos.getAllWhiteOccupied()) != Bitboard.EMPTY_BOARD) {
        highestExchangeValue = highestValueImmediateCapture(queenValue, rookValue, bishopValue, knightValue, pawnValue, blackPawnAttacks,
            blackKnightAttacks, blackBishopAttacks, blackRookAttacks, pos.getWhiteKnights(), pos.getWhiteBishops(), pos.getWhiteRooks(),
            pos.getWhiteQueens(), victimParamNameRef, captorParamNameRef);
      }
    }
    mgScore += params.tempoAdvantageMg * colorFactor;
    egScore += params.tempoAdvantageEg * colorFactor;
    score += taperedEvalScore(mgScore, egScore, phaseScore);
    score += highestExchangeValue * colorFactor;
    // Mop-up evaluation for KRK and KQK end-games.
    int numOfPieces = numOfWhitePawns + numOfBlackPawns + numOfWhiteKnights + numOfBlackKnights + numOfWhiteBishops + numOfBlackBishops +
        numOfWhiteRooks + numOfBlackRooks + numOfWhiteQueens + numOfBlackQueens;
    int weakKingCenterTropism = 0;
    if (numOfPieces == 1) {
      if (numOfWhiteRooks == 1 || numOfWhiteQueens == 1) {
        weakKingCenterTropism = MANHATTAN_DISTANCE_TO_CENTER[blackKingInd];
      } else if (numOfBlackRooks == 1 || numOfBlackQueens == 1) {
        weakKingCenterTropism = -MANHATTAN_DISTANCE_TO_CENTER[whiteKingInd];
      }
    }
    score += params.mopUpCenterTropismWeight * weakKingCenterTropism;
    // Adjust the score based on the color to move.
    score *= colorFactor;
    if (evalTable != null) {
      entry.set(pos.getKey(), score, hashGen);
      entry.setupKey();
      evalTable.put(entry);
    }
    if (gradientCache != null) {
      // Calculate the derivative of the tapered evaluation w.r.t. the mid-game and end-game scores.
      double dPstEgParam = (double) phaseScore / Position.MAX_PHASE_SCORE;
      double dPstMgParam = 1d - dPstEgParam;
      gradientCache.put("queenValueMg", dPstMgParam * numOfQueensDiff);
      gradientCache.put("queenValueEg", dPstEgParam * numOfQueensDiff);
      gradientCache.put("rookValueMg", dPstMgParam * numOfRooksDiff);
      gradientCache.put("rookValueEg", dPstEgParam * numOfRooksDiff);
      gradientCache.put("bishopValueMg", dPstMgParam * numOfBishopsDiff);
      gradientCache.put("bishopValueEg", dPstEgParam * numOfBishopsDiff);
      gradientCache.put("knightValueMg", dPstMgParam * numOfKnightsDiff);
      gradientCache.put("knightValueEg", dPstEgParam * numOfKnightsDiff);
      gradientCache.put("pawnValueMg", dPstMgParam * numOfPawnsDiff);
      gradientCache.put("pawnValueEg", dPstEgParam * numOfPawnsDiff);
      gradientCache.put("bishopPairAdvantageMg", dPstMgParam * bishopPairAdvantageDiff);
      gradientCache.put("bishopPairAdvantageEg", dPstEgParam * bishopPairAdvantageDiff);
      gradientCache.put("stoppedPawnWeightMg", dPstMgParam * numOfStoppedPawnsDiff);
      gradientCache.put("stoppedPawnWeightEg", dPstEgParam * numOfStoppedPawnsDiff);
      gradientCache.put("pawnShieldWeight1Mg", dPstMgParam * pawnShield1Diff);
      gradientCache.put("pawnShieldWeight1Eg", dPstEgParam * pawnShield1Diff);
      gradientCache.put("pawnShieldWeight2Mg", dPstMgParam * pawnShield2Diff);
      gradientCache.put("pawnShieldWeight2Eg", dPstEgParam * pawnShield2Diff);
      gradientCache.put("blockedPawnWeightMg", dPstMgParam * numOfBlockedPawnsDiff);
      gradientCache.put("blockedPawnWeightEg", dPstEgParam * numOfBlockedPawnsDiff);
      gradientCache.put("passedPawnWeightMg", dPstMgParam * numOfPassedPawnsDiff);
      gradientCache.put("passedPawnWeightEg", dPstEgParam * numOfPassedPawnsDiff);
      gradientCache.put("isolatedPawnWeightMg", dPstMgParam * numOfIsolatedPawnsDiff);
      gradientCache.put("isolatedPawnWeightEg", dPstEgParam * numOfIsolatedPawnsDiff);
      gradientCache.put("backwardPawnWeightMg", dPstMgParam * numOfBackwardPawnsDiff);
      gradientCache.put("backwardPawnWeightEg", dPstEgParam * numOfBackwardPawnsDiff);
      gradientCache.put("queenMobilityWeightMg", dPstMgParam * queenMobilityDiff);
      gradientCache.put("queenMobilityWeightEg", dPstEgParam * queenMobilityDiff);
      gradientCache.put("rookMobilityWeightMg", dPstMgParam * rookMobilityDiff);
      gradientCache.put("rookMobilityWeightEg", dPstEgParam * rookMobilityDiff);
      gradientCache.put("bishopMobilityWeightMg", dPstMgParam * bishopMobilityDiff);
      gradientCache.put("bishopMobilityWeightEg", dPstEgParam * bishopMobilityDiff);
      gradientCache.put("knightMobilityWeightMg", dPstMgParam * knightMobilityDiff);
      gradientCache.put("knightMobilityWeightEg", dPstEgParam * knightMobilityDiff);
      gradientCache.put("pawnMobilityWeightMg", dPstMgParam * pawnMobilityDiff);
      gradientCache.put("pawnMobilityWeightEg", dPstEgParam * pawnMobilityDiff);
      gradientCache.put("pieceDefendedQueenWeightMg", dPstMgParam * numOfPieceDefendedQueensDiff);
      gradientCache.put("pieceDefendedQueenWeightEg", dPstEgParam * numOfPieceDefendedQueensDiff);
      gradientCache.put("pieceDefendedRookWeightMg", dPstMgParam * numOfPieceDefendedRooksDiff);
      gradientCache.put("pieceDefendedRookWeightEg", dPstEgParam * numOfPieceDefendedRooksDiff);
      gradientCache.put("pieceDefendedBishopWeightMg", dPstMgParam * numOfPieceDefendedBishopsDiff);
      gradientCache.put("pieceDefendedBishopWeightEg", dPstEgParam * numOfPieceDefendedBishopsDiff);
      gradientCache.put("pieceDefendedKnightWeightMg", dPstMgParam * numOfPieceDefendedKnightsDiff);
      gradientCache.put("pieceDefendedKnightWeightEg", dPstEgParam * numOfPieceDefendedKnightsDiff);
      gradientCache.put("pieceDefendedPawnWeightMg", dPstMgParam * numOfPieceDefendedPawnsDiff);
      gradientCache.put("pieceDefendedPawnWeightEg", dPstEgParam * numOfPieceDefendedPawnsDiff);
      gradientCache.put("pawnDefendedQueenWeightMg", dPstMgParam * numOfPawnDefendedQueensDiff);
      gradientCache.put("pawnDefendedQueenWeightEg", dPstEgParam * numOfPawnDefendedQueensDiff);
      gradientCache.put("pawnDefendedRookWeightMg", dPstMgParam * numOfPawnDefendedRooksDiff);
      gradientCache.put("pawnDefendedRookWeightEg", dPstEgParam * numOfPawnDefendedRooksDiff);
      gradientCache.put("pawnDefendedBishopWeightMg", dPstMgParam * numOfPawnDefendedBishopsDiff);
      gradientCache.put("pawnDefendedBishopWeightEg", dPstEgParam * numOfPawnDefendedBishopsDiff);
      gradientCache.put("pawnDefendedKnightWeightMg", dPstMgParam * numOfPawnDefendedKnightsDiff);
      gradientCache.put("pawnDefendedKnightWeightEg", dPstEgParam * numOfPawnDefendedKnightsDiff);
      gradientCache.put("pawnDefendedPawnWeightMg", dPstMgParam * numOfPawnDefendedPawnsDiff);
      gradientCache.put("pawnDefendedPawnWeightEg", dPstEgParam * numOfPawnDefendedPawnsDiff);
      gradientCache.put("friendlyQueenTropismWeightMg", dPstMgParam * friendlyQueenKingTropismDiff);
      gradientCache.put("friendlyQueenTropismWeightEg", dPstEgParam * friendlyQueenKingTropismDiff);
      gradientCache.put("friendlyRookTropismWeightMg", dPstMgParam * friendlyRookKingTropismDiff);
      gradientCache.put("friendlyRookTropismWeightEg", dPstEgParam * friendlyRookKingTropismDiff);
      gradientCache.put("friendlyBishopTropismWeightMg", dPstMgParam * friendlyBishopKingTropismDiff);
      gradientCache.put("friendlyBishopTropismWeightEg", dPstEgParam * friendlyBishopKingTropismDiff);
      gradientCache.put("friendlyKnightTropismWeightMg", dPstMgParam * friendlyKnightKingTropismDiff);
      gradientCache.put("friendlyKnightTropismWeightEg", dPstEgParam * friendlyKnightKingTropismDiff);
      gradientCache.put("friendlyPassedPawnTropismWeightMg", dPstMgParam * friendlyPassedPawnKingTropismDiff);
      gradientCache.put("friendlyPassedPawnTropismWeightEg", dPstEgParam * friendlyPassedPawnKingTropismDiff);
      gradientCache.put("friendlyWeakPawnTropismWeightMg", dPstMgParam * friendlyWeakPawnKingTropismDiff);
      gradientCache.put("friendlyWeakPawnTropismWeightEg", dPstEgParam * friendlyWeakPawnKingTropismDiff);
      gradientCache.put("friendlyNormalPawnTropismWeightMg", dPstMgParam * friendlyNormalPawnKingTropismDiff);
      gradientCache.put("friendlyNormalPawnTropismWeightEg", dPstEgParam * friendlyNormalPawnKingTropismDiff);
      gradientCache.put("opponentQueenTropismWeightMg", dPstMgParam * opponentQueenKingTropismDiff);
      gradientCache.put("opponentQueenTropismWeightEg", dPstEgParam * opponentQueenKingTropismDiff);
      gradientCache.put("opponentRookTropismWeightMg", dPstMgParam * opponentRookKingTropismDiff);
      gradientCache.put("opponentRookTropismWeightEg", dPstEgParam * opponentRookKingTropismDiff);
      gradientCache.put("opponentBishopTropismWeightMg", dPstMgParam * opponentBishopKingTropismDiff);
      gradientCache.put("opponentBishopTropismWeightEg", dPstEgParam * opponentBishopKingTropismDiff);
      gradientCache.put("opponentKnightTropismWeightMg", dPstMgParam * opponentKnightKingTropismDiff);
      gradientCache.put("opponentKnightTropismWeightEg", dPstEgParam * opponentKnightKingTropismDiff);
      gradientCache.put("opponentPassedPawnTropismWeightMg", dPstMgParam * opponentPassedPawnKingTropismDiff);
      gradientCache.put("opponentPassedPawnTropismWeightEg", dPstEgParam * opponentPassedPawnKingTropismDiff);
      gradientCache.put("opponentWeakPawnTropismWeightMg", dPstMgParam * opponentWeakPawnKingTropismDiff);
      gradientCache.put("opponentWeakPawnTropismWeightEg", dPstEgParam * opponentWeakPawnKingTropismDiff);
      gradientCache.put("opponentNormalPawnTropismWeightMg", dPstMgParam * opponentNormalPawnKingTropismDiff);
      gradientCache.put("opponentNormalPawnTropismWeightEg", dPstEgParam * opponentNormalPawnKingTropismDiff);
      gradientCache.put("attackedKingZoneSquareWeightMg", dPstMgParam * uniqueAttackedKingZoneSquaresDiff);
      gradientCache.put("attackedKingZoneSquareWeightEg", dPstEgParam * uniqueAttackedKingZoneSquaresDiff);
      gradientCache.put("kingZoneAttackerWeightMg", dPstMgParam * numKingZoneAttackersDiff);
      gradientCache.put("kingZoneAttackerWeightEg", dPstEgParam * numKingZoneAttackersDiff);
      gradientCache.put("mopUpCenterTropismWeight", (double) weakKingCenterTropism);
      for (int i = 0; i < 64; i++) {
        byte piece = pos.getPiece(i);
        if (piece == Piece.NULL.ind) {
          continue;
        }
        String pstMgParamName = PST_MG_PARAM_NAMES[piece - 1][i];
        String pstEgParamName = PST_EG_PARAM_NAMES[piece - 1][i];
        Double cachedPstMgParamGrad = gradientCache.getOrDefault(pstMgParamName, 0d);
        Double cachedPstEgParamGrad = gradientCache.getOrDefault(pstEgParamName, 0d);
        if (piece < Piece.B_KING.ind) {
          gradientCache.put(pstMgParamName, cachedPstMgParamGrad + dPstMgParam);
          gradientCache.put(pstEgParamName, cachedPstEgParamGrad + dPstEgParam);
        } else {
          gradientCache.put(pstMgParamName, cachedPstMgParamGrad - dPstMgParam);
          gradientCache.put(pstEgParamName, cachedPstEgParamGrad - dPstEgParam);
        }
      }
      String victimParamName = victimParamNameRef.get();
      String captorParamName = captorParamNameRef.get();
      if (victimParamName != null) {
        String victimParamMgName = victimParamName + MG_SUFFIX;
        String victimParamEgName = victimParamName + EG_SUFFIX;
        gradientCache.put(victimParamMgName, gradientCache.getOrDefault(victimParamMgName, 0d) + dPstMgParam * colorFactor);
        gradientCache.put(victimParamEgName, gradientCache.getOrDefault(victimParamEgName, 0d) + dPstEgParam * colorFactor);
      }
      if (captorParamName != null) {
        String captorParamMgName = captorParamName + MG_SUFFIX;
        String captorParamEgName = captorParamName + EG_SUFFIX;
        gradientCache.put(captorParamMgName, gradientCache.getOrDefault(captorParamMgName, 0d) - dPstMgParam * colorFactor);
        gradientCache.put(captorParamEgName, gradientCache.getOrDefault(captorParamEgName, 0d) - dPstEgParam * colorFactor);
      }
      gradientCache.put("tempoAdvantageMg", dPstMgParam * colorFactor);
      gradientCache.put("tempoAdvantageEg", dPstEgParam * colorFactor);
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
