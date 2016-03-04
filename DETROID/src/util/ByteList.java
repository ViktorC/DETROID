package util;

import java.util.Iterator;

/**
 * A simple abstract class for providing the basics for byte-based list data structures such as stacks and queues.
 * 
 * @author Viktor
 *
 */
public abstract class ByteList implements Iterable<Byte> {

	/**
	 * A list entry containing two fields, 'data' that holds the information, a byte, and 'next' referencing the subsequent element in the list
	 * 
	 * @author Viktor
	 *
	 */
	protected class ByteListItem {
		
		protected byte data;
		protected ByteListItem next;
		
		ByteListItem(byte data) {
			this.data = data;
		}
	}
	
	protected ByteListItem head;
	protected ByteListItem iterator;			// Used for keeping track of the current node while iterating over the list.
	
	/**
	 * Returns the data held in the first element of the list. If the list is empty, it returns 0.
	 * 
	 * @return
	 */
	public byte getHead() {
		if (head != null)
			return head.data;
		return 0;
	}
	/**
	 * Adds all nodes of another list to this list.
	 * 
	 * @param list
	 */
	public void addAll(ByteList list) {
		while (list.hasNext())
			add(list.next());
	}
	/**
	 * Returns whether the pointer/iterator has more nodes to process or has already reached the end of the list. Once the iterator has no more
	 * nodes no to process, the method will return 'false' and the iterator will be reset.
	 * 
	 * @return
	 */
	public boolean hasNext() {
		if (iterator == null) {
			reset();
			return false;
		}
		return true;
	}
	/**
	 * Returns the data held in the pointer and sets it to the next element.
	 * 
	 * @return
	 */
	public byte next() {
		byte next = iterator.data;
		iterator = iterator.next;
		return next;
	}
	/**
	 * Resets the pointer to the head.
	 */
	public void reset() {
		iterator = head;
	}
	/**
	 * Removes the head node form the list and returns the data stored in it.
	 * 
	 * If there is nothing to pop, it returns 0.
	 * 
	 * @return
	 */
	public byte pop() {
		if (head != null) {
			byte data = head.data;
			if (head.next != null)
				head = head.next;
			else
				head = null;
			reset();
			return data;
		}
		return 0;
	}
	/**
	 * Its usage is advised against as the sole purpose of this class is the avoidance of implicit casting to increase performance.
	 */
	@Override
	public Iterator<Byte> iterator() {
		return new Iterator<Byte>() {
			public boolean hasNext() {
				return hasNext();
			}
			public Byte next() {
				return next();
			}
		};
	}
	/**
	 * Copies the data stored in the nodes of the list into an array in order, and returns the array.
	 * 
	 * @return
	 */
	public byte[] toArray() {
		int i = 0;
		byte[] arr = new byte[length()];
		while (hasNext()) {
			arr[i] = next();
			i++;
		}
		return arr;
	}
	/**
	 * Returns the data held in the last element of the list. If the list is empty, it returns 0.
	 * 
	 * @return
	 */
	public abstract byte getTail();
	/**
	 * Creates a node for the input data and adds it to the list.
	 * 
	 * @param data
	 */
	public abstract void add(byte data);
	/**
	 * Returns the number of nodes in the list.
	 * 
	 * @return
	 */
	public abstract int length();
}
