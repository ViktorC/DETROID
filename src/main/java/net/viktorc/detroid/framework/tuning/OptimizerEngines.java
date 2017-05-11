package net.viktorc.detroid.framework.tuning;

import net.viktorc.detroid.framework.validation.ControllerEngine;

/**
 * A class for storing references to engines required to perform the
 * optimization.
 * 
 * @author Viktor
 *
 */
public final class OptimizerEngines {

	private final TunableEngine engine;
	private final TunableEngine opponentEngine;
	private final ControllerEngine controller;

	/**
	 * Constructs an instance holding references to the three engines necessary for self-play based optimization.
	 * 
	 * @param tunableEngine The engine to be tuned.
	 * @param opponentEngine The opponent engine.
	 * @param controller The controller engine for the {@link net.viktorc.detroid.framework.tuning.Arena}.
	 * @throws Exception If the engines cannot be initialised.
	 */
	public OptimizerEngines(TunableEngine tunableEngine, TunableEngine opponentEngine, ControllerEngine controller) 
			throws Exception {
		if (tunableEngine == null || opponentEngine == null || controller == null)
			throw new IllegalArgumentException("The parameters engine, opponentEngine, and controller cannot be null");
		if (!tunableEngine.isInit())
			tunableEngine.init();
		this.engine = tunableEngine;
		this.opponentEngine = opponentEngine;
		this.controller = controller;
	}
	/**
	 * Returns the engine to be tuned.
	 * 
	 * @return The engine to be tuned.
	 */
	public TunableEngine getEngine() {
		return engine;
	}
	/**
	 * Returns the opponent engine
	 * 
	 * @return The opponent engine.
	 */
	public TunableEngine getOpponentEngine() {
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
