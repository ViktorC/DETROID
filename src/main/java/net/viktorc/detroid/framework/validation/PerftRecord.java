package net.viktorc.detroid.framework.validation;

/**
 * A performance test record holding a position, a depth, and the correct node count for the test. The entries use the format '[FEN];
 * [depth]; [nodes];'.
 *
 * @author Viktor
 */
public class PerftRecord {

  private final String perftRecord;
  private final String position;
  private final int depth;
  private final long nodes;

  /**
   * Constructs a perft record based on the specified string.
   *
   * @param perftRecord A perft record of the format '[FEN]; [depth]; [nodes];'.
   */
  public PerftRecord(String perftRecord) {
    this.perftRecord = perftRecord;
    String[] parts = perftRecord.split(";");
    if (parts.length < 3) {
      throw new IllegalArgumentException("Illegal perft record format.");
    }
    String position = parts[0].trim();
    int depth = Integer.parseInt(parts[1].trim());
    int nodes = Integer.parseInt(parts[2].trim());
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
  public long getNodes() {
    return nodes;
  }

  @Override
  public String toString() {
    return perftRecord;
  }

}
