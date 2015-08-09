package util;

/**A generic abstract class for providing the basics for list data structures such as stacks and queues.
 * 
 * @author Viktor
 *
 */
public abstract class List<Data> {

	/**A list entry containing two fields, 'data' that holds the data, and 'next' referencing the subsequent element in the list
	 * 
	 * @author Viktor
	 *
	 */
	protected class IntListItem {
		
		protected Data data;
		protected IntListItem next;
		
		IntListItem(Data data) {
			this.data = data;
		}
	}
	
	protected IntListItem head;
	protected IntListItem iterator;			//used for keeping track of the current node while iterating over the list
	
	/**Returns the data held in the first element of the list. If the list is empty, it returns null.*/
	public Data getHead() {
		if (this.head != null)
			return this.head.data;
		return null;
	}
	/**The addition method that differs for different types of list data structures thus needs to be implemented by subclasses.*/
	public abstract void add(Data data);
	/**Returns whether the iterator has more nodes to process or has already reached the end of the list. Once the iterator has no more nodes no to
	 * process, the method will return 'false' and the iterator will be reset.*/
	public boolean hasNext() {
		if (this.iterator == null) {
			this.reset();
			return false;
		}
		return true;
	}
	/**Returns the data held in the iterator and increments the iterator.*/
	public Data next() {
		Data next = this.iterator.data;
		this.iterator = this.iterator.next;
		return next;
	}
	/**Resets the iterator to the head.*/
	public void reset() {
		this.iterator = this.head;
	}
	/**Removes the head node form the list and returns the data stored in it.
	 * 
	 * If there is nothing to pop, it returns null.*/
	public Data pop() {
		if (this.head != null) {
			Data data = this.head.data;
			if (this.head.next != null)
				this.head = this.head.next;
			else
				this.head = null;
			this.reset();
			return data;
		}
		return null;
	}
}
