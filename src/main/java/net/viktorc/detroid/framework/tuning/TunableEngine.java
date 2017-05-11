package net.viktorc.detroid.framework.tuning;

import net.viktorc.detroid.framework.uci.UCIEngine;

/**
 * An interface for a tunable chess engine whose parameters can be retrieved.
 * 
 * @author Viktor
 */
public interface TunableEngine extends UCIEngine {

	/**
	 * Returns the parameters which are used by the engine to control search, evaluation, and other aspects of game play.
	 * 
	 * @return The engine parameters.
	 */
	EngineParameters getParameters();
	/**
	 * Notifies the engine that the parameters have changed and that if it uses cached values, it should reload them. If 
	 * reloading a parameter would affect a UCI setting whose current value is not the default value, it should not be 
	 * reloaded. (E.g. if the hash size is defined in the parameter configuration XML file, but the UCI option "Hash" has 
	 * been set to a value other than its default value, the hash size used should remain unchanged.)
	 */
	void notifyParametersChanged();
	/**
	 * Sets whether the engine should support deterministic 0-depth search which is a quiescence search without the use of hash tables 
	 * such as a transposition table or any other mechanisms that bring non-determinism to the search. A 0-depth search is triggered by 
	 * calling the {@link #search(java.util.Set, Boolean, Long, Long, Long, Long, Integer, Integer, Long, Integer, Long, Boolean) search} 
	 * method with depth set to 0 and everything else to null while deterministic 0-depth mode is set to true. A 0-depth search is not 
	 * expected to return a move, only a score. When in deterministic 0-depth mode, engines should keep their hash size minimal.
	 * 
	 * @param on Whether the engine should support deterministic 0-depth search.
	 */
	void setDeterministicZeroDepthMode(boolean on);
	
}
