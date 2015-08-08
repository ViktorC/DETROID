package engine;

/**Some position information--such as castling and en passant rights, fifty-move rule clock, repetitions and the square indices of checkers--is stored
 * in longs so as to make reverting to the previous position when unmaking a  move easier. This enum type contains the ranges of the different bits of
 * information held within the long that can be extracted using simple bit-shifts by the specified numbers contained in the 'shift' field and then
 * AND-ing the shifted values with the numbers contained in the 'mask' field.
 * 
 * @author Viktor
 *
 */
public enum PositionInfo {
	
	WHITE_CASTLING_RIGHTS (0,  15),
	BLACK_CASTLING_RIGHTS (4,  15),
	EN_PASSANT_RIGHTS     (8,  15),
	FIFTY_MOVE_RULE_CLOCK (12, 255),
	REPETITIONS 		  (20, 7),
	CHECKER1  			  (23, 127),
	CHECKER2 			  (30, 127);
	
	public final byte shift;		//the bit-index at which the interval designated for the information described by this enum constant is supposed to begin in a move long
	public final long mask;			//the mask with which the information described by this enum constant can be obtained when AND-ed with a move a long right-shifted by the same enum constants 'shift' value

	private PositionInfo(int shift, long mask) {
		this.shift = (byte) shift;
		this.mask = mask;
	}
}
