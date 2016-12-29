package main.java.engine;

/**
 * A simple enum type for the representation of a side's castling rights in a position.
 * 
 * @author Viktor
 *
 */
enum CastlingRights {
	
	NONE,
	SHORT,
	LONG,
	ALL;
	
	final byte ind;	// Numeric representation of the the castling rights.
	
	private CastlingRights() {
		ind = (byte)ordinal();
	}
	/**
	 * Returns a CastlingRights type based on the argument numeral.
	 * 
	 * @param ind
	 * @return
	 */
	static CastlingRights getByIndex(int ind) {
		switch (ind) {
			case 0: return NONE; case 1: return SHORT; case 2: return LONG; case 3: return ALL;
			default: throw new IllegalArgumentException();
		}
	}
	/**
	 * Returns a string representation in FEN notation of two castling right enum types for white and black respectively.
	 * 
	 * @param white
	 * @param black
	 * @return
	 */
	static String toFEN(CastlingRights white, CastlingRights black) {
		if (white == null || black == null)
			return null;
		String out = "";
		switch (white) {
			case NONE:
			break;
			case SHORT:
				out += "K";
			break;
			case LONG:
				out += "Q";
			break;
			case ALL:
				out += "KQ";
		}
		switch (black) {
			case NONE:
			break;
			case SHORT:
				out += "k";
			break;
			case LONG:
				out += "q";
			break;
			case ALL:
				out += "kq";
		}
		if (out.equals(""))
			return "-";
		return out;
	}
}