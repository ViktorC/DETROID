package util;

public class IntQueue extends IntList {
	
	private static IntList last;
	
	public IntQueue() {
		
	}
	public IntQueue(int data) {
		this.data = data;
	}
	public IntList getLast() {
		return last;
	}
	public void add(int data) {
		if (this.data == 0) {
			this.data = data;
			last = this;
		}
		else {
			last.next = new IntQueue(data);
			last = last.next;
		}
	}
}
