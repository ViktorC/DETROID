package engine;

import util.*;
import util.Comparable;

/**A transposition table entry that stores information about searched positions identified by the key such as the depth of the search, the best move from this
 * position compressed into a short, the score belonging to it, the type of the score, and the age of the entry. One entry takes merely 128 bits without the
 * object memory overhead.
 * 
 * @author Viktor
 *
 */
public class TTEntry extends HashTable.Entry implements Comparable<TTEntry> {
	
	public final static byte TYPE_EXACT = 0;
	public final static byte TYPE_FAIL_HIGH = 1;
	public final static byte TYPE_FAIL_LOW = 2;
	
	long  key;
	short depth;
	byte  type;
	short score;
	short bestMove;
	byte  age;

	/**Returns whether this entry is more valuable for storing than the input parameter entry.*/
	public boolean greaterThan(TTEntry e) {
		if (age <= e.age && depth >= e.depth && (type == TYPE_EXACT || type == e.type))
			return true;
		return false;
	}
	/**Returns whether this entry is less valuable for storing than the input parameter entry.*/
	public boolean smallerThan(TTEntry e) {
		if (age > e.age || depth < e.depth || (e.type == TYPE_EXACT && type != TYPE_EXACT))
			return true;
		return false;
	}
}
