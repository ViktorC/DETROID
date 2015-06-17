package util;

public class LongStack extends LongList {

	public LongStack() {
		
	}
	public LongStack(long data) {
		this.data = data;
	}
	public LongStack(LongStack stack) {
		this.data = stack.data;
		this.next = stack.next;
	}
	public void add(long data) {
		LongList temp = new LongStack(this);
		this.data = data;
		this.next = temp;
	}
}
