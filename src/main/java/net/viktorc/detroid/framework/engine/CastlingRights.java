package net.viktorc.detroid.framework.engine;

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

	/**
	 * Numeric representation of the the castling rights.
	 */
	final byte ind;
	
	CastlingRights() {
		ind = (byte) ordinal();
	}
	/**
	 * @param ind The index of the castling rights type.
	 * @return The castling rights enum instance.
	 */
	static CastlingRights getByIndex(int ind) {
		switch (ind) {
			case 0: return NONE;
			case 1: return SHORT;
			case 2: return LONG;
			case 3: return ALL;
			default: throw new IllegalArgumentException();
		}
	}
	/**
	 * Returns a string representation in FEN notation of two castling right enum types for white and black
	 * respectively.
	 * 
	 * @param white The castling rights for white.
	 * @param black The castling rights for black.
	 * @return The FEN castling notation.
	 */
	static String toFEN(CastlingRights white, CastlingRights black) {
		if (white == null || black == null)
			return null;
		String out = "";
		switch (white) {
			case SHORT:
				out += "K";
				break;
			case LONG:
				out += "Q";
				break;
			case ALL:
				out += "KQ";
				break;
			case NONE:
				break;
		}
		switch (black) {
			case SHORT:
				out += "k";
				break;
			case LONG:
				out += "q";
				break;
			case ALL:
				out += "kq";
				break;
			case NONE:
				break;
		}
		if (out.isEmpty())
			return "-";
		return out;
	}
	
}