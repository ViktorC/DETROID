package net.viktorc.detroid.framework.engine;

import java.io.IOException;
import java.util.Map;
import net.viktorc.detroid.framework.engine.Bitboard.Square;
import net.viktorc.detroid.framework.util.BitOperations;

/**
 * An enum of pre-calculated move set databases for each square of a chess board for saving the time costs of calculating move sets on the
 * fly at the price of about 850KB. Each instance contains a so called 'fancy magic move tablebase' for sliding pieces and pre-calculated
 * move sets for all other pieces as well.
 *
 * @author Viktor
 */
public enum MoveSetBase {

  A1, B1, C1, D1, E1, F1, G1, H1,
  A2, B2, C2, D2, E2, F2, G2, H2,
  A3, B3, C3, D3, E3, F3, G3, H3,
  A4, B4, C4, D4, E4, F4, G4, H4,
  A5, B5, C5, D5, E5, F5, G5, H5,
  A6, B6, C6, D6, E6, F6, G6, H6,
  A7, B7, C7, D7, E7, F7, G7, H7,
  A8, B8, C8, D8, E8, F8, G8, H8;

  public final long kingMoveMask;
  public final long knightMoveMask;
  public final long whitePawnAdvanceMoveMask;
  public final long blackPawnAdvanceMoveMask;
  public final long whitePawnCaptureMoveMask;
  public final long blackPawnCaptureMoveMask;
  public final long rookOccupancyMask;
  public final long bishopOccupancyMask;
  private final byte rookMagicShift;
  private final byte bishopMagicShift;
  private final long rookMagicNumber;
  private final long bishopMagicNumber;
  private final long[] rookMoveSets;
  private final long[] bishopMoveSets;

  MoveSetBase() {
    int sqrInd = ordinal();
    long bit = Square.values()[sqrInd].bitboard;
    kingMoveMask = Bitboard.computeKingMoveSets(bit, Bitboard.FULL_BOARD);
    knightMoveMask = Bitboard.computeKnightMoveSets(bit, Bitboard.FULL_BOARD);
    whitePawnAdvanceMoveMask = Bitboard.computeWhitePawnAdvanceSets(bit, Bitboard.FULL_BOARD);
    blackPawnAdvanceMoveMask = Bitboard.computeBlackPawnAdvanceSets(bit, Bitboard.FULL_BOARD);
    whitePawnCaptureMoveMask = Bitboard.computeWhitePawnCaptureSets(bit, Bitboard.FULL_BOARD);
    blackPawnCaptureMoveMask = Bitboard.computeBlackPawnCaptureSets(bit, Bitboard.FULL_BOARD);
    rookOccupancyMask = Bitboard.computeRookOccupancyMasks(bit);
    bishopOccupancyMask = Bitboard.computeBishopOccupancyMasks(bit);
    MagicsConfig magicsConfig = MagicsConfig.getInstance();
    try {
      magicsConfig.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Map.Entry<Long, Byte> rookMagics = magicsConfig.getRookMagics(sqrInd);
    Map.Entry<Long, Byte> bishopMagics = magicsConfig.getBishopMagics(sqrInd);
    rookMagicNumber = rookMagics.getKey();
    rookMagicShift = rookMagics.getValue();
    bishopMagicNumber = bishopMagics.getKey();
    bishopMagicShift = bishopMagics.getValue();
    rookMoveSets = new long[1 << (64 - rookMagicShift)];
    bishopMoveSets = new long[1 << (64 - bishopMagicShift)];
    long[] rookOccupancyVariations = BitOperations.getAllSubsets(rookOccupancyMask);
    long[] bishopOccupancyVariations = BitOperations.getAllSubsets(bishopOccupancyMask);
    long[] rookMoveSetVariations = Bitboard.computeRookMoveSetVariations(bit, rookOccupancyVariations);
    long[] bishopMoveSetVariations = Bitboard.computeBishopMoveSetVariations(bit, bishopOccupancyVariations);
    for (int i = 0; i < rookOccupancyVariations.length; i++) {
      int index = (int) ((rookOccupancyVariations[i] * rookMagicNumber) >>> rookMagicShift);
      rookMoveSets[index] = rookMoveSetVariations[i];
    }
    for (int i = 0; i < bishopOccupancyVariations.length; i++) {
      int index = (int) ((bishopOccupancyVariations[i] * bishopMagicNumber) >>> bishopMagicShift);
      bishopMoveSets[index] = bishopMoveSetVariations[i];
    }
  }

  /**
   * @return A simple queen move mask, i.e. the file, rank, diagonal, and anti-diagonal that cross each other on the square indexed by this
   * enum instance.
   */
  public long getQueenMoveMask() {
    return rookMoveSets[0] | bishopMoveSets[0];
  }

  /**
   * @return A simple rook move mask, i.e. the file and rank that cross each other on the square indexed by this enum instance.
   */
  public long getRookMoveMask() {
    return rookMoveSets[0];
  }

  /**
   * @return A simple bishop move mask, i.e. the diagonal and anti-diagonal that cross each other on the square indexed by this enum
   * instance.
   */
  public long getBishopMoveMask() {
    return bishopMoveSets[0];
  }

  /**
   * @param allNonSameColorOccupied All squares not occupied by pieces of the same color as the king.
   * @return A king's pseudo-legal move set.
   */
  public long getKingMoveSet(long allNonSameColorOccupied) {
    return kingMoveMask & allNonSameColorOccupied;
  }

  /**
   * @param allNonSameColorOccupied All squares not occupied by pieces of the same color as the queen.
   * @param allOccupied All squared occupied by any piece.
   * @return A queen's pseudo-legal move set given the occupancies fed to the method.
   */
  public long getQueenMoveSet(long allNonSameColorOccupied, long allOccupied) {
    return (rookMoveSets[(int) (((rookOccupancyMask & allOccupied) * rookMagicNumber) >>> rookMagicShift)] |
        bishopMoveSets[(int) (((bishopOccupancyMask & allOccupied) * bishopMagicNumber) >>> bishopMagicShift)]) &
        allNonSameColorOccupied;
  }

  /**
   * @param allNonSameColorOccupied All squares not occupied by pieces of the same color as the rook.
   * @param allOccupied All squared occupied by any piece.
   * @return A rook's pseudo-legal move set given the occupancies fed to the method.
   */
  public long getRookMoveSet(long allNonSameColorOccupied, long allOccupied) {
    return rookMoveSets[(int) (((rookOccupancyMask & allOccupied) * rookMagicNumber) >>> rookMagicShift)] &
        allNonSameColorOccupied;
  }

  /**
   * @param allNonSameColorOccupied All squares not occupied by pieces of the same color as the bishop.
   * @param allOccupied All squared occupied by any piece.
   * @return A bishop's pseudo-legal move set given the occupancies fed to the method.
   */
  public long getBishopMoveSet(long allNonSameColorOccupied, long allOccupied) {
    return bishopMoveSets[(int) (((bishopOccupancyMask & allOccupied) * bishopMagicNumber) >>> bishopMagicShift)] &
        allNonSameColorOccupied;
  }

  /**
   * @param allNonSameColorOccupied All squares not occupied by pieces of the same color as the knight.
   * @return A knight's pseudo-legal move set.
   */
  public long getKnightMoveSet(long allNonSameColorOccupied) {
    return knightMoveMask & allNonSameColorOccupied;
  }

  /**
   * @param allBlackOccupied All squares occupied by black pieces.
   * @return A white pawn's pseudo-legal attack set.
   */
  public long getWhitePawnCaptureSet(long allBlackOccupied) {
    return whitePawnCaptureMoveMask & allBlackOccupied;
  }

  /**
   * @param allWhiteOccupied All squares occupied by white pieces.
   * @return A black pawn's pseudo-legal attack set.
   */
  public long getBlackPawnCaptureSet(long allWhiteOccupied) {
    return blackPawnCaptureMoveMask & allWhiteOccupied;
  }

  /**
   * @param allEmpty All empty squares.
   * @return A white pawn's pseudo-legal quiet move set.
   */
  public long getWhitePawnAdvanceSet(long allEmpty) {
    long adv = whitePawnAdvanceMoveMask & allEmpty;
    if (ordinal() < 16) {
      adv |= Bitboard.computeWhitePawnAdvanceSets(adv, allEmpty);
    }
    return adv;
  }

  /**
   * @param allEmpty All empty squares.
   * @return A black pawn's pseudo-legal quiet move set.
   */
  public long getBlackPawnAdvanceSet(long allEmpty) {
    long adv = blackPawnAdvanceMoveMask & allEmpty;
    if (ordinal() > 47) {
      adv |= Bitboard.computeBlackPawnAdvanceSets(adv, allEmpty);
    }
    return adv;
  }

  /**
   * @param allBlackOccupied All squares occupied by black pieces.
   * @param allEmpty All empty squares.
   * @return A white pawn's pseudo-legal complete move set.
   */
  public long getWhitePawnMoveSet(long allBlackOccupied, long allEmpty) {
    return getWhitePawnAdvanceSet(allEmpty) | getWhitePawnCaptureSet(allBlackOccupied);
  }

  /**
   * @param allWhiteOccupied All squares occupied by white pieces.
   * @param allEmpty All empty squares.
   * @return A black pawn's pseudo-legal complete move set.
   */
  public long getBlackPawnMoveSet(long allWhiteOccupied, long allEmpty) {
    return getBlackPawnAdvanceSet(allEmpty) | getBlackPawnCaptureSet(allWhiteOccupied);
  }

  /**
   * @param sqrInd The index of the origin square.
   * @return The move set database for the specified origin square.
   */
  public static MoveSetBase getByIndex(int sqrInd) {
    switch (sqrInd) {
      case 0:
        return A1;
      case 1:
        return B1;
      case 2:
        return C1;
      case 3:
        return D1;
      case 4:
        return E1;
      case 5:
        return F1;
      case 6:
        return G1;
      case 7:
        return H1;
      case 8:
        return A2;
      case 9:
        return B2;
      case 10:
        return C2;
      case 11:
        return D2;
      case 12:
        return E2;
      case 13:
        return F2;
      case 14:
        return G2;
      case 15:
        return H2;
      case 16:
        return A3;
      case 17:
        return B3;
      case 18:
        return C3;
      case 19:
        return D3;
      case 20:
        return E3;
      case 21:
        return F3;
      case 22:
        return G3;
      case 23:
        return H3;
      case 24:
        return A4;
      case 25:
        return B4;
      case 26:
        return C4;
      case 27:
        return D4;
      case 28:
        return E4;
      case 29:
        return F4;
      case 30:
        return G4;
      case 31:
        return H4;
      case 32:
        return A5;
      case 33:
        return B5;
      case 34:
        return C5;
      case 35:
        return D5;
      case 36:
        return E5;
      case 37:
        return F5;
      case 38:
        return G5;
      case 39:
        return H5;
      case 40:
        return A6;
      case 41:
        return B6;
      case 42:
        return C6;
      case 43:
        return D6;
      case 44:
        return E6;
      case 45:
        return F6;
      case 46:
        return G6;
      case 47:
        return H6;
      case 48:
        return A7;
      case 49:
        return B7;
      case 50:
        return C7;
      case 51:
        return D7;
      case 52:
        return E7;
      case 53:
        return F7;
      case 54:
        return G7;
      case 55:
        return H7;
      case 56:
        return A8;
      case 57:
        return B8;
      case 58:
        return C8;
      case 59:
        return D8;
      case 60:
        return E8;
      case 61:
        return F8;
      case 62:
        return G8;
      case 63:
        return H8;
      default:
        throw new IllegalArgumentException("Invalid square index.");
    }
  }

}
