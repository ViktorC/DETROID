package engine;

/**
 * An enumeration type for different game state scores such as check mate, stale mate, and draw due to different reasons.
 * 
 * @author Viktor
 *
 */
enum Termination {
	
	CHECK_MATE				(Short.MIN_VALUE + 1),
	STALE_MATE				(0),
	INSUFFICIENT_MATERIAL	(0),
	DRAW_CLAIMED			(0);
	
	public final short score;
	
	private Termination(int score) {
		this.score = (short)score;
	}
}
