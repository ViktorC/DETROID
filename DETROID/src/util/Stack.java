package util;

/**A generic stack.
 * 
 * @author Viktor
 *
 */
public class Stack<Data> extends List<Data> {

	public Stack() {

	}
	public Stack(Data data) {
		this.head.data = data;
		this.iterator = head;
	}
	/**Pushes a new node storing the input parameter data onto the stack.*/
	public void add(Data data) {
		IntListItem temp = this.head;
		this.head = new IntListItem(data);
		this.head.next = temp;
		this.reset();
	}
}
