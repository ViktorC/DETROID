package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.util.Cache.Entry;

/**
 * A transposition table entry that stores information about searched positions identified by the key such as the
 * depth of the search, the best move from this position compressed into a short, the score belonging to it, the type
 * of the score, and the age of the entry. It uses Hyatt's lock-less hashing.
 *
 * Lockless hashing: <a href="http://www.craftychess.com/hyatt/hashing.html">http://www.craftychess.com/hyatt/hashing.html</a>
 * 
 * @author Viktor
 *
 */
class TTEntry implements Entry<TTEntry> {
	
	private volatile long key;
	private volatile short depth;
	private volatile byte type;
	private volatile short score;
	private volatile int bestMove;
	private volatile byte generation;
	private volatile boolean busy;

	/**
	 * @return The 64 bit position hash key.
	 */
	public long getKey() {
		return key;
	}
	/**
	 * @return How deep the position has been searched.
	 */
	public short getDepth() {
		return depth;
	}
	/**
	 * @return The type of the returned score.
	 */
	public byte getType() {
		return type;
	}
	/**
	 * @return The returned score.
	 */
	public short getScore() {
		return score;
	}
	/**
	 * @return The best move compressed into an int.
	 */
	public int getBestMove() {
		return bestMove;
	}
	/**
	 * @return The age of the entry.
	 */
	public byte getGeneration() {
		return generation;
	}
	/**
	 * @param generation The age of the entry.
	 */
	public void setGeneration(byte generation) {
		this.generation = generation;
	}
	/**
	 * @return Whether the entry is in a 'busy' state.
	 */
	public boolean isBusy() {
		return busy;
	}

	/**
	 * @param busy Whether the entry is in a 'busy' state.
	 */
	public void setBusy(boolean busy) {
		this.busy = busy;
	}
	/**
	 * @param key The 64 bit position hash key.
	 * @param depth How deep the position has been searched.
	 * @param type The type of the returned score.
	 * @param score The returned score.
	 * @param bestMove The best move compressed into an int.
	 * @param generation The age of the entry.
	 * @param busy Whether the entry is in a 'busy' state.
	 */
	void set(long key, short depth, byte type, short score, int bestMove, byte generation, boolean busy) {
		this.key = key;
		this.depth = depth;
		this.type = type;
		this.score = score;
		this.bestMove = bestMove;
		this.generation = generation;
		this.busy = busy;
	}
	/**
	 * XORs the data fields into the key.
	 */
	public void setupKey() {
		key ^= depth^type^score^bestMove;
	}
	@Override
	public long hashKey() {
		return key^depth^type^score^bestMove;
	}
	@Override
	public boolean isEmpty() {
		return key == 0;
	}
	@Override
	public void assume(TTEntry entry) {
		set(entry.key, entry.depth, entry.type, entry.score, entry.bestMove, entry.generation, entry.busy);
	}
	@Override
	public void swap(TTEntry entry) {
		long key = this.key;
		short depth = this.depth;
		byte type = this.type;
		short score = this.score;
		int bestMove = this.bestMove;
		byte generation = this.generation;
		boolean busy = this.busy;
		assume(entry);
		entry.set(key, depth, type, score, bestMove, generation, busy);
	}
	@Override
	public void empty() {
		key = 0;
	}
	@Override
	public int compareTo(TTEntry e) {
		if (generation < e.generation)
			return -1;
		if (type == e.type) {
			if (depth == e.depth) {
				/* To increase the chances of the score being greater than any beta and thus produce more frequent
				 * ready-to-return hash hits... */
				if (type == NodeType.FAIL_HIGH.ordinal())
					return score - e.score;
				// To increase the chances of the score being lower than any alpha.
				else if (type == NodeType.FAIL_LOW.ordinal())
					return e.score - score;
				// Both exact, same depth.
				else
					return 0;
			}
			// Let the search depth determine how valuable the entries are.
			else
				return depth - e.depth;
		} else {
			// If this entry is exact and the other is not, this one is better.
			if (type == NodeType.EXACT.ordinal())
				return 1;
			// If neither of them are exact, let depth determine which one is better.
			else if (e.type != NodeType.EXACT.ordinal())
				return depth - e.depth;
			// If this entry is not exact while the other one is, do not replace.
			else
				return -1;
		}
	}
	@Override
	public String toString() {
		String move = (bestMove == 0) ? null : Move.toMove(bestMove).toString();
		String type = "";
		switch (this.type) {
			case 0:
				type = NodeType.EXACT.toString();
				break;
			case 1:
				type = NodeType.FAIL_HIGH.toString();
				break;
			case 2:
				type = NodeType.FAIL_LOW.toString();
		}
		return String.format("KEY: %s; DEPTH: %d; TYPE: %s; SCORE: %d; MOVE: %s; GENERATION: %d; BUSY: %s",
				key, depth, type, score, move, generation, String.valueOf(busy));
	}
	
}
