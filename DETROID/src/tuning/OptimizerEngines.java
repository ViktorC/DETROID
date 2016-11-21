package tuning;

import uibase.ControllerEngine;

/**
 * A parameter class for {@link #EngineParameterOptimizer EngineParameterOptimizer} storing references to engines required to perform the
 * optimization.
 * 
 * @author Viktor
 *
 */
public class OptimizerEngines {

	private final TunableEngine engine;
	private final TunableEngine opponentEngine;
	private final ControllerEngine controller;

	/**
	 * Constructs an instance holding references to the three engines necessary for {@link #EngineParameterOptimizer EngineParameterOptimizer}
	 * to run an optimization thread.
	 * 
	 * @param tunableEngine The engine to be tuned.
	 * @param opponentEngine The opponent engine.
	 * @param controller The controller engine for the {@link #tuning.Arena Arena}.
	 * @throws IllegalArgumentException If any of the parameters are null.
	 */
	public OptimizerEngines(TunableEngine tunableEngine, TunableEngine opponentEngine, ControllerEngine controller) 
			throws IllegalArgumentException {
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
	 * @return
	 */
	public TunableEngine getEngine() {
		return engine;
	}
	/**
	 * Returns the opponent engine
	 * 
	 * @return
	 */
	public TunableEngine getOpponentEngine() {
		return opponentEngine;
	}
	/**
	 * Returns the controller engine.
	 * 
	 * @return
	 */
	public ControllerEngine getController() {
		return controller;
	}
}
