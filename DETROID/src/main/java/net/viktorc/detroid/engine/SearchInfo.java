package net.viktorc.detroid.engine;

import java.util.ArrayList;
import java.util.List;
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
	 * Returns the principal variation of the search as a list of moves.
	 * 
	 * @return
	 */
	List<Move> getPvMoveList() {
		lock.readLock().lock();
		try {
			return pVline;
		} finally {
			lock.readLock().unlock();
		}
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
		lock.readLock().lock();
		try {
			return 0;
		} finally {
			lock.readLock().unlock();
		}
	}
	@Override
	public String[] getPv() {
		lock.readLock().lock();
		try {
			if (pVline == null)
				return null;
			ArrayList<String> pV = new ArrayList<>(pVline.size());
			for (Move m : pVline)
				pV.add(m.toString());
			return pV.toArray(new String[pV.size()]);
		} finally {
			lock.readLock().unlock();
		}
	}
	@Override
	public String getCurrentMove() {
		lock.readLock().lock();
		try {
			return currentMove == null ? null : currentMove.toString();
		} finally {
			lock.readLock().unlock();
		}
	}
	@Override
	public int getCurrentMoveNumber() {
		lock.readLock().lock();
		try {
			return currentMoveNumber;
		} finally {
			lock.readLock().unlock();
		}
	}
	@Override
	public short getDepth() {
		lock.readLock().lock();
		try {
			return nominalDepth;
		} finally {
			lock.readLock().unlock();
		}
	}
	@Override
	public short getSelectiveDepth() {
		lock.readLock().lock();
		try {
			return selectiveDepth;
		} finally {
			lock.readLock().unlock();
		}
	}
	@Override
	public short getScore() {
		lock.readLock().lock();
		try {
			return score;
		} finally {
			lock.readLock().unlock();
		}
	}
	@Override
	public ScoreType getScoreType() {
		lock.readLock().lock();
		try {
			return scoreType;
		} finally {
			lock.readLock().unlock();
		}
	}
	@Override
	public long getNodes() {
		lock.readLock().lock();
		try {
			return nodes;
		} finally {
			lock.readLock().unlock();
		}
	}
	@Override
	public long getTime() {
		lock.readLock().lock();
		try {
			return time;
		} finally {
			lock.readLock().unlock();
		}
	}
	@Override
	public String toString() {
		lock.readLock().lock();
		try {
			return getPvString() + getStatString();
		} finally {
			lock.readLock().unlock();
		}
	}
	
}
