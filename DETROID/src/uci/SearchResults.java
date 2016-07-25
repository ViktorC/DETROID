package uci;

/**
 * A simple immutable container class for the best move found in a search and the suggested ponder move for the next search both in
 * pure algebraic coordinate notation.
 * 
 * @author Viktor
 */
public class SearchResults {

	private final String bestMove;
	private final String suggestedPonderMove;
	
	public SearchResults(String bestMove, String suggestedPonderMove) {
		this.bestMove = bestMove;
		this.suggestedPonderMove = suggestedPonderMove;
	}
	public String getBestMove() {
		return bestMove;
	}
	public String getSuggestedPonderMove() {
		return suggestedPonderMove;
	}
}
