package util;

/**A generic singly linked stack. It is very simple and is designed for simple applications. For the target use, it is very fast and efficient.
 * It does not hold incrementally updated references to the last element neither does it have a 'length' field, thus its {@link #getTail() getTail},
 * {@link #length() length}, and {@link #toArray() toArray} methods have O(n) time-complexity and are not intended to be used in performance sensitive
 * applications.
 * 
 * @author Viktor
 *
 */
public class Stack<T> extends List<T> {

	public Stack() {

	}
	public Stack(T data) {
		head.data = data;
		iterator = head;
	}
	/**Pushes a new node storing the input parameter data onto the stack.*/
	public void add(T data) {
		ListItem temp = head;
		head = new ListItem(data);
		head.next = temp;
		reset();
	}
	/**Returns the data held in the last element of the list. If the list is empty, it returns null.
	 *
	 * This stack holds no incrementally updated reference to the last node, thus this operation
	 * requires the method to loop over all the nodes which makes it quite expensive.
	 *
	 * @return
	 */
	public T getTail() {
		T out = null;
		while (hasNext())
			out = next();
		return out;
	}
	/**Returns the number of nodes in the list.
	 * 
	 * This stack has no incrementally updated 'length' field, thus this operation requires
	 * the method to loop over all the nodes and count them, which makes it quite expensive.
	 *
	 * @return
	 */
	public int length() {
		int c = 0;
		while (hasNext()) {
			next();
			c++;
		}
		return c;
	}
}
