package util;

/**A simple abstract class for providing the basics for long-based list data structures such as stacks and queues.
 * 
 * @author Viktor
 *
 */
public abstract class LongList {

	/**A list entry containing two fields, 'data' that holds the information, a long, and 'next' referencing the subsequent element in the list
	 * 
	 * @author Viktor
	 *
	 */
	protected class LongListItem {
		
		protected long data;
		protected LongListItem next;
		
		LongListItem(long data) {
			this.data = data;
		}
	}
	
	protected LongListItem head;
	protected LongListItem iterator;			//used for keeping track of the current node while iterating over the list
	
	/**Returns the data held in the first element of the list.*/
	public long getHead() {
		return this.head.data;
	}
	/**The addition method that differs for different types of list data structures thus needs to be implemented by subclasses.*/
	public abstract void add(long data);
	/**Returns whether the iterator has more nodes to process or has already reached the end of the list.*/
	public boolean hasNext() {
		if (this.iterator == null)
			return false;
		return true;
	}
	/**Returns the data held in the iterator and increments the iterator.*/
	public long next() {
		long next = this.iterator.data;
		this.iterator = this.iterator.next;
		return next;
	}
	/**Resets the iterator to the head.*/
	public void reset() {
		this.iterator = this.head;
	}
	/**Removes the head node form the list.
	 * 
	 * !It does not return its value!
	 */
	public void pop() {
		if (this.head != null) {
			if (this.head.next != null)
				this.head = this.head.next;
			else
				this.head = null;
		}
		this.reset();
	}
}
