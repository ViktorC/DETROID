package engine;

/**A thread-safe table implementation for the killer heuristic.
 * 
 * @author Viktor
 *
 */
public class KillerTable {

	/**A killer heuristic table entry for storing two killer moves compressed into two integers.
	 * 
	 * @author Viktor
	 *
	 */
	public static class KillerTableEntry {
		
		int move1;
		int move2;
		
		public int getMove1() { return move1; }
		public int getMove2() { return move2; }
	}
	
	private KillerTableEntry[] t;
	
	/**Creates a killer table of the given size.
	 * 
	 * @param size The length of the table. Should be the maximum allowed search depth.
	 */
	public KillerTable(int size) {
		t = new KillerTableEntry[size];
		for (int i = 0; i < t.length; i++)
			t[i] = new KillerTableEntry();
	}
	/**Inserts a move into the killer table ensuring distinctness within plies.
	 * 
	 * @param ply The ply from which the move caused the cut-off.
	 * @param m The move that caused the cut-off.
	 * @throws ArrayIndexOutOfBoundsException Does not check whether the ply is within the table's bounds.
	 */
	public void add(int ply, Move m) throws ArrayIndexOutOfBoundsException {
		KillerTableEntry e = t[ply];
		int compM = m.toInt();
		if (e.move1 != compM) {
			synchronized (e) {
				e.move2 = e.move1;
				e.move1 = compM;
			}
		}
	}
	/**Retrieves the killer table entry for the given ply.
	 * 
	 * @param ply The ply for which the killer moves are sought.
	 * @return The killer table entry for the ply.
	 * @throws ArrayIndexOutOfBoundsException Does not check whether the ply is within the table's bounds.
	 */
	public KillerTableEntry retrieve(int ply) throws ArrayIndexOutOfBoundsException {
		return t[ply];
	}
}
