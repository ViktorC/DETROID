package engine;

import uci.DebugInfo;

/**
 * An subclass of the UCI abstract class {@link #uci.DebugInfo DebugInfo} for holding debug info data.
 * 
 * @author Viktor
 *
 */
public class DebugInformation extends DebugInfo {

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
