package engine;

public enum Move {
	
	FROM 									(0,  63),
	TO	 									(6,  63),
	MOVED_PIECE 							(12, 15),
	CAPTURED_PIECE 							(16, 15),
	TYPE									(20, 7),
	PREVIOUS_WHITE_CASTLING_RIGHTS	 		(23, 3),
	PREVIOUS_BLACK_CASTLING_RIGHTS	 		(25, 3),
	PREVIOUS_ENPASSANT_RIGHTS		 		(27, 15),
	PREVIOUS_CHECK					 		(31, 1),
	PREVIOUS_FIFTY_MOVE_RULE_INDEX			(32, 127),
	PREVIOUS_LAST_IRREVERSIBLE_MOVE_INDEX 	(39, 1023),
	PREVIOUS_REPETITIONS			 		(49, 7),
	VALUE							 		(52, 2047);
	
	
	final byte shift;
	final int  mask;
	
	private Move(int shift, int mask) {
		this.shift = (byte) shift;
		this.mask = mask;
	}
	public byte getShift() {
		return this.shift;
	}
	public int getMask() {
		return this.mask;
	}
}