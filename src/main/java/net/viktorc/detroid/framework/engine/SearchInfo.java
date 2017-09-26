package net.viktorc.detroid.framework.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.viktorc.detroid.framework.uci.ScoreType;
import net.viktorc.detroid.framework.uci.SearchInformation;

/**
 * An observable class for the results and statistics of a search.
 * 
 * @author Viktor
 *
 */
class SearchInfo extends SearchInformation {
	
	// Principal variation.
	private List<Move> pVline;
	// The root move currently being searched.
	private Move currentMove;
	// The ordinal of the move currently being searched in the root position's move list.
	private int currentMoveNumber;
	// The nominal depth of the search.
	private short nominalDepth;
	// The greatest depth of the search.
	private short selectiveDepth;
	// The result score of the search.
	private short score;
	/* Whether it is a mate score, in which case the score denotes the mate distance, an exact score, 
	 * or a lower/upper bound. */
	private ScoreType scoreType;
	// The number of nodes searched.
	private long nodes;
	// Time spent on the search.
	private long time;
	// A lock ensuring integrity among the accessed field values.
	private ReadWriteLock lock;
	
	/**
	 * Constructs a default instance.
	 */
	SearchInfo() {
		lock = new ReentrantReadWriteLock();
	}
	/**
	 * Sets the search results according to the specified parameters.
	 * 
	 * @param PVline
	 * @param currentMove
	 * @param currentMoveNumber
	 * @param nominalDepth
	 * @param selectiveDepth
	 * @param score
	 * @param scoreType
	 * @param nodes
	 * @param time
	 * @param isCancelled
	 */
	void set(List<Move> PVline, Move currentMove, int currentMoveNumber, short nominalDepth,
			short selectiveDepth, short score, ScoreType scoreType, long nodes, long time) {
		lock.writeLock().lock();
		try {
			this.pVline = PVline;
			this.currentMove = currentMove;
			this.currentMoveNumber = currentMoveNumber;
			this.nominalDepth = nominalDepth;
			this.selectiveDepth = selectiveDepth;
			this.score = score;
			this.scoreType = scoreType;
			this.nodes = nodes;
			this.time = time;
		} finally {
			lock.writeLock().unlock();
		}
		setChanged();
		notifyObservers();
	}
	/**
	 * Returns a read lock for the instance.
	 * 
	 * @return
	 */
	Lock getLock() {
		return lock.readLock();
	}
	/**
	 * Returns the principal variation of the search as a list of moves.
	 * 
	 * @return
	 */
	List<Move> getPvMoveList() {
		return pVline;
	}
	/**
	 * Returns a one-line String representation of the principal variation result.
	 * 
	 * @return
	 */
	String getPvString() {
		lock.readLock().lock();
		try {
			String out = "";
			for (Move m : pVline)
				out += m.toString() + " ";
			out += "\n";
			return out;
		} finally {
			lock.readLock().unlock();
		}
	}
	/**
	 * Returns a String of some search statistics such as greatest nominal depth, score, search speed, etc.
	 * 
	 * @return
	 */
	String getStatString() {
		lock.readLock().lock();
		try {
			String out = "";
			out += "Nominal depth: " + nominalDepth + "\n";
			out += "Score: " + score + "; Type: " + scoreType + "\n";
			out += String.format("Time: %.2fs\n", (float)time/1000);
			out += "Nodes: " + nodes + "\n";
			out += "Search speed: " + nodes/Math.max(time, 1) + "kNps\n";
			return out;
		} finally {
			lock.readLock().unlock();
		}
	}
	@Override
	public int getPvNumber() {
		return 0;
	}
	@Override
	public String[] getPv() {
		if (pVline == null)
			return null;
		ArrayList<String> pV = new ArrayList<>(pVline.size());
		for (Move m : pVline)
			pV.add(m.toString());
		return pV.toArray(new String[pV.size()]);
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
	public short getSelectiveDepth() {
		return selectiveDepth;
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
