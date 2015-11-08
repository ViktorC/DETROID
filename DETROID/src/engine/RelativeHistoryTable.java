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
	
	private int[][] historyT;	// An [origin square][destination square] table for the history heuristic.
	private int[][] butterflyT;	// An [origin square][destination square] table for the butterfly heuristic.
	
	public RelativeHistoryTable() {
		historyT = new int[64][64];
		butterflyT = new int[64][64];
	}
	/**If a move causes a cut-off, this method updates the relative history table accordingly.
	 * 
	 * @param m The move that caused the cut-off.
	 */
	public void recordSuccessfulMove(Move m) {
		int[] row;
		synchronized (row = historyT[m.from]) {
			row[m.to] += MAX_SCORE;
		}
		synchronized (row = butterflyT[m.from]) {
			row[m.to]++;
		}
	}
	/**If a move does not cause a cut-off, this method updates the relative history table accordingly.
	 * 
	 * @param m The move that did not cause a cut-off.
	 */
	public void recordUnsuccessfulMove(Move m) {
		int[] row = butterflyT[m.from];
		synchronized (row) {
			row[m.to]++;
		}
	}
	/**Decrements the current values in the tables by a certain factor for when a new search is started allowing for more significance associated with
	 * the new values than with the old ones.*/
	public synchronized void decrementCurrentValues() {
		for (int i = 0; i < 64; i++) {
			for (int j = 0; j < 64; j++) {
				historyT[i][j] /= DECREMENT_FACTOR;
				butterflyT[i][j] /= DECREMENT_FACTOR;
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
		int bTscore = butterflyT[m.from][m.to];
		if (bTscore != 0)
			return historyT[m.from][m.to]/bTscore;
		else
			return 0;
	}
}
