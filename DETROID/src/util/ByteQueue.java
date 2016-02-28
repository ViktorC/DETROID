package util;

/**
 * A primitive type singly-linked queue data structure for 8 bit integers that holds an incrementally updated node count and a reference to the
 * last node. All of its methods are executed in constant time except {@link #toArray() toArray} which is still relatively fast and has a low O(n)
 * time-complexity.
 * 
 * @author Viktor
 *
 */
public class ByteQueue extends ByteList {
	
	private ByteListItem tail;		// A reference to the last node of the queue; for faster addition.
	private int length = 0;			// The number of nodes contained in the list.
	
	public ByteQueue() {

	}
	public ByteQueue(byte data) {
		head = new ByteListItem(data);
		tail = head;
		iterator = head;
		length++;
	}
	/**
	 * Returns the data held in the last element of the list. If the list is empty, it returns 0.
	 * 
	 * @return
	 */
	@Override
	public byte getTail() {
		if (tail != null)
			return tail.data;
		return 0;
	}
	/**
	 * Returns the number of nodes in the list.
	 * 
	 * @return
	 */
	@Override
	public int length() {
		return length;
	}
	/**
	 * Enqueues a new node storing the input parameter data.
	 */
	@Override
	public void add(byte data) {
		if (head == null) {
			head = new ByteListItem(data);
			tail = head;
			iterator = head;
		}
		else {
			tail.next = new ByteListItem(data);
			tail = tail.next;
		}
		length++;
	}
	/**
	 * Removes the head node form the list and returns the data stored in it.
	 * 
	 * If there is nothing to pop, it returns 0.
	 */
	@Override
	public byte pop() {
		length--;
		if (length == 0)
			tail = null;
		return super.pop();
	}
}
