package engine;

import engine.Bitboard.Square;
import util.*;

/**
 * Some position and move information--such as castling and en passant rights, fifty-move rule clock, repetitions, a bitmap representing checkers,
 * and the moved and captured pieces--is stored in this unencapsulated class' instances so as to make reverting back to the previous position when
 * unmaking a move faster.
 * 
 * @author Viktor
 *
 */
class UnmakeMoveRegister {

	public final byte whiteCastlingRights;
	public final byte blackCastlingRights;
	public final byte enPassantRights;
	public final short fiftyMoveRuleClock;
	public final byte repetitions;
	public final long checkers;
	
	public UnmakeMoveRegister(byte whiteCastlingRights, byte blackCastlingRights, byte enPassantRights,
	short fiftyMoveRuleClock, byte repetitions, long checkers) {
		this.whiteCastlingRights = whiteCastlingRights;
		this.blackCastlingRights = blackCastlingRights;
		this.enPassantRights = enPassantRights;
		this.fiftyMoveRuleClock = fiftyMoveRuleClock;
		this.repetitions = repetitions;
		this.checkers = checkers;
	}
	/**
	 * Returns a human-readable String representation of the position information stored in the long.
	 * 
	 * @param positionInfo
	 * @return
	 */
	@Override
	public String toString() {
		long checker;
		String rep = "";
		if ((checker = BitOperations.getLSBit(checkers)) != 0) {
			rep += String.format("%-23s " + Square.getByIndex(BitOperations.indexOfBit(checker)).toString(), "Checker(s):");
			if ((checker = BitOperations.getLSBit(checkers^checker)) != 0)
				rep += ", " + Square.getByIndex(BitOperations.indexOfBit(checker)).toString();
		}
		rep += String.format("%-23s ", "Castling rights:");
		rep += CastlingRights.toFEN(CastlingRights.getByIndex(whiteCastlingRights), CastlingRights.getByIndex(blackCastlingRights));
		rep += "\n";
		rep += String.format("%-23s ", "En passant rights:");
		rep += EnPassantRights.getByIndex(enPassantRights).toString() + "\n";
		rep += String.format("%-23s " + fiftyMoveRuleClock + "\n", "Fifty-move rule clock:");
		rep += String.format("%-23s " + repetitions + "\n", "Repetitions:");
		return rep;
	}
}
