package main.java.net.viktorc.detroid.framework.uci;

/**
 * A simple immutable container class for the best move found in a search and the suggested ponder move for the next search both in
 * pure algebraic coordinate notation.
 * 
 * @author Viktor
 */
public class SearchResults {

	private final String bestMove;
	private final String suggestedPonderMove;
	
	/**
	 * Constructs an instance according to the specified parameters.
	 * 
	 * @param bestMove
	 * @param suggestedPonderMove
	 */
	public SearchResults(String bestMove, String suggestedPonderMove) {
		this.bestMove = bestMove;
		this.suggestedPonderMove = suggestedPonderMove;
	}
	/**
	 * Returns a Pure Algebraic Coordinate Notation representation of the best move.
	 * 
	 * @return
	 */
	public String getBestMove() {
		return bestMove;
	}
	/**
	 * Returns a Pure Algebraic Coordinate Notation representation of the suggested ponder move.
	 * 
	 * @return
	 */
	public String getSuggestedPonderMove() {
		return suggestedPonderMove;
	}
	
}
