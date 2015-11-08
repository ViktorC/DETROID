package engine;

import util.*;
import engine.Search.NodeType;

/**A transposition table entry that stores information about searched positions identified by the key such as the depth of the search, the best move from this
 * position compressed into a short, the score belonging to it, the type of the score, and the age of the entry. One entry takes 144 bits without the object
 * memory overhead.
 * 
 * @author Viktor
 *
 */
public class TTEntry implements HashTable.Entry<TTEntry> {
	
	long key;			// The 64-bit position hash key.
	short depth;		// How deep the position has been searched.
	byte  type;			// The type of the returned score.
	short score;		// The returned score.
	int   bestMove;		// The best move compressed into an int.
	byte  generation;	// The age of the entry.

	public TTEntry(long key, int depth, byte type, int score, int bestMove, byte age) {
		this.key = key;
		this.depth = (short)depth;
		this.type = type;
		this.score = (short)score;
		this.bestMove = bestMove;
		this.generation = age;
	}
	/**Returns whether this entry is more valuable for storing than the input parameter entry.*/
	public boolean betterThan(TTEntry e) {
		if (generation >= e.generation && depth >= e.depth && (type == NodeType.EXACT.ind || e.type != NodeType.EXACT.ind))
			return true;
		return false;
	}
	/**Returns whether this entry is less valuable for storing than the input parameter entry.*/
	public boolean worseThan(TTEntry e) {
		if (generation < e.generation - 2 || depth < e.depth || (e.type == NodeType.EXACT.ind && type != NodeType.EXACT.ind))
			return true;
		return false;
	}
	/**Returns a 64-bit hash code identifying this object.*/
	public long hashKey() {
		return key;
	}
	/**Returns a String representation of the object state.*/
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
