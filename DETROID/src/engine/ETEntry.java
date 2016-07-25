package engine;

import util.LossyHashTable;
import util.SizeOf;

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
	public final long key;
	/**
	 * The evaluation score.
	 */
	public final short score;
	/**
	 * Whether the score stored is exact.
	 */
	public final boolean isExact;
	/**
	 * The age of the entry.
	 */
	byte generation;

	/**
	 * The total size of the entry in bytes.
	 */
	public final static int SIZE = (int)SizeOf.roundedSize(SizeOf.OBJ_POINTER.numOfBytes + SizeOf.LONG.numOfBytes +
			SizeOf.SHORT.numOfBytes + 2*SizeOf.BYTE.numOfBytes);

	public ETEntry(long key, short score, boolean isExact, byte age) {
		this.key = key;
		this.score = score;
		this.isExact = isExact;
		this.generation = age;
	}
	/**
	 * Returns whether this entry is more valuable for storing than the input parameter entry.
	 */
	@Override
	public int compareTo(ETEntry e) {
		if ((isExact || !e.isExact) && generation >= e.generation)
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
