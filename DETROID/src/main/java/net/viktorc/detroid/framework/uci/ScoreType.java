package main.java.net.viktorc.detroid.framework.uci;

/**
 * An enum for different score types that a search can return.
 * 
 * @author Viktor
 *
 */
public enum ScoreType {

	/**
	 * PV-node score; Knuth's type 1.
	 */
	EXACT,
	/**
	 * Cut-node score; Knuth's type 2.
	 */
	UPPER_BOUND,
	/**
	 * All-node score; Knuth's type 3.
	 */
	LOWER_BOUND,
	/**
	 * Check mate score.
	 */
	MATE;
	
}