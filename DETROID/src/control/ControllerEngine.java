package control;

import uci.UCIEngine;

/**
 * An interface for an engine that provides the basic functionalities required for a user interface base engine.
 * 
 * @author Viktor
 *
 */
public interface ControllerEngine extends UCIEngine {


	/**
	 * Returns the current game state as labelled by {@link #uibase.GameState GameState}.
	 * 
	 * @return
	 */
	GameState getGameState();
	/**
	 * Parses the Portable Game Notation string and sets its internal data structure tracking the state of the game
	 * accordingly. It returns whether the game could be successfully set or not.
	 * 
	 * @param pgn
	 * @return
	 */
	boolean setGame(String pgn);
	/**
	 * Sets the names of the players.
	 * 
	 * @param whitePlayer
	 * @param blackPlayer
	 */
	void setPlayers(String whitePlayer, String blackPlayer);
	/**
	 * Sets the event at which the game takes place.
	 * 
	 * @param event
	 */
	void setEvent(String event);
	/**
	 * Sets the site where the game takes place.
	 * 
	 * @param site
	 */
	void setSite(String site);
	/**
	 * Unmakes the last move and returns it in Pure Algebraic Coordinate Notation. It returns null if no moves have been made yet.
	 * 
	 * @return
	 */
	String unplayLastMove();
	/**
	 * Converts a move legal in the current position from Pure Algebraic Coordinate Notation to Standard Algebraic Notation.
	 * 
	 * @param move The move in PACN.
	 * @return The move in SAN.
	 */
	String convertPACNToSAN(String move);
	/**
	 * Returns a Forsyth–Edwards Notation string representing the current position.
	 * 
	 * @return
	 */
	String toFEN();
	/**
	 * Returns a Portable Game Notation string representing the state of the game.
	 * 
	 * @return
	 */
	String toPGN();
	/**
	 * Sets whether the engine should run in controller mode.
	 * 
	 * @param on
	 */
	void setControllerMode(boolean on);
	
}
