package engine;

import util.List;
import util.Comparable;
import engine.Bitboard.Square;

/**A simple unencapsulated class that provides objects for storing information about moves necessary for making them.
 * 
 * @author Viktor
 *
 */
public class Move implements Comparable<Move> {
	
	private static byte SHIFT_TO = 6;
	private static byte SHIFT_TYPE = 12;
	private static byte MASK_FROM_TO = 63;

	int from;	//denotes the index of the origin square
	int to;		//denotes the index of the destination square
	int type;	//denotes the type of the move; 0 - normal, 1 - short castling, 2 - long castling, 3 - en passant, 4 - promotion to queen, 5 - promotion to rook, 6 - promotion to bishop, 7 - promotion to knight
	int value;	//the value assigned to the move at move ordering, or the value returned by the search algorithm based on the evaluator's scoring for the position that the move leads to
	
	public Move() {
	}
	public Move(int score) {
		value = score;
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
	/**Parses a move encoded in a 16 bit integer.*/
	public static Move toMove(short move) {
		Move m = new Move();
		m.from = move & MASK_FROM_TO;
		m.to = (move >>> SHIFT_TO) & MASK_FROM_TO;
		m.type = move >>> SHIFT_TYPE;
		return m;
	}
	/**Returns the difference of the owner and the parameter Move instances' value fields.*/
	public int compareTo(Move m) throws NullPointerException {
		return value - m.value;
	}
	/**Returns whether the owner Move instance's value field holds a greater number than the parameter Move instance's.*/
	public boolean greaterThan(Move m) throws NullPointerException {
		return (value > m.value);
	}
	/**Returns whether the owner Move instance's value field holds a smaller number than the parameter Move instance's.*/
	public boolean smallerThan(Move m) throws NullPointerException {
		return (value < m.value);
	}
	/**Returns a move as a String in pseudo-algebraic chess notation for better human-readability.
	 *
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
	/**Returns a move as a 16 bit integer with information on the state of the object stored in designated bits,
	 * except for the score. Useful in memory sensitive applications like the transposition table.
	 *
	 * @return
	 */
	public short toShort() {
		return (short)(from | (to << SHIFT_TO) | (type << SHIFT_TYPE));
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
