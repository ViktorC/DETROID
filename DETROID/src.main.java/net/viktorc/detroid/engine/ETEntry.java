package net.viktorc.detroid.engine;

import net.viktorc.detroid.util.LossyHashTable;

/**
 * An evaluation hash table entry that stores information about the static evaluation scores of leaf nodes.
 * 
 * @author Viktor
 *
 */
class ETEntry implements LossyHashTable.Entry<ETEntry> {

	/**
	 * The 64-bit position hash key.
	 */
	final long key;
	/**
	 * The evaluation score.
	 */
	final short score;
	/**
	 * The age of the entry.
	 */
	byte generation;

	ETEntry(long key, short score, byte age) {
		this.key = key;
		this.score = score;
		this.generation = age;
	}
	/**
	 * Returns whether this entry is more valuable for storing than the input parameter entry.
	 */
	@Override
	public int compareTo(ETEntry e) {
		if (generation >= e.generation)
			return 1;
		return -1;
	}
	/**
	 * Returns a 64-bit hash code identifying this object.
	 */
	@Override
	public long hashKey() {
		return key;
	}
	
}
