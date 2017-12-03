package net.viktorc.detroid.framework.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simplified implementation of the Extended Position Description standard to represent test suite records.
 * 
 * @author Viktor
 *
 */
public class EPDRecord {
	
	protected static final String BEST_MOVE_OP_CODE = "bm ";
	protected static final String ID_OP_CODE = "id ";
	
	protected final String epd;
	protected final String[] ops;
	protected final String id;
	protected final String position;
	protected final List<String> bestMoves;
	
	/**
	 * Constructs an instance based on the specified EPDRecord record.
	 * 
	 * @param epd The EPD record.
	 */
	public EPDRecord(String epd) {
		this.epd = epd;
		String[] parts = epd.split(" ");
		if (parts.length < 4)
			throw new IllegalArgumentException("Illegal EPD format.");
		position = String.join(" ", Arrays.copyOf(parts, 4)).trim();
		String operations = epd.substring(position.length(), epd.length());
		ops = operations.trim().split(";");
		String id = null;
		String[] bestMoves = new String[0];
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
		this.bestMoves = Arrays.asList(bestMoves);
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
	 * Returns an list of the best moves for the position in SAN.
	 * 
	 * @return A list of the best moves in SAN.
	 */
	public List<String> getBestMoves() {
		return new ArrayList<>(bestMoves);
	}
	@Override
	public String toString() {
		return epd;
	}
	
}
