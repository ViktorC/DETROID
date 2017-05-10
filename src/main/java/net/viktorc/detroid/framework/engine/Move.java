package net.viktorc.detroid.framework.engine;

/**
 * A simple class that provides objects for storing information about moves necessary for making them.
 * 
 * @author Viktor
 *
 */
class Move implements Comparable<Move> {
	
	// Mask and shift values for encoding contents of a Move object into an int; and vica versa.
	private static final byte SHIFT_TO = 6;
	private static final byte SHIFT_MOVED = 12;
	private static final byte SHIFT_CAPTURED = 16;
	private static final byte SHIFT_TYPE = 20;
	private static final byte MASK_FROM = 63;
	private static final byte MASK_TO = 63;
	private static final byte MASK_MOVED = 15;
	private static final byte MASK_CAPTURED = 15;
	
	/**
	 * Represents a null move instance.
	 */
	static final Move NULL_MOVE = new Move();
	
	/**
	 * The index of the origin square.
	 */
	final byte from;
	/**
	 * The index of the destination square.
	 */
	final byte to;
	/**
	 * The numeric notation of the type of piece moved.
	 */
	final byte movedPiece;
	/**
	 * The numeric notation of the type of piece captured; if none, 0.
	 */
	final byte capturedPiece;
	/**
	 * The type of the move; 0 - normal, 1 - short castling, 2 - long castling, 3 - en passant, 4 - promotion to queen,
	 * 5 - promotion to rook, 6 - promotion to bishop, 7 - promotion to knight.
	 */
	final byte type;
	/**
	 * The value assigned to the move at move ordering (or the value returned by the search algorithm based on the evaluator's
	 * scoring for the position that the move leads to).
	 */
	short value;
	
	Move() {
		from = 0;
		to = 0;
		movedPiece = 0;
		capturedPiece = 0;
		type = 0;
	}
	Move(byte from, byte to, byte movedPiece, byte capturedPiece, byte type) {
		this.from = from;
		this.to = to;
		this.movedPiece = movedPiece;
		this.capturedPiece = capturedPiece;
		this.type = type;
	}
	/**
	 * Parses a move encoded in a 32 bit integer.
	 */
	static Move toMove(int move) {
		byte from, to, movedPiece, capturedPiece, type;
		from = (byte)(move & MASK_FROM);
		to = (byte)((move >>> SHIFT_TO) & MASK_TO);
		movedPiece = (byte)((move >>> SHIFT_MOVED) & MASK_MOVED);
		capturedPiece = (byte)((move >>> SHIFT_CAPTURED) & MASK_CAPTURED);
		type = (byte)(move >>> SHIFT_TYPE);
		return new Move(from, to, movedPiece, capturedPiece, type);
	}
	/**
	 * Returns a move as a 32 bit integer with information on the state of the object stored in designated bits, except for the score.
	 * Useful in memory sensitive applications like the transposition table as it identifies a 30 byte Move object in merely 4 bytes.
	 *
	 * @return
	 */
	int toInt() {
		return (from | (to << SHIFT_TO) | (movedPiece << SHIFT_MOVED) |
			   (capturedPiece << SHIFT_CAPTURED) | (type << SHIFT_TYPE));
	}
	/**
	 * Returns whether the move is a material move or not.
	 * 
	 * @param move
	 * @return
	 */
	boolean isMaterial() {
		return capturedPiece != Piece.NULL.ind || type >= MoveType.PROMOTION_TO_QUEEN.ind;
	}
	/**
	 * Returns whether this move is equal to the input parameter move. The assigned theoretical value is disregarded as it is highly
	 * context dependent.
	 * 
	 * @param m
	 * @return
	 */
	boolean equals(Move m) {
		return (from == m.from && to == m.to && movedPiece == m.movedPiece && capturedPiece == m.capturedPiece && type == m.type);
	}
	/**
	 * Returns whether this move is equal to the input parameter 'compressed' move.
	 * 
	 * @param m
	 * @return
	 */
	boolean equals(int m) {
		return (from == (m & MASK_FROM) && to == ((m >>> SHIFT_TO) & MASK_FROM) &&
			movedPiece == ((m >>> SHIFT_MOVED) & MASK_MOVED) &&
			capturedPiece == ((m >>> SHIFT_CAPTURED) & MASK_CAPTURED) &&
			type == (m >>> SHIFT_TYPE));
	}
	@Override
	public boolean equals(Object o) {
		return o instanceof Move ? equals((Move) o) : o instanceof Integer ? equals((Integer) o) : false;
	}
	@Override
	public int hashCode() {
		return toInt();
	}
	/**
	 * Returns whether the owner Move instance's value field holds a greater number than the parameter Move instance's.
	 */
	@Override
	public int compareTo(Move m) throws NullPointerException {
		return value - m.value;
	}
	/**
	 * Returns a move as a String in Pure Algebraic Coordinate Notation for better human-readability.
	 *
	 * @return
	 */
	@Override
	public String toString() {
		String pacn, originFile, originRank, destFile, destRank;
		originRank = Integer.toString(from/8 + 1);
		originFile = Character.toString((char)(from%8 + 'a'));
		destRank = Integer.toString(to/8 + 1);
		destFile = Character.toString((char)(to%8 + 'a'));
		pacn = originFile + originRank + destFile + destRank;
		switch (type) {
			case 4:
				return pacn + "q";
			case 5:
				return pacn + "r";
			case 6:
				return pacn + "b";
			case 7:
				return pacn + "n";
			default:
				return pacn;
		}
	}
	
}