package net.viktorc.detroid.framework.engine;

/**
 * A simple enum for game tree node and hash table entry types based on the relation of their values to alpha and beta.
 *
 * @author Viktor
 */
public enum NodeType {

  EXACT,
  FAIL_HIGH,
  FAIL_LOW;

  public final byte ind;

  NodeType() {
    ind = (byte) ordinal();
  }

}