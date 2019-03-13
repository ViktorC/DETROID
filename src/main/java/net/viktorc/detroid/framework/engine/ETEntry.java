package net.viktorc.detroid.framework.engine;

import net.viktorc.detroid.framework.util.Cache.Entry;

/**
 * An evaluation hash table entry that stores information about the static evaluation scores of leaf nodes. It uses Robert Hyatt's lock-less
 * hashing.
 *
 * @author Viktor
 */
public class ETEntry implements Entry<ETEntry> {

  private volatile long key;
  private volatile short score;
  private volatile byte generation;

  /**
   * @return The 64 bit position hash key.
   */
  public long getKey() {
    return key;
  }

  /**
   * @return The evaluation score.
   */
  public short getScore() {
    return score;
  }

  /**
   * @return The age of the entry.
   */
  public byte getGeneration() {
    return generation;
  }

  /**
   * @param generation The age of the entry.
   */
  public void setGeneration(byte generation) {
    this.generation = generation;
  }

  /**
   * @param key The 64 bit position hash key.
   * @param score The evaluation score.
   * @param generation The age of the entry.
   */
  public void set(long key, short score, byte generation) {
    this.key = key;
    this.score = score;
    this.generation = generation;
  }

  /**
   * XORs the data fields into the key.
   */
  public synchronized void setupKey() {
    key ^= score;
  }

  @Override
  public long hashKey() {
    return key ^ score;
  }

  @Override
  public boolean isEmpty() {
    return key == 0;
  }

  @Override
  public void assume(ETEntry entry) {
    set(entry.key, entry.score, entry.generation);
  }

  @Override
  public void swap(ETEntry entry) {
    long key = this.key;
    short score = this.score;
    byte generation = this.generation;
    assume(entry);
    entry.set(key, score, generation);
  }

  @Override
  public void empty() {
    key = 0;
  }

  @Override
  public int compareTo(ETEntry e) {
    return generation - e.generation;
  }

  @Override
  public String toString() {
    return String.format("KEY: %s; SCORE: %d; GENERATION: %d", key, score, generation);
  }

}