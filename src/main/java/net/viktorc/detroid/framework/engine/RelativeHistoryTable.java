package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.engine.Bitboard.Square;

/**
 * A thread-safe table-pair for relative history heuristic implementation. It contains a history table that is only incremented upon a
 * cutoff and a butterfly table that is incremented upon every searched move no matter what. Using these two tables' respective values for
 * the same move, a relative score can be retrieved based upon the frequency of success in the past on making that move.
 *
 * @author Viktor
 */
public class RelativeHistoryTable {

  /**
   * The maximum score a move can have.
   */
  private final short maxScore;

  private final long[][] historyT;  // A [piece][destination square] table for the history heuristic.
  private final long[][] butterflyT;  // A [piece][destination square] table for the butterfly heuristic.

  /**
   * Default constructor.
   */
  public RelativeHistoryTable() {
    maxScore = Short.MAX_VALUE;
    /* The numbering of the pieces starts from one, so each table has a redundant first row to save
     * the expenses of always subtracting one from the moved piece numeral both on read and write. */
    historyT = new long[Piece.values().length][Square.values().length];
    butterflyT = new long[Piece.values().length][Square.values().length];
  }

  /**
   * If a move causes a cut-off, this method updates the relative history table accordingly.
   *
   * @param m The move that caused the cut-off.
   */
  public void recordSuccessfulMove(Move m) {
    byte movedPiece = m.movedPiece;
    byte to = m.to;
    historyT[movedPiece][to]++;
    butterflyT[movedPiece][to]++;
  }

  /**
   * If a move does not cause a cut-off, this method updates the relative history table accordingly.
   *
   * @param m The move that did not cause a cut-off.
   */
  public void recordUnsuccessfulMove(Move m) {
    butterflyT[m.movedPiece][m.to]++;
  }

  /**
   * Returns the relative history heuristic score for the move parameter.
   *
   * @param m The move to be scored.
   * @return The relative history heuristic score for the move according to the cut-off to occurence ratio of the associated entries in the
   * from-to tables.
   */
  public short score(Move m) {
    byte movedPiece = m.movedPiece;
    byte to = m.to;
    long bTscore = butterflyT[movedPiece][to];
    return bTscore != 0 ? (short) (maxScore * historyT[movedPiece][to] / bTscore) : 0;
  }

}
