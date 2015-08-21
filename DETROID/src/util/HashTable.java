package util;

public class HashTable<Entry extends Comparable & Hashable> {
	
	private static long hashMask1 = (1 << 32) - 1;
	private static long hashMask2 = -1^hashMask1;
	
	private Entry[] table;
	private int vacancies;
	
	public HashTable(int depth) {
		table = (Entry[])(new Object[5 << depth]);
		vacancies = table.length/2;
	}
	private int hash1(long key) {
		return (int)(key & hashMask1)%table.length;
	}
	private int hash2(long key) {
		return (int)(key & hashMask2)%table.length;
	}
	public void insert(Entry e) {
		
	}
	public Entry lookUp(long key) {
		Entry e = table[hash1(key)];
		if (e.key == key)
			return e;
		else if ((e = table[hash2(key)]).key == key)
			return e;
		return null;
	}
}
