package net.viktorc.detroid.framework.validation;

/**
 * All the different states a chess game can have.
 *
 * @author Viktor
 */
public enum GameState {

  IN_PROGRESS("*"),
  WHITE_MATES("1-0"),
  BLACK_MATES("0-1"),
  STALE_MATE("1/2-1/2"),
  DRAW_BY_INSUFFICIENT_MATERIAL("1/2-1/2"),
  DRAW_BY_3_FOLD_REPETITION("1/2-1/2"),
  DRAW_BY_50_MOVE_RULE("1/2-1/2"),
  DRAW_BY_AGREEMENT("1/2-1/2"),
  /**
   * E.g. black resigns or loses on time.
   */
  UNSPECIFIED_WHITE_WIN("1-0"),
  /**
   * E.g. white resigns or loses on time.
   */
  UNSPECIFIED_BLACK_WIN("0-1");

  private final String pgnCode;

  GameState(String pgnCode) {
    this.pgnCode = pgnCode;
  }

  /**
   * @return The PGN code of the game state.
   */
  public String getPGNCode() {
    return pgnCode;
  }

}
