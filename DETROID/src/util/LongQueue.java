package util;

public class LongQueue {

	private class LongListItem {

		long data;
		LongListItem next;

		LongListItem(long data) {
			this.data = data;
		}
	}

	private LongListItem head;
	private LongListItem tail;
	private LongListItem iterator;
	private int length = 0;

	public LongQueue() {

	}
	public LongQueue(long data) {
		this.head = new LongListItem(data);
		this.tail = head;
		this.iterator = head;
		length++;
	}
	public long getHead() {
		return this.head.data;
	}
	public long getTail() {
		return this.tail.data;
	}
	public void add(long data) {
		if (this.head == null) {
			this.head = new LongListItem(data);
			this.tail = head;
			this.iterator = head;
		}
		else {
			this.tail.next = new LongListItem(data);
			this.tail = tail.next;
		}
		length++;
	}
	public boolean hasNext() {
		if (this.iterator == null)
			return false;
		return true;
	}
	public long next() {
		long next = this.iterator.data;
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
