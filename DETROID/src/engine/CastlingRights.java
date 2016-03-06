package engine;

/**
 * A simple enum type for the representation of a side's castling rights in a position.
 * 
 * @author Viktor
 *
 */
public enum CastlingRights {
	
	NONE,
	SHORT,
	LONG,
	ALL;
	
	public final byte ind;	// Numeric representation of the the castling rights.
	
	private CastlingRights() {
		ind = (byte)ordinal();
	}
	/**
	 * Returns a CastlingRights type based on the argument numeral.
	 * 
	 * @param num
	 * @return
	 */
	public static CastlingRights getByIndex(int num) {
		switch (num) {
			case 0: return NONE; case 1: return SHORT; case 2: return LONG; case 3: return ALL;
			default: throw new IllegalArgumentException();
		}
	}
	/**
	 * Parses a string in FEN notation and returns an array of two containing white's and black's castling rights respectively.
	 * 
	 * @param fen
	 * @return
	 */
	public static CastlingRights[] getInstancesByFEN(String fen) {
		if (fen == null)
			return new CastlingRights[] { null, null };
		if (fen.equals("-"))
			return new CastlingRights[] { CastlingRights.NONE, CastlingRights.NONE };
		CastlingRights whiteCastlingRights, blackCastlingRights;
		if (fen.contains("K")) {
			if (fen.contains("Q"))
				whiteCastlingRights = CastlingRights.ALL;
			else
				whiteCastlingRights = CastlingRights.SHORT;
		}
		else if (fen.contains("Q"))
			whiteCastlingRights = CastlingRights.LONG;
		else
			whiteCastlingRights = CastlingRights.NONE;
		if (fen.contains("k")) {
			if (fen.contains("q"))
				blackCastlingRights = CastlingRights.ALL;
			else
				blackCastlingRights = CastlingRights.SHORT;
		}
		else if (fen.contains("q"))
			blackCastlingRights = CastlingRights.LONG;
		else
			blackCastlingRights = CastlingRights.NONE;
		return new CastlingRights[] { whiteCastlingRights, blackCastlingRights };
	}
	/**
	 * Returns a string representation in FEN notation of two castling right enum types for white and black respectively.
	 * 
	 * @param white
	 * @param black
	 * @return
	 */
	public static String toFEN(CastlingRights white, CastlingRights black) {
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