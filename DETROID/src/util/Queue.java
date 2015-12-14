package util;

/**A generic singly-linked queue that keeps an incrementally updated node count and a reference to its last node. All of its methods are executed in constant
 * time except {@link #toArray() toArray} which is still relatively fast and has a low O(n) complexity, near half of that of my stacks.
 * 
 * @author Viktor
 *
 * @param <T>
 */
public class Queue<T> extends List<T> {

	private ListItem tail;		//a reference to the last node of the queue; for faster addition
	private int length = 0;		//the number of nodes contained in the list

	public Queue() {

	}
	public Queue(T data) {
		head = new ListItem(data);
		tail = head;
		iterator = head;
		length++;
	}
	/**Returns the data held in the last element of the list. If the list is empty, it returns null.
	 * 
	 * @return
	 */
	public T getTail() {
		if (tail != null)
			return tail.data;
		return null;
	}
	/**Returns the number of nodes in the list.
	 * 
	 * @return
	 */
	public int length() {
		return length;
	}
	/**Enqueues a new node storing the input parameter data.*/
	public void add(T data) {
		if (head == null) {
			head = new ListItem(data);
			tail = head;
			iterator = head;
		}
		else {
			tail.next = new ListItem(data);
			tail = tail.next;
		}
		length++;
	}
	/**Removes the head node form the list and returns the data stored in it.
	 * 
	 * If there is nothing to pop, it returns null.*/
	public T pop() {
		length--;
		if (length == 0)
			tail = null;
		return super.pop();
	}
}
