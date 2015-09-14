package util;

import java.util.function.Predicate;

/**A generic, so far non-thread-safe hash table utilizing cuckoo hashing with constant look-up time and amortized constant insertion time. Entries of
 * the hash table are required to extend {@link #HashTable.Entry Entry} and implicitly implement {@link #Comparable Comparable}.
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
 * @author Viktor
 *
 * @param <E>
 */
public class HashTable<E extends HashTable.Entry<E>> {
	
	/**An abstract base class for hash table entries the defines the long variable 'key' upon what the entry objects are hashed onto an index of the hash table.
	 * 
	 * @author Viktor
	 *
	 */
	public static abstract class Entry<E> implements Comparable<E>, Hashable {
		
		protected long key;	//a 64 bit integer key that is hashed onto an index
		
		/**Returns a 64 bit hash code that can be used for the identification of the object, however its uniqueness is not guaranteed.*/
		public long key() {
			return key;
		}
	}
	
	private final static long UNSIGNED_LONG = (1L << 63) - 1;
	
	public final static int DEFAULT_SIZE = 1 << 10;
	
	private final static float T1_SHARE = 0.325F;
	private final static float T2_SHARE = 0.275F;
	private final static float T3_SHARE = 0.225F;
	private final static float T4_SHARE = 0.175F;
	
	private final static float MINIMUM_LOAD_FACTOR = 1/3F;
	
	private final static float EPSILON = 1.36F;
	
	private E[] t1;
	private E[] t2;
	private E[] t3;
	private E[] t4;
	
	private int load = 0;
	
	/**Instantiates a HashTable with a default length of 1022.*/
	@SuppressWarnings({"unchecked"})
	public HashTable() {
		t1 = (E[])new Entry[(int)(T1_SHARE*DEFAULT_SIZE)];
		t2 = (E[])new Entry[(int)(T2_SHARE*DEFAULT_SIZE)];
		t3 = (E[])new Entry[(int)(T3_SHARE*DEFAULT_SIZE)];
		t4 = (E[])new Entry[(int)(T4_SHARE*DEFAULT_SIZE)];
	}
	/**Instantiates a HashTable with the specified length.
	 * 
	 * @param size > 0
	 */
	@SuppressWarnings({"unchecked"})
	public HashTable(int size) {
		if (size <= 0)
			size = DEFAULT_SIZE;
		t1 = (E[])new Entry[(int)(T1_SHARE*size)];
		t2 = (E[])new Entry[(int)(T2_SHARE*size)];
		t3 = (E[])new Entry[(int)(T3_SHARE*size)];
		t4 = (E[])new Entry[(int)(T4_SHARE*size)];
	}
	/**Returns the length of the hash table.
	 * 
	 * @return
	 */
	public int size() {
		return t1.length + t2.length + t3.length + t4.length;
	}
	/**Returns the number of occupied slots in the hash table.
	 * 
	 * @return
	 */
	public int load() {
		return load;
	}
	/**Inserts an entry into the hash table.
	 * 
	 * @param e
	 */
	public void insert(E e) {
		int ind;
		E slot;
		if ((slot = t1[(ind = hash1(e.key))]) != null && e.key == slot.key) {
			if (e.betterThan(slot))
				t1[ind] = e;
			return;
		}
		if ((slot = t2[(ind = hash2(e.key))]) != null && e.key == slot.key) {
			if (e.betterThan(slot))
				t2[ind] = e;
			return;
		}
		if ((slot = t3[(ind = hash3(e.key))]) != null && e.key == slot.key) {
			if (e.betterThan(slot))
				t3[ind] = e;
			return;
		}
		if ((slot = t4[(ind = hash4(e.key))]) != null && e.key == slot.key) {
			if (e.betterThan(slot))
				t4[ind] = e;
			return;
		}
		for (int i = 0; i <= MINIMUM_LOAD_FACTOR*(Math.log(size())/Math.log(EPSILON)); i++) {
			if ((slot = t1[(ind = hash1(e.key))]) == null) {
				t1[ind] = e;
				load++;
				return;
			}
			t1[ind] = e;
			e = slot;
			if ((slot = t2[(ind = hash2(e.key))]) == null) {
				t2[ind] = e;
				load++;
				return;
			}
			t2[ind] = e;
			e = slot;
			if ((slot = t3[(ind = hash3(e.key))]) == null) {
				t3[ind] = e;
				load++;
				return;
			}
			t3[ind] = e;
			e = slot;
			if ((slot = t4[(ind = hash4(e.key))]) == null) {
				t4[ind] = e;
				load++;
				return;
			}
			t4[ind] = e;
			e = slot;
		}
		rehash();
		insert(e);
	}
	/**Return the entry identified by the input parameter key or null if it is not in the table.
	 * 
	 * @param key
	 * @return
	 */
	public E lookUp(long key) {
		E e;
		if ((e = t1[hash1(key)]) != null && e.key == key)
			return e;
		if ((e = t2[hash2(key)]) != null && e.key == key)
			return e;
		if ((e = t3[hash3(key)]) != null && e.key == key)
			return e;
		if ((e = t4[hash4(key)]) != null && e.key == key)
			return e;
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
		E e;
		if ((e = t1[(ind = hash1(key))]) != null && e.key == key) {
			t1[ind] = null;
			return true;
		}
		if ((e = t2[(ind = hash2(key))]) != null && e.key == key) {
			t2[ind] = null;
			return true;
		}
		if ((e = t3[(ind = hash3(key))]) != null && e.key == key) {
			t3[ind] = null;
			return true;
		}
		if ((e = t4[(ind = hash4(key))]) != null && e.key == key) {
			t4[ind] = null;
			return true;
		}
		return false;
	}
	/**Removes all the entries that match the condition specified in the argument.
	 * 
	 * @param condition
	 */
	public void remove(Predicate<E> condition) {
		E e;
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
		for (int i = 0; i < t3.length; i++) {
			e = t3[i];
			if (e != null && condition.test(e))
				t3[i] = null;
		}
		for (int i = 0; i < t4.length; i++) {
			e = t4[i];
			if (e != null && condition.test(e))
				t4[i] = null;
		}
		rehash();
	}
	@SuppressWarnings({"unchecked"})
	private void rehash() {
		E[] oldTable1 = t1;
		E[] oldTable2 = t2;
		E[] oldTable3 = t3;
		E[] oldTable4 = t4;
		float size = load/MINIMUM_LOAD_FACTOR;
		load = 0;
		t1 = (E[])new Entry[(int)(T1_SHARE*size)];
		t2 = (E[])new Entry[(int)(T2_SHARE*size)];
		t3 = (E[])new Entry[(int)(T3_SHARE*size)];
		t4 = (E[])new Entry[(int)(T4_SHARE*size)];
		for (E e : oldTable1) {
			if (e != null)
				insert(e);
		}
		for (E e : oldTable2) {
			if (e != null)
				insert(e);
		}
		for (E e : oldTable3) {
			if (e != null)
				insert(e);
		}
		for (E e : oldTable4) {
			if (e != null)
				insert(e);
		}
	}
	/**Replaces the current tables with new, empty hash tables of the same sizes.*/
	@SuppressWarnings({"unchecked"})
	public void clear() {
		load = 0;
		t1 = (E[])new Entry[t1.length];
		t2 = (E[])new Entry[t2.length];
		t3 = (E[])new Entry[t3.length];
		t4 = (E[])new Entry[t4.length];
	}
	/**Prints all non-null entries to the console.*/
	public void printAll() {
		System.out.println("TABLE_1:\n");
		for (E e : t1) {
			if (e != null)
				System.out.println(e);
		}
		System.out.println("TABLE_2:\n");
		for (E e : t2) {
			if (e != null)
				System.out.println(e);
		}
		System.out.println("TABLE_3:\n");
		for (E e : t3) {
			if (e != null)
				System.out.println(e);
		}
		System.out.println("TABLE_4:\n");
		for (E e : t4) {
			if (e != null)
				System.out.println(e);
		}
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
