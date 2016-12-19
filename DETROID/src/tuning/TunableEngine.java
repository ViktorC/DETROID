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
	 * @return The engine parameters.
	 */
	EngineParameters getParameters();
	/**
	 * Notifies the engine that the parameters have changed and that if it uses cached values, it should reload them.
	 */
	void reloadParameters();
	/**
	 * Sets whether the engine should run in static evaluation mode. Static evaluation mode should support 0-depth search 
	 * which is a deterministic quiescent search.
	 * 
	 * @param on Whether the engine should run in static evaluation mode.
	 */
	void setStaticEvalTuningMode(boolean on);
}
