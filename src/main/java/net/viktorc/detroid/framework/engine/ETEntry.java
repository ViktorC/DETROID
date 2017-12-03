package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.util.Cache.Entry;

/**
 * An evaluation hash table entry that stores information about the static evaluation scores of leaf nodes. It uses Hyatt's lock-less hashing.
 * 
 * @author Viktor
 *
 */
class ETEntry implements Entry<ETEntry> {
	
	/**
	 * The 64-bit position hash key.
	 */
	volatile long key;
	/**
	 * The evaluation score.
	 */
	volatile short score;
	/**
	 * Whether the score stored is exact.
	 */
	volatile boolean isExact;
	/**
	 * The age of the entry.
	 */
	volatile byte generation;

	/**
	 * Sets the state of the instance.
	 * 
	 * @param key
	 * @param score
	 * @param isExact
	 * @param generation
	 */
	void set(long key, short score, boolean isExact, byte generation) {
		this.key = key;
		this.score = score;
		this.isExact = isExact;
		this.generation = generation;
	}
	/**
	 * XORs the data fields into the key.
	 */
	void setupKey() {
		key ^= score^(isExact ? 1 : 0);
	}
	@Override
	public long hashKey() {
		return key^score^(isExact ? 1 : 0);
	}
	@Override
	public boolean isEmpty() {
		return key == 0;
	}
	@Override
	public void assume(ETEntry entry) {
		set(entry.key, entry.score, entry.isExact, entry.generation);
	}
	@Override
	public void swap(ETEntry entry) {
		long key = this.key;
		short score = this.score;
		boolean isExact = this.isExact;
		byte generation = this.generation;
		assume(entry);
		entry.set(key, score, isExact, generation);
	}
	@Override
	public void empty() {
		key = 0;
	}
	@Override
	public int compareTo(ETEntry e) {
		if ((isExact || !e.isExact) && generation >= e.generation)
			return 1;
		return -1;
	}
	@Override
	public String toString() {
		return String.format("KEY: %s; SCORE: %d; EXACT: %s; GENERATION: %d",
				key, score, String.valueOf(isExact), generation);
	}
	
}