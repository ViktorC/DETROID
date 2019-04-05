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

  static final int MAX_PHASE_SCORE = 256;
  static final int QUEEN_PHASE_WEIGHT = 4;
  static final int ROOK_PHASE_WEIGHT = 2;
  static final int BISHOP_PHASE_WEIGHT = 1;
  static final int KNIGHT_PHASE_WEIGHT = 1;
  static final int TOTAL_OPENING_PHASE_WEIGHT = 24;

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

  private final DetroidParameters params;
  // Evaluation score hash table.
  private final Cache<ETEntry> evalTable;

  private byte[][] pstMg;
  private byte[][] pstEg;

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
    byte[] pstPawnMg = params.getPstPawnMg();
    byte[] pstPawnEg = params.getPstPawnEg();
    byte[] pstKnight = params.getPstKnight();
    byte[] pstBishop = params.getPstBishop();
    byte[] pstRookMg = params.getPstRookMg();
    byte[] pstRookEg = params.getPstRookEg();
    byte[] pstQueen = params.getPstQueen();
    byte[] pstKingMg = params.getPstKingMg();
    byte[] pstKingEg = params.getPstKingEg();
    byte[] pstWPawnMg = new byte[64];
    byte[] pstWPawnEg = new byte[64];
    byte[] pstWKnight = new byte[64];
    byte[] pstWBishop = new byte[64];
    byte[] pstWRookMg = new byte[64];
    byte[] pstWRookEg = new byte[64];
    byte[] pstWQueen = new byte[64];
    byte[] pstWKingMg = new byte[64];
    byte[] pstWKingEg = new byte[64];
    byte[] pstBPawnMg = new byte[64];
    byte[] pstBPawnEg = new byte[64];
    byte[] pstBKnight = new byte[64];
    byte[] pstBBishop = new byte[64];
    byte[] pstBRookMg = new byte[64];
    byte[] pstBRookEg = new byte[64];
    byte[] pstBQueen = new byte[64];
    byte[] pstBKingMg = new byte[64];
    byte[] pstBKingEg = new byte[64];
    /* Due to the reversed order of the rows in the definition of the white piece-square tables, they are just
     * right for black with negated values. */
    for (int i = 0; i < 64; i++) {
      pstBPawnMg[i] = (byte) -pstPawnMg[i];
      pstBPawnEg[i] = (byte) -pstPawnEg[i];
      pstBKnight[i] = (byte) -pstKnight[i];
      pstBBishop[i] = (byte) -pstBishop[i];
      pstBRookMg[i] = (byte) -pstRookMg[i];
      pstBRookEg[i] = (byte) -pstRookEg[i];
      pstBQueen[i] = (byte) -pstQueen[i];
      pstBKingMg[i] = (byte) -pstKingMg[i];
      pstBKingEg[i] = (byte) -pstKingEg[i];
    }
    // To get the right values for the white piece-square tables, vertically mirror and negate the ones for black.
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        int c1 = i * 8 + j;
        int c2 = ((7 - i) * 8) + j;
        pstWPawnMg[c1] = (byte) -pstBPawnMg[c2];
        pstWPawnEg[c1] = (byte) -pstBPawnEg[c2];
        pstWKnight[c1] = (byte) -pstBKnight[c2];
        pstWBishop[c1] = (byte) -pstBBishop[c2];
        pstWRookMg[c1] = (byte) -pstBRookMg[c2];
        pstWRookEg[c1] = (byte) -pstBRookEg[c2];
        pstWQueen[c1] = (byte) -pstBQueen[c2];
        pstWKingMg[c1] = (byte) -pstBKingMg[c2];
        pstWKingEg[c1] = (byte) -pstBKingEg[c2];
      }
    }
    // Set the opening and endgame arrays of piece square tables.
    pstMg = new byte[][]{pstWKingMg, pstWQueen, pstWRookMg, pstWBishop, pstWKnight, pstWPawnMg,
        pstBKingMg, pstBQueen, pstBRookMg, pstBBishop, pstBKnight, pstBPawnMg};
    pstEg = new byte[][]{pstWKingEg, pstWQueen, pstWRookEg, pstWBishop, pstWKnight, pstWPawnEg,
        pstBKingEg, pstBQueen, pstBRookEg, pstBBishop, pstBKnight, pstBPawnEg};
  }

  /**
   * Returns the value of a piece type based on the engine parameters.
   *
   * @param pieceInd The index of the piece type.
   * @return The value of the piece type.
   */
  public short materialValueByPieceInd(int pieceInd) {
    if (pieceInd == Piece.W_KING.ind) {
      return params.kingValue;
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
      return params.kingValue;
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
  public short MVVLVA(Move move) {
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
  public int phaseScore(Position pos) {
    int numOfQueens = BitOperations.hammingWeight(pos.getWhiteQueens() | pos.getBlackQueens());
    int numOfRooks = BitOperations.hammingWeight(pos.getWhiteRooks() | pos.getBlackRooks());
    int numOfBishops = BitOperations.hammingWeight(pos.getWhiteBishops() | pos.getBlackBishops());
    int numOfKnights = BitOperations.hammingWeight(pos.getWhiteKnights() | pos.getBlackKnights());
    return phaseScore(numOfQueens, numOfRooks, numOfBishops, numOfKnights);
  }

  private int taperedEvalScore(int openingEval, int endGameEval, int phaseScore) {
    return (openingEval * (MAX_PHASE_SCORE - phaseScore) + endGameEval * phaseScore) / MAX_PHASE_SCORE;
  }

  private short pawnKingStructureScore(long whiteKing, long blackKing, long whitePawns, long blackPawns) {
    int score = 0;
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
    long whiteAdvanceSpans = Bitboard.fillNorth(whitePawns) & ~whitePawns;
    long whiteAttackSpans = ((whiteAdvanceSpans >>> 1) & ~File.H.bitboard) | ((whiteAdvanceSpans << 1) &
        ~File.A.bitboard);
    long whiteFrontSpans = whiteAdvanceSpans | whiteAttackSpans;
    long blackAdvanceSpans = Bitboard.fillSouth(blackPawns) & ~whitePawns;
    long blackAttackSpans = ((blackAdvanceSpans >>> 1) & ~File.H.bitboard) | ((blackAdvanceSpans << 1) &
        ~File.A.bitboard);
    long blackFrontSpans = blackAdvanceSpans | blackAttackSpans;
    long whitePassedPawns = whitePawns & ~blackFrontSpans;
    long blackPassedPawns = blackPawns & ~whiteFrontSpans;
    score += params.passedPawnWeight * (BitOperations.hammingWeight(whitePassedPawns) -
        BitOperations.hammingWeight(blackPassedPawns));
    // Isolated pawns.
    long whiteSideSpans = Bitboard.fillSouth(whiteAttackSpans);
    long blackSideSpans = Bitboard.fillNorth(blackAttackSpans);
    long whiteIsolatedPawns = whitePawns & ~whiteSideSpans;
    long blackIsolatedPawns = blackPawns & ~blackSideSpans;
    score += params.isolatedPawnWeight * (BitOperations.hammingWeight(blackIsolatedPawns) -
        BitOperations.hammingWeight(whiteIsolatedPawns));
    // Backward pawns.
    long whiteBackwardPawns = whitePawns & ((blackPawnAttacks & ~whiteAttackSpans) >>> 8);
    long blackBackwardPawns = blackPawns & ((whitePawnAttacks & ~blackAttackSpans) << 8);
    score += params.backwardPawnWeight * (BitOperations.hammingWeight(blackBackwardPawns) -
        BitOperations.hammingWeight(whiteBackwardPawns));
    long whiteWeakPawns = whiteIsolatedPawns | whiteBackwardPawns;
    long blackWeakPawns = blackIsolatedPawns | blackBackwardPawns;
    // King-pawn tropism.
    int numOfWhitePawns = 0;
    int numOfBlackPawns = 0;
    byte whiteKingInd = BitOperations.indexOfBit(whiteKing);
    byte blackKingInd = BitOperations.indexOfBit(blackKing);
    int whiteKingWhitePawnTropism = 0;
    int blackKingWhitePawnTropism = 0;
    long whitePawnSet = whitePawns;
    while (whitePawnSet != Bitboard.EMPTY_BOARD) {
      numOfWhitePawns++;
      long pawn = BitOperations.getLSBit(whitePawnSet);
      byte pawnInd = BitOperations.indexOfBit(pawn);
      if ((whitePassedPawns & pawn) != Bitboard.EMPTY_BOARD) {
        whiteKingWhitePawnTropism += params.friendlyPassedPawnTropismWeight * MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
        blackKingWhitePawnTropism += params.opponentPassedPawnTropismWeight * MANHATTAN_DISTANCE[blackKingInd][pawnInd];
      } else if ((whiteWeakPawns & pawn) != Bitboard.EMPTY_BOARD) {
        whiteKingWhitePawnTropism += params.friendlyWeakPawnTropismWeight * MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
        blackKingWhitePawnTropism += params.opponentWeakPawnTropismWeight * MANHATTAN_DISTANCE[blackKingInd][pawnInd];
      } else {
        whiteKingWhitePawnTropism += params.friendlyNormalPawnTropismWeight * MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
        blackKingWhitePawnTropism += params.opponentNormalPawnTropismWeight * MANHATTAN_DISTANCE[blackKingInd][pawnInd];
      }
      whitePawnSet = BitOperations.resetLSBit(whitePawnSet);
    }
    long blackPawnSet = blackPawns;
    int whiteKingBlackPawnTropism = 0;
    int blackKingBlackPawnTropism = 0;
    while (blackPawnSet != Bitboard.EMPTY_BOARD) {
      numOfBlackPawns++;
      long pawn = BitOperations.getLSBit(blackPawnSet);
      byte pawnInd = BitOperations.indexOfBit(pawn);
      if ((blackPassedPawns & pawn) != Bitboard.EMPTY_BOARD) {
        whiteKingBlackPawnTropism += params.opponentPassedPawnTropismWeight * MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
        blackKingBlackPawnTropism += params.friendlyPassedPawnTropismWeight * MANHATTAN_DISTANCE[blackKingInd][pawnInd];
      } else if ((blackWeakPawns & pawn) != Bitboard.EMPTY_BOARD) {
        whiteKingBlackPawnTropism += params.opponentWeakPawnTropismWeight * MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
        blackKingBlackPawnTropism += params.friendlyWeakPawnTropismWeight * MANHATTAN_DISTANCE[blackKingInd][pawnInd];
      } else {
        whiteKingBlackPawnTropism += params.opponentNormalPawnTropismWeight * MANHATTAN_DISTANCE[whiteKingInd][pawnInd];
        blackKingBlackPawnTropism += params.friendlyNormalPawnTropismWeight * MANHATTAN_DISTANCE[blackKingInd][pawnInd];
      }
      blackPawnSet = BitOperations.resetLSBit(blackPawnSet);
    }
    score -= numOfWhitePawns != 0 ? (whiteKingWhitePawnTropism - blackKingWhitePawnTropism) / numOfWhitePawns : 0;
    score += numOfBlackPawns != 0 ? (blackKingBlackPawnTropism - whiteKingBlackPawnTropism) / numOfBlackPawns : 0;
    return (short) score;
  }

  private int getValueOfMostValuableRookCapture(long candidates, long queens) {
    // If the attacker is a rook and it isn't attacking a queen, the exchange cannot be profitable.
    return (queens & candidates) != Bitboard.EMPTY_BOARD ? params.queenValue : 0;
  }

  private int getValueOfMostValuableMinorPieceOrPawnCapture(long candidates, long queens, long rooks, long bishops, long knights) {
    // The queen is clearly the most valuable piece.
    if ((queens & candidates) != Bitboard.EMPTY_BOARD) {
      return params.queenValue;
    }
    // And the rook is clearly the second most valuable.
    if ((rooks & candidates) != Bitboard.EMPTY_BOARD) {
      return params.rookValue;
    }
    // Beyond that, it gets contentious.
    int highestValue = 0;
    if ((bishops & candidates) != Bitboard.EMPTY_BOARD) {
      highestValue = Math.max(highestValue, params.bishopValue);
    }
    if ((knights & candidates) != Bitboard.EMPTY_BOARD) {
      highestValue = Math.max(highestValue, params.knightValue);
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
   * @return The score of the position.
   */
  public int score(Position pos, byte hashGen, ETEntry entry) {
    // Probe evaluation hash table.
    if (evalTable != null) {
      ETEntry eE = evalTable.get(pos.getKey());
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
    score += params.queenValue * (numOfWhiteQueens - numOfBlackQueens);
    score += params.rookValue * (numOfWhiteRooks - numOfBlackRooks);
    score += params.bishopValue * (numOfWhiteBishops - numOfBlackBishops);
    score += params.knightValue * (numOfWhiteKnights - numOfBlackKnights);
    score += params.pawnValue * (numOfWhitePawns - numOfBlackPawns);
    short midgameScore = 0;
    short endgameScore = 0;
    // Piece-square scores.
    for (int i = 0; i < 64; i++) {
      byte piece = (byte) (pos.getPiece(i) - 1);
      if (piece < Piece.NULL.ind) {
        continue;
      }
      midgameScore += pstMg[piece][i];
      endgameScore += pstEg[piece][i];
    }
    // Phase score for tapered evaluation.
    int phaseScore = phaseScore(numOfWhiteQueens + numOfBlackQueens, numOfWhiteRooks + numOfBlackRooks,
        numOfWhiteBishops + numOfBlackBishops, numOfWhiteKnights + numOfBlackKnights);
    score += (short) taperedEvalScore(midgameScore, endgameScore, phaseScore);
    // Bishop pair advantage.
    if (numOfWhiteBishops >= 2 && Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(
        pos.getWhiteBishops())).ind % 2 != Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(
        BitOperations.resetLSBit(pos.getWhiteBishops()))).ind % 2) {
      score += params.bishopPairAdvantage;
    }
    if (numOfBlackBishops >= 2 && Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(
        pos.getBlackBishops())).ind % 2 != Diagonal.getBySquareIndex(BitOperations.indexOfLSBit(
        BitOperations.resetLSBit(pos.getBlackBishops()))).ind % 2) {
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
    // Pinned pieces.
    byte whiteKingInd = BitOperations.indexOfBit(pos.getWhiteKing());
    byte blackKingInd = BitOperations.indexOfBit(pos.getBlackKing());
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
    score += params.pinnedQueenWeight * (BitOperations.hammingWeight(pos.getBlackQueens() & blackPinnedPieces) -
        BitOperations.hammingWeight(pos.getWhiteQueens() & whitePinnedPieces));
    score += params.pinnedRookWeight * (BitOperations.hammingWeight(pos.getBlackRooks() & blackPinnedPieces) -
        BitOperations.hammingWeight(pos.getWhiteRooks() & whitePinnedPieces));
    score += params.pinnedBishopWeight * (BitOperations.hammingWeight(pos.getBlackBishops() & blackPinnedPieces) -
        BitOperations.hammingWeight(pos.getWhiteBishops() & whitePinnedPieces));
    score += params.pinnedKnightWeight * (BitOperations.hammingWeight(pos.getBlackKnights() & blackPinnedPieces) -
        BitOperations.hammingWeight(pos.getWhiteKnights() & whitePinnedPieces));
    score += params.pinnedPawnWeight * (BitOperations.hammingWeight(pos.getBlackPawns() & blackPinnedPieces) -
        BitOperations.hammingWeight(pos.getWhitePawns() & whitePinnedPieces));
    // Iterate over pieces to assess their mobility and distance from the opponent's king.
    long whitePieceSet = ((pos.getAllWhiteOccupied() ^ pos.getWhitePawns()) ^ pos.getWhiteKing());
    long blackPieceSet = ((pos.getAllBlackOccupied() ^ pos.getBlackPawns()) ^ pos.getBlackKing());
    long whitePawnCaptors = Bitboard.computeBlackPawnCaptureSets(blackPieceSet, pos.getWhitePawns());
    long blackPawnCaptors = Bitboard.computeWhitePawnCaptureSets(whitePieceSet, pos.getBlackPawns());
    whitePieceSet |= whitePawnCaptors;
    blackPieceSet |= blackPawnCaptors;
    int whiteQueenKingTropism = 0;
    int blackQueenKingTropism = 0;
    long whitePawnCaptures = Bitboard.EMPTY_BOARD;
    long whiteKnightAttacks = Bitboard.EMPTY_BOARD;
    long whiteBishopAttacks = Bitboard.EMPTY_BOARD;
    long whiteRookAttacks = Bitboard.EMPTY_BOARD;
    int whiteQueenMobility = 0;
    int whiteRookMobility = 0;
    int whiteBishopMobility = 0;
    int whiteKnightMobility = 0;
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
      if (pieceType == Piece.W_QUEEN.ind) {
        long moveSet = (moveSetDb.getQueenMoveSet(pos.getAllNonWhiteOccupied(), pos.getAllOccupied()) & pinnedPieceMoveSetRestriction);
        whiteQueenMobility += BitOperations.hammingWeight(moveSet);
        whiteQueenKingTropism += CHEBYSHEV_DISTANCE[pieceInd][blackKingInd];
      } else if (pieceType == Piece.W_ROOK.ind) {
        long moveSet = (moveSetDb.getRookMoveSet(pos.getAllNonWhiteOccupied(), pos.getAllOccupied()) & pinnedPieceMoveSetRestriction);
        whiteRookAttacks |= moveSet;
        whiteRookMobility += BitOperations.hammingWeight(moveSet);
      } else if (pieceType == Piece.W_BISHOP.ind) {
        long moveSet = (moveSetDb.getBishopMoveSet(pos.getAllNonWhiteOccupied(), pos.getAllOccupied()) & pinnedPieceMoveSetRestriction);
        whiteBishopAttacks |= moveSet;
        whiteBishopMobility += BitOperations.hammingWeight(moveSet);
      } else if (pieceType == Piece.W_KNIGHT.ind) {
        long moveSet = (moveSetDb.getKnightMoveSet(pos.getAllNonWhiteOccupied()) & pinnedPieceMoveSetRestriction);
        whiteKnightAttacks |= moveSet;
        whiteKnightMobility += BitOperations.hammingWeight(moveSet);
      } else { // W_PAWN
        long moveSet = (moveSetDb.getWhitePawnCaptureSet(pos.getAllBlackOccupied()) & pinnedPieceMoveSetRestriction);
        whitePawnCaptures |= moveSet;
      }
      whitePieceSet = BitOperations.resetLSBit(whitePieceSet);
    }
    long blackPawnCaptures = Bitboard.EMPTY_BOARD;
    long blackKnightAttacks = Bitboard.EMPTY_BOARD;
    long blackBishopAttacks = Bitboard.EMPTY_BOARD;
    long blackRookAttacks = Bitboard.EMPTY_BOARD;
    int blackQueenMobility = 0;
    int blackRookMobility = 0;
    int blackBishopMobility = 0;
    int blackKnightMobility = 0;
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
      if (pieceType == Piece.B_QUEEN.ind) {
        long moveSet = (moveSetDb.getQueenMoveSet(pos.getAllNonBlackOccupied(), pos.getAllOccupied()) & pinnedPieceMoveSetRestriction);
        blackQueenMobility += BitOperations.hammingWeight(moveSet);
        blackQueenKingTropism += CHEBYSHEV_DISTANCE[pieceInd][whiteKingInd];
      } else if (pieceType == Piece.B_ROOK.ind) {
        long moveSet = (moveSetDb.getRookMoveSet(pos.getAllNonBlackOccupied(), pos.getAllOccupied()) & pinnedPieceMoveSetRestriction);
        blackRookAttacks |= moveSet;
        blackRookMobility += BitOperations.hammingWeight(moveSet);
      } else if (pieceType == Piece.B_BISHOP.ind) {
        long moveSet = (moveSetDb.getBishopMoveSet(pos.getAllNonBlackOccupied(), pos.getAllOccupied()) & pinnedPieceMoveSetRestriction);
        blackBishopAttacks |= moveSet;
        blackBishopMobility += BitOperations.hammingWeight(moveSet);
      } else if (pieceType == Piece.B_KNIGHT.ind) {
        long moveSet = (moveSetDb.getKnightMoveSet(pos.getAllNonBlackOccupied()) & pinnedPieceMoveSetRestriction);
        blackKnightAttacks |= moveSet;
        blackKnightMobility += BitOperations.hammingWeight(moveSet);
      } else { // B_PAWN
        long moveSet = (moveSetDb.getBlackPawnCaptureSet(pos.getAllWhiteOccupied()) & pinnedPieceMoveSetRestriction);
        blackPawnCaptures |= moveSet;
      }
      blackPieceSet = BitOperations.resetLSBit(blackPieceSet);
    }
    // Mobility scores.
    score += params.queenMobilityWeight * (whiteQueenMobility - blackQueenMobility);
    score += params.rookMobilityWeight * (whiteRookMobility - blackRookMobility);
    score += params.bishopMobilityWeight * (whiteBishopMobility - blackBishopMobility);
    score += params.knightMobilityWeight * (whiteKnightMobility - blackKnightMobility);
    // Queen-king tropism.
    score -= numOfWhiteQueens != 0 ? params.opponentQueenTropismWeight * whiteQueenKingTropism / numOfWhiteQueens : 0;
    score += numOfBlackQueens != 0 ? params.opponentQueenTropismWeight * blackQueenKingTropism / numOfBlackQueens : 0;
    // Asymmetric evaluation terms for possible captures and promotions for unquiet positions.
    // Find the most valuable immediate capture.
    int mostValuableExchange;
    if (pos.isWhitesTurn()) {
      mostValuableExchange = Math.max(0, getValueOfMostValuableRookCapture(whiteRookAttacks, pos.getBlackQueens()) - params.rookValue);
      mostValuableExchange = Math.max(mostValuableExchange, getValueOfMostValuableMinorPieceOrPawnCapture(whiteBishopAttacks,
          pos.getBlackQueens(), pos.getBlackRooks(), pos.getBlackBishops(), pos.getBlackKnights()) - params.bishopValue);
      mostValuableExchange = Math.max(mostValuableExchange, getValueOfMostValuableMinorPieceOrPawnCapture(whiteKnightAttacks,
          pos.getBlackQueens(), pos.getBlackRooks(), pos.getBlackBishops(), pos.getBlackKnights()) - params.knightValue);
      mostValuableExchange = Math.max(mostValuableExchange, getValueOfMostValuableMinorPieceOrPawnCapture(whitePawnCaptures,
          pos.getBlackQueens(), pos.getBlackRooks(), pos.getBlackBishops(), pos.getBlackKnights()) - params.pawnValue);
    } else {
      // Adjust score to side to move.
      score *= -1;
      mostValuableExchange = Math.max(0, getValueOfMostValuableRookCapture(blackRookAttacks, pos.getWhiteQueens()) - params.rookValue);
      mostValuableExchange = Math.max(mostValuableExchange, getValueOfMostValuableMinorPieceOrPawnCapture(blackBishopAttacks,
          pos.getWhiteQueens(), pos.getWhiteRooks(), pos.getWhiteBishops(), pos.getWhiteKnights()) - params.bishopValue);
      mostValuableExchange = Math.max(mostValuableExchange, getValueOfMostValuableMinorPieceOrPawnCapture(blackKnightAttacks,
          pos.getWhiteQueens(), pos.getWhiteRooks(), pos.getWhiteBishops(), pos.getWhiteKnights()) - params.knightValue);
      mostValuableExchange = Math.max(mostValuableExchange, getValueOfMostValuableMinorPieceOrPawnCapture(blackPawnCaptures,
          pos.getWhiteQueens(), pos.getWhiteRooks(), pos.getWhiteBishops(), pos.getWhiteKnights()) - params.pawnValue);
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

}
