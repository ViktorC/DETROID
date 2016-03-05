package engine;

import util.HashTable;
import util.SizeOf;
import engine.Search.NodeType;

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

	public TTEntry(long key, int depth, byte type, int score, int bestMove, byte age) {
		this.key = key;
		this.depth = (short)depth;
		this.type = type;
		this.score = (short)score;
		this.bestMove = bestMove;
		this.generation = age;
	}
	/**
	 * Returns whether this entry is more valuable for storing than the input parameter entry.
	 */
	@Override
	public boolean betterThan(TTEntry e) {
		if (key == e.key) {
			if (depth >= e.depth) {
				if (type == NodeType.EXACT.ind)
					return true;
				else if (type == e.type) {
					if (type == NodeType.FAIL_LOW.ind)
						return score <= e.score;
					else
						return score >= e.score;
				}
				else
					return e.type != NodeType.EXACT.ind;
			}
			return false;
		}
		else
			return generation >= e.generation && depth >= e.depth && (type == NodeType.EXACT.ind ||  e.type != NodeType.EXACT.ind);
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