package engine;

import util.LongList;
import engine.Board.Square;

/**Some position information--such as castling and en passant rights, fifty-move rule clock, repetitions and the square indices of checkers--is stored
 * in longs so as to make reverting to the previous position when unmaking a  move easier. This enum type contains the ranges of the different bits of
 * information held within the long that can be extracted using simple bit-shifts by the specified numbers contained in the 'shift' field and then
 * AND-ing the shifted values with the numbers contained in the 'mask' field.
 * 
 * @author Viktor
 *
 */
public enum PositionInfo {
	
	WHITE_CASTLING_RIGHTS (0,  3),
	BLACK_CASTLING_RIGHTS (2,  3),
	EN_PASSANT_RIGHTS     (4,  15),
	FIFTY_MOVE_RULE_CLOCK (8,  255),
	REPETITIONS 		  (16, 7),
	CHECK				  (19, 3),
	CHECKER1  			  (21, 127),
	CHECKER2 			  (28, 127);
	
	public final byte shift;		//the bit-index at which the interval designated for the information described by this enum constant is supposed to begin in a move long
	public final long mask;			//the mask with which the information described by this enum constant can be obtained when AND-ed with a move a long right-shifted by the same enum constants 'shift' value

	private PositionInfo(int shift, long mask) {
		this.shift = (byte) shift;
		this.mask = mask;
	}
	/**Returns a human-readable String representation of the position information stored in the long.
	 * 
	 * @param positionInfo
	 * @return
	 */
	public static String toString(long positionInfo) {
		String rep = "";
		long whiteCastlingRights, blackCastlingRights, enPassantRights, fiftyMoveRuleClock, repetitions;
		fiftyMoveRuleClock		= (positionInfo >>> PositionInfo.FIFTY_MOVE_RULE_CLOCK.shift)	& PositionInfo.FIFTY_MOVE_RULE_CLOCK.mask;
		enPassantRights 		= (positionInfo >>> PositionInfo.EN_PASSANT_RIGHTS.shift)		& PositionInfo.EN_PASSANT_RIGHTS.mask;
		whiteCastlingRights 	= (positionInfo >>> PositionInfo.WHITE_CASTLING_RIGHTS.shift)	& PositionInfo.WHITE_CASTLING_RIGHTS.mask;
		blackCastlingRights 	= (positionInfo >>> PositionInfo.BLACK_CASTLING_RIGHTS.shift)	& PositionInfo.BLACK_CASTLING_RIGHTS.mask;
		repetitions				= (positionInfo >>> PositionInfo.REPETITIONS.shift)				& PositionInfo.REPETITIONS.mask;
		switch ((int)((positionInfo >>> PositionInfo.CHECK.shift) & PositionInfo.CHECK.mask)) {
			case 1:
				rep += String.format("%-23s " + Square.toString((int)((positionInfo >>> PositionInfo.CHECKER1.shift) & PositionInfo.CHECKER1.mask)) + "\n", "Checker:");
			break;
			case 2:
				rep += String.format("%-23s " + Square.toString((int)((positionInfo >>> PositionInfo.CHECKER1.shift) & PositionInfo.CHECKER1.mask)) + ", " +
												Square.toString((int)((positionInfo >>> PositionInfo.CHECKER2.shift) & PositionInfo.CHECKER2.mask)) + "\n", "Checker:");
		}
		rep += String.format("%-23s ", "Castling rights:");
		if ((whiteCastlingRights & 1) != 0)
			rep += "K";
		if ((whiteCastlingRights & 2) != 0)
			rep += "Q";
		if ((blackCastlingRights & 1) != 0)
			rep += "k";
		if ((blackCastlingRights & 2) != 0)
			rep += "q";
		if (whiteCastlingRights == 0 && blackCastlingRights == 0)
			rep += "-";
		rep += "\n";
		rep += String.format("%-23s ", "En passant rights:");
		if (enPassantRights == 8)
			rep += "-\n";
		else
			rep += (char)('a' + enPassantRights) + "\n";
		rep += String.format("%-23s " + fiftyMoveRuleClock + "\n", "Fifty-move rule clock:");
		rep += String.format("%-23s " + repetitions + "\n", "Repetitions:");
		return rep;
	}
	/**Prints all position information longs contained in the input parameter to the console.
	 * 
	 * @param moves
	 */
	public static void printPositionInfoToConsole(LongList positionInfoHistory) {
		System.out.println();
		while (positionInfoHistory.hasNext())
			System.out.println(toString(positionInfoHistory.next()));
		System.out.println();
	}
	/**Prints all position information longs contained in the input parameter to the console.
	 * 
	 * @param moves
	 */
	public static void printPositionInfoToConsole(long[] positionInfoHistory) {
		System.out.println();
		for (int i = 0; i < positionInfoHistory.length; i++)
			System.out.println(toString(positionInfoHistory[i]));
		System.out.println();
	}
}
