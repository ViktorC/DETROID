package util;

public class HashTable<E extends HashTable.Entry & Comparable<E>> {
	
	public static abstract class Entry {
		
		protected long key;
		
	}
	
	public static interface HashFunction {
		
		public int hash(long key);
		
	}
	
	private final static int DEFAULT_SIZE = 1 << 6;
	
	private final static long HASH_MASK1 = (1 << 32) - 1;
	private final static long HASH_MASK2 = -1^HASH_MASK1;
	
	private HashFunction h1 = key -> (int)(key & HASH_MASK1);
	private HashFunction h2 = key -> (int)(key & HASH_MASK2);
	
	private E[] table;
	private int vacancies;
	
	@SuppressWarnings({"unchecked"})
	public HashTable() {
		table = (E[])new Object[DEFAULT_SIZE];
		vacancies = DEFAULT_SIZE/2;
	}
	public HashTable(HashFunction h1, HashFunction h2) {
		this();
		this.h1 = h1;
		this.h2 = h2;
	}
	@SuppressWarnings({"unchecked"})
	public HashTable(int size) {
		table = (E[])new Object[size];
		vacancies = size/2;
	}
	public HashTable(int size, HashFunction h1, HashFunction h2) {
		this(size);
		this.h1 = h1;
		this.h2 = h2;
	}
	public int size() {
		return table.length;
	}
	public int load() {
		return table.length/2 - vacancies;
	}
	public void insert(E e) {
		int ind = h1.hash(e.key);
		E slot = table[ind];
		if (slot == null) {
			table[ind] = e;
			return;
		}
		else if (slot.key == e.key) {
			if (e.greaterThan(slot))
				table[ind] = e;
			return;
		}
		slot = table[(ind = h2.hash(e.key))];
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
			for (int i = 0; i < vacancies; i++) {
				
			}
		}
	}
	public E lookUp(long key) {
		E e = table[h1.hash(key)];
		if (e.key == key)
			return e;
		else if ((e = table[h2.hash(key)]).key == key)
			return e;
		return null;
	}
	@SuppressWarnings({"unchecked"})
	private void rehash(int newSize) {
		E[] oldTable = table;
		table = (E[])new Object[newSize];
		for (E e : oldTable)
			insert(e);
	}
}
