package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.engine.Bitboard.Square;

/**
 * A simple enum type for the representation of a side's en passant rights in a position.
 * 
 * @author Viktor
 *
 */
enum EnPassantRights {
	
	A,
	B,
	C,
	D,
	E,
	F,
	G,
	H,
	NONE;
	
	/**
	 * The difference between the EP right index and the square index of the destination of EP for white.
	 */
	final static byte TO_W_DEST_SQR_IND = Square.A6.ind;
	/**
	 * The difference between the EP right index and the square index of the possible victim of EP for white.
	 */
	final static byte TO_W_VICT_SQR_IND = Square.A5.ind;
	/**
	 * The difference between the EP right index and the square index of the destination of EP for black.
	 */
	final static byte TO_B_DEST_SQR_IND = Square.A3.ind;
	/**
	 * The difference between the EP right index and the square index of the possible victim of EP for black.
	 */
	final static byte TO_B_VICT_SQR_IND = Square.A4.ind;

	/**
	 * Numeric representation of the the en passant rights.
	 */
	final byte ind;
	
	EnPassantRights() {
		ind = (byte) ordinal();
	}
	/**
	 * @param ind The index of the en passant rights type.
	 * @return The en passant rights enum instance.
	 */
	static EnPassantRights getByIndex(int ind) {
		switch (ind) {
			case 0: return A;
			case 1: return B;
			case 2: return C;
			case 3: return D;
			case 4: return E;
			case 5: return F;
			case 6: return G;
			case 7: return H;
			case 8: return NONE;
			default: throw new IllegalArgumentException();
		}
	}
	/**
	 * Returns a string representation.
	 */
	@Override
	public String toString() {
		if (this == NONE)
			return "-";
		return super.toString().toLowerCase();
	}
	
}