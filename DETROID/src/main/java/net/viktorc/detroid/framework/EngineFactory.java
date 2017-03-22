package net.viktorc.detroid.framework;

import net.viktorc.detroid.framework.engine.Detroid;
import net.viktorc.detroid.framework.tuning.TunableEngine;
import net.viktorc.detroid.framework.validation.ControllerEngine;

/**
 * An interface for factory classes providing new engine instances.
 * 
 * @author Viktor
 *
 */
public interface EngineFactory {

	/**
	 * Returns a new {@link #TunableEngine TunableEngine} instance.
	 * 
	 * @return A new tunable, UCI compatible engine instance.
	 */
	TunableEngine newEngineInstance();
	/**
	 * Returns a new {@link #ControllerEngine ControllerEngine} instance.
	 * 
	 * @return A new controller engine instance.
	 */
	default ControllerEngine newControllerEngineInstance() {
		return new Detroid();
	}

}