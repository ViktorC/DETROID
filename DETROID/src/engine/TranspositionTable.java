package engine;

import engine.Search.NodeType;
import util.HashTable;
import util.SizeOf;

public class TranspositionTable extends HashTable<TranspositionTable.TTEntry> {

	/**
	 * A transposition table entry that stores information about searched positions identified by the key such as the depth of the search, the best move from this
	 * position compressed into a short, the score belonging to it, the type of the score, and the age of the entry. One entry takes 144 bits without the object
	 * memory overhead.
	 * 
	 * @author Viktor
	 *
	 */
	public static class TTEntry implements HashTable.Entry<TTEntry> {
		
		/**
		 * The 64-bit position hash key.
		 */
		final long  key;
		/**
		 * How deep the position has been searched.
		 */
		final short depth;
		/**
		 * The type of the returned score.
		 */
		final byte  type;
		/**
		 * The returned score.
		 */
		final short score;
		/**
		 * The best move compressed into an int.
		 */
		final int   bestMove;
		/**
		 * 
		 */
		final byte  generation;	// The age of the entry.
		
		/**
		 * The total size of the entry in bytes.
		 */
		public final static int SIZE = (int)SizeOf.roundedSize(SizeOf.POINTER.numOfBytes + SizeOf.LONG.numOfBytes + SizeOf.INT.numOfBytes +
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
			return depth >= e.depth && (type == NodeType.EXACT.ind || e.type != NodeType.EXACT.ind);
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

	public TranspositionTable() {
		super(-1, TTEntry.SIZE);
	}
	public TranspositionTable(int sizeMB) {
		super(sizeMB, TTEntry.SIZE);
	}
	@Override
	public long size() {
		return SizeOf.roundedSize(baseSize());
	}
}
