package util;

public class LongQueue extends LongList {
	
	private static LongList last;
	
	public LongQueue() {
		
	}
	public LongQueue(long data) {
		this.data = data;
	}
	public LongList getLast() {
		return last;
	}
	public void add(long data) {
		if (this.data == 0) {
			this.data = data;
			last = this;
		}
		else {
			last.next = new LongQueue(data);
			last = last.next;
		}
	}
}