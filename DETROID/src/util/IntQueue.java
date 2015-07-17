package util;

public class IntQueue {
	
	private class IntListItem {
		
		int data;
		IntListItem next;
		
		IntListItem(int data) {
			this.data = data;
		}
	}
	
	private IntListItem head;
	private IntListItem tail;
	private IntListItem iterator;
	private int length = 0;
	
	public IntQueue() {

	}
	public IntQueue(int data) {
		this.head = new IntListItem(data);
		this.tail = head;
		this.iterator = head;
		length++;
	}
	public int getHead() {
		return this.head.data;
	}
	public int getTail() {
		return this.tail.data;
	}
	public void add(int data) {
		if (this.head == null) {
			this.head = new IntListItem(data);
			this.tail = head;
			this.iterator = head;
		}
		else {
			this.tail.next = new IntListItem(data);
			this.tail = tail.next;
		}
		length++;
	}
	public boolean hasNext() {
		if (this.iterator == null)
			return false;
		return true;
	}
	public int next() {
		int next = this.iterator.data;
		this.iterator = this.iterator.next;
		return next;
	}
	public void reset() {
		this.iterator = this.head;
	}
	public void pop() {
		if (this.head != null) {
			if (this.head.next != null)
				this.head = this.head.next;
			else
				this.head = null;
		}
		this.reset();
		length--;
		if (length == 0)
			this.tail = null;
	}
}
