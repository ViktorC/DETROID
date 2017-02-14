package net.viktorc.detroid.framework.uci;

import java.util.Optional;

/**
 * A simple immutable container class for the best move found in a search and the suggested ponder move for the next search both in
 * pure algebraic coordinate notation. It also has an optional score field which can hold the search score if there is one, i.e. the
 * best move was not read from an opening book.
 * 
 * @author Viktor
 */
public class SearchResults {

	private final String bestMove;
	private final Optional<String> suggestedPonderMove;
	private final Optional<Short> score;
	private final Optional<ScoreType> scoreType;
	
	/**
	 * Constructs an instance according to the specified parameters.
	 * 
	 * @param bestMove The best move in PACN.
	 * @param suggestedPonderMove The expected reply to the best move in PACN. It is optional and thus can be null.
	 * @param score An optional search score value in centipawns. If the move was found using other methods than classic search 
	 * and there is no score associated with it, the score should be null.
	 * @param scoreType An optional search score type. If the score is not present, the score type is ignored. If the score is 
	 * present, the score type cannot be null either.
	 */
	public SearchResults(String bestMove, String suggestedPonderMove, Short score, ScoreType scoreType) {
		this.bestMove = bestMove;
		this.suggestedPonderMove = Optional.ofNullable(suggestedPonderMove);
		this.score = Optional.ofNullable(score);
		this.scoreType = this.score.isPresent() ? Optional.of(scoreType) : Optional.ofNullable(null);
	}
	/**
	 * Returns a Pure Algebraic Coordinate Notation representation of the best move.
	 * 
	 * @return The best move in PACN.
	 */
	public String getBestMove() {
		return bestMove;
	}
	/**
	 * Returns an optional Pure Algebraic Coordinate Notation representation of the suggested ponder move.
	 * 
	 * @return The optional expected reply of the opponent to the best move found in PACN.
	 */
	public Optional<String> getSuggestedPonderMove() {
		return suggestedPonderMove;
	}
	/**
	 * Returns an optional 16-bit integer that may hold the score of the search results in centipawns, or if it is a mate 
	 * score, the mate distance.
	 * 
	 * @return The search score if it is available.
	 */
	public Optional<Short> getScore() {
		return score;
	}
	/**
	 * Returns the type of the score if there is a score present.
	 * 
	 * @return The type of the score.
	 */
	public Optional<ScoreType> getScoreType() {
		return scoreType;
	}
	
}
