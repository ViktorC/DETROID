package engine;



/**
 * A thread-safe table-pair for relative history heuristic implementation. It contains a history table that is only incremented upon a cutoff
 * and a butterfly table that is incremented upon every searched move no matter what. Using these two tables' respective values for the same
 * move, a relative score can be retrieved based upon the frequency of success in the past on making that move.
 * 
 * @author Viktor
 *
 */
class RelativeHistoryTable {

	private Parameters params;
	
	/**The maximum score a move can have. */
	public final short MAX_SCORE;
	
	private int[][] historyT;	// A [piece][destination square] table for the history heuristic.
	private int[][] butterflyT;	// A [piece][destination square] table for the butterfly heuristic.
	
	private Object[][] locks;	// Lock objects for synchronization.
	
	public RelativeHistoryTable(Parameters params) {
		this.params = params;
		MAX_SCORE = (short)(2*(params.QUEEN_VALUE - params.PAWN_VALUE));
		/* The numbering of the pieces starts from one, so each table has a redundant first row to save
		 * the expenses of always subtracting one from the moved piece numeral both on read and write. */
		historyT = new int[13][64];
		butterflyT = new int[13][64];
		locks = new Object[13][64];
		for (int i = 0; i < locks.length; i++) {
			for (int j = 0; j < locks[i].length; j++)
				locks[i][j] = new Object();
		}
	}
	/**
	 * If a move causes a cut-off, this method updates the relative history table accordingly.
	 * 
	 * @param m The move that caused the cut-off.
	 */
	public void recordSuccessfulMove(Move m) {
		synchronized(locks[m.movedPiece][m.to]) {
			historyT[m.movedPiece][m.to] += MAX_SCORE;
			butterflyT[m.movedPiece][m.to]++;
		}
	}
	/**
	 * If a move does not cause a cut-off, this method updates the relative history table accordingly.
	 * 
	 * @param m The move that did not cause a cut-off.
	 */
	public void recordUnsuccessfulMove(Move m) {
		synchronized(locks[m.movedPiece][m.to]) {
			butterflyT[m.movedPiece][m.to]++;
		}
	}
	/**
	 * Decrements the current values in the tables by a certain factor for when a new search is started allowing for more significance associated
	 * with the new values than with the old ones.
	 */
	public void decrementCurrentValues() {
		for (int i = 1; i < historyT.length; i++) {
			for (int j = 0; j < 64; j++) {
				synchronized(locks[i][j]) {
					historyT[i][j] /= params.RHT_DECREMENT_FACTOR;
					butterflyT[i][j] /= params.RHT_DECREMENT_FACTOR;
				}
			}
		}
	}
	/**
	 * Returns the relative history heuristic score for the move parameter.
	 * 
	 * @param m The move to be scored.
	 * @return The relative history heuristic score for the move according to the cut-off to occurence ratio of the associated entries in the
	 * from-to tables.
	 */
	public short score(Move m) {
		int bTscore;
		synchronized(locks[m.movedPiece][m.to]) {
			bTscore = butterflyT[m.movedPiece][m.to];
			if (bTscore != 0)
				return (short)(historyT[m.movedPiece][m.to]/bTscore);
			else
				return 0;
		}
	}
}
