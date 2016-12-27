package testing;

import java.util.Arrays;

/**
 * A simplified implementation of the Extended Position Description standard to represent test suite records.
 * 
 * @author Viktor
 *
 */
public class EPD {
	
	private static final String BEST_MOVE_OP_CODE = "bm";
	private static final String ID_OP_CODE = "id";
	
	private final String id;
	private final String position;
	private final String[] bestMoves;
	
	/**
	 * Constructs an instance based on the specified EPD record.
	 * 
	 * @param epd The EPD record.
	 */
	public EPD(String epd) {
		String[] parts = epd.split(" ");
		if (parts.length < 4)
			throw new IllegalArgumentException("Illegal EPD format.");
		position = String.join(" ", Arrays.copyOf(parts, 4)).trim();
		String operations = epd.substring(position.length(), epd.length());
		String[] ops = operations.trim().split(";");
		String id = null;
		String[] bestMoves = null;
		for (String op : ops) {
			op = op.trim();
			if (op.startsWith(BEST_MOVE_OP_CODE)) {
				op = op.substring(BEST_MOVE_OP_CODE.length(), op.length()).trim();
				bestMoves = op.split(" ");
			} else if (op.startsWith(ID_OP_CODE)) {
				op = op.substring(ID_OP_CODE.length(), op.length()).trim();
				id = op.trim().replace("\"", "");
			}
		}
		this.id = id;
		this.bestMoves = bestMoves;
	}
	/**
	 * Returns the ID of the record without the enclosing quotation marks.
	 * 
	 * @return The ID of the record without the enclosing quotation marks.
	 */
	public String getId() {
		return id;
	}
	/**
	 * Returns the position in FEN.
	 * 
	 * @return The position in FEN.
	 */
	public String getPosition() {
		return position;
	}
	/**
	 * Returns an array of the best moves for the position in SAN.
	 * 
	 * @return An array of the best moves in SAN.
	 */
	public String[] getBestMoves() {
		return bestMoves == null ? null : Arrays.copyOf(bestMoves, bestMoves.length);
	}

}
