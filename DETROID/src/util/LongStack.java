package util;

/**A primitive type stack data structure for 64-bit integers, i.e. longs.
 * 
 * @author Viktor
 *
 */
public class LongStack extends LongList {
	
	public LongStack() {

	}
	public LongStack(long data) {
		this.head.data = data;
		this.iterator = head;
	}
	/**Pushes a new node storing the input parameter data onto the stack.*/
	public void add(long data) {
		LongListItem temp = this.head;
		this.head = new LongListItem(data);
		this.head.next = temp;
		this.reset();
	}
}