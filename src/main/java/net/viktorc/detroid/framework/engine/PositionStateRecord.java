package net.viktorc.detroid.framework.engine;

/**
 * A class for storing position state information.
 * 
 * @author Viktor
 *
 */
public class PositionStateRecord {

	private final byte whiteCastlingRights;
	private final byte blackCastlingRights;
	private final byte enPassantRights;
	private final byte fiftyMoveRuleClock;
	private final long checkers;

	/**
	 * @param whiteCastlingRights The castling rights of white.
	 * @param blackCastlingRights The castling rights of black.
	 * @param enPassantRights The en passant rights of the position.
	 * @param fiftyMoveRuleClock The fifty-move counter state.
	 * @param checkers A bitboard for all pieces checking the side to move's king.
	 */
	public PositionStateRecord(byte whiteCastlingRights, byte blackCastlingRights, byte enPassantRights,
			byte fiftyMoveRuleClock, long checkers) {
		this.whiteCastlingRights = whiteCastlingRights;
		this.blackCastlingRights = blackCastlingRights;
		this.enPassantRights = enPassantRights;
		this.fiftyMoveRuleClock = fiftyMoveRuleClock;
		this.checkers = checkers;
	}
	/**
	 * @return The castling rights of white.
	 */
	public byte getWhiteCastlingRights() {
		return whiteCastlingRights;
	}
	/**
	 * @return The castling rights of black.
	 */
	public byte getBlackCastlingRights() {
		return blackCastlingRights;
	}
	/**
	 * @return The en passant rights of the position.
	 */
	public byte getEnPassantRights() {
		return enPassantRights;
	}
	/**
	 * @return The fifty-move counter state.
	 */
	public byte getFiftyMoveRuleClock() {
		return fiftyMoveRuleClock;
	}
	/**
	 * @return A bitboard for all pieces checking the side to move's king.
	 */
	public long getCheckers() {
		return checkers;
	}
	
}
