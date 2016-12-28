package testing;

/**
 * A performance test record holding a position, a depth, and a node count for the test.
 * 
 * @author Viktor
 *
 */
public class PerftRecord {

	private final String position;
	private final int depth;
	private final int nodes;
	
	/**
	 * Constructs a perft record based on the specified parameters.
	 * 
	 * @param position The position on which the test shall be run in FEN.
	 * @param depth The depth at which the leaf nodes shall be counted.
	 * @param nodes The correct leaf node count at the given depth in the given position.
	 */
	public PerftRecord(String position, int depth, int nodes) {
		this.position = position;
		this.depth = depth;
		this.nodes = nodes;
	}
	/**
	 * Returns the test position in FEN.
	 * 
	 * @return The test position in FEN.
	 */
	public String getPosition() {
		return position;
	}
	/**
	 * Returns the depth at which the leaf nodes are to be counted.
	 * 
	 * @return The depth at which the leaf nodes are to be counted.
	 */
	public int getDepth() {
		return depth;
	}
	/**
	 * Returns the correct node count.
	 * 
	 * @return The correct node count.
	 */
	public int getNodes() {
		return nodes;
	}
	
	
}
