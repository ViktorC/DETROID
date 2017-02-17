package net.viktorc.detroid.framework.validation;

/**
 * A utility class for calculating Elo score differences based on match statistics such as the 
 * win ratio or the numbers of wins, losses, and draws.
 * 
 * @author Viktor
 *
 */
public final class Elo {

	/**
	 * The minimum allowed win ratio. Any win ratio smaller than this defaults to this value.
	 */
	private static final double MIN_WIN_RATIO = 1e-5;
	/**
	 * The maximum allowed win ratio. Any win ratio greater than this defaults to this value.
	 */
	private static final double MAX_WIN_RATIO = 1 - MIN_WIN_RATIO;
	
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
		winRatio = Math.max(winRatio, MIN_WIN_RATIO);
		winRatio = Math.min(winRatio, MAX_WIN_RATIO);
		return (int) -Math.round(Math.log10(1/winRatio - 1)*400);
	}
	
}
