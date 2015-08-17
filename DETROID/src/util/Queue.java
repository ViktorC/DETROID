package util;

/**A generic queue that keeps a node count and a reference to its last node.
 * 
 * @author Viktor
 *
 * @param <Data>
 */
public class Queue<Data> extends List<Data> {

	private IntListItem tail;		//a reference to the last node of the queue; for faster addition
	private int length = 0;			//the number of nodes contained in the list

	public Queue() {

	}
	public Queue(Data data) {
		head = new IntListItem(data);
		tail = head;
		iterator = head;
		length++;
	}
	/**Returns the data held in the last element of the list. If the list is empty, it returns null.
	 * 
	 * @return
	 */
	public Data getTail() {
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
	public void add(Data data) {
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
	 * If there is nothing to pop, it returns null.*/
	public Data pop() {
		length--;
		if (length == 0)
			tail = null;
		return super.pop();
	}
}
