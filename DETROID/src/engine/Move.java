package engine;

import util.List;
import util.Comparable;

/**A simple unencapsulated class that provides objects for storing information about moves necessary for making them.
 * 
 * @author Viktor
 *
 */
public class Move implements Comparable<Move> {
	
	/**Mask and shift values for encoding contents of a Move object into an int; and vica versa.
	 * 
	 * @author Viktor
	 *
	 */
	private enum ToInt {
		
		SHIFT_TO 			(6),
		SHIFT_MOVED_PIECE	(12),
		SHIFT_CAPTURED_PIECE(16),
		SHIFT_TYPE			(20),
		MASK_FROM_TO		(63),
		MASK_MOVED_CAPTURED	(15);
		
		final byte value;
		
		private ToInt(int value) {
			this.value = (byte)value;
		}
	}
	

	int from;			//the index of the origin square
	int to;				//the index of the destination square
	int movedPiece;		//the numeric notation of the type of piece moved
	int capturedPiece;	//the numeric notation of the type of piece captured; if none, 0
	int type;			//the type of the move; 0 - normal, 1 - short castling, 2 - long castling, 3 - en passant, 4 - promotion to queen, 5 - promotion to rook, 6 - promotion to bishop, 7 - promotion to knight
	int value;			//the value assigned to the move at move ordering, or the value returned by the search algorithm based on the evaluator's scoring for the position that the move leads to
	
	public Move() {
	}
	public Move(int score) {
		value = score;
	}
	public Move(int from, int to, int movedPiece, int capturedPiece, int type) {
		this.from = from;
		this.to = to;
		this.movedPiece = movedPiece;
		this.capturedPiece = capturedPiece;
		this.type = type;
	}
	/**Parses a move encoded in a 32 bit integer.*/
	public static Move toMove(int move) {
		Move m = new Move();
		m.from = move & ToInt.MASK_FROM_TO.value;
		m.to = (move >>> ToInt.SHIFT_TO.value) & ToInt.MASK_FROM_TO.value;
		m.movedPiece = (move >>> ToInt.SHIFT_MOVED_PIECE.value) & ToInt.MASK_MOVED_CAPTURED.value;
		m.capturedPiece = (move >>> ToInt.SHIFT_CAPTURED_PIECE.value) & ToInt.MASK_MOVED_CAPTURED.value;
		m.type = move >>> ToInt.SHIFT_TYPE.value;
		return m;
	}
	/**Returns whether the owner Move instance's value field holds a greater number than the parameter Move instance's.*/
	public boolean betterThan(Move m) throws NullPointerException {
		return (value > m.value);
	}
	/**Returns whether the owner Move instance's value field holds a smaller number than the parameter Move instance's.*/
	public boolean worseThan(Move m) throws NullPointerException {
		return (value < m.value);
	}
	/**Returns a move as a String in pseudo-algebraic chess notation for better human-readability.
	 *
	 * @return
	 */
	public String toString() {
		String alg, movedPiece, capture, originFile, originRank, destFile, destRank;
		if (type == 1)
			return "0-0";
		else if (type == 2)
			return "0-0-0";
		originRank	= Integer.toString(from/8 + 1);
		originFile	= Character.toString((char)(from%8 + 'a'));
		destRank	= Integer.toString(to/8 + 1);
		destFile	= Character.toString((char)(to%8 + 'a'));
		movedPiece  = "" + Piece.fenNotation(this.movedPiece);
		if (this.capturedPiece == 0)
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
	/**Returns a move as a 32 bit integer with information on the state of the object stored in designated bits, except for the score.
	 * Useful in memory sensitive applications like the transposition table as it identifies a 30 byte Move object in merely 4 bytes.
	 *
	 * @return
	 */
	public int toInt() {
		return (from | (to << ToInt.SHIFT_TO.value) | (movedPiece << ToInt.SHIFT_MOVED_PIECE.value) |
			   (capturedPiece << ToInt.SHIFT_CAPTURED_PIECE.value) | (type << ToInt.SHIFT_TYPE.value));
	}
	/**Returns whether this move is equal to the input parameter move.
	 * 
	 * @param m
	 * @return
	 */
	public boolean equals(Move m) {
		if (from == m.from && to == m.to && type == m.type)
			return true;
		return false;
	}
	/**Returns whether this move is equal to the input parameter move.
	 * 
	 * @param move
	 * @return
	 */
	public boolean equls(short move) {
		return equals(toMove(move));
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
