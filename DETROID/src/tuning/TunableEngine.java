package tuning;

import uci.UCIEngine;

/**
 * An interface for a tunable chess engine whose parameters can be retrieved.
 * 
 * @author Viktor
 */
public interface TunableEngine extends UCIEngine {

	/**
	 * Returns the parameters which are used by the engine to control search, evaluation, and other aspects of game play.
	 * 
	 * @return
	 */
	EngineParameters getParameters();
	/**
	 * Notifies the engine that the parameters have changed and that if it uses cached values, it should reload them.
	 */
	void reloadParameters();
}
