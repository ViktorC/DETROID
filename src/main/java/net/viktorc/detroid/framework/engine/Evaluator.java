package net.viktorc.detroid.framework.engine;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import net.viktorc.detroid.framework.engine.Bitboard.Diagonal;
import net.viktorc.detroid.framework.engine.Bitboard.File;
import net.viktorc.detroid.framework.engine.Bitboard.Rank;
import net.viktorc.detroid.framework.util.BitOperations;
import net.viktorc.detroid.framework.util.Cache;

/**
 * A class for evaluating chess positions. It uses an evaluation hash table to improve performance. It also offers a static exchange
 * evaluation function.
 *
 * @author Viktor
 */
public class Evaluator {

  // MVV/LVA piece values.
  private static final byte[] MVV_LVA_PIECE_VALUES = new byte[]{0, 6, 5, 4, 3, 2, 1, 6, 5, 4, 3, 2, 1};
  // The values of different captor-victim combinations for MVV/LVA move assessment.
  private static final byte[][] MVV_LVA;

  static {
    short comb;
    byte assignedVal;
    int attackerVal, victimVal, lastVal;
    int attacker, victim;
    byte kingValue = MVV_LVA_PIECE_VALUES[Piece.W_KING.ordinal()];
    MVV_LVA = new byte[13][13];
    List<Entry<Short, Integer>> combVals = new ArrayList<>();
    // Include illogical combinations without discrimination for the sake of better performance.
    for (Piece a : Piece.values()) {
      if (a != Piece.NULL) {
        for (Piece v : Piece.values()) {
          if (v != Piece.NULL) {
            comb = (short) (((short) v.ordinal()) | (((short) a.ordinal()) << 7));
            attackerVal = MVV_LVA_PIECE_VALUES[a.ordinal()];
            victimVal = MVV_LVA_PIECE_VALUES[v.ordinal()];
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
      r1 = Rank.getBySquareIndex(i).ordinal();
      f1 = File.getBySquareIndex(i).ordinal();
      for (int j = 0; j < 64; j++) {
        r2 = Rank.getBySquareIndex(j).ordinal();
        f2 = File.getBySquareIndex(j).ordinal();
        rankDist = Math.abs(r2 - r1);
        fileDist = Math.abs(f2 - f1);
        MANHATTAN_DISTANCE[i][j] = (byte) (rankDist + fileDist);
        CHEBYSHEV_DISTANCE[i][j] = (byte) Math.max(rankDist, fileDist);
      }
    }
  }

  private final DetroidParameters params;
  // Evaluation score hash table.
  private final Cache<ETEntry> eT;
  // The sum of the respective weights of pieces for assessing the game phase.
  private final int totalPhaseWeights;

  private byte[][] pstOpening;
  private byte[][] pstEndgame;

  /**
   * Initializes a chess position evaluator.
   *
   * @param params A reference to the engine parameters.
   * @param evalTable A reference to the evaluation hash table to use.
   */
  public Evaluator(DetroidParameters params, Cache<ETEntry> evalTable) {
    this.params = params;
    totalPhaseWeights = 4 * (params.knightPhaseWeight + params.bishopPhaseWeight + params.rookPhaseWeight) + 2 * params.queenPhaseWeight;
    eT = evalTable;
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
      int bishopColor = Diagonal.getBySquareIndex(bishopSqrArr[0]).ordinal() % 2;
      for (int i = 1; i < bishopSqrArr.length; i++) {
        if (Diagonal.getBySquareIndex(bishopSqrArr[i]).ordinal() % 2 != bishopColor) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private void initPieceSquareArrays() {
    byte[] pstPawnOpening = params.getPstPawnOpening();
    byte[] pstPawnEndgame = params.getPstPawnEndgame();
    byte[] pstKnightOpening = params.getPstKnightOpening();
    byte[] pstKnightEndgame = params.getPstKnightEndgame();
    byte[] pstBishopOpening = params.getPstBishopOpening();
    byte[] pstBishopEnding = params.getPstBishopEnding();
    byte[] pstRookOpening = params.getPstRookOpening();
    byte[] pstRookEndgame = params.getPstRookEndgame();
    byte[] pstQueenOpening = params.getPstQueenOpening();
    byte[] pstQueenEnding = params.getPstQueenEnding();
    byte[] pstKingOpening = params.getPstKingOpening();
    byte[] pstKingEndgame = params.getPstKingEndgame();
    byte[] pstWpawnOpening = new byte[64];
    byte[] pstWpawnEndgame = new byte[64];
    byte[] pstWknightOpening = new byte[64];
    byte[] pstWknightEndgame = new byte[64];
    byte[] pstWbishopOpening = new byte[64];
    byte[] pstWbishopEnding = new byte[64];
    byte[] pstWrookOpening = new byte[64];
    byte[] pstWrookEndgame = new byte[64];
    byte[] pstWqueenOpening = new byte[64];
    byte[] pstWqueenEnding = new byte[64];
    byte[] pstWkingOpening = new byte[64];
    byte[] pstWkingEndgame = new byte[64];
    byte[] pstBpawnOpening = new byte[64];
    byte[] pstBpawnEndgame = new byte[64];
    byte[] pstBknightOpening = new byte[64];
    byte[] pstBknightEndgame = new byte[64];
    byte[] pstBbishopOpening = new byte[64];
    byte[] pstBbishopEnding = new byte[64];
    byte[] pstBrookOpening = new byte[64];
    byte[] pstBrookEndgame = new byte[64];
    byte[] pstBqueenOpening = new byte[64];
    byte[] pstBqueenEnding = new byte[64];
    byte[] pstBkingOpening = new byte[64];
    byte[] pstBkingEndgame = new byte[64];
    /* Due to the reversed order of the rows in the definition of the white piece-square tables, they are just
     * right for black with negated values. */
    for (int i = 0; i < 64; i++) {
      pstBpawnOpening[i] = (byte) -pstPawnOpening[i];
      pstBpawnEndgame[i] = (byte) -pstPawnEndgame[i];
      pstBknightOpening[i] = (byte) -pstKnightOpening[i];
      pstBknightEndgame[i] = (byte) -pstKnightEndgame[i];
      pstBbishopOpening[i] = (byte) -pstBishopOpening[i];
      pstBbishopEnding[i] = (byte) -pstBishopEnding[i];
      pstBrookOpening[i] = (byte) -pstRookOpening[i];
      pstBrookEndgame[i] = (byte) -pstRookEndgame[i];
      pstBqueenOpening[i] = (byte) -pstQueenOpening[i];
      pstBqueenEnding[i] = (byte) -pstQueenEnding[i];
      pstBkingOpening[i] = (byte) -pstKingOpening[i];
      pstBkingEndgame[i] = (byte) -pstKingEndgame[i];
    }
    // To get the right values for the white piece-square tables, vertically mirror and negate the ones for black.
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        int c1 = i * 8 + j;
        int c2 = ((7 - i) * 8) + j;
        pstWpawnOpening[c1] = (byte) -pstBpawnOpening[c2];
        pstWpawnEndgame[c1] = (byte) -pstBpawnEndgame[c2];
        pstWknightOpening[c1] = (byte) -pstBknightOpening[c2];
        pstWknightEndgame[c1] = (byte) -pstBknightEndgame[c2];
        pstWbishopOpening[c1] = (byte) -pstBbishopOpening[c2];
        pstWbishopEnding[c1] = (byte) -pstBbishopEnding[c2];
        pstWrookOpening[c1] = (byte) -pstBrookOpening[c2];
        pstWrookEndgame[c1] = (byte) -pstBrookEndgame[c2];
        pstWqueenOpening[c1] = (byte) -pstBqueenOpening[c2];
        pstWqueenEnding[c1] = (byte) -pstBqueenEnding[c2];
        pstWkingOpening[c1] = (byte) -pstBkingOpening[c2];
        pstWkingEndgame[c1] = (byte) -pstBkingEndgame[c2];
      }
    }
    // Set the opening and endgame arrays of piece square tables.
    pstOpening = new byte[][]{pstWkingOpening, pstWqueenOpening, pstWrookOpening, pstWbishopOpening,
        pstWknightOpening, pstWpawnOpening, pstBkingOpening, pstBqueenOpening, pstBrookOpening,
        pstBbishopOpening, pstBknightOpening, pstBpawnOpening};
    pstEndgame = new byte[][]{pstWkingEndgame, pstWqueenEnding, pstWrookEndgame, pstWbishopEnding,
        pstWknightEndgame, pstWpawnEndgame, pstBkingEndgame, pstBqueenEnding, pstBrookEndgame,
        pstBbishopEnding, pstBknightEndgame, pstBpawnEndgame};
  }

  /**
   * Returns the value of a piece type based on the engine parameters.
   *
   * @param pieceInd The index of the piece type.
   * @return The value of the piece type.
   */
  public short materialValueByPieceInd(int pieceInd) {
    if (pieceInd == Piece.W_KING.ordinal()) {
      return params.kingValue;
    } else if (pieceInd == Piece.W_QUEEN.ordinal()) {
      return params.queenValue;
    } else if (pieceInd == Piece.W_ROOK.ordinal()) {
      return params.rookValue;
    } else if (pieceInd == Piece.W_BISHOP.ordinal()) {
      return params.bishopValue;
    } else if (pieceInd == Piece.W_KNIGHT.ordinal()) {
      return params.knightValue;
    } else if (pieceInd == Piece.W_PAWN.ordinal()) {
      return params.pawnValue;
    } else if (pieceInd == Piece.B_KING.ordinal()) {
      return params.kingValue;
    } else if (pieceInd == Piece.B_QUEEN.ordinal()) {
      return params.queenValue;
    } else if (pieceInd == Piece.B_ROOK.ordinal()) {
      return params.rookValue;
    } else if (pieceInd == Piece.B_BISHOP.ordinal()) {
      return params.bishopValue;
    } else if (pieceInd == Piece.B_KNIGHT.ordinal()) {
      return params.knightValue;
    } else if (pieceInd == Piece.B_PAWN.ordinal()) {
      return params.pawnValue;
    } else if (pieceInd == Piece.NULL.ordinal()) {
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
  public short MVVLVA(Move move) {
    short score = 0;
    if (move.getType() == MoveType.PROMOTION_TO_QUEEN.ordinal()) {
      byte queenValue = MVV_LVA_PIECE_VALUES[Piece.W_QUEEN.ordinal()];
      score += queenValue * queenValue;
    }
    score += MVV_LVA[move.getMovedPiece()][move.getCapturedPiece()];
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
    short victimVal = materialValueByPieceInd(move.getCapturedPiece());
    // If the captor was a king, return the captured piece's value as capturing the king would be illegal.
    if (move.getMovedPiece() == Piece.W_KING.ordinal() || move.getMovedPiece() == Piece.B_KING.ordinal()) {
      return victimVal;
    }
    int i = 0;
    short[] gains = new short[32];
    gains[i] = victimVal;
    short attackerVal;
    // In case the move is a promotion.
    if (move.getType() >= MoveType.PROMOTION_TO_QUEEN.ordinal()) {
      if (move.getType() == MoveType.PROMOTION_TO_QUEEN.ordinal()) {
        gains[i] += params.queenValue - params.pawnValue;
        attackerVal = params.queenValue;
      } else if (move.getType() == MoveType.PROMOTION_TO_ROOK.ordinal()) {
        gains[i] += params.rookValue - params.pawnValue;
        attackerVal = params.rookValue;
      } else if (move.getType() == MoveType.PROMOTION_TO_BISHOP.ordinal()) {
        gains[i] += params.bishopValue - params.pawnValue;
        attackerVal = params.bishopValue;
      } else { // PROMOTION_TO_KNIGHT
        gains[i] += params.knightValue - params.pawnValue;
        attackerVal = params.knightValue;
      }
    } else {
      attackerVal = materialValueByPieceInd(move.getMovedPiece());
    }
    long occupied = pos.getAllOccupied() ^ BitOperations.toBit(move.getFrom());
    boolean whitesTurn = pos.isWhitesTurn();
    MoveSetBase dB = MoveSetBase.values()[move.getTo()];
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
          attackerVal = params.kingValue;
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
          attackerVal = params.kingValue;
        } else {
          break;
        }
      }
      // If the previous attacker was a king and the side to move can attack it, the exchange is over.
      if (prevAttackerVal == params.kingValue) {
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

  private int phaseScore(int numOfQueens, int numOfRooks, int numOfBishops, int numOfKnights) {
    int phase = totalPhaseWeights - (numOfQueens * params.queenPhaseWeight + numOfRooks * params.rookPhaseWeight
        + numOfBishops * params.bishopPhaseWeight + numOfKnights * params.knightPhaseWeight);
    return (phase * params.gamePhaseEndgameUpper + totalPhaseWeights / 2) / totalPhaseWeights;
  }

  /**
   * Returns an estimation of the phase in which the current game is based on the given position.
   *
   * @param pos The position whose phase is to be gauged.
   * @return The phase estimate.
   */
  public int phaseScore(Position pos) {
    int numOfQueens = BitOperations.hammingWeight(pos.getWhiteQueens() | pos.getBlackQueens());
    int numOfRooks = BitOperations.hammingWeight(pos.getWhiteRooks() | pos.getBlackRooks());
    int numOfBishops = BitOperations.hammingWeight(pos.getWhiteBishops() | pos.getBlackBishops());
    int numOfKnights = BitOperations.hammingWeight(pos.getWhiteKnights() | pos.getBlackKnights());
    return phaseScore(numOfQueens, numOfRooks, numOfBishops, numOfKnights);
  }

  private int taperedEvalScore(int openingEval, int endGameEval, int phaseScore) {
    return (openingEval * (params.gamePhaseEndgameUpper - phaseScore) + endGameEval * phaseScore) /
        params.gamePhaseEndgameUpper;
  }

  private short pawnKingStructureScore(long whiteKing, long blackKing, long whitePawns, long blackPawns) {
    int score = 0;
    byte numOfWhitePawns = BitOperations.hammingWeight(whitePawns);
    byte numOfBlackPawns = BitOperations.hammingWeight(blackPawns);
    // Pawn attacks.
    long whitePawnAttacks = Bitboard.computeWhitePawnCaptureSets(whitePawns, Bitboard.FULL_BOARD);
    long blackPawnAttacks = Bitboard.computeBlackPawnCaptureSets(blackPawns, Bitboard.FULL_BOARD);
    // Blocked pawns.
    score += params.blockedPawnWeight1 * (BitOperations.hammingWeight((blackPawns >>> 8) & blackPawns) -
        BitOperations.hammingWeight((whitePawns << 8) & whitePawns));
    score += params.blockedPawnWeight2 * (BitOperations.hammingWeight((blackPawns >>> 16) & blackPawns) -
        BitOperations.hammingWeight((whitePawns << 16) & whitePawns));
    score += params.blockedPawnWeight3 * (BitOperations.hammingWeight((blackPawns >>> 24) & blackPawns) -
        BitOperations.hammingWeight((whitePawns << 24) & whitePawns));
    // Passed pawns.
    long whiteAdvanceSpans = whitePawns << 8;
    whiteAdvanceSpans |= whiteAdvanceSpans << 16;
    whiteAdvanceSpans |= whiteAdvanceSpans << 32;
    long whiteAttackSpans = ((whiteAdvanceSpans >>> 1) & ~File.H.getBitboard()) | ((whiteAdvanceSpans << 1) &
        ~File.A.getBitboard());
    long whiteFrontSpans = whiteAdvanceSpans | whiteAttackSpans;
    long blackAdvanceSpans = blackPawns >>> 8;
    blackAdvanceSpans |= blackAdvanceSpans >>> 16;
    blackAdvanceSpans |= blackAdvanceSpans >>> 32;
    long blackAttackSpans = ((blackAdvanceSpans >>> 1) & ~File.H.getBitboard()) | ((blackAdvanceSpans << 1) &
        ~File.A.getBitboard());
    long blackFrontSpans = blackAdvanceSpans | blackAttackSpans;
    long whitePassedPawns = whitePawns & ~blackFrontSpans;
    long blackPassedPawns = blackPawns & ~whiteFrontSpans;
    score += params.passedPawnWeight * (BitOperations.hammingWeight(whitePassedPawns) -
        BitOperations.hammingWeight(blackPassedPawns));
    // Backward pawns.
    long whiteOpenBackwardPawns = (whitePawns & (blackPawnAttacks >>> 8) &
        ~(whiteAttackSpans | (whiteAttackSpans >>> 8))) & ~blackAdvanceSpans;
    long blackOpenBackwardPawns = (blackPawns & (whitePawnAttacks << 8) &
        ~(blackAttackSpans | (blackAttackSpans << 8))) & ~whiteAdvanceSpans;
    score += params.openBackwardPawnWeight * (BitOperations.hammingWeight(blackOpenBackwardPawns) -
        BitOperations.hammingWeight(whiteOpenBackwardPawns));
    // King-pawn tropism.
    byte whiteKingInd = BitOperations.indexOfBit(whiteKing);
    byte blackKingInd = BitOperations.indexOfBit(blackKing);
    long whitePawnSet = whitePawns;
    int whiteKingWhitePawnTropism = 0;
    int blackKingWhitePawnTropism = 0;
    while (whitePawnSet != Bitboard.EMPTY_BOARD) {
      long pawn = BitOperations.getLSBit(whitePawnSet);
      byte pawnInd = BitOperations.indexOfBit(pawn);
      if ((whitePassedPawns & pawn) != Bitboard.EMPTY_BOARD) {
        whiteKingWhitePawnTropism += params.kingFriendlyPassedPawnTropismWeight *
            MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
        blackKingWhitePawnTropism += params.kingOpponentPassedPawnTropismWeight *
            MANHATTAN_DISTANCE[blackKingInd][pawnInd];
      } else if ((whiteOpenBackwardPawns & pawn) != Bitboard.EMPTY_BOARD) {
        whiteKingWhitePawnTropism += params.kingFriendlyOpenBackwardPawnTropismWeight *
            MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
        blackKingWhitePawnTropism += params.kingOpponentOpenBackwardPawnTropismWeight *
            MANHATTAN_DISTANCE[blackKingInd][pawnInd];
      } else {
        whiteKingWhitePawnTropism += params.kingFriendlyNormalPawnTropismWeight *
            MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
        blackKingWhitePawnTropism += params.kingOpponentNormalPawnTropismWeight *
            MANHATTAN_DISTANCE[blackKingInd][pawnInd];
      }
      whitePawnSet = BitOperations.resetLSBit(whitePawnSet);
    }
    long blackPawnSet = blackPawns;
    int whiteKingBlackPawnTropism = 0;
    int blackKingBlackPawnTropism = 0;
    while (blackPawnSet != Bitboard.EMPTY_BOARD) {
      long pawn = BitOperations.getLSBit(blackPawnSet);
      byte pawnInd = BitOperations.indexOfBit(pawn);
      if ((blackPassedPawns & pawn) != Bitboard.EMPTY_BOARD) {
        whiteKingBlackPawnTropism += params.kingOpponentPassedPawnTropismWeight *
            MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
        blackKingBlackPawnTropism += params.kingFriendlyPassedPawnTropismWeight *
            MANHATTAN_DISTANCE[blackKingInd][pawnInd];
      } else if ((blackOpenBackwardPawns & pawn) != Bitboard.EMPTY_BOARD) {
        whiteKingBlackPawnTropism += params.kingOpponentOpenBackwardPawnTropismWeight *
            MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
        blackKingBlackPawnTropism += params.kingFriendlyOpenBackwardPawnTropismWeight *
            MANHATTAN_DISTANCE[blackKingInd][pawnInd];
      } else {
        whiteKingBlackPawnTropism += params.kingOpponentNormalPawnTropismWeight *
            MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
        blackKingBlackPawnTropism += params.kingFriendlyNormalPawnTropismWeight *
            MANHATTAN_DISTANCE[blackKingInd][pawnInd];
      }
      blackPawnSet = BitOperations.resetLSBit(blackPawnSet);
    }
    score -= numOfWhitePawns != 0 ? (whiteKingWhitePawnTropism - blackKingWhitePawnTropism) / numOfWhitePawns : 0;
    score += numOfBlackPawns != 0 ? (blackKingBlackPawnTropism - whiteKingBlackPawnTropism) / numOfBlackPawns : 0;
    return (short) score;
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
  int score(Position pos, byte hashGen, ETEntry entry) {
    // Probe evaluation hash table.
    if (eT != null) {
      ETEntry eE = eT.get(pos.getKey());
      if (eE != null && eE.hashKey() == pos.getKey()) {
        eE.setGeneration(hashGen);
        return eE.getScore();
      }
    }
    short score = 0;
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
        return Score.INSUFFICIENT_MATERIAL.getValue();
      }
      if (numOfAllPieces >= 4 && numOfWhiteKnights == 0 && numOfBlackKnights == 0) {
        boolean allSameColor = true;
        long bishopSet = pos.getWhiteBishops() | pos.getBlackBishops();
        int bishopSqrColor = Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(bishopSet)).ordinal() % 2;
        bishopSet = BitOperations.resetLSBit(bishopSet);
        while (bishopSet != 0) {
          if (Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(bishopSet)).ordinal() % 2 != bishopSqrColor) {
            allSameColor = false;
            break;
          }
          bishopSet = BitOperations.resetLSBit(bishopSet);
        }
        if (allSameColor) {
          return Score.INSUFFICIENT_MATERIAL.getValue();
        }
      }
    }
    byte numOfWhitePawns = BitOperations.hammingWeight(pos.getWhitePawns());
    byte numOfBlackPawns = BitOperations.hammingWeight(pos.getBlackPawns());
    // Base material score.
    score += params.queenValue * (numOfWhiteQueens - numOfBlackQueens);
    score += params.rookValue * (numOfWhiteRooks - numOfBlackRooks);
    score += params.bishopValue * (numOfWhiteBishops - numOfBlackBishops);
    score += params.knightValue * (numOfWhiteKnights - numOfBlackKnights);
    short openingScore = (byte) ((numOfWhitePawns - numOfBlackPawns) * params.pawnValue);
    short endgameScore = (byte) ((numOfWhitePawns - numOfBlackPawns) * params.pawnEndgameValue);
    // Piece-square scores.
    for (int i = 0; i < 64; i++) {
      byte piece = (byte) (pos.getPiece(i) - 1);
      if (piece < Piece.NULL.ordinal()) {
        continue;
      }
      openingScore += pstOpening[piece][i];
      endgameScore += pstEndgame[piece][i];
    }
    // Phase score for tapered evaluation.
    int phase = phaseScore(numOfWhiteQueens + numOfBlackQueens, numOfWhiteRooks + numOfBlackRooks,
        numOfWhiteBishops + numOfBlackBishops, numOfWhiteKnights + numOfBlackKnights);
    score += (short) taperedEvalScore(openingScore, endgameScore, phase);
    // Bishop pair advantage.
    if (numOfWhiteBishops >= 2 && Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(
        pos.getWhiteBishops())).ordinal() % 2 != Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(
        BitOperations.resetLSBit(pos.getWhiteBishops()))).ordinal() % 2) {
      score += params.bishopPairAdvantage;
    }
    if (numOfBlackBishops >= 2 && Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(
        pos.getBlackBishops())).ordinal() % 2 != Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(
        BitOperations.resetLSBit(pos.getBlackBishops()))).ordinal() % 2) {
      score -= params.bishopPairAdvantage;
    }
    // Pawn structure.
    score += pawnKingStructureScore(pos.getWhiteKing(), pos.getBlackKing(), pos.getWhitePawns(), pos.getBlackPawns());
    // Stopped pawns.
    score += params.stoppedPawnWeight *
        (BitOperations.hammingWeight(Bitboard.computeBlackPawnAdvanceSets(pos.getBlackPawns(),
            Bitboard.FULL_BOARD) & (pos.getAllWhiteOccupied() ^ pos.getWhitePawns())) -
            BitOperations.hammingWeight(Bitboard.computeWhitePawnAdvanceSets(pos.getWhitePawns(),
                Bitboard.FULL_BOARD) & (pos.getAllBlackOccupied() ^ pos.getBlackPawns())));
    // Adjust score to side to move.
    if (!pos.isWhitesTurn()) {
      score *= -1;
    }
    // Tempo advantage.
    score += params.tempoAdvantage;
    if (eT != null) {
      entry.set(pos.getKey(), score, hashGen);
      entry.setupKey();
      eT.put(entry);
    }
    return score;
  }

}
