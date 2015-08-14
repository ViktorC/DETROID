package engine;

import engine.Board.Square;
import util.*;

/**Moves are stored in ints and this enum type contains the ranges of the different bits of information held within the int that can be extracted
 * using simple bit-shifts by the specified numbers contained in the 'shift' field and then AND-ing the shifted values with the numbers contained
 * in the 'mask' field.
 * 
 * @author Viktor
 *
 */
public enum Move {
	
	VALUE			(0,  0b10000000000000001111111111111111),	//the value assigned to the move at move ordering, or the value returned by the search algorithm based on the evaluator's scoring for the position that the move leads to; it's the first 'field' in the int and it owns the last bit so it can store negative values
	FROM 			(16, 63),									//denotes the index of the origin square
	TO				(22, 63),									//denotes the index of the destination square
	TYPE			(28, 7);									//denotes the type of the move; 0 - normal, 1 - short castling, 2 - long castling, 3 - en passant, 4 - promotion to queen, 5 - promotion to rook, 6 - promotion to bishop, 7 - promotion to knight
	
	public final byte shift;		//the bit-index at which the interval designated for the information described by this enum constant is supposed to begin in a move long
	public final int  mask;			//the mask with which the information described by this enum constant can be obtained when AND-ed with a move a long right-shifted by the same enum constants 'shift' value
	
	private Move(int shift, int mask) {
		this.shift = (byte) shift;
		this.mask = mask;
	}
	/**Returns a move as a String in pseudo-algebraic chess notation for better human-readability.
	 * 
	 * @param move
	 * @return
	 */
	public static String pseudoAlgebraicNotation(int move) {
		String alg;
		int from 			= (int)((move >>> Move.FROM.shift)		 	  & Move.FROM.mask);
		int to	 			= (int)((move >>> Move.TO.shift) 			  & Move.TO.mask);
		int type			= (int)((move >>> Move.TYPE.shift)  		  & Move.TYPE.mask);
		if (type == 1)
			return "0-0";
		else if (type == 2)
			return "0-0-0";
		alg = Square.toString(from).toLowerCase() + Square.toString(to).toLowerCase();
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
	public static void printMovesToConsole(IntList moves) {
		System.out.println();
		while (moves.hasNext())
			System.out.println(pseudoAlgebraicNotation(moves.next()));
		System.out.println();
	}
	/**Prints all moves contained in the input parameter to the console.
	 * 
	 * @param moves
	 */
	public static void printMovesToConsole(int[] moves) {
		System.out.println();
		for (int i = 0; i < moves.length; i++)
			System.out.println(pseudoAlgebraicNotation(moves[i]));
		System.out.println();
	}
}
