package net.viktorc.detroid.framework.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A generic hash table for statistical or turn-based game AI applications with massive storage requirements where losslessness is
 * inefficient. It utilizes a lossy version of cuckoo hashing with constant time look-up and constant time insertion that, instead of pushing out
 * and relocating entries (and rehashing all of them when a cycle is entered) until all hash collisions are resolved, just does the equivalent of
 * one and a half iterations of the standard cuckoo insertion loop in case of a hash conflict. Entries of the hash table implement
 * {@link net.viktorc.detroid.framework.util.LossyHashTable.Entry} and thus implement the {@link java.lang.Comparable} and 
 * {@link net.viktorc.detroid.framework.util.Hashable} interfaces.
 * 
 * The storage scheme is based on asymmetric hashing with two hash tables with different sizes in decreasing order, thus it does not really have
 * two unique hash functions. All it ever does is take the absolute value of the hash keys of the entries and derive mod [respective table's
 * size]; it applies no randomization whatsoever either. Due to the uneven table sizes, look up is biased towards the first table.
 * 
 * @author Viktor
 *
 * @param <T> The hash table entry type that implements the {@link net.viktorc.detroid.framework.util.LossyHashTable.Entry} interface.
 */
public class LossyHashTable<T extends LossyHashTable.Entry<T>> implements Collection<T> {
	
	/* The lengths of the four inner hash tables are not equal so as to avoid the need for unique hash functions for each; and for faster access
	 * due to the order of the tables tried as the probability of getting a hit in bigger tables is higher. */
	private static final float T1_SHARE = 0.6f;
	private static final float T2_SHARE = 0.4f;
	private static final int MAX_CAPACITY = 1 << 30; // The maximum number of slots.
	private static final int MIN_CAPACITY = 1 << 10; // The minimum number of slots.
	
	private final long capacity; // The number of hash table slots.
	private T[] t1, t2; // The two hash tables.
	
	/**
	 * Initializes a hash table with a capacity of the closest lesser than or equal prime number to the specified maximum capacity. The capacity has
	 * to be at least 1024 (2^10) and at most 1073741824 (2^30).
	 * 
	 * @param capacity Maximum hash table capacity.
	 */
	@SuppressWarnings({"unchecked"})
	public LossyHashTable(int capacity) {
		long tL1, tL2;
		if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY)
			throw new IllegalArgumentException("Illegal capacity. The capacity has to be between " +
					MIN_CAPACITY + " and " + MAX_CAPACITY + ".");
		// Ensuring all tables have prime lengths.
		tL2 = MillerRabin.greatestLEPrime((long) (T2_SHARE*capacity));
		if ((tL1 = MillerRabin.greatestLEPrime((long) (T1_SHARE*capacity))) == tL2)
			tL2 = tL2 >= 3 ? MillerRabin.greatestLEPrime(tL2 - 1) : MillerRabin.leastGEPrime(tL1 + 1);
		t1 = (T[]) new Entry[(int) tL1];
		t2 = (T[]) new Entry[(int) tL2];
		this.capacity = ((long) t1.length) + t2.length;
	}
	/**
	 * Returns the number of occupied slots in the hash table.
	 * 
	 * @return The number of entries in the table.
	 */
	public long load() {
		long load = 0;
		for (Entry<?> e : t1) {
			if (e != null)
				load++;
		}
		for (Entry<?> e : t2) {
			if (e != null)
				load++;
		}
		return load;
	}
	/**
	 * Returns the total number of slots in the hash tables.
	 * 
	 * @return The total number of entry slots.
	 */
	public long capacity() {
		return capacity;
	}
	/**
	 * Inserts an entry into the hash table. No null checks are made.
	 * 
	 * @param e The entry to be inserted.
	 * @return Whether the entry has been inserted into one of the tables.
	 * @throws NullPointerException If e is null.
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
			return true;
		}
		if (slot2IsEmpty) {
			t2[ind2] = e;
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
				if (t2[(altInd = (int) (altAbsKey%t2.length))] == null)
					t2[altInd] = slot1;
			} else {
				t2[ind2] = e;
				if (t1[(altInd = (int) (altAbsKey%t1.length))] == null)
					t1[altInd] = slot1;
			}
			return true;
		}
		if (e.compareTo(slot2) > 0) {
			altAbsKey = slot2.hashKey() & Long.MAX_VALUE;
			if (slot1IsInT1) {
				t2[ind2] = e;
				if (t1[(altInd = (int) (altAbsKey%t1.length))] == null)
					t1[altInd] = slot2;
			} else {
				t1[ind1] = e;
				if (t2[(altInd = (int) (altAbsKey%t2.length))] == null)
					t2[altInd] = slot2;
			}
			return true;
		}
		// The new entry is weaker than the old entries with colliding hash keys are.
		return false;
	}
	/**
	 * Return the entry identified by the input parameter key or null if it is not in the table.
	 * 
	 * @param key The 64 bit hash key.
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
	 * @param key The 64 bit hash key.
	 * @return Whether there was an entry mapped to the specified key.
	 */
	public boolean remove(long key) {
		int ind;
		T e;
		long absKey = key & Long.MAX_VALUE;
		if ((e = t1[(ind = (int) (absKey%t1.length))]) != null && e.hashKey() == key) {
			t1[ind] = null;
			return true;
		}
		if ((e = t2[(ind = (int) (absKey%t2.length))]) != null && e.hashKey() == key) {
			t2[ind] = null;
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
		T e;
		for (int i = 0; i < t1.length; i++) {
			e = t1[i];
			if (e != null && condition.test(e))
				t1[i] = null;
		}
		for (int i = 0; i < t2.length; i++) {
			e = t2[i];
			if (e != null && condition.test(e))
				t2[i] = null;
		}
	}
	/**
	 * Replaces the current tables with new, empty hash tables of the same sizes.
	 */
	@SuppressWarnings({"unchecked"})
	public void clear() {
		t1 = (T[]) new Entry[t1.length];
		t2 = (T[]) new Entry[t2.length];
	}
	/**
	 * Fills the specified array with the contents of the tables.
	 * 
	 * @param arr The array to fill.
	 */
	private void toObjectArray(Object[] arr) {
		int i;
		for (i = 0; i < t1.length; i++)
			arr[i] = t1[i];
		for (int j = i + 1; j < t2.length && j < arr.length; j++)
			arr[j] = t1[j];
	}
	@Override
	public Object[] toArray() {
		Object[] arr = new Object[size()];
		toObjectArray(arr);
		return arr;
	}
	@Override
	public <U> U[] toArray(U[] a) {
		toObjectArray(a);
		return a;
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
	public int size() {
		return (int) Math.min(Integer.MAX_VALUE, load());
	}
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	@Override
	public boolean contains(Object o) {
		return o instanceof Entry &&  o.equals(get(((Entry<?>) o).hashKey()));
	}
	@Override
	public boolean add(T e) {
		return put(e);
	}
	@Override
	public boolean remove(Object o) {
		if (o instanceof Entry) {
			int ind;
			Entry<?> e = (Entry<?>) o;
			long key = e.hashKey();
			long absKey = key & Long.MAX_VALUE;
			if (t1[(ind = (int) (absKey%t1.length))].equals(o)) {
				t1[ind] = null;
				return true;
			}
			if (t2[(ind = (int) (absKey%t2.length))].equals(o)) {
				t2[ind] = null;
				return true;
			}
			return false;
		}
		return false;
	}
	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o))
				return false;
		}
		return true;
	}
	@Override
	public boolean addAll(Collection<? extends T> c) {
		return c.stream().map(this::put).anyMatch(b -> b);
	}
	@Override
	public boolean removeAll(Collection<?> c) {
		return c.stream().map(this::remove).anyMatch(b -> b);
	}
	@Override
	public boolean retainAll(Collection<?> c) {
		boolean changed = false;
		Set<?> set = new HashSet<>(c);
		for (int i = 0; i < t1.length; i++) {
			T e = t1[i];
			if (e != null && !set.contains(e)) {
				t1[i] = null;
				changed = true;
			}
		}
		for (int i = 0; i < t2.length; i++) {
			T e = t2[i];
			if (e != null && !set.contains(e)) {
				t2[i] = null;
				changed = true;
			}
		}
		return changed;
	}
	@Override
	public String toString() {
		long load = load();
		return String.format("Load/Capacity: %s; Factor: %.1f%\nSize: %.2fMB", load, capacity,
				(load*100)/(double) capacity, SizeEstimator.getInstance().sizeOf(this)/(double)
				(1 << 20));
	}
	
	/**
	 * An interface for hash table entries that implicitly extends the {@link java.lang.Comparable} and 
	 * {@link net.viktorc.detroid.framework.util.Hashable} interfaces.
	 * 
	 * @author Viktor
	 *
	 * @param <T> The type of the hash table entry that implements this interface.
	 */
	public static interface Entry<T extends LossyHashTable.Entry<T>> extends Comparable<T>, Hashable {
		
	}
	
}
