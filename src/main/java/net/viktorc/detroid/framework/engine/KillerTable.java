package net.viktorc.detroid.framework.engine;


/**
 * A table implementation for the killer heuristic.
 * 
 * @author Viktor
 *
 */
class KillerTable {
	
	private KillerTableEntry[] t;
	
	/**
	 * Creates a killer table of the given size.
	 * 
	 * @param size The length of the table. Should be the maximum allowed search depth.
	 */
	KillerTable(int size) {
		t = new KillerTableEntry[size];
		for (int i = 0; i < t.length; i++)
			t[i] = new KillerTableEntry(0, 0);
	}
	/**
	 * Inserts a move into the killer table ensuring distinctness within plies.
	 * 
	 * @param ply The ply from which the move caused the cut-off.
	 * @param m The move that caused the cut-off.
	 * @throws ArrayIndexOutOfBoundsException Does not check whether the ply is within the table's bounds.
	 */
	void add(int ply, Move m) throws ArrayIndexOutOfBoundsException {
		KillerTableEntry e = t[ply];
		int compM = m.toInt();
		if (e.move1 != compM) {
			e.move2 = e.move1;
			e.move1 = compM;
		}
	}
	/**
	 * Retrieves an entry containing the two (or less) killer moves from the killer table entry for the given ply.
	 * 
	 * @param ply The ply for which the killer moves are sought.
	 * @return The killer move entry from the table entry for the ply.
	 * @throws ArrayIndexOutOfBoundsException Does not check whether the ply is within the table's bounds.
	 */
	KillerTableEntry retrieve(int ply) throws ArrayIndexOutOfBoundsException {
		return t[ply];
	}
	
	/**
	 * A killer heuristic table entry for storing two killer moves compressed into two integers.
	 * 
	 * @author Viktor
	 *
	 */
	static class KillerTableEntry {
		
		private int move1;
		private int move2;
		
		KillerTableEntry(int move1, int move2) {
			this.move1 = move1;
			this.move2 = move2;
		}
		int getMove1() { return move1; }
		int getMove2() { return move2; }
	}
	
}
