package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.uci.DebugInformation;

/**
 * An subclass of the UCI abstract class {@link net.viktorc.detroid.framework.uci.DebugInformation} for holding debug
 * info data.
 * 
 * @author Viktor
 *
 */
class DetroidDebugInfo extends DebugInformation {

	private String info;
	
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
