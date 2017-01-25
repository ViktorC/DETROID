package net.viktorc.detroid.framework.uci;

import java.util.Observable;

/**
 * An observable abstract class for containing search information and notifying the observers on changes.
 * 
 * @author Viktor
 *
 */
public abstract class SearchInformation extends Observable {

	/**
	 * Returns the index of the PV line. If multi-PV is not supported, it should always return 0. If it is supported, the numbering 
	 * should start from 1 with the best line.
	 * 
	 * @return
	 */
	public abstract int getPvNumber();
	/**
	 * Returns an array of the principal variation with the moves in pure algebraic coordinate notation.
	 * 
	 * @return
	 */
	public abstract String[] getPv();
	/**
	 * Returns the currently searched root move in pure algebraic coordinate notation.
	 * 
	 * @return
	 */
	public abstract String getCurrentMove();
	/**
	 * Returns the number of the currently searched move in the move list of the root position.
	 * 
	 * @return
	 */
	public abstract int getCurrentMoveNumber();
	/**
	 * Returns the greatest nominal depth of the search.
	 * 
	 * @return
	 */
	public abstract short getDepth();
	/**
	 * Returns the result score of the search for the side to move.
	 * 
	 * @return
	 */
	public abstract short getScore();
	/**
	 * Returns whether it is an exact score, a lower bound, an upper bound, or a mate score, in which case the score denotes the mate
	 * distance in half moves. If the side to move in the root position is going to get mated, the negative distance is returned.
	 * 
	 * @return
	 */
	public abstract ScoreType getScoreType();
	/**
	 * Returns the number of nodes searched to reach this result.
	 * 
	 * @return
	 */
	public abstract long getNodes();
	/**
	 * Returns the time spent on the search to reach this result in milliseconds.
	 * 
	 * @return
	 */
	public abstract long getTime();
	
}