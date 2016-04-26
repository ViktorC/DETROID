package util;

/**
 * A primitive type singly linked stack for 8 bit integers. It is very simple and is designed for simple applications. For the target use, it is
 * very fast and efficient. It does not hold incrementally updated references to the last element neither does it have a 'length' field, thus its
 * {@link #getTail() getTail}, {@link #length() length}, and {@link #toArray() toArray} methods have O(n) time-complexity and are not intended to
 * be used in performance sensitive applications.
 * 
 * @author Viktor
 *
 */
public class ByteStack extends ByteList {
	
	public ByteStack() {

	}
	public ByteStack(byte data) {
		head = new ByteListItem(data);
		iterator = head;
	}
	/**
	 * Pushes a new node storing the input parameter data onto the stack.
	 */
	@Override
	public void add(byte data) {
		ByteListItem temp = head;
		head = new ByteListItem(data);
		head.next = temp;
		reset();
	}
	/**
	 * Returns the data held in the last element of the stack. If the list is empty, it returns 0.
	 *
	 * This stack holds no incrementally updated reference to the last node, thus this operation
	 * requires the method to loop over all the nodes which makes it quite expensive.
	 *
	 * @return
	 */
	@Override
	public byte getTail() {
		byte out = 0;
		while (hasNext())
			out = next();
		return out;
	}
	/**
	 * Returns the number of nodes in the list.
	 * 
	 * This stack has no incrementally updated 'length' field, thus this operation requires
	 * the method to loop over all the nodes and count them, which makes it quite expensive.
	 *
	 * @return
	 */
	@Override
	public int length() {
		int c = 0;
		while (hasNext()) {
			next();
			c++;
		}
		return c;
	}
}
