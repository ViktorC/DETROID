package net.viktorc.detroid.framework.gui.viewmodels;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A view model for {@link #net.viktorc.detroid.framework.uci.SearchInformation SearchInformation}.
 * 
 * @author Viktor
 *
 */
public final class SearchInfoViewModel {
	
	private final StringProperty depth;
	private final StringProperty time;
	private final StringProperty nodes;
	private final StringProperty moveNo;
	private final StringProperty principalVar;
	private final StringProperty score;
	private final StringProperty nps;
	private final StringProperty hash;
	
	/**
	 * 
	 */
	public SearchInfoViewModel() {
		depth = null;
		time = null;
		nodes = null;
		moveNo = null;
		principalVar = null;
		score = null;
		nps = null;
		hash = null;
	}
	/**
	 * Constructs the view model using the specified parameter values.
	 */
	public SearchInfoViewModel(String depth, String time, String nodes, String moveNo, String principalVar, String score,
			String nps, String hash) {
		this.depth = new SimpleStringProperty(depth);
		this.time = new SimpleStringProperty(time);
		this.nodes = new SimpleStringProperty(nodes);
		this.moveNo = new SimpleStringProperty(moveNo);
		this.principalVar = new SimpleStringProperty(principalVar);
		this.score = new SimpleStringProperty(score);
		this.nps = new SimpleStringProperty(nps);
		this.hash = new SimpleStringProperty(hash);
	}
	/**
	 * See the corresponding {@link #net.viktorc.detroid.framework.uci.SearchInformation SearchInformation} method.
	 */
	public String getDepth() {
		return depth.get();
	}
	/**
	 * See the corresponding {@link #net.viktorc.detroid.framework.uci.SearchInformation SearchInformation} method.
	 */
	public String getTime() {
		return time.get();
	}
	public String getNodes() {
		return nodes.get();
	}
	/**
	 * See the corresponding {@link #net.viktorc.detroid.framework.uci.SearchInformation SearchInformation} method.
	 */
	public String getMoveNo() {
		return moveNo.get();
	}
	/**
	 * See the corresponding {@link #net.viktorc.detroid.framework.uci.SearchInformation SearchInformation} method.
	 */
	public String getPrincipalVar() {
		return principalVar.get();
	}
	/**
	 * See the corresponding {@link #net.viktorc.detroid.framework.uci.SearchInformation SearchInformation} method.
	 */
	public String getScore() {
		return score.get();
	}
	/**
	 * Returns the average number of nodes the engine searched in a second; a unit of search speed.
	 * 
	 * @return The average number of nodes the engine searched in a second.
	 */
	public String getNps() {
		return nps.get();
	}
	/**
	 * Returns the load factor of the hash tables the engine uses.
	 * 
	 * @return A string representing the load factor of the hash tables the engine uses.
	 */
	public String getHash() {
		return hash.get();
	}
	
}