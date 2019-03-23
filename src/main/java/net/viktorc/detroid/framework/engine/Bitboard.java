package net.viktorc.detroid.framework.engine;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import net.viktorc.detroid.framework.util.BitOperations;

/**
 * A class that provides utility functions for bitboard based move set generation and groups together board entity enums such as squares,
 * files, ranks, and diagonals.
 *
 * It can be used to compute move set bitboards for multiple pieces at a time or to generate 'magic' numbers and 'magic' shift values for
 * hashing occupancy variations onto an index in a pre-calculated sliding piece move database by multiplying the intersection of the
 * occupancy bitmap and the occupancy mask by the magic number, then right shifting the product by the magic shift value specific to the
 * given square (calculated by subtracting the number of set bits in the relevant occupancy mask from 64).
 *
 * With better magics, even smaller tablebases can be produced. Some magic numbers can be multiplied by the occupancy bitmap and shifted to
 * the right by one more than the usual number of bits and they will still hash on the right index because of the existence of different
 * occupancy variations that allow for the same move sets. All one has to do is try generating magics with the usual shift value plus one
 * for the squares for which better magics are to be found.
 *
 * @author Viktor
 */
public class Bitboard {

  /**
   * A bitboard with all the 64 bits set.
   */
  public static final long FULL_BOARD = -1L;
  /**
   * A bitboard with none of the bits set.
   */
  public static final long EMPTY_BOARD = 0L;

  private static final long[][] LINE_SEGMENTS;

  static {
    LINE_SEGMENTS = new long[64][64];
    for (int origin = 0; origin < 64; origin++) {
      Rays originRays = Rays.values()[origin];
      for (int target = 0; target < 64; target++) {
        Rays targetRays = Rays.values()[target];
        long line = (originRays.rankPos & targetRays.rankNeg);
        line |= (originRays.rankNeg & targetRays.rankPos);
        line |= (originRays.diagonalPos & targetRays.diagonalNeg);
        line |= (originRays.diagonalNeg & targetRays.diagonalPos);
        line |= (originRays.filePos & targetRays.fileNeg);
        line |= (originRays.fileNeg & targetRays.filePos);
        line |= (originRays.antiDiagonalPos & targetRays.antiDiagonalNeg);
        line |= (originRays.antiDiagonalNeg & targetRays.antiDiagonalPos);
        LINE_SEGMENTS[origin][target] = line;
      }
    }
  }

  private Bitboard() {
  }

  /**
   * @param fromSqrInd The origin square's index.
   * @param toSqrInd The destination square's index.
   * @return A bitboard of straight line, if one exists, between an origin square and a target square. If the two squares do not fall on the
   * same rank, file, diagonal, or anti-diagonal, it returns 0.
   */
  public static long getLineSegment(int fromSqrInd, int toSqrInd) {
    return LINE_SEGMENTS[fromSqrInd][toSqrInd];
  }

  /**
   * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction north of multiple sliding pieces at the
   * same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares.
   *
   * @param generator Piece squares.
   * @param propagator All empty squares.
   * @return The filled
   */
  public static long fillNorth(long generator, long propagator) {
    generator |= (generator << 8) & propagator;
    propagator &= (propagator << 8);
    generator |= (generator << 16) & propagator;
    propagator &= (propagator << 16);
    generator |= (generator << 32) & propagator;
    return generator;
  }

  /**
   * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction south of multiple sliding pieces at the
   * same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares.
   *
   * @param generator Piece squares.
   * @param propagator All empty squares.
   * @return The filled
   */
  public static long fillSouth(long generator, long propagator) {
    generator |= (generator >>> 8) & propagator;
    propagator &= (propagator >>> 8);
    generator |= (generator >>> 16) & propagator;
    propagator &= (propagator >>> 16);
    generator |= (generator >>> 32) & propagator;
    return generator;
  }

  /**
   * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction west of multiple sliding pieces at the
   * same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around
   * effect is handled by the method.
   *
   * @param generator Piece squares.
   * @param propagator All empty squares.
   * @return The filled
   */
  public static long fillWest(long generator, long propagator) {
    propagator &= 0x7F7F7F7F7F7F7F7FL;
    generator |= (generator >>> 1) & propagator;
    propagator &= (propagator >>> 1);
    generator |= (generator >>> 2) & propagator;
    propagator &= (propagator >>> 2);
    generator |= (generator >>> 4) & propagator;
    return generator;
  }

  /**
   * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction east of multiple sliding pieces at the
   * same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap around
   * effect is handled by the method.
   *
   * @param generator Piece squares.
   * @param propagator All empty squares.
   * @return The filled
   */
  public static long fillEast(long generator, long propagator) {
    propagator &= 0xFEFEFEFEFEFEFEFEL;
    generator |= (generator << 1) & propagator;
    propagator &= (propagator << 1);
    generator |= (generator << 2) & propagator;
    propagator &= (propagator << 2);
    generator |= (generator << 4) & propagator;
    return generator;
  }

  /**
   * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction north-west of multiple sliding pieces at
   * the same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap
   * around effect is handled by the method.
   *
   * @param generator Piece squares.
   * @param propagator All empty squares.
   * @return The filled
   */
  public static long fillNorthWest(long generator, long propagator) {
    propagator &= 0x7F7F7F7F7F7F7F7FL;
    generator |= (generator << 7) & propagator;
    propagator &= (propagator << 7);
    generator |= (generator << 14) & propagator;
    propagator &= (propagator << 14);
    generator |= (generator << 28) & propagator;
    return generator;
  }

  /**
   * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction north-east of multiple sliding pieces at
   * the same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap
   * around effect is handled by the method.
   *
   * @param generator Piece squares.
   * @param propagator All empty squares.
   * @return The filled
   */
  public static long fillNorthEast(long generator, long propagator) {
    propagator &= 0xFEFEFEFEFEFEFEFEL;
    generator |= (generator << 9) & propagator;
    propagator &= (propagator << 9);
    generator |= (generator << 18) & propagator;
    propagator &= (propagator << 18);
    generator |= (generator << 36) & propagator;
    return generator;
  }

  /**
   * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction south-west of multiple sliding pieces at
   * the same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap
   * around effect is handled by the method.
   *
   * @param generator Piece squares.
   * @param propagator All empty squares.
   * @return The filled
   */
  public static long fillSouthWest(long generator, long propagator) {
    propagator &= 0x7F7F7F7F7F7F7F7FL;
    generator |= (generator >>> 9) & propagator;
    propagator &= (propagator >>> 9);
    generator |= (generator >>> 18) & propagator;
    propagator &= (propagator >>> 18);
    generator |= (generator >>> 36) & propagator;
    return generator;
  }

  /**
   * A parallel prefix occluded fill algorithm that returns the move (non-attack) sets in direction south-east of multiple sliding pieces at
   * the same time. The generator is usually the set of pieces to be shifted, and the propagator is the set of empty squares. The wrap
   * around effect is handled by the method.
   *
   * @param generator Piece squares.
   * @param propagator All empty squares.
   * @return The filled
   */
  public static long fillSouthEast(long generator, long propagator) {
    propagator &= 0xFEFEFEFEFEFEFEFEL;
    generator |= (generator >>> 7) & propagator;
    propagator &= (propagator >>> 7);
    generator |= (generator >>> 14) & propagator;
    propagator &= (propagator >>> 14);
    generator |= (generator >>> 28) & propagator;
    return generator;
  }

  /**
   * Generates a bitboard of the basic king's move set. Does not include target squares of castling; handles the wrap-around effect.
   *
   * @param king The biboard for the king.
   * @param allNonSameColorOccupied The bitboard for all squares non occupied by the same color as the king.
   * @return The bitboard for the king's move set.
   */
  public static long computeKingMoveSets(long king, long allNonSameColorOccupied) {
    return ((((king << 7) | (king >>> 9) | (king >>> 1)) & ~File.H.bitboard) |
        (king << 8) | (king >>> 8) |
        (((king << 9) | (king >>> 7) | (king << 1)) & ~File.A.bitboard)) & allNonSameColorOccupied;
  }

  /**
   * Generates a bitboard of the basic knight's move set. Occupancies are disregarded. It handles the wrap-around effect.
   *
   * @param knight The bitboard for the knights.
   * @param allNonSameColorOccupied The bitboard for all squares non occupied by the same color as the knights.
   * @return The bitboard for the knights' move sets.
   */
  public static long computeKnightMoveSets(long knight, long allNonSameColorOccupied) {
    return ((((knight << 15) | (knight >>> 17)) & ~File.H.bitboard) |
        (((knight << 6) | (knight >>> 10)) & ~(File.H.bitboard | File.G.bitboard)) |
        (((knight << 10) | (knight >>> 6)) & ~(File.A.bitboard | File.B.bitboard)) |
        (((knight << 17) | (knight >>> 15)) & ~File.A.bitboard)) & allNonSameColorOccupied;
  }

  /**
   * Generates a bitboard of the basic white pawn's capture set. Occupancies are disregarded. It handles the wrap-around effect.
   *
   * @param whitePawns The bitboard for the white pawns.
   * @param allBlackOccupied The bitboard for all squares occupied by black pieces.
   * @return The bitboard for the capture move sets of the pawns.
   */
  public static long computeWhitePawnCaptureSets(long whitePawns, long allBlackOccupied) {
    return (((whitePawns << 7) & ~File.H.bitboard) | ((whitePawns << 9) & ~File.A.bitboard)) & allBlackOccupied;
  }

  /**
   * Generates a bitboard of the basic black pawn's capture set. Occupancies are disregarded. It handles the wrap-around effect.
   *
   * @param blackPawns The bitboard for the black pawns.
   * @param allWhiteOccupied The bitboard for all squares occupied by white pieces.
   * @return The bitboard for the capture move sets of the pawns.
   */
  public static long computeBlackPawnCaptureSets(long blackPawns, long allWhiteOccupied) {
    return (((blackPawns >>> 9) & ~File.H.bitboard) | ((blackPawns >>> 7) & ~File.A.bitboard)) & allWhiteOccupied;
  }

  /**
   * Generates a bitboard of the basic white pawn's advance set. Double advance from initial square is included. Occupancies are
   * disregarded. It handles the wrap-around effect.
   *
   * @param whitePawns The bitboard for the white pawns.
   * @param allEmpty A bitboard for all the empty squares.
   * @return The bitboard for the advance move sets of the pawns.
   */
  public static long computeWhitePawnAdvanceSets(long whitePawns, long allEmpty) {
    return (whitePawns << 8) & allEmpty;
  }

  /**
   * Generates a bitboard of the basic black pawn's advance set. Double advance from initial square is included. Occupancies are
   * disregarded. It handles the wrap-around effect.
   *
   * @param blackPawns The bitboard for the black pawns.
   * @param allEmpty A bitboard for all the empty squares.
   * @return The bitboard for the advance move sets of the pawns.
   */
  public static long computeBlackPawnAdvanceSets(long blackPawns, long allEmpty) {
    return (blackPawns >>> 8) & allEmpty;
  }

  /**
   * Generates a move set bitboard for a set of bishops.
   *
   * @param bishops The bitboard for the bishops.
   * @param allOpponentOccupied The bitboard for all the sqaures occupied by the opponent.
   * @param allEmpty The bitboard of all the empty squares.
   * @return The bitboard for the move sets of the bishops.
   */
  public static long computeBishopMoveSets(long bishops, long allOpponentOccupied, long allEmpty) {
    long gen = fillNorthWest(bishops, allEmpty);
    long attackSet = gen | (((gen << 7) & ~File.H.bitboard) & allOpponentOccupied);
    gen = fillSouthWest(bishops, allEmpty);
    attackSet |= gen | (((gen >>> 9) & ~File.H.bitboard) & allOpponentOccupied);
    gen = fillNorthEast(bishops, allEmpty);
    attackSet |= gen | (((gen << 9) & ~File.A.bitboard) & allOpponentOccupied);
    gen = fillSouthEast(bishops, allEmpty);
    attackSet |= gen | (((gen >>> 7) & ~File.A.bitboard) & allOpponentOccupied);
    return attackSet ^ bishops;
  }

  /**
   * Generates a move set bitboard for a set of rooks.
   *
   * @param rooks The bitboard for the rooks.
   * @param allOpponentOccupied The bitboard for all the sqaures occupied by the opponent.
   * @param allEmpty The bitboard of all the empty squares.
   * @return The bitboard for the move sets of the rooks.
   */
  public static long computeRookMoveSets(long rooks, long allOpponentOccupied, long allEmpty) {
    long gen = fillNorth(rooks, allEmpty);
    long attackSet = gen | ((gen << 8) & allOpponentOccupied);
    gen = fillSouth(rooks, allEmpty);
    attackSet |= gen | ((gen >>> 8) & allOpponentOccupied);
    gen = fillWest(rooks, allEmpty);
    attackSet |= gen | (((gen >>> 1) & ~File.H.bitboard) & allOpponentOccupied);
    gen = fillEast(rooks, allEmpty);
    attackSet |= gen | (((gen << 1) & ~File.A.bitboard) & allOpponentOccupied);
    return attackSet ^ rooks;
  }

  /**
   * Generates a move set bitboard for a set of queens.
   *
   * @param queens The bitboard for the queens.
   * @param allOpponentOccupied The bitboard for all the sqaures occupied by the opponent.
   * @param allEmpty The bitboard of all the empty squares.
   * @return The bitboard for the move sets of the queens.
   */
  public static long computeQueenMoveSets(long queens, long allOpponentOccupied, long allEmpty) {
    return computeRookMoveSets(queens, allOpponentOccupied, allEmpty) |
        computeBishopMoveSets(queens, allOpponentOccupied, allEmpty);
  }

  /**
   * Generates an occupancy mask bitboard for a set of bishops.
   *
   * @param bishops The bitboard for the bishops.
   * @return The occupancy mask.
   */
  public static long computeBishopOccupancyMasks(long bishops) {
    return computeBishopMoveSets(bishops, EMPTY_BOARD, FULL_BOARD) &
        ~(File.A.bitboard | File.H.bitboard | Rank.R1.bitboard | Rank.R8.bitboard);
  }

  /**
   * Generates an occupancy mask bitboard for a set of rooks.
   *
   * @param rooks The bitboard for the rooks.
   * @return The occupancy mask.
   */
  public static long computeRookOccupancyMasks(long rooks) {
    return (fillNorth(rooks, ~Rank.R8.bitboard) |
        fillSouth(rooks, ~Rank.R1.bitboard) |
        fillWest(rooks, ~File.A.bitboard) |
        fillEast(rooks, ~File.H.bitboard)) ^ rooks;
  }

  /**
   * Generates an array of bishop move set variations given an array of occupancy variations.
   *
   * @param bishops The bitboard of bishops.
   * @param occupancyVariations An array of occupancy bitboard variations for the above bishops.
   * @return The array of bishop move set variations.
   */
  public static long[] computeBishopMoveSetVariations(long bishops, long[] occupancyVariations) {
    long[] moveSetVariations = new long[occupancyVariations.length];
    for (int i = 0; i < occupancyVariations.length; i++) {
      moveSetVariations[i] = computeBishopMoveSets(bishops, FULL_BOARD, ~occupancyVariations[i]);
    }
    return moveSetVariations;
  }

  /**
   * Generates an array of rook move set variations given an array of occupancy variations.
   *
   * @param rooks The bitboard of rooks.
   * @param occupancyVariations An array of occupancy bitboard variations for the above rooks.
   * @return The array of rook move set variations.
   */
  public static long[] computeRookMoveSetVariations(long rooks, long[] occupancyVariations) {
    long[] moveSetVariations = new long[occupancyVariations.length];
    for (int i = 0; i < occupancyVariations.length; i++) {
      moveSetVariations[i] = computeRookMoveSets(rooks, FULL_BOARD, ~occupancyVariations[i]);
    }
    return moveSetVariations;
  }

  /**
   * Generates a magic number for the square specified either for a rook or for a bishop depending on the parameters. If enhanced is set
   * true, it will try to find a magic that can be right shifted by one more than the usual value allowing for denser tables lookup tables.
   *
   * @param sqrInd The origin square index.
   * @param rook Whether the magics are to be generated for a rook move set or a bishop move set.
   * @param enhanced Whether the magic number is to work with a greater magic shift value.
   * @return A magic number and magic shift value pair.
   * @throws ExecutionException If something goes wrong while generating enhanced magic numbers.
   * @throws InterruptedException If the thread is interrupted while waiting for the background threads to compute the enhanced magic
   * numbers.
   */
  public static Map.Entry<Long, Byte> generateMagics(int sqrInd, boolean rook, boolean enhanced)
      throws InterruptedException, ExecutionException {
    final Random random = new Random();
    long[] occVar, moveVar;
    long bit = Square.values()[sqrInd].bitboard;
    if (rook) {
      occVar = BitOperations.getAllSubsets(computeRookOccupancyMasks(bit));
      moveVar = computeRookMoveSetVariations(bit, occVar);
    } else {
      occVar = BitOperations.getAllSubsets(computeBishopOccupancyMasks(bit));
      moveVar = computeBishopMoveSetVariations(bit, occVar);
    }
    int variations = occVar.length;
    Function<Byte, Long> gen = s -> {
      long magicNumber;
      boolean collision;
      long[] magicDatabase = new long[variations];
      do {
        for (int i = 0; i < variations; i++) {
          magicDatabase[i] = 0;
        }
        collision = false;
        magicNumber = random.nextLong() & random.nextLong() & random.nextLong();
        for (int i = 0; i < variations; i++) {
          int index = (int) ((occVar[i] * magicNumber) >>> s);
          if (magicDatabase[index] == 0) {
            magicDatabase[index] = moveVar[i];
          } else if (magicDatabase[index] != moveVar[i]) {
            collision = true;
            break;
          }
        }
      } while (collision);
      return magicNumber;
    };
    byte shift = (byte) (64 - BitOperations.indexOfBit(variations));
    long num;
    if (enhanced) {
      int threads = Runtime.getRuntime().availableProcessors();
      CompletionService<Long> pool = new ExecutorCompletionService<>(Executors.newFixedThreadPool(threads));
      ArrayList<Future<Long>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        futures.add(pool.submit(() -> gen.apply((byte) (shift + 1))));
      }
      num = pool.take().get();
      for (Future<Long> f : futures) {
        f.cancel(true);
      }
    } else {
      num = gen.apply(shift);
    }
    return new AbstractMap.SimpleEntry<>(num, shift);
  }

  /**
   * Generates magic numbers for each square either for a rook or a bishop. For the squares whose indices are fed to this method, it will
   * attempt to find enhanced magics.
   *
   * @param rook Whether the magics are to be generated for a rook move set or a bishop move set.
   * @param enhancedSquares The indices of the squares for which enhanced magic numbers are to be computed.
   * @return A list of 64 magic number and magic shift value pairs.
   * @throws ExecutionException If something goes wrong while generating enhanced magic numbers.
   * @throws InterruptedException If the thread is interrupted while waiting for the background threads to compute the enhanced magic
   * numbers.
   */
  public static List<Map.Entry<Long, Byte>> generateAllMagics(boolean rook, int... enhancedSquares)
      throws InterruptedException, ExecutionException {
    List<Map.Entry<Long, Byte>> allMagics = new ArrayList<>(64);
    OuterLoop:
    for (int i = 0; i < 64; i++) {
      for (int sqr : enhancedSquares) {
        if (sqr == i) {
          allMagics.add(generateMagics(i, rook, true));
          continue OuterLoop;
        }
      }
      allMagics.add(generateMagics(i, rook, false));
    }
    return allMagics;
  }

  /**
   * Returns a long in binary form aligned like a chess board with one byte per row, in a human-readable way.
   *
   * @param bitboard The bitboard to represent as a string.
   * @return The string representation of the
   */
  public static String bitboardToString(long bitboard) {
    StringBuilder stringBuffer = new StringBuilder();
    String board = BitOperations.toBinaryString(bitboard);
    for (int i = 0; i < 64; i += 8) {
      for (int j = i + 7; j >= i; j--) {
        stringBuffer.append(board.charAt(j));
      }
      stringBuffer.append("\n");
    }
    stringBuffer.append("\n");
    return stringBuffer.toString();
  }

  /**
   * An enum type for the 64 squares of the chess board. Each constant has a field that contains a long with only the bitboard on the
   * respective square's index set.
   *
   * @author Viktor
   */
  public enum Square {

    A1, B1, C1, D1, E1, F1, G1, H1,
    A2, B2, C2, D2, E2, F2, G2, H2,
    A3, B3, C3, D3, E3, F3, G3, H3,
    A4, B4, C4, D4, E4, F4, G4, H4,
    A5, B5, C5, D5, E5, F5, G5, H5,
    A6, B6, C6, D6, E6, F6, G6, H6,
    A7, B7, C7, D7, E7, F7, G7, H7,
    A8, B8, C8, D8, E8, F8, G8, H8;

    public final byte ind;
    public final long bitboard;

    Square() {
      ind = (byte) ordinal();
      bitboard = BitOperations.toBit(ordinal());
    }

    /**
     * Returns a String representation of the square.
     */
    @Override
    public String toString() {
      int ind = ordinal();
      return ("" + (char) ('a' + ind % 8) + "" + (ind / 8 + 1)).toUpperCase();
    }

  }

  /**
   * An enum type for the 8 files/columns of a chess board. Each constant has a field that contains a long with only the bitboard falling on
   * the file set.
   *
   * @author Viktor
   */
  public enum File {

    A,
    B,
    C,
    D,
    E,
    F,
    G,
    H;

    public final byte ind;
    public final long bitboard;

    File() {
      ind = (byte) ordinal();
      bitboard = 0x0101010101010101L << ordinal();
    }

    /**
     * Returns the file of the chess board on which the input parameter square lies.
     *
     * @param sqrInd The index of the square.
     * @return The file on which the square the index points to is.
     */
    public static File getBySquareIndex(int sqrInd) {
      return values()[sqrInd & 7];
    }

  }

  /**
   * An enum type for the 8 ranks/rows of a chess board. Each constant has a field that contains a long with only the byte on the rank's
   * index set.
   *
   * @author Viktor
   */
  public enum Rank {

    R1,
    R2,
    R3,
    R4,
    R5,
    R6,
    R7,
    R8;

    public final byte ind;
    public final long bitboard;

    Rank() {
      ind = (byte) ordinal();
      bitboard = 0x00000000000000FFL << (8 * ordinal());
    }

    /**
     * Returns the rank of the chess board on which the input parameter square lies.
     *
     * @param sqrInd The index of the square.
     * @return The rank on which the square the index points to is.
     */
    public static Rank getBySquareIndex(int sqrInd) {
      return values()[sqrInd >>> 3];
    }

  }

  /**
   * An enum type for the 15 diagonals of a chess board. Each constant has a field that contains a long with only the bitboard on indices of
   * the squares falling on the diagonal set.
   *
   * @author Viktor
   */
  public enum Diagonal {

    DG1,
    DG2,
    DG3,
    DG4,
    DG5,
    DG6,
    DG7,
    DG8,
    DG9,
    DG10,
    DG11,
    DG12,
    DG13,
    DG14,
    DG15;

    public final byte ind;
    public final long bitboard;

    Diagonal() {
      ind = (byte) ordinal();
      long base = 0x0102040810204080L;
      int shift = 7 - ordinal();
      bitboard = shift > 0 ? base >>> 8 * shift : base << 8 * -shift;

    }

    /**
     * Returns the diagonal of the chess board on which the input parameter square lies.
     *
     * @param sqrInd The index of a square.
     * @return The diagonal on which the square the index points to is.
     */
    public static Diagonal getBySquareIndex(int sqrInd) {
      return values()[(sqrInd & 7) + (sqrInd >>> 3)];
    }

  }

  /**
   * An enum type for the 15 anti-diagonals of a chess board. Each constant has a field that contains a long with only the bitboard on
   * indices of the squares falling on the diagonal set.
   *
   * @author Viktor
   */
  public enum AntiDiagonal {

    ADG1,
    ADG2,
    ADG3,
    ADG4,
    ADG5,
    ADG6,
    ADG7,
    ADG8,
    ADG9,
    ADG10,
    ADG11,
    ADG12,
    ADG13,
    ADG14,
    ADG15;

    public final byte ind;
    public final long bitboard;

    AntiDiagonal() {
      ind = (byte) ordinal();
      long base = 0x8040201008040201L;
      int shift = 7 - ordinal();
      bitboard = shift > 0 ? base << 8 * shift : base >>> 8 * -shift;
    }

    /**
     * Returns the anti-diagonal of the chess board on which the input parameter square lies.
     *
     * @param sqrInd The index of a square.
     * @return The anti-diagonal on which the square the index points to is.
     */
    public static AntiDiagonal getBySquareIndex(int sqrInd) {
      return values()[(sqrInd & 7) + (7 - (sqrInd >>> 3))];
    }

  }

  /**
   * An enum type for all the eight different rays on the chess board for each square.
   *
   * @author Viktor
   */
  public enum Rays {

    A1, B1, C1, D1, E1, F1, G1, H1,
    A2, B2, C2, D2, E2, F2, G2, H2,
    A3, B3, C3, D3, E3, F3, G3, H3,
    A4, B4, C4, D4, E4, F4, G4, H4,
    A5, B5, C5, D5, E5, F5, G5, H5,
    A6, B6, C6, D6, E6, F6, G6, H6,
    A7, B7, C7, D7, E7, F7, G7, H7,
    A8, B8, C8, D8, E8, F8, G8, H8;

    public final byte ind;
    public final long rankPos;
    public final long rankNeg;
    public final long filePos;
    public final long fileNeg;
    public final long diagonalPos;
    public final long diagonalNeg;
    public final long antiDiagonalPos;
    public final long antiDiagonalNeg;

    Rays() {
      ind = (byte) ordinal();
      long sqrBit = Square.values()[ind].bitboard;
      Rank rank = Rank.getBySquareIndex(ind);
      File file = File.getBySquareIndex(ind);
      Diagonal diagonal = Diagonal.getBySquareIndex(ind);
      AntiDiagonal antiDiagonal = AntiDiagonal.getBySquareIndex(ind);
      rankPos = rank.bitboard & -(sqrBit << 1);
      rankNeg = rank.bitboard & (sqrBit - 1);
      filePos = file.bitboard & -(sqrBit << 1);
      fileNeg = file.bitboard & (sqrBit - 1);
      diagonalPos = diagonal.bitboard & -(sqrBit << 1);
      diagonalNeg = diagonal.bitboard & (sqrBit - 1);
      antiDiagonalPos = antiDiagonal.bitboard & -(sqrBit << 1);
      antiDiagonalNeg = antiDiagonal.bitboard & (sqrBit - 1);
    }

  }

}
