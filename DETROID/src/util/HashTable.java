package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

/**
 * A generic, concurrent hash table for statistical or turn-based game AI applications with massive storage requirements where losslessness is
 * inefficient. It utilizes a lossy version of cuckoo hashing with constant time look-up and constant time insertion that, instead of pushing out
 * and relocating entries (and rehashing all of them when a cycle is entered) until all hash collisions are resolved, just does the equivalent of
 * one and a half iterations of the standard cuckoo insertion loop in case of a hash conflict. Entries of the hash table implement
 * {@link #HashTable.Entry Entry} and thus implement the {@link #Comparable Comparable} and {@link #Hashable Hashable} interfaces.
 * 
 * The storage scheme is based on asymmetric hashing with two hash tables with different sizes in decreasing order, thus it does not really have
 * two unique hash functions. All it ever does is take the absolute value of the hash keys of the entries and derive mod [respective table's
 * size]; it applies no randomization whatsoever either. Due to the uneven table sizes, look up is biased towards the first table.
 * 
 * The default size of the hash table is 64MB, the minimum is 1MB, and it can hold up to 2^30 entries.
 * 
 * The target application-context, when concurrent, involves moderate contention and more frequent reads than writes.
 * 
 * @author Viktor
 *
 * @param <T> The hash table entry type that implements the {@link #HashTable.Entry Entry} interface.
 */
public class HashTable<T extends HashTable.Entry<T>> implements Iterable<T>, Estimable {
	
	/**
	 * An interface for hash table entries that implicitly extends the {@link #Comparable Comparable} and {@link #Hashable Hashable} interfaces.
	 * 
	 * @author Viktor
	 *
	 * @param <T> The type of the hash table entry that implements this interface.
	 */
	public static interface Entry<T extends HashTable.Entry<T>> extends Comparable<T>, Hashable {
		
	}
	
	/* The lengths of the four inner hash tables are not equal so as to avoid the need for unique hash functions for each; and for faster access
	 * due to the order of the tables tried as the probability of getting a hit in bigger tables is higher. */
	private final static double T1_SHARE = 0.6F;
	private final static double T2_SHARE = 0.4F;
	
	/**
	 * The default maximum hash table size in megabytes.
	 */
	public final static int DEFAULT_SIZE = 1 << 6;
	
	private final static long MAX_CAPACITY = 2L << 30;	// The maximum number of slots.
	private final static long MIN_CAPACITY = 2L << 7;	// The minimum number of slots.
	
	private long entrySize;	// The size of a hash entry, including all overheads, in bytes.
	private long capacity;	// The number of hash table slots.
	
	private AtomicLong load = new AtomicLong(0);	// Load counter.
	
	private T[] t1, t2;	// The two hash tables.
	
	private Lock readLock;	// The reentrant read lock of a read-write lock for concurrency handling.
	private Lock writeLock;	// The reentrant write lock of the read-write lock.
	
	/**
	 * Initializes a hash table with a maximum capacity calculated from the specified maximum allowed memory space and the size of the entry
	 * type's instance. The size has to be at least 512 (2^9) and at most 1073741824 (2^30) times greater than the entry size. E.g. if the entry
	 * size is 32 bytes, the maximum allowed maximum hash table size is 32GB and the minimum maximum size is 1MB (32*512 bytes < 1MB).
	 * 
	 * @param sizeMB Maximum hash table size in megabytes. It can not not be guaranteed to be completely accurately reflected, but will be very
	 * closely approximated. The minimum maximum size is 1MB. If sizeMB is 0 or less, the hash table defaults to 64MB.
	 * @param entrySizeB The size of an instance of the entry class in bytes.
	 */
	@SuppressWarnings({"unchecked"})
	public HashTable(int sizeMB, final int entrySizeB) {
		long tL1, tL2;
		long t1Cap, t2Cap;
		long adjCap;
		MillerRabin prim;
		ReadWriteLock lock;
		entrySize = entrySizeB;
		if (sizeMB <= 0)
			sizeMB = DEFAULT_SIZE;
		capacity = ((long)sizeMB*(1 << 20))/entrySize;
		if (capacity < MIN_CAPACITY)
			throw new IllegalArgumentException("The size has to be great enough for the hash table " +
					"to be able to accommodate at least 512 (2^9) entries of the specified entry size.");
		if (capacity > MAX_CAPACITY)
			throw new IllegalArgumentException("The size has to be small enough for the hash table not " +
					"to be able to accommodate more than 1073741824 (2^30) entries of the specified entry size.");
		lock = new ReentrantReadWriteLock();
		readLock = lock.readLock();
		writeLock = lock.writeLock();
		// Ensuring all tables have prime lengths.
		adjCap = capacity;
		tL1 = tL2 = 0;
		prim = new MillerRabin();
		for (int i = 0; i < 2; i++) {
			t2Cap = (long)(T2_SHARE*adjCap);
			tL2 = prim.greatestLEPrime(t2Cap);
			adjCap += t2Cap - tL2;
			t1Cap = (long)(T1_SHARE*adjCap);
			if ((tL1 = prim.greatestLEPrime(t1Cap)) == tL2)
				tL1 = prim.leastGEPrime(t1Cap);
			adjCap += t1Cap - tL1;
		}
		t1 = (T[])new Entry[(int)tL1];
		t2 = (T[])new Entry[(int)tL2];
		capacity = t1.length + t2.length;
	}
	/**
	 * Initializes a hash table with a default maximum size of 64MB and a maximum capacity calculated from the division of this default maximum
	 * size by the specified size of the entry type's instance.
	 * 
	 * @param entrySizeB The size of an instance of the entry class in bytes.
	 */
	public HashTable(final int entrySizeB) {
		this(0, entrySizeB);
	}
	/**
	 * Returns the number of occupied slots in the hash table.
	 * 
	 * @return
	 */
	public long getLoad() {
		return load.get();
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
	public boolean insert(T e) throws NullPointerException {
		int ind1, ind2, altInd;
		T slot1, slot2, temp;
		long key = e.hashKey();
		boolean slot1IsEmpty, slot2IsEmpty, slot1IsInT1;
		long altAbsKey, absKey = key & Long.MAX_VALUE;
		writeLock.lock();
		try {
			slot1IsEmpty = slot2IsEmpty = true;
			// Checking for an entry with the same key. If there is one, insertion can terminate regardless of its success.
			if ((slot1 = t1[(ind1 = (int)(absKey%t1.length))]) != null) {
				if (key == slot1.hashKey()) {
					if (e.betterThan(slot1)) {
						t1[ind1] = e;
						return true;
					}
					return false;
				}
				slot1IsEmpty = false;
			}
			if ((slot2 = t2[(ind2 = (int)(absKey%t2.length))]) != null) {
				if (key == slot2.hashKey()) {
					if (e.betterThan(slot2)) {
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
				load.incrementAndGet();
				return true;
			}
			if (slot2IsEmpty) {
				t2[ind2] = e;
				load.incrementAndGet();
				return true;
			}
			/* If the method is still executing, there was no empty slot or an entry with an identical key, so we will check if the new entry is
			 * better than any of the entries with different keys. To make sure that the least valuable entry gets pushed out, we first check the
			 * new entry against the "weaker" old entry.
			 */
			if (slot1.betterThan(slot2)) {
				temp = slot1;
				slot1 = slot2;
				slot2 = temp;
				slot1IsInT1 = false;
			}
			else
				slot1IsInT1 = true;
			if (e.betterThan(slot1)) {
				altAbsKey = slot1.hashKey() & Long.MAX_VALUE;
				if (slot1IsInT1) {
					t1[ind1] = e;
					// If the entry that is about to get pushed out's alternative slot is empty insert the entry there.
					if (t2[(altInd = (int)(altAbsKey%t2.length))] == null) {
						t2[altInd] = slot1;
						load.incrementAndGet();
					}
				}
				else {
					t2[ind2] = e;
					if (t1[(altInd = (int)(altAbsKey%t1.length))] == null) {
						t1[altInd] = slot1;
						load.incrementAndGet();
					}
				}
				return true;
			}
			if (e.betterThan(slot2)) {
				altAbsKey = slot2.hashKey() & Long.MAX_VALUE;
				if (slot1IsInT1) {
					t2[ind2] = e;
					if (t1[(altInd = (int)(altAbsKey%t1.length))] == null) {
						t1[altInd] = slot2;
						load.incrementAndGet();
					}
				}
				else {
					t1[ind1] = e;
					if (t2[(altInd = (int)(altAbsKey%t2.length))] == null) {
						t2[altInd] = slot2;
						load.incrementAndGet();
					}
				}
				return true;
			}
			// The new entry is weaker than the old entries with colliding hash keys are.
			return false;
		}
		finally {
			writeLock.unlock();
		}
	}
	/**
	 * Return the entry identified by the input parameter key or null if it is not in the table.
	 * 
	 * @param key
	 * @return The entry mapped to the speciied key.
	 */
	public T lookUp(long key) {
		T e;
		long absKey = key & Long.MAX_VALUE;
		readLock.lock();
		try {
			if ((e = t1[(int)(absKey%t1.length)]) != null && e.hashKey() == key)
				return e;
			if ((e = t2[(int)(absKey%t2.length)]) != null && e.hashKey() == key)
				return e;
			return null;
		}
		finally {
			readLock.unlock();
		}
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
		writeLock.lock();
		try {
			if ((e = t1[(ind = (int)(absKey%t1.length))]) != null && e.hashKey() == key) {
				t1[ind] = null;
				load.decrementAndGet();
				return true;
			}
			if ((e = t2[(ind = (int)(absKey%t2.length))]) != null && e.hashKey() == key) {
				t2[ind] = null;
				load.decrementAndGet();
				return true;
			}
			return false;
		}
		finally {
			writeLock.unlock();
		}
	}
	/**
	 * Removes all the entries that match the condition specified in the argument.
	 * 
	 * @param condition
	 */
	public void remove(Predicate<T> condition) throws NullPointerException {
		T e;
		writeLock.lock();
		try {
			for (int i = 0; i < t1.length; i++) {
				e = t1[i];
				if (e != null && condition.test(e)) {
					t1[i] = null;
					load.decrementAndGet();
				}
			}
			for (int i = 0; i < t2.length; i++) {
				e = t2[i];
				if (e != null && condition.test(e)) {
					t2[i] = null;
					load.decrementAndGet();
				}
			}
		}
		finally {
			writeLock.unlock();
		}
	}
	/**
	 * Replaces the current tables with new, empty hash tables of the same sizes.
	 */
	@SuppressWarnings({"unchecked"})
	public void clear() {
		writeLock.lock();
		try {
			load = new AtomicLong(0);
			t1 = (T[])new Entry[t1.length];
			t2 = (T[])new Entry[t2.length];
		}
		finally {
			writeLock.unlock();
		}
	}
	/**
	 * Returns the base size of the HashTable instance in bytes.
	 * 
	 * @return The size of the underlying hash table structure.
	 */
	@Override
	public long size() {
		readLock.lock();
		try {
			// Total size of entries and empty slots
			return SizeOf.roundedSize(capacity*SizeOf.OBJ_POINTER.numOfBytes + load.get()*(entrySize - SizeOf.OBJ_POINTER.numOfBytes));
		}
		finally {
			readLock.unlock();
		}
	}
	@Override
	public Iterator<T> iterator() {
		ArrayList<T> list;
		readLock.lock();
		try {
			list = new ArrayList<T>();
			list.addAll(Arrays.asList(t1));
			list.addAll(Arrays.asList(t2));
			return list.iterator();
		}
		finally {
			readLock.unlock();
		}
	}
	@Override
	public String toString() {
		return "Load/Capacity: " + load.get() + "/" + capacity + "; " +
				String.format("Factor: %.1f", (load.get()*100)/(double)capacity) + "%\n" +
				String.format("Size: %.2fMB", size()/(double)(1 << 20));
	}
}
