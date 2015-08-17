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
		head = new IntListItem(data);
		tail = head;
		iterator = head;
		length++;
	}
	/**Returns the data held in the last element of the list. If the list is empty, it returns 0.
	 * 
	 * @return
	 */
	public int getTail() {
		if (tail != null)
			return tail.data;
		return 0;
	}
	/**Returns the number of nodes in the list.
	 * 
	 * @return
	 */
	public int length() {
		return length;
	}
	/**Enqueues a new node storing the input parameter data.*/
	public void add(int data) {
		if (head == null) {
			head = new IntListItem(data);
			tail = head;
			iterator = head;
		}
		else {
			tail.next = new IntListItem(data);
			tail = tail.next;
		}
		length++;
	}
	/**Removes the head node form the list and returns the data stored in it.
	 * 
	 * If there is nothing to pop, it returns 0.*/
	public int pop() {
		length--;
		if (length == 0)
			tail = null;
		return super.pop();
	}
}
