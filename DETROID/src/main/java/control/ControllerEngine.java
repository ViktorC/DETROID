package main.java.control;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import main.java.uci.UCIEngine;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;


/**
 * An interface for an engine that provides the basic functionalities required for a user interface base engine.
 * 
 * @author Viktor
 *
 */
public interface ControllerEngine extends UCIEngine {


	/**
	 * Returns a list of all legal moves in the current position in Pure Algebraic Coordinate Notation.
	 * 
	 * @return A list of all legal moves in the current position in PACN.
	 */
	List<String> getLegalMoves();
	/**
	 * Returns the current game state as labeled by {@link #uibase.GameState GameState}.
	 * 
	 * @return The current game state.
	 */
	GameState getGameState();
	/**
	 * Parses the Portable Game Notation string and sets its internal data structure tracking the state of the game
	 * accordingly. It returns whether the game could be successfully set or not.
	 * 
	 * @param pgn A PGN string representation of a chess game.
	 * @return Whether the game could be successfully set or not.
	 */
	boolean setGame(String pgn);
	/**
	 * Sets the names of the players.
	 * 
	 * @param whitePlayer The name of the white player.
	 * @param blackPlayer The name of the black player.
	 */
	void setPlayers(String whitePlayer, String blackPlayer);
	/**
	 * Sets the event at which the game is taking place.
	 * 
	 * @param event The event at which the game is taking place.
	 */
	void setEvent(String event);
	/**
	 * Sets the site where the game is taking place.
	 * 
	 * @param site The site where the game is taking place.
	 */
	void setSite(String site);
	/**
	 * Takes back the last move and returns it in Pure Algebraic Coordinate Notation. It returns null if no moves have been made yet.
	 * 
	 * @return The last move made in Pure Algebraic Coordinate Notation
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
	 * Converts a move legal in the current position from Standard Algebraic Notation to Pure Algebraic Coordinate Notation.
	 * 
	 * @param move The move in SAN.
	 * @return The move in PACN.
	 */
	String convertSANToPACN(String move);
	/**
	 * Returns a Forsyth–Edwards Notation string representing the current position.
	 * 
	 * @return A Forsyth–Edwards Notation string representing the current position.
	 */
	String toFEN();
	/**
	 * Returns a Portable Game Notation string representing the state of the game.
	 * 
	 * @return A Portable Game Notation string representing the state of the game.
	 */
	String toPGN();
	/**
	 * Sets whether the engine should run in controller mode.
	 * 
	 * @param on Whether the engine should run in controller mode.
	 */
	void setControllerMode(boolean on);
	/**
	 * Runs a perft to the specified depth in the current position and returns the number of leaf nodes counted while 
	 * traversing the tree.
	 * 
	 * @param depth The depth at which the leaf nodes are to be counted.
	 * @return The number of leaf nodes counted.
	 */
	default long perft(int depth) {
		long leafNodes = 0;
		List<String> moves = getLegalMoves();
		if (depth == 1)
			return moves.size();
		for (String m : moves) {
			play(m);
			leafNodes += perft(depth - 1);
			unplayLastMove();
		}
		return leafNodes;
	}
	/**
	 * Returns a set of entries with all legal moves in the current position as the key set and the perft results at 
	 * depth - 1 for the positions the respective moves lead to as the value set. The moves should be in PACN.
	 * 
	 * @param depth The perft depth. Perft to a depth of this minus one will be executed for each legal move from the 
	 * current position.
	 * @return A set of entries with all legal moves in the current position as the key set and the perft results at 
	 * depth - 1 for the positions the respective moves lead to as the value set.
	 */
	default Set<Entry<String, Long>> divide(int depth) {
		Set<Entry<String, Long>> set = new HashSet<>();
		for (String m : getLegalMoves()) {
			play(m);
			long nodes = perft(depth - 1);
			unplayLastMove();
			set.add(new SimpleImmutableEntry<String, Long>(m, nodes));
		}
		return set;
	}
	
}
