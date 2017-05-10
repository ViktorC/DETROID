package net.viktorc.detroid;

import net.viktorc.detroid.framework.EngineFactory;
import net.viktorc.detroid.framework.engine.Detroid;
import net.viktorc.detroid.framework.ApplicationFramework;
import net.viktorc.detroid.framework.tuning.TunableEngine;

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
		}, args)).run();
	}
	
}
