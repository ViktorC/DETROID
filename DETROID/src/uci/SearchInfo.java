package uci;

import java.util.Collection;
import java.util.Observable;

public abstract class SearchInfo extends Observable {

	/**
	 * Returns a collection of the prinicipal variation with the moves in pure algebraic coordinate notation.
	 * 
	 * @return
	 */
	public abstract Collection<String> getPv();
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