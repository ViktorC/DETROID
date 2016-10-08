package tuning;

/**
 * A utility class for calculating Elo score differences based win ratios.
 * 
 * @author Viktor
 *
 */
public class Elo {

	private Elo() {
		
	}
	/**
	 * Calculates the Elo rating difference between a player and its opponent based on the match record against
	 * this opponent.
	 * 
	 * @param wins The number of wins. It has to be 0 or greater.
	 * @param draws The number of draws. It has to be 0 or greater.
	 * @param losses The number of losses. It has to be  0 or greater.
	 * @return
	 */
	public final static int calculateDifference(int wins, int draws, int losses) {
		if (wins < 0 || draws < 0 || losses < 0)
			throw new IllegalArgumentException("All parameters have to be 0 or greater.");
		double points = wins + draws*0.5;
		int games = wins + draws + losses;
		return (int) -Math.round(Math.log10((games - points)/points)*400);
	}
	/**
	 * Calculates the Elo rating difference between a player and its opponent based on the win ration against this
	 * opponent.
	 * 
	 * @param winRatio The win ratio. It has to between 0 and 1.
	 * @return
	 */
	public final static int calculateDifference(double winRatio) {
		if (winRatio < 0 || winRatio > 1)
			throw new IllegalArgumentException("The win ratio has to between 0 and 1.");
		return (int) -Math.round((Math.log((1 - winRatio)/winRatio)*400));
	}
	/**
	 * Calculates the Elo rating difference between two engines based on a ({@link #MatchResult MatchResult} instance.
	 * 
	 * @param result The results of an {@link #Arena Arena} match.
	 * @return
	 */
	public final static int calculateDifference(MatchResult result) {
		return calculateDifference(result.getEngine1Wins(), result.getDraws(), result.getEngine2Wins());
	}
}
