package engine;

import util.HashTable;
import util.SizeOf;

/**
 * An evaluation hash table entry that stores information about the static evaluation scores of leaf nodes.
 * 
 * @author Viktor
 *
 */
public class ETEntry implements HashTable.Entry<ETEntry> {

	/**
	 * The 64-bit position hash key.
	 */
	public final long key;
	/**
	 * The evaluation score.
	 */
	public final short score;
	/**
	 * The type of the stored score (i.e. lower bound, exact, or upper bound).
	 */
	public final byte type;
	/**
	 * The age of the entry.
	 */
	byte generation;

	/**
	 * The total size of the entry in bytes.
	 */
	public final static int SIZE = (int)SizeOf.roundedSize(SizeOf.OBJ_POINTER.numOfBytes + SizeOf.LONG.numOfBytes +
			SizeOf.SHORT.numOfBytes + 2*SizeOf.BYTE.numOfBytes);

	public ETEntry(long key, short score, byte type, byte age) {
		this.key = key;
		this.score = score;
		this.type = type;
		this.generation = age;
	}
	/**
	 * Returns whether this entry is more valuable for storing than the input parameter entry.
	 */
	@Override
	public int compareTo(ETEntry e) {
		if (generation < e.generation)
			return -1;
		if (type == e.type) {
			// To increase the chances of the score being greater than any beta and thus produce more frequent ready-to-return hash hits...
			if (type == NodeType.FAIL_HIGH.ind)
				return score - e.score;
			// To increase the chances of the score being lower than any alpha.
			else if (type == NodeType.FAIL_LOW.ind)
				return e.score - score;
			else
				return 1;
		}
		else
			return type == NodeType.EXACT.ind || e.type != NodeType.EXACT.ind ? 1 : -1;
	}
	/**
	 * Returns a 64-bit hash code identifying this object.
	 */
	@Override
	public long hashKey() {
		return key;
	}
}
