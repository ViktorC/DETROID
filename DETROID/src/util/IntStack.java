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
		this.head.data = data;
		this.iterator = head;
	}
	/**Pushes a new node storing the input parameter data onto the stack.*/
	public void add(int data) {
		IntListItem temp = head;
		this.head = new IntListItem(data);
		this.head.next = temp;
	}
}
