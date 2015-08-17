package util;

/**A primitive type stack data structure for 32-bit integers.
 * 
 * @author Viktor
 *
 */
public class IntStack extends IntList {
	
	public IntStack() {

	}
	public IntStack(int data) {
		head.data = data;
		iterator = head;
	}
	/**Pushes a new node storing the input parameter data onto the stack.*/
	public void add(int data) {
		IntListItem temp = head;
		head = new IntListItem(data);
		head.next = temp;
		reset();
	}
	/**Returns the data held in the last element of the stack. If the list is empty, it returns 0.
	 *
	 * This stack holds no incrementally updated reference to the last node, thus this operation
	 * requires the method to loop over all the nodes which makes it quite expensive.
	 *
	 * @return
	 */
	public int getTail() {
		int out = 0;
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
