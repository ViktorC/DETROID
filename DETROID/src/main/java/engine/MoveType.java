package main.java.engine;

/**
 * An enum type defining the seven move types the engine handles differently.
 * 
 * @author Viktor
 *
 */
enum MoveType {
	
	NORMAL,
	SHORT_CASTLING,
	LONG_CASTLING,
	EN_PASSANT,
	PROMOTION_TO_QUEEN,
	PROMOTION_TO_ROOK,
	PROMOTION_TO_BISHOP,
	PROMOTION_TO_KNIGHT;
	
	final byte ind;	// A numeric representation of the move type; to avoid the overhead of calling the ordinal function.
	
	private MoveType() {
		ind = (byte)ordinal();
	}
	
}