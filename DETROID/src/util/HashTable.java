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
 * and relocating entries (and rehashing all of them when a cycle is entered) until all hash collisions are resolved, just checks four possible
 * alternative locations for the pushed out entry in case of a hash conflict. Entries of the hash table implement {@link #HashTable.Entry Entry}
 * and thus implement the {@link #Comparable Comparable} and {@link #Hashable Hashable} interfaces.
 * 
 * The storage scheme is based on asymmetric hashing with four hash tables with different sizes in decreasing order, thus it does not really have
 * four unique hash functions. All it ever does is take the absolute value of the hash keys of the entries and derive mod [respective table's
 * size]; it applies no randomization whatsoever either. Due to the uneven table sizes, look up is biased towards the foremost tables. The odds
 * of a look up terminating after checking the first two tables is 60%.
 * 
 * The default size of the hash table is 64MB; the minimum is 1MB and the maximum is 6GB.
 * 
 * The target application-context, when concurrent, involves moderate contention and similar frequencies of reads and writes.
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
	
	// A long with which when another long is AND-ed, the result will be that other long's absolute value.
	private final static long UNSIGNED_LONG = (1L << 63) - 1;
	
	/* The lengths of the four inner hash tables are not equal so as to avoid the need for unique hash functions for each; and for faster access
	 * due to the order of the tables tried as the probability of getting a hit in bigger tables is higher. */
	private final static float T1_SHARE = 0.325F;
	private final static float T2_SHARE = 0.275F;
	private final static float T3_SHARE = 0.225F;
	private final static float T4_SHARE = 0.175F;
	
	/**
	 * The default maximum hash table size in megabytes.
	 */
	public final static int DEFAULT_SIZE = 1 << 6;
	private final static int MAX_SIZE = 3*(1 << 11); // The absolute maximum hash table size in megabytes.
	
	private long entrySize;	// The size of a hash entry, including all overheads, in bytes.
	private long capacity;	// The number of allowed hash table slots.
	
	private AtomicLong load = new AtomicLong(0);	// Load counter.
	
	private T[] t1, t2, t3, t4;	// The four hash tables.
	
	private Lock readLock;	// The reentrant read lock of a read-write lock for concurrency handling.
	private Lock writeLock;	// The reentrant write lock of the read-write lock.
	
	/**
	 * Initializes a hash table with a maximum capacity calculated from the specified maximum allowed memory space and the size of the entry
	 * type's instance.
	 * 
	 * @param sizeMB Maximum hash table size in megabytes.
	 * @param entrySizeB The size of an instance of the entry class in bytes.
	 */
	@SuppressWarnings({"unchecked"})
	public HashTable(int sizeMB, final int entrySizeB) {
		ReadWriteLock lock = new ReentrantReadWriteLock();
		readLock = lock.readLock();
		writeLock = lock.writeLock();
		entrySize = entrySizeB;
		if (sizeMB <= 0)
			sizeMB = DEFAULT_SIZE;
		else if (sizeMB > MAX_SIZE)
			sizeMB = MAX_SIZE;
		capacity = (sizeMB*(1 << 20))/entrySize;
		t1 = (T[])new Entry[(int)Math.round(T1_SHARE*capacity)];
		t2 = (T[])new Entry[(int)Math.round(T2_SHARE*capacity)];
		t3 = (T[])new Entry[(int)Math.round(T3_SHARE*capacity)];
		t4 = (T[])new Entry[(int)Math.round(T4_SHARE*capacity)];
		capacity = t1.length + t2.length + t3.length + t4.length;
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
		int ind1, ind2, ind3, ind4, altInd;
		T slot1, slot2, slot3, slot4;
		long key = e.hashKey();
		long altAbsKey, absKey = key & UNSIGNED_LONG;
		writeLock.lock();
		try {
			if ((slot1 = t1[(ind1 = (int)(absKey%t1.length))]) != null) {
				if (key == slot1.hashKey()) {
					if (e.betterThan(slot1)) {
						t1[ind1] = e;
						return true;
					}
					return false;
				}
			}
			else {
				t1[ind1] = e;
				load.incrementAndGet();
				return true;
			}
			if ((slot2 = t2[(ind2 = (int)(absKey%t2.length))]) != null) {
				if (key == slot2.hashKey()) {
					if (e.betterThan(slot2)) {
						t2[ind2] = e;
						return true;
					}
					return false;
				}
			}
			else {
				t2[ind2] = e;
				load.incrementAndGet();
				return true;
			}
			if ((slot3 = t3[(ind3 = (int)(absKey%t3.length))]) != null) {
				if (key == slot3.hashKey()) {
					if (e.betterThan(slot3)) {
						t3[ind3] = e;
						return true;
					}
					return false;
				}
			}
			else {
				t3[ind3] = e;
				load.incrementAndGet();
				return true;
			}
			if ((slot4 = t4[(ind4 = (int)(absKey%t4.length))]) != null) {
				if (key == slot4.hashKey()) {
					if (e.betterThan(slot4)) {
						t4[ind4] = e;
						return true;
					}
					return false;
				}
			}
			else {
				t4[ind4] = e;
				load.incrementAndGet();
				return true;
			}
			if (e.betterThan(slot1)) {
				t1[ind1] = e;
				altAbsKey = slot1.hashKey() & UNSIGNED_LONG;
				if (t2[(altInd = (int)(altAbsKey%t2.length))] == null)
					t2[altInd] = slot1;
				else if (t3[(altInd = (int)(altAbsKey%t3.length))] == null)
					t3[altInd] = slot1;
				else if (t4[(altInd = (int)(altAbsKey%t4.length))] == null)
					t4[altInd] = slot1;
				return true;
			}
			if (e.betterThan(slot2)) {
				t2[ind2] = e;
				altAbsKey = slot2.hashKey() & UNSIGNED_LONG;
				if (t1[(altInd = (int)(altAbsKey%t1.length))] == null)
					t1[altInd] = slot2;
				else if (t3[(altInd = (int)(altAbsKey%t3.length))] == null)
					t3[altInd] = slot2;
				else if (t4[(altInd = (int)(altAbsKey%t4.length))] == null)
					t4[altInd] = slot2;
				return true;
			}
			if (e.betterThan(slot3)) {
				t3[ind3] = e;
				altAbsKey = slot3.hashKey() & UNSIGNED_LONG;
				if (t1[(altInd = (int)(altAbsKey%t1.length))] == null)
					t1[altInd] = slot3;
				else if (t2[(altInd = (int)(altAbsKey%t2.length))] == null)
					t2[altInd] = slot3;
				else if (t4[(altInd = (int)(altAbsKey%t4.length))] == null)
					t4[altInd] = slot3;
				return true;
			}
			if (e.betterThan(slot4)) {
				t4[ind4] = e;
				altAbsKey = slot4.hashKey() & UNSIGNED_LONG;
				if (t1[(altInd = (int)(altAbsKey%t1.length))] == null)
					t1[altInd] = slot4;
				else if (t2[(altInd = (int)(altAbsKey%t2.length))] == null)
					t2[altInd] = slot4;
				else if (t3[(altInd = (int)(altAbsKey%t3.length))] == null)
					t3[altInd] = slot4;
				return true;
			}
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
		long absKey = key & UNSIGNED_LONG;
		readLock.lock();
		try {
			if ((e = t1[(int)(absKey%t1.length)]) != null && e.hashKey() == key)
				return e;
			if ((e = t2[(int)(absKey%t2.length)]) != null && e.hashKey() == key)
				return e;
			if ((e = t3[(int)(absKey%t3.length)]) != null && e.hashKey() == key)
					return e;
			if ((e = t4[(int)(absKey%t4.length)]) != null && e.hashKey() == key)
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
		long absKey = key & UNSIGNED_LONG;
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
			if ((e = t3[(ind = (int)(absKey%t3.length))]) != null && e.hashKey() == key) {
				t3[ind] = null;
				load.decrementAndGet();
				return true;
			}
			if ((e = t4[(ind = (int)(absKey%t4.length))]) != null && e.hashKey() == key) {
				t4[ind] = null;
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
			for (int i = 0; i < t3.length; i++) {
				e = t3[i];
				if (e != null && condition.test(e)) {
					t3[i] = null;
					load.decrementAndGet();
				}
			}
			for (int i = 0; i < t4.length; i++) {
				e = t4[i];
				if (e != null && condition.test(e)) {
					t4[i] = null;
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
			t3 = (T[])new Entry[t3.length];
			t4 = (T[])new Entry[t4.length];
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
			return SizeOf.roundedSize(capacity*SizeOf.POINTER.numOfBytes + load.get()*(entrySize - SizeOf.POINTER.numOfBytes));
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
			list.addAll(Arrays.asList(t3));
			list.addAll(Arrays.asList(t4));
			return list.iterator();
		}
		finally {
			readLock.unlock();
		}
	}
	@Override
	public String toString() {
		return "Load/Capacity: " + load.get() + "/" + capacity + "\n" +
				"Size: " + size()/(1 << 20) + "MB";
	}
}
