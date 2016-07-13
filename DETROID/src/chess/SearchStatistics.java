package chess;

import java.util.Observable;

import util.List;

/**
 * An observable class for the results and statistics of a search.
 * 
 * @author Viktor
 *
 */
public class SearchStatistics extends Observable {
	
	private List<Move> pVline;		// Principal variation.
	private short nominalDepth;		// The depth to which the PV has been searched.
	private short score;			// The result score of the search.
	private ScoreType scoreType;	// Whether it is a mate score, in which case the score denotes the mate distance,
									// an exact score or a lower/upper bound.
	private long nodes;				// The number of nodes searched.
	private long time;				// Time spent on the search.
	private boolean isFinal;		// Whether it is the final result of the search.
	
	SearchStatistics() {
		
	}
	/**
	 * Returns the principal variation of the search as a queue of moves.
	 * 
	 * @return
	 */
	public List<Move> getPvLine() {
		return pVline;
	}
	/**
	 * Returns the greatest nominal depth of the search. It does not necessarily mean that the whole ply has been searched.
	 * 
	 * @return
	 */
	public short getNominalDepth() {
		return nominalDepth;
	}
	/**
	 * Returns the result score of the search for the side to move.
	 * 
	 * @return
	 */
	public short getScore() {
		return score;
	}
	/**
	 * Returns whether it is an exact score, a lower bound, an upper bound, or a mate score, in which case the score denotes the mate
	 * distance. If the side to move in the root position is going to get mated, the negative distance is returned.
	 * 
	 * @return
	 */
	public ScoreType getScoreType() {
		return scoreType;
	}
	/**
	 * Returns the number of nodes searched to reach this result.
	 * 
	 * @return
	 */
	public long getNodes() {
		return nodes;
	}
	/**
	 * Returns the time spent on the search to reach this result in milliseconds.
	 * 
	 * @return
	 */
	public long getTime() {
		return time;
	}
	/**
	 * Returns whether the result is final, i.e. it will not be updated anymore in this run of the search.
	 * 
	 * @return
	 */
	public boolean isFinal() {
		return isFinal;
	}
	void set(List<Move> PVline, short nominalDepth, short score, ScoreType scoreType, long nodes, long time, boolean isFinal) {
		this.pVline = PVline;
		this.nominalDepth = nominalDepth;
		this.score = score;
		this.scoreType = scoreType;
		this.nodes = nodes;
		this.time = time;
		this.isFinal = isFinal;
		setChanged();
		notifyObservers(this);
	}
	/**
	 * Returns a one-line String representation of the principal variation result.
	 * 
	 * @return
	 */
	public String getPvString() {
		String out = "";
		for (Move m : pVline)
			out += m.toString() + " ";
		out += "\n";
		return out;
	}
	/**
	 * Returns a String of some search statistics such as greatest nominal depth, score, search speed, etc.
	 * 
	 * @return
	 */
	public String getStatString() {
		String out = "";
		out += "Nominal depth: " + nominalDepth + "\n";
		out += "Score: " + score + "; Type: " + scoreType + "\n";
		out += String.format("Time: %.2fs\n", (float)time/1000);
		out += "Nodes: " + nodes + "\n";
		out += "Search speed: " + nodes/Math.max(time, 1) + "kNps\n";
		return out;
	}
	@Override
	public String toString() {
		return getPvString() + getStatString();
	}
}