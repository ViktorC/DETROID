package net.viktorc.detroid.framework.tuning;

/**
 * An class for storing the results of a match between two engines.
 *
 * @author Viktor
 */
class MatchResult {

  private final int engine1Wins;
  private final int engine2Wins;
  private final int draws;

  /**
   * Constructs an instance using the specified parameters.
   */
  public MatchResult(int engine1Wins, int engine2Wins, int draws) {
    if (engine1Wins < 0 || engine2Wins < 0 || draws < 0) {
      throw new IllegalArgumentException("All parameters have to be 0 or greater.");
    }
    this.engine1Wins = engine1Wins;
    this.engine2Wins = engine2Wins;
    this.draws = draws;
  }

  /**
   * Returns the number of times Engine 1 won.
   */
  public int getEngine1Wins() {
    return engine1Wins;
  }

  /**
   * Returns the number of times Engine 2 won.
   */
  public int getEngine2Wins() {
    return engine2Wins;
  }

  /**
   * Returns the number of times the two engines drew.
   */
  public int getDraws() {
    return draws;
  }

}