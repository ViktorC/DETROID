package engine;

import util.*;
import engine.Bitboard.Square;

/**Some position and move information--such as castling and en passant rights, fifty-move rule clock, repetitions, a bitmap representing checkers, and the
 * moved and captured pieces--is stored in this unencapsulated class' instances so as to make reverting back to the previous position when unmaking a move faster.
 * 
 * @author Viktor
 *
 */
public class UnmakeRegister {

	int movedPiece;
	int capturedPiece;
	int whiteCastlingRights;
	int blackCastlingRights;
	int enPassantRights;
	int fiftyMoveRuleClock;
	int repetitions;
	long checkers;
	
	public UnmakeRegister(int movedPiece, int capturedPiece, int whiteCastlingRights, int blackCastlingRights, int enPassantRights, int fiftyMoveRuleClock, int repetitions, long checkers) {
		this.movedPiece = movedPiece;
		this.capturedPiece = capturedPiece;
		this.whiteCastlingRights = whiteCastlingRights;
		this.blackCastlingRights = blackCastlingRights;
		this.enPassantRights = enPassantRights;
		this.fiftyMoveRuleClock = fiftyMoveRuleClock;
		this.repetitions = repetitions;
		this.checkers = checkers;
	}
	/**Returns a human-readable String representation of the position information stored in the long.
	 * 
	 * @param positionInfo
	 * @return
	 */
	public String toString() {
		long checker;
		String rep = "";
		rep += "Moved piece: " + movedPiece + "\n";
		rep += "Captured piece: " + capturedPiece + "\n";
		if ((checker = BitOperations.getLSBit(checkers)) != 0) {
			rep += String.format("%-23s " + Square.toString(BitOperations.indexOfBit(checker)), "Checker(s):");
			if ((checker = BitOperations.getLSBit(checkers^checker)) != 0)
				rep += ", " + Square.toString(BitOperations.indexOfBit(checker));
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
	public static void printPositionInfoToConsole(List<UnmakeRegister> positionInfoHistory) {
		System.out.println();
		while (positionInfoHistory.hasNext())
			System.out.println(positionInfoHistory.next());
		System.out.println();
	}
	/**Prints all position information longs contained in the input parameter to the console.
	 * 
	 * @param moves
	 */
	public static void printPositionInfoToConsole(UnmakeRegister[] positionInfoHistory) {
		System.out.println();
		for (int i = 0; i < positionInfoHistory.length; i++)
			System.out.println(positionInfoHistory[i]);
		System.out.println();
	}
}
