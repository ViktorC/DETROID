package util;

/**A primitive type queue data structure for 32-bit integers.
 * 
 * @author Viktor
 *
 */
public class IntQueue extends IntList {
	
	private IntListItem tail;		//a reference to the last node of the queue; for faster addition
	private int length = 0;			//the number of nodes contained in the list
	
	public IntQueue() {

	}
	public IntQueue(int data) {
		this.head = new IntListItem(data);
		this.tail = head;
		this.iterator = head;
		length++;
	}
	/**Returns the data held in the last element of the list.*/
	public int getTail() {
		if (this.tail != null)
			return this.tail.data;
		return 0;
	}
	/**Returns the number of nodes in the list.*/
	public int length() {
		return this.length;
	}
	/**Enqueues a new node storing the input parameter data.*/
	public void add(int data) {
		if (this.head == null) {
			this.head = new IntListItem(data);
			this.tail = head;
			this.iterator = head;
		}
		else {
			this.tail.next = new IntListItem(data);
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
