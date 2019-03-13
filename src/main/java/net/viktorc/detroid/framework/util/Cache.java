package net.viktorc.detroid.framework.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * A generic, pre-allocated hash table for statistical or turn-based game AI applications with massive hash storage requirements where
 * losslessness and dynamic heap allocation are inefficient. It utilizes a lossy version of cuckoo hashing with constant time look-up and
 * constant time insertion that, instead of pushing out and relocating entries (and rehashing all of them when a cycle is entered) until all
 * hash collisions are resolved, only does a specified number of iterations of the standard cuckoo insertion loop in case of a hash
 * conflict. Entries of the hash table must implement the {@link net.viktorc.detroid.framework.util.Cache.Entry} interface.
 *
 * The storage scheme is based on asymmetric hashing with two hash tables with different sizes in decreasing order, thus it does not really
 * have two unique hash functions. All it ever does is take the absolute value of the hash keys of the entries and derive mod [respective
 * table's size]; it applies no randomization whatsoever either. Due to the uneven table sizes, look up is biased towards the first table.
 * This data structure is not thread safe.
 *
 * @param <T> A hash table entry type that implements the {@link net.viktorc.detroid.framework.util.Cache.Entry} interface.
 * @author Viktor
 */
public class Cache<T extends Cache.Entry<T>> implements Iterable<T> {

  /* The lengths of the two arrays are not equal so as to avoid the need for unique hash functions for each; and for
   * faster access due to the order of the tables tried as the probability of getting a hit in bigger tables is
   * higher. */
  private static final float T1_SHARE = 0.6f;
  private static final float T2_SHARE = 0.4f;

  private final int capacity;
  private final int recursions;
  private T[] t1, t2; // The two hash tables.
  private int size;

  /**
   * Constructs a lossy hash table with at least the specified capacity.
   *
   * @param factory The factory to construct the pre-initialized hash table entries.
   * @param capacity The guaranteed minimum capacity the hash table is to have.
   * @param recursions The maximum number of relocation cycles to perform a la cuckoo hashing.
   */
  @SuppressWarnings({"unchecked"})
  public Cache(EntryFactory<T> factory, int capacity, int recursions) {
    long s1, s2, tL1, tL2;
    if (factory == null) {
      throw new NullPointerException("The factory cannot be null.");
    }
    if (capacity <= 0) {
      throw new IllegalArgumentException("Illegal capacity. The capacity has to be greater than 0.");
    }
    if (recursions < 0) {
      throw new IllegalArgumentException("Illegal number of recursions specified. The number of " +
          "recursions have to be greater than 0.");
    }
    s1 = Math.round(T1_SHARE * capacity);
    s2 = Math.round(T2_SHARE * capacity);
    // Ensure all tables have unique prime lengths.
    tL1 = MillerRabin.leastGEPrime(Math.max(2, s1));
    tL2 = MillerRabin.leastGEPrime(Math.max(2, s2));
    if (tL1 == tL2) {
      tL1 = MillerRabin.leastGEPrime(tL1 + 1);
    }
    t1 = (T[]) new Entry[(int) tL1];
    t2 = (T[]) new Entry[(int) tL2];
    for (int i = 0; i < t1.length; i++) {
      t1[i] = factory.newInstance();
    }
    for (int i = 0; i < t2.length; i++) {
      t2[i] = factory.newInstance();
    }
    this.capacity = t1.length + t2.length;
    this.recursions = recursions;
  }

  /**
   * Constructs a lossy hash table with at least the specified capacity.
   *
   * @param factory The factory to construct the pre-initialized hash table entries.
   * @param capacity The guaranteed minimum capacity the hash table is to have.
   */
  public Cache(EntryFactory<T> factory, int capacity) {
    this(factory, capacity, 0);
  }

  /**
   * Returns the total number of slots in the hash table.
   *
   * @return The total number of entry slots.
   */
  public int capacity() {
    return capacity;
  }

  /**
   * Returns the number of non-empty entries in the hash table.
   *
   * @return The total number of non-empty entries.
   */
  public int size() {
    return size;
  }

  /**
   * Returns the size of the hash table in bytes.
   *
   * @return The size of the hash table in bytes.
   */
  public long memorySize() {
    return SizeEstimator.getInstance().sizeOf(this);
  }

  /**
   * Performs recursive cuckoo hashing to the specified depth.
   *
   * @param entry The entry to relocate.
   * @param intoTable1 If it is to be relocated to table 1 or table 2.
   * @param depth The number of recursions left.
   */
  private boolean relocate(T entry, boolean intoTable1, int depth) {
    T otherEntry;
    T[] table;
    long key, absKey;
    // If no more recursions or empty slots left, terminate.
    if (depth == 0 || size >= capacity) {
      return false;
    }
    key = entry.hashKey();
    absKey = key & Long.MAX_VALUE;
    table = intoTable1 ? t1 : t2;
    otherEntry = table[(int) (absKey % table.length)];
    if (otherEntry.isEmpty()) {
      otherEntry.assume(entry);
      size++;
      return true;
    } else {
      otherEntry.swap(entry);
      return relocate(entry, !intoTable1, depth - 1);
    }
  }

  /**
   * Inserts an entry into the hash table. No null checks are made.
   *
   * @param entry The entry to be inserted.
   * @return Whether the entry has been inserted into one of the tables.
   * @throws NullPointerException If e is null.
   */
  public boolean put(T entry) throws NullPointerException {
    T entry1, entry2;
    boolean recurse;
    long key, absKey;
    if (entry.isEmpty()) {
      return false;
    }
    key = entry.hashKey();
    absKey = key & Long.MAX_VALUE;
    // Checking for an entry with the same key. If there is one, insertion can terminate regardless of its success.
    entry1 = t1[(int) (absKey % t1.length)];
    if (!entry1.isEmpty() && key == entry1.hashKey()) {
      if (entry.compareTo(entry1) >= 0) {
        entry1.assume(entry);
        return true;
      }
      return false;
    }
    entry2 = t2[(int) (absKey % t2.length)];
    if (!entry2.isEmpty() && key == entry2.hashKey()) {
      if (entry.compareTo(entry2) >= 0) {
        entry2.assume(entry);
        return true;
      }
      return false;
    }
    // If there was no entry with the same key, but there was at least one empty slot, insert the entry into it.
    if (entry1.isEmpty()) {
      entry1.assume(entry);
      size++;
      return true;
    }
    if (entry2.isEmpty()) {
      entry2.assume(entry);
      size++;
      return true;
    }
    recurse = recursions > 0;
    /* There is no identical entry and there are no empty slots; make sure not to push out the better entry
     * out of the two. */
    if (entry1.compareTo(entry2) <= 0) {
      if (recurse) {
        entry1.swap(entry);
        relocate(entry, false, recursions);
        return true;
      } else if (entry.compareTo(entry1) >= 0) {
        entry1.assume(entry);
        return true;
      }
    } else {
      if (recurse) {
        entry2.swap(entry);
        relocate(entry, true, recursions);
        return true;
      } else if (entry.compareTo(entry2) >= 0) {
        entry2.assume(entry);
        return true;
      }
    }
    // No relocation enabled and the new entry is worse than the worse of the two old entries.
    return false;
  }

  /**
   * Return the entry identified by the input parameter key or null if it is not in the table.
   *
   * @param key The 64 bit hash key.
   * @return The entry mapped to the specified key.
   */
  public T get(long key) {
    T entry;
    long absKey = key & Long.MAX_VALUE;
    if (!(entry = t1[(int) (absKey % t1.length)]).isEmpty() && entry.hashKey() == key) {
      return entry;
    }
    if (!(entry = t2[(int) (absKey % t2.length)]).isEmpty() && entry.hashKey() == key) {
      return entry;
    }
    return null;
  }

  /**
   * Removes the entry identified by the input parameter long integer 'key' from the hash table and returns true if it is in the hash table;
   * returns false otherwise.
   *
   * @param key The 64 bit hash key.
   * @return Whether there was an entry mapped to the specified key.
   */
  public boolean remove(long key) {
    T entry;
    long absKey = key & Long.MAX_VALUE;
    if (!(entry = t1[(int) (absKey % t1.length)]).isEmpty() && entry.hashKey() == key) {
      entry.empty();
      size--;
      return true;
    }
    if (!(entry = t2[(int) (absKey % t2.length)]).isEmpty() && entry.hashKey() == key) {
      entry.empty();
      size--;
      return true;
    }
    return false;
  }

  /**
   * Removes all the entries that match the condition specified in the argument.
   *
   * @param condition The condition on which an entry should be removed.
   */
  public void remove(Predicate<T> condition) throws NullPointerException {
    for (T entry : t1) {
      if (!entry.isEmpty() && condition.test(entry)) {
        entry.empty();
        size--;
      }
    }
    for (T entry : t2) {
      if (!entry.isEmpty() && condition.test(entry)) {
        entry.empty();
        size--;
      }
    }
  }

  /**
   * Clears the hash table by emptying all entries.
   */
  public void clear() {
    for (T entry : t1) {
      entry.empty();
    }
    for (T entry : t2) {
      entry.empty();
    }
    size = 0;
  }

  @Override
  public Iterator<T> iterator() {
    ArrayList<T> list;
    list = new ArrayList<>();
    for (T entry : t1) {
      if (!entry.isEmpty()) {
        list.add(entry);
      }
    }
    for (T entry : t2) {
      if (!entry.isEmpty()) {
        list.add(entry);
      }
    }
    return list.iterator();
  }

  @Override
  public String toString() {
    long load = size();
    return String.format("Load: %d; Capacity: %d; Load Factor: %.2f; Size: %dkB", load, capacity,
        ((double) load) / capacity, Math.round(((double) memorySize()) / (1L << 10)));
  }

  /**
   * An interface for hash table entries that extends the {@link java.lang.Comparable} interface and defines methods required for the
   * management of entries in the hash table.
   *
   * @param <T> The type of the hash table entry that implements this interface.
   * @author Viktor
   */
  public interface Entry<T extends Cache.Entry<T>> extends Comparable<T> {

    /**
     * Returns a long integer hash code.
     *
     * @return A 64 bit hash key.
     */
    long hashKey();

    /**
     * Determines whether the entry is to be considered void.
     *
     * @return Whether the entry is empty, by whatever definition.
     */
    boolean isEmpty();

    /**
     * It has the object assume the state of the parameter instance.
     *
     * @param entry The entry whose state is to be assumed.
     */
    void assume(T entry);

    /**
     * It swaps the states of the object and the parameter instance.
     *
     * @param entry The instance with which the entry on which the method is invoked is supposed to swap states.
     */
    void swap(T entry);

    /**
     * It renders the entry empty.
     */
    void empty();

  }

  /**
   * A factory interface for creating table entries. It serves the purpose of initializing all the entries of the hash table so as to
   * pre-allocate the required heap memory and take advantage of spatial locality.
   *
   * @param <T> A hash table entry type that implements the {@link net.viktorc.detroid.framework.util.Cache.Entry} interface.
   * @author Viktor
   */
  public interface EntryFactory<T extends Cache.Entry<T>> {

    /**
     * Constructs and returns a new entry instance.
     *
     * @return A new entry object.
     */
    T newInstance();

  }

}
