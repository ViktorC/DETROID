package engine;

import util.*;

/**Moves are stored in longs and this enum type contains the ranges of the different bits of information held within the long that can be extracted
 * using simple bit-shifts by the specified numbers contained in the 'shift' field and then AND-ing the shifted values with the numbers contained
 * in the 'mask' field.
 * 
 * @author Viktor
 *
 */
public enum Move {
	
	FROM 									(0,  63),		//denotes the index of the origin square
	TO										(6,  63),		//denotes the index of the destination square
	MOVED_PIECE 							(12, 15),		//denotes the type of the moved piece according to Board.Piece
	CAPTURED_PIECE 							(16, 15),		//denotes the type of the captured piece according to Board.Piece, 0 means no piece has been captured
	TYPE									(20, 7),		//denotes the type of the move; 0 - normal, 1 - short castling, 2 - long castling, 3 - en passant, 4 - promotion to queen, 5 - promotion to rook, 6 - promotion to bishop, 7 - promotion to knight
	PREVIOUS_WHITE_CASTLING_RIGHTS	 		(23, 3),	
	PREVIOUS_BLACK_CASTLING_RIGHTS	 		(25, 3),
	PREVIOUS_ENPASSANT_RIGHTS		 		(27, 15),
	PREVIOUS_CHECK					 		(31, 1),
	PREVIOUS_FIFTY_MOVE_RULE_CLOCK			(32, 127),
	PREVIOUS_REPETITIONS			 		(39, 7),
	VALUE							 		(42, 2097151);
	
	
	final byte shift;		//the bit-index at which the interval designated for the information described by this enum constant is supposed to begin in a move long
	final long  mask;		//the mask with which the information described by this enum constant can be obtained when AND-ed with a move a long right-shifted by the same enum constants 'shift' value
	
	private Move(int shift, long mask) {
		this.shift = (byte) shift;
		this.mask = mask;
	}
	/**Returns the bit-index at which the interval designated for the information described by this enum constant is supposed to begin in a move long.*/
	public byte getShift() {
		return this.shift;
	}
	/**Returns the mask with which the information described by this enum constant can be obtained when AND-ed with a move a long right-shifted by the same enum constants 'shift' value.*/
	public long getMask() {
		return this.mask;
	}
	/**Returns a move as a String in pseudo-algebraic chess notation for better human-readability.
	 * 
	 * @param move
	 * @return
	 */
	public static String pseudoAlgebraicNotation(long move) {
		String alg, movedPiece, capture, originFile, originRank, destFile, destRank;
		int from 			= (int)((move >>> Move.FROM.shift)		 	  & Move.FROM.mask);
		int to	 			= (int)((move >>> Move.TO.shift) 			  & Move.TO.mask);
		int moved			= (int)((move >>> Move.MOVED_PIECE.shift) 	  & Move.MOVED_PIECE.mask);
		int captured	 	= (int)((move >>> Move.CAPTURED_PIECE.shift)  & Move.CAPTURED_PIECE.mask);
		int type			= (int)((move >>> Move.TYPE.shift)  		  & Move.TYPE.mask);
		if (type == 1)
			return "0-0";
		else if (type == 2)
			return "0-0-0";
		originRank	= Integer.toString(from/8 + 1);
		originFile	= Character.toString((char)(from%8 + 'a'));
		destRank	= Integer.toString(to/8 + 1);
		destFile	= Character.toString((char)(to%8 + 'a'));
		if (moved > 6)
			moved -= 6;
		switch (moved) {
			case 1:
				movedPiece = "K";
			break;
			case 2:
				movedPiece = "Q";
			break;
			case 3:
				movedPiece = "R";
			break;
			case 4:
				movedPiece = "B";
			break;
			case 5:
				movedPiece = "N";
			break;
			default:
				movedPiece = "";
		}
		if (captured == 0)
			capture = "";
		else
			capture = "x";
		alg = movedPiece + originFile + originRank + capture + destFile + destRank;
		switch (type) {
			case 3:
				return alg + "e.p.";
			case 4:
				return alg + "=Q";
			case 5:
				return alg + "=R";
			case 6:
				return alg + "=B";
			case 7:
				return alg + "=N";
			default:
				return alg;
		}
	}
	/**Prints all moves contained in the input parameter to the console.
	 * 
	 * @param moves
	 */
	public static void printMovesToConsole(LongList moves) {
		System.out.println();
		while (moves.hasNext())
			System.out.println(pseudoAlgebraicNotation(moves.next()));
		System.out.println();
	}
	/**Prints all moves contained in the input parameter to the console.
	 * 
	 * @param moves
	 */
	public static void printMovesToConsole(long[] moves) {
		System.out.println();
		for (int i = 0; i < moves.length; i++)
			System.out.println(pseudoAlgebraicNotation(moves[i]));
		System.out.println();
	}
}
