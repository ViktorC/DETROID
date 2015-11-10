package engine;

import engine.Evaluator.MaterialScore;

/**A thread-safe table-pair for relative history heuristic implementation. It contains a history table that is only incremented upon a cutoff and a
 * butterfly table that is incremented upon every searched move no matter what. Using these two tables' respective values for the same move, a
 * relative score can be retrieved based upon the frequency of success in the past on making that move.
 * 
 * @author Viktor
 *
 */
public class RelativeHistoryTable {

	public final static int MAX_SCORE = 2*(MaterialScore.QUEEN.value - MaterialScore.PAWN.value);
	private final static int DECREMENT_FACTOR = 4;
	
	private int[][] historyT;	// A [piece][destination square] table for the history heuristic.
	private int[][] butterflyT;	// A [piece][destination square] table for the butterfly heuristic.
	
	public RelativeHistoryTable() {
		/* The numbering of the pieces starts from one, so each table has a redundant first row to save
		 * the expenses of always subtracting one from the moved piece numeral both on read and write. */
		int numOfPieces = Piece.values().length;
		historyT = new int[numOfPieces][64];
		butterflyT = new int[numOfPieces][64];
	}
	/**If a move causes a cut-off, this method updates the relative history table accordingly.
	 * 
	 * @param m The move that caused the cut-off.
	 */
	public void recordSuccessfulMove(Move m) {
		int[] bRow = butterflyT[m.movedPiece];
		synchronized(bRow) {
			historyT[m.movedPiece][m.to] += MAX_SCORE;
			bRow[m.to]++;
		}
	}
	/**If a move does not cause a cut-off, this method updates the relative history table accordingly.
	 * 
	 * @param m The move that did not cause a cut-off.
	 */
	public void recordUnsuccessfulMove(Move m) {
		int[] bRow = butterflyT[m.movedPiece];
		synchronized(bRow) {
			bRow[m.to]++;
		}
	}
	/**Decrements the current values in the tables by a certain factor for when a new search is started allowing for more significance associated with
	 * the new values than with the old ones.*/
	public void decrementCurrentValues() {
		int[] bRow;
		for (int i = 1; i < historyT.length; i++) {
			bRow = butterflyT[i];
			synchronized(bRow) {
				for (int j = 0; j < 64; j++) {
					historyT[i][j] /= DECREMENT_FACTOR;
					bRow[j] /= DECREMENT_FACTOR;
				}
			}
		}
	}
	/**Returns the relative history heuristic score for the move parameter.
	 * 
	 * @param m The move to be scored.
	 * @return The relative history heuristic score for the move according to the cut-off to occurence ratio of the associated entries in the from-to
	 * tables.
	 */
	public int score(Move m) {
		int bTscore;
		int[] bRow = butterflyT[m.movedPiece];
		synchronized(bRow) {
			bTscore = bRow[m.to];
			if (bTscore != 0)
				return historyT[m.movedPiece][m.to]/bTscore;
			else
				return 0;
		}
	}
}
