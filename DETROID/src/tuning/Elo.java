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
	 * @param losses The number of losses. It has to be  0 or greater.
	 * @param draws The number of draws. It has to be 0 or greater.
	 * @return
	 */
	public final static int calculateDifference(int wins, int losses, int draws) {
		if (wins < 0 || draws < 0 || losses < 0)
			throw new IllegalArgumentException("All parameters have to be 0 or greater.");
		int games = wins + draws + losses;
		if (games == 0)
			return 0;
		double points = wins + draws*0.5;
		return calculateDifference(points/games);
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
		if (winRatio == 0)
			return Integer.MIN_VALUE;
		if (winRatio == 1)
			return Integer.MAX_VALUE;
		return (int) -Math.round((Math.log((1 - winRatio)/winRatio)*400));
	}
	/**
	 * Calculates the Elo rating difference between two engines based on a ({@link #MatchResult MatchResult} instance.
	 * 
	 * @param result The results of an {@link #Arena Arena} match.
	 * @return
	 */
	public final static int calculateDifference(MatchResult result) {
		return calculateDifference(result.getEngine1Wins(), result.getEngine2Wins(), result.getDraws());
	}
}
