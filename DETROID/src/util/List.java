package util;

import java.lang.reflect.Array;

/**A generic abstract class for providing the basics for list data structures such as stacks and queues.
 * 
 * @author Viktor
 *
 * @param <Data>
 */
public abstract class List<Data> {

	/**A list entry containing two fields, 'data' that holds the data, and 'next' referencing the subsequent element in the list
	 * 
	 * @author Viktor
	 *
	 */
	protected class ListItem {
		
		protected Data data;
		protected ListItem next;
		
		ListItem(Data data) {
			this.data = data;
		}
	}
	
	protected ListItem head;
	protected ListItem pointer;			//used for keeping track of the current node while iterating over the list
	
	/**Returns the data held in the first element of the list. If the list is empty, it returns null.
	 * 
	 * @return
	 */
	public Data getHead() {
		if (head != null)
			return head.data;
		return null;
	}
	/**Returns the data held in the last element of the list. If the list is empty, it returns null.
	 *
	 * @return
	 */
	public abstract Data getTail();
	/**Creates a node for the input data and adds it to the list.
	 * 
	 * @param data
	 */
	public abstract void add(Data data);
	/**Returns whether the pointer/iterator has more nodes to process or has already reached the end of the list. Once the iterator has no more
	 * nodes no to process, the method will return 'false' and the iterator will be reset.
	 * 
	 * @return
	 */
	public boolean hasNext() {
		if (pointer == null) {
			reset();
			return false;
		}
		return true;
	}
	/**Returns the data held in the pointer and sets it to the next element.
	 * 
	 * @return
	 */
	public Data next() {
		Data next = pointer.data;
		pointer = pointer.next;
		return next;
	}
	/**Resets the pointer to the head.*/
	public void reset() {
		pointer = head;
	}
	/**Removes the head node form the list and returns the data stored in it.
	 *  
	 * If there is nothing to pop, it returns null.
	 * 
	 * @return
	 */
	public Data pop() {
		if (head != null) {
			Data data = head.data;
			if (head.next != null)
				head = head.next;
			else
				head = null;
			reset();
			return data;
		}
		return null;
	}
	/**Returns the number of nodes in the list.
	 * 
	 * @return
	 */
	public abstract int length();
	/**Copies references to the nodes of the list into an array in order, and returns the array.
	 *
	 * If the list is empty, it throws a NullPointerException.
	 *
	 * @return
	 */
	public Data[] toArray() throws NullPointerException {
		int i = 0;
		@SuppressWarnings({"unchecked"})
		Data[] arr = (Data[])Array.newInstance(head.data.getClass(), length());
		while (hasNext()) {
			arr[i] = next();
			i++;
		}
		return arr;
	}
}
