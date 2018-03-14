package net.viktorc.detroid.framework.tuning;

import net.viktorc.detroid.framework.uci.UCIEngine;
import net.viktorc.detroid.framework.validation.ControllerEngine;

/**
 * A class for storing references to engines required to perform self-play.
 * 
 * @author Viktor
 *
 * @param <T> The type of the engines to pit against each other.
 */
public final class SelfPlayEngines<T extends UCIEngine> {

	private final T engine;
	private final T opponentEngine;
	private final ControllerEngine controller;

	/**
	 * Constructs an instance holding references to the three engines necessary for self-play.
	 * 
	 * @param engine The engine to be tuned.
	 * @param opponentEngine The opponent engine.
	 * @param controller The controller engine for the {@link net.viktorc.detroid.framework.tuning.Arena}.
	 * @throws Exception If the engines cannot be initialised.
	 */
	public SelfPlayEngines(T engine, T opponentEngine, ControllerEngine controller) 
			throws Exception {
		if (engine == null || opponentEngine == null || controller == null)
			throw new IllegalArgumentException("The parameters engine, opponentEngine, and controller cannot be null");
		if (!engine.isInit())
			engine.init();
		this.engine = engine;
		this.opponentEngine = opponentEngine;
		this.controller = controller;
	}
	/**
	 * Returns the first engine.
	 * 
	 * @return The first engine.
	 */
	public T getEngine() {
		return engine;
	}
	/**
	 * Returns the opponent engine
	 * 
	 * @return The opponent engine.
	 */
	public T getOpponentEngine() {
		return opponentEngine;
	}
	/**
	 * Returns the controller engine.
	 * 
	 * @return The controller engine.
	 */
	public ControllerEngine getController() {
		return controller;
	}
	
}
