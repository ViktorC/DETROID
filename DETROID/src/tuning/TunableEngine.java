package tuning;

import uci.UCIEngine;
import util.Parameters;

/**
 * An interface for a tunable chess engine whose parameters can be retrieved.
 * 
 * @author Viktor
 *
 * @param <T>
 */
public interface TunableEngine extends UCIEngine {

	/**
	 * Returns the parameters which are used by the engine to control search, evaluation, and other aspects of game play.
	 * 
	 * @return
	 */
	Parameters getParameters();
	
}
