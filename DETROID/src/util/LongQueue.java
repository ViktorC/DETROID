package util;

/**A primitive type queue data structure for 64-bit integers, i.e. longs.
 * 
 * @author Viktor
 *
 */
public class LongQueue extends LongList {
	
	private LongListItem tail;		//a reference to the last node of the queue; for faster addition
	private int length = 0;			//the number of nodes contained in the list
	
	public LongQueue() {

	}
	public LongQueue(long data) {
		this.head = new LongListItem(data);
		this.tail = head;
		this.iterator = head;
		length++;
	}
	/**Returns the data held in the last element of the list.*/
	public long getTail() {
		if (this.tail != null)
			return this.tail.data;
		return 0;
	}
	/**Returns the number of nodes in the list.*/
	public int length() {
		return this.length;
	}
	/**Enqueues a new node storing the input parameter data.*/
	public void add(long data) {
		if (this.head == null) {
			this.head = new LongListItem(data);
			this.tail = head;
			this.iterator = head;
		}
		else {
			this.tail.next = new LongListItem(data);
			this.tail = tail.next;
		}
		length++;
	}
	/**Removes the head node form the list.
	 * 
	 * !It does not return its value!
	 */
	public void pop() {
		super.pop();
		length--;
		if (length == 0)
			this.tail = null;
	}
}