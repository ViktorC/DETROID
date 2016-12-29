package main.java.uci;

import java.util.Observable;

/**
 * A simple observable abstract class for keeping the observers up to date with debug information strings.
 * 
 * @author Viktor
 *
 */
public abstract class DebugInformation extends Observable {
	
	/**
	 * It returns the content of the instance, i.e. the debug information string.
	 * 
	 * @return
	 */
	public abstract String getContent();
	
}
