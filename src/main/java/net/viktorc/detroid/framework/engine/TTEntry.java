package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.util.Cache.Entry;

/**
 * A transposition table entry that stores information about searched positions identified by the key such as the depth of the search, the best move from this
 * position compressed into a short, the score belonging to it, the type of the score, and the age of the entry. It uses Hyatt's lock-less hashing.
 * 
 * @author Viktor
 *
 */
class TTEntry implements Entry<TTEntry> {
	
	/**
	 * The 64-bit position hash key.
	 */
	volatile long key;
	/**
	 * How deep the position has been searched.
	 */
	volatile short depth;
	/**
	 * The type of the returned score.
	 */
	volatile byte type;
	/**
	 * The returned score.
	 */
	volatile short score;
	/**
	 * The best move compressed into an int.
	 */
	volatile int bestMove;
	/**
	 * The age of the entry.
	 */
	volatile byte generation;
	/**
	 * Whether the entry is in a 'busy' state.
	 */
	volatile boolean busy;
	
	/**
	 * Sets the state of the instance.
	 * 
	 * @param key
	 * @param depth
	 * @param type
	 * @param score
	 * @param bestMove
	 * @param generation
	 * @param busy
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
	void setupKey() {
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
				// To increase the chances of the score being greater than any beta and thus produce more frequent ready-to-return hash hits...
				if (type == NodeType.FAIL_HIGH.ind)
					return score - e.score;
				// To increase the chances of the score being lower than any alpha.
				else if (type == NodeType.FAIL_LOW.ind)
					return e.score - score;
				else // Both exact, same depth.
					return 0;
			} else // Let the search depth determine how valuable the entries are
				return depth - e.depth;
		} else {
			if (type == NodeType.EXACT.ind) // If this entry is exact and the other is not, this one is better.
				return 1;
			else if (e.type != NodeType.EXACT.ind) // If neither of them are exact, let depth determine which one is better.
				return depth - e.depth;
			else // If this entry is not exact while the other one is, do not replace.
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
