package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.engine.Bitboard.Square;

/**
 * A simple enum type for the representation of a side's en passant rights in a position.
 *
 * @author Viktor
 */
public enum EnPassantRights {

  A,
  B,
  C,
  D,
  E,
  F,
  G,
  H,
  NONE;

  /**
   * The difference between the EP right index and the square index of the destination of EP for white.
   */
  public final static byte TO_W_DEST_SQR_IND = (byte) Square.A6.ordinal();
  /**
   * The difference between the EP right index and the square index of the possible victim of EP for white.
   */
  public final static byte TO_W_VICT_SQR_IND = (byte) Square.A5.ordinal();
  /**
   * The difference between the EP right index and the square index of the destination of EP for black.
   */
  public final static byte TO_B_DEST_SQR_IND = (byte) Square.A3.ordinal();
  /**
   * The difference between the EP right index and the square index of the possible victim of EP for black.
   */
  public final static byte TO_B_VICT_SQR_IND = (byte) Square.A4.ordinal();

}