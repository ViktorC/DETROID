package net.viktorc.detroid.framework.util;

/**
 * A utility class for gray coding integers and for converting gray coded numbers into their natural values.
 *
 * @author Viktor
 */
public class GrayCode {

  private GrayCode() {
  }

  /**
   * Converts a number to its gray coded value.
   *
   * @param n The natural value of the number.
   * @return The gray coded value of the number.
   * @throws IllegalArgumentException If n is less than 0.
   */
  public static long encode(long n) throws IllegalArgumentException {
    if (n < 0) {
      throw new IllegalArgumentException("n has to be unsigned (positive).");
    }
    return n ^ (n >>> 1);
  }

  /**
   * Converts a gray coded number to its natural value.
   *
   * @param g The gray coded value of the number.
   * @return The natural value of the number.
   */
  public static long decode(long g) {
    g ^= (g >> 32);
    g ^= (g >> 16);
    g ^= (g >> 8);
    g ^= (g >> 4);
    g ^= (g >> 2);
    g ^= (g >> 1);
    return g;
  }

}