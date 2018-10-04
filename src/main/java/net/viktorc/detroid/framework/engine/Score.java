package net.viktorc.detroid.framework.engine;

/**
 * An enumeration type for different game state scores such as check mate, stale mate, and draw due to different reasons.
 * 
 * @author Viktor
 *
 */
enum Score {
	
	NULL(Short.MIN_VALUE + 2),
	MIN(Short.MIN_VALUE + 1),
	MAX(Short.MAX_VALUE),
	LOSING_CHECK_MATE ((Short.MIN_VALUE + 2)/2),
	WINNING_CHECK_MATE((Short.MAX_VALUE - 1)/2),
	STALE_MATE (0),
	INSUFFICIENT_MATERIAL (0),
	DRAW_CLAIMED (0);
	
	/**
	 * The score associated with the termination.
	 */
	final short value;
	
	Score(int value) {
		this.value = (short) value;
	}
	
}
