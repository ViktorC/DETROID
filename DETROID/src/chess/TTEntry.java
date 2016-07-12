package chess;

import util.HashTable;
import util.SizeOf;

/**
 * A transposition table entry that stores information about searched positions identified by the key such as the depth of the search, the best move from this
 * position compressed into a short, the score belonging to it, the type of the score, and the age of the entry. One entry takes 144 bits without the object
 * memory overhead.
 * 
 * @author Viktor
 *
 */
public class TTEntry implements HashTable.Entry<TTEntry> {
	
	/**
	 * The 64-bit position hash key.
	 */
	public final long key;
	/**
	 * How deep the position has been searched.
	 */
	public final short depth;
	/**
	 * The type of the returned score.
	 */
	public final byte type;
	/**
	 * The returned score.
	 */
	public final short score;
	/**
	 * The best move compressed into an int.
	 */
	public final int bestMove;
	/**
	 * The age of the entry.
	 */
	byte generation;
	
	/**
	 * The total size of the entry in bytes.
	 */
	public final static int SIZE = (int)SizeOf.roundedSize(SizeOf.OBJ_POINTER.numOfBytes + SizeOf.LONG.numOfBytes + SizeOf.INT.numOfBytes +
			2*SizeOf.SHORT.numOfBytes + 2*SizeOf.BYTE.numOfBytes);

	public TTEntry(long key, short depth, byte type, short score, int bestMove, byte age) {
		this.key = key;
		this.depth = depth;
		this.type = type;
		this.score = score;
		this.bestMove = bestMove;
		this.generation = age;
	}
	/**
	 * Returns whether this entry is more valuable for storing than the input parameter entry.
	 */
	@Override
	public int compareTo(TTEntry e) {
		if (generation < e.generation || depth < e.depth)
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
	/**
	 * Returns a String representation of the object state.
	 */
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
		return String.format("%-17s %2d %-9s %7d  %10s %2d",Long.toHexString(key), depth, type, score, move, generation);
	}
}