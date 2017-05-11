package net.viktorc.detroid.framework.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import net.viktorc.detroid.framework.uci.UCIEngine;


/**
 * An interface for an engine that provides the basic functionalities required for a user interface base engine.
 * 
 * @author Viktor
 *
 */
public interface ControllerEngine extends UCIEngine {


	/**
	 * Returns whether it's white's turn.
	 * 
	 * @return Whether the side to move is white.
	 */
	boolean isWhitesTurn();
	/**
	 * Returns the starting position of the current game in Forsyth-Edwards Notation.
	 * 
	 * @return The start position of the game in FEN.
	 */
	String getStartPosition();
	/**
	 * Returns the current game state as labelled by {@link net.viktorc.detroid.framework.validation.GameState}.
	 * 
	 * @return The current game state.
	 */
	GameState getGameState();
	/**
	 * Returns a list of all past moves made on the board in Pure Algebraic Coordinate Notation in chronological order.
	 * 
	 * @return A list of all past moves made on the board in PACN. It should never return null.
	 */
	List<String> getMoveHistory();
	/**
	 * Returns a list of all legal moves in the current position in Pure Algebraic Coordinate Notation.
	 * 
	 * @return A list of all legal moves in the current position in PACN. It should never return null.
	 */
	List<String> getLegalMoves();
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
	 * Sets whether the engine should run in controller mode.
	 * 
	 * @param on Whether the engine should run in controller mode.
	 */
	void setControllerMode(boolean on);
	/**
	 * Sets the game state to draw by agreement from both parties.
	 */
	void drawByAgreement();
	/**
	 * Sets the game state to unspecified black victory. This can be due to a loss on time, resignation, or breaking of the rules.
	 */
	void whiteForfeit();
	/**
	 * Sets the game state to unspecified white victory. This can be due to a loss on time, resignation, or breaking of the rules.
	 */
	void blackForfeit();
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
	 * Returns a Forsyth-Edwards Notation string representing the current position.
	 * 
	 * @return A FEN string representing the current position.
	 */
	String toFEN();
	/**
	 * Returns a Portable Game Notation string representing the state of the game.
	 * 
	 * @return A PGN string representing the state of the game.
	 */
	String toPGN();
	/**
	 * Takes back the last move and returns it in Pure Algebraic Coordinate Notation. It returns null if no moves have been made yet.
	 * 
	 * @return The last move made in Pure Algebraic Coordinate Notation
	 */
	default String unplayLastMove() {
		List<String> moveHistory = getMoveHistory();
		if (moveHistory.size() == 0)
			return null;
		String initFen = getStartPosition();
		setPosition(initFen);
		for (int i = 0; i < moveHistory.size() - 1; i++)
			play(moveHistory.get(i));
		return moveHistory.get(moveHistory.size() - 1);
	}
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
