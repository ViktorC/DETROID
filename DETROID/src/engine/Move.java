package engine;

import util.*;
import engine.Board.Square;

/**A simple unencapsulated class that provides objects for storing information about moves necessary for making them.
 * 
 * @author Viktor
 *
 */
public class Move {

	int from;	//denotes the index of the origin square
	int to;		//denotes the index of the destination square
	int type;	//denotes the type of the move; 0 - normal, 1 - short castling, 2 - long castling, 3 - en passant, 4 - promotion to queen, 5 - promotion to rook, 6 - promotion to bishop, 7 - promotion to knight
	int value;	//the value assigned to the move at move ordering, or the value returned by the search algorithm based on the evaluator's scoring for the position that the move leads to
	
	public Move() {
		
	}
	public Move(int from, int to) {
		this.from = from;
		this.to = to;
	}
	public Move(int from, int to, int type) {
		this.from = from;
		this.to = to;
		this.type = type;
	}
	/**Returns a move as a String in pseudo-algebraic chess notation for better human-readability.
	 * 
	 * @param move
	 * @return
	 */
	public String toString() {
		String alg;
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
	public static void printMovesToConsole(List<Move> moves) {
		System.out.println();
		while (moves.hasNext())
			System.out.println(moves.next());
		System.out.println();
	}
	/**Prints all moves contained in the input parameter to the console.
	 * 
	 * @param moves
	 */
	public static void printMovesToConsole(Move[] moves) {
		System.out.println();
		for (int i = 0; i < moves.length; i++)
			System.out.println(moves[i]);
		System.out.println();
	}
}
