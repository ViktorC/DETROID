package engine;

import java.util.ArrayList;

import uci.ScoreType;
import uci.SearchInformation;

/**
 * An observable class for the results and statistics of a search.
 * 
 * @author Viktor
 *
 */
class SearchInfo extends SearchInformation {
	
	private ArrayList<Move> pVline;	// Principal variation.
	private Move currentMove;		// The root move currently being searched.
	private int currentMoveNumber;	// The ordinal of the move currently being searched in the root position's move list.
	private short nominalDepth;		// The depth to which the PV has been searched.
	private short score;			// The result score of the search.
	private ScoreType scoreType;	// Whether it is a mate score, in which case the score denotes the mate distance,
									// an exact score or a lower/upper bound.
	private long nodes;				// The number of nodes searched.
	private long time;				// Time spent on the search.
	
	SearchInfo() {
		
	}
	void set(ArrayList<Move> PVline, Move currentMove, int currentMoveNumber, short nominalDepth,
			short score, ScoreType scoreType, long nodes, long time) {
		this.pVline = PVline;
		this.currentMove = currentMove;
		this.currentMoveNumber = currentMoveNumber;
		this.nominalDepth = nominalDepth;
		this.score = score;
		this.scoreType = scoreType;
		this.nodes = nodes;
		this.time = time;
		setChanged();
		notifyObservers();
	}
	/**
	 * Returns the principal variation of the search as a list of moves.
	 * 
	 * @return
	 */
	ArrayList<Move> getPvMoveList() {
		return pVline;
	}
	/**
	 * Returns a one-line String representation of the principal variation result.
	 * 
	 * @return
	 */
	String getPvString() {
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
	String getStatString() {
		String out = "";
		out += "Nominal depth: " + nominalDepth + "\n";
		out += "Score: " + score + "; Type: " + scoreType + "\n";
		out += String.format("Time: %.2fs\n", (float)time/1000);
		out += "Nodes: " + nodes + "\n";
		out += "Search speed: " + nodes/Math.max(time, 1) + "kNps\n";
		return out;
	}
	@Override
	public int getPvNumber() {
		return 0;
	}
	@Override
	public String[] getPv() {
		String[] arr = new String[pVline.size()];
		int i = 0;
		for (Move m : pVline) {
			arr[i++] = m.toString();
		}
		return arr;
	}
	@Override
	public String getCurrentMove() {
		return currentMove == null ? null : currentMove.toString();
	}
	@Override
	public int getCurrentMoveNumber() {
		return currentMoveNumber;
	}
	@Override
	public short getDepth() {
		return nominalDepth;
	}
	@Override
	public short getScore() {
		return score;
	}
	@Override
	public ScoreType getScoreType() {
		return scoreType;
	}
	@Override
	public long getNodes() {
		return nodes;
	}
	@Override
	public long getTime() {
		return time;
	}
	@Override
	public String toString() {
		return getPvString() + getStatString();
	}
}
