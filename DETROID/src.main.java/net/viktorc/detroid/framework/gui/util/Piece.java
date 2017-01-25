package net.viktorc.detroid.framework.gui.util;

/**
 * An enumeration of chess piece types and the corresponding Unicode values.
 * 
 * @author Viktor
 *
 */
public enum Piece {

	W_KING('K', '\u2654'),
	W_QUEEN('Q', '\u2655'),
	W_ROOK('R', '\u2656'),
	W_BISHOP('B', '\u2657'),
	W_KNIGHT('N', '\u2658'),
	W_PAWN('P', '\u2659'),
	
	B_KING('k', '\u265A'),
	B_QUEEN('q', '\u265B'),
	B_ROOK('r', '\u265C'),
	B_BISHOP('b', '\u265D'),
	B_KNIGHT('n', '\u265E'),
	B_PAWN('p', '\u265F');
	
	private final char fenNote;
	private final char code;
	
	private Piece(char fenNote, char code) {
		this.fenNote = fenNote;
		this.code = code;
	}
	/**
	 * Returns the FEN sign of the given piece.
	 * 
	 * @return The FEN note of the piece.
	 */
	public char getFENNote() {
		return fenNote;
	}
	/**
	 * Returns the Unicode code of the given piece.
	 * 
	 * @return The Unicode character code of the piece.
	 */
	public char getCode() {
		return code;
	}
	/**
	 * Returns the piece whose Unicode code matches the parameter.
	 * 
	 * @param unicode The Unicode character code of the sought piece.
	 * @return The matching piece or null if there is no match.
	 */
	public static Piece getByUnicode(char unicode) {
		for (Piece p : values()) {
			if (p.code == unicode)
				return p;
		}
		return null;
	}
	/**
	 * Returns the piece whose FEN sign matches the parameter.
	 * 
	 * @param fenNote The FEN sign of the sought piece.
	 * @return The matching piece or null if there is no match.
	 */
	public static Piece getByFENNote(char fenNote) {
		for (Piece p : values()) {
			if (p.fenNote == fenNote)
				return p;
		}
		return null;
	}
	
}
