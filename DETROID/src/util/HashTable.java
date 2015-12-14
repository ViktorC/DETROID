package util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**A generic, concurrent hash table utilizing cuckoo hashing with constant look-up time and amortized constant insertion time. Entries of the hash
 * table are required to be immutable to guarantee thread-safety, and implement {@link #HashTable.Entry Entry} and thus implicitly implement the
 * {@link #Comparable Comparable} and {@link #Hashable Hashable} interfaces.
 * 
 * It uses asymmetric hashing with four hash tables with different sizes in decreasing order, thus it does not really have four unique hash functions.
 * All it ever does is take the absolute value of the hash keys of the entries and derive mod [respective table's size]; it applies no randomization
 * whatsoever either. The average load factor is around 60%, but it can be as high as 92% and it never goes below 33%. Due to the uneven table sizes,
 * look up is biased towards the foremost tables. The odds of a look up terminating after checking the first two tables is 60%. It is reasonable
 * compromise between memory and time efficiency. I have determined epsilon and the optimal minimum load factor rather through brute-force tuning
 * methods than mathematics. The framework itself was also more of the fruit of intuition than research, but it works way better than any others I have
 * tried.
 * 
 * The default length of the hash table (the sum of the four tables' lengths) is 1022.
 * 
 * The target application-context, when concurrent, involves moderate contention and similar frequencies of reads and writes.
 * 
 * @author Viktor
 *
 * @param <T> The hash table entry type that implements the {@link #HashTable.Entry Entry} interface.
 */
public class HashTable<T extends HashTable.Entry<T>> {
	
	/**An interface for hash table entries that extends the {@link #Comparable Comparable} and {@link #Hashable Hashable} interfaces.
	 * 
	 * @author Viktor
	 *
	 * @param <T> The type of the hash table entry that implements this interface.
	 */
	public static interface Entry<T> extends Comparable<T>, Hashable {}
	
	// A long with which when another long is AND-ed, the result will be that other long's absolute value.
	private final static long UNSIGNED_LONG = (1L << 63) - 1;
	
	public final static int DEFAULT_SIZE = 1 << 10;
	
	/* The lengths of the four inner hash tables are not equal so as to avoid the need for unique hash functions for each; and for faster access due to the
	 * order of the tables tried as the probability of getting a hit in bigger tables is higher. */
	private final static float T1_SHARE = 0.325F;
	private final static float T2_SHARE = 0.275F;
	private final static float T3_SHARE = 0.225F;
	private final static float T4_SHARE = 0.175F;
	
	private final static float MINIMUM_LOAD_FACTOR = 1/3F;
	private final static float EPSILON = 1.36F;
	
	private AtomicInteger load = new AtomicInteger(0);	// Load counter.
	
	private T[] t1, t2, t3, t4;	// The four hash tables.
	
	/**Instantiates a HashTable with a default length of 1022.*/
	@SuppressWarnings({"unchecked"})
	public HashTable() {
		t1 = (T[])new Entry[(int)(T1_SHARE*DEFAULT_SIZE)];
		t2 = (T[])new Entry[(int)(T2_SHARE*DEFAULT_SIZE)];
		t3 = (T[])new Entry[(int)(T3_SHARE*DEFAULT_SIZE)];
		t4 = (T[])new Entry[(int)(T4_SHARE*DEFAULT_SIZE)];
	}
	/**Instantiates a HashTable with the specified length.
	 * 
	 * @param size > 0
	 */
	@SuppressWarnings({"unchecked"})
	public HashTable(int size) {
		if (size <= 0)
			size = DEFAULT_SIZE;
		t1 = (T[])new Entry[(int)(T1_SHARE*size)];
		t2 = (T[])new Entry[(int)(T2_SHARE*size)];
		t3 = (T[])new Entry[(int)(T3_SHARE*size)];
		t4 = (T[])new Entry[(int)(T4_SHARE*size)];
	}
	/**Returns the length of the hash table.
	 * 
	 * @return
	 */
	public int size() {
		synchronized (t1) { synchronized (t2) { synchronized (t3) { synchronized (t4) {
			return t1.length + t2.length + t3.length + t4.length;
		}}}}
		
	}
	/**Returns the number of occupied slots in the hash table.
	 * 
	 * @return
	 */
	public int load() {
		return load.get();
	}
	/**Inserts an entry into the hash table.
	 * 
	 * @param e
	 */
	public void insert(T e) {
		int ind;
		T slot;
		long key = e.hashKey();
		synchronized (t1) {
			if ((slot = t1[(ind = hash1(key))]) != null && key == slot.hashKey()) {
				if (e.betterThan(slot))
					t1[ind] = e;
				return;
			}
		}
		synchronized (t2) {
			if ((slot = t2[(ind = hash2(key))]) != null && key == slot.hashKey()) {
				if (e.betterThan(slot))
					t2[ind] = e;
				return;
			}
		}
		synchronized (t3) {
			if ((slot = t3[(ind = hash3(key))]) != null && key == slot.hashKey()) {
				if (e.betterThan(slot))
					t3[ind] = e;
				return;
			}
		}
		synchronized (t4) {
			if ((slot = t4[(ind = hash4(key))]) != null && key == slot.hashKey()) {
				if (e.betterThan(slot))
					t4[ind] = e;
				return;
			}
		}
		for (int i = 0; i <= MINIMUM_LOAD_FACTOR*(Math.log(size())/Math.log(EPSILON)); i++) {
			synchronized (t1) {
				if ((slot = t1[(ind = hash1(key))]) == null) {
					t1[ind] = e;
					load.incrementAndGet();
					return;
				}
				t1[ind] = e;
				e = slot;
			}
			synchronized (t2) {
				if ((slot = t2[(ind = hash2(e.hashKey()))]) == null) {
					t2[ind] = e;
					load.incrementAndGet();
					return;
				}
				t2[ind] = e;
				e = slot;
			}
			synchronized (t3) {
				if ((slot = t3[(ind = hash3(e.hashKey()))]) == null) {
					t3[ind] = e;
					load.incrementAndGet();
					return;
				}
				t3[ind] = e;
				e = slot;
			}
			synchronized (t4) {
				if ((slot = t4[(ind = hash4(e.hashKey()))]) == null) {
					t4[ind] = e;
					load.incrementAndGet();
					return;
				}
				t4[ind] = e;
				e = slot;
			}
		}
		rehash();
		insert(e);
	}
	/**Return the entry identified by the input parameter key or null if it is not in the table.
	 * 
	 * @param key
	 * @return
	 */
	public T lookUp(long key) {
		T e;
		synchronized (t1) {
			if ((e = t1[hash1(key)]) != null && e.hashKey() == key)
				return e;
		}
		synchronized (t2) {
			if ((e = t2[hash2(key)]) != null && e.hashKey() == key)
				return e;
		}
		synchronized (t3) {
			if ((e = t3[hash3(key)]) != null && e.hashKey() == key)
				return e;
		}
		synchronized (t4) {
			if ((e = t4[hash4(key)]) != null && e.hashKey() == key)
				return e;
		}
		return null;
	}
	/**Removes the entry identified by the input parameter long integer 'key' from the hash table and returns true if it is in the hash table; returns
	 * false otherwise.
	 * 
	 * @param key
	 * @return
	 */
	public boolean remove(long key) {
		int ind;
		T e;
		synchronized (t1) {
			if ((e = t1[(ind = hash1(key))]) != null && e.hashKey() == key) {
				t1[ind] = null;
				load.decrementAndGet();
				return true;
			}
		}
		synchronized (t2) {
			if ((e = t2[(ind = hash2(key))]) != null && e.hashKey() == key) {
				t2[ind] = null;
				load.decrementAndGet();
				return true;
			}
		}
		synchronized (t3) {
			if ((e = t3[(ind = hash3(key))]) != null && e.hashKey() == key) {
				t3[ind] = null;
				load.decrementAndGet();
				return true;
			}
		}
		synchronized (t4) {
			if ((e = t4[(ind = hash4(key))]) != null && e.hashKey() == key) {
				t4[ind] = null;
				load.decrementAndGet();
				return true;
			}
		}
		return false;
	}
	/**Removes all the entries that match the condition specified in the argument.
	 * 
	 * @param condition
	 */
	public void remove(Predicate<T> condition) {
		T e;
		synchronized (t1) {
			for (int i = 0; i < t1.length; i++) {
				e = t1[i];
				if (e != null && condition.test(e)) {
					t1[i] = null;
					load.decrementAndGet();
				}
			}
		}
		synchronized (t2) {
			for (int i = 0; i < t2.length; i++) {
				e = t2[i];
				if (e != null && condition.test(e)) {
					t2[i] = null;
					load.decrementAndGet();
				}
			}
		}
		synchronized (t3) {
			for (int i = 0; i < t3.length; i++) {
				e = t3[i];
				if (e != null && condition.test(e)) {
					t3[i] = null;
					load.decrementAndGet();
				}
			}
		}
		synchronized (t4) {
			for (int i = 0; i < t4.length; i++) {
				e = t4[i];
				if (e != null && condition.test(e)) {
					t4[i] = null;
					load.decrementAndGet();
				}
			}
		}
	}
	@SuppressWarnings({"unchecked"})
	private void rehash() {
		synchronized (t1) { synchronized (t2) { synchronized (t3) { synchronized (t4) {
			T[] oldTable1 = t1;
			T[] oldTable2 = t2;
			T[] oldTable3 = t3;
			T[] oldTable4 = t4;
			float size = load.get()/MINIMUM_LOAD_FACTOR;
			load = new AtomicInteger(0);
			t1 = (T[])new Entry[(int)(T1_SHARE*size)];
			t2 = (T[])new Entry[(int)(T2_SHARE*size)];
			t3 = (T[])new Entry[(int)(T3_SHARE*size)];
			t4 = (T[])new Entry[(int)(T4_SHARE*size)];
			for (T e : oldTable1) {
				if (e != null)
					insert(e);
			}
			for (T e : oldTable2) {
				if (e != null)
					insert(e);
			}
			for (T e : oldTable3) {
				if (e != null)
					insert(e);
			}
			for (T e : oldTable4) {
				if (e != null)
					insert(e);
			}
		}}}}
	}
	/**Replaces the current tables with new, empty hash tables of the same sizes.*/
	@SuppressWarnings({"unchecked"})
	public void clear() {
		synchronized (t1) { synchronized (t2) { synchronized (t3) { synchronized (t4) {
			load = new AtomicInteger(0);
			t1 = (T[])new Entry[t1.length];
			t2 = (T[])new Entry[t2.length];
			t3 = (T[])new Entry[t3.length];
			t4 = (T[])new Entry[t4.length];
		}}}}
	}
	/**Prints all non-null entries to the console.*/
	public void printAll() {
		synchronized (t1) { synchronized (t2) { synchronized (t3) { synchronized (t4) {
			System.out.println("TABLE_1:\n");
			for (T e : t1) {
				if (e != null)
					System.out.println(e);
			}
			System.out.println("TABLE_2:\n");
			for (T e : t2) {
				if (e != null)
					System.out.println(e);
			}
			System.out.println("TABLE_3:\n");
			for (T e : t3) {
				if (e != null)
					System.out.println(e);
			}
			System.out.println("TABLE_4:\n");
			for (T e : t4) {
				if (e != null)
					System.out.println(e);
			}
		}}}}
	}
	private int hash1(long key) {
		return (int)((key & UNSIGNED_LONG)%t1.length);
	}
	private int hash2(long key) {
		return (int)((key & UNSIGNED_LONG)%t2.length);
	}
	private int hash3(long key) {
		return (int)((key & UNSIGNED_LONG)%t3.length);
	}
	private int hash4(long key) {
		return (int)((key & UNSIGNED_LONG)%t4.length);
	}
}