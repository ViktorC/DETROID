package net.viktorc.detroid.framework.gui.models;

/**
 * A view model bean for {@link #net.viktorc.detroid.framework.uci.SearchInformation SearchInformation}.
 * 
 * @author Viktor
 *
 */
public final class SearchData {
	
	private final String depth;
	private final String time;
	private final String nodes;
	private final String moveNo;
	private final String principalVar;
	private final String score;
	private final String nps;
	private final String hash;
	
	/**
	 * Constructs a default instance.
	 */
	public SearchData() {
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
	public SearchData(String depth, String time, String nodes, String moveNo, String principalVar, String score,
			String nps, String hash) {
		this.depth = depth;
		this.time = time;
		this.nodes = nodes;
		this.moveNo = moveNo;
		this.principalVar = principalVar;
		this.score = score;
		this.nps = nps;
		this.hash = hash;
	}
	/**
	 * See the corresponding {@link #net.viktorc.detroid.framework.uci.SearchInformation SearchInformation} method.
	 */
	public String getDepth() {
		return depth;
	}
	/**
	 * See the corresponding {@link #net.viktorc.detroid.framework.uci.SearchInformation SearchInformation} method.
	 */
	public String getTime() {
		return time;
	}
	/**
	 * See the corresponding {@link #net.viktorc.detroid.framework.uci.SearchInformation SearchInformation} method.
	 */
	public String getNodes() {
		return nodes;
	}
	/**
	 * See the corresponding {@link #net.viktorc.detroid.framework.uci.SearchInformation SearchInformation} method.
	 */
	public String getMoveNo() {
		return moveNo;
	}
	/**
	 * See the corresponding {@link #net.viktorc.detroid.framework.uci.SearchInformation SearchInformation} method.
	 */
	public String getPrincipalVar() {
		return principalVar;
	}
	/**
	 * See the corresponding {@link #net.viktorc.detroid.framework.uci.SearchInformation SearchInformation} method.
	 */
	public String getScore() {
		return score;
	}
	/**
	 * Returns the average number of nodes the engine searched in a second; a unit of search speed.
	 * 
	 * @return The average number of nodes the engine searched in a second.
	 */
	public String getNps() {
		return nps;
	}
	/**
	 * Returns the load factor of the hash tables the engine uses.
	 * 
	 * @return A string representing the load factor of the hash tables the engine uses.
	 */
	public String getHash() {
		return hash;
	}
	
}