package engine;

import engine.Board.Square;

/**
 * A simple enum type for the representation of a side's en passant rights in a position.
 * 
 * @author Viktor
 *
 */
public enum EnPassantRights {
	
	A, B, C, D, E, F, G, H,
	NONE;
	
	public final byte ind;										// Numeric representation of the the en passant rights.
	public final static byte TO_W_DEST_SQR_IND = Square.A6.ind;	// The difference between the en passant right index and the square index of the destination of en passant for white.
	public final static byte TO_W_VICT_SQR_IND = Square.A5.ind;	// The difference between the en passant right index and the square index of the possible vicim of en passant for white.
	public final static byte TO_B_DEST_SQR_IND = Square.A3.ind;	// The difference between the en passant right index and the square index of the destination of en passant for black.
	public final static byte TO_B_VICT_SQR_IND = Square.A4.ind;	// The difference between the en passant right index and the square index of the possible vicim of en passant for black.
	
	private EnPassantRights() {
		ind = (byte)ordinal();
	}
	/**
	 * Returns a EnPassantRights type based on the argument numeral.
	 * 
	 * @param num
	 * @return
	 */
	public static EnPassantRights getByIndex(int num) {
		switch (num) {
			case 0: return A; case 1: return B; case 2: return C; case 3: return D; case 4: return E;
			case 5: return F; case 6: return G; case 7: return H; case 8: return NONE;
			default: throw new IllegalArgumentException();
		}
	}
	/**
	 * Parses a string in FEN notation and returns an EnPassantRights type.
	 * 
	 * @param fen
	 * @return
	 */
	public static EnPassantRights getByFEN(String fen) {
		if (fen == null || fen.length() < 2)
			return null;
		if (fen.equals("-"))
			return NONE;
		return values()[fen.toLowerCase().charAt(0) - 'a'];
	}
	/**
	 * Returns a string representation.
	 */
	@Override
	public String toString() {
		if (this == NONE)
			return "-";
		else
			return super.toString().toLowerCase();
	}
}