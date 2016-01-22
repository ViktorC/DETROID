package engine;

/**An enumration type for game phases such as opening, middle game, and end game so searches can be conducted accordingly.
 * 
 * @author Viktor
 *
 */
public enum GamePhase {
	
	OPENING 	(0, 22),
	MIDDLE_GAME (23, 170),
	END_GAME	(171, 256);	// Very early end game.
	
	public final short lowerBound;
	public final short upperBound;
	
	private GamePhase(int lowerBound, int upperBound) {
		this.lowerBound = (short)lowerBound;
		this.upperBound = (short)upperBound;
	}
	/**Returns the phase associated with the given phase score.
	 * 
	 * @param phaseScore
	 * @return
	 */
	public static GamePhase getByPhaseScore(int phaseScore) {
		if (phaseScore < MIDDLE_GAME.lowerBound)
			return OPENING;
		else if (phaseScore >= END_GAME.lowerBound)
			return END_GAME;
		else
			return MIDDLE_GAME;
	}
}