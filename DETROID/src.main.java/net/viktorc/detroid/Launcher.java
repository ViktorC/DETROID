package net.viktorc.detroid;

import net.viktorc.detroid.engine.Detroid;
import net.viktorc.detroid.framework.EngineFactory;
import net.viktorc.detroid.framework.ApplicationFramework;
import net.viktorc.detroid.framework.tuning.TunableEngine;
import net.viktorc.detroid.framework.validation.ControllerEngine;

/**
 * The main class for the engine and tuning framework.
 * 
 * @author Viktor
 *
 */
class Launcher {
	
	/**
	 * The entry point for the application.
	 * 
	 * @param args The same as for {@link #FrameworkApplication}.
	 */
	public static void main(String[] args) {
		(new ApplicationFramework(new EngineFactory() {
			
			@Override
			public TunableEngine newEngineInstance() {
				return new Detroid();
			}
			
			@Override
			public ControllerEngine newControllerEngineInstance() {
				return new Detroid();
			}
		}, args)).run();
	}
	
}
