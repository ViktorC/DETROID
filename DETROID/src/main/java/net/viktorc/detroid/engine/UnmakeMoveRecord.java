package net.viktorc.detroid.engine;

import net.viktorc.detroid.engine.Bitboard.Square;
import net.viktorc.detroid.util.BitOperations;

/**
 * Some position and move information--such as castling and en passant rights, fifty-move rule clock, a bitmap representing checkers,
 * and the moved and captured pieces--is stored in this class' instances so as to make reverting back to the previous position when
 * unmaking a move faster.
 * 
 * @author Viktor
 *
 */
class UnmakeMoveRecord {

	final byte whiteCastlingRights;
	final byte blackCastlingRights;
	final byte enPassantRights;
	final byte fiftyMoveRuleClock;
	final long checkers;
	
	UnmakeMoveRecord(byte whiteCastlingRights, byte blackCastlingRights, byte enPassantRights,
			byte fiftyMoveRuleClock, long checkers) {
		this.whiteCastlingRights = whiteCastlingRights;
		this.blackCastlingRights = blackCastlingRights;
		this.enPassantRights = enPassantRights;
		this.fiftyMoveRuleClock = fiftyMoveRuleClock;
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
		return rep;
	}
	
}
