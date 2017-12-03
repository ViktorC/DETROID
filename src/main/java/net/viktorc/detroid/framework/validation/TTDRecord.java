package net.viktorc.detroid.framework.validation;

/**
 * A time-to-depth test record containing a position and the depth to which it is to be searched. It is used 
 * for time-to-depth speedup measurements for SMP.
 * 
 * @author Viktor
 *
 */
public class TTDRecord {

	private final String ttdRecord;
	private final String position;
	private final int depth;
	
	/**
	 * Constructs a time-to-depth test record based on the specified string.
	 * 
	 * @param ttdRecord A time-to-depth test record of the format '[FEN]; [depth]'.
	 */
	public TTDRecord(String ttdRecord) {
		this.ttdRecord = ttdRecord;
		String[] parts = ttdRecord.split(";");
		if (parts.length != 2)
			throw new IllegalArgumentException("Illegal TTD record format.");
		String position = parts[0].trim();
		int depth = Integer.parseInt(parts[1].trim());
		this.position = position;
		this.depth = depth;
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
	 * Returns the depth to which the position is to be searched.
	 * 
	 * @return The depth to which the position is to be searched.
	 */
	public int getDepth() {
		return depth;
	}
	@Override
	public String toString() {
		return ttdRecord;
	}
	
}
