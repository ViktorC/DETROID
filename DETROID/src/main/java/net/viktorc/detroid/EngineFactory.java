package main.java.net.viktorc.detroid;

import main.java.net.viktorc.detroid.engine.Detroid;
import main.java.net.viktorc.detroid.framework.control.ControllerEngine;
import main.java.net.viktorc.detroid.framework.tuning.TunableEngine;

/**
 * A singleton factory class for providing new engine instances.
 * 
 * @author Viktor
 *
 */
public final class EngineFactory {

	private static final EngineFactory INSTANCE = new EngineFactory();
	
	private EngineFactory() {
		
	}
	/**
	 * Returns the only existing instance.
	 * 
	 * @return The only instance.
	 */
	public static EngineFactory getInstance() {
		return INSTANCE;
	}
	/**
	 * Returns a new {@link #TunableEngine TunableEngine} instance.
	 * 
	 * @return A new tunable, UCI compatible engine instance.
	 */
	public TunableEngine newEngineInstance() {
		/* Create and return an instance of your engine. */
		return new Detroid();
	}
	/**
	 * Returns a new {@link #ControllerEngine ControllerEngine} instance.
	 * 
	 * @return A new controller engine instance.
	 */
	public ControllerEngine newControllerEngineInstance() {
		/* Create and return an instance of your engine or if it does not implement the interface, create and return an instance of
		 * a ControllerEngine. */
		return new Detroid();
	}
	
}
