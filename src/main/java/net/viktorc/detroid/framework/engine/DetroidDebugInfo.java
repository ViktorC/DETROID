package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.uci.DebugInformation;

/**
 * An subclass of the UCI abstract class {@link #uci.DebugInfo DebugInfo} for holding debug info data.
 * 
 * @author Viktor
 *
 */
class DetroidDebugInfo extends DebugInformation {

	private String info;
	
	/**
	 * Sets the content of the instance and notifies all observers.
	 * 
	 * @param info
	 */
	void set(String info) {
		this.info = info;
		setChanged();
		notifyObservers();
	}
	@Override
	public String getContent() {
		return info;
	}

}
