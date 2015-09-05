package engine;

import util.*;

/**A transposition table entry that stores information about searched positions identified by the key such as the depth of the search, the best move from this
 * position compressed into a short, the score belonging to it, the type of the score, and the age of the entry. One entry takes 144 bits without the object
 * memory overhead.
 * 
 * @author Viktor
 *
 */
public class TTEntry extends HashTable.Entry<TTEntry> {
	
	public final static byte TYPE_EXACT = 0;
	public final static byte TYPE_FAIL_HIGH = 1;
	public final static byte TYPE_FAIL_LOW = 2;
	
	short depth;		//how deep the position has been searched
	byte  type;			//the type of the returned score
	short score;		//the returned score
	int   bestMove;		//the best move compressed into a short
	byte  generation;	//the age of the entry

	public TTEntry(long key, int depth, byte type, int score, int bestMove, byte age) {
		this.key = key;
		this.depth = (short)depth;
		this.type = type;
		this.score = (short)score;
		this.bestMove = bestMove;
		this.generation = age;
	}
	/**Returns whether this entry is more valuable for storing than the input parameter entry.*/
	public boolean greaterThan(TTEntry e) {
		if (generation >= e.generation && depth >= e.depth && (type == TYPE_EXACT || e.type != TYPE_EXACT))
			return true;
		return false;
	}
	/**Returns whether this entry is less valuable for storing than the input parameter entry.*/
	public boolean smallerThan(TTEntry e) {
		if (generation < e.generation - 2 || depth < e.depth || (e.type == TYPE_EXACT && type != TYPE_EXACT))
			return true;
		return false;
	}
	/**Returns a String representation of the object state.*/
	public String toString() {
		String move = (bestMove == 0) ? null : Move.toMove(bestMove).toString();
		String type = "";
		switch (this.type) {
			case 0:
				type = "EXACT";
			break;
			case 1:
				type = "FAIL_HIGH";
			break;
			case 2:
				type = "FAIL_LOW";
		}
		return String.format("%-17s %2d %-9s %7d  %10s %2d",Long.toHexString(key), depth, type, score, move, generation);
	}
}
