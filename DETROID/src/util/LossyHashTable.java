package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * A generic hash table for statistical or turn-based game AI applications with massive storage requirements where losslessness is
 * inefficient. It utilizes a lossy version of cuckoo hashing with constant time look-up and constant time insertion that, instead of pushing out
 * and relocating entries (and rehashing all of them when a cycle is entered) until all hash collisions are resolved, just does the equivalent of
 * one and a half iterations of the standard cuckoo insertion loop in case of a hash conflict. Entries of the hash table implement
 * {@link #HashTable.Entry Entry} and thus implement the {@link #Comparable Comparable} and {@link #Hashable Hashable} interfaces.
 * 
 * The storage scheme is based on asymmetric hashing with two hash tables with different sizes in decreasing order, thus it does not really have
 * two unique hash functions. All it ever does is take the absolute value of the hash keys of the entries and derive mod [respective table's
 * size]; it applies no randomization whatsoever either. Due to the uneven table sizes, look up is biased towards the first table.
 * 
 * @author Viktor
 *
 * @param <T> The hash table entry type that implements the {@link #HashTable.Entry Entry} interface.
 */
public class LossyHashTable<T extends LossyHashTable.Entry<T>> implements Iterable<T> {
	
	/**
	 * An interface for hash table entries that implicitly extends the {@link #Comparable Comparable} and {@link #Hashable Hashable} interfaces.
	 * 
	 * @author Viktor
	 *
	 * @param <T> The type of the hash table entry that implements this interface.
	 */
	public static interface Entry<T extends LossyHashTable.Entry<T>> extends Comparable<T>, Hashable {
		
	}
	
	/* The lengths of the four inner hash tables are not equal so as to avoid the need for unique hash functions for each; and for faster access
	 * due to the order of the tables tried as the probability of getting a hit in bigger tables is higher. */
	private final static float T1_SHARE = 0.6f;
	private final static float T2_SHARE = 0.4f;
	
	private final static int MAX_CAPACITY = (int) (1L << 30);	// The maximum number of slots.
	private final static int MIN_CAPACITY = (int) (1L << 10);	// The minimum number of slots.
	
	private long capacity;	// The number of hash table slots.
	
	private long load = 0;	// Load counter.
	
	private T[] t1, t2;	// The two hash tables.
	
	/**
	 * Initializes a hash table with a capacity of the closest lesser than or equal prime number to the specified maximum capacity. The capacity has
	 * to be at least 1024 (2^10) and at most 1073741824 (2^30).
	 * 
	 * @param capacity Maximum hash table capacity.
	 */
	@SuppressWarnings({"unchecked"})
	public LossyHashTable(int capacity) {
		long tL1, tL2;
		MillerRabin prim;
		if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY)
			throw new IllegalArgumentException("Illegal capacity. The capacity has to be between " + MIN_CAPACITY + " and " + MAX_CAPACITY + ".");
		// Ensuring all tables have prime lengths.
		tL1 = tL2 = 0;
		prim = new MillerRabin();
		tL2 = prim.greatestLEPrime((long) (T2_SHARE*capacity));
		if ((tL1 = prim.greatestLEPrime((long) (T1_SHARE*capacity))) == tL2) {
			if (tL2 >= 3)
				tL2 = prim.greatestLEPrime(tL2 - 1);
			else
				tL1 = prim.leastGEPrime(tL1 + 1);
		}
		t1 = (T[]) new Entry[(int) tL1];
		t2 = (T[]) new Entry[(int) tL2];
		this.capacity = t1.length + t2.length;
	}
	/**
	 * Returns the number of occupied slots in the hash table.
	 * 
	 * @return
	 */
	public long getLoad() {
		return load;
	}
	/**
	 * Returns the total number of slots in the hash tables.
	 * 
	 * @return
	 */
	public long getCapacity() {
		return capacity;
	}
	/**
	 * Inserts an entry into the hash table. No null checks are made.
	 * 
	 * @param e The entry to be inserted.
	 * @return Whether the entry has been inserted into one of the tables.
	 * @throws NullPointerException
	 */
	public boolean put(T e) throws NullPointerException {
		int ind1, ind2, altInd;
		T slot1, slot2, temp;
		long key = e.hashKey();
		boolean slot1IsEmpty, slot2IsEmpty, slot1IsInT1;
		long altAbsKey, absKey = key & Long.MAX_VALUE;
		slot1IsEmpty = slot2IsEmpty = true;
		// Checking for an entry with the same key. If there is one, insertion can terminate regardless of its success.
		if ((slot1 = t1[(ind1 = (int) (absKey%t1.length))]) != null) {
			if (key == slot1.hashKey()) {
				if (e.compareTo(slot1) > 0) {
					t1[ind1] = e;
					return true;
				}
				return false;
			}
			slot1IsEmpty = false;
		}
		if ((slot2 = t2[(ind2 = (int) (absKey%t2.length))]) != null) {
			if (key == slot2.hashKey()) {
				if (e.compareTo(slot2) > 0) {
					t2[ind2] = e;
					return true;
				}
				return false;
			}
			slot2IsEmpty = false;
		}
		// If there was no entry with the same key, but there was at least one empty slot, insert the entry into it.
		if (slot1IsEmpty) {
			t1[ind1] = e;
			load++;
			return true;
		}
		if (slot2IsEmpty) {
			t2[ind2] = e;
			load++;
			return true;
		}
		/* If the method is still executing, there was no empty slot or an entry with an identical key, so we will check if the new entry is
		 * better than any of the entries with different keys. To make sure that the least valuable entry gets pushed out, we first check the
		 * new entry against the "weaker" old entry.
		 */
		if (slot1.compareTo(slot2) > 0) {
			temp = slot1;
			slot1 = slot2;
			slot2 = temp;
			slot1IsInT1 = false;
		} else
			slot1IsInT1 = true;
		if (e.compareTo(slot1) > 0) {
			altAbsKey = slot1.hashKey() & Long.MAX_VALUE;
			if (slot1IsInT1) {
				t1[ind1] = e;
				// If the entry that is about to get pushed out's alternative slot is empty insert the entry there.
				if (t2[(altInd = (int) (altAbsKey%t2.length))] == null) {
					t2[altInd] = slot1;
					load++;
				}
			} else {
				t2[ind2] = e;
				if (t1[(altInd = (int) (altAbsKey%t1.length))] == null) {
					t1[altInd] = slot1;
					load++;
				}
			}
			return true;
		}
		if (e.compareTo(slot2) > 0) {
			altAbsKey = slot2.hashKey() & Long.MAX_VALUE;
			if (slot1IsInT1) {
				t2[ind2] = e;
				if (t1[(altInd = (int) (altAbsKey%t1.length))] == null) {
					t1[altInd] = slot2;
					load++;
				}
			} else {
				t1[ind1] = e;
				if (t2[(altInd = (int) (altAbsKey%t2.length))] == null) {
					t2[altInd] = slot2;
					load++;
				}
			}
			return true;
		}
		// The new entry is weaker than the old entries with colliding hash keys are.
		return false;
	}
	/**
	 * Return the entry identified by the input parameter key or null if it is not in the table.
	 * 
	 * @param key
	 * @return The entry mapped to the specified key.
	 */
	public T get(long key) {
		T e;
		long absKey = key & Long.MAX_VALUE;
		if ((e = t1[(int) (absKey%t1.length)]) != null && e.hashKey() == key)
			return e;
		if ((e = t2[(int) (absKey%t2.length)]) != null && e.hashKey() == key)
			return e;
		return null;
	}
	/**
	 * Removes the entry identified by the input parameter long integer 'key' from the hash table and returns true if it is in the hash table;
	 * returns false otherwise.
	 * 
	 * @param key
	 * @return Whether there was an entry mapped to the specified key.
	 */
	public boolean remove(long key) {
		int ind;
		T e;
		long absKey = key & Long.MAX_VALUE;
		if ((e = t1[(ind = (int) (absKey%t1.length))]) != null && e.hashKey() == key) {
			t1[ind] = null;
			load--;
			return true;
		}
		if ((e = t2[(ind = (int) (absKey%t2.length))]) != null && e.hashKey() == key) {
			t2[ind] = null;
			load--;
			return true;
		}
		return false;
	}
	/**
	 * Removes all the entries that match the condition specified in the argument.
	 * 
	 * @param condition
	 */
	public void remove(Predicate<T> condition) throws NullPointerException {
		T e;
		for (int i = 0; i < t1.length; i++) {
			e = t1[i];
			if (e != null && condition.test(e)) {
				t1[i] = null;
				load--;
			}
		}
		for (int i = 0; i < t2.length; i++) {
			e = t2[i];
			if (e != null && condition.test(e)) {
				t2[i] = null;
				load--;
			}
		}
	}
	/**
	 * Replaces the current tables with new, empty hash tables of the same sizes.
	 */
	@SuppressWarnings({"unchecked"})
	public void clear() {
		load = 0;
		t1 = (T[])new Entry[t1.length];
		t2 = (T[])new Entry[t2.length];
	}
	@Override
	public Iterator<T> iterator() {
		ArrayList<T> list;
		list = new ArrayList<T>();
		list.addAll(Arrays.asList(t1));
		list.addAll(Arrays.asList(t2));
		return list.iterator();
	}
	@Override
	public String toString() {
		return "Load/Capacity: " + load + "/" + capacity + "; " +
				String.format("Factor: %.1f", (load*100)/(double) capacity) + "%\n" +
				String.format("Size: %.2fMB", SizeEstimator.getInstance().sizeOf(this)/(double) (1 << 20));
	}
}
