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
	 * @return The index of the PV line.
	 */
	public abstract int getPvNumber();
	/**
	 * Returns an array of the principal variation with the moves in Pure Algebraic Coordinate Notation.
	 * 
	 * @return An array of the moves in PACN that make up the PV.
	 */
	public abstract String[] getPv();
	/**
	 * Returns the currently searched root move in Pure Algebraic Coordinate Notation.
	 * 
	 * @return The currently search root move in PACN.
	 */
	public abstract String getCurrentMove();
	/**
	 * Returns the number of the currently searched move in the move list of the root position.
	 * 
	 * @return The current move index in the list of legal moves.
	 */
	public abstract int getCurrentMoveNumber();
	/**
	 * Returns the nominal depth of the search.
	 * 
	 * @return The nominal depth of the search.
	 */
	public abstract short getDepth();
	/**
	 * Returns the greatest depth of the search.
	 * 
	 * @return The greatest depth of the search.
	 */
	public abstract short getSelectiveDepth();
	/**
	 * Returns the result score of the search for the side to move.
	 * 
	 * @return The search score in centipawns or if it is a mate score, the mate distance.
	 */
	public abstract short getScore();
	/**
	 * Returns whether it is an exact score, a lower bound, an upper bound, or a mate score, in which case the score denotes the mate
	 * distance in half moves. If the side to move in the root position is going to get mated, the negative distance is returned.
	 * 
	 * @return The type of the returned score.
	 */
	public abstract ScoreType getScoreType();
	/**
	 * Returns the number of nodes searched to reach this result.
	 * 
	 * @return The number of nodes traversed.
	 */
	public abstract long getNodes();
	/**
	 * Returns the time spent on the search to reach this result in milliseconds.
	 * 
	 * @return The time spent searching in milliseconds.
	 */
	public abstract long getTime();
	
}