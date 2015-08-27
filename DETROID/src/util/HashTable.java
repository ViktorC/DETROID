package util;

/**A generic, so far non-thread-safe hash table utilizing cuckoo hashing with constant look-up time. Entries of the hash table are required to extend
 * {@link #HashTable.Entry Entry} and implement {@link #Comparable Comparable}.
 * 
 * @author Viktor
 *
 * @param <E>
 */
public class HashTable<E extends HashTable.Entry & Comparable<E>> {
	
	/**An abstract base class for hash table entries the defines the long variable 'key' upon what the entry objects are hashed onto an index of the hash table.
	 * 
	 * @author Viktor
	 *
	 */
	public static abstract class Entry {
		
		protected long key; //a 64 bit integer key that is hashed onto an index
		
	}
	
	public final static int DEFAULT_SIZE = 1 << 6;
	
	private final static long HASH_MASK1 = (1L << 63) - 1;
	private final static long HASH_MASK2 = HASH_MASK1^((1L << 32) - 1);
	
	private E[] table;
	private int vacancies;
	
	/**Instantiates a HashTable with a default size of 64.*/
	@SuppressWarnings({"unchecked"})
	public HashTable() {
		table = (E[])new Entry[DEFAULT_SIZE];
		vacancies = DEFAULT_SIZE/2;
	}
	@SuppressWarnings({"unchecked"})
	public HashTable(int size) {
		table = (E[])new Entry[size];
		vacancies = size/2;
	}
	/**Returns the size of the hash table.
	 * 
	 * @return
	 */
	public int size() {
		return table.length;
	}
	/**Returns the number of occupied slots in the hash table.
	 * 
	 * @return
	 */
	public int load() {
		return table.length/2 - vacancies;
	}
	/**Inserts an entry into the hash table.
	 * 
	 * @param e
	 */
	public void insert(E e) {
		int ind;
		E slot;
		for (int i = 0; i < vacancies; i++) {
			ind = hash1(e.key);
			slot = table[ind];
			if (slot == null) {
				table[ind] = e;
				return;
			}
			else if (slot.key == e.key) {
				if (e.greaterThan(slot))
					table[ind] = e;
				return;
			}
			slot = table[(ind = hash2(e.key))];
			if (slot == null) {
				table[ind] = e;
				return;
			}
			else if (slot.key == e.key) {
				if (e.greaterThan(slot))
					table[ind] = e;
				return;
			}
			else {
				table[ind] = e;
				e = slot;
			}
		}
		rehash(table.length << 1);
		insert(e);
	}
	/**Return the entry identified by the input parameter key or null if it is not in the table.
	 * 
	 * @param key
	 * @return
	 */
	public E lookUp(long key) {
		E e = table[hash1(key)];
		if (e != null && e.key == key)
			return e;
		else if ((e = table[hash2(key)]) != null && e.key == key)
			return e;
		return null;
	}
	@SuppressWarnings({"unchecked"})
	private void rehash(int newSize) {
		E[] oldTable = table;
		vacancies = newSize/2 - load();
		table = (E[])new Entry[newSize];
		for (E e : oldTable) {
			if (e != null)
				insert(e);
		}
	}
	private int hash1(long key) {
		return (int)((key & HASH_MASK1)%table.length);
	}
	private int hash2(long key) {
		return (int)((key & HASH_MASK2)%table.length);
	}
	/**Replaces the current table with a new, empty hash table of the same size.*/
	@SuppressWarnings({"unchecked"})
	public void clear() {
		table = (E[])new Entry[table.length];
		vacancies = table.length/2;
	}
}
