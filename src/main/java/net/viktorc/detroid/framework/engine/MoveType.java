package net.viktorc.detroid.framework.engine;

/**
 * An enum type defining the seven move types the engine handles differently.
 *
 * @author Viktor
 */
public enum MoveType {

  NORMAL,
  SHORT_CASTLING,
  LONG_CASTLING,
  EN_PASSANT,
  PROMOTION_TO_QUEEN,
  PROMOTION_TO_ROOK,
  PROMOTION_TO_BISHOP,
  PROMOTION_TO_KNIGHT;

  public final byte ind;

  MoveType() {
    ind = (byte) ordinal();
  }

}