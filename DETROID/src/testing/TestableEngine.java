package testing;

import control.ControllerEngine;

/**
 * An interface for a testable chess engine that implements perft.
 * 
 * @author Viktor
 *
 */
public interface TestableEngine extends ControllerEngine {

	/**
	 * Runs a perft to the specified depth in the current position and returns the number of leaf nodes counted while 
	 * traversing the tree.
	 * 
	 * @param depth The depth at which the leaf nodes are to be counted.
	 * @return The number of leaf nodes counted.
	 */
	long perft(int depth);
	
}
