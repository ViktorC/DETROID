package net.viktorc.detroid.framework.validation;

/**
 * All the different states a chess game can have.
 * 
 * @author Viktor
 *
 */
public enum GameState {
	
	IN_PROGRESS,
	WHITE_MATES,
	BLACK_MATES,
	STALE_MATE,
	DRAW_BY_INSUFFICIENT_MATERIAL,
	DRAW_BY_3_FOLD_REPETITION,
	DRAW_BY_50_MOVE_RULE,
	DRAW_BY_AGREEMENT,
	/**
	 * E.g. black resigns or loses on time.
	 */
	UNSPECIFIED_WHITE_WIN,
	/**
	 * E.g. white resigns or loses on time.
	 */
	UNSPECIFIED_BLACK_WIN,
	
}
