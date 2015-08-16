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
		this.head = new IntListItem(data);
		this.tail = head;
		this.iterator = head;
		length++;
	}
	/**Returns the data held in the last element of the list. If the list is empty, it returns null.
	 * 
	 * @return
	 */
	public Data getTail() {
		if (this.tail != null)
			return this.tail.data;
		return null;
	}
	/**Returns the number of nodes in the list.
	 * 
	 * @return
	 */
	public int length() {
		return this.length;
	}
	/**Enqueues a new node storing the input parameter data.*/
	public void add(Data data) {
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
	/**Removes the head node form the list and returns the data stored in it.
	 * 
	 * If there is nothing to pop, it returns null.*/
	public Data pop() {
		length--;
		if (length == 0)
			this.tail = null;
		return super.pop();
	}
	/**Copies references to the nodes of the queue into an array in order, and returns the array.
	 * 
	 * @return
	 */
	public Data[] toArray() {
		@SuppressWarnings({"unchecked"})
		Data[] arr = (Data[])new Object[this.length];
		int i = 0;
		while (this.hasNext()) {
			arr[i] = this.next();
			i++;
		}
		return arr;
	}
}
