package net.viktorc.detroid;

import net.viktorc.detroid.framework.engine.Detroid;
import net.viktorc.detroid.framework.EngineFramework;

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
		(new EngineFramework(Detroid::new, args)).run();
	}
	
}
