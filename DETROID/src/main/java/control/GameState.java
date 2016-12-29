package main.java.control;

/**
 * All the different states a chess game can have disregarding time control.
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
	DRAW_BY_50_MOVE_RULE
	
}
