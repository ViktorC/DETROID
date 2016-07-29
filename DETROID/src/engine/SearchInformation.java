package engine;

import uci.ScoreType;
import uci.SearchInfo;
import util.List;

/**
 * An observable class for the results and statistics of a search.
 * 
 * @author Viktor
 *
 */
public class SearchInformation extends SearchInfo {
	
	private List<Move> pVline;		// Principal variation.
	private Move currentMove;		// The root move currently being searched.
	private int currentMoveNumber;	// The ordinal of the move currently being searched in the root position's move list.
	private short nominalDepth;		// The depth to which the PV has been searched.
	private short score;			// The result score of the search.
	private ScoreType scoreType;	// Whether it is a mate score, in which case the score denotes the mate distance,
									// an exact score or a lower/upper bound.
	private long nodes;				// The number of nodes searched.
	private long time;				// Time spent on the search.
	
	SearchInformation() {
		
	}
	/**
	 * Returns the principal variation of the search as a queue of moves.
	 * 
	 * @return
	 */
	public List<Move> getPvMoveList() {
		return pVline;
	}
	/* (non-Javadoc)
	 * @see chess.SearchInfo#getPv()
	 */
	@Override
	public String[] getPv() {
		String[] arr = new String[pVline.size()];
		int i = 0;
		for (Move m : pVline) {
			arr[i++] = m.toString();
		}
		return arr;
	}
	/* (non-Javadoc)
	 * @see chess.SearchInfo#getPv()
	 */
	@Override
	public String getCurrentMove() {
		return currentMove == null ? null : currentMove.toString();
	}
	/* (non-Javadoc)
	 * @see chess.SearchInfo#getPv()
	 */
	@Override
	public int getCurrentMoveNumber() {
		return currentMoveNumber;
	}
	/* (non-Javadoc)
	 * @see chess.SearchInfo#getNominalDepth()
	 */
	@Override
	public short getDepth() {
		return nominalDepth;
	}
	/* (non-Javadoc)
	 * @see chess.SearchInfo#getScore()
	 */
	@Override
	public short getScore() {
		return score;
	}
	/* (non-Javadoc)
	 * @see chess.SearchInfo#getScoreType()
	 */
	@Override
	public ScoreType getScoreType() {
		return scoreType;
	}
	/* (non-Javadoc)
	 * @see chess.SearchInfo#getNodes()
	 */
	@Override
	public long getNodes() {
		return nodes;
	}
	/* (non-Javadoc)
	 * @see chess.SearchInfo#getTime()
	 */
	@Override
	public long getTime() {
		return time;
	}
	void set(List<Move> PVline, Move currentMove, int currentMoveNumber, short nominalDepth,
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
