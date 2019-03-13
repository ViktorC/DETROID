package net.viktorc.detroid.framework.gui.models;

/**
 * A simple, immutable class for time control definition.
 *
 * @author Viktor
 */
public final class TimeControl {

  private final long whiteTime;
  private final long blackTime;
  private final long whiteInc;
  private final long blackInc;

  /**
   * Constructs an instance using the specified parameters.
   *
   * @param whiteTime White's initial time in milliseconds.
   * @param blackTime Black's initial time in milliseconds.
   * @param whiteInc White's time increment per move in milliseconds.
   * @param blackInc Black's time increment per move in milliseconds.
   */
  public TimeControl(long whiteTime, long blackTime, long whiteInc, long blackInc) {
    this.whiteTime = whiteTime;
    this.blackTime = blackTime;
    this.whiteInc = whiteInc;
    this.blackInc = blackInc;
  }

  /**
   * Returns white's initial time in milliseconds.
   *
   * @return White's initial time in milliseconds.
   */
  public long getWhiteTime() {
    return whiteTime;
  }

  /**
   * Returns black's initial time in milliseconds.
   *
   * @return Black's initial time in milliseconds.
   */
  public long getBlackTime() {
    return blackTime;
  }

  /**
   * Returns white's time increment per move in milliseconds.
   *
   * @return White's time increment per move in milliseconds.
   */
  public long getWhiteInc() {
    return whiteInc;
  }

  /**
   * Returns black's time increment per move in milliseconds.
   *
   * @return Black's time increment per move in milliseconds.
   */
  public long getBlackInc() {
    return blackInc;
  }

}
